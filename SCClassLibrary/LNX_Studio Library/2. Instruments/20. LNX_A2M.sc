
// its Audio to MIDI CC init

LNX_A2M : LNX_InstrumentTemplate {

	classvar <noChannels=4;

	var <nameModels, <valueInModels, <valueOutModels, <lastValue;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	// an immutable list of methods available to the network
	interface		{^#[ \netChangeName ]}		// network interface
	*studioName		{^"Side Chain CC"}			// type/info
	*sortOrder		{^2.88}						// sort order in library
	isInstrument	{^true}						// does this need to be true?
	isMIDI			{^true}						// has midi out
	canBeSequenced	{^false}					// doesn't have a sequencer
	mixerColor		{^Color(0.3,0.3,0.3,0.2)}	// colour in mixer
	onColor			{^Color(0.5,0.7,1)}			// Mixer on button colour
	alwaysOnModel	{^models[4]}				// always on model
	alwaysOn		{^models[4].isTrue}			// am i? used by melody maker to change onOff widgets
	canAlwaysOn		{^true}						// can i?
	soloModel       {^models[0]} 				// mixer solo model
	onOffModel      {^models[1]} 				// mixer on model
	onNow			{ ^(this.isOn)||(this.alwaysOn) } // is this on now. include onSolo & alwaysOn

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
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],

			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me| this.onOffAlt(me.value) }],

			/////////////

			// 2.rate
			[30, [1,40,1], midiControl, 2, "Rate", (label_:"Rate"), {|me,val,latency,send|
				this.setPReplaceVP(2,val,latency,send)
			}],

			// 3.peak lag
			[1, [0,2], midiControl, 3, "Peak Lag", (label_:"Peak Lag"), {|me,val,latency,send|
				this.setPReplaceVP(3,val,latency,send)
			}],

			// 4. Always ON
			[0, \switch, (\strings_:"Always"), midiControl, 4, "Always On",
				{|me,val,latency,send,toggle|
					this.setPVPModel(4,val,latency,send);
					this.updateOnSolo(latency);
					{studio.updateAlwaysOn(id,val.isTrue)}.defer;
				}],

		];

		// each channel has set-up, in channel, on, amp, min, max, & curve cc
		noChannels.do{|i|
			var j = i*11;  // model offset index
			var k = i+1;  // for strings or symbols
			// 5 - 12 ( 8 models each channel)
			// 5 - 15 ( 11 models each channel)

			modelTemplate = modelTemplate ++ [
				// 5. channelSetup1
				[0,[0,2,\lin,1], midiControl, 5+j, "Channel"+k+"Setup",
					(\items_:["Left + Right","Left","Right"]),
					{|me,val,latency,send|
						this.setSynthArgVH(5+j,val,(\channelSetup++k).asSymbol,val,latency,send) }],

				// 6. in channels
				[0+i,[0,LNX_AudioDevices.defaultFXBusChannels/2-1,\linear,1],
					midiControl, 6+j, "In"+k+"Channel",
					(\items_:LNX_AudioDevices.fxMenuList),
					{|me,val,latency,send|
						var in  = LNX_AudioDevices.firstFXBus+(val*2);
						this.setSynthArgVH(6+j,val,(\inputChannels++k).asSymbol,in,latency,send) }],

				// 7. on
				[1,\switch, midiControl, 7+j, "On"+k, {|me,val,latency,send|
					this.setSynthArgVH(7+j,val,(\on++k).asSymbol,val,latency,send) }],

				// 8. amp
				[\db8, midiControl, 8+j, "Amp"+k, {|me,val,latency,send|
					this.setSynthArgVP(8+j,val,(\amp++k).asSymbol,val.dbamp,latency,send) }],

				// 9. min
				[0, \unipolar, midiControl, 9+j, "Min"+k, {|me,val,latency,send|
					this.setPVP(9+j,val,latency,send) }],

				// 10.max
				[1, \unipolar, midiControl, 10+j, "Max"+k, {|me,val,latency,send|
					this.setPVP(10+j,val,latency,send)}],

				// 11.curve
				[0,[-4,4], midiControl, 11+j, "Slope"+k, (label_:"Slope", \zeroValue_:0), {|me,val,latency,send|
					this.setPVP(11+j,val,latency,send)}],

				// 12. cc
				[64+i, \MIDIcc, midiControl, 12+j, "CC"+k, {|me,val,latency,send|
					this.setPVP(12+j,val,latency,send)}],

				// 13. peak / rms
				[0,\switch,  midiControl, 13+j, "Peak/RMS"+k, (\strings_:["Peak","RMS"]), {|me,val,latency,send|
					this.setPVP(13+j,val,latency,true)}],

				// 14. attack (0-1) > (0-0.99)
				[0,\unipolar, midiControl, 14+j, "Attack"+k, (label_:"Attack"), {|me,val,latency,send|
					this.setPVP(14+j,val,latency,send)}],

				// 15. decay (0-1) > (0-0.99)
				[0,\unipolar, midiControl, 15+j, "Decay"+k, (label_:"Decay"), {|me,val,latency,send|
					this.setPVP(15+j,val,latency,send)}],

			];

		};

		#models,defaults = modelTemplate.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=(0..1);
		randomExclusion=(0..1)++[5,6,13,14,21,22,29,30];
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
	netChangeName{|i,text| nameModels[i.asInt].string_(text.asString) }

	*thisWidth {^420}
	*thisHeight{^605}

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
							\shadow_	: true,
							\noShadows_	: 2,
							\colors_	: (\string : Color.white));

		gui[\labelTheme2]=( \font_		: Font("Helvetica", 14,true),
							\align_		: \left,
							\shadow_	: false,
							\noShadows_	: 0,
							\colors_	: (\string : Color.black));

		gui[\plainTheme] = (\colors_ : (\off: Color(0.545, 0.562, 0.669)));

		// widgets
		gui[\scrollView] = MVC_RoundedComView(window,
							Rect(11,11,thisWidth-22,thisHeight-23), gui[\scrollTheme]);

		MVC_PlainSquare(gui[\scrollView],Rect(0, 0, 403, 60),gui[\plainTheme] );

		// logo
		MVC_StaticText(gui[\scrollView], Rect(10, 0, 118, 25),gui[\labelTheme])
			.string_("Side Chain CC");

		// 1.on/off
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect( 6, 33,26,18),gui[\onOffTheme])
			.rounded_(true)
			.permanentStrings_(["On"]);

		// 0.solo
		MVC_OnOffView(models[0],gui[\scrollView] ,Rect( 38, 33,20,18),gui[\soloTheme])
			.rounded_(true);

		// 4.always
		MVC_OnOffView(models[4],gui[\scrollView] ,Rect( 68, 33,55,18),gui[\onOffTheme])
			.rounded_(true)
			.permanentStrings_(["Always"]);

		// rate
		MVC_MyKnob3(models[2], gui[\scrollView], Rect(144,15,30,30), gui[\knobTheme]);

		// peak lag
		MVC_MyKnob3(models[3], gui[\scrollView], Rect(223,15,30,30), gui[\knobTheme]);

		// MIDI Settings
 		MVC_FlatButton(gui[\scrollView],Rect(312, 3, 43, 19),"MIDI",gui[\button])
			.action_{ this.createMIDIInOutModelWindow(window,2,3,
				colors:(border1:Color(0.545 , 0.562, 0.669), border2:Color(0.766, 0.766, 0.766))
			)};

		// midi control button
		MVC_FlatButton(gui[\scrollView],Rect(288, 32, 43, 19),"Cntrl",gui[\button])
			.action_{ LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front };

		MVC_FlatButton(gui[\scrollView],Rect(341 , 32, 43, 19),"All",gui[\button])
			.action_{ LNX_MIDIControl.editControls(studio); LNX_MIDIControl.window.front };

		/////////////////////////////

		MVC_PlainSquare(gui[\scrollView],Rect(0, 485, 408, 102),gui[\plainTheme]);

		noChannels.do{|i|
			var j = i*11;
			var osx = (i*100)-10;

			if (i>0) { MVC_PlainSquare(gui[\scrollView],Rect(6+osx,60,5,425),gui[\plainTheme])};

			// number label
			MVC_StaticText(gui[\scrollView], Rect(20+osx, 65, 20, 20),gui[\labelTheme2])
				.string_((i+1).asString);

			// 13. peak/rms
			MVC_OnOffView(gui[\scrollView], models[13+j], Rect(38+osx, 66, 43, 18) ,gui[\learnTheme])
				.color_(\on,Color(50/77,61/77,1))
				.color_(\off,Color(1,0.4,1));

			// learn button
			MVC_FlatButton(gui[\scrollView], Rect(15+osx, 90, 43, 18),"Learn",gui[\learnTheme])
				.action_{
					var name;
					name=midi.learn(p[12+j],64); // send & get midi learn
					if (name.notNil) { this.addTextToName(i,name) };
				};

			// 12. cc
			MVC_NumberBox(gui[\scrollView], models[12+j],Rect(61+osx, 91, 41, 16),  gui[\ccBoxTheme]);

			// in levels
			MVC_FlatDisplay(gui[\scrollView], valueInModels[i], Rect(32+osx, 116, 7, 200-10));
			MVC_Scale(gui[\scrollView], Rect(30+osx, 116, 2, 190));
			MVC_Scale(gui[\scrollView], Rect(39+osx, 116, 2, 190));

			// 8.amp
			MVC_SmoothSlider(gui[\scrollView], models[8+j],Rect(44+osx, 116, 31,190))
				.label_(nil)
				.thumbSizeAsRatio_(0.18,8)
				.numberFunc_(\float2)
				.showNumberBox_(true)
				.numberFont_(Font("Helvetica",10))
				.color_(\knob,Color(1,1,1))
				.color_(\hilite,Color(0,0,0,0.5))
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color.white);

			// out levels
			MVC_FlatDisplay(gui[\scrollView], valueOutModels[i], Rect(80+osx, 116, 7, 190));
			MVC_Scale(gui[\scrollView], Rect(78+osx, 116, 2, 190));
			MVC_Scale(gui[\scrollView], Rect(87+osx, 116, 2, 190));

			// 9. min
			MVC_PeakLevel(gui[\scrollView], models[9+j] ,Rect(16+osx, 116, 13, 190)).icon_(\play);

			// 10. max
			MVC_PeakLevel(gui[\scrollView], models[10+j] ,Rect(89+osx, 116, 13, 190));

			// 7. on1
			MVC_OnOffView(gui[\scrollView], models[7+j], Rect(24+osx,339, 27, 21), gui[\onOffTheme])
				.labelShadow_(false)
				.orientation_(\horiz)
				.color_(\label,Color.black);

			// 11. curve
			MVC_MyKnob3(models[11+j], gui[\scrollView], Rect(61+osx, 335, 30, 30), gui[\knobTheme]);

			// 14. attack (0-1) > (0-0.99)
			MVC_MyKnob3(models[14+j], gui[\scrollView], Rect(20+osx, 396, 30, 30), gui[\knobTheme]);

			// 15. decay (0-1) > (0-0.99)
			MVC_MyKnob3(models[15+j], gui[\scrollView], Rect(68+osx, 396, 30, 30), gui[\knobTheme]);

			// 5. channelSetup1
			MVC_PopUpMenu3(models[5+j],gui[\scrollView],Rect(22+osx, 441, 75,17), gui[\menuTheme ] );

			// 6. in channel
			MVC_PopUpMenu3(models[6+j],gui[\scrollView],Rect(22+osx,461,75,17), gui[\menuTheme ] );

			//  name ( look at nameSafe we use : alot in control names )
			MVC_Text(gui[\scrollView],Rect(15,491+(i*17),378,16),nameModels[i])
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
		};

		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],15@563,285,
				Color(0.8,0.8,1)/1.6,
				Color(0.7,0.7,1)/3,
				Color(0.7,0.7,1)/1.5,
				Color(35/48,122/157,5/6),
				Color.black
			);
		this.attachActionsToPresetGUI;
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
		SynthDef("LNX_A2M", {|id=0, rate=40, lag=0, allOn=1,
			inputChannels1=2, channelSetup1=0, on1=1, amp1=1
			inputChannels2=4, channelSetup2=0, on2=1, amp2=1
			inputChannels3=6, channelSetup3=0, on3=1, amp3=1
			inputChannels4=8, channelSetup4=0, on4=1, amp4=1|
			var signal1, signal2, signal3, signal4;
			signal1 = In.ar(inputChannels1, 2) * Lag.kr(on1 * allOn * amp1);
			signal1 = Select.ar(channelSetup1,[ (signal1[0]+signal1[1])*0.5, signal1[0], signal1[1] ]);
			signal2 = In.ar(inputChannels2, 2) * Lag.kr(on2 * allOn * amp2);
			signal2 = Select.ar(channelSetup2,[ (signal2[0]+signal2[1])*0.5, signal2[0], signal2[1] ]);
			signal3 = In.ar(inputChannels3, 2) * Lag.kr(on3 * allOn * amp3);
			signal3 = Select.ar(channelSetup3,[ (signal3[0]+signal3[1])*0.5, signal3[0], signal3[1] ]);
			signal4 = In.ar(inputChannels4, 2) * Lag.kr(on4 * allOn * amp4);
			signal4 = Select.ar(channelSetup4,[ (signal4[0]+signal4[1])*0.5, signal4[0], signal4[1] ]);
			SendPeakRMS.kr([signal1,signal2,signal3,signal4] , rate, lag, "/A2M", id);
		}).send(s);
	}

	// method in from osc responders (where the magic happens)
	a2m_in_{|value|
		var coefAdj = p[2].lincurve(1,40,0.25,1,-6);	// scale the coefficients to account for replyRate
		noChannels.do{|i|
			var j       = i*11;   									// j is offset for model index
			var in      = value[(i*2)+p[13+j]].clip(0,1);			// current value in, clipped
			var z1      = lastValue[i];								// 1 unit delay (previous value)
			var upLag   = p[14+j].lincurve(0,1,0,0.99,-4)*coefAdj;	// from 0 - 0.99
			var downLag = p[15+j].lincurve(0,1,0,0.99,-4)*coefAdj;	// from 0 - 0.99
			if (in>z1) { in=(in*(1-upLag))+(z1*upLag) }{ in=(in*(1-downLag))+(z1*downLag) }; // apply a 1 pole lag
			if (((in.abs) - (z1.abs)).abs>0.001) {					// only do if diff is >0.001
				lastValue[i] = in;									// store for z1 value (last value)
				valueInModels[i].lazyValueAction_(in, nil, false);	// update input levels
				in = in.lincurve(0, 1, p[9+j], p[10+j], p[11+j]);	// map input onto min,max & curve
				valueOutModels[i].lazyValueAction_(in, nil, false);	// update output levels
				midi.control(p[12+j], in*127, nil, false, true);	// send midi data
			}
		}
	}

	startDSP{
		synth = Synth.tail(groups[\sideGroup],"LNX_A2M"); // make me a new 1
		node  = synth.nodeID;
	}

	stopDSP{
		if (node.notNil) { server.sendBundle(nil, [11, node]) }; // node could be different than synth
		synth.free;
	}

	updateOnSolo{|latency|
		server.sendBundle(latency +! syncDelay, [\n_set, node, \allOn, this.onNow.if(1,0)] )
	}

	updateDSP{|oldP,latency|
		server.sendBundle(latency +! syncDelay,
			[\n_set, node, \id, 			id],
			[\n_set, node, \rate, 			p[2]],
			[\n_set, node, \lag, 			p[3]],
			[\n_set, node, \allOn, 			this.onNow.if(1,0)],
			[\n_set, node, \channelSetup1,	p[5]],
			[\n_set, node, \inputChannels1,	LNX_AudioDevices.firstFXBus+(p[6]*2) ],
			[\n_set, node, \on1, 			p[7]],
			[\n_set, node, \amp1, 			p[8].dbamp],
			[\n_set, node, \channelSetup2,	p[16]],
			[\n_set, node, \inputChannels2,	LNX_AudioDevices.firstFXBus+(p[17]*2) ],
			[\n_set, node, \on2, 			p[18]],
			[\n_set, node, \amp2, 			p[19].dbamp],
			[\n_set, node, \channelSetup3,	p[27]],
			[\n_set, node, \inputChannels3,	LNX_AudioDevices.firstFXBus+(p[28]*2) ],
			[\n_set, node, \on3, 			p[29]],
			[\n_set, node, \amp3, 			p[30].dbamp],
			[\n_set, node, \channelSetup4,	p[38]],
			[\n_set, node, \inputChannels4,	LNX_AudioDevices.firstFXBus+(p[39]*2) ],
			[\n_set, node, \on4, 			p[40]],
			[\n_set, node, \amp4, 			p[41].dbamp],
		);
	}

	replaceDSP{|latency|
		var previousNode = node;
		node = server.nextNodeID; // i could update synth with new node

		// send the new synth to the server
		server.sendBundle(latency, ([\s_new, "LNX_A2M", node, 4,  previousNode] ++ [
			\id,				id,
			\rate,				p[2],  // rate and lag are both i rate hence a new synth to replace last one
			\lag,				p[3],
			\allOn, 			this.onNow.if(1,0),
			\channelSetup1,		p[5],
			\inputChannels1,	LNX_AudioDevices.firstFXBus+(p[6]*2),
			\on1,				p[7],
			\amp1,				p[8].dbamp,
			\channelSetup2,		p[16],
			\inputChannels2,	LNX_AudioDevices.firstFXBus+(p[17]*2),
			\on2,				p[18],
			\amp2,				p[19].dbamp,
			\channelSetup3,		p[27],
			\inputChannels3,	LNX_AudioDevices.firstFXBus+(p[28]*2),
			\on3,				p[29],
			\amp3,				p[30].dbamp,
			\channelSetup4,		p[38],
			\inputChannels4,	LNX_AudioDevices.firstFXBus+(p[39]*2),
			\on4,				p[40],
			\amp4,				p[41].dbamp,
		]));

	}

} // end ////////////////////////////////////
