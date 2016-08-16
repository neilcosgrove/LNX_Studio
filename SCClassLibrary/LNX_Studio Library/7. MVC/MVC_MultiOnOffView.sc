
/////////// MVC_OnOffView /////////////////////////////////////////////////////////////////

MVC_MultiOnOffView : MVC_View {

	var <states, <>darkerWhenPressed=true, down=false;

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
		view=UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				state=states[value];
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

						if (darkerWhenPressed) {
							Pen.fillColor_(state[enabled.if(1,3)]/1.5)
						}{
							Pen.fillColor_(state[enabled.if(1,3)])
						}

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

						Pen.fillColor_(state[enabled.if(1,3)]);
					};


					if (state.notNil) {

						Pen.smoothing_(true);
						Pen.font_(font);
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
			down=true;
			this.refresh;
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode) { this.moveBy(x-startX,y-startY) };
		};

		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			var inBounds=(xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1};

			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}{
				if (	inBounds && (down.not) ) { down=true; this.refresh};
				if (	(inBounds.not) && (down) ) { down=false; this.refresh};
			}
		};

		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				if (buttonPressed==0) {
					this.viewValueAction_((value+1).asInt.wrap(0,states.size-1),nil,true,false);
				}{
					if (buttonPressed==1) {
						this.viewValueAction2_(
							(value+1).asInt.wrap(0,states.size-1),nil,true,false);
					}
				};
			};
			down=false;
			this.refresh;
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
