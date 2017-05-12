// ************ //
// Record mode  //
// ************ //

/*

// stop record on url for the moment

\new saves and reloads as \url. i now can record over temp. reload as \new
when a song is saved all \new have the option to be saved as a \url \file?
when a \url is loaded can we record over it into a \new (uses temp) ?
	what options on saving here?
and what if not recorded yet so file doesn't exist yet

// update LNX_BufferProxy:paths and LNX_BufferArray: various
// also update source
// update gui
// also any other buffer which uses this

Possible sources..
0 - Master
1+  i.mixerInstruments.collect{|i| [i.id, i.instNo, i.name, i.instGroupChannel] }
1-  LNX_AudioDevices.inputMenuList
*/

+ LNX_StrangeLoop {

	// should be in sampleBank ??

	// used in discard when saving
	revertTempToNew{|index|
		sampleBank.revertTempToNew(index);
	}

	/////////////////////////////////////////////////////////////////////////////////////////////

	// make a new buffer
	guiNewBuffer{
		var numFrames, length;
		var sampleRate = studio.server.sampleRate;

		if (gui[\length].view.keyString.notNil) {			// try and get length from keyString 1st
			length = gui[\length].view.keyString.interpret; // interpret keyString to get a value
			models[34].valueAction_(length);				// and use this in the model now
		}{
			length = (models[34].value); 					// length is from model
		};

		if (length<=0) { length = 64 };										// if zero use 64
		numFrames = length * 3 * (studio.absTime) * (sampleRate);			// work that out in frames
		sampleBank.guiNewBuffer(numFrames, 2, sampleRate, length:length);	// make a new buffer in the bank
	}

	// gui save button pressed
	guiSaveBuffer{
		var guiTextField, index=p[11], buffer = sampleBank[index];

		if (studio.saveBuffersWindow.notNil) {^this}; // window singleton already open exception
		if (buffer.isNil) { "No buffer to save".warn; ^this };								// no buffer exception
		if (buffer.source==\url) { "Sample is already saved as a local file".warn; ^this };	// already saved exception
		if (buffer.convertedPath.pathExists!=\file)
											{ "Temporary file doesn't exist".warn; ^this }; // no file exception

		if ((buffer.source==\new)||(buffer.source==\temp)) { 								// why new here ???
			var scrollView, filename;
			var songName = (studio.name.size==0).if("LNX_Studio",studio.name);
			var path= "LNX_Songs" +/+ songName +/+ (this.instNo+1) ++ "." ++ (this.name)
						++ "(" ++ (p[11]+1) ++ ")" + (Date.getDate.stamp) ++ ".aiff"; 		// sugggested name

			path = path.replace(":",""); // remove any :

			filename = path.removeExtension;

			studio.saveBuffersWindow = MVC_ModalWindow(this.window, 600@90, (
				background:		Color.new255(122,132,132),
				border2:		Color.new255(122,132,132)/2,
				border1:		Color.black
			));
			studio.saveBuffersWindow.onClose_{ studio.saveBuffersWindow = nil };

			scrollView = studio.saveBuffersWindow.scrollView;

			// text field for the instrument / filename
			guiTextField = MVC_Text(scrollView, Rect(10,21,548,16), gui[\textTheme1])
				.label_("Save buffer as...")
				.string_(filename)
				.stringAction_{|me,string|
					string=string.replace(":",""); // don't allow user to type :
					me.string_(string);
				}
				.enterKeyAction_{|me,filename| this.updateTempToLocalFile(index, buffer, filename, {
					studio.saveBuffersWindow.close;
				})}
				.focus.startEditing;

			// Cancel
			MVC_OnOffView(scrollView,Rect(464, 44, 55, 20),"Cancel", gui[\onOffTheme1])
				.action_{
					studio.saveBuffersWindow.close;
				};

			// Ok
			MVC_OnOffView(scrollView,Rect(524, 44, 50, 20),"Ok", gui[\onOffTheme1])
				.action_{ this.updateTempToLocalFile(index, buffer, guiTextField.string, {
					studio.saveBuffersWindow.close;
				}) };

		};
	}

	// the copy has already happened by here

	// should be in sampleBank ??

	updateTempToLocalFile{|index, buffer, path, func|
		var url  = "file://" ++ path ++ ".aiff";
		var name = path.basename;

		path = (LNX_BufferProxy.userFolder +/+ path ++ ".aiff");

		if (PathName(path)
			.fileNameWithoutExtension.size==0)	{         "No Filename".warn; ^this }; // no name exception
		if (path.pathExists) 					{ "File already exists".warn; ^this }; // already exists exception
		if (path.contains("//")) 				{        "Bad filename".warn; ^this }; // bad name exception

		// we could use sfcovert here instead of copyTo and use many different sound formats eg.mp3
		if (buffer.convertedPath.copyFile(path, silent:true)){
			buffer.convertedPath.removeFile(toTrash:false, ask:false, silent:true);
			func.value;								// closes window
			buffer.updateTempToLocalFile(path,url); // filename is now new local filename
			sampleBank.name_(index, name);			// update name in sampleBank
			sampleBankGUI[\path].string_(url);
			sampleBankGUI[\sampleView].refresh;
			// also update gui
		}{
			"Bad filename".warn;
		};

		// this doesn't work for some reason?
		// buffer.convertedPath.afconvert({"made mp3".postln}, "MPG3", ".mp3");

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

	// record tigger from clockIn3 (starts record)
	record_ClockIn3{|instBeat,absTime,latency,beat|
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

	// record is go...
	record{|latency|
		var sample, bufferL, bufferR, multiBuffer, numFrames,  startFrame, endFrame, durFrame, recordIndex, recordSource;
		var playBufferL, playBufferR, overdub, recordLevel;
		var sampleIndex = p[11];					  					// sample used in bank
		if (sampleBank[sampleIndex].isNil) {
			{gui[\record].value_(0)}.deferIfNeeded;
			^this														// no samples loaded in bank exception
		};

		recordIndex = p[35].asInt.clip((LNX_AudioDevices.numInputBusChannels/2*3).neg, studio.insts.mixerInstruments.size);
		if (recordIndex!=p[35]) { recordIndex = 0 }; 					// force master if bus doesn't exist

		case
			{recordIndex==0} { recordSource = [0,1] } 					// master out L&R
			{recordIndex> 0} { recordSource = [0,1] + studio.insts.mixerInstruments[recordIndex-1].instGroupChannel } // inst
			{recordIndex< 0} {
				var b = LNX_AudioDevices.firstInputBus;					// 1st audio in bus
				var i = (recordIndex.abs - 1).div(3);					// which set of busues
				var j = (recordIndex.abs - 1) % 3;						// L&R, L or R
				switch (j.asInt)
					{0}{ recordSource = [b+(i*2)  ,b+(i*2)+1] } 		// Left & Right
					{1}{ recordSource = [b+(i*2)  ,b+(i*2)  ] } 		// Left
					{2}{ recordSource = [b+(i*2)+1,b+(i*2)+1] };		// Right
			};

		sample		= sampleBank[sampleIndex];							// the sample
		bufferL		= sample.recordBuffers[0].bufnum;          			// this only comes from LNX_BufferArray
		bufferR		= sample.recordBuffers[1].bufnum; 					// this only comes from LNX_BufferArray
		playBufferL	= sample.bufnumPlayback(0);          				// this only comes from LNX_BufferArray
		playBufferR	= sample.bufnumPlayback(1) ? playBufferL; 			// this only comes from LNX_BufferArray
		multiBuffer = sample.multiChannelBuffer.bufnum;					// multi channel buffer only used to save & > SClang
		numFrames	= sampleBank.numFrames  (sampleIndex); 				// total number of frames in sample
		startFrame	= sampleBank.actualStart(sampleIndex) * numFrames;	// start pos frame
		endFrame	= sampleBank.actualEnd  (sampleIndex) * numFrames;	// end pos frame
		durFrame	= endFrame - startFrame;			 				// frames playing for
		overdub     = p[36];											// overdub onto the previous buffer
		recordLevel = p[37].dbamp;

		if (bufferR.notNil) {
			var path;
			var recordNode	= this.record_Buffer(
				recordSource, bufferL, bufferR, multiBuffer, 1, startFrame, durFrame, latency, playBufferL, playBufferR,
				overdub, recordLevel
			);
			var node		= Node.basicNew(server, recordNode);
			var watcher		= NodeWatcher.register(node);
			var func 		= {|changer,message|
				if (message==\n_end) {
					node.removeDependant(func);
					recordBus.free;

					{ gui[\record].value_(0).color_(\on,Color(50/77,61/77,1)) }.deferIfNeeded;

					// now save to temp so it can be loaded into lang
					path = (LNX_BufferProxy.tempFolder) +/+ (sample.path);

					sample.multiChannelBuffer.write(path.standardizePath, completionMessage:{
						{
							sample.updateSampleData(path);
							recordLevelModels[0].lazyValueAction_(0, nil, false);	// update input levels
							recordLevelModels[1].lazyValueAction_(0, nil, false);	// update input levels
							sampleBankGUI.sampleView.refresh; //}.defer(0.25);
						}.defer(0.25)
					});

					sample.cleanupRecord(latency); // update & free buffers no longer used

					// update synth with new buffer numbers
					this.marker_changeBuffers( sample.bufnum(0), sample.bufnum(1) , nil);
					// Surely this can be done sooner...!
				};
			};

			node.addDependant(func);
			lastRecordNode = recordNode;

			{ gui[\record].color_(\on,Color(1,0.25,0.25)) }.deferIfNeeded;

		};

	}

	guiStopRecord{
		cueRecord = false;
		{ gui[\record].color_(\on,Color(1,0.5,0.5)) }.deferIfNeeded;
		if (lastRecordNode.notNil) {
			//server.sendBundle(studio.latency +! syncDelay, [\n_free, lastRecordNode])
		};
	}

	// play a buffer for sequencer mode
	record_Buffer{|inArray, bufnumL, bufnumR, multiBuffer, rate, startFrame, durFrame, latency, playBufferL, playBufferR,
		overdub, recordLevel|
		var indexNode  = server.nextNodeID;
		var recordNode = server.nextNodeID;
		recordBus 	   = Bus.audio(server,1);

		server.sendBundle(latency +! syncDelay,
			["/s_new", \SLoopRecordStereo, recordNode, 0, studio.groups[\channelOut].nodeID,
			\leftIn,		inArray[0],
			\rightIn,		inArray[1],
			\id,			id,
			\bufnumL,		bufnumL,
			\bufnumR,		bufnumR,
			\multiBuffer,	multiBuffer,
			\startFrame,	startFrame,
			\durFrame,		durFrame,
			\indexBus,		recordBus.index,
			\rate,			rate,
			\playBufferL,	playBufferL,
			\playBufferR,	playBufferR,
			\overdub,       overdub,
			\id, 			id,
			\recordLevel, 	recordLevel,
		]);

		server.sendBundle(latency +! syncDelay,
			["/s_new", \SLoopRecordIndex, indexNode, 0, studio.groups[\channelOut].nodeID,
			\bus,			recordBus.index,
			\rate,			rate,
		]);

		^recordNode; // for stop record
	}

	*record_initUGens{|server|

		// index for recoring, OffsetOut gives us the correct index to record at
		SynthDef("SLoopRecordIndex",{|bus=0,rate=1|
			OffsetOut.ar(bus,Integrator.ar(rate.asAudio))
		}).send(server);

		// we can add to side group to record
		SynthDef("SLoopRecordStereo",{|leftIn=0, rightIn=1, bufnumL=0, bufnumR=1, multiBuffer=1, id=0, rate=1, gate=1,
								startFrame=0, durFrame=44100, indexBus=0, playBufferL=0, playBufferR=1, overdub=0, recordLevel=1|
			var indexIn  = In.ar(indexBus,1); 					// comes from SynthDef above
			var index    = startFrame + ( indexIn * BufRateScale.ir(bufnumL)         ).clip(0,durFrame); // real from in
			var refindex = Integrator.ar((rate    * BufRateScale.ir(bufnumL)).asAudio).clip(0,durFrame); // fake
			var slope    = Slope.ar(index);
			var signalIn = [In.ar(leftIn,1), In.ar(rightIn,1)] * (recordLevel.lag);	// source in
			var signal   = signalIn + (BufRd.ar(1, [playBufferL,playBufferR], index, loop:0) * (overdub.lag)); // overdub

			signal = signal * (slope>0); 						// an index with a slope <=0 turns input off
			signal = signal.clip(-1,1);							// signal needs to be clipped because it will be when saved

			BufWr.ar(signal[0],  bufnumL, index, loop:0);		// write to the left buffer
			BufWr.ar(signal[1],  bufnumR, index, loop:0);		// write to the right buffer
			BufWr.ar(signal, multiBuffer, index, loop:0);		// and stereo (the multi-channel for saving)

			DetectSilence.ar(Slope.ar(index+refindex), doneAction:3); // ends when index & fake slope = 0

			SendPeakRMS.kr(signalIn , 20, 1, "/sRecLvl", id);	// return to client

		}).send(server);

	}

	// record levels from server
	sRecLvl_{|l,r|
		recordLevelModels[0].lazyValueAction_(l, nil, false);	// update input levels
		recordLevelModels[1].lazyValueAction_(r, nil, false);	// update input levels
	}

}

+ LNX_Studio {

	reviewBufferSaves{|bufferInstBankDict,saveMode|
		if (saveBuffersWindow.isNil) { this.privateReviewBufferSaves(bufferInstBankDict,saveMode) }
	}

	privateReviewBufferSaves{|bufferInstBankDict,saveMode|
		// ( buffer: [inst, bank, indexOfBuffer] )
		var date      = Date.getDate.stamp;
		var songName  = (this.name.size==0).if("LNX_Studio",this.name);
		var startPath = "LNX_Songs" +/+ songName ++ "/";
		var info      = bufferInstBankDict.collect{|list,buffer|
			var instNo   = list[0].instNo;
			var instName = list[0].name;
			var bufNum   = list[2];
			var path     = startPath ++ (instNo+1) ++ "." ++ instName ++ "(" ++ (bufNum+1) ++ ")" + date; // ++ ".aiff";
			[
				path.asModel,		// 0: path model
				\switch.asModel,    // 1: 0=save, 1=delete
				buffer,				// 2: buffer
				list[0],			// 3: inst
				list[1],			// 4: bank
				instNo,				// 5: instNo
				instName,			// 6: instName
				bufNum,				// 7: buffer number
				date,				// 8: date
				startPath			// 9: start part of path
			]
		}.asList.sort{|a,b| (a[5]==b[5]).if{ a[7]<=b[7] }{ a[5]<=b[5] }};

		var guiTextField;
		var gui = IdentityDictionary[];
		var size = info.size;

		gui[\onOffTheme2] = ( \rounded_: true, \colors_: ( \on: Color(1,1,1,0.5), \off: Color(1,1,1,0.5)));

		gui[\onOffTheme1] = ( \rounded_: true, \colors_: ( \off: Color(0.5,1,0.5,0.66), \on: Color(1,0.5,0.5,0.66)));

		// for path
		gui[\textTheme1]= (	labelShadow_: false, shadow_:false, canEdit_:true, hasBorder_:true, enterStopsEditing_:false,
							\font_:	  Font("Helvetica", 13,true),
							\colors_:  ( \label:Color.black, \edit:Color.white, \editBackground:Color(0,0,0,0.6),
										\string:Color.black,
										\cursor:Color.orange, \focus:Color(0,0,0,0.1),  \background:Color(0,0,0,0.3) ));

		gui[\textTheme2]= (	labelShadow_: false, shadow_:false, canEdit_:false,
							\font_:	  Font("Helvetica", 13,true),
							\colors_:  ( \label:Color.black, \string:Color.black));

		saveBuffersWindow = MVC_ModalWindow(mixerWindow, 700@((size*20).clip(0,390)+99), (
			background: 	Color(59/77,59/77,59/77),
			border2: 		Color(6/11,42/83,29/65),
			border1: 		Color(3/77,1/103,0,65/77),
			menuBackground:	Color(1,1,0.9)
		));
		saveBuffersWindow.onClose_{ saveBuffersWindow = nil };

		gui[\scrollView] = saveBuffersWindow.scrollView;

		// text field for the instrument / filename
		MVC_StaticText(gui[\scrollView], Rect(10,3,655,16), gui[\textTheme2])
			.align_(\center)
			.string_("There are samples in this song thats haven't been saved yet.");

		MVC_StaticText(gui[\scrollView], Rect(10,21,655,16), gui[\textTheme2])
			.font_(Font("Helvetica", 13))
			.string_("What do you want to do with them?");

		gui[\infoScrollView] = MVC_ScrollView(gui[\scrollView], Rect(10,40,655,(size*20).clip(0,390)+3) )
			.hasBorder_(true)
			.color_(\background, Color(0.9,0.9,0.9));

		info.do{|list,i|
			// path
			MVC_Text(list[0], gui[\infoScrollView], Rect(2,2+(i*20),550,16), gui[\textTheme1]);

			// play
			MVC_OnOffView(gui[\infoScrollView], Rect(555,1+(i*20),40,18),"Play", gui[\onOffTheme2])
				.action_{ list[2].play };

			// save discard
			MVC_OnOffView(list[1], gui[\infoScrollView], Rect(598,1+(i*20),53,18), gui[\onOffTheme1] )
				.strings_(["Save","Discard"])
		};

		// Cancel
		MVC_OnOffView(gui[\scrollView], Rect(564, (size*20).clip(0,390)+50, 55, 20),"Cancel", gui[\onOffTheme2])
			.action_{ saveBuffersWindow.close };

		// Ok
		MVC_OnOffView(gui[\scrollView], Rect(624, (size*20).clip(0,390)+50, 50, 20),"Ok", gui[\onOffTheme2])
		.action_{

			if (saveMode==\saveAs) {
				Dialog.savePanel{|path|
					var baseName = path.basename.removeExtension.replace(":","");
					var newStartPath= "LNX_Songs" +/+ baseName ++ "/";
					info.do{|list|
						var inst    = list[3];
						var bank 	= list[4];
						var bufNum	= list[7];
						if (list[1].isFalse) {
							var newName = list[0].string;
							if (newName.beginsWith(startPath)) { newName = newStartPath ++ (newName.drop(startPath.size)) };
							// warning this will be asynchronous in network
							inst.updateTempToLocalFile(bufNum, bank[bufNum], newName);
						}{
							// warning this will be asynchronous in network
							inst.revertTempToNew(bufNum, bank[bufNum]);
						}
					};
					this.save(path);
				};
			};

			if (saveMode==\save) {
				info.do{|list|
					var inst    = list[3];
					var bank 	= list[4];
					var bufNum	= list[7];
					if (list[1].isFalse) {
						var newName = list[0].string;
						// warning this will be asynchronous in network
						inst.updateTempToLocalFile(bufNum, bank[bufNum], newName);
					}{
						// warning this will be asynchronous in network
						inst.revertTempToNew(bufNum, bank[bufNum]);
					}
				};
				this.save(songPath);

			};

			saveBuffersWindow.close;
		};
	}

}
