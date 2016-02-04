
// sometimes when server rebooted this ramps up the volume and distorts input, why ??
// time belows up with big changes from zero to a +ive value 

LNX_GVerb : LNX_InstrumentTemplate {

	var lastRoom, lastSpread;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"G Verb"}
	*sortOrder{^3}
	
	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^true}
	
	mixerColor{^Color(0.3,0.3,0.8,0.2)} // colour in mixer
	
	header { 
		instrumentHeaderType="SC G Verb Doc";
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
	outModel{^models[13]}
	outChModel{^models[3]}

	// the models
	initModel {
	
		var template = [
			0, // 0.solo
			
			// 1.onOff
			[1, \switch, midiControl, 1, "On", (\strings_:((this.instNo+1).asString)),
				{|me,val,latency,send| this.setSynthArgVP(1,val,\on,val,latency,send)}],
				
								
			// 2. in channels
			[0,[0,LNX_AudioDevices.defaultFXBusChannels/2-1,\linear,1],
				midiControl, 2, "In Channel",
				(\items_:LNX_AudioDevices.fxMenuList),
				{|me,val,latency,send|
					var i=LNX_AudioDevices.firstFXBus+(val*2);
					this.setSynthArgVH(2,val,\inputChannels,i,latency,send);
			}],

			// 3. out channels		
			[\audioOut, midiControl, 3, "Out Channel",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var i;
					i=(((val>=0).if(val*2,LNX_AudioDevices.firstFXBus-(val*2)-2)));
					this.setSynthArgGUIVH(3,val,\outputChannels,i,3,val,latency,send);
			}],
			
			0.25,     // 4. in
			0.5,    // 5. room
			0.5,   // 6. time
			0.4,  // 7. damp
			0.2,  // 8. dampIn
			0.5, // 9. spread
			0, // 10. dry
			0.5, // 11. early
			0.5, // 12. taillevel
			1, // 13. out
			20, // 14. hp freq
			0, // 15. left delay
			0, // 16. right delay
	
		];
			
		["IN","room","time","damp","dampIn","spread","dry","early","tail","OUT",
			"highPass", "delayL", "delayR"
		
		].do{|text,i|
			template[i+4] = [ template[i+4], \unipolar, midiControl, i+4, text,
				(\label_:text,\numberFunc_:\float2),
				{|me,val,latency,send| this.setSynthArgVP(i+4,val,
				[\inAmp,\room,\time,\damp,\dampIn,\spread,\dry,\early,\taillevel,\outAmp,
					\highPass, \delayL, \delayR
				][i],
				val,latency,send)}]
		};
	
		#models,defaults=template.generateAllModels;
		
		models[14].controlSpec_(\freq);
		models[15].controlSpec_([0,0.2,1]);
		models[16].controlSpec_([0,0.2,1]);
		
		// room & spread
		models[5].action_{|me,val,latency,send| this.setPReplaceVP(5,val,latency,send) };
		models[9].action_{|me,val,latency,send| this.setPReplaceVP(9,val,latency,send) };			
		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1,2,3,4,13];
		autoExclusion=[];
		
	}
	
		// return the volume model
	volumeModel{^models[13] }
	
	*thisWidth  {^257}
	*thisHeight {^203+60}
	
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
						\numberWidth_	: (-20), 
						\numberFont_	: Font("Helvetica",10),
						\colors_		: (	\on		: Color(0.82, 0.79, 0.98),
										\label	: Color.black,
										\numberUp	: Color.black,
										\numberDown : Color.white));
	
		// widgets
		
		gui[\scrollView] = MVC_RoundedComView(window,
								Rect(11,11,thisWidth-22,thisHeight-22-1),gui[\scrollTheme]);
	

	
		// 10.in
		gui[\in] = MVC_PopUpMenu3(models[2],gui[\scrollView],Rect(  7,7,70,17),gui[\menuTheme]);
		
		// 11.out
		MVC_PopUpMenu3(models[3],gui[\scrollView],Rect(158,7,70,17),gui[\menuTheme]);
		
		// 1.onOff				
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect(83, 6, 22, 19),gui[\onOffTheme1])
			.color_(\on, Color(0.25,1,0.25) )
			.color_(\off, Color(0.4,0.4,0.4) )
			.rounded_(true);
		
		// midi control button
		gui[\midi]=MVC_FlatButton(gui[\scrollView],Rect(109, 6, 43, 19),"Cntrl", gui[\midiTheme])
			.action_{ LNX_MIDIControl.editControls(this).front };
		
		// knobs
		13.do{|i|
			gui[i] = MVC_MyKnob3(
				models[i+4],gui[\scrollView],
				Rect((i<10).if( 10+((i%5)*45),  40+((i%5)*60)),48+((i/5).asInt*60),30,30),
				gui[\knobTheme]
			);
		};
		
		gui[10].numberFunc_(\freq);
		gui[10].numberWidth_(0);
		
		gui[1].color_(\on,Color(173/277,1,1));
		gui[5].color_(\on,Color(173/277,1,1));
		gui[11].color_(\on,Color.white);
		gui[12].color_(\on,Color.white);
		
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],
									17@(gui[\scrollView].bounds.height-23),100,
			Color(0.74, 0.74, 0.88)*0.6,
			Color.black,
			Color(0.74, 0.74, 0.88),
			stringColor:Color.white
		);
		this.attachActionsToPresetGUI;
	
	}

	////////
	
	*initUGens{|s|
	
		if (verbose) { "SynthDef loaded: G Verb".postln; };
	
		SynthDef("LNX_GVerb_FX", {
			|
				outputChannels=0,
				inputChannels=4,
				pan=0,
				inAmp=1,
				outAmp=1,
				room=30,      // must start with largest size as maxSize has bug
				time=0.5,
				damp=0.4,
				dampIn=0.2,
				spread=15,
				dry=0,
				early=0.5,
				taillevel=0.5,
				on=1,
				highPass=20, delayL=0, delayR=0
				
			|
			var in, out;
			time=(time*4)**2;
			damp=1-damp;
			in  = In.ar(inputChannels, 2)*inAmp;	
			out = SelectX.ar(on.lag,[Silent.ar,in]);
			
			out = HPF.ar(out,highPass.lag(0.2));
			out = GVerb.ar(out[0]+out[1], 			
					room,
					Lag.kr(time,0.5),
					damp,
					dampIn,
					spread, //spread,
					dry,
					early,
					taillevel,
					room+1
					);
			out = DelayN.ar(out, 0.2, [delayL.lag,delayR.lag]);
			
			out = SelectX.ar(on.lag,[in,out]);
			out = out * outAmp;
			Out.ar(outputChannels,out);
		}).send(s);

	}
		
	startDSP{
		synth = Synth.tail(fxGroup,"LNX_GVerb_FX");
		node  = synth.nodeID;
		lastRoom = lastSpread = nil;
	}
		
	stopDSP{
		if (node.notNil) { server.sendBundle(nil, [11, node]) };
		synth.free;
		lastRoom = lastSpread = nil;	
	}
	
	// this is called by program change and putLoadList
	updateDSP{|oldP,latency|
	
		var out,in, room, spread;
		
		room = ((p[5]*3.64)+1)**3;
		spread = ((p[9]+0.125)*2.48)**3;
		
		if ((room!=lastRoom)or:{spread!=lastSpread}) {
			this.replaceDSP(latency);
		}{
			out=(p[3]>=0).if(p[3]*2,LNX_AudioDevices.firstFXBus+(p[3].neg*2-2));
			in=LNX_AudioDevices.firstFXBus+(p[2]*2);
					
			server.sendBundle(latency,
				[\n_set, node, \inAmp     ,p[4]],
				[\n_set, node, \time      ,p[6]],
				[\n_set, node, \damp      ,p[7]],
				[\n_set, node, \dampIn    ,p[8]],
				[\n_set, node, \dry       ,p[10]],
				[\n_set, node, \early     ,p[11]],
				[\n_set, node, \taillevel ,p[12]],
				[\n_set, node, \outAmp    ,p[13]],
				[\n_set, node, \outputChannels,out],
				[\n_set, node, \inputChannels,in],
				[\n_set, node, \on        ,p[1]],
				[\n_set, node, \highPass  ,p[14]],
				[\n_set, node, \delayL    ,p[15]],
				[\n_set, node, \delayR    ,p[16]]);
		}
	}
	
	replaceDSP{|latency|
		var out,in, previousNode;

		lastRoom = ((p[5]*3.64)+1)**3;
		lastSpread = ((p[9]+0.125)*2.48)**3;
	
		out=(p[3]>=0).if(p[3]*2,LNX_AudioDevices.firstFXBus+(p[3].neg*2-2));
		in=LNX_AudioDevices.firstFXBus+(p[2]*2);
	
		previousNode = node;
		node = server.nextNodeID;
		
		// send the new synth to the server
		server.sendBundle(latency, ([\s_new, "LNX_GVerb_FX", node, 4,  previousNode] ++ [
			\inAmp     ,p[4],
			\room      ,lastRoom,
			\time      ,p[6],
			\damp      ,p[7],
			\dampIn    ,p[8],
			\spread    ,lastSpread,
			\dry       ,p[10],
			\early     ,p[11],
			\taillevel ,p[12],
			\outAmp    ,p[13],
			\outputChannels,out,
			\inputChannels,in,
			\on        ,p[1],
			\highPass  ,p[14],
			\delayL    ,p[15],
			\delayR    ,p[16]
		]));
	
	}
	
}
