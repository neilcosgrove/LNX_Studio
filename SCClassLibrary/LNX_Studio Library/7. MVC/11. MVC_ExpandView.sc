/*(
w = MVC_Window( "ExpandView", Rect(128,64,208,220) ).front;
q = MVC_ScrollView( w, Rect(0,0,208,220));
q.addFlowLayout(nil,1@1).hasVerticalScroller_(true).hasBorder_(false);
v = 5.collect({ // create 5 ExpandViews
	MVC_ExpandView( q, 200@112, 200@16  ) //
});
w.create;
)*/
/*
w.close;
w.open;
ColorPicker(v[0]);
*/

// SCScrollView

// a scroll view forces every item to redraw

MVC_ExpandView {

	var	<parent,		<>window, 	<rect,	<view;
	
	var	<smallBounds, <collapsed, button;
		
	var	<hasBorder=true,
		<autoScrolls=false,			<autohidesScrollers=true,
		<hasHorizontalScroller=false,	<hasVerticalScroller=false;
		
	var	<gui, 		<colors;
	
	var	<resize=1,	<>resizeAction, <>expandAction, <>collapseAction;
	
	var <editMode=false,	<grid=1,		<editResize=false; // view editing
	
	var visibleOrigin;
	
	expand{
		collapsed=false;
		if (view.notClosed) {
			gui.do{|view| view.hidden_(true)};
			view.expand;
		};
	}
	
	collapse{
		collapsed=true;
		if (view.notClosed) {
			gui.do{|view| view.hidden_(false)};
			view.collapse;
		};
	}
	
	expanded{^collapsed.not}
			
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
		
	*new {|view,bounds,smallBounds, collapsed = true, button = true| ^super.new.init(view,bounds,smallBounds, collapsed, button) }
		
	init {|argView,bounds,small,coll, but|
		
		collapsed=coll;
		
		button = but;

		 case
			{argView.isKindOf(Rect)} {
				rect=argView;
				smallBounds=bounds;
				
			}
			{argView.isKindOf(MVC_Window)} {
				window=nil;
				rect=bounds;
				smallBounds=small;
				argView.addView(this);
			}
			{argView.isKindOf(MVC_ScrollView)} {
				if (argView.isOpen) {window = argView.view } { window=nil };
				rect=bounds;
				smallBounds=small;
				argView.addView(this);
			}
			{argView.isKindOf(MVC_RoundedScrollView)} {
				if (argView.isOpen) {window = argView.view } { window=nil };
				rect=bounds;
				smallBounds=small;
				argView.addView(this);
			}
			{argView.isKindOf(MVC_TabView)} {
				window=nil;
				rect=bounds;
				smallBounds=small;
				argView.addView(this);
			}
			{argView.isKindOf(Window)} {
				window=argView;  // else is view or window
				rect=bounds;
				smallBounds=small;
			};
		
		colors=(\background:Color.white.alpha_(0.5));
		gui=[];

		visibleOrigin=0@0;
		
		if (window.notNil) { this.create(window) }
	}

	// add or remove an MVC_View to the view, all views will be created this scrollView
	
	addView{|view|
		gui=gui.add(view);
		if (view.isKindOf(MVC_View)) {
			view.viewEditMode_(editMode).viewEditResize_(editResize).viewGrid_(grid);
		};
	}
	removeView{|view| gui.remove(view) }
	
	// create the gui's items that make this MVC_View
	create{|argParent|
		if (view.isClosed) {
			if (argParent.notNil) {
				window = argParent;
				parent = argParent.asView; // actual view
			};
			this.createView;
		}{
			"View already exists.".warn;
		}
	}
	
	// override this
	createView{
		
		view=ExpandView(window,rect,smallBounds,collapsed,button)
			.resize_(resize)
			.expandAction_{
				expandAction.value;
				collapsed=false;
				gui.do{|view| view.hidden_(false)};
			}
			.collapseAction_{
				collapseAction.value;
				collapsed=true;
				{gui.do{|view| view.hidden_(true)}}.defer(0.075);
			};		
		
		if (colors[\background].notNil) {
			view.background_(colors[\background])
		};
				
		gui.do(_.create(view)); // now make all views inside this view
	
	}

	// get properties
	
	isClosed { ^view.isClosed }
	notClosed { ^view.notClosed }
	bounds { if (view.notClosed) {^view.bounds} {^rect} }
	visibleOrigin { if (view.notClosed) {^visibleOrigin=view.visibleOrigin} { visibleOrigin=0@0 } }
	
	// set properties
	
	// set the bounds
	bounds_{|argRect|
		rect=argRect;
		if (view.notClosed) {^view.bigBounds_(rect)};
	}
	
	visibleOrigin_{|point|
		visibleOrigin=point;
		if (view.notClosed) { view.visibleOrigin_(point) }
	}
	
	// boarder
	hasBorder_{|bool|
		hasBorder=bool;
		if (view.notClosed) {	view.hasBorder_(bool) }
	}
	
	// auto scroll
	autoScrolls_{|bool|
		autoScrolls=bool;
		if (view.notClosed) {	view.(bool) }
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
			rect=view.bounds;
			resizeAction.value(this);
			gui.do{|item|
				item.doResizeAction(this)
			}
		}
	}
	
	// delete this object
	free{
		gui.do{|i| i.free};
		gui=nil;
		this.remove;
		parent=nil;
		window=nil;
	}
	
	// from from window
	remove{
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
		}
	}
	
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
	
}

+ ExpandView {
	isClosed { if (view.notNil) { ^view.isClosed } {^true} }
	notClosed {^this.isClosed.not}
	remove {composite.remove}
	
}


