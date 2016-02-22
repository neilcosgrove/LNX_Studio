
// ScrollView

// a scroll view forces every item to redraw

MVC_RoundedCompositeView : MVC_CompositeView {
	
	var <>edit=false, <>verbose=false;
	var <roundedView,		<startX,		<startY;
	var noResizeFlag=true;
	
	var startBounds, st,sl,sr,sb;
			
	forceHold_{|flag| roundedView.forceHold_(flag) } // temp bug fix in VOLCA beats
			
	*new {|view,bounds| ^super.new.init2(view,bounds) }
		
	init2 {|argView,bounds|
		this.init(argView,bounds);
		this.hasBorder_(false)
			.autoScrolls_(false)
			.hasVerticalScroller_(false);
		roundedView=MVC_RoundBounds(argView,bounds);
		
		if (noResizeFlag) {roundedView.noResize};
		
	{
		
		var buttonPressed;
		
		roundedView.views[\top].mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			startX=x;
			startY=y;
			buttonPressed=buttonNumber;
			if (edit||verbose) {view.bounds.postln};
			startBounds=view.bounds;
			st=roundedView.views[\top].bounds;
			sl=roundedView.views[\left].bounds;
			sr=roundedView.views[\right].bounds;
			sb=roundedView.views[\bottom].bounds;
		};
	
		roundedView.views[\top].mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			if (edit) {
				this.moveBy(x-startX,y-startY,buttonPressed);
			}
		};
	
	}.defer(1);
		
	}
	
	
	moveBy{|x,y,button=0|
		
		x=x.asInt; y=y.asInt;
		
		view.bounds_(view.bounds.moveBy(x,y).postln);
		
		roundedView.views[\top].bounds_(roundedView.views[\top].bounds.moveBy(x,y));
		roundedView.views[\left].bounds_(roundedView.views[\left].bounds.moveBy(x,y));
		roundedView.views[\right].bounds_(roundedView.views[\right].bounds.moveBy(x,y));
		roundedView.views[\bottom].bounds_(roundedView.views[\bottom].bounds.moveBy(x,y));
		
	}
	
	noResize{
		noResizeFlag=true;
		roundedView.noResize;
	}
	
	hasResize{
		noResizeFlag=false;
		roundedView.hasResize;
	}
	
	width_{|x| roundedView.width_(x) }
	
	width{ ^roundedView.width }
	
	// set the color in the Dictionary 
	color_{|index,color...more|
		colors[index]=color;
		if ((index=='background') and:{ view.notClosed}) {
			{view.background_(color)}.defer;	
		};
		if (index=='border') { roundedView.color_(\background,color)};
	}
	
}
	

MVC_CompositeView : MVC_ScrollView {

	var borderGUI;
		
	//*new {|view,bounds| ^super.new.init(view,bounds) }
	
	// set the left only
	left_{|value|
		if (value!=(rect.left)) {
			rect=rect.left_(value);
			this.bounds_(rect)
		}
	}

	// set the top only
	top_{|value|
		if (value!=(rect.top)) {
			rect=rect.top_(value);
			this.bounds_(rect)
		}
	}
	
	// short cut for now	
	*new {|view,bounds,hasBorder=true| ^super.new.init(view,bounds,hasBorder) } 			
	init {|argView,bounds,argHasBorder|

		hasBorder=argHasBorder;

		 case
			{argView.isKindOf(Rect)} {
				rect=argView;
			}
			{argView.isKindOf(MVC_Window)} {
				if (argView.isOpen) {window=argView.view } {window=nil};
				rect=bounds;
				parent = argView;
				argView.addView(this);
			}
			{argView.isKindOf(MVC_ScrollView)} {
				if (argView.isOpen) {window=argView.view } {window=nil};
				rect=bounds;
				parent = argView;
				argView.addView(this);
			}
			{argView.isKindOf(MVC_CompositeView)} {
				if (argView.isOpen) {window=argView.view } {window=nil};
				rect=bounds;
				parent = argView;
				argView.addView(this);
			}
			{argView.isKindOf(MVC_TabView)} {
				if (argView.isOpen) {window=argView.view } {window=nil};
				rect=bounds;
				parent = argView;
				argView.addView(this);
			}
			{argView.isKindOf(Window)} {
				window=argView;  // else is view or window
				rect=bounds;
			};
			
		if (parent.notNil) { parentViews = parent.parentViews };
		
		colors=IdentityDictionary[];
		gui=[];
		borderGUI=IdentityDictionary[];
		visibleOrigin=0@0;
		
		if (window.notNil) { this.create(window) };

	}

	// add or remove an MVC_View to the view, all views will be created this scrollView
	
//	addView{|view|
//		gui=gui.add(view);
//		if (view.isKindOf(MVC_View)) {
//			view.viewEditMode_(editMode).viewEditResize_(editResize).viewGrid_(grid);
//		};
//	}
//	removeView{|view| gui.remove(view) }
	
	// create the gui's items that make this MVC_View
//	create{|argParent|
//		if (view.isClosed) {
//			if (argParent.notNil) {
//				window = argParent;
//				parent = argParent.asView; // actual view
//			};
//			this.createView;
//		}{
//			"View already exists.".warn;
//		}
//	}
	
	// override this
	createView{
		
		var l,t,w,h;
		
		view = CompositeView.new(window,rect)
//			.hasBorder_(hasBorder)
//			.autoScrolls_(autoScrolls)
//			.hasHorizontalScroller_(hasHorizontalScroller)
//			.hasVerticalScroller_(hasVerticalScroller)
//////			.autohidesScrollers_(autohidesScrollers)  // this causes redraws on creation
			.resize_(resize)
//			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
//				"THIS DOESN'T WORK???".postln;
//				mouseDownAction.value(me, x, y, modifiers, buttonNumber, clickCount);
//			}
		;
		if (colors[\background].notNil) {
			view.background_(colors[\background])
		};
		
		// these are not resized !!!
		
		if (hasBorder) {
			l=rect.left;
			t=rect.top;
			w=rect.width;
			h=rect.height;
			
			myGUI=[
			
				UserView(window,Rect(l,t-1,w,1))
					.canFocus_(false)
					.drawFunc_{|me|
						Pen.use{
							//Pen.width_(1);
							//Pen.smoothing_(false);
							Color.black.set;
							Pen.fillRect(Rect(0,0,w,1));
						}
					} ,
					
				UserView(window,Rect(l-1,t,1,h))
					.canFocus_(false)
					.drawFunc_{|me|
						Pen.use{
							//Pen.width_(1);
							//Pen.smoothing_(false);
							Color.black.set;
							Pen.fillRect(Rect(0,0,1,h));
						}
					} ,
				
				UserView(window,Rect(l,t+h,w,1))
					.canFocus_(false)
					.drawFunc_{|me|
						Pen.use{
							//Pen.width_(1);
							//Pen.smoothing_(false);
							Color.black.set;
							Pen.fillRect(Rect(0,0,w,1));
						}
					},
		
				UserView(window,Rect(l+w,t,1,h))
					.canFocus_(false)
					.drawFunc_{|me|
						Pen.use{
							//Pen.width_(1);
							//Pen.smoothing_(false);
							Color.black.set;
							Pen.fillRect(Rect(0,0,1,h));
						}
					}
			];
			
		};
		
		if (addFlowLayout) { view.addFlowLayout(margin, gap) };
		
		gui.do(_.create(view)); // now make all views inside this view
		
		//view.visibleOrigin_(visibleOrigin);
		
	
	}
	
//	// get properties
//	
//	isClosed { ^view.isClosed }
//	notClosed { ^view.notClosed }
//	bounds { if (view.notClosed) {^view.bounds} {^rect} }
//	visibleOrigin { if (view.notClosed) {^visibleOrigin=view.visibleOrigin} { visibleOrigin=0@0 } }
//	
	// set properties
	
//	// set the bounds
//	bounds_{|argRect|
//		rect=argRect;
//		if (view.notClosed) {^view.bounds_(rect)};
//	}
	
	visibleOrigin_{|point|
		visibleOrigin=point;
		//if (view.notClosed) { view.visibleOrigin_(point) }
	}
	
	// boarder
	hasBorder_{|bool|
		hasBorder=bool;
		//if (view.notClosed) {	view.hasBorder_(bool) }
	}
	
	// auto scroll
	autoScrolls_{|bool|
		autoScrolls=bool;
		//if (view.notClosed) {	view.(bool) }
	}
	
	// has Horizontal Scroller
	hasHorizontalScroller_{|bool|
		hasHorizontalScroller=bool;
		//if (view.notClosed) {	view.hasHorizontalScroller_(bool) }
	}
	
	// has Vertical Scroller
	hasVerticalScroller_{|bool|
		hasVerticalScroller=bool;
		//if (view.notClosed) {	view.hasVerticalScroller_(bool) }
	}
	
	// auto hides scrollers (this causes many redraws)
	autohidesScrollers_{|bool|
		autohidesScrollers=bool;
		//if (view.notClosed) {	view.autohidesScrollers_(bool) }
	}
	
//	resize_{|num|
//		resize=num;
//		if (view.notClosed) {	view.resize_(resize) }
//	}
	
//	doResizeAction{
//		if (this.notClosed) {
//			if (resize!=1) {
//				rect=view.bounds; // i still need to adapt the if & this for other resizes
//			};
//			resizeAction.value(this);
//			gui.do{|item|
//				item.doResizeAction(this)
//			}
//		}
//	}
//	
//	// delete this object
//	free{
//		gui.do{|i| i.free};
//		gui=nil;
//		this.remove;
//		parent=nil;
//		window=nil;
//	}
	
	// from from window
//	remove{
//		if (view.notClosed) {
//			view.remove;
//			view=nil;
//		}
//	}
	
	// set the color in the Dictionary 
//	color_{|index,color...more|
//		colors[index]=color;
//		if ((index=='background') and:{ view.notClosed}) {
//			{view.background_(color)}.defer;	
//		}
//	}
	
	// refresh the view
//	refresh{ if (view.notClosed) {view.refresh} }
//	
//	refreshColors{
//		if (this.notClosed) {
//			if (colors[\background].notNil) {
//				view.background_(colors[\background])
//			};
//		}
//	}
//	
//	// does rect intersect other gui items? (warning labels are created after view?)
//	intersects{|rect|
//		var i=0,	j;
//		while ({i<(gui.size)}, {
//			// the view
//			if (gui[i].bounds.intersects(rect)) { ^true };
//			// the labels
//			if (gui[i].respondsTo(\labelBounds)and:{gui[i].labelBounds.notNil}) {
//				j=0;
//				while ({j<(gui[i].labelBounds.size)}, {
//					if (gui[i].labelBounds[j].intersects(rect)) { ^true };
//					j=j+1; // iterate
//				});
//			};
//			// the number
//			if (gui[i].respondsTo(\numberBounds)and:{gui[i].numberBounds.notNil}) {
//				if (gui[i].numberBounds.intersects(rect)) { ^true };
//			};
//			i=i+1; // iterate
//		});
//		^false;
//	}
	
}

