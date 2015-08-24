
// LNX_MyPopUpMenu 
// also supports mapping and unmapping for the MVC_AudioOutSpec control spec 

MVC_PopUpMenu2 : MVC_View {

	initView{
		colors=colors++(
			'backgroundDisabled'	: Color.grey,
			'string'				: Color.black,
			'background'			: Color(1,1,0.9)
		);
		canFocus=true;
		items=items ? [""];
	}
	
	items_{|array|
		items=array.collect{|item| item.replace("(","") }
		           .collect{|item| (item=="-").if("",item) };

	}
	
		// create the gui's items that make this MVC_View
	create{|argWindow|
		if (view.isClosed) {
			if (argWindow.notNil) { window = argWindow };
			// the following order is important
			// DOES this happen any more? >> also makes sure the view overlaps number
			if ((numberFunc.notNil) and:{showNumberBox}) { this.createNumberGUI };
			this.createView;
			
			this.addControls;
			this.createLabel;
			this.dragControls;
			this.postCreate;
		}{
			"View already exists.".warn;
		}
	}
	
	
	createView{
			
		view=NSPopUpButton(window,rect,true);
		
		view.items_(items)
			.visible_(visible);
			
		view.initAction("doAction:");
	
	}

	addControls{
		var val;
		view.action_{|me|
			
			val=items.indexOfString(me.titleOfSelectedItem)-1;
			
			if (controlSpec.isKindOf(MVC_AudioOutSpec)) {
				this.viewValueAction_(controlSpec.unmap2(val),nil,true,false);
			}{
				this.viewValueAction_(val,nil,true,false);
			}
		};

	}
	
	dragControls{}
	
	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
//		if (view.notClosed) {
//			view.background_(colors[bool.if(\midiLearn,enabled.if(
//											\background,\backgroundDisabled))]);
//		};
		labelGUI.do(_.refresh);
	}
	
	// set the font
	font_{|argFont|
		font=argFont;
		//if (view.notClosed) { view.font_(font) };
	}

	// set the colour in the Dictionary
	color_{|key,color|
//		if (colors.includesKey(key).not) {^this}; // drop out
//		colors[key]=color;
//		if (key=='focus'      ) { {if (view.notClosed) { view.focusColor_(color ) } }.defer };		if (key=='string'     ) { {if (view.notClosed) { view.stringColor_(color) } }.defer };
//		if (key=='background' ) { {if (view.notClosed) { view.background_(color ) } }.defer };
	}
	

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		if (view.notClosed) {
//			view.background_(colors[enabled.if(\background,\backgroundDisabled)]);
		};
		labelGUI.do(_.refresh);
	}
		
	// item is enabled	
	enabled_{|bool|
		if (enabled!=bool) {
			enabled=bool;
			{
				if (view.notClosed) {
//					view.background_(colors[midiLearn.if(\midiLearn,
//						enabled.if(\background,\backgroundDisabled))])
//						.enabled_(enabled)
				}
			}.defer;
		}
	}
	
	// normally called from model
	value_{|val|
//		if (items.notNil) {
//			if (controlSpec.isKindOf(MVC_AudioOutSpec).not){
//				val=val.round(1).asInt.wrap(0,items.size-1);
//			}{
//				// this only works in some cases ??
//				// val=controlSpec.map2(val); // map to audio spec (this is all a bit hacky)
//			};
//		}{
//			value=val.asInt;
//		};
//		if (value!=val) {
//			value=val;
//			this.refreshValue;
//		};
	}	
	
	// fresh the menu value
	refreshValue{
//		if (view.notClosed) {
//			if (controlSpec.isKindOf(MVC_AudioOutSpec)or:
//					{controlSpec.isKindOf(MVC_AudioOutMasterSpec)}) {
//				view.value_(controlSpec.map2(value))
//			}{
//				view.value_(value)
//			}
//		}
	}
	
	// unlike SCView there is no refresh needed
	refresh{}

}
