
// sometimes when server rebooted this ramps up the volume and distorts input, why ??
// time belows up with big changes from zero to a +ive value

LNX_ExternalFX : LNX_InstrumentTemplate {

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"External FX"}
	*sortOrder{^2.9}

	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^true}

	mixerColor{^Color(0.3,0.3,0.8,0.2)} // colour in mixer

	header {
		instrumentHeaderType="SC External FX Doc";
		version="v1.1";
	}

	// these are the models and their defaults. you can add to this list as you
	// develope the instrument and it will still save and load previous versions. it
	// will just insert the missing models for you.
	// if you reduce the size of this list it will cause problems when loading older versions.
	// the only 2 items i'm going for fix are 0.solo & 1.onOff

	// fake onOff model
	onOffModel{^fxFakeOnOffModel }
	// and the real one
	fxOnOffModel{^models[1]}

	inModel{^models[4]}
	inChModel{^models[2]}
	outModel{^models[5]}
	outChModel{^models[3]}

	// the models
	initModel {

		var template = [
			0, // 0.solo

			// 1.onOff
			[1, \switch, midiControl, 1, "On", (\strings_:((this.instNo+1).asString)),
				{|me,val,latency,send| this.setSynthArgVP(1,val,\on,val,latency,send)}],


			// 2. internal input channel
			[0,[0,LNX_AudioDevices.defaultFXBusChannels/2-1,\linear,1],
				midiControl, 2, "In Channel",
				(\items_:LNX_AudioDevices.fxMenuList),
				{|me,val,latency,send|
					var i=LNX_AudioDevices.firstFXBus+(val*2);
					this.setSynthArgVH(2,val,\inputChannels,i,latency,send);
			}],

			// 3. internal out channel
			[\audioOut, midiControl, 3, "Out Channel",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var value = (((val>=0).if(val*2,LNX_AudioDevices.firstFXBus-(val*2)-2)));
					this.setSynthArgGUIVH(3,val,\outputChannels,value,3,val,latency,send);
			}],

			// 4. in amp
			[\db6, midiControl, 4, "In Amp", (\label_:"In Amp",\numberFunc_:\db),
				{|me,val,latency,send|
					this.setSynthArgVP(4,val,\inAmp,val,latency,send)
			}],

			// 5. out amp
			[\db6, midiControl, 5, "OUT Amp", (\label_:"Out Amp",\numberFunc_:\db),
				{|me,val,latency,send|
					this.setSynthArgVP(5,val,\outAmp,val,latency,send)
			}],

			// 6. external out channel
			[\audioOut, midiControl, 6, "External Out Channel",
				(\items_:LNX_AudioDevices.outputAndFXMenuList, \label_:"To device"),
				{|me,val,latency,send|
					var value = (((val>=0).if(val*2,LNX_AudioDevices.firstFXBus-(val*2)-2)));
					this.setSynthArgGUIVH(6,val,\xOutputChannels,value,6,val,latency,send);
			}],

			// 7. external in channels
			[0,[0,LNX_AudioDevices.numInputBusChannels/2,\linear,1],
				midiControl, 7, "External In Channel",
				(\items_:LNX_AudioDevices.inputMenuList, \label_:"From device"),
				{|me,val,latency,send|
					var value  = LNX_AudioDevices.firstInputBus+(val*2);
					this.setSynthArgVH(7,val,\xInputChannels,value,latency,send);
				}],

			// 8.to external channelSetup
			[0,[0,3,\lin,1], midiControl, 8, "Channel Setup",
				(\items_:["Left & Right","Left + Right","Left","Right"]),
				{|me,val,latency,send|
					this.setSynthArgVH(8,val,\channelSetup,val,latency,send);
				}],

			// 9. from external xChannelSetup
			[0,[0,3,\lin,1], midiControl, 9, "Channel Setup",
				(\items_:["Left & Right","Left + Right","Left","Right"]),
				{|me,val,latency,send|
					this.setSynthArgVH(9,val,\xChannelSetup,val,latency,send);
				}],

			// 10. empty (was mute)
			[0],

			// 11. sendChannels
			[-1, \audioOut, midiControl, 11, "Send Channel",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var channel = LNX_AudioDevices.getOutChannelIndex(val);
					this.setSynthArgVH(11,val,\sendChannels,channel,latency,send);
				}],

			// 12. sendAmp
			[-inf,\db6,midiControl, 12, "Send amp", (label_:"Send"),
				{|me,val,latency,send,toggle|
					this.setSynthArgVP(12,val,\sendAmp,val,latency,send);
				}],

			// 13.low pass
			[20000,\freq,midiControl, 13, "Low pass", (label_:"Low Pass", \numberFunc_:\freq),
				{|me,val,latency,send,toggle|
					this.setSynthArgVP(13,val,\lowFreq,val,latency,send);
				}],

			// 14.high pass
			[20,\freq,midiControl, 14, "High pass", (label_:"High Pass", \numberFunc_:\freq),
				{|me,val,latency,send,toggle|
					this.setSynthArgVP(14,val,\highFreq,val,latency,send);
				}],

			// 15. left delay
			[0, [0,0.2,1], midiControl, 15, "delayL", (\label_:"delayL", numberFunc_:\float2),
				{|me,val,latency,send| this.setSynthArgVP(15,val,\delayL,val,latency,send)}
			],

			// 16. right delay
			[0, [0,0.2,1], midiControl, 16, "delayR", (\label_:"delayR", numberFunc_:\float2),
				{|me,val,latency,send| this.setSynthArgVP(16,val,\delayR,val,latency,send)}
			],

		];

		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1,2,3,4,5];
		autoExclusion=[];

	}

	// return the volume model
	volumeModel{^models[4] }

	*thisWidth  {^261}
	*thisHeight {^263}

	createWindow{|bounds| this.createTemplateWindow(bounds,Color.black) }

	createWidgets{

		// themes

		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background :Color.white));

		gui[\scrollTheme]=( \background	: Color(59/77,59/77,59/77),
						  \border		: Color(0.25,0.257,0.387));

		gui[\midiTheme]= ( \rounded_	: true,
						\canFocus_	: false,
						\shadow_		: true,
						\colors_		: (	\up 		: Color(0.31,0.31,0.49),
										\down	: Color(0.31,0.31,0.49),
										\string	: Color.white));

		gui[\knobTheme]=( \labelShadow_	: false,
						\numberWidth_	: (0),
						\numberFont_	: Font("Helvetica",10),
						\colors_		: (	\on		: Color(0.82, 0.79, 0.98),
										\label	: Color.black,
										\numberUp	: Color.black,
										\numberDown : Color.white));

		gui[\onOffTheme]=( \font_		: Font("Helvetica", 12, true),
		 				\rounded_		: true,
						\colors_      : (\on : Color.red, \off : Color(0.4,0.4,0.4)));

		// widgets

		gui[\scrollView] = MVC_RoundedComView(window,
								Rect(11,11,thisWidth-22,thisHeight-23),gui[\scrollTheme]);

		// midi control button
		MVC_FlatButton(gui[\scrollView],Rect(97,191,43,19),"Cntrl", gui[\midiTheme])
			.action_{ LNX_MIDIControl.editControls(this).front };

		// 1.onOff
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect(108,168,22,19),gui[\onOffTheme1])
			.color_(\on, Color(0.25,1,0.25) )
			.color_(\off, Color(0.4,0.4,0.4) )
			.rounded_(true);

		// 2.in
		MVC_PopUpMenu3(models[2],gui[\scrollView],Rect(7,7,70,17),gui[\menuTheme]);

		// 11. sendChannels
		MVC_PopUpMenu3(models[11],gui[\scrollView],Rect(85,7,70,17),gui[\menuTheme]);

		// 3.out
		MVC_PopUpMenu3(models[3],gui[\scrollView],Rect(163,7,70,17),gui[\menuTheme]);

		// 6. external out channel
		MVC_PopUpMenu3(models[6],gui[\scrollView],Rect(7,170,70,17), gui[\menuTheme ] );

		// 7. external in channels
		MVC_PopUpMenu3(models[7],gui[\scrollView],Rect(163,170,70,17), gui[\menuTheme ] );

		// 8.to external channelSetup
		MVC_PopUpMenu3(models[8],gui[\scrollView],Rect(7,190,70,17), gui[\menuTheme ] );

		// 9. from external xChannelSetup
		MVC_PopUpMenu3(models[9],gui[\scrollView],Rect(163,190,70,17), gui[\menuTheme ] );

		// 4. inAmp
		MVC_MyKnob3(models[4],gui[\scrollView],Rect(28,48,30,30),gui[\knobTheme]);

		// 12. sendAmp
		MVC_MyKnob3(models[12],gui[\scrollView],Rect(105,48,30,30),gui[\knobTheme]);

		// 5. outAmp
		MVC_MyKnob3(models[5],gui[\scrollView],Rect(183,48,30,30),gui[\knobTheme]);

		// 13.low pass
		MVC_MyKnob3(models[13],gui[\scrollView],Rect(20,108,30,30),gui[\knobTheme]);

		// 14.high pass
		MVC_MyKnob3(models[14],gui[\scrollView],Rect(80,108,30,30),gui[\knobTheme])
			.zeroValue_(20000); // cause its hi pass

		// 15. left delay
		MVC_MyKnob3(models[15],gui[\scrollView],Rect(140,108,30,30),gui[\knobTheme]);

		// 16. right delay
		MVC_MyKnob3(models[16],gui[\scrollView],Rect(200,108,30,30),gui[\knobTheme]);

		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView], 17@(gui[\scrollView].bounds.height-23),105,
			Color(0.74, 0.74, 0.88)*0.6,
			Color.black,
			Color(0.74, 0.74, 0.88),
			stringColor:Color.white
		);
		this.attachActionsToPresetGUI;

	}

	////////

	*initUGens{|s|

		if (verbose) { "SynthDef loaded: External FX".postln; };

		SynthDef("External FX", {|outputChannels=0, inputChannels=4, pan=0, inAmp=1, outAmp=1,
			xOutputChannels=0, xInputChannels=0, channelSetup=0, xChannelSetup=0,
			sendChannels=4, sendAmp=0, on=1, lowFreq=20000, highFreq=20,delayL=0,delayR=0|
			var in2Out, out2In, silent, mono;

			var in = In.ar(inputChannels, 2)*(inAmp.dbamp); 			// from LNX input bus
			silent = Silent.ar;											// silence
			in2Out = SelectX.ar(on.lag,[silent,in]);					// on/Off (bypass)
			mono   = in2Out[0]+in2Out[1];
			in2Out = Select.ar(channelSetup,[
				[in2Out[0],in2Out[1]],mono.dup, [mono,silent], [silent,mono]]);
			in2Out = LPF.ar(in2Out,lowFreq.clip(20,20000).lag(0.2));	// the lowpass filter
			in2Out = HPF.ar(in2Out,highFreq.clip(20,20000).lag(0.2));	// and the high pass
			Out.ar(xOutputChannels,in2Out);								// send to external

			out2In = In.ar(xInputChannels, 2);							// return from external
			out2In = DelayN.ar(out2In, 0.2, [delayL.lag,delayR.lag]);   // post delay

			out2In = Select.ar(xChannelSetup,[
				[out2In[0],out2In[1]],(out2In[0]+out2In[1]).dup, out2In[0].dup, out2In[1].dup]);
			out2In = SelectX.ar(on.lag,[in,out2In]);					// on/Off (bypass)
			Out.ar(outputChannels, out2In * (outAmp.dbamp));			// to LNX output bus
			Out.ar(sendChannels  , out2In * (sendAmp.dbamp));				// and LNX sendOut bus

		}).send(s);

	}

	startDSP{
		synth = Synth.tail(fxGroup,"External FX");
		node  = synth.nodeID;
	}

	stopDSP{
		if (node.notNil) {server.sendBundle(nil, [11, node])};
		synth.free;
	}

	// this is called by program change and putLoadList
	updateDSP{|oldP,latency|
		var in=LNX_AudioDevices.firstFXBus+(p[2]*2);
		var out=(p[3]>=0).if(p[3]*2,LNX_AudioDevices.firstFXBus+(p[3].neg*2-2));
		var outCh = LNX_AudioDevices.getOutChannelIndex(p[11]);
		var xin  = LNX_AudioDevices.firstInputBus+(p[7]*2);
		var xout;

		if (p[6]>=0) {
			xout = p[6]*2
		}{
			xout = LNX_AudioDevices.firstFXBus+(p[6].neg*2-2);
		};

		server.sendBundle(latency,
			[\n_set, node, \inAmp     ,p[4]],
			[\n_set, node, \outAmp    ,p[5]],
			[\n_set, node, \outputChannels,out],
			[\n_set, node, \inputChannels,in],
			[\n_set, node, \xOutputChannels,xout],
			[\n_set, node, \xInputChannels,xin],
			[\n_set, node, \channelSetup, p[8]],
			[\n_set, node, \xChannelSetup, p[9]],
			[\n_set, node, \on, p[1]],
			[\n_set, node, \sendAmp, p[12]],
			[\n_set, node, \sendChannels, outCh],
			[\n_set, node, \lowFreq ,p[13]],
			[\n_set, node, \highFreq,p[14]],
			[\n_set, node, \delayL ,p[15]],
			[\n_set, node, \delayR ,p[16]]
		);

	}

}
