
// UserView

MVC_UserView{

	var <window, bounds, <view;
	var <canFocus=false, <focusColor, <drawFunc, <mouseOverAction;
	var <mouseDownAction, <mouseMoveAction, <mouseUpAction, <keyDownAction, <keyUpAction;
	var <resize=5;
	var <rect;
	var <>clearOnRefresh=true;
	var <>onClose;
	var <beginDragAction, <canReceiveDragHandler, <receiveDragHandler;


	*new {|...args| ^super.new.init(*args) }

	init {|...args|
	
		bounds  = args.findKindOf(Rect);
		rect = bounds;
		window  = args.findKindOf(MVC_Window)
				?? {args.findKindOf(MVC_TabbedView)}
				?? {args.findKindOf(MVC_TabView)}
				?? {args.findKindOf(MVC_ScrollView)}
				?? {args.findKindOf(MVC_CompositeView)};

		// will need to take care of MVC_TabbedViews here
		if (window.notNil) {
			// register this MVC_item with the view
			if (window.isKindOf(MVC_TabbedView)) {
				// get index to the tabbed view from supplied args index +1
				window.addView(args[args.indexOf(window)+1],this);
			}{
				window.addView(this);	// add so this view can be created with the MVC_ScrollView
			};
			if (window.isClosed) {
				window=nil;	// MVC_ScrollView is not a real view so don't use as a window
			}{
				window = window.view;	
			};
		}{
			window=args.findKindOf(Window)
					?? { args.findKindOf(ScrollView)}
					?? {args.findKindOf(CompositeView)};
		};
		
		// and make it if in a standard window
		if (window.notNil) { this.create(window) }
	}

	create{|argParent|
		//if (view.isClosed) {
			window=argParent;
			this.createView;
	//	}{
	//		"View already exists.".warn;
	//	}
	}
	
	createView{
		view=UserView(window,rect)
			.canFocus_(canFocus)
			.drawFunc_(drawFunc)
			.mouseDownAction_(mouseDownAction)
			.mouseMoveAction_(mouseMoveAction)
			.mouseUpAction_(mouseUpAction)
			.mouseOverAction_(mouseOverAction) 
			.keyDownAction_(keyDownAction)
			.keyUpAction_(keyUpAction)
			.resize_(resize)
			.clearOnRefresh_(clearOnRefresh)
			.beginDragAction_(beginDragAction)
			.canReceiveDragHandler_(canReceiveDragHandler)
			.receiveDragHandler_(receiveDragHandler)
			.onClose_({|me|
				onClose.value(this);
				me.drawFunc_(nil)
					.mouseDownAction_(nil)
					.mouseMoveAction_(nil)
					.mouseUpAction_  (nil)
					.keyDownAction_  (nil)
					.onClose_        (nil);
			});
					
		if (focusColor.notNil){ view.focusColor_(focusColor) };
	}
	
	// what i send
	beginDragAction_{|func|
		beginDragAction=func;
		if (view.notClosed) { view.beginDragAction_(func) }
	}
	
	// what can i recieve
	canReceiveDragHandler_{|func|
		canReceiveDragHandler=func;
		if (view.notClosed) { view.canReceiveDragHandler_(func) }
	}
	
	// what i get passed
	receiveDragHandler_{|func|
		receiveDragHandler=func;
		if (view.notClosed) { view.receiveDragHandler_(func) }
	}
	
	drawFunc_{|func|
		drawFunc=func;
		if (view.notClosed) { view.drawFunc_(drawFunc).refresh}; // does this need refresh ?
	}
	
	mouseDownAction_{|func|
		mouseDownAction=func;
		if (view.notClosed) { view.mouseDownAction_(mouseDownAction) };
	}
	
	mouseMoveAction_{|func|
		mouseMoveAction=func;
		if (view.notClosed) { view.mouseMoveAction_(mouseMoveAction) };
	}
	
	mouseUpAction_{|func|
		mouseUpAction=func;
		if (view.notClosed) { view.mouseUpAction_(mouseUpAction) };
	}
	
	mouseOverAction_{|func|
		mouseOverAction=func;
		if (view.notClosed) { view.mouseOverAction_(mouseOverAction) };
	}
	
	keyDownAction_{|func|
		keyDownAction=func;
		if (view.notClosed) { view.keyDownAction_(keyDownAction) };
	}

	keyUpAction_{|func|
		keyUpAction=func;
		if (view.notClosed) { view.keyUpAction_(keyUpAction) };
	}
	
	// user can focus on item
	canFocus_{|bool|
		canFocus=bool;
		if (view.notClosed){ view.canFocus_(canFocus) }
	}
	
	// set focus to this item
	focus{ if (view.notClosed) { view.focus } }
	
	focusColor_{|color|
		focusColor=color;
		if (view.notClosed){ view.focusColor_(focusColor) }
	}
	
	refresh{ if (view.notClosed) { view.refresh } }
	
	bounds_{|argRect|
		bounds=argRect;
		rect=bounds;
		if (view.notClosed) { view.bounds_(rect) }
	}
	
	bounds{^rect}
	
	ref{"qwe".postln;}
	
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

}


/* TESTING

(
	w=MVC_Window.new;
		
	v=MVC_UserView(w, Rect(10,10,300,300));
	v.drawFunc={|v|
			Pen.use{
				1000.do{
					Color.red(rrand(0.0, 1), rrand(0.0, 0.5)).set;
					Pen.moveTo((v.bounds.width.rand)@(v.bounds.height.rand));
					Pen.lineTo((v.bounds.width.rand)@(v.bounds.height.rand));
					Pen.stroke;
				}
			};
		};
	v.mouseDownAction={v.refresh};
w.create;
)

w.close;

w.open;

*/ 