////////////////////////////////////////////////////////////////////////////////////////////////////
// for easy access to trigid, is this a good idea?

+ UGen {
	poll {|trigid = -1, trig = 10, label|
          ^Poll(trig, this, label, trigid)
	}
}

+ Array {
	poll {|trigid = -1,trig = 10, label|
		if(label.isNil){ label = this.size.collect{|index| "UGen Array [%]".format(index) } };
		^Poll(trig, this, label, trigid)
	}
}
