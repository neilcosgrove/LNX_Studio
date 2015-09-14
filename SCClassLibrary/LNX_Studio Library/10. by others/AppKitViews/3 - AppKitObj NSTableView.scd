
(
	w = Window.new("NSTableView", Rect(200,500,400,220));
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

/*
	h = c.headerCell(false);
	h.setStringValue_("Items");
	h.setAlignment_( 2 );
	h.release;
*/
	h = c.headerCell(false);
	h.setStringValue_("Items" , true);
	h.setAlignment_(2, true);
	h.release;

	
	t.addTableColumn_(c, true);

	w.front;
)

(
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

SCNSObject.freePool;



/* Definitions of filer types

#define _C_ID		'@'
#define _C_CLASS	'#'
#define _C_SEL	':'
#define _C_CHR	'c'
#define _C_UCHR	'C'
#define _C_SHT	's'
#define _C_USHT	'S'
#define _C_INT	'i'
#define _C_UINT	'I'
#define _C_LNG	'l'
#define _C_ULNG	'L'
#define _C_FLT	'f'
#define _C_DBL	'd'
#define _C_BFLD	'b'
#define _C_VOID	'v'
#define _C_UNDEF	'?'
#define _C_PTR	'^'
#define _C_CHARPTR	'*'
#define _C_ARY_B	'['
#define _C_ARY_E	']'
#define _C_UNION_B	'('
#define _C_UNION_E	')'
#define _C_STRUCT_B	'{'
#define _C_STRUCT_E	'}'

*/