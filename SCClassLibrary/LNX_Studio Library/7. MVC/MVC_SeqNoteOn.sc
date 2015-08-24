
// LNX_SeqNoteOn

MVC_SeqNoteOn : MVC_View {

	// set your defaults
	initView{
		colors=colors++(
			'background'	: Color.black,
			'border'		: Color(0.3686,0.3373,0.2412),
			'on'			: Color.green,
			'onDisabled'	: Color.grey+0.1
		);
		isSquare=true;
		// [ 0:onBG, 1:offBG, 2:onOffText, 3:onBGUnen, 4:offBUnen, 5:onOffTextUnen ] 
		// colors=[Color.ndcOnOffON,Color.ndcOnOffOFF,Color.ndcOnOffText,
		//		Color.ndcOnOffONUen,Color.ndcOnOffOFFUen,Color.ndcOnOffTextUnen];
		
	}
	
	// make the view
	createView{
		view=SCUserView.new(window,rect)
			.drawFunc={|me|
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);
					colors[ enabled.if(\background,\disabled)].set;
					Pen.fillRect(Rect(0,0,w,h));
					
					if (midiLearn) {
						colors[\midiLearn].set;
					}{
						colors[ enabled.if(\border,\onDisabled)].set;
					};	
					Pen.fillRect(Rect(1,1,w- 2,h- 2));				
					colors[ enabled.if(\background,\disabled)].set;
					Pen.fillRect(Rect(2,2,w-4,h-4));

					if (value>0) {
						if (midiLearn) {
							colors[\midiLearn].set;
						}{
							colors[ enabled.if(\on,\onDisabled)].set;
						};
						Pen.fillOval(Rect(4,4,w-8,h-8));
						if (midiLearn) {
							colors[\midiLearn].set;
						}{
							colors[ enabled.if(\on,\onDisabled)].darken(0.5).set;
						};
						Pen.fillOval(Rect(5,5,w-10,h-10));
					};
				}; // end.pen
			};		
	}
	
	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) {
				lw=lh=nil;startX=x; startY=y; view.bounds.postln; // for moving
			}{
				buttonPressed=buttonNumber;
				evaluateAction=true;
				if (modifiers==524576) { buttonPressed=1 };
				if (modifiers==262401) {buttonNumber=2};
				if (buttonNumber==2) {
					this.toggleMIDIactive
				}{
					this.viewValueAction_((value>=0.5).if(0,1),nil,true,false);
				};
			}
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode) { this.moveBy(x-startX,y-startY) };
		};

	}
	
	// set the colour in the Dictionary 
	color_{|key,color|
		if (key=='slider') {key='on'};
		if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		if (key=='focus') {
			{
				if (view.notClosed) { view.focusColor_(color) }
			}.defer;	
		}{
			this.refresh;
		}
	}

}
