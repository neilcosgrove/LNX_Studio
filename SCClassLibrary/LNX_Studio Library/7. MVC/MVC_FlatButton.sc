
// LNX_BevelPushOnOffView

MVC_FlatButton : MVC_View {

	var <mode = nil;  // nil = normal, 'icon' < strings
	var <down=false, <>rounded=false, clicks;
	var <>attachedDown;
	var <>shadow=false;
	var <>insetBy=0;

	// set your defaults
	initView{
		// [ 0:onBG, 1:offBG, 2:onOffText, 3:onBGUnen, 4:offBUnen, 5:onOffTextUnen ]
		colors=colors++(
			'background'	: Color.black,
			'down'		: Color.green+0.1/3,
			'up'			: Color.green+0.1/1.5,
			'string'		: Color.black
		);
		font=Font("Helvetica",12);
	}

	// make the view
	createView{
		view = UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					var col;

					Pen.smoothing_(false);

					if (midiLearn) {
						col=colors[\midiLearn];
					}{
						if (enabled) {
							if (down) { col=colors[\down] }{ col=colors[\up]};
						}{
							col=colors[\disabled];
						};

					};


					if (rounded) {
						Pen.roundedRect( Rect(1,1,w- 2,h- 2),5 );
						if (down) {
							(col*0.66).penFill( Rect(1,1,w- 2,h- 2) );
						}{
							col.penFill( Rect(1,1,w- 2,h- 2) );
						};
						if (down) {
							Pen.width_(1.5);
							Pen.smoothing_(true);
																			Color(0,0,0,0.5).set;
							Pen.roundedRect( Rect(2,2,w-3,h-3),5 );
							Pen.stroke;

							Color(1,1,1,0.3).set;
							Pen.roundedRect( Rect(1,1,w-3,h-3),5 );
							Pen.stroke;
						}{													Pen.width_(1.5);
							Pen.smoothing_(true);
							Color(1,1,1,0.5).set;
							Pen.roundedRect( Rect(2,2,w-3,h-3),5 );
							Pen.stroke;
												//							Pen.roundedRect( Rect(3,3,w-4,h-4),5 );
//							Pen.stroke;
						};
						Pen.width_(1.5);
						Pen.smoothing_(true);
						colors[\background].set;
						Pen.roundedRect( Rect(1,1,w-2,h-2),5 );
						Pen.stroke;
//						Pen.roundedRect( Rect(1,1,w-2,h-2),5 );
//						Pen.stroke;
					}{


						colors[\background].set;
						Pen.fillRect(Rect(0,0,w,h));
						if (enabled) {
							col.set;
							Pen.fillRect(Rect(1,1,w- 2,h- 2));
							if (down) { (col*1.5).set; }{ (col/2).set };
							Pen.line(1@(h-1),(w-2)@(h-1));
							Pen.line((w-2)@(2),(w-2)@(h-1));
							Pen.fillStroke;
							if (down) { (col/2).set; }{ (col+0.5).set };
							Pen.line(1@2,(w-3)@2);
							Pen.line(1@2,(1)@(h-2));
							Pen.fillStroke;
						}{
							col.set;
							Pen.fillRect(Rect(1,1,w- 2,h- 2));
						};

					};

					case
						{mode===nil}{
							Pen.smoothing_(true);
							Pen.font_(font);

							if (shadow) {
								Pen.fillColor_(Color.black);
								Pen.stringCenteredIn(strings[value.asInt.wrap(0,strings.size-1)],
									Rect(1,1,w,h));
							};

							if (down) {
								Pen.fillColor_(colors[\string]*0.75);
							}{
								Pen.fillColor_(colors[\string]);
							};
							Pen.stringCenteredIn(strings[value.asInt.wrap(0,strings.size-1)],
								Rect(0,0,w,h));
						}
						{mode==='icon'}{
							Pen.smoothing_(true);
							Pen.font_(font);
							if (down) {
								(colors[\string]*0.5).set;
							}{
								colors[\string].set;
							};
							Pen.smoothing_(true);

							if (value>=0.5) {
								DrawIcon.symbolArgs(strings|@|1, Rect(0,0,w,h).insetBy(insetBy,insetBy));
							}{
								DrawIcon.symbolArgs(strings|@|0, Rect(0,0,w,h).insetBy(insetBy,insetBy));
							};

						}

				}; // end.pen
			};
	}

	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
		// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple


			if (modifiers==524576) { buttonNumber=1 };
			if (modifiers==262401) { buttonNumber=2 };
			buttonPressed=buttonNumber;

			clicks=clickCount;

			if (modifiers.asBinaryDigits[4]==0) { // check apple not pressed because of drag
				if (editMode) {
					lw=lh=nil; startX=x; startY=y; view.bounds.postln; // for moving
				}{
					//buttonPressed=buttonNumber;
					evaluateAction=true;
					//if (modifiers==524576) { buttonPressed=1 };
					//if (modifiers==262401) { buttonNumber=2 };
					//buttonPressed=buttonNumber;

					if (buttonNumber==2) {
						this.toggleMIDIactive
					}{
						down=true;
						view.refresh;
						if(attachedDown.notNil) {attachedDown.down_(down) };
					};
				}
			};

		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if (editMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}{
				if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}) {
					if (down.not) {
						down=true;
						view.refresh;
						if(attachedDown.notNil) {attachedDown.down_(down) };
					};
				}{
					if (down) {
						down=false;
						view.refresh;
						if(attachedDown.notNil) {attachedDown.down_(down) };
					}
				};
			};
		};
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				down=false;
				this.viewDoValueAction_((value+1).asInt.wrap(0,strings.size-1),nil,true,false);
				if (clicks==2) {
					this.valueActions(\mouseUpDoubleClickAction,this);
					if (model.notNil){ model.valueActions(\mouseUpDoubleClickAction,model) };
				};
				if(attachedDown.notNil) {attachedDown.down_(down) };
			};
		};
	}

	down_{|bool|
		down=bool;
		if (view.notClosed) {view.refresh}
	}

	mode_{|symbol|
		mode=symbol;
		this.refresh
	}

}
