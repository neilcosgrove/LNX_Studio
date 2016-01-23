
// LNX_MySlider

MVC_Slider : MVC_View {

	var	<thumbSize=12, preValue;

	initView{
		colors=colors++(
			'knob'				: Color.orange,
			'backgroundDisabled'	: Color.grey/2,
			'knobDisabled'		: Color.grey*1.2
		);

	}
	
	createView{
		view=SCSlider.new(window,rect)
			.background_(colors[enabled.if(\background,\backgroundDisabled)])
			.knobColor_(colors[midiLearn.if(\midiLearn,enabled.if(\knob,\knobDisabled))])
			.thumbSize_(thumbSize);
		if (controlSpec.notNil) {
			view.value_(controlSpec.unmap(value))
				.step_(controlSpec.step/(controlSpec.clipHi-(controlSpec.clipLo)))
		}{
			view.value_(value.clip(0,1));
		}
	}
	
	// set the bounds, account for direction
	bounds_{|argRect|
		rect=argRect;
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		if (w>h) { orientation=\horizontal }{ orientation=\vertical };
		if (view.notClosed) { view.bounds_(rect) };
		this.adjustLabels;
	}
	
	addControls{
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
			if (modifiers==524576) { buttonNumber=1  };
			if (modifiers==262401) { clickCount=2  };
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
					if (modifiers==262401) { toggle = true  };
					if (buttonNumber>=1  ) { toggle = true  };
					if (toggle) { this.toggleMIDIactive };
				};
			};
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode||viewEditMode) { this.moveBy(x-startX,y-startY,buttonPressed) };
		};	
		
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode||viewEditMode) { {this.refreshValue}.defer(0.05) };
		};		
	}
	
	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		if (view.notClosed) {
			view.knobColor_(colors[midiLearn.if(\midiLearn,enabled.if(\knob,\knobDisabled))]);
		};
		labelGUI.do(_.refresh);
		this.refresh;
	}

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		if (view.notClosed) {
			view.knobColor_(colors[enabled.if(\knob,\knobDisabled)]);
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
					view.knobColor_(colors[
						midiLearn.if(\midiLearn,enabled.if(\knob,\knobDisabled))]);
					view.background_(colors[enabled.if(\background,\backgroundDisabled)]);
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
	
	// change the thumb size of the knob
	thumbSize_{|size|
		thumbSize=size;
		if (view.notClosed) {
			view.thumbSize_(size)
		}
	}
	
	// set the colour in the Dictionary
	// need to do disable here
	color_{|key,color|
		if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		if (key=='focus'      ) { {if (view.notClosed) { view.focusColor_(color) } }.defer };		if (key=='knob'       ) { {if (view.notClosed) { view.knobColor_ (color) } }.defer };
		if (key=='background' ) { {if (view.notClosed) { view.background_(color) } }.defer };
		if (key=='label') { {labelGUI.do(_.refresh)}.defer };
	}
	
	// add a dict of colours, useful for colour themes
	colors_{|dict|
		dict.pairsDo{|key,color|
			if (colors.includesKey(key)) {
				colors[key]=color;
				if (key=='focus'     ) { {if (view.notClosed) {view.focusColor_(color)}}.defer };
				if (key=='knob'      ) { {if (view.notClosed) {view.knobColor_ (color)}}.defer };
				if (key=='background') { {if (view.notClosed) {view.background_(color)}}.defer };
				if (key=='label') { {labelGUI.do(_.refresh)}.defer };
			}
		};
	}
	
	// fresh the Slider Value
	refreshValue{
		if (view.notClosed) {
			if (controlSpec.notNil) {
				view.value_(controlSpec.unmap(value));
			}{
				view.value_(value);
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

}
