
// midi patching  ////////////////////////////////////////////////////////////////////

LNX_MIDIPatch {

	classvar <patches, 		<noPatches=0,
			<initialized=false,
	
			<noInPorts, 		<noOutPorts,
			<midiSourceNames, 	<midiDestinationNames,
			<midiSourceUIDs, 	<midiDestinationUIDs,
			<inPortsActive,	<outPortsActive,
			<inPoints,		<outPoints,
			<outs,
			
			noInternalBuses=3, <noteOnTrigFunc;
			
	classvar	nextUnusedIn= -1,	nextUnusedOut= -1;
	
	classvar <>verbose = false;
	
	classvar <>midiSyncLatency = 0.1;

	var		<>patchNo,
			
			<midiIn,	  		<midiOut, 	
			<uidIn,			<uidOut,
			<midiInChannel,	<midiOutChannel,
			<midiInName,       <midiOutName,
			
			<>noteOnFunc,		<>noteOffFunc,
			<>controlFunc, 	<>bendFunc, 
			<>touchFunc,		<>programFunc,
			<>sysrtFunc,		<>internalFunc,
			
			<>pipeFunc,  // to replace all off the above funcs... when i get round to it
			
			<>previousInUID,	<>previousOutUID,
			
			<portInGUI,		<portOutGUI,
			channelInGUI,		channelOutGUI,
			
			<window,			<>action;
			
	// find in & out port by name (uses find not ==)
	findByName{|inName,outName|
		var inIndex, outIndex;
		midiSourceNames.do{|name,i| [name, name.find(inName)].postln; if (name.find(inName).notNil) { inIndex=i } };
		midiDestinationNames.do{|name,i| if (name.find(outName).notNil) { outIndex=i } };
		if (inIndex.notNil) { uidIn = midiSourceUIDs[inIndex] };
		if (outIndex.notNil) { uidOut = midiDestinationUIDs[outIndex] };
		this.putLoadList([uidIn,midiInChannel,uidOut,midiOutChannel]);
	}

	*new { arg in=0, inC=(-1), out=1, outC=0; if (initialized) {^super.new.init(in,inC,out,outC)} }
	
	*initClass { patches=[] }
	
	*nextUnusedIn{
		nextUnusedIn=nextUnusedIn+1;
		//^[(nextUnusedIn/16).asInt%noInternalBuses+1,nextUnusedIn%16]
		^[1,0];
	}
	
	*nextUnusedOut{
		nextUnusedOut=nextUnusedOut+1;
		^[(nextUnusedOut/16).asInt%noInternalBuses+1,nextUnusedOut%16]
		//^[1,0];
	}
	
	*resetUnused{ nextUnusedIn= -1; nextUnusedOut= -1;}
	
	initInstance{
		patches=patches.add(this);
		patchNo=noPatches;
		noPatches=noPatches+1;
	}
	
	init { arg in,inC,out,outC;
		this.initInstance;
		this.initVars;
		this.setInterface(in,inC,out,outC);
	}
	
	outPoint{^outPoints[midiOut]}
	inPoint{^inPoints[midiIn]}
	
	*init {
	
		if (initialized.not) {
		 
		 	//MIDIClient.init;
			MIDIClient.init(50,50); // why 50?
			noInPorts=MIDIClient.sources.size;
			noOutPorts=MIDIClient.destinations.size;
			
			midiSourceNames=["None"];
			midiSourceUIDs=[0];
			inPortsActive=[true];
			
			MIDIClient.sources.do({|m|
				midiSourceUIDs=midiSourceUIDs.add(m.uid);
				m=m.asString;
				m=m[13..(m.size -2)].replace("\"","","\"");
				m=m.split($,).drop(0);
				if (m[0]=="IAC Driver") {
					m="IAC: "++(m[1].drop(1));
				}{
					if (m[0]=="RemoteSL IN") {
						m="Remote SL: "++(m[1].drop(1));
					}{
						m=m[1].drop(1);
					}
				};
				midiSourceNames=midiSourceNames.add(m);
				inPortsActive=inPortsActive.add(true);
			});
			
			noInternalBuses.do({|i|
				midiSourceNames=midiSourceNames.add("Internal: Bus"+((i+1).asString));
				midiSourceUIDs=midiSourceUIDs.add(i+1);
				inPortsActive=inPortsActive.add(true);
			});
		
			midiDestinationNames=["None"];
			midiDestinationUIDs=[0];
			outPortsActive=[true];
			MIDIClient.destinations.do({|m|
				midiDestinationUIDs=midiDestinationUIDs.add(m.uid);
				m=m.asString;
				m=m[13..(m.size -2)].replace("\"","","\"");
				m=m.split($,).drop(0);
				if (m[0]=="IAC Driver") {
					m="IAC: "++(m[1].drop(1));
				}{
					m=m[1].drop(1);
				};
				midiDestinationNames=midiDestinationNames.add(m);
				outPortsActive=outPortsActive.add(true);
			});
			
			noInternalBuses.do({|i|
				midiDestinationNames=midiDestinationNames.add("Internal: Bus"+((i+1).asString)); 
				midiDestinationUIDs=midiDestinationUIDs.add(i+1);
				outPortsActive=outPortsActive.add(false);
			});
			
			outs=[NoMIDI];
			outPoints=[NoMIDI];
			noOutPorts.do({|i|
				outPoints = outPoints.add(LNX_MIDIEndPoint(MIDIClient.destinations[i]));
				outs = outs.add( MIDIOut(i, MIDIClient.destinations[i].uid));
				outs[i].latency_(0);
				// latency is set to zero, the studio will control when latency is apllied
				// this depends on internal sequencers, external midi in
				// no midiclock, ext midiClockIn or send midiClockOut
				// seq control signals (internal external)
			
			});
			noInternalBuses.do({
				outs=outs.add(NoMIDI);
				outPoints = outPoints.add(NoMIDI);
			});
			
			inPoints=[NoMIDI];
			
			noInPorts.do({|i| 
				inPoints = inPoints.add(LNX_MIDIEndPoint(MIDIClient.sources[i]));
				MIDIIn.connect(i, MIDIClient.sources[i].uid);
			});
			noInternalBuses.do({
				inPoints=inPoints.add(NoMIDI);
			});
			
			noInPorts=noInPorts+1+noInternalBuses;
			noOutPorts=noOutPorts+1+noInternalBuses;
			
			MIDIIn.noteOn  = { arg src, chan, note, vel;
				//["note on","src",src,"chan",chan,"note",note,"vel",vel].postln;
				patches.do({|patch|
					if ( (patch.uidIn==src) and:
						{(chan==(patch.midiInChannel)) or: {(patch.midiInChannel)==(-1)}} ) {
							patch.noteOnFunc.value(src, chan, note, vel);
							// send with no latency parameter
							if (patch.pipeFunc.notNil) {
								var pipe = LNX_NoteOn(note,vel,nil,\external);
								pipe[\endPoint] = patch.inPoint;
								pipe[\source] = src;
								pipe[\channel] = chan;
								
								patch.pipeFunc.value(pipe);
							};	
					}
				});
			};
			MIDIIn.noteOff = { arg src, chan, note, vel;
				//["note off","src",src,"chan",chan,"note",note,"vel",vel].postln;
				patches.do({|patch|
					if ( (patch.uidIn==src) and:
						{(chan==(patch.midiInChannel)) or: {(patch.midiInChannel)==(-1)}} ) {
							patch.noteOffFunc.value(src, chan, note, vel);
								// send with no latency parameter
							
							if (patch.pipeFunc.notNil) {
								var pipe = LNX_NoteOff(note,vel,nil,\external);
								pipe[\endPoint] = patch.inPoint;
								pipe[\source] = src;
								pipe[\channel] = chan;
								patch.pipeFunc.value(pipe);
							};	
						}
				});
			};
			MIDIIn.program = { arg src, chan, prog;
				//["program","src",src,"chan",chan,"prog",prog].postln;
				patches.do({|patch|
					if ( (patch.uidIn==src) and:
						{(chan==(patch.midiInChannel)) or: {(patch.midiInChannel)==(-1)}} ) {
							patch.programFunc.value(src, chan, prog);
								// send with no latency parameter
							if (patch.pipeFunc.notNil) {
								var pipe = LNX_Program(prog,nil,\external);
								pipe[\endPoint] = patch.inPoint;
								pipe[\source] = src;
								pipe[\channel] = chan;
								patch.pipeFunc.value(pipe);
							};	
					}
				});
			};		
			MIDIIn.control = { arg src, chan, num,  val;
				//["control","src", src, "chan", chan, "num", num, "val", val,
				//		(val>64).if(64-val,val)].postln;
				
	LNX_MIDIControl.controlIn(src, chan, num,  val, nil, true, false); // direct into MIDI Control
				// later i might need to think about this more
				// or i could plug all of these diredtly in MIDIIn
				// the last arg is sourceIsInternal
				
				patches.do({|patch|
					if ( (patch.uidIn==src) and:
						{(chan==(patch.midiInChannel)) or: {(patch.midiInChannel)==(-1)}} ) {
							patch.controlFunc.value(src, chan, num, val, nil, true, false);
								// send with no latency parameter
								
							if (patch.pipeFunc.notNil) {
								var pipe = LNX_Control(num,val,nil,\external);
								pipe[\endPoint] = patch.inPoint;
								pipe[\source] = src;
								pipe[\channel] = chan;
								patch.pipeFunc.value(pipe);
							};	
					}
				});

			};
			MIDIIn.bend    = { arg src, chan, bend;
				patches.do({|patch|
					if ( (patch.uidIn==src) and:
						{(chan==(patch.midiInChannel)) or: {(patch.midiInChannel)==(-1)}} ) {							patch.bendFunc.value(src, chan, bend);
							
																			if (patch.pipeFunc.notNil) {
								var pipe = LNX_Bend(bend,nil,\external);
								pipe[\endPoint] = patch.inPoint;
								pipe[\source] = src;
								pipe[\channel] = chan;
								patch.pipeFunc.value(pipe);
							};	
							
								
						// send with no latency parameter
							
		LNX_MIDIControl.controlIn(src, chan, -1,  bend/16383*127, nil, true, false); // direct into
						
					}
				});
			};
			MIDIIn.touch   = { arg src, chan, pressure;
				patches.do({|patch|
					if ( (patch.uidIn==src) and:
						{(chan==(patch.midiInChannel)) or: {(patch.midiInChannel)==(-1)}} ) {							patch.touchFunc.value(src, chan, pressure);
							// send with no latency parameter
							
																			if (patch.pipeFunc.notNil) {
								var pipe = LNX_Touch(pressure,nil,\external);
								pipe[\endPoint] = patch.inPoint;
								pipe[\source] = src;
								pipe[\channel] = chan;
								patch.pipeFunc.value(pipe);
							};	
					}
				});
			};
			
			MIDIIn.sysrt = { arg src, chan, val;
				//[src,chan,val].postln;			
				patches.do({|patch|
					if ( patch.uidIn==src ) {
						patch.sysrtFunc.value(src, chan, val); // send with no latency parameter
					}
				});
			};
			
			if (verbose) { 					
				Post << "MIDI Sources: " << Char.nl;
				midiSourceNames.do({ |x,y|
					Post << Char.tab << y << " : " << x << Char.nl });
				Post << "MIDI Destinations: " << Char.nl;
				midiDestinationNames.do({ |x,y|
					Post << Char.tab << y << " : " << x << Char.nl });
			};
			
			initialized=true;
			
		}{ // else.if initialized
		
			if (verbose) { "MIDI Client already initialized".postln; };
			
		}; // end.if initialized
	}
	
	// this is used to get a midi note on trigger
	// it replace note on until the 1st noteOn is recieved
	*noteOnTrigFunc_{|f|
		noteOnTrigFunc=f;
		MIDIIn.noteOn  = { arg src, chan, note, vel;
			//["note on","src",src,"chan",chan,"note",note,"vel",vel].postln;
			patches.do({|patch|
				if ( (patch.uidIn==src) and:
					{(chan==(patch.midiInChannel)) or: {(patch.midiInChannel)==(-1)}} ) {
						patch.noteOnFunc.value(src, chan, note, vel);
									// send with no latency parameter
																		if (patch.pipeFunc.notNil) {
							var pipe = LNX_NoteOn(note,vel,nil,\external);
							pipe[\source] = src;
							pipe[\channel] = chan;
							patch.pipeFunc.value(pipe);
						};	
				}
			});
			if (noteOnTrigFunc.notNil) {
				noteOnTrigFunc.value(src, chan, note, vel);
				noteOnTrigFunc=nil;
				// replace original func after
				MIDIIn.noteOn  = { arg src, chan, note, vel;
					//["note onB","src",src,"chan",chan,"note",note,"vel",vel].postln;
					patches.do({|patch|
						if ( (patch.uidIn==src) and:
							{(chan==(patch.midiInChannel)) or:
							{(patch.midiInChannel)==(-1)}} ) {
								patch.noteOnFunc.value(src, chan, note, vel);
										// send with no latency parameter
										
																				if (patch.pipeFunc.notNil) {
									var pipe = LNX_NoteOn(note,vel,nil,\external);
									pipe[\source] = src;
									pipe[\channel] = chan;
									patch.pipeFunc.value(pipe);
								};	
						}
					});
				};
			};
		};
	}
	
	pipeIn{|pipe|
		
		if ((uidOut>0)and:{uidOut<=noInternalBuses}) {
		 	// internal buses
	 		patches.do({|patch,j|
				if ( (patch.uidIn==uidOut) and:{(patch===this).not} and:
					{(patch.midiInChannel==midiOutChannel) or: {patch.midiInChannel==(-1)}} ) {
						// internal
						patch.pipeFunc.value(pipe);
				}
			}); 		
	 	}{
		 	// external MIDIOUt
		 	var midi = outs[midiOut];
			var latency = pipe.latency;
		
			// i should be able to get rid of this line soon
			if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };

	 		switch (pipe.kind)
				{\noteOn} { // noteOn
					midi.noteOnLatency(midiOutChannel, pipe.note, pipe.velocity, latency); 
				}
				{\noteOff} { // noteOff
					midi.noteOffLatency(midiOutChannel, pipe.note, pipe.velocity, latency);
				}
				{\control} { // control *** might need to return name for step sequencer
					midi.controlLatency(midiOutChannel, pipe.num, pipe.val, latency);
				}
				{\touch} { // touch
					midi.touchLatency(midiOutChannel, pipe.pressure, latency)
				}
				{\program} {
					// to do and confirm
					midi.programLatency(midiOutChannel, pipe.program, latency );
				}
				{\bend} { // bend
					midi.bendLatency(midiOutChannel, pipe.val, latency) 
				}
	 	};
	}
	
	// these now include latency which is been supplied by the source
	// internal/externalInOut internalSeq control midiClockIn/out
	
	noteOn{|note, vel, latency|	
	 	if ((uidOut>0)and:{uidOut<=noInternalBuses}) {
		 	// internal buses
	 		patches.do({|patch,j|
				if ( (patch.uidIn==uidOut) and:{(patch===this).not} and:
					{(patch.midiInChannel==midiOutChannel) or: {patch.midiInChannel==(-1)}} ) {
						patch.noteOnFunc.value (uidIn, midiOutChannel, note, vel, latency);
						
						if (patch.pipeFunc.notNil) {
							var pipe = LNX_NoteOn(note,vel,nil,\internal);
							pipe[\source] = uidIn; 
							pipe[\channel] = midiOutChannel;
							patch.pipeFunc.value(pipe);
						};	
				}
			}); 		
	 	}{
		 	// MIDIOUt
		 	if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
	 		outs[midiOut].noteOnLatency(midiOutChannel, note, vel, latency); 
	 	};
	}
	 
	noteOff{|note, vel, latency|
	 	if ((uidOut>0)and:{uidOut<=noInternalBuses}) {
		 	// internal buses
	 		patches.do({|patch|
				if ( (patch.uidIn==uidOut) and:{(patch===this).not} and:
					{(patch.midiInChannel==midiOutChannel) or: {patch.midiInChannel==(-1)}} ) {
						patch.noteOffFunc.value (uidIn, midiOutChannel, note, vel, latency);
						
																		if (patch.pipeFunc.notNil) {
							var pipe = LNX_NoteOff(note,vel,nil,\internal);
							pipe[\source] = uidIn;
							pipe[\channel] = midiOutChannel;
							patch.pipeFunc.value(pipe);
						};	
				}
			}); 		
	 	}{
			// MIDIOut
			if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
			outs[midiOut].noteOffLatency(midiOutChannel, note,   vel, latency );		};		
	}
			
	control{|ctlNum, val, latency, send, ignore=false|
		var name;
		if ((uidOut>0)and:{uidOut<=noInternalBuses}) {
			name=LNX_MIDIControl.controlIn(
				uidOut, midiOutChannel, ctlNum,  val, latency, send, ignore);
				// direct into MIDI Control
	 		patches.do({|patch|
				if ( (patch.uidIn==uidOut) and:{(patch===this).not} and:
					{(patch.midiInChannel==midiOutChannel) or: {patch.midiInChannel==(-1)}} ) {
						patch.controlFunc.value (uidIn, midiOutChannel, ctlNum, val,
												       latency, send, ignore);
				}
			}); 		
	 	}{
		 	if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
			outs[midiOut].controlLatency(midiOutChannel, ctlNum, val, latency); // no latency to out
		};
		^name
	}
	
	// used as the control method but split in two (isNextControlLearn + learn)
	// to control traffic
	isNextControlLearn{
		if ((uidOut>0)and:{uidOut<=noInternalBuses}and:{LNX_MIDIControl.activeModel.notNil}){
			^true
		}{
			^false
		}
	}
	
	learn{|ctlNum, val| ^LNX_MIDIControl.learnIn(uidOut, midiOutChannel, ctlNum,  val); }
	
	program{|val, latency|	
		if ((uidOut>0)and:{uidOut<=noInternalBuses}) {
	 		patches.do({|patch|
				if ( (patch.uidIn==uidOut) and:{(patch===this).not} and:
					{(patch.midiInChannel==midiOutChannel) or: {patch.midiInChannel==(-1)}} ) {
						patch.programFunc.value (uidIn, midiOutChannel, val, latency);
				}
			}); 		
	 	}{
		 	if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
			outs[midiOut].programLatency(midiOutChannel, val, latency );
		};
	}

	// special internal messages only lnx understands (comms via midi) used in LNX_Controllers
	internal{|...msg|	
		if ((uidOut>0)and:{uidOut<=noInternalBuses}) {
	 		patches.do({|patch|
				if ( (patch.uidIn==uidOut) and:{(patch===this).not} and:
					{(patch.midiInChannel==midiOutChannel) or: {patch.midiInChannel==(-1)}} ) {
						patch.internalFunc.value(*msg);
				}
			}); 		
		};
	}

	
	// need to update these to include internal buses, but i'm not them at the moment
	bend { |val, latency|
		if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
		outs[midiOut].bendLatency   (midiOutChannel, val, latency);
		
	}
	touch{ |val, latency|
		if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
		outs[midiOut].touchLatency  (midiOutChannel, val, latency);
		
	}
	
	panic{ 127.do({|n|     outs[midiOut].noteOff(midiOutChannel, n, 0) }) }
	
	// for midi clock out, only used for external out
	songPtr {|songPtr,latency|
		if (((uidOut>0)and:{uidOut<=noInternalBuses}).not) {
			if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
			outs[midiOut].songPtrLatency(songPtr,latency);
		}
	}
	
	start {|latency|
		if (((uidOut>0)and:{uidOut<=noInternalBuses}).not) {
			if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
			outs[midiOut].startLatency(latency);
		}
	}
	
	stop {|latency|
		if (((uidOut>0)and:{uidOut<=noInternalBuses}).not) {
			if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
			outs[midiOut].stopLatency(latency);
		}
	}
	
	continue {|latency|
		if (((uidOut>0)and:{uidOut<=noInternalBuses}).not) {
			if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
			outs[midiOut].continueLatency(latency);
		}
	}
	
	midiClock {|latency|
		if (((uidOut>0)and:{uidOut<=noInternalBuses}).not) {
			if (latency.isNil) { latency = 0 }{ latency = latency + midiSyncLatency };
			outs[midiOut].midiClockLatency(latency);
		}
	}


	setInPort{|in|
		midiIn=in.clip(0,noInPorts+noInternalBuses- 1);
		uidIn=midiSourceUIDs.[midiIn];
		previousInUID=uidIn;
		midiInName=midiSourceNames[midiIn];
		this.refreshGUI;
	}
	
	setOutPort{|out|
		midiOut=out.clip(0,noOutPorts+noInternalBuses- 1);
		uidOut=midiDestinationUIDs.[midiOut];
		previousOutUID=uidOut;
		midiOutName=midiDestinationNames[midiOut];
		this.refreshGUI;
	}
	
	setInChannel{|inC|
		midiInChannel=inC.clip(-1,15);
		this.refreshGUI;
	}
	
	setOutChannel{|outC|
		midiOutChannel=outC.clip(0,15);
		this.refreshGUI;
	}
	
	setInterface{|in,inC,out,outC|
		midiIn=in.clip(0,noInPorts+noInternalBuses- 1);
		midiOut=out.clip(0,noOutPorts+noInternalBuses- 1);
		uidIn=midiSourceUIDs.[midiIn];
		uidOut=midiDestinationUIDs.[midiOut];
		previousInUID=uidIn;
		previousOutUID=uidOut;
		midiInChannel=inC.clip(-1,15);
		midiOutChannel=outC.clip(0,15);
		midiInName=midiSourceNames[midiIn];
		midiOutName=midiDestinationNames[midiOut];
		this.refreshGUI;
	}
		
	initVars{
	
	}
	
	*deviceSourceByUID{|u|
		if (u==0) {^["None","None"]};
		if ((u>0)&&(u<=16)) {^["Internal","Bus"+(u.asString)]};
		MIDIClient.sources.do({|i| if (i.uid==u) {^[i.device,i.name]}});
		^["Missing","None"];		
	}
	
	*deviceDestinationByUID{|u|
		if (u==0) {^["None","None"]};
		if ((u>0)&&(u<=16)) {^["Internal","Bus"+(u.asString)]};
		MIDIClient.destinations.do({|i| if (i.uid==u) {^[i.device,i.name]}});
		^["Missing","None"];		
	}
	
	getSaveList{ ^[uidIn,midiInChannel,uidOut,midiOutChannel] }
	
	putLoadList{ |l|
		# uidIn,midiInChannel,uidOut,midiOutChannel = l.asInteger;
		previousInUID=uidIn;
		midiIn =midiSourceUIDs.indexOf(uidIn)?0;
		if (midiIn==0) {uidIn=0};
		previousOutUID=uidOut;
		midiOut=midiDestinationUIDs.indexOf(uidOut)?0;
		if (midiOut==0) {uidOut=0};
		midiInName=midiSourceNames[midiIn];
		midiOutName=midiDestinationNames[midiOut];
		this.refreshGUI;
	}
	
	// net change
	putNetChangeList{|l|
		# uidIn,midiInChannel,uidOut,midiOutChannel = l.asInteger;
		if ((uidIn<0) or:{uidIn>noInternalBuses }) { uidIn =0 }; // you can't access others
		if ((uidOut<0)or:{uidOut>noInternalBuses}) { uidOut=0 }; // hardware yet...
		previousInUID=uidIn;
		midiIn =midiSourceUIDs.indexOf(uidIn)?0;
		if (midiIn==0) {uidIn=0};
		previousOutUID=uidOut;
		midiOut=midiDestinationUIDs.indexOf(uidOut)?0;
		if (midiOut==0) {uidOut=0};
		this.refreshGUI;
	}
	
	clear {
	
	}
	
	free {
		
		if ((patchNo+1)<noPatches) {
			for (patchNo+1, noPatches - 1, {|i|
				patches[i].patchNo=patches[i].patchNo - 1;
			});
		};
		patches.removeAt(patchNo);
		noPatches=patches.size;

	}
	 
	*uidInName{|i|
		i=midiSourceUIDs.indexOf(i);
		if (i.isNil) { ^"Missing" } { ^midiSourceNames[i] };
	}
	
	printOn { arg stream;
		stream
			<< this.class.name << "("
			<< " In : " << (34.asAscii) << midiInName
			<< (34.asAscii) << " , " << midiInChannel << " , "
			<< "Out : " << (34.asAscii) << midiOutName
			<< (34.asAscii) << " , " << midiOutChannel << " )"
	}
	// putLoadList doesn't update midiInName & midiOutName
	
	*dump {
			patches.postln; 		
			["In:" ,noInPorts, "Out:", noOutPorts].postln;
			"midiSourceNames".postln; 
			midiSourceNames.postln;	
			"midiDestinationNames".postln;
			midiDestinationNames.postln;
			"midiSourceUIDs".postln;
			midiSourceUIDs.postln;
			"midiDestinationUIDs".postln;
			midiDestinationUIDs.postln;
	}
	
/////////////// refresh ports ////////////////
	
	*changeNoInternalBuses{|b| this.refreshPorts(b) }
	
	*refreshPorts{|argNoInternalBuses|
	
		var newSources, newDestinations, removalsIn, removalsOut, win, text;
	
		argNoInternalBuses=argNoInternalBuses ? noInternalBuses;
	
		////////// get updated midi port lists //////////////////
	
		MIDIClient.list;
		newSources=[0];
		MIDIClient.sources.do     ({|m| newSources=newSources.add(m.uid.asInteger); });
		argNoInternalBuses.do     ({|i| newSources=newSources.add(i+1) });
		newDestinations=[0];
		MIDIClient.destinations.do({|m| newDestinations=newDestinations.add(m.uid.asInteger); });
		argNoInternalBuses.do     ({|i| newDestinations=newDestinations.add(i+1) });

		//////// REMOVALS ////////////////////////////////////////
		
		//////// 1st test for in & outs been used and removed ////

		removalsIn=Set[];
		patches.do({|p| var temp;
			if (newSources.includes(p.uidIn.asInteger).not) {
				removalsIn=removalsIn.add(p.midiIn);
				temp=p.uidIn;
				p.setInPort(0);
				p.previousInUID=temp;
			}
		});
		 
		removalsOut=Set[];
		patches.do({|p,j| var temp;
			//[p.uidOut,j].postln;
			if (newDestinations.includes(p.uidOut.asInteger).not) {
				//[p.uidOut,p.uidOut.species].postln;
				removalsOut=removalsOut.add(p.midiOut);
				temp=p.uidOut;
				p.setOutPort(0);
				p.previousOutUID=temp;
			}
		});
		
		//["remove:",removalsOut,newDestinations].postln;
		
		/////// and warn user //////////////////////////////////////
		
		if (((removalsIn.size)>0)||((removalsOut.size)>0)) {
		
		 	text="";
		 	if (removalsIn.size>0) {
				text=text++"\nMIDI Sources:\n";
				removalsIn.do({|i| text=text++"     "++midiSourceNames[i]++("\n"); });
			};			 
		 	if (removalsOut.size>0) {
				text=text++"\nMIDI Destinations:\n";
				removalsOut.do({|i| text=text++"     "++midiDestinationNames[i]++("\n"); });
			};	
		 
	 		win=Window.new("MIDI / Pitch",
			Rect(200,600, 460, 200), resizable: false);
			win.view.background = Gradient(Color.ndcBack2,Color.ndcBack3);
			win.front.alwaysOnTop_(true);
			
			StaticText.new(win,Rect(20, 10, 460, 42))
			.string_("                                                         WARNING !\n"
				++"The following MIDI Device(s) have been removed from the system.")
			.stringColor_(Color.white);
				
			TextView.new(win,Rect(50, 60, 350, 87))
			.hasVerticalScroller_(true)
			.editable_(false)
			.string_(text);
				
			SCButton.new(win,Rect(373, 158, 60, 25))  
			.states_([ [ "OK", Color(1.0, 1.0, 1.0, 1.0), Color.ndcDarkButtonBG ]])
			.action_{win.close}
			.focus;
		
		};

		//////// then test all in & outs removed to capture the others //
	
		removalsIn=Set[];
		midiSourceUIDs.do({|p,i| 
			if (newSources.includes(p).not) {
				removalsIn=removalsIn.add(i);
			}
		});
		 
		removalsOut=Set[];
		midiDestinationUIDs.do({|p,i| 
			if (newDestinations.includes(p).not) {
				removalsOut=removalsOut.add(i);
			}
		});
	
		/////// disable ports & gui menu items ///////////////////////////
	
		if (removalsIn.size>0) {
			removalsIn.do({|i| 
				inPortsActive[i]=false;
				patches.do({|p| var items; 	           // deactivate gui item
					if (p.portInGUI.notNil) { 
						items=p.portInGUI.items.copy;
						items[i]="("++midiSourceNames[i];
						if ((p.portInGUI.notNil) and: {p.portInGUI.isClosed.not}) { 
							p.portInGUI.items_(items)
						};
					};
				});
			});
		};
		 
		if (removalsOut.size>0) {
			removalsOut.do({|i|
				outPortsActive[i]=false;
				text=text++"     "++midiDestinationNames[i]++("\n");
				patches.do({|p| var items; 	           // deactivate gui item
					if (p.portOutGUI.notNil) {
						items=p.portOutGUI.items.copy;
						items[i]="("++midiDestinationNames[i];
						if ((p.portOutGUI.notNil) and: {p.portOutGUI.isClosed.not}) { 
							p.portOutGUI.items_(items)
						};
					};
				});	
			});
		};
		
		//////// ADDITIONS ////////////////////////////////////////
		
		/////// 1st reistate ins & outs that have been removed ////
		
		midiSourceUIDs.do({|uid, i|
			if (newSources.includes(uid)) {
				inPortsActive[i]=true;
				patches.do({|p| var items; 	           // activate gui item
					if (p.portInGUI.notNil) { 
						items=p.portInGUI.items.copy;
						items[i]=midiSourceNames[i];
						if ((p.portInGUI.notNil) and: {p.portInGUI.isClosed.not}) {
							p.portInGUI.items_(items)
						};
					};
					if (p.previousInUID==uid) {	p.setInPort(i); }
				});
			};
		});
		
		midiDestinationUIDs.do({|uid, i|
			if (newDestinations.includes(uid)) {
				outPortsActive[i]=true;
				patches.do({|p| var items; 	           // activate gui item
					if (p.portOutGUI.notNil) { 
						items=p.portOutGUI.items.copy;
						items[i]=midiDestinationNames[i];
						if ((p.portOutGUI.notNil) and: {p.portOutGUI.isClosed.not}) {
							p.portOutGUI.items_(items)
						};
					};
					if (p.previousOutUID==uid) { p.setOutPort(i); }
				});
			};
		});
		
		///////// and then add any new devices /////////////////////

		newSources.do({|u,i| var m;
			//[u,i].postln;
			if( midiSourceUIDs.includes(u).not) {
				if ((u>0)&&(u<=argNoInternalBuses)) {
					midiSourceUIDs=midiSourceUIDs.add(u);
					midiSourceNames=midiSourceNames.add("Internal: Bus"+((u).asString));
				}{
					m=MIDIClient.sources[i- 1];
					midiSourceUIDs=midiSourceUIDs.add(m.uid);
					m=m.asString;
					m=m[13..(m.size -2)].replace("\"","","\"");
					m=m.split($,).drop(0);
					if (m[0]=="IAC Driver") {
						m="IAC: "++(m[1].drop(1));
					}{
						if (m[0]=="RemoteSL IN") {
							m="Remote SL: "++(m[1].drop(1));
						}{
							m=m[1].drop(1);
						}
					};
					midiSourceNames=midiSourceNames.add(m);
					MIDIIn.connect(noInPorts, u);
				};
				inPortsActive=inPortsActive.add(true);
				noInPorts=noInPorts+1;
				patches.do({|p| var items; 	           // activate gui item
					if ((p.portInGUI.notNil) and: {p.window.isClosed.not}) { 
						items=p.portInGUI.items.copy;
						items=items.add(midiSourceNames[i]);
						p.portInGUI.items_(items);
					};
				});
				
			}
		});
	
		newDestinations.do({|u,i| var m;
			if( midiDestinationUIDs.includes(u).not) {
				if ((u>0)&&(u<=argNoInternalBuses)) {
					midiDestinationUIDs=midiDestinationUIDs.add(u);
					midiDestinationNames=midiDestinationNames.add
						("Internal: Bus"+((u).asString));
				}{
					m=MIDIClient.destinations[i- 1];
					midiDestinationUIDs=midiDestinationUIDs.add(m.uid);
					m=m.asString;
					m=m[13..(m.size -2)].replace("\"","","\"");
					m=m.split($,).drop(0);
					if (m[0]=="IAC Driver") {
						m="IAC: "++(m[1].drop(1));
					}{
						m=m[1].drop(1);
					};
					midiDestinationNames=midiDestinationNames.add(m);
					outs=outs.add( MIDIOut(noOutPorts, u));
				};
				outPortsActive=outPortsActive.add(true);
				noOutPorts=noOutPorts+1;
				patches.do({|p| var items; 	           // activate gui item
					if ((p.portOutGUI.notNil) and: {p.window.isClosed.not}) { 
						items=p.portOutGUI.items.copy;
						items=items.add(midiDestinationNames[i]);
						p.portOutGUI.items_(items);
					};
				});
			}
		});
	
		noInternalBuses=argNoInternalBuses;
	
	}
	
//////////////// GUI//////////////////////////////

	createInGUIA{arg argWindow, xy;

		var x, y, menuItems;
			
		xy=xy?(0@0);
		x=xy.x;
		y=xy.y;
		window=argWindow;
		menuItems=[];
		midiSourceNames.do({|n,j|
			menuItems=menuItems.add( (inPortsActive[j]==true).if(n,"("++n) );
		});
							
		portInGUI=MVC_PopUpMenu3(window,Rect(x+25,y+2,150,17))
			.items_(menuItems)
			.color_(\background,Color.ndcMenuBG)
			.canFocus_(false)
			.action_{|me| 
				//panicFunc ??
				this.setInPort(me.value);
				action.value(this);
			}
			.value_(midiIn)
			.canFocus_(false)
			.font_(Font("Arial", 10));
	
	}
	
	createInMVUA{arg argWindow, xy, test, background;

		var x, y, menuItems;
			
		background = background ? Color.ndcMenuBG; 
			
		xy=xy?(0@0);
		x=xy.x;
		y=xy.y;
		window=argWindow;
		menuItems=[];
		midiSourceNames.do({|n,j|
			menuItems=menuItems.add( (inPortsActive[j]==true).if(n,"("++n) );
		});
							
		portInGUI=MVC_PopUpMenu3(window,Rect(x,y,144,17))
			.items_(menuItems)
			.color_(\background,background)
			.canFocus_(false)
			.action_{|me| 
				//panicFunc ??
				this.setInPort(me.value);
				action.value(this);
			}
			.value_(midiIn)
			.canFocus_(false)
			.font_(Font("Arial", 10));
	
	}
	
	createInGUIB{arg argWindow, xy;

		var x, y, menuItems;
			
		xy=xy?(0@0);
		x=xy.x;
		y=xy.y;
		window=argWindow;
							
		channelInGUI=MVC_PopUpMenu3(window,Rect(x+25,y+2,97-40-4,17))
			.items_(["All","Ch. 1","Ch. 2","Ch. 3"
					 ,"Ch. 4","Ch. 5","Ch. 6","Ch. 7","Ch. 8"
		               ,"Ch. 9","Ch. 10","Ch. 11","Ch. 12","Ch. 13"
		               ,"Ch. 14","Ch. 15","Ch. 16"])
			.color_(\background,Color.ndcMenuBG)
			.canFocus_(false)
			.action_{|me|
				midiInChannel=me.value - 1;
				action.value(this);
			}
			.value_(midiInChannel+1)
			.canFocus_(false)
			.font_(Font("Arial", 10));

	}
	
	createInMVUB{arg argWindow, xy, test, background;

		var x, y, menuItems;
			
		xy=xy?(0@0);
		x=xy.x;
		y=xy.y;
		window=argWindow;
						
		background = background ? Color.ndcMenuBG;
							
		channelInGUI=MVC_PopUpMenu3(window,Rect(x,y,97-40-4,17))
			.items_(["All","Ch. 1","Ch. 2","Ch. 3"
					 ,"Ch. 4","Ch. 5","Ch. 6","Ch. 7","Ch. 8"
		               ,"Ch. 9","Ch. 10","Ch. 11","Ch. 12","Ch. 13"
		               ,"Ch. 14","Ch. 15","Ch. 16"])
			.color_(\background,background)
			.canFocus_(false)
			.action_{|me|
				midiInChannel=me.value - 1;
				action.value(this);
			}
			.value_(midiInChannel+1)
			.canFocus_(false)
			.font_(Font("Arial", 10));

	}
	
	createInGUI{arg argWindow, xy, label=true, includeInternal=true;

		var x, y, menuItems;
			
		xy=xy?(0@0);
		x=xy.x;
		y=xy.y;
		window=argWindow;
		menuItems=[];
		midiSourceNames.do({|n,j|
			if ((midiSourceUIDs[j]>0) and: {midiSourceUIDs[j]<=noInternalBuses}) {
				menuItems=menuItems.add("("++n);
			}{
				menuItems=menuItems.add( (inPortsActive[j]==true).if(n,"("++n) );
			};
		});

		if (label==true) {
			StaticText.new(window,Rect(x, y, 30, 20))
				.string_("   In")
				.stringColor_(Color.white);
		};
							
		portInGUI=MVC_PopUpMenu3(window,Rect(x+25,y+2,220-35-35-6,17))
			.items_(menuItems)
			.color_(\background,Color.ndcMenuBG)
			.canFocus_(false)
			.action_{|me| 
				//panicFunc ??
				this.setInPort(me.value);
				action.value(this);
			}
			.value_(midiIn)
			.canFocus_(false)
			.font_(Font("Arial", 10));

		channelInGUI=MVC_PopUpMenu3(window,Rect(x+25+40+5+4-3,y+2+20,97-40-4,17))
			.items_(["All","Ch. 1","Ch. 2","Ch. 3"
					 ,"Ch. 4","Ch. 5","Ch. 6","Ch. 7","Ch. 8"
		               ,"Ch. 9","Ch. 10","Ch. 11","Ch. 12","Ch. 13"
		               ,"Ch. 14","Ch. 15","Ch. 16"])
			.color_(\background,Color.ndcMenuBG)
			.canFocus_(false)
			.action_{|me|
				midiInChannel=me.value - 1;
				action.value(this);
			}
			.value_(midiInChannel+1)
			.canFocus_(false)
			.font_(Font("Arial", 10));

	}
	
	createOutGUI{arg argWindow, xy, label=true;

		var x, y, menuItems;	
		x=xy.x;
		y=xy.y;

		window=argWindow;

		menuItems=[];
		midiDestinationNames.do({|n,j|
			menuItems=menuItems.add( (outPortsActive[j]==true).if(n,"("++n) );
		});

		if (label==true) {
			StaticText.new(window,Rect(x, y, 30, 20)).string_("Out").stringColor_(Color.white);
		};
					
		portOutGUI=MVC_PopUpMenu3(window,Rect(x+25,y+2,220-35-35,17))
			.items_(midiDestinationNames)
			.color_(\background,Color.ndcMenuBG)
			.canFocus_(false)
			.action_{|me| 
				this.setOutPort(me.value);
				action.value(this);
			}	
			.value_(midiOut)
			.font_(Font("Arial", 10));
	
		channelOutGUI=MVC_PopUpMenu3(window,Rect(x+250-70,y+2,50,17))
			.items_(["Ch. 1","Ch. 2","Ch. 3"
					 ,"Ch. 4","Ch. 5","Ch. 6","Ch. 7","Ch. 8"
		               ,"Ch. 9","Ch. 10","Ch. 11","Ch. 12","Ch. 13"
		               ,"Ch. 14","Ch. 15","Ch. 16"])
			.color_(\background,Color.ndcMenuBG)
			.canFocus_(false)
			.action_{|me|
				midiOutChannel=me.value;
				action.value(this);
			}
			.value_(midiOutChannel)
			.font_(Font("Arial", 10));

	}
	
	createOutGUIA{arg argWindow, xy, label=true, w=150;

		var x, y, menuItems;	
		x=xy.x;
		y=xy.y;

		window=argWindow;

		menuItems=[];
		midiDestinationNames.do({|n,j|
			menuItems=menuItems.add( (outPortsActive[j]==true).if(n,"("++n) );
		});

		if (label==true) {
			StaticText.new(window,Rect(x-25, y-2, 30, 20))
				.string_("Out")
				.stringColor_(Color.white);
		};
					
		portOutGUI=MVC_PopUpMenu3(window,Rect(x,y,w,17))
			.items_(midiDestinationNames)
			.color_(\background,Color.ndcMenuBG)
			.canFocus_(false)
			.action_{|me| 
				this.setOutPort(me.value);
				action.value(this);
			}	
			.value_(midiOut)
			.font_(Font("Arial", 10));
	
		}
	
	// **********
	
	createOutMVUA{arg argWindow, xy,label=true, w=144, background;

		var x, y, menuItems;
			
		background = background ? Color.ndcMenuBG; 
			
		xy=xy?(0@0);
		x=xy.x;
		y=xy.y;
		window=argWindow;
		menuItems=[];
		midiDestinationNames.do({|n,j|
			[n, j].postln;
			menuItems=menuItems.add( (outPortsActive[j]==true).if(n,"("++n) );
		});
							
		portOutGUI=MVC_PopUpMenu3(window,Rect(x,y,w,17))
			.items_(midiDestinationNames)
			.color_(\background,background)
			.canFocus_(false)
			.action_{|me| 
				//panicFunc ??
				this.setOutPort(me.value);
				action.value(this);
			}
			.value_(midiOut)
			.canFocus_(false)
			.font_(Font("Arial", 10));
	
	}
	
	createOutGUIB{arg argWindow, xy, label=true;

		var x, y, menuItems;	
		x=xy.x;
		y=xy.y;

		window=argWindow;

		channelOutGUI=MVC_PopUpMenu3(window,Rect(x,y,50,17))
			.items_(["Ch. 1","Ch. 2","Ch. 3"
					 ,"Ch. 4","Ch. 5","Ch. 6","Ch. 7","Ch. 8"
		               ,"Ch. 9","Ch. 10","Ch. 11","Ch. 12","Ch. 13"
		               ,"Ch. 14","Ch. 15","Ch. 16"])
			.color_(\background,Color.ndcMenuBG)
			.canFocus_(false)
			.action_{|me|
				midiOutChannel=me.value;
				action.value(this);
			}
			.value_(midiOutChannel)
			.font_(Font("Arial", 10));

	}
	
	createOutMVUB{arg argWindow, xy, background;

		var x, y, menuItems;
			
		background = background ? Color.ndcMenuBG; 	
		
		xy=xy?(0@0);
		x=xy.x;
		y=xy.y;
		window=argWindow;
							
		channelOutGUI=MVC_PopUpMenu3(window,Rect(x,y,53,17))
			.items_(["Ch. 1","Ch. 2","Ch. 3"
					 ,"Ch. 4","Ch. 5","Ch. 6","Ch. 7","Ch. 8"
		               ,"Ch. 9","Ch. 10","Ch. 11","Ch. 12","Ch. 13"
		               ,"Ch. 14","Ch. 15","Ch. 16"])
			.color_(\background,background)
			.canFocus_(false)
			.action_{|me|
				midiOutChannel=me.value;
				action.value(this);
			}
			.value_(midiOutChannel)
			.canFocus_(false)
			.font_(Font("Arial", 10));

	}
	
	refreshGUI{

		if ((portInGUI.notNil)     and: { portInGUI.isClosed.not}) { 
			portInGUI.value=midiIn
		};
		if ((channelInGUI.notNil)  and: { channelInGUI.isClosed.not}) {
			channelInGUI.value=midiInChannel + 1
		};
		if ((portOutGUI.notNil)    and: { portOutGUI.isClosed.not}) {
			portOutGUI.value=midiOut
		};
		if ((channelOutGUI.notNil) and: { channelOutGUI.isClosed.not}) {
			channelOutGUI.value=midiOutChannel
		};

	}
	
} ////////////////////////////// end.LNX_MIDIPatch Object /////////////////////////////

// used as a fake in & out device for the "None" midi device
// and it is used as if it was an instrance rather than an object
// any functionality added to LNX_MIDIPatch that is called by other objects also need
// to be added here

NoMIDI {
	
	*latency_ {}
	*noteOn   {}
	*noteOff  {}
	*control  {}
	*bend     {}
	*touch    {}
	*program  {}
	*songPtr  {}
	*start    {}
	*stop     {}
	*midiClock{}
	*continue {}
	*noteOnLatency {}
	*noteOffLatency{}
	*controlLatency{}
	*programLatency{}
	touchLatency   {}
	bendLatency    {}
	*songPtrLatency{}
	*startLatency  {}
	*stopLatency   {}
	*continueLatency {}
	*midiClockLatency{}
	*endPoint{}
	*device  {}
	*name    {}
	*uid     {}
	*isSameDevice{|endPoint| ^(endPoint === this) }
	*isSameName{|endPoint| ^(endPoint === this) }
	*isSameDeviceAndName{|endPoint| ^(endPoint === this) } 
	*isSameEndPoint{|endPoint|^(endPoint === this)}
	
} /////////////////////////////// end.NoMIDI Patch ///////////////////////////////////////

