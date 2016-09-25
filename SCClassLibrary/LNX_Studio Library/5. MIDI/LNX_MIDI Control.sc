/*
// the class organises the instances and directs the midiControlIn
LNX_MIDIControl.elements.postList;

// the instance is for the whole instrument / midiControl
a.a.models[3].controlGroup.modelIDs
a.a.models[0].controlGroup.models
a.a.models[0].controlGroup.names
*/

// v1.2 has automation data saved

LNX_MIDIControl {

	classvar <>autoSave=true;

	classvar <patches, <noPatches=0,
			<activeModel,<elements,<window,p, controlView, elementsToEdit, readableList,
			startValue, group, ids, idIndex, startId, headerGUI, displayParent, <version="v1.2",
			<>studio;

	var		<>patchNo, <parent, <>modelIDs, <>models, <>names;

	*initClass {
		patches=[];
		elements=[];
	}

	*new {|parent|
		^super.new.init(parent);
	}

	init { arg argParent;
		parent=argParent ? ();
		this.initInstance;
		this.initVars;
		^this
	}

	initInstance{
		patches=patches.add(this);
		patchNo=noPatches;
		noPatches=noPatches+1;
	}

	initVars{
		models=[];
		modelIDs=[];
		names=[];
	}

	// register a model with an id and name
	register{|id,model,name|
		name=name ? "unnamed";
		if (modelIDs.includes(id).not) {
			modelIDs=modelIDs.add(id);
			models=models.add(model);
			names=names.add(name);
		}{
			models.put(modelIDs.indexOf(id),model); // this replaces previously registered models
		};
	}

	changeModelID{|oldID,newID|
		// don't remove, could cause problems for other models
		//		if (	oldID!=newID) {
		//			this.removeModel(newID); // remove if it already exists
		//		};

		// but on the other hand removeModel must be done before changeModelID
		// else we may remove a changed modelID

		elements.do{|element,i|
			if ((element[3]===this)and:{element[4]==oldID }) {
				element[4]=newID;					// change the elements
			};
		};
		modelIDs[modelIDs.indexOf(oldID)] = newID; // and the model ID
	}

	// remove that model
	removeModel{|id,send=true|
		var index;
		elements.removeAllSuchThat({|element|
			// this & element[3] are the instance of MIDIControl
			// element[4] is the model ID
			(element[3]===this)and:{element[4]==id }
		});
		if (send) {
			// update the parent of the MIDIControl (not the model!)
			this.parent.newMidiControlAdded;
		};
		this.class.updateGUI;

		// test here because MVC_Model:changeControlID	might ask to remove a non-existant control
		if (modelIDs.includes(id)) {
			index = modelIDs.indexOf(id);
			modelIDs.removeAt(index);
			models.removeAt(index);
			names.removeAt(index);
		};
	}

	// used in MVC_Model:midiLearn_ to make control active to midi Learn
	*makeElementActive{|e|
		if (activeModel.notNil) { activeModel.deactivate };
		activeModel=e;
	}

	*makeElementGroupActive{|e|
		activeModel=e;
	}

	// all midi controls gets passed here by midiPatch
	*controlIn { |src, chan, num, val, latency, send, ignore|
		//[src, chan, num, val].postln;
		var name;
		if ((activeModel.notNil)and:{ignore.not}) {
			name=this.learnIn(src, chan, num, val)
		}{
			elements.do({|e|
				if ((src==e[0]) and: {chan==e[1]} and: {num==e[2]}) {
					e[3].models[e[3].modelIDs.indexOf(e[4])].midiSet
						(val.map(0,127,e[5],e[6]),e[7],latency,send); // much better!
				};
			});
		};
		^name
	}

	// learn this control
	*learnIn{ |src, chan, num, val|
		var exists, name;
		if (activeModel.notNil) { // shouldn't need this if but safer with it
			exists=false;
			elements.do({|e|
				if (	     ( src==e[0])
					and: {chan==e[1]}
					and: { num==e[2]}
					and: {activeModel.controlGroup===e[3]}
					and: {activeModel.controlID==e[4]}
					){
						exists=true
					};
			});
			if (exists.not) {
				elements=elements.add([
					src,                             // e[0]
					chan,                            // e[1]
					num,                             // e[2]
					activeModel.controlGroup,        // e[3]
					activeModel.controlID,           // e[4]
					0,                               // e[5]
					127,                             // e[6]
					activeModel.defaultControlType   // e[7]
				]);
				{this.updateGUI}.defer;
				this.networkAnyChanges(activeModel);
				//activeModel.controlGroup.parent.newMidiControlAdded;
				name=activeModel.controlGroup.names[
					activeModel.controlGroup.modelIDs.indexOf(activeModel.controlID)];


			};
			activeModel.deactivate;
			activeModel=nil;
		}
		^name
	}

	*networkAnyChanges{|element| element.controlGroup.parent.newMidiControlAdded }

	*clear{ patches.do(_.clear)	}

	post { "a LNX_MIDIControl :".postln; this.getElements.do(_.postln); }

	*post { "LNX_MIDIControl :".postln; elements.do(_.postln); }

	getElements{
		var l;
		l=[];
		elements.do({|e,j| if (e[3]===this) { l=l.add(e++[j]) } });
		^l
	}

	getSaveElements{
		var l,t;
		l=[];
		elements.do({|e| if (e[3]===this) { t=e.copy; t.removeAt(3); l=l.add(t) } });
		^l
	}

	// from elements with this
	clear{
		elements.removeAllSuchThat({|item| item[3]===this });
	}

	// this frees the whole midiControl
	free{
		this.clear;
		models=[]; // ??
		modelIDs=[];

		// need to remove from patches=[];

		if ((patchNo+1)<noPatches) {
				for (patchNo+1, noPatches - 1, {|i|
					patches[i].patchNo=patches[i].patchNo - 1;
				});
			};
		patches.removeAt(patchNo);
		noPatches=patches.size;

	}

	getDetails{|id|
		if (parent==studio) {
			^["LNX_Studio", names[modelIDs.indexOf(id)]]
		}{
			^[(parent.instNo+1)++". "++ (parent.name), names[modelIDs.indexOf(id)]]
		}
	}

	getDetails2{|id| ^[parent.id, id] }

	getModel{|id| ^models[modelIDs.indexOf(id)] }


	////////////

	getSaveList{
		var l,saveList;
		var modelDict;

		l=this.getSaveElements;
		saveList=["*** MIDI Control Doc"+version,l.size];
		l.do({|i| saveList=saveList++i});

		if (autoSave) {

			modelDict = IdentityDictionary[]; // this is how models should be stored anyways
			models.do{|model,i| modelDict[modelIDs[i]] = model};
			modelDict = modelDict.select{|auto| auto.automation.size>0 };
			saveList = saveList.add(modelDict.size);
			modelDict.pairsDo{|id,model|
				saveList = saveList ++ [id] ++ (model.getSaveList)
			};

		}{
			saveList = saveList.add(0);
		};

		saveList=saveList++["*** End MIDI Control Doc ***"];
		^saveList
	}

	getSaveListForLibrary{
		var l,saveList;
		l=[]; // no midi controls
		saveList=["*** MIDI Control Doc"+version,l.size];
		saveList = saveList.add(0); // no automation
		saveList=saveList++["*** End MIDI Control Doc ***"];
		^saveList
	}

	putLoadList{|l,version|
		var noE, element;
		this.clear;
		l=l.reverse;
		noE=l.pop.asInt;
		noE.do({
			element=l.popNI(7); // this stops the use of symbols for ids
			//element=(l.popNI(3))++(l.pop)++(l.popNI(3));  // this makes Char which is no good
			element=element.insert(3,this);
			elements=elements.add(element); // fix saving and loading to include new params
		});
		// if there is automation data
		if (version>=1.2){
			var noAutos = l.popI;
			noAutos.do{
				var id = l.popI;
				var size = l.popI;
				var events = l.popNF(1+(size*2));
				models[modelIDs.indexOf(id)].putLoadList(events);
			}
		};
	}

	/////////////

	*editControls{|argDisplayParent|
		displayParent=argDisplayParent;
		this.createGUI;
		controlView.index_(nil);
		controlView.resetViewIndex;
	}

	*updateGUI{ if (((window.isNil)or:{window.isClosed}).not) {this.createGUI} }

	*networkElementChanges{|e| elements[e.asInt][3].parent.newMidiControlAdded }

	*front { window.front }

	*closeWindow{
		if ((window.notNil)and:{window.isClosed.not}) { window.close };
		window=nil;
	}

	*createGUI{|update=true|

		var gui;

		if (update) {
			if (displayParent.isNil.not) { p=displayParent.controlPatches }; // all patches
			p = p ? patches.copy;
			p=[p].flat;
			elementsToEdit=[];
			p.do({|p| elementsToEdit=elementsToEdit++(p.getElements) });
		};

		readableList=[]; // [srcName, chan, num, parent, parent's name, id,
					   //  midiControl, index, parameter name, min, max, type]
		elementsToEdit.do({|e|
			var id, group, index;
			group=e[3];
			id=e[4];
			index=group.modelIDs.indexOf(e[4]);
			readableList=readableList.add([
				LNX_MIDIPatch.uidInName(e[0]),
				e[1],
				e[2],
				group.parent,
				group.parent.name,
				id,
				group,
				index,
				group.names[index],
				e[5],
				e[6],
				e[7]
			]);
		});

		if (((window.isNil)or:{window.isClosed}).not) {

			headerGUI.string_("MIDI Controls: "++(displayParent.controlTitle));
			controlView.items_(readableList).refresh;

		}{
			if (displayParent.notNil) {
					// this stops opening before 1st call. this could be done in a clearer way
				window=Window.new("MIDI Controls",
						Rect	(Window.screenBounds.left+LNX_Studio.osx,
							 Window.screenBounds.height-147-(22*3)-(LNX_Studio.thisHeight),
							 550-13+22, 280-25+22), resizable: false);
				window.view.background = Color(0,1/103,3/77,65/77);
				window.toFrontAction_{studio.frontWindow_(window)};
				window.front; //.alwaysOnTop_(true);

				gui=();

				MVC_RoundBounds(window, Rect(11,11,window.bounds.width-22,window.bounds.height-22-1))
					.width_(6)
					.color_(\background, Color(29/65,42/83,6/11));

				// the main view
				gui[\scrollView]=CompositeView(window, Rect(11,11,window.bounds.width-22,window.bounds.height-22-1))
					.background_(Color(50/77,56/77,59/77));
					//.hasHorizontalScroller_(false)
					//.hasVerticalScroller_(false);

				headerGUI = StaticText.new(gui[\scrollView],Rect(16, 6, 480, 22))
					.font_(Font("Helvetica",12,true))
					.string_("MIDI Controls: "++(displayParent.controlTitle))
					.stringColor_(Color.black)
					.action_{|v| };

				StaticText.new(gui[\scrollView],Rect(16, 8+17, 480, 22))
					.string_("       Path                    Parameter"++
					"          MIDI Port            Ch   Num  Min  Max  Type")
					.stringColor_(Color.black)
					.action_{|v| };

				// Ok
				MVC_OnOffView(gui[\scrollView],Rect(470, 237-16, 50, 20),"Ok")
					.rounded_(true)
					.color_(\on,Color(1,1,1,0.5))
					.color_(\off,Color(1,1,1,0.5))
					.action_{	 window.close};

				MVC_OnOffView(gui[\scrollView],Rect(100, 237-16, 50, 20),"Delete")
					.rounded_(true)
					.color_(\on,Color(1,1,1,0.5))
					.color_(\off,Color(1,1,1,0.5))
					.action_{ controlView.deleteItem; controlView.focus };

				MVC_OnOffView(gui[\scrollView],Rect(15, 237-16, 70, 20),"Delete All")
					.rounded_(true)
					.color_(\on,Color(1,1,1,0.5))
					.color_(\off,Color(1,1,1,0.5))
					.action_{
						var elementsToDelete, tempE;
						if (p.isNil.not) {
							elementsToDelete=[];
							p.do({|p| elementsToDelete=elementsToDelete++(p.getElements) });
							elementsToDelete.do({|e| elements[e[8]]=nil });
							tempE=[];
							elements.do({|e| if (e.isNil.not) { tempE=tempE.add(e) } });
							elements=tempE;
							controlView.indexNoRefresh_(nil);
							this.createGUI;
							controlView.focus;
							p.do{|p| p.parent.newMidiControlAdded}; // network all patches
						};
					};

				MVC_OnOffView(gui[\scrollView],Rect(165, 237-16, 40, 20),"Flip")
					.rounded_(true)
					.color_(\on,Color(1,1,1,0.5))
					.color_(\off,Color(1,1,1,0.5))
					.action_{ controlView.flip; controlView.focus };

				controlView=LNX_MIDIControlInterface(gui[\scrollView],Rect(15,30+15,502,180-16),this)
				.items_(readableList)
				// mouse down action
				.action_{|me|
					var i,sx,uid;
					//me.index.postln;
					i=elementsToEdit[me.index][8];
					sx=me.startX;
					if (sx==0) {
						group=elements[i][3];
						idIndex=patches.indexOf(group);
						startId=elements[i][4];
						startValue=idIndex;
					};
					if (sx==1) {
						group=elements[i][3];
						ids=group.modelIDs;
						idIndex=ids.indexOf(elements[i][4]);
					};
					if (sx==2) {
						uid=elements[i][0];
						startValue=LNX_MIDIPatch.midiSourceUIDs.indexOf(uid);
						if (startValue.isNil) {
							startValue=0;
							this.createGUI;
						};
					};
					if (sx==3) { startValue=elements[i][1] };
					if (sx==4) { startValue=elements[i][2] };
					if (sx==5) { startValue=elements[i][5] };
					if (sx==6) { startValue=elements[i][6] };
					if (sx==7) {
						elements[i][7]=(elements[i][7]+1).wrap(0,4);
						startValue=elements[i][7];
						this.createGUI;
						this.networkElementChanges(i); // network the changes
					};
				}
				// and move
				.mouseMoveAction_{|me,i,x,y|
					var sx,newVal, oldPatch;
					//me.index.postln;
					i=elementsToEdit[me.index][8];
					sx=me.startX;

					// there is a bug in this

//					if (sx==0) {
//						newVal=(startValue-((y/10).asInt)).clip(0,patches.size- 1);
//						// warning for later. this will not be
//						// compatable with multiple studio's
//						newVal=newVal.asInt;
//						if (newVal!=idIndex) {
//							idIndex=newVal;
//							oldPatch=elements[i][3]; // for networking
//							elements[i][3]=patches[idIndex];
//							if (patches[idIndex].modelIDs.includes(startId)) {
//								elements[i][4]=startId.asInt;
//							}{
//								elements[i][4]=0;
//							};
//							elementsToEdit[me.index][3]=patches[idIndex];
//							elementsToEdit[me.index][4]=elements[i][4];
//							this.createGUI(false);
//							oldPatch.parent.newMidiControlAdded; // network old
//						};
//					};

					if (sx==1) {
						elements[i][4]=ids[(idIndex-((y/10).asInt)).clip(0,ids.size- 1)];
						this.createGUI;
					};
					if (sx==2) {
						elements[i][0]=LNX_MIDIPatch.midiSourceUIDs[(startValue+((y/15).asInt))
							.clip(0,LNX_MIDIPatch.midiSourceUIDs.size-1)];
						this.createGUI;
					};
					if (sx==3) {
						elements[i][1]=(startValue-((y/10).asInt)).clip(0,15);
						this.createGUI
					};
					if (sx==4) {
						elements[i][2]=(startValue-((y/2.5).asInt)).clip(-1,127);
						this.createGUI
					};
					if (sx==5) {
						elements[i][5]=(startValue-((y/2.5).asInt)).clip(0,127);
						this.createGUI
					};
					if (sx==6) {
						elements[i][6]=(startValue-((y/2.5).asInt)).clip(0,127);
						this.createGUI
					};
					if (sx==7) {
						elements[i][7]=(startValue-((y/20).asInt)).clip(0,4);
						this.createGUI
					};
					this.networkElementChanges(i); // network the changes
				}
				.mouseUpAction_{|me,i,x,y|
					var sx,newVal;
					//me.index.postln;
					i=elementsToEdit[me.index][8];
					sx=me.startX;
					//if (sx==0) {
						if (me.index>=me.items.size) {me.index_(nil)};
						this.createGUI;
					//};
				}
				.doubleClickAction_{|me| }
				.flipAction_{|me|
					var i,t;
					i=elementsToEdit[me.index][8];
					t=elements[i][5];
					elements[i][5]=elements[i][6];
					elements[i][6]=t;
					this.createGUI;
					this.networkElementChanges(i); // network this patch
				}
				.deleteKeyAction_{|me|
					var i,patch;
					i=elementsToEdit[me.index][8];
					patch=elements[i][3]; // used later for network update
					elements.removeAt(i);
					me.indexNoRefresh_(nil);
					this.createGUI;
					patch.parent.newMidiControlAdded; // network that shit
				};

			};

		};

	}

}
	