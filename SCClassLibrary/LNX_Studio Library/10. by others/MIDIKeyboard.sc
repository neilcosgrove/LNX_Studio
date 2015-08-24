// (c) 2006, Thor Magnusson - www.ixi-software.net
// GNU licence - google it.

MIDIKeyboard {

	var <>keys; 
	var trackKey, chosenkey, mouseTracker;
	var win, bounds, octaves, startnote;
	var downAction, upAction, trackAction;
	var keyCodeMap, keyCodesPressed;
	var <transpose=0, <>miscKeyAction;
	var <>keyboardColor;
	
	*new { arg w, bounds, octaves, startnote; 
		^super.new.initMIDIKeyboard(w, bounds, octaves, startnote);
	}
	
	initMIDIKeyboard { arg w, argbounds, argoctaves=3, argstartnote;
		var r, pix, pen;
		octaves = argoctaves ? 4;
		bounds = argbounds ? Rect(20, 10, 364, 60);
				
		if((win= w).isNil, {
			win = GUI.window.new("MIDI Keyboard",
				Rect(10, 250, bounds.left + bounds.width + 40, bounds.top + bounds.height+30));
			win.front
		});

 		mouseTracker = GUI.userView.new(win, bounds); // thanks ron!
 		bounds = mouseTracker.bounds;

		pen	= GUI.pen;

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
								(bounds.width/octaves/7))).round(1) + ((bounds.width/octaves)*j)).round(1)+0.5,
							bounds.top, 
							bounds.width/octaves/10, 
							bounds.height/1.7);
					keys.add(MIDIKey.new(startnote+i+(j*12), r, Color.black));
				}, {
					r = Rect((bounds.left+(pix[i]*((bounds.width/octaves) -
								(bounds.width/octaves/7))).round(1) + ((bounds.width/octaves)*j)).round(1)+0.5,
							bounds.top, 
							bounds.width/octaves/7, 
							bounds.height);
					keys.add(MIDIKey.new(startnote+i+(j*12), r, Color.white));
				});
			});
		});

		mouseTracker
			.canFocus_(true)
			.focusColor_(Color.red)
			.mouseDownAction_({|me, x, y, mod|
				chosenkey = this.findNote(x, y);
				trackKey = chosenkey;
				chosenkey.color = keyboardColor;
				downAction.value(chosenkey.note);
				this.refresh;	
			})
			.mouseMoveAction_({|me, x, y, mod|
				y=y.clip(0,bounds.height);
				x=x.clip(1,bounds.width);
				chosenkey = this.findNote(x, y);
				if(trackKey.note != chosenkey.note, {
					trackKey.color = trackKey.scalecolor; // was : type
					trackKey = chosenkey;
					chosenkey.color = keyboardColor;
					trackAction.value(chosenkey.note);
					this.refresh;
				});
			})
			.mouseUpAction_({|me, x, y, mod|
				chosenkey = this.findNote(x, y);
				trackKey = chosenkey;
				chosenkey.color = chosenkey.scalecolor; // was:  type
				upAction.value(chosenkey.note);
				this.refresh;
			})
			.drawFunc_({
				octaves.do({arg j;
					// first draw the white keys
					12.do({arg i;
						var key;
						key = keys[i+(j*12)];
						if(key.type == Color.white, {
							pen.color = Color.black;
							pen.strokeRect(Rect(key.rect.left+0.5, key.rect.top+0.5, key.rect.width+0.5, key.rect.height-0.5));
							pen.color = key.color; // white or grey
							pen.fillRect(Rect(key.rect.left+0.5, key.rect.top+0.5, key.rect.width+0.5, key.rect.height-0.5));
						});
					});
					// and then draw the black keys on top of the white
					12.do({arg i;
						var key;
						key = keys[i+(j*12)];
						if(key.type == Color.black, {
							pen.color = key.color;
							pen.fillRect(Rect(key.rect.left+0.5, key.rect.top+0.5, key.rect.width+0.5, key.rect.height+0.5));
						});
					})
				})
			})
			.keyDownAction_{|me,char, modifiers, unicode, keycode|
				var kcm=keyCodeMap.indexOf(keycode);
				
				//[me,char, modifiers, unicode, keycode].postln;
				
				if (keycode==126) {miscKeyAction.value(\up)   ;};
				if (keycode==125) {miscKeyAction.value(\down) ;};
				if (keycode==123) {miscKeyAction.value(\left) ;};
				if (keycode==124) {miscKeyAction.value(\right);};
				if (keycode== 49) {miscKeyAction.value(\space);};
				
				if (keycode== 27) {transpose=(transpose-1).clip(-1,2)};
				if (keycode== 24) {transpose=(transpose+1).clip(-1,2)};
				
				// keyboard note pressed
				if (kcm.isNumber) {									if (keyCodesPressed.includes(keycode).not) { // test for repeat key press
						this.keyOn((kcm+(transpose*12)).clip(-24,127-24)); // play note
						keyCodesPressed=keyCodesPressed.add(keycode); // store for next key test
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
		downAction.value(note);
		
		this.setColor(note,keyboardColor,0.75);
	}
	
	keyOff{|note|
		note=note+24;
		//("Key Off"+(note.asString)).postln;
		upAction.value(note);
		this.removeColor(note);
	}
	
	refresh {
		if (win.isClosed.not) {
			mouseTracker.refresh;
		};
	}
	
	focus{
		mouseTracker.focus;
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
		mouseTracker.remove;
		win.refresh;
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

//MIDIKey {
//	var <rect, <>color, <note, <type;
//	var <>scalecolor, <>inscale;
//	
//	*new { arg note, rect, type; 
//		^super.new.initMIDIKey(note, rect, type);
//	}
//	
//	initMIDIKey {arg argnote, argrect, argtype;
//		note = argnote;
//		rect = argrect;
//		type = argtype;
//		color = argtype;
//		scalecolor = color;
//		inscale = false;
//	}	
//
//}
