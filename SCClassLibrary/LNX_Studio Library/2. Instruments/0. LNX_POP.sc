
////////////////////////////////////////////////////////////////////////////////////////        .
//                                                                                    //
// LNX_POP Presets of presets (The prog tab in the mixer - selecting inst presets)    //
//                                                                                    //
////////////////////////////////////////////////////////////////////////////////////////
/*
//pretty
(
a=0;
{
{
1.0.rand.wait;
LNX_POP.midi.noteOn(127.rand.asInt,((a)%19.rand)+64.rand);
LNX_POP.midi.noteOn(127.rand.asInt,0);
LNX_POP.midi.noteOn(127.rand.asInt,0);
LNX_POP.midi.noteOn(127.rand.asInt,0);
LNX_POP.midi.noteOn(127.rand.asInt,0);
a=a-1;
}.loop;
}.fork
)
*/

LNX_POP {

	classvar classAPI;
	classvar <listOfPofP, <studioModels, <resets, <padNotes, <clipboard;
	classvar <gui, <midi, <>studio, <when, <noPOP=16, <maxPOP=128, lastNote, lastPad;
	
	var <api, <inst, <>presetsOfPresets, <instGUI, <items;
		
	*initClass{
		Class.initClassTree(LNX_API);
		classAPI = LNX_API.newPermanent(this, \LNX_POP,
			#[\netPopReset, \more, \less, \netSetNow, \netSetWhen,
				\netPaste, \netInsert, \netClear, \netRemove ], #[]); // network interface
	}
	
	*initMIDI{
		
		var pref;
		
		// note value of pads left to right, top to bottom
		padNotes =  #[ 0, 1, 2, 3, 4, 5, 6, 7, 16, 17, 18, 19, 20, 21, 22, 23,
			32, 33, 34, 35, 36, 37, 38, 39, 48, 49, 50, 51, 52, 53, 54, 55,
			64, 65, 66, 67, 68, 69, 70, 71, 80, 81, 82, 83, 84, 85, 86, 87,
			96, 97, 98, 99, 100, 101, 102, 103, 112, 113, 114, 115, 116, 117, 118, 119 ];
		
		midi = LNX_MIDIPatch(0,0,0,0);	
		
		// load preferences or find a midi controller with "pad" in the name
		pref = "POP Controller".loadPref;
		if (pref.isNil) { midi.findByName("pad","pad") }{ midi.putLoadList(pref.asInt) };
		
		midi.noteOnFunc  = {|src, chan, note, vel ,latency|
			var prog =padNotes.indexOf(note);

			note.postln;

			if (prog.notNil and: {prog<noPOP}) {
				if (lastNote.notNil) { midi.noteOn(lastNote,0) }; // 0 off
				lastNote=note;
				studioModels[\toBecome].lazyValueAction_(prog);
			};
			
		};
		midi.programFunc = {|src, chan, prog, latency| "program in pop: ".post; prog.postln };
	}
	
	*saveMIDIPrefs{ midi.getSaveList.savePref("POP Controller") }
	
	*new {|inst,api| ^super.new.init(inst,api) } // what api is this? from inst. will this be used?
	
	init{|argInst,argApi|
		api              = argApi;
		inst             = argInst;
		presetsOfPresets = 2 ! noPOP;
		listOfPofP       = listOfPofP.add(this);
		instGUI          = IdentityDictionary[];
		items            = this.itemHeader ++ inst.popItems; // get from inst
	}
	
	*initModels{|argModels, midiControl|
		
		var lastProg;
		
		studioModels = argModels;  // this is studio's models becareful
		resets       = 0 ! maxPOP;
		
		// 16. program number (current) - auto works on this, needs a midi control
		studioModels[\program] =	[-1, [-1,noPOP-1,\lin,1,1], midiControl, 16, "Program",
			{|me,value,latency,send,toggle,jumpTo,offset|
				var padProg;
				this.modelSetProgram(value, false, latency, jumpTo, offset);
				padProg = (value%8)+(value.div(8)*16);
				if (lastProg.notNil) { midi.noteOn(lastProg,0, latency) }; // 0 off
				midi.noteOn(padProg, 48, latency); // 48 green
				lastProg = padProg;
				lastNote = nil;
			}].asModel
			.isProgramModel_(true);	
				
		// program number (to become)
		studioModels[\toBecome] = [-1, [-1,noPOP-1,\lin,1,1],
			{|me,value,latency,send| this.guiToBecome(value) }].asModel;
						
		// 17.quant on steps
		studioModels[\quant] = [32,[1,512,\lin,1,1], midiControl, 17, "Prog Steps",
			(label_:"Steps"),
			{|me,value,latency,send| studio.setPVP(\quant,value,nil,send) }].asModel;
		
	}

	// gui call: "to become program".
	// This is only called by program number (to become)
	*guiToBecome{|value|
		var beat=studio.instBeat;
		var quant=studioModels[\quant]*6;
		var when;
		// do program change now or later?
		if (studio.isPlaying) {
			// quantise timing
			when=(beat.div(quant)+1)*quant;
			classAPI.groupCmdOD(\netSetWhen,value,when);
		}{
			// not quantised so do it now instead of waiting for clockIn3
			classAPI.groupCmdOD(\netSetNow,value);
		}
	}
	
	// just set the program now, no beat quantise
	*netSetNow{|value|
		// we are not setting beat in the call 2 lazyValueActionDoAutoBeat_
		// this will add it automation on the current beat
		value = value.asInt;
		studioModels[\program].lazyValueActionDoAutoBeat_(value,nil,true);
		studioModels[\toBecome].lazyValue_(-1,false);
		when = nil;
	}

	// set to a time in the future
	*netSetWhen{|value,argWhen|
		value = value.asInt;
		when  = argWhen.asInt;
		studioModels[\toBecome].lazyValue_(value,false);
	}
	
	// program changes are synced to clock events
	*clockIn3{|beat,masterBeat,absTime,latency,absBeat|
		var pad, quant;	
		if (when.notNil) {
			// flash the gui
			if ((beat%12==0)) {
				gui[\program].flash_((beat%24).clip(0,1));
				if (studioModels[\toBecome]>=0) {
					var prog = (studioModels[\toBecome]%8)+(studioModels[\toBecome].div(8)*16);
					midi.noteOn(prog, ((1-(beat%24).clip(0,1))*51), latency);  // flashYellow
				};
			};	
			// so we can play catch up on the network the test here is >= and not ==
			if (beat>=when) {
				// special method to avoid latency adjustments in automation
				studioModels[\program].lazyValueActionDoAutoBeat_(
					studioModels[\toBecome],latency,true,false, masterBeat, beat-when);
				studioModels[\toBecome].lazyValue_(-1,false);
				when = nil;
			};
		};
		quant = studioModels[\quant].value;
		pad = (beat.div(6)%quant/quant*8).asInt;
		pad = #[120, 104, 88, 72, 56, 40, 24, 8].wrapAt(pad);
		if (pad != lastPad) {
			if (lastPad.notNil) { midi.noteOn(lastPad,0,latency) }; // 0 off
			midi.noteOn(pad, 48, latency);
			lastPad = pad;
		}
	}
	
	*clockStop{} // not using this for now

	// the model now sets the program
	*modelSetProgram{|value,updateGUI=true,latency,jumpTo,offset=0|
		// reset bar
		if ((resets[value].isTrue)and:{jumpTo!=\jumpTo}) { studio.resetInstBeat(offset) }; 
		listOfPofP.do{|pops| pops.modelSetProgram(value,updateGUI=true,latency) };
		if (updateGUI) { {studioModels[\program].value_(value)}.defer } ;
	}
	
	// set for each instance of each instrument
	modelSetProgram{|value,updateGUI=true,latency|
		var prog = presetsOfPresets[value]?2;
		if (prog>2) { inst.popSelectProgram((prog-3),latency) };// special, doesn't network in inst
		if (prog==2) {^this}; // do nothing
		// on/off, prog =  0 is off, 2 do nothing (above), everthing else>0 is on
		// testing for isFX is in instTemplate, is this best?
		inst.popOnOff_(prog.clip(0,1),latency) 
	}

	// GUI STuff ******************************************************
	
	// master control (LEFT)
	*createWidgets{|window,window2|
		
		var lastProgram;
		
		gui = IdentityDictionary[];	
		
		gui[\window] = window;
		gui[\window2] = window2;
		
		gui[\theme2]=(	\orientation_  : \horiz,
				\resoultion_	 : 3,
				\visualRound_  : 0.001,
				\rounded_      : true,
				\font_		 : Font("Helvetica",12),
				\showNumberBox_: false,
				\colors_       : (	\label : Color.black,
								\background : Color(0.43,0.44,0.43)*1.4,
								\backgroundDown : Color(0.1,0.1,0.1,0.85),
								\string : Color.black,
								\focus : Color(0,0,0,0.5)));
								
		// Quantise
		MVC_NumberBox(studioModels[\quant], gui[\window] ,Rect(13,45, 35, 18),  gui[\theme2])
			.orientation_(\vert)
			.labelShadow_(false);
		
		// the Edit menu
		MVC_PopUpMenu3(  gui[\window], Rect(51+2,45, 18, 18))
			.items_(["Copy","Paste", "-", "Insert","Clear","Remove"])
			.showTick_(false)
			.staticText_("")
			.color_(\background,Color(1,1,1,0.7))
			.color_(\string,Color.black)
			.canFocus_(false)
			.action_{|me|
				switch (me.value.asInt)
					{0}{ this.guiCopy   }
					{1}{ this.guiPaste  }
					{3}{ this.guiInsert }
					{4}{ this.guiClear  }
					{5}{ this.guiRemove }
			};
		
		MVC_StaticText(gui[\window], Rect(1, 1, 74, 27))
			.shadow_(false)
			.color_(\string,Color(0.85,0.85,0.85))
			.align_(\center)
			.penShadow_(true)
			.font_(Font("AvenirNext-Heavy",14))
			.string_("Program");	
		
		// 23.program number (to become)
		gui[\program]=MVC_ProgramChangeMain(studioModels[\toBecome],gui[\window],
			Rect(10,42+26,40,noPOP*21))
			.ww_(40)
			.hh_(21)
			.noPOP_(noPOP);
			
		noPOP.do{|i| this.createResetWidget(i) };
			
		// 18.program number	
		gui[\programFuncAdaptor]=MVC_FuncAdaptor(studioModels[\program]).func_{|me,value|
			gui[\program].actualProgram_(value);
			listOfPofP.do{|pop| pop.highlight(value,lastProgram) };
			lastProgram = value;
		}
		.numberFunc_(\intPlus1);

		// plainSquare to cover top of resets
 		MVC_PlainSquare(gui[\window],Rect(50, 68, 22, 1))
 			.color_(\on,Color.black)
 			.color_(\off,Color.black);

		// More
 		gui[\more]=MVC_FlatButton(gui[\window],Rect(47, (noPOP+3)*21+12, 19, 19),"+")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(10665/19997,223/375,2/3) )
			.color_(\down,Color(10665/19997,223/375,2/3) )
			.color_(\string,Color.white)
			.action_{ classAPI.groupCmdOD(\more) };
			
		// Less
 		gui[\less]=MVC_FlatButton(gui[\window],Rect(18, (noPOP+3)*21+12, 19, 19),"-")
			.rounded_(true)
			.canFocus_(false)
			.shadow_(true)
			.color_(\up,Color(10665/19997,223/375,2/3) )
			.color_(\down,Color(10665/19997,223/375,2/3) )
			.color_(\string,Color.white)
			.action_{  classAPI.groupCmdOD(\less) };
	
		// plainSquare to extend scroll
 		gui[\plainSquare]=MVC_PlainSquare(gui[\window],Rect(0, (noPOP+4.5)*21+3, 1, 1))
 			.color_(\on,Color.clear)
 			.color_(\off,Color.clear);
 			
 		// plainSquare to extend scroll
 		gui[\plainSquare3]=MVC_PlainSquare(gui[\window],Rect(0, 0, 1, 1))
 			.color_(\on,Color.clear)
 			.color_(\off,Color.clear);
 		
 		// plainSquare to extend scroll
 		gui[\plainSquare2]=MVC_PlainSquare(gui[\window2],Rect(0, (noPOP+4.5)*21+3, 1, 1))
 			.color_(\on,Color.clear)
 			.color_(\off,Color.clear);

	}
	
	// give me more programs i can use
	*more{
		this.noPOP_(noPOP+16);
		gui[\window2].view.visibleOrigin_(0@((21+4)*noPOP));
		gui[\window].view.visibleOrigin_(0@((21+4)*noPOP));
	}
	
	// give me less
	*less{ this.noPOP_(noPOP-16) }
	
	// set the resets via the network
	*netPopReset{|index,value|
		index=index.asInt;
		value=value.asInt;
		resets[index]=value;
		{gui[(\popReset++index).asSymbol].value_(value)}.defer;
	}
	
	// make the reset widgets
	*createResetWidget{|i|
		gui[(\popReset++i).asSymbol] = MVC_OnOffView(gui[\window],Rect(50,43+26+(21*i),22,21))
			.action_{|me|
				resets[i]=me.value;
				classAPI.groupCmdOD(\netPopReset,i,me.value)
			}
			.insetBy_(1)
			.hAdjust_(-1)
			.mode_(\icon)
			.iconOffColor_(true)
			.strings_(["return","return"])
			.color_(\background,Color(0,0,0))
			.color_(\on,Color(0.5,0.5,0.5))
			.color_(\off,Color(0.5,0.5,0.5))
			.color_(\icon,Color.white)
			.font_(Font("Helvetica",11))
	}
	
	// and free the widgets
	*freeResetWidget{|i|
		gui[(\popReset++i).asSymbol].free;
		gui[(\popReset++i).asSymbol]=nil;
		resets[i]=0;
	}
	
	
	// highlight a row as a program is selected
	highlight{|now,last|
		if (instGUI[(\pop++last).asSymbol].notNil) {instGUI[(\pop++last).asSymbol].highlight_(1)};
		if (instGUI[(\pop++now).asSymbol].notNil) {instGUI[(\pop++now).asSymbol].highlight_(0.45)};
	}
	
	itemHeader{ if (inst.canTurnOnOff) { ^["Mute", "On", " - "] } { ^[" - "] } }
	
	// put in the names of the presets
	items_{|list|
		if (inst.isKindOf(LNX_Sub37Control).not) {	
			items = this.itemHeader ++ list;
			noPOP.do{|i| instGUI[(\pop++i).asSymbol].items_(items) }
		};
	}
	
	// inst / channel control (RIGHT)
	createWidgets{|window|
		instGUI[\popWindow] = window;
		this.adjustSize;
		noPOP.do{|i| this.createPOPWidget(i) };
	}
	
	// individual widgets
	createPOPWidget{|i|
		
		instGUI[(\pop++i).asSymbol] = MVC_PopUpMenu3(instGUI[\popWindow],
			Rect(2,43+(21*i)+26,70,21))
			.color_(\background, inst.mixerColor)
			.font_(Font("Helvetica",11))
			.items_(items)
			.isPOPcanTurnOnOff_(inst.canTurnOnOff)
			.value_(presetsOfPresets[i] - (inst.canTurnOnOff.if(0,2)) )
			.action_{|me|
				// this uses the instrument api to talk to its instance of LNX_POP
				api.groupCmdOD(\netSetPOP,i,me.value);
			}
			.updateFunc_{ LNX_POP.alignFromPOP };
	}
	
	// keep both container views aligned
	*alignFromPOP{
		var pos = gui[\window2].view.visibleOrigin.clip(0,inf);
		
		gui[\plainSquare3].bounds_(Rect(0,pos.y,1,1));
		
		if ( gui[\window].visibleOrigin.y!=pos.y ) {
			gui[\window].visibleOrigin_(pos);
		};
	}
	
	// set the number of programs
	netSetPOP{|index,value|
		presetsOfPresets[index]=value + (inst.canTurnOnOff.if(0,2));
		{instGUI[(\pop++index).asSymbol].value_(value,false)}.defer;
	}
	
	guiSetPOP{|index,value| api.groupCmdOD(\netSetPOP,index,value); } 
	
	// free the instruments pop menu widgets
	freePOPWidget{|i|
		instGUI[(\pop++i).asSymbol].free;
		instGUI[(\pop++i).asSymbol]=nil;
		presetsOfPresets[i] = 2;
	}
	
	// change number of pops
	*noPOP_{|num|
		num=num.clip(16,maxPOP);
		if (num!=noPOP) {
			var old = noPOP;
			noPOP = num;
			// adjust gui widgets	
			gui[\more].bounds_(Rect(47, (noPOP+3)*21+12, 19, 19));
			gui[\less].bounds_(Rect(18, (noPOP+3)*21+12, 19, 19));
			gui[\plainSquare].bounds_(Rect(10, (noPOP+4.5)*21+4, 1, 1));
			gui[\plainSquare2].bounds_(Rect(10, (noPOP+4.5)*21, 1, 1));
			gui[\program].noPOP_(noPOP);
			studioModels[\program].controlSpec_( [-1,noPOP-1,\lin,1,1]);
			studioModels[\toBecome].controlSpec_( [-1,noPOP-1,\lin,1,1]);
			
			if (noPOP>old) {
				// add
				listOfPofP.do{|pop|
					pop.adjustSize;
					pop.presetsOfPresets = pop.presetsOfPresets.extend(noPOP,2);
					(old..(noPOP-1)).do{|i| pop.createPOPWidget(i) };
				};
				(old..(noPOP-1)).do{|i| this.createResetWidget(i)};
				resets = resets.extend(noPOP,0);	
			}{
				// remove	
				listOfPofP.do{|pop|
					pop.adjustSize;
					(noPOP..(old-1)).do{|i| pop.freePOPWidget(i) };
					pop.presetsOfPresets = pop.presetsOfPresets[0..(noPOP-1)];
				};
				(noPOP..(old-1)).do{|i| this.freeResetWidget(i) };
			};
			
			// bug fix to align resets
			{
				gui[\plainSquare2].moveBy(1,0);
				gui[\plainSquare2].moveBy(-1,0);
			}.defer;
			
		}
	}
	
	// adjust the size of the composite view
	adjustSize{
		instGUI[\popWindow].bounds_(instGUI[\popWindow].bounds.height_(21*(noPOP+4)+12));
	}
	
	// incoming midi program as a pipe ?
	pipeIn{|pipe| }	

	// get the save list
	*getSaveList{
		var list = ["SC POP Doc v1.1", studio.insts.size, noPOP, studioModels[\quant].value ]
					++ (resets[0..noPOP-1]);
		studio.insts.visualOrder.do{|inst,y| list = list ++ (inst.presetsOfPresets.getSaveList) };
		list = list ++ ["***EOD of POP Doc***"];
		^list;
	}
	
	// the insts pops, limit to number of programs
	getSaveList{ ^presetsOfPresets[0..noPOP-1] } 
	
	// put the load list
	*putLoadList{|list|
		list = list.reverse;
		if (list.popS=="SC POP Doc v1.0") {
			var noInstsLoading = list.popI;
			this.noPOP_(list.popI);
			studioModels[\quant].value_(list.popI);
			resets=list.popNI(noPOP);
			noPOP.do{|i| gui[(\popReset++i).asSymbol].value_(resets[i]) };
			studio.insts.visualOrder.do{|inst,y|
				inst.presetsOfPresets.putLoadList(list.popNI(noPOP)+2);
			};
		}{
			var noInstsLoading = list.popI;
			this.noPOP_(list.popI);
			studioModels[\quant].value_(list.popI);
			resets=list.popNI(noPOP);
			noPOP.do{|i| gui[(\popReset++i).asSymbol].value_(resets[i]) };
			studio.insts.visualOrder.do{|inst,y|
				inst.presetsOfPresets.putLoadList(list.popNI(noPOP));
			};
		};
	}
	
	// and put back in
	putLoadList{|list|
		presetsOfPresets=list;
		presetsOfPresets.do{|value,i|
			instGUI[(\pop++i).asSymbol].value_(value  - (inst.canTurnOnOff.if(0,2)) )
		};
	}
	
	// when adding a song
	*addLoadList{|list|
		list = list.reverse;
		if (list.popS=="SC POP Doc v1.0") {
			var noInstsLoading = list.popI;
			var addPOP = list.popI;
			this.noPOP_(noPOP.max(addPOP));
			list.popI; // just pop quant
			list.popNI(addPOP); // just pop resets
			studio.insts.visualOrder.keep(noInstsLoading.neg).do{|inst,y|
				inst.presetsOfPresets.addLoadList(list.popNI(addPOP));
			}
		}
	}
	
	// when adding a song
	addLoadList{|list|
		presetsOfPresets=list.extend(noPOP,2);
		presetsOfPresets.do{|value,i| instGUI[(\pop++i).asSymbol].value_(value) };
	}
	
	// when a song is closed, do this
	*reset{
		this.noPOP_(16);
		noPOP.do{|i|
			resets[i]=0;
			this.gui[(\popReset++i).asSymbol].value_(0);
		};
		studioModels[\quant].value_(32);
		studioModels[\program].value_(-1);
		studioModels[\toBecome].value_(-1);
	}
	
	// free stuff
	free{
		listOfPofP.remove(this);
		instGUI.do(_.free);
		api = inst = presetsOfPresets = nil;
	}	

	// ediT menu *****************************************************************

	// gui call to copy selected prog to clipboard
	*guiCopy{
		var prog = studioModels[\program].asInt;
		if (prog>=0) {
			clipboard = [ resets[prog],  studio.insts.size ] ++
				// gets pops in the same visual order we see them
				studio.insts.visualOrder.collect{|inst| 					inst.presetsOfPresets.presetsOfPresets[prog]
				};
		};
	}
	
	// gui call to paste clipboard to selected prog
	*guiPaste{ 
		var prog = studioModels[\program].asInt;
		if (clipboard.notNil && (prog>=0)) {	
			classAPI.groupCmdOD(\netPaste, prog, *clipboard) // network it
		};
	}
	
	// network call to paste clipboard to selected prog
	*netPaste{|prog,reset,noInst... pops|
		prog=prog.asInt;
		reset=reset.asInt;
		noInst=noInst.asInt;
		pops=pops.collect(_.asInt);
		if (prog<noPOP) {  // i shouldn't need this test
			this.netPopReset(prog,reset); // set the resets
			// put pops back in the same visual order we see them
			studio.insts.visualOrder.collect{|inst,i|
				// test to stop an insts over noInst in the clipboard
				if (i<noInst) { inst.presetsOfPresets.editSetPOP(prog,pops[i]) };
			};
		};	
	}
	
	// set the number of programs,  maintains index in pops & adjusts gui of canTurnOnOff insts
	editSetPOP{|index,value|
		presetsOfPresets[index]=value;
		{instGUI[(\pop++index).asSymbol].value_(value - (inst.canTurnOnOff.if(0,2)),false)}.defer;
	}
	
	// gui call, insert a clear row at selected prog
	*guiInsert{
		var prog = studioModels[\program].asInt;
		if (prog>=0) { classAPI.groupCmdOD(\netInsert,prog) }  // network it
	}
	
	// network call, insert a clear row at selected prog
	*netInsert{|prog|
		prog=prog.asInt;
		if (prog<(noPOP-1)) { 
			// working from the bottom up to prog+1
			((noPOP-1)..(prog+1)).do{|p| 
				this.netPopReset(p,resets[p-1]); // move resets
				// and move pops
				studio.insts.visualOrder.collect{|inst,i|
					inst.presetsOfPresets.editSetPOP(
						p,inst.presetsOfPresets.presetsOfPresets[p-1])
				}
			}
		};
		this.netClear(prog); // clear the selected row
	}
	
	// gui call, clear row at selected prog
	*guiClear{
		var prog = studioModels[\program].asInt;
		if (prog>=0) { classAPI.groupCmdOD(\netClear, prog) };  // network it
	}
	
	// network call, clear row at selected prog
	*netClear{|prog|
		prog=prog.asInt;
		this.netPopReset(prog,0);
		studio.insts.visualOrder.collect{|inst,i| inst.presetsOfPresets.editSetPOP(prog,2) }
	}
	
	// gui call, remove row at selected prog 
	*guiRemove{
		var prog = studioModels[\program].asInt;
		if (prog>=0) { classAPI.groupCmdOD(\netRemove,prog) }  // network it
	}
	
	// network call, remove row at selected prog 
	*netRemove{|prog|
		prog=prog.asInt;
		if (prog<(noPOP-1)) { // test to stop if last row selected
			// working from prog down to noPOP-2
			((prog)..(noPOP-2)).do{|p|
				this.netPopReset(p,resets[p+1]); // move reset
				// and move pops
				studio.insts.visualOrder.collect{|inst,i|
					inst.presetsOfPresets.editSetPOP(
						p,inst.presetsOfPresets.presetsOfPresets[p+1])
				}
			}
		};
		this.netClear(noPOP-1); // clear the bottom row
	}
		
}

