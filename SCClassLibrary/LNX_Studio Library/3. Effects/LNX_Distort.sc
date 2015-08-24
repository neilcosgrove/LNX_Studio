
LNX_Distort : LNX_InstrumentTemplate {

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id;
		^super.new(server,studio,instNo,bounds,open,id)
	}

	*studioName {^"Drive"}
	*sortOrder{^3}
	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^false}
	
	mixerColor{^Color(0.2,0.2,1,0.2)} // colour in mixer
	
	header { 
		instrumentHeaderType="SC Distort Doc";
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
			1, // 1.onOff
				
			0.1,  // 2. drive
			0,    // 3. type
			0.8,    // 4. out
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
					
		["drive","type","OUT"].do{|text,i|
			if ((i+2)==3) {
				template[i+2] = [ template[i+2], [0,4], midiControl, i+2, text,
					(\label_:text,\numberFunc_:{|n|
						var s;
						case
							{n<0.04}     			{s="tanh"}
							{(n>=0.96)&&(n<=1.04)}	{s="soft"}
							{(n>=1.96)&&(n<=2.04)} 	{s="distort"}
							{(n>=2.96)&&(n<=3.04)} 	{s="clip"}
							{n>3.96}				{s="fold"}
							{true}				{s=n.asFormatedString(1,2)};
						s
					}),
					{|me,val,latency,send| this.setSynthArgVP(i+2,val,
								[\inAmp,\type,\outAmp][i],val,latency,send)}]
			}{
				template[i+2] = [ template[i+2], \unipolar, midiControl, i+2, text,
					(\label_:text,\numberFunc_:\float2),
					{|me,val,latency,send| this.setSynthArgVP(i+2,val,
								[\inAmp,\type,\outAmp][i],val,latency,send)}]
			}
		};
	
		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1,10,11,4];
		autoExclusion=[];
		
	}
	
	// return the volume model
	volumeModel{^models[4] }
	
	*thisWidth  {^220+22}
	*thisHeight {^95+26+22}
	
	createWindow{|bounds| this.createTemplateWindow(bounds,Color.black) }

	createWidgets{
				
		// themes
		
		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background :Color.white));
	
		gui[\scrollTheme]=( \background	: Color(0.766, 0.766, 0.766),
						 \border		: Color(0.445 , 0.462, 0.769));
	
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
		 gui[\midi]=MVC_FlatButton(gui[\scrollView],Rect(85, 6, 43, 19),"Cntrl", gui[\midiTheme])
			.action_{ LNX_MIDIControl.editControls(this).front };
	
		// 10.in
		MVC_PopUpMenu3(models[10],gui[\scrollView] ,Rect(  7,7,70,17),gui[\menuTheme  ]);
		// 11.out
		MVC_PopUpMenu3(models[11],gui[\scrollView] ,Rect(138,7,70,17),gui[\menuTheme  ]);
//		
//		// knob com view
//		gui[\ksv] = MVC_CompositeView(gui[\scrollView], Rect(3,30,thisWidth-28,62),true)
//			.color_(\background,Color(0.478,0.525,0.613));
//		
		// knobs
		3.do{|i| gui[i]=
			MVC_MyKnob3(models[i+2],gui[\scrollView] ,Rect(27+(i*66),48,30,30), gui[\knobTheme])
		};
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],
									17@(gui[\scrollView] .bounds.height-23),88,
			Color(0.74, 0.74, 0.88)*0.6,
			Color.black,
			Color(0.74, 0.74, 0.88),
			stringColor:Color.white
		);
		this.attachActionsToPresetGUI;
	
	}
		
	iInitMIDI{ midi.putLoadList([ 1, 0, 1, 0 ]) }
	
	////////
	
	*initUGens{|s|
	
		if (verbose) { "SynthDef loaded: Drive".postln; };
	
		SynthDef("LNX_Distortion_FX", {
			|
				outputChannels=0,
				inputChannels=4,
				pan=0,
				inAmp=1,
				type=1,
				outAmp=1
			|
			var out;
			out = In.ar(inputChannels, 2)*(((inAmp**2)+0.02)*50);
			out= SelectX.ar(type,[out.tanh,out.softclip,out.distort,out.clip(-1,1),out.fold(-1,1)]);
			
//			out= LeakDC.ar(out);
//			out= SelectX.ar(type,[out.tanh,out.softclip,out.distort,out.clip(-1,inf),out.fold(-1,1)]);
//			out= LeakDC.ar(out);
			
			out = out * ((outAmp**2)*(1-(inAmp/2.6)));
			Out.ar(outputChannels,out);
		}).send(s);

	}
		
	startDSP{
		synth = Synth.tail(fxGroup,"LNX_Distortion_FX");
		node  = synth.nodeID;	
	}
		
	stopDSP{ synth.free }
	
	updateDSP{|oldP,latency|
		var in  = LNX_AudioDevices.firstFXBus+(p[10]*2);
		var out = (p[11]>=0).if(p[11]*2,LNX_AudioDevices.firstFXBus+(p[11].neg*2-2));
		server.sendBundle(latency,
			["/n_set", node, \inAmp ,p[2]],
			["/n_set", node, \type  ,p[3]],
			["/n_set", node, \outAmp,p[4]],
			["/n_set", node, \inputChannels,in],
			["/n_set", node, \outputChannels,out]
		);
		
	}
	
}
