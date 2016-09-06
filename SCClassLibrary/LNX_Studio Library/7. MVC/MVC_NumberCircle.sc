
// a round number box

MVC_NumberCircle : MVC_View {

	var startVal, <>resoultion=1,  <align='center', <>moveRound=nil;

	var down=false;

	var typedString;

	initView{
		colors=colors++(
			'focus'					: Color.clear,
			'backgroundDisabled'	: Color(1,1,1,0.3),
			'string'				: Color.white,
			'down'					: Color.orange,
			'background'			: Color(0.1,0.1,0.1,0.67),
			'backgroundDown'		: Color(0.1,0.1,0.1,0.85),
			'circleFocus'			: Color(1,1,1,0.66),
			'circleFocusDown'		: Color(0,0,0,0.5)
		);
		canFocus=true;
		showNumberBox=false;
		font=Font("Helvetica",10);
	}

	// make it
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					if (enabled) {
						colors[midiLearn.if(\midiLearn,
							down.if(\backgroundDown,\background))].set;
					}{
						colors[midiLearn.if(\midiLearn,\backgroundDisabled)].set;
					};
					Pen.fillOval(Rect(0,0,w,h));
					if (me.hasFocus.not) {typedString=nil};
					colors[(me.hasFocus.not).if(\circleFocusDown,\circleFocus)].set;
					Pen.width_(2);
					Pen.strokeOval(Rect(0,0,w,h).insetBy(1,1));
					Pen.font_(font);

					if (enabled) {
						Pen.fillColor_(colors[down.if(\down,\string)]);
					}{
						Pen.fillColor_(colors[down.if(\down,\string)].alpha_(0.3));
					};

					if (typedString.isNil) {
						if (numberFunc.notNil) {
							Pen.stringCenteredIn (
								numberFunc.value(value).asString,Rect(0,0,w,h));
						}{
							Pen.stringCenteredIn (value.asString,Rect(0,0,w,h));
						};
					}{
						Pen.fillColor_(colors[\down]);
						Pen.stringCenteredIn(typedString,Rect(0,0,w,h));
					};
				}; // end.pen
			};
	}

	// control it
	addControls{
		var val, val2;
		view.action_{|me|
			val=val2=me.value;
			if (controlSpec.notNil) { val2=controlSpec.constrain(val2) };
			this.viewValueAction_(val2,nil,true,false);
			if (val2!=val) { me.value_(val2) };
		}
		.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			MVC_LazyRefresh.mouseDown;
			if (modifiers.isAlt)  {buttonNumber = 1 };
			if (modifiers.isCtrl) {buttonNumber = 2 };
			buttonPressed=buttonNumber; // store for move
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			if (modifiers.asBinaryDigits[4]==0) { // check apple not pressed because of drag
				startX=x;
				startY=y;
				if (editMode||viewEditMode) {lw=lh=nil; if (verbose) {view.bounds.postln} };

				if (buttonNumber==2)	{
					this.toggleMIDIactive
				}{
					down=true;
					view.refresh;
				};

				if (buttonNumber==0) {
					this.valueActions(\action1Down,this, x, y, modifiers);
					if (model.notNil) { model.valueActions(\action1Down,this, x, y, modifiers) };
				};
				if (buttonNumber==1) {
					this.valueActions(\action2Down,this, x, y, modifiers);
					if (model.notNil) { model.valueActions(\action2Down,this, x, y, modifiers) };
				};

				//startVal=value;
				if (controlSpec.notNil) {
					startVal=controlSpec.unmap(value);
				}{
					startVal=value;
				};
			}
		}
		.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			var val,spenRange;
			typedString=nil;
			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}{
				val=(startVal+((startY-y)/200/resoultion/(buttonPressed*6+1))).clip(0,1);
				if (controlSpec.notNil) { val=controlSpec.map(val) };
				if (moveRound.notNil) { val=val.round(moveRound) };
				if (val!=value) {
					this.viewValueAction_(val,nil,true,false);
				};
			}
		}
		.mouseUpAction_{|me|
			MVC_LazyRefresh.mouseUp;
			down=false;
			view.refresh;
		}
		// @TODO: new Qt "key" codes
		.keyDownAction_{|me,char,mod,uni,keycode, key|
			var val;

			if ((keycode==126)or:{keycode==124}) {
				val=value;
				if (controlSpec.notNil) {
					val=val+controlSpec.step;
					val=controlSpec.constrain(val);
				}{
					val=val+1
				};
				this.viewValueAction_(val,nil,true,false)
			};

			if ((keycode==125)or:{keycode==123}) {
				val=value;
				if (controlSpec.notNil) {
					val=val-controlSpec.step;
					val=controlSpec.constrain(val);
				}{
					val=val-1
				};
				this.viewValueAction_(val,nil,true,false)
			};

			if (typedString.isNil) {typedString=""};


			if ((char.isAlphaNum)||(char.isPunct)) { typedString=typedString++char};

			if (keycode==36) {
				val=typedString.asFloat;
				typedString=nil;
				if (controlSpec.notNil) { val=controlSpec.constrain(val) };
				if (val!=value) {
					this.viewValueAction_(val,nil,true,false);
				}{
					view.refresh;
				};

			}{
				if (keycode==51) { typedString=typedString.drop(-1) };
				if ( typedString.size==0) { typedString=nil };
				view.refresh;
			};

		};
	}

	// access via themeMethods
	action1Down_{|func| this.actions_(\action1Down,func)}
	action2Down_{|func| this.actions_(\action2Down,func)}

	// make just units (has problem, updating model doesn't refresh units)
	postCreate{
		if ((view.notClosed)
			and: {numberFunc.isNil} // no number func
			and: {showNumberBox} 	 // but do show units
			and: {controlSpec.notNil}
			and: {numberGUI.isNil}
			) {
				if (colors['numberUp'  ].isNil) {colors['numberUp'  ]=Color.white};
				if (colors['numberDown'].isNil) {colors['numberDown']=Color.black};

				numberFont = numberFont ?? {Font("Helvetica",12)};
				numberBounds=this.getNumberRect("UNIT");
				numberGUI=UserView.new(window,numberBounds)
					.drawFunc_{|me|
						var active,col;
						if (verbose) { [this.class.asString, 'numberUnits' , label].postln };
						//Color.black.alpha_(0.1).set; // to remove
						//Pen.fillRect(Rect(0,0,numberBounds.width,h)); // to remove
						active= (midiLearn or: (enabled.not)).not;
						if (active.not) { col= midiLearn.if(\midiLearn,\disabled) };
						Pen.smoothing_(true);
						numberString=controlSpec.units;
						Pen.font_(numberFont);
						Pen.fillColor_(colors[active.if(\numberUp,col)]);
						if (orientation==\horizontal) {
							Pen.stringLeftJustIn(numberString,
								Rect(0,0,numberBounds.width,numberBounds.height));
						}{
							Pen.stringCenteredIn(numberString,
								Rect(0,0,numberBounds.width,numberBounds.height));
						};
					}
					.canFocus_(false)
					.onClose_{ numberGUI=nil };
		};
	}

}


/*
(
w=MVC_Window.new;
m=[0,[0,128,\lin,1],a.midiControl,12414,"a"].asModel;
b=MVC_NumberCircle(w,m,Rect(50,20,22,22));
w.front;
)
(
w=MVC_Window.new;
m=[\db,a.midiControl,12414,"a"].asModel;
b=MVC_NumberCircle(w,m,Rect(50,20,22,22)).numberFunc_(\round);
w.front;
)
*/
