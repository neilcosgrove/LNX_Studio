
LNX_Ring : LNX_InstrumentTemplate {

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"Ring Mod"}
	*sortOrder{^3}
	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^false}
	
	mixerColor{^Color(0.77,0.6,1,0.3)} // colour in mixer
	
	header { 
		instrumentHeaderType="SC Ring Doc";
		version="v1.1";
	}
	
	// these are the models and their defaults. you can add to this list as you
	// develope the instrument and it will still save and load previous versions. it
	// will just insert the missing models for you.
	// if you reduce the size of this list it will cause problems when loading older versions.
	// the only 2 items i'm going for fix are 0.solo & 1.onOff

	inModel{^models[3]}
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
				
			60,  // 2. freq
			
			
			0,  // 3. mix
			
			
			0,    // 4. out dbs
			
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
		
		["Freq","Mix","OUT"].do{|text,i|
			template[i+2] = [ template[i+2], [\widefreq,\bipolar,\db6][i], 
				midiControl, i+2, text,
				(\label_:text,\numberFunc_:[\freq,\mix,\db][i]),
				{|me,val,latency,send| this.setSynthArgVP(i+2,val,
							[\freq,\mix,\outAmp][i],val,latency,send)}]
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
						\numberWidth_	: (0), 
						\numberFont_	: Font("Helvetica",10),
						\colors_		: (	\on		: Color(0.875, 0.852, 1),
										\label	: Color.black,
										\numberUp	: Color.black,
										\numberDown : Color.white));
	
		// widgets
		
		gui[\scrollView] = MVC_RoundedComView(window,
							Rect(11,11,thisWidth-22,thisHeight-22-1), gui[\scrollTheme]);
							
		// 10.in
		MVC_PopUpMenu3(models[10],gui[\scrollView],Rect(  7,7,70,17),gui[\menuTheme]);
		
		// 11.out
		MVC_PopUpMenu3(models[11],gui[\scrollView],Rect(158,7,70,17),gui[\menuTheme]);
		
		// 1.onOff				
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect(83, 6, 22, 19),gui[\onOffTheme1])
			.color_(\on, Color(0.25,1,0.25) )
			.color_(\off, Color(0.4,0.4,0.4) )
			.rounded_(true)
			.permanentStrings_(["On"]);
		
		// midi control button
		gui[\midi]=MVC_FlatButton(gui[\scrollView],Rect(109, 6, 43, 19),"Cntrl", gui[\midiTheme])
			.action_{ LNX_MIDIControl.editControls(this).front };
		
		// knobs
		3.do{|i| gui[i]=
			MVC_MyKnob3(models[i+2],gui[\scrollView],Rect(47+(i*55),48,30,30), gui[\knobTheme])
		};
		
		models[3].dependantsPerform(\zeroValue_,0);
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],
												17@(gui[\scrollView].bounds.height-23),108,
			Color(0.74, 0.74, 0.88)*0.6,
			Color.black,
			Color(0.74, 0.74, 0.88),
			stringColor:Color.white
		);
		this.attachActionsToPresetGUI;
	
	}
	
	////////
	
	*initUGens{|s|
	
		if (verbose) { "SynthDef loaded: Ring".postln; };
	
		SynthDef("LNX_Ring_FX", {
			|
				outputChannels=0,
				inputChannels=4,
				freq=60,
				mix=0,
				outAmp=1,
				on=1
			|
			var in, out, ring;
			
			in = In.ar(inputChannels, 2);
			
			ring = in * SinOsc.ar(freq);
			
			out = XFade2.ar(ring,in,mix,outAmp.dbamp);
			
			out = SelectX.ar(on.lag,[in,out]);
			
			Out.ar(outputChannels,out);
		}).send(s);

	}
		
	startDSP{
		synth = Synth.tail(fxGroup,"LNX_Ring_FX");
		node  = synth.nodeID;	
	}
		
	stopDSP{ synth.free }
	
	updateDSP{|oldP,latency|
		var in  = LNX_AudioDevices.firstFXBus+(p[10]*2);
		var out = (p[11]>=0).if(p[11]*2,LNX_AudioDevices.firstFXBus+(p[11].neg*2-2));
		
		server.sendBundle(latency,
			["/n_set", node, \freq ,p[2]],
			["/n_set", node, \mix ,p[3]],
			["/n_set", node, \outAmp,p[4]],
			["/n_set", node, \inputChannels,in],
			["/n_set", node, \outputChannels,out]
		);
	}
	
}
