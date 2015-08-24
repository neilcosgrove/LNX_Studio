
// add show units

// LNX_MyNumberBox

/*(
w=MVC_Window();
128.do{|i|
MVC_NumberBox(w,Rect(i%16*42+20,i.div(16)*40+20,38,18)).value_(100.rand).label_((i+1).asString);
};
w.create;
)*/

MVC_NumberBox : MVC_View {

	var startVal, <>resoultion=1,  <align='center', <>visualRound=0, <>moveRound=nil;
	
	var <>rounded=true, <showBorder=true, <postfix="", <postfixFunc, <step;

	showBorder_{|bool|
		showBorder=bool;
		if (view.notNil) {view.showBorder_(bool) }
	}

	initView{
		colors=colors++(
			'backgroundDisabled'	: Color.grey/2,
			'string'				: Color.black,
			'typing'				: Color.orange,
			'background'			: Color(1,1,0.9)
		);
		canFocus=true;
		showNumberBox=false;
	}
	
	postfix_{|string|
		postfix=string;
		if (view.notClosed) { view.postfix_(postfix) };
	}
	
	postfixFunc_{|func|
		postfixFunc=func;
		if (view.notClosed) { view.postfixFunc_(postfixFunc) };
	}
	
	// make it
	createView{
		
		if (rounded) {
			view=AdjustedRoundNumberBox(window,rect)
				.postfix_(postfix)
				.postfixFunc_(postfixFunc);
		}{
			view=AdjustedSCNumberBox(window,rect);
		};
		
		view.background_(colors[midiLearn.if(\midiLearn,
						enabled.if(\background,\backgroundDisabled))])
			.value_(value)
			.typingColor_(colors[\typing])
			.normalColor_(colors[\string])
			.stringColor_(colors[\string])
			.focusColor_(colors[\focus])
			.font_(font)
			.showBorder_(showBorder);
			
		if (rounded) {	
			view.align_(align);
		}{
			view.setProperty(\align,align);
		};
		
		if (controlSpec.notNil) {
			view.step_(
				(controlSpec.step==0).if(
					(controlSpec.maxval-(controlSpec.minval))/100,controlSpec.step?1
				)
			);
		}{
			view.step_(1);
		};
		
		if (step.notNil) { view.step_(step) }; // overrides controlSpec
		
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
			if (modifiers==524576)	{buttonNumber = 1      }; 
			if (modifiers==262401)	{buttonNumber = 2      };
			buttonPressed = buttonNumber;
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			startX=x;
			startY=y;
			if (editMode||viewEditMode) {lw=lh=nil;
				//if (verbose) {
					view.bounds.postln
				//}
			};
			if (buttonNumber==2)	{this.toggleMIDIactive; };
			if (buttonNumber==0) {
				this.valueActions(\action1Down,this, x, y, modifiers);
				if (model.notNil) { model.valueActions(\action1Down,this, x, y, modifiers) };
			};
			if (buttonNumber==1) {
				this.valueActions(\action2Down,this, x, y, modifiers);
				if (model.notNil) { model.valueActions(\action2Down,this, x, y, modifiers) };
			};
			buttonPressed=buttonNumber; // store for move
			//startVal=value;
			if (controlSpec.notNil) {
				startVal=controlSpec.unmap(value);
			}{
				startVal=value;
			};
			me.stringColor_(Color.black);
		}
		.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			var val,spenRange;
			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}{
				val=(startVal+((startY-y)/200/resoultion/(buttonPressed*6+1))).clip(0,1);
				if (controlSpec.notNil) { val=controlSpec.map(val) };
				if (moveRound.notNil) { val=val.round(moveRound) };
				if (val!=value) { 
					this.viewValueAction_(val,nil,true,false);
					me.value_(val.round(visualRound));
				};
			};
			mouseMoveAction.value(this, x, y, modifiers, buttonNumber, clickCount);
		}
		.mouseUpAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			me.stringColor_(colors[\string]);
			mouseUpAction.value(this, x, y, modifiers, buttonNumber, clickCount);
		};		
	}
	
	// access via themeMethods
	action1Down_{|func| this.actions_(\action1Down,func)}
	action2Down_{|func| this.actions_(\action2Down,func)}
	
	// set the font
	align_{|argAlign|
		align=argAlign;
		if (view.notClosed) { view.align_(align) };
	}
	
	// set the font
	font_{|argFont|
		font=argFont;
		if (view.notClosed) { view.font_(font) };
	}
	
	// overrides controlSpec step for keyboard
	step_{|value|
		step=value;
		if ((view.notClosed) && (step.notNil)) { view.step_(step) };
	}
	
	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		if (view.notClosed) {
			view.background_(colors[bool.if(\midiLearn,enabled.if(
											\background,\backgroundDisabled))]);
		};
		labelGUI.do(_.refresh);
	}

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		if (view.notClosed) {
			view.background_(colors[enabled.if(\background,\backgroundDisabled)]);
		};
		labelGUI.do(_.refresh);
	}
		
	// item is enabled	
	enabled_{|bool|
		if (enabled!=bool) {
			enabled=bool;
			{
				if (view.notClosed) {
					view.background_(colors[midiLearn.if(\midiLearn,
						enabled.if(\background,\backgroundDisabled))])
				}
			}.defer;
		}
	}
	
	// assign a ControlSpec to map to
	controlSpec_{|spec|
		if (spec.notNil) {
			controlSpec=spec.asSpec;
			value=controlSpec.constrain(value);
			this.refreshValue;
			this.refresh;
			if (view.notClosed) {
				view.step_(
					(controlSpec.step==0).if(
						(controlSpec.maxval-(controlSpec.minval))/100,controlSpec.step?1
					)
				);
			};
		}{
			controlSpec=nil;
			if (view.notClosed) {
				view.step_(1);
				this.refresh;
			};
		}
	}
	
	// normally called from model
	value_{|val|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if (value!=val) {
			value=val;
			this.refreshValue;
			this.refresh;
		};
	}
	
	// fresh the Slider Value
	refreshValue{
		if (view.notClosed) {
			view.value_(value.round(visualRound));
		}
	}
	
	// set the colour in the Dictionary
	// need to do disable here 
	color_{|key,color|
		if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		if (key=='focus'      ) { {if (view.notClosed) { view.focusColor_(color) } }.defer };		if (key=='string'     ) { {if (view.notClosed) {
			view.normalColor_(color).stringColor_(color)
		} }.defer };
		if (key=='background' ) { {if (view.notClosed) { view.background_(color) } }.defer };
		if (key=='typing' ) { {if (view.notClosed) { view.typingColor_(color) } }.defer };
	}
	
	// unlike SCView there is no refresh needed with SCSlider
	// this is not true, valueAction_ calls this and so value does need updating. Changed!
	refresh{
		if (numberGUI.notNil) {
			numberGUI.refresh;
		};
		this.refreshValue;
	}
	
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
				numberGUI=SCUserView.new(window,numberBounds)
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

// adjust so control just goes to action for MVC_View

AdjustedSCNumberBox : SCNumberBoxOld {
	
	var <>showBorder = true;

	*viewClass { ^SCNumberBoxOld }

	mouseDown{arg x, y, modifiers, buttonNumber, clickCount;
		mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
	}
	mouseUp{arg x, y, modifiers, buttonNumber, clickCount;
		mouseUpAction.value(this, x, y, modifiers, buttonNumber, clickCount);
	}
	mouseMove{arg x, y, modifiers, buttonNumber, clickCount;
		mouseMoveAction.value(this, x, y, modifiers, buttonNumber, clickCount);
	}	

}

AdjustedRoundNumberBox : RoundNumberBox {

	free{ this.remove }

	mouseDown { arg x, y, modifiers, buttonNumber, clickCount;
		if( enabled )
		{	
			hit = Point(x,y);
			startHit = hit;
			if (scroll == true, { inc = this.getScale(modifiers) });			
			// stops double press and midi active works
			// mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
		};
	}
	
	mouseMove { arg x, y, modifiers;
//		var direction;
//		var angle;
//		if( enabled ) {
//		
//		if (scroll == true, {
//			direction = 1.0;
//				// horizontal or vertical scrolling:
//			//if ( (x - hit.x) < 0 or: { (y - hit.y) > 0 }) { direction = -1.0; };
//			inc = this.getScale( modifiers );
//			angle = ((x@y) - hit).theta.wrap(-0.75pi, 1.25pi);
//			//angle = angle = ((x@y) - startHit).theta.wrap(-0.75pi, 1.25pi);
//			direction = 
//				case { angle.inclusivelyBetween( -0.6pi, 0.1pi ) }
//					{ 1.0 }
//					{ angle.inclusivelyBetween( 0.4pi, 1.1pi )  }
//					{ -1.0 }
//					{ true }
//					{ 0.0 };
//			if( value.respondsTo( '+' ) && { value.class != String }  )
//				{ this.valueAction = (this.value + (inc * this.scroll_step * direction)); };
//			hit = Point(x, y);
//		});
//		//mouseMoveAction.value(this, x, y, modifiers);
//			
//		};
		
	}


//	mouseUp{arg x, y, modifiers, buttonNumber, clickCount;
//		mouseUpAction.value(this, x, y, modifiers, buttonNumber, clickCount);
//	}
//	mouseMove{arg x, y, modifiers, buttonNumber, clickCount;
//		mouseMoveAction.value(this, x, y, modifiers, buttonNumber, clickCount);
//	}	


}
