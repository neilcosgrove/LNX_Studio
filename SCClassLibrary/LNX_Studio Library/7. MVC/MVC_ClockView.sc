// a Circle clock amount
/*

w = MVC_Window().create;
m = [0.5,\unipolar].asModel;
v = MVC_ClockView(w, m, Rect(10,10,100,100));

m.value_(0);
m.value_(0.25);
m.value_(0.33);
m.value_(0.5);
m.value_(0.75);
m.value_(1);

v.innerWidth_(50).refresh;

*/

MVC_ClockView : MVC_View {

	var <>edgeWidth=2, <>innerWidth=0.1;

	// set your defaults
	initView{
		colors=colors++(
			'background'	: Color(0,0,0,0.55),
			'on'			: Color(1,1,1,0.7);
		);
		isSquare=true;
	}

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				var w2,h2, rect2, x2;
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					var val;
					// unmap values
					if (controlSpec.notNil) {
						val=controlSpec.unmap(value);
					}{
						val=value.clip(0,1);
					};

					Pen.smoothing_(true);
					colors[\background].set;
					Pen.fillOval(Rect(0,0,w,h));
					colors[\on].set;
					Pen.addAnnularWedge(
						(w/2)@(h/2),
						innerWidth,
						(w/2) - edgeWidth,
						-pi/2,
						val * 2pi
					);
					Pen.perform(\fill);

				}; // end.pen
			};
	}

	// add the controls
	addControls{}

}
