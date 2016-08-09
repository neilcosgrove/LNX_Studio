
// a starting template to work from

MVC_ImageView : MVC_View {

	classvar gs, lnx;

	var <image="lnx.jpg", scimage;

	*initClass{
/*		lnx = Image.new(Platform.lnxResourceDir +/+ "lnx.jpg");*/
		// gs = SCImage.new(String.scDir +/+ "GS Rhythm.jpg");
	}

	// set your defaults
	initView{
		if (image=="lnx.jpg") {
			scimage = lnx
		} {
			scimage = Image.new(Platform.lnxResourceDir +/+ image)
		}
	}

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				scimage.drawInRect(Rect(0,0,w,h),Rect(0,0,scimage.width,scimage.height),2,1.0);
			};
	}

	image_{|filename|
		image=filename;
		this.initView;
		if (view.notClosed) {view.refresh}
	}

	imagePath_{|filename|
		image=filename;
		scimage=SCImage.new(image);
		if (view.notClosed) {view.refresh}
	}

	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (modifiers==524576) { buttonNumber=1 };
			if (modifiers==262401) { buttonNumber=2 };
			buttonPressed=buttonNumber;
			if (modifiers.asBinaryDigits[4]==0) { // check apple not pressed because of drag
				if (editMode) {
					lw=lh=nil; startX=x; startY=y; view.bounds.postln; // for moving
				}
			};
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if (editMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}
		};
	}

}