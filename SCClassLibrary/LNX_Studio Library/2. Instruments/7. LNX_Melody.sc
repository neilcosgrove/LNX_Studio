
// Melody Maker ////////////////////////////////////////////////////////////////////////////////
		
LNX_Melody : LNX_InstrumentTemplate {
	
	var <musicMod, <storeChordsMod, <sequencer, <chordQuantiserMod, <arpeggiatorMod;
	var <midiBuffer1, <midiBuffer2, <midiBuffer3, <midiBuffer4, <midiBuffer5, <midiBuffer6;
	var <lastKeyboardNote, <arpegSequencer, <multiPipeOut;	
	*new {arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName   {^"Melody Maker"}
	*sortOrder    {^2}
	mixerColor    {^Color(1,0.75,1,0.4)} // colour in mixer
	isMIDI        {^true}
	canBeSequenced{^true}
	isInstrument  {^true}
	onColor       {^Color(0.5,0.7,1)} 
	clockPriority {^4}
	alwaysOnModel {^models[23]}
	alwaysOn      {^models[23].isTrue} // am i? used by melody maker to change onOff widgets
	canAlwaysOn   {^true} // can i?
	syncModel     {^models[31]}
	
	// an immutable list of methods available to the network
	interface{^#[\netMIDI] }

	header { 
		// define your document header details
		instrumentHeaderType="SC Melody Doc";
		version="v1.2";		
	}

	// the models
	initModel {

		#models,defaults=[
			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me| this.onOffAlt(me.value) }],
				
			// 2.MIDI low 
			[0, \MIDInote, midiControl, 2, "MIDI Low",
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
						me.lazyValueAction_(note,nil,true)} // enter value via keyboard
				}),
				{|me,val,latency,send|
					var value=val.clip(0,p[3]);
					this.setPVP(2,value,latency,send);
					midiBuffer1.releaseBelow(p[2]);
					if (value!=val) {{me.value_(value)}.defer(0.05)};
				}],
			
			// 3.MIDI high
			[127, \MIDInote, midiControl, 3, "MIDI High",
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
						me.lazyValueAction_(note,nil,true);} // enter value via keyboard
				}),
				{|me,val,latency,send|
					var value=val.clip(p[2],127);
					this.setPVP(3,value,latency,send);
					midiBuffer1.releaseAbove(p[3]);
					if (value!=val) {{me.value_(value)}.defer(0.05)};
				}],
				
			// 4.chord ON/OFF
			[0, \switch, (\strings_:["Off","On"]), midiControl, 4, "Chords On/Off",
				{|me,val,latency,send,toggle| this.setPVH(4,val,latency,send,toggle) }],
			
			// 5. chord
			[0,[0,LNX_Music.names[1].size-1,\linear,1],midiControl, 5, "Chord",
				(\items_:LNX_Music.names[1]),
				{|me,val,latency,send,toggle|
					this.setPVPModel(5,val,latency,send);
					musicMod.updateChords(latency);
				}],
		
			// 6. Arpeggiator ON/OFF
			[0, \switch, (\strings_:["Off","On"]), midiControl, 6, "Arpeggiator On/Off",
				{|me,val,latency,send,toggle| this.setPVPModel(6,val,latency,send) }],
			
			// 7. style
			[0,[0,9,\linear,1],midiControl, 7, "Style",
				(\items_:["Up", "Down", "Up & Down", "Down & Up",
				"Converge", "Diverge", "Con & Div", "UpUpDown", "DownDownUp", "Random"]),
				{|me,val,latency,send,toggle| this.setPVPModel(7,val,latency,send) }],
			
			// 8. Arp Fixed/Ratio
			[1,\unipolar,midiControl, 8, "Arp Fixed/Ratio",
				(\strings_:["Fixed","Ratio"]),
				{|me,val,latency,send,toggle| this.setPVPModel(8,val,latency,send) }],
				
			// 9. duration
			[50,[1,99,\linear,1,50,"%"],midiControl, 9, "duration",
				(\label_:"Dur",\numberFunc_:'int'),
				{|me,val,latency,send,toggle| this.setPVPModel(9,val,latency,send) }],
				
			// 10. Record ON/OFF
			[0, \switch, (\strings_:"Rec"), midiControl, 10, "Record On/Off",
				{|me,val,latency,send,toggle| this.setPVH(10,val,latency,send,toggle) }],

			// 11. Play ON/OFF
			[0, \switch, (\strings_:["Off","On"]), midiControl, 11, "Play On/Off",
				{|me,val,latency,send,toggle| this.setPVH(11,val,latency,send,toggle) }],
			
			// 12. transpose music
			[0,[-12,12,\linear,1],midiControl, 12, "Transpose",
				(\label_:"Trans",\numberFunc_:'intSign','zeroValue_':0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(12,val,latency,send);
					musicMod.updateChords(latency);
				}],
				
			// 13. quant chord select
			[0,[0,0,\linear,1],midiControl, 13, "Quant chord",
				(\label_:"Key",\numberFunc_:'int','zeroValue_':0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(13,val,latency,send);
					// test for onOff here
					if (p[14].isTrue) { 
						midiBuffer3.releaseAll;
						chordQuantiserMod.guiOnOff;
					};
				}],
				
			// 14. Quant ON/OFF
			[0, \switch, (\strings_:["Off","On"]), midiControl, 14, "Quant On/Off",
				{|me,val,latency,send,toggle|
					this.setPVPModel(14,val,latency,send);
					midiBuffer3.releaseAll;	
				}],
				
			// 15. IN ON/OFF
			[1, \switch, (\strings_:"MIDI"), midiControl, 15, "IN On/Off",
				{|me,val,latency,send,toggle|
					this.setPVPModel(15,val,latency,send);
					midiBuffer1.releaseSource(\MIDIIn);
				}],
				
			// 16. quant retrigger
			[1, \switch, (\strings_:"Trig"), midiControl, 16, "Retrigger",
				{|me,val,latency,send,toggle| this.setPVPModel(16,val,latency,send) }],
				
			// 17. arpeg up
			[0,[0,12,\linear,1],midiControl, 17, "Arpeg Up",
				(\label_:"Up",\numberFunc_:'intSign','zeroValue_':0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(17,val,latency,send);
					arpeggiatorMod.sortNotes;
				}],
				
			// 18. arpeg down
			[0,[-12,0,\linear,1],midiControl, 18, "Arpeg Up",
				(\label_:"Down",\numberFunc_:'intSign','zeroValue_':0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(18,val,latency,send);
					arpeggiatorMod.sortNotes;
				}],
				
			// 19. arpeg repeat
			[1,[1,8,\linear,1],midiControl, 19, "Repeat Arpeg",
				(\label_:"Repeat",\numberFunc_:'intSign','zeroValue_':1),
				{|me,val,latency,send,toggle| this.setPVPModel(19,val,latency,send) }],

			// 20. master velocity
			[1,[1/8,8,\exp],midiControl, 20, "Velocity",
				(\label_:"Vel",\numberFunc_:'float2','zeroValue_':1),
				{|me,val,latency,send,toggle| this.setPVPModel(20,val,latency,send) }],		
			// 21. quant transpose
			[0,[-12,12,\linear,1],midiControl, 21, "Q Trans",
				(\label_:"Trans",\numberFunc_:'intSign','zeroValue_':0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(21,val,latency,send);
					// test for onOff here
					if (p[14].isTrue) { 
						midiBuffer3.releaseAll;
						chordQuantiserMod.guiOnOff;
					};
				}],
				
			// 22. Legato ON/OFF
			[0, \switch, (\strings_:"Legato"), midiControl, 22, "Legato",
				{|me,val,latency,send,toggle| this.setPVPModel(22,val,latency,send) }],
				
			// 23. Always ON
			[0, \switch, (\strings_:"Always"), midiControl, 23, "Always On",
				{|me,val,latency,send,toggle|
					this.setPVPModel(23,val,latency,send);
					if ((val==0)&&(instOnSolo.isOff)) {this.stopAllNotes}; 
					{studio.updateAlwaysOn(id,val.isTrue)}.defer;
				}],
			
			// 24. Net On/Off
			[0, \switch, (\strings_:"Net"), midiControl, 24, "Net",
				{|me,val,latency,send,toggle|
					this.setPVPModel(24,val,latency,send);
					midiBuffer5.releaseAll;
				}],
				
			// 25. arpeg poly
			[1,[1,4,\linear,1],midiControl, 25, "Poly Arpeg",
				(\label_:"Poly",\numberFunc_:'int'),
				{|me,val,latency,send,toggle| this.setPVPModel(25,val,latency,send) }],
				
			// 26. storeChords root
			[24,\MIDInote,midiControl, 26, "Store Root",
				(\numberFunc_:'MIDInote'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(26,val,latency,send);
					{storeChordsMod.updateKeyboard}.defer;
				}],
				
			// 27. steps per octave (spo) 
			[12,[1,24,\linear,1],midiControl, 27, "Steps/Octave",
				( \label_:"Steps/Oct", \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(27,val,latency,send);
				}],
				
			// 28. chord type
			[1,[0,LNX_Music.myGroupNames.size,\linear,1],midiControl, 28, "Chord Type",
				(\items_:LNX_Music.myGroupNames),
				{|me,val,latency,send,toggle|
					//[me,val,latency,send,toggle].postln;
					this.setPVPModel(28,val,latency,send);
					musicMod.setActiveGroup_(val.asInt);
					{
						models[5]
							.themeMethods_( (\items_: musicMod.activeChordNames) )
							.controlSpec_([0,(musicMod.size-1),\linear,1])
							.updateDependants;
					}.defer;
				}],
				
			// 29. Rotate Output
			[0, \switch, (\strings_:"R"), midiControl, 23, "Rotate Out",
				{|me,val,latency,send,toggle| this.setPVPModel(29,val,latency,send) }],
				
			// 30. Allow midi program change in
			[0, \switch, (\strings_:"Prg"), midiControl, 30, "MIDI Program Change",
				{|me,val,latency,send,toggle| this.setPVPModel(30,val,latency,send) }],
				
			// 31. syncDelay
			[[-1,1,\lin,0.001,0], {|me,val,latency,send|
				this.setPVP(31,val,latency,send);
				this.syncDelay_(val);
			}],	
			
		].generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,30,31];
		randomExclusion=[0,1,30,31];	
		autoExclusion=[31];
		
		models[5].constrain_(false); // stop controlSpec constraint for chord selected
		
		models[27].addDependant{
			storeChordsMod.updateChordsAndNames; // this will update ChordQuant as well
			sequencer.refresh(false,false);
			[midiBuffer1, midiBuffer2, midiBuffer3, midiBuffer4,
			 midiBuffer5, midiBuffer6].do(_.releaseAll);
			// arpeggiatorMod.sortNotes; // do i need this?
			//chordQuantiserMod.retrigger;
		};
				
	}
		
	// Presets /////////////////////////////////////////////////////////////////////////////////
	
	// get the current state as a list
	iGetPresetList{
		^(sequencer.iGetPresetList) ++ 
		(storeChordsMod.iGetPresetList) ++
		(multiPipeOut.iGetPresetList) ++
		[arpegSequencer.iGetPrestSize] ++ (arpegSequencer.iGetPresetList);
	}

	// add a state list to the presets
	iAddPresetList{|l|
		var presetSize;
		sequencer.iAddPresetList(l.popEND("*** END Score DOC ***").reverse);
		storeChordsMod.iAddPresetList(l.popEND("*** END LNX_StoreChords DOC ***"));
		multiPipeOut.iAddPresetList(l.popEND("*** END LNX_MultiPipeOut DOC ***"));
		presetSize=l.popI;
		arpegSequencer.iAddPresetList(l.popNF(presetSize));
	}
	
	// save a state list over a current preset
	iSavePresetList{|i,l|
		var presetSize;
		sequencer.iSavePresetList(i,l.popEND("*** END Score DOC ***").reverse);
		storeChordsMod.iSavePresetList(i,l.popEND("*** END LNX_StoreChords DOC ***"));
		multiPipeOut.iSavePresetList(i,l.popEND("*** END LNX_MultiPipeOut DOC ***"));
		presetSize=l.popI;
		arpegSequencer.iSavePresetList(i,l.popNF(presetSize));
	}
	
	// ** OVERRIDE INSTRUMENT TEMPLATE
	// load up that preset (override template so iLoadPreset goes 1st for storeChord model spec
	// this fixes bug for MelodyMaker
	loadPreset{|i,latency|
		var presetToLoad, oldP;
		this.iLoadPreset(i);    // any instrument specific details (1st 4 melody)
		presetToLoad=presetMemory[i].copy;
		presetExclusion.do{|i| presetToLoad[i]=p[i]}; // exclude these parameters
		// updt models
		presetToLoad.do({|v,j| if (p[j]!=v) { models[j].lazyValueAction_(v,latency) }});
		oldP=p.copy;
		p=presetToLoad;         // copy the paramaters to p (is this needed any more?)
		this.updateDSP(oldP);  // and update any dsp (none in Melody Maker)
		//this.setAsPeakLevel; // part of myHack
	}
	
	// for your own load preset
	iLoadPreset{|i,newP,latency|
		sequencer.iLoadPreset(i);
		storeChordsMod.iLoadPreset(i);
		multiPipeOut.iLoadPreset(i);
		arpegSequencer.iLoadPreset(i);
	}
	
	// for your own remove preset
	iRemovePreset{|i|
		sequencer.iRemovePreset(i);
		storeChordsMod.iRemovePreset(i);
		multiPipeOut.iRemovePreset(i);
		arpegSequencer.iRemovePreset(i);
	}
	
	// for your own removal of all presets
	iRemoveAllPresets{
		sequencer.iRemoveAllPresets;
		storeChordsMod.iRemoveAllPresets;
		multiPipeOut.iRemoveAllPresets;
		arpegSequencer.iRemoveAllPresets;
	}
	
	// random preset
	iRandomisePreset{}	
		
	// clear the sequencer
	clearSequencer{
		sequencer.clear;
		arpegSequencer.clearSequencer;
	}
		
	// disk i/o /////////////////////////////////////////////////////////////////////////////////
	
	// for saving to disk
	iGetSaveList{ 
		^(sequencer.getSaveList) ++
		(storeChordsMod.getSaveList) ++
		(multiPipeOut.getSaveList) ++
		(arpegSequencer.getSaveList);
	}
	
	// for loading from disk
	iPutLoadList{|l,noPre,loadVersion|
		sequencer.putLoadList( l.popEND("*** END PRSeq DOC ***") );
		if (loadVersion>=1.1) {
			storeChordsMod.putLoadList( l.popEND("*** END LNX_StoreChords DOC ***") );
		};
		if (loadVersion>=1.2) {
			multiPipeOut.putLoadList( l.popEND("*** END LNX_MultiPipeOut DOC ***") );
		};
		if (l.last.asString=="LNX StepSequencer Doc v1.1"){
			arpegSequencer.putLoadList(l.popEND("*** END OBJECT DOC ***"))
		};
	 }

	// anything that needs doing after the entire song has been loaded and id's assigned to insts
	iPostSongLoad{|offset| multiPipeOut.iPostSongLoad(offset) }

	// any post midiInit stuff
	iInitMIDI{
		// override midi program func so it is dependant on the models[30]
		midi.programFunc = {|src, chan, prog ,latency|
			if (models[30].isTrue) {
				this.midiSelectProgram(prog ,latency)
			}
		};
		midi.putLoadList([0,0,0,0])
	}
	
	// GUI /////////////////////////////////////////////////////////////////////////////////////
	
	*thisWidth  {^920}
	*thisHeight {^520}
	
	createWindow{|bounds| this.createTemplateWindow(bounds,Color(0,1/103,9/77,65/77),
		resizable:false) }
	
	// window created after 0.01 secs (see deferOpenWindow)
	iPostNew{ {sequencer.resizeToWindow}.defer(0.02) }

	// create all the GUI widgets while attaching them to models
	createWidgets{
		
		var background1 = Color(0.50602409638554, 0.44615384615385, 0.63636363636364);
		var border      = Color(0, 0.0097087378640777, 0.11688311688312, 0.84415584415584);
				
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));

		gui[\onOffTheme1]=( \font_		: Font("Helvetica-Bold", 12),
						 \colors_     : (\on : Color(0.25,1,0.25),
						 				\off : Color(0.4,0.4,0.4)),
						 \rounded_		: true
						 );
						 
		gui[\onOffTheme2]=( \font_		: Font("Helvetica-Bold", 12),
						 \colors_     : (\on : Color(0.25,0.75,1),
						 				\off : Color(0.4,0.4,0.4)),
						 \rounded_		: true
						 );
						 
		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background : Color(1,1,0.8)),
						\canFocus_	: false
						);
						
		gui[\theme2]=(	\layout_       : \reversed,
						\resoultion_	 : 1,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",12),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.43,0.40,0.38),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.white,
										\focus : Color(0,0,0,0)));
										
		gui[\knobTheme1]=(\colors_		: (\on : Color(0.25,0.75,1),
									   \label: Color.black, 
									   \numberUp:Color.black,
									   \numberDown:Color.white),
						\labelShadow_	: false,
						\labelFont_   : Font("Helvetica",12),
						\numberFont_  : Font("Helvetica",11));
						
		gui[\textTheme] = (\font_		: Font("Helvetica-Bold", 14),
						\align_		: 'center',
						\colors_		: ( \string: Color.black),
						\noShadows_	: 0 );
						
		gui[\textThemeL] = (\font_		: Font("Helvetica-Bold", 14),
						\align_		: 'left',
						\colors_		: ( \string: Color.black),
						\noShadows_	: 0 );

		// composite views ////////////////////////////////////////////////////////////////

		gui[\scrollView] = MVC_RoundedCompositeView(window,
				Rect(11,11,this.thisWidth-22,this.thisHeight-23))
			.color_(\background, Color(59/77,59/77,63/77) )
			.color_(\border, background1 )
			.width_(6);

		// in
		
		MVC_StaticText(gui[\scrollView],Rect(10, 28, 100, 22),gui[\textTheme])
			.string_("Input");
		
		gui[\rcv1] = MVC_RoundedCompositeView(gui[\scrollView],Rect(10, 55, 100, 27))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);

		MVC_PlainSquare(gui[\scrollView],Rect(92, 88, 6, 26))
			.color_(\off,border);

		// music
		
		MVC_StaticText(gui[\scrollView],Rect(7, 94, 80, 22),gui[\textThemeL])
			.string_("Chords");
		
		gui[\rcv2] = MVC_RoundedCompositeView(gui[\scrollView],Rect(10, 120, 100, 108))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
			
		MVC_PlainSquare(gui[\scrollView],Rect(92, 234, 6, 22))
			.color_(\off,border);
		
		// store bank	
		
		MVC_StaticText(gui[\scrollView],Rect(7, 237, 80, 22),gui[\textThemeL])
			.string_("Bank");
		
		gui[\rcv3] = MVC_RoundedCompositeView(gui[\scrollView],Rect(10, 262, 100, 221))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
			
		MVC_PlainSquare(gui[\scrollView],Rect(116, 427, 5, 6))
			.color_(\off,border);
			
		// arpeg	
		
		MVC_StaticText(gui[\scrollView],Rect(660, 25+3, 100, 22),gui[\textTheme])
			.string_("Arpeggiator");
		
		gui[\rcv4] = MVC_RoundedCompositeView(gui[\scrollView],Rect(660, 55, 100, 350))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
		
		MVC_PlainSquare(gui[\scrollView],Rect(766, 282, 11, 6))
			.color_(\off,border);

		// quant
		
		MVC_StaticText(gui[\scrollView],Rect(783, 239+3, 100, 22),gui[\textTheme])
			.string_("Quantise");
		
		gui[\rcv5] = MVC_RoundedCompositeView(gui[\scrollView],Rect(783, 270, 100, 215))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
			
		MVC_PlainSquare(gui[\scrollView],Rect(649, 479, 128, 6))
			.color_(\off,border);
		
		// out
		
		MVC_StaticText(gui[\scrollView],Rect(783, 28, 100, 22),gui[\textTheme])
			.string_("Output");
		
		gui[\rcv6] = MVC_RoundedCompositeView(gui[\scrollView],Rect(783, 55, 100, 177))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
			
		MVC_PlainSquare(gui[\scrollView],Rect(766, 64, 11, 6))
			.color_(\off,border);
			
		// piano roll

		gui[\rcv7] = MVC_RoundedCompositeView(gui[\scrollView],Rect(127,13,516,470))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);

		// the piano roll ///////////////////////////////////////////////////////////////////
						
		sequencer.createWidgets(gui[\rcv7],Rect(2, 9, 512, 340),
						(\selectRect: Color.white,
						 \background: Color(42/83,42/83,6.5/11)*0.9,
						 \velocityBG: Color(3/77,1/103,0,65/77),
						 \buttons:    Color(6/11,42/83,29/65)*1.2,
						 \boxes:		Color(0.1,0.05,0,0.5),
						 \noteBG:     Color(0.5,0.75,1),
						 \noteBGS:    Color(0.75,0.875,1),
						 \noteBS:     Color(1,1,1),
						 \velocity:   Color(0.45,0.7,1),
						 \velocitySel: Color.white
						), 25,
						parentViews:[window]
						
						);

		// MIDI In Lamp
		gui[\pianoRollLamp] = MVC_PipeLampView(gui[\rcv7],Rect(236,7,10,10));

		// the keyboard, fix bug so we don't need this scrollView
		gui[\keyboardOuterView]=MVC_ScrollView(gui[\rcv7],Rect(0, 383, 517, 87))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);
		
		toFrontAction={ gui[\keyboard].focus};	
		
		gui[\keyboard]=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0, 0, 517, 87),5,24)
			.useSelect_(true)
			.stopGUIUpdate_(true)
			.keyboardColor_(Color(0.25,0.25,1));
			
		this.initKeyboard; // moved to help design process (i should move this out of here)
		
		// the arpget sequencer
		arpegSequencer.createButtonWidgets(gui[\rcv7], Rect(-58, 308, 652, 250),
			controls:false, showName:false );
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],(710+30)@(7),85-30,
				background1,
				Color.black,
				background1,
				background1, // background
				Color.black
			);
		this.attachActionsToPresetGUI;
		
		// 27. steps per octave (spo)
		MVC_NumberBox(models[27],gui[\scrollView], Rect(717, 436, 43, 17))
			.labelShadow_(false)
			.orientation_(\horizontal)
			.color_(\label,Color.black);
			
		// 31. sync
		MVC_NumberBox(models[31],gui[\scrollView], Rect(717, 457, 43, 17))
			.labelShadow_(false)
			.label_("Sync")
			.orientation_(\horizontal)
			.color_(\label,Color.black);
			
		// melody maker text
		MVC_Text(gui[\scrollView],Rect(654, 411, 118, 23))
				.align_(\center)
				.shadow_(false)
				.penShadow_(true)
				.font_(Font("AvenirNext-Heavy",15))
				.string_("Melody Maker");
		
		// 1.on/off
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect(4, 7,22,18),gui[\onOffTheme1])
			.rounded_(true)
			.permanentStrings_(["On"]);
			
		// 0.solo
		MVC_OnOffView(models[0],gui[\scrollView] ,Rect(30, 7,20,18),gui[\soloTheme  ])
			.rounded_(true);
			
		// MIDI Settings
 		MVC_FlatButton(gui[\scrollView],Rect(72, 7, 43, 19),"MIDI")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(6/11,29/65,42/83))
			.color_(\down,Color(6/11,29/65,42/83)/2)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ this.createMIDIInOutModelWindow(window,2,3,
				colors:(border1:Color(0,1/103,9/77,65/77), border2:background1)
			)};
			
		// MIDI Controls
 		MVC_FlatButton(gui[\scrollView],Rect(655, 7, 43, 19),"Cntl")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(6/11,29/65,42/83))
			.color_(\down,Color(6/11,29/65,42/83)/2)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{
				LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front 	
			};
			
		// 30. Allow midi program change in
		MVC_OnOffView(models[30],gui[\scrollView] ,Rect(702, 7,33,19),gui[\onOffTheme1])
			.rounded_(true);
			
		// LEFT //
		
		// IN //////////////////////////////////////////////////////////////////
		
		// 15. IN ON/OFF	
		MVC_OnOffView(models[15],gui[\rcv1] ,Rect(2,4,36,18),gui[\onOffTheme2]);
			
		// 24. Net
		MVC_OnOffView(models[24],gui[\rcv1] ,Rect(60,4,36,18),gui[\onOffTheme2]);

		// MIDI In Lamp
		gui[\midiInLamp] = MVC_PipeLampView(gui[\rcv1],Rect(44,8,10,10));
	
		// MUSIC ///////////////////////////////////////////////////////////////

		// 4. musicChords on/off
		MVC_OnOffView(models[4], gui[\rcv2], Rect(2,4,31,18),gui[\onOffTheme1]);
			
		// 5. musicChords
		MVC_PopUpMenu3(models[5], gui[\rcv2], Rect(3,28,95,18),gui[\menuTheme ]);
		
		// 5. musicChords		
		MVC_MyKnob3(models[5], gui[\rcv2] , Rect(11,66,28,28),gui[\knobTheme1])
			.showNumberBox_(false)
			.label_("Chords");
			
		// 12. transpose		
		MVC_MyKnob3(models[12], gui[\rcv2] , Rect(58,66,28,28),gui[\knobTheme1]);
		
		// 28. max notes in chords
		MVC_PopUpMenu3(models[28], gui[\rcv2], Rect(39,4,30,18),gui[\menuTheme ]);
		
		// storeChords Lamp
		gui[\musicLamp] = MVC_PipeLampView(gui[\rcv2],Rect(80,7,10,10));
				
		// STORE ///////////////////////////////////////////////////////////////
		
		// 11. play on/off
		MVC_OnOffView(models[11],gui[\rcv3] ,Rect(2,4,31,18),gui[\onOffTheme1]);
		
		// 10. Record on/off
		MVC_OnOffView(models[10],gui[\rcv3] ,Rect(2,29,31,18),gui[\onOffTheme1])
			.color_(\on,Color(1,0.25,0.25));
				
		// Add button
 		MVC_FlatButton(gui[\rcv3],Rect(39,29, 31, 18),"Add")
			.rounded_(true)
			.canFocus_(false)
			.color_(\up,Color(6/11,42/83,29/65))
			.color_(\down,Color(6/11,42/83,29/65)/2)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ storeChordsMod.guiAddChord };
	
		// 26. storeChords root	
		MVC_NoteView(models[26], gui[\rcv3], Rect(39,4,31,18))
			.canFocus_(false);	

		// chord copy paste menu
		gui[\cpMenu]=MVC_PopUpMenu3(gui[\rcv3],Rect(79,31,16,16),gui[\menuTheme ])
			.staticText_("")
			.showTick_(false)
			.items_(["Copy Chords","Paste Chords","-","Delete Chord","Delete All Chords"])
			.color_(\background,Color.ndcMenuBG)
			.action_{|me|
				if (me.value==0) { storeChordsMod.guiCopyChords };
				if (me.value==1) { storeChordsMod.guiPasteChords };
				if (me.value==3) {
					storeChordsMod.guiDeleteChord(gui[\storeChordsListView].value)
				};
				if (me.value==4) { storeChordsMod.guiAllDeleteChords };
			}
			.value_(0)
			.font_(Font("Helvetica", 12));

		// storeChords List View
		gui[\storeChordsListView] = MVC_ListView2 (gui[\rcv3], Rect(0, 55, 100, 166))
			.canFocus_(false)
			.items_([])
			.font_(Font("Helvetica",10));

		// storeChords Lamp
		gui[\storeChordsLamp] = MVC_PipeLampView(gui[\rcv3],Rect(80,7,10,10));

		// RIGHT //

		// Arpeggiator ///////////////////////////////////////////////////////////////

		// 6. Arpeggiator on/off
		MVC_OnOffView(models[6],gui[\rcv4] ,Rect(2,4,31,18), gui[\onOffTheme1]);
		
		// 22. Legato ON/OFF
		MVC_OnOffView(models[22],gui[\rcv4] ,Rect(36, 4, 45, 18), gui[\onOffTheme2]);

		// 7. style
		MVC_PopUpMenu3(models[7],gui[\rcv4], Rect(2,28,93,18), gui[\menuTheme ]);
		
		// 18. arpeg down
		MVC_MyKnob3(models[18], gui[\rcv4], Rect(11,66,28,28), gui[\knobTheme1]);
		
		// 17. arpeg up	
		MVC_MyKnob3(models[17], gui[\rcv4], Rect(59,66,28,28), gui[\knobTheme1]);
				
		// 19. arpeg repeat
		MVC_MyKnob3(models[19], gui[\rcv4], Rect(11,123,28,28),gui[\knobTheme1]);
		
		// 25. arpeg poly
		MVC_MyKnob3(models[25], gui[\rcv4], Rect(59,123,28,28),gui[\knobTheme1]);
		
		// 20. master velocity
		MVC_MyKnob3(models[20], gui[\rcv4], Rect(11,180,28,28),gui[\knobTheme1]);

		// 9. duration
		MVC_MyKnob3(models[9], gui[\rcv4], Rect(59,180,28,28),gui[\knobTheme1]);

		// 8. Arp Fixed/Ratio
		MVC_OnOffView(models[8],gui[\rcv4] ,Rect(50, 243, 43, 18),gui[\onOffTheme2]);
		
		// rulerModel
		MVC_MyKnob3(arpegSequencer.rulerModel, gui[\rcv4], Rect(11,241,28,28),gui[\knobTheme1])
			.label_("Ruler");
		
		// stepsModel		
		MVC_MyKnob3(arpegSequencer.stepsModel, gui[\rcv4], Rect(11,300,28,28),gui[\knobTheme1])
			.label_("Steps");
		
		// speedModel
		MVC_MyKnob3(arpegSequencer.speedModel, gui[\rcv4], Rect(59,300,28,28),gui[\knobTheme1])
			.label_("Speed");
			
		// storeChords Lamp
		gui[\arpegLamp] = MVC_PipeLampView(gui[\rcv4],Rect(85,7,10,10));
			
		// Quantise Chord /////////////////////////////////////////////////////////////
		
		// 14. Quant on/off
		MVC_OnOffView(models[14],gui[\rcv5], Rect(2,4,31,18),gui[\onOffTheme1]);
		
		// 16. Quant retrigger
		MVC_OnOffView(models[16],gui[\rcv5] ,Rect(38,4,40,18),gui[\onOffTheme2]);

		// 13. quant chord select		
		MVC_MyKnob3(models[13], gui[\rcv5], Rect(11,40,28,28),gui[\knobTheme1]);
		
		// 21. quant transpose
		MVC_MyKnob3(models[21], gui[\rcv5], Rect(59,40,28,28),gui[\knobTheme1]);
		
		gui[\quantChordsListView] = MVC_ListView2(models[13],gui[\rcv5],Rect(0, 82, 100, 134))
			.canFocus_(false)
			.items_([])
			.showNumberBox_(false)
			.font_(Font("Helvetica",10))
			.label_(nil);
			
		// Quantise Lamp
		gui[\quantiseLamp] = MVC_PipeLampView(gui[\rcv5],Rect(84,7,10,10));

		// Out ///////////////////////////////////////////////////////////////
	
		// 23. Always ON
		MVC_OnOffView(models[23], gui[\rcv6], Rect(4, 4, 52, 18),gui[\onOffTheme1]);
		
		// MIDI Out Lamp
		gui[\midiOutLamp] = MVC_PipeLampView(gui[\rcv6],Rect(61,7,10,10));
		
		// 29. Rotate Output
		MVC_OnOffView(models[29],gui[\rcv6] ,Rect(76,4,20,18),gui[\onOffTheme2]);
		
		multiPipeOut.makeGUI(gui[\rcv6], Rect(0, 27, 100, 150));
		
	}

} // end ////////////////////////////////////
