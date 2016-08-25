/*
LNX_ColorPicker(object:a.window)

*/
// originally by miguel negrao & felix
// adapted for LNX by neil

LNX_ColorPicker {

	var <window, <color, update, updateHSV;

	var r,g,b,h,s,v,a, colorview, hView;

	var keys, keyList;

	var object,updateObject;

	*new {|bounds,object,updateObject| bounds;^super.new.init(bounds,object,updateObject) }

	updateSliderColors{

		var hsva;

		// *0.99 fixes a bug with Color.red gradients
		a.color_(\background,Gradient(color.copy.alpha_(1)*0.99,color.copy.alpha_(0)*0.99,\v));

		hsva=color.asHSV;
		hView.satValAlpha_(hsva[1],hsva[2],hsva[3]);

		r.color_(\background,Gradient(color.copy.red_(1)*0.99,color.copy.red_(0)*0.99,\v));
		g.color_(\background,Gradient(color.copy.green_(1)*0.99,color.copy.green_(0)*0.99,\v));
		b.color_(\background,Gradient(color.copy.blue_(1)*0.99,color.copy.blue_(0)*0.99,\v));

		s.color_(\background,Gradient(color.copy.sat_(1)*0.99,color.copy.sat_(0)*0.99,\v));
		v.color_(\background,Gradient(color.copy.val_(1)*0.99,color.copy.val_(0)*0.90,\v));

	}

	init {|bounds,argObject,argUpdateObject|

		color = color ? Color.black;

		object=argObject;
		updateObject=argUpdateObject;
		bounds = bounds ? Rect(100,100,410,125);

		//if (object.isNil) {object=~q};

		if (updateObject.isNil) {updateObject=object};

		if (object.isKindOf(Collection).not) {
			if (object.respondsTo(\colors)) {
				object=object.colors;
			}
		};

		// the window
		window = MVC_Window("Widget Editor",bounds,false)
			.color_(\background,Color(0.7,0.7,0.7)/2.5);

		// red, green, blue & alpha
		r = MVC_SmoothSlider(window, Rect(15+2,22+30,150-15,20))
			.label_("R").orientation_(\horiz).value_(0).color_(\knob,Color.white)
			.color_(\hilite,Color.clear);
		g = MVC_SmoothSlider(window, Rect(15+2,44+30,150-15,20))
			.label_("G").orientation_(\horiz).value_(0).color_(\knob,Color.white)
			.color_(\hilite,Color.clear);
		b = MVC_SmoothSlider(window, Rect(15+2,66+30,150-15,20))
			.label_("B").orientation_(\horiz).value_(0).color_(\knob,Color.white)
			.color_(\hilite,Color.clear);

		a = MVC_SmoothSlider(window, Rect(15+250,66+30,150-15,20))
			.label_("A").orientation_(\horiz).value_(1).color_(\knob,Color.white)
			.color_(\hilite,Color.clear);

		// hue background
		hView = MVC_PlainHueGradient (window,Rect(15+250+2,0+30+2,150-15-4+1,20-4))
			.steps_((150-15-4+1)/2);

		// hue, saturation & value
		h = MVC_SmoothSlider(window, Rect(15+250,0+30,150-15,20))
			.label_("H").orientation_(\horiz).value_(0.5).color_(\knob,Color.white)
			.color_(\hilite,Color.clear)
			.color_(\background,Color.clear);

		s = MVC_SmoothSlider(window, Rect(15+250,22+30,150-15,20))
			.label_("S").orientation_(\horiz).value_(0).color_(\knob,Color.white)
			.color_(\hilite,Color.clear);

		v = MVC_SmoothSlider(window, Rect(15+250,44+30,150-15,20))
			.label_("V").orientation_(\horiz).value_(0).color_(\knob,Color.white)
			.color_(\hilite,Color.clear);

		// actions
		[r,g,b,a].do({|item| item.action_({|v|update.()})});

		[h,s,v].do({|item| item.action_({|v|updateHSV.()})});

		// keyList to colors
		keyList = MVC_PopUpMenu3(window, Rect(15+2,30,135,18))
			.color_(\focus,Color.clear)
			.color_(\background,Color(0.2,0.2,0.2))
			.color_(\string,Color.white)
			.items_([])
			.value_(0)
			.action_{this.updateColor; update.();};

		if (object.notNil) {
			if (object.isKindOf(SequenceableCollection)){
				keys=(0..object.size-1)	;
			}{
				keys=object.keys.asList.sort;
			};
			keyList.items_(keys.collect(_.asString));
		};

		// rounded border
		MVC_RoundBounds(window,Rect(160,30,86,64+22)).width_(3).color_(\background,Color.black)
			.noResize;

		// color square
		colorview = MVC_PlainSquare(window,Rect(160,30,86,64+22))
			.value_(1)
    			.color_(\on, color)
    			.mouseDownAction_{
	    			color.postln.postSmall;
	    		};

		// update color
		update = {
			var hsvList;
			color = Color(r.value,g.value,b.value,a.value);
			colorview.color_(\on,color);
			this.updateSliderColors;
			if (object.notNil) {
				object[keys[keyList.value]]=color;
				if (updateObject.notNil) {

					if (updateObject.respondsTo(\colorWithAction_)) {

						updateObject.colorWithAction_(keys[keyList.value],color);
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
			if (hsvList[0].isNaN.not) { h.value_(hsvList[0])};
			if (hsvList[1].isNaN.not) { s.value_(hsvList[1])};
			if (hsvList[2].isNaN.not) { v.value_(hsvList[2])};
		};

		// update color with hsv
		updateHSV = {
			var th;
			th=h.value;
			if (th==1) {th=0};
			color = Color.hsv(th,s.value,v.value,a.value);
			if (color.red.isNil) {color=Color(v.value,v.value,v.value,a.value)};
			colorview.color_(\on,color);
			this.updateSliderColors;
			if (object.notNil) {
				object[keys[keyList.value]]=color;
				if (updateObject.respondsTo(\colorWithAction_)) {

					updateObject.colorWithAction_(keys[keyList.value],color);
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

		window.create;

		this.updateColor;

	}

	keyAndColor{
		if (keys.isNil) {^()};
		if (keys[keyList.value].isNil) {^()};
		^(keys[keyList.value]:color)
	}

	key{
		if (keys.isNil) {^nil};
		if (keys[keyList.value].isNil) {^nil};
		^keys[keyList.value];
	}

	object_{|argObject,name|
		var lastKey=this.key;

		object=argObject;

		updateObject=object;

		if (object.isKindOf(Collection).not) {
			if (object.respondsTo(\colors)) {
				object=object.colors;
			}
		};

		if (object.isKindOf(SequenceableCollection)){
			keys=(0..object.size-1)	;
		}{
			if (object.notNil) {
				keys=object.keys.asList.sort;

				// this is to tidy up gui editor options
				keys.removeAll([\disabled,\focus,\backgroundDisabled,\knobDisabled,
					\sliderDisabled, \offDisabled, \onDisabled, \stringDisabled])
			}{
				keys=[];
			}
		};
		keyList.items_(keys.collect(_.asString));

		if (lastKey.notNil) {
			if (keys.includes(lastKey)) {
				keyList.value_(keys.indexOf(lastKey))
			};
		};

		this.updateColor;
		update.();

		if (name.notNil) {
			if (window.name!=("Widget Editor: "++name)) {
				window.name_("Widget Editor: "++name);
			}
		}{
			window.name_("Widget Editor");
		};
	}

	updateColor{
		if (keys.notNil) {
			if (keys[keyList.value].notNil) {
				color=object[keys[keyList.value]];
				r.value_(color.red);
				g.value_(color.green);
				b.value_(color.blue);
				a.value_(color.alpha);
				colorview.color_(\on,color);
				this.updateSliderColors;
			}
		}
	}

	// usual stuff

	create{ window.create }

	bounds{^window.bounds}

	// open the window
	open{ window.front }

	// and close it
	close{ window.close }

	front{ window.front }

	// free me...
	free{
		window.close;
		window.free;
		window = color = r = g = b = update = colorview =
		h = s = v = a = keys = keyList = updateHSV =
		object = updateObject = nil;
	}

}
	 