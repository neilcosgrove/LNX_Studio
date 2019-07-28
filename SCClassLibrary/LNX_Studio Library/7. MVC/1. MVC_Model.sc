//////////////////////////////////////////////////////
//                                                  //
// MVC_Models                                       //
//                                                  //
//////////////////////////////////////////////////////
//
// A MVC_Model is a proxy(ref) to a value
// & interacts with MVC_Views, LNX_MIDIPatch (Learn) & Automation
//
// aims:
// have many dependant views for one model that all update using different methods i.e lazyRefreh
// the model does not require a view to exist
// models have ControlSpecs to define their behavour
// a model can learn midi controls
// automation can be recorded & played back
// eset a frame rate of view refreshes
// gobal enable
// MVC_Views can inherit themeMethods eg. All views will have a blue label or what ever you want.
// Maths can be done directly on a model. eg. \freq.asModel + 1

MVC_Model {

	classvar <>studio;
	classvar <>isRecording=false;

	var	<dependants, 			<noDependants=0;

	var	<>action,				<>action2,
		<actions,				<value=0,
		<string,				<enabled=true,
		<>maxStringSize;

	var	<midiLearn=false, 		<hasMIDIcontrol=false,
		<controlID,				<controlGroup,
		<>resoultion=200,		<>defaultControlType=0;  // resoultion might be removed

	var	<>inheritSpec = true,	<controlSpec, <themeMethods, <>constrain = true;
	var <>midiMode='range'; 						// range, switch, button, tap
	var <>synthDefControl;							// used in LNX_Code
	var <>hackAction;								// used in my hack

	var <automation, <>automationActive=true, <>isProgramModel=false;

	var lazyRefresh,  <freed=false;

	// maths & logic support (more to add)

	+{|aNumber|  ^value + aNumber}
	-{|aNumber|  ^value - aNumber}
	/{|aNumber|  ^value / aNumber}
	*{|aNumber|  ^value * aNumber}
	**{|aNumber| ^value ** aNumber}
	=={|aNumber| ^value == aNumber}
	!={|aNumber| ^value != aNumber}
	>{|aNumber|  ^value >  aNumber}
	>={|aNumber| ^value >= aNumber}
	<{|aNumber|  ^value <  aNumber}
	<={|aNumber| ^value <= aNumber}
	isTrue{      ^value.isTrue }
	isFalse{     ^value.isFalse }
	div{|aNumber| ^value.div(aNumber)}
	%{|aNumber|  ^value%aNumber}
	abs{         ^value.abs}
	do{|func|    ^value.do(func)}
	asInt{       ^value.asInt }
	asFloat{     ^value.asFloat}
	asInteger{   ^value.asInterger}

	// automation support  ////////////////////////////////////////////////////////////////

	numberFunc{ ^dependants.asList.first.numberFunc }

	getSaveList{ ^automation.getSaveList }

	putLoadList{|list|
		var startValue = list.removeAt(0);
		if (automation.isNil) {
			automation = MVC_Automation(this,startValue);
		}{
			automation.startValue_(startValue);
		};
		automation.putLoadList(list)
	}

	freeAutomation{ automation.free; automation=nil }
	getDetails{ ^controlGroup.getDetails(controlID) }
	getDetails2{ ^controlGroup.getDetails2(controlID) }
	parent { ^controlGroup.parent }

	// auto start when adding a new instrument
	// lead point in networking but after most done in MVC_Automation
	autoStart{|beat|
		// is playing maybe better
		var startValue = (beat==0).if(1,0); // start value on if beat==0 else starts off
		if (automation.isNil) {
			automation=MVC_Automation(this,startValue,false); // this is creating 2 on sender
		}{
			automation.startValue_(startValue);
		};
		if (beat>0) { automation.addEvent(beat,1).checkDuration(beat) }; // is playing maybe better
		MVC_Automation.refreshGUI;
	}

	// add a new automation, comes via the host
	netAddAuto{|startValue,uid|
		if (automation.isNil) {

			if (uid.asSymbol!=studio.network.thisUserID) {
				// make a new one but do not send else inf loop
				automation=MVC_Automation(this,startValue,false); // this is creating 2 on sender
				MVC_Automation.refreshGUI;
			}
		}{
			// update start value (most likey sender but could be anyone but inlikely)
			automation.startValue_(startValue);
		}
	}

	// this changing model is now adding the event to the automation
	autoValue_{|value,lastValue,beat|
		if ( (isRecording) && (automationActive) and: {studio.isPlaying}) {
			if (automation.isNil) {
				automation=MVC_Automation(this,lastValue);
				automation.valueIn_(value,beat);
				MVC_Automation.refreshGUI;
			}{
				automation.valueIn_(value,beat);
			};
		};
	}

	// automation setting the value of this model
	autoIn_{|val,jumpTo|
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if ((value!=val)or:isProgramModel) {
			value=val;
			action.value(this,value,studio.actualLatency,false,false,jumpTo);
			this.lazyValueRefresh;
			this.changed(\value,value);
			hackAction.value(this,0); // call hackaction (this is from gui)
		}
	}

	/////////////////////////////////////////////////////////////////////////////

	*new { ^super.new.init }

	init {
		dependants   = IdentitySet[];
		actions      = IdentityDictionary[];
		themeMethods = IdentityDictionary[];
		lazyRefresh  = MVC_LazyRefresh().model_(this);
	}

	// add a sub class of MVC_View to the model
	addView{|dependant|
		dependants.add(dependant);
		noDependants=dependants.size;
		// update dependant
		{
			dependant.string_(string)
			.enabled_(enabled)
			.midiLearn_(midiLearn)
			.hasMIDIcontrol_(hasMIDIcontrol);
			if (inheritSpec) { dependant.controlSpec_(controlSpec) };
			themeMethods.pairsDo{|method,args|
				if (dependant.respondsTo(method)) { dependant.perform(method,args) };
			};
			dependant.value_(value);
			if (dependant.respondsTo(\maxStringSize_)) { dependant.maxStringSize_(maxStringSize) } ;
		}.deferIfNeeded;
	}

	// remove a gui item from the model
	removeView{|dependant|
		dependants.remove(dependant);
		noDependants=dependants.size;
	}

	// free all dependants
	releaseAllViews{
		dependants.do(_.free);  // do i need this ??
		dependants=IdentitySet[];
		noDependants=0;
	}

	// add an alternate action
	actions_{|key,action| actions[key]=action }

	// call an action
	valueActions{|key...args|
		actions[key].value(this,*args)
	}

	// free this model and all dependants
	free{
		this.freeAutomation;
		lazyRefresh.free;
		dependants.do(_.free);
		lazyRefresh = automation = dependants = noDependants = action = action2 = actions = value =
		string = enabled = midiLearn = controlID = controlGroup = resoultion =
		defaultControlType = controlSpec = themeMethods = midiMode = hackAction = synthDefControl = nil;
		freed = true;
	}

	// set enabled in all dependants
	enabled_{|bool|
		if (enabled!=bool) {
			enabled=bool;
			dependants.do{|view| view.enabled_(enabled)}
		}
	}

	// set the mapping of the model to min, max & step
	map_{|min,max,step| this.controlSpec_([min,max,\lin,step?0]) }

	// assign a ControlSpec to map to
	controlSpec_{|spec|
		var val;
		if (spec.notNil) { controlSpec=spec.asSpec } { controlSpec=nil };
		// update dependants
		if (inheritSpec) { dependants.do{|view| view.controlSpec_(controlSpec) } };

		if (controlSpec.notNil) {
			this.value_(value);
			// how can i use controlSpec_ and midiSet to organise midi control > model
			if ((controlSpec.minval==0)and:{controlSpec.maxval==1}and:{controlSpec.step==1}) {
				defaultControlType=0;  // 0=knob, toggle, onOff, rotary ??
			};

		}; // this will force a constrain
	}

	// set the value of model and update all guis
	value_{|val,delta|
		if (constrain && (controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (value!=val) {
			value=val;
			dependants.do{|view| view.value_(val,delta)}; // delta is used by lamp
			this.changed(\value,value);
		};
	}

	// there is a discrepency between what is given back by the model and the view
	// here it is (this,value,latency,send,toggle)
	// view it is (this,latency,send,toggle)
	// i need to check and make sure everything is doing the same thing

	// set the value of model, call action and update all guis (also called from view)
	// send is also used to filter events to the LNX_Automation
	valueAction_{|val,latency,send=false,toggle=false,dependant,button|
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (value!=val) {
			if (hasMIDIcontrol && send and: {latency.isNil}) {this.autoValue_(val,value)};
			value=val;
			action.value(this,value,latency,send,toggle);  // this,latency,send
			dependants.do{|view|
				if (view!==dependant) { view.value_(val,latency) } // don't update dependant
			};
			this.changed(\value,value);
			hackAction.value(this,button); // call hackaction (this is from gui)
		}
	}

	// set the value of model, call action and update all guis
	// no test for equality, used in buttons and lamps
	// send is also used to filter events to the LNX_Automation
	doValueAction_{|val,latency,send,toggle,dependant,clickCount|
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (hasMIDIcontrol && send and: {latency.isNil}) {this.autoValue_(val,value)};
		value=val;
		action.value(this,value,latency,send,toggle,clickCount); // this,latency,send
		dependants.do{|view|
			if (view!==dependant) { view.value_(val) } // don't update dependant
		};
		this.changed(\value,value);
	}

	// do the action
	doAction{|latency,send=false,toggle=false| action.value(this,value,latency,send,toggle) }

	// multipy the value of model, call action and update all guis
	// no test for equality, used in buttons and lamps
	// send is also used to filter events to the LNX_Automation
	multipyValueAction_{|val,latency,send,toggle,dependant|
		val = value*val;
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (value!=val) {
			if (hasMIDIcontrol && send and: {latency.isNil}) {this.autoValue_(val,value)};
			value=val;
			action.value(this,value,latency,send,toggle);// this,latency,send
			dependants.do{|view|
				if (view!==dependant) { view.value_(val) } // don't update dependant
			};
			this.changed(\value,value);
		};
	}

	// set the value of model, call 2nd action and update all guis
	// send is also used to filter events to the LNX_Automation
	valueAction2_{|val,latency,send,toggle,dependant|
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (value!=val) {
			value=val;
			if (hasMIDIcontrol && send and: {latency.isNil}) {this.autoValue_(val,value)};
			if (action2.notNil) {
				action2.value(this,value,latency,send,toggle); // this,latency,send
			}{
				action.value(this,value,latency,send,toggle);  // this,latency,send
			};
			dependants.do{|view|
				if (view!==dependant) { view.value_(val) } // don't update dependant
			};
			this.changed(\value,value);
		}
	}

	// set the value of model, call action and update all guis
	// no test for equality, used in buttons and lamps
	// send is also used to filter events to the LNX_Automation
	doValueAction2_{|val,latency,send,toggle,dependant|
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (hasMIDIcontrol && send and: {latency.isNil}) {this.autoValue_(val,value)};
		value=val;
		action2.value(this,value,latency,send,toggle);	// this,latency,send
		dependants.do{|view|
			if (view!==dependant) { view.value_(val) } // don't update dependant
		};
		this.changed(\value,value);
	}

	// set the value of model and lazy update. use this to clip frame rate of models gui
	// auto is used to filter events to the LNX_Automation
	lazyValue_{|val,auto|
		if (auto.isNil) {auto=false; "*MVC_Model:lazyValue_ you may have missed 1 neil".postln };
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (value!=val) {
			if (hasMIDIcontrol && auto) {this.autoValue_(val,value)};
			value=val;
			this.changed(\value,value);
			this.lazyValueRefresh;
		};
	}

	// set the value of model, call action & lazy update. use this to clip frame rate of models gui
	lazyValueAction_{|val,latency,send=false,toggle=false|
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (value!=val) {
			if (hasMIDIcontrol && send and: {latency.isNil}) {this.autoValue_(val,value)};
			value=val;
			action.value(this,value,latency,send,toggle); // this,latency,send
			this.changed(\value,value);
			this.lazyValueRefresh;

		}
	}

	// as above but forces action. used in program changes MoogSub37
	doLazyValueAction_{|val,latency,send=false,toggle=false|
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (hasMIDIcontrol && send and: {latency.isNil}) {this.autoValue_(val,value)};
		value=val;
		action.value(this,value,latency,send,toggle); // this,latency,send
		this.changed(\value,value);
		this.lazyValueRefresh;
	}


	// same as above but no testing on auto. used in LNX_POP for program changes
	lazyValueActionDoAutoBeat_{|val,latency,send=false,toggle=false,beat,offset|
		var draw = false;
		if (constrain &&(controlSpec.notNil)) { val=controlSpec.constrain(val) };
		if (hasMIDIcontrol) {this.autoValue_(val,value,beat)};
		if (value!=val) { draw = true };
		value=val;
		action.value(this,value,latency,send,toggle, offset:offset); // this,latency,send
		this.changed(\value,value);
		if (draw) { this.lazyValueRefresh };
	}

	// register this model with LNX_MIDIControl
	controlID_{|id,argControlGroup,name|
		hasMIDIcontrol=true;
		dependants.do{|view| view.hasMIDIcontrol_(true)};
		controlGroup=argControlGroup;
		controlID=id;
		controlGroup.register(id,this,name);
	}

	// change the control id been used
	changeControlID{|newID|
		if (controlGroup.notNil) {
			if (controlID!=newID) {
				controlGroup.changeModelID(controlID,newID);
				controlID=newID;
			};
		};
	}

	// remove this model from midiControl
	removeMIDIControl{|send=true|
		if (hasMIDIcontrol) {
			if (midiLearn) { this.midiLearn_(false) };
			controlGroup.removeModel(controlID,send);
			hasMIDIcontrol=false;
			dependants.do{|view| view.hasMIDIcontrol_(false)};
			controlGroup = controlID = nil;
		};
	}

	// set to MIDI learn
	midiLearn_{|bool|
		midiLearn=bool;
		if (midiLearn) {
			LNX_MIDIControl.makeElementActive(this);
		}{
			LNX_MIDIControl.makeElementActive(nil);
		};
		dependants.do{|view| view.midiLearn_(bool)};
	}

	// remove from MIDI learn
	deactivate{
		midiLearn=false;
		{dependants.do(_.deactivate)}.defer;
	}

	// midi controls and lazy refresh rates
	midiSet{|val,type,latency,send|
		case { midiMode=='range' } {
			if (type==1) {
				if (val>64) {val=1-value} {val=value}; // toggle
			}{
				if (type==2) {
					val=(val*127.clip(0,127));	// on/off
					if (val>64) {val=1-value} {val=value};
				}{
					if (type==4) {
						val=(value>0).if(0,127);
					}{
						val=val/127; // full range
					};

					// where is rotary?
				};
			};
			if (controlSpec.notNil) { val=controlSpec.map(val) };
			if (value!=val) { this.lazyValueAction_(val,latency,send,(type==1)||(type==2)) };
		} { midiMode=='tap' }{
			this.doValueAction_(value,latency,send,(type==1)||(type==2))
		};
		hackAction.value(this); // call hackaction (this is from midi)
	}
	// i need to think about latency and updating the model vs updating the views

	// set with a range 0-1
	set_{|val,latency,send=true|
		if (freed) { ^this };
		if (controlSpec.notNil) { val=controlSpec.map(val) };
		if (value!=val) { this.lazyValueAction_(val,latency,send) }; // important its lazy!!
	}

	// get the value in the range 0-1
	get{ if (controlSpec.notNil) { ^controlSpec.unmap(value) }{ ^value } }

	// randomise the parameters
	randomise{
		var val;
		val=1.0.rand;
		if (controlSpec.notNil) { val=controlSpec.map(val) };
		if (value!=val) { this.lazyValueAction_(val,nil,true,false) };
	}

	rand{ this.randomise }

	//reset
	reset{
		var val;
		if (controlSpec.notNil) { val=controlSpec.default };
		val = val ? 0;
		if (value!=val) { this.lazyValueAction_(val,nil,true,false) };
	}

	// only refresh at a frame rate
	lazyValueRefresh{ lazyRefresh.lazyValueRefresh }

	// used to update dependants if needed
	updateDependants{
		var val=value;
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		{
			dependants.do{|view| view.value_(val)}
		}.defer
	}

	// set the string of a model, call action and update all guis
	stringAction_{|argString,dependant|
		string=argString.keep(maxStringSize?(argString.size));
		this.valueActions(\stringAction,this);
		dependants.do{|view|
			if (view!==dependant) { view.string_(string) } // don't update dependant
		};
	}

	// set the model string and update dependants
	string_{|argString|
		string=argString.keep(maxStringSize?(argString.size));
		dependants.do{|view| view.string_(string) }; // don't update dependant
	}

	// add theme method data to dependants and themeMethods dictionary
	themeMethods_{|dict|
		dict.pairsDo{|method,args|
			dependants.do{|dependant|
				if (dependant.respondsTo(method)) { dependant.perform(method,args) }
			};
		};
		themeMethods=themeMethods++dict;
	}

	// incase needed, should avoid use
	refresh{ dependants.do(_.refresh) }

	// perform a method on all the dependants (only use if needed)
	// global states should be stored in the model and not accessed directly
	dependantsPerform{|selector ... args|
		dependants.do{|dependant|
			if (dependant.respondsTo(selector)) { dependant.perform(selector,*args) };
		};
	}

	// TO DO, usefull for keeping newly added views upto date to changes i.e global colors etc...
	addDependantsPerform{|selector ... args|
		// same as dependantsPerform
		// but also adds to a dict for views that get added later

	}

}

/////////////////////////////////////////////////////////////////////////////////////////

// convience methods to make models

+ SequenceableCollection {

	/*
	[500, \freq, (label_:"A control"), LNX_MIDIControl() , 1, "A", { "action".postln }].asModel

	resolve items in collection into args for a new model
	the following rules apply:
	   if 1st item is a number use this as the default value
	   add a control using any of the follow: a ControlSpec, a Symbol \freq or an Array [0,1,\lin]
	   a function will become the action of the model { "do something".postln }
	   a LNX_MIDIControl will register this model for midi Control. needs to be followed by an id
	   an IdentityDictionary will perform all items on all dependants (if respondsTo)
	   any Association with do that action on the model. Key is method value is argumnet
	*/

	asModel{
		var model        = MVC_Model();
		var action       = this.findKindOf(Function);
		var spec         = this.findKindOf(Symbol);
		var midiControl  = this.findKindOf(LNX_MIDIControl);
		var data         = this.findKindOf(IdentityDictionary);
		var associations = this.select(_.isKindOf(Association));
		var default;

		// first item in the array is always the default IF it's a number
		if (this.at(0).isKindOf(Number)) { default=this.at(0) };

		// order for spec selection is Symbol, Array, ControlSpec
		if (spec.isNil) { spec = this.findKindOf(SequenceableCollection) };
		if (spec.isNil) { spec = this.findKindOf(ControlSpec) };
		if (spec.isNil) { spec = this.findKindOf(MVC_AudioOutSpec) };

		// add a spec
		if (spec.notNil) {
			spec=spec.asSpec;
			if (spec.notNil) {
				if (default.isNil) { default=spec.default };
				model.controlSpec_(spec);
			}{
				"Spec did not exist".warn;
			}
		};

		// add a midi Control
		if (midiControl.notNil) {
			model.controlID_(
				this.at(this.indexOf(midiControl)+1), // id
				midiControl,						// control
				this.at(this.indexOf(midiControl)+2) // name
			)
		};

		// add an action
		if (action.notNil) { model.action_(action) };

		// and any other association
		associations.do{|a|
			if (a.value.isKindOf(Association)){
				model.perform(a.key,a.value.key,a.value.value)
			}{
				model.perform(a.key,a.value)
			}
		};

		// if there is still not a default make it zero
		if (default.isNil) { default = 0 };

		// add child method data
		if (data.notNil) { model.themeMethods_(data)};

		// put the default value
		model.value_(default);

		^model;
	}

	generateModel{
		var model=this.asModel;
		^[model,model.value];
	}

	// make a list of models and their defaults using generateModel / asModel of each member
	generateAllModels{
		var models,defaults,m,d;
		models=Array.newClear(this.size);
		defaults=Array.newClear(this.size);
		this.do{|item,i|
			#m,d = item.generateModel;
			models[i]=m;
			defaults[i]=d;
		};
		^[models,defaults]
	}

}

+ Number {
	asModel { ^[this].asModel }
	generateModel { ^[this.asModel,this] }
}

+ Symbol {
	asModel { ^[this].asModel }
	generateModel{
		var model=[this].asModel;
		^[model,model.value];
	}
}

+ ControlSpec {
	asModel { ^[this].asModel }
	generateModel{
		var model=[this].asModel;
		^[model,model.value];
	}
}

+ String {
	asModel { ^MVC_Model().string_(this) }
	generateModel{
		var model=[this].asModel;
		^[model,model.value];
	}
}

// maybe use something like this in the future
//
//MList : List {
//
//	var <>models;
//
//	put { arg i, item;
//		array.put(i, item);
//		models[item].lazyValue_(i);
//	}
//
//
//}


