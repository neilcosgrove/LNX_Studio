
// a starting template to work from
/*
(
w=MVC_Window().create;
MVC_RoundEdges(w,Rect(10,10,100,20));
)
*/

MVC_RoundEdges : MVC_View {

	var <>place=\top;

	// set your defaults
	initView{
		colors=colors++(
			'background' 	: Color.red
		);
	}

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(true);
					Pen.fillColor_(colors[\background].set);
					Pen.fillRect(Rect(h,0,w-(h*2),h));
					Pen.color_(colors[\background]);

					if (place==\top) {

						Pen.addWedge(h@h, h, 2pi*1.5, 2pi*0.25); // top left
						Pen.addWedge((w-h)@h, h, 2pi*1.75, 2pi*0.25); // top right

					}{
						Pen.addWedge(h@0, h, 2pi*2.25, 2pi*0.25); // top left
						Pen.addWedge((w-h)@0, h, 2pi*2, 2pi*0.25); // top right

					};

					Pen.perform(\fill);

				}; // end.pen
			};
	}

}
