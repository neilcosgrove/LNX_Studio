
// a grain & sample based drum machine

LNX_GSRhythm : LNX_InstrumentTemplate {

	classvar <samples, <defaultSteps=32, <defaultChannels=8, ampSpec, freqSpec;
	classvar filtFreqSpec, overlapSpec;
	
	var <steps, <channels, <sequencers, <modSequencers, <channelNames;
	var <channelOnSolo, <channelOnSoloGroup, <modValues, <synthArgFuncs, <synthArgValues;
	var <chokeLamp, <mutateModel, <userBanks, <webBrowsers, <channelBanks;
	var <lastGoodSample, <lastGoodAmp, <lastGoodOffset, <lastGoodStartFrame, <lastGoodLoop;
	var <lastTask, <lastTaskFunc, <lastMetaWindow;
	var <filterNodes, <filterBuses, <filterModel, <voicer, <filterOffTasks;

	*initClass {
		Class.initClassTree(Spec);
		// specs to do unmap in synth arg funcs seq update
		ampSpec=\db.asSpec;
		freqSpec=[15,200,\exp,0,60," Hz"].asSpec;
		filtFreqSpec=\freq.asSpec;
		overlapSpec=[1,12,\exp].asSpec;
	}
	
	// post studio server init (useful for loading LNX_BufferProxys)
	*initServer{|server|
		samples = [
			String.scDir+/+"sounds/Roland/505/505 Kit",
			String.scDir+/+"sounds/Roland/606/606 Kit",
			String.scDir+/+"sounds/Roland/707/707 Kit",
			String.scDir+/+"sounds/Roland/808/808 Kit",
			String.scDir+/+"sounds/Roland/909/909 Kit",
			String.scDir+/+"sounds/Roland/Kicks",
			String.scDir+/+"sounds/Roland/Snares",
			String.scDir+/+"sounds/Roland/Rims",
			String.scDir+/+"sounds/Roland/Claps",
			String.scDir+/+"sounds/Roland/Toms",
			String.scDir+/+"sounds/Roland/Misc",
			String.scDir+/+"sounds/Roland/Hats",
			String.scDir+/+"sounds/Roland/Cymbs"
		].collect{|path|
			LNX_SampleBank(server,path)
		};
	}

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id;
		^super.new(server,studio,instNo,bounds,open,id)
	}

	*studioName {^"GS Rhythm"}
	*sortOrder{^1}
	isInstrument{^true}
	canBeSequenced{^true}
	isMixerInstrument{^true}
	mixerColor{^Color(0.75,0.75,1,0.4)} // colour in mixer
	hasLevelsOut{^true}

	// define your document header details
	header { 
		instrumentHeaderType="SC GSRhythm Doc";
		version="v1.5";		
	}

	// an immutable list of methods available to the network
	interface{^#[\netChannelOnOff, \netChannelSolo]}

   	// anything thats needed before the models are made
	initPreModel{
		
		// user content !!!!!!!		
		userBanks = {|i|
			LNX_SampleBank(server,apiID:((id++"_url_"++i).asSymbol))
				.selectedAction_{|bank,v,send=true|
					// when an item is selected
					models[100+i].doValueAction_(13,send:send);
					models[108+i].doValueAction_(v,send:send);
				}
				.itemAction_{|bank,items,send=false| // i don't think send is needed here
					// when a sample is added or removed from the bank
					this.updateSampleControlSpec(i);
					models[100+i].doValueAction_(13,send:send);// send was true
				}
				.title_("User: "++((i+1).asString))
		} ! defaultChannels;
		
		// the webBrowsers used to search for new sounds!
		webBrowsers = userBanks.collect{|bank,i| LNX_WebBrowser(server,bank) };
		
		// channel banks are the drum banks + the user banks for each channel
		channelBanks = [];
		defaultChannels.do{|i| channelBanks = channelBanks.add( samples.copy.add(userBanks[i]) ) };
		
		lastGoodSample     = nil ! defaultChannels;
		lastGoodAmp        = nil ! defaultChannels;
		lastGoodOffset     = nil ! defaultChannels;
		lastGoodLoop       = nil ! defaultChannels;
		lastGoodStartFrame = nil ! defaultChannels;
		lastTask           = nil ! defaultChannels;
		lastTaskFunc       = nil ! defaultChannels;
		lastMetaWindow     = nil ! defaultChannels;
		filterOffTasks     = nil ! defaultChannels;
		
	}

	// when samples are added to a user bank, the controlSpec needs updating
	// 108-115. channel sample
	updateSampleControlSpec{|i|
		models[108+i].controlSpec_([0,channelBanks[i].collect(_.size).sort.last,\linear,1])
	}

	updateAllSampleControlSpecs{ defaultChannels.do{|i| this.updateSampleControlSpec(i)} }

	iInitVars{
		
		// will i ever allow a variable number of channels (alot easier not to)
		channels = defaultChannels;

		channelNames=["Bass","Snare","Clap","Rimshot","Cowbell","Closed","Open", "Cymbal"
					].collect{|i,j| (j+1)++"."++i};
		
		// the onSolos and their container
		channelOnSoloGroup=LNX_OnSoloGroup();
		channelOnSolo={|i|
			LNX_OnSolo(channelOnSoloGroup,p[20+i],p[12+i],i)
		} ! defaultChannels;
					
		// the lamps
		gui[\lamps]=LNX_EmptyGUIShell!defaultChannels;	
		
		// the main sequencer
		sequencers={|i|
			MVC_StepSequencer((id.asString++"_Seq_"++i).asSymbol,
								defaultSteps,midiControl,1000+(1000*i), \unipolar )
				.name_("")
				.action_{|velocity,latency| this.bang(i,velocity,latency,true) }
				.nameClickAction_{|seq|
					gui[\tabView].value_(i);
					gui[\tabView2].value_(i);
					
					//webBrowsers[i].open
				}
		}! channels;
		
		// the modulation sequencer
		modSequencers={|i|
			MVC_StepSequencer((id.asString++"_Adj_"++i).asSymbol,
								defaultSteps,midiControl,100000+(1000*i), \unipolar, true )
				.name_(channelNames[i])
				.action_{|val,latency|
					if (modValues[i]!=val) {
						modValues[i]=val;	
						this.updateSynthArg(\rate,i,latency);
						this.updateSynthArg(\sendAmp,i,latency);
						this.updateSynthArg(\posRate,i,latency);
						this.updateSynthArg(\density,i,latency);
						this.updateSynthArg(\overlap,i,latency);
						this.updateFilterArg(\filtFreq,i,latency);
					}
				};
		}! channels;

		// synth stuff
		modValues       = 0 ! defaultChannels;
		synthArgFuncs   = IdentityDictionary[];
		synthArgValues  = {IdentityDictionary[]} ! defaultChannels; // last vals - test for updates
		chokeLamp		  = {[0,0]} ! defaultChannels;
		
		mutateModel = [50,[1,100,\lin,1]].asModel;
		
		// the voicer which controls the mono channels & uses the choke models
		voicer = LNX_GSRVoicer(server,8)
					.chokeModels_(models[92..99])
					.onKillAction_{|i|
						lastTask[i].stop;
						lastTaskFunc[i].value;
					};
		
		this.initSynthArgFuncs;
		
	}
		
	// functions to calculate synth args
	initSynthArgFuncs{
	
		// out channel
		synthArgFuncs[\outputChannels]={|i|
			var out, sameAsMaster;
			
			case {p[36+i]==0} {
					out = this.instGroupChannel; // select group channel if master
				}
				{p[36+i]>0} {
					out = (p[36+i]-1)*2; // or get drum channel out
				}{
					out = LNX_AudioDevices.firstFXBus+(p[36+i].neg*2-2); // or drum ch fx channel
				};
				
			if (p[3]>=0) {
				sameAsMaster = p[3]*2; // get master out anyway
			}{	
				sameAsMaster = LNX_AudioDevices.firstFXBus+(p[3].neg*2-2); // or if its an fx
			};
			
			if (sameAsMaster==out) { out = this.instGroupChannel}; // if ch = master use instGroup	
			out
		};
		
		// send channels
		synthArgFuncs[\sendChannels]={|i| LNX_AudioDevices.getOutChannelIndex(p[76+i]) };
		
		// master send channels
		synthArgFuncs[\masterSendChannels]={|i| LNX_AudioDevices.getOutChannelIndex(p[287]) };
	
		synthArgFuncs[\rate]={|i| (p[6]+p[68+i]+(modValues[i]*p[196+i])).midicps/0.midicps };
		synthArgFuncs[\amp ]={|i| 
			(
				(if (lastGoodAmp[i].notNil) {lastGoodAmp[i].dbamp} {nil})
				? ({(channelBanks[i][p[100+i]].amp(p[108+i]).dbamp)}.try ? 1) 
							// try stop error on load
			) *(p[28+i].dbamp)*(p[2].dbamp) 
		};
		synthArgFuncs[\pan ]={|i|
			if (p[4]>=0) { p[44+i].map(-1,1,p[4]*2-1,1) }{ p[44+i].map(-1,1,-1,1+(p[4]*2)) }};
			
			
		synthArgFuncs[\sendAmp]={|i|
			p[84+i].dbamp +( p[212+i].dbamp * ampSpec.map(modValues[i]).dbamp)};
			
		synthArgFuncs[\masterSendAmp]={|i| p[288].dbamp} ;
			
		synthArgFuncs[\filtFreq ]={|i|
			if (p[236+i]==1) {
				if (p[244+i]>=0) {
					filtFreqSpec.map(
						(modValues[i]*p[244+i]).map(0,1,filtFreqSpec.unmap(p[124+i]),1))
				}{
					filtFreqSpec.map(
						(modValues[i]*p[244+i].abs).map(0,1,filtFreqSpec.unmap(p[124+i]),0)) 
				};
			}{	
				p[233]
			}
		};
		synthArgFuncs[\filtRes  ]={|i| if (p[236+i]==1) {p[132+i]} {p[234]} };
		synthArgFuncs[\drive    ]={|i| if (p[236+i]==1) {p[140+i]} {p[235]} };
		synthArgFuncs[\posRate]={|i|
			if ((p[148+i]==0)and:{p[228]==1}) {
				p[229].map(0,1,2,0)
			}{	
				(p[156+i].map(0,1,2,0))
				*
				(modValues[i].map(0,1,1, p[220+i].map(0,1,2,0)))
			}
		};
		synthArgFuncs[\density]={|i|
			if ((p[148+i]==0)and:{p[228]==1}) {
				p[230]
			}{
				if (p[172+i]>=0) {
					freqSpec.map((modValues[i]*p[172+i]).map(0,1,freqSpec.unmap(p[164+i]),1))
				}{
					freqSpec.map((modValues[i]*p[172+i].abs).map(0,1,freqSpec.unmap(p[164+i]),0)) 
				};
			}
		};
		synthArgFuncs[\rand]={|i| 
			if ((p[148+i]==0)and:{p[228]==1}) {
				p[231]
			}{
				p[180+i]
			}
		};
		synthArgFuncs[\overlap]={|i|
			if ((p[148+i]==0)and:{p[228]==1}) {
				p[260]
			}{
				if (p[269+i]>=0) {
					overlapSpec.map((modValues[i]*p[269+i])
						.map(0,1,overlapSpec.unmap(p[261+i]),1))
				}{
					overlapSpec.map((modValues[i]*p[269+i].abs)
						.map(0,1,overlapSpec.unmap(p[261+i]),0)) 
				};
			}
		};
		synthArgFuncs[\type]={|i| (p[236+i]==1).if(p[188+i],p[232]) }
			
	}
		
	// disk i/o ///////////////////////////////
		
	// for your own saving
	iGetSaveList{
		var l;
		l=[channels];
		sequencers.do{|s| l=l++(s.getSaveList) };	
		modSequencers.do{|s| l=l++(s.getSaveList) };	
		userBanks.do{|bank| l=l++(bank.getSaveListURL) };
		^l	
	}
	
	// for your own loading
	iPutLoadList{|l,noPre,loadVersion,templateLoadVersion|
		var channels;		
		channels=l.popI; // not really used yet 
		sequencers.do{|s| s.putLoadList(l.popEND("*** END OBJECT DOC ***")) };
		modSequencers.do{|s| s.putLoadList(l.popEND("*** END OBJECT DOC ***")) };
		if (loadVersion>=1.5) {
			userBanks.do{|bank,i|
				bank.putLoadListURL( l.popEND("*** END URL Bank Doc ***") );
			}		
		};
	}
	
	// anything that needs doing after a load
	iPostLoad{|noPre,loadVersion,templateLoadVersion|
		var cs, cb;
		this.arrangeWindow;
		channels.do{|i| channelOnSolo[i].on_(p[20+i]).solo_(p[12+i])  };
		this.refreshOnOffEnabled;
		
		// for older versions of gs update old samples to new samples
		if (loadVersion==1.3) {
			"updating GS to 1.4".postln;
			// 100-107. channel bank
			cb=(0:3,1:12);
			(100..107).do{|i|
				models[i].lazyValueAction_( cb[ p[i].asInt ]);
				noPre.do{|j|
					presetMemory[j][i] = cb[ presetMemory[j][i].asInt ];
				};
			};
			// 108-115. channel sample
			cs=(0:0,1:2,2:4,3:5,4:6,5:15,6:16,7:18,8:13,9:10,10:11,11:8,12:14,13:12,14:9);
			(108..115).do{|i|
				models[i].lazyValueAction_( cs[ p[i].asInt ]);
				noPre.do{|j|
					presetMemory[j][i] = cs[ presetMemory[j][i].asInt ];
				};
			};
		};	
	}
	
	iFreeAutomation{
		sequencers.do(_.freeAutomation);
		modSequencers.do(_.freeAutomation);
	}
	
	// free this
	iFree{
		sequencers.do(_.free);
		modSequencers.do(_.free);
		channelOnSolo.do(_.free);
		userBanks.do(_.free);
		channelOnSoloGroup = channelOnSolo = nil;
		{LNX_SampleBank.emptyTrash}.defer(1); // empty trash 1 seconds later
	}
	
	// PRESETS /////////////////////////
	
	// get the current state as a list
	iGetPresetList{
		var l;
		l=[sequencers[0].iGetPrestSize];
		sequencers.do{|s| l=l++(s.iGetPresetList) };
		modSequencers.do{|s| l=l++(s.iGetPresetList) };
		^l
	}
	
	// add a statelist to the presets
	iAddPresetList{|l|
		var presetSize;
		presetSize=l.popI;
		sequencers.do{|s| s.iAddPresetList(l.popNF(presetSize)) };
		modSequencers.do{|s| s.iAddPresetList(l.popNF(presetSize)) };
	}
	
	// save a state list over a current preset
	iSavePresetList{|i,l|
		var presetSize;
		presetSize=l.popI;
		sequencers.do{|s| s.iSavePresetList(i,l.popNF(presetSize)) };
		modSequencers.do{|s| s.iSavePresetList(i,l.popNF(presetSize)) };
	}
	
	// for your own load preset
	iLoadPreset{|i,newP,latency|
		// maybe update so we keep seq
		sequencers.do{|s| s.iLoadPreset(i) };
		modSequencers.do{|s| s.iLoadPreset(i) };
	}
	
	// for your own remove preset
	iRemovePreset{|i|
		sequencers.do{|s| s.iRemovePreset(i) };
		modSequencers.do{|s| s.iRemovePreset(i) }; 
	}
	
	// for your own removal of all presets
	iRemoveAllPresets{ 
		sequencers.do{|s| s.iRemoveAllPresets };
		modSequencers.do{|s| s.iRemoveAllPresets }; 
	}
	
	// clear the sequencer
	clearSequencer{
		sequencers.do(_.clearSequencer);
		modSequencers.do(_.clearSequencer);
	}
	
	// mutate the seq (0=0% -> 1=100%)
	mutate{
		var amount = mutateModel.value/100;
		var indexs, hits, hit, steps;
		steps=sequencers[0].steps;
		indexs=sequencers.collect{|s| (0..(steps-1)).difference(s.seq.indicesOfEqual(0)) };
		hits=[];
		indexs.do{|sublist,y|
			sublist.do{|x|
				hits=hits.add( (\x:x,\y:y,\value:sequencers[y].seq[x]));
			}
		};
		(hits.size*amount).asInt.do{
			[{
				// add (to do: exclude unused tracks)
				sequencers[defaultChannels.rand].setStep(32.rand,1.0.rand**0.5);
			},{
				// delete
				hit=hits.choose;
				sequencers[hit[\y]].setStep(hit[\x],0);
			},{
				// move
				hit=hits.choose;
				sequencers[hit[\y]].setStep(hit[\x],0);
				hit[\x]=(hit[\x]+4.rand2*(2.rand+1)).asInt.wrap(0,steps-1);
				sequencers[hit[\y]].setStep(hit[\x],hit[\value]);
			}
			].wchoose([0.3,0.1,0.6]).value;
		};
	}
	
	// select drum kit from 505, 606,707, 808, 909 or rand
	setDrumKit{|symbol|
		
		switch (symbol,
			'505', {
				models[100..107].do{|m| m.valueAction_(0,send:true) };
				(108..115).do{|i,j|
					models[i].lazyValueAction_(#[0,1,2,3,5,12,13,14][j],send:true) };
			},
			'606', {
				models[100..107].do{|m| m.valueAction_(1,send:true) };
				(108..115).do{|i,j|
					models[i].lazyValueAction_(#[0,1,1,2,3,4,5,6,7][j],send:true) };
			},
			'707', {
				models[100..107].do{|m| m.valueAction_(2,send:true) };
				(108..115).do{|i,j|
					models[i].lazyValueAction_(#[0,1,2,4,3,8,9,11][j],send:true) };
			},
			'808', {
				models[100..107].do{|m| m.valueAction_(3,send:true) };
				(108..115).do{|i,j|
					models[i].lazyValueAction_(#[0,1,4,5,6,15,16,17][j],send:true) };
			},
			'909', {
				models[100..107].do{|m| m.valueAction_(4,send:true) };
				(108..115).do{|i,j|
					models[i].lazyValueAction_(#[0,2,6,7,7,8,9,9][j],send:true) };
			},
			'Rand', {
				// rand bank as well ?
				channels.do{|y|
					//models[100+y].lazyValueAction_((channelBanks[y].size).rand,send:true);
					models[108+y].lazyValueAction_((channelBanks[y][p[100+y]].size-1).rand,
						send:true);
				}
			})
		
	}
	
	// networking the models /////////////////////////////////////
	
	// onOff (channel)
	channelOnOff{|i,val,latency,send,toggle|
		p[20+i]=val;
		channelOnSolo[i].on_(val);
		if (send) { api.sendVP(id++\cOnOff++i,\netChannelOnOff,i,val) };
	}
	
	// and its net call
	netChannelOnOff{|i,val| models[20+i].lazyValueAction_(val,nil,false,false) }
	
	// solo (channel)
	channelSolo{|i,val,latency,send,toggle|
		p[12+i]=val;
		channelOnSolo[i].solo_(val);
		{this.refreshOnOffEnabled}.defer;
		if (send) { api.sendVP(id++\cSOLO++i,\netChannelSolo,i,val) };
	}
	
	// and its net call
	netChannelSolo{|i,val| models[12+i].lazyValueAction_(val,nil,false,false) }
	
	// refresh the enabled for onSolos
	refreshOnOffEnabled{
		if (channelOnSoloGroup.isSoloOn) {
			channels.do{|i| models[20+i].enabled_(false)};
		}{
			channels.do{|i| models[20+i].enabled_(true)};
		};
	}
	
	//// midi players & uGens //////////////////////////

	// noteOn
	noteOn	{|note, vel, latency|
		if ((note<p[7])or:{note>p[8]}) {^nil}; // drop out if out of midi range
		this.bang((note-36).asInt.wrap(0,7),vel/127,latency);
	} 

	/// midi clock in
	clockIn {|beat,latency|
		modSequencers.do(_.clockIn(beat,latency)); // mod comes 1st
		sequencers.do(_.clockIn(beat,latency));	 // the bundles are then made here
	
		// then play the bundles so choke has a chance to work on the other channels
		voicer.playAll(latency, {|channel,velocity|
			this.bangMIDI(channel,velocity,latency);
			{gui[\lamps][channel].value_(chokeLamp[channel][0],0.1)}.defer(chokeLamp[channel][1]);
		});

	}
	
	// temp to hook up volca beats
	bangMIDI{|channel,velocity, latency, volume=1|
		midi.noteOn(channel+44, velocity*127, latency);
		{
			midi.noteOff(channel+44, velocity*127, latency);
			nil;
		}.sched(studio.absTime*3);
	}
	
	// reset sequencers posViews
	clockStop {
		sequencers.do(_.clockStop(studio.actualLatency));
		modSequencers.do(_.clockStop(studio.actualLatency));
	}
	
	// remove any clock hilites
	clockPause{
		sequencers.do(_.clockPause(studio.actualLatency));
		modSequencers.do(_.clockPause(studio.actualLatency));
	}
	
	// trigger a new drum
	bang{|i,velocity,latency,internalSeq=false|
		var sample, thisNode, envFlatList, pan, out;
		var rate, amp, duration, modDur, sendChannels, sendAmp, choke;
		var masterSendChannels, masterSendAmp;
		var tempGrainDur, posRate, density, rand, isGrainPlayer;
		var filterOn, vel, eVal;
		var sampleBank, offset, startFrame, overlap, loop;
		
		if (      instOnSolo.onOff==0) {^nil}; // drop out if instrument onSolo is off
		if (channelOnSolo[i].onOff==0) {^nil}; // drop out if channel onSolo is off
		if (p[116+i]==1) { if (1.0.rand>(velocity+p[9])) {^nil} }; // beat probality drop out
			
		velocity=velocity.map(0,1,p[252+i],1); // scale the velocity
		
		sampleBank = channelBanks[i][p[100+i]]; // get the sample bank
		
		//if (sampleBank.isLoading) {^nil}; // drop out if still loading
		
		if (sampleBank.size>0) {
			
			if (p[289+i].isTrue) {
				models[108+i].lazyValueAction_(sampleBank.size.rand.asInt,latency); // random smp
			};
			sample = sampleBank.samples.wrapAt(p[108+i]);     // get the sample
			if ((sample.isNil) or: {sample.buffer.isNil}) {   // if it's not there or not ready
				sample = lastGoodSample[i];                  // then use last good sample
			}{
				// else this is now the last good sample so get metaData
				lastGoodAmp[i]        = sampleBank.amp(p[108+i]);
				lastGoodOffset[i]     = sampleBank.start(p[108+i]);
				lastGoodLoop[i]       = sampleBank.loop(p[108+i]);
				lastGoodStartFrame[i] = sampleBank.numFrames(p[108+i]) * (lastGoodOffset[i]);
			}; 
		};
		if (sample.isNil) {^nil};   // if we still didn't find a sample then drop out
		lastGoodSample[i] = sample; // else store this as the last good one
			
		// use last good sample metaData
		offset     = lastGoodOffset[i] ? 0;
		startFrame = lastGoodStartFrame[i] ? 0;
		loop       = lastGoodLoop[i] ? 0;
		
		// envelope shape, a little over the top in scaling but it gets the results i like
		eVal=p[60+i];
		if (p[204+i]>=0) {
			eVal=((modValues[i]*p[204+i])).map(0,1,eVal,1);
		}{
			eVal=((modValues[i]*p[204+i])).map(-1,0,-1,eVal);
		};
		eVal=(eVal*0.63639610306789+0.7778174593052)**2-1;
		eVal=(((2**(eVal.tanh.abs))-1)*2).tan*4.66*(eVal.sign);
		envFlatList=Env.new([0,1,0],[p[277+i]**2,1],[0,eVal]).asArray;
		
		// get everything else
		vel      = ampSpec.map(velocity).dbamp;
		amp      = this.getAndStoreSynthArg(\amp, i);
		
		this.setFilterArg(\amp,i,amp,latency); // set amp (this will need to move)
		
		pan      = this.getAndStoreSynthArg(\pan, i);
		rate     = this.getAndStoreSynthArg(\rate,i);
		if ((p[236+i]==1) or:{p[11]==1}) { filterOn=1 }  { filterOn=0 };
		
		out                = this.getSynthArg(\outputChannels,i);
		sendAmp            = this.getSynthArg(\sendAmp,i);
		sendChannels       = this.getSynthArg(\sendChannels,i);
		masterSendAmp      = this.getSynthArg(\masterSendAmp,i);
		masterSendChannels = this.getSynthArg(\masterSendChannels,i);

		voicer.killChannelNow(i,latency); // kill previous if it exists (mono)
		
		// am i a grain player?
		isGrainPlayer=(p[148+i]==1);	
		if (p[228]==1) { isGrainPlayer=true };
	
		// get the nodeID for the \samplePlayer
		thisNode = server.nextNodeID;
		
		if (isGrainPlayer) {

			// grain player
			posRate      = this.getAndStoreSynthArg(\posRate,i);
			tempGrainDur = (1/posRate).clip(0,10);  // why this? it could go on for ever!!
			duration     = sample.duration * p[5] * p[52+i] * tempGrainDur;
			density      = this.getAndStoreSynthArg(\density,i);
			rand         = this.getAndStoreSynthArg(\rand   ,i);
			overlap      = this.getAndStoreSynthArg(\overlap,i);
			
			voicer.storeBundle(i, \samplePlayer, thisNode, duration, vel,
				[\s_new, #[["monoGSNF","monoGS-F"],
							["stereoGSNF","stereoGS-F"]][sample.numChannels-1][filterOn],
							thisNode, 0,  groupIDs[\inst],
					\bufnum,				sample.bufnum,
					\rate,				rate,
					\amp, 				amp,
					\velocity,			vel,
					\loop,				loop,
					\dur, 				duration,
					\pan, 				pan,
					\outputChannels,		[out,filterBuses[i].index][filterOn],
					
					\sendChannels,		sendChannels,
					\sendAmp,				sendAmp,
					\masterSendChannels,	masterSendChannels,
					\masterSendAmp,		masterSendAmp,

					\offset,				offset,
					\startFrame,			startFrame,
					\posRate,				posRate,
					\density,				density,
					\rand,				rand,
					\overlap,				overlap
				],[\n_setn, thisNode, \newEnv, envFlatList.size] ++ envFlatList
			);
					
		}{
			
			// normal player
			duration = sample.duration / rate * p[5] * p[52+i];

			voicer.storeBundle(i, \samplePlayer, thisNode, duration, vel,
				[\s_new, #[["monoNF","mono-F"],
							["stereoNF","stereo-F"]][sample.numChannels-1][filterOn],
							thisNode, 0,  groupIDs[\inst],
					\bufnum,				sample.bufnum,
					\rate,				rate,
					\amp, 				amp,
					\velocity,			vel,
					\loop,				loop,
					\dur, 				duration,
					\pan, 				pan,
					\outputChannels,		[out,filterBuses[i].index][filterOn],
					
					\sendChannels,		sendChannels,
					\sendAmp,				sendAmp,
					\masterSendChannels,	masterSendChannels,
					\masterSendAmp,		masterSendAmp,

					\offset,				offset, // i don't think this is needed
					\startFrame,			startFrame,
				],[\n_setn, thisNode, \newEnv, envFlatList.size] ++ envFlatList
			);
		
		};
				
		voicer.choke(i,latency); // choke other channels as needed
		
		// play it if not from this internal sequencer, else play it from clockIn
		if (internalSeq.not) {
			if (latency.isNil) {
				voicer.play(i,latency); // if from gui or extrnal midi
			}{
				// else from another internal sequencer (use slight delay method to allow choke)
				{
					voicer.play(i,latency-0.001);
					nil;
				}.sched(0.001);
			};
			{gui[\lamps][i].value_(velocity,0.1)}.defer(latency?0);
		}{
			chokeLamp[i][0]=velocity;
			chokeLamp[i][1]=latency?0;
		};
		
		// set the pos marker
		this.posMarker(sampleBank, i, sampleBank.samples.indexOf(sample), duration,
											sample.duration,offset,isGrainPlayer,loop);
		
	}
	
	// put the pos marker in
	posMarker{|sampleBank,i , sampleIndex,duration, sampleDuration,offset,isGrain,loop|
			
		var task, rate;
		
		lastTask[i].stop;
		lastTaskFunc[i].value;
		
		if (sampleIndex.isNil) {^this};
		
		// only do this if the window is open
		if ((lastMetaWindow[i].notNil) and:{lastMetaWindow[i].isOpen}) {
			
			lastTaskFunc[i] = {
				if (sampleBank.otherModels[sampleIndex].notNil) { 
					sampleBank.otherModels[sampleIndex][\pos].valueAction_(-1,0.2,true);
				};
				lastTaskFunc[i]=nil; // only do it once
			};
			
			// for the gui playback
			task= Task({
				var startTime = AppClock.now;
				var lastTime = startTime;
				var pos = offset;
				
				inf.do{
					var now = AppClock.now;
						
					rate = this.getSynthArg(isGrain.if(\posRate,\rate),i);
					
					if (loop.isTrue) {
						pos = (pos + ((now - lastTime)/sampleDuration*rate)).wrap(0,1);
					}{
						pos = (pos + ((now - lastTime)/sampleDuration*rate)).clip(0,1);
					};
					
					lastTime = now;
					
					if ((pos>=1)or:{(now-startTime)>duration}) {
						
						lastTaskFunc[i].value;
						task.stop;
					}{	
						if (sampleBank.otherModels[sampleIndex].notNil) {
							sampleBank.otherModels[sampleIndex][\pos].valueAction_(pos,0.2);
						};
					};
	
					(1/30).wait; // 30 fps (could be lower if needed)
					
				};
			}).start(AppClock);
				
			lastTask[i]=task;
		
		}
	}
	
	// dsp ////////////////////////////////////////////////////////
	
	// called from the models to update synth arguments
	updateSynthArg{|synthArg,i,latency|
		var value=synthArgFuncs[synthArg].value(i);
		if (value!=synthArgValues[i][synthArg]) {
			synthArgValues[i][synthArg]=value;	
			voicer.setArg(i,\samplePlayer, synthArg, value, latency);
		}
	}
	
	// used in bang while creating a new synth
	getAndStoreSynthArg{|synthArg,i|
		var value=this.getSynthArg(synthArg,i); // as below
		synthArgValues[i][synthArg]=value;
		^value
	}
	
	// give me the synth arg value calculated in the arg functions
	getSynthArg{|synthArg,i| ^synthArgFuncs[synthArg].value(i) }
	
	// this is called once to start ugens at instrument creation
	startDSP{
		filterBuses = defaultChannels.collect{ Bus.audio(server,2) };
		synth = defaultChannels.collect{|i|
			Synth.head(groups[\gsrFilter], "GSR_Filter",[\inputChannels, filterBuses[i].index ])
		};
		filterNodes  = synth.collect(_.nodeID);	
	}
		
	// and once to stop ugen when the instrument is deleted
	stopDSP{
		synth.do(_.free);
		filterBuses.do(_.free);
	}

	// update synth parameters in a load
	updateDSP{|oldP,latency| // this will need latency...(source midi preset seq, sync etc)
		var out;				
		if (p[3]>=0) {
			out = p[3]*2
		}{	
			out = LNX_AudioDevices.firstFXBus+(p[3].neg*2-2);
		};
		this.instOutChannel_(out,latency);	
		filterNodes.do{|node,i| this.updateFilterDSP(i,latency) }; // update all filters
	}
	
	// update an individual filter
	updateFilterDSP{|i,latency|
		server.sendBundle(latency,
			[\n_set, filterNodes[i], \drive,          this.getSynthArg(\drive,             i)],
			[\n_set, filterNodes[i], \filtFreq,       this.getSynthArg(\filtFreq,          i)],
			[\n_set, filterNodes[i], \filtRes,        this.getSynthArg(\filtRes,           i)],
			[\n_set, filterNodes[i], \type,           this.getSynthArg(\type,              i)],
			
			[\n_set, filterNodes[i], \outputChannels, this.getSynthArg(\outputChannels,    i)],
			[\n_set, filterNodes[i], \amp,            this.getSynthArg(\amp,               i)],
			[\n_set, filterNodes[i], \sendAmp,        this.getSynthArg(\sendAmp,           i)],
			[\n_set, filterNodes[i], \sendChannels,   this.getSynthArg(\sendChannels,      i)], 
			[\n_set, filterNodes[i], \masterSendChannels,
				this.getSynthArg(\masterSendChannels,i)], 
			[\n_set, filterNodes[i], \masterSendAmp,  this.getSynthArg(\masterSendAmp,     i)], 
			
			[12, filterNodes[i],(p[11]+p[236+i]).binaryValue] // pause
		);	
	}
	
	// will these need latency... prob espically with syncs now!!
	setFilterArg{|synthArg,i,val,latency|
		server.sendBundle(latency, [\n_set, filterNodes[i], synthArg, val ] )
	}
	
	
	// called from the models to update the filter synth arguments
	updateFilterArg{|synthArg,i,latency|
		
		var old = synthArgValues[i][synthArg]; // get the old
		var new = this.getAndStoreSynthArg(synthArg,i); // this will also set synthArgValues[][];
		
		if (old!=new) {
			this.setFilterArg(synthArg,i,new,latency);
		}; // update the dsp
	}
	
	// and finally set all filter used if master controls
	setAllFilterArgs{|synthArg,latency|
		defaultChannels.do{|i| this.updateFilterArg(synthArg,i,latency) }
	}
	
	// for switch between channel & master filter
	updateFilterOnOff{|i,latency|
		var filterOn = (p[11]+p[236+i]).isTrue;
		if (filterOn) {
			filterOffTasks[i].stop; // stop the turn off filter task
			filterOffTasks[i]=nil;
			server.sendBundle(latency, [12, filterNodes[i],1]); // unpause filter
			this.updateFilterArg(\drive,i,latency); 
			this.updateFilterArg(\filtFreq,i,latency);
			this.updateFilterArg(\filtRes,i,latency);
			this.updateFilterArg(\type,i,latency);
		}{
			var timeRemaining = voicer.timeRemaining(i,\samplePlayer);
			if (timeRemaining.notNil) {
				filterOffTasks[i]={
					timeRemaining.wait;  // pause the filter after time remaining is up
					server.sendBundle(latency, [12, filterNodes[i],0]); // pause filter
					filterOffTasks[i]=nil;
				}.fork(AppClock); 
					
			}{
				server.sendBundle(latency, [12, filterNodes[i],0]); // else pause filter now
			}
		}
	}
	
	// for switch between channel & master filter
	updateAllFilterOnOff{|latency|
		defaultChannels.do{|i| this.updateFilterOnOff(i,latency) }
	}
	
	// sets both the filter and individual drum args
	setBoth{|synthArg,i,latency|
		var value = this.getSynthArg(synthArg,i);
		if (value!=synthArgValues[i][synthArg]) {
			synthArgValues[i][synthArg]=value;
			// can do testing for both here... can i be arsed?
			this.setFilterArg(synthArg,i,value,latency);
			this.setSynths(synthArg,i,value,latency);
		};
	}
	
	// as setBoth but on all channels
	setAllBoth{|synthArg,latency| defaultChannels.do{|i| this.setBoth(synthArg,i,latency) } }	
	// called from the models to update synth arguments, no testing
	// currently set only for \samplePlayer
	setSynths{|synthArg,i,value,latency|
		voicer.setArg(i,\samplePlayer, synthArg, value, latency);
	}
	
} // end ////////////////////////////////////
