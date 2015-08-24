
// SCNSObject based AppKit views/controls interface to sc la la land
// blackrain

AppKitView {
	var <scnsobj, <>onClose;
	var <parent, <bounds, <>action;
	var cell;

	*new { arg parent, viewclass, initializer ...args;
		^super.new.init(parent, viewclass, initializer, args);
	}
/*	
	// CocoaViewAddaptor version
	init { arg prnt, viewclass, initializer, args;
	
		if (prnt.isKindOf(CocoaViewAdaptor)) {
			parent = prnt
		}{
			parent = prnt.asView;
		};

		if (args[0].isKindOf(Rect) or:{ args[0].isKindOf(Point) }) {
			bounds = args[0].asRect;
		};

		initializer = initializer ? "initWithFrame:";
		scnsobj = SCNSObject(viewclass, initializer, args);
		if (parent.isKindOf(CocoaViewAdaptor)) {
			this.parent.dataptr.asNSReturn.invoke("addSubview:", [scnsobj], true);
		}{
			this.window.dataptr.asNSReturn.invoke("addSubview:", [scnsobj], true);
		};
		parent.add(this);
	}
*/
	// the NO CocoaViewAdaptor version
	init { arg prnt, viewclass, initializer, args;
	
		parent = prnt.asView;

		if (args[0].isKindOf(Rect) or:{ args[0].isKindOf(Point) }) {
			bounds = args[0].asRect;
		};

		initializer = initializer ? "initWithFrame:";
		scnsobj = SCNSObject(viewclass, initializer, args);

		this.window.dataptr.asNSReturn.invoke("addSubview:", [scnsobj], true);

		parent.add(this);
	}

	asView { ^this }

	bounds_ { arg bnds;
		bounds = bnds;

	//	if (parent.relativeOrigin) {
			bounds.left = bounds.left + parent.absoluteBounds.left;
			bounds.top = bounds.top + parent.absoluteBounds.top;
	//	};
		
		scnsobj.invoke("setFrame:", [ this.bounds ], true);
		this.window.refresh;	// a better way to do this than refreshing the whole parent view?
	}
	window {
		var parentView, win;
		try {

			parentView = parent.getParents.last;

			if (parentView.notNil) {
				win = parentView.findWindow;
			}{
				win = parent.findWindow;
			};
		}{ arg error;
			scnsobj.release;
			error.throw;
		};
		^win
	}
	size { arg width, height;
		bounds.width = width ? bounds.height;
		bounds.height = height ? bounds.height;
		scnsobj.invoke("setFrame:", [ bounds ], true);
		this.window.refresh;	// a better way to do this than refreshing the whole parent view?
	}
	visible {
		^scnsobj.invoke("isHidden") == 0;
	}
	visible_ { arg bool;
		scnsobj.invoke("setHidden:", [ bool.not ], true);
	}
	
	canFocus { ^false }
	canFocus_ { arg bool; }
	focus { arg flag=true; }
	hasFocus{ ^false }

	refresh {
		scnsobj.invoke("setNeedsDisplay:", [ true ], true);
		this.window.refresh;	// a better way to do this than refreshing the whole parent view?
	}

	refreshInRect { arg rect;
		scnsobj.invoke("setNeedsDisplayInRect:", [ rect ], true);
	}
	
	isClosed { ^scnsobj.dataptr.isNil }
	notClosed { ^scnsobj.dataptr.notNil }
	remove {
	/*
		// remove from superview blah blah... to do
		if(scnsobj.dataptr.notNil,{
			parent.prRemoveChild(this);
			this.prClose;
		},{
			"AppKitView:remove - this object already removed.".debug(this);
		});
	*/
	}
	
	background_ { arg color; }
	
	addAction { arg func, selector=\action;
		this.perform(selector.asSetter, this.perform(selector).addFunc(func));
	}
	removeAction { arg func, selector=\action;
		this.perform(selector.asSetter, this.perform(selector).removeFunc(func));
	}

	// get the view parent tree up to the SCTopView
	getParents {
		var parents, view;
		view = this;
		parents = List.new;
		while({(view = view.parent).notNil},{ parents.add(view)});
		^parents
	}
	
	absoluteBounds {
		var rect;
		rect = this.bounds.copy;
		
	//	if (parent.relativeOrigin) {
			rect.left = bounds.left - parent.absoluteBounds.left;
			rect.top = bounds.top - parent.absoluteBounds.top;
	//	};

		^rect
	}
	
	// this rerains the cell object
	// common for most control operations
	// the cell will be released with the object
	cell {
		if (cell.isNil) {
			^cell = scnsobj.invoke("cell");
		};
		^cell
	}

	// convenience methods - some views/controls may not respond to these
	enabled {
		^this.invoke("isEnabled") == 1
	}
	enabled_ { arg bool;
		this.invoke("setEnabled:", [bool], true);
	}
	setFont { arg fontName, fontSize;
		var font;

		font = SCNSObject("NSFont", "fontWithName:size:", [fontName, fontSize]);
		scnsobj.invoke("setFont:", [font], true);
		font.release;
	}
	// convenience - action manager. see addAction and removeAction
	initAction { arg actionName = "doFloatAction:";
		var cocoaAction = scnsobj.initAction(actionName);
		^cocoaAction.action = { arg v, val;
			this.action.value(this, val);
		};
	}
	// do like a SCNSObject
	invoke { arg method, args, defer=false;
		^scnsobj.invoke(method, args, defer);
	}
	// private
	// forward unknown messages to the contained SCNSObject
	doesNotUnderstand { arg selector ... args;
		var isSetter, shouldDefer;
		isSetter = selector.isSetter;
		selector = selector.asString.tr($_, $:).asSymbol;
		if (isSetter) {
			if (args.size == 1) {
				shouldDefer = true;
			}{
				shouldDefer = args.pop ? true;
			};
		}{
			shouldDefer = args.pop ? false;
		}
		//	[isSetter, selector, args, shouldDefer].postln;
		// objc may return a new obj type on setters so always return the result
		^scnsobj.invoke(selector, args, shouldDefer);
	}
	prSetSCNSObject { arg newobject;
		scnsobj = newobject
	}
	prClose {
		scnsobj.release;
		if (cell.notNil) { cell.release; cell=nil; };
		onClose.value(this);
	}
}

+ SCNSObjectAbstract {
	// convenience methods. note some views/controls will not respond to these
	enabled {
		^this.invoke("isEnabled").ascii == 1
	}
	enabled_ { arg bool;
		this.invoke("setEnabled:", [bool], true);
	}
	// handy
	setFont { arg fontName, fontSize;
		var font;

		font = SCNSObject("NSFont", "fontWithName:size:", [fontName, fontSize]);
		this.invoke("setFont:", [font], true);
		font.release;
	}
	// forward unknown selectors as this object NS methods
	// invoke IS still available
	doesNotUnderstand { arg selector ... args;
		var isSetter, shouldDefer;
		isSetter = selector.isSetter;
		selector = selector.asString.tr($_, $:).asSymbol;
		if (isSetter) {
			if (args.size == 1) {
				shouldDefer = true;
			}{
				shouldDefer = args.pop ? true;
			};
		}{
			shouldDefer = args.pop ? false;
		}
		//	[isSetter, selector, args, shouldDefer].postln;
		// objc may return a new obj type on setters so always return the result
		^this.invoke(selector, args, shouldDefer);
	}
}

