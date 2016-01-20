////////////////////////////////////////////////////////////////////////////////////////////////////
// GUI code for LNX_Code                                                                           .

+ LNX_CodeFX {
	
	*thisWidth  {^263}
	*thisHeight {^302-12}
	
	createWindow{|bounds|
		this.createTemplateWindow(bounds,Color(3/77,1/103,0,65/77), resizable:true);
		window.minWidth_(263).minHeight_(150)
	}
	
	createTemplateWindow{|argBounds,background,resizable=false,scroll=false|
		bounds = bounds ? argBounds;
	//	bounds = bounds.setExtent(thisWidth,thisHeight);
		background=background?Color.grey;
		window = MVC_Window(((this.instNo+1).asString++"."+this.name),
					bounds, resizable: resizable, scroll: scroll);
		window.color_(\background,background);
		
		window.resizeAction_{|me|
			if ((p[16]!=me.bounds.width) or:{p[17]!=me.bounds.height} ) {
			 
				p[16]=me.bounds.width;
				models[16].value_(p[16]);
				p[17]=me.bounds.height;
				models[17].value_(p[17]);
				
				api.sendVP(\netBounds,\netBounds,p[16],p[17]);
				
			}
		};
	}
	
	netBounds{|w,h|
		p[16]=w;
		models[16].value_(p[16]);
		p[17]=h;
		models[17].value_(p[17]);
		{ window.setInnerExtentSuppressResizeAction(p[16],p[17]) }.defer; // this will feedback ???
	}

	// create all the GUI widgets while attaching them to models
	createWidgets{
		
		var background=Color(59/77,59/77,59/77);
		
		var thisWidth = window.bounds.width;
		var thisHeight = window.bounds.height;
		
		
		gui[\scrollView] = MVC_RoundedComView(window,
									Rect(11,11,thisWidth-22,thisHeight-23))
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
		
						 
		gui[\knobTheme1]=(\colors_		: (\on : Color(1,71/129,1), 
									   \numberUp:Color(50/77,1,61/77),
									   \numberDown:Color.black),
						\labelFont_   : Font("Helvetica",12),
						\numberFont_  : Font("Helvetica",11));
						 
		gui[\knobTheme2]=(\colors_		: (\on : Color(1,71/129,1), 
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

		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\layout_      : \reversed,
						\colors_      : (\background : background*1.5, \focus: Color.clear));
		
		// the tab view
		
		gui[\userGUIScrollView] = MVC_ScrollView(window,
												Rect(14,50,thisWidth-28,thisHeight-90))
			.color_(\background,Color(6/11,42/83,29/65))
			.resize_(5)
			.hasBorder_(false)
			.autoScrolls_(true)
			.hasHorizontalScroller_(true)
			.hasVerticalScroller_(true)
			.autohidesScrollers_(true);

		// master

		// 2. master volume
		systemViews[\volume] = 
			MVC_MyKnob3(models[2],gui[\userGUIScrollView],Rect(181,20,26,26),gui[\knobTheme2])
			.boundsAction_{|view,rect| api.sendVP("nBV",\netBoundsSystem,\volume,*rect.storeArgs)}
			.mouseDownAction_{|me| this.selectGUI(me,systemViews,\volume,\system) }
			.colorAction_{|view,key,color|
				api.sendVP("2_color",\netColorSystem,\volume,key, *color.storeArgs);
			};

		// 10. sendAmp
		systemViews[\sendAmp] =
			MVC_MyKnob3(models[10],gui[\userGUIScrollView],Rect(103,20,26,26),gui[\knobTheme2])
			.boundsAction_{|view,rect|
				api.sendVP("10_bounds",\netBoundsSystem,\sendAmp,*rect.storeArgs)}
			.mouseDownAction_{|me| this.selectGUI(me,systemViews,\sendAmp,\system) }
			.colorAction_{|view,key,color|
				api.sendVP("10_color",\netColorSystem,\sendAmp,key, *color.storeArgs);
			};

		// 15. inAmp
		systemViews[\inAmp] =
			MVC_MyKnob3(models[15],gui[\userGUIScrollView],Rect(26,20,26,26),gui[\knobTheme2])
			.boundsAction_{|view,rect|
				api.sendVP("15_bounds",\netBoundsSystem,\inAmp,*rect.storeArgs)}
			.mouseDownAction_{|me| this.selectGUI(me,systemViews,\inAmp,\system) }
			.colorAction_{|view,key,color|
				api.sendVP("15_color",\netColorSystem,\inAmp,key, *color.storeArgs);
			};
			
		// 3.output channels
		MVC_PopUpMenu3(models[3],gui[\scrollView],Rect(161,5,70,17),gui[\menuTheme  ]);

		MVC_PlainSquare(gui[\scrollView],Rect(84,5,70,17))
			.color_(\on,Color(0,0,0,0.2))
			.color_(\off,Color(0,0,0,0.2));		
			
		// 9. sendOut	
		gui[\sendOut] =
			MVC_PopUpMenu3(models[9],gui[\scrollView],Rect(84,5,70,17),gui[\menuTheme  ]);

		// 11. in	
		gui[\in] =
			MVC_PopUpMenu3(models[11],gui[\scrollView],Rect(7,5,70,17),gui[\menuTheme  ]);

		///////////

		// MIDI Control
 		gui[\midi] = MVC_FlatButton(gui[\scrollView],Rect(6, thisHeight-47, 43, 19),"Cntrl")
			.rounded_(true)
			.resize_(7)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(6/11,42/83,29/65))
			.color_(\down,Color(6/11,42/83,29/65)/2)
			.color_(\string,Color.white)
			.action_{  LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front  };
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],(54)@(thisHeight-46),62,
				Color(6/11,42/83,29/65),
				Color.black,
				Color(6/11,42/83,29/65),
				background,
				Color.black,
				resize:7
			);
		this.attachActionsToPresetGUI;
			
		// code tab //////////////////
		
		// edit
		gui[\edit]=MVC_FlatButton(gui[\scrollView],Rect(215, thisHeight-46, 21, 18),"+")
			.mode_(\icon)
			.rounded_(true)
			.resize_(7)
			.canFocus_(false)
			.color_(\up,Color(6/11,42/83,29/65))
			.color_(\down,Color(6/11,42/83,29/65)/2)
			.action_{
				if (gui[\codeWindow].isNil) {
					
					// code window
					gui[\codeWindow] = MVC_Window(
						"SC Code FX",
						Rect(window.bounds.left+thisWidth-223-5,window.bounds.top+151,640,355)
					
						)
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
						
					// 18. font size
					MVC_NumberBox(models[18], gui[\codeWindow], Rect(360,322,26,18),
											gui[\theme4])
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
							if (this.guiEvaluate) { this.startDSP };
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
				
			};
					

	}

	
}