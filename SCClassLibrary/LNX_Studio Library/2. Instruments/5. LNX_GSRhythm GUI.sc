
// a grain & sample based drum machine

+ LNX_GSRhythm {

	// GUI ////////////////////////////////////////////////////////////////////////////////
	
	*thisWidth  {^779}
	*thisHeight {^528}
	
	createWindow{|bounds| this.createTemplateWindow(bounds,Color(0,1/103,9/77,65/77),false)}
	
	arrangeWindow{
		if (p[10]==1) {
			window.setInnerExtent(thisWidth,thisHeight);
		}{
			window.setInnerExtent(thisWidth-486,thisHeight-298-8);
			if (window.isClosed) {
				gui[\compositeView].bounds_(
					Rect(11,11,this.thisWidth-508,this.thisHeight-328));
			};
		};
	}

	// create all the GUI widgets while attaching them to models
	
	createWidgets{
	
		var c, tab, sv, sv2, background, lastY, lastX, lastValue;
		
		gui[\text]=();
		
		gui[\textTime]=();
		
		// the themes and sub views //
		
		background=Color(35/48,122/157,5/6);
		
		gui[\menuTheme2 ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background : Color(29/267,7/54,23/77),
										\string: Color.white, \focus: Color.clear));
		
		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background : background
						
						, \focus: Color.clear));
						
		gui[\knobTheme1]=(\colors_		: (\on : Color(50/77,61/77,1), 
									   \numberUp:Color.black,
									   \numberDown:Color.white,
									   \disabled:Color.black),
						\numberFont_  : Font("Helvetica",11));
		
		gui[\knobTheme2]=(\colors_		: (\on :  Color(25/77,30/77,1), 
									   \numberUp:Color.black,
									   \numberDown:Color.white,
									   \disabled:Color.black,
									   \label:Color(0.75,0.75,1)),
						\numberFont_  : Font("Helvetica",10),
						\labelFont_	: Font("Helvetica",11)
						);
		
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));
		
		gui[\onOffTheme2]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color(50/77,61/77,1), \off: Color(0.4,0.4,0.4)));
						 
		gui[\onOffTheme1]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color(0.25,1,0.25), \off : Color(0.4,0.4,0.4)));
						 
		gui[\theme2]=(	\orientation_  : \horizontal,
						\resoultion_	 : 3,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",12),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.1,0.1,0.1,0.67),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.white,
										\focus : Color(0,0,0,0)));

		gui[\comTheme]= (	\border	:   Color(0.1725, 0.184, 0.307),
						\background : Color(33/74,48/97,57/86,28/51));
						
	
		// the border and composite view
		gui[\compositeView] = MVC_RoundedComView(window,
				Rect(11,11,this.thisWidth-22,this.thisHeight-22))
				.color_(\border,  Color(59/108,65/103,505/692)  )
				.color_(\border2, Color(0,1/103,9/77))
				.color_(\background, Color(59/77,43/54,9/11) );
	
		c= (channels+1).collect{|i|
		    Color(0,46/275,4/11,13/77)
		};
	
		// tab view
		gui[\tabView]=MVC_TabbedView(gui[\compositeView] ,Rect(10, 200, 676+22+30+10, 248+30+8))
			.tabPosition_('top')
			.labels_(((1..8).collect(_.asString)++["Overview"]))
			.tabCurve_(8)
			.tabWidth_(24!8++60)
			.unfocusedColors_(Color(511/1363,511/1363,30/77,53/77)!channels)
			.labelColors_(c)
			.backgrounds_(c)
			.font_(GUI.font.new("Helvetica",12))
			.tabHeight_(\auto)
			.value_(8)
			.action_{|me| gui[\tabView2].value_(me.value) }; //p[3]=me.value};
			
		// tab view 2
		gui[\tabView2]=MVC_TabbedView(gui[\compositeView] ,Rect(272, 15, 410+4+22+30+10, 201))
			.tabPosition_('top')
			.labels_(["All"]++((1..8).collect(_.asString)))
			.tabHeight_(0)
			.unfocusedColors_(Color(511/1363,511/1363,30/77,53/77)!channels)
			.labelColors_(c)
			.backgrounds_(c)
			.font_(GUI.font.new("Helvetica",12))
			.value_(8)
			.action_{|me| }; //p[3]=me.value};
			
		MVC_RoundEdges(gui[\compositeView] ,Rect(272, 5, 476, 10))
			.color_(\background,c[0]);
			
		MVC_RoundEdges(gui[\compositeView] ,Rect(10, 486, 738, 10))
			.color_(\background,c[0])
			.place_(\bottom);
		
		// mixer scroll view	
		gui[\mixerView]=MVC_CompositeView(gui[\compositeView] ,Rect(10, 10, 257, 185))
			.hasBorder_(false);
		
		// Sequencers //////////////////////////////
		
		MVC_PlainSquare(gui[\tabView].mvcTab(8),Rect(341, 0, 7, 14))
				.color_(\off, Color(0.1725, 0.184, 0.307));
		
		sv = MVC_RoundedComView(gui[\tabView].mvcTab(8), Rect(15,14, 709, 247), gui[\comTheme]);
		
		// sequencers
		sequencers.do{|s,i|
			
			s.createButtonWidgets(sv , Rect(64,15+(i*26), 652, 250));
			
			MVC_FlatButton(sv ,Rect(7, 25+(i*26) , 20, 20),"search")
				.color_(\up,background/3 )
				.color_(\down,background/3 )
				.color_(\string,Color.white)
				.rounded_(true)
				.mode_(\icon)
				.attachedDown_(s.nameWidget)
				.action_{ webBrowsers[i].open };

			MVC_FlatButton(sv ,Rect(32, 25+(i*26), 20, 20),"sine")
				.color_(\up,background/3 )
				.color_(\down,background/3 )
				.color_(\string,Color.white)
				.rounded_(true)
				.mode_(\icon)
				.attachedDown_(s.nameWidget)
				.action_{
					var temp;
					temp = userBanks[i].openMetadataEditor(
						window.view,p[108+i], true, webBrowsers[i],
						(
							border2: 	Color(59/108,65/103,505/692),
							border1: 	Color(0,1/103,9/77),
						)
					);
					lastMetaWindow[i] = temp[0];
					temp[1].selectMeFunc_(temp[2]);
				};

			MVC_PlainSquare(gui[\tabView].mvcTab(i),Rect(341, 0, 7, 14))
				.color_(\off, Color(0.1725, 0.184, 0.307));
	
			sv2=(\sv++i).asSymbol;
	
			gui[sv2] = 
				MVC_RoundedComView(gui[\tabView].mvcTab(i),
					Rect(15,14, 677+32, 247), gui[\comTheme]);
					
			// the sequencer
			s.createRoundWidgets(gui[sv2],Rect(25,35,652,105),
				(\hilite:Color.green, \on:Color(0.5,1,0.5)),
				'outer');
					
			// the modulation sequencers
			modSequencers[i].createRoundWidgets(gui[sv2],Rect(25, 140, 652, 105),
				(\hilite:Color(0,0,1) , \on:Color(0,0.5,1))
			);
				
		};
				
		// move all <->	
		MVC_UserView(gui[\tabView].mvcTab(8),Rect(618,18,35,20))
			.drawFunc_{|me|
				if (lastValue==nil) {Color.black.set} {Color.white.set};
				DrawIcon( \lineArrow, Rect(-3,-1,me.bounds.width+2,me.bounds.height+2) , pi);
				DrawIcon( \lineArrow, Rect(3,-1,me.bounds.width+2,me.bounds.height+2) );
			}
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				lastX=x;
				lastValue=0;
				me.refresh;
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var val = (x-lastX).div(15);
				var offset = (lastValue-val);
				lastValue = val;
				if (offset!=0) {
					sequencers.do{|seq| seq.move(offset) };
					modSequencers.do{|seq| seq.move(offset) };
				};
				me.refresh;
			}
			.mouseUpAction_{|me|
				lastX=nil;
				lastValue=nil;
				me.refresh;
			};
		
		// ruler
		MVC_StaticText(gui[\tabView].mvcTab(8),Rect(654,21-2,20,16))
			.align_(\center)
			.shadowDown_(false)
			.alwaysDown_(true)
			.font_(Font("Helvetica",12))
			.string_("R")
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				lastY=y;
				lastValue=0;
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var val = (y-lastY).div(10);
				var offset = (lastValue-val);
				lastValue = val;
				sequencers.do{|seq| seq.addValueToSP(4,offset)};
			};
			
		// speed
		MVC_StaticText(gui[\tabView].mvcTab(8),Rect(675,21-2,20,16))
			.align_(\center)
			.shadowDown_(false)
			.alwaysDown_(true)
			.font_(Font("Helvetica",12))
			.string_("Sp")
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				lastY=y;
				lastValue=0;
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var val = (y-lastY).div(10);
				var offset = (lastValue-val);
				lastValue = val;
				sequencers.do{|seq| seq.addValueToSP(6,offset)};
			};
			
		// steps	
		MVC_StaticText(gui[\tabView].mvcTab(8),Rect(696,21-2,20,16))
			.align_(\center)
			.shadowDown_(false)
			.alwaysDown_(true)
			.font_(Font("Helvetica",12))
			.string_("St")
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				lastY=y;
				lastValue=0;
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var val = (y-lastY).div(10);
				var offset = (lastValue-val);
				lastValue = val;
				sequencers.do{|seq| seq.addValueToSP(3,offset)};
			};
				
		// master MIXER //////////////////////////////////////////////////////////////////////
		
		// 1.on/off
		MVC_OnOffView(models[1],gui[\mixerView] ,Rect( 206, 1,22,18),gui[\onOffTheme1])
			.rounded_(true)
			.permanentStrings_(["On"]);
			
		// 0.solo
		MVC_OnOffView(models[0],gui[\mixerView] ,Rect( 231, 1,20,18),gui[\soloTheme  ])
			.rounded_(true);
		
		// 2. master volume
		MVC_SmoothSlider(gui[\mixerView],models[2],Rect(213,39,30,100))
			.numberFont_(Font("Helvetica",11))
			.color_(\knob,Color(1,1,1,86/125))
			.color_(\hilite,Color(0,0,0,0.5))
			.color_(\numberUp,Color.black)
			.color_(\numberDown,Color.white);
			
		// 4.master pan
		MVC_MyKnob3(models[4],gui[\mixerView],Rect(204, 157, 26, 26),gui[\knobTheme1])
			.showNumberBox_(false);
			//.penWidth_(2.5);
			
		// 10.hide
		MVC_OnOffView(models[10], gui[\mixerView],Rect(235, 162, 19, 16),gui[\onOffTheme2])
			.rounded_(true)
			.strings_(["-","+"]);	

		// MASTER inst tab/////////////////////////////////////////////////////////////////////
		
		tab=gui[\tabView2].mvcTab(8);

		MVC_Text(tab,Rect(217, 157, 254, 44))
				.align_(\center)
				.shadow_(false)
				.penShadow_(true)
				.font_(Font("AvenirNext-Heavy",28))
				.string_("GS Rhythm Box");
		
		// MIDI Settings
 		MVC_FlatButton(tab,Rect(10, 1, 43, 19),"MIDI")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(10665/19997,223/375,2/3) )
			.color_(\down,Color(10665/19997,223/375,2/3) )
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ this.createMIDIInOutModelWindow(window,low:7,high:8,
				colors:(border1:Color(0,1/103,9/77,65/77) , border2:Color(59/108,65/103,505/692))
			) };
			
		// MIDI Control
 		MVC_FlatButton(tab,Rect(63, 1, 43, 19),"Cntrl")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(10665/19997,223/375,2/3) )
			.color_(\down,Color(10665/19997,223/375,2/3))
			.color_(\string,Color.white)
			.resize_(9)
			.action_{  LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front  };
			
		// the preset interface
		presetView=MVC_PresetMenuInterface(tab,120@2,168-78,
				Color(0.8,0.8,1)/1.6,
				Color(0.7,0.7,1)/3,
				Color(0.7,0.7,1)/1.5,
				background,
				Color.black
			);
		this.attachActionsToPresetGUI;		

		// 287. master send channel
		MVC_PopUpMenu3(models[287],tab,Rect(395-80,2,70,17),gui[\menuTheme  ]);
			
		// 3.output channels
		MVC_PopUpMenu3(models[3],tab,Rect(395,2,70,17),gui[\menuTheme  ]);
		
		
		
		
		
		
				
		// 9.master bp adjust (-1,1)
		MVC_MyKnob3(models[9],tab, Rect(185, 173, 28, 28),gui[\knobTheme1])
			.showNumberBox_(false);
		

//		MVC_NumberBox(models[9], tab, Rect(185, 173, 28, 22), gui[\theme2])
//			.orientation_(\horizontal)
//			.moveRound_(0.01);
		
		
		
		sv = MVC_RoundedComView(tab, Rect(16-2,33, 140, 60), gui[\comTheme] );
		
		// 6.master pitch
		MVC_MyKnob3(models[6],sv, Rect(5+(50*0), 17, 28, 28),gui[\knobTheme1]);
		
		// 5.master duration
		MVC_MyKnob3(models[5],sv, Rect(5+(50*1),17, 28, 28),gui[\knobTheme1]);

		// 288. master send amp
		MVC_MyKnob3(models[288],sv, Rect(5+(50*2),17, 28, 28),gui[\knobTheme1]);
		
		
		///////////
		
		MVC_PlainSquare(tab,Rect(79, 99, 7, 16))
				.color_(\off, Color(0.1725, 0.184, 0.307));

		// the border and composite view
		sv = MVC_RoundedComView(tab, Rect(16-2,115, 140, 80), gui[\comTheme] );
			
		// set Drum Kit buttons 
		['505','606','707','808','909','Rand'].do{|s,i|
			MVC_FlatButton(sv,Rect(7+(44*(i%3)), 3+(27*((i/3).asInt)), 39, 19),
								 s.asString)
				.rounded_(true)
				.canFocus_(false)
				.color_(\up,Color(50/77,61/77,1) )
				.color_(\down,Color(50/77,61/77,1) )
				.color_(\string,Color.black)
				.resize_(9)
				.action_{ this.setDrumKit(s) };	
		};
		
		// mutate
 		MVC_FlatButton(sv,Rect(24, 56, 47, 19),"Mutate")
			.rounded_(true)
			.canFocus_(false)
			.color_(\up,background/3 )
			.color_(\down,background/3 )
			.color_(\string,Color.white)
			.resize_(9)
			.action_{  this.mutate   };
				
		// mutate amount
		MVC_NumberCircle(mutateModel, sv, Rect(78, 54, 25, 23), gui[\theme2]);
		
		// Master Filter //////////////////////////////////////////////////////////////////
		
		MVC_PlainSquare(tab,Rect(337, 85, 11, 6))
			.color_(\off, Color(0.1725, 0.184, 0.307));
				
		// the border and composite view
		sv = MVC_RoundedComView(tab, Rect(355-2, 33, 110, 115), gui[\comTheme] );
				
		// 11. Master filter on/off	
		MVC_OnOffView(models[11], sv,Rect(5,10, 40, 18),gui[\onOffTheme2])
			.rounded_(true);
			
		// 232. Master filter type 0=lp, 1=hp
		MVC_OnOffView(models[232], sv,Rect(10, 35, 28, 16),gui[\onOffTheme2])
			.rounded_(true);
			
		// 233. Master filt freq
		MVC_MyKnob3(models[233], sv, Rect(12, 73, 28, 28),
			gui[\knobTheme1]);
			
		// 234. Master filt res
		MVC_MyKnob3(models[234], sv, Rect(65, 73, 28, 28),
			gui[\knobTheme1]);	
			
		// 235. Master filter Drive
		MVC_MyKnob3(models[235], sv, Rect(65, 16, 28, 28),
			gui[\knobTheme1]);
		
		// enabled adaptor for master filter on/off
		MVC_FuncAdaptor(models[11])
			.func_{|me,value|
				value=(value==1);
				models[232].enabled_(value);
				models[233].enabled_(value);
				models[234].enabled_(value);
				models[235].enabled_(value);
			}
			.freshAdaptor;
		
		// enabled adaptor for master filter type
		MVC_FuncAdaptor(models[232])
			.func_{|me,value|
				if (value==0) {
					models[233].dependantsPerform(\zeroValue_,20);
				}{
					models[233].dependantsPerform(\zeroValue_,20000);
				};
			}
			.freshAdaptor;
	
		// Master grains  //////////////////////////////////////////////////////////////////
		
		MVC_PlainSquare(tab,Rect(157, 59, 11, 6))
			.color_(\off, Color(0.1725, 0.184, 0.307));
		
		// master grain scroll view	
		sv = MVC_RoundedComView(tab, Rect(176-2, 33, 159, 115), gui[\comTheme] );
		
		// 228. Master grain on/off
		MVC_OnOffView(models[228], sv,Rect(5, 20, 48, 18),gui[\onOffTheme2])
			.rounded_(true);
		
		// enabled adaptor for master grain on/off
		MVC_FuncAdaptor(models[228])
			.func_{|me,value|
				value=(value==1);
				models[229].enabled_(value);
				models[230].enabled_(value);
				models[231].enabled_(value);
				models[260].enabled_(value);
			}
			.freshAdaptor;
		
		// 229. Master grain stretch
		MVC_MyKnob3(models[229], sv, Rect(15, 73, 28, 28),gui[\knobTheme1]);
		
		// 230. Master grain density
		MVC_MyKnob3(models[230], sv, Rect(70, 73, 28, 28),gui[\knobTheme1]);
		
		// 231. Master grain random
		MVC_MyKnob3(models[231], sv, Rect(70, 16, 28, 28),gui[\knobTheme1]);
		
		// 260. Master grain overlap
		MVC_MyKnob3(models[260], sv, Rect(120,73,28,28),gui[\knobTheme1]);
			
			
		// channel tabs & mixer ////////////////////////////////////////////////
		
		channels.do{|i|
			
			// Channel MIXER /////////////////////////////////////////////////////////////
			
			// 20-27. channel onOff	
			MVC_OnOffView(models[20+i], gui[\mixerView],Rect(3+(i*25),0,19,16),gui[\onOffTheme2])
				.rounded_(true);
			
			// 12-19. channel solo
			MVC_OnOffView(models[12+i], gui[\mixerView],Rect(3+(i*25),19,19,16),gui[\soloTheme])
				.rounded_(true);
			
			// 28-35 channel volumes
			MVC_SmoothSlider(gui[\mixerView],models[28+i],Rect(3+(i*25),39,20,100))
				.showNumberBox_(false)
				.color_(\hilite,Color(0,0,0,0.5))
				.color_(\knob,Color(50/77,61/77,1));
			
			// 44-51. channel pan
			MVC_MyKnob3(models[44+i],gui[\mixerView],Rect(2+(i*25),143, 21,21),gui[\knobTheme1])
				.showNumberBox_(false)
				.penWidth_(2.5);
				
			// lamps
			gui[\lamps][i]=MVC_LampView(gui[\mixerView],Rect(5+(i*25),167,15,15))
				.color_(\on,Color.green)
				.color_(\off,Color.green/8)
				.action_{|me| this.bang(i,100/127) };	
				
			// CHANNEL tabs ///////////////////////////////////////////////////////////////
			
			tab =  gui[\tabView2].mvcTab(i);

			// tab2 labels
			gui[\text][i] = MVC_StaticText(gui[(\sv++i).asSymbol],Rect(302, 5, 340, 21),"")
				.align_(\left)	
				.font_(SCFont("Arial", 14))
				.shadowDown_(false)
				.color_(\stringDown,Color.black)
				.action_{webBrowsers[i].open };	
			
			
//			
//			// back
//			MVC_FlatButton(gui[(\sv++i).asSymbol] ,Rect(60, 8 , 20, 20),"back")
//				.color_(\up,background/3 )
//				.color_(\down,background/3 )
//				.color_(\string,Color.white)
//				.rounded_(true)
//				.mode_(\icon)
//				.attachedDown_(gui[\text][i])
//				.action_{
//					gui[\tabView].value_(8);
//					gui[\tabView2].value_(8);
//				};	
//			
			
			
			// search the web button
			MVC_FlatButton(gui[(\sv++i).asSymbol] ,Rect(250, 8 , 20, 20),"search")
				.color_(\up,background/3 )
				.color_(\down,background/3 )
				.color_(\string,Color.white)
				.rounded_(true)
				.mode_(\icon)
				.attachedDown_(gui[\text][i])
				.action_{ webBrowsers[i].open };		
				
				
			// metadata editor
			MVC_FlatButton(gui[(\sv++i).asSymbol] ,Rect(275, 8 , 20, 20),"sine")
				.mode_(\icon)
				.color_(\up,background/3 )
				.color_(\down,background/3 )
				.color_(\string,Color.white)
				.rounded_(true)
				.attachedDown_(gui[\text][i])
				.action_{
					var temp;
					temp = userBanks[i].openMetadataEditor(
						window.view,p[108+i], true, webBrowsers[i],
						(
							border2: 	Color(59/108,65/103,505/692),
							border1: 	Color(0,1/103,9/77),
						)
					);
					lastMetaWindow[i] = temp[0];
					temp[1].selectMeFunc_(temp[2]);
				};
					
			// time of sample
			gui[\textTime][i]  = MVC_StaticText(gui[(\sv++i).asSymbol],Rect(588, 5, 100, 21),"")
				.align_(\right)
				.shadow_(false)
				.color_(\string,Color.black)
				.font_(SCFont("Arial", 11));

			MVC_StaticText(gui[(\sv++i).asSymbol],Rect(652+10, 43+10, 20, 70))
				.align_(\rotate)
				.rotate_(0.5pi)
				.font_(Font("Helvetica",12))
				.string_("Velocity");
								
			MVC_StaticText(gui[(\sv++i).asSymbol],Rect(652+10, 147+10, 20, 70))
				.align_(\rotate)
				.rotate_(0.5pi)
				.font_(Font("Helvetica",12))
				.string_("Modulaton");
				
			// 100-107. channel bank
			MVC_PopUpMenu3(models[100+i],tab, Rect(10, 2, 59, 17),gui[\menuTheme ]);

			// 108-115. channel sample
			MVC_PopUpMenu3(tab, Rect(75, 2, 112, 17),gui[\menuTheme  ])
				.items_(channelBanks[i][p[100+i]].names)
				.model_(models[108+i]);	
				
							
			// 116-123. channel bp on/off
			MVC_OnOffView(models[116+i], tab, Rect(219, 1, 19, 19),gui[\onOffTheme2])
				.rounded_(true);	
				
				
				
							
			// 289-296. static or random sample
			MVC_OnOffView(models[289+i], tab, Rect(193, 1, 19, 19),gui[\onOffTheme2])
				.rounded_(true);	
				
				
				
				
				
			
			MVC_FuncAdaptor(models[108+i])
				.func_{|me,value|
					var text = channelBanks[i][p[100+i]].names.wrapAt(value)??{""};
					var text2= text.select{|i| i.isAlpha};
					var time = channelBanks[i][p[100+i]].samples.wrapAt(value);
					
					sequencers[i].name_((i+1).asString++"."++text2);
					
					if (time.notNil) {
						if (time.duration.notNil) {
							time=time.duration.round(0.01);
						}{
							time="";
						};
						gui[\text][i].string_(text);
						
						gui[\textTime][i].string_(time++" secs");
						
					}{
						gui[\text][i].string_("");
					};
				}.freshAdaptor;

			// 76-83. channel send channels
			MVC_PopUpMenu3(models[76+i],tab, Rect(170+135+10, 2, 70, 17),gui[\menuTheme  ])
				.orientation_(\horizontal);	
				
			// 36-43. channel out channel (master vs individual)
			MVC_PopUpMenu3(models[36+i],tab, Rect(395, 2, 70, 17),gui[\menuTheme  ])
				.orientation_(\horizontal);	

				
			
				
			// 92-99. channel choke
			MVC_NumberCircle(models[92+i], tab, Rect(290-15+10, 0, 22, 22), gui[\theme2])
				.orientation_(\horizontal)
				.resoultion_(0.5);	
				
				
				
			// sample control scroll view	
			
						
						
			sv = MVC_RoundedComView(tab,Rect(16-2, 33,140, 162), gui[\comTheme]);
					
			
				
				
			// 68-75. channel pitch
			MVC_MyKnob3(models[68+i], sv, Rect(5, 17, 28, 28),gui[\knobTheme1]);
			
			// 196-203. channel mod : pitch
			MVC_MyKnob3(models[196+i], sv, Rect(9, 131, 20, 20),gui[\knobTheme2]);
			
			// 52-59. channel duration
			MVC_MyKnob3(models[52+i], sv, Rect(55, 17, 28, 28),gui[\knobTheme1]);
			
			// 204-211. channel mod : duration
			MVC_MyKnob3(models[204+i], sv, Rect(59, 131, 20, 20),gui[\knobTheme2]);

			// 277-84. channel attack
			MVC_MyKnob3(models[277+i], sv, Rect(9, 74, 28, 28),gui[\knobTheme1]);

			
			// 60-67. channel decay
			MVC_MyKnob3(models[60+i], sv, Rect(55, 74, 28, 28),gui[\knobTheme1]);
			
			// 252-275. channel velocity
			MVC_MyKnob3(models[252+i], sv, Rect(105, 74, 28, 28),gui[\knobTheme1]);
			

			
			// 84-91. channel send amps
			MVC_MyKnob3(models[84+i], sv, Rect(105, 17, 28, 28),gui[\knobTheme1]);
			
			// 212-219. channel mod : send
			MVC_MyKnob3(models[212+i], sv, Rect(109, 131, 20, 20),gui[\knobTheme2]);
			
			
			
			// CHANNEL FILTER scroll view ////////////////////////////////////////////////////
			
			MVC_PlainSquare(tab,Rect(337, 116, 11, 6))
				.color_(\off, Color(0.1725, 0.184, 0.307));
					
			// filter scroll view			
			sv = MVC_RoundedComView(tab,Rect(355-2, 33, 110, 162), gui[\comTheme]);
								
		
			// 236-243. channel filter on/off
			MVC_OnOffView(models[236+i], sv,Rect(5, 10, 40, 18),gui[\onOffTheme2])
				.rounded_(true);
				
			// 188-195. channel type 0=lp, 1=hp
			MVC_OnOffView(models[188+i], sv,Rect(10, 35, 28, 16),gui[\onOffTheme2])
				.rounded_(true);
				
			// 124-131. channel filt freq
			MVC_MyKnob3(models[124+i], sv, Rect(12, 73, 28, 28),
				gui[\knobTheme1]);
				
			// 132-139. channel filt res
			MVC_MyKnob3(models[132+i], sv, Rect(65, 73, 28, 28),
				gui[\knobTheme1]);	
			// 140-147. channel filter Drive
			MVC_MyKnob3(models[140+i], sv, Rect(65, 16, 28, 28),
				gui[\knobTheme1]);
			
			// 244-251. channel mod : filter freq
			MVC_MyKnob3(models[244+i], sv, Rect(17, 130, 20, 20),
				gui[\knobTheme2]);
				
				
				
			
			// enabled adaptor for master filter on/off
			MVC_FuncAdaptor(models[236+i])
				.func_{|me,value|
					value=(value==1);
					models[188+i].enabled_(value);
					models[124+i].enabled_(value);
					models[132+i].enabled_(value);
					models[140+i].enabled_(value);
					models[244+i].enabled_(value);
					//models[277+i].enabled_(value);
				}
				.freshAdaptor;
			
			// enabled adaptor for master grain on/off
			MVC_FuncAdaptor(models[188+i])
				.func_{|me,value|
					if (value==0) {
						models[124+i].dependantsPerform(\zeroValue_,20);
					}{
						models[124+i].dependantsPerform(\zeroValue_,20000);
					};
				}
				.freshAdaptor;
			
			// CHANNEL GRAINS  ////////////////////////////////////////////////////////////////
			
			// grain scroll view				
	
			MVC_PlainSquare(tab,Rect(157, 116, 11, 6))
				.color_(\off, Color(0.1725, 0.184, 0.307));
	
			sv = MVC_RoundedComView(tab,Rect(176-2, 33, 159, 162), gui[\comTheme]);
					
			
			
			// 148-155. channel grain on/off
			MVC_OnOffView(models[148+i], sv, Rect(5, 20, 48, 18), gui[\onOffTheme2])
				.rounded_(true);
				
			// enabled adaptor for channel grain on/off
			MVC_FuncAdaptor(models[148+i])
				.func_{|me,value|
					value=(value==1);
					models[156+i].enabled_(value);
					models[164+i].enabled_(value);
					models[180+i].enabled_(value);
					models[220+i].enabled_(value);
					models[172+i].enabled_(value);
					models[261+i].enabled_(value);
					models[269+i].enabled_(value);
				}
				.freshAdaptor;
			
			// 156-163. channel grain stretch
			MVC_MyKnob3(models[156+i], sv, Rect(15, 73, 28, 28), gui[\knobTheme1]);
			
			// 164-171. channel grain density
			MVC_MyKnob3(models[164+i], sv, Rect(70, 73, 28, 28), gui[\knobTheme1]);
			
			// 180-187. channel grain random
			MVC_MyKnob3(models[180+i], sv, Rect(70, 16, 28, 28), gui[\knobTheme1]);
			
			// 220-227. channel mod : stretch
			MVC_MyKnob3(models[220+i], sv, Rect(19, 130, 20, 20), gui[\knobTheme2]);
			
			// 172-179.  channel mod : grain density
			MVC_MyKnob3(models[172+i], sv, Rect(74, 130, 20, 20), gui[\knobTheme2]);
				
			// 261-268. grain overlap
			MVC_MyKnob3(models[261+i], sv, Rect(120,73,28,28),gui[\knobTheme1]);
			
			// 269-276.  channel mod : grain overlap
			MVC_MyKnob3(models[269+i], sv, Rect(124, 130, 20, 20), gui[\knobTheme2]);	
		
		};
		
	}
	
} // end ////////////////////////////////////
