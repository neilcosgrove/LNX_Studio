
// LNX_MyTextView
// Also supports UGen menu (Apple click or middle mouse click on "Object" in text)

MVC_TextView : MVC_View {

	classvar <menus;

	var <>colorizeOnOpen = false, <>autoColorize=false, helpWindow, task, parentWindow, menuXY;

	// set your defaults
	initView{
		colors=colors++(
			'background'	: Color.white,
			'string'      : Color.black,
			'edit'        : Color.red
		);
		canFocus=true;
		if (string.isNil) {string=""};
		font=Font("Monaco",9);
	}

	// make the view
	createView{
		view=MVC_SCTextView.new(window,rect)
			.string_(string)
			.font_ (font)
			.background_(colors[\background])
			.stringColor_(colors[\string])
			.usesTabToFocusNextView_(false)
			.hasVerticalScroller_ (true);

		if (colorizeOnOpen) {view.syntaxColorize};
	}

	// add the controls
	addControls{
		view.action_{|me|
				string=me.string;
				this.valueActions(\stringAction,this);
				if (model.notNil){ model.stringAction_(string,this) };
				if (autoColorize) {view.syntaxColorize};
			}
			.enterAction_{|me|
				this.valueActions(\enterAction,this);
				if (model.notNil){ model.valueActions(\enterAction,model) };
				if (autoColorize) {view.syntaxColorize};
			}
			.mouseUpAction_{|me,x, y, modifiers, buttonNumber, clickCount, clickPos|
				this.valueActions(\mouseUpAction,
					this,x, y, modifiers, buttonNumber, clickCount, clickPos);
				if (model.notNil){ model.valueActions(\mouseUpAction,
					this,x, y, modifiers, buttonNumber, clickCount, clickPos) };
			}
	}

	// set the font
	font_{|argFont|
		font=argFont;
		if (view.notClosed) { view.font_(font) };
	}

	// set the string
	string_{|argString|
		string=argString.asString;
		if (view.notClosed) {
			view.string_(string);
			if (autoColorize) {view.syntaxColorize};
		};
		this.refresh;
	}

	string{ if (view.notClosed) { ^view.string }{ ^string } }

	selectedString{ if (view.notClosed) { ^view.selectedString }{ ^"" } }

	setString{|string, rangestart=0, rangesize=0|
		if (view.notClosed) {
			view.setString(string, rangestart, rangesize);
			if (autoColorize) {view.syntaxColorize};
			string=view.string;
		}
	}

	setStringAction{|string, rangestart=0, rangesize=0|
		if (view.notClosed) {
			view.setString(string, rangestart, rangesize);
			if (autoColorize) {view.syntaxColorize};
			string=view.string;
			this.valueActions(\stringAction,this);
			if (model.notNil){ model.stringAction_(string,this) };
		}
	}

	// set the colour in the Dictionary
	color_{|index,color|
		colors[index]=color;
		if (index=='string'     ) { {if (view.notClosed) { view.stringColor_(color)  } }.defer };
		if (index=='background' ) { {if (view.notClosed) { view.background_ (color)  } }.defer };
		if (index=='focus'      ) { {if (view.notClosed) { view.focusColor_ (color)  } }.defer };
	}

	focus{ if (view.notClosed) {view.focus} }

	// unselected the range selected in view
	clearRangeSize{ if (view.notClosed) { view.clearRangeSize} }

	// colorize it
	syntaxColorize{ if (view.notClosed) {view.syntaxColorize} }

	// UGen Menu support //////////////////////////////////////////////////////////////////

	// init widget, extracts UGen names from Help.tree for use in UGen help menus
	*initClass{
		var function;
		Class.initClassTree(Help);
		menus=[];
/*		// function call calls itself every time a new branch is found else add item to list
		function={|dict|
			var list=[];
			dict.pairsDo{|key,item|
				if (item.isKindOf(Dictionary)) {
					// found a new branch so call this function
					function.value(item)
				}{
					// found an item so add it
					if (key.asSymbol.asClass.notNil) {
						list=list.add(key.asSymbol);
					};
				};
			};
			// add this list to the list of menus
			menus=menus.add(list);
		};
		// call the above funtion on UGens
		Platform.case(
			\osx, { function.value(Help.global.tree["[[UGens]]"]) }
		);*/
	}

	// get a list of menus that contain symbol
	*getMenuList{|symbol|
		var index=[];
		menus.do{|i,j|
			var idx=i.indexOf(symbol);
			if (idx.notNil) {index=index.add(j)}
		};
		^index.collect{|i| menus[i] }.flat.asSet.asList.sort
	}

	// make UGen active, need to supply the window and an x,y offset
	attachCodeHelpFunction{|argParentWindow,x,y|
		parentWindow=argParentWindow;
		menuXY=x@y;
		this.actions_(\mouseUpAction, {|me,x, y, modifiers, buttonNumber, clickCount, clickPos|
			this.codeHelpFunction(x, y, modifiers, buttonNumber, clickCount, clickPos)}
		)
	}

	// the UGen help
	codeHelpFunction {|x, y, modifiers, buttonNumber, clickCount, clickPos|
		var string = view.selectedString;
		var summary, summaryList;
		var textView, selectView, selected, class, sB, rect;
		var menuList, pop;

		// stop previous windows, just in case
		task.stop;
		helpWindow.free;

		menuXY = menuXY ? (0@0);

		// start is selectionSize>0	and have mods
		if (( view.selectionSize>0)&&(
				(modifiers==1048840) || (modifiers==1114376) || (buttonNumber!=0)
			)) {
				string=string.split($.)[0]; // .split($.)[0] for later OS default
				class=string.asSymbol.asClass;
				menuList=this.class.getMenuList(string.asSymbol); // get the menus
				summary = class.getClassArgsSummaryHelp;     // get the summary from the class
				summaryList = class.getClassArgsSummaryList; // get the summary from the class

				// summary might be nil if not a class or doesn't support .new .ar .kr .ir
				if (summary.notNil) {
					// the string bounds
					sB = GUI.stringBounds(summary,Font("Helvetica-Bold",14));
					// get rect for window
					Platform.case(\osx,{
						rect=Rect(
							OSXPlatform.getMouseCoords.x-7,
							OSXPlatform.getMouseCoords.convert.y+8,
							sB.width+5+20, sB.height+2
						);
					},{
						if (LNX_MouseXY.active) {
							rect=Rect(
								(LNX_MouseXY.pos.x).clip(
									parentWindow.bounds.left,parentWindow.bounds.right),
								(LNX_MouseXY.pos.y-33).clip(
									parentWindow.bounds.top,parentWindow.bounds.bottom),
								sB.width+5+20, sB.height+2)
						}{
							rect=Rect(
								(parentWindow.bounds.left)+(this.bounds.left)+x+(menuXY.x),
								(parentWindow.bounds.top)+(this.bounds.top)+(
										y.clip(0,this.bounds.height))+(menuXY.y),
								sB.width+5+20, sB.height+2)
						};
					});

					// the window
					helpWindow=MVC_Window(string, rect , border:false);
					// to help fix mouseOver bug
					task=Task({{
						if (helpWindow.isClosed.not) {
							helpWindow.acceptsMouseOver_(false);
							helpWindow.acceptsMouseOver_(true);
							helpWindow.front;
							// and also front window after menu item not selected
						};
						0.5.wait;
					}.loop},AppClock).start;

					// the mouse over selection view
					selectView=MVC_UserView(helpWindow,Rect(20,0,sB.width+5, sB.height+1))
						.drawFunc_{|me|
							var bounds=me.bounds;
							if (selected.notNil){
								Pen.use{
									Pen.smoothing_(false);
									Color(0,0.5,1,0.3).set;
									Pen.fillRect(Rect(0,selected*19,sB.width+5,19));
								}
							}
						};

					// the summary text view, overlaps selectView
					textView=MVC_StaticText(helpWindow,Rect(20,0,sB.width+5, sB.height+1))
						.shadow_(false)
						.string_(summary)
						.font_(Font("Helvetica-Bold",14))
						.color_(\background,Color(0,0,0,0.05))
						.color_(\string,Color.black)
						.mouseOverAction_{|me, x, y|
							var val = (y/19).asInt.clip(0,summaryList.size-1); // get index

							//val.postln;

							if (selected!=val) {
								selected=val;		 // set index
								selectView.refresh; // refresh the view
							};
						}
						.mouseUpAction_{|me, x, y|

							// get index
							selected=(y/19).asInt.clip(0,summaryList.size-1);
							// set the string
							this.setStringAction(summaryList|@|(selected),view.selectionStart,
								view.selectionSize);
							// close the window
							task.stop;
							helpWindow.free;
						};

						// the pop up menu will other suggestions
						pop=MVC_PopUpMenu(helpWindow,Rect(1,1,18,sB.height))
							.items_(
								["("++string  ++" : "++
								((class.categories.collect{|c|
									c.split($>)[1]++", "}?"").join.drop(-2))
								, "Help","--",]++
								menuList.collect(_.asString)
							)
							.action_{|me|
								var path;
								if (me.value==1) {
									// close the window
									task.stop;
									helpWindow.free;
									// open the help file
									path=string.findHelpFile;
									if (path.notNil) {
										// this crashes without the defer
										{
											class.help;
										}.defer(0.5);
									};
								}{
									if (me.value>=3) {
										// close the window
										task.stop;
										helpWindow.free;
										// set the string
										this.setStringAction(menuList[me.value-3].asString,
											view.selectionStart,string.size);
									}
								};
							}
							.color_(\background,Color(0,0,0,0))
							.value_(
								(menuList.indexOf(string.asSymbol)?(-3))+3
							);
					// create the window
					helpWindow.create;
					// must be done here
					helpWindow.view.endFrontAction_{
						task.stop;
						helpWindow.free;
					};

				};
		}
	}


}

//// adapted TextView for use in MVC_TextView (not for direct use)

MVC_SCTextView : TextView {

	var <>enterAction;

	*viewClass { ^TextView } // this ensures that UserView's primitive is called

	initView { arg argParent;
		super.initView(argParent); // actual view
		this.background = Color.clear;
		this.enterInterpretsSelection_(false)
			.font_(Font("Helvetica",12))
			.keyDownAction_{|char, modifiers, unicode, keycode, key|
				this.onKeyDown(char, modifiers, unicode, keycode, key)};
	}

	value{^this.string}

	value_{|v| this.string=v}

	align_{} // this makes myTextView exchangable with myTextField

	onKeyDown {|char, modifiers, unicode, keycode, key|
		if (key.isArrow.not) {
			{
				action.value(this);
				if (key.isEnter) {
					enterAction.value(this);
				}; // enter action. (put here incase of paste)
			}.defer(0.05);
		};
	}

	// unselected the range selected in view
	clearRangeSize{
		if (this.selectionSize>0) {
			this.string_(this.string);
		};
		^this
	}

}
