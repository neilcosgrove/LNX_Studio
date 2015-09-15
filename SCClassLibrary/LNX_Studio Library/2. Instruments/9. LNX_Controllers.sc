
// a virtual midi controller keyboard

LNX_Controllers : LNX_InstrumentTemplate {

	var keyboardView,keyboardOuterView,lastKeyboardNote;
	
	var launchPadNotes;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id;
		^super.new(server,studio,instNo,bounds,open,id)
	}

	*studioName {^"Keyboard & Controls"}
	*sortOrder{^2}

	mixerColor{^Color(0.3,0.75,1,0.2)} // colour in mixer
	isMIDI{^true}
	isFX{^false}
	isInstrument{^false}
	canTurnOnOff{^false}
	clockPriority{^1} // sequenced preset changes before any note generation

	header { 
		instrumentHeaderType="SC Controllers Doc";
		version="v1.1";
	}
	
	// an immutable list of methods available to the network
	interface{^#[
		\netNoteOn, \netNoteOff, \netMidiControl, \netMidiControlVP
	]}
	
	initModel {

		var template =[
			// 0.solo
			[0, \switch, (\strings_:"S"), 
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), 
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me| this.onOffAlt(me.value) }],
			
			// 2-9. knob controls x8	
			0,0,0,0,0,0,0,0, 
			
			// 10-17. push button controls x8
			0,0,0,0,0,0,0,0, 
			
			// 18. empty
			0, 
			
			// 19.network this
			[1, \switch, (\strings_:"Net"), midiControl, 19, "Network",
				{|me,val,latency,send| this.setPVH(19,val,latency,send) }],
				
			// 20-24. empty
			0, 0, 0, 0, 0
				
		];
		
		8.do{|i|
			template[i+2]=[0, [0,127,\linear,1], ( label_:(i.asString), numberFunc_:\int),
				{|me,val| this.midiControlVP(i,val) }];
			
			template[i+10]=[0, \switch, ( strings_:(i+8).asString ),
				{ this.guiMidiControl(i+8,1) }];
		};

		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1];
		autoExclusion=[0,1]; //=(2..17);
		
	}

	guiAddPreset{} // not for this

	selectProgram{} // not for this
	
	////////////////
	
	// override insts template to stop select preset when loading 
	updateGUI{|tempP|
		tempP.do({|v,j| if (p[j]!=v) { models[j].lazyValue_(v,false) } });
		this.iUpdateGUI(tempP);
	}
	
	*thisWidth  {^320}
	*thisHeight {^240}
	
	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(0,1/103,9/77));
	}

	createWidgets{
	
		// the border and composite view
		gui[\compositeView] = MVC_RoundedComView(window,
				Rect(11,11,this.thisWidth-22,this.thisHeight-32-75))
				.color_(\border,  Color(59/108,65/103,505/692)  )
				.color_(\border2, Color(0,1/103,9/77))
				.color_(\background, Color(59/77,43/54,9/11)*1.1 );
	
	
		// 19 net 
		MVC_OnOffView(models[19],gui[\compositeView],Rect(10, 10, 28, 18),gui[\soloTheme  ])
			.rounded_(true)
			.color_(\on,Color(0.3,0.5,1)+0.3)
			.color_(\off,Color(0.3,0.5,1)+0.3/3)
			.font_(Font("Helvetica-Bold",11));

		// MIDI Settings
 		MVC_FlatButton(gui[\compositeView],Rect(124, 9, 43, 19),"MIDI")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(0.3,0.5,1)+0.3/2)
			.color_(\down,Color(0.3,0.5,1)+0.3/6)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ this.createMIDIInOutModelWindow(window)};
		
		// midi controls
		MVC_FlatButton(gui[\compositeView],Rect(242, 9, 43, 19),"Cntrl")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(0.3,0.5,1)+0.3/2)
			.color_(\down,Color(0.3,0.5,1)+0.3/6)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ 
				LNX_MIDIControl.editControls(studio); LNX_MIDIControl.window.front
			};
	
		// controllers
		8.do{|i|
			// knobs
			MVC_MyKnob3(models[i+2],gui[\compositeView],Rect(10+(i*35)+((i/4).asInt*5),85,30,30))
				.color_(\on,Color(0.3,0.5,1))
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color(0.3,0.5,1)+0.3)
				.numberWidth_(-24);
			// buttons
			MVC_FlatButton2(models[i+10],gui[\compositeView],
										Rect(9+(i*35)+((i/4).asInt*5)+5,39,21,21))
				.font_(Font("Helvetica",12))
				.color_(\on,Color(0.3,0.5,1)+1)
				.color_(\off,Color.black)
				.color_(\background,Color(0.15,0.2,0.3)*2);
		};
		
		// the keyboard, fix bug so we don't need this scrollView
		gui[\keyboardOuterView]=MVC_CompositeView(window,Rect(6+5,148+9,305-10,75))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);
			
		keyboardView=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0,0,305-10,75),3,24)
			.keyboardColor_(Color(0.3,0.5,1)+0.3)
			.keyDownAction_{|note|
				lastKeyboardNote=note;
				midi.noteOn(note,100);
				if (p[19]==1) { api.sendND('netNoteOn',note,100) }
			}
			.keyUpAction_{|note|
				midi.noteOff(note,100);
				if (p[19]==1) { api.sendGD('netNoteOff',note,100) }
			}
			.keyTrackAction_{|note|
				midi.noteOn(note,100);
				midi.noteOff(lastKeyboardNote,100);
				if (p[19]==1) {
					api.send('netNoteOn',note,100);
					api.sendGD('netNoteOff',lastKeyboardNote,100);
				};
				lastKeyboardNote=note;
			}.miscKeyAction_{|key|
				if (key==\up   ) { this.guiSetProgram(p[18]-4) };
				if (key==\down ) { this.guiSetProgram(p[18]+4) };
				if (key==\left ) { this.guiSetProgram(p[18]-1) };
				if (key==\right) { this.guiSetProgram(p[18]+1) };
			};
	
	}
	
	////////////////////////
	
	iInitMIDI{ midi.putLoadList([0, 0]++LNX_MIDIPatch.nextUnusedOut) }
	
	// midi note on
	noteOn{|note, vel, latency|
		midi.noteOn(note,vel,latency);
		{keyboardView.setColor(note,Color(0.5,0.5,1),0.75);}.defer;
		if (p[19]==1) {
			api.send('netNoteOn',note,vel);
		}
	}
	
	netNoteOn{|note, vel|
		midi.noteOn(note,vel);
		{keyboardView.setColor(note,Color(0.5,0.5,1),0.75);}.defer;
	}	
	
	// midi note off
	noteOff{|note, vel, latency|
		midi.noteOff(note,vel,latency);
		{keyboardView.removeColor(note);}.defer;
		if (p[19]==1) {
			api.sendGD('netNoteOff',note,vel);
		};
	}
	
	netNoteOff{|note, vel|
		midi.noteOff(note,vel);
		{keyboardView.removeColor(note);}.defer;
	}
	
	
	// midiControl or learn the item and send over network (for buttons)
	guiMidiControl{|item,value|
		var name;
		if	(midi.isNextControlLearn) {
			name=midi.learn(item,value); // midi control learn
			// no networking here, done inside newMidiControlAdded via LNX_MIDIControl 
		}{				
			value = (p[item+2]>0.5).if(0,1);	
			p[item+2]=value;
			{models[item+2].value_(value)}.defer;
			name=midi.control(item,value*127,nil,false,true); // midi control out
												// (item,value,latency,send,ignore)
			api.sendOD('netMidiControl',item,value,midi.uidOut,midi.midiOutChannel);
		};
	}
	
	// net version of above
	netMidiControl{|item,value,uidOut,midiOutChannel|
		p[item+2]=value;
		models[item+2].lazyValue_(value,false);
		// ignore is set to true so no items learnt from this
		midi.control(item,value*127,nil,false,true);
	}
	
	// as above but uses VP, (for knobs), VP not used for buttons because of toggle
	midiControlVP{|item,value|
		var name;
		p[item+2]=value;
		if	(midi.isNextControlLearn) {
			name=midi.learn(item,value); // midi control learn
			// no networking here, done inside newMidiControlAdded via LNX_MIDIControl 
		}{
			name=midi.control(item,value,nil,false,true); // midi control out
					// (item,value,latency,send,ignore)
					
			// activate this later, when finished with cc sequencing
			api.sendVP((id++"_ccvp_"++item).asSymbol,
						'netMidiControlVP',item,value,midi.uidOut,midi.midiOutChannel);
		};
	}
	
	// net version of above
	netMidiControlVP{|item,value,uidOut,midiOutChannel|
		p[item+2]=value;
		models[item+2].lazyValue_(value,false);
		midi.control(item,value,nil,false,true); // ignore set to true so no items learnt from this
	}
	
}
