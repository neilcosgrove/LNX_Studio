
+ LNX_BumNote {

	//// GUI ///////////////////////////////////////////
	
	*thisWidth  {^711}
	*thisHeight {^715+8}
	
	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(0.1,0.1,0.1,1)); 
	}

	// size the window and position of the views inside
	arrangeWindow{
		var osv, kov;
		osv=gui[\outerSeqView].bounds;
		kov=gui[\keyboardOuterView].bounds;
		if (p[34]==1) {					
			if (p[35]==1) {
				window.setInnerExtent(thisWidth,thisHeight);
			}{
				window.setInnerExtent(thisWidth,thisHeight-100);
			};
			// it takes a moment for the bounds to update
			{
				gui[\outerSeqView].bounds_(Rect(osv.left,osv.top,690,417+12));
				gui[\keyboardOuterView].bounds_(Rect(kov.left,607+16, kov.width,kov.height));
			}.defer(0.05); // needed else chops off bottom of outerSeqView

		}{
			gui[\outerSeqView].bounds_(Rect(osv.left,osv.top,0,0));
			gui[\keyboardOuterView].bounds_(Rect(kov.left,190, kov.width,kov.height));
			if (p[35]==1) {
				window.setInnerExtent(thisWidth,thisHeight-417-8);
			}{
				window.setInnerExtent(thisWidth,thisHeight-417-110-8);
			};
		};
		bounds=window.bounds;
	}

	// create all the GUI widgets while attaching them to models
	createWidgets{
	
		var r1,r2,r3; //MVC_View.showLabelBackground_(true);
		
		// Themes
									
		gui[\knob1Theme]=(	\labelFont_   : Font("Helvetica",12),
						\numberWidth_ : -22,
						\colors_      : (\on : Color.orange, \pin : Color.orange,
									   \numberDown : Color.orange));
		
		gui[\knob2Theme]=(	\labelFont_   : Font("Helvetica",12),
						\numberWidth_ : -22,
						\colors_      : (\on : Color(1,0.15,0), \pin : Color(1,0.15,0),
									   \numberDown : Color(1,0.15,0)));

		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background : Color(1,1,0.8)));
						
		gui[\menuTheme2]=( \font_		: Font("Arial", 10),
						\colors_      : (\background:Color(0.1,0.1,0.1),\string:Color.orange,
									   \focus:Color.clear));
		
		gui[\boxTheme  ]=(	\orientation_ : \horizontal,
						\resoultion_	: 2,
						\showNumberBox_: false,
						\colors_      : (\label : Color.orange, \background : Color(1,1,0.9)));
		
		gui[\boxTheme2 ]=(	\orientation_ : \horizontal,
						\resoultion_	: 2,
						\font_		: Font("Helvetica",10),
						\labelFont_	: Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_      : (\label : Color.white, \background : Color(0.2,0.2,0.2),
										\string : Color.orange, \focus : Color(0,0,0,0)));
						
		gui[\labelTheme]=( \font_		: Font("Chalkboard-Bold", 16),
						\align_		: \center,
						\colors_		: (\string : Color.orange));
						
		gui[\labelTheme2]=( \font_		: Font("Chalkboard-Bold", 14),
						\align_		: \center,
						\colors_		: (\string : Color(1,0.75,0)));
						
		gui[\sliderTheme]=(\thumbSize_	: 18,
						 \colors_		: (\background : Color(0,0,0,0.3), \knob : Color.orange));
						
		gui[\onOffTheme]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color.orange, \off : Color(0.4,0.4,0.4)));
		
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));
		
		gui[\onOffTheme2]=( \font_		: Font("Helvetica-Bold", 12),
						 \colors_     : (\on : Color(0.25,1,0.25), \off : Color(0.4,0.4,0.4)));
						
		gui[\onOffTheme3]=( \font_		: Font("Helvetica-Bold", 10),
						 \colors_     : (\on : Color.red, \off : Color.red/3));
						 
		gui[\onOffTheme4]=( \font_		: Font("Helvetica-Bold", 11),
						 \colors_     : (\on : (Color.orange+Color.red)/2, 
						 			   \off : (Color.orange+Color.red)/4));
		
		gui[\onOffTheme5]=( \font_		: Font("Helvetica-Bold", 11),
						 \colors_     : (\on : Color.orange, 
						 			   \off : Color.orange/2));
						 			   
		gui[\multiTheme ]=(\font_:Font("Helvetica",11),
			\states_ : [
				["Low"   ,Color.red/1.5   ,Color.black,Color.grey/3,Color.grey/2],
				["Med"   ,(Color.orange+Color.red)/2,Color.black,Color.grey/2,Color.grey/3],
				["Hi"    ,Color.orange,Color.black,Color.grey/4,Color.grey/2]]);
		
		// The scroll views
		
		gui[\masterView] = MVC_CompositeView(window, Rect(9,34,690,146))
			.color_(\background,Color(0.28693181818182, 0.16699522898306, 0));
		
		gui[\lfoView] = MVC_CompositeView(gui[\masterView], Rect(5,21,46,119))
			.color_(\background,Color(0.15,0.15,0.15));
			
		gui[\oscView] = MVC_CompositeView(gui[\masterView], Rect(92,21,115,119))
			.color_(\background,Color(0.15,0.15,0.15));
	
		gui[\filterView] = MVC_CompositeView(gui[\masterView], Rect(254,21,188,119))
			.color_(\background,Color(0.15,0.15,0.15));
			
		gui[\adsrView] = MVC_CompositeView(gui[\masterView], Rect(486,21,155,61))
			.color_(\background,Color(0.15,0.15,0.15));
				
		gui[\outerSeqView] = MVC_ScrollView(window, Rect(9,190,690,432))
			.color_(\background,Color(0.13,0.13,0.13));
			
		gui[\innerSeqView] = MVC_ScrollView(gui[\outerSeqView], Rect(65,5,581,418))
			.color_(\background,Color(0.08,0.08,0.08))
			.hasHorizontalScroller_(true);
			
		gui[\keyboardOuterView] = MVC_CompositeView(window,Rect(17,625,672,90));
			
		// view labels
		
		MVC_StaticText(gui[\masterView],Rect(444, 84, 101, 31)).string_("Bum Note")
			.color_(\string,Color(1,0.75,0)).font_(Font("Chalkboard-Bold", 22))
			.shadow_(false).penShadow_(true);
		MVC_StaticText(gui[\masterView],Rect(4,-8,46,28),gui[\labelTheme]).string_("LFO");
		MVC_StaticText(gui[\masterView],Rect(99,-8,98,28),gui[\labelTheme]).string_("Oscillators");
		MVC_StaticText(gui[\masterView],Rect(295,-8,44,28),gui[\labelTheme]).string_("Filter");
		MVC_StaticText(gui[\masterView],Rect(493,-8,75,28),gui[\labelTheme]).string_("Envelope");
		MVC_StaticText(gui[\outerSeqView],Rect(648,17,38,20),gui[\labelTheme2]).string_("Note");
		MVC_StaticText(gui[\outerSeqView],Rect(648,32,38,20),gui[\labelTheme2]).string_("ON");
		MVC_StaticText(gui[\outerSeqView],Rect(648,63,38,20),gui[\labelTheme2]).string_("Note");
		MVC_StaticText(gui[\outerSeqView],Rect(648,80,38,20),gui[\labelTheme2]).string_("OFF");
		MVC_StaticText(gui[\outerSeqView],Rect(648,177,38,20),gui[\labelTheme2]).string_("MIDI");
		MVC_StaticText(gui[\outerSeqView],Rect(648,195,38,20),gui[\labelTheme2]).string_("Note");
		MVC_StaticText(gui[\outerSeqView],Rect(648,261,38,20),gui[\labelTheme2]).string_("Slide");
		MVC_StaticText(gui[\outerSeqView],Rect(648,341,38,20),gui[\labelTheme2]).string_("Vel");
		
		this.arrangeWindow; // this causes a triple redraw when used, so before gui created
		// will this clip like before
		
		// widgets
		
		// row y-cord
		r1=18; r2=76; r3=65;
		
		// 0.solo
		MVC_OnOffView(models[0],window     ,Rect( 30,10,17,17),gui[\soloTheme  ]);
		// 1.on/off
		MVC_OnOffView(models[1],window     ,Rect( 10,10,17,17),gui[\onOffTheme2]);
		// 2.amp
		MVC_SmoothSlider(models[ 2],gui[\masterView],Rect(648, 4,34,78),gui[\sliderTheme]);
		// 3.filter q
		MVC_MyKnob(models[ 3],gui[\filterView],Rect( 46,r1,26,26),gui[\knob1Theme ]);
		// 4.filter env
		MVC_MyKnob(models[ 4],gui[\filterView],Rect( 82,r1,26,26),gui[\knob1Theme ]);
		// 5.pusle width
		MVC_MyKnob(models[ 5],gui[\oscView   ],Rect( 82,r1,26,26),gui[\knob1Theme ]);
		// 6.mod freq
		MVC_MyKnob(models[ 6],gui[\lfoView   ],Rect( 10,r1,26,26),gui[\knob1Theme ]);		// 7.mod amp
		MVC_MyKnob(models[ 7],gui[\lfoView   ],Rect( 10,r2,26,26),gui[\knob1Theme ]);
		// 8.osc 1-2 mix
		MVC_MyKnob(models[ 8],gui[\masterView],Rect(217,r3,26,26),gui[\knob1Theme ]);
		// 9.filter fq
		MVC_MyKnob(models[ 9],gui[\filterView],Rect( 10,r1,26,26),gui[\knob1Theme ]);
		// 10.filterLFO
		MVC_MyKnob(models[10],gui[\filterView],Rect(118,r1,26,26),gui[\knob1Theme ]);
		// 11.steps per octave
		MVC_PopUpMenu(models[11],window    ,Rect(320,10,48,17),gui[\menuTheme  ]);
		// 12.MIDI base
		MVC_NumberBox(models[12],window    ,Rect(465,10,26,17),gui[\boxTheme   ]);
		// 13.osc1 pitch
		MVC_MyKnob(models[13],gui[\oscView   ],Rect( 10,r1,26,26),gui[\knob1Theme ]);
		// 14.osc1 fine
		MVC_MyKnob(models[14],gui[\oscView   ],Rect( 46,r1,26,26),gui[\knob1Theme ]);
		// 15.osc2 pitch
		MVC_MyKnob(models[15],gui[\oscView   ],Rect( 10,r2,26,26),gui[\knob1Theme ]);
		// 16.osc2 fine
		MVC_MyKnob(models[16],gui[\oscView   ],Rect( 46,r2,26,26),gui[\knob1Theme ]);
		// 17.invert filter env
		MVC_OnOffView(models[17],gui[\filterView],Rect(83,59,23,15),gui[\onOffTheme]);
		// 18.filter KYBD
		MVC_MyKnob(models[18],gui[\filterView],Rect(154,r1,26,26),gui[\knob1Theme ]);
		// 19.slide osc pitch
		MVC_MyKnob(models[19],gui[\masterView],Rect( 58,r3,26,26),gui[\knob1Theme ]);
		// 20.Attack (Amp ADSR Env)
		MVC_MyKnob(models[20],gui[\adsrView  ],Rect( 10,r1,26,26),gui[\knob1Theme ]);
		// 21.Decay (Amp ADSR Env)	
		MVC_MyKnob(models[21],gui[\adsrView  ],Rect( 46,r1,26,26),gui[\knob1Theme ]);
		// 22.Sustain (Amp ADSR Env)
		MVC_MyKnob(models[22],gui[\adsrView  ],Rect( 82,r1,26,26),gui[\knob1Theme ]);
		// 23.Release (Amp ADSR Env)
		MVC_MyKnob(models[23],gui[\adsrView  ],Rect(118,r1,26,26),gui[\knob1Theme ]);
		// 24.Filter Attack (Env)
		MVC_MyKnob(models[24],gui[\filterView],Rect(62,r2,26,26),gui[\knob1Theme  ]);
		// 25.Filter Release (Env)
		MVC_MyKnob(models[25],gui[\filterView],Rect(102,r2,26,26),gui[\knob1Theme ]);
		// 26.Pan
		MVC_MyKnob(models[26],gui[\masterView],Rect(660,102,26,26),gui[\knob1Theme])
			.numberFont_(Font("Helvetica",10))
			.numberWidth_(-22);
		// 27.MIDI low
		MVC_NumberBox(models[27],window    ,Rect(402, 10,26,17),gui[\boxTheme  ]);
		// 28.MIDI high
		MVC_NumberBox(models[28],window    ,Rect(524, 10,26,17),gui[\boxTheme  ]);
		// 29.output channels
		MVC_PopUpMenu(models[29],window    ,Rect(629,10,70,18),gui[\menuTheme  ]);
		// 30.send channels
		MVC_PopUpMenu(models[30],gui[\masterView],Rect(552,96,70,16),gui[\menuTheme2]);
		// 31.send amp
		MVC_MyKnob(models[31],gui[\masterView],Rect(626,102,26,26),gui[\knob1Theme ])
			.numberFunc_(\float1)
			.numberWidth_(-15)
			.numberFont_(Font("Helvetica",10))
			.numberWidth_(0);
		// 32.Filer glide
		MVC_MyKnob(models[32],gui[\masterView],Rect(451, 40,26,26),gui[\knob1Theme ]);
		// 33.velocity > filter
		MVC_MyKnob(models[33],gui[\filterView],Rect(150, 82,20,20),gui[\knob2Theme ]);
		// 34.show sequencer
		MVC_OnOffView(models[34],window    ,Rect(557, 10,30,18),gui[\onOffTheme ]);
		// 35. show keyboard
		MVC_OnOffView(models[35],window    ,Rect(592, 10,30,18),gui[\onOffTheme ]);
		// 36. stepping filter
		MVC_MyKnob(models[36],gui[\filterView],Rect( 16, 82,20,20),gui[\knob2Theme ]);
		// 37.Filter Type (0=FF, 1=LADDER)
		MVC_PopUpMenu(models[37],gui[\masterView  ],Rect(344, 2,65,16),gui[\menuTheme2]);
		// 38.Lock Seq
		MVC_OnOffView(models[38],gui[\outerSeqView],Rect( 2,210,61,18),gui[\onOffTheme]);
		// 40.adv counter
		MVC_OnOffView(models[40],gui[\outerSeqView],Rect(20,357,31,18),gui[\onOffTheme2]);
		// 41.eq
		MVC_OnOffView(models[41],gui[\masterView  ],Rect(604, 4,20,14),gui[\onOffTheme4]);
		// 42.ladder pre-amp
		MVC_MultiOnOffView(models[42],gui[\masterView],Rect(576,4,25,14),gui[\multiTheme ]);
		// 43.link osc''s
		MVC_OnOffView(models[43],gui[\oscView     ],Rect( 81,99,28,14),gui[\onOffTheme5]);
		
		// other GUI items
		
		// midi in
		midi.createInMVUA (window, 57@10, false);
		midi.createInMVUB (window, 205@10, false);
		
		// midi control button
		MVC_RoundButton(window,Rect(265, 9, 43, 19))
			.states_([ [ "Cntrl", Color(1.0, 1.0, 1.0, 1.0), Color(0.3,0.15,0) ] ])
			.font_(Font("Helvetica",12))
			.canFocus_(false)
			.action_{ LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front };
			
		// the keyboard
		gui[\keyboardView]=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0,0,672,90),6,12)
			.keyDownAction_{|note|
				lastKeyboardNote=note;
				this.noteOn_Key(note,100);
			}
			.keyUpAction_{|note|
				this.noteOff_Key(note,100);
				if (isRecording.not) {this.setNoteDisplay(note,gui[\lastSeqIndex])};
			}
			.keyTrackAction_{|note|
				this.noteOn_Key(note,100);
				this.noteOff_Key(lastKeyboardNote,100);
				lastKeyboardNote=note;
			};
			
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\masterView],(447)@(122),77,
			background:Color(0.28693181818182, 0.16699522898306, 0),
			textBackground:Color(0.28693181818182, 0.16699522898306, 0)
		
		);
		this.attachActionsToPresetGUI;
			
		// note on lamp
		gui[\noteOnLamp]=MVC_LampView(gui[\oscView],Rect(89, 74, 15, 15));
			
		// write
		gui[\writeModel] = [isKeyboardWriting.binaryValue,\switch,midiControl,-2,"Write"].asModel			.action_{|me,val,latency,send|
				{
					isKeyboardWriting=val.isTrue;
					gui[\recordModel].value_(0);
					isRecording=false;
					if (val==0) {
						this.setNoteDisplay(-1,nil);
					};
				}.defer; // this will be on the System clock so defer
			};
		
		gui[\write]=MVC_OnOffView(gui[\writeModel],gui[\outerSeqView],Rect(15,405,41,18))
			.strings_("Write")
			.font_(Font("Helvetica-Bold",12))
			.color_(\on,Color.orange)
			.color_(\off,Color(0.4,0.4,0.4));
			
		// record
		gui[\recordModel] = [isRecording.binaryValue,\switch,midiControl,-1,"Record"].asModel;
		
		gui[\record]=MVC_OnOffView(gui[\recordModel],gui[\outerSeqView],Rect(20,381,31,18),"Rec")
			.action_{|me,val,latency,send|
				{
					isRecording=(val==0).if(false,true);
					gui[\writeModel].value_(0);
					isKeyboardWriting=false;
				}.defer;  // this will be on the System clock so defer
			}
			.font_(Font("Helvetica-Bold",12))
			.color_(\on,Color.red)
			.color_(\off,Color(0.4,0.4,0.4));
			
		// seq gui ////////////////////////////////	
		
		defaultSteps.do{|i|
			var c;
			
			// 0.i.note on
			c=(i%(sP[0][4])).map(0,sP[0][4],1,0.4);
			seqMVCViews[0][i]= MVC_SeqNoteOn(gui[\innerSeqView],seqModels[0][i],
										Rect(4+(i*18),19, 17, 17))
				.color_(\border,Color(c*0.5,c*0.5,c*0.5))
				.color_(\on,Color(c*0,c*1,c*0));
			
			// 1.i.note off
			c=(i%(sP[1][4])).map(0,sP[1][4],1,0.4);
			seqMVCViews[1][i]= MVC_SeqNoteOn(gui[\innerSeqView],seqModels[1][i],
										Rect(4+(i*18),69, 17, 17))
				.color_(\border,Color(c*0.5,c*0.5,c*0.5))
				.color_(\on,Color(c*1,c*0,c*0));
			
			// 2.i.midi note	
			c=(i%(sP[2][4])).map(0,sP[2][4],1,0.4);
			seqMVCViews[2][i]= MVC_NoteSeqView(gui[\innerSeqView],seqModels[2][i],
										Rect(4+(i*18),123, 17, 133))
				.color_(\border,Color(c*0.5,c*0.5,c*0.5))
				.color_(\slider,Color(c*1,c*0.5,c*0))
				.seqItems_(seqMVCViews[2]);
				
			// 3.i.velocity
			c=(i%(sP[3][4])).map(0,sP[3][4],1,0.4);
			seqMVCViews[3][i]= MVC_FlatSlider(gui[\innerSeqView],seqModels[3][i],
										Rect(4+(i*18),306, 17, 90))
				.rounded_(true)
				.color_(\border,Color(c*0.5,c*0.5,c*0.5))
				.color_(\slider,Color(c*1,c*0.5,c*0))
				.seqItems_(seqMVCViews[3]);
				
			// 4.i.slide
			c=(i%(sP[4][4])).map(0,sP[4][4],1,0.4);
			MVC_SeqNoteOn(gui[\innerSeqView],seqModels[4][i],Rect(4+(i*18),257, 17, 17))
				.color_(\border,Color(c*0.5,c*0.5,c*0.5))
				.color_(\on,Color(c*1,c*0.5,c*0));
		};
		
		
		// seq controls
		[0,50,104,286].do{|yos,i|
			// 3.steps
		   MVC_NumberBox(spModels[i][3],gui[\outerSeqView],Rect(37,21+yos,23,14),gui[\boxTheme2]);
			// 4.ruler	
	 	   MVC_NumberBox(spModels[i][4],gui[\outerSeqView],Rect(37, 6+yos,23,14),gui[\boxTheme2]);
	 	   MVC_RulerView(spModels[i][4],gui[\innerSeqView],Rect(4 , 2+yos,(defaultSteps*18),15))
	 	   	.label_(nil).steps_(defaultSteps);
			// 6.speed
		   MVC_NumberBox(spModels[i][6],gui[\outerSeqView],Rect(37,36+yos,23,14),gui[\boxTheme2]);
		};
		
		// the seq positions
		[0,50,237,358].do{|yos,i|
			MVC_PosView(posModels[i],gui[\innerSeqView],Rect(4,40+yos,(defaultSteps*18), 5))
				.color_(\on,Color.orange)
				.color_(\background,Color(0.2,0.2,0.2))
				.extend_(true)
				.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					var index = (x/18).asInt.clip(0,defaultSteps-1);
					gui[\lastSeqIndex]=index;
					posModels[2].dependantsPerform(\highlight_,index)	
				};
		};
					
		// the note display
		gui[\noteDisplay]=MVC_NoteView(gui[\outerSeqView], Rect(6, 180, 53, 23))
			.actions_(\keyAction_,{|me|
				var i,v;
				i=gui[\lastSeqIndex];
				v=me.value;
				if ((v.isNumber)&&(i.isNumber)) {
					this.setSeq(2,i,v);
					seqModels[2][i].value_(v);
				};
			})
			.action_({|me,val|
				var i,v,npoNote;
				findingNote=true;
				i=gui[\lastSeqIndex];
				v=me.value;
				if ((v.isNumber)&&(i.isNumber)) {
					this.setSeq(2,i,v);
					seqModels[2][i].value_(v);
					if (v>=0) {
						npoNote=v-p[12]/(p[11]+1)*12+p[12];
						findingNote=false;  // a little cheat to make this work
						this.acidNote(npoNote,0,nil);
						findingNote=true;
					};
				};
			})
			.font_(Font("Helvetica-Bold",14))
			.actions_(\upAction,{ findingNote=false })
			.color_(\background,Color(0.05,0.05,0.05))
			.color_(\string,Color.orange)
			.showNumberBox_(false)
			.numberFunc_(\note);	
			
	}
	
	setNoteDisplay{|note,index|
		gui[\noteDisplay].value_(note);
		gui[\lastSeqIndex]=index;
		posModels[2].dependantsPerform(\highlight_,index);
	}

	// change the color of the sequencer steps to match the ruler
	changeSeqColours{|y,value|
		defaultSteps.do({|x|	
				var c;
				c=((x)%value).map(0,value,1,0.4);
				if (y==0) {
					seqMVCViews[y][x]
						.color_(\border,Color(c*0.5,c*0.5,c*0.5))
						.color_(\on,Color(c*0,c*1,c*0))
				};
				if (y==1) {
					seqMVCViews[y][x]
						.color_(\border,Color(c*0.5,c*0.5,c*0.5))
						.color_(\on,Color(c*1,c*0,c*0))
				};
				if (y==2) {
					seqMVCViews[y][x]
						.color_(\border,Color(c*0.5,c*0.5,c*0.5))
						.color_(\slider,Color(c*1,c*0.5,c*0))
				};
				if (y==3) {
					seqMVCViews[y][x]
						.color_(\border,Color(c*0.5,c*0.5,c*0.5))
						.color_(\slider,Color(c*1,c*0.5,c*0))
				};
			});	
	}

} // end ////////////////////////////////////
