/*
+ MIDIClient {
	*init { arg inports, outports; // by default initialize all available ports
								// you still must connect to them using MIDIIn.connect
		this.prInit(50,50);
		this.list;
		if(inports.isNil,{inports = sources.size});
		if(outports.isNil,{outports = destinations.size});
			// this.disposeClient;

		this.prInit(inports,outports);
		initialized = true;
		
		// might ask for 1 and get 2 if your device has it
		// or you might ask for a 1 and get 0 of nothing is plugged in
		// so we warn you
		if(sources.size < inports or: {destinations.size < outports},{
			"WARNING:".postln;
			("MIDIClient-init requested " ++ inports ++ " inport(s) and " ++ outports
				++ " outport(s),").postln;
			("but found only " ++ sources.size ++ " inport(s) and " ++ destinations.size
				++ " outport(s).").postln;
			"Some expected MIDI devices may not be available.".postln;
		});

		this.list;

		ShutDown.add { this.disposeClient };

		Post << "MIDI Sources:" << Char.nl;
		sources.do({ |x| Post << Char.tab << x << Char.nl });
		Post << "MIDI Destinations:" << Char.nl;
		destinations.do({ |x| Post << Char.tab << x << Char.nl });
	}
	
}
*/
