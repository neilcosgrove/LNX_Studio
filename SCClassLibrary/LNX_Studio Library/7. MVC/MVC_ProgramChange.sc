
// LNX_MyListView

MVC_ProgramChange : MVC_View {

	var scrollView, <actualProgram, <flash=1;

	initView{
		colors=colors++(
			'background'	: Color(0.5,0.5,1)/5,
			'on'			: Color.blue+0.2
		);
		canFocus=false;
		font=Font("Helvetica",9);
	}

	actualProgram_{|prg|
		actualProgram=prg;
		{this.refresh}.defer;
	}

	flash_{|f|
		flash=f;
		{this.refresh}.defer;
	}


	createView{
		scrollView=ScrollView.new(window, rect)
			.hasBorder_(true)
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false)
			.autoScrolls_(false);
		view=UserView(scrollView,Rect(0,0,4*20,64*18))
			.drawFunc_{|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);
					colors[\background].set;
					Pen.fillRect(Rect(0,0,4*20,64*18));
					Pen.font_(font);
					Pen.smoothing_(false);
					Pen.strokeColor_(Color.black);
					4.do{|x|
						64.do{|y|
							if ((y*4+x)==actualProgram) {
								(colors[\on]+0.2/10).set;
								Pen.fillRect(Rect(x*20+1,y*18+1,20-1,18-1));
								(colors[\on]+0.2).set;
								Pen.fillRect(Rect(x*20+1,y*18+1,20-2,18-2));
								(colors[\on]/1.5+0.2).set;
								Pen.fillRect(Rect(x*20+2,y*18+2,20-3,18-3));
								Pen.strokeColor_(Color.black);
							}{
								if ((y*4+x)==model.value) {
									(Color(1,1,1,0.2)*flash).set;
									Pen.fillRect(Rect(x*20+2,y*18+2,20-3,18-3));
									Pen.strokeColor_(Color.black);
								};
								Pen.strokeRect(Rect(x*20,y*18,20,18));


							};
						}
					};
					Pen.fillColor_(colors[\on]+0.2);
					Pen.smoothing_(true);
					4.do{|x|
						64.do{|y|
							if ((y*4+x)==actualProgram) {
								Pen.fillColor_(Color.white);
								Pen.stringCenteredIn((y*4+x).asString,Rect(x*20,y*18,20,18));

							}{
								if ((y*4+x)==model.value) {
									Pen.fillColor_(Color.white*(flash+1/2));
								}{
									Pen.fillColor_(colors[\on]+0.2);
								};

								Pen.stringCenteredIn((y*4+x).asString,Rect(x*20,y*18,20,18));
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
				if (modifiers.isAlt ) { buttonNumber=1 };
				if (modifiers.isCtrl) { buttonNumber=2 };
				if (editMode) {
					lw=lh=nil;
					startX=x;
					startY=y;
					scrollView.bounds.postln;
				}{
					if (buttonNumber==2) {
						this.toggleMIDIactive
					}{
						val=(y/18).asInt*4+((x/20).asInt.clip(0,3));
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

	// move gui, label and number also quant to grid
	moveBy{|x,y|
		var l,t,nx,ny;
		l=scrollView.bounds.left;
		t=scrollView.bounds.top;
		if (editResize) {
			// resize causes crash
		}{
			nx=(l+x).round(grid);
			ny=(t+y).round(grid);
			x=nx-l;
			y=ny-t;
			this.bounds_(Rect(nx,ny,w,h).postln);
		};
		this.adjustLabels;
	}

	// set the bounds
	bounds_{|argRect|
		rect=argRect;
		l=rect.bounds.left;
		t=rect.bounds.top;
		w=rect.bounds.width;
		h=rect.bounds.height;
		if (view.notClosed) {
			scrollView.bounds_(rect);
			//view.bounds_(Rect(0,0,w-13,this.internalHeight));
		}
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

	showValue {
		var val=(value/4).asInt;
		if (view.notClosed) {
			if ((val*18)<(scrollView.visibleOrigin.y)) {
				scrollView.visibleOrigin_(0@(val*18))
			};

			if ((val*18)>(scrollView.visibleOrigin.y+(rect.bounds.height-18))) {
				scrollView.visibleOrigin_(0@(val*18))
			};
		}
	}

}
