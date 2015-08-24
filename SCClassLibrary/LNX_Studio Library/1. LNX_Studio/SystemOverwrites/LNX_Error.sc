
// error handling

LNX_Error {
	
	classvar >reportTo;
	
	*reportError{|error|
		if (reportTo.notNil) {
			reportTo.value(error);
			reportTo=nil;
		}
	}
	
	*remove{|func|
		if (reportTo==func) { reportTo=nil };	
	}
	
}

+ SynthDef {

	checkInputs {
		var seenErr = false, s="";
		children.do { arg ugen;
			var err;
			if ((err = ugen.checkInputs).notNil) {
				seenErr = true;
				s=(ugen.class.asString + err).postln;
				ugen.dumpArgs;
			};
		};
		if(seenErr) {
			Error("SynthDef" + this.name + "build failed.\n"++s).throw;
		};
		^true
	}
}


+ String {
	error {
		"ERROR:\n".post;
		this.postln;
		LNX_Error.reportError("ERROR:"++this);
	}
}
	
+ Exception {
	
	reportError {
		var s; 
		s = this.errorString.postln;
		this.dumpBackTrace;
		LNX_Error.reportError(s);
	}

}

+ MethodError {

	reportError {
		var s;
		s = this.errorString.postln;
		"RECEIVER:\n".post;
		receiver.dump;
		this.dumpBackTrace;		
		s=(s.asString++"\n".findReplace("\n"," ")++"\nRECEIVER: "++(receiver.asString));
		LNX_Error.reportError(s);
	}
	
}

+ DoesNotUnderstandError {
	
	reportError {
		var s;
		
		s=this.errorString.postln;
		
		"RECEIVER:\n".post;
		
		s=s++(" RECEIVER: ");
		s=s++(receiver.asString);
		
		receiver.dump;
		"ARGS:\n".post;
		args.dumpAll;
		
		this.dumpBackTrace;
		
		LNX_Error.reportError(s);
	}
	
}


