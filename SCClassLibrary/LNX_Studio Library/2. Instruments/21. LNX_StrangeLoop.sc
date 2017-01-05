////////////////////////////////////////////////////////////////////////////////////////////
// LNX_StrangeLoop //
////////////////////
/*
Possible playback modes are...
	repitch : sample rate is changed to fit loop length
	marker  : sample rate is fixed and events happen on markers. clip or fold to extend.
	grain   : grains are streched to fit loop length & Grain events happen on markers

To do / Think about...
----------------------
syncDelay
max is a reset instead
when you enter your bpm with keyboard length should change
new sample default is current song bpm
what if each marker could snd out its own midi
new sample length (n) beats (mono/stereo)
record audio - many sources (level in) (overdub,replace)
	transfer data from server to sclang via a temp file
save names in cashe / dialog ?
playback bpm [div 2] [*2] buttons
quantise options
grains
clock offset start
one shot
zeroX or peak
where does sample metadata sit
proll has dark patches for out of range
marker metadata ie default
markers can clip, wrap or rev
markers have colour
this might need a buffer and a voicer
attack decay envelope

BUGS: !!!
---------
need to put latency sync in
aliasing when scrolling
focus is lost when adding samples now
space bar is playing wrong sample on load
still release problems when swapping over from hold or stopping

Done
----
reverse button
tap bpm
stopping sampleRefresh when not following
also funcs called a lot when selecting sample
entering numbers via keyboard to tempo doesn't update LNX_InstrumentTemplate:bpmChange
lazy pRoll marker
loop true is default
freeze button
beat repeat based on fixed frame rather than events
	seperate from event based
	both can't happen at the same time & neither can stop the other
	has repeat%, freeze, memory, trans & amp
	repeat remains on chance?
	starts every (n) beat - range and quant i.e [2-8 every 2] = (2,4,6,8) // maybe only offset
	repeat length (n) beat - range and quant i.e [2-8 every 2] = (2,4,6,8)

*/

LNX_StrangeLoop : LNX_InstrumentTemplate {

	var <sampleBank,		<sampleBankGUI,	<webBrowser, 		<relaunch = false,	<newBPM = false;
	var <mode = \marker,	<markerSeq,		<lastMarkerEvent,	<lastMarkerEvent2;
	var <allMakerEvents,    <noteOnNodes,	<sequencer,			<seqOutBuffer;
	var <repeatMode,		<recordNode;

	var <repeatNo=0,		<repeatRate=0,	<repeatAmp=1,		<repeatStart=0;
	var <repeatNoE=0,		<repeatRateE=0,	<repeatAmpE=1;

	var <guiModeModel,		<previousMode,	<currentRateAdj=1;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName		 {^"StRAnGE loOp"}
	*sortOrder		 {^1.5}
	*isVisible		 {^true}
	isInstrument	 {^true}
	canBeSequenced	 {^true}
	isMixerInstrument{^true}
	mixerColor		 {^Color(0,0.1,0.1,0.4)} // colour in mixer
	hasLevelsOut	 {^true}
	peakModel   	 {^models[6]}
	volumeModel 	 {^models[2]}
	outChModel  	 {^models[4]}
	soloModel   	 {^models[0]}
	onOffModel  	 {^models[1]}
	panModel    	 {^models[5]}
	sendChModel 	 {^models[7]}
	sendAmpModel	 {^models[8]}
	syncModel   	 {^models[10]}

	header {
		// define your document header details
		instrumentHeaderType="SC StrangeLoop Doc";
		version="v1.0";
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////

	// the models
	initModel {

		#models,defaults=[
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

			// 11. selected sample
			[0, [0,sampleBank.size,\linear,1],  (label_:"Sample"), midiControl, 11, "Sample",
				{|me,val,latency,send|
					this.setPVPModel(11,val,latency,send);
					relaunch = true;

					// i still need this below...

					sampleBank.allInterfacesSelect(val,false);
			}],

			// 12. transpose  -48 to 48
			[0, [-48,48,\linear,1],  (label_:"Transpose", numberFunc_:\intSign), midiControl, 12, "Transpose",
				{|me,val,latency,send|
					this.setPVPModel(12,val,latency,send);
					if (mode===\marker) { this.marker_changeRate };
			}],

			// 13. fine  -1 to 1
			[0, [-1,1],  (label_:"Fine", numberFunc_:\float2Sign), midiControl, 13, "Fine",
				{|me,val,latency,send|
					this.setPVPModel(13,val,latency,send);
					if (mode===\marker) { this.marker_changeRate };
			}],

			// 14. clip, fold or wrap
			[0, [0,2,\linear,1],  (items_:["Clip","Fold","Wrap"]), midiControl, 14, "Fold/Wrap",
				{|me,val,latency,send| this.setPVPModel(14,val,latency,send) }],

			// 15. repeat prob
			[0, [0,100,\lin,0.1,0,"%"],  (label_:"Repeat", numberFunc_:\float1), midiControl, 15, "Repeat",
				{|me,val,latency,send| this.setPVPModel(15,val,latency,send) }],

			// 16. repeat transpose  -48 to 48
			[0, [-48,48,\linear,1],  (label_:"Trans", numberFunc_:\intSign), midiControl, 16, "R Trans",
				{|me,val,latency,send| this.setPVPModel(16,val,latency,send) }],

			// 17. repeat decay
			[1, [0,1],  (label_:"Decay", numberFunc_:\float2), midiControl, 17, "R Decay",
				{|me,val,latency,send| this.setPVPModel(17,val,latency,send) }],

			// 18. play back mode
			[1, \switch,  (items_:["Sequncer","Play Loop"]), midiControl, 18, "Play back mode",
				{|me,val,latency,send|
					this.setPVPModel(18,val,latency,send);
					if (val==0) {
						this.marker_stopBuffer(latency ? (studio.latency))
					}{
						seqOutBuffer.releaseAll(latency ? studio.actualLatency)
					};
			}],

			// 19. event freeze
			[0, \switch, midiControl, 19, "Freeze",
				{|me,val,latency,send| this.setPVPModel(19,val,latency,send) }],

			// 20. memory 1-8
			[1, [1,8,\linear,1],  (label_:"Memory", numberFunc_:\int), midiControl, 20, "Memory",
				{|me,val,latency,send| this.setPVPModel(20,val,latency,send) }],

			// 21. reverse
			[0, \switch, midiControl, 21, "Rev",
				{|me,val,latency,send| this.setPVPModel(21,val,latency,send) }],

			// 22. frame freeze
			[0, \switch, midiControl, 22, "Frame",
				{|me,val,latency,send| this.setPVPModel(22,val,latency,send) }],

			// 23. frame length 1-32
			[4, [1,16,\linear,1],  (label_:"Frame", numberFunc_:\int), midiControl, 23, "Frame",
				{|me,val,latency,send| this.setPVPModel(23,val,latency,send) }],

			// 24. repeat prob
			[0, [0,100,\lin,0.1,0,"%"],  (label_:"Repeat", numberFunc_:\float1), midiControl, 24, "Repeat",
				{|me,val,latency,send| this.setPVPModel(24,val,latency,send) }],

			// 25. memory 1-8
			[1, [1,8,\linear,1],  (label_:"Memory", numberFunc_:\int), midiControl, 25, "Memory",
				{|me,val,latency,send| this.setPVPModel(25,val,latency,send) }],

			// 26. repeat transpose  -48 to 48
			[0, [-48,48,\linear,1],  (label_:"Trans", numberFunc_:\intSign), midiControl, 26, "R Trans",
				{|me,val,latency,send| this.setPVPModel(26,val,latency,send) }],

			// 27. repeat decay
			[1, [0,1],  (label_:"Decay", numberFunc_:\float2), midiControl, 27, "R Decay",
				{|me,val,latency,send| this.setPVPModel(27,val,latency,send) }],

			// 28. frame Start
			[4, [1,16,\linear,1],  (label_:"Start", numberFunc_:\int), midiControl, 28, "Start",
				{|me,val,latency,send|
					this.setPVPModel(28,val,latency,send);
					{ models[29].controlSpec_( [0,val-1,\linear,1] )}.defer;
			}],

			// 29. frame offset
			[0, [0,15,\linear,1],  (label_:"Offset", numberFunc_:\int), midiControl, 29, "Offset",
				{|me,val,latency,send| this.setPVPModel(29,val,latency,send) }],

			// 30. reset / latch
			[129, [1,129,\linear,1], midiControl, 30, "Reset & Latch",
				(label_:"Latch", numberFunc_:{|n| (n==129).if("inf",n.asInt.asString)}),
				{|me,val,latency,send| this.setPVPModel(30,val,latency,send) }],

			// 31. reset latch Mode
			[1, \switch, midiControl, 31, "Reset Mode",
				{|me,val,latency,send| this.setPVPModel(31,val,latency,send) }],

		].generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1,10];
		autoExclusion=[];


		guiModeModel = [0,[0,2,\lin,1,1]].asModel;
	}

	updateGUI{|tempP|
		(11..18).do{|i| p[i] = tempP[i] };
		tempP.do{|v,j|
			if (p[j]!=v) {
				models[j].lazyValueAction_(v,send:false)
			}{
				models[j].lazyValue_(v,auto:false)
			};
		};
		this.iUpdateGUI(tempP);
	}

	iInitVars{
		// user content !!!!!!!
		sampleBank = LNX_SampleBank(server,apiID:((id++"_url_").asSymbol))
				.defaultLoop_(1)
				.selectedAction_{|bank,val,send=true,update=true|
					if (update) {models[11].valueAction_(val,nil,true)};
					if (mode===\marker ) { this.marker_update(\selectFunc) };
					relaunch = true;
				}
				.itemAction_{|bank,items,send=false|
					models[11].controlSpec_([0,sampleBank.size,\linear,1]);
					{models[11].dependantsPerform(\items_,bank.names)}.defer;
					if (mode===\marker ) { this.marker_update(\itemFunc) };
				}
				.metaDataUpdateFunc_{|me,model|
					if (mode===\repitch) { this.pitch_update (model) };
					if (mode===\marker ) { this.marker_update(model) };
				}
				.finishedLoadingFunc_{|me|
					// needed else we have wrong markers in sampleBank
					if (me.size>0) { me.allInterfacesSelect(p[11], false, false) };
				}
				.title_("");

		// the webBrowsers used to search for new sounds!
		webBrowser = LNX_WebBrowser(server,sampleBank);

		sequencer = LNX_PianoRollSequencer(id++\pR)
			.pipeOutAction_{|pipe|
				if (this.isOff.not) {seqOutBuffer.pipeIn(pipe)};
			}
			.releaseAllAction_{ seqOutBuffer.releaseAll(studio.actualLatency) }
			.keyDownAction_{|me, char, modifiers, unicode, keycode, key|
				//keyboardView.view.keyDownAction.value(me,char, modifiers, unicode, keycode, key)
			}
			.keyUpAction_{|me, char, modifiers, unicode, keycode, key|
				//.view.keyUpAction.value(me, char, modifiers, unicode, keycode, key)
			}
			.recordFocusAction_{
				//keyboardView.focus
			};

		this.marker_initVars;

		noteOnNodes = nil ! 128; // note on events store synth nodes so note off event can release them

	}

	iInitMIDI{
		this.useMIDIPipes;
		seqOutBuffer  = LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromSequencerBuffer(pipe) };
	}

	fromSequencerBuffer{|pipe| this.marker_pipeIn(pipe) }

	pipeIn{|pipe| this.marker_pipeIn(pipe) }

	// mode selecting /////////////////////////////////////////////////////////////////////////////////////////

	// clock in, select mode
	clockIn3{|instBeat,absTime3,latency,beat|

		if (mode===\repitch) {
			this.pitch_clockIn3 (instBeat,absTime3,latency,beat);
			//if (p[18].isFalse) { sequencer.clockIn3(beat,absTime,latency,beat) }; // i want pRoll 2nd for repeat reasons
			^this
		};

		if (mode===\marker ) {
			this.marker_clockIn3(instBeat,absTime3,latency,beat); // SHOULD BE BELOW SEQ BUT CAUSE HELD MARKERS
			if (p[18].isFalse) { sequencer.clockIn3(beat,absTime,latency,beat) };
			^this
		};

	}

	// and these events need to happen with latency

	clockStop {|latency|
		sequencer.do(_.clockStop(studio.actualLatency));
		seqOutBuffer.releaseAll(studio.actualLatency);
		this.marker_stopPlay;
		if (mode===\repitch) { this.pitch_stopBuffer (latency); ^this };
		if (mode===\marker ) { this.marker_stopBuffer(latency); ^this };
	}

	clockPause{|latency|
		sequencer.do(_.clockPause(studio.actualLatency));
		seqOutBuffer.releaseAll(studio.actualLatency);
		if (mode===\repitch) { this.pitch_stopBuffer (latency); ^this };
		if (mode===\marker ) { this.marker_stopBuffer(latency); ^this };
	}

	updateOnSolo{|latency|
		if (this.isOn) { ^this }; // is on exception, below is done when off
		sequencer.do(_.clockStop(studio.actualLatency));
		seqOutBuffer.releaseAll(studio.actualLatency);
		this.marker_stopPlay;
		if (mode===\repitch) { this.pitch_stopBuffer (latency); ^this };
		if (mode===\marker ) { this.marker_stopBuffer(latency); ^this }; // doesn't do anything, release above does
	}

	bpmChange	{
		if (mode===\repitch) { newBPM = true; ^this };
		//if (mode===\marker ) { this.updateVarsMarker; ^this };
	}

	// all these events must happen on the clock else sample playback will drift
	// should maintain sample accurate playback. to test

	updatePOS	{ relaunch = true; this.marker_stopPlay; }

	clockPlay	{ relaunch = true }

	jumpTo   	{ relaunch = true; this.marker_stopPlay; }

	*initUGens{|server|
		this.pitch_initUGens (server);
		this.marker_initUGens(server);
		this.record_initUGens(server);
	}

	// mixer synth stuFF /////////////////////////////////////////////////////////////////////////////////////////

	getMixerArgs{^[
		[\amp,           p[2].dbamp       ],
		[\outChannel,    LNX_AudioDevices.getOutChannelIndex(p[4])],
		[\pan,           p[5]             ],
		[\sendChannel,  LNX_AudioDevices.getOutChannelIndex(p[7])],
		[\sendAmp,       p[8].dbamp       ]
	]}

	updateDSP{|oldP,latency|
		if (instOutSynth.notNil) {
			server.sendBundle(latency +! syncDelay,
				*this.getMixerArgs.collect{|i| [\n_set, instOutSynth.nodeID]++i } );
		};
	}

	// disk i/o /////////////////////////////////////////////////////////////////////////////////////////

	// for your own saving
	iGetSaveList{ ^(sampleBank.getSaveListURL) ++ (sequencer.getSaveList) }

	// for your own loading
	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		sampleBank.putLoadListURL( l.popEND("*** END URL Bank Doc ***"));
		sampleBank.adjustViews;
		sequencer.putLoadList(l.popEND("*** END OBJECT DOC ***"));
	}

	iPostLoad{|noPre,loadVersion,templateLoadVersion|
		this.marker_makeSeq;
	}

	// free this
	iFree{
		sampleBank.free;
		webBrowser.free;
		sequencer.free;
		sampleBankGUI = nil;
		{LNX_SampleBank.emptyTrash}.defer(1); // empty trash 1 seconds later
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

	// GUI ////////////////////////////////////////////////////////////////////////////////////////////

	*thisWidth  {^870}
	*thisHeight {^600}

	createWindow{|bounds|
		this.createTemplateWindow(bounds,Color.black);
	}

	// create all the GUI widgets while attaching them to models
	createWidgets{

		gui[\knobTheme1]=(	\labelFont_   : Font("Helvetica",12),
							\numberFont_  : Font("Helvetica",12),
							\numberWidth_ : 0,
							\colors_      : (\on : Color(50/77,61/77,1), \numberDown : Color(0.66,0.66,1)/4),
							\resoultion_  : 1.5 );

		gui[\flatButton]=(	\rounded_: true,
							\font_:	  Font("Helvetica",12,true),
							\colors_:  ( \up: Color(50/77,61/77,1), \down: Color(1,1,1,0.88)/4 ) );

		gui[\scrollViewOuter] = MVC_RoundedComView(window, Rect(11,11,thisWidth-22,thisHeight-22-1))
			.color_(\background,Color.new255(122,132,132))
			.color_(\border, Color.new255(122,132,132)/2)
			.resize_(5);

		gui[\scrollView] = MVC_CompositeView(gui[\scrollViewOuter], Rect(0,0,thisWidth-22,thisHeight-22-1))
			.color_(\background,Color.new255(122,132,132))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);

		// the list scroll view
		gui[\sampleListCompositeView] = MVC_RoundedScrollView(gui[\scrollView], Rect(583, 40, 150, 41))
			.width_(3.5)
			.hasBorder_(true)
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false)
			.color_(\background, Color(0.5,0.45,0.45))
			.color_(\border, Color(0,0,0,0.35));

		gui[\speakerIcon] = MVC_Icon(gui[\scrollViewOuter], Rect(716, 17, 12, 12))
			.icon_(\speaker);

		sampleBank.speakerIcon_(gui[\speakerIcon]);

		// this is the list
		sampleBank.window2_(gui[\sampleListCompositeView]);

		// the sample editor
		sampleBankGUI = sampleBank.openMetadataEditor(
			gui[\scrollView],
			0,
			true,
			webBrowser,
			(border2: Color(59/108,65/103,505/692), border1: Color(0,1/103,9/77)),
			0,
			interface:\strangeLoop
		)[3];

		// 11. sample
		MVC_PopUpMenu3(gui[\scrollView], models[11], Rect(583, 14, 112, 17), gui[\menuTheme  ])
			.items_(sampleBank.names);

		// 12. transpose
		MVC_MyKnob3(gui[\scrollView], models[12], Rect(605, 200, 28, 28), gui[\knobTheme1]).zeroValue_(0)
			.resoultion_(5);

		// 13. fine
		MVC_MyKnob3(gui[\scrollView], models[13], Rect(667, 200, 28, 28), gui[\knobTheme1]).zeroValue_(0);

		// 14. fold/wrap
		MVC_PopUpMenu3(gui[\scrollView], models[14], Rect(686, 260, 80, 17), gui[\menuTheme]);

		// 18. play back mode
		MVC_PopUpMenu3(gui[\scrollView], models[18], Rect(593, 260, 80, 17), gui[\menuTheme]);

		// ***************** EVENT

		MVC_TabbedView(gui[\scrollView],Rect(580, 290, 250, 92), 0@0)
			.labels_([""])
			.tabWidth_([80])
			.tabCurve_(4)
			.labelColors_([Color(0,0,0,0.3)])
			.unfocusedColors_([Color(0,0,0,0.3)])
			.backgrounds_([Color(0,0,0,0.3)])
			.tabHeight_(32);

		MVC_TabbedView(gui[\scrollView],Rect(580, 356, 250, 155), 170@0)
			.tabPosition_('top')
			.labels_([""])
			.tabWidth_([80])
			.tabCurve_(4)
			.labelColors_([Color(0,0,0,0.3)])
			.unfocusedColors_([Color(0,0,0,0.3)])
			.backgrounds_([Color(0,0,0,0.3)])
			.tabHeight_(32);

		// freeze button
		gui[\freezeButton] = MVC_FlatButton(gui[\scrollView], Rect(590, 360, 50, 20),"Freeze", gui[\flatButton])
			.downAction_{ models[19].valueAction_(1,nil,true,false) }
			.upAction_{ models[19].valueAction_(0,nil,true,false) };

		gui[\eventLamp] = MVC_PipeLampView(gui[\scrollView],models[19], Rect(643,363,14,14))
			.doLazyRefresh_(false)
			.border_(true)
			.insetBy2_(1)
			.border2_(true)
			.mouseWorks_(true)
			.color_(\on,Color(50/77,61/77,1));

		MVC_FuncAdaptor(models[19]).func_{|me,val| gui[\freezeButton].down_(val.isTrue) };

		// 20. memory
		MVC_MyKnob3(gui[\scrollView], models[20], Rect(665, 308, 28, 28), gui[\knobTheme1]);

		// 15. repeat prob
		MVC_MyKnob3(gui[\scrollView], models[15], Rect(605, 308, 28, 28), gui[\knobTheme1]);

		// 16. repeat trans
		MVC_MyKnob3(gui[\scrollView], models[16], Rect(725, 308, 28, 28), gui[\knobTheme1])
			.zeroValue_(0)
			.resoultion_(5);

		// 17. repeat amp
		MVC_MyKnob3(gui[\scrollView], models[17], Rect(785, 308, 28, 28), gui[\knobTheme1]);

		// ***************** REVERSE

		// rev button
		gui[\revButton] = MVC_FlatButton(gui[\scrollView], Rect(676, 360, 40, 20),"Rev", gui[\flatButton])
			.downAction_{ models[21].valueAction_(1,nil,true,false) }
			.upAction_{ models[21].valueAction_(0,nil,true,false) };

		MVC_PipeLampView(gui[\scrollView],models[21], Rect(723,363,14,14))
			.doLazyRefresh_(false)
			.border_(true)
			.mouseWorks_(true)
			.color_(\on,Color(50/77,61/77,1));

		MVC_FuncAdaptor(models[21]).func_{|me,val| gui[\revButton].down_(val.isTrue) };

		// ***************** FRAME

		// frame button
		gui[\frameButton] = MVC_FlatButton(gui[\scrollView], Rect(774, 360, 50, 20), "Frame", gui[\flatButton])
			.downAction_{ models[22].valueAction_(1,nil,true,false) }
			.upAction_{ models[22].valueAction_(0,nil,true,false) };

		gui[\frameLamp] = MVC_PipeLampView(gui[\scrollView],models[22], Rect(755,363,14,14))
			.doLazyRefresh_(false)
			.border_(true)
			.insetBy2_(1)
			.border2_(true)
			.mouseWorks_(true)
			.color_(\on,Color(50/77,61/77,1));

		MVC_FuncAdaptor(models[22]).func_{|me,val| gui[\frameButton].down_(val.isTrue) };

		// 28. frame start
		MVC_MyKnob3(gui[\scrollView], models[28], Rect(605, 468, 28, 28), gui[\knobTheme1]);

		// 29. frame offset
		MVC_MyKnob3(gui[\scrollView], models[29], Rect(665, 468, 28, 28), gui[\knobTheme1]);

		// 23. frame length
		MVC_MyKnob3(gui[\scrollView], models[23], Rect(725, 468, 28, 28), gui[\knobTheme1]);

		// 30. reset / latch
		gui[\latchReset] = MVC_MyKnob3(gui[\scrollView], models[30], Rect(785, 468, 28, 28), gui[\knobTheme1]);

		// 24. repeat prob
		MVC_MyKnob3(gui[\scrollView], models[24], Rect(605, 405, 28, 28), gui[\knobTheme1]);

		// 25. memory
		MVC_MyKnob3(gui[\scrollView], models[25], Rect(665, 405, 28, 28), gui[\knobTheme1]);

		// 26. repeat trans
		MVC_MyKnob3(gui[\scrollView], models[26], Rect(725, 405, 28, 28), gui[\knobTheme1])
			.zeroValue_(0)
			.resoultion_(5);

		// 27. repeat amp
		MVC_MyKnob3(gui[\scrollView], models[27], Rect(785, 405, 28, 28), gui[\knobTheme1]);

		// 31. reset latch
		MVC_OnOffView(gui[\scrollView], models[31], Rect(790, 520, 40, 20))
			.strings_(["Reset","Latch"])
			.rounded_(true)
			.color_(\on,Color(50/77,61/77,1))
			.color_(\off,Color(1,1,1,0.88)/4);

		MVC_FuncAdaptor(models[31]).func_{|me,val|
			{gui[\latchReset].label_( val.isTrue.if("Latch","Reset") )} .deferIfNeeded
		};


		// *****************

		// piano roll
		sequencer.createWidgets(gui[\scrollView],Rect(3,220,567, 340)
				,(\selectRect:Color.white,
				 \background: Color(0.22, 0.22, 0.25)*0.66,
				 \velocityBG: Color(0.22, 0.22, 0.25),
				 \buttons:    Color(50/77,61/77,1),
				 \boxes:	  Color(0.1, 0.05, 0, 0.5),
				 \noteBG:     Color(50/77,61/77,1),
				 \noteBGS:    Color(50/77,61/77,1)*1.5,
				 \velocity:   Color(50/77,61/77,1),
				 \noteBS:     Color.black,
				 \velocitySel:Color(0.5,0.5,1)*1.5
				),
				parentViews: [ window]
				);

		// import button
		MVC_FlatButton(gui[\scrollView], Rect(242, 213, 49, 20), "Import", gui[\flatButton])
			.action_{this.marker_Import };

		// new button
		MVC_FlatButton(gui[\scrollView], Rect(768, 104, 40, 20), "New", gui[\flatButton])
			.action_{ this.guiNewBuffer };

		// record
		gui[\record]= MVC_OnOffView(gui[\scrollView], Rect(767, 71, 40, 20),"Rec")
			.action_{|me,val| if (me.value.isTrue) { this.guiRecord } { this.guiStopRecord } }
			.rounded_(true)
			.color_(\on,Color(50/77,61/77,1))
			.color_(\off,Color(1,1,1,0.88)/4);

		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],580@520,100,
			Color(0.8,0.8,1)/1.6,
			Color(0.7,0.7,1)/3,
			Color(0.7,0.7,1)/1.5,
			Color(0.77,1,1),
			Color.black
		);
		this.attachActionsToPresetGUI;

		///// highlight which repeat mode is used

		MVC_FuncAdaptor(guiModeModel).func_{|me,val|
			if (val==0) {
				gui[\frameLamp].color_(\border,Color.black);
				gui[\eventLamp].color_(\border,Color.black);
			};
			if (val==1) {
				gui[\frameLamp].color_(\border,Color.white);
				gui[\eventLamp].color_(\border,Color.black);
			};
			if (val==2) {
				gui[\frameLamp].color_(\border,Color.black);
				gui[\eventLamp].color_(\border,Color.white);
			};
		};

	}

	// highlight in gui which repeat mode is used
	guiHighlight{|mode, latency|
		if (mode!=previousMode) {
			{
				if (mode.isNil)	   { guiModeModel.lazyValueAction_(0) };
				if (mode===\frame) { guiModeModel.lazyValueAction_(1) };
				if (mode===\event) { guiModeModel.lazyValueAction_(2) };
			}.defer(latency);
		};
		previousMode = mode;
	}

} // end ////////////////////////////////////
