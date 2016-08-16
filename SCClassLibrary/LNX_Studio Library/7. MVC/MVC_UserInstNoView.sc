
// instrument and user icon view

MVC_UserInstNoView : MVC_View {

	var <selected = false, down=false;

	// set your defaults
	initView{
		colors=colors++(
			'icon'		: nil,
			'stringOn'	: Color.white,
			'stringOff'	: Color.black,
			'stringDown'  : Color.orange,
			'move'		: Color(1,1,1,0.4)
		);

		colors[\background]=nil;
	}

	selected_{|bool| selected=bool; this.refresh; }

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				var str=(value+1).asString;
				var x,y;
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);

					if (down) {
						if (colors[\move].notNil) {
							colors[\move].set;
							Pen.fillRect(Rect(0,0,w,h));
						}
					}{
						if (colors[\background].notNil) {
							colors[\background].set;
							Pen.fillRect(Rect(0,0,w,h));
						};
					};

					Pen.smoothing_(true);

					if (colors[\icon].notNil) {
						Pen.fillColor_(Color.black);
						DrawIcon( \user, Rect(-1,1,w,h).insetBy(-5.25,-5.25) );
						DrawIcon( \user, Rect(1,1,w,h).insetBy(-5.25,-5.25) );
						Pen.fillColor_(colors[\icon]*0.9);
						DrawIcon( \body, Rect(0,1,w,h).insetBy(-4,-4) );
						Pen.fillColor_(colors[\icon]*1.3+0.3);
						DrawIcon( \head, Rect(0,1,w,h).insetBy(-4,-4) );
					};

					Pen.font_(font);
					if (selected) {
						if (colors[\icon].notNil) {
							Pen.fillColor_(Color.black);
							Pen.font_(Font("Helvetica-Bold",15));
							Pen.stringCenteredIn(str,Rect(1,0,w,h));
							Pen.stringCenteredIn(str,Rect(0,0,w,h));
							Pen.fillColor_(colors[\stringOn]);
						}{
							Pen.fillColor_(colors[\stringOn])
						};
					}{
						if (colors[\icon].notNil) {
							Pen.fillColor_(Color.black);
							Pen.font_(Font("Helvetica-Bold",15));
							Pen.stringCenteredIn(str,Rect(1,0,w,h));
							Pen.stringCenteredIn(str,Rect(0,0,w,h));
							Pen.fillColor_(colors[\stringOn]);
						}{
							Pen.fillColor_(colors[\stringOff])
						};
					};

					if (down) {
						if (colors[\icon].notNil) {
							Pen.fillColor_(Color.black)
						}{
							Pen.fillColor_(colors[\stringDown])
						}
					};

					Pen.font_(Font("Helvetica",12));
					Pen.stringCenteredIn(str,Rect(1,0,w,h));

				}
			};


	}

	addControls{

		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			startX=x;
			startY=y;
			if (editMode) { lw=lh=nil; view.bounds.postln };
			if (modifiers==524576) {buttonNumber=1};
			if (modifiers==262401) {buttonNumber=2};
			buttonPressed=buttonNumber;
			if (buttonNumber==2) {
				this.toggleMIDIactive
			}{
				down=true;
				this.valueActions(\mouseDown,this, x, y, modifiers);
				this.refresh;
			}
		};

		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var val;
			if (editMode) {
				this.moveBy(x-startX,y-startY)
			}{
				if (buttonPressed!=2) {

					this.valueActions(\mouseMove,this, x, y, modifiers);

				}
			};
		};

		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			down=false;
			this.refresh;
			//if (editMode.not) {
//				if (buttonPressed!=2) {
//					this.valueActions(\mouseUp,this, x, y, modifiers)
//				}
//			}
		}
	}

	// set the colour in the Dictionary
	color_{|key,color|
		//if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		if (key=='focus') {
			{
				if (view.notClosed) { view.focusColor_(color) }
			}.defer;
		}{
			if (key=='label') {
				labelGUI.do(_.refresh);
			}{
				this.refresh;
			};

		}
	}





}
