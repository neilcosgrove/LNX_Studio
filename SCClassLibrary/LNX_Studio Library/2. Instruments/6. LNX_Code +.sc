////////////////////////////////////////////////////////////////////////////////////////////////////
// SC code instrument                                                                              .

Protect{
	*new{|signal,clip=2|
		^Select.ar(CheckBadValues.ar(signal, 0, 0)>0, [signal,DC.ar(0)]).clip2(clip);
	}

	*newNoClip{|signal,clip=2|
		^Select.ar(CheckBadValues.ar(signal, 0, 0)>0, [signal,DC.ar(0)]);
	}

}

// easy stereo output for mono, stereo or multichannel signals to out channel
// & effect send with pan pos
// options in protect, mix down multichannel and scale multichannel volume

// old way for fx's
LNX_OldOut{
	*new{|signal,out,amp,pan=0,sendOut,sendAmp,protect=true,mix=true,scaleVolume=true,leak=false|
		var sigOut = signal;
		var size = signal.size;
		var leftPan, rightPan;

		// mix it
		if (mix) {
			sigOut = sigOut.oddEvenMix; // mixes to a stereo pair
			// and scale volume
			if((scaleVolume)&&(size>2)) {
				if (amp.isNumber) {
					amp=amp/(size/2);	// if amp is number adjust amp
				}{
					sigOut=sigOut/(size/2); // else scale now
				};
			};
		};
		// reduce to an OutputProxy or [ an OutputProxy, an OutputProxy ]
		case {sigOut.size==1} {
			sigOut=sigOut[0];
		}
		{sigOut.size>2} {
			sigOut=sigOut[0..1];
		};
		// apply amp
		if (amp.notNil) { sigOut = sigOut * amp };
		// do protect
		if (protect) { sigOut = Protect(sigOut) }; // filters bad numbers
		// do leak
		if (leak) { sigOut = LeakDC.ar(sigOut) }; // leak any dc offset
		// if mono
		case {sigOut.size==0} {
			sigOut=Pan2.ar(sigOut, pan); // pan it
			OffsetOut.ar(out,sigOut);	 // output
			if ((sendOut.notNil) && (sendAmp.notNil)) {
				OffsetOut.ar(sendOut, sigOut * sendAmp );   // effect send
			};
		}{ // else stereo
			leftPan= (pan*2-1).clip(-1,1);   // left pos
			rightPan= (pan*2+1).clip(-1,1);  // right pos
			sigOut=LinPan2.ar(sigOut[0], leftPan) + LinPan2.ar(sigOut[1], rightPan); // pair
			OffsetOut.ar(out,sigOut);	 // output
			if ((sendOut.notNil) && (sendAmp.notNil)) {
				OffsetOut.ar(sendOut, sigOut * sendAmp );   // effect send
			};
		};
		^signal;	// return the signal, unchanged

	}
}

// old out for insts forcus no amp, pan or send
LNX_Out{
	*new{|signal,out,amp,pan=0,sendOut,sendAmp,protect=true,mix=true,scaleVolume=true,leak=false|
		var sigOut = signal;
		var size = signal.size;
		var leftPan, rightPan;

		// stop the following from working. This is now done by the mixer synthDef
		amp=1;
		pan=0;
		sendAmp=0;

		// mix it
		if (mix) {
			sigOut = sigOut.oddEvenMix; // mixes to a stereo pair
			// and scale volume
			if((scaleVolume)&&(size>2)) {
				if (amp.isNumber) {
					amp=amp/(size/2);	// if amp is number adjust amp
				}{
					sigOut=sigOut/(size/2); // else scale now
				};
			};
		};
		// reduce to an OutputProxy or [ an OutputProxy, an OutputProxy ]
		case {sigOut.size==1} {
			sigOut=sigOut[0];
		}
		{sigOut.size>2} {
			sigOut=sigOut[0..1];
		};
		// apply amp
		if (amp.notNil) { sigOut = sigOut * amp };       // check amp not nil
		// do protect
		if (protect)    { sigOut = Protect(sigOut) };    // filters bad numbers
		// do leak
		if (leak)       { sigOut = LeakDC.ar(sigOut) };  // leak any dc offset
		// if mono
		case {sigOut.size==0} {
			OffsetOut.ar(out,sigOut!2);	 // just output
		}{ // else stereo
			OffsetOut.ar(out,sigOut);	     // just output
		};
		^signal;	// return the signal, unchanged
	}
}

LNX_InstOut{
	*new{|signal,out,protect=true,mix=true,scaleVolume=true,leak=false|
		var sigOut = signal;
		var size = signal.size;
		var leftPan, rightPan;
		var amp=1;

		// mix it
		if (mix) {
			sigOut = sigOut.oddEvenMix; // mixes to a stereo pair
			// and scale volume
			if((scaleVolume)&&(size>2)) { amp=amp/(size/2) }; // if amp is number adjust amp
		};
		// reduce to an OutputProxy or [ an OutputProxy, an OutputProxy ]
		case {sigOut.size==1} {
			sigOut=sigOut[0];
		}
		{sigOut.size>2} {
			sigOut=sigOut[0..1];
		};
		// apply amp
		sigOut = sigOut * amp;
		// do protect
		if (protect) { sigOut = Protect(sigOut) };    // filters bad numbers
		// do leak
		if (leak) { sigOut = LeakDC.ar(sigOut) };  // leak any dc offset
		// if mono
		case {sigOut.size==0} {
			OffsetOut.ar(out,sigOut!2);	 // just output
		}{ // else stereo
			OffsetOut.ar(out,sigOut);	     // just output
		};
		^signal;	// return the signal, unchanged
	}
}


LNX_FXOut{
	*new{|signal, signalIn, on, out, amp, pan=0, sendOut, sendAmp, protect=true, mix=true,
										scaleVolume=true, leak=false|
		var sigOut = signal;
		var size = signal.size;
		var leftPan, rightPan;
		// mix it
		if (mix) {
			sigOut = sigOut.oddEvenMix; // mixes to a stereo pair
			// and scale volume
			if((scaleVolume)&&(size>2)) {
				if (amp.isNumber) {
					amp=amp/(size/2);	// if amp is number adjust amp
				}{
					sigOut=sigOut/(size/2); // else scale now
				};
			};
		};
		// reduce to an OutputProxy or [ an OutputProxy, an OutputProxy ]
		case {sigOut.size==1} {
			sigOut=sigOut[0];
		}
		{sigOut.size>2} {
			sigOut=sigOut[0..1];
		};
		// do protect
		if (protect) { sigOut = Protect(sigOut) };	// filters bad numbers
		// do leak
		if (leak) { sigOut = LeakDC.ar(sigOut) }; // leak any dc offset
		// if mono
		case {sigOut.size==0} {
			sigOut=Pan2.ar(sigOut, pan); // pan it
			sigOut = SelectX.ar(on.lag,[signalIn,sigOut]); // on
			if (amp.notNil) { sigOut = sigOut * amp }; // apply amp
			OffsetOut.ar(out,sigOut);	 // output
			if ((sendOut.notNil) && (sendAmp.notNil)) {
				OffsetOut.ar(sendOut, sigOut * sendAmp );   // effect send
			};
		}{ // else stereo
			leftPan= (pan*2-1).clip(-1,1);   // left pos
			rightPan= (pan*2+1).clip(-1,1);  // right pos
			sigOut=LinPan2.ar(sigOut[0], leftPan) + LinPan2.ar(sigOut[1], rightPan); // pair
			sigOut = SelectX.ar(on.lag,[signalIn,sigOut]); //on
			if (amp.notNil) { sigOut = sigOut * amp }; // apply amp
			OffsetOut.ar(out,sigOut);	 // output
			if ((sendOut.notNil) && (sendAmp.notNil)) {
				OffsetOut.ar(sendOut, sigOut * sendAmp );   // effect send
			};
		};
		^signal;	// return the signal, unchanged
	}
}

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
"// A simple SynthDef template

SynthDef(\"MySaw\",{|out, gate, midi, vel, filtFreq=2000, q=0.5, dur=0.5|

  var signal;

  signal = Saw.ar(midi.midicps, 0.33);
  signal = DFM1.ar(signal, filtFreq * vel, q);
  signal = signal * EnvGen.ar(Env.adsr(0,0,1,1), gate, vel, 0, dur,2);

  LNX_InstOut(signal, out);

}, metadata: ( specs: ( filtFreq: \\freq ) ) )
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

