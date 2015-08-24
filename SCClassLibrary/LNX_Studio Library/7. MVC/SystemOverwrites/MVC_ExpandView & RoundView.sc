
+ ExpandView {
	expand { 
		//if( composite.bounds != bigBounds ) {
			expandAction.value( this );
			this.changeSize( bigBounds );
			button.value = 1;
		//}
	}
	
}


// everything is relative these days

+ RoundButton {
		
	mouseDown {
		arg x, y, modifiers, buttonNumber, clickCount;
		if (modifiers.asBinaryDigits[4]==0) { // check apple not pressed because of drag
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);			pressed = true; this.refresh;
		};
	}
		
}