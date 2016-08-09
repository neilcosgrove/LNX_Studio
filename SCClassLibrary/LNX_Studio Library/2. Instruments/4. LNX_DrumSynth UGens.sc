
+ LNX_DrumSynth {

	// synth defs //////////////////////////////////////

	*initUGens{|server|
	
		var s=server;

		if (verbose) { "SynthDef loaded: Drum Synth".postln; };

		// kick //////////////////
		
		SynthDef("kick", {	|outputChannels=0,
				 amp=1,
				 note=29,
				 dur=1,
				 mDur=1,
				 attackTime=0.005,
				 attackTime2=1,
				 attackAmount=1,
				 filterScale=1.5,
				 q=0,
				 gate=1,
				 noise=0,
				 pan=0,
				 sendChannels=0,
				 send=0,
				 masterAmp=0,
				 masterPan=0,
				 select=0
				|
		
			var env0, env1, env1m, out, out2;
			
			// amp env
			env0 =  EnvGen.ar(Env.new([0.5, 1, 0.5, 0],
					[0.005*mDur, 0.06*dur*mDur, 0.26*dur*mDur], [-4, -2, -4]),
						gate, doneAction:2);
	
			// filter+oscs pitch
			env1 = EnvGen.ar(Env.new([
									((110-note)*attackAmount)+note,
									(( 59-note)*attackAmount)+note, 
									note
								  ], [attackTime, 0.29*attackTime2], [-4, -5]));
			env1m = env1.midicps;
			
			out = LFPulse.ar(env1m, 0, 0.5, 1, -0.5);
			out = out + (WhiteNoise.ar(1)*noise);
			out = MoogFF.ar(out, env1m*filterScale, q*3.98);
			
			out = out + SinOsc.ar(env1m, 0.5, 1);
			
			out = out *env0;
			out = out.clip2(1);
			out = out * amp;
			
			out=Pan2.ar(out,pan);
			
			masterPan = masterPan*2;
			out2= Pan2.ar(out[0], (masterPan-1).clip(-1,1))
			       + Pan2.ar(out[1], (masterPan+1).clip(-1,1)); // this is local ch chPan
			out2 = out2 * masterAmp;
			
			// so we need chPan and chAmp for ch sends & incase out not to mixer synth
				
			// these may or may not get amp & pan, depends 
			// all out as 1??
			OffsetOut.ar(outputChannels,Select.ar(select,[out,out2])); // main out to mixer
			
			// this won't get amp and pan yet
			OffsetOut.ar(sendChannels,[out2[0]*send,out2[1]*send]); // channel send
			
			
		}).send(s);
		
		
		// snare //////////////////
		
		SynthDef("snare", {|
			outputChannels=0,
			amp=0.8,
			note=49,
			dur=1,
			noiseFilt1=1,
			noiseFilt2=1,
			noiseMix=0.5,
			nDur=1,
			mDur=1,
			attackAmount=1,
			gate=1,
			pan=0,
			sendChannels=0,
			send=0,
			masterAmp=0,
			masterPan=0,
			select=0|
			
			var env0, env1, env2, env1m, oscs, noise, out, out2;
			
			// osc amp env
			env0 = EnvGen.ar(Env.new([0.5, 1, 0.5, 0], [0.005, 0.03, 0.10],
									[-4, -2, -4]),gate,timeScale:dur*mDur);
			
			// osc pitch env
			env1 = EnvGen.ar(Env.new([
									((110-note)*attackAmount)+note,
									(( 60-note)*attackAmount)+note, 
									note
									], [0.005, 0.1], [-4, -5]),timeScale:dur);
			
			env1m = env1.midicps;
			
			// noise amp env
			env2 = EnvGen.ar(Env.new([1, 0.4, 0,0], [0.05*nDur, 0.13*nDur,0.18*(1-nDur)],
							[-2, -2]),gate, doneAction:2,timeScale:dur*mDur);
			
			oscs = LFPulse.ar(env1m, 0, 0.5, 1, -0.5)
				  + LFPulse.ar(env1m * 1.6, 0, 0.5, 0.5, -0.25);
			oscs = LPF.ar(oscs, env1m*1.2, env0);
			oscs = oscs + SinOsc.ar(env1m, 0.8, env0);
			
			noise = WhiteNoise.ar(0.2);
			noise = HPF.ar(noise,  200, 2);
			noise = BPF.ar(noise, (6900*noiseFilt2).clip(0,22000), noiseFilt1, 3);//+noise;
			noise = noise * env2;
			
			out = (noise*((1-noiseMix))) - (oscs*noiseMix);
			out = out.clip2(1) * amp;
			
			out=Pan2.ar(out,pan);
			
			masterPan = masterPan*2;
			out2= Pan2.ar(out[0], (masterPan-1).clip(-1,1))
			       + Pan2.ar(out[1], (masterPan+1).clip(-1,1)); // this is local ch chPan
			out2 = out2 * masterAmp;
			
			OffsetOut.ar(outputChannels,Select.ar(select,[out,out2])); // main out to mixer
					
			OffsetOut.ar(sendChannels,[out2[0]*send,out2[1]*send]);
			
			
		}).send(s);
		
		// clap //////////////////
	
		SynthDef("clap", {
			  | outputChannels=0,
			    amp = 0.5,
			    pan=0,
			    dur=1,
			    sendChannels=0,
			    send=0,
			    gate=1,
			    q=1,
			    fq=1,
			    rnd1=0,
			    rnd2=0,
			    rnd3=0,
			    masterAmp=0,
			    masterPan=0,
			    select=0|
		
			var env1, env2, out, noise1, noise2, out2;
			
			// noise 1 - 4 short repeats
			env1 = EnvGen.ar(Env.new([0, 1, 0, 1, 0, 1, 0, 1, 0], 
					[0.001, 0.013+rnd1, 0, 0.01+rnd2, 0, 0.01+rnd3, 0, 0.03],
					[0, -3, 0, -3, 0, -3, 0, -4]),
					timeScale:dur);
			noise1 = WhiteNoise.ar(env1);
			noise1 = HPF.ar(noise1, 600);
			noise1 = BPF.ar(noise1, 2000*fq, 3*q);
			
			// noise 2 - 1 longer single
			env2 = EnvGen.ar(Env.new([0, 1, 0], [0.02, 0.3], [0, -4]),gate,
					doneAction:2, timeScale:dur);
			noise2 = WhiteNoise.ar(env2);
			noise2 = HPF.ar(noise2, 1000);
			noise2 = BPF.ar(noise2, 1200*fq, 0.7*q, 0.7);
			
			out = noise1 + noise2;
			out = out * 2;
			out = out.softclip * amp;
			
			out=Pan2.ar(out,pan);
			
			masterPan = masterPan*2;
			out2= Pan2.ar(out[0], (masterPan-1).clip(-1,1))
			       + Pan2.ar(out[1], (masterPan+1).clip(-1,1)); // this is local ch chPan
			out2 = out2 * masterAmp;
			
			OffsetOut.ar(outputChannels,Select.ar(select,[out,out2])); // main out to mixer
			
			OffsetOut.ar(sendChannels,[out2[0]*send,out2[1]*send]);
			
		}).send(s);

		// tom //////////////////
		
		SynthDef(\SOStom,
		// recipe basically from Gordon Reid
		// http://www.soundonsound.com/sos/Mar02/articles/synthsecrets0302.asp
		// programmed by Renick Bell, renick_at_gmail.com
			{arg
				outputChannels = 0,
				decay = 0.6,            // dur
				drum_mode_level = 0.25, // change stick amp instead
				note = 55,
				attackAmount = 1,
				eDur = 0.5,
				cFreq = 803,
				slope=0,
				slopeDur=0.25,
				drum_timbre = 3.0,      // 0 - 5 is good
				indexEnv=1,
				amp = 0.8,
				pan=0,
				sendChannels=0,
				send=0,
				gate=1,
				chorus=1.16,
				stick=4,
				masterAmp=0,
				masterPan=0,
				select=0;
			
			var drum_mode_sin_1, drum_mode_sin_2, drum_mode_pmosc, drum_mode_mix,
			drum_mode_env;
			var stick_noise, stick_env;
			var drum_reson, tom_mix;
			var env1, idxEnv, out, out2;
			
			
			// osc pitch env
			env1 = EnvGen.ar(Env.new([((100-note)*attackAmount)+note,note
									], [0.1], -5),timeScale:eDur);
			env1 = env1.midicps;
		
			drum_mode_env = EnvGen.ar(Env.perc(0.005, decay), gate, doneAction: 2);
			
			idxEnv = EnvGen.ar(Env.new([1,slope],[slopeDur],-5));
			
			drum_mode_sin_1 = SinOsc.ar(env1*(chorus/2), 0, drum_mode_env );
			drum_mode_sin_2 = SinOsc.ar(env1/(chorus*2), 0, drum_mode_env );
			drum_mode_pmosc = PMOsc.ar( 	env1.clip(1,22050),
										cFreq.clip(1,22050),
										(drum_timbre/1.3)*idxEnv,
										mul: drum_mode_env*5,
										add: 0);
			drum_mode_mix = Mix([drum_mode_sin_1, drum_mode_sin_2,drum_mode_pmosc]) * drum_mode_level;
			
			
			stick_env = EnvGen.ar(Env.new([0,1,0],[0.005,0.05],-5), 1, stick);
			stick_noise = Crackle.ar(1.99, stick_env);
			
			tom_mix= (drum_mode_mix+stick_noise)*(amp*0.15);
			
			out = Pan2.ar(tom_mix,pan);
			
			
			masterPan = masterPan*2;
			out2= Pan2.ar(out[0], (masterPan-1).clip(-1,1))
			       + Pan2.ar(out[1], (masterPan+1).clip(-1,1)); // this is local ch chPan
			out2 = out2 * masterAmp;
			
			OffsetOut.ar(outputChannels,Select.ar(select,[out,out2])); // main out to mixer
			
			OffsetOut.ar(sendChannels,[out2[0]*send,out2[1]*send]);

			}
		).send(s);
 		
		// hat //////////////////
			
		SynthDef("hat", {	
			arg outputChannels=0,
				amp=0.3,
				dur=0.5,
				noteAdj=(0),
				//mix=0.5, //(0-1)
				lp=0.5, //(0 - 1)
				q=0.4,  //(0-1)
				hp=1,   //(1 - 2)
				gate=1,
				pan=0,
				sendChannels=0,
				send=0,
				masterAmp=0,
				masterPan=0,
				select=0;
		
			var env1, env2, oscs1, noise, n, n2;

			var out=0, baseFreq = 300, time = 250;
			//var freqs = [baseFreq, baseFreq*1.3420, baseFreq*1.2312, baseFreq*1.6532, baseFreq*1.9523, baseFreq*2.1523];
			//var freqs = [78.6, 140.44, 123.87, 219.4, 787.5, 531.3];
			//var freqs = [205.35, 254.29, 294.03, 304.41, 369.64, 522.71];
			
			var freqs; 
			var signal, pulseEnv, out2;
			
			dur=dur/3;
			noteAdj=(noteAdj.midicps)/(0.midicps);
			
			freqs = [205.35*noteAdj, 304.41*noteAdj, 369.64*noteAdj,
						522.71*noteAdj, 540.54*noteAdj, 812.21*noteAdj];
			
			pulseEnv = EnvGen.ar(Env.new([1.0, 0.6], [time], [-0.5]), timeScale:(1/1000*dur));
			signal = Mix.new(LFPulse.ar(freqs * 4.09));
			signal = (BinaryOpUGen('==', signal, 6.0) * 0.6) + (BinaryOpUGen('==', signal, 2.0)
						* 0.2) + (BinaryOpUGen('==', signal, 1.0) * 0.9); // XOR
		   	signal = (signal * pulseEnv) + (Mix.new(LFPulse.ar(freqs, width:0.55)) * 0.9);
		 	signal = RLPF.ar(signal, 7000*lp, 1-q);
		  	signal = RHPF.ar(signal, 6800*hp, 1.5);
		 	signal = RHPF.ar(signal, 6800*hp, 1.5);
		 	signal = RHPF.ar(signal, 1200*hp, 1.5);
			signal = signal + FreeVerb.ar(signal);
			signal = signal * EnvGen.ar(Env.new([0, 1, 0.4, 0, 0], [2, time, 50, 500],
							[0, -0.5, 0, -50]),gate, timeScale:(1/1000*dur), doneAction:2);
	
			out = signal * (amp*3*((hp-0.25)**2)*((2-lp)**1.5));
			out = Pan2.ar(out,pan);
			out = [out[0], DelayN.ar(out[1], 0.002, 0.002)];
			
			
			masterPan = masterPan*2;
			out2= Pan2.ar(out[0], (masterPan-1).clip(-1,1))
			       + Pan2.ar(out[1], (masterPan+1).clip(-1,1)); // this is local ch chPan
			out2 = out2 * masterAmp;
			
			OffsetOut.ar(outputChannels,Select.ar(select,[out,out2])); // main out to mixer
			
			OffsetOut.ar(sendChannels,[out2[0]*send,out2[1]*send]);
			
		}).send(s);
		
	}

} // end ////////////////////////////////////
