
// a Circle sequencer view

MVC_PinSeqView : MVC_View {
	
	var <>seqItems, lastItem, <>zeroValue, <startVal;

	var <down=false;
	
	var <>hilite=false, <>left=false, <>right=false;

	// set your defaults
	initView{ 
		colors=colors++(
			'background'	: Color.black,
			'on'			: Color.green,
			'off'		: Color.black
		);
		isSquare=true;
	}
	
	// make the view
	createView{
		view=SCUserView.new(window,rect)
			.drawFunc={|me|
				var w2,h2, rect2, x2;
				
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					
					// clip to a square
					w2=w.clip(0,h);
					h2=h.clip(0,w);
					Pen.smoothing_(true);
					
					if (enabled) {
						Color.black.set;
					}{
						Color.black.alpha_(0.5).set;
					};
					
					Pen.width_(8);
					if (down) { if (value>0) { x2=5 } { x2=4 } }{ x2=3 };
					if (left)  { Pen.line((0)@(h2/2),(x2)@(h2/2)) };
					if (right) { Pen.line((w-x2)@(h2/2),(w)@(h2/2))};
					Pen.stroke;
					
					if (midiLearn) {										colors[\midiLearn].set;
					}{
						if (enabled) {
							if (value>0) {
								colors[\background].set;
							}{
								(colors[\background]*0.8).set;
							}	
						}{
							colors[\background].copy.alpha_(0.4).set;
						};
					};
						
					Pen.width_(2);
					if (down) {
						rect2=Rect(4,4,w2-8,h2-8);
					}{
						rect2=Rect(2,2,w2-4,h2-4);
					};
					if (value>0) {
						if (hilite) {
							Pen.fillOval(Rect(0,0,w2,h2));
						}{
							Pen.fillOval(rect2);
						}
					}{
						if (hilite) {
							Pen.fillOval(rect2);
						}{
							Pen.strokeOval(rect2);
						}
					};
					
					if (midiLearn) {										colors[\midiLearn].set;
					}{
						if (enabled) {
							(colors[(value>0).if(\on,\off)]*(value.map(0,1,0.6,1))).set;
						}{
							(colors[(value>0).if(\on,\off)]+0.5/1.7).set;
						};
					};
					Pen.fillOval(rect2.insetBy(2,2));
				}; // end.pen
			};		
	}
	
	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (modifiers==524576) { buttonNumber=1 };
			if (modifiers==262401) { buttonNumber=2 };
			buttonPressed=buttonNumber;
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			if (modifiers.asBinaryDigits[4]==0) { // check apple not pressed because of drag
				if (editMode||viewEditMode) {
					lw=lh=nil; startX=x; startY=y;
					if (verbose) {view.bounds.postln}; // for moving
				}{
					x=x+l;
					y=y+t-1;
					evaluateAction=true;
					if (buttonPressed==1) {
						seqItems.do({|i,j|	
							if ((x>=(i.l))and:{(x<=((i.l)+(i.w)))}) {
								lastItem=j;  // this finds this item index
							}
						});			
						if (value>controlSpec.clipLo) {
							this.viewValueAction_(controlSpec.clipLo,nil,true,false);
						}{
							this.viewValueAction_(controlSpec.clipHi,nil,true,false);
						};
						startVal=value;			
						startY=y;	
					}{
						if (buttonNumber==2) {
							this.toggleMIDIactive
						}{
							down=true;
							view.refresh;
						
						};
					};
				}
			};
			
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var thisItem, lastValue, size, thisValue, val;
			var xx=x/w, yy=y/h;
			
			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed)
			}{				
				if ((buttonPressed==1)and:{seqItems.notNil}) {
					x=x+l;
					y=y+t-1;
					thisValue=(startVal+((startY-y)/75));
					if (controlSpec.notNil) { thisValue=controlSpec.map(thisValue) };
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
						
					if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}) {
						if (down.not) {
							down=true;
							view.refresh;
						};
					}{
						if (down) {
							down=false;
							view.refresh;
						}
					};
				}
			};
		};
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				down=false;
				if (buttonPressed!=1) {
					if (controlSpec.notNil) {
						if (value>controlSpec.clipLo) {
							this.viewValueAction_(controlSpec.clipLo,nil,true,false);
						}{
							this.viewValueAction_(controlSpec.clipHi,nil,true,false);
						};
					}{
						this.viewValueAction_((value>0).if(0,1),nil,true,false);
					};
				}{
					this.refresh;	
				};
			};
			if (down==true) {
				down=false;
				this.refresh;
			};
		};
	}

	down_{|bool|
		down=bool;
		if (view.notClosed) {view.refresh}
	}

}
