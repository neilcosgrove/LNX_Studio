/*
(
w=MVC_Window().create;

v=MVC_Text(w,Rect(20,20,200,14), "".asModel.maxStringSize_(10))
	.shadow_(false)
	.canEdit_(true)
	.enterStopsEditing_(true)
	.stringAction_{|me,string| string.postln }
	.enterKeyAction_{|me,string| string.postln }
	.color_(\string,Color(59/77,59/77,59/77)*1.4)
	.color_(\edit,Color.orange)
	.color_(\background,Color.black)
	.color_(\cursor,Color.white)
	.color_(\focus,Color.orange)
	.color_(\editBackground, Color(0.2,0.2,0.2))
	.font_(Font.new("STXihei", 12));
)

v.charSizesIntegral

*/

// StaticText or a label
// idea: add a prefix and postfix

MVC_Text : MVC_StaticText {}

MVC_StaticText : MVC_View {

	var <align='left', 	<shadow=true, 	<>noShadows=1;
	var <rotate=0,		<down=false, 		<>shadowDown=true;
	var clicks, 			<>downColor, 		<>active=true;	var <>alwaysDown=false,	<>excludeFromVerbose=false;
	var <>keyDownAction, 	<>keyUpAction, 	<>upKeyAction;
	var <>downKeyAction, 	<>enterKeyAction, 	<>stringAction;
	var <clipChars=false,	<charSizes, 		<charSizesIntegral;
	var <clipString, 		<>hasBorder=false;
	var <editing=false, 	<cursor=nil, 		<cursorFlash=false;
	var <canEdit=false,	<>enterStopsEditing = true;
	var <>tasks,			<>penShadow=false, <>rightIfIcon=false;
	var <>maxStringSize;
	
	// add the colour to the Dictionary, no testing to see if its there already
	addColor_{|key,color|
		colors[key]=color;
		if (key=='focus') {
			{
				if (view.notClosed) { view.focusColor_(color) }
			}.defer;
		}{
			if (key=='label') {
				labelGUI.do(_.refresh);
			}{
				this.refresh;
			};	
		}
	}
	
	// can the text be edited
	canEdit_{|bool|
		canEdit=bool;
		this.canFocus_(bool);
	}
	
	// set the string
	string_{|argString|
		string=argString.asString;
		this.calcCharSizes;
		this.refresh;
	}
	
	// set the font
	font_{|argFont|
		font=argFont;
		this.calcCharSizes;
		this.refresh;
	}
	
	// does widget clip chars on display
	clipChars_{|bool|
		clipChars=bool;
		this.calcCharSizes;
	}
	
	// calculate the size of all characters and intergate
	calcCharSizes{
		if (clipChars||canEdit) {
			charSizes=[];
			string.do{|c|
				charSizes=charSizes.add(c.asString.bounds(font).width);
			};
			charSizesIntegral = charSizes.integrate;
		};
		this.makeClipString;
	}
	
	// clip the number of chars displayed
	makeClipString{
		if (clipChars) {	
			if (string.size==0) {
				clipString=string;
			}{
				clipString = string[0.. 
				 ( (charSizesIntegral.indexOfNearest(w-5)).clip(0,inf).asInt)];
			};
		}{
			clipString=string;
		};
	}
		
	// start editing the text, called by mouseclick or return on keyboard
	startEditing{|x|
		// find index for cursor
		if (x.isNumber) { cursor = ([0]++charSizesIntegral).indexOfNearest(x) };
		editing=true; 
		this.calcCharSizes;
		tasks[\cursorFlash] = {{
			cursorFlash = cursorFlash.not;
			this.refresh;
			0.6.wait;
		}.loop}.fork(AppClock); // start flashing cursor task
	}
	
	// stop editing and flashing cursor task
	stopEditing{
		editing=false; 
		tasks[\cursorFlash].stop;
		tasks[\cursorFlash] = nil;
	}
	
	// set your defaults
	initView{
		charSizes         = [];
		charSizesIntegral = [];
		colors['icon']           = nil;
		colors['background']     = Color.clear;
		colors['editBackground'] = Color(0,0,0,0.8);
		colors['string']         = Color.white;
		colors['stringDown']     = Color.black;
		colors['edit']           = Color.orange;
		colors['border']         = Color(0,0,0,0.5);
		colors['cursor']         = Color(0,0,0);
		canFocus=true;
		tasks = IdentityDictionary[];
	}
	
	// make the view
	createView{
		var r1, align2;
		
		view=UserView(window,rect)
			.canFocus_(true) // without focus we can't use keyboard
			.drawFunc_{|me|
				if (me.hasFocus.not) {this.stopEditing}; // stop editing if we loose focus
				if (verbose && (excludeFromVerbose.not)) {
					[this.class.asString, 'drawFunc', label].postln };
					
				r1=Rect(0,0,w,h);
				Pen.use{
					// draw background
					if (colors[\background].notNil) {
						colors[\background].set;
						if (editing) { colors['editBackground'].set};
						Pen.fillRect(r1);
					};
					if (showLabelBackground) {
						Color.black.alpha_(0.2).set;
						Pen.fillRect(r1);
					};
					// draw border
					if (hasBorder) {
						colors[\border].set;
						Pen.strokeRect(r1);
					};
					
					align2=align;
					
					// draw user Icon
					if (colors[\icon].notNil) {
						if (rightIfIcon) {
							Pen.fillColor_(Color.black);
							DrawIcon( \user, Rect(-1,1,h,h).insetBy(-1,-1) );
							DrawIcon( \user, Rect(1,1,h,h).insetBy(-1,-1) );
							Pen.fillColor_(colors[\icon]*0.9);
							DrawIcon( \body, Rect(0,1,h,h).insetBy(0,0) );
							Pen.fillColor_(colors[\icon]*1.3+0.3);
							DrawIcon( \head, Rect(0,1,h,h).insetBy(0,0) );
						}{
							Pen.fillColor_(Color.black);
							DrawIcon( \user, Rect(0,1,h,h).insetBy(-1,-1) );
							DrawIcon( \user, Rect(2,1,h,h).insetBy(-1,-1) );
							Pen.fillColor_(colors[\icon]*0.9);
							DrawIcon( \body, Rect(1,1,h,h).insetBy(0,0) );
							Pen.fillColor_(colors[\icon]*1.3+0.3);
							DrawIcon( \head, Rect(1,1,h,h).insetBy(0,0) );
						};
						if (rightIfIcon) {align2='right'};
					};
					
					// manually draw shadow
					Pen.rotate(rotate,w/2,h/2);
					Pen.fillColor_(Color.black);
					Pen.font_(font);
					if ((shadow)&&((down.not)||(shadowDown))) {
						noShadows.do{|i|
							i=i+1;
							switch (align2)
								{'left'}  {
									Pen.stringLeftJustIn (clipString,r1.moveBy(i,i));
									Pen.stringLeftJustIn (clipString,r1.moveBy(i+0.5,i+0.5));
								}
								{'center'}  {
									Pen.stringCenteredIn (clipString,r1.moveBy(i,i));
									Pen.stringCenteredIn (clipString,r1.moveBy(i+0.5,i+0.5));
								}
								{'rotate'} {
									Pen.stringCenteredIn(clipString,Rect(0.5,0.5,w,h));
									Pen.stringCenteredIn(clipString,Rect(1,1,w,h));
								}
								{'right'} {
									Pen.stringRightJustIn(clipString,r1.moveBy(i,i));
									Pen.stringRightJustIn(clipString,r1.moveBy(i+0.5,i+0.5));
								};
							}
					};
					if (midiLearn) {
						Pen.fillColor_(colors[\midiLearn]);
					}{			
						Pen.fillColor_(colors[enabled.if(
							(down&&(shadowDown.not)&&(colors['stringDown'].notNil)).if(
								\stringDown,\string),
						\disabled)]);	
					};
					if (editing) { Pen.fillColor_(colors[\edit])};
					
					// use pen shadow
					if (penShadow) {
						Pen.setShadow((-2)@(-2), 5, Color.black);
					};

					// draw text
					switch (align2)
						{'left'}  { Pen.stringLeftJustIn (clipString,r1) }
						{'center'}  { Pen.stringCenteredIn (clipString,r1) }
						{'rotate'} {				
							Pen.stringCenteredIn(clipString,Rect(0,0,w,h));
						}
						{'right'} {
							if ((rightIfIcon)and:{colors[\icon].notNil}) {
								Pen.stringRightJustIn(clipString,r1.moveBy(-2,0))
							}{
								Pen.stringRightJustIn(clipString,r1)
							};	
						};
					
					// draw cursor
					if (editing&&cursorFlash) {
						case {cursor.isNil} { // cursor auto at end
							var x = (([0]++charSizesIntegral)?0).last;
							if (align=='left') { x=x+3 };							if (align=='center') {
								x= (w - (charSizesIntegral.last?0))/2 + x;
							};
							Pen.smoothing_(false);
							Pen.strokeColor_(colors[\cursor]);
							Pen.line((x@1),(x@(h-2)));
							Pen.stroke;	
						} {cursor.isNumber} { // cursor pos as index, 0 is before 1st char
							var x = ((([0]++charSizesIntegral)[cursor])?0);
							if (align=='left') { x=x+3 };
							if (align=='center') {
								if (clipChars && (clipString.size!=string.size)) {
									x=x;
								}{
									x= (w - (charSizesIntegral.last?0))/2 + x;
								};
							};
							Pen.smoothing_(false);
							Pen.strokeColor_(colors[\cursor]);
							Pen.line((x@1),(x@(h-2)));
							Pen.stroke;
						} {cursor.isCollection} { // cursor is selection [begin,end], to do?
							
						};	
					};
				}; // end.pen
			};		
	}
	
	dragControls{
		// what i send
		view.beginDragAction_{|me|
			me.dragLabel_(string); // the drag label
			string
		};
		// these wouldn't normally be created with staticText
		if (numberGUI.notNil) { numberGUI.beginDragAction_(view.beginDragAction) };
		labelGUI.do{|labelView| labelView.beginDragAction_(view.beginDragAction) };
	}
	
	// align 'left' or 'center'. right not done yet
	align_{|symbol|
		align=symbol;
		if (view.notClosed) { view.refresh };
	}

	// rotate the text by an angle
	rotate_{|angle|
		rotate=angle;
		if (view.notClosed) { view.refresh };
	}
	
	// draw a manual shadow
	shadow_{|bool|
		shadow=bool;
		if (view.notClosed) { view.refresh };
	}
	
	// add the controls
	addControls{
		
		var didAnything=false, noKeyPresses=0;
		
		view.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			didAnything=false; 
			this.stopEditing;
			mouseDownAction.value(me, x, y, modifiers, buttonNumber, clickCount);
			if (modifiers==524576) { buttonNumber=1 };
			if (modifiers==262401) { buttonNumber=2 };
			buttonPressed=buttonNumber;
			clicks=clickCount;
			if (modifiers.asBinaryDigits[4]==0) { // check apple not pressed because of drag
				if (editMode) {
					lw=lh=nil; startX=x; startY=y; view.bounds.postln; // for moving
				}{
					evaluateAction=true;
					if (buttonNumber==2) {
						this.toggleMIDIactive
					}{
						down=true;
					};
				}
			};
			this.refresh
		};
		
		view.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			// moving stops us from begin able to edit text. insted we can move insts & fx
			if (enterStopsEditing && didAnything.not) {
				didAnything=true; 
				this.stopEditing;
				this.refresh
			};
			mouseMoveAction.value(me, x, y, modifiers, buttonNumber, clickCount);
			if (editMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}{				
				if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}) {
					if (down.not) {
						down=true;
						view.refresh;
					};
				}{
					if ((down)and:{alwaysDown.not}) {
						down=false;
						view.refresh;
					};
				};
			};	
		};
		
		view.mouseUpAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if (active) {value=0};
			down=false;
			mouseUpAction.value(me, x, y, modifiers, buttonNumber, clickCount);
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				this.viewDoValueAction_(value,nil,true,false);
				if (clicks==2) {
					this.valueActions(\mouseUpDoubleClickAction,this);
					if (model.notNil){ model.valueActions(\mouseUpDoubleClickAction,model) };
				}
			};
			if ((didAnything.not)&&canEdit) {	
				switch (align) {'left'} {
					this.startEditing(x);	
				}{'center'}{
					if (clipChars && (clipString.size!=string.size)) {
						this.startEditing(x);
					}{
						this.startEditing(x- ((w - (charSizesIntegral.last?0))/2));
					};
				};
				this.refresh
			};			
			if (alwaysDown) { this.refresh };
		};
		
		// @TODO: new Qt "key" codes
		view.keyDownAction_{|me,char,mod,uni,keycode,key|
			keycode.postln;
			// help fixes double press bug by see if cmd is pressed
			if (mod.isXCmd) {
				noKeyPresses=noKeyPresses+1;
				case {key.isLeft} { // left	
					cursor = 0;
					if (editing.not) {this.startEditing};
					this.refresh;
				} {key.isRight} { // right
					cursor = string.size;
					if (editing.not) {this.startEditing};
					this.refresh;
				};
			}{ noKeyPresses=1 };
			
			if (noKeyPresses.odd) { // stops double press bug
				keyDownAction.value(me,char,mod,uni,keycode);
				if (mod.isXCmd) { // XPlatform cmd key
					if (key.isAlphaKey('C')) { // cmd copy	
						case {cursor.isNil} {
							Platform.case(
								\osx, { ("echo"+string+"| pbcopy").unixCmd },
								\linux, { ("echo"+string+"| xclip -i").unixCmd },
								\windows, { "copy not supported yet on windows".postln; });
						} {cursor.isNumber} {

						} {cursor.isCollection} {	
							
						};
					}; 
					
					if (editing) {
						case {key.isAlphaKey('V')} { // cmd paste
							var clipboard = Platform.case(
								\osx, { "pbpaste".unixCmdGetStdOut },
								\linux, { "xclip -o".unixCmdGetStdOut },
								\windows, { "paste not supported yet on windows".postln; });
							clipboard.postln;
							clipboard = clipboard.select{|char| 
								(char.isAlphaNum)||(char.isPunct)||(char==($\ ))
							};
							case {cursor.isNil} {
								string=string++clipboard;
							} {cursor.isNumber} {									string=string.insert(cursor,clipboard);
								cursor = (cursor+(clipboard.size)).clip(0,string.size);
							} {cursor.isCollection} {		
							};
							this.clipStringToMaxSize;
							this.calcCharSizes;
							this.refresh;
							this.valueActions(\stringAction,this);
							if (model.notNil){ model.stringAction_(string,this) };
						}{keycode==51} { // cmd delete
							case {cursor.isNil} {
								string="";
							} {cursor.isNumber} {									string = string.drop(cursor);
								cursor=0;
							} {cursor.isCollection} {		
							};	
							this.calcCharSizes;
							this.refresh;
							this.valueActions(\stringAction,this);
							if (model.notNil){ model.stringAction_(string,this) };
							stringAction.value(this,string);
						}
					};	
				}{ // not cmd key
					case {keycode==126} { // up
						this.stopEditing;
						this.refresh;
						upKeyAction.value(this,string);
					} {keycode==125} { // down
						this.stopEditing;
						this.refresh;
						downKeyAction.value(this,string);
					} {keycode==123} { // left	
						case {cursor.isNil} {
							cursor = string.size;
						} {cursor.isNumber} {	
							cursor = (cursor-1).clip(0,string.size);
						} {cursor.isCollection} {	
							
						};
						this.refresh;
					} {keycode==124} { // right
						case {cursor.isNil} {
							cursor = string.size;
						} {cursor.isNumber} {	
							cursor = (cursor+1).clip(0,string.size);
						} {cursor.isCollection} {	
							
						};
						this.refresh;
					};
					if (editing) {		
						case {keycode==51} { // delete
							case {cursor.isNil} {
								string=string.drop(-1);
							} {cursor.isNumber} {
								if (cursor>0) {
									string = string.select{|i,index| index != (cursor-1) };
									cursor= (cursor-1).clip(0,string.size);
								}
							} {cursor.isCollection} {		
							};		
							this.calcCharSizes;
							this.refresh;
							this.valueActions(\stringAction,this);
							if (model.notNil){ model.stringAction_(string,this) };
							stringAction.value(this,string);
						} {keycode==36} { // enter
							cursor=nil;
							if (enterStopsEditing) {this.stopEditing};
							this.refresh;
							enterKeyAction.value(this,string);
						}{ 		
							// alphaNum etc..
							if ((char.isAlphaNum)||(char.isPunct)||(char==($\ ))) {
								case {cursor.isNil} {
									string=string++char;
								} {cursor.isNumber} {									string=string.insert(cursor,char);
									this.clipStringToMaxSize;
								} {cursor.isCollection} {	
								};
								this.clipStringToMaxSize;
								if (cursor.isNumber) {cursor=(cursor+1).clip(0,string.size)};
								this.calcCharSizes;
								this.refresh;
								this.valueActions(\stringAction,this);
								if (model.notNil){ model.stringAction_(string,this) };
								stringAction.value(this,string);
							};
						}
					}{
						if ((canEdit)&&(keycode==36)) { // enter
							this.startEditing;
							this.refresh;
						};
					}
				}	
			}
		};
		
		view.keyUpAction_{ noKeyPresses=0 }
		
	}
	
	clipStringToMaxSize{ if (maxStringSize.isNumber) { string = string[0..(maxStringSize-1)] } }

	down_{|bool|
		down=bool;
		if (view.notClosed) {view.refresh}
	}
}
