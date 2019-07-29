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

~osc = Kosc();
~osc.array.plot;
~osc.nextTickArray.plot;
~osc.freq = 2;
~osc.nextTickArray.plot;

*/

	// 2nd freq can be up down or left and right
	// one shot animation
	// reverse fill animation
	// copy waveForm

Kosc{

	// type = \sine (sin), \triangle (tri), \sawUp, \sawDown, \square
	var  <type=\sine, <>freq=1, <>freq2=1, <>phase=0, <size=64, <>time=0, <>tick=0.01, <>controlSpec, <array;

	// make me a new one
	*new {|type=\sine, freq=1, freq2=1, phase=0, size=64, time=0, tick=0.01, controlSpec|
		^super.new.init(type, freq, freq2, phase, size, time, tick, controlSpec)
	}

	init{|argType, argFreq, argFreq2, argPhase, argSize, argTime, argTick, argControlSpec|
		this.type   = argType;
		freq        = argFreq;
		freq2       = argFreq2;
		phase       = argPhase;
		size        = argSize;
		time        = argTime;
		tick        = argTick;
		controlSpec = argControlSpec;
		array = DoubleArray.fill(size, 0);
		this.generateArray;
	}

	// change wave types
	type_{|argType|
		type = argType;
		if (type.isNumber) { type = #[\sine, \triangle, \sawUp, \sawDown, \square][type] };
		if (type == \sin) { type = \sine     };
		if (type == \tri) { type = \triangle };
	}

	// 1 tick of the clock
	tickClock{ time = time + (tick * freq * freq2) }

	// make the array
	generateArray{
		// sine wave
		if (type==\sine) {
			if (controlSpec.isNil){
				size.do{|i|
					array[i] = ( (time + phase + 0.75 * 2pi) + (i * freq * 2pi / size) ).sin + 1 * 0.5
				};
			}{
				size.do{|i|
					array[i] = controlSpec.map( ( (time + phase + 0.75 * 2pi) + (i * freq * 2pi / size) ).sin + 1 * 0.5 )
				};
			};
		};
		// triangle wave
		if (type==\triangle) {
			if (controlSpec.isNil){
				size.do{|i|
					array[i] = ( (time + phase + 0.75) * 2 + 0.5 + (i * freq * 2 / size) ).fold(0.0,1.0)
				};
			}{
				size.do{|i|
					array[i] = controlSpec.map( ( (time + phase + 0.75) * 2 + 0.5 + (i * freq * 2 / size) ).fold(0.0,1.0) )
				};
			};
		};
		// sawUp wave
		if (type==\sawUp) {
			if (controlSpec.isNil){
				if (freq==0) {
					size.do{|i| array[i] = 0 };
				}{
					size.do{|i|
						array[i] = ( (time + phase) + (i * freq / size) ).wrap(0.0,1.0);
					};
				};
			}{
				if (freq==0) {
					size.do{|i| array[i] = controlSpec.map( 0 ) };
				}{
					size.do{|i|
						array[i] = controlSpec.map( ( (time + phase) + (i * freq / size) ).wrap(0.0,1.0) );
					};
				};
			};
		};
		// sawDown wave
		if (type==\sawDown) {
			if (controlSpec.isNil){
				if (freq==0) {
					size.do{|i| array[i] = 1 };
				}{
					size.do{|i|
						array[i] = 1 - (( (time + phase) + (i * freq / size) ).wrap(0.0,1.0));
					};
				};
			}{
				if (freq==0) {
					size.do{|i| array[i] = controlSpec.maxval };
				}{
					size.do{|i|
						array[i] = controlSpec.map( 1-( ( (time + phase) + (i * freq / size) ).wrap(0.0,1.0) ) );
					};
				};
			};
		};
		// square wave
		if (type==\square) {
			if (controlSpec.isNil){
				if (freq==0) {
					size.do{|i| array[i] = 1 };
				}{
					size.do{|i|
						array[i] = 1 - (( (time + phase) + (i * freq / size) ).wrap(0.0,1.0).round(1));
					};
				};
			}{
				if (freq==0) {
					size.do{|i| array[i] = controlSpec.maxval };
				}{
					size.do{|i|
						array[i] = controlSpec.map( 1-( ( (time + phase) + (i * freq / size) ).wrap(0.0,1.0).round(1) ) );
					};
				};
			};
		};
	}

	// next tick of clock + make next array
	nextTickArray{|argTick|
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