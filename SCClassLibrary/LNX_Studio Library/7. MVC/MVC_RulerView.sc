
// LNX_RulerView

MVC_RulerView : MVC_View {

	var <>buttonWidth=18, rulerTemplate, ruler, rulerStep=4, <>steps=32;

	// set your defaults
	initView{
		
		// [ 0:onBG, 1:offBG, 2:onOffText, 3:onBGUnen, 4:offBUnen, 5:onOffTextUnen ] 
		//colors=[Color.ndcOnOffON,Color.ndcOnOffOFF,Color.ndcOnOffText,
		//		Color.ndcOnOffONUen,Color.ndcOnOffOFFUen,Color.ndcOnOffTextUnen];
		
		colors=colors++(
			'background'			: Color.grey/3,
			'backgroundDisabled'	: Color.grey/2,
			'string'				: Color.black,
			'on'					: Color.orange,
			'disabled'			: Color(0.4,0.4,0.4)
		);
		

		
		rulerTemplate=#[
			[1],  // 1
			[1,0.25],  // 2
			[1,0.25,0.25], // 3
			[1,0.25,0.5 ,0.25],  // 4
			[1,0.25,0.25,0.25,0.25],  // 5
			[1,0.25,0.25,0.5 ,0.25,0.25],  // 6
			[1,0.25,0.25,0.25,0.25,0.25,0.25],  // 7
			[1,0.25,0.25,0.25,0.5 ,0.25,0.25,0.25],  // 8
			[1,0.25,0.25,0.5 ,0.25,0.25,0.5 ,0.25,0.25],  // 9
			[1,0.25,0.25,0.25,0.25,0.5 ,0.25,0.25,0.25,0.25], // 10
			[1,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25],  // 11
			[1,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25],  // 12
			[1,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25],  // 13
			[1,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25],  // 14
			[1,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25,0.25],  // 15
			[1,0.25,0.25,0.25,0.35,0.25,0.25,0.25,0.5 ,0.25,0.25,0.25,0.35,0.25,0.25,0.25]  // 16
		];
		
		font=Font("Helvetica",9);
		
	}
	
	// make the view
	createView{
		view=SCUserView.new(window,rect)
			.drawFunc={|me|
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					if (showLabelBackground) {
						Color.black.alpha_(0.2).set;
						Pen.fillRect(Rect(0,0,w,h));
					};
					
					Pen.smoothing_(false);
					colors[enabled.if(\background,\backgroundDisabled)].set;
					Pen.fillRect(Rect(0,0,w,h));
					colors[enabled.if(\on,\disabled)].set;
					Pen.font_(font);
					(steps).do({|i|
						Pen.moveTo((i*buttonWidth)@
							(h*(1-( rulerTemplate.clipAt(value-1).wrapAt(i) ))));
						Pen.lineTo((i*buttonWidth)@h);
						
						if ((i%value)==0) {
							Pen.fillColor_(colors[\string]);
							Pen.stringLeftJustIn(((i/value).asInt+1).asString,
								Rect(i*buttonWidth+1,0,buttonWidth,h));
							colors[enabled.if(\on,\disabled)].set;
						};
							
					});
					
					Pen.moveTo((steps*buttonWidth-1)@
						(h*(1-(rulerTemplate.clipAt(value.asInt-1).wrapAt(steps)))));
					Pen.lineTo((steps*buttonWidth-1)@h);
					Pen.stroke;

				}; // end.pen
			};		
	}
	
	// add the controls
	addControls{
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) {lw=lh=nil; startX=x; startY=y; view.bounds.postln }; // for moving
			buttonPressed=buttonNumber;
			evaluateAction=true;
			if (modifiers==524576) { buttonPressed=1 };
			if (modifiers==262401) {buttonNumber=2};
			if (buttonNumber==2) { this.toggleMIDIactive };
		};
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			if (editMode) { this.moveBy(x-startX,y-startY) };
		};
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				if (buttonPressed==0) {
					//this.viewValueAction_(value+1,nil,true,false);
				};
			};
		};
	}

}
