// ************ //
// Record mode  //
// ************ //

+ LNX_StrangeLoop {


	/////////////////////////////////////////////////////////////////////////////////////////////

	// make a new buffer
	guiNewBuffer{
		var numFrames, length = (sampleBankGUI[\length].value); // length is from gui widget
		if (length<=0) { length = 32 };							// if zero use 32
		numFrames = length * 3 * (studio.absTime) * 44100;		// work that out in frames
		sampleBank.guiNewBuffer(numFrames, length:length);		// make a new buffer in the bank
	}

	// recording ///////////////////////////////////////////////////////////////////////////////////////////

	guiRecord{
		var sample, bufferL, bufferR, numFrames,  startFrame, endFrame, durFrame;
		var sampleIndex = p[11];					  	// sample used in bank
		if (sampleBank[sampleIndex].isNil) { gui[\record].value_(0); ^this };	// no samples loaded in bank exception

		sample		= sampleBank[sampleIndex];							// the sample
		bufferL		= sample.buffer.bufnum(0);          				// this only comes from LNX_BufferArray
		bufferR		= sample.buffer.bufnum(1); 							// this only comes from LNX_BufferArray
		numFrames	= sampleBank.numFrames  (sampleIndex); 				// total number of frames in sample
		startFrame	= sampleBank.actualStart(sampleIndex) * numFrames;	// start pos frame
		endFrame	= sampleBank.actualEnd  (sampleIndex) * numFrames;	// end pos frame
		durFrame	= endFrame - startFrame;			 				// frames playing for

		if (bufferR.notNil) {
			var recordNode	= this.record_Buffer(0, bufferL, bufferR, 1, startFrame, durFrame);
			var node		= Node.basicNew(server, recordNode);
			var watcher		= NodeWatcher.register(node);

			var func = {|changer,message|
				if (message==\n_end) {
					node.removeDependant(func);
					"END".postln;

					// path = PathName.tmp ++ this.hash.asString;

/*
s.boot;
b = Buffer.read(s, Platform.resourceDir +/+ "sounds/a11wlk01.wav");
// same as Buffer.plot
b.loadToFloatArray(action: { arg array; a = array; {a.plot;}.defer; "done".postln;});
b.free;
*/

/*					// now save to temp so it can be loaded into lang
					sampleBank.sample(0).buffer.buffers[0].write( ("~/Desktop/"++"temp.aiff").standardizePath,
						completionMessage:{
						"SAVED".postln;

					} );*/

					sampleBank.sample(0).buffer.buffers[0].loadToFloatArray(action: { arg array;
						{array.plot;}.defer;
						"done".postln;

					});


				};
			};

			node.addDependant(func);


		};

		"Recording...".postln;

	}

	guiStopRecord{
		"Stopped...".postln;

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
			var signal = In.ar(inputChannels,2)  * (slope>0);

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
