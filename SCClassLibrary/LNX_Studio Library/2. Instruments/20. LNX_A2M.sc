
// Audio to MIDI CC init

LNX_A2M : LNX_InstrumentTemplate {

	classvar <noChannels=4;

	var <nameModels, <valueInModels, <valueOutModels, <lastValue;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	// an immutable list of methods available to the network
	interface{^#[ \netChangeName ]}

	// type/info
	*studioName      {^"Side Chain CC"}
	*sortOrder       {^2.88}
	isInstrument     {^true}
	isMIDI       	 {^true}
	canBeSequenced   {^false}
	mixerColor       {^Color(0.3,0.3,0.3,0.2)} // colour in mixer

	// mixer models
	soloModel   {^models[0]}
	onOffModel  {^models[1]}

	header{
		// define your document header details
		instrumentHeaderType="SC A2M Doc";
		version="v1.0";
	}

	// the models
	initModel{

		var modelTemplate;

		valueInModels  = {\unipolar.asModel} ! noChannels;	// model to hold value of incoming RMS signal
		valueOutModels = {\unipolar.asModel} ! noChannels;  // model to hold value of mapped RMS signal
		lastValue      = 0 ! noChannels;					// the last RMS value, used to stop updating
		nameModels     = {|i| "".asModel.actions_(\stringAction,{|me| // the string name for each channel
			api.sendOD(\netChangeName,i,me.string.nameSafe) // update the network
		})} ! noChannels;

		modelTemplate = [
			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle|
					this.solo(val,latency,send,toggle);
					if (node.notNil) {server.sendBundle(latency,[\n_set, node, \on, this.isOn])};
				},
				\action2_ -> {|me|
					this.soloAlt(me.value);
					if (node.notNil) {server.sendBundle(nil,[\n_set, node, \on, this.isOn])};
				 }],

			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle|
					this.onOff(val,latency,send,toggle);
					if (node.notNil) {server.sendBundle(latency,[\n_set, node, \on, this.isOn])};
				},
				\action2_ -> {|me|
					this.onOffAlt(me.value);
					if (node.notNil) {server.sendBundle(nil,[\n_set, node, \on, this.isOn])};
				}],

			/////////////

			// 2.rate
			[30, [1,40,1], midiControl, 2, "Rate", (label_:"Rate"), {|me,val,latency,send|
				this.setPReplaceVP(2,val,latency,send)
			}],

			// 3.peak lag
			[1, [0,2], midiControl, 3, "Peak Lag", (label_:"Peak Lag"), {|me,val,latency,send|
				this.setPReplaceVP(3,val,latency,send)
			}],

		];

		// each channel has set-up, in channel, on, amp, min, max, & curve cc
		noChannels.do{|i|
			var j = i*8;  // model offset index
			var k = i+1;  // for strings or symbols
			// 4 - 11 ( 8 models each channel)

			modelTemplate = modelTemplate ++ [
				// 4. channelSetup1
				[0,[0,2,\lin,1], midiControl, 4+j, "Channel"+k+"Setup",
					(\items_:["Left + Right","Left","Right"]),
					{|me,val,latency,send|
						this.setSynthArgVH(4+j,val,(\channelSetup++k).asSymbol,val,latency,send) }],

				// 5. in channels
				[0+i,[0,LNX_AudioDevices.defaultFXBusChannels/2-1,\linear,1],
					midiControl, 5+j, "In"+k+"Channel",
					(\items_:LNX_AudioDevices.fxMenuList),
					{|me,val,latency,send|
						var in  = LNX_AudioDevices.firstFXBus+(val*2);
						this.setSynthArgVH(5+j,val,(\inputChannels++k).asSymbol,in,latency,send) }],

				// 6. on
				[1,\switch, midiControl, 6+j, "On"+k, {|me,val,latency,send|
					this.setSynthArgVH(6+j,val,(\on++k).asSymbol,val,latency,send) }],

				// 7. amp
				[\db6, midiControl, 7+j, "Amp"+k, {|me,val,latency,send|
					this.setSynthArgVP(7+j,val,(\amp++k).asSymbol,val.dbamp,latency,send) }],

				// 8. min
				[0, \unipolar, midiControl, 8+j, "Min"+k, {|me,val,latency,send|
					this.setPVP(8+j,val,latency,true) }],

				// 9.max
				[1, \unipolar, midiControl, 9+j, "Max"+k, {|me,val,latency,send|
					this.setPVP(9+j,val,latency,true)}],

				// 10.curve
				[0,[-4,4], midiControl, 10+j, "Slope"+k, (label_:"Slope", \zeroValue_:0), {|me,val,latency,send|
					this.setPVP(10+j,val,latency,true)}],

				// 11. cc
				[64+i, \midi, midiControl, 11+j, "CC"+k, {|me,val,latency,send|
					this.setPVP(11+j,val,latency,true)}],

			];

		};

		#models,defaults = modelTemplate.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=(0..1);
		randomExclusion=(0..1)++[4,5,12,13,20,21,28,29];
		autoExclusion=[];

	}

	// GUI /////////////////////////////////////////////////////////////////

	// add text to the name model, comes from midi learn
	addTextToName{|i,text|
		text=text.nameSafe;                // make if safe
		if (nameModels[i].string.notEmpty) {text= ", "++text}; // add a ", " if needed
		text = nameModels[i].string++text; // add the text we got from the midi learn
		nameModels[i].string_(text);       // update the name models
		api.sendOD(\netChangeName,i,text); // and network
	}

	// net of both both above
	netChangeName{|i,text|
		nameModels[i.asInt].string_(text.asString);
	}

	*thisWidth  {^420}
	*thisHeight {^585}

	createWindow{|bounds| this.createTemplateWindow(bounds,Color.black,false) }

	// create all the GUI widgets while attaching them to models
	createWidgets{

		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background :Color.white));

		gui[\scrollTheme]=( \background	: Color(0.766, 0.766, 0.766),
						 \border		: Color(0.545, 0.562, 0.669));

		gui[\midiTheme]= ( \rounded_	: true,
						\canFocus_	: false,
						\shadow_		: true,
						\colors_		: (	\up : Color(0.31,0.31,0.49),
										\down	: Color(0.31,0.31,0.49),
										\string	: Color.white));

		gui[\knobTheme]=( \labelShadow_	: false,
						\numberWidth_	: (-20),
						\numberFont_	: Font("Helvetica",10),
						\colors_		: (	\on		: Color(0.875, 0.852, 1),
										\label		: Color.black,
										\numberUp	: Color.black,
										\numberDown : Color.white));

		gui[\theme2]=(	\orientation_ 	: \horiz,
						\resoultion_	: 3,
						\visualRound_	: 0.001,
						\rounded_		: true,
						\font_			: Font("Helvetica",10),
						\labelFont_		: Font("Helvetica",10),
						\showNumberBox_	: false,
						\colors_		: ( \label 		: Color.white,
										\background 	: Color(0.43,0.44,0.43)*1.4,
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string 		: Color.black,
										\focus 			: Color(0,0,0,0)));

		gui[\onOffTheme]=(	\font_		: Font("Helvetica", 12, true),
							\rounded_	: true,
							\canFocus_	: false,
						 	\colors_	: (\on	: Color(0.25,1,0.25),
						 	   			 \off 	: Color(0.4,0.4,0.4)));

		gui[\learnTheme]=(	\font_		: Font("Helvetica", 12, true),
							\rounded_	: true,
							\canFocus_	: false,
						 	\colors_	: (\up	: Color(50/77,61/77,1),
						 	   			 \down 	: Color(50/77,61/77,1),
										\string	: Color.black) );

		gui[\ccBoxTheme]=(	\orientation_ 	: \horiz,
							\resoultion_	: 8,
							\visualRound_ 	: 1,
							\rounded_		: true,
							\font_			: Font("Helvetica",12),
							\labelFont_		: Font("Helvetica",12),
							\showNumberBox_	: false,
							\colors_		: (	\background : Color(0,0,0,0.15),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.black,
										\focus : Color(0,0,0,0)));

		gui[\soloTheme ]=( \font_		: Font("Helvetica", 12, true),
						   \colors_     : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));

		gui[\button] = (\rounded_		: true,
						\shadow_		: true,
						\canFocus_		: false,
						\colors_		: (\up    : Color(0.7,0.7,1)/1.5,
									   \down	  : Color(0.5,0.5,1.0)/4,
									   \string	  :Color.white));


		gui[\labelTheme]=( \font_		: Font("Helvetica", 16,true),
							\align_		: \left,
							\shadow_	: false,
							\noShadows_	: 0,
							\colors_	: (\string : Color.black));

		// widgets
		gui[\scrollView] = MVC_RoundedComView(window,
							Rect(11,11,thisWidth-22,thisHeight-23), gui[\scrollTheme]);


		MVC_PlainSquare(gui[\scrollView],Rect(0, 0, 403, 70)).color_(\off, Color(0, 0, 0, 0.3));

		// logo
		MVC_StaticText(gui[\scrollView], Rect(11, 1, 140, 35),gui[\labelTheme])
			.string_("Side Chain CC");

		// 1.on/off
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect( 8, 36,22,18),gui[\onOffTheme])
			.rounded_(true)
			.permanentStrings_(["On"]);

		// 0.solo
		MVC_OnOffView(models[0],gui[\scrollView] ,Rect( 38, 36,20,18),gui[\soloTheme])
			.rounded_(true);

		// TBC.always
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect( 68, 36,55,18),gui[\onOffTheme])
			.rounded_(true)
			.permanentStrings_(["Always"]);

		// rate
		MVC_MyKnob3(models[2], gui[\scrollView], Rect(144,18,30,30), gui[\knobTheme]);

		// peak lag
		MVC_MyKnob3(models[3], gui[\scrollView], Rect(223,18,30,30), gui[\knobTheme]);

		// MIDI Settings
 		MVC_FlatButton(gui[\scrollView],Rect(312, 6, 43, 19),"MIDI",gui[\button])
			.action_{ this.createMIDIInOutModelWindow(window,2,3,
				colors:(border1:Color(0.545 , 0.562, 0.669), border2:Color(0.766, 0.766, 0.766))
			)};

		// midi control button
		MVC_FlatButton(gui[\scrollView],Rect(288, 35, 43, 19),"Cntrl",gui[\button])
			.action_{ LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front };

		MVC_FlatButton(gui[\scrollView],Rect(341 , 35, 43, 19),"All",gui[\button])
			.action_{ LNX_MIDIControl.editControls(studio); LNX_MIDIControl.window.front };

		/////////////////////////////

		MVC_PlainSquare(gui[\scrollView],Rect(0, 485, 428, 82)).color_(\off, Color(0, 0, 0, 0.3));

		noChannels.do{|i|
			var j = i*8;
			var osx = (i*100)-10;

			if (i>0) {
				MVC_PlainSquare(gui[\scrollView],Rect(6+osx, 70, 5, 415))
				.color_(\off, Color(0, 0, 0, 0.3));
			};

			// learn button
			MVC_FlatButton(gui[\scrollView], Rect(38+osx, 79, 43, 18),"Learn",gui[\learnTheme])
				.action_{
					var name;
					name=midi.learn(p[11+j],64); // send & get midi learn
					this.addTextToName(i,name);
				};

			// 11. cc
			MVC_NumberBox(gui[\scrollView], models[11+j],Rect(39+osx, 103, 41, 16),  gui[\ccBoxTheme]);

			// in levels
			MVC_FlatDisplay(gui[\scrollView], valueInModels[i], Rect(32+osx, 126, 7, 200));
			MVC_Scale(gui[\scrollView], Rect(30+osx, 126, 2, 200));
			MVC_Scale(gui[\scrollView], Rect(39+osx, 126, 2, 200));

			// 7.amp
			MVC_SmoothSlider(gui[\scrollView], models[7+j],Rect(44+osx, 126, 31, 200))
				.label_(nil)
				.thumbSizeAsRatio_(0.18,8)
				.numberFunc_(\float2)
				.showNumberBox_(true)
				.numberFont_(Font("Helvetica",10))
				.color_(\knob,Color(1,1,1))
				.color_(\hilite,Color(0,0,0,0.5))
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color.white);

			// 6. on1
			MVC_OnOffView(gui[\scrollView], models[6+j], Rect(46+osx,341, 27, 21), gui[\onOffTheme])
				.label_((i+1).asString)
				.labelShadow_(false)
				.orientation_(\horiz)
				.color_(\label,Color.black);

			// out levels
			MVC_FlatDisplay(gui[\scrollView], valueOutModels[i], Rect(80+osx, 126, 7, 200));
			MVC_Scale(gui[\scrollView], Rect(78+osx, 126, 2, 200));
			MVC_Scale(gui[\scrollView], Rect(87+osx, 126, 2, 200));

			// 8. min
			MVC_PeakLevel(gui[\scrollView], models[8+j] ,Rect(16+osx, 126, 13, 200)).icon_(\play);

			// 9. max
			MVC_PeakLevel(gui[\scrollView], models[9+j] ,Rect(89+osx, 126, 13, 200));

			// curve
			MVC_MyKnob3(models[10+j], gui[\scrollView], Rect(44+osx, 384, 30, 30), gui[\knobTheme]);

			// 4. channelSetup1
			MVC_PopUpMenu3(models[4+j],gui[\scrollView],Rect(22+osx, 436, 75,17), gui[\menuTheme ] );

			// 5. in channel
			MVC_PopUpMenu3(models[5+j],gui[\scrollView],Rect(22+osx,456,75,17), gui[\menuTheme ] );

			//  name ( look at nameSafe we use : alot in control names )
			MVC_Text(gui[\scrollView],Rect(15,495+(i*17)-4,383-5,16),nameModels[i])
				.label_((i+1).asString)
				.labelShadow_(false)
				.orientation_(\horiz)
				.color_(\label,Color.black)
				.shadow_(false)
				.canEdit_(true)
				.maxStringSize_(100)
				.color_(\background,Color(0,0,0,0.3))
				.color_(\string,Color.black)
				.color_(\cursor,Color.white)

				.font_(Font("Helvetica",12));

		}
	}

	///////////////////////////

	// for your own saving
	iGetSaveList{ ^nameModels.collect(_.string) }

	// for your own loading
	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		noChannels.do{|i| nameModels[i].string_(l.popS ? "") };
		^l
	}

	//////////////////////////

	// any post midiInit stuff
	iInitMIDI{ midi.putLoadList([0, 0 ]++LNX_MIDIPatch.nextUnusedOut) }

	*initUGens{|s|
		if (verbose) { "SynthDef loaded: A2M".postln; };

		SynthDef("LNX_A2M", {|id=0, rate=40, lag=0,
			inputChannels1=2, channelSetup1=0, on1=1, amp1=1
			inputChannels2=4, channelSetup2=0, on2=1, amp2=1
			inputChannels3=6, channelSetup3=0, on3=1, amp3=1
			inputChannels4=8, channelSetup4=0, on4=1, amp4=1

			|
			var signal1, signal2, signal3, signal4;

			signal1 = In.ar(inputChannels1, 2) * Lag.kr(on1 * amp1);
			signal1 = Select.ar(channelSetup1,[ (signal1[0]+signal1[1])*0.5, signal1[0], signal1[1] ]);

			signal2 = In.ar(inputChannels2, 2) * Lag.kr(on2 * amp2);
			signal2 = Select.ar(channelSetup2,[ (signal2[0]+signal2[1])*0.5, signal2[0], signal2[1] ]);

			signal3 = In.ar(inputChannels3, 2) * Lag.kr(on3 * amp3);
			signal3 = Select.ar(channelSetup3,[ (signal3[0]+signal3[1])*0.5, signal3[0], signal3[1] ]);

			signal4 = In.ar(inputChannels4, 2) * Lag.kr(on4 * amp4);
			signal4 = Select.ar(channelSetup4,[ (signal4[0]+signal4[1])*0.5, signal4[0], signal4[1] ]);

			SendPeakRMS.kr([signal1,signal2,signal3,signal4] , rate, lag, "/A2M", id);

		}).send(s);


	}

	// method in from osc responders (where the magic happens)
	a2m_in_{|...value|
		noChannels.do{|i|
			var j = i*8;   												// j is offset for moel index
			if (((value[i].abs) - (lastValue[i]).abs).abs>0.001) {      // only do if diff is >0.001
				lastValue[i] = value[i];								// store value for last value
				value[i] = value[i].clip(0,1);							// clip the input
				valueInModels[i].lazyValueAction_(value[i],nil,false);	// update input levels
				value[i] = value[i].lincurve(0,1,p[8+j],p[9+j],p[10+j]);// map input on to min,max & curve
				valueOutModels[i].lazyValueAction_(value[i],nil,false); // update output levels
				midi.control(p[11+j], value[i]*127, nil, false, true);  // send midi data
			};
		};
	}

	startDSP{
		synth = Synth.tail(fxGroup,"LNX_A2M"); // make me a new 1
		node  = synth.nodeID;
	}

	stopDSP{ synth.free }

	updateOnSolo{|latency| }

	updateDSP{|oldP,latency|
		server.sendBundle(latency +! syncDelay,
			[\n_set, node, \id, 			id],
			[\n_set, node, \rate, 			p[2]],
			[\n_set, node, \lag, 			p[3]],

			[\n_set, node, \channelSetup1,	p[4]],
			[\n_set, node, \inputChannels1,	LNX_AudioDevices.firstFXBus+(p[5]*2) ],
			[\n_set, node, \on1, 			p[6]],
			[\n_set, node, \amp1, 			p[7].dbamp],

			[\n_set, node, \channelSetup2,	p[4+8]],
			[\n_set, node, \inputChannels2,	LNX_AudioDevices.firstFXBus+(p[5+8]*2) ],
			[\n_set, node, \on2, 			p[6+8]],
			[\n_set, node, \amp2, 			p[7+8].dbamp],

			[\n_set, node, \channelSetup3,	p[4+16]],
			[\n_set, node, \inputChannels3,	LNX_AudioDevices.firstFXBus+(p[5+16]*2) ],
			[\n_set, node, \on3, 			p[6+16]],
			[\n_set, node, \amp3, 			p[7+16].dbamp],

			[\n_set, node, \channelSetup4,	p[4+24]],
			[\n_set, node, \inputChannels4,	LNX_AudioDevices.firstFXBus+(p[5+24]*2) ],
			[\n_set, node, \on4, 			p[6+24]],
			[\n_set, node, \amp4, 			p[7+24].dbamp],
		);
	}

	replaceDSP{|latency|
		var previousNode;

		previousNode = node;
		node = server.nextNodeID;

		// send the new synth to the server
		server.sendBundle(latency, ([\s_new, "LNX_A2M", node, 4,  previousNode] ++ [
			\id,				id,
			\rate,				p[2],  // rate and lag are both i rate hence a new synth to replace last one
			\lag,				p[3],

			\channelSetup1,		p[4],
			\inputChannels1,	LNX_AudioDevices.firstFXBus+(p[5]*2),
			\on1,				p[6],
			\amp1,				p[7].dbamp,

			\channelSetup2,		p[4+8],
			\inputChannels2,	LNX_AudioDevices.firstFXBus+(p[5+8]*2),
			\on2,				p[6+8],
			\amp2,				p[7+8].dbamp,

			\channelSetup3,		p[4+16],
			\inputChannels3,	LNX_AudioDevices.firstFXBus+(p[5+16]*2),
			\on3,				p[6+16],
			\amp3,				p[7+16].dbamp,

			\channelSetup4,		p[4+24],
			\inputChannels4,	LNX_AudioDevices.firstFXBus+(p[5+24]*2),
			\on4,				p[6+24],
			\amp4,				p[7+24].dbamp,
		]));

	}

} // end ////////////////////////////////////
