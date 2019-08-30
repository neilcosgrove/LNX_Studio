/* /////////////////////////////////////////////////////////////////////////////////////////////////
**  multi DoubleArray with multiple DoubleArray sizes
**  1 array exists for each size in the range minSize..maxSize which gets dyamically created as used
**  used in K-Hole
** /////////////////////////////////////////////////////////////////////////////////////////////////

m = LNX_MultiDoubleArray(64);
m[1]=2;
m[1];
m.size_(3);  // you can change the size of the array, must be between minSize and maxSize or throws an error
m;           // now array is 3 size
m.size_(64); // gives you back the orignal array of size 64
m;           // the original array

m = LNX_MultiDoubleArray(16);
m.randFill;        // fill with random values
m.clipAtLin(0.5);  // half way between 0 & 1
m.wrapAtLin(-0.5); // half way between 0 & -1
m.plot;
m.resizeLin_(100); // resize current data to new size using linear interpolation
m.plot;
m.resizeLin_(10);  // resize current data to new size using linear interpolation
m.plot;

~osc = Koscillator();
~osc.array.plot;
~osc.nextTickArray.plot;
~osc.freq = 2;
~osc.nextTickArray.plot;

*/

Kmodulator{

	// wave = \sine (sin), \triangle (tri), \sawUp, \sawDown, \square
	var  <wave=\sine, <>freq=1, <>phase=0, <>time=0, <>tick=0.01, <value=0, <>oneShot=false;

	// make me a new one
	*new {|wave=\sine, freq=1, phase=0, oneShot=false, time=0, tick=0.01|
		^super.new.init(wave, freq, phase, oneShot, time, tick)
	}

	init{|argWave, argFreq, argPhase, argOneShot, argTime, argTick|
		this.wave   = argWave;
		freq        = argFreq;
		phase       = argPhase;
		oneShot     = argOneShot;
		time        = argTime;
		tick        = argTick;
		this.generateValue;
	}

	// change wave types
	wave_{|argWave|
		wave = argWave;
		if (wave.isNumber) { wave = #[\sine, \triangle, \sawUp, \sawDown, \square][wave.asInt] };
		if (wave == \sin) { wave = \sine     };
		if (wave == \tri) { wave = \triangle };
	}

	// 1 tick of the clock
	tickClock{
		time = time + (tick * freq);
		if (oneShot) {
			if (time>1)   { time =  1.0 };
			if (time< -1) { time = -1.0 };
		};
	}

	// make the value
	generateValue{
		// sine wave
		if (wave==\sine) {
			value = ( (time + phase + 0.75 * 2pi) ).sin + 1 * 0.5
		};
		// triangle wave
		if (wave==\triangle) {
			value = ( (time + phase + 0.75) * 2 + 0.5 ).fold(0.0,1.0)
		};
		// sawUp wave
		if (wave==\sawUp) {
			if (freq==0) {
				value = 0;
			}{
				value = ( time + phase ).wrap(0.0,1.0);
			};
		};
		// sawDown wave
		if (wave==\sawDown) {
			if (freq==0) {
				value = 1;
			}{
				value = 1 - (( time + phase ).wrap(0.0,1.0));
			};
		};
		// square wave
		if (wave==\square) {
			if (freq==0) {
				value= 1;
			}{
				value = 1 - (( time + phase ).wrap(0.0,1.0).round(1));
			};
		};
	}

	// next tick of clock + make next value
	nextTick{|argTick|
		tick = argTick ? tick;
		this.tickClock;
		this.generateValue;
		^value
	}

	// reset the clock + make value
	reset{
		time = 0;
		this.generateValue;
	}

}

// %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

Koscillator{

	// wave = \sine (sin), \triangle (tri), \sawUp, \sawDown, \square
	var <wave=\sine, <>freq=1, <>freq2=1, <>phase=0, <size=64, <>time=0, <>tick=0.01, <>controlSpec, <array;
	var <>oneShot=false, <>mirror=false, <>mul=1, <>add=0, <>mode=\none, <clipboard;

	// make me a new one
	*new {|wave=\sine, freq=1, freq2=1, phase=0, mul=1, add=0, oneShot=false, mirror=false, size=64, time=0, tick=0.01, controlSpec|
		^super.new.init(wave, freq, freq2, phase, mul, add, oneShot, mirror, size, time, tick, controlSpec)
	}

	init{|argWave, argFreq, argFreq2, argPhase, argMul, argAdd, argOneShot, argMirror, argSize, argTime, argTick, argControlSpec|
		this.wave   = argWave;
		freq        = argFreq;
		freq2       = argFreq2;
		phase       = argPhase;
		mul         = argMul;
		add         = argAdd;
		oneShot     = argOneShot;
		mirror      = argMirror;
		size        = argSize;
		time        = argTime;
		tick        = argTick;
		controlSpec = argControlSpec;
		array = DoubleArray.fill(size, 0);
		this.generateArray;
		clipboard = nil;
	}

	// happens when noBands is changed
	size_{|n|
		var newArray;
		if (clipboard.notNil) {
			newArray = DoubleArray.fill(n, 0);
			n.do{|i| newArray[i] = clipboard.atL(i/n*size) };
			clipboard = newArray;
		};
		newArray = DoubleArray.fill(n, 0);
		n.do{|i| newArray[i] = array.atL(i/n*size) };
		array = newArray;
		size=n;
	}

	// add array to the clipboard
	addToClipboard{|array| clipboard=array  }

	// remove item from clipboard
	emptyClipboard{|item| clipboard = nil }

	// change wave types
	wave_{|argWave|
		wave = argWave;
		if (wave.isNumber) { wave = #[\sine, \triangle, \sawUp, \sawDown, \square, \noise1, \noise2, \user][wave.asInt] };
		if (wave == \sin) { wave = \sine     };
		if (wave == \tri) { wave = \triangle };
	}

	// 1 tick of the clock
	tickClock{
		time = time + (tick * freq * freq2);
	}

	// make the array
	generateArray{
		// sine wave
		if (wave==\sine) {
			if (oneShot) {
				size.do{|i|
					array[i] = ((time + (phase * freq) + 0.75 * 2pi) + (i * freq / size* 2pi)).clip(2pi*0.75,2pi*1.75).sin + 1 * 0.5
				};
			}{
				size.do{|i|
					array[i] = ( (time + (phase * freq) + 0.75 * 2pi) + (i * freq * 2pi / size) ).sin + 1 * 0.5
				};
			}
		};
		// triangle wave
		if (wave==\triangle) {
			if (oneShot) {
				size.do{|i|
					array[i] = ( (time + (phase * freq) + 0.75) * 2 + 0.5 + (i * freq * 2 / size) ).clip(2,4).fold(0.0,1.0)
				};
			}{
				size.do{|i|
					array[i] = ( (time + (phase * freq) + 0.75) * 2 + 0.5 + (i * freq * 2 / size) ).fold(0.0,1.0)
				}
			}
		};
		// sawUp wave
		if (wave==\sawUp) {
			if (oneShot) {
				if (freq==0) {
					size.do{|i| array[i] = 0 };
				}{
					size.do{|i|
						array[i] = ( (time + (phase * freq)) + (i * freq / size) ).clip(0,1).wrap(0.0,1.0);
					};
				};
			}{
				if (freq==0) {
					size.do{|i| array[i] = 0 };
				}{
					size.do{|i|
						array[i] = ( (time + (phase * freq)) + (i * freq / size) ).wrap(0.0,1.0);
					};
				};
			};
		};
		// sawDown wave
		if (wave==\sawDown) {
			if (oneShot) {
				if (freq==0) {
					size.do{|i| array[i] = 0 };
				}{
					size.do{|i|
						array[i] = ( 1-((time + (phase * freq)) + (i * freq / size)) ).clip(0,1).wrap(0.0,1.0);
					};
				};
			}{
				if (freq==0) {
					size.do{|i| array[i] = 0 };
				}{
					size.do{|i|
						array[i] = 1 - (( (time + (phase * freq)) + (i * freq / size) ).wrap(0.0,1.0));
					};
				};
			}
		};
		// square wave
		if (wave==\square) {
			if (oneShot) {
				if (freq==0) {
					size.do{|i| array[i] = 0 };
				}{
					size.do{|i|
						array[i] = ( 1-((time + (phase * freq)) + (i * freq / size)) ).clip(0,1).wrap(0.0,1.0).round(1);
					};
				};
			}{
				if (freq==0) {
					size.do{|i| array[i] = 1 };
				}{
					size.do{|i|
						array[i] = 1 - (( (time + (phase * freq)) + (i * freq / size) ).wrap(0.0,1.0).round(1));
					};
				};
			};
		};
		// noise1
		if (wave==\noise1) {
			size.do{|i|
				array[i] = ((time * 4) + ( phase * 1000000 ) + (i / size * freq * 4 )).hashScapeSin;
			};
		};
		// noise2
		if (wave==\noise2) {
			size.do{|i|
				array[i] = ((time * 4) + ( phase * 1000000 ) + (i / size * freq * 4 )).hashScape;
			};
		};

		// user wave
		if (wave==\user) {
			if (clipboard.notNil) {
				var clipSize  = clipboard.size;
				if (oneShot) {
					if (freq==0) {
						size.do{|i| array[i] = clipboard[0] };
					}{
						size.do{|i|
							var index = ( (time + (phase * freq)) + (i * freq / size) ).clip(0,1)*(clipSize);
							array[i] = clipboard.atL(index);
						};
					};
				}{
					if (freq==0) {
						size.do{|i| array[i] = clipboard[0] };
					}{
						size.do{|i|
							var index = ( (time + (phase * freq)) + (i * freq / size) ).wrap(0.0,1.0)*(clipSize);
							array[i] = clipboard.atLW(index);
						};
					};
				};
			}{
				^this
			};
		};

		if (mode==\power){
			size.do{|i| array[i] = array[i] **mul     }
		} {
			if (mul!=1)      { size.do{|i| array[i] = array[i] * mul     } }
		};
		if (add!=0)      { size.do{|i| array[i] = array[i] + add     } };
		if (mode==\clip) { size.do{|i| array[i] = array[i].clip(0,1) } };
		if (mode==\wrap) { size.do{|i| array[i] = array[i].wrap(0,1) } };
		if (mode==\fold) { size.do{|i| array[i] = array[i].fold(0,1) } };
		if (mirror)      { array = array.reverse }; // you can do this in place more efficiently
		if (controlSpec.notNil) { size.do{|i| array[i] = controlSpec.map( array[i] ) } };

	}

	// next tick of clock + make next array
	nextTick{|argTick|
		tick = argTick ? tick;
		this.tickClock;
		this.generateArray;
		^array
	}

	// reset the clock + make array
	reset{
		time = 0;
		this.generateArray;
	}

}

// ***********************************************************************************************************************

LNX_MultiDoubleArray{

	var <size, <minSize, <maxSize, <>controlSpec, <arrays, <array;

	// make me a new one
	*new {|size=64, minSize=3, maxSize=512, controlSpec=\unipolar|
		^super.new.init(size.clip(minSize,maxSize),minSize,maxSize,controlSpec)
	}

	// init and make the 1st array with size size
	init{|argSize,argMinSize,argMaxSize,argControlSpec|
		size         = argSize;
		minSize      = argMinSize;
		maxSize      = argMaxSize;
		controlSpec  = argControlSpec.asSpec;
		arrays       = IdentityDictionary[];
		array        = DoubleArray.fill(size, controlSpec.default);
		arrays[size] = array;
	}

	// at
	at{|index| ^array[index] }
	@ {|index| ^array[index] }
	unmapAt{|index| ^controlSpec.unmap(array[index]) }
	unmapNoClipAt{|index| ^controlSpec.unmapNoClip(array[index]) }


	// clipAt with linear interpolation, map & unmap with control spec
	clipAtLin{|index,fromSize|
		var frac,v1,v2;
		fromSize = fromSize ? size;
		frac = index.frac;
		v1 = controlSpec.unmap( arrays[fromSize].clipAt(index.floor.asInt) );
		v2 = controlSpec.unmap( arrays[fromSize].clipAt(index.ceil.asInt) );
		^controlSpec.map( (v2*frac) + (v1*(1-frac)) );
	}

	// wrapAt with linear interpolation, map & unmap with control spec
	wrapAtLin{|index,fromSize|
		var frac,v1,v2;
		fromSize = fromSize ? size;
		frac = index.frac;
		v1 = controlSpec.unmap( arrays[fromSize].wrapAt(index.floor.asInt) );
		v2 = controlSpec.unmap( arrays[fromSize].wrapAt(index.ceil.asInt) );
		^controlSpec.map( (v2*frac) + (v1*(1-frac)) );
	}

	// put
	put{|index,value| array.put(index, value) }

	// put and contrain value with the controlSpec
	putMap{|index,value| array.put( index, controlSpec.map(value)) }

	// put and contrain value with the controlSpec
	putConstrain{|index,value| array.put( index, controlSpec.constrain(value)) }

	// fill with random values
	randFill{ size.do{|i| array.put(i,controlSpec.map(1.0.rand)) } }

	// replace array with new array
	replace{|newArray|
		array = newArray;
		arrays[size] = array;
	}

	// rest to control spec default
	reset{ size.do{|i| array.put(i,controlSpec.default) } }

	normalize{
		var min, max;
		array.size.do{|i|
			array[i] = controlSpec.unmap(array[i]);
		};
		min = array.minItem;
		max = array.maxItem;
		array.size.do{|i|
			array[i] = controlSpec.map(array[i].map(min,max,0,1));
		};
	}

	constrain{
		array.size.do{|i|
			array[i] = controlSpec.constrain(array[i]);
		};
	}

	// select what size array you want
	size_{|newSize|
		if ((newSize>=minSize) && (newSize<=maxSize)) {
			size = newSize.asInt;
			if (arrays[size].isNil) {
				array = DoubleArray.fill(size, controlSpec.default);
				arrays[size] = array;
			}{
				array = arrays[size];
			};
		}{
			"Out of range".throw;
		}
	}

	// resize array to newSize using data fromSize using linear interpolation
	resizeLin_{|newSize,fromSize|
		fromSize = (fromSize ? size).asInt;
		if (arrays[fromSize].isNil) { "Size does not exist".throw };
		this.size_(newSize);
		if (newSize==fromSize) { ^this };
		newSize.do{|i|
			array.put(i,
				this.clipAtLin(i/(newSize-1)*(fromSize-1), fromSize)
			);
		};
	}

	plot{ array.plot }

	printOn {|stream| stream << this.class.name << "(" << size << ", " << minSize << ", " <<
		maxSize << ", " << controlSpec.asCompileString << ")\n" << array.asCompileString }

}