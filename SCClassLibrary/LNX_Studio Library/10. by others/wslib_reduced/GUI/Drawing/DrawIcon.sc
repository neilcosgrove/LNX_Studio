// wslib 2006
//
// bank of iconographic Pen-functions

DrawIcon {
	classvar <drawFuncs;
	
	*new { |name, rect ... more|
		rect = (rect ? (32@32)).asRect;
		^drawFuncs[ name ].value( rect, *more ); // returns pen function
		}
		
	*symbolArgs { |name, rect|
		var more;
		more = name.asString.split( $_ );
		name = more[0].asSymbol;
		more = more[1..].collect( _.interpret );
		^this.new( name, rect, *more );
		}
		
	*names { ^drawFuncs.keys }
		
	// use:
	//  DrawIcon( \play, Rect( 20, 20, 30, 30 ) )
	// within a drawfunc this will draw a "play" icon centered in the given rect
	
	*initClass {
		// add more later
		drawFuncs = (
			none: { },
			
			user: { | rect| // user
			
				var square, sl,sh,st,sw,sb,sr, w3,wd;
				
				
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
						
				#sl, st, sw, sh = square.asArray;
				#sr, sb = square.rightBottom.asArray;
				wd = square.width * (1/4);
					
				GUI.pen.fillOval( 
					Rect( sl + (( sw - wd ) / 2), st, wd, wd ).insetBy( wd * -0.5,  
						wd * -0.5   ) );
				
				GUI.pen.width = (1/7) * sw;
		
				GUI.pen.stroke;
				
				GUI.pen.moveTo( (sl + (0.5*sw))@(st + ((1/8) * sh)) );
				GUI.pen.lineTo( (sl + (0.1*sw))@(st + ((4/4) * sh)) );
				GUI.pen.lineTo( (sl + (0.9*sw))@(st + ((4/4) * sh)) );
				
				GUI.pen.fill;

				},
				
			head: { | rect| // the head of a user
			
				var square, sl,sh,st,sw,sb,sr, w3,wd;
				
				
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
						
				#sl, st, sw, sh = square.asArray;
				#sr, sb = square.rightBottom.asArray;
				wd = square.width * (1/4);
					
				GUI.pen.fillOval( 
					Rect( sl + (( sw - wd ) / 2), st, wd, wd ).insetBy( wd * -0.5,  
						wd * -0.5   ) );
				
				GUI.pen.width = (1/7) * sw;
		
				GUI.pen.stroke;

				},
				
			body: { | rect| // the body of a user
			
				var square, sl,sh,st,sw,sb,sr, w3,wd;
				
				
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
						
				#sl, st, sw, sh = square.asArray;
				#sr, sb = square.rightBottom.asArray;
				wd = square.width * (1/4);
				
				GUI.pen.moveTo( (sl + (0.5*sw))@(st + ((1/8) * sh)) );
				GUI.pen.lineTo( (sl + (0.1*sw))@(st + ((4/4) * sh)) );
				GUI.pen.lineTo( (sl + (0.9*sw))@(st + ((4/4) * sh)) );
				
				GUI.pen.fill;

				},
			
			triangle: { |rect, angle = 0, size = 1, width = 1, mode = \fill|
				var radius, center, backCenter;
				radius = (rect.width.min( rect.height ) / 4) * size;
				center = rect.center + Polar( radius * (2/9), angle );
				backCenter =  center + Polar( radius, angle + pi ).asPoint;
				GUI.pen.moveTo( backCenter );
				GUI.pen.lineTo( backCenter + Polar( radius * width, angle + 1.5pi ).asPoint );
				GUI.pen.lineTo( center + Polar( radius, angle ).asPoint );
				GUI.pen.lineTo( backCenter + Polar( radius * width, angle + 0.5pi ).asPoint );
				GUI.pen.lineTo( backCenter );
				GUI.pen.perform( mode );
				},
				
			play:{ |rect, mode = \fill|  // triangle ( > )
				drawFuncs[ \triangle ].value( rect, 0, 1, 1, mode );
				},
				
			back: { |rect, mode = \fill|  // triangle ( < )
				drawFuncs[ \triangle ].value( rect, pi, 1, 1, mode );
				},
								
			up: { | rect, mode = \fill|  // triangle ( ^ )
				drawFuncs[ \triangle ].value( rect, 1.5pi, 1, 1, mode );
				},
				
			down: { | rect, mode = \fill|  // triangle ( v )
				drawFuncs[ \triangle ].value( rect, 0.5pi, 1, 1, mode );
				},
				
			stop: { | rect, mode = \fill| // square
				var square;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
				if( mode == \fill )
					{ GUI.pen.fillRect( square ); }
					{ GUI.pen.strokeRect( square ); };
				},
				
			speaker:	{ | rect, mode = \fill| // square
				var square;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
				
				square = square.insetBy( square.width / 6, 0 );
				square = square.moveBy( square.width / -12, 0 );
	
				GUI.pen.moveTo( square.rightTop );
				GUI.pen.lineTo( square.rightBottom );
				GUI.pen.lineTo( (square.left + (square.width / 2.5))@
					(square.center.y + (square.width / 4)) );
				GUI.pen.lineTo( square.left@(square.center.y + (square.width / 4)) );
				GUI.pen.lineTo( square.left@(square.center.y - (square.width / 4)) );
				GUI.pen.lineTo( (square.left + (square.width / 2.5))@
					(square.center.y - (square.width / 4)) );
				GUI.pen.lineTo( square.rightTop );
				GUI.pen.perform( mode );
				},

			record:	{  | rect, mode = \fill|  // circle at 1/2 size
				var square;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
				if( mode == \fill )
					{ GUI.pen.fillOval( square ); }
					{ GUI.pen.strokeOval( square ); }		
				},
				
			sign: { |rect, text = "@", font, color|
				var center, radius, textBounds;
				center = rect.center; 
				radius = rect.width.min( rect.height ) / 2;
				
				font = font ? Font( "Monaco", 12 );
				textBounds = text.asString.bounds( font );
				GUI.pen.use({
					var scaleFactor;
					scaleFactor =  (radius * 2) / ( textBounds.width.max( textBounds.height ) );
					//scaleFactor = 2.1;
				
					//GUI.pen.scale( radius / ( textBounds.width.max( textBounds.height ) ) );
					
					GUI.pen.translate( center.x, center.y );
					GUI.pen.scale( scaleFactor, scaleFactor );
					text.asString.drawAtPoint( 
					
						 (0@0) - textBounds.center, font, color ? Color.black );
					});

				},
												
			pause:	{ | rect| // two vertical stripes ( || )
				var square;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
	
				GUI.pen.fillRect( square - Rect( 0, 0, square.width / 1.5, 0  ) );
				GUI.pen.fillRect( square + Rect( square.width / 1.5, 0, 
					square.width.neg / 1.5, 0  ) );
				},
				
			return2:	{ | rect| // stripe and backwards triangle ( |< )
				var square, wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				wd = square.width * 1/4;
	
				GUI.pen.moveTo( square.rightTop );
				GUI.pen.lineTo( (square.left + (wd*2))@square.center.y );
				GUI.pen.lineTo( square.rightBottom );
				GUI.pen.fill;
				
				GUI.pen.moveTo( square.leftTop );
				GUI.pen.lineTo( (square.right - (wd*2))@square.center.y );
				GUI.pen.lineTo( square.leftBottom );
				GUI.pen.fill;
				
				GUI.pen.fillRect( square.copy.width_( wd*0.7 ).moveBy(wd*1.66,0) );
				},

			return:	{ | rect| // stripe and backwards triangle ( |< )
				var square, wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				wd = square.width * 1/4;
	
				GUI.pen.moveTo( square.rightTop );
				GUI.pen.lineTo( (square.left + wd)@square.center.y );
				GUI.pen.lineTo( square.rightBottom );
				GUI.pen.fill;
				
				GUI.pen.fillRect( square.copy.width_( wd ) );
				},
							
			forward:	{ | rect| // 2 triangles ( >> )
				var square, wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				wd = square.width * 1/4;
				square = square.insetBy( 0, wd / 1.5 );
				square = square.resizeBy( wd / 1.5, 0);
					
				GUI.pen.moveTo( square.leftTop );
				GUI.pen.lineTo( square.center );
				GUI.pen.lineTo( square.leftBottom );
				GUI.pen.fill;
				
				GUI.pen.moveTo( square.center.x@square.top );
				GUI.pen.lineTo( square.right@square.center.y );
				GUI.pen.lineTo(square.center.x@square.bottom );
				GUI.pen.fill;
				},
			
			rewind:	{ | rect| // 2 triangles ( << )
				var square, wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				wd = square.width * 1/4;
				square = square.insetBy( 0, wd / 1.5 );
				square = square.resizeBy( wd / 1.5, 0);
				square = square.moveBy( wd / -1.5, 0);
				
				GUI.pen.moveTo( square.center.x@square.top );
				GUI.pen.lineTo( square.left@square.center.y );
				GUI.pen.lineTo(square.center.x@square.bottom );
				GUI.pen.fill;

				GUI.pen.moveTo( square.rightTop );
				GUI.pen.lineTo( square.center );
				GUI.pen.lineTo( square.rightBottom );
				GUI.pen.fill;
				
				},
				
			delete: { | rect|  // as seen in Mail 2.0; circle with stripe
				var square, wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
				
				wd = square.width * (1/5);
				GUI.pen.width = wd;
				GUI.pen.addArc( square.center, square.width / 2, 0, 2pi );
				//GUI.pen.stroke;	
				GUI.pen.line( 
					Polar( square.width / 2, 1.25pi ).asPoint + square.center,
					Polar( square.width / 2, 0.25pi ).asPoint + square.center
					  );
				GUI.pen.stroke;	
				
				},
				
			power:	{ | rect| // power button sign (stripe + 3/4 circle)
			
				var square, sl,sh,st,sw,sb,sr, w3,wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
						
				#sl, st, sw, sh = square.asArray;
				#sr, sb = square.rightBottom.asArray;
				wd = square.width * (1/5);
				// v2
				GUI.pen.width = wd;
				
				GUI.pen.addArc( square.center, sw / 3, 1.75pi, 1.5pi );
				GUI.pen.stroke;
				GUI.pen.line( square.center, ((sl + sr) / 2)@st );
				GUI.pen.stroke;
				},
				
			cmd: { |rect| // apple key sign
				var square, sl,sh,st,sw;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
				#sl, st, sw, sh = square.asArray;
				GUI.pen.width = (1/12) * sw;
	
				GUI.pen.moveTo( ( sl + ((1/6) * sw ))@( st + ((2/6) * sh )) );
				
				GUI.pen.lineTo( ( sl + ((5/6) * sw ))@( st + ((2/6) * sh )) );
				GUI.pen.addArc( ( sl + ((5/6) * sw ))@( st + ((1/6) * sh )),
					 (1/6) * sw, 0.5pi, -1.5pi );
					 
				GUI.pen.lineTo( ( sl + ((4/6) * sw ))@( st + ((5/6) * sh )) );
				GUI.pen.addArc( ( sl + ((5/6) * sw ))@( st + ((5/6) * sh )),
					 (1/6) * sw, pi, -1.5pi );
					 
				GUI.pen.lineTo( ( sl + ((1/6) * sw ))@( st + ((4/6) * sh )) );
				GUI.pen.addArc( ( sl + ((1/6) * sw ))@( st + ((5/6) * sh )),
					 (1/6) * sw, 1.5pi, -1.5pi );
				
				GUI.pen.lineTo( ( sl + ((2/6) * sw ))@( st + ((1/6) * sh )) );
				GUI.pen.addArc( ( sl + ((1/6) * sw ))@( st + ((1/6) * sh )),
					 (1/6) * sw, 0, -1.5pi );
					 
				GUI.pen.stroke;
				},
			
			alt: { |rect| // option key sign
				var square, sl,sh,st,sw;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
				#sl, st, sw, sh = square.asArray;
				GUI.pen.width = (1/12) * sw;
	
				GUI.pen.moveTo( sl@(st + ((1/4) * sh)) );
				GUI.pen.lineTo( (sl + ((1/3) * sw))@(st + ((1/4) * sh)) );
				GUI.pen.lineTo( (sl + ((2/3) * sw))@(st + ((3/4) * sh)) );
				GUI.pen.lineTo( (sl + sw)@(st + ((3/4) * sh)) );
				
				GUI.pen.moveTo( (sl + ((2/3) * sw))@(st + ((1/4) * sh)) );
				GUI.pen.lineTo( (sl + sw)@(st + ((1/4) * sh)) );
				
				GUI.pen.stroke;

				},
				
			shift: { |rect, direction = 0, mode = \stroke| // shift key sign 
				var square, wd;
				
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
					
				GUI.pen.rotate( direction - 0.5pi, square.center.x, square.center.y );
				
				GUI.pen.moveTo( (square.left)@(square.center.y) );
				GUI.pen.lineTo( (square.left)@(square.top + (square.height * (1/4) ) ) );
				GUI.pen.lineTo( (square.center.x)@(square.top + (square.height * (1/4) ) ) );
				GUI.pen.lineTo( (square.center.x)@(square.top) );
				GUI.pen.lineTo( (square.right)@(square.center.y) );
				GUI.pen.lineTo( (square.center.x)@(square.bottom) );
				GUI.pen.lineTo( (square.center.x)@(square.top + (square.height * (3/4) ) ) );
				GUI.pen.lineTo( (square.left)@(square.top + (square.height * (3/4) ) ) );
				GUI.pen.lineTo( (square.left)@(square.center.y) );
				
				GUI.pen.width = (1/12) * square.width;
				GUI.pen.perform( mode );
				
				GUI.pen.rotate( (direction - 0.5pi).neg, square.center.x, square.center.y );
				
				},
				
			lock: { |rect|
				var size = rect.width.min( rect.height ) * 0.8;
				
				GUI.pen.use({
					
					GUI.pen.translate( *rect.center.asArray );
					
					GUI.pen.fillRect( Rect( size.neg * 0.25,0, size / 2, size / 3 ) );
					GUI.pen.line( (size.neg /6)@0, (size.neg /6)@( size.neg / 6) )
						.addArc( 0@(size.neg / 6), size.neg / 6,0, pi )
						.lineTo( (size /6)@0 )
						.lineTo( (size /12)@0 )
						.lineTo(  (size /12)@( size.neg / 6) )
						.addArc( 0@(size.neg / 6), size.neg / 12,pi.neg, pi.neg )
						.lineTo( (size.neg / 12)@0 )
						.fill;
					});
				},
				
			unlock: { |rect|
				var size = rect.width.min( rect.height ) * 0.8;
				
				GUI.pen.use({
					GUI.pen.translate( *rect.center.asArray );
					
					GUI.pen.fillRect( Rect( size.neg * (1/6),0, size / 2, size / 3 ) );
					
					GUI.pen.line( (size.neg / 3)@0, (size.neg /3)@( size.neg / 6) )
						.addArc( (size.neg / 6)@(size.neg / 6), size.neg / 6,0, pi )
						.lineTo( 0@0 )
						.lineTo( (size.neg /12)@0 )
						.lineTo(  (size.neg /12)@( size.neg / 6) )
						.addArc( (size.neg / 6)@(size.neg / 6), size.neg / 12,pi.neg, pi.neg )
						.lineTo( (size.neg * (3/12) )@0 )
						.lineTo(  (size.neg /3)@0 )
						.fill;
					});
				},
		
			 x: { | rect| // x
			
				var square, sl,sh,st,sw,sb,sr, w3,wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				#sl, st, sw, sh = square.asArray;
				#sr, sb = square.rightBottom.asArray;
				wd = square.width * (1/5);
				
				GUI.pen.width = wd;
	
				GUI.pen.moveTo( rect.center + Polar( (sw / 2), 0.25pi ).asPoint );
				GUI.pen.lineTo( rect.center + Polar( (sw / 2), 1.25pi ).asPoint );
				GUI.pen.moveTo( rect.center + Polar( (sw / 2), 0.75pi ).asPoint );
				GUI.pen.lineTo( rect.center + Polar( (sw / 2), 1.75pi ).asPoint );
				GUI.pen.stroke; 
				
				}, 
			 	 
			i: 	{ | rect| // i
				var square, sl,sh,st,sw,sb,sr, w3,wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				#sl, st, sw, sh = square.asArray;
				#sr, sb = square.rightBottom.asArray;
				wd = square.width * (1/4);
				
				GUI.pen.fillRect( Rect( sl + (( sw - wd ) / 2) , st + (1.4 * wd), 
					wd, sh - (1.4 * wd) ) );
				GUI.pen.fillOval( 
					Rect( sl + (( sw - wd ) / 2), st, wd, wd ).insetBy( wd * -0.1,  
						wd * -0.1   ) );
				
				},
				
			'!':  	{ | rect| // !
				var square, sl,sh,st,sw,sb,sr, w3,wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				#sl, st, sw, sh = square.asArray;
				#sr, sb = square.rightBottom.asArray;
				wd = square.width * (1/4);
				
				GUI.pen.fillRect( Rect( sl + (( sw - wd ) / 2) , st, wd, sh - (1.33 * wd) ) );
				GUI.pen.fillRect( Rect( sl + (( sw - wd ) / 2), sb - wd, wd, wd ) );
				
				},
			'+': 	{ | rect| // +
				var square, sl,sh,st,sw,sb,sr,wd,w3;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				#sl, st, sw, sh = square.asArray;
				#sr, sb = square.rightBottom.asArray;
				wd =  square.width * (1/4);
				w3 = (square.width - wd) / 2;
				
				GUI.pen.moveTo( sl@( st+w3) );
				
				[ (sl+w3)@(st+w3), (sl+w3)@(st), (sr-w3)@(st), (sr-w3)@(st+w3),
					sr@(st+w3), sr@(sb-w3), (sr-w3)@(sb-w3), (sr-w3)@sb,
					(sl+w3)@sb, (sl+w3)@(sb-w3), sl@(sb-w3), sl@( st+w3) ]
					.do( GUI.pen.lineTo( _ ) );
					    
				GUI.pen.fill;
				
				},
			'-': 	{ | rect| // -
				var square, wd, w3;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				wd = square.width * (1/4);
				w3 = (square.width - wd) / 2;
				
				GUI.pen.fillRect( Rect( 
					square.left, square.top + w3,
					square.width,  wd ) )
				
				},
			
			star:   { |rect, numSides = 6, start| // *

				var square, sw, wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
				
				start = start ? -0.5pi;
				sw = square.width;	
				wd = sw * (1/5);
				
				GUI.pen.width = wd;
				
				numSides.do({ |i|
					GUI.pen.moveTo( rect.center );
					GUI.pen.lineTo( rect.center + Polar( (sw / 2), 
						 (2pi * ( i/numSides )) + start).asPoint );
				});
				
				GUI.pen.stroke; 
				
				},
				
			/*
			polygon:   { |rect, numSides = 6, start, mode = \fill|

				var square, sw, wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
				
				start = start ? -0.5pi;
				sw = square.width;	
				wd = sw * (1/8);
				
				//GUI.pen.width = wd;
				
				GUI.pen.moveTo( square.center + Polar( (sw / 2), start ) );
				
				(numSides + 1).do({ |i|
					GUI.pen.lineTo( square.center + Polar( (sw / 2), 
						 (2pi * ( i/numSides )) + start).asPoint );
				});
				
				GUI.pen.perform( mode ); 
				
				},
			*/
				
			polygon:  { |rect, numSides = 6, start, mode = \fill, type = \normal| 
					// type can also be \star ( kindly added by Jesper Elen )
				var square, sw, wd, factor;
				if (type == \star) { if (numSides.odd) 
						{ factor = (numSides / 2).floor } 
						{ factor = (numSides / 2) + 1 }} 
					{ factor = 1 };				
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 3, 
						rect.width.min( rect.height ) / 3 );
				start = start ? -0.5pi;
				sw = square.width;	
				wd = sw * (1/8);
				GUI.pen.moveTo( square.center + Polar( (sw / 2), start ) );
				(numSides + 1).do({ |i|
					GUI.pen.lineTo( square.center + Polar( (sw / 2), 
						(2pi * ( i/numSides * factor )) + start).asPoint );
				});
				GUI.pen.perform( mode ); 
				},
			
				
			lineArrow: { |rect, direction = 0, inSquare = true |
				
				var square, wd, radius;
				
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
										
				wd = (square.width) * 1/5;
				
				if( inSquare )
					{ radius = square.width/2; }
					{ radius = ( case 
							{ [0.25,0.75,1.25,1.75].includes( (direction % 2pi) / pi ) }
							{ radius = square.width/2; }
							{ 
						(direction % 2pi).inRange(0.25,0.75pi) or:
						(direction % 2pi).inRange(1.25,1.75pi) }
							{ radius = rect.height / 4  }
							{ true }
							{ radius = rect.width / 4  } );
						
						 };
							
				GUI.pen.width_( wd );
				
				GUI.pen.moveTo( square.center + Polar( radius, direction + pi ).asPoint );
				GUI.pen.lineTo( square.center + Polar( radius, direction ).asPoint );
				
				GUI.pen.moveTo( ( square.center + Polar( radius, direction ) ).asPoint
					+ Polar( wd * 2.5, direction + 0.75pi ).asPoint );
				GUI.pen.lineTo( ( square.center + Polar( radius, direction ) ).asPoint );
				GUI.pen.lineTo( ( square.center + Polar( radius, direction ) ).asPoint
					+ Polar( wd * 2.5, direction - 0.75pi ).asPoint );
				
				GUI.pen.stroke;
				},
			
			
			arrow: { |rect, direction = 0, mode = \fill|
				var square, wd;
				
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
					
				GUI.pen.rotate( direction, square.center.x, square.center.y );
				
				GUI.pen.moveTo( (square.left)@(square.center.y) );
				GUI.pen.lineTo( (square.left)@(square.top + (square.height * (1/3) ) ) );
				GUI.pen.lineTo( (square.center.x)@(square.top + (square.height * (1/3) ) ) );
				GUI.pen.lineTo( (square.center.x)@(square.top) );
				GUI.pen.lineTo( (square.right)@(square.center.y) );
				GUI.pen.lineTo( (square.center.x)@(square.bottom) );
				GUI.pen.lineTo( (square.center.x)@(square.top + (square.height * (2/3) ) ) );
				GUI.pen.lineTo( (square.left)@(square.top + (square.height * (2/3) ) ) );
				GUI.pen.lineTo( (square.left)@(square.center.y) );
				
				GUI.pen.perform( mode );
				
				GUI.pen.rotate( direction.neg, square.center.x, square.center.y );
				
				},
			
			roundArrow: { |rect, startAngle = -0.5pi, arcAngle = 1.5pi|
				var square, radius, wd, arrowSide;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
				
				radius = square.height / 2;
				wd = radius * (1/4);
				arrowSide = (wd*1.5) * 2.sqrt;
						
						
				GUI.pen.moveTo( Polar( radius - (wd/2), startAngle ).asPoint + square.center );
				GUI.pen.addArc( square.center, radius, startAngle, arcAngle ); //outer circle
				GUI.pen.lineTo( Polar( radius + wd, startAngle + arcAngle ).asPoint 
					+ square.center );
				GUI.pen.lineTo( Polar( radius + wd, startAngle + arcAngle ).asPoint +
						Polar( arrowSide, (startAngle + arcAngle) + 0.75pi ).asPoint 
							+ square.center);
				GUI.pen.lineTo( Polar( radius - (wd*2),  startAngle + arcAngle ).asPoint 
							+ square.center );
				GUI.pen.addArc( square.center, radius - wd, startAngle + arcAngle, arcAngle.neg );
				GUI.pen.lineTo(  Polar( radius - (wd/2), startAngle ).asPoint + square.center );
				
				GUI.pen.fill;
				
				},
			
			clock: { |rect, h = 4, m = 0, secs| // draw seconds when not nil
				var square, radius, hSize, mSize, hAngle, mAngle, wd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
				radius = square.height / 2;	
				wd = radius * (1/6);
				hSize = radius * (3/5);
				mSize = radius * (4/5);
				m = m + ( ( secs ? 0) / 60 ); 
				h = h + (m / 60);
				
				hAngle = ((h / 12) * 2pi) - 0.5pi;
				mAngle = ((m / 60) * 2pi) - 0.5pi;
				
				GUI.pen.width = wd;
				
				GUI.pen.addArc( square.center, radius, 0, 2pi );
				GUI.pen.stroke;
				
				GUI.pen.line( Polar( wd/2, hAngle - pi ).asPoint + square.center, 
					Polar( hSize, hAngle ).asPoint + square.center );
				//GUI.pen.stroke;
				
				GUI.pen.line( Polar( wd/2, mAngle - pi ).asPoint + square.center, 
					Polar( mSize, mAngle  ).asPoint + square.center );
				GUI.pen.stroke;
				
				if( secs.notNil )
					{ GUI.pen.width = wd / 3;
						GUI.pen.line( Polar( wd/2, ((secs / 60) * 2pi) - 1.5pi ).asPoint 
								+ square.center, 
						Polar( mSize, ((secs / 60) * 2pi) - 0.5pi  ).asPoint 
								+ square.center );
						GUI.pen.stroke;
					};
				},
				
			wait: { |rect, pos = 0, numLines = 16, active = true, color| 
					
				var radius, center, lineWidth = 0.08, centerSize = 0.5;				color = color ? Color.black;
				radius = rect.width.min( rect.height ) / 4;
				center = rect.center;
				if( active )
					{ Array.series( numLines + 1, -0.5pi, 2pi/numLines)[1..] 
						.do({ |val, i|
							color.copy.alpha_(
								(( (i/numLines) + (1 - pos) )%1.0) * 
									( if( active ) { 1 } { 0.5 } ) 
								).set;
							GUI.pen.width_( lineWidth * radius * 2 );
							GUI.pen.moveTo( ((val.cos@val.sin) * (radius * centerSize )) 
									+ center );
							GUI.pen.lineTo( ((val.cos@val.sin) * radius) + center );
							GUI.pen.stroke;
						});
					};
				},
				
			search: { |rect| // spotlight symbol
				var square, wd, rd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
				
				wd = square.width / 6;
				square = square.insetAll( wd, wd, 0, 0 );
				rd = square.width / 4;
				
				GUI.pen.width = wd;
				
				GUI.pen.addArc( ( square.left + rd )@(square.top + rd ), rd + wd, 0, 2pi );
				GUI.pen.moveTo( square.center );
				GUI.pen.lineTo( square.rightBottom );
				
				GUI.pen.stroke;
				
				}, 
				
			sine: { |rect, n = 1, phase = 0, res, fitToRect = false|
			
				var square, wd, step;
				
				if( fitToRect )
					{ square = Rect.aboutPoint( rect.center, 
						rect.width / 2.5, 
						rect.height / 4 ); }
					{ square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 ); };
				
				
				wd = square.height.min( square.width ) / 12;
				
				res = res ?? { (square.width / 6).ceil.max( 50 ) };
				
				
				step = (square.width / (n*res));
				
				GUI.pen.width = wd;
				
				((n*res) + 1).do({ |i|
					var point;
					point = ((i*step) + square.left)
						@(((( (i / res) * 2pi ) + phase).sin.neg + 1 * (square.height / 2))
							+ square.top);
					if( i == 0 )
						{ GUI.pen.moveTo( point ) }
						{ GUI.pen.lineTo( point ) };
					});
				GUI.pen.stroke;
				
				},
				
			document: { |rect|
				var square, docRect, wd, rd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				wd = square.width / 16;
				
				rd = square.width / 4;
				
				docRect = square.insetBy( 
					(square.width / 2) - (( square.height / 2.sqrt ) / 2) ,0);
					
				GUI.pen.width = wd;
					
				GUI.pen.moveTo( (docRect.center.x)@(docRect.top) );
				GUI.pen.lineTo( (docRect.right - rd)@(docRect.top) );
				
				GUI.pen.lineTo( (docRect.right)@(docRect.top + rd ) );
				GUI.pen.lineTo( docRect.rightBottom );
				GUI.pen.lineTo( docRect.leftBottom );
				GUI.pen.lineTo( docRect.leftTop );
				GUI.pen.lineTo( (docRect.center.x)@(docRect.top) );
					
				
			
				GUI.pen.moveTo( (docRect.right )@(docRect.top + rd) );
				GUI.pen.lineTo( (docRect.right - rd )@(docRect.top + rd) );
				GUI.pen.lineTo( (docRect.right - rd )@(docRect.top) );				
				GUI.pen.stroke;	
				
				},
				
			file: { |rect| drawFuncs[ \document ].value( rect ); },
				
			folder: { |rect|
				var square, docRect, wd, rd;
				var folderRect;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				wd = square.width / 16;
				
				rd = square.width / 8;
				
				docRect = square.insetBy( 
					(square.width / 2) - (( square.height / 2.sqrt ) / 2) ,0);
				
				folderRect = square.insetBy( 
					0, (square.width / 2) - (( square.height / 2.sqrt ) / 2)); 
				
					
				GUI.pen.width = wd;
				
					
				GUI.pen.moveTo( (folderRect.right)@(folderRect.center.y) );
				GUI.pen.lineTo( folderRect.rightBottom );
				GUI.pen.lineTo( folderRect.leftBottom );
				GUI.pen.lineTo( folderRect.leftTop );
				
				GUI.pen.lineTo( (folderRect.left + rd )@(folderRect.top - rd ) );
				GUI.pen.lineTo( ( (folderRect.left + (folderRect.width/2)) - rd )
					@(folderRect.top - rd) );
				GUI.pen.lineTo( ( folderRect.left + (folderRect.width/2) )@folderRect.top );
				GUI.pen.lineTo( folderRect.rightTop );
				GUI.pen.lineTo(  (folderRect.right)@(folderRect.center.y) );
				
				
								
				GUI.pen.stroke;	
				
				},
				
			led: { |rect, contents = '8'|  // 2 digits
				var square, docRect, wd, hOffset, vOffset = 0;
				var array, size;
				
				square = Rect.aboutPoint( rect.center, 
						rect.width / 4, rect.height / 4 );
						
				contents = contents.asString.toUpper.reverse;
				
				size = contents.size;				
				wd = ( square.width / ( size * 4 + (size - 1 ) ) )
					.min( square.height / 7 );
				
				//vOffset = (size * 2.5) - 4;
				vOffset = ( square.height - (wd * 7) ) / 2;
				hOffset = ( square.width - ( wd *  ( size * 4 + (size - 1 ) ) ) ) / 2;
				
				
				GUI.pen.translate( hOffset + square.left, vOffset + square.top );
			
				
				array =	( { [0,0,0, 0,0,0,0] } ! size ).collect({ |item, i|
						var out;
						out = ( 	
							////   hor.   vert.
							'0': [ 1,0,1, 1,1,1,1 ],
							'1': [ 0,0,0, 0,1,0,1 ],
							'2': [ 1,1,1, 0,1,1,0 ],
							'3': [ 1,1,1, 0,1,0,1 ],
							'4': [ 0,1,0, 1,1,0,1 ],
							'5': [ 1,1,1, 1,0,0,1 ],
							'6': [ 1,1,1, 1,0,1,1 ],
							'7': [ 1,0,0, 0,1,0,1 ],
							'8': [ 1,1,1, 1,1,1,1 ],
							'9': [ 1,1,1, 1,1,0,1 ],
							'-': [ 0,1,0, 0,0,0,0 ],
							'A': [ 1,1,0, 1,1,1,1 ],
							'B': [ 0,1,1, 1,0,1,1 ],
							'C': [ 1,0,1, 1,0,1,0 ],
							'D': [ 0,1,1, 0,1,1,1 ],
							'E': [ 1,1,1, 1,0,1,0 ],
							'F': [ 1,1,0, 1,0,1,0 ],
							'G': [ 1,0,1, 1,0,1,1 ],
							'H': [ 0,1,0, 1,0,1,1 ],
							'I': [ 0,0,0, 1,0,1,0 ],
							'J': [ 0,0,1, 0,1,0,1 ],
							'L': [ 0,0,1, 1,0,1,0 ],
							'O': [ 0,1,1, 0,0,1,1 ],
							'P': [ 1,1,0, 1,1,1,0 ],
							'Q': [ 1,1,0, 1,1,0,1 ],
							'R': [ 0,1,0, 0,0,1,0 ],
							'S': [ 1,1,1, 1,0,0,1 ],
							'T': [ 0,1,1, 1,0,1,0 ],
							'U': [ 0,0,1, 1,1,1,1 ],
							'Y': [ 0,1,1, 1,1,0,1 ],
							'Z': [ 1,1,1, 0,1,1,0 ],
							'_': [ 0,0,1, 0,0,0,0 ],
							'*': [ 1,1,0, 1,1,0,0 ]
							)[ contents[i].asSymbol ];
						
						out ? item;
						}).reverse;
					
				array.do({ |gItem, i|
					var ghPos;
					ghPos = i * 5;
					
					gItem[[0,1,2]].do({ |item, ii|
						var vPos; 
						if( item != 0 )
						{	vPos = ii * 3;
							
							GUI.pen.moveTo( ( (ghPos + 0.5)@( vPos + 0.5 ) ) * wd  );
							GUI.pen.lineTo( ( (ghPos + 1)@vPos ) * wd );
							GUI.pen.lineTo( ( (ghPos + 3)@vPos ) * wd );
							GUI.pen.lineTo( ( (ghPos + 3.5)@(vPos + 0.5) ) * wd );
							GUI.pen.lineTo( ( (ghPos + 3)@(vPos + 1) ) * wd );
							GUI.pen.lineTo( ( (ghPos + 1)@(vPos + 1) ) * wd );
							GUI.pen.lineTo( ( (ghPos + 0.5)@( vPos + 0.5 ) ) * wd  );
							
							GUI.pen.fill;
						};
						
						});
					
					gItem[[3,4,5,6]].do({ |item, ii|
						var vPos, hPos;
						if( item != 0 )
						{ 	vPos = (ii / 2).floor * 3;
							hPos = (ii % 2) * 3;
							
							GUI.pen.moveTo( ( (ghPos + hPos + 0.5)@( vPos + 0.5 ) ) * wd  );
							GUI.pen.lineTo( ( (ghPos + hPos + 1)@( vPos + 1 ) ) * wd );
							GUI.pen.lineTo( ( (ghPos + hPos + 1)@( vPos + 3 ) ) * wd );
							GUI.pen.lineTo( ( (ghPos + hPos + 0.5)@(vPos + 3.5) ) * wd );
							GUI.pen.lineTo( ( (ghPos + hPos)@(vPos + 3) ) * wd );
							GUI.pen.lineTo( ( (ghPos + hPos)@(vPos + 1) ) * wd );
							GUI.pen.lineTo( ( (ghPos + hPos + 0.5)@( vPos + 0.5 ) ) * wd  );
							 
							GUI.pen.fill;
						};
						
						});
					
										
					});
				
				GUI.pen.translate( hOffset.neg + square.left.neg, vOffset.neg + square.top.neg );
				
				},
				
				
			// combined
			
			textDocument: { |rect|
				var square, docRect, wd, rd;
				square = Rect.aboutPoint( rect.center, 
						rect.width.min( rect.height ) / 4, 
						rect.width.min( rect.height ) / 4 );
						
				wd = square.width / 16;
				
				rd = square.width / 4;
				
				docRect = square.insetBy( 
					(square.width / 2) - (( square.height / 2.sqrt ) / 2) ,0);
				
				drawFuncs[ \document ].value( rect );
				
				4.do({ |i|
					var yy;
					yy = docRect.top + rd + wd + ((i+1) * (wd * 2));
					GUI.pen.line( 
						(docRect.left + (wd * 2))@yy,
						(docRect.right - (wd * 2))@yy  );
					});
				
				GUI.pen.stroke;
								
				},
			
			warning:  { |rect| 
				drawFuncs[ \polygon ].value( rect, 8, 0.125pi );
				if( GUI.pen.respondsTo( \color_ ) )
					{ GUI.pen.color = Color.white; }
					{ Color.white.set; };
				GUI.pen.width = 2;
				drawFuncs[ \polygon ].value( rect
					.insetBy( 4, 4 ), 8, 0.125pi, \stroke );
				drawFuncs[ '!' ].value(  rect
					.insetBy( 8, 8 ) );
				}
				
				);
	
		}
	}