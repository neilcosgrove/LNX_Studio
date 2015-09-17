// wslib 2005

// fast GUI creation for SynthDef / Function
// analyses the controlnames and generates a control window for all parameters
// * non-zero parameter values can be scaled to arg paramScale (default 4)
// * Integers will be scaled as integers (stepsize=1), floats as floats
// * positive parameters can not be set to zero, only to (default * (1/paramScale))
// * zeros can be scaled to zeroScale
// * zeros with minus signs (-0.0 and -0) can be scaled between -zeroScale and zeroScale
// * integer 1 can be used as switch (0/1)
//
// Routine-fastgui:
// creates a small window with play/stop, reset, next and Clock controls



+ SynthDef {
	fastgui {arg sent = false, paramScale = 4, zeroScale = 1;
var win, lay, slider, synth, bus, tgt, addAct, fxOn, moveSynth, nodeLabel, help, synthParams; 
var name, param;
name = this.name;
param = [
	this.allControlNames.collect(_.name),
	this.allControlNames.collect(_.name),
	this.allControlNames.collect({ |item|
		var default;
		default = item.defaultValue;
		//if(default.isNil) { default=0.0 };
		case
			{default == 0}
				{case
					{default.isInteger and: (default === 0)}
						{[0, 1, \linear, 1].asSpec;}
					{default.isInteger and: (default === 0).not} // i.e. -0
						{[zeroScale.neg, zeroScale, \linear, 1].asSpec;}
					{default.isFloat and: (default === 0.0)}
						{[0, zeroScale, \linear, 0].asSpec;}
					{default.isFloat and: (default === 0.0).not} // i.e. -0.0
						{[zeroScale.neg, zeroScale, \linear, 0].asSpec;}
				}
			{(default === 1)}
				{[0, 1, \linear, 1].asSpec;}
			{default.isInteger and: default.isNegative} 
				{[default * paramScale, default.neg * paramScale, \linear, 1].asSpec;}
			{default.isInteger}
				{[default * (1/paramScale), default * paramScale, \exponential, 1].asSpec;}
			{default.isFloat and: default.isNegative} 
				{[default * paramScale, default.neg * paramScale, \linear, 0].asSpec;}
			{default.isFloat}
				{[default * (1/paramScale), default * paramScale, \exponential, 0].asSpec;}
			{true}
				{[default * (1/paramScale), default * paramScale, \linear, 0].asSpec;};
			}),
									
	this.allControlNames.collect(_.defaultValue)];
if(sent.not) {this.sendAlways(Server.default)};
tgt = 1; 
bus = 0; 
addAct = \addToTail; 
fxOn = false; 
slider = Array.newClear(param[0].size); 
win = Window(" fastgui: " ++ name, Rect(10 + 20.rand, 500 + 20.rand, 300, (param[0].size * 20) + 50), false); 
win.view.decorator = lay = FlowLayout(win.view.bounds, 5@5, 5@5); 

StaticText(win, 50 @ 15).font_(Font("Helvetica", 9)).string_("Bus").align_(\right); 
SCNumberBox(win, 30 @ 15).font_(Font("Helvetica", 9)).value_(bus).action_({|val| 
   	val.value = 0.max(val.value);
	bus = val.value; 
  	if (fxOn, { synth.set(\outbus,bus) }); 
}); 
StaticText(win, 15 @ 15).font_(Font("Helvetica", 9)).string_("Tgt").align_(\right); 
SCNumberBox(win, 40 @ 15).font_(Font("Helvetica", 9)).value_(tgt).action_({|val| 
	val.value = 0.max(val.value); 
	tgt = val.value.asInteger; 
		moveSynth.value; 
}); 

SCPopUpMenu(win, 60@15)
	.font_(Font("Helvetica", 9))
	.items_(["addToHead", "addToTail", "addAfter", "addBefore"])
	.value_(1)
	.action_({|val|
		addAct = val.items.at(val.value).asSymbol;
		moveSynth.value; 
	}); 
	 
Button(win,18@15)
	.font_(Font("Helvetica", 9))
	.states_([["#"]])
	.action_({|val| 
			synthParams = Array.new; 
			synthParams = synthParams.add(param[1]); // synth params 
			synthParams = synthParams.add(param[3]); // argument values 
			synthParams = ['outbus', bus].add(synthParams.flop).flat.asCompileString; 
			("Synth.new(\\" ++ this.name ++ ", " ++ synthParams ++ 
				", target: " ++ tgt ++ ", addAction: \\" ++ addAct ++ ")").postln; 
	}); 

Button(win,40@15)
	.font_(Font("Helvetica", 9))
	.states_([["On"],["On", Color.black, Color.red(alpha:0.2)]])
	.action_({|val| 
		if ( val.value == 0, { 
			fxOn = false; 
			nodeLabel.string = "none"; 
		if(this.canReleaseSynth)
			{synth.release}
		  		{synth.free};
		},{ 
			fxOn = true; 
			synthParams = Array.new; 
			synthParams = synthParams.add(param[1]); // synth params 
			synthParams = synthParams.add(param[3]); // argument values 
			synthParams = ['outbus', bus].add(synthParams.flop).flat; 
			synth = Synth.new(this.name, synthParams, target: tgt.asTarget, addAction: addAct); 
			nodeLabel.string = synth.nodeID; 
		if(this.canFreeSynth && this.hasGateControl.not)
			{Routine({
				0.1.wait;
				val.value = 0; 
				0.5.wait;
				fxOn = false; 
						nodeLabel.string = "none";
				}).play(AppClock)  };
		 }) 
	}); 

param[0].size.do({|i| 
	slider[i] = EZSlider(win, 270@15, param[0][i], param[2][i], labelWidth: 50, numberWidth: 40); 
	slider[i].labelView.font_(Font("Helvetica", 9)); 
	slider[i].numberView.font_(Font("Helvetica", 9)); 
	slider[i].sliderView.background_(Gradient(Color.blue(alpha:0.0), 
		Color.blue(alpha:0.2), \h, 20)); 
	slider[i].action = {|v| 
		param[3][i] = v.value; 
		if (fxOn, { synth.set(param[1][i], v.value) }) 
	 }; 
	slider[i].value = param[3][i]; 
	lay.nextLine; 
}); 
StaticText(win,50 @ 15).font_(Font("Helvetica", 9)).align_(\right).string_("nodeID"); 
nodeLabel = StaticText(win,50 @ 15).font_(Font("Helvetica", 9)).align_(\left).string_("none"); 
moveSynth = { 
	if ( fxOn, { 
		case 
			{ addAct === \addToHead }  { synth.moveToHead(tgt.asTarget) } 
			{ addAct === \addToTail }  { synth.moveToTail(tgt.asTarget) } 
			{ addAct === \addAfter  }  { synth.moveAfter(tgt.asTarget)  } 
			{ addAct === \addBefore }  { synth.moveBefore(tgt.asTarget) } 
	}) 
}; 
win.view.keyDownAction = { arg ascii, char; 
	case 
		{char === $n} { Server.default.queryAllNodes } 
}; 
win.onClose_({ if (fxOn, { synth.free }) }); 
win.front;

}

playAlways { arg target, outbus = 0, fadeTime=0.02, addAction=\addToHead;
		// improved version of .play method derrived from Function-play
		var def, synth, server, bytes, synthMsg;
		target = target.asTarget;
		server = target.server;
		if(server.serverRunning.not) { 
			("server '" ++ server.name ++ "' not running.").warn; ^nil
		};
		def = this;  // the only thing changed
		synth = Synth.basicNew(def.name,server);
		bytes = def.asBytes;
		synthMsg = synth.newMsg(target, [\i_out, outbus, \out, outbus], addAction);
		if(bytes.size > 8192) {
			def.load(server, synthMsg);
		} {
			server.sendMsg("/d_recv", bytes, synthMsg)
		};
		^synth
	}
	
sendAlways {arg target, outbus = 0, fadeTime=0.02, addAction=\addToHead;
		// improved version of .send, uses .load when needed
		var def, server, bytes, synthMsg;
		target = target.asTarget;
		server = target.server;
		if(server.serverRunning.not) { 
			("server '" ++ server.name ++ "' not running.").warn; ^nil
		};
		def = this;  // the only thing changed
		bytes = def.asBytes;
		
		if(bytes.size > 8192) {
			def.load(server);
		} {
			server.sendMsg("/d_recv", bytes)
		};
		^this
}

}

+ Function {
	fastgui {
		arg paramScale = 4, zeroScale = 1;
		^this.asSynthDef.fastgui(false, paramScale, zeroScale);
		}
}

+ Routine {
	fastgui { arg name = "routine";
		var win, controls, clock, isPlaying = false, tempoClock;
		win = Window(" fastgui: " ++ name, Rect(10 + 20.rand, 500 + 20.rand, 180, 60), false);
		win.front;
		tempoClock = TempoClock.default;
		clock = SCPopUpMenu(win, Rect(80, 10, 80,18))
			.items_(["TempoClock","AppClock","SystemClock"]);
		controls = (
		play: Button(win, Rect(10,10,65,18) )
			.states_([["play"],["stop", Color.black, Color.red]])
			.action_({ |button| case 
				{button.value == 1}
					{ 	this.reset;
						this.play([tempoClock, AppClock, SystemClock][clock.value]);
						isPlaying = true; }
				{button.value == 0}
					{ this.stop; isPlaying = false; }
				}),
		reset: Button(win, Rect(10,30,65,18) ).states_([["reset"]])
			.action_({ this.reset }),
		next: Button(win, Rect(80,30,80,18) ).states_([["next >>"]])
			.action_({ this.next })
		/* tempo: SCSlider(win, Rect(10,30,160,18)).value_(0.5)
			.action({ |slider| tempoClock.tempo = slider.value.rangeExp(0.25,4) }) */
		);
		win.onClose = { this.stop }; 
	}
}


 
