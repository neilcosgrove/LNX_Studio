
// LNX_SeqView & LNX_VelocityView (use zero value)
// zero value works but is a little messy, tidy up later
/*
(
w=MVC_Window();
MVC_DownloadStatus(w,Rect(10,10,200,10));
w.create;
)
*/

MVC_DownloadStatus : MVC_View {

	var <>seqItems, lastItem, <>zeroValue;

	var <>direction=\vertical, <>rounded=false, <>radius=10;

	var <>align = \center;

	initView{
		colors=colors++(
			'background'		: Color(0,0,0,0.2),
			'slider'			: Color(0,0,0,0.5),
			'border'			: Color.black,
			'string'			: Color.black
		);
		if (w>h) { direction=\horizontal }{ direction=\vertical };

	}

	// set the bounds, account for direction
	bounds_{|argRect|
		rect=argRect;
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		if (view.notClosed) { view.bounds_(rect) };
		this.adjustLabels;
	}

	createView{
		view = UserView.new(window,rect)
			.drawFunc={|me|
				var val, r,c;
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(true);

					if (value>=0) {

						val = ((value / 100)*(w-4)).asInt;

						Pen.roundedRect(Rect(2,2,w-4,h-4),radius);
						Pen.clip;

						colors[\background].set;
						Pen.fillRect(Rect(2+val,2,w-val,h-4));

						colors[\slider].set;
						Pen.fillRect(Rect( 2,2,val,h-4));

						Pen.color = colors[\border];
						Pen.roundedRect(Rect(2,2,w-4,h-4),radius);
						Pen.stroke;

					}{
						var string = ["Connecting�","Converting�","Ok","Failed"]@(value.abs-1);
						val = value.abs-1;

						if (val==2) {
							var p=(l+w-13)@(t+h-24);


							Pen.capStyle_(1);
							Pen.joinStyle_(1);


							Pen.strokeColor = Color.black;
							Pen.width_(6);
							Pen.moveTo(p);
							Pen.lineTo(p+(2@2));
							Pen.lineTo(p+(2@2)+(4.5@(-4.5)));
							Pen.stroke;
																			Pen.strokeColor = Color(0 ,1,0);
							Pen.width_(2);
							Pen.moveTo(p);
							Pen.lineTo(p+(2@2));
							Pen.lineTo(p+(2@2)+(4.5@(-4.5)));
							Pen.stroke;
						}{

							Pen.fillColor_(colors[\string]);
							Pen.font_(Font("Helvetica",9));

							// stringLeftJustIn
							// stringCenteredIn
							// stringRightJustIn


							if (val==0) { Pen.stringLeftJustIn (string,Rect(0,0,w,h)) };


							if (align==\left) {val==1}; // messy code

							if (val==1) { Pen.stringCenteredIn (string,Rect(0,0,w,h)) };
							//if (val==2) { Pen.stringRightJustIn(string,Rect(0,0,w,h)) };
							if (val==3) { Pen.stringCenteredIn (string,Rect(0,0,w,h)) };
						}
					};

				};
			};
	}

	addControls{
		//
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var val;
			if (locked.not) {
				if (modifiers==524576) { buttonNumber=1  };
				if (modifiers==262401) { buttonNumber=2  };
				buttonPressed=buttonNumber;
				mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
				if (modifiers.asBinaryDigits[4]==0) {  // if apple not pressed because of drag
					if (editMode||viewEditMode) {
						lw=lh=nil;
						startX=x;
						startY=y;
						if (verbose) {view.bounds.postln};
					}{
						x=x+l;
						y=y+t-1;
						evaluateAction=true;
						if (modifiers==524576) { buttonNumber=1  };
						if (modifiers==262401) {buttonNumber=2};
						buttonPressed=buttonNumber;
						if (buttonPressed==1) {
							seqItems.do({|i,j|
								if ((x>=(i.l))and:{(x<=((i.l)+(i.w)))}) {
									lastItem=j;  // draw a line !!!
								}
							});
						};
						if ((buttonNumber==2)and:{hasMIDIcontrol}) {
							this.toggleMIDIactive;
						}{
							if (direction==\vertical) {
								val=(1-((y-t-3)/(h-6))).clip(0,1);
							}{
								val=((x-l-3)/(w-6)).clip(0,1);
							};
							if (controlSpec.notNil) { val=controlSpec.map(val) };
							this.viewValueAction_(val,nil,true,false);
						};
					};
				};
			}
		};

		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var thisItem, lastValue, size, thisValue, val;
			if (locked.not) {
				if (editMode||viewEditMode) {
					this.moveBy(x-startX,y-startY,buttonPressed)
				}{
					x=x+l;
					y=y+t-1;
					if (direction==\vertical) {
						thisValue=(1-((y-t-3)/(h-6))).clip(0,1);
					}{
						thisValue=((x-l-3)/(w-6)).clip(0,1);
					};
					if (controlSpec.notNil) { thisValue=controlSpec.map(thisValue) };
					if ((buttonPressed==1)and:{seqItems.notNil}) {
						seqItems.do({|i,j|	if ((x>=(i.l))and:{(x<=((i.l)+(i.w)))}) { thisItem=j } });
						if (x<seqItems[0].l) { thisItem=0 }; // catch the 1st and last
						if (x>seqItems[seqItems.size-1].l) { thisItem=seqItems.size-1 };
						if (thisItem.isNil) { thisItem=lastItem };
						if (thisItem==lastItem) {
							if (seqItems[thisItem].value!=thisValue) {
								seqItems[thisItem].viewValueAction_(thisValue,nil,true,false);
							};
						}{ // draw a line
							if (thisItem>lastItem) {
								lastValue=seqItems[lastItem].value;
								size=thisItem-lastItem+1;
								size.do({|i|
									val=((i/(size-1))*thisValue)+(1-(i/(size-1))*lastValue);
									if (seqItems[i+lastItem].value!=val) {
										seqItems[i+lastItem].viewValueAction_(val,nil,true,false);
									};
								});
							}{
								lastValue=seqItems[lastItem].value;
								size=lastItem-thisItem+1;
								size.do({|i|
									val=((i/(size-1))*lastValue)+(1-(i/(size-1))*thisValue);
									if (seqItems[i+thisItem]!=val) {
										seqItems[i+thisItem].viewValueAction_(val,nil,true,false);
									};
								});
							};
						};
						lastItem=thisItem;
					}{
						if (buttonPressed!=2) {
							if (thisValue!=value) {this.viewValueAction_(thisValue,nil,true,false)};
						};
					};
				};
			};
		}


	}

}
