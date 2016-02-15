
// TabbedView
// a scroll view forces every item to redraw many times!
// you can't add or remove tabs yet
		
MVC_TabbedView {

	var	<parent,		<>window, 		<rect,		<view,     <parentViews;
		
	var	<gui, 		<colors,			<>font;	
	
	var	<resize=1,	<>resizeAction;
	
	var	<>tabPosition='bottom', 		<>tabCurve=8, 
		<>tabWidth='auto', 			<>tabHeight='auto',
		<>followEdges=true;
	
	var	<labels,						<>unfocusedColors,
		<>labelColors,				<>backgrounds;
		
	var	<value=0;
	
	var 	<>action, 		<focusActions, 	<unfocusActions, <>onClose;
	
	var	<mvcTabs, <offset, <scroll;
	
	var <>adjustments;
	
	*new {|view,bounds,offset,scroll=false| ^super.new.init(view,bounds,offset,scroll) }
		
	init {|argView,bounds,argOffset,argScroll|

		// also need to add Tabbed here as a parent view, like in MVC_View

		offset = argOffset ? (0@0);
		scroll = argScroll;
		
		adjustments=[];

		 case
			{argView.isKindOf(Rect)} {
				rect=argView;
			}
			{argView.isKindOf(MVC_Window)} {
				window=nil;
				rect=bounds;
				argView.addView(this);
				if (argView.parentViews.notNil)
					{ parentViews = argView.parentViews };
			
			}
			{argView.isKindOf(MVC_ScrollView)} {
				window=nil;
				parent = argView;
				rect=bounds;
				argView.addView(this);
				if (argView.parentViews.notNil)
					{ parentViews = argView.parentViews};
			}
			{argView.isKindOf(SCWindow)} {
				window=argView;  // else is view or window
				parent = argView;
				rect=bounds;
			};
		
		colors=IdentityDictionary[][\background]=Color(0,0,0,0);
		gui=IdentityDictionary[];
		mvcTabs=[];

		//if (window.notNil) { this.create(window) }
	}

	// set the tabs, all instances must invoke this before creation
	labels_{|list|
		labels=list;
		list.size.do{|tabNo|
			// add a Dict if needed
			if (gui[tabNo].isNil) { 
				gui[tabNo]=IdentitySet[];
			};
			// add a new MVC_TabView
			if (mvcTabs.size<=tabNo) {
				mvcTabs=mvcTabs.add(MVC_TabView(this,tabNo))
			}
		}
	}

	// a list of actual views
	views{^view.views}
	
	// the actual view of tab(n)
	tab{|n| ^view.views[n] }
	
	// is tab n hidden
	tabIsHidden{|n| ^(n!=value) }
	
	// the mvc tab (use this to add your mvc views to)
	mvcTab{|n| ^mvcTabs[n] }

	// refresh the view
	refresh{ if (view.notClosed) {view.refresh} }

	// add or remove an MVC_View to the view, all views will be created as scrollView's
	// this works more like register
	
	addView{|index,view| gui[index].add(view) }
	removeView{|index,view| gui[index].remove(view) }
	
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
		
		font = font ? (GUI.font.default);
		
		view=TabbedView(window,rect,labels, scroll:scroll, offset:offset)
			.resize_          (resize)
			.tabPosition_     (tabPosition)
			.tabCurve_        (tabCurve)
			.tabWidth_        (tabWidth)
			.tabHeight_       (tabHeight)
			.font_            (font);
			
		if (followEdges.notNil)		{ view.followEdges_     (followEdges    ) };
		if (unfocusedColors.notNil)	{ view.unfocusedColors_ (unfocusedColors) };
		if (labelColors.notNil)		{ view.labelColors_     (labelColors    ) };
		
		if (focusActions.notNil) 	{ view.focusActions_(focusActions) };
		if (unfocusActions.notNil) 	{ view.unfocusActions_(unfocusActions) };
		
		if (backgrounds.notNil)		{
			view.backgrounds_     (backgrounds    );
			colors[\background]=backgrounds.last;	
		};
		
		
		adjustments.do{|i,j|
			if (i.notNil) {
				view.views[j].bounds_(i+view.views[j].bounds);
			};
			
		};
		
			
		view.action_{|me|
				value=me.value;
				action.value(me);
			}
			.onClose_{ onClose.value(this) }
			.value_(value);
			
		view.views.do{|v,j| gui[j].do(_.create(v)) }; // // now make all views inside the views
		
		mvcTabs.do(_.createView);
		
	}
	
	refreshColors{
		if (this.notClosed) {
			if (labelColors.notNil)		{ view.labelColors_     (labelColors    ) };
			if (backgrounds.notNil)		{ view.backgrounds_     (backgrounds    ) };
		}
	}
	
	// get properties
	
	isClosed { ^view.isClosed }
	notClosed { ^view.notClosed }
	bounds { if (view.notClosed) {^view.bounds} {^rect} }

	bounds_{|bounds|
		rect=bounds;
		if (view.notClosed) {
			view.bounds_(bounds)
		}
	}
	
//	// generic set property
//	property_{|obj|
//		property=obj;
//		if (view.notClosed) {	view.property_(obj) }
//	}
	
	// active tab
	value_{|val|
		value=val;
		if (view.notClosed) {	view.value_(value) }
	}
	
	// resize number
	resize_{|num|
		resize=num;
		if (view.notClosed) {	view.resize_(resize) }
	}
	
	focusActions_{|actions|
		focusActions = actions;
		if (view.notClosed) { view.focusActions_(focusActions) };
		
	}
		
	unfocusActions_{|actions|
		unfocusActions = actions;
		if (view.notClosed) { view.unfocusActions_(unfocusActions) };
	}	
	
	
	// use in resize updates
	doResizeAction{
		if (this.notClosed) {
			rect=view.bounds;
			resizeAction.value(this);
			
			view.views.do{|v,j|
				gui[j].do{|item|
					item.doResizeAction(v); // cascade to children
				}
			}
		}
	}
	
	// delete this object
	free{
		gui.do{|i| i.do{|j| j.free}};
		mvcTabs.do{|i| i.do{|j| j.free}};
		gui=nil;
		mvcTabs=nil;
		this.remove;
		parent=nil;
		window=nil;
		focusActions=nil;
		unfocusActions=nil;
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
			
			backgrounds.do{|b,i| backgrounds[i]=color};
			view.backgrounds_     (backgrounds    );
			//this.refresh;
		}
	}
	
}

// an MVC_TabView is made for each tab in MVC_TabbedView
// it talks to both MVC_TabbedView and MVC_View (as a go between)
		
MVC_TabView {

	var <parent, <tabIndex, <gui, <parentViews;

	*new {|tabbedView,tabIndex| ^super.new.init(tabbedView,tabIndex) }
		
	init {|argtabbedView,argTabIndex|
		parent=argtabbedView;  // else is view or window
		tabIndex=argTabIndex;
		gui=[];
		//if (window.notNil) { this.create(window) }
		
		parentViews = parent.parentViews ++ [this];
	}
	
	// the actual view
	view{ ^parent.tab(tabIndex) }
	
	// i should really store bounds on close, will i ever want this?
	bounds { if (parent.notClosed) {^this.view.bounds} {^Rect(0,0,0,0)} }
	
	
	isVisible{ ^parent.notClosed && (parent.tabIsHidden(tabIndex).not) }
	
	isHidden{ ^parent.tabIsHidden(tabIndex) } // very useful to stop updates from other tabs
	
	isClosed { ^parent.isClosed }
	
	notClosed { ^parent.notClosed }
	
	isOpen { ^parent.notClosed }
	
	// you can't do any of these
	resize_{"You can't resize a tab you must resize it's parent".warn}
	
	bounds_{"You can't change the bounds of a tab you must resize it's parent".warn}
	
	// add & remove views
	
	addView{|view| gui=gui.add(view) }
	
	removeView{|view| gui.remove(view) }
	
	createView{
		var view;
		view=this.view;
		gui.do{|item| item.do(_.create(view)) }; // now make all views inside the views
	}
	
	// refresh the view
	refresh{ if (parent.notClosed) {this.view.refresh} }
	
	// delete this object
	free{
		gui.do{|i| i.do{|j| j.free}};
		gui=nil;
		this.remove;
		parent=nil;
	}
	
	// from from window
	remove{
		//gui.do(_.remove)
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
