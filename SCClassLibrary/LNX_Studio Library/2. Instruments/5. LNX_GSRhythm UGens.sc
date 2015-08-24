
// a grain & sample based drum machine

+ LNX_GSRhythm {

	// synth defs //////////////////////////////////////
	
	*initUGens{|server|
	
		var s=server;
		
		if (verbose) { "SynthDef loaded: LNX_GSRhythm".postln;};
			
		// filters ////////////////////////////////////////////////////////////////
			
		SynthDef("GSR_Filter",{|inputChannels=4, outputChannels=0,
							
								amp=1,
							
								sendChannels=0, sendAmp=0,
								masterSendChannels=0, masterSendAmp=0,
							
								filterOn=1, filtFreq=20000,
								filtRes=0, drive=1, type=0|
								
			var driveAmp;
			var signal = In.ar(inputChannels, 2);
			
			signal = DFM1.ar(signal, Lag.kr(filtFreq,0.1), filtRes,drive,type, 
											noiselevel:0);
			
			driveAmp = 1/(drive.linlin(0.5,4,1,18)**0.4);
			
			signal = signal * Lag.kr(amp*driveAmp);
			
			sendAmp       = Lag.kr(sendAmp,0.075);
			masterSendAmp = Lag.kr(masterSendAmp,0.075);
			
			Out.ar(outputChannels,[signal[0],        signal[1]        ]);
			Out.ar(  sendChannels,[signal[0]*sendAmp,signal[1]*sendAmp]);
			Out.ar(  masterSendChannels,[signal[0]*masterSendAmp,signal[1]*masterSendAmp]);
			
		}).send(s);
					
// new style //
				
		// sample players ////////////////////////////////////////////////////////////////

		SynthDef("mono-F",{|bufnum=0, rate=1, gate=1, velocity=1, loop=0, dur=1, pan=0,
							outputChannels=0,offset=0, startFrame=0|
			
			var signal, osc, env, envctl, myEnv,driveAmp;
			
			signal = PlayBuf.ar(1,bufnum,rate*BufRateScale.kr(bufnum),
												loop:loop, startPos:startFrame);
			FreeSelfWhenDone.kr(signal);
			
			pan    = Lag.kr(pan,0.075);
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2) * velocity;

			signal = signal * env;

			signal = Pan2.ar(signal,pan);	
			OffsetOut.ar(outputChannels, signal);
			
		}).send(s);
		
		SynthDef("monoGS-F",{|bufnum=0, rate=1, gate = 1, velocity=1, loop=0, dur=1, pan=0,
							outputChannels=0, posRate=1, density=60, rand=0,  
							offset=0, startFrame=0, overlap=2|
					
			var osc, env, envctl, myEnv, driveAmp;
			var gDur, signal, bDur, pos;
			
			var randD=rand*50;
			var randP=rand/10;
			
			overlap=overlap + (rand*2);
	
			bDur = BufDur.ir(bufnum);
			
			pos  = Integrator.ar( posRate.asAudioRateInput ) / ( SampleRate.ir * bDur);
			
			gDur = overlap / density;
			
			signal =	PlayBuf.ar(1,bufnum,rate*BufRateScale.ir(bufnum), startPos:startFrame);
			
			signal = signal * EnvGen.ar(Env([1,1,0], [0.25,0.25],[0,-5]),
				timeScale: gDur, doneAction: 0);
			
			signal = signal + GrainBufJ.ar(
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
			
			pan    = Lag.kr(pan,0.075);
			
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2) * velocity;
			
			signal = signal * env;
			
			signal = Pan2.ar(signal,pan);	
			OffsetOut.ar(outputChannels, signal);
			
		}).send(s);
				
		//////////////////////////// stereo //////////////////////////////////////////////

		SynthDef("stereo-F",{|bufnum=0, rate=1, gate=1, velocity=1, loop=0, dur=1, pan=0,
						outputChannels=0,
						offset=0, startFrame=0|
			
			var signal, osc, env, envctl, myEnv;
			
			signal = PlayBuf.ar(1,[bufnum, bufnum+1],rate*BufRateScale.kr(bufnum),
												loop:loop, startPos:startFrame);
			FreeSelfWhenDone.kr(signal);
			
			pan    = Lag.kr(pan,0.075);
			
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2) * velocity;

			signal = signal * env;

			signal[0]=signal[0]*( (1-pan).clip(0,1) );
			signal[1]=signal[1]*( (1+pan).clip(0,1) );
							
			signal = Pan2.ar(signal[0], (pan-1).clip(-1,1))
			       + Pan2.ar(signal[1], (pan+1).clip(-1,1));
			
			OffsetOut.ar(outputChannels, signal);
			
		}).send(s);
		
		SynthDef("stereoGS-F",
					{|bufnum=0, rate=1, gate = 1, velocity=1, loop=0, dur=1, pan=0,
						outputChannels=0,
						posRate=1, density=60, rand=0, offset=0, startFrame=0,
						overlap=2|
					
			var osc, env, envctl, myEnv;
			var gDur, signal, bDur, pos;
			
			var randD=rand*50;
			var randP=rand/10;
			
			overlap=overlap + (rand*2);
	
			bDur = BufDur.ir(bufnum);
			
			pos  = Integrator.ar( posRate.asAudioRateInput ) / ( SampleRate.ir * bDur);
			
			gDur = overlap / density;
			
			signal = PlayBuf.ar(1,[bufnum, bufnum+1],rate*BufRateScale.kr(bufnum),
												loop:0, startPos:startFrame);
			
			signal = signal * EnvGen.ar(Env([1,1,0], [0.25,0.25],[0,-5]),
				timeScale: gDur, doneAction: 0);
			
			signal = signal + GrainBufJ.ar(
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
			
			pan    = Lag.kr(pan,0.075);
			
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2) * velocity;
			
			signal = signal * env;
			
			signal = Pan2.ar(signal[0], (pan-1).clip(-1,1))
			       + Pan2.ar(signal[1], (pan+1).clip(-1,1));
			
			OffsetOut.ar(outputChannels, signal);
			
		}).send(s);

 // old style //	
			
		// sample players ////////////////////////////////////////////////////////////////
		
		SynthDef("monoNF",{|bufnum=0, rate=1, amp=1, gate=1, velocity=1, loop=0, dur=1, pan=0,
						sendChannels=0, sendAmp, outputChannels=0,
						filtFreq=20000, filtRes=1, drive=1, type=0, offset=0,
						startFrame=0, masterSendChannels=0, masterSendAmp|
			
			var signal, osc, env, envctl, myEnv;
			
			signal = PlayBuf.ar(1,bufnum,rate*BufRateScale.kr(bufnum),
												loop:loop, startPos:startFrame);
			FreeSelfWhenDone.kr(signal);
			
			amp           = Lag.kr(amp*velocity,0.075);
			pan           = Lag.kr(pan*2,0.075);
			sendAmp       = Lag.kr(sendAmp,0.075);
			masterSendAmp = Lag.kr(masterSendAmp,0.075);
			
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2) * amp;
			
			signal = signal * env;
			
			signal = Pan2.ar(signal,pan);	
			OffsetOut.ar(outputChannels,[signal[0],        signal[1]        ]);
			OffsetOut.ar(  sendChannels,[signal[0]*sendAmp,signal[1]*sendAmp]);
			OffsetOut.ar(  masterSendChannels,[signal[0]*masterSendAmp,signal[1]*masterSendAmp]);
			
		}).send(s);
		
		SynthDef("monoGSNF",{|bufnum=0, rate=1,amp = 1, gate = 1, velocity=1, loop=0, dur=1, pan=0,
						sendChannels=0, sendAmp, outputChannels=0,
						posRate=1, density=60, rand=0, filtFreq=20000, filtRes=1, drive=1,
						type=0,  offset=0, startFrame=0, overlap=2,
						masterSendChannels=0, masterSendAmp|
					
			var osc, env, envctl, myEnv;
			var gDur, signal, bDur, pos;
			
			var randD=rand*50;
			var randP=rand/10;
			
			overlap=overlap + (rand*2);
	
			bDur = BufDur.ir(bufnum);
			
			pos  = Integrator.ar( posRate.asAudioRateInput ) / ( SampleRate.ir * bDur);
			
			gDur = overlap / density;
			
			signal =	PlayBuf.ar(1,bufnum,rate*BufRateScale.ir(bufnum), startPos:startFrame);
			
			signal = signal * EnvGen.ar(Env([1,1,0], [0.25,0.25],[0,-5]),
				timeScale: gDur, doneAction: 0);
			
			signal = signal + GrainBufJ.ar(		
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
						
			amp           = Lag.kr(amp*velocity,0.075);
			pan           = Lag.kr(pan*2,0.075);
			sendAmp       = Lag.kr(sendAmp,0.075);
			masterSendAmp = Lag.kr(masterSendAmp,0.075);
			
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2) * amp;
			
			signal = signal * env;
			
			signal = Pan2.ar(signal,pan);	
			OffsetOut.ar(outputChannels,[signal[0],        signal[1]        ]);
			OffsetOut.ar(  sendChannels,[signal[0]*sendAmp,signal[1]*sendAmp]);
			OffsetOut.ar(  masterSendChannels,[signal[0]*masterSendAmp,signal[1]*masterSendAmp]);
			
		}).send(s);
				
		//////////////////////////// stereo //////////////////////////////////////////////
		
		SynthDef("stereoNF",{|bufnum=0, rate=1, amp=1, gate=1, velocity=1, loop=0, dur=1, pan=0,
						sendChannels=0, sendAmp, outputChannels=0,
						filtFreq=20000, filtRes=1, drive=1, type=0, offset=0,
						startFrame=0, masterSendChannels=0, masterSendAmp|
			
			var signal, osc, env, envctl, myEnv;
			
			signal = PlayBuf.ar(1,[bufnum, bufnum+1],rate*BufRateScale.kr(bufnum),
												loop:loop, startPos:startFrame);
			FreeSelfWhenDone.kr(signal);
			
			amp           = Lag.kr(amp*velocity,0.075);
			pan           = Lag.kr(pan*2,0.075);
			sendAmp       = Lag.kr(sendAmp,0.075);
			masterSendAmp = Lag.kr(masterSendAmp,0.075);
			
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2) * amp;
			
			signal = signal * env;
						
			signal = Pan2.ar(signal[0], (pan-1).clip(-1,1))
			       + Pan2.ar(signal[1], (pan+1).clip(-1,1));
			
			OffsetOut.ar(outputChannels,[signal[0],        signal[1]        ]);
			OffsetOut.ar(  sendChannels,[signal[0]*sendAmp,signal[1]*sendAmp]);
			OffsetOut.ar(  masterSendChannels,[signal[0]*masterSendAmp,signal[1]*masterSendAmp]);
			
		}).send(s);
		
		SynthDef("stereoGSNF",{|bufnum=0, rate=1,amp = 1, gate = 1, velocity=1, loop=0, dur=1,
							pan=0, sendChannels=0, sendAmp, outputChannels=0,
							posRate=1, density=60, rand=0, filtFreq=20000, filtRes=1, drive=1, 
							type=0,  offset=0, startFrame=0, overlap=2,
							masterSendChannels=0, masterSendAmp|
					
			var osc, env, envctl, myEnv;
			var gDur, signal, bDur, pos;
			
			var randD=rand*50;
			var randP=rand/10;
			
			overlap=overlap + (rand*2);
	
			bDur = BufDur.ir(bufnum);
			
			pos  = Integrator.ar( posRate.asAudioRateInput ) / ( SampleRate.ir * bDur);
			
			gDur = overlap / density;
			
			signal = PlayBuf.ar(1,[bufnum, bufnum+1],rate*BufRateScale.kr(bufnum),
												loop:0, startPos:startFrame);
			
			signal = signal * EnvGen.ar(Env([1,1,0], [0.25,0.25],[0,-5]),
				timeScale: gDur, doneAction: 0);
			
			signal = signal + GrainBufJ.ar(
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
						
			amp           = Lag.kr(amp*velocity);
			pan           = Lag.kr(pan*2,0.075);
			sendAmp       = Lag.kr(sendAmp,0.075);
			masterSendAmp = Lag.kr(masterSendAmp,0.075);
			
			myEnv  = Env.newClear(4);
			envctl = Control.names([\newEnv]).kr( myEnv.asArray );
			env    = EnvGen.ar(envctl,gate,1,timeScale:dur,doneAction: 2) * amp;
			
			signal = signal * env;
			
			signal = Pan2.ar(signal[0], (pan-1).clip(-1,1))
			       + Pan2.ar(signal[1], (pan+1).clip(-1,1));
			
			OffsetOut.ar(outputChannels,[signal[0],        signal[1]        ]);
			OffsetOut.ar(  sendChannels,[signal[0]*sendAmp,signal[1]*sendAmp]);
			OffsetOut.ar(  masterSendChannels,[signal[0]*masterSendAmp,signal[1]*masterSendAmp]);
			
		}).send(s);
						
	}

} // end ////////////////////////////////////
