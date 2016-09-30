
LNX_Delay : LNX_InstrumentTemplate {

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"Delay"}
	*sortOrder{^3}
	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^true}

	mixerColor{^Color(0.77,0.6,1,0.3)} // colour in mixer

	// fake onOff model
	onOffModel{^fxFakeOnOffModel }
	// and the real one
	fxOnOffModel{^models[1]}

	inModel{^models[2]}
	inChModel{^models[10]}
	outModel{^models[6]}
	outChModel{^models[11]}

	header {
		instrumentHeaderType="SC Delay Doc";
		version="v1.1";
	}

	// an immutable list of methods available to the network
	interface{^#[\netDelayScale]}

	// these are the models and their defaults. you can add to this list as you
	// develope the instrument and it will still save and load previous versions. it
	// will just insert the missing models for you.
	// if you reduce the size of this list it will cause problems when loading older versions.
	// the only 2 items i'm going for fix are 0.solo & 1.onOff

	// the models
	initModel {

		var template=[
			0, // 0.solo

			// 1.onOff
			[1, \switch, midiControl, 1, "On", (\strings_:((this.instNo+1).asString)),
				{|me,val,latency,send| this.setSynthArgVP(1,val,\on,val,latency,send)}],

			1,   // 2. in
			1,    // 3.mix
			0.5,   // 4. delayTime
			0.5, // 5.decay
			1,  // 6.out
			0,0,0, // 7-9. knob controls

			// 10. in channels
			[0, [0,LNX_AudioDevices.defaultFXBusChannels/2-1,\linear,1],
				midiControl, 10, "In Channel",
				(\items_:LNX_AudioDevices.fxMenuList),
				{|me,val,latency,send|
					var i=LNX_AudioDevices.firstFXBus+(val*2);
					this.setSynthArgVH(10,val,\inputChannels,i,latency,send);
			}],

			// 11. out channels
			[\audioOut, midiControl, 11, "Out Channel",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var i;
					i=(((val>=0).if(val*2,LNX_AudioDevices.firstFXBus-(val*2)-2)));
					this.setSynthArgGUIVH(11,val,\outputChannels,i,11,val,latency,send);
			}],

			// 12. beats=1, time=0
			[1, \switch, midiControl, 12, "Beats/Time", (strings_:["Time","Beats"]),
				{|me,val,latency,send| this.setDelayScale(val,latency,send) }],

		];

		["IN","mix","time","decay","OUT"].do{|text,i|
			template[i+2] = [ template[i+2], \unipolar, midiControl, i+2, text,
				(\label_:text,\numberFunc_:\float2),
				{|me,val,latency,send|
					this.setSynthArgVP(i+2,val,
							[\inAmp,\mix,\delayTime,\decay,\outAmp][i],val,latency,send)}]
		};

		#models,defaults=template.generateAllModels;

		models[4].action_{|me,val,latency,send|
			this.setSynthArgVP(4,val,\delayTime,this.getDelayTime(p[12],val),latency,send);
		};

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1,10,11,2,6];
		autoExclusion=[];

	}

	// return the volume model
	volumeModel{^models[6] }

	*thisWidth  {^260}
	*thisHeight {^144}

	createWindow{|bounds| this.createTemplateWindow(bounds,Color(0,1/103,9/77,65/77)) }

	createWidgets{

		// themes

		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background :Color.white));

		gui[\scrollTheme]=( \background	: Color(59/77,59/77,59/77),
						  \border		: Color(0.50602409638554, 0.44615384615385, 0.63636363636364));

		gui[\midiTheme]= ( \rounded_	: true,
						\canFocus_	: false,
						\shadow_		: true,
						\colors_		: (	\up 		: Color(0.31,0.31,0.49),
										\down	: Color(0.31,0.31,0.49),
										\string	: Color.white));

		gui[\knobTheme]=( \labelShadow_	: false,
						\numberWidth_	: (-20),
						\numberFont_	: Font("Helvetica",10),
						\colors_		: (	\on		: Color(0.875, 0.852, 1),
										\label	: Color.black,
										\numberUp	: Color.black,
										\numberDown : Color.white));

		// widgets

		gui[\scrollView] = MVC_RoundedComView(window,
							Rect(11,11,thisWidth-22,thisHeight-23), gui[\scrollTheme]);

		// 10.in
		gui[\in]= MVC_PopUpMenu3(models[10], gui[\scrollView], Rect(  7,7,70,17),gui[\menuTheme]);

		// 11.out
		gui[\out]=MVC_PopUpMenu3(models[11], gui[\scrollView] ,Rect(158,7,70,17),gui[\menuTheme]);

		// 1.onOff
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect(83, 6, 22, 19),gui[\onOffTheme1])
			.color_(\on, Color(0.25,1,0.25) )
			.color_(\off, Color(0.4,0.4,0.4) )
			.rounded_(true);

		// midi control button
		gui[\midi]=MVC_FlatButton(gui[\scrollView],Rect(109, 6, 43, 19),"Cntrl", gui[\midiTheme])
			.action_{ LNX_MIDIControl.editControls(this).front };

		// knobs
		5.do{|i| gui[i]=
			MVC_MyKnob3(models[i+2], gui[\scrollView], Rect(10+(i*45),48,30,30), gui[\knobTheme])
		};

		// 12 Beats/time
		gui[\beats]=MVC_OnOffView(models[12],gui[\scrollView] ,Rect(8, 100, 34, 15))
			.color_(\on,Color(0.64, 0.64, 0.98))
			.color_(\off,Color(0.4,0.4,0.4))
			.font_(Font("Helvetica",11));

		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView], 									47@(gui[\scrollView].bounds.height-23),92,
			Color(0.74, 0.74, 0.88)*0.6,
			Color.black,
			Color(0.74, 0.74, 0.88),
			stringColor:Color.white
		);
		this.attachActionsToPresetGUI;
	}

	// init MIDI
	iInitMidi{ midi.putLoadList([ 1, 0, 1, 0 ]) }

	// fix for fx menus, there has to be a neater way of doing this
	iUpdateGUI{|p| this.updateDelayNumberFunc }

	////////

	*initUGens{|s|

		if (verbose) { "SynthDef loaded: LNX_Delay_FX".postln; };

		SynthDef("LNX_Delay_FX", {
			|
				outputChannels=0,
				inputChannels=4,
				pan=0,
				inAmp=1,
				mix=1,
				delayTime=0.5,
				decay=0.5,
				outAmp=1, on=1
			|
			var in, out;

			in  = In.ar(inputChannels, 2)*inAmp;
			out = SelectX.ar(on.lag,[Silent.ar,in]);

			delayTime=delayTime.clip(0.0074,3);
			out = AllpassL.ar(out,3,
					Lag.ar(delayTime.asAudioRateInput,0.66),(decay*(-1.73))**2*(delayTime+1));
			out = (((mix*outAmp)*out)+(((1-mix)*outAmp)*in));

			out = SelectX.ar(on.lag,[in,out]);
			Out.ar(outputChannels,out);
		}).send(s);

	}

	getDelayTime{|scale,value|
		if (scale==0) {
			^(value*1.4406452869252)**3+0.0074;
		}{
			^(value*31+1).asInt*absTime/8;
		}
	}

	// called from a studio bpm change
	bpmChange{ this.updateDelayTime }

	// gui called from scale button
	setDelayScale{|value,latency,send=true|
		p[12]=value;
		this.setSynthArgVP(4,p[4],\delayTime,this.getDelayTime(p[12],p[4]),latency,send);
		{ this.updateDelayNumberFunc }.defer;
		if (send) { api.groupCmdOD(\netDelayScale,value) };
	}

	// the network method
	netDelayScale{|value|
		p[12]=value;
		this.updateDelayNumberFunc;
		this.updateDelayTime;
		models[12].lazyValue_(value,false);
	}

	updateDelayNumberFunc{
		{
			if (p[12]==0) {
				models[4].dependantsPerform(\numberFunc_,
					{|n| ((n*1.4406452869252)**3+0.0074).asFormatedString(1,3) }).refresh;
			}{
				models[4].dependantsPerform(\numberFunc_,{|n| (n*31+1).asInt/2 }).refresh;
			}
		}.defer
	}

	updateDelayTime{ this.setSynthArgVP(4,p[4],\delayTime,this.getDelayTime(p[12],p[4])) }

	startDSP{
		synth = Synth.tail(fxGroup,"LNX_Delay_FX");
		node  = synth.nodeID;
	}

	stopDSP{ synth.free }

	updateDSP{|oldP,latency|
		var in  = LNX_AudioDevices.firstFXBus+(p[10]*2);
		var out = (p[11]>=0).if(p[11]*2,LNX_AudioDevices.firstFXBus+(p[11].neg*2-2));

		server.sendBundle(latency,
			[\n_set, node, \inAmp          ,p[2]],
			[\n_set, node, \mix            ,p[3]],
			[\n_set, node, \delayTime      ,this.getDelayTime(p[12],p[4])],
			[\n_set, node, \decay          ,p[5]],
			[\n_set, node, \outAmp         ,p[6]],
			[\n_set, node, \inputChannels  ,in  ],
			[\n_set, node, \outputChannels ,out ],
			[\n_set, node, \on             ,p[1]]
		);
		this.updateDelayNumberFunc;	// not best place for this
	}

}
