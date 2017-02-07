
// loads each channel into adjcent buffers on the server ////////////////////////////////

LNX_BufferArray {

	classvar <emptyMono, <emptyStereo;

	var <>verbose=false;
	var <buffers, <numChannels, <numFrames, <sampleRate, <duration, <sampleData;
	var <multiChannelBuffer, <recordBuffers, <path;
	var <playbackBuffers;

	//

	*serverReboot{|server|
		var bufnum  = server.bufferAllocator.alloc(2); // make 2 buffers
		emptyMono   = Buffer.alloc(server, 1, 1, {}, bufnum);
		emptyStereo = Buffer.alloc(server, 1, 2, {}, bufnum+1);
	}

	// missing ///////////////////////////////////////////////////////////////////////////////

	// to include numFrames, numChannels & sampleRate so it can be saved in new file format

	*missing {|server,path,action| ^super.new.initMissing(server,path,action) }

	initMissing{|server, argPath, action|
		var soundFile = SoundFile();

		soundFile.numChannels_(2);
		soundFile.sampleRate_(41100);

		path	    = argPath;
		numFrames   = 1;
		numChannels = 2;
		sampleRate  = 41100;
		duration	= 1/41100;
		sampleData  = FloatArray.fill(1,0); // causes lates with large samples

		// easy way to reduce num of samples needed.

		buffers			= [emptyMono, emptyMono];
		playbackBuffers = [emptyMono, emptyMono];
		recordBuffers	= [];

		{action.value(this)}.defer(0.01)

	}

	// new empty for support with sLoop ////////////////////////////////////////////////

	*new{|server, path, numFrames, numChannels, sampleRate, action|
		^super.new.initNew(server, path, numFrames, numChannels, sampleRate, action)
	}

	initNew{|server, argPath, argFrames, argChannels, argSampleRate, action|
		var bufnum, done;
		var soundFile = SoundFile();

		soundFile.numChannels_(argChannels);
		soundFile.sampleRate_(argSampleRate);

		path	    = argPath;
		numFrames   = argFrames;
		numChannels = soundFile.numChannels;
		sampleRate  = soundFile.sampleRate;
		duration	= argFrames / argSampleRate;
		sampleData  = FloatArray.fill(numFrames*numChannels,0); // causes lates with large samples

		done   = 1 ! (numChannels+1); // reverse of normal 0 = done

		// easy way to reduce num of samples needed.
		playbackBuffers = [emptyMono, emptyMono];

		// maybe action should only call after all have loaded
		bufnum = server.bufferAllocator.alloc(numChannels); // make sure buffers are adj
		buffers = [];
		numChannels.do{|i|
			buffers = buffers.add(
				Buffer.alloc(server, numFrames, 1,{|buf|
					done[i] = 0; // when sum of done is zero, all buffers have been allocated
					if (done.sum==0) {  {action.value(this)}.defer(0.01) }; // fails without defer
				}, bufnum+i);
			)
		};

		recordBuffers = buffers; // 1st time round the same

		// for saving and loading only. I ONLY EVER NEED 1 OF THESE so make in initNew
		bufnum = server.bufferAllocator.alloc(1); // make sure buffers are adj
		multiChannelBuffer = Buffer.alloc(server, numFrames, numChannels ,{|buf|
			done[numChannels] = 0; // when sum of done is zero, all buffers have been allocated
			if (done.sum==0) {  {action.value(this)}.defer(0.01) }; // fails without defer
		}, bufnum);

	}

	// make a url into a temp
	makeTemp{|server,argPath|
		var bufnum;
		path = argPath;
		// maybe action should only call after all have loaded
		bufnum = server.bufferAllocator.alloc(numChannels); // make sure buffers are adj
		recordBuffers = [];
		numChannels.do{|i|
			recordBuffers = buffers.add(
				Buffer.alloc(server, numFrames, 1,{|buf|

				}, bufnum+i);
			)
		};
		// for saving and loading only. I ONLY EVER NEED 1 OF THESE so make in initNew
		bufnum = server.bufferAllocator.alloc(1); // make sure buffers are adj
		multiChannelBuffer = Buffer.alloc(server, numFrames, numChannels ,{|buf|

		}, bufnum);
	}

	// alloc buffers for recording
	nextRecord{|server, action|
		var bufnum = server.bufferAllocator.alloc(numChannels+1); // make sure buffers are adj
		var done   = 1 ! numChannels; // reverse of normal 0 = done

		// for recording only
		recordBuffers = [];
		numChannels.do{|i|
			recordBuffers = recordBuffers.add(
				Buffer.alloc(server, numFrames, 1,{|buf|
					done[i] = 0; // when sum of done is zero, all buffers have been allocated
					if (done.sum==0) {  {action.value(this)}.defer(0.01) }; // fails without defer
				}, bufnum+i);
			)
		};
	}

	// finish by swapping out and freeing old buffers
	cleanupRecord{|latency|
		if (buffers != recordBuffers) { buffers.do{|b| b.freeWithLatency(latency)}; "Free".postln };
		buffers = recordBuffers;
		playbackBuffers = recordBuffers;
		recordBuffers = nil;
	}

	// update sampleData with the soundfile at path
	updateSampleData{|path|
		var soundFile = SoundFile();
		soundFile.openRead(path.standardizePath); // why does only standardizePath work?
		sampleData  = FloatArray.fill(numFrames*numChannels,0); // causes lates with large samples
		soundFile.readData(sampleData); // fast but causes lates
	}

	// update buffer to a local file with the filename path //////////////////////////////////////

	updateTempToLocalFile{|argPath|
		path = argPath;
	}

	// new from file /////////////////////////////////////////////////////////////////////////////

	*read {|server,path,action| ^super.new.init(server,path,action) }

	init{ |server, path, action|

		var bufnum;
		var soundFile = SoundFile();
		var done;

		soundFile.openRead(path);
		numChannels = soundFile.numChannels;
		numFrames   = soundFile.numFrames;
		sampleRate  = soundFile.sampleRate;
		duration    = soundFile.duration;
		//sampleData  = FloatArray[0]; // temp

		sampleData  = FloatArray.fill(numFrames*numChannels,0); // causes lates with large samples

		// fast but causes lates
		soundFile.readData(sampleData);

//		// slow causes less lates
//		soundFile.readByChunks(action:{|data|
//			//sampleData=data;
//			if (verbose) {path.postln};
//			soundFile.close;
//		}, floatArray:sampleData);

		bufnum = server.bufferAllocator.alloc(numChannels); // make sure buffers are adj

		done = 1 ! numChannels; // reverse of normal 0 = done

		// maybe action should only call after all have loaded
		numChannels.do{|i|
			buffers = buffers.add(

				// fast but caauses lates
				Buffer.readChannel (server, path,0, -1, [i], {|buf|
					done[i] = 0; // when sum of done is zero, all buffers have loaded
					if (done.sum==0) {action.value(this)}
				}, bufnum+i );

//				// slow but causes less lates and also unpredicable read fails
//				Buffer.readChannelByChunk (server, path,0, -1, 0, false, [i], bufnum+i , {|buf|
//					done[i] = 0; // when sum of done is zero, all buffers have loaded
//					if (done.sum==0) {action.value(this)}
//				});


			)
		};

		playbackBuffers = buffers;

	}

	// play this buffer
	play{|loop = false, mul = 1, start=0, end=1|

	 	if (buffers.notNil) {
		 	if (numChannels==1) {
		 		^buffers[0].playMono(loop,mul,start,end);
		 	}{
			 	^buffers[0].playStereo(loop,mul,start,end);
		 	}
		 }{ ^nil }
	}

	getWithRef{|idx,action,ref1,ref2,channel|
		buffers[channel].getWithRef(idx,action,ref1,ref2)
	}

	bufnum {|i=0| if (buffers[i].notNil) {^buffers[i].bufnum }{^nil} }

	bufnumPlayback{|i=0| if (playbackBuffers[i].notNil) {^playbackBuffers[i].bufnum }{^nil} }

	free{
		buffers.asSet.do(_.free);
		multiChannelBuffer.free;
		sampleData = nil;
	}

}

////////////////////////////////////////////////////////////////////////////////////////////

+ Buffer {

	// adjust this so mono files play in stereo, and new stereo array

	playPan { arg loop = false, mul = 1, pan=0;
		^{ var player;
			player = PlayBuf.ar(numChannels,bufnum,BufRateScale.kr(bufnum),
				loop: loop.binaryValue);
			loop.not.if(FreeSelfWhenDone.kr(player));
			Pan2.ar(player * mul, pan);
		}.play(server,fadeTime:0);
	}

	playMono {|loop = false, amp = 1, start=0, end=1|
		if (loop) {
			^{|mul=1, start=0, end=1|
				var player;
				var index  = Integrator.ar(BufRateScale.ir(bufnum).asAudio);
				index = (start*numFrames)  + index.wrap(0, (end-start)*numFrames); // this does the looping
				player = BufRd.ar(1, [bufnum,bufnum], index, loop:0); // mono, might need to be leaked
				//loop.not.if(FreeSelfWhenDone.kr(player));
				player * mul;
			}.play(server,fadeTime:0, args:[\mul:amp,\start:start,\end:end]);
		}{
			^{|mul=1, start=0, end=1|
				var player;
				var index  = Integrator.ar(BufRateScale.ir(bufnum).asAudio);
				index = (start*numFrames)  + index.clip(0, (end-start)*numFrames); // this does the looping
				player = BufRd.ar(1, [bufnum,bufnum], index, loop:0); // mono, might need to be leaked
				loop.not.if(FreeSelfWhenDone.kr(player));
				player * mul;
			}.play(server,fadeTime:0, args:[\mul:amp,\start:start,\end:end]);
		};
	}

	playStereo {|loop = false, amp = 1, start=0, end=1|
		if (loop) {
			^{|mul=1, start=0, end=1|
				var player;
				var index  = Integrator.ar(BufRateScale.ir(bufnum).asAudio);
				index = (start*numFrames)  + index.wrap(0, (end-start)*numFrames); // this does the looping
				player = BufRd.ar(1, [bufnum,bufnum+1], index, loop:0); // mono, might need to be leaked
				//loop.not.if(FreeSelfWhenDone.kr(player));
				player * mul;
			}.play(server,fadeTime:0, args:[\mul:amp,\start:start,\end:end]);
		}{
			^{|mul=1, start=0, end=1|
				var player;
				var index  = Integrator.ar(BufRateScale.ir(bufnum).asAudio);
				index = (start*numFrames)  + index.clip(0, (end-start)*numFrames); // this does the looping
				player = BufRd.ar(1, [bufnum,bufnum+1], index, loop:0); // mono, might need to be leaked
				loop.not.if(FreeSelfWhenDone.kr(player));
				player * mul;
			}.play(server,fadeTime:0, args:[\mul:amp,\start:start,\end:end]);
		};
	}

}


/*

f = SoundFile.openRead("/Users/neilcosgrove/Desktop/acid tracks/walking from hassocks.aiff");
f.readByChunks(action: { |data|
	d = data.postln;
	f.close;
});

1048576/2
524288

*/

+ SoundFile {

	// by hjh. Thankyou, it helps a little.

	readByChunks { |startFrame = 0, numFrames = -1, chunkSize = 1048576,
					wait = 0.02, action, updateAction, floatArray|

		var data, chunk, readFrames = 0, readSize;

		if(numFrames < 0) { numFrames = this.numFrames - startFrame };
		chunkSize = chunkSize.round(this.numChannels).asInteger;
		this.seek(startFrame, 0);
		data = floatArray ? FloatArray.newClear(numFrames * this.numChannels);

		{
			while {
				readFrames < numFrames and: { // do nothing when we've read enough frames
					readSize = min(chunkSize, (numFrames - readFrames) * this.numChannels);
					chunk = FloatArray.newClear(readSize);
					this.readData(chunk);
					if(chunk.size > 0) { // at EOF, readData empties the array
						data.overWrite(chunk, readFrames * this.numChannels);
						readFrames = readFrames + (chunk.size / this.numChannels);
						true
					}{
						false
					} // exit at EOF
				}
			} {
				updateAction.value(data);
				wait.wait;
			};
			action.value(data);
		}.fork(AppClock);

	}

}

/*

Buffer.readChannelByChunk(s,"/Users/neilcosgrove/Desktop/acid tracks/walking from hassocks.aiff", 0, -1, 0, false, [0,1], nil, {|buf| b=buf; b.play; "done".postln}, 2**20);

*/

+ Buffer {

	*readChannelByChunk{|server, path, startFrame = 0, numFrames = -1, bufStartFrame = 0,
					leaveOpen = false, channels, bufnum, action, chunkSize = 1048576|

		var sf, numCh, buf, incr = 0;

		sf = SoundFile.openRead(path);

		if(sf.isOpen) {

			numCh = sf.numChannels;
			if(numFrames < 0) {
				numFrames = sf.numFrames - startFrame;
			}{
				numFrames = min(numFrames, sf.numFrames - startFrame);
			};

			sf.close;

			buf = Buffer.alloc(server, numFrames, channels.size,nil,bufnum);

			{
				server.sync;
				(numFrames / chunkSize).roundUp.do{
					buf.readChannel(path,
						startFrame + incr,
						chunkSize.clip(0,numFrames-startFrame-incr),
						startFrame + incr,
						leaveOpen, channels);
					server.sync;
					0.02.wait;
					incr = incr + chunkSize;
				};
				action.value(buf);
			}.fork(AppClock);

			^buf;

		}

	}

}

//

// so buffer still availble until swapped out
+ Buffer{
	freeWithLatency{|latency,completionMessage|
		server.listSendBundle(latency, [this.freeMsg(completionMessage)] )
	}
}


