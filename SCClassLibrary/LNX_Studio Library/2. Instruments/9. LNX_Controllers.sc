
// a virtual midi controller keyboard

LNX_Controllers : LNX_InstrumentTemplate {

	var keyboardView,keyboardOuterView,lastKeyboardNote;

	var launchPadNotes;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
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

			// 18. thru
			[0, \switch, (\strings_:"Thru"), midiControl, 18, "Thru",
				{|me,val,latency,send| this.setPVH(18,val,latency,send) }],

			// 19.network this
			[1, \switch, (\strings_:"Net"), midiControl, 19, "Network",
				{|me,val,latency,send| this.setPVH(19,val,latency,send) }],

			// 20-27.(return to world) learnable knob controls x8
			0,0,0,0,0,0,0,0,

			// 28-35.(return to world) learnable push button controls x8
			0,0,0,0,0,0,0,0,

		];

		8.do{|i|
			template[i+2]=[0, [0,127,\linear,0], ( label_:(i.asString), numberFunc_:\int),
				{|me,val| this.midiControlVP(i,val) }];

			template[i+10]=[0, \switch, ( strings_:(i+8).asString ),
				{ this.guiMidiControl(i+8,1) }];

			// return to world
			template[i+20]=[0, [0,127,\linear,0], midiControl, i+20, ( label_:(i.asString), numberFunc_:\int),
				{|me,val|
					p[i+20]=val;
					World_World.controlFromLNX(i,val/127,worldNumber)
			}];

			template[i+28]=[0, \switch,  midiControl, i+28,( strings_:(i+8).asString ),
				{|me,val|
					p[i+28]=val;
					World_World.controlFromLNX(i+8,val,worldNumber)
			}];

		};

		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1];
		autoExclusion=[0,1]; //=(2..17);

	}

	////////////////

	// override insts template to stop select preset when loading
	updateGUI{|tempP|
		tempP.do({|v,j| if (p[j]!=v) { models[j].lazyValue_(v,false) } });
		this.iUpdateGUI(tempP);
	}

	*thisWidth  {^320}
	*thisHeight {^370+50}

	createWindow{|bounds|
		this.createTemplateWindow(bounds,Color(0,1/103,9/77));
	}

	createWidgets{

		gui[\labelTheme]=(
			\font_	    	:  Font("Helvetica", 16,true),
			\align_		    :  \left,
			\shadow_		: false,
			\noShadows_  	: 0,
			\colors_		: (\string : Color.black));

		// the border and composite view
		gui[\compositeView] = MVC_RoundedComView(window,
				Rect(11,11,this.thisWidth-22,this.thisHeight-32-75))
				.color_(\border,  Color(59/108,65/103,505/692)  )
				.color_(\border2, Color(0,1/103,9/77))
				.color_(\background, Color(59/77,43/54,9/11)*1.1 );


		// 18 thru
		MVC_OnOffView(models[18],gui[\compositeView],Rect(60, 10, 40, 18),gui[\soloTheme  ])
			.rounded_(true)
			.color_(\on,Color(0.3,0.5,1)+0.3)
			.color_(\off,Color(0.3,0.5,1)+0.3/3)
			.font_(Font("Helvetica",11, true));

		// 19 net
		MVC_OnOffView(models[19],gui[\compositeView],Rect(10, 10, 28, 18),gui[\soloTheme  ])
			.rounded_(true)
			.color_(\on,Color(0.3,0.5,1)+0.3)
			.color_(\off,Color(0.3,0.5,1)+0.3/3)
			.font_(Font("Helvetica",11, true));

		// MIDI Settings
 		MVC_FlatButton(gui[\compositeView],Rect(124, 9, 43, 19),"MIDI")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(0.3,0.5,1)+0.3/2)
			.color_(\down,Color(0.3,0.5,1)+0.3/6)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ this.createMIDIInOutModelWindow(window,
				colors:(border1:Color(0,1/103,9/77,65/77), border2:Color(59/108,65/103,505/692))
			)};

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

		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\compositeView],10@284,187,
				Color(0.8,0.8,1)/1.6,
				Color(0.7,0.7,1)/3,
				Color(0.7,0.7,1)/1.5,
				Color(35/48,122/157,5/6),
				Color.black
			);
		this.attachActionsToPresetGUI;

		MVC_StaticText(gui[\compositeView], Rect(112, 37, 81, 18),gui[\labelTheme]).string_("To World");
		MVC_StaticText(gui[\compositeView], Rect(105, 160, 100, 18),gui[\labelTheme]).string_("From World");

		// controllers
		8.do{|i|
			// knobs
			MVC_MyKnob3(models[i+2],gui[\compositeView],Rect(10+3+(i*35)+((i/4).asInt*5),85+25,30,30))
				.color_(\on,Color(0.3,0.5,1))
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color(0.3,0.5,1)+0.3)
				.numberWidth_(-24);
			// buttons
			MVC_FlatButton2(models[i+10],gui[\compositeView],
										Rect(9+(i*35)+((i/4).asInt*5)+5,39+25,21,21))
				.font_(Font("Helvetica",12))
				.color_(\on,Color(0.3,0.5,1)+1)
				.color_(\off,Color.black)
				.color_(\background,Color(0.15,0.2,0.3)*2);

			// knobs
			MVC_MyKnob3(models[i+20],gui[\compositeView],Rect(10+(i*35)+((i/4).asInt*5),85+100+50,30,30))
				.color_(\on,Color(0.3,0.5,1))
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color(0.3,0.5,1)+0.3)
				.numberWidth_(-24);
			// buttons
			MVC_FlatButton2(models[i+28],gui[\compositeView],
										Rect(9+(i*35)+((i/4).asInt*5)+5,39+100+50,21,21))
				.font_(Font("Helvetica",12))
				.color_(\on,Color(0.3,0.5,1)+1)
				.color_(\off,Color.black)
				.color_(\background,Color(0.15,0.2,0.3)*2);

		};

		// the keyboard, fix bug so we don't need this scrollView
		gui[\keyboardOuterView]=MVC_CompositeView(window,Rect(6+5,298+9+30,305-10,75))
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
/*				if (key==\up   ) { this.guiSetProgram(p[18]-4) };
				if (key==\down ) { this.guiSetProgram(p[18]+4) };
				if (key==\left ) { this.guiSetProgram(p[18]-1) };
				if (key==\right) { this.guiSetProgram(p[18]+1) };*/
			};

	}

	////////////////////////

	iInitMIDI{ midi.putLoadList([0, 0]++LNX_MIDIPatch.nextUnusedOut) }

	externalIn{|index,value| models[index+2].lazyValueAction_(value) }

	// a quick way to get working, to be used by all midi in future
	pipeIn{|pipe|
		switch (pipe.kind)
		{\noteOn} { // noteOn
			var note = pipe.note;
			var vel  = pipe.velocity;
			var latency = pipe.latency;
			midi.noteOn(note,vel,latency);
			{keyboardView.setColor(note,Color(0.5,0.5,1),0.75);}.defer;
			if (p[19]==1) { api.send('netNoteOn',note,vel) };
		}
		{\noteOff} { // noteOff
			var note = pipe.note;
			var vel  = pipe.velocity;
			var latency = pipe.latency;
			midi.noteOff(note,vel,latency);
			{keyboardView.removeColor(note);}.defer;
			if (p[19]==1) { api.sendGD('netNoteOff',note,vel) };
		}
		{\control} { // control
			midi.control(pipe.num, pipe.val, pipe.latency)
		}
		{\touch} { // touch
			midi.touch(pipe.pressure, pipe.latency)
		}
		{\program} {
			// to do and confirm
		}
		{\bend} { // bend
			midi.bend(pipe.val, pipe.latency)
		}
	}

	touch  {|pressure,       latency| World_World.pipeIn( LNX_Touch  (pressure,      latency, \MIDIIn), worldNumber) }
	control{|num, val,       latency| World_World.pipeIn( LNX_Control(num,  val/127, latency, \MIDIIn), worldNumber) }
	bend   {|bend,           latency| World_World.pipeIn( LNX_Bend   (bend,          latency, \MIDIIn), worldNumber) }

	// midi note on
	noteOn{|note, vel, latency|
		if (p[18]==1) { midi.noteOn(note,vel,latency) };
		{keyboardView.setColor(note,Color(0.5,0.5,1),0.75);}.defer;
		if (p[19]==1) { api.send('netNoteOn',note,vel) };
		World_World.pipeIn( LNX_NoteOn (note, vel, latency, \MIDIIn), worldNumber );
	}

	netNoteOn{|note, vel|
		if (p[18].isTrue) {midi.noteOn(note,vel) };
		{keyboardView.setColor(note,Color(0.5,0.5,1),0.75);}.defer;
	}

	// midi note off
	noteOff{|note, vel, latency|
		if (p[18]==1) { midi.noteOff(note,vel,latency) };
		{keyboardView.removeColor(note);}.defer;
		if (p[19]==1) { api.sendGD('netNoteOff',note,vel) };
		World_World.pipeIn( LNX_NoteOff (note, vel, latency, \MIDIIn), worldNumber );
	}

	netNoteOff{|note, vel|
		if (p[18].isTrue) {midi.noteOff(note,vel)};
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
