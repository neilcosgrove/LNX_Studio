
// still need to think how to do midi control of different number of channels over different users

MVC_AudioOutSpec : ControlSpec {

	*initClass {
		Class.initClassTree(Warp);
		Class.initClassTree(LNX_AudioDevices);
		specs = specs.addAll([
			\audioOut -> MVC_AudioOutSpec()
		])
	}

	*new { arg minval=0.0, maxval=1.0, warp='lin', step=0.0, default=0, units="";
		^super.new.init
	}

	storeArgs { ^[this.minval,this.maxval,warp.asSpecifier,step,default,units] }

	minval { ^(LNX_AudioDevices.numFXBusChannels/2).neg }
	maxval { ^LNX_AudioDevices.numOutputBusChannels/2 - 1 }
	clipLo { ^(LNX_AudioDevices.numFXBusChannels/2).neg }
	clipHi { ^LNX_AudioDevices.numOutputBusChannels/2 - 1 }

	init {
		this.updateArgs;
		warp = \lin.asWarp(this);
		step = 1;
		default = 0;
		units = "";
	}

	updateArgs{
		minval = this.minval;
		maxval = this.maxval;
		clipLo = minval;
		clipHi = maxval;
	}

	minval_ { }
	maxval_ { }
	step_{ }

	constrain { arg value; ^value.asFloat.clip(clipLo, clipHi).round(step) }

	range { ^this.maxval - this.minval }
	ratio { ^this.maxval / this.minval }

	map { arg value;
		// maps a value from [0..1] to spec range
		^warp.map(value.clip(0.0, 1.0)).round(step);
	}

	unmap { arg value;
		// maps a value from spec range to [0..1]
		^warp.unmap(value.round(step).clip(this.clipLo, this.clipHi));
	}


	map2 { arg value;
		// maps a value from model/Spec to menuIndex
		value=value.clip(this.clipLo, this.clipHi);
		^(value<0).if(this.maxval-value,value)

	}

	unmap2 { arg value;
		// maps a value from menuIndex to model/Spec
		^(value>this.maxval).if(this.maxval-value,value).clip(this.clipLo, this.clipHi);
	}

	guessNumberStep { ^1 }

	*newFrom { ^this.copy }

	copy { ^this.copy }

}

MVC_AudioOutMasterSpec : MVC_AudioOutSpec {

	*initClass {
		Class.initClassTree(Warp);
		specs = specs.addAll([
			\audioOutMaster -> MVC_AudioOutMasterSpec()
		])
	}

	maxval { ^LNX_AudioDevices.numOutputBusChannels/2 }
	clipHi { ^LNX_AudioDevices.numOutputBusChannels/2 }

}

MVC_AudioInSpec : MVC_AudioOutSpec{

	*initClass {
		Class.initClassTree(Warp);
		Class.initClassTree(LNX_AudioDevices);
		specs = specs.addAll([
			\audioIn -> MVC_AudioInSpec()
		])
	}

	maxval { ^LNX_AudioDevices.numInputBusChannels/2 - 1 }
	clipHi { ^LNX_AudioDevices.numInputBusChannels/2 - 1 }

}


/* Testing

q=\audioOut.asSpec;
q
q.map(0);
q.map(1);
q.unmap2(10);
q.map2(1);
(-20..20).do{|i| q.unmap2(q.map2(i)).postln};
q.isKindOf(MVC_AudioOutSpec);

q=\audioIn.asSpec;
q
q.map(0);
q.map(1);
q.unmap2(10);
q.map2(1);
(-20..20).do{|i| q.unmap2(q.map2(i)).postln};
q.isKindOf(MVC_AudioOutSpec);

*/

