
/////////// MVC_OnOffView /////////////////////////////////////////////////////////////////

MVC_MultiOnOffView : MVC_View {

	var <states;

	// set your defaults
	initView{
		colors=colors++(
			'background'	: Color.black
		);
		states=[
			// 0      1            2           3           4
			["Low"   ,Color.red   ,Color.black,Color.grey/3,Color.grey/2],
			["Med"   ,Color.yellow,Color.black,Color.grey/2,Color.grey/3],
			["Hi"    ,Color.green ,Color.black,Color.grey/4,Color.grey/2]
		];
	}
	
	// make the view
	createView{
		var state;
		view=SCUserView.new(window,rect)
			.drawFunc={|me|
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				state=states[value];
				Pen.use{
					Pen.smoothing_(false);
					colors[\background].set;
					Pen.fillRect(Rect(0,0,w,h));
					if (state.notNil) {
						if (midiLearn) {
							colors[\midiLearn].set;
						}{
							state[enabled.if(1,3)].set;
						};	
						Pen.fillRect(Rect(1,1,w- 2,h- 2));
						Pen.font_(font);
						Pen.fillColor_(state[enabled.if(2,4)]);
						Pen.smoothing_(true);		
						Pen.stringCenteredIn(state[0].asString,Rect(0,0,w,h));
					};
				}; // end.pen
			};		
	}
	
	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) { lw=lh=nil; startX=x; startY=y; view.bounds.postln }; // for moving
			buttonPressed=buttonNumber;
			evaluateAction=true;
			if (modifiers==524576) { buttonPressed=1 };
			if (modifiers==262401) {buttonNumber=2};
			if (buttonNumber==2) { this.toggleMIDIactive };
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode) { this.moveBy(x-startX,y-startY) };
		};
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				if (buttonPressed==0) {
					this.viewValueAction_((value+1).asInt.wrap(0,states.size-1),nil,true,false);
				}{
					if (buttonPressed==1) {
						this.viewValueAction2_((value+1).asInt.wrap(0,states.size-1),nil,true,false);
					}
				};
			};
		};
	}
	
	// set the states
	states_{|array|
		states=array;
		if (view.notClosed) {
			view.refresh;
		};
	}
	
	// normally called from model
	value_{|val|
		if (states.notNil) {
			val=val.round(1).asInt.wrap(0,states.size-1);
		}{
			value=val.asInt;
		};
		if (value!=val) {
			value=val;
			this.refresh;
		};
	}

}
