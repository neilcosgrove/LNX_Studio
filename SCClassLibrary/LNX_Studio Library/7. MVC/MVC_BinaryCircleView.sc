
/////////// MVC_OnOffView /////////////////////////////////////////////////////////////////

MVC_BinaryCircleView : MVC_View {

	var <>down = false;
	
	var <>permanentStrings; // used primarily in studio_inst onOff button to stop its string changing 


	// set your default colours
	initView{
		colors=colors++(
			'background'		: Color.black,			
			'stringOn'		: Color.ndcOnOffText,
			'stringOff'		: Color.ndcOnOffText,
			'upOn'			: Color.ndcOnOffON,
			'downOn'			: Color.ndcOnOffONUen,
			'upOff'			: Color.ndcOnOffOFF,
			'downOff'			: Color.ndcOnOffOFFUen
			
		);
	}
	
	// make the view
	createView{
		view=SCUserView.new(window,rect)
			.drawFunc={|me|
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
			
				if (down.not) {
					if (value==1) { (colors[\downOn]/4).set; }{ (colors[\downOff]/4).set };
				}{
					Color.black.set;
				};
				Pen.fillOval(Rect(0,0,w,h));

				if (midiLearn) {
						colors[\midiLearn].set;
					}{
					if (down.not) {
						if (value==1) { colors[\upOn].set; }{ colors[\upOff].set };
					}{
						if (value==1) { colors[\downOn].set; }{ colors[\downOff].set };
					};
				};	
				Pen.fillOval(Rect(2-0.5,2-0.5,w- 4+1,h- 4+1));
				Pen.font_(font);
				Pen.fillColor_((value==1).if(colors[\stringOn],colors[\stringOff]));
				Pen.smoothing_(true);
				if (value==1) {
					Pen.stringCenteredIn((permanentStrings?strings)@0,Rect(0,0,w,h));
				}{
					Pen.stringCenteredIn((permanentStrings?strings)@@1,Rect(0,0,w,h));
				};
			}; // end.pen
		};		
	}
	
	addControls{
		
		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode) {
				lw=lh=nil;
				startX=x;
				startY=y;
				view.bounds.postln;
			};
			
			buttonPressed=buttonNumber;
			evaluateAction=true;
			if (modifiers==524576) { buttonPressed=1 };
			if (modifiers==262401) {buttonNumber=2};
			if (buttonNumber==2) {
				this.toggleMIDIactive
			}{
				down=true;
				me.refresh;
			};
		};
		
		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			if (editMode) { this.moveBy(x-startX,y-startY) };
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}) {
				if (down.not) {
					down=true;
					me.refresh;
				}
			}{
				if (down) {
					down=false;
					me.refresh;
				}
			};
				
		};
		
		view.mouseUpAction={|me, x, y, modifiers, buttonNumber, clickCount|
			var xx=x/w, yy=y/h;
			down=false;
			if ( (xx>=0)and:{xx<=1}and:{yy>=0}and:{yy<=1}and:{evaluateAction}and:{editMode.not}) {
				if (buttonPressed==0) {
					this.viewDoValueAction_((value>=0.5).if(0,1),nil,true,false);
				}{
					if (buttonPressed==1) {
						this.viewDoValueAction2_((value>=0.5).if(0,1),nil,true,false);
					}
				};
			};
		};
		
	}


}
