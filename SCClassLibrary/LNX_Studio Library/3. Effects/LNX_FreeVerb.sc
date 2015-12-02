
LNX_FreeVerb : LNX_InstrumentTemplate {

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"Free Verb"}
	*sortOrder{^3}
	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^false}

	mixerColor{^Color(0.3,1,1,0.3)} // colour in mixer

	header { 
		instrumentHeaderType="SC FreeVerb Doc";
		version="v1.1";
	}
	
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
			1, // 1.onOff
		
			1,   // 2. in
			1,    // 3.mix
			0.5,   // 4. room
			0.5, // 5.damp
			1,  // 6.out
			
			0,0,0, // 7-9. knob controls
			
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
					
		["IN","mix","room","damp","OUT"].do{|text,i|
			template[i+2] = [ template[i+2], \unipolar, midiControl, i+2, text,
				(\label_:text,\numberFunc_:\float2),
				{|me,val,latency,send| this.setSynthArgVP(i+2,val,
							[\inAmp,\mix,\room,\damp,\outAmp][i],val,latency,send)}]
		};
	
		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,10,11];
		randomExclusion=[0,1,10,11,2,6];
		autoExclusion=[];
		
	}
	
	// return the volume model
	volumeModel{^models[6] }
	
	*thisWidth  {^220+22}
	*thisHeight {^95+26+22}
	
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
						\numberWidth_	: (-20), 
						\numberFont_	: Font("Helvetica",10),
						\colors_		: (	\on		: Color(0.644, 1, 0.983),
										\label	: Color.black,
										\numberUp	: Color.black,
										\numberDown : Color.white));
	
		// widgets
		gui[\scrollView] = MVC_RoundedComView(window,
							Rect(11,11,thisWidth-22,thisHeight-22-1), gui[\scrollTheme]);
	
		// midi control button
		 gui[\midi]=MVC_FlatButton(gui[\scrollView],Rect(85, 6, 43, 19),"Cntrl", gui[\midiTheme])
			.action_{ LNX_MIDIControl.editControls(this).front };
	
		// 10.in
		gui[\in]=MVC_PopUpMenu3(models[10],gui[\scrollView],Rect(  7,7,70,17),gui[\menuTheme]);
		
		// 11.out
		gui[\out]=MVC_PopUpMenu3(models[11],gui[\scrollView],Rect(138,7,70,17),gui[\menuTheme]);
		
//		// knob com view
//		gui[\ksv] = MVC_CompositeView(gui[\scrollView], Rect(3,30,thisWidth-28,62),true)
//			.color_(\background,Color(0.478,0.525,0.613));
		
		// knobs
		5.do{|i| gui[i]=
			MVC_MyKnob3(models[i+2],gui[\scrollView],Rect(10+(i*40),48,30,30), gui[\knobTheme])
		};
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],
											17@(gui[\scrollView].bounds.height-23),88,
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
				outAmp=1
			|
			var out;
			out = In.ar(inputChannels, 2)*inAmp;
			out = FreeVerb2.ar(out[0],out[1],1,mix,room,damp);
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
			["/n_set", node, \inAmp ,p[2]],
			["/n_set", node, \mix   ,p[3]],
			["/n_set", node, \room  ,p[4]],
			["/n_set", node, \damp  ,p[5]],
			["/n_set", node, \outAmp,p[6]],
			["/n_set", node, \inputChannels,in],
			["/n_set", node, \outputChannels,out]
		);
	}
	
}
