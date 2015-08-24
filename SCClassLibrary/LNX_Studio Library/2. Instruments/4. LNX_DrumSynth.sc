
// a drum synthesis instrument

LNX_DrumSynth : LNX_InstrumentTemplate {
 	
	classvar <drumNames, <shortNames;

	var <seq,  <sP;
	var <steps, <channels, <defaultSteps=32, <defaultChannels=5;
	var <seqPresetMemory, <sPPresetMemory;
	var <midiNotes, <bangsOn, <bangNumber, <synthsOn;
	var <seqModels,	<seqMVCViews;
	var <spModels, 	<spMVCViews,	<posModels;
	
	*initClass {
		drumNames=#["Bass","Snare","Clap","Tom","Hat"];
		shortNames=#["BD","SN","CP","TM","HH"];
	}

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id;
		^super.new(server,studio,instNo,bounds,open,id)
	}

	*studioName {^"Drum Synth"}
	*sortOrder{^1}
	isInstrument{^true}
	canBeSequenced{^true}
	isMixerInstrument{^true}
	hasLevelsOut{^true}
	mixerColor{^Color(0.75,1,0.75,0.4)} // colour in mixer
	
	// an immutable list of methods available to the network
	interface{^#[ \netSeq, \netChannelItem, \netSteps, \netRuler]}
	
	header { 
		// define your document header details
		instrumentHeaderType="SC DrumSynth Doc";
		version="v1.3";
	}
	
	initModel {

		var template;
		
		template=[
			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me| this.onOffAlt(me.value) }],
			
			// 2.amp
			[ \db6, midiControl, 2, "Amp",
				(mouseDownAction_:{hack[\fadeTask].stop}),
				{|me,val,latency,send| this.setPVP(2,val,latency,send) }],
			
			0,    // 3.tab position (this doesn't need a model
			
		// bass drum
			
			// 4.note (10-59) 
			[30, [10,59,\lin,1], midiControl, 4, "BD Note",
				(\label_:"Note", \numberFunc_:\note),
				{|me,val,latency,send| this.setPVP(4,val,latency,send) }],
			// 5.dur  
			[1.6, [0.5,3], midiControl, 5, "BD Dur", (\label_:"Dur"),
				{|me,val,latency,send| this.setPVP(5,val,latency,send) }],
			// 6.attack time
			[0.0785, [0.03,1], midiControl, 6, "BD R1", (\label_:"R1"),
				{|me,val,latency,send| this.setPVP(6,val,latency,send) }],
			// 7.attack amount
			[0.72, [0,1], midiControl, 7, "BD Env", (\label_:"Env"),
				{|me,val,latency,send| this.setPVP(7,val,latency,send) }],
			// 8. 2nd attack rate (0,1)
			[0.135, [0,1], midiControl, 8, "R2", (\label_:"R2"),
				{|me,val,latency,send| this.setPVP(8,val,latency,send) }],
			// 9. filter scale (0,3)
			[1.3, [0,10], midiControl, 9, "BD Filter", (\label_:"Filter"),
				{|me,val,latency,send| this.setPVP(9,val,latency,send) }],
			// 10. q (0-1)
			[0, [0,1], midiControl, 10, "BD Q", (\label_:"Q"),
				{|me,val,latency,send| this.setPVP(10,val,latency,send) }],
		
		// 11.bp scale (-1,1)
			[\bipolar, midiControl, 11, "Probability", (\label_:"Probability", \zeroValue_:0),
				{|me,val,latency,send| this.setPVP(11,val,latency,send)}],
			
		// & their ranges
			
			// 12.note range
			[\unipolar, midiControl, 12, "BD Note Range",
				{|me,val,latency,send| this.setPVP(12,val,latency,send) }],
			// 13.dur
			[\unipolar, midiControl, 13, "BD Dur Range",
				{|me,val,latency,send| this.setPVP(13,val,latency,send) }],
			// 14.attack time
			[\unipolar, midiControl, 14, "BD R1 Range",
				{|me,val,latency,send| this.setPVP(14,val,latency,send) }],
			// 15.attack amount
			[\unipolar, midiControl, 15, "BD Env Range",
				{|me,val,latency,send| this.setPVP(15,val,latency,send) }],
			// 16. 2nd attack rate
			[\unipolar, midiControl, 16, "BD R2 Range",
				{|me,val,latency,send| this.setPVP(16,val,latency,send) }],
			// 17. filter scale
			[\unipolar, midiControl, 17, "BD Filter Range",
				{|me,val,latency,send| this.setPVP(17,val,latency,send) }],
			// 18. q
			[\unipolar, midiControl, 18, "BD Q Range",
				{|me,val,latency,send| this.setPVP(18,val,latency,send) }],
			// 19.noise
			[\unipolar, midiControl, 19, "BD Noise",  (\label_:"Noise"),
				{|me,val,latency,send| this.setPVP(19,val,latency,send) }],
			// 20.noise range
			[\unipolar, midiControl, 20, "BD Noise Range",
				{|me,val,latency,send| this.setPVP(20,val,latency,send) }],
			
		/// top section

			// 21.spo
			[12, [1,24,\linear,1], midiControl, 21, "Steps per octave",
				( \label_:"SPO", \numberFunc_:'int'),
				{|me,val,latency,send| this.setPVP(21,val,latency,send) }],
					
			// 22.base note
			[\MIDInote, midiControl, 22, "Root", (\label_:"Root"),
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
						me.lazyValueAction_(note,nil,true)}}),
				{|me,val,latency,send| this.setPVP(22,val,latency,send) }],
			
			// 23.MIDI low 
			[0, \MIDInote, midiControl, 23, "MIDI Low", (\label_:"Low"),
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
						me.lazyValueAction_(note,nil,true)} // enter value via keyboard
				}),
				{|me,val,latency,send|
					var value=val.clip(0,p[24]);
					this.setPVP(23,value,latency,send);
					if (value!=val) {{me.value_(value)}.defer(0.05)};
				}],
			
			// 24.MIDI high
			[127, \MIDInote, midiControl, 24, "MIDI High", (\label_:"High"),
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
						me.lazyValueAction_(note,nil,true);} // enter value via keyboard
				}),
				{|me,val,latency,send|
					var value=val.clip(p[23],127);
					this.setPVP(24,value,latency,send);
					if (value!=val) {{me.value_(value)}.defer(0.05)};
				}],
				
			// 25.output channels
			[0, \LNX_audiobus, midiControl, 25, "Output channels",
				(\numberFunc_:\LNX_audiobus,\showNumberBox_:false,
					\items_:LNX_AudioDevices.outputChannelList),
				{|me,val,latency,send|
					this.instOutChannel_(val*2);
					this.setPVPModel(25,val,0,true);
				}],
			
			// 26.show sequencer
			[1, \switch, (\strings_:"Seq"), midiControl, 26, "Show/Hide Seq",
			{|me,val,latency,send|  p[26]=val; {this.arrangeWindow}.defer }],// not networked
			
		/// for all drums				
		// 27-31.out channel (defined below)
		0,0,0,0,0, 		
		// 32-36.pan (defined below)
		0,0,0,0,0,
		// 37-41.send channel (defined below)
		0,0,0,0,0,
		// 42-46.send (defined below)
		0,0,0,0,0,		
		// 47-51.send range (defined below)
		0,0,0,0,0,
		// 52-56. bp for each drum (defined below)
		0,0,0,0,0,
		// 57-61.velocity (defined below)
		0,0,0,0,0,
		// 62-66. not used					
		1,1,1,1,1, 
			
		// snare
			
			// 67.note
			[44, [30,70,\lin,1], midiControl, 67, "SN Note",
				(\label_:"Note", \numberFunc_:\note),
				{|me,val,latency,send| this.setPVP(67,val,latency,send) }],
			// 68. note range
			[\unipolar, midiControl, 68, "SN Note Range",
				{|me,val,latency,send| this.setPVP(68,val,latency,send) }],
			// 69. duration
			[1, [0.3,2], midiControl, 69, "SN Dur", (\label_:"Dur"),
				{|me,val,latency,send| this.setPVP(69,val,latency,send) }],
			// 70. duration range
			[\unipolar, midiControl, 70, "SN Dur Range",
				{|me,val,latency,send| this.setPVP(70,val,latency,send) }],
			// 71. noise mix
			[0.5, \unipolar, midiControl, 71, "SN Mix", (\label_:"Mix"),
				{|me,val,latency,send| this.setPVP(71,val,latency,send) }],
			// 72. noiseFiltQ=1
			[0.99, [0.01,0.99], midiControl, 72, "SN Q", (\label_:"Q"),
				{|me,val,latency,send| this.setPVP(72,val,latency,send) }],
			// 73. noiseFilt2=1
			[0.9, \unipolar, midiControl, 73, "SN Filter", (\label_:"Filter"),
				{|me,val,latency,send| this.setPVP(73,val,latency,send) }],
			// 74. Env
			[1, [0,1.3], midiControl, 74, "SN Env", (\label_:"Env"),
				{|me,val,latency,send| this.setPVP(74,val,latency,send) }],
			// 75. noiseDuration
			[1, \unipolar, midiControl, 75, "SN Noise Dur", (\label_:"Dur"),
				{|me,val,latency,send| this.setPVP(75,val,latency,send) }],
			// 76. noise mix range
			[\unipolar, midiControl, 76, "SN Mix Range",
				{|me,val,latency,send| this.setPVP(76,val,latency,send) }],
			// 77. noiseFiltQ=1 range
			[\unipolar, midiControl, 77, "SN Q Range",
				{|me,val,latency,send| this.setPVP(77,val,latency,send) }],
			// 78. noiseFilt2=1 range
			[\unipolar, midiControl, 78, "SN Filter Range",
				{|me,val,latency,send| this.setPVP(78,val,latency,send) }],
			// 79. Env range
			[\unipolar, midiControl, 79, "SN Env Range",
				{|me,val,latency,send| this.setPVP(79,val,latency,send) }],
			// 80. noiseDuration range
			[\unipolar, midiControl, 80, "SN Noise Dur Range",
				{|me,val,latency,send| this.setPVP(80,val,latency,send) }],
			
		// clap
			
			// 81. duration
			[1, [0.3,2], midiControl, 81, "CP Dur", (\label_:"Dur"),
				{|me,val,latency,send| this.setPVP(81,val,latency,send) }],
			// 82. duration range
			[\unipolar, midiControl, 82, "CP Dur Range",
				{|me,val,latency,send| this.setPVP(82,val,latency,send) }],
			// 83. Q
			[1.5, [0.1,1.5], midiControl, 83, "CP Q", (\label_:"Q"),
				{|me,val,latency,send| this.setPVP(83,val,latency,send) }],
			// 84. Q range
			[\unipolar, midiControl, 84, "CP Q Range",
				{|me,val,latency,send| this.setPVP(84,val,latency,send) }],
			// 85. Filt Fq
			[2.9, [0.01,10], midiControl, 85, "CP Filter Fq", (\label_:"Filter"),
				{|me,val,latency,send| this.setPVP(85,val,latency,send) }],
			// 86. Filt Fq range
			[\unipolar, midiControl, 86, "CP Filt Fq Rng",
				{|me,val,latency,send| this.setPVP(86,val,latency,send) }],
			// 87. rand
			[\unipolar, midiControl, 87, "CP Rand", (\label_:"Rand"),
				{|me,val,latency,send| this.setPVP(87,val,latency,send) }],
			// 88. rand range
			[\unipolar, midiControl, 88, "CP Rand Range",
				{|me,val,latency,send| this.setPVP(88,val,latency,send) }],
			
		// tom
			
			// 89&90 note & note range
			[40, [30,80,\lin,1], midiControl, 89, "TM Note",
				(\label_:"Note", \numberFunc_:\note),
				{|me,val,latency,send| this.setPVP(89,val,latency,send) }],
			
			[\unipolar, midiControl, 90, "TM Note Range",
				{|me,val,latency,send| this.setPVP(90,val,latency,send) }],
			
			// 91-92 dur & dur range
			[0.7, [0.1,2], midiControl, 91, "TM Dur", (\label_:"Dur"),
				{|me,val,latency,send| this.setPVP(91,val,latency,send) }],
			
			[\unipolar, midiControl, 92, "TM Dur Range",
				{|me,val,latency,send| this.setPVP(92,val,latency,send) }],
				
			// 93+94 timbre & timbre range
			[0.5, [0,10], midiControl, 93, "TM FM", (\label_:"FM"),
				{|me,val,latency,send| this.setPVP(93,val,latency,send) }],
			
			[\unipolar, midiControl, 94, "TM FM Range",
				{|me,val,latency,send| this.setPVP(94,val,latency,send) }],
				
			// 95+96 carrier adjust & range
			[0, [-4,4], midiControl, 95, "TM FM Adj", (\label_:"Adj",\zeroValue_:0),
				{|me,val,latency,send| this.setPVP(95,val,latency,send) }],
			
			[\unipolar, midiControl, 96, "TM FM Adj Range",
				{|me,val,latency,send| this.setPVP(96,val,latency,send) }],
				
			// 97+98 index slope & range
			[0.3, \unipolar, midiControl, 97, "TM Slope", (\label_:"Slope"),
				{|me,val,latency,send| this.setPVP(97,val,latency,send) }],
			
			[\unipolar, midiControl, 98, "TM Slope Range",
				{|me,val,latency,send| this.setPVP(98,val,latency,send) }],
				
			// 99+100 r1 dur range
			[0.5, [0.2,2], midiControl, 99, "TM R1", (\label_:"R1"),
				{|me,val,latency,send| this.setPVP(99,val,latency,send) }],
			
			[\unipolar, midiControl, 100, "TM R1 Range",
				{|me,val,latency,send| this.setPVP(100,val,latency,send) }],
				
			// 101+102 attackAmount
			[0.5, \unipolar, midiControl, 101, "TM Env", (\label_:"Env"),
				{|me,val,latency,send| this.setPVP(101,val,latency,send) }],
			
			[\unipolar, midiControl, 102, "TM Env Range",
				{|me,val,latency,send| this.setPVP(102,val,latency,send) }],
				
			// 103+104 stick
			[0.2, [0,2], midiControl, 103, "TM Stick", (\label_:"Stick"),
				{|me,val,latency,send| this.setPVP(103,val,latency,send) }],
			
			[\unipolar, midiControl, 104, "TM Stick Range",
				{|me,val,latency,send| this.setPVP(104,val,latency,send) }],
			
		// hat
			
			// 105-106 dur
			[0.7, [0.1,4], midiControl, 105, "HH Dur", (\label_:"Dur"),
				{|me,val,latency,send| this.setPVP(105,val,latency,send) }],
			
			[\unipolar, midiControl, 106, "HH Dur Range",
				{|me,val,latency,send| this.setPVP(106,val,latency,send) }],
			
			// 107-108 tone
			[0, [-20,10], midiControl, 107, "HH Tone", (\label_:"Tone"),
				{|me,val,latency,send| this.setPVP(107,val,latency,send) }],
			
			[\unipolar, midiControl, 108, "HH Tone Range",
				{|me,val,latency,send| this.setPVP(108,val,latency,send) }],
			
			0.5 , 0, // 109-10  : mix   // not used
			
			// 111-112 lp
			[1, [0.5,1], midiControl, 111, "HH LP", (\label_:"LP"),
				{|me,val,latency,send| this.setPVP(111,val,latency,send) }],
			
			[\unipolar, midiControl, 112, "HH LP Range",
				{|me,val,latency,send| this.setPVP(112,val,latency,send) }],
			
			// 113-114 lp q
			[0.4, [0,0.8], midiControl, 113, "HH LP Q", (\label_:"Q"),
				{|me,val,latency,send| this.setPVP(113,val,latency,send) }],
			
			[\unipolar, midiControl, 114, "HH LP Q Range",
				{|me,val,latency,send| this.setPVP(114,val,latency,send) }],
			
			// 115-116 hp
			[1, [0.5,1], midiControl, 115, "HH HP", (\label_:"HP"),
				{|me,val,latency,send| this.setPVP(115,val,latency,send) }],
			
			[\unipolar, midiControl, 116, "HH HP Range",
				{|me,val,latency,send| this.setPVP(116,val,latency,send) }],
			
		// inst
			
			// 117 duration of all instruments
			[1,[0.01,1.99], midiControl, 117, "Duration", (\label_:"Duration", \zeroValue_:1),
				{|me,val,latency,send| this.setPVP(117,val,latency,send)}],
			
			// 118 not used
			[\switch, midiControl, 118, "Not used", 
				{|me,val,latency,send|}],
			
			// 119 keep sequencer
			[\switch, midiControl, 119, "Lock Seq", (\strings_:"Lock Seq"), 
				{|me,val,latency,send| this.setPVP(119,val,latency,send)}],
					
		// 120-124 On/Off for each drum (defined below)
		0,0,0,0,0,
		// 125-129 New Amp for each drum (defined below)
		0,0,0,0,0,
			
			// 130 mater pan		
			[\bipolar, midiControl, 130, "Pan", (\label_:"Pan", zeroValue_:0),
				{|me,val,latency,send| this.setPVP(130,val,latency,send)}],
				
			// 131. peak level
			[0.7, \unipolar,  midiControl, 131, "Peak Level",
					{|me,val,latency,send| this.setPVP(131,val,latency,send) }],
			
			// 132. master send channel
			[-1, \audioOut,  midiControl, 132, "Master Send Channel",
				(\items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send| this.setPVH(132,val,latency,send) }],			
			// 133. master send amp
			[-inf, \db2,  midiControl, 133, "Maseter Send Amp", (label_:"Send"),
				{|me,val,latency,send|
					this.setPVPModel(133,val,latency,send);
				}],	
			
 		];
 			
 		defaultChannels.do{|y|
 							
			// 27-31.out channel
			template[27+y]=[0, \LNX_audiobusM, midiControl, 27+y, shortNames[y]+"out channel",
				(\numberFunc_:\LNX_audiobusM,\showNumberBox_:false,
					\items_:(["Master"]++(LNX_AudioDevices.outputChannelList))),
				{|me,val,latency,send| this.setPVH(27+y,val,latency,send)}];
				
			// 32-36.pan
			template[32+y]=[\bipolar, midiControl, 32+y, shortNames[y]+"Pan",
				(\label_:"Pan", zeroValue_:0),
				{|me,val,latency,send| this.setPVP(32+y,val,latency,send)}];
 		
 			// 37-41.send channel
			template[37+y]=[2, \LNX_audiobus, midiControl, 37+y, shortNames[y]+"send channel",
				(\numberFunc_:\LNX_audiobus,\showNumberBox_:false,
					\items_:LNX_AudioDevices.outputChannelList),
				{|me,val,latency,send| this.setPVH(37+y,val,latency,send)}];
 		
 			// 42-46.send
			template[42+y]=[-inf, \db6, midiControl, 42+y, shortNames[y]+"Send", (\label_:"Send"),
				{|me,val,latency,send| this.setPVP(42+y,val,latency,send)}];
 		
 			// 47-51.send range
			template[47+y]=[\unipolar, midiControl, 47+y, shortNames[y]+"Send Range",
				{|me,val,latency,send| this.setPVP(47+y,val,latency,send) }];
				
			// 52-56. bp for each drum
			template[52+y]=[\switch, midiControl, 52+y, shortNames[y]+"bp", (\strings_:"P"),
				{|me,val,latency,send| this.setPVH(52+y,val,latency,send)}];
 		
 			// 57-61.velocity
			template[57+y]=[\unipolar, midiControl, 57+y, shortNames[y]+"Velocity",
				(\label_:"Vel", zeroValue_:1),
				{|me,val,latency,send| this.setPVP(57+y,val,latency,send)}];
 		
 			// 120-124 On/Off for each drum
			template[120+y]=[ 1, \switch, midiControl, 120+y, shortNames[y]+"On/Off",
				(\strings_:drumNames[y]),
				{|me,val,latency,send| this.setPVH(120+y,val,latency,send)}];
				
			// 125-129 New Amp for each drum	
 			template[125+y]=
 			
 			[ \db6, midiControl, 125+y, shortNames[y]+"Amp",
				{|me,val,latency,send| this.setPVP(125+y,val,latency,send)}];
				
 		};
 	
 		#models,defaults = template.generateAllModels;
 		
 		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,21,22,26,119];
		randomExclusion=[0,1,2,21,22,23,24,25,26,27,28,29,30,31,37,38,
				39,40,41,118,119,120,121,122,123,124,125,126,127,128,129,131];
		autoExclusion=[26];

	}
	
	// peak / target volume model
	peakModel{^models[131]}

	// return the volume model
	volumeModel{^models[2] }
	outChModel{^models[25]}
	
	soloModel{^models[0]}
	onOffModel{^models[1]}
	panModel{^models[130]}
	
	sendChModel{^models[132]}
	sendAmpModel{^models[133]}
		
	// your own vars
	iInitVars{
		channels=defaultChannels;
		steps=defaultSteps ! channels;
		seq={0 ! defaultSteps} ! channels;
		
		midiNotes=[];
		channels.do({|i| midiNotes=midiNotes.add(60+i)});
		
		posModels = {0.asModel} ! channels;
		seqMVCViews = {LNX_EmptyGUIShell ! defaultSteps} ! channels;
		seqModels = {nil ! defaultSteps} ! channels;
		
		channels.do{|y|
			// seq
			defaultSteps.do{|x|
				seqModels[y][x]=[\unipolar,
					midiControl,(y+1)*200+10+x,drumNames[y]++" Step:"++((x+1).asString),
					{|me,val,latency,send| this.setSeq(y,x,val,latency,send) }].asModel;
			};
		
		};
		
		// new MVC seq controls
		
		spModels=Array.newClear(channels);
		sP=Array.newClear(channels);
		
		channels.do{|y|
			var mods,sPs;
			#mods,sPs= [
				1,			// 
				0.5,			// 
				36,			// 
				
				// 3. no of steps
				[ 32, [1,defaultSteps,\lin,1], midiControl, (y+1)*200+3, "Steps ch:"++(y+1),
					(label_:"Steps"),
					{|me,val,latency,send|
						this.setSteps(y,val,latency,send);
					}],	         
				
				// 4. ruler
				[ 4, [2,16,\lin,1], midiControl, (y+1)*200+4, "Ruler ch:"++(y+1),
					(label_:"Ruler"),
					{|me,val,latency,send|
						this.setRuler(y,val,latency,send);
					}],	 
				0,            // 
				// 6. speed divider
				[ 2, [1,32,\lin,1], midiControl, (y+1)*200+6, "Speed ch:"++(y+1),
					(label_:"Speed"),
					{|me,val,latency,send|
						this.setChannelItem(y,6,val,latency,send);
					}],	 
			].generateAllModels;
			spModels[y]=mods;
			sP[y]=sPs;
		};
		
		seqPresetMemory=[];
		sPPresetMemory=[];
		
		// synth stuff
		
		synthsOn = nil ! channels;
		
		bangsOn=[0] ! 128; // one for each midi note 
		bangNumber= [0] ! 128; // (this is no. times played for note removal)
		
	
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	
	// any post midiInit stuff
	iInitMIDI{ midi.putLoadList(LNX_MIDIPatch.nextUnusedIn++[1, 0 ]) }
	
	// anything else that needs doing after a server reboot; 
	iServerReboot{ synthsOn = nil ! channels; }
		
	// for your own clear, used to when loading studio preset 
	iClear{}
	
	iFreeAutomation{
		seqModels.freeAutomation;
		spModels.do{|c| c.do(_.freeAutomation)};
	}
	
	// for freeing anything when closing
	iFree{
		seqModels.free;
		spModels.do{|c| c.do(_.free)};
	}
	
	///////////////////////////
	
	// get the current state as a list
	iGetPresetList{
		var l;
		l=[channels,sP[0].size];
		channels.do{|i|
			l=l++sP[i];
			l=l++seq[i];
		};
		^l
	}
	
	// add a statelist to the presets
	iAddPresetList{|l|
		var channels,sPSize;
		#channels,sPSize=l.popNI(2);
		sPPresetMemory = sPPresetMemory.add(0!channels);
		seqPresetMemory=seqPresetMemory.add(0!channels);
		channels.do{|j|
			sPPresetMemory [ sPPresetMemory.size-1][j]=l.popNF(sPSize);
			seqPresetMemory[seqPresetMemory.size-1][j]=l.popNF(defaultSteps);
		};
	}
	
	// save a state list over a current preset
	iSavePresetList{|i,l|
		var channels,sPSize;
		#channels,sPSize=l.popNI(2);
		channels.do{|j|
			sPPresetMemory [i][j]=l.popNF(sPSize);
			seqPresetMemory[i][j]=l.popNF(defaultSteps);
		};
	}

	// for your own load preset
	iLoadPreset{|i,newP,latency|
		if (p[119]==0) {
			seq=seqPresetMemory[i].deepCopy;
			sP=sPPresetMemory[i].deepCopy;
			{this.iUpdateGUI}.defer;
		};
	}
	
	 // for your own remove preset
	iRemovePreset{|i| seqPresetMemory.removeAt(i); sPPresetMemory.removeAt(i);}
	
	// for your own removal of all presets
	iRemoveAllPresets{ seqPresetMemory=[]; sPPresetMemory=[] }
	
	// clear the sequencer
	clearSequencer{
		seqModels.do{|seq,i| seq.do{|models| models.lazyValueAction_(0,nil,true) } };
	}
	
	///////////////////////////
	
	// for your own saving
	iGetSaveList{
		var l;
		l=[channels,sP[0].size];
		channels.do{|i|
			l=l++sP[i];
			l=l++seq[i];
		};
		seqPresetMemory.size.do{|i|
			channels.do{|j|
				l=l++((sPPresetMemory[i][j]));
				l=l++((seqPresetMemory[i][j]));
			}
		};
		^l
	}
		
	// for your own loading
	iPutLoadList{|l,noPre,instrumentLoadVersion,templateLoadVersion|
		var sPSize;
		l.pop; // to use later when you can change no of channels
		sPSize=l.pop.asInt;
		channels.do{|i|
			sP[i]=l.popNF(sPSize);
			seq[i]=l.popNF(defaultSteps)
		};
		sPPresetMemory=[]!channels!noPre;
		seqPresetMemory=[]!channels!noPre;
		if (instrumentLoadVersion>=1.2) {
			noPre.do{|i|
				channels.do{|j|
					sPPresetMemory[i][j]=l.popNF(sPSize);
					seqPresetMemory[i][j]=l.popNF(defaultSteps);
				};
			};
		}{
			noPre.do{|i|
				channels.do{|j|
					sPPresetMemory[i][j]=sP[j].copy;
					seqPresetMemory[i][j]=seq[j].copy;
				};
			};

		};
		
		{
			// update amp
			if (instrumentLoadVersion<1.3) {
				models[2].valueAction_(p[2].ampdb);
				5.do{|i|
					models[125+i].valueAction_(p[125+i].ampdb);
					models[42+i].valueAction_(p[42+i].ampdb);
				};
				presetMemory.size.do{|i|
					presetMemory[i][2] = presetMemory[i][2].ampdb;
					5.do{|j|
						presetMemory[i][125+j] = presetMemory[i][125+j].ampdb;
						presetMemory[i][42+j]  = presetMemory[i][42+j].ampdb;
					};
				};
			}; 
		}.defer(0.1);
		
		^l 
	}
	
	// for your own loading inst update
	iUpdateGUI{|p|
		var val,enabled, oldRulerValue;
	
		channels.do{|y|
			val=sP[y][3];
			if(spModels[y][3].value!=val) {
				seqModels[y].do({|i,x| 
					enabled=(x<val).if(true,false);
					if (i.enabled!=enabled) {
						i.enabled_(enabled)
					};
				});
			};
			oldRulerValue=spModels[y][4].value;
			
			seqModels[y].do{|i,j| if (seq[y][j]!=i.value) {i.lazyValue_(seq[y][j],false)}};
			spModels[ y].do{|i,j| if (sP[y][j]!=i.value) {i.lazyValue_(sP[y][j],false)}};
			val=sP[y][4];
			if (oldRulerValue != sP[y][4]) { this.changeSeqColours(y,val) };
		}
	
	}
	
	// anything that needs doing after a load
	iPostLoad{
		this.arrangeWindow;
	}
	
	//// Networking ////////////////////////////////////
	
	setSeq{|y,x,value,latency,send=true|
		if (seq[y][x]!=value) {
			seq[y][x]=value;
			if (send) { api.sendVP((id++"ss"++y++"_"++x).asSymbol,'netSeq',y,x,value) };
		};
	}
	
	netSeq{|y,x,value|
		seq[y][x]=value;
		seqModels[y][x].lazyValue_(value,false);
	}
		
	setChannelItem{|y,x,value,latency,send=true|
		if (value!=sP[y][x]) {
			sP[y][x]=value;
			if (send) {
				api.sendVP((id++"sc"++y++"_"++x).asSymbol,'netChannelItem',y,x,value);
			};
		};
	}
	
	netChannelItem{|y,x,value|
		sP[y][x]=value;
		spModels[y][x].lazyValue_(value,false);
	}

	setSteps{|y,value,latency,send=true|
		var enabled;
		if (sP[y][3]!=value) {
			sP[y][3]=value;
			if (send) { api.sendVP((id++"sS"++y).asSymbol,'netSteps',y,value) };			{
				seqModels[y].do({|i,x|
					enabled=(x<value).if(true,false);
					if (i.enabled!=enabled) {
						i.enabled_(enabled)
					};
				});
			}.defer;
		}
	}
	
	netSteps{|y,value|
		var enabled;
		sP[y][3]=value;
		spModels[y][3].lazyValue_(value,false);
		{
			seqModels[y].do({|i,x|
				enabled=(x<value).if(true,false);
				if (i.enabled!=enabled) {
					i.enabled_(enabled)
				};
			});
		}.defer;
	}
	
	setRuler{|y,value,latency,send=true|
		if (sP[y][4]!=value) {
			sP[y][4]=value;
			if (send) { api.sendVP((id++"sr"++y).asSymbol,'netRuler',y,value) };
			{ this.changeSeqColours(y,value) }.defer;
		};
	}
	
	netRuler{|y,value|
		sP[y][4]=value;
		spModels[y][4].lazyValue_(value,false);
		{this.changeSeqColours(y,value) }.defer;
	}

	//// uGens & midi players //////////////////////////
	
	noteOn	{|note, vel, latency|
		if ((note<p[23])or:{note>p[24]}) {^nil}; // drop out if out of midi range
		this.bang((note-36).wrap(0,4),vel/127,latency);
	} // noteOn
	
	/// midi Clock ////////////////////////////////////////
	
	updateDSP{|oldP,latency|
	
		 this.instOutChannel_( p[25]*2, latency)
		 
	} // also used for server reboot
	
	//clockIn is the clock pulse, with the current song pointer in beats
	clockIn   {|beat|
		channels.do({|y|
			var vel,pos,note,speed,dur;
			speed=sP[y][6];
			if ((beat%speed)==0) {
				pos=((beat/speed).asInt)%(sP[y][3]);
				vel=seq[y][pos];
				note=sP[y][2];
				if (vel>0) {
					this.bang(y,vel,studio.actualLatency);
				};
				{posModels[y].lazyValue_(pos,false)}.defer(studio.actualLatency);
			};
		});
	}	
	
	mapVelocityToRange{|vel,val,range,low,hi|
		var rng=(hi-low)*range;
		var l=(val-rng).clip(low,hi);
		var h=(val+rng).clip(low,hi);
		^vel.map(0,1,l,h);
	}
	
	// band it boy!!
	bang{|note,vel,latency|
	
		var noteOffNumber, drum, node,	
		    attackRate, attackAmount, attackRate2,
		    filterScale, fq, q, noise, npoNote, amp, dur, pan, outputChannels,
		    sendChannels, send, nMix, nDur, env, rnd, timbre, freq, cFreq, slope, slopeDur,
		    stick, mix, lp, hp, mDur;
		    
		var masterSendChannels, masterSendAmp;

		if (instOnSolo.onOff==0) {^nil}; // drop out if onSolo is off

		drum=note.asInt;
			
		if (p[120+drum]==0) {^nil}; // drop out if off
		if (p[52+drum]==1) { if (1.0.rand>(vel+p[11])) {^nil} }; // beat probality drop out
		
		amp=((vel*(1-p[57+drum]))+p[57+drum])*(p[2].dbamp)
			*(p[125+drum].dbamp);
		//pan=p[32+drum];
		
		if (p[130]>=0) {
			pan = p[32+drum].map(-1,1,p[130]*2-1,1)
		}{
			pan = p[32+drum].map(-1,1,-1,1+(p[130]*2))
		};
			
		// select out channel
		outputChannels=p[27+drum]; // from each drum
		
		if (outputChannels==0) {
			outputChannels= this.instGroupChannel; // select group if master
		}{
			outputChannels=(outputChannels-1)*2;
			if (outputChannels==(p[25]*2)) { outputChannels= this.instGroupChannel };
			// select group if the same as master
		}; 
		
		sendChannels=p[37+drum]*2;
		send=vel.mapVelocityToRange(p[42+drum].dbamp,p[47+drum],0,1);
		dur=p[117]**2;
		
		
		masterSendAmp = p[133].dbamp;
		masterSendChannels = LNX_AudioDevices.getOutChannelIndex(p[132]);
		
		
		switch (drum)
		
				// bass
				{0} {
	
					note        =vel.mapVelocityToRange(p[4] ,p[12],10  ,59).asInt;
					npoNote     =note-p[22]/(p[21])*12+p[22];
					mDur        =dur;
					dur         =vel.mapVelocityToRange(p[5] ,p[13],0.5 ,3 );
					attackRate  =vel.mapVelocityToRange(p[6] ,p[14],0.03,1 );
					attackAmount=vel.mapVelocityToRange(p[7] ,p[15],0   ,1 );
					attackRate2 =vel.mapVelocityToRange(p[8] ,p[16],0   ,1 );
					filterScale =vel.mapVelocityToRange(p[9] ,p[17],0   ,10);
					q           =vel.mapVelocityToRange(p[10],p[18],0   ,1 );
					noise       =vel.mapVelocityToRange(p[19],p[20],0   ,1 );
					node=server.nextNodeID;
					server.sendBundle(latency,
						["/s_new", "kick", node, 0, instGroupID,
							\amp,		   amp,
							\note,	   	   npoNote,
							\dur,		   dur**2,
							\mDur,           mDur,
							\attackTime,	   attackRate/2**2,
							\attackAmount,   attackAmount,
							\attackTime2,    attackRate2*3,
							\filterScale,    filterScale,
							\q,              q,
							\noise,          noise,
							\outputChannels, outputChannels,
							\pan,            pan,
							\sendChannels,   sendChannels,
							\send,           send,
							\masterSendChannels,	masterSendChannels,
							\masterSendAmp,		masterSendAmp,

						]);
					if (synthsOn[drum].notNil) {
						server.sendBundle(latency,[\n_set, synthsOn[drum], \gate, -1.005]);
					};
					synthsOn[drum]=node;
					{gui[\lamps][drum].value_(vel,0.15);}.defer(latency?0);
				}
				
				// snare
				{1} {
								
					note        =vel.mapVelocityToRange(p[67] ,p[68],30  ,70).asInt;
					npoNote     =note-p[22]/(p[21])*12+p[22];
					mDur        =dur;
					dur         =vel.mapVelocityToRange(p[69] ,p[70],0.3 ,2   );
					nMix        =vel.mapVelocityToRange(p[71] ,p[76],0   ,1   );
					q           =vel.mapVelocityToRange(p[72] ,p[77],0.01,0.99)**2;
					fq          =vel.mapVelocityToRange(p[73] ,p[78],0.01,1.8 );
					env         =vel.mapVelocityToRange(p[74] ,p[79],0   ,1.3 );
					nDur        =vel.mapVelocityToRange(p[75] ,p[80],0   ,1   );
					node=server.nextNodeID;
					server.sendBundle(latency,
						["/s_new", "snare", node, 0, instGroupID,
							\amp,            amp,	
							\note,	   	   npoNote,
							\dur,		   dur**2,
							\outputChannels, outputChannels,
							\pan,            pan,
							\sendChannels,   sendChannels,
							\send,           send,
							\noiseMix,       1-nMix,
							\noiseFilt1,     q,
				              \noiseFilt2,     fq,
				              \attackAmount,   env,
				              \nDur,           nDur,
				              \mDur,           mDur,
							\masterSendChannels,	masterSendChannels,
							\masterSendAmp,		masterSendAmp,
							]);
					if (synthsOn[drum].notNil) {
						server.sendBundle(latency,[\n_set, synthsOn[drum], \gate, -1.005]);
					};
					synthsOn[drum]=node;
					{gui[\lamps][drum].value_(vel,0.1);}.defer(latency?0);
				}
				
				// clap
				{2} {
				
					dur         =vel.mapVelocityToRange(p[81] ,p[82],0.3 ,2   ) * dur;
					q           =vel.mapVelocityToRange(p[83] ,p[84],0.01,1.5 );
					fq          =vel.mapVelocityToRange(p[85] ,p[86],0.01,10  )/10**2*10;
					// this little formula stops the clap from exploding
					fq=fq-(((fq*2000*q*3).clip(17000,9999999)-17000)/(2000*q*3));
					rnd         =vel.mapVelocityToRange(p[87] ,p[88],0   ,1   )/20;
					node=server.nextNodeID;
					server.sendBundle(latency,
						["/s_new", "clap", node, 0, instGroupID,
							\amp,            amp*(q.map(0,2,0.5,0.25)),
							\outputChannels, outputChannels,
							\pan,            pan,
							\sendChannels,   sendChannels,
							\send,           send,
							\dur,            dur,
							\q,              q**1.5,
							\fq,             fq,
							\rnd1,           rnd.rand2,
							\rnd2,           rnd.rand2,
							\rnd2,           rnd.rand2,
							\masterSendChannels,	masterSendChannels,
							\masterSendAmp,		masterSendAmp,
							]);
					if (synthsOn[drum].notNil) {
						server.sendBundle(latency,[\n_set, synthsOn[drum], \gate, -1.005]);
					};
					synthsOn[drum]=node;
					{gui[\lamps][drum].value_(vel,0.1);}.defer(latency?0);
				}
				
				// tom
				{3} {
				
					note      =vel.mapVelocityToRange(p[89] ,p[90],30  ,80).asInt;
					npoNote   =(note-p[22]/(p[21])*12+p[22]);
					freq      =npoNote.midicps.clip(0.0001,18000);
					dur       =vel.mapVelocityToRange(p[91] ,p[92],0.1 ,2   ) * dur;
					timbre    =vel.mapVelocityToRange(p[93] ,p[94],0   ,10  );
					timbre    =((timbre/10)**1.5)*10;
					cFreq     =(vel.mapVelocityToRange(p[95] ,p[96],-4 ,4)*(p[21]+1)).asInt;
					cFreq     =(note+cFreq-p[22]/(p[21]+1)*12+p[22]).midicps.clip(0.0001,18000);
					slope     =vel.mapVelocityToRange(p[97] ,p[98],0   ,1  );
					slopeDur  =vel.mapVelocityToRange(p[97].map(0,1,0.2,2),p[98],0.2 ,2)**2;
					
					attackRate=vel.mapVelocityToRange(p[99],p[100],0.2 ,2)**2;
					attackAmount=vel.mapVelocityToRange(p[101],p[102],0,1);
					
					stick = (vel.mapVelocityToRange(p[103],p[104],0,2)**2)**1.25;
					
					node=server.nextNodeID;
					server.sendBundle(latency,
						["/s_new", "SOStom", node, 0, instGroupID,
							\amp,            amp,
							\decay,          dur,
							
							\freq,           freq,
							\cFreq,          cFreq,
							\note,           npoNote,
							
							\slope,          slope,
							\slopeDur,       slopeDur,
							
							\drum_timbre,    timbre,
							\outputChannels, outputChannels,
							\pan,            pan,
							\sendChannels,   sendChannels,
							\send,           send,
							
							\eDur,           attackRate,
							\attackAmount,   attackAmount,
							\stick,          stick,
							
							\masterSendChannels,	masterSendChannels,
							\masterSendAmp,		masterSendAmp,
							
						]);
					if (synthsOn[drum].notNil) {
						server.sendBundle(latency,[\n_set, synthsOn[drum], \gate, -1.01]);
					};
					synthsOn[drum]=node;
					{gui[\lamps][drum].value_(vel,0.1);}.defer(latency?0);
				}
				
				// hat
				{4} {
									
					dur       =(vel.mapVelocityToRange(p[105] ,p[106],0.1 ,4   )**2)*dur;
					note      =vel.mapVelocityToRange(p[107] ,p[108],-20 ,10  );
					//mix       =vel.mapVelocityToRange(p[109] ,p[110],  0 ,1   );
					lp        =vel.mapVelocityToRange(p[111] ,p[112],  0.5 ,1 )**1.5;
					q         =vel.mapVelocityToRange(p[113] ,p[114],  0 ,0.8);
					hp        =vel.mapVelocityToRange(p[115] ,p[116],  0.5 ,1  );
					
					node=server.nextNodeID;	
					server.sendBundle(latency,
						["/s_new", "hat", node, 0, instGroupID,
							\amp,amp,
							\outputChannels, outputChannels,
							\pan,            pan,
							\sendChannels,   sendChannels,
							\send,           send,
							\dur,            dur,
							\noteAdj,        note,
							//\mix,            mix,
							\lp,             lp,
							\q,              q,
							\hp,             hp,
							\masterSendChannels,	masterSendChannels,
							\masterSendAmp,		masterSendAmp,
						]);
					if (synthsOn[drum].notNil) {
						server.sendBundle(latency,[\n_set, synthsOn[drum], \gate, -1.01]);
					};
					synthsOn[drum]=node;
					{gui[\lamps][drum].value_(vel/2+0.5,dur/10);}.defer(latency?0);
				};
				
	}
	
	clockPlay { }		//play and stop are called from both the internal and extrnal clock
	clockStop {
		channels.do({|y|
			{posModels[y].value_(0)}.defer; // reset gui counter positions
		})
	}	
	clockPause{ }		// pause only comes the internal clock

	/// i have down timed the clock by 3, so this gives 32 beats in a 4x4 bar
	/// this is all the resoultion i need at the moment but could chnage in the future
	
	// this is called by the studio for the zeroSL auto map midi controller
	autoMap{|num,val|
		var vPot;
		vPot=(val>64).if(64-val,val);
	}

} // end ////////////////////////////////////
