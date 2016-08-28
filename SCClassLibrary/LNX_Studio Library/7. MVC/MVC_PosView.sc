
// LNX_PosView

MVC_PosView : MVC_View {

	var <>buttonWidth=18, <highlight, <>gap=1, <>type='rect';

	var <>extend=false;

	// set your defaults
	initView{

		// [ 0:onBG, 1:offBG, 2:onOffText, 3:onBGUnen, 4:offBUnen, 5:onOffTextUnen ]
		colors=colors++(
			'background'			: Color.grey/3,
			'backgroundDisabled'	: Color.grey/2,
			'on'					: Color.orange,
			'disabled'				: Color(0.4,0.4,0.4),
			'highLight'				: Color.orange*1.2
		);

	}

	// make the view
	createView{
		view=UserView.new(window,extend.if(rect.resizeBy(0,4),rect))
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					if (showLabelBackground) {
						Color.black.alpha_(0.2).set;
						Pen.fillRect(Rect(0,0,w,h));
					};
					Pen.smoothing_(false);
					colors[enabled.if(\background,\backgroundDisabled)].set;
					Pen.fillRect(Rect(0,0,w,h));

					colors[enabled.if(\on,\disabled)].set;

					if (type=='rect') {
						Pen.fillRect(Rect(buttonWidth*(value.round(1))+gap,gap,
							buttonWidth-1,h-(gap*2)));
					}{
						Pen.smoothing_(true);
						Pen.fillOval(Rect(buttonWidth*(value.round(1))+gap,gap-1,
							buttonWidth-1,h-(gap*2)).centerSquare);
					};

					if (highlight.notNil) {
						colors['highLight'].set;
						if (type=='rect') {
							Pen.strokeRect(Rect(buttonWidth*highlight,1,buttonWidth,h-1));
						}{
							Pen.strokeOval(Rect(buttonWidth*highlight,1,buttonWidth,h-1)
											.centerSquare);
						};
					};


				}; // end.pen
			};
	}

	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) { lw=lh=nil;startX=x; startY=y; view.bounds.postln }; // for moving
			buttonPressed=buttonNumber;
			evaluateAction=true;
			if (modifiers==524576) { buttonPressed=1 };
			if (modifiers==262401) {buttonNumber=2};
			if (buttonNumber==2) { this.toggleMIDIactive };
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);

		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode) { this.moveBy(x-startX,y-startY) };
		};
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				if (buttonPressed==0) {
					//this.viewValueAction_(value+1,nil,true,false);
				};
			};
		};
	}

	highlight_{|step|
		highlight=step;
		if (view.notClosed) { this.refresh }
	}

}
