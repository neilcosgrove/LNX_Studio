
// LNX_NoteDisplayView

MVC_NoteView : MVC_View {

	var startValue;

	// set your defaults
	initView{
		canFocus=true;
		showNumberBox=false;
		font=Font("Helvetica-Bold",10);
		colors=colors++(
			'background'	: Color.grey/3,
			'string'		: Color.white,
			'border'		: Color.black
		);
	}

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);
					if (midiLearn) { colors[\midiLearn].set } {  colors[\background].set };
					Pen.fillRect(Rect(0,0,w,h));
					colors[\border].set;
					Pen.strokeRect(Rect(0,1,w-1,h-1));
					Pen.font_(font);
					Pen.smoothing_(true);
					Pen.fillColor_(colors[\string]);
					Pen.stringCenteredIn(if(value>=0,value.asNote,"---"),Rect(0,0,w,h));
				}; // end.pen
			};
	}

	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			MVC_LazyRefresh.mouseDown;
			if (modifiers==524576) { buttonNumber=1 };
			if (modifiers==262401) { buttonNumber=2 };
			buttonPressed=buttonNumber;
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			if (editMode||viewEditMode) {
				lw=lh=nil; startX=x; startY=y;
				if (verbose) {view.bounds.postln}; // for moving
			}{
				startY=y;	startValue=value;
			};
			evaluateAction=true;
			if (buttonNumber==2) { this.toggleMIDIactive };
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h, val;
			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}{
				val=((startValue+((startY-y)/5)).asInt).clip(-1,127);

				if (controlSpec.notNil) { val= controlSpec.constrain(val) };

				if (val!=value) {
					this.viewValueAction_(val,nil,true,false);
				};
			};
		};
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			MVC_LazyRefresh.mouseUp;
			this.valueActions(\upAction,this);
			if (model.notNil) {model.valueActions(\upAction,model)};		};

		// @TODO: new QT "key" codes
		view.keyDownAction={|me, char, modifiers, unicode, keycode, key|
			//[modifiers, unicode, keycode].postln;
			var val;
			// up
			if (keycode==126) {
				val=value+1;
				if (controlSpec.notNil) { val= controlSpec.constrain(val) };
				this.viewValueAction_(val.clip(-1,127),nil,true,false);
				this.valueActions(\keyAction,this);
				if (model.notNil) {model.valueActions(\keyAction,model)};
			};

			// down
			if (keycode==125) {
				val=value-1;
				if (controlSpec.notNil) { val= controlSpec.constrain(val) };
				this.viewValueAction_(val.clip(-1,127),nil,true,false);
				this.valueActions(\keyAction,this);
				if (model.notNil) {model.valueActions(\keyAction,model)};
			};


		};

	}

}
