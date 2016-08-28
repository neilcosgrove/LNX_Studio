
// MVC_MyKnob2RangeView need to be added to mvc_windows in a seperate way and free'd with window

// LNX_MyKnob2View

MVC_MyKnob2 : MVC_View {

	var <>rangeView,	<controlSpec2,  <>resoultion=1;

	var <value2=0, <min2, <max2, <step2;

	initView{
		// 0: Color.ndcKnobOn, 1: Color.ndcKnobOff, 2: Color.ndcKnobPin, 3: Color.ndcKnobText
		colors=colors++(
			'on' 			: Color.ndcKnobOn,
			'off'			: Color.ndcKnobOff,
			'midiLearn2'	: Color.magenta,
			//'pin'		: Color.ndcKnobOn,  // if 'pin' not defined then it uses 'on' instead
			'centre'		: Color.ndcKnobPin,
			'string'		: Color.ndcKnobText
		);
		isSquare=true;
		this.numberFunc_(\freq);
	}

	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				var di,active,col,val,val2;
				var radius,offset,circum;
				var x1,x2,x3;
				MVC_LazyRefresh.incRefresh;
				if (verbose) { ['MVC_MyKnob2', 'drawFunc' , label].postln };

				if (showLabelBackground) {
					Color.black.alpha_(0.2).set;
					Pen.fillRect(Rect(0,0,w,h));
				};

				// useful numbers for working out the angles
				radius=w/2;
				offset=2pi*21/64;
				circum=2pi*54/64;

				// unmap both values
				if (controlSpec.notNil) {
					val=controlSpec.unmap(value);
				}{
					val=value.clip(0,1);
				};
				if (controlSpec2.notNil) {
					val2=controlSpec2.unmap(value2);
				}{
					val2=value2.clip(0,1);
				};

				// left & right values	clipped
				x1=(val-val2).clip(0,1);
				x2=(val+val2).clip(0,1);
				x3=val.clip(0,1);

				// what colour should it be
				active= (midiLearn or: {midiLearn2} or: {enabled.not} ).not;
				if (active.not) {
					col= midiLearn.if(\midiLearn,\disabled);
					if (midiLearn2) {col=\midiLearn2};
				};

				Pen.use{

					Pen.smoothing_(true);

					// innner fill background
					colors[active.if(\on,col)].darken(0.5).set;
					Pen.addWedge(
						(radius)@(radius),
						radius-3,
						offset,
						circum
					);
					Pen.perform(\fill);

					// centre pin dark
					Color.black.set;
					Pen.addWedge(
						(radius)@(radius),
						4,
						0,
						2pi
					);
					Pen.perform(\fill);

					// left line dark
					Pen.width_(3);
					Pen.addWedge(
						(radius)@(radius),
						radius,
						2pi*(x1.map(0,1,0.3295,1.1705)),
						0
					);
					// right line dark
					Pen.addWedge(
						(radius)@(radius),
						radius,
						2pi*(x2.map(0,1,0.3295,1.1705)),
						0
					);
					Pen.perform(\stroke);

					// outter ring off 1st section
					colors[active.if(\off,col)].set;
					Pen.addAnnularWedge(
						(radius)@(radius),
						radius-3,
						radius,
						offset,
						circum*x1
					);

					// outter ring off 2nd section
					Pen.addAnnularWedge(
						(radius)@(radius),
						radius-3,
						radius,
						offset+(circum*x2),
						circum*((1-x2))
					);
					Pen.perform(\fill);

					// outter ring on
					colors[active.if(\on,col)].set;
					di=circum*x1;
					Pen.addAnnularWedge(
						(radius)@(radius),
						radius-3,
						radius,
						offset+di,
						circum*x2-di
					);
					Pen.perform(\fill);


					// middle line dark
					Color.black.set;
					Pen.width_(2);
					Pen.addWedge(
						(radius)@(radius),
						radius-1,
						2pi*(x3.map(0,1,0.3295,1.1705)),
						0
					);
					Pen.perform(\stroke);

					// innner fill on
					colors[active.if(\on,col)].darken(0.5).set;
					Pen.addWedge(
						(radius)@(radius),
						radius-3,
						offset+di,
						circum*x2-di
					);
					Pen.perform(\fill);

					// line from pin to outter ring, left
					colors[active.if(\on,col)].set;
					Pen.width_(2);
					Pen.addWedge(
						(radius)@(radius),
						radius,
						2pi*(x1.map(0,1,0.3295,1.1705)),
						0
					);

					// line from pin to outter ring, right
					Pen.addWedge(
						(radius)@(radius),
						radius,
						2pi*(x2.map(0,1,0.3295,1.1705)),
						0
					);
					Pen.perform(\stroke);

					// centre pin
					colors[active.if(colors[\pin].notNil.if(\pin,\on),col)].set;
					Pen.addWedge(
						(radius)@(radius),
						3,
						0,
						2pi
					);
					Pen.perform(\fill);

				}; // end.pen

			};

	}

	addControls{

		var toggle;

		view.mouseUpAction={ MVC_LazyRefresh.mouseUp };

		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			MVC_LazyRefresh.mouseDown;
			toggle = false;
			startX = x;
			startY = y;
			if (editMode||viewEditMode) { lw=lh=nil; view.bounds.postln };
			if (y>w) {buttonNumber=1}; // numbers

			if (modifiers==524576) {buttonNumber=1};
			if (modifiers==262401) {clickCount=2};
			buttonPressed=buttonNumber;

			if (buttonPressed==1) {
				if (controlSpec2.notNil)
					{ startVal=controlSpec2.unmap(value2) }
					{ startVal=value2};
			}{
				if (controlSpec.notNil) { startVal=controlSpec.unmap(value) }{ startVal=value};


				if (hasMIDIcontrol) {
					if ((clickCount>1)&&doubleClickLearn){ toggle = true };
					if (modifiers==262401) { toggle = true  };
					if (buttonNumber>=1  ) { toggle = true  };
					if (toggle) { this.toggleMIDIactive };
				};

			};

		};

		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var val;
			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY)
			}{
				if (toggle.not) {
					if (buttonPressed!=1) {
						// set value 1
						val=(startVal+
								((startY-y)/resoultion/200/(buttonPressed*6+1))).clip(0,1);
						if (controlSpec.notNil) { val=controlSpec.map(val) };
						if (val!=value) { this.viewValueAction_(val,nil,true,false) };
					}{
						// set value 2
						val=(startVal+((startY-y)/resoultion/25/(buttonPressed*6+1))).clip(0,1);
						if (controlSpec2.notNil) { val=controlSpec2.map(val) };
						if (val!=value2) {
							// this is normally done in viewValueAction_
							value2=val;
							this.refresh;
							action2.value(this,nil,true,false);
							if (rangeView.notNil) {
								rangeView.viewValueAction_(val,nil,true,false);
							};
						}

					};
				}
			};
		};

	}


	// add the controls to the number box
	// this is overridden in the various views
	addNumberControls{
		numberGUI.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			startX=x;
			startY=y;
			if (editMode) { view.bounds.postln };
			if (y>w) {buttonNumber=1}; // numbers
			if (modifiers==524576) {buttonNumber=1};
			if (modifiers==262401) {buttonNumber=2};
			buttonPressed=buttonNumber;
			if (buttonPressed==1) {
				if (controlSpec2.notNil)
					{ startVal=controlSpec2.unmap(value2) }
					{ startVal=value2};
			}{
				if (controlSpec.notNil) { startVal=controlSpec.unmap(value) }{ startVal=value};
			};
			if (buttonNumber==2) { this.toggleMIDIactive };
			numberHeld=true;
			numberGUI.refresh;
		}
		.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var val;
			if (editMode) {
				this.moveBy(x-startX,y-startY)
			}{
				if (buttonPressed!=1) {
					// set value 1
					val=(startVal+((startY-y)/800/(buttonPressed*6+1))).clip(0,1);
					if (controlSpec.notNil) { val=controlSpec.map(val) };
					if (val!=value) { this.viewValueAction_(val,nil,true,false) };
				}{
					// set value 2
					val=(startVal+((startY-y)/400/(buttonPressed*6+1))).clip(0,1);
					if (controlSpec2.notNil) { val=controlSpec2.map(val) };
					if (val!=value2) {
						// this is normally done in viewValueAction_
						value2=val;
						this.refresh;
						action2.value(this,nil,true,false);
						if (rangeView.notNil) {
							rangeView.viewValueAction_(val,nil,true,false);
						};
					}

				};
			};
		}
		.mouseUpAction_{
			numberHeld=false;
			numberGUI.refresh;
		}
	}

	toggleMIDIactive{
		if (hasMIDIcontrol && hasMIDIcontrol2) {
			if (midiLearn) {
				rangeView.midiLearnToModel_(true);
			}{
				if (midiLearn2) {
					rangeView.midiLearnToModel_(false);
				}{
					model.midiLearn_(true);
				};
			};
		}{
			// both of these can't be true
			if (hasMIDIcontrol)  { model.midiLearn_(midiLearn.not) };
			if (hasMIDIcontrol2) { rangeView.midiLearnToModel_(midiLearn2.not) };
		};
	}

	// catch any zero values from knob to knob2
	zeroValue_{}

	// normally called from model, sets the gui item value
	value2_{|val|
		if (controlSpec2.notNil) { val=controlSpec2.constrain(val) };
		if (value2!=val) {
			value2=val;
			this.refresh;
		};
	}

	// map the item to min, max and step
	map2_{|argMin,argMax,argStep|
		"Warning: Map in ".post;
		this.class.post;
		" is depressiated".postln;
		this.controlSpec2_([argMin,argMax,\lin,argStep?0]);
	}

	// assign a ControlSpec to map to
	controlSpec2_{|spec|
		if (spec.notNil) {
			controlSpec2=spec.asSpec;
			value2=controlSpec2.constrain(value2);
			this.refresh;
		}{
			controlSpec2=nil;
		}
	}

	// make the view cyan / magenta for midiLearn active
	midiLearn2_{|bool|
		midiLearn2=bool;
		this.refresh;
		labelGUI.do(_.refresh);
	}

		// deactivate midi learn from this item
	deactivate2{
		midiLearn2=false;
		this.refresh;
		labelGUI.do(_.refresh);
	}
}

// use to connect a MVC_Model to a MVC_MyKnob2 range
// it redirect method calls to secondary value methods

MVC_MyKnob2RangeView {

	var model,knob; // only 2 vars : comms between 2, keep it simple

	// ok

	*new {|model,knob| ^super.new.init(model,knob) }

	init {|argModel,argKnob|
		model=argModel;
		knob=argKnob;
		knob.rangeView_(this);
		model.addView(this);  // add to the model
	}

	map_{|min,max,step| knob.map2_(min,max,step) }

	controlSpec_{|spec| knob.controlSpec2_(spec) }

	refresh{ knob.refresh }

	value { ^knob.value2 }

	value_ {|val| knob.value2_(val)  }

	action_{|val| knob.action2_(val) }

	create{}

	// model >> view

	hasMIDIcontrol{ knob.hasMIDIcontrol2 }

	hasMIDIcontrol_{|bool| knob.hasMIDIcontrol2_(bool) }

	midiLearn_{|bool| knob.midiLearn2_(bool)}

	deactivate{ knob.deactivate2 }

	free{ knob=nil; model=nil }

	remove{ knob=nil }

	enabled_{}

	string_{}

	// view >> model

	viewValueAction_{|val,latency,send,toggle| model.valueAction_(val,latency,send,toggle,this) }

	midiLearnToModel_{|bool| model.midiLearn_(bool) }

} // end. My Knob2 container

