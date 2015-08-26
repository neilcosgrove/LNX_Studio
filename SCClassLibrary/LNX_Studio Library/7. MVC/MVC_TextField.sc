
// LNX_MyTextView

// bug: text index after delete or typing

MVC_TextField : MVC_View {

	var <clearOnEnter=false, <align=\left, <>maxStringSize;

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
		view=MVC_SCTextField2(window,rect)
			.string_(string)
			.font_(font)
			.clearOnEnter_(clearOnEnter)
			.background_(colors[\background])
			.normalColor_(colors[\string])
			.stringColor_(colors[\string])
			.editColor_(colors[\edit])
			.align_(align)
			.usesTabToFocusNextView_(false);	
	}
	
	align_{|symbol|
		align=symbol;
		if (view.notClosed) {view.align_(align)};
	}
	
	// add the controls
	addControls{
		view.action_{|me|
				string=me.string;
				string=string.keep(maxStringSize?(string.size));
				this.valueActions(\stringAction,this);
				if (model.notNil){ model.stringAction_(string,this) };
				me.string_(string);
			}
			.enterAction_{|me|
				this.valueActions(\enterAction,this);
				if (model.notNil){ model.valueActions(\enterAction,model) };
			}
			.upAction_{|me|
				this.valueActions(\upAction,this);
				if (model.notNil){ model.valueActions(\upAction,model) };
			}
			.downAction_{|me|
				this.valueActions(\downAction,this);
				if (model.notNil){ model.valueActions(\downAction,model) };
			}
			.mouseUpAction_{|me|
				this.valueActions(\mouseUpAction,this);
				if (model.notNil){ model.valueActions(\mouseUpAction,model) };
			}
			.clearOnEnterAction_{|me| // internal
				string="";
				if (model.notNil){ model.string_("") }
			}
	}
	
	// set the colour in the Dictionary
	// need to do disable here
	color_{|index,color|
		colors[index]=color;
		if (index=='string'     ) { {if (view.notClosed) {
			view.stringColor_(color).normalColor_(color)
		} }.defer };
		if (index=='background' ) { {if (view.notClosed) { view.background_ (color)  } }.defer };
		if (index=='edit'       ) { {if (view.notClosed) { view.editColor_  (color)  } }.defer };
		if (index=='focus'      ) { {if (view.notClosed) { view.focusColor_ (color)  } }.defer };
	}
	
	// set the font
	font_{|argFont|
		font=argFont;
		if (view.notClosed) { view.font_(font) };
	}
	
	// set the string
	string_{|argString|
		string=argString.asString;
		if (view.notClosed) { view.string_(string) };
	}
	
	// the string will clear the contents after enter is press
	clearOnEnter_{|bool|
		clearOnEnter=bool;
		if (view.notClosed) { view.clearOnEnter_(clearOnEnter) };
	}
	
	// focus
	focus{ if (view.notClosed) {view.focus} }
	
	// unselected the range selected in view
	clearRangeSize{ if (view.notClosed) { view.clearRangeSize} }
	
	// reset text color to normal
	clearEditMode{ if (view.notClosed) { view.clearEditMode } }

}

/* Tests
(
m="".asModel
	.actions_(\stringAction,{|me| ("stringAction"+me.string).postln})
	.actions_(\enterAction,{|me| ("enterAction"+me.string).postln})
	.actions_(\upAction,{|me| ("upAction"+me.string).postln})
	.actions_(\downAction,{|me| ("downAction"+me.string).postln})
	.actions_(\mouseUpAction,{|me| ("mouseUpAction"+me.string).postln});
w=MVC_Window();
t = MVC_TextField(m,w,Rect(10,10,100,30));
MVC_TextField(m,w,Rect(10,50,100,30)).clearOnEnter_(true);
MVC_TextView(m,w,Rect(10,90,100,60));
w.create;
)
t.string = "test"
w.close;
w.open;
*/

// adapted TextView for use in MVC_TextView (not for direct use)
// action < all typing
// enterAction < only after enter
// upAction & downAction < up and down arrow keys

MVC_SCTextField : TextView {

	var <>enterAction;
	var <>upAction, <>downAction, <>normalColor;
	var <>editColor, <>isEditing=false;
	var <>clearOnEnter=true,	<>clearOnEnterAction;

	*viewClass { ^TextView } // this ensures that UserView's primitive is called

	init { arg argParent, argBounds;
		this.setParent(argParent.asView); // actual view
		this.background = Color.clear;
			// call asView again because parent by this point might be a FlowView
		this.prInit(this.parent.asView, argBounds.asRect,this.class.viewClass);
		argParent.add(this);//maybe window or viewadapter
		this.enterInterpretsSelection_(false)
			.font_(Font("Helvetica",12));
		normalColor=Color.black;
		editColor=Color.red;
	}
	
	value{^this.string}
	
	value_{|v| this.string=v}
	
	align_{} // this makes myTextView exchangable with myTextField
	
	keyDown { arg char, modifiers, unicode,keycode;
		var val;
		//[char, modifiers, unicode,keycode].postln;
		val=this.value;
		if (keycode==126) { upAction.value(this); this.stringColor_(normalColor); ^this}; //up
		if (keycode==125) { downAction.value(this); this.stringColor_(normalColor); ^this}; //down
		if((keycode==123)||(keycode==124)) {^this}; // left or right

		// enter
		if (keycode==36) {
			{
				//val=val.asFileSafeString; 
				//val=val.drop(-1); // remove enter
				this.string_(val);
				action.value(this);
				enterAction.value(this);
				if (clearOnEnter) {
					this.string_("");
					clearOnEnterAction.value(this);
				}{	
					this.stringColor_(normalColor);
					isEditing=false;
				};
			}.defer(0.05);
		}{
			if (clearOnEnter.not) {
				isEditing=true;
				this.stringColor_(editColor);
			};
			{action.value(this);}.defer(0.05);
		};
	}
	
	// unselected the range selected in view
	clearRangeSize{
		if (this.selectionSize>0) {
			this.string_(this.string);
		};
		^this
	}
	
	// reset text color to normal
	clearEditMode{
		if (isEditing) {
			this.stringColor_(normalColor);
			isEditing=false;
		};
		^this
	}

}

MVC_SCTextField2 : TextField {

	var <>enterAction;
	var <>upAction, <>downAction, <>normalColor;
	var <>editColor, <>isEditing=false;
	var <>clearOnEnter=false,	<>clearOnEnterAction;
	var <>usesTabToFocusNextView;

	var <>storedString="";

	*viewClass { ^TextField } // this ensures that UserView's primitive is called

	// fixes cmd-w bug
	getProperty { arg key, value;
		^this.getPropertyPrivate(key, value)
	}

	// fixes cmd-w bug
	getPropertyPrivate { arg key, value;
		_QObject_GetProperty
		^this.primitiveFailed
	}

	*new { arg argParent, argBounds;
		^super.new(argParent.asView, argBounds).initMVC_SCTextField2;
	}

	initMVC_SCTextField2 {
		this.background = Color.clear;
		this.font_(Font("Helvetica",12));
		normalColor=Color.black;
		editColor=Color.red;
	}

	string_ { arg aString;
		super.string_(aString);
		storedString = aString;
	}
	
	//string{^storedString}
	
	value{^this.string}
	
	value_{|v|
		this.string=v;
		storedString=v;
	}
	
	align_{} // this makes myTextView exchangable with myTextField
	
	keyDown { arg char, modifiers, unicode, keycode, key;
		var val, tempS;
		//[char, modifiers, unicode,keycode].postln;
			
		val=this.value;
		if (keycode==126) {
			upAction.value(this);
			this.stringColor_(normalColor);
			this.string_(storedString);
			^this
		}; //up
		if (keycode==125) {
			downAction.value(this);
			this.stringColor_(normalColor); 
			this.string_(storedString);
			^this
		}; //down
		if((keycode==123)||(keycode==124)) {^this}; // left or right

		// enter
		if (keycode==36) {			
			val=storedString;
			{
				//val=val.asFileSafeString; 
				//val=val.drop(-1); // remove enter
				this.string_(val);
				action.value(this);
				enterAction.value(this);
				if (clearOnEnter) {
					this.string_("");
					clearOnEnterAction.value(this);
				}{	
					this.stringColor_(normalColor);
					isEditing=false;
				};
			}.defer(0.05);
		}{
			if (clearOnEnter.not) {
				isEditing=true;
				this.stringColor_(editColor);
			};
			{
				tempS=this.value;
				if (tempS.select{|c| c.ascii<0}.size>0) {
					this.string_(storedString);
				}{	
					action.value(this);
					storedString=this.value.select{|c| c.ascii>0};
				}
			}.defer(0.05);
		};
	}
	
	// you can do selection on SCTextField
	selectionSize{^this.size}
	
	// unselected the range selected in view
	clearRangeSize{
		if (this.selectionSize>0) {
			this.string_(this.string);
		};
	}
	
	// reset text color to normal
	clearEditMode{
		if (isEditing) {
			this.stringColor_(normalColor);
			isEditing=false;
		};
	}

}

/*
(
w = MVC_Window();
)
*/