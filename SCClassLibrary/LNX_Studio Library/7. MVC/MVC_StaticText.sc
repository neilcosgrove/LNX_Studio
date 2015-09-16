/*
(
w=MVC_Window().create;

v=MVC_Text(w,Rect(20,20,200,14))
	.string_("testing testing 123\r\n1234567890")
	.canEdit_(true)
	.enterStopsEditing_(false)
	.stringAction_{|me,string|
		string.postln;
	}
	.enterKeyAction_{|me,string|
		string.postln;
		//me.string_("");
	}
	.color_(\string,Color(59/77,59/77,59/77)*1.4)
	.color_(\edit,Color(59/77,59/77,59/77)*1.4)
	.color_(\background,Color(0.14,0.12,0.11)*0.4)
	.color_(\focus,Color.orange)
	.color_(\editBackground, Color(0,0,0,0.7))
	.font_(Font.new("STXihei", 12));
	
)


v.charSizesIntegral

*/

// StaticText or a label
// idea: add a prefix and postfix

MVC_Text : MVC_StaticText {}

MVC_StaticText : MVC_View {

	var <align='left', <shadow=true, <>noShadows=1, <rotate=0, <down=false, <>shadowDown=true, 
		clicks, <>downColor, <>active=true, <>alwaysDown=false;
	var <>excludeFromVerbose=false;
	var <>keyDownAction, <>keyUpAction,
	    <>upKeyAction, <>downKeyAction, <>enterKeyAction, <>stringAction;
	var <charSizes, <charSizesIntegral, <clipChars=false;
	var <clipString, <>hasBorder=false;
	var <editing=false, <cursor=nil, <cursorFlash=false, <canEdit=false;
	var <>enterStopsEditing = true;
	var <>rightIfIcon=false;
	var <>tasks;
	var <>penShadow=false;
	
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
	
	clipChars_{|bool|
		clipChars=bool;
		this.calcCharSizes;
	}
	
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
		
	startEditing{
		editing=true; 
		this.calcCharSizes;
		tasks[\cursorFlash] = {{
			cursorFlash = cursorFlash.not;
			this.refresh;
			0.6.wait;
		}.loop}.fork(AppClock);
	}
	
	stopEditing{
		editing=false; 
		tasks[\cursorFlash].stop;
		tasks[\cursorFlash] = nil;
	}
	
	// set your defaults
	initView{
		charSizes=[];
		charSizesIntegral=[];
		colors['icon'] = nil;
		
		colors['background']= Color.clear;
		colors['editBackground']=  Color(0,0,0,0.8);
		colors['string']= Color.white;
		colors['stringDown']= Color.black;
		colors['edit']=  Color.orange;
		colors['border'] = Color(0,0,0,0.5);
		colors['cursor'] = Color(0,0,0);
		
		canFocus=true;
		
		tasks = IdentityDictionary[];
	}
	
	// make the view
	createView{
		var r1, align2;
		
		view=UserView(window,rect)
			.canFocus_(true)
			.drawFunc_{|me|

	
				if (me.hasFocus.not) {this.stopEditing};

				if (verbose && (excludeFromVerbose.not)) {
					[this.class.asString, 'drawFunc', label].postln };
			
				r1=Rect(0,0,w,h);
				Pen.use{
					//Pen.smoothing_(false);
					if (colors[\background].notNil) {
						colors[\background].set;
						
						if (editing) { colors['editBackground'].set};
						
						Pen.fillRect(r1);
					};
					if (showLabelBackground) {
						Color.black.alpha_(0.2).set;
						Pen.fillRect(r1);
					};
					
					if (hasBorder) {
						colors[\border].set;
						Pen.strokeRect(r1);
					};
					
					align2=align;
					
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
						\disabled)
						]);
						
					};
					if (editing) { Pen.fillColor_(colors[\edit])};
					
					if (penShadow) {
						Pen.setShadow((-2)@(-2), 5, Color.black);
					};

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
						
					if (editing&&cursorFlash) {
					
						var x = ([0]++charSizesIntegral).last+3;
						
						if (align=='left') { x=x };
						if (align=='center') { x= (x+w)/2 };
						Pen.smoothing_(false);
						Pen.strokeColor_(colors[\cursor]);
						Pen.line((x@1),(x@(h-2)));
						Pen.stroke;
						
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
	
	
	align_{|symbol|
		align=symbol;
		if (view.notClosed) { view.refresh };
	}


	rotate_{|angle|
		rotate=angle;
		if (view.notClosed) { view.refresh };
	}
	
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
					//buttonPressed=buttonNumber;
					evaluateAction=true;
					//if (modifiers==524576) { buttonPressed=1 };
					//if (modifiers==262401) { buttonNumber=2 };
					//buttonPressed=buttonNumber;
					
					if (buttonNumber==2) {
						this.toggleMIDIactive
					}{
						down=true;
					//	view.refresh;
					};
				}
			};
			
			this.refresh
			
		};
		
		view.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			
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
					if (down) {
						if (alwaysDown.not) {
							down=false;
							view.refresh;
						};
					}
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
				this.startEditing;
				this.refresh
			}; 
			
			if (alwaysDown) { this.refresh };
			
		};
		
		// @TODO: new Qt "key" codes
		view.keyDownAction_{|me,char,mod,uni,keycode,key|
			
			// help fixes double press bug by see if cmd is pressed
			if (mod.isCmd) { noKeyPresses=noKeyPresses+1 }{ noKeyPresses=1 };
			
			if (noKeyPresses.odd) { // stops double press bug
				
				keyDownAction.value(me,char,mod,uni,keycode);
				
				if (mod.isCmd) { // apple cmd key
					
					if (keycode==8) { ("echo"+string+"| pbcopy").unixCmd }; // copy
					
					if (editing) {
						case {keycode==9} { //paste
							string=string++("pbpaste".unixCmdGetStdOut);
							
							if (string.last==$\n) {string=string.drop(-1)};
							
							this.calcCharSizes;
							this.refresh;
							this.valueActions(\stringAction,this);
							if (model.notNil){ model.stringAction_(string,this) };
					
						}{keycode==51} { // delete
							string="";
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
					};
		
					if (editing) {
							
						case {keycode==51} { // delete
							string=string.drop(-1);
							this.calcCharSizes;
							this.refresh;
							this.valueActions(\stringAction,this);
							if (model.notNil){ model.stringAction_(string,this) };
							stringAction.value(this,string);
		
						} {keycode==36} { // enter
							if (enterStopsEditing) {this.stopEditing};
							this.refresh;
							enterKeyAction.value(this,string);
						
						}{ 		
							if ((char.isAlphaNum)||(char.isPunct)||(char.isSpace)) {
								// alphaNum etc..
								string=string++char;
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

	down_{|bool|
		down=bool;
		if (view.notClosed) {view.refresh}
	}
}
