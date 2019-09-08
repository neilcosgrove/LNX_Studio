/*
(
w=MVC_Window(scroll:true);
m=[0,[0,1],{|me|
v=MVC_PopUpMenu4(w,Rect(20,20,100,20))
	.items_(["A","-","b","cC","gG","ABCDEfghIJKgw fwef ewf ","1","2","3","4","5","6","7"]);
w.create;
)
*/
// LNX_MyPopUpMenu
// also supports mapping and unmapping for the MVC_AudioOutSpec control spec

// I might try out the new menus at some point...

MVC_PopUpMenu4 : MVC_PopUpMenu3 {}

MVC_PopUpMenu5 : MVC_View {

	var indexValue=0, <>menuFont, <>style=0, <>staticText, <>showTick=true, <down=false,
		<startTime, <textView, <menuRect, <menuWindow, <>updateFunc, <>menuOpen=false;
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
			menuFont = Font("HelveticaNeue",14,true);
		}{
			menuFont = Font.sansSerif(14,true);
		};
	}

	items_{|array|
		items = array; // use ( and - for disable and break

		if (view.notClosed) { view.items_(items) };

	}

	createView{
		view=PopUpMenu.new(window,rect)
		.items_(items)
		.value_(value)
		.font_(menuFont)
		.stringColor_(colors[\string])
		.background_(colors[\background])
		.action_{|me|
			var val = me.value;
			if (val!=value) { this.viewValueAction_(val,nil,true,false) };
		};
	}

	addControls{

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

		{ if (view.notClosed) {
			view.value_(value)

		} }.defer;
	}

	// unlike SCView there is no refresh needed
	refresh{ this.refreshValue }

}
