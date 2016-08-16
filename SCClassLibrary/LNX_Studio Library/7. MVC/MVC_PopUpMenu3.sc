/*
(
w=MVC_Window(scroll:true);
m=MVC_PopUpMenu3(w,Rect(20,20,100,20))
	.items_(["A","-","b","cC","gG","ABCDEfghIJKgw fwef ewf ","1","2","3","4","5","6","7"])
	.menuFont_(Font("Helvetica",15));
MVC_PopUpMenu3(w,Rect(60,60,100,20))
	.items_(["A","-","b","cC","gG","ABCDEfghIJKgw fwef ewf ","1","2","3","4","5","6","7"])
	.menuFont_(Font("Helvetica",15));
MVC_PopUpMenu3(w,Rect(60,60,100,20))
	.items_(["|....................."])
	.menuFont_(Font("Helvetica",15));
w.create;
)
*/
// LNX_MyPopUpMenu
// also supports mapping and unmapping for the MVC_AudioOutSpec control spec

MVC_PopUpMenu3 : MVC_View {

	var indexValue=0, <>menuFont, <>style=0, <>staticText, <>showTick=true, <down=false,
		<startTime, <textView, <menuRect, <menuWindow, <>updateFunc;
	var <highlight = 1;

	var <>isPOPcanTurnOnOff = false;

	highlight_{|number|
		highlight=number;
		{ if (view.notClosed) { view.refresh } }.defer;
	}

	initView{
		colors=colors++(
			'backgroundDisabled'	: Color.grey,
			'string'				: Color.black,
			'background'			: Color(1,1,0.9),
			'border'				: Color.black

		);
		canFocus=true;
		items=items ? [];

		// @TODO: standardise how we get fonts
		if (Font.availableFonts.find("HelveticaNeuew").notNil) {
			menuFont = Font("HelveticaNeue",15);
		}{
			menuFont = Font.sansSerif(15);
		};
	}

	items_{|array|
		items = array; // use ( and - for disable and break
		this.refresh;
	}

	createView{
		view=UserView.new(window,rect)
			.drawFunc_{|me|
				var r1=Rect(0,0,w,h);
				var r2=Rect(0,2,h-3,h-3);
				var r3=Rect(h-7,0,w-h+5,h-1);
				MVC_LazyRefresh.incRefresh;
				Pen.use{
					Pen.smoothing_(false);
					if (midiLearn) {
						colors[\midiLearn].set;
					}{
						(colors[\background]*(highlight)).set;
					};
					Pen.fillRect(r1);

					if(style==0) {
						Color(1,1,1,0.5).set;
						Pen.moveTo(0@h);
						Pen.lineTo(0@1);
						Pen.lineTo(w@1);
						Pen.stroke;
						Color(0,0,0,0.5).set;
						Pen.moveTo(0@h);
						Pen.lineTo((w-1)@h);
						Pen.lineTo((w-1)@0);
						Pen.stroke;
					}{
						colors[\border].set;
						Pen.strokeRect(r1);
						Pen.strokeRect(r2.insetBy(2));
					};

					if (midiLearn) {
						Color.black.set;
						Pen.fillColor_(Color.black);
					}{
						colors[\string].set;
						Pen.fillColor_(colors[\string]);
					};
					// icon
					Pen.smoothing_(true);
					DrawIcon(\down, r2.moveBy(0,-1));
					// and text

					if (isPOPcanTurnOnOff) {
						if (value==0) {
							Pen.width_(1);
							Color(0,0,0).set;
							Pen.strokeRect(r3.insetBy(9,3));
							Color(0,0,0,0.2).set;
							Pen.fillRect(r3.insetBy(10,4));
						};
						if (value==1) {

							Pen.smoothing_(false);
							Pen.width_(1);
																			Color(1,1,1,0.5).set;
							Pen.fillRect(r3.insetBy(15,3).resizeBy(0,1));
							Color(1,1,1).set;
							Pen.strokeRect(r3.insetBy(14,3).resizeBy(-1,1));

						};
					};

					Pen.moveTo((h-7)@0);
					Pen.lineTo((w-2)@0);
					Pen.lineTo((w-2)@h);
					Pen.lineTo((h-7)@h);
					Pen.clip;

					Pen.font_(font);

					Pen.smoothing_(true);

					if (((staticText ? (items[indexValue]) ? "").bounds(font).width)>(w-h)) {
						Pen.stringLeftJustIn (staticText ? (items[indexValue]) ? "", r3);
					}{
						Pen.stringCenteredIn (staticText ? (items[indexValue]) ? "", r3);
					};
				};

				updateFunc.value;

			}
	}

	openMenu{
		var summary, summaryList;
		var selectView, selected, class, sB, rect;
		var menuList, pop, task, used;
		var w,h;

		if (menuWindow.notNil) { menuWindow.close };

		onClose={ if (menuWindow.notNil) { menuWindow.close } };

		if (items.size>0) {

			var mousePos, moveY, rect2;

			sB = items.collect{|item| GUI.stringBounds(item,menuFont).rightBottom.asArray };
			w = sB.collect(_.first).sort.last+4+40;
			h = sB.first.last;

			mousePos = Rect(GUI.cursorPosition.x, GUI.cursorPosition.y, 0, 0).convert;

			menuRect=rect=Rect(
				mousePos.left,
				mousePos.top,
				w+4, h*(items.size)+4
			).convert;

			// resize and adjust position for size of screen
			rect2 = rect.copy;
			// rect2 = rect2.height_(rect2.height.clip(0,Window.screenBounds.height-60)) ;
			// moveY = (Window.screenBounds.height - rect2.top - rect2.height-80).clip(-inf,0);
			// rect2 = rect2.moveBy(0,moveY);

			// the window
			menuWindow=MVC_Window("", rect2, border:false, scroll:true);
			// to help fix mouseOver bug
			task=Task({{
				if (menuWindow.isClosed.not) {
					menuWindow.acceptsMouseOver_(false);
					menuWindow.acceptsMouseOver_(true);
					menuWindow.front;
					// and also front window after menu item not selected
				};
				if (menuWindow.view.view.absoluteBounds.contains(GUI.cursorPosition).not) {
					// not in bounds
					if (selected.notNil){
						selected=nil;
						selectView.refresh;
					};
					0.1.wait;
				}{
					if (down) {
						var cord = GUI.cursorPosition.convert-(menuWindow.bounds.leftTop);
						textView.mouseOverAction.value(textView, cord.x, cord.y);
						0.05.wait;
					}{
						0.1.wait;
					};
				};
			}.loop},AppClock).start;

			// the border
			MVC_PlainSquare(menuWindow,Rect(0,0,w+3,1))
				.color_(\on,Color(1,1,1))
				.color_(\off,Color(1,1,1));
			MVC_PlainSquare(menuWindow,Rect(0,0,1,rect.bounds.height))
				.color_(\on,Color(1,1,1,0.77))
				.color_(\off,Color(1,1,1,0.77));
			MVC_PlainSquare(menuWindow,Rect(0,rect.bounds.height-1,w+3,1))
				.color_(\on,Color(0,0,0,0.66))
				.color_(\off,Color(0,0,0,0.66));
			MVC_PlainSquare(menuWindow,Rect(w+3,0,1,rect.bounds.height))
				.color_(\on,Color(0,0,0,0.66))
				.color_(\off,Color(0,0,0,0.66));
			MVC_PlainSquare(menuWindow,Rect(1,1,w+3-2,1))
				.color_(\on,Color(1,1,1,0.5))
				.color_(\off,Color(1,1,1,0.5));
			MVC_PlainSquare(menuWindow,Rect(1,1,1,rect.bounds.height-2))
				.color_(\on,Color(1,1,1,0.5))
				.color_(\off,Color(1,1,1,0.5));
			MVC_PlainSquare(menuWindow,Rect(1,rect.bounds.height-2,w+1,1))
				.color_(\on,Color(0,0,0,0.33))
				.color_(\off,Color(0,0,0,0.33));
			MVC_PlainSquare(menuWindow,Rect(w+2,1,1,rect.bounds.height-2))
				.color_(\on,Color(0,0,0,0.33))
				.color_(\off,Color(0,0,0,0.33));

			// the mouse over selection view
			selectView=MVC_UserView(menuWindow,Rect(0,1,w+3,h*(items.size)))
				.drawFunc_{|me|
					var bounds=me.bounds;

					if ((selected.notNil)and:{items[selected]!="-"}){
						Pen.use{
							Pen.smoothing_(false);
							Color(0.25,0.5,1).set;
							Pen.fillRect(Rect(0,selected*h,w+4,h));
						};

					};

					if ((showTick)&&(value.notNil)) {
						var indexValue;
						if (controlSpec.isKindOf(MVC_AudioOutSpec)or:
							{controlSpec.isKindOf(MVC_AudioOutMasterSpec)}) {
							indexValue = controlSpec.map2(value);
						}{
							indexValue = value.clip(0,items.size-1);
						};
						Pen.smoothing_(true);
						Pen.fillColor_(Color.black);
						Pen.font_(menuFont);
						Pen.stringLeftJustIn(" *", Rect(2,indexValue*h+1,w+4,h));
					};

					items.do{|t,i|
						if (t=="-") {
							Color(0,0,0,0.25).set;
							Pen.moveTo(1@((i+0.5)*h));
							Pen.lineTo((w+2)@((i+0.5)*h));
							Pen.stroke;
							Color(1,1,1,0.5).set;
							Pen.moveTo(1@(((i+0.5)*h)+1));
							Pen.lineTo((w+2)@(((i+0.5)*h)+1));
							Pen.stroke;
						};
					};
				};


			// textView=MVC_View(menuWindow,Rect(22,1,w-20,h*(items.size)))
			// 	.color_(\background, Color.clear)

			items.do {|s, i|
				textView = MVC_StaticText(menuWindow, Rect(22,2+(h*i),w-20,h))
					.shadow_(false)
					.string_(
						if (s[0]==$() { s=s.drop(1) };
						(s=="-").if {""} {s.asString};
					)
					.font_(menuFont)
					.color_(\string,Color.black)
			};

			MVC_StaticText(menuWindow,Rect(0,1,w+3,h*(items.size)))
				.string_("")
				.mouseOverAction_{|me, x, y|
					var val = (y/h).asInt.clip(0,items.size-1);
					if (selected!=val) {
						selected=val;		 // set index
						selectView.refresh; // refresh the view
					};
				}
				.mouseUpAction_{|me, x, y|
					// is something currently selected?
					if (selected.notNil) {
						// get index
						value=(y/h).asInt.clip(0,items.size-1);
						// close the window
						task.stop;
						menuWindow.free;
						this.refreshValue;
						// call action
						if (controlSpec.isKindOf(MVC_AudioOutSpec)) {
							this.viewValueAction_(controlSpec.unmap2(value),nil,true,false);
						}{
							this.viewValueAction_(value,nil,true,false);
						};
					}{
						// just close
						task.stop;
						menuWindow.free;
					};
				};

			menuWindow.create;
			menuWindow.view.alpha_(0.95);

			// must be done here
			menuWindow.view.endFrontAction_{
				task.stop;
				menuWindow.free;
			};

		};

	}

	addControls{
		view.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			if (modifiers==524576) { buttonNumber=1 };
			if (modifiers==262401) { buttonNumber=2 };
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) {
				lw=lh=nil;
				startX=x;
				startY=y;
			}{
				if (buttonNumber==2) {
					this.toggleMIDIactive
				}{
					if (modifiers.isXCmd.not) { this.openMenu };
				};
				down=true;
				startTime = SystemClock.now;
			};

		}
		.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if (editMode) { this.moveBy(x-startX,y-startY) }
		}
		.mouseUpAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			down = false;
			if (editMode.not) {
				if (SystemClock.now-startTime>0.5) {
					var cord = GUI.cursorPosition.convert-(menuWindow.bounds.leftTop);
					textView.mouseUpAction.value(textView, cord.x, cord.y);
				};
			};
		}
	}

	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		this.refresh;
		labelGUI.do(_.refresh);
	}

	// set the font
	font_{|argFont|
		font=argFont;
		this.refresh;
	}

	// set the colour in the Dictionary
	color_{|key,color|
		if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		this.refresh;
	}

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		labelGUI.do(_.refresh);
		this.refresh;
	}

	// item is enabled
	enabled_{|bool|
		enabled=bool;
		this.refresh;
	}

	// normally called from model
	value_{|val|

		if (items.notNil) {
			if (controlSpec.isKindOf(MVC_AudioOutSpec).not){
				val=val.round(1).asInt.wrap(0,items.size-1);
			}{
				// this only works in some cases ??
				// val=controlSpec.map2(val); // map to audio spec (this is all a bit hacky)
			};
		}{
			value=val.asInt;
		};
		if (value!=val) {
			value=val;
			this.refreshValue;
		};
	}

	// fresh the menu value
	refreshValue{
		if (controlSpec.isKindOf(MVC_AudioOutSpec)or:
				{controlSpec.isKindOf(MVC_AudioOutMasterSpec)}) {
			indexValue = controlSpec.map2(value);
		}{
			indexValue = value.clip(0,items.size-1);
		};

		{ if (view.notClosed) { view.refresh } }.defer;
	}

	// unlike SCView there is no refresh needed
	refresh{ this.refreshValue }

}
