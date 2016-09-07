// ********************************************************************************************** //
// Control your RolandJP08(s) with this. BOOM!                                                    //
// use JP-08 in Chain mode [MANUAL] + [9] = [1] (OFF) [2] (ON)                                    //
// ********************************************************************************************** //

LNX_RolandJP08 : LNX_InstrumentTemplate {

	classvar <rolandPresets, <>isVisiblePref, <rolandPresetDict;

	var <sequencer, <keyboardView, <noControl, <midiInBuffer, <midiOutBuffer, <seqOutBuffer;
	var <lastProgram, <midi2, <sysex2Time;

	// init & info ///////////////////////////////////////////////////////

	*initClass{
		Class.initClassTree(LNX_File);
		// load in all the preferences
		isVisiblePref = ("RolandIsVisible".loadPref ? [false])[0].isTrue;
		// how shall i insert the factory defaults here?
		rolandPresets = "Roland Presets".loadPref ?? { (8*8).collect{|i| "" } };
		rolandPresetDict = IdentityDictionary[];
		"Roland Preset Dict".loadPref.do{|string|
			var list = string.interpret;
			rolandPresetDict[list[0].asInt] = list[1..];
		};
	}

	*saveIsVisiblePref{ [isVisiblePref].savePref("RolandIsVisible") }

	*isVisible{^isVisiblePref}

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	// properties
	*studioName      {^"Roland JP-08"}
	*sortOrder       {^2}
	isInstrument     {^true}
	canBeSequenced   {^true}
	isMixerInstrument{^true}
	mixerColor       {^Color(1, 0.75, 0.5)} // colour in mixer
	hasLevelsOut     {^true}
	hasMIDIClock     {^true}

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
		instrumentHeaderType="SC RolandJP08 Doc";
		version="v1.0";
	}

	// an immutable list of methods available to the network
	interface{^#[\netMidiControlVP, \netExtCntIn, \extProgIn, \netPipeIn]}

	iInitVars{

		midi2 = LNX_MIDIPatch(-1,0,-1,0);  // for control / sysex data
		sysex2Time = IdentityDictionary[]; // last time data sent, to prevent feedback

		// the main sequencer
		sequencer = LNX_PianoRollSequencer(id++\pR)
			.pipeOutAction_{|pipe|
				if (((p[13]>0)&&(this.isOff)).not) {seqOutBuffer.pipeIn(pipe)};
			}
			.releaseAllAction_{ seqOutBuffer.releaseAll(studio.actualLatency)  }
			.keyDownAction_{|me, char, modifiers, unicode, keycode|
				keyboardView.view.keyDownAction.value(me,char, modifiers, unicode, keycode)
			}
			.keyUpAction_{|me, char, modifiers, unicode, keycode|
				keyboardView.view.keyUpAction.value(me, char, modifiers, unicode, keycode)
			}
			.recordFocusAction_{ keyboardView.focus };
	}

	// the models ///////////////////////////////////////////////////////

	initModel {

		var template=[

			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],

			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me|	this.onOffAlt(me.value) }],

			// 2.master amp
			[\db6,midiControl, 2, "Master volume",
				(\label_:"Volume" , \numberFunc_:'db',mouseDownAction_:{hack[\fadeTask].stop}),
				{|me,val,latency,send,toggle|
					this.setPVPModel(2,val,0,send);             // set p & network model via VP
					this.setMixerSynth(\amp,val.dbamp,latency); // set mixer synth
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
					this.setPVPModel(4,val,0,send);     // set p & network model via VP
					this.setMixerSynth(\outChannel,channel,latency); // set mixer synth
				}], // test on network

			// 5.master pan
			[\pan, midiControl, 5, "Pan",
				(\numberFunc_:\pan, \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(5,val,0,send);      // set p & network model via VP
					this.setMixerSynth(\pan,val,latency); // set mixer synth
				}],

			// 6. peak level
			[0.7, \unipolar,  midiControl, 6, "Peak Level",
				{|me,val,latency,send| this.setPVP(6,val,latency,send) }],

			// 7. send channel
			[-1,\audioOut, midiControl, 7, "Send channel",
				(\label_:"Send", \items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					var channel = LNX_AudioDevices.getOutChannelIndex(val);
					this.setPVPModel(7,val,0,send);             // set p & network model via VP
					this.setMixerSynth(\sendChannel,channel,latency); // set mixer synth
				}],

			// 8. sendAmp
			[-inf,\db6,midiControl, 8, "Send amp", (label_:"Send"),
				{|me,val,latency,send,toggle|
					this.setPVPModel(8,val,0,send);             // set p & network model via VP
					this.setMixerSynth(\sendAmp,val.dbamp,latency); // set mixer synth
				}],

			// 9. channelSetup
			[0,[0,4,\lin,1], midiControl, 9, "Channel Setup",
				(\items_:["Left & Right","Left + Right","Left","Right","No Audio"]),
				{|me,val,latency,send|
					this.setSynthArgVH(9,val,\channelSetup,val,latency,send);
				}],

			// 10. syncDelay
			[\sync, {|me,val,latency,send|
				this.setPVPModel(10,val,latency,send);
				this.syncDelay_(val);
			}],

			// 11.network keyboard
			[0, \switch, midiControl, 11, "Network", (strings_:["Net"]),
				{|me,val,latency,send|	this.setPVH(11,val,latency,send) }],

			// 12. preset
			[0,[1,64,\lin,1], midiControl, 12, "Preset",{|me,value,latency,send,toggle|
				this.setODModel(12,value,latency,send); // network must come 1st
				this.setRolandProgram(latency);         // send out to the Roland Module
				this.loadRolandPresetDict(value);       // make the gui match the Roland
			}],

			// 13. onSolo turns audioIn, seq or both on/off
			[1, [0,2,\lin,1],  midiControl, 13, "On/Off Model",
				(\items_:["Audio In","Sequencer","Both"]),
				{|me,val,latency,send|
					this.setPVP(13,val,latency,send);
					this.updateOnSolo;
				}],

			// 14. x clock out
			[0, \switch, midiControl, 14, "MIDI Clock",
					(strings_:["MIDI Clock"]),
					{|me,val,latency,send|
						this.setPVPModel(14,val,latency,send);
						if (val.isFalse) {
							//">>> Out Stop >>> ".post; [latency +! syncDelay].postln;
							midi.stop(latency +! syncDelay)
						};
					}],

			// 15. use controls in presets
			[1, \switch, midiControl, 15, "Controls Preset",
					(strings_:["Controls"]),
					{|me,val,latency,send|
						this.setPVPModel(15,val,latency,send);
						if (val.isTrue) {
							presetExclusion=[0,1,10,14];
						}{
							presetExclusion=[0,1,10,14]++((1..RolandJP08.size)+16);
						}
					}],

			// 16. use program in presets
			[1, \switch, midiControl, 16, "Program",
					(strings_:["Program"]),
					{|me,val,latency,send|	this.setPVP(16,val,latency,send);}],

		];

		// 17-?: add all the RolandJP08 controls
		noControl = RolandJP08.size;

		noControl.do{|i|
			var spec = RolandJP08.specAt(i);
			template= template.add(
				[spec.asSpec.default ? 0, spec, midiControl, i+17, RolandJP08.nameAt(i),
				(label_:(RolandJP08.labelAt(i)),numberFunc_:(RolandJP08.numberFuncAt(i))),
				{|me,val,latency| this.midiControlVP(i,val,latency) }]);
		};

		// 60. oct VCO2
		template = template.add(
			[0, \switch, midiControl, 60, "VCO2 Oct",
					(strings_:["Oct"]),
					{|me,val,latency,send|	this.setPVP(60,val,latency,send);}],
		);

		#models,defaults=template.generateAllModels;

		// adjust VCO2 so it quants to the Oct if model[60] is on
		models[29].action_{|me,val,latency|
			var newVal = val;
			if (p[60].isTrue) { newVal = val.nearestInList([1, 32, 92, 166, 224, 255]) };
			if (thisThread.clock==SystemClock) {
				if (newVal!=val) { me.value_(newVal) };
			}{
				{if (newVal!=val) { me.value_(newVal) }}.defer;
			};
			this.midiControlVP(12,newVal,latency);
		};

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,10,14];
		randomExclusion=[0,1,10,14];
		autoExclusion=[10,12];

	}

	getMixerArgs{^[
		[\amp,           p[ 2].dbamp       ],
		[\outChannel,    LNX_AudioDevices.getOutChannelIndex(p[4])],
		[\pan,           p[5]             ],
		[\sendChannel,  LNX_AudioDevices.getOutChannelIndex(p[7])],
		[\sendAmp,       p[8].dbamp       ]
	]}

	// MIDI patching ///////////////////////////////////////////////////////

	// any post midiInit stuff
	iInitMIDI{
		midi.findByName("Boutique","Boutique");
		this.useMIDIPipes;
		midiOutBuffer = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.toMIDIPipeOut(pipe) };
		midiInBuffer  = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromInBuffer(pipe)  };
		seqOutBuffer  = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromSequencerBuffer(pipe) };
		midi2.sysexFunc = {|src, data, latency| this.sysex2(data, latency) };
	}

	midiSelectProgram{} // this needs to be disabled for everything

	// midi pipe in. This is the future
	pipeIn{|pipe|
		// exceptions
		if (instOnSolo.isOff and: {p[13]>0} ) {^this}; // drop if sequencer off
		if (pipe.historyIncludes(this))       {^this}; // drop to prevent internal feedback loops

		switch (pipe.kind)
			{\control} {  // if control
				^this    // drop
			}
			{\program} { // if program
				api.groupCmdOD(\extProgIn, pipe.program+1,false); // network prog change
				^this   // & drop
			};

		// network if needed
		if ((p[11].isTrue) and:
			{#[\external, \controllerKeyboard, \MIDIIn, \keyboard].includes(pipe.source)}) {
				if (((pipe.source==\controllerKeyboard) and:{studio.isCntKeyboardNetworked}).not)
					{ api.sendOD(\netPipeIn, pipe.kind, pipe.note, pipe.velocity)}
		};

		midiInBuffer.pipeIn(pipe); // to in Buffer. (control & progam are dropped above)
	}

	// networked midi pipes
	netPipeIn{|type,note,velocity|
		switch (type.asSymbol)
			{\noteOn } { this.pipeIn(LNX_NoteOn(note,velocity,nil,\network)) }
			{\noteOff} { this.pipeIn(LNX_NoteOff(note,velocity,nil,\network)) }
		;
	}

	// sysex in from the JP08 physical MIDI Out port, not the USB MIDI port
	// to prevent feedback ouput chokes input for 1 second
	sysex2{|data, latency|
		var index, key, value;

		// if (true) {^this}; // temp disable

		// exceptions
		if (data.size!=16)    {^this};          // packet size exception
		if (data[1]!=65)      {^this};          // Roland SYSEX number
		if (data[6]!=28)      {^this};          // JP-08 product code
		if (data[2]!=16)      {^this};          // not this device, should stop feedback
		if (data[8]!=3)       {^this};          // unsure, maybe set value command?
		if (data[0]!=(-16))   {^this};          // first byte
		if (data[15]!=(-9))   {^this};          // last byte

		key   = (data[10]*25) + data[11];       // key to metadata in RolandJP08 dictionary
		value = (data[12]*16) + data[13];       // 8 bit value (0-255)
		index = RolandJP08.indexOfKey(key);     // index of model

		if (index.isNil)      {^this};          // key not found

		// feedback exception, not needed if we different device IDs, to check with new jp
		if (sysex2Time[index].notNil and: { SystemClock.now < sysex2Time[index] })
			{ ^this }                          // exception
			{ sysex2Time[index] = nil };       // else clear time

		//"<<< In Sysex  <<< ".post; data.postln;

		models[(index+17)].lazyValue_(value, true); // set model, no action
		p[index+17]=value;                          // set p[]

		api.sendVP(\ccvp++index, \netExtCntIn, index+17, value);  // network it

	}

	// sysex out to the JP08 physical MIDI In port, not the USB MIDI port
	// to prevent feedback ouput chokes input for 1 second
	midiControlOut{|index,value,latency|
		var key      = RolandJP08.keyAt(index.asInt);
		var data     = Int8Array[ -16, 65, 16, 0, 0, 0, 28, 18, 3, 0, key.div(25), key%25,
											value.div(16), value.asInt%16, 0, -9 ];

		data[14] = RolandJP08.checkSum(data[8..13]); // put in Roland checksum

		//">>> Out Sysex >>> ".post; data.postln;

		midi.sysex(data,latency +! syncDelay);       // midi control out

		// to prevent feedback, when can i start listening again?
		sysex2Time[index] =
			SystemClock.now + ((latency +! syncDelay) ? 0) + (studio.midiSyncLatency) + 1;
			// not sure how the sync works here

	}

/*
[F0, 41 (Roland SYSEX number), 10 (device ID), 00 (modelID),00,00,1C�(Product code for the JP-08), 12, 03, 00, address MSB, address LSB, value MSB, value LSB, checksum, F7]

checksum for byte @ index 14 is...
RolandJP08.checkSum(Int8Array[ -16, 65, 16, 0, 0, 0, 28, 18, 3, 0, 1, 18, 15, 13,  78, -9 ][8..13]);
Int8Array[ -16, 65, 16, 0, 0, 0, 28, 18, 3, 0, 1, 18, 15, 13,  78, -9 ].size
*/

	// import a complete folder of roland patches
	importFolder{
		Dialog.openPanel({|files|
			files = files.select{|path|
				(path.basename[..9] == "JP08_PATCH")&&(path.extension=="PRM")
			};
			files.do{|path|
				var index = (path.basename.drop(10).drop(-4).asInt);
				this.dropFile(index,path);
			}
		},multipleSelection:true)
	}

	// drag and drop a Roland PATCH.PRM file into the preset
	dropFile{|i,path|
		var file,loadList;

		// file type / name exceptions
		if (path.keep(-4)!=".PRM")      { "Not a .PRM file".postln;  ^this};
		if (path.contains("PATCH").not) { "Not a PATCH file".postln; ^this};

		// make a flat list contains pairs of keys & values
		file = path.loadList;
		loadList = [file.last[11..].drop(-2)]  ++ file.drop(-1).collect{|s|
			[ RolandJP08.keyOfName(s[0..15]),s[16..].interpret ]
		}.reject{|i| i[0].isNil}.flat;

		rolandPresetDict[i] = loadList; // store this list in the rolandPresetDict

		// update the name in the gui, index in gui starts at zero
		rolandPresets[i-1]=loadList.first;
		gui[i-1].string_(loadList.first);
		rolandPresets.savePref("Roland Presets"); // & save

		// and save rolandPresetDict. I know I have 2 pref files, this may change
		rolandPresetDict.collect{|value,key| ([key] ++ value).asCompileString }.asList
			.savePref("Roland Preset Dict");

	}

	// update gui to a preset from the preference dict
	loadRolandPresetDict{|i|
		i=i.asInt;
		if (rolandPresetDict[i].notNil) {
			rolandPresetDict[i].drop(1).pairsDo{|key,value|  // drop 1st item which is name
				var index = RolandJP08.indexOfKey(key);     // index of model
				models[(index+17)].lazyValue_(value, false); // set model, no action
				p[index+17]=value;                          // set p[]
			};
		};
	}

	// set control
	extCntIn{|item,value,latency|
		api.sendVP((id++"_ccvp_"++item).asSymbol,
			'netExtCntIn',item,value,midi.uidOut,midi.midiOutChannel);
	}

	// net version of above
	netExtCntIn{|item,value,uidOut,midiOutChannel|
		p[item+17]=value;
		models[item+17].lazyValue_(value,false);

		// need to do this via sysex on nrtwork

		// go on, do a pipe here
		midi.control(RolandJP08.keyAt(item) ,value,nil,false,true);
		// ignore set to true so no items learnt from this
	}

	// midi coming from in buffer
	fromInBuffer{|pipe|
		sequencer.pipeIn(pipe);                 // to the sequencer
		keyboardView.pipeIn(pipe,Color.orange); // to the gui keyboard
		// drop out and Don't send if pipe is external
		// and coming from RolandJP08 going to RolandJP08
		if ((pipe.source==\external) && {midi.outPoint.isSameDeviceAndName(pipe[\endPoint])}) {
			^this
		};
		this.toMIDIOutBuffer(pipe);             // to midi out buffer
	}

	// output from the sequencer buffer
	fromSequencerBuffer{|pipe|
		keyboardView.pipeIn(pipe,Color.orange); // to the gui keyboard
		this.toMIDIOutBuffer(pipe);             // and to the out buffer
	}

	// to the output buffer
	toMIDIOutBuffer{|pipe|
		if (instOnSolo.isOff and: {p[13]>0} ) {^this}; // drop if sequencer off
		midiOutBuffer.pipeIn(pipe);                    // to midi out buffer
	}

	// and finally to the midi out
	toMIDIPipeOut{|pipe|
		//">>> Out Pipe >>> ".post; pipe.postln;
		midi.pipeIn(pipe.addToHistory(this).addLatency(syncDelay));    // add this to its history
	}

	// release all played notes, uses midi Buffer
	stopAllNotes{
		midiInBuffer.releaseAll(studio.actualLatency);
		seqOutBuffer.releaseAll(studio.actualLatency);
		midiOutBuffer.releaseAll(studio.actualLatency);
		{keyboardView.clear}.defer(studio.actualLatency);
	}

	// external midi setting prog number (these are network methods)
	extProgIn{|prog,send=false|
		prog = models[12].controlSpec.constrain(prog).asInt; // use spec to constrain
		p[12] = prog;                                        // external
		models[12].lazyValue_(p[12],false);                  // false is no auto
		this.updatePresetGUI;
		if (send.isTrue) { this.setRolandProgram };

		this.loadRolandPresetDict(prog);
	}

	// gui stuff for the preset tab.
	updatePresetGUI{
		var prog=	p[12].asInt-1;
		{
			if (lastProgram.notNil) {
				gui[lastProgram].color_(\background,Color(0.14,0.12,0.11)*0.4);
				gui[1000+(lastProgram%8)].color_(\background,Color(0.14,0.12,0.11,0.25)*0.4);
				gui[2000+(lastProgram.div(8))].color_(\background,
					Color(0.14,0.12,0.11,0.25)*0.4);
			};
			gui[prog].color_(\background,Color(0.5,0.5,0.5,0.5));
			gui[1000+(prog%8)].color_(\background,Color(0.14,0.12,0.11)*0.4);
			gui[2000+(prog.div(8))].color_(\background,Color(0.14,0.12,0.11)*0.4);
			lastProgram = prog;
		}.defer;
	}

	// clock in //////////////////////////////

	// clock in for pRoll sequencer
	clockIn3{|beat,absTime,latency,absBeat| sequencer.do(_.clockIn3(beat,absTime,latency,absBeat))}

	// reset sequencers posViews
	clockStop {
		sequencer.do(_.clockStop(studio.actualLatency));
		seqOutBuffer.releaseAll(studio.actualLatency);
	}

	// remove any clock hilites
	clockPause{
		sequencer.do(_.clockPause(studio.actualLatency));
		seqOutBuffer.releaseAll(studio.actualLatency);
	}

	// clock in for midi out clock methods
	midiSongPtr{|songPtr,latency| if (p[14].isTrue) {
		//">>> Out songPtr >>> ".post; [songPtr,latency].postln;
		midi.songPtr(songPtr,latency +! syncDelay);
	}}
	midiStart{|latency|           if (p[14].isTrue) {
		//">>> Out start >>> ".post; [latency].postln;
		midi.start(latency +! syncDelay);
	} }
	midiClock{|latency|           if (p[14].isTrue) {
		//">>> Out midiClock >>> ".post; [latency].postln;
		midi.midiClock(latency +! syncDelay)
	} }
	midiContinue{|latency|        if (p[14].isTrue) {
		//">>> Out continue >>> ".post; [latency].postln;
		midi.continue(latency +! syncDelay)
	} }
	midiStop{|latency|            if (p[14].isTrue) {
		//">>> Out stop >>> ".post; [latency].postln;
		midi.stop(latency +! syncDelay)
	} }

	// disk i/o ///////////////////////////////

	// for your own saving
	iGetSaveList{ ^(midi2.getSaveList) ++ (sequencer.getSaveList) }

	// anything else that needs doing before a load
	preLoadP{|tempP|
		models[16].doLazyValueAction_(tempP[16],nil,false);
		// check send program  1st and then send
		if (models[16].isTrue) {
			models[12].doLazyValueAction_(tempP[12],nil,false); // THIS WILL LOAD PROGRAM
		};
		^tempP
	}

	// override insts template to stop select program / preset when loading
	// add 0.075 to latency to allow control to update
	// and this should really be called update models
	updateGUI{|tempP|
		tempP.do({|v,j|
			if (p[j]!=v) {
				if (j==12) {
					// dont do any actions on p== 12
					models[j].lazyValue_(v,false);
				}{
					if ((tempP[89].isFalse)&&(j>=17)) {
						models[j].lazyValue_(v,false); // don't do action
					}{
						models[j].lazyValueAction_(v,
							0.3 - syncDelay.clip(-inf,0) - studio.midiSyncLatency
						,send:false); // extra here as well
					}
				}
			}
		});
		this.iUpdateGUI(tempP);
	}

	// for your own loading
	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		midi2.putLoadList(l.popNI(4));
		sequencer.putLoadList(l.popEND("*** END OBJECT DOC ***"));
	}

	// anything else that needs doing after a load. all paramemters will be loaded by here
	iPostLoad{}

	// free this
	iFree{
		midiInBuffer.releaseAll;
		seqOutBuffer.releaseAll;
		midiOutBuffer.releaseAll;
		sequencer.do(_.free);
		midi2.free;
		midiInBuffer.free;
		midiOutBuffer.free;
		seqOutBuffer.free;
		sequencer = keyboardView = noControl = midiInBuffer = midiOutBuffer = seqOutBuffer =
		lastProgram = midi2 = sysex2Time = nil;
	}

	// PRESETS /////////////////////////

	// get the current state as a list
	iGetPresetList{ ^sequencer.iGetPresetList }

	// add a statelist to the presets
	iAddPresetList{|l| sequencer.iAddPresetList(l.popEND("*** END Score DOC ***").reverse) }

	// save a state list over a current preset
	iSavePresetList{|i,l| sequencer.iSavePresetList(i,l.popEND("*** END Score DOC ***").reverse) }

	// for your own load preset
	iLoadPreset{|i,newP,latency| sequencer.iLoadPreset(i) }

	// for your own remove preset
	iRemovePreset{|i| sequencer.iRemovePreset(i) }

	// for your own removal of all presets
	iRemoveAllPresets{ sequencer.iRemoveAllPresets }

	// clear the sequencer
	clearSequencer{ sequencer.clear }

	// roland jp08 stuff ///////////////////////***************************************************

	//  this is overriden to force program changes & exclusions by put p[15 & 16] 1st
	loadPreset{|i,latency|
		var oldP;
		var presetToLoad=  presetMemory[i].copy;
		var adjustedLatency;

		models[15].lazyValueAction_(presetToLoad[15],latency,false); // 89.use controls
		models[16].lazyValueAction_(presetToLoad[16],latency,false); // 16.use program
		presetExclusion.do{|i| presetToLoad[i]=p[i]};                // exclude these parameters

		// check send program  1st and then send
		if (models[16].isTrue) {
			models[12].doLazyValueAction_(presetToLoad[12],latency,false);
		};

		if (latency.isNil) {
			adjustedLatency = 0.1 - syncDelay.clip(-inf,0) - studio.midiSyncLatency
		}{
			adjustedLatency = latency + 0.075;
			// above updateGUI only works after 0.2 so why 0.075 here? because of out adjustments
		};

		if (p[15].isTrue) {
			// update models
			presetToLoad.do({|v,j|
				if (#[15,16,12].includes(j).not) {
					if (p[j]!=v) {
						models[j].lazyValueAction_(v,adjustedLatency,false)
					}
				};
			});
		};

		this.iLoadPreset(i,presetToLoad,latency);    // any instrument specific details

		oldP=p.copy;
		p=presetToLoad;               // copy the paramaters to p (is this needed any more?)
		this.updateDSP(oldP,latency); // and update any dsp

	}

	// set program on roland, latency isn't really needed here
	setRolandProgram{|latency|
		var prog=	p[12];
		//">>> Out program >>> ".post; [prog-1, latency +! syncDelay].postln;
		midi.program(prog-1, latency +! syncDelay); // out to midi
		this.updatePresetGUI;
	}

	// set control
	midiControlVP{|item,value,latency|
		p[item+17]=value;
		this.midiControlOut(item,value,latency);
		api.sendVP((id++"_ccvp_"++item).asSymbol,
					'netMidiControlVP',item,value,midi.uidOut,midi.midiOutChannel);
	}

	// net version of above
	netMidiControlVP{|item,value,uidOut,midiOutChannel|
		p[item+17]=value;
		models[item+17].lazyValue_(value,false);
		this.midiControlOut(item.asInt,value);
	}

	//////////////////////////*****************************************************
	// GUI

	*thisWidth  {^952}
	*thisHeight {^460}

	createWindow{|bounds|
		this.createTemplateWindow(bounds,Color(0.1221, 0.0297, 0.0297));
	}

	iPostNew{ { sequencer.resizeToWindow }.defer(0.02) }

	createWidgets{

		gui[\scrollTheme]=( \background	: Color(0.25, 0.25, 0.25), \border : Color.orange);

		gui[\plainTheme]=( colors_: (\on	: Color.black, \off : Color.black));

		gui[\plainTheme2]=( colors_: (\on : Color.orange, \off : Color.orange));

		gui[\scaleTheme]=( colors_: (\background : Color.clear, \marks : Color.white), marks_:7);
		gui[\onOffTheme1]=( \font_		: Font("Helvetica", 12,true),
						 \rounded_	: true,
						 \colors_     : (\on : Color(20/77,1,20/77), \off: Color(0.4,0.4,0.4)));

		gui[\onOffTheme2]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color(50/77,61/77,1), \off: Color(0.4,0.4,0.4)));

		gui[\onOffTheme3]=( \font_		: Font("Helvetica", 12,true),
						 \rounded_	: true,
						 \colors_     : (\on : Color.orange*0.75, \off: Color(0.4,0.4,0.4)));

		gui[\onOffTheme4]=( \font_		: Font("Helvetica", 12,true),
						 \rounded_	: true,
						 \colors_     : (\on : Color.orange, \off: Color(0.4,0.4,0.4)));

		gui[\soloTheme ]=( \font_		: Font("Helvetica", 12,true),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));


		gui[\menuTheme2]=( \font_		: Font("Arial", 10),
						\labelShadow_	: false,
						\colors_      : (\background: Color.orange*0.75,
									   \label:Color.black,
									   \string:Color.black,
									   \focus:Color.clear));

		gui[\theme2]=(	\orientation_  : \horiz,
						\resoultion_	 : 3,
						\visualRound_  : 0.001,
						\rounded_      : true,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color.orange*0.75,
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.black,
										\focus : Color(0,0,0,0)));

		// control strip ///////

		// 1. channel onOff
		MVC_OnOffView(models[1], window,Rect(185, 4, 26, 18),gui[\onOffTheme1])
			.permanentStrings_(["On","On"]);

		// 0. channel solo
		MVC_OnOffView(models[0], window, Rect(215, 4, 26, 18),gui[\soloTheme])
			.rounded_(true);

		// 13. onSolo turns audioIn, seq or both on/off
		MVC_PopUpMenu3(models[13],window,Rect(258,5,70,16), gui[\menuTheme2 ] );

		// 3. in
		MVC_PopUpMenu3(models[3],window,Rect(338,5,70,16), gui[\menuTheme2 ] );

		// 9. channelSetup
		MVC_PopUpMenu3(models[9],window,Rect(417,5,75,16), gui[\menuTheme2 ] );

		// MIDI Settings
 		MVC_FlatButton(window,Rect(502, 4, 43, 18),"MIDI")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color.orange*0.75)
			.color_(\down,Color.orange*0.75/2)
			.color_(\string,Color.white)
			.action_{ this.createMIDIInOutModelWindow(window,nil,nil,
				(border1:Color(0.1221, 0.0297, 0.0297), border2: Color.orange), midi2
			)};

		// MIDI Controls
	 	MVC_FlatButton(window,Rect(553, 4, 43, 18),"Cntrl")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color.orange*0.75)
			.color_(\down,Color.orange*0.75/2)
			.color_(\string,Color.white)
			.action_{ LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front };

		// 4.output channels
		MVC_PopUpMenu3(models[4],window    ,Rect(864,5,70,16),gui[\menuTheme2  ]);

		// 7.send channels
		MVC_PopUpMenu3(models[7],window    ,Rect(783,5,70,16),gui[\menuTheme2]);

		// tabs ///////
		gui[\masterTabs]=MVC_TabbedView(window, Rect(0,12, 952, 433), offset:((20)@0))
			.labels_(["Synth","Piano Roll","Prg"])
			.font_(Font("Helvetica", 11))
			.tabPosition_(\top)
			.unfocusedColors_(Color.orange/2! 3)
			.labelColors_(  Color.orange!3)
			.backgrounds_(  Color.clear!3)
			.tabCurve_(8)
			.tabHeight_(15)
			.followEdges_(true)
			.value_(0);

		gui[\synthTab] = gui[\masterTabs].mvcTab(0);
		gui[\pRollTab] = gui[\masterTabs].mvcTab(1);
		gui[\preTab] = gui[\masterTabs].mvcTab(2);

		MVC_UserView.new(gui[\synthTab],Rect(5,0,thisWidth-10,15))
			.canFocus_(false)
			.resize_(1)
			.drawFunc_{|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				l=thisRect.left;
				t=thisRect.top;
				w=thisRect.width;
				h=thisRect.height;
				Pen.use{
					Pen.smoothing_(true);
					Pen.fillColor_(Color.orange.set);
					Pen.fillRect(Rect(h,0,w-(h*2),h));
					Pen.color_(Color.orange);
					Pen.addWedge(h@h, h, 2pi*1.5, 2pi*0.25); // top left
					Pen.addWedge((w-h)@h, h, 2pi*1.75, 2pi*0.25); // top right
					Pen.perform(\fill);
				}; // end.pen
			};

		gui[\scrollView] = MVC_RoundedComView(gui[\synthTab],
							Rect(11,16,thisWidth-22,thisHeight-158), gui[\scrollTheme]);

		gui[\pScrollView] = MVC_RoundedComView(gui[\pRollTab],
							Rect(11,6,thisWidth-22,thisHeight-148), gui[\scrollTheme]);

		gui[\preScrollView] = MVC_RoundedComView(gui[\preTab],
							Rect(11,6,thisWidth-22,thisHeight-148), gui[\scrollTheme]);

		// piano roll
		sequencer.createWidgets(gui[\pScrollView],Rect(5,9,920,298),
				(\selectRect: Color.white,
				 \background: Color(0.5, 0.5, 0.5)*0.7,
				 \velocityBG: Color(3/77,1/103,0,65/77),
				 \buttons:    Color(6/11,42/83,29/65)*1.2,
				 \boxes:		Color(0.1,0.05,0,0.5),
				 \noteBG:     Color(1,0.5,0),
				 \noteBGS:    Color(1,0.75,0.25),
				 \noteBS:     Color(1,1,1),
				 \velocity:   Color(1,0.7,0.45),
				 \velocitySel: Color.white
				),
				parentViews: [ window, gui[\masterTabs].mvcTab(1)]
				);

		gui[\textHeader]=MVC_StaticText(gui[\synthTab],Rect(14,0,thisWidth-10, 18 ))
			.shadow_(false)
			.font_(Font("Helvetica",12,true))
			.color_(\string,Color.black)
			.string_("                      LFO                         |                                  VCO mod                              |                             VCO-1                       |                                     VCO-2                            |    VCO 1 + 2")  ;

		MVC_StaticText(gui[\synthTab],Rect(14,128,thisWidth-10, 18 ))
			.shadow_(false)
			.font_(Font("Helvetica",12,true))
			.color_(\string,Color.black)
			.string_("    HPF     |                                            VCF                                                |             VCA             |                               ENV-1                                 |                                  ENV-2")  ;

		// screen dividers
		MVC_PlainSquare(gui[\scrollView], Rect(0,113,930,15),  gui[\plainTheme2]);
		MVC_PlainSquare(gui[\scrollView], Rect(0,243,930,3),   gui[\plainTheme2]);
		MVC_PlainSquare(gui[\scrollView], Rect(0,0,930,2),     gui[\plainTheme]);		MVC_PlainSquare(gui[\scrollView], Rect(0,127,930,2),   gui[\plainTheme]);		MVC_PlainSquare(gui[\scrollView], Rect(0,246,930,2),   gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(169,0,3,113),   gui[\plainTheme2]);
		MVC_PlainSquare(gui[\scrollView], Rect(418,0,3,113),   gui[\plainTheme2]);
		MVC_PlainSquare(gui[\scrollView], Rect(614,0,3,113),   gui[\plainTheme2]);
		MVC_PlainSquare(gui[\scrollView], Rect(849,0,3,113),   gui[\plainTheme2]);
		MVC_PlainSquare(gui[\scrollView], Rect(56,127,3,116),  gui[\plainTheme2]);
		MVC_PlainSquare(gui[\scrollView], Rect(359,127,3,116), gui[\plainTheme2]);
		MVC_PlainSquare(gui[\scrollView], Rect(466,127,3,116), gui[\plainTheme2]);
		MVC_PlainSquare(gui[\scrollView], Rect(697,127,3,116), gui[\plainTheme2]);

		// logo
		MVC_ImageView(gui[\scrollView],Rect(2, 249, 168, 27))
			.image_("fontImages/JP08.png");

		// levels
		MVC_FlatDisplay(this.peakLeftModel,gui[\scrollView],Rect(365, 147, 6, 80));
		MVC_FlatDisplay(this.peakRightModel,gui[\scrollView],Rect(371, 147, 6, 80));

		// outer slider divisions, top
		[17,41, 68,91, 187,210, 237,260, 339,362, 439,463].do{|i|
			MVC_Scale(gui[\scrollView],Rect(i, 30, 5, 57), gui[\scaleTheme])
		};

		// outer slider divisions, bottom
		[17,41, 68,91, 111,134, 192,215, 243,267, 324,348, 380,404, 477,501,
			522,546, 567,591, 612,636, 710,734, 755,779, 800,824,  845,869].do{|i|
			MVC_Scale(gui[\scrollView],Rect(i,162, 5, 57), gui[\scaleTheme])
		};

		// controllers
		noControl.do{|i|

			// knobs
			if (RolandJP08.kindAt(i)==\knob) {
				MVC_MyKnob3(models[i+17],gui[\scrollView],RolandJP08.rectAt(i))
					.labelFont_(Font("Helvetica",11))
					.zeroValue_(models[i+17].controlSpec.default) // this might change
					.color_(\on,Color.orange)
					.color_(\numberUp,Color.white)
					.color_(\numberDown,Color.orange)
					.numberWidth_((i==14).if(4,-11));
			};

			// sliders
			if ((RolandJP08.kindAt(i)==\slider)or:{RolandJP08.kindAt(i)==\sliderH}) {
				MVC_SmoothSlider(models[i+17],gui[\scrollView],RolandJP08.rectAt(i))
					.labelFont_(Font("Helvetica",11))
					.orientation_( (RolandJP08.kindAt(i)==\slider).if(\vertical,\horizontal  ))
					.numberWidth_(-11)
					.color_(\knob,Color.orange)
					.color_(\numberUp,Color.white)
					.color_(\numberDown,Color.orange)
					.color_(\background,Color(0.2,0.2,0.2))
					.color_(\hilite,Color.black);
			};

		};


		// 60. VCO2 Oct
		MVC_OnOffView(models[60], gui[\scrollView], Rect(624, 90, 33, 19),gui[\onOffTheme4]);

		// 16. use program in presets
		MVC_OnOffView(models[16], gui[\scrollView], Rect(717, 253, 63, 19),gui[\onOffTheme4]);

		// 15. use controls in presets
		MVC_OnOffView(models[15], gui[\scrollView], Rect(785, 253, 63, 19),gui[\onOffTheme4]);

		// 14. midi clock out
		MVC_OnOffView(models[14], gui[\scrollView], Rect(854, 253, 73, 19),gui[\onOffTheme4]);

		// program
		MVC_NumberBox(models[12],gui[\scrollView],Rect(66, 278, 28, 19))
				.labelFont_(Font("Helvetica",13,true))
				.orientation_(\horiz)
				.label_("Program")
				.color_(\background,Color(0.25,0.25,0.25))
				.color_(\typing,Color.orange)
				.color_(\string,Color.white)
				.numberWidth_(-24);

		// program name text
		gui[\programName] = MVC_Text(gui[\scrollView],Rect(104, 273,132,26))
			.font_(Font("Helvetica",13,true))
			.align_(\left)
			.shadow_(false)
			.penShadow_(true)
			.string_("Roland JP08");

		// adaptor of program model to put text into gui[\programName]
		MVC_FuncAdaptor(models[12]).func_{
			var prog=	p[12];
			gui[\programName].string_(rolandPresets[prog-1]);
		};

		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],715@278,117,
				Color.orange/1.6,
				Color.orange/3,
				Color.orange/1.5,
				Color.orange,
				Color.black
			);
		this.attachActionsToPresetGUI;

		// 11.network this
		MVC_OnOffView(models[11], gui[\preTab], Rect(903, 295, 33, 18), gui[\onOffTheme1]);

		// the keyboard, fix bug so we don't need this scrollView
		gui[\keyboardOuterView]=MVC_CompositeView(window,Rect(12, 330+25, 925, 93))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);

		keyboardView=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0, 0, 925, 93),8,12)
			.keyboardColor_(Color(1.0, 0.8, 0.3))
			.pipeFunc_{|pipe|
				sequencer.pipeIn(pipe);     // to sequencer
				this.toMIDIOutBuffer(pipe); // and midi out
				if (p[11].isTrue) {
					api.sendOD(\netPipeIn, pipe.kind, pipe.note, pipe.velocity)}; // and network
			};

		// program names in a 8 x 8 grid
		rolandPresets.do{|string,i|
			gui[i]=MVC_Text(gui[\preTab], Rect( 40+((i%8)*113), 30+(i.div(8)*33), 105, 29) )
				.string_(string)
				.canEdit_(false)
				.enterStopsEditing_(true)
				.stringAction_{|me,string|
					rolandPresets[i]=string;
					rolandPresets.savePref("Roland Presets");
				}
				.enterKeyAction_{|me,string|
					keyboardView.focus
				}
				.mouseDownAction_{|me|
					if (gui[\select].value.isTrue) {
						api.groupCmdOD(\extProgIn, i+1, true);
					};
				}
				.color_(\string,Color(59/77,59/77,59/77)*1.4)
				.color_(\edit,Color(59/77,59/77,59/77)*1.4)
				.color_(\background,Color(0.14,0.12,0.11)*0.4)
				.color_(\focus,Color.orange)
				.color_(\editBackground, Color(0,0,0,0.7))
				.font_(Font.new("Helvetica", 14))
				.canReceiveDragHandler_{
					(View.currentDrag.isArray)and:{View.currentDrag[0].isString}
				}
				.receiveDragHandler_{
				// is the 1st item a string?
					if ((View.currentDrag.isArray)and:{View.currentDrag[0].isString}) {
						this.dropFile(i+1,View.currentDrag[0]);
					};
				};

		};

		// program grid indexs
		8.do{|i|
			gui[1000+i]=MVC_Text(gui[\preTab], Rect( 40+(i*113), 13, 105, 14) )
				.string_((i+1).asString)
				.align_(\center)
				.color_(\string,Color(59/77,59/77,59/77)*1.4)
				.color_(\background,Color(0.14,0.12,0.11,0.25)*0.4)
				.font_(Font.new("Helvetica", 11));
			gui[2000+i]=MVC_Text(gui[\preTab], Rect( 15, 30+(i*33), 20, 29) )
				.align_(\right)
				.string_((i+1).asString)
				.color_(\string,Color(59/77,59/77,59/77)*1.4)
				.color_(\background,Color(0.14,0.12,0.11,0.25)*0.4)
				.font_(Font.new("Helvetica", 11));
		};

		// edit text or select preset
		gui[\select]=MVC_OnOffView(gui[\preTab],Rect(15,11, 20, 17),gui[\onOffTheme1])
			.value_(1)
			.permanentStrings_(["S","S"])
			.action_{|me| rolandPresets.do{|string,i| gui[i].canEdit_(me.value.isFalse) } };

		// ImportFolder
	 	MVC_FlatButton(gui[\preTab],Rect(40, 294, 57, 20),"Import")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color.orange*0.75)
			.color_(\down,Color.orange*0.75/2)
			.color_(\string,Color.white)
			.action_{
				this.importFolder;
			};

		// info text

		MVC_Text(gui[\preTab],Rect(97, 294, 346, 20))
			.string_("= [PATCH 2 + Power On] JP-08/BACKUP/All files")
			.font_(Font("Helvetica",11))
			.shadow_(false)
			.color_(\string,Color.black)

	}

	// dsp stuFF for audio in /////////////////////////////////////////////////////////////////////

	// uses same synth def as LNX_AudioIn

	*initUGens{|s|

		if (verbose) { "SynthDef loaded: Audio In x2".postln; };

		SynthDef("LNX_AudioIn2", {
			|outputChannels=0, inputChannels=2, channelSetup=0, on=1|
			var signal, signalL, signalR;

			signal  = In.ar(inputChannels, 2);
			signal  = signal * Lag.kr(on*2);
			signalL = Select.ar(channelSetup,[
				signal[0], signal[0]+signal[1], signal[0], signal[1], Silent.ar]);
			signalR = Select.ar(channelSetup,[
				signal[1], signal[0]+signal[1], signal[0], signal[1], Silent.ar]);

			Out.ar(outputChannels,[signalL,signalR]);
		}).send(s);

	}

	startDSP{
		synth = Synth.tail(instGroup,"LNX_AudioIn2");
		node  = synth.nodeID;
	}

	stopDSP{ synth.free }

	// used for noteOff in sequencers
	// efficiency issue: this is called 3 times in alt_solo over a network
	stopNotesIfNeeded{|latency|
		this.updateOnSolo(latency);
	}

	updateOnSolo{|latency|
		switch (p[13].asInt)
			{0} {
				// "Audio In"
				if (node.notNil) {
					server.sendBundle(latency +! syncDelay,[\n_set, node, \on, this.isOn]) };
			}
			{1} {
				// "Sequencer"
				if (node.notNil) {
					server.sendBundle(latency +! syncDelay,[\n_set, node, \on, true]) };
				if (this.isOff) {this.stopAllNotes};
			}
			{2} {
				// "Both"
				if (node.notNil) {
					server.sendBundle(latency +! syncDelay,[\n_set, node, \on, this.isOn]) };
				if (this.isOff) {this.stopAllNotes};
			};
	}

	updateDSP{|oldP,latency|
		var in  = LNX_AudioDevices.firstInputBus+(p[3]*2);
		var on;

		if (p[13]==1) { on=true } { on=this.isOn };

		server.sendBundle(latency +! syncDelay,
			[\n_set, node, \inputChannels,in],
			[\n_set, node, \outputChannels,this.instGroupChannel],
			[\n_set, node, \on, on],
			[\n_set, node, \channelSetup, p[9]]
		);

		if (instOutSynth.notNil) {
			server.sendBundle(latency +! syncDelay,
				*this.getMixerArgs.collect{|i| [\n_set, instOutSynth.nodeID]++i } );
		};

	}

} // end ////////////////////////////////////

// metadata for the controls of the RolandJP08 /////////////////////////////////////////////

RolandJP08 {

	classvar <keys, <metadata;

	*initClass{

		metadata=(
	// top row
	0:  ["LFO RATE        "	,\slider, Rect(25,18,20,89),  [0,255,\linear,1], \int, "Rate"],
	2:  ["LFO DELAY TIME  "	,\slider, Rect(81,18,20,89),  [0,255,\linear,1], \int, "Delay Time"],
	4:  ["LFO WAVE        "	,\knob,   Rect(123,37,50,50), [0,5,\linear,1],   \jp1, "Wave"],
	25: ["OSC LFO MOD     "	,\slider, Rect(213,18,20,89), [0,255,\linear,1], \int, "LFO Mod"],
	27: ["OSC ENV MOD     "	,\slider, Rect(269,18,20,89), [0,255,\linear,1], \int, "Env Mod"],
	29: ["OSC FREQ MOD DST"	,\slider, Rect(322,42,27,40), [0,2,\linear,1],   \jp2, "Freq Mod"],
	31: ["PWM             "	,\slider, Rect(382,18,20,89), [0,255,\linear,1], \int, "Pulse Width"],
	33: ["PWM SOURCE      "	,\slider, Rect(428,42,27,40), [0,2,\linear,1],   \jp3, "Mod"],
	35: ["OSC1 CROSS MOD  "	,\slider, Rect(494,18,20,89), [0,255,\linear,1], \int, "Cross Mod"],
	37: ["OSC1 RANGE      "	,\knob,   Rect(545,37,50,50), [0,5,\linear,1],   \jp4, "Oct"],
	39: ["OSC1 WAVE       "	,\knob,   Rect(613,37,50,50), [0,5,\linear,1],   \jp5, "Wave"],
	41: ["OSC2 SYNC       "	,\slider, Rect(698,42,27,40), [0,1,\linear,1],   \switch, "Sync"],
	43: ["OSC2 RANGE      "	,\knob,   Rect(740,37,50,50), [0,255,\linear,1,128], \int, "Oct"],
	45: ["OSC2 TUNE       "	,\knob,   Rect(810,37,50,50), [0,255,\linear,1,128], \int, "Tune"],
	47: ["OSC2 WAVE       "	,\knob,   Rect(881,37,50,50), [0,5,\linear,1],   \jp6, "Wave"],
	49: ["MIX BALANCE     "	,\knob,   Rect(967,37,50,50), [0,255,\linear,1,128], \int,"Source Mix"],
	// bottom row
	50: ["HPF             "	,\slider, Rect(25,163,20,89), [0,255,\linear,1], \int, "Cutoff"],
	52: ["CUTOFF          "	,\slider, Rect(81,163,20,89), [0,255,\linear,1], \int, "Cutoff"],
	54: ["RESONANCE       "	,\slider, Rect(129,163,20,89), [0,255,\linear,1],\int, "Res"],
	56: ["FLT LPF SLOPE   "	,\slider, Rect(173,187,27,40), [0,1,\linear,1],  \jp7, "Slope"],
	62: ["FLT LFO MOD     "	,\slider, Rect(219,163,20,89), [0,255,\linear,1],\int, "LFO Mod"],
	58: ["FLT ENV MOD     ",\slider, Rect(276,163,20,89), [0,255,\linear,1],\int, "Env Mod"],
	60: ["FLT ENV MOD SRC "	,\slider, Rect(318,187,27,40), [0,1,\linear,1],  \jp8, "Env"],
	64: ["FLT KEY FOLLOW  "	,\slider, Rect(366,163,20,89), [0,255,\linear,1],\int, "Key"],
	75: ["AMP LEVEL       "	,\slider, Rect(428,163,20,89), [0,255,\linear,1],\int, "Level"],
	77: ["AMP LFO MOD     "	,\slider, Rect(472,187,27,40), [0,3,\linear,1],  \int, "LFO Mod"],
	100:["ENV1 ATTACK     "	,\slider, Rect(536,163,20,89), [0,255,\linear,1],\int, "A"],
	102:["ENV1 DECAY      "	,\slider, Rect(586,163,20,89), [0,255,\linear,1],\int, "D"],
	104:["ENV1 SUSTAIN    "	,\slider, Rect(636,163,20,89), [0,255,\linear,1],\int, "S"],
	106:["ENV1 RELEASE    "	,\slider, Rect(686,163,20,89), [0,255,\linear,1],\int, "R"],
	108:["ENV1 POLARITY   "	,\slider, Rect(729,187,27,40), [0,1,\linear,1],  \jp9, "Env"],
	125:["ENV2 ATTACK     "	,\slider, Rect(795,163,20,89), [0,255,\linear,1],\int, "A"],
	127:["ENV2 DECAY      "	,\slider, Rect(845,163,20,89), [0,255,\linear,1],\int, "D"],
	129:["ENV2 SUSTAIN    "	,\slider, Rect(895,163,20,89), [0,255,\linear,1],\int, "S"],
	131:["ENV2 RELEASE    "	,\slider, Rect(945,163,20,89), [0,255,\linear,1],\int, "R"],
	133:["ENV2 KEY FOLLOW "	,\slider, Rect(989,187,27,40), [0,3,\linear,1],  \jp10, "Key"],
	// other
	402:["DELAY LEVEL     ",\knob, Rect(279,294,23,23), [0,15,\linear,1],\int, "Delay Level"],
	404:["DELAY TIME      ",\knob, Rect(279+75,294,23,23), [0,15,\linear,1],\int, "Delay Time"],
	406:["DELAY FEEDBACK  ",\knob, Rect(279+(75*2),294,23,23), [0,15,\linear,1],\int, "Delay FB"],
	425:["PORTA SW        ",\slider, Rect(279+(75*3),294,23,23), [0,1,\linear,1],\switch, "Porta"],
	427:["PORTA TIME      ",\knob, Rect(279+(75*4),294,23,23), [0,255,\linear,1],\int, "Porta Time"],
	431:["ASSIGN MODE     ",\slider, Rect(279+(75*5),294,23,23), [0,3,\linear,1],\jp11, "Mode"],
	433:["BEND RANGE      ",\knob, Rect(279+(75*6),294,23,23), [0,15,\linear,1],\int, "Bend Range"],


		);
		keys = metadata.keys.asArray.sort;
	}

	*size{ ^metadata.size }
	*keyAt {|index| ^keys.at(index) }
	*indexOfKey{|key| ^keys.indexOf(key) }
	*keyOfName{|name| metadata.pairsDo{|key,value| if (value[0]==name) {^key} }; ^nil}
	*nameAt{|index| ^metadata.at(keys.at(index)).at(0) }
	*kindAt{|index| ^metadata.at(keys.at(index)).at(1) }
	*specAt{|index| ^metadata.at(keys.at(index)).at(3) }
	*numberFuncAt{|index| ^metadata.at(keys.at(index)).at(4) }
	*labelAt{|index| ^metadata.at(keys.at(index)).at(5) }
	*postAll{ keys.do{|key,i| key.post; ": \"".post; this.nameAt(i).post; "\"".postln } }
	*rectAt{|index|
		var s=0.9, r=metadata.at(keys.at(index)).at(2);
		^Rect(r.left*s,r.top*s+1,r.width*s,r.height*s);  // adjust size
	}

	// roland checksum. use on bytes 8..13
	*checkSum{|data|
		var cksum=Int8Array[0];
		data.size.do{|i|
			cksum[0] = cksum[0] + (data[i].bitAnd(0xFF))
		};
		cksum[0] = (0x100 - cksum[0]) & 0x7F;
		^cksum[0]
	}

}

/*

[F0, 41 (Roland SYSEX number), 10 (device ID), 00 (modelID),00,00,1C�(Product code for the JP-08), 12, 03, 00, address MSB, address LSB, value MSB, value LSB, checksum, F7]

the checksum for the byte @ index 14 is...

RolandJP08.checkSum(Int8Array[ -16, 65, 16, 0, 0, 0, 28, 18, 3, 0, 1, 18, 15, 13,  78, -9 ][8..13]);
RolandJP08.checkSum(Int8Array[ -16, 65, 16, 0, 0, 0, 28, 18, 3, 0, 1, 12,  0,  1, 111, -9 ][8..13]);
RolandJP08.checkSum(Int8Array[ -16, 65, 16, 0, 0, 0, 28, 18, 3, 0, 2,  8,  4,  1, 110, -9 ][8..13]);

f="/Users/neilcosgrove/Desktop/JP08_PATCH20.PRM".loadList;
f.last[11..].drop(-2).postln;
g=f.drop(-1).collect{|s| [ s[0..15],s[16..].interpret ] };
g[3][0];
RolandJP08.keyOfName(g[3][0]);

h=f.drop(-1).collect{|s| [ RolandJP08.keyOfName(s[0..15]),s[16..].interpret ] }.reject{|i| i[0].isNil}.flat.pairsDo{|a,b| [a,b].postln};


*/
