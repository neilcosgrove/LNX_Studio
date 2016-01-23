////////////////////////////////////////////////////////////////////////////////////////////////////
// GUI code for LNX_Code                                                                           .

+ LNX_Code {
	
	// GUI
	
	*thisWidth  {^670}
	*thisHeight {^525-5}
	
	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(3/77,1/103,0,65/77), resizable:false); 
	}

	// create all the GUI widgets while attaching them to models
	createWidgets{
		
		var background=Color(59/77,59/77,59/77);
		
		
		gui[\scrollView] = MVC_RoundedComView(window,
									Rect(11,11,thisWidth-22,thisHeight-22-1))
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border, Color(6/11,42/83,29/65))
			.resize_(5);
		
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));

		gui[\onOffTheme1]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color(0.25,1,0.25), \off : Color(0.4,0.4,0.4)));

		gui[\userGUI]    =(\colors_		: (\on : Color.orange,
									   \main: Color.orange, 
									   \numberUp:Color.black,
									   \numberDown:Color.orange),
						\labelFont_   : Font("Helvetica",12),
						\numberFont_  : Font("Helvetica",11));
		
						 
		gui[\knobTheme1]=(\colors_		: (\on : Color.orange, 
									   \numberUp:Color(50/77,1,61/77),
									   \numberDown:Color.black),
						\labelFont_   : Font("Helvetica",12),
						\numberFont_  : Font("Helvetica",11));
						 
		gui[\knobTheme2]=(\colors_		: (\on : Color.orange, 
									   \numberUp:Color.black,
									   \numberDown:Color.white),
						\numberFont_  : Font("Helvetica",11));
						
		gui[\knobTheme3]=(\colors_		: (\on : Color(50/77,61/77,1), 
									   \numberUp:Color.black,
						 			  \numberDown:Color.white),
						\numberFont_  : Font("Helvetica",11));
						
		gui[\theme2]=(	\layout_       : \reversed,
						\resoultion_	 : 3,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",12),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.43,0.40,0.38),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.white,
										\focus : Color(0,0,0,0)));
		
		
		gui[\theme3]=(	\orientation_  : \horizontal,
						\resoultion_	 : 3,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",12),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.1,0.1,0.1,0.67),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.white,
										\focus : Color(0,0,0,0)));
										
		gui[\theme4]=(	\layout_       : \reversed,
						\resoultion_	 : 1,
						\font_		 : Font("Helvetica",12),
						\labelFont_	 : Font("Helvetica",12),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(1,1,1,0.3),
										\typing: Color(1,1,1),
										\string : Color.black,
										\focus : Color(0,0,0,0)));
										
		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\layout_      : \reversed,
						\colors_      : (\background : background*1.5, \focus: Color.clear));
						
		gui[\infoTheme]=(	\orientation_ : \horiz,
						\labelShadow_ : false,
						\labelFont_   : Font("Helvetica", 10),
						\shadow_      : false,
						\align_       : \left,
						\colors_      : (\string : Color.black, \label : Color.black),
						\font_        : Font("Helvetica", 10));
					
		// the tab view
		
		gui[\tabView]=MVC_TabbedView(gui[\scrollView],Rect(5,30+1,640,365+2))
			.labels_(["Widgets","Piano Roll","Samples","SC Code"])
			.unfocusedColors_(
					   (( Color(3/77,1/103,0,65/77)+0.4)!4) )
			.labelColors_(( Color(3/77,1/103,0,65/77)!4) )
			.backgrounds_(( Color(3/77,1/103,0,65/77)!4) )
			.resize_(5)
			.tabPosition_(\top)
			.tabCurve_(5)
			.tabWidth_([90,90,75,75])
			.tabHeight_(16+1)
			.followEdges_(true)
			.action_{|me|
				models[13].value_(me.value);
				p[13]=me.value;
			};
			
		gui[\userGUIScrollView] = MVC_ScrollView(gui[\tabView].mvcTab(0),
												Rect(8,8,640-15,365-15-15-2))
			.color_(\background,Color(6/11,42/83,29/65))
			.hasBorder_(false)
			.autoScrolls_(true)
			.hasHorizontalScroller_(true)
			.hasVerticalScroller_(true)
			.autohidesScrollers_(true);
		
		MVC_RoundBounds(gui[\tabView].mvcTab(0),Rect(8,8,640-15,365-15-15-2))
			.width_(4)
			.color_(\background, Color(6/11,42/83,29/65));
		
		// the keyboard
			
		gui[\keyboardOuterView] = MVC_CompositeView(gui[\scrollView],Rect(5,365+30+5+4,640,90));
			
		gui[\keyboardView]=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0,0,640,90),6,24)
			.keyDownAction_{|note|
				this.keyboardNoteOn(note,100/127);
				lastKeyboardNote=note;
			}
			.keyUpAction_{|note|
				this.keyboardNoteOff(note);
			}
			.keyTrackAction_{|note|
				this.keyboardNoteOn(note,100/127);
				if (lastKeyboardNote.notNil) {
					this.keyboardNoteOff(lastKeyboardNote);
				};
				lastKeyboardNote=note;
			};
			
		toFrontAction = { gui[\keyboardView].focus };
			
		// master
		
		// 1.on/off
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect( 6, 6,22,18),gui[\onOffTheme1])
			.rounded_(true)
			.permanentStrings_(["On"]);
			
		// 0.solo
		MVC_OnOffView(models[0],gui[\scrollView] ,Rect( 33, 6,20,18),gui[\soloTheme  ])
			.rounded_(true);
		
		// 2. master volume
		systemViews[\volume] = 
			MVC_SmoothSlider(models[2],gui[\userGUIScrollView],Rect(567,20,30,100))
			.numberFont_(Font("Helvetica",11))
			.color_(\knob,Color.orange)
			.color_(\numberUp,Color.black)
			.color_(\numberDown,Color.white)
			.boundsAction_{|view,rect| api.sendVP("nBV",\netBoundsSystem,\volume,*rect.storeArgs)}
			.mouseDownAction_{|me| this.selectGUI(me,systemViews,\volume,\system) }
			.colorAction_{|view,key,color|
				api.sendVP("2_color",\netColorSystem,\volume,key, *color.storeArgs);
			};
			
		// 4.master pan
		systemViews[\pan] =
			MVC_MyKnob3(models[4],gui[\userGUIScrollView],Rect(568,152,26,26),gui[\knobTheme2])
			.boundsAction_{|view,rect| api.sendVP("nBP",\netBoundsSystem,\pan,*rect.storeArgs)}
			.mouseDownAction_{|me| this.selectGUI(me,systemViews,\pan,\system) }
			.colorAction_{|view,key,color|
				api.sendVP("4_color",\netColorSystem,\pan,key, *color.storeArgs);
			};
		
		// 10. sendAmp
		systemViews[\sendAmp] =
			MVC_MyKnob3(models[10],gui[\userGUIScrollView],Rect(568,210,26,26),gui[\knobTheme2])
			.boundsAction_{|view,rect|
				api.sendVP("10_bounds",\netBoundsSystem,\sendAmp,*rect.storeArgs)}
			.mouseDownAction_{|me| this.selectGUI(me,systemViews,\sendAmp,\system) }
			.colorAction_{|view,key,color|
				api.sendVP("10_color",\netColorSystem,\sendAmp,key, *color.storeArgs);
			};

		// 15. inAmp
		systemViews[\inAmp] =
			MVC_MyKnob3(models[15],gui[\userGUIScrollView],Rect(568,268,26,26),gui[\knobTheme2])
			.boundsAction_{|view,rect|
				api.sendVP("15_bounds",\netBoundsSystem,\inAmp,*rect.storeArgs)}
			.mouseDownAction_{|me| this.selectGUI(me,systemViews,\inAmp,\system) }
			.colorAction_{|view,key,color|
				api.sendVP("15_color",\netColorSystem,\inAmp,key, *color.storeArgs);
			}
			.visible_(false);

		// MIDI Settings
 		MVC_FlatButton(gui[\scrollView],Rect(63, 6, 43, 19),"MIDI")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(6/11,42/83,29/65))
			.color_(\down,Color(6/11,42/83,29/65)/2)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ this.createMIDIInModelWindow(window,low:7,high:8) };

		// MIDI Control
 		MVC_FlatButton(gui[\scrollView],Rect(111, 6, 43, 19),"Cntrl")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(6/11,42/83,29/65))
			.color_(\down,Color(6/11,42/83,29/65)/2)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{  LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front  };
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],(165)@(6),92-25-30+33,
				Color(6/11,42/83,29/65),
				Color.black,
				Color(6/11,42/83,29/65),
				background,
				Color.black
			);
		this.attachActionsToPresetGUI;
		
		// 12. poly
		MVC_NumberBox(models[12], gui[\scrollView], Rect(581,7,22+4,22-4), gui[\theme2])
			.rounded_(true);
			
		// 3.output channels
		MVC_PopUpMenu3(models[3],gui[\scrollView],Rect(500,7,70,17),gui[\menuTheme  ]);

		MVC_PlainSquare(gui[\scrollView],Rect(420,7,70,17))
			.color_(\on,Color(0,0,0,0.2))
			.color_(\off,Color(0,0,0,0.2));		
			
		// 9. sendOut	
		gui[\sendOut] =
			MVC_PopUpMenu3(models[9],gui[\scrollView],Rect(420,7,70,17),gui[\menuTheme  ]);

		MVC_PlainSquare(gui[\scrollView],Rect(340,7,70,17))
			.color_(\on,Color(0,0,0,0.2))
			.color_(\off,Color(0,0,0,0.2));

		// 11. in	
		gui[\in] =
			MVC_PopUpMenu3(models[11],gui[\scrollView],Rect(340,7,70,17),gui[\menuTheme  ])
				.visible_(false);

		// sampele tab /////////////////

		gui[\sampleGUIScrollView] = MVC_CompositeView(gui[\tabView].mvcTab(2),
												Rect(8, 8, 625, 335))
			.color_(\background,Color(6/11,42/83,29/65))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);
		
		MVC_RoundBounds(gui[\tabView].mvcTab(2),Rect(8, 8, 625, 335))
			.width_(4)
			.color_(\background, Color(6/11,42/83,29/65));

		//samples //*****		

		MVC_Icon(gui[\sampleGUIScrollView], Rect(386,210, 18, 18))
			.icon_(\speaker);

		gui[\speakerIcon] = MVC_Icon(gui[\sampleGUIScrollView], Rect(386,210, 18, 18).insetBy(3,3))
			.icon_(\speaker);

		userBank.speakerIcon_(gui[\speakerIcon]);

 		MVC_StaticText( gui[\sampleGUIScrollView], Rect(160,212, 315, 18-3),gui[\infoTheme])
 			.font_(Font("Helvetica-Bold", 11))
			.string_(
		" Sample                                                Amp              Pitch");

		// the list scroll view
		gui[\sampleListCompositeView] = MVC_RoundedScrollView(gui[\sampleGUIScrollView],
				Rect(160, 230, 315, 101))
			.width_(3.5)
			.hasBorder_(true)
			.hasVerticalScroller_(true)
			.autohidesScrollers_(false)
			.color_(\background, Color(0,0,0,0.2))
			.color_(\border, Color(0,0,0,0.35));
		
		userBank.window2_(gui[\sampleListCompositeView]);
		
		// the sample editor
		userBank.openMetadataEditor(
			gui[\sampleGUIScrollView],
			0,
			true,
			webBrowser,
			(border2: Color(59/108,65/103,505/692), border1: Color(0,1/103,9/77)),
			50
		);

		// 18. Sample list or Music inst
		MVC_OnOffView(models[18], gui[\sampleGUIScrollView], Rect(12, 229, 50, 20))
			.strings_(["List","Inst"])
			.rounded_(true)  
			.color_(\on,Color(0.5,1,0.5,0.88))
			.color_(\off,Color(50/77,61/77,1));
								
		// 19. Sample root
		MVC_NumberBox(models[19],gui[\sampleGUIScrollView], Rect(97, 229, 43, 19))
			.labelShadow_(false)
			.orientation_(\horizontal)
			.color_(\label,Color.black);
	
		// 20. Sample steps per octave (spo)
		MVC_NumberBox(models[20],gui[\sampleGUIScrollView], Rect(97, 253, 43, 19))
			.labelShadow_(false)
			.orientation_(\horizontal)
			.color_(\label,Color.black);
				
		// 21. Sample transpose
		MVC_MyKnob3(models[21], gui[\sampleGUIScrollView],Rect(37, 293, 28, 28),gui[\knobTheme3]);
		
		// 22. Static Sample transpose
		MVC_MyKnob3(models[22], gui[\sampleGUIScrollView],Rect(96, 293, 28, 28),gui[\knobTheme3]);

		// code tab //////////////////
			
		gui[\codeGUIScrollView] = MVC_CompositeView(gui[\tabView].mvcTab(3),
												Rect(8,8,640-15,365-15-15))
			.color_(\background,Color(6/11,42/83,29/65))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);
		
		MVC_RoundBounds(gui[\tabView].mvcTab(3),Rect(8,8,640-15,365-15-15))
			.width_(4)
			.color_(\background, Color(6/11,42/83,29/65));
		
		// code view
		gui[\code]=MVC_TextView(gui[\codeGUIScrollView],codeModel,Rect(15-10,15,610,300-10))
			.label_("Code")
			.resize_(5)
			.attachCodeHelpFunction(window,30,75)
			.color_(\string,Color.black)
			.color_(\background,Color.white)
			.color_(\focus,Color(1,1,1,0.5))
			.colorizeOnOpen_(true)
			.autoColorize_(true);

		// error view
		gui[\error]=MVC_TextView(gui[\codeGUIScrollView],errorModel,
				Rect(5,310,370-35,25))
			.resize_(5)
			.color_(\string,Color.red)
			.color_(\background,Color.white)
			.color_(\focus,Color.clear);
			
		// 16. font size
		MVC_NumberBox(models[16], gui[\codeGUIScrollView], Rect(351,312,26,18), gui[\theme4])
			.rounded_(true);
		
		// this fixes background bug
		MVC_PlainSquare(gui[\codeGUIScrollView],Rect(530+38-10+58, 322-10, 10, 18))
			.color_(\on,Color.clear)
			.color_(\off,Color.clear);
			
		// build button
		MVC_FlatButton(gui[\codeGUIScrollView],Rect(530+38-10, 322-10, 58, 18),"Build")
			.rounded_(true)
			.canFocus_(false)
			.color_(\background,Color(1,1,1,0.3))
			.color_(\up,Color(1,1,1,0.3))
			.color_(\down,Color(1,1,1,0.6))
			.resize_(9)
			.action_{
				// fixes bug with copy & paste that doesn't update the code model
				var string = gui[\code].string;
				codeModel.string_(string);
				this.editString(string);
				this.guiEvaluate;
			};
			
		// auto build button
		MVC_OnOffView(gui[\codeGUIScrollView],Rect(530+38-10-40-3, 322-10, 35+3, 18),"Auto",
			models[14])
			.rounded_(true)
			.color_(\on,Color(1,0,0,1))
			.color_(\off,Color(1,1,1,0.3))
			.resize_(9);
		
		// help button
		MVC_FlatButton(gui[\codeGUIScrollView],Rect(530+38-65-10-40-3, 322-10, 58, 18),"Help")
			.rounded_(true)
			.canFocus_(false)
			.color_(\background,Color(1,1,1,0.3))
			.color_(\up,Color(1,1,1,0.3))
			.color_(\down,Color(1,1,1,0.6))
			.resize_(9)
			.action_{
				var string=gui[\code].selectedString.split($.)[0];
				if (string.asSymbol.asClass.isNil) {
					"How to program LNX_Studio".help
				}{
					string.help
				}
			};
			
		window.helpAction_{
			var string=gui[\code].selectedString.split($.)[0];
			if (string.asSymbol.asClass.isNil) {
				"How to program LNX_Studio".help
			}{
				string.help
			};
			false; // don't open studio help
		};
		
		// ugen help button
		MVC_FlatButton(gui[\codeGUIScrollView],Rect(465+38-65-10-40-3, 322-10, 58, 18),"UGens")
			.rounded_(true)
			.canFocus_(false)
			.color_(\background,Color(1,1,1,0.3))
			.color_(\up,Color(1,1,1,0.3))
			.color_(\down,Color(1,1,1,0.6))
			.resize_(9)
			.action_{ HelpBrowser.openBrowsePage("UGens") };
			
		// edit
		gui[\edit]=MVC_FlatButton(gui[\scrollView],Rect(618-2, 6, 19+2, 18),"+")
			.rounded_(true)
			.canFocus_(false)
			.color_(\up,Color(6/11,42/83,29/65))
			.color_(\down,Color(6/11,42/83,29/65)/2)
			.resize_(9)
			.action_{
				if (gui[\codeWindow].isNil) {
					
					// code window
					gui[\codeWindow] = MVC_Window(
						"SC Code",
						Rect(window.bounds.left+thisWidth-228,window.bounds.top+151,640,355))
						.color_(\background,Color(6/11,42/83,29/65))
						.minWidth_(237)
						.minHeight_(146)
						.toFrontAction_{
							studio.frontWindow_(gui[\codeWindow]); 
							studio.hilightInst(id);
						};
					
					studio.frontWindow_(gui[\codeWindow]); // force it

					// code view
					gui[\codeWindowText] = MVC_TextView(
									gui[\codeWindow],codeModel,Rect(15,15,610,300))
						.resize_(5)
						.attachCodeHelpFunction(gui[\codeWindow],5,10)
						.color_(\string,Color.black)
						.color_(\background,Color.white)
						.color_(\focus,Color(1,1,1,0.5))
						.colorizeOnOpen_(true)
						.autoColorize_(true);
						
					// error view
					gui[\error]=MVC_TextView(gui[\codeWindow],errorModel,
								Rect(15,15+300+5,410-40-35,30-5))
						.resize_(8)
						.color_(\string,Color.red)
						.color_(\background,Color.white)
						.color_(\focus,Color.clear);
						
					// 16. font size
					MVC_NumberBox(models[16], gui[\codeWindow],
										Rect(360,322,26,18), gui[\theme4])
						.resize_(9)
						.rounded_(true);
							
					// build button				
					MVC_FlatButton(gui[\codeWindow],Rect(530+38, 322, 58, 18),"Build")
						.rounded_(true)
						.canFocus_(false)
						.color_(\background,Color(1,1,1,0.3))
						.color_(\up,Color(1,1,1,0.3))
						.color_(\down,Color(1,1,1,0.6))
						.resize_(9)
						.action_{
							// fixes bug with copy & paste that doesn't update the code model
							var string = gui[\codeWindowText].string;
							codeModel.string_(string);
							this.editString(string);
							this.guiEvaluate;
						};
						
					// auto build button
					MVC_OnOffView(gui[\codeWindow],Rect(530+38-40-3, 322, 35+3, 18),"Auto",
						models[14])
						.rounded_(true)
						.color_(\on,Color(1,0,0,1))
						.color_(\off,Color(1,1,1,0.3))
						.resize_(9);
						
					// help button
					MVC_FlatButton(gui[\codeWindow],Rect(530+38-65-43, 322, 58, 18),"Help")
						.rounded_(true)
						.canFocus_(false)
						.color_(\background,Color(1,1,1,0.3))
						.color_(\up,Color(1,1,1,0.3))
						.color_(\down,Color(1,1,1,0.6))
						.resize_(9)
						.action_{
							var string=gui[\codeWindowText].selectedString.split($.)[0];
							if (string.asSymbol.asClass.isNil) {
								"How to program LNX_Studio".help
							}{
								string.help
							}
						};
						
					gui[\codeWindow].helpAction_{
						var string=gui[\codeWindowText].selectedString.split($.)[0];
						if (string.asSymbol.asClass.isNil) {
							"How to program LNX_Studio".help
						}{
							string.help
						};
						false; // don't open studio help
					};
						
					// ugen help button
					MVC_FlatButton(gui[\codeWindow],Rect(465+38-65-43, 322, 58, 18),"UGens")
						.rounded_(true)
						.canFocus_(false)
						.color_(\background,Color(1,1,1,0.3))
						.color_(\up,Color(1,1,1,0.3))
						.color_(\down,Color(1,1,1,0.6))
						.resize_(9)
						.action_{ HelpBrowser.openBrowsePage("UGens") };
						
					// color picker	
					gui[\colorPicker]=LNX_ColorPicker(
						Rect(window.bounds.left+thisWidth,window.bounds.top,410,125)
					);
						
					// and additions to color picker window
						
					// move
					gui[\move]=MVC_OnOffView(gui[\colorPicker].window,Rect(303,5,47,18),
										gui[\soloTheme])
						.value_(0)
						.rounded_(true)
						.permanentStrings_(["Move"])
						.action_{|me,val|
							gui[\resize].value_(0);
							if (me.value==1) {
								gui[\userGUIScrollView].editMode_(true);
								gui[\userGUIScrollView].editResize_(false);
							}{
								gui[\userGUIScrollView].editMode_(false);
								gui[\userGUIScrollView].editResize_(false);
							};
						};
						
					//resize
					gui[\resize]=MVC_OnOffView(gui[\colorPicker].window,
						Rect(354,5,47,18),gui[\soloTheme])
						.value_(0)
						.rounded_(true)
						.permanentStrings_(["Resize"])
						.action_{|me,val|
							gui[\move].value_(0);
							if (me.value==1) {
								gui[\userGUIScrollView].editMode_(true);
								gui[\userGUIScrollView].editResize_(true);
							}{
								gui[\userGUIScrollView].editMode_(false);
								gui[\userGUIScrollView].editResize_(false);
							};
							
						};
						
					// grid
					MVC_NumberCircle(gui[\colorPicker].window,Rect(277,5,20,18), gui[\theme3],
						[1,[1,50,\lin,1]].asModel.action_{|me,val|
							gui[\userGUIScrollView].grid_(val);
					}).label_("Grid").orientation_(\horiz);
					
					// change gui type
					gui[\guiType]=MVC_PopUpMenu3(gui[\colorPicker].window, Rect(17,5,135,18))
						.color_(\focus,Color.clear)
						.color_(\background,Color(0.65,0.65,0.65))
						.items_(guiTypesNames)
						.value_(0)
						.action_{|me|
							this.changeGUIType(me.value)
						};
						
					// set all
					gui[\setAll]=MVC_OnOffView(gui[\colorPicker].window,
						Rect(163,5,76,18),gui[\soloTheme])
						.color_(\off,Color(0.65,0.65,0.65))
						.value_(0)
						.rounded_(true)
						.permanentStrings_(["Set All"])
						.action_{|me,val|
							var key = gui[\colorPicker].key;
							var color = gui[\colorPicker].color;
							
							userViews.do{|view| view.colorWithAction_(key,color)};
							systemViews.do{|view| view.colorWithAction_(key,color)};
							
							gui[\setAll].value_(0);
							
						};
								
					gui[\codeWindow].create;

				}{
					gui[\codeWindow].front;	
					gui[\colorPicker].front;
				};
				
				gui[\tabView].value_(0);
				
			};
					
		// inst //
		
		gui[\seqGUIScrollView] = MVC_CompositeView(gui[\tabView].mvcTab(1),
												Rect(8,8,640-15,365-15-15))
			.color_(\background,Color(59/77,59/77,59/77))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);
		
		MVC_RoundBounds(gui[\tabView].mvcTab(1),Rect(8,8,640-15,365-15-15))
			.width_(4)
			.color_(\background, Color(59/77,59/77,59/77));
		
		
		sequencer.createWidgets(gui[\seqGUIScrollView],Rect(5,8,630-20+5,330-8),
						(\selectRect: Color.white,
						 \background: Color(6/11,42/83,29/65)*0.9,
						 \velocityBG: Color(3/77,1/103,0,65/77),
						 \buttons:    Color(6/11,42/83,29/65)*1.2,
						 \boxes:		Color(0.1,0.05,0,0.5)
						));

	}

	
}