
// link a function to a model

MVC_FuncAdaptor : MVC_View {

	var <>func;
	
	// set your defaults
	initView{ }

	// no view
	createView{}

	// also called from model, sets the value of the gui item
	value_{|val,delta|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if (value!=val) {
			func.value(this,val,delta);
			value=val;
		};
		
	}
	
	// just do it, no testing for !=
	valueDo_{|val,delta|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		func.value(this,val,delta);
		value=val;
	}
	
	freshAdaptor{ this.valueDo_(model.value) }
	
	viewFree{ func=nil }

}

// link a model to a color change to an item in guiViews list

MVC_ColorAdaptor : MVC_View {

	var <>views, <>colorIndex;
	
	// set your defaults
	initView{ 
		colors=colors++(
			'on'			: Color.green,
			'off'		: Color.black
		);
	}

	// no view
	createView{}

	// also called from model, sets the value of the gui item
	value_{|val|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if (value!=val) {
			if ((views.notNil)&&(colorIndex.notNil)&&(val<views.size)) {
				if (value!=val) {
					views[value]
						.hilite_(false)
						.color_(colorIndex,colors[\off]); // remove old
					views[val]
						.hilite_(true)
						.color_(colorIndex,colors[\on]); // put new
				};	
			};
			value=val;
		};
		
	}
	
	pause{
		if (views.notNil) {
			views[value]
				.hilite_(false)
				.refresh
		}
	}
	
	// just do it, no testing for !=
	valueDo_{|val|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if ((views.notNil)&&(colorIndex.notNil)&&(val<views.size)) {
			views[value].color_(colorIndex,colors[\off]);
			views[val].color_(colorIndex,colors[\on]);
		};
		value=val;
	}
	
	viewFree{
		views=colorIndex=nil;
	}

}

// hilite adaptor for counters

MVC_HiliteAdaptor : MVC_View {

	var <>views, <>refreshZeros=true;

	// also called from model, sets the value of the gui item
	value_{|val|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if (value!=val) {
			if ((views.notNil)&&(val<views.size)) {
				if (value!=val) {
					views[value].hilite_(false);
					if ((refreshZeros)or:{views[value].value>0}) { views[value].refresh };
				};
				views[val].hilite_(true);
				if ((refreshZeros)or:{views[val].value>0}) { views[val].refresh };
			};
			value=val;
		};
	}
	
	pause{
		if (views.notNil) {
			views[value]
				.hilite_(false)
				.refresh
		}
	}
	
	viewFree{ views=nil }

}

