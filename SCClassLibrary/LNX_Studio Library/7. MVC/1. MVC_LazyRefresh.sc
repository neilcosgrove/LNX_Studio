/////////////////////////////////////////////////////////////////////////////
// lazy refresh limits a refresh to fps, this is not the same as a frame rate
// 1st call always happens at that time and not quant to a time frame
// following calls then happen every 1 frame
// if last frame was called over a frame ago then
//    everything is reset and next call will happen immediately
// also defers if current thread uses SystemClock
// can also set a globalFPS
/////////////////////////////////////////////////////////////////////////////

MVC_LazyRefresh{

    classvar <>globalFPS;

    var <>refreshFunc, <>fps=25, lastTime=0, nextTime;

	lazyRefresh{
		var now=SystemClock.seconds;			    // the time now
        if ((now-lastTime)>(1/(globalFPS?fps))) {   // if time since last refresh is > frame duration
            lastTime=now;						    // so last time becomes now
			nextTime=nil;						    // nothing is now scheduled for the future
			if (thisThread.clock==SystemClock) {    // do i need to defer to the AppClock ?
                {refreshFunc.value}.defer;          // defer the refresh func
			}{
				refreshFunc.value;			        //  call the refresh func now
			};
        }{									        // else time since last refresh is < frame dur
			if (nextTime.isNil) {				    // if there isn't a refresh coming up
				nextTime=lastTime+(1/(globalFPS?fps)); // do it when a frame duration has passed
				{
					refreshFunc.value;		        // call the refresh func
					nextTime=nil;				    // no next time set now
				}.defer(nextTime-now);			    // defer to the correct delta
				lastTime=nextTime;				    // this is now the last time a refreshed happened
			}
		}
	}

    free{ refreshFunc=nil }

}