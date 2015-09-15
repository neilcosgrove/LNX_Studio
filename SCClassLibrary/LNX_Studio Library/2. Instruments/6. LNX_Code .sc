////////////////////////////////////////////////////////////////////////////////////////////////////
// SC code instrument                                                                              .
// nb: indices is used to refer to the key by LNX_SynthDefControl:index
//
// DESIGN ISSUES:
// future issue (hard to solve) :
// seq midi control (with latency) vs notes played on internal keyboard (no latency) misses messages
// this happens with / after  evaluate, closing window and then reopen.
//
// BUGS:
// closing song leaves notes playing - mostly fixed but cuts sound short.

LNX_Code : LNX_InstrumentTemplate {
	
	classvar guiTypes, guiTypesNames;

	var <codeModel, <synthDef, <sythDefID, <errorModel;
	var <system, <user, <userList, <systemIndices; // a temp moveMVC_
	var <valid=false; // used to validate correct synthDef
	var <active=false; // is the instrument active in a co-op
	var <sequencer, <voicer;
	var <userModels, <userViews, <systemViews, <userModelsBySymbol;
	var <selectedView, <selectedIndex, <selectedBounds, <selectedType;
	var <userPresets;
	var <loadingUserViews, <loadingSystemViews;
	var <lastKeyboardNote, lastBuildString, lastClockBeat=0;
	var pollResponder;
	var <userBank, <webBrowser;

   	// anything thats needed before the models are made
	initPreModel{
		
		// user content !!!!!!!		
		userBank = LNX_SampleBank(server,apiID:((id++"_url_").asSymbol))
				.selectedAction_{|bank,val,send=true|
					// when an item is selected
					// models[108+i].doValueAction_(val,send:send);
					// find dup calls to this func below
					/*
					val.postln;
					thisProcess.dumpBackTrace;
					*/
				}
				.itemAction_{|bank,items,send=false| // i don't think send is needed here
					// when a sample is added or removed from the bank
					// this.updateSampleControlSpec(i);
					// models[100+i].doValueAction_(13,send:send);// send was true
				}
				.title_("");
		
		// the webBrowsers used to search for new sounds!
		webBrowser = LNX_WebBrowser(server,userBank);
		
	}

	*initClass {
		guiTypes=#[\MVC_MyKnob, \MVC_MyKnob3, \MVC_SmoothSlider, \MVC_Slider, \MVC_FlatSlider,
		           \MVC_OnOffView, \MVC_OnOffRoundedView, \MVC_NoteView, \MVC_PinSeqView,
		           \MVC_NumberBox, \MVC_NumberCircle];
		           
		guiTypesNames=#["Knob", "Dial", "Smooth Slider", "Slider", "Flat Slider",
					"OnOff Switch", "OnOff Rounded", "MIDI Note", "Circle Switch",
					"Number Box", "Number Circle" ];
	}

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id;
		^super.new(server,studio,instNo,bounds,open,id)
	}
	
	// an immutable list of methods available to the network
	interface{^#[\netEditString, \netSetUserModel, \netEvaluate, \netColorSystem,
	             \netBoundsUser, \netColorUser,    \netBoundsSystem, \netChangeGUIType ]}

	*studioName {^"SC Code"}
	*sortOrder{^0}
	isInstrument{^true}
	canBeSequenced{^true}
	isMixerInstrument{^true}
	hasLevelsOut{^true}

	header { 
		// define your document header details
		instrumentHeaderType="SC Code Doc";
		version="v1.5";		
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
				(\label_:"Volume" , \numberFunc_:'db', mouseDownAction_:{hack[\fadeTask].stop}),
				{|me,val,latency,send,toggle|
					this.setPVPModel(2,val,latency,send);
					this.updateSynthArg(systemIndices[\amp],val.dbamp,latency);
				}],						
			
			// 3. out channels		
			[\audioOut, midiControl, 3, "Output channels", 
				(\label_:"Out", \items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					this.setPVPModel(3,val,0,true);
					this.instOutChannel_( LNX_AudioDevices.getOutChannelIndex(p[3]));
				}],
								
			// 4.master pan
			[\pan, midiControl, 4, "Pan",
				(\label_:"Pan", \numberFunc_:\pan, \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(4,val,latency,send);
					this.updateSynthArg(systemIndices[\pan],val,latency);
				}],
			
			// 5.master duration (i don't think this is used!)
			[1,\duration,midiControl, 5, "Unused", {|me,val,latency,send,toggle|}],
			
			// 6.master pitch (transpose)
			[\pitch,midiControl, 6, "Master pitch",
				(\label_:"Pitch" , \numberFunc_:'float2Sign', \zeroValue_:0),
				{|me,val,latency,send,toggle|
					this.setPVPModel(6,val,latency,send);
					//channels.do{|i| this.updateSynthArg(\rate,i,latency)};
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
				
			// 9. sendOut
			[-1,\audioOut, midiControl, 9, "Send channel",
				(\label_:"Send", \items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					this.setPVPModel(9,val,latency,send);
					if (systemIndices[\sendOut].notNil) {
						this.updateSynthArg(systemIndices[\sendOut],
							LNX_AudioDevices.getOutChannelIndex(val),latency);
					};
				}],
			
			// 10. sendAmp
			[-inf,\db6,midiControl, 10, "Send amp", (label_:"Send"),
				{|me,val,latency,send,toggle|
					this.setPVPModel(10,val,latency,send);
					if (systemIndices[\sendAmp].notNil) {
						this.updateSynthArg(systemIndices[\sendAmp],val.dbamp,latency);
					};
				}], 
				
			// 11. in channels		
			[0,[0,LNX_AudioDevices.fxMenuList.size-1,\linear,1], midiControl, 3,"Input channels",
				(\label_:"In", \items_:LNX_AudioDevices.fxMenuList),
				{|me,val,latency,send|
					this.setPVPModel(11,val,latency,send);
					if (systemIndices[\in].notNil) {
						this.updateSynthArg(systemIndices[\in],
							LNX_AudioDevices.getFXChannelIndex(val),latency);
					};
				}],
				
			// 12. poly
			[8,[1,128,\linear,1],midiControl, 12, "Poly", (label_:"Poly"),
				{|me,val,latency,send,toggle|
					this.setPVPModel(12,val,latency,send);
					voicer.poly_(val);
					voicer.limitPoly(latency ? studio.actualLatency,0);
				}],
				
			// 13. tab
			[0,[0,3,\linear,1], {|me,val,latency,send,toggle|
				gui[\tabView].value_(val);
				p[13]=val;
			}],
				
			// 14. auto-build
			[1,\switch, {|me,val,latency,send,toggle| this.setPOD(14,val) }],
			
			// 15. inAmp
			[-inf,\db2,midiControl, 15, "In amp", (label_:"In"),
				{|me,val,latency,send,toggle|
					this.setPVPModel(15,val,latency,send);
					if (systemIndices[\inAmp].notNil) {
						this.updateSynthArg(systemIndices[\inAmp],val.dbamp,latency);
					};
				}],
				
			// 16.font size
			[9,[9,30,1,1],{|me,val,latency,send,toggle|
				this.setPVPModel(16,val,latency,send:false);
				{codeModel.themeMethods_((\font_:Font("Monaco",p[16])))}.defer;
			}],
			
			// 17. peak level
			[0.7, \unipolar,  midiControl, 17, "Peak Level",
				{|me,val,latency,send| this.setPVP(17,val,latency,send) }],
			
			// ***	
			
			// 18. Sample list or Music inst
			[0, \switch, midiControl, 18, "List or Inst",
				{|me,val,latency,send| this.setPVH(18,val,latency,send) }],
			
			// 19. Sample root
			[\MIDInote, midiControl, 19, "Root", (\label_:"Root"),
				{|me,val,latency,send| this.setPVP(19,val,latency,send)}],
			
			// 20. Sample steps per octave (spo)
			[12,[1,24,\linear,1],midiControl, 20, "Steps/Octave",
				( \label_:"Steps/Oct", \numberFunc_:'int'),
				{|me,val,latency,send,toggle| this.setPVPModel(20,val,latency,send) }],
				
			// 21. Sample transpose
			[0,[-48,48,\linear,1],midiControl, 21, "Trans",
				(\label_:"Trans",\numberFunc_:'intSign','zeroValue_':0),
				{|me,val,latency,send,toggle| this.setPVPModel(21,val,latency,send)}],
			
			// 22. Static Sample transpose
			[0,[-48,48,\linear,1],midiControl, 22, "Static",
				(\label_:"Static",\numberFunc_:'intSign','zeroValue_':0),
				{|me,val,latency,send,toggle| this.setPVPModel(22,val,latency,send)}],
				
		
		].generateAllModels;
		
		// networked pianoRoll
		sequencer=LNX_PianoRollSequencer(id++\pR)
			.action_   {|note,velocity,latency| this.seqNoteOn(note,velocity,latency) }
			.offAction_{|note,velocity,latency| this.seqNoteOff(note,velocity*127,latency) }
			.releaseAllAction_{
				voicer.releaseAllNotes(studio.actualLatency);
				{gui[\keyboardView].clear}.defer(studio.actualLatency);
			}
			.keyDownAction_{|me, char, modifiers, unicode, keycode|
				gui[\keyboardView].view.keyDownAction.value(me,char, modifiers, unicode, keycode)
			}
			.keyUpAction_{|me, char, modifiers, unicode, keycode|
				gui[\keyboardView].view.keyUpAction.value(me, char, modifiers, unicode, keycode)
			}
			.recordFocusAction_{ gui[\keyboardView].focus };
		
		// poly voicer control, for use with the LNX_PianoRollSequencer
		voicer=LNX_Voicer(server);
		
		this.initCode;

		codeModel.actions_(\enterAction, {|me|
			if (p[14]==0) {this.guiEvaluate}; // test becasue string action will auto-build
		});
		
		codeModel.actions_(\stringAction,{|me|
			this.editString(me.string);
			if (p[14]==1) {this.guiEvaluate};
		});

		errorModel = "".asModel;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,13];
		randomExclusion=[0,1,2,3,7,8,13,9,11,14,16,17];
		autoExclusion=[];
		
		// unique id for this instrument to use in SynthDef name
		sythDefID = LNX_SynthDefID.next;
		
		// the controls
		systemIndices = IdentityDictionary[]; // index of system control in synthDef args
		system        = IdentityDictionary[]; // sytem LNX_SynthDefControl's by argName as key
		user          = IdentityDictionary[]; // to do..
		userList      = IdentityDictionary[];
		userModels    = IdentityDictionary[];
		userViews     = IdentityDictionary[];
		systemViews   = IdentityDictionary[];
		userPresets   = [];
		
		pollResponder = OSCresponderNode(
			server.addr, '/tr', {arg ...msg;
				if (msg[2][2]==id) { this.postPoll( msg[2].drop(3).asString.drop(-2).drop(2) ) };
			}).add;
		
	}
	
	// peak / target volume model
	peakModel{^models[17]}
	
	// return the volume model
	volumeModel{^models[2] }
	outChModel{^models[3]}
	
	soloModel{^models[0]}
	onOffModel{^models[1]}
	
	panModel{^models[4]}
	
	sendChModel{^models[9]}
	sendAmpModel{^models[10]}
	
	// MIDI and synth control ////////////////////////////////////////////////////////////
	
	clockIn{|beat,latency|
		if (systemIndices[\clock].notNil) {
			this.updateSynthArg(systemIndices[\clock],beat,latency);
		};
		lastClockBeat=beat;
	}
	
	// midi clock in (this is at MIDIClock rate)
	clockIn3 {|beat,absTime,latency| sequencer.clockIn3(beat,absTime,latency) }
	
	bpmChange{|latency|
		if (systemIndices[\bpm].notNil) {
			this.updateSynthArg(systemIndices[\bpm],bpm,latency);
		};
	}
	
	// reset sequencers posViews
	clockStop {
		sequencer.clockStop(studio.actualLatency);
		voicer.killAllNotes(studio.actualLatency);
		{if (gui[\keyboardView].notNil) {gui[\keyboardView].clear};}.defer(studio.actualLatency);
		lastClockBeat=0;
	}
	
	// remove any clock hilites
	clockPause{
		sequencer.clockPause(studio.actualLatency);
		voicer.releaseAllNotes(studio.actualLatency);
		{if (gui[\keyboardView].notNil) {gui[\keyboardView].clear};}.defer(studio.actualLatency)
	}
	
	// called from onSolo funcs
	stopAllNotes{
		voicer.releaseAllNotes(studio.actualLatency);
		{if (gui[\keyboardView].notNil) {gui[\keyboardView].clear};}.defer(studio.actualLatency);	
	}
	
	stopDSP{
		voicer.releaseAllNotes(studio.actualLatency);
		voicer.killAllNotes(studio.actualLatency);
	}

	// noteOn (from MIDI in)
	noteOn{|note, velocity, latency|	
		// need to limit this to just midi in
		if ((note<p[7])or:{note>p[8]}) {^nil}; // drop out if out of midi range
		velocity=velocity/127; // scale to 0-1
		{
			if (gui[\keyboardView].notNil) {
				gui[\keyboardView].setColor(note,Color(1,0,0),1)
			}		
		}.defer(latency?0);
		this.startNote(note, velocity, latency);
		sequencer.noteOn(note, velocity, latency);
	}
	
	// called from sequencerFunc only
	seqNoteOn{|note, velocity, latency|
		this.startNote(note, velocity, latency);
		{
			if (gui[\keyboardView].notNil) {
				gui[\keyboardView].setColor(note,Colour(1,0.5,0),1)
			}
		}.defer(latency);	
	}
	
	// called from gui keyboard action
	keyboardNoteOn{|note, velocity, latency|
		this.startNote(note, velocity, latency);
		sequencer.noteOn  (note, velocity, latency);
	}
	
	// start a note (called by noteOn, seqNoteOn & keyboard)
	startNote{|note, velocity, latency|
		var voicerNode;
		if (valid) {  // this is test to see if synthDef is valid
			if (instOnSolo.onOff==0) {^nil}; // drop out if instrument onSolo is off
			voicerNode = voicer.noteOn(note, velocity, latency); // create a voicer node
			server.sendBundle(latency,[\s_new, synthDef.name, voicerNode.node, 0, scCodeGroupID]++
					(this.getSystemMsg(note,velocity))++(this.getUserMsg));
		};
	}
	
	// noteOff (from MIDI in)
	noteOff{|note, velocity, latency|
		if ((note<p[7])or:{note>p[8]}) {^nil}; // drop out if out of midi range
		velocity=velocity/127; // scale to 0-1 but not used
		{
			if (gui[\keyboardView].notNil) {
				gui[\keyboardView].removeColor(note)
			}
			
		}.defer(latency?0);
		this.stopNote(note, velocity, latency);
		sequencer.noteOff(note, velocity, latency);
	}
	
	// called from sequencerFunc only
	seqNoteOff{|note, velocity, latency|
		this.stopNote(note, velocity, latency);
		{
			if (gui[\keyboardView].notNil) {
				gui[\keyboardView].removeColor(note)
			}		
		}.defer(latency);
	}
	
	// called from gui keyboard action
	keyboardNoteOff{|note, velocity, latency|
		this.stopNote(note, velocity, latency);
		sequencer.noteOff(note, velocity, latency);
	}
	
	// stop note (called by noteOn, seqNoteOff & keyboard)
	stopNote{|note, velocity, latency|
		voicer.releaseNote(note,latency);
	}

	///////////////////////////////////////////////////////////////////////////////////
	
	updateDSP{|oldP,latency|
		// this is used in server reboot
		this.instOutChannel_( LNX_AudioDevices.getOutChannelIndex(p[3]), latency );
	}
	
	
	// make the list of system args for the synth
	getSystemMsg{|note,velocity|
		var list=[];
		
		
		if ((systemIndices[\bufL].notNil)or:{systemIndices[\bufR].notNil}) {
				
			var bufL, bufR, bufRate, bufAmp, bufStartFrame, bufStartPos, bufLoop, bufDur;
					
			if (models[18].value.isTrue) {
				// music inst
				# bufL, bufR, bufRate, bufAmp, bufStartFrame, bufStartPos, bufLoop, bufDur
					= userBank.getMusicInst(note,*models[19..22].collect(_.value));
			}{
				// sample list
				# bufL, bufR, bufRate, bufAmp, bufStartFrame, bufStartPos, bufLoop, bufDur
					= userBank.getSampleList(note,*models[19..22].collect(_.value));
			};
					
			if (systemIndices[\bufL].notNil) { 
				list=list++[systemIndices[\bufL],bufL];
			};
			
			if (systemIndices[\bufR].notNil) { 
				list=list++[systemIndices[\bufR],bufR];
			};

			if (systemIndices[\bufRate].notNil) { 
				list=list++[systemIndices[\bufRate],bufRate];
			};
			
			if (systemIndices[\bufAmp].notNil) { 
				list=list++[systemIndices[\bufAmp],bufAmp.dbamp];
			};
			
			if (systemIndices[\bufStartFrame].notNil) { 
				list=list++[systemIndices[\bufStartFrame],bufStartFrame];
			};
			
			if (systemIndices[\bufStartPos].notNil) { 
				list=list++[systemIndices[\bufStartPos],bufStartPos];
			};
			
			if (systemIndices[\bufLoop].notNil) { 
				list=list++[systemIndices[\bufLoop],bufLoop];
			};
									
			if (systemIndices[\bufDur].notNil) { 
				list=list++[systemIndices[\bufDur],bufDur];
			};
						
		};
		
		// out
		if (systemIndices[\out].notNil) {
			list=list++[systemIndices[\out], this.instGroupChannel];
		};
		
		// amp
		if (systemIndices[\amp].notNil) {
			list=list++[systemIndices[\amp],p[2].dbamp];
		};
		
		// velocity
		if (systemIndices[\velocity].notNil) {
			list=list++[systemIndices[\velocity],velocity];
		};
		
		// vel (alt velocity)
		if (systemIndices[\vel].notNil) {
			list=list++[systemIndices[\vel],velocity];
		};

		// midi
		if (systemIndices[\midi].notNil) {
			list=list++[systemIndices[\midi],note];
		};
		
		// freq
		if (systemIndices[\freq].notNil) {
			list=list++[systemIndices[\freq],note.midicps]; // here i can add spo
		};
		
		// pan
		if (systemIndices[\pan].notNil) {
			list=list++[systemIndices[\pan],p[4]];
		};
		
		// clock
		if (systemIndices[\clock].notNil) {
			list=list++[systemIndices[\clock],lastClockBeat];
		};

		// i_clock
		if (systemIndices[\i_clock].notNil) {
			list=list++[systemIndices[\i_clock],lastClockBeat];
		};

		// bpm
		if (systemIndices[\bpm].notNil) {
			list=list++[systemIndices[\bpm],bpm];
		};
		
		// sendOut
		if (systemIndices[\sendOut].notNil) {
			list=list++[systemIndices[\sendOut],LNX_AudioDevices.getOutChannelIndex(p[9])];
		};
		
		// sendAmp
		if (systemIndices[\sendAmp].notNil) {
			list=list++[systemIndices[\sendAmp],p[10].dbamp];
		};	
		
		// in
		if (systemIndices[\in].notNil) {
			list=list++[systemIndices[\in],LNX_AudioDevices.getFXChannelIndex(p[11])];
		};
		
		// inAmp
		if (systemIndices[\inAmp].notNil) {
			list=list++[systemIndices[\inAmp],p[15].dbamp];
		};
		
		// poll
		if (systemIndices[\poll].notNil) {
			list=list++[systemIndices[\poll],id];
		};
		
		^list;
	}
	
	// get the user arg values
	getUserMsg{
		var list=[];
		userModels.pairsDo{|indices,model,i|
			list=list++[indices,model.value];
		};		
		^list;
	}	
	
	// Presets /////////////////////////////////////////////////////////////////////////////////
	
	// get the current state as a list
	iGetPresetList{
		var userPresetList;
		this.updateUserModelsBySymbol;
		userPresetList = [userModelsBySymbol.size]++(userModelsBySymbol.collect(_.value).getPairs);
		^sequencer.iGetPresetList++userPresetList;
	}

	// add a state list to the presets
	iAddPresetList{|l|
		var noUserModels, userPrestDict;
		sequencer.iAddPresetList(l.popEND("*** END Score DOC ***").reverse);
		noUserModels=l.popI;
		
		// add user models to the user presets
		userPrestDict=IdentityDictionary[];
		noUserModels.do{
			var symbol = l.pop.asSymbol;
			var value  = l.popF;
			userPrestDict[symbol] = value;
		};
		userPresets=userPresets.add(userPrestDict);
	}
	
	// save a state list over a current preset
	iSavePresetList{|i,l|
		var noUserModels, userPrestDict;
		sequencer.iSavePresetList(i,l.popEND("*** END Score DOC ***").reverse);
		noUserModels=l.popI;
		
		// add user models to the user presets
		userPrestDict=IdentityDictionary[];
		noUserModels.do{
			var symbol = l.pop.asSymbol;
			var value  = l.popF;
			userPrestDict[symbol] = value;
		};
		userPresets[i]=userPrestDict;	
	}
	
	// for your own load preset
	iLoadPreset{|i,newP,latency|
		this.updateUserModelsBySymbol;
		sequencer.iLoadPreset(i);
		userPresets[i].pairsDo{|symbol, value|
			if (userModelsBySymbol[symbol].notNil) {
				userModelsBySymbol[symbol].lazyValueAction_(value,latency,false,false);
			};
		};
	}
	
	// for your own remove preset
	iRemovePreset{|i|
		sequencer.iRemovePreset(i);
		userPresets.removeAt(i);
	}
	
	// for your own removal of all presets
	iRemoveAllPresets{
		sequencer.iRemoveAllPresets;
		userPresets = [];
	}
	
	// random preset
	iRandomisePreset{
		userModels.do{|m,i| m.randomise };
	}	
		
	// clear the sequencer
	clearSequencer{ sequencer.clear }
		
	// disk i/o /////////////////////////////////////////////////////////////////////////////////
	
	// for saving to disk
	iGetSaveList{
		var l;
		l=[lastBuildString.size]++(lastBuildString.ascii); // the last successful build code
		l=l++[systemViews.size,userViews.size];            // the number of system & user views
		// the system gui
		systemViews.pairsDo{|indices,view|
			l=l++[indices,view.class.asSymbol];           // doesn't have model value
			l=l++(view.getSaveList);
		};
		// the users gui
		userViews.pairsDo{|indices,view|
			l=l++[indices,view.class.asSymbol,userModels[indices].value]; // has model value
			l=l++(view.getSaveList);
		};
		
		// the sequencer
		l=l++(sequencer.getSaveList);
		
		// each preset size, key & value pair
		l=l++ ( (userPresets.collect(_.size))++(userPresets.collect(_.getPairs).flat) );
		
		// the samples
		l=l++(userBank.getSaveListURL);
		
		^l	
	}
	
	// for loading from disk
	iPutLoadList{|l,noPre,loadVersion|
		var chars, asciiArray, systemListSize, userListSize, userPresetSizes, presetDict;
		
		chars=l.popI; 							// number of chars in code
		asciiArray=l.popNI(chars); 					// the code as ascii list
		codeModel.string_(asciiArray.asciiToString);	// put into the code model
		systemListSize=l.popI;						// the number of system widgets
		userListSize=l.popI;						// the number of user widgets
		
		// store the system view load lists for later use in iPostLoad
		loadingSystemViews = IdentityDictionary[];
		systemListSize.do{
			var indices = l.pop.asSymbol;
			loadingSystemViews[indices]=l.popEND("*** END MVC_View Document ***");
		};
		
		// store the user view load lists for later use in iPostLoad
		loadingUserViews = IdentityDictionary[];
		userListSize.do{
			var indices = l.popI;
			loadingUserViews[indices]=l.popEND("*** END MVC_View Document ***");
		};
		
		// the sequencer
		sequencer.putLoadList( l.popEND("*** END PRSeq DOC ***") );
		
		// and user item presets
		userPresets={ IdentityDictionary[] } ! noPre;
		
		if (loadVersion>=1.4) {
			userPresetSizes = l.popNI(noPre); // pop the sizes
			userPresetSizes.do{|size,i|
				size.do{
					var symbol = l.pop.asSymbol; // get the key value pair
					var value  = l.popF;
					userPresets[i][symbol] = value; // and put in user preset dict
				};
			};
		};
		
		// the samples
		if (loadVersion>=1.5) {
			userBank.putLoadListURL( l.popEND("*** END URL Bank Doc ***") );
		};
		
	}
	
	// anything that needs doing after a load
	iPostLoad{
		
		
		
		this.guiEvaluate(false); // evaluate code 1st
		
		
		{
		
		userBank.adjustViews; // wierd alignment bug in sampleBank fixed by this
		
		if (userBank.size>0) { userBank.allInterfacesSelect(0) };
		
		
		// put in system views
		loadingSystemViews.pairsDo{|indices,list|
			var class=list[0];	
			var loadList = list.drop(1);
			var oldView, newView;
		
			oldView = systemViews[indices];	// the old view
			
			// make the new view, with the old model and actions
			newView = class.asSymbol.asClass.new(false,oldView.model,gui[\userGUIScrollView], 
												  Rect(),gui[\userGUI])
					.boundsAction_(oldView.boundsAction)
					.mouseDownAction_(oldView.mouseDownAction)
					.colorAction_(oldView.colorAction)
					.putLoadList(loadList); // put in the load data
			
			newView.create;                  // now make it
			systemViews[indices]=newView;    // store it
			oldView.remove.removeModel.free; // and get rid of the old one
			
		};
		
		systemViews[\sendAmp].visible_(system[\sendAmp].notNil);
		systemViews[\inAmp].visible_(system[\inAmp].notNil);
		
		// put in user views	
		loadingUserViews.pairsDo{|indices,list|
			var class=list[0];	
			var modelValue = list[1].asFloat;
			var loadList = list.drop(2);
			var oldView, newView;
			
			// make the new view, with the old model and actions
			oldView = userViews[indices];
			newView = class.asSymbol.asClass.new(false,oldView.model,gui[\userGUIScrollView], 
												  Rect(),gui[\userGUI])
					.boundsAction_(oldView.boundsAction)
					.mouseDownAction_(oldView.mouseDownAction)
					.colorAction_(oldView.colorAction)
					.putLoadList(loadList); // put in the load data

			newView.create;                   // now make it
			userViews[indices]=newView;       // store it
			newView.valueAction_(modelValue,nil,false,false); // update the value
			oldView.remove.removeModel.free;  // and get rid of the old one
		};
		
		loadingSystemViews.clear;
		loadingUserViews.clear;
		
		codeModel.themeMethods_((\font_:Font("Monaco",p[16])));
		
		if (lastTemplateLoadVersion<1.2) {
			this.setAsPeakLevel; // part of myHack
		};
		
		}.defer(0.05); // this defer looks annoying, need to rethink, maybe .newFrom(list)
		
	}
	
	iFreeAutomation{ userModels.do(_.freeAutomation) }
	
	// free this
	iFree{		
		userModels.do(_.free);
		userBank.free;
		pollResponder.remove;
		sequencer.free;
		systemViews.do(_.free);
		codeModel = synthDef = sythDefID = system = user = userList =
		systemIndices = sequencer = userModels = pollResponder = nil;
		{LNX_SampleBank.emptyTrash}.defer(1); // empty trash 1 seconds later
	}
	
	// networking /////////////////////////////////////////////////////////////////////
	
	// ** Code ** //
	
	// edit code (max size 100k chars)
	editString{|string|
		// TO DO: when over 1 msg, do a lazy update over network
		if (string.size<1000000) {
			api.sendClumpedList(\netEditString,string.ascii,false);
		}
	}
	
	// net of editString
	netEditString{|asciiArray| codeModel.string_(asciiArray.asciiToString) }
	
	// called from the models to update synth arguments
	updateSynthArg{|synthArg,val,latency|
		voicer.allNodes.do{|voicerNode|
			// i have decided to go with studioLatency to stop wrong values on userModel changes
			server.sendBundle(studio.actualLatency,[\n_set,voicerNode.node,synthArg,val]);
		};
	}
	
	// ** user ** //
	
	// called from user models to set values
	setUserModel{|index,value,latency,send=true|
		if (send) { api.sendVP(id+"_vpm_"++index,'netSetUserModel',index,value); };
	}
	
	// net reciever of above
	netSetUserModel{|index,value|  userModels[index].lazyValueAction_(value,nil,false,false) }
	
	// net update gui bounds
	netBoundsUser{|indices,l,t,w,h|
		{
			if (userViews[indices.asInt].notNil) {
				userViews[indices.asInt].bounds_(Rect(l,t,w,h))
			};
		}.defer;
	}
	
	// set color for user widget
	netColorUser{|indices,key,r,g,b,a|
		{
			if (userViews[indices.asInt].notNil) {
				userViews[indices.asInt].color_(key.asSymbol,Color(r,g,b,a))
			};
		}.defer;		
	}
	
	// ** system ** //

	// net update system widget bounds
	netBoundsSystem{|indices,l,t,w,h|
		{systemViews[indices.asSymbol].bounds_(Rect(l,t,w,h)) }.defer;
	}

	// set color of system widget
	netColorSystem{|indices,key,r,g,b,a|
		{
			if (systemViews[indices.asSymbol].notNil) {
				systemViews[indices.asSymbol].color_(key.asSymbol,Color(r,g,b,a))
			};
		}.defer;		
	}
	
	
	// code //////////////////////////////////////////////////////////////////////
	
	iPostNew{
		this.guiEvaluate(false);
		{
			sequencer.resizeToWindow;
			gui[\userGUIScrollView].refresh; // fixes a small bug in this scrollView when opened
			
		}.defer(0.02);
			// window created after 0.01 secs (see deferOpenWindow)
	}
	
	// we can divide the next two methods up better, but wait for TO DO list
	
	// gui call
	guiEvaluate{|send=true|
		var def, string, pass, warnFunc;
		
		string = codeModel.string;
		pass = string.interpretSafe;
		
		if (pass==true) {
			this.clearError;
			
			warnFunc = {|error|
				this.warnFromError(error);
				LNX_Error.remove(warnFunc);
			};
			
			LNX_Error.reportTo_(warnFunc);
			def = string.interpret;
			LNX_Error.remove(warnFunc);
			
			if (def.isKindOf(SynthDef)) {
				def.name_(sythDefID.asString+(def.name));	
				if (def.metadata.isNil) { def.metadata=() };
				if (def.metadata[\specs].isNil) { def.metadata[\specs]=() };
				//"SynthDef : ".post;
				//def.name.postln;	
				this.dissectSynthDef(def);
				if (send) { api.sendOD(\netEvaluate) };
			}{
				this.warnNotSynthDef(def);
			}
		}{
			this.warnKeyword(pass);
		}
	}
	
	// net evaluate
	netEvaluate{
		var def, string, pass, warnFunc;
		
		string = codeModel.string;
		pass = string.interpretSafe;
		
		if (pass==true) {
			this.clearError;
			
			warnFunc = {|error|
				this.warnFromError(error);
				LNX_Error.remove(warnFunc);
			};
			
			LNX_Error.reportTo_(warnFunc);
			def = string.interpret;
			LNX_Error.remove(warnFunc);
			
			if (def.isKindOf(SynthDef)) {
				def.name_(sythDefID.asString+(def.name));
				if (def.metadata.isNil) { def.metadata=() };
				if (def.metadata[\specs].isNil) { def.metadata[\specs]=() };
				//"SynthDef : ".post;
				//def.name.postln;	
				this.dissectSynthDef(def);
			}{
				this.warnNotSynthDef(def);
			}
		}{		
			this.warnKeyword(pass);
		}
	}
	
	// after a restart
	startDSP{
		if (synthDef.notNil) { synthDef.send(server) };
	}
	
	// store, set and send def
	setNewDef{|def|
		synthDef = def;
		synthDef.send(server);
	}
	
	// dissect the synth def into appropriate parts
	dissectSynthDef{|synthDef|
		var controlNames, metaData, names, indices, defaultValues, types;
		var missingArgs, failed=false;
		
		valid = true;
		
		systemIndices = IdentityDictionary[];
		system        = IdentityDictionary[];
		user          = IdentityDictionary[];
		userList      = IdentityDictionary[];
		
		controlNames  = synthDef.allControlNames;
		names         = controlNames.collect(_.name);
		indices       = controlNames.collect(_.index);
		defaultValues = controlNames.collect(_.defaultValue);
		metaData      = synthDef.metadata[\specs];
		
		// test if it can be released
		if (synthDef.canReleaseSynth.not) {
			this.warnNoRelease;
			failed=true;
		};
		
		// test for compulsory arguments before proceeding
		missingArgs=[];
		#[\out, \amp, \gate, \pan].do{|argName|
			if (names.includes(argName).not) {
				missingArgs=missingArgs.add(argName);
			}
		};
		if (missingArgs.notEmpty) {
			this.warnMissingArgs(missingArgs);
			failed=true;
		};
		
		if (failed) {^nil}; // drop out if failed
		
		this.successError; // pass was a success
		
		lastBuildString = codeModel.string.copy; // it didn't fail so this will be last successful
		
		// work out which type the control is. either system, user or userList
		types = names.collect{|name,i|
			if ((#[\out, \amp, \velocity, \vel, \freq, \midi, \gate, \pan, \bpm, \clock, \i_clock,
				   \sendOut, \sendAmp, \in, \inAmp, \poll, 
				   
				   \bufL, \bufR, \bufRate, \bufAmp, \bufStartFrame,
				   \bufStartPos, \bufLoop, \bufDur
   
				].includes(name)
				and:{defaultValues[i].isNumber})) {
					// the first item with this name is a system control
					if (systemIndices[name].isNil) {
						systemIndices[name]=indices[i];
						\system
					}{
						// and the 2nd will be user. This can only be from a NamedControl
						\user	
					}
			}{
				// is it a single arg or a list
				if (defaultValues[i].isNumber) { \user }{ \userList }
			}	
		};
		
		// make the LNX_SynthDefControl
		controlNames.collect{|control,i|
			var spec;
			switch (types[i],
				\system, {
					// uses name as key as only one instance
					
					// no spec as already defined in models or doesn't have one
					system[names[i]] = control.asSynthDefControl(types[i])
					
				}, \user, {
					// uses index as key
					
					// 1st use metaData to make a spec
					if (metaData[names[i]].notNil) {
						spec = metaData[names[i]].asSpec.copy; // must use copy
						if (spec.isNil) { this.warnSpec( metaData[names[i]]) }; // test it
					};
					
					// if still nil make one from the default
					if (spec.isNil) { 
						if (defaultValues[i]==0) {
							spec = \unipolar.asSpec.copy; // must use copy
						}{
							spec = [0,defaultValues[i]*2,\linear,0,defaultValues[i]].asSpec;
						};
						// TO DO: or work out possible spec from part of argName
						// could also use as part of metaData name
					};	
										
					// constrain it
					spec.default_(spec.constrain(defaultValues[i]));
					
					// make a SynthDefControl
 					user[indices[i]] = control.asSynthDefControl(types[i],spec);
					
				}, \userList,{
					// uses index as key
					// will prob do the same as \user
					userList[indices[i]] =
						control.asSynthDefControl(types[i],metaData[names[i]].asSpec)
				}
			);			
		};
		
		// buildModels could fail so do 1st before setNewDef
		this.buildModels(synthDef);    // join system to models
		this.setNewDef(synthDef);      // store and send
				
	}
	
	// build the models and gui views
	// METHOD will do 1 of 3 things
	//	1. delete removed controls
	//	2. keep old controls but update them (adapting).
	//	3. add any new controls
	// var user is SDControl only but will be the new order. we need to adapt old
	
	buildModels{|synthDef|
		var oldModels, adaptModels, newControls, strategy;
		var oldModelsOrdered, newControlsOrdered;
		var oldViews, newToOld, viewIndex=0, viewRect;

		oldModels   = userModels.copy;      // copy oldModels 
		adaptModels = IdentityDictionary[]; // what need to be adapted
		oldViews    = userViews.copy;       // for views
		newToOld    = IdentityDictionary[];    // used to go from new to old indices in adapt
		oldModelsOrdered = oldModels.ordered; //make an old list of associations + sort by indices
		newControlsOrdered = user.ordered;   // make an new list of associations + sort by indices
		
		// make a list of what to do with each item in user: DELETE, ADAPT(location), NEW
		strategy = IdentityDictionary[];
		user.keysDo{|indices| strategy[indices] = \new };
		
		// ** SCAN and find adapts with same name, may have moved.
		// by trying to match them in indices order, by name
		newControlsOrdered.do{|association|
			var newIndices=association.key;
			var newControl=association.value;
			var oldIndices, oldModel, oldControl;
			var i=0;
			
			while ({i<(oldModelsOrdered.size)}, {
				
				if (oldModelsOrdered[i].notNil) { // once used nil checking will be needed here
				
					oldIndices = oldModelsOrdered[i].key;
					oldModel   = oldModelsOrdered[i].value;
					oldControl = oldModel.synthDefControl;
					
					if (oldControl.name==newControl.name) {
						
						// do matching here !! (to check)
						strategy[newIndices]    = \adapt;
						adaptModels[newIndices] = oldModel;
						oldModels[oldIndices]   = nil;
						oldModelsOrdered[i]=nil; // remove from selection
						
						newToOld[newIndices] = oldIndices; //for view but maybe i could use this
						
						i=oldModelsOrdered.size; // and stop
					};
				};	
				i=i+1; // iterate
			});
		};
		
		// make a list of new controls
		newControls = user.select{|c,indices| strategy[indices]==\new };
		
		// wipe the dictionaries. this way does not make new objects.
		userModels.clear;
		userViews.clear;
		
		// now we can DELETE oldModels, UPDATE adaptModels and ADD newControls

		// so everything left in old can be deleted
		oldModels.pairsDo{|indices,model| model.removeMIDIControl(false).free };
		
		// update the auto gui
		if (oldModels.notEmpty) { MVC_Automation.refreshGUI };
		
		// update all ADAPTS
		adaptModels.pairsDo{|indices,model|
			var specSymbol, zeroValueSpec;
			var newControl = user[indices];
			
			// this will also update the index for the action function
			model.synthDefControl_(newControl)          // store the control in the model
				.changeControlID(indices.neg-1000000)  // offset the midi controls...
				.controlSpec_(newControl.controlSpec); // update the spec
			
			// update gui number func & zero value
			specSymbol = synthDef.metadata[\specs][newControl.name];
			
			if (specSymbol.isKindOf(Symbol)) {
				// add a number func based on spec name
				if (MVC_NumberFunc.funcs.includesKey(specSymbol)) {
					model.themeMethods_( (numberFunc_: specSymbol) );
				}{
					model.themeMethods_( (numberFunc_:\float2) ); // default
				};
				
				// and add zero Value, use the default of metaData:\symbol.asSpec if possible
				zeroValueSpec = specSymbol.asSpec;
				if (zeroValueSpec.notNil) {
					model.themeMethods_( (zeroValue_:(zeroValueSpec.default)) );
				}{
					model.themeMethods_( (zeroValue_:(newControl.controlSpec.default)) );
				};
			
			}{
				model.themeMethods_( (numberFunc_:\float2) ); // default
			};
			
			userModels[indices] = model; // store it
			userViews[indices]  = oldViews[newToOld[indices]]; // and the view
			
		};

		// now put the correct control ID (this avoids mixing up controls)
		adaptModels.pairsDo{|indices|
			userModels[indices].changeControlID(indices.neg); // now put correct control id
		};

		// and create all NEW models from the controls (sorted into associations)
		newControls.ordered.do{|association|
			var indices=association.key;
			var sdControl=association.value;
			var model, specSymbol, zeroValueSpec, name;
			
			// ** MAKE A MODEL from the spec
			model = sdControl.controlSpec.asModel; // we make the model here :)
			model.synthDefControl_(sdControl); // store the control in the model
			userModels[indices] = model; // store it

			// ** ADD AN ACTION 
			model.action_{|me,val,latency,send,toggle|
				var synthDefControl = me.synthDefControl;
				// no point setting a scalar
				if (synthDefControl.rate!=\scalar) {
									
					voicer.allNodes.do{|voicerNode|
						
						server.sendBundle(latency,
							[\n_set,voicerNode.node,synthDefControl.index,val]);
					};
					
					
				};
				this.setUserModel(synthDefControl.index,val,latency,send,toggle);
			};
			
			// ** NAME IT
			name = sdControl.name.asString[..16]; // make a suitable name
			model.themeMethods_( (label_:name) ); // do the label
			
			// ** CONNECT A MIDI CONTROL
			model.controlID_(sdControl.index.neg, midiControl, name); // add midiControl(use .neg)
			
			// ** GIVE IT A NUMBER FUNC
			specSymbol = synthDef.metadata[\specs][sdControl.name];
			if (specSymbol.isKindOf(Symbol)) {
				// add a number func based on spec name
				if (MVC_NumberFunc.funcs.includesKey(specSymbol)) {
					model.themeMethods_( (numberFunc_: specSymbol) );
				}{
					model.themeMethods_( (numberFunc_:\float2) ); // default;
				};
				
				// and add zero Value, use the default of metaData:\symbol.asSpec if possible
				zeroValueSpec = specSymbol.asSpec;
				if (zeroValueSpec.notNil) {
					model.themeMethods_( (zeroValue_:(zeroValueSpec.default)) );
				}{
					model.themeMethods_( (zeroValue_:(sdControl.controlSpec.default)) );
				};
			}{
				model.themeMethods_( (numberFunc_:\float2) ); // default;
			};
			
			// ** MAKE THE GUI

			// find a free rect
			while ( {
				viewRect=Rect(40 + ((viewIndex%11)*50), 40+((viewIndex/11).asInt*30), 50, 50);
				gui[\userGUIScrollView].intersects(viewRect.insetBy(-5,-5));
			},{ viewIndex = viewIndex + 1 });

			// and make the gui
			userViews[indices] = MVC_MyKnob(userModels[indices],gui[\userGUIScrollView],
				viewRect,gui[\userGUI])
					.boundsAction_{|view,rect|
						api.sendVP("nBU"++view.model.synthDefControl.index,\netBoundsUser,
							view.model.synthDefControl.index, *rect.storeArgs);
						selectedBounds=rect;
					}
					.mouseDownAction_{|view|
						this.selectGUI(view,userViews,view.model.synthDefControl.index,\user);
					}.colorAction_{|view,key,color|
						api.sendVP("nCU"++view.model.synthDefControl.index,\netColorUser,
							view.model.synthDefControl.index,key, *color.storeArgs);
					};
			
		};
		
		// again with the defering, i really need to tidy this up
		{
			// make system controls visible (why do i need to defer this when initalising
			systemViews[\sendAmp].visible_(system[\sendAmp].notNil);
			gui[\sendOut].visible_(system[\sendOut].notNil);
			
			gui[\in].visible_(system[\in].notNil);
			systemViews[\inAmp].visible_(system[\inAmp].notNil);
		}.defer(0.075);  
		
		// for easy preset access
		this.updateUserModelsBySymbol;
		
		// remove unused symbols from user presets
		userPresets.do{|userPrestDict|
			userPrestDict.pairsDo{|symbol,value|
				if (userModelsBySymbol.keys.includes(symbol).not) {
					userPrestDict[symbol]=nil;	
				};
			};
		};
		
		// update the gui view to remove any residual drawing
		gui[\userGUIScrollView].refresh;
		
		if (gui[\colorPicker].notNil) {gui[\colorPicker].object_(nil)};
		
	}
	
	// for easy preset access
	updateUserModelsBySymbol{
		userModelsBySymbol=IdentityDictionary[];
		userModels.do{|model|
			userModelsBySymbol[model.synthDefControl.name] = model;
		};
	}
	
	// document below, who is calling this? !!
	
	// update the gui type menu
	selectGUI{|view,collection,index,collectionType|
		if (gui[\colorPicker].notNil) {
			selectedView=view;
			selectedType=collectionType;
			selectedIndex=index;
			selectedBounds=view.bounds;
			gui[\colorPicker].object_(view,view.label);
			gui[\guiType].value_( guiTypes.indexOf(view.class.asSymbol))
		}
	}
	
	// change the type of the gui widget (via host 1st)
	changeGUIType{|i| api.groupCmdOD(\netChangeGUIType,i,selectedIndex,selectedType, api.uid,
		*selectedBounds.asArray) }
	
	// change the type of gui, called from menu in this (but ColorPicker window)
	netChangeGUIType{|i,netSelectedIndex,netSelectedType,uid,l,t,w,h|
		
		var newView, oldView, selectedCollection, selectedBounds = Rect(l,t,w,h);
		
		if (netSelectedType==\system) {
			netSelectedIndex = netSelectedIndex.asSymbol;
			selectedCollection = systemViews;
			oldView = systemViews[netSelectedIndex];
		};
		
		if (netSelectedType==\user) {
			netSelectedIndex = netSelectedIndex.asInt;
			selectedCollection = userViews;
			oldView = userViews[netSelectedIndex]
		};

		// make the new view
		newView = guiTypes[i].asClass.new(false,oldView.model,gui[\userGUIScrollView], 
											  selectedBounds,gui[\userGUI])
				.colors_(oldView.colors)
				.boundsAction_(oldView.boundsAction)
				.mouseDownAction_(oldView.mouseDownAction)
				.colorAction_(oldView.colorAction);
		
		newView.create;
		oldView.remove.removeModel.free;
		selectedCollection[netSelectedIndex] = newView;
		
		// update
		gui[\userGUIScrollView].refresh;
			
		// this should just be for user not group
		if (
			(((api.isConnected) and: {uid.asSymbol == api.uid}) // its me
			or: // or i have selected too
			{ (netSelectedType==selectedType)and:{selectedIndex==netSelectedIndex}  })) 
		 {
			
			if (gui[\colorPicker].notNil) {
				selectedView = newView;
				gui[\colorPicker].object_(newView,newView.label);
				gui[\guiType].value_( guiTypes.indexOf(newView.class.asSymbol));
			};
		 }
				
	}

} // end ////////////////////////////////////


