
LNX_FreeVerb : LNX_InstrumentTemplate {

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"Free Verb"}
	*sortOrder{^3}
	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^true}
	
	mixerColor{^Color(0.3,1,1,0.3)} // colour in mixer

	header { 
		instrumentHeaderType="SC FreeVerb Doc";
		version="v1.1";
	}
	
	// fake onOff model
	onOffModel{^fxFakeOnOffModel }
	
	// and the real one
	fxOnOffModel{^models[1]}
	
	inModel{^models[2]}
	inChModel{^models[10]}
	outModel{^models[6]}
	outChModel{^models[11]}
	
	// the models
	initModel {
		
		// these are the models and their defaults. you can add to this list as you
		// develope the instrument and it will still save and load previous versions. it
		// will just insert the missing models for you.
		// if you reduce the size of this list it will cause problems when loading older versions.
		// the only 2 items i'm going for fix are 0.solo & 1.onOff
		
		var template=[
			0, // 0.solo
			
			// 1.onOff
			[1, \switch, midiControl, 1, "On", (\strings_:((this.instNo+1).asString)),
				{|me,val,latency,send| this.setSynthArgVP(1,val,\on,val,latency,send)}],
				
			1,   // 2. in
			1,    // 3.mix
			0.5,   // 4. room
			0.5, // 5.damp
			1,  // 6.out
			0, // 7. hi pass
			20000, // 8. low pass
			0, // 9. knob controls
			
			// 10. in channels
			[0,[0,LNX_AudioDevices.defaultFXBusChannels/2-1,\linear,1],
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
			
			// 12. left delay
			[0, [0,0.2,1], midiControl, 12, "delayL", (\label_:"delayL", numberFunc_:\float2),
				{|me,val,latency,send| this.setSynthArgVP(12,val,\delayL,val,latency,send)}
			],
				
			// 13. right delay
			[0, [0,0.2,1], midiControl, 13, "delayR", (\label_:"delayR", numberFunc_:\float2),
				{|me,val,latency,send| this.setSynthArgVP(13,val,\delayR,val,latency,send)}
			],
							
		];
					
		["IN","mix","room","damp","OUT","hi pass","low pass"].do{|text,i|
			template[i+2] = [ template[i+2],
				[\unipolar,\unipolar,\unipolar,[1,0],\unipolar,\freq,\freq][i]
				, midiControl, i+2, text,
				(\label_:text,
					\numberFunc_:[\float2,\float2,\float2,\float2,\float2,\freq,\freq][i]
				),
				{|me,val,latency,send| this.setSynthArgVP(i+2,val,
					[\inAmp,\mix,\room,\damp,\outAmp,\hiFreq,\lowFreq][i],val,latency,send)}]
		};
	
		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,10,11];
		randomExclusion=[0,1,10,11,2,6];
		autoExclusion=[];
		
	}
	
	// return the volume model
	volumeModel{^models[6] }
	
	*thisWidth  {^262}
	*thisHeight {^200}
	
	createWindow{|bounds| this.createTemplateWindow(bounds,Color.black) }

	createWidgets{
		
		// themes
		
		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background :Color.white));
	
		gui[\scrollTheme]=( \background	: Color(59/77,59/77,59/77),
						  \border		: Color(0.367,0.618,0.620));
	
		gui[\midiTheme]= ( \rounded_	: true,
						\canFocus_	: false,
						\shadow_		: true,
						\colors_		: (	\up 		: Color(0.31,0.489,0.49),
										\down	: Color(0.31,0.489,0.49),
										\string	: Color.white));
										
		gui[\knobTheme]=( \labelShadow_	: false,
						\numberWidth_	: (-15), 
						\numberFont_	: Font("Helvetica",10),
						\colors_		: (	\on		: Color(0.644, 1, 0.983),
										\label	: Color.black,
										\numberUp	: Color.black,
										\numberDown : Color.white));
	
		// widgets
		gui[\scrollView] = MVC_RoundedComView(window,
							Rect(11,11,thisWidth-22,thisHeight-22-1), gui[\scrollTheme]);
							
		// 10.in
		gui[\in]=MVC_PopUpMenu3(models[10],gui[\scrollView],Rect(  7,7,70,17),gui[\menuTheme]);
		
		// 11.out
		gui[\out]=MVC_PopUpMenu3(models[11],gui[\scrollView],Rect(158,7,70,17),gui[\menuTheme]);
		
		// 1.onOff				
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect(83, 6, 22, 19),gui[\onOffTheme1])
			.color_(\on, Color(0.25,1,0.25) )
			.color_(\off, Color(0.4,0.4,0.4) )
			.rounded_(true);
		
		// midi control button
		gui[\midi]=MVC_FlatButton(gui[\scrollView],Rect(109, 6, 43, 19),"Cntrl", gui[\midiTheme])
			.action_{ LNX_MIDIControl.editControls(this).front };
		
		
		// knobs
		5.do{|i| gui[i+2]=
			MVC_MyKnob3(models[i+2],gui[\scrollView],Rect(10+(i*45),48,30,30), gui[\knobTheme])
		};

		// knobs
		2.do{|i| gui[i+7]=
			MVC_MyKnob3(models[i+7],gui[\scrollView],Rect(15+(i*57),108,30,30), gui[\knobTheme])
				.numberWidth_(6)
		};
		
		// delay L & R 12 &13
		MVC_MyKnob3(models[12],gui[\scrollView],Rect(15+(2*57),108,30,30), gui[\knobTheme])
			.color_(\on,Color(0,1,1));
		MVC_MyKnob3(models[13],gui[\scrollView],Rect(15+(3*57),108,30,30), gui[\knobTheme])
			.color_(\on,Color(0,1,1));
		
		gui[7].zeroValue_(20000); // hi pass
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],
											17@(gui[\scrollView].bounds.height-23),108,
			Color(0.7, 0.77, 0.88)*0.6,
			Color.black,
			Color(0.7, 0.77, 0.88),
			stringColor:Color.white
		);
		this.attachActionsToPresetGUI;
	
	}
	
	////////
	
	*initUGens{|s|
	
		if (verbose) { "SynthDef loaded: Free Verb".postln; };
	
		SynthDef("LNX_FreeVerb_FX", {
			|
				outputChannels=0,
				inputChannels=4,
				pan=0,
				inAmp=1,
				mix=1,
				room=0.5,
				damp=0.5,
				outAmp=1,
				hiFreq=20,
				lowFreq=20000,
				on=1,
				delayL=0,delayR=0
			|
			var in, out;
			in  = In.ar(inputChannels, 2)*inAmp;
			out = SelectX.ar(on.lag,[Silent.ar,in]);
			
			out = LPF.ar(out,lowFreq.clip(20,20000).lag(0.2));
			out = HPF.ar(out,hiFreq.clip(20,20000).lag(0.2));
			out = FreeVerb2.ar(out[0],out[1],mix,room,damp);
			out = DelayN.ar(out, 0.2, [delayL.lag,delayR.lag]);
			
			out = SelectX.ar(on.lag,[in,out]);
			out = out * outAmp;
			Out.ar(outputChannels,out);
		}).send(s);

	}
		
	startDSP{
		synth = Synth.tail(fxGroup,"LNX_FreeVerb_FX");
		node  = synth.nodeID;	
	}
		
	stopDSP{ synth.free }
	
	// work out the output channel from p[11], negative numbers are fx buses.
	
	updateDSP{|oldP,latency|
		var out = (p[11]>=0).if(p[11]*2,LNX_AudioDevices.firstFXBus+(p[11].neg*2-2));
		var in  = LNX_AudioDevices.firstFXBus+(p[10]*2);
		
		server.sendBundle(latency,
			["/n_set", node, \inAmp  ,p[2]],
			["/n_set", node, \mix    ,p[3]],
			["/n_set", node, \room   ,p[4]],
			["/n_set", node, \damp   ,p[5]],
			["/n_set", node, \outAmp ,p[6]],
			["/n_set", node, \hiFreq ,p[7]],
			["/n_set", node, \lowFreq,p[8]],
			["/n_set", node, \inputChannels,in],
			["/n_set", node, \outputChannels,out],
			["/n_set", node, \on     ,p[1]],
			["/n_set", node, \delayL ,p[12]],
			["/n_set", node, \delayR ,p[13]]			
		);
	}
	
}
