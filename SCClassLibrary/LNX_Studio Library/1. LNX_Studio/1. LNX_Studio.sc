   //                                //        //||     //  || //
  // ****************************** //        // ||    //   ||//
 //     LNX_STUDIO Version 2.0     //        //  ||   //    |//
// ****************************** //        //   ||  //     //|
//                               //        //    || //     //||
//   2016 by neil cosgrove      //======= //     ||//     // ||
//        & andrew lambert
//   for Mac & Linux
//
// (nc) no copyright. use and abuse. :)
// refer to a gpl or something similar
//
// Over of Features:
// Instruments, FX's, MIDI Instruments, Save, Load, Instrument Presets
// MIDI In/Out patching, MIDI control of all GUI, Internal MIDI Ports, Internal/Ext MIDI Clock
// Networking - Syncing over a network all of the Studio features
// Automation, Instrument Library, Preset sequencing
// Loading samples directly from a web browser into the instruments
//
// Instruments &  Effects:
// =======================
// Bum Note 2   - Mono/poly acidic bass line synth with sequencers
// Drum Synth   - Drum synth with step seq
// GS Rhythm	- A sample & grain based drum machine
// SC Code      - A programmable instrument using sc code
// SC Code FX   - A programmable effect using sc code
// Step Seq     - A 32x8 Step Sequencer
// Controllers  - Internal Midi Controller
// FreeVerb     - SC's FreeVerb (coded from experiments with faust)
// PitchShift   - A pitch shifter
// GVerb        - The "GVerb" LADSPA effect (by Juhana Sadeharju)
// Limiter      - A Limiter
// Delay        - A Delay
// Drive        - A distortion
// Ring		  - A ring mod
// AudioIn      - Audio Input
// Melody Maker - Tool for making melodies and playing instruments
//
// LNX_Studio is a large project and i'm going to break it down into smaller parts
// at the moment the LXN_Studio object itself is spilt over 3 files
// this is the main file, there is also "LNX_Studio GUI" & "LNX_Studio transport"
//
// You can only have 1 instance of LNX_Studio.
//
// Many thanks to the following people for their help and their lovely code.
// Juan           : Too many things to mention
// Tillman        : Much testing
// BlackRain      : NetAddr extensions
// jostM          : TabbedView
// Wouter Snoei   : pretty much everything in wslib
// The UserList   : for all the fixes and bits i got stuck on
// otophilia      : the acid
// Ross Bencina   : OscGroups
// Josh           : helping with the SC side of OscGroups
// and nonprivate : sound advice.
//
// also thanks to Southern, Themes Link and Gatwick Express for the time
// I now revoke my thanks to Southern, what a joke.
//
// enjoy, love neil x (lnx)
//

LNX_Studio {

	//// class ////////////////////////////////////////////////////

	classvar	<>versionMajor=2,	<>versionMinor=0,	<version,
				<internetVersion,	<fileLoadVersion=3;

	classvar	studios,			<instTypes,			<>yos=0,
				<thisWidth=212, 	<thisHeight,		<defaultHeight=374,
				<>osx=0,			<visibleTypes,		<instLibraryFileNames,
				<libraryFolder;

	classvar 	<>verbose=false;

	//// instruments & main studio ///////////////////////////////
	var	<server,			<songPath,			<title="LNX_Studio",
		<insts,				<onSoloGroup,		<>groups,
		<>groupIDs,			<serverBootNo,		<channelOutSynths;

	//// groups // to be replaced by groups & groupIDs above;
	var	<instGroup, 		<fxGroup,			<channelOutGroup,
		<scCodeGroup,		<instOutGroup,		<gsrFilterGroup,
		<lfoGroup,			<eqGroup;

	//// midi
	var	<midi,				<noInternalBuses=3,
		midiWin, 			<midiClock, 		noInternalBusesGUI,
		<autoMapGroup,		autoMapOn=true,
		<midiControl,		<controlTitle="Studio + All Instruments",
		<latency=0.2,		<syncDelay=0,		<midiSyncLatency= -0.022,
		<midiCnrtLastNote;

	//// gui stuff
	var	<gui,				<models, 			<showDev=true,
		<>frontWindow,		<show1=false, 		<showNone=false,
		<visibleTypesGUI,	<libraryGUI,		<alwaysOnTop=false,
		<mixerWindow,		<mixerGUI;

	//// clipboard
	var	clipboard, 			netClipboard;

	//// loading, transport & misc
	var	<isLoading=false,	<>loadedAction,		songToLoad,
		<bpm=135, 			<absTime,
		<extClock=false, 	<>beat=0,			<>instBeat=0,
		<isPlaying=false,
		lastPos=0,			extBeat=0, 			<extIsPlaying=false,
		<tapTempo,			<extTiming,			tasks,
		<>batchOn=false,	<>batch,			<>batchFolder=0,
		<lastBatchFolder,	jumpTo;

	//// the network
	var	<network, 		<netTransport=true,	// transport on network
		<api,			transmitInstChange=true;

	var <>myHack,		<>hackOn=false; // for my own hacking of lnx, to remove

	var midi2, padNotes; // temp for CARBON ************

	var <loadIndex = -1, <loadPaths, <>saveBuffersWindow;

	// create a new studio ///////////////////////////////////////////////////////

	*new {|server| if (studios.isEmpty) { ^super.new.init(server) } } // only 1 instance for now

	// start all services

	init {|server|

		this.initInstance;			 // initialise this instance of the studio
		this.createInstrumentList;   // create the lists of instruments available
		this.initLibrary;            // make all the files & folders for the inst library
		this.initVars;				 // all the main vars are initialised here
		this.initMIDI;				 // start all midi services

		this.initNetwork;			 // initialise the network
		this.startResponders;		 // start responders for SCSynth >> SCLang

		this.initServer(server);	 // init the audio server
		this.initModels;		 	 // make studio models
		this.initServerPostModels;   // do any server stuff after models have initialised
		this.bootServer;			 // and now boot it

		this.createMixerWindow;      // the main lnx window
		this.createNetworkWidgets;   // and the network widgets
		this.createMixerWidgets;     // add the mixer widgets
		this.createLibraryWidgets;   // add the library widgets
		this.autoSizeGUI;			 // autosize to number of users. (to add widgets & remove)
		mixerWindow.create;          // now make the window

		LNX_SplashScreen.init(this); // start splash screen
		CmdPeriod.add(this);		 // add this object to CmdPeriod
		this.startClockOff;          // and start off_clock for client side lfos

		MVC_LazyRefresh.startRefreshWatchingTask; // start the lazy refresh task

	}

	//////////////////////////////////////////////////////////////////////////////////////////

	// version & standalone mode //

	*isStandalone{^true} // dev is now standalone

	*versionAtLeast { |maj, min|
		^if((maj==versionMajor) and:{min.notNil}){ versionMinor >= min }{ versionMajor >= maj };
	}

	*versionAtMost { |maj, min|
		^if((maj==versionMajor) and:{min.notNil}){ versionMinor <= min }{ versionMajor <= maj };
	}

	// & instance versions

	versionAtLeast{|maj, min| ^this.class.versionAtLeast(maj,min) }
	versionAtMost{|maj, min| ^this.class.versionAtMost(maj,min) }
	version{^version}
	isStandalone{^this.class.isStandalone}

	// init class ////////////////////////

	*initClass {

		version="v"++versionMajor++"."++versionMinor++"."++fileLoadVersion; // version string
		// these are needed before we continue
		Class.initClassTree(LNX_File);
		Class.initClassTree(LNX_AudioDevices);
		Class.initClassTree(LNX_MIDIPatch);
		// get the latest version number online
		Platform.getURL(
			"http://lnxstudio.sourceforge.net/lnx_version.scd",
			Platform.lnxResourceDir+/+"lnx_version",
			{|status|
				internetVersion = (Platform.lnxResourceDir+/+"lnx_version").loadList;

				if (internetVersion.notNil) {
					if (internetVersion.size>0) {
						internetVersion = internetVersion[0].asFloat;
						if (internetVersion>version.drop(1).asFloat) {
							studios[0].addTextToDialog( "New LNX_Studio available on sourceforge v"
								++(internetVersion.asString),true,true);
						};
					}
				};
			}
		);

		studios = [];

	}

	// create the lists of instruments available
	createInstrumentList{
		// all instruemts
		instTypes    = LNX_InstrumentTemplate.subclasses;
		// and all visible ones. depreciated or test instruments will be missing
		visibleTypes = instTypes.select(_.isVisible).sort{|a,b|
			if (a.sortOrder==b.sortOrder) {
				(a.studioName) <= (b.studioName)
			}{
				(a.sortOrder) <= (b.sortOrder)
			}
		};
		thisHeight=defaultHeight+(((visibleTypes.size)/2).asInt*25); // auto size window
	}

	// this might be used in the future so many studios can be run at once
	// at the moment some class vars in this class and others that stop this from work properly

	initInstance{ studios=studios.add(this) }

	// studio vars (note this is called when clear is called ?) /////////////////////////////////

	initVars{
		myHack=(); // to remove

		onSoloGroup = LNX_InstrumentTemplate.onSoloGroup;
		insts       = LNX_Instruments(this);
		models      = IdentityDictionary[];
		gui         = IdentityDictionary[];
		tasks       = IdentityDictionary[];
		bpm         = 90.rrand(200);
		absTime     = 2.5/bpm;
		extTiming   = [];
		tapTempo    = LNX_TapTempo().tapFunc_{|me,bpm| models[\tempo].valueAction_(bpm.asInt) };
		MVC_StepSequencer.studio_(this);      // not great, why am i doing this? To get absTime
		LNX_SampleBank.studio_(this);         // to find out if playing to change download speed
		LNX_PianoRollSequencer.studio_(this); // for pianoroll guiJumpTo call
		MVC_Model.studio_(this);              // is playing
		MVC_Automation.studio_(this);         // for finding MVC_Automation's on the network
		LNX_POP.studio_(this);                // for finding instBeat when gui pressing program
		LNX_BufferProxy.studio_(this);        // for gui flashing
		LNX_URLDownloadManager.studio_(this); // for "Downloading samples..." & "Finished." Dialog
		insts.addDependant(LNX_POP); 		  // for updating gui positions

		#show1, showNone = (("show1 showNone").loadPref?[true,false]).collect(_.isTrue);
	}

	// the Network ///////////////////////////////////////////////////////////////

	// create the network and api

	initNetwork{
		network=LNX_Network(this);
		// the 1st interface is the actual studio interface, the 2nd is for debugging
		api=LNX_API.newPermanent(this, \std, #[
			\netSyncSong,	\noInternalBuses_, \netNewMidiControlAdded, \hostAddInst,
			\netAddInst, \selectInst, \hostDeleteInst, \deleteInst, \hostPaste, \paste,
			\hostDuplicate, \duplicate, \netSelectedInst, \closeStudio, \netTalk,
			\netSyncCollaboration, \netAddLoadList, \netOnSoloUpdate, \netMove, \hostPlay,
			\play, \netPause, \hostStop, \netStop, \hostSetBPM, \setBPM,
			\netAddInstWithLoadList,\netAllInstsSelectProgram, \netSetModel, \hostJumpTo,
			\netSetAuto, \netNoteOn, \netNoteOff, \netAllToPOP, \jumpWhileStopped
		],#[
			\post, \postMe, \postList, \postAll, \postStuff, \postTime, \postClock,
			\postSpecies
		]);
		// i think long periods of sleep with the network causes a crash, to check again
		// this might have to do with post window being open?
		if (thisProcess.platform.name==\osx) {
			thisProcess.platform.sleepAction_{
				network.disconnect;
				this.guiStop;
			};
		};
	}

	// Audio, server & UGens /////////////////////////////////////////////////////

	// booting ///////////

	// pre initModel

	initServer{|argServer|
		server=argServer;
		server.options.outDevice=LNX_AudioDevices.defaultOutput;
		server.options.inDevice=LNX_AudioDevices.defaultInput;
		server.options.memSize_(8192*16); // = 128MB (this is for UGens not buffers)
		instTypes.do{|inst| inst.initServer(server)}; // and all insts
		ServerBoot.add({this.postBootFuncs},server);
	}

	// post initModel

	initServerPostModels{
		var blockSize = (2**(5..9))[models[\blockSize].value];
		server.options.blockSize_(blockSize);
	}

	// boot the server and run postBootFuncs when done

	bootServer{ LNX_AudioDevices.bootServer(server) }

	// send all the instrument UGens to the server, and other misc stuff

	postBootFuncs{
		{
			if (serverBootNo != LNX_AudioDevices.bootNo) {

				serverBootNo = LNX_AudioDevices.bootNo;
				Server.default_(server);			// set as default server
				server.sendMsg("error",0);			// turn off error messaging
				this.latency_(("latency".loadPref ? [latency])[0].asFloat); // set the latency

				//fxBuses to be use properly later
				((LNX_AudioDevices.numFXBusChannels/2).asInt.collect{ Bus.audio(server,2) });

				LNX_BufferArray.serverReboot(server);// make blank buffers
				LNX_BufferProxy.serverReboot;		// load bufers
				this.initUGens;						// send studio SynthDefs (Limiter Out)
				instTypes.do(_.initUGens(server));  // init all instrument uGens
				insts.do(_.initUGens(server));		// used by SC Code FX
				LNX_SampleBank.initUGens(server);	// sample bank for tuning
				LNX_EQ.initUGens(server);           // and EQ
				LNX_Voicer.update_(server);			// update voicer

				{this.initGroups}.defer(0.1);		// start inst, code, fx & out groups
				{this.startDSP}.defer(0.3);			// if using internal, wait 4 it 2 catch up
				{
					server.volume_(models[\volume].value);
					if (models[\mute].isTrue) { server.mute };
				}.defer(0.3);
				{
					insts.visualOrder
						.do(_.serverReboot)
						.do(_.startInstOutDSP)
						.do(_.startDSP)
						.do(_.updateDSP)
						.do(_.restartEQ);
				}.defer(0.3); // defer used to help LNX_CodeFX
				{
					if (songToLoad.notNil) {
						api.sendClumpedList(\netSyncSong,songToLoad);
						this.putLoadList(songToLoad);
						songToLoad=nil;
					}
				}.defer(0.4); // if a song is waiting to load, then load it

			};

		}.defer(0.1);
	}

	// start groups

	initGroups{
		var sideGroup;
		var postFilterGroup;

		lfoGroup        = Group();				          	// lfo group
		instGroup       = Group(lfoGroup,\addAfter);        // instruments group
		gsrFilterGroup  = Group(instGroup,\addAfter);       // gsRythmn filter group
		postFilterGroup = Group(gsrFilterGroup,\addAfter);  // gsRythmn post filter group
		scCodeGroup     = Group(postFilterGroup,\addAfter); // sc code instruments
		eqGroup         = Group(scCodeGroup,\addAfter);     // eq group before inst out
		instOutGroup    = Group(eqGroup,\addAfter);         // the inst out group for levels & outs
		fxGroup         = Group(instOutGroup,\addAfter);    // the effects
		sideGroup       = Group(fxGroup,\addAfter);			// the effects
		channelOutGroup = Group.after(sideGroup);           // the channel outputs

		groups = (
			\lfo:			lfoGroup,
			\inst: 			instGroup,
			\gsrFilter:		gsrFilterGroup,
			\postFilter: 	postFilterGroup,
			\scCode: 	 	scCodeGroup,
			\eq:     		eqGroup,
			\instOut:		instOutGroup,
			\fx:			fxGroup,
			\sideGroup:		sideGroup,
			\channelOut:	channelOutGroup
		);

		groupIDs = groups.collect(_.nodeID);

		insts.do(_.initGroups);
	}

	// the studio SynthDefs

	initUGens{

		// all purpose out for studio.
		// nb need to think about >2 channels out because this wont work

		SynthDef("LNX_LimitOut", {|channel=0,preAmp=0|
			var out;
			out=In.ar(channel, 2);
			out=Protect.newNoClip(out);
			out=LeakDC.ar(out);
			out=out * (preAmp.dbamp);
			out=Limiter.ar(out,0.99, 0.001);
			SendPeakRMS.kr(out, 20, 1.5, "/peakOut");
			ReplaceOut.ar(channel,out);	// replace
		}).send(server);

		// SynthDef out instOut, does levels, pan & send

		SynthDef("LNX_InstOut", {|inChannel=0, outChannel=0, id=0, amp=1, pan=0, sendAmp=0, sendChannel=0|
			var leftPan, rightPan;
			var out  = In.ar(inChannel, 2);    // signal in
			leftPan  = (pan*2-1).clip(-1,1);   // left pos
			rightPan = (pan*2+1).clip(-1,1);   // right pos
			out      = LinPan2.ar(out[0], leftPan) + LinPan2.ar(out[1], rightPan); // pan
			out      = out * amp;                                 // apply amp
			SendPeakRMS.kr(out, 20, 1.5, "/instPeakOut", id); // left meter
			//SendPeakRMS.kr(out[1], 20, 1.5, "/instPeakOutR", id); // right meter
			Out.ar(outChannel,out);                               // now send out
			out = out*sendAmp;                                    // apply send amp
			Out.ar(sendChannel,out);                          	  // and send to fxs
		}).send(server);

	}

	// start studio Synths

	startDSP{
		// add a limiter to each stereo out pair
		channelOutSynths = (LNX_AudioDevices.numOutputBusChannels/2).asInt.collect{|i|
			Synth.tail(channelOutGroup,"LNX_LimitOut",i*2);
		};
		if (thisProcess.platform.name!=\osx) {
			LNX_MouseXY.startDSP(server); // old hack for getting x,y on other op systems
		};
		this.setPreAmp;
	}

	setPreAmp{
		channelOutSynths.do{|synth| synth.set(\preAmp, models[\preAmp].value) };
	}

	// the network responders for levels back from server and into the studio. + also lfo values

	startResponders {

		OSCFunc({|msg|
			models[\peakOutL].lazyValueAction_(msg[3]*(models[\volume].value.dbamp),0);
			models[\peakOutR].lazyValueAction_(msg[5]*(models[\volume].value.dbamp),0);
		}, '/peakOut');

		OSCFunc({|msg|
			var inst = insts[msg[2]];
			if (inst.notNil) {
				inst.peakOutLeft_ (msg[3]);
				inst.peakOutRight_(msg[5]);
			};
		}, '/instPeakOut');

		OSCFunc({|msg|
			if (insts[msg[2]].notNil) { insts[msg[2]].lfo_(msg[3],msg[5]) }
		}, '/lfo');

		OSCFunc({|msg|
			if (insts[msg[2]].notNil) { insts[msg[2]].filter_(msg[3],msg[5],msg[7]) }
		}, '/filter');

		OSCFunc({|msg|
			if (insts[msg[2]].notNil) { insts[msg[2]].a2m_in_(msg[3..10]) }
		}, '/A2M');

		OSCFunc({|msg|
			if (insts[msg[2]].notNil) { insts[msg[2]].sIdx_in_(msg[3]) }
		}, '/sIdx');

		OSCFunc({|msg|
			if (insts[msg[2]].notNil) { insts[msg[2]].sRecLvl_(msg[3],msg[5]) }
		}, '/sRecLvl');

	}

	// reset all dsp (stop all audio, boot server if needed and restart all audio)

	restartDSP{
		var temp;
		insts.do(_.stopDSP);
		server.freeAll;
		if (server.serverRunning.not) {
			this.bootServer;
		}{
			this.initGroups;
			this.startDSP;
			insts.visualOrder.do(_.startDSP);
			insts.visualOrder.do(_.startInstOutDSP);
			insts.visualOrder.do(_.updateDSP);
			insts.visualOrder.do(_.restartEQ);
			{server.volume_(models[\volume].value)}.defer(0.1);
		};
	}

	// this is called at command Period

	cmdPeriod{
		this.pause;
		this.startResponders;		 // for levels back into SCLang
		insts.do(_.cmdPeriod);
		{this.restartDSP}.defer(0.5);
		MVC_LazyRefresh.startRefreshWatchingTask;
	}

	// and this on close

	onClose{
		this.saveWindowBounds;
		network.disconnect;
		this.free;
		// also need to add quick save if insts.size>0
	}

	// Latency ///////////////////////////////////////////////////////////////////////////////

	// the total latency of this system. includes syncDelay. sync delay is -ive delay in
	// AudioIn and MIDIOut

	actualLatency{ ^latency + syncDelay }

	// set latency of studio
	latency_{|argLatency|
		if (argLatency<latency) {
			latency=argLatency;
			insts.do(_.stopAllNotes); // if its smaller release notes
		}{
			latency=argLatency;
		};

		// replace with something like above
		// this.updateSyncDelay; // update sync delay so intruments are updated too

		server.latency_(this.actualLatency); // this is now the latency between Lang & Server
		[latency].savePref("latency");       // save preference
	}

	// work out the largest -ive sync delay in all instruments
	// and set that as the studio +ive syncdelay
	checkSyncDelay{
		var oldSync = syncDelay;
		// largest only -ive is turned into a +ive syncDelay so we don't below latency
		syncDelay = insts.collect{|inst| inst.syncDelay.clip(-inf,0).abs }.asList.sort.last ? 0;
		if (oldSync!=syncDelay) {insts.do(_.stopAllNotes) };
	}

	// MIDI ///////////////////////////////////////////////////////////////////////////////

	// start midi stuff

	initMIDI{
		var midiClockPrefs= "MIDI Clock".loadPref;	     // get midi clock in & out prefs
		var midiInternalPrefs= "MIDI Internal".loadPref; // get no internal midi ch prefs

		midiSyncLatency = ("MIDI Sync Latency".loadPref ? [midiSyncLatency])[0].asFloat;
		LNX_MIDIPatch.midiSyncLatency_(midiSyncLatency);

		midiClockPrefs= (midiClockPrefs ? [0,-1,0,0]).collect(_.asInt); // put none if absent
		midiInternalPrefs= (midiInternalPrefs ? [noInternalBuses])[0].asInt;

		if (LNX_MIDIPatch.initialized.not) { LNX_MIDIPatch.init };	// init LNX_MIDIPatch
		this.noInternalBuses_(midiInternalPrefs);					// set no of buses
		this.autoMapInit;											// init auto map
		midiControl = LNX_MIDIControl.new(this);					// new controls for studio
		midiClock   = LNX_MIDIPatch(0,-1,0,0);						// for midiClock in and out
		midiClock.putLoadList(midiClockPrefs);						// put in the prefs
		midiClock.sysrtFunc={|src,chan,val| this.midiClockIn(chan,val)};
		LNX_MIDIControl.studio_(this); // used to register this studio in midi control so
									   // midi control can update front window in studio
									   // so this will remain on top when networking
		LNX_POP.initMIDI;

		this.initPadMixerMIDI; // temp for CARBON ************

	}

	// save the MIDI preferences

	saveMIDIprefs{
		midiClock.getSaveList.savePref("MIDI Clock");
		[noInternalBuses].savePref("MIDI Internal");
	}

	// save the MIDI control keyboard preference

	saveControllerKeyboardPrefs{ midi.getSaveList.savePref("ControllerKeyboard") }

	// change no internal midi buses

	noInternalBuses_{|n|
		noInternalBuses=n;
		LNX_MIDIPatch.changeNoInternalBuses(n);
		LNX_InstrumentTemplate.noInternalBuses_(n);
	}

	// gui call to change noInternal Buses

	guiNoInternalBuses_{|n| api.groupCmdOD(\noInternalBuses_,n) }

	// used by MIDI Control, nil actually means see all controls (studio & insts)

	controlPatches{ ^nil }

	// called from midiControl to inform a new control has been added
	// or changed and needs to be sent over the net

	newMidiControlAdded{
		var l;
		l=midiControl.getSaveList;
		if (network.isConnected) {
			api.sendClumpedList(\netNewMidiControlAdded,l); // send it over the net
		};
	}

	// reciever of above

	netNewMidiControlAdded{|l|
		var midiLoadVersion;
		l=l.reverse; // reverse for popping
		l.do{|i,j|
			if (i.species==Symbol){
				l[j]=i.asString; // convert symbols to strings
			}
		};
		midiLoadVersion=l.pop.version;
		midiControl.putLoadList(l.reverse,midiLoadVersion);
		LNX_MIDIControl.updateGUI;
	}

	instNo{^-1} // used to sort automations
	id{^-1}     // used to find automations in network

	// preset add for all insts

	guiAllInstsAddPreset{ insts.do(_.guiAddPreset) }

	// add a preset to all insts and then add it to the next free POP

	// gui call for add all to POP
	guiAllToPOP{|includeFX=true|
		api.hostCmdClumpedList(\netAllToPOP,
			[includeFX.asInt] ++ insts.visualOrder.collect{|i| (i.canTurnOnOff) && (i.isOn)}.asInt
		)
	}

	// net call of add all to POP
	netAllToPOP{|onOffList|
		var includeFX;
		// find next free pop index
		var popFreeAt = insts.collect(_.presetsOfPresets).collect(_.presetsOfPresets)
			.asList.flop.collect{|i| i.collect{|i| i==2}.includes(false).not }.indexOf(true);

		if (popFreeAt.isNil) { LNX_POP.more }; // add more pops if needed

		// find next free pop index
		popFreeAt = insts.collect(_.presetsOfPresets).collect(_.presetsOfPresets)
			.asList.flop.collect{|i| i.collect{|i| i==2}.includes(false).not }.indexOf(true);

		includeFX = onOffList[0].isTrue;	// 1st item is includeFX?
		onOffList = onOffList[1..].asInt;	// and the rest is onOffs
		if (popFreeAt.isNil) { ^this };		// if still no free space then drop

		// add presets to all or  add presets to everything but fxs
		if (includeFX) { insts.do(_.guiAddPreset) } { insts.notEffect.do(_.guiAddPreset) };

		insts.visualOrder.do{|inst,n| 		// now add them to pop
			if (inst.canTurnOnOff) { 		// is it an instrument i can turn on & off?
				if (onOffList[n].isTrue) {
					// add the preset we just made + 2
					inst.presetsOfPresets.guiSetPOP(popFreeAt, inst.presetMemory.size + 2)
				}{
					inst.presetsOfPresets.guiSetPOP(popFreeAt, 0); // else mute it
				};
			}{
				// add the preset we just made
				if (includeFX) {
					inst.presetsOfPresets.guiSetPOP(popFreeAt, inst.presetMemory.size)
				}
			};
		};
	}

	guiAllInstsSelectProgram{|prog,latency|
		insts.do{|i| i.midiSelectProgram(prog,latency) };
		api.sendOD('netAllInstsSelectProgram',prog);
	}

	netAllInstsSelectProgram{|prog| insts.do{|i| i.midiSelectProgram(prog,0) } }

	/////////////////////////////////////

	// auto map midi init

	autoMapInit{
		var ckPref="ControllerKeyboard".loadPref;

		ckPref = (ckPref ? [ 0, -1, 0, 0 ]).collect(_.asInt);
		midi=LNX_MIDIPatch(0,-1,0,0);
		midi.putLoadList(ckPref);

		midiCnrtLastNote = IdentityDictionary[]; // last note played, holds inst

		// attach functions to the midiIn controller

		midi.controlFunc = {|src, chan, num,  val , latency| this.autoMap(num,val,latency) };

		midi.programFunc = {|src, chan, prog,latency|
			if ((autoMapOn)and:{insts.selectedInst.notNil}) {
		    		insts.selectedInst.midiSelectProgram(prog,latency)}
		    	};
		midi.noteOnFunc  = {|src, chan, note, vel ,latency|
			if ((autoMapOn)and:{insts.selectedInst.notNil}
			   and: {(midi.uidIn)!=(insts.selectedInst.midi.uidIn)}) {
				this.noteOn(note, vel, latency);
			};
		};
		midi.noteOffFunc = {|src, chan, note, vel ,latency|
			if ((autoMapOn)and:{insts.selectedInst.notNil}
			    and:{((midi.uidIn)!=(insts.selectedInst.midi.uidIn))}) {
					this.noteOff(note, vel, latency);
			};
		};
		midi.touchFunc = {|src, chan, pressure  ,latency|
			if ((autoMapOn)and:{insts.selectedInst.notNil}
			    and:{((midi.uidIn)!=(insts.selectedInst.midi.uidIn))}) {
					insts.selectedInst.touch(pressure, latency);
			};
		};
		midi.controlFunc = {|src, chan, num,  val ,latency|
			if ((autoMapOn)and:{insts.selectedInst.notNil}
			    and:{((midi.uidIn)!=(insts.selectedInst.midi.uidIn))}) {
					insts.selectedInst.control(num, val, latency);
			}
		};
		midi.bendFunc    = {|src, chan, bend,latency|
			if ((autoMapOn)and:{insts.selectedInst.notNil}
			    and:{((midi.uidIn)!=(insts.selectedInst.midi.uidIn))}) {
					insts.selectedInst.bend(bend, latency);
			}
		};
	}

	/////

	isCntKeyboardNetworked{ ^models[\networkCntKeyboard].isTrue }

	// noteOn from controller keyboard
	noteOn{|note, vel, latency|
		// do note on event
		this.doNoteOn(insts.selectedInst, note.asInt, vel, latency, \controllerKeyboard);
		if (this.isCntKeyboardNetworked) {
			api.sendOD(\netNoteOn,insts.selectedInst.id, note, vel); // and send over network
		};
	}

	// net version of above
	netNoteOn{|id, note, vel|
		this.doNoteOn(insts[id.asInt], note.asInt, vel.asFloat, nil, \network)
	}

	// do controller keyboard note On
	doNoteOn{|inst, note, vel, latency, source|
		if(midiCnrtLastNote[note].notNil) {
			this.doNoteOff(midiCnrtLastNote[note], note, vel, latency, source);// finish last note
		};
		inst.pipeIn( LNX_NoteOn(note,vel,latency,source) ); // do note on
		midiCnrtLastNote[note] = inst;   // and store for note off
	}

	// noteOff from controller keyboard
	noteOff{|note, vel, latency|
		// do note off event
		this.doNoteOff(insts.selectedInst, note.asInt, vel, latency, \controllerKeyboard);
		if (this.isCntKeyboardNetworked) {
			api.sendOD(\netNoteOff,insts.selectedInst.id, note, vel); // and send over network
		};
	}

	// net version of above
	netNoteOff{|id, note, vel|
		this.doNoteOff(insts[id.asInt], note.asInt, vel.asFloat, nil, \network)
	}

	// do controller keyboard note On
	doNoteOff{|inst, note, vel, latency, source|
		(midiCnrtLastNote[note] ? inst).pipeIn(
			LNX_NoteOff(note,vel,latency,source) ); // do note on
		// use midiCnrtLastNote 1st
		midiCnrtLastNote[note] = nil; // and remove from midiCnrtLastNote IdentityDictionary
	}

	// auto map MIDI in ( to review ) /////

	autoMap{|num,val|
		if (autoMapOn) {
			if ((num==75)&&(val==1)) { {this.togglePlay  }.defer; ^nil }; // play
			if ((num==74)&&(val==1)) { {this.guiStop     }.defer; ^nil }; // stop
			if ((num==76)&&(val==1)) { {this.toggleRecord}.defer; ^nil }; // record
			if (insts.selectedInst.notNil) {
				if ((num==88)and:{val==1}) { {this.selectPreviousInst}.defer; ^nil }; // up
				if ((num==89)and:{val==1}) { {this.selectNextInst    }.defer; ^nil }; // down
				insts.selectedInst.autoMap(num,val);
			};
		};
	}


	// pass this on

	update{|object, model, arg1,arg2| if (model==\name) { this.changed(\name,arg1,arg2) } }

	/////////// create, delete and edit methods /////////////////////////////////////////////

	// gui call for add instrument, also called from library

	guiAddInst{|type,loadList,name|
		api.hostCmdClumpedList(\hostAddInst,
			[network.thisUser.id,type.asSymbol,network.isListening.if(1,0),name,
				MVC_Model.isRecording, beat]++loadList
		)
	}

	// the host command, userID is ID of person who added the inst and called host
	// or just the user if not connected to a network

	hostAddInst{|list|
		var id, userID, class, userIsListening, loadList, name, autoAdd, autoBeat, bus;

		#userID, class, userIsListening, name, autoAdd, autoBeat...loadList = list;

		// // i need to change so it doesn't matter if audio server is running
		if ((isLoading.not)and:{server.serverRunning}) {

			id=LNX_ID.nextID; // get the id for me and everyone else

			bus=insts.firstFXBus; // also get the next free fx bus (used in fx's in)

			// network this
			if (network.isConnected) {
				api.sendClumpedList(\netAddInstWithLoadList,
					[class,id,userID,userIsListening,1, bus, autoAdd, autoBeat]++loadList );
			};

			// what i should do as host
			if (userID==network.thisUser.id) {
				// for me as host or just me

				this.thisAddInst(class,id,1,true, autoAdd, autoBeat,loadList);

				if (network.isConnected) {  // this test is needed for simpleSeq
					if (network.isListening.not) { onSoloGroup.userInstOn_('lnx_song',id,0) };
					// turn off in song cause the others won't here it
				};
			}{
				// from someone else to the host
				this.netAddInst(class,id,userID,userIsListening,0, autoAdd, autoBeat,loadList);
			};

			// put the load list if there is one (this only happens with duplicate i believe)
			{
				if (loadList.size>0) {
					// insts[id].putLoadList(loadList);
					insts[id].postSongLoad; // i might change this to help melodyMaker
				};

				// this does not send
				this.updateFXBusIN(insts[id],bus);

				// this does not send
				if ((name.isString)||(name.isSymbol)) { insts[id].name_(name.asString) } ;

			}.defer(0.05);



		};
	}

	// this is also called from duplicate

	thisAddInst{|class,id,onOff,new, autoAdd, autoBeat,loadList|
		this.addInst(class, open:new.isTrue, id:id, onOff:onOff,
					autoAdd:autoAdd, autoBeat:autoBeat, loadList:loadList); // add the instrument
		if (show1) { insts.do({|inst| if (inst.id!=id) {inst.closeWindow} } )};
		this.selectInst(id);
	}

	updateFXBusIN{|inst,bus=0|
		if (inst.inChModel.notNil) {
			inst.inChModel.valueAction_(bus,this.actualLatency,false); // this does not send
		}
	}

	// net version of thisAddInst, calls netAddInst but also adds a load list if present

	netAddInstWithLoadList{|list|
		var class,id,userID,userIsListening,new,loadList,bus, autoAdd, autoBeat;
		#class,id,userID,userIsListening,new,bus, autoAdd, autoBeat...loadList = list;

		this.netAddInst(class,id,userID,userIsListening,new, autoAdd, autoBeat,loadList);

		{
			this.updateFXBusIN(insts[id],bus); // this does not send
			if (userID==network.thisUser.id) { this.selectInst(id) };
		}.defer(0.05);
	}

	// net version of thisAddInst (this always comes from the host)

	netAddInst{|class,id,userID,userIsListening,new, autoAdd, autoBeat,loadList|
		var previousFrontWindow, previousInst, onOff;

		if ((isLoading.not)and:{server.serverRunning}) {
			previousInst = insts.selectedInst;
			previousFrontWindow = frontWindow;
			// set onOff for userID isListening
			onOff=1;
			if (userID!=network.thisUser.id) {
				if ((userIsListening==0)or:{network.isListening.not}) { onOff=0 }
			};
			this.addInst(class, open:new.isTrue, id:id, onOff:onOff,
							autoAdd:autoAdd, autoBeat:autoBeat,loadList:loadList); // add the instrument
			insts[id].postSongLoad; // used by melody maker so we have all inst ids

			// update the onSolo for userID isListening
			if (userID==network.thisUser.id) {
				if (network.isListening.not) {onSoloGroup.userInstOn_('lnx_song',id,0) }
			}{
				if (userIsListening==0) {
					onSoloGroup.userInstOn_('lnx_song',id,0);
					onSoloGroup.userInstOn_(network.thisUser.id,id,0);
				}
			};
			if (userID!=network.thisUser.id) {
				// to stop the new window from getting in front of anything we maybe doing,
				// we do this
				insts[id].onOpen_({
					if ((previousInst.notNil)
						and:{showNone.not}and:{previousInst.window.isClosed.not}) {
							previousInst.front;
					};
					if ((previousFrontWindow.notNil)and:{previousFrontWindow.isClosed.not}){
						previousFrontWindow.front;
					};
				});
			};
		}
	}

	// add an instrument

	addInst{|type,bounds,open=true,id,onOff=1, autoAdd=false, autoBeat, loadList|
		var i,inst;

		if (type.isNumber) { type=visibleTypes[type] }; // convert index to class
		if (type.species==Symbol) { type=type.asClass };
		i=insts.size;
		if (i<1) {this.updateOSX};                      // update the studio window offset
												  		//work out bounds
		bounds=bounds ? Rect((i*25)+thisWidth+osx+3,i*23+50,type.thisWidth,type.thisHeight);

		// this is the only place a new inst is created
		inst=type.new(server, this, i, bounds, open,id, loadList); // make the instrument

		this.refreshOnOffEnabled;					    // update solo gui enabled/not enabled

		this.addMixerPaddDependant(inst).updatePadMixer(inst); // temp for CARBON ************

		// do onSolo stuff
		if (onOff==0) {
			inst.onOff_(0);
			if (network.isListening) {
			 	onSoloGroup.userInstOn_('lnx_song',id,0);
			}{
				if (network.isConnected) { onSoloGroup.userInstOn_(network.thisUser.id,id,0) };
			};
		};

		inst.addDependant(this);

		// when automation is recording if a new instrument is added then it will be off at start
		// and then on at current autoBeat
		if ((inst.isMixerInstrument) && (autoAdd.isTrue)) {
			inst.onOffModel.autoStart(autoBeat);
		};

	}

	// gui call to delete an instrument

	guiDeleteInst{
		var id, users;
		if (insts.selectedInst.notNil) {
			id=insts.selectedID;

			clipboard=insts.selectedInst.getSaveList.compress;

			if (network.isConnected) {

				users=network.connectedUsers; // all users now

				if (users.size>0) {
					LNX_Request(users.asList,
						"Delete Instrument Request",
						network.thisUser.shortName +
						"wants to delete the instrument"+
						insts.selectedInst.name +
						"\n",
						"Decline","Accept",\warning, false ,
						"Delete Instrument Request",
						"You are trying to delete the instrument" +
						insts.selectedInst.name +
						"\n"++
						"The others need to agree.",
						"Cancel", nil, \wait_0,
						{},
						{
							api.hostCmdGD(\hostDeleteInst,id);
						})
				}{
					api.hostCmdGD(\hostDeleteInst,id);
				}
			}{
				api.hostCmdGD(\hostDeleteInst,id);
			}
		};
	}

	// the host command for delete

	hostDeleteInst{|userID,id|
		if (network.isConnected.not) {
			this.deleteInst(id);
		}{
			this.deleteInst(id);
			api.sendOD(\deleteInst,id); // network this
		}
	}

	// delete instrument i

	deleteInst{|id|
		var isLoading=false, inst;
		if (insts[id].notNil) {
			insts.do{|inst| if (inst.isLoading) {isLoading=true} };
			if ((isLoading.not) && (this.isLoading.not)) {
				// delete that instrument
				this.soloDelete(id,0); // needs to change
				inst=insts.removeInst(id); // remove via containe
				inst.removeDependant(this);
				inst.stopDSP;
				inst.free;
				this.selectInst(insts.selectedID); // selectInst changes with removeInst
				insts.visualOrder.do({|inst,index| inst.instNo_(index) }); // change numbers
				mixerGUI[id].do(_.remove);
				this.alignInstGUI; // re-align gui here
				MVC_Automation.updateDurationAndGUI; // temp
				this.checkSyncDelay;
			};
		}
	}

	// copy, paste & duplicate instrument
	// called from menu item copy

	guiCopy{
		if (insts.selectedInst.notNil) { clipboard=insts.selectedInst.getSaveList.compress }
	}

	// called from menu item paste.
	// the clipboard is compressed in copy
	// it is then sent to the host, the host then sends it to everyone who then decompress

	guiPaste{
		if ((insts.selectedInst.notNil)&&(clipboard.notNil)) {
			api.hostCmdClumpedList(\hostPaste,[insts.selectedID]++clipboard); // host this
		}
	}

	// host of paste

	hostPaste {|clipboard|
		if (network.isConnected) {api.sendClumpedList(\paste,clipboard)};
		this.paste(clipboard);
	}

	// paste (clipboard is compressed and has iid attached)

	paste{|clipboard|
		var id=clipboard[0];
		var inst=insts[id];
		if (inst.notNil) {
			inst.putLoadList(clipboard.drop(1).decompress);
			inst.postSongLoad; // i might change this to help melodyMaker
			{ MVC_Automation.updateDurationAndGUI }.defer(1);
		};
	}

	// gui call from menu to duplicate an instrument

	guiDuplicate{
		if (insts.selectedInst.notNil) {
			this.guiAddInst(insts.selectedInst.class,insts.selectedInst.getSaveList);
			{ MVC_Automation.updateDurationAndGUI }.defer(1);
		};
	}

	// select instrument is called from the studio gui textBox for that instrument
	// also via hostAddInst, loadSong and addSong

	selectInst{|id|
		if (id.notNil) {
			if (showNone.not) {
				if (show1) {insts.do({|inst| if (inst.id!=id) { inst.closeWindow.closeEQ }})};
				insts[id].front;
				//insts[id].openEQIfOn;
			};
			insts.selectedInst_(id);
			this.setInstNumberColours;
			this.removeInstTextRange(id);
			if (transmitInstChange) { api.sendOD(\netSelectedInst,network.thisUser.id,id) };
		}{
			if (insts.selectedInst.notNil) {
				this.removeInstTextRange(id);
			};
			insts.selectedInst_(id);
			this.setInstNumberColours;
		};
	}

	// a fix for preventing netSelectedInst messages from been sent

	transmitInstChange{
		{
			transmitInstChange=true;
			if (insts.selectedID.notNil) {
				api.sendOD(\netSelectedInst,network.thisUser.id,insts.selectedID);
			};
		}.defer(1);
	}

	// net of above, only used to highlight where user is.

	netSelectedInst{|userID,instID|
		network.users[userID].instID_(instID);
		this.setInstNumberColours;
	}

	// and highlight is called from instrument window to front

	hilightInst{|id|
		if (insts.selectedID!=id) {
			insts.selectedInst_(id);
			this.setInstNumberColours;
			this.removeInstTextRange(id);
		};
		// during a load
		if ((transmitInstChange)and:{insts.ids.includes(id)}) {
			api.sendOD(\netSelectedInst,network.thisUser.id,id);
		}
	}

	// set the colours of the numbers next to each instrument

	setInstNumberColours{
		var colours=();
		network.connectedUsers.do{|user|
			var instID=user.instID;
			if (instID.notNil) {
				if (colours[instID].isNil) {
					colours[instID]=user.color;
				}{
					colours[instID]=Color.white;
				};
			};
		};
		// update user icon color and inst selected.
		insts.pairsDo{|id,inst|
			inst.userIcon_(colours[id]).selected_(id==insts.selectedID);
			if (mixerGUI[id][\instNo].notNil) {
				if (id==insts.selectedID) {
					mixerGUI[id][\instNo].color_(\string,Color.white);
					mixerGUI[id][\instNoPOP].color_(\string,Color.white);
				}{
					mixerGUI[id][\instNo].color_(\string,Color.black);
					mixerGUI[id][\instNoPOP].color_(\string,Color.black);
				};
			};
		};
	}

	// remove all selected text in instrument name

	removeInstTextRange{|id| insts.do{|inst| if (inst.id!=id) { inst.resetNameEditFields } } }

	// gui call for closing the song

	guiCloseStudio{
		if ((insts.size>0)||(models.collect(_.automation).size>0)) {
			if (network.isConnected) {
				LNX_Request(nil,
					"Close Song Request",
					network.thisUser.shortName+"wants to close this song.\n"++
					"All unsaved changes will be lost.",
					"Decline","Accept",\warning, false ,
					"Close Song Request",
					"You are requesting to close this song.\n"++
					"The rest of the collaboration needs to agree.",
					"Cancel", nil, \wait_0,
					{},
					{
						api.sendOD(\closeStudio);
						this.closeStudio;
					})
			}{
				this.closeStudio;
			}
		};
	}

	// close studio, i.e. delete all instruments and start again

	closeStudio{
		this.stop.stop.resetTime;
		this.clear;
		this.title_("");
		songPath=nil;
	}

	// clear is ALSO used before a new song is loaded

	clear{
		insts.ids.do{|id|
			mixerGUI[id].do(_.remove);
			mixerGUI[id]=IdentityDictionary[];
		}; // remove always on gui
		insts.do(_.stopDSP);
		insts.do(_.free);
		insts.clear; // clear instruments

		LNX_MIDIControl.closeWindow;
		midiControl.clear;
		LNX_Protocols.clearObjects;
		LNX_ID.setNextID(0);
		LNX_MIDIPatch.resetUnused;
		LNX_POP.reset;

		models.do{|model| model.freeAutomation };

		MVC_Automation.updateDurationAndGUI; // temp
		MVC_Automation.resetZoom;

		onSoloGroup.reset; // this does not clear onSolo it's been set after by something else?

		this.checkSyncDelay;

		{LNX_SampleBank.emptyTrash}.defer(7.5); // empty trash 7.5 seconds later
		// needs to be triggered when finished loading

	}

	// and free is not completed yet

	free{ this.clear }

	freeAutomation{ models.do{|model| model.freeAutomation } }

	freeAllAutomation{
		this.freeAutomation;
		insts.do(_.freeAutomation);
		MVC_Automation.updateDurationAndGUI; // temp
	}

	///////////////  info  /////////////////////////

	openHelp{
		if ( (frontWindow.isKindOf(Window))
			or: {frontWindow.isNil}
			or: {frontWindow.helpAction.value}) {
				"LNX_Studio Help".help
		}
	}


	fullDump{
		"// LNX_Studio Dump ///////////".postln;
		this.dump;
		"//////////////////////////////".postln;
		super.dump;
		"//////////////////////////////".postln;
		insts.postList;
		"//////////////////////////////".postln;
		insts.do(_.dump);
	}

	// debugging & network tools //

	a{^insts.visualAt(0)}
	b{^insts.visualAt(1)}
	c{^insts.visualAt(2)}
	n{^network }

	post{|a,b,c| [a,b,c].postln}
	postMe{|a| a.postln}
	postList{|a| a.postList}
	postAll{|...msg| msg.postln}
	postStuff{|a,b,c,d,e| [a,b,c,d,e].do(_.postln)}
	postTime{|a,b,c,d,e| SystemClock.now.postln; [a,b,c,d,e].do(_.postln)}
	postClock{|a| thisThread.clock.post; " : ".post; a.postln}
	postSpecies{|me| me.species.post; " : ".post; me.postln}

	///////////////////////////

	// send a message to the messenger
	talk{|msg,myName| network.room.talk(msg,true,true) }

	// the studio title which is changed when loading or save as...

	title_{|t|
		title=t;
		if (title.size>0) {
			mixerWindow.name_("LNX_Studio - "++title);
		}{
			mixerWindow.name_("LNX_Studio");
		};
		LNX_MIDIControl.updateGUI;
	}

	name{^title}

	name_{|string| this.title_(string) }

	// changing the info in the two text displays above the studio transport controls

	dialog1{|d| if (verbose) {"Dilog 1:".post; d.postln } }
	dialog2{|d| if (verbose) {"Dilog 2:".post; d.postln } }

	// Last Song Filename //////////////////

	// store this filename as the last song that was saved or loaded

	*saveLastFileName{|n| [n].savePref("lastFileName") }

	// get the filename of the last song that was saved or loaded in

	*getLastFileName{
		var pref="lastFileName".loadPref;
		if (pref.isNil) {^nil} {^pref[0]};
	}

	////////////// save ////////////////////

	// save song every 1 min with a new name

	saveInterval{
		var i=1;
		if (tasks[\saveInterval].isPlaying) {
			tasks[\saveInterval].stop;
		}{
			Dialog.savePanel({|path|
				tasks[\saveInterval]=Task({|a,b,c|
					loop {
						if (this.isPlaying) {
							this.save((path+i+(
								Date.getDate.format("%Y-%d-%e %R:%S").replace(":",".").drop(2)))
							);
							i=i+1;
						};
						60.wait;
					};
				},AppClock).start;
			})
		}
	}

	// Save dialog

	saveDialog{
		if (songPath.notNil) {
			var bufferInstBankDict = this.getBufferInstBankDict;
			if (bufferInstBankDict.size>0) {
				this.reviewBufferSaves(bufferInstBankDict,\save);
			}{
				this.save(songPath);
			};
		} {
			this.saveAsDialog
		}
	}

	// Save as... user dialog

	saveAsDialog{
		var bufferInstBankDict = this.getBufferInstBankDict;
		if (bufferInstBankDict.size>0) {
			this.reviewBufferSaves(bufferInstBankDict,\saveAs);
		}{
			Dialog.savePanel({|path| this.save(path)});
		};
	}

	// dictionary of every temp buffer so they can be reviewed for saving

	getBufferInstBankDict{
		var bufferInstBankDict = IdentityDictionary[];
		insts.do{|inst|
			inst.saveBanks.do{|bank|
				var buffers = bank.tempFiles;
				buffers.do{|buffer|
					// at index Buffer is a list of [ inst, bank, BufferProxy ]
					bufferInstBankDict[buffer] = [ inst, bank, bank.samples.indexOf(buffer) ];
				};
			};
		};
		^bufferInstBankDict;
	}

	// save this song as path

	save{|path|
		var f,g,saveList;

		LNX_Studio.saveLastFileName(path);
		songPath=path;
		this.title_(path.basename);
		saveList=this.getSaveList;  // get the saveList
		f = File(path,"w");
			saveList.do({|i|
				f.write(i.asString);
				f.write("\n");
			});
		f.close;
	}

	// get a list that contains all the data needed to reproduce the current song

	getSaveList{
		var saveList, b;
		saveList=List["SC Studio Doc"+version,
					server.options.outDevice.asString,
					server.options.inDevice.asString,
					extClock.asString,
					bpm,
					latency,
					models[\volume].value,
					models[\preAmp].value,
					title.asSymbol,
					insts.size,
					noInternalBuses,
					MVC_Automation.barLength
				];
		saveList=saveList++(insts.visualOrder.collect(_.name));
		saveList=saveList++(midiControl.getSaveList);
		insts.visualOrder.do({|inst|
			saveList=saveList++[inst.class.asString];
			b=inst.realBounds.asArray;
			b[0]=b[0]-osx;
			saveList=saveList++(b); // change
			saveList=saveList++(inst.getSaveList);
		});
		saveList=saveList++LNX_POP.getSaveList;
		saveList=saveList++(List["*** END STUDIO DOC ***"]);
		^saveList
	}

	postSaveList{ this.getSaveList.do(_.postln); ^""} // used for testing

	////////////// load ///////////////////

	// check to see if we can load a song now?

	canLoadSong{
		if ((server.serverRunning) and: {isLoading.not} and: {network.isConnecting.not}) {
			^true
		}{
			^false
		}
	}

	// quick load gui call

	quickLoad{
		var g,l,i,path;
		if (this.canLoadSong) {
			path=LNX_Studio.getLastFileName;
			if ((path.isNil.not)and:{File.exists(path)}) {
				g = File(path,"r");
				l=List[g.getLine];
				if (l[0].documentType=="SC Studio Doc") {
					this.title_(path.basename);
					songPath=path;
					i = g.getLine;
					while ( { (i.notNil)&&(i!="*** END STUDIO DOC ***")  }, {
						l=l.add(i);
						i = g.getLine;
					});
					this.updateOSX; // update the studio window offset
					this.confirmLoadList(l);
				};
				g.close;
			};
		}
	}

	// load previous song if many files selected

	nextSong{
		if (loadPaths.size>1) {
			loadIndex=loadIndex+1;
			this.loadPath(loadPaths.wrapAt(loadIndex));
		}
	}

	// load previous song if many files selected

	previousSong{
		if (loadPaths.size>1) {
			loadIndex=loadIndex-1;
			this.loadPath(loadPaths.wrapAt(loadIndex));
		}
	}

	// load user dialog

	loadDialog{
		if (this.canLoadSong) {
			Dialog.openPanel({ arg paths;
				if (paths.size>1) {
					loadIndex = -1;
					loadPaths = loadPaths ++ (paths.sort);
					// if more than 1 song selected make a menu of them all
					Platform.case(\osx, {
						// on macOS...
						// load previous song
						MainMenu.register( Action("Next song", {
							this.nextSong;
						}).shortcut_("Ctrl+1"),"Songs","Controls");
						// load next song
						MainMenu.register( Action("Previous song", {
							this.previousSong;
						}).shortcut_("Ctrl+2"),"Songs","Controls");
						// load a song
						loadPaths.do{|path,j|
							MainMenu.register( Action(path.basename, {
 								loadIndex=j;
								this.loadPath(path);
							}),"Songs","Songs");
						};
					});
				}{
					// else just load the song
					this.loadPath(paths@0);
				}
			}, multipleSelection: true);
		}
	}

	// load the demo song

	loadDemoSong{|useAsLastOpened=false|
		if (this.canLoadSong) {
			this.loadPath(Platform.lnxResourceDir+/+"demo song",useAsLastOpened);
		};
	}

	loadPath{|path,useAsLastOpened=true|
		var g,l,i;
		g = File(path,"r");
		l=List[g.getLine];
		if (l[0].documentType=="SC Studio Doc") {
			if (useAsLastOpened) {
				LNX_Studio.saveLastFileName(path);
				this.title_(path.basename);
				songPath=path;
			}{
				this.title_(path.basename);
				songPath=nil;
			};

			i = g.getLine;
			while ( { (i.notNil)&&(i!="*** END STUDIO DOC ***")  }, {
				l=l.add(i);
				i = g.getLine;
			});
			this.updateOSX; // update the studio window offset
			this.confirmLoadList(l);
		};
		g.close;
	}

	// when a file is dropped into studio

	dropLoad{|doc|
		var l,name,path;
		if (this.canLoadSong) {
			l=doc.string.split($\n);
			name=doc.name;
			path=doc.path;
			if (l[0].documentType=="SC Studio Doc") {
				if ((path.reverse[..4].reverse)!=".rtfd") {
					LNX_Studio.saveLastFileName(path);
					songPath=path;
				}{
					songPath=nil;
				};
				// do not add if rich text document, this stops the demo song from being over
				// writen, plus a loading bug for rtdf's and saving over old songs with demo
				this.title_(name);
				this.updateOSX; // update the studio window offset
				if (server.serverRunning) {
					this.confirmLoadList(l);
				}{
					songToLoad=l;
				}
			}
		}
	}

	// confirm load 1st if needed

	confirmLoadList{|l|
		if ((network.isConnected)and:{insts.size>0}) {
			LNX_Request(nil,
				"Load Song Request",
				network.thisUser.shortName+"wants to load a song.\n"++
				"All unsaved changes to this song will be lost.",
				"Decline","Accept",\warning, false ,
				"Load Song Request",
				"You are requesting to close this song and load another.\n"++
				"The rest of the collaboration needs to agree.",
				"Cancel", nil, \wait_0,
				{},
				{
					api.sendClumpedList(\netSyncSong,l.copy);
					this.putLoadList(l);
				})
		}{
			if (network.isConnected) { api.sendClumpedList(\netSyncSong,l.copy) };
			this.putLoadList(l);
		};
	}

	// send an entire song (this is only used in syncing a collaboration)
	// because we need to preserve comm ids.
	// else this could be lost when deleting insts beforehand

	syncCollaboration{
		var l;
		l=this.getSaveList;
		l=[insts.size,LNX_ID.queryNextID]++(insts.visualOrder.collect(_.id))++l;
		api.sendClumpedList(\netSyncCollaboration,l);
		// this.netSyncCollaboration(l.copy); // a quick fix.
		// what is this a quick fix for?
		// This breaks ids so why do it?
		LNX_PianoRollSequencer.resetAllNoteIDs; // make sure all noteID in a collab are the same

 	}

	// net of above (new need to preserve the id of objects)

	netSyncCollaboration{|l|
		var noI,ids,newID;
		noI   =l[0].asInt;
		newID =l[1].asInt;
		ids   =l[2..(noI+1)].asInt;
		this.putLoadList(l.drop(noI+2),ids);
		songPath=nil;
		LNX_ID.setNextID(newID); // after load because clear resets nextID in comms

		// make sure all noteID in a collab are the same
		{ LNX_PianoRollSequencer.resetAllNoteIDs }.defer(this.actualLatency+0.2);
	}

	// recieve an entire song, this is called over a network from many places
	// ID's will be preserved in these cases because clear is called and resets IDs

	netSyncSong{|l|
		this.putLoadList(l);
		songPath=nil;
	}

	// put the list into the studio as the new song. must read back in same order as getSaveList

	putLoadList{|l,ids|
		var noInst, instType, header, loadVersion, subVersion, midiLoadVersion;
		if (insts.size<1) {this.updateOSX}; // update the studio window offset
		l=l.reverse;
		header=l.popS;

		loadVersion=header.version;

		if (this.versionAtLeast(loadVersion.asInt,loadVersion.frac.asString.drop(2).asInt)) {

			if (((header.documentType)=="SC Studio Doc")&&(isLoading.not)) {
				// house keeping ahead
				isLoading=true;
				network.stopTimeOut; // stop room time out
				this.stop;	// stop transport
				this.kill; 	// this will stop all note on events

				// now wait until all gui calls, seqs and messages sent before closing

				{
					this.clear;	// clear also does stopDSP on all insts
				}.defer(this.actualLatency+0.05);

				{
					// this.clearShowWindowsOptions; // show all instrument windows
					// close midi
					if (LNX_MIDIControl.window.notNil) {LNX_MIDIControl.window.close};

					this.dialog1("Loading...");
					loadedAction={
						{
							this.selectInst(insts.visualAt(0).id);
							network.startTimeOut;
						}.defer;
						this.dialog1("Finished loading.",Color.white);
						{
							this.dialog1("Studio"+version);
							this.dialog2("April 2016 - l n x");
							isLoading=false;
																			// maybe move this here?
							// insts.do(_.postSongLoad); // after all insts added.

							MVC_Automation.updateDurationAndGUI;

						}.defer(1);

					};

					// this is where i used to change audio device if needed
					if (loadVersion>1.1) { l.popS; l.popS }{ l.popS };

					if (loadVersion>1.0) {
						extClock=(l.popS=="true").if(true,false);
						models[\extClock].value_(extClock.binaryValue);
						this.clockSwap;
						bpm=l.popI;
						models[\tempo].value_(bpm);
						absTime=2.5/bpm;
						l.popF; // pop latency and do nothing with it
						models[\volume].valueAction_(l.popF);
						if (header.subVersion>=3) {
							models[\preAmp].valueAction_(l.popF);
						}{
							models[\preAmp].valueAction_(0);
						};
					};
					// start putting the info in
					title=l.popS;
					noInst=l.popI;
					this.noInternalBuses_(l.popI);

					// use subversion to add bar length to save
					if (header.subVersion>=1) { MVC_Automation.barLength_(l.popI)};

					l.popNS(noInst); // pop inst names (not for use yet.
					midiLoadVersion=l.popS.version;
					midiControl.putLoadList(l.popEND(
						"*** End MIDI Control Doc ***"),midiLoadVersion);
					if (noInst==0) {
						loadedAction=nil;
						this.dialog1("Finished loading.",Color.white);
						{
							this.dialog1("Studio"+version);
							this.dialog2("April 2016 - l n x");
							isLoading=false;
						}.defer(1);
					}{
						this.recursiveLoad(0,l,noInst,loadVersion,ids);
						insts.do(_.postSongLoad); // after all insts added.
						insts.orderEffects; // fix: some effects start in the wrong order
						if (header.subVersion>=2) {
							LNX_POP.putLoadList(l.popEND("***EOD of POP Doc***"));
						};
					};

					// leave inst windows open or close
					if (showNone) {
						{ models[\showNone].doValueAction_(1,nil,false) }.defer(0.1);
					}{
						if (show1) {
							{ models[\show1].doValueAction_(1,nil,false) }.defer(0.1);
						}
					};

				}.defer(this.actualLatency+0.1);
				// if you change this defer value update value in netSyncCollaboration

			};

		}{
			this.addTextToDialog("I can't load LNX"+loadVersion+"songs.",true,true);
		}

	}

	// recursiveLoad is used to load one instrument at a time so the server does get
	// overloaded by messages to load buffers etc.. buffer loading needs to be defered
	// comm ids are either generated within or supplied from a collobration sync

	recursiveLoad{|i,l,noInst,loadVersion,ids|
		var ti, instType,id;

		network.room.broadcastOnce;

		instType=l.popS;
		if (instType[0..3]!="LNX_") { instType="LNX_"++instType }; // temp fix for new names
		if (instType=="LNX_Acid309") { instType="LNX_BumNote"};    // temp fix for bumnote name
		instType=instType.asSymbol.asClass;
		this.dialog1("Reading"+i+(instType.asString));
		if (ids.notNil) {
			id=ids[i]
		}{
			id=LNX_ID.nextID;
		};

		if (loadVersion==1.0) {
			this.addInst(instType,Rect(l.popI+osx,l.popI+osx,100,100),
				false,id:id,loadList:(l.popEND("*** END INSTRUMENT DOC ***"))
			);
		}{
			this.addInst(instType,Rect.fromArray(l.popNI(4)).moveBy(osx,0),
				false,id:id,loadList:(l.popEND("*** END INSTRUMENT DOC ***"))
			);
			// changed;
		};

		if (i<(noInst - 1)) {
			this.recursiveLoad(i+1,l,noInst,loadVersion,ids);
		}{
			loadedAction.value(this);
			loadedAction=nil;
		};

	}

	// onSolo methods.. //////////////////////////////////////////////////////////

	// alt onOff for studio and inst
	// called from LNX_InstrumentTemplate:onOffAlt

	onOffAlt_{|id,v|
		var test=false;
		if (v==1) {
			insts.pairsDo{|iid,inst|
				if ((iid!=id)and:{inst.instOnSolo.on==0}) { test=true }
			};
			if (test) {
				insts.do({|inst| inst.onOff_(1) });
			}{
				insts.pairsDo{|iid,inst| inst.onOff_((iid==id).if(1,0)) };
			};
		}{
			insts[id].onOff_(0);
		}
	}

	// alt solo (alt key/right mouse buttom pressed)
	// called from inst gui LNX_InstrumentTemplate:soloAlt

	soloAlt_{|id,v|
		if (v==1) {
			insts.do{|inst|
				inst.solo_(0);
				inst.onOffEnabled_(false);
			};
			insts[id].solo_(1);
		}{
			insts[id].solo_(0);
			if (onSoloGroup.isSoloOn.not) {
				insts.do{|inst|
					inst.onOffEnabled_(true)
				};
			};
		};
		insts.do(_.stopNotesIfNeeded);
	}

	// update user onSolo and send update to the collaboration
	// to make thing easier i just send everything and update
	// need to test when loading and adding

	sendOnSoloUpdate{
		var dictList=[],userID;
		insts.keysValuesDo{|instID,inst|
			dictList=dictList.add(instID);
			dictList=dictList.add(inst.instOnSolo.on);
			dictList=dictList.add(inst.instOnSolo.solo);
		};
		api.sendOD(\netOnSoloUpdate,network.thisUser.id,network.isListening,*dictList);

		// also need to store for myself
		if (network.isListening) { userID='lnx_song'} {userID=network.thisUser.id};
		dictList.clump(3).do{|l|   					   // [instID,on,solo]
			onSoloGroup.userInstOn_  (userID,l[0],l[1]); // on
			onSoloGroup.userInstSolo_(userID,l[0],l[2]); // solo
		};
	}

	// recieve a onSolo update from another user

	netOnSoloUpdate{|userID,isListening...dictList|
		var isSoloOn;
		dictList=dictList.clump(3);

		if ((isListening.isTrue)and:{network.isListening}) {
			dictList.do{|l,i|
				insts[l[0]].onOff_(l[1]).solo_(l[2]); // [instID,on,solo]
			};
			insts.do(_.stopNotesIfNeeded);
			{
				isSoloOn=onSoloGroup.isSoloOn;
			}.defer;
		};

		if (isListening.isTrue) { userID='lnx_song'};
		dictList.do{|l|   							   // [instID,on,solo]
			onSoloGroup.userInstOn_  (userID,l[0],l[1]); // on
			onSoloGroup.userInstSolo_(userID,l[0],l[2]); // solo
		};

		this.refreshOnOffEnabled;

	}

	// used for changing listening to 'the' song (syncing onOff and solo) over a network

	userHasChangedListening{
		var newOnSoloList;
		if (network.thisUser.isListening) {
			newOnSoloList=onSoloGroup.usersOnSoloList['lnx_song'];
		}{
			newOnSoloList=onSoloGroup.usersOnSoloList[network.thisUser.id];
		};

		newOnSoloList.keysValuesDo{|id,osList|
			insts[id].onOff_(osList[0]).solo_(osList[1]);
		};
		this.refreshOnOffEnabled;
	}

	// update onOff.enabled_ to reflect solo

	refreshOnOffEnabled{
		if (onSoloGroup.isSoloOn) {
			insts.do({|inst| inst.onOffEnabled_(false)});
		}{
			insts.do({|inst| inst.onOffEnabled_(true)});
		};
	}

	// a special method for deleteInst that:
	// 1: clears the solo from the deleted instrument
	// 2: stop solo from send this info over the net and causing another bug. a nil.solo_ method

	soloDelete{|id,v|
		insts[id].solo_(v);
		this.refreshOnOffEnabled;
	}

	// mixer control for launch pad, temp until proper implementation ////////////////////////////////

	// temp for CARBON ************
	// LNX_POP changed
	// LNX_Instruments:move changed
	// LNX_Studio:stop

	initPadMixerMIDI{

		// note value of pads left to right, top to bottom
		padNotes =  #[
//			0, 1, 2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 20, 21, 22, 23,
//			32, 33, 34, 35, 36, 37, 38, 39, 48, 49, 50, 51, 52, 53, 54, 55,
			64, 65, 66, 67, 68, 69, 70, 71, 80, 81, 82, 83, 84, 85, 86, 87,
			96, 97, 98, 99, 100, 101, 102, 103, 112, 113, 114, 115, 116, 117, 118, 119 ];

		midi2 = LNX_POP.midi;

	}

	padMixerNoteOnFunc{|src, chan, note, vel ,latency|
		var inst, index = padNotes.indexOf(note);
		if (index.notNil) {
			inst = insts.mixerInstruments[index];
			if (inst.notNil) {
				inst.onOffModel.lazyValueAction_(1 -(inst.onOffModel.value) );
			};
		};
	}


	updatePadMixer{|inst|
		var index = insts.mixerInstruments.indexOf(inst);
		if (index.notNil and:{padNotes[index].notNil}) {
			if (inst.onOffModel.isTrue) {
				midi2.noteOn(padNotes[index],48, latency) ; // green
			}{
				midi2.noteOn(padNotes[index],0, latency) ; // off
			}
		};
	}

	updateAllPadMixer{ insts.mixerInstruments.do{|inst| this.updatePadMixer(inst) } }

	addMixerPaddDependant{|inst|
		var index = insts.mixerInstruments.indexOf(inst);
		if (index.notNil) {
			inst.onOffModel.addDependant{
				this.updatePadMixer(inst)
			}
		};
	}

	// EXPORT STEMS //
	/*
	a.quickLoad;
	a.loadDemoSong(true);
	a.exportStems;
	*/
	exportStems {|extraTime=3|
		var i = 0,
			n = insts.mixerInstruments.size;

		Dialog.savePanel({ arg path;

			path.mkdir;

			{ this.exportNext(path, i, n, extraTime); }.fork;

		});

	}

	exportNext {|path, i, n, extraTime|
		var inst, path1;
        inst = insts.mixerInstruments[i];
        inst.postln;
        insts.mixerInstruments.do {|inst2|
            if (inst2.id != inst.id) {
                { this.deleteInst(inst2.id) }.defer(0);
            };
        };

        0.1.wait;

        path1 = server.prepareForRecord(
            path +/+
            (inst.id + "-" + inst.name) ++
            "." ++ (server.recHeaderFormat)
        );

        0.1.wait;

        server.record;

        0.5.wait;

        { this.play(LNX_Protocols.latency, beat); }.defer(0);

        MVC_Automation.absDuration.wait;

        { this.pause; }.defer(0);

        extraTime.wait;

        server.stopRecording;

        i = i+1;

        { this.quickLoad }.defer(0);

        if (i < n) {
            0.1.wait;
            while ({ this.isLoading }) { 0.1.wait; };
            this.exportNext(path, i, n, extraTime);
        }
	}

}

  /////////////////////////////////////////////////////////////
  //////////////// END STUDIO  l n x  2015   //////////////////
  /////////////////////////////////////////////////////////////
