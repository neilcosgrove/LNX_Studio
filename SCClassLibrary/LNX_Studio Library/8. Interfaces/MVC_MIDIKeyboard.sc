// (c) 2006, Thor Magnusson - www.ixi-software.net
// GNU licence - google it.

MVC_MIDIKeyboard {

	var <>keys; 
	var trackKey, chosenkey, <view;
	var window, bounds, octaves, startnote;
	var downAction, upAction, trackAction, spaceBarAction;
	var keyCodeMap, keyCodesPressed;
	var <transpose=0, <>miscKeyAction;
	var <>keyboardColor, <resize=1;
	var <>useSelect=false, <>stopGUIUpdate=false;
	
	var <>pipeFunc, <lastKeyboardNote;
	
	// 3 methods below needs adjusting for key presses & tracking at the same time 
	
	outDownAction{|note|
		downAction.value(note);
		lastKeyboardNote=note;
		pipeFunc.value(LNX_NoteOn(note,100,nil,\keyboard));
	}
	
	outUpAction{|note|	
		upAction.value(note);
		pipeFunc.value(LNX_NoteOff(note,1,nil,\keyboard));
	}
	
	outTrackAction{|note|
		trackAction.value(note);
		pipeFunc.value(LNX_NoteOn(note,100,nil,\keyboard));
		pipeFunc.value(LNX_NoteOff(lastKeyboardNote,1,nil,\keyboard));
		lastKeyboardNote=note;
	}
	
	*new { arg window, bounds, octaves, startnote; 
		^super.new.init(window, bounds, octaves, startnote);
	}
	
	// can use to get lost focus
	hasFocus{ if (view.notNil) { ^view.hasFocus } {^nil} }
	
	doResizeAction{ /* to do*/ }
	
	free {
		this.remove;
		view = window = bounds = downAction = upAction =
		trackAction = keyCodeMap = keyCodesPressed = transpose = miscKeyAction =
		keyboardColor = nil;
	}
	
	init { arg argwindow, argbounds, argoctaves=3, argstartnote;
		var r, pix, pen;
		octaves = argoctaves ? 4;
		bounds = argbounds ? Rect(20, 10, 364, 60);
		
		startnote = argstartnote ? 48;
		trackKey = 0;
		//pix = [0, 6, 10, 16, 20, 30, 36, 40, 46, 50, 56, 60];
		pix = [ 0, 0.1, 0.17, 0.27, 0.33, 0.5, 0.6, 0.67, 0.77, 0.83, 0.93, 1 ]; // as above but normalized
		keys = List.new;
		
		keyCodeMap=[6,1,7,2,8,9,5,11,4,45,38,46,12,19,13,20,14,
									15,23,17,22,16,26,32,34,25,31,29,35];
		keyCodesPressed=[];
		
		keyboardColor=Colour(0.5,0.5,0.5);

		octaves.do({arg j;
			12.do({arg i;
				if((i == 1) || (i == 3) || (i == 6) || (i == 8) || (i == 10), {
					r = Rect(	(bounds.left+ (pix[i]*((bounds.width/octaves) -
								(bounds.width/octaves/7))).round(1) +
								((bounds.width/octaves)*j)).round(1)+0.5,
							bounds.top, 
							bounds.width/octaves/10, 
							bounds.height/1.7);
					keys.add(MIDIKey.new(startnote+i+(j*12), r, Color.black));
				}, {
					r = Rect((bounds.left+(pix[i]*((bounds.width/octaves) -
								(bounds.width/octaves/7))).round(1) +
								((bounds.width/octaves)*j)).round(1)+0.5,
							bounds.top, 
							bounds.width/octaves/7, 
							bounds.height);
					keys.add(MIDIKey.new(startnote+i+(j*12), r, Color.white));
				});
			});
		});
		
		window=argwindow;
		
		if (window.isKindOf(MVC_Window) or:{window.isKindOf(MVC_ScrollView) } ) {
			window.addView(this);
		
		}{
			if (window.notNil) {
				// i need to work on tabs here
			
			}{
				window = GUI.window.new("MIDI Keyboard",
					Rect(10, 250, bounds.left + bounds.width + 40, bounds.top + bounds.height+30));
				window.front
		
			};
		};
		
		
		
		
		
	}
	
	resize_{|int| resize=int; if (view.notClosed) {view.resize_(int)} }
	
	// create the gui's items that make this MVC
	create{|argParent|
		if (view.isClosed) {
			if (argParent.notNil) { window = argParent };
			this.createView;
		}{
			"View already exists.".warn;
		}
	}
	
	createView{
	
		var	pen	= GUI.pen;
		
		view = GUI.userView.new(window, bounds); // thanks ron!
 		bounds = view.bounds;
	
		view	.canFocus_(true)
			.focusColor_(Color.clear)
			.mouseDownAction_({|me, x, y, mod|
				chosenkey = this.findNote(x, y);
				trackKey = chosenkey;
				if (chosenkey.notNil) {
					if (stopGUIUpdate.not) {
						if (useSelect) {
							chosenkey.selectColor_(keyboardColor);
						}{
							chosenkey.color = keyboardColor;
						};
					};
					this.outDownAction(chosenkey.note);
					if (stopGUIUpdate.not) {this.refresh};
				}
			})
			.mouseMoveAction_({|me, x, y, mod|
				y=y.clip(0,bounds.height);
				x=x.clip(1,bounds.width);
				chosenkey = this.findNote(x, y);
				if (chosenkey.notNil) {
					if(trackKey.note != chosenkey.note, {
						if (useSelect) {
							if (stopGUIUpdate.not) {trackKey.selectColor_(nil)};
							trackKey = chosenkey;
							if (stopGUIUpdate.not) {chosenkey.selectColor_(keyboardColor)};
						}{
							if (stopGUIUpdate.not) {trackKey.color = trackKey.scalecolor};
							trackKey = chosenkey;
							if (stopGUIUpdate.not) {chosenkey.color = keyboardColor};
						};
						this.outTrackAction(chosenkey.note);
						if (stopGUIUpdate.not) {this.refresh};
					});
				};
			})
			.mouseUpAction_({|me, x, y, mod|
				chosenkey = this.findNote(x, y);
				trackKey = chosenkey;
				if (chosenkey.notNil) {
					if (stopGUIUpdate.not) {
						if (useSelect) {
							chosenkey.selectColor_(nil);
						}{
							chosenkey.color = chosenkey.scalecolor; // was:  type
						};
					};
					this.outUpAction(chosenkey.note);
					if (stopGUIUpdate.not) {this.refresh};
				}
			})
			.drawFunc_({
				
				var l,t,w,h,b,m;
				
				octaves.do({arg j;
					// first draw the white keys
					12.do({arg i;
						var key;				
						key = keys[i+(j*12)];
						if(key.type == Color.white, {
							
							l = key.rect.left;
							t = key.rect.top;
							w = key.rect.width;
							h = key.rect.height;
							b = key.rect.bottom;
							
							pen.color = Color.black;
							pen.strokeRect(Rect(l+0.5, t+0.5, w+0.5, h-0.5));
							pen.color = key.color; // white or grey
							
							pen.fillRect(Rect(l+0.5, t+0.5, w+0.5, h-0.5));
									
							// finger circle highlight
							if (key.selectColor.notNil) {
								pen.color = key.selectColor; // white or grey
								pen.fillOval( Rect(l+1.5, b-1.5-w, w-3, w-3) );
							};
											
						});
					});
					// and then draw the black keys on top of the white
					12.do({arg i;
						var key;
						key = keys[i+(j*12)];
						
						if(key.type == Color.black, {
							
							l = key.rect.left;
							t = key.rect.top;
							w = key.rect.width;
							h = key.rect.height;
							b = key.rect.bottom;
							
							pen.color = key.color;
							pen.fillRect(Rect(l+0.5, t+0.5, w+0.5, h+0.5));
																			if (key.selectColor.notNil) {
								pen.color = key.selectColor; // white or grey
								pen.fillOval(Rect(l+0.5, b-0.5-w-2, w+1, w+1));
							};
							
						});
					});
					
					/*
					(30..50).do{|n| a.a.gui[\keyboard].setStoreColor(n,Color(1,0,0,0.66))};
					(30..50).do{|n| a.a.gui[\keyboard].setStoreActive(n,0.5.coin)};
					*/
										
					12.do({arg i;
						var key, os;				
						key = keys[i+(j*12)];

						os=#[-1,0,0,0,1,-1,0,0,0,0,0,1][(i+(j*12))%12];

						// triangle at top of key (storeColor, storeActive)
						if (key.storeColor.notNil) {
							l = key.rect.left;
							t = key.rect.top;
							w = key.rect.width;
							h = key.rect.height;
							b = key.rect.bottom;
							l=l+(os*w/7);
							m = w/2+l;
							Pen.fillColor = key.storeColor; // white or grey
							Pen.moveTo((m-5)@t);
							Pen.lineTo((m)@( key.storeActive.if(16.6,10)+t ));
							Pen.lineTo((m+5)@t);
							Pen.lineTo((m-5)@t);
							Pen.fill;		
						};
					});
				})
			})
			.keyDownAction_{|me,char, modifiers, unicode, keycode|
			
				var kcm=keyCodeMap.indexOf(keycode);
				
				if (modifiers.asBinaryDigits[4]==0) {  // no apple modifier
				
					if (keycode==126) {miscKeyAction.value(\up)   ;};
					if (keycode==125) {miscKeyAction.value(\down) ;};
					if (keycode==123) {miscKeyAction.value(\left) ;};
					if (keycode==124) {miscKeyAction.value(\right);};
					if (keycode== 49) {
							miscKeyAction.value(\space);
							spaceBarAction.value;
						};
					
					if (keycode== 27) {transpose=(transpose-1).clip(-2,5)};
					if (keycode== 24) {transpose=(transpose+1).clip(-2,5)};
					
					// keyboard note pressed
					if (kcm.isNumber) {									if (keyCodesPressed.includes(keycode).not) { // test for repeat key press
							this.keyOn((kcm+(transpose*12)).clip(-24,127-24)); // play note
							keyCodesPressed=keyCodesPressed.add(keycode); // store for next key test
						}
					}
	
				}
			}
			.keyUpAction = { |me,char,modifiers,unicode,keycode|
				var kcm=keyCodeMap.indexOf(keycode);
				if (kcm.isNumber) {	
					this.keyOff((kcm+(transpose*12)).clip(-24,127-24)); // note off
					keyCodesPressed.remove(keycode); // remove from repeat key list
				}	
			};
			
	
	}
	
	keyOn{|note|
		note=note+24;
		//("Key On"+(note.asString)).postln;
		this.outDownAction(note);
		if (stopGUIUpdate.not) {
			if (useSelect) {
				this.setSelectColor(note,keyboardColor,0.75);
			}{
				this.setColor(note,keyboardColor,0.75);
			};
		}
	}
	
	keyOff{|note|
		note=note+24;
		//("Key Off"+(note.asString)).postln;
		this.outUpAction(note);
		if (stopGUIUpdate.not) {
			if (useSelect) {
				this.removeSelectColor(note);
			}{
				this.removeColor(note);
			};
		}
	}
	
	refresh {
		if (view.notClosed) { view.refresh }
	}
	
	focus{
		if (view.notClosed) { view.focus }
	}
	
	keyDown { arg note, color; // midinote
		if(this.inRange(note), {
			keys[note - startnote].color = Color.grey;
		});
		this.refresh;
	}
	
	keyUp { arg note; // midinote
		if(this.inRange(note), {
			keys[note - startnote].color = keys[note - startnote].scalecolor;
		});
		this.refresh;	
	}
	
	spaceBarAction_{|func|
		spaceBarAction=func;
	}
	
	keyDownAction_ { arg func;
		downAction = func;
	}
	
	keyUpAction_ { arg func;
		upAction = func;
	}
	
	keyTrackAction_ { arg func;
		trackAction = func;
	}
	
	showScale {arg argscale, key=startnote, argcolor;
		var color, scale, counter, transp;
		this.clear; // erase scalecolors (make them their type)
		counter = 0;
		color = argcolor ? Color.red;
		transp = key%12;
		scale = argscale + transp + startnote;		
		keys.do({arg key, i;
			key.color = key.type; // back to original color
			if(((i-transp)%12 == 0)&&((i-transp)!=0), { counter = 0; scale = scale+12;});			if(key.note == scale[counter], {
				counter = counter + 1; 
				key.color = key.color.blend(color, 0.5);
				key.scalecolor = key.color;
				key.inscale = true;
			});
		});
		this.refresh;
	}
	
	clear {
		keys.do({arg key, i;
			key.color = key.type; // back to original color
			key.scalecolor = key.color;
			key.inscale = false;
			key.selectColor_(nil);
		});
		this.refresh;
	}
	
	// just for fun
	playScale { arg argscale, key=startnote, int=0.5;
		var scale = argscale;
		SynthDef(\midikeyboardsine, {arg freq, amp = 0.25;
			Out.ar(0, (SinOsc.ar(freq,0,amp)*EnvGen.ar(Env.perc, doneAction:2)).dup)
		}).load(Server.default);
		Task({
			scale.mirror.do({arg note;
				Synth(\midikeyboardsine, [\freq, (key+note).midicps]);
				int.wait;
			});		}).start;
	}
	
	setColor {arg note, color, blend=0.5;
		var newcolor;
		if (this.inRange(note)) {
			newcolor = keys[note - startnote].color.blend(color,blend);
			keys[note - startnote].color = newcolor;
			keys[note - startnote].scalecolor = newcolor;
			this.refresh;
		}
	}

	// additions for lnx //////////
	// select colour //////////////

	pipeIn{|pipe,color,blend=1,select=false|
		{	
			if (select) {
				if (pipe.isNoteOn)  { this.setSelectColor(pipe.note, pipe[\color]?color, blend)};
				if (pipe.isNoteOff) { this.removeSelectColor(pipe.note)};
			}{
				if (pipe.isNoteOn)  { this.setColor(pipe.note, pipe[\color] ? color, blend)};
				if (pipe.isNoteOff) { this.removeColor(pipe.note) };
			};
		}.defer(pipe.latency?0);
	}

	setSelectColor {|note, color|
		if (this.inRange(note)) {
			keys[note - startnote].selectColor_(color);
			this.refresh;
		}
	}
	
	removeSelectColor {|note|
		if (this.inRange(note)) {
			keys[note - startnote].selectColor_(nil);
			this.refresh;
		}
	}
	
	// the triangle at the top <>storeColor
	
	setStoreColor {|note, color|
		if (this.inRange(note)) {
			keys[note - startnote].storeColor_(color);
			this.refresh;
		}
	}
	
	setStoreColorNoRefresh {|note, color|
		if (this.inRange(note)) {
			keys[note - startnote].storeColor_(color);
		}
	}
	
	removeStoreColor {|note|
		if (this.inRange(note)) {
			keys[note - startnote].storeColor_(nil);
			this.refresh;
		}
	}

	clearAllStoreColors{
		keys.do(_.storeColor_(nil));
		this.refresh;
	}	

	clearAllStoreColorsNoRefresh{
		keys.do(_.storeColor_(nil));
	}	

	// if the triangle is active ie bigger <>storeActive=false
	
	setStoreActive {|note,bool=true|
		if (this.inRange(note)) {
			keys[note - startnote].storeActive_(bool);
			this.refresh;
		}
	}
	
	removeStoreActive {|note|
		if (this.inRange(note)) {
			keys[note - startnote].storeActive_(false);
			this.refresh;
		}
	}

	clearAllStoreActive {
		keys.do(_.storeActive_(false));
		this.refresh;
	}	

	//////////////////////////////
	
	getColor { arg note;
		if (this.inRange(note)) {
			^keys[note - startnote].color;
		}{
			^nil
		}
	}
	
	
	getType { arg note;
		^keys[note - startnote].type;
	}
	
	removeColor {arg note;
		if (this.inRange(note)) {
			keys[note - startnote].scalecolor = keys[note - startnote].type;
			keys[note - startnote].color = keys[note - startnote].type;
			this.refresh;
		}
	}
	
	inScale {arg key;
		^keys[key-startnote].inscale;
	}
	
	retNote {arg key;
		^keys[key].note;
	}
	
	remove {
		if (view.notClosed) {
			view.remove;
			window.refresh;
		}
	}
	
	// local function
	findNote {arg x, y;
		var chosenkeys;
		chosenkeys = [];
		keys.reverse.do({arg key;
			if(key.rect.containsPoint(Point.new(x,y)), {
				chosenkeys = chosenkeys.add(key);
			});
		});
		block{|break|
			chosenkeys.do({arg key;
				if(key.type == Color.black, { 
					chosenkey = key; 
					break.value; // the important part
				}, {
					chosenkey = key; 
				});
			});
		};
		^chosenkey;
	}
	
	// local
	inRange {arg note; // check if an input note is in the range of the keyboard
		if (note.isNumber.not) {^false};
		if((note>=startnote) && (note<(startnote + (octaves*12))), {^true}, {^false});
	}
	


}

MIDIKey {
	var <rect, <>color, <note, <type;
	var <>scalecolor, <>inscale, <>selectColor, <>storeColor, <>storeActive=false;
	
	*new { arg note, rect, type; ^super.new.initMIDIKey(note, rect, type) }
	
	initMIDIKey {arg argnote, argrect, argtype;
		note = argnote;
		rect = argrect;
		type = argtype;
		color = argtype;
		scalecolor = color;
		inscale = false;
	}	

}
