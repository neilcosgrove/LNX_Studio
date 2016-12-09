// ************ //
// Marker mode  //
// ************ //

LNX_MarkerEvent {
	var <>markerNo, <>deltaBeats, <>offset, <>startFrame, <>durFrame;
	*new     {|markerNo, deltaBeats, offset, startFrame, durFrame|
		^super.newCopyArgs(markerNo, deltaBeats, offset, startFrame, durFrame)
	}
	endFrame { ^startFrame+durFrame }
	free     { markerNo = deltaBeats = offset = startFrame = durFrame = nil }
	printOn  {|stream| stream << this.class.name   << "(" << markerNo << "," << deltaBeats << "," << offset
							  << "," << startFrame << "," << durFrame <<")" }
}

+ LNX_StrangeLoop {

	marker_initVars{
		markerSeq		= []; // marker seq for playback loop
		allMakerEvents	= []; // all makers for pRoll sequencer
		lastMarkerEvent	= []; // previous marker event for freeze memory
	}

	// sample length is updated when bpm>0 and start or end markers are changed
	marker_updateLength{
		var bpm, startRatio, endRatio, duration, length;
		var sampleIndex=p[11];

		if (sampleBank[sampleIndex].isNil) { ^this }; 		// no samples loaded in bank exception
		bpm			= sampleBank.bpm(sampleIndex);			// start pos ratio
		if (bpm<=0) { ^this }; 								// no bpm set exception
		startRatio	= sampleBank.actualStart(sampleIndex);	// start pos ratio
		endRatio    = sampleBank.actualEnd  (sampleIndex);	// end   pos ratio
		duration    = sampleBank.duration   (sampleIndex);	// dur in sec(s)
		length      = (endRatio - startRatio) * duration * bpm / 60 * 8; // formula for length of loops in beats
		length      = length.round(1).asInt;				// do we want to quant this more than 1?
		sampleBank.length_(sampleIndex,length);				// set length
	}

	// make the basic playback sequence and all marker events
	marker_makeSeq{
		var startRatio, endRatio, durRatio, length, length3;
		var workingMarkers, workingDurs, numFrames;
		var sampleIndex=p[11];

		if (sampleBank[sampleIndex].isNil) { ^this }; 		// no samples loaded in bank exception

		startRatio	= sampleBank.actualStart(sampleIndex);	// start pos ratio
		endRatio    = sampleBank.actualEnd  (sampleIndex);	// end   pos ratio
		durRatio    = endRatio - startRatio;				// dur ratio
		length 		= sampleBank.length(sampleIndex).asInt;	// length of loop in beats
		length3		= length * 3;							// this is length of loop in beats on clock3
		numFrames	= sampleBank.numFrames(sampleIndex);	// total number of frames in sample
		markerSeq	= nil ! length3; // now i need to put the markers into this seq, clock dur split up into beats.

		allMakerEvents = [];

		workingMarkers = sampleBank.workingMarkers(sampleIndex); // ratio of buf len
		workingDurs    = sampleBank.workingDurs   (sampleIndex); // ratio of buf len

		// into seq goes:  offset start in sec, startFrame, durFrame
		workingMarkers.do{|marker,markerNo|
			// when will it happen from 0 @ start & length3
			var deltaBeats  = ((marker - startRatio) / durRatio * length3);
			var when        = deltaBeats.round(0).clip(0,markerSeq.size-1); // quantise
			var index  		= when.floor.asInt;					// where to put in the seq index
			var offset		= when.frac;     					// what is frac offset from beat
			var startFrame	= (marker * numFrames).asInt;		// start frame of marker
			var markerDur   = workingDurs[markerNo];			// dur of marker as ratio of loop dur
			var durFrame	= (markerDur * numFrames).asInt;    // dur of marker in frames
			var markerEvent = LNX_MarkerEvent(markerNo, index, offset, startFrame, durFrame);
			allMakerEvents 	= allMakerEvents.add(markerEvent);	// for pRoll
			markerSeq.clipPut(index, markerEvent);				// for autoSeq
		};

	}

	// import play loop seq into the piano roll
	marker_Import{
		var length, length3, sampleIndex=p[11];				// sample index
		if (sampleBank[sampleIndex].isNil) { ^this }; 		// no samples loaded in bank exception

		length  = sampleBank.length(sampleIndex).asInt;		// length of loop in beats
		length3 = length * 3;								// and 83 for midi clock in3
		models[18].valueAction_(0,nil,true);				// swap to sequencer mode
		sequencer.netClear; 								// clear the pRoll, this not networked
		sequencer.netDur_(length);							// set the lengths the same

		// for all events
		allMakerEvents.do{|marker,j|
			var nextMarker = allMakerEvents[j+1];			// the next marker so we work out duration
			var nextTime;
			var thisTime = ((marker.deltaBeats) + (marker.deltaBeats))/6; 				// this beat
			if (nextMarker.notNil) {
				nextTime = ((nextMarker.deltaBeats) + (nextMarker.deltaBeats))/6; 		// next beat is either marker
			}{
				nextTime = length;														// or the length of the loop
			};
			sequencer.addNote(marker.markerNo, thisTime, nextTime-thisTime , 0.7874);	// add to pRoll (0.7874 =100/127)
		}

	}

	// marker clock in mode
	marker_clockIn3{|instBeat3,absTime3,latency,beat3|
		var length3, markerEvent, instBeat;
		var sampleIndex=p[11];						  // sample used in bank

		if (this.isOff   ) { ^this };                 // inst is off exception
		if (p[18].isFalse) { ^this };				  // no hold exception
		if (sampleBank[sampleIndex].isNil) { ^this }; // no samples loaded in bank exception

		length3     = sampleBank.length(sampleIndex).asInt * 3; // this is length of loop in beats on clock3
		markerEvent = markerSeq.wrapAt(instBeat3); // midi in might be out of range so wrap. maybe return nil?

		// launch at start pos  [ or relaunch sample if needed** no relaunch yet]
		if (markerEvent.notNil) {
			var sample      = sampleBank[sampleIndex];				// the sample
			var rate		= (p[12]+p[13]).midiratio.round(0.0000000001).clip(0,100000);
			var amp         = 0.7874;								// 100/127 - amp in pRoll
			var clipMode    = p[14];								// clip, wrap or fold
			var bufferL		= sample.buffer.bufnum(0);          	// this only comes from LNX_BufferArray
			var bufferR		= sample.buffer.bufnum(1) ? bufferL; 	// this only comes from LNX_BufferArray

			var probability = p[15]/100;							// beat repeat
			if (p[19]==1) { probability=1 };

			// repeat mode fixed frame will only happen here
			// and if on should stop pipeIn from working

			// event repeat
			if ((probability.coin) && (lastMarkerEvent.notEmpty) && (repeatMode!=\frame)) {
				repeatMode  = \event;
				markerEvent = lastMarkerEvent.wrapAt(repeatNo);		// repeat
				repeatNo 	= repeatNo + 1;							// inc number of repeats
				rate		= (p[12]+p[13]+repeatRate).midiratio.round(0.0000000001).clip(0,100000);
				repeatRate	= repeatRate + p[16];
				amp         = (100/127) * repeatAmp;
				repeatAmp	= repeatAmp * p[17];

			}{
				if (repeatMode == \event) {
					repeatMode  = nil;
					repeatNo	= 0; // reset all vars
					repeatRate	= 0;
					repeatAmp	= 1;
				};
			};

			if (repeatMode.isNil) { lastMarkerEvent = lastMarkerEvent.insert(0,markerEvent) }; // add last event
			lastMarkerEvent = lastMarkerEvent.keep(p[20].asInt); // memory of length p[20]

			{
				this.marker_playBuffer(
					bufferL,bufferR,rate,markerEvent.startFrame,markerEvent.durFrame,1,clipMode,amp,latency);
				nil;
			}.sched(markerEvent.offset * absTime3);

		};

	}

	// sample bank has changed...so do this
	marker_update{|model|
		var sampleIndex=p[11];  // sample used in bank
		if (sampleBank[sampleIndex].isNil) { ^this }; // no samples loaded in bank exception

		if (model==\itemFunc) {
			^this
		};
		if (model==\selectFunc) {
			this.marker_makeSeq;
			^this
		};
		if ((model==\start)||(model==\end)) {
			this.marker_updateLength;
			this.marker_makeSeq;
			relaunch = true; // modelValueAction_ may do a relaunch as well but not always
			^this
		};
		if (model==\length) {
			this.marker_makeSeq;
			relaunch = true;
			^this
		};
		if (model==\markers) {
			this.marker_makeSeq;
			relaunch = true;
			^this
		};
	}

	// change playback rate
	marker_changeRate{|latency|
/*		var rate = (p[12]+p[13]).midiratio.round(0.0000000001); // this is changed by repeat *** !!!!
		(noteOnNodes++node).select(_.notNil).do{|node|
			server.sendBundle(latency +! syncDelay, [\n_set, node, \rate, rate]);
		};*/
	}

	// both midi in & pRoll seq destinations
	marker_pipeIn{|pipe|

		if (sampleBank[p[11]].isNil) { ^this }; // no sample exception

		// note OFF
		if (pipe.isNoteOff) {
			var note    = pipe.note.asInt;
			var latency = pipe.latency;
			var node    = noteOnNodes[note];
			if (node.isNil) { ^this }; // no node exception
			server.sendBundle(latency +! syncDelay, ["/n_set", node, \gate, 0]);
			noteOnNodes[note] = nil;
		};

		if (p[18].isTrue) { ^this }; // hold on exception
		if (this.isOff)   { ^this }; // instrument is off exception

		// note ON (with midi max number of usable markers is 0-127)
		if (pipe.isNoteOn ) {
			var probability;
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

			// beat repeat (event)
			if (p[19]==1) { probability = 1 } { probability = p[15]/100 };
			if ((probability.coin) && (lastMarkerEvent.notEmpty)  && (repeatMode!=\frame)) {
				repeatMode  = \event;
				markerEvent = lastMarkerEvent.wrapAt(repeatNo);	// repeat
				repeatNo 	= repeatNo + 1;						// inc number of repeats
				rate		= (p[12]+p[13]+repeatRate).midiratio.round(0.0000000001).clip(0,100000);
				repeatRate	= repeatRate + p[16];
				amp         = (vel/127) * repeatAmp;
				repeatAmp	= repeatAmp * p[17];
			}{
				if (repeatMode == \event) {
					repeatMode  = nil;
					repeatNo	= 0; // reset all vars
					repeatRate	= 0;
					repeatAmp	= 1;
				};
			};

			if (repeatMode.isNil) { lastMarkerEvent = lastMarkerEvent.insert(0,markerEvent) }; // add last event
			lastMarkerEvent = lastMarkerEvent.keep(p[20].asInt); // memory of length p[20]

			noteOnNodes[note] =
			  this.marker_playBufferMIDI(
				bufferL,bufferR,rate,markerEvent.startFrame,markerEvent.durFrame,1,clipMode,amp,latency
			);

		};

	}

	// play a buffer for play loop mode
	marker_playBuffer{|bufnumL,bufnumR,rate,startFrame,durFrame,attackLevel,clipMode,amp,latency|
		this.marker_stopBuffer(latency);
		node = server.nextNodeID;
		server.sendBundle(latency +! syncDelay,
			["/s_new", p[21].isTrue.if(\SLoopMarkerRev,\SLoopMarker), node, 0, instGroupID,
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
		server.sendBundle(latency +! syncDelay,
			["/s_new", p[21].isTrue.if(\SLoopMarkerRev,\SLoopMarker), node, 0, instGroupID,
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
			var signal, index;

			index  = Integrator.ar((rate * BufRateScale.ir(bufnumL)).asAudio);
			index  = startFrame + Select.ar(clipMode,
						[ index.clip(0,durFrame) , index.fold(0,durFrame) , index.wrap(0,durFrame) ]);

			signal = BufRd.ar(1, [bufnumL,bufnumR], index, loop:0); // might need to be leaked
			signal = signal * EnvGen.ar(Env.new([attackLevel,1,0], [0.01,0.01], [2,-2], 1),gate,amp,doneAction:2);

			DetectSilence.ar(Slope.ar(index), doneAction:2); // ends when index slope = 0
			OffsetOut.ar(outputChannels,signal);			 // now send out
			SendReply.kr(Impulse.kr(20), '/sIdx', [index], id); // send sample index back to client

		}).send(server);


		SynthDef("SLoopMarkerRev",{|outputChannels=0,bufnumL=0,bufnumR=0,rate=1,startFrame=0,durFrame=44100,
				gate=1,attackLevel=1, clipMode=0, id=0, amp=1|
			var signal, index;

			index  = Integrator.ar((rate * BufRateScale.ir(bufnumL)).asAudio);
			index  = startFrame + durFrame - Select.ar(clipMode,
						[ index.clip(0,durFrame) , index.fold(0,durFrame) , index.wrap(0,durFrame) ]);

			signal = BufRd.ar(1, [bufnumL,bufnumR], index, loop:0); // might need to be leaked
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

	// stop playing buffer (this doesn't work)
	marker_stopBuffer{|latency|
		if (node.notNil) { server.sendBundle(latency +! syncDelay, ["/n_set", node, \gate, 0]) };
		noteOnNodes.do{|node,j|
			if (node.notNil) {
				node.postln;
				server.sendBundle(latency +! syncDelay, ["/n_set", node, \gate, 0]);
				noteOnNodes[j]=nil;
			};
		}
	}

	marker_stopPlay{
		repeatMode		= nil;
		lastMarkerEvent = [];
		repeatNo        = 0;
		repeatRate 		= 0;
		repeatAmp		= 1;
	}

}
