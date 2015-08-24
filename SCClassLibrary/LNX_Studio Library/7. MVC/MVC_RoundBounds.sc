
// a starting template to work from
/*
(
w=MVC_Window();
MVC_RoundBounds(w,Rect(50,50,100,100)).width_(6);
w.create;
)
*/

MVC_RoundBounds : MVC_View {
	
	var <oldBounds, <>width=6, <views, noResizeFlag=false, rl=4, rt=2, rr=6, rb=8;
	var <>forceHold=false, holdRect; // temp bug fix in VOLCA beats

	// set your defaults
	initView{
		colors=colors++(
			'background' 	: Color(0,0,0,0.5)
		);
		views=IdentityDictionary[];
		canFocus=false;
		resize=2;
		
		oldBounds=();
		this.resizeAction_{
			oldBounds = views.collect(_.bounds);
		};
	}
	
	postCreate{
		oldBounds.pairsDo{|key,bounds|
			views[key].bounds_(bounds)	
		};	
	}	
	
	noResize{
		noResizeFlag=true;
		resize=1;
		if (view.notClosed) {
			views.do(_.resize_(1));
		};
	}
	
	hasResize{
		noResizeFlag=false;
		if (view.notClosed) {
			views[\top].resize_(2);
			views[\bottom].resize_(8);
			views[\left].resize_(4);
			views[\right].resize_(6);
		};
		rl=4; rt=2; rr=6; rb=6;
	}
	
	setResize{|l,t,r,b|
		noResizeFlag=false;
		if (view.notClosed) {
			views[\top].resize_(t);
			views[\bottom].resize_(b);
			views[\left].resize_(l);
			views[\right].resize_(r);
		};
		rl=l; rt=t; rr=r; rb=b;
	}
	
	// create the gui's items that make this MVC_View
	create{|argWindow|
		if (view.isClosed) {
			if (argWindow.notNil) { window = argWindow };
			// the following order is important
			// DOES this happen any more? >> also makes sure the view overlaps number
			this.createView;
			
			view.canFocus_(canFocus)
				.onClose_{ onClose.value(this) };				
		}{
			"View already exists.".warn;
		}
	}
	
	// make the view
	createView{
		
		var l,t,w,h;
		
		if (forceHold && (holdRect.isNil)) { holdRect=rect };
		
		rect = holdRect ? rect;
		
		
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		
		views[\top]=SCUserView.new(window,Rect(l-width,t-width,w+(width*2),width))
			.canFocus_(false)
			.resize_(rt)
			.drawFunc={|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				l=thisRect.left;
				t=thisRect.top;
				w=thisRect.width;
				h=thisRect.height;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(true);			
					Pen.fillColor_(colors[\background].set);
					Pen.fillRect(Rect(h,0,w-(h*2),h));
					Pen.color_(colors[\background]);
					
					if (\top==\top) {
					
						Pen.addWedge(h@h, h, 2pi*1.5, 2pi*0.25); // top left
						Pen.addWedge((w-h)@h, h, 2pi*1.75, 2pi*0.25); // top right
						
					}{
						Pen.addWedge(h@0, h, 2pi*2.25, 2pi*0.25); // top left
						Pen.addWedge((w-h)@0, h, 2pi*2, 2pi*0.25); // top right
						
					};			
					Pen.perform(\fill);
				}; // end.pen
			};	
			
		view=views[\top];
			
		views[\bottom]=SCUserView.new(window,Rect(l-width,t+h,w+(width*2),width))
			.canFocus_(false)
			.resize_(rb)
			.drawFunc={|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				l=thisRect.left;
				t=thisRect.top;
				w=thisRect.width;
				h=thisRect.height;

				Pen.use{
					Pen.smoothing_(true);			
					Pen.fillColor_(colors[\background].set);
					Pen.fillRect(Rect(h,0,w-(h*2),h));
					Pen.color_(colors[\background]);
					
					if (\bottom==\top) {
					
						Pen.addWedge(h@h, h, 2pi*1.5, 2pi*0.25); // top left
						Pen.addWedge((w-h)@h, h, 2pi*1.75, 2pi*0.25); // top right
						
					}{
						Pen.addWedge(h@0, h, 2pi*2.25, 2pi*0.25); // top left
						Pen.addWedge((w-h)@0, h, 2pi*2, 2pi*0.25); // top right
						
					};			
					Pen.perform(\fill);
				}; // end.pen
			};
			
		views[\left]=SCUserView.new(window,Rect(l-width,t,width,h))
			.canFocus_(false)
			.resize_(rl)
			.drawFunc={|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				w=thisRect.width;
				h=thisRect.height;
				
				Pen.use{
					Pen.smoothing_(true);			
					Pen.fillColor_(colors[\background].set);
					Pen.fillRect(Rect(0,0,w,h));
				}; // end.pen
			};

		views[\right]=SCUserView.new(window,Rect(l+w,t,width,h))
			.canFocus_(false)
			.resize_(rr)
			.drawFunc={|me|
				var thisRect,l,t,w,h;
				thisRect=me.bounds;
				w=thisRect.width;
				h=thisRect.height;

				Pen.use{
					Pen.smoothing_(true);			
					Pen.fillColor_(colors[\background].set);
					Pen.fillRect(Rect(0,0,w,h));
				}; // end.pen
			};
		
		if (noResizeFlag) { this.noResize };
	}
	
	// you can't use System clock to call refresh
	refresh{
		if (view.notClosed) {
			// drop of if tab is hiddden			
			if ( (parent.isKindOf(MVC_TabView))and:{parent.isHidden} ) { ^this };
			// else
			views.do(_.refresh);
		}
	}

}
