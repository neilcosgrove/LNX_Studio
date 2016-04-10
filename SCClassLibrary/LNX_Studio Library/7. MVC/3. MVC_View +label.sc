
// NUMBER LABELS OVERLAP !!

+ MVC_View {

	// change the orientation gui, label and numberbox
	orientation_{|symbol|
		if (symbol==\vert) { symbol=\vertical };
		if (symbol==\horiz) { symbol=\horizontal };
		if (orientation!=symbol) {
			orientation=symbol;
			this.adjustLabels;		
		}
	}
	
	layout_{|symbol|
		if (layout!=symbol) {
			layout=symbol;
			this.adjustLabels;
		}		
	}

	// update label and number position
	adjustLabels{
		if (view.notClosed) {
			if (labelGUI.size>0) {
				label.size.do{|j|	
					labelBounds[j]=this.getLabelRect(j);
					labelGUI[j].bounds_(labelBounds[j]);
				};
				
			};
			if (numberGUI.notNil) {
					numberBounds=this.getNumberRect;
					numberGUI.bounds_(numberBounds);
			}
		};
	}

	// label ////////////////////////////////////////////////////////////////////////////
	
	// get label[i] bounds
	getLabelRect{|i|
		var textBounds, th;
		if (orientation==\vertical) {
			textBounds=label[i].bounds(labelFont);
			if (layout==\normal) {
				^Rect(
					(l+(w/2)-(textBounds.width/2)).asInt,
					t-((textBounds.height-3)*(label.size-i))-4,
					textBounds.width.asInt+1,
					textBounds.height
				);
			}{
				^Rect(
					(l+(w/2)-(textBounds.width/2)).asInt,
					t+h+((textBounds.height-3)*i),
					textBounds.width.asInt+1,
					textBounds.height
				);			
					
			}
		};
		if (orientation==\horizontal) {
			textBounds=label[i].bounds(labelFont);
			th=textBounds.height;
			^Rect(
				(l-textBounds.width.asInt-4),
				( t+ (h/2) + (th*i) - (label.size*th/2) ).asInt,
				textBounds.width.asInt+1,
				textBounds.height
			);
		};
	}
	
	// make a multi text label that can activate MIDI learn
	//  (there are refs that need to be removed here)
	createLabel{
		var textWidth,textHeight;
		labelGUI.do(_.remove); // need to test
		labelGUI=IdentityDictionary[];
		if (label.notNil) {
			labelFont = labelFont ?? { Font("Helvetica",12) };
			labelBounds=nil!(label.size);
			label.size.do{|j|	
				labelBounds[j]=this.getLabelRect(j);
				labelGUI[j]=SCUserView.new(window, labelBounds[j])
				.drawFunc_{|me|
					if (verbose) { [this.class.asString, 'labelFunc' , label].postln };
					if (showLabelBackground) {
						Color.black.alpha_(0.2).set;
						Pen.fillRect(Rect(0,0,labelBounds[j].width,labelBounds[j].height));
					};
					Pen.font_(labelFont);
					if (labelShadow) {
						Pen.fillColor_(Color.black);
						Pen.stringCenteredIn(label[j],
							Rect(1,1,labelBounds[j].width,labelBounds[j].height));
						Pen.stringCenteredIn(label[j],
							Rect(1.5,1.5,labelBounds[j].width,labelBounds[j].height));
					};
					if (enabled) {
						Pen.fillColor_(colors[midiLearn.if(\midiLearn,
							midiLearn2.if(\midiLearn2,\label)
						)]);
					}{
						Pen.fillColor_(colors[midiLearn.if(\midiLearn,
							midiLearn2.if(\midiLearn2,\label)
						)] * 0.8);
						
					};
					Pen.stringCenteredIn(label[j],
					Rect(0,0,labelBounds[j].width,labelBounds[j].height));
		
				}
				.visible_(visible)
				.canFocus_(false)
				.resize_(resize)
				.mouseDownAction_{|me,x, y, modifiers, buttonNumber, clickCount|
					// check apple not pressed because of dra
					if (editMode||viewEditMode) {
						lw=lh=nil;
						startX=x;
						startY=y;
						view.bounds.postln;
					}{
						if (hasMIDIcontrol && labelActivatesMIDI) {
							var toggle = false;
							if ((clickCount>1)&&doubleClickLearn){ toggle = true };
							if (modifiers==262401) { toggle = true  };
							if (buttonNumber>=1  ) { toggle = true  };
							if (toggle) { this.toggleMIDIactive };
						};
					};
				}
				.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl,
					var val;
					if  (editMode||viewEditMode) {
						this.moveBy(x-startX,y-startY);
					}
				}
				.onClose_{
					labelGUI[j].drawFunc_(nil).mouseDownAction_(nil).onClose_(nil);
					labelGUI[j]=nil;
				};
			}; // end.do				
		}; // end.if	
	}
	
	// change the label
	label_{|argLabel|
		if (argLabel.isString) { label=[argLabel] } { label=argLabel };
		if (view.notClosed) {
			this.createLabel;
		}
	}
	
	// do you want the label to have a shadow
	labelShadow_{|bool|
		labelShadow=bool;
		this.labelRefresh;
	}
	
	labelRefresh{ labelGUI.do(_.refresh) }
	
	//////// number box ///////////////////////////////////////////////////////////////////

	// assign a func to display the value in the number box, nil will remove the boc
	numberFunc_{|func|
		if (func.isKindOf(Symbol)) {
			numberFunc = MVC_NumberFunc.at(func);
		}{
			numberFunc = func;
		};
		if (numberFunc.notNil) {
			if (colors['numberUp'  ].isNil) {colors['numberUp'  ]=Color.white};
			if (colors['numberDown'].isNil) {colors['numberDown']=Color.black};
		};
		if ((this.notClosed) and: {numberFunc.notNil} and: {numberGUI.isNil}) {
			this.createNumberGUI;
		}{
			// or remove
			if ((this.notClosed) and: {numberFunc.isNil} and: {numberGUI.notNil}) {
				this.removeNumberGUI;
				window.refresh;
			};
		};
	}
	
	// get the number box bounds
	getNumberRect{|string="200.00 db"|
		var textBounds,textWidth,textHeight,unitStr,numberYOS=0;
		textBounds = string.bounds(numberFont);
		// set numberYOS=2 if units have lowercase tail characters ie. gjpqy
		if ((controlSpec.notNil)&&(showUnits)) {
			unitStr=controlSpec.units;
			[$g, $j, $p, $q, $y].do{|char| if (unitStr.includes(char)) {numberYOS=2}};
		};
		textWidth  = (textBounds.width+numberWidth).asInt;
		textHeight=textBounds.height-2+numberYOS;
				
		if (orientation==\vertical) {
			^Rect((l+(w/2)-(textWidth/2)).asInt,t+h,textWidth,textHeight);
		};
		
		// there is a bug in SCView that refreshes twice when vert bounds don't match
		if (textBounds.height>h) {
			// in this situation the bug can't be avoided because text taller than widget
			if (orientation==\horizontal) {
				^Rect(l+w+1,(t+(h/2)-(textHeight/2)).asInt-1,
					textWidth+1,textBounds.height);
			};
		}{
			// if its smaller we can make them match and avoid the bug
			if (orientation==\horizontal) {
				^Rect(l+w+1,t,textWidth+1,h);
			};
		};
		
	}
	
	// make the number box gui 
	createNumberGUI{
		if (showNumberBox) {
			numberFont = numberFont ?? {SCFont("Helvetica",12)};
			numberBounds=this.getNumberRect;
			numberGUI=SCUserView.new(window,numberBounds.moveBy(0,numberOffset))
				.drawFunc_{|me|
					var active,col;
					
					if (verbose) { [this.class.asString, 'numberFunc' , label].postln };
					if (showLabelBackground) {
						Color.black.alpha_(0.2).set;
						Pen.fillRect(Rect(0,0,numberBounds.width,numberBounds.height));
					};
					active= (midiLearn or: (enabled.not)).not;
					if (active.not) { col= midiLearn.if(\midiLearn,\disabled) };
					Pen.smoothing_(true);	
					numberString=numberFunc.value(value).asString;
					if ((controlSpec.notNil)&&(showUnits)) {
						numberString=numberString++(controlSpec.units)
					};
					Pen.font_(numberFont);
					Pen.fillColor_(colors[active.if(\numberUp,col)]);
					if (numberHeld) {Pen.fillColor_(colors[\numberDown])};
					
					if (orientation==\horizontal) {
						Pen.stringLeftJustIn(numberString,
							Rect(0,0,numberBounds.width,numberBounds.height));
					}{
						Pen.stringCenteredIn(numberString,
							Rect(0,0,numberBounds.width,numberBounds.height));
					};
				}
				.canFocus_(false)
				.visible_(visible)
				.onClose_{ numberGUI=nil };
			this.addNumberControls;
		}
	}
	
	// and remove the number box
	removeNumberGUI{
		if ((numberGUI.notNil) and: {numberGUI.notClosed}) {
			numberGUI.remove;
			numberGUI=nil;
		};
	}
	
	// show the number or not
	showNumberBox_{|bool|
		showNumberBox=bool;
		// alot of testing here
		if (showNumberBox and: {numberFunc.notNil} and: {view.notClosed} and:{numberGUI.isNil}) {
			this.createNumberGUI;
		};
		if (showNumberBox.not) {
			this.removeNumberGUI;
			if (window.isClosed.not) { window.refresh };
		}
	}
	
	// show units in the number box
	showUnits_{|bool|	
		showUnits=bool;
		if (numberGUI.notClosed) { numberGUI.refresh };
	}
	
	// add the controls to the number box
	// this is overridden in the various views,  (there are refs that need to be removed here)
	addNumberControls{
		numberGUI.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			startX=x;
			startY=y;
			if (editMode)			{view.bounds.postln };
			if (controlSpec.notNil) {
				startVal=controlSpec.unmap(value);
			}{
				startVal=value;
			};
			if (hasMIDIcontrol && labelActivatesMIDI) {
				var toggle = false;
				if ((clickCount>1)&&doubleClickLearn){ toggle = true };
				if (modifiers==262401) { toggle = true  };
				if (buttonNumber>=1  ) { toggle = true  };
				if (toggle) { this.toggleMIDIactive };
			};
			if (y>w)				{buttonNumber = 1.5 }; // numbers
			if (modifiers==524576)	{buttonNumber = 1.5 };
			buttonPressed=buttonNumber;	
			numberHeld=true;
			numberGUI.refresh;
		}
		.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			var val;
			if (editMode) {
				this.moveBy(x-startX,y-startY)
			}{
				val=(startVal+((startY-y)/800/(buttonPressed*6+1))).clip(0,1);
				if (controlSpec.notNil) { val=controlSpec.map(val) };
				if (val!=value) {
					this.viewValueAction_(val,nil,true,false);
					this.refreshValue;	
				};
			};
		}
		.mouseUpAction_{
			numberHeld=false;
			numberGUI.refresh;
		}
	}

}

