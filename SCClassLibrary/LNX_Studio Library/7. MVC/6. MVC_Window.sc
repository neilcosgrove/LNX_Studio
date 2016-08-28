
// Window

MVC_Window {

	classvar windows, <frontWindow;

	var <view, bounds, <name, <resizable, <>border, <>scroll;
	var <>onClose, <userCanClose=true;
	var <gui, <colors;
	var <alwaysOnTop=false, <hidden=false;
	var <drawHook;
	var <acceptsMouseOver=false;
	var <acceptsClickThrough = true;
	var <>toFrontAction, <>endFrontAction, <missFirstToFront=true;
	var <>resizeAction, <>isFront=false;
	var <>minWidth, <>maxWidth, <>minHeight, <>maxHeight;
	var lastTimeWindowResize, rectToBe, <>resizeWait=0.25;
	var <>keyDownAction;
	var <created=false;
	var <margin, <gap, addFlowLayout=false;
	var suppressTime=0;
	var <>helpAction;
	var <>keepClosed=false;

	var <parentViews;

	*initClass {
		windows = [];
	}

	// new only creates an instance of MVC_Window
	// use .create .open or .front to bring to the front
	*new {|name="mvc_panel", bounds, resizable = true, border = true, server, scroll = false|
		^super.new.init(name, bounds, resizable, border, scroll)
	}

	init {|argName, argBounds, argResizable, argBorder, argScroll|
		windows=windows.add(this);
		name=argName;
		// stops crash on cocoa
		if (argBounds.isKindOf(Rect)) {
			bounds = Rect(argBounds.left.clip(0,Window.screenBounds.width-1),
						argBounds.top.clip(0,Window.screenBounds.height-1),
						argBounds.width, argBounds.height);
		}{
			bounds=Rect(128, 64, 400, 400);
		};

		resizable=argResizable;
		border=argBorder;
		scroll=argScroll;
		colors=IdentityDictionary[];
		gui=[];
		helpAction={true};

		parentViews = [this];

	}

	// add or remove an MVC_View to the view, all views will be created this scrollView

	addView{|view| gui=gui.add(view) }
	removeView{|view| gui.remove(view) }

	// create the gui's items that make this MVC_View
	create{|toFront=true|
		created=true;
		if (view.isClosed) {
			this.createView(toFront);
		}{
			"View already exists.".warn;
		}
	}

	// replacement for endFrontAction_ bug
	looseFront{
		if (isFront) {
			isFront=false;
			endFrontAction.value(this);
		};
	}

	// make the window and all gui items
	createView{|toFront=true|

		var resizeview;

		suppressTime = AppClock.now;

		// the window
		view = Window.new(name++" ",bounds.convert.moveBy(0,24), resizable, border, nil, scroll)
			.userCanClose_(userCanClose)
			.alwaysOnTop_(alwaysOnTop)
			.drawFunc_(drawHook)
			.acceptsMouseOver_(acceptsMouseOver)
			.acceptsClickThrough_(acceptsClickThrough)
			.toFrontAction_{
				frontWindow = this;
				isFront = true;
				bounds = view.bounds.convert;
				if (missFirstToFront) {
					missFirstToFront=false;
				}{
					if(this.isOpenAndHidden) {this.show};
					toFrontAction.value(this);
				};

				windows.do{|window|
					if (window!=this) {window.looseFront}


				}
			}
//			.endFrontAction_{endFrontAction.value(this)}  // this is the apple Y bug, can't fix
			.onClose_{
				resizeview.remove;
				onClose.value(this);
				view.drawFunc_(nil);
				view.toFrontAction_(nil);
				view.view.keyDownAction_(nil);
			}
		;
		if (colors[\background].notNil) {
			//[name, colors[\background]].postln;
			view.view.background_(colors[\background])
		};

		if (addFlowLayout) { view.addFlowLayout(margin, gap) };

		gui.do(_.create(view)); // now make all views inside this view

		if (toFront) {view.front}; // why the delay between the 2 windows ?

		view.view.keyDownAction_{|me, char, modifiers, unicode, keycode, key|
			keyDownAction.value(me, char, modifiers, unicode, keycode, key)
		};

		// used for resize functions. will not work if Rect(0,0,1,1) is off view.
		// So window scrollers will stop this working.
		resizeview = UserView(view,Rect(0,0,1,1))
			.canFocus_(false)
			.drawFunc={
				var oldRect,oldW,oldH,w,h;
				// rect when we start
				oldRect=this.bounds;

				if (bounds.notNil) { // stops free bug

					oldW=w=oldRect.width;
					oldH=h=oldRect.height;
					// clip w&h if we need to
					if ((minWidth.notNil ) and: {w<minWidth})  { w=minWidth  };
					if ((maxWidth.notNil ) and: {w>maxWidth})  { w=maxWidth  };
					if ((minHeight.notNil) and: {h<minHeight}) { h=minHeight };
					if ((maxHeight.notNil) and: {h>maxHeight}) { h=maxHeight };
					if ((oldW!=w)or:{oldH!=h}) {
						// this is now the rect we need to be in resizeWait
						rectToBe=Rect(oldRect.left,oldRect.top,w,h);
						if (lastTimeWindowResize.isNil) {
							lastTimeWindowResize=SystemClock.now;
							{this.autoResize}.defer(resizeWait);   // defer the resize
						}{
							lastTimeWindowResize=SystemClock.now;  // delay the resize
						};
					}{
						// we don't need a resize now
						rectToBe=nil;
						lastTimeWindowResize=nil;
					};
					// do resize action for this view
					if ((AppClock.now-suppressTime)>0.5) {
						resizeAction.value(this);
					};

					// and all gui views
					gui.do{|v|
						if (v.resize!=1) {
							// if their resize isn't 1
							v.doResizeAction(this)
						};
					};
				};
			};
	}

	// autorize window (used in createView method)
	autoResize{
		if (rectToBe.notNil)	{
			if ((SystemClock.now-lastTimeWindowResize)>resizeWait){
				this.bounds_(rectToBe);
				lastTimeWindowResize=nil;
			}{
				{this.autoResize}.defer(SystemClock.now-lastTimeWindowResize);
			}
		}
	}

	// add a flow layout
	addFlowLayout {|argMargin,argGap|
		addFlowLayout=true;
		margin=argMargin;
		gap=argGap;
		if (view.notClosed) {view.addFlowLayout(margin, gap) };
	}

	// open the window
	open{ this.front }

	// and close it
	close{ if (this.notClosed) { view.close } }

	guiClose { if(userCanClose) { this.close} }

	// bring window to front
	front{
		if (keepClosed.not) {
			if (created) {
				if (view.isClosed) {
					this.createView;
				}{
					view.front;
					view.refresh;
				}
			};
		}
	}

	// is closed
	isClosed { ^view.isClosed }

	// is not closed
	notClosed { ^view.isClosed.not }

	// is open
	isOpen { ^this.notClosed }

	// hide window, does close but resizes to zero. this is quick to reopen
	hide{
		if ((view.notNil)and:{view.isClosed.not}and:{hidden.not}){

/*			if (view.currentSheet.notNil) {view.currentSheet.close};

			if ((view.bounds.width>0)and:{view.bounds.height>0}) {
				bounds=view.bounds.convert;
			};
			view.bounds_(Rect(0,0,0,0));*/
			view.visible_(false);
			hidden=true;
		}
	}

	// show a hidden window
	show{
		if (keepClosed.not) {
			if (this.isOpenAndHidden){
/*				this.bounds_(bounds);
				view.alpha_(1);*/
                view.visible_(true);
				hidden=false;
			}
		}
	}

	// is this window open but hidden
	isOpenAndHidden{
		^((view.notNil)and:{view.isClosed.not}and:{hidden})
	}

	// is this window visable i.e. open and not hidden
	isVisible{
		^((view.notNil)and:{view.isClosed.not}and:{hidden.not})
	}

	fullScreen{ if (this.isOpen) { view.fullScreen } }

	endFullScreen{ if (this.isOpen) { view.endFullScreen } }

	// delete this object (1st attempt)
	free{
		this.close;
		// below is broken, causes crash, why?

//		gui.do(_.free);
//		this.remove;
//		{
//		view = bounds = name = resizable = border = scroll = onClose = userCanClose =
//		gui = colors = alwaysOnTop = acceptsClickThrough = hidden =
		drawHook = acceptsMouseOver = toFrontAction = endFrontAction = keyDownAction = nil;

		windows.remove(this);

//		}.defer(0.1);
	}

	// from from window
	remove{
		if (this.notClosed) {
			view.toFrontAction_(nil).endFrontAction_(nil).onClose_(nil);
			view.close;
			view=nil;
		}
	}

	// set the color in the Dictionary
	color_{|index,color...more|
		colors[index]=color;
		if ((index=='background') and:{ this.notClosed}) {
			{view.view.background_(color)}.defer;
		}
	}

	// hides the window, only keeping its representation in the dock, taskbar, etc..
	minimize{ if (this.notClosed) { view.minimize } }

	// restores the window's previous state after being minimized.
	unminimize{if (this.notClosed) { view.unminimize } }

	// refresh the view
	refresh{ if (this.notClosed) {view.refresh} }

	// used by colorTool
	refreshColors{
		if (this.notClosed) {
			view.view.background_(colors[\background])
		}
	}

	// covert from bottom left co-ords to top right co-ords
	convert{|rect|
		this.deprecated(thisMethod, Rect.findRespondingMethodFor(\convert));
		^rect.convert;
	}

	origin{^this.bounds.origin}

	origin_{|pt|
		this.bounds_(this.bounds.origin_(pt))
	}

	// set the bounds
	bounds_{ arg argBounds;
		// stops crash on cocoa
		bounds = Rect(argBounds.left.clip(0,inf), argBounds.top.clip(0,inf), argBounds.width,
			 argBounds.height);
		if (this.notClosed) {
			view.bounds_(bounds.convert);
		};
	}

	// set the bounds with no action
	boundsSuppressResizeAction_{ arg argBounds;
		suppressTime = AppClock.now;
		bounds=argBounds;
		if (this.notClosed) {
			view.bounds_(bounds.convert);
		};
	}

	// get the bounds
	bounds{
		if (created) {
			if (this.isVisible) {
				^view.bounds.convert
			}{
				^bounds
			}
		}{
			^bounds
		}
	}

	// resize window keeping top left corner fixed
	setInnerExtent { arg w,h;
		var b;
		b = this.bounds;
		w = w ? b.width;
		h = h ? b.height;
		this.bounds = Rect(b.left,b.top, w,h);
	}


	// resize window keeping top left corner fixed
	setInnerExtentSuppressResizeAction { arg w,h;
		var b;
		suppressTime = AppClock.now;
		b = this.bounds;
		w = w ? b.width;
		h = h ? b.height;
		this.bounds = Rect(b.left,b.top, w,h);
	}


	// is resizable
	resizable_{|bool|
		if (resizable!=bool) {
			resizable=bool;
			if (this.notClosed) {
				this.close;
				{this.createView;}.defer(0.05);
			};
		};

	}

	// rename the window
	name_{|string|
		name=string;
		if (this.notClosed) { view.name_(name++" ")};
	}

	// user can close the window
	userCanClose_{|bool|
		userCanClose=bool;
		if (this.notClosed) { view.userCanClose_(userCanClose) };
	}

	// window always on top
	alwaysOnTop_{|bool|
		alwaysOnTop=bool;
		if (this.notClosed) { view.alwaysOnTop_(bool) };
	}

	// the draw hook
	drawHook_{|func|
		drawHook=func;
		if (this.notClosed) { view.drawFunc_(func) };
	}

	// accepts mouse over
	acceptsMouseOver_{|bool|
		acceptsMouseOver=bool;
		if (this.notClosed) { view.acceptsMouseOver_(bool) };
	}

	// accepts click through
	acceptsClickThrough_{|bool|
		acceptsClickThrough=bool;
		if (this.notClosed) { view.acceptsClickThrough_(bool) };
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

	a{|...a| this.labelWithNumbers(*a)}

	labelWithNumbers{|snap=1,v|
		var gui=(), ps=[0@0], move, size=16;

		view = v ? view;

		gui[\scrolView] = ScrollView(view,Rect(0,0,view.bounds.width,view.bounds.height));

		gui[\view] = UserView(gui[\scrolView],Rect(0,0,view.bounds.width,view.bounds.height))
			.resize_(5)
			.drawFunc_{|view|
				ps.do{|p,n|
					p=Rect(p.x,p.y,size,size);
					Pen.use{
						Pen.font_(Font("Helvetica",14));
						Pen.width_(4);
						Color.black.set;
						Pen.strokeOval(p);
						Pen.width_(2);
						Color.white.set;
						Pen.strokeOval(p);
						Pen.fillColor_(Color.black);
						Pen.stringCenteredIn((n+1).asString,p.moveBy(-1,-1));
						Pen.stringCenteredIn((n+1).asString,p.moveBy(-1,1));
						Pen.stringCenteredIn((n+1).asString,p.moveBy(1,-1));
						Pen.stringCenteredIn((n+1).asString,p.moveBy(1,1));
						Pen.fillColor_(Color.white);
						Pen.stringCenteredIn((n+1).asString,p);
					}
				}
			}
			.mouseDownAction_{|me, x, y, mod|
				if ([524576,262401,8388864].includes(mod)) {
					ps=ps.add((x-(size/2))@(y-(size/2)));
					move=ps.size-1;
					me.refresh;
				}{
					move=nil;
					ps.do{|p,n|
						p=Rect(p.x,p.y,size,size);
						if (p.contains(x@y)) {move=n}
					};
				}
			}
			.mouseMoveAction_{|me, x, y|
				if (move.notNil) {
					ps[move]=((x-(size/2)).round(snap))@((y-(size/2)).round(snap));
					me.refresh;
				}

			}

	}

}

