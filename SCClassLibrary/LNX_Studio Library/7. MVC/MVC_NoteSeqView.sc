
// LNX_NoteSeqView (use zero value)
// zero value works but is a little messy, tidy up later

MVC_NoteSeqView : MVC_View {

	var <>seqItems, lastItem, <>zeroValue=0, <buttonWidth=8, ah;

	initView{
		colors=colors++(
			'slider'			: Color.orange,
			'border'			: Color.grey,
			'sliderDisabled'	: Color.grey*1.2,
			'belowZero'			: Color.red
		);
	}

	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				var val;
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);
					colors[enabled.if(\background,\disabled)].set;
					Pen.fillRect(Rect(0,0,w,h));
					if (midiLearn) {
						colors[\midiLearn].set;
					}{
						(colors[enabled.if(\border,\disabled)]).set;
					};
					Pen.strokeRect(Rect(1,1,w- 3,h- 3));
					if (midiLearn) {
						colors[\midiLearn].set;
					}{
						if (enabled) {
							(colors[\slider]*1.4).set;
						}{
							colors[\sliderDisabled].set;
						};
					};

					if (controlSpec.notNil) {
						val=controlSpec.unmap(value); // this will always give (0-1)
					}{
						val=value.clip(0,1);
					};


					ah=h-6-buttonWidth;

					if ((zeroValue.notNil) and: {value<zeroValue}) {
						// filled rect below zero
						colors[\belowZero].set;
						val=1.neg/(h-6);
						Pen.fillRect(Rect(3,(h-6)*(1-val)+3,w-6,(h-6)*(val)));
					}{
						if (enabled) {
							Pen.fillRect(Rect(3,(ah*(1-val)+3).asInt,w-6,buttonWidth));
							if (midiLearn.not) {
								(colors[\slider]/2).set;
								Pen.fillRect(
									Rect(3+1,1+(ah*(1-val)+3).asInt,w-6-1,buttonWidth-1));
							}
						}{
							Pen.fillRect(Rect(3,(ah*(1-val)+3).asInt,w-6,buttonWidth));
						};
					};

					if ((enabled) && (value>=zeroValue) && (midiLearn.not)) {
						colors[\slider].set;
						Pen.fillRect(Rect(4,(ah*(1-val)+3).asInt+1,w-8,buttonWidth-2));

						Color(colors[\slider].red/2,colors[\slider].green/2,
							colors[\slider].blue/2).set;
						Pen.fillRect(Rect(5,(ah*(1-val)+3).asInt+2,w-10,buttonWidth-4));
					};


				};
			};
	}

	// this is a copy of flatslider controls
	// and doesn't take the button width into account, to do later, maybe

	addControls{

		view.mouseUpAction={ MVC_LazyRefresh.mouseUp };

		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var val;
			MVC_LazyRefresh.mouseDown;
			if (modifiers.asBinaryDigits[4]==0) {  // if apple not pressed because of drag
				if (editMode) {
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
					};

					if ((buttonNumber==2)and:{hasMIDIcontrol}) {
						this.toggleMIDIactive;
					}{
						val=(1-((y-t-3)/(h-6))).clip(0,1);
						if (controlSpec.notNil) { val=controlSpec.map(val) };
						this.viewValueAction_(val,nil,true,false);
					};
				};
			};
		};

		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var thisItem, lastValue, size, thisValue, val;
			if (editMode) {
				this.moveBy(x-startX,y-startY)
			}{
				x=x+l;
				y=y+t-1;
				thisValue=(1-((y-t-3)/(h-6))).clip(0,1);
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

	// width of button
	buttonWidth_{|width|
		buttonWidth=width;
		if (view.notClosed) {view.refresh};
	}

}
