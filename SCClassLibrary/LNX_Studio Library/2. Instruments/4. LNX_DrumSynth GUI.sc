
+ LNX_DrumSynth {

	//// GUI ///////////////////////////////////////////

	*thisWidth  {^745-10}
	*thisHeight {^463-30}
	
	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(0.15,0.15,0.15,1));  // replaced this
	}
	
	createWidgets{
	
		var item;
	
		// colours
		
		gui[\drumColours]=[
			Color(0.4,0.4,0.2)/2,
			Color(0.3,0.4,0.2)/2,
			Color(0.2,0.4,0.2)/2,
			Color(0.2,0.4,0.3)/2,
			Color(0.2,0.4,0.4)/2];
			
		gui[\highlightColours]=[
			Color(0.75,1,0),
			Color(0.5,1,0),
			Color(0,1,0),
			Color(0,1,0.5),
			Color(0,1,0.75)];
			
		gui[\sliderColours]=[
			Color(0.75,1,0),
			Color(0.5,1,0),
			Color(0,1,0),
			Color(0,1,0.5),
			Color(0,1,0.75)];
	
	// Themes
		
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));
		
		gui[\onOffTheme1]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color(0.25,1,0.25), \off : Color(0.4,0.4,0.4)));
		
		gui[\onOffTheme2]=( \font_		: Font("Helvetica-Bold", 12),
						 \colors_     : (\on : Color(0.25,1,0.25), \off : Color(0.4,0.4,0.4)));
		
		gui[\onOffTheme3]=( \font_		: Font("Helvetica-Bold", 10),
						 \colors_     : (\on : Color.red, \off : Color.red/3));
						 				 
		gui[\sliderTheme]=(\thumbSize_	: 15,
						 \colors_		: (\background : Color(0,0,0,0.3),
									   \hilite : Color(0,0,0,0.3), 
						 			   \knob : gui[\highlightColours][4]));
				
		gui[\sliderTheme2]=(\thumbSize_	: 8,
						  \colors_	: (\background : Color(0,0,0,0.3),
						  			   \hilite : Color(0,0,0,0.4)));
						 				
		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background : Color(1,1,0.9)));
		
		gui[\menuTheme2]=( \font_		: Font("Helvetica", 10));
				
		gui[\boxTheme  ]=(	\orientation_ : \horizontal,
						\resoultion_	: 2,
						\showNumberBox_: false,
						\colors_      : (\label : Color(0.75,0.9,0.75),
									   \background : Color(1,1,0.9)));
										
		gui[\onOffTheme]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color.green, \off : Color(0.4,0.4,0.4)));
						
		gui[\knob2Theme0]=( \labelFont_	: Font("Helvetica",10), 
						 \colors_		: (\on : gui[\highlightColours][0]),
						 \showNumberBox_ : false);
						 
		gui[\knob2Theme1]=( \labelFont_	: Font("Helvetica",10),
						 \showNumberBox_ : false);
					
		gui[\knob2ThemeSN]=( \labelFont_	: Font("Helvetica",10), 
						 \colors_		: (\on : gui[\highlightColours][1]),
						 \showNumberBox_ : false);
						 
		gui[\knob2ThemeCP]=( \labelFont_	: Font("Helvetica",10), 
						 \colors_		: (\on : gui[\highlightColours][2]),
						 \showNumberBox_ : false);	
						 
		gui[\knob2ThemeTM]=( \labelFont_	: Font("Helvetica",10), 
						 \colors_		: (\on : gui[\highlightColours][3]),
						 \showNumberBox_ : false);
						 
		gui[\knob2ThemeHH]=( \labelFont_	: Font("Helvetica",10), 
						 \colors_		: (\on : gui[\highlightColours][4]),
						 \showNumberBox_ : false);	
				
		gui[\knob2Theme0Note]=( \labelFont_	: Font("Helvetica",10), 
						     \colors_		: (\on : gui[\highlightColours][0])); 
						  
		gui[\onOffTheme4]=( \font_		: Font("Helvetica-Bold", 11),
						 \colors_		: (\off : Color(0.7,0.7,0.7)/3));
		
	// The scroll views
	
		// the border and composite view
		gui[\compositeView] = MVC_RoundedComView(window,
				Rect(11,11,this.thisWidth-22,this.thisHeight-22))
				.color_(\border, Color(39/103,743/1792,117/296) )
				.color_(\border2, Color(0,9/77,1/103))
				.color_(\background, Color(0.818,0.818,0.818) );
	
	
	
		gui[\masterView]= MVC_CompositeView(gui[\compositeView],Rect(10, 204-29, 700, 275))
			.hasBorder_(false);
			
	
		gui[\tabView]=MVC_TabbedView(gui[\compositeView],Rect(6, 7, bounds.width-45+10, 160),
									(-1)@0)
			.labels_(["Bass","Snare","Clap","Tom","Hi Hat"])
			.tabWidth_([40,40,40,40,45])
			.tabCurve_(4)
			.labelColors_(gui[\drumColours]*1.25)
			.unfocusedColors_(Color.grey!5)
			.backgrounds_(Color.clear!5)
			.font_(GUI.font.new("Helvetica",12)).tabHeight_(\auto)
			.value_(p[3])
			.action_{|me| p[3]=me.value};
			
		
		gui[\drumViews]=nil!5;
		
		gui[\tabCompViews]=nil!5;
			
		gui[\drumColours].do({|c,j|
							
			gui[\drumViews][j]= MVC_RoundedCompositeView(gui[\masterView],
				Rect(1+(j*130),11,115,213))
				.forceHold_(true)
				.color_(\border,c*1.25)
				.color_(\background, Color.grey(j.map(0,4,0.6,0.4)) );
				
			gui[\tabCompViews][j]= MVC_RoundedCompositeView(gui[\tabView].mvcTab(j),
					Rect(5,5,bounds.width-45-5-5+10, 138-5))
				.color_(\border,c*1.25)
				.color_(\background, Color.grey(j.map(0,4,0.6,0.5)) )
				.hasBorder_(false)
				.autoScrolls_(false)
				.hasHorizontalScroller_(false)
				.hasVerticalScroller_(false)
				.autohidesScrollers_(false);
			
			MVC_PlainSquare(gui[\tabView].mvcTab(j),Rect(-1,140, 4, 5)).color_(\off,c*1.25);
				
		});
			
		gui[\miscView]= MVC_CompositeView(gui[\compositeView], Rect(265,158,400,20))
			.hasBorder_(false);
		
	// widgets
	

		// 1.on/off
		MVC_OnOffView(models[1],gui[\compositeView] ,Rect(221, 156, 22,18),gui[\onOffTheme1])
			.rounded_(true)
			.permanentStrings_(["On"]);
			
		// 0.solo
		MVC_OnOffView(models[0],gui[\compositeView] ,Rect(249, 156,20,18),gui[\soloTheme  ])
			.rounded_(true);
		
		// MIDI Settings
 		MVC_FlatButton(gui[\compositeView],Rect(279, 156, 43, 19),"MIDI")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(223/375,2/3,10665/19997) )
			.color_(\down,Color(223/375,2/3,10665/19997) )
			.color_(\string,Color.white)
			.action_{ this.createMIDIInModelWindow(gui[\compositeView],low:23,high:24) };
		
		// MIDI Control
		MVC_FlatButton(gui[\compositeView],Rect(329, 156, 43, 19),"Cntrl")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(223/375,2/3,10665/19997) )
			.color_(\down,Color(223/375,2/3,10665/19997) )
			.color_(\string,Color.white)
			.action_{ LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front };
			
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\compositeView],383@156,75,
			Color(0.5,0.55,0.5),Color.black,Color(0.5,0.55,0.5)/1.5);
		this.attachActionsToPresetGUI;
		
	
		// 132.master send channels
		MVC_PopUpMenu3(models[132],gui[\compositeView], Rect(561, 156, 70, 18),gui[\menuTheme]);		
		
		
		// 25.output channels
		MVC_PopUpMenu3(models[25],gui[\compositeView], Rect(637, 156, 70, 18),gui[\menuTheme]);
	
	
		// 2.amp
		MVC_SmoothSlider(models[2],gui[\masterView], Rect(658, 42, 20, 63),gui[\sliderTheme2])
			.color_(\knob,Color.white);
		
		// 11.bp scale (-1,1)
		MVC_MyKnob3(models[11],gui[\masterView],Rect(659,19,20,20),gui[\knob2Theme1])
			.color_(\on, Color.white);
			
		// 130.pan
		MVC_MyKnob3(models[130],gui[\masterView],Rect(659,166,20,20),gui[\knob2Theme1])
			.color_(\on, Color.white);
			

		// 133.master send
		MVC_MyKnob3(models[133],gui[\masterView],Rect(659,203,20,20),gui[\knob2Theme1])
			.color_(\on, Color.white);

			
		// 117.dur
		MVC_MyKnob3(models[117],gui[\masterView],Rect(659,128,20,20),gui[\knob2Theme1])
			.color_(\on,Color.white);
			
			
			
		
	// KICK DRUM

		// 4 & 12 bass note
		gui[\bd_noteRange]=MVC_MyKnob2RangeView(models[12],
			MVC_MyKnob2(models[4],gui[\drumViews][0],Rect(44,114,20,20),gui[\knob2Theme0Note])
				.numberFont_(Font("Helvetica",10)));
				
		// 5 & 13.dur
		gui[\bd_durRange]=MVC_MyKnob2RangeView(models[13],
			MVC_MyKnob2(models[5],gui[\drumViews][0],Rect(75,114,20,20),gui[\knob2Theme0]));
		
		// 6 & 14.attack time
		gui[\bd_r1Range]=MVC_MyKnob2RangeView(models[14],
			MVC_MyKnob2(models[6],gui[\drumViews][0],Rect(58,42,20,20),gui[\knob2Theme0]));
		
		// 7 & 15.attack amount
		gui[\bd_envRange]=MVC_MyKnob2RangeView(models[15],
			MVC_MyKnob2(models[7],gui[\drumViews][0],Rect(30,42,20,20),gui[\knob2Theme0]));
		
		// 8 & 16. 2nd attack rate
		gui[\bd_r2Range]=MVC_MyKnob2RangeView(models[16],
			MVC_MyKnob2(models[8],gui[\drumViews][0],Rect(87,42,20,20),gui[\knob2Theme0]));
		
		// 9 & 17. filter scale
		gui[\bd_filterRange]=MVC_MyKnob2RangeView(models[17],
			MVC_MyKnob2(models[9],gui[\drumViews][0],Rect(30,78,20,20),gui[\knob2Theme0]));
				
		// 10 & 18. q
		gui[\bd_qRange]=MVC_MyKnob2RangeView(models[18],
			MVC_MyKnob2(models[10],gui[\drumViews][0],Rect(58,78,20,20),gui[\knob2Theme0]));
				
		// 19 & 20. noise
		gui[\bd_noiseRange]=MVC_MyKnob2RangeView(models[20],
			MVC_MyKnob2(models[19],gui[\drumViews][0],Rect(88,78,20,20),gui[\knob2Theme0]));
		
	// SNARE DRUM
		
		// 67 & 68 snare note
		gui[\bd_noteRange]=MVC_MyKnob2RangeView(models[68],
			MVC_MyKnob2(models[67],gui[\drumViews][1],Rect(44,114,20,20),gui[\knob2Theme0Note])
				.numberFont_(Font("Helvetica",10))
				.color_(\on, gui[\highlightColours][1]));
		
		// 69 & 70 snare dur
		gui[\sn_durRange]=MVC_MyKnob2RangeView(models[70],
			MVC_MyKnob2(models[69],gui[\drumViews][1],Rect(75,114,20,20),gui[\knob2ThemeSN]));

		// 71 & 76 snare noise
		gui[\sn_noiseRange]=MVC_MyKnob2RangeView(models[76],
			MVC_MyKnob2(models[71],gui[\drumViews][1],Rect(87,78,20,20),gui[\knob2ThemeSN]));
			
		// 72 & 77 snare q
		gui[\sn_qRange]=MVC_MyKnob2RangeView(models[77],
			MVC_MyKnob2(models[72],gui[\drumViews][1],Rect(58,78,20,20),gui[\knob2ThemeSN]));
		
		// 73 & 78 snare filter
		gui[\sn_filterRange]=MVC_MyKnob2RangeView(models[78],
			MVC_MyKnob2(models[73],gui[\drumViews][1],Rect(30,78,20,20),gui[\knob2ThemeSN]));
		
		// 74 & 79 snare env
		gui[\sn_envRange]=MVC_MyKnob2RangeView(models[79],
			MVC_MyKnob2(models[74],gui[\drumViews][1],Rect(44,42,20,20),gui[\knob2ThemeSN]));
		
		// 75 & 80 snare noise dur
		gui[\sn_noiseDurRange]=MVC_MyKnob2RangeView(models[80],
			MVC_MyKnob2(models[75],gui[\drumViews][1],Rect(75,42,20,20),gui[\knob2ThemeSN]));
			
	// CLAP
		// 81 & 82. duration
		gui[\cp_durRange]=MVC_MyKnob2RangeView(models[82],
			MVC_MyKnob2(models[81],gui[\drumViews][2],Rect(58,114,20,20),gui[\knob2ThemeCP]));

		// 83 & 84. Q
		gui[\cp_QRange]=MVC_MyKnob2RangeView(models[84],
			MVC_MyKnob2(models[83],gui[\drumViews][2],Rect(88,78,20,20),gui[\knob2ThemeCP]));
			
		// 85 & 86. Filt Fq
		gui[\cp_filtRange]=MVC_MyKnob2RangeView(models[86],
			MVC_MyKnob2(models[85],gui[\drumViews][2],Rect(30,78,20,20),gui[\knob2ThemeCP]));
			
		// 87 & 88. rand
		gui[\cp_randRange]=MVC_MyKnob2RangeView(models[88],
			MVC_MyKnob2(models[87],gui[\drumViews][2],Rect(58,42,20,20),gui[\knob2ThemeCP]));
			
	// TOM TOM
	
		// 89-90 note & note range
		gui[\tm_noteRange]=MVC_MyKnob2RangeView(models[90],
			MVC_MyKnob2(models[89],gui[\drumViews][3],Rect(58,114,20,20),gui[\knob2ThemeTM])
				.showNumberBox_(true).numberFont_(Font("Helvetica",10)));
		
		// 91-92 dur & dur range
		gui[\tm_durRange]=MVC_MyKnob2RangeView(models[92],
			MVC_MyKnob2(models[91],gui[\drumViews][3],Rect(87,114,20,20),gui[\knob2ThemeTM]));
			
		// 93+94 timbre & timbre range
		gui[\tm_fmRange]=MVC_MyKnob2RangeView(models[94],
			MVC_MyKnob2(models[93],gui[\drumViews][3],Rect(58,78,20,20),gui[\knob2ThemeTM]));
			
		// 95+96 carrier adjust & range
		gui[\tm_adjRange]=MVC_MyKnob2RangeView(models[96],
			MVC_MyKnob2(models[95],gui[\drumViews][3],Rect(30,78,20,20),gui[\knob2ThemeTM]));
			
		// 97+98 index slope & range
		gui[\tm_slopeRange]=MVC_MyKnob2RangeView(models[98],
			MVC_MyKnob2(models[97],gui[\drumViews][3],Rect(87,78,20,20),gui[\knob2ThemeTM]));
			
		// 99+100 r1 dur range
		gui[\tm_r1Range]=MVC_MyKnob2RangeView(models[100],
			MVC_MyKnob2(models[99],gui[\drumViews][3],Rect(58,42,20,20),gui[\knob2ThemeTM]));
			
		// 101+102 attackAmount
		gui[\tm_envRange]=MVC_MyKnob2RangeView(models[102],
			MVC_MyKnob2(models[101],gui[\drumViews][3],Rect(30,42,20,20),gui[\knob2ThemeTM]));
			
		// 103+104 stick
		gui[\tm_stickRange]=MVC_MyKnob2RangeView(models[104],
			MVC_MyKnob2(models[103],gui[\drumViews][3],Rect(87,42,20,20),gui[\knob2ThemeTM]));
			
	// hat
			
		// 105-106 dur
		gui[\hh_durRange]=MVC_MyKnob2RangeView(models[106],
			MVC_MyKnob2(models[105],gui[\drumViews][4],Rect(58,114,20,20),gui[\knob2ThemeHH]));
		
		// 107-108 tone
		gui[\hh_toneRange]=MVC_MyKnob2RangeView(models[108],
			MVC_MyKnob2(models[107],gui[\drumViews][4],Rect(44,78,20,20),gui[\knob2ThemeHH]));
			
		// 111-112 lp
		gui[\hh_lpRange]=MVC_MyKnob2RangeView(models[112],
			MVC_MyKnob2(models[111],gui[\drumViews][4],Rect(30,42,20,20),gui[\knob2ThemeHH]));
			
		// 113-114 lp q
		gui[\hh_lpqRange]=MVC_MyKnob2RangeView(models[114],
			MVC_MyKnob2(models[113],gui[\drumViews][4],Rect(58,42,20,20),gui[\knob2ThemeHH]));
			
		// 115-116 hp
		gui[\hh_hpRange]=MVC_MyKnob2RangeView(models[116],
			MVC_MyKnob2(models[115],gui[\drumViews][4],Rect(87,42,20,20),gui[\knob2ThemeHH]));
		
	// all drums
		
		gui[\lamps]=nil!5;
		
		gui[\drumViews].do{|view,y|
			// onOff
			MVC_OnOffView(models[120+y],view,Rect(5,4,38,19),gui[\onOffTheme4])
				.rounded_(true)
				.color_(\on,gui[\highlightColours][y]);
			// BP
			MVC_OnOffView(models[52+y],view,Rect(89,4,19,19),gui[\onOffTheme4])
				.rounded_(true)
				.color_(\on,gui[\highlightColours][y]);
			// Amp
			MVC_SmoothSlider(models[125+y],view,Rect(5,29,20,65),gui[\sliderTheme2])
				.color_(\knob,gui[\highlightColours][y]);
			// lamp
			gui[\lamps][y]=MVC_LampView(view,Rect(58,5,17,17))
				.action_{|me| this.bang(y,100/127) };	
			// velocity
			MVC_MyKnob(models[57+y],view,Rect(6,114,20,20),gui[\knob2Theme1])
				.color_(\on, gui[\highlightColours][y]);
			// 32-36.pan
			MVC_MyKnob(models[32+y],view,Rect(6,150,20,20),gui[\knob2Theme1])
				.color_(\on, gui[\highlightColours][y])
				.color_(\pin, gui[\highlightColours][y]);
			// 42-46.send & range (47-51)
			gui[(\send++y).asSymbol]=MVC_MyKnob2RangeView(models[47+y],
				MVC_MyKnob2(models[42+y],view,Rect(6,186,20,20),gui[\knob2Theme1])
					.color_(\on, gui[\highlightColours][y]);
			);
			// 27-31.out channel
			gui[\q]=MVC_PopUpMenu3(models[27+y],view,Rect(37,152,70,17),gui[\menuTheme2])
				.color_(\background,(gui[\drumColours][y]/3).alpha_(0.5))
				.color_(\string, gui[\highlightColours][y]);
			// 37-41.send channel
			MVC_PopUpMenu3(models[37+y],view,Rect(37,185,70,17),gui[\menuTheme2])
				.color_(\background,(gui[\drumColours][y]/3).alpha_(0.5))
				.color_(\string, gui[\highlightColours][y]);
		};
		
		// other GUI items

		channels.do{|y|
			var theme,tab;
			// seq
			
			tab=gui[\tabCompViews][y];
			
			defaultSteps.do{|x|
				var c,col;
				c=(x%(sP[y][4])).map(0,sP[y][4],1,0.4);
				col=gui[\sliderColours][y];
				seqMVCViews[y][x]= MVC_PinSlider(tab,seqModels[y][x],
										Rect(71+(x*19),20,17,100))
				.color_(\background,Color.black)
				.color_(\border,Color(col.red*c/3.4,col.green*c/3.4,col.blue*c/3.4))
				.color_(\on,Color(col.red*c,col.green*c,col.blue*c))
				.color_(\hilite,Color(col.red*c,col.green*c,col.blue*c))
				.hiliteMode_(\inner)
				.seqItems_(seqMVCViews[y]);
			};
			
			theme=(	\orientation_  : \horizontal,
					\resoultion_	 : 1,
					\font_		 : Font("Helvetica",10),
					\labelFont_	 : Font("Helvetica",10),
					\showNumberBox_: false,
					\colors_       : (	\label : Color.white,
									\background : Color(0.1,0.1,0.1),
									\string : gui[\highlightColours][y],
									\focus : Color(0,0,0,0)));

			// 4.ruler	
	 	 	MVC_NumberBox(spModels[y][4],tab,Rect(40,6,23,17),theme);
	 	  	MVC_RulerView(spModels[y][4],tab,Rect(70,2,(defaultSteps*19),15))
	 	  		.buttonWidth_(19)
	 	   		.label_(nil).steps_(defaultSteps)
	 	   		.color_(\on,Color.black)
	 	   		.color_(\string,Color.white)
				.color_(\background,Color(0,0,0,0.2));
				
			//3.steps
			MVC_NumberBox(spModels[y][3],tab,Rect(40,25,23,17),theme);
			// 6.speed
		  	MVC_NumberBox(spModels[y][6],tab,Rect(40,44,23,17),theme);
		  	
		  	// pos
		  	MVC_PosView(posModels[y],tab,Rect(70, 112+10,(defaultSteps*19), 9))
		  		.type_(\circle)
				.color_(\on,gui[\highlightColours][y])
				.color_(\background,Color.clear);
				
							
			// pos Hilite Adaptor
			gui[\posHiliteAdaptor]=MVC_HiliteAdaptor(posModels[y])
				.refreshZeros_(false)
				.views_(seqMVCViews[y]);
			
		}
	}
		
	arrangeWindow{
//		// use p[26];
//		if (p[26]==1) {
//			gui[\tabView].bounds_(Rect(10, 32, bounds.width-40+15, 140));
//			gui[\miscView].bounds_(Rect(265,158,400,20));
//			gui[\masterView].bounds_(Rect(10,179,650,225));
//			window.setInnerExtent(thisWidth,thisHeight);
//			
//		}{
//			gui[\tabView].bounds_(Rect(10, -1032, 0, 0));
//			gui[\miscView].bounds_(Rect(50,30,400,20));
//			gui[\masterView].bounds_(Rect(10,179-138,650,225));
//			window.setInnerExtent(thisWidth,thisHeight-138);
//		};
	}
	
	changeSeqColours{|y,value|
		defaultSteps.do({|x|	
				var c,col;
				col=gui[\sliderColours][y];
				c=((x)%value).map(0,value,1,0.4);
				seqMVCViews[y][x]
					.color_(\border,Color(col.red*c/3.4,col.green*c/3.4,col.blue*c/3.4))
					.color_(\on,Color(col.red*c,col.green*c,col.blue*c));
				});	
	}

} // end ////////////////////////////////////
