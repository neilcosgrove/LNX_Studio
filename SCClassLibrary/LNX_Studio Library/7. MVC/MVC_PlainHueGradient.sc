
// a starting template to work from

MVC_PlainHueGradient : MVC_View {

	var <sat=1, <val=1, <alpha=1, <steps=64;

	satValAlpha_{|s,v,a|
		if ((sat!=s)||(val!=v)||(alpha!=a)) {
			sat=s;
			val=v;
			alpha=a;
			if (view.notClosed) {view.refresh};
		};
	}

	steps_{|s|
		steps=s;
		if (view.notClosed) {view.refresh};
	}

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{


					Pen.smoothing_(false);

					steps.do{|i|

						Color.hsv(i/(steps),sat,val,alpha).set;


						Pen.fillRect(Rect(w/steps*i,0,w/steps,h));
					};


				}; // end.pen
			};
	}



}
