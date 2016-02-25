
// audio in & it can do +ive & -ive time syncing to studio clock!!!

LNX_VolcaBeats : LNX_InstrumentTemplate {

	classvar <defaultSteps=32, <defaultChannels=10, <>isVisiblePref;

	var <steps, <channels, <sequencers, <channelNames;
	var <channelOnSolo, <channelOnSoloGroup;

	*initClass{
		Class.initClassTree(LNX_File);
		isVisiblePref = ("KorgIsVisible".loadPref ? [true])[0].isTrue;
	}
	
	*saveIsVisiblePref{ [isVisiblePref].savePref("KorgIsVisible") }

	*isVisible{^isVisiblePref}

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	// an immutable list of methods available to the network
	interface{^#[\netChannelOnOff, \netChannelSolo]}

	*studioName      {^"Volca Beats"}
	*sortOrder       {^2}
	isInstrument     {^true}
	canBeSequenced   {^true}
	isMixerInstrument{^true}
	mixerColor       {^Color(0.653, 0.612, 0.544)} // colour in mixer
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
		instrumentHeaderType="SC Volca Beats Doc";
		version="v1.0";		
	}

	iInitVars{
		
		// will i ever allow a variable number of channels?
		channels = defaultChannels;

		channelNames=[
			"1.Kick", "2.Snare", "3.Lo Tom", "4.Hi Tom", "5.Closed", "6.Open", "7.Clap",
			"8.Claves", "9.Agogo", "10.Crash"
		];
		
		// the onSolos and their container
		channelOnSoloGroup=LNX_OnSoloGroup();
		channelOnSolo={|i|
			LNX_OnSolo(channelOnSoloGroup,1 , 0 ,i)  // 1 & 0 are onOff & solo
		} ! channels;
					
		// the lamps
		gui[\lamps]=LNX_EmptyGUIShell!defaultChannels;	
		
		// the main sequencer
		sequencers={|i|
			MVC_StepSequencer((id.asString++"_Seq_"++i).asSymbol,
								defaultSteps,midiControl,1000+(1000*i), \unipolar )
				.name_(channelNames[i])
				.action_{|velocity,latency|
					if (((p[51]>0)&&(this.isOff)).not) { this.bang(i,velocity,latency) }
				}
				.nameClickAction_{|seq|
					gui[\masterTabs].value_(i+1);
				}
		}! channels;

		
	}
	
	// bang the volca beats
	bang{|channel, velocity=1, latency|
		
		if (channelOnSolo[channel].isOff) {^this}; // exceptions: drop out if not on
		
		midi.control(channel+40, velocity*127*p[channel+21], latency +! syncDelay); // volume 40-49
		midi.noteOn([36,38,43,50,42,46,39,75,67,49][channel], 127, latency +! syncDelay);
		{gui[\lamps][channel].value_(velocity,0.1)}.defer(latency);
	}
		
	// clock in //////////////////////////////
	
	clockIn {|beat,latency| sequencers.do(_.clockIn(beat,latency)) }
	
	// reset sequencers posViews
	clockStop { sequencers.do(_.clockStop(studio.actualLatency)) }
	
	// remove any clock hilites
	clockPause{ sequencers.do(_.clockPause(studio.actualLatency)) }
	
	// clock in for midi out clock methods
	midiSongPtr {|songPtr,latency| if (p[52].isTrue) { midi.songPtr(songPtr,latency +! syncDelay)}} 
	midiStart   {|latency|         if (p[52].isTrue) { midi.start(latency +! syncDelay) } }
	midiClock   {|latency|         if (p[52].isTrue) { midi.midiClock(latency +! syncDelay) } }
	midiContinue{|latency|         if (p[52].isTrue) { midi.continue(latency +! syncDelay) } }
	midiStop    {|latency|         if (p[52].isTrue) { midi.stop(latency +! syncDelay) } }
	
	///////////////////////////////////////////////////////
	
	// the models
	initModel {
		
		var template=[

			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle|
					this.solo(val,latency,send,toggle);
				},
				\action2_ -> {|me|
					this.soloAlt(me.value);
				 }],
			
			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle|
					this.onOff(val,latency,send,toggle);
				},
				\action2_ -> {|me|	
					this.onOffAlt(me.value);
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
			[\sync, {|me,val,latency,send|
				this.setPVPModel(10,val,latency,send);
				this.syncDelay_(val);
			}],
			
			// 11. midiControl 50 PCM Speed "Clap",
			[\midi, midiControl, 11, "PCM Clap", (\label_:"PCM Clap" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(11, val, latency, send);    // network this
					midi.control(50, val, latency +! syncDelay); // send midi control data
				}],	
				
			// 12. midiControl 51 PCM Speed "Claves"
			[\midi, midiControl, 12, "PCM Claves", (\label_:"PCM Claves" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(12, val, latency, send);    // network this
					midi.control(51, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 13. midiControl 52 PCM Speed "Agogo"
			[\midi, midiControl, 13, "PCM Agogo", (\label_:"PCM Agogo" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(13, val, latency, send);    // network this
					midi.control(52, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 14. midiControl 53 PCM Speed "Crash"
			[\midi, midiControl, 14, "PCM Crash", (\label_:"PCM Crash" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(14, val, latency, send);    // network this
					midi.control(53, val, latency +! syncDelay); // send midi control data
				}],	

			// 15. midiControl 54 Shutter Time
			[0, \midi, midiControl, 15, "Shutter Time",
			(\label_:"Shutter Time" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(15, val, latency, send);    // network this
					midi.control(54, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 16. midiControl 55 Shutter Depth
			[0, \midi, midiControl, 16, "Shutter Depth",
			(\label_:"Shutter Depth" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(16, val, latency, send);    // network this
					midi.control(55, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 17. midiControl 56 Tom Decay
			[\midi, midiControl, 17, "Tom Decay", (\label_:"Tom Decay" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(17, val, latency, send);    // network this
					midi.control(56, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 18. midiControl 57 Closed Hat Decay
			[\midi, midiControl, 18, "Closed Decay",
			(\label_:"Closed Decay" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(18, val, latency, send);    // network this
					midi.control(57, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 19. midiControl 58 Open Hat Decay
			[\midi, midiControl, 19, "Open Decay", (\label_:"Open Decay" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(19, val, latency, send);    // network this
					midi.control(58, val, latency +! syncDelay); // send midi control data
				}],	
			
			// 20. midiControl 59 Hat Grain
			[\midi, midiControl, 20, "Hat Grain", (\label_:"Hat Grain" , \numberFunc_:'int'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(20, val, latency, send);    // network this
					midi.control(59, val, latency +! syncDelay); // send midi control data
				}],
		];
		
		template=template.extend(54,0);
		
		defaultChannels.do{|i|
			
			// 21-30 channel volumes
			template[21+i]=[0.71,\unipolar,midiControl, 21+i, "Volume"+(i+1),
				//(\numberFunc_:'db'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(21+i,val,latency,send);
				}];
				
			// 31-40. channel solo
			template[31+i]=[0, \switch, (\strings_:"S"), midiControl, 12+i, "Solo"+(i+1),
				{|me,val,latency,send,toggle| 
					this.channelSolo(i,val,latency,send,toggle)
			}];
			
			// 41-50. channel onOff
			template[41+i]=[1, \switch,  midiControl, 20+i, "OnOff"+(i+1),
				(\strings_:(i+1).asString),
				{|me,val,latency,send,toggle| 
					 this.channelOnOff(i,val,latency,send,toggle)
			}];
			
		};

		
		// 51. onSolo turns audioIn, seq or both on/off
		template[51]=[1, [0,2,\lin,1],  midiControl, 51, "On/Off Model",
				(\items_:["Audio In","Sequencer","Both"]),
				{|me,val,latency,send|
					this.setPVP(51,val,latency,send);
					this.updateOnSolo;
				}];

		// 52. midi clock out
		template[52] = [0, \switch, midiControl, 52, "MIDI Clock", (strings_:["MIDI Clock"]),
			{|me,val,latency,send|
				this.setPVPModel(52,val,latency,send);
				if (val.isFalse) { midi.stop(latency +! syncDelay) };
			}];				

		// 53. use controls in presets
		template[53] = [0, \switch, midiControl, 53, "Controls Preset", (strings_:["Controls"]),
			{|me,val,latency,send|	
				this.setPVPModel(53,val,latency,send);
				if (val.isTrue) {
					presetExclusion=[0,1,10,52];
				}{
					presetExclusion=[0,1,10,52]++(11..20);
				}	
			}];

		#models,defaults=template.generateAllModels;
	
		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,10,52]++(11..20);
		randomExclusion=[0,1,10,52];
		autoExclusion=[10];

	}


	
	// networking the models /////////////////////////////////////
	
	// onOff (channel)
	channelOnOff{|i,val,latency,send,toggle|
		p[41+i]=val;
		channelOnSolo[i].on_(val);
		if (send) { api.sendVP(id++\cOnOff++i,\netChannelOnOff,i,val) };
	}
	
	// and its net call
	netChannelOnOff{|i,val| models[41+i].lazyValueAction_(val,nil,false,false) }
	
	// solo (channel)
	channelSolo{|i,val,latency,send,toggle|
		p[31+i]=val;
		channelOnSolo[i].solo_(val);
		{this.refreshOnOffEnabled}.defer;
		if (send) { api.sendVP(id++\cSOLO++i,\netChannelSolo,i,val) };
	}
	
	// and its net call
	netChannelSolo{|i,val| models[31+i].lazyValueAction_(val,nil,false,false) }
	
	// refresh the enabled for onSolos
	refreshOnOffEnabled{
		if (channelOnSoloGroup.isSoloOn) {
			channels.do{|i| models[41+i].enabled_(false)};
		}{
			channels.do{|i| models[41+i].enabled_(true)};
		};
	}

	// disk i/o ///////////////////////////////
		
	// for your own saving
	iGetSaveList{
		var l;
		l=[channels];
		sequencers.do{|s| l=l++(s.getSaveList) };	
		^l	
	}
	
	// for your own loading
	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		var channels;		
		channels=l.popI; // not really used yet 
		sequencers.do{|s| s.putLoadList(l.popEND("*** END OBJECT DOC ***")) };
	}
	
	// anything that needs doing after a load
	iPostLoad{|noPre,loadVersion,templateLoadVersion|
		channels.do{|i| channelOnSolo[i].on_(p[41+i]).solo_(p[31+i])  };
		this.refreshOnOffEnabled;	
	}
	
	iFreeAutomation{ sequencers.do(_.freeAutomation) }
	
	// free this
	iFree{
		sequencers.do(_.free);
		channelOnSolo.do(_.free);
		channelOnSoloGroup = channelOnSolo = nil;
	}


	// PRESETS /////////////////////////
	
	// get the current state as a list
	iGetPresetList{
		var l;
		l=[sequencers[0].iGetPrestSize];
		sequencers.do{|s| l=l++(s.iGetPresetList) };
		^l
	}
	
	// add a statelist to the presets
	iAddPresetList{|l|
		var presetSize;
		presetSize=l.popI;
		sequencers.do{|s| s.iAddPresetList(l.popNF(presetSize)) };
	}
	
	// save a state list over a current preset
	iSavePresetList{|i,l|
		var presetSize;
		presetSize=l.popI;
		sequencers.do{|s| s.iSavePresetList(i,l.popNF(presetSize)) };
	}
	
	// for your own load preset
	iLoadPreset{|i,newP,latency|
		// maybe update so we keep seq
		sequencers.do{|s| s.iLoadPreset(i) };
	}
	
	// for your own remove preset
	iRemovePreset{|i|
		sequencers.do{|s| s.iRemovePreset(i) };
	}
	
	// for your own removal of all presets
	iRemoveAllPresets{ 
		sequencers.do{|s| s.iRemoveAllPresets };
	}
	
	// clear the sequencer
	clearSequencer{
		sequencers.do(_.clearSequencer);
	}

	// program stuff ////////////////////////*****************************************************
	//  this is overriden to force program changes & exclusions by put p[89 & 90] 1st
	loadPreset{|i,latency|
		var presetToLoad, oldP;
		
		presetToLoad=presetMemory[i].copy;
		
		// 29.use controls
		models[53].lazyValueAction_(presetToLoad[53],latency,false);

		// exclude these parameters
		presetExclusion.do{|i| presetToLoad[i]=p[i]};
		
		// update models
		presetToLoad.do({|v,j|	
			if (j!=53) {
				if (p[j]!=v) {
					models[j].lazyValueAction_(v,latency,false)
				}
			};
		});
		
		this.iLoadPreset(i,presetToLoad,latency);    // any instrument specific details
		oldP=p.copy;
		p=presetToLoad;               // copy the paramaters to p (is this needed any more?)
		this.updateDSP(oldP,latency); // and update any dsp
		
	}

	//////////////////////////*****************************************************
	// GUI
	
	*thisWidth  {^681}
	*thisHeight {^502}
	
	createWindow{|bounds| this.createTemplateWindow(bounds,Color(0.1221, 0.1221, 0.1221),true) }

	// create all the GUI widgets while attaching them to models
	createWidgets{
		
		var lastValue, lastY, lastX;
										
		gui[\menuTheme ]=( \font_		: Font("Arial", 10),
						\labelShadow_	: false,
						\colors_      : (\background: Color(0.6 , 0.562, 0.5),
										\label:Color.black,
										\string:Color.black,
									   \focus:Color.clear));
	
//		gui[\scrollTheme]=( \background	: Color(19/71,19/71,19/71),
//				 \border		: Color(0.6 , 0.562, 0.5));
	
		gui[\midiTheme]= ( \rounded_	: true,
						\canFocus_	: false,
						\shadow_		: true,
						\colors_		: (	\up 		: Color(0.31,0.31,0.49),
										\down	: Color(0.31,0.31,0.49),
										\string	: Color.white));
										
		gui[\knobTheme]=( \labelShadow_	: false,
						\numberWidth_	: (-20), 
						\numberFont_	: Font("Helvetica",10),
						\colors_		: (	\on		: Color.orange,
										\label	: Color.black,
										\numberUp	: Color.black,
										\numberDown : Color.white));
										
		gui[\knobTheme2]=(\colors_		: (\on :  Color.orange, 
									   \numberUp:Color.black,
									   \numberDown:Color.white,
									   \disabled:Color.black,
									   \label:Color.white),
						\labelShadow_ : true,
						\numberFont_  : Font("Helvetica",10),
						\labelFont_	: Font("Helvetica",11)
						);
								
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

		gui[\onOffTheme1]=( \font_		: Font("Helvetica-Bold", 12),
						 \rounded_	: true,
						 \colors_     : (\on : Color(20/77,1,20/77), \off: Color(0.4,0.4,0.4)));
										
		gui[\onOffTheme2]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color.orange, \off: Color(0.4,0.4,0.4)));
						 
		gui[\onOffTheme3]=( \font_		: Font("Helvetica-Bold", 12),
						 \rounded_	: true,
						 \colors_     : (\on : Color.orange, \off: Color(0.4,0.4,0.4)));
		
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));
		
	
		// widgets
		
		//gui[\scrollView] = MVC_RoundedComView(window,
		//					Rect(11,11,thisWidth-22,thisHeight-22-1), gui[\scrollTheme]);
						
		gui[\scrollView] = window;
						
		MVC_Text(gui[\scrollView],Rect(4, 180,137,25))
			.align_(\center)
			.shadow_(false)
			.penShadow_(false)
			.font_(Font("AvenirNext-Heavy",22))
			.string_("Volca Beats");
						
		// 3. in	
		MVC_PopUpMenu3(models[3],gui[\scrollView],Rect(5,5,70,17), gui[\menuTheme ] );
	
		// 9. channelSetup
		MVC_PopUpMenu3(models[9],gui[\scrollView],Rect(85,5,75,17), gui[\menuTheme ] );
		
		// 51. onSolo turns audioIn, seq or both on/off
		MVC_PopUpMenu3(models[51], gui[\scrollView] ,Rect(289, 5, 70, 17), gui[\menuTheme] );

		// 52. midi clock out
		MVC_OnOffView(models[52], gui[\scrollView], Rect(180, 16, 70, 18),gui[\onOffTheme3]);

		// 53. use controls in presets
		MVC_OnOffView(models[53], gui[\scrollView], Rect(289, 27, 70, 18),gui[\onOffTheme3]);
					
		// MIDI Settings
 		MVC_FlatButton(gui[\scrollView],Rect(20, 27, 43, 19),"MIDI")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(0.6 , 0.562, 0.5))
			.color_(\down,Color(0.6 , 0.562, 0.5) )
			.color_(\string,Color.white)
			.resize_(9)
			.action_{ this.createMIDIInOutModelWindow(window,
				colors:(border1:Color(0.1221, 0.0297, 0.0297), border2: Color(0.6 , 0.562, 0.5))
			) };
			
		// MIDI Control
 		MVC_FlatButton(gui[\scrollView],Rect(99, 27, 43, 19),"Cntrl")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(0.6 , 0.562, 0.5) )
			.color_(\down,Color(0.6 , 0.562, 0.5) )
			.color_(\string,Color.white)
			.resize_(9)
			.action_{  LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front  };
			

		//MVC_PlainSquare(gui[\scrollView],Rect(9,27, 4, 5)).color_(\off,border);
		
		gui[\masterTabs]=MVC_TabbedView(gui[\scrollView], Rect(9, 200, 670, 295), offset:(345@(-2)))
			.labels_(["Overview","1", "2", "3", "4", "5", "6", "7", "8", "9", "10" ])
			.font_(Font("Helvetica", 12))
			.tabPosition_(\top)
			.unfocusedColors_(Color(1,1,1,0.25)! 11)
			.labelColors_(  Color(0.6 , 0.562, 0.5)!11)
			.backgrounds_(  Color.clear!11)
			.tabCurve_(8)
			.tabWidth_([63]++(24!10))
			.tabHeight_(15)
			.followEdges_(true)
			.value_(0);
		
		// control tab
		
		gui[\allView] = MVC_RoundedCompositeView(gui[\masterTabs].mvcTab(0), Rect(4, 4, 654, 269))
			.color_(\border,  Color(0.6 , 0.562, 0.5))
			.color_(\background, Color.grey(0.357))
			.hasBorder_(false)
			.autoScrolls_(false)
			.hasHorizontalScroller_(false)
			.hasVerticalScroller_(false)
			.autohidesScrollers_(false);
			
		// levels
		MVC_FlatDisplay(this.peakLeftModel,gui[\scrollView],Rect(370, 44, 6, 110+15))
			.color_(\background,Color.white.alpha_(0.1));
		MVC_FlatDisplay(this.peakRightModel,gui[\scrollView],Rect(378, 44, 6, 110+15))
			.color_(\background,Color.white.alpha_(0.1));
		MVC_Scale(gui[\scrollView],Rect(6+370, 44, 2, 110+15))
			.color_(\background,Color.white.alpha_(0.1));

		// 2. channel volume
		MVC_SmoothSlider(gui[\scrollView],models[2],Rect(388, 44, 25, 110+15))
			.label_(nil)
			.showNumberBox_(false)
			.color_(\border,Color.grey(0.45))
			.color_(\hilite,Color.grey(0.35))
			.color_(\knob,Color.white);	
			
		// 1. channel onOff	
		MVC_OnOffView(models[1], gui[\scrollView], Rect(388, 5, 25, 16),gui[\onOffTheme1])
			.rounded_(true)
			.permanentStrings_(["On","On"]);
		
		// 0. channel solo
		MVC_OnOffView(models[0], gui[\scrollView], Rect(388, 24, 25, 16),gui[\soloTheme])
			.rounded_(true);
			
		channels.do{|i|

			gui[i] = MVC_RoundedCompositeView(gui[\masterTabs].mvcTab(i+1), Rect(4, 4, 654, 269))
				.color_(\border,  Color(1.0, 0.65, 0.3))
				.color_(\background, Color.grey(0.357))
				.hasBorder_(false)
				.autoScrolls_(false)
				.hasHorizontalScroller_(false)
				.hasVerticalScroller_(false)
				.autohidesScrollers_(false);
			
			// channel volume
			MVC_SmoothSlider(gui[\scrollView],models[21+i],Rect(421+(i*25),39+5,20,110+15))
				.showNumberBox_(false)
				.color_(\border,Color.grey(0.35))
				.color_(\hilite,Color.grey(0.25));	
				
			// lamps
			gui[\lamps][i]=MVC_LampView(gui[\scrollView],Rect(423+(i*25),162+14,15,15))
				.color_(\on,Color.orange)
				.color_(\off,Color.orange/8)
				.action_{|me| this.bang(i,100/127) };	
			
			// 41-50. channel onOff	
			MVC_OnOffView(models[41+i], gui[\scrollView],
				Rect(420+(i*25),5,21,16),gui[\onOffTheme2])
				.rounded_(true);
			
			// 31-40. channel solo
			MVC_OnOffView(models[31+i], gui[\scrollView],
				Rect(420+(i*25),19+5,21,16),gui[\soloTheme])
				.rounded_(true);	
				
		};
				
		// sequencers
		sequencers.do{|s,i|
			
			var adj = [0,-4,0,-4,0,-4,0,-4,-8,-12];
			
			s.createButtonWidgets(gui[\allView] , Rect(10,16+(i*26)-5+adj[i], 675, 250),
				 ( \background:Color(0,0,0,0.8), \on:Color.orange,
							\border:Color(0.5,0.66,1,1), \string:Color.white,
							\rulerOn: Color.white, \rulerBackground : Color (1,1,1,0.4),
							\position : Color.white, \pinOn : Color.orange  )
				
				);
				
			s.createRoundWidgets(gui[i],Rect(12,65,610,150),
				(\hilite:Color.orange, \on:Color.orange),'outer');
				
			MVC_Text(gui[i],Rect(250, 8,150, 44))
				.align_(\center)
				.shadow_(false)
				.penShadow_(true)
				.font_(Font("AvenirNext-Heavy",22))
				.string_(channelNames[i]);
		
		};
		
		// move all <->	
		MVC_UserView(gui[\allView],Rect(550,0,35,20))
			.drawFunc_{|me|
				if (lastValue==nil) {Color.black.set} {Color.white.set};
				DrawIcon( \lineArrow, Rect(-3,-1,me.bounds.width+2,me.bounds.height+2) , pi);
				DrawIcon( \lineArrow, Rect(3,-1,me.bounds.width+2,me.bounds.height+2) );
			}
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				lastX=x;
				lastValue=0;
				me.refresh;
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var val = (x-lastX).div(15);
				var offset = (lastValue-val);
				lastValue = val;
				if (offset!=0) { sequencers.do{|seq| seq.move(offset) } };
				me.refresh;
			}
			.mouseUpAction_{|me|
				lastX=nil;
				lastValue=nil;
				me.refresh;
			};
				
		// ruler
		MVC_StaticText(gui[\allView],Rect(604-18,1,20,16))
			.align_(\center)
			.shadowDown_(false)
			.alwaysDown_(true)
			.font_(Font("Helvetica",12))
			.string_("R")
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				lastY=y;
				lastValue=0;
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var val = (y-lastY).div(10);
				var adj = (lastValue-val);
				lastValue = val;
				sequencers.do{|seq| seq.addValueToSP(4,adj)};
			};
			
		// speed
		MVC_StaticText(gui[\allView],Rect(625-18,1,20,16))
			.align_(\center)
			.shadowDown_(false)
			.alwaysDown_(true)
			.font_(Font("Helvetica",12))
			.string_("Sp")
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				lastY=y;
				lastValue=0;
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var val = (y-lastY).div(10);
				var adj = (lastValue-val);
				lastValue = val;
				sequencers.do{|seq| seq.addValueToSP(6,adj)};
			};
			
		// steps	
		MVC_StaticText(gui[\allView],Rect(646-19,1,20,16))
			.align_(\center)
			.shadowDown_(false)
			.alwaysDown_(true)
			.font_(Font("Helvetica",12))
			.string_("St")
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				lastY=y;
				lastValue=0;
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var val = (y-lastY).div(10);
				var adj = (lastValue-val);
				lastValue = val;
				sequencers.do{|seq| seq.addValueToSP(3,adj)};
			};
			
		// composite view for controls
		gui[\controlView] = MVC_RoundedCompositeView(gui[\scrollView],Rect(15, 35+22, 345, 116))
			.forceHold_(true)
			.color_(\border, Color(0.6 , 0.562, 0.5))
			.color_(\background, Color.grey(0.3))
			.width_(6);
		
		// 11. midiControl 50 PCM Speed "Clap",
		MVC_MyKnob3(models[11], gui[\controlView], Rect(18, 75, 28, 28),gui[\knobTheme2]);
		// 12. midiControl 51 PCM Speed "Claves"
		MVC_MyKnob3(models[12], gui[\controlView], Rect(88, 75, 28, 28),gui[\knobTheme2]);
		// 13. midiControl 52 PCM Speed "Agogo"
		MVC_MyKnob3(models[13], gui[\controlView], Rect(158, 75, 28, 28),gui[\knobTheme2]);
		// 14. midiControl 53 PCM Speed "Crash"
		MVC_MyKnob3(models[14], gui[\controlView], Rect(228, 75, 28, 28),gui[\knobTheme2]);
		// 15. midiControl 54 Shutter Time
		MVC_MyKnob3(models[15], gui[\controlView], Rect(18, 15, 28, 28),gui[\knobTheme2]);
		// 16. midiControl 55 Shutter Depth
		MVC_MyKnob3(models[16], gui[\controlView], Rect(88, 15, 28, 28),gui[\knobTheme2]);
		// 17. midiControl 56 Tom Decay
		MVC_MyKnob3(models[17], gui[\controlView], Rect(158, 15, 28, 28),gui[\knobTheme2]);
		// 18. midiControl 57 Closed Hat Decay
		MVC_MyKnob3(models[18], gui[\controlView], Rect(228, 15, 28, 28),gui[\knobTheme2]);
		// 19. midiControl 58 Open Hat Decay
		MVC_MyKnob3(models[19], gui[\controlView], Rect(298, 15, 28, 28),gui[\knobTheme2]);
		// 20. midiControl 59 Hat Grain
		MVC_MyKnob3(models[20], gui[\controlView], Rect(298, 75, 28, 28),gui[\knobTheme2]);
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],145@187,105,
				Color(0.7,0.65,0.65)/1.6,
				Color(0.7,0.65,0.65)/3,
				Color(0.7,0.65,0.65)/1.5,
				Color(0.7,0.65,0.65),
				Color.black
			);
		this.attachActionsToPresetGUI;	

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
		switch (p[51].asInt)
			{0} {
				// "Audio In"
				if (node.notNil)
					{server.sendBundle(latency,[\n_set, node, \on, this.isOn.asInt])};
			}
			{1} {
				// "Sequencer"
				if (node.notNil) {server.sendBundle(latency,[\n_set, node, \on, true.asInt])};
			}
			{2} {
				// "Both"
				if (node.notNil)
					{server.sendBundle(latency,[\n_set, node, \on, this.isOn.asInt])};
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
		if (p[51]==1) { on=true } { on=this.isOn };
				
		server.sendBundle(latency,
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
