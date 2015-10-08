
// all my additions to the standard sc library

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

LNX_Out{
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
		if (protect) { sigOut = Protect(sigOut) };	// filters bad numbers
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

// not used yet
SendTrigID {
	classvar <id=1000;
	*initClass { id = 1000; }
	*next  { ^id = id + 1; }
}

Colour : Color {}

// covert from top left co-ords to bottom left co-ords
+ Rect {	
	convert{ ^Rect(left, SCWindow.screenBounds.bounds.height-top-height-44, width, height) }
}
+ Point { convert{ ^Point(x, SCWindow.screenBounds.bounds.height-y-44) } }

+ Dictionary {
	ordered { arg func;
		var assoc=[];
		if( this.isEmpty, { ^assoc });
		this.keysValuesDo { arg key, val;
			assoc = assoc.add(key -> val);
		};
		^assoc.sort(func)
	}
}

// safety

+ Object {
	
	// summary for apple click on sc code
	getClassArgsSummary{
		var class=this.class;
		var args=[];
		SymbolArray['ar','kr','ir','new'].do{|symbol|
			var method;
			if (symbol==\new) {
				method = class.findMethod(symbol);
			}{
				method = class.findRespondingMethodFor(symbol);
			};
			if (method.notNil) {
				args=args.add( symbol -> (method.argNames.drop(1)) ) ;
			};			
		};
		if (args.size>0) {^args} {^nil}
	}
	
	getClassArgsSummaryHelp{
		var summary=this.getClassArgsSummary;
		var list="";
		if (summary.notNil){
			summary.do{|ass|
				var string = this.asString++"."++(ass.key.asString)++"(";
				ass.value.do{|symbol|
					string=string++(symbol.asString)++", ";
				};
				string=string.drop(-2)++")";
				list=list++(string)++"\n";
			}
			^list.drop(-1);
		}{
			^nil	
		}
	}


	getClassArgsSummaryList{
		var summary=this.getClassArgsSummary;
		var list=[];
		if (summary.notNil){
			summary.do{|ass|
				var string = this.asString++"."++(ass.key.asString)++"(";
				ass.value.do{|symbol|
					string=string++(symbol.asString)++", ";
				};
				string=string.drop(-2)++")";
				list=list.add(string);
			}
			^list;
		}{
			^nil	
		}
	}

}



/*
make a bench with number of iterations
messures each iteration
gives min, max, averages and total
*/

+ Function {
	
	benchMark{|n=10,print=true|
		var dt;
		var t0;
		var total=0;
		var min=inf, max=0;
		
		n.do{|i| 
			t0 = Main.elapsedTime;
			this.value;
			dt = Main.elapsedTime - t0;
			total = total + dt;
			if (dt<min) { min=dt};
			if (dt>max) { max=dt};
		};
		
		if (print) {
			"".postln;
			"Total: ".post;
			total.post;
			" (x".post;
			n.post;
			")".postln;
			"Average: ".post;
			(total / n).postln;
			"Min: ".post;
			min.postln;
			"Max: ".post;
			max.postln;			
		};
		
		^total
		
	}
	
	
}

// this is cool, make any method a function

+ Object{
 
  asFunc{ |methodName|
         ^{ |Éargs| this.performList(methodName,args) }  
   }
 
}

+ Nil {
	isClosed{^true}
	notClosed{^false}
	isConnected{^false}
	thisUser{^nil}
	id{^nil}
	host{^nil}
	isHost{^nil}
} 

+ Color{

	postSmall{
		"Color(".post;
		if ((red==1)||(red==0)) { red.post } {
			red.asFraction[0].post;
			"/".post;
			red.asFraction[1].post;
		};
		",".post;
		
		
		if ((green==1)||(green==0)) { green.post } {
			green.asFraction[0].post;
			"/".post;
			green.asFraction[1].post;
		};
		
		",".post;
		
		if ((blue==1)||(blue==0)) { blue.post } {
			blue.asFraction[0].post;
			"/".post;
			blue.asFraction[1].post;
		};
		
		if (alpha!=1) {
			
			",".post;
			
			if (alpha==0) { alpha.post } {
				alpha.asFraction[0].post;
				"/".post;
				alpha.asFraction[1].post;
			};
		};
		
		")".postln;
	}


	/ {|y| ^Color.new(red/y,green/y,blue/y, alpha) }
	* {|y| ^Color.new((red*y).clip(0,1),(green*y).clip(0,1),(blue*y).clip(0,1), alpha) }
	+ {|y|
		if (y.species==Color) {
			^Color.new((red+y.red),(green+y.green),(blue+y.blue), alpha)
		}{
			^Color.new((red+y).clip(0,1),(green+y).clip(0,1),(blue+y).clip(0,1), alpha)
		}
	}
	- {|y| ^Color.new((red-y).clip(0,1),(green-y).clip(0,1),(blue-y).clip(0,1), alpha) }
	
	// i've been lazy at keeping this upto date, alot of colours are difined in the insts now
	
	*orange {^Color(1,0.5,0)}
	*purple {^Color(1,0  ,1)}
	
	*ndcTab1Focused   {^Color(0.4686,0.4373,0.3412)}
	*ndcTab1Unfocused {^Color(0.3686,0.3373,0.2412)}
	
	*ndcTab2Focused   {^Color(0.4686,0.4373,0.3412)}
	*ndcTab2Unfocused {^Color(0.3686,0.3373,0.2412)}
	
	*ndcTab3Focused   {^Color(0.4686,0.4373,0.3412)}
	*ndcTab3Unfocused {^Color(0.3686,0.3373,0.2412)}
	
	*ndcBack1         {^Color(0.6275,0.6039,0.4902)}
	*ndcBack0         {^Color(0.5686,0.5373,0.3912)}
	*ndcBack2         {^Color(0.1176,0.1176,0.0627)}
	*ndcBack3         {^Color(0.5686,0.5373,0.3412)}
	*ndcBack4         {^Color(0.2, 0.1, 0.1, 1)}
	
	*ndcStudioBack0   {^Color(0.5, 0.07, 0.07, 1)}
	*ndcStudioBack1   {^Color(0.2, 0.07, 0.07, 1)}
	*ndcStudioBack2   {^Color(0.6, 0.15, 0.15, 1)}
	*ndcStudioBack3   {^Color(0.3, 0.10, 0.10, 1)}
	*ndcStudioText    {^Color(1, 0.5, 0.5, 1)}
	*ndcStudioButton  {^Color(0.7, 0.25, 0.25, 1)}
	
	//*ndcStudioText0   {^Color(0.5, 0.25, 0.25, 1)}
	*ndcStudioText1   {^Color(0.5, 0.35, 0.25, 1)}
	*ndcStudioText2   {^Color(1, 1, 1, 1)}
	*ndcStudioSelect  {^Color(0.6, 0.35, 0.25, 1)} // {^Color(0.48, 0.06, 0.06, 1)}
	*ndcStudioBG	    {^Color(0.3, 0.2, 0.2, 1)}
	
	*ndcStudioOnOffBack {^Color(0.4, 0.15, 0.15, 1)}
	
	
	*ndcDarkButtonBG  {^Color(0.3086,0.2373,0.1512)}  // {^Color(0.2, 0.2, 0.3, 1.0 )}
	*ndcLightButtonBG {^Color(0.5686,0.5373,0.3412)}
	*ndcEnvBG         {^Color(0.1876,0.1876,0.1027)}  //  {^Color(0.1, 0.2, 0.25     )}
	
	*ndcSliderDarkBG  {^Color(0.1, 0.05, 0.05    )}
	*ndcSliderDarkKB  {^Color(0.5275,0.4939,0.4102)}
	
	*ndcSliderLightBG {^Color(0.3, 0.15, 0.15 )}
	*ndcSliderLightKB {^Color(0.4196,0.3843,0.2706)} // 
	
	*ndcMenuBG        {^Color(1,1,0.9)}  // {^Color(1,1,0.9)} 
	*ndcNumberBoxBG   {^Color(1,1,0.9)}  // {^Color(1,1,0.9)} 
	
	*ndcTextDarkBG    {^Color(0,0,0)}
	*ndcTextLightBG   {^Color(1,0.95,0.95)}
	
	*ndcListBackGrad1 {^Color(0.2876,0.2876,0.2027)}
	*ndcListBackGrad2 {^Color(0.1876,0.1876,0.1027)}
	*ndcListSelect    {^Color(0.4196,0.3843,0.2706)}
	
	*ndcVelocity1     {^Color(0.4196,0.3843,0.2706)}
	*ndcVelocity2     {^Color(0.6275,0.6039,0.4902)}
	*ndcVelocity3     {^Color(0.8275,0.8078,0.6784)}
	
	*ndcWaveform      {^Color(0.5686,0.5373,0.3412)}
	
	*ndcOnOffON		{^Color(1, 0.9, 0.3)}
	*ndcOnOffOFF		{^Color(0.3686,0.3373,0.2412)}
	*ndcOnOffText		{^Color(0,  0, 0)}
	*ndcOnOffONUen	{^Color(0.5, 0.5, 0.5)}
	*ndcOnOffOFFUen	{^Color(0.2, 0.2, 0.2)}
	*ndcOnOffTextUnen	{^Color(0.35,0.35,0.35)}
	
	*ndcOnOffON2		{^Color(0.25, 1, 0.25)}
	
	*ndcSoloBG		{^Color(1,0.2,0.2)}
	*ndcUpdateBG		{^Color(0.95,0.55,0.3)}
	
	*ndcLampBG		{^Color(0,0,0)}
	*ndcLampON		{^Color(1,0,0)}
	*ndcLampOFF		{^Color(0.3,0,0)}
	*ndcLampBorder	{^Color(1,1,1,0.6)}
	
	*ndcKnobOff		{^Color(0, 0, 0 )}
	*ndcKnobOn		{^Color(1, 0.9, 0.3)}
	*ndcKnobPin		{^Color(0,   0,   0)}
	*ndcKnobText		{^Color(0, 0, 0)}
	*ndcKnobOn2		{^Color(1, 0.6, 0.25)}
	*tabText			{^Color(1, 0.9, 0.3)}
	*tabText2			{^Color(1, 0.6, 0.2)}
	
	*ndcNameText		{^Color(0.6275,0.6039,0.4902)}
	*ndcNameText2		{^Color(0.4196,0.3843,0.2706)}
	*ndcNameBG		{^Color(0.15,0.07,0.07)}
	*ndcNameBG2		{^Color(0.3686,0.3373,0.2012)}
	
	*ndcMIDIActive	{^Color(0,1,1)}
	*ndcMIDIActive2	{^Color(1,0,1)}
	
	*ndcDark 			{^Color(0.1176,0.1176,0.0627)}
	*ndcDark2			{^Color(0.2386,0.2073,0.1412)}

}

//+ ServerOptions { 
//	*outDevices{ ^["Built-in Audio","Soundflower (2ch)"] }
//	*inDevices{ ^["Built-in Audio","Soundflower (2ch)"] }
//	*devices{ ^["Built-in Audio","Soundflower (2ch)"] }
//} // intel testing of ppc audio settings

//+ ServerOptions { 
//	*outDevices{ ^[ "Built-in Output", "Built-in Line Output", "Built-in Digital Output" ] }
//	*inDevices{ ^[ "Built-in Line Input", "Built-in Digital Input" ] }
//	*devices{ ^[ "Built-in Line Input", "Built-in Digital Input", "Built-in Output", "Built-in Line Output", "Built-in Digital Output"] }
//} // intel testing of audio settings 4 richie's computer

// isTrue & isFalse

+ Object {
	isTrue {^false}
	isFalse{^false}
}

+ Boolean {
	isTrue {^this==true}
	isFalse{^this==false}
}

+ Number {
	isTrue {^ this>=1 }
	isFalse{^ this< 1 }
}

+ String  {
	isTrue{  if ( (this=="1")or:{this=="true"}) {^true} {^false} }
	isFalse{ if ( (this=="0")or:{this=="false"}) {^true} {^false} }
}

+ Symbol  {
	isTrue{  if ( (this=='1')or:{this=='true'}) {^true} {^false} }
	isFalse{ if ( (this=='0')or:{this=='false'}) {^true} {^false} }
}

//////////////////////////////////////////////////////////////////////////////

+ Object { asAudio{ ^this.asAudioRateInput } }

+ SimpleNumber {

	asAudio{ ^this.asAudioRateInput}
	
	notNaN { ^(this >= 0 or: { this <= 0 }) }
	
	roundFloor{|n=1| ^(this-(n/2)).round(n) }
	
	logN{|n| ^log2(this)/log2(n) } // Base n logarithm. 

}

+ Number {
	
	++ {|item| ^this.asString++item }	
	
	mapVelocityToRange{|val,range,low,hi|
		var rng=(hi-low)*range;
		var l=(val-rng).clip(low,hi);
		var h=(val+rng).clip(low,hi);
		^this.map(0,1,l,h);
	}
	
	// digital reduction
	
	dr{
		^{|n|
			n=n.asInt.abs.asString.asList.collect(_.asString).collect(_.asInt).reduce('+');
			if (n>9) { thisFunction.value(n) } { n }
		}.value(this)
	}
	
	dr2{ if (this==0) { ^this }{ ^this.wrap(1,9) } }
	
/*
	(0..100).collect(_.dr2);
	(-50..50).collect{|x| (x**5)-(x**4*3)-(x**3)+(x**2*4)+(x*1)+1;}.collect(_.dr2).plot;
*/
	
	
}


+ MIDIOut {
	
	writeLatency { arg len, hiStatus, loStatus, a=0, b=0, argLatency;
		//[port, uid, len, hiStatus, loStatus, a, b, argLatency].postln;
		// argLatency.postln;
		this.send(port, uid, len, hiStatus, loStatus, a, b, argLatency);
	}
	
	noteOnLatency { arg chan, note=60, veloc=64, latency;
		this.writeLatency(3, 16r90, chan.asInteger, note.asInteger, veloc.asInteger, latency);
	}
	
	noteOffLatency { arg chan, note=60, veloc=64, latency;
		this.writeLatency(3, 16r80, chan.asInteger, note.asInteger, veloc.asInteger, latency);
	}
	
	controlLatency { arg chan, ctlNum=7, val=64, latency;
		this.writeLatency(3, 16rB0, chan.asInteger, ctlNum.asInteger, val.asInteger, latency);
	}
	programLatency { arg chan, num=1, latency;
		this.writeLatency(2, 16rC0, chan.asInteger, num.asInteger, argLatency:latency);
	}
	touchLatency { arg chan, val=64, latency;
		this.writeLatency(2, 16rD0, chan.asInteger, val.asInteger, argLatency:latency);
	}
	bendLatency { arg chan, val=8192, latency;
		val = val.asInteger;
		this.writeLatency(3, 16rE0, chan, val bitAnd: 127, val >> 7, latency);
	}
	
	songPtrLatency{ arg songPtr, latency;
		songPtr = songPtr.asInteger;	
		this.writeLatency(4, 16rF0, 16r02, songPtr & 16r7f, songPtr >> 7 & 16r7f, latency);
	}
	
	songSelectLatency { arg song, latency;
		this.writeLatency(3, 16rF0, 16r03, song.asInteger, 0, latency);
	}	
	midiClockLatency {|latency|
		this.writeLatency(1, 16rF0, 16r08, 0, 0, latency);
	}
	startLatency {|latency|
		this.writeLatency(1, 16rF0, 16r0A, 0, 0, latency);
	}
	continueLatency {|latency|
		this.writeLatency(1, 16rF0, 16r0B, 0, 0, latency);
	}
	stopLatency {|latency|
		this.writeLatency(1, 16rF0, 16r0C, 0, 0, latency);
	}
	resetLatency {|latency|
		this.writeLatency(1, 16rF0, 16r0F, 0, 0, latency);
	}
		
}

+ TabbedView {
	bounds_{|b|
		view.bounds_(b);
		this.updateViewSizes;
	}
}

+ SystemClock {
	*now{ ^this.seconds }
}

+ AppClock {
	*now{ ^this.seconds }
}

+ Function {
	// like defer but for system clock
	sched{|delta|
		delta=delta?0;
		if (delta>0) {
			//SystemClock.schedAbs(thisThread.seconds + delta,this);
			SystemClock.sched(delta,this);
		}{
			this.value;
			//this.value(thisThread.seconds,thisThread.seconds,SystemClock);
			// it should be this to be the same as System clock but i don't need to
		}
	}
	
}


//////////////////////////////////////////////////////////////////////////////////
// Clumps & List Compression
//////////////////////////////////////////////////////////////////////////////////


+ UGen {
	
	oddEvenMix{^this}
}

+ SequenceableCollection {
	
	// mixing down multichannel expansion to a stereo pair
	oddEvenMix{
		var odds;	
		var evens;
		if (this.size>1) {
			odds  = [];	
			evens = [];
			this.do{|i,j|
				if (j.odd) {
					odds=odds.add(i);
				}{
					evens=evens.add(i);
				};	
			};	
			^[Mix(evens),Mix(odds)]
		}{			
			^this[0]	
		};	
	}
		
	lnx_ClumpBundles {|clumpSize=4096|  // 4096, 6144, 8192
		var size=0, res, clumps, count=0, bundleSizes, tempList;
		tempList=[nil];
		
		// this might be very inefficient, check it out
		bundleSizes = this.collect {|item| tempList.put(0,item).msgSize }; // does this work ??
											 // what about msgSize
		bundleSizes.do { |a, i|
			size = size + a;
			if(size >= clumpSize) { clumps = clumps.add(count); count = 0; size = a };
			count = count + 1;
		};
		if (clumps.isNil) {
			^[this]
		}{
			^this.clumps(clumps)
		}
	}

	// compress a list (simply looks for repeats and compresses them)

	// make this more efficient by making func & items the max size they need to be, then trim

	compress{
		var list=this, size=this.size, mode=nil, thisItem, nextItem, doSize=size-1,
		    items=[], func=[], n, toRepeat, count, fromIndex;
		    
		if (size>1) {
			
			n=0;
			
			while {n<size} {
			
				thisItem=list[n];
				nextItem=list[n+1];
				
				if (mode.isNil) {
					if (thisItem==nextItem) {
						mode=\repeat;
						toRepeat=thisItem;
						count=1;
					}{
						mode=\diff;
						fromIndex=n;
						count=1;
						if (n==doSize) {
							func=func.add(count);
							items=items.add(thisItem);
						};
					};
				}{
					if (mode==\repeat) {
						if (thisItem==nextItem) {
							count=count+1;
						}{
							count=count+1;
							func=func.add(count);
							items=items.add(toRepeat);
							mode=nil;
						};
					};
					if (mode==\diff) {
						if (thisItem!=nextItem) {
							count=count+1;
							if (n==doSize) {
								func=func.add(count.neg);
								items=items++list[fromIndex..(n)];
							};
						}{
							func=func.add(count.neg);
							items=items++list[fromIndex..(n-1)];
							n=n-1;
							mode=nil;
						};
					};
				};
				n=n+1;	
			};			
			^[this.size,func.size]++func++items;
		}{
			^this
		}
	}
		
	decompress{
		var list,funcSize,func,items,item,n,totalSize,index;
		if (this.size>1) {	
			list=[];	
			totalSize=this[0];
			funcSize=this[1];	
			func=this[2..(funcSize+1)];
			items=this.drop(funcSize+2);
			list=Array.newClear(totalSize);	
			n=0;
			index=0;
			func.do{|i|
				if (i>0) {
					// repeat
					item=items[n];
					i.do{|j| list.put(index+j,item)};
					index=index+i;
					n=n+1;
				}{
					//diff
					i=i.abs;
					i.do{|j| list.put(index+j,items[n+j])};
					index=index+i;
					n=n+i;
				};
			};
			^list;	
		}{
			^this
		}
		
		

	}	
		
	
	/////////////////////////////////////////////////////////////////////////

	findNearest{|n,offset=0|
		var index=this.indexOfNearest(n);	
		if (index.notNil) {
			^this.clipAt(index+offset);
		}{
			^nil	
		}	
	}
	
	indexOfNearest{|n|
		var diff=inf, index;	
		this.do{|i,j|
			var d = (n-i).abs;
			if (d<diff) {
				diff=d;
				index=j
			};
		};
		^index
	}

	findKindOf{|species|
		var i,s;
		s=this.size;
		i = 0;
		while ( { i < s }, {
			if (this[i].isKindOf(species)) {^this[i]};
			i = i + 1;
		});
		^nil;
	}

	containsString{|string|
		string=string.asSymbol;
		this.size.do({|i|
			if ((this.at(i).isString)and:{this.at(i).asSymbol==string}) {^true}
		});
		^false
	}
	
	indexOfString{|string|
		string=string.asSymbol;
		this.size.do({|i|
			if ((this.at(i).isString)and:{this.at(i).asSymbol==string}) {^i}
		});
		^nil
	}
	
	indexOfList{|list|
		this.size.do({|i|
			if (this.at(i)==list) {^i}
		});
		^nil
	}

	// these pop statments have been optimised for speed ////////////////////
	
	popInt{ ^this.pop.asInt    }
	popI  { ^this.pop.asInt    }
	popF  { ^this.pop.asFloat  }
	popS  { ^this.pop.asString }	

	popN{|n|
		var l=Array.newClear(n);
		n.do{|i| l.put(i,this.pop) };
		^l
	}

	popNI{|n|
		var l=Array.newClear(n);
		n.do{|i| l.put(i,this.pop.asInt) };
		^l
	}
	
	popNF{|n|
		var l=Array.newClear(n);
		n.do{|i| l.put(i,this.pop.asFloat) };
		^l
	}
	
	popNS{|n|
		var l=Array.newClear(n);
		n.do{|i| l.put(i,this.pop.asString) };
		^l
	}

	popEND{|i|
		var l,j,index,size,doSize,item;
		i=i.asSymbol;
		j=i.asString;
		size=this.size;
		doSize=size-1;
		index=0;
		while ( {item=this.at(doSize-index); (index<size)&&(item!=i)&&(item!=j)}, {
			index=index+1;
		});
		l=this.popN(index);
		this.pop;
		^l
	}

	average { ^this.mean }
	
	asInt32Array{
		var int32Array;
		int32Array=Int32Array.newClear(this.size);
		this.do{|i,j| int32Array.put(j,i.asInt) }
		^int32Array
	}

}

+ Array {
	
	// reverse of String:ascii
	asciiToString{
		var s=String.newClear(this.size);
		this.do{|c,i| s[i]=c.asAscii};
		^s
	}

}

+ Collection {

	postList{
		var maxSize=0;
		"".postln;
		if (this.isSequenceableCollection) {
			maxSize=this.size.asString.size;
			this.do{|i,j| 
				"[".post;
				j=j.asString;
				(maxSize-(j.size)).clip(0,maxSize).do{" ".post};
				
				j.post;
				"] ".post;
				i.postln;
			};
		
		}{
			this.pairsDo{|i| maxSize=maxSize.max(i.asString.size)};
		
			this.pairsDo{|i,j| 
				i=i.asString;
				"[".post;
				i.post;
				(maxSize-(i.size)).clip(0,maxSize).do{" ".post};
				"] -> ".post;
				j.postln;
			};
		};
		^''
	}

}


+ Env {

	insertAtTime{|time,level| 
		var t=0, idx, thisTime;	
		times.do({|i,j| t=t+i; if ((t>time)&&(idx.isNil)) { idx= j+1; thisTime=t-times[j] } });
		times=times.insert(idx - 1,time - thisTime);
		levels=levels.insert(idx,level);
		time=time- thisTime;
		times[idx]=times[idx]-time;
		if ((idx- 1)<releaseNode) { releaseNode=releaseNode+1 };
	}
	
	removeAt{|node|
		var t;
		node=node.asInt;	
		if (((levels.size)>2)&&(node>0)&&(node<(levels.size- 1))) {
				t=times[node];
				times.removeAt(node);
				levels.removeAt(node);
				times.put(node- 1,times[node- 1]+t);
		}
	
	}
	
	dump2 {
		"An Env".postln;
		"levels, times, curves, releaseNode, loopNode".postln;
		levels.postln;
		times.postln;
		curves.postln;
		releaseNode.postln;
		loopNode.postln;
	}
	
}

// i don't know why i did this in the 1st place but it makes sc crash on 3rd compile now
//+ MIDIClient {
//
//	*init { arg inports=1, outports=1;
//		this.prInit(inports,outports);
//		initialized = true;
//		this.list;
//	}
//	
//}

+ Symbol {

	asInt{ ^this.asInteger }
	
	version { ^this.asString.split($ ).last.drop(1).asFloat }

	documentType {
		var c,d;
		c=this.asString.split($ ).drop(-1);
		d="";
		c.do({|c| d=d+c});
		d=d.drop(1);
		^d
	}
	

}

+ String {

	asInt{ ^this.asInteger }
	
	version {
		var numbers = this.split($ ).last.drop(1).split($.);
		^numbers.keep(2).join(".").asFloat
	}
	
	subVersion{
		var numbers = this.split($ ).last.drop(1).split($.);
		if (numbers.size>2) {
			^numbers[2].asInt
		}{
			^0	
		}
	}

	documentType {
		var c,d;
		c=this.split($ ).drop(-1);
		d="";
		c.do({|c| d=d+c});
		d=d.drop(1);
		^d
	}
	
	unixSafe{
		var string;
		string=this;
		#[$ ,$!,$$,$%,$&,$*,$(,$_,$<,$>,$?,$;,$",$',$|,$[,$],$=,$`,$~].do({|char|
			string=string.escapeChar(char);
		});
		^string
	}
	
	addressSafe{
		var string;	
		string=this;
		[" "].do{|char| string=string.findReplaceAll(char,"_") };
		["\r"].do{|char| string=string.findReplaceAll(char,"") };
		^string
	}

	profileSafe{
		var string;	
		string=this;
		["\r","\n","*"].do{|char| string=string.findReplaceAll(char,"") };
		^string[..38]
	}
	
	nameSafe{
		var string;	
		string=this;
		["\r"].do{|char| string=string.findReplaceAll(char,"") };
		^string[..100]
	}
	
	filenameSafe{
		var string;	
		string=this;
		["\r","\n","*","/"].do{|char| string=string.findReplaceAll(char,"") };
		^string[..38]
	}
	
	dropFolder{|d|
		var string="";
		if (d>0) {d=d+1};
		this.copy.split.drop(d).do{|s| string=string +/+ s };
		^string	
	}
	
	openApplication {
		("open -a" + this.quote).systemCmd;
	}	
	
}

+ Buffer {

	getWithRef { arg index, action, ref1, ref2;
		OSCpathResponder(server.addr,['/b_set',bufnum,index],{ arg time, r, msg; 
			action.value(msg.at(3),index, ref1, ref2); r.remove }).add;
		server.listSendMsg(["/b_get",bufnum,index]);
		
		}
		
}


+ Integer {

	clipIndexToList { arg list; ^(this.clip(0,list.size - 1)) }
	
	asFormatedString{|wn=1,dp=1| ^this.asFloat.asFormatedString(wn,dp); }
	
	map { |x1,x2,y1,y2| ^this.asFloat.map(x1,x2,y1,y2) }
	
	asInt { ^this }
	
	asNote {|prefix|
		prefix= prefix ? false;
		^(prefix.if((this.asString)++": ","")
     		++(#["C-","C#","D-","D#","E-","F-","F#","G-","G#","A-","A#","B-"].wrapAt(this))
     		++((this div: 12).asString));
	}

	asNote2 {|prefix|
		prefix= prefix ? false;
		^(prefix.if((this.asString)++": ","")
     		++(#["C","C#","D","D#","E","F","F#","G","G#","A","A#","B"].wrapAt(this))
     		++((this div: 12).asString));
	}

	asNote3 { ^#["C","C#","D","D#","E","F","F#","G","G#","A","A#","B"].wrapAt(this) }

	asNote4 {|spo=12| ^(this % (spo.asInt) + 65).asAscii ++((this div: spo).asString) }
	
}


+ Float {

	decimal{|dp| ^(this-(this.round)*(10**dp)).round/(10**dp)+(this.round)}
	
	asFormatedString{|wn=1,dp=1| 
		var strList;
		
		if (this==inf) {^"inf"};
		if (this==(-inf)) {^"-inf"};
		
		strList=this.decimal(dp).asString.split($.);
		while ( { (strList@0).size < wn }, { strList=[" "++(strList@0),strList@1] });
		if (dp>0)	{
			if (strList.size<2) {		
				strList=strList.add("0");
			};
			while ( { (strList@1).size < dp }, { strList=[strList@0 ,(strList@1)++"0"] });
			^(strList@0)++"."++(strList@1)
		}{
			^strList@0
		}
		^this
	}
	
	map{|x1,x2,y1,y2| ^((this-x1/(x2-x1))*(y2-y1)+y1) }
	
	asInt{ ^this.asInteger }
	
	asNote{|prefix| ^this.asInteger.asNote(prefix) }
	
	asNote2{|prefix| ^this.asInteger.asNote2(prefix) }
	
	asNote3{^this.asInteger.asNote3 }
	
	asNote4{^this.asInteger.asNote4 }
	
}

+ SCTextField {

	background_{|c|
		if (Main.versionAtMost(3,2)) {
			this.setProperty(\boxColor,c) 
		}{
			super.background_(c);
		}
	} // backwards compatability to v3.2

}


////////////////////////////////////////////////


