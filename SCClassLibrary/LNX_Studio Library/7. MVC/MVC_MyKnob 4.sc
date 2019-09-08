/*
(

~w = MVC_Window().create;
~m = [1,[1,10,1,1],{|me,value| [me,value.postln]  }].asModel;
~k = MVC_MyKnob4(~w, Rect(40,40,140,140), ~m);

~k.view.color_(~k.view.color[0] = Color.red); // knob
~k.view.color_(~k.view.color[1] = Color.green); // on
~k.view.color_(~k.view.color[2] = Color.blue); // off
~k.view.color_(~k.view.color[3] = Color.yellow); // hilite

[colors[\knob],colors[\on], colors[\off], colors[\hilite]]

)
*/

// LNX_MyKnobView

MVC_MyKnob4 : MVC_View {

	var <zeroValue=0, <>resoultion=1, <centered = false;

	var  preValue, <mouseDown=false;

	initView{
		// 0: Color.ndcKnobOn, 1: Color.ndcKnobOff, 2: Color.ndcKnobPin, 3: Color.ndcKnobText
		colors=colors++(
			\knob   : Color(0.91764705882353, 0.91764705882353, 0.91764705882353),
			\hilite : Color.black,
			\on     : Color.black,
			\off    : Color.white
		);
		colors[\background]=nil; // doesn't init as nil in sc code gui editor??
		isSquare=true;
		this.numberFunc_(\float2);
	}

	createView{

		view=Knob(window,rect)
		.mode_(\vert)
		.centered_(centered)
		.shift_scale_(1)
		.color_([colors[\knob],colors[\on], colors[\off], colors[\hilite]]);

		if (controlSpec.notNil) {
			view.value_(controlSpec.unmap(value))
		}{
			view.value_(value.clip(0,1));
		}


	}

	bounds_{|argRect|
		rect=argRect;
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		if (view.notClosed) { view.bounds_(rect) };
		this.adjustLabels;
	}

	addControls{

		var toggle;

		// i should do this everywhere in own method
		view.onClose_{|me|
			onClose.value(this);
			me.drawFunc_(nil).mouseDownAction_(nil).mouseMoveAction_(nil).onClose_(nil);
		};


		view.action_{|me|
			if ((editMode||viewEditMode).not) {
				if (controlSpec.notNil) {
					this.viewValueAction_(controlSpec.map(me.value),nil,true,false);
				}{
					this.viewValueAction_(me.value,nil,true,false);
				}
			}
		};

		 view.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
		 	// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			mouseDown = true;
		 	MVC_LazyRefresh.mouseDown;
		 	if (modifiers.isAlt ) { buttonNumber=1  };
		 	if (modifiers.isCtrl) { clickCount  =2  };
		 	buttonPressed=buttonNumber;
		 	preValue=value;
		 	mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
		 	if (editMode||viewEditMode) {
		 		lw=lh=nil;
		 		startX=x;
		 		startY=y;
		 		if (verbose) {view.bounds.postln};
		 	}{
		 		var toggle = false;
		 		if (hasMIDIcontrol) {
		 			if ((clickCount>1)&&doubleClickLearn){ toggle = true };
		 			if (modifiers.isCtrl) { toggle = true  };
		 			if (toggle) { this.toggleMIDIactive };
		 		};
		 	};
		 };
		 view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
		 	if (editMode||viewEditMode) { this.moveBy(x-startX,y-startY,buttonPressed) };
		 };
		 view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			mouseDown = false;
		 	MVC_LazyRefresh.mouseUp;
			this.refreshValue;
		 	if (editMode||viewEditMode) { {this.refreshValue}.defer(0.05) };
		 };

	}

	// need to do disable here
	color_{|key,color|
		if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		if (view.notClosed) {
			view.color_([colors[\knob],colors[\on], colors[\off], colors[\hilite]]);

		};
	}


	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		if (view.notClosed) {
			if (midiLearn) {
				view.color_(view.color[0] = colors[\midiLearn]);
			}{
				view.color_(view.color[0] = Color(0.91764705882353, 0.91764705882353, 0.91764705882353));
			};
		};
		labelGUI.do(_.refresh);
		this.refresh;
	}

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		if (view.notClosed) {
			view.color_(view.color[0] = Color(0.91764705882353, 0.91764705882353, 0.91764705882353));
		};
		labelGUI.do(_.refresh);
		this.refresh;
	}

	// item is enabled
	enabled_{|bool|
		if (enabled!=bool) {
			enabled=bool;
			{
				if (view.notClosed) {
					this.refresh;
				}
			}.defer;
		}
	}

	// assign a ControlSpec to map to
	controlSpec_{|spec|
		if (spec.notNil) {
			controlSpec=spec.asSpec;
			value=controlSpec.constrain(value);
			if (view.notClosed) {
				view.step_(controlSpec.step/(controlSpec.clipHi-(controlSpec.clipLo)));
				this.refreshValue;
				this.refresh;
			};
		}{
			controlSpec=nil;
			if (view.notClosed) {view.step_(0)};
		}
	}

	// normally called from model
	value_{|val|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if (value!=val) {
			value=val;
			this.refreshValue;
			this.refresh;
		};
	}


	// fresh the Slider Value
	refreshValue{
		if (view.notClosed) {
			if (controlSpec.notNil) {
				if (mouseDown.not) {view.value_(controlSpec.unmap(value))};
				MVC_LazyRefresh.incRefresh;
			}{
				if (mouseDown.not) {view.value_(value)};
				MVC_LazyRefresh.incRefresh;
			};
		}
	}


	// unlike SCView there is no refresh needed with SCSlider
	// this is not true, valueAction_ calls this and so value does need updating. Changed!
	refresh{
		if ((view.notClosed) and: { (numberGUI.notNil) }) {
			numberGUI.refresh;
			this.refreshValue;
		}
	}

	centered_{|bool|
		centered = bool;
		if (view.notClosed) { view.centered_(centered) };
	}

	// zero value is where on the knob the value is measured from (0-1)
	zeroValue_{|val|
		if (controlSpec.notNil) {
			zeroValue=controlSpec.unmap(val);
		}{
			zeroValue=val;
		};
		this.refresh;
	}

}