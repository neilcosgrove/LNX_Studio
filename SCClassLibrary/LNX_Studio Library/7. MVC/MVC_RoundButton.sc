
// RoundButton

MVC_RoundButton : MVC_View {

	var scrollView, fontHeight=18, <states;

	initView{
		colors=colors++(
			'backgroundDisabled'	: Color.grey,
			'string'				: Color.black,
			'background'			: Color(0,0,0,0.3)
		);
		states=[];
		canFocus=true;
	}

	// set the states
	states_{|array|
		states=array;
		if (view.notClosed) {
			view.states_(states);
		};
	}

	// create the button
	createView{
		view=RoundButton(window, rect)
			.border_(1.5)
			.radius_(8)
			.states_(states)
			.font_(font)
			.value_(value);
	}

	// the controls
	addControls{
		var val, val2;
		view.action_{|me|
			this.viewDoValueAction_(me.value,nil,true,false);
		};
		view.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			if (modifiers==524576) { buttonNumber=1 };
			if (modifiers==262401) { buttonNumber=2 };
			buttonPressed=buttonNumber;
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) {
				lw=lh=nil;
				startX=x;
				startY=y;
				view.bounds.postln;
			};
			if (buttonNumber==2) { this.toggleMIDIactive };
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode) { this.moveBy(x-startX,y-startY,buttonPressed) };
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
			this.refreshValue;
		};
	}

	// fresh the Slider Value
	refreshValue{ if (view.notClosed) { view.value_(value); MVC_LazyRefresh.incRefresh; } }

	// unlike SCView there is no refresh needed with SCSlider
	refresh{}

}

// everything is relative these days

+ RoundButton {

	init { |parent, bounds|
		//relativeOrigin = true;
		super.init( parent, bounds );
		super.focusColor = Color.clear;
	}

}

