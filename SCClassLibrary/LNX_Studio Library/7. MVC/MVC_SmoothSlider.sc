
// SmoothSlider

/* (
w=MVC_Window();
m=\db.asModel;
q=MVC_SmoothSlider(w,m,Rect(20,20,20,100));
r=MVC_SmoothSlider(w,m,Rect(50,20,20,100));
w.create;
) */

MVC_SmoothSlider : MVC_View {

	var	<thumbSize=6, <border=2, <extrude=false;
	
	var <>seqItems, lastItem, <>zeroValue;
	
	var <>direction=\vertical;
	
	var <drawNegative=true;

	var <>hilite; // used externally

	drawNegative_{|bool| drawNegative=bool; if (view.notNil) { view.drawNegative_(bool) }}

	thumbSizeAsRatio_{|ratio,min=6| this.thumbSize_( (w-min-(border*2))*ratio+min+(border*2)) }

	initView{
		colors=colors++(
			'knob'				: Color.orange,
			'backgroundDisabled'	: Color.grey/2,
			'knobDisabled'		: Color.grey*1.2,
			'focus'  				: Color.red,
			'background'			: Color(0,0,0,29/77),
			'hilite'				: Color(0,0,0,0.3),
			'border'				: Color.black,
			'knobBorder'			: Color.black,
			'numberUp'			: Color.white,
			'numberDown'			: Color.white
		);
		canFocus=true;
		if (w>h) { direction=\horizontal }{ direction=\vertical };
	}
	
	// set the bounds, account for direction
	bounds_{|argRect|
		rect=argRect;
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		if (w>h) { direction=orientation=\horizontal }{ direction=orientation=\vertical };
		if (view.notClosed) { view.bounds_(rect) };
		this.adjustLabels;
	}
	
	createView{
		view=SmoothSliderAjusted.new(window,rect)
			.background_(colors[enabled.if(\background,\backgroundDisabled)])
			.hiliteColor_(colors[enabled.if(\hilite,\backgroundDisabled)])
			.knobColor_(colors[midiLearn.if(\midiLearn,enabled.if(\knob,\knobDisabled))])
			.thumbSize_(thumbSize)
			.border_(border)
			.knobBorder_(colors[\knobBorder])
			.extrude_(extrude)
			.drawNegative_(drawNegative)
			.borderColor_(colors[midiLearn.if(\midiLearn,\border)])
			.focusColor_(colors[\focus]);
		if (controlSpec.notNil) {
			view.value_(controlSpec.unmap(value))
				.step_(controlSpec.step/(controlSpec.clipHi-(controlSpec.clipLo)))
		}{
			view.value_(value.clip(0,1));
		};
		
		{ if (view.notNil) { view.view.canFocus_(false) } }.defer(0.1); // fix
	}
	
	addControls{	
		
		var toggle;
			
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var val;			
			toggle = false;
			w = me.view.bounds.width;
			
			if (editMode||viewEditMode) { view.bounds.postln};
				
			if (modifiers.asBinaryDigits[4]==0) {  // if apple not pressed because of drag
				mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
				if (editMode||viewEditMode) {
					lw=lh=nil;
					startX=x;
					startY=y;
					if (verbose) {view.bounds.postln};
				}{
					if (hasMIDIcontrol) {
						if ((clickCount>1)&&doubleClickLearn){ toggle = true };
						if (modifiers==262401) { toggle = true  };
						if (buttonNumber>=1  ) { toggle = true  };
						if (toggle) { this.toggleMIDIactive };
					};
					
					if (toggle.not) {
						if (modifiers==524576) { buttonNumber = 1  };
						buttonPressed=buttonNumber;	
						x=x+l;
						y=y+t-1;
						evaluateAction=true;
						if (buttonPressed==1) {
							seqItems.do({|i,j|	
								if ((x>=(i.l))and:{(x<=((i.l)+(i.w)))}) {
									lastItem=j;  // draw a line !!!
								}
							});
						};
						if (direction==\vertical) {
							val=(1-((y-t-3)/(h-6))).clip(0,1);
						}{
							val=((x-l-3)/(w-6)).clip(0,1);
						};
						if (controlSpec.notNil) { val=controlSpec.map(val) };
						this.viewValueAction_(val,nil,true,false,buttonPressed);
					};
				};
			};
		};
		
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var thisItem, lastValue, size, thisValue, val;
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			
			w = me.view.bounds.width;
			
			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}{
				if (toggle.not) {
					x=x+l;
					y=y+t-1;
					if (direction==\vertical) {
						thisValue=(1-((y-t-3)/(h-6))).clip(0,1);
					}{
						thisValue=((x-l-3)/(w-6)).clip(0,1);
					};
					if (controlSpec.notNil) { thisValue=controlSpec.map(thisValue) };
					if ((buttonPressed==1)and:{seqItems.notNil}) {
						seqItems.do{|i,j|
							if ((x>=(i.l))and:{(x<=((i.l)+(i.w)))}) { thisItem=j }
						};
						if (x<seqItems[0].l) { thisItem=0 }; // catch the 1st and last
						if (x>seqItems[seqItems.size-1].l) { thisItem=seqItems.size-1 };
						if (thisItem.isNil) { thisItem=lastItem };
						if (thisItem==lastItem) {
							if (seqItems[thisItem].value!=thisValue) {
								seqItems[thisItem].viewValueAction_(
									thisValue,nil,true,false,buttonPressed)
									.refreshValue;
							};
						}{ // draw a line
							if (thisItem>lastItem) {
								lastValue=seqItems[lastItem].value;
								size=thisItem-lastItem+1;
								size.do({|i|
									val=((i/(size-1))*thisValue)+(1-(i/(size-1))*lastValue);
									if (seqItems[i+lastItem].value!=val) {
										seqItems[i+lastItem].viewValueAction_(
											val,nil,true,false,buttonPressed)
											.refreshValue;
									};
								});
							}{
								lastValue=seqItems[lastItem].value;
								size=lastItem-thisItem+1;
								size.do({|i|
									val=((i/(size-1))*lastValue)+(1-(i/(size-1))*thisValue);
									if (seqItems[i+thisItem]!=val) {
										seqItems[i+thisItem].viewValueAction_(
											val,nil,true,false,buttonPressed)
											.refreshValue;
									};
								});
							};
						};
						lastItem=thisItem;
					}{	
						if (buttonPressed!=2) {
							if (thisValue!=value) {this.viewValueAction_(
									thisValue,nil,true,false,buttonPressed)};
							this.refreshValue;
						};
					};
				};
			};
		};

	}
	
	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		if (view.notClosed) {
			view.knobColor_(colors[midiLearn.if(\midiLearn,enabled.if(\knob,\knobDisabled))])
				.borderColor_(colors[midiLearn.if(\midiLearn,\border)]);
		};
		labelGUI.do(_.refresh);
		this.refresh;
	}

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		if (view.notClosed) {
			view.knobColor_(colors[enabled.if(\knob,\knobDisabled)])
				.borderColor_(colors[\border]);
		};
		labelGUI.do(_.refresh);
		this.refresh;
	}
		
	// item is enabled	
	enabled_{|bool|
		if (enabled!=bool) {
			enabled=bool;
			{
				if (view.notClosed) {
					view.knobColor_(colors[
						midiLearn.if(\midiLearn,enabled.if(\knob,\knobDisabled))]);
					view.background_(colors[enabled.if(\background,\backgroundDisabled)]);
					this.refresh;
				}
			}.defer;
		}
	}

	// assign a ControlSpec to map to
	controlSpec_{|spec|
		if (spec.notNil) {
			controlSpec=spec.asSpec;
			value=controlSpec.constrain(value);
			if (view.notClosed) {
				view.step_(controlSpec.step/(controlSpec.clipHi-(controlSpec.clipLo)));
				this.refreshValue;
				this.refresh;
			};
		}{
			controlSpec=nil;
			if (view.notClosed) {view.step_(0)};
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
	
	// change the thumb size of the knob
	thumbSize_{|size|
		thumbSize=size;
		if (view.notClosed) { view.thumbSize_(thumbSize) }
	}
	
	// change the border size of the slider
	border_{|size|
		border=size;
		if (view.notClosed) { view.border_(border) }
	}
	
	extrude_{|bool|
		extrude=bool;
		if (view.notClosed) { view.extrude_(extrude) }
	}
	
	// set the colour in the Dictionary
	// need to do disable here
	color_{|key,color|
		if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		if (key=='focus'      ) { {if (view.notClosed) { view.focusColor_(
				colors[\focus]) } }.defer };
		if (key=='knob'       ) { {if (view.notClosed) { view.knobColor_ (
				colors[midiLearn.if(\midiLearn,enabled.if(\knob,\knobDisabled))]) } }.defer };
		if (key=='hilite'	 ) { {if (view.notClosed) { view.hiliteColor_(
				colors[enabled.if(\hilite,\backgroundDisabled)]) } }.defer };
		if (key=='background' ) { {if (view.notClosed) { view.background_(
				colors[enabled.if(\background,\backgroundDisabled)]) } }.defer };
		if (key=='border'     ) { {if (view.notClosed) { view.borderColor_(
				colors[midiLearn.if(\midiLearn,\border)]) } }.defer };
		if (key=='label') { {labelGUI.do(_.refresh)}.defer };
	
		
		if (key=='knobBorder') { {if (view.notClosed) {
				view.knobBorder_(colors[\knobBorder])
		} }.defer };
		
		if (key=='numberUp') { { if (numberGUI.notClosed) { numberGUI.refresh } }.defer; };
		
	}
	
	// add a dict of colours, useful for colour themes
	colors_{|dict|
		dict.pairsDo{|key,color|
			if (colors.includesKey(key)) {
				colors[key]=color;
			}
		};
		this.refreshColors;
		labelGUI.do(_.refresh);
	}
	
	refreshColors{
		if (this.notClosed) {
			view.focusColor_ (colors[\focus])
				.knobColor_  (colors[midiLearn.if(\midiLearn,enabled.if(\knob,\knobDisabled))])
				.hiliteColor_(colors[enabled.if(\hilite,\backgroundDisabled)])
				.background_ (colors[enabled.if(\background,\backgroundDisabled)])
				.borderColor_(colors[midiLearn.if(\midiLearn,\border)]);
		}
	}
	
	// fresh the Slider Value
	refreshValue{
		
		// this some how needs to stop updating when..
		
		
		if (view.notClosed) {
			if (controlSpec.notNil) {
				if ( (parent.isKindOf(MVC_TabView))and:{parent.isHidden} ) {
					view.valueNoRefresh_(controlSpec.unmap(value))
				}{
					view.value_(controlSpec.unmap(value))
				}
			}{
				if ( (parent.isKindOf(MVC_TabView))and:{parent.isHidden} ) {
					view.valueNoRefresh_(value);
				}{
					view.value_(value);
				}
			};
		}
	}
	
	// unlike SCView there is no refresh needed with SCSlider
	// this is not true, valueAction_ calls this and so value does need updating. Changed!
	refresh{
		if ((view.notClosed) and: { (numberGUI.notNil) }) {
			if ( (parent.isKindOf(MVC_TabView))and:{parent.isHidden} ) { ^this };
			numberGUI.refresh;
			this.refreshValue;
		}
	}

}

SmoothSliderAjusted : SmoothSlider {
	
	var <>drawNegative=true;
	
	*initClass{ RoundView.focusRingSize_(0) }
	mouseDown{}
	mouseMove{}
	mouseUp{}
	
	valueNoRefresh_{|val| value=val}
	
	bounds_ { |newBounds|
		if( newBounds.width > newBounds.height )
			{ orientation = \h }
			{orientation = \v }; 
		if( expanded ) 
			{ view.bounds = newBounds.asRect.insetBy(focusRingSize.neg,focusRingSize.neg); }
			{ view.bounds = newBounds; } ;
		}
	
	free{
	
		view.remove.free;
		
		color = value = step = hit = keystep = mode = isCentered  = centerPos =
		border  = baseWidth  = extrude  = knobBorderScale = knobSize  = hitValue =
		orientation = thumbSize = focusColor = deltaAction = allwaysPerformAction =
		outOfBoundsAction = clipMode = string = font = align = stringOrientation =
		stringAlignToKnob = shift_scale = ctrl_scale  = alt_scale = grid = expanded =
		shrinkForFocusRing = enabled = couldFocus = backgroundImage = drawFunc = pen =
		action = mouseDownAction = mouseUpAction = mouseOverAction = mouseMoveAction =
		keyDownAction = keyUpAction = keyModifiersChangedAction = beginDragAction =
		canReceiveDragHandler = receiveDragHandler = onClose = focusGainedAction =
		focusLostAction = view = nil;	
		
	}
	
	draw {
		var startAngle, arcAngle, size, widthDiv2, aw;
		var knobPosition, realKnobSize;
		var rect, drawBounds, radius;
		var baseRect, knobRect;
		var center, strOri;
		
		var bnds; // used with string
		
		Pen.use {
			
			rect = this.drawBounds;
				
			drawBounds = rect.insetBy( border, border );
			
			if( orientation == \h )
				{  drawBounds = Rect( drawBounds.top, drawBounds.left, 
					drawBounds.height, drawBounds.width );
					
				   // baseRect = drawBounds.insetBy( (1-baseWidth) * (drawBounds.width/2), 0 );
				   
				   Pen.rotate( 0.5pi, (rect.left + rect.right) / 2, 
				   					 rect.left + (rect.width / 2)  );
				};
			
			baseRect = drawBounds.insetBy( (1-baseWidth) * (drawBounds.width/2), 0 );
			
			size = drawBounds.width;
			widthDiv2 = drawBounds.width * 0.5;
					
			realKnobSize = (knobSize * drawBounds.width)
					.max( thumbSize ).min( drawBounds.height );
			radius = (knobSize * drawBounds.width) / 2;
			knobPosition = drawBounds.top + ( realKnobSize / 2 )
						+ ( (drawBounds.height - realKnobSize) * (1- value).max(0).min(1));
			
			if( this.hasFocus ) // rounded focus rect
				{
				Pen.use({
					Pen.color = focusColor ?? { Color.gray(0.2).alpha_(0.8) };
					Pen.width = 2;
					Pen.roundedRect( baseRect.insetBy(-2 - border,-2 - border), 
						(radius.min( baseRect.width/2) + 1) + border );
					Pen.stroke;
					});
				};
				
			Pen.use{	
			color[0] !? { // base / background
				//Pen.fillColor = color[0];
				Pen.roundedRect( baseRect, radius.min( baseRect.width/2) );
				color[0].penFill( baseRect );
				};
			
			if( backgroundImage.notNil )
				{ 
				Pen.roundedRect( baseRect, radius.min( baseRect.width/2) );
				backgroundImage[0].penFill( baseRect, *backgroundImage[1..] );
				}
			};
			
			Pen.use{
			color[2] !? { // // border
				if( border > 0 )
					{ 
					
					  if( color[2].notNil && { color[2] != Color.clear } )
					  	{
						  // this is the outside border
						  
						   Pen.strokeColor = color[2];

						  
						  Pen.width = border;
						  Pen.roundedRect( baseRect.insetBy( border/(-2), border/(-2) ), 
						  	radius.min( baseRect.width/2) + (border/2) ).stroke;
						  };
					  if( extrude )
					  	{ 
					  	Pen.use{	
						  	  Pen.rotate( (h: -0.5pi, v: 0 )[ orientation ],
					   				(rect.left + rect.right) / 2, 
					   				rect.left  + (rect.width / 2)  );
					   		
						  	  Pen.extrudedRect( 
						  	  	baseRect.rotate((h: 0.5pi, v: 0 )[ orientation ],
					   				(rect.left + rect.right) / 2, 
					   				rect.left  + (rect.width / 2))
					   					.insetBy( border.neg, border.neg ), 
						  		(if( radius == 0 ) 
						  			{ radius } { radius + border }).min( baseRect.width/2 ),
						  		border, 
						  		inverse: true )
						  	}
					  	};
					};
				};
				};
			
			
			if ((drawNegative) or: {(drawNegative.not)and:{value>0}}) {
			
			
				Pen.use{	
				
				color[1] !? { 
					//color[1].set; // hilight
					if( isCentered )
					{
					Pen.roundedRect( Rect.fromPoints( 
							baseRect.left@
								((knobPosition - (realKnobSize / 2))
									.min( baseRect.bottom.blend( baseRect.top, centerPos ) ) ),
							baseRect.right@
								((knobPosition + (realKnobSize / 2))
									.max( baseRect.bottom.blend( baseRect.top, centerPos ) ) ))
							
						, radius ); //.fill;
					color[1].penFill( baseRect );
					}
					{
					Pen.roundedRect( Rect.fromPoints( 
							baseRect.left@(knobPosition - (realKnobSize / 2)),
							baseRect.right@baseRect.bottom ), radius.min( baseRect.width/2) );
					
					color[1].penFill( baseRect );
					};
					};
					
					};
					
				Pen.use{
	
				color[3] !? {	 
					knobRect =  Rect.fromPoints(
						Point( drawBounds.left, 
							( knobPosition - (realKnobSize / 2) ) ),
						Point( drawBounds.right, knobPosition + (realKnobSize / 2) ) );
	
	
					if (color[2].notNil) {
	
		//				Pen.roundedRect( knobRect, radius );//.fill; 
	//					
	//					color[2].penFill( knobRect ); // requires extGradient-fill.sc methods
	//					
	//					
	
				
	
						//Pen.strokeColor = color[2];
				
				
						// this is the inside border
										  
						Pen.strokeColor = knobBorder;
						 
						 
					  	Pen.width = 2;
						Pen.roundedRect( knobRect.insetBy(0,-1), radius ).stroke;
						
						
						Pen.roundedRect( knobRect.insetBy(0,0), radius );//.fill; 
						
						color[3].penFill( knobRect ); // requires extGradient-fill.sc methods
						
						
						
						
						
						
					}{
						Pen.roundedRect( knobRect, radius );//.fill; 
						
						color[3].penFill( knobRect ); // requires extGradient-fill.sc methods
					};
					
					 if( extrude && ( knobRect.height >= border ) )
						  	{ 
						  	Pen.use{	
							  	  Pen.rotate( (h: -0.5pi, v: 0 )[ orientation ],
						   				(rect.left + rect.right) / 2, 
						   				rect.left  + (rect.width / 2)  );
						   		
							  	  Pen.extrudedRect( 
							  	  	knobRect.rotate((h: 0.5pi, v: 0 )[ orientation ],
						   				(rect.left + rect.right) / 2, 
						   				rect.left  + (rect.width / 2)), 
							  		radius.max( border ), border * knobBorderScale)
							  	}
						  	};
					};
					
					};
					
			};
					
			
			string !? {
				
				
				if( stringAlignToKnob )
					{ bnds = knobRect ?? { Rect.fromPoints(
						Point( drawBounds.left, 
							( knobPosition - (realKnobSize / 2) ) ),
						Point( drawBounds.right, knobPosition + (realKnobSize / 2) ) );  }; }
					{ bnds = drawBounds };
				
				stringOrientation = stringOrientation ? \h;
								
				Pen.use{	
					
					center = drawBounds.center;
					
					strOri = (h: 0pi, v: 0.5pi, u: -0.5pi, d: 0.5pi, up: -0.5pi, down: 0.5pi )
							[stringOrientation] ? stringOrientation;
					
					strOri = strOri + (h: -0.5pi, v: 0)[ orientation ];
					
					if( strOri != 0 )
					{ Pen.rotate( strOri, center.x, center.y );
					 bnds = bnds.rotate( strOri.neg, center.x, center.y );
					};
						 		 
					font !? { Pen.font = font };
					Pen.color = color[4] ?? { Color.black; };
					string = string.asString;
					
					switch( align ? \center,
						\center, { Pen.stringCenteredIn( string, bnds ) },
						\middle, { Pen.stringCenteredIn( string, bnds ) },
						\left, { Pen.stringLeftJustIn( string, bnds ) },
						\right, { Pen.stringRightJustIn( string, bnds ) } );
					
					font !? { Pen.font = nil; };
					};
				};
			
			if( enabled.not )
				{
				Pen.use {
					Pen.fillColor = Color.white.alpha_(0.5);
					Pen.roundedRect( 
						baseRect.insetBy( border.neg, border.neg ), 
						radius.min( baseRect.width/2) ).fill;
					};
				};
		
			};
		}
		
}

