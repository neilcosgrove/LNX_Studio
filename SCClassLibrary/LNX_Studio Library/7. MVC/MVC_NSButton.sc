
// LNX_BevelPushOnOffView
/*
w=Window().front;

40.do{|n|
	MVC_NSButton(w,Rect((n%10)*35,(n/10).asInt*35,33,33),"").bezelStyle_(n);
};
*/

MVC_NSButton : MVC_View {

	var	<bezelStyle=1;

	// set your defaults
	initView{
		if (strings.notNil) {
			string=string ? strings[0] ? "";
		}{
			string=string ? "";
		};
	}
	
	// create the gui's items that make this MVC_View
	create{|argParent|
		if (view.isClosed) {
			if (argParent.notNil) {
				window = argParent;
				parent = argParent.asView; // actual view
			};
			this.createView;
			view.onClose_{ onClose.value(this) };
			this.addControls;
			this.createLabel;
		}{
			"View already exists.".warn;
		}
	}
	
	// make the view
	createView{
		view=AppKitView.new(window, "NSButton", "initWithFrame:", rect);
		view.setTitle = string;
		view.setBezelStyle_(bezelStyle);
	}
	
	// add the controls
	addControls{
		
		view.initAction("doAction:");
		view.action = { |me, val|
			this.viewDoValueAction_(1,nil,true,false)
		};

	}
	
	bezelStyle_{|num|
		bezelStyle=num;
		if (view.notClosed){
			view.setBezelStyle_(bezelStyle)
		};
	}

}
