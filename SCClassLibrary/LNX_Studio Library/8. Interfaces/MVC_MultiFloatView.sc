/* /////////////////////////////////////////////////////////////////////////////////////////////////
**  MVC_MultiFloatView(s) displays LNX_MultiFloatArray(s)
**  used in K_Hole D_HoleFX
** /////////////////////////////////////////////////////////////////////////////////////////////////

w=MVC_Window();
w.setInnerExtent(800);
f=LNX_MultiFloatArray(32).randFill;
v=MVC_MultiFloatView(w,Rect(10,10,780,320)).multiFloatArray_(f);
m=[3,128,0,1,32].asSpec.asModel.action_{|me,val| f.resizeLin_(val,n ? (f.size)); v.refresh; };
k=MVC_MyKnob(w,Rect(10,335,40,40),m);
k.mouseDownAction_{ n=f.size };
w.create;

f.randFill; v.refresh;
f.resizeLin_(8,32); v.refresh;
f.resizeLin_(16,32); v.refresh;
f.resizeLin_(31,32); v.refresh;
f.resizeLin_(32,32); v.refresh;
f.resizeLin_(33,32); v.refresh;
f.resizeLin_(63,32); v.refresh;
f.resizeLin_(64,32); v.refresh;
f.resizeLin_(65,32); v.refresh;
f.resizeLin_(128,32); v.refresh;

f.size.do{|i| f[i] = (i/0.25pi).sin+1*0.5}; v.refresh;
o=f.size; { inf.do{|i| f.resizeLin_(i.fold(3,128),o); v.refresh; 0.025.wait } }.fork(AppClock);

0.exit;
*/

MVC_MultiFloatView : MVC_View {

	var <multiFloatArray, size=1, sw=1, sw2=1, sh=1, lx, ly, <>multiFloatAction;

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
				sw2  = (sw).clip(1,inf);
				Pen.smoothing_(false);
				Color(0.1,0.1,0.2).set;
				Pen.fillRect(Rect(0,0,w,h));
				Color(0.87,0.87,1).set;
				size.do{|i|
					var value = multiFloatArray.unmapNoClipAt(i);
					if ((value>1)||(value<0)) { value = value.wrap(0,1) }; // wrap for offset
					//(Color(0.8,0.8,0.95)*((i%3).map(0,2,0.8,1))).set;
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
		view.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			lx = ((x-1)/sw).asInt.clip(0,size-1);
			ly = 1-(((y-1)/(h-2)).clip(0,1));
			multiFloatArray.putMap(lx, ly);
			multiFloatAction.value(this,multiFloatArray);
			this.refresh;
			if (modifiers.isAlt)  { buttonNumber=1 };
			if (modifiers.isCtrl) { buttonNumber=2 };
			buttonPressed=buttonNumber;
			if (modifiers.asBinaryDigits[4]==0) { // check apple not pressed because of drag
				if (editMode) {
					lw=lh=nil; startX=x; startY=y; view.bounds.postln; // for moving
				}
			};
		};
		view.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			var nx = ((x-1)/sw).asInt.clip(0,size-1);
			var ny = 1-(((y-1)/(h-2)).clip(0,1));
			if ((nx-lx).abs==0) {
				multiFloatArray.putMap(nx, ny);
			}{
				if (lx<nx) {
					lx.for(nx,{|i|
						var frac = (i-lx) / ((lx-nx).abs);
						multiFloatArray.putMap(i, ( ny * frac ) + ( ly * (1 - frac) ) );
					});
				}{
					nx.for(lx,{|i|
						var frac = (i-nx) / ((lx-nx).abs);
						multiFloatArray.putMap(i, ( ly * frac ) + ( ny * (1 - frac) ) );
					});
				};
			};
			lx = nx;
			ly = ny;
			multiFloatAction.value(this,multiFloatArray);
			this.refresh;
			if (editMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}
		};
	}

}