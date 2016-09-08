
+ LNX_CodeFX {

	code{^codeModel.string}

	initCode{

		codeModel =
"// A simple FX template

SynthDef(\"LNX_CodeFX\", {|on, out, amp, in, inAmp, sendOut, sendAmp, poll, filtFreq=1800, q=0.5|

	var signal, signalIn = In.ar(in, 2) * inAmp;

	signal = DFM1.ar(signalIn, filtFreq, q);

	LNX_FXOut(signal,signalIn,on,out,amp,0,sendOut,sendAmp);

}, metadata: ( specs: ( filtFreq: \\freq, q: \\unipolar )))
"
		.asModel;

	}

	// Build & Exeception errors

	clearError{
		errorModel.dependantsPerform(\color_,\string,Color.black);
		errorModel.string_("");

	}

	successError{
		errorModel.dependantsPerform(\color_,\string,Color.black);
		errorModel.string_((p[14]==1).if("[Auto] Build successful.","Build successful."));

	}

	warnFromError{|error|
		errorModel.dependantsPerform(\color_,\string,Color.red);
		errorModel.string_(error);
	}

	warnNotSynthDef{|def|
		if (def.isNil) {
			if (LNX_Studio.isStandalone.not) {
				"ERROR: Parse error in code.".error
			};
			this.warnFromError("ERROR: Parse error in code.");
		}{
			if (LNX_Studio.isStandalone.not) {
				"ERROR: Recieved: "++(def.asString)++", is not a SynthDef.".error;
			};
			this.warnFromError(
				"ERROR: Recieved: "++(def.asString)++", is not a SynthDef."
			);
		}
	}

	warnMissingArgs{|missingArgs|
		("ERROR: The following compulsory arguments are missing: "++(missingArgs.asString)).error;
		this.warnFromError(
			"ERROR: The following compulsory arguments are missing: "++(missingArgs.asString));
	}

	warnSpec{|spec|
		("The spec: "++(spec.asString)++" is not defined.").error;
		this.warnFromError("The spec: "++(spec.asString)++" is not defined.");
	}

	warnNoRelease{
		("ERROR: This synth cannot be released.").error;
		this.warnFromError("ERROR: This synth cannot be released.");
	}

	warnKeyword{|word|
		("ERROR: Cannot use the keyword: "++word).error;
		this.warnFromError("ERROR: Cannot use the keyword: "++word);
	}

	postPoll{|string|
		{
			errorModel.dependantsPerform(\color_,\string,Color.black);
			errorModel.string_(string.asString);
		}.defer;

	}

} // end ////////////////////////////////////

