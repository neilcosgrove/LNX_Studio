
// MVC_PinSlider

/* (
w=MVC_Window();
m=0.asModel;
q=MVC_PinSlider(w,m,Rect(20,20,16,100)).width_(8);
r=MVC_PinSlider(w,m,Rect(50,20,14,100)).width_(6);
r=MVC_PinSlider(w,m,Rect(80,20,12,100)).width_(4);
w.create;
) */


MVC_PinSlider : MVC_FlatSlider {

	var down=false, <>width=6, <>hilite=false, <>hiliteMode='innner', <>zeroValue;
	
	var <>left=false, <>right=false;

	initView{
		colors=colors++(
			'background'	: Color.black,
			'disabled'	: Color(0.3,0.3,0.3,0.5),
			'on'			: Color.green,
			'hilite'		: Color.green,
			'hiliteTrue'	: Color.white
		);
		if (w>h) { direction=\horizontal }{ direction=\vertical };

	}
	
	createView{
		view=SCUserView.new(window,rect)
			.drawFunc={|me|
				var w2,h2, rect2, x2, zeroVal, val;
				
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				
				if (controlSpec.notNil) { val = controlSpec.unmap(value) }{ val = value };
							
				zeroVal=zeroValue ? 0;	
						
				Pen.use{
					if (showLabelBackground) {
						Color.black.alpha_(0.2).set;
						Pen.fillRect(Rect(0,0,w,h));
					};
					
					
					Pen.smoothing_(true);
					
					
					Color(0,0,0,0.25).set;
					
					
					x2=(w/2);
					h2=h.clip(0,w);
					
					
					if (left)  {
						Pen.fillRect(Rect(0,6,x2,h-(h2/2)-5 ));
					};
					if (right) {
						Pen.fillRect(Rect(w-x2,6,x2,h-(h2/2)-5 ));
					};
					
					
					
					if (midiLearn) {
						colors[\midiLearn].set;
					}{
						colors[enabled.if(\background,\disabled)].set;
					};
					
					Pen.width_(width);
			
					Pen.line((w/2)@width,(w/2)@(h-width));

					Pen.addWedge((w/2)@width,width/200,0,-2pi/2);
					Pen.addWedge((w/2)@(h-width),width/200,0, 2pi/2);
					Pen.stroke;
					
					
					Pen.stroke;
					
					if (midiLearn) {
						colors[\background].set;
					}{
						if (enabled)
							{ colors[\hilite].alpha_(1).set }
							{ (colors[\hilite].alpha_(0.5)*0.5).set };
					};
					
					Pen.width_(width-3);
					Pen.capStyle_(1);
					
					Pen.line((w/2)@(val.map(1,0,width+1,h-width-1)),
						(w/2)@(h-width-1));
					
					Pen.stroke;
					
					if ((hilite)and:{hiliteMode!='inner'}and:{val>zeroVal}) {
						colors[\hiliteTrue].set;
					}{
						if (midiLearn) {
							colors[\midiLearn].set;
						}{
							colors[enabled.if(\background,\disabled)].set;
						};
					};
					
					w2=w.clip(0,h);
					h2=h.clip(0,w);
					
					Pen.width_(2);
					
					rect2=Rect(0,(h-h2)*(1-val),w2,h2);
					
					if ((down)and:{val>zeroVal}) { rect2=rect2.insetBy(2) };
					
					if (val==0) { rect2=rect2.insetBy(3) };
					
					Pen.fillOval(rect2);
					if (val>zeroVal) {
						if (midiLearn) {
							colors[\background].set;
						}{
							if (enabled) { colors[\on].set } { colors[\on].alpha_(0.5).set };
						};
						Pen.fillOval(rect2.insetBy(2,2));
						
						if ((hilite)and:{hiliteMode=='inner'}and:{val>zeroVal}) {
							colors[\hiliteTrue].set;
						}{
							if (midiLearn) {
								colors[\midiLearn].set;
							}{
								colors[enabled.if(\background,\disabled)].set;
							};
						}; 
						Pen.fillOval(rect2.insetBy(4,4));
					};
					
					
				}
				
			};
	}
	
	addControls{
		
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var val;
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			if (modifiers.asBinaryDigits[4]==0) {  // if apple not pressed because of drag
				if (editMode||viewEditMode) {
					lw=lh=nil;
					startX=x;
					startY=y;
					view.bounds.postln;
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
					}{
						down=true;
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
		};
		
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var thisItem, lastValue, size, thisValue, val;
			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY)
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
		
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (down==true) {
				down=false;
				this.refresh;
			};
		};
		
				
	}

	
}
