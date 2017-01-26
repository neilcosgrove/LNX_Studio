// ************* //
// Repitch mode  //
// ************* //

+ LNX_StrangeLoop {

	// repitch mode
	pitch_clockIn3{|instBeat,absTime3,latency,beat|
		var length;
		var sampleIndex=p[11];						  // sample used in bank
		if (this.isOff) { ^this };                    // inst is off exception
		if (sampleBank[sampleIndex].isNil) { ^this }; // no samples loaded in bank exception
		beat   = instBeat/3; 					      // use inst beat at slower rate
		length = sampleBank.length(sampleIndex);      // lenth of loop in (n) beats

		// pos index (all the time for the moment)
		if (instBeat%2==0) {
			var startRatio  = sampleBank.actualStart(sampleIndex);	// start pos ratio
			var endRatio    = sampleBank.actualEnd  (sampleIndex);	// end   pos ratio
			var durRatio    = endRatio - startRatio;				// dur ratio
			var offsetRatio = durRatio * (beat%length) / length;	// offset in frames
			// is this just drawing the line?
			sampleBank.otherModels[sampleIndex][\pos].valueAction_(startRatio+offsetRatio,latency +! syncDelay);
		};

		// launch at start pos or relaunch sample if needed
		if (relaunch or:{beat % length==0}) {
			var numFrames, bufferL, bufferR, duration, rate, offset, startFrame, endFrame, durFrame, attackLevel;
			var sample      = sampleBank[sampleIndex];
			bufferL			= sample.bufnumPlayback(0);          	// this only comes from LNX_BufferArray
			bufferR			= sample.bufnumPlayback(1) ? bufferL; 	// this only comes from LNX_BufferArray
			numFrames		= sampleBank.numFrames  (sampleIndex);	// total number of frames in sample
			startFrame 		= sampleBank.actualStart(sampleIndex) * numFrames;	// start pos frame
			endFrame		= sampleBank.actualEnd  (sampleIndex) * numFrames;	// end pos frame
			durFrame        = endFrame - startFrame; 							// frames playing for
			duration		= sampleBank.duration   (sampleIndex) * (durFrame/numFrames); // playing dur in secs
			rate			= duration / ((studio.absTime)*3*length);// play back rate so it fits in (n) beats
			offset 			= durFrame * (beat % length) / length;	 // offset in frames
			attackLevel     = relaunch.if(0,1);						 // fade in if relaunched else no attack

			this.pitch_playBuffer(bufferL,bufferR,rate,startFrame+offset,durFrame,attackLevel,latency); // play sample

			relaunch= false; newBPM = false; // stop next stage from happening next time
			^this;
		};

		// change pos rate if bpm changed
		if (newBPM and:{node.notNil}) {
			var numFrames, duration, rate, startFrame, endFrame, durFrame;
			numFrames		= sampleBank.numFrames  (sampleIndex); 	// total number of frames in sample
			startFrame      = sampleBank.actualStart(sampleIndex) * numFrames;	// start pos frame
			endFrame		= sampleBank.actualEnd  (sampleIndex) * numFrames;	// end pos frame
			durFrame        = endFrame - startFrame;			 				// frames playing for
			duration		= sampleBank.duration   (sampleIndex) * (durFrame/numFrames); // playing dur in secs
			rate			= duration / ((studio.absTime)*3*length); // play back rate so it fits in (n) beats

			server.sendBundle(latency +! syncDelay,[\n_set, node, \rate, rate]); // change playback rate

			newBPM = false; // stop this from happening next time
		};

	}

	// sample bank has changed...so do this
	pitch_update{|model|
		var sampleIndex=sampleBank.selectedSampleNo;  // sample used in bank
		if (sampleBank[sampleIndex].isNil) { ^this }; // no samples loaded in bank exception
		//model.postln;
		if ((model==\start)||(model==\end)) {
			var startRatio  = sampleBank.actualStart(sampleIndex);	// start pos ratio
			var endRatio    = sampleBank.actualEnd  (sampleIndex);	// end   pos ratio
			var durRatio    = endRatio - startRatio;				// dur ratio
			var duration	= sampleBank.duration   (sampleIndex) * durRatio;
			var bpm			= sampleBank.bpm		(sampleIndex);  // use the bpm of the sample
			var length      = duration / ((60/bpm)/24*3); // to work out number of beats
			sampleBank.modelValueAction_(sampleIndex,\length,length,send:false);
			relaunch = true; // modelValueAction_ may do a relaunch as well but not always
		};
		if (model==\length) { relaunch = true };
	}

	// play a buffer
	pitch_playBuffer{|bufnumL,bufnumR,rate,startFrame,durFrame,attackLevel,latency|

		if (node.notNil) { server.sendBundle(latency +! syncDelay, ["/n_free", node] )};
		node = server.nextNodeID;

		server.sendBundle(latency +! syncDelay, ["/s_new", \SLoopRepitch, node, 0, instGroupID,
			\outputChannels, this.instGroupChannel,
			\bufnumL,bufnumL,
			\bufnumR,bufnumR,
			\rate,rate,
			\startFrame,startFrame,
			\durFrame:durFrame,
			\attackLevel:attackLevel
		]);

	}

	*pitch_initUGens{|server|

		SynthDef("SLoopRepitch",{|outputChannels=0,bufnumL=0,bufnumR=0,rate=1,startFrame=0,durFrame=44100,
				gate=1,attackLevel=1|

			var index  = startFrame + Integrator.ar((rate * BufRateScale.ir(bufnumL)).asAudio).clip(0,durFrame);
			var signal = BufRd.ar(1, [bufnumL,bufnumR], index ,loop:0); // mono, might need to be leaked
			signal = signal * EnvGen.ar(Env.new([attackLevel,1,0], [0.01,0.01], [2,-2], 1),gate,doneAction:2);

			DetectSilence.ar(Slope.ar(index), doneAction:2); // ends when index slope = 0
			OffsetOut.ar(outputChannels,signal);			 // now send out

		}).send(server);

	}

	// stop playing buffer
	pitch_stopBuffer{|latency|
		if (node.notNil) {
			server.sendBundle(latency +! syncDelay, ["/n_set", node, \gate, 0])
			//server.sendBundle(latency +! syncDelay, ["/n_free", node] )
		};
	}

}