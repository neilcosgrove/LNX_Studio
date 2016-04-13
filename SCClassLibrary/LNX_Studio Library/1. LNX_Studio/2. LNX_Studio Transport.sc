/*************************/
/* LNX_STUDIO transport */
/***********************/

+ LNX_Studio {
	
	//////////////////////////////////////////////////////////////////////////////////
	////// transport /////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////

	// called by gui "play" widget
	guiPlay{|val| api.hostCmdGD(\hostPlay,val) } // host it
	
	hostPlay{|userID,val|	
		if (extClock) {
			models[\play].value_( (extIsPlaying).if(1,0)  ); // and do nothing
		}{
			if (val==1) {
				this.play(LNX_Protocols.latency,beat); // play now !
				api.sendDeltaOD(LNX_Protocols.latency,\play,beat);
			}{
				api.sendOD(\netPause);
				this.pause;
			};
		};	
	}
	
	// this method will have to be triggered inside of play
	resetInstBeat{|beat|
		beat = beat ? 0;
		// surely a dedicated releaseAll method would be better than clockPause?
		insts.clockPriority.do{|inst| inst.clockReset(this.actualLatency) };
		instBeat=beat;
	}
	
	// start play from delta (delta can be negative but will start with lates)
	play	{|delta=0,argBeat|
		var b, firstLoop=true;
		
		this.startTime;
		
		if (extClock.not) {
			
			isPlaying=true;
			if (argBeat.isNumber) { instBeat = beat = argBeat };
			
			insts.clockPriority.do(_.clockPlay); // no latency because this method will never be
											// called by an internal clock
			
			MVC_Automation.pickUpNewRef;
			
			// the main clock of LNX_Studio
			SystemClock.sched(delta,{
				var actualLatency = this.actualLatency;
				
				if(isPlaying) {
					
					// for use with jumpTo (PianoRoll) or FastForwards or Rewind 
					// needs to be done in play clock so all automations sets to correct values
					if (jumpTo.notNil) {
						insts.do(_.stopAllNotes);
						insts.clockPriority.do{|inst| inst.clockPause(actualLatency) };
						instBeat = beat = jumpTo.asInt;
						MVC_Automation.jumpTo(jumpTo);
						jumpTo=nil;
						MVC_Automation.updateBeatRefNow(beat,absTime);
					};
					
					// Automation (uses beat which doesn't change on preset selections)
					MVC_Automation.clockIn3(beat,absTime,actualLatency);
					
					// now presets of presets
					LNX_POP.clockIn3(instBeat,beat,absTime,actualLatency);
					
					// mainly piano rolls here (uses instBeat which does change on presets)
					// the absTime/3 is bad here
					insts.clockPriority
						.do(_.clockIn3(instBeat,absTime/3,actualLatency,beat));
			
					// & mainly step sequencers here
					if ((instBeat%3)==0) {
						b=(instBeat/3).asInt;
						if (hackOn) {
							{myHack[\codeFunc].value(
								this,b,actualLatency, myHack[\netAddr]) }.try;
						};
						insts.clockPriority.do(_.clockIn(b,actualLatency));
					};
					
					if ((beat%6)==0) { this.refreshGuiBeat};
					
					// midi clock out
					if (firstLoop) {
						if (beat==0) {
							midiClock.songPtr(0,actualLatency);
							midiClock.start(actualLatency);   
							midiClock.midiClock(actualLatency);
							insts.midiClock.do{|inst|								inst.midiSongPtr(0,actualLatency);
								inst.midiStart(actualLatency);   
								inst.midiClock(actualLatency);
							};
							
						}{
							midiClock.continue(actualLatency);
							midiClock.midiClock(actualLatency);
							insts.midiClock.do{|inst|								inst.midiContinue(actualLatency);
								inst.midiClock(actualLatency);
							};
						};	
						if (hackOn) {
							{myHack[\startFunc].value( this, myHack[\netAddr]) }.try
						};
						firstLoop = false;
					}{
						midiClock.midiClock(actualLatency);
						insts.midiClock.do{|inst|	
							inst.midiClock(actualLatency);
						};
					};

					beat = beat + 1;
					instBeat = instBeat + 1;	
					absTime

				}{
					this.startClockOff(beat); // or start offClock
					nil
				}
			});
		models[\play].value_(1);
		};
	}

	// do i need to start to use a slower clock due to SystemClock issues?
	startClockOff{|argBeat=0|
		var offBeat = beat;
		tasks[\clockOff].stop; // if there
		// only every 1/3 midiClock beat is used in off clock. only needed by bumNote's LFOs
		tasks[\clockOff] = SystemClock.sched(0,{
			if(isPlaying.not) {
				insts.clockPriority.do(_.clockOff(offBeat.div(3),this.actualLatency));
				offBeat = offBeat + 3; // every 3	
				absTime*3
			}{	
				nil
			}
		});
	}
	
	//////////////////////
	
	pause {
		if (extClock.not) {
			isPlaying=false;
			MVC_Automation.clearRef;
			LNX_POP.clockStop;
			insts.clockPriority.do{|inst| inst.clockPause(this.actualLatency) };
			//midiClock.stop(this.actualLatency);
			midiClock.stop(0);
			insts.midiClock.do{|inst| inst.midiStop(0) };
			models[\play].value_(0); // used to update gui when called by command-.
			this.stopTime;
		};
		
	}
	
	netPause{ if (netTransport) { this.pause } }
	
	/////////////////////
	
	guiStop{ api.hostCmdGD(\hostStop) }
	
	hostStop {
		if (extClock) {
			models[\play].value_( (extIsPlaying).if(1,0)  );
			// and do nothing
		}{
			this.stop;
			api.sendOD(\netStop);
		}
	}
	
	stop{
		//if (extClock.not) {  // we can stop external clocks now
			
		jumpTo=nil;
		MVC_Automation.clearRef;
		LNX_POP.clockStop;	
		this.stopTime;
		
		if (isPlaying) {
			isPlaying=false;
			insts.clockPriority.do{|inst| inst.clockPause(this.actualLatency) };
			//midiClock.stop(this.actualLatency);
			midiClock.stop(0);
			insts.midiClock.do{|inst| inst.midiStop(0) };
		}{	
			if (beat == 0) { LNX_MIDIPatch.panic }; // do panic after 3
			instBeat = beat = 0;	
			this.refreshGuiBeat;
			MVC_Automation.reset;
			MVC_Automation.clockStop(beat,this.actualLatency);
			insts.clockPriority.do{|inst| inst.clockStop(this.actualLatency) };
//				midiClock.stop(this.actualLatency);
//				midiClock.songPtr(0,this.actualLatency);
			midiClock.stop(0);
			midiClock.songPtr(0,0);
			insts.midiClock.do{|inst|
				inst.midiStop(0);
				inst.midiSongPtr(0,0);
			};
			this.resetTime;
		};
		models[\play].lazyValue_(0,false);
		models[\autoRecord].lazyValueAction_(0);
		
		if (hackOn) { {myHack[\stopFunc].value( this, myHack[\netAddr]) }.try };
			
		//};
			
	}
	
	netStop{
		if (netTransport) {
			this.stop;
			models[\play].value_(0);
		}
	}
	
	// used at start collaboration
	stopNow{
		this.stop.stop.resetTime;
		extClock=false;
		extIsPlaying=false;	
	}
	
	// jump to song pos
	guiJumpTo{|val| 
		if (isPlaying) {
			if (network.isConnected) {
				api.groupCmdSync(\hostJumpTo,val)
			}{
				this.hostJumpTo(val);
			}	
		}{
			// when not playing	
			if (network.isConnected) {
				api.groupCmdSync(\jumpWhileStopped,val)
			}{
				this.jumpWhileStopped(val);
			}	
			
		}
	}
	
	// host it
	hostJumpTo{|val| jumpTo=val; }
	
	// jumpTo while stopped
	jumpWhileStopped{|val|
		instBeat = beat = val.asInt;
		MVC_Automation.jumpWhileStopped(beat);
		jumpTo=nil;
		this.refreshGuiBeat;
	}
	
	// for gui clock & beat ///////////////////////////
	
	// time starts when play is pressed
	startTime{
		if (tasks[\time].isNil) {
			tasks[\time]=Task({
				loop {
					1.wait;
					{models[\time].valueAction_(models[\time].value+1)}.defer;
				};
			},AppClock).start; // problems with SystemClock at moment
		}{
			tasks[\time].start	
		};
	}
	
	// and stops when paused or stopped
	stopTime{
		tasks[\time].stop;
	}
	
	// reset on a double stop press
	resetTime{
		tasks[\time].stop;
		tasks[\time]=nil;
		{models[\time].lazyValueAction_(0)}.defer;
	}
	
	// refresh mixer gui of beat
	refreshGuiBeat{
		{
			var b = (beat.div(3)/2).asInt;
			mixerGUI[\beat].string_( (b.div(MVC_Automation.barLength)+1)
			++"."++((b%(MVC_Automation.barLength))+1))
			
		}.defer
	}
	
	///////////////////////////
	
	kill {
		isPlaying=false;
		this.stop;
		insts.clockPriority.do{|inst| inst.clockStop(this.actualLatency) };
		insts.do(_.stopDSP);
		models[\play].value_(0);
	}
	
	// extrnal midi clock in
	midiClockIn{|chan,val|	
		var b, extBPM;
		if (extClock) {		
			switch (chan) 
				{ 8} { // midiClock tick
					if(extIsPlaying) {	
						extTiming=extTiming.add(SystemClock.now);
						extTiming=extTiming.keep(-7);
						if (extTiming.size>6) {
							extBPM=(2.5/(extTiming.differentiate.drop(1).average));
							this.setBPM(nil,extBPM);
						};
						
						MVC_Automation.clockIn3(extBeat,absTime,this.actualLatency);
						
						
						if ((instBeat%3)==0) {	
							// i need to test the nil in these
							b=(instBeat/3).asInt;
							insts.clockPriority.do(_.clockIn(b,this.actualLatency));
							// no latency
						};
						insts.clockPriority.do(_.clockIn3(
												instBeat,absTime/3,this.actualLatency));
												
						if ((extBeat%6)==0) { this.refreshGuiBeat };
												
						instBeat = instBeat +1;
						extBeat  = extBeat  +1;
					};
				} 
				{10} { // start
					extTiming=[];
					extIsPlaying=true;
					this.setPlayColor;
					models[\play].lazyValue_(1,false);
				}                 
				{12} { // stop
					extTiming=[];
					extIsPlaying=false;
					this.setPlayColor;
					models[\play].lazyValue_(0,false);
					{
						insts.clockPriority.do{|inst| inst.clockPause(this.actualLatency) };
						insts.clockPriority.do{|inst| inst.clockStop (this.actualLatency) };
					}.defer(absTime);
				}         
				{11} { // continue
					extTiming=[];
					extBeat=lastPos;
					extIsPlaying=true;
					this.setPlayColor;
					models[\play].lazyValue_(1,false);
				}
				{ 2} { // song pointer
					extTiming=[];
					extBeat=val*6;
					instBeat = extBeat;
					lastPos = extBeat;
				}          					             
			;
		}
	}
	
	// change the play button color to yellow when using an external clock
	setPlayColor{
		{
			if (extIsPlaying) {
				models[\play].dependantsPerform(\color_,\on,Color(0.9,0.9,0));
			}{
				models[\play].dependantsPerform(\color_,\on,Color.green);
			}
		}.defer;
	}
	
	// make sure internal
	forceInternalClock{
		if (extClock) {
			extClock=false;	
			this.clockSwap;
		};
	}
	
	// change clock modes (this isn't  a swap, this is done beforehand in model and above
	clockSwap {
		if (extClock) {
			isPlaying=false;
			extIsPlaying=false;
			this.setPlayColor;
			models[\play].value_(0);
			instBeat = extBeat = 0;
			lastPos=0;
//			midiClock.stop(this.actualLatency);
			midiClock.stop(0);
		}{
			isPlaying=false;
			extIsPlaying=false;
			this.setPlayColor;
			models[\play].value_(0);
			instBeat = beat = 0;
		};
	}
	
	///////////////////////////////////////////////
	
	// gui call to set tempo 
	guiSetBPM{|value|
		if (bpm!=value) {
			api.hostCmd(\hostSetBPM,value); // this is ok because via host
		}
	}
	
	// host the gui call
	hostSetBPM{|userID,value|
		this.setBPM(LNX_Protocols.latency,value); // play now !
		api.sendDeltaOD(LNX_Protocols.latency,\setBPM,value);
	}
	
	// set the bpm
	setBPM{|delta,value|
		{
			bpm=value;
			insts.do{|inst| inst.bpm_(bpm)}; // update all insts
			absTime=2.5/bpm;
			models[\tempo].lazyValue_(bpm,false);
			nil
		}.sched(delta);
	}
	
	// tap tempo bpm
	tap{
		var thisTimeTap, bpm;
		if (noTaps==0) {
			firstTapTime=SystemClock.now;
			lastTapTime=firstTapTime;
			noTaps=noTaps+1;
		}{
			thisTimeTap=SystemClock.now;
			if ((thisTimeTap-lastTapTime)>2) {
				noTaps=1;
				firstTapTime=SystemClock.now;
				lastTapTime=thisTimeTap;
				totalTapTime=0;
			}{
				lastTapTime=thisTimeTap;
				totalTapTime=lastTapTime-firstTapTime;
				noTaps=noTaps+1;
				bpm=60/(totalTapTime/(noTaps-1));
				models[\tempo].valueAction_(bpm.asInt);
			}
		};
	}
	
	togglePlay  { if (extClock.not) {models[\play].valueAction_  (1- models[\play].value)} }
	toggleRecord{ if (extClock.not) {models[\record].valueAction_(1- models[\record].value)} }
		
}
  /////////////////////////////////////////////////////////////
  //////////////// END STUDIO  l n x  2009   //////////////////
  /////////////////////////////////////////////////////////////
