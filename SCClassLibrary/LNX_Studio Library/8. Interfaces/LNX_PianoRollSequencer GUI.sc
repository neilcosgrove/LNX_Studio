///////////////////////////////////////////////////////////////////////////////////////////////////
// the gui                                                                                      .

+ LNX_PianoRollSequencer{

	// calculate the rect of every note so it doesn't have to be done on every refresh
	calcNoteRects{
		noteRects=IdentityDictionary[];
		score.notesDict.pairsDo{|key,note|
			var l,t,w,h;
			if (note.enabled) {
				l=note.start*gridW;
				t=(127-note.note)*gridH;
				w=note.dur*gridW;
				h=1*gridH;
				noteRects[key]=Rect(
					(l+1).asInteger,t.asInteger,
					(w-1).clip(4,inf).asInteger,(h-1).clip(4,inf).asInteger);
			};
		}
	}

	// return the rect which covers the total area of the selected notes
	getSelectedAreaRect{
		var minX,maxX,minY,maxY;
		if (notesSelected.size>0) {
			notesSelected.do{|id|
				var note = this.note(id);
				if (minX.isNil) {
					minX=note.start;
					maxX=note.end;
					minY=maxY=note.note;
				}{
					if (note.start<minX) { minX=note.start};
					if (note.end  >maxX) { maxX=note.end  };
					if (note.note <minY) { minY=note.note };
					if (note.note >maxY) { maxY=note.note };
				};
			};
			^Rect.fromPoints(minX@minY,maxX@maxY);
		}{
			^nil;
		}
	}

	// refresh the note view with optional flags to recalulate note rects and update the velocity
	refresh{|calcNoteRects=true, refreshVel=true|
		if (gui[\notes].notNil) {
			if (calcNoteRects) {this.calcNoteRects}; // calc rects 1st, then draw
			if (thisThread.clock==SystemClock) {
				{
					lastVisibleRect=nil;
					gui[\notes].refresh;
					if (refreshVel) { gui[\vel].refresh };
				}.defer;
			}{
				lastVisibleRect=nil;
				gui[\notes].refresh;
				if (refreshVel) { gui[\vel].refresh };
			};
		}
	}

	refreshColors{ this.refresh(true,true) } // for color picker

	// set the position of the transport marker
	pos_{|x|
		pos =  x;
        if (lazyRefresh.notNil) {  lazyRefresh.lazyRefresh };
	}

	// gui call to enter fullscreen mode
	guiFullScreen{

		var bounds, colors, vo, sx, sy, lw, lh;

		colors = gui[\colors];
		bounds = Rect(5,10,gui[\bounds].width,gui[\bounds].height);
		gui2   = gui.copy;
		gui    = IdentityDictionary[];

		gui[\window2]=MVC_Window("Piano Roll",border:false).setInnerExtent(
			bounds.width+10,bounds.height+20).color_(\background,
			Color(59/77,59/77,59/77))
			.userCanClose_(true)
			.onClose_{
				this.exitFullScreenResize;
				//this.refresh;
			}
			.create.fullScreen;

		this.createWidgets(gui[\window2],bounds,colors, menuOld:true);
		this.refresh;

		// set grid size & orgin so it matches
		{
			sx = (gui[\window2].bounds.width)/(gui2[\window].bounds.width);  // scaleX by
			sy = (gui[\window2].bounds.height)/(gui2[\window].bounds.height); // scaleY by
			vo = gui2[\scrollView].visibleOrigin;      // get the visible Origin
			lw = models[\gridW].value;                 // store last width
			lh = models[\gridH].value;                 // store last height
			models[\gridW].multipyValueAction_(sx);    // now scale the grid width
			models[\gridH].multipyValueAction_(sy);    // and scale the grid height
			sx = models[\gridW].value / lw;            // new scaleX incase grid width constrained
			sy = models[\gridH].value / lh;            // new scaleX incase grid width constrained
			gui[\scrollView].visibleOrigin_( (vo.x * sx) @ (vo.y * sy) ); // now set new origin
		}.defer(0.1); // defer because gui[\window2].fullScreen doesn't update bounds immediately

	}

	// on exit fullscreen resize back into orginal scrollview
	exitFullScreenResize{
		var vo, sx, sy, lw, lh, guiSwap;

		guiSwap = gui;
		gui = gui2;
		gui2 = guiSwap;

		sx = (gui[\window].bounds.width)/(gui2[\window2].bounds.width);  // scaleX by
		sy = (gui[\window].bounds.height)/(gui2[\window2].bounds.height); // scaleY by
		vo = gui2[\scrollView].visibleOrigin;      // get the visible Origin
		lw = models[\gridW].value;                 // store last width
		lh = models[\gridH].value;                 // store last height
		models[\gridW].multipyValueAction_(sx);    // now scale the grid width
		models[\gridH].multipyValueAction_(sy);    // and scale the grid height
		sx = models[\gridW].value / lw;            // new scaleX incase grid width constrained
		sy = models[\gridH].value / lh;            // new scaleX incase grid width constrained
		gui[\scrollView].visibleOrigin_( (vo.x * sx) @ (vo.y * sy) ); // now set new origin

		gui2 = nil;

	}

	// gui call to exit full screen. uses onClose to call exitFullScreenResize method
	guiExitFullScreen{ gui[\window2].close }

	// the gui widgets
	createWidgets{|window, bounds, argColors, velocityOffset=0, menuOld=false, parentViews|

		var visibleOrigin=0@0, visibleRect=bounds, buttonPressed;

		var velButtonPressed;

		var startX,startY,lastX,lastY,lastMX,lastMY, selectRect, lastVisX;

		var startVelY;

		var selectArea, xScale=1, yScale=1;

		var noDash     = FloatArray[];
		var gridDash1  = FloatArray[2,3];
		var selectDash = FloatArray[3,3];
		var markerDash = FloatArray[4,4];

		var bCount=0;

		var sb = ScrollBars.addIfSome(13);

		gui[\window] = window;

		gui[\velocityOffset] = velocityOffset;

		gui[\bounds] = bounds;

		gui[\colors] = argColors;

		if (gui[\notes].isNil) { // just a quick test to remind me about mvc violation

			this.calcNoteRects;

			// the colors
			colors=(
				\background:  Color(0.3,0.3,0.5,0.25),
				\background2: Color(0,0,0.5,0.25),
				\background3: Color(0,0.5,0.25),
				\buttons:     Color(0.6,0.6,0.9),
				\boxes:       Color(11/53,79/178,41/77,45/77)/3,
				\grid:        Color(1,1,1,0.25),
				\grid2:       Color(0,0,0,0.5),
				\noteBG:      Color.orange,
				\noteB:       Color(0,0.0,0),
				\noteBGS:     Color.orange*1.5,
				\noteBS:      Color(1,1,0.9),
				\selectRect:  Color(0,0,0),
				\selectArea:  Color(1,1,1,0.2),
				\durDiv:      Color(0,0,0,0.5),
				\record:      Color(1,0,0),
				\velocity:    Color.orange,
				\velocityBG:  Color(0.08,0.27,0.37)*0.7,
				\velocitySel: Color.orange*1.78,
				\marker:	  Color(1,1,1,0.5)
			)++(argColors?IdentityDictionary[]);

			gui[\buttonTheme]=(
						\rounded_ 		: true,
						\canFocus_		: false,
						\colors_  	    : (\on:colors[\buttons],\off:colors[\buttons]));

			gui[\nBoxTheme]=(
						\orientation_ 	: \horiz,
						\rounded_		: true,
						\font_			: Font("Helvetica", 10),
						\colors_      	: (\focus:Color.clear, \string:Color.white,
									   \typing:Color.yellow, \background: colors[\boxes]));

			gui[\snap]=MVC_OnOffRoundedView(window,
				Rect(bounds.left+60+6, bounds.top-6, 39, 18),"Snap")
				.value_(1)
				.action_{|me| snapToGrid=me.value.isTrue; this.refresh(false) }
				.rounded_(true)
				.canFocus_(false)
				.color_(\on,colors[\buttons])
				.color_(\off,colors[\buttons]/2);

			gui[\record]=MVC_OnOffRoundedView(models[\record], window,
						Rect(bounds.left+98+7+6, bounds.top-6, 35, 18),"Rec")
				.action_{recordFocusAction.value}
				.rounded_(true)
				.canFocus_(false)
				.color_(\on,colors[\record])
				.color_(\off,colors[\buttons]/2);

			if (menuOld) {

				gui[\speed]=MVC_PopUpMenu(models[\speed], window,
					Rect(bounds.right-202, bounds.top-5, 39, 15))
					.font_(Font("Helvetica", 10))
					.color_(\background,colors[\boxes])
					.color_(\string,Color.white)
					.resize_(3);

				gui[\menu]=MVC_PopUpMenu(window,
					Rect(bounds.left, bounds.top-6+1, 60, 16))
					.items_(["Edit","-","Copy","Paste",
							"Select All","-","Quantise","-","Delete",
							"-","Fit to window","Exit Full Screen"])
					.color_(\background,colors[\boxes])
					.color_(\string,Color.white)
					.canFocus_(false)
					.font_(Font("Helvetica", 12))
					.action_{|me|
						switch (me.value.asInt)
							{ 2} {this.guiCopy }
							{ 3} {this.guiPaste }
							{ 4} {this.guiSelectAll }
							{ 6} {this.quantise(notesSelected) }
							{ 8} {this.guiDelete }
							{ 10} {this.fitToWindow }
							{ 11} {
								if (gui2.isNil) {
									this.guiFullScreen;
								}{
									this.guiExitFullScreen;
								};
							};
						me.value_(0);
					};
			}{

				gui[\speed]=MVC_PopUpMenu3(models[\speed], window,
					Rect(bounds.right-202, bounds.top-5, 39, 15))
					.font_(Font("Helvetica", 10))
					.color_(\background,colors[\boxes])
					.color_(\string,Color.white)
					.resize_(3);

				gui[\menu]=MVC_PopUpMenu3(window,
					Rect(bounds.left, bounds.top-6+1, 60, 16))
					.items_(["Copy","Paste","Select All","-","Quantise","-","Delete",
							"-","Fit to window","Full Screen"])
					.color_(\background,colors[\boxes])
					.color_(\string,Color.white)
					.canFocus_(false)
					.font_(Font("Helvetica", 12))
					.staticText_("Edit")
					.showTick_(false)
					.action_{|me|
						switch (me.value.asInt)
							{ 0} {this.guiCopy }
							{ 1} {this.guiPaste }
							{ 2} {this.guiSelectAll }
							{ 4} {this.quantise(notesSelected) }
							{ 6} {this.guiDelete }
							{ 8} {this.fitToWindow }
							{ 9} {
								if (gui2.isNil) {
									this.guiFullScreen;
								}{
									this.guiExitFullScreen;
								};
							};
					};

			};

			// ZOOM

			gui[\yZoomDec]=MVC_OnOffRoundedView(window,gui[\buttonTheme],
				Rect(bounds.left+179, bounds.top-6+1, 20, 16),"-")
				.orientation_(\horiz)
				.action_{|me| models[\gridH].multipyValueAction_(1/1.4) };

			gui[\yZoomInc]=MVC_OnOffRoundedView(window,gui[\buttonTheme],
				Rect(bounds.left+204, bounds.top-6+1, 20, 16),"+")
				.action_{|me| models[\gridH].multipyValueAction_(1.4) };

			gui[\jump]=MVC_OnOffRoundedView(window,gui[\buttonTheme],
				Rect(bounds.left+152, bounds.top-6, 20, 18))
				.strings_([\return])
				.mode_(\icon)
				.action_{|me|
					this.releaseAll;
					studio.guiJumpTo( (marker*3*(score.speed)).round(3) );
				}
				.action2_{ if (studio.isPlaying.not) {studio.guiPlay(1) }{ studio.guiStop} };

			gui[\xZoomDec]=MVC_OnOffRoundedView(window,gui[\buttonTheme],
				Rect(bounds.right-229+30-50-5-4, bounds.top-6+1, 20, 16),"-")
				.orientation_(\horiz)
				.action_{|me| models[\gridW].multipyValueAction_(1/1.4) }
				.resize_(3);

			gui[\xZoomInc]=MVC_OnOffRoundedView(window,gui[\buttonTheme],
				Rect(bounds.right-229+30+25-50-5-4, bounds.top-6+1, 20, 16),"+")
				.action_{|me| models[\gridW].multipyValueAction_(1.4) }
				.resize_(3);

			// grid stuff

			gui[\quantiseStep]=MVC_NumberBox(models[\quantiseStep], window, gui[\nBoxTheme],
				Rect(bounds.right-160+5+6+1, bounds.top-6+1, 39, 16))
				.label_(": ")
				.resoultion_(1)
				.resize_(3);

			gui[\bars]=MVC_NumberBox(models[\bars], window, gui[\nBoxTheme],
				Rect(bounds.right-160+55+5+6, bounds.top-6+1, 39, 16))
				.label_("x")
				.resoultion_(1)
				.resize_(3);

			gui[\dur]=MVC_NumberBox(models[\dur], window, gui[\nBoxTheme],
				Rect(bounds.right-40, bounds.top-6+1, 39, 16))
				.label_("=")
				.resoultion_(80)
				.resize_(3);

			// notes can scroll
			gui[\scrollView] = MVC_ScrollView(window,bounds.resizeBy(0,-50).moveBy(0,15))
				.hasVerticalScroller_(true)
				.hasHorizontalScroller_(true)
				.autoScrolls_(true)
				.resize_(5)
				.action_({ this.refresh(true, false) })
			;

	// I'm going to remove tabs to make it easier to code !!! /////////////////////////////////////

	gui[\notes] = MVC_UserView(gui[\scrollView],
			Rect(0,0,gridW*(score.dur),gridH*128).resizeBy(sb.neg))

		.addParentView(parentViews)

		.clearOnRefresh_(false) //  ******** MUST BE TRUE NOW (memory leak when moving notes)

	// DRAW /////////////////////////////////////////////////////////////////

	.drawFunc_{|me|
		var bounds, w, h, svb, x1, x2, voL,voR, voT, voB, vBars, vNum;
		var spo = this.spo;
		var noRect=0;

		svb=gui[\scrollView].bounds.resizeBy(sb.neg);
		visibleOrigin=gui[\scrollView].visibleOrigin;
		voL = visibleOrigin.x;
		voR = voL+svb.width+sb;
		voT = visibleOrigin.y;
		voB = voT+svb.height+sb;

		visibleRect=Rect(voL,voT,svb.width+sb,svb.height+sb);

		score.viewArgs_(gridW,gridH,voL,voT);

		//me.frameRate.postln;
		//if (	lastVisibleRect!=visibleRect ) {

		bounds = me.bounds;
		w = bounds.width+sb;
		h = bounds.height+sb;

		xScale = 1;
		if (gridW<24) {xScale=2};
		if (gridW<12) {xScale=4};
		if (gridW<6)  {xScale=8};
		if (gridW<3)  {xScale=16};
		if (gridW<1.5) {xScale=32};
		if (gridW<1.25) {xScale=64};
		if (gridW<1.125) {xScale=128};

		xScale = xScale.roundFloor(quantiseStep).clip(quantiseStep,inf);
		vBars  = bars * quantiseStep;
		vNum   = 4.roundFloor(quantiseStep).clip(quantiseStep,inf);

		if (spo==12) {
			yScale=1;
			if (gridH<12) {yScale=2};
			if (gridH<6)  {yScale=4};
			if (gridH<3)  {yScale=6};
			if (gridH<2)  {yScale=12};
		}{
			yScale=1;
		};

		x1=(voL/(gridW*xScale)).floor;
		x2=(voR/(gridW*xScale)).ceil;
		x2=x2-x1+1; // now width here

		Pen.use{
			// background
			Pen.smoothing_(false);
			colors[\background].alpha_(1).set;
			Pen.fillRect(Rect(0,0,w,h));

			colors[\grid].set;
			// grid (horiz)
			(h.div(gridH*yScale)+1).do{|y|
				if ((y*yScale)%spo==0) {
					Pen.stroke;
					colors[\grid2].set;
				}{
					Pen.stroke;
					colors[\grid].set;
				};
				Pen.line(voL@(y*gridH*yScale),voR@(y*gridH*yScale));
			};
			Pen.stroke;

			Pen.font_(Font("Helvetica",10));

			// grid (vert)
			if (snapToGrid.not) { Pen.lineDash_(gridDash1) };
			(x2).asInt.do{|x| x=x+x1;
				if ((x*xScale)%vBars==0) {
					//Pen.stroke;
					colors[\grid2].set;
				}{
					colors[\grid].set;
				};
				Pen.line((x*gridW*xScale)@voT,(x*gridW*xScale)@voB);
				Pen.stroke;

				//
				if ((x*xScale)%(xScale*vNum)==0) {

					Pen.fillColor_(Color.black);
					Pen.stringCenteredIn((x*xScale+1).asString,

						Rect.aboutPoint((x*gridW*xScale+11)@(voT+7),5,5)
					);
				};

			};

			// draw the selected area when moving
			if ((selectArea.notNil)and:{notesSelected.size>1}) {
				colors[\selectArea].set;

				Pen.fillRect(Rect( selectArea.left*gridW, (127-selectArea.top+1)*gridH,
						selectArea.width*gridW, (selectArea.height.neg-1)*gridH ));
			};

			// notes
			noteRects.pairsDo{|key,rect|
				var dur;
				if (rect.intersects(visibleRect)){

					Pen.lineDash_(noDash);

					if (notesSelected.includes(key)) {
						colors[\noteBS].set;
						Pen.fillRect(rect);
						colors[\noteBGS].set;
						Pen.fillRect(rect.insetBy(1,1));
					}{													colors[\noteB].set;
						Pen.fillRect(rect);
						colors[\noteBG].set;
						Pen.fillRect(rect.insetBy(1,1));
					};

					Pen.lineDash_(selectDash);
					colors[\durDiv].set;
					dur=(rect.right)-((rect.width*0.25));//.clip(0,gridW));
					Pen.line(dur@(rect.top+2), dur@(rect.bottom-1));
					Pen.stroke;

					noRect = noRect + 1;
				};
			};

			MVC_LazyRefresh.incRefreshN(noRect/10);

			// the selction rect
			if (selectRect.notNil) {
				Color(1,1,1,0.065).set;
				Pen.fillRect(selectRect);
				colors[\selectRect].set;
				Pen.lineDash_(selectDash);
				Pen.strokeRect(selectRect);
			};

		};

		lastVisibleRect=visibleRect;

		// refresh the velocity is the view has move along the x-axis
		if (lastVisX!=visibleOrigin.x) {
			{gui[\vel].refresh}.defer(0.01);
			lastVisX=visibleOrigin.x;
		};

		//}

	};

	// DOWN /////////////////////////////////////////////////////////////////

	// this is the transport position marker which is on top of the notes view
	// and its this view that actually recieves all the mouse actions

	gui[\notesPosAndMouse] = MVC_UserView(gui[\scrollView],
		Rect(0,0,gridW*(score.dur),gridH*128).resizeBy(sb.neg))
		.canFocus_(true)

		.addParentView(parentViews)

		.clearOnRefresh_(true)
		.onClose_{ lastVisibleRect=nil }
		.drawFunc_{|me|

			var visibleOrigin, svb, voT, voB;
			visibleOrigin=gui[\scrollView].visibleOrigin;
			svb=me.bounds;
			voT = visibleOrigin.y;
			voB = voT+svb.height;
			Pen.use{
				Pen.smoothing_(false);
				Color.white.set;
				Pen.line((pos*gridW)@(voT),(pos*gridW)@voB);
				Pen.stroke;

				Pen.lineDash_(markerDash);
				colors[\marker].set;
				Pen.line((marker*gridW)@(voT),(marker*gridW)@voB);
				Pen.stroke;
			};
		}

	.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
		//mods256:none,131330:shift,8388864:func,262401:ctrl,524576:alt,1048840:apple
		var val, minX,maxX,minY,maxY;
		var noteSelected, downNoteRect;

		MVC_LazyRefresh.mouseDown;

		if (modifiers==131330)  { buttonNumber=1 };
		if (modifiers==8388864) { buttonNumber=1 };
		if (modifiers.isCtrl)  { buttonNumber=1 };
		if (modifiers.isAlt)  { buttonNumber=1 };
		//if (modifiers.isAlt) { buttonNumber=2  }; // i've changed alt to button 2 here

		buttonPressed=buttonNumber;

		mouseMode=nil;
		startX=x;
		startY=y;
		lastX=x;
		lastY=y;

		selectRect=nil;

		// find if mouse down is in a note rect
		noteRects.pairsDo{|key,rect|
			if (rect.contains(x@y)){
				noteSelected=key;
			}
		};

		// if it is
		if (noteSelected.notNil) {

			// clear selection only if not includes and button 0
			if ((notesSelected.includes(noteSelected).not)and:{(buttonPressed==0)}) {
				notesSelected.clear;
			};

			// add this note to selection
			notesSelected=notesSelected.add(noteSelected);
			downNoteRect=noteRects[noteSelected];

			// which part of the note is it in?
			if (x>((downNoteRect.right)-((downNoteRect.width*0.25)))) {
				mouseMode=\dur;
			}{
				if (buttonPressed==1) {
					mouseMode=\copy;
					copyIDs = notesSelected.copy;
					this.requestIDs(copyIDs.size);
					notesSelected.clear;
					selectArea=nil;
				}{
					mouseMode=\move;
					// get the rect which covers the total area of the selected notes
					selectArea=this.getSelectedAreaRect;
				};
			};
		}{
			// is down a note
			if (buttonPressed==0) {
				notesSelected.clear;
				selectArea=nil;
			};

		};

		// if so store start pos for a move
		if ((mouseMode==\move)||(mouseMode==\copy)||(mouseMode==\dur)) {
			lastMX=x/gridW;
			if (snapToGrid) { lastMX = lastMX.roundFloor(quantiseStep) };
			lastMY=127-(y.div(gridH));
		};

		// add a note on double click
		if ((mouseMode.isNil)&&(clickCount==2)) {
			x=(x/gridW);
			y=127-(y.div(gridH));
			if (snapToGrid) { x=x.roundFloor(quantiseStep) };
			this.addNote(y,x)
		}{
			this.refresh(false);
		};
	}

	// MOVE /////////////////////////////////////////////////////////////////

	.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
		var mx,my,dx,dy;

		var minX,maxX,minY,maxY;

		// do a select rect
		if ((mouseMode==\select)or:{mouseMode==nil}) {

			selectRect=Rect.fromPoints(startX@startY,x@y);

			if (buttonPressed==0) { notesSelected.clear };

			noteRects.pairsDo{|key,rect|
				if (rect.intersects(selectRect)){
					notesSelected=notesSelected.add(key);
				};
			};

			if (((startX-x).abs>2)or:{(startY-y).abs>2}) { mouseMode=\select };
			if (notesSelected.size>0) { mouseMode=\select };

			this.refresh(false,true); // only refresh
		};

		// move notes
		if (mouseMode==\move) {
			// covert from pixels to grid (use quantiseStep if needed)
			mx=x/gridW;
			if (snapToGrid) { mx=mx.roundFloor(quantiseStep)};
			my=(127-(y.div(gridH)));

			// dx & dy of movement
			dx=mx-lastMX;
			dy=my-lastMY;

			dx=dx-(selectArea.left+dx).clip(-inf,0); // clip movement to left edge
			//dx=dx+score.dur-(selectArea.right+dx).clip(score.dur,inf); // clip movement to right
			dy=dy-(selectArea.top+dy).clip(-inf,0); // clip movement to bottom edge
			dy=dy+127-(selectArea.bottom+dy).clip(127,inf); // clip movement to right edge

			if ((dx!=0)||(dy!=0)){
				selectArea = selectArea.moveBy(dx,dy);
				this.moveNotesBy(dx,dy,notesSelected);
			};
			lastMX=mx;
			lastMY=my;
		};

		// make a copy
		if (mouseMode==\copy) {
			if (requestID.notNil) {

				// copy the notes and use in selection
				score.makeCopy(copyIDs.asList[0..(requestN-1)],requestID,requestN);
				notesSelected = notesSelected.addAll((requestID..(requestID+requestN-1)));
				requestID=requestN=nil;

				// get selected notes area
				notesSelected.do{|id|
					var note = this.note(id);
					if (minX.isNil) {
						minX=note.start;
						maxX=note.end;
						minY=maxY=note.note;
					}{
						if (note.start<minX) { minX=note.start};
						if (note.end  >maxX) { maxX=note.end  };
						if (note.note <minY) { minY=note.note };
						if (note.note >maxY) { maxY=note.note };
					};
				};
				selectArea=Rect.fromPoints(minX@minY,maxX@maxY);

				mouseMode=\move; // use move next time

				selectArea = selectArea.moveBy(0,0);
				this.moveNotesBy(0,0,notesSelected);
			};
		};

		// move dur
		if (mouseMode==\dur) {
			mx=x/gridW;
			if (snapToGrid) { mx=mx.roundFloor(quantiseStep) };
			if ((mx-lastMX)!=0){
				this.moveDurBy(mx-lastMX,notesSelected);
			};
			lastMX=mx;
		};

	}

	// UP /////////////////////////////////////////////////////////////////

	.mouseUpAction_{|me, x, y, modifiers, buttonNumber, clickCount|

		MVC_LazyRefresh.mouseUp;

		if (mouseMode==\select){ selectRect=nil }; // remove the select rect

		// set the marker
		if (mouseMode==nil) {
			selectRect=nil;
			marker=x/gridW;
			if (snapToGrid) { marker=marker.roundFloor(quantiseStep)};
		};

		// move notes
		if ((mouseMode==\move)||(mouseMode==\dur)) {
			selectArea=nil;
			this.useAdjustments;
		};
		this.refresh(true);

	}

	// Keydown /////////////////////////////////////////////////////////////////

	.keyDownAction_{|me, char, modifiers, unicode, keycode, key|
		// [char, modifiers, unicode, keycode, key].postln;
		// kc 51=delete, 36=return, 126,125,123,124=up,down,left,right
		// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple

		// bCount=bCount+1;

		if (modifiers.isXCmd) {
			if (key.isAlphaKey(\C)) {this.guiCopy }; // copy
			if (key.isAlphaKey(\V)) {this.guiPaste}; //paste
			if (key.isAlphaKey(\A)) {this.guiSelectAll};
		};

		if (key.isDel) { this.guiDelete};

		keyDownAction.value(this, char, modifiers, unicode, keycode, key);

	}
	.keyUpAction_{|me, char, modifiers, unicode, keycode, key|
		keyUpAction.value(this, char, modifiers, unicode, keycode);
	}

	// drag and drop midi files /////////////////////////////////////////////////////////////////

	.canReceiveDragHandler_{
		(View.currentDrag.isArray)and:{View.currentDrag[0].isString}
	}

	/*
	f = SimpleMIDIFile.read("/Users/neilcosgrove/Desktop/xmas/wonderful_christmas_time.mid");
	f.tempoMap
	f = SimpleMIDIFile.read("/Users/neilcosgrove/Desktop/xmas/chords.mid");
	f.usedTracks.collect{|t| t.asString ++"."+  f.trackName(t).stripRTF };

	*/

	.receiveDragHandler_{
		var file;
		var tempoAdjust=24;
		// is the 1st item a string?
		if ((View.currentDrag.isArray)and:{View.currentDrag[0].isString}) {
			// try opening path as a midi file
			{file = SimpleMIDIFile.read( View.currentDrag[0] ) }.try;
			if (file.notNil) {

				// make a window
				var win=MVC_Window("Import track")
					.setInnerExtent(200,320)
					.color_(\background, Color.grey(0.2))
					.alwaysOnTop_(true)
					.create;

				// and show which tracks we can import
				MVC_ListView2(win,Rect(10,10,180,275))
					.items_(file.usedTracks.collect{|t|
						t.asString ++"."+  (file.trackName(t)?"").stripRTF })
					.actions_(\upDoubleClickAction,{|me|
						file.asNoteDicts(track:(file.usedTracks[me.value])).collect{|note|
							[note[\note],
							note[\absTime]/tempoAdjust,
							note[\dur]/tempoAdjust,
							note[\velo]/127]
						}.do{|note|
							this.addNote(*note);
						};
						win.close;
					});

				// scale the tempo by this
				MVC_NumberBox(win,Rect(110,291,50,18))
					.orientation_(\horizontal)
					.labelShadow_(false)
					.label_("Scale tempo")
					.value_(tempoAdjust)
					.action_{|me|
						tempoAdjust=me.value;
					};
			};
		}
	};

	///////////////////////////////////////////////////////////////////////////////////////////////

	// velocity /////////////

	gui[\vel] = MVC_UserView(window,Rect(bounds.left,
		bounds.top+bounds.height-25+2-10+velocityOffset,bounds.width,35))
		.resize_(8)
		.drawFunc_{|me|
			var bounds,w,h,vox,xos;
			bounds = me.bounds;
			w = bounds.width;
			h = bounds.height-7;
			visibleOrigin=gui[\scrollView].visibleOrigin;
			vox = visibleOrigin.x;

			xos = vox%gridW;

			Pen.use{

				// background
				Pen.smoothing_(false);
				colors[\velocityBG].set;
				Pen.fillRect(Rect(0,0,w,h+7));

				Pen.smoothing_(true);
				Pen.width_(1.25);

				// notes
				noteRects.pairsDo{|key,rect|

					var x=rect.left-vox; // x is pos on vel view
					var y=(1-score.note(key).vel)*h+3.5;
					var p1=x@y;

					if ((x>=0)&&(x<=w)) {

						if (notesSelected.includes(key)) {
							colors[\velocitySel].set
						}{
							colors[\velocity].set
						};

						Pen.line(p1, x@(h+7));
						Pen.stroke;
						Pen.fillOval(Rect.aboutPoint(p1,3.5,3.5));

						colors[\velocityBG].set;
						Pen.fillOval(Rect.aboutPoint(p1,2,2));
					};
				};

			}
		}
		.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			//mods256:none,131330:shift,8388864:func,262401:ctrl,524576:alt,1048840:apple
			var val, minX,maxX,minY,maxY;
			var noteSelected, downNoteRect, keyDown;

			var bounds,w,h,vox,xos, p;

			if (modifiers.isAlt) { buttonNumber=1  };
			if (modifiers.isCtrl) { buttonNumber=2  };
			velButtonPressed=buttonNumber;

			bounds = me.bounds;
			w = bounds.width;
			h = bounds.height;
			visibleOrigin=gui[\scrollView].visibleOrigin;
			vox = visibleOrigin.x;

			p=x@y;

			// notes
			noteRects.pairsDo{|key,rect|
				var vx=rect.left-vox; // x is pos on vel vie
				var vy=(1-score.note(key).vel)*h;
				var r=Rect.aboutPoint(vx@vy,5,5);
				if (r.containsPoint(p)) {

					keyDown=key;
				};
			};

			startVelY=y;
			velSelected=nil;

			if ((notesSelected.notNil) and: {notesSelected.includes(keyDown)}) {
				velSelected=notesSelected.copy;
			}{
				if (keyDown.notNil) { velSelected=[keyDown] };
			};

		}
		.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
			//mods256:none,131330:shift,8388864:func,262401:ctrl,524576:alt,1048840:apple;
			if (velSelected.notNil) {
				this.adjustVel((startVelY - y)/(me.bounds.height)*0.333,velSelected);
			};
			startVelY = y;
		}


	///////////////////////////////////////////////////////////////////////////////////////////////

		};

        lazyRefresh.refreshFunc_{ if (gui.notNil) { gui[\notesPosAndMouse].refresh } };

	}

	guiSelectAll{
		notesSelected = noteRects.keys;
		this.refresh;
	}

	guiDelete{
		if (notesSelected.size>0) {
			this.deleteNotes(notesSelected.asList);
		};
	}

}

+ Rect {
	containsPointPR { arg aPoint;
		^((aPoint.x>=left) and: {aPoint.x<(left + width) } // difference is with x<(left+width)
			and: {aPoint.y>=top} and: {aPoint.y<=(top + height) })
	}
}

	