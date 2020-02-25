/* /////////////////////////////////////////////////////////////////////////////////////////////////
**  MVC_MultiFloatView(s) displays LNX_MultiDoubleArray(s)
**  used in K_Hole D_HoleFX
** /////////////////////////////////////////////////////////////////////////////////////////////////

w=MVC_Window();
w.setInnerExtent(800);
f=LNX_MultiDoubleArray(32).randFill;
v=MVC_MultiFloatView(w,Rect(10,10,780,320)).multiDoubleArray_(f);
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

	var <multiDoubleArray, size=1, sw=1, sw2=1, sh=1, lx, ly, <>multiFloatAction, <>drawMode=\rect;
	var <>guiFeedbackAction, <>feedbackText, <graphView;
	var <>options, <>lowMid, <>midHigh;

	*initClass{}

	// set your defaults
	initView{

		colors=colors++(
			'background' : Color(0.15,0.15,0.15),
			'graph' 	 : Color(31/59,52/85,28/43)
		);


	}

	// make the view as line
	createView{

		graphView=UserView.new(window,rect)
		.drawFunc={|me|
			if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
			if (multiDoubleArray.notNil) {
				if (drawMode==\rect) {
					// update vars for use in other methods
					size = multiDoubleArray.size;
					sw   = ((w-2)/size).clip(1,inf);
					sw2  = (sw).clip(1,inf);
					Pen.smoothing_(false);
					colors[\background].set;
					Pen.fillRect(Rect(0,0,w,h));
					colors[\graph].set;
					size.do{|i|
						var value = multiDoubleArray.unmapNoClipAt(i);
						if ((value>1)||(value<0)) { value = value.wrap(0,1) }; // wrap for offset
						//(Color(0.8,0.8,0.95)*((i%3).map(0,2,0.8,1))).set;
						Pen.fillRect( Rect(i*sw+1, h-1, sw2, value.neg*(h-2) ) );
					};
				}{
					// update vars for use in other methods
					size = multiDoubleArray.size;
					sw   = ((w-2)/(size-1)).clip(1,inf);
					sw2  = (sw).clip(1,inf);
					Pen.smoothing_(true);
					colors[\background].set;
					Pen.fillRect(Rect(0,0,w,h));
					colors[\graph].set;

					Pen.width_(8);
					size.do{|i|
						var value = multiDoubleArray.unmapNoClipAt(i);
						if ((value>1)||(value<0)) { value = value.wrap(0,1) }; // wrap for offset
						if (i==0) {
							Pen.moveTo(((i*sw+1) @ (h- (value*(h-2)))));
						}{
							Pen.lineTo(((i*sw+1) @ (h- (value*(h-2)))));
						};
					};
					Pen.stroke;

					/*
					Pen.moveTo((1@(h-1)));
					size.do{|i|
						var value = multiDoubleArray.unmapNoClipAt(i);
						if ((value>1)||(value<0)) { value = value.wrap(0,1) }; // wrap for offset
						Pen.lineTo(((i*sw+1) @ (h- (value*(h-2)))));
					};
					Pen.lineTo(((w-1)@(h-1)));
					Pen.lineTo((1@(h-1)));
					Pen.fillStroke;
					*/

				};
				if (feedbackText.notNil) {
					Pen.smoothing_(true);
					Pen.font_(Font("Helvetica",14));
					Pen.fillColor_(Color(1,1,1));
					Pen.stringLeftJustIn(feedbackText,Rect(5,3,400,20));
				};
			};
		};

		view=UserView.new(window,rect)
		.drawFunc={|me|
			// start scale
			if (options.notNil) {
				var controlSpec = multiDoubleArray.controlSpec;
				var min  = options.minFreq;
				var max  = options.maxFreq;
				var hAxis = ( ((1..9)*10)++((1..9)*100)++((1..9)*1000)++((1..2)*10000)).collect{|freq|
					((w-2) * log(freq/min) / log(max/min) + 1).asInt };
				var hAxisShaeds = [1]++((2..10)/10)++((2..10)/10)++((2..10)/10)++((2..2)/10);
				var hAxisT = [10, 100, 1000, 10000].collect{|freq| ((w-2) * log(freq/min) / log(max/min) + 1).asInt };
				var hText = ["10 Hz", "100 Hz", "1 kHz", "10 kHz"];
				var vAxis = (1..4).collect{|n|
					n=n/4;
					[ (h -((h-2) * n)).asInt - 1, controlSpec.mapNoRound(n).round(0.01) ++ (controlSpec.units) ];
				};

				sw   = ((w-2)/size).clip(1,inf);

				Pen.fillColor_(Color(1,1,1,0.5));

				if (lowMid.notNil) { Pen.fillOval( Rect.aboutPoint(((sw*lowMid+1)@(h/2+0.5)),3,3) ) };
				if (midHigh.notNil) { Pen.fillOval( Rect.aboutPoint(((sw*midHigh+1)@(h/2+0.5)),3,3) ) };

				Pen.smoothing_(false);
				hAxis.do{|x,n|
					Color(1,1,1,hAxisShaeds[n]/3).set;
					if (hAxisShaeds[n]==1) {
						Pen.lineDash_(FloatArray[w,0]);
					}{
						Pen.lineDash_(FloatArray[4,2]);
					};
					Pen.moveTo(x@0);
					Pen.lineTo(x@h);
					Pen.stroke;
				};
				Pen.lineDash_(FloatArray[w,0]);
				Color(1,1,1,0.2).set;
				vAxis.do{|list|
					var y = list[0];
					Pen.moveTo(0@y);
					Pen.lineTo(w@y);
					Pen.stroke;
				};
				Color(1,1,1).set;
				Pen.smoothing_(true);
				Pen.font_(Font("Helvetica",13));
				hAxisT.do{|x,n|
					Pen.stringLeftJustIn(hText[n],Rect(x+2,h-15,80,15));
				};
				vAxis.do{|list|
					var y = list[0];
					var string = list[1];
					Pen.stringRightJustIn(string,Rect(w-182,y+2,180,15));
				};
			};
			// end scale
		};

	}

	multiDoubleArray_{|array|
		multiDoubleArray=array;
		if (view.notClosed) {  {view.refresh}.deferIfNeeded  }
	}

	// add the controls
	addControls{
		view.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			size = multiDoubleArray.size;
			lx = ((x-1)/sw).asInt.clip(0,size-1);
			ly = 1-(((y-1)/(h-2)).clip(0,1));
			guiFeedbackAction.value(this,lx,ly); // before refresh
			multiDoubleArray.putMap(lx, ly);
			multiFloatAction.value(this,multiDoubleArray);
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
			guiFeedbackAction.value(this,nx,ny); // before refresh
			if ((nx-lx).abs==0) {
				multiDoubleArray.putMap(nx, ny);
			}{
				if (lx<nx) {
					lx.for(nx,{|i|
						var frac = (i-lx) / ((lx-nx).abs);
						multiDoubleArray.putMap(i, ( ny * frac ) + ( ly * (1 - frac) ) );
					});
				}{
					nx.for(lx,{|i|
						var frac = (i-nx) / ((lx-nx).abs);
						multiDoubleArray.putMap(i, ( ly * frac ) + ( ny * (1 - frac) ) );
					});
				};
			};
			lx = nx;
			ly = ny;
			multiFloatAction.value(this,multiDoubleArray);
			this.refresh;
			if (editMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}
		};

		view.mouseUpAction_{
			feedbackText = nil;
			this.refresh;
		};

	}

	refresh{
		if (view.notClosed) {
			// drop of if tab is hiddden
			//if ( (parent.isKindOf(MVC_TabView))and:{parent.isHidden} ) { ^this };
			parentViews.do{|view| if (view.isVisible.not) { ^this }};
			// else
			graphView.refresh;
		}
	}

	refreshScale{
		if (view.notClosed) {
			{view.refresh}.deferIfNeeded;
		}
	}

	bounds_{|argRect|
		rect=argRect;
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		if (view.notClosed) {
			view.bounds_(rect);
			graphView.bounds_(rect);

		};
	}

}