/////////////////////////////////////////////////////////////////////////////
//
// MVC_LazyRefresh
//
// lazy refresh limits a refresh to fps, this is not the same as a frame rate
// 1st call always happens at that time and not quant to a time frame
// following calls then happen every 1 frame
// if last frame was called over a frame ago then
//    everything is reset and next call will happen immediately
// also defers if current thread uses SystemClock
//
/////////////////////////////////////////////////////////////////////////////
// lazyRefresh is constantly adjusting its rate, so the whole gui system
// doesn't freeze when too much is going on
/////////////////////////////////////////////////////////////////////////////
/*
MVC_LazyRefresh.incRefresh;
MVC_LazyRefresh.mouseDown;
MVC_LazyRefresh.mouseUp;
*/


MVC_LazyRefresh{

	classvar <>verbose = true;

    classvar <>globalFPS, <refreshCount=0, <lastRefreshCount=0, <task, <taskRate=1, <rateAdjust=1;

    var <>refreshFunc, <>fps=20, lastTime=0, nextTime, <>model, <>spread=true;

	// all views add 1 to the refreshCount when they are refreshed
	*incRefresh{ refreshCount = refreshCount + 1 }

	// add n to the refreshCount (for more expensive guis i.e. pRoll)
	*incRefreshN{|n=1| refreshCount = refreshCount + n }

	// mouse down events set a global FPS so we get more responsive controls in Qt
	*mouseDown{ globalFPS = 5  }

	// this global FPS is released with mouse up events
	*mouseUp  { globalFPS = nil}

	// a task that adjusts the lazyRefresh rate of the whole gui according to how many freshCount
	// have been had, this helps stop the gui from freezing if to many refreshes are called
	*startRefreshWatchingTask{
		if (task.isPlaying) {^this};           // only 1 task playing
		if (task.notNil) { task.play; ^this }; // only 1 task playing
		task = Task({
			inf.do{
				lastRefreshCount = refreshCount * taskRate * (globalFPS.isNil.if(1,1.33));
				refreshCount = 0;
				if (lastRefreshCount>700) {
					// slow gui down
					rateAdjust = (rateAdjust * (lastRefreshCount.linexp(700,1000,1,0.1,nil))).clip(0.1,1);
				}{
					// speed gui up
					rateAdjust = (rateAdjust * (lastRefreshCount.linexp(700,0,1,2,nil))).clip(0.1,1);
				};
				taskRate.reciprocal.wait;
				if (verbose) { [lastRefreshCount, rateAdjust].post; ",".postln;};
		}},SystemClock).play;
	}

	lazyRefresh{
		var now=SystemClock.seconds;			    // the time now
		if ((now-lastTime)>(((globalFPS?fps)*rateAdjust).reciprocal)) {
													// if time since last refresh is > frame duration
            lastTime=now;						    // so last time becomes now
			nextTime=nil;						    // nothing is now scheduled for the future
			if (thisThread.clock==SystemClock) {    // do i need to defer to the AppClock ?
                {refreshFunc.value}.defer;          // defer the refresh func
			}{
				refreshFunc.value;			        //  call the refresh func now
			};
        }{									        // else time since last refresh is < frame dur
			if (nextTime.isNil) {				    // if there isn't a refresh coming up
				nextTime=lastTime+(((globalFPS?fps)*rateAdjust).reciprocal);
													// do it when a frame duration has passed
				{
					refreshFunc.value;		        // call the refresh func
					nextTime=nil;				    // no next time set now
				}.defer(nextTime-now);			    // defer to the correct delta
				lastTime=nextTime;				    // this is now the last time a refreshed happened
			}
		}
	}

	// this replaces MVC_Model lazyValueRefresh so it sits in here
	lazyValueRefresh{
		var now;
		now=SystemClock.seconds;
		if ((now-lastTime)>(((globalFPS ? fps)*rateAdjust).reciprocal)) {
			lastTime=now;
			nextTime=nil;
			if ((spread) and: {model.noDependants>1}) {
				model.dependants.do{|view,j|
					{
						view.value_(model.value)
					}.defer(j/((globalFPS ? fps)*rateAdjust*(model.noDependants))); // spread over frame
				};
			}{
				{model.dependants.do{|view| view.value_(model.value)};}.defer;
			};
		}{
			if (nextTime.isNil) {
				nextTime=lastTime+(((globalFPS ? fps)*rateAdjust).reciprocal);
				{
					if ((spread) and: {model.noDependants>1}) {
						model.dependants.do{|view,j|
							{
								view.value_(model.value)
							}.defer(j/((globalFPS ? fps)*rateAdjust*(model.noDependants)))
						};
					}{
						model.dependants.do{|view|
							view.value_(model.value)
						};
					};
					nextTime=nil;
				}.defer(nextTime-now);
				lastTime=nextTime;
			}
		}
	}

	// free me!
    free{ refreshFunc = model = nil }

}