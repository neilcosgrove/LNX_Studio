
// Audio devices  ////////////////////////////////////////////////////////////////////
//
// Manage Audio IO devices.
// TO DO: Develop class to save no. in's & out's
/*
LNX_AudioDevices.inputDevices;
LNX_AudioDevices.channelHistoryIn;
LNX_AudioDevices.inputDevicesChannels;
LNX_AudioDevices.outputDevices;
LNX_AudioDevices.channelHistoryOut;
LNX_AudioDevices.outputDevicesChannels;
LNX_AudioDevices.post;
*/


LNX_AudioDevices {

	classvar	defaultOutput,			defaultInput,
				<defaultFXBusChannels   =32,
				<defaultAudioBusChannels=2048;

	classvar	<outputDevices,			<inputDevices,
				<friendlyOutputNames,	<friendlyInputNames,
				<outputDevicesChannels,	<inputDevicesChannels;

	classvar	<outputMenuList,		<inputMenuList,
				<fxMenuList,			<outputAndFXMenuList,
				<inputAndFXMenuList,  	<outputInputAndFXMenuList;

	classvar	<outDevice,				<inDevice,
				<numAudioBusChannels,
				<numOutputBusChannels=2,<numInputBusChannels=2,
				<numFXBusChannels;

	classvar	<>updateMenuAction,		<gui;

	classvar 	<>action,				<>verbose=false,
				<bootNo=0;

	classvar 	<channelHistoryIn,		<channelHistoryOut;

	// init this class
	*initClass {
		var devices;
		Class.initClassTree(LNX_File);
		gui = IdentityDictionary[];
		devices = "AudioDevices".loadPref ? ["nil","nil"];  // load preferences
		# defaultOutput, defaultInput = devices;

		Platform.case(
			\osx, {
				if (ServerOptions.prListDevices(0,1).containsString(defaultOutput).not) {
					defaultOutput = nil;
				};

				if (ServerOptions.prListDevices(1,0).containsString(defaultInput).not) {
					defaultInput = nil;
				};
		});

		numFXBusChannels = defaultFXBusChannels;
		numAudioBusChannels = defaultAudioBusChannels;
		this.updateDeviceList;
		this.devices_(defaultOutput,defaultInput,false);
		if (verbose) { this.post };
	}

	// info for app
	*defaultOutput        { ^(defaultOutput=="nil").if(nil,defaultOutput) }
	*defaultInput         { ^(defaultInput=="nil").if(nil,defaultInput) }
	*firstOutputBus       { ^0 }
	*firstInputBus        { ^numOutputBusChannels }
 	*firstFXBus           { ^numOutputBusChannels+numInputBusChannels }
	*firstPrivateBus      { ^numOutputBusChannels+numInputBusChannels+numFXBusChannels }
	*instGroupChannel {|id| ^numAudioBusChannels - 2 - (id*2) }
	*numPrivateBusChannels{ ^numAudioBusChannels-this.firstPrivateBus }
	*getOutChannelIndex{|i| if (i>=0) { ^i*2 }{ ^this.firstFXBus+(i.neg*2-2) } }
	*getFXChannelIndex {|i| ^(this.firstFXBus)+(i*2) }
	*outputChannelList    { ^outputInputAndFXMenuList } // a temp fix to remove

	*outputOnlyList    { ^outputMenuList } // a temp fix to remove

	// return the input & output devices as a list
	*devices{^[outDevice,inDevice]}

	// save devices used in preferences
	*savePref{ this.devices.savePref("AudioDevices") }

	// set both the input & output devices
	*devices_{|out,in,save=true|
		if ((outDevice!=out) or:{inDevice!=in}) {
			this.outDevice_(out);
			this.inDevice_(in);
			if (save) {this.savePref};
		};
		this.updateMenuLists;
	}

	// set the output device
	*outDevice_{|out|
		if ((out.notNil) and: {outputDevices.containsString(out)}) {
			outDevice=out;
			numOutputBusChannels=outputDevicesChannels[outputDevices.indexOfString(out)];
		}
	}

	// set the input device
	*inDevice_{|in|
		if ((in.notNil) and: {inputDevices.containsString(in)}) {
			inDevice=in;
			numInputBusChannels=inputDevicesChannels[inputDevices.indexOfString(in)];
		}
	}

	// does the output device exist?
	*outDeviceExists{|out|
		if ((out.notNil) and: {outputDevices.containsString(out)}) {^true} {^false}
	}

	// does the input device exist?
	*inDeviceExists{|in|
		if ((in.notNil) and: {inputDevices.containsString(in)}) {^true} {^false}
	}

	// refresh the list of available devices. make make a friendly name list
	// and a list of number of channels for each device
	*updateDeviceList {

		Platform.case(
		    \osx, {
				outputDevices = ["nil"] ++ ServerOptions.outDevices;
				inputDevices  = ["nil"] ++ ServerOptions.inDevices;
			},
		    \linux, {
				outputDevices = ["nil"];
				inputDevices  = ["nil"];
			},
		    \windows, {
				outputDevices = ["nil"];
				inputDevices  = ["nil"];
			}
		);

		friendlyOutputNames=[];
		outputDevices.do{|n|
			if (n=="nil") { n="Default Output Device" };
			friendlyOutputNames = friendlyOutputNames.add(n)
		};

		friendlyInputNames=[];
		inputDevices.do{|n|
			if (n=="nil") { n="Default Input Device" };
			friendlyInputNames = friendlyInputNames.add(n)
		};

		this.updateChannelHistory;

		inputDevicesChannels  = inputDevices.collect {|device| channelHistoryIn[device ] ? 2 };
		outputDevicesChannels = outputDevices.collect{|device| channelHistoryOut[device] ? 2 };
	}

	// channelHistory is a dictionary of all audio in & out devices that have been added
	// to this machine. each new device defaults to 2ch in & 2out
	// the number of channels is updated by the user as needed. this is saved in preferences
	// and everytime from then on the number of channels for that device will be used

	*updateChannelHistory{

		var save=false;

		var in  = "channelHistoryIn".loadPref;                // load in prefs
		var out = "channelHistoryOut".loadPref;               // load out prefs
		channelHistoryIn  = channelHistoryIn ? Dictionary[];  // make a dict if needed
		channelHistoryOut = channelHistoryOut ? Dictionary[]; // make a dict if needed

		// add any new devices to in
		inputDevices.do{|device|
			if (channelHistoryIn[device].isNil) {                   // if device not already found
				var findNumber = device.select(_.isDecDigit).asInt; // try and find a number
				if ((findNumber>0) && (findNumber<=128) && (findNumber.even)) {  // if its usable
					channelHistoryIn[device] = findNumber;         // use this number
				}{
					channelHistoryIn[device] = 2;                  // else use default 2
				};
				save = true;
			};

		};
		channelHistoryIn = channelHistoryIn.putPairs(in).collect(_.asInt); // add prefs-overwrites

		// add any new devices to out
		outputDevices.do{|device|
			if (channelHistoryOut[device].isNil) {                  // if device not already found
				var findNumber = device.select(_.isDecDigit).asInt; // try and find a number
				if ((findNumber>0) && (findNumber<=128) && (findNumber.even)) {  // if its usable
					channelHistoryOut[device] = findNumber;        // use this number
				}{
					channelHistoryOut[device] = 2;                 // else use default 2
				};
				save = true;
			};

		};
		channelHistoryOut = channelHistoryOut.putPairs(out).collect(_.asInt); // add prefs

		if (save) { this.saveChannelHistory }; // save this history

	}

	// save the in & out channelHistory Dictionaries in prefs
	*saveChannelHistory{
		channelHistoryIn.getPairs.savePref("channelHistoryIn");   // save channelHistoryIn
		channelHistoryOut.getPairs.savePref("channelHistoryOut"); // save channelHistoryOut
	}

	// make / update a list of all channels i.e.  ["Out: 1&2","Out: 3&4]
	*updateMenuLists{

		outputMenuList=[];
		((numOutputBusChannels/2).asInt).do{|i|
			outputMenuList=outputMenuList.add("Out: "++(i*2+1)++"&"++(i*2+2));
		};

		inputMenuList=[];
		((numInputBusChannels/2).asInt).do{|i|
			inputMenuList=inputMenuList.add("In: "++(i*2+1)++"&"++(i*2+2));
		};

		fxMenuList=[];
		((numFXBusChannels/2).asInt).do{|i|
			fxMenuList=fxMenuList.add("Bus: "++(i*2+1)++"&"++(i*2+2));
		};

		outputAndFXMenuList=outputMenuList++fxMenuList;
		inputAndFXMenuList=inputMenuList++fxMenuList;
		outputInputAndFXMenuList=outputMenuList++inputMenuList++fxMenuList;

		updateMenuAction.value(this);

	}

	//
	*changeAudioDevices{|server,devices,postBootFunc|
		var exists;
		devices=devices?[nil,nil];

		devices[0]=devices[0] ? defaultOutput;
		devices[1]=devices[1] ? defaultInput;

		if (this.outDeviceExists(devices[0]).not) {
			("Output Device"+(devices[0].asString)+
				"does not exist. Going to use the default device.").warn;
			devices[0]=defaultOutput;
		};
		if (this.inDeviceExists(devices[1]).not) {
			("Input Device"+(devices[1].asString)+
				"does not exist. Going to use the default device.").warn;
			devices[1]=defaultInput;
		};

		if (devices[0]!=server.options.outDevice) {
			if (verbose) { ("Changing audio out device to:"+devices[0]).postln;};
		};
		if (devices[1]!=server.options.inDevice) {
			if (verbose) { ("Changing audio in device to:"+devices[1]).postln;};
		};

		this.devices_(devices[0],devices[1]);

		server.quit;
		if (devices[0]=="nil") {devices[0]=nil};
		if (devices[1]=="nil") {devices[1]=nil};
		server.options.outDevice=devices[0];
		server.options.inDevice=devices[1];

		server.options.numOutputBusChannels_(numOutputBusChannels);
		server.options.numInputBusChannels_(numInputBusChannels);
		server.options.numAudioBusChannels_(numAudioBusChannels);

		this.prBoot(server);

	}

	*post {
		"------------------------------------------".postln;
		("Found" + (outputDevices.size) + "audio output devices:").postln;
		outputDevices.do { arg d, i; ("\t" + i + ": ["
			++outputDevicesChannels[i].asFormatedString(2,0)
			++" ch(s)] \""++(friendlyOutputNames[i])++"\"").postln; };
		"----------------------------".postln;
		("Found" + (inputDevices.size) + "audio input devices:").postln;
		inputDevices.do { arg d, i; ("\t" + i + ": ["
			++inputDevicesChannels[i].asFormatedString(2,0)
			++" ch(s)] \""++(friendlyInputNames[i])++"\"").postln; };
		"------------------------------------------".postln;
	}

	// gui menus ////////

	*audioHardwareGUI {|window,xy|

		var item;
		var x=xy.x;
		var y=xy.y;

		this.updateDeviceList;

		gui[\textTheme] = (
			\canEdit_ : false,
			\shadow_  : false,
			\align_   : 'left',
			\font_    : Font("Helvetica", 12),
			\colors_  : (\string: Color.black),
		);

		gui[\numberTheme]=(
			\resoultion_	 : 3,
			\font_		 : Font("Helvetica",10),
			\labelFont_	 : Font("Helvetica",12),
			\showNumberBox_: false,
			\labelShadow_  : false,
			\rounded_      : true,
			\colors_       : (	\label : Color.black,
							\background : Color(0.43,0.40,0.38),
							\backgroundDown : Color(0.1,0.1,0.1,0.85),
							\background : Color.ndcMenuBG,
							\string : Color.black,
							\focus : Color(0,0,0,0))
		);

		// in device menu
		gui[\inMenu]=MVC_PopUpMenu3(window,Rect(x+10,y+25,150,17))
			.items_(friendlyInputNames)
			.color_(\background,Color.ndcMenuBG)
			.label_("Audio In")
			.orientation_(\horiz)
			.labelShadow_(false)
			.color_(\label,Color.black)
			.action_{|me|
				this.devices_(outputDevices[gui[\outMenu].value],
							 inputDevices[gui[\inMenu].value]);
				this.setNoChannelsGUI;
				action.value(this.devices);
			}
			.value_(inputDevices.indexOfString(inDevice))
			.font_(Font("Arial", 10));

		// in channels
		gui[\inNoChannels] = MVC_NumberBox(window, Rect(x+165,y+25,25,17), gui[\numberTheme])
			.controlSpec_([2,128,\lin,2,2])
			.label_("Channels")
			.mouseWorks_(false)
			.action_{|me|
				var value=me.value.asInt;
				numInputBusChannels = value;
				channelHistoryIn[inDevice]=value;
				inputDevicesChannels[inputDevices.indexOfString(inDevice)]=value;
				this.saveChannelHistory;
				this.updateMenuLists;
				action.value(this.devices);
			};


		// out device menu
		gui[\outMenu]=MVC_PopUpMenu3(window,Rect(xy.x+10,xy.y+45,150,17))
			.items_(friendlyOutputNames)
			.color_(\background,Color.ndcMenuBG)
			.label_("Out")
			.orientation_(\horiz)
			.labelShadow_(false)
			.color_(\label,Color.black)
			.action_{|me|
				this.devices_(outputDevices[gui[\outMenu].value],
							 inputDevices[gui[\inMenu].value]);
				this.setNoChannelsGUI;
				action.value(this.devices)
			}
			.value_(outputDevices.indexOfString(outDevice))
			.font_(Font("Arial", 10));

		// out channels
		gui[\outNoChannels] = MVC_NumberBox(window, Rect(x+165,y+45,25,17), gui[\numberTheme])
			.controlSpec_([2,128,\lin,2,2])
			.rounded_(true)
			.mouseWorks_(false)
			.action_{|me|
				var value=me.value.asInt;
				numOutputBusChannels = value;
				channelHistoryOut[outDevice]=value;
				outputDevicesChannels[outputDevices.indexOfString(outDevice)]=value;
				this.saveChannelHistory;
				this.updateMenuLists;
				action.value(this.devices);
			};

		this.setNoChannelsGUI;

	}

	// set the number of channels in the gui
	*setNoChannelsGUI{
		if (gui[\inNoChannels].notNil) { gui[\inNoChannels].value_(numInputBusChannels ? 2) };
		if (gui[\outNoChannels].notNil) { gui[\outNoChannels].value_(numOutputBusChannels ? 2) };
	}

	// boot the sever
	*prBoot{|server|
		bootNo = bootNo + 1;
		"Bootng Server: ".post;
		bootNo.postln;
		server.waitForBoot({},1,true);
	}


	// try and boot the server, supply a gui if it fails after 10s
	*bootServer{|server|
		if (server.serverRunning.not) {
			server.options.numOutputBusChannels_(numOutputBusChannels);
			server.options.numInputBusChannels_ (numInputBusChannels);
			server.options.numAudioBusChannels_(numAudioBusChannels);
			{ this.prBoot(server) }.defer(0.2); // this should boot
			{ if (server.serverRunning.not) { this.failToStart(server) } }.defer(10);
		};
	}

	// server failed to start gui
	*failToStart{|server|

		var window, gui, task;

		task=Task{
			inf.do{
				3.wait;
				if (server.serverRunning) {
					window.close;
					task.stop;
				}{
					Server.killAll;
					0.5.wait;
					this.prBoot(server);
				}
			};
		}.play;

		gui=IdentityDictionary[];

		gui[\theme] = ( orientation_:\horizontal,
			rounded_:	true,
			colors_: (up:Color(0.9,0.9,0.9), down:Color(0.9,0.9,0.9)/2)
		);

		window=MVC_Window("Audio Server",
				Rect	(Window.screenBounds.left+LNX_Studio.osx,
					 Window.screenBounds.height-147-(22*3)-(LNX_Studio.thisHeight),
					 270, 170), resizable: false);
		window.color_(\background,(Color(0,1/103,3/77,65/77))).alwaysOnTop_(true);

		MVC_RoundBounds(window, Rect(11,11,window.bounds.width-22,window.bounds.height-22-1))
			.width_(6)
			.color_(\background, Color(29/65,42/83,6/11));

		// the main view
		gui[\scrollView]=MVC_CompositeView(window,
				Rect(11, 11, window.bounds.width-22, window.bounds.height-23))
			.color_(\background,Color(50/77,56/77,59/77))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);

		MVC_StaticText(gui[\scrollView],Rect(16, 6, 480, 22))
			.font_(Font("Helvetica",14,true))
			.string_("WARNING:")
			.color_(\string,Color.white);

		MVC_StaticText(gui[\scrollView],Rect(16, 30, 480, 22))
			.font_(Font("Helvetica",11))
			.shadow_(false)
			.color_(\string,Color.black)
			.string_("It looks like there was a problem starting the");

		MVC_StaticText(gui[\scrollView],Rect(16, 45, 480, 22))
			.font_(Font("Helvetica",11))
			.shadow_(false)
			.color_(\string,Color.black)
			.string_("Audio Server. Make sure the Input & Output");

		MVC_StaticText(gui[\scrollView],Rect(16, 60, 480, 22))
			.font_(Font("Helvetica",11))
			.shadow_(false)
			.color_(\string,Color.black)
			.string_("devices have the same sample rate. Do this");

		MVC_StaticText(gui[\scrollView],Rect(16, 75, 480, 22))
			.font_(Font("Helvetica",11))
			.shadow_(false)
			.color_(\string,Color.black)
			.string_("in the system Audio MIDI Setup.");

		// Open Audio MIDI Setup
		MVC_FlatButton(gui[\scrollView],Rect(16, 110, 150, 20),"Open Audio MIDI Setup",gui[\theme])
			.canFocus_(true)
			.color_(\up,Color(50/77,56/77,59/77)*1.35)
			.action_{
				"Audio MIDI Setup".openApplication;
			};
		// Ok
		MVC_FlatButton(gui[\scrollView],Rect(190, 110, 50, 20),"Ok",gui[\theme])
			.canFocus_(true)
			.color_(\up,Color.white)
			.action_{
				task.stop;
				window.close;
				{ if (server.serverRunning.not) {
					this.failToStart(server)
				} }.defer(3);
			};

		window.create;

	}

}
// ----------------------------
