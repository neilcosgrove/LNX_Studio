
// used to make peak meters

/*
(
w=MVC_Window();
MVC_FlatDisplay(w,0.5.asModel,Rect(10,10,6,200));
w.create;
)
*/

MVC_FlatDisplay : MVC_View {

	var <>right=false, <>direction=\vertical, <lastDrawValue;

	var <>invert=false;

	initView{
		colors=colors++(
			'border'		: Color.black.alpha_(0.75),
			'background'	: Color.black.alpha_(0.2),
			'slider'		: Color.orange*1.33
		);
		if (w>h) { direction=\horizontal }{ direction=\vertical };

	}

	// set the bounds, account for direction
	bounds_{|argRect|
		rect=argRect;
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		if (w>h) { direction=orientation=\horizontal }{ direction=orientation=\vertical };
		if (view.notClosed) { view.bounds_(rect) };
		this.adjustLabels;
	}

	// also called from model, sets the value of the gui item
	value_{|val|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if (value!=val) {
			var thisDrawValue;

			value=val;

			// the following stops refresh on screen if need value draws in new location
			// i.e if 0.001 followed by 0.0 result in the same thing been drawn then
			// there is no refresh;

			if (controlSpec.notNil) {
				val=controlSpec.unmap(value); // this will always give (0-1)
			}{
				val=value.clip(0,1);
			};

			thisDrawValue = ((h-4)*(1-val)+2).ceil;

			if (thisDrawValue != lastDrawValue) {
				lastDrawValue = thisDrawValue;
				this.refresh;
			};
		};
	}

	createView{

		var bgRect, r2=Rect(0,0,0,0), r3=Rect(0,0,0,0);

		view=UserView.new(window,rect)
			.drawFunc={|me|
				var val, hv;
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };

				Pen.use{

					Pen.smoothing_(false);

					colors[\background].set;
					if (bgRect.isNil) {bgRect=Rect(0,0,w,h)};
					Pen.fillRect(bgRect);

					if (controlSpec.notNil) {
						val=controlSpec.unmap(value); // this will always give (0-1)
					}{
						val=value.clip(0,1);
					};

					hv = ((h-4)*(1-val)+2).ceil;

					if (invert) {
						r2.set( 2, 2, w-4, hv-2);
						r3.set( 1, 1, w-2, hv);
					}{
						r2.set( 2, hv, w-4, h-hv-2);
						r3.set( 1, hv-1, w-2, h-hv);
					};

					colors[\border].set;
					Pen.fillRect(r3);
					colors[\slider].set;
					Pen.fillRect(r2);

				};
			};
		}

	addControls{

		// i should do this everywhere in own method
		view.onClose_{|me|
			onClose.value(this);
			me.drawFunc_(nil).mouseDownAction_(nil).mouseMoveAction_(nil).onClose_(nil);
		};

		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			startX=x;
			startY=y;
		//	mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			if (editMode||viewEditMode) {lw=lh=nil; if (verbose==verbose) {view.bounds.postln} };
			if (modifiers==524576)	{buttonNumber = 1.5    };
			if (modifiers==262401)	{buttonNumber = 2      };
			if (buttonNumber==2)	{this.toggleMIDIactive };
			buttonPressed=buttonNumber; // store for move
		};

		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed);
			};
		};

	}

}
