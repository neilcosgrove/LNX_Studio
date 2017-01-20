// ************ //
// Record mode  //
// ************ //

+ LNX_StrangeLoop {

	/////////////////////////////////////////////////////////////////////////////////////////////

	// make a new buffer
	guiNewBuffer{
		var numFrames;
		var sampleRate = studio.server.sampleRate;
		var length     = (sampleBankGUI[\length].value); 					// length is from gui widget

		if (length<=0) { length = 32 };										// if zero use 32
		numFrames = length * 3 * (studio.absTime) * (sampleRate);			// work that out in frames
		sampleBank.guiNewBuffer(numFrames, 2, sampleRate, length:length);	// make a new buffer in the bank
	}

	// recording ///////////////////////////////////////////////////////////////////////////////////////////

	// gui has pressed record
	guiRecord{
		if (sampleBank[p[11]].isNil) { this.guiNewBuffer };
		cueRecord = true;
	}

	// from clockIn3
	record_ClockIn3{|instBeat,absTime3,latency,beat|
		var length;
		if (cueRecord.not) {^this};					// recording not cued exception

		length = (sampleBankGUI[\length].value);	// length is from gui widget
		if (length<=0) { length = 32 };				// else 32

		if ((instBeat%(length*3))==0) {				// if at beginning of bar
			cueRecord = false;						// turn of cue
			this.record(latency);					// start recording
			^this;
		};

		// flash record button for user feedback
		if ((instBeat%(12))==0) {
			{gui[\record].color_(\on,Color(50/77,61/77,1) / (instBeat.div(12).even.if(1,2))) }.deferIfNeeded
		};

	}

	record{|latency|
		var sample, bufferL, bufferR, numFrames,  startFrame, endFrame, durFrame;
		var sampleIndex = p[11];					  	// sample used in bank
		if (sampleBank[sampleIndex].isNil) {
			{gui[\record].value_(0)}.deferIfNeeded;
			^this
		};	// no samples loaded in bank exception

		sample		= sampleBank[sampleIndex];							// the sample
		bufferL		= sample.buffer.bufnum(0);          				// this only comes from LNX_BufferArray
		bufferR		= sample.buffer.bufnum(1); 							// this only comes from LNX_BufferArray
		numFrames	= sampleBank.numFrames  (sampleIndex); 				// total number of frames in sample
		startFrame	= sampleBank.actualStart(sampleIndex) * numFrames;	// start pos frame
		endFrame	= sampleBank.actualEnd  (sampleIndex) * numFrames;	// end pos frame
		durFrame	= endFrame - startFrame;			 				// frames playing for

		if (bufferR.notNil) {
			var recordNode	= this.record_Buffer(0, bufferL, bufferR, 1, startFrame, durFrame, latency);
			var node		= Node.basicNew(server, recordNode);
			var watcher		= NodeWatcher.register(node);

			var func = {|changer,message|
				if (message==\n_end) {
					node.removeDependant(func);
					"END".postln;

					{
						gui[\record]
							.value_(0)
							.color_(\on,Color(50/77,61/77,1));
					}.deferIfNeeded;


					// ##### only if save do we copy to a true stereo buffer and then save!!!!

					// path = PathName.tmp ++ this.hash.asString;
/*
s.boot;
b = Buffer.read(s, Platform.resourceDir +/+ "sounds/a11wlk01.wav");
// same as Buffer.plot
b.loadToFloatArray(action: { arg array; a = array; {a.plot;}.defer; "done".postln;});
b.free;
*/
/*

					Used this to transfer to Client

sampleBank.sample(0).buffer.buffers[0].loadToFloatArray(action: { arg array;
{array.plot;}.defer;
"done".postln;
});
*/
					// now save to temp so it can be loaded into lang
					sample.buffer.buffers[0].write( ("~/Desktop/"++"temp.aiff").standardizePath,
						completionMessage:{
						"SAVED".postln;

					} );

				};
			};

			{
				gui[\record].color_(\on,Color(1,0.25,0.25));
			}.deferIfNeeded;

			node.addDependant(func);


		};

		"Recording...".postln;

	}

	guiStopRecord{
		"Stopped...".postln;
		cueRecord = false;
		{
			gui[\record].color_(\on,Color(1,0.5,0.5));
		}.deferIfNeeded;

	}


	// play a buffer for sequencer mode
	record_Buffer{|inputChannels, bufnumL, bufnumR, rate, startFrame, durFrame, latency|
		var node = server.nextNodeID;
		server.sendBundle(latency +! syncDelay,
			["/s_new", \SLoopRecordStereo, node, 0, studio.groups[\channelOut].nodeID,
			\inputChannels,	inputChannels,
			\id,			id,
			\bufnumL,		bufnumL,
			\bufnumR,		bufnumR,
			\startFrame,	startFrame,
			\durFrame:		durFrame,
			\rate,			rate,
		]);
		^node; // for stop record
	}


	*record_initUGens{|server|
		// we can add to side group to record
		SynthDef("SLoopRecordStereo",{|inputChannels=0, bufnumL=0, bufnumR=1, id=0, rate=1, gate=1, startFrame=0, durFrame=44100|
			var index  = startFrame + Integrator.ar((rate * BufRateScale.ir(bufnumL)).asAudio).clip(0,durFrame);
			var slope  = Slope.ar(index);
			var signal = In.ar(inputChannels,2) * (slope>0);

			BufWr.ar(signal[0], bufnumL, index, loop:0);
			BufWr.ar(signal[1], bufnumR, index, loop:0);

			DetectSilence.ar(slope, doneAction:2); // ends when index slope = 0

		}).send(server);
	}


	// *pitch_initUGens{|server|
	//
	// 	SynthDef("SLoopRepitch",{|outputChannels=0,bufnumL=0,bufnumR=0,rate=1,startFrame=0,durFrame=44100,
	// 		gate=1,attackLevel=1|
	//
	// 		var index  = startFrame + Integrator.ar((rate * BufRateScale.ir(bufnumL)).asAudio).clip(0,durFrame);
	// 		var signal = BufRd.ar(1, [bufnumL,bufnumR], index ,loop:0); // mono, might need to be leaked
	// 		signal = signal * EnvGen.ar(Env.new([attackLevel,1,0], [0.01,0.01], [2,-2], 1),gate,doneAction:2);
	//
	// 		DetectSilence.ar(Slope.ar(index), doneAction:2); // ends when index slope = 0
	// 		OffsetOut.ar(outputChannels,signal);			 // now send out
	//
	// 	}).send(server);
	//
	// }

}
