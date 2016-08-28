
LNX_EmptyGUIShell {

	// this is a fake gui that stops SC from going fatal when
	// a window is closed but its gui is updated

	*isClosed		{ ^true }
	*value			{ ^nil  }
	*value_			{ ^this }
	*valueAction_	{ ^this }
	*valueAction2_	{ ^this }
	*enabled		{ ^false}
	*enabled_		{ ^this }
	*prClose		{ ^this }
	*prRemove		{ ^this }
	*items			{ ^nil  }
	*items_			{ ^this }
	*velocityOn		{ ^nil  }
	*dividerView	{ ^this }
	*action			{ ^this }
	*midiSet		{ ^this }
	*refresh		{ ^this }
	*set			{ ^this }
	*env_			{ ^this }
	*string_		{ ^this }
	*draw       	{ ^this }
	*on         	{ ^this }
	*off        	{ ^this }
	*indexToSelection_{ ^this }
	*refreshToSelection{ "EmptyShell called"; ^this }
	*controlID_{|id,controlGroup,name|
		controlGroup.register(id,this,name); // to stop nil errors in midi control
		^this
	}
	*draw_{}
	*color_{}
	*automation{^nil}
}
