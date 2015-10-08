
 /////////////////////////////////////////////////////////////////////////////////////////////////
//    //                                                         //                        // N  //
// B  //   BUM NOTE 2                                          +-*-+                   :)  //  O //
//  U //              adapted for lnx 2009-2014                                            // T  //
// M  //   based on the acid_otophilia ugens from the example pieces created by otophilia  //  E //
//    //___________________________________________________________________________________//    //
 /////////////////////////////////////////////////////////////////////////////////////////////////

LNX_BumNote2 : LNX_InstrumentTemplate {
	
	var	<defaultSteps=32, 		<defaultChannels=5;
	
	var	<notesOn, 			recordIndex=0,	lastKeyboardNote,
	 	lastNotePlayedBySeq,	writeIndexModel,	lastNotePlayedByMIDI,		isKeyboardWriting=false;
			
	var	<ampGateOpen=false,	<notePlaying,		<keyPlaying,
		<sequencers, 			<lfoModelA, 		<lfoModelB,
		<lowModel, 			<highModel, 		<pianoRoll,
		<lfoBus, 				<lfoSynth, 		<lfoNodeID, 
		<voicer,				<monoSynth, 		<monoSynthNode,
		<phase=1,				<noteID=0,		<noteIDServer=0;
	
	*new { arg server=Server.default,studio,instNo,bounds,open=true,id;
		^super.new(server,studio,instNo,bounds,open,id)
	}

	*studioName {^"Bum Note 2"}
	*sortOrder{^1}
	isInstrument{^true}
	canBeSequenced{^true}
	isMixerInstrument{^true}
	hasLevelsOut{^true}
	mixerColor{^Color(1,0.75,1,0.4)} // colour in mixer
	
	header { 
		// define your document header details
		instrumentHeaderType="SC BumNote2 Doc";
		version="v1.6";		
	}
	
	// an immutable list of methods available to the network
	interface{^#[]} // only the standard api is needed with bn2 

	// the models
	initModel {
		writeIndexModel = [0,[0,inf,\lin,1]].asModel;
		lfoModelA       = [0,[0,2]].asModel.fps_(20); // the 4 models below are the visual feedback
		lfoModelB       = [0,[0,2]].asModel.fps_(20); // from the server to sclang for the 2 lfos
		lowModel        = \freq.asModel.fps_(20);     // and the two filters
		highModel       = \freq.asModel.fps_(20);
		
		// poly voicer control, for use with the LNX_PianoRollSequencer
		voicer=LNX_Voicer(server);
				
		// the sequencers /////////////////////////////////////////////////////////////////
		
		sequencers=[
		
			// 0. note ON
			MVC_StepSequencer((id.asString++"_Seq_0").asSymbol,
								defaultSteps,midiControl,1000, \switch )
				.name_("Note ON")
				.controlName_("Note ON")
				.action_{|velocity,latency,absBeat,beat| 
					var note;
					if (instOnSolo.isOn) {
						this.ampGateOn(studio.actualLatency); // turn the gate on
						if (p[55].isTrue) { this.filterGateOn(studio.actualLatency) }; // filter
						note=lastNotePlayedBySeq; // because note below is in a defer
						
						if ((p[38].isTrue)and:{lastNotePlayedByMIDI.notNil}) {
							this.acidNote(lastNotePlayedByMIDI,0,latency);
							// force no slide, else it will do it alot.
							lastNotePlayedByMIDI=nil;
						};
						
						// seq latch
						if (p[60].isTrue) {
							sequencers[3].clockIn(beat,latency);
							sequencers[4].clockIn(beat,latency);
							sequencers[5].clockIn(beat,latency);
						};
					};	
				},
				
			// 1. note OFF
			MVC_StepSequencer((id.asString++"_Seq_1").asSymbol,
								defaultSteps,midiControl,2000, \switch )
				.name_("Note OFF")
				.controlName_("Note OFF")
				.action_{|velocity,latency|			
					if (instOnSolo.isOn) {
						this.ampGateOff(studio.actualLatency-0.002); // turn the gate off
					}
				},
				
			// 2. Env - filt env gate now called Accent
			MVC_StepSequencer((id.asString++"_Seq_2").asSymbol,
								defaultSteps,midiControl,3000, \switch )
				.name_("Accent")
				.controlName_("Accent")
				.action_{|velocity,latency| 
					if (instOnSolo.isOn) {
						this.filterGateOn(studio.actualLatency);
					}
				},	
				
			// 3. noteOn Seq
			MVC_StepSequencer((id.asString++"_Seq_3").asSymbol,
								defaultSteps,midiControl,4000, \midiNoteSeq)
				.name_("Note")
				.controlName_("Note")
				.action_{|value,latency,absPos,beatNo,dur,pos|
					if (instOnSolo.isOn) {
						var slide = sequencers[4].seq[pos];
						models[59].valueAction_(value);
						this.midiNote_Seq(p[59], slide);
					}
				},
				
			// 4. slide
			MVC_StepSequencer((id.asString++"_Seq_4").asSymbol,
								defaultSteps,midiControl,5000, \switch )
				.name_("Slide")
				.controlName_("Slide"),
				
			// 5. velocity
			MVC_StepSequencer((id.asString++"_Seq_5").asSymbol,
								defaultSteps,midiControl,6000, \unipolar )
				.name_("Velocity")
				.controlName_("Velocity")
				.action_{|value,latency|
					if (instOnSolo.isOn) {
						if (this.isMono) {
							models[58].valueAction_(value);
							this.setVelocity(value);
						};
					}
				}		
				
		];
		
		// attach alternative models to sequuencers
		
		sequencers[3].spModels[3].addDependant{|model,what,value|
			sequencers[4].spModels[3].valueAction_(value)
		};

		sequencers[3].spModels[4].addDependant{|model,what,value|
			sequencers[4].spModels[4].valueAction_(value)
		};
		
		sequencers[3].spModels[6].addDependant{|model,what,value|
			sequencers[4].spModels[6].valueAction_(value)
		};	

		sequencers[3].spModels[3].addDependant{|model,what,value|
			sequencers[5].spModels[3].valueAction_(value)
		};

		sequencers[3].spModels[4].addDependant{|model,what,value|
			sequencers[5].spModels[4].valueAction_(value)
		};
		
		sequencers[3].spModels[6].addDependant{|model,what,value|
			sequencers[5].spModels[6].valueAction_(value)
		};	
	
		// these are the models and their defaults. you can add to this list as you
		// develop the instrument and it will still save and load previous versions. it
		// will just insert the missing models for you.
		// if u reduce the size of this list it will cause problems when loading older versions
		// the only 2 items i'm going for fix are 0.solo & 1.onOff

		#models,defaults=[
			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me| this.onOffAlt(me.value) }],
			
			// 2.amp
			[ \db6,  midiControl, 2, "Volume",
				(mouseDownAction_:{hack[\fadeTask].stop}, numberFunc_:\db, label_:"Amp"),
				{|me,val,latency,send| this.setSynthArgVP(2,val,\amp,val.dbamp,latency,send)}],
				
			// 3.q
			[0.9,[0,0.9728,-2], (\label_:"Q",\numberFunc_:\float2), midiControl, 3, "Filter Q",
				{|me,val,latency,send|
					var amp;
					this.setPVPModel(3,val,latency,send);
					#amp,val=this.getQandAmp(val);
					this.updateSynthArg(\q   , val, latency);
					this.updateSynthArg(\qAmp, amp, latency);
				}],
				
			// 4.filter env
			[0.23, \bipolar, midiControl, 4, "Filter Env",
				(\label_:"Env",\numberFunc_:\float2,  \zeroValue_:0),
				{|me,val,latency,send|
					this.setSynthArgVP(4,val,\filterEnv,val,latency,send)}],
					
			// 5.pulse width
			[0, [0,0.49], (\label_:"PW",\numberFunc_:\float2), midiControl, 5, "Pulse Width",
				{|me,val,latency,send| this.setSynthArgVP(5,val,\pw,0.5-val,latency,send)}],
					
			// 6.mod freq	
			[1, \lowFreq, midiControl, 6, "LFO Freq",(\label_:"Freq",\numberFunc_:\freq),
				{|me,val,latency,send|
					this.setLFOArgVP(6,val,\lfoFreqA,val,latency,send)}],
			
			// 7.mod amp
			[0, \unipolar, (\label_:"LFO",\numberFunc_:\float2), midiControl, 7, "Pitch LFO",
				{|me,val,latency,send|
					this.setSynthArgVP(7,val,\modAmp,(val**1.75)*4,latency,send)}],
				
			// 8.pulse amp
			[0, \db,  midiControl, 8, "Pulse", (\label_:"Pulse", \numberFunc_:\db),
				{|me,val,latency,send| this.setSynthArgVP(8,val,\pulseAmp,val,latency,send) }],
			
			// 9.filter fq
			[-15.9, [-45,85], midiControl, 9, "Filter Freq",
				(\label_:"Freq",\numberFunc_:\float1),
				{|me,val,latency,send| this.setSynthArgVP(9,val,\filterFq,val,latency,send)}],
			
			// 10.filterLFO
			[0, [-50,50], midiControl, 10, "Filter LFO",(\label_:"LFO", \zeroValue_:0,
				\numberFunc_: {|n| n=n.map(0,50,0,1); n.asFormatedString(1,2) }),
				{|me,val,latency,send|
					this.setSynthArgVP(10,val,\filterLFO,val,latency,send)}],
			
			// 11.spo
			[11, [0,23], midiControl, 11, "Steps per octave",
				(\items_: ((1..24).collect{|i| i+("oct")}), label_:"Steps/Oct"),
				{|me,val,latency,send|
					this.setPVPModel(11,val,latency,send);
					this.updateSynthArg(\osc1Pitch, this.getOscPitch1, latency);
					this.updateSynthArg(\osc2Pitch, this.getOscPitch2, latency);
				}],
			
			// 12.root note
			[\MIDInote, midiControl, 12, "SPO Root", (\label_:"Root"),
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, velocity|
						me.lazyValueAction_(note,nil,true)}}),
				{|me,val,latency,send|
					this.setPVPModel(12,val,latency,send);
					this.updateSynthArg(\osc1Pitch, this.getOscPitch1, latency);
					this.updateSynthArg(\osc2Pitch, this.getOscPitch2, latency);
				}],
					
			// 13.osc1 pitch	
			[0, \pitchAdj48, midiControl, 13, "Osc1 pitch",
				(\label_:"Pitch", \numberFunc_:\intSign, \zeroValue_:0),
				{|me,val,latency,send|
					this.setPVPModel(13,val,latency,send);
					this.updateSynthArg(\osc1Pitch, this.getOscPitch1, latency); 
				}],
			
			// 14.osc1 adjust
			[0, \bipolar, midiControl, 14, "Osc1 fine",
				(\label_:"Fine", \numberFunc_:\float2Sign, \zeroValue_:0),
				{|me,val,latency,send|
					this.setPVPModel(14,val,latency,send);
					this.updateSynthArg(\osc1Pitch, this.getOscPitch1, latency);   
				}],
			
			// 15.osc2 pitch
			[0, \pitchAdj48, midiControl, 15, "Osc2 pitch",
				(\label_:"Pitch", \numberFunc_:\intSign, \zeroValue_:0),
				{|me,val,latency,send|
					this.setPVPModel(15,val,latency,send);
					this.updateSynthArg(\osc2Pitch, this.getOscPitch2, latency);   
				}],
			
			// 16.osc2 adjust
			[0, \bipolar, midiControl, 16, "Osc2 fine",
				(\label_:"Fine", \numberFunc_:\float2Sign, \zeroValue_:0),
				{|me,val,latency,send|
					this.setPVPModel(16,val,latency,send);
					this.updateSynthArg(\osc2Pitch, this.getOscPitch2, latency);  
				}],
									
			// 17. pink noise	
			[-inf, \db,  midiControl, 17, "Noise", (\label_:"Noise",  numberFunc_:\db),
				{|me,val,latency,send| this.setSynthArgVP(17,val,\noise,val,latency,send) }],
			
			// 18.filter KYBD
			[0.2, \bipolar, midiControl, 18, "Filter KYBD",
				(\label_:"KYBD",\numberFunc_:\float2,\zeroValue_:0),
				{|me,val,latency,send| this.setSynthArgVP(18,val,\kybd,val,latency,send) }],
			
			// 19.slide osc pitch
			[0.2, \unipolar, (\label_:"Slide",\numberFunc_:\float2), midiControl, 19, "Slide",
				{|me,val,latency,send| this.setPVP(19,val,latency,send) }],
			
			// 20.Attack (Amp ADSR Env)
			[0.01, [0.001,12,2], (\label_:"A",\numberFunc_:\float2), midiControl, 20, "Attack",
				{|me,val,latency,send|
					this.setPVPModel(20,val,latency,send);
					this.updateSynthArgN(\newAmpEnv, this.getAmpEnvList, latency);
			}],
			
			// 21.Decay (Amp ADSR Env)
			[4, [0,4], (\label_:"D",\numberFunc_:\float2), midiControl, 21, "Decay",
				{|me,val,latency,send|
					this.setPVPModel(21,val,latency,send);
					this.updateSynthArgN(\newAmpEnv, this.getAmpEnvList, latency);
				}],					
			// 22.Sustain (Amp ADSR Env)
			[1, [0,1], (\label_:"S",\numberFunc_:\float2), midiControl, 22, "Sustain",
				{|me,val,latency,send|
					this.setPVPModel(22,val,latency,send);
					this.updateSynthArgN(\newAmpEnv, this.getAmpEnvList, latency);
				}],
				
			// 23.Release (Amp ADSR Env)
			[0.25, [0.01,4,\exp], midiControl, 23, "Release",
				(\label_:"R",\numberFunc_:\float2),
				{|me,val,latency,send|
					this.setPVPModel(23,val,latency,send);
					this.updateSynthArgN(\newAmpEnv, this.getAmpEnvList, latency);
				}],
			
			// 24.Filter Attack (Env)
			[0.001, [0,2],  midiControl, 24, "Filter Attack",
				 (\label_:"A",\numberFunc_:\float2),
				{|me,val,latency,send|
					this.setPVPModel(24,val,latency,send);
					this.updateSynthArgN(\newFilterEnv, this.getLPEnvList, latency);
				}],
			
			// 25.Filter Release (Env)
			[0.72, [0.01,4],  midiControl, 25, "Filter Release",
				(\label_:"R",\numberFunc_:\float2),
				{|me,val,latency,send|
					this.setPVPModel(25,val,latency,send);
					this.updateSynthArgN(\newFilterEnv, this.getLPEnvList, latency);
				}],
				
			// 26.pan
			[\pan, (\label_:"Pan",\numberFunc_:\pan, \zeroValue_:0), midiControl,26,"Pan",
				{|me,val,latency,send| this.setSynthArgVP(26,val,\pan,val,latency,send) }],
			
			// 27.MIDI low 
			[0, \MIDInote, midiControl, 27, "MIDI Low", (\label_:"Low"),
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, velocity|
						me.lazyValueAction_(note,nil,true)} // enter value via keyboard
				}),
				{|me,val,latency,send|
					var value=val.clip(0,p[28]);
					this.setPVP(27,value,latency,send);
					if (value!=val) {{me.value_(value)}.defer(0.05)};
				}],
			
			// 28.MIDI high
			[127, \MIDInote, midiControl, 28, "MIDI High", (\label_:"High"),
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, velocity|
						me.lazyValueAction_(note,nil,true);} // enter value via keyboard
				}),
				{|me,val,latency,send|
					var value=val.clip(p[27],127);
					this.setPVP(28,value,latency,send);
					if (value!=val) {{me.value_(value)}.defer(0.05)};
				}],
			
			// 29.output channels
			[0, \audioOut, midiControl, 29, "Output channels",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var channel = LNX_AudioDevices.getOutChannelIndex(val);
					this.instOutChannel_(channel);
					this.setPVPModel(29,val,0,send);   // to test on network
				}],
			
			// 30.send channels = 4&5
			[-1, \audioOut, midiControl, 30, "Send Channels",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var channel = LNX_AudioDevices.getOutChannelIndex(val);
					this.setSynthArgVP(30,val,\sendChannels,channel,latency,send)}],
		
			// 31.send amp
			[-inf, \db6, midiControl, 31, "Send Amp", (\label_:"Send"),
				{|me,val,latency,send|
					this.setSynthArgVP(31,val,\sendAmp,val.dbamp,latency,send) }],
			
			// 32.filterLag
			[0, \unipolar, midiControl,32, "Filter Glide",
				(\label_:"Glide",\numberFunc_:\float2),
				{|me,val,latency,send|
					this.setSynthArgVP(32,val,\filterLag,val**2,latency,send) }],
			
			// 33.velocity > filter
			[0, \bipolar, midiControl, 33, "Filter Velocity",
				(\label_:"Vel", \numberFunc_:\float2, \zeroValue_:0),
				{|me,val,latency,send|
					this.setSynthArgVP(33,val,\freqSeq,val,latency,send) }],
			
			// 34.EQ Boost High
			[1, \switch, (\strings_:"High"), midiControl, 34, "Boost High",
				{|me,val,latency,send|
					this.setSynthArgVP(34,val,\eqHi,val,latency,send)
				}],
			
			// 35.MIDI NoteOn > Env Gate
			[1, \switch, (\strings_:"ADSR"), midiControl, 35, "ADSR Gate",
				{|me,val,latency,send| this.setPVH(35,val,latency,send) }],
			
			// 36. tri amp	
			[-inf, \db,  midiControl, 36, "Tri Amp", (\label_:"Tri",  numberFunc_:\db),
				{|me,val,latency,send| this.setSynthArgVP(36,val,\fmAmp,val,latency,send) }],
					
			// 37.empty
			[1, [0,2,\lin,1], midiControl, 37, "empty",
				{|me,val,latency,send| }],
			
			// 38. midi note Latch mode
			[0, \switch, (\strings_:["Normal","Latch"]), midiControl, 38, "MIDI Note Latch",
				{|me,val,latency,send| this.setPVH(38,val,latency,send) }],
				
			// 39. filter curve
			[-4, [-10,10], midiControl, 39, "Curve", (\label_:"Curve", \zeroValue_:-4),
				{|me,val,latency,send|
					this.setPVPModel(39,val,latency,send);
					this.updateSynthArgN(\newFilterEnv, this.getLPEnvList, latency);
				}],
			
			// 40.adv counter
			[0, \switch, (\strings_:"Adv"), midiControl, 40, "Advance",
				{|me,val,latency,send| p[40]=val }],
			
			// 41.eq boost low
			[1, \switch, (\strings_:"Low"), midiControl, 41, "Boost Low",
				{|me,val,latency,send| this.setSynthArgVP(41,val,\eq,val,latency,send)}],
			
			// 42.saw amp
			[-inf, \db,  midiControl, 42, "Saw", (\label_:"Saw", numberFunc_:\db ),
				{|me,val,latency,send| this.setSynthArgVP(42,val,\sawAmp,val,latency,send) }],
			
			// 43.link osc''s
			[0, \switch, (\strings_:"Link"), midiControl, 43, "Link Osc",
				{|me,val,latency,send|
					this.setPVPModel(43,val,latency,send);
					this.updateSynthArg(\linkOsc, val, latency);
					{
						models[15].enabled_(val.isFalse);
						models[16].enabled_(val.isFalse);
					}.defer;
				}],
				
			// 44. peak level
			[0.7, \unipolar,  midiControl, 44, "Peak Level",
				{|me,val,latency,send| this.setPVP(44,val,latency,send) }],
				
			// high pass filter ////////////////////////////////////////////
			
			// 45.filter fq
			[-45, [-45,85], midiControl, 45, "HP Filter Freq",
				(\label_:"Freq",\numberFunc_:\float1, \zeroValue_:85),
				{|me,val,latency,send|
					this.setSynthArgVP(45,val,\filterFqHP,val,latency,send)
				}],

			// 46.q
			[0, [0,0.975,-2], (\label_:"Q",\numberFunc_:\float2), midiControl, 46, "HP Filter Q",
				{|me,val,latency,send| this.setSynthArgVP(46,val,\qHP,val,latency,send) }],
				
			// 47.filter env
			[0, \bipolar, midiControl, 47, "HP Filter Env",
				(\label_:"Env",\numberFunc_:\float2,  \zeroValue_:0),
				{|me,val,latency,send|
					this.setSynthArgVP(47,val,\filterEnvHP,val,latency,send)
				}],
					
			// 48.filterLFO
			[0, [-50,50], midiControl, 48, "HP Filter LFO",(\label_:"LFO",\zeroValue_:0,
				\numberFunc_: {|n| n=n.map(0,50,0,1); n.asFormatedString(1,2) }),
				{|me,val,latency,send|
					this.setSynthArgVP(48,val,\filterLFOHP,val,latency,send)
				}],
					
			// 49.filter KYBD
			[0, \bipolar, midiControl, 49, "HP Filter KYBD",
				(\label_:"KYBD",\numberFunc_:\float2,\zeroValue_:0),
				{|me,val,latency,send| this.setSynthArgVP(49,val,\kybdHP,val,latency,send) }],
						
			// 50. tri mod	
			[0, \unipolar,  midiControl, 50, "Tri FM", (\label_:"FM", \numberFunc_:\float2),
				{|me,val,latency,send| this.setSynthArgVP(50,val,\fmMod,val,latency,send) }],
			
			// 51.Filter Attack (Env)
			[0.001, [0,2],  midiControl, 51, "HP Filter Attack",
				 (\label_:"A",\numberFunc_:\float2),
				{|me,val,latency,send| 
					this.setPVPModel(51,val,latency,send);
					this.updateSynthArgN(\newFilterEnvHP, this.getHPEnvList, latency);
				}],
			
			// 52.Filter Release (Env)
			[0.8, [0.01,4],  midiControl, 52, "HP Filter Release",
				(\label_:"R",\numberFunc_:\float2),
				{|me,val,latency,send|
					this.setPVPModel(52,val,latency,send);
					this.updateSynthArgN(\newFilterEnvHP, this.getHPEnvList, latency);
				}],
				
			// 53. filter curve
			[-4, [-10,10], midiControl, 53, "HP Curve",
				(\label_:"Curve", \zeroValue_:-4),
				{|me,val,latency,send|
					this.setPVPModel(53,val,latency,send);
					this.updateSynthArgN(\newFilterEnvHP, this.getHPEnvList, latency);
				}],
				
			// 54. velocity > filter
			[0, \bipolar, midiControl, 54, "HP Filter Velocity",
				(\label_:"Vel", \numberFunc_:\float2,\zeroValue_:0),
				{|me,val,latency,send|
					this.setSynthArgVP(54,val,\freqSeqHP,val,latency,send)
				}],

			// 55. envOn to Filter EnvOn
			[1, \switch, (\strings_:"Filter"), midiControl, 55, "Filter Gate",
				{|me,val,latency,send| this.setPVH(55,val,latency,send) }],
							
			// 56. lfo slope	
			[1,[1,8.5,\exp],  midiControl, 56, "LFO Slope",
				(\label_:"Slope", \numberFunc_:\float2),
				{|me,val,latency,send| this.setLFOArgVP(56,val,\lfoSlopeA,val,latency,send) }],
			
			// 57. lfo wave	
			[0,[0,5,\lin,1],  midiControl, 57, "LFO Wave", (
					\items_:["Sine","Saw Down","Saw Up","Square","Noise 0","Noise 1"]),
				{|me,val,latency,send| this.setLFOArgVP(57,val,\lfoWaveA,val,latency,send) }],
			
			///////
					
			// 58. last velocity
			[0,[0,1,\lin,1], {|me,val,latency,send| p[58]=val } ],
			
			// 59. last note played by keyboard	
			[\midiNoteSeq, {|me,val,latency,send| p[59]=val } ],
			
			// 60. seq note Latch
			[0, \switch, (\strings_:"Latch"), midiControl, 60, "Latch Note",
				{|me,val,latency,send|
					this.setPVH(60,val,latency,send);
					sequencers[3].latch_(val);	
					sequencers[4].latch_(val);
					sequencers[5].latch_(val);
				}],
			
			// 61. PWM	
			[0, \unipolar,  midiControl, 61, "PWM", (\label_:"PMW", \numberFunc_:\float2),
				{|me,val,latency,send| this.setSynthArgVP(61,val,\pwm,val,latency,send) }],
						
			// 62.lfo S&H
			[0, [0,32,\lin,1], midiControl, 62, "S&H", (\label_:"S&H", \numberFunc_:'s&h'),
				{|me,val,latency,send| this.setLFOArgVP(62,val,\lfoSandHA,val,latency,send) }],
				
			// lfo B ////////

			// 63. lfo B wave	
			[0,[0,5,\lin,1],  midiControl, 63, "LFO B Wave", (
					\items_:["Sine","Saw Down","Saw Up","Square","Noise 0","Noise 1"]),
				{|me,val,latency,send| this.setLFOArgVP(63,val,\lfoWaveB,val,latency,send) }],
				
			// 64.lfo B freq	
			[1, \lowFreq, midiControl, 64, "LFO B Freq",(\label_:"Freq",\numberFunc_:\freq),
				{|me,val,latency,send|
					this.setLFOArgVP(64,val,\lfoFreqB,val,latency,send)}],
					
			// 65. lfo B slope	
			[1,[1,8.5,\exp],  midiControl, 65, "LFO B Slope",
				(\label_:"Slope", \numberFunc_:\float2),
				{|me,val,latency,send| this.setLFOArgVP(65,val,\lfoSlopeB,val,latency,send) }],
				
			// 66.lfo B S&H
			[0, [0,32,\lin,1], midiControl, 66, "LFO B S&H", (\label_:"S&H", \numberFunc_:'s&h'),
				{|me,val,latency,send| this.setLFOArgVP(66,val,\lfoSandHB,val,latency,send) }],
				
			// routing
						
			// 67.filterHP lfo A or B			
			[0, \switch, midiControl, 67, "HP LFO", (\strings_:["A","B"]),
				{|me,val,latency,send|
					this.setSynthArgVP(67,val,\hpSelectLFO,val,latency,send) }],

			// 68.filterHP lfo A or B			
			[0, \switch, midiControl, 68, "LP LFO", (\strings_:["A","B"]),
				{|me,val,latency,send|
					this.setSynthArgVP(68,val,\lpSelectLFO,val,latency,send) }],						
			// 69.pitch lfo A or B	
			[0, \switch, midiControl, 69, "LP LFO", (\strings_:["A","B"]),
				{|me,val,latency,send|
					this.setSynthArgVP(69,val,\pitchSelectLFO,val,latency,send) }],		
			// 70.PWM lfo A or B	
			[0, \switch, midiControl, 70, "LP LFO", (\strings_:["A","B"]),
				{|me,val,latency,send|
					this.setSynthArgVP(70,val,\pwmSelectLFO,val,latency,send) }],
			
			// 71. mono/poly
			[0, \switch, (\strings_:["Mono","Poly"]), midiControl, 71, "Mono/Poly",
				{|me,val,latency,send|
					this.setPVPModel(71,val,latency,send);
					this.switchPoly;
					{
						models[35].enabled_(val.isFalse);
						models[38].enabled_(val.isFalse);
						models[73].enabled_(val.isFalse);
						models[19].enabled_(val.isFalse);
					}.defer;
				}],
				
			// 72. poly
			[8,[1,128,\linear,1],midiControl, 72, "Poly",
				{|me,val,latency,send,toggle|
					this.setPVPModel(72,val,latency,send);
					voicer.poly_(val);
					voicer.limitPoly(latency ? studio.actualLatency,0);
				}],
				
			// 73. MIDI NoteOn Velocity > Velocity
			[1, \switch, (\strings_:"Vel"), midiControl, 73, "MIDI Velocity",
				{|me,val,latency,send| this.setPVH(73,val,latency,send) }],
				
			// 74. Velocity > Amp
			[1, \unipolar, (\label_:"Vel", zeroValue_:1), midiControl, 74, "Velocity to Amp",
				{|me,val,latency,send| this.setSynthArgVP(74,val,\velAmp,val,latency,send) }],
					
		].generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,40,58,59];
		randomExclusion=[0,1,2,11,12,27,28,29,30,40,44,58,59];
		autoExclusion=[];
		
		// networked pianoRoll
		pianoRoll = LNX_PianoRollSequencer(id++\pR)
			.action_{|note,velocity,latency|
				this.noteOn2(note, velocity*127,latency);
			}
			.offAction_{|note,velocity,latency|
				this.noteOff2(note, velocity*127,latency);
			}
			.releaseAllAction_{ voicer.releaseAllNotes(studio.actualLatency) }
			.keyDownAction_{|me, char, modifiers, unicode, keycode|
				gui[\keyboardView].view.keyDownAction.value(me,char, modifiers, unicode, keycode)
			}
			.keyUpAction_{|me, char, modifiers, unicode, keycode|
				gui[\keyboardView].view.keyUpAction.value(me, char, modifiers, unicode, keycode)
			}
			.recordFocusAction_{ gui[\keyboardView].focus }
			//.spoModel_(models[11]) // wrong spec
			;

	}
	
	// is the synth isn mono or poly mode
	isMono{^p[71].isFalse}
	isPoly{^p[71].isTrue}
	
	// peak / target volume model
	peakModel{^models[44]}
	
	// return the volume model
	volumeModel{^models[2] }
	outChModel{^models[29]}
	
	soloModel{^models[0]}
	onOffModel{^models[1]}
	panModel{^models[26]}
	
	sendChModel{^models[30]}
	sendAmpModel{^models[31]}
	
	// your own vars
	iInitVars{
		notesOn=[];
	}
		
	// anything else that needs doing after a server reboot; 
	iServerReboot{}
		
	// for your own clear, used to when loading studio preset 
	iClear{}
	
	iFreeAutomation{
		sequencers.do(_.freeAutomation);
		//pianoRoll.freeAutomation; // pRoll doesn't have any yet?
	}
	
	// for freeing anything when closing
	iFree{
		sequencers.do(_.free);
		pianoRoll.free;
	}
	
	iPostNew{
		{ pianoRoll.resizeToWindow }.defer(0.02);
	}
	
	////////////////////////////
	
	// set args to the lfo
	setLFOArgVP{|index,value,synthArg,argValue,latency,send|
		send = send ? true;
		p[index]=value;

		server.sendBundle(latency,[\n_set, node, synthArg, argValue]);

		if ((network.isConnected)and:{send}) {
			api.sendVP((id+""++index).asSymbol,\netSynthArg,index,value,synthArg,argValue);
		};
	}

	// set args to the main synth body and network
	setSynthArgVP{|index,value,synthArg,argValue,latency,send|
		send = send ? true;
		p[index]=value;
		
		if (this.isMono) {
			server.sendBundle(latency,[\n_set, monoSynthNode, synthArg, argValue]);
		}{
			voicer.allNodes.do{|voicerNode|
				server.sendBundle(latency,[\n_set, voicerNode.node, synthArg, argValue]);
			};	
		};
		
		if ((network.isConnected)and:{send}) {
			api.sendVP((id+""++index).asSymbol,\netSynthArg,index,value,synthArg,argValue);
		};
	}
	
	// network of above
	netSynthArg{|index,value,synthArg,argValue|
		if (p[index]!=value) {
			p[index]=value;
			models[index].lazyValue_(value,false);
			if (this.isMono) {
				server.sendBundle(nil,[\n_set, monoSynthNode, synthArg, argValue]);
			}{
				voicer.allNodes.do{|voicerNode|
					server.sendBundle(nil,[\n_set, voicerNode.node, synthArg, argValue]);
				};	
			};
		};
	}

	// called from the models to update synth arguments
	updateSynthArg{|synthArg,argValue,latency|
		if (this.isMono) {
			server.sendBundle(latency,[\n_set, monoSynthNode, synthArg, argValue]);
		}{
			voicer.allNodes.do{|voicerNode|
				server.sendBundle(latency,[\n_set, voicerNode.node, synthArg, argValue]);
			};	
		};
	}

	// set main body args, no network here
	updateSynthArgN{|synthArg,list,latency|
		if (this.isMono) {
			server.sendBundle(latency,[\n_setn, monoSynthNode, synthArg] ++ list);
		}{
			voicer.allNodes.do{|voicerNode|
				server.sendBundle(latency,[\n_setn, voicerNode.node, synthArg] ++ list);
			};	
		};	
	}
	
	// called from the models to update synth arguments
	updateLFOArg{|synthArg,val,latency|
		server.sendBundle(studio.actualLatency,[\n_set,node,synthArg,val]);
	}
	
	///////////////////////////
	
	// get the current state as a list
	iGetPresetList{
		var l;
		l=[sequencers[0].iGetPrestSize];
		sequencers.do{|s| l=l++(s.iGetPresetList) };
		^pianoRoll.iGetPresetList ++ l;  // pRoll is 1st
	}
	
	// add a statelist to the presets
	iAddPresetList{|l|
		var presetSize;
		pianoRoll.iAddPresetList(l.popEND("*** END Score DOC ***").reverse);
		presetSize=l.popI;
		sequencers.do{|s| s.iAddPresetList(l.popNF(presetSize)) };
	}
	
	// save a state list over a current preset
	iSavePresetList{|i,l|
		var presetSize;
		pianoRoll.iSavePresetList(i,l.popEND("*** END Score DOC ***").reverse);
		presetSize=l.popI;
		sequencers.do{|s| s.iSavePresetList(i,l.popNF(presetSize)) };
	}

	// for your own load preset
	iLoadPreset{|i,newP,latency|
		pianoRoll.iLoadPreset(i);
		sequencers.do{|s| s.iLoadPreset(i) };
		{this.iUpdateGUI(newP)}.defer;
	}
	
	// for your own remove preset
	iRemovePreset{|i|
		pianoRoll.iRemovePreset(i);
		sequencers.do{|s| s.iRemovePreset(i) };	
	}
	
	// for your own removal of all presets
	iRemoveAllPresets{
		pianoRoll.iRemoveAllPresets;
		sequencers.do{|s| s.iRemoveAllPresets };
	}
	
	// clear the sequencer
	clearSequencer{
		pianoRoll.clear;
		sequencers.do(_.clearSequencer);
	}
	
	////////////////////////////////
	
	// for your own saving
	iGetSaveList{
		var l =[sequencers.size];
		sequencers.do{|s| l=l++(s.getSaveList) };	// the step sequencers
		l=l++(pianoRoll.getSaveList);             // the pianoRoll
		^l
	}
	
	// only used here to correct out & send channels to new list not including in channels
	preLoadP{|l,loadVersion|
		// changes output channels
		if (loadVersion<1.5) {
			[29,30].do{|j| var i=l[j];  l[j]=((i<0).if(i,(i>1).if(1-i,0))) }; // this converts
		};
		^l
	} // used to change p in Bum2
		
	// for your own loading
	iPutLoadList{|l,noPre,instrumentLoadVersion,templateLoadVersion|
		var channels;		
		channels=l.popI; // not used yet but here if needed
		sequencers.do{|s| s.putLoadList(l.popEND("*** END OBJECT DOC ***")) };
		if (instrumentLoadVersion>=1.5) {		
			pianoRoll.putLoadList( l.popEND("*** END PRSeq DOC ***") ); // the pianoRoll	
		};
		^l 
	}
	
	// for your own loading inst update
	iUpdateGUI{|p|
		var val,enabled, oldRulerValue;
		models[15].enabled_(#[true,false].at(p[43]));
		models[16].enabled_(#[true,false].at(p[43]));
	}
	
	// anything else that needs doing after a load. all paramemters will be loaded by here
	iPostLoad{
		// temp fix to use the right filter
		this.stopDSP;
		this.startDSP;
		this.updateDSP;
	}
	
	iPostInit{}
	
	// get various states of the synth in a list so they can be sent to the server
	getAmpEnvList{
		var env = Env.adsr(p[20], p[21], p[22], p[23], 1, -4).asArray;
		^[env.size]++env
	}
	
	getLPEnvList{
		var env = Env.perc(p[24]**2, p[25], 1, [p[39].neg,p[39]]).asArray;
		^[env.size]++env
	}
	
	getHPEnvList{
		var env = Env.perc(p[51]**2, p[52], 1, [p[53].neg,p[53]]).asArray;
		^[env.size]++env
	}
	
	getQandAmp{|val| 
		^[LNX_ControlMap.value(\moogLadderQAmp,val), LNX_ControlMap.value(\moogLadderQ,val)]
	}
	
	getOscPitch1{^((p[13]+p[14])/(p[11]+1)*12)}
	
	getOscPitch2{^((p[15]+p[16])/(p[11]+1)*12)}
	
	// feedback from server lfos to user interface
	lfo_{|valA,valB|
		lfoModelA.valueAction_(valA,nil,false);
		lfoModelB.valueAction_(valB,nil,false);
	}
	// feedback from server filter to user interface
	filter_{|valLow,valHigh,argNoteID|		
		if  (((this.isMono)and:{ argNoteID==0}) or: {noteID==argNoteID}) {
			{
				lowModel.valueAction_(valLow,nil,false);
				highModel.valueAction_(valHigh,nil,false);
			}.defer;
		}
	}
	
	//// uGens & midi players //////////////////////////

	// this will be called by studio after booting
	*initUGens{|server|
	
		if (verbose) { "SynthDef loaded: Bum Note".postln; };
		
		// the lfos run separately so they can be used in mono & poly mode
		
		SynthDef("BumNote_LFO", {
						
			arg	lfoFreqA=1, 		lfoSlopeA=1,		lfoWaveA=0,
				lfoSandHA=0,		lfoFreqB=1, 		lfoSlopeB=1,
				lfoWaveB=0,		lfoSandHB=0,		clockIn,
				lfoOutChannel,	id;
			
			var	lfoOutA,	lfoOutB;
			
			// LFO A			
			lfoOutA = LFSaw.ar(lfoFreqA); 		// ar!!
			lfoOutA = (lfoOutA.abs**lfoSlopeA) * (lfoOutA.sign); // slope
			lfoOutA = Select.ar(lfoWaveA, [
				sin (lfoOutA + 1 * (0.5 * 2pi)),	// sine
				(lfoOutA.neg),				// saw down
				lfoOutA,						// saw up
				(lfoOutA<0.025)*2-1,			// square
				LFNoise0.ar(lfoFreqA*2),		// noise 0
				LFNoise1.ar(lfoFreqA*2)			// noise 1
			]);
			
			// S&H lfo
			lfoOutA = Select.ar( lfoSandHA.clip(0,1) , [ lfoOutA,
				Latch.ar(lfoOutA, Slope.kr((clockIn/ (lfoSandHA.clip(1,9999)) ).floor)) 
			]);
						
			// LFO B
			lfoOutB = LFSaw.ar(lfoFreqB); 		// ar!!
			lfoOutB = (lfoOutB.abs**lfoSlopeB) * (lfoOutB.sign); // slope
			lfoOutB = Select.ar(lfoWaveB, [
				sin (lfoOutB + 1 * (0.5 * 2pi)),	// sine
				(lfoOutB.neg),				// saw down
				lfoOutB,						// saw up
				(lfoOutB<0.025)*2-1,			// square
				LFNoise0.ar(lfoFreqB*2),		// noise 0
				LFNoise1.ar(lfoFreqB*2)			// noise 1
			]);
			
			// S&H lfo
			lfoOutB = Select.ar( lfoSandHB.clip(0,1) , [ lfoOutB,
				Latch.ar(lfoOutB, Slope.kr((clockIn/ (lfoSandHB.clip(1,9999)) ).floor)) 
			]);
			
			Out.ar(lfoOutChannel,[lfoOutA,lfoOutB]);
			
			// and back to the client
			SendPeakRMS.kr([lfoOutA+1,lfoOutB+1], 20, 0, "/lfo", id);
			
			
		}).send(server);

		SynthDef("BumNote_PolyBody", {
			
			arg	outputChannels=0,	gate=1, 			amp=0.1,
				pw=0.5,			velocity=0,		filterFq=0,
				q=0.3,			qAmp=1,			filterEnv=1,
				hpSelectLFO=0,	lpSelectLFO=0,	pitchSelectLFO=0,
				pwmSelectLFO=0,	lfoOutChannel=0,	filterLFO=0,
				osc1Pitch=0,		osc2Pitch=0,		kybd=1,
				pan=0,			filterLag=0,		sendAmp=0,
				sendChannels=4,	pGate=0,			eq=1,
				eqHi=1,			linkOsc=0,		freqSeq=0,
				pulseAmp=0,		sawAmp=0,			noise=0,
				fmAmp=0,			fmMod=0,			velAmp=1,
				filterFqHP=(-45),	qHP=0.3,			qAmpHP=1,
				filterEnvHP=1,	kybdHP=0, 		filterLFOHP=0,
				freqSeqHP=0,		filterGate=0,		pwm=0,
				id=0,			pitch=34,			slide=0,
				modAmp=0,			noteID=0,			phase=1;
				
			var	ampEnv, 			env2,			out,
			 	out1, 			out2, 			out3,
				lfoOutA, 			lfoOutB,			lfoOutFilterLP,
				lfoOutPW,			sawFreq,			pulseFreq,
			    	envctl, 			myEnv,			lfoPitch,
			    	envctl2, 			myEnv2, 			filterResponse,
			    	pEnv, 			pc,
			    	pitch1,			pitch2,			myEnv2HP,
			    	envctl2HP,		env2HP,			lfoOutFilterHP,
			    	outPink,			outFM,			filterResponseHP,
			    	lfoPWM,			in;
			
			// amp envelope
			myEnv  = Env.adsr(0.001, 2  , 0, 0.04,  1, -4);
			envctl = Control.names([\newAmpEnv]).kr( myEnv.asArray );
			ampEnv = EnvGen.ar(envctl,  gate, doneAction:2);
			
			// filter envelope
			myEnv2 = Env.perc(0.001, 0.8, 1, -4);
			envctl2= Control.names([\newFilterEnv]).kr( myEnv2.asArray );
			env2   = EnvGen.ar(envctl2, filterGate, filterEnv*70*1.5);

			// hp filter envelope
			myEnv2HP = Env.perc(0.001, 0.8, 1, -4);
			envctl2HP= Control.names([\newFilterEnvHP]).kr( myEnv2HP.asArray );
			env2HP   = EnvGen.ar(envctl2HP, filterGate, filterEnvHP*70);

			// pitch & slide with VarLag 
			pitch = VarLag.kr(pitch, slide, Env.shapeNumber(\lin));

			// get lfo's in			
			in = In.ar(lfoOutChannel,2);
			lfoOutA = in[0];
			lfoOutB = in[1];
			
			// the lfo adjusted for each mod
			lfoOutFilterLP = Select.ar(lpSelectLFO,    [lfoOutA, lfoOutB] ) * filterLFO;
			lfoOutFilterHP = Select.ar(hpSelectLFO,    [lfoOutA, lfoOutB] ) * filterLFOHP;
			lfoPitch       = Select.ar(pitchSelectLFO, [lfoOutA, lfoOutB] ) * modAmp;
			lfoPWM         =(Select.ar(pwmSelectLFO,   [lfoOutA, lfoOutB] ) + 1) *pwm*0.25;
						
			// the two pitches of the osc's
			pitch1 = pitch + osc1Pitch + lfoPitch;
			pitch2 = pitch + osc2Pitch + lfoPitch;
			
			// now work out their freq's
			sawFreq = ((pitch1*linkOsc)+(pitch2*(1-linkOsc))).midicps.clip(0,22050);
			pulseFreq = pitch1.midicps.clip(0,22050);
			
			// the oscillators
			out1    = Pulse.ar(pulseFreq,
						(pw-(lfoPWM*(pw*2)) ).clip(0.01,0.5) // the pw
					, pulseAmp.dbamp*0.2);     		         // the pulse
			out2    = Saw.ar(sawFreq,sawAmp.dbamp*0.266);      // the best saw
			
			outPink = PinkNoise.ar(noise.dbamp*0.25);                          // the pink
			outFM   = SinOsc.ar(sawFreq, SinOsc.ar(pulseFreq,0,fmMod*1.5)).asin// the tri (fm)
						* (fmAmp.dbamp / pi * 0.835);                              
						
			// mix it all together
			out  = (out1 + out2 + outPink + outFM) * (AmpComp.ar(pitch + lfoPitch).clip(0,2))*0.2;
			
			// and leak any dc to stop filter noise (from pulse)
			out = LeakDC.ar(out);

			// work out the hp filter freq
			filterResponseHP=Lag.ar((
				(pitch*kybdHP*2)
				+((1-kybdHP)*40)
				+(velocity.linlin(0,1,-65,65)*freqSeqHP)
				+Lag.kr(filterFqHP,0.05)
				+lfoOutFilterHP
				+env2HP
			),filterLag).midicps.clip(0,22000);
	
			// apply the filter
			out = DFM1.ar(out,filterResponseHP, qHP*1.015 ,1,1,0) * qAmpHP;

			// work out the lp filter freq
			filterResponse=Lag.ar((
				(pitch*kybd*2)
				+((1-kybd)*40)
				+(velocity.linlin(0,1,-65,65)*freqSeq)
				+Lag.kr(filterFq,0.05)
				+lfoOutFilterLP
				+env2
			),filterLag).midicps.clip(0,22000);

			// apply the LP filter
			out = MoogLadder.ar(out,filterResponse,(q*1.128)**4);
				
			// apply eq
			out = Select.ar(eq,  [out, BLowShelf.ar(out, 114, 1, 10 )]);
			out = Select.ar(eqHi,[out, BHiShelf.ar (out, 3905, 1, 12)]);
						
			// and the amp env
			out = out * ampEnv * Lag.kr(velocity.linlin(0,1,velAmp,1) * ((q+1) * (qAmp*amp*7.5)));
			
			// phase flips from 1 to -1 for alternate notes, this helps removes phase spikes
			// and easily doubles the dynamic range when multiple notes are played together
			out = out * phase;

			// now send out
			Out.ar(outputChannels,Pan2.ar(out,pan));
			out = out*sendAmp;
			Out.ar(sendChannels,out.dup);
			
			// and back to the client
			SendPeakRMS.kr([Lag.ar(filterResponse),Lag.ar(filterResponseHP),noteID]
				, 20, 0, "/filter", id);
			
		}).send(server);
		
		// and the mono body. only difference is done action on the adsr env

		SynthDef("BumNote_MonoBody", {
			
			arg	outputChannels=0,	gate=1, 			amp=0.1,
				pw=0.5,			velocity=0,		filterFq=0,
				q=0.3,			qAmp=1,			filterEnv=1,
				hpSelectLFO=0,	lpSelectLFO=0,	pitchSelectLFO=0,
				pwmSelectLFO=0,	lfoOutChannel=0,	filterLFO=0,
				osc1Pitch=0,		osc2Pitch=0,		kybd=1,
				pan=0,			filterLag=0,		sendAmp=0,
				sendChannels=4,	pGate=0,			eq=1,
				eqHi=1,			linkOsc=0,		freqSeq=0,
				pulseAmp=0,		sawAmp=0,			noise=0,
				fmAmp=0,			fmMod=0,			velAmp=1,
				filterFqHP=(-45),	qHP=0.3,			qAmpHP=1,
				filterEnvHP=1,	kybdHP=0, 		filterLFOHP=0,
				freqSeqHP=0,		filterGate=0,		pwm=0,
				id=0,			pitch=34,			slide=0,
				modAmp=0,			noteID=0;
				
			var	ampEnv, 			env2,			out,
			 	out1, 			out2, 			out3,
				lfoOutA, 			lfoOutB,			lfoOutFilterLP,
				lfoOutPW,			sawFreq,			pulseFreq,
			    	envctl, 			myEnv,			lfoPitch,
			    	envctl2, 			myEnv2, 			filterResponse,
			    	pEnv, 			pc,
			    	pitch1,			pitch2,			myEnv2HP,
			    	envctl2HP,		env2HP,			lfoOutFilterHP,
			    	outPink,			outFM,			filterResponseHP,
			    	lfoPWM,			in;
			
			// amp envelope
			myEnv  = Env.adsr(0.001, 2  , 0, 0.04,  1, -4);
			envctl = Control.names([\newAmpEnv]).kr( myEnv.asArray );
			ampEnv = EnvGen.ar(envctl,  gate, doneAction:0); // this is the only difference
			
			// filter envelope
			myEnv2 = Env.perc(0.001, 0.8, 1, -4);
			envctl2= Control.names([\newFilterEnv]).kr( myEnv2.asArray );
			env2   = EnvGen.ar(envctl2, filterGate, filterEnv*70*1.5);

			// hp filter envelope
			myEnv2HP = Env.perc(0.001, 0.8, 1, -4);
			envctl2HP= Control.names([\newFilterEnvHP]).kr( myEnv2HP.asArray );
			env2HP   = EnvGen.ar(envctl2HP, filterGate, filterEnvHP*70);

			// pitch & slide with VarLag 
			pitch = VarLag.kr(pitch, slide, Env.shapeNumber(\lin));

			// get lfo's in			
			in = In.ar(lfoOutChannel,2);
			lfoOutA = in[0];
			lfoOutB = in[1];
			
			// the lfo adjusted for each mod
			lfoOutFilterLP = Select.ar(lpSelectLFO,    [lfoOutA, lfoOutB] ) * filterLFO;
			lfoOutFilterHP = Select.ar(hpSelectLFO,    [lfoOutA, lfoOutB] ) * filterLFOHP;
			lfoPitch       = Select.ar(pitchSelectLFO, [lfoOutA, lfoOutB] ) * modAmp;
			lfoPWM         =(Select.ar(pwmSelectLFO,   [lfoOutA, lfoOutB] ) + 1) *pwm*0.25;
						
			// the two pitches of the osc's
			pitch1 = pitch + osc1Pitch + lfoPitch;
			pitch2 = pitch + osc2Pitch + lfoPitch;
			
			// now work out their freq's
			sawFreq = ((pitch1*linkOsc)+(pitch2*(1-linkOsc))).midicps.clip(0,22050);
			pulseFreq = pitch1.midicps.clip(0,22050);
			
			// the oscillators
			out1    = Pulse.ar(pulseFreq,
						(pw-(lfoPWM*(pw*2)) ).clip(0.01,0.5) // the pw
					, pulseAmp.dbamp*0.2);     		         // the pulse
			out2    = Saw.ar(sawFreq,sawAmp.dbamp*0.266);      // the best saw
			outPink = PinkNoise.ar(noise.dbamp*0.25);                          // the pink
			outFM   = SinOsc.ar(sawFreq, SinOsc.ar(pulseFreq,0,fmMod*1.5)).asin// the tri (fm)
						* (fmAmp.dbamp / pi * 0.835);                              
						
			// mix it all together
			out  = (out1 + out2 + outPink + outFM) * (AmpComp.ar(pitch + lfoPitch).clip(0,2))*0.2;
			
			// and leak any dc to stop filter noise (from pulse)
			out = LeakDC.ar(out);

			// work out the hp filter freq
			filterResponseHP=Lag.ar((
				(pitch*kybdHP*2)
				+((1-kybdHP)*40)
				+(velocity.linlin(0,1,-65,65)*freqSeqHP)
				+Lag.kr(filterFqHP,0.05)
				+lfoOutFilterHP
				+env2HP
			),filterLag).midicps.clip(0,22000);
	
			// apply the filter
			out = DFM1.ar(out,filterResponseHP, qHP*1.015 ,1,1,0) * qAmpHP;

			// work out the lp filter freq
			filterResponse=Lag.ar((
				(pitch*kybd*2)
				+((1-kybd)*40)
				+(velocity.linlin(0,1,-65,65)*freqSeq)
				+Lag.kr(filterFq,0.05)
				+lfoOutFilterLP
				+env2
			),filterLag).midicps.clip(0,22000);

			// apply the LP filter
			out = MoogLadder.ar(out,filterResponse,(q*1.128)**4);
				
			// apply eq
			out = Select.ar(eq,  [out, BLowShelf.ar(out, 114, 1, 10 )]);
			out = Select.ar(eqHi,[out, BHiShelf.ar (out, 3905, 1, 12)]);
						
			// and the amp env
			out = out * ampEnv * Lag.kr(velocity.linlin(0,1,velAmp,1) * ((q+1) * (qAmp*amp*7.5)));

			// now send out
			Out.ar(outputChannels,Pan2.ar(out,pan));
			out = out*sendAmp;
			Out.ar(sendChannels,out.dup);
			
			// and back to the client
			SendPeakRMS.kr([Lag.ar(filterResponse),Lag.ar(filterResponseHP),noteID]
				, 20, 0, "/filter", id);
			
		}).send(server);
		
	}
		
	// start the synth
	startDSP{
		lfoBus = Bus.audio(server,2); 
		synth  = Synth.head(groups[\lfo ], "BumNote_LFO",
			[\lfoOutChannel, lfoBus.index, \id, id] );
		node   = synth.nodeID;
		this.switchPoly;
	}
	
	// used to update when switching between between mono and poly
	switchPoly{
		if (this.isMono) {
			voicer.releaseAllNotes(studio.actualLatency); // and any poly notes
			voicer.killAllNotes(studio.actualLatency);
			// start the monoSynth
			monoSynth = Synth.head(groups[\inst], "BumNote_MonoBody",
				[\lfoOutChannel, lfoBus.index, \gate, 0, \id, id, \noteID, 0] );
			monoSynthNode = monoSynth.nodeID; // get the node
			this.updateDSP; // update it 
		}{
			monoSynth.free; // or just stop the monoSynth
			noteIDServer=noteID;
		};
		{gui[\keyboardView].clear}.defer;
	}
	
	// will stop both mono & poly synth notes
	stopDSP{
		synth.free;
		monoSynth.free;
		voicer.releaseAllNotes(studio.actualLatency);
		voicer.killAllNotes(studio.actualLatency);
	}
	
	// get the args for the synth body
	getSynthArgs{
		var val,amp=1;
		#amp,val=this.getQandAmp(p[3]);
		^[
		[\lfoOutChannel, lfoBus.index      ],
		[\q,             val               ],
		[\qAmp,          amp               ],
		[\filterEnv,     p[ 4]             ],
		[\pw,            0.5- p[5]         ],
		[\amp,           p[ 2].dbamp       ],
		[\filterLag,     p[32]**2          ],
		[\pwm,           p[61]             ],
		[\modAmp,        (p[7]**1.75)*4    ],
		[\osc1Pitch,     this.getOscPitch1 ],
		[\osc2Pitch,     this.getOscPitch2 ],
		[\filterLFO,     p[10]             ],
		[\filterFq,      p[ 9]             ],
		[\kybd,          p[18]             ],
		[\pan,           p[26]             ],
		[\sendAmp,       p[31].dbamp       ],
		[\sendChannels,  LNX_AudioDevices.getOutChannelIndex(p[30])],
		[\outputChannels,this.instGroupChannel],
		[\eq,            p[41]             ],
		[\eqHi,          p[34]             ],
		[\linkOsc,       p[43]             ],
		[\filterFqHP,    p[45]             ],
		[\kybdHP,        p[49]             ],
		[\qHP,           p[46]             ],
		[\filterEnvHP,   p[47]             ],
		[\filterLFOHP,   p[48]             ],
		[\freqSeq,       p[33]             ],
		[\freqSeqHP,     p[54]             ],
		[\noise,         p[17]             ],
		[\fmAmp,         p[36]             ],
		[\fmMod,         p[50]             ],
		[\pulseAmp,      p[ 8]             ],
		[\sawAmp,        p[42]             ],
		[\hpSelectLFO,   p[67]             ],
		[\lpSelectLFO,   p[68]             ],
		[\pitchSelectLFO,p[69]             ],
		[\pwmSelectLFO,  p[70]             ],
		[\velAmp,        p[74]             ],
		(this.isMono).if ([\velocity, p[58]],[])
	]}
	
	// get the args for the lfos's
	getLFOArgs{^[
		[\lfoOutChannel, lfoBus.index      ],
		[\lfoWaveA,      p[57]             ],
		[\lfoFreqA,      p[ 6]             ],
		[\lfoSlopeA,     p[56]             ],
		[\lfoSandHA,     p[62]             ],
		[\lfoWaveB,      p[63]             ],
		[\lfoFreqB,      p[64]             ],
		[\lfoSlopeB,     p[65]             ],
		[\lfoSandHB,     p[66]             ],
	]}
	
	
	// update the play synths with the correct arguments
	updateDSP{|oldP,latency|
		var val,amp=1;
		
		var updateNote = oldP.isNil; // a crude test to fix bug with Bum2 Seq vs POP 
		
		oldP=oldP ? p; // previous P if needed
		
		val=p[3];
		#amp,val=this.getQandAmp(val); // filter q and amp adjust for filter q
		
		// lfo stuff
		server.sendBundle(latency, *this.getLFOArgs.collect{|i| [\n_set, node]++i } );
		
		if (this.isMono) {
			// mono body update
			server.sendBundle(latency,
				*this.getSynthArgs.collect{|i| [\n_set, monoSynthNode]++i } );
			// ADSR, LP & HP envs
			server.sendBundle(latency,[\n_setn, monoSynthNode, \newAmpEnv     ] ++
				(this.getAmpEnvList));
			server.sendBundle(latency,[\n_setn, monoSynthNode, \newFilterEnv  ] ++
				(this.getLPEnvList));
			server.sendBundle(latency,[\n_setn, monoSynthNode, \newFilterEnvHP] ++
				(this.getHPEnvList));
		}{
			// ADSR, LP & HP envs
			server.sendBundle(latency,[\n_setn, node, \newAmpEnv     ] ++ (this.getAmpEnvList));
			server.sendBundle(latency,[\n_setn, node, \newFilterEnv  ] ++ (this.getLPEnvList));
			server.sendBundle(latency,[\n_setn, node, \newFilterEnvHP] ++ (this.getHPEnvList));
			
		};

		this.instOutChannel_(LNX_AudioDevices.getOutChannelIndex(p[29]),latency);
			
		if (updateNote) {
			// the pitch
			this.acidNote(models[59].value,0,latency); // pitch, slide, latency
		};

	}
	
	// play a poly note
	polyNoteOn{|note,velocity,latency|
		var voicerNode;
		
		// drop if off, mono or out of range 
		if ((instOnSolo.isOff)or:{this.isMono}or:(note<p[27])or:{note>p[28]}) {^nil};
		
		// get the next next node from the voicer
		voicerNode = voicer.noteOn(note, velocity, latency); // create a voicer node
		
		noteIDServer=noteIDServer+1;          // we have a seperate holder for nodeID
		{noteID=noteID+1;nil}.sched(latency); // to sync with the server
		
		phase=phase.neg; // flip phase to prevent phase spikes when multiple notes are played
		                 // this really helps increase the dynamic range of the poly synth					 
		// send the synth to the server
		server.sendBundle(latency,
			[\s_new, "BumNote_PolyBody", voicerNode.node, 0, groups[\inst].nodeID] ++
			[\pitch , this.getNote(note), \gate, 1, \id, id, \filterGate, p[55], \phase, phase,
			\velocity, velocity, \noteID, noteIDServer] 
			++ (this.getSynthArgs.flat),
			[\n_setn, voicerNode.node, \newAmpEnv     ] ++ (this.getAmpEnvList),
			[\n_setn, voicerNode.node, \newFilterEnv  ] ++ (this.getLPEnvList),
			[\n_setn, voicerNode.node, \newFilterEnvHP] ++ (this.getHPEnvList)
		);
		
		this.setKeyColorOnPoly(note,latency);
	
	}
	
	// anything to do after a cmdPeriod
	cmdPeriod{ noteIDServer=noteID  }
	
	// and stop a poly note
	polyNoteOff{|note,velocity,latency|
		// drop if off, mono or out of range 
		if ((instOnSolo.isOff)or:{this.isMono}or:(note<p[27])or:{note>p[28]}) {^nil};
		// get the voicer to release it
		voicer.releaseNote(note,latency);
		this.setKeyColorOffPoly(note,latency);
	}
	
	// the internal sequenced paramemters, this is going out to the server
			
	// trigger the amplitude envelope
	ampGateOn{|latency|
		server.sendBundle(latency,[\n_set, monoSynthNode, \gate, 1]);
		{gui[\noteOnLamp].on}.defer(latency);
		this.setKeyColorOn(notePlaying,latency);
		ampGateOpen=true;
	}
	
	// close the amplitude envelope
	ampGateOff{|latency|
		// slightly early before next noteOn event from seq
		server.sendBundle(latency,[\n_set, monoSynthNode, \gate, 0]);
		{gui[\noteOnLamp].off}.defer(latency);
		this.setKeyColorOff(latency);
		ampGateOpen=false;
	}
	
	// trigger the filter envelope
	filterGateOn{|latency|
		server.sendBundle(latency,[\n_set, monoSynthNode, \filterGate, 1]);
		{server.sendBundle(latency,[\n_set, monoSynthNode, \filterGate, 0]);nil;}.sched(0.01);
		{
			gui[\filtEnvLamp1].on;
			gui[\filtEnvLamp2].on;
		}.defer(latency?0);
		{
			gui[\filtEnvLamp1].off;
			gui[\filtEnvLamp2].off;
		}.defer((latency?0)+0.2);
	}
	
	
	getNote{|note| ^note-p[12]/(p[11]+1)*12+p[12] }
	
	// this method changes the current note, called by midiIn, step sequencer & guiKeyboard
	acidNote{|note,slide=0,latency|
		var spoNote;
		notePlaying=note;
		if (ampGateOpen) { this.setKeyColorOn(notePlaying,latency) }; 
		models[59].valueAction_(note); 			// store the note in the last note model
		spoNote=this.getNote(note);
		this.setPitch(spoNote,slide,latency);
	}
	
	// sends new note to synth (mono)
	setPitch{|pitch=60,slide=0,latency|
		server.sendBundle(latency,
			[\n_set, monoSynthNode, \pitch , pitch], [\n_set, monoSynthNode, \slide , p[19]*slide]
		);
	}
	
	// set the filter pitch, this is called by the velocity step sequencer
	setVelocity{|velocity|
		server.sendBundle(studio.actualLatency,[\n_set, monoSynthNode, \velocity, velocity]);
	}
	
	// only allows 1 coloured key at a time
	setKeyColorOn{|note,latency|
		if (this.isMono) {
			{
				if (keyPlaying.notNil) { gui[\keyboardView].removeColor(keyPlaying)};
				keyPlaying=note;
				gui[\keyboardView].setColor(keyPlaying,Color(1,0.5,1),1)
			}.defer(latency);
		};
	}
		
	// only allows 1 coloured key at a time
	setKeyColorOff{|latency|
		if (this.isMono) {
			{
				if (keyPlaying.notNil) { gui[\keyboardView].removeColor(keyPlaying)};
			}.defer(latency);
		};
	}
	
	setKeyColorOnPoly{|note,latency|
		if (this.isPoly) {
			{gui[\keyboardView].setColor(note,Color(0.4,0.8,0.8),1);}.defer(latency);
		}
	}
			
	setKeyColorOffPoly{|note,latency|
		if (this.isPoly) {
			{gui[\keyboardView].removeColor(note);}.defer(latency);
		}
	}

	// noteOn or noteOff events //////////////////
	
	// stops all notes in either mono or poly mode
	stopAllNotes{
		voicer.releaseAllNotes(studio.actualLatency+0.02);
		//voicer.killAllNotes(studio.actualLatency); // this doesn't always kill, why?
		notesOn=[];	
		this.ampGateOff(studio.actualLatency+0.02); // slightly later to catch all
		{
			gui[\keyboardView].clear;
			lastNotePlayedBySeq=nil;
		}.defer(studio.actualLatency+0.02);
	}
	
	// need to add a midi out latency option for other //////////////////
	// internal sequencers using internal midi ports
	
	// from midiIn
	noteOn{|note, velocity,latency|
		pianoRoll.noteOn(note,velocity/127);
		this.noteOn2(note, velocity,latency);
	}
	
	// from midiIn
	noteOn2{|note, velocity,latency|
		if ((note<p[27])or:{note>p[28]}) {^nil}; // drop out if out of midi range
		if (instOnSolo.isOn) {
			this.polyNoteOn(note,velocity/127,latency);
			this.monoNoteON(note,velocity/127,latency,false);
			{gui[\keyboardView].setSelectColor(note,Color.blue)}.defer(latency);
		}
	}
	
	// from internal seq
	// this is only note change not a note on event!!!
	midiNote_Seq{|note,slide|
		lastNotePlayedByMIDI = nil; // clear we don't want this used later
		this.acidNote(note,slide,studio.actualLatency);
		lastNotePlayedBySeq=note;
	}
	
	//******//
	
	// from gui keyboard (mono)
	noteOn_Key{|note,velocity|
		if ((note<p[27])or:{note>p[28]}) {^nil}; // drop out if out of midi range
		if (instOnSolo.isOn) {
			this.monoNoteON(note,velocity,nil,false);
		}
	}
		
	// actual monoNoteON method from midi and gui keyboard
	monoNoteON{|note, velocity,latency,setColor=true|
		var x,index;

		var recIndex;


		if ((latency.notNil)and:{latency>0}) {
			recIndex = studio.beat.div(3);
		}{
			recIndex = recordIndex
		};
	
		// if MIDI NoteOn > Env gate
		if (p[35].isTrue) { this.ampGateOn(latency) };
	
		if ((notesOn.isEmpty)and:{p[55].isTrue}) { this.filterGateOn(latency) };
		
		
		if (p[38].isFalse) {
			lastNotePlayedByMIDI = nil;		
			this.acidNote(note,notesOn.size.sign,latency); // this changes the note
		}{
			lastNotePlayedByMIDI = note; // store for later
		};
		
		if ((this.isMono) and: {p[73].isTrue}) {
			models[58].valueAction_(velocity);
			this.setVelocity(velocity);
		};
		
		// keyboard gui stuff
		lastNotePlayedBySeq=note;
		
		// record noteOn
		if ((this.isRecording)and:{notesOn.size==0}) { sequencers[0].recordStep(recIndex,1) };
		
		// add it to notesOn
		if (notesOn.includes(note).not) { notesOn=notesOn.add(note) };
		
		if (this.isRecording) {
			sequencers[3].recordStep(recIndex,note); // record midiNote
		}{
			index=writeIndexModel.value;
			if (isKeyboardWriting) {
					{
						sequencers[3].setStep(index,note);
						if (p[40]==1) {
							index=(index+1).wrap(0,sequencers[3].sP[3]);
						};
						writeIndexModel.value_(index);
					}.defer;
			};
		};
		
		{
			gui[\midiInLamp].on;
		}.defer(latency?0);
		{
			gui[\midiInLamp].off;
		}.defer((latency?0)+0.2);
		
	}

	// from midiIn
	noteOff{|note, velocity,latency|
		pianoRoll.noteOff(note,velocity/127);
		this.noteOff2(note, velocity,latency);
	}	
	// from midiIn
	noteOff2{|note, velocity,latency|
		if ((note<p[27])or:{note>p[28]}) {^nil}; // drop out if out of midi range
		if (instOnSolo.isOn) {
			this.polyNoteOff(note,velocity,latency);
			this.monoNoteOff(note,velocity,latency);
			{gui[\keyboardView].removeSelectColor(note)}.defer(latency);
			
		}
	}
	
	// from gui keyboard (mono)
	noteOff_Key{|note,velocity|
		if (instOnSolo.isOn) {
			this.monoNoteOff(note,velocity,nil);
			// this.polyOff(note,velocity);
		}
	}
	
	monoNoteOff{|note, velocity,latency|
		var x,spoNote;
		
		var recIndex;
		if ((latency.notNil)and:{latency>0}) {
			recIndex = studio.beat.div(3);
		}{
			recIndex = recordIndex
		};
				
		if (notesOn.includes(note)) {
			notesOn.remove(note);
			if (notesOn.notEmpty) {
				note=notesOn.last;
				
				if (p[38].isFalse) {
					this.acidNote(note,1,latency);
				};
				
				if (this.isRecording) {
					sequencers[3].recordStep(recIndex,note); // midi Note
				};
				
			}{
				// if MIDI NoteOn > Env gate
				if (p[35].isTrue) { this.ampGateOff(latency) };
			};
		};
		
		if ((this.isRecording)and:{notesOn.size==0}) {
			sequencers[1].recordStep(recIndex,1); // note off
		};
	
	}
	
	/// midi Clock ////////////////////////////////////////
	
	//clockIn is the clock pulse, with the current song pointer in beats
	clockIn{|beat,latency|	
		sequencers.do{|seq,n|
			if ((((n>=3)and:{n<=5})and:{p[60].isTrue}).not) {
				seq.clockIn(beat,latency);
			};			
		};
		server.sendBundle(latency,[\n_set, node, \clockIn, beat]);
		if (this.isRecording) {
			{recordIndex=beat;nil}.sched(latency);
		};
	}	
	
	// midi clock in (this is at MIDIClock rate)
	clockIn3 {|beat,absTime,latency| pianoRoll.clockIn3(beat,absTime,latency) }
	
	// this clock is playing when a song is not playing, used in lfos 
	clockOff{|beat,latency|
		server.sendBundle(latency,[\n_set, node, \clockIn, beat]);
	} 
	
	clockPlay{ }		//play and stop are called from both the internal and extrnal clock
	clockStop{
		pianoRoll.clockStop(studio.actualLatency);
		voicer.killAllNotes(studio.actualLatency);
		sequencers.do(_.clockStop(studio.actualLatency));
		this.stopAllNotes;
	}	
	clockPause{
		pianoRoll.clockPause(studio.actualLatency);
		voicer.releaseAllNotes(studio.actualLatency);
		sequencers.do(_.clockPause(studio.actualLatency));
		this.stopAllNotes;
	}		// pause only comes the internal clock

	// when pop resets beat
	clockReset{|latency|
		pianoRoll.clockPause(studio.actualLatency);
		voicer.releaseAllNotes(studio.actualLatency);
		sequencers.do(_.clockPause(studio.actualLatency));
		
		
		voicer.releaseAllNotes(studio.actualLatency);
		notesOn=[];	
		this.ampGateOff(studio.actualLatency-0.002); // slightly early
		{
			gui[\keyboardView].clear;
			lastNotePlayedBySeq=nil;
		}.defer(studio.actualLatency);
	}

	/// i have down timed the clock by 3, so this gives 32 beats in a 4x4 bar
	/// this is all the resoultion i need at the moment but could chnage in the future
	
	// this is called by the studio for the zeroSL auto map midi controller
	autoMap{|num,val|
		var vPot;
		vPot=(val>64).if(64-val,val);
	}

} // end ////////////////////////////////////
