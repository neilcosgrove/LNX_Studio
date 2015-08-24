
/*(
Help.forgetTree.rebuildTree; // add new category, recompile, execute this & wait 7 seconds
)*/

// Categorisation of other SC UGens by lnx, 2012.

+ AmplitudeMod		{ *categories { ^ #["UGens>Analysis>Amplitude"] } }
+ AverageOutput		{ *categories { ^ #["UGens>Filters>Linear"] } }
+ Balance				{ *categories { ^ #["UGens>Multichannel>Panners"] } }
+ BMoog				{ *categories { ^ #["UGens>Filters>Linear"] } }
+ Breakcore			{ *categories { ^ #["UGens>Buffer"] } }
+ Coyote				{ *categories { ^ #["UGens>Analysis"] } }
+ DFM1				{ *categories { ^ #["UGens>Filters>Linear"] } }
+ Dbrown2				{ *categories { ^ #["UGens>Demand"] } }
+ DoubleNestedAllpassN	{ *categories { ^ #["UGens>Delays"] } }
+ MoogLadder			{ *categories { ^ #["UGens>Filters>Linear"] } }
+ AutoTrack			{ *categories { ^ #["UGens>Analysis","UGens>MachineListening"] } }
+ AnalyseEvents2		{ *categories { ^ #["UGens>Analysis","UGens>MachineListening"] } }
+ CombLP				{ *categories { ^ #["UGens>Delays"] } }
+ Decimator			{ *categories { ^ #["UGens>Filters"] } }
+ Disintegrator		{ *categories { ^ #["UGens>Filters"] } }
+ CQ_Diff				{ *categories { ^ #["UGens>Analysis"] } }
+ DoubleWell			{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ DoubleWell2			{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ DoubleWell3			{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ Fhn2DN				{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ FitzHughNagumo 		{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ EnvDetect			{ *categories { ^ #["UGens>Analysis>Amplitude"] } }
+ FM7				{ *categories { ^ #["UGens>Oscillators"] } }
+ FrameCompare		{ *categories { ^ #["UGens>Analysis", "UGens>FFT"] } }
+ GaussTrig			{ *categories { ^ #["UGens>Generators>Stochastic"] } }
+ Gbman2DN			{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ Gendy4				{ *categories { ^ #["UGens>Generators>Stochastic"] } }
+ Goertzel			{ *categories { ^ #["UGens>FFT"] } }
+ Henon2DN			{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ IIRFilter			{ *categories { ^ #["UGens>Filters>Linear"] } }
+ Latoocarfian2DN		{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ Lorenz2DN			{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ LPF1				{ *categories { ^ #["UGens>Filters>Linear"] } }
+ LPFVS6				{ *categories { ^ #["UGens>Filters>Linear"] } }
+ LPF18				{ *categories { ^ #["UGens>Filters>Linear"] } }
+ MdaPiano			{ *categories { ^ #["UGens>Oscillators"] } }
+ MembraneCircle		{ *categories { ^ #["UGens>Generators>PhysicalModels"] } }
+ MoogVCF				{ *categories { ^ #["UGens>Filters>Linear"] } }
+ PV_CommonMag		{ *categories { ^ #["UGens>FFT"] } }
+ PV_Compander		{ *categories { ^ #["UGens>FFT"] } }
+ PV_SoftWipe			{ *categories { ^ #["UGens>FFT"] } }
+ PV_MagGate			{ *categories { ^ #["UGens>FFT"] } }
+ PV_MagMinus			{ *categories { ^ #["UGens>FFT"] } }
+ PV_MagScale			{ *categories { ^ #["UGens>FFT"] } }
+ PV_Morph			{ *categories { ^ #["UGens>FFT"] } }
+ PV_XFade			{ *categories { ^ #["UGens>FFT"] } }
+ LFBrownNoise0		{ *categories { ^ #["UGens>Generators>Stochastic"] } }
+ LoopBuf				{ *categories { ^ #["UGens>Buffer"] } }
+ MarkovSynth			{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ WeaklyNonlinear		{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ WeaklyNonlinear2		{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ TermanWang			{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ WaveletDaub			{ *categories { ^ #["UGens>Filters>Nonlinear"] } }
+ WarpZ				{ *categories { ^ #["UGens>Buffer", "UGens>Generators>Granular"] } }
+ WAmp 				{ *categories { ^ #["UGens>Analysis>Amplitude"] } }
+ WalshHadamard		{ *categories { ^ #["UGens>Filters>Linear"] } }
+ NTube				{ *categories { ^ #["UGens>Generators>PhysicalModels"] } }
+ TwoTube				{ *categories { ^ #["UGens>Generators>PhysicalModels"] } }
+ TrigAvg				{ *categories { ^ #["UGens>Triggers"] } }
+ TPV				{ *categories { ^ #["UGens>FFT"] } }
+ TGrains2 			{ *categories { ^ #["UGens>Buffer", "UGens>Generators>Granular"] } }
+ TGaussRand			{ *categories { ^ #["UGens>Random"] } }
+ TBrownRand			{ *categories { ^ #["UGens>Random"] } }
+ TBetaRand			{ *categories { ^ #["UGens>Random"] } }
+ Tartini				{ *categories { ^ #["UGens>Analysis>Pitch", "UGens>MachineListening"] } }
+ SwitchDelay			{ *categories { ^ #["UGens>Delays"] } } 
+ SVF				{ *categories { ^ #["UGens>Filters>Linear"] } }
+ Streson				{ *categories { ^ #["UGens>Filters>Linear"] } }
+ Standard2DN			{ *categories { ^ #["UGens>Generators>Chaotic"] } }
+ SMS				{ *categories { ^ #["UGens>FFT"] } }
+ SmoothDecimator		{ *categories { ^ #["UGens>Filters"] } }
+ SawDPW				{ *categories { ^ #["UGens>Generators>Deterministic"] } }
+ RLPFD				{ *categories { ^ #["UGens>Filters>Linear"] } }
+ Qitch				{ *categories { ^ #["UGens>Analysis>Pitch", "UGens>MachineListening"] } }
+ Max				{ *categories { ^ #["UGens>Analysis>Amplitude"] } }
+ Maxamp				{ *categories { ^ #["UGens>Info"] } }
+ Metro				{ *categories { ^ #["UGens>Triggers"] } }
+ NLFiltN 			{ *categories { ^ #["UGens>Filters>Nonlinear"] } }
+ Perlin3				{ *categories { ^ #["UGens>Noise"] } }
+ PrintVal			{ *categories { ^ #["UGens>Info"] } }
+ SelectL				{ *categories { ^ #["UGens>Multichannel>Select"] } }
+ SLOnset				{ *categories { ^ #["UGens>Analysis","UGens>MachineListening"] } }
+ LNX_Out				{ *categories { ^ #["UGens>InOut"] } }
+ Fb					{ *categories { ^ #["UGens>Delays","UGens>InOut"] } }
+ Protect				{ *categories { ^ #["UGens>InOut","UGens>Filters"] } }


