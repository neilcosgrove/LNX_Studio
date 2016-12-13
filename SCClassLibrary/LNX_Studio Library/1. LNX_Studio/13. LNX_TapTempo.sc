/*************************/
/* LNX_STUDIO_TapTempo  */
/***********************/
/*
t= LNX_TapTempo();
t.tapFunc_{|me,val| me.asInt.postln}
t.tap;
tapTempo = LNX_TapTempo().tapFunc_{|me,bpm| models[\tempo].valueAction_(bpm.asInt) };
*/

LNX_TapTempo {

	var maxTime = 1.75; // this will force a min bpm

	var	noTaps=0, firstTapTime=0, lastTapTime=0, totalTapTime=0, >tapFunc, >firstTapFunc, >stopFunc;

	var task;

	// tap tempo bpm
	tap{
		var thisTimeTap, bpm;

		this.startEndTask;

		if (noTaps==0) {								// is this the 1st tap?
			firstTapTime=SystemClock.now;				// firstTapTime is now
			lastTapTime=firstTapTime;					// also make last time now
			noTaps=noTaps+1;							// inc taps by 1
			firstTapFunc.value(this);					// 1st tap func call

		}{												// else not 1st tap so
			thisTimeTap=SystemClock.now;				//   thisTimeTap is now

			if ((thisTimeTap-lastTapTime)>maxTime) {	// if tap time over maxTime sec limit start again
				firstTapTime=SystemClock.now;			//   firstTapTime is now
				lastTapTime=thisTimeTap;				//   also make last time now
				noTaps=1;								//   reset taps to 1
				totalTapTime=0;							//   totalTapTime is 0
				firstTapFunc.value(this);				//   1st tap func call

			}{											// else witin maxTime sec limit
				lastTapTime=thisTimeTap;				//	 last tap time is now
				totalTapTime=lastTapTime-firstTapTime;	//   total tap time is now-start
				noTaps=noTaps+1;						//   inc taps by 1
				bpm=60/(totalTapTime/(noTaps-1));		//   bpm is this formula
				tapFunc.value(this,bpm);				//   tap func call, with new bpm
			}
		}
	}

	startEndTask{
		if (task.isNil) {
			task = Task({
    			inf.do({
					if ((SystemClock.now - lastTapTime)>maxTime) {
						stopFunc.value(this);
						task.stop;
						task = nil;
					};
        			0.25.wait;
    			});
			});
			task.start;

		};
	}

}
