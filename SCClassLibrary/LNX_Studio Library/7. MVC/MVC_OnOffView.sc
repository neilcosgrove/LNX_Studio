
/////////// MVC_OnOffView /////////////////////////////////////////////////////////////////

/*
(
w=MVC_Window();
MVC_OnOffView(\switch.asModel,w,Rect(10, 10, 19, 19))
	.resize_(3)
	.color_(\border,Color.black)
	.color_(\on,Color.red/2)
	.color_(\off,Color(46/77,46/79,72/145)/1.5)
	.color_(\icon,Color.red)
	.color_(\iconOff,Color.green)
	.mode_(\icon)
	.strings_([\record]);
w.create;
)
(
w=MVC_Window();
MVC_OnOffFlatView(\switch.asModel,w,Rect(10, 10, 19, 19))
	.insetBy_(1)
	.color_(\border,Color.black)
	.color_(\on,Color.green)
	.color_(\off,Color(46/77,46/79,72/145)/1.5)
	.color_(\icon,Color.green)
	.color_(\iconOff,Color.grey)
	.mode_(\icon)
	.strings_([\play]);
w.create;
)

*/

MVC_OnOffFlatView : MVC_OnOffView {

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };

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
							if (value==1) {
								Pen.fillColor_(colors[\on]/1.5)
							}{
								Pen.fillColor_(colors[\off]/1.5)
							};
						}{
							if (value==1) {
								Pen.fillColor_(colors[\on])
							}{
								Pen.fillColor_(colors[\off])
							};
						};

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

						if (value==1) {
							Pen.fillColor_(colors[\on]*flashState)
						}{
							Pen.fillColor_(colors[\off]*flashState)
						};

					};

					case
						{mode==nil}{
							Pen.smoothing_(true);
							if (down) {
								Pen.font_(font.copy.size_(font.size-1));
							}{
								Pen.font_(font);
							};

							if (value==1) {
								Pen.stringCenteredIn(strings@@1,
									Rect(down.if(1,0),down.if(2,1),w,h));
							}{
								Pen.stringCenteredIn(strings[0],
									Rect(down.if(1,0),down.if(2,1),w,h));
							};
						}{
							Pen.smoothing_(true);
							if (value>=0.5) {
							DrawIcon.symbolArgs((permanentStrings?strings)|@|1,
								Rect(down.if(1,0),down.if(1,0),w-1,h).insetBy(insetBy));
							}{
							DrawIcon.symbolArgs((permanentStrings?strings)|@|0,
								Rect(down.if(1,0),down.if(1,0),w-1,h).insetBy(insetBy));
							};
						}
				}; // end.pen

		}
	}


}

MVC_OnOffView : MVC_View {

	var <mode = nil;  // nil = normal, 'play' = play button, 'stop' = stop button, 'icon' < strings
	var <>mouseMode = 'switch'; // nil = default 'switch', 'button', 'tap', 'multi'
	var <down=false;
	var <>permanentStrings; // used primarily in studio_inst onOff button to stop its string changing
	var <>rounded=false,  <>insetBy = -2, <>hAdjust=0;
	var <>onValue = 1;
	var flashState = 1, flashTask, lastFlashTime, <>flashTime = 0.33, <>flashDuration = 1.65;
	var <>iconOffColor=true;
	var <>darkerWhenPressed=true;

	flash{
		if (flashTask.isNil) {
			flashState = 1;
			lastFlashTime = AppClock.now;
			flashTask = AppClock.sched(0,{
				var now;
				flashState = 1 - flashState;
				now =  AppClock.now;
				if ( (now-lastFlashTime) > flashDuration) {
					flashTask = nil;
					flashState = 1;
					this.refresh;
					nil
				}{
					this.refresh;
					flashTime
				}
			}).play;
		}{
			lastFlashTime = AppClock.now;
		};
	}

	// set your default colours
	initView{
		colors=colors++(
			'background'		: Color.black,
			'on'				: Color.ndcOnOffON,
			'off'				: Color.ndcOnOffOFF,
			'onDisabled'		: Color.ndcOnOffONUen,
			'offDisabled'		: Color.ndcOnOffOFFUen,
			'string'			: Color.ndcOnOffText,
			'stringDisabled'	: Color.ndcOnOffTextUnen,
			'icon'				: Color.black,
			'iconOff'			: Color.black
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

						if ((value>0.5)and:{colors[\backgroundOn].notNil}) {
							colors[\backgroundOn].set;
						}{
							colors[\background].set;
						};
						Pen.fillRect(Rect(0,0,w,h));

						(col*(flashState*0.5+0.5)).set;

						// draw border
						if (colors[\border].notNil) {
							Pen.fillRect(Rect(0,0+hAdjust,w,h-hAdjust));
							colors[\border].set;
							Pen.strokeRect(Rect(0,0,w-1,h-1));
						}{
							Pen.fillRect(Rect(1,1+hAdjust,w- 2,h- 2-hAdjust));
						};

						if (colors[\innerBorder].notNil) {
							colors[\innerBorder].set;
							Pen.strokeRect(Rect(1,2,w-3,h-3));
						};

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
						{mode==='stop'}{										Color.black.set;
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

							Pen.smoothing_(true);

							if (colors[\iconBackground].notNil) {
								Pen.fillColor_(colors[\iconBackground]);
																				if (value>=0.5) {
								DrawIcon.symbolArgs((permanentStrings?strings)|@|1,
									Rect(0,0,w,h).insetBy(insetBy-1.75));
								}{
								DrawIcon.symbolArgs((permanentStrings?strings)|@|0,
									Rect(0,0,w,h).insetBy(insetBy-1.75));
								};

							};

							if ((iconOffColor)and:{value<0.5}) {
								Pen.fillColor_(colors[\iconOff]);
							}{
								Pen.fillColor_(colors[\icon]);
							};

							if (value>=0.5) {
							DrawIcon.symbolArgs((permanentStrings?strings)|@|1,
								Rect(0,0,w,h).insetBy(insetBy));
							}{
							DrawIcon.symbolArgs((permanentStrings?strings)|@|0,
								Rect(0,0,w,h).insetBy(insetBy));
							};
						}

				}; // end.pen
			};
	}

	addControls{

		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			MVC_LazyRefresh.mouseDown;
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
							down=true;
							value=1;
							view.refresh;
						} {mouseMode==\tap} {
							down=true;
							this.viewDoValueAction_(1,nil,true,false);
						} {mouseMode==\switch} {
							down=true;
							this.refresh;
						} {mouseMode==\multi} {
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
				//if ((mouseMode==\switch) || (mouseMode==\multi)) {
					if (	inBounds && (down.not) ) { down=true; this.refresh};
					if (	(inBounds.not) && (down) ) { down=false; this.refresh};
				//};
			}
		};

		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			MVC_LazyRefresh.mouseUp;
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

							if (mouseMode==\multi) {

																				value=(value.asInt+1).wrap(0,strings.size-1);
								this.viewDoValueAction_(value,nil,true,false);
							}{

								if (buttonPressed==0) {
									this.viewValueAction_((value>=0.5).if(0,onValue),nil,true,false);
								}{
									if (buttonPressed==1) {
										this.viewValueAction2_(
											(value>=0.5).if(0,onValue),nil,true,false);
									}
								};

							};
						}
					};


				}
			};
			if (down) {
				down=false;
				this.refresh;
			};
		};

	}

	mode_{|symbol|
		mode=symbol;
		this.refresh
	}

	down_{|bool|
		down=bool;
		if (view.notClosed) {view.refresh}
	}

}
