// adapted from wslib 2006

LNX_InviteInterface {
	
	classvar <>modal = true;
	
	var <win, <string, <buttons, <>actions, <>doClose,
	    <color, <iconName, iconView, <stringView, <buttonViews;
	var <window, <>onCloseIndex = -1;
	var <>buttonClosesWindow = true;
	
	var <>wishList, <>invitedUsers, <>rejects, <gui, <myInvite;
	
	var <>doOnClose;
	
	refresh{
	
		// resize and display
		gui[\userList].refresh;
	}
	
	close{ window.close }
	
	string_ {  |newString = ""|
		string = newString;
		if( window.notNil && { window.dataptr.notNil } )
			{ stringView.string = string };
		}
		
	string2_ {  |newString = ""|
		if( window.notNil && { window.dataptr.notNil } )
			{ gui[\bottomString].string = newString };
		}
		
	iconName_ { |newIconName = \warning|
		iconName = newIconName.asSymbol;
		if( window.notNil && { window.dataptr.notNil } )
			{ iconView.refresh; };
		}
		
	color_ { |newColor|
		color = newColor ? Color.red.alpha_(0.75);
		if( window.notNil && { window.dataptr.notNil } )
			{ window.refresh; };
		}
		
	background { ^window.view.background }
	background_ { |aColor|
		if( window.notNil && { window.dataptr.notNil } )
			{  window.view.background = aColor };
		}
		
	hit { |index| // focussed or last if no index provided
		if( window.notNil && { window.dataptr.notNil } )
			{ index = index ?? { buttonViews.detectIndex({ |bt| bt.hasFocus }) ?
					 ( buttonViews.size - 1 ) }; 
				 buttonViews[ index ] !? 
				 	{ buttonViews[ index ].action.value( buttonViews[ index ], this ) }
				};
		}
		
	enable { |index| if( index.notNil ) // all if no index provided
				{ buttonViews[ index ].enabled_( true ) }
				{ buttonViews.do( _.enabled_( true ) ) };
		}
		
	disable { |index| if( index.notNil )
				{ buttonViews[ index ].enabled_( false ) }
				{ buttonViews.do( _.enabled_( false ) ) };
		}
		
	isEnabled { |index| if( index.notNil )
			{ ^buttonViews[ index ].enabled }
			{ ^buttonViews.collect( _.enabled ) };
		}
		
	focus { |index| if( index.notNil )
				{ buttonViews[ index ].focus( true ); }
				{ buttonViews.last.focus( true ); }
		}
		
	buttonLabel { |index = 0|
		^buttonViews.wrapAt( index ).states[0][0];
		}
	
	buttonLabel_ { |index = 0, newLabel = ""|
		buttonViews.wrapAt( index ).states = [ [ newLabel ] ];
		buttonViews.wrapAt( index ).refresh;
		buttons[ index.wrap( 0, buttons.size - 1 ) ] = newLabel;
		}
	
		
	*new { | win,  string = "Warning!", buttons, actions, doClose, color, background,
			iconName = \warning,
			border = true,w=0,h=0,name="Alert",wishList,invitedUsers,rejects,myInvite=false |
		^super.newCopyArgs( win, string, buttons, actions,doClose, color, iconName ).init( background, border,w,h,name,wishList,invitedUsers,rejects,myInvite );
		}
		
	openAgain { ^this.init; }
		
	init { |background, border,w,h,name,argWishList,argInvitedUsers,argRejects,argMyInvite|
		//var buttonViews;
		var charDict;
		
		wishList=argWishList ?();
		invitedUsers=argInvitedUsers ?[];
		rejects=argRejects ?[];
		myInvite=argMyInvite;
			
		gui=();
		
		background = background ? Color.white;
		color = color ? Color.red.alpha_( 0.75 );
		buttons = buttons ?? 
			{buttons = [	["cancel"],
						["ok"]
					 ];
			};
		
		buttons = buttons.collect( { |item|
			case { item.isString }
				{ [ item,  ] }
				{ item.class == Symbol }
				{ [ item.asString ] }
				{ item.isArray }
				{ item }
				{ true }
				{ [ item.asString ] }
			} );
				
		actions = actions ?? { ( { |i| { |button| buttons[i][0].postln; } } ! buttons.size ); };
			
		doOnClose=true;
		
		h=h+((wishList.size/2).asInt*14);
			
		if( modal )		
			{ 
				if ((win.isNil)or:{win.isClosed}) {
					window = SCModalWindow( name, 
						Rect.aboutPoint( Window.screenBounds.center, 
							((buttons.size * 42) + 2).max( 160 ), 
								((26 + (string.occurrencesOf( $\n ) * 10) ) + 4).max( 52 )
								).resizeBy(w,h), false, border ? true );
					win=window;
				}{
			
					window = SCModalSheet( win, 
						Rect.aboutPoint( Window.screenBounds.center, 
							((buttons.size * 42) + 2).max( 160 ), 
								((26 + (string.occurrencesOf( $\n ) * 10) ) + 4).max( 52 )
								).resizeBy(w,h), false, border ? true );
					win.front;
				}
			} {
			window = Window( name, 
				Rect.aboutPoint( Window.screenBounds.center, 
					((buttons.size * 42) + 2).max( 160 ), 
						((26 + (string.occurrencesOf( $\n ) * 10) ) + 4).max( 52 )
						).resizeBy(w,h), false, border ? true );
			
			window.front;
			};
		
		window.view.background_( background );
		window.alwaysOnTop_( true );
		window.alpha_( 0.95 );
		window.drawFunc_( { |w|
			Pen.width = 2;
			color.set;
			Pen.strokeRect( w.bounds.left_(0).top_(0).insetBy(1, 1) );
			} );
		
		iconView = UserView( window, Rect( 4,4, 72, 72) ).drawFunc_({ |vw|
			//color.set;
			Color.orange.set;
			DrawIcon.symbolArgs( iconName, vw.bounds );
			}).canFocus_( false );
		
		stringView = StaticText(window, Rect(80,4, window.bounds.width - 84, 66 ) )
			.string_( string ).font_( Font( "Helvetica-Bold", 12 ) )
			//.background_(Gradient(Color(0.3,0.3,0.3),Color(0.5,0.5,0.35)))
			;
			//.align_( \center );
			
		gui=();
		
		gui[\bottomString]=  StaticText(window, Rect(80,window.bounds.height - 34,
												     window.bounds.width - 84,20 ) )
			.string_("Waiting...").font_( Font( "Helvetica-Bold", 12 ) );
		
		gui[\userList]=SCUserView(window,Rect(80,75, window.bounds.width - 84, 
											 window.bounds.height - 119 ))
						.canFocus_( false )
						.drawFunc_{|me|
						
							var wishListSize=(wishList.size/2).ceil;
						
							Pen.use{
								Pen.smoothing_(true);
								Pen.font_(Font( "Helvetica-Bold", 12 ));
								
								gui[\allUsers]=[];
								wishList.keysDo{|key|
									if (myInvite) {
										gui[\allUsers]=gui[\allUsers].add(
											[key,wishList[key].name.asString]);
									}{
										gui[\allUsers]=gui[\allUsers].add(
											[key,wishList[key]]);
									};
								};
								
								gui[\allUsers].sort({|a,b|(a[1].toLower)<(b[1].toLower)});
								
								
								gui[\allUsers].do{|l,i|
									var id=l[0], name=l[1],rect;
								
									rect=Rect((i/wishListSize).asInt*150+15,
											(i%wishListSize)*14,50,14);
								
									Pen.fillColor_(Color.black);
									Pen.font_(Font( "Helvetica-Bold", 12 ));
									
									Pen.stringLeftJustIn(name,rect);
									
									Pen.font_(Font( "Helvetica-Bold", 13 ));
										
									if (myInvite) {
										if (invitedUsers.includesKey(id)) {
											Pen.fillColor_(Color.black);
											Pen.stringLeftJustIn((195.asAscii.asString),
												rect.moveBy(-14,1));
											Pen.fillColor_(Color.green);
											Pen.stringLeftJustIn((195.asAscii.asString),
												rect.moveBy(-15,0));
										};												if (rejects.includesKey(id)) {
											Pen.fillColor_(Color.black);
											Pen.stringLeftJustIn("X",
												rect.moveBy(-14,1));
											Pen.fillColor_(Color.red);
											Pen.stringLeftJustIn("X",
												rect.moveBy(-15,0));
										};
									}{
										if (invitedUsers.includes(id)) {
											Pen.fillColor_(Color.black);
											Pen.stringLeftJustIn((195.asAscii.asString),
												rect.moveBy(-14,1));
											Pen.fillColor_(Color.green);
											Pen.stringLeftJustIn((195.asAscii.asString),
												rect.moveBy(-15,0));
										};												if (rejects.includes(id)) {
											Pen.fillColor_(Color.black);
											Pen.stringLeftJustIn("X",
												rect.moveBy(-14,1));
											Pen.fillColor_(Color.red);
											Pen.stringLeftJustIn("X",
												rect.moveBy(-15,0));
										};
									};
									
								};
								
								
							};
						
						};
		
		buttonViews = { |i| 
			var rect;
			rect = Rect( 
					(window.view.bounds.width) - ((buttons.size - i ) * 84), 
					window.view.bounds.height - 24, 80,20 );
			Button(window, rect)
					.states_( [
						buttons[i] ] )
					.action_( { |button|
						if( button.enabled )
							{ 
								doOnClose=false;
								actions.wrapAt(i).value( button, this );
								if( buttonClosesWindow && { window.dataptr.notNil } ){
									if (doClose.wrapAt(i)) {
										window.close;
									}}
								};
						} );
					} ! buttons.size;
					
		buttonViews.last.focus;
		
		charDict = ();
		buttonViews.do({ |item, i| // keydownactions for first letters of buttons
			charDict[ item.states[0][0][0].toLower.asSymbol ] = {
				doOnClose=false;
				item.action.value( item, this ) };
			});
		
		buttonViews.do({ |item|
			item.keyDownAction = { |v, char, a,b|
				case { [13,3].includes( b ) } // enter or return
					{ doOnClose=false; v.action.value( v, this ) }
					{ true }
					{ charDict[ char.asSymbol ].value; };
				};
			});
		
		window.refresh;
		window.onClose_(
			{if (doOnClose) {
				buttonViews[onCloseIndex] !? 
				{ buttonViews[onCloseIndex].action.value( buttonViews[onCloseIndex], this ) };
				
			};}
		
		);
		//^super.newCopyArgs( window, string, buttonViews, actions, color, iconName, iconV, strV );
	}
}