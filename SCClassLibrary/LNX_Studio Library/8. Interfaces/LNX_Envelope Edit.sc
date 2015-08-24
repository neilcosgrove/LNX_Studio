/*
Old class - not used
LNX_EnvelopeEdit : SCEnvelopeEdit {

	classvar <>clipboard;

	var <>updateFunc;
	
	value_ { arg val,do=false;
	
		if (do) {
			if(val.at(1).size != val.at(0).size,{
				// otherwise its a fatal crash
				Error("SCEnvelopeView got mismatched times/levels arrays").throw;
			});
			this.size = val.at(0).size;
			this.setProperty(\value, val)
		}
		
	}
	
	redraw {
		this.value_(viewPoints,true);
	}
	
	refresh {
		this.updateAll;
		this.value_(viewPoints,true);
	}
	
	defaultReceiveDrag {
		if(currentDrag.isString, {
			this.addValue;
			items = items.insert(this.lastIndex + 1, currentDrag);
			this.strings_(items);		
		},{		
			this.value_(currentDrag,true);
		});
	}
	
	
	defaultKeyDownAction { arg key, modifiers, unicode;
	
		var type, newEnv;
	
		//modifiers.postln; 16rF702
		//[unicode,modifiers].postln;
		
		if (unicode == 127, {  
			if (modifiers == 256, {
				// delete node
				env.removeAt(env.releaseNode);
				env.releaseNode= (env.releaseNode- 1).clip(1,env.levels.size- 1);
				this.env_(env);
				updateFunc.value(env);
				^this
			});
			if (modifiers == 524576, {
				// delete env
				newEnv=Env([1,1,0],[1,0],'lin',1);
				this.env_(newEnv);
				updateFunc.value(newEnv);
				^this
			});	
		});
		
		if ((unicode == 231)||(unicode == 3)||(unicode == 99)) { clipboard=Env(env.levels.copy,env.times.copy,env.curves,env.releaseNode); };
		
		if (((unicode == 8730)||(unicode == 22)||(unicode == 118))&&(clipboard.notNil)) 
			{ this.env_(Env(clipboard.levels.copy,clipboard.times.copy,clipboard.curves,clipboard.releaseNode)); updateFunc.value(env); ^this };	
		
		if (unicode == 63232) { 
			type=env.curves;
			if (type=='lin') { env.curves_('sin'); this.env_(env) ;updateFunc.value(env) ^this};
			if (type=='sin') { env.curves_('wel'); this.env_(env) ;updateFunc.value(env) ^this};
			if (type=='wel') { env.curves_('sqr'); this.env_(env) ;updateFunc.value(env) ^this};
			if (type=='sqr') { env.curves_('lin'); this.env_(env) ;updateFunc.value(env) ^this};
			^this 
		};
		
		if (unicode == 63233) {
			type=env.curves;
			if (type=='sqr') { env.curves_('wel'); this.env_(env) ;updateFunc.value(env) ^this};
			if (type=='wel') { env.curves_('sin'); this.env_(env) ;updateFunc.value(env) ^this};
			if (type=='sin') { env.curves_('lin'); this.env_(env) ;updateFunc.value(env) ^this};
			if (type=='lin') { env.curves_('sqr'); this.env_(env) ;updateFunc.value(env) ^this};
			^this 
		};
		
		if (unicode == 63234, {
			env.releaseNode= (env.releaseNode- 1).wrap(0,env.levels.size- 1);
			this.env_(env);
			updateFunc.value(env);
			^this
		}); // left
		
		if (unicode == 63235, {
			env.releaseNode= (env.releaseNode+ 1).wrap(0,env.levels.size- 1);
			this.env_(env);
			updateFunc.value(env);
			^this
		}); // right
		^nil		// bubble if it's an invalid key
	}

	env_{|e|
		this.initSCEnvelopeEdit(e, pointsPerSegment);
		updateFunc.value(env);
	}
	
	envNoAction_{|e|
		this.initSCEnvelopeEdit(e, pointsPerSegment);
		//updateFunc.value(env);
	}

	initSCEnvelopeEdit { arg argEnv, argPPS, setMinMax=true;
		env = argEnv;
		pointsPerSegment = argPPS.asInteger;
		if(setMinMax){
			minLevel = 0;
			maxLevel = 1;
			if(minLevel == maxLevel){ minLevel = 0.0 };
		};
		minTime = 0;
		maxTime = env.times.sum;
		totalDurRec = 1/(maxTime - minTime);
		
		absTimes = Array.newClear(env.times.size + 1);
		absTimes[0] = 0;
		for(1, env.times.size, { arg i;
			absTimes[i] = absTimes[i-1] + env.times[i-1];
		});
		
		numPoints = (pointsPerSegment * env.times.size) + 1;  // add 1 for the last point
		viewPoints = Array.with(Array.newClear(numPoints), Array.newClear(numPoints));
		
		this
			.selectionColor_(Color.clear)
			.drawLines_(true)	// resize broken when no lines drawn
			.drawRects_(true)
		;
		this.mouseDownAction_{|view, x, y, modifiers, buttonNumber, clickCount|
			this.defaultMauseDownAction(x, y, modifiers, buttonNumber, clickCount);
			updateFunc.value(env);
		};
		this.action = { arg view, x, y, modifiers, buttonNumber, clickCount;
			var bp, bpm1, bpp1, bpLevel, timePos;
				
			//[view, x, y, modifiers, buttonNumber, clickCount].postln; // does not work
					
			// if it's a breakpoint
			if((view.index % pointsPerSegment) == 0, {
				bp = view.index.div(pointsPerSegment);
				
				//modifiers.postln;
				
				bpm1 = bp - 1;
				bpp1 = bp + 1;
				
				bpLevel = view.currentvalue.linlin(0.0, 1.0, minLevel, maxLevel);
				env.levels[bp] = bpLevel;
				
				timePos = view.value[0][view.index].linlin(0.0, 1.0, minTime, maxTime);

				// first breakpoint
				if(bp == 0, {
					if( timePos <= absTimes[bpp1], {
						timePos=0;
						env.times[bp] = absTimes[bpp1] - timePos;
						absTimes[bp] = timePos;
					},{ // going past right break point
						//env.times[bp] = 0;
						//absTimes[bp] = absTimes[bpp1];
					});
					this.updateSegment(bp);
				// end breakpoint
				},{ if(bp == env.times.size, {
					if( timePos >= absTimes[bpm1], {
						timePos=absTimes[bpm1+1];
						env.times[bpm1] = timePos - absTimes[bpm1];
						absTimes[bp] = timePos;
					},{	// going past left break point
						//env.times[bpm1] = 0;
						//absTimes[bp] = absTimes[bpm1];
					});
					this.updateSegment(bpm1);		
				// a middle break point
				},{
					if(timePos > absTimes[bpp1], {	// past right break point
						env.times[bpm1] = absTimes[bp] - absTimes[bpm1];
						env.times[bp] = 0;
						absTimes[bp] = absTimes[bpp1];
					},{ if(timePos < absTimes[bpm1], { // past left break point
						env.times[bpm1] = 0;
						env.times[bp] = absTimes[bpp1] - absTimes[bp];
						absTimes[bp] = absTimes[bpm1];
					},{
						// set left segment dur
						env.times[bpm1] = timePos - absTimes[bpm1];
						
						// set right segment dur
						env.times[bp] = absTimes[bpp1] - timePos;
						
						absTimes[bp] = timePos;					
					}); });
					this.updateSegment(bpm1);
					this.updateSegment(bp);
					if((timePos <= absTimes[bpp1]) && (timePos >= absTimes[bpm1]), {


					});
				}); });
				
				//[env.times, env.levels].postln;
				this.redraw;
				updateFunc.value(env);
			});
		};
		
		this.updateAll;
		this.redraw;
		
		numPoints.do({ arg i;
			var colorIdx;
			// make a breakpoint
			if((i%pointsPerSegment) == 0, {
				this.setThumbSize(i, 10);
				
				// color code breakpoints
				if(i.div(pointsPerSegment) == env.releaseNode, {
					this.setFillColor(i, Color.red(0.8));
				},{
					if(i.div(pointsPerSegment) == env.loopNode, {
						this.setFillColor(i, Color.green(0.8));
					},{
						colorIdx=i/pointsPerSegment/(env.levels.size);
						colorIdx=1- (colorIdx/1.5);
						this.setFillColor(i, Color(colorIdx,colorIdx,1));
					});
				});
				
			// Other points should be hidden.
			},{ this.setThumbSize(i, 0) });
		});
		
	}
	
	defaultMauseDownAction{|x, y, modifiers, buttonNumber, clickCount|
		var level, time, bounds;
		bounds = this.bounds;
		//level = y.linlin(bounds.top, bounds.top+bounds.height, maxLevel, minLevel);
		//time = x.linlin(bounds.left, bounds.left+bounds.width, minTime, maxTime);
		
		level = y.linlin(0,bounds.height, maxLevel, minLevel);
		time = x.linlin(0,bounds.width, minTime, maxTime);

		
		
//		this.debug([time, level]);
		if(clickCount == 2){
			//add value
			//modifiers.postln;
			this.insertAtTime(time, level);
			numPoints=numPoints+1;
		}
	}

	// updates segment values in viewPoints array
	updateSegment { arg segNum;
		var time, slope, index1, index2, timeOffset;
		
		// update envelope cache
//		this.debug(env.levels === env.levels);
		env.times = env.times;
		
		segNum = segNum.asInteger;

		time = absTimes[segNum];
		timeOffset = absTimes[0];
		
		slope = env.times[segNum] / pointsPerSegment;

		index1 = pointsPerSegment * segNum;
		index2 = index1 + pointsPerSegment - 1 ;

		//slope.postln;
		
		for(index1, index2, { arg i;
			viewPoints[0][i] = time.linlin(minTime, maxTime, 0.0, 1.0);
			viewPoints[1][i] = env[(time - timeOffset)].linlin(minLevel, maxLevel, 0.0, 1.0);
			time = time + slope;
		});
		
		//viewPoints.postln;
		
		// draw break point at right level
		if(slope == 0, {
			viewPoints[1][index1] = env.levels[segNum].linlin(minLevel, maxLevel, 0.0, 1.0);
		});
		
		// the last segment has an extra point at the end
		if(segNum == (env.times.size-1), {
			index2 = index2 + 1;
			viewPoints[0][index2] = time.linlin(minTime, maxTime, 0.0, 1.0);
			viewPoints[1][index2] = env.levels.last.linlin(minLevel, maxLevel, 0.0, 1.0);
		});		
	}


}
*/
