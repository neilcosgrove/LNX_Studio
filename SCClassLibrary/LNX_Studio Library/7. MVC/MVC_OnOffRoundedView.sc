
/////////// MVC_OnOffView /////////////////////////////////////////////////////////////////

MVC_OnOffRoundedView : MVC_View {

	var <mode = nil;  // nil = normal, 'play' = play button, 'stop' = stop button, 'icon' < strings
	var <>mouseMode = 'switch'; // nil = default 'switch', 'button', 'tap'
	var <down=false;
	var <>permanentStrings; // used primarily in studio_inst onOff button to stop its string changing
	var <>rounded=true;
	var <>insetBy=0;

	// set your default colours
	initView{
		colors=colors++(
			'background'		: Color.black,
			'on'				: Color.ndcOnOffON,
			'off'				: Color.ndcOnOffOFF,
			'onDisabled'		: Color.ndcOnOffONUen,
			'offDisabled'		: Color.ndcOnOffOFFUen,
			'string'			: Color.ndcOnOffText,
			'stringDisabled'	: Color.ndcOnOffTextUnen
		);
	}

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					var col;

					if (midiLearn) {
						col=colors[\midiLearn];
					}{
						if (enabled) {
							if ((mode==='play')or:{mode==='stop'}) {
								if (value>=0.5) { col=(colors[\on]/3) }{ col=colors[\off] }
							}{
								col=colors[(value>=0.5).if(\on,\off)];
							};
						}{
							col=colors[(value>=0.5).if(\onDisabled,\offDisabled)];
						};
					};

					if (rounded) {
						colors[\background].set;
						Pen.roundedRect( Rect(1,1,w- 2,h- 2),5 );
						if (down) {
							(col*0.66).penFill( Rect(1,1,w- 2,h- 2) );
						}{
							col.penFill( Rect(1,1,w- 2,h- 2) );
						};
						if (down) {
							Pen.width_(1.5);
							Pen.smoothing_(true);
							Color(0,0,0,0.4).set;
							Pen.roundedRect( Rect(2,2,w-3,h-3),5 );
							Pen.stroke;

							Color(1,1,1,0.2).set;
							Pen.roundedRect( Rect(1,1,w-3,h-3),5 );
							Pen.stroke;
						}{
							Pen.width_(1.5);
							Pen.smoothing_(true);
							Color(1,1,1,0.4).set;
							Pen.roundedRect( Rect(2,2,w-3,h-3),5 );
							Pen.stroke;
						};
						Pen.width_(1.5);
						Pen.smoothing_(true);
						colors[\background].set;
						Pen.roundedRect( Rect(1,1,w-2,h-2),5 );
						Pen.stroke;
					}{
						// flat
						Pen.smoothing_(false);
						colors[\background].set;
						Pen.fillRect(Rect(0,0,w,h));
						col.set;
						Pen.fillRect(Rect(1,1,w- 2,h- 2));
					};

					case
						{mode===nil}{
							Pen.font_(font);
							Pen.fillColor_(colors[enabled.if(\string,\stringDisabled)]);
							Pen.smoothing_(true);
							if (permanentStrings.isNil) {
								Pen.stringCenteredIn(((

									(strings.size!=0).if(strings,["Off","On"])

									)|@|(
										value.round.asInt)).asString,
									Rect(0,1,w,h));
							}{
								Pen.stringCenteredIn((permanentStrings|@|(value.round.asInt))
									.asString,Rect(0,1,w,h));
							};
						}
						{mode==='play'}{
							Color.black.set;
							Pen.fillRect(Rect(2,2,w-4,h-4));
							if (midiLearn) {
								colors[\midiLearn].set;
							}{
								if (enabled) {
									colors[(value>=0.5).if(\on,\off)].set;
								}{
									colors[(value>=0.5).if(\onDisabled,\offDisabled)].set;
								};
							};
							Pen.smoothing_(true);
							Pen.moveTo(3@3);
							Pen.lineTo((w-3)@(h/2));
							Pen.lineTo(3@(h-3));
							Pen.fill;
						}
						{mode==='stop'}{
							Color.black.set;
							Pen.fillRect(Rect(2,2,w-4,h-4));
							if (midiLearn) {
								colors[\midiLearn].set;
							}{
								if (enabled) {
									colors[(value>=0.5).if(\on,\off)].set;
								}{
									colors[(value>=0.5).if(\onDisabled,\offDisabled)].set;
								};
							};
							Pen.smoothing_(true);
							Pen.moveTo(5@5);
							Pen.lineTo((w-5)@(5));
							Pen.lineTo((w-5)@(h-5));
							Pen.lineTo(5@(h-5));
							Pen.fill;
						}
						{mode==='icon'}{
							Pen.font_(font);
							if (enabled) {
								Pen.fillColor_(colors[\string]);
							}{
								Pen.fillColor_(colors[\stringDisabled]);
							};
							Pen.fillColor_(Color.black);
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

	addControls{

		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (modifiers.isAlt ) { buttonNumber=1 };
			if (modifiers.isCtrl) { buttonNumber=2 };
			buttonPressed=buttonNumber;
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			if (modifiers.asBinaryDigits[4]==0) { // check apple not pressed because of drag
				if (editMode||viewEditMode) {
					lw=lh=nil;
					startX=x;
					startY=y;
					if (verbose) {view.bounds.postln};
				}{
					evaluateAction=true;
					if (buttonNumber==2) {
						this.toggleMIDIactive
					}{
						case {mouseMode==\button} {
							value=1;
							view.refresh;
						} {mouseMode==\tap} {
							this.viewDoValueAction_(1,nil,true,false);
						} {mouseMode==\switch} {
							down=true;
							this.refresh;
						};
					};
				};
			}
		};

		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			var inBounds=(xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1};

			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}{
				if ((mouseMode==\button)and:{evaluateAction}) {
					if (inBounds) {
						if (value==0) {
							value=1;
							view.refresh;
						}
					}{
						if (value==1) {
							value=0;
							view.refresh;
						}
					}
				};
				if (mouseMode==\switch) {
					if (	inBounds && (down.not) ) { down=true; this.refresh};
					if (	(inBounds.not) && (down) ) { down=false; this.refresh};
				};
			}
		};

		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if (editMode.not) {
				if (mouseMode==\tap) {
					value=0;
					view.refresh;
				}{
					if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}
									and:{evaluateAction}and:{editMode.not}) {
						if (mouseMode==\button) {
							value=0;
							this.viewDoValueAction_(0,nil,true,false);
						}{
							if (buttonPressed==0) {
								this.viewValueAction_((value>=0.5).if(0,1),nil,true,false);
							}{
								if (buttonPressed==1) {
									this.viewValueAction2_(
										(value>=0.5).if(0,1),nil,true,false);
								}
							};
						}
					};
					if (down) {
						down=false;
						this.refresh;
					};

				}
			}
		};

	}

	mode_{|symbol|
		mode=symbol;
		this.refresh
	}

}
