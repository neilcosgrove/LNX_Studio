+ MIDIClient {
	
	// bug fix for mac. disabled primitive _DisposeMIDIClient so quitting doesn't cause crash
	// still can't recompile with MIDI devices attached
	*disposeClient {
		^this
//		_DisposeMIDIClient
//		^this.primitiveFailed
	}
	
}