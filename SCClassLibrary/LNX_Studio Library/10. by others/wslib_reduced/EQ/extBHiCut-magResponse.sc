+ BHiCut {
	
	*magResponse { arg freqs = 1000, sr = 44100, freq, order = 2;
		var rqs, in; 
		rqs = allRQs.clipAt(order); 
		
		if( freqs.isNumber ) // autoscale 20-22000
			{ freqs = (..freqs).linexp(0,freqs-1, 20, 22000); };
		in = 1!freqs.size;
		rqs.do { |rq| in = in * this.filterClass.magResponse( freqs, sr, freq, rq ) };
		^in
	}
	
}