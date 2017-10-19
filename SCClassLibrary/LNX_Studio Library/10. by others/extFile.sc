+SoundFile {
	extractMarkers {
		switch (headerFormat)
		{"AIFF"} { ^this.extractMarkersAiff() }
		{"WAV"} { ^this.extractMarkersWav() };
	}

	extractMarkersWav {
		var cueRe, markerList, cueStrings, headerString;
		cueRe = "Cue ID :\\s*(\\d+)\\s*Pos :\\s*(\\d+)";
		headerString = this.readHeaderAsString;
		cueStrings = headerString.findRegexp(cueRe);
		if((cueStrings.size % 3) == 0, {
			^cueStrings.clump(3).collect({
				| cue |
				var label, id = cue[1][1];
				label = headerString.findRegexp("labl :\\s*" ++ id.asString ++ " : (.*?)\n");
				label = (label.size == 2).if({ label[1][1] }, id);

				(
					\position: cue[2][1].asInteger,
					\label: label
				)
			})
		},{
			"Unexpected result searching for cue markers".warn;
			this.readHeaderAsString.postln;
		});
		^[]
	}

	extractMarkersAiff {
		var cueRe, markerList, cueStrings, headerString;
		cueRe = "(Mark ID\\s*:\\s*([0-9]+).*?Position\\s*:\\s*([0-9]+).*?Name\\s*:\\s*(.*?)\n)";
		headerString = this.readHeaderAsString;
		cueStrings = headerString.findRegexp(cueRe);
		if((cueStrings.size % 5) == 0, {
			^cueStrings.clump(5).collect({
				| cue |
				var label, position;
				label = cue[4][1];
				position = cue[3][1];
				(
					\position: position.asInteger,
					\label: label
				)
			})
		},{
			"Unexpected result searching for cue markers".warn;
			this.readHeaderAsString.postln;
		});
		^[]
	}

}