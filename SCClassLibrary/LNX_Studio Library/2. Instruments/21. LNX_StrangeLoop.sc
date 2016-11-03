///////////////////////
//  LNX_StrangeLoop //
//////////////////////////////////////////////////////////////////////////////////////////////////////
// Possible modes are...
//	  repitch : sample rate is changed to fit loop length
//	  marker  : sample rate is fixed and event happen on markers. clip or fold to extend.
//    grain   : grains are streched to fit loop length & Grain events happen on markers
//
// Think about...
//    clock offset start, one shot, zeroX, tap bpm, stopping sampleRefresh when not following
//
// BUGS: !!!
//
// focus is lost when adding samples now
// also funcs called a lot when selecting sample
//  entering numbers via keyboard to tempo doesn't update LNX_InstrumentTemplate:bpmChange

LNX_StrangeLoop : LNX_InstrumentTemplate {

	var <sampleBank, <webBrowser;

	var <relaunch = false, <newBPM = false;

	var <mode = \repitch; // normal, repitch, grains, etc..

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName		 {^"Strange Loop"}
	*sortOrder		 {^1.5}
	*isVisible		 {^true}
	isInstrument	 {^true}
	canBeSequenced	 {^true}
	isMixerInstrument{^true}
	mixerColor		 {^Color(0.75,0.75,1,0.4)} // colour in mixer
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
					sampleBank.allInterfacesSelect(val,false);
			}],

		].generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1,10];
		autoExclusion=[];

	}

	iInitVars{
		// user content !!!!!!!
		sampleBank = LNX_SampleBank(server,apiID:((id++"_url_").asSymbol))
				.selectedAction_{|bank,val,send=true|
					models[11].valueAction_(val,nil,true);
					relaunch = true;
				}
				.itemAction_{|bank,items,send=false|
					models[11].controlSpec_([0,sampleBank.size,\linear,1]);
					{models[11].dependantsPerform(\items_,bank.names)}.defer;
				}
				.metaDataUpdateFunc_{|me,model|
					if (mode===\repitch) { this.updateRepitch(model) };
				}
				.title_("");

		// the webBrowsers used to search for new sounds!
		webBrowser = LNX_WebBrowser(server,sampleBank);

	}

	// mode selecting /////////////////////////////////////////////////////////////////////////////////////////

	// clock in, select mode
	clockIn3{|instBeat,absTime3,latency,beat|
		if (mode===\repitch) { this.clockInRepitch(instBeat,absTime3,latency,beat); ^this };
	}

	// and these events need to happen with latency

	clockStop {|latency|
		if (mode===\repitch) { this.stopBufferRepitch(latency); ^this };
	}

	clockPause{|latency|
		if (mode===\repitch) { this.stopBufferRepitch(latency); ^this };
	}

	// all these events must happen on the clock else sample playback will drift
	// should maintain sample accurate playback. to test

	updatePOS	{ relaunch = true }
	bpmChange	{ newBPM   = true }
	clockPlay	{ relaunch = true }
	jumpTo   	{ relaunch = true }

	*initUGens{|server|
		this.initUGensRepitch(server);
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
	iGetSaveList{ ^sampleBank.getSaveListURL }

	// for your own loading
	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		sampleBank.putLoadListURL( l.popEND("*** END URL Bank Doc ***"));
		sampleBank.adjustViews;
		if (sampleBank.size>0) { sampleBank.allInterfacesSelect(0) };
	}

	iFree{
		sampleBank.free;
		webBrowser.free;
	}

	// GUI ////////////////////////////////////////////////////////////////////////////////////////////

	*thisWidth  {^800}
	*thisHeight {^400}

	createWindow{|bounds|
		this.createTemplateWindow(bounds,Color(0.15,0.15,0.15));
	}

	// create all the GUI widgets while attaching them to models
	createWidgets{

		gui[\knob2Theme]=(	\labelFont_   : Font("Helvetica",12),
						\numberFont_	: Font("Helvetica",12),
						\numberWidth_ : 0,
						\colors_      : (\on : Color(1,0.5,0),
									   \numberDown : Color(1,0.5,0)/4),
						\resoultion_	: 1.5 );

		gui[\scrollViewOuter] = MVC_RoundedComView(window,
									Rect(11,11,thisWidth-22,thisHeight-22-1))
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border, Color(6/11,42/83,29/65))
			.resize_(5);

		gui[\scrollView] = MVC_CompositeView(gui[\scrollViewOuter] ,
				Rect(0,0,thisWidth-22,thisHeight-22-1))
			.color_(\background,Color(6/11,42/83,29/65))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);

		// the list scroll view
		gui[\sampleListCompositeView] = MVC_RoundedScrollView(gui[\scrollView],
				Rect(160, 230, 315, 101))
			.width_(3.5)
			.hasBorder_(true)
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false)
			.color_(\background, Color(0.5,0.45,0.45))
			.color_(\border, Color(0,0,0,0.35));

		gui[\speakerIcon] = MVC_Icon(gui[\scrollViewOuter], Rect(386,210, 18, 18).insetBy(3,3))
			.icon_(\speaker);

		sampleBank.speakerIcon_(gui[\speakerIcon]);

		// this is the list
		sampleBank.window2_(gui[\sampleListCompositeView]);

		// the sample editor
		sampleBank.openMetadataEditor(
			gui[\scrollView],
			0,
			true,
			webBrowser,
			(border2: Color(59/108,65/103,505/692), border1: Color(0,1/103,9/77)),
			200,
			interface:\strangeLoop
		);

		// 11. sample
		MVC_PopUpMenu3(gui[\scrollView], models[11], Rect(20, 340, 112, 17),gui[\menuTheme  ])
			.items_(sampleBank.names);

	}

} // end ////////////////////////////////////
