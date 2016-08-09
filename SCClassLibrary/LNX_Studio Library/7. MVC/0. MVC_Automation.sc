
////////////////////////////////////////////////////////////////////////////////////////////////////
//
// An MVC_AutoEvent is a single MVC_Automation event which has...
// a parent automation, model, time in beats and a value
//
////////////////////////////////////////////////////////////////////////////////////////////////////

MVC_AutoEvent{

	var <automation, <model, <beat, <value, <beatInt, <beatFrac, notFree=true;

	*new {|automation,model,beat,value| ^super.new.init(automation,model,beat,value) }

	init {|argAutomation, argModel,argBeat,argValue|
		automation = argAutomation;
		model      = argModel;
		beat       = argBeat;
		beatInt    = beat.asInt;
		beatFrac   = beat.frac;
		value      = argValue
	}

	beat_{|argBeat|
		beat       = argBeat;
		beatInt    = beat.asInt;
		beatFrac   = beat.frac;
	}

	printOn {|stream| stream << this.class.name << "(" << automation<< ", " << model << ", "
		<< beat << ", " << value <<  ")" }

	// bang assumed to be called at the start of beat, so will sched in beatFrac time
	bang{|absTime,jumpTo|
		{ if (notFree) {model.autoIn_(value,jumpTo)}; nil }.sched(beatFrac*absTime)
	}

	// let me go
	free{
		notFree = false;
		automation = model = beat = value = beatInt = beatFrac =nil;
	}

	// am i free?
	isFree{^notFree.not} // i'm using notFree because its less testing in bang method above

}

/* ////////////////////////////////// MVC_Automation ///////////////////////////////////////////////

MVC_Automation.allAutomations.postList;
MVC_Automation.allEvents.postList;
MVC_Automation.allAutomations.postList;
MVC_Automation.allAutomations.collect(_.name).sort.postList;
MVC_Automation.allEvents.postList;
a.models.do(_.freeAutomation);
a.freeAllAutomation;

MVC_Automation.jumpTo(12);

////////////////////////////////////////////////////////////////////////////////////////////////////
//
// a MVC_Automation is a collection of MVC_AutoEvent(s) attached to an MVC_Model
// These are created once recording automation and the model value is changed.
//
///////////////////////////////////////////////////////////////////////////////////////////////// */

MVC_Automation {

	classvar <>verbose=false;
	classvar >studio, api, interface;
	classvar <allAutomations, <allEvents, <>isPlaying=true;
	classvar lastTime=0;
	classvar <newRef=false, <refBeat, <refAbsTime, <refTime;
	classvar <>gui, <models, <duration=1, <>fps=20, nextTime;
	classvar <zoom=1, <offset=0, <cx, <xc, mouseUp = true, selA, selB;

	var <model, <events, <>startValue, <>overwrite=false, owLastTime;

	///////////

	*initClass{
		Class.initClassTree(LNX_API);
		allAutomations  = [];
		allEvents       = IdentityDictionary[]; // allEvents[beat][number] = MVC_AutoEvent()
		models          = IdentityDictionary[]; // models for gui (not the model in the automation)
		CmdPeriod.add({ allAutomations.do{|autos| autos.overwrite_(false) } }); // cancel overwrite
		// api & interface for networking
		api = LNX_API.newPermanent(this, \auto, #[\netNew, \netGCOD, \netDeleteAll, \netEdit]);
		interface = #[\test, \netAddEvent, \netRRM, \netFreeEvent2, \netAddEventMouse,
					\netRemoveRangeMouse, \netDelete, \netEdit, \netDrawLine];
	}

	*new {|model,startValue,send=true| ^super.new.init(model,startValue,send) }

	init {|argModel,argStartValue,send|
		model=argModel;
		events = [];
		startValue = argStartValue;
		allAutomations=allAutomations.add(this);
		// we get parent id & control id, get MVC_Automation to network & add & update if absent
		// the api belongs to the class not the instacnce
		if (send) { api.groupCmdOD(\netNew, startValue, *this.getNetDetails) };
	}

	// this call comes from the already created instance above if this is same user
	// now we have everthing we need to add if absent
	// startValue, parent, parentID, midiControl, cotrolID, model;
	*netNew{|startValue, uid, parentID, cotrolID|
		var model = this.getModel(parentID, cotrolID);
		model.netAddAuto(startValue,uid); // take generation outside to the model
	}

	// info methods
	size         { ^events.size }
	getDetails   { ^model.getDetails }
	getDetails2  { ^model.getDetails2 }
	getNetDetails{ ^[studio.network.thisUserID]++(model.getDetails2)}
	parent       { ^model.parent }
	name         { ^this.getDetails.add(")").insert(1," (").join}
	controlSpec  { ^model.controlSpec }
	numberFunc   { ^model.numberFunc }

	printOn {|stream| stream << this.class.name << "(" << model << ", " << startValue << ")"}

//	// add events to the events list in the order they happen.
//	// NB reversing the while will be quicker ?
//	addEventInOrder{|event|
//		var beat  = event.beat;
//		var size = events.size;
//		var index = size - 1;
//		if (events.size>0) {
//			while { (index >= 0 ) and: {beat < (events.at(index).beat)}} {
//				index = index - 1
//			};
//			events = events.insert(index+1,event);
//		}{
//			events = events.add(event);
//		};
//	}

	addEventInOrder{|event|
		var beat  = event.beat;
		var index = 0;
		var size = events.size;
		if (events.size>0) {
			while { (index < size ) and: {beat > (events.at(index).beat)}} {
				index = index + 1
			};
			events = events.insert(index,event);

		}{
			events = events.add(event);
		};
	}

	*updateDurationAndGUI{
		this.updateDuration;
		this.refreshGUI;
	}

	// update the longest duration any automation lasts for, used when displaying automation in gui
	// this really cuts down on cpu, maybe make this lazy for recording?
	*updateDuration{
		duration=1;
		allEvents.pairsDo{|key,item|
			if(item.isEmpty) { allEvents[key]=nil}{ duration=duration.max(key) }
		};
	}

	// value in from MVC_Model (to review all sources)... i think i've check all sources now tbc
	// this is where the models trigger automation recording
	valueIn_{|value,argBeat|
		var beat, autoEvent, beatInt, beatNoLatency;
		var now = SystemClock.now;
		if (argBeat.isNumber) {
			beat = argBeat
		}{
			beat = MVC_Automation.whatBeatIsNowWithLatency;
		};
		beatNoLatency = MVC_Automation.whatBeatIsNow;
		if (beat.notNil) {
			// set dureation of overwtite time to 1 sec after last valueIn_ event
			if (owLastTime.isNil) {
				overwrite  = true; 			// start overwriting
				owLastTime = now;				// last overwrite time is now

				MVC_Automation.fps_(1);
				lastTime=now;

				// remove any events coming after this one
				this.groupCmdOD(\netRRM, beat, beatNoLatency.asInt+1);
				// use rmobeRangeMouse to clear to next
				// and prevent any in the next 1 sec

				{
					var now = SystemClock.now;
					if (now-owLastTime>=1) {	// have we gone over 1 complete second?
						overwrite  = false;	// we are no longer overwriting
						owLastTime = nil;		// reset for next time
						MVC_Automation.fps_(20).lazyRefreshGUI;
						nil           		// stop scheduling now
					}{
						(1-(now-owLastTime))	// next time to sched for 1 complete second
					}
				}.sched(1); // sched for 1 second time
			}{
				owLastTime = now;	// update last overwrite time
			};
			this.groupCmdOD(\netAddEvent, beat, value);
			if (verbose) {"<".post};
		};
	}

	// net version of removeRangeMouse used in valueIn_
	netRRM{|uid,from,to| this.removeRangeMouse(from,to) }

	// add event via host
	netAddEvent{|uid,beat,value|
		var autoEvent;
		var beatInt = beat.asInt;

		// make an array if not present
		if (allEvents[beatInt].isNil) { allEvents[beatInt]=[] };

		// now add the new event
		autoEvent = MVC_AutoEvent(this,model,beat,value);
		this.addEventInOrder(autoEvent);
		allEvents[beatInt]=allEvents[beatInt].add(autoEvent);

		// update stuff
		duration=duration.max(beat);   // update duration if increased
		MVC_Automation.lazyRefreshGUI; // should say source to lazy refresh to prevent updates ?
		studio.mixerGUI[\autoRec].flash; // flash the mixer record widget

	}

	// the clock has stopped so update seq Index and pointer
	*clockStop{|beat,latency|
		{
			if (gui.notNil) {
				gui[\seqIndex] = beat;
				if (models[\follow].value.isTrue) {
					models[\offset].lazyValueAction_((beat/duration).clip(0,1));
				}{
					this.refreshPointer;
				};
			}
		}.defer(latency);
	}

	// use the next incoming clock event as our new time reference
	*clearRef{
		newRef = false;
		refBeat = refAbsTime = refTime = nil;
	}


	// use the next incoming clock event as our new time reference
	*pickUpNewRef{
		newRef = true;
		refBeat = refAbsTime = refTime = nil;
	}

	// update our time reference, this will be a steady reference point until we change state.
	*updateBeatRef{|beat,absTime|
		if ((newRef) or: {(refAbsTime.notNil) and: { refAbsTime!=absTime}}) {
			this.updateBeatRefNow(beat,absTime);
		};
	}

	// no testing for use with jumpTo
	*updateBeatRefNow{|beat,absTime|
		newRef = false;
		refBeat = beat;
		refAbsTime = absTime;
		refTime = SystemClock.now;
	}

	// tell me what the beat is, worked out from updateBeatRef ( ^ above )
	*whatBeatIsNow{
		if (refAbsTime.notNil) {
			^refBeat + ((SystemClock.now - refTime)/refAbsTime)
		}{
			^nil
		};
	}

	// tell me what the beat is, worked out from updateBeatRef ( ^ above )
	*whatBeatIsNowWithLatency{
		if (refAbsTime.notNil) {
			^refBeat + ((SystemClock.now - (studio.actualLatency) - refTime)/refAbsTime)
		}{
			^nil
		};
	}

	// clock 3 in form LNX_Studio
	*clockIn3{|beat,absTime,latency,absBeat|

		this.updateBeatRef(beat,absTime); // update beat ref if needed
		beat     = beat.asInt;            // asInt to make sure identity in a dictionary
		lastTime = SystemClock.now;       // last time is now now

		if (isPlaying) {
			// for all events happening in this beat do the following...
			allEvents[beat].copy.do{|event|

				if (event.automation.isNil) {
					// i don't know why this happens, it shouldn't

				}{


					// if this automation is in overwrite mode
					if (event.automation.overwrite) {
						event.automation.freeEvent2(event); // remove other automations
						if (verbose) {"*".post};
					}{
						event.bang(absTime); // else make it happen :)
						if (verbose) {">".post};
					};
				}
			};
			// flash the mixer play widget
			if (allEvents[beat].size>0) { studio.mixerGUI[\autoPlay].flash };
		};

		// only update gui every 3 beats to save cpu
		if ((beat%3)==0) {
			{
				if (gui.notNil) {
					gui[\seqIndex] = beat;
					if (models[\follow].value.isTrue && mouseUp) {
						models[\offset].lazyValueAction_((beat/duration).clip(0,1));
					}{
						this.refreshPointer;
					};
				}
			}.defer(latency);
		};

	}

	// free a single event - network version checking for equality
	freeEvent2{|event| this.groupCmdOD(\netFreeEvent2, event.beat, event.value) }

	netFreeEvent2{|uid,beat,value|
		// find all events at this beat that belong to this automation, have the same beat & value
		var eventsToRemove = allEvents[beat.asInt].select{|event|
			(event.automation==this) and: {event.beat==beat} and: {event.value==value}
		};

		eventsToRemove.do{|event|
			allEvents[event.beatInt].remove(event);
			if (allEvents[event.beatInt].isEmpty) { allEvents[event.beatInt]=nil };
			events.remove(event);
			event.free;
		}
	}

	// saving
	getSaveList{
		var l = [ this.size, startValue ];
		events.do{|event| l = l.add(event.beat).add(event.value) };
		^l
	}

	// and loading
	putLoadList{|list|
		list.clump(2).do{|eventList| this.addEvent(*eventList)};
	}

	// called from putLoadList
	addEvent{|beat,value|
		var autoEvent, beatInt = beat.asInt;
		if (allEvents[beatInt].isNil) { allEvents[beatInt]=[] }; // make an array if not present
		autoEvent = MVC_AutoEvent(this,model,beat,value);        // make a new event
		this.addEventInOrder(autoEvent);                         // now add the new event
		allEvents[beatInt]=allEvents[beatInt].add(autoEvent);    // + to all for *clockIn3
	}

	checkDuration{|beat| if (beat>duration) {duration=beat} } // update if needed

	// reset allAutomations to start values, happens when stop button is pressed twice
	*reset{
		if (isPlaying) {
			{
				allAutomations.do{|auto|
					if (auto.model.isProgramModel) {
						{auto.reset}.defer(0.1); // later to do last
					}{
						auto.reset;
					}
				}
			}.defer(studio.absTime*2)
		}
	}

	// reset to start value
	reset{ if (isPlaying) { model.autoIn_(startValue) } }

	// free me!
	free{
		// remove all this events from allEvents
		events.do{|event| allEvents[event.beatInt].remove(event) };
		// remove any empty lists
		allEvents.pairsDo{|beat,list| if (list.isEmpty) { allEvents[beat] = nil}}; // doesn't works
		allAutomations.remove(this);		// remove this automation from allAutomations
		events.do(_.free);					// free the event itself
		model = events = startValue = nil;	// clear vars
	}

	// free a single event
	freeEvent{|event|
		allEvents[event.beatInt].remove(event);
		if (allEvents[event.beatInt].isEmpty) { allEvents[event.beatInt]=nil };
		events.remove(event);
		event.free;
	}


	// delete all the automations
	*guiDeleteAll{ api.groupCmdOD(\netDeleteAll) }

	*netDeleteAll{ studio.freeAllAutomation } // net reciever of above

	// delete this automation
	guiDelete{ this.groupCmdOD(\netDelete) }

	// and net receiver of guiDelete
	netDelete{
		model.freeAutomation;
		MVC_Automation.refreshGUI;
	}

	// called from mouse only
	addEventMouse{|beat,value| this.groupCmdOD(\netAddEventMouse, beat, value) }

	// net receiver of addEventMouse (above)
	netAddEventMouse{|uid,beat,value,refresh=true|
		var autoEvent, beatInt = beat.asInt;
		if (beat<=0) {
			startValue=value;
		}{

			// find previous event might slow things down

			var previousEvent = this.findEventBefore(beat);
			if ((previousEvent== (-1)) or:{previousEvent.value!=value}) {

				if (allEvents[beatInt].isNil) { allEvents[beatInt]=[] }; //make array ifNot there
				autoEvent = MVC_AutoEvent(this,model,beat,value);        // make a new event
				this.addEventInOrder(autoEvent);                         // now add the new event
				allEvents[beatInt]=allEvents[beatInt].add(autoEvent);    // + add to class var

			}

		};
		if (refresh) {gui[\graph].refresh};
	}

	// net receiver of addEventMouse (above)
	netAddEventMouse_DrawLine{|uid,beat,value,refresh=true|
		var autoEvent, beatInt = beat.asInt;
		if (beat<=0) {
			startValue=value;
		}{
			// find previous event might slow things down ??
			if (allEvents[beatInt].isNil) { allEvents[beatInt]=[] }; //make array ifNot there
			autoEvent = MVC_AutoEvent(this,model,beat,value);        // make a new event
			this.addEventInOrder(autoEvent);                         // now add the new event
			allEvents[beatInt]=allEvents[beatInt].add(autoEvent);    // + add to class var
		};
		if (refresh) {gui[\graph].refresh};
	}


	// delete a range of values
	guiRemoveRangeMouse{|last,now| this.groupCmdOD(\netRemoveRangeMouse, last, now) }

	netRemoveRangeMouse{|uid, last, now| this.removeRangeMouse(last, now) } // from above

	// remove a range of events from last to now, used in mouseMoveAction
	removeRangeMouse{|last,now|
		var from=min(last,now);
		var to  =max(last,now);
		// which direction is the mouse moving?
		if (now>last) {
			// from left to right
			events.copy.do{|event|
				if ((event.beat>from)and:{event.beat<=to}) {
					this.freeEvent(event);
				}
			}
		}{
			if (now<last) {
				// from right to left
				events.copy.do{|event|
					if ((event.beat>=from)and:{event.beat<to}) {
						this.freeEvent(event);
					}
				}
			}{
				// else equal
				events.copy.do{|event|
					if (event.beat==from) { this.freeEvent(event) }
				}
			}
		}
	}

	// new style jump, do events in order on time line
	*jumpToEventsInOrder{|beat|
		var events =[];
		allAutomations.do{|automation|
			var event = automation.collectEventBefore(beat);
			if (event.notNil) { events=events.add(event) };
		};

		events = events.sort{|a, b| a.beat <= b.beat };

		events.do{|event| event.bang(refAbsTime?0,\jumpTo) };

	}

	// get the event but don't bang it unless a start event
	collectEventBefore{|beat|
		var event = this.findEventBefore(beat);
		if (event==(-1)) {
			this.reset;
			^nil
		}{
			^event
		};
	}

	// (class) all Automations jump to beat in song
	*jumpTo{|beat|
		this.jumpToEventsInOrder(beat);
		//if (isPlaying) { allAutomations.do{|automation| automation.jumpTo(beat) } };
	}

	// jump to beat in song (this is not used now)
	jumpTo{|beat|
		var event = this.findEventBefore(beat);
		overwrite = false; // stop overwrite
		if (event==(-1)) {
			this.reset;
		}{
			event.bang(refAbsTime?0,\jumpTo);
		};
	}

	// find the previous event to this beat
	findEventBefore{|beat|
		var jumpTo = -1;
		events.do{|event,i|
			if (event.beat<=beat) {
				jumpTo = event
			}{
				^jumpTo;
			}
		};
		^jumpTo
	}

	// jump when to transport stopped
	*jumpWhileStopped{|beat|
		this.jumpToEventsInOrder(beat);
		this.clearRef;
		{
			if (gui.notNil) {
				gui[\seqIndex] = beat;
				if (models[\follow].value.isTrue && mouseUp) {
					models[\offset].lazyValueAction_((beat/duration).clip(0,1));
				}{
					this.refreshPointer;
				};
			}
		}.defer;
	}

	// GUI Widgets /////////////////////////////////////////////////////////////////////////////

	// \ins, \insAll, \rem, \remAll

	*guiEdit{|cmd|
		if ((gui[\automation].notNil) && (selA.notNil)) {
			api.groupCmdOD(\netEdit,cmd,selA,selB);
		};
	}

	*netEdit{|cmd,selA,selB|
		allAutomations.do{|auto| auto.netEdit(nil,cmd,selA,selB,false) }; // <- nil is uid
		this.updateDurationAndGUI;
	}

	guiEdit{|cmd| if (selA.notNil) { this.groupCmdOD(\netEdit,cmd,selA,selB) } }
	netEdit{|uid,cmd,selA,selB,update=true|
		#selA,selB = [selA,selB].sort;
		if (cmd.asSymbol==\rem) {
			this.removeRange(selA,selB); // remove range
		}{
			this.insert(selA,selB);      // insert space
		};
		if (update) { this.class.updateDurationAndGUI };
	}

	insert{|from,to|
		var range = to - from;
		if (range<=0) {
			to = models[\barLength] * 6 + from;
			range = to - from;
		}; // set range as bar if a range is not selected
		events.copy.do{|event|
			if (event.beat>=from) {
				// remove it
				allEvents[event.beatInt].remove(event);
				if (allEvents[event.beatInt].isEmpty) { allEvents[event.beatInt]=nil };
				events.remove(event);
				// change its times
				event.beat_(event.beat + range);
				// and add it again
				if (allEvents[event.beatInt].isNil) { allEvents[event.beatInt]=[] };
				this.addEventInOrder(event);
				allEvents[event.beatInt]=allEvents[event.beatInt].add(event);
			};
		};
	}

	removeRange{|from,to|
		var range = to - from;
		events.copy.do{|event|
			if ((event.beat>=from)and:{event.beat<to}) {
				this.freeEvent(event);
			}{
				if (event.beat>=to) {
					// remove it
					allEvents[event.beatInt].remove(event);
					if (allEvents[event.beatInt].isEmpty) { allEvents[event.beatInt]=nil };
					events.remove(event);
					// change its times
					event.beat_(event.beat - range);
					// and add it again
					if (allEvents[event.beatInt].isNil) { allEvents[event.beatInt]=[] };
					this.addEventInOrder(event);
					allEvents[event.beatInt]=allEvents[event.beatInt].add(event);
				};
			};
		};
	}

	guiDrawLine{|lastMouseBeat, beat, stepSize, lastMouseValue, thisMouseValue|
		this.groupCmdOD(\netDrawLine,lastMouseBeat, beat, stepSize, lastMouseValue, thisMouseValue)
	}

	netDrawLine{|uid, lastMouseBeat, beat, stepSize, lastMouseValue, thisMouseValue|
		var controlSpec = this.controlSpec;
		forBy(lastMouseBeat,beat,stepSize,{|i,j|
			var index = (i-lastMouseBeat)/(beat-lastMouseBeat);
			this.netAddEventMouse_DrawLine(nil,i,
				controlSpec.map((((1-index) * lastMouseValue) + ( thisMouseValue * index))),
				false   // to stop refresh
			);
		});
		gui[\graph].refresh;
	}


	*barLength{^models[\barLength].value}
	*barLength_{|beats| ^models[\barLength].valueAction_(beats)}

	// create the gui interface
	*createWidgets{|window|

		var w,h, h2, lastValue, thisValue, lastX, thisX, beatWidth, controlSpec, numberFunc;
		var gridWidth, gridAdjust, lastMouseBeat, lastMouseValue, lastMouseX, low, hi;
		var draw, ii, step, ev, size, lastY, thisY;

		cx = {|x| x * zoom - (w*offset*(zoom-1)) }; // transform of x with zoom & offset
		xc = {|x| x + (w*offset*(zoom-1)) / zoom }; // inverse of cx.()

		gui = IdentityDictionary[];

		this.updateGUIList;

		gui[\window] = window;

		gui[\items] = MVC_ListView2(gui[\window],Rect(205, 5, 273, 110))
			.items_(gui[\names])
			.font_(Font("Helvetica",11))
			.action_{
				gui[\automation] = gui[\automations][gui[\items].value];
				gui[\graph].refresh;
			}
			.actions_(\deleteKeyAction, {
				if (gui[\automation].notNil) {gui[\automation].guiDelete}
			})
			.actions_(\doubleClickAction, {
				var id = gui[\automation].parent.id;
				if (id>=0) { studio.selectInst(id) };
			})
			.color_(\background, Color(0,0,0,0.75));

		gui[\automation] = gui[\automations][gui[\items].value];
		gui[\seqIndex]   = 0;

		// zoom & offset models
		models[\zoom]      = [1,[1,inf],{gui[\graph].refresh}].asModel;  // zoom
		models[\offset]    = [0,[0,1]  ,{MVC_Automation.lazyRefreshGUI}].asModel;  // offset
		models[\quant]     = [0,[0,128,\lin,1], (label_:"Quantise:")].asModel;  // quantise
		models[\penMode]   = [2,[0,3,\lin,1], (label_:"Pen Mode:")].asModel; // write or erase
		models[\follow]    = [0,\switch].asModel;
		models[\barLength] = [32,[1,128,\lin,1],{gui[\graph].refresh},
												(label_:"Bar (steps):")].asModel;
		models[\aValue]    = [0,[-inf,inf], (label_:"=")].asModel;

		gui[\theme2]=(	\orientation_  : \horiz,
						\resoultion_	 : 3,
						\visualRound_  : 0.001,
						\rounded_      : true,
						\font_		 : Font("Helvetica",12),
						//\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.black,
										\background : Color(0.43,0.44,0.43)*1.4,
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : Color.black,
										\focus : Color(0,0,0,0.5)));

		gui[\themeOnOff] = (	\mouseMode_:		\multi,
							\labelShadow_:	false,
							\rounded_: 		true,
							\orientation_: 	\horiz,
							\colors_:			( \label: Color.black, \on: Color(1,1,1,0.5),
												\off: Color(1,1,1,0.5)));

		// logo
/*		MVC_ImageView(gui[\window],Rect(33, 3, 107, 22))
			.image_("fontImages/Automation.tiff");	*/

		// pen mode (write, erase, slect, value)
		gui[\write] = MVC_OnOffView(gui[\window],Rect(10, 91, 49, 19), gui[\themeOnOff])
			.action_{ models[\penMode].lazyValueAction_(1) }
			.strings_(["Write"]);

		gui[\erase] = MVC_OnOffView(gui[\window],Rect(10, 62, 49, 19), gui[\themeOnOff])
			.action_{ models[\penMode].lazyValueAction_(0) }
			.color_(\on, Color(1,0.2,0.2))
			.strings_(["Erase"]);

		gui[\select] = MVC_OnOffView(gui[\window],Rect(10, 32, 49, 19), gui[\themeOnOff])
			.action_{ models[\penMode].lazyValueAction_(2) }
			.strings_(["Select"]);

		gui[\aValue] = MVC_OnOffView(gui[\window],Rect(76, 91, 49, 19), gui[\themeOnOff])
			.action_{ models[\penMode].lazyValueAction_(3) }
			.strings_(["Value"]);

		MVC_FuncAdaptor(models[\penMode]).func_{|me,value|
			[\erase,\write,\select,\aValue].do{|item,j|
				if (j==value) {
					if (item==\erase) {
						gui[item].color_(\on,Color(1,0.3,0.3));
						gui[item].color_(\off,Color(1,0.3,0.3));
					}{
						if (item==\select) {
							gui[item].color_(\on,Color(0.3,1,0.3));
							gui[item].color_(\off,Color(0.3,1,0.3));
						}{
							gui[item].color_(\on,Color(0.7,0.7,1,0.7));
							gui[item].color_(\off,Color(0.7,0.7,1,0.7));
						}
					};
				}{
					gui[item].color_(\on,Color(0.25,0.25,0.25,0.5));
					gui[item].color_(\off,Color(0.25,0.25,0.25,0.5));
				};
			};
		}.freshAdaptor;


		// barLength
		MVC_NumberBox(models[\barLength], gui[\window],Rect(145, 32, 56, 18),  gui[\theme2])
			.labelShadow_(false);

		// Quantise
		MVC_NumberBox(models[\quant], gui[\window],Rect(144, 62, 56, 18),  gui[\theme2])
			.labelShadow_(false);

		// a value
		MVC_NumberBox(models[\aValue], gui[\window],Rect(144, 91, 56, 18),  gui[\theme2])
			.labelShadow_(false);


		// edit menu
		MVC_PopUpMenu3(window, Rect(144, 6, 56, 18))
				.items_([
					"Insert Bar",
					"Insert Bar Into All",
					"-",
					"Remove Selection",
					"Remove Selection From All",
					"-",
					"Delete This Automation",
					"Delete All Automations"
				])
				.staticText_("Edit")
				.showTick_(false)
				.color_(\background,Color(1,1,1,0.7))
				.color_(\string,Color.black)
				.canFocus_(false)
				.font_(Font("Helvetica", 12))
				.action_{|me|
					switch (me.value.asInt)
						 {0}{
							if (gui[\automation].notNil) { gui[\automation].guiEdit(\ins) }
						}{1}{
							this.guiEdit(\ins)
						}{3}{
							if (gui[\automation].notNil) { gui[\automation].guiEdit(\rem) }
						}{4}{
							this.guiEdit(\rem)
						}{6}{
							if (gui[\automation].notNil) {gui[\automation].guiDelete}
						}{7}{
							this.guiDeleteAll
						}
						;
				};

		// offset
		gui[\offset] = MVC_SmoothSlider(gui[\window],models[\offset],Rect(135, 405+15, 343, 20))
			.thumbSizeAsRatio_(1)
			.resize_(5)
			.color_(\knob,Color(1,1,1,86/125))
			.color_(\hilite,Color(0,0,0,0.5))
			.color_(\numberUp,Color.black)
			.color_(\numberDown,Color.white);

		// jumpTo
		MVC_OnOffView(window, Rect(10, 5, 20, 19))
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.strings_([\return])
			.mode_(\icon)
			.action_{|me| if (selA.notNil) { studio.guiJumpTo( selA ) }};

		// follow
		MVC_OnOffView(models[\follow], gui[\window],Rect(10, 405+15, 45, 20))
			.strings_(["Follow"])
			.rounded_(true)
			.color_(\on,Color(0.7,0.7,1,0.7))
			.color_(\off,Color(0,0,0,0.5));

		// zoom out
		MVC_OnOffView(gui[\window],Rect(60, 405+15, 20, 20),"-")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{
				models[\zoom].multipyValueAction_(1/1.66);
				gui[\offset].thumbSizeAsRatio_(1/(models[\zoom].value));
			};

		// zoom in
		MVC_OnOffView(gui[\window],Rect(85, 405+15, 20, 20),"+")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{
				var ratio;
				zoom  = zoom * 1.66;                    // apply zoom as if ok
				ratio = (duration/(w*zoom));            // ratio of beat width with new zoom
				ratio = ratio.clip(0.02,inf);           // clip it to a max zoom in
				zoom  = duration / ( ratio * w) ;       // use clipped ratio to get cliped zoom
				models[\zoom].valueAction_(zoom);       // store it in the model
				gui[\offset].thumbSizeAsRatio_(1/zoom); // update the size of the offset thumb
			};

		// fit to window
		MVC_OnOffView(gui[\window],Rect(110, 405+15, 20, 20),"=")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{
				models[\zoom].valueAction_(1);
				gui[\offset].thumbSizeAsRatio_(1/(models[\zoom].value));
			};

		gui[\graph] = MVC_UserView(gui[\window], Rect(10, 120, 468, 280+15) )

		// 0.0625 is max resoultion we would need when drawing

		.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			mouseUp = false;
			if (gui[\automation].notNil) {
				lastMouseBeat = (xc.(x-2)/beatWidth).round(models[\quant]*6).round(0.0625);
				if (models[\penMode]==2) {
					selA = selB = lastMouseBeat;
					me.refresh;
				}{
					// if is same
					gui[\automation].guiRemoveRangeMouse(lastMouseBeat,lastMouseBeat);
					if ((models[\penMode]==1)||(models[\penMode]==3)) {
						lastMouseX = x;
						lastY = ((h-y+4)/h).clip(0,1);
						lastMouseValue = controlSpec.map(lastY);
						if (models[\penMode]==3) {
							lastMouseValue=models[\aValue].value;
							if (gui[\automation].model.isProgramModel) {
								lastMouseValue=lastMouseValue-1
							};
						};
						gui[\automation].addEventMouse(lastMouseBeat,lastMouseValue);
					}{
						gui[\graph].refresh;
					};
				};
			};
		}
		.mouseMoveAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			var beat;
			if (gui[\automation].notNil) {
				beat = (xc.(x-2)/beatWidth).round(models[\quant]*6)
										.round(0.0625).clip(0,duration);
				if (models[\penMode]==2) {
					selB = beat;
					me.refresh;
				}{
					// delete over range
					gui[\automation].guiRemoveRangeMouse(lastMouseBeat,beat);
					if ((models[\penMode]==1)||(models[\penMode]==3)) {
						var thisMouseValue, stepSize;
						thisMouseValue = controlSpec.map(((h-y+4)/h).clip(0,1));
						thisY = ((h-y+4)/h).clip(0,1);

						stepSize = ((beat - lastMouseBeat) / (x -lastMouseX))
							.filterNaN.round(0.0625) *((beat - lastMouseBeat).sign);

						if (models[\penMode]==3) {
							stepSize=0;
							thisMouseValue=models[\aValue].value;
							if (gui[\automation].model.isProgramModel) {
								thisMouseValue=thisMouseValue-1
							};
						};

						if ( ((x-lastMouseX)==0) or: {thisMouseValue==lastMouseValue}
							or: {stepSize==0} or:{ models[\quant]>0 }) {
							// add single event
							gui[\automation].addEventMouse(beat,thisMouseValue);
						}{
							gui[\automation].guiDrawLine(lastMouseBeat, beat, stepSize,
											             lastY, thisY);
						};
						lastMouseValue = thisMouseValue;
						lastMouseX = x;
						lastY = thisY;
					}{
						gui[\graph].refresh
					};
					lastMouseBeat = beat;
				};
			};
		}
		.mouseUpAction_{ mouseUp = true }
		.drawFunc_{|me|

			zoom   = models[\zoom].value;   // range 1-inf default:1
			offset = models[\offset].value; // range 0-1   default:0
			w=gui[\graph].view.bounds.width;
			h=gui[\graph].bounds.height;
			h2= h/2;

			Pen.use{

				// background & horizontal ruler
				Pen.width_(3);
				Pen.smoothing_(false);
				Color(0,0,0).set;
				Pen.strokeRect(Rect(0,0,w,h));
				Color(0,0,0,0.8).set;
				Pen.fillRect(Rect(0,0,w,h));
				w=w-3;
				Pen.width_(1);
				Color.black.set;
				Pen.moveTo(2@(h2));
				Pen.lineTo(w@(h2));
				Pen.stroke;
				(Color(0.8,0.8,0.8,0.8)/3).set;
				Pen.moveTo(2@(h2+1));
				Pen.lineTo(w@(h2+1));
				Pen.stroke;
				Color(0,0,0,0.33).set;
				Pen.moveTo(2@((h*0.75)));
				Pen.lineTo(w@((h*0.75)));
				Pen.moveTo(2@((h*0.25)));
				Pen.lineTo(w@((h*0.25)));
				Pen.stroke;
				Color(0.8,0.8,0.8,0.1).set;
				Pen.moveTo(2@((h*0.75+1)));
				Pen.lineTo(w@((h*0.75+1)));
				Pen.moveTo(2@((h*0.25+1)));
				Pen.lineTo(w@((h*0.25+1)));
				Pen.stroke;

				if (gui[\automation].notNil) {

					h=h-4;

					controlSpec =  gui[\automation].controlSpec;
					numberFunc = gui[\automation].numberFunc ?? {MVC_NumberFunc.at('round0.01')};

					beatWidth = w / duration;

					// vertical ruler
					Color(0.8,0.8,0.8,0.1).set;
					Pen.smoothing_(true);
					Pen.font_(Font("Helvetica",10));

					gridWidth  = models[\barLength]*6;
					gridAdjust = ((w/zoom/beatWidth/gridWidth).div(8)+1);
					gridAdjust = (2**(gridAdjust.log2.ceil));
					gridWidth  = gridWidth * gridAdjust;

					low = (xc.(0+2)/beatWidth/gridWidth-1).floor;
					hi = (xc.(w+2)/beatWidth/gridWidth-1).floor;

					Pen.smoothing_(false);

					// selection
					if (selA.notNil) {
						var x=cx.(selA*beatWidth)+1;
						var w1=cx.((selB)*beatWidth)+1-x;
						if (selA==selB) {
							Pen.lineDash_(FloatArray[4,2]);
							Color(0.8,0.8,1,0.4).set;
							Pen.moveTo(x@1);
							Pen.lineTo(x@(h-1));
							Pen.stroke;
							Pen.lineDash_(FloatArray[]);
						}{
							Pen.fillColor_(Color(0.6,0.6,1,0.2));
							Pen.fillRect(Rect(x,1,w1,h-1));
						};
					};

					if (low.asInt.odd) {
						Pen.fillColor_(Color(1,1,1,0.3))
					}{
						Pen.fillColor_(Color(1,1,1,0.1))
					};

					forBy (low, hi, 2, {|i|
						Pen.moveTo( (cx.(i+1*gridWidth*beatWidth).asInt+2) @2);
						Pen.lineTo( (cx.(i+1*gridWidth*beatWidth).asInt+2) @h);
						Pen.stringRightJustIn( (i+1*gridAdjust+1).asString ,
							Rect(cx.(i+1*gridWidth*beatWidth)+2,h-10,10,10)
						); // x scale
					});

					Pen.stroke;

					if ((low.asInt+1).odd) {
						Pen.fillColor_(Color(1,1,1,0.3))
					}{
						Pen.fillColor_(Color(1,1,1,0.1))
					};

					forBy (low+1, hi, 2, {|i|
						Pen.moveTo( (cx.(i+1*gridWidth*beatWidth).asInt+2) @2);
						Pen.lineTo( (cx.(i+1*gridWidth*beatWidth).asInt+2) @h);
						Pen.stringRightJustIn( (i+1*gridAdjust+1).asString ,
							Rect(cx.(i+1*gridWidth*beatWidth)+1,h-10,10,10)
						); // x scale
					});

					Pen.stroke;

					Pen.fillColor_(Color(1,1,1,0.33));
					Pen.smoothing_(true);

					for (low, hi, {|i|
						Pen.stringRightJustIn( (i+1*gridAdjust+1).asString ,
							Rect(cx.(i+1*gridWidth*beatWidth)+2,h-10,10,10)
						); // x scale
					});

					// y scale
					Pen.stringRightJustIn( numberFunc.(controlSpec.map(1)).asString
						++(controlSpec.units), Rect(w-40,2,40,10));

					Pen.stringRightJustIn( numberFunc.(controlSpec.map(0.75)).asString
						++(controlSpec.units), Rect(w-40,h*0.25-4,40,10));

					Pen.stringRightJustIn( numberFunc.(controlSpec.map(0.5)).asString
						++(controlSpec.units), Rect(w-40,h*0.5-4,40,10));

					Pen.stringRightJustIn( numberFunc.(controlSpec.map(0.25)).asString
						++(controlSpec.units), Rect(w-40,h*0.75-2,40,10));
					Pen.stringRightJustIn( numberFunc.(controlSpec.map(0)).asString
						++(controlSpec.units), Rect(w-40,h-10,40,10));

					// the wave form / curve ////////////////////////////////////////
					//
					// extremely efficient drawing of the curve
					// doesn't draw anything off the view bounds
					// clips no of events drawn to 2000 by adjusting step size
					// alising will happen for sizes > 2000

					Color(0.7,0.8,1).set;

					if (fps==1) {
						Pen.width_(1);
						Pen.smoothing_(false);
					}{
						Pen.width_(1.5);
						Pen.smoothing_(true);
					};

					ii    = 0;
					size  = gui[\automation].events.size;
					step  = size.div(2000).clip(1,inf); // we can clip no events drawn with step
					draw  = false;
					lastX = cx.(0);

					// last value starts as startValue
					lastValue = controlSpec.unmap(
						controlSpec.constrain(gui[\automation].startValue)).clip(0,1);

					while ({ ii < size }, {
						ev = gui[\automation].events.at(ii);
						thisX = cx.((ev.beat*beatWidth))+2;

						// this could be moved inside below ??
						thisValue = controlSpec.unmap(
							controlSpec.constrain(ev.value)).clip(0,1);

						// maybe this if statement can be improved
						if (((lastX<0)&&(thisX<0))||((lastX>w)&&(thisX>w))) {
							// if off screen
							if (thisX>w) { ii = size }; // stop drawing now if off righ side
							// if (thisX<0) { step=(size-ii).div(2000).clip(1,inf)}; //new step
							ii = ii + step; // inc
						}{
							if (draw) {
								// we have already drawn before so carry on
								if ((thisX-lastX)>=2) {
									Pen.lineTo(thisX@((1-lastValue)*h+2));
								};
								Pen.lineTo(thisX@((1-thisValue)*h+2));
							}{
								// 1st time drawing so set start (moveTo)
								Pen.moveTo( lastX @((1-lastValue)*h+1));
								if ((thisX-lastX)>=2) {									Pen.lineTo(thisX@((1-lastValue)*h+2));
								};
								Pen.lineTo(thisX@((1-thisValue)*h+2));
								draw=true; // set so we carry on drawing
							};
							ii = ii + step; // inc
						};
						// these are now our last values
						lastValue = thisValue;
						lastX     = thisX;
					});

					if (draw.not) { Pen.moveTo( lastX @((1-lastValue)*h+1)) };
					Pen.lineTo((cx.((w)+2))@((1-lastValue)*h+2));
					Pen.stroke;

					// song pos marker //////////////////////////////////////////////////

					Pen.smoothing_(false);
					gui[\actualSeqX]=cx.(gui[\seqIndex]*beatWidth).asInt+2;
					gui[\lastBeatWidth]=beatWidth;

					if (gui[\actualSeqX]>=0 and:{gui[\actualSeqX]<=(w+4)}) {
						Pen.width_(1);
						Color(1,1,1).set;
						Pen.moveTo(gui[\actualSeqX]@2);
						Pen.lineTo(gui[\actualSeqX]@h);
						Pen.stroke;

					};

				}
			};
		};

	}

	*resetZoom{
		models[\zoom].value_(1);
		models[\offset].valueAction_(0);
	}

	*updateGUIList{
		gui[\automations] = allAutomations.copy.sort{|a,b|
			if (a.parent.instNo==b.parent.instNo) {
				a.name.toLower<b.name.toLower
			}{
				a.parent.instNo<b.parent.instNo
			}
		};
		gui[\names] = gui[\automations].collect(_.name);
	}

	*refreshGUI{
		{
			if (gui.notNil) {
				this.updateGUIList;
				gui[\items].items_(gui[\names]);
				// is the previous automation still here?
				if (gui[\automations].includes(gui[\automation])) {
					// if so keep it selected
					gui[\items].value_(gui[\automations].indexOf(gui[\automation]))
				}{
					// otherwise clip selection to size
					gui[\automation] = gui[\automations].clipAt(gui[\items].value);
					gui[\items].value_(gui[\items].value.clip(0,gui[\automations].size-1));
				};
				this.lazyRefreshGUI;
			}
		}.defer;
	}

	// this needs adjusting for zoom & offset
	*refreshPointer{
		if ((gui.notNil)and:{gui[\lastBeatWidth].notNil}) {
			if (gui[\actualSeqX]!=(cx.(gui[\seqIndex]*gui[\lastBeatWidth]).asInt+2)){
				this.lazyRefreshGUI;
			}
		}
	}

	// frame rate, max time & source
	*lazyRefreshGUI{
		var now;
		if ((gui.notNil) and:{gui[\window].notClosed}) {
			now=SystemClock.seconds;
			if ((now-lastTime)>(1/fps)) {
				lastTime=now;
				nextTime=nil;
				{gui[\graph].refresh}.defer;
			}{
				if (nextTime.isNil) {
					nextTime=lastTime+(1/fps);
					{
						gui[\graph].refresh;
						nextTime=nil;
					}.defer(nextTime-now);
					lastTime=nextTime;
				}
			}
		}
	}

	// network stuff /////////////////////////////////////////////////////////////////////////////

	// get the model from parentID & cotrolID
	*getModel{|parentID,cotrolID|
		var parent, midiControl;
		if (parentID==(-1)) { parent=studio } { parent=studio.insts[parentID] };
		midiControl = parent.midiControl;
		^midiControl.getModel(cotrolID);
	}

	// for comms with this instance need to go via the Meta_Class.
	groupCmdOD{|method...args|
		var details = model.getDetails2;
		api.groupCmdOD(\netGCOD, studio.network.thisUserID, details[0], details[1], method, *args )
	}

	// reciever of above
	*netGCOD{|uid,parentID,cotrolID,method...args|
		if (interface.includes(method)) {
			var model = this.getModel(parentID,cotrolID);
			if ((model.notNil)and:{model.automation.notNil}) {
				model.automation.perform(method,uid,*args)
			}
		}
	}

}

// this silences .autoIn_ bug. bad neil you should find what is going on!
+ Nil { autoIn_{} } // why does nil get called with autoIn_ ?

/*

ERROR: Message 'autoIn_' not understood.
RECEIVER:
   nil
ARGS:
   nil
CALL STACK:
	DoesNotUnderstandError:reportError   0x13368e5e8
		arg this = <instance of DoesNotUnderstandError>
		var s = "ERROR: Message 'autoIn_' not..."
	< closed FunctionDef >   0x133692c58
		arg error = <instance of DoesNotUnderstandError>
	Integer:forBy   0x179487e98
		arg this = 0
		arg endval = 52
		arg stepval = 2
		arg function = <instance of Function>
		var i = 42
		var j = 21
	SequenceableCollection:pairsDo   0x18381a2d8
		arg this = [*54]
		arg function = <instance of Function>
	Scheduler:seconds_   0x1a158b338
		arg this = <instance of Scheduler>
		arg newSeconds = 373.544973445
	Meta_AppClock:tick   0x14fb81888
		arg this = <instance of Meta_AppClock>
		var saveClock = <instance of Meta_SystemClock>
	Process:tick   0x180649508
		arg this = <instance of Main>
ERROR: Message 'autoIn_' not understood.
RECEIVER:
   nil
ARGS:
   nil
CALL STACK:
	DoesNotUnderstandError:reportError   0x1336d9e88
		arg this = <instance of DoesNotUnderstandError>
		var s = "ERROR: Message 'autoIn_' not..."
	< closed FunctionDef >   0x1336d7bd8
		arg error = <instance of DoesNotUnderstandError>
	Integer:forBy   0x179487e98
		arg this = 0
		arg endval = 52
		arg stepval = 2
		arg function = <instance of Function>
		var i = 44
		var j = 22
	SequenceableCollection:pairsDo   0x18381a2d8
		arg this = [*54]
		arg function = <instance of Function>
	Scheduler:seconds_   0x1a158b338
		arg this = <instance of Scheduler>
		arg newSeconds = 373.544973445
	Meta_AppClock:tick   0x14fb81888
		arg this = <instance of Meta_AppClock>
		var saveClock = <instance of Meta_SystemClock>
	Process:tick   0x180649508
		arg this = <instance of Main>

*/

////////////////////////////////////////////////////////////////////////////////////////////////////
