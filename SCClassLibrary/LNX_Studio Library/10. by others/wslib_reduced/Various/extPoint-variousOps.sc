// wslib 2010
// additional operators to make Point more flexible and usable

+ Point {
	
	asUGenInput { ^this.asArray } // dangerous?
	asControlInput { ^this.asArray }
	
	performOnEach { arg selector ...args; // more optimization possible?
		args = args.collect(_.asArray);
		^this.asArray.collect({ |item, i| item.perform( selector, *args.collect(_.wrapAt(i)) ) })
			.asPoint;
	}
	
	angle { ^this.theta }
	
	// unary ops
	neg { ^this.performOnEach( thisMethod.name ) }
	ceil { ^this.performOnEach( thisMethod.name ) }
	floor { ^this.performOnEach( thisMethod.name ) }
	frac { ^this.performOnEach( thisMethod.name ) }
	squared { ^this.performOnEach( thisMethod.name ) }
	cubed { ^this.performOnEach( thisMethod.name ) }
	sqrt { ^this.performOnEach( thisMethod.name ) }
	exp { ^this.performOnEach( thisMethod.name ) }
	reciprocal { ^this.performOnEach( thisMethod.name ) }
	
	log { ^this.performOnEach( thisMethod.name ) }
	log2 { ^this.performOnEach( thisMethod.name ) }
	log10 { ^this.performOnEach( thisMethod.name ) }
	
	sin { ^this.performOnEach( thisMethod.name ) }
	cos { ^this.performOnEach( thisMethod.name ) }
	tan { ^this.performOnEach( thisMethod.name ) }
	asin { ^this.performOnEach( thisMethod.name ) }
	acos { ^this.performOnEach( thisMethod.name ) }
	atan { ^this.performOnEach( thisMethod.name ) }
	sinh { ^this.performOnEach( thisMethod.name ) }
	cosh { ^this.performOnEach( thisMethod.name ) }
	tanh { ^this.performOnEach( thisMethod.name ) }
	
	rand { ^this.performOnEach( thisMethod.name ) }
	rand2 { ^this.performOnEach( thisMethod.name ) }
	linrand { ^this.performOnEach( thisMethod.name ) }
	bilinrand { ^this.performOnEach( thisMethod.name ) }
	sum3rand { ^this.performOnEach( thisMethod.name ) }
	
	distort { ^this.performOnEach( thisMethod.name ) }
	softclip { ^this.performOnEach( thisMethod.name ) }
	
	// binary ops
	pow { arg that, adverb; ^this.performOnEach( thisMethod.name, that, adverb ) }
	min { arg that, adverb; ^this.performOnEach( thisMethod.name, that, adverb ) }
	max { arg that=0, adverb; ^this.performOnEach( thisMethod.name, that, adverb )}
	roundUp { arg that=1.0, adverb; ^this.performOnEach( thisMethod.name, that, adverb )}
	
	clip2 { arg that, adverb; ^this.performOnEach( thisMethod.name, that, adverb ) }
	fold2 { arg that, adverb; ^this.performOnEach( thisMethod.name, that, adverb ) }
	wrap2 { arg that, adverb; ^this.performOnEach( thisMethod.name, that, adverb ) }

	excess { arg that, adverb;  ^this.performOnEach( thisMethod.name, that, adverb ) }
	firstArg { arg that, adverb; ^this.performOnEach( thisMethod.name, that, adverb ) }
	rrand { arg that, adverb; ^this.performOnEach( thisMethod.name, that, adverb ) }
	exprand { arg that, adverb; ^this.performOnEach( thisMethod.name, that, adverb ) }
	
	// other methods
	clip { arg lo, hi; ^this.performOnEach( \clip, lo, hi ) }
	wrap { arg lo, hi; ^this.performOnEach( \wrap, lo, hi ) }
	fold { arg lo, hi; ^this.performOnEach( \fold, lo, hi ) }
		
	linlin { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, clip=\minmax;
		^this.performOnEach( \linlin, inMin, inMax, outMin, outMax, clip) }
		
	linexp { arg inMin = 0, inMax = 1, outMin = 0.001, outMax = 1, clip=\minmax;
		^this.performOnEach( \linexp, inMin, inMax, outMin, outMax, clip) }
		
	explin { arg inMin = 0.001, inMax = 1, outMin = 0, outMax = 1, clip=\minmax;
		^this.performOnEach( \explin, inMin, inMax, outMin, outMax, clip) }
		
	expexp { arg inMin = 0.001, inMax = 1, outMin = 0.001, outMax = 1, clip=\minmax;
		^this.performOnEach( \expexp, inMin, inMax, outMin, outMax, clip) }
		
	lincurve { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip=\minmax;
		^this.performOnEach( \lincurve, inMin, inMax, outMin, outMax, clip) }
		
	curvelin { arg inMin = 0, inMax = 1, outMin = 0, outMax = 1, curve = -4, clip=\minmax;
		^this.performOnEach( \curvelin, inMin, inMax, outMin, outMax, clip) }
	
	bilin { arg inCenter, inMin, inMax, outCenter, outMin, outMax, clip=\minmax;
		^this.performOnEach( \bilin, inMin, inMax, outCenter, outMin, outMax, clip) 
	}
	
	biexp { arg inCenter, inMin, inMax, outCenter, outMin, outMax, clip=\minmax;
		^this.performOnEach( \biexp, inMin, inMax, outCenter, outMin, outMax, clip) 
	}
	
	// hmmm... dubious... (works for ControlSpec though)
	< { arg that, adverb;  ^this.rho.perform( thisMethod.name, that.asPoint.rho, adverb ) }
	> { arg that, adverb;  ^this.rho.perform( thisMethod.name, that.asPoint.rho, adverb ) }
	<= { arg that, adverb; ^this.rho.perform( thisMethod.name, that.asPoint.rho, adverb ) }
	>= { arg that, adverb; ^this.rho.perform( thisMethod.name, that.asPoint.rho, adverb ) }

	
}