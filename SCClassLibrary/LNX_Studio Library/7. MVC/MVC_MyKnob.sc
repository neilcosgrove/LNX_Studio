
// LNX_MyKnobView

MVC_MyKnob : MVC_View {

	var <zeroValue=0, <>resoultion=1, <penWidth=\auto;

	initView{
		// 0: Color.ndcKnobOn, 1: Color.ndcKnobOff, 2: Color.ndcKnobPin, 3: Color.ndcKnobText
		colors=colors++(
			'on' 	: Color.ndcKnobOn,
			'off'	: Color.ndcKnobOff,
			'pin'	: Color.ndcKnobPin
		);
		colors[\background]=nil; // doesn't init as nil in sc code gui editor??
		isSquare=true;
		this.numberFunc_(\float2);
	}

	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				var di,str,active,col,val;
				var radius,a1,a2,zeroClipped;
				var pW;
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };

				if (showLabelBackground) {
					Color.black.alpha_(0.2).set;
					Pen.fillRect(Rect(0,0,w,h));
				};

				radius=w/2;
				a1=2pi*21/64;
				a2=2pi*54/64;

				if (penWidth==\auto) {
					pW=w*0.11538461538462;
				}{
					pW=penWidth;
				};

				if (controlSpec.notNil) {
					val=controlSpec.unmap(value);
				}{
					val=value.clip(0,1);
				};

				zeroClipped=zeroValue.clip(-2pi,2pi);

				active= (midiLearn or: (enabled.not)).not;
				if (active.not) {
					col= midiLearn.if(\midiLearn,\disabled)
				};

				Pen.use{

					Pen.smoothing_(true);

					colors[active.if(\off,col)].set;
					di=a2*zeroClipped;
					// right side
					Pen.addAnnularWedge(
						(radius)@(radius),
						radius-pW,
						radius,
						a1+(a2*(val.clip(zeroClipped,1))),
						a2-(a2*(val.clip(zeroClipped,1)))
					);
					// left side
					Pen.addAnnularWedge(
						(radius)@(radius),
						radius-pW,
						radius,
						a1+(a2*(0.clip(0,zeroClipped))),
						a2*(val.clip(0,zeroClipped))
					);
					Pen.perform(\fill);
					colors[active.if(\on,col)].set;
					// active section (zero value to value)
					di=a2*zeroClipped;
					Pen.addAnnularWedge(
						(radius)@(radius),
						radius-pW,
						radius,
						a1+di,
						a2*val-di
					);
					Pen.perform(\fill);
					// line from centre to value
					colors[active.if(\pin,col)].set;
					Pen.width_(pW);
					Pen.addWedge(
						(radius)@(radius),
						radius,
						2pi*(val.map(0,1,0.3295,1.1705)),
						0
					);
					Pen.perform(\stroke);
					// centre bump
					Pen.addWedge(
						(radius)@(radius),
						(pW)/2,
						0,
						2pi
					);
					Pen.perform(\fill);
				}; // end.pen

			};

	}

	addControls{

		var toggle;

		// i should do this everywhere in own method
		view.onClose_{|me|
			onClose.value(this);
			me.drawFunc_(nil).mouseDownAction_(nil).mouseMoveAction_(nil).onClose_(nil);
		};

		view.mouseUpAction={ MVC_LazyRefresh.mouseUp };

		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			MVC_LazyRefresh.mouseDown;
			toggle = false;

			startX=x;
			startY=y;
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			if (editMode||viewEditMode) {lw=lh=nil; if (verbose==verbose) {view.bounds.postln} };

			if (modifiers==262401)	{clickCount = 2 };

			if (modifiers==524576)	{
				buttonNumber = 1.5
			}{
				if (hasMIDIcontrol) {
					if ((clickCount>1)&&doubleClickLearn){ toggle = true };
					if (modifiers==262401) { toggle = true  };
					if (buttonNumber>=1  ) { toggle = true  };
					if (toggle) { this.toggleMIDIactive };
				};
			};

			buttonPressed=buttonNumber; // store for move
			if (controlSpec.notNil) {
				startVal=controlSpec.unmap(value);
			}{
				startVal=value;
			};
		};

		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var val;

			//[me, x, y, modifiers, buttonNumber, clickCount].postln;

			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed);
			}{
				if (toggle.not) {
					val=(startVal+((startY-y)/resoultion/200/(buttonPressed*6+1))).clip(0,1);
					if (controlSpec.notNil) { val=controlSpec.map(val) };
					if (val!=value) { this.viewValueAction_(val,nil,true,false) };
				};
			};
		};

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

	penWidth_{|pixels|
		penWidth=pixels;
		this.refresh;
	}

}