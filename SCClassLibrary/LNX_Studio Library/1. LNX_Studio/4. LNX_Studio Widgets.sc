
// the main studio, mixer & its widgets

+ LNX_Studio {

	// these get and set in the range 0-1, makes it easier to linear fades in dbs
	volume{ ^models[\volume].get }
	volume_{|val| models[\volume].set_(val) }
		
	peakLevel{ ^models[\peakLevel].value} // get the model value
	
	// set the model
	peakLevel_{|val| models[\peakLevel].valueAction_(val,0,true,false) }
	
	// fade in the master volume 
	fadeIn{|level,beats=128,resolution=1|
		var startVolume,x;
		
		beats = #[256, 128, 64][models[\fadeSpeed].value];
		
		level = level ? (this.peakLevel) ? 1;
		startVolume = this.volume;
		tasks[\fadeTask].stop;
		tasks[\fadeTask]={
			(beats*resolution).do{|n|			
				(absTime*24/8/resolution).wait;
				x=(n+1)/beats/resolution;
				this.volume_(
					(level*x)+(startVolume*(1-x))
				);
			}
		}.fork(AppClock); // needs to go back to SystemClock when fixed
	}
	
	// fade out the master volume
	fadeOut{|beats=128,resolution=1|
		var startVolume;
		
		beats = #[256, 128, 64 ][models[\fadeSpeed].value];
		
		startVolume = this.volume;
		tasks[\fadeTask].stop;	
		tasks[\fadeTask]={
			(beats*resolution).do{|n|			
				(absTime*24/8/resolution).wait;
				this.volume_(
					startVolume*(beats*resolution-n-1)/(beats*resolution)
				)
			}
		}.fork(AppClock); // needs to go back to SystemClock when fixed
	}

	// create the mixer gui
	createMixerWindow{
		var bounds, width=1012- ScrollBars.addIfNone(7), height=494;
		
		bounds = this.savedWindowBounds ? Rect(osx,0,width,height);
		bounds = bounds.setExtent(width,height).moveBy(0,0);
		
		mixerWindow=MVC_Window("LNX_Studio",bounds, resizable: true)
			.userCanClose_(false)
			.minHeight_(height)
			.maxHeight_(height)
			.minWidth_(width)
			.toFrontAction_{|me| LNX_SplashScreen.close }
			.keyDownAction_{|me, char, modifiers, unicode, keycode|
				//if (keycode==49) {this.togglePlay}; // space
			}
			.color_(\background,Color(4/77,2/103,0,65/77));		
	}
	
	// flash the border of the mixer window
	flashWindow{|color,duration=2|
		{mixerWindow.color_(\background,color ? (Color.white))}.defer;
		{mixerWindow.color_(\background,Color(3/77,1/103,0,65/77))}.defer(duration);
	}
	
	flashServerIcon{
		if (mixerGUI.notNil and: {mixerGUI[\serverGUI].notNil}) {mixerGUI[\serverGUI].flash};
	}
	
	stopFlashServerIcon{
		if (mixerGUI.notNil and: {mixerGUI[\serverGUI].notNil}) {mixerGUI[\serverGUI].flashStop};
	}
	
	// text that says "Instruments"... on empty song
	updateMixerInfoText{
		mixerGUI[\instInfoText].hidden_(insts.mixerInstruments.size>0);
		mixerGUI[\fxInfoText].hidden_(insts.effects.size>0);
		mixerGUI[\midiInfoText].hidden_(insts.midi.size>0);
		mixerGUI[\everythingInfoText].hidden_(insts.size>0);
		if (insts.size<1) { 
			mixerGUI[\instInfoText].bounds_(
				Rect(0, 190, mixerGUI[\instScrollView].bounds.width , 43));
			mixerGUI[\everythingInfoText].bounds_(
				Rect(20, 190, mixerGUI[\presetTab].bounds.width-40 , 43));
		};
	}
	
	// and create the the widgets
	createMixerWidgets{
		
		var sv;
		
		insts.addDependant{ this.updateMixerInfoText }; // to update infoText
		
		models[\volume].hackAction_{|model,button| if (button==1) { this.peakLevel_(model.get) }};
		
		mixerGUI = IdentityDictionary[];
		
		// themes
		mixerGUI[\buttonTheme2 ] = (\colors_ : (	\up    : Color(6/11,42/83,29/65)*0.8,
										\down  : Color(6/11,42/83,29/65)/2,
					 					\string: Color.black),
							\rounded_ : true);	
							
		mixerGUI[\textTheme] = (\colors_ : (	\string    : Color(0.85,0.85,0.85)),
							\shadow_	: false,
							\penShadow_ : true,
							\align_	: \center,
							\font_	:Font("AvenirNext-Heavy",24));
		
		gui[\textTheme2] = (	\shadow_	: false,
							\font_	: Font("Helvetica", 10),
							\align	: \left,
							\colors_	: (	\string:	Color(46/77,46/79,72/145) ) );
							
		gui[\onOffTheme] = (	\font_	: Font("Helvetica",12),
							\colors_	: (	\on:		Color.red,
										\off:	Color(46/77,46/79,72/145)/1.5,
										\string:	Color.black ) );
										
		gui[\syncTheme]=(	\orientation_  : \horiz,
						\resoultion_	 : 8,
						\visualRound_  : 0.1,
						\rounded_      : true,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\background : Color(0,0,0,0.15),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.black,
										\focus : Color(0,0,0,0)));
					
		// file menu 10
		MVC_PopUpMenu2(mixerWindow,Rect(7, 3, 74, 22))
			.items_(["  File","Open...","Add Song","Open Last Song",
						"-","Save ","Save As...",
						(this.isStandalone && LNX_Mode.isSafe).if("(","")++
						"Add Instrument to Library",
						"-",
						"Close Song","-","Network","Preferences..."])
			.action_{|me,val|
				switch (me.value.asInt)
				 {0}  {this.loadDialog		    }
				 {1}  {this.addDialog		    }
				 {2}  {this.quickLoad           }
				 {4}  {this.saveDialog		    }
				 {5}  {this.saveAsDialog        }
				 {6}  {this.guiSaveInstToLibrary}
				 {8}  {this.guiCloseStudio      }
				 {10} {network.guiConnect       }
				 {11} {this.preferences		    }
				;
			};
		
		// edit menu 10
		MVC_PopUpMenu2(mixerWindow,Rect(80, 3, 75, 22))
			.items_(["  Edit","Copy","Paste","Duplicate","-","Delete Instrument","-",
						"Edit MIDI Controls"])
			.action_{|me|
				switch (me.value.asInt)
				{0} {this.guiCopy		}
				{1} {this.guiPaste		}
				{2} {this.guiDuplicate	}
				{4} {this.guiDeleteInst	}
				{6} {LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front; }
			}
			.visible_(alwaysOnTop.not);
			
		// all or 1 window		
		MVC_BinaryCircleView(models[\show1],mixerWindow,Rect(160, 4, 20, 20))
			.strings_(["1","A"])
			.font_(Font("Helvetica-Bold",14))
			.colors_((\upOn:Color(0.9,0.7,0), \upOff:Color(0,0.6,0), \stringOn:Color.black,
				\stringOff:Color.black, \downOn:Color(0.5,0.5,0), \downOff:Color(0,0.2,0)));
			
		// close all inst
		MVC_BinaryCircleView(models[\showNone],mixerWindow,Rect(182, 4, 20, 20),"X")
			.font_(Font("Helvetica-Bold",14))
			.colors_((\upOn:Color(0.9,0.075,0.075),\upOff:Color(0.8,0.15,0.15),
			\stringOn:Color.black, \stringOff:Color(0.6,0.15,0.15),
			\downOn:Color(0.6,0.1,0.1), \downOff:Color(0.6,0.1,0.1)));
			
		// internal or extrnal clock
		MVC_OnOffFlatView(models[\extClock],mixerWindow,Rect(218, 4, 29, 19),gui[\onOffTheme])
			.font_(Font("Helvetica-Bold", 12))
			.color_(\on,Color(0.85,0.85,0.2))
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2);

		// tempo (bpm)
		MVC_NumberBox(models[\tempo],mixerWindow, Rect(250, 5, 38, 17))
			.rounded_(false)
			.visualRound_(0.1)
			.font_(Font("Helvetica", 11))
			.color_(\focus,Color.grey(alpha:0))
			.color_(\string,Color.white)
			.color_(\typing,Color.yellow)
			.color_(\background,Color(46/77,46/79,72/145)/1.5);
				
		// tap tempo
		MVC_OnOffFlatView(models[\tap],mixerWindow,Rect(292,4,32,19),gui[\onOffTheme])
			.mouseMode_(\tap)
			.darkerWhenPressed_(false)
			.font_(Font("Helvetica-Bold", 12))
			.color_(\on,Color(0.85,0.85,0.2))
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2);
	
		// record
		MVC_OnOffFlatView(models[\record],mixerWindow,Rect(339, 4, 35, 19),gui[\onOffTheme])
			.strings_(["Rec"])
			.font_(Font("Helvetica-Bold", 12))
			.color_(\on,Color(1,0,0))
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2);

		// play
		MVC_OnOffFlatView(models[\play],mixerWindow,Rect(453, 3, 19, 19))
			.strings_(["play"])
			.insetBy_(-1)
			.color_(\on,Color.green)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/1.7)
			.mode_(\play);

		// stop
		MVC_OnOffFlatView(models[\stop],mixerWindow,Rect(475, 3, 19, 19))
			.strings_(["stop"])
			.darkerWhenPressed_(false)
			.insetBy_(0)
			.color_(\on,Color.yellow)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/1.7)
			.mode_(\stop)
			.mouseMode_(\button);

		// forwards
		MVC_OnOffFlatView(models[\fowards], mixerWindow, Rect(498, 3, 19, 19))
			.mode_(\icon)
			.darkerWhenPressed_(false)
			.mouseMode_(\button)
			.insetBy_(-2)
			.color_(\on,Color.green)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/1.7)
			.strings_(["forward"]);
					
		// rewind
		MVC_OnOffFlatView(models[\rewind], mixerWindow,Rect(431, 3, 19, 19))
			.mode_(\icon)
			.darkerWhenPressed_(false)
			.mouseMode_(\button)
			.insetBy_(-2)
			.color_(\on,Color.green)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/1.7)
			.strings_(["rewind"]);	
						
		// auto On (play)
		mixerGUI[\autoPlay] = MVC_OnOffFlatView(models[\autoOn],mixerWindow,Rect(722, 468, 19, 19))
			.resize_(3)
			.color_(\on,Color.white)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2)
			.strings_(["A"]);
			
		// auto record
		mixerGUI[\autoRec] = MVC_OnOffFlatView(models[\autoRecord],mixerWindow,Rect(722, 445, 19, 19))
			.resize_(3)
			.color_(\on,Color.red)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2)
			.mode_(\icon)
			.strings_([\record]);	


		// server stats: cpu
		MVC_StaticText(mixerWindow,Rect(635, 6, 24, 15),gui[\textTheme2])
			.resize_(3)
			.string_("CPU:")
			.excludeFromVerbose_(true);
			
		mixerGUI[\cpu]=MVC_StaticText(mixerWindow,Rect(661, 6, 30, 15),gui[\textTheme2])
			.resize_(3)
			.string_("-")
			.excludeFromVerbose_(true);		

		mixerGUI[\time]=MVC_StaticText(mixerWindow,Rect(518, 2, 52, 23),gui[\textTheme2])
			.font_(Font("Helvetica",14))
			.align_(\center)
			.string_("0:00")
			.excludeFromVerbose_(true);
			
		mixerGUI[\beat]=MVC_StaticText(mixerWindow,Rect(376, 2, 52, 23),gui[\textTheme2])
			.font_(Font("Helvetica",14))
			.align_(\center)
			.string_("1.1")
			.excludeFromVerbose_(true);
	
		// server on/off view
		mixerGUI[\serverGUI]=MVC_FlatButton2(models[\serverRunning],mixerWindow,
								Rect(571, 4, 26, 19))
			.resize_(3)
			.font_(Font("Helvetica-Bold",11))
			.color_(\off,Color.black)
			.color_(\on,Color.green)
			.color_(\background,Color(46/77,46/79,72/145)/2) ;
			
		// net on/off view
		MVC_FlatButton2(models[\network],mixerWindow,Rect(601, 4, 26, 19))
			.resize_(3)
			.font_(Font("Helvetica-Bold",11))
			.color_(\off,Color.black)
			.color_(\on,Color.green)
			.color_(\background,Color(46/77,46/79,72/145)/2) ;

		gui[\multiTheme ]=(\font_:Font("Helvetica-Bold",12),
			\states_ : [
				["S"   ,Color(1, 0,   0)/1.15   ,Color.black,Color.grey/3,Color.grey/2],
				["M"    ,Color(1, 0.5, 0)/1.15,Color.black,Color.grey/2,Color.grey/3],
				["F"   ,Color(0.5, 1,  0)/1.15,Color.black,Color.grey/4,Color.grey/2]]);
				
			
		MVC_MultiOnOffView(models[\fadeSpeed], mixerWindow,Rect(696,4,19,19),gui[\multiTheme ])
			.color_(\background,Color(46/77,46/79,72/145)/4)
			.resize_(3);
								
		this.createLibraryScrollView;

// *******************

		mixerGUI[\popProgramsScrollView] = MVC_ScrollView(mixerWindow,Rect(220, 33, 73, 450))
			.autoScrolls_(true)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(true)
			.hasBorder_(false)
			.visible_(false)
			.color_(\background,Color(0.8,0.8,0.8));

		mixerGUI[\masterTabs]=MVC_TabbedView(mixerWindow,Rect(220, 33, 514, 450),
			scroll:[true,true,false], offset:(6@(279)))
			.action_{|me|}
			.labels_(["Mixer","Prog","Auto"])
			.resize_(2)
			.font_(Font("Helvetica", 14))
			.tabPosition_(\right)
			.unfocusedColors_( Color(6/11,42/83,29/65)/2 ! 3)
			.labelColors_(   Color(6/11,42/83,29/65) !3)
			.backgrounds_(  [Color(0.8,0.8,0.8),Color(0.8,0.8,0.8),Color(0.8,0.8,0.8)] )
			.tabCurve_(5)
			.tabWidth_([ 45, 41, 38 ])
			.tabHeight_(22)
			.followEdges_(true)
			.adjustments_([nil, Rect(83-10,0,-83+10,0)])
			.value_(0)
			.resizeAction_{
				if (insts.size<1) { 
					mixerGUI[\instInfoText].bounds_(
						Rect(0, 190, mixerGUI[\instScrollView].bounds.width , 43));
					mixerGUI[\everythingInfoText].bounds_(
						Rect(20, 190, mixerGUI[\presetTab].bounds.width-40 , 43));
				};
			};
		
		mixerGUI[\masterTabs].focusActions_([{},{
			mixerGUI[\popProgramsScrollView].visible_(true);
		},{}]);
		mixerGUI[\masterTabs].unfocusActions_([{},{ 
			mixerGUI[\popProgramsScrollView].visible_(false);
		},{}]);
		
		// mixer tab
		
		mixerGUI[\instScrollView] = mixerGUI[\masterTabs].mvcTab(0)
			.hasVerticalScroller_(false);
		
		MVC_RoundBounds(mixerWindow,Rect(220, 33, 492, 450))
			.setResize(1,2,3,2)
			.width_(6)
			.color_(\background, Color(6/11,42/83,29/65));
		
		mixerGUI[\instInfoText] = MVC_StaticText(mixerGUI[\instScrollView], mixerGUI[\textTheme],
			Rect(162, 190, 174, 43)).string_("Instruments");
			
		// the fx scroll view
		mixerGUI[\fxScrollView] = MVC_RoundedScrollView (mixerWindow,
				Rect(731, 10, 177 - ScrollBars.addIfNone(7), 288))
			.hasBorder_(false)
			.resizeList_([3,3,3,3,3]) //  0:view 1:left 2:top 3:right 4:bottom
			.autoScrolls_(true)
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border,Color(6/11,42/83,29/65))
			.hasVerticalScroller_(true);
					
		mixerGUI[\fxInfoText] = MVC_StaticText(mixerGUI[\fxScrollView], mixerGUI[\textTheme],
						Rect(33, 110, 102, 47))
			.font_(Font("Helvetica-Bold",20))
			.string_("Effects");	
		
		// the midi scroll view	
		mixerGUI[\midiScrollView] = MVC_RoundedScrollView (mixerWindow,
				Rect(753, 316, 247 - ScrollBars.addIfNone(7), 167))
			.hasBorder_(false)
			.resizeList_([3,3,3,3,3]) //  0:view 1:left 2:top 3:right 4:bottom
			.autoScrolls_(true)
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border,Color(6/11,42/83,29/65))
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false);
			
		mixerGUI[\midiInfoText] = MVC_StaticText(mixerGUI[\midiScrollView], mixerGUI[\textTheme],
						Rect(65, 58, 102, 47))
			.font_(Font("Helvetica-Bold",20))
			.string_("MIDI");
		
		// the master levels scroll view
		mixerGUI[\masterLevelsScrollView] = MVC_RoundedComView(mixerWindow, 
								Rect(927- ScrollBars.addIfNone(7), 10, 73, 288))
			.hasBorder_(false)
			.resizeList_([3,3,3,3,3]) //  0:view 1:left 2:top 3:right 4:bottom
			.autoScrolls_(true)
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border,Color(6/11,42/83,29/65))
			.hasVerticalScroller_(false);
		
		sv = mixerGUI[\masterLevelsScrollView];
		
		// levels
		MVC_FlatDisplay(models[\peakOutL],sv,Rect(3, 23, 7, 192-7));
		MVC_FlatDisplay(models[\peakOutR],sv,Rect(12, 23, 7, 192-7));
		MVC_Scale(sv,Rect(10, 23, 2, 192-7));
			
		// peakLevel
		MVC_PeakLevel(sv,models[\peakLevel],Rect(56, 27, 13, 184-7));		
		// volume
		MVC_SmoothSlider(models[\volume],sv,Rect(22, 23, 33, 192-7))
			.labelShadow_(false)
			.thumbSizeAsRatio_(0.18,8)
			.numberFunc_(\float2)
			.showNumberBox_(true)
			.numberFont_(Font("Helvetica",10))
			.color_(\label,Color.black)
			.color_(\knob,Color(1,1,1))
			.color_(\hilite,Color(0,0,0,0.5))
			.color_(\numberUp,Color.black)
			.color_(\numberDown,Color.white);
				
		// mute
		MVC_OnOffView(models[\mute],sv, Rect(21, 250-7, 34, 20))
				.permanentStrings_(["Mute"])
				.font_(Font("Helvetica-Bold",11))
				.color_(\on,Color.red)
				.color_(\off, Color(0.3686, 0.3373, 0.2412,0.5) )
				.rounded_(true)
				.canFocus_(false);
				

		MVC_NumberBox(models[\preAmp], sv,Rect(12, 268, 50, 16),  gui[\syncTheme])
			.postfix_(" db")
			.color_(\label,Color.white);
			
			
		// fadeIn
		MVC_FlatButton(sv,Rect(25, 3, 27, 17), mixerGUI[\buttonTheme2 ] ,"up")
			.mode_(\icon)
			.action_{ this.fadeIn };
			
		// fadeOut
		MVC_FlatButton(sv,Rect(25, 222, 27, 17), mixerGUI[\buttonTheme2 ] ,"down")
			.mode_(\icon)
			.action_{ this.fadeOut };
			
		// add the automation widgets				
		MVC_Automation.createWidgets(mixerGUI[\masterTabs].mvcTab(2));
			
		mixerGUI[\presetTab] = mixerGUI[\masterTabs].mvcTab(1);
			
		// add the preset widgets	
		LNX_POP.createWidgets( mixerGUI[\popProgramsScrollView], mixerGUI[\presetTab]);
		
		mixerGUI[\everythingInfoText] = MVC_StaticText(mixerGUI[\presetTab], mixerGUI[\textTheme],
			Rect(20, 190, 369, 43)).string_("Everything");
						
	}
	
	
	// align the instrument gui including any gaps from delete
	alignMixerGUI{
		var y;
		// all instruments
		insts.mixerInstruments.do{|inst,y|
			var id=inst.id;
			y=y*70;
			mixerGUI[id][\scrollView].left_(y);
		};	
		// all effects
		insts.effects.do{|inst,y|
			var id=inst.id;
			y=y*62;
			mixerGUI[id][\scrollView].top_(y);
		};
		// and all midi
		insts.midi.do{|inst,y|
			var id=inst.id;
			y=y*23;
			mixerGUI[id][\scrollView].top_(y);
		};
		// and insts
		insts.visualOrder.do{|inst,y|
			var id=inst.id;
			y=y*70;
			mixerGUI[id][\scrollViewPOP].left_(y);
		};	
	}
	
	// create the widgets for the instrument in the mixer
	
	createMixerInstWidgets{|inst|

		var id=inst.id;
		var i;
		var y;
		var moveDict;
		var sv, hasMoved=false;
		
		mixerGUI[id]=IdentityDictionary[];
		
		// themes
		
		mixerGUI[\knobTheme1]=(\colors_		: (\on : Color.orange, 
									   \numberUp:Color.black,
									   \numberDown:Color.white,
									   \disabled:Color.black),
										\numberFont_  : Font("Helvetica",11));
										
		mixerGUI[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));
		
		mixerGUI[\onOffTheme2]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color(0.25,1,0.25),
						 			  \off : Color(0.4,0.4,0.4)));
				 			  
		mixerGUI[\menuTheme2]=( \font_		: Font("Arial", 9),
				\colors_      : (\background:Color(0.1,0.1,0.1,0.1),\string:Color.black,
							   \focus:Color.clear));
							  
		gui[\syncTheme]=(	\orientation_  : \horiz,
						\resoultion_	 : 7.5,
						\visualRound_  : 0.001,
						\rounded_      : true,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\background : Color(0,0,0,0.15),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.black,
										\focus : Color(0,0,0,0)));
		
		//gui[\presetTab]; /////////////////// *************************************
		
		// for LNX_POP
		
		i=insts.allInstX(id);
		y = (i*70);
			
		mixerGUI[id][\scrollViewPOP] = MVC_CompositeView(mixerGUI[\presetTab],
									Rect(y,0,70,21*20+30), hasBorder:false);
		
		sv=mixerGUI[id][\scrollViewPOP];
		
		// divider
		mixerGUI[id][\divider]=MVC_PlainSquare(sv, Rect(71,0,1,79))
			.color_(\on,Color(0,0,0,0.3))
			.color_(\off,Color(0,0,0,0.3));
	
		// number
		mixerGUI[id][\instNoPOP] = MVC_StaticText(sv,
											inst.instNoModel,Rect(6,3,62,21))
			.hasBorder_(true)
			.active_(false)
			.clipChars_(true)
			.color_(\background, inst.mixerColor)
			.color_(\string,Color.black)
			.shadow_(false)
			.align_(\center)
			.font_(Font("Helvetica-Bold",14))
			.mouseDownAction_{|me,x,y,modifiers|
				hasMoved=false;
				moveDict=IdentityDictionary[];
				moveDict[\id]=id;
				moveDict[\startPos]=insts.getY(id);
			}
			.mouseMoveAction_{|me,x,y,modifiers|
				if (x<0) {x=x-70}; // because -0.9.asInt=0
				x=(x/70).asInt;
				if (x!=0) {hasMoved=true};
				this.move(id,moveDict[\startPos]+x);
				moveDict[\startPos]=(moveDict[\startPos]+x).clip(0,insts.size-1);
				mixerWindow.front;
			}
			.mouseUpAction_{|me,x,y,modifiers|
				if (hasMoved.not) {this.selectInst(id) }
			};
		
		MVC_FuncAdaptor(inst.instNoModel).func_{|me,value|
			mixerGUI[id][\instNoPOP].string_((value+1).asString)
		}.freshAdaptor;
				
		// name
		MVC_Text(sv,inst.nameModel,Rect(6,23,62,16))
			.hasBorder_(true)
			.canEdit_(true)
			.clipChars_(true)
			.color_(\background, inst.mixerColor)
			.color_(\string,Color.black)
			.color_(\cursor, Color.orange)
			.shadow_(false)
			.align_(\center)
			.font_(Font("Helvetica",9))
			.color_(\focus,Color(1,1,1,0.66))
			.mouseDownAction_{|me,x,y,modifiers|
				moveDict=IdentityDictionary[];
				moveDict[\id]=id;
				moveDict[\startPos]=insts.getY(id);
			}
			.mouseMoveAction_{|me,x,y,modifiers|
				if (x<0) {x=x-70}; // because -0.9.asInt=0
				x=(x/70).asInt;
				this.move(id,moveDict[\startPos]+x);
				moveDict[\startPos]=(moveDict[\startPos]+x).clip(0,insts.size-1);
			};
			
		if (inst.canTurnOnOff) {
				
			if (inst.fxOnOffModel.notNil) {
				// on/off
				MVC_OnOffView(inst.fxOnOffModel, sv, Rect(27,49, 20, 20), mixerGUI[\onOffTheme2])
					.rounded_(true)
					.canFocus_(false);
			}{
			
				// on/off
				MVC_OnOffView( inst.onOffModel, sv, Rect(13,49, 20, 20),mixerGUI[\onOffTheme2])
					//.permanentStrings_(["On"])
					.rounded_(true)
					.canFocus_(false);
							
				// solo
				MVC_OnOffView(inst.soloModel , sv, Rect(41,49, 20, 20),mixerGUI[\soloTheme])
					.rounded_(true)
					.canFocus_(false);
			}
		
		};
			
		inst.createPOPWidgets(sv,mixerGUI[id]);
		
		//////////////////////	^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^				   					  
		if (inst.isMixerInstrument) {
			
			i=insts.mixerInstY(id);
			y = (i*70);
			
			mixerGUI[id][\scrollView] = MVC_CompositeView(mixerGUI[\instScrollView],
										Rect(0+y,0,72,450-12), hasBorder:false);
			
			sv=mixerGUI[id][\scrollView];
		
			// number
			mixerGUI[id][\instNo] = MVC_StaticText(sv,
												inst.instNoModel,Rect(6,3,62,21))
				.hasBorder_(true)
				.active_(false)
				.clipChars_(true)
				.color_(\background, inst.mixerColor)
				.color_(\string,Color.black)
				.shadow_(false)
				.align_(\center)
				.font_(Font("Helvetica-Bold",14))
				.mouseDownAction_{|me,x,y,modifiers|
					hasMoved=false;
					moveDict=IdentityDictionary[];
					moveDict[\id]=id;
					moveDict[\startPos]=insts.getY(id);
				}
				.mouseMoveAction_{|me,x,y,modifiers|
					if (x<0) {x=x-70}; // because -0.9.asInt=0
					x=(x/70).asInt;
					if (x!=0) {hasMoved=true};
					this.move(id,moveDict[\startPos]+x);
					moveDict[\startPos]=(moveDict[\startPos]+x).clip(0,insts.size-1);
					mixerWindow.front;
				}
				.mouseUpAction_{|me,x,y,modifiers|
					if (hasMoved.not) {this.selectInst(id) }
				};
				
			MVC_FuncAdaptor(inst.instNoModel).func_{|me,value|
					mixerGUI[id][\instNo].string_((value+1).asString)
				}.freshAdaptor;
		
			// name
			MVC_Text(sv,inst.nameModel,Rect(6,23,62,16))
				.hasBorder_(true)
				.canEdit_(true)
				.clipChars_(true)
				.color_(\background, inst.mixerColor)
				.color_(\string,Color.black)
				.color_(\cursor, Color.orange)
				.shadow_(false)
				.align_(\center)
				.font_(Font("Helvetica",9))
				.color_(\focus,Color(1,1,1,0.66))
				.mouseDownAction_{|me,x,y,modifiers|
					moveDict=IdentityDictionary[];
					moveDict[\id]=id;
					moveDict[\startPos]=insts.getY(id);
				}
				.mouseMoveAction_{|me,x,y,modifiers|
					if (x<0) {x=x-70}; // because -0.9.asInt=0
					x=(x/70).asInt;
					this.move(id,moveDict[\startPos]+x);
					moveDict[\startPos]=(moveDict[\startPos]+x).clip(0,insts.size-1);
				}
				;
								
			// levels
			MVC_FlatDisplay(inst.peakLeftModel,sv,Rect(5, 128, 6, 180));
			MVC_FlatDisplay(inst.peakRightModel,sv,Rect(13, 128, 6, 180));
			MVC_Scale(sv,Rect(11, 128, 2, 180));
				
			// volume
			MVC_SmoothSlider(sv,inst.volumeModel,Rect(22, 128, 31, 180))
				.label_(nil)
				.thumbSizeAsRatio_(0.18,8)
				.numberFunc_(\float2)
				.showNumberBox_(true)
				.numberFont_(Font("Helvetica",10))
				.color_(\knob,Color(1,1,1))
				.color_(\hilite,Color(0,0,0,0.5))
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color.white);
				
			// peakLevel
			MVC_PeakLevel(sv,inst.peakModel,Rect(54, 132, 13, 172));
			
			// fadeIn
			MVC_FlatButton(sv,Rect(24, 107, 27, 17), mixerGUI[\buttonTheme2 ] ,"up")
				.mode_(\icon)
				.action_{ inst.fadeIn };
				
			// fadeOut
			MVC_FlatButton(sv,Rect(24, 290+30, 27, 17), mixerGUI[\buttonTheme2 ] ,"down")
				.mode_(\icon)
				.action_{ inst.fadeOut };
			
//			// pan text
//			MVC_StaticText(sv,Rect(3, 344, 22, 10))
//				.string_("Pan")
//				.shadow_(false)
//				.penShadow_(false)
//				.font_(Font("Helvetica",10))
//				.color_(\string,Color.black)
//				.excludeFromVerbose_(true);
			
			// pan
			MVC_MyKnob3(inst.panModel,sv,Rect(25, 340 + ScrollBars.addIfNone(2), 25, 25),mixerGUI[\knobTheme1])
				.numberFont_(Font("Helvetica",10))
				.numberFunc_(\pan)
				.label_(nil)
				.showNumberBox_(true);
			
			if (inst.sendAmpModel.notNil) {

				// fx
				MVC_StaticText(sv,Rect(3, 46, 17, 10))
					.string_("FX")
					.shadow_(false)
					.penShadow_(false)
					.font_(Font("Helvetica",10))
					.color_(\string,Color.black)
					.excludeFromVerbose_(true);

				// send
				MVC_MyKnob3(inst.sendAmpModel,sv,Rect(25, 46, 25, 25),
					mixerGUI[\knobTheme1])
					.numberFont_(Font("Helvetica",10))
					.label_(nil)
					.showNumberBox_(true);	
					
				// send channel	
				MVC_PopUpMenu3(inst.sendChModel,sv, Rect(5, 85, 64, 16),mixerGUI[\menuTheme2])
					.label_(nil)
					.color_(\background, inst.mixerColor);
							
			};
	
			// on/off
			MVC_OnOffView(inst.onOffModel, sv, Rect(13, 378+ScrollBars.addIfNone(3), 20, 20),mixerGUI[\onOffTheme2])
				//.permanentStrings_(["On"])
				.rounded_(true)
				.canFocus_(false);
					
			// solo
			MVC_OnOffView(inst.soloModel , sv, Rect(42, 378+ScrollBars.addIfNone(3), 20, 20),mixerGUI[\soloTheme])
				.rounded_(true)
				.canFocus_(false);
		
			// out channel	
			MVC_PopUpMenu3(inst.outChModel,sv, Rect(5, 401+ScrollBars.addIfNone(5), 64, 16),
				mixerGUI[\menuTheme2])
				.label_(nil)
				.color_(\background, inst.mixerColor);
			
			// sync	
			if (inst.syncModel.notNil) {
				MVC_NumberBox(inst.syncModel, sv,Rect(12, 422+ScrollBars.addIfNone(7), 50, 16),  gui[\syncTheme])
					.postfix_(" s")
					.color_(\label,Color.white);
			};
							
			// divider
			mixerGUI[id][\divider]=MVC_PlainSquare(sv, Rect(71,0,1,450))
				.color_(\on,Color(0,0,0,0.3))
				.color_(\off,Color(0,0,0,0.3));

			// divider
			mixerGUI[id][\divider2]=MVC_PlainSquare(sv, Rect(72,0,1,450))
				.color_(\on,Color(1,1,1,0.4))
				.color_(\off,Color(1,1,1,0.4));
			
		};

		if (inst.isFX) {
			
			i=insts.mixerFXY(id);
			y = (i*62);
		
			mixerGUI[id][\scrollView] = MVC_CompositeView(mixerGUI[\fxScrollView],
										Rect(0,0+y,170,62), hasBorder:false);
			
			sv=mixerGUI[id][\scrollView];

			// number
			mixerGUI[id][\instNo] = MVC_StaticText(sv,
												inst.instNoModel,Rect(5,3,34,17))
				.hasBorder_(true)
				.active_(false)
				.clipChars_(true)
				.rightIfIcon_(true)
				.color_(\background, inst.mixerColor)
				.color_(\string,Color.black)
				.shadow_(false)
				.align_(\center)
				.font_(Font("Helvetica-Bold",14))
				.mouseDownAction_{|me,x,y,modifiers|
					hasMoved=false;
					moveDict=IdentityDictionary[];
					moveDict[\id]=id;
					moveDict[\startPos]=insts.getY(id);
				}
				.mouseMoveAction_{|me,x,y,modifiers|
					if (y<0) {y=y-62}; // because -0.9.asInt=0
					y=(y/62).asInt;
					if (y!=0) {hasMoved=true};
					this.move(id,moveDict[\startPos]+y);
					moveDict[\startPos]=(moveDict[\startPos]+y).clip(0,insts.size-1);
					mixerWindow.front;
				}
				.mouseUpAction_{|me,x,y,modifiers|
					if (hasMoved.not) {this.selectInst(id) }
				};
				
			MVC_FuncAdaptor(inst.instNoModel).func_{|me,value|
					mixerGUI[id][\instNo].string_((value+1).asString)
				}.freshAdaptor;
		
			// name
			MVC_Text(sv,inst.nameModel,Rect(38,3,111,17))
				.hasBorder_(true)
				.canEdit_(true)
				.clipChars_(true)
				.color_(\background, inst.mixerColor)
				.color_(\string,Color.black)
				.color_(\cursor, Color.orange)
				.shadow_(false)
				.align_(\center)
				.font_(Font("Helvetica",9))
				.color_(\focus,Color(1,1,1,0.66))
				.mouseDownAction_{|me,x,y,modifiers|
					moveDict=IdentityDictionary[];
					moveDict[\id]=id;
					moveDict[\startPos]=insts.getY(id);
				}
				.mouseMoveAction_{|me,x,y,modifiers|
					if (y<0) {y=y-62}; // because -0.9.asInt=0
					y=(y/62).asInt;
					this.move(id,moveDict[\startPos]+y);
					moveDict[\startPos]=(moveDict[\startPos]+y).clip(0,insts.size-1);
				}
				;

			// in
			MVC_SmoothSlider(sv,inst.inModel,Rect(15,43,60,13))
				.orientation_(\horiz)
				.label_("In")
				.thumbSizeAsRatio_(0.05,0)
				.labelShadow_(false)
				.labelFont_(Font("Arial", 9))
				.showNumberBox_(false)
				.numberFont_(Font("Helvetica",10))
				.color_(\label,Color.black)
				.color_(\knob,Color(1,1,1,86/125))
				.color_(\hilite,Color(0,0,0,0.5))
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color.white);

			// out
			MVC_SmoothSlider(sv,inst.outModel,Rect(104,43,60,13))
				.orientation_(\horiz)
				.label_("Out")
				.thumbSizeAsRatio_(0.05,0)
				.labelShadow_(false)
				.labelFont_(Font("Arial", 9))
				.showNumberBox_(false)
				.numberFont_(Font("Helvetica",10))
				.color_(\label,Color.black)
				.color_(\knob,Color(1,1,1,86/125))
				.color_(\hilite,Color(0,0,0,0.5))
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color.white);

			if (inst.fxOnOffModel.notNil) {
				MVC_OnOffView(inst.fxOnOffModel, sv ,Rect(148,3,17,17))
					.permanentStrings_([\record])
					.mode_(\icon)
					.insetBy_(0.14)
					.color_(\border,Color(0,0,0,0.25), forceAdd:true)
					.color_(\background,Color.clear)
					.color_(\on, inst.mixerColor)
					.color_(\off, inst.mixerColor)
					.color_(\onDisabled, inst.mixerColor)
					.color_(\offDisabled, inst.mixerColor)
					.color_(\iconBackground,Color(0,0,0), forceAdd:true)
					.color_(\icon,Color(0.3,1,0.3))
					.color_(\iconOff,Color(0.5,0.5,0.5));
			};
			
			// in channel	
			mixerGUI[id][\in]=MVC_PopUpMenu3(inst.inChModel, sv, Rect(5, 23, 76, 16), 								mixerGUI[\menuTheme2])
				.label_(nil)
				.color_(\background, inst.mixerColor);

			// out channel	
			MVC_PopUpMenu3(inst.outChModel,sv, Rect(89, 23, 76, 16), 
								mixerGUI[\menuTheme2])
				.label_(nil)
				.color_(\background, inst.mixerColor);
	
			// divider
			mixerGUI[id][\divider]=MVC_PlainSquare(sv, Rect(0,59,170+7,1))
					.color_(\on,Color(0,0,0,0.3))
					.color_(\off,Color(0,0,0,0.3));
	
			// divider
			mixerGUI[id][\divider2]=MVC_PlainSquare(sv, Rect(0,60,170+7,1))
					.color_(\on,Color(1,1,1,0.4))
					.color_(\off,Color(1,1,1,0.4));	
			
		};
		
		if (inst.isMIDI) {
			
			i=insts.midiY(id);
			y = (i*23);
					
			mixerGUI[id][\scrollView] = MVC_CompositeView(mixerGUI[\midiScrollView],
										Rect(0,0+y,250,25), hasBorder:false);
			
			sv=mixerGUI[id][\scrollView];

			// number
			mixerGUI[id][\instNo] = MVC_StaticText(sv,
												inst.instNoModel,Rect(5,3,34,17))
				.hasBorder_(true)
				.active_(false)
				.clipChars_(true)
				.rightIfIcon_(true)
				.color_(\background, inst.mixerColor)
				.color_(\string,Color.black)
				.shadow_(false)
				.align_(\center)
				.font_(Font("Helvetica-Bold",14))
				.mouseDownAction_{|me,x,y,modifiers|
					hasMoved=false;
					moveDict=IdentityDictionary[];
					moveDict[\id]=id;
					moveDict[\startPos]=insts.getY(id);
				}
				.mouseMoveAction_{|me,x,y,modifiers|
					if (y<0) {y=y-23}; // because -0.9.asInt=0
					y=(y/23).asInt;
					if (y!=0) {hasMoved=true};
					this.move(id,moveDict[\startPos]+y);
					moveDict[\startPos]=(moveDict[\startPos]+y).clip(0,insts.size-1);
					mixerWindow.front;
				}
				.mouseUpAction_{|me,x,y,modifiers|
					if (hasMoved.not) {this.selectInst(id) }
				};
				
			MVC_FuncAdaptor(inst.instNoModel).func_{|me,value|
					mixerGUI[id][\instNo].string_((value+1).asString)
				}.freshAdaptor;
		
			MVC_Text(sv,inst.nameModel,Rect(38,3,109,17))
				.hasBorder_(true)
				.canEdit_(true)
				.clipChars_(true)
				.color_(\background, inst.mixerColor)
				.color_(\string,Color.black)
				.color_(\cursor, Color.orange)
				.shadow_(false)
				.align_(\center)
				.font_(Font("Helvetica",9))
				.color_(\focus,Color(1,1,1,0.66))
				.mouseDownAction_{|me,x,y,modifiers|
					moveDict=IdentityDictionary[];
					moveDict[\id]=id;
					moveDict[\startPos]=insts.getY(id);
				}
				.mouseMoveAction_{|me,x,y,modifiers|
					if (y<0) {y=y-23}; // because -0.9.asInt=0
					y=(y/23).asInt;
					this.move(id,moveDict[\startPos]+y);
					moveDict[\startPos]=(moveDict[\startPos]+y).clip(0,insts.size-1);
				};
				
			if (inst.canTurnOnOff.not) {
				
				// MIDI icon
				mixerGUI[id][\midi]=MVC_OnOffView(inst.onOffModel,sv,
											Rect(151,3,39,16))
					.permanentStrings_(["MIDI"])
					.canFocus_(false)
					.color_(\on, Color(0.2,0.3,0.5))
					.color_(\off,Color(0.2,0.3,0.5))
					.font_(Font("Helvetica-Bold",11));
				
			}{
						
				// on/off
				mixerGUI[id][\onOff]=MVC_OnOffView(inst.onOffModel, sv,
									Rect(151,3,20,16),gui[\onOffTheme2])
						.permanentStrings_(["On"])
						.canFocus_(false);
						
				// always on, hidden at start
				if (inst.canAlwaysOn) {
					mixerGUI[id][\alwaysOn]=MVC_OnOffView(1, sv, Rect(150,3,39,16),
							( \font_		: Font("Helvetica-Bold", 12),
							 \colors_     : (\on : inst.onColor,
							 			  \off : inst.onColor)))
						.permanentStrings_(["On"])
						.canFocus_(false)
						.visible_(false);	
						
					// always on
					MVC_OnOffView(inst.alwaysOnModel, sv, Rect(195, 3, 40, 16)
							,gui[\onOffTheme2])
						.color_(\on,inst.onColor )
						.canFocus_(false)
						.font_(Font("Helvetica",11));								
				};
						
				if (inst.onColor.notNil) {   mixerGUI[id][\onOff].color_(\on,inst.onColor ) };
					
				// solo
				mixerGUI[id][\solo]=MVC_OnOffView(inst.soloModel , sv,
									Rect(175,3,16,16),gui[\soloTheme])
						.canFocus_(false);
				
			};	
				
			// divider
			mixerGUI[id][\divider]=MVC_PlainSquare(sv, Rect(0,22,250,1))
					.color_(\on,Color(0,0,0,0.3))
					.color_(\off,Color(0,0,0,0.3));
	
			// divider
			mixerGUI[id][\divider2]=MVC_PlainSquare(sv, Rect(0,23,250,1))
					.color_(\on,Color(1,1,1,0.4))
					.color_(\off,Color(1,1,1,0.4));

		};

	}

}

