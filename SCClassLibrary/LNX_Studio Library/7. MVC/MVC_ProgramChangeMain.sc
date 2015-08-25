
// LNX_MyListView

MVC_ProgramChangeMain : MVC_View {

	var <>ww=60, <>hh=19, <noPOP=128;

	var <actualProgram, <flash=1;

	initView{
		colors=colors++(
			'background'	: Color(0,0,0,0.2),
			'on'			: Color(0.8,0.8,1)/2,
			'off'		: Color.black
		);
		canFocus=false;
		font=Font("Helvetica",12);
	}
	
	createView{

		view=UserView(window,Rect(rect.left,rect.top,ww,hh*noPOP+1))
			.drawFunc_{|me|	
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);
					colors[\background].set;
					Pen.fillRect(Rect(0,0,ww,hh**noPOP+1));
					Pen.font_(font);
					Pen.smoothing_(false);
					Pen.strokeColor_(Color.black);

					noPOP.do{|y|
						if (y==actualProgram) {
							// program now
							Color.black.set;
							Pen.fillRect(Rect(0,y*hh,ww,hh));
						}{
							// to be
							if (y==model.value) {
								(Color(1,1,1,0.2)*(1-flash)).set;
								Pen.fillRect(Rect(0,y*hh,ww,hh));
								Pen.strokeColor_(Color.black);
							}{
								Pen.strokeColor_(Color.black);
							};
							Pen.strokeRect(Rect(0,y*hh+1,ww,hh));
	
							
						};
					};
					
					Pen.fillColor_(colors[\on]+0.2);
					Pen.smoothing_(true);

					noPOP.do{|y|
						if (y==actualProgram) {
							if (y==model.value) {
								Pen.fillColor_(Color.white*((flash)+1/2));
							}{
								Pen.fillColor_(Color.white);
							};
							Pen.stringCenteredIn((y+1).asString,Rect(0,y*hh,ww,hh));
							
						}{	
							if (y==model.value) {
								Pen.fillColor_(Color.white*((1-flash)+1/2));
							}{	
								Pen.fillColor_(colors[\off]+0.2);
							};
							
							Pen.stringCenteredIn((y+1).asString,Rect(0,y*hh+1,ww,hh));
						};	
					}	
					
				}; // end.pen
			};

	}

	addControls{
		var val, val2;
		
		view.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
			var val;
			y=y-3;
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (modifiers.asBinaryDigits[4]==0) {  // if apple not pressed because of drag
				if (modifiers==524576) { buttonNumber=1 };
				if (modifiers==262401) { buttonNumber=2 };
				if (editMode) {
					lw=lh=nil;
					startX=x;
					startY=y;
					rect.bounds.postln;
				}{
					if (buttonNumber==2) {
						this.toggleMIDIactive
					}{
						val=(y/hh).asInt.clip(0,noPOP-1);
						// if (val!=value) { // no checking for inequality
						this.viewDoValueAction_(val,nil,true,false);
					}
				}
			}				
		};
		view.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			if (modifiers.asBinaryDigits[4]==0) {  // if apple not pressed because of drag
				if (editMode) { this.moveBy(x-startX,y-startY) };
			}
		};	
	}
	
	actualProgram_{|prg|
		actualProgram=prg;
		{this.refresh}.defer;
	}
	
	noPOP_{|num|
		if (noPOP!=num) {
			noPOP=num;	
			{
				if (view.notNil) { this.bounds_(Rect(rect.left,rect.top,ww,hh*noPOP+1)) };
			}.defer;
		};
	}
	
	flash_{|f|
		flash=f;
		{this.refresh}.defer;
	}
		
	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		if (view.notClosed) {
			"midi learn?".postln;
		};
		labelGUI.do(_.refresh);
	}

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		if (view.notClosed) {
			//view.background_(colors[enabled.if(\background,\backgroundDisabled)]);
		};
		labelGUI.do(_.refresh);
	}
	
	// normally called from model
	value_{|val|
		val=val.round(1).asInt.clip(0,noPOP-1);
		if (value!=val) {
			value=val;
			this.refresh;
		};
	}

}
