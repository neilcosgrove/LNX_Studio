
// there are different versions becasue of clipping issues with com view

MVC_RoundedComView : MVC_RoundedScrollView {

	createView{
		view = CompositeView.new(window,rect).resize_(resize);
		if (colors[\background].notNil) { view.background_(colors[\background]) };
		if (addFlowLayout) { view.addFlowLayout(margin, gap) };
		gui.do(_.create(view)); // now make all views inside this view
		this.postCreate;
	}
		
	visibleOrigin_{|point| visibleOrigin=point }
	hasBorder_{|bool| hasBorder=bool }
	autoScrolls_{|bool| autoScrolls=bool }
	hasHorizontalScroller_{|bool| hasHorizontalScroller=bool }
	hasVerticalScroller_{|bool| hasVerticalScroller=bool }
	autohidesScrollers_{|bool| autohidesScrollers=bool }
	
}

MVC_RoundedScrollView : MVC_ScrollView {
	
	var <>width=6, <views, <resizeList; // in future might need resizeList_{|array| ...
	
	refreshOthers{ if (view.notClosed) {views.do(_.refresh)} }
	
	postInit{
		resize=5;
		
		resizeList = []; // 0:view 1:left 2:top 3:right 4:bottom [5,4,2,6,8]
		
		if (colors[\border].isNil) {
			colors=colors++( 'border' 	: Color(0,0,0,0.5) );
		};
		views=IdentityDictionary[];
	}
	
	resizeList_{|list|
		resizeList = list;
		if (view.notNil) {
			view.resize_(resizeList[0]);
			views[\left].resize_(resizeList[1]);
			views[\top].resize_(resizeList[2]);
			views[\right].resize_(resizeList[3]);
			views[\bottom].resize_(resizeList[4]);
		};
	}
	
	bounds_{|argRect|
		var l,t,w,h;
		
		rect=argRect;
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		
	
		if (view.notClosed) {
			
			views[\left].bounds_(Rect(l-width,t,width,h));
			views[\top].bounds_(Rect(l-width,t-width,w+(width*2),width));
			views[\right].bounds_(Rect(l+w,t,width,h));
			views[\bottom].bounds_(Rect(l-width,t+h,w+(width*2),width));
			
			
			
			^view.bounds_(rect);
		};
	
		
	}
	
		
	// delete this object
	free{
		views.do{|i| i.remove.free};
		myGUI.do{|i| i.remove.free};
		myGUI=nil;
		gui.do{|i| i.free};
		gui=nil;
		this.remove;
		parent=nil;
		window=nil;
	}
	
	
	postCreate{
			
		var l,t,w,h;
		l=view.bounds.left;
		t=view.bounds.top;
		w=view.bounds.width;
		h=view.bounds.height;
		
		
		view.resize_(resizeList[0]?resize);
		
		// these two paint over the area that is suppose to be clipped by com view
		
		if (colors[\border2].notNil) {
			
			UserView(window,Rect(l-width,t+h+width,w+width+width,width))
				.canFocus_(false)
				.resize_(8)
				.drawFunc_{|me|
					var thisRect=me.bounds;
					Pen.fillColor_(colors[\border2].set);
					Pen.fillRect(Rect(0,0,thisRect.width,thisRect.height));
				};
				
			UserView(window,Rect(l+w+width,t-width-width,width,h+width+width+width))
				.canFocus_(false)
				.resize_(6)
				.drawFunc_{|me|
					var thisRect=me.bounds;
					Pen.fillColor_(colors[\border2].set);
					Pen.fillRect(Rect(0,0,thisRect.width,thisRect.height));
				};
			
		};
		
		// these 4 are the border
		
		views[\left] = UserView.new(window,Rect(l-width,t,width,h))
			.canFocus_(false)
			.resize_(resizeList[1]?4)
			.drawFunc={|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				w=thisRect.width;
				h=thisRect.height;
				
				Pen.use{
					Pen.smoothing_(true);			
					Pen.fillColor_(colors[\border].set);
					Pen.fillRect(Rect(0,0,w,h));
				}; // end.pen
			};
		
		views[\top] = UserView.new(window,Rect(l-width,t-width,w+(width*2),width))
			.canFocus_(false)
			.resize_(resizeList[2]?2)
			.drawFunc={|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				l=thisRect.left;
				t=thisRect.top;
				w=thisRect.width;
				h=thisRect.height;
				Pen.use{
					Pen.smoothing_(true);			
					Pen.fillColor_(colors[\border].set);
					Pen.fillRect(Rect(h,0,w-(h*2),h));
					Pen.color_(colors[\border]);
					Pen.addWedge(h@h, h, 2pi*1.5, 2pi*0.25); // top left
					Pen.addWedge((w-h)@h, h, 2pi*1.75, 2pi*0.25); // top right
					Pen.perform(\fill);
				}; // end.pen
			};	
			
		views[\right] = UserView.new(window,Rect(l+w,t,width,h))
			.canFocus_(false)
			.resize_(resizeList[3]?6)
			.drawFunc={|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				w=thisRect.width;
				h=thisRect.height;

				Pen.use{
					Pen.smoothing_(true);			
					Pen.fillColor_(colors[\border].set);
					Pen.fillRect(Rect(0,0,w,h));
				}; // end.pen
			};
			
		views[\bottom] = UserView.new(window,Rect(l-width,t+h,w+(width*2),width))
			.canFocus_(false)
			.resize_(resizeList[4]?8)
			.drawFunc={|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				l=thisRect.left;
				t=thisRect.top;
				w=thisRect.width;
				h=thisRect.height;

				Pen.use{
					Pen.smoothing_(true);			
					Pen.fillColor_(colors[\border].set);
					Pen.fillRect(Rect(h,0,w-(h*2),h));
					Pen.color_(colors[\border]);
					Pen.addWedge(h@0, h, 2pi*2.25, 2pi*0.25); // bottom left
					Pen.addWedge((w-h)@0, h, 2pi*2, 2pi*0.25); // bottom right
					Pen.perform(\fill);
				}; // end.pen
			};
		
	}
	
}


// ScrollView

// a scroll view forces every item to redraw

MVC_ScrollView {

	var	<parent,		<>window, 	<rect,	<view, <action, <parentViews;
		
	var	<hasBorder=true,
		<autoScrolls=false,			<autohidesScrollers=true,
		<hasHorizontalScroller=false,	<hasVerticalScroller=false;
		
	var	<gui, 		<colors, 		<myGUI,    <>mouseDownAction;
	
	var	<resize=1,	<>resizeAction;
	
	var <editMode=false,	<grid=1,		<editResize=false; // view editing
	
	var visibleOrigin, <visible=true;
	
	var <margin, <gap, addFlowLayout=false;
	
	*new {|view,bounds,colors| ^super.new.init(view,bounds,colors) }
		
	init {|argView,bounds,argColors|

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
				window=nil;
				rect=bounds;
				parent = argView;
				argView.addView(this);
			}
			{argView.isKindOf(Window)} {
				window=argView;  // else is view or window
				rect=bounds;
			};
		
		colors= argColors ? IdentityDictionary[];
		gui=[];
		visibleOrigin=0@0;
		
		myGUI=IdentityDictionary[];
		
		if (parent.notNil) { parentViews = parent.parentViews };
		
		this.postInit;
		
		if (window.notNil) { this.create(window) };

	}

	action_ {|func|
		action = func;
		if (view.notNil) {
			view.action = func;
		};
	}

	postInit{}

	// add or remove an MVC_View to the view, all views will be created this scrollView
	
	addView{|view|
		gui=gui.add(view);
		if (view.isKindOf(MVC_View)) {
			view.viewEditMode_(editMode).viewEditResize_(editResize).viewGrid_(grid);
		};
	}
	removeView{|view| gui.remove(view) }
	
	// create the gui's items that make this MVC_View
	create{|argWindow|
		if (view.isClosed) {
			if (argWindow.notNil) {
				window = argWindow.asView;
				//parent = argWindow; // actual view
			};
			this.createView;
		}{
			"View already exists.".warn;
		}
	}
	
	// override this
	createView{
		view = ScrollView.new(window,rect)
			.hasBorder_(hasBorder)
			// .autoScrolls_(autoScrolls)
			.hasHorizontalScroller_(hasHorizontalScroller)
			.hasVerticalScroller_(hasVerticalScroller)
			.visible_(visible)
//			.autohidesScrollers_(autohidesScrollers)  // this causes redraws on creation
			.resize_(resize)
		;
		if (action.notNil) { view.action = action };
		if (colors[\background].notNil) { view.background_(colors[\background]) };
		if (addFlowLayout) { view.addFlowLayout(margin, gap) };
		gui.do(_.create(view)); // now make all views inside this view
		this.postCreate;
		view.visibleOrigin_(visibleOrigin);
	}
	
	postCreate{}
	
	// get properties
	
	isClosed { if(view.notNil) {^view.isClosed} {^true} }
	notClosed { ^this.isClosed.not }
	isOpen { ^this.notClosed }
	
	bounds { if (view.notClosed) {^view.bounds} {^rect} }
	visibleOrigin { if (view.notClosed)
		{^visibleOrigin=view.visibleOrigin} { ^visibleOrigin=0@0 } }
	
	focus { if (this.isOpen) {view.focus} }
	
	// set properties
	
	// set the bounds
	bounds_{|argRect|
		rect=argRect;
		if (view.notClosed) {^view.bounds_(rect)};
	}
	
	visibleOrigin_{|point|
		visibleOrigin=point;
		if (view.notClosed) { view.visibleOrigin_(point) }
	}
	
	// is this window visable i.e. open and not hidden
	isVisible{
		^((view.notNil) && visible and:{view.isClosed.not})
	}
	
	
	// boarder
	hasBorder_{|bool|
		hasBorder=bool;
		if (view.notClosed) {	view.hasBorder_(bool) }
	}
	
	// auto scroll
	autoScrolls_{|bool|
		autoScrolls=bool;
		// if (view.notClosed) {	view.autoScrolls_(bool) }
	}
	
	visible_{|bool|
		visible=bool;
		if (view.notClosed) {	view.visible_(bool) }
	}
	
	// has Horizontal Scroller
	hasHorizontalScroller_{|bool|
		hasHorizontalScroller=bool;
		if (view.notClosed) {	view.hasHorizontalScroller_(bool) }
	}
	
	// has Vertical Scroller
	hasVerticalScroller_{|bool|
		hasVerticalScroller=bool;
		if (view.notClosed) {	view.hasVerticalScroller_(bool) }
	}
	
	// auto hides scrollers (this causes many redraws)
	autohidesScrollers_{|bool|
		autohidesScrollers=bool;
		//if (view.notClosed) {	view.autohidesScrollers_(bool) }
	}
	
	resize_{|num|
		resize=num;
		if (view.notClosed) {	view.resize_(resize) }
	}
	
	doResizeAction{
		if (this.notClosed) {
			if (resize!=1) {
				rect=view.bounds; // i still need to adapt the if & this for other resizes
			};
			resizeAction.value(this);
			gui.do{|item|
				item.doResizeAction(this)
			}
		}
	}
	
	// delete this object
	free{
		myGUI.do{|i| i.remove.free};
		myGUI=nil;
		gui.do{|i| i.free};
		gui=nil;
		this.remove;
		parent=nil;
		window=nil;
	}
	
	// from from window
	remove{
		if (parent.notNil) { parent.removeView(this) }; // remove so this view can be deleted
		if (view.notClosed) {
			view.remove;
			view=nil;
		}
	}
	
	// set the color in the Dictionary 
	color_{|index,color...more|
		colors[index]=color;
		if ((index=='background') and:{ view.notClosed}) {
			{view.background_(color)}.defer;	
		}{
			this.refreshOthers	
		}
	}
	
	refreshOthers{}
	
	// refresh the view
	refresh{ if (view.notClosed) {view.refresh} }
	
	refreshColors{
		if (this.notClosed) {
			if (colors[\background].notNil) {
				view.background_(colors[\background])
			};
		}
	}
	
	// does rect intersect other gui items? (warning labels are created after view?)
	intersects{|rect|
		var i=0,	j;
		while ({i<(gui.size)}, {
			// the view
			if (gui[i].bounds.intersects(rect)) { ^true };
			// the labels
			if (gui[i].respondsTo(\labelBounds)and:{gui[i].labelBounds.notNil}) {
				j=0;
				while ({j<(gui[i].labelBounds.size)}, {
					if (gui[i].labelBounds[j].intersects(rect)) { ^true };
					j=j+1; // iterate
				});
			};
			// the number
			if (gui[i].respondsTo(\numberBounds)and:{gui[i].numberBounds.notNil}) {
				if (gui[i].numberBounds.intersects(rect)) { ^true };
			};
			i=i+1; // iterate
		});
		^false;
	}
	
	// for expand / collapse view
	
	addFlowLayout {|argMargin,argGap|
		addFlowLayout=true;
		margin=argMargin;
		gap=argGap;
		if (view.notClosed) {view.addFlowLayout(margin, gap) };
	}
		
	// for widget editing
			
	editMode_{|bool|
		editMode=bool;
		gui.do{|view|
			if (view.isKindOf(MVC_View)) {
				view.viewEditMode_(editMode)
			};		
		};
	}
	
	editResize_{|bool|
		editResize=bool;
		gui.do{|view|
			if (view.isKindOf(MVC_View)) {
				view.viewEditResize_(editResize)
			};
		};
	}
	
	grid_{|pixels|
		grid=pixels;
		gui.do{|view|
			if (view.isKindOf(MVC_View)) {
				view.viewGrid_(pixels)
			};
		};
	}
		
	
}

