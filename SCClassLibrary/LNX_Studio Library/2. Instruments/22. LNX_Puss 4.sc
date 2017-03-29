//////////////////////////////
// a puss 4 control patcher //
//////////////////////////////
/*
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

*/

LNX_Puss4Patch{

	classvar <>verbose = false;
	classvar <allHIDs, <paths, <pathAsStrings, <dependants, <exclude, <reverse, <thresh, <off;

	*initClass{
		Class.initClassTree(HID);
		{ 1.wait; this.restartAll }.fork;
	}

	*restartAll{
		allHIDs		= IdentityDictionary[];
		dependants	= dependants ? IdentityDictionary[];
		exclude		= [6,7,19];
		reverse		= [15,17];
		thresh		= 0.01;
		off			= 0.03;
		{
			HID.findAvailable;
			1.wait;
			HID.available.do{|info|
				if (info.vendorName  =="Sony Interactive Entertainment") {
					var path         = info.path.asSymbol;
					var hid          = HID.openPath(path: (path.asString) );
					var lastValue    = IdentityDictionary[];
					var lastDPad	 = 4;
					if (hid.notNil) {
						allHIDs[path]    = hid;
						dependants[path] = dependants[path] ? IdentitySet[];
						22.do{|index|
							lastValue[index] = inf;
							if (exclude.includes(index).not) {
								hid.elements[index].action = {|i,element|
									var value = element.value;
									var pass  = false;
									var newIndex;
									if (((value-0.5).abs < off)and:{(index>=14)&&(index<=17)}) {
										value=0.5;
										if ((lastValue[index] - value).abs >= thresh) { pass = true };
									}{
										if (value==0) { pass = true };
										if (value==1) { pass = true };
										if ((lastValue[index] - value).abs >= thresh) { pass = true };

										// important this comes after above line
										if (index==18) { // dpad
											value = (value*4).asInt; // 0=up, 1=right, 2=down, 3=left, 4=centre
											// dpad out as up/down [18,19] left/right [6,7]
											switch (lastDPad)
											{0} { dependants[path].do{|func| func.value(18, 0)}; } // up
											{1} { dependants[path].do{|func| func.value( 7, 0)}; } // right
											{2} { dependants[path].do{|func| func.value(19, 0)}; } // down
											{3} { dependants[path].do{|func| func.value( 6, 0)}; } // left
											{4} { pass = false }; // centre

											switch (value)
											{0} { newIndex = 18; lastDPad = 0; value = 1; pass = true }
											{1} { newIndex =  7; lastDPad = 1; value = 1; pass = true }
											{2} { newIndex = 19; lastDPad = 2; value = 1; pass = true }
											{3} { newIndex =  6; lastDPad = 3; value = 1; pass = true }
											{4} { lastDPad =  4; pass = false }; // centre
										};
									};
									// output value if passes all tests above
									if (pass) {
										lastValue[index] = value;
										if (reverse.includes(index)) { value = 1 - value };
										dependants[path].do{|func| func.value(newIndex ? index, value)};
										if (verbose) { [newIndex ? index,value].postln };
									};
								}
							};
						};
					};
				};
			};

			paths = [\None] ++ (allHIDs.keys.asList);

			pathAsStrings = paths.collect(_.asString);

		}.fork;
	}

	*addAction{|path,func|
		if (path.isNumber) { path = paths[path] } {	path = path.asSymbol };
		dependants[path] = dependants[path].add(func)
	}

	*removeAction{|path,func| dependants[path.asSymbol].remove(func) }

}

/*
[0]  // square
[1]  // X
[2]  // O
[3]  // Triangle
[4]  // L1 (on/off)
[5]  // R1 (on/off)
[6]  // DPad LEFT / L2 [DISABLED]
[7]  // DPad RIGHT / R2 [DISABLED]
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
[18] // DPad UP / urdlc (0, 0.28571429848671, 0.57142859697342, 0.85714286565781, 1.1428571939468]
[19] // DPad DOWN / ? Noisey. [DISABLED]
[20] // L2 (pos)
[21] // R2 (pos)

dpad [18,19] up/down [6,6] left/right

128/22 = 5.8

use [18] - up, down. inc dec prog

*/

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

		];

/*		8.do{|i|
			template[i+2]=[0, [0,127,\linear,1], ( label_:(i.asString), numberFunc_:\int),
				{|me,val| this.midiControlVP(i,val) }];

			template[i+10]=[0, \switch, ( strings_:(i+8).asString ),
				{ this.guiMidiControl(i+8,1) }];
		};*/

		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1];
		autoExclusion=[0,1]; //=(2..17);

		func = {|index,value| this.puss4In(index,value) };
		LNX_Puss4Patch.addAction(path,func);

	}

	puss4In{|index,value|
		[index,value].postln;
		midi.control(index, value * 127, nil, send:false , ignore:false);
	}


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
				Rect(11,11,this.thisWidth-22,this.thisHeight-32-75))
				.color_(\border,  Color(59/108,65/103,505/692)  )
				.color_(\border2, Color(0,1/103,9/77))
				.color_(\background, Color(59/77,43/54,9/11)*1.1 );


		// path menu
		MVC_PopUpMenu3(gui[\compositeView  ],Rect(5,5,100,16),gui[\menuTheme2])
			.items_(LNX_Puss4Patch.pathAsStrings)
			.action_{|me|
				LNX_Puss4Patch.removeAction(path,func);
				path = LNX_Puss4Patch.paths[me.value.asInt];
				LNX_Puss4Patch.addAction(path,func);
			};


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
