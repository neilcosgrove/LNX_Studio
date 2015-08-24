
// a virtual midi controller keyboard

LNX_Controllers : LNX_InstrumentTemplate {

	var keyboardView,keyboardOuterView,lastKeyboardNote;
	
	var <midi2, launchPadNotes, when;

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
		\netNoteOn, \netNoteOff, \netMidiProgram, \netMidiControl, \netMidiControlVP
	]}
	
	initModel {

		var template =[
			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me| this.onOffAlt(me.value) }],
			
			// 2-9. knob controls x8	
			0,0,0,0,0,0,0,0, 
			
			// 10-17. push button controls x8
			0,0,0,0,0,0,0,0, 
			
			// 18.program number (current) - auto works on this
			[-1, [-1,127,\lin,1,1], midiControl, 18, "Program",
				{|me,val,latency,send| this.guiSetProgram(val,false,latency) }],
			
			// 19.network this
			[1, \switch, (\strings_:"Net"),
				{|me,val,latency,send| this.setPVH(19,val,latency,send) }],
				
			// 20.all
			[0,\switch,(\strings_:"All"),
				{|me,val,latency,send| this.setPVH(20,val,latency,send) }],
				
			// 21.quant on
			[1,\switch,(\strings_:["Quant","Quant"]),
				{|me,val,latency,send| this.setPVH(21,val,latency,send) }],
			
			// 22. quant on steps
			[32,[1,512,\lin,1,1],(\strings_:"Steps"),
				{|me,val,latency,send| this.setPVH(22,val,latency,send) }],
				
			// 23.program number (to become)
			[-1, [-1,127,\lin,1,1],
				{|me,val,latency,send| this.guiToBecome(val,latency) }],
				
			// 24.reset beats on program change
			[1,\switch,(\strings_:["Reset","Reset"]),
				{|me,val,latency,send| this.setPVH(24,val,latency,send) }],
				
		];
		
		8.do{|i|
			template[i+2]=[0, [0,127,\linear,1], ( label_:(i.asString), numberFunc_:\int),
				{|me,val| this.midiControlVP(i,val) }];
			
			template[i+10]=[0, \switch, ( strings_:(i+8).asString ),
				{ this.guiMidiControl(i+8,1) }];
		};

		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,18,23];
		randomExclusion=[0,1,18,23];
		autoExclusion=[23];
		
	}

	// gui call: "to become program"
	guiToBecome{|val,latency|
		var beat=studio.instBeat;
		var quant=p[22]*6;
		if (p[21].isTrue) {
			// quantise timing
			when=(beat.div(quant)+1)*quant;
		}{
			// do now
			when=beat+1;
		};	
	}

	// program changes are synced to clock events
	clockIn3{|beat,absTime,latency|
		if (beat==when) {
			// special method to avoid latency adjustments in automation
			models[18].lazyValueActionDoAutoBeat_(models[23].value,latency,true,beat:beat);
			models[23].lazyValue_(-1,false);
			if (p[24].isTrue) { studio.resetInstBeat };
			when = nil;
		};
		if ((when.notNil)&&(beat%12==0)) {
			gui[\program].flash_((beat%24).clip(0,1))
		};
	}

	// gui set the program
	guiSetProgram{|val,updateGUI=true,latency|	
		if (p[20]==1) {
			studio.guiAllInstsSelectProgram(val)
		}{
			this.midiProgram(val,latency)
		};
		p[18]=val;
		if (updateGUI) { {models[18].value_(val)}.defer } ;
	}
	
	// midi program in
	program{|program  , latency|
		program=program.clip(0,127);
		this.midiProgram(program);
		{models[18].value_(program)}.defer;
	}
	
	// used by keyboard arrow keys and external midi in
	setProgram{|program|
		program=program.clip(0,127);
		this.midiProgram(program);
		models[18].value_(program);
	}
	
	midiProgram{|program,latency|
		p[18]=program;
		midi.program(program,latency); // midi control out
		api.sendOD('netMidiProgram',program,midi.uidOut,midi.midiOutChannel);
	}
	
	// this just updates gui and any external midi out
	// program changes will be transmitted within each instrument
	netMidiProgram{|program,uidOut,midiOutChannel|
		p[18]=program;
		models[18].lazyValue_(program,false);
		// only if uid's & channels don't match then send midi program out to catch other equipment
		if (((uidOut==(midi.uidOut))and:{midiOutChannel==(midi.midiOutChannel)}).not) {
			midi.program(program);
		};
	}
	

	guiAddPreset{} // not for this
	selectProgram{} // not for this
	


	////////////////

	
	noteOn2{|note, vel,  latency|
		
		var preset;
		
		{
		preset=launchPadNotes.indexOf(note.asInt); 
		
		if (preset.notNil) {	
			midi2.noteOn(launchPadNotes[p[18].abs],0,0);
			this.guiToBecome(preset);
			midi2.noteOn(note,3,0);
		
		};
//		if (note==8) {this.guiAddP};
		}.defer;
	}
	
	// override insts template to stop select preset when loading 
	updateGUI{|tempP|
		tempP.do({|v,j| if (p[j]!=v) { models[j].lazyValue_(v,false) } });
		this.iUpdateGUI(tempP);
	}
	
	
	*thisWidth  {^400}
	*thisHeight {^205+15+25}
	
	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(0.5,0.5,1.9,0.5)/10);
	}

	createWidgets{
	
		// 19 net 
		MVC_OnOffView(models[19],window,Rect(10, 10, 28, 18),gui[\soloTheme  ])
			.rounded_(true)
			.color_(\on,Color(0.3,0.5,1)+0.3)
			.color_(\off,Color(0.3,0.5,1)+0.3/3)
			.font_(Font("Helvetica-Bold",11));


		// 21 quant on 
		MVC_OnOffView(models[21],window,Rect(150, 35, 75, 20),gui[\soloTheme  ])
			.rounded_(true)
			.color_(\on,Color(0.3,0.5,1)+0.3)
			.color_(\off,Color(0.3,0.5,1)+0.3/3)
			.font_(Font("Helvetica-Bold",11));


		// 22.quant steps
		MVC_NumberBox(models[22], window,Rect(233, 35, 30, 20))
			.resoultion_(5);


		// 24.reset steps			
		MVC_OnOffView(models[24],window,Rect(100, 35, 45, 20),gui[\soloTheme  ])
			.rounded_(true)
			.color_(\on,Color(0.3,0.5,1)+0.3)
			.color_(\off,Color(0.3,0.5,1)+0.3/3)
			.font_(Font("Helvetica-Bold",11));
			

		// 20. all inst 
		MVC_OnOffView(models[20],window,Rect(295, 10, 24, 18),gui[\soloTheme  ])
			.rounded_(true)
			.color_(\on,Color(0.3,0.5,1)+0.3)
			.color_(\off,Color(0.3,0.5,1)+0.3/3)
			.font_(Font("Helvetica-Bold",11));
			
		// add preset button
		MVC_FlatButton(window,Rect(324,10,66,18)).strings_("Add Preset")
			.canBeHidden_(false)
			.font_(Font("Helvetica-Bold",11))
			.rounded_(true)
			.color_(\background,Color.black)
			.color_(\up,Color(0.3,0.5,1)+0.3)
			.color_(\down,Color(0.3,0.5,1)+0.3/2)
			.action_{	 this.guiAddP };
	
		// midi
		midi.createOutMVUA (window, (14+30)@10, false, 130);
		midi.createOutMVUB (window, (230-50-30+30)@10);
		
		
		MVC_RoundButton(window,Rect(240, 9, 43, 19))
			.states_([ [ "Cntrl", Color(1.0, 1.0, 1.0, 1.0), Color(0.15,0.15,0.3) ] ])
			.canFocus_(false)
			.font_(Font("Helvetica",12))
			.action_{ LNX_MIDIControl.editControls(studio); LNX_MIDIControl.window.front };
	
		// controllers
		
		8.do{|i|
			// knobs
			MVC_MyKnob(models[i+2],window,Rect(10+(i*35)+((i/4).asInt*5),106,30,30))
				.color_(\on,Color(0.3,0.5,1)+0.3)
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color(0.3,0.5,1)+0.3)
				.numberWidth_(-24);
			// buttons
			MVC_FlatButton2(models[i+10],window,Rect(10+(i*35)+((i/4).asInt*5)+5,65,19,19))
				.font_(Font("Helvetica",11))
				.color_(\on,Color(0.3,0.5,1)+1)
				.color_(\off,Color.black)
				.color_(\background,Color(0.15,0.2,0.3)*1.1);
		};
		
		
		// 23.program number (to become)
		gui[\program]=MVC_ProgramChange(models[23],window,Rect(295,33,95,90+25))
			.color_(\on,Color(0.3,0.5,0.8)*0.8);
			
		// 18.program number	
		MVC_FuncAdaptor(models[18]).func_{|me,value| gui[\program].actualProgram_(value) };
			
	
		// the keyboard, fix bug so we don't need this scrollView
		gui[\keyboardOuterView]=MVC_CompositeView(window,Rect(5,128+25,390,85))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);
			
		keyboardView=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0,0,390,85),3,24)
			.keyboardColor_(Color(0.3,0.5,1)+0.3)
			.keyDownAction_{|note|
				lastKeyboardNote=note;
				midi.noteOn(note,100);
				if (p[19]==1) {
					api.sendND('netNoteOn',note,100);
				}
			}
			.keyUpAction_{|note|
				midi.noteOff(note,100);
				if (p[19]==1) {
					api.sendGD('netNoteOff',note,100);
				}
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
	

	iInitMIDI{
		midi.putLoadList([0, -1 ]++LNX_MIDIPatch.nextUnusedOut);
		
		midi2=LNX_MIDIPatch(1,0,0,0);
		
		midi2.putLoadList([ 1180758771, 0, 1514112147, 0 ]);
		
		midi2.noteOnFunc  = {|src, chan, note, vel ,latency|
			this.noteOn2 (note, vel,  latency)};
		
		launchPadNotes=#[0, 1, 2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 20, 21, 22, 23, 32, 33, 34, 35,
						36, 37, 38, 39, 48, 49, 50, 51, 52, 53, 54, 55, 64, 65, 66, 67, 68,
						69, 70, 71, 80, 81, 82, 83, 84, 85, 86, 87, 96, 97, 98, 99,
						 100, 101, 102, 103, 112, 113, 114, 115, 116, 117, 118, 119];
		
	}
	
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
	
	
	

	
	// gui add preset
	guiAddP{
		if (p[20]==1) {
			studio.guiAllInstsAddPreset	
		}{
			midi.internal(\addPrest);
		}	
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
	
	// this is called by the studio for the zeroSL auto map midi controller
	autoMap{|num,val|
		var vPot;
		vPot=(val>64).if(64-val,val);
			
		if (((num-8)>=0)and:{(num-8)<=7}) {
			this.midiControlVP(num-8,val);
			{models[num-8+2].value_(val)}.defer;
		};
		
		if (((num-32)>=0)and:{(num-32)<=7}) {
			val=val/127;
			if (val==0) {this.guiMidiControl(num-32+8,127)};
			{models[num-32+2+8].value_(val)}.defer;
			
		};	
		
	}
	
}
