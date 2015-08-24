
NSButton : AppKitView {
	*new { arg parent, bounds;
		^super.new(parent, "NSButton", "initWithFrame:", bounds);
	}
}

NSSlider : AppKitView {
	*new { arg parent, bounds;
		^super.new(parent, "NSSlider", "initWithFrame:", bounds);
	}
}

NSTextField : AppKitView {
	*new { arg parent, bounds;
		^super.new(parent, "NSTextField", "initWithFrame:", bounds);
	}
}

NSPopUpButton : AppKitView {
	*new { arg parent, bounds, pullsdown=false;
		^super.new(parent, "NSPopUpButton", "initWithFrame:pullsDown:", bounds, pullsdown);
	}
	items_ { arg items;
		items.do({ arg item;
			this.addItemWithTitle_(item);
		});
	}
}

PullDownButton : NSPopUpButton {
	*new { arg parent, bounds;
		^super.new(parent, bounds, true);
	}
}

NSSegmentedControl : AppKitView {
	*new { arg parent, bounds;
		^super.new(parent, "NSSegmentedControl", "initWithFrame:", bounds);
	}
}

NSProgressIndicator : AppKitView {
	*new { arg parent, bounds;
		^super.new(parent, "NSProgressIndicator", "initWithFrame:", bounds);
	}
}

NSButtonCell : SCNSObject {
	*new { arg title;
		^super.new("NSButtonCell", "initTextCell:", [title]);
	}
}

NSMatrix : AppKitView {
	*new { arg parent, bounds, title, rows=4, cols=1;
		var btnCell, obj;
		btnCell = NSButtonCell.new( "Radio Btns" );
		btnCell.setControlSize_(1, false);
		btnCell.setButtonType_(4, false);
		btnCell.setBezelStyle_(2, false);
		
		//	typedef enum _NSMatrixMode {
		//	   NSRadioModeMatrix     = 0,
		//	   NSHighlightModeMatrix = 1,
		//	   NSListModeMatrix      = 2,
		//	   NSTrackModeMatrix     = 3
		//	} NSMatrixMode;
		obj = super.new(parent, "NSMatrix",
		"initWithFrame:mode:prototype:numberOfRows:numberOfColumns:",
		bounds, 0, btnCell, rows, cols);
		
		btnCell.release;
		
		^obj
	}
	strings_ { arg strings;
		strings.do({ arg caption, row;
			var cell;
			cell = this.cellAtRow_column_(row,0,false);
			cell.setTitle_(caption);
			cell.release;
		});
	}
}

NSTableView : SCNSObject {
	*new { arg bounds;
		^super.new("NSTableView", "initWithFrame:", [ bounds ]);
	}
}

NSTableColumn : SCNSObject {
	*new { arg id;
		^super.new("NSTableColumn", "initWithIdentifier:", [ id ] );
	}
}

NSScrollView : AppKitView {
	*new { arg parent, bounds;
		^super.new(parent, "NSScrollView", "initWithFrame:", bounds);
	}
}

NSBox : AppKitView {
	var <children, <>decorator;
	*new { arg parent, bounds;
		^super.new(parent, "NSBox", "initWithFrame:", bounds);
	}
	add { arg child;
		children = children.add(child);
		if (decorator.notNil, { decorator.place(child); });
		this.addSubview_( child.scnsobj );
	}
}

