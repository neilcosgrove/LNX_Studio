// ************ //
// Marker mode  //
// ************ //

// +++ proll + dark patches + marker metadata ie default, clip or wrap,  rev
// ** markers aren't updated when deleted or added, only when moved
// ** make pos lazy driven
// ** when mouseDown or scrolling reduce fps with lazyFresh

LNX_MarkerEvent {
	var <>markerNo, <>deltaBeats, <>offset, <>startFrame, <>durFrame;
	*new     {|markerNo, deltaBeats, offset, startFrame, durFrame|
		^super.newCopyArgs(markerNo, deltaBeats, offset, startFrame, durFrame)
	}
	endFrame { ^startFrame+durFrame }
	free     { markerNo = deltaBeats = offset = startFrame = durFrame = nil }
	printOn  {|stream| stream << this.class.name << "(" << markerNo << "," << deltaBeats << "," << offset
							  << "," << startFrame << "," << durFrame << ")" }
}

// pseudo random coin, provide your own seed each time
// how can we use seeds to make similar but different?
// you migth use hash coin like this
// probRatio.hashCoin(beat); // samll difference in probRatio will have similar result but small diff
// or
// probRatio.hashCoin(beat*probRatio); // samll difference in probRatio will not have similar result i.e big diff
+ Number{ hashCoin{|hash,precision=1000000| ^(this>(hash.hash%precision/precision))} }

// space bar is playing wrong sample on load
// you can't move start marker to zero

+ LNX_StrangeLoop {

	marker_initVars{
		markerSeq = [];
		allMakerEvents = [];
	}
		// method 1
		// loop length is end-start @ playbackRate of 1
		// assume 4 x 4 unless specified
		// compare to absTime to work out nearest multiple of beatLength 64 beats or what ever (but can be set)
		// so then time line is streched to fit this length of n beats.
		// and markers are scheduled to this timeline

		// method 2
		// do as before but this time force a playbackRate of 1
		// launch events at marker point
		// this still need bpm knowledge

	// called from load & ?

	// on load is no good, must be on 1st import?

	// this should only happen on 1st import else length could change

	// this isn't called by anthing yet
	marker_updateVars{
		var startRatio, endRatio, durRatio, totalDuration, absTime, loopDurTime, length;
		var sampleIndex=p[11];

		if (sampleBank[sampleIndex].isNil) { ^this };		 	// no samples loaded in bank exception

		startRatio    = sampleBank.actualStart(sampleIndex);	// start pos ratio
		endRatio      = sampleBank.actualEnd  (sampleIndex);	// end   pos ratio
		durRatio      = endRatio - startRatio;					// dur ratio
		totalDuration = sampleBank.duration(sampleIndex);		// total dur of sample in secs
		loopDurTime   = totalDuration * durRatio;			 	// total dur of loop in secs
		absTime       = studio.absTime;							// rate of stuido clock ticks
		length		  = (loopDurTime / absTime /3).round.asInt;	// dur of loop in beats, on slower clock

		sampleBank.modelValueAction_(sampleIndex,\length,length,send:false); // set length in buffer metadata

		this.marker_makeSeq;
	}

	// ***********

	marker_makeSeq{
		var startRatio, endRatio, durRatio, length, length3;
		var workingMarkers, workingDurs, numFrames;
		var sampleIndex=p[11];

		if (sampleBank[sampleIndex].isNil) { ^this }; 		// no samples loaded in bank exception

		startRatio	= sampleBank.actualStart(sampleIndex);	// start pos ratio
		endRatio    = sampleBank.actualEnd  (sampleIndex);	// end   pos ratio
		durRatio    = endRatio - startRatio;				// dur ratio
		length 		= sampleBank.length(sampleIndex).asInt;
		length3		= length * 3;							// this is length of loop in beats on clock3
		numFrames	= sampleBank.numFrames(sampleIndex);	// total number of frames in sample

		// ***
		// so this length needs to be shorten some how ??
		//
		//markerSeq	= nil ! length3; // now i need to put the markers into this seq, clock dur split up into beats.
		markerSeq	= nil ! ((length3 * durRatio).round(3).asInt); // this is not a good idea ??

		// maybe don't do *durRatio above ?

		allMakerEvents = [];

		workingMarkers = sampleBank.workingMarkers(sampleIndex); // ratio of buf len
		workingDurs    = sampleBank.workingDurs   (sampleIndex); // ratio of buf len

		// into seq goes:  offset start in sec, startFrame, durFrame
		workingMarkers.do{|marker,markerNo|
			var deltaBeats  = (marker - startRatio * length3); // when will it happen from 0 @ start & length3

			//var deltaBeats  = ((marker - startRatio) / durRatio * length3); // when will it happen from 0 @ start & length3

			var when        = deltaBeats.round(3).clip(0,markerSeq.size-1); // quantise
			var index  		= when.floor.asInt;					// where to put in the seq index
			var offset		= when.frac;     					// what is frac offset from beat
			var startFrame	= (marker * numFrames).asInt;
			var durFrame	= (workingDurs[markerNo] * numFrames).asInt;
			var markerEvent = LNX_MarkerEvent(markerNo, deltaBeats, offset, startFrame, durFrame);

			allMakerEvents 	= allMakerEvents.add(markerEvent); // for pRoll
			markerSeq.clipPut(index, markerEvent); // for autoSeq

		};

		if (gui[\newMarkerLength].notNil) { gui[\newMarkerLength].string_((markerSeq.size/3*64/length).asString) };

	}

	marker_Import{
		markerSeq.postln;
		markerSeq.size.postln; // we are forcing a length that might not be correct, can we set this?
		allMakerEvents.postln;


	}


	//// ***** use BPM !!!!!

	// repitch mode
	marker_clockIn3{|instBeat3,absTime3,latency,beat3|
		var length3, markerEvent, instBeat;
		var sampleIndex=p[11];						  // sample used in bank

		if (this.isOff   ) { ^this };                 // inst is off exception
		if (p[18].isFalse) { ^this };				  // no hold exception
		if (sampleBank[sampleIndex].isNil) { ^this }; // no samples loaded in bank exception

		//instBeat = instBeat3/3; 					  // use inst beat at slower rate

		length3 = sampleBank.length(sampleIndex).asInt * 3; // this is length of loop in beats on clock3

//		markerEvent = markerSeq[instBeat3 % length3];
		markerEvent = markerSeq.wrapAt(instBeat3); // midi in might be out of range so wrap. maybe return nil?

		if  (markerEvent.notNil) {
			if (((p[15]/100).hashCoin(beat3)) && (markerEvent.notNil)) {
				markerEvent = lastMarkerEvent;	// repeat
				repeatNo = repeatNo + 1;		// inc number of repeats
			}{
				repeatNo = 0;					// don't repeat
			};
		};

		// launch at start pos  [ or relaunch sample if needed** no relaunch yet]
		if (markerEvent.notNil) {
			var sample      = sampleBank[sampleIndex];
			var rate		= (p[12]+p[13]+(repeatNo*p[16])).midiratio.round(0.0000000001).clip(0,100000);
			var amp         = p[17]**repeatNo;
			var clipMode    = p[14];
			var bufferL		= sample.buffer.bufnum(0);          	// this only comes from LNX_BufferArray
			var bufferR		= sample.buffer.bufnum(1) ? bufferL; 	// this only comes from LNX_BufferArray

			{
		this.marker_playBuffer(bufferL,bufferR,rate,markerEvent.startFrame,markerEvent.durFrame,1,clipMode,amp,latency);
				nil;
			}.sched(markerEvent.offset * absTime3);

			lastMarkerEvent = markerEvent;
		};

		// change pos rate if bpm changed
		if (newBPM and:{node.notNil}) {};

	}

	// sample bank has changed...so do this
	marker_update{|model|
		var sampleIndex=p[11];  // sample used in bank

		if (sampleBank[sampleIndex].isNil) { ^this }; // no samples loaded in bank exception

		if (model==\itemFunc) { ^this };

		if (model==\selectFunc) {
			this.marker_makeSeq;
		};
		if ((model==\start)||(model==\end)) {
			this.marker_makeSeq;
			relaunch = true; // modelValueAction_ may do a relaunch as well but not always
		};
		if (model==\length) {
			this.marker_makeSeq;
			relaunch = true;
		};
		if (model==\markers) {
			this.marker_makeSeq;
			relaunch = true;
		};
	}

	// change playback rate
	marker_changeRate{|latency|
		(noteOnNodes++node).select(_.notNil).do{|node|
			server.sendBundle(latency +! syncDelay,[\n_set, node, \rate, (p[12]+p[13]).midiratio.round(0.0000000001)]);
		};
	}

	// this might need a buffer and a voicer

	// still release problems when swapping over from hold or stopping
	marker_pipeIn{|pipe|

		if (sampleBank[p[11]].isNil) { ^this }; // no sample exception

		if (pipe.isNoteOff) {
			var note    = pipe.note.asInt;
			var latency = pipe.latency;
			var node    = noteOnNodes[note];
			if (node.isNil) { ^this }; // no node exception
			if (node.notNil) { server.sendBundle(latency +! syncDelay, ["/n_set", node, \gate, 0]) };
			noteOnNodes[note] = nil;
		};

		if (p[18].isTrue) { ^this }; // hold on exception

		if (pipe.isNoteOn ) {
			var note		= pipe.note.asInt;
			var vel			= pipe.velocity;
			var latency     = pipe.latency;
			var markerEvent = allMakerEvents.wrapAt(note);
			var sampleIndex = p[11];
			var sample      = sampleBank[sampleIndex];
			var rate		= (p[12]+p[13]).midiratio.round(0.0000000001).clip(0,100000);
			var amp         = vel/127;
			var clipMode    = p[14];
			var bufferL		= sample.buffer.bufnum(0);          	// this only comes from LNX_BufferArray
			var bufferR		= sample.buffer.bufnum(1) ? bufferL; 	// this only comes from LNX_BufferArray
			if (markerEvent.isNil) { ^this }; // no marker event exception

			// incase already playing ??
			if (noteOnNodes[note].notNil) {
				server.sendBundle(latency +! syncDelay, ["/n_set", noteOnNodes[note], \gate, 0])
			};

			noteOnNodes[note] =
			  this.marker_playBufferMIDI(
				bufferL,bufferR,rate,markerEvent.startFrame,markerEvent.durFrame,1,clipMode,amp,latency
			);
			lastMarkerEvent = markerEvent;
		};

	}

	// play a buffer for play loop mode
	marker_playBuffer{|bufnumL,bufnumR,rate,startFrame,durFrame,attackLevel,clipMode,amp,latency|

		this.marker_stopBuffer(latency);

		node = server.nextNodeID;

		server.sendBundle(latency +! syncDelay, ["/s_new", \SLoopMarker, node, 0, instGroupID,
			\outputChannels,	this.instGroupChannel,
			\id,				id,
			\amp,				amp,
			\bufnumL,			bufnumL,
			\bufnumR,			bufnumR,
			\rate,				rate,
			\startFrame,		startFrame,
			\durFrame:			durFrame,
			\attackLevel:		attackLevel,
			\clipMode:			clipMode
		]);
	}

	// play a buffer for sequencer mode
	marker_playBufferMIDI{|bufnumL,bufnumR,rate,startFrame,durFrame,attackLevel,clipMode,amp,latency|
		var node = server.nextNodeID;

		server.sendBundle(latency +! syncDelay, ["/s_new", \SLoopMarker, node, 0, instGroupID,
			\outputChannels,	this.instGroupChannel,
			\id,				id,
			\amp,				amp,
			\bufnumL,			bufnumL,
			\bufnumR,			bufnumR,
			\rate,				rate,
			\startFrame,		startFrame,
			\durFrame:			durFrame,
			\attackLevel:		attackLevel,
			\clipMode:			clipMode
		]);

		^node; // for noteOn noteOff

	}

	*marker_initUGens{|server|

		SynthDef("SLoopMarker",{|outputChannels=0,bufnumL=0,bufnumR=0,rate=1,startFrame=0,durFrame=44100,
				gate=1,attackLevel=1, clipMode=0, id=0, amp=1|
			var signal;
			var index  = Integrator.ar((rate * BufRateScale.ir(bufnumL)).asAudio);

			index = startFrame +
				Select.ar(clipMode,[ index.clip(0,durFrame) , index.fold(0,durFrame) , index.wrap(0,durFrame) ]);

			signal = BufRd.ar(1, [bufnumL,bufnumR], index, loop:0); // mono, might need to be leaked
			signal = signal * EnvGen.ar(Env.new([attackLevel,1,0], [0.01,0.01], [2,-2], 1),gate,amp,doneAction:2);

			DetectSilence.ar(Slope.ar(index), doneAction:2); // ends when index slope = 0
			OffsetOut.ar(outputChannels,signal);			 // now send out

			SendReply.kr(Impulse.kr(20), '/sIdx', [index], id); // send sample index back to client

		}).send(server);

	}

	// index of playback, returned from the server
	sIdx_in_{|index|
		var sampleIndex = p[11];
		var numFrames	= sampleBank.numFrames(sampleIndex);	// total number of frames in sample

		if (sampleBank.otherModels[sampleIndex].notNil) {
			sampleBank.otherModels[sampleIndex][\pos].lazyValueAction_(index/numFrames);
		};
	}

	// stop playing buffer
	marker_stopBuffer{|latency|
		if (node.notNil) { server.sendBundle(latency +! syncDelay, ["/n_set", node, \gate, 0]) };
	}

	marker_stopPlay{
		lastMarkerEvent = nil;
		repeatNo        = 0;
	}

}
