
// LNX_MyPopUpMenu 
// also supports mapping and unmapping for the MVC_AudioOutSpec control spec 

MVC_PopUpMenu : MVC_View {

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
		if (view.notClosed) { view.items_(items) };
		// this.value_(value); // this is causing problems with samples in gsR
		// can't see any bad side effects of removing this 
	}
	
	createView{
		view = PopUpMenu.new(window,rect)
			.background_(colors[midiLearn.if(\midiLearn,
						enabled.if(\background,\backgroundDisabled))])
			.items_(items)
			
			.enabled_(enabled)
			.stringColor_(colors[\string])
			.font_(font);
			
		if (controlSpec.isKindOf(MVC_AudioOutSpec)) {
			view.value_(controlSpec.map2(value))
		}{
			view.value_(value)
		};		
	}

	addControls{
		var val, val2;
		view.action_{|me|
			if (controlSpec.isKindOf(MVC_AudioOutSpec)) {
				this.viewValueAction_(controlSpec.unmap2(me.value.asInt),nil,true,false);
			}{
				this.viewValueAction_(me.value.asInt,nil,true,false);
			}
		};
		view.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			if (modifiers==524576) { buttonNumber=1 };
			if (modifiers==262401) { buttonNumber=2 };
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) {
				lw=lh=nil;
				startX=x;
				startY=y;
				view.bounds.postln;
			};	
			if (buttonNumber==2) { this.toggleMIDIactive };
		};		
	}
	
	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		if (view.notClosed) {
			view.background_(colors[bool.if(\midiLearn,enabled.if(
											\background,\backgroundDisabled))]);
		};
		labelGUI.do(_.refresh);
	}
	
	// set the font
	font_{|argFont|
		font=argFont;
		if (view.notClosed) { view.font_(font) };
	}

	// set the colour in the Dictionary
	color_{|key,color|
		if (colors.includesKey(key).not) {^this}; // drop out
		colors[key]=color;
		if (key=='focus'      ) { {if (view.notClosed) { view.focusColor_(color ) } }.defer };		if (key=='string'     ) { {if (view.notClosed) { view.stringColor_(color) } }.defer };
		if (key=='background' ) { {if (view.notClosed) { view.background_(color ) } }.defer };
	}
	

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		if (view.notClosed) {
			view.background_(colors[enabled.if(\background,\backgroundDisabled)]);
		};
		labelGUI.do(_.refresh);
	}
		
	// item is enabled	
	enabled_{|bool|
		if (enabled!=bool) {
			enabled=bool;
			{
				if (view.notClosed) {
					view.background_(colors[midiLearn.if(\midiLearn,
						enabled.if(\background,\backgroundDisabled))])
						.enabled_(enabled)
				}
			}.defer;
		}
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
				
		if (view.notClosed) {
			if (controlSpec.isKindOf(MVC_AudioOutSpec)or:
					{controlSpec.isKindOf(MVC_AudioOutMasterSpec)}) {
				view.value_(controlSpec.map2(value))
			}{
				view.value_(value.clip(0,items.size-1))
			}
		}
	}
	
	// unlike SCView there is no refresh needed
	refresh{}

}
