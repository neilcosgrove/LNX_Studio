/*
(
w=MVC_Window.new;
b=MVC_ButtonLamp(w,Rect(50,20,46,18),"Off","On");
w.front;
)
*/

// a button with a lamp

MVC_ButtonLamp : MVC_View {

	var <down=false;

	// set your defaults
	initView{
		colors=colors++(
			'background'	: Color.black,
			'down'		: Color.grey+0.1/3,
			'up'			: Color.grey+0.1/1.5,
			'on'			: Color.green,
			'off'		: Color.black,
			'string'		: Color.black
		);
		font=Font("Helvetica-Bold",12);
	}

	// make the view
	createView{
		view = UserView.new(window,rect)
			.drawFunc={|me|
				var w2,h2;
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);
					colors[\background].set;
					Pen.fillRect(Rect(0,0,w,h));

					if (midiLearn) {
							colors[\midiLearn].set;
							Pen.fillRect(Rect(1,1,w- 2,h- 2));
						}{
						if (enabled) {
							if (down) { colors[\down].set; }{ colors[\up].set };
							Pen.fillRect(Rect(1,1,w- 2,h- 2));
							if (down) { (colors[\down]*1.5).set; }{ (colors[\up]/2).set };
							Pen.line(1@(h-1),(w-2)@(h-1));
							Pen.line((w-2)@(2),(w-2)@(h-1));
							Pen.fillStroke;
							if (down) { (colors[\down]/2).set; }{ (colors[\up]+0.5).set };
							Pen.line(1@2,(w-3)@2);
							Pen.line(1@2,(1)@(h-2));
							Pen.fillStroke;
						}{
							if (down) {(colors[\down]+0.5/1.7).set}{(colors[\up]+0.5/1.7).set};
							Pen.fillRect(Rect(1,1,w- 2,h- 2));
						};
					};
					// clip to a square
					w2=w.clip(0,h);
					h2=h.clip(0,w);
					Pen.smoothing_(true);
					Color.black.set;
					Pen.fillOval(Rect(4,4,w2-8,h2-8));					if (enabled) {
						(colors[(value>0).if(\on,\off)]*(value.map(0,1,0.6,1))).set;
					}{
						(colors[(value>0).if(\on,\off)]+0.5/1.7).set;
					};
					Pen.fillOval(Rect(5,5,w2-10,h2-10));
					if (strings.size>0) {
						Pen.font_(font);
						Pen.fillColor_(colors[\string]);
						Pen.stringLeftJustIn(strings[value.asInt.wrap(0,strings.size-1)],
							Rect(w2,0,w-w2,h));
					};

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
				this.viewValueAction_((value>0).if(0,1),nil,true,false);
			};
		};
	}

	down_{|bool|
		down=bool;
		if (view.notClosed) {view.refresh}
	}

}
