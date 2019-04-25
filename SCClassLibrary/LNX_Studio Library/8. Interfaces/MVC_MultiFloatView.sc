/* /////////////////////////////////////////////////////////////////////////////////////////////////
**  MVC_MultiFloatView(s) displays LNX_MultiFloatArray(s)
**  used in K_Hole D_HoleFX
** /////////////////////////////////////////////////////////////////////////////////////////////////

w=MVC_Window();
f=LNX_MultiFloatArray(64).randFill;
v=MVC_MultiFloatView(w,Rect(10,10,380,200)).multiFloatArray_(f);
w.create;

f.randFill; v.refresh;

64.do{|i| f[i] = (i/3).sin+1*0.5}; v.refresh;
f.resizeLin_(8,64); v.refresh;
f.resizeLin_(16,64); v.refresh;
f.resizeLin_(31,64); v.refresh;
f.resizeLin_(32,64); v.refresh;
f.resizeLin_(33,64); v.refresh;
f.resizeLin_(63,64); v.refresh;
f.resizeLin_(64,64); v.refresh;
f.resizeLin_(65,64); v.refresh;
f.resizeLin_(128,64); v.refresh;

{ inf.do{|i| f.resizeLin_(i.fold(3,128),64); v.refresh; 0.05.wait } }.fork(AppClock);

0.exit;
*/

MVC_MultiFloatView : MVC_View {

	var <multiFloatArray, size=1, sw=1, sw2=1, sh=1;

	*initClass{}

	// set your defaults
	initView{}

	// make the view
	createView{
		view=UserView.new(window,rect)
		.drawFunc={|me|
			if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
			if (multiFloatArray.notNil) {
				// update vars for use in other methods
				size = multiFloatArray.size;
				sw   = ((w-2)/size).clip(1,inf);
				sw2  = (sw-2).clip(1,inf);

				Pen.smoothing_(false);
				Color.black.set;
				Pen.fillRect(Rect(0,0,w,h));
				Color.orange.set;
				size.do{|i|
					var value = multiFloatArray.unmapAt(i);
					//Color(1,i/size/4+0.25).set;
					Pen.fillRect( Rect(i*sw+1, h-1, sw2, value.neg*(h-2) ) );
				};

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