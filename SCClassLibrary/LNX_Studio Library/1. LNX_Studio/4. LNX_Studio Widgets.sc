
// the main studio, mixer & its widgets

+ LNX_Studio {

	// these get and set in the range 0-1, makes it easier to linear fades in dbs
	volume{ ^models[\volume].get }
	volume_{|val| models[\volume].set_(val) }

	peakLevel{ ^models[\peakLevel].value} // get the model value

	// set the model
	peakLevel_{|val| models[\peakLevel].valueAction_(val,0,true,false) }

	// fade in the master volume
	fadeIn{|level,beats,resolution=1|
		var startVolume,x;

		beats = beats ?? { #[256, 128, 64][models[\fadeSpeed].value] };

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
	fadeOut{|beats,resolution=1|
		var startVolume;

		beats = beats ?? {#[256, 128, 64 ][models[\fadeSpeed].value]};

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
		var bounds, width=1083 - ScrollBars.addIfNone(7), height=494;

		if (LNX_StartUp.hasWindowMenus ) { yos = 23 };

		bounds = this.savedWindowBounds ? Rect(osx,0,width,height+yos);
		bounds = bounds.setExtent(width,height+yos);

		mixerWindow = MVC_Window("LNX_Studio",bounds, resizable: true)
			.userCanClose_(false)
			.minHeight_(height+yos)
			.maxHeight_(height+yos)
			.minWidth_(width)
			.toFrontAction_{|me| LNX_SplashScreen.close }
			.keyDownAction_{|me, char, modifiers, unicode, keycode|
				//if (keycode==49) {this.togglePlay}; // space
			}
			.color_(\background,Color(4/77,2/103,0)*2.5);
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
				Rect(155+ (mixerGUI[\instScrollView].bounds.width-492/2), 210, 163, 30)

				);
			mixerGUI[\everythingInfoText].bounds_(
				Rect(155+ (mixerGUI[\presetTab].bounds.width-492/2), 212, 148, 34)
			);
		};
	}

	editMIDIControl{
		LNX_MIDIControl.editControls(this);
		LNX_MIDIControl.window.front;
	}

	// and create the the widgets
	createMixerWidgets{

		var sv;

		insts.addDependant{ this.updateMixerInfoText }; // to update infoText

		models[\volume].hackAction_{|model,button| if (button==1) { this.peakLevel_(model.get) }};

		mixerGUI = IdentityDictionary[];

		if (LNX_StartUp.hasWindowMenus) { LNX_AppMenus.addWindowMenus(mixerWindow) };

		// themes
		mixerGUI[\buttonTheme2 ] = (\colors_  : (	\up    : Color(6/11,42/83,29/65)*0.8,
									\down     : Color(6/11,42/83,29/65)/2,
					 				\string   : Color.black),
									\rounded_ : true);

		gui[\textTheme2] = (	\shadow_	: false,
								\font_		: Font("Helvetica", 10),
								\align		: \left,
								\colors_	: (	\string:	Color(46/77,46/79,72/145) ) );

		gui[\onOffTheme] = (	\font_	: Font("Helvetica",12),
								\colors_: (	\on:		Color.red,
											\off:	Color(46/77,46/79,72/145)/1.5,
											\string:	Color.black ) );

		gui[\syncTheme]=(	\orientation_ 	: \horiz,
							\resoultion_	: 8,
							\visualRound_ 	: 0.1,
							\rounded_		: true,
							\font_			: Font("Helvetica",10),
							\labelFont_		: Font("Helvetica",10),
							\showNumberBox_	: false,
							\colors_		: (	\background : Color(0,0,0,0.15),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.black,
										\focus : Color(0,0,0,0)));

		// all or 1 window
		MVC_BinaryCircleView(models[\show1],mixerWindow,Rect(215, 3+yos, 20, 20))
			.strings_(["1","A"])
			.font_(Font("Helvetica",14,true))
			.colors_((\upOn:Color(0.9,0.7,0), \upOff:Color(0,0.6,0), \stringOn:Color.black,
				\stringOff:Color.black, \downOn:Color(0.5,0.5,0), \downOff:Color(0,0.2,0)));

		// close all inst
		MVC_BinaryCircleView(models[\showNone],mixerWindow,Rect(237, 3+yos, 20, 20),"X")
			.font_(Font("Helvetica",14,true))
			.colors_((\upOn:Color(0.9,0.075,0.075),\upOff:Color(0.8,0.15,0.15),
			\stringOn:Color.black, \stringOff:Color(0.6,0.15,0.15),
			\downOn:Color(0.6,0.1,0.1), \downOff:Color(0.6,0.1,0.1)));

		// internal or extrnal clock
		MVC_OnOffFlatView(models[\extClock],mixerWindow,Rect(273, 4+yos, 29, 19),gui[\onOffTheme])
			.font_(Font("Helvetica", 12,true))
			.color_(\on,Color(0.85,0.85,0.2))
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2);

		// tempo (bpm)
		MVC_NumberBox(models[\tempo],mixerWindow, Rect(305, 5+yos, 38, 16))
			.rounded_(true)
			.visualRound_(0.1)
			.font_(Font("Helvetica", 11))
			.color_(\focus,Color.grey(alpha:0))
			.color_(\string,Color.white)
			.color_(\typing,Color.yellow)
			.color_(\background,Color(46/77,46/79,72/145)/1.5);

		// tap tempo
		MVC_OnOffFlatView(models[\tap],mixerWindow,Rect(347,4+yos,32,19),gui[\onOffTheme])
			.mouseMode_(\tap)
			.darkerWhenPressed_(false)
			.font_(Font("Helvetica", 12,true))
			.color_(\on,Color(0.85,0.85,0.2))
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2);

		// record
		MVC_OnOffFlatView(models[\record],mixerWindow,Rect(394, 4+yos, 35, 19),gui[\onOffTheme])
			.strings_(["Rec"])
			.font_(Font("Helvetica", 12, true))
			.color_(\on,Color(1,0,0))
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2);

		// play
		MVC_OnOffFlatView(models[\play],mixerWindow,Rect(508, 4+yos, 19, 19))
			.strings_(["play"])
			.insetBy_(-1)
			.color_(\on,Color.green)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/1.7)
			.mode_(\play);

		// stop
		MVC_OnOffFlatView(models[\stop],mixerWindow,Rect(530, 4+yos, 19, 19))
			.strings_(["stop"])
			.darkerWhenPressed_(false)
			.insetBy_(0)
			.color_(\on,Color.yellow)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/1.7)
			.mode_(\stop)
			.mouseMode_(\button);

		// forwards
		MVC_OnOffFlatView(models[\fowards], mixerWindow, Rect(553, 4+yos, 19, 19))
			.mode_(\icon)
			.darkerWhenPressed_(false)
			.mouseMode_(\button)
			.insetBy_(-2)
			.color_(\on,Color.green)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/1.7)
			.strings_(["forward"]);

		// rewind
		MVC_OnOffFlatView(models[\rewind], mixerWindow,Rect(486, 4+yos, 19, 19))
			.mode_(\icon)
			.darkerWhenPressed_(false)
			.mouseMode_(\button)
			.insetBy_(-2)
			.color_(\on,Color.green)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/1.7)
			.strings_(["rewind"]);

		// time / beat
		mixerGUI[\time]=MVC_StaticText(mixerWindow,Rect(573, 3+yos, 52, 23),gui[\textTheme2])
			.font_(Font("Helvetica",14))
			.align_(\center)
			.string_("0:00")
			.excludeFromVerbose_(true);

		mixerGUI[\beat]=MVC_StaticText(mixerWindow,Rect(431, 3+yos, 52, 23),gui[\textTheme2])
			.font_(Font("Helvetica",14))
			.align_(\center)
			.string_("1.1")
			.excludeFromVerbose_(true);

		// server on/off view
		mixerGUI[\serverGUI]=MVC_FlatButton2(models[\serverRunning],mixerWindow,
								Rect(637, 4+yos, 26, 19))
			.resize_(3)
			.font_(Font("Helvetica",11,true))
			.color_(\off,Color.black)
			.color_(\on,Color.green)
			.color_(\background,Color(46/77,46/79,72/145)/2) ;

		// net on/off view
		MVC_FlatButton2(models[\network],mixerWindow,Rect(667, 4+yos, 26, 19))
			.resize_(3)
			.font_(Font("Helvetica",11,true))
			.color_(\off,Color.black)
			.color_(\on,Color.green)
			.color_(\background,Color(46/77,46/79,72/145)/2);

		// server stats: cpu
		MVC_StaticText(mixerWindow,Rect(701, 7+yos, 24, 15),gui[\textTheme2])
			.resize_(3)
			.string_("CPU:")
			.excludeFromVerbose_(true);

		mixerGUI[\cpu]=MVC_StaticText(mixerWindow,Rect(727, 7+yos, 30, 15),gui[\textTheme2])
			.resize_(3)
			.string_("-")
			.excludeFromVerbose_(true);

		gui[\multiTheme ]=(\font_:Font("Helvetica",12,true),
			\states_ : [
				["S"   ,Color(1, 0,   0)/1.15 ,Color.black,Color.grey/3,Color.grey/2],
				["M"   ,Color(1, 0.5, 0)/1.15 ,Color.black,Color.grey/2,Color.grey/3],
				["F"   ,Color(0.5, 1, 0)/1.15 ,Color.black,Color.grey/4,Color.grey/2]]);


		MVC_MultiOnOffView(models[\fadeSpeed], mixerWindow,Rect(770,4+yos,19,19),gui[\multiTheme ])
			.color_(\background,Color(46/77,46/79,72/145)/4)
			.resize_(3);

		this.createLibraryScrollView;

// *******************

		mixerGUI[\masterTabs]=MVC_TabbedView(mixerWindow,Rect(220, 33+yos, 585, 450),
			scroll:[true,true,false,false], offset:(6@(279)))
			.action_{|me|}
			.labels_(["Mixer","Prog","Auto"])
			.resize_(2)
			.font_(Font("Helvetica", 14))
			.tabPosition_(\right)
			.unfocusedColors_( Color(6/11,42/83,29/65)/2 ! 4)
			.labelColors_(   Color(6/11,42/83,29/65) !4)
			.backgrounds_(  Color(0.8,0.8,0.8)!4 )
			.tabCurve_(5)
			.tabWidth_([ 45, 41, 38,12 ])
			.tabHeight_(22)
			.followEdges_(true)
			.adjustments_([nil, Rect(83-10,0,-83+10,0),nil,nil])
			.value_(0)
			.resizeAction_{
				if (insts.size<1) {
					mixerGUI[\instInfoText].bounds_(
						Rect(155+ (mixerGUI[\instScrollView].bounds.width-492/2), 210, 163, 30)
					);
					mixerGUI[\everythingInfoText].bounds_(
						Rect(155+ (mixerGUI[\presetTab].bounds.width-492/2), 212, 148, 34)
					);
				};
			};

		mixerGUI[\popProgramsScrollView] = MVC_ScrollView(mixerWindow,Rect(220, 33+yos, 73, 450))
			.autoScrolls_(true)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(true)
			.hasBorder_(false)
			.visible_(false)
			.color_(\background,Color(0.8,0.8,0.8));

		mixerGUI[\masterTabs].focusActions_([{},{
			mixerGUI[\popProgramsScrollView].visible_(true);
		},{}]);
		mixerGUI[\masterTabs].unfocusActions_([{},{
			mixerGUI[\popProgramsScrollView].visible_(false);
		},{}]);

		// mixer tab

		mixerGUI[\instScrollView] = mixerGUI[\masterTabs].mvcTab(0)
			.hasVerticalScroller_(false);

		MVC_RoundBounds(mixerWindow,Rect(220, 33+yos, 492+71, 450))
			.setResize(1,2,3,2)
			.width_(6)
			.color_(\background, Color(6/11,42/83,29/65));

		// instrument logo
		mixerGUI[\instInfoText] = MVC_ImageView(mixerGUI[\instScrollView],Rect(155, 212, 163, 30))
			.image_("fontImages/Instruments.png");

		// the fx scroll view
		mixerGUI[\fxScrollView] = MVC_RoundedScrollView (mixerWindow,
			Rect(803, 13+yos, 177 - ScrollBars.addIfNone(7), 285))
			.hasBorder_(false)
			.resizeList_([3,3,3,3,3]) //  0:view 1:left 2:top 3:right 4:bottom
			.autoScrolls_(true)
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border,Color(6/11,42/83,29/65))
			.hasVerticalScroller_(true);

		// effects logo
		mixerGUI[\fxInfoText] = MVC_ImageView(mixerGUI[\fxScrollView],Rect(49, 130, 80, 27))
			.image_("fontImages/Effects.png");

		// the midi scroll view
		mixerGUI[\midiScrollView] = MVC_RoundedScrollView (mixerWindow,
				Rect(824, 316+yos, 247 - ScrollBars.addIfNone(7), 167))
			.hasBorder_(false)
			.resizeList_([3,3,3,3,3]) //  0:view 1:left 2:top 3:right 4:bottom
			.autoScrolls_(true)
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border,Color(6/11,42/83,29/65))
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false);

		// MIDI logo
		mixerGUI[\midiInfoText] = MVC_ImageView(mixerGUI[\midiScrollView],Rect(88, 71, 59, 26))
			.image_("fontImages/MIDI.png");

		// the master levels scroll view
		mixerGUI[\masterLevelsScrollView] = MVC_RoundedScrollView (mixerWindow,
						Rect(998- ScrollBars.addIfNone(7), 13+yos, 73, 285))
			.hasBorder_(false)
			.resizeList_([3,3,3,3,3]) //  0:view 1:left 2:top 3:right 4:bottom
			.autoScrolls_(true)
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border,Color(6/11,42/83,29/65))
			.hasVerticalScroller_(false);

		sv = mixerGUI[\masterLevelsScrollView];

		// levels
		MVC_FlatDisplay(models[\peakOutL],sv,Rect(3, 23, 7, 178));
		MVC_FlatDisplay(models[\peakOutR],sv,Rect(12, 23, 7, 178));
		MVC_Scale(sv,Rect(10, 23, 2, 192-10-7));

		// peakLevel
		MVC_PeakLevel(sv,models[\peakLevel],Rect(56, 27, 13, 170));
		// volume
		MVC_SmoothSlider(models[\volume],sv,Rect(22, 23, 33, 178))
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
		MVC_OnOffView(models[\mute],sv, Rect(21, 236, 34, 20))
				.permanentStrings_(["Mute"])
				.font_(Font("Helvetica",11,true))
				.color_(\on,Color.red)
				.color_(\off, Color(0.3686, 0.3373, 0.2412,0.5) )
				.rounded_(true)
				.canFocus_(false);


		MVC_NumberBox(models[\preAmp], sv,Rect(12, 260, 50, 19),  gui[\syncTheme])
			.postfix_(" db")
			.color_(\label,Color.white);

		// fadeIn
		MVC_FlatButton(sv,Rect(25, 3, 27, 17), mixerGUI[\buttonTheme2 ] ,"up")
			.mode_(\icon)
			.action_{ this.fadeIn };

		// fadeOut
		MVC_FlatButton(sv,Rect(25, 215, 27, 17), mixerGUI[\buttonTheme2 ] ,"down")
			.mode_(\icon)
			.action_{ this.fadeOut };

		// add the automation widgets
		MVC_Automation.createWidgets(mixerGUI[\masterTabs].mvcTab(2));

		mixerGUI[\presetTab] = mixerGUI[\masterTabs].mvcTab(1);

		// add the preset widgets
		LNX_POP.createWidgets( mixerGUI[\popProgramsScrollView], mixerGUI[\presetTab]);

        // Everything logo
		mixerGUI[\everythingInfoText] = MVC_ImageView(mixerGUI[\presetTab],Rect(155, 212, 148, 34))
			.image_("fontImages/Everything.png");

		// auto On (play)
		mixerGUI[\autoPlay] = MVC_OnOffFlatView(models[\autoOn],mixerWindow,Rect(793, 468+yos, 19, 19))
			.resize_(3)
			.color_(\on,Color.white)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2)
			.strings_(["A"]);

		// auto record
		mixerGUI[\autoRec] = MVC_OnOffFlatView(models[\autoRecord],mixerWindow,Rect(793, 445+yos, 19, 19))
			.resize_(3)
			.color_(\on,Color.red)
			.color_(\off,Color.black)
			.color_(\background,Color(46/77,46/79,72/145)/2)
			.mode_(\icon)
			.strings_([\record]);

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

		this.alignEQWidgets;
	}

	// align eq widgets because alternate widgets have different pos
	alignEQWidgets{
		insts.mixerInstruments.do{|inst,i|
			var id = inst.id;
			if (i.even) {
				mixerGUI[id][\eqButton].moveTo(42,356);
				mixerGUI[id][\eqLamp].moveTo(47,341);

			}{
				mixerGUI[id][\eqButton].moveTo(42,343);
				mixerGUI[id][\eqLamp].moveTo(47,362);
			};
		}
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

		mixerGUI[\soloTheme ]=( \font_		: Font("Helvetica", 12, true),
								\colors_    : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));

		mixerGUI[\onOffTheme2]=( \font_		: Font("Helvetica", 12),
						 		 \colors_   : (\on : Color(0.25,1,0.25),
						 	     \off 		: Color(0.4,0.4,0.4)));

		mixerGUI[\menuTheme2]=( \font_		: Font("Arial", 9),
				\colors_      : (\background:Color(0.1,0.1,0.1,0.1),\string:Color.black,
							   \focus:Color.clear));

		gui[\syncTheme]=(
						\orientation_  : \horiz,
						\resoultion_   : 7.5,
						\visualRound_  : 0.001,
						\rounded_      : true,
						\font_		   : Font("Helvetica",10),
						\labelFont_	   : Font("Helvetica",10),
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
									Rect(y,0,72,21*20+30), hasBorder:false);

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
			.font_(Font("Helvetica",14,true))
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
										Rect(0+y,0,72,450-3), hasBorder:false);

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
				.font_(Font("Helvetica",14,true))
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

/*
m=\switch.asModel;
w=MVC_Window().create;
f=MVC_FlatButton(w,m,Rect(10,10,20,20));
m.action_{|...a| a.postln}
//-10, -11 are free for fadeIn & fadeOut
*/

			// fadeIn
			MVC_FlatButton(inst.fadeInModel, sv,Rect(24, 107, 27, 17), mixerGUI[\buttonTheme2 ] ,"up")
				.mode_(\icon);

			// fadeOut
			MVC_FlatButton(inst.fadeOutModel, sv,Rect(24, 290+30, 27, 17), mixerGUI[\buttonTheme2 ] ,"down")
				.mode_(\icon);

			// pan
			MVC_MyKnob3(inst.panModel,sv,Rect(12, 340 + ScrollBars.addIfNone(2), 25, 25),mixerGUI[\knobTheme1])
				.numberFont_(Font("Helvetica",10))
				.numberFunc_(\pan)
				.label_(nil)
				.numberWidth_(-6)
				.showNumberBox_(true);

			if (insts.mixerInstruments.indexOf(inst).even) {

				// eq button
				mixerGUI[id][\eqButton] = MVC_FlatButton(sv, Rect(42, 356, 21, 17),"EQ")
					.rounded_(true)
					.font_(Font("Helvetica",10,true))
					.resize_(7)
					.canFocus_(false)
					.color_(\up,Color(1,1,1))
					.color_(\down,Color(6/11,42/83,29/65)/2)
					.action_{ inst.openEQ };

				// eq on/off
				mixerGUI[id][\eqLamp] = MVC_PipeLampView(sv,Rect(47,341,11,11),inst.eqOnOffModel)
					.insetBy_(1)
					.border_(true)
					.mouseWorks_(true)
					.color_(\on,Color.green);

			}{

				// eq button
				mixerGUI[id][\eqButton] = MVC_FlatButton(sv, Rect(42, 343, 21, 17),"EQ")
					.rounded_(true)
					.font_(Font("Helvetica",10,true))
					.resize_(7)
					.canFocus_(false)
					.color_(\up,Color(1,1,1))
					.color_(\down,Color(6/11,42/83,29/65)/2)
					.action_{ inst.openEQ };

				// eq on/off
				mixerGUI[id][\eqLamp] = MVC_PipeLampView(sv,Rect(47,362,11,11),inst.eqOnOffModel)
					.insetBy_(1)
					.border_(true)
					.mouseWorks_(true)
					.color_(\on,Color.green);

			};

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
			MVC_OnOffView(inst.onOffModel, sv,
					Rect(13, 378+ScrollBars.addIfNone(3), 20, 20),mixerGUI[\onOffTheme2])
				//.permanentStrings_(["On"])
				.rounded_(true)
				.canFocus_(false);

			// solo
			MVC_OnOffView(inst.soloModel , sv,
					Rect(42, 378+ScrollBars.addIfNone(3), 20, 20),mixerGUI[\soloTheme])
				.rounded_(true)
				.canFocus_(false);

			// out channel
			MVC_PopUpMenu3(inst.outChModel,sv, Rect(5, 401+ScrollBars.addIfNone(5), 64, 16),
				mixerGUI[\menuTheme2])
				.label_(nil)
				.color_(\background, inst.mixerColor);

			// sync
			if (inst.syncModel.notNil) {
				MVC_NumberBox(inst.syncModel, sv,
						Rect(12, 422+ScrollBars.addIfNone(7), 50, 16),  gui[\syncTheme])
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
										Rect(0,0+y,170+ ScrollBars.addIfSome(7),62), hasBorder:false);

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
				.font_(Font("Helvetica",14,true))
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
			mixerGUI[id][\in]=MVC_PopUpMenu3(inst.inChModel, sv, Rect(5, 23, 76, 16),
				mixerGUI[\menuTheme2])
				.label_(nil)
				.color_(\background, inst.mixerColor);

			// out channel
			MVC_PopUpMenu3(inst.outChModel,sv, Rect(89, 23, 76, 16),
								mixerGUI[\menuTheme2])
				.label_(nil)
				.color_(\background, inst.mixerColor);

			// divider
			mixerGUI[id][\divider]=MVC_PlainSquare(sv, Rect(0,59,170+7+ ScrollBars.addIfSome(7),1))
					.color_(\on,Color(0,0,0,0.3))
					.color_(\off,Color(0,0,0,0.3));

			// divider
			mixerGUI[id][\divider2]=MVC_PlainSquare(sv, Rect(0,60,170+7+ ScrollBars.addIfSome(7),1))
					.color_(\on,Color(1,1,1,0.4))
					.color_(\off,Color(1,1,1,0.4));

		};

		if (inst.isMIDI) {

			i=insts.midiY(id);
			y = (i*23);

			mixerGUI[id][\scrollView] = MVC_CompositeView(mixerGUI[\midiScrollView],
										Rect(0,0+y,240+ ScrollBars.addIfSome(7),25), hasBorder:false);

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
				.font_(Font("Helvetica",14,true))
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
											Rect(151,3,84,17))
					.permanentStrings_(["MIDI"])
					.canFocus_(false)
					.color_(\background, Color.clear)
					.color_(\border, Color(0,0,0,0.33), true)
					.color_(\on, inst.mixerColor)
					.color_(\off,inst.mixerColor)
					.font_(Font("Helvetica",11,true));

			}{

				// on/off
				mixerGUI[id][\onOff]=MVC_OnOffView(inst.onOffModel, sv,
									Rect(151,3,20,16),gui[\onOffTheme2])
						.permanentStrings_(["On"])
						.canFocus_(false);

				// always on, hidden at start
				if (inst.canAlwaysOn) {
					mixerGUI[id][\alwaysOn]=MVC_OnOffView(1, sv, Rect(150,3,39,16),
							( \font_		: Font("Helvetica",true, 12),
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
			mixerGUI[id][\divider]=MVC_PlainSquare(sv, Rect(0,22,250 + ScrollBars.addIfSome(7),1))
					.color_(\on,Color(0,0,0,0.3))
					.color_(\off,Color(0,0,0,0.3));

			// divider
			mixerGUI[id][\divider2]=MVC_PlainSquare(sv, Rect(0,23,250 + ScrollBars.addIfSome(7),1))
					.color_(\on,Color(1,1,1,0.4))
					.color_(\off,Color(1,1,1,0.4));

		};

	}

	///////// preferences //////////////////////////////////////////////////////////////////

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

		gui[\labelTheme]=( \font_		:  Font("Helvetica", 14,true),
						\align_		: \left,
						\shadow_		: false,
						\noShadows_	: 0,
						\colors_		: (\string : Color.black));

		if ( (midiWin.isNil) or: {midiWin.window.isClosed } ) {

			midiWin = MVC_ModalWindow(
				(mixerWindow.isVisible).if(mixerWindow,window),
				(420)@(468));
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
				.label_("Program Launchpad In")
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
			noInternalBusesGUI=MVC_PopUpMenu3(scrollView,Rect(170, 386, 70, 17))
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
			MVC_FlatButton(scrollView,Rect(240 ,415, 70, 20),"Scan MIDI",gui[\buttonTheme])
				.canFocus_(false)
				.action_{ LNX_MIDIPatch.refreshPorts };


			// Ok
			MVC_FlatButton(scrollView,Rect(332, 415, 50, 20),"Ok",gui[\buttonTheme])
				.canFocus_(true)
				.color_(\up,Color.white)
				.action_{	 midiWin.close };

		}{
			midiWin.front;
		}
	}

	// gui window to quit LNX ///////////////////////////////////////////////////////////

	quit{
		var gui = IdentityDictionary[];
		var colors = (
			background: 	Color(59/77,59/77,59/77),
			border2: 		Color(6/11,42/83,29/65),
			border1: 		Color(3/77,1/103,0,65/77),
			menuBackground:	Color(1,1,0.9)
		) ++ (colors?());

		if (mixerWindow.isClosed) {
			this.doQuit;
			^this
		};

		gui[\window] = MVC_ModalWindow(mixerWindow, (190)@(132), colors).postln;
		gui[\scrollView] = gui[\window].scrollView;

		MVC_StaticText( gui[\scrollView], Rect(10,5,190,18))
			.shadow_(false)
			.color_(\string,Color.black)
			.font_(Font("Helvetica", 13,true))
		.string_(LNX_Studio.is_in_a_khole.if("Quit [k] hole?","Quit LNX_Studio?"));

		MVC_StaticText( gui[\scrollView], Rect(10,30,190,18*2))
			.shadow_(false)
			.color_(\string,Color.black)
			.font_(Font("Helvetica", 11))
			.string_("Any unsaved information\n will be lost");

		// Ok
		gui[\ok] = MVC_OnOffView(gui[\scrollView],Rect(110, 78, 50, 20),"Ok")
			.rounded_(true)
			.canFocus_(true)
			.color_(\on,Color(0.88,0.88,0.88))
			.color_(\off,Color(0.88,0.88,0.88))
			.action_{
				gui[\window].close;
				this.doQuit;
			};

		{
			//.focusColor_(Color(0.5,0.5,1))
			gui[\ok].view.canFocus_(true).focus.keyUpAction_{|me,char,mod,uni,keycode,key|
				// return
				if (key==16777220) { gui[\ok].down_(true); this.doQuit };
				// escape
				if (key==16777216) { gui[\cancel].down_(true); {gui[\window].close}.defer(0.25) };
			};
		}.defer(0.5);

		// Cancel
		gui[\cancel] = MVC_OnOffView(gui[\scrollView],Rect(53, 78, 50, 20),"Cancel")
			.rounded_(true)
			.color_(\on,Color(0.88,0.88,0.88))
			.color_(\off,Color(0.88,0.88,0.88))
			.action_{ gui[\window].close };
	}

	doQuit{
		{
			HID.closeAll;
			LNX_BufferProxy.emptyTempFolder;
			CmdPeriod.run;
			0.25.wait;
			this.free;
			0.25.wait;
			Server.quitAll;
			0.exit;
		}.fork(AppClock);
	}

	// for when studio isn't running
	*quit{
		{
			0.25.wait;
			Server.quitAll; // ShutDown
			0.exit;
		}.fork(AppClock);
	}


}

