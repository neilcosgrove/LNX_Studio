
// LNX_MyListView

MVC_ProgramChangeMoog : MVC_View {

	var scrollView;

	initView{
		colors=colors++(
			'background'	: Color(0.5,0.5,1)/5,
			'on'			: Color.blue+0.2
		);
		canFocus=false;
		font=Font("Helvetica",9);
	}

	createView{

		view=UserView(window,Rect(rect.left,rect.top,((controlSpec.maxval.asInt+1))*20,1*18))
			.drawFunc_{|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);
					colors[\background].set;
					Pen.fillRect(Rect(0,0,16*20,1*18));
					Pen.font_(font);
					Pen.smoothing_(false);
					Pen.strokeColor_(Color.black);
					(controlSpec.maxval.asInt+1).do{|x|
						1.do{|y|
							if (x==value) {
								(colors[\on]+0.2/10).set;
								Pen.fillRect(Rect(x*20+1,y*18+1,20-1,18-1));
								(colors[\on]+0.2).set;
								Pen.fillRect(Rect(x*20+1,y*18+1,20-2,18-2));
								(colors[\on]/1.5+0.2).set;
								Pen.fillRect(Rect(x*20+2,y*18+2,20-3,18-3));
								Pen.strokeColor_(Color.black);
							}{
								Pen.strokeRect(Rect(x*20,y*18,20,18));

							};
						}
					};
					Pen.fillColor_(colors[\on]+0.2);
					Pen.smoothing_(true);
					(controlSpec.maxval.asInt+1).do{|x|
						1.do{|y|
							if ((y*16+x)==value) {
								Pen.fillColor_(Color.white);
								Pen.stringCenteredIn((x+1).asString,Rect(x*20,y*18,20,18));

							}{
								Pen.fillColor_(colors[\on]+0.2);
								Pen.stringCenteredIn((x+1).asString,Rect(x*20,y*18,20,18));
							};
						}
					};
				}; // end.pen
			};

	}

	addControls{
		var val, val2;

		view.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			var val;
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (modifiers.asBinaryDigits[4]==0) {  // if apple not pressed because of drag
				if (modifiers.isAlt) { buttonNumber=1 };
				if (modifiers.isCtrl) { buttonNumber=2 };
				if (editMode) {
					lw=lh=nil;
					startX=x;
					startY=y;
					view.bounds.postln;
				}{
					if (buttonNumber==2) {
						this.toggleMIDIactive
					}{
						val=((x/20).asInt.clip(0,controlSpec.maxval.asInt));
						// if (val!=value) { // no checking for inequality
						this.viewDoValueAction_(val,nil,true,false);
					}
				}
			}
		};
		view.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			if (modifiers.asBinaryDigits[4]==0) {  // if apple not pressed because of drag
				if (editMode) { this.moveBy(x-startX,y-startY) };
			}
		};
	}

	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		if (view.notClosed) {
			"midi learn?".postln;
		};
		labelGUI.do(_.refresh);
	}

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		if (view.notClosed) {
			//view.background_(colors[enabled.if(\background,\backgroundDisabled)]);
		};
		labelGUI.do(_.refresh);
	}

	// normally called from model
	value_{|val|
		val=val.round(1).asInt.clip(0,127);
		if (value!=val) {
			value=val;
			this.refresh;
			this.showValue;
		};
	}

	showValue {}

}
