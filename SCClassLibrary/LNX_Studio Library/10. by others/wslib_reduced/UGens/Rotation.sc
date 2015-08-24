// Rotation of input arrays (RotateL, RotateN, XFadeRotate)
// and crossfading Select (XFadeSelect)
// wslib 2005

RotateN : Panner { // rotate an array of channels
	*ar { arg n = 0, in;
		^Array.fill(in.size,
			{ |i| Select.ar((i + n)%in.size, in); })
	}
	
	*kr { arg n = 0, in;
		^Array.fill(in.size,
			{ |i| Select.kr((i + n)%in.size, in); })
	}
}

RotateL : Panner { // rotate an array of channels with interpolation (linear crossfade)
	*ar { arg n = 0, in;
		/* if(amt.rate = 'control')
			{amt = K2A.ar(n); }; */
		^Array.fill(in.size,
			{ |i| SelectL.ar((i + n)%in.size, in); });
	} 
	
	
	*kr { arg n = 0, in;
		^Array.fill(in.size,
			{ |i| SelectL.kr((i + n)%in.size, in); })
	}
	
}

XFadeRotate : Panner{ // same as RotateL, but with equal power crossfading
	 *ar { arg n = 0, in;
		var insize = in.size;
		^Mix.fill(insize, {|i|
			PanAz.ar(insize, in[i], (i + n) * (2 / insize));
			});
		}
		
	*kr { arg n = 0, in;
		var insize = in.size;
		^Mix.fill(insize, {|i|
			PanAz.kr(insize, in[i], (i + n) * (2 / insize));
			});
		}
	
	}
	
SelectL : UGen { // select and interpolate
	*ar { arg which, array, envDiv = 10;
		var whichfrac, whichfloor;
		// if 'which' is .kr klicks are heard at the turning points..
		// use .arSwitch to avoid this (or use K2A.ar on the 'which' input)
		whichfrac = which.frac; whichfloor = which.floor;
		^(Select.ar(whichfloor%array.size, array) * (1 - whichfrac))
		+ (Select.ar((whichfloor + 1)%array.size, array) * whichfrac)
		}
		
	*arSwitch { arg which, array;
		^if(which.rate == 'control')
			{ SelectL.ar(K2A.ar(which), array); }
			{ SelectL.ar(which, array); };
		}

	*kr { arg which, array;
		var whichfrac, whichfloor;
		whichfrac = which.frac; whichfloor = which.floor;
		^(Select.kr(whichfloor%array.size, array) * (1 - whichfrac))
		+ (Select.kr((whichfloor + 1)%(array.size), array) * whichfrac)
		}
	}
