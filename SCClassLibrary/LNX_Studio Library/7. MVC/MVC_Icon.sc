
// LNX_BevelPushOnOffView

MVC_Icon : MVC_View {

	var <down=false, <icon='power';

	// set your defaults
	initView{
		// [ 0:onBG, 1:offBG, 2:onOffText, 3:onBGUnen, 4:offBUnen, 5:onOffTextUnen ]
		colors=colors++(
			'iconUp'	: Color.black,
			'iconDown'	: Color.black
		);
		font=Font("Helvetica",12);
	}

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					if (enabled) {
						if (down) {
							colors[\iconDown].set;
						}{
							colors[\iconUp].set;
						};
					}{
						(colors[\disabled]/2).set;
					};
					Pen.smoothing_(true);
					DrawIcon.symbolArgs(icon,Rect(0,0,w,h));
				}; // end.pen
			};
	}

	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
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
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
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
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				down=false;
				this.viewDoValueAction_((value+1).asInt.wrap(0,strings.size-1),nil,true,false);
			};
		};
	}

	// set down if needed
	down_{|bool|
		down=bool;
		if (view.notClosed) {view.refresh}
	}

	// set the icon
	icon_{|symbol|
		icon=symbol;
		if (view.notClosed) {view.refresh}
	}

}
