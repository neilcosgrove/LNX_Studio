
// audio in & it can do +ive & -ive time syncing to studio clock!!!

LNX_ExternalInstrument : LNX_InstrumentTemplate {

	var <sequencer, <keyboardView, lastKeyboardNote, <midiInBuffer, <midiOutBuffer, <seqOutBuffer;

	*initClass{ Class.initClassTree(LNX_VolcaBeats) }
	*isVisible{ ^true }

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName      {^"External Instrument"}
	mixerColor       {^Color(0.803, 0.731, 0.668)} // colour in mixer
	*sortOrder       {^2.89}
	isInstrument     {^true}
	canBeSequenced   {^true}
	isMixerInstrument{^true}
	hasLevelsOut     {^true}
	hasMIDIClock     {^true}
	
	// mixer models
	peakModel   {^models[6]}
	volumeModel {^models[2]}
	outChModel  {^models[4]}
	soloModel   {^models[0]}
	onOffModel  {^models[1]}
	panModel    {^models[5]}
	sendChModel {^models[7]}
	sendAmpModel{^models[8]}

	header { 
		// define your document header details
		instrumentHeaderType="SC ExtInst Doc";
		version="v1.0";		
	}
	
	// an immutable list of methods available to the network
	interface{^#[\netMidiControlVP, \netExtCntIn, \extProgIn]}

	// MIDI patching ///////////////////////////////////////////////////////

	iInitVars{
		// the main sequencer
		sequencer = LNX_PianoRollSequencer(id++\pR)
			.pipeOutAction_{|pipe|
				if (((p[11]>0)&&(this.isOff)).not) {seqOutBuffer.pipeIn(pipe)};
			}
			.releaseAllAction_{ seqOutBuffer.releaseAll }
			.keyDownAction_{|me, char, modifiers, unicode, keycode|
				keyboardView.view.keyDownAction.value(me,char, modifiers, unicode, keycode)
			}
			.keyUpAction_{|me, char, modifiers, unicode, keycode|
				keyboardView.view.keyUpAction.value(me, char, modifiers, unicode, keycode)
			}
			.recordFocusAction_{ keyboardView.focus };
	}
	
	// any post midiInit stuff
	iInitMIDI{
		this.useMIDIPipes;
		midiOutBuffer = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.toMIDIPipeOut(pipe) };
		midiInBuffer  = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromInBuffer(pipe)  };
		seqOutBuffer  = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromSequencerBuffer(pipe) };
		
		
	}

	// midi pipe in. This is the future
	pipeIn{|pipe|
		if (instOnSolo.isOff and: {p[11]>0} ) {^this}; // drop if sequencer off
		if (pipe.historyIncludes(this)) {^this};       // drop to prevent internal feedback loops
		switch (pipe.kind)
			{\control} { // control	
				var index = pipe.num.asInt;			
				models[index+14].lazyValue_(pipe.val, true); // set model, no action
				p[index+14]=pipe.val;                        // set p[]
				api.sendOD(\netExtCntIn, index+14, pipe.val);// network it
			}
			{\program} { // program
				this.program(pipe.program,pipe.latency);
				^this // drop	
			};
		if (pipe.isNote) {	 midiInBuffer.pipeIn(pipe) }; // to in Buffer
	}
	
	// external midi setting controls (these are network methods)
	netExtCntIn{|index,val|
		p[index.asInt]=val.asFloat;
		models[index.asInt].lazyValue_(val.asFloat,false); // false is no auto
	}
	
	// midi coming from in buffer
	fromInBuffer{|pipe|
		sequencer.pipeIn(pipe);                 // to the sequencer
		keyboardView.pipeIn(pipe,Color.orange); // to the gui keyboard
		// drop out and Don't send if pipe is external and coming from Sub37 going to Sub37
		if ((pipe.source==\external) && {midi.outPoint.isSameDeviceAndName(pipe[\endPoint])}) {
			^this
		};
		this.toMIDIOutBuffer(pipe);             // to midi out buffer
	}
	
	// output from the sequencer buffer
	fromSequencerBuffer{|pipe|
		keyboardView.pipeIn(pipe,Color.orange); // to the gui keyboard
		this.toMIDIOutBuffer(pipe);             // and to the out buffer
	}
	
	// to the output buffer
	toMIDIOutBuffer{|pipe|
		if (instOnSolo.isOff and: {p[11]>0} ) {^this}; // drop if sequencer off
		midiOutBuffer.pipeIn(pipe);             // to midi out buffer
	}
	
	// and finally to the midi out
	toMIDIPipeOut{|pipe|
		midi.pipeIn(pipe.addToHistory(this));          // add this to its history
	}

	// release all played notes, uses midi Buffer
	stopAllNotes{ 
		midiInBuffer.releaseAll;
		seqOutBuffer.releaseAll;
		midiOutBuffer.releaseAll;
		{keyboardView.clear}.defer(studio.actualLatency);
	}
	
	///////////////////////////////////////////////////////

	// the models
	initModel {
		
		var template=[

			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle|
					this.solo(val,latency,send,toggle);
					if (node.notNil) {
						server.sendBundle(latency,[\n_set, node, \on, this.isOn]);
					};
				},
				\action2_ -> {|me|
					this.soloAlt(me.value);
					if (node.notNil) {
						server.sendBundle(nil,[\n_set, node, \on, this.isOn]);
					}
				 }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle|
					this.onOff(val,latency,send,toggle);
					if (node.notNil) {
						server.sendBundle(latency,[\n_set, node, \on, this.isOn]);
					};
				},
				\action2_ -> {|me|	
					this.onOffAlt(me.value);
					if (node.notNil) {
						server.sendBundle(nil,[\n_set, node, \on, this.isOn]);
					};
				}],
					
			// 2.master amp
			[\db6,midiControl, 2, "Master volume",
				(\label_:"Volume" , \numberFunc_:'db',mouseDownAction_:{hack[\fadeTask].stop}),
				{|me,val,latency,send,toggle|
					this.setSynthArgVP(2,val,\amp,val.dbamp,latency,send);
				}],	
				
			// 3. in channels
			[0,[0,LNX_AudioDevices.numInputBusChannels/2,\linear,1],
				midiControl, 3, "In Channel",
				(\items_:LNX_AudioDevices.inputMenuList),
				{|me,val,latency,send|
					var in  = LNX_AudioDevices.firstInputBus+(val*2);
					this.setSynthArgVH(3,val,\inputChannels,in,latency,send);
				}],
				
			// 4. out channels		
			[0,\audioOut, midiControl, 4, "Output channels",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var channel = LNX_AudioDevices.getOutChannelIndex(val);
					this.instOutChannel_(channel);
					this.setPVPModel(4,val,0,send);   // to test on network
				}], // test on network
								
			// 5.master pan
			[\pan, midiControl, 5, "Pan",
				(\numberFunc_:\pan, \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setSynthArgVH(5,val,\pan,val,latency,send);
				}],
				
			// 6. peak level
			[0.7, \unipolar,  midiControl, 6, "Peak Level",
				{|me,val,latency,send| this.setPVP(6,val,latency,send) }],
											
			// 7. send channel
			[-1,\audioOut, midiControl, 7, "Send channel",
				(\label_:"Send", \items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					this.setSynthArgVH(7,val,
						\sendChannels,LNX_AudioDevices.getOutChannelIndex(val),latency,send);
				}],
			
			// 8. sendAmp
			[-inf,\db6,midiControl, 8, "Send amp", (label_:"Send"),
				{|me,val,latency,send,toggle|
					this.setSynthArgVH(8,val,\sendAmp,val.dbamp,latency,send);
				}], 		
				
			// 9. channelSetup
			[0,[0,3,\lin,1], midiControl, 9, "Channel Setup",
				(\items_:["Left & Right","Left + Right","Left","Right"]),
				{|me,val,latency,send|
					this.setSynthArgVH(9,val,\channelSetup,val,latency,send);
				}],
				
			// 10. syncDelay
			[[-1,1,\lin,0.001,0], midiControl, 10, "Sync",
				(label_:"Sync", zeroValue_:0),
				{|me,val,latency,send|
					this.setPVP(10,val,latency,send);
					this.syncDelay_(val.clip(-inf,0).abs); // this will update delay as well
				}],
			
			// 11. onSolo turns audioIn, seq or both on/off
			[1, [0,2,\lin,1],  midiControl, 11, "On/Off Model",
				(\items_:["Audio In","Sequencer","Both"]),
				{|me,val,latency,send|
					this.setPVP(11,val,latency,send);
					this.updateOnSolo;
				}],	
			
			// 12. midi clock out
			[1, \switch, midiControl, 12, "MIDI Clock", (strings_:["MIDI Clock"]),
				{|me,val,latency,send|
					this.setPVP(12,val,latency,send);
					if (val.isFalse) { midi.stop(latency)};
				}],
				
			// 13. use controls in presets
			[0, \switch, midiControl, 13, "Controls Preset", (strings_:["Controls"]),
				{|me,val,latency,send|	
					this.setPVP(13,val,latency,send);
					if (val.isTrue) {
						presetExclusion=[0,1];
					}{
						presetExclusion=[0,1]++(14..141);
					}	
				}],
		];
		
		// 14..141 all 127 midi controls
		(0..127).do{|i|
			template = template.add(
				[0, \midi, midiControl, 14+i, "CC"++(i+1),
					(\label_:(i+1).asString , \numberFunc_:'int'),
					{|me,val,latency,send,toggle|
						this.setPVPModel(i, val, latency, send); // network this
						midi.control(i, val, latency);           // send midi control data
					}],	
			);
		};
		
		#models,defaults=template.generateAllModels;

		presetExclusion=(0..1)++(14..141);
		randomExclusion=(0..1)++10;
		autoExclusion=[];

	}

	// sync stuff ///////////////////////////
	
	delayTime{^(this.mySyncDelay.clip(0,inf))+(p[10].clip(0,inf))  }
	iSyncDelayChanged{ this.setDelay }
	setDelay{ if (node.notNil) {server.sendBundle(nil,[\n_set, node, \delay, this.delayTime])} }
	
	// clock in //////////////////////////////
	
	// clock in for pRoll sequencer
	clockIn3{|beat,absTime,latency,absBeat| sequencer.do(_.clockIn3(beat,absTime,latency,absBeat))}
	
	// reset sequencers posViews
	clockStop {
		sequencer.do(_.clockStop(studio.actualLatency));
		seqOutBuffer.releaseAll;
	}
	
	// remove any clock hilites
	clockPause{
		sequencer.do(_.clockPause(studio.actualLatency));
		seqOutBuffer.releaseAll;	
	}
	
	// clock in for midi out clock methods
	midiSongPtr{|songPtr,latency| if (p[12].isTrue) { midi.songPtr(songPtr,latency) } } 
	midiStart{|latency| if (p[12].isTrue) { midi.start(latency) } }
	midiClock{|latency| if (p[12].isTrue) { midi.midiClock(latency) } }
	midiContinue{|latency| if (p[12].isTrue) { midi.continue(latency) } }
	midiStop{|latency| if (p[12].isTrue) { midi.stop(latency) } }
	
	// disk i/o ///////////////////////////////
		
	// for your own saving
	iGetSaveList{ ^sequencer.getSaveList }
	
	// for your own loading
	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		sequencer.putLoadList(l.popEND("*** END OBJECT DOC ***"));
	}
	
	//iFreeAutomation{ sequencer.freeAutomation }
	
	// free this
	iFree{ sequencer.do(_.free) }

	// PRESETS /////////////////////////
	
	// get the current state as a list
	iGetPresetList{ ^sequencer.iGetPresetList }
	
	// add a statelist to the presets
	iAddPresetList{|l| sequencer.iAddPresetList(l.popEND("*** END Score DOC ***").reverse) }
	
	// save a state list over a current preset
	iSavePresetList{|i,l| sequencer.iSavePresetList(i,l.popEND("*** END Score DOC ***").reverse) }
	
	// for your own load preset
	iLoadPreset{|i,newP,latency| sequencer.iLoadPreset(i) }
	
	// for your own remove preset
	iRemovePreset{|i| sequencer.iRemovePreset(i) }
	
	// for your own removal of all presets
	iRemoveAllPresets{ sequencer.iRemoveAllPresets }
	
	// clear the sequencer
	clearSequencer{ sequencer.clear }

	// program stuff ////////////////////////*****************************************************

	//  this is overriden to force program changes & exclusions by put p[89 & 90] 1st
	loadPreset{|i,latency|
		var presetToLoad, oldP;
		
		presetToLoad=presetMemory[i].copy;
		
		// 13.use controls
		models[13].lazyValueAction_(presetToLoad[13],latency,false);

		// exclude these parameters
		presetExclusion.do{|i| presetToLoad[i]=p[i]};
		
		// update models
		presetToLoad.do({|v,j|	
			if (j!=13) {
				if (p[j]!=v) {
					models[j].lazyValueAction_(v,latency,false)
				}
			};
		});
		
		this.iLoadPreset(i,presetToLoad,latency);    // any instrument specific details
		oldP=p.copy;
		p=presetToLoad;               // copy the paramaters to p (is this needed any more?)
		this.updateDSP(oldP,latency); // and update any dsp
		
	}
	
	//////////////////////////*****************************************************
	// GUI
	
	*thisWidth  {^680}
	*thisHeight {^423}
	
	createWindow{|bounds| this.createTemplateWindow(bounds,Color.grey(0),false) }

	iPostNew{ { sequencer.resizeToWindow }.defer(0.02) }

	// create all the GUI widgets while attaching them to models
	createWidgets{
										
		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\labelShadow_	: false,
						\colors_      : (\background: Color(0.6 , 0.562, 0.5),
										\label:Color.black,
										\string:Color.black,
									   \focus:Color.clear));
	
		gui[\midiTheme]= ( \rounded_	: true,
						\canFocus_	: false,
						\shadow_		: true,
						\colors_		: (	\up 		: Color(0.31,0.31,0.49),
										\down	: Color(0.31,0.31,0.49),
										\string	: Color.white));
										
		gui[\knobTheme]=( \labelShadow_	: true,
						\numberWidth_	: (-20), 
						\numberFont_	: Font("Helvetica",10),
						\colors_		: (	\on		: Color.orange,
										\label	: Color.white,
										\numberUp	: Color.black,
										\numberDown : Color.white));
										
		gui[\knobTheme2]=(\colors_		: (\on :  Color.orange, 
									   \numberUp:Color.black,
									   \numberDown:Color.white,
									   \disabled:Color.black,
									   \label:Color.white),
						\labelShadow_ : true,
						\numberFont_  : Font("Helvetica",10),
						\labelFont_	: Font("Helvetica",11)
						);
								
		gui[\theme2]=(	\orientation_  : \horiz,
						\resoultion_	 : 3,
						\visualRound_  : 0.001,
						\rounded_      : true,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.6 , 0.562, 0.5),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.black,
										\focus : Color(0,0,0,0)));

		gui[\onOffTheme1]=( \font_		: Font("Helvetica-Bold", 12),
						 \rounded_	: true,
						 \colors_     : (\on : Color(20/77,1,20/77), \off: Color(0.4,0.4,0.4)));
										
		gui[\onOffTheme2]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color.orange, \off: Color(0.4,0.4,0.4)));
						 
		gui[\onOffTheme3]=( \font_		: Font("Helvetica-Bold", 12),
						 \rounded_	: true,
						 \colors_     : (\on : Color.orange, \off: Color(0.4,0.4,0.4)));
		
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));
	
		// widgets
						
		// 3. in	
		MVC_PopUpMenu3(models[3],window,Rect(5,5,70,17), gui[\menuTheme ] );
	
		// 9. channelSetup
		MVC_PopUpMenu3(models[9],window,Rect(85,5,75,17), gui[\menuTheme ] );
		
		// 10. syncDelay
		MVC_NumberBox(models[10], window,Rect(194, 30-25, 40, 18),  gui[\theme2])
			.labelShadow_(false)
			.color_(\label,Color.white);
			
		MVC_StaticText(Rect(100+140,30-25, 40, 18), window,)
			.string_("sec(s)")
			.font_(Font("Helvetica",10))
			.shadow_(false)
			.color_(\string,Color.white);
		
		// MIDI Settings
 		MVC_FlatButton(window,Rect(277, 4, 43, 19),"MIDI")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(0.6 , 0.562, 0.5))
			.color_(\down,Color(0.6 , 0.562, 0.5) )
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ this.createMIDIInOutModelWindow(window) };
			
		// MIDI Control
 		MVC_FlatButton(window,Rect(327, 4, 43, 19),"Cntrl")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(0.6 , 0.562, 0.5) )
			.color_(\down,Color(0.6 , 0.562, 0.5) )
			.color_(\string,Color.white)
			.resize_(9)
			.action_{  LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front  };
			
		MVC_PlainSquare(window,  Rect(668,27, 5, 5 ))
			.color_(\off, Color(0.6 , 0.562, 0.5));
		
		gui[\masterTabs]=MVC_TabbedView(window, Rect(9, 14, 670, 310-15), offset:((526)@(-2)))
			.labels_(["Control","Piano Roll"])
			.font_(Font("Helvetica", 12))
			.tabPosition_(\top)
			.unfocusedColors_(Color(0.5,0.5,0.5,0.8)! 2)
			.labelColors_(  Color(0.6 , 0.562, 0.5)!2)
			.backgrounds_(  Color.clear!2)
			.tabCurve_(8)
		//	.tabWidth_([63,63])
			.tabHeight_(15)
			.followEdges_(true)
			.value_(0);
		
		// control tab
		
		gui[\controlsTab] = MVC_RoundedScrollView(gui[\masterTabs].mvcTab(0),Rect(4, 4, 654, 269))
			.color_(\border,  Color(0.6 , 0.562, 0.5))
			.color_(\background, Color.grey(0.3))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(true)
			.autohidesScrollers_(true);
			
			
		gui[\sequencerTab] = MVC_RoundedCompositeView(gui[\masterTabs].mvcTab(1), Rect(4, 4, 654, 269))
			.color_(\border,  Color(0.6 , 0.562, 0.5))
			.color_(\background, Color.grey(0.207))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);


		// levels
		MVC_FlatDisplay(this.peakLeftModel,gui[\controlsTab],Rect(5+630-18, 9, 6, 160));
		MVC_FlatDisplay(this.peakRightModel,gui[\controlsTab],Rect(13+630-18, 9, 6, 160));
		MVC_Scale(gui[\controlsTab],Rect(11+630-18, 9, 2, 160));

		// 2. channel volume
		MVC_SmoothSlider(gui[\controlsTab],models[2],Rect(587, 9, 27, 160))
			.label_(nil)
			.showNumberBox_(false)
			.color_(\hilite,Color(1,1,1,0.3))
			.color_(\knob,Color.orange);	
			
		// 1. channel onOff	
		MVC_OnOffView(models[1], gui[\controlsTab],Rect(583, 177, 26, 18),gui[\onOffTheme1])
			.rounded_(true)
			.permanentStrings_(["On","On"]);
		
		// 0. channel solo
		MVC_OnOffView(models[0], gui[\controlsTab], Rect(616, 177, 26, 18),gui[\soloTheme])
			.rounded_(true);

		// 11. onSolo turns audioIn, seq or both on/off
		MVC_PopUpMenu3(models[11], gui[\controlsTab] ,Rect(576, 202, 70, 16), gui[\menuTheme] );

		// 11. midi clock out
		MVC_OnOffView(models[12], gui[\controlsTab], Rect(575, 223, 73, 19),gui[\onOffTheme3]);

		// 13. use controls in presets
		MVC_OnOffView(models[13], gui[\controlsTab], Rect(575, 245, 73, 19),gui[\onOffTheme3]);

		// 14..141   all 127 midi controls
		(0..127).do{|i|
			MVC_MyKnob3(models[14+i], gui[\controlsTab],gui[\knobTheme2],
				Rect(10+(i%16*35), 20+(i.div(16)*53), 25, 25)
			);
		};
	
		// the preset interface
		presetView=MVC_PresetMenuInterface(window,380@5,50,
				Color(0.7,0.65,0.65)/1.6,
				Color(0.7,0.65,0.65)/3,
				Color(0.7,0.65,0.65)/1.5,
				Color(0.7,0.65,0.65),
				Color.black
			);
		this.attachActionsToPresetGUI;	
		
		// piano roll	
		sequencer.createWidgets(gui[\sequencerTab],Rect(2,9,650,256)
				,(\selectRect: Color.white,
				 \background: Color(0.43, 0.41, 0.42),
				 \velocityBG: Color(0, 0, 0, 0.5),
				 \buttons:    Color.orange,
				 \boxes:		Color(0.1, 0.05, 0, 0.5),
				 \noteBG:     Color.orange,
				 \noteBGS:    Color.orange*1.5,
				 \noteBS:     Color.black,
				 \velocitySel: Color.orange*1.5
				)
				);
		
		// the keyboard, fix bug so we don't need this scrollView
		gui[\keyboardOuterView]=MVC_CompositeView(window,Rect(12,320,653,93))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);
			
		keyboardView=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0,0,655,93),6,12)
			.keyboardColor_(Color(1,0.5,0)+0.3)
			.pipeFunc_{|pipe|
				sequencer.pipeIn(pipe);     // to sequencer
				this.toMIDIOutBuffer(pipe); // and midi out
			};

	}
	
	// dsp stuFF for audio in /////////////////////////////////////////////////////////////////////

	// uses same synth def as LNX_AudioIn
	
	startDSP{
		synth = Synth.tail(instGroup,"LNX_AudioIn");
		node  = synth.nodeID;	
	}
		
	stopDSP{ synth.free }
	
	// used for noteOff in sequencers
	// efficiency issue: this is called 3 times in alt_solo over a network
	stopNotesIfNeeded{
		this.updateOnSolo;
	}
	
	updateOnSolo{
		switch (p[11].asInt)
			{0} {
				// "Audio In"
				if (node.notNil) { server.sendBundle(nil,[\n_set, node, \on, this.isOn]) };
			}
			{1} {
				// "Sequencer"
				if (node.notNil) { server.sendBundle(nil,[\n_set, node, \on, true]) };
				if (this.isOff) {this.stopAllNotes};
			}
			{2} {
				// "Both"
				if (node.notNil) { server.sendBundle(nil,[\n_set, node, \on, this.isOn]) };
				if (this.isOff) {this.stopAllNotes};
			};		
	}
	
	updateDSP{|oldP,latency|
		var in  = LNX_AudioDevices.firstInputBus+(p[3]*2);
		var out, on;				
		if (p[4]>=0) {
			out = p[4]*2
		}{	
			out = LNX_AudioDevices.firstFXBus+(p[4].neg*2-2);
		};
		this.instOutChannel_(out,latency);
		if (p[11]==1) { on=true } { on=this.isOn };
				
		server.sendBundle(latency,
			[\n_set, node, \amp,p[2].dbamp],
			[\n_set, node, \pan,p[5]],
			[\n_set, node, \inputChannels,in],
			[\n_set, node, \outputChannels,this.instGroupChannel],
			[\n_set, node, \on, on],
			[\n_set, node, \sendChannels,LNX_AudioDevices.getOutChannelIndex(p[7])],
			[\n_set, node, \sendAmp, p[8].dbamp],
			[\n_set, node, \channelSetup, p[9]],
			[\n_set, node, \delay, this.delayTime ]
			
		);
	}

} // end ////////////////////////////////////
