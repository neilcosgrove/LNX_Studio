// ************ //
// Record mode  //
// ************ //

// Buffer

//************************************************************************************
//
// COPYING NEW BUFFERS ACROSS SAMPLEBANKS DELETES THE CASHE FOLDER !!!!!!!!!!!!!!!!!!!
//
//************************************************************************************

// do i need a new buffer everytime so old isn't recorded over while played and recorded

// how does this play out
//     server restart, saving songs, network


// do a wet/dry mix
// overdub or mix

// we need to swap out the mono buffers on new recording
// empty temp folder

// i can copy channels either so the only way...
// record -> stereo buffer -> save to temp -> load as 2 mono files
// after if need to save, move temp file or save again

// or generate both stereo & 2 mono buffers and record both at same time in ugen

// but what if the server restarts you loose info
// also want to avoid cpu spikes copying info
// ##### only if save do we copy to a true stereo buffer and then save!!!!
// path = PathName.tmp ++ this.hash.asString;

// also problem with 2nd new sample and correct marker playback..
/*
s.boot;
b = Buffer.read(s, Platform.resourceDir +/+ "sounds/a11wlk01.wav");
// same as Buffer.plot
b.loadToFloatArray(action: { arg array; a = array; {a.plot;}.defer; "done".postln;});
b.free;

a.a.sampleBank[0].buffer.multiChannelBuffer;
a.a.sampleBank[0].buffer.buffers;

*/
/*

Used this to transfer to Client

sampleBank.sample(0).buffer.buffers[0].loadToFloatArray(action: { arg array;
{array.plot;}.defer;
"done".postln;
});
*/

/* DONE
// DC is a problem

*/

// SEND ?

/*
SO what next? --> now saves & loads to disk via url

// stop record on url for the moment

\new saves and reloads as \url. i now can record over temp. reload as \new

when a song is saved all \new have the option to be saved as a \url \file?

when a \url is loaded can we record over it into a \new (uses temp) ?
	what options on saving here?

and what if not recorded yet so file doesn't exist yet

exclude temp from file dialog

*/

/*


/*

"Save new buffer".postln;
"===============".postln;
path.postln;
("file://" ++ path).postln;
(LNX_BufferProxy.userFolder +/+ path).postln;
(LNX_BufferProxy.userFolder +/+ path).pathExists.postln;
buffer.convertedPath.postln;
buffer.convertedPath.pathExists.postln;*/

// use moveTo

// update LNX_BufferProxy:paths and LNX_BufferArray: various
// also update source
// update gui
// also any other buffer which uses this

// fix no file on load
// what if buffer saved but song isn't? sample is missing from original as well
// or any other that refered to it

// maybe you can't save temp files as part of a song?

// make a dialog here.

// WHEN A SONG IS SAVED and any temp files are left, they are auto saved

*/


+ LNX_StrangeLoop {

	/////////////////////////////////////////////////////////////////////////////////////////////

	// make a new buffer
	guiNewBuffer{
		var numFrames;
		var sampleRate = studio.server.sampleRate;
		var length     = (sampleBankGUI[\length].value); 					// length is from gui widget

		if (length<=0) { length = 64 };										// if zero use 64
		numFrames = length * 3 * (studio.absTime) * (sampleRate);			// work that out in frames
		sampleBank.guiNewBuffer(numFrames, 2, sampleRate, length:length);	// make a new buffer in the bank
	}

	// who will this work?
	guiSaveBuffer{
		var guiTextField, index=p[11], buffer = sampleBank[index];

		if (buffer.isNil) { "No buffer to save".warn; ^this };										// no buffer exception
		if (buffer.source==\url) { "Sample is already saved as a local file".warn; ^this };         // already saved exception
		if (buffer.convertedPath.pathExists!=\file) { "Temporary file doesn't exist".warn; ^this }; // no file exception

		if (buffer.source==\new) {
			var window, scrollView, filename;

			var path = "LNX_Songs" +/+ (studio.name) +/+ (this.instNo+1) ++ "." ++ (this.name)
						++ "(" ++ (p[11]+1) ++ ")" + (Date.getDate.stamp) ++ ".aiff"; 			// sugggested name

			path = path.replace(":",""); // remove any :

			filename = path.removeExtension;

			window = MVC_ModalWindow(this.window, 600@90, (
				background:		Color.new255(122,132,132),
				border2:		Color.new255(122,132,132)/2,
				border1:		Color.black
			));
			scrollView = window.scrollView;

			// text field for the instrument / filename
			guiTextField = MVC_Text(scrollView, Rect(10,21,548,16), gui[\textTheme1])
				.label_("Save buffer as...")
				.string_(filename)
				.stringAction_{|me,string|
				string=string.replace(":",""); // don't allow user to type :
					me.string_(string);
				}
				.enterKeyAction_{|me,filename| this.updateTempToLocalFile(index, buffer, filename, {window.close})}
				.focus.startEditing;

			// Cancel
			MVC_OnOffView(scrollView,Rect(464, 44, 55, 20),"Cancel", gui[\onOffTheme1])
				.action_{ window.close };

			// Ok
			MVC_OnOffView(scrollView,Rect(524, 44, 50, 20),"Ok", gui[\onOffTheme1])
				.action_{ this.updateTempToLocalFile(index, buffer, guiTextField.string, { window.close }) };

		};
	}

	// the copy has already happened by here
	updateTempToLocalFile{|index, buffer, path, func|
		var url= "file://" ++ path ++ ".aiff";
		path = (LNX_BufferProxy.userFolder +/+ path ++ ".aiff");

		if (PathName(path).fileNameWithoutExtension.size==0){         "No Filename".warn; ^this }; // no name exception
		if (path.pathExists) 								{ "File already exists".warn; ^this }; // already exists exception
		if (path.contains("//")) 							{        "Bad filename".warn; ^this }; // bad name exception

		// we could use sfcovert here instead of copyTo and use many different sound formats eg.mp3
		if (buffer.convertedPath.copyFile(path, silent:true)){
			buffer.convertedPath.removeFile(toTrash:false, ask:false, silent:true);
			func.value;
			buffer.updateTempToLocalFile(path,url); // filename is now new local filename

			// also update gui

		}{
			"Bad filename".warn;
		};

	}

	// recording ///////////////////////////////////////////////////////////////////////////////////////////

	// gui has pressed record
	guiRecord{
		// i need to make multi if doesn't already exist
		// what about samples for net or local ?

		if (sampleBank[p[11]].isNil) {
			this.guiNewBuffer
		}{
			if (sampleBank[p[11]].source==\url) {
				"Sample is a url and recording not supported yet".warn;
				{ gui[\record].value_(0).color_(\on,Color(1,0.5,0.5)) }.deferIfNeeded;
				^this;
			};
			sampleBank[p[11]].nextRecord(studio.server,{});
		};
		cueRecord = true;
	}

	// from clockIn3
	record_ClockIn3{|instBeat,absTime3,latency,beat|
		var length;

		if (cueRecord.not) {^this};					// recording not cued exception

		length = (sampleBankGUI[\length].value);	// length is from gui widget
		if (length<=0) { length = 64 };				// else 64

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
		var sample, bufferL, bufferR, multiBuffer, numFrames,  startFrame, endFrame, durFrame;
		var sampleIndex = p[11];					  	// sample used in bank
		if (sampleBank[sampleIndex].isNil) {
			{gui[\record].value_(0)}.deferIfNeeded;
			^this
		};	// no samples loaded in bank exception

		sample		= sampleBank[sampleIndex];							// the sample
		bufferL		= sample.recordBuffers[0].bufnum;          			// this only comes from LNX_BufferArray
		bufferR		= sample.recordBuffers[1].bufnum; 					// this only comes from LNX_BufferArray
		multiBuffer = sample.multiChannelBuffer.bufnum;					// multi channel buffer only used to save & > SClang
		numFrames	= sampleBank.numFrames  (sampleIndex); 				// total number of frames in sample
		startFrame	= sampleBank.actualStart(sampleIndex) * numFrames;	// start pos frame
		endFrame	= sampleBank.actualEnd  (sampleIndex) * numFrames;	// end pos frame
		durFrame	= endFrame - startFrame;			 				// frames playing for

		if (bufferR.notNil) {
			var path;
			var recordNode	= this.record_Buffer(0, bufferL, bufferR, multiBuffer, 1, startFrame, durFrame, latency);
			var node		= Node.basicNew(server, recordNode);
			var watcher		= NodeWatcher.register(node);
			var func 		= {|changer,message|
				if (message==\n_end) {
					node.removeDependant(func);
					//"END".postln;

					{ gui[\record].value_(0).color_(\on,Color(50/77,61/77,1)) }.deferIfNeeded;

					// now save to temp so it can be loaded into lang
					path = (LNX_BufferProxy.tempFolder) +/+ (sample.path);

					sample.multiChannelBuffer.write(path.standardizePath, completionMessage:{
						//"SAVED".postln;
						{
							sample.updateSampleData(path);
							{sampleBankGUI.sampleView.refresh}.defer(0.25);
						}.defer(0.25)
					});

					sample.cleanupRecord(latency); // update & free buffers no longer used

					// update synth with new buffer numbers
					this.marker_changeBuffers( sample.bufnum(0), sample.bufnum(1) ,latency);

				};
			};

			node.addDependant(func);

			{ gui[\record].color_(\on,Color(1,0.25,0.25)) }.deferIfNeeded;

		};

		//"Recording...".postln;

	}

	guiStopRecord{
		//"Stopped...".postln;
		cueRecord = false;
		{ gui[\record].color_(\on,Color(1,0.5,0.5)) }.deferIfNeeded;
	}

	// play a buffer for sequencer mode
	record_Buffer{|inputChannels, bufnumL, bufnumR, multiBuffer, rate, startFrame, durFrame, latency|
		var node = server.nextNodeID;
		server.sendBundle(latency +! syncDelay,
			["/s_new", \SLoopRecordStereo, node, 0, studio.groups[\channelOut].nodeID,
			\inputChannels,	inputChannels,
			\id,			id,
			\bufnumL,		bufnumL,
			\bufnumR,		bufnumR,
			\multiBuffer,	multiBuffer,
			\startFrame,	startFrame,
			\durFrame:		durFrame,
			\rate,			rate,
		]);
		^node; // for stop record
	}

	*record_initUGens{|server|
		// we can add to side group to record
		SynthDef("SLoopRecordStereo",{|inputChannels=0, bufnumL=0, bufnumR=1, multiBuffer=1,
										id=0, rate=1, gate=1, startFrame=0, durFrame=44100|

			var index  = startFrame + Integrator.ar((rate * BufRateScale.ir(bufnumL)).asAudio).clip(0,durFrame);
			var slope  = Slope.ar(index);
			var signal = In.ar(inputChannels,2) * (slope>0);

			BufWr.ar(signal[0], bufnumL, index, loop:0);	// left
			BufWr.ar(signal[1], bufnumR, index, loop:0);	// right
			BufWr.ar(signal, multiBuffer, index, loop:0);	// and stereo

			DetectSilence.ar(slope, doneAction:2); // ends when index slope = 0

		}).send(server);
	}

}
