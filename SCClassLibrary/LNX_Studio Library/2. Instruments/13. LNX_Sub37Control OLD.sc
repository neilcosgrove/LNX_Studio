/*











NOT USED















*/
// a virtual controller from the Moog Sub37

LNX_Sub37Control : LNX_InstrumentTemplate {

	var <noControl, keyboardView, lastKeyboardNote;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName  {^"Moog Sub37"}
	*sortOrder   {^2}
	mixerColor   {^Color(0.3,0.75,1,0.2)} // colour in mixer
	isMIDI       {^true}
	isFX         {^false}
	isInstrument {^false}
	clockPriority{^2} // sequenced preset changes before any note generation
	canTurnOnOff {^false} // this may change with audio in

	*isVisible{^false} // this instrument is depreciated 

	header { 
		instrumentHeaderType="SC Sub37 Doc";
		version="v1.1";
	}
	
	// an immutable list of methods available to the network
	interface{^#[\netMidiControlVP]}
	
	initModel {

		var template =[
		
			0,0,
		
			// 0.solo
//			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
//				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
//				\action2_ -> {|me| this.soloAlt(me.value) }],
//			
//			// 1.onOff
//			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
//				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
//				\action2_ -> {|me| this.onOffAlt(me.value) }],
			
			// 2. bank
			[0,[0,16,\lin,1], midiControl, 2, "Bank",{|me,value,latency,send,toggle|
				this.setODModel(2,value,latency,send); // network must come 1st
				this.setMoogProgram(latency);
			}],
			
			// 3. preset
			[0,[0,16,\lin,1], midiControl, 3, "Preset",{|me,value,latency,send,toggle|
				this.setODModel(3,value,latency,send); // network must come 1st
				this.setMoogProgram(latency);
			}],
			
			// 4. send program
			[1, \switch, midiControl, 4, "Send Program",{|me,value,latency,send,toggle|
//				this.setODModel(4,value,latency,send); // network must come 1st
			},(\strings_:"Prg")],
			
			// 5. send controls
			[0, \switch, midiControl, 5, "Send Controls",{|me,value,latency,send,toggle|
//				this.setODModel(5,value,latency,send); // network must come 1st
//				if (value.isTrue) {
//					presetExclusion=[0,1,4,5];
//				}{
//					presetExclusion=[0,1,4,5]++((1..Sub37.size)+5);
//				}	
			},(\strings_:"Cntr")],
		
		];
		
		// add all the sub37 controls
		noControl = Sub37.size;
		
		noControl.do{|i|
			template= template.add([0, [0,127,\linear,1], midiControl, i+6, Sub37.nameAt(i),
				( label_:(Sub37.nameAt(i)),numberFunc_:\int),
				{|me,val,latency| this.midiControlVP(i,val,latency) }]);
		};

		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,2,3,4,5]++((1..Sub37.size)+5);
		randomExclusion=[0,1,2,3,4,5];
		autoExclusion=[2,3,4,5];
		
	}
	
	// this give LNX_POP the names of the 256 presets as bank & preset...  B_ P_
	popItems{
		var list = [] ;
		 16.do{|i| 16.do{|j| list=list.add( "B" ++ (i+1) ++ " " ++ "P" ++ (j+1) )}};
		 ^list;
	}
	
	midiSelectProgram{} // this needs to be disabled for everything
	
	program{|progam,latency| } // will this be useful? not 14bit from sub37
	
	// this only comes from POP
	popSelectProgram{|prog,latency|
		p[2]=prog.div(16);
		p[3]=prog%16;
		models[2].lazyValue_(p[2],false);
		models[3].lazyValue_(p[3],false);
		this.setMoogProgram(latency);
	}
	
	// set program on moog, latency isn't really needed here
	setMoogProgram{|latency|
		var prog=	(p[2]*16)+p[3];
		midi.control(32, prog.div(128), latency);
		midi.program(prog % 128, latency);
	}
	
	// any post midiInit stuff
	iInitMIDI{ midi.findByName("Moog Sub 37","Moog Sub 37") }

	// override insts template to stop select preset when loading 
	updateGUI{|tempP|
		// dont do action, do i want this?
		tempP.do({|v,j| if (p[j]!=v) { models[j].lazyValue_(v,false) } });
		this.iUpdateGUI(tempP);
	}
	
	*thisWidth  {^1047}
	*thisHeight {^410}
	
	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(0.185, 0.045, 0.045 , 1)*0.66);
	}

	createWidgets{


		gui[\scrollTheme]=( \background	: Color(0.25, 0.25, 0.25),
				 \border		: Color(0.6 , 0.562, 0.5));

		gui[\plainTheme]=( colors_: (\on	: Color(0,0,0),
				 		\off	: Color(0,0,0)));	
				 		
		MVC_UserView.new(window,Rect(5,5,thisWidth-10,15))
			.canFocus_(false)
			.resize_(1)
			.drawFunc_{|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				l=thisRect.left;
				t=thisRect.top;
				w=thisRect.width;
				h=thisRect.height;
				Pen.use{
					Pen.smoothing_(true);			
					Pen.fillColor_(Color(0.6 , 0.562, 0.5).set);
					Pen.fillRect(Rect(h,0,w-(h*2),h));
					Pen.color_(Color(0.6 , 0.562, 0.5));
					Pen.addWedge(h@h, h, 2pi*1.5, 2pi*0.25); // top left
					Pen.addWedge((w-h)@h, h, 2pi*1.75, 2pi*0.25); // top right
					Pen.perform(\fill);
				}; // end.pen
			};	
				 		
				 		

		gui[\scrollView] = MVC_RoundedComView(window,
							Rect(11,21,thisWidth-22,thisHeight-132-1), gui[\scrollTheme]);
							
		MVC_StaticText(window,Rect(14,1,thisWidth-10, 25 ))
			.shadow_(false)
			.font_(Font("Helvetica",11))
			.color_(\string,Color.black)
			.string_("Arpeggiator        Glide                      Mod 1                               Mod 2                            Oscillators                         Mixer                               Filter                                               Envelope Generators")  ;

		// controllers
		
		MVC_PlainSquare(gui[\scrollView], Rect(64,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(128,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(249,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(374,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(499,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(602,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(732,0,5,277), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(0,217,735,5), gui[\plainTheme]);
		
		noControl.do{|i|

			// buttons
			if (Sub37.kindAt(i)==\button) {
				MVC_OnOffView(models[i+6],gui[\scrollView],Sub37.rectAt(i),Sub37.nameAt(i))
					.rounded_(true)
					.onValue_(127)
					.showNumberBox_(false)
					.label_(nil)
					.font_(Font("Helvetica-Bold", 10))
					.color_(\on,Color.orange)
					.color_(\off,Color(0.4,0.4,0.4));
			};
			
			// knobs
			if (Sub37.kindAt(i)==\knob) {
				MVC_MyKnob3(models[i+6],gui[\scrollView],Sub37.rectAt(i))
					.labelFont_(Font("Helvetica",11))
					.color_(\on,Color.orange)
					.color_(\numberUp,Color.black)
					.color_(\numberDown,Color.orange)
					.numberWidth_(-24);
			};
			
			// sliders
			if ((Sub37.kindAt(i)==\slider)or:{Sub37.kindAt(i)==\sliderH}) {
				MVC_SmoothSlider(models[i+6],gui[\scrollView],Sub37.rectAt(i))
					.labelFont_(Font("Helvetica",11))
					.orientation_( (Sub37.kindAt(i)==\slider).if(\vertical,\horizontal  ))
					.numberWidth_(-30)
					.color_(\knob,Color.orange)
					.color_(\numberUp,Color.black)
					.color_(\numberDown,Color.orange);
			};
			
			// numberBox
			if (Sub37.kindAt(i)==\numberBox) {
				MVC_NumberBox(models[i+6],gui[\scrollView],Sub37.rectAt(i))
					//.labelFont_(Font("Helvetica",11))
					.label_(nil)
					.color_(\background,Color(0,0,0,0.5))
					.color_(\string,Color.white)
					.numberWidth_(-24);
			};
				
		};
		
		MVC_ProgramChangeMoog(gui[\scrollView], models[2], Rect(230, 229, 320, 19))
			.color_(\background,Color(0.2,0.2,0.2))
			.color_(\on,Color(0.5,0.5,0.5));
		MVC_ProgramChangeMoog(gui[\scrollView], models[3], Rect(230, 252, 320, 19))
			.color_(\background,Color(0.2,0.2,0.2))
			.color_(\on,Color(0.5,0.5,0.5));
		
		// MIDI Settings
 		MVC_FlatButton(gui[\scrollView],Rect(585, 241, 43, 19),"MIDI")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(0.55, 0.5, 0.45))
			.color_(\down,Color(0.55, 0.5, 0.45)/2)
			.color_(\string,Color.white)
			.action_{ this.createMIDIInOutModelWindow(window,nil,nil,(
				background:Color(63/77,59/77,59/77),
				border2:Color(7/11,42/83,29/65),
				border1:Color(3*3/77,1/103,0,65/77)
			))};
	
		// MIDI Controls
	 	MVC_FlatButton(gui[\scrollView],Rect(654, 241, 43, 19),"Cntrl")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(0.55, 0.5, 0.45))
			.color_(\down,Color(0.55, 0.5, 0.45)/2)
			.color_(\string,Color.white)
			.action_{ LNX_MIDIControl.editControls(studio); LNX_MIDIControl.window.front };




		// the keyboard, fix bug so we don't need this scrollView
		gui[\keyboardOuterView]=MVC_CompositeView(window,Rect(12,310,1020,93))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);
			
		keyboardView=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0,0,1020,93),8,12)
			.keyboardColor_(Color(1,0.5,0)+0.3)
			.keyDownAction_{|note|
				lastKeyboardNote=note;
				midi.noteOn(note,100);
			}
			.keyUpAction_{|note|
				midi.noteOff(note,100);

			}
			.keyTrackAction_{|note|
				midi.noteOn(note,100);
				midi.noteOff(lastKeyboardNote,100);
				lastKeyboardNote=note;
			};
	
		
	}
	
	////
	
	
	// midi note on
	noteOn{|note, vel, latency|
		//midi.noteOn(note,vel,latency);
		{keyboardView.setColor(note,Color(0.5,0.5,1),0.75);}.defer;
	}
	
	// midi note off
	noteOff{|note, vel, latency|
		//midi.noteOff(note,vel,latency);
		{keyboardView.removeColor(note);}.defer;
	}
	
	////////////////////////
	
	// should i use midi pipes here?
	control{|num,  val, latency|
		var index = Sub37.keys.indexOf(num.asInt);
		
		// drop out if volume because sub37 gives a midi feedback loop for some reason
		if (num==7) {^this}; 
		
		if (index.notNil) {
			models[index+6].lazyValue_(val,true);
			p[index+6]=val;
		};
	}	
	
	// set control
	midiControlVP{|item,value,latency|
		p[item+6]=value;
		midi.control(Sub37.keyAt(item),value,latency,false,true); // midi control out
		api.sendVP((id++"_ccvp_"++item).asSymbol,
					'netMidiControlVP',item,value,midi.uidOut,midi.midiOutChannel);
	}
	
	// net version of above
	netMidiControlVP{|item,value,uidOut,midiOutChannel|
		p[item+6]=value;
		models[item+6].lazyValue_(value,false);
		midi.control(Sub37.keyAt(item) ,value,nil,false,true);
		// ignore set to true so no items learnt from this
	}
	
}
