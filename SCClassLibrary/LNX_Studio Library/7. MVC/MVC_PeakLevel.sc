
// LNX_SeqView & LNX_VelocityView (use zero value)
// zero value works but is a little messy, tidy up later
/*
(
w=MVC_Window();
MVC_PeakLevel(w,Rect(10,10,20,200));

w.create;
)
*/

MVC_PeakLevel : MVC_View {

	var <>seqItems, lastItem, <>zeroValue;
	
	var <>direction=\vertical;

	initView{
		colors=colors++(
			'background'		: Color(0,0,0,0),
			'slider'			: Color(0,0,0,0.75),
			'sliderDisabled'	: Color.grey*1.2
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
		if (w>h) { direction=orientation=\horizontal }{ direction=orientation=\vertical };
		if (view.notClosed) { view.bounds_(rect) };
		this.adjustLabels;
	}
	
	createView{
		view=SCUserView.new(window,rect)
			.drawFunc={|me|
				var val, r,c;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(true);
					
					colors[enabled.if(\background,\disabled)].set;
					Pen.fillRect(Rect(0,0,w,h));
					
					
					
					if (midiLearn) {
						c=colors[\midiLearn];
					}{
						c=colors[enabled.if(\slider,\sliderDisabled)]
					};
					
					if (controlSpec.notNil) {
						val=controlSpec.unmap(value); // this will always give (0-1)
					}{
						val=value.clip(0,1);
					};
					

					if (direction==\vertical) {
						
						r=Rect(0,((h-w)*(1-val)),w,w);
						
					}{
						r= Rect( 3,3,  (w-6)*(val), h-6);
						
					};
					

					Pen.smoothing_(true);

					Pen.fillColor_(c);
					DrawIcon.symbolArgs(\back,r.insetBy(-5,-5).moveBy(1,0));
			
					
				};
			};
	}
	
	addControls{
		
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
							this.viewValueAction_(val,nil,true,false,buttonNumber);
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
								seqItems[thisItem].viewValueAction_(
									thisValue,nil,true,false,buttonNumber);
							};
						}{ // draw a line
							if (thisItem>lastItem) {
								lastValue=seqItems[lastItem].value;
								size=thisItem-lastItem+1;
								size.do({|i|
									val=((i/(size-1))*thisValue)+(1-(i/(size-1))*lastValue);
									if (seqItems[i+lastItem].value!=val) {
										seqItems[i+lastItem].viewValueAction_(
											val,nil,true,false,buttonNumber);
									};
								});
							}{
								lastValue=seqItems[lastItem].value;
								size=lastItem-thisItem+1;
								size.do({|i|
									val=((i/(size-1))*lastValue)+(1-(i/(size-1))*thisValue);
									if (seqItems[i+thisItem]!=val) {
										seqItems[i+thisItem].viewValueAction_(
											val,nil,true,false,buttonNumber);
									};
								});
							};
						};
						lastItem=thisItem;
					}{	
						if (buttonPressed!=2) {
							if (thisValue!=value) {this.viewValueAction_(
									thisValue,nil,true,false,buttonNumber)};
						};
					};
				};
			};
		}
	}
	
}
