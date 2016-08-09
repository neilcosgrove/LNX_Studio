
////////////////////////////////////////////////////////////////////////////////////////        .
//                                                                                    //
// LNX_InstrumentTemplate ( all instruments are made from this )                      //
//                                                                                    //
////////////////////////////////////////////////////////////////////////////////////////

LNX_InstrumentTemplate {

	classvar templateVersion="v1.3", <>noInternalBuses=3;
	classvar <onSoloGroup,			<>verbose=false;
	classvar fxFakeOnOffModel;

	var <lastTemplateLoadVersion=1.3;

	// header vars
	var	<instrumentHeaderType,	<version,			<studioName,
		<thisWidth, 			<thisHeight, 		defaultP;
			
	// inst vars
	var	server,			studio, 			<id,	
		network,			api,
	
		<>models,			<>defaults,
		<instNoModel,		<nameModel,		<controlTitle="",		
		<>p,

		<window,			<>bounds,			<>onOpen,
		<gui,			<hidden=false,	<hiddenBounds,
		<tasks,
		
		<midi, 			<midiControl,
		
		<isLoading=false, 	<>loadedAction,
		
		<notesOn, 		<synthsOn, 		<releaseTimes,
		<instOnSolo,
		
		<presetView,		<presetMemory,	<presetExclusion,
		<>presetNames,	<presetInterface,	<randomExclusion,
		<autoExclusion,	<presetsOfPresets,
		
		<>groups,			<>groupIDs,
		
		// to be replaced by groups & groupIDs above;
		<>lfoGroup,
		<>instGroup, 		<>instGroupID,
		<>instOutGroup,	<>instOutGroupID,	<>instOutSynth,
		<>fxGroup, 		<>fxGroupID,
		<>gsrFilterGroup,	<>gsrFilterGroupID,
		
		scCodeGroup, 		scCodeGroupID,  // scCode has it' own group to help io
		<synth,            <node,
		<bpm=120,			<absTime=0.5,
		
		<hack,			<>toFrontAction,
		
		<peakLeftModel,	<peakRightModel,
		
		<eq;
	
	var <syncDelay=0;
				
	////////////////////////////////////////
	//                                    //
	//// init stuff ////////////////////////
			
	// init the class
	*initClass {
		Class.initClassTree(LNX_OnSoloGroup);
		onSoloGroup=LNX_OnSoloGroup();
		onSoloGroup.addUser('lnx_song'); // add a fake user to use in a collaboration
		fxFakeOnOffModel = 1.asModel;
	}
	
	// post studio server init (useful for loading LNX_BufferProxys)
	*initServer{|server| }

	// initialise all services
	init {|argServer, argStudio, argInstNo, argBounds, new, argID, loadList|
		studio=argStudio;
		bounds=argBounds;
		server=argServer;
		id=argID;
		network=studio.network;
		
		if (loadList.size==0) { loadList=nil };
			
		// this is needed early on
		instNoModel=(argInstNo?0).asModel
			.action_{|me,val|
				//update ON/off buttons
				models[1].dependantsPerform(\strings_,(val+1).asString);
				this.updateWindowName;
			};
		
		// the network interface for all instruments 
		api=LNX_API.newTemp(this,id,#[
			\netMidiChange, \netNewMidiControlAdded, \netName_, \hostAddPreset,
			\netAddPresetList, \hostSavePreset, \netSavePresetList, \netLoadPreset,
			\removePreset, \removeAllPresets, \netRenamePreset, \netOnOff, \netSolo,
			\netSetP, \netSynthArg, \netSynthArgGUI, \netPReplace, \netSetPVPModel, \netSetPOP
		], this.interface);
	
		this.header;				// header info for inst
		this.initMIDI;			// i need this 1st becuase of midi in the model
		this.initPreModel; 		// anything needed before the models are made
		this.initModel;    		// subclass override this
		this.initGroups;			// start groups
		this.initVars;			// init superclass vars
		this.iInitVars;			// init instrument vars
		this.iInitMIDI;			// start midi
		this.createWindow(bounds);	// create window (this doesn't open it)
		this.createWidgets;		// create the widgets for it
		
		studio.insts.addInst(this,id);		// add to instruments
		studio.createMixerInstWidgets(this);	// make the mixer widgets
		
		if (loadList.notNil) { this.putLoadList(loadList,updateDSP:false)} {this.noPutList};
		
		this.startInstOutDSP;		// start the instrument out group if it has one
		this.startDSP;			// start any dsp
		this.updateDSP;			// update that dsp
		this.iPostInit;			// anything that needs doing after init
		this.deferOpenWindow;		// now open window after a defer to help stop lates
		if (new) { this.iPostNew };	// anything that needs doing after new creation
		this.eqName;

	}
	
	noPutList{} // used in LNX_CodeFX to do guiEvaluate on new instrument
	
	// initialise all variables here	
	initVars{
		
		studioName=this.class.studioName; // needed as ref ?
		
		nameModel=studioName.copy.asModel
			.actions_(\stringAction,{|me|
				this.nameInst_(me.string);
				MVC_Automation.refreshGUI;	
			})
			.maxStringSize_(25);
		
		thisWidth =this.class.thisWidth;
		thisHeight=this.class.thisHeight;
		notesOn=[];
		synthsOn=[];
		releaseTimes=[];
		presetMemory=[];
		presetNames=[];
		p=defaults.copy;  // still need for extending old versions
		gui=IdentityDictionary[];
		tasks=IdentityDictionary[];
		instOnSolo=LNX_OnSolo.new(onSoloGroup,p[1],p[0],id);   // for instruments
		bpm=studio.bpm;
		absTime=60/bpm;
		controlTitle="Instrument \""++(this.name)++"\" ("++(this.studioName)++")";
		
		peakLeftModel = [\unipolar].asModel.fps_(20); // fps same as in LNX_Studio:initUGens
		peakRightModel = [\unipolar].asModel.fps_(20); // fps same as in LNX_Studio:initUGens
		
		autoExclusion.do{|i| models[i].automationActive_(false) };
		
		// hack action!!
		hack=IdentityDictionary[];
		if (this.volumeModel.notNil) {
			this.volumeModel.hackAction_{|model,button|
				if (button==1) {
					this.setPeakLevel(model.get)
				};
			};
		};
		
		presetsOfPresets = LNX_POP(this,api);
		
		// add eq if needed
		if (this.isMixerInstrument) {
			eq = LNX_EQ(server, groups[\eq], this.instGroupChannel, ('inst_'++id++'_eq').asSymbol,
				bounds: Rect(950, 25 + (studio.insts.mixerInstruments.size*24), 325, 235),
				midiControl:midiControl
			)
		};

	}
	
	// eq stuff
	openEQ{ if(eq.notNil) { eq.open } }
	openEQIfOn{ if(eq.notNil) { eq.openIfOn } }
	closeEQ{ if(eq.notNil) { eq.close } }
	freeEQ{  if(eq.notNil) { eq.free; eq = nil }  }
	eqOnOffModel{ if(eq.notNil) { ^eq.onOffModel}{ ^nil } }
	eqName{
		if(eq.notNil) {
			var name = (instNoModel.value+1).asString++"."+(nameModel.string);
			eq.name_(("[EQ] "++name) );
		}
	}
	
	// groups
	initGroups{
		lfoGroup         = studio.lfoGroup;
		instGroup        = studio.instGroup;
		gsrFilterGroup   = studio.gsrFilterGroup;
		instOutGroup     = studio.instOutGroup;
		fxGroup          = studio.fxGroup;
		scCodeGroup      = studio.scCodeGroup;
		
		instGroupID      = instGroup.nodeID;
		gsrFilterGroupID = gsrFilterGroup.nodeID;
		instOutGroupID   = instOutGroup.nodeID;
		fxGroupID        = fxGroup.nodeID;
		scCodeGroupID    = scCodeGroup.nodeID;
		
		groups = studio.groups;
		groupIDs = studio.groupIDs;
		
	}
	
	// instrument levels
	// synthDef for groups out in LNX_Studio. Easier becasue only done once
	
	hasLevelsOut{^false}
	
	startInstOutDSP{
		if (this.hasLevelsOut) {
			instOutSynth = Synth.tail(instOutGroup,"LNX_InstOut",
				[\inChannel, this.instGroupChannel, \outChannel, 0, \id, id]);
		};
	}
	
	restartEQ{ if (this.hasEQ) { eq.restartDSP(groups[\eq]) } }
	
	instGroupChannel{ ^LNX_AudioDevices.instGroupChannel(id) }
		
	// set mixer synth arg
	setMixerSynth{|synthArg,value,latency|
		if (instOutSynth.notNil) {
			server.sendBundle(latency +! syncDelay,
				[\n_set, instOutSynth.nodeID, synthArg, value]);
		};
	}

	peakOutLeft_{|value| peakLeftModel.lazyValueAction_(value,0) }
	peakOutRight_{|value| peakRightModel.lazyValueAction_(value,0) }
		
	///////////// sync stuff ///////////////
	
	instLatency{ ^studio.actualLatency + syncDelay } // actual latency of this inst
		
	// set the sync time
	syncDelay_{|val|
		syncDelay = val;
		studio.checkSyncDelay; // studio will call stopAllNotes to all inst
		if (eq.notNil) { eq.syncDelay_(val) };
	}
		
	////////////////////////////////////////
	//                                    //
	//// MIDI stuff ////////////////////////
	
	// create both midiIn&Out & midiControl
	initMIDI{
		midi=LNX_MIDIPatch(0,0,0,0); // all midi in&out for this inst and their default settings
		midi.noteOnFunc  = {|src, chan, note, vel ,latency| this.noteOn (note, vel,  latency)};
		midi.noteOffFunc = {|src, chan, note, vel ,latency| this.noteOff(note, vel,  latency)};
		midi.controlFunc = {|src, chan, num,  val ,latency| this.control(num,  val,  latency)};
		midi.bendFunc    = {|src, chan, bend      ,latency| this.bend   (bend     ,  latency)};
		midi.touchFunc   = {|src, chan, pressure  ,latency| this.touch  (pressure ,  latency)};
		midi.programFunc = {|src, chan, prog      ,latency|
											      this.midiSelectProgram(prog ,latency)};
		midi.sysexFunc   = {|src, data, latency|            this.sysex(data, latency) };
		midi.internalFunc= {|...msg| this.internal(*msg); this.midiInternal(*msg) };
		midi.action      = {|me| api.sendClumpedList(\netMidiChange,me.getSaveList) };
		midiControl=LNX_MIDIControl(this); // for midi controls only
	}
	
	// used by keyboard controller to add presets
	internal{|command,arg1,arg2| if (command==\addPrest) { this.guiAddPreset } }
	
	// this tells the midi control editor which patches to edit 
	controlPatches{ ^[midiControl] }
	
	// midi in/out device change sent over net
	netMidiChange{|l| midi.putNetChangeList(l) }
	
	// called from midiControl to inform a new control has been added and needs to sent over net
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
	
	// midi In select program also called by studios auto map midi controller
	midiSelectProgram{|prog,latency|
		this.selectProgram(prog ,latency);
		this.program(prog,latency);
	}
	
	// use the new midi pipes and not the old style midi functions
	useMIDIPipes{
		midi.pipeFunc_{|pipe| this.pipeIn(pipe) };
		midi.noteOnFunc  = nil;
		midi.noteOffFunc = nil;
		midi.controlFunc = nil;
		midi.bendFunc    = nil;
		midi.touchFunc   = nil;
		midi.programFunc = nil;
	}
		
	///////////////////////////////////////////////////
	//                                               //
	////  name and instrument numbers /////////////////
	
	// name of instrument
	name{ ^nameModel.string }
	
	// change the name of this inst
	name_{|string,send=false|
		string=string.asString;
		nameModel.string_(string);
		this.nameInst_(string,send)
	}
	
	// split name_ & nameInst_ so nameModel doesn't set itself & you loose cursor pos in textView
	nameInst_{|string,send|
		send=send?true;
		string=string.asString;
		this.updateWindowName;
		controlTitle="Instrument \""++string++"\" ("++(this.studioName)++")";
		LNX_MIDIControl.updateGUI; // update this
		this.changed(\name,id,this.name); // update the studio
		if (send) {
			api.sendOD(\netName_,string);
		};
	}

	// net version of above
	netName_{|string|
		string=string.asString;
		controlTitle="Instrument \""++string++"\" ("++(this.studioName)++")";
		{
			nameModel.string_(string);
			//window.name_(string);
			this.updateWindowName;
			LNX_MIDIControl.updateGUI; // update this
			this.changed(\name,id,this.name); // update the studio
		}.defer;	
	}
	
	// update the name of the window
	updateWindowName{
		var name = (instNoModel.value+1).asString++"."+(nameModel.string);
		window.name_(name);
		this.eqName;
	}
	
	// if you select another field the editing and selection remains in other fields
	// this removes it
	resetNameEditFields{
		nameModel.dependantsPerform(\clearRangeSize).dependantsPerform(\clearEditMode)
	}
	
	// this inst number
	instNo{^instNoModel.value}
	
	// used to renumber inst after a different 1 has been deleted. also important for ons & solos
	instNo_{|i| instNoModel.valueAction_(i) }
	
	// set user icon color for this, nil = no icon attached
	userIcon_{|color| instNoModel.dependantsPerform(\addColor_   ,'icon', color) }
	
	// set if this instrument is selected by the user
	selected_{|bool|  instNoModel.dependantsPerform(\selected_, bool) }
	
	///////////////////////////////////////////////////
	//                                               //
	//// Misc Stuff ///////////////////////////////////
		
	// set bmp
	bpm_{|value|
		bpm=value;
		absTime=60/bpm;
		this.bpmChange(studio.actualLatency); // i need to think about the source
	}
	
	// used when the sound device is changed
	serverReboot{
		notesOn=[];
		synthsOn=[];
		releaseTimes=[];
		this.iServerReboot;
	}
	
	// used to clear this inst before loading a new one into it
	clear{ 
		presetMemory=[];
		midiControl.clear;
		this.iClear;
	}
	
	// free all automation from the inst models
	freeAutomation{
		models.do(_.freeAutomation);
		this.iFreeAutomation;	
	}
	
	// free up this instrument/child
	free{
		
		this.iFree;
		
		studio.midiCnrtLastNote.removeAll(this); // not the best place but the easiest to do
		
		presetsOfPresets.free;
		instOutSynth.free;
		api.free;
	 	midi.free;
	 	midiControl.free;
	 	instOnSolo.free;
	 	
	 	//this.iFree;
		
		this.freeWindow;
		gui.do{|i| if (i.isKindOf(Collection)) { i.do(_.free) } { i.free } };
		nameModel.free;
		instNoModel.free;
		models.do(_.free);
		fxFakeOnOffModel.free;
		tasks.do(_.stop);
		this.freeEQ;
		
		{
			presetsOfPresets = tasks = models = defaults = nameModel = api =
			instrumentHeaderType = version = studioName =thisWidth =thisHeight = defaultP =
			server = studio = id = instNoModel = network = window = bounds = gui = 
			hidden = hiddenBounds = controlTitle = p = midi = midiControl = isLoading =
			loadedAction = notesOn = synthsOn = releaseTimes = instOnSolo = presetView =
			presetMemory = presetExclusion = randomExclusion = presetNames = presetInterface =
			instGroup = instGroupID = fxGroup = fxGroupID = synth = node = bpm = absTime= nil;
		}.defer(1);
	 }
	 
	  ///////////////////////////////////////////////////////////////////////////////////////
	 //                                                                                   //
	////////////////////////////////// WINDOW methods /////////////////////////////////////

	createTemplateWindow{|argBounds,background,resizable=false,scroll=false|
		bounds = bounds ? argBounds;
		bounds = bounds.setExtent(thisWidth,thisHeight);
		background=background?Color.grey;
		window = MVC_Window(((this.instNo+1).asString++"."+this.name),
					bounds, resizable: resizable, scroll: scroll);
		window.color_(\background,background);
		
	}
	
	// this is defered to help reduce cpu load and lates to the server
	deferOpenWindow{
		{
			window.create;
			this.attachActionsToPresetGUI ; // this is a temp fix
			onOpen.value;
			onOpen=nil;
			window.toFrontAction_{
				studio.frontWindow_(window); 
				if(this.isOpenAndHidden) {this.show};
				studio.hilightInst(id);
				toFrontAction.value;
			};
			
			toFrontAction.value;
			
		}.defer(0.01);
	}

	// open or reopen the instrument window
	openWindow{ window.open }
	
	// window front
	front { window.front }
	
	// used by studio to hide this instrument
	hide{ window.hide }
	
	// used by studio to show the instrument when hidden
	show{ window.show }
	
	// tell the studio if this instrument is open & hidden
	isOpenAndHidden{ ^window.isOpenAndHidden }
	
	// tell the studio if this instrument is visible on the screen
	isVisible{ ^window.isVisible }
	
	// what is/was the current/last actual bounds this instrument has/had
	realBounds{ ^window.bounds } // this is redundant: MVC always returns real bounds
	
	// close that window its in my way (but really we're hiding it)
	closeWindow{ window.hide }
	
	// this is closing really
	freeWindow{ window.free }
	
	////////////////////////////////////////////////////////////////////////////////////////
	//                                                                                    //
	// Presets                                                                            //
	//                                                                                    //
	////////////////////////////////////////////////////////////////////////////////////////

	// get the current state as a list
	getPresetList{
		if (this.hasEQ) {
			^[this.class.asSymbol] ++ p ++ (eq.getPresetList) ++(this.iGetPresetList) // EQ **
		}{
			^[this.class.asSymbol] ++ p ++(this.iGetPresetList)
		};
	}
	
	// the gui side of adding a preset
	guiAddPreset{ api.hostCmdGD(\hostAddPreset) } // host this command
	
	// the host cmd, this is done by host which saves on larger lists been sent twice
	hostAddPreset{
		var l;
		if (presetMemory.size<128) {
			l=this.getPresetList;
			this.addPresetList(l);
			if (network.isConnected) {
				api.sendClumpedList(\netAddPresetList,l.compress); // net this
			};		
		}
	}
	
	// the list is compressed so we need to decompress before we use it
	netAddPresetList{|l| this.addPresetList(l.decompress) }
	
	// add a statelist to the presets
	addPresetList{|l|
		l=l.reverse;
		if (this.class.asSymbol==(l.pop.asSymbol)) {
			presetMemory=presetMemory.add(l.popN(p.size));
			
			if (this.hasEQ) { eq.addPresetList(l.popNF(eq.pSize)) }; // EQ **
			
			presetNames=presetNames.add("Preset"+((presetMemory.size- 1).asString));
			if (presetView.notNil) {
				
				// update preset gui
				
				this.updatePresetNames;
				presetView.value_(presetMemory.size- 1);
			};
			this.iAddPresetList(l);
		};
	}

	// again the gui side of saving these setting over the current preset
	guiSavePreset{|i|  api.hostCmdGD(\hostSavePreset,i) } // host this command
	
	// the host cmd, this is done by host which saves on larger lists been sent twice
	hostSavePreset{|uid,i|
		var l;
		if (i.isNumber) {
			l=this.getPresetList;
			this.savePresetList(i,l);
			if (network.isConnected) {
				api.sendClumpedList(\netSavePresetList,[i]++(l.compress))
			};
		}
	}	
	
	// the list is compressed so we need to decompress before we use it
	netSavePresetList{|l| this.savePresetList(l[0],l.drop(1).decompress) }
	
	// save a state list over a current preset
	savePresetList{|i,l|
		if (i<presetMemory.size) {
			l=l.reverse;
			if (this.class.asSymbol==(l.pop.asSymbol)) {
				presetMemory=presetMemory.put(i,l.popN(p.size));
				
				if (this.hasEQ) { eq.savePresetList(i,l.popNF(eq.pSize)) }; // EQ **
				
				this.iSavePresetList(i,l);
			};
		}
	}
	
	// load up that preset (this is overriden with own version in melody maker)
	loadPreset{|i,latency|
		var presetToLoad, oldP;
		
		presetToLoad=presetMemory[i].copy;
		// exclude these parameters
		presetExclusion.do{|i| presetToLoad[i]=p[i]};
		// update models
		presetToLoad.do({|v,j| if (p[j]!=v) { models[j].lazyValueAction_(v,latency,false) } });
		
		if (this.hasEQ) { eq.loadPreset(i,latency) }; // EQ **
		
		this.iLoadPreset(i,presetToLoad,latency);    // any instrument specific details
		oldP=p.copy;
		p=presetToLoad;               // copy the paramaters to p (is this needed any more?)
		this.updateDSP(oldP,latency); // and update any dsp
		
		// if older version set current volume as peak level
		if (lastTemplateLoadVersion<1.2) { this.setAsPeakLevel };
		
	}
	
	// gui side of loading presets
	guiLoadPreset{|i|
		this.loadPreset(i);
		if (network.isConnected) { api.sendOD(\netLoadPreset,i) };
	}
	
	// net version also updates preset gui
	netLoadPreset{|i|
		if ((window.isClosed.not)&&(presetView.notNil)) { presetView.value_(i) };
		this.loadPreset(i);
	}
	
	// deleting presets
	removePreset{|i|
		if (presetMemory.size>i) {
			presetMemory.removeAt(i);
			
			if (this.hasEQ) { eq.removePreset(i) }; // EQ **
			
			this.iRemovePreset(i);
			presetNames.removeAt(i);
			if (presetView.notNil) {
				
				// update preset gui
				
				this.updatePresetNames;
				presetView.value_(nil);
			};
		}
	}
	
	// and the gui bit
	guiRemovePreset{|i|
		if ((i.isNumber)&&(i<(presetMemory.size))) {
			this.removePreset(i);
			if (network.isConnected) { api.sendOD(\removePreset,i) };
			//presetView.refresh;
		}
	}
	
	// gui call for remove all presets
	guiRemoveAllPresets{
		this.removeAllPresets;
		if (network.isConnected) { api.sendOD(\removeAllPresets) };
	}
	
	// remove all + also net version
	removeAllPresets{
		presetMemory=[];
		presetNames=[];
		
		if (this.hasEQ) { eq.removeAllPresets }; // EQ **
		
		this.iRemoveAllPresets;
		if (presetView.notNil) {
			
			// update preset gui
			
			this.updatePresetNames;
			presetView.value_(nil);
		};
	}
	
	// gui call
	guiRenamePreset{|i,n|
		this.renamePreset(i,n);
		if (network.isConnected) { api.sendVP(("pn"++id).asSymbol,\netRenamePreset,i,n) };
	}
	
	// rename preset	
	renamePreset{|i,n|
		presetNames[i]=(n.asString);
		this.updatePresetNames;
	}
	
	// net version
	netRenamePreset{|i,n|
		this.renamePreset(i,n);
	}
	
	// random preset ( this could be done in a more efficient way over the network )
	randomisePreset{
		models.do{|m,i|
			if (randomExclusion.includes(i).not) {
				m.randomise
			};
		};
		this.iRandomisePreset
	}	
		
	// change to new preset program
	selectProgram{|prog|
		if (presetMemory.notEmpty) {
			prog=prog.clip(0,presetMemory.size-1);
			if (prog<(presetMemory.size)) {
				this.guiLoadPreset(prog);
				{
					if (presetView.notNil) { presetView.value_(prog) };
				}.defer;
			}
		}
	}
	
	// pop selection of preset/program
	popSelectProgram{|prog,latency|
		
		if (presetMemory.notEmpty) {
			prog=prog.clip(0,presetMemory.size-1);
			if (prog<(presetMemory.size)) {
				this.loadPreset(prog,latency); // with latency for 1st time, cascade to all insts
				{ if (presetView.notNil) { presetView.value_(prog) } }.defer;
			}
		}
	}
	
	// turn on and off via pop (presets of presets)
	popOnOff_{|value,latency|	
		if (this.canTurnOnOff) {
			(this.fxOnOffModel ? this.onOffModel).lazyValueAction_(value,latency,false,false)
		};
	}
	
	// create gui widgets for pop (presets of presets)
	createPOPWidgets{|window,gui|  presetsOfPresets.createWidgets(window,gui) } 
	
	// net set pop value
	netSetPOP{|index,value| presetsOfPresets.netSetPOP(index.asInt,value.asInt) }

	///////
	
	attachActionsToPresetGUI{
		if (presetView.notNil) {
			presetView
				.items_(presetNames)
				.action_      {|i|   this.guiLoadPreset(i)}
				.renameAction_{|i,n| this.guiRenamePreset(i,n)}
				.addAction_   {      this.guiAddPreset }
				.removeAction_{|i|   this.guiRemovePreset(i)}
				.writeAction_ {|i|   this.guiSavePreset(i)}
				.clearAction_ {      this.guiRemoveAllPresets}
				.randomAction_{      this.randomisePreset}
				.midiAction_  {
					var col;
					col=window.colors[\background].alpha_(1).blend(Color.white,0.85);
					this.createMIDIInModelWindow2(window,
						(background:col,border2:col/2,border1:col/8)
					);
				}
				.isFX_(this.isFX);
		};
	}
	
	updatePresetNames{
		{
			if ((window.isClosed.not)&&(presetView.notNil)) {
				presetView.items_(presetNames);
			};
			presetsOfPresets.items_(presetNames);
		}.defer
	}
	

	////////////////////////////////////////////////////////////////////////////////////////
	//                                                                                    //
	// DISC IN/OUT                                                                        //
	//                                                                                    //
	////////////////////////////////////////////////////////////////////////////////////////

	////////////// save ////////////////////
		
	// generate the save list containing all the info to recreate this instrument
	// the putLoadList method must read the info in the same order
	getSaveList{
		var saveList;
		// the header
		saveList=[instrumentHeaderType+version,
					"Template Version"+templateVersion,
					this.species.asString,
					this.name.asSymbol,
					p.size,
					presetMemory.size
				];
		//master
		saveList=saveList++p;
		saveList=saveList++(presetMemory.flat);
		saveList=saveList++(presetNames            );
		saveList=saveList++(midi.getSaveList       );
		saveList=saveList++(midiControl.getSaveList);
		if (this.hasEQ) { saveList=saveList++(eq.getSaveList) }; // eq 
		saveList=saveList++(this.iGetSaveList);	// all other inst stuff here
		// end of document
		saveList=saveList++(["*** END INSTRUMENT DOC ***"]);
		^saveList
	}
	
	// make sure if adding to library we save with solo off and on on, no controls & no automation
	getSaveListForLibrary{
		var saveList;
		// the header
		saveList=[instrumentHeaderType+version,
					"Template Version"+templateVersion,
					this.species.asString,
					this.name.asSymbol,
					p.size,
					presetMemory.size
				];
		//master
		saveList=saveList++p;
		saveList.put(6,0).put(7,1); // a little hacky but what the hell
		saveList=saveList++(presetMemory.flat);
		saveList=saveList++(presetNames            );
		saveList=saveList++(midi.getSaveList       );
		saveList=saveList++(midiControl.getSaveListForLibrary); // not controls or automation
		if (this.hasEQ) { saveList=saveList++(eq.getSaveList) }; // eq 
		saveList=saveList++(this.iGetSaveList);	// all other inst stuff here
		// end of document
		saveList=saveList++(["*** END INSTRUMENT DOC ***"]);
		^saveList; // a little hacky but what the hell
	}
	
	////////////// load ///////////////////
		
	// put all that info back into this instrument.
	// this doesn't create a new instrument
	// Studio creates the instrument 1st, this just does the loading
	putLoadList{|l,updateDSP=true|
		
		var n,noP,noPre,tempP, header, loadVersion, templateLoadVersion, midiLoadVersion;
		var midiContolList;
		
		l=l.reverse; // reverse the list so we can pop things off in order
		
		header=l.popS;
		
		if ((header.documentType)==instrumentHeaderType) {
		
			isLoading               = true;
			loadVersion             = header.version;
			templateLoadVersion     = l.popS.version;
			lastTemplateLoadVersion = templateLoadVersion;
			
			l.pop; // ingore object type, was used for loading inst presets but not any more
		
			this.name_(l.popS,false); // false = don't send over network
					
			noP   = l.popI;	// pop number of elements in p
			noPre = l.popI;	// pop number of presets

			// am i ever going to need this?
			this.clear; // clear presets, midi controls and call .iClear

			tempP = l.popNF(noP); // read tempP now, for use after midi loaded

			// now put in the presets	
			presetMemory = 0!noPre;
			noPre.do({|i|
				var temp;
				temp = l.popNF(noP);
				temp = (temp++defaults[(temp.size)..(defaults.size)])[0..(defaults.size-1)];
				
				// extend older versions with deault P and clip any extra
				presetMemory[i] = temp;
			});
			presetNames = l.popNS(noPre);

			// put in midi
			midi.putLoadList(l.popNI(4));
			
			// extend older versions with deault P and clip any extra
			tempP = (tempP++defaults[(tempP.size)..(defaults.size)])[0..(defaults.size-1)];
			
			// now use tempP
			// pop & adjust in preLoadP if needed, used to change p in LNX_BumNote2:preLoadP only
			tempP = this.preLoadP(tempP,loadVersion);
			
			// and midi controls but do later...
			midiLoadVersion = l.pop.version;
			midiContolList = l.popEND("*** End MIDI Control Doc ***");
			
			// the mixer eq
			if (this.hasEQ) {
				if (lastTemplateLoadVersion>=1.3) {
					eq.putLoadList(l.popEND("*** END LNX_EQ DOC ***"))
				}{
					eq.noSaveList(noPre);		
				}
			};
			
			l = this.iPutLoadList(l,noPre,loadVersion,templateLoadVersion); // for instance
								
			this.onOffModel.valueAction_(tempP[1],nil,false,false);
			this.soloModel.valueAction_(tempP[0],nil,false,false);
			
			this.updateGUI(tempP); // before p=tempP, this calls the models with lazyValueAction
			
			// update preset gui
			if (presetView.notNil) {this.updatePresetNames; presetView.refresh};
			
			p = tempP; // this might not be needed now, but is safe
			
			this.iPostLoad(noPre,loadVersion,templateLoadVersion);
			
			if (updateDSP) {this.updateDSP};  // will this ever happen now? i don't know
			
			// now we add midi controls
			midiControl.putLoadList(midiContolList,midiLoadVersion); // connect at end
			
			// if old use vol as peak
			if (lastTemplateLoadVersion<1.2) { this.setAsPeakLevel(false) }; 
			
			// finish		
			isLoading=false;
			loadedAction.value(this);
			
		};
	}
	
	preLoadP{|l,loadVersion| ^l} // used to change p in LNX_BumNote2:preLoadP
	
	// update the gui during loading (this needs to be change to protect against window closure)
	// and networking
	// and network messages been sent when should and failing silent
	
	// I need to rethink this in light of MVC. maybe just lazyValueAction_
	// also in loadPreset_
	
	updateGUI{|tempP|
		tempP.do{|v,j| if (p[j]!=v) { models[j].lazyValueAction_(v,send:false) } };
		this.iUpdateGUI(tempP);	
	}
	
	// anything that needs doing after the entire song has been loaded and id's assigned to insts
	postSongLoad{|offset| this.iPostSongLoad(offset) }

	////////////////////////////////////////////////////////////////////////////////////////
	//                                                                                    //
	// instrument onOff & Solo functionality                                              //
	//                                                                                    //
	////////////////////////////////////////////////////////////////////////////////////////

	// onSoloGroup is a classVar so returns it for the instances
	
	onSoloGroup{^onSoloGroup} 

	isOn {^instOnSolo.isOn} // easier to remember
	isOff {^instOnSolo.isOff} // easier to remember

	// these methods are called from this instruments gui
	
	// call from onOff GUI (left click)
	onOff {|v,latency,send=true,toggle=false|
		var doISet=true;
		p[1]=v;
		instOnSolo.on_(v);
		this.stopNotesIfNeeded(latency);
		// if (send) mean this could have come from external or internal controllers
		if (send) {
			if (network.isListening) {
				api.groupCmdOD('netOnOff',v,studio.network.thisUser.id,network.isListening);
				// network this
			}{
				if (network.isConnected) {
					onSoloGroup.userInstOn_(studio.network.thisUser.id,id,v);
					doISet=false;  // to stop set of groupSong when not listening to it
				};
				// store your own user onSolos
			};
		}{
			// else do nothing. we don't know the correct state the groupSong should be in
			// this is a result of not knowing if the simpleSeq is onOff in the groupSong
			// will have to wait to do a master sequencer for all instruments
		};
		
		if (doISet) {
			if (toggle) {
				onSoloGroup.userInstOn_('lnx_song',id,1 - onSoloGroup.userInstOn('lnx_song',id)); 
			}{
				onSoloGroup.userInstOn_('lnx_song',id,v); // always set the groupSong
				// this is the only solution i can see at the moment
			}
		};
	}
	
	// network version of onOff
	netOnOff {|v,userID,userIsListening|	
		if (userIsListening.isTrue) {	
			if (network.isListening) {
				this.onOffModel.lazyValue_(v,false);
				p[1]=v;
				instOnSolo.on_(v);
				this.stopNotesIfNeeded; // no latency
			};
			onSoloGroup.userInstOn_('lnx_song',id,v);
		}{
			onSoloGroup.userInstOn_(userID,id,v);
		};
	}
	
	// call from onOff GUI (right click)
	onOffAlt {|v|
		studio.onOffAlt_(id,v);
		if (network.isConnected) {studio.sendOnSoloUpdate};
	}

	// call from solo GUI (left click)
	solo {|v,latency,send=true,toggle=false|
		var doISet=true;
		p[0]=v;
		instOnSolo.solo_(v);
		{studio.refreshOnOffEnabled}.defer;
		this.stopNotesIfNeeded(latency);
		if (send) {
			if (network.isListening) {
				// network this
				api.groupCmdOD('netSolo',v,studio.network.thisUser.id,network.isListening);
				//onSoloGroup.userInstSolo_('lnx_song',id,v);
			}{
				if (network.isConnected) {
					onSoloGroup.userInstSolo_(studio.network.thisUser.id,id,v);
					doISet=false;
				};
			}; 
		};	
		
		if (doISet) {
			if (toggle) {
				onSoloGroup.userInstSolo_('lnx_song',id,
					1 - onSoloGroup.userInstSolo('lnx_song',id)); 
			}{
				onSoloGroup.userInstSolo_('lnx_song',id,v); // always set the groupSong
				// this is the only solution i can see at the moment
			}
		};
	}
	
	// net version of solo
	netSolo {|v,userID,userIsListening|
		if (userIsListening.isTrue) {	
			if (network.isListening) {
				models[0].lazyValue_(v,false);
				p[0]=v;
				instOnSolo.solo_(v);
				studio.refreshOnOffEnabled;
				this.stopNotesIfNeeded; // no latency
			};
			onSoloGroup.userInstSolo_('lnx_song',id,v);
		}{
			onSoloGroup.userInstSolo_(userID,id,v);
		};
	}

	// call from solo GUI (right click) Do this in studio
	soloAlt {|v|
		studio.soloAlt_(id,v);
		if (network.isConnected) {studio.sendOnSoloUpdate};
	}

	/// these are called from the studio's copy of onOff & solo gui's ////////////////////////////

	toggleOnOff{ this.onOff_(1-p[1]) }

	onOff_{|v|	
		p[1]=v;
		instOnSolo.on_(v);
		this.onOffModel.lazyValue_(v,false);
		this.stopNotesIfNeeded;
	}
	
	solo_{|v|
		p[0]=v;
		instOnSolo.solo_(v);
		models[0].lazyValue_(v,false);
		this.stopNotesIfNeeded;
	}
	
	onOffEnabled_{|v|
		this.onOffModel.enabled_(v);
		this.stopNotesIfNeeded;
	}
	
	// used for noteOff in sequencers
	// efficiency issue: this is called 3 times in alt_solo over a network
	stopNotesIfNeeded{|latency|
		if (instOnSolo.isOff) {this.stopAllNotes};
		this.updateOnSolo(latency);
	}
	
	// set inst parameters from gui and net ////////////////////////////////////////////////
	
	// can go missing
	
	setP{|index,value,latency,send=true|
		if (p[index]!=value) {
			p[index]=value;
			if (send) {
				//"-setP-------------------------------------------------".postln;
				api.send('netSetP',index,value);
				//thisThread.dumpBackTrace;
				//"--------------------------------------------------".postln;
			}
		}
	}
	
	// is Guaranteed
	
	setPGD{|index,value,latency,send=true|
		if (p[index]!=value) {
			p[index]=value;
			if (send) {
				//"-setPGD-------------------------------------------------".postln;
				api.sendGD('netSetP',index,value);
				//thisThread.dumpBackTrace;
				//"--------------------------------------------------".postln;
			}
		}
	}
	
	// is guaranteed in Order sent
	
	setPOD{|index,value|
		p[index]=value;
		//"-setPOD------------------------------------------------".postln;
		api.sendOD('netSetP',index,value);
		//thisThread.dumpBackTrace;
		//"--------------------------------------------------".postln;
	}
	
	// is guaranteed in Order sent via Host
	
	setPVH{|index,value,latency,send=true|
		if (send) {
			//"-setPVH-------------------------------------------------".postln;
			api.groupCmdOD('netSetP',index,value);
			//thisThread.dumpBackTrace;
			//"--------------------------------------------------".postln;
		}{
			this.netSetP(index,value); // no latency needed
		}
	}
	
	// variable parameter uses send until end value is sent using sendOD via host
	// i'm keeping latency in here even though its not used by the method
	setPVP{|index,value,latency,send=true|
		if (p[index]!=value) { 
			p[index]=value;
			if (send) {
				//"-setPVP-------------------------------------------------".postln;
				api.sendVP(id+"_vp_"++index,'netSetP',index,value);
				//thisThread.dumpBackTrace;
				//"--------------------------------------------------".postln;
			};
		}
	}
	
	// net reciever of all the 5 methods above
			
	netSetP{|index,value|
		if (p[index]!=value) {
			p[index]=value;
			models[index].lazyValue_(value,false);
		}
	}
	
	// new to set via models (this could be the future) ///////////////////////////////
	
	// for network to update via model !!! (why didn't i think of this before)
	
	setPVPModel{|index,value,latency,send=true|
		if (p[index]!=value) { 
			p[index]=value;
			if (send) {
				api.sendVP(id+"_vpm_"++index,'netSetPVPModel',index,value);
			};
		}
	}
	
	// like above but uses od and no PVP index
	setODModel{|index,value,latency,send=true|
		if (p[index]!=value) { 
			p[index]=value;
			if (send) { api.sendOD('netSetPVPModel',index, value) };
		}
	}
	
	// net reciever
	
	netSetPVPModel{|index,value|  models[index].lazyValueAction_(value,nil,false,false) }
	
	// set inst parameters and send to synth at the same time. /////////////////////////
	
	setSynthArg{|index,value,synthArg,argValue,latency,send|
		send = send ? true;
		p[index]=value;
		if (node.notNil) { server.sendBundle(latency +! syncDelay,
				[\n_set, node, synthArg, argValue]) };
		if ((network.isConnected)and:{send}) {
			api.send(\netSynthArg,index,value,synthArg,argValue);
		}
	}
	
	// GD version
	
	setSynthArgGD{|index,value,synthArg,argValue,latency,send|
		send = send ? true;
		p[index]=value;
		if (node.notNil) { server.sendBundle(latency +! syncDelay,
				[\n_set, node, synthArg, argValue]) };
		if ((network.isConnected)and:{send}) {
			api.sendGD(\netSynthArg,index,value,synthArg,argValue);
		};
	}
	
	// OD version
	
	setSynthArgOD{|index,value,synthArg,argValue,latency,send|
		send = send ? true;
		p[index]=value;
		if (node.notNil) {
			server.sendBundle(latency +! syncDelay,[\n_set, node, synthArg, argValue])
		};
		if ((network.isConnected)and:{send}) {
			api.sendOD(\netSynthArg,index,value,synthArg,argValue);
		};
	}
	
	// VH version
	
	setSynthArgVH{|index,value,synthArg,argValue,latency,send|
		send = send ? true;
		if (send) {
			api.groupCmdOD(\netSynthArg,index,value,synthArg,argValue); // via host
		}{
			// or if from internal seq don't send
			this.setSynthArgVP(index,value,synthArg,argValue,latency,false)
		}
	}
	
	// VP version
	
	setSynthArgVP{|index,value,synthArg,argValue,latency,send|
		send = send ? true;
		p[index]=value;
		if (node.notNil) {
			server.sendBundle(latency +! syncDelay,[\n_set, node, synthArg, argValue])
		};
		if ((network.isConnected)and:{send}) {
			api.sendVP((id+""++index).asSymbol,\netSynthArg,index,value,synthArg,argValue);
		};
	}
	
	// net reciever of all the 5 methods above
	
	netSynthArg{|index,value,synthArg,argValue|
		if (p[index]!=value) {
			p[index]=value;
			if (node.notNil) { server.sendBundle(nil,[\n_set, node, synthArg, argValue]) };
			models[index].lazyValue_(value,false);
		};
	}
	
	// set inst parameters, send to synth and set GUI at the same time. ////////////////////////
	// this is useful for more complex parameter vs synth vs GUI 
	
	setSynthArgGUI{|index,value,synthArg,argValue,guiIndex,guiValue,latency,send|
		send = send ? true;
		p[index]=value;
		if (node.notNil) {
			server.sendBundle(latency +! syncDelay,[\n_set, node, synthArg, argValue])
		};
		if (send) {
			api.send(\netSynthArgGUI,index,value,synthArg,argValue,guiIndex,guiValue);
		}
	}
	
	// GD version
	
	setSynthArgGUIGD{|index,value,synthArg,argValue,guiIndex,guiValue,latency,send|
		send = send ? true;
		p[index]=value;
		if (node.notNil) {
			server.sendBundle(latency +! syncDelay,[\n_set, node, synthArg, argValue])
		};
		if (send) {
			api.sendGD(\netSynthArgGUI,index,value,synthArg,argValue,guiIndex,guiValue);
		}
	}
	
	// OD version
	
	setSynthArgGUIOD{|index,value,synthArg,argValue,guiIndex,guiValue,latency,send|
		send = send ? true;
		p[index]=value;
		if (node.notNil) {
			server.sendBundle(latency +! syncDelay,[\n_set, node, synthArg, argValue])
		};
		if (send) {
			api.sendOD(\netSynthArgGUI,index,value,synthArg,argValue,guiIndex,guiValue);
		}
	}
	
	// VH version
	
	setSynthArgGUIVH{|index,value,synthArg,argValue,guiIndex,guiValue,latency,send|
		send = send ? true;
		if (send) {
			// via host
			api.groupCmdOD(\netSynthArgGUI,index,value,synthArg,argValue,guiIndex,guiValue);
		}{
			// or if from internal seq and don't send
			this.netSynthArgGUI(index,value,synthArg,argValue,guiIndex,guiValue)
		}
	}
	
	// VP version
	
	setSynthArgGUIVP{|index,value,synthArg,argValue,guiIndex,guiValue,latency,send|
		send = send ? true;
		p[index]=value;
		if (node.notNil) {
			server.sendBundle(latency +! syncDelay,[\n_set, node, synthArg, argValue])
		};
		if (send) {
			api.sendVP(id+"_ssvp_"++index,
				\netSynthArgGUI,index,value,synthArg,argValue,guiIndex,guiValue);
		};
	}
	
	// net reciever of all the 5 methods above
	
	netSynthArgGUI{|index,value,synthArg,argValue,guiIndex,guiValue|
		if (p[index]!=value) {
			p[index]=value;
			if (node.notNil) { server.sendBundle(nil,[\n_set, node, synthArg, argValue]) };
			models[guiIndex].lazyValue_(guiValue,false);
		}
	}
			
	// set inst parameters and replace synth with a new one. /////////////////////////////////
	// used for parameters you can't change once a synth is created //////////////////////////
	
	setPReplace{|index,value,latency,send|
		send = send ? true;
		p[index]=value;
		this.replaceDSP(latency);
		if (send) {
			api.send(\netPReplace,index,value);
		}
	}
	
	// GD version
	
	setPReplaceGD{|index,value,latency,send|
		send = send ? true;
		p[index]=value;
		this.replaceDSP(latency);
		if (send) {
			api.sendGD(\netPReplace,index,value);
		}
	}
	
	// OD version
	
	setPReplaceOD{|index,value,latency,send|
		send = send ? true;
		p[index]=value;
		this.replaceDSP(latency);
		if (send) {
			api.sendOD(\netPReplace,index,value);
		}
	}
	
	// VH version
	
	setPReplaceVH{|index,value,latency,send|
		send = send ? true;
		if (send) {
			api.groupCmdOD(\netPReplace,index,value)
		}{
			p[index]=value;
			this.replaceDSP(latency);
			models[index].lazyValue_(value,false);
		}
	}
	
	// VP version
	
	setPReplaceVP{|index,value,latency,send|
		send = send ? true;
		p[index]=value;
		this.replaceDSP(latency);
		if (send) {
			api.sendVP(id+"_prvp_"++index,\netPReplace,index,value);
		};
	}
	
	// net reciever of all the 5 methods above
	
	netPReplace{|index,value|
		if (p[index]!=value) {
			p[index]=value;
			this.replaceDSP;
			models[index].lazyValue_(value,false);
		}
	}
	
	createMIDIInModelWindow{|window,low,high,colors|
		var gui = ();
		colors = (
			background: 		Color(59/77,59/77,59/77),
			border2: 			Color(6/11,42/83,29/65),
			border1: 			Color(3/77,1/103,0,65/77),
			menuBackground:	Color(1,1,0.9)
		) ++ (colors?());
		
		gui[\window] = MVC_ModalWindow(window.view, (250-60)@(150-18), colors);
		gui[\scrollView] = gui[\window].scrollView;
		
		MVC_StaticText( gui[\scrollView], Rect(10,23-18,110,18))
			.shadow_(false)
			.color_(\string,Color.black)
			.font_(Font("Helvetica-Bold", 13))
			.string_("MIDI Input");		
		
		// midi in
		midi.createInMVUA (gui[\scrollView], (13)@(40+20-13-18), false,colors[\menuBackground]);
		midi.createInMVUB (gui[\scrollView], (211-50-60+3)@(71-18), false,colors[\menuBackground]);
			
		// 7.MIDI low 
		MVC_NumberBox(models[low], gui[\scrollView], Rect(44,93-18,24,18), gui[\theme2])
			.label_("Low")
			.labelShadow_(false)
			.color_(\label,Color.black)
			.orientation_(\horizontal)
			.rounded_(true);
		
		// 8.MIDI High
		MVC_NumberBox(models[high], gui[\scrollView], Rect(44,70-18,24,18), gui[\theme2])
			.label_("High")
			.labelShadow_(false)
			.color_(\label,Color.black)
			.orientation_(\horizontal)
			.rounded_(true);
		
		// Ok
		MVC_OnOffView(gui[\scrollView],Rect(105, 78, 50, 20),"Ok")
			.rounded_(true)  
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 gui[\window].close };
		
	}
	
	createMIDIInModelWindow2{|window,colors|
		var gui = ();
		colors = (
			background: 		Color(59/77,59/77,59/77),
			border2: 			Color(6/11,42/83,29/65),
			border1: 			Color(3/77,1/103,0,65/77),
			menuBackground:	Color(1,1,0.9)
		) ++ (colors?());
		
		gui[\window] = MVC_ModalWindow(window.view, (250-60)@(150-18-25), colors);
		gui[\scrollView] = gui[\window].scrollView;
		
		MVC_StaticText( gui[\scrollView], Rect(10,23-18,110,18))
			.shadow_(false)
			.color_(\string,Color.black)
			.font_(Font("Helvetica-Bold", 13))
			.string_("MIDI Input");		
		
		// midi in
		midi.createInMVUA (gui[\scrollView], (13)@(40+20-13-18), false,colors[\menuBackground]);
		midi.createInMVUB (gui[\scrollView], (13)@(71-18), false,colors[\menuBackground]);
			
		// Ok
		MVC_OnOffView(gui[\scrollView],Rect(105, 53, 50, 20),"Ok")
			.rounded_(true)  
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 gui[\window].close };
		
	}
	
	createMIDIInOutModelWindow{|window,low,high,colors, midi2|
		var gui = ();
		var midi2Offset = midi2.isNil.if(0,25);
		
		
		colors = (
			background: 		Color(59/77,59/77,59/77),
			border2: 			Color(6/11,42/83,29/65),
			border1: 			Color(3/77,1/103,0,65/77),
			menuBackground:	Color(1,1,0.9)
		) ++ (colors?());
		
		gui[\window] = MVC_ModalWindow(window.view, (315)@(120-18+midi2Offset), colors);
		gui[\scrollView] = gui[\window].scrollView;
		
		// midi out
		
		MVC_StaticText( gui[\scrollView], Rect(2,5,110,18))
			.shadow_(false)
			.color_(\string,Color.black)
			.font_(Font("Helvetica-Bold", 13))
			.string_("MIDI Output");	
				
		midi.createOutMVUA (gui[\scrollView], (85)@(5), false, background:colors[\menuBackground]);
		midi.createOutMVUB (gui[\scrollView], (235)@(5),false, background:colors[\menuBackground]);
		
		
		// midi in
		
		MVC_StaticText( gui[\scrollView], Rect(2,30,110,18))
			.shadow_(false)
			.color_(\string,Color.black)
			.font_(Font("Helvetica-Bold", 13))
			.string_("MIDI Input");	
			
		midi.createInMVUA (gui[\scrollView], (85)@(30), false,colors[\menuBackground]);
		midi.createInMVUB (gui[\scrollView], (235)@(30), false,colors[\menuBackground]);
			
		if (low.notNil) {
				// 7.MIDI low 
				MVC_NumberBox(models[low], gui[\scrollView], Rect(83, 55, 24, 18), gui[\theme2])
					.label_("Low")
					.labelShadow_(false)
					.color_(\label,Color.black)
					.orientation_(\horizontal)
					.rounded_(true);
		};
		
		if (high.notNil) {
			// 8.MIDI High
			MVC_NumberBox(models[high], gui[\scrollView], Rect(146, 55, 24, 18), gui[\theme2])
				.label_("High")
				.labelShadow_(false)
				.color_(\label,Color.black)
				.orientation_(\horizontal)
				.rounded_(true);
		};
		
		// Ok
		MVC_OnOffView(gui[\scrollView],Rect(235, 55+midi2Offset, 50, 20),"Ok")
			.rounded_(true)  
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 gui[\window].close };
			
			
		if (midi2.notNil) {
					
//			MVC_StaticText( gui[\scrollView], Rect(2,55,110,18))
//				.shadow_(false)
//				.color_(\string,Color.black)
//				.font_(Font("Helvetica-Bold", 13))
//				.string_("System Out");	
			
			MVC_StaticText( gui[\scrollView], Rect(2,55,110,18))
				.shadow_(false)
				.color_(\string,Color.black)
				.font_(Font("Helvetica-Bold", 13))
				.string_("System In");	
				
//			midi2.createOutMVUA (gui[\scrollView], (85)@(55),
//				false, background:colors[\menuBackground]);
//			midi2.createOutMVUB (gui[\scrollView], (235)@(55),
//				false, background: colors[\menuBackground]);
				
			midi2.createInMVUA (gui[\scrollView], (85)@(55), false,colors[\menuBackground]);
			midi2.createInMVUB (gui[\scrollView], (235)@(55), false,colors[\menuBackground]);
						
		};
			
	}	
	
	
	lfo_{} // quick bug fix for loading new songs with new bumnote
	filter_{} // quick bug fix for loading new songs with new bumnote
	
// my Hack tOOLs ! :D fub-2013 /////////////////////////////////////////////////////////////////
// now used as part of mixer.
// only keep what will be used outside of myHack?
	
	on_{|flag| this.onOff_(flag.binaryValue) }
	
	on{ ^this.onOffModel.value }
	
	preset_{|preset| this.selectProgram(preset) }	
	
	incPreset{} // this prob won't be used outside of myHack
	decPreset{} // this prob won't be used outside of myHack
		
	volume{ if (this.volumeModel.notNil) { ^this.volumeModel.get} {^nil} }
	
	volume_{|val|
		if (this.volumeModel.notNil) {
			this.volumeModel.set_(val) // uses set. could use to avoid lastLevel not from fade
		}
	}
	
	// old style
	setPeakLevel{|level|
		hack[\peakLevel]=level;
		this.peakLevel_(level); // to remove ??
	}
	
	setAsPeakLevel{|send=true|
		hack[\peakLevel]=this.volume;
		this.peakLevel_(this.volume,send); // to remove ??
	}
	
	// set the model
	peakLevel_{|val,send=true|
		if (this.peakModel.notNil) {
			this.peakModel.lazyValueAction_(val,0,send,false)
		}
	}
	
	// get the model value
	peakLevel{ ^this.peakModel.value}
	
	fadeTo{|level,beats=64,resolution=1|
		var startVolume,x;
		if (this.volumeModel.notNil) {
			startVolume = this.volume;
	
			hack[\fadeTask].stop;
			hack[\fadeTask]={
				(beats*resolution).do{|n|			
					(absTime/8/resolution).wait;
					
					x=(n+1)/beats/resolution;
					this.volume_(
						(level*x)+(startVolume*(1-x))
						
					)
				}
			}.fork(SystemClock);	
		}
	}
	
	fadeIn{|level,beats=64,resolution=1|
		var startVolume,x;
		
		beats = #[ 192, 64, 24 ][studio.models[\fadeSpeed].value];
		
		if (this.volumeModel.notNil) {	
			level=level ? (this.peakLevel) ? 1; // levels ?
			startVolume = this.volume;
			
			// this.volume_(0); // do i want this?
			
			hack[\fadeTask].stop;
			hack[\fadeTask]={
				(beats*resolution).do{|n|			
					(absTime/8/resolution).wait;
					x=(n+1)/beats/resolution;
					this.volume_(
						//level*x
						(level*x)+(startVolume*(1-x))
					);
				}
			}.fork(SystemClock);	
		}
	}
	
	fadeOut{|beats=64,resolution=1|
		var startVolume;
		
		beats = #[ 192, 64, 24 ][studio.models[\fadeSpeed].value];
		
		if (this.volumeModel.notNil) {
			startVolume = this.volume; // levels?
			hack[\fadeTask].stop;	
			hack[\fadeTask]={
				(beats*resolution).do{|n|			
					(absTime/8/resolution).wait;
					this.volume_(startVolume*(beats*resolution-n-1)/(beats*resolution))
				}
			}.fork(SystemClock);	
		}
	}
		
} // end //////////////////////////////////////////////
	