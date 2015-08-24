/******* by jostM June11, 2009 version 1.25 *******/
TabbedView {
	var labels,
		labelColors,
		unfocusedColors,
		backgrounds,
		stringColor,
		stringFocusedColor,
		<>focusActions,
		<>unfocusActions,
		<focusFrameColor,
		<>unfocusTabs=false,
		tabWidth = \auto,
		tabWidths,
		scroll=false,
		<tabHeight=\auto,
		tbht,
		tabCurve = 8,
		<tabViews,
		<font,
		<views,
		<resize = 1,
		<tabPosition = \top,
		<followEdges=true,
		<activeTab = 0,
		focusHistory = 0,
		<labelPadding = 20,
		left=0,
		top=0,
		<>swingFactor,
		<view,
		<>onClose,
		<>action,
		<rect,
		<offset,
		<window,
		<windowBounds;
		
	isClosed { ^view.isClosed }
	notClosed { ^view.notClosed }
	
	remove{ view.remove }
	
	*new{ arg parent, bounds, labels, colors, name=" ", scroll=false, offset;
		^super.new.init(parent, bounds, labels, colors, name, scroll, offset );
	}
	
	init{ arg parent, bounds, lbls, colors, name, scr, argOffset ;
		
		offset = argOffset ? (0@0);
		
		window=parent;
		window.isNil.if{ window = GUI.window.new(name,bounds).front;
			bounds = window.view.bounds; 
			resize = 5};
			
		//must be written this way, or nested views don't work with a nil bounds.
		bounds.isNil.if{
					bounds = window.asView.bounds.moveTo(0,0);
		};
		bounds=bounds.asRect;
		
		
		windowBounds = bounds.moveBy(2,5);
		
		//if (window.isKindOf(SCWindow)) { windowBounds=windowBounds.moveTo(0,0) };
		
		
		view = GUI.compositeView.new(window,bounds).resize_(resize);
		lbls= lbls ? ["tab1","tab2","tab3"];
		scroll=scr;
		labels = [];
		focusActions = [];
		unfocusActions = [];
		focusFrameColor=Color.clear;
		font=GUI.font.default;		
		stringColor = Color.black;
		stringFocusedColor = Color.white;
		swingFactor=Point(0.52146,1.25); 		
		if( GUI.id === \cocoa)  {
			labelColors = colors ? [Color.grey.alpha_(0.2)];
			}{
			labelColors = colors ? [Color(0.85,0.85,0.85)];
			unfocusTabs=true; // unfocus Tabs if not Cocoa;
			};
		unfocusedColors = Array.fill(labelColors.size,{arg i;
			var col;
			col = labelColors[i%labelColors.size].asArray;
			if( GUI.id === \cocoa)  
				{col = col*[0.7,0.7,0.7,1];}
				{col = col*[0.9,0.9,0.9,1];};
			col = Color(*col);
				});
		backgrounds = labelColors;
		
		tabViews = [];
		views = [];
		
		tabWidths = []; 
		
		lbls.do{arg label,i;
			this.add(label);
			
		};
		this.focus(0);
		^this;
	}
	
	bounds{ ^view.bounds }
	
	add { arg label,index; //actually this is an insert method with args backwards
		var tab, container, calcTabWidth, i;
		
		index = index ? labels.size; //if index is nil, add it to the end
		i=index;
		labels=labels.insert(i,label.asString);
		i = labels.size-1;
		
		label=label.asString; //allows for use of symbols as arguments
		
//		if (tabWidth == \auto) //overwrite tabWidths if autowidth
//			{ calcTabWidth=label.bounds.width + labelPadding }
//			{ calcTabWidth = tabWidth };
			
		tabWidths=tabWidths.insert(index,50);	
		
		tab = GUI.userView.new(window); //bounds are set later
		if (index==0) {
			tab.onClose_{onClose.value(this)}
		};
		tab.enabled = true;
		tab.focusColor=focusFrameColor;
		tab.mouseDownAction_({
			this.focus(i);
			unfocusTabs.if{tab.focus(false)}; 
		});
		tabViews = tabViews.insert(index, tab);
		
		// added by lnx
		if (scroll.isCollection) {
			scroll[index].if{container = GUI.scrollView.new(view).resize_(5)}
			{container = GUI.compositeView.new(view,view.bounds).resize_(5)}; //bounds are set 
			
		}{
		
			scroll.if{container = GUI.scrollView.new(view).resize_(5)}
			{container = GUI.compositeView.new(view,view.bounds).resize_(5)}; //bounds are set later
		};
		
		
		container.background = backgrounds[i%backgrounds.size];
		
		
		views = views.insert(index,container);
		
		focusActions = focusActions.insert(index,{});
		unfocusActions = unfocusActions.insert(index,{});
		tabViews.do{ arg tab, i;
			tab.mouseDownAction_({
				this.focus(i);
				unfocusTabs.if{tab.focus(false)}; 
			});
			tab.canReceiveDragHandler_({
				this.focus(i);
				unfocusTabs.if{tab.focus(false)}; 
			});
		};
		this.updateViewSizes();
		^this.views[index];

	}
		
	
	insert{ arg index,label;
		^this.add(label,index);
	}
	
	
	/** this paints the tabs with rounded edges **/
	paintTab{ arg tabLabelView,label = "label", background, strColor;
			var drawCenter,drawLeft,drawTop,drawRect,drawRight,
					drawBottom,rotPoint,moveBy,rotPointText,drawRectText,
					drawRectText2,rot1=pi/2,rot2=0;	
		followEdges.if{
			switch(tabPosition)
				{\top}{rot1=0;rot2=0}
				{\left}{rot1=pi;rot2=pi/2}
				{\bottom}{rot1=pi;rot2=pi}
				{\right}{rot1=0;rot2=pi/2};
			}{
			switch(tabPosition)
				{\top}{rot1=0;rot2=pi/2.neg}
				{\left}{rot1=pi;rot2=pi}
				{\bottom}{rot1=pi;rot2=pi/2}
				{\right}{rot1=0;rot2=0};
			};
		
		tabLabelView.canFocus_(false); // added by lnx
	
		tabLabelView.drawFunc = { arg tview;
			var drawCenter,drawLeft,drawTop,drawRect,drawRight,
					drawBottom,rotPoint,moveBy,rotPointText,drawRectText,drawRectText2;
					
			//rect=view.bounds; // hacky fix
			
			drawRect= tview.bounds.moveTo(0,0);
			drawCenter=Point(drawRect.left+(drawRect.width/2),drawRect.top+(drawRect.height/2));
			
			([\top,\bottom].occurrencesOf(tabPosition)>0).if{
				drawRectText=Rect(drawRect.left-((drawRect.height-drawRect.width)/2),
					drawRect.top+((drawRect.height-drawRect.width)/2),drawRect.height,drawRect.width);
			}{drawRectText=drawRect};
			
			([\right,\left].occurrencesOf(tabPosition)>0).if{
				drawRectText2=Rect(drawRect.left-((drawRect.height-drawRect.width)/2),
					drawRect.top+((drawRect.height-drawRect.width)/2),drawRect.height,drawRect.width);
			}{drawRectText2=drawRect};
			
			drawLeft=drawCenter.x-(drawRect.width/2);
			drawTop=drawCenter.y-(drawRect.height/2);
			drawRight=drawCenter.x+(drawRect.width/2);
			drawBottom=drawCenter.y+(drawRect.height/2);
			GUI.pen.use{
				GUI.pen.rotate(rot1,drawCenter.x,drawCenter.y);
					GUI.pen.width_(1);
				GUI.pen.color_(background);
				([\top,\bottom].occurrencesOf(tabPosition)>0).if{
					GUI.pen.addWedge( (drawLeft + tabCurve)@(drawTop + tabCurve),
						tabCurve, pi, pi/2);
					GUI.pen.addWedge( (drawRight - tabCurve)@(drawTop + tabCurve),
						tabCurve, 0, (pi/2).neg);
						
					GUI.pen.addRect( Rect(drawLeft + tabCurve, 
								drawTop,
								drawRect.width - tabCurve - tabCurve, 
								tabCurve) 
								);
					GUI.pen.addRect( Rect(drawLeft, 
								drawTop+tabCurve,
								drawRect.width,
								drawRect.height-tabCurve)
							);
				}{
					GUI.pen.addWedge( (drawLeft+drawRect.width - tabCurve)
						@(drawTop + tabCurve),
						tabCurve, -pi/2, pi/2);
					GUI.pen.addWedge( (drawLeft+drawRect.width - tabCurve)
						@(drawTop + drawRect.height - tabCurve),
						tabCurve, 0, pi/2);
						
					GUI.pen.addRect( Rect(drawLeft+drawRect.width - tabCurve , 
								drawTop + tabCurve,
								tabCurve, 
								drawRect.height - tabCurve - tabCurve) 
								);
					GUI.pen.addRect( Rect(drawLeft, 
								drawTop,
								drawRect.width-tabCurve,
								drawRect.height)
							);
				};
				GUI.pen.fill;

				GUI.pen.rotate(rot2,drawCenter.x,drawCenter.y);
				GUI.pen.font_(font);
				GUI.pen.color_(strColor);
				
				//Pen.setShadow(0@0.neg, 5, Color.white.alpha_(1));

				followEdges.if{
 					GUI.pen.stringCenteredIn(label, 
 						drawRectText2.moveBy(0,if(tabPosition==\top){1}{0};));
 					}{
 					GUI.pen.stringLeftJustIn(label, 
 						drawRectText.insetAll((labelPadding/2)-2,0,0,0).moveBy(0,1));
 				};
			};
		
		};
		tabLabelView.refresh;
	}
	
	

	updateFocus{
		
		tabViews.do{ arg tab,i;
			if (activeTab == i){
				this.paintTab( tab, labels[i], 
					labelColors[ i%labelColors.size ], 
					stringFocusedColor ); // focus colors 
				views[i].visible_(true);
				// do the user focusAction only on focus
			}{
				this.paintTab( tab, labels[i], 
					unfocusedColors[ i%unfocusedColors.size ], 
					stringColor );// unfocus colors
				views[i].visible_(false);
					//do the user unfocusAction only on unfocus
			};
		};
	}
	
	doActions{
		action.value(this);
		tabViews.do{ arg tab,i;
			if (activeTab == i){
				if (focusHistory!= i){ focusActions[i].value; };
			}{
				if (focusHistory == i)
					//do the user unfocusAction only on unfocus
					{ unfocusActions[ focusHistory ].value };
			};
		};
		focusHistory = activeTab;
	}
	
	stringBounds { |string, font|
		(GUI.id === \swing).if{
		^Rect(0, 0, string.size * font.size * swingFactor.x, font.size * swingFactor.y);
		}{
		^GUI.stringBounds(string, font);
		}
	}
	
	updateViewSizes{
		
		left = 0;
		top  = 0;
		
			if ( tabHeight == \auto ){ tbht = (this.stringBounds("A",font).height+1 )}{tbht=tabHeight};
			tabViews.do{ arg tab, i; 
				if ( tabWidth.asSymbol == \auto )
					{ 
					tabWidths[i] = this.stringBounds(labels[i],font).width + labelPadding }
					{
						if (tabWidth.isNumber) {
							tabWidths[i] = tabWidth
						}{
							tabWidths[i] = tabWidth[i];
						}
					};
					
			};
//		{ /////This is a sloppy swing font width calculation
//			if ( tabHeight == \auto ){ tbht = (font.size+6)}{tbht=tabHeight};
//			tabViews.do{ arg tab, i; 
//				if ( tabWidth.asSymbol == \auto )
//					{ tabWidths[i] = (labels[i].size * swingFactor*font.size*0.09)+ labelPadding }
//					{ tabWidths[i] = tabWidth };
//			};
//		};

		
		switch(tabPosition)
		{\top}{this.updateViewSizesTop}
		{\left}{this.updateViewSizesLeft}
		{\bottom}{this.updateViewSizesBottom}
		{\right}{this.updateViewSizesRight};
		
		
		views.do{arg v;
			v.children.notNil.if{
				if (v.children[0].class.name==\FlowView){
					//v.children[0].bounds_(v.bounds);
					//this is redundant, but fixes strange behavior for some reason
					//v.children[0].bounds_(v.children[0].bounds.moveBy(2,2));
					v.children[0].reflowAll;
				};
			};
		};
	}
	
	
	
	updateViewSizesTop{
			
		tabViews.do{ arg tab, i; 
			followEdges.if{
				tab.bounds_( Rect(
						left + ( ([0]++tabWidths).integrate.at(i) ) + i,
						top,
						tabWidths[i],
						tbht)
						.moveBy(offset.x + windowBounds.left- 2,offset.y + windowBounds.top-5)
					);
			}{
				tab.bounds_( Rect(
						left + (i*tbht) + i,
						top ,
						tbht,
						tabWidths.maxItem)
						.moveBy(offset.x + windowBounds.left- 2,offset.y + windowBounds.top-5)
					);
			};
			tab.resize = switch(resize)
				{1}{1}
				{2}{1}
				{3}{3}
				{4}{1}
				{5}{1}
				{6}{3}
				{7}{7}
				{8}{7}
				{9}{9};
		};
		followEdges.if{
			views.do{ arg v, i; 
				v.bounds = Rect(
					left,
					top + tbht,
					view.bounds.width,
					view.bounds.height-tbht);
					v.background_( backgrounds[ i%backgrounds.size ] );
					
			};
		}{
			views.do{ arg v, i; 
				v.bounds = Rect(
					left,
					top + tabWidths.maxItem,
					view.bounds.width,
					view.bounds.height-tabWidths.maxItem);
					v.background_( backgrounds[ i%backgrounds.size ] );
					
			};
		};
		this.updateFocus;
	}
	
	updateViewSizesLeft{
		
		tabViews.do{ arg tab, i; 
			followEdges.not.if{
				tab.bounds_( Rect(
						left,
						top + (i*tbht) + i,
						tabWidths.maxItem,
						tbht)
						.moveBy(offset.x + windowBounds.left- 2,offset.y + windowBounds.top-5)

					);
			}{
				tab.bounds_( Rect(
						left,
						top + ( ([0]++tabWidths).integrate.at(i) )  + i,
						tbht,
						tabWidths[i])
						.moveBy(offset.x + windowBounds.left- 2,offset.y + windowBounds.top-5)

					);
			};
				
			tab.resize = switch(resize)
				{1}{1}
				{2}{1}
				{3}{3}
				{4}{1}
				{5}{1}
				{6}{3}
				{7}{7}
				{8}{7}
				{9}{9};
		};
		followEdges.not.if{
			views.do{arg v, i; 
				v.bounds = Rect(
					left + tabWidths.maxItem,
					top,
					view.bounds.width-tabWidths.maxItem,
					view.bounds.height);
					v.background_( backgrounds[ i%backgrounds.size ] );
			};
		}{
		views.do{arg v, i; 
			v.bounds = Rect(
				left + tbht,
				top,
				view.bounds.width-tbht,
				view.bounds.height);
				v.background_( backgrounds[ i%backgrounds.size ] );
			};
		};
		
		this.updateFocus();
	}	
	
	updateViewSizesBottom{
	
		tabViews.do{ arg tab, i; 
			followEdges.if{

				tab.bounds_( Rect(
						left + ( ([0]++tabWidths).integrate.at(i) ) + i,
						top+view.bounds.height-tbht,
						tabWidths[i],
						tbht)
						.moveBy(offset.x + windowBounds.left- 2,offset.y + windowBounds.top-5)

					);
			}{
				tab.bounds_( Rect(
						left + (i*tbht) + i,
						top+view.bounds.height-tabWidths.maxItem ,
						tbht,
						tabWidths.maxItem)
						.moveBy(offset.x + windowBounds.left- 2,offset.y + windowBounds.top-5)

					);
			};
				
			tab.resize = switch(resize)
				{1}{1}
				{2}{1}
				{3}{3}
				{4}{7}
				{5}{7}
				{6}{9}
				{7}{7}
				{8}{7}
				{9}{9};
		};
		
		followEdges.if{
			views.do{arg v, i; 
				v.bounds = Rect(
					left,
					top,
					view.bounds.width,
					view.bounds.height-tbht);
					v.background_( backgrounds[ i%backgrounds.size ] );
			};
		}{
			views.do{ arg v, i; 
				v.bounds = Rect(
					left,
					top ,
					view.bounds.width,
					view.bounds.height-tabWidths.maxItem);
					v.background_( backgrounds[ i%backgrounds.size ] );
					
			};
		};
		
		
		this.updateFocus;
	}	
	
	updateViewSizesRight{
			tabViews.do{ arg tab, i;
		followEdges.not.if{
				tab.bounds_( Rect(
						view.bounds.width + left - tabWidths.maxItem,
						top + (i*tbht) + i,
						tabWidths.maxItem,
						tbht)
						.moveBy(offset.x + windowBounds.left- 2,offset.y + windowBounds.top-5)
					);
			}{
				tab.bounds_( Rect(
						view.bounds.width + left - tbht,
						top + ( ([0]++tabWidths).integrate.at(i) )  + i,
						tbht,
						tabWidths[i])
						.moveBy(offset.x + windowBounds.left- 2,offset.y + windowBounds.top-5)
					);
			};
		
			tab.resize = switch(resize)
				{1}{1}
				{2}{3}
				{3}{3}
				{4}{3}
				{5}{3}
				{6}{3}
				{7}{9}
				{8}{9}
				{9}{9};
		};	
		followEdges.not.if{
			views.do{arg v, i; 
				v.bounds = Rect(
					left,
					top,
					view.bounds.width-tabWidths.maxItem,
					view.bounds.height);
					v.background_( backgrounds[ i%backgrounds.size ] );
			};
		}{
			views.do{arg v, i; 
				v.bounds = Rect(
					left,
					top,
					view.bounds.width-tbht,
					view.bounds.height
					);
					v.background_( backgrounds[ i%backgrounds.size ] );
			};
		};
		
		this.updateFocus();

	}	
	
	tabPosition_{arg symbol; // \left, \top, \right, or \bottom
	 	tabPosition=symbol;
		this.updateViewSizes();
		
	}
	
	followEdges_{arg bool; 
	 	followEdges=bool;
		this.updateViewSizes();
		
	}
	
	resize_{arg int;
		resize = int;
		view.resize_(int);
		views.do{|v| v.resize_(int)};
		this.updateViewSizes;	
	}
	
	focus_{arg index;
		activeTab = index;
		"focus_(index) deprecated. please use focus(index)".postln;
		this.updateFocus();
		this.doActions;
	}
	focus{arg index;
		activeTab = index;
		this.updateFocus();
		this.doActions;
	}
	
	labelColors_{arg colorArray; 
		labelColors = colorArray; 
		this.updateViewSizes();
	}
	
 	unfocusedColors_{arg colorArray; 
 		unfocusedColors = colorArray; 
 		this.updateViewSizes();
 	}
 	
	backgrounds_{arg colorArray; 
		backgrounds = colorArray; 
		this.updateViewSizes();
	}
	
	stringColor_{arg color; 
		stringColor = color; 
		this.updateViewSizes()
	}
	stringFocusedColor_{arg color; 
		stringFocusedColor = color; 
		this.updateViewSizes()
	}
	labelPadding_{arg int; 
		labelPadding = int;
		this.updateViewSizes;
	}
	
	tabWidth_{arg int; 
		tabWidth = int;
		this.updateViewSizes();
	}
	
	tabHeight_{arg val; 
		tabHeight = val;
		this.updateViewSizes();
	}
	
	tabCurve_{arg int; 	
		tabCurve = int;
		this.updateViewSizes();
	}

	font_{arg fnt; 	
		 font = fnt;
		 this.updateViewSizes();
		 
	}
	
	focusFrameColor_{arg color;focusFrameColor=color; tabViews.do{arg v; v.focusColor=focusFrameColor}}
	
	
	removeAt{ arg index;
	
		labels.removeAt(index);
		tabViews[index].remove;
		tabViews.removeAt(index);
		views[index].remove;
		views.removeAt(index);
		tabWidths.removeAt(index);
		focusActions.removeAt(index);
		unfocusActions.removeAt(index);
		
		tabViews.do{ arg tab, i;
			tab.mouseDownAction_({
				this.focus(i);
				unfocusTabs.if{tab.focus(false)}; 
			});
			tab.canReceiveDragHandler_({
				this.focus(i);
				unfocusTabs.if{tab.focus(false)}; 
			});
		};
		
		this.focus(0);
		focusHistory=0;
		this.updateViewSizes();
		view.refresh;
	}
		

	// use these as examples to make your own class extentions according to your needs
	*newBasic{ arg parent, bounds, labels, colors, name=" ", scroll=false;
		var q;
		q=this.new(parent, bounds, labels, colors, name, scroll);
		if( GUI.id === \cocoa)  {
			q.labelColors_([Color.white.alpha_(0.3)]);
			q.backgrounds_([Color.white.alpha_(0.3)]);
		}{
			q.labelColors_([Color(0.9,0.9,0.9)]);
			q.backgrounds_([Color(0.9,0.9,0.9)]);
			q.unfocusedColors_([Color(0.8,0.8,0.8)]);
		};
		^q;
	}
	
	*newRGBLabels{ arg parent, bounds, labels, colors, name=" ", scroll=false;
		"\nWarning: TabbedView.newRGBLabels deprecated. Use .newColorLabels instead".postln;
		^this.newColorLabels(parent, bounds, labels, colors, name, scroll);
		}
		
	*newColorLabels{ arg parent, bounds, labels, colors, name=" ", scroll=false;
		var q;
		q=this.newBasic(parent, bounds, labels, colors, name, scroll);
		q.labelColors_([Color.red,Color.blue,Color.yellow]);
		if( GUI.id === \cocoa)  {
			q.backgrounds_([Color.white.alpha_(0.3)]);
		}{
			q.backgrounds_([Color(0.9,0.9,0.9)]);
			q.unfocusedColors_([Color(0.9,0.75,0.75),
							Color(0.75,0.75,0.9),
							Color(0.9,0.9,0.75)]);
		};
		^q;
	}
	
	*newRGB{ arg parent, bounds, labels, colors, name=" ", scroll=false;
		"\nWarning: TabbedView.newRGB  deprecated. Use .newColor instead".postln;
		^this.newColor(parent, bounds, labels, colors, name, scroll);
		}
		
	*newColor{ arg parent, bounds, labels, colors, name=" ", scroll=false;
		var q;
		q=this.new(parent, bounds, labels, colors, name, scroll);
		q.labelColors_([Color.red,Color.blue,Color.yellow]);
		if( GUI.id === \cocoa)  {
			q.backgrounds_([Color.red.alpha_(0.1),
								Color.blue.alpha_(0.1),
								Color.yellow.alpha_(0.1)]);
			q.unfocusedColors_([Color.red.alpha_(0.2),
								Color.blue.alpha_(0.2),
								Color.yellow.alpha_(0.2)]);
		}{
			q.backgrounds_([Color(0.9,0.85,0.85),
								Color(0.85,0.85,0.9),
								Color(0.9,0.9,0.85)]);
			q.unfocusedColors_([Color(0.9,0.75,0.75),
								Color(0.75,0.75,0.9),
								Color(0.9,0.9,0.75)]);
		};

		^q;
	}
	
	*newFlat{ arg parent, bounds, labels, colors, name=" ", scroll=false;
		var q;
		q=this.newBasic(parent, bounds, labels, colors, name, scroll);
		q.tabHeight=14;
		q.tabWidth= 70;
		q.tabCurve=3;
		^q;
	}
	
	*newTall{ arg parent, bounds, labels, colors, name=" ", scroll=false;
		var q;
		q=this.newBasic(parent, bounds, labels, colors, name, scroll);
		q.tabHeight= 30;
		q.tabWidth= 70;
		q.tabCurve=3;
	^q;
	}
	
	*newTransparent{ arg parent, bounds, labels, colors, name=" ", scroll=false;
		var q;
		q=this.new(parent, bounds, labels, colors, name, scroll);
		if( GUI.id === \cocoa)  {
			q.labelColors_([Color.white.alpha_(0.3)]);
		}{	
			q.labelColors_([Color(0.9,0.9,0.9)]);
			q.unfocusedColors_([Color(0.8,0.8,0.8)]);
		};
		q.backgrounds_([Color.clear]);
		^q;
	}
	
	*newPacked{ arg parent, bounds, labels, colors, name=" ", scroll=false;
		var q;
		q=this.new(parent, bounds, labels, colors, name, scroll);
		if( GUI.id === \cocoa)  {
			q.labelColors_([Color.white.alpha_(0.3)]);
			q.backgrounds_([Color.white.alpha_(0.3)]);
			}{
			q.labelColors_([Color(0.85,0.85,0.85)]);
			q.backgrounds_([Color(0.85,0.85,0.85)]);
			q.unfocusedColors_([Color(0.8,0.8,0.8)]);
		};
		q.tabCurve=3;
		q.labelPadding=8;
		q.tabHeight=14;
		^q;
	}
		
		
	prClose { onClose.value }
	
	value{ ^activeTab }
	
	value_{ |v|
		activeTab = v.asInt;
		this.updateFocus();
		^this
	}
	
	valueAction_{ |v|
		activeTab = v.asInt;
		this.updateFocus();
		^this
	}
	
	refresh{
		this.updateFocus();
		^this
	}

	
}
