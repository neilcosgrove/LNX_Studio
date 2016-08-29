
MVC_PresetMenuInterface{
	
	var	<presetView,
		<window,
		<xy,
		<value,
		<presetNames,
		<view,  // this can go
		<guiItems,
		<gui,
		<width,
		<>background,
		<>editColor,
		<>buttonColor,
		<>textBackground,
		<>stringColor,
		
		<>action,
		<>renameAction,
		<>addAction,
		<>removeAction,
		<>writeAction,
		<>clearAction,
		<>randomAction,
		<>midiAction,
		
		<resize,
		
		<isFX=false;
	
	*new {|window,xy,width,background,editColor,buttonColor,textBackground,stringColor,resize| 
		^super.new.init(
			window,xy,width,background,editColor,buttonColor,textBackground,stringColor,resize)
	}
	
	init {|argWindow,argXY,argWidth,argBackground,argEditColor,argButtonColor,argTextBackground,
			argStringColor,argResize|
		#window,xy,width=[argWindow,argXY,argWidth];
		presetNames=[];
		background=argBackground ? Color(0.3,0.15,0);
		editColor=argEditColor?(Color.orange);
		buttonColor=argButtonColor?(Color.orange);
		textBackground=argTextBackground?background;
		stringColor=argStringColor?Color.white;
		width=width?70;
		resize = argResize;
		this.createGUI;
	}
	
	bounds{^gui[\view].bounds}
	
	bounds_{|rect| gui[\view].bounds_(rect) }

	createGUI{
	
		var item;
	
		gui=IdentityDictionary[];
	
		//guiItems=nil!7;
	
		// view
		gui[\view]=MVC_CompositeView(window, Rect(xy.x,xy.y,93+width,18), false)
			.color_(\background,background/2.8);
			
		if (resize.notNil) { gui[\view].resize_(resize) };
			
		// 0.menu
		gui[\menu]=MVC_PopUpMenu3(gui[\view],Rect(2,2,14,14))
			.staticText_("")
			.showTick_(false)
			.action_{|me|
				var v = me.value;
				if ((v>=0)&&(v<(presetNames.size))){
					value=v;
					gui[\text].string_(presetNames@value).color_(\string,stringColor);
					action.value(value);
				
				}{
					if (v==(presetNames.size+1)) { this.morphGUI };
					if (v==(presetNames.size+2)) { clearAction.value };
					if (v==(presetNames.size+3)) { midiAction.value };
					
				};
				me.value_(0);
			}
			.color_(\background,background)
			.color_(\string,editColor)
			.canFocus_(false)
			.font_(Font("Arial", 12));

		this.setMenuItems;
		//gui[\menu].value_(0);

		
		// 1.text box	
		gui[\text]=MVC_Text(gui[\view],Rect(17,2,width+(15*2)-1-3,14))
			.string_(value.isNumber.if(presetNames@@value,"No presets"))
			.shadow_(false)
			.canEdit_(true)
			.actions_(\stringAction,{|me|
				if (presetNames.size>0) {
					if (value.isNumber) {
						presetNames[value]=me.string;
						this.setMenuItems;
						renameAction.value(value,me.string);
					}{
						me.string_("Select preset");
					};
				}{
					me.string_("No presets");
				};
			})
			.downKeyAction_{
				var v;
				if (value.notNil) {
					v=(value+1).wrap(0,presetNames.size-1);
					this.valueAction_(v);
				}{
					this.valueAction_(0)
				}
			}
			.upKeyAction_{
				var v;
				if (value.notNil) {
					v=(value-1).wrap(0,presetNames.size-1);
					this.valueAction_(v);
				}{
					this.valueAction_(presetNames.size-1)
				}
			}
			.font_(Font("Arial", 11))
			.color_(\cursor,Color.white)
			.color_(\editBackground,editColor)
			.color_(\background,textBackground)
			.color_(\string,stringColor)
			.color_(\focus,Color(0,0,0,0));
	
		// 2.add
		gui[\add]=MVC_FlatButton(gui[\view],Rect(width+17-3+(15*2),1,16,16),"+")
			.mode_(\icon)
			.action_{ addAction.value }
			.font_(Font("Helvetica",10))
			.color_(\down,buttonColor/2)
			.color_(\up,buttonColor);

		// 3.delete
		gui[\delete]=MVC_FlatButton(gui[\view],Rect(width+17-2+(15*3),1,16,16),"-")
			.mode_(\icon)
			.action_{
				if ((presetNames.size>0)and:{value.isNumber}and:{value<presetNames.size}){
					removeAction.value(value);
				}
			}
			.font_(Font("Helvetica",10))
			.color_(\down,buttonColor/2)
			.color_(\up,buttonColor);
			
		// 4.write
		gui[\write]=MVC_FlatButton(gui[\view],Rect(width+17-1+(15*4),1,16,16),"W")
			.action_ {
				if ((presetNames.size>0)and:{value.isNumber}and:{value<presetNames.size}){
					writeAction.value(value)
				}
			}
			.font_(Font("Helvetica",10,true))
			.color_(\down,buttonColor/2)
			.color_(\up,buttonColor);
	}

	value_{|i|
		if (presetNames.size>0) {
			if (i.notNil) {
				value=i.clip(0,presetNames.size-1);
				if (window.isClosed.not) {
					gui[\text].string_(presetNames@value).color_(\string,stringColor);
				}
			}{
				value=nil;
				if (window.isClosed.not) {
					gui[\text].string_("").color_(\string,stringColor);
				}
			};
		}{
			value=nil;
			if (window.isClosed.not) {
				gui[\text].string_("No presets").color_(\string,stringColor);
			}
		}
	}
	
	valueAction_{|i|
		var v;
		if (i!=value) {
			v=value;
			this.value_(i);
			if (v!=value) {
				if (value.notNil) {
					action.value(value);
				}
			};
		};
	}
	
	items_{|names|
	
		this.presetNames_(names)
	
	}
	
	items{^presetNames}
	
	presetNames_{|names|
		presetNames=names;
		if (presetNames.size>0) {
			if (value.notNil) { value=value.clip(0,presetNames.size-1); };
			if (window.isClosed.not) {
				this.setMenuItems;
				if (value.notNil) {
					gui[\text].string_(presetNames@value).color_(\string,stringColor)
				}{
					gui[\text].string_("Select preset").color_(\string,stringColor)
				};
			};
		}{
			value=nil;
			if (window.isClosed.not) {
				this.setMenuItems;
				gui[\text].string_("No presets").color_(\string,stringColor);
			};
		};
	}
	
	setMenuItems{
		gui[\menu].items_(presetNames++
						["-","Random","Clear all"]++if(isFX,["MIDI In settings"],[]));
	}
	
	isFX_{|bool|
		if (isFX!=bool) {
			isFX=bool;
			this.setMenuItems
		}
	}
	
	refresh{
		if (view.notClosed) {
			view.refresh;
		}
	}
	
	morphGUI{
		randomAction.value;
	}
	
	focus{}
	enabled_{}
	
	free{
		gui.do(_.free);
		gui=presetNames=window=action=renameAction=addAction=removeAction=writeAction=
		clearAction=randomAction=nil;
	}
	remove{}
	
	show{view.bounds_(Rect(xy.x,xy.y,94+width,18))}
	hide{view.bounds_(Rect(xy.x,xy.y,0,0))}
	
	refreshToSelection{} // temp, remove this later
	
}
