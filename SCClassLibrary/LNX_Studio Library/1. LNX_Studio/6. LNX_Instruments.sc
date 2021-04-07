
// instruments container and organiser //////////////////////////////////

/*
   this          - dict (key==id)
   clockPriority - list (clock priorty order)
   visualOrder   - list (gui visual order (to do: should affect clockPriority and fx ordering))
   selectedInst is via id
*/

LNX_Instruments : IdentityDictionary {

	var <studio, selectedInst, <clockPriority, <visualOrder, <midiClock;

	var <mixerInstruments, <midiModules, <effects;

	*new {|studio,n=1| ^super.new(n).init(studio) } // because an IdentityDictionary

	init {|argStudio|
		studio=argStudio;
		clockPriority=[];
		midiClock=[];
		visualOrder=[];
		mixerInstruments = [];
		midiModules = [];
		effects = [];
	}

	updateTypeContainers{
		mixerInstruments = visualOrder.select(_.isMixerInstrument);
		midiModules = visualOrder.select(_.isMIDI);
		effects = visualOrder.select(_.isFX);
		mixerInstruments.do{|inst,i| inst.worldNumber_(i) };
		midiModules.do{|inst,i| inst.worldNumber_(i) };
		effects.do{|inst,i| inst.worldNumber_(i) };
	}

	// clear contents
	clear {
		this.keys.do{|key| this[key]=nil };
		clockPriority=[];
		midiClock=[];
		visualOrder=[];
		mixerInstruments = [];
		midiModules = [];
		effects = [];
		selectedInst=nil;
		this.changed(\instruments,\clear);
	}

	// add an instrument ...to the end at the moment
	// used in LNX_Studio:addInst
	addInst{|inst,id|
		this[id]=inst;
		visualOrder=visualOrder.add(inst);
		this.updateTypeContainers;
		this.updateClockPriority;
		this.changed(\instruments,\add);
	}

	// remove and return inst (supply id)
	// used in LNX_Studio:deleteInst
	removeInst{|id|
		var inst = this[id];
		if (selectedInst==id) { selectedInst=this.nextID }; // must go 1st
		this[id]=nil;
		visualOrder.remove(inst);
		this.updateTypeContainers;
		this.updateClockPriority;
		this.changed(\instruments,\removed);
		^inst;
	}

	// used in this object to decide order of clock priority

	// needed to include visual position later
	updateClockPriority{
		clockPriority=visualOrder.copy.sort{|a,b| a.clockPriority<b.clockPriority};
		midiClock = visualOrder.select{|a,b| a.hasMIDIClock };
	}

	// move inst to pos ( return true or false if moved or not )
	move{|id,pos|
		var inst = this[id];
		pos = pos.clip(0,size-1);
		if (visualOrder.indexOf(inst)!=pos) {
			visualOrder.remove(inst);
			visualOrder.insert(pos,inst);
			this.orderEffects;
			visualOrder.do({|inst,index| inst.instNo_(index) }); // change numbers
			this.updateTypeContainers;
			this.changed(\instruments,\moved);
			studio.updateAllPadMixer;          // temp for CARBON ************
			^true
		}{
			^false
		}
	}

	// get all ids
	ids{^this.keys}

	// get the visual Y index
	getY{|id| ^visualOrder.indexOf(this[id]) }

	// change the selected instrument
	selectedInst_{|id|
		if (this.includesKey(id)) {
			selectedInst=id;
		} {
			selectedInst=nil;
		};
	}

	// the currently selected instrument
	selectedInst{ ^this[selectedInst] } // rememeber we are using id's here

	// the selected instrument id
	selectedID{ ^selectedInst }

	// get the previous visual instrument id
	previousID{
		var inst;
		inst=visualOrder[ visualOrder.indexOf(this[selectedInst]) -1 ];
		if (inst.notNil) {^inst.id} {^nil}
	}

	// get the next visual instrument id
	nextID{
		var inst;
		inst=visualOrder[ visualOrder.indexOf(this[selectedInst]) +1 ];
		if (inst.notNil) {^inst.id} {^nil}
	}

	// get the previous visual instrument id (using wrap)
	previousWrapID{
		var inst;
		inst=visualOrder.wrapAt(visualOrder.indexOf(this[selectedInst]) -1);
		if (inst.notNil) {^inst.id} {^nil}
	}

	// get the next visual instrument id (using wrap)
	nextWrapID{
		var inst;
		inst=visualOrder.wrapAt(visualOrder.indexOf(this[selectedInst]) +1 );
		if (inst.notNil) {^inst.id} {^nil}
	}

	// return the instrment at visual index
	visualAt{|index| ^visualOrder[index] }

	// return effects ( list is in order of execution )
	// effects{ ^visualOrder.select(_.isFX) }

	// return everything that is not an effect ( list is in order of execution )
	notEffect{ ^visualOrder.reject(_.isFX) }

	// note to self (am i just moving everything from studio to instruemts now)
	effectSynths { ^this.effects.collect(_.synth) }

	effectNodes { ^this.effects.collect(_.node) }

	// order effects
	orderEffects{
		if (this.effects.size>1) {
			[this.effectSynths.drop(-1),this.effectSynths.drop(1)].flop.do{|nodes|
				nodes[1].moveAfter(nodes[0]);
			};
		};
	}

	mixerFXY{|id| ^this.effects.indexOf(this[id]) }

	// return all insts
	instruments{ ^visualOrder.select(_.isInstrument) }

	// for presetsOfPresets
	allInstX{|id| ^this.visualOrder.indexOf(this[id]) }

	last{^visualOrder.last}

	// return all insts that can be sequenced
	canBeSequenced { ^visualOrder.select(_.canBeSequenced) }

	// and their y pos
	canBeSequencedInstNo{ ^this.canBeSequenced.collect{|i| this.getY(i.id) } }

	// return all mixer insts
	//mixerInstruments { ^visualOrder.select(_.isMixerInstrument) }

	mixerInstY{|id| ^this.mixerInstruments.indexOf(this[id]) }

	// MIDI
	// midi{ ^visualOrder.select(_.isMIDI) }
	midi{ ^midiModules}

	midiY{|id| ^this.midi.indexOf(this[id]) }

	// 1st free fx bus in
	firstFXBus{
		^(0..15).difference(this.collect({|i|
			if (i.inChModel.notNil) {i.inChModel.value.asInt}{nil}
		}).values).first?0

	}


}
