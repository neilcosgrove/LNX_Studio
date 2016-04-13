
+ LNX_BumNote2 {

	//// GUI ///////////////////////////////////////////
	
	*thisWidth  {^711+22-10}
	*thisHeight {^573+22-30}
	
	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(0,1/103,9/77,65/77)); 
	}

	// create all the GUI widgets while attaching them to models
	createWidgets{
	
		var r1,r2,r3; 
		var darkBorder      = Color(0,1/103,9/77,65/77);
		var border          = Color(0, 0.0097087378640777, 0.11688311688312, 0.84415584415584);
		var background1     = Color(0.50602409638554, 0.44615384615385, 0.63636363636364);
		var lightBackground = Color(59/77,59/77,63/77);
		
		// Themes
						
		gui[\menuTheme2]=( \font_		: Font("Arial", 10),
						\labelShadow_	: false,
						\colors_      : (\background:darkBorder,
										\label:Color.black,
										\string:Color(0.4,1,1),
									   \focus:Color.clear));
		
		gui[\boxTheme  ]=(
						\resoultion_	: 6,
						\showNumberBox_: false,
						\labelShadow_	: false,
						\colors_      : (\label : Color.black, 
								\background : Color(1,1,1,0.66)));
		
		gui[\boxTheme2 ]=(	\orientation_ : \horizontal,
						\resoultion_	: 2,
						\font_		: Font("Helvetica",10),
						\labelFont_	: Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_      : (\label : Color.white, \background : Color(0.2,0.2,0.2),
										\string : Color.orange, \focus : Color(0,0,0,0)));
						
		gui[\labelTheme]=( \font_		:  Font("Helvetica-Bold", 14),
						\align_		: \left,
						\shadow_		: false,
						\noShadows_	: 0,
						\colors_		: (\string : Color.black));
						
		gui[\labelTheme2]=( \font_		: Font("Chalkboard-Bold", 14),
						\align_		: \left,
						\colors_		: (\string : Color(1,0.75,0)));
						
		gui[\sliderTheme]=(\thumbSize_	: 18,
						\border_		: 4,
						 \labelFont_	: Font("Helvetica-Bold", 14),
						 \colors_		: (	\background : Color(0,0,0,0.3), 
						 				\border     : darkBorder,
						 				\knob       : Color(0.25,0.75,1),
						 				\label      : Color.black, 
									  	\numberUp   :Color.black,
									  	\numberDown :Color.white),
						\labelShadow_	: false);
						
		gui[\onOffTheme]=( \font_		: Font("Helvetica-Bold", 12),
		 				\rounded_		: true,
						\colors_      : (\on : Color.orange, \off : Color(0.4,0.4,0.4)));

						
		gui[\onOffTheme3]=( \font_		: Font("Helvetica-Bold", 10),
						 \rounded_	: true,
						 \colors_     : (\on : Color.red, \off : Color.red/3));
						 
		gui[\onOffTheme4]=( \font_		: Font("Helvetica-Bold", 11),
						 \rounded_	: true,
						 \colors_     : (\on : (Color.orange+Color.red)/2, 
						 			   \off : (Color.orange+Color.red)/4));
		
		gui[\onOffTheme5]=( \font_		: Font("Helvetica-Bold", 11),
						 \rounded_	: true,
						 \colors_     : (\on : Color.orange, 
						 			   \off : Color.orange/2));
						 			   
		gui[\onOffTheme6]=( \font_		: Font("Helvetica-Bold", 11),
						 \rounded_	: true,
						 \colors_     : (\off : Color.orange, 
						 			   \on : Color.yellow));
						 			   
		gui[\multiTheme ]=(\font_:Font("Helvetica",11),
			\states_ : [
				["Low"   ,Color.red/1.5   ,Color.black,Color.grey/3,Color.grey/2],
				["Med"   ,(Color.orange+Color.red)/2,Color.black,Color.grey/2,Color.grey/3],
				["Hi"    ,Color.orange,Color.black,Color.grey/4,Color.grey/2]]);
		
		// from mm
				
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));

		gui[\onOffTheme1]=( \font_		: Font("Helvetica-Bold", 12),
						 \colors_     : (\on : Color(0.25,0.75,1),
						 				\off : Color(0.4,0.4,0.4)),
						 \rounded_		: true
						 );
						 
		gui[\onOffTheme2]=( \font_		: Font("Helvetica-Bold", 12),
						 \colors_     : (\off : Color(0.25,0.75,1),
						 				\on : Color(0.75,0.25,1)),
						 \rounded_		: true
						 );
				
		gui[\onOffTheme3]=( \font_		: Font("Helvetica-Bold", 12),
						 \colors_     : (\on : Color(0.25,0.75,1),
						 				\off : Color(0.75,0.25,1)),
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
										
		gui[\knob1Theme]=(\colors_		: (\on : Color(0.25,0.75,1),
									   \label: Color.black, 
									   \numberUp:Color.black,
									   \numberDown:Color.white),
						\numberWidth_ : -12,
						\labelShadow_	: false,
						\labelFont_   : Font("Helvetica",12),
						\numberFont_  : Font("Helvetica",11));

		gui[\knob2Theme]=(\colors_		: (\on : Color(0.25,0.75,1)*1.5,
									   \label: Color.black, 
									   \numberUp:Color.black,
									   \numberDown:Color.white),
						\numberWidth_ : -12,
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

		r1=18; r2=76; r3=65;
				
		// composite views ////////////////////////////////////////////////////////////////

		gui[\scrollView] = MVC_RoundedCompositeView(window,
				Rect(11,11,this.thisWidth-22,this.thisHeight-23))
			.color_(\background, lightBackground )
			.color_(\border, background1 )
			.width_(6);
		
		// The Tabs
			
		MVC_PlainSquare(gui[\scrollView],Rect(9,27, 4, 5)).color_(\off,border);
		
		gui[\masterTabs]=MVC_TabbedView(gui[\scrollView], Rect(9, 6, 691, 430))
			.labels_(["Controls","Step","Piano"])
			.font_(Font("Helvetica-Bold", 14))
			.tabPosition_(\top)
			.unfocusedColors_(Color(0.35, 0.35, 0.35,0.75) ! 3)
			.labelColors_(  darkBorder!3)
			.backgrounds_(  Color.clear!3)
			.tabCurve_(5)
			.tabWidth_([80,55,55])
			.tabHeight_(22)
			.followEdges_(true)
			.value_(0);
		
		// control tab
		
		gui[\controlsView] = MVC_CompositeView(gui[\masterTabs].mvcTab(0), Rect(4, 4, 675, 404))
			.color_(\background,lightBackground * 0.8)
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);
		
		MVC_RoundBounds(gui[\masterTabs].mvcTab(0),Rect(6, 6, 671, 396))
			.width_(6)
			.color_(\background, darkBorder);
		
		// sequencer tab
			
		gui[\seqView] = MVC_CompositeView(gui[\masterTabs].mvcTab(1),Rect(4, 4, 675, 404))
			.color_(\background,lightBackground * 0.8)
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);
		
		MVC_RoundBounds(gui[\masterTabs].mvcTab(1),Rect(6, 6, 671, 396))
			.width_(6)
			.color_(\background, darkBorder);
			

		// piano roll tab
			
		gui[\pianoView] = MVC_CompositeView(gui[\masterTabs].mvcTab(2),Rect(4, 4, 675, 404))
			.color_(\background,lightBackground * 0.8)
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);
		
		MVC_RoundBounds(gui[\masterTabs].mvcTab(2),Rect(6, 6, 671, 396))
			.width_(6)
			.color_(\background, darkBorder);

		pianoRoll.createWidgets(gui[\pianoView],Rect(7, 12, 660, 380),
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
				), 0,
				parentViews: [ window, gui[\masterTabs].mvcTab(2)]
				
				);

		// lines ////////////////////////////////////////////////////////////////////
				
		//saw
		MVC_PlainSquare(gui[\controlsView],Rect(108, 274, 26, 6)).color_(\off,border);
		// tri
		MVC_PlainSquare(gui[\controlsView],Rect(112, 172, 8, 6)).color_(\off,border);
		MVC_PlainSquare(gui[\controlsView],Rect(120, 172, 5, 50)).color_(\off,border);
		MVC_PlainSquare(gui[\controlsView],Rect(125, 216, 9, 6)).color_(\off,border);
		// mixer lp
		MVC_PlainSquare(gui[\controlsView],Rect(191, 261, 24, 6)).color_(\off,border);
		// hp adsr
		MVC_PlainSquare(gui[\controlsView],Rect(432, 64, 16, 6)).color_(\off,border);
		// adsr volume
		MVC_PlainSquare(gui[\controlsView],Rect(615, 64, 10, 6)).color_(\off,border);
		// lp hp
		MVC_PlainSquare(gui[\controlsView],Rect(227, 161, 5, 36)).color_(\off,border);
		// pulse
		MVC_PlainSquare(gui[\controlsView],Rect(120, 101, 5, 62)).color_(\off,border);		MVC_PlainSquare(gui[\controlsView],Rect(125, 157, 9, 6)).color_(\off,border);
		
		// lfo pwm
		MVC_PlainSquare(gui[\controlsView],Rect(159, 101, 5, 5)).color_(\off,border);
		// lfo hp
		MVC_PlainSquare(gui[\controlsView],Rect(370, 25, 5, 5)).color_(\off,border);
		// lfo lp
		MVC_PlainSquare(gui[\controlsView],Rect(370,192, 5, 5)).color_(\off,border);
		
		// link
		MVC_PlainSquare(models[43],gui[\controlsView],Rect(32, 101, 3, 139))
			.color_(\off,Color(0,0,0,0.21))
			.color_(\on,border);
			
		// BUm note text
		MVC_Text(gui[\controlsView],Rect(2, 0, 111, 25))
				.align_(\center)
				.shadow_(false)
				.penShadow_(true)
				.font_(Font("AvenirNext-Heavy",16))
				.string_("Bum Note 2");
						
		// the high pass filter ///////////////////////////////////////////////////////////////////
			
		MVC_StaticText(gui[\controlsView], Rect(215, 11, 81, 18),gui[\labelTheme])
			.string_("High Pass");	
			
		gui[\filterViewHP] = MVC_RoundedCompositeView(gui[\controlsView],Rect(221, 36, 205, 119))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
//			
//		MVC_UserView(gui[\filterViewHP], Rect(66+12,25+4,98-24,70-7))
//			.parent_(gui[\masterTabs].mvcTab(0))
//			.drawFunc_{|me|
//				Pen.use{
//					var w = me.bounds.width;
//					var h = me.bounds.height-3;
//					Color(1,1,1,0.25).set;
//					Pen.width_(3);
//					Pen.lineDash_(FloatArray[2,3]);
//					Pen.moveTo(0@h).lineTo((w/2)@0).lineTo(w@h).lineTo(0@h);
//					Pen.stroke;
//					Color(1,1,1,0.15).set;
//					Pen.moveTo(0@h).lineTo((w/2)@0).lineTo(w@h).lineTo(0@h);
//					Pen.fill;
//				};
//			};
//				
		// filter response HP
		MVC_FlatDisplay(gui[\filterViewHP],highModel,Rect(5, 3, 7, 114)).invert_(true)
			.color_(\slider,Color(0.25,0.75,1)*1.5);
		MVC_Scale(gui[\filterViewHP],Rect(3, 3, 2, 114));
		MVC_Scale(gui[\filterViewHP],Rect(12, 3, 2, 114));
		
		// 45.filter fq
		MVC_MyKnob3(models[45],gui[\filterViewHP],Rect(21, 41, 40, 40),gui[\knob1Theme ])
			.resoultion_(1.5);
		// 46.filter q
		MVC_MyKnob3(models[46],gui[\filterViewHP],Rect( 46+20,r1,26,26),gui[\knob1Theme ])
			.resoultion_(1.5);
		// 47.filter env
		MVC_MyKnob3(models[47],gui[\filterViewHP],Rect( 82+20,r1,26,26),gui[\knob1Theme ]);
		// 48.filterLFO
		MVC_MyKnob3(models[48],gui[\filterViewHP],Rect(118+20,r1,26,26),gui[\knob1Theme ]);
		// 49.filter KYBD
		MVC_MyKnob3(models[49],gui[\filterViewHP],Rect(154+20,r1,26,26),gui[\knob1Theme ]);
		// 51.Filter Attack (Env)
		MVC_MyKnob3(models[51],gui[\filterViewHP],Rect(46+20,r2,26,26),gui[\knob1Theme  ]);
		// 52.Filter Release (Env)
		MVC_MyKnob3(models[52],gui[\filterViewHP],Rect(118+20,r2,26,26),gui[\knob1Theme ]);
		// 53. filter curve
		MVC_MyKnob3(models[53],gui[\filterViewHP],Rect(105, 79,20,20),gui[\knob2Theme ]);
		// 54.velocity > filter
		MVC_MyKnob3(models[54],gui[\filterViewHP],Rect(177, 79, 20, 20),gui[\knob2Theme ]);
		
		// 67.filterHP lfo A or B	
		MVC_OnOffView(models[67],gui[\controlsView  ],Rect(362, 5, 21, 21),gui[\onOffTheme2]);
		
		// filt env on lamp
		gui[\filtEnvLamp1]=MVC_PipeLampView(gui[\controlsView],Rect(330, 14, 10, 10))
			.color_(\on,Color.red);
	
		// the low pass filter ////////////////////////////////////////////////////////////////////
		
		MVC_StaticText(gui[\controlsView], Rect(240, 178, 81, 18),gui[\labelTheme])
			.string_("Low Pass");
		
		gui[\filterView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(221, 203, 205, 119))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
//			
//		MVC_UserView(gui[\filterView], Rect(66+12,25+4,98-24,70-7))
//			.parent_(gui[\masterTabs].mvcTab(0)) 
//			.drawFunc_{|me|
//				Pen.use{
//					var w = me.bounds.width;
//					var h = me.bounds.height-3;
//					Pen.width_(3);
//					Color(1,1,1,0.25).set;
//					Pen.lineDash_(FloatArray[2,3]);
//					Pen.moveTo(0@h).lineTo((w/2)@0).lineTo(w@h).lineTo(0@h);
//					Pen.stroke;
//					Color(1,1,1,0.15).set;
//					Pen.moveTo(0@h).lineTo((w/2)@0).lineTo(w@h).lineTo(0@h);
//					Pen.fill;
//				};
//			};
			
		// filter response LP
		MVC_FlatDisplay(gui[\filterView],lowModel,Rect(5, 3, 7, 114))
			.color_(\slider,Color(0.25,0.75,1)*1.5);
		MVC_Scale(gui[\filterView],Rect(3, 3, 2, 114));
		MVC_Scale(gui[\filterView],Rect(12, 3, 2, 114));

		// 9.filter fq
		MVC_MyKnob3(models[ 9],gui[\filterView],Rect(21, 41, 40, 40),gui[\knob1Theme ])
			.resoultion_(1.5);
		// 3.filter q
		MVC_MyKnob3(models[ 3],gui[\filterView],Rect( 46+20,r1,26,26),gui[\knob1Theme ])
			.resoultion_(1.5);
		// 4.filter env
		MVC_MyKnob3(models[ 4],gui[\filterView],Rect( 82+20,r1,26,26),gui[\knob1Theme ]);
		// 10.filterLFO
		MVC_MyKnob3(models[10],gui[\filterView],Rect(118+20,r1,26,26),gui[\knob1Theme ]);
		// 18.filter KYBD
		MVC_MyKnob3(models[18],gui[\filterView],Rect(154+20,r1,26,26),gui[\knob1Theme ]);
		// 24.Filter Attack (Env)
		MVC_MyKnob3(models[24],gui[\filterView],Rect(46+20,r2,26,26),gui[\knob1Theme  ]);
		// 25.Filter Release (Env)
		MVC_MyKnob3(models[25],gui[\filterView],Rect(118+20,r2,26,26),gui[\knob1Theme ]);
		// 39. filter curve
		MVC_MyKnob3(models[39],gui[\filterView],Rect(105, 79,20,20),gui[\knob2Theme ]);
		// 33.velocity > filter
		MVC_MyKnob3(models[33],gui[\filterView],Rect(177, 79, 20, 20),gui[\knob2Theme ]);
		
		// 68.filterLP lfo A or B	
		MVC_OnOffView(models[68],gui[\controlsView  ],Rect(362, 171, 21, 21),gui[\onOffTheme2]);
		
		// filt env on lamp
		gui[\filtEnvLamp2]=MVC_PipeLampView(gui[\controlsView],Rect(330, 180, 10, 10))
			.color_(\on,Color.red);	
		
		/////////////////////////////////////////////////////////////////////////
		
		gui[\lfoOuterView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(464, 118, 137, 223))
			.color_(\background, Color(1,1,1,0.4) )
			.color_(\border, Color(1,1,1,0.4) )
			.width_(6);
	
		// lfo A /////////////////////////////////////////////////////////////////////////

		MVC_StaticText(gui[\controlsView], Rect(469, 114, 51, 18),gui[\labelTheme])
			.string_("LFO A");

		gui[\lfoView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(470,138,125,80))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
		
		// 75. SyncOnLfoA	- lfoSyncOnA
		MVC_OnOffView(models[75],gui[\lfoView ],Rect(6, 3, 20, 20),gui[\onOffTheme1]);
	
		// 57. lfo wave	
		MVC_PopUpMenu3(models[57],gui[\lfoView  ],Rect(31,5,67,16),gui[\menuTheme2]);
		// 6.lfo freq
		gui[\lfoKnobA] =
			MVC_MyKnob3(models[ 6],gui[\lfoView   ],Rect(11,40,26,26),gui[\knob1Theme ]);
			
		// the lfo lamp
		MVC_PipeLampView(lfoModelA, gui[\lfoView   ], Rect(105, 8, 10, 10))
			.color_(\on,Color.red); 
		// 56. lfo slope
		MVC_MyKnob3(models[ 56],gui[\lfoView   ],Rect( 52,40,26,26),gui[\knob1Theme ]);
		// 62. lfo s&h
		MVC_MyKnob3(models[ 62],gui[\lfoView   ],Rect(91, 40, 26, 26),gui[\knob1Theme ])
			.resoultion_(2);
		
		// lfo B /////////////////////////////////////////////////////////////////////////
		
		MVC_StaticText(gui[\controlsView], Rect(469, 232, 51, 18),gui[\labelTheme])
			.string_("LFO B");
		
		gui[\lfoView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(470,255,125,80))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
		
		// 77. SyncOnLfoA	- lfoSyncOnA
		MVC_OnOffView(models[77],gui[\lfoView ],Rect(6, 3, 20, 20),gui[\onOffTheme1]);
	
		// 63. lfo wave	
		MVC_PopUpMenu3(models[63],gui[\lfoView  ],Rect(31,5,67,16),gui[\menuTheme2]);
		
		// 64.lfo freq
		gui[\lfoKnobB] =
			MVC_MyKnob3(models[64],gui[\lfoView   ],Rect(11,40,26,26),gui[\knob1Theme ]);
		
		// the lfo lamp
		MVC_PipeLampView(lfoModelB, gui[\lfoView   ],  Rect(105, 8, 10, 10))
			.color_(\on,Color.red); 
		// 65. lfo slope
		MVC_MyKnob3(models[65],gui[\lfoView   ],Rect( 52,40,26,26),gui[\knob1Theme ]);
		// 66. lfo s&h
		MVC_MyKnob3(models[66],gui[\lfoView   ],Rect(91, 40, 26, 26),gui[\knob1Theme ])
			.resoultion_(2);
		
		// pulse osc ////////////////////////////////////////////////////////////////////
				
		MVC_StaticText(gui[\controlsView], Rect(143, 11, 50, 18),gui[\labelTheme])
			.string_("Pulse");
				
		gui[\oscView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(25, 35, 162, 60))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);		
			
		// 13.osc1 pitch
		MVC_MyKnob3(models[13],gui[\oscView   ],Rect( 10,r1,26,26),gui[\knob1Theme ])
			.resoultion_(4.5);
		
		// 14.osc1 fine
		MVC_MyKnob3(models[14],gui[\oscView   ],Rect( 46+3,r1,26,26),gui[\knob1Theme ]);
		
		// 5.pusle width
		MVC_MyKnob3(models[ 5],gui[\oscView   ],Rect( 46+35+6,r1,26,26),gui[\knob1Theme ]);
		
		// 61. PWM
		MVC_MyKnob3(models[61],gui[\oscView],Rect(46+35+35+9, r1, 26, 26),gui[\knob1Theme ]);
		
		// 70.PWM lfo A or B	
		MVC_OnOffView(models[70],gui[\controlsView  ],Rect(151, 106, 21, 21),gui[\onOffTheme2]);
		
		// tri osc ////////////////////////////////////////////////////////////////////////
		
		MVC_StaticText(gui[\controlsView], Rect(71, 118, 25, 18),gui[\labelTheme])
			.string_("Tri");
		
		gui[\oscView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(64, 142, 42, 60))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);		
		
		// 50. Tri mod	
		MVC_MyKnob3(models[50],gui[\oscView],Rect( 46-40,r1,26,26),gui[\knob1Theme ]);
		
		// saw osc ////////////////////////////////////////////////////////////////////////
		
		MVC_StaticText(gui[\controlsView], Rect(46, 223, 33, 18),gui[\labelTheme])
			.string_("Saw");
		
		gui[\oscView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(20, 246, 82, 60))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
		
		// 43.link osc''s
		MVC_OnOffView(models[43],gui[\controlsView ],Rect(17, 161, 35, 20),gui[\onOffTheme1]);
		
		// 15.osc2 pitch
		MVC_MyKnob3(models[15],gui[\oscView   ],Rect( 10,r1,26,26),gui[\knob1Theme ])
			.resoultion_(4.5);
			
		// 16.osc2 fine
		MVC_MyKnob3(models[16],gui[\oscView   ],Rect( 46,r1,26,26),gui[\knob1Theme ]);
		
		// mixer //////////////////////////////////////////////////////////////////////////
		
		MVC_StaticText(gui[\controlsView], Rect(142, 378, 44, 18),gui[\labelTheme])
			.string_("Mixer");
		
		gui[\oscView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(140, 138, 45, 232))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);	
			
		// 8.pulse Amp
		MVC_MyKnob3(models[ 8],gui[\oscView],Rect(10, r1, 26, 26),gui[\knob1Theme ])
			.numberWidth_(0);
		
		// 36. Tri amp	
		MVC_MyKnob3(models[36],gui[\oscView],Rect( 10,r1+58,26,26),gui[\knob1Theme ])
			.numberWidth_(0);
				
		// 42.saw Amp
		MVC_MyKnob3(models[42],gui[\oscView],Rect(10,r1+58+58,26,26),gui[\knob1Theme ])
			.numberWidth_(0);		
		
		// 17.noise amp
		MVC_MyKnob3(models[ 17],gui[\oscView],Rect(10,r1+58+58+58,26,26),gui[\knob1Theme ])
			.numberWidth_(0);		
			
		// other osc controls /////////////////////////////////////////////////////////////
		
		gui[\slideLFOView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(20, 333, 98, 52))
			.color_(\background, Color(1,1,1,0.4) )
			.color_(\border, Color(1,1,1,0.4) )
			.width_(6);
					
		// lfo pitch
		MVC_PlainSquare(gui[\slideLFOView],Rect(67,24, 9, 5)).color_(\off,border);
					
		// 19.slide osc pitch
		MVC_MyKnob3(models[19],gui[\slideLFOView],Rect(5, 14, 26, 26),gui[\knob1Theme ]);
		
		// 7.mod amp (lfo > pitch)
		MVC_MyKnob3(models[ 7],gui[\slideLFOView],Rect(41, 14, 26, 26),gui[\knob1Theme ]);
		
		// 69.pitch lfo A or B		
		MVC_OnOffView(models[69],gui[\slideLFOView  ],Rect(76, 16, 21, 21),gui[\onOffTheme2]);
			
		// adsr /////////////////////////////////////////////////////////////////////////
		
		MVC_StaticText(gui[\controlsView], Rect(452, 11, 51, 18),gui[\labelTheme])
			.string_("ADSR");
		
		gui[\adsrView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(454,36,155,61))
			.color_(\background, background1 )
			.color_(\border, border )
			.width_(6);
		
		// 20.Attack (Amp ADSR Env)
		MVC_MyKnob3(models[20],gui[\adsrView  ],Rect( 10,r1,26,26),gui[\knob1Theme ]);
		// 21.Decay (Amp ADSR Env)	
		MVC_MyKnob3(models[21],gui[\adsrView  ],Rect( 46,r1,26,26),gui[\knob1Theme ]);
		// 22.Sustain (Amp ADSR Env)
		MVC_MyKnob3(models[22],gui[\adsrView  ],Rect( 82,r1,26,26),gui[\knob1Theme ]);
		// 23.Release (Amp ADSR Env)
		MVC_MyKnob3(models[23],gui[\adsrView  ],Rect(118,r1,26,26),gui[\knob1Theme ]);
		
		// note on lamp
		gui[\noteOnLamp]=MVC_PipeLampView(gui[\controlsView],Rect(526, 14, 10, 10))
			.color_(\on,Color.red);

		// amp ///////////////////////////////////////////////////////////////////////////////
		
		// 2.amp
		MVC_SmoothSlider(models[ 2],gui[\controlsView],Rect(625, 30, 38, 145),gui[\sliderTheme]);
		
		// 26.Pan
		MVC_MyKnob3(models[26],gui[\controlsView],Rect(631,209, 26, 26),gui[\knob1Theme])
			.numberFont_(Font("Helvetica",10))
			.numberWidth_(-22);
			
		// 31.send amp
		MVC_MyKnob3(models[31],gui[\controlsView],Rect(631, 263, 26, 26),gui[\knob1Theme ])
			.numberFunc_(\float1)
			.numberFont_(Font("Helvetica",10))
			.numberWidth_(0);

		// 74. velocity
		MVC_MyKnob3(models[74],gui[\controlsView],Rect(631, 318, 26, 26),gui[\knob1Theme ])
			.numberFont_(Font("Helvetica",10));

		// 71. Mono Poly
		MVC_OnOffView(models[71],gui[\controlsView  ],Rect(623, 362, 43, 21),gui[\onOffTheme3 ]);
	
		// 72. poly
		MVC_NumberBox(models[72], gui[\controlsView],Rect(588, 364, 26, 18), gui[\boxTheme])
			.rounded_(true);

		// others   ////////////////////////////////////////////////////////////////////

		// 32.Filer glide
		MVC_MyKnob3(models[32],gui[\controlsView],Rect(321, 354, 26, 23),gui[\knob1Theme ]);
		
		gui[\eqView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(214, 362, 93-5, 23))
			.color_(\background, Color(1,1,1,0.4) )
			.color_(\border, Color(1,1,1,0.4) )
			.width_(6);

		MVC_StaticText(gui[\controlsView], Rect(248, 337, 25, 18),gui[\labelTheme])
			.string_("EQ");
		
		// 41.eq low boost
		MVC_OnOffView(models[41],gui[\eqView  ],Rect(0, 0, 40, 23),gui[\onOffTheme1]);
		
		// 34.EQ Boost High
		MVC_OnOffView(models[34],gui[\eqView  ],Rect(48, 0, 40, 23),gui[\onOffTheme1 ]);	
		// MIDI In ///////////////////////////////////////////////////////////////////////
		
		gui[\midiInView] = MVC_RoundedCompositeView(gui[\controlsView],Rect(365, 362, 206, 23))
			.color_(\background, Color(1,1,1,0.4) )
			.color_(\border, Color(1,1,1,0.4) )
			.width_(6);
		
		MVC_StaticText(gui[\controlsView], Rect(365,336, 81, 18),gui[\labelTheme])
			.string_("MIDI In");
		
		// 55. MIDI NoteOn > Filter Gate
		MVC_OnOffView(models[55],gui[\midiInView],Rect(166, 0, 41, 23),gui[\onOffTheme1 ]);
			
		// 35.MIDI NoteOn > ADSR Gate
		MVC_OnOffView(models[35],gui[\midiInView  ],Rect(72, 0, 47, 23),gui[\onOffTheme1 ]);

		// 38.MIDI NoteOn > Latch
		MVC_OnOffView(models[38],gui[\midiInView  ],Rect(15, 0, 52, 23),gui[\onOffTheme2 ]);

		// 73. MIDI NoteOn Velocity > Velocity
		MVC_OnOffView(models[73],gui[\midiInView  ],Rect(123, 0, 37, 23),gui[\onOffTheme1 ]);
	
		// midi In lamp
		gui[\midiInLamp]=MVC_PipeLampView(gui[\midiInView],Rect(0, 6, 10, 10))
			.color_(\on,Color.red);
	
		//////////////////////////////////////////////////////////////////////
			
		// 0.solo
		MVC_OnOffView(models[0],gui[\scrollView]     ,Rect( 234,6,19,19),gui[\onOffTheme1])
			.color_(\on,Color.red);
		// 1.on/off
		MVC_OnOffView(models[1],gui[\scrollView]     ,Rect( 210,6,19,19),gui[\onOffTheme1])
			.color_(\on,Color.green);
		
		// 11.steps per octave
		MVC_PopUpMenu3(models[11],gui[\seqView] ,Rect(607, 293, 48, 17), gui[\menuTheme2  ]);
		// 12.SPO root
		MVC_NumberBox(models[12],gui[\seqView] ,Rect(618, 345, 26, 17),gui[\boxTheme   ]);

		// 29.output channels
		MVC_PopUpMenu3(models[29],gui[\scrollView]    ,Rect(619,6,70,18),gui[\menuTheme2  ]);
		// 30.send channels
		MVC_PopUpMenu3(models[30],gui[\scrollView]    ,Rect(619-75,6,70,18),gui[\menuTheme2]);
		
		// other GUI items ////////////////////////////////////////////////////////////////////

		// MIDI Settings
 		MVC_FlatButton(gui[\scrollView],Rect(261, 6, 43, 19),"MIDI")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(6/11,29/65,42/83))
			.color_(\down,Color(6/11,29/65,42/83)/2)
			.color_(\string,Color.white)
			//.resize_(9)
			.action_{ this.createMIDIInModelWindow(window,low:27,high:28,
				colors:(border1:Color(0,1/103,9/77,65/77), border2:background1)
			)};
		
		// MIDI Controls
		MVC_FlatButton(gui[\scrollView],Rect(311, 6, 43, 19),"Cntrl")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(6/11,29/65,42/83))
			.color_(\down,Color(6/11,29/65,42/83)/2)
			.color_(\string,Color.white)
			//.resize_(9)
			.action_{ LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front };
			
		// 80.network keyboard
		MVC_OnOffView(models[80], gui[\seqView], Rect(637, 377, 32, 18), gui[\onOffTheme1]);
				
		gui[\keyboardOuterView] = MVC_CompositeView(gui[\scrollView],Rect(17,444,672,90));	
		toFrontAction={ gui[\keyboardView].focus};
		
		// the keyboard
		gui[\keyboardView]=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0,0,672,90),6,12)
			.useSelect_(true)
			.pipeFunc_{|pipe|
				if (p[80].isTrue) {
					api.sendOD(\netPipeIn, pipe.kind, pipe.note, pipe.velocity)}; // and network
			}
			//.stopGUIUpdate_(true)
			.keyboardColor_(Color.blue)
			.keyDownAction_{|note|	
				lastKeyboardNote=note;
				this.noteOn_Key(note,100/127);
				this.polyNoteOn(note,100/127);
				pianoRoll.noteOn(note,100/127);
			}
			.keyUpAction_{|note|
				this.noteOff_Key(note,100/127);
				this.polyNoteOff(note,100/127);
				pianoRoll.noteOff(note,100/127);
			}
			.keyTrackAction_{|note|
				this.noteOn_Key(note,100/127);
				this.noteOff_Key(lastKeyboardNote,100/127);
				this.polyNoteOn(note,100/127);
				this.polyNoteOff(lastKeyboardNote,100/127);
				pianoRoll.noteOn(note,100/127);
				pianoRoll.noteOff(lastKeyboardNote,100/127);
				lastKeyboardNote=note;
			};
			
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],(178-32+215)@(6),77,				background1,
				Color.black,
				background1,
				background1, // background
				Color.black
		);
		this.attachActionsToPresetGUI;	
		
		// sequencers ///////////////////////////////////////////////////////////////////////
		
		MVC_StaticText(gui[\seqView],Rect(592, 5, 65, 16))
				.align_(\center)
				.font_(Font("Helvetica",12))
				.string_("R   Sp  St");
		
		MVC_PlainSquare(gui[\seqView],Rect(12, 18, 576, 3)).color_(\off,Color(0,0,0,0.2));
		
		// noteOn
		sequencers[0].createButtonWidgets(gui[\seqView], Rect(18, 15, 652, 25));
		// noteOff
		sequencers[1].createButtonWidgets(gui[\seqView], Rect(18, 35, 652, 25),
			colors: ( \background:Color(0,0,0,0.8), \on:Color(61/128,47/95,43/75),
							\border:Color(0.5,0.66,1,1), \string:Color.white,
							\rulerOn: Color.white, \rulerBackground : Color (1,1,1,0.4),
							\position : Color.white, \pinOn : Color.red)
		);
		// accent
		sequencers[2].createButtonWidgets(gui[\seqView], Rect(18, 55, 652, 25),
			colors: ( \background:Color(0,0,0,0.8), \on:Color(61/128,47/95,43/75),
							\border:Color(0.5,0.66,1,1), \string:Color.white,
							\rulerOn: Color.white, \rulerBackground : Color (1,1,1,0.4),
							\position : Color.white, \pinOn : Color(0.5,0.5,1) )
		);
		
		MVC_PlainSquare(gui[\seqView],Rect(12, 87, 643, 3)).color_(\off,Color(0,0,0,0.2));
		
		// note seq
		sequencers[3].createSmoothWidgets(gui[\seqView], Rect(18, 109-20+7, 652, 190),
			writeModel:writeIndexModel );
		
		// slide
		sequencers[4].createButtonWidgets(gui[\seqView], Rect(18,75+174+7, 652, 25),
			colors: ( \background:Color(0,0,0,0.8), \on:Color(61/128,47/95,43/75),
							\border:Color(0.5,0.66,1,1), \string:Color.white,
							\rulerOn: Color.white, \rulerBackground : Color (1,1,1,0.4),
							\position : Color.white, \pinOn : Color(0.5,0.5,1) ),
			controls: false
		);
		
		// velocity seq
		sequencers[5].createVelocityWidgets(gui[\seqView], Rect(18, 302+7, 652, 100),
			(\hilite:Color(0,0,1) , \on:Color(0,0.5,1)), 'inner', controls: false);
			
		// seq controls ////////////////////////////////////////////////////////////////////////
		
		// 40.adv counter
		MVC_OnOffView(models[40],gui[\seqView],Rect(613,175,35,20),gui[\onOffTheme1])
			.color_(\on,Color.green);
		
		// 60. Latch	
		gui[\write]=MVC_OnOffView(models[60],gui[\seqView],
						Rect(10, 102, 48, 20),gui[\onOffTheme1]);
			
		// write
		gui[\writeModel] = [isKeyboardWriting.binaryValue,\switch,midiControl,-2,"Write"].asModel			.action_{|me,val,latency,send|
				{
					isKeyboardWriting=val.isTrue;
					gui[\recordModel].value_(0);	
					
				}.defer; // this will be on the System clock so defer
			};
		
		gui[\write]=MVC_OnOffView(gui[\writeModel],
			gui[\seqView],Rect(608,133,46,20),gui[\onOffTheme1])
			.strings_("Write")
			.color_(\on,Color.orange);
			
		// record
		gui[\recordModel] = [this.isRecording.binaryValue,\switch,midiControl,-1,"Record"].asModel
			.action_{|me,val,latency,send|
				{
					gui[\writeModel].value_(0);
					isKeyboardWriting=false;
					
				}.defer;  // this will be on the System clock so defer
			};
			
		gui[\record]=MVC_OnOffView(gui[\recordModel],gui[\seqView],Rect(613,219,35,20),"Rec",
			gui[\onOffTheme1])
			.color_(\on,Color.red);
			
	}
	
	isRecording{ ^(gui[\recordModel].value.isTrue)&&(studio.isPlaying) }
	
} // end ////////////////////////////////////
