////////////////////////////////////////////////////////////////////////////////////////////////////
// SC code instrument                                                                              .
// also MVC_Model ?

LNX_SynthDefID {
	classvar <id=1000;
	*initClass { id = 1000; }
	*next  { ^id = id + 1; }
}

LNX_SynthDefControl : ControlName {
	
	var <>type, <>controlSpec;

	*new { arg name, index, rate, defaultValue, argNum, lag, type, controlSpec ;
		^super.newCopyArgs(name.asSymbol, index, rate, defaultValue, argNum, lag ? 0.0, type, controlSpec)
	}
	
	printOn { arg stream;
		stream << "LNX_SynthDefControl  P " << index.asString;
		if (name.notNil) { stream << " " << name; };
		if (rate.notNil) { stream << " " << rate; };
		if (defaultValue.notNil) { stream << " " << defaultValue; };
		if (argNum.notNil) { stream << " " << argNum; };
		if (lag.notNil) { stream << " " << lag; };
		if (type.notNil) { stream << " " << type; };
		if (controlSpec.notNil) { stream << " " << controlSpec; };
		//stream << "\n"
	}

}
//
//+ UGen {
//	poll {|trigid = -1, trig = 10, label|
//          ^Poll(trig, this, label, trigid)
//	}
//}
//
//+ Array {
//	poll {|trigid = -1,trig = 10, label|
//		if(label.isNil){ label = this.size.collect{|index| "UGen Array [%]".format(index) } };
//		^Poll(trig, this, label, trigid)
//	}
//}

+ ControlName {
	
	asSynthDefControl{|type,spec|
		^LNX_SynthDefControl(name, index, rate, defaultValue, argNum, lag, type, spec)
	}
	
}

+ LNX_Code {

	code{^codeModel.string}

	initCode{

		codeModel =
"
// A simple SynthDef template

SynthDef(\"LNX_Saw1\", {|out, amp, vel, freq, midi, pan, gate=1, sendOut, sendAmp, bpm, clock,
	i_clock, poll, bufL, bufR, bufRate, bufAmp, bufStartFrame, bufStartPos, bufLoop, bufDur
	filtFreq=1800, q=0.5, dur=1|
	
	var signal;
	
	signal = Saw.ar(midi.midicps, 0.33);
	signal = DFM1.ar(signal, filtFreq * vel, q);
	signal = signal * EnvGen.ar(Env.new([1,1,0], [0,2], [-1,-1], 1), gate, vel, 0, dur, 2);
		
	LNX_Out(signal, out, amp, pan, sendOut, sendAmp);
		
}, metadata: ( specs: ( filtFreq: \\freq, q: \\unipolar )))

//signal.poll(poll); // use for debugging
//PlayBuf.ar(1,[bufL,bufR], BufRateScale.kr([bufL,bufR])*bufRate, 0, bufStartFrame, bufLoop)*bufAmp;
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
				"¥ ERROR: Parse error in code.".error
			};
			this.warnFromError("¥ ERROR: Parse error in code.");
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
	
