
// LNX_MyLampView

MVC_LampView : MVC_View {

	var call=0; // used to tag last update to stop lamp turning off in mid noteOn

	var lastTime=0, nextTime, fps=25; // has its own fps

	// set your defaults
	initView{
		colors=colors++(
			'background'	: Color.ndcLampBG,
			'border'		: Color.ndcLampBorder,
			'on'			: Color.ndcLampON,
			'off'		: Color.ndcLampOFF
		);
		isSquare=true;
	}
	
	// make the view
	createView{
		view=SCUserView.new(window,rect)
			.drawFunc={|me|
				var val;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(true);
					Color(0.1,0.1,0.1,0.4).set;
					Pen.fillRect(Rect(0,0,w,h));
					colors[\border].set;
					Pen.fillRect(Rect(2,2,w-4,h-4));
					colors[\background].set;
					Pen.fillOval(Rect(2,2,w-4,h-4));
					
					if (controlSpec.notNil) {
						val=controlSpec.unmap(value); // this will always give (0-1)
						if (val>0)
							{ (colors[\on]*val).set; }
							{ colors[\off].set; };
					}{
						if (value>0)
							{ (colors[\on]*(value.map(0,1,0.5,1))).set; }
							{ colors[\off].set; };
					};
					Pen.fillOval(Rect(4,4,w-8,h-8));
				}; // end.pen
			};		
	}
	
	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) {lw=lh=nil; startX=x; startY=y; view.bounds.postln }; // for moving
			buttonPressed=buttonNumber;
			evaluateAction=true;
			if (modifiers==524576) { buttonPressed=1 };
			if (modifiers==262401) {buttonNumber=2};
			if (buttonNumber==2) { this.toggleMIDIactive };
			if ((buttonPressed==0)and:{editMode.not}) {
				this.viewDoValueAction_(value,nil,true,false);
			};
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode) { this.moveBy(x-startX,y-startY) };
		};
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|

		};
	}
	

	
	on { this.value_(1) }
	off{ this.value_(0) }
	
	value_{ |val,d|
		var c;
		value=val;
		call=call+1;
		c=call;
		this.lazyRefresh;
		if (d.isNumber) {
			{this.deferOff(c)}.defer(d);
		}
		^this
	}
		
	deferOff{|c|
		if (c==call) {
			value=0;
			this.lazyRefresh;
		}
	}	

	// only refresh at a frame rate
	lazyRefresh{
		var now;
		now=SystemClock.seconds;
		if ((now-lastTime)>(1/fps)) {
			lastTime=now;
			nextTime=nil;
			this.refresh;
		}{
			if (nextTime.isNil) {
				nextTime=lastTime+(1/fps);
				{
					this.refresh;
					nextTime=nil;
				}.defer(nextTime-now);
				lastTime=nextTime;
			}
		}
	
	}

}
