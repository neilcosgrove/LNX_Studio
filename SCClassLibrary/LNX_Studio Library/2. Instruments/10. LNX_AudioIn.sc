
// audio in & it can do +ive & -ive time syncing to studio clock!!!

LNX_AudioIn : LNX_InstrumentTemplate {

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"Audio In"}
	*sortOrder{^2.88}
	isInstrument{^true}
	canBeSequenced{^false}
	isMixerInstrument{^true}
	mixerColor{^Color(0.3,0.3,0.3,0.2)} // colour in mixer
	hasLevelsOut{^true}
	
	// mixer models
	peakModel   {^models[6]}
	volumeModel {^models[2]}
	outChModel  {^models[4]}
	soloModel   {^models[0]}
	onOffModel  {^models[1]}
	panModel    {^models[5]}
	sendChModel {^models[7]}
	sendAmpModel{^models[8]}
	syncModel   {^models[10]}

	header { 
		// define your document header details
		instrumentHeaderType="SC Audio In Doc";
		version="v1.0";		
	}

	// the models
	initModel {

		#models,defaults=[
			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle|
					this.solo(val,latency,send,toggle);
					if (node.notNil) {server.sendBundle(latency,[\n_set, node, \on, this.isOn])};
				},
				\action2_ -> {|me|
					this.soloAlt(me.value);
					if (node.notNil) {server.sendBundle(nil,[\n_set, node, \on, this.isOn])};
				 }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle|
					this.onOff(val,latency,send,toggle);
					if (node.notNil) {server.sendBundle(latency,[\n_set, node, \on, this.isOn])};
				},
				\action2_ -> {|me|	
					this.onOffAlt(me.value);
					if (node.notNil) {server.sendBundle(nil,[\n_set, node, \on, this.isOn])};
				}],
					
			// 2.master amp
			[\db6,midiControl, 2, "Master volume",
				(\label_:"Volume" , \numberFunc_:'db',mouseDownAction_:{hack[\fadeTask].stop}),
				{|me,val,latency,send,toggle|
					this.setSynthArgVP(2,val,\amp,val.dbamp,latency,send);
				}],	
				
			// 3. in channels
			[0,[0,LNX_AudioDevices.numInputBusChannels/2,\linear,1],
				midiControl, 3, "In Channel",
				(\items_:LNX_AudioDevices.inputMenuList),
				{|me,val,latency,send|
					var in  = LNX_AudioDevices.firstInputBus+(val*2);
					this.setSynthArgVH(3,val,\inputChannels,in,latency,send);
				}],
				
			// 4. out channels		
			[0,\audioOut, midiControl, 4, "Output channels",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var channel = LNX_AudioDevices.getOutChannelIndex(val);
					this.instOutChannel_(channel);
					this.setPVPModel(4,val,0,send);   // to test on network
				}], // test on network
								
			// 5.master pan
			[\pan, midiControl, 5, "Pan",
				(\numberFunc_:\pan, \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setSynthArgVP(5,val,\pan,val,latency,send);
				}],
				
			// 6. peak level
			[0.7, \unipolar,  midiControl, 6, "Peak Level",
				{|me,val,latency,send| this.setPVP(6,val,latency,send) }],
											
			// 7. send channel
			[-1,\audioOut, midiControl, 7, "Send channel",
				(\label_:"Send", \items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					this.setSynthArgVH(7,val,
						\sendChannels,LNX_AudioDevices.getOutChannelIndex(val),latency,send);
				}],
			
			// 8. sendAmp
			[-inf,\db6,midiControl, 8, "Send amp", (label_:"Send"),
				{|me,val,latency,send,toggle|
					this.setSynthArgVH(8,val,\sendAmp,val.dbamp,latency,send);
				}], 		
				
			// 9. channelSetup
			[0,[0,3,\lin,1], midiControl, 9, "Channel Setup",
				(\items_:["Left & Right","Left + Right","Left","Right"]),
				{|me,val,latency,send|
					this.setSynthArgVH(9,val,\channelSetup,val,latency,send);
				}],
				
			// 10. syncDelay
			[\syncTime, midiControl, 10, "Sync",
				{|me,val,latency,send|
					this.setSynthArgVP(10,val,\delay,val,latency,send);
				}],
		
		].generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=(0..9);
		randomExclusion=(0..10);
		autoExclusion=[];

	}

	// GUI
	
	*thisWidth  {^187}
	*thisHeight {^78}
	
	createWindow{|bounds| this.createTemplateWindow(bounds,Color.black,false) }

	// create all the GUI widgets while attaching them to models
	createWidgets{
										
		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\colors_      : (\background :Color.white));
	
		gui[\scrollTheme]=( \background	: Color(0.766, 0.766, 0.766),
						 \border		: Color(0.545 , 0.562, 0.669));
	
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
						
		gui[\theme2]=(	\orientation_  : \horiz,
						\resoultion_	 : 3,
						\visualRound_  : 0.001,
						\rounded_      : true,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.43,0.44,0.43)*1.4,
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.black,
										\focus : Color(0,0,0,0)));
		
		// widgets
		
		gui[\scrollView] = MVC_RoundedComView(window,
							Rect(11,11,thisWidth-22,thisHeight-22-1), gui[\scrollTheme]);
						
		// 3. in	
		MVC_PopUpMenu3(models[3],gui[\scrollView],Rect(5,5,70,17), gui[\menuTheme ] );
	
		// 9. channelSetup
		MVC_PopUpMenu3(models[9],gui[\scrollView],Rect(85,5,75,17), gui[\menuTheme ] );
		
		// 10. syncDelay
		MVC_NumberBox(models[10], gui[\scrollView],Rect(59, 30, 40, 18),  gui[\theme2])
			.labelShadow_(false)
			.label_("Sync")
			.color_(\label,Color.black);
			
		MVC_StaticText(Rect(100,30, 40, 18), gui[\scrollView],)
			.string_("sec(s)")
			.font_(Font("Helvetica",10))
			.shadow_(false)
			.color_(\string,Color.black);

	}
	
	//////////////////////////
		
	*initUGens{|s|
	
		if (verbose) { "SynthDef loaded: Audio In".postln; };
	
		SynthDef("LNX_AudioIn+Delay", {
			|outputChannels=0, inputChannels=2, pan=0, amp=0, delay=0, channelSetup=0, on=1,
			sendChannels=4, sendAmp=0 |
			
			var signal = In.ar(inputChannels, 2);
			
			var signalL, signalR;
			
			signal  = DelayN.ar(signal, \syncTime.asSpec.maxval, delay);
			signal  = signal * Lag.kr(amp*on);
			pan     = Lag.kr(pan*2);
			sendAmp = Lag.kr(sendAmp);
			

			              
			signalL = Select.ar(channelSetup,[
				signal[0], signal[0]+signal[1], signal[0], signal[1] ]);
				
			signalR = Select.ar(channelSetup,[
				signal[1], signal[0]+signal[1], signal[0], signal[1] ]);
				
				
			signal = LinPan2.ar(signalL, (pan-1).clip(-1,1))
			       + LinPan2.ar(signalR, (pan+1).clip(-1,1));

			Out.ar(outputChannels,signal);
						
			Out.ar(  sendChannels,[signal[0]*sendAmp,signal[1]*sendAmp]);
			
		}).send(s);
	
		SynthDef("LNX_AudioIn", {
			|outputChannels=0, inputChannels=2, pan=0, amp=0, channelSetup=0, on=1,
			sendChannels=4, sendAmp=0 |
			
			var signal = In.ar(inputChannels, 2);
			
			var signalL, signalR;
			
			signal  = signal * Lag.kr(amp*on);
			pan     = Lag.kr(pan*2);
			sendAmp = Lag.kr(sendAmp);
			              
			signalL = Select.ar(channelSetup,[
				signal[0], signal[0]+signal[1], signal[0], signal[1] ]);
				
			signalR = Select.ar(channelSetup,[
				signal[1], signal[0]+signal[1], signal[0], signal[1] ]);
				
				
			signal = LinPan2.ar(signalL, (pan-1).clip(-1,1))
			       + LinPan2.ar(signalR, (pan+1).clip(-1,1));

			Out.ar(outputChannels,signal);
						
			Out.ar(  sendChannels,[signal[0]*sendAmp,signal[1]*sendAmp]);
			
		}).send(s);

	}
		
	startDSP{
		synth = Synth.tail(instGroup,"LNX_AudioIn+Delay");
		node  = synth.nodeID;	
	}
		
	stopDSP{ synth.free }
	
	updateOnSolo{|latency|
		if (node.notNil) {server.sendBundle(latency +! syncDelay, [\n_set, node, \on, this.isOn])} 
	}
	
	updateDSP{|oldP,latency|
		var in  = LNX_AudioDevices.firstInputBus+(p[3]*2);
		var out;				
		if (p[4]>=0) {
			out = p[4]*2
		}{	
			out = LNX_AudioDevices.firstFXBus+(p[4].neg*2-2);
		};
		this.instOutChannel_(out,latency);
				
		server.sendBundle(latency,
			[\n_set, node, \amp,p[2].dbamp],
			[\n_set, node, \pan,p[5]],
			[\n_set, node, \inputChannels,in],
			[\n_set, node, \outputChannels,this.instGroupChannel],
			[\n_set, node, \on, this.isOn],
			[\n_set, node, \sendChannels,LNX_AudioDevices.getOutChannelIndex(p[7])],
			[\n_set, node, \sendAmp, p[8].dbamp],
			[\n_set, node, \channelSetup, p[9]],
			[\n_set, node, \delay, p[10] ]
		);
	}

} // end ////////////////////////////////////
