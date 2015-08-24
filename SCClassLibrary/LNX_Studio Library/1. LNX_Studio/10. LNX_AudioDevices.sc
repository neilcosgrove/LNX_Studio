
// Audio devices  ////////////////////////////////////////////////////////////////////

// Manage Audio IO devices.
// TO DO: Develop class to save no. in's & out's

LNX_AudioDevices {

	classvar	defaultOutput,		defaultInput,
			<defaultFXBusChannels   =32,
			<defaultAudioBusChannels=2048;
	
	classvar	<outputDevices,		<inputDevices,
			<friendlyOutputNames,	<friendlyInputNames,
			<outputDevicesChannels,	<inputDevicesChannels;
			
	classvar	<outputMenuList,		<inputMenuList,
			<fxMenuList,			<outputAndFXMenuList,
			<inputAndFXMenuList,   <outputInputAndFXMenuList;
	         
	classvar	<outDevice,			<inDevice,
			<numAudioBusChannels,
			<numOutputBusChannels=2,<numInputBusChannels=2,
			<numFXBusChannels;
			
	classvar	<guiItems,			<>updateMenuAction;
	
	classvar <>action,				<>verbose=false;

	*initClass {
		var devices;
		Class.initClassTree(LNX_File);
		devices = "AudioDevices".loadPref ? ["nil","nil"];
		# defaultOutput, defaultInput = devices;
		numFXBusChannels=defaultFXBusChannels;
		numAudioBusChannels=defaultAudioBusChannels;
		this.updateDeviceList;
		this.devices_(defaultOutput,defaultInput);
		if (verbose) { this.post };
	}
	
	*defaultOutput{ ^(defaultOutput=="nil").if(nil,defaultOutput) }
	
	*defaultInput{ ^(defaultInput=="nil").if(nil,defaultInput) }
	
	*firstOutputBus {^0} 
	
	*firstInputBus  {^numOutputBusChannels}
 	
 	*firstFXBus     {^numOutputBusChannels+numInputBusChannels}
 	
	*firstPrivateBus{^numOutputBusChannels+numInputBusChannels+numFXBusChannels}
	
	*instGroupChannel{|id| ^numAudioBusChannels - 2 - (id*2) }
	
	*numPrivateBusChannels{^numAudioBusChannels-this.firstPrivateBus}
	
	*getOutChannelIndex{|i| if (i>=0) { ^i*2 }{ ^this.firstFXBus+(i.neg*2-2) } }
	
	*getFXChannelIndex{|i| ^(this.firstFXBus)+(i*2) }
	
	*devices_{|out,in|
		if ((outDevice!=out) or:{inDevice!=in}) {
			this.outDevice_(out);
			this.inDevice_(in);
			this.updateMenuLists;
			this.savePref;
		};
	}
	
	*devices{^[outDevice,inDevice]}
	
	*savePref{ this.devices.savePref("AudioDevices") }
	
	*outDevice_{|out|
		if ((out.notNil) and: {outputDevices.containsString(out)}) {
			outDevice=out;
			numOutputBusChannels=outputDevicesChannels[outputDevices.indexOfString(out)];
		}
	}
	
	*inDevice_{|in|
		if ((in.notNil) and: {inputDevices.containsString(in)}) {
			inDevice=in;
			numInputBusChannels=inputDevicesChannels[inputDevices.indexOfString(in)];
		}
	}
	
	*outDeviceExists{|out|
		if ((out.notNil) and: {outputDevices.containsString(out)}) {^true} {^false}
	}
		
	*inDeviceExists{|in|
		if ((in.notNil) and: {inputDevices.containsString(in)}) {^true} {^false}
	}
	
	*updateDeviceList {
		
		var outDevStored = 
		Dictionary[ "M-Track Eight" -> 8, "Soundflower (16ch)" -> 16, "Soundflower (64ch)" -> 64 ];
		
		var inDevStored = 
		Dictionary[ "M-Track Eight" -> 8, "Soundflower (16ch)" -> 16, "Soundflower (64ch)" -> 64 ];
						
		outputDevices = ["nil"] ++ ServerOptions.outDevices;
		inputDevices  = ["nil"] ++ ServerOptions.inDevices;
		
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
			
		outputDevicesChannels = outputDevices.collect{|device| outDevStored[device] ? 2 };
		inputDevicesChannels  = inputDevices.collect {|device| inDevStored[device ] ? 2 };
		
	}
	
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
	
	*outputChannelList{^outputInputAndFXMenuList} // a temp fix to remove
	
	// this needs to move to LNX_AudioDevices !!!!!!!!!!!!!!!!!!
	
	*changeAudioDevices{|server,devices,postBootFunc,noChangeFunc|
		var exists;
		devices=devices?[nil,nil];
		
		devices[0]=devices[0] ? defaultOutput;
		devices[1]=devices[1] ? defaultInput;
		
		if (LNX_AudioDevices.outDeviceExists(devices[0]).not) {
			("Output Device"+(devices[0].asString)+
				"does not exist. Going to use the default device.").warn;
			devices[0]=defaultOutput;
		};
		if (LNX_AudioDevices.inDeviceExists(devices[1]).not) {
			("Input Device"+(devices[1].asString)+
				"does not exist. Going to use the default device.").warn;
			devices[1]=defaultInput;
		};
		
		if ((devices[0]==server.options.outDevice)&&(devices[1]==server.options.inDevice)) {
			noChangeFunc.value; // used to carry on during a load
		}{
			if (devices[0]!=server.options.outDevice) {
				if (verbose) { ("Changing audio out device to:"+devices[0]).postln;};
			};
			if (devices[1]!=server.options.inDevice) {
				if (verbose) { ("Changing audio in device to:"+devices[1]).postln;};
			};
			
			LNX_AudioDevices.devices_(devices[0],devices[1]);
			
			server.quit;
			if (devices[0]=="nil") {devices[0]=nil};
			if (devices[1]=="nil") {devices[1]=nil};
			server.options.outDevice=devices[0];
			server.options.inDevice=devices[1];
			
			server.options.numOutputBusChannels_(LNX_AudioDevices.numOutputBusChannels);
			server.options.numInputBusChannels_(LNX_AudioDevices.numInputBusChannels);
			server.options.numAudioBusChannels_(numAudioBusChannels);
			
			server.boot;
			server.waitForBoot({
				postBootFunc.value;
			});
		}
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
			
		this.updateDeviceList;
	
		guiItems=[];
		
		SCStaticText.new(window,Rect(xy.x+42,xy.y+3,100, 22))
			.string_("Audio Hardware")
			.stringColor_(Color.black);

		SCStaticText.new(window,Rect(xy.x-10,xy.y+24,20, 22))
			.string_("In")
			.stringColor_(Color.black);
			
		item=MVC_PopUpMenu3(window,Rect(xy.x+10,xy.y+25,150,17))
			.items_(friendlyInputNames)
			.color_(\background,Color.ndcMenuBG)
			.action_{|me|
				this.devices_(outputDevices[guiItems[1].value],
							 inputDevices[guiItems[0].value]);
				action.value(this.devices);
			}		
			.value_(inputDevices.indexOfString(inDevice))
			.font_(Font("Arial", 10));
		guiItems=guiItems.add(item);
	
		SCStaticText.new(window,Rect(xy.x-14,xy.y+43,20, 22))
			.string_("Out")
			.stringColor_(Color.black);
		
		item=MVC_PopUpMenu3(window,Rect(xy.x+10,xy.y+45,150,17))
			.items_(friendlyOutputNames)
			.color_(\background,Color.ndcMenuBG)
			.action_{|me|
				this.devices_(outputDevices[guiItems[1].value],inputDevices[guiItems[0].value]);
				action.value(this.devices)
			}		
			.value_(outputDevices.indexOfString(outDevice))
			.font_(Font("Arial", 10));
		guiItems=guiItems.add(item);
	
	}
	
	*bootServer{|server|
		if (server.serverRunning.not) {
			server.options.numOutputBusChannels_(LNX_AudioDevices.numOutputBusChannels);
			server.options.numInputBusChannels_ (LNX_AudioDevices.numInputBusChannels);
			server.options.numAudioBusChannels_(numAudioBusChannels);
			{ server.waitForBoot({},1,true) }.defer(0.2); // this should boot
			{ if (server.serverRunning.not) { this.failToStart(server) } }.defer(10);
		};
	}
	
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
					server.waitForBoot({},1,true);
				}
			};
		}.play;
								
		gui=();	
		
		gui[\theme] = ( orientation_:\horizontal,
			rounded_:	true,
			colors_: (up:Color(0.9,0.9,0.9), down:Color(0.9,0.9,0.9)/2)
		);
	
		window=MVC_Window("Audio Server",
				Rect	(SCWindow.screenBounds.left+LNX_Studio.osx,
					 SCWindow.screenBounds.height-147-(22*3)-(LNX_Studio.thisHeight),
					 270, 170), resizable: false);
		window.color_(\background,(Color(0,1/103,3/77,65/77))).alwaysOnTop_(true);
	
		MVC_RoundBounds(window, Rect(11,11,window.bounds.width-22,window.bounds.height-22-1))
			.width_(6)
			.color_(\background, Color(29/65,42/83,6/11));
		
		// the main view
		gui[\scrollView]=MVC_CompositeView(window,
				Rect(11, 11, window.bounds.width-22, window.bounds.height-22-1))
			.color_(\background,Color(50/77,56/77,59/77))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);
		
		MVC_StaticText.new(gui[\scrollView],Rect(16, 6, 480, 22))
			.font_(Font("Helvetica-Bold",14))
			.string_("WARNING:")
			.color_(\string,Color.white);
			
		MVC_StaticText.new(gui[\scrollView],Rect(16, 30, 480, 22))
			.font_(Font("Helvetica",11))
			.shadow_(false)	
			.color_(\string,Color.black)
			.string_("It looks like there was a problem starting the");
			
		MVC_StaticText.new(gui[\scrollView],Rect(16, 45, 480, 22))
			.font_(Font("Helvetica",11))
			.shadow_(false)	
			.color_(\string,Color.black)
			.string_("Audio Server. Make sure the Input & Output");
		
		MVC_StaticText.new(gui[\scrollView],Rect(16, 60, 480, 22))
			.font_(Font("Helvetica",11))
			.shadow_(false)	
			.color_(\string,Color.black)
			.string_("devices have the same sample rate. Do this");
			
		MVC_StaticText.new(gui[\scrollView],Rect(16, 75, 480, 22))
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
				} }.defer(2);
			};
		
		window.create;
			
	}
	
}
// ----------------------------
