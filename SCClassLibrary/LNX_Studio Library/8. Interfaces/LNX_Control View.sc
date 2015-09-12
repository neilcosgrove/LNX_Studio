
// midi control editor view

LNX_MIDIControlInterface {

	var	<parent, window, <presetView, slider, <enabled=true, <>onClose,
	rect, l, t, w, h, viewIndex, <parentSynth, <items, <index, nol, itemHeight,
	<>action, <>doubleClickAction, <>deleteKeyAction, <startX,startY, <>mouseMoveAction,
	<>mouseUpAction, <>flipAction;

	*new { arg window,rect,parentSynth; ^super.new.init(window,rect,parentSynth); }
	
	init { arg argWindow,argRect,argParentSynth;
	
		var y;
		
		parent = argWindow.asView; // actual view
		//parent.add(this);  // not a good idea
		parentSynth=argParentSynth;
		
		window=argWindow;
		rect=argRect;
		l=rect.bounds.left;
		t=rect.bounds.top;
		w=rect.bounds.width;
		h=rect.bounds.height;
		nol=10;
		itemHeight=h/nol;
		
		items=[];
		
		viewIndex=0;
		
		presetView=MVC_UserView(window,Rect(l,t,w,h))
		.drawFunc_{|me|
	
			var drawMin,drawMax,
				bgColor = Color(0.1,0.1,0.1),
				text1 = Color(0.8,0.8,0.8),
				select = Color(0.35, 0.45, 0.6, 1)
				;
	
			y=viewIndex;
	
			Pen.smoothing_(false);
			Pen.use{
			
				bgColor.set;
				Pen.fillRect(Rect(0,0,w-16,h));
					
				Color.ndcNameText2.set;
				Pen.fillColor_(text1);
				Pen.smoothing_(false);
				Pen.font_(Font("Helvetica",9));
				
				drawMin=(y.neg/itemHeight).asInt;
				drawMax=drawMin+nol+1;
				
				for (drawMin+1,drawMax,{|i|
					Color(0.4,0.4,0.4).set;
					Pen.moveTo(0@(itemHeight*i+y));
					Pen.lineTo((w-17)@(itemHeight*i+y));
					Pen.stroke;
					
					if ((i-1)==index) {
						select.set;
						Pen.fillRect(Rect(0,(itemHeight*index)+y+2,w-17,itemHeight-4));
						Color.new(1,1,1).set;
						Pen.fillColor_(Color.ndcStudioText2);
						Pen.stringCenteredIn(
							i.asString,Rect(0,(itemHeight*(i-1))+y,18,itemHeight));
						Color.new(0.6,0.4,0.4).set;
						Pen.fillColor_(text1);
					}{
						Pen.stringCenteredIn(
							i.asString,Rect(0,(itemHeight*(i-1))+y,18,itemHeight));
					};
					
				});
				Pen.stroke;	

				Pen.smoothing_(true);
				Pen.fillColor_(Color.new(0.75,1,0.3));
				Pen.font_(Font("Helvetica Bold",10));
				
				do (items[drawMin..drawMax],{|i,j|
					var yPos;
					if ((j+drawMin)==index) {
						Pen.fillColor_(Color.ndcStudioText2);
						//Pen.font_(Font("Helvetica Bold",10));
					}{
						Pen.fillColor_(text1);
						//Pen.font_(Font("Helvetica",10));
					};
					j=j+1;
					yPos=(itemHeight*(j+drawMin-1))+y;
//					Pen.stringLeftJustIn(
//						(i[4]++" ("++i[3].asString.drop(2)++")")[0..15],
//						Rect(19,yPos,100,itemHeight));
					Pen.stringLeftJustIn(i[4][0..15],Rect(19,yPos,100,itemHeight));
						
					if ((j+drawMin- 1)==index) {
						select.set;
					}{
						bgColor.set;
					};
					Pen.smoothing_(false);
					Pen.fillRect(Rect(115,yPos+2,103,itemHeight-4));
					Pen.smoothing_(true);
					Pen.stringLeftJustIn(i[8],Rect(119,yPos,100,itemHeight));
					Pen.stringLeftJustIn(i[0],Rect(219,yPos,100,itemHeight));
					if ((j+drawMin- 1)==index) {
						select.set;
					}{
						bgColor.set;
					};
					Pen.smoothing_(false);
					Pen.fillRect(Rect(319,yPos+2,103,itemHeight-4));
					Pen.smoothing_(true);
					Pen.stringRightJustIn((i[1]+1).asString,Rect(319,yPos,30,itemHeight));
					Pen.stringRightJustIn(i[2].asString,Rect(349,yPos,30,itemHeight));
					Pen.stringRightJustIn(i[9].asString,Rect(379,yPos,30,itemHeight));
					Pen.stringRightJustIn(i[10].asString,Rect(409,yPos,30,itemHeight));
					Pen.stringCenteredIn(["Knob","Toggle","On/Off","Rotary","Flip flop"][i[11]],Rect(439,yPos,47,itemHeight));
				});
				
				Pen.smoothing_(false);
				[18,119,219,319,349,379,409,439].do({|i| Pen.moveTo(i@0); Pen.lineTo(i@h);});
				Color.ndcStudioBG.set;
				Pen.stroke;
								
			}; // end.pen
				
		};
		
		// @TODO: new Qt "key" codes
		presetView.keyDownAction_{|me, char, modifiers, unicode,keycode,key|
		
			//["mods", modifiers,"unicode", unicode, "keycode", keycode].postln;
			
			if ((keycode==126)&&(items.size>0)) {
				if (index.isNumber) {
					index=(index-1).wrap(0,items.size- 1);
					this.refreshToSelection;
					action.value(this); //up arrow
				}{
					index=items.size-1;
					this.refreshToSelection;
					action.value(this); //up arrow no items selected
				}
			};
			
			if ((keycode==125)&&(items.size>0)) {
				if (index.isNumber) {
					index=(index+1).wrap(0,items.size- 1);
					this.refreshToSelection;
					action.value(this); // down arrow
				}{
					index=0;
					this.refreshToSelection;
					action.value(this); //down arrow no items selected
				}
			};
			
			if ((keycode==51)&&(items.size>0)) { // delete key
				if (index.isNumber) {
					deleteKeyAction.value(this);
				}
			};
		
		};
		
		presetView.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var xx=x/w, yy=y/h, win, numBox;
			startY=y;
			y=(y-2-viewIndex/itemHeight).asInt;
			startX=0;
			[119,219,319,349,379,409,439].do({|xx,i| if (x>xx) { startX=i+1 }});
			if (y<items.size) {
				index=y;
				presetView.refresh;
				action.value(this, x, y, modifiers, buttonNumber, clickCount);
			};
			if ((clickCount==2)and:{y<items.size}) { };
		};
		
		presetView.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			y-startY;
			if (index.notNil) {
				mouseMoveAction.value(this,index,startX,y-startY);
			};
		};
		
		presetView.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			y-startY;
			if (index.notNil) {
				mouseUpAction.value(this,index,startX,y-startY);
			};
		};
		
		presetView.refresh;
		
		slider=MVC_SmoothSlider.new(window,Rect(l+w-15,t,15,h))
			.action_{|me|
				viewIndex=((1- me.value)*((items.size-10).clip(0,9999)*itemHeight)).asInt.neg;
				presetView.refresh
			} 
			.canFocus_(false)
			.value_(1)
			.color_(\background,Color.black)
			.color_(\knob,Color(1,1,1)*0.8);	

	}
	
	items_{|i|
		var s;
		items=i;
		if (items.size==0) { index=nil };
		s=(i.size-10).clip(0,i.size);
		if (s>0) {
			s=(h/(s/10+1)).clip(15,h) 
		}{
			s=h
		};
		slider.thumbSize_(s);
		this.setSliderToViewIndex;
	}

	resetViewIndex{ viewIndex=0; presetView.refresh }

	deleteItem{ if (index.isNumber) { deleteKeyAction.value(this) } }
	
	flip { if (index.isNumber) { flipAction.value(this) } }
		
	setSliderToViewIndex { 
		if (viewIndex.isNil) {
			slider.value_(1)
		}{
			slider.value_(1-((viewIndex.neg/itemHeight)/((items.size-10).clip(1,items.size+1))))
		};
	}
	
	refreshToSelection{
		var y1,y2;
		if (index.isNumber) {
			y1=(itemHeight*index)+viewIndex;
			y2=(itemHeight*(index+1))+viewIndex;
			if (y1<0) {
				viewIndex=((index-9).clip(0,9999)*itemHeight).neg;
				this.setSliderToViewIndex
			}; // this doesn't work properly
			if (y2>h) {
				viewIndex=((index).clip(0,items.size-10)*itemHeight).neg;
				this.setSliderToViewIndex
			};
		};
		if (window.isClosed.not) {presetView.refresh}
	}
	
	index_{|i|
		index=i;
		if (window.isClosed.not) { presetView.refresh }
	}
	
	indexNoRefresh_{|i| index=i }
	
	indexToSelection_{|i|
		index=i;
		this.refreshToSelection;
	}
	
	focus{ presetView.focus }
	
	enabled_{ |bool|
		enabled=bool;
		presetView.enabled_(bool);
	}
	
	//prClose { onClose.value; }
	
	//prRemove { "divider remove".postln; onClose.value }
	
	refresh{ if (window.isClosed.not) {presetView.refresh} }
	
} // end.Preset View

