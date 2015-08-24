
// LNX_MyListView

MVC_ListView : MVC_View {

	var <scrollView, <>fontHeight=18, lastEnterTime=0;

	initView{
		colors=colors++(
			'backgroundDisabled'	: Color(0.05,0.05,0.1),
			'string'				: Color(0.9,0.9,1)*0.6,
			'selectedString'		: Color(1,1,1),
			'hilite'				: Color(0.9,0.9,1)*0.5,
			'background'			: Color(0,0,0,0.5)
		);
		canFocus=true;
		items = items ? [];
	}
	
	items_{|array|
		items=array;
		if (view.notClosed) {
			view.items_(items);
			this.autoSize;
		};
	}
	
	itemsMinSize_{|array|
		items=array;
		if (view.notClosed) { view.items_(items) };
		this.minSize; // fixes bounds bug
		^this
	}
	
	createView{
		scrollView=SCScrollView.new(window, rect)
			.hasBorder_(true)
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false)
			.autohidesScrollers_(false);
		view=SCListView.new(scrollView,Rect(0,0,w,this.internalHeight))
			.background_(colors[midiLearn.if(\midiLearn,
						enabled.if(\background,\backgroundDisabled))])
			.hiliteColor_(colors[\hilite])
			.selectedStringColor_(colors[\selectedString])
			.items_(items)
			.value_(value)
			.stringColor_(colors[\string])
			.font_(font);
	}
	
		// set the colour in the Dictionary
	// need to do disable here
	color_{|key,color|
		if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		if (key=='focus'      ) { {if (view.notClosed) { view.focusColor_(color) } }.defer };		if (key=='hilite'     ) { {if (view.notClosed) { view.hiliteColor_ (color) } }.defer };
		if (key=='background' ) { {if (view.notClosed) { view.background_(color) } }.defer };
		if (key=='string' ) { {if (view.notClosed) { view.stringColor_(color) } }.defer };		if (key=='selectedString' ) { {if (view.notClosed) { 
										view.selectedStringColor_(color) } }.defer };
		
		if (key=='label') { {labelGUI.do(_.refresh)}.defer };
	}
	

	autoSize{
		if (view.notClosed) {
			view.bounds_(Rect(0,0,w,this.internalHeight));
		};
	}

	internalHeight{
		^(items.size*fontHeight).clip(h-1,h+(items.size*fontHeight));
	}
	
	minSize{
		this.bounds_(Rect(0,0,w+2,h));
	}

	showItem{	
		if ((value*fontHeight)<(scrollView.visibleOrigin.y)) {
			scrollView.visibleOrigin_(0@(value*fontHeight))
		}{
			if ((value*fontHeight)>(scrollView.visibleOrigin.y+h-fontHeight)) {
				scrollView.visibleOrigin_(0@(value*fontHeight))
			}
		};
	}
		
	canSee{
		if (view.notClosed) {
			if (items.size==0) {^true};
			if (((value*fontHeight)<(scrollView.visibleOrigin.y))
				or:{(value*fontHeight)>(scrollView.visibleOrigin.y+h-fontHeight)}){
					^false
			};
			^true;
		};
		^false
	}

	addControls{
		var val, val2;
		view.action_{|me|
			this.viewValueAction_(me.value.asInt,nil,true,false);
			this.showItem;
		};
		view.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			if (modifiers==524576) { buttonNumber=1 };
			if (modifiers==262401) { buttonNumber=2 };
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) {
				lw=lh=nil;
				startX=x;
				startY=y;
				scrollView.bounds.postln;
			};	
			if (buttonNumber==2) { this.toggleMIDIactive };
			
			if (clickCount==2) {
				this.valueActions(\doubleClickAction, this);
				if (model.notNil) { model.valueActions(\doubleClickAction,this) };
			};
			
			if (clickCount==3) {
				this.valueActions(\tripleClickAction, this);
				if (model.notNil) { model.valueActions(\tripleClickAction,this) };
			};
			
		};
		view.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode) { this.moveBy(x-startX,y-startY) };
		};
		
		view.keyDownAction_{|me, char, modifiers, unicode|
			var index;
			//[me, char, modifiers, unicode].postln;

			// delete
			if (unicode==127)  {
				this.specialActions(\deleteKeyAction, this);
				//if (model.notNil) { model.valueActions(\deleteKeyAction, this) };
			};
			
			// space & return
			if ((char == $ )||(char == $\n)||(char == $\r)) {
				if ((SystemClock.now-lastEnterTime)<0.5) {
					this.specialActions(\tripleClickAction, this);
					//if (model.notNil) { model.valueActions(\tripleClickAction, this) };
				}{
					this.specialActions(\enterKeyAction, this);
					//if (model.notNil) { model.valueActions(\enterKeyAction, this) };
				};
				lastEnterTime=SystemClock.now;
			};
			// right arrow
			if (char == 3.asAscii, {
				//this.valueAction = (this.value + 1).wrap(0,items.size-1);
				this.specialActions(\enterKeyAction, this);
				//if (model.notNil) { model.valueActions(\enterKeyAction, this) };
			});
			// left arrow
			if (unicode == 16rF702, {
				this.valueAction = (this.value.asInt - 1).wrap(0,items.size-1);
				this.specialActions(\enterKeyAction, this);
				//if (model.notNil) { model.valueActions(\enterKeyAction, this) };
			});
			if (unicode == 16rF700, { this.valueAction =  (this.value.asInt - 1).wrap(0,items.size-1) });
			if (unicode == 16rF703, { this.valueAction =  (this.value.asInt + 1).wrap(0,items.size-1) });
			if (unicode == 16rF701, { this.valueAction =  (this.value.asInt + 1).wrap(0,items.size-1) });
			if (char.isAlpha, {
				char = char.toUpper;
				index = items.detectIndex({|item| item.asString.at(0).toUpper >= char });
				if (index.notNil, {
					this.valueAction = index
				});
			});

		}
	}
	
	// move gui, label and number also quant to grid
	moveBy{|x,y|
		var l,t,nx,ny;
		l=scrollView.bounds.left;
		t=scrollView.bounds.top;
		if (editResize) {
			// resize causes fatal crash
//			if (lw.isNil) {lw=w;lh=h};
//			if (isSquare) {
//				w=(lw+x).clip(0,inf).round(grid);
//				h=(lh+y).clip(0,inf).round(grid);
//				w=w.clip(0,h).clip(30,inf);	
//				h=h.clip(0,w).clip(60,inf);
//			}{
//				w=(lw+x).round(grid).clip(30,inf);
//				h=(lh+y).round(grid).clip(60,inf);
//			};
//			this.bounds_(Rect(l,t,w,h).postln);
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
			//view.bounds_(Rect(0,0,w,this.internalHeight));
		}
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
	
	// set the font
	font_{|argFont|
		font=argFont;
		fontHeight="".bounds(font).height+3;
		if (view.notClosed) {
			view.font_(font);
			this.autoSize;
		};	
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
	
	// normally called from model
	value_{|val|
		if (items.notNil) {
			val=val.round(1).asInt.wrap(0,items.size-1);
		}{
			value=val.asInt;
		};
		if (value!=val) {
			value=val;
			this.refreshValue;
		};
	}
	
	// and action
	valueAction_{|val|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if (value!=val) {
			value=val;
			this.refreshValue;
			action.value(val,nil,true,false);
			if (model.notNil) {
				model.valueAction_(val,nil,true,false);
			}
			
		};
	}
	
	// fresh the menu value
	refreshValue{
		if (view.notClosed) {
			view.value_(value);
			this.showItem;
		}
	}
	
	// unlike SCView there is no refresh needed
	refresh{ this.refreshValue }

}
