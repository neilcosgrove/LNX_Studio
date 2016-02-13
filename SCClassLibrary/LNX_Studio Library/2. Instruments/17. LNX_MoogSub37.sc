
// Control your Sub37 with this bad boy

// on load program need to trigger program change as needed
// wired bug selecting programs stored in presets

// using preset controls is unpredictable

LNX_MoogSub37 : LNX_InstrumentTemplate {
		
	classvar <moogPresets, <>isVisiblePref;

	var <sequencer, <keyboardView, <noControl, <midiInBuffer, <midiOutBuffer, <seqOutBuffer,
		<lastProgram;

	// goto updateGUI

	*initClass{
		Class.initClassTree(LNX_File);
		isVisiblePref = ("MoogIsVisible".loadPref ? [true])[0].isTrue;
		moogPresets = "Moog Presets".loadPref ?? { 256.collect{|i| "" } };
	}
	
	*saveIsVisiblePref{ [isVisiblePref].savePref("MoogIsVisible") }

	*isVisible{^isVisiblePref}

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	// properties
	*studioName      {^"Moog Sub 37"}
	*sortOrder       {^2}
	isInstrument     {^true}
	canBeSequenced   {^true}
	isMixerInstrument{^true}
	mixerColor       {^Color(0.2,0.2,0.2,0.5)} // colour in mixer
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
		instrumentHeaderType="SC Sub 37 Doc";
		version="v1.0";		
	}
	
	// an immutable list of methods available to the network
	interface{^#[\netMidiControlVP, \netExtCntIn, \extProgIn]}

	// MIDI patching ///////////////////////////////////////////////////////

	iInitVars{
		// the main sequencer
		sequencer = LNX_PianoRollSequencer(id++\pR)
			.pipeOutAction_{|pipe|
				if (((p[13]>0)&&(this.isOff)).not) {seqOutBuffer.pipeIn(pipe)};
			}
			.releaseAllAction_{ seqOutBuffer.releaseAll(studio.actualLatency) }
			.keyDownAction_{|me, char, modifiers, unicode, keycode|
				keyboardView.view.keyDownAction.value(me,char, modifiers, unicode, keycode)
			}
			.keyUpAction_{|me, char, modifiers, unicode, keycode|
				keyboardView.view.keyUpAction.value(me, char, modifiers, unicode, keycode)
			}
			.recordFocusAction_{ keyboardView.focus };
	}
	
	// any post midiInit stuff
	iInitMIDI{
		midi.findByName("Moog Sub 37","Moog Sub 37");
		this.useMIDIPipes;
		midiOutBuffer = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.toMIDIPipeOut(pipe) };
		midiInBuffer  = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromInBuffer(pipe)  };
		seqOutBuffer  = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromSequencerBuffer(pipe) };
	}
	
	midiSelectProgram{} // this needs to be disabled for everything
		
	// midi pipe in. This is the future
	pipeIn{|pipe|
		if (instOnSolo.isOff and: {p[13]>0} ) {^this}; // drop if sequencer off
		if (pipe.historyIncludes(this)) {^this};       // drop to prevent internal feedback loops
		
		switch (pipe.kind)
			{\control} { // control	
				var index = Sub37.keys.indexOf(pipe.num.asInt);
				
				// pipe.postln; // find out if controls come with a preset change
				
				// drop if volume(7) because sub37 gives a midi feedback loop for some reason
				if (pipe.num==7) {^this}; 		
				if (index.notNil) {
					models[index+14].lazyValue_(pipe.val, true); // set model, no action
					p[index+14]=pipe.val;                        // set p[]
					this.extCntIn(index, pipe.val, pipe.latency);
				};		
				^this // and drop 
			}
			{\program} { // program
				
				// this isn't going to work properly and only cause confusion
				
				// api.groupCmdOD(\extProgIn, pipe.program,false);
				^this // drop	
			};
		midiInBuffer.pipeIn(pipe); // to in Buffer. (control & progam are dropped above)
	}
	
	// set control
	extCntIn{|item,value,latency|
		api.sendVP((id++"_ccvp_"++item).asSymbol,
			'netExtCntIn',item,value,midi.uidOut,midi.midiOutChannel);
	}
	
	// net version of above
	netExtCntIn{|item,value,uidOut,midiOutChannel|
		p[item+14]=value;
		models[item+14].lazyValue_(value,false);
		// go on, do a pipe here
		midi.control(Sub37.keyAt(item) ,value,nil,false,true);
		// ignore set to true so no items learnt from this
	}

	// midi coming from in buffer
	fromInBuffer{|pipe|
		sequencer.pipeIn(pipe);                 // to the sequencer
		keyboardView.pipeIn(pipe,Color.orange); // to the gui keyboard
		// drop out and Don't send if pipe is external and coming from Sub37 going to Sub37
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
		midiOutBuffer.pipeIn(pipe);             // to midi out buffer
	}
	
	// and finally to the midi out
	toMIDIPipeOut{|pipe|
		midi.pipeIn(pipe.addToHistory(this).addLatency(syncDelay)); // add this to its history
	}

	// release all played notes, uses midi Buffer
	stopAllNotes{ 
		midiInBuffer.releaseAll(studio.actualLatency);
		seqOutBuffer.releaseAll(studio.actualLatency); 
		midiOutBuffer.releaseAll(studio.actualLatency); 
		{keyboardView.clear}.defer(studio.actualLatency);
	}
	
//	// external midi setting controls (these are network methods)
//	netExtCntIn{|index,val|
//		p[index.asInt]=val.asFloat;
//		models[index.asInt].lazyValue_(val.asFloat,false); // false is no auto
//	}
	
	// external midi setting prog number (these are network methods)
	extProgIn{|prog,send=false|
		prog=prog.asInt;
		p[11] = prog.div(16); // bank
		p[12] = prog%16;      // external
		models[11].lazyValue_(p[11],false); // false is no auto
		models[12].lazyValue_(p[12],false); // false is no auto
		this.updatePresetGUI;
		if (send.isTrue) { this.setMoogProgram };
	}
	
	// gui stuff for the preset tab.
	updatePresetGUI{
		var prog=	((p[11]*16)+p[12]).asInt;
		{
			if (lastProgram.notNil) {	
				gui[lastProgram].color_(\background,Color(0.14,0.12,0.11)*0.4);
				gui[1000+(lastProgram%16)].color_(\background,Color(0.14,0.12,0.11,0.25)*0.4);
				gui[2000+(lastProgram.div(16))].color_(\background,
					Color(0.14,0.12,0.11,0.25)*0.4);
			};
			gui[prog].color_(\background,Color(0.5,0.5,0.5,0.5));
			gui[1000+(prog%16)].color_(\background,Color(0.14,0.12,0.11)*0.4);
			gui[2000+(prog.div(16))].color_(\background,Color(0.14,0.12,0.11)*0.4);
			lastProgram = prog;
		}.defer;
	}
	
	///////////////////////////////////////////////////////

	// the models
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
					this.setSynthArgVH(5,val,\pan,val,latency,send);
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
			[[-1,1,\lin,0.001,0], {|me,val,latency,send|
				this.setPVP(10,val,latency,send);
				this.syncDelay_(val);
			}],		
						
			// 11. bank
			[0,[0,15,\lin,1], midiControl, 11, "Bank",{|me,value,latency,send,toggle|
				this.setODModel(11,value,latency,send); // network must come 1st
				this.setMoogProgram(latency);
			}],
			
			// 12. preset
			[0,[0,15,\lin,1], midiControl, 12, "Preset",{|me,value,latency,send,toggle|
				this.setODModel(12,value,latency,send); // network must come 1st
				this.setMoogProgram(latency);
			}],	
			
			// 13. onSolo turns audioIn, seq or both on/off
			[1, [0,2,\lin,1],  midiControl, 13, "On/Off Model",
				(\items_:["Audio In","Sequencer","Both"]),
				{|me,val,latency,send|
					this.setPVP(13,val,latency,send);
					this.updateOnSolo;
				}],
									
		];
		
		// 14-87: add all the sub37 controls
		noControl = Sub37.size;
		
		noControl.do{|i|
			template= template.add([0, [0,127,\linear,1], midiControl, i+14, Sub37.nameAt(i),
				( label_:(Sub37.nameAt(i)),numberFunc_:\int),
				{|me,val,latency| this.midiControlVP(i,val,latency) }]);
		};
		
		// 88. midi clock out
		template = template.add([1, \switch, midiControl, 88, "MIDI Clock",
				(strings_:["MIDI Clock"]),
				{|me,val,latency,send|
					this.setPVP(88,val,latency,send);
					if (val.isFalse) { midi.stop(latency +! syncDelay) };
				}]);
				
		// 89. use controls in presets
		template = template.add([0, \switch, midiControl, 89, "Controls Preset",
				(strings_:["Controls"]),
				{|me,val,latency,send|	
					this.setPVP(89,val,latency,send);
					if (val.isTrue) {
						presetExclusion=[0,1,10];
					}{
						presetExclusion=[0,1,10]++((1..Sub37.size)+13);
					}	
				}]);

		// 90. use program in presets
		template = template.add([1, \switch, midiControl, 90, "Program",
				(strings_:["Program"]),
				{|me,val,latency,send|	
					this.setPVP(90,val,latency,send);
				}]);
		
		#models,defaults=template.generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,10]++((1..Sub37.size)+13);
		randomExclusion=[0,1,10];
		autoExclusion=[10,11,12];

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
	midiSongPtr{|songPtr,latency| if (p[88].isTrue) { midi.songPtr(songPtr,latency +! syncDelay) }} 
	midiStart{|latency|           if (p[88].isTrue) { midi.start(latency +! syncDelay) } }
	midiClock{|latency|           if (p[88].isTrue) { midi.midiClock(latency +! syncDelay) } }
	midiContinue{|latency|        if (p[88].isTrue) { midi.continue(latency +! syncDelay) } }
	midiStop{|latency|            if (p[88].isTrue) { midi.stop(latency +! syncDelay) } }

	// disk i/o ///////////////////////////////
		
	// for your own saving
	iGetSaveList{ ^sequencer.getSaveList }
	
	// for your own loading
	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		sequencer.putLoadList(l.popEND("*** END OBJECT DOC ***"));
	}
	
	// anything else that needs doing before a load
	preLoadP{|tempP|
		models[90].doLazyValueAction_(tempP[90],nil,false); // use programs in presets
		// check send program  1st and then send
		if (models[90].isTrue) {
			models[11].lazyValue_(tempP[11],false);
			p[11] = tempP[11];
			models[12].lazyValue_(tempP[12],false);
			p[12] = tempP[12];
			this.setMoogProgram;
		};
		^tempP
	}
	
	// override insts template, only does this on load, should be called update models
	updateGUI{|tempP|
		tempP.do({|v,j|
			if (p[j]!=v) { 
				if ((j==11)||(j==12)) { 
					// dont do any actions on p==11 or 12
					models[j].lazyValue_(v,false);
				}{
					if ((tempP[89].isFalse)&&(j>=14)&&(j<=87)) {
						models[j].lazyValue_(v,false); // don't do action
					}{
						models[j].lazyValueAction_(v, nil ,send:false);
					}
				}
			}
		});
		this.iUpdateGUI(tempP);
	}
	
	// anything else that needs doing after a load. all paramemters will be loaded by here
	iPostLoad{}
	
	// free this
	iFree{ sequencer.do(_.free) }

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

	// sub 37 stuff ////////////////////////*****************************************************

	//  this is overriden to force program changes & exclusions by put p[89 & 90] 1st
	loadPreset{|i,latency|
		var presetToLoad, oldP;
		
		presetToLoad=presetMemory[i].copy;
		
		// 89.use controls
		models[89].lazyValueAction_(presetToLoad[89],latency,false);
		
		// 90.use program
		models[90].lazyValueAction_(presetToLoad[90],latency,false);
		
		// exclude these parameters
		presetExclusion.do{|i| presetToLoad[i]=p[i]}; // THIS STOP UPDATING DOWN BELOW *** VVVV		
		// check send program  1st and then send
		if (models[90].isTrue) {
			models[11].lazyValue_(presetToLoad[11],false);
			p[11] = presetToLoad[11];
			models[12].lazyValue_(presetToLoad[12],false);
			p[12] = presetToLoad[12];
			this.setMoogProgram(latency);
		};
			
		// 89. use controls in presets & presetExclusion WORK HERE FROM ABOVE *** ^^^
		presetToLoad.do{|v,j|	
			if (#[89,90,11,12].includes(j).not) {
				if (p[j]!=v) {
					models[j].lazyValueAction_(v,latency,false)
				}
			};
		};

		this.iLoadPreset(i,presetToLoad,latency);    // any instrument specific details
		oldP=p.copy;
		p=presetToLoad;               // copy the paramaters to p (is this needed any more?)
		this.updateDSP(oldP,latency); // and update any dsp
				
	}

	// set program on moog, latency isn't really needed here
	setMoogProgram{|latency|
		var prog=	(p[11]*16)+p[12];		
		midi.control(32, prog.div(128), latency +! syncDelay);
		midi.program(prog % 128, latency +! syncDelay);
		this.updatePresetGUI;
	}

	// set control
	midiControlVP{|item,value,latency|
		p[item+14]=value;		
		midi.control(Sub37.keyAt(item),value,latency +! syncDelay,false,true); // midi control out
		api.sendVP((id++"_ccvp_"++item).asSymbol,
					'netMidiControlVP',item,value,midi.uidOut,midi.midiOutChannel);
	}
	
	// net version of above
	netMidiControlVP{|item,value,uidOut,midiOutChannel|
		p[item+14]=value;
		models[item+14].lazyValue_(value,false);
		
		// go on, do a pipe here
		midi.control(Sub37.keyAt(item) ,value,nil,false,true);
		// ignore set to true so no items learnt from this
	}


	//////////////////////////*****************************************************
	// GUI
	
	*thisWidth  {^1047}
	*thisHeight {^425+10}
	
	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(0.185, 0.045, 0.045 , 1)*0.66);
	}
	
	iPostNew{ { sequencer.resizeToWindow }.defer(0.02) }

	createWidgets{

		gui[\scrollTheme]=( \background	: Color(0.25, 0.25, 0.25),
				 \border		: Color(0.6 , 0.562, 0.5));

		gui[\plainTheme]=( colors_: (\on	: Color(0,0,0),
				 		\off	: Color(0,0,0)));	
		
		gui[\onOffTheme1]=( \font_		: Font("Helvetica-Bold", 12),
						 \rounded_	: true,
						 \colors_     : (\on : Color(20/77,1,20/77), \off: Color(0.4,0.4,0.4)));
										
		gui[\onOffTheme2]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color(50/77,61/77,1), \off: Color(0.4,0.4,0.4)));
						 
		gui[\onOffTheme3]=( \font_		: Font("Helvetica-Bold", 12),
						 \rounded_	: true,
						 \colors_     : (\on : Color.orange, \off: Color(0.4,0.4,0.4)));
		
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));
						
						
		gui[\menuTheme2]=( \font_		: Font("Arial", 10),
						\labelShadow_	: false,
						\colors_      : (\background: Color(0.6 , 0.562, 0.5),
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
										\background : Color(0.6 , 0.562, 0.5),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.black,
										\focus : Color(0,0,0,0)));
		
		// control strip ///////
		
		// 1. channel onOff	
		MVC_OnOffView(models[1], window,Rect(182, 4, 26, 18),gui[\onOffTheme1])
			.permanentStrings_(["On","On"]);
		
		// 0. channel solo
		MVC_OnOffView(models[0], window, Rect(211, 4, 26, 18),gui[\soloTheme])
			.rounded_(true);

		// 13. onSolo turns audioIn, seq or both on/off
		MVC_PopUpMenu3(models[13],window,Rect(241,5,70,16), gui[\menuTheme2 ] );

		// 3. in	
		MVC_PopUpMenu3(models[3],window,Rect(318,5,70,16), gui[\menuTheme2 ] );
	
		// 9. channelSetup
		MVC_PopUpMenu3(models[9],window,Rect(395,5,75,16), gui[\menuTheme2 ] );
						
		// MIDI Settings
 		MVC_FlatButton(window,Rect(488, 5, 43, 18),"MIDI")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(0.6 , 0.562, 0.5))
			.color_(\down,Color(0.6 , 0.562, 0.5)/2)
			.color_(\string,Color.white)
			.action_{ this.createMIDIInOutModelWindow(window,nil,nil,
				(border1:Color(0.1221, 0.0297, 0.0297), border2: Color(0.6 , 0.562, 0.5))
			)};
	
		// MIDI Controls
	 	MVC_FlatButton(window,Rect(534, 5, 43, 18),"Cntrl")
			.rounded_(true)
			.shadow_(true)
			.canFocus_(false)
			.color_(\up,Color(0.6 , 0.562, 0.5))
			.color_(\down,Color(0.6 , 0.562, 0.5)/2)
			.color_(\string,Color.white)
			.action_{ LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front };
							
		// the preset interface
		presetView=MVC_PresetMenuInterface(window,589@5,180+17,
				Color(0.6 , 0.562, 0.5)/1.6,
				Color(0.6 , 0.562, 0.5)/3,
				Color(0.6 , 0.562, 0.5)/1.5,
				Color(0.6 , 0.562, 0.5),
				Color.black
			);
		this.attachActionsToPresetGUI;			
				 	
		// 4.output channels
		MVC_PopUpMenu3(models[4],window    ,Rect(965,5,70,16),gui[\menuTheme2  ]);
		
		// 7.send channels
		MVC_PopUpMenu3(models[7],window    ,Rect(965-75,5,70,16),gui[\menuTheme2]);
			
		// tabs /////// 		
		
		gui[\masterTabs]=MVC_TabbedView(window, Rect(0,12, 1047, 433), offset:((20)@0))
			.labels_(["Synth","Piano Roll","Prg"])
			.font_(Font("Helvetica", 11))
			.tabPosition_(\top)
			.unfocusedColors_(Color(0.6 , 0.562, 0.5)/2! 3)
			.labelColors_(  Color(0.6 , 0.562, 0.5)!3)
			.backgrounds_(  Color.clear!3)
			.tabCurve_(8)
		//	.tabWidth_([63,63])
			.tabHeight_(15)
			.followEdges_(true)
			.value_(0);
		
		// control tab
	
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
					Pen.fillColor_(Color(0.6 , 0.562, 0.5).set);
					Pen.fillRect(Rect(h,0,w-(h*2),h));
					Pen.color_(Color(0.6 , 0.562, 0.5));
					Pen.addWedge(h@h, h, 2pi*1.5, 2pi*0.25); // top left
					Pen.addWedge((w-h)@h, h, 2pi*1.75, 2pi*0.25); // top right
					Pen.perform(\fill);
				}; // end.pen
			};	
				 		
		gui[\scrollView] = MVC_RoundedComView(gui[\synthTab],
							Rect(11,16,thisWidth-22,thisHeight-132-1-25), gui[\scrollTheme]);

		gui[\pScrollView] = MVC_RoundedComView(gui[\pRollTab],
							Rect(11,6,thisWidth-22,thisHeight-132-1-15), gui[\scrollTheme]);
			
		gui[\preScrollView] = MVC_RoundedComView(gui[\preTab],
							Rect(11,6,thisWidth-22,thisHeight-132-1-15), gui[\scrollTheme]);
							
		// piano roll	
		sequencer.createWidgets(gui[\pScrollView],Rect(5,9,700+315,256+17),
				(\selectRect: Color.white,
				 \background: Color(0.50090909090909, 0.50602409638554, 0.50602409638554)*0.7,
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
										
		MVC_StaticText(gui[\synthTab],Rect(14,0,thisWidth-10, 18 ))
			.shadow_(false)
			.font_(Font("Helvetica",11))
			.color_(\string,Color.black)
			.string_("Arpeggiator        Glide                      Mod 1                               Mod 2                            Oscillators                         Mixer                               Filter                                               Envelope Generators")  ;

		// controllers
		
		MVC_PlainSquare(gui[\scrollView], Rect(64,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(128,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(249,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(374,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(499,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(602,0,5,217), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(732,0,5,277), gui[\plainTheme]);
		MVC_PlainSquare(gui[\scrollView], Rect(0,217,735,5), gui[\plainTheme]);
								
		MVC_Text(gui[\scrollView],Rect(540, 222,177,39))
			.align_(\center)
			.shadow_(false)
			.penShadow_(true)
			.font_(Font("AvenirNext-Heavy",23))
			.string_("Moog Sub 37");
			
		// levels
		MVC_FlatDisplay(this.peakLeftModel,gui[\scrollView],Rect(718, 224, 6, 51));
		MVC_FlatDisplay(this.peakRightModel,gui[\scrollView],Rect(724, 224, 6, 51));
		//MVC_Scale(gui[\controlsTab],Rect(11+630, 11, 2, 160));
		
		noControl.do{|i|

			// buttons
			if (Sub37.kindAt(i)==\button) {
				MVC_OnOffView(models[i+14],gui[\scrollView],Sub37.rectAt(i),Sub37.nameAt(i))
					.rounded_(true)
					.onValue_(127)
					.showNumberBox_(false)
					.label_(nil)
					.font_(Font("Helvetica-Bold", 10))
					.color_(\on,Color.orange)
					.color_(\off,Color(0.4,0.4,0.4));
			};
			
			// knobs
			if (Sub37.kindAt(i)==\knob) {
				MVC_MyKnob3(models[i+14],gui[\scrollView],Sub37.rectAt(i))
					.labelFont_(Font("Helvetica",11))
					.color_(\on,Color.orange)
					.color_(\numberUp,Color.black)
					.color_(\numberDown,Color.orange)
					.numberWidth_(-24);
			};
			
			// sliders
			if ((Sub37.kindAt(i)==\slider)or:{Sub37.kindAt(i)==\sliderH}) {
				MVC_SmoothSlider(models[i+14],gui[\scrollView],Sub37.rectAt(i))
					.labelFont_(Font("Helvetica",11))
					.orientation_( (Sub37.kindAt(i)==\slider).if(\vertical,\horizontal  ))
					.numberWidth_(-30)
					.color_(\knob,Color.orange)
					.color_(\numberUp,Color.black)
					.color_(\numberDown,Color.orange);
			};
			
			// numberBox
			if (Sub37.kindAt(i)==\numberBox) {
				MVC_NumberBox(models[i+14],gui[\scrollView],Sub37.rectAt(i))
					//.labelFont_(Font("Helvetica",11))
					.label_(nil)
					.color_(\background,Color(0,0,0,0.5))
					.color_(\string,Color.white)
					.numberWidth_(-24);
			};
				
		};
		
		// 88. midi clock out
		MVC_OnOffView(models[88], gui[\scrollView], Rect(3, 250, 73, 19),gui[\onOffTheme3]);

		// 89. use controls in presets
		MVC_OnOffView(models[89], gui[\scrollView], Rect(137, 250, 63, 19),gui[\onOffTheme3]);
		
		// 90. use program in presets
		MVC_OnOffView(models[90], gui[\scrollView], Rect(137, 226, 63, 19),gui[\onOffTheme3]);
		
		// bank & preset
		MVC_ProgramChangeMoog(gui[\scrollView], models[11], Rect(215, 229, 320, 19))
			.color_(\background,Color(0.2,0.2,0.2))
			.color_(\on,Color(0.5,0.5,0.5));
		MVC_ProgramChangeMoog(gui[\scrollView], models[12], Rect(215, 252, 320, 19))
			.color_(\background,Color(0.2,0.2,0.2))
			.color_(\on,Color(0.5,0.5,0.5));
				
		gui[\programName] = MVC_Text(gui[\scrollView],Rect(540, 258,177,18))
			.align_(\center)
			.shadow_(false)
			.penShadow_(true)
			.string_("Moog Sub 37");	
			
		MVC_FuncAdaptor(models[11]).func_{
			var prog=	(p[11]*16)+p[12];
			gui[\programName].string_(moogPresets[prog]);
		};
		MVC_FuncAdaptor(models[12]).func_{
			var prog=	(p[11]*16)+p[12];
			gui[\programName].string_(moogPresets[prog]);
		};
				
		// the keyboard, fix bug so we don't need this scrollView
		gui[\keyboardOuterView]=MVC_CompositeView(window,Rect(12,310+20,1020,93))
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false);
			
		keyboardView=MVC_MIDIKeyboard(gui[\keyboardOuterView],Rect(0,0,1020,93),8,12)
			.keyboardColor_(Color(1,0.5,0)+0.3)
			.pipeFunc_{|pipe|
				sequencer.pipeIn(pipe);     // to sequencer
				this.toMIDIOutBuffer(pipe); // and midi out
			};
				
		moogPresets.do{|string,i|
			gui[i]=MVC_Text(gui[\preTab], Rect( 40+((i%16)*62), 30+(i.div(16)*16), 60-2, 13) )
				.string_(string)
				.canEdit_(true)
				.enterStopsEditing_(true)
				.stringAction_{|me,string|
					moogPresets[i]=string;
					moogPresets.savePref("Moog Presets");
				}
				.enterKeyAction_{|me,string|
					keyboardView.focus
				}
				.mouseDownAction_{|me|
					if (gui[\select].value.isTrue) {
						api.groupCmdOD(\extProgIn, i, true);
					};
				}
				.color_(\string,Color(59/77,59/77,59/77)*1.4)
				.color_(\edit,Color(59/77,59/77,59/77)*1.4)
				.color_(\background,Color(0.14,0.12,0.11)*0.4)
				.color_(\focus,Color.orange)
				.color_(\editBackground, Color(0,0,0,0.7))
				.font_(Font.new("STXihei", 10));	
			
		};
			
		16.do{|i|
			gui[1000+i]=MVC_Text(gui[\preTab], Rect( 40+(i*62), 13, 60-2, 14) )
				.string_((i+1).asString)
				.align_(\center)
				.color_(\string,Color(59/77,59/77,59/77)*1.4)
				.color_(\background,Color(0.14,0.12,0.11,0.25)*0.4)
				.font_(Font.new("STXihei", 11));	
			gui[2000+i]=MVC_Text(gui[\preTab], Rect( 15, 30+(i*16), 20, 14) )
				.align_(\right)
				.string_((i+1).asString)
				.color_(\string,Color(59/77,59/77,59/77)*1.4)
				.color_(\background,Color(0.14,0.12,0.11,0.25)*0.4)
				.font_(Font.new("STXihei", 11));	
		};
		
		// select preset	
		gui[\select]=MVC_OnOffView(gui[\preTab],Rect(15,11, 20, 17),gui[\onOffTheme1])
			.permanentStrings_(["S","S"])
			.action_{|me|
				moogPresets.do{|string,i| gui[i].canEdit_(me.value.isFalse) }
			};
			
			
	}
	
	// dsp stuFF for audio in /////////////////////////////////////////////////////////////////////

	// uses same synth def as LNX_AudioIn
		
	startDSP{
		synth = Synth.tail(instGroup,"LNX_AudioIn");
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
		var out, on;				
		if (p[4]>=0) {
			out = p[4]*2
		}{	
			out = LNX_AudioDevices.firstFXBus+(p[4].neg*2-2);
		};
		this.instOutChannel_(out,latency);
		if (p[13]==1) { on=true } { on=this.isOn };
			
		server.sendBundle(latency +! syncDelay,
			[\n_set, node, \amp,p[2].dbamp],
			[\n_set, node, \pan,p[5]],
			[\n_set, node, \inputChannels,in],
			[\n_set, node, \outputChannels,this.instGroupChannel],
			[\n_set, node, \on, on],
			[\n_set, node, \sendChannels,LNX_AudioDevices.getOutChannelIndex(p[7])],
			[\n_set, node, \sendAmp, p[8].dbamp],
			[\n_set, node, \channelSetup, p[9]]
			
		);
	}

} // end ////////////////////////////////////

// metadata for the controls of the Moog Sub37 /////////////////////////////////////////////

/*
Sub37.controls;
Sub37.keys;
Sub37.names;
Sub37.postAll;
*/

Sub37 {
	
	classvar <keys, <metadata;
	
	*initClass{
		
		metadata=(
			  1: ["Mod Wheel", 	\knob, Rect(96,198, 25, 25), [0,127], ],
			  3: ["Rate", 		\knob, Rect(170, 23, 25, 25), [0,127], ], // mod 1
			  4: ["Pitch Amt", 	\knob, Rect(170, 110, 25, 25), [0,127], ], // mod 1
			  5: ["Time", 		\knob, Rect(96, 23, 25, 25), [0,127], ],
			  7: ["Volume", 		\sliderH, Rect(891, 278, 206, 23), [0,127], ],
			  8: ["Rate", 		\knob, Rect(304, 23, 25, 25), [0,127], ], // mod 2
			  9: ["Wave 1", 		\knob, Rect(507, 23, 25, 25), [0,127], ],
			 11: ["Filter Amt", 	\knob, Rect(230, 110, 25, 25), [0,127], ], // mod 1
			 12: ["Freq", 		\knob, Rect(444, 198, 25, 25), [0,127], ],
			 13: ["Beat Freq", 	\knob, Rect(511, 198, 25, 25), [0,127], ],
			 14: ["Wave 2", 		\knob, Rect(511, 110, 25, 25), [0,127], ],
			 15: ["Pitch Amt", 	\knob, Rect(304, 110, 25, 25), [0,127], ], // mod 2
			 16: ["Filter Amt", 	\knob, Rect(364, 110, 25, 25), [0,127], ], // mod 2
			 17: ["Mod Amt", 		\knob, Rect(364, 198, 25, 25), [0,127], ], // mod 2
			 18: ["Multidrive", 	\knob, Rect(770, 110, 25, 25), [0,127], ],
			 19: ["Cutoff", 		\knob, Rect(725, 23, 50, 50), [0,127], ],
			 20: ["Mod Amt", 		\knob, Rect(230, 198, 25, 25), [0,127], ], // mod 1
			 21: ["Resonance", 	\knob, Rect(700, 110, 25, 25), [0,127], ],
			 22: ["KB Track",		\knob, Rect(770, 198, 25, 25), [0,127], ],
			 23: ["Attack (F)", 	\knob, Rect(845, 23, 25, 25), [0,127], ],
			 24: ["Decay (F)", 	\knob, Rect(920, 23, 25, 25), [0,127], ],
			 25: ["Sustain (F)", 	\knob, Rect(1000, 23, 25, 25), [0,127], ],
			 26: ["Release (F)", 	\knob, Rect(1080, 23, 25, 25), [0,127], ],
			 27: ["EG Amt", 		\knob, Rect(700, 198, 25, 25), [0,127], ],
			 28: ["Attack (A)",	\knob, Rect(845, 173, 25, 25), [0,127], ],
			 29: ["Decay (A)", 	\knob, Rect(920, 173, 25, 25), [0,127], ],
			 30: ["Sustain (A)", 	\knob, Rect(1000, 173, 25, 25), [0,127], ],
			 31: ["Release (A)", 	\knob, Rect(1080, 173, 25, 25), [0,127], ],
			 64: ["Hold Pedal", 	\button, Rect(9, 254, 69, 21), [0,127], ],
			 65: ["On", 			\button, Rect(80, 154, 60, 21), [0,127], ], // glide
			 69: ["Latch",		\button, Rect(7, 64, 60, 21), [0,127], ], // arp
			 70: ["Osc", 			\button, Rect(150, 154, 60, 21), [0,127], ], // mod 1
			 71: ["Source", 		\knob, Rect(230, 23, 25, 25), [0,127], ], // mod 1
			 72: ["Source", 		\knob, Rect(364, 23, 25, 25), [0,127], ], // mod 2
			 73: ["On", 			\button, Rect(7, 95, 60, 21), [0,127], ], // arp
			 74: ["Octave 1", 		\knob, Rect(444, 23, 25, 25), [0,127], ],
			 75: ["Octave 2", 		\knob, Rect(444, 110, 25, 25), [0,127], ],
			 76: ["Hi Range",		\button, Rect(150, 64, 60, 21), [0,127], ], // mod 1
			 77: ["Hard sync", 	\button, Rect(424, 64, 60, 21), [0,127], ],
			 78: ["Hi Range",		\button, Rect(284, 64, 60, 21), [0,127], ], // mod 2
			 79: ["KB Track (F)",	\knob, Rect(1080, 83, 25, 25), [0,127], ],
			 80: ["KB Track (A)", 	\knob, Rect(1080, 228, 25, 25), [0,127], ],
			 81: ["KB Reset", 		\button, Rect(490, 64, 60, 21), [0,127], ], // osc
			 82: ["Reset (F)", 	\button, Rect(902, 126, 60, 21), [0,127], ],
			 83: ["Reset (A)", 	\button, Rect(1061, 126, 60, 21), [0,127], ], // amp
			 85: ["Type", 		\button, Rect(80, 95, 60, 21), [0,127], ], // glide
			 86: ["Vel Amt (F)", 	\knob, Rect(1000, 83, 25, 25), [0,127], ],
			 87: ["Vel Amt (A)", 	\knob, Rect(1000, 228, 25, 25), [0,127], ],
			 88: ["Osc", 			\button, Rect(284, 154, 60, 21), [0,127], ], // mod 2
			 89: ["KB Octave", 	\knob, Rect(25, 198, 25, 25), [0,127], ],
			 90: ["Rate", 		\knob, Rect(25, 23, 25, 25), [0,127], ], // arp
			 91: ["Dest", 		\knob, Rect(170, 198, 25, 25), [0,127], ], // mod 1
			 92: ["Dest", 		\knob, Rect(304, 198, 25, 25), [0,127], ], // mod 2
			 93: ["KB Reset", 		\button, Rect(214, 64, 60, 21), [0,127], ], // mod 1
			 94: ["Legato", 		\button, Rect(80, 124, 60, 21), [0,127], ], // glide
			 95: ["KB Reset", 		\button, Rect(351, 64, 60, 21), [0,127], ], // mod 2
			102: ["Osc", 			\button, Rect(80, 64, 60, 21), [0,127], ], // glide
			103: ["Delay (F)", 	\knob, Rect(845, 83, 25, 25), [0,127], ],
			104: ["Delay (A)", 	\knob, Rect(845, 228, 25, 25), [0,127], ],
			105: ["Hold (F)", 		\knob, Rect(920, 83, 25, 25), [0,127], ],
			106: ["Hold (A)", 		\knob, Rect(920, 228, 25, 25), [0,127], ],
			107: ["Pitch Up",		\numberBox, Rect(93, 252, 43, 20), [0,127], ],
			108: ["Pitch Down", 	\numberBox, Rect(93,277, 43, 20), [0,127], ],
			109: ["Slope", 		\slider, Rect(698, 155, 100, 14), [0,127], ],
			110: ["Duo Mode", 		\button, Rect(490, 154, 60, 21), [0,127], ],
			111: ["KB Ctrl", 		\button, Rect(424, 154, 60, 21), [0,127], ], // duo
			112: ["Multi Trig", 	\button, Rect(827, 126, 60, 21), [0,127], ],
			113: ["Multi Trig",	\button, Rect(981, 126, 60, 21), [0,127], ], // amp
			114: ["Osc1 Amp", 		\knob, Rect(580, 23, 25, 25), [0,127], ],
			115: ["Sub1 Amp", 		\knob, Rect(625, 66, 25, 25), [0,127], ],
			116: ["Osc2 Amp", 		\knob, Rect(580, 110, 25, 25), [0,127], ],
			117: ["Noise Amp", 	\knob, Rect(625, 153, 25, 25), [0,127], ],
			118: ["FB / Ext", 		\knob, Rect(580, 198, 25, 25), [0,127], ],
			119: ["Transpose", 	\knob, Rect(25,141, 25, 25), [0,127], ]
		);
		
		keys = metadata.keys.asArray.sort;		

	}

	*size{ ^metadata.size }		
	*keyAt {|index| ^keys.at(index) }
	*nameAt{|index| ^metadata.at(keys.at(index)).at(0) }
	*kindAt{|index| ^metadata.at(keys.at(index)).at(1) }
	*specAt{|index| ^metadata.at(keys.at(index)).at(3) }
	*postAll{ keys.do{|key,i| key.post; ": \"".post; this.nameAt(i).post; "\"".postln } }
	*rectAt{|index|
		var s=0.9, r=metadata.at(keys.at(index)).at(2);
		^Rect(r.left*s,r.top*s,r.width*s,r.height*s);  // adjust size
	}
	
}
