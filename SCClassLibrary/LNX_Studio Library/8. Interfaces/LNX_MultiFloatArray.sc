/* ///////////////////////////

  multi FloatArray with multiple FloatArray sizes
  1 array exists for each size in the range minSize..maxSize which gets dyamically created as used
  used in K-Hole

m = LNX_MultiFloatArray(64);
m[1]=2;
m[1];
m.size_(3); // you can change the size of the array, must be between minSize and maxSize or throws an error
m; // now array is 3 size
m.size_(64); // gives you back the orignal array of size 64
m; // the original array

*/ ///////////////////////////

LNX_MultiFloatArray{

	var <size, <minSize, <maxSize, <controlSpec, <arrays, <array;

	// make me a new one
	*new {|size=64, minSize=3, maxSize=128, controlSpec=\unipolar|
		^super.new.init(size,minSize,maxSize,controlSpec)
	}

	// init and make the 1st array with size size
	init{|argSize,argMinSize,argMaxSize,argControlSpec|
		size = argSize;
		minSize = argMinSize;
		maxSize = argMaxSize;
		controlSpec = argControlSpec.asSpec;

		arrays = IdentityDictionary[];
		array = FloatArray.fill(size, controlSpec.default);
		arrays[size] = array;
	}

	// at
	at{|index| ^array[index] }
	@ {|index| ^array[index] }

	// put
	put{|index,value| array.put(index, value) }

	// put and contrain value with the controlSpec
	putConstrain{|index,value| array.put( index, controlSpec.constrain(value)) }

	// select what size array you want
	size_{|size|
		size = size.asInt;
		if ((size>=minSize) && (size<=maxSize)) {
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

	printOn {|stream| stream << this.class.name << "(" << size << ", " << minSize << ", " <<
		maxSize << ", " << controlSpec.asCompileString << ")\n" << array.asCompileString }

}