
///////////////////////////////////////////////////////////////////////////////
//
// ************************ MIDI Pipes ****************************************
// 
///////////////////////////////////////////////////////////////////////////////

// all MIDI messages are made into subclasses of LNX_MIDIPipe, it is not used directly.
// all Pipes have a latency. Could come from many sources... MIDIIn, GUI Keyboard, Internal Seq, Net
/*

These classes define the music chords, store chords, buffer, quant & arpeggiator mods used in
Melody Maker. They use the MIDI Pipe subclasses to help make distribution of MIDI messages easier

n=LNX_NoteOn(1,2,3,4,5);
n[\a];
n.tag_(\a,1);
n.tag(\a);
n[\b]=1;
n[\b];
n.addToHistory(a);
n.history.includes(a);
n.historyIncludes(a);
LNX_NoteOff(1,2,3,4,5).isNoteOff;
LNX_Touch(1,2);
LNX_Control(1,2,3,4);
LNX_Bend(1,2,3);

*/

LNX_MIDIPipe {
	
	var	<>latency, <>source, tag, <history;
	
	kind{^nil}
	isNoteOn {^false}
	isNoteOff{^false}
	isTouch{^false}
	isControl{^false}
	isBend{^false}
	isNote{^false}
	isProgram{^false}

	// add a tag to the pipe
	tag_{|key,value|
		tag = tag ?? {IdentityDictionary[]};
		tag[key]=value;
	}
	
	tag{|key| if (tag.notNil) {^tag[key]} {^nil} }	// access the tags
	
	@{|key| if (tag.notNil) {^tag[key]} {^nil} }	// access the tags
	
	at{|key| if (tag.notNil) {^tag[key]} {^nil} }	// access the tags
	
	// add a tag to the note
	put{|key,value|
		tag = tag ?? {IdentityDictionary[]};
		tag[key]=value;
	}
	
	tags{^tag} // get all tags
	
	tags_{|tags| tag=tags} // set all tags
	
	// you can add a history to each pipe. i.e. where it has been
	// this stops feedback loops from crashing the lang. i.e MelodyMaker feedback loops
	addToHistory{|item| 
		history = history ?? {IdentitySet[]};
		history=history.add(item);
	}
	
	removeFromHistory{|item| history.remove(item) }
	
	historyIncludes{|item|
		if (history.isNil) {^false};
		^history.includes(item);
	}

}

// MIDI noteOn pipe

LNX_NoteOn : LNX_MIDIPipe {
	
	var <>note, <>velocity;
	
	kind{^\noteOn}
	isNoteOn{^true}
	isNote{^true}
	
	*new {|note, velocity, latency, source| ^super.new.init(note, velocity, latency, source) }
	
	init {|argNote, argVelocity, argLatency, argSource|
		note     = argNote;
		velocity = argVelocity;
		latency  = argLatency;
		source   = argSource;
	}
	
	// make a note off from this note on
	asNoteOff{ ^LNX_NoteOff(note, velocity, latency, source).tags_(tag.copy) }
	
	asNoteOn{ ^this }
	
	multVel{|number| velocity = (velocity*number).clip(0,127) } // scale the velocity
	
	noteAndVelocity{^[note,velocity]}
	
	printOn {|stream| stream << this.class.name << "(" << note << ", " << velocity << ", " <<
		latency << ", " << source.asCompileString << ")" }
	
}

// MIDI noteOff pipe

LNX_NoteOff : LNX_MIDIPipe {
	
	var <>note, <>velocity;
	
	kind{^\noteOff}
	isNoteOff{^true}
	isNote{^true}
	
	*new {|note, velocity, latency, source| ^super.new.init(note, velocity, latency, source) }
	
	init {|argNote, argVelocity, argLatency, argSource|
		note     = argNote;
		velocity = argVelocity;
		latency  = argLatency;
		source   = argSource;
	}
	
	asNoteOn{ ^LNX_NoteOn(note, velocity, latency, source).tags_(tag.copy) }
	
	asNoteOff{ ^this }
	
	multVel{|number| velocity = (velocity*number).clip(0,127) }
	
	noteAndVelocity{^[note,velocity]}
	
	printOn {|stream| stream << this.class.name << "(" << note << ", " << velocity << ", " <<
		latency << ", " << source.asCompileString << ")" }
	
}

// MIDI touch pipe

LNX_Touch : LNX_MIDIPipe {
	
	var <>pressure;
	
	kind{^\touch}
	isTouch{^true}
	
	*new {|pressure, latency, source| ^super.new.init(pressure, latency, source) }
	
	init {|argPressure, argLatency, argSource|
		pressure = argPressure;
		latency  = argLatency;
		source   = argSource;
	}
	
	printOn {|stream| stream << this.class.name << "(" << pressure << ", " <<
		latency << ", " << source.asCompileString << ")" }
	
}

// MIDI control pipe

LNX_Control : LNX_MIDIPipe {

	var <>num, <>val;
	
	kind{^\control}
	isControl{^true}
	
	*new {|num, val, latency, source| ^super.new.init(num, val, latency, source) }
	
	init {|argNum, argVal, argLatency, argSource|
		num      = argNum;
		val      = argVal;
		latency  = argLatency;
		source   = argSource;
	}
	
	printOn {|stream| stream << this.class.name << "(" << num << ", " << val << ", " <<
		latency << ", " << source.asCompileString << ")" }

}

// MIDI bend pipe

LNX_Bend : LNX_MIDIPipe {
	
	var <>val;
	
	kind{^\bend}
	isBend{^true}
	
	*new {|val, latency, source| ^super.new.init(val, latency, source) }
	
	init {|argVal, argLatency, argSource|
		val      = argVal;
		latency  = argLatency;
		source   = argSource;
	}
	
	printOn {|stream| stream << this.class.name << "(" << val << ", " <<
		latency << ", " << source.asCompileString << ")" }
	
}

// MIDI program pipe

LNX_Program : LNX_MIDIPipe {
	
	var <>program;
	
	kind{^\program}
	isProgram{^true}
	
	*new {|program, latency, source| ^super.new.init(program, latency, source) }
	
	init {|argProgram, argLatency, argSource|
		program  = argProgram;
		latency  = argLatency;
		source   = argSource;
	}
	
	printOn {|stream| stream << this.class.name << "(" << program << ", " <<
		latency << ", " << source.asCompileString << ")" }
	
}


// same as MIDIEndPoint but device & name are symbols. enables fast test with identiy.

LNX_MIDIEndPoint {
	
	var <endPoint, <device, <name, <uid;
	
	*new{|endPoint| ^super.new.init(endPoint) }
	
	init{|argEndPoint|
		endPoint = argEndPoint;
		device   = endPoint.device.asSymbol;
		name     = endPoint.name.asSymbol;
		uid      = endPoint.uid;
	}
	
	printOn { arg stream;
		stream << this.class.name << "(" <<<*
			[device, name]  <<")"
	}
	
	isSameDevice{|endPoint| ^(endPoint.device === device) }
	isSameName{|endPoint| ^(endPoint.name === name) }
	isSameDeviceAndName{|endPoint| ^((endPoint.device === device) && (endPoint.name === name)) } 
	isSameEndPoint{|endPoint| endPoint.uid == uid }
	
}

///////////////////////////////////////////////////////////////////////////////
//
// ************************ Buffer ********************************************
// 
///////////////////////////////////////////////////////////////////////////////

// used to manage >1 MIDI Pipe sources. (2 modes allPass = true or false)
// allPass = true, lets 1 source override another and force a NoteOff
// allPass = false, stops 1 source from overriding another
// both stop multiple noteON events from all sources
// keeps track of which noteON events are on and then releases them when asked

LNX_MIDIBuffer{
	
	var <notesOn, <>midiPipeOutFunc, <models, <>allPass=false;
	
	*new { ^super.new.init }
	
	init {
		notesOn = IdentityDictionary[];
		models  = IdentityDictionary[];
	}
	
	model_{|key,model| models[key]=model }
	
	pipeIn{|pipe|	
		switch (pipe.kind)
			{\noteOn} { // noteOn
				
				if ((pipe.note>127)||(pipe.note<0)) {^this}; // drop if out of range
				
				if (models[\velocity].notNil) {
					pipe=pipe	.copy.multVel(models[\velocity].value);
				};
				
				if (notesOn[pipe.note].isNil) {
					notesOn[pipe.note]=pipe;
					midiPipeOutFunc.value(pipe);
				}{						
					if (allPass) {
						midiPipeOutFunc.value(notesOn[pipe.note].asNoteOff); // stop other
						notesOn[pipe.note]=pipe;
						midiPipeOutFunc.value(pipe);
					};
				};
			}
			{\noteOff} { // noteOff
				
				if ((pipe.note>127)||(pipe.note<0)) {^this}; // drop if out of range
				
				if (models[\velocity].notNil) {
					pipe=pipe	.copy.multVel(models[\velocity].value);
				};
				
				if ((notesOn[pipe.note].notNil)and:{notesOn[pipe.note].source==pipe.source}) {
					notesOn[pipe.note]=nil;
					midiPipeOutFunc.value(pipe);
				};
			}{
				midiPipeOutFunc.value(pipe);
			};	
	}
	
	// release everything
	releaseAll{
		notesOn.do{|pipe| midiPipeOutFunc.value(pipe.asNoteOff) };
		notesOn.clear;
	}
	
	// release all from a source
	releaseSource{|source|
		notesOn.pairsDo{|note,pipe|
			if (pipe.source==source) {
				midiPipeOutFunc.value(pipe.asNoteOff);
				notesOn[\note]=nil;
			};
		};
	}
	
	// release everything below a note
	releaseBelow{|low|
		notesOn.pairsDo{|note,pipe|
			if (pipe.note<low) {
				midiPipeOutFunc.value(pipe.asNoteOff);
				notesOn[\note]=nil;
			};
		};
	}
	
	// release everything above a note
	releaseAbove{|high|
		notesOn.pairsDo{|note,pipe|
			if (pipe.note>high) {
				midiPipeOutFunc.value(pipe.asNoteOff);
				notesOn[\note]=nil;
			};
		};
	}
	
}

///////////////////////////////////////////////////////////////////////////////
//
// ************************ Music *********************************************
// 
///////////////////////////////////////////////////////////////////////////////

LNX_Music {
	
	classvar <musicChords, <chordNames, <myGroups, <myGroupNames, <groups, <names;
	
	var <notesOn, <chordsOn, <>midiPipeOutFunc, <activeMusicChords, <activeChordNames;
	var <models, <>storeChordsMod;
	
	*initClass {
		
		// musical chords
		// [ difference, velocities, name]
		
		musicChords = #[
			[ [ 0, 4, 7 ], [ 1, 1, 1 ], "Major" ],
			[ [ 0, 3, 7 ], [ 1, 1, 1 ], "Minor" ],
			[ [ 0, 3, 6 ], [ 1, 1, 1 ], "Dim" ],
			[ [ 0, 4, 8 ], [ 1, 1, 1 ], "Aug" ],
			[ [ 0, 4, 6 ], [ 1, 1, 1 ], "Major b5" ],
			[ [ 0, 2, 7 ], [ 1, 1, 1 ], "Sus 2" ],
			[ [ 0, 5, 7 ], [ 1, 1, 1 ], "Sus 4" ],
			[ [ 0, 7 ], [ 1, 1 ], "5" ],
			[ [ 0, 4, 7, 14 ], [ 1, 1, 1, 1 ], "Add 9" ],
			[ [ 0, 3, 7, 14 ], [ 1, 1, 1, 1 ], "Minor add 9" ],
			[ [ 0, 4, 7, 9 ], [ 1, 1, 1, 1 ], "Major 6" ],
			[ [ 0, 3, 7, 9 ], [ 1, 1, 1, 1 ], "Minor 6" ],
			[ [ 0, 5, 7, 9 ], [ 1, 1, 1, 1 ], "6 sus4" ],
			[ [ 0, 4, 7, 9, 14 ], [ 1, 1, 1, 1, 1 ], "6 add 9" ],
			[ [ 0, 3, 7, 9, 14 ], [ 1, 1, 1, 1, 1 ], "Minor 6 add 9" ],
			[ [ 0, 4, 7, 10 ], [ 1, 1, 1, 1 ], "7" ],
			[ [ 0, 5, 7, 10 ], [ 1, 1, 1, 1 ], "7 sus4" ],
			[ [ 0, 3, 6, 9 ], [ 1, 1, 1, 1 ], "Dim 7" ],
			[ [ 0, 4, 8, 10 ], [ 1, 1, 1, 1 ], "Aug 7" ],
			[ [ 0, 4, 6, 10 ], [ 1, 1, 1, 1 ], "7 b5" ],
			[ [ 0, 4, 7, 11 ], [ 1, 1, 1, 1 ], "Major 7" ],
			[ [ 0, 5, 7, 11 ], [ 1, 1, 1, 1 ], "Major 7 sus4" ],
			[ [ 0, 3, 7, 10 ], [ 1, 1, 1, 1 ], "Minor 7" ],
			[ [ 0, 3, 6, 10 ], [ 1, 1, 1, 1 ], "Minor 7 b5" ],
			[ [ 0, 3, 7, 11 ], [ 1, 1, 1, 1 ], "Minor Major 7" ],
			[ [ 0, 4, 7, 10, 14 ], [ 1, 1, 0.5, 1, 1 ], "9" ],
			[ [ 0, 5, 7, 10, 14 ], [ 1, 1, 0.5, 1, 1 ], "9 sus4" ],
			[ [ 0, 4, 7, 11, 14 ], [ 1, 1, 0.5, 1, 1 ], "Major 9" ],
			[ [ 0, 5, 7, 11, 14 ], [ 1, 1, 0.5, 1, 1 ], "Major 9 sus4" ],
			[ [ 0, 3, 7, 10, 14 ], [ 1, 1, 0.5, 1, 1 ], "Minor 9" ],
			[ [ 0, 3, 7, 11, 14 ], [ 1, 1, 0.5, 1, 1 ], "Minor Major 9" ],
			[ [ 0, 4, 7, 10, 14, 17 ], [ 1, 0.5, 1, 1, 0.5, 1 ], "11" ],
			[ [ 0, 4, 7, 11, 14, 17 ], [ 1, 0.5, 1, 1, 0.5, 1 ], "Major 11" ],
			[ [ 0, 3, 7, 10, 14, 17 ], [ 1, 1, 0.5, 1, 0.5, 1 ], "Minor 11" ],
			[ [ 0, 4, 7, 10, 14, 17, 21 ], [ 1, 0.5, 1, 1, 0.5, 0.5, 1 ], "13" ],
			[ [ 0, 4, 7, 11, 14, 17, 21 ], [ 1, 0.5, 1, 1, 0.5, 0.5, 1 ], "Major 13" ],
			[ [ 0, 3, 7, 10, 14, 17, 21 ], [ 1, 1, 0.5, 1, 0.5, 0.5, 1 ], "Minor 13" ]
		];
		
		chordNames = musicChords.collect(_[2]); // get the names for easy access
			
		// make the different size groups (3,4,5,6,7)
		myGroups = [	
			(0..7),
			(0..12)++(15..24),
			(0..30),
			(0..33),
			(0..36)
		];	
		myGroupNames = [ "3", "4", "5", "6","7" ];	
		groups = myGroups.collect{|g| g.collect{|c| musicChords[c] } };
		names = groups.collect{|g| g.collect(_[2])};
		
	}

	*new { ^super.new.init }
	
	init {
		models   = IdentityDictionary[];
		notesOn  = IdentityDictionary[];
		chordsOn = IdentityDictionary[];
		
		activeMusicChords = groups[1];
		activeChordNames = names[1];
	}
	
	model_{|key,model| models[key]=model } // attach models
	
	setActiveGroup_{|n|
		activeMusicChords = groups[n];
		activeChordNames = names[n];
	}
	
	size{^activeMusicChords.size}
	
	// midi in
	pipeIn{|pipe|	
		
		var chord, vel, note;
	
		switch (pipe.kind)
		
			{\noteOn} { // noteOn
				
				if (pipe.note==storeChordsMod.storeKey) {
					storeChordsMod.guiAddChord; // skip if storeChord key and add to store
					^nil
				}; 
				
				if ((models[\onOff].value.isTrue) and: { storeChordsMod.chordAt
										(pipe.note).isNil}) { // and key is not stored
						
					chord = this.transposeChord(
						activeMusicChords.clipAt(models[\chord].value)[0]); // transpose
					vel   = this.transposeVelocity(
						activeMusicChords.clipAt(models[\chord].value)[1]); // transpose
					
					note = pipe.note;
					notesOn[note]  = pipe;
					chordsOn[note] = chord;
					
					chord.do{|i,j|
						midiPipeOutFunc.value(
							LNX_NoteOn(note+i, pipe.velocity*vel[j], pipe.latency, \music)
						);	
					};
				}{
					midiPipeOutFunc.value(pipe); // not on so just pipe out
				};
			}
			{\noteOff} { // noteOff
			
				if (pipe.note==storeChordsMod.storeKey) {^nil }; 
			
				note = pipe.note;
				
				if (chordsOn[note].notNil) {
					
					chordsOn[note].do{|i,j|
						midiPipeOutFunc.value(
							LNX_NoteOff(note+i, notesOn[note].velocity, pipe.latency, \music)
						);	
					};
					
					notesOn[note]  = nil;
					chordsOn[note] = nil;
								
				}{	
					midiPipeOutFunc.value(pipe); // not on so just pipe out
				};
			}{
				midiPipeOutFunc.value(pipe)
			};

	}
	
	transposeChord{|chord|	
		var tempChord = chord.copy;
		var amount = models[\transpose].value;
		var spo = models[\spo].value;
		
		if (amount>=0) { 
			amount.abs.do({|i| tempChord.wrapPut(i, tempChord.wrapAt(i)+spo) });
		}{
			amount.abs.do({|i| tempChord.wrapPut(i.neg+(tempChord.size- 1),
						tempChord.wrapAt(i.neg+(tempChord.size- 1)) - spo) });
		};
		^tempChord
	}

	transposeVelocity{|chord|	
		var tempChord = chord.copy;
		var amount = models[\transpose].value;
		
		if (amount>=0) { 
			amount.abs.do({|i| tempChord.wrapPut(i, tempChord.wrapAt(i)) });
		}{
			amount.abs.do({|i| tempChord.wrapPut(i.neg+(tempChord.size- 1),
						tempChord.wrapAt(i.neg+(tempChord.size- 1))) });
		};
		^tempChord
	}
	
	updateChords{|latency|
		var note, chord, vel;
		
		if (models[\onOff].value.isTrue) {
				
			// stop all
			notesOn.do{|pipe,j|
				note=pipe.note;
				chordsOn[note].do{|i,j|
					midiPipeOutFunc.value(
						LNX_NoteOff(note+i, pipe.velocity, latency, \music)
					);	
				};
				chordsOn[note]=nil;	
			};
			
			// play all
			notesOn.do{|pipe,j|
				chord = this.transposeChord(
					activeMusicChords.clipAt(models[\chord].value)[0]);
				vel   = this.transposeVelocity(
					activeMusicChords.clipAt(models[\chord].value)[1]);
				note = pipe.note;
				chordsOn[note] = chord;
				chord.do{|i,j|
					midiPipeOutFunc.value(
						LNX_NoteOn(note+i, pipe.velocity*vel[j], latency, \music)
					);	
				};			
			};
		}
		
	}
	
	releaseAll{ } // will need with missing data (DO at end, all mods)
				// using a MIDIBuffer now for easy of use
}

///////////////////////////////////////////////////////////////////////////////
//
// ************************ StoreChords ***************************************
// 
///////////////////////////////////////////////////////////////////////////////

LNX_StoreChords {
	 
	classvar	<musicChords, <musicNames, clipboard;
	
	var <presets, api;
	var <chords, <pipes, <notesOn, <chordsOn, <>midiPipeOutFunc;
	var <models, <pressed=false, offset=24, <store=9999; // i'm turning store key off for the mo
	var <chordNames;
	var <>chordOnFunc, <>chordOffFunc;

	*initClass {
		Class.initClassTree(LNX_Music);
		musicChords = LNX_Music.musicChords.collect(_.at(0)); // copy from LNX_Music 
		musicNames  = LNX_Music.musicChords.collect(_.at(2)); // copy from LNX_Music
		clipboard   = [];
	}

	*new { ^super.new.init }
		
	init {
		presets  = [];
		chords   = []; 					// storage as ints
		pipes    = []; 					// storage as pipes
		models   = IdentityDictionary[];
		notesOn  = IdentityDictionary[];
		chordsOn = IdentityDictionary[];
		chordNames = [];
	}
	
	offset{^models[\root].value}
	
	apiID_{|id| api = LNX_API(this,id,#[\netAddChord, \netDeleteChord, \netPasteChords,
			\netDeleteAllChords]) }
	
	// which note is been used as a "store this chord" key
	storeKey{ if (models[\record].value.isTrue) {^store} {^nil} }
	
	model_{|key,model| models[key]=model }
	
	chordAt{|note| ^chords[note-(this.offset)]}
	
	pipeAt{|note| ^pipes[note-(this.offset)]}
	
	pipeIn{|pipe|	
		
		var chord, note;
		
		var offset = this.offset; 
		
		switch (pipe.kind)
		
			{\noteOn} { // noteOn
				note = pipe.note;      				// get the note
				notesOn[note] = pipe;  				// store the pipe under the key note
				if ((models[\play].value.isTrue) and:{pipe.source!=\music}) {
												// if play and not from the music mod
					chord = pipes[note-offset];		// look up the chord @ key pos
					if (chord.notNil) {			// if a chord exists there
						chordsOn[note] = chord;		// store it
						chord.do{|chordPipe|		// play each pipe from that chord
							midiPipeOutFunc.value(
								LNX_NoteOn(
									chordPipe.note,
									chordPipe.velocity / 127 * pipe.velocity, // apply vel
									pipe.latency,
									\chords
								)
							);	
						};
						chordOnFunc.value(note-offset,note);
					}{
						midiPipeOutFunc.value(pipe); // if no chord just forward pipe
					}
				}{
					midiPipeOutFunc.value(pipe);	// else just forward pipe
				};
			}
		
			{\noteOff} { // noteOff
				note = pipe.note;
				notesOn[note] = nil;
				if (chordsOn[note].notNil) {
					chordsOn[note].do{|chordPipe|
						midiPipeOutFunc.value(
							LNX_NoteOff(
								chordPipe.note,
								chordPipe.velocity / 127 * pipe.velocity,
								pipe.latency,
								\chords
							)
						);	
					};
					chordsOn[note]=nil;	
					chordOffFunc.value(note-offset,note);	
				}{
					midiPipeOutFunc.value(pipe);
				};	
				if (chordsOn.size==0) { pressed=false };
			}
			
			{\touch} { // touch
				if ((pressed.not)&&(pipe.pressure>95)) {
					pressed=true;
					if (models[\record].value.isTrue) {
						this.guiAddChord;
					};
				};
				midiPipeOutFunc.value(pipe);
			}
				// else
			{
				midiPipeOutFunc.value(pipe)
			};
		
	}
	
	releaseAll{ }
	
	// the gui call for adding a chord
	
	guiAddChord{
		var normaliseVelocityMultiplier, chord, list;
		
		if (notesOn.notEmpty) {
			// work out a multiplier that will normilise velocity to 127
			normaliseVelocityMultiplier=127/notesOn.values.collect(_.velocity).sort.last;
			if (normaliseVelocityMultiplier==inf) {normaliseVelocityMultiplier=0};
			// also get the chord as a list of pipes
			chord = notesOn.values.collect(_.copy).sort{|a,b| a.note<=b.note};
			// adjust the pipes
			chord.do{|p|
				p.multVel(normaliseVelocityMultiplier)
					.source_(\chords)
					.latency_(\nil)
			};
			list = chord.collect(_.noteAndVelocity).flat;
			api.groupCmdOD(\netAddChord,*list); // send
		}
	
	}
	
	// the network call for adding a call
	netAddChord{|...list|
		var chord, pipesToAdd = list.clump(2).collect{|l| LNX_NoteOn(l[0],l[1],nil,\chords)};
		chords = chords.add(pipesToAdd.collect(_.note)); // store as a list of ordered numbers
		chord = pipesToAdd.sort{|a,b| a.note<=b.note};
		pipes = pipes.add(pipesToAdd); // and store the pipes
		chordNames=chordNames.add(this.getChordName(chord)); // add this name to list
		this.changed(\chords,chords,chordNames);
	}
	
	updateKeyboard{ this.changed(\chords,chords,chordNames) }
	
	// update the chord list for Quant mod & update the names with the current set-up
	// used by load, loadPrest & paste chords
	updateChordsAndNames{
		chords=[];
		chordNames=[];
		pipes.do{|chord|
			chords=chords.add(chord.collect(_.note));
			chordNames=chordNames.add(this.getChordName(chord));// add this name to list
		};
		this.changed(\chords,chords,chordNames);	
	}
	
	// work out the name of this chord
	getChordName{|chord|
		var nameIndex, chordList, chordStart;
		var spo=models[\spo].value ? 12;
		if (spo==12) {
			chordList=chord.collect(_.note);    // collect the notes
			chordStart=chordList.first;         // get the lowest note
			chordList = chordList.copy - chordStart; // subract this from the list
			nameIndex = musicChords.indexOfList(chordList); // find if this exists
			if (nameIndex.notNil) {
				// if it does exist find and make the name
				^chordStart.asNote2++" "++musicNames[nameIndex];
			}{	
				// else just make the name from the notes
				^chord.collect{|pipe| pipe.note.asNote2++" "}.join.drop(-1);
			};	
		}{
			^chord.collect{|pipe| pipe.note.asNote4(spo)++" "}.join.drop(-1);
		}		
	}
	
	// NETWORK ALL THIS !!!!
	
	// delete the last chord added (I dont think this is been used)
	deleteLastChord{
		chords = chords.drop(-1);
		pipes = pipes.drop(-1);
		chordNames = chordNames.drop(-1);
		this.changed(\chords,chords,chordNames);
	}

	// gui of delete chord
	guiDeleteChord{|index| api.groupCmdOD(\netDeleteChord,index) } // send

	// net remove chord at index
	netDeleteChord{|index|
		if ((index.isNumber)and:{index<chords.size}) {
			index=index.asInt;
			chords.removeAt(index);
			pipes.removeAt(index);
			chordNames.removeAt(index);
			this.changed(\chords,chords,chordNames);
		}
	}
	
	// gui of delete all chords
	guiAllDeleteChords{  api.groupCmdOD(\netDeleteAllChords) } // send
	
	// net of delete all chords
	netDeleteAllChords{
		chords     = []; 					// storage as ints
		pipes      = []; 					// storage as pipes
		chordNames = [];
		this.changed(\chords,chords,chordNames);
	}
	
	// gui copy the chords to the clipboard
	guiCopyChords{ if (pipes.notEmpty) { clipboard = pipes.deepCopy } }	
	// gui paste the chords to the clipboard
	guiPasteChords{
		if (clipboard.notEmpty) {
			api.groupCmdOD(\netPasteChords,
				*[ clipboard.size,
					clipboard.collect{|c|  [c.size] ++
						(c.collect(_.noteAndVelocity)) }.flat
				].flat;
			)
		}		
	}
	
	// net paste the chords from the clipboard (partly putLoadList)
	netPasteChords{|...l|
		var size,pipesToAdd;
		l=l.reverse;
		size = l.popI;
		
		// current set-up 
		size.do{
			var size2, pipesToAdd2=[];
			size2=l.popI;
			size2.do{
				pipesToAdd2=pipesToAdd2.add(LNX_NoteOn(l.popI,l.popI,nil,\chords));
			};
			pipesToAdd=pipesToAdd.add(pipesToAdd2);
		};
		pipes=pipesToAdd;
		
		this.updateChordsAndNames;
	}
		
	// Presets ////////////////////////////////////////
	
	// get the current state as a list
	iGetPresetList{
		^["LNX_StoreChords DOC", pipes.size]
			++pipes.collect{|c|
				[c.size]++(c.collect(_.noteAndVelocity).flat)
			}.flat
			++["*** END LNX_StoreChords DOC ***"];
	}
	
	// add a statelist to the presets
	iAddPresetList{|l|
		var size, pipesToAdd=[];		
		l=l.reverse;		
		l.pop; // get rid of header	
		
		size=l.popI;
		size.do{
			var size2, pipesToAdd2=[];
			size2=l.popI;
			size2.do{
				pipesToAdd2=pipesToAdd2.add(LNX_NoteOn(l.popI,l.popI,nil,\chords));
			};
			pipesToAdd=pipesToAdd.add(pipesToAdd2);
			
		};
		presets=presets.add(pipesToAdd);		
	}
	
	// save a state list over a current preset
	iSavePresetList{|i,l|
		var size, pipesToAdd=[];		
		l=l.reverse;		
		l.pop; // get rid of header	
		
		size=l.popI;
		size.do{
			var size2, pipesToAdd2=[];
			size2=l.popI;
			size2.do{
				pipesToAdd2=pipesToAdd2.add(LNX_NoteOn(l.popI,l.popI,nil,\chords));
			};
			pipesToAdd=pipesToAdd.add(pipesToAdd2);
		};
		if (presets[i].notNil) { presets[i]=pipesToAdd };
	}

	// for your own load preset
	iLoadPreset{|i|
		pipes = presets[i].deepCopy;
		chordNames=[];
		chords=[];
		this.updateChordsAndNames;		
	}
	
	// for your own remove preset
	iRemovePreset{|i| presets.removeAt(i) }
	
	// for your own removal of all presets
	iRemoveAllPresets{ presets=[] }
		
	// save list
	getSaveList{
	
		// header
		^["LNX_StoreChords DOC", pipes.size, presets.size]
		
		// current set-up 
		++pipes.collect{|c|
			[c.size]++(c.collect(_.noteAndVelocity).flat)
		}.flat
		
		// each preset
		++presets.collect{|preset|
			[preset.size]++
				preset.collect{|c|
					[c.size]++(c.collect(_.noteAndVelocity).flat)
				}.flat
		}.flat
		
		// footer
		++["*** END LNX_StoreChords DOC ***"];
				
	}
	
	// put load list
	putLoadList{|l|
		
		var size, presetSize, pipesToAdd=[];
		
		pipes=[];
		presets=[];
		
		// header
		l=l.reverse;
		l.pop; // get rid of header	
		size = l.popI;
		presetSize = l.popI; 
		
		// current set-up 
		size.do{
			var size2, pipesToAdd2=[];
			size2=l.popI;
			size2.do{
				pipesToAdd2=pipesToAdd2.add(LNX_NoteOn(l.popI,l.popI,nil,\chords));
			};
			pipesToAdd=pipesToAdd.add(pipesToAdd2);
		};
		pipes=pipesToAdd;
		this.updateChordsAndNames;
		
		// all presets
		presetSize.do{
			// for each preset
			size = l.popI;
			pipesToAdd=[];
			// for each chord
			size.do{
				var size2, pipesToAdd2=[];
				size2=l.popI;
				// for each pipe
				size2.do{
					pipesToAdd2=pipesToAdd2.add(LNX_NoteOn(l.popI,l.popI,nil,\chords));
				};
				pipesToAdd=pipesToAdd.add(pipesToAdd2);
			};
			presets=presets.add(pipesToAdd);		
		};
		
	}
	
}

///////////////////////////////////////////////////////////////////////////////
//
// ************************ ChordQuantiser ************************************
// 
///////////////////////////////////////////////////////////////////////////////

// use \quant
// recommend to use a buffer afterwards
// midiBuffer3 is actually used to release notes not this mod

LNX_ChordQuantiser{
	
	classvar	<musicChords, <musicNames;
		
	var <>midiPipeOutFunc, <models, <chords, <notesOn, <chordNames;

	*initClass {
		Class.initClassTree(LNX_Music);
		musicChords=LNX_Music.musicChords.collect(_.at(0)); // copy from LNX_Music 
		musicNames=LNX_Music.musicChords.collect(_.at(2)); // copy from LNX_Music
	}

	*new { ^super.new.init }
	
	init {
		models = IdentityDictionary[];
		notesOn = IdentityDictionary[];
		chords = [];
		chordNames = [];
	}
	
	model_{|key,model| models[key]=model }
	
	pipeIn{|pipe|
		var note;
		var spo=models[\spo].value;
			
		if ((pipe.kind==\noteOn)or:{pipe.kind==\noteOff}) {
				// note on or off
				
				if (pipe.kind==\noteOn) { notesOn[pipe.note]=pipe}; // store original
				if (pipe.kind==\noteOff) { notesOn[pipe.note]=nil};
								
				if (models[\onOff].value.isTrue) {
					// change the note	
					var chord = chords[models[\chord].value], newnote;
					if(chord.notNil) {		
						note=pipe.note;
						//findNearest with offset
						newnote = chord.findNearest(note%spo,models[\transpose].value );
						pipe = pipe.copy
								.note_((note/spo).asInt*spo+newnote)
								.source_((\quant++note).asSymbol);// new pipe
						pipe.tag_(\quant,note != pipe.note); // used for lamp 
					};	
				};
				midiPipeOutFunc.value(pipe);
			}{
				midiPipeOutFunc.value(pipe);
			};	
	}
	
	// update the name + quantise list for pipeIn
	update {|object, model, arg1,arg2|	
		var spo;
		spo=models[\spo].value;
		if (model==\chords) {
			chordNames=[];
			chords=arg1.collect{|c|
				var chord = c.asArray.mod(spo).asSet.asArray.sort;
				var range;
				
				chordNames = chordNames.add(this.getChordName(c));
				
				range = (120/spo).asInt;
				
				//[chord.last-12] ++ chord ++ [chord.first+12]; // extend range above & below
				((range.neg)..range).collect{|i| (i*spo)+chord}.flat; //now later for transpose
			};
		};
		{
		models[\chord].controlSpec_([0,(chords.size-1).clip(0,inf),\linear,1])
			.themeMethods_((items_:chordNames));
		}.defer;	
	}
	
	// work out the name of this chord
	getChordName{|chord|
		var nameIndex, chordStart, chordList;
		var spo = models[\spo].value;
		
		if (spo==12) {
			
			chordStart = chord.first;         // get the lowest note
			chordList  = chord.copy - chordStart; // subract this from the list
			
			nameIndex  = musicChords.indexOfList(chordList); // find if this exists
			if (nameIndex.notNil) {
				// if it does exist find and make the name
				^chordStart.asNote3++" "++musicNames[nameIndex];
			}{	
				// else just make the name from the notes
				^chord.collect{|note| note.asNote3++" "}.join.drop(-1);
			};	
		
		}{
			// else just make the name from the notes
			^chord.collect{|note| note.asNote4(spo).first++" "}.join.drop(-1);
		}
					
	}

	// will need with missing data (DO at end, all mods)
	releaseAll{
		notesOn = IdentityDictionary[];
	}
	
	retrigger{
		notesOn.do{|pipe| this.pipeIn(pipe) }
	}
	
	guiOnOff{
		if (models[\retrigger].value.isTrue) {
			if (models[\onOff].value.isTrue) {
				this.retrigger;
			};
		}{
			this.releaseAll;
		}
	}

}

///////////////////////////////////////////////////////////////////////////////
//
// ************************ Arpeggiator ***************************************
// 
///////////////////////////////////////////////////////////////////////////////

LNX_Arpeggiator{
			
	var <transpose=0, <notesOn, <chordsOn, <>midiPipeOutFunc, <sortedNotes;
	var <lastIndex=0, legIndex=0;
	var <models; // \onOff, \style, \beats, \duration

	*new { ^super.new.init }
	
	init {
		models      = IdentityDictionary[];
		notesOn     = IdentityDictionary[];
		chordsOn    = IdentityDictionary[];
		sortedNotes = [];
	}
	
	model_{|key,model| models[key]=model }
	
	pipeIn{|pipe|	
		switch (pipe.kind)
			{\noteOn} { // noteOn
				notesOn[pipe.note] = pipe;
				this.sortNotes;
				if (models[\onOff].value.isFalse) {
					midiPipeOutFunc.value(pipe);
				}
			}
			{\noteOff} { // noteOff
				notesOn[pipe.note] = nil;
				this.sortNotes;
				if (models[\onOff].value.isFalse) {
					midiPipeOutFunc.value(pipe);
				}
			}{
				midiPipeOutFunc.value(pipe);
			};		
	}
	
	// nb. overlapping notes in chords when released make the other chord short
	// if arpeg extended this can result in higher or lower than epected extensions
	
	sortNotes{
		// get notesOn and sort it
		
		var spo = models[\spo].value;
		
		sortedNotes=notesOn.asList.sort{|a,b| a.note<=b.note };
		
		if (sortedNotes.size>0) {
			
			sortedNotes = // extend range both up & down
			((models[\down].value)..(sortedNotes.size-1+(models[\up].value))).collect{|i|
				var pipe = sortedNotes.wrapAt(i); 
				LNX_NoteOn(
					pipe.note+(i.div(sortedNotes.size)*spo),
					pipe.velocity,
					pipe.latency,
					pipe.source
				)
			};
			// set next beat as start of legeto
			if (legIndex.isNil) {legIndex=lastIndex+1};
		}{
			// reset legeto
			legIndex=nil;
		};
			
	}
	
	releaseAll{ } // notes have already been sched for NoteOff
	
	bang{|velocity,latency,beat,beatNo,absTime,dur| this.clockIn2(beatNo,latency,absTime,dur) }
	
	reset{ lastIndex=0; legIndex=0; }
	
	clockIn2{|index,latency, absTime,dur|
		
		var pipe, beat, size, polyPipes;
		
		index = index.asInt; // to make sure we wrap using integers & not floats
		
		// legato
		if (models[\legato].value.isTrue) {
			beat = (index-(legIndex?0)).div(models[\repeat].value);
		}{
			beat = index.div(models[\repeat].value);
		};
		lastIndex = index;
		
		if ((models[\onOff].value.isTrue) and: { sortedNotes.size>0 }) {

			size = sortedNotes.size;

			switch (models[\style].value.asInt)
				// up
				{0} { beat = beat%size }
				// down
				{1} { beat = (-1-beat)%size }
				// updown
				{2} { beat = beat.fold(0,size-1) } // fold @ size-1 because of integers
				// downup
				{3} { beat = (0-beat).fold(0,size-1) }
				
				// Converge
				{4} {
					var size2 = (size/2).floor.asInt.clip(1,inf).asInt;
					beat=beat.wrap(0,(size-1));
					if (beat.even) { // even up
						beat = beat.div(2);
						beat = beat.wrap(0,size2);
						beat = beat % size;
					}{ // odd down
						beat = beat.div(2);
						beat = beat.wrap(0,size2);
						beat = (-1-beat) % size;
					};
				}
				
				// Diverge
				{5} {
					var size2 = (size/2).floor.asInt.clip(1,inf).asInt;
					beat=(-1-beat).wrap(0,(size-1));
					if (beat.even) { // even up
						beat = beat.div(2);
						beat = beat.wrap(0,size2);
						beat = beat % size;
					}{ // odd down
						beat = beat.div(2);
						beat = beat.wrap(0,size2);
						beat = (-1-beat) % size;
					};	
				}
				
				// Con & Div
				{6} { 
					if (beat.even) { // even up
						beat = beat.div(2);
						beat = beat % size;
					}{ // odd down
						beat = beat.div(2);
						beat = (-1-beat) % size;
					};	
				}
				
				// UpUpDown
				{7} {
					beat = beat-(beat.div(3)*2);
					beat = beat%size;
				}

				// DownDownUp
				{8} {
					beat=(-1-4-beat);
					beat = beat-(beat.div(3)*2);
					beat = beat%size;
				}
				
				// random
				{9} { beat = (beat.hash)%(sortedNotes.size) };
				
			// make a poly set of pipe to play in a chord
			polyPipes = Set[];	
			models[\poly].value.do{|i|
				polyPipes=polyPipes.add(sortedNotes.wrapAt(beat+i));
			};	
			
			polyPipes.do{|pipe|	
				// noteOn
				midiPipeOutFunc.value( LNX_NoteOn(pipe.note, pipe.velocity, latency, \arpeg) );
				// noteOff	
				if (models[\ratio].value.isTrue) {	
					{
						midiPipeOutFunc.value(
							LNX_NoteOff(pipe.note, pipe.velocity, latency, \arpeg)
						);
						nil;
					}.sched(dur*absTime/8*(models[\duration].value)/100); // ratio
				}{
					{
						midiPipeOutFunc.value(
							LNX_NoteOff(pipe.note, pipe.velocity, latency, \arpeg)
						);
						nil;
					}.sched(
						(4*absTime/8*(models[\duration].value)/100)
							.clip(0,dur*absTime/8*(models[\duration].value)/100*0.99)
					); // fixed
				};
				
			}
			
		}
		
	}
	
}

///////////////////////////////////////////////////////////////////////////////
//
// ************************ MultiPipeOut **************************************
// 
///////////////////////////////////////////////////////////////////////////////

LNX_MultiPipeOut{
	
	var studio, parent, <insts, <names, <ids, <instNo, <onOffs, <midiBuffers, <gui, api;
	var <>midiOutBuffer;
	var w, h, fontHeight, rect, <midiOut=true;
	var <presets, <midiOutPresets, <postLoadList;
	var <models;
	
	var rotateIndex=0, rotateNoteDict;
	
	*new {|studio,parent| ^super.new.init(studio,parent) }
	
	init{|argStudio, argParent|
		studio = argStudio; 
		parent = argParent; // what is this used for
		models = IdentityDictionary[];
		insts  = [];
		names  = [];
		ids    = [];
		instNo = [];
		onOffs      = IdentityDictionary[];
		midiBuffers = IdentityDictionary[];
		gui         = IdentityDictionary[];
		presets        = []; // for onOffs
		midiOutPresets = []; // for midiOut OnOff
		studio.addDependant(this);
		studio.insts.addDependant(this);
		
		rotateNoteDict = IdentityDictionary[];
		
	}
	
	apiID_{|id| api = LNX_API(this,id,#[\netOnOffID_, \netMIDIOut_]) }
	
	model_{|key,model| models[key]=model }
	
	reset{ rotateIndex=0 }
	
	free{
		models = nil;
		studio.removeDependant(this);
		studio.insts.removeDependant(this);	
	}
	
	pipeIn{|pipe|
		if (models[\rotate].value.isTrue) {
			// rotate outputs 
			if ((pipe.isNoteOn) || (pipe.isNoteOff)) {         // (only sends note events)
				var idsOn;
				var id, note = pipe.note;
				if (pipe.isNoteOn) { 				         // if this is a note on event
					idsOn = midiOut.if([\midiOut],[]) ++ 
								ids.select{|id| onOffs[id]==1 }; // get all insts that are on
					id = idsOn.wrapAt(rotateIndex);          // select an out by wrapping Index
					if (id.notNil) {
						rotateNoteDict[note] = id;          // store id in the noteDict
						if (id==\midiOut) {
							midiOutBuffer.pipeIn(pipe.addToHistory(parent)); // send to midiout
						}{
							midiBuffers[id].pipeIn(pipe.addToHistory(parent)); // send it inst
						};
						rotateIndex = rotateIndex+1;        // add 1 to the rotation index
					};
				}{
					// else its a note off event
					id = rotateNoteDict[note];               // look up id in the noteDict    
					if (id.notNil) {                         // if present
						if (id==\midiOut) {
							midiOutBuffer.pipeIn(pipe.addToHistory(parent)); // send to midiout
						}{
							midiBuffers[id].pipeIn(pipe.addToHistory(parent)); // send to inst
						};
						rotateNoteDict[note] = nil;         // remove this from the noteDict
					}
				};
			};
		}{
			// out to all outputs
			if (midiOut) { midiOutBuffer.pipeIn(pipe) };
			midiBuffers.pairsDo{|id,buffer|
				if (onOffs[id]==1) { buffer.pipeIn(pipe.addToHistory(parent)) }
			};	
		};
	}
	
	onOff_{|index,value| this.onOffID_(ids[index],value) }

	onOffID_{|id,value|
		this.netOnOffID_(id,value);
		api.sendVP(id,\netOnOffID_,id,value);
	}
	
	netOnOffID_{|id,value|
		onOffs[id]=value;
		if (value==0) { midiBuffers[id].releaseAll };
		{this.refresh}.defer;
	}
	
	onOffToggle{|index| this.onOff_(index,(1-onOffs[ids[index]])) }
	
	midiOutToggle{
		midiOut = midiOut.not;
		this.changed(\midiOut);
		api.sendVP(\midiOut,\netMIDIOut_, midiOut.asInt);
		{this.refresh}.defer;
	}
	
	netMIDIOut_{|bool|
		midiOut=bool.isTrue;
		this.changed(\midiOut);
		{this.refresh}.defer;
	}
	
	// does this also need to update the presets??
	
	update{|object, model, arg1,arg2|
		
		if (model==\name) {
			if (ids.includes(arg1)) {
				names[ids.indexOf(arg1)]=arg2;
				this.autoSize;
			}
		};
		
		if (model==\instruments) {
			
			object = object ? (studio.insts);
			insts  = object.canBeSequenced;
			
			insts.remove(parent); // not me
			
			names  = insts.collect(_.name);
			ids    = insts.collect(_.id);
			instNo = object.canBeSequencedInstNo;
			
			// remove old
			onOffs.keys.difference(ids).do{|id|
				onOffs[id]=nil;
				midiBuffers[id]=nil;
			};
			
			// add new
			ids.difference(onOffs.keys).do{|id|
				onOffs[id]=0;
				midiBuffers[id]=LNX_MIDIBuffer()
					.midiPipeOutFunc_{|pipe| studio.insts[id].pipeIn(pipe) };
			};
			
			this.autoSize;

		};
		
	}	
	
	//////////////////////////////////////////////////////////
	
	// not sure when to use this 
	updatePresets{
		presets.do{|preset,i|
			preset.keys.difference(ids).do{|id| preset[id]=nil }; // remove old
			ids.difference(preset.keys).do{|id| preset[id]=0 }; // add new
		}
	}
	
	// purge just removes old ones 
	purgePresets{
		presets.do{|preset,i|
			preset.keys.difference(ids).do{|id| preset[id]=nil }; // remove old
		}
	}
	
	// get the current state as a list
	iGetPresetList{
		^["LNX_MultiPipeOut DOC", onOffs.size, midiOut.asInt]
			++onOffs.getPairs
			++["*** END LNX_MultiPipeOut DOC ***"];
	}
	
	// add a statelist to the presets
	iAddPresetList{|l|
		var size, presetToAdd;	
		l=l.reverse;		
		l.pop; // get rid of header	
		size=l.popI;
		
		midiOutPresets = midiOutPresets.add(l.popI.isTrue);
		
		presetToAdd = IdentityDictionary[];
		(l.popNI(size*2)).pairsDo{|key,value| presetToAdd[key]=value};
		presets = presets.add(presetToAdd);
	}
	
	// think about copy, paste, save, load etc...
	/*
	a.a.multiPipeOut.midiOutPresets;
	a.a.multiPipeOut.presets;
	a.a.multiPipeOut.updatePresets;
	a.a.multiPipeOut.ids;
	*/
	 
	// save a state list over a current preset
	iSavePresetList{|i,l|
		var size, presetToAdd;	
		l=l.reverse;		
		l.pop; // get rid of header	
		size=l.popI;
		
		if (midiOutPresets[i].notNil) { midiOutPresets[i] = l.popI.isTrue };
		
		presetToAdd = IdentityDictionary[];
		(l.popNI(size*2)).pairsDo{|key,value| presetToAdd[key]=value};
		if (presets[i].notNil) { presets[i] = presetToAdd };
			
	}

	// for your own load preset
	iLoadPreset{|i|
		var preset;
	
		if (midiOut != midiOutPresets[i]) {
			midiOut = midiOutPresets[i];
			this.changed(\midiOut);	
		};
	
		preset = presets[i];
		onOffs.keysDo{|id|
			if (preset[id].notNil) { 
				onOffs[id]= preset[id];
				if (onOffs[id]==0) { midiBuffers[id].releaseAll };
			};
		};

		{ this.refresh }.defer;
	}
	
	// for your own remove preset
	iRemovePreset{|i|
		presets.removeAt(i);
		midiOutPresets.removeAt(i);
	}
	
	// for your own removal of all presets
	iRemoveAllPresets{
		presets = []; // for onOffs
		midiOutPresets = []; // for midiOut OnOff
	}
	
	// via insts order? update/purge 1st?
	
	/*
	
	a.a.multiPipeOut.getSaveList;
	a.a.multiPipeOut.presets.last;
	
	a.a.multiPipeOut.
	
	*/
	
	// the save list
	getSaveList{ 
		
		this.purgePresets; // remove any missing presets attached to deleted instruments
			
		^["LNX_MultiPipeOut DOC", onOffs.size, presets.size, midiOut.asInt]
		
		// we need to use inst no and not ids when saving because
		// instrument ids will chnage on next load, esp after moving / adding other insts
		
		//++ (instNo.collect{|i,j| [i,onOffs[ids[j]]] }.flat) 
		
		// ? is a sloppy fix for rare duplicate bug NO FIX HERE
		
		++ (onOffs.collect{|value,id|  [instNo[ids.indexOf(id)], value]   }.asList.flat)
		
		// NOW OLD ONES CAN NO LONGER BE PRESSED
		
		++ (presets.collect{|preset,i|
				[preset.size, midiOutPresets[i].asInt]
					++ preset.collect{|value,id| [ instNo[ids.indexOf(id)] , value] }
			}.flat)
		
		++ ["*** END LNX_MultiPipeOut DOC ***"];		
			
	}
	
	// and put it back in
	putLoadList{|l| postLoadList=l } // save it until all the song is loaded and we have id's
	
	// after the song has been loaded and id's assigned to the insts.
	iPostSongLoad{|offset|
		var onOffsSize, presetsSize, l = postLoadList;
		
		offset = offset ? 0;
		
		if (postLoadList.notNil) {
		
			l=l.reverse;		
			l.pop; // get rid of header	
			onOffsSize=l.popI;
			presetsSize=l.popI;
			
			// current settings
			midiOut=l.popI.isTrue;
			onOffs.clear;
			onOffsSize.do{
				var no     = l.popI + offset;
				var value  = l.popI;
				var id;
				
				if (instNo.indexOf(no).notNil) {
					id = ids[instNo.indexOf(no)]; // need to test becasue of paste & add
					if (id.notNil) {
						onOffs[id] = value;
						if (value==0) { midiBuffers[id].releaseAll }; // what if we are pasting?
					};
				};
			};
			
			// presets
			presets=[];
			midiOutPresets=[];
			presetsSize.do{
				var presetToAdd;
				var presetSize = l.popI;
				midiOutPresets = midiOutPresets.add(l.popI.isTrue);
				presetToAdd = IdentityDictionary[];
				presetSize.do{
					var no     = l.popI + offset;
					var value  = l.popI; 
					var id;
					if (instNo.indexOf(no).notNil) { // as above
						id = ids[instNo.indexOf(no)];
						if (id.notNil) {
							presetToAdd[id] = value;
						};
					};
				};
				presets=presets.add(presetToAdd);
			};
			
			this.update(model:\instruments); // this is important to make all onOffs
			// because of onOffs.clear earlier
			//{this.refresh}.defer; // no longer needed
		};
		
		postLoadList=nil; // stops it if addSong happens

	}
	
	iPostSongAdd{}
	
	iPostPaste{}
	
	// GUI //////////////////////////////////////////////////////////
	
	refresh{ if (gui[\userView].notNil) {gui[\userView].refresh} }
	
	makeGUI{|window,argRect|
		
		var 	colors=(
			'background'	: Color.black.alpha_(0.5),
			'stringOn'	: Color(0.9,0.9,1),
			'stringOff'	: Color(0.9,0.9,1)*0.6,
			'on'			: Color(0.9,0.9,1),
			'off'		: Color(0.9,0.9,1)*0.8,
		);
		
		var font = Font("Helvetica",11);
		
		rect=argRect;
		
		fontHeight = "".bounds(font).height+3;
		w=rect.width;
		h=rect.height;
		
		gui[\scrollView] = MVC_ScrollView(window, rect)
			.hasBorder_(true)
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false)
			.autohidesScrollers_(false);
		
		gui[\userView] = MVC_UserView(gui[\scrollView], Rect(0,0,w,h))
			.drawFunc_{|me|
				w=me.bounds.width;
				h=me.bounds.height;
				
				Pen.use{
					Pen.smoothing_(false);
					colors[\background].set;
					Pen.fillRect(Rect(0,0,w,h));
					Pen.font_(font);
					Pen.smoothing_(true);
					
					// midi
					colors[\off].set;
					Pen.strokeOval( Rect(1,2,fontHeight-4,fontHeight-4).insetBy(2,2) );
			
					Pen.moveTo(0@fontHeight);
					Pen.lineTo(w@fontHeight);
					Pen.stroke;
			
					if (midiOut) {
						colors[\on].set;
						Pen.fillOval( Rect(1,2,fontHeight-4,fontHeight-4).insetBy(4,4));	
						Pen.fillColor_(colors[\stringOn]);
					}{
						Pen.fillColor_(colors[\stringOff]);
					};
					Pen.stringLeftJustIn("MIDI Out", Rect(13,0,w-fontHeight-20,fontHeight));
			
					// insts
					names.do{|name,n|
						colors[\off].set;
						Pen.strokeOval(
							Rect(1,(n+1)*fontHeight+2,fontHeight-4,fontHeight-4)
								.insetBy(2,2)
						);
						if (onOffs[ids[n]]==1) {
							colors[\on].set;
							Pen.fillOval(
							Rect(1,(n+1)*fontHeight+2,fontHeight-4,fontHeight-4)
								.insetBy(4,4);
							);	
						};					
					};
					names.do{|name,n|
						Pen.fillColor_(colors[(onOffs[ids[n]]==1).if(\stringOn,\stringOff)]);
						Pen.stringLeftJustIn(name,
							Rect(13,(n+1)*fontHeight,w-fontHeight-20,fontHeight));
					};
				};

			}
			.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
				y=y.div(fontHeight).clip(0,names.size)-1;
				if (y>=0) {
					this.onOffToggle(y);
				}{
					this.midiOutToggle;
				}
			};
	}
	
	internalHeight{ ^((names.size+1)*fontHeight)
						.clip(rect.height-1,rect.height+((names.size+1)*fontHeight))
	}
	
	autoSize{ gui[\userView].bounds_(Rect(0,0,w,this.internalHeight)) }
	
}

