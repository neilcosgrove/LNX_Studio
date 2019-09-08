
// number funcs used to covert values to strings in MVC_Views

MVC_NumberFunc{

	classvar <>funcs;

	*initClass{

		Class.initClassTree(Spec);
		Class.initClassTree(ControlSpec);

		// any extra specs i want

		Spec.add(\sync,         ControlSpec(-1, 1, 'linear', 0.001, 0, " s") );
		Spec.add(\syncTime,     ControlSpec(0, \sync.asSpec.minval.abs, 'linear', 0.001, 0, " s") );

		Spec.add(\delayms,      ControlSpec(0.0001, 1, 'exp', 0, 0.3, " ms") );
		Spec.add(\duration,     ControlSpec(0.005,  1, 'exp', default:1) );
		Spec.add(\duration2,    ControlSpec(0.005,  2, 'exp', default:1) );
		Spec.add(\duration4,    ControlSpec(0.005,  4, 'exp', default:1) );

		Spec.add(\durationMed,  ControlSpec(0.005,  1, 'exp', default:1) );

		Spec.add(\switch,       ControlSpec(0, 1, 'lin', 1, 0 ,"") );
		Spec.add(\0-1,          ControlSpec(0, 1) );
		Spec.add(\normal,       ControlSpec(0, 1) );
		Spec.add(\MIDInote,     ControlSpec(0, 127, step:1, default: 60) );
		Spec.add(\MIDIcc,       ControlSpec(0, 127, step:1, default: 60) );
		Spec.add(\MIDIvelocity, ControlSpec(0, 127, step:0, default: 0 ) );
		Spec.add(\transpose,    ControlSpec(-24, 24, step:1, default: 0) );
		Spec.add(\pitchAdj,     ControlSpec(-24, 24, step:1, default: 0) );

		Spec.add(\pitchAdj48,   ControlSpec(-48, 48, step:1, default: 0) );

		Spec.add(\pitch   ,     ControlSpec(-24, 24, step:0, default: 0) );
		Spec.add(\LNX_audiobus, ControlSpec(0,LNX_AudioDevices.outputChannelList.size -1,step:1));
		Spec.add(\LNX_audiobusM,ControlSpec(0,LNX_AudioDevices.outputChannelList.size,step:1));
		Spec.add(\db2,          ControlSpec(0.ampdb, 2, \db, units: " dB"));
		Spec.add(\db4,          ControlSpec(-inf, 4, \db, 0, 0, " dB"));
		Spec.add(\db8,          ControlSpec(-inf, 8, \db, 0, 0, " dB"));
		Spec.add(\db6,          ControlSpec(-inf, 6, \db, 0, 0, " dB"));
		Spec.add(\db12,         ControlSpec(-inf, 12, \db, 0, 0, " dB"));
		Spec.add(\db24,         ControlSpec(-inf, 24, \db, 0, 0, " dB"));

		Spec.add(\dbEQ,         ControlSpec(-20, 20, \lin, 0, 0, " dB"));

		Spec.add(\lowFreq,      ControlSpec(0.1, 100, \exp, 0, 6, units: " Hz"));

		Spec.add(\fft,          ControlSpec(7, 13, 'linear', 1, 10, ""));
		Spec.add(\bpm,          ControlSpec(20, 999, default: 120) );

		Spec.add(\octSeq,       ControlSpec(0, 9, step:1, default: 0));
		Spec.add(\noteSeq,      ControlSpec(0, 12, step:1, default: 0));
		Spec.add(\midiNoteSeq,  ControlSpec(-1,127,\lin,1,-1));

	    Spec.add(\eqAmp,        ControlSpec(-24, 24, units: " dB",default: 0));
		Spec.add(\length,       ControlSpec(1,(2**14).asInt,\lin,step:1,default:32));

		// the number box functions

		// asFormatedString is prob quite slow

		funcs=(

			//'nil'			: {|n| n }, // do i need to return nil for some reason?
			'int'  			: {|n| n.asInt },
			'round'			: {|n| n.round(1)},
			'round1'		: {|n| n.round(1)},
			'round0.1'		: {|n| n.round(0.1)},
			'round0.01'		: {|n| n.round(0.01)},
			'round0.001'	: {|n| n.round(0.001)},
			'round10'		: {|n| n.round(10)},
			'round100'		: {|n| n.round(100)},
			'round1000'		: {|n| n.round(1000)},
			'float'			: {|n| n },
			'float0'		: {|n| n.asFormatedString(1,0) },
			'float1'		: {|n| n.asFormatedString(1,1) },
			'float2'		: {|n| n.asFormatedString(1,2) },
			'float3'		: {|n| n.asFormatedString(1,3) },
			'float4'		: {|n| n.asFormatedString(1,4) },
			'float5'		: {|n| n.asFormatedString(1,5) },
			'ampdb'         : {|n| n.ampdb.asFormatedString(1,1)++" dbs" },
			'intSign'		: {|n| (n<=0).if(n.asInt,"+"++(n.asInt.asString)) },
			'floatSign'		: {|n| (n>0).if("+","")++(n.asFormatedString(1,0)) },
			'float1Sign'	: {|n| (n>0).if("+","")++(n.asFormatedString(1,1)) },
			'float2Sign'	: {|n| (n>0).if("+","")++(n.asFormatedString(1,2)) },
			'float3Sign'	: {|n| (n>0).if("+","")++(n.asFormatedString(1,3)) },
			'intPlus1'  	: {|n| n.asInt+1 },

			'stretch'		: {|n| if (n==1) {"x inf"}{"x"++
								((1/(n.map(0,1,2,0))).asFormatedString(1,2)[0..3])}},

			'overlap'		: {|n| "x"++(n.asFormatedString(1,2)[0..3])},

			'choke'  		: {|n| (n==0).if("-",n) },

			'freq'			: {|n|
				case
				{n>=1e6} { (n/1e6).asFormatedString(1,1)++"M" }
				{n>=1e5} { (n/1e3).asFormatedString(1,0)++"k" }
				{n>=1e4} { (n/1e3).asFormatedString(1,1)++"k" }
				{n>=1e3} { (n/1e3).asFormatedString(1,2)++"k" }
				{n>=1e2} {       n.asFormatedString(1,0)      }
				{n>=1e1} {       n.asFormatedString(1,1)      }
				{true}   {       n.asFormatedString(1,2)      }},

			'db' 	: {|n|
				case
				{n==(-inf)} { "-inf" }
				{true}   {       n.asFormatedString(1,1)      }},

			'note'	: {|n| if (n>=0) {n.asNote} {"-"} },

			'switch'	: {|n| if (n>=0.5) {"On"} {"Off"} },

			'delayms'	: {|n| n=n*1000;
				case
					{n>=1e2} { n.asFormatedString(1,0) }
					{n>=1e1} { n.asFormatedString(1,1) }
					{true}   { n.asFormatedString(1,2) }},

			'pan'	: {|n|
				var s;
				case
					{n==0} 			{ s="0" }
					{n==(-1)} 		{ s="L" }
					{n==1} 			{ s="R" }
					{(n<0)&&(n>(-1))}	{ s=n.asFormatedString(1,2) }
					{(n>0)&&(n<1)} 	{ s="+"++(n.asFormatedString(1,2)) };
				s
			},

			'mix'	: {|n|
				var s;
				case
					{n==0} 			{ s="0" }
					{n==(-1)} 		{ s="Wet" }
					{n==1} 			{ s="Dry" }
					{(n<0)&&(n>(-1))}	{ s=n.asFormatedString(1,2) }
					{(n>0)&&(n<1)} 	{ s="+"++(n.asFormatedString(1,2)) };
				s
			},

			'LNX_audiobus' : {|n| LNX_AudioDevices.outputChannelList.at(n) },
			'LNX_audiobusM': {|n|( ["Master"]++LNX_AudioDevices.outputChannelList).at(n) },

			'fft' 	: {|n|2**n},

			's&h'	: {|n| if (n>0) {n.asInt} {"Off"} },

			'jp1': {|n| #["Sine","Tri","Saw","Squ","RND","Noise"][n]},
			'jp2': {|n| #["VCO-2","1+2","VCO-1"][n]},
			'jp3': {|n| #["Env-1","Man","LFO"][n]},
			'jp4': {|n| #["64","32","16","8","4","2"][n]},
			'jp5': {|n| #["Sine","Tri","Saw","Pulse","Squ","Noise"][n]},
			'jp6': {|n| #["Sine","Saw","Pulse","Low Sine","Low Saw","Low Pulse"][n]},
			'jp7': {|n| #["-24db","-12db"][n]},
			'jp8': {|n| #["2","1"][n]},
			'jp9': {|n| #["-ive","+ive"][n]},
			'jp10': {|n| #["Off","Env-1","Env-2","1+2"][n]},
			'jp11': {|n| #["Poly","?","Mono","Unison"][n]},
		)
	}

	*at{|key| ^funcs[key] }

	*add{|key,func| funcs[key]=func }

	*remove{|key| funcs[key]=nil }

	*value{|key,value| ^funcs[key].value(value) }

}
