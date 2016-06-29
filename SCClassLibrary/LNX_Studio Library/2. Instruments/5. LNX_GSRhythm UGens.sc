
// a grain & sample based drum machine

+ LNX_GSRhythm {

	// synth defs //////////////////////////////////////
	
	*initUGens{|server|
	
		var s=server;
		
		if (verbose) { "SynthDef loaded: LNX_GSRhythm".postln;};
			
		// the filter ////////////////////////////////////////////////////////////////
				
		SynthDef("GSR_Filter",{|inputChannels=4, outputChannels=0, chAmp=1, chPan=0,
							amp=1, pan=0, select=0,
							sendChannels=0, sendAmp=0filterOn=1, filtFreq=20000,
							filtRes=0, drive=1, type=0|
								
			var driveAmp, signal2, signal = In.ar(inputChannels, 2);
			
			signal = DFM1.ar(signal, Lag.kr(filtFreq,0.1), filtRes,drive,type, noiselevel:0);
			
			driveAmp = 1/(drive.linlin(0.5,4,1,18)**0.4);		
			signal = signal * Lag.kr(chAmp*driveAmp);
			
			signal = Pan2.ar(signal[0], (chPan-1).clip(-1,1))
			       + Pan2.ar(signal[1], (chPan+1).clip(-1,1)); // this is local ch chPan
			
			// make a version with master amp & pan so it can be used if needed
			amp    = Lag.kr(amp);
			pan    = Lag.kr(pan*2);
			signal2= Pan2.ar(signal[0], (pan-1).clip(-1,1))
			       + Pan2.ar(signal[1], (pan+1).clip(-1,1)); // this is local ch chPan
			signal2 = signal2 * amp;
			
			// this could need (master pan & master amp) 
			// so we select which one to use
			Out.ar(outputChannels, Select.ar(select, [signal2,signal])); // to instGroupChannel 
			
			// this needs (master pan and master amp)
			Out.ar(sendChannels,[signal2[0]*sendAmp,signal2[1]*sendAmp]); // for ch only
					
		}).send(s);
				
		// the basic version, no filter but we can route to a different out & do channel sends
		// draw backs no eq on different out or channel sends
		
		// so we need to get masterAmp, masterPan and isGoingToMixerSynth coming in her
		
		SynthDef("GSR_NoFilter",{|inputChannels=4, outputChannels=0, chAmp=1, chPan=0,
							amp=1, pan=0, select=0,
							sendChannels=0, sendAmp=0|
								
			var signal2, signal = In.ar(inputChannels, 2);
			
			signal = signal * Lag.kr(chAmp);
			
			chPan  = Lag.kr(chPan*2);
			
			signal = Pan2.ar(signal[0], (chPan-1).clip(-1,1))
			       + Pan2.ar(signal[1], (chPan+1).clip(-1,1)); // this is local ch chPan
			
			// make a version with master amp & pan so it can be used if needed
			amp    = Lag.kr(amp);
			pan    = Lag.kr(pan*2);
			signal2= Pan2.ar(signal[0], (pan-1).clip(-1,1))
			       + Pan2.ar(signal[1], (pan+1).clip(-1,1)); // this is local ch chPan
			signal2 = signal2 * amp;
			
			// this could need (master pan & master amp) 
			// so we select which one to use
			Out.ar(outputChannels, Select.ar(select, [signal2,signal])); // to instGroupChannel 
			
			// this needs (master pan and master amp)
			Out.ar(sendChannels,[signal2[0]*sendAmp,signal2[1]*sendAmp]); // for ch only
					
		}).send(s);
				
		// sample players ////////////////////////////////////////////////////////////////

		// now the output of these is always fixed to Filter/NoFilter

		SynthDef("mono_GSR",{|bufnum=0, amp=1, rate=1, gate=1, velocity=1, loop=0, dur=1,
							outputChannels=0,offset=0, startFrame=0|
			
			var signal, osc, env, envctl, myEnv,driveAmp;
			signal = PlayBuf.ar(1,bufnum,rate*BufRateScale.kr(bufnum),
												loop:loop, startPos:startFrame);
			FreeSelfWhenDone.kr(signal);
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			amp    = Lag.kr(amp*velocity,0.075);         // local ch pan & velocity
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2);
			signal = signal * env * amp;
						
			OffsetOut.ar(outputChannels, signal!2);

		}).send(s);
		
		SynthDef("monoGS_GSR",{|bufnum=0, amp=1, rate=1, gate = 1, velocity=1, loop=0, dur=1,
							outputChannels=0, posRate=1, density=60, rand=0,  
							offset=0, startFrame=0, overlap=2|
					
			var osc, env, envctl, myEnv, driveAmp;
			var gDur, signal, bDur, pos;
			var randD=rand*50;
			var randP=rand/10;
			
			overlap = overlap + (rand*2);
			bDur    = BufDur.ir(bufnum);
			pos     = Integrator.ar( posRate.asAudioRateInput ) / ( SampleRate.ir * bDur);
			gDur    = overlap / density;
			signal  =	PlayBuf.ar(1,bufnum,rate*BufRateScale.ir(bufnum), startPos:startFrame);
			signal  = signal * EnvGen.ar(Env([1,1,0], [0.25,0.25],[0,-5]), timeScale: gDur,
												doneAction: 0);
			signal  = signal + GrainBufJ.ar(
				numChannels:	1,
				loop:		loop,
				trigger:		Impulse.ar( density + LFNoise1.kr.range( randD.neg, randD) ), 
				dur:			gDur, 
				sndbuf: 		bufnum,
				rate: 		rate + LFNoise1.kr.range( randP.neg, randP),
				pos: 		pos + offset, 
				interp: 		2,
				pan: 		0,
				envbufnum: 	-1
			);
			signal = LeakDC.ar(signal);
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			amp    = Lag.kr(amp*velocity,0.075);         // local ch pan & velocity
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2);
			signal = signal * env * amp;
			
			OffsetOut.ar(outputChannels, signal!2);
		
		}).send(s);
				
		//////////////////////////// stereo //////////////////////////////////////////////

		SynthDef("stereo_GSR",{|bufnum=0, amp=1, rate=1, gate=1, velocity=1, loop=0, dur=1,
						outputChannels=0, offset=0, startFrame=0|
			
			var signal, osc, env, envctl, myEnv;
			
			signal = PlayBuf.ar(1,[bufnum, bufnum+1],rate*BufRateScale.kr(bufnum),
												loop:loop, startPos:startFrame);
			FreeSelfWhenDone.kr(signal);
					
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			amp    = Lag.kr(amp*velocity,0.075);         // local ch pan & velocity
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2);
			signal = signal * env * amp;
			
			OffsetOut.ar(outputChannels, signal);

		}).send(s);
		
		SynthDef("stereoGS_GSR",
					{|bufnum=0, amp=1, rate=1, gate = 1, velocity=1, loop=0, dur=1,
						outputChannels=0, posRate=1, density=60, rand=0, offset=0, startFrame=0,
						overlap=2|
					
			var osc, env, envctl, myEnv;
			var gDur, signal, bDur, pos;
			var randD=rand*50;
			var randP=rand/10;
			
			overlap = overlap + (rand*2);
			bDur    = BufDur.ir(bufnum);
			pos     = Integrator.ar( posRate.asAudioRateInput ) / ( SampleRate.ir * bDur);
			gDur    = overlap / density;
			signal  = PlayBuf.ar(1,[bufnum, bufnum+1],rate*BufRateScale.kr(bufnum),
												loop:0, startPos:startFrame);
			signal  = signal * EnvGen.ar(Env([1,1,0], [0.25,0.25],[0,-5]),
				timeScale: gDur, doneAction: 0);
			signal  = signal + GrainBufJ.ar(
				numChannels:	1,
				loop:		loop,
				trigger:		Impulse.ar( density + LFNoise1.kr.range( randD.neg, randD) ), 
				dur:			gDur, 
				sndbuf: 		[bufnum, bufnum+1],
				rate: 		rate + LFNoise1.kr.range( randP.neg, randP),
				pos: 		pos + offset, 
				interp: 		2,
				pan: 		0,
				envbufnum: 	-1
			);
			signal = LeakDC.ar(signal);
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			amp    = Lag.kr(amp*velocity,0.075);         // local ch pan & velocity
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2);
			signal = signal * env * amp;
			
			OffsetOut.ar(outputChannels, signal);

		}).send(s);
						
	}

} // end ////////////////////////////////////
