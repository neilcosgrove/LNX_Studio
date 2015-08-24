+ Gradient {
	fill { |rect|
		if( direction == \h )
			{ ^Pen.fillAxialGradient( rect.left@rect.center.y, rect.right@rect.center.y,
				color1, color2 ); }
			{ ^Pen.fillAxialGradient( rect.center.x@rect.top, rect.center.x@rect.bottom,
				color1, color2 ); };
		}
		
	penFill { |rect|
		if( direction == \h )
			{ ^Pen.fillAxialGradient( rect.left@rect.center.y, rect.right@rect.center.y,
				color1, color2 ); }
			{ ^Pen.fillAxialGradient( rect.center.x@rect.top, rect.center.x@rect.bottom,
				color1, color2 ); };
		}
	}
	
+ Color {
	fill { Pen.fillColor = this; ^Pen.fill; }
	
	penFill { Pen.fillColor = this; ^Pen.fill; }
	}
	
+ Function {
	penFill { |rect| 
		Pen.use({ 
			Pen.clip; 
			this.value( rect );
		}) 
	}
}

+ Symbol {
	penFill { |rect|
		Pen.use({ 
			Pen.clip; 
			DrawIcon.symbolArgs( this, rect );
		}) 
	}
}