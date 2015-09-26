
// miguel negrao
// felix
// neil
/*

ColorPicker(); // 

*/
// 

ColorPicker {

	*new { arg object,updateObject;
		
		var color;
		
		var w,r,g,b,update,colorview;
		
		var h,s,v,a;
		
		var keys, keyList;
		
		var updateHSV;
		
		if (object.isNil) {object=~q};
		
		if (updateObject.isNil) {updateObject=object};
		
		if (object.isKindOf(Collection).not) {
			
			if (object.respondsTo(\colors)) {
				object=object.colors;
			
			}
		};

		w = Window.new("Color Tools",Rect(100,100,410,170),false);

		r = EZSlider(w, Rect(2,0,150,20),"R",labelWidth:10).value_(0.5);
		g = EZSlider(w, Rect(2,22,150,20),"G",labelWidth:10).value_(0.5);
		b = EZSlider(w, Rect(2,44,150,20),"B",labelWidth:10).value_(0.5);
		
		a = EZSlider(w, Rect(2,66,150,20),"A",labelWidth:10).value_(1);
		
		h = EZSlider(w, Rect(250,0,150,20),"H",labelWidth:10).value_(0.5);
		s = EZSlider(w, Rect(250,22,150,20),"S",labelWidth:10).value_(0.5);
		v = EZSlider(w, Rect(250,44,150,20),"V",labelWidth:10).value_(0.5);
		
		[r,g,b,a].do({|item| item.action_({|v|update.()}).sliderView.canFocus_(false)});
		
		[h,s,v].do({|item| item.action_({|v|updateHSV.()}).sliderView.canFocus_(false)});
		
		MVC_TextField(w,Rect(27,70+22,150+45,16))
			.actions_(\enterAction,{|v|
				var o=("{"+v.string+"}.try").interpret;
				if (o.notNil) {
					object=o;
					if (object.isKindOf(SequenceableCollection)){
						keys=(0..object.size-1)	;
					}{
						keys=object.keys.asList;
					};
					keyList.items_(keys.collect(_.asString));
				};
			})
			.string_((object.notNil).if("\\\\supplied: "++(object.class.asString),"") );
			
		keyList = MVC_PopUpMenu(w, Rect(27,70+20+22,150+45,18))
			.items_([])
			.action_{
				if (object.notNil) {
					color=object[keys[keyList.value]];
					r.value_(color.red);
					g.value_(color.green);
					b.value_(color.blue);
					a.value_(color.alpha);
					colorview.background_(color);
				}
			};
			
		if (object.notNil) {
			if (object.isKindOf(SequenceableCollection)){
				keys=(0..object.size-1)	;
			}{
				keys=object.keys.asList;
			};
			keyList.items_(keys.collect(_.asString));
		};
		
		MVC_TextField(w,Rect(27,70+22+20+20,150+45,16))
			.actions_(\enterAction,{|v|
				var o=("{"+v.string+"}.try").interpret;
				if (o.notNil) {
					updateObject=o;
				};
			})
			.string_((updateObject.notNil).if(
				"\\\\supplied: "++(updateObject.class.asString),"") );
		
		
		colorview = UserView(w,Rect(160,0,86,64+22))
    			.background_(color ? Color.grey)
    			.enabled_(true)
    			.mouseDownAction_({
	    			colorview.background.postln.postSmall;
	    		})
			.beginDragAction_({colorview.background})
			.canFocus_(false);

		update = {
			var hsvList;
			
			color = Color(r.value,g.value,b.value,a.value);
			colorview.background_(color);
			if (object.notNil) {
				object[keys[keyList.value]]=color;	
				if (updateObject.notNil) {
					
					if (updateObject.respondsTo(\color_)) {
						
						updateObject.color_(keys[keyList.value],color);
					}{
						if (updateObject.respondsTo(\refreshColors)) {
							updateObject.refreshColors
						}{
							updateObject.refresh
						};
					}
				};
				
				
			};
			
			hsvList=color.asHSV;
			
			h.value_(hsvList[0];);
			s.value_(hsvList[1];);
			v.value_(hsvList[2];);
			
		
		}; 
		
		updateHSV = {
						
			var th;
			
			th=h.value;
			
			if (th==1) {th=0};
						
			color = Color.hsv(th,s.value,v.value,a.value);
			colorview.background_(color);
			if (object.notNil) {
				object[keys[keyList.value]]=color;
				
				
				if (updateObject.respondsTo(\color_)) {
					
					updateObject.color_(keys[keyList.value],color);
				}{
					if (updateObject.respondsTo(\refreshColors)) {
						updateObject.refreshColors
					}{
						updateObject.refresh
					};
				}
		
			};

			r.value_(color.red);
			g.value_(color.green);
			b.value_(color.blue);
			
			
			
		};		

		w.front;

		keyList.viewDoValueAction_(0);
		{update.value}.defer(0.1);

	}

	/*
	    drag mouse to alter hue/saturation
	    control drag for value alpha
	    double click for action
	*/
	*hsv { arg color,onPick;
		var w,hslider,sslider,vslider,aslider,update,pallette;
		var h,s,v,a;
		
        color = color ? Color.grey;
		# h,s,v,a = color.asHSV;
        
		w = Window.new("Color Picker",Rect(100,100,250,88),false);
		if(onPick.isNil,{
			onPick = { arg clr; clr.postln;}				
		});
		pallette = UserView(w,Rect(160,0,84,84))
			.background_(color)
			.enabled_(true)
			.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			    if(clickCount == 2,{
			        onPick.value(pallette.background)
			    });
			})
			.mouseMoveAction_({ arg view,x,y,mod;
			    var h,s,v,a;
			    if(x < 85 and: {y<85},{

			        x = (x / 84.0).clip(0.0,0.999);
			        y = 1.0 - (y / 84.0).clip(0.0,1.0);

    			    # h,s,v,a =pallette.background.asHSV;
    			    if(mod==0,{
    			        // value / alpha
    			        pallette.background = Color.hsv( h,s, x,y);
    			        vslider.value = x;
    			        aslider.value = y;
    			    },{
    			        // hue / saturation
    			        pallette.background = Color.hsv( x,y,v,a);
    			        hslider.value = x;
    			        sslider.value = y;
    		        });
    		    })
			})
			.beginDragAction_({pallette.background})
			.canFocus_(false);

		update = { 
		    pallette.background_(Color.hsv(hslider.value,sslider.value,vslider.value,aslider.value)) 
		}; 

		hslider = EZSlider(w, Rect(2,0,150,20),"H",initVal:h,labelWidth:20)
			.action_(update);
		sslider = EZSlider(w, Rect(2,22,150,20),"S",initVal:s,labelWidth:20)
			.action_(update);
		vslider = EZSlider(w, Rect(2,44,150,20),"V",initVal:v,labelWidth:20)
			.action_(update);
		aslider = EZSlider(w, Rect(2,66,150,20),"A",initVal:a,labelWidth:20)
			.action_(update);
		
		[hslider,sslider,vslider,aslider].do({|item| item.sliderView.canFocus_(false) });

		update.value();
		
		w.front;

	}
    /*
        a single square, no sliders

	    drag mouse for hue/saturation
	    control drag for value/alpha
	    
	    livePick: 
	        true: do onPick any time the value changes
	        false: do it on double click

        onPick(color,window)
            window or parent 
            so double click can select and do window.close

    */
	*hsvMini { arg parent,bounds,color,onPick,livePick=false;
		var w,b, hval,sval,vval,aval,pallette;
		var h,s,v,a;
		
        color = color ? Color.grey;
		# h,s,v,a = color.asHSV;
        
		w = parent ?? {Window.new("Color Picker",(bounds ? Rect(100,100,100,100)).asRect,false)};
        b = (bounds ?? w.bounds.insetBy(2,2)).moveTo(2,2);
		if(onPick.isNil,{
			onPick = { arg clr; clr.postln;}				
		});
		pallette = UserView(w,b)
			.background_(color)
			.enabled_(true)
			.mouseMoveAction_({ arg view,x,y,mod;
			    var h,s,v,a;
			    if(x < b.width and: {y<b.height},{

			        x = (x / 84.0).clip(0.0,0.999);
			        y = 1.0 - (y / 84.0).clip(0.0,1.0);

    			    # h,s,v,a =pallette.background.asHSV;
    			    if(mod.isCtrl,{
    			        // value / alpha
    			        pallette.background = Color.hsv( h,s, x,y);
    			        vval = x;
    			        aval = y;
    			    },{
    			        // hue / saturation
    			        pallette.background = Color.hsv( x,y,v,a);
    			        hval = x;
    			        sval = y;
    		        });
    		        if(livePick,{
    		            onPick.value(pallette.background,w)
    		        });
    		    })
			})
			.beginDragAction_({pallette.background})
			.canFocus_(false);

        if(livePick.not,{
			pallette.mouseDownAction_({ arg view, x, y, modifiers, buttonNumber, clickCount;
			    if(clickCount == 2,{
			        onPick.value(pallette.background,w)
			    });
			})
        });

        if(parent.isNil,{
		    w.front;
		})

	}	
}
	 