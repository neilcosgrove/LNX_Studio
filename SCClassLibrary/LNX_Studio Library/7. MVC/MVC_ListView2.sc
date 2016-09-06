/*
(
w=MVC_Window().setInnerExtent(170,320).color_(\background, Color.grey(0.2)).create;
v=MVC_ListView2(w,Rect(10,10,150,300))
	.items_(["a","b","c"])
	.actions_(\upDoubleClickAction,{|val| val.postln; w.close})
)

v.hilite_(0,Color.red);
v.hilite_(1,Color.green);
v.hilite_(2,Color.blue);
v.hilite_(0,nil);

*/
// LNX_MyListView

MVC_ListView2 : MVC_View {

	var <scrollView, <>fontHeight=18, lastEnterTime=0;

	var <hilite;

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

		hilite = IdentityDictionary[];
	}

	hilite_{|key,value|
		hilite[key.asInt]=value;
		{
			if (view.notClosed) { view.refresh };
		}.defer;
	}

	items_{|array|
		items=array;
		if (view.notClosed) {
			view.refresh;
			this.autoSize;
		};
	}

	itemsMinSize_{|array|
		items=array;
		if (view.notClosed) { view.refresh };
		this.minSize; // fixes bounds bug
		^this
	}

	createView{
		scrollView=ScrollView(window, rect)
			.hasBorder_(true)
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false)
			.autohidesScrollers_(false)
			.resize_(5);

		view=UserView(scrollView,Rect(0,0,w,this.internalHeight));

		view.resize_(5);

		view.drawFunc_{|me|
			//var w,h;
			MVC_LazyRefresh.incRefresh;
			if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
			Pen.use{
				w=scrollView.bounds.width;

				if (me.bounds.width!= w) { {me.bounds_(me.bounds.width_(w)) }.defer  };

				h=me.bounds.height;

				if (showLabelBackground) {
					Color.black.alpha_(0.2).set;
					Pen.fillRect(Rect(0,0,w,h));
				};

				Pen.smoothing_(false);
				colors[enabled.if(\background,\backgroundDisabled)].set;
				Pen.fillRect(Rect(0,0,w,h));
				Pen.font_(font);
				Pen.smoothing_(true);

				items.do{|item,n|
					if (n==value) {
						colors[\hilite].set;
						Pen.fillRect(Rect(0,n*fontHeight,w,fontHeight));
						if (hilite[n].isKindOf(Color)) {
							Pen.fillColor_(hilite[n]);
						}{
							Pen.fillColor_(colors[\selectedString]);
						};
						Pen.stringLeftJustIn(item,Rect(0,n*fontHeight,w,fontHeight));
					}{
						if (hilite[n].isKindOf(Color)) {
							Pen.fillColor_(hilite[n]);
						}{
							Pen.fillColor_(colors[\string]);
						};
						Pen.stringLeftJustIn(item,Rect(0,n*fontHeight,w,fontHeight));

					};

				};
			};

		};


	}

	// set the colour in the Dictionary
	// need to do disable here
	color_{|key,color|
		if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		this.refresh;
	}

	autoSize{
		if (view.notClosed) {
			view.bounds_(Rect(0,0,w,this.internalHeight+1));
		};
	}

	internalHeight{
		^(items.size*fontHeight).clip(h-1,h+(items.size*fontHeight));
	}

	minSize{
		view.bounds_(Rect(0,0,w+2,h)); // what is this??
	}

	showItem{
		if ((value*fontHeight)<(scrollView.visibleOrigin.y)) {
			scrollView.visibleOrigin_(0@(value*fontHeight))
		}{
			if ((value*fontHeight)>(scrollView.visibleOrigin.y
							+(scrollView.bounds.height)-fontHeight)) {
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
		var val, val2, clickCounts;

		view.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			var updateValue=true;

			clickCounts=clickCount;
			if (modifiers.isAlt ) { buttonNumber=1 };
			if (modifiers.isCtrl) { buttonNumber=2 };

			if (editMode) {
				lw=lh=nil;
				startX=x;
				startY=y;
				scrollView.bounds.postln;
				updateValue=false;
			}{

				if (buttonNumber==2) { this.toggleMIDIactive; updateValue=false; };

				if (clickCount==2) {
					this.valueActions(\doubleClickAction, this);
					if (model.notNil) { model.valueActions(\doubleClickAction,this) };
					updateValue=false;
				};

				if (clickCount==3) {
					this.valueActions(\tripleClickAction, this);
					if (model.notNil) { model.valueActions(\tripleClickAction,this) };
					updateValue=false;
				};

				if (updateValue) {
					this.valueAction_(y.div(fontHeight).clip(0,items.size-1));
				};

				this.valueActions(\anyClickAction, this, clickCount);
				if (model.notNil) { model.valueActions(\anyClickAction,this, clickCount) };
			}
		};
		view.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode) { this.moveBy(x-startX,y-startY) };
		};

		view.mouseUpAction_{|me,x, y, modifiers, buttonNumber, clickCount|

			if (clickCounts==1) {
				this.valueActions(\upSingleClickAction, this);
				if (model.notNil) { model.valueActions(\upSingleClickAction,this) };
			};

			if (clickCounts==2) {
				this.valueActions(\upDoubleClickAction, this);
				if (model.notNil) { model.valueActions(\upDoubleClickAction,this) };
			};

			if (clickCounts==3) {
				this.valueActions(\upTripleClickAction, this);
				if (model.notNil) { model.valueActions(\upTripleClickAction,this) };
			};

		};

		// @TODO: new Qt "key" codes
		// nothing lets this view has focus so no key actions used at the moment
		view.keyDownAction_{|me, char, modifiers, unicode, keycode, key|
			var index;
			[me, char, modifiers, unicode, keycode, key].postln;

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
			// up arrow
			if (unicode == 16rF700, {
				this.valueAction =  (this.value.asInt - 1).wrap(0,items.size-1)

			});
			// down arrow
			if (unicode == 16rF703, {
				this.valueAction =  (this.value.asInt + 1).wrap(0,items.size-1)

			});
			// ?
			if (unicode == 16rF701, {
				this.valueAction =  (this.value.asInt + 1).wrap(0,items.size-1)
			});

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
			view.bounds_(Rect(0,0,w,this.internalHeight));
		}
	}

	// resize action called by parents
	doResizeAction{
		if (this.notClosed) {
			//scrollView.bounds.postln; // why is this changing ?
			//rect=scrollView.bounds; // needed to change this for scrollView

			// i've deactivated the above becasue i can't work it out

			l=rect.bounds.left;
			t=rect.bounds.top;
			w=rect.bounds.width;
			h=rect.bounds.height;
			resizeAction.value(this);
			this.adjustLabels;
		}
	}

	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		if (view.notClosed) {
//			view.background_(colors[bool.if(\midiLearn,enabled.if(
//											\background,\backgroundDisabled))]);
			view.refresh;
		};
		labelGUI.do(_.refresh);
	}

	// set the font
	font_{|argFont|
		font=argFont;
		fontHeight="".bounds(font).height+3;
		if (view.notClosed) {
			view.refresh;
			this.autoSize;
		};
	}

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		if (view.notClosed) {
			//view.background_(colors[enabled.if(\background,\backgroundDisabled)]);
			view.refresh;
		};
		labelGUI.do(_.refresh);
	}

	// item is enabled
	enabled_{|bool|
		if (enabled!=bool) {
			enabled=bool;
			{
				if (view.notClosed) {
					view.refresh;
//					view.background_(colors[midiLearn.if(\midiLearn,
//						enabled.if(\background,\backgroundDisabled))])
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
			view.refresh;
			//view.value_(value);
			this.showItem;
		}
	}

	// unlike SCView there is no refresh needed
	refresh{ this.refreshValue }

}
