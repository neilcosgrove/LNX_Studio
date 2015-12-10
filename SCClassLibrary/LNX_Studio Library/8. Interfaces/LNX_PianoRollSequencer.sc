
////////////////////////////////////////////////////////////////////////////////////////////////////
// id creator for notes                                                                         .

LNX_NoteID {
	var     <>id;
	*new    { ^super.new.init }
	init    { this.resetID }
	next    { ^id = id + 1 }
	nextN   {|n| id = id + n; ^id - n + 1 } // used when nonHost user requests new ids
	resetID { id=1000 }
}

// a note ////////////////////////////////////////////////////////////////////////////
// the note can be disabled or its duration can be adjusted, then restablished
// this is useful when editing a score with a pianoroll

LNX_Note{
	
	var <>id, <>note, <>start, >dur, <vel, <>enabled=true, <>durAdj;
	
	*new{|id,note,start,dur,vel| ^super.newCopyArgs(id,note,start,dur,vel) }
	
 	copyToNew{|id| ^LNX_Note(id,note,start,dur,vel) } // make a new note from this one, with new ID
		
	// move this note to..
	move{|argNote,argStart|
		note  = argNote;
		start = argStart;	
	}
	
	// move this note by..
	moveBy{|argNote,argStart|
		note  = note + argNote;
		start = start + argStart;	
	}

	// set the velocity (0-1)
	vel_{|float| vel=float.clip(0,1) }
	
	// the end of the note
	end{ ^start+this.dur }
	
	// set the end
	end_{|value| dur = value - start }
	
	// the end of the note including adjustments
	dur{ if (durAdj.notNil) {^durAdj} {^dur} }
	
	// set the adjusted end point (usally because we have moved notes over this one)
	endAdj_{|value| durAdj= value - start }
	
	// now use these adjustments as the actual duration (done after editing)
	useAdjustments{
		if (durAdj.notNil){
			dur=durAdj;
			durAdj=nil;
		}
	}
	
	// clear the adjustments
	resetAdjustments{
		durAdj  = nil;
		enabled = true;
	}

	// return a Rect that represents the note
	// with time in beats on x axis and Pitch in MIDI Notes on y axis
	notesSeqRect{ ^Rect(start,note,this.dur,0) }
	
	// as above but the with the original duration
	originalNotesSeqRect{ ^Rect(start,note,dur,0) }
	
	// various ways get notes args
	storeArgs{ ^[id,note,start,this.dur,vel] }
	storeArgsPlus{ ^[id,note,start,this.dur,vel,enabled.if(1,0)] } // for networking
	storeArgsMinus{ ^[note,start,this.dur,vel] } // for saving
	
	// update this note with it new values
	updateArgs{|argNote,argStart,argDur,argVel|
		note  = argNote;
		start = argStart;
		dur   = argDur;
		vel   = argVel;
	}
	
	// free it
	free{
		id = note = start = dur = vel = durAdj = nil;
		enabled = false;
	}
	
	// and print it
	printOn {|stream| stream << this.class.name << "(" << id << "," << note << "," <<
		start << "," << dur << "," << vel << ")" }

}

// a score ///////////////////////////////////////////////////////////////////////////
// max 132 notes with 6 variables before clumping is used to send adjustments (slower but needed)
// i'm also manually clumping velocity updates @350, ideally will need to develop a clumpedVP

LNX_Score{
	
	var <notes, <notesDict, <adjusted;
	var <>start=0, <>dur=32;
	var <gridW=22, <gridH=15, <visibleOriginX=0, <visibleOriginY=0; // this may not be a good idea
	var <>quantiseStep=1, <>bars=16, <>speed=1;
	
	*new{|dur=32| ^super.new.init(dur) }
	
	init{|argDur|
		dur       = argDur;
		notes     = [];
		notesDict = IdentityDictionary[];
		adjusted  = IdentitySet[];
	}
	
	// will this work if changed before a colaboration is started, (like insts?)
	// we need to reset if possible, THIS HAS BEEN DONE NOW
	
	getSaveList{|plusIDs=false| // slightly different for presets because of note ID
		var l=["LNX Score DOC"+"v"++1.4, notes.size, start, dur,
					gridW, gridH, visibleOriginX, visibleOriginY, quantiseStep, bars, speed];
		if (plusIDs) { 
			notes.do{|note| l=l++(note.storeArgs)} // for presets
		}{
			notes.do{|note| l=l++(note.storeArgsMinus)} // for saving
		};	
		l=l++(["*** END Score DOC ***"]);
		^l;
	}
	
	// put save list back in
	putLoadList{|l,noteIDObject,plusIDs=false|
		var noNotes;
		var header, loadVersion;		
		header=l[0];
		loadVersion=header.version;
		if ((header.documentType=="LNX Score DOC")) {
			if (loadVersion>=1.3)	{
				
				// clear everything
				notes     = [];
				notesDict = IdentityDictionary[];
				adjusted  = IdentitySet[];
				
				// the properties
				noNotes=l[1].asInt;
				start=l[2].asInt;   // i'm not using this yet
				dur=l[3].asInt;
				gridW=l[4].asFloat;
				gridH=l[5].asFloat;
				visibleOriginX=l[6].asFloat;
				visibleOriginY=l[7].asFloat;
				quantiseStep=l[8].asInt;
				bars=l[9].asInt;
				if (loadVersion>=1.4)	{
					speed=l[10].asFloat;
					l=l.drop(11);
				}{
					l=l.drop(10);	
				};
				
				// and the notes (IDs included depend upon the plusIDs flag)
				if (plusIDs) { 
					l=l.clump(5);
					l.do{|loadList|
						this.addNote(*loadList.collect(_.asFloat));
					};
				}{
					l=l.clump(4);
					l.do{|loadList|
						this.addNote(noteIDObject.next,*loadList.collect(_.asFloat));
					};
				};
					
				this.sort;
	
			}
		}
	}
			
	// only used when syncing collaborations. Host could have different ids so they are all reset
	// to start from 1001
	resetAllNoteIDs{
		var id=1000;
		notes.do{|note|
			notesDict[note.id]=nil;
			id=id+1;
			note.id_(id);
			notesDict[id] = note;
		};
	}	
	
	// the view args for the piano roll are stored here
	// its easier to store the last view with the score if a bit lazy
	viewArgs{^[gridW, gridH, visibleOriginX, visibleOriginY, quantiseStep, bars, speed]}
	
	viewArgs_{|w,h,x,y|
		gridW=w;
		gridH=h;
		visibleOriginX=x;
		visibleOriginY=y;
	}
	
	// the end beat
	end{^start+dur}
	
	// get a note from it's id
	note{|id| ^notesDict[id] }
	
	// i was going to sort the notes but i might not do this now
	sort{} // i haven't use sort so far
	// sort{ notes.sort{|a,b| a.start<=b.start}} // this can slow things down and isn't used much
	
	// clear the score
	clear{
		notes     = [];
		notesDict = IdentityDictionary[];
		adjusted  = IdentitySet[];
	}
	
	// free this object
	free{
		notesDict.do(_.free);
		notes = notesDict = adjusted = nil;
	}
	
	// add an note with an ID
	addNote{|id,note,start,dur,vel|
		id = id.asInt;
		note  = LNX_Note(id,note,start,dur,vel);
		notes = notes.add(note);
		notesDict[id] = note;
	}
	
	// this is called from mouseMove and makes a copy of the notes when availble and mouse moved
	makeCopy{|copyIDs,id,n|
		copyIDs.do{|nid,i|
			var note=notesDict[nid];
			if (note.notNil) { this.addNote(id+i,note.note,note.start,note.dur,note.vel)}
		}
		
	}
	
	// delete a note with an ID
	deleteNote{|id|
		var note = this.note(id);
		notes.removeEvery([note]);
		notesDict[id]=nil;
		note.free;
	}
	
	// host only
	hostMoveNote{|id,argNote,start|
		var note = this.note(id);
		note.move(argNote,start);
		adjusted=adjusted.add(id);
	}
	
	// host only
	hostMoveDur{|id,argDur|
		this.note(id).dur_(argDur);
		adjusted=adjusted.add(id);	
	}
	
	// host only
	hostQuantNote{|id,start,dur|
		var note = this.note(id);
		note.start_(start);
		note.dur_(dur);
		adjusted=adjusted.add(id);
	}
	
	// reset all the adjustments made
	resetAdjustments{
		notes.do(_.resetAdjustments);
	}
	
	// work out adjustments ... has to been done after all moves otherwise will affect notes
	deleteAndClip{|ids| // of notes moved 
		// work out adjustments
		var notesSeqRects = ids.collect{|id| notesDict[id].notesSeqRect};
		
		notesDict.pairsDo{|id,note| 		// of notes been adjusted
			var i, noteRect;
			var start   = note.start;
			var pitch   = note.note;
			var enabled = true;
			
			if (ids.includes(id).not) {
				
				i=0;
				// test start for disabled
				while ({(i<(notesSeqRects.size))&&enabled}, {
					
					// disable / delete if start is under selected notes
					if (notesSeqRects[i].containsPointPR(start@pitch)) {
						note.enabled_(false);
						adjusted=adjusted.add(id);
						enabled=false;
					};
					
					// test intersects for duration clipping
					if (enabled) {
						noteRect = note.originalNotesSeqRect;
						if (noteRect.containsPointPR(notesSeqRects[i].origin)) {
							note.endAdj_(notesSeqRects[i].left);
							adjusted=adjusted.add(id);
						};
					};
					
					i=i+1; // iterate
				});		
				
			};	
			
		};
	}
	
	// work out adjustments ... has to been done after all moves otherwise will affect notes
	quantiseDeleteAndClip{|ids| // of notes moved 
		// work out adjustments
		var notesSeqRects = ids.collect{|id| notesDict[id].notesSeqRect};
		var notesSeqIDS = ids.collect{|id| id };
		
		notesDict.pairsDo{|id,note| 		// of notes been adjusted
			var i, noteRect;
			var start   = note.start;
			var pitch   = note.note;
			var enabled = true;
				
			i=0;
			
			// test start for disabled
			while ({(i<(notesSeqRects.size))&&enabled}, {
				
				// not the same note and STILL enabled
				if ((notesSeqIDS[i]!=id)and:{notesDict[notesSeqIDS[i]].enabled}) {
						
					// disable / delete if start is under selected notes
					if (notesSeqRects[i].containsPointPR(start@pitch)) {
						note.enabled_(false);
						adjusted=adjusted.add(id);
						enabled=false;
					};
					
					// test intersects for duration clipping
					if (enabled) {
						noteRect = note.originalNotesSeqRect;
						if (noteRect.containsPointPR(notesSeqRects[i].origin)) {
							note.endAdj_(notesSeqRects[i].left);
							adjusted=adjusted.add(id);
						};
					};
					
				};
				
				i=i+1; // iterate
			});		
			
		};
	}
	
	// net adjustments (a list is supplied with all the updates) /////////////////////////////
	netAdjust{|adjustList|
		//var deleted=[];
		adjustList.do{|l|
			var id=l[0];
			l=l.drop(1);
			if (l.last.isTrue) {                    // if its enabled
				l=l.drop(-1);				
				if (notesDict[id].notNil) {        // and exists
					notesDict[id].updateArgs(*l); // update it
				}{
					this.addNote(id,*l);          // else create it
				};
			}{	
				this.deleteNote(id);               // or it needs deleting
				//deleted=deleted.add(id);
			}
		};
		//^deleted
	}
	
	// return a store list of all noteIDs that have been adjusted
	adjustedList{
		^adjusted.collect{|id| 
			if (notesDict[id].notNil) {
				notesDict[id].storeArgsPlus
			}{
				[];	
			};
		}.asList.flat
	}
	
	// use the adjustments I have made
	useAdjustments{
		notesDict.pairsDo{|id,note|
			if (note.enabled.not) {
				this.deleteNote(id);
			}{
				note.useAdjustments;		
			}
		};
		adjusted.clear;
	}
	
}

// a piano roll sequencer ///////////////////////////////////////////////////////////////
/*

LNX_PianoRollSequencer.allPianoRolls;
a.a.sequencer.score.notes

*/

LNX_PianoRollSequencer{
	
	classvar <clipboard, >studio, allPianoRolls;
	
	var <id, <score, <api, <noteIDObject;
	var <>snapToGrid=true, <quantiseStep=1, <bars=16;
	var <gui, <gui2, <colors, <notesSelected, <velSelected;
	var <gridW=22, <gridH=15, <noteRects;
	var <lastDuration=2, <lastVelocity;
	var <scores, <initialSize=64;
	var <>action, <>offAction, <>releaseAllAction, <>keyDownAction, <>keyUpAction;
	var <>pipeOutAction;
	var <>recordFocusAction;
	var <>notesOff;
	var <pos=0, lastPos, drawPos=false, lastVisibleRect, mouseMode;
	var <models;
	var <isRecording=false, <recordNotes, <>isPlaying=false, <absTime=1;
	var <lastBeat=0, <lastTime=0, <lastLatency=0;
	var <requestID, <requestN, copyIDs;
	var <marker=0;
	var <>spoModel;
	
	*initClass{ allPianoRolls = [] } // all pRolls kept in allPianoRolls

	*new{|id| ^super.new.init(id) }
	
	init{|argID|
		id     = argID;
		api    = LNX_API.newTemp(this,id,#[\hostAddNote,\netAddNote,\hostDeleteNote,\netDeleteNote,
								\netAdjustList, \netDur_, \netAdjustVel, \returnRequestID,
								\netRequestIDs, \updateIDs, \hostUseAdjustments,
								\hostDeleteNotes, \netSpeed_, \netClear ]);
		score         = LNX_Score(initialSize);
		gui           = IdentityDictionary[];
		colors        = IdentityDictionary[];
		noteIDObject  = LNX_NoteID.new;
		notesSelected = IdentitySet[];
		recordNotes   = IdentityDictionary[];	
		notesOff      = IdentityDictionary[];
		models        = IdentityDictionary[];
		
		allPianoRolls = allPianoRolls.add(this);
		
		lastVelocity=100/127;
		scores=[];

		// the duration of the score
		models[\dur] = [initialSize,[1,1024*8,\lin,1]].asModel.action_{|me,value|
			this.dur_(value);		
			api.sendVP(\dur,\netDur_,value);
		};
		
		// model for the width of 1 beat in pixels (used as horizontal a zoom)
		models[\gridW] = [gridW,[0,40,\lin,0]].asModel.action_{|me,value|
			var lastMidX, vo;
			// find center y-pos before we zoom
			if (gui[\scrollView].notNil) {
				vo = gui[\scrollView].visibleOrigin;
				lastMidX=(vo.x)/gridW;
			};
			// change zoom
			if (gui[\scrollView].notNil) {
				value=value.clip((gui[\scrollView].bounds.width)/(score.dur),inf);
			};
			this.gridW_(value);
			me.value_(value);
			// re-align center y-pos
			if (gui[\scrollView].notNil) {
				gui[\scrollView].visibleOrigin_((lastMidX*gridW)@(vo.y));
			};
		};
		
		// model for the height of 1 note in pixels (used as vertical a zoom)
		models[\gridH] = [gridH,[2,30,\lin,1]].asModel.action_{|me,value|
			var lastMidY, vo;
			// find center y-pos before we zoom
			if (gui[\scrollView].notNil) {
				vo = gui[\scrollView].visibleOrigin;
				lastMidY=(vo.y + (gui[\scrollView].bounds.height/2))/gridH;
			};
			// change zoom
			if (gui[\scrollView].notNil) {
				value=value.clip((gui[\scrollView].bounds.height)/128,inf);
			};
			this.gridH_(value);
			me.value_(value);
			// re-align center y-pos
			if (gui[\scrollView].notNil) {
				gui[\scrollView].visibleOrigin_((vo.x)@
					(lastMidY*gridH-(gui[\scrollView].bounds.height/2)));
			};
		};
		
		// model for record incoming 
		models[\record] = [0,\switch].asModel.action_{|me,value|
			value = value.isTrue;
			if (isRecording!=value) {
				isRecording=value;
				recordNotes.clear
			};
		};
		
		// model for the quantise step
		models[\quantiseStep] = [1, [1,16,\lin,1],
			{|me,val,latency,send|
				quantiseStep=val;
				score.quantiseStep_(val);
				this.refresh(false,false);
				
			}].asModel.doAction;
			
		// model for the quantise step
		models[\bars] = [16, [2,32,\lin,1],
			{|me,val,latency,send|
				bars=val;
				score.bars_(val);
				this.refresh(false,false);
				
			}].asModel.doAction;
			
		// model for speed
		models[\speed] = [3,[0,7,\lin,1], (\items_:["Ö8","Ö4","Ö2"," - ","x2","x4","x8"]),
			{|me,value|
				score.speed_(#[8,4,2,1,0.5,0.25,0.125][value]);
				api.sendVP(\speed,\netSpeed_,value);
				releaseAllAction.value;	
			}].asModel;
			
	}
	
	// only used when syncing collaborations. Host could have different ids so they are all reset
	// to start from 1001
	*resetAllNoteIDs{ allPianoRolls.do(_.resetAllNoteIDs) } // for class
	
	// above for instance
	resetAllNoteIDs{
		score.resetAllNoteIDs;
		scores.do(_.resetAllNoteIDs);
		this.calcNoteRects;
	}
	
	// zoom out so seq fits to window
	fitToWindow{
		models[\gridW].multipyValueAction_(0);
		models[\gridH].multipyValueAction_(0);
	}

	// default view setting on new
	resizeToWindow{
		models[\gridW].multipyValueAction_(0);
		models[\gridH].valueAction_(15);
		
		if (gui[\scrollView].notNil) {
			gui[\scrollView].visibleOrigin_((gui[\scrollView].visibleOrigin.x)@
				((gridH*68)));
				//(lastMidY*gridH-(gui[\scrollView].bounds.height/2)));
		};

	}

	// change the duration of the score & update gui
	dur_{|beats|
		var gridw;
		if (this.dur!=beats) {	
			releaseAllAction.value;				
			score.dur_(beats);
			gridw=gridW;
			if (gui[\scrollView].notNil) {
				gridw=(gui[\scrollView].bounds.width)/(score.dur);
			};
			this.gridW_(gridw); // this will redreaw everything
			models[\gridW].value_(gridw);
		}
	}

	// net version of dur_
	netDur_{|beats|
		var gridw;
		if (this.dur!=beats) {	
			releaseAllAction.value;				
			score.dur_(beats);
			gridw=gridW;
			{
				if (gui[\scrollView].notNil) {
					gridw=(gui[\scrollView].bounds.width)/(score.dur);
				};
				this.gridW_(gridw); // this will redreaw everything
				models[\gridW].value_(gridw);
				models[\dur].value_(beats); // also updates model
			}.defer;
		}
	}
	
	// duration of the current score		
	dur{^score.dur}
	
	// net change the speed
	netSpeed_{|value|
		score.speed_(#[8,4,2,1,0.5,0.25,0.125][value]);
		{models[\speed].value_(value)}.defer;
		releaseAllAction.value;	
	}
	
	// change the grid height
	gridH_{|pixels|
		gridH=pixels;
		this.calcNoteRects;
		this.refreshSeqBounds;
	}
	
	// change the frid width
	gridW_{|pixels|
		gridW=pixels; 
		this.calcNoteRects;
		this.refreshSeqBounds;
		if (gui[\vel].notNil) { gui[\vel].refresh };
	}
		
	// resize the bounds of the note view
	refreshSeqBounds{
		if (gui[\notes].notNil) {
			lastVisibleRect=nil;
			gui[\notes].bounds_(Rect(0,0,gridW*(score.dur),gridH*128));
			gui[\notes].refresh;
			
			gui[\notesPosAndMouse].bounds_(Rect(0,0,gridW*(score.dur),gridH*128));
		};
	}
	
	// Presets /////////////////////////////////////////////////////////////////////////////////
	// this use a different version of getSaveList
	
	// get the current state as a list
	iGetPresetList{ ^score.getSaveList(true) }

	// add a statelist to the presets
	// clear selectedRect
	iAddPresetList{|l|
		l=l.popEND("*** END Score DOC ***");
		scores=scores.add( LNX_Score(initialSize).putLoadList(l,nil,true) )
	}
	
	// save a state list over a current preset
	iSavePresetList{|i,l|
		l=l.popEND("*** END Score DOC ***");
		scores[i].putLoadList(l,nil,true);
	}
	
	// for your own load preset
	iLoadPreset{|i|
		score.free;
		score = scores[i].deepCopy;
		this.clearGUIEditing;
		models[\dur].value_(score.dur); // update the dur model
		
		models[\speed].valueAction_( 6-(log(score.speed*8)/log(2)) );
			// coverts [8,4,2,1,0.5,0.25,0.125] to (0..6) 
		{
		this.viewArgs_(*score.viewArgs); // update view position
		this.refresh;
		}.defer;
	}
	
	// for your own remove preset
	iRemovePreset{|i|
		scores[i].free;
		scores.removeAt(i);
	}
	
	// for your own removal of all presets
	iRemoveAllPresets{ 
		scores.do(_.free);
		scores=[];
	}
	
	// incase someone else is editing the score then stop them
	clearGUIEditing{
		notesSelected.clear;
		mouseMode=nil;
	}
	
	clear{ api.groupCmdOD(\netClear) }
	
	netClear{
		//"net clear".postln;
		score.clear;
		this.refresh;	
	}
		
	// DISK I/O /////////////////////////////////////////////////////////////////////////////////
	
	//  a full save
	getSaveList{
		var l=["LNX PRSeq DOC"+"v"++1.2,scores.size];
		l=l++(score.getSaveList);
		scores.do{|score| l=l++(score.getSaveList) };
		l=l++(["*** END PRSeq DOC ***"]);
		^l
	}
	
	// a full load
	putLoadList{|l|
		var header, loadVersion, noScores;		
		l=l.reverse;
		header=l.popS;
		loadVersion=header.version;
		if ((header.documentType=="LNX PRSeq DOC")&&(1.2>=loadVersion)) {
			this.clearGUIEditing; // for safety
			noScores=l.popI;
			if (loadVersion==1.1) {noScores=noScores-1};
			score.free;
			scores.do(_.free);
			score=LNX_Score(initialSize);
			scores={LNX_Score(initialSize)}!noScores;
			noteIDObject.resetID; // we need to reset so all scores have same ids on network
			score.putLoadList(l.popEND("*** END Score DOC ***"),noteIDObject);
			scores.do{|score|
				score.putLoadList(l.popEND("*** END Score DOC ***"),noteIDObject);
			};
			this.viewArgs_(*score.viewArgs); // update view position
			this.refresh; // now redraw everything
			models[\dur].value_(score.dur); // update the dur model
			models[\speed].value_( 6-(log(score.speed*8)/log(2)) ); // and the speed
		};
	}
	
	// set the view args of this pRoll (includes vert & horz zoom + visable origin)
	viewArgs_{|w,h,x,y,q,b|
		gridW=w;
		gridH=h;
		models[\gridW].value_(w);
		models[\gridH].value_(h);
		quantiseStep=q;
		bars=b;
		models[\quantiseStep].value_(q);
		models[\bars].value_(b);
		
		if (gui[\scrollView].notNil) {
			this.refreshSeqBounds;
			gui[\scrollView].visibleOrigin_(x@y);
		};
	}
	
	// get a note from its id
	note{|id| ^score.note(id) }
	
	// free this object
	free{
		allPianoRolls.remove(this);
		score.free;
		scores.do(_.free);
		id = score = api = noteIDObject = snapToGrid = quantiseStep =
		gui = colors = notesSelected = gridW = gridH = noteRects =
		lastDuration = lastVelocity = scores = nil;
	}
	
	freeAutomation{} // there is none, but just incase it gets called
	
	spo{^spoModel.value?12}
	
	// adding deleteing notes ///////////////////////////
	
	// this add is asynchronous via host
	addNote{|note,start,dur,vel|
		api.hostCmdGD(\hostAddNote, note, start, 
			dur ? lastDuration.clip(quantiseStep,inf), vel ? lastVelocity)
	}
	
	// host version
	hostAddNote{|uid,note,start,dur,vel|
		var noteID = noteIDObject.next;
		// add the note
		score.addNote(noteID,note,start,dur,vel);
		api.sendOD(\netAddNote,noteID,note,start,dur,vel);
		// remove any notes that start underneath it
		score.resetAdjustments; 
		score.deleteAndClip([noteID]);
		api.sendClumpedList(\netAdjustList,score.adjustedList);
		score.useAdjustments;
		// redraw the view
		this.refresh;
		score.sort;
	}
	
	// net version
	netAddNote{|noteID,note,start,dur,vel|
		noteID = noteID.asInt;
		score.addNote(noteID,note,start,dur,vel);
		noteIDObject.id_(noteID); // incase we loose the host
		this.refresh;
		score.sort;
	}
	
	// this delete is asynchronous via host
	deleteNotes{|ids|
		api.hostCmdClumpedList(\hostDeleteNotes,ids)
	}
	
	// host of above
	hostDeleteNotes{|ids|
		api.sendClumpedList(\netDeleteNote,ids);
		this.netDeleteNote(ids)
	}

	//net version
	netDeleteNote{|ids|
		ids.do{|id| score.deleteNote(id) };
		this.refresh;
	}
	
	// move notes by x & y
	moveNotesBy{|x,y,notesSelected|
		var moveList=[];
		
		// this gets done 1st, it doesn't remove notes already changed for changed list
		score.resetAdjustments; 
		
		// TO DO: clip movement to seq bounds
		notesSelected.do{|id|
			var note = score.note(id);
			var n,s;
			if (note.notNil) {
				n=note.note+y;
				s=note.start+x;
				moveList=moveList.add(id);
				score.hostMoveNote(id,n,s);
			};	
		};
		
		// adjust other notes to this movement, this just passes the noteIDs that have moved
		score.deleteAndClip(moveList);
		
		this.refresh;
		score.sort;
		api.sendClumpedList(\netAdjustList,score.adjustedList);
		
	}
	
	// net recieve the adjustments list and change the score
	netAdjustList{|list|
		score.netAdjust(list.clump(6));
		score.sort;
		this.refresh;	
	}
		
	// set adjustment (called from mouseUpAction_)
	useAdjustments{
		var adjustedToEveryone = score.adjustedList.copy;
		score.useAdjustments; // do i need to do this?
		score.sort;			// do i need to do this?
		
		api.hostCmdClumpedList(\hostUseAdjustments,adjustedToEveryone);
			// expensive to me but works
	}
	
	// hosted from above
	hostUseAdjustments{|list|
		api.sendClumpedList(\netAdjustList,list);
		this.netAdjustList(list);
	}
		
	// move notes by x & y
	moveDurBy{|x,notesSelected|
		var moveList=[];
		// this gets done 1st, it doesn't remove notes already changed from changed list
		score.resetAdjustments; 
		// TO DO: clip movement to seq bounds
		notesSelected.do{|id|
			var note = score.note(id);
			var d, maxDistance;
			if (note.notNil) {
				maxDistance = this.findSoonestFrom(notesSelected,id,note.start);
				if (snapToGrid) {
					d=(note.dur+x).clip(
						quantiseStep.clip(0.25,inf).min(maxDistance),maxDistance);
				}{
					d=(note.dur+x).clip(0.25.min(maxDistance),maxDistance);
				};
				moveList=moveList.add(id);
				score.hostMoveDur(id,d);
				lastDuration=d;
			};	
		};
		// adjust other notes to this movement
		score.deleteAndClip(moveList);
		// 
		this.refresh;
		score.sort;
		api.sendClumpedList(\netAdjustList,score.adjustedList);
		
	}
	
	// adjust the velocity of a list of notes
	adjustVel{|value,notesSelected|
		var note, setList=[];
		notesSelected.do{|key|
			note=score.note(key);
			if (note.notNil) {
				note.vel_(note.vel+value);
				setList=setList.add(key).add(note.vel);
			};
		};
		
		if (gui[\vel].notNil) { gui[\vel].refresh };
		if (setList.size>0) {
			// because i don't have a protocol to do this yet
			// i'm clumping at 350
			setList.clump(350).do{|setList,i|
				api.sendVP(\netVel++i, \netAdjustVel, *setList);
			};
		};
	}
	
	// net version of above
	netAdjustVel{|...notesSelected|
		notesSelected=notesSelected.clump(2);
		notesSelected.do{|list|
			var note,key,value;
			#key,value = list;
			note=score.note(key.asInt);
			if (note.notNil) { note.vel_(value) };
		};
		{if (gui[\vel].notNil) { gui[\vel].refresh }}.defer;
	}
	
	// find the time to the next start on this note
	findSoonestFrom{|ids,id,start|
		var distance = inf;
		var note = score.note(id);
		ids.do{|checkID|
			var checkNote;
			if (checkID!=id) {
				checkNote = score.note(checkID);	
				if ((checkNote.note==note.note) 
					and: {checkNote.start>note.start} 
					and: {checkNote.start<distance}) {
						distance=checkNote.start;
				};
			};
			
		};
		^distance-(note.start)
	}	
		
	// quanise notes (most of this can go into score)
	quantise{|notesSelected|
		var moveList=[];
		// this gets done 1st, it doesn't remove notes already changed for changed list
		score.resetAdjustments; 
		// TO DO: clip movement to seq bounds
		notesSelected.do{|id|
			var note = score.note(id);
			var s,d;
			if (note.notNil) {
				s=(note.start).round(quantiseStep).clip(0,score.dur-1);
				d=(note.dur).round(quantiseStep).clip(quantiseStep,inf);
				moveList=moveList.add(id);
				score.hostQuantNote(id,s,d);
			};	
		};
		// adjust other notes to this movement
		score.quantiseDeleteAndClip(moveList);  // do a version of this
		this.useAdjustments;
	}
		
	// MIDI clock in /////////////////////////////////////////////
	
	// clock in 3 is faster than the normal instrument clockIn, which results in fast tempo updates
	clockIn3   {|beat,argAbsTime,latency|	
		var now,then,i,endIndex, speed=score.speed;
		
		
		//[beat,argAbsTime,latency,beat.species,argAbsTime.species,latency.species].postln;
		
		// NB: F is Fast, S is Slow
		isPlaying=true;
		absTime=argAbsTime;  // F is time of 1/3  of normal beat
		// for recording
		lastBeat = beat;     //  F beat x3 = 1 normal beat
		lastTime = SystemClock.now;
		lastLatency = latency;
		// the current beat wrapped to score duration
		beat = beat % (score.dur*3*speed);  // F
		// now and the next beat
		now = beat/3;       // S
		then = (beat+1)/3;  // S
		
		// stop any notes as needed 1st
		if (notesOff[beat].notNil) {
			notesOff[beat].do{|l|
				{this.seqNoteOff(l[1],latency); nil;}.sched(l[0]*absTime*3)
			};
			notesOff[beat]=nil;	
		};
		// go through the score
		score.notes.do{|note|
			var pitch = note.note;	
			// if the note starts between now and the next midi tick
			if (((note.start*speed)>=now)&&((note.start*speed)<then)) {
				{
					if (note.enabled) {
						
						this.seqNoteOn(pitch,note.vel,latency); // play it
						
						// clip end to shortest time allowed
						if ((note.end*speed)<then) {
							{this.seqNoteOff(pitch,latency); nil;}.sched(
								(note.dur*3*speed)*(absTime*3))
						}{
							endIndex = (note.end*3*speed).asInt%(score.dur*3*speed);
							// this will add to nil
							notesOff[endIndex]=notesOff[endIndex].add(
								[(note.end*3*speed).frac,pitch]);
						};
					};
					nil;
				}.sched( ((note.start*3*speed)-beat)*(absTime*3)*0.99999);
				//sched it to the correct time
			}
		};
		// update the gui pos
		
		{this.pos_(now/speed)}.defer(latency);
		
	}
	
	releaseAll{ notesOff=IdentityDictionary[] } // no actual release, done via buffer
	
	seqNoteOn{|note,velocity,latency|
		action.value(note,velocity,latency); // play it
		pipeOutAction.value( LNX_NoteOn(note,velocity*127,latency,\sequencer).addToHistory(this) );
	}
		
	// do the note off action (i could make this redundant and put directly in methods)
	seqNoteOff{|note,latency|
		offAction.value(note,0,latency);
		pipeOutAction.value( LNX_NoteOff(note,0,latency,\sequencer).addToHistory(this) );
	}
	
	// clock stop messages should come here
	clockStop {|latency|
		this.releaseAll;
		{this.pos_(0)}.defer(latency);
		this.isRecording_(false);
		isPlaying=false;
	}
	
	// clock pause messages should come here
	clockPause{|latency|
		this.releaseAll;
		this.isRecording_(false);
		isPlaying=false;
	}
	
	// recording /////////////////////////////////////////////
	
	pipeIn{|pipe|
		case
			{pipe.isNoteOn } { this.noteOn (pipe.note, pipe.velocity/127, pipe.latency) }
			{pipe.isNoteOff} { this.noteOff(pipe.note, pipe.velocity/127, pipe.latency) }
	}
	
	
	// set isRecording (needs to be defered becasue of scheduling to clockStop & clockPause
	isRecording_{|bool| {models[\record].valueAction_(bool.if(1,0))}.defer }
	
	// midi note on (for recording)
	noteOn{|note, velocity, latency|	
		var indexPos, actualPos, offset, speed=score.speed;
		if (isRecording && isPlaying) {
			note      = note.asInt;
			latency   = latency ? 0;
			// lastLatency comes from clockIn
			// offset is now - last beat time - last latency + this midiOn lataency
			// which gives a time vs audio 
			// then divide by absTime and 3 to convert to sequencer beats
			// so offset is time since last beat in beats
			offset    = (SystemClock.now-lastTime-lastLatency+latency)/absTime/3;
			// indexPos is the total index time (used for start of duration)
			indexPos  = (lastBeat + offset)/3;
			// and actualPos is wrapped to score duration (used for start time)
			actualPos = (((lastBeat + offset)/speed) % (score.dur*3))/3;
			// store it for noteOff
			recordNotes[note] = [ note, velocity, indexPos, actualPos];
		}
	}
	
	// midi note off (for recording)
	noteOff{|note, velocity, latency|
		var indexPos, offset, speed=score.speed;
		var start, dur, vel;
		if (isRecording && isPlaying) {
			note = note.asInt;
			if (recordNotes[note].notNil) {
				latency   = latency ? 0;
				// so offset is time since last beat in beats
				offset    = (SystemClock.now-lastTime-lastLatency+latency)/absTime/3;
				// indexPos is the total index time (used for end of duration) 
				indexPos  = (lastBeat + offset)/3;
				// now add to the sequence
				start = recordNotes[note][3];
				dur   = indexPos - recordNotes[note][2]/speed;
				vel   = recordNotes[note][1];
				this.addNote(note,start,dur,vel); // add to notesSelected
				recordNotes[note]=nil;
			};
		}
	}
	
	// request n noteIDs from host
	requestIDs{|n|
		requestID = requestN = nil;
		api.hostCmdGD(\netRequestIDs,n);
	}
	
	// the host now supplies the next ids to the user and update others to the lastest id 
	netRequestIDs{|userID,n|
		if (api.isConnected) {
			api.sendToGD(userID,\returnRequestID,noteIDObject.nextN(n),n); // when connected
			api.sendOD(\updateIDs,noteIDObject.id); // update others incase we loose the host
			
		}{
			this.returnRequestID(noteIDObject.nextN(n),n); // when not, this could be better
		}
	}
	
	// store the ids for use (used is mouseMoveAction)
	returnRequestID{|startID,n|
		requestID=startID;
		requestN=n;
	}
	
	// update lastest id incase we loose the host, otherwise new note ids will not match
	updateIDs{|id| noteIDObject.id_(id.asInt) }
	
	// gui call for copy
	guiCopy{
		var removeOffset;
		if (notesSelected.size>0) {
			removeOffset=this.getSelectedAreaRect.left.neg;
			clipboard=[];
			notesSelected.do{|id|
				clipboard=clipboard.add(
					this.note	(id).copyToNew(nil).moveBy(0,removeOffset)
				)
			};
		};
	}
	
	// gui call for paste (inefficient on network but works)
	guiPaste{
		if  (clipboard.notNil) {
			clipboard.do{|note|
				this.addNote(note.note,note.start+marker,note.dur,note.vel)
			};
		}	
	}
	
}		
		
	