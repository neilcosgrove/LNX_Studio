////////////////////////////////////////////////////////////////////////////////////////////////////
// SC code instrument                                                                           .
// nb: indices is used to refer to the key by LNX_SynthDefControl:index
//
// DESIGN ISSUES:
// future issue (hard to solve) :
// seq midi control (with latency) vs notes played on internal keyboard (no latency) misses 
// messages
// this happens with / after  evaluate, closing window and then reopen.
//
// BUGS:
// strange error when sequencing i_room in GVerb and then editing the codeModel.
// 	the synth stops replacing.

LNX_CodeFX : LNX_InstrumentTemplate {
	
	classvar guiTypes, guiTypesNames;

	var <codeModel, <synthDef, <sythDefID, <errorModel;
	var <system, <user, <userList, <systemIndices; // a temp moveMVC_
	var <valid=false; // used to validate correct synthDef
	var <active=false; // is the instrument active in a co-op
	var <userModels, <userViews, <systemViews, <userModelsBySymbol;
	var <selectedView, <selectedIndex, <selectedBounds, <selectedType;
	var <userPresets;
	var <loadingUserViews, <loadingSystemViews;
	var lastBuildString, lastClockBeat=0;
	var pollResponder;

	*initClass {
		guiTypes=#[\MVC_MyKnob, \MVC_MyKnob3, \MVC_SmoothSlider, \MVC_Slider, \MVC_FlatSlider,
		           \MVC_OnOffView, \MVC_OnOffRoundedView, \MVC_NoteView, \MVC_PinSeqView,
		           \MVC_NumberBox, \MVC_NumberCircle];
		           
		guiTypesNames=#["Knob", "Dial", "Smooth Slider", "Slider", "Flat Slider",
					"OnOff Switch", "OnOff Rounded", "MIDI Note", "Circle Switch",
					"Number Box", "Number Circle" ];
	}

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}
	
	// an immutable list of methods available to the network
	interface{^#[\netEditString, \netSetUserModel, \netEvaluate, \netColorSystem,
	             \netBoundsUser, \netColorUser,    \netBoundsSystem,
	             \netChangeGUIType, \netBounds ]}

	*studioName {^"SC Code FX"}
	*sortOrder{^0.5}
	isFX{^true}
	isInstrument{^false}
	canTurnOnOff{^false}
	
	header { 
		// define your document header details
		instrumentHeaderType="SC CodeFX Doc";
		version="v1.4";		
	}

	inModel{^models[15]}
	inChModel{^models[11]}
	outModel{^models[2]}
	outChModel{^models[3]}

	// the models
	initModel {

		#models,defaults=[
			// 0.solo
			[0],
			
			// 1.onOff
			[1, \switch, midiControl, 1, "On", (permanentStrings_:["I","I"]),
				{|me,val,latency,send|
					if (systemIndices[\on].notNil) {
						this.updateSynthArg(systemIndices[\on],val,latency);
					};
				}],
				
					
			// 2.master amp
			[\db6,midiControl, 2, "Out",
				(\label_:"Out" , \numberFunc_:'db'),
				{|me,val,latency,send,toggle|
					this.setPVPModel(2,val,latency,send);
					this.updateSynthArg(systemIndices[\amp],val.dbamp,latency);
				}],						
			
			// 3. out channels		
			[\audioOut, midiControl, 3, "Output channels", 
				(\label_:"Out", \items_:LNX_AudioDevices.outputAndFXMenuList),
				{|me,val,latency,send|
					this.setPVPModel(3,val,latency,send);
					this.updateSynthArg(systemIndices[\out],
						LNX_AudioDevices.getOutChannelIndex(val),latency);
				}],
								
			// 4.master pan
			[0],
			
			// 5.master duration (and not used!)
			[1,\duration,midiControl, 5, "Master duration",
				(\label_:"Duration" , \numberFunc_:'float2'),
				{|me,val,latency,send,toggle| this.setP(5,val,latency,send)}],
			
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
			[-2,\audioOut, midiControl, 9, "Send channel",
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
				}],
				
			// 13. tab
			[0,[0,1,\linear,1], {|me,val,latency,send,toggle|
				gui[\tabView].value_(val);
				p[13]=val;
			}],
				
			// 14. auto-build
			[1,\switch, {|me,val,latency,send,toggle| this.setPOD(14,val) }],
			
			// 15. inAmp
			[0,\db6,midiControl, 15, "In amp", (label_:"In"),
				{|me,val,latency,send,toggle|
					this.setPVPModel(15,val,latency,send);
					if (systemIndices[\inAmp].notNil) {
						this.updateSynthArg(systemIndices[\inAmp],val.dbamp,latency);
					};
				}],
				
			// 16. width
			[thisWidth,{|me,val,latency,send,toggle|
				this.setPVPModel(16,val,latency,send);
				window.setInnerExtentSuppressResizeAction(p[16],p[17]);
				if (window.isClosed) {
					gui[\scrollView].bounds_(Rect(11,11,p[16]-22,p[17]-23));
					gui[\userGUIScrollView].bounds_(Rect(14,50,p[16]-28,p[17]-90));

				};
			}],
			
			// 17. height
			[thisHeight, {|me,val,latency,send,toggle|
				this.setPVPModel(17,val,latency,send);
				window.setInnerExtentSuppressResizeAction(p[16],p[17]);
				if (window.isClosed) {
					gui[\scrollView].bounds_(Rect(11,11,p[16]-22,p[17]-23));
					gui[\userGUIScrollView].bounds_(Rect(14,50,p[16]-28,p[17]-90));
					presetView.bounds_(Rect(3,p[17]-46,155-10,18));
					gui[\on].bounds_(Rect(151,p[17]-47,22,19));
					gui[\midi].bounds_(Rect(177,p[17]-47,37,19));
					gui[\edit].bounds_(Rect(218,p[17]-46,20,18));
				};
			}],
				
			// 18.font size
			[9,[9,30,1,1],{|me,val,latency,send,toggle|
				this.setPVPModel(18,val,latency,send:false);
				{codeModel.themeMethods_((\font_:Font("Monaco",p[18])))}.defer;
			}]
		
		].generateAllModels;
		
		
		this.initCode;

		codeModel.actions_(\enterAction, {|me|
			if (p[14]==0) {
				if (this.guiEvaluate) { this.startDSP };
			}; // test becasue string action will auto-build
		});
		
		codeModel.actions_(\stringAction,{|me|
			this.editString(me.string);
			if (p[14]==1) {
				if (this.guiEvaluate) { this.startDSP };
			};
		});

		errorModel = "".asModel;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1,13,16,17];
		randomExclusion=[0,1,2,3,7,8,13,16,17,9,11,10,15,14,18];
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
				if (msg[2][2]==id) 
					{ this.postPoll( msg[2].drop(3).asString.drop(-2).drop(2) ) };
			}).add;
		
	}
	
	// return the volume model
	volumeModel{^models[2] }
	
	// MIDI and synth control ////////////////////////////////////////////////////////////
	
	clockIn{|beat,latency|
		if (systemIndices[\clock].notNil) {
			this.updateSynthArg(systemIndices[\clock],beat,latency);
		};
		lastClockBeat=beat;
	}
		
	bpmChange{|latency|
		if (systemIndices[\bpm].notNil) {
			this.updateSynthArg(systemIndices[\bpm],bpm,latency);
		};
	}

	///////////////////////////////////////////////////////////////////////////////////
	
	// make the list of system args for the synth
	getSystemMsg{
		var list=[];
		
		// on 
		if (systemIndices[\on].notNil) {
			list=list++[systemIndices[\on],p[1]];
		};		
		// out
		if (systemIndices[\out].notNil) {
			list=list++[systemIndices[\out],LNX_AudioDevices.getOutChannelIndex(p[3])];
		};
		
		// amp
		if (systemIndices[\amp].notNil) {
			list=list++[systemIndices[\amp],p[2].dbamp];
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
		this.updateUserModelsBySymbol;
		^ ([userModelsBySymbol.size]++(userModelsBySymbol.collect(_.value).getPairs));
	}

	// add a state list to the presets
	iAddPresetList{|l|
		var noUserModels, userPrestDict;
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
		userPresets[i].pairsDo{|symbol, value|
			if (userModelsBySymbol[symbol].notNil) {
				userModelsBySymbol[symbol].lazyValueAction_(value,latency,false,false);
			};
		};
	}
	
	// for your own remove preset
	iRemovePreset{|i|
		userPresets.removeAt(i);
	}
	
	// for your own removal of all presets
	iRemoveAllPresets{
		userPresets = [];
	}
	
	// random preset
	iRandomisePreset{
		userModels.do{|m,i| m.randomise };
	}	
		
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
			
		// each preset size, key & value pair
		l=l++ ( (userPresets.collect(_.size))++(userPresets.collect(_.getPairs).flat) );
		
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
	
		// and user item presets
		userPresets={ IdentityDictionary[] } ! noPre;
		
		if (loadVersion>=1.4) {
			userPresetSizes = l.popNI(noPre);         // pop the sizes
			userPresetSizes.do{|size,i|
				size.do{
					var symbol = l.pop.asSymbol;    // get the key value pair
					var value  = l.popF;
					userPresets[i][symbol] = value; // and put in user preset dict
				};
			};
		};
		
		// evaluate code
		this.guiEvaluate(false); 
		
		// put in system views
		loadingSystemViews.pairsDo{|indices,list|
			var class=list[0].asSymbol.asClass;	
			var loadList = list.drop(1);
			var oldView, newView;
		
			oldView = systemViews[indices];	// the old view
			
			// make the new view, with the old model and actions
			newView = class.new(false,oldView.model,gui[\userGUIScrollView], 
												  Rect(),gui[\userGUI])
					.boundsAction_(oldView.boundsAction)
					.mouseDownAction_(oldView.mouseDownAction)
					.colorAction_(oldView.colorAction)
					.putLoadList(loadList);                      // put in the load data
			if (gui[\userGUIScrollView].isOpen) {newView.create}; // now make it
			systemViews[indices]=newView;                         // store it
			oldView.remove.removeModel.free;                      // and get rid of the old one
			
		};
		
		systemViews[\sendAmp].visible_(system[\sendAmp].notNil);
		
		// put in user views	
		loadingUserViews.pairsDo{|indices,list|
			var class=list[0].asSymbol.asClass;
			var modelValue = list[1].asFloat;
			var loadList = list.drop(2);
			var oldView, newView;
			
			// make the new view, with the old model and actions
			oldView = userViews[indices];
			newView = class.new(false,oldView.model,gui[\userGUIScrollView], 
												  Rect(),gui[\userGUI])
					.boundsAction_(oldView.boundsAction)
					.mouseDownAction_(oldView.mouseDownAction)
					.colorAction_(oldView.colorAction)
					.putLoadList(loadList);                        // put in the load data
			if ((class==MVC_OnOffView) || (class==MVC_OnOffRoundedView)) {
				newView.showNumberBox_(false);	
			};
			if (gui[\userGUIScrollView].isOpen) { newView.create }; // now make it
			userViews[indices]=newView;                             // store it
			newView.valueAction_(modelValue,nil,false,false);       // update the value
			oldView.remove.removeModel.free;                        // and get rid of the old one
		};
		
		loadingSystemViews.clear;
		loadingUserViews.clear;
		
		codeModel.themeMethods_((\font_:Font("Monaco",p[18])));
		
		this.setAsPeakLevel; // part of myHack
		
	}
	
	iFreeAutomation{ userModels.do(_.freeAutomation) }
	
	// free this
	iFree{
		//{
		userModels.do(_.free);
		pollResponder.remove;
		systemViews.do(_.free);
		codeModel = synthDef = sythDefID = system = user = userList =
		systemIndices = userModels = pollResponder = nil;
		//}.defer(1);
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
		if (node.notNil) {
			server.sendBundle(latency,["/n_set",node,synthArg,val]);
		};
	}
	
	// ** user ** //
	
	// called from user models to set values
	setUserModel{|index,value,latency,send=true|
		if (send) { api.sendVP(id+"_vpm_"++index,'netSetUserModel',index,value) };
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

	noPutList{
		this.guiEvaluate(false);
		// fixes a small bug in this scrollView when opened
		// window created after 0.01 secs (see deferOpenWindow)
		{ gui[\userGUIScrollView].refresh }.defer(0.02);
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
				pass = this.dissectSynthDef(def);
				if (send) { api.sendOD(\netEvaluate) };
			}{
				this.warnNotSynthDef(def);
				pass = false;
			}
		}{
			this.warnKeyword(pass);
		};
		
		^pass; // return pass flag to know if a new SynthDef neeeds to be sent to the server
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
				if (this.dissectSynthDef(def)) { this.startDSP };
			}{
				this.warnNotSynthDef(def);
			}
		}{		
			this.warnKeyword(pass);
		}
	}
	
	// start with an empty synthDef
	*initUGens{|server| } 
	
	// add if already made
	initUGens{|server| }
	
/*
0 add the new node to the the head of the group specified by the add target ID.
1 add the new node to the the tail of the group specified by the add target ID.
2 add the new node just before the node specified by the add target ID.
3 add the new node just after the node specified by the add target ID.
4 the new node replaces the node specified by the add target ID. The target node is freed.
*/
	
	// send synthDef, create new, replace old, correct args
	startDSP{
		var previousNode = node;  // keep old node
		node = server.nextNodeID; // and get a new one
		
		// send def and then create a Synth object. Message stylie
		if (previousNode.isNil) {
			// a new one
			synthDef.send(server, ([\s_new, synthDef.name, node, 1,  fxGroup.nodeID]++
											(this.getSystemMsg) ++ (this.getUserMsg))
			);	
		}{
			// or replace the old one
			synthDef.send(server, ([\s_new, synthDef.name, node, 4,  previousNode]++
											(this.getSystemMsg) ++ (this.getUserMsg))
			);
		};
		
		// and just make a synth for ordering effects in LNX_Instruments:orderEffects
		synth = Synth.basicNew(synthDef.name, server, node);
		
	}
	
	stopDSP{
		if (node.notNil) {server.sendBundle(nil, [11, node]) };
		node = nil;
		synth = nil;
	}
	
	// load & preset changes
	updateDSP{
		// updated by models. Will this always be the case?
	}
	
	// when is this called ? will use for scalar / initial rate
	replaceDSP{|latency|
		var previousNode = node;  // keep old node
		node = server.nextNodeID; // and get a new one
		
		// send the new synth to the server
		server.sendBundle(latency, ([\s_new, synthDef.name, node, 4,  previousNode]
			++ (this.getSystemMsg) ++ (this.getUserMsg)));
			
		synth = Synth.basicNew(synthDef.name, server, node);
							
	}
		
	// dissect the synth def into appropriate parts
	dissectSynthDef{|def|
		var controlNames, metaData, names, indices, defaultValues, types;
		var missingArgs, failed=false;
		
		valid = true; // should this -->
		
		systemIndices = IdentityDictionary[];
		system        = IdentityDictionary[];
		user          = IdentityDictionary[];
		userList      = IdentityDictionary[];
		
		controlNames  = def.allControlNames;
		names         = controlNames.collect(_.name);
		indices       = controlNames.collect(_.index);
		defaultValues = controlNames.collect(_.defaultValue);
		metaData      = def.metadata[\specs];
		
		// test for compulsory arguments before proceeding
		missingArgs=[];
		#[\out, \amp, \in, \inAmp].do{|argName|
			if (names.includes(argName).not) {
				missingArgs=missingArgs.add(argName);
			}
		};
		if (missingArgs.size>0) {
			this.warnMissingArgs(missingArgs);
			failed=true;
		};
		
		if (failed) {^false}; // drop if failed
		
		valid = true; // <-- be moved here (also for instrument)
		
		this.successError; // pass was a success
		
		lastBuildString = codeModel.string.copy; // it didn't fail so this will be last successful
		
		// work out which type the control is. either system, user or userList
		types = names.collect{|name,i|
			if ((#[\on, \out, \amp, \bpm, \clock, \i_clock,
				   \sendOut, \sendAmp, \in, \inAmp, \poll, 
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
		this.buildModels(def); // join system to models
		synthDef = def         // and store
		
		^true; // succesful return
				
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
		var widthMod;

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
				if (node.notNil) {
					// no point setting a scalar
					if (synthDefControl.rate!=\scalar) {
						server.sendBundle(latency,["/n_set",node,synthDefControl.index,val]);
					}{
						this.replaceDSP(latency); // use replace instead	
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
			widthMod=(gui[\userGUIScrollView].bounds.width/33.57).asInt; // no items pw
			while ( {
				viewRect=Rect(20 + ((viewIndex%widthMod)*27), 
						84+((viewIndex/widthMod).asInt*33), 35, 35);
				gui[\userGUIScrollView].intersects(viewRect.insetBy(-5,-5));
			},{ viewIndex = viewIndex + 1 });

			// and make the gui
			userViews[indices] = MVC_MyKnob(userModels[indices],gui[\userGUIScrollView],
				viewRect,gui[\userGUI])
					.color_(\on,Color(1,71/129,1))
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
		}.defer(0.1);  
		
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
		
		var newView, oldView, selectedCollection, selectedBounds = Rect(l,t,w,h), class;
		
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

		class = guiTypes[i].asClass;

		// make the new view
		newView = class.new(false,oldView.model,gui[\userGUIScrollView], 
											  selectedBounds,gui[\userGUI])
				.colors_(oldView.colors)
				.boundsAction_(oldView.boundsAction)
				.mouseDownAction_(oldView.mouseDownAction)
				.colorAction_(oldView.colorAction);
		
		if ((class==MVC_OnOffView) || (class==MVC_OnOffRoundedView)) {
			newView.showNumberBox_(false);	
		};
		
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


