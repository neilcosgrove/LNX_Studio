
LNX_PitchShift : LNX_InstrumentTemplate {

	var lastSize;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"Pitch Shift"}
	*sortOrder{^3}
	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^true}
	
	mixerColor{^Color(0.77,0.6,1,0.3)} // colour in mixer
	
	header { 
		instrumentHeaderType="SC Pitch Shift Doc";
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
	
	inModel{^models[2]}
	inChModel{^models[10]}
	outModel{^models[7]}
	outChModel{^models[11]}

	// the models
	initModel {
	
		var template = [
			0, // 0.solo
			
			// 1.onOff
			[1, \switch, midiControl, 1, "On", (\strings_:((this.instNo+1).asString)),
				{|me,val,latency,send| this.setSynthArgVP(1,val,\on,val,latency,send)}],
				
				
			1,   // 2. in
			0,    // 3.pitch
			0,   // 4. rand pitch
			0,  // 5. rand time
			0.5, // 6. size
			
			1, // 7.out
			0, // 8.
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
			}]
				
		];
		
		["IN","pitch","rand P","rand T","size","OUT"].do{|text,i|
			var j=i+2;
			if (j==3) {
				template[j] = [ 0, [-24,24], midiControl, j, text,
					(\label_:text,\numberFunc_:\float2,\zeroValue_:0),
					{|me,val,latency,send|
				this.setSynthArgVP(3,val,\pitch,((60+val).midicps)/(60.midicps),latency,send)}]
			}{
				template[j] = [ template[j], (i==4).if([0,2],\unipolar), midiControl, j, text,
					(\label_:text,\numberFunc_:\float2),
					{|me,val,latency,send| this.setSynthArgVP(j,val,
						[\inAmp,\pitch,\randP,\randT,\size,\outAmp][i],val,latency,send)}]
			};
			
		};

		#models,defaults=template.generateAllModels;
		
		models[6].action_{|me,val,latency,send| this.setPReplaceVP(6,val,latency,send) };

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1,10,11,2,7];
		autoExclusion=[];
		
	}
	
	// return the volume model
	volumeModel{^models[7] }
	
	*thisWidth  {^274}
	*thisHeight {^143}
	
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

		// 10.in
		MVC_PopUpMenu3(models[10],gui[\scrollView],Rect(7,7,70,17),gui[\menuTheme]);
		
		// 11.out
		MVC_PopUpMenu3(models[11],gui[\scrollView],Rect(172,7,70,17),gui[\menuTheme]);
		
		// 1.onOff				
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect(87, 6, 22, 19),gui[\onOffTheme1])
			.color_(\on, Color(0.25,1,0.25) )
			.color_(\off, Color(0.4,0.4,0.4) )
			.rounded_(true);
				
		// midi control button
		 gui[\midi]=MVC_FlatButton(gui[\scrollView],Rect(117, 6, 43, 19),"Cntrl", gui[\midiTheme])
			.action_{ LNX_MIDIControl.editControls(this).front };		
		// knobs
		6.do{|i| gui[i]=
			MVC_MyKnob3(models[i+2],gui[\scrollView],Rect(10+(i*40),48,30,30), gui[\knobTheme])
		};
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],
											(17+20)@(gui[\scrollView].bounds.height-23),80,
			Color(0.74, 0.74, 0.88)*0.6,
			Color.black,
			Color(0.74, 0.74, 0.88),
			stringColor:Color.white
		);
		this.attachActionsToPresetGUI;
	
	}
		
	////////
	
	*initUGens{|s|
	
		if (verbose) { "SynthDef loaded: Pitch Shift".postln; };
	
		SynthDef("LNX_PitchShift_FX", {
			|
				outputChannels=0,
				inputChannels=4,
				pan=0,
				inAmp=1,
				pitch=1,
				randP=0,
				randT=0,
				size=0.02,
				mix=1,
				room=0.5,
				damp=0.5,
				outAmp=1,
				on=1
			|
			var in, out;	
			
			in = In.ar(inputChannels, 2)*inAmp;
			
			out=PitchShift.ar(in, size, pitch, randP, randT);

			out = SelectX.ar(on.lag,[in,out]);
			
			out = out * outAmp;
			
			Out.ar(outputChannels,out);
		}).send(s);

	}
		
	startDSP{
		synth = Synth.tail(fxGroup,"LNX_PitchShift_FX");
		node  = synth.nodeID;
		lastSize = nil;	
	}
		
	stopDSP{
		if (node.notNil) {server.sendBundle(nil, [11, node]) };
		synth.free;
		lastSize=nil;
	}
	
	updateDSP{|oldP,latency|
		
		var in,out, size;
		
		size = ((p[6]/4)**3)+0.005;
		
		if (size!=lastSize) {
			// this is done to make window size an active control
			this.replaceDSP(latency);
		}{	
			out=(p[11]>=0).if(p[11]*2,LNX_AudioDevices.firstFXBus+(p[11].neg*2-2));
			in=LNX_AudioDevices.firstFXBus+(p[10]*2);
			
			server.sendBundle(latency,["/n_set", node, \inAmp ,p[2]]);
			server.sendBundle(latency,["/n_set", node, \pitch ,((60+p[3]).midicps)/(60.midicps)]);
			server.sendBundle(latency,["/n_set", node, \randP ,p[4]]);
			server.sendBundle(latency,["/n_set", node, \randT ,p[5]]);
			server.sendBundle(latency,["/n_set", node, \outAmp,p[7]]);
			server.sendBundle(latency,["/n_set", node, \outputChannels,out]);
			server.sendBundle(latency,["/n_set", node, \inputChannels,in]);
			server.sendBundle(latency,["/n_set", node, \on,p[1]]);
		}
		
	}
	
	replaceDSP{|latency|
		var in,out, previousNode;
		
		lastSize =  ((p[6]/4)**3)+0.005;
		
		out=(p[11]>=0).if(p[11]*2,LNX_AudioDevices.firstFXBus+(p[11].neg*2-2));
		in=LNX_AudioDevices.firstFXBus+(p[10]*2);
	
		previousNode = node;
		node = server.nextNodeID;
	
		// send the new synth to the server
		server.sendBundle(latency, ([\s_new, "LNX_PitchShift_FX", node, 4,  previousNode] ++ [
			\inAmp ,p[2],
			\pitch ,((60+p[3]).midicps)/(60.midicps),
			\randP ,p[4],
			\randT ,p[5],
			\size, lastSize,
			\outAmp,p[7],
			\outputChannels,out,
			\inputChannels,in,
			\on, p[1]
		]));
	
//		synth = Synth.replace(synth.nodeID,"LNX_PitchShift_FX",[
//			\inAmp ,p[2],
//			\pitch ,((60+p[3]).midicps)/(60.midicps),
//			\randP ,p[4],
//			\randT ,p[5],
//			\size, lastSize,
//			\outAmp,p[7],
//			\outputChannels,out,
//			\inputChannels,in
//		]);
//		
//		node  = synth.nodeID; // need to update node

	}
	
}
