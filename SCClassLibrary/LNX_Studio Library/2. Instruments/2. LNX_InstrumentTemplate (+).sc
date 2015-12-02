
// make your own instruments with this template for subclasses

// LNX_MyInst : LNX_InstrumentTemplate {

+ LNX_InstrumentTemplate {

	// var myVars;

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new.init(server,studio,instNo,bounds,open,id,loadList)
	// remove this   ^  init when creating your own instruments
	}

	*studioName {^"Template"}
	*thisWidth  {^100} // the default width of this instrument
	*thisHeight {^100} // the default height of this instrument
	*sortOrder{^10}
	
	isMIDI{^false}
	isFX{^false}
	isInstrument{^false}
	canBeSequenced{^false}
	isMixerInstrument{^false}
	onColor{^nil}  // if you want a different color on button in the studio put it here
	mixerColor{^Color(1,1,1,0.4)} // colour in mixer
	alwaysOn{^false} // am i always on? used by melody maker to change onOff widgets
	canAlwaysOn{^false} // can i always be on as an inst?
	canTurnOnOff{^true}
	hasMIDIClock{^false}
	
	clockPriority{^5} // order for clock beats. sequenced controllers 1st. instruments last.
					// the lower the number the higher the priority
					
	interface{^nil} // supply an immutable list of methods to make available to the network

	*isVisible{^true}

	header { 
		// define your document header details
		instrumentHeaderType="SC Template Doc";
		version="v1.1";
	}
	
	initPreModel{} // anything needed before the models are made
	
	// override this ( move to template plus)
	initModel {
		// these are the default parameters for the instrument. you can add to this list as you
		// develope the instrument and it will still save and load previous versions. it
		// will just insert the missing default parameters for you when loading an older version.
		// if you reduce the size of this list or the order you will need to sort it out in the
		// .iPutLoadList method.
		// the only 2 items i'm going for fix in the studio are solo & onOff as below
		#models,defaults=[
			[ 0, \switch],  // 0.solo
			[ 1, \switch]   // 1.onOff
		].generateAllModels;
		presetExclusion=[0,1];
		randomExclusion=[0,1];
		autoExclusion=[];
	}

	// return your output model, for fadeIn, fadeOut, and Mixer
	peakModel{^nil}
	volumeModel{^nil } // volumeModel{^models[2] } 
	outChModel{^nil}

	// solo model
	soloModel{ ^models[0] }
	
	// onOff model
	onOffModel{^models[1] }

	panModel{^nil}

	sendChModel{^nil}
	sendAmpModel{^nil}

	// in models (fx's)
	inModel{^nil}
	inChModel{^nil}
	
	// latency & sync
	iSyncDelayChanged{}

	// your own vars
	iInitVars{}
	
	// any post midiInit stuff
	iInitMIDI{
		// if you have no guis that can be midi controlled
		// add an empty control to stop a nil error with this
		// LNX_EmptyGUIShell.controlID_(0,midiControl,"None");
	}
	
	// post init
	iPostInit{}
	
	// after init only for new inst, not duplicate or load
	iPostNew{ }
	
	// anything else that needs doing after a server reboot; 
	iServerReboot{}
		
	// for your own clear, used to when loading studio presets 
	iClear{}
	
	// for freeing any automation you have. i.e. step sequencers, sccode usermodels
	iFreeAutomation{}
	
	// for freeing anything when closing
	iFree{}
	
	// Presets ////////////////////////////////////////
	
	// get the current state as a list
	iGetPresetList{ ^[] }
	
	// add a statelist to the presets
	iAddPresetList{|l| }
	
	// save a state list over a current preset
	iSavePresetList{|i,l| }

	// for your own load preset
	iLoadPreset{|i,newP,latency| }
	
	// for your own remove preset
	iRemovePreset{|i|}
	
	// for your own removal of all presets
	iRemoveAllPresets{}
	
	// for your own randomise preset
	iRandomisePreset{}
	
	// clear the sequencer
	clearSequencer{}
	
	// Saving and Loading ///////////////////////////////	
	// for your own saving
	iGetSaveList{ ^[] }
		
	// for your own loading
	iPutLoadList{|l,noPre,loadVersion| ^l }
	
	// once a song is loaded this method will update any special GUI items not
	// covered by the instrument template 
	iUpdateGUI{|p|}
	
	// anything else that needs doing after a load. all paramemters will be loaded by here
	iPostLoad{|noPre,loadVersion,templateLoadVersion|}
	
	// anything that needs doing after the entire song has been loaded and id's assigned to insts
	iPostSongLoad{|offset|}
	
	//// GUI ///////////////////////////////////////////

	createWindow{|bounds|	
		this.createTemplateWindow(bounds,Color(0.1,0.1,0.1,1),resizable:false,scroll:false);
	}
	
	createWidgets{ } // put your GUI code in here

	//// uGens & midi players //////////////////////////

	// this will be called by the studio after booting
	*initUGens{|server| }
	
	// or an instance version
	initUGens{}
	
	// anything to do after a cmdPeriod
	cmdPeriod{}
	
	// startDSP  : this is called once to start ugens at instrument creation
	// stopDSP   : and once to stop ugen when the instrument is deleted
	//             used to start & stop fx's or mono synths
	// updateDSP : and also update synth parameters in a load
	startDSP  {}
	stopDSP   {}
	updateDSP{|oldP,latency| }
	replaceDSP{} // to be used by synth's or fx's that need to replace the current
	             // synth with a new one in order to update values
	
	// midi In methods, latency is supplied
	noteOn	{|note, vel, latency|}		// noteOn
	noteOff	{|note, vel, latency|}		// noteOff
	control	{|num,  val, latency|}		// control
	bend 	{|bend     , latency|}		// bend
	touch	{|pressure , latency|}		// pressure
	program	{|program  , latency|}      // and program (the selectProgram method is called 1st
	midiInternal{|command,arg1,arg2| }   // internal comms via midi (not used yet)

	// midi in to midi clock out methods
	midiSongPtr{|songPtr,latency|} 
	midiStart{|latency| }
	midiClock{|latency| }
	midiContinue{|latency| }
	midiStop{|latency| }
	

	stopAllNotes{} 			// used for noteOff when stopping sequencers & midi Devices
							// also called by onOff & solo buttons
							
	updateOnSolo{}  // onSolo has changed
		
	popItems{^[]}
		
	// a quick way to get working, to be used by all midi in future
	pipeIn{|pipe|	
		switch (pipe.kind)
			{\noteOn} { // noteOn
				this.noteOn(pipe.note, pipe.velocity, pipe.latency)
			}
			{\noteOff} { // noteOff
				this.noteOff(pipe.note, pipe.velocity, pipe.latency)
			}
			{\control} { // control
				this.control(pipe.num, pipe.val, pipe.latency)
			}
			{\touch} { // touch
				this.touch(pipe.pressure, pipe.latency)
			}
			{\program} {
				// to do and confirm
			}
			{\bend} { // bend
				this.bend(pipe.val, pipe.latency)
			}	
	} 
	
	/// clock in ////////////////////////////////////////
	
	clockIn{|beat|} //clockIn is the clock pulse, with the current song pointer in beats
	clockIn3{|beat,absTime,latency|} // same as clcokIn but with x3 the resolution
	clockOff{|beat,latency|} // this clock is playing when a song is not playing, used in lfos 
	clockPlay{|latency| }		//play and stop are called from both the internal and extrnal clock
	clockStop{|latency| }
	clockPause{|latency| }		// pause only comes the internal clock
	clockReset{|latency| this.clockPause(latency) } // when pop resets beat, override if needed

	/// i have down timed the clock by 3, so this gives 32 beats in a 4x4 bar
	/// this is all the resoultion i need at the moment
	
	// this is called every time a changing in bpm happens
	bpmChange{
		// use var bpm or absTime
	}
		
	// this is called by the studio for the auto map midi controller
	autoMap{|num,val|
		var vPot;
		vPot=(val>64).if(64-val,val);
	}
	
	// summary of networking //////////////////////////////////////////////////////////////////
	
	/*
		send               : others recieve  this.method(arg1,arg2...)
		sendND             : others recieve  this.method(arg1,arg2...)          no defer
		sendList		     : others recieve  this.method(list)
		sendTo             : user   recieves this.method(arg1,arg2...)          no defer
		sendToND           : user   recieves this.method(arg1,arg2...)
		sendDelta          : users  recieve  this.method(delta,arg1,arg2...)    from host only
		hostCmd		     : host   recieves this.method(userID,arg1,arg2...)   with userID
			
		sendOD             : same as send but Guaranteed in the Order sent
		sendGD             : same as send but Guaranteed, order may change
		sendVP             : send a variable message with a final value sent via sendOD (needs id)
		sendToGD           : same as sendTo but Guaranteed
		sendToOD           : same as sendTo but Guaranteed in the Order sent
		sendDeltaOD        : same as sendDelta but Guaranteed in the Order sent
		sendClumpedList    : others recieve  this.method(list)
		hostCmdClumpedList : send a clumped command list to the host. Guaranteed.
		hostCmdGD          : same as hostCmd but Guaranteed
		
		groupCmdOD		: everyone will do command in order via host
		groupCmdSync		: everyone will do command in order via host at the same time
	*/
	
	// and a summary of setting parameters in the network ///////////////////////////////////////
	
	/*
		setP  (index,value) - just set the parameter and gui
		setPGD(index,value) - guaranteed
		setPOD(index,value) - ordered
		setPVH(index,value) - via host
		setPVP(index,value) - variable parameter 
		
		setSynthArg  (index,value,synthArg,argValue) - also set the synth arg
		setSynthArgGD(index,value,synthArg,argValue)
		setSynthArgOD(index,value,synthArg,argValue)
		setSynthArgVH(index,value,synthArg,argValue)
		setSynthArgVP(index,value,synthArg,argValue)
		
		setSynthArgGUI  (index,value,synthArg,argValue,guiIndex,guiValue) - and a specific gui
		setSynthArgGUIGD(index,value,synthArg,argValue,guiIndex,guiValue)
		setSynthArgGUIOD(index,value,synthArg,argValue,guiIndex,guiValue)
		setSynthArgGUIVH(index,value,synthArg,argValue,guiIndex,guiValue)
		setSynthArgGUIVP(index,value,synthArg,argValue,guiIndex,guiValue)
	
		setPReplace  (item,value) - replaces the synth (for parameters you can't change)
		setPReplaceGD(item,value)
		setPReplaceOD(item,value)
		setPReplaceVH(item,value)
		setPReplaceVP(item,value)
	*/
	
} // end ////////////////////////////////////
