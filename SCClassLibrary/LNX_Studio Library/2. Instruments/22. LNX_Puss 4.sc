//////////////////////////////
// a puss 4 control patcher //
//////////////////////////////
/*

TO DO

Can output both midi cc & note  (each control has a cc index and can be set with previous memory
Can turn off program up/down
Fade In & Out are registered models with -ive indexs
Nice GUI
Text labels showing whats learned
network it

//////////

LNX_Puss4Patch.allHIDs;
LNX_Puss4Patch.paths;
LNX_Puss4Patch.verbose_(false);
LNX_Puss4Patch.addAction('USB_054c_09cc_1a120000',{|index, value| [index, value].postln});
LNX_Puss4Patch.addAction(1,{|index, value| [index, value].postln}); // 0 is none
LNX_Puss4Patch.restartAll; // if connection is lost, you can run this again

(
~midi = LNX_MIDIPatch(1,0,1,2);
LNX_Puss4Patch.addAction(1,{|index, value| ~midi.control(index, value * 127, nil, send:true , ignore:false)});
)

[0]  // square
[1]  // X
[2]  // O
[3]  // Triangle
[4]  // L1 (on/off)
[5]  // R1 (on/off)
[6]  // L2 (pos) / L2 [DISABLED]
[7]  // R2 (pos) / R2 [DISABLED]
[8]  // Share
[9]  // Options
[10] // L3
[11] // R3
[12] // PS button
[13] // Track PAD button
[14] // L Joy L-R
[15] // L Joy U-D
[16] // R Joy L-R
[17] // R Joy U-D
[18] // DPad LEFT / urdlc (0, 0.28571429848671, 0.57142859697342, 0.85714286565781, 1.1428571939468]
[19] // DPad RIGHT / ? Noisey. [DISABLED]

[20] // DPad UP / L2 (pos) [MOVED 6]
[21] // DPad DOWN / R2 (pos) [MOVED 7]

128/20 = 6

Sets for 6 are..
-> [  0, 19 ]
-> [ 20, 39 ]
-> [ 40, 59 ]
-> [ 60, 79 ]
-> [ 80, 99 ]
-> [ 100, 119 ]

use [18] - up, down. inc dec prog
*/

LNX_Puss4Patch{

	classvar <>verbose = false;

	classvar <allHIDs, <paths, <pathAsStrings, <dependants, <exclude, <reverse, <resolution, <off;
	classvar <elementNames, <noElements;

	*learnOn { resolution = 0.25 }
	*learnOff{ resolution = 0.01 }

	*initClass{
		Class.initClassTree(HID);
		{ 1.wait; this.restartAll }.fork; // everything else needs a chance to start 1st
		elementNames = ["Square", "Cross","Circle", "Triangle", "L1", "R1", "L2", "R2", "Share", "Options", "L3", "R3",
			"PS Button", "TPad Button", "LJoy L&R", "LJoy U&D", "RJoy L&R", "RJoy U&D", "DPad Left", "DPad Right",
			"DPad Up", "DPad Down"];
		noElements=elementNames.size;
	}

	*restartAll{
		allHIDs		= IdentityDictionary[]; // only & all ps4 controllers
		dependants	= dependants ? IdentityDictionary[]; // make a dict for dependants if it doesn't already exist
		exclude		= [6,7,19]; // raw in to exclude
		reverse		= [15,17];	// reverse the joy's up & down
		resolution	= 0.01;		// mininum resolution
		off			= 0.03;		// off threshold
		{
			var deviceNo=1;
			if (HID.running) { HID.closeAll; 1.wait }; // this should stop 2 on 1 device when no. devces>1
			HID.findAvailable;	// this will init HID
			1.wait;
			HID.available.do{|info,i|
				if (info.vendorName  =="Sony Interactive Entertainment") {
					var path         = info.path.asSymbol;
					var hid          = HID.openPath(path: (path.asString) ); 	 // the device
					var lastValue    = IdentityDictionary[];
					var lastDPad	 = 4;									 	 // last position the dPad was in

					if (hid.notNil) {
						var idName		   = ("Puss_"++deviceNo).asSymbol;		 // make a name
						deviceNo		   = deviceNo + 1;
						allHIDs[idName]    = hid;
						dependants[idName] = dependants[idName] ? IdentitySet[]; // dependants that get updates
						// for the 1st 22 elements
						22.do{|index|
							lastValue[index] = inf;
							if (exclude.includes(index).not) {
								// actions for each element...
								hid.elements[index].action = {|i,element|
									var value = element.value;
									var pass  = false;
									var newIndex;
									if (((value-0.5).abs < off)and:{(index>=14)&&(index<=17)}) { // Joys are off
										value=0.5;
										if ((lastValue[index] - value).abs >= resolution) { pass = true };
									}{
										if (index==20) { newIndex=6 }; // move L2 from 20 to 6
										if (index==21) { newIndex=7 }; // move R2 from 21 to 7
										if (value==0) { pass = true }; // override resolution if value is 0 or 1
										if (value==1) { pass = true };
										if ((lastValue[index] - value).abs >= resolution) { pass = true }; // resolution

										// important this comes after above line
										if (index==18) { // dpad
											value = (value*4).asInt; // 0=up, 1=right, 2=down, 3=left, 4=centre
											// dpad out as up/down [18,19] left/right [20,21]
											// dpad out as up/down [20,21] left/right [18,19]
											switch (lastDPad)
												{0} { dependants[idName].do{|func| func.value(20, 0)}; } // up off
												{1} { dependants[idName].do{|func| func.value(19, 0)}; } // right off
												{2} { dependants[idName].do{|func| func.value(21, 0)}; } // down off
												{3} { dependants[idName].do{|func| func.value(18, 0)}; } // left off
												{4} { pass = false }; // centre
											switch (value)
												{0} { newIndex = 20; lastDPad = 0; value = 1; pass = true } // up on
												{1} { newIndex = 19; lastDPad = 1; value = 1; pass = true } // right on
												{2} { newIndex = 21; lastDPad = 2; value = 1; pass = true } // down on
												{3} { newIndex = 18; lastDPad = 3; value = 1; pass = true } // left on
												{4} { lastDPad =  4; pass = false }; // centre
										};
									};
									// output value if passes all tests above
									if (pass) {
										lastValue[index] = value;							// store last value
										if (reverse.includes(index)) { value = 1 - value }; // reverse jpad up/down
										dependants[idName].do{|func| func.value(newIndex ? index, value)}; // action
										if (verbose) { [newIndex ? index,value].postln };
									};
								};
							};
						};
					};
				};
			};
			paths = [\None] ++ (allHIDs.keys.asList.sort);  // all paths as symbols
			pathAsStrings = paths.collect(_.asString);		// all paths as strings
		}.fork;
	}

	*addAction{|path,func|
		if (path.isNumber) { path = paths[path] } {	path = path.asSymbol };
		dependants[path] = dependants[path].add(func)
	}

	*removeAction{|path,func| dependants[path.asSymbol].remove(func) }

}

////////////////////////////////////
// a puss 4 controller instrument //
////////////////////////////////////

LNX_Puss4 : LNX_InstrumentTemplate {

	var <path = 'None', <func;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"Puss 4"}
	*sortOrder	{^1.999}
	mixerColor	{^Color(0,0,0,0.2)} // colour in mixer
	isMIDI		{^true}
	isFX		{^false}
	isInstrument{^false}
	canTurnOnOff{^false}
	clockPriority{^1} // sequenced preset changes before any note generation

	header {
		instrumentHeaderType="SC Puss 4 Doc";
		version="v1.1";
	}

	// an immutable list of methods available to the network
	interface{^#[ \netMidiControl, \netMidiControlVP ]}

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

			// 2. CC range
			[0, [0,31,\lin,1], midiControl, 2, "CC range",
				(items_:(31+1).collect{|i|  (i*20)++"-"++(i*20+19)++(if(i>5,"*",""))   }, label_:"CC range"),
				{|me,val,latency,send| this.setPVP(2,val,latency,send)}]

		];

/*
		// noE = 22;
		LNX_Puss4Patch.noElements.do{|i|
			template=template. add([0, \switch, ( label_:("Type:"++LNX_Puss4Patch.elementNames[i]), numberFunc_:\int),
			{|me,val,latency,send| this.setPVP(2+i,val,latency,send)}]);
		};
		LNX_Puss4Patch.noElements.do{|i|
		template=template. add([0, \unipolar, ( label_:("CC:"++LNX_Puss4Patch.elementNames[i]), numberFunc_:\int),
			{|me,val,latency,send| this.setPVP(22+2+i,val,latency,send)}]);
		};
*/

		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1];
		autoExclusion=[0,1]; //=(2..17);

		func = {|index,value| this.puss4In(index,value) };
		LNX_Puss4Patch.addAction(path,func);

	}

	puss4In{|index,value|
		if (index<=19) {
			// texample for sending notes
			// if (index == 4) { if (value>0.5) { midi.noteOn(53,100) } { midi.noteOff(53,100) } };
			index = (p[2]*20)+index; // CC range
			midi.control(index, value * 127, nil, send:false, ignore:false);
			^this
		};
		if (value==0) {^this};
		if (index==20) { // Dpad UP
			LNX_POP.previousProg;
			^this
		};
		if (index==21) { // Dpad DOWN
			LNX_POP.nextProg;
			^this
		};
	}

	iGetSaveList{ ^[path.asString] }

	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		var index;
		LNX_Puss4Patch.removeAction(path,func);
		path = l.popS.asSymbol;
		index = LNX_Puss4Patch.paths.indexOf(path);
		if (index.notNil) { gui[\pathMenu].value_(index) };
		LNX_Puss4Patch.addAction(path,func);
	}

	iFree{ LNX_Puss4Patch.removeAction(path,func) }

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

		gui[\menuTheme2]=( \font_		: Font("Arial", 10),
						\labelShadow_	: false,
						\colors_      : (\background:Color.white,
										\label:Color.black,
										\string:Color(0,0,0),
									    \focus:Color.clear));

		// the border and composite view
		gui[\compositeView] = MVC_RoundedComView(window,
				Rect(11,11,this.thisWidth-22,this.thisHeight-22))
				.color_(\border,  Color(59/108,65/103,505/692)  )
				.color_(\border2, Color(0,1/103,9/77))
				.color_(\background, Color(59/77,43/54,9/11)*1.1 );

		// device
		gui[\pathMenu] = MVC_PopUpMenu3(gui[\compositeView  ],Rect(10,20,80,19),gui[\menuTheme2])
			.items_(LNX_Puss4Patch.pathAsStrings)
			.label_("Device")
			.action_{|me|
				LNX_Puss4Patch.removeAction(path,func);
				path = LNX_Puss4Patch.paths[me.value.asInt];
				LNX_Puss4Patch.addAction(path,func);
			};

		// 2. CC range
		MVC_PopUpMenu3(gui[\compositeView  ],Rect(216,20,75,19),gui[\menuTheme2], models[2]);

		// MIDI Settings
 		MVC_FlatButton(gui[\compositeView],Rect(104, 20, 43, 19),"MIDI")
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
		MVC_FlatButton(gui[\compositeView],Rect(158, 20, 43, 19),"Cntrl")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(0.3,0.5,1)+0.3/2)
			.color_(\down,Color(0.3,0.5,1)+0.3/6)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ LNX_MIDIControl.editControls(studio); LNX_MIDIControl.window.front };

		// reconnect
		MVC_FlatButton(gui[\compositeView],Rect(218, 192, 74, 19),"Reconnect")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(0.3,0.5,1)+0.3/2)
			.color_(\down,Color(0.3,0.5,1)+0.3/6)
			.color_(\string,Color.white)
			.resize_(9)
			.action_{
				LNX_Puss4Patch.restartAll;
				{
					var index;
					gui[\pathMenu].items_(LNX_Puss4Patch.pathAsStrings);
					index = LNX_Puss4Patch.paths.indexOf(path) ? 0;
					gui[\pathMenu].value_(index);
				}.defer(1.5);
			};

		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\compositeView],10@190,100,
				Color(0.8,0.8,1)/1.6,
				Color(0.7,0.7,1)/3,
				Color(0.7,0.7,1)/1.5,
				Color(35/48,122/157,5/6),
				Color.black
			);
		this.attachActionsToPresetGUI;

/*		// controllers
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
		};*/

	}

	////////////////////////

	iInitMIDI{ midi.putLoadList([0, 0]++LNX_MIDIPatch.nextUnusedOut) }

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
