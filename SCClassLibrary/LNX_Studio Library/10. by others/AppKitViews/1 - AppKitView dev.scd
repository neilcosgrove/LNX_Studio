
// NSButton
(
	w = SCWindow.new("Window", Rect(100,550,350,250)).front;
	p = AppKitView.new(w, "NSButton", "initWithFrame:", Rect(100,50,100,24));
)
SCNSObject.dumpPool;

p.setTitle = "OK";
p.setBezelStyle_(1);	// NSRoundedBezelStyle

p.initAction("doAction:");
SCNSObject.dumpPool;
p.action = { arg view, val; [view, val].postln; } // click the  btn

f  = { "woohooo!".postln; };
p.addAction(f);		// click the  btn

p.removeAction(f);		// click the  btn

p.visible
p.visible = false
p.visible = true

p.enabled
p.enabled = false
p.enabled = true

p.isClosed
w.close;
p.isClosed

SCNSObject.dumpPool;

// decorator
(
	w = SCWindow.new("Window", Rect(100,550,320,250)).front;
	w.view.decorator = FlowLayout.new(w.view.bounds); // <--- decorator!

	p = Array.fill(24, { arg i;
		var btn;
		btn = AppKitView.new(w, "NSButton", "initWithFrame:", 100@24);
		btn.setBezelStyle_(10);
		btn.setTitle_("Button" + (i + 1));
	});
)
SCNSObject.dumpPool;

w.close;
SCNSObject.dumpPool;

//
(
	w = SCWindow.new("Window", Rect(200,550,320,170)).front;
	p = AppKitView.new(w, "NSButton", "initWithFrame:", Rect(100,50,100,24));
	p.setBezelStyle_(1);
	p.initAction("doAction:");
	p.action = { arg view, val; [view, val, p.state].postln; }
)
p.setTitle = "Hello!";

p.bounds
p.setBezelStyle_(3);	// NSThickSquareBezelStyle
p.bounds_(Rect(50,20,150,120))
p.state

p.setBezelStyle_(8);	// NSTexturedSquareBezelStyle

p.cell.className

p.cell.controlSize
p.cell.setControlSize = 1
p.cell.setControlSize = 2
p.cell.setControlSize = 0
p.bounds = Rect(20,20,120,40);
p.bezelStyle

// setBezelStyle
p.setBezelStyle_(0);	// 
p.setBezelStyle_(1);	// NSRoundedBezelStyle
p.cell.setControlSize = 1
p.cell.setControlSize = 2
p.cell.setControlSize = 0

p.setBezelStyle_(2);	// NSRegularSquareBezelStyle
p.setBezelStyle_(3);	// NSThickSquareBezelStyle
p.setBezelStyle_(4);	// NSThickerSquareBezelStyle

p.setTitle_("");
p.setBezelStyle_(5);	// NSDisclosureBezelStyle - click on the button and check its state
p.setButtonType_(1);	// NSPushOnPushOffButton
p.state
p.state

p.setBezelStyle_(6);	// NSShadowlessSquareBezelStyle
p.setTitle_("Hello!");
p.state

p.setButtonType_(7);	// NSMomentaryPushInButton

p.setTitle_("A");
p.setBezelStyle_(7);	// NSCircularBezelStyle
p.cell.setControlSize = 1
p.cell.setControlSize = 2
p.cell.setControlSize = 0

p.setBezelStyle_(8);	// NSTexturedSquareBezelStyle
p.setTitle_("Hello!");
p.cell.setControlSize = 0
p.cell.setControlSize = 1
p.cell.setControlSize = 2

p.setTitle_("");
p.setBezelStyle_(9);	// NSHelpButtonBezelStyle

p.setBezelStyle_(10);	// NSSmallSquareBezelStyle
p.setTitle_("Hello!");
p.setBezelStyle_(11);	// NSTexturedRoundedBezelStyle
p.setBezelStyle_(12);	// NSRoundRectBezelStyle
p.setBezelStyle_(13);	// NSRecessedBezelStyle
p.setBezelStyle_(15);	// NSRoundedDisclosureBezelStyle

p.setBezelStyle_(3);	// NSThickSquareBezelStyle

// setButtonType
p.setButtonType_(0);	// NSMomentaryLightButton
p.setButtonType_(1);	// NSPushOnPushOffButton

p.state

p.setButtonType_(2);	// NSToggleButton
p.setButtonType_(3);	// NSSwitchButton
p.setButtonType_(4);	// NSRadioButton
p.setButtonType_(5);	// NSMomentaryChangeButton
p.setButtonType_(6);	// NSOnOffButton
p.setButtonType_(7);	// NSMomentaryPushInButton
p.setButtonType_(0);	// NSMomentaryPushButton
p.setButtonType_(7);	// NSMomentaryLight

w.bounds

w.close;
SCNSObject.dumpPool;

// NSSlider
(
	w = SCWindow.new("Window", Rect(250,550,550,250)).front;
	w.view.decorator = FlowLayout.new(w.view.bounds);

	p = AppKitView.new(w, "NSSlider", "initWithFrame:", Rect(100,70,250,20));
	q = AppKitView.new(w, "NSSlider", "initWithFrame:", Rect(360,50,20,100));
)
p.initAction;
p.action = { arg view, val; [view, val].postln; }

p.cell.setControlSize = 2
p.cell.setControlSize = 1
p.setFloatValue = 0.25
p.floatValue
p.doubleValue

p.cell.controlSize
p.cell.controlTint 
p.cell.setControlTint_(6); w.front;

p.numberOfTickMarks
p.setNumberOfTickMarks_(11)
p.tickMarkPosition
p.setTickMarkPosition = 1
p.setTickMarkPosition = 0
p.setNumberOfTickMarks = 0

w.close;
SCNSObject.dumpPool;

// NSTextField
(
	w = SCWindow.new("Window", Rect(10,400,550,250)).front;
	w.view.decorator = FlowLayout.new(w.view.bounds);

	p = AppKitView.new(w, "NSTextField", "initWithFrame:", Rect(100,50,200,22));
	//p.invoke("setFocusRingType:", [ 1 ]); // NSFocusRingTypeNone
	// or... p.setFocusRingType_( 1 );
	p.cell.setTitle = "Hello, World!";
	p.initAction("doAction:");
	p.action = { arg view, val; [view, p.cell.title].postln; }

)
p.cell.className
p.cell.setTitle = "Hello!";

p.alignment;

p.setAlignment = 1;
p.setAlignment = 2;
p.setAlignment = 0;

w.close;
SCNSObject.dumpPool;


// NSPopUpButton
(
	w = SCWindow.new("Window", Rect(100,600,350,100)).front;
	w.view.decorator = FlowLayout.new(w.view.bounds);

	p = AppKitView.new(w, "NSPopUpButton", "initWithFrame:", Rect(100,50,100,22));
	
	["Zero", "One", "Two", "Three", "Four", "Five", "Six"].do({ arg title;
		p.addItemWithTitle_(title);
	});
)
p.cell.setControlSize_(1)
p.cell.setFont("Lucida Grande", 11); // convenience method

SCNSObject.dumpPool;

p.initAction("doAction:");
p.action = { arg view, val; [view, view.titleOfSelectedItem ].postln };

SCNSObject.dumpPool;

p.indexOfSelectedItem

p.selectItemWithTitle_("asd"); // none
p.indexOfSelectedItem

p.selectItemWithTitle_("One");
p.indexOfSelectedItem

SCSlider.new(w, Rect(20,50,200,16));
SCSlider.new(w, Rect(20,70,200,16));

w.close
SCNSObject.dumpPool;


// PullDownButton
(
	w = SCWindow.new("Window", Rect(10,400,550,250)).front;
	w.view.decorator = FlowLayout.new(w.view.bounds);

	p = AppKitView.new(w, "NSPopUpButton", 
		"initWithFrame:pullsDown:", Rect(100,50,150,22), true);
	
	["PullDownButton", "One", "Two", "Three", "Four", "Five", "Six"].do({ arg title;
		p.addItemWithTitle_(title);
	});

	p.initAction("doAction:");
	p.action = { arg view, val; [view, view.titleOfSelectedItem ].postln };
)

p.visible
p.visible = false
p.visible = true

p.enabled
p.enabled = false
p.enabled = true

SCNSObject.dumpPool;

w.close
SCNSObject.dumpPool;

// in SCCompositeView
(
	w = SCWindow.new("Window", Rect(10,400,550,250)).front;
	w.view.decorator = FlowLayout.new(w.view.bounds);
	
	c = SCCompositeView.new(w, Rect(20,5,250,200))
		.background_(Color.blue(0.1,0.1));
		
	c.decorator = FlowLayout.new(c.bounds);

	p = AppKitView.new(c, "NSPopUpButton", "initWithFrame:", Rect(100,50,100,22));
	
	["Zero", "One", "Two", "Three", "Four", "Five", "Six"].do({ arg title;
		p.addItemWithTitle_(title);
	});

	SCSlider.new(c, Rect(50,50,150,16));

)

w.close;

// NSSegmentedControl
(
	w = SCWindow.new("Window", Rect(200,500,550,250)).front;
//	w.view.decorator = FlowLayout.new(w.view.bounds);

	p = AppKitView.new(w, "NSSegmentedControl", "initWithFrame:", Rect(10,10,210,20));
	p.cell.setControlSize_(1);
	p.cell.setFont("Lucida Grande", 11);
	p.setSegmentCount_(4);
	["One", "Two", "Three", "Four"].do { arg label, segment;
		p.setWidth_forSegment_(50, segment, true);
		p.setLabel_forSegment_(label, segment, true);
	};

)

p.selectedSegment
p.setSelectedSegment_(3);
p.setSelectedSegment_(1);

SCNSObject.dumpPool;

// NSMenu for a control
m = SCNSObject("NSMenu", "initWithTitle:", ["My Menu"]);
m.autoenablesItems == 1;
m.setAutoenablesItems_( false );

i = SCNSObject("NSMenuItem", "init");
i.setTitle_( "Item 1" );
i.initAction("doAction:");
i.nsAction.action = { i.setState_(1); j.setState_(0); "eep!".postln; };

m.addItem_( i );

j = SCNSObject("NSMenuItem", "init");
j.setTitle_("Item 2");
j.initAction("doAction:");
j.nsAction.action = { i.setState_(0); j.setState_(1); "eep 2!".postln; };

m.addItem_(j);

p.setMenu_forSegment_(m, 0, true);

// control action - will have to press the mouse btn longer for the menu to pop up
p.initAction("doAction:");
p.action = { arg view, val; [view, p.selectedSegment ].postln };

SCNSObject.dumpPool;

(
	w.close;
	[m, i, j].do(_.release);
)

SCNSObject.dumpPool;

// NSProgressIndicator
(
	w = SCWindow.new("Window", Rect(100,650,410,40)).front;
	w.view.decorator = FlowLayout.new(w.view.bounds);

	p = AppKitView.new(w, "NSProgressIndicator", "initWithFrame:", Rect(100,50,400,16));
	
)

p.startAnimation_(nil); w.front;
p.stopAnimation_(nil);

p.isIndeterminate == 1
p.setIndeterminate = false;
p.setIndeterminate = true;

p.setIndeterminate = false;
p.minValue
p.maxValue
p.setDoubleValue = 25;
(
Routine({
	w.front;
	p.setDoubleValue = 0;
	100.do {
		p.incrementBy_(1);
		0.03.wait;
	};
	\done.postln
}).play(AppClock);
)

p.size(20,20)
p.sizeToFit
p.style
p.setStyle = 1
p.setControlSize = 1
p.refresh

p.startAnimation_(nil);
p.animationDelay
p.setAnimationDelay = 0.1
p.stopAnimation_(nil);

SCNSObject.dumpPool;

w.close;
SCNSObject.dumpPool;


// in Sheet
(
Sheet({ arg layout;
	l = layout;
//	layout.flow({ arg layout;	// uncomment these to test relativeOrigin and nesting containers
//	layout.flow({ arg layout;	// of AppKitView objects
//	layout.flow({ arg layout;
		p =  AppKitView.new(layout, "NSPopUpButton", "initWithFrame:", Rect(10,10,150,22));
		p.cell.setControlSize = 1;
		p.cell.setFont("Lucida Grande", 11);
		["Zero", "One", "Two", "Three", "Four", "Five", "Six"].do({ arg title;
			p.addItemWithTitle_(title);
		});
		p.initAction("doAction:");
		p.action = { arg view, val; [view, view.titleOfSelectedItem ].postln };
		
		q = AppKitView.new(layout, "NSPopUpButton", "initWithFrame:pullsDown:", Rect(10,10,150,16), true);
		q.cell.setControlSize = 2;
		q.cell.setFont("Lucida Grande", 9);
		["PullDownButton", "One...", "Two...", "Three", "Four", "Five", "Six"].do({ arg title;
			q.addItemWithTitle_(title);
		});
		m = q.menu(false);
		q.onClose = { m.release };
		q.initAction("doAction:");
		q.action = { arg view, val;
			var index, menuItem;
			
			(3..6).do { arg n;
				menuItem = m.itemAtIndex_(n, false);
				menuItem.setState_(0);
				menuItem.release;
			};
			
			index = view.indexOfSelectedItem;
			menuItem = m.itemAtIndex_(index, false);
			if (index > 2) { menuItem.setState_(1); };
			menuItem.release;
			[view, view.titleOfSelectedItem ].postln
		};

		layout.startRow;
		
		t = AppKitView.new(layout, "NSTextField", "initWithFrame:", Rect(100,50,300,20));
		t.cell.setTitle = "Hello, World!";
		t.initAction("doAction:");
		t.action = { arg view, val; [view, view.cell.title].postln; };
	
		layout.startRow;

		h = AppKitView.new(layout, "NSTextField", "initWithFrame:", Rect(100,50,300,20));
		h.cell.setTitle_("Hello, again!");
		h.initAction("doAction:");

		layout.startRow;

		SCUserView.new(layout, Rect(10,10,300,18))
			.backgroundImage_( SCImage.new( "/Library/Desktop Pictures/Ripples Blue.jpg" ) );

		layout.startRow;

		j = AppKitView.new(layout, "NSSlider", "initWithFrame:", Rect(100,50,300,18))
			.cell.setControlSize_(1);

		layout.startRow;
		b = AppKitView.new(layout, "NSButton", "initWithFrame:", Rect(100,50,100,24));
		b.setTitle_("Hello!");
		b.initAction("doAction:");
		b.action_({ if (b.state == 1) { i.startAnimation_(nil); }{ i.stopAnimation_(nil); } });
		b.setBezelStyle_(6);	// NSShadowlessSquareBezelStyle
		b.setButtonType_(1);

		i = AppKitView.new(layout, "NSProgressIndicator", "initWithFrame:", Rect(10,50,196,18));


		layout.background_(Color.blue(0.1,0.1));
//	});
//	});
//	});
	
	layout.background_(Color.blue(0.1,0.1));
}, "NSControls");
)

l.view.children;

l.view.showViewAction = { arg view, show; [ view, show ].postln; };
(
	l.view.showViewAction = { arg view, show;
		view.children.do({ arg obj;
			if( obj.isKindOf(AppKitView) ) {
				obj.visible_(show);
			};
		});
	};
)

p.visible;

l.view.visible;
l.view.visible = false;
l.view.visible = true;

SCView:visible

SCNSObject.dumpPool;

// NSTableView
(
	w = SCWindow.new("NSTableView", Rect(200,500,400,220));
	w.onClose_({ t.release; c.release; });

	t = SCNSObject("NSTableView", "initWithFrame:", [ Rect(0,0,380,200) ]);
	t.setUsesAlternatingRowBackgroundColors_(true);
	t.setFocusRingType_( 1 ); //NSFocusRingTypeNone

	v = AppKitView(w, "NSScrollView", "initWithFrame:", Rect(10,10,380,200));
	v.setHasVerticalScroller_( true, false );
	v.setHasHorizontalScroller_( true, false );
	v.setAutoresizingMask_(18);
	
	k = v.verticalScroller(false);
	k.setControlSize_(1, true);
	k.release;

	l = v.horizontalScroller(false);
	l.setControlSize_(1, true);
	l.release;
	
	v.setDocumentView_(t);

	c = SCNSObject("NSTableColumn", "initWithIdentifier:", [ "column_1" ] );
	c.setEditable_( true );
	c.setWidth_( 200 );
	c.setResizingMask_( 2 );

	h = c.headerCell(false);
	h.setStringValue_("Items" , true);
	h.setAlignment_(2, true);
	h.release;
	
	t.addTableColumn_(c, true);

	w.front;
)

(
	// set data source
	i = ["Zero", "One", "Two", "Three", "Four", "Five", "Six"];
	t.initAction("doAction:");
	t.setDelegate;
	
	t.nsDelegate.addMethod("tableView:objectValueForTableColumn:row:", "@", "@@i",
	{ arg method, args;
	//	[method, args].postln;
		
		^i[ args[2] ]
	});
	
	t.nsDelegate.addMethod("numberOfRowsInTableView:", "i", "@",
	{ arg method, args;
	//	[method, args].postln;
		
		^i.size
	});
	
	t.setDataSource_(t.nsDelegate);
)


t.reloadData(true);

SCNSObject.dumpPool;

w.close;
SCNSObject.dumpPool;


// NSMatrix - todo
(
	w = SCWindow.new("Window", Rect(100,650,300,200)).front;
//	w.view.decorator = FlowLayout.new(w.view.bounds);

	q = SCNSObject.new("NSButtonCell", "initTextCell:", ["Radio Btns"]);
	q.setControlSize_(1, false);
	q.setButtonType_(4, false);
	q.setBezelStyle_(2, false);

	/*
		typedef enum _NSMatrixMode {
		   NSRadioModeMatrix     = 0,
		   NSHighlightModeMatrix = 1,
		   NSListModeMatrix      = 2,
		   NSTrackModeMatrix     = 3
		} NSMatrixMode;
	*/
	m = AppKitView.new(w, "NSMatrix",
		"initWithFrame:mode:prototype:numberOfRows:numberOfColumns:",
		Rect(100,50,150,150), 0, q, 5, 1);
	q.release;
	
	["One", "Two", "Three", "Four", "Five"].do({ arg caption, row;
		var cell;
		cell = m.cellAtRow_column_(row,0,false);
		cell.setTitle_(caption);
		cell.release;
	});
)

m.initAction("doAction:");
m.action = { arg view; m.postln; }

c = m.invoke("cellAtRow:column:",[0,0],false);
c.setTitle_("One");

c = m.cellAtRow_column_(0,0,false); // check doesNotUndestand redirection
c.setTitle_("asd");
		
w.close;

SCNSObject.dumpPool;

SCNSObject.freePool;

