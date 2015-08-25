
// LNX_BevelPushOnOffView

MVC_FlatButton2 : MVC_View {

	var <>mouseMode = 'switch'; // nil = default 'switch', 'tapUp'
	var flashState = 1, flashTask, lastFlashTime, <>flashTime = 0.33, <>flashDuration = inf;
	var down=false, clickCount;


	flash{
		if (flashTask.isNil) {	
			flashState = 1;
			lastFlashTime = AppClock.now;
			flashTask = {{
				var now;
				flashState = 1 - flashState;
				now =  AppClock.now;
				if ( (now-lastFlashTime) > flashDuration) {
					flashTask = nil;
					flashState = 1;
					this.refresh;
					flashTask.stop
				}{
					this.refresh;
					flashTime.wait;
				}
			}.loop}.fork(AppClock);
		}{
			lastFlashTime = AppClock.now;	
		};
	}
	
	flashStop{
		flashTask.stop;
		flashState = 1;
		flashTask = nil;
		{this.refresh}.defer;
	}
	
	
	// set your defaults
	initView{
		// [ 0:onBG, 1:offBG, 2:onOffText, 3:onBGUnen, 4:offBUnen, 5:onOffTextUnen ] 
		colors=colors++(
			'background'	: Color(0.1,0.1,0.1),
			'off'		: Color.red,
			'on'			: Color.green
		);		
	}
	
	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);
					Color.black.set;
					Pen.fillRect(Rect(0,0,w,h));
					if (down) {
						(colors[\background]/1.3).set;
					}{
						colors[\background].set;
					};
					if (midiLearn) {
						colors[\midiLearn].set;
					};
					Pen.fillRect(Rect(1,1,w-2,h-2));
				
					if (down) {
						Color(0,0,0,0.55).set;
						Pen.fillRect(Rect(1,1,w-3,1));
						Pen.fillRect(Rect(1,1,1,h-3));
						
						Color(1,1,1,0.15).set;
						Pen.fillRect(Rect(w-2,2,1,h-3));
						Pen.fillRect(Rect(2,h-2,w-3,1));
						
						Color(0,0,0,0.13).setFill;
						Pen.moveTo(2@2);
						Pen.lineTo((w-3)@2);
						Pen.lineTo(2@(h-3));
						Pen.lineTo(2@2);
						Pen.fill;
						
						if (value==1) {Pen.fillColor_(colors[\on]/1.5)}{Pen.fillColor_(colors[\off]/1.5)};
					}{
						Color(1,1,1,0.2).set;
						Pen.fillRect(Rect(1,1,w-3,1));
						Pen.fillRect(Rect(1,1,1,h-3));
						Color(0,0,0,0.45).set;
						Pen.fillRect(Rect(w-2,2,1,h-3));
						Pen.fillRect(Rect(2,h-2,w-3,1));
						
						Color(1,1,1,0.065).setFill;
						Pen.moveTo(2@2);
						Pen.lineTo((w-3)@2);
						Pen.lineTo(2@(h-3));
						Pen.lineTo(2@2);
						Pen.fill;
						
						if (value==1) {Pen.fillColor_(colors[\on]*flashState)} {Pen.fillColor_(colors[\off]*flashState)};
					};
					
					if (down) {				
						Pen.font_(font.copy.size_(font.size-1));
					}{
						Pen.font_(font);
					};
						
					Pen.smoothing_(true);
					if (value==1) {
						Pen.stringCenteredIn(strings@@1,Rect(0,0,w,h));
					}{
						Pen.stringCenteredIn(strings[0],Rect(0,0,w,h));
					};
				}; // end.pen

		}	
	}
	
	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, argClickCount|
			clickCount=argClickCount;
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (modifiers.asBinaryDigits[4]==0) { // check apple not pressed because of drag
				if (editMode) {
					lw=lh=nil; startX=x; startY=y; view.bounds.postln; // for moving
				}{
					buttonPressed=buttonNumber;
					evaluateAction=true;
					if (modifiers==524576) { buttonPressed=1 };
					if (modifiers==262401) {buttonNumber=2};
					if (buttonNumber==2) {
						this.toggleMIDIactive
					}{
						down=true;
						view.refresh;
					};
				}
			};
			
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber|
			var xx=x/w, yy=y/h;
			if (editMode) {
				this.moveBy(x-startX,y-startY)
			}{				
				if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}) {
					if (down.not) {
						down=true;
						view.refresh;
					};
				}{
					if (down) {
						down=false;
						view.refresh;
					}
				};
			};
		};
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber|
			var xx=x/w, yy=y/h;
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				down=false;
				if (mouseMode==\tapUp) {
					this.viewDoValueAction_(value,nil,true,false,clickCount);
				}{
					this.viewDoValueAction_(
						(value+1).asInt.wrap(0,strings.size-1),nil,true,false,clickCount);
				};
			};
		};
	}

}
