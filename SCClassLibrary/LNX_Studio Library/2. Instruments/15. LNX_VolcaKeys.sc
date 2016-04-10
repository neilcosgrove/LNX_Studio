
// audio in & it can do +ive & -ive time syncing to studio clock!!!

LNX_VolcaKeys : LNX_InstrumentTemplate {

	var <sequencer, <keyboardView, <midiInBuffer, <midiOutBuffer, <seqOutBuffer;

	*initClass{ Class.initClassTree(LNX_VolcaBeats) }
	*isVisible{ ^LNX_VolcaBeats.isVisible }

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName      {^"Volca Keys"}
	*sortOrder       {^2}
	isInstrument     {^true}
	canBeSequenced   {^true}
	isMixerInstrument{^true}
	mixerColor       {^Color(0.728, 0.6715, 0.606)} // colour in mixer
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
	syncModel   {^models[10]}

	header { 
		// define your document header details
		instrumentHeaderType="SC Volca Keys Doc";
		version="v1.0";		
	}
	
	// an immutable list of methods available to the network
	interface{^#[\netPipeIn]}
		
	// MIDI patching ///////////////////////////////////////////////////////

	iInitVars{
		// the main sequencer
		sequencer = LNX_PianoRollSequencer(id++\pR)
			.pipeOutAction_{|pipe|
				if (((p[27]>0)&&(this.isOff)).not) {seqOutBuffer.pipeIn(pipe)};
			}
			.releaseAllAction_{ seqOutBuffer.releaseAll(studio.actualLatency) }
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
		if (instOnSolo.isOff and: {p[27]>0} ) {^this}; // drop if sequencer off
		if (pipe.historyIncludes(this)) {^this};       // drop to prevent internal feedback loops
		switch (pipe.kind)
			{\program} { // program
				//this.program(pipe.program,pipe.latency);
				^this // drop	
			};
		
		// network if needed		
		if ((p[30].isTrue) and:
			{#[\external, \controllerKeyboard, \MIDIIn, \keyboard].includes(pipe.source)}) {
				if (((pipe.source==\controllerKeyboard) and:{studio.isCntKeyboardNetworked}).not) 
					{ api.sendOD(\netPipeIn, pipe.kind, pipe.note, pipe.velocity)}
		};
			
		if (pipe.isNote) {	 midiInBuffer.pipeIn(pipe) }; // to in Buffer
	}
	
	// networked midi pipes	
	netPipeIn{|type,note,velocity|
		switch (type.asSymbol)
			{\noteOn } { this.pipeIn(LNX_NoteOn(note,velocity,nil,\network)) }
			{\noteOff} { this.pipeIn(LNX_NoteOff(note,velocity,nil,\network)) }
		;
	}
	
	// midi coming from in buffer
	fromInBuffer{|pipe|
		sequencer.pipeIn(pipe);                 // to the sequencer
		keyboardView.pipeIn(pipe,Color.orange); // to the gui keyboard
		this.toMIDIOutBuffer(pipe);             // to midi out buffer
	}
	
	// output from the sequencer buffer
	fromSequencerBuffer{|pipe|
		keyboardView.pipeIn(pipe,Color.orange); // to the gui keyboard
		this.toMIDIOutBuffer(pipe);             // and to the out buffer
	}
	
	// to the output buffer
	toMIDIOutBuffer{|pipe|
		if (instOnSolo.isOff and: {p[27]>0} ) {^this}; // drop if sequencer off
		midiOutBuffer.pipeIn(pipe);             // to midi out buffer
	}
	
	// and finally to the midi out
	toMIDIPipeOut{|pipe|
		midi.pipeIn(pipe.addToHistory(this).addLatency(syncDelay)); // add this to its history
	}

	// release all played notes, uses midi Buffer
	stopAllNotes{ 
		midiInBuffer.releaseAll(studio.actualLatency);
		seqOutBuffer.releaseAll(studio.actualLatency); 
		midiOutBuffer.releaseAll(studio.actualLatency); 
		{keyboardView.clear}.defer(studio.actualLatency);
	}


	///////////////////////////////////////////////////////

	// the models
	initModel {
		
		var template=[

			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me|	this.onOffAlt(me.value) }],
					
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
			[0,[0,4,\lin,1], midiControl, 9, "Channel Setup",
				(\items_:["Left & Right","Left + Right","Left","Right","No Audio"]),
				{|me,val,latency,send|
					this.setSynthArgVH(9,val,\channelSetup,val,latency,send);
				}],
				
			// 10. syncDelay
			[\sync,{|me,val,latency,send|
				this.setPVPModel(10,val,latency,send);
				this.syncDelay_(val);
			}],
					
			// 11. midiControl 5. Portamento
			[0,\midi, midiControl, 11, "Portamento", (\label_:"Portamento" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(11, val, latency, send);   // network this
					midi.control(5, val, latency +! syncDelay); // send midi control data
				}],	
				
			// 12. midiControl 11. Expression
			[127, \midi, midiControl, 12,"Expression",(\label_:"Expression" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(12, val, latency, send);    // network this
					midi.control(11, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 13. midiControl 40. Voice
			[0,\midi, midiControl, 13, "Voice", (\label_:"Voice" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(13, val, latency, send);    // network this
					midi.control(40, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 14. midiControl 41. Octave
			[\midi, midiControl, 14, "Octave", (\label_:"Octave" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(14, val, latency, send);    // network this
					midi.control(41, val, latency +! syncDelay); // send midi control data
				}],	

			// 15. midiControl 42. Detune
			[0, \midi, midiControl, 15, "Detune", (\label_:"Detune", \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(15, val, latency, send);    // network this
					midi.control(42, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 16. midiControl 43. VCO Eg Int
			[0, \midi, midiControl, 16, "VCO Eg Int", (\label_:"VCO Eg Int" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(16, val, latency, send);    // network this
					midi.control(43, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 17. midiControl 44. Cutoff
			[\midi, midiControl, 17, "Cutoff", (\label_:"Cutoff" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(17, val, latency, send);    // network this
					midi.control(44, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 18. midiControl 45. VCF Eg Int
			[\midi, midiControl, 18, "VCF Eg Int", (\label_:"VCF Eg Int" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(18, val, latency, send);    // network this
					midi.control(45, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 19. midiControl 46. LFO Rate
			[\midi, midiControl, 19, "LFO Rate", (\label_:"LFO Rate" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(19, val, latency, send);    // network this
					midi.control(46, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 20. midiControl 47. LFO Pitch
			[0,\midi, midiControl, 20, "LFO Pitch", (\label_:"LFO Pitch" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(20, val, latency, send);    // network this
					midi.control(47, val, latency +! syncDelay); // send midi control data
				}],	
				
			// 21. midiControl 48. LFO Cutoff
			[0,\midi, midiControl, 21, "LFO Cutoff", (\label_:"LFO Cutoff" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(21, val, latency, send);    // network this
					midi.control(48, val, latency +! syncDelay); // send midi control data
				}],	
				
			// 22. midiControl 49. Attack
			[0,\midi, midiControl, 22, "Attack", (\label_:"Attack" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(22, val, latency, send);    // network this
					midi.control(49, val, latency +! syncDelay); // send midi control data
				}],	
				
			// 23. midiControl 50. Decay/Release
			[\midi, midiControl, 23, "Decay", (\label_:"Decay" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(23, val, latency, send);    // network this
					midi.control(50, val, latency +! syncDelay); // send midi control data
				}],	
				
			// 24. midiControl 51. Sustain
			[\midi, midiControl, 24, "Sustain", (\label_:"Sustain" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(24, val, latency, send);    // network this
					midi.control(51, val, latency +! syncDelay); // send midi control data
				}],	
				
			// 25. midiControl 52. Delay Time
			[\midi, midiControl, 25, "Delay Time", (\label_:"Delay Time" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(25, val, latency, send);    // network this
					midi.control(52, val, latency +! syncDelay); // send midi control data
				}],	
				
			// 26. midiControl 53. Feedback
			[0,\midi, midiControl, 26, "Feedback", (\label_:"Feedback" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(26, val, latency, send);    // network this
					midi.control(53, val, latency +! syncDelay); // send midi control data
				}],
							
			// 27. onSolo turns audioIn, seq or both on/off
			[1, [0,2,\lin,1],  midiControl, 27, "On/Off Model",
				(\items_:["Audio In","Sequencer","Both"]),
				{|me,val,latency,send|
					this.setPVP(27,val,latency,send);
					this.updateOnSolo;
				}],
				
			// 28. midi clock out
			[0, \switch, midiControl, 28, "MIDI Clock", (strings_:["MIDI Clock"]),
				{|me,val,latency,send|
					this.setPVPModel(28,val,latency,send);
					if (val.isFalse) { midi.stop(latency +! syncDelay) };
				}],
				
			// 29. use controls in presets
			[0, \switch, midiControl, 29, "Controls Preset", (strings_:["Controls"]),
				{|me,val,latency,send|	
					this.setPVPModel(29,val,latency,send);
					if (val.isTrue) {
						presetExclusion=[0,1,10,28];
					}{
						presetExclusion=[0,1,10,28]++(11..26);
					}	
				}],
		
			// 30.network keyboard
			[0, \switch, midiControl, 30, "Network", (strings_:["Net"]),
				{|me,val,latency,send|	this.setPVH(30,val,latency,send) }],
		
		];
		
		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change, rand or automation
		presetExclusion=[0,1,10,28]++(11..26);
		randomExclusion=[0,1,10,28];
		autoExclusion=[10];

	}

	// clock in //////////////////////////////
	
	// clock in for pRoll sequencer
	clockIn3{|beat,absTime,latency,absBeat| sequencer.do(_.clockIn3(beat,absTime,latency,absBeat))}
	
	// reset sequencers posViews
	clockStop{
		sequencer.do(_.clockStop(studio.actualLatency));
		seqOutBuffer.releaseAll(studio.actualLatency);
	}
	
	// remove any clock hilites
	clockPause{
		sequencer.do(_.clockPause(studio.actualLatency));
		seqOutBuffer.releaseAll(studio.actualLatency);	
	}
	
	// clock in for midi out clock methods
	midiSongPtr{|songPtr,latency| if (p[28].isTrue) { midi.songPtr(songPtr,latency +! syncDelay) }} 
	midiStart{|latency|           if (p[28].isTrue) { midi.start(latency +! syncDelay) } }
	midiClock{|latency|           if (p[28].isTrue) { midi.midiClock(latency +! syncDelay) } }
	midiContinue{|latency|        if (p[28].isTrue) { midi.continue(latency +! syncDelay) } }
	midiStop{|latency|            if (p[28].isTrue) { midi.stop(latency +! syncDelay) } }
	
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
		
		// 29.use controls
		models[29].lazyValueAction_(presetToLoad[29],latency,false);

		// exclude these parameters
		presetExclusion.do{|i| presetToLoad[i]=p[i]};
		
		// update models
		presetToLoad.do({|v,j|	
			if (j!=29) {
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
	
	iPostLoad{
		if (models[29].isTrue) {
			(11..28).do{|j| models[j].doLazyValueAction_(p[j],0,false) }
		};
	}
		

	//////////////////////////*****************************************************
	// GUI
	
	*thisWidth  {^680}
	*thisHeight {^423}
	
	createWindow{|bounds| this.createTemplateWindow(bounds,Color.black,false) }

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
			
		// 1. channel onOff	
		MVC_OnOffView(models[1], window,Rect(10, 5, 26, 18),gui[\onOffTheme1])
			.permanentStrings_(["On","On"]);
		
		// 0. channel solo
		MVC_OnOffView(models[0], window, Rect(40, 5, 26, 18),gui[\soloTheme])
			.rounded_(true);
						
		// 3. in	
		MVC_PopUpMenu3(models[3],window,Rect(80,5,70,17), gui[\menuTheme ] );
	
		// 9. channelSetup
		MVC_PopUpMenu3(models[9],window,Rect(160,5,75,17), gui[\menuTheme ] );
				
		// MIDI Settings
 		MVC_FlatButton(window,Rect(250, 4, 43, 19),"MIDI")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(0.6 , 0.562, 0.5))
			.color_(\down,Color(0.6 , 0.562, 0.5) )
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ this.createMIDIInOutModelWindow(window,
				colors:(border1:Color(0.1221, 0.0297, 0.0297), border2: Color(0.6 , 0.562, 0.5))
			) };
			
		// MIDI Control
 		MVC_FlatButton(window,Rect(300, 4, 43, 19),"Cntrl")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(0.6 , 0.562, 0.5))
			.color_(\down,Color(0.6 , 0.562, 0.5) )
			.color_(\string,Color.white)
			.resize_(9)
			.action_{  LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front  };
			

		MVC_PlainSquare(window, Rect(668,27, 5, 5 ))
			.color_(\off, Color(0.6 , 0.562, 0.5));
		
		gui[\masterTabs]=MVC_TabbedView(window, Rect(9, 14, 670, 310-15), offset:((345+181)@(-2)))
			.labels_(["Control","Piano Roll"])
			.font_(Font("Helvetica", 12))
			.tabPosition_(\top)
			.unfocusedColors_(Color(0.5,0.5,0.5,0.5)! 2)
			.labelColors_(  Color(0.6 , 0.562, 0.5)!2)
			.backgrounds_(  Color.clear!2)
			.tabCurve_(8)
			.tabHeight_(15)
			.followEdges_(true)
			.value_(0);
		
		// control tab
		
		gui[\controlsTab] = MVC_RoundedCompositeView(gui[\masterTabs].mvcTab(0), Rect(4,4,654,269))
			.color_(\border, Color(0.6 , 0.562, 0.5))
			.color_(\background,Color.grey(0.3))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);
			
		MVC_Text(gui[\controlsTab],Rect(7, 223,139, 42))
			.align_(\center)
			.shadow_(false)
			.penShadow_(true)
			.font_(Font("AvenirNext-Heavy",22))
			.string_("Volca Keys");
			
		gui[\sequencerTab] = MVC_RoundedCompositeView(gui[\masterTabs].mvcTab(1),Rect(4,4,654,269))
			.color_(\border, Color(0.6 , 0.562, 0.5))
			.color_(\background,Color.grey(0.3))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);			

		// levels
		MVC_FlatDisplay(this.peakLeftModel,gui[\controlsTab],Rect(635, 11, 6, 150));
		MVC_FlatDisplay(this.peakRightModel,gui[\controlsTab],Rect(643, 11, 6, 150));
		MVC_Scale(gui[\controlsTab],Rect(641, 11, 2, 150));

		// 2. channel volume
		MVC_SmoothSlider(gui[\controlsTab],models[2],Rect(600, 11, 27, 150))
			.label_(nil)
			.showNumberBox_(false)
			.color_(\hilite,Color(0,0,0,0.5))
			.color_(\knob,Color.white);	
		
		
		// 27. onSolo turns audioIn, seq or both on/off
		MVC_PopUpMenu3(models[27], gui[\controlsTab] ,Rect(579, 174, 70, 16), gui[\menuTheme] );

		// 28. midi clock out
		MVC_OnOffView(models[28], gui[\controlsTab], Rect(577, 198, 73, 19),gui[\onOffTheme3]);

		// 29. use controls in presets
		MVC_OnOffView(models[29], gui[\controlsTab], Rect(577, 222, 73, 19),gui[\onOffTheme3]);

		// 11. midiControl 5. Portamento
		MVC_MyKnob3(models[11], gui[\controlsTab], Rect(126, 105, 28, 28),gui[\knobTheme2]);
		// 12. midiControl 11. Expression
		MVC_MyKnob3(models[12], gui[\controlsTab], Rect(484, 175, 28, 28),gui[\knobTheme2]);
		// 13. midiControl 40. Voice
		MVC_MyKnob3(models[13], gui[\controlsTab], Rect(35, 42, 56, 56),gui[\knobTheme2]);
		// 14. midiControl 41. Octave
		MVC_MyKnob3(models[14], gui[\controlsTab], Rect(35, 133, 56, 56),gui[\knobTheme2]);
		// 15. midiControl 42. Detune
		MVC_MyKnob3(models[15], gui[\controlsTab], Rect(126, 35, 28, 28),gui[\knobTheme2]);
		
		// 16. midiControl 43. VCO Eg Int
		MVC_MyKnob3(models[16], gui[\controlsTab], Rect(126, 175, 28, 28),gui[\knobTheme2]);
		// 17. midiControl 44. Cutoff
		MVC_MyKnob3(models[17], gui[\controlsTab], Rect(203, 35, 28, 28),gui[\knobTheme2]);
		// 18. midiControl 45. VCF Eg Int
		MVC_MyKnob3(models[18], gui[\controlsTab], Rect(210, 175, 28, 28),gui[\knobTheme2]);
		// 19. midiControl 46. LFO Rate
		MVC_MyKnob3(models[19], gui[\controlsTab], Rect(280, 35, 28, 28),gui[\knobTheme2]);
		// 20. midiControl 47. LFO Pitch
		MVC_MyKnob3(models[20], gui[\controlsTab], Rect(280, 105, 28, 28),gui[\knobTheme2]);
		
		// 21. midiControl 48. LFO Cutoff
		MVC_MyKnob3(models[21], gui[\controlsTab], Rect(280, 175, 28, 28),gui[\knobTheme2]);
		// 22. midiControl 49. Attack
		MVC_MyKnob3(models[22], gui[\controlsTab], Rect(357, 35, 28, 28),gui[\knobTheme2]);
		// 23. midiControl 50. Decay/Release
		MVC_MyKnob3(models[23], gui[\controlsTab], Rect(357, 105, 28, 28),gui[\knobTheme2]);
		// 24. midiControl 51. Sustain
		MVC_MyKnob3(models[24], gui[\controlsTab], Rect(357, 175, 28, 28),gui[\knobTheme2]);
		// 25. midiControl 52. Delay Time
		MVC_MyKnob3(models[25], gui[\controlsTab], Rect(441, 105, 28, 28),gui[\knobTheme2]);
		// 26. midiControl 53. Feedback
		MVC_MyKnob3(models[26], gui[\controlsTab], Rect(525, 105, 28, 28),gui[\knobTheme2]);
			
		// the preset interface
		presetView=MVC_PresetMenuInterface(window,350@4,50,
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
				),
				parentViews: [ window, gui[\masterTabs].mvcTab(1)]
				);
		
		// the keyboard, fix bug so we don't need this scrollView
		gui[\keyboardOuterView]=MVC_CompositeView(window,Rect(12,320,653,93))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);
				
		// 30.network keyboard
		MVC_OnOffView(models[30], gui[\controlsTab], Rect(617, 246, 32, 19), gui[\onOffTheme1]);
			
		keyboardView=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0,0,655,93),6,12)
			.keyboardColor_(Color(1,0.5,0)+0.3)
			.pipeFunc_{|pipe|
				sequencer.pipeIn(pipe);     // to sequencer
				this.toMIDIOutBuffer(pipe); // and midi out
				if (p[30].isTrue) {
					api.sendOD(\netPipeIn, pipe.kind, pipe.note, pipe.velocity)}; // and network
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
	stopNotesIfNeeded{|latency|
		this.updateOnSolo(latency);
	}
	
	updateOnSolo{|latency|
		switch (p[27].asInt)
			{0} {
				// "Audio In"
				if (node.notNil) {server.sendBundle(latency +! syncDelay,
					[\n_set, node, \on, this.isOn])};
			}
			{1} {
				// "Sequencer"
				if (node.notNil) {server.sendBundle(latency +! syncDelay,
					[\n_set, node, \on, true])};
				if (this.isOff) {this.stopAllNotes};
			}
			{2} {
				// "Both"
				if (node.notNil) {server.sendBundle(latency +! syncDelay,
					[\n_set, node, \on, this.isOn])};
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
		if (p[27]==1) { on=true } { on=this.isOn };
				
		server.sendBundle(latency,
			[\n_set, node, \amp,p[2].dbamp],
			[\n_set, node, \pan,p[5]],
			[\n_set, node, \inputChannels,in],
			[\n_set, node, \outputChannels,this.instGroupChannel],
			[\n_set, node, \on, on],
			[\n_set, node, \sendChannels,LNX_AudioDevices.getOutChannelIndex(p[7])],
			[\n_set, node, \sendAmp, p[8].dbamp],
			[\n_set, node, \channelSetup, p[9]]
			
		);
	}

} // end ////////////////////////////////////
