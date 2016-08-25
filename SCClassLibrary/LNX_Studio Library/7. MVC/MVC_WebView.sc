
// MVC_WebView
/*
(
	w=MVC_Window();
	v=MVC_WebView(w, Rect(10,10,350,350))
		.resize_(5);
	w.create;
)

v.html_("Not found")


*/

MVC_WebView{

	var <window, bounds, <view, <rect;
	var <canFocus=true, <focusColor, <drawFunc;
	var <resize=5, <editable=true;
	var <url, <onLoadFinished, <onLoadFailed, <onLinkActivated, <enterInterpretsSelection=false;

	title{ if (this.isOpen) {^view.title} {""} }

	url_{|path|
		url=path;
		if (this.isOpen) {view.url_(url) };
	}

	onLoadFinished_{|action|
		onLoadFinished=action;
		if (this.isOpen) {view.onLoadFinished_(action)};
	}

	onLoadFailed_{|action|
		onLoadFailed=action;
		if (this.isOpen) {view.onLoadFailed_(action)};
	}

	onLinkActivated_{|action|
		onLinkActivated=action;
		if (this.isOpen) {view.onLinkActivated_(action)};
	}

	enterInterpretsSelection_{|bool|
		enterInterpretsSelection=bool;
		if (this.isOpen) {view.enterInterpretsSelection_(bool)};
	}

	editable_{|bool|
		editable=bool;
		if (this.isOpen) {view.editable_(bool)}
	}

	selectedText { if (this.isOpen) {^view.getProperty(\selectedText)} {^""} }

	forward { if (this.isOpen) {view.forward} }
	back    { if (this.isOpen) {view.back   } }
	reload  { if (this.isOpen) {view.reload } }
	didLoad { onLoadFinished.value(this); }
	didFail { onLoadFailed.value(this); }
	linkActivated {|linkString| onLinkActivated.value(this, linkString) }

	// Get the displayed content in html form.
	html { if (this.isOpen) {^view.html} {^""} }

	// Set html content.
	html_ {|htmlString|
		if (this.isOpen) {view.html_(htmlString)};
	}

	findText { arg string, reverse = false;
		if (this.isOpen) {view.findText(string, reverse) };
	}

	*new {|...args| ^super.new.init(*args) }

	init {|...args|

		bounds  = args.findKindOf(Rect);
		rect = bounds;
		window  = args.findKindOf(MVC_Window)
				?? {args.findKindOf(MVC_TabbedView)}
				?? {args.findKindOf(MVC_TabView)}
				?? {args.findKindOf(MVC_ScrollView)};

		// will need to take care of MVC_TabbedViews here
		if (window.notNil) {
			// register this MVC_item with the view
			if (window.isKindOf(MVC_TabbedView)) {
				// get index to the tabbed view from supplied args index +1
				window.addView(args[args.indexOf(window)+1],this);
			}{
				window.addView(this);	// add so this view can be created with the MVC_ScrollView
			};
			window=nil;			// MVC_ScrollView is not a real view so don't use as a window
		}{
			window=args.findKindOf(Window)
					?? { args.findKindOf(ScrollView)}
					?? {args.findKindOf(CompositeView)};
		};

		// and make it if in a standard window
		if (window.notNil) { this.create(window) }
	}

	create{|argParent|
		if (view.isClosed) {
			window=argParent;
			this.createView;
		}{
			"View already exists.".warn;
		}
	}

	createView{

		view = WebView(window,rect)
			.editable_(editable)
			.canFocus_(canFocus)
			.resize_(resize)
			.onLoadFinished_(onLoadFinished)
			.onLoadFailed_(onLoadFailed)
			.onLinkActivated_(onLinkActivated)
			.enterInterpretsSelection_(enterInterpretsSelection);

			if (url.notNil) {view.url_(url)};

	}

	// user can focus on item
	canFocus_{|bool|
		canFocus=bool;
		if (view.notClosed){ view.canFocus_(canFocus) }
	}

	// set focus to this item
	focus{ if (view.notClosed) { view.focus } }

	focusColor_{}

	refresh{ if (view.notClosed) { view.refresh } }

	bounds_{|argRect|
		bounds=argRect;
		rect=bounds;
		if (view.notClosed) { view.bounds_(rect) }
	}

	bounds{^rect}

	// resize item
	resize_{|num|
		resize=num;
		if (view.notClosed){ view.resize_(resize) }
	}

	// from from window
	remove{
		if (view.notClosed) { view.free };
		view=nil;
	}

	// resize action called by parents
	doResizeAction{
		if (this.notClosed) {
			rect=view.bounds;
		}
	}

	// is closed
	isClosed { if (view.isNil) { ^true } { ^view.isClosed } }

	// is not closed
	notClosed { ^this.isClosed.not }

	isOpen { ^this.notClosed }

}
