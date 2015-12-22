
// superclass for all MVC views

// aims :
// create a template for all views
// views interact in a very similar way
// creation of view is independant of view init so all can start after creation
// if window is closed, view remembers all states and can recreate themselves
// interface with MVC_Model

// TO check in sc 3.4 horizontal view & numberbox cause a tripple refresh
// TO DO MVC_Views not attached to a model do not return action.value(this,value)
// TO DO i can now check which clock this process is running in and defer if needed with
// thisThread.clock==SystemClock


MVC_View {

	classvar	<>editMode=false,		<>grid=1,		
			<>editResize=true,		<>verbose=false,
			<>labelActivatesMIDI=true, <>showLabelBackground=false;
		
	var	<model;
	
	var	<parent,		<>window, 	<rect,
		<l, 			<t, 			<w, 		<h,
		<view,		<enabled=true,<canFocus=false,
		<string,		<>onClose,	<strings,
		<colors,		<font,		buttonPressed,
		lw, 			lh,			<visible=true,
		<orientation=\vertical,		isSquare=false,
		<layout=\normal;
		
	var	<value=0,		<controlSpec, <items;
		
	var	<>action,		<>action2	,	<actions;	
		
	var	<startX,		<startY,		<startVal;
	
	var	<midiLearn=false,	<>evaluateAction=true, 	<>hasMIDIcontrol=false;

	var	<midiLearn2=false,	<>hasMIDIcontrol2=false;
	
	var	<resize=1,		<>resizeAction;
	
	var	<label, <>labelFont, <labelShadow=true, <labelBounds, <labelGUI;
	
	var	<numberFunc,		<numberString="",	<numberGUI,    <>numberFont,
		numberHeld=false,	<>numberWidth=0 ,	<numberBounds,  <showNumberBox=true,
		<showUnits=true;
		
	var	<>boundsAction, 	<>mouseDownAction, <>mouseMoveAction, <>mouseUpAction,
		<>mouseOverAction, <>colorAction;
	
	var	<>viewEditMode=false,	<>viewGrid=1,		<>viewEditResize=false;
	
	var	<hidden=false, <hiddenBounds, <>canBeHidden=true;
	
	var <>locked = false, <>numberOffset = 0;
	
	var <beginDragAction, <canReceiveDragHandler, <receiveDragHandler;
	
	// you can supply a MVC_Model, MVC_TabView, a SCWindow, a MVC_Window, a MVU_ScrollView, a Rect
	// or a themeMethod Dict in any order
	
	*new {|...args| ^super.new.init(*args) }
		
	// now resolve these args into the parent view (window), a model, a rect, a theme, strings
	
	init {|...args|
		// get aRect, aModel and aWindow from the args if present
		var themeMethods = args.findKindOf(IdentityDictionary);
		var open = args.findKindOf(Boolean);
		rect    = args.findKindOf(Rect) ? Rect(); // temp fix for ColorAdaptor & FuncAdaptor
		model   = args.findKindOf(MVC_Model       );
		window  = args.findKindOf(MVC_Window      )  ??
		         {args.findKindOf(MVC_TabbedView  )  ?? 
		         {args.findKindOf(MVC_TabView     )} ??
		         {args.findKindOf(MVC_ExpandView  )} ??
		         {args.findKindOf(MVC_ScrollView) }};
		strings = args.select(_.isKindOf(String));
		
		// register this MVC_item with the parent
		if (window.notNil) {
			window.addView(this);	// add so this view can be created with the MVC_ScrollView
			parent=window; // parent will be the mvc window view & window the view
			// the follow will add view if MVC_Window/MVC_TabbedView/MVC_ScrollView is open
			if (window.notClosed) {
				window=window.view;
			}{
				window=nil;	// MVC_ScrollView / Window ? is not a real view
							// the real view doesn't exist yet so don't use as one
			};
		}{
			window=args.findKindOf(SCWindow)
				?? { args.findKindOf(SCScrollView) }
				?? { args.findKindOf(SCCompositeView) };
			
		};
		
		// these are common vars used in most views
		l=rect.bounds.left;
		t=rect.bounds.top;
		w=rect.bounds.width;
		h=rect.bounds.height;
		font=Font("Helvetica-Bold",12);
		// standard colours for all views
		colors=(
			'background'	: Color.black.alpha_(0.5),
			'disabled'	: Color(0.3,0.3,0.3),
			'midiLearn'	: Color.ndcMIDIActive,
			'label'		: Color.white,
			'focus'  		: Color.clear
		//	'main'		: Color(0.65,1,0.8) // to use as main color
		);
		labelGUI=IdentityDictionary[];
		actions=IdentityDictionary[];
		this.initView;
		
		
		if (isSquare) {
			w=w.clip(5,h);	
			h=h.clip(5,w);
			rect = Rect(l,t,w,h);
		};
		
		// add to a model if we have one,
		// it's important that we add last so model can update after init
		if (model.notNil) {model.addView(this)};
		
		// apply a theme 
		if (themeMethods.notNil) { this.themeMethods_(themeMethods) };
		
		// and make it if in a standard window
		if (open!=false) {
			if (window.notNil) { this.create(window) }
		};
	}
	
	// change the model (quicker than creating a new widget)
	model_{|argModel|
		if (model.notNil) { model.removeView(this) };
		model=argModel;
		if (model.notNil) { model.addView(this) };
	}
	
	// override this
	initView{}
	
	// create the gui's items that make this MVC_View
	create{|argWindow|
		if (view.isClosed) {
			if (argWindow.notNil) { window = argWindow };
			// the following order is important
			// DOES this happen any more? >> also makes sure the view overlaps number
			if ((numberFunc.notNil) and:{showNumberBox}) { this.createNumberGUI };
			this.createView;
			
			view.canFocus_(canFocus)
				.visible_(visible)
				.focusColor_(colors[\focus])
				.onClose_{ onClose.value(this) }
				.resize_(resize)
				.mouseOverAction_(mouseOverAction)
				
				//.mouseOverAction_{|me,x,y| [me,x,y,label].postln} // for help later
				;				
			this.addControls;
			this.createLabel;
			this.dragControls;
			this.postCreate;
		}{
			"View already exists.".warn;
		}
	}
	
	// override this
	createView{
		view=SCUserView.new(window,rect)
			.drawFunc={|me|
				Pen.use{
					Pen.smoothing_(false);
					colors[0].set;
					Pen.fillRect(Rect(0,0,w,h));
				}; // end.pen
			};
	}
	
	// override this
	addControls{}
	
	// override this if needed + also add for label (there are refs that need to be removed here)
	dragControls{
			
		// what i send
		if (beginDragAction.notNil) {
			view.beginDragAction_(beginDragAction); 
		}{
			view.beginDragAction_{|me|
				var val;
				if (numberFunc.isNil) {
					val=value.asString
				}{
					val=numberFunc.value(value).asString;
				};
				if ((controlSpec.notNil) && (showUnits)) { val=val+(controlSpec.units) };
				// +units
				me.dragLabel_(val); // the drag label
				value.asFloat;
			};
		};
		
		// what can i recieve
		if (canReceiveDragHandler.notNil) {
			view.canReceiveDragHandler_(canReceiveDragHandler);
		}{
			view.canReceiveDragHandler_{ SCView.currentDrag.isNumber };
		};
		
		// what i get passed
		if (receiveDragHandler.notNil) {
			view.receiveDragHandler_(receiveDragHandler);
		}{		
			view.receiveDragHandler_{
				this.valueAction_(SCView.currentDrag, send:true);
				this.refreshValue;	
			};
		};
				
		// just add begin to number box
		if (numberGUI.notNil) { numberGUI.beginDragAction_(view.beginDragAction) };
		labelGUI.do{|labelView| labelView.beginDragAction_(view.beginDragAction) };
	}
	
	// what i send
	beginDragAction_{|func|
		beginDragAction=func;
		if (view.notClosed) { view.beginDragAction_(func) }
	}
	
	
	// what can i recieve
	canReceiveDragHandler_{|func|
		canReceiveDragHandler=func;
		if (view.notClosed) { view.canReceiveDragHandler_(func) }
	}
	
	// what i get passed
	receiveDragHandler_{|func|
		receiveDragHandler=func;
		if (view.notClosed) { view.receiveDragHandler_(func) }
	}
	
	// override to do anything after creation
	postCreate{}
	
	// is closed
	isClosed { if (view.isNil) { ^true } { ^view.isClosed } }

	// is not closed
	notClosed { ^this.isClosed.not }
	
	// assign a ControlSpec to map to
	controlSpec_{|spec|
		if (spec.notNil) {
			controlSpec=spec.asSpec;
			value=controlSpec.constrain(value);
			this.refresh;
		}{
			controlSpec=nil;
		}
	}
	
	// make the view cyan / magenta for midiLearn active
	midiLearn_{|bool|
		midiLearn=bool;
		this.refresh;
		labelGUI.do(_.refresh);
	}
	
	// toggle the midi mode
	toggleMIDIactive{
		if (hasMIDIcontrol) {
			evaluateAction=false;
			model.midiLearn_(midiLearn.not);
		};
	}

	// deactivate midi learn from this item
	deactivate{
		midiLearn=false;
		this.refresh;
		labelGUI.do(_.refresh);
	}
	
	// set the string
	string_{|argString|
		string=argString.asString;
		if (view.notClosed) {this.refresh};
	}
	
	// set the strings, used in items like flat button
	strings_{|list|
		if (list.isString) {list=[list]};
		strings=list;
		if (view.notClosed) {this.refresh};
	}
	
	// user can focus on item
	canFocus_{|bool|
		canFocus=bool;
		if (view.notClosed){ view.canFocus_(canFocus) }
	}
	
	// set focus to this item
	focus{ if (view.notClosed) { view.focus } }

	// resize item
	resize_{|num|
		resize=num;
		if (view.notClosed){
			view.resize_(resize);
			labelGUI.do{|view| view.resize_(resize)};	
		}
	}
	
	// resize action called by parents
	doResizeAction{
		if (this.notClosed) {
			rect=view.bounds;
			l=rect.bounds.left;
			t=rect.bounds.top;
			w=rect.bounds.width;
			h=rect.bounds.height;
			resizeAction.value(this);
			this.adjustLabels;
		}
	}
	
	// the bounds (should i change rect to bounds?)
	bounds{^rect}

	// set the bounds
	bounds_{|argRect|
		rect=argRect;
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		if (view.notClosed) { view.bounds_(rect) };
		this.adjustLabels;
	}
	
	// set the top only
	top_{|value|
		if (value!=t) {
			this.bounds_(Rect(l,value,w,h))
		}
	}
	
	// item is enabled	
	enabled_{|bool|
		if (enabled!=bool) {
			enabled=bool;
			this.refresh;
			this.labelRefresh;
		}
	}
	
	// the item visible (when notVisible, remove but do not free this holder)
	visible_{|bool|
		if (visible!=bool) {
			visible = bool;
			if (view.notClosed) {
				view.visible_(visible);
				labelGUI.do{|v| v.visible_(visible)};
				if (numberGUI.notNil) { numberGUI.visible_(visible) };
			};
		}
	}	
	
	isVisible{^visible}
	
	// helps resolve a drawing issue in MVC_ExpandView, makes bounds=Rect(0,0,0,0)
	hidden_{|bool|
		if (canBeHidden) {
			if (hidden!=bool) {
				hidden=bool;
				if (hidden) {
					hiddenBounds=this.bounds;
					this.bounds_(Rect(0,0,0,0));
				}{
					this.bounds_(hiddenBounds);
				};
			}
		}
	}
	
	// I/O ///////////////////////////////////////////////////////////////
	
	// get a save list from this GUI item
	getSaveList{
		var l, iList=this.iGetSaveList;	
		l=["*** MVC_View Document v1 ***", colors.size, iList.size,
			this.bounds.left, this.bounds.top, this.bounds.width, this.bounds.height];
					
		colors.pairsDo{|key,color| l=l++[key,color.red,color.green,color.blue,color.alpha] };
		l=l++iList;
		l=l.add("*** END MVC_View Document ***");
		^l
	}
	
	// put a save list back into this gui
	putLoadList{|l|
		var header, colorSize, iListSize, dict;
		l=l.reverse; // reverse for popping
		header=l.popS;
		if (header=="*** MVC_View Document v1 ***") {
			#colorSize, iListSize = l.popNI(2);
			this.bounds_(l.popNI(4).asRect);
			dict=();
			colorSize.do{
				var key,color;
				key=l.pop.asSymbol;
				color= Color(*l.popNF(4));
				dict[key]=color;
			};
			this.colors_(dict);
			this.iPutLoadList(l.popN(iListSize))
			
		}{
			"WARNING: not an MVC_View Document".postln;
			l.reverse.postln;
		};
	}
	
	// override as needed	
	iGetSaveList{ ^[] }
	
	// override as needed
	iPutLoadList{|list| }
	
	///////////////////////////////////////////////////////////////////////////
	
	// delete this object
	free{
		this.remove;
		model=parent=window=onClose=action=actions=string=strings=items=
		colors=labelGUI=font=controlSpec=label=labelFont=resizeAction=
		numberFunc=numberString=numberFont=boundsAction=nil;
		this.viewFree;
	}
	
	viewFree{} // override this (used in adaptors)
	
	// from from window
	remove{
		
		if (parent.notNil) { parent.removeView(this) }; // remove so this view can be deleted
		if (view.notClosed) {
			view.free;
			labelGUI.do(_.free);
			numberGUI.free;
		};
		
		labelGUI=IdentityDictionary[];
		view=nil;
		numberGUI=nil;
	}
	
	removeModel{ if (model.notNil) { model.removeView(this) } }
	
	// color & colors may reqire some work for non SCView widgets

	// set the colour in the Dictionary 
	color_{|key,color,forceAdd=false|
		
		if  ((forceAdd.not)and:{ (colors.includesKey(key).not)}) {^this}; // drop out
		colors[key]=color;
		if (key=='focus') {
			{
				if (view.notClosed) { view.focusColor_(color) }
			}.defer;
		}{
			if (key=='label') {
				labelGUI.do(_.refresh);
			}{
				this.refresh;
			};
			
		}
	}
	
	// add a dict of colours, useful for colour themes
	colors_{|dict|
		dict.pairsDo{|key,color|
			if (colors.includesKey(key)) {
				colors[key]=color;
			}
		};
		this.refresh;
		labelGUI.do(_.refresh);
	}

	// add a dict of colours, useful for colour themes
	colorWithAction_{|key,color|
		this.color_(key,color);
		colorAction.value(this,key,color);
	}
	
	// do theme method data
	themeMethods_{|dict|
		dict.pairsDo{|method,args|	
			if (this.respondsTo(method)) { this.perform(method,args) }
		};
	}
	
	// set the font
	font_{|argFont|
		font=argFont;
		this.refresh;
	}
	
	// also called from model, sets the value of the gui item
	value_{|val|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if (value!=val) {
			value=val;
			this.refresh;
		};
	}

	// this is also used by SeqView's adject gui items to draw lines
	valueAction_{|val,latency,send,toggle,button|
		if (controlSpec.notNil) { val=controlSpec.constrain(val) };
		if (value!=val) {
			value=val;
			this.refresh;
			if (model.notNil) {
				model.valueAction_(val,latency,send,toggle,button);
			}
		};
	}
	
	// used in gui controls to update model, set value and call action as needed
	// no controlSpec constraints (this should be done in the individual view)
	viewValueAction_{|val,latency,send,toggle,button|
		value=val;
		this.refresh;
		if (model.notNil){ model.valueAction_(val,latency,send,toggle,this,button) };
		action.value(this,latency,send,toggle,button)
	}
	
	// and the 2nd action (i might move this to MVC_OnOffView)
	viewValueAction2_{|val,latency,send,toggle|
		value=val;
		this.refresh;
		if (model.notNil){ model.valueAction2_(val,latency,send,toggle,this) };
		if (action2.notNil) {
			action2.value(this,latency,send,toggle)
		}{
			action.value(this,latency,send,toggle)
		};
	}
	
	// used in gui controls to update model via doValueAction_
	viewDoValueAction_{|val,latency,send,toggle,clickCount|
		value=val;
		this.refresh;
		if (model.notNil){ model.doValueAction_(val,latency,send,toggle,this,clickCount) };
		action.value(this,latency,send,toggle)
	}
	
	// used in gui controls to update model via doValueAction_
	viewDoValueAction2_{|val,latency,send,toggle|
		value=val;
		this.refresh;
		if (model.notNil){ model.doValueAction2_(val,latency,send,toggle,this) };
		if (action2.notNil) {
			action2.value(this,latency,send,toggle)
		}{
			action.value(this,latency,send,toggle)
		};
	}
	
	// add an alternate action
	actions_{|key,action| actions[key]=action }
	
	// call an action
	valueActions{|key...args| actions[key].value(*args) }
	
	specialActions{|key...args|
		if (actions[key].notNil) { actions[key].value(    *args) };
		if (       model.notNil) { model.valueActions(key,*args) };
	}
	
	// used in lists and menus
	items_{|array| items=array }
	
	// you can't use System clock to call refresh
	refresh{
		if (view.notClosed) {
			// drop of if tab is hiddden			
			if ( (parent.isKindOf(MVC_TabView))and:{parent.isHidden} ) { ^this };
			// else
			view.refresh;
			if (numberGUI.notNil) { numberGUI.refresh }
		}
	}
	
	// override this refreh for any non SCView that needs a value update
	refreshValue{}
	
	// move gui, label and number also quant to grid
	moveBy{|x,y,button=0|
		var l,t,nx,ny,rect;
		var thisGrid, thisResize;
		
		l=view.bounds.left;
		t=view.bounds.top;
		
		// class or instance
		if (viewEditMode) {
			thisGrid=viewGrid;
			thisResize=viewEditResize;
		}{
			thisGrid=grid;
			thisResize=editResize;
		};
		
		if (button!=0) { thisResize = thisResize.not };
		
		if (thisResize) {
			if (lw.isNil) {lw=w;lh=h};
			if (isSquare) {
				w=(lw+x).clip(0,inf).round(thisGrid);
				h=(lh+y).clip(0,inf).round(thisGrid);
				w=w.clip(5,h);	
				h=h.clip(5,w);
			}{
				w=(lw+x).round(thisGrid).clip(5,inf);
				h=(lh+y).round(thisGrid).clip(5,inf);
			};
			rect=Rect(l,t,w,h);
		}{
			nx=(l+x).round(thisGrid);
			ny=(t+y).round(thisGrid);
			x=nx-l;
			y=ny-t;
			rect=Rect(nx.clip(0,inf),ny.clip(0,inf),w,h);
		};
		this.bounds_(rect);
		if (verbose)      { rect.postln };
		boundsAction.value(this,rect);
	}
	
}


+ SCView {

	free{
		if (this.notClosed) { this.remove };
		
		action = mouseDownAction = mouseUpAction = mouseOverAction = mouseMoveAction =
		keyDownAction = keyUpAction = keyTyped = keyModifiersChangedAction =
		beginDragAction = canReceiveDragHandler = receiveDragHandler = nil;
		
		onClose = nil; // check this doesn't stop on close
	
	}

}

