
// based on the acid_otophilia ugens from the example pieces created by otophilia
// adapted by lnx 2009

LNX_BumNote : LNX_InstrumentTemplate {

	var	<seq,  <sP;
	
	var	<steps, <channels, <defaultSteps=128, <defaultChannels=5;
	
	var	<seqPresetMemory, <sPPresetMemory;

	var	<pitchStart, <pitchEnd, <pitchEnv, <pitchEnvArray;
	    
	var	<midiNotes, <acid, <acidNode, lastFilterPitch=0,
		<notesOn, <velocitysOn, findingNote=false,
		isRecording=false, recordIndex=0,
		lastKeyboardNote, isKeyboardWriting=true,
		lastNotePlayedBySeq;
		
	var	<seqModels, <seqMVCViews;
	
	var	<spModels, <spMVCViews,  <posModels;
		
	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"Bum Note"}
	*sortOrder{^1}
	isInstrument{^true}
	canBeSequenced{^true}
	isMixerInstrument{^true}
	
	hasLevelsOut{^true}
	
	mixerColor{^Color(1,0.75,0.75,0.4)} // colour in mixer
	
	*isVisible{^false}
	
	header { 
		// define your document header details
		instrumentHeaderType="SC BumNote Doc";
		version="v1.4";		
	}
	
	// an immutable list of methods available to the network
	interface{^#[
		\netSeq, \netChannelItem, \netSteps, \netRuler, \netAcidMix, \netAmpEnv,
		\netFilterVelocity, \netFilterEnv, \netFilterType, \netLink, \netQ, \netSetPnpoAdjust
	]}

	// the models
	initModel {
	
		// these are the models and their defaults. you can add to this list as you
		// develope the instrument and it will still save and load previous versions. it
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
			[ \db6,  midiControl, 2, "Amp",
				(mouseDownAction_:{hack[\fadeTask].stop}),
				{|me,val,latency,send| this.setSynthArgVP(2,val,\amp,val.dbamp,latency,send)}],
				
			// 3.q
			[0.9, \unipolar, (\label_:"Q",\numberFunc_:\float2), midiControl, 3, "Filter Q",
				{|me,val,latency,send| this.setQ(val,latency,send) }],
				
			// 4.filter env
			[1, \unipolar, (\label_:"Env",\numberFunc_:\float2), midiControl, 4, "Filter Env",
				{|me,val,latency,send|
					this.setSynthArgVP(4,val,\filterEnv,(1-(p[17]*2))*val,latency,send)}],
					
			// 5.pulse width
			[0, [0,0.49], (\label_:"PW",\numberFunc_:\float2), midiControl, 5, "Pulse Width",
				{|me,val,latency,send| this.setSynthArgVP(5,val,\pw,0.5-val,latency,send)}],
					
			// 6.mod freq	
			[2.5, [0.1,5], midiControl, 6, "LFO Freq",(\label_:"Freq",\numberFunc_:\float1),
				{|me,val,latency,send|
					this.setSynthArgVP(6,val,\modFreq,val**2.5,latency,send)}],
			
			// 7.mod amp
			[0, \unipolar, (\label_:"Amp",\numberFunc_:\float2), midiControl, 7, "Mod Amp",
				{|me,val,latency,send|
					this.setSynthArgVP(7,val,\modAmp,(val**1.75)*4,latency,send)}],
				
			// 8.osc 1-2 mix
			[0, \unipolar, midiControl, 8, "Osc 1-2 Mix",(\label_:"Mix", \zeroValue_:0.5,
				\numberFunc_: {|n| var s;
					n=n*2-1;
					case	
						{(n<0)and:{-1<n}}	{ s=n.asFormatedString(1,2) }
						{(n>0)and:{n<1}}	{ s="+"++(n.asFormatedString(1,2)) }
						{n==(-1)}			{ s="Pulse" }
						{n==1}			{ s="Saw" }
						{n==0}			{ s="0" };
					s		
				}),
				{|me,val,latency,send| this.setAcidMix(val,latency,send)}],
			
			// 9.filter fq
			[-24.75, [-45,65], midiControl, 9, "Filter Freq",
				(\label_:"Filter",\numberFunc_:\float1),
				{|me,val,latency,send| this.setSynthArgVP(9,val,\filterFq,val,latency,send)}],
			
			// 10.filterLFO
			[0, [0,50], midiControl, 10, "Filter LFO",(\label_:"LFO",
				\numberFunc_: {|n| n=n.map(0,50,0,1); n.asFormatedString(1,2) }),
				{|me,val,latency,send|
					this.setSynthArgVP(10,val,\filterLFO,val*1.5,latency,send)}],
			
			// 11.spo
			[11, [0,23], midiControl, 11, "Steps per octave",
				(\items_: ((1..24).collect{|i| i+("oct")})),
				{|me,val,latency,send| this.setPnpoAdjust(11,val,latency,send)}],
			
			// 12.base note
			[\MIDInote, midiControl, 12, "MIDI Base", (\label_:"Base"),
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
						me.lazyValueAction_(note,nil,true)}}),
				{|me,val,latency,send|
					this.setPnpoAdjust(12,val,latency,send)
				}],
					
			// 13.osc1 pitch	
			[0, \pitchAdj, midiControl, 13, "Osc1 pitch",
				(\label_:"Pitch", \numberFunc_:\intSign, \zeroValue_:0),
				{|me,val,latency,send|
					this.setSynthArgVP(13,val,\osc1Pitch,
						(val+p[14])/(p[11]+1)*12,latency,send)   
				}],
			
			// 14.osc1 adjust
			[0, \bipolar, midiControl, 14, "Osc1 fine",
				(\label_:"Fine", \numberFunc_:\float2Sign, \zeroValue_:0),
				{|me,val,latency,send|
					this.setSynthArgVP(14,val,\osc1Pitch,
						(p[13]+val)/(p[11]+1)*12,latency,send)   
				}],
			
			// 15.osc2 pitch
			[0, \pitchAdj, midiControl, 15, "Osc2 pitch",
				(\label_:"Pitch", \numberFunc_:\intSign, \zeroValue_:0),
				{|me,val,latency,send|
					this.setSynthArgVP(15,val,\osc2Pitch,
						(val+p[16])/(p[11]+1)*12,latency,send)   
				}],
			
			// 16.osc2 adjust
			[0, \bipolar, midiControl, 16, "Osc2 fine",
				(\label_:"Fine", \numberFunc_:\float2Sign, \zeroValue_:0),
				{|me,val,latency,send|
					this.setSynthArgVP(16,val,\osc2Pitch,
						(p[15]+val)/(p[11]+1)*12,latency,send)   
				}],
			
			// 17.invert filter env
			[0, \switch, (\strings_:"Inv"), midiControl,17,"Filter Invert",
				{|me,val,latency,send|
					this.setSynthArgVP(17,val,\filterEnv,(1-(val*2))*p[4],latency,send)}],
			
			// 18.filter KYBD
			[0, \bipolar, midiControl, 18, "Filter KYBD",
				(\label_:"KYBD",\numberFunc_:\float2,\zeroValue_:0),
				{|me,val,latency,send| this.setSynthArgVP(18,val,\kybd,val,latency,send) }],
			
			// 19.slide osc pitch
			[0.2, \unipolar, (\label_:"Slide",\numberFunc_:\float2), midiControl, 19, "Slide",
				{|me,val,latency,send| this.setPVP(19,val,latency,send) }],
			
			// 20.Attack (Amp ADSR Env)
			[0.001, [0.001,1], (\label_:"A",\numberFunc_:\float2), midiControl, 20, "Attack",
				{|me,val,latency,send| this.setAmpEnv(20,val,latency,send) }],
			
			// 21.Decay (Amp ADSR Env)
			[2, [0,4], (\label_:"D",\numberFunc_:\float2), midiControl, 21, "Decay",
				{|me,val,latency,send| this.setAmpEnv(21,val,latency,send) }],					
			// 22.Sustain (Amp ADSR Env)
			[1, [0,1], (\label_:"S",\numberFunc_:\float2), midiControl, 22, "Sustain",
				{|me,val,latency,send| this.setAmpEnv(22,val,latency,send) }],
				
			// 23.Release (Amp ADSR Env)
			[0.8, [0.01,4,\exp], midiControl, 23, "Release",
				(\label_:"R",\numberFunc_:\float2),
				{|me,val,latency,send| this.setAmpEnv(23,val,latency,send) }],
			
			// 24.Filter Attack (Env)
			[0.001, [0,2],  midiControl, 24, "Filter Attack",
				 (\label_:"A",\numberFunc_:\float2),
				{|me,val,latency,send| this.setFilterEnv(24,val,latency,send) }],
			
			// 25.Filter Release (Env)
			[0.8, [0.01,4],  midiControl, 25, "Filter Release",
				(\label_:"R",\numberFunc_:\float2),
				{|me,val,latency,send| this.setFilterEnv(25,val,latency,send) }],
				
			// 26.pan
			[\pan, (\label_:"Pan",\numberFunc_:\pan, \zeroValue_:0), midiControl,26,"Pan",
				{|me,val,latency,send| this.setSynthArgVP(26,val,\pan,val,latency,send) }],
			
			// 27.MIDI low 
			[0, \MIDInote, midiControl, 27, "MIDI Low", (\label_:"Low"),
				\actions_ -> (\action2Down -> {|me|
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
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
					LNX_MIDIPatch.noteOnTrigFunc_{|src, chan, note, vel|
						me.lazyValueAction_(note,nil,true);} // enter value via keyboard
				}),
				{|me,val,latency,send|
					var value=val.clip(p[27],127);
					this.setPVP(28,value,latency,send);
					if (value!=val) {{me.value_(value)}.defer(0.05)};
				}],
			
			// 29.output channels
			[0, \LNX_audiobus, midiControl, 29, "Output channels",
				(\numberFunc_:\LNX_audiobus,\showNumberBox_:false,
					\items_:LNX_AudioDevices.outputChannelList),
				{|me,val,latency,send|
					this.instOutChannel_((val*2));
					this.setPVPModel(29,val,0,send);   // to test on network
				}],
			
			// 30.send channels = 4&5
			[2, \LNX_audiobus, midiControl, 30, "Send Channels",
				(\numberFunc_:\LNX_audiobus, \showNumberBox_:false,
					\items_:LNX_AudioDevices.outputChannelList),
				{|me,val,latency,send|
					this.setSynthArgVH(30,val,\sendChannels,val*2,latency,send)}],
					
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
			[0.5, \unipolar, midiControl, 33, "Filter Velocity",
				(\label_:"Velocity", \numberFunc_:\float2),
				{|me,val,latency,send|
					this.setFilterVelocity(val,latency,send) }],
			
			// 34.show sequencer
			[0, \switch, (\strings_:"Seq"), midiControl, 34, "Show/Hide Seq",
			{|me,val,latency,send|  p[34]=val; {this.arrangeWindow}.defer }], // not networked
			
			// 35.show keyboard
			[1, \switch, (\strings_:"Key"), midiControl, 35, "Show/Hide Keyboard",
			{|me,val,latency,send|  p[35]=val; {this.arrangeWindow}.defer }], // not networked
			
			// 36.stepping filter
			[0, [0,7.3680629972808], midiControl, 36, "Filter Stepping",
				(\label_:"Stepping",\numberFunc_:{|n| (n**1.5).asFormatedString(1,1) }), 
				{|me,val,latency,send|
					this.setSynthArgVP(36,val,\steppingFilter,val**1.5,latency,send)}],
			
			// 37.Filter Type (0=FF, 1=LADDER, 2=DFM1_lp, 3=DMF1_hp)
			[1, [0,3,\lin,1], midiControl, 37, "Filter Type",
				(\items_:["Normal","Ladder","DFM1 lp","DFM1 hp"]),
				{|me,val,latency,send| this.setFilterType(me.value,latency,send) }],
			
			// 38.Lock Seq
			[0, \switch, (\strings_:"Lock Seq"), midiControl, 38, "Lock Seq",
				{|me,val,latency,send| this.setPGD(38,val,latency,send) }],
			
			// 39. Not used
			[0, [0,1], (\strings_:"!"), midiControl, 39, "Not used",
				{|me,val,latency,send|
			
			}],
			
			// 40.adv counter
			[0, \switch, (\strings_:"Adv"), midiControl, 40, "Advance",
				{|me,val,latency,send| p[40]=val }],
			
			// 41.eq
			[1, \switch, (\strings_:"EQ"), midiControl, 41, "EQ",
				{|me,val,latency,send| this.setSynthArgVH(41,val,\eq,val,latency,send)}],
			
			// 42.ladder pre-amp
			[0, [0,2,\lin,1], midiControl, 42, "Ladder Pre-Amp",
			{|me,val,latency,send|
				this.setSynthArgVP(42,val,\filterDrive,
					[5,1,0.45].clipAt(val.asInt),latency,send)
			}],
			
			// 43.link osc''s
			[0, \switch, (\strings_:"Link"), midiControl, 43, "Link Osc",
				{|me,val,latency,send| this.setLink(me.value,latency,send)}],
				
			// 44. peak level
			[0.7, \unipolar,  midiControl, 44, "Peak Level",
				{|me,val,latency,send| this.setPVP(44,val,latency,send) }],
			
		].generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,34,35,38,40];
		randomExclusion=[0,1,2,11,12,27,28,29,30,34,35,38,39,40,44];
		autoExclusion=[];

	}
	
		
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
		channels=defaultChannels;
		steps=defaultSteps ! channels;
		
		// seq steps
		// make models with defaults
		
		seq={0 ! defaultSteps} ! channels; 
		
		seq[2]=(-1) ! defaultSteps;
		seq[3]=(-1) ! defaultSteps;
		
		// new mvc seq
	
		seqMVCViews = {LNX_EmptyGUIShell ! defaultSteps} ! channels;
		seqModels = {nil ! defaultSteps} ! channels;
		posModels = {0.asModel} ! channels;
	
		defaultSteps.do{|i|
		
			// 0.note on
			seqModels[0][i]=[0,\switch,midiControl,200+10+i, "Note ON Step:"++((i+1).asString),
				{|me,val,latency,send|
					this.setSeq(0,i,val,latency,send);
					seqModels[1][i].lazyValueAction_(val,nil,true,false);
					{
						this.setNoteDisplay(seq[2][i],i);
					}.defer;
				}].asModel;
				
			// 1.note off
			seqModels[1][i]=[0,\switch,
				midiControl,(2)*200+10+i,"Note OFF Step:"++((i+1).asString),
				{|me,val,latency,send|
					this.setSeq(1,i,val,latency,send);
				}].asModel;
				
			// 2.midi note
			seqModels[2][i]=[-1,[-1,127,\lin,1],
				(zeroValue_: 0, numberFunc_: \note, showNumberBox_:false),
				midiControl,(3)*200+10+i,"Note Step:"++((i+1).asString),
				{|me,val,latency,send|
					this.setSeq(2,i,val,latency,send);
					{
						this.setNoteDisplay(val,i);
						gui[\noteDisplay].focus;
					}.defer;			
				}].asModel;
			
			// 3.velocity
			seqModels[3][i]=[-1,[-1,127,\lin,0], (zeroValue_: 0),
				midiControl,(4)*200+10+i,"Velocity Step:"++((i+1).asString),
				{|me,val,latency,send|
					this.setSeq(3,i,val,latency,send);
				}].asModel;
			
			// 4.slide
			seqModels[4][i]=[0,\switch,
				midiControl,(5)*200+10+i,"Slide Step:"++((i+1).asString),
				{|me,val,latency,send|
					this.setSeq(4,i,val,latency,send);
					{
						this.setNoteDisplay(seq[2][i],i);
					}.defer;
				}].asModel;
		};

		// 1st velocity in sequence = 100
		seq[3][0]=100;
		seqModels[3][0].value_(100); // can we just do .valueAction_(100) in model
		
		// new MVC seq controls
		
		spModels=Array.newClear(channels);
		sP=Array.newClear(channels);
		
		channels.do{|i|
			var mods,sPs;
			#mods,sPs= [
				1,			// 
				0.5,			// 
				36,			// 
				
				// 3. no of steps
				[ 32, [1,defaultSteps,\lin,1], midiControl, (i+1)*200+3, "Steps ch:"++(i+1),
					(label_:"Steps"),
					{|me,val,latency,send| this.setSteps(i,val,latency,send) }],	         
				
				// 4. ruler
				[ 4, [2,16,\lin,1], midiControl, (i+1)*200+4, "Ruler ch:"++(i+1),
					(label_:"Ruler"),
					{|me,val,latency,send| this.setRuler(i,val,latency,send) }],	 
				0,            // 
				// 6. speed divider
				[ 2, [1,32,\lin,1], midiControl, (i+1)*200+6, "Speed ch:"++(i+1),
					(label_:"Speed"),
					{|me,val,latency,send| this.setChannelItem(i,6,val,latency,send) }]	 
			].generateAllModels;
			spModels[i]=mods;
			sP[i]=sPs;
		};
		
		// presets
		seqPresetMemory=[];
		sPPresetMemory=[];		
		
		// gui's
		gui[\keyboardView]=LNX_EmptyGUIShell;
		gui[\noteOnLamp]=LNX_EmptyGUIShell; 
		
		// synth stuff
		
		midiNotes=[];
		channels.do({|i| midiNotes=midiNotes.add(60+i)});  // this maybe redundant for simpleSeq
		
		notesOn=[];
		velocitysOn=[];
		
		pitchStart=34;
		pitchEnd=34;
		pitchEnv=0;
		pitchEnvArray=[ 10, 2, -99, -99, 10, 0, 1, 0, 20, 30, 1, 0 ];
		
	}
	
	// anything else that needs doing after a server reboot; 
	iServerReboot{}
		
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
		if (p[38]==0) {
			seq=seqPresetMemory[i].deepCopy;
			sP=sPPresetMemory[i].deepCopy;
			{this.iUpdateGUI(newP)}.defer;
		}{
			{
				models[42].enabled_(#[false,true,true,true].at(newP[37]));
				models[15].enabled_(#[true,false].at(newP[43]));
				models[16].enabled_(#[true,false].at(newP[43]));
			}.defer;
		};
	}
	
	// for your own remove preset
	iRemovePreset{|i| seqPresetMemory.removeAt(i); sPPresetMemory.removeAt(i);}
	
	// for your own removal of all presets
	iRemoveAllPresets{ seqPresetMemory=[]; sPPresetMemory=[] }
	
	// clear the sequencer
	clearSequencer{
		seqModels.do{|seq,i| seq.do{|models|
			models.lazyValueAction_([0,0,-1,0,0][i],nil,true)
		} };
		seqModels[3][0].lazyValueAction_(100,nil,true);
	}
	
	////////////////////////////////
	
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
		var sPSize, channels;
		
		channels=(instrumentLoadVersion>=1.3).if(5,4); // slide seq
		
		l.pop; // to use later when you can change no of channels
		sPSize=l.pop.asInt;
		channels.do{|i|
			sP[i]=l.popNF(sPSize);
			seq[i]=l.popNF(defaultSteps)
		};
		sPPresetMemory=[]!defaultChannels!noPre;
		seqPresetMemory=[]!defaultChannels!noPre;
		if (instrumentLoadVersion>=1.2) {
			noPre.do{|i|
				channels.do{|j|
					sPPresetMemory[i][j]=l.popNF(sPSize);
					seqPresetMemory[i][j]=l.popNF(defaultSteps);
				};
				if (instrumentLoadVersion==1.2) {
					sPPresetMemory[i][4]=sPPresetMemory[i][2].copy;
					seqPresetMemory[i][4]=0!defaultSteps;
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
			if (instrumentLoadVersion<1.4) {
				models[2].valueAction_(((p[2]**2)/3).ampdb);
				models[31].valueAction_(p[31].ampdb);
				presetMemory.size.do{|i|
					presetMemory[i][2]= ((presetMemory[i][2]**2)/3).ampdb;
					presetMemory[i][31]= presetMemory[i][31].ampdb;
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
			
			seqModels[y].do{|i,j| if (seq[y][j]!=i.value) {i.lazyValue_(seq[y][j])}};
			spModels[ y].do{|i,j| if ( sP[y][j]!=i.value) {i.lazyValue_( sP[y][j])}};
		
			if (oldRulerValue != sP[y][4]) { this.changeSeqColours(y,val) };	
			models[42].enabled_(#[false,true,true,true].at(p[37]));
			models[15].enabled_(#[true,false].at(p[43]));
			models[16].enabled_(#[true,false].at(p[43]));
			
		}
	}
	
	// anything else that needs doing after a load. all paramemters will be loaded by here
	iPostLoad{
		this.arrangeWindow;
		// temp fix to use the right filter
		this.stopDSP;
		this.startDSP;
		this.updateDSP;
	}
	
	iPostInit{
		channels.do{|y|
			var val=sP[y][3],enabled;
			
			seqModels[y].do({|i,x| 
				enabled=(x<val).if(true,false);
					if (i.enabled!=enabled) {
						i.enabled_(enabled)
					};
				});
		};
		
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
		seqModels[y][x].lazyValue_(value);
	}
	
	//
	
	setChannelItem{|y,x,value,latency,send=true|
		if (value!=sP[y][x]) {
			sP[y][x]=value;
		};
		if (send) {
			api.sendVP((id++"sc"++y++"_"++x).asSymbol,'netChannelItem',y,x,value);
		};
	}
	
	netChannelItem{|y,x,value|
		sP[y][x]=value;
		spModels[y][x].lazyValue_(value);	
	}
	
	//
	
	setSteps{|y,value,latency,send=true|
		var enabled;
		if (y==2) { this.setSteps(4,value,latency,send) };
		
		if (sP[y][3]!=value) {
			sP[y][3]=value;
			if (send) {
				api.sendVP((id++"sS"++y).asSymbol,'netSteps',y,value);
			};
			{	
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
		spModels[y][3].lazyValue_(value);
		{
			seqModels[y].do({|i,x|
				enabled=(x<value).if(true,false);
				if (i.enabled!=enabled) {
					i.enabled_(enabled)
				};
			});
		}.defer;
	}
	
	//
	
	setRuler{|y,value,latency,send=true|
		if (sP[y][4]!=value) {
			sP[y][4]=value;
			if (send) { api.sendVP((id++"sr"++y).asSymbol,'netRuler',y,value) };
			{ this.changeSeqColours(y,value) }.defer;
		};
	}
	
	netRuler{|y,value|
		sP[y][4]=value;
		spModels[y][4].lazyValue_(value);
		{this.changeSeqColours(y,value) }.defer;
	}
	
	//
	
	setAcidMix{|val,latency,send=true|
		if (p[8]!=val) {
			p[8]=val;
			server.sendBundle(latency,[\n_set, node, \osc1Amp,  (1- p[8])**0.5]);
			server.sendBundle(latency,[\n_set, node, \osc2Amp,      p[8] **0.5]);
			if (send) {
				api.sendVP(id++"sam"++8,\netAcidMix,val);
			}
		}
	}
	
	netAcidMix{|val|
		if (p[8]!=val) {
			p[8]=val;
			models[8].lazyValue_(val);
			server.sendBundle(nil,[\n_set, node, \osc1Amp,  (1- p[8])**0.5]);
			server.sendBundle(nil,[\n_set, node, \osc2Amp,  p[8] **0.5  ]);
		}
	}
	
	//
	
	setAmpEnv{|index,val,latency,send=true|
		var env;
		if (p[index]!=val) {
			p[index]=val;
			env=Env.adsr(p[20], p[21], p[22], p[23], 1, -4).asArray;
			server.sendBundle(latency,[\n_setn, node, \newAmpEnv, env.size] ++ env);
			if (send) {
				api.sendVP(id++"sae"++index,\netAmpEnv,index,val);
			}
		}
	}
	
	netAmpEnv{|index,val|
		var env;
		if (p[index]!=val) {
			p[index]=val;
			models[index].lazyValue_(val);
			env=Env.adsr(p[20], p[21], p[22], p[23], 1, -4).asArray;
			server.sendBundle(nil,[\n_setn, node, \newAmpEnv, env.size] ++ env);
		}
	}
	
	// this might force this internal sequencer filterVelocity early if delta zero
	setFilterVelocity{|val,latency,send=true|
		if (p[33]!=val) { 
			p[33]=val;
			server.sendBundle(latency,[\n_set, node, \filterPitch,lastFilterPitch*p[33]]);
			if (send) {
				api.sendVP(id++"nfv",\netFilterVelocity,val);
			}
		};
	}
	
	netFilterVelocity{|val|
		if (p[33]!=val) {
			p[33]=val;
			models[33].lazyValue_(val);
			server.sendBundle(nil,[\n_set, node, \filterPitch,lastFilterPitch*p[33]]);
		}
	}
	
	//
	
	setFilterEnv{|index,val,latency,send=true|
		var env;
		if (p[index]!=val) {
			p[index]=val;
			env=Env.perc(p[24]**2, p[25], 1, -4);
			env.releaseNode_(2);
			env=env.asArray;
			server.sendBundle(latency,[\n_setn, node, \newFilterEnv, env.size] ++ env);
			if (send) {
				api.sendVP(("_"++id++index).asSymbol,\netFilterEnv,index,val);
			};
		};
	}
	
	netFilterEnv{|index,val|
		var env;
		if (p[index]!=val) {
			p[index]=val;
			models[index].lazyValue_(val);
			env=Env.perc(p[24]**2, p[25], 1, -4);
			env.releaseNode_(2);
			env=env.asArray;
			server.sendBundle(nil,[\n_setn, node, \newFilterEnv, env.size] ++ env);
		}
	}
	
	//
	
	setFilterType{|type,latency,send=true|
		if (send) {
			api.groupCmdOD(\netFilterType,type);
		}{
			this.netFilterType(type)
		}
	}
	
	netFilterType{|type|
		if (p[37]!=type) {
			p[37]=type;
			this.stopDSP;
			this.startDSP;
			this.updateDSP;
			{
				models[37].lazyValue_(type);
				models[42].enabled_(type.isTrue);
			}.defer;
		}
	}
	
	// set link and disable 2nd osc pitch control if needed
	
	setLink{|val,latency,send=true|
		if (send) {
			api.groupCmdOD(\netLink,val);
		}{
			p[43]=val;
			server.sendBundle(latency,[\n_set, node, \linkOsc, val]);
			{
				models[15].enabled_(val.isFalse);
				models[16].enabled_(val.isFalse);
			}.defer;
		}
	}
	
	// net of above
	
	netLink{|val|
		p[43]=val;
		server.sendBundle(nil,[\n_set, node, \linkOsc, val]);
		{
			models[43].lazyValue_(val);
			models[15].enabled_(val.isFalse);
			models[16].enabled_(val.isFalse);
		}.defer;
	}
	
	// get 
	
	getQandAmp{|val|	
		switch ((p[37].asInt),
			  0, { //moog ff
				^[LNX_ControlMap.value(\moogFFQAmp,val), LNX_ControlMap.value(\moogFFQ,val)]
			},1, { //moog ladder
				^[LNX_ControlMap.value(\moogLadderQAmp,val),
					LNX_ControlMap.value(\moogLadderQ,val)]
			},2, { //DFMlp
				^[0.7*LNX_ControlMap.value(\DFMlpQAmp,val), LNX_ControlMap.value(\DFMlpQ,val)]
			},3, { //DFMhp
				^[0.6*LNX_ControlMap.value(\DFMhpQAmp,val), LNX_ControlMap.value(\DFMhpQ,val)]
			}
		);
	}

	// set the q of the filter
	
	setQ{|val,latency,send=true|
		var amp;
		if (p[3]!=val) {
			p[3]=val;
			if (send) { api.sendVP(id++"_q",\netQ,val) };
			#amp,val=this.getQandAmp(val);
			server.sendBundle(latency,[\n_set, node, \q,    val]);
			server.sendBundle(latency,[\n_set, node, \qAmp, amp]);
		};
	}
	
	netQ{|val|
		var amp=1;
		if (p[3]!=val) {
			p[3]=val;
			models[3].lazyValue_(p[3]);
			#amp,val=this.getQandAmp(val);
			server.sendBundle(nil,[\n_set, node, \q,    val]);
			server.sendBundle(nil,[\n_set, node, \qAmp, amp]);
		}
	}
	
	// set npo steps and adjust osc pitch
	
	setPnpoAdjust{|item,value,latency,send=true|
		if (p[item]!=value) {
			p[item]=value;
			server.sendBundle(latency,[\n_set, node, \osc1Pitch,(p[13]+p[14])/(p[11]+1)*12]);
			server.sendBundle(latency,[\n_set, node, \osc2Pitch,(p[15]+p[16])/(p[11]+1)*12]);
			if (send) {
				api.sendVP(id++"spnpoa"++item,'netSetPnpoAdjust',item,value);
			};
		};
	}
			
	netSetPnpoAdjust{|item,value|
		if (p[item]!=value) {
			p[item]=value;
			models[item].lazyValue_(value);
			server.sendBundle(nil,[\n_set, node, \osc1Pitch,(p[13]+p[14])/(p[11]+1)*12]);
			server.sendBundle(nil,[\n_set, node, \osc2Pitch,(p[15]+p[16])/(p[11]+1)*12]);
		}
	}
	
	//// uGens & midi players //////////////////////////

	// this will be called by studio after booting
	*initUGens{|server|
	
		if (verbose) { "SynthDef loaded: Bum Note".postln; };
	
		// MoogFF version
	
		SynthDef("BumNote_FF", {
			
			arg	outputChannels=0,	gate=1, 			amp=0.1,
				pw=0.5,			filterPitch=0,	filterFq=0,
				q=0.3,			qAmp=1,			filterEnv=1,
				modFreq=10, 		modAmp=0, 		filterLFO=0,
				osc1Amp=1,   		osc2Amp=0,
				osc1Pitch=0,		osc2Pitch=0,
				kybd=1,			pan=0,
				filterLag=0,		steppingFilter=0,
				sendAmp=0, 		sendChannels=4,
				pGate=0,			filterDrive=1,	eq=1,
				linkOsc=0;
				
			var	ampEnv, 			env2,			out,
				out1, 			out2,			out3,
				lfoOut, 			lfoOutFilter, 	lfoOutPW,
			    	envctl, 			myEnv,
			    	envctl2, 			myEnv2, 			filterResponse,
			    	pEnv, 			pc, 				pitch=34,
			    	pitch1,			pitch2;
			
			// amp envelope
			myEnv  = Env.adsr(0.001, 2  , 0, 0.04,  1, -4);
			envctl = Control.names([\newAmpEnv]).kr( myEnv.asArray );
			ampEnv = EnvGen.ar(envctl,  gate);
			
			// filter envelope
			myEnv2 = Env.perc(0.001, 0.8, 1, -4);
			myEnv2.releaseNode_(2);
			envctl2= Control.names([\newFilterEnv]).kr( myEnv2.asArray );
			env2   = EnvGen.kr(envctl2, gate*2-1, filterEnv*70);

			// pitch slide envelope
			pEnv  = Env([34,34,34],[0,0.4]).asArray;
			pc    = Control.names([\pitchEnv]).kr(pEnv.asArray);
			pitch = EnvGen.kr(pc, pGate, timeScale:0.5);

			// the LFO
			lfoOut       = SinOsc.kr(modFreq,0,1);
			lfoOutFilter = lfoOut*filterLFO;
			lfoOut       = lfoOut*modAmp;
			
			// the two pitches of the osc's
			pitch1=(lfoOut+(pitch+osc1Pitch)).midicps.clip(0,22050);
			pitch2=(lfoOut+(pitch+lfoOut+osc2Pitch)).midicps.clip(0,22050);
			
			// the saw (also used to make the linked pulse)
			// the link pulse, and the pulse 
			//out2 = SawDPW.ar((pitch1*linkOsc)+(pitch2*(1-linkOsc)),0);
			
			out2 = Saw.ar((pitch1*linkOsc)+(pitch2*(1-linkOsc)));
			out3 = ((out2>(0.5-pw))-0.5) * (osc1Amp*2);
			out1 = Pulse.ar(pitch1,pw,2,-1)*osc1Amp;
			
			// amp the saw because we have finished using it and mix together
			out  = (out2*(osc2Amp*3)) + Select.ar(linkOsc,[out1,out3]);
			
			// and leak any dc to stop filter noise (from pulse)
			out = LeakDC.ar(out);
			
			// work out the filter freq	
			filterResponse=(
					(pitch*kybd*2)
					+((1-kybd)*40)
					+Lag.kr(filterPitch,filterLag) // just lag the velocity
					+Lag.kr(filterFq,0.05)
					+lfoOutFilter
					+env2
				).round(steppingFilter).midicps.clip(0,22000);
			
			// apply the filter
			out = MoogFF.ar(out,filterResponse,q*3.98);
			
			// apply eq
			out=Select.ar(eq,[out,BHiShelf.ar(BLowShelf.ar(out, 114, 1, 10 ), 3905, 1, 12)]);
			
			// and the amp env
			out = out * ampEnv *  (qAmp*amp*0.75);
			
			// now send out
			Out.ar(outputChannels,Pan2.ar(out,pan));
			out = out*sendAmp;
			Out.ar(sendChannels,out.dup);
				
		}).send(server);
		
		// MoogLadder version

		SynthDef("BumNote_Ladder", {
			
			arg	outputChannels=0,	gate=1, 			amp=0.1,
				pw=0.5,			filterPitch=0,	filterFq=0,
				q=0.3,			qAmp=1,			filterEnv=1,
				modFreq=10, 		modAmp=0, 		filterLFO=0,
				osc1Amp=1,   		osc2Amp=0,
				osc1Pitch=0,		osc2Pitch=0,
				kybd=1,			pan=0,
				filterLag=0,		steppingFilter=0,
				sendAmp=0, 		sendChannels=4,
				pGate=0,			filterDrive=1,	eq=1,
				linkOsc=0;
				
			var	ampEnv, 			env2,			out,
			 	out1, 			out2, 			out3,
				lfoOut, 			lfoOutFilter, 	lfoOutPW,
			    	envctl, 			myEnv,
			    	envctl2, 			myEnv2, 			filterResponse,
			    	pEnv, 			pc, 				pitch=34,
			    	pitch1,			pitch2;
			
			// amp envelope
			myEnv  = Env.adsr(0.001, 2  , 0, 0.04,  1, -4);
			envctl = Control.names([\newAmpEnv]).kr( myEnv.asArray );
			ampEnv = EnvGen.ar(envctl,  gate);
			
			// filter envelope
			myEnv2 = Env.perc(0.001, 0.8, 1, -4);
			myEnv2.releaseNode_(2);
			envctl2= Control.names([\newFilterEnv]).kr( myEnv2.asArray );
			env2   = EnvGen.kr(envctl2, gate*2-1, filterEnv*70);

			// pitch slide envelope
			pEnv  = Env([34,34,34],[0,0.4]).asArray;
			pc    = Control.names([\pitchEnv]).kr(pEnv.asArray);
			pitch = EnvGen.kr(pc, pGate, timeScale:0.5);

			// the LFO
			lfoOut       = SinOsc.kr(modFreq,0,1);
			lfoOutFilter = lfoOut*filterLFO;
			lfoOut       = lfoOut*modAmp;
			
			// scale amp to filterDrive before filter
			osc1Amp=osc1Amp/filterDrive;
			osc2Amp=osc2Amp/filterDrive;
			
			// the two pitches of the osc's
			pitch1=(lfoOut+(pitch+osc1Pitch)).midicps.clip(0,22050);
			pitch2=(lfoOut+(pitch+lfoOut+osc2Pitch)).midicps.clip(0,22050);
			
			// the saw (also used to make the linked pulse)
			// the link pulse, and the pulse 
			out2 = Saw.ar((pitch1*linkOsc)+(pitch2*(1-linkOsc)));
			out3 = ((out2>(0.5-pw))-0.5) * (osc1Amp*2);
			out1 = Pulse.ar(pitch1,pw,2,-1)*osc1Amp;
			
			// amp the saw because we have finished using it and mix together
			out  = (out2*(osc2Amp*3)) + Select.ar(linkOsc,[out1,out3]);
			
			// and leak any dc to stop filter noise (from pulse)
			out = LeakDC.ar(out);
			
			// work out the filter freq	
			filterResponse=(
					(pitch*kybd*2)
					+((1-kybd)*40)
					+Lag.kr(filterPitch,filterLag) // just lag the velocity
					+Lag.kr(filterFq,0.05)
					+lfoOutFilter
					+env2
				).round(steppingFilter).midicps.clip(0,22000);
				
			// apply the filter
			out = MoogLadder.ar(out,filterResponse,(q*1.128)**4);
			
			// apply eq
			out=Select.ar(eq,[out,BHiShelf.ar(BLowShelf.ar(out, 114, 1, 10 ), 3905, 1, 12)]);
			
			// and the amp env
			out = out * ampEnv * ((q+1) * (qAmp*amp*filterDrive*0.75));
			
			// now send out
			Out.ar(outputChannels,Pan2.ar(out,pan));
			out = out*sendAmp;
			Out.ar(sendChannels,out.dup);
			
		}).send(server);
				
		SynthDef("BumNote_BFM1_lp", {
			
			arg	outputChannels=0,	gate=1, 			amp=0.1,
				pw=0.5,			filterPitch=0,	filterFq=0,
				q=0.3,			qAmp=1,			filterEnv=1,
				modFreq=10, 		modAmp=0, 		filterLFO=0,
				osc1Amp=1,   		osc2Amp=0,
				osc1Pitch=0,		osc2Pitch=0,
				kybd=1,			pan=0,
				filterLag=0,		steppingFilter=0,
				sendAmp=0, 		sendChannels=4,
				pGate=0,			filterDrive=1,	eq=1,
				linkOsc=0;
				
			var	ampEnv, 			env2,			out,
			 	out1, 			out2, 			out3,
				lfoOut, 			lfoOutFilter, 	lfoOutPW,
			    	envctl, 			myEnv,
			    	envctl2, 			myEnv2, 			filterResponse,
			    	pEnv, 			pc, 				pitch=34,
			    	pitch1,			pitch2;
			
			// amp envelope
			myEnv  = Env.adsr(0.001, 2  , 0, 0.04,  1, -4);
			envctl = Control.names([\newAmpEnv]).kr( myEnv.asArray );
			ampEnv = EnvGen.ar(envctl,  gate);
			
			// filter envelope
			myEnv2 = Env.perc(0.001, 0.8, 1, -4);
			myEnv2.releaseNode_(2);
			envctl2= Control.names([\newFilterEnv]).kr( myEnv2.asArray );
			env2   = EnvGen.kr(envctl2, gate*2-1, filterEnv*70);

			// pitch slide envelope
			pEnv  = Env([34,34,34],[0,0.4]).asArray;
			pc    = Control.names([\pitchEnv]).kr(pEnv.asArray);
			pitch = EnvGen.kr(pc, pGate, timeScale:0.5);

			// the LFO
			lfoOut       = SinOsc.kr(modFreq,0,1);
			lfoOutFilter = lfoOut*filterLFO;
			lfoOut       = lfoOut*modAmp;
			
			// scale amp to filterDrive before filter
			osc1Amp=osc1Amp/filterDrive;
			osc2Amp=osc2Amp/filterDrive;
			
			// the two pitches of the osc's
			pitch1=(lfoOut+(pitch+osc1Pitch)).midicps.clip(0,22050);
			pitch2=(lfoOut+(pitch+lfoOut+osc2Pitch)).midicps.clip(0,22050);
			
			// the saw (also used to make the linked pulse)
			// the link pulse, and the pulse 
			out2 = Saw.ar((pitch1*linkOsc)+(pitch2*(1-linkOsc)));
			out3 = ((out2>(0.5-pw))-0.5) * (osc1Amp*2);
			out1 = Pulse.ar(pitch1,pw,2,-1)*osc1Amp;
			
			// amp the saw because we have finished using it and mix together
			out  = (out2*(osc2Amp*3)) + Select.ar(linkOsc,[out1,out3]);
			
			// and leak any dc to stop filter noise (from pulse)
			out = LeakDC.ar(out);
			
			// work out the filter freq
			filterResponse=(
					(pitch*kybd*2)
					+((1-kybd)*40)
					+Lag.kr(filterPitch,filterLag) // just lag the velocity
					+Lag.kr(filterFq,0.05)
					+lfoOutFilter
					+env2
				).round(steppingFilter).midicps.clip(0,22000);
				
			// apply the filter
			out = DFM1.ar(out,filterResponse,q*1.015,0.25,0);
			
			// apply eq
			out=Select.ar(eq,[out,BHiShelf.ar(BLowShelf.ar(out, 114, 1, 10 ), 3905, 1, 12)]);
			
			// and the amp env
			out = out * ampEnv * ((q+1) * (qAmp*amp*filterDrive*0.75));
			
			// now send out
			Out.ar(outputChannels,Pan2.ar(out,pan));
			out = out*sendAmp;
			Out.ar(sendChannels,out.dup);
			
		}).send(server);
		
		SynthDef("BumNote_BFM1_hp", {
			
			arg	outputChannels=0,	gate=1, 			amp=0.1,
				pw=0.5,			filterPitch=0,	filterFq=0,
				q=0.3,			qAmp=1,			filterEnv=1,
				modFreq=10, 		modAmp=0, 		filterLFO=0,
				osc1Amp=1,   		osc2Amp=0,
				osc1Pitch=0,		osc2Pitch=0,
				kybd=1,			pan=0,
				filterLag=0,		steppingFilter=0,
				sendAmp=0, 		sendChannels=4,
				pGate=0,			filterDrive=1,	eq=1,
				linkOsc=0;
				
			var	ampEnv, 			env2,			out,
			 	out1, 			out2, 			out3,
				lfoOut, 			lfoOutFilter, 	lfoOutPW,
			    	envctl, 			myEnv,
			    	envctl2, 			myEnv2, 			filterResponse,
			    	pEnv, 			pc, 				pitch=34,
			    	pitch1,			pitch2;
			
			// amp envelope
			myEnv  = Env.adsr(0.001, 2  , 0, 0.04,  1, -4);
			envctl = Control.names([\newAmpEnv]).kr( myEnv.asArray );
			ampEnv = EnvGen.ar(envctl,  gate);
			
			// filter envelope
			myEnv2 = Env.perc(0.001, 0.8, 1, -4);
			myEnv2.releaseNode_(2);
			envctl2= Control.names([\newFilterEnv]).kr( myEnv2.asArray );
			env2   = EnvGen.kr(envctl2, gate*2-1, filterEnv*70);

			// pitch slide envelope
			pEnv  = Env([34,34,34],[0,0.4]).asArray;
			pc    = Control.names([\pitchEnv]).kr(pEnv.asArray);
			pitch = EnvGen.kr(pc, pGate, timeScale:0.5);

			// the LFO
			lfoOut       = SinOsc.kr(modFreq,0,1);
			lfoOutFilter = lfoOut*filterLFO;
			lfoOut       = lfoOut*modAmp;
			
			// scale amp to filterDrive before filter
			osc1Amp=osc1Amp/filterDrive;
			osc2Amp=osc2Amp/filterDrive;
			
			// the two pitches of the osc's
			pitch1=(lfoOut+(pitch+osc1Pitch)).midicps.clip(0,22050);
			pitch2=(lfoOut+(pitch+lfoOut+osc2Pitch)).midicps.clip(0,22050);
			
			// the saw (also used to make the linked pulse)
			// the link pulse, and the pulse 
			out2 = Saw.ar((pitch1*linkOsc)+(pitch2*(1-linkOsc)));
			out3 = ((out2>(0.5-pw))-0.5) * (osc1Amp*2);
			out1 = Pulse.ar(pitch1,pw,2,-1)*osc1Amp;
			
			// amp the saw because we have finished using it and mix together
			out  = (out2*(osc2Amp*3)) + Select.ar(linkOsc,[out1,out3]);
			
			// and leak any dc to stop filter noise (from pulse)
			out = LeakDC.ar(out);
			
			// work out the filter freq	
			filterResponse=(
					(pitch*kybd*2)
					+((1-kybd)*40)
					+Lag.kr(filterPitch,filterLag) // just lag the velocity
					+Lag.kr(filterFq,0.05)
					+lfoOutFilter
					+env2
				).round(steppingFilter).midicps.clip(0,22000);
				
			// apply the filter
			out = DFM1.ar(out,filterResponse,q*1.015,0.25,1);
			
			// apply eq
			out=Select.ar(eq,[out,BHiShelf.ar(BLowShelf.ar(out, 114, 1, 10 ), 3905, 1, 12)]);
			
			// and the amp env
			out = out * ampEnv * ((q+1) * (qAmp*amp*filterDrive*0.75));
			
			// now send out
			Out.ar(outputChannels,Pan2.ar(out,pan));
			out = out*sendAmp;
			Out.ar(sendChannels,out.dup);
			
		}).send(server);


	}
		
	startDSP{
		synth = Synth.head(instGroup, (#["BumNote_FF","BumNote_Ladder","BumNote_BFM1_lp","BumNote_BFM1_hp"][p[37]]), [\gate, 0]);
		node  = synth.nodeID;	
	}
		
	stopDSP{ synth.free }
	
	// need 2 do sendAmp, sendChannels, outputChannels
	
	updateDSP{|oldP,latency|
		var env,val,amp=1;
		
		oldP=oldP ? p;
		
		if (p[37]!=oldP[37]) {
			this.stopDSP;
			this.startDSP;
		};
		
		val=p[3];
		
		#amp,val=this.getQandAmp(val);
		
		server.sendBundle(latency,
			[\n_set, node, \q,             val               ],
			[\n_set, node, \qAmp,          amp               ],
			[\n_set, node, \filterEnv,     (1-(p[17]*2))*p[4]],
			[\n_set, node, \pw,            0.5- p[5]         ],
			[\n_set, node, \amp,           p[2].dbamp        ],
			[\n_set, node, \filterLag,     p[32]**2          ],
			[\n_set, node, \modFreq,       p[6]**2.5         ],
			[\n_set, node, \modAmp,        (p[7]**1.75)*4    ],
			[\n_set, node, \osc1Amp,       (1- p[8])**0.5    ],
			[\n_set, node, \osc2Amp,       p[8]**0.5         ],
			[\n_set, node, \osc1Pitch,     (p[13]+p[14])/(p[11]+1)*12],
			[\n_set, node, \osc2Pitch,     (p[15]+p[16])/(p[11]+1)*12],
			[\n_set, node, \filterLFO,     p[10]*1.5         ],
			[\n_set, node, \filterFq,      p[9]              ],
			[\n_set, node, \kybd,          p[18]             ],
			[\n_set, node, \pan,           p[26]             ],
			[\n_set, node, \steppingFilter,p[36]**1.5        ],
			[\n_set, node, \sendAmp       ,p[31].dbamp       ],
			[\n_set, node, \sendChannels  ,p[30]*2           ],
			[\n_set, node, \outputChannels,this.instGroupChannel          ],
			[\n_set, node, \eq            ,p[41]             ],
			[\n_set, node, \filterDrive   ,[5,1,0.45].clipAt(p[42].asInt)     ],
			[\n_set, node, \linkOsc       ,p[43]             ]
		);
	
		this.instOutChannel_(p[29]*2,latency);
		
		env=Env.adsr(p[20], p[21], p[22], p[23], 1, -4).asArray;
		server.sendBundle(latency,[\n_setn, node, \newAmpEnv, env.size] ++ env);
		env=Env.perc(p[24]**2, p[25], 1, -4);
		env.releaseNode_(2);
		env=env.asArray;
		server.sendBundle(latency,[\n_setn, node, \newFilterEnv, env.size] ++ env);

	}
	
	// the sequenced paramemters
	
	acidGateOn{|vel|	
		server.sendBundle(studio.actualLatency,[\n_set, node, \gate, 1]);
		{gui[\noteOnLamp].on}.defer(studio.actualLatency);
	}
	
	acidGateOff{|vel|
		server.sendBundle(studio.actualLatency-0.002,
						[\n_set, node, \gate, 0]); // slightly early before next note
		{gui[\noteOnLamp].off}.defer(studio.actualLatency-0.002);
	}
	
	// change the note

	acidNote{|note,slide=0,latency|
		var npoNote,env;
		if (findingNote.not) {
			
			npoNote=note-p[12]/(p[11]+1)*12+p[12];
			
			pitchStart=pitchEnd;
			pitchEnd=npoNote;
			pitchEnv=1 - pitchEnv;
						
			pitchEnvArray.put(0,pitchStart);
			pitchEnvArray.put(4,pitchStart);
			pitchEnvArray.put(8,pitchEnd);
			pitchEnvArray.put(9,p[19]*slide);
			server.sendBundle(latency,
				[\n_setn , node, \pitchEnv, 12] ++ pitchEnvArray, // 12 is array size
				[\n_set, node, \pGate , 1]);
			
			if (latency.isNil) {
				{server.sendBundle(nil, [\n_set, node, \pGate , 0]); nil;}.sched(0.02);
			}{
				server.sendBundle(latency+0.02, [\n_set, node, \pGate , 0]);
			};
			
			
		};
	}
	
	acidFilterPitch{|pitch|
		pitch=pitch.map(0,127,-45,65);
		lastFilterPitch=pitch;
		server.sendBundle(studio.actualLatency,[\n_set, node, \filterPitch, pitch*p[33]]);
	}
	
	////////
	
	stopAllNotes{
		var toDelete;
		server.sendBundle(studio.actualLatency+0.02,
				[\n_set, node, \gate, 0]); // slightly later to catch all
		notesOn=[];
		velocitysOn=[];
		{gui[\noteOnLamp].off}.defer(studio.actualLatency+0.02);
		{
			gui[\keyboardView].clear;
			lastNotePlayedBySeq=nil;
		}.defer(studio.actualLatency+0.02);
	}
	
	// need to add a midi out latency option for other //////////////////
	// internal sequencers using internal midi ports
	
	// from midiIn
	noteOn{|note, vel,latency|
		if ((note<p[27])or:{note>p[28]}) {^nil}; // drop out if out of midi range
		if (instOnSolo.isOn) {
			this.note_ON(note,vel,latency);
			{gui[\keyboardView].setColor(note,Color(0.5,0.5,1),1);}.defer;
		}
	}
	
	// from internal seq
	// this is only note change not a note on event!!!
	midiNote_Seq{|note,slide|
		var toRemove;
		this.acidNote(note,slide,studio.actualLatency);	
		toRemove=lastNotePlayedBySeq;
		{
			if (toRemove.notNil) {
				gui[\keyboardView].removeColor(toRemove);
			};
			gui[\keyboardView].setColor(note,Colour(1,0.5,0),1);
		}.defer(studio.actualLatency);
		lastNotePlayedBySeq=note;
	}
	
	// from internal seq
	noteOn_Seq{
		var note;
		this.acidGateOn;
		note=lastNotePlayedBySeq;
		{
			gui[\keyboardView].setColor(note,Colour(1,0.5,0),1);
		}.defer(studio.actualLatency);	
	}
	
	// from gui keyboard
	noteOn_Key{|note,vel|
		if (instOnSolo.isOn) {
			this.note_ON(note,vel,nil)
		}
	}
	
	// actual note_ON method for midi and gui keyboard
	note_ON{|note, vel,latency|
		var x,i,toRemove;
	
		server.sendBundle(latency,[\n_set, node, \gate, 1]);
			
		this.acidNote(note,notesOn.size.sign,latency);
		
		toRemove=lastNotePlayedBySeq;
		{
			if (toRemove.notNil) {
				gui[\keyboardView].removeColor(toRemove);
			};
			gui[\keyboardView].setColor(note,Colour(1,0.5,0),1);
		}.defer(studio.actualLatency);
		
		
		lastNotePlayedBySeq=note;
		
		lastFilterPitch=vel.map(0,127,-45,65);
		server.sendBundle(latency,[\n_set, node, \filterPitch,lastFilterPitch*p[33]]);
		
		if ((isRecording)and:{notesOn.size==0}and:{p[34]==1}) {
			x=(recordIndex/sP[0][6]).asInt%(sP[0][3]);
			this.setSeq(0,x,1);
			seqModels[0][x].lazyValue_(1);			
		};
		
		if (notesOn.includes(note).not) {
			notesOn=notesOn.add(note);
			velocitysOn=velocitysOn.add(vel);
		};
		{gui[\noteOnLamp].on}.defer;
		
		if (isRecording) {
			if (p[34]==1) {
				x=(recordIndex/sP[2][6]).asInt%(sP[2][3]);
				this.setSeq(2,x,note);
				seqModels[2][x].lazyValue_(note);
			};		
		}{
			i=gui[\lastSeqIndex];
			if ((isKeyboardWriting)and:{p[34]==1}) {
				if (i.notNil) {
					{
						seqModels[2][i].value_(note);
						this.setSeq(2,i,note);
						
						
						if (p[40]==1) {
							i=(i+1).wrap(0,sP[2][3]);
						};
						this.setNoteDisplay(note,i);
						
					}.defer;
				};
			}{
				{this.setNoteDisplay(note,i);}.defer;
			};
		
		};
		
	}
	
	// from midiIn
	noteOff{|note, vel,latency|
		if ((note<p[27])or:{note>p[28]}) {^nil}; // drop out if out of midi range
		if (instOnSolo.isOn) {
			this.note_OFF(note,vel,latency);
			{gui[\keyboardView].removeColor(note);}.defer;
		}
	}
	
	// from internal seq
	noteOff_Seq{|note,vel|
		var toRemove;
		this.acidGateOff;	
		toRemove=lastNotePlayedBySeq;
		{
			if (toRemove.notNil) {
				gui[\keyboardView].removeColor(toRemove);
			};
		}.defer(studio.actualLatency-0.05);	
	}
	
	// from gui keyboard
	noteOff_Key{|note,vel|
		if (instOnSolo.isOn) {
			this.note_OFF(note,vel,nil)
		}
	}
	
	note_OFF	{|note, vel,latency|
		var x,npoNote;
				
		if (notesOn.includes(note)) {
			velocitysOn.removeAt(notesOn.indexOf(note));
			notesOn.remove(note);
			if (notesOn.notEmpty) {
				note=notesOn.last;
				npoNote=note-p[12]/(p[11]+1)*12+p[12];
				this.acidNote(npoNote,1,latency);
				server.sendBundle(latency,[\n_set, node, \filterPitch,
					(velocitysOn.last).map(0,127,-45,65)*p[33]
				]);
				
				if ((isRecording)and:{p[34]==1}) {
					x=(recordIndex/sP[2][6]).asInt%(sP[2][3]);
					this.setSeq(2,x,note);
					seqModels[2][x].lazyValue_(note);
				};
				
			}{
				server.sendBundle(latency,[\n_set, node, \gate, 0]);
				{gui[\noteOnLamp].off}.defer;
			};
		};
		
		if ((isRecording)and:{notesOn.size==0}and:{p[34]==1}) {
			x=(recordIndex/sP[1][6]).asInt%(sP[1][3]);
			this.setSeq(1,x,1);
			seqModels[1][x].lazyValue_(1);			
		};
	
	}
	
	/// midi Clock ////////////////////////////////////////
	
	//clockIn is the clock pulse, with the current song pointer in beats
	clockIn   {|beat|
		channels.do({|y|
			var vel,pos,note,speed,dur;
			speed=sP[y][6];
			if ((beat%speed)==0) {
				pos=((beat/speed).asInt)%(sP[y][3]);
				vel=seq[y][pos];
				if (instOnSolo.onOff==1) {
					switch (y)
						{0} { if (vel> 0) { this.noteOn_Seq           } }
						{1} { if (vel> 0) { this.noteOff_Seq          } }
						{2} { if (vel>=0) { this.midiNote_Seq(vel,seq[4][pos]) } }
										//also for keyboard
						{3} { if (vel>=0) { this.acidFilterPitch(vel) } };
				};
				{posModels[y].lazyValue_(pos)}.defer(studio.actualLatency);
			};
		});
		if (isRecording) {
			{recordIndex=beat;nil}.sched(studio.actualLatency);
		};
	}	
	
	clockPlay { }		//play and stop are called from both the internal and extrnal clock
	clockStop {
		channels.do({|y|
			{posModels[y].lazyValue_(0)}.defer(studio.actualLatency); // reset gui counter positions
		});
		this.stopAllNotes;
	}	
	clockPause{ this.stopAllNotes }		// pause only comes the internal clock

	/// i have down timed the clock by 3, so this gives 32 beats in a 4x4 bar
	/// this is all the resoultion i need at the moment but could chnage in the future
	
	// this is called by the studio for the zeroSL auto map midi controller
	autoMap{|num,val|
		var vPot;
		vPot=(val>64).if(64-val,val);
	}

} // end ////////////////////////////////////
