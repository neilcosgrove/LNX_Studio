
 // ************* this instrument is depreciated ************ ///

LNX_Limiter : LNX_InstrumentTemplate {

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"Limiter"}
	*sortOrder{^3}
	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^false}
	
	*isVisible{^false} // this instrument is depreciated 
	
	mixerColor{^Color(0.77,0.6,1,0.3)} // colour in mixer
	
	header { 
		instrumentHeaderType="SC Limiter Doc";
		version="v1.1";
	}
	
	// these are the models and their defaults. you can add to this list as you
	// develope the instrument and it will still save and load previous versions. it
	// will just insert the missing models for you.
	// if you reduce the size of this list it will cause problems when loading older versions.
	// the only 2 items i'm going for fix are 0.solo & 1.onOff

	inModel{^models[2]}
	inChModel{^models[10]}
	outModel{^models[4]}
	outChModel{^models[11]}

	// the models
	initModel {
		
		var template=[
			0, // 0.solo
			
			// 1.onOff
			[1, \switch, midiControl, 1, "On", (permanentStrings_:["I","I"]),
				{|me,val,latency,send| this.setSynthArgVP(1,val,\on,val,latency,send)}],
				
			
			0.5,  // 2. in
			0.5,  // 3. limit
			1,    // 4. out
			0,0,0,0,0, // 5-9. knob controls
			
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
			}]
			
		];
		
		["IN","limit","OUT"].do{|text,i|
			template[i+2] = [ template[i+2], \unipolar, midiControl, i+2, text,
				(\label_:text,\numberFunc_:\float2),
				{|me,val,latency,send| this.setSynthArgVP(i+2,val,
							[\inAmp,\limit,\outAmp][i],val,latency,send)}]
		};
	
		#models,defaults=template.generateAllModels;
		
		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1,10,11,4];
		autoExclusion=[];
		
	}
	
	// return the volume model
	volumeModel{^models[4] }
	
	*thisWidth  {^262}
	*thisHeight {^95+26+22}
	
	createWindow{|bounds| this.createTemplateWindow(bounds,Color.black) }

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
							Rect(11,11,thisWidth-22,thisHeight-22-1), gui[\scrollTheme]);
		
		// midi control button
		MVC_FlatButton(gui[\scrollView] ,Rect(85, 6, 43, 19), "Cntrl", gui[\midiTheme])
			.action_{ LNX_MIDIControl.editControls(this).front };
	
		// 10.in
		MVC_PopUpMenu3(models[10],gui[\scrollView] ,Rect(  7,7,70,17),gui[\menuTheme  ]);
		
		// 11.out
		MVC_PopUpMenu3(models[11],gui[\scrollView] ,Rect(138,7,70,17),gui[\menuTheme  ]);
		
		// knobs
		3.do{|i| gui[i]=
			MVC_MyKnob3(models[i+2],gui[\scrollView],Rect(10+((i+1)*40),48,30,30), gui[\knobTheme])
		};
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],
									17@(gui[\scrollView].bounds.height-23),88,
			Color(0.74, 0.74, 0.88)*0.6,
			Color.black,
			Color(0.74, 0.74, 0.88),
			stringColor:Color.white
		);
		this.attachActionsToPresetGUI;
	
	}
	
	////////
	
	*initUGens{|s|
	
		if (verbose) { "SynthDef loaded: Limiter".postln; };
	
		SynthDef("LNX_Limiter_FX", {
			|
				outputChannels=0,
				inputChannels=4,
				pan=0,
				inAmp=1,
				limit=1,
				outAmp=1
			|
			var out;
			limit=(1-(limit*0.9))**2;
			out = In.ar(inputChannels, 2)*inAmp;
			out = Limiter.ar(out,limit,0.01)*(1/limit);
			out = out * outAmp;
			Out.ar(outputChannels,out);
		}).send(s);

	}
		
	startDSP{
		synth = Synth.tail(fxGroup,"LNX_Limiter_FX");
		node  = synth.nodeID;	
	}
		
	stopDSP{ synth.free }
	
	updateDSP{|oldP,latency|
		var in  = LNX_AudioDevices.firstFXBus+(p[10]*2);
		var out = (p[11]>=0).if(p[11]*2,LNX_AudioDevices.firstFXBus+(p[11].neg*2-2));
		
		server.sendBundle(latency,
			["/n_set", node, \inAmp ,p[2]],
			["/n_set", node, \limit ,p[3]],
			["/n_set", node, \outAmp,p[4]],
			["/n_set", node, \inputChannels,in],
			["/n_set", node, \outputChannels,out]
		);
	}
	
}
