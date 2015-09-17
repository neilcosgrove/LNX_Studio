
// a grain & sample based drum machine

+ LNX_GSRhythm {

	// the models
	initModel {

		var template=[
		
		// instrument parameters ////////////////////////////////////////////////////////
		
			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me| this.onOffAlt(me.value) }],
				
			// 2.master amp
			[\db6,midiControl, 2, "Master volume",
				(\label_:"Volume" , \numberFunc_:'db',
				mouseDownAction_:{hack[\fadeTask].stop}),
				{|me,val,latency,send,toggle|
					this.setPVPModel(2,val,latency,send);
					
					this.setAllBoth(\amp,latency); // new
					
					//channels.do{|i| this.updateSynthArg(\amp,i,latency)};
				}],	
				
			// 3. out channels		
			[\audioOut, midiControl, 3, "Output channels",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var out;
					if (val>=0) {
						out = val*2
					}{	
						out = LNX_AudioDevices.firstFXBus+(val.neg*2-2);
					};
					this.instOutChannel_(out);
					this.setPVPModel(3,val,0,true);
				}], // test on network
								
			// 4.master pan
			[\pan, midiControl, 4, "Pan",
				(\numberFunc_:\pan, \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(4,val,latency,send);
					channels.do{|i| this.updateSynthArg(\pan,i,latency)};
				}],
			
			// 5.master duration
			[1,\durationMed,midiControl, 5, "Master duration",
				(\label_:"Duration" , \numberFunc_:'float2'),
				{|me,val,latency,send,toggle| this.setPVP(5,val,latency,send)}],
			
			// 6.master pitch
			[\pitch,midiControl, 6, "Master pitch",
				(\label_:"Pitch" , \numberFunc_:'float2Sign', \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(6,val,latency,send);
					channels.do{|i| this.updateSynthArg(\rate,i,latency)};
				}],
			
			// 7.MIDI low 
			[0, \MIDInote, midiControl, 7, "MIDI Low",
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
						me.lazyValueAction_(note,nil,true)} // enter value via keyboard
				}),
				{|me,val,latency,send|
					var value=val.clip(0,p[8]);
					this.setPVP(7,value,latency,send);
					if (value!=val) {{me.value_(value)}.defer(0.05)};
				}],
			
			// 8.MIDI high
			[127, \MIDInote, midiControl, 8, "MIDI High",
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
						me.lazyValueAction_(note,nil,true);} // enter value via keyboard
				}),
				{|me,val,latency,send|
					var value=val.clip(p[7],127);
					this.setPVP(8,value,latency,send);
					if (value!=val) {{me.value_(value)}.defer(0.05)};
				}],
			
			// 9.master bp adjust (-1,1)
			[\bipolar, midiControl, 9, "Prob Adj", (\label_:"Prob", \zeroValue_:0),
				{|me,val,latency,send| this.setPVP(9,val,latency,send)}],
			
			// 10.show sequencer (not networked)
			[1, \switch, midiControl, 10, "Show/Hide Seq",
				{|me,val,latency,send|  p[10]=val; {this.arrangeWindow}.defer }]
				
		];
			
		template=template.extend(289+8,0); // extend the list to add the channel parameters
			
	// MASTER FILTER ///////////////////////////////////////////////////////////////////////
						
		// 11. Master filter on/off		
		template[11]=[0,\switch,midiControl, 11, "Master Filter On/Off", (\strings_:["Filter"]), 
			{|me,val,latency,send,toggle|
				this.setPVP(11,val,latency,send);
				
				this.updateAllFilterOnOff(latency);
				
			}];
							
		// 232. Master filter type 0=lp, 1=hp
		template[232]=[0,\switch,midiControl, 232, "Master Filter type", (\strings_:["LP","HP"]),
			{|me,val,latency,send,toggle|
				this.setPVP(232,val,latency,send);
				this.setAllFilterArgs(\type,latency); // new
				
			}];
		
		// 233. Master filt freq
		template[233]=[20000,\freq,midiControl, 233, "Master Filt Freq",
			(\label_:"Freq" , \numberFunc_:\freq),
			{|me,val,latency,send,toggle|
				this.setPVP(233,val,latency,send);
				//channels.do{|i| this.updateSynthArg(\filtFreq,i,latency)};
				
				this.setAllFilterArgs(\filtFreq,latency); // new
				
			}];
		
		// 234. Master filt res
		template[234]=[0,[0,1,\lin],midiControl, 234, "Master Res",
			(\label_:"Res" , \numberFunc_:\float2),
			{|me,val,latency,send,toggle|
				this.setPVP(234,val,latency,send);
				//channels.do{|i| this.updateSynthArg(\filtRes,i,latency)};
				
				this.setAllFilterArgs(\filtRes,latency); // new
				
			}];
		
		// 235. Master filter Drive
		template[235]=[0.5,[0.5,4,\lin],midiControl, 235, "Master Drive",
			(\label_:"Drive",\numberFunc_:'float2'),
			{|me,val,latency,send,toggle|
				this.setPVP(235,val,latency,send);
				//channels.do{|i| this.updateSynthArg(\drive,i,latency)};
				
				this.setAllFilterArgs(\drive,latency); // new
				
			}];					
						
	// 228-235 MASTER GRAINS //////////////////////////////////////////////////////////
	
		// 228. Master grain on/off
		template[228]=[0,\switch,midiControl, 228, "Master Grain On/Off",
			(\strings_:"Grains"),
			{|me,val,latency,send,toggle| this.setPVP(228,val,latency,send) }];
		
		// 229. Master grain stretch
		template[229]=[0.5,\unipolar,midiControl, 229, "Master Stretch",
			(\label_:"Stretch",\numberFunc_:'stretch',\zeroValue_:0.5),
			{|me,val,latency,send,toggle|
				this.setPVP(229,val,latency,send);
				if (p[228]==1) {
					channels.do{|i|
						this.updateSynthArg(\posRate,i,latency)
					}
				}
			}];
		
		// 230. Master grain density
		template[230]=[60,[15,200,\exp,0,60," Hz"],midiControl, 230, "Master Density",
			(\label_:"Density",\numberFunc_:'freq'),
			{|me,val,latency,send,toggle|
				this.setPVP(230,val,latency,send);
				if (p[228]==1) {
					channels.do{|i|
						this.updateSynthArg(\density,i,latency)
					}
				}
			}];
			
		// 231. Master grain random
		template[231]=[\unipolar,midiControl, 231, "Master Rand",
			(\label_:"Rand",\numberFunc_:'float2'),
			{|me,val,latency,send,toggle|
				this.setPVP(231,val,latency,send);
				if (p[228]==1) {
					channels.do{|i|
						this.updateSynthArg(\rand,i,latency)
					}
				}
			}];
			
		// 260. Master grain overlap
		template[260]=[2,[1,12,\exp],midiControl, 260, "Master Overlap",
			(
			\label_:"Overlap",
				\numberFunc_:'float2'
			),
			{|me,val,latency,send,toggle|
				this.setPVP(260,val,latency,send);
				if (p[228]==1) {
					channels.do{|i|
						this.updateSynthArg(\overlap,i,latency)
					}
				}
			}];
			
		// 286. peak level
		template[286]=[0.7, \unipolar,  midiControl, 286, "Peak Level",
				{|me,val,latency,send| this.setPVP(286,val,latency,send) }];

		// 287. master send channel
		template[287]=[-1, \audioOut,  midiControl, 287, "Master Send Channel",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					this.setPVH(287,val,latency,send);
					this.setAllBoth(\masterSendChannels,latency);
				}];		
				
		// 288. master send amp
		template[288]=[-inf, \db2,  midiControl, 288, "Maseter Send Amp", (label_:"Send"),
				{|me,val,latency,send|
					this.setPVPModel(288,val,latency,send);
					this.setAllBoth(\masterSendAmp,latency);
				}];		
		
	// channels parameters // * * * * * * * * * * * * * * * * * * *//////////////////////
						
		defaultChannels.do{|i|
			
			// 12-19. channel solo
			template[12+i]=[0, \switch, (\strings_:"S"), midiControl, 12+i, "Solo"+(i+1),
				{|me,val,latency,send,toggle| 
					this.channelSolo(i,val,latency,send,toggle)
			}];
			
			// 20-27. channel onOff
			template[20+i]=[1, \switch,  midiControl, 20+i, "OnOff"+(i+1),
				(\strings_:(i+1).asString),
				{|me,val,latency,send,toggle| 
					 this.channelOnOff(i,val,latency,send,toggle)
			}];
			
			// 28-35 channel volumes
			template[28+i]=[\db6,midiControl, 28+i, "Volume"+(i+1),
				(\numberFunc_:'db'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(28+i,val,latency,send);
					this.setBoth(\amp,i,latency); // new
				}];
				
			// 36-43. channel out channel (master vs individual)
			template[36+i]=[0,\audioOutMaster, midiControl, 36+i, "Out channel"+(i+1),
				(\items_:(["Master"]++LNX_AudioDevices.outputAndFXMenuList)),
				{|me,val,latency,send|
					this.setPVH(36+i,val,latency,send);
					this.setBoth(\outputChannels,i,latency);
				}];
			
			// 44-51. channel pan
			template[44+i]=[\pan,midiControl, 44+i, "Pan"+(i+1),
				(\numberFunc_:\pan, \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(44+i,val,latency,send);
					this.updateSynthArg(\pan,i,latency);
				}];
			
			// 52-59. channel duration
			template[52+i]=[1,\duration4,midiControl, 52+i, "Duration"+(i+1),
				(\label_:"Duration" , \numberFunc_:'float2', \zeroValue_:1),
				{|me,val,latency,send,toggle| this.setPVP(52+i,val,latency,send)}];
			
			// 60-67. channel Decay
			template[60+i]=[0.8,\bipolar,midiControl, 60+i, "Decay"+(i+1),
				(\label_:"Decay" , \numberFunc_:'float2', \zeroValue_:0),
				{|me,val,latency,send,toggle| this.setPVP(60+i,val,latency,send)}];
			
			// 68-75. channel pitch
			template[68+i]=[\pitch,midiControl, 68+i, "Pitch"+(i+1),
				(\label_:"Pitch" , \numberFunc_:'float2Sign', \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(68+i,val,latency,send);
					this.updateSynthArg(\rate,i,latency);
				}];
			
			// 76-83. channel send channels	
			template[76+i]=[-1,\audioOut, midiControl, 76+i, "Send channel"+(i+1),
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					
					this.setPVH(76+i,val,latency,send);
					
					this.setBoth(\sendChannels,i,latency);
					
				}];
			
			// 84-91. channel send amps
			template[84+i]=[-inf,\db2,midiControl, 84+i, "Send amp"+(i+1), (label_:"Send"),
				{|me,val,latency,send,toggle|
					
					this.setPVPModel(84+i,val,latency,send);
				//	this.updateSynthArg(\sendAmp,i,latency);
					
					this.setBoth(\sendAmp,i,latency);
				}];
			
			// 92-99. channel choke
			template[92+i]=[0,[0,4,\linear,1],midiControl, 92+i, "Choke"+(i+1),
				(\label_:"Choke" , \numberFunc_:'choke'),
				{|me,val,latency,send,toggle| this.setPVP(92+i,val,latency,send)}];
					
			// 100-107. channel bank
			template[100+i]=[3,[0,channelBanks[i].size-1,\linear,1],
												midiControl, 100+i, "Bank"+(i+1),
				(\items_:channelBanks[i].collect(_.title)),
				{|me,val,latency,send,toggle|
					this.setPVP(100+i,val,latency,send);
					{
					models[108+i]
						.dependantsPerform(\items_,channelBanks[i][val].names)
						.dependantsPerform(\freshAdaptor)
					}.defer;
				}];
				
				
			// 108-115. channel sample
			template[108+i]=[#[0,1,4,5,6,15,16,17]@i,
			
				// this is not the best solution
				[0,channelBanks[i].collect(_.size).sort.last,\linear,1],
			
				midiControl, 108+i, "Sample"+(i+1),
				{|me,val,latency,send,toggle| this.setPVP(108+i,val,latency,send) }];


			// 116-123. channel bp on/off
			template[116+i]=[0,\switch,midiControl, 116+i, "Probability"+(i+1),
				(\strings_:"P"),
				{|me,val,latency,send,toggle|
					this.setPVP(116+i,val,latency,send);
				}];
			
			// 124-131. channel filt freq
			template[124+i]=[20000,\freq,midiControl, 124+i, "Filt Freq"+(i+1),
				(\label_:"Freq" , \numberFunc_:\freq),
				{|me,val,latency,send,toggle|
					this.setPVP(124+i,val,latency,send);
					this.updateFilterArg(\filtFreq,i,latency); // new
				}];
			
			// 132-139. channel filt res
			template[132+i]=[0,[0,1, \lin ],midiControl, 132+i, "Res"+(i+1),
				(\label_:"Res" , \numberFunc_:\float2),
				{|me,val,latency,send,toggle|
					this.setPVP(132+i,val,latency,send);
					this.updateFilterArg(\filtRes,i,latency); // new
				}];
			
			// 140-147. channel filter Drive
			template[140+i]=[0.5,[0.5,4,\lin],midiControl, 140+i, "Drive"+(i+1),
				(\label_:"Drive",\numberFunc_:'float2'),
				{|me,val,latency,send,toggle|
					this.setPVP(140+i,val,latency,send);
					this.updateFilterArg(\drive,i,latency); // new
				}];
					
			// 148-155. channel grain on/off
			template[148+i]=[0,\switch,midiControl, 148+i, "Grain On/Off"+(i+1),
				(\strings_:"Grains"),
				{|me,val,latency,send,toggle| this.setPVP(148+i,val,latency,send) }];
			
			// 156-163. channel grain stretch
			template[156+i]=[0.5,\unipolar,midiControl, 156+i, "Stretch"+(i+1),
				(\label_:"Stretch",\numberFunc_:'stretch',\zeroValue_:0.5),
				{|me,val,latency,send,toggle|
					this.setPVP(156+i,val,latency,send);
					this.updateSynthArg(\posRate,i,latency)
				}];
			
			// 164-171. channel grain density
			template[164+i]=[60,[15,200,\exp,0,60," Hz"],midiControl, 164+i, "Density"+(i+1),
				(\label_:"Density",\numberFunc_:'freq'),
				{|me,val,latency,send,toggle|
					this.setPVP(164+i,val,latency,send);
					this.updateSynthArg(\density,i,latency)
				}];
			
			// 172-179.  channel mod : grain density
			template[172+i]=[\bipolar,midiControl, 172+i, "Denity Mod"+(i+1),
				(\label_:"Density",\numberFunc_:'float2Sign', \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVP(172+i,val,latency,send);
					this.updateSynthArg(\density,i,latency)
				}];
			
			// 180-187. channel grain random movement / pitch
			template[180+i]=[\unipolar,midiControl, 180+i, "Rand"+(i+1),
				(\label_:"Rand",\numberFunc_:'float2'),
				{|me,val,latency,send,toggle|
					this.setPVP(180+i,val,latency,send);
					this.updateSynthArg(\rand,i,latency)
				}];
			
			// 188-195. channel filter type 0=lp, 1=hp
			template[188+i]=[0,\switch,midiControl, 188+i, "Filter type"+(i+1),
				(\strings_:["LP","HP"]), // ["Off","LP","HP"]
				{|me,val,latency,send,toggle|
					this.setPVP(188+i,val,latency,send);
					this.updateFilterArg(\type,i,latency); // new
				}];
				
			// 196-203. channel mod : pitch
			template[196+i]=[\pitch,midiControl, 196+i, "Pitch Mod"+(i+1),
				(\label_:"Pitch" , \numberFunc_:'float2Sign', \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(196+i,val,latency,send);
					this.updateSynthArg(\rate,i,latency);
				}];
			
			// 204-211. channel mod : Decay
			template[204+i]=[0,\bipolar,midiControl, 204+i, "Decay Mod"+(i+1),
				(\label_:"Decay" , \numberFunc_:'float2Sign', \zeroValue_:0),
				{|me,val,latency,send,toggle| this.setPVP(204+i,val,latency,send)}];
			
			// 212-219. channel mod : send
			template[212+i]=[-inf,\db2,midiControl, 212+i, "Send Mod"+(i+1),
				(\label_:"Send" , \numberFunc_:'db'),
				{|me,val,latency,send,toggle|
					this.setPVP(212+i,val,latency,send);
					this.updateSynthArg(\sendAmp,i,latency);
				}];
			
			// 220-227. channel mod : stretch
			template[220+i]=[0.5,\unipolar,midiControl, 220+i, "Stretch Mod"+(i+1),
				(\label_:"Stretch",\numberFunc_:'stretch',\zeroValue_:0.5),
				{|me,val,latency,send,toggle|
					this.setPVP(220+i,val,latency,send);
					this.updateSynthArg(\posRate,i,latency)
				}];
			
			// 228-235. used for master controls
			
			// 236-243. channel filter on/off
			template[236+i]=[0,\switch,midiControl, 236+i, "Filter On/Off"+(i+1),
				(\strings_:["Filter"]), 
				{|me,val,latency,send,toggle|
					this.setPVP(236+i,val,latency,send);
					this.updateFilterOnOff(i,latency); // new
				}];
			
			// 244-251. channel mod : filter freq
			template[244+i]=[\bipolar,midiControl, 244+i, "Filt Freq Mod"+(i+1),
				(\label_:"Freq",\numberFunc_:'float2Sign', \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVP(244+i,val,latency,send);
					this.updateSynthArg(\filtFreq,i,latency)
				}];
				
			//  252-259 channel velocity
			template[252+i]=[0,\unipolar,midiControl, 252+i, "Velocity"+(i+1),
				(\label_:"Vel" , \numberFunc_:'float2Sign', \zeroValue_:1),
				{|me,val,latency,send,toggle| this.setPVP(252+i,val,latency,send) }];
								
			// 261-268. channel grain overlap
			template[261+i]=[2,[1,12,\exp],midiControl, 261+i, "Overlap"+(i+1),
				(
				\label_:"Overlap",
				\numberFunc_:'float2'),
				{|me,val,latency,send,toggle|
					this.setPVP(261+i,val,latency,send);
					this.updateSynthArg(\overlap,i,latency)
				}];
							
			// 269-276.  channel mod : grain overlap
			template[269+i]=[\bipolar,midiControl, 269+i, "Overlap Mod"+(i+1),
				(\label_:"Overlap",\numberFunc_:'float2Sign', \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVP(269+i,val,latency,send);
					this.updateSynthArg(\overlap,i,latency)
				}];
	
			// 277-285.  attack
			template[277+i]=[[0,1,2],midiControl, 277+i, "Attack"+(i+1),
				(\label_:"Attack"),
				{|me,val,latency,send,toggle| this.setPVP(277+i,val,latency,send) }];
				
			// 289-296. static or random sample
			template[289+i]=[0,\switch,midiControl, 289+i, "Rand Smp"+(i+1),
				(\strings_:["R"]),
				{|me,val,latency,send,toggle| this.setPVP(289+i,val,latency,send) }];
	
		};
		
		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=#[0,1,10];
		randomExclusion=#[0,1,2,3,4,7,8,10,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,
						29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,76,77,78,79,80,81,82,83,
						5,6,52,53,54,55,56,57,58,59,68,69,70,71,72,73,74,75,286];
		autoExclusion=#[10];
						
		defaultChannels.do{|i| models[108+i].constrain_(false) }; // this may not work
		
		
		// for seperate filters
		filterModel = [1,\switch,{|me,val| }].asModel;
		
		
	}
	
	// peak / target volume model
	peakModel{^models[286]}
	
	// return the volume model
	volumeModel{^models[2] }
	outChModel{^models[3]}
	
	soloModel{^models[0]}
	onOffModel{^models[1]}
	panModel{^models[4]}
	
	sendChModel{^models[287]}
	sendAmpModel{^models[288]}
	
}

