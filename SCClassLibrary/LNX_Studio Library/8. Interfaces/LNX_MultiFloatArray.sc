/* /////////////////////////////////////////////////////////////////////////////////////////////////
**  multi FloatArray with multiple FloatArray sizes
**  1 array exists for each size in the range minSize..maxSize which gets dyamically created as used
**  used in K-Hole
** /////////////////////////////////////////////////////////////////////////////////////////////////

m = LNX_MultiFloatArray(64);
m[1]=2;
m[1];
m.size_(3);  // you can change the size of the array, must be between minSize and maxSize or throws an error
m;           // now array is 3 size
m.size_(64); // gives you back the orignal array of size 64
m;           // the original array

m = LNX_MultiFloatArray(16);
m.randFill;        // fill with random values
m.clipAtLin(0.5);  // half way between 0 & 1
m.wrapAtLin(-0.5); // half way between 0 & -1
m.plot;
m.resizeLin_(100); // resize current data to new size using linear interpolation
m.plot;
m.resizeLin_(10);  // resize current data to new size using linear interpolation
m.plot;

*/ ///////////////////////////

LNX_MultiFloatArray{

	var <size, <minSize, <maxSize, <controlSpec, <arrays, <array;

	// make me a new one
	*new {|size=64, minSize=3, maxSize=128, controlSpec=\unipolar|
		^super.new.init(size.clip(minSize,maxSize),minSize,maxSize,controlSpec)
	}

	// init and make the 1st array with size size
	init{|argSize,argMinSize,argMaxSize,argControlSpec|
		size         = argSize;
		minSize      = argMinSize;
		maxSize      = argMaxSize;
		controlSpec  = argControlSpec.asSpec;
		arrays       = IdentityDictionary[];
		array        = FloatArray.fill(size, controlSpec.default);
		arrays[size] = array;
	}

	// at
	at{|index| ^array[index] }
	@ {|index| ^array[index] }
	unmapAt{|index| ^controlSpec.unmap(array[index]) }

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

	// select what size array you want
	size_{|newSize|
		if ((newSize>=minSize) && (newSize<=maxSize)) {
			size = newSize.asInt;
			if (arrays[size].isNil) {
				array = FloatArray.fill(size, controlSpec.default);
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