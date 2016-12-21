/*
a.a.touch(100);
a.a.touch(0);
*/

+ LNX_Melody {

	// main body //////////////////////////////////////////////////////////////////////////////

	serverReboot{} // override to stop notesOn been reset on server reboot

	iInitVars{

		arpegSequencer = MVC_StepSequencer(
						(id.asString++"_Seq").asSymbol,32,midiControl,1000, \switch )
				.name_("")
				.fill(2)
				.action_{|velocity,latency,beat,beatNo,dur|
					this.bang(velocity,latency,beat,beatNo,dur)
				};

		// networked pianoRoll
		sequencer = LNX_PianoRollSequencer(id++\pR)
			.pipeOutAction_{|pipe| this.fromSequencer(pipe) }
			.releaseAllAction_{
				this.stopAllNotes;
				{gui[\keyboard].clear}.defer(studio.actualLatency);
			}
			.keyDownAction_{|me, char, modifiers, unicode, keycode, key|
				gui[\keyboard].view.keyDownAction.value(me,char, modifiers, unicode, keycode, key)
			}
			.keyUpAction_{|me, char, modifiers, unicode, keycode, key|
				gui[\keyboard].view.keyUpAction.value(me, char, modifiers, unicode, keycode, key)
			}
			.recordFocusAction_{ gui[\keyboard].focus }
			.spoModel_(models[27]);

		// store chords mod
		storeChordsMod = LNX_StoreChords()
			.midiPipeOutFunc_{|pipe| this.fromStoreChordsMod(pipe) }
			.apiID_(id++"_sc")
			.model_(\record, models[10])
			.model_(\play,   models[11])
			.model_(\root,   models[26])
			.model_(\spo,    models[27])
			.chordOnFunc_ {|n,key|
				gui[\storeChordsListView].hilite_(n,Color.red);
				{
					if (gui[\keyboard].notNil) { gui[\keyboard].setStoreActive(key) };

					gui[\storeChordsLamp].keyIn(n,1);

				}.defer;
			}
			.chordOffFunc_{|n,key|
				gui[\storeChordsListView].hilite_(n,nil);
				{
					if (gui[\keyboard].notNil) { gui[\keyboard].removeStoreActive(key) };

					gui[\storeChordsLamp].keyIn(n,nil);

				}.defer;
			};

		// music mod
		musicMod = LNX_Music()
			.midiPipeOutFunc_{|pipe| this.fromMusicMod(pipe) }
			.model_(\onOff,     models[4])
			.model_(\chord,     models[5])
			.model_(\transpose, models[12])
			.model_(\spo,    models[27])
			.storeChordsMod_(storeChordsMod);

		// chord quant mod
		chordQuantiserMod = LNX_ChordQuantiser()
			.midiPipeOutFunc_{|pipe| this.fromChordQuantiserMod(pipe) }
			.model_(\onOff,     models[14])
			.model_(\chord,     models[13])
			.model_(\retrigger, models[16])
			.model_(\transpose, models[21])
			.model_(\spo,       models[27]);

		// add more stuff to chords mod
		storeChordsMod
			.addDependant(chordQuantiserMod)
			.addDependant({|object, model, arg1, arg2|
				{
				gui[\storeChordsListView].items_(arg2);

				gui[\keyboard].clearAllStoreColorsNoRefresh;
				arg2.do{|i,j|
					gui[\keyboard].setStoreColorNoRefresh(object.offset+j,Color(1,0,0,0.75));
				};
				if (gui[\keyboard].notNil) {gui[\keyboard].refresh}
				}.defer;
			});

		// arpeg mod
		arpeggiatorMod = LNX_Arpeggiator()
			.midiPipeOutFunc_{|pipe| this.fromArpeggiatorMod(pipe) }
			.model_(\onOff,    models[6])
			.model_(\style,    models[7])
			.model_(\duration, models[9])
			.model_(\up,       models[17])
			.model_(\down,     models[18])
			.model_(\repeat,   models[19])
			.model_(\ratio,    models[8])
			.model_(\legato,   models[22])
			.model_(\poly,   	models[25])
			.model_(\spo,      models[27]);

		// midiBuffers 1, 2, 3, 4, 5 & 6
		midiBuffer1=LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromMIDIBuffer1(pipe) };
		midiBuffer2=LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromMIDIBuffer2(pipe) };
		midiBuffer3=LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromMIDIBuffer3(pipe) }
			.allPass_(true);
		midiBuffer4=LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromMIDIBuffer4(pipe) }
			.allPass_(true)
			.model_(\velocity, models[20]);
		midiBuffer5=LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromMIDIBuffer5(pipe) };
		midiBuffer6=LNX_MIDIBuffer().midiPipeOutFunc_{|pipe| this.fromMIDIBuffer6(pipe) };

		multiPipeOut = LNX_MultiPipeOut(studio,this)
			.apiID_(id++"_mpo")
			.model_(\rotate, models[29])
			.midiOutBuffer_(midiBuffer6)
			.addDependant({|object, model, arg1, arg2| midiBuffer6.releaseAll });

	}

	// attach actions to the keyboard
	initKeyboard{
		gui[\keyboard].keyDownAction_{|note|
				lastKeyboardNote=note;
				this.fromKeyboard(LNX_NoteOn(note,100,nil,\keyboard));
			}
			.keyUpAction_{|note|
				this.fromKeyboard(LNX_NoteOff(note,100,nil,\keyboard));
			}
			.keyTrackAction_{|note|
				if (lastKeyboardNote.notNil) {
					this.fromKeyboard(LNX_NoteOff(lastKeyboardNote,100,nil,\keyboard));
				};
				lastKeyboardNote=note;
				this.fromKeyboard(LNX_NoteOn(note,100,nil,\keyboard));
			}
			.spaceBarAction_{
				storeChordsMod.guiAddChord;
			};

		//window.endFrontAction_{
		gui[\keyboard].focusLostAction_{
			midiBuffer1.releaseSource(\keyboard);
			midiBuffer2.releaseSource(\keyboard);
			midiBuffer3.releaseSource(\keyboard);
		};
	}

	// used for noteOff in sequencers
	// efficiency issue: this is called 3 times in alt_solo over a network
	stopNotesIfNeeded{|latency|
		if ((instOnSolo.isOff)and:{this.alwaysOn.not}) {this.stopAllNotes};
		this.updateOnSolo(latency);
	}

	// also called by onOff & solo buttons & alwaysOn model
	stopAllNotes{
		midiBuffer1.releaseAll(studio.actualLatency);
		midiBuffer2.releaseAll(studio.actualLatency);
		midiBuffer3.releaseAll(studio.actualLatency);
		midiBuffer4.releaseAll(studio.actualLatency);
		midiBuffer5.releaseAll(studio.actualLatency);
		chordQuantiserMod.releaseAll(studio.actualLatency);
		gui[\pianoRollLamp].releaseAll(studio.actualLatency);
	}

	// the slower clock
	clockIn{|index,latency|
		var updateFunc = arpegSequencer.clockIn(index,studio.actualLatency);
		// the optimisation updates pos but only defers once for all channels.
		{ updateFunc.value }.defer(latency); // too many defers make Qt gui slow on MacOS.
	}

	// midi clock in (this is at MIDIClock rate)
	clockIn3{|beat,absTime,latency,absBeat|
		 sequencer.clockIn3(beat,absTime,studio.actualLatency,absBeat) }

	// reset sequencers posViews
	clockStop {
		this.stopAllNotes;
		sequencer.clockStop(studio.actualLatency);
		arpegSequencer.clockStop(studio.actualLatency);
		multiPipeOut.reset;
		arpeggiatorMod.reset;

	}

	// remove any clock hilites
	clockPause{
		this.stopAllNotes;
		sequencer.clockPause(studio.actualLatency);
		arpegSequencer.clockPause(studio.actualLatency);
	}

	// override instTemplate
	pipeIn{|pipe|	this.fromMIDIIn(pipe)  }

	// MIDI In
	noteOn{|note, velocity, latency|
		this.fromMIDIIn(LNX_NoteOn(note,velocity,latency,\MIDIIn))
	}
	noteOff{|note, velocity, latency|
		this.fromMIDIIn(LNX_NoteOff(note,velocity,latency,\MIDIIn))
	}
	touch{|pressure, latency|
		this.fromMIDIIn(LNX_Touch(pressure , latency, \MIDIIn))
	}
	control{|num,  val, latency| this.fromMIDIIn(LNX_Control(num,  val , latency, \MIDIIn)) }
	bend{|bend, latency| this.fromMIDIIn(LNX_Bend(bend , latency, \MIDIIn)) }

	bang{|velocity,latency,beat,beatNo,dur|
		arpeggiatorMod.bang(velocity,latency,beat,beatNo,absTime,dur)
	}

	iFreeAutomation{ arpegSequencer.freeAutomation }

	iFree{
		arpeggiatorMod.free;
		arpegSequencer.free;
		multiPipeOut.free;
		sequencer.free;
	}

	// pipes in for distribution ********************************************************** !!!

	// FROM (PATCH HERE) *******

	fromKeyboard{|pipe|
		this.toMIDIBuffer1(pipe);
	}

	fromMIDIIn{|pipe|
		if (pipe.historyIncludes(this)) { ^this }; // drop out to prevent feedback loops
		if (pipe.isNote) {
			// drop out if out of midi range
			if (((pipe.note)<p[2])or:{(pipe.note)>p[3]}) {^nil};
			if (p[15].isTrue) { this.toMIDIBuffer1(pipe) };
		};
		if (pipe.isTouch) {this.toMIDIBuffer1(pipe)};
	}

	fromMIDIBuffer1{|pipe|
		this.toMusicMod(pipe);
		this.toKeyboardSelectColors(pipe);
		if (p[24].isTrue) {this.toNet(pipe)};
		gui[\midiInLamp].pipeIn(pipe);
	}

	fromNetwork{|pipe|
		if (p[24].isTrue) { this.toMIDIBuffer5(pipe) };
	}

	fromMIDIBuffer5{|pipe|
		this.toMusicMod(pipe);
		this.toKeyboardSelectColors(pipe);
		gui[\midiInLamp].pipeIn(pipe);
	}

	fromMusicMod{|pipe|
		if (pipe.source==\music) { gui[\musicLamp].pipeIn(pipe)};
		this.toStoreChordsMod(pipe)
	}

	fromStoreChordsMod{|pipe|
		this.toSequencer(pipe);
		this.toMIDIBuffer2(pipe);
	}

	fromSequencer{|pipe|
		this.toMIDIBuffer2(pipe);
		gui[\pianoRollLamp].pipeIn(pipe);
	}

	fromMIDIBuffer2{|pipe|
		this.toChordQuantiserMod(pipe);
		if (pipe.source==\sequencer) { this.toKeyboardSelectColors(pipe) };
	}

	fromChordQuantiserMod{|pipe|
		this.toMIDIBuffer3(pipe);
	}

	fromMIDIBuffer3{|pipe|

		if (pipe[\quant].isTrue) {
			gui[\quantiseLamp].pipeIn(pipe)
		};

		this.toArpeggiatorMod(pipe);
	}

	fromArpeggiatorMod{|pipe|
		if (pipe.source==\arpeg) { gui[\arpegLamp].pipeIn(pipe)};
		if ((p[23].isTrue)or:{this.isOn}) {
			this.toMIDIBuffer4(pipe);
		};
	}

	fromMIDIBuffer4{|pipe|
		//if (multiPipeOut.midiOut) { this.toMIDIBuffer6(pipe) };
		this.toMultiPipeOut(pipe);
		this.toKeyboardColors(pipe);
		gui[\midiOutLamp].pipeIn(pipe);
	}

	fromMIDIBuffer6{|pipe|
		this.toMIDIOut(pipe);
	}

	// TO *****************************************************************

	toNet{|pipe|
		if (pipe.isNoteOn || pipe.isNoteOff) {
			api.sendOD(\netMIDI, pipe.kind, pipe.note, pipe.velocity);
		};
	}

	netMIDI{|type,note,velocity|
		if (type==\noteOn)  { this.fromNetwork(LNX_NoteOn (note,velocity,nil,\network)) };
		if (type==\noteOff) { this.fromNetwork(LNX_NoteOff(note,velocity,nil,\network)) };
	}

	toMIDIBuffer1       {|pipe| midiBuffer1.pipeIn(pipe) }
	toMIDIBuffer2       {|pipe| midiBuffer2.pipeIn(pipe) }
	toMIDIBuffer3       {|pipe| midiBuffer3.pipeIn(pipe) }
	toMIDIBuffer4       {|pipe| midiBuffer4.pipeIn(pipe) }
	toMIDIBuffer5       {|pipe| midiBuffer5.pipeIn(pipe) }
	//toMIDIBuffer6       {|pipe| midiBuffer6.pipeIn(pipe) }
	toMusicMod          {|pipe| musicMod.pipeIn(pipe) }
	toStoreChordsMod    {|pipe| storeChordsMod.pipeIn(pipe) }
	toChordQuantiserMod {|pipe| chordQuantiserMod.pipeIn(pipe) }
	toArpeggiatorMod    {|pipe| arpeggiatorMod.pipeIn(pipe) }
	toMultiPipeOut      {|pipe| multiPipeOut.pipeIn(pipe.addLatency(syncDelay)) }

	toSequencer{|pipe|
		case
			{pipe.isNoteOn } { sequencer.noteOn (pipe.note, pipe.velocity/127, pipe.latency) }
			{pipe.isNoteOff} { sequencer.noteOff(pipe.note, pipe.velocity/127, pipe.latency) };
	}

	toMIDIOut{|pipe|
		case
			{pipe.isNoteOn } { midi.noteOn (pipe.note, pipe.velocity, pipe.latency) }
			{pipe.isNoteOff} { midi.noteOff(pipe.note, pipe.velocity, pipe.latency) }
	}

	toKeyboardSelectColors{|pipe|
		{
			if (gui[\keyboard].notNil) {
				case
					{pipe.isNoteOn } {
						gui[\keyboard].setSelectColor(pipe.note,Color(1,0.25,0.25),1) }
					{pipe.isNoteOff} { gui[\keyboard].removeSelectColor(pipe.note) };
			};
		}.defer(pipe.latency?0);
	}

	toKeyboardColors{|pipe|
		{
			if (gui[\keyboard].notNil) {
				case
					{pipe.isNoteOn } {
						gui[\keyboard].setColor(pipe.note,Color(1,0.5,0)+0.33,1) }
					{pipe.isNoteOff} { gui[\keyboard].removeColor(pipe.note)};
			};
		}.defer(pipe.latency?0);
	}

} // end ////////////////////////////////////
