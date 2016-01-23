
// the models and the views.

+ LNX_Studio {

	/////// server controls & studio transport models //////////////////////////////////////////


	makeBatch{
	
		if (batchOn && (lastBatchFolder.notNil)) {
			this.guiLoadInstFromLibrary("Library/SC Code/BATCH",LNX_Code,"BATCH");
			{
				0.5.wait;
				lastBatchFolder.folderContents.do{|fileName|
					insts.visualOrder.last.userBank.guiAddURL(
						("file:/"++(fileName.dropFolder(5)))
					);	
				};
			}.fork(AppClock);
			batchOn=false;
			batchFolder_(batchFolder+1);
			batch_(1);
		};
	}

	initModels {
		var path;
								
		// block size
		models[\blockSize] = [(("blockSize".loadPref?[1])[0]).asInt,[0,4,\lin,1],
			(\label_:"Block Size", \items_: (2**(5..9)).collect(_.asString) ),
			{|me,val,latency,send|
				this.initServerPostModels;   // update server options
				Server.killAll;              // and restart
				[val].savePref("blockSize"); // save to preferences folder
			}].asModel;
		
		// master out level meter
		models[\peakOutL] = [\unipolar].asModel.fps_(20); // fps same as in LNX_Studio:initUGens
		models[\peakOutR] = [\unipolar].asModel.fps_(20); // fps same as in LNX_Studio:initUGens

		// network master voulme and mute controls
		models[\networkMaterVolume] = [
			("networkMasterVolume".loadPref?[true])[0].isTrue.if(1,0), \switch,
			{|me,val| [val.isTrue].savePref("networkMasterVolume") }].asModel;


		// network Controller Keyboard
		models[\networkCntKeyboard] = [
			("networkCntKeyboard".loadPref?[true])[0].isTrue.if(1,0), \switch,
			{|me,val| [val.isTrue].savePref("networkCntKeyboard") }].asModel;


		// peak level
		models[\peakLevel]=[1, \unipolar,  midiControl, 12, "Peak Level",
				{|me,val,latency,send=true| this.setPVP(\peakLevel,val,nil,send) }].asModel;

		// fade speed
		models[\fadeSpeed]=[0, [0,2,\lin,1], midiControl, 13, "Fade Speed"].asModel
			.automationActive_(false);

		// time
		models[\time]=[0, [0,inf],{|me,val|
			val=val.asInt;
//			if ([ 150, 300, 600, 900, 1200 ].includes(val)) {
//				this.flashWindow( (150:Color.orange, 600:Color.orange)[val] )
//			};
			mixerGUI[\time].string_(
				val.div(60).asString ++ ":"++( ((val%60)<10).if("0","")) ++(val%60));
		}].asModel;

		// play
		models[\play]=[\switch, midiControl, 3, "Play", {|me,val| this.guiPlay(val)}].asModel
			.automationActive_(false);

		// stop
		models[\stop]=[\switch, midiControl, 4, "Stop", {|me,val| this.guiStop}].asModel
			.automationActive_(false);

		// record
		models[\record]=[\switch, midiControl, 2, "Record", (strings_:["Rec","Stop"]),
			{|me,val|
				
				
		if (server.serverRunning) {
			if (me.value == 1) {
				if (batchOn.not) {
					path = server.prepareForRecord(	
						"~/Desktop".standardizePath +/+
						(this.name) +
						(Date.getDate.format("%Y-%d-%e %R:%S").replace(":",".").drop(2)) ++
						"." ++ (server.recHeaderFormat)
					);
					{server.record}.defer(0.1);
				}{
					var dir = LNX_BufferProxy.userPath +/+ "BATCH" +/+
								(this.name) ++"_bin" + batchFolder;
					
					lastBatchFolder = dir;
					
					if (dir.pathExists(false).not) { dir.makeDir };
					path = server.prepareForRecord(	
						dir +/+
						(this.name) + (("00"++batch).keep(-3)) ++
						"." ++ (server.recHeaderFormat)
					);
					batch = batch +1;
					{server.record}.defer(0.15);
				};
				
				
			}{
				server.stopRecording;
			};
		}{
			"The server must be booted to record it".postln;
			me.value_(0);
		};
			
			
			}].asModel
			.automationActive_(false);

		// extClock
		models[\extClock]=[extClock.binaryValue, \switch, midiControl, 5, "Int / Ext",
			(strings_:["Int","Ext"]),
			{|me,val|
				{
					if (network.isConnected.not) {
						extClock=(val==1);
						this.clockSwap;
					}{
						me.value_(0);
					};
				}.defer
			}].asModel
			.automationActive_(false);

		// tempo
		models[\tempo]=[bpm,[1,999], midiControl, 7, "Tempo", (moveRound_:1,resoultion_:10),
			{|me,val| this.guiSetBPM(val) }].asModel;

		// tap
		models[\tap]=[0, [0,0], midiControl, 6, "Tap", (strings_:"Tap"), {this.tap}].asModel
			.automationActive_(false);

		// mute server
		models[\mute]=[\switch, midiControl, 0, "Mute", (strings_:"M"),
			{|me,val,latency,send=true,toggle|
				(server.serverRunning).if{
					[{server.unmute}, {server.mute}][val].value;
				}{
					"The server must be booted to mute it".warn;
					me.value_(0);
				};
				this.setPVP(\mute,val,nil,send);
			}].asModel;

		// set range of volume
		server.volume.setVolumeRange(-inf, 0);

		// volume
		models[\volume]=[0,[server.volume.min,server.volume.max,\db,0,-inf, " dB"],
			midiControl, 1, "Volume",
			(mouseDownAction_:{tasks[\fadeTask].stop}),
			{|me,val,latency,send=true,toggle|
				server.volume_(val);
				this.setPVP(\volume,val,nil,send);
			}].asModel;

		// connect my mvc mute & volume models to SC's Volume model
		SimpleController(server.volume)
			.put(\amp, {|changer, what, vol| { models[\volume].value_(vol) }.defer })
			.put(\mute,{|changer, what,flag| { models[\mute].value_(flag.binaryValue)}.defer});

		// server Running model
		models[\serverRunning] = [\switch, midiControl, 8, "Server Reset",
			( strings_:["Off","On"], mouseMode_:\tapUp ),
			{|...clickCount|
				if ((clickCount.last?0)>=2) {
					if (clickCount.last==2) {
						Server.quitAll;
					}{
						Server.killAll
					};
				}{
					this.restartDSP
				};
			}].asModel.midiMode_(\tap).automationActive_(false);

		// open the network window
		models[\network] = [\switch, midiControl, 9, "Network Room",
			( strings_:"Net", mouseMode_:\tapUp ),
			{ {network.guiConnect}.defer }].asModel.midiMode_(\tap).automationActive_(false);

		// connect server stats & serverRunning to models
		SimpleController(server)
			.put(\counts,{
				mixerGUI[\cpu].string_(server.peakCPU.asInt.clip(0,100).asString++"%");
				//mixerGUI[\synths].string_( (server.numSynths-1).clip(0,inf) );

			})
			.put(\serverRunning,{
				if ((models[\serverRunning].value.isTrue) and: {server.serverRunning.not}) {
					{this.restartDSP}.defer(0.5);
				};
				models[\serverRunning].value_(server.serverRunning.binaryValue);
				if (server.serverRunning.not) {
					mixerGUI[\cpu].string_("-");
					//mixerGUI[\synths].string_("-");
				};
			});

		// start polling the server
		server.startAliveThread;

		// show 1 inst or show all
		models[\show1]=[\switch, midiControl, 11, "All or 1",
			{|me,val|
				transmitInstChange=false; // stop this from going over the net
				showNone=false;
				models[\showNone].value_(0);
				show1=val.booleanValue;
				if ((insts.size>0) and: {insts.selectedInst.notNil}) {
					if (show1) {
						insts.pairsDo{|id,inst|
							if (id!=insts.selectedID) { inst.hide }
						};
						insts.selectedInst.front;
					}{
						insts.visualOrder.do({|inst| inst.front });
						insts.selectedInst.front;
					};
				};
				this.transmitInstChange;
			}].asModel.automationActive_(false);

		// hide all instruments
		models[\showNone]=[\switch, midiControl, 10, "Hide All",
			{|me,val|
				showNone=me.value.booleanValue;
				transmitInstChange=false; // stop this from going over the net
				if (insts.size>0) {
					if (showNone) {
						insts.do(_.closeWindow)
					}{
						if (insts.selectedInst.notNil) {
							if (show1) {
								insts.pairsDo{|id,inst|
									if (id!=insts.selectedID) {inst.closeWindow}
								};
								insts.selectedInst.front;
							}{
								insts.visualOrder.do{|inst| inst.front };
								insts.selectedInst.front;
							};
						}{
							// covers a bug in networking
							// check to see if display ALL or 1 selected
							// when net user has add insts but i haven't selected 1
							insts.visualOrder.do{|inst| inst.front };
							{this.selectInst(nil)}.defer(0.2);
						};
					};
				};
				this.transmitInstChange;
			},
			\action2_ -> {|me,val|
				var anyVisable=false;
				transmitInstChange=false; // stop this from going over the net
				insts.do{|i| if (i.isVisible) {anyVisable=true} };
				if (anyVisable) {
					insts.do(_.closeWindow)
				}{
					insts.do(_.openWindow)
				};
				me.value_(0);
				showNone=false;
				this.transmitInstChange;
			}].asModel.automationActive_(false);

			// this is used in LNX_Network:createNetworkGUI

			// is user listening to the group song
			models[\isListening]=[1,\switch, midiControl, -1, "Listen to song",
				{|me,val| network.isListening_(val.isTrue)}].asModel.automationActive_(false);
				
			// automation playing back
			models[\autoOn]=[1,\switch, midiControl, 14, "Automation",
				{|me,val,latency,send=true,toggle|
					if (network.isConnected) {
						this.groupCmdSync(\netSetAuto,val)
					}{
						this.netSetAuto(val);
					};
				}].asModel.automationActive_(false);
			
			// automation recording
			models[\autoRecord]=[0,\switch, midiControl, 15, "Auto Record",
				{|me,val|
					MVC_Model.isRecording_(val.isTrue);
				}].asModel.automationActive_(false);
			
			// ***** midiControls >=16 used in PresetsOfPresets *******
			LNX_POP.initModels(models, midiControl);

			// always on top (not best location but will do for now
			alwaysOnTop = ("alwaysOnTop".loadPref ?? [false])[0].isTrue;

	}
	
	// group sync a command
	groupCmdSync{|method...args| api.groupCmdSync(method, *args) }
	
	// set auto On/off
	netSetAuto{|value|
		value = value.asInt;
		MVC_Automation.isPlaying_(value.isTrue);
		if (value.isFalse) { models[\autoRecord].lazyValueAction_(0)};
		models[\autoOn].lazyValue_(value,false);
	}
	
	// set model from user side
	setPVP{|model,val,latency,send=true|
		if (send) {
			if ((#[\mute, \volume].includes(model))and:
				{ models[\networkMaterVolume].value.isFalse }) {^this}; // drop
			if (#[\peakLevel, \mute, \volume,\quant,\autoOn].includes(model)) {  // only these
					api.sendVP("std_vp_"++model,'netSetModel',model,val);// network
			}
		}
	}

	// set model from the network
	netSetModel{|model,val|
		model=model.asSymbol;
		if ((#[\mute, \volume].includes(model))and:
			{ models[\networkMaterVolume].value.isFalse }) {^this}; // drop
		if (#[\peakLevel, \mute, \volume,\quant,\autoOn].includes(model)) {         // only these
			if (models[\networkMaterVolume].value.isTrue) {              // and allowed in prefs
				models[model.asSymbol].lazyValueAction_(val,nil,false); // lazy value
			};
		}
	}

	// save the top left corner of the mixer window to preferences
	saveWindowBounds{ [mixerWindow.bounds.left,mixerWindow.bounds.top].savePref("winPos") }

	// update & save the top left corner of the mixer window. used when adding, loading & saving
	updateOSX{
		this.saveWindowBounds;
		osx=mixerWindow.bounds.left
	}

	// get the osx of the studio window
	savedWindowBounds{
		var pos;
		pos="winPos".loadPref;
		if (pos.isNil) {
			^nil
		}{
			pos=(pos.asInteger.clip(0,inf)++[thisWidth,thisHeight]).asRect;
			osx=pos.left;
			^pos
		}
	}

	// save dialog for safe mode
	safeModeSaveDialog{
		var path, window, scrollView, filename;


		if (songPath.isNil) {
			filename = "";
		}{
			filename   = title.copy;
		};

		window = MVC_ModalWindow(mixerWindow.view, 195@90);
		scrollView = window.scrollView;

		// text field for the instrument / filename
		MVC_Text(scrollView,Rect(10,20,142,16))
			.string_(filename)
			.label_("Save song as...")
			.labelShadow_(false)
			.canEdit_(true)
			.shadow_(false)
			.color_(\string,Color(59/77,59/77,59/77)*1.4)
			.color_(\edit,Color.orange)
			.color_(\background,Color.black)
			.color_(\cursor,Color.white)
			.color_(\focus,Color.orange)
			.color_(\editBackground, Color.black)
			.font_(Font.new("STXihei", 13))
			.actions_(\stringAction,{|me|
				filename=me.string.filenameSafe;
				me.string_(filename);

			})
			.actions_(\enterAction,{|me|
				filename=me.string.filenameSafe;
				me.string_(filename);
				if (filename.size>0) {
					window.close;
					this.save("/Users/Shared" +/+ filename);
					("/Users/Shared" +/+ filename).revealInFinder;
				};

			}).focus;

		// Cancel
		MVC_OnOffView(scrollView,Rect(130-11-60, 55-11, 55, 20),"Cancel")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 window.close };

		// Ok
		MVC_OnOffView(scrollView,Rect(130-11, 55-11, 50, 20),"Ok")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{
				if (filename.size>0) {
					window.close;
					this.save("/Users/Shared" +/+ filename);
					("/Users/Shared" +/+ filename).revealInFinder;
				};
			};

	}

	// used when loading a song to clear the current show window options to default = ALL
	clearShowWindowsOptions{
		showNone=false;
		models[\showNone].value_(0);
		show1=false;
		models[\show1].value_(0);
	}

	// create the network widgets on the studio window (hidden at start-up) /////////////////////

	createNetworkWidgets{

		var h,w;
		h=130;
		w=thisWidth-12;

		// scroll view for network dialog
		gui[\netScrollView] = MVC_RoundedScrollView(mixerWindow, Rect(11,319+30,w-10,h-25))
			.resizeList_([1,1,1,1,1]) //  0:view 1:left 2:top 3:right 4:bottom
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border,Color(6/11,42/83,29/65))
			.hasBorder_(false)
			.autoScrolls_(true)
			.hasVerticalScroller_(true);

		// user dialog output display
		gui[\userDialog]=MVC_DialogView(gui[\netScrollView],Rect(1,1,w-12,h-44))
			.color_(\background,Color(0.14,0.12,0.11)*0.4)
			.color_(\string,Color.white);


		this.addTextToDialog("",false,true);
		this.addTextToDialog("",false,true);
		this.addTextToDialog("                  LNX_Studio",false,true);
		this.addTextToDialog("",false,true);
		this.addTextToDialog("                 "+(version.drop(1))+"alpha",false,true);
		this.addTextToDialog("",false,true);

		// user dialog input
		MVC_StaticText(gui[\netScrollView],Rect(1,1+h-36-6,w-30,14))
			.string_("")
			.shadow_(false)
			.canEdit_(true)
			.enterStopsEditing_(false)
			.enterKeyAction_{|me,string|
				this.talk(string);
				me.string_("");
			}
			.color_(\string,Color(59/77,59/77,59/77)*1.4)
			.color_(\edit,Color(59/77,59/77,59/77)*1.4)
			.color_(\background,Color(0.14,0.12,0.11)*0.4)
			.color_(\focus,Color.orange)
			.color_(\editBackground, Color(0,0,0,0.7))
			.color_(\cursor,Color.white)
			.font_(Font.new("STXihei", 12));

		// isListening (uses model from studio)
		MVC_OnOffView(models[\isListening],gui[\netScrollView],Rect(w-27,1+h-37-6,16,16))
			.mode_(\icon)
			.strings_([\speaker])
			.color_(\off,Color(0.15, 0.15, 0.4, 1))
			.color_(\on,Color.green)
			.color_(\string,Color.black);

		network.collaboration.createStudioGUI(mixerWindow); // should this move here?

	}

	// add text to the dialog in the studio window

	addTextToDialog{|text,flash=true,force=false|
		if ((network.isConnected)||force) {
			gui[\userDialog].addText(text,flash)
		};
	}

	// show the network section of the studio window

	autoSizeGUI{
		var h;
		h=network.collaboration.autoSizeGUI(menuGap.y);
		mixerGUI[\libraryScrollView].bounds_(Rect(11, 36, 190, 269-h+8+30+menuGap.y));
		gui[\netScrollView].bounds_(Rect(11,319+30-h+8+menuGap.y,190,105));
	}


	showNetworkGUI{ this.autoSizeGUI }

	// hide the network section of the studio window

	hideNetworkGUI{ {this.autoSizeGUI}.defer }

	// instruments studio window gui ////////////////////////////////////////////////////////

	updateAlwaysOn{|id,bool|
		if (bool) {
			mixerGUI[id][\alwaysOn].visible_(true);
			mixerGUI[id][\onOff].visible_(false);
			mixerGUI[id][\solo].visible_(false);
		}{
			mixerGUI[id][\alwaysOn].visible_(false);
			mixerGUI[id][\onOff].visible_(true);
			mixerGUI[id][\solo].visible_(true);
		};
	}

	// select the next instrument
	selectNextInst{
		var scrollView=gui[\scrollView];
		insts.selectedInst_(insts.nextWrapID);
		if (show1) {insts.do(_.closeWindow)};
		this.selectInst(insts.selectedID);
		if ((insts.getY(insts.selectedID)*19)<(scrollView.visibleOrigin.y)) {
			scrollView.visibleOrigin_(0@(insts.getY(insts.selectedID)*19))
		};
		if ((insts.getY(insts.selectedID)*19)>(scrollView.visibleOrigin.y+180)) {
			scrollView.visibleOrigin_(0@(insts.getY(insts.selectedID)*19))
		};
	}

	// select the previous instrument
	selectPreviousInst{
		var scrollView=gui[\scrollView];
		insts.selectedInst_(insts.previousWrapID);
		if (show1) {insts.do(_.closeWindow)};
		this.selectInst(insts.selectedID);
		if ((insts.getY(insts.selectedID)*19)<(scrollView.visibleOrigin.y)) {
			scrollView.visibleOrigin_(0@(insts.getY(insts.selectedID)*19))
		};
		if ((insts.getY(insts.selectedID)*19)>(scrollView.visibleOrigin.y+180)) {
			scrollView.visibleOrigin_(0@(insts.getY(insts.selectedID)*19))
		};
	}

	// move inst to pos (return true or false if moved)
	move{|id,pos|
		var moved=insts.move(id,pos);
		if (moved){
			this.alignInstGUI;	// move gui's
			insts.visualOrder.do({|inst,index| inst.instNo_(index) }); // change numbers
			api.sendOD(\netMove,id,pos);
			MVC_Automation.refreshGUI;
		};
	}

	// move inst to pos (return true or false if moved)
	netMove{|id,pos|
		var moved=insts.move(id,pos);
		if (moved){
			{
				this.alignInstGUI;	// move gui's
				insts.visualOrder.do({|inst,index| inst.instNo_(index) }); // change numbers
				MVC_Automation.refreshGUI;
			}.defer;
		};
	}

	// align the instrument gui including any gaps from delete
	alignInstGUI{ this.alignMixerGUI }

	///////// preferences //////////////////////////////////////////////////////////////////

	// this is all old school gui stuff...

	preferences {

		var window=this.mixerWindow.view;

		var scrollView;
		
		var gui=IdentityDictionary[];

		gui[\textTheme] = (
			\canEdit_ : false,
			\shadow_  : false,
			\align_   : 'left',
			\font_    : Font("Helvetica", 12),
			\colors_  : (\string: Color.black),
		);
		
		gui[\sliderTheme] = (
			\orientation_ : \horizontal,
			\colors_      : (
				\background : Color.grey/2,
				\knob       : Color.white,
				\numberDown : Color.black,
				\numberUp   : Color.black
			)
		);
		
		gui[\buttonTheme] = (
			orientation_:\horizontal,
			rounded_:	true,
			colors_: (up:Color(0.9,0.9,0.9), down:Color(0.9,0.9,0.9)/2)
		);

		gui[\labelTheme]=( \font_		:  Font("Helvetica-Bold", 14),
						\align_		: \left,
						\shadow_		: false,
						\noShadows_	: 0,
						\colors_		: (\string : Color.black));

		if ( (midiWin.isNil) or: {midiWin.window.isClosed } ) {

			midiWin = MVC_ModalWindow(
				(mixerWindow.isVisible).if(mixerWindow.view,window.view),
				(420)@(449));
			scrollView = midiWin.scrollView.view;



			MVC_StaticText(scrollView, Rect(10, 10, 170, 18),gui[\labelTheme])
			.string_("LNX_Studio Preferences");

			// MIDI Controller Keyboard In
			midi.createInGUIA (scrollView, (170-25)@(272-2), false, false);
			midi.createInGUIB (scrollView, (330-25)@(272-2), false, false);
			midi.action_{|me| this.saveControllerKeyboardPrefs};
			midi.portInGUI
				.label_("MIDI Controller Keyboard In")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black);
				

			// midi preset controller in
			LNX_POP.midi.createInGUIA (scrollView, (170-25)@(222-2), false, false);
			LNX_POP.midi.createInGUIB (scrollView, (330-25)@(222-2), false, false);
			LNX_POP.midi.action_{|me| LNX_POP.saveMIDIPrefs };
			LNX_POP.midi.portInGUI
				.label_("Preset MIDI controller In")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black);
			
			// midi preset controller OUT
			LNX_POP.midi.createOutGUIA (scrollView, (170)@(242), false);
			LNX_POP.midi.createOutGUIB (scrollView, (330)@(242), false);
			LNX_POP.midi.portOutGUI	
				.label_("Out")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black);

			// midi clock
			midiClock.createInGUIA (scrollView, (170-25)@(168-2), false);
			midiClock.createOutGUIA (scrollView, (170)@(188), false);
			midiClock.action_{|me| this.saveMIDIprefs };
			midiClock.portInGUI
				.label_("MIDI Clock In")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black);
			midiClock.portOutGUI
				.label_("Out")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black);

			// audio devices
			LNX_AudioDevices.audioHardwareGUI(scrollView,(170-10)@(36-25))
				.action_{|devices|
					//LNX_AudioDevices.changeAudioDevices(server,devices,{this.postBootFuncs});
					LNX_AudioDevices.changeAudioDevices(server,devices)
				};

			// latency
			MVC_SmoothSlider(scrollView, Rect(170,117,150, 16), gui[\sliderTheme])
				.label_("Latency (secs)")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black)
				.color_(\numberUp,Color.black)
				.numberFunc_(\float3)
				.controlSpec_([0.05,1,\linear,0.001])
				.value_(latency)
				.action_{|me| this.latency_(me.value) };
				
			// blocksize
			MVC_PopUpMenu3(models[\blockSize],scrollView,Rect(170,85,75,17),
				( \font_		 : Font("Arial", 10),
				  \labelShadow_: false,
				  \orientation_: \horiz,
				  \colors_     : (\background : Color.ndcMenuBG, \label : Color.black ))
			);
			


			// network networkCntKeyboard
			MVC_OnOffView(models[\networkCntKeyboard],scrollView,Rect(170, 299, 70, 19),
				"Network", ( \font_		: Font("Helvetica", 11),
								 \colors_     : (\on : Color.orange+0.25,
						 					   \off : Color.grey/2)))
				.label_("Controller Keyboard")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black);
				
			// network master volume changes
			MVC_OnOffView(models[\networkMaterVolume],scrollView,Rect(170, 328, 70, 19),
				"Network", ( \font_		: Font("Helvetica", 11),
								 \colors_     : (\on : Color.orange+0.25,
						 					   \off : Color.grey/2)))
				.label_("Master Volume & Mute")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black);
						 					   
			// moog sub 37 is visible
			MVC_OnOffView(scrollView,Rect(311, 299, 72, 19), "Sub 37",
								( \font_		: Font("Helvetica", 11),
								 \colors_     : (\on : Color.orange+0.25,
						 					   \off : Color.grey/2)))
				.value_(LNX_MoogSub37.isVisiblePref.asInt)
				.label_("Moog")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black)
				.action_{|me|
					LNX_MoogSub37.isVisiblePref_(me.value.isTrue).saveIsVisiblePref;
					this.recreateLibraryGUI;
				};
				
			// korg volva is visible
			MVC_OnOffView(scrollView,Rect(311, 328, 72, 19), "Volca",
								( \font_		: Font("Helvetica", 11),
								 \colors_     : (\on : Color.orange+0.25,
						 					   \off : Color.grey/2)))
				.value_(LNX_VolcaBeats.isVisiblePref.asInt)
				.label_("Korg")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black)
				.action_{|me|
					LNX_VolcaBeats.isVisiblePref_(me.value.isTrue).saveIsVisiblePref;
					this.recreateLibraryGUI;
				};
						 					   
				
			// roland is visible
			MVC_OnOffView(scrollView,Rect(311, 357, 72, 19), "JP-08",
								( \font_		: Font("Helvetica", 11),
								 \colors_     : (\on : Color.orange+0.25,
						 					   \off : Color.grey/2)))
				.value_(LNX_RolandJP08.isVisiblePref.asInt)
				.label_("Roland")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black)
				.action_{|me|
					LNX_RolandJP08.isVisiblePref_(me.value.isTrue).saveIsVisiblePref;
					this.recreateLibraryGUI;
				};
				
			// midi sync latency
			MVC_SmoothSlider(scrollView, Rect(170, 139,150, 16),gui[\sliderTheme])
				.numberFunc_(\float3Sign)
				.controlSpec_([-0.1,0.1,\linear,0.001,0])
				.value_(midiSyncLatency)
				.label_("MIDI Sync Latency Adj (secs)")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black)
				.action_{|me|
					midiSyncLatency=me.value;
					LNX_MIDIPatch.midiSyncLatency_(midiSyncLatency);
					[midiSyncLatency].savePref("MIDI Sync Latency");
				};
				
				
			// doubleClickLearn
			MVC_OnOffView(scrollView,Rect(170, 357, 70, 19), "On",
								( \font_		: Font("Helvetica", 11),
								 \colors_     : (\on : Color.orange+0.25,
						 					   \off : Color.grey/2)))
				.value_(MVC_View.doubleClickLearn.asInt)
				.label_("Double Click to MIDI Learn")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black)
				.action_{|me|
					MVC_View.doubleClickLearn_(me.value.isTrue);
				};
				
				
			// internal midi buses
			noInternalBusesGUI=MVC_PopUpMenu3(scrollView,Rect(170, 397, 70, 17))
				.items_(["None","1 Bus","2 Buses","3 Buses"
						 ,"4 Buses","5 Buses","6 Buses","7 Buses","8 Buses"
						 ,"9 Buses","10 Buses","11 Buses","12 Buses","13 Buses"
						 ,"14 Buses","15 Buses","16 Buses"])
				.color_(\background,Color.ndcMenuBG)
				.label_("No. of Internal MIDI Buses")
				.orientation_(\horiz)
				.labelShadow_(false)
				.color_(\label,Color.black)
				.action_{|me|
					this.guiNoInternalBuses_(me.value);
					this.saveMIDIprefs;
				}
				.value_(noInternalBuses)
				.font_(Font("Arial", 10));
				
			// scan for new midi equipment
			MVC_FlatButton(scrollView,Rect(252 ,396, 70, 20),"Scan MIDI",gui[\buttonTheme])
				.canFocus_(false)
				.action_{ LNX_MIDIPatch.refreshPorts };
				
				
			// Ok
			MVC_FlatButton(scrollView,Rect(332, 396, 50, 20),"Ok",gui[\buttonTheme])
				.canFocus_(true)
				.color_(\up,Color.white)
				.action_{	 midiWin.close };
						 					   
		}{
			midiWin.front;
		}
	}

}

// END LNX_Studio MVC_GUI
