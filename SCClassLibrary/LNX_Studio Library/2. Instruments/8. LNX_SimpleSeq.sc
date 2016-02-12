
// a multi-purpose step sequencer

LNX_SimpleSeq : LNX_InstrumentTemplate {

	var <seq, <midiNotes, <steps, <channels,
	    <bangsOn, <bangNumber, <interpNo, <lastCCvalue, <wasOn,
	    <defaultSteps=32, <defaultChannels=8,  <defaultSP, <sP;
		
	var <channelNames, <nameViews, <seqPresetMemory, <sPPresetMemory;

	var	<seqModels, <seqMVCViews;

	var	<spModels, <spMVCViews,  <posModels;
	
	var	<midiSet;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}
	
	*studioName  {^"Step Sequencer"}
	isMIDI       {^true}
	*sortOrder   {^1.99}
	isInstrument {^true}
	onColor      {^Color(0.5,0.7,1)} 
	clockPriority{^3}
	alwaysOnModel{^models[2]}
	alwaysOn     {^models[2].isTrue} // am i? used by melody maker to change onOff widgets
	canAlwaysOn  {^true} // can i?
	syncModel    {^models[3]}

	header { 
		// define your document header details
		instrumentHeaderType="SC SimpleSequencer Doc";
		version="v1.3";		
	}

	// an immutable list of methods available to the network
	interface{^#[
		\netSeq, \netChannelItem, \netSteps, \netRuler, \netChangeName, \stopInterpAndSet
	]}

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
				
			// 2. Always ON
			[0, \switch, (\strings_:"Always"), midiControl, 2, "Always On",
				{|me,val,latency,send,toggle|
					this.setPVPModel(2,val,latency,send);
					if ((val==0)&&(instOnSolo.isOff)) {this.stopAllNotes}; 
					{studio.updateAlwaysOn(id,val.isTrue)}.defer;
				}],
				
			// 3. syncDelay
			[[-1,1,\lin,0.001,0], {|me,val,latency,send|
				this.setPVP(3,val,latency,send);
				this.syncDelay_(val);
			}],	
			
		].generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,3];
		randomExclusion=[0,1,3];
		autoExclusion=[3];
		
	}
	
	// your own vars
	iInitVars{
		channels=defaultChannels;
		steps=defaultSteps ! channels;
		seq=(-1) ! defaultSteps ! channels;
		
		midiNotes=[];
		channels.do({|i| midiNotes=midiNotes.add(60+i)});
		
		// new
		
		posModels = {0.asModel} ! channels;
		seqMVCViews = {LNX_EmptyGUIShell ! defaultSteps} ! channels;
		seqModels = {nil ! defaultSteps} ! channels;
		
		channels.do{|y|
			// seq
			defaultSteps.do{|x|
				seqModels[y][x]=[-1, [-1,127,\linear,1], (zeroValue_: 0),
					midiControl,(y+1)*100+10+x,
						"Seq ch:"++((y+1).asString)++" Step:"++((x+1).asString),
					{|me,val,latency,send| this.setSeq(y,x,val,latency,send) }].asModel;
			};
		
		};
		
		// new MVC seq controls
		
		spModels=Array.newClear(channels);
		sP=Array.newClear(channels);
		
		channels.do{|y|
			var mods,sPs;
			#mods,sPs= [
			
				// 0. on / Off
				[ 1, \switch, midiControl, (y+1)*100+0, "On/Off ch:"++(y+1),
					{|me,val,latency,send| this.setChannelItem(y,0,val,latency,send) }],
			
				// 1. duration 
				[ 0.5, [0.014,1], midiControl,(y+1)*100+1, "Duration ch:"++(y+1),
					(label_:"Dur", numberFunc_:'float2'),
					{|me,val,latency,send| this.setChannelItem(y,1,val,latency,send) }],
			
				// 2. midi note (there should be a 3rd action here when the value is changed)
				[36+y,[0,127,\linear,1], midiControl,(y+2)*100+2, "MIDI ch:"++(y+1),
					(label_:"MIDI", numberFunc_:'int'),
					{|me,val,latency,send|
						// this test stops breaking symmetry
						if ((network.isConnected)
							and: {studio.isPlaying}
							and: {instOnSolo.groupSongOnOff==1}
							and: {sP[y][0]==1}
							and: {sP[y][7].isTrue}) {
								me.lazyValue_(sP[y][2],false);
						}{
							this.setChannelItem(y,2,val,latency,send);
						};
					},
					\actions_ -> (\action2Down -> {|me|
						if (sP[y][7].isFalse) {
							this.bang(sP[y][2],100,sP[y][1]);
						};
					})
				],
				
				// 3. no of steps
				[ defaultSteps, [1,defaultSteps,\lin,1],
					midiControl, (y+1)*100+3, "Steps ch:"++(y+1),
					(label_:"Steps"),
					{|me,val,latency,send| this.setSteps(y,val,latency,send) }],	         
				// 4. ruler
				[ 4, [2,16,\lin,1], midiControl, (y+1)*100+4, "Ruler ch:"++(y+1),
					(label_:"Ruler"),
					{|me,val,latency,send| this.setRuler(y,val,latency,send) }],
					
				// 5. velocity adjust
				[ 0, [-127,127], midiControl,(y+1)*100+5, "Adjust ch:"++(y+1),
					(label_:"Adj", zeroValue_: 0,
						numberFunc_:{|n| n=n.round; if (n>0) {"+"++(n.asString)} {n} }),
					{|me,val,latency,send| this.setChannelItem(y,5,val,latency,send) }],
			
				// 6. speed divider
				[ 2, [1,32,\lin,1], midiControl, (y+1)*100+6, "Speed ch:"++(y+1),
					(label_:"Speed"),
					{|me,val,latency,send| this.setChannelItem(y,6,val,latency,send) }],
				
				// 7. type 0=midiNote, 1=midiControl
				[ 1, \switch, midiControl, (y+1)*100+7, "Mode:Note/CC ch:"++(y+1),
					{|me,val,latency,send| this.setChannelItem(y,7,val,latency,send) }],
				
				// 8. interpolation  (x1,x2,x3,x4,x6,x8,x12,x16)  index:(0-7)           
				[ 0, [0,7,\lin,1], midiControl, (y+1)*100+8, "Interpolation ch:"++(y+1),
					(label_:"Interp", items_:#["x1","x2","x3","x4","x6","x8","x12","x16"]),
					{|me,val,latency,send| this.setChannelItem(y,8,val,latency,send) }]
					 
			].generateAllModels;
			spModels[y]=mods;
			sP[y]=sPs;
		};
		defaultSP=sP[0].copy;
			
		interpNo = 0 ! channels;
		lastCCvalue = nil ! channels;
		wasOn = true ! channels;
		
		channelNames = "" ! channels;
		nameViews = LNX_EmptyGUIShell ! channels;
		
		bangsOn=[0] ! 128; // one for each midi note 
		bangNumber= [0] ! 128; // (this is no. times played for note removal)

		seqPresetMemory=[];
		sPPresetMemory=[];
		
		sP.do({|i,j| sP[j].put(2,36+j) });
			
	}
	
	*thisWidth  {^686+25}
	*thisHeight {^470+25+3}

	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(0,0,0.2),resizable: true);
	}

	createWidgets{
	
		var cH=144, osY=17;
		
	// Themes
		
		
		gui[\soloTheme ]=( \font_		: Font("Helvetica-Bold", 12),
						\colors_      : (\on : Color(1,0.2,0.2), \off : Color(0.4,0.4,0.4)));

		gui[\onOffTheme1]=( \font_		: Font("Helvetica", 12),
						 \colors_     : (\on : Color(0.25,1,0.25), \off : Color(0.4,0.4,0.4)));
		
		gui[\onOffTheme2]=( \font_		: Font("Helvetica-Bold", 12),
						 \colors_     : (\on : Color(0.25,1,0.25), \off : Color(0.4,0.4,0.4)));
						 
		gui[\numTheme]  =(	\orientation_  : \horizontal,
						\resoultion_	 : 1,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.2,0.2,0.2),
										\string : Color(0.7,0.7,1),
										\focus : Color(0,0,0,0)));
										
		gui[\button] = (	\rounded_		: true,
						\shadow_		: true,
						\canFocus_	: false,
						\colors_		: (\up    : Color(0.7,0.7,1)/1.5,
									   \down  : Color(0.5,0.5,1.0)/4,
									   \string:Color.white));
						
		gui[\knobTheme]=(	\labelFont_ 	: Font("Helvetica",10),
						\numberFont_	: Font("Helvetica",10),
						\numberWidth_	: -22,
						\colors_		: ( \numberUp : Color.black, \numberDown : Color.purple,
										\on : Color.purple));
		
			
		gui[\scrollTheme]=( \background	: Color(0.6, 0.6, 0.6),
						 \border		: Color(0.36, 0.24, 0.6));
						
	// widgets
	
		gui[\scrollView] = MVC_RoundedComView(window,
							Rect(11,11,thisWidth-22,thisHeight-22-1), gui[\scrollTheme]);
							
		gui[\seqView] = MVC_ScrollView(gui[\scrollView],
									Rect(10,32,bounds.width-18-25, bounds.height-40-25))
			.color_(\background,Color(0.35,0.35,0.35,0.6))
			.color_(\border,Color(0.15, 0.15, 0.225))
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false)
			.hasBorder_(true)
			.resize_(5);
	
		// 1.on/off
		MVC_OnOffView(models[1],gui[\scrollView] ,Rect( 10, 6,22,19),gui[\onOffTheme1])
			.rounded_(true)
			.permanentStrings_(["On"]);
			
		// 0.solo
		MVC_OnOffView(models[0],gui[\scrollView] ,Rect( 37, 6,20,19),gui[\soloTheme  ])
			.rounded_(true);

		// 1.always on
		MVC_OnOffView(models[2],gui[\scrollView] ,Rect(65, 6,75,19),gui[\onOffTheme1])
			.rounded_(true)
			.permanentStrings_(["Always On"]);
						
		defaultChannels.do({|y|
			
			var yOffset=y*cH+2;
			
			// dividing line
			MVC_PlainSquare(gui[\seqView],Rect(0,((y+1)*cH+2)+osY-18,71 + (32*18) + 8,1))
				.color_(\off,((y==7).if(Color(0.5,0.5,1),Color.black)));

			// sliders
			defaultSteps.do({|x|
				var c;
				c=((x)%(sP[y][4])).map(0,sP[y][4],1,0.4);
				seqMVCViews[y][x]=MVC_PinSlider(gui[\seqView],seqModels[y][x],
							Rect(71+(x*18), 21+(yOffset)+osY, 17, cH-53+3))
					.seqItems_(seqMVCViews[y])
					.hiliteMode_(\inner)
					.color_(\background,Color.black)
					.color_(\on,Color(c*0.5,c*0.3,c*1))
					.color_(\hilite,Color(c*0.5,c*0.3,c*1)/1.7);
			});
			
			// 0. on / Off
			MVC_OnOffView(spModels[y][0],gui[\seqView],Rect(3,yOffset+osY-15,19,16))
				.rounded_(true)
				.strings_((y+1).asString)
				.color_(\on,Color.ndcOnOffON2)
				.color_(\off,Color(0.7,0.7,0.7));
				
			// 7. type 0=midiNote, 1=midiControl
			MVC_OnOffView(spModels[y][7],gui[\seqView],Rect(24,yOffset+osY-15,42,16))
				.rounded_(true)
				.strings_(["Note","Control"])
				.font_(Font("Helvetica",11))
				.color_(\on,Color(0.5,0.5,1))
				.color_(\off,Color.purple);
					
			// learn button
			MVC_FlatButton(gui[\seqView],Rect(71-3,yOffset+osY-15,35+3,16),"Learn")
				.rounded_(true)
				.action_{
					var name;
					name=midi.learn(sP[y][2],64);
					this.addTextToName(y,name);
				}
				.font_(Font("Helvetica-Bold",11))
				.color_(\focus,Color.grey(alpha:0.05))
				.color_(\string,Color.black)
				.color_(\down,Color(0.5,0.5,1.0)/4)
				.color_(\up,Color(0.7,0.7,1)/1.5);
				
			// learn name ( look at nameSafe we use : alot in control names )
			nameViews[y]=MVC_Text(gui[\seqView],Rect(107,yOffset+osY-15,468,16))
				.string_("")
				.actions_(\stringAction,{|me|
					var string=me.string.nameSafe;
					me.string_(string);
					this.changeName(y,string);
				})
				.shadow_(false)
				.canEdit_(true)
				.maxStringSize_(100)
				.color_(\background,Color.grey/4)
				.color_(\focus,Color.grey(alpha:0))
				.color_(\string,Color(0.6,0.6,1))
				.color_(\cursor,Color.white)
				.font_(Font("Helvetica",12));
						
			//  8. interpolation  (x1,x2,x3,x4,x6,x8,x12,x16)  index:(0-7)
			MVC_PopUpMenu3(spModels[y][8],gui[\seqView],Rect(608,(yOffset)+osY-15,38,16))
				.color_(\background,Color(0.7,0.7,1)/1.5)
				.font_(Font("Helvetica-Bold",11))
				.orientation_(\horizontal)
				.font_(Font("Helvetica-Bold",11))
				.labelFont_(Font("Helvetica",10))
				.canFocus_(false);

			// 4.ruler	
	 	 	MVC_NumberBox(spModels[y][4],gui[\seqView],Rect(42,yOffset+20,23,17),gui[\numTheme]);
	 	  	MVC_RulerView(spModels[y][4],gui[\seqView],
	 	  								Rect(70,yOffset+osY+3,defaultSteps*18,15))
	 	   		.label_(nil).steps_(defaultSteps)
	 	   		.color_(\on,Color.black)
	 	   		.color_(\string,Color.white*0.7)
				.color_(\background,Color(0,0,0,0.2));
		
			// 3.steps
			MVC_NumberBox(spModels[y][3],gui[\seqView],Rect(42,yOffset+38,23,17),gui[\numTheme]);
			
			// 6. speed divider
			MVC_NumberBox(spModels[y][6],gui[\seqView],Rect(42,yOffset+56,23,17),gui[\numTheme]);
			
			// 2. midi note 
			MVC_NumberBox(spModels[y][2],gui[\seqView],Rect(42,yOffset+74,23,17),gui[\numTheme]);

			// pos
		  	MVC_PosView(posModels[y],gui[\seqView],Rect(70,(y+1)*cH+osY-28+2,32*18,9))
				.color_(\on,Color(0.55,0.55,1))
				.type_(\circle)
				.color_(\background,Color.clear);
				
			// pos Hilite Adaptor
			gui[\posHiliteAdaptor]=MVC_HiliteAdaptor(posModels[y])
				.refreshZeros_(false)
				.views_(seqMVCViews[y]);
					
			// 1. duration
			MVC_MyKnob3(spModels[y][1],gui[\seqView],Rect(11,yOffset+osY+89,26,26),gui[\knobTheme]);

			// 5. adjust
			MVC_MyKnob3(spModels[y][5],gui[\seqView],Rect(41,yOffset+osY+89,26,26),gui[\knobTheme]);

		});
		
		// MIDI Settings
 		MVC_FlatButton(gui[\scrollView],Rect(174, 6, 43, 19),"MIDI",gui[\button])
			.action_{ this.createMIDIInOutModelWindow(window,nil,nil,
				colors:(border1:Color(0,0,0.2), border2:Color(0.36, 0.24, 0.6))
			)};
		
		// midi control button
		MVC_FlatButton(gui[\scrollView],Rect(228, 6, 43, 19),"Cntrl",gui[\button])
			.action_{ LNX_MIDIControl.editControls(this); LNX_MIDIControl.window.front };
				
		MVC_FlatButton(gui[\scrollView],Rect(282 , 6, 43, 19),"All",gui[\button])
			.action_{ LNX_MIDIControl.editControls(studio); LNX_MIDIControl.window.front };
			
		// 31. sync
		MVC_NumberBox(models[3],gui[\scrollView], Rect(380, 6, 43, 17))
			.labelShadow_(false)
			.label_("Sync")
			.orientation_(\horizontal)
			.color_(\label,Color.black);
		
		// the preset interface
		presetView=MVC_PresetMenuInterface(gui[\scrollView],(375+134-50)@(6),75+50,
				Color.grey,Color(0.7,0.7,1)/3,Color(0.7,0.7,1)/1.5);
		this.attachActionsToPresetGUI;	
	}
	
	changeSeqColours{|y,value|  // value is ruler size
		defaultSteps.do{|x|	
			var c,col;
			c=((x)%value).map(0,value,1,0.4);
			seqMVCViews[y][x]
				.color_(\on,Color(c*0.5,c*0.3,c*1))
				.color_(\hilite,Color(c*0.5,c*0.3,c*1)/1.7);
		}	
	}

	///////////////////////////
	
	// any post midiInit stuff
	iInitMIDI{ midi.putLoadList([0, 0 ]++LNX_MIDIPatch.nextUnusedOut) }
	
	// anything else that needs doing after a server reboot; 
	iServerReboot{}
		
	// for your own clear, used to when loading studio preset 
	iClear{}
	
	iFreeAutomation{
		seqModels.do{|c| c.do(_.freeAutomation) };
		spModels.do{|c| c.do(_.freeAutomation)  };
	}
	
	// for freeing anything when closing
	iFree{
		seqModels.do{|c| c.do(_.free)};
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
		seq=seqPresetMemory[i].deepCopy;
		sP=sPPresetMemory[i].deepCopy;
		{this.iUpdateGUI}.defer;
	}
	
	 // for your own remove preset
	iRemovePreset{|i| seqPresetMemory.removeAt(i); sPPresetMemory.removeAt(i);}
	
	// for your own removal of all presets
	iRemoveAllPresets{ seqPresetMemory=[]; sPPresetMemory=[] }
	
	///////////////////////////
	
	// for your own saving
	iGetSaveList{
		var l;
		l=[channels,sP[0].size];
		channels.do{|y|
			l=l++sP[y];
			l=l++seq[y];
			l=l.add(channelNames[y]);
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
	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		var sPSize;
		l.pop; // to use later when you can change no of channels
		sPSize=l.pop.asInt;
		channels.do{|y|
			sP[y]=l.popNF(sPSize);
			sP[y]=sP[y]++defaultSP[(sP[y].size)..(defaultSP.size)]; // extend if needed
			seq[y]=l.popNF(defaultSteps);
			if (loadVersion>=1.2) {
				channelNames[y]=l.popS;
			};
		};
		sPPresetMemory=[]!channels!noPre;
		seqPresetMemory=[]!channels!noPre;
		if (loadVersion>=1.3) {
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
		^l 
	}
	
	// for your own loading inst update
	iUpdateGUI{
		var val,enabled, oldRulerValue;
	
		channels.do{|y|
			
			val=sP[y][3];
			if(spModels[y][3].value!=val) {
				seqModels[y].do({|i,x| 
					enabled=(x<val).if(true,false);
					if (i.enabled!=enabled) { i.enabled_(enabled) };
				});
			};

			oldRulerValue=spModels[y][4].value;

			seqModels[y].do{|i,j| if (seq[y][j]!=i.value) {i.lazyValue_(seq[y][j],false)}};
			spModels[y].do{|i,j| if (sP[y][j]!=i.value) {i.lazyValue_(sP[y][j],false)}};
			val=sP[y][4];
			if (oldRulerValue != sP[y][4]) { this.changeSeqColours(y,val) };
			
			nameViews[y].string_(channelNames[y]);
			
		};
	
	}

	//// Networking new ////////////////////////////////////
	
	setSeq{|y,x,value,latency,send=true|
		if (seq[y][x]!=value) {
			seq[y][x]=value;
			if (send) { api.sendVP((id++"ss"++y+"_"++x).asSymbol,'netSeq',y,x,value) };
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
			if (send) { api.sendVP((id++"sS"++y).asSymbol,'netSteps',y,value) };
			{	
				seqModels[y].do({|i,x|
					enabled=(x<value).if(true,false);
					if (i.enabled!=enabled) { i.enabled_(enabled) };
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
				if (i.enabled!=enabled) { i.enabled_(enabled) };
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
		{ this.changeSeqColours(y,value) }.defer;
	}
	
	//// GUI ///////////////////////////////////////////
	
	 
	// add text to the midi controls learnt text view, from learn.
	addTextToName{|y,text|
	 	if (text.notNil) {
	 		if (channelNames[y].notEmpty) {channelNames[y]=(channelNames[y]++", ")};
	 		text=(channelNames[y]++text).nameSafe;
	 		channelNames[y]=text;
	 		api.sendOD(\netChangeName,y,text);
		 	nameViews[y].string_(text);
		}
	}
	 
	// change the text in the midi control learnt text view
	changeName{|y,text|
		//[y,text].postln;
		text=text.nameSafe;
		channelNames[y]=text;
		api.sendOD(\netChangeName,y,text);
	}
	
	// net of both both above
	netChangeName{|y,text|
		channelNames[y]=text;
		{ nameViews[y].string_(text) }.defer;
	}

	//// uGens & midi players //////////////////////////

	// this will be called by studio after booting
	*initUGens{|server| }
	
	control	{|num,  val|}      // control
	bend 	{|bend|     }		// bend
	touch	{|pressure| }		// and pressure
	
	/// midi Clock ////////////////////////////////////////
	
	//clockIn is the clock pulse, with the current song pointer in beats
	clockIn   {|beat,latency|
	
		channels.do({|y|
		
			var vel,pos,note,speed,dur;
			
			var interp, nextVel, iVel, intN;
			
			speed=sP[y][6];
			if ((beat%speed)==0) {
				pos=((beat/speed).asInt)%(sP[y][3]);
				vel=seq[y][pos];
				note=sP[y][2];
				
				interp=#[1,2,3,4,6,8,12,16][sP[y][8]];
					
				// there is a buf with this test
				// if ((instOnSolo.groupSongOnOff==1) and: {sP[y][0]==1}) {
						
				// this is a temp fix for noUsers=1
				// but breaks model symmetry when not in group listening mode
				
				
				if (((p[2].isTrue)or: {instOnSolo.isOn}) and: {sP[y][0].isTrue}) {
						
					if (sP[y][7].isTrue) {
						// control
								
						wasOn[y]=true;
						
						if (vel>=0) {
																			vel=(vel+sP[y][5]).clip(0,127);
							dur=sP[y][1];
							
							midi.control(note,vel,latency +! syncDelay,false,true);
							lastCCvalue[y]=vel;
							
							nextVel=seq[y][((beat/speed).asInt+1)%(sP[y][3])];
							if (nextVel>=0) {
								nextVel=(nextVel+sP[y][5]).clip(0,127);
								interpNo[y]=interpNo[y]+1;
								{
									intN=interpNo[y];
									(interp-1).do{|i|
										// this test stops pervious interps if they overlap
										if (intN==interpNo[y]) {
											(studio.absTime*speed*3/interp).wait;
											iVel=vel+((nextVel-vel)/interp*(i+1));
											lastCCvalue[y]=iVel;
											midi.control(note, iVel, latency +! syncDelay, 
												false, true);
										};
									};
								}.fork(SystemClock);
							};
						
						}
						
					}{
						// midi note
						if (vel>=0) {
							vel=(vel+sP[y][5]).clip(0,127);
							dur=sP[y][1];
							this.bang(note,vel,dur);
						};
						wasOn[y]=false;
					};
					
				}{
					// this test stops breaking symmetry
					if ((wasOn[y])and:{network.isHost}and:{lastCCvalue[y].notNil}) {
						{api.groupCmdOD(\stopInterpAndSet,y,lastCCvalue[y],sP[y][2]);}.defer;
					};
					wasOn[y]=false;
				};
				{posModels[y].lazyValue_(pos,false)}.defer(latency);
			};
		});
	}	
	
	// stops interpolations and send a cc value so last midi was the same on all machines
	stopInterpAndSet{|y,val,cc|
		interpNo.do{|i,j| interpNo.put(j,i+1) };
		midi.control(cc,val,studio.actualLatency +! syncDelay + 0.05,false,true);
	}
	
	// band it boy!!
	bang{|note,vel,dur|
		var noteOffNumber;
		// stop previous note
		if (bangsOn[note]==1) {
			midi.noteOff(note,vel,studio.actualLatency +! syncDelay);
		};
		// note on
		midi.noteOn(note,vel,studio.actualLatency +! syncDelay);
		bangsOn[note]=1; // on
		noteOffNumber=bangNumber[note]+1;
		bangNumber[note]=noteOffNumber;
		//note off
		SystemClock.sched(dur,{
			if (bangNumber[note]==noteOffNumber) {
				midi.noteOff(note,vel,studio.actualLatency +! syncDelay);
				bangsOn[note]=0; // off
			};
			nil
		});
	}
	
	clockPlay { }		//play and stop are called from both the internal and extrnal clock
	clockStop {
		channels.do{|y|
			{posModels[y].lazyValue_(0,false)}.defer(studio.actualLatency); // reset counter pos
		}
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
