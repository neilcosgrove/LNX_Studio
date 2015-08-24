// wslib 2011

// EM =-= EnvironmentModel
// supports simple MVC structures for Environments
// whenever a value for a key is changed it calls .changed

/*

e = EM( \freq, 440, \amp, 0.1 );

(
// make a gui for it:
e.makeWindow = { |env|
	var win, cnt;
	win = Window( "e", Rect( 128 + 100.rand, 300 + 100.rand, 400, 52 )).front;
	win.addFlowLayout;
	
	cnt = SimpleController( env );
	win.onClose_({ cnt.remove }); // remove controller at window close
	
	env.keysValuesDo({ |key, value|
		var sl;
		if( value.isNumber ) {	
			sl = EZSmoothSlider( win, 380@20, key, key, { |sl| env[ key ] = sl.value } );
			cnt.put( key, { |ev, what, val| sl.value = ev[ key ] });
			env.changed( key ); // update the slider
		};
	});
	
	win;
};

e.makeWindow;
);

e.makeWindow; // add a second window

e.amp = 0.25; // slider jumps in both windows
e.freq = 220;

e[ \amp ] = 0.5; // slider jumps

e.use({ ~amp = 0.33 }); // slider doesn't jump unfortunately

// now move an amp slider yourself
e.amp.postln; // value changed

*/


EM : Environment {
	
	*new { arg ...pairs;
		^super.new(8, nil,nil, true).putPairs( pairs );
	}
	
	put { |key, value|
		super.put( key, value );
		this.changed( key, value );
	}
		
	putGet { |key, value|
		var res;
		res = super.putGet( key, value );
		this.changed( key, value );
	}
	
	// overwrite super methods
	stop { |...args|
		^this.doesNotUnderstand( \stop, *args );
	}
	
	release { |...args|
		^this.doesNotUnderstand( \release, *args );
	}
	
	doesNotUnderstand { arg selector ... args;
		var func;
		if (know) {

			func = this[selector];
			if (func.notNil) {
				^func.functionPerformList(\value, this, args);
			};

			if (selector.isSetter) {
				selector = selector.asGetter;
				if( [ \stop, \release ].includes( selector ).not && { this.respondsTo(selector) }) {
					warn(selector.asCompileString
						+ "exists a method name, so you can't use it as pseudo-method.")
				};
				^this[selector] = args[0];
			};
			func = this[\forward];
			if (func.notNil) {
				^func.functionPerformList(\value, this, selector, args);
			};
			^nil
		};
		^this.superPerformList(\doesNotUnderstand, selector, args);
	}
	
}