/* /////////////////////////////////////////////////////////////////////////////////////////////////
**  MVC_MultiFloatView(s) displays LNX_MultiFloatArray(s)
**  used in K_Hole D_HoleFX
** /////////////////////////////////////////////////////////////////////////////////////////////////

w=MVC_Window();
f=LNX_MultiFloatArray(64);
MVC_MultiFloatView(w,Rect(10,10,380,200)).multiFloatArray_(f);
w.create;

*/

MVC_MultiFloatView : MVC_View {

	var <multiFloatArray;

	*initClass{}

	// set your defaults
	initView{}

	// make the view
	createView{
		view=UserView.new(window,rect)
		.drawFunc={|me|
			if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
			if (multiFloatArray.notNil) {
				Pen.smoothing_(false);
				Color.black.set;
				Pen.fillRect(Rect(0,0,w,h));
			}
		}
	}

	multiFloatArray_{|array|
		multiFloatArray=array;
		if (view.notClosed) {view.refresh}
	}

	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (modifiers.isAlt)  { buttonNumber=1 };
			if (modifiers.isCtrl) { buttonNumber=2 };
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