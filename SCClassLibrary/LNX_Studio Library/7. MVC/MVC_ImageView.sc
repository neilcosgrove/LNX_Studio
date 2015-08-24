
// a starting template to work from

MVC_ImageView : MVC_View {

	classvar gs, lnx;

	var <image="lnx.jpg", scimage;

	*initClass{
		lnx = Image.new(Platform.lnxResourceDir +/+ "lnx.jpg");
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

}