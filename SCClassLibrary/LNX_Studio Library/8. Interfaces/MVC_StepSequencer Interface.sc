
// mvc step sequencer 

MVC_StepSequencer {
	
	classvar <objectHeaderType="LNX StepSequencer Doc", <version=1.1, >studio;
	
	var <id, <api, <midiControl, <midiOffset, <controlSpec;
	
	var <nameModel, <>controlName="Seq";
	
	var <defaultSteps,		<steps, 			<seq,	<sP,
	    <seqModels, 		<spModels,	
	    <models, 			<gui,
	    <interpolate,		<interpNo=0, 		<lastCCvalue,
	    <seqPresetMemory,	<sPPresetMemory, 	<>nameClickAction;
	
	var <seqRaw, 			<seqInt, 			<durInt; // for arpeg seq
	    
	var <>action,			<nameWidget,		<latch=false;
	
	// make me a step sequencer

	*new {|id, defaultSteps, midiControl, midiOffset, controlSpec, interpolate=false|
		^super.new.init(id, defaultSteps, midiControl, midiOffset, controlSpec, interpolate)
	}

	// init it

	init{|argID, argDefaultSteps, argMIDIControl, argMIDIOffset, argControlSpec, argInterpolate|
		id=argID;
		defaultSteps = argDefaultSteps;
		steps = defaultSteps;
		midiControl = argMIDIControl;
		midiOffset = argMIDIOffset;
		controlSpec = argControlSpec;
		interpolate = argInterpolate;
		this.initModels;
		this.initVars;
		this.makeSeqLists;
	}

	// the models

	initModels{
		
		models=();

		seqModels = nil ! steps;
		
		steps.do{|i|
			seqModels[i]=[controlSpec,
				midiControl, midiOffset+100+i, controlName++" Step:"++((i+1).asString),
				{|me,val,latency,send| this.setSeq(i,val,latency,send) }].asModel;
		};
		
		models[\pos] = 0.asModel;
		
		#spModels,sP= [
			1,			// 0.on/off
			0.5,			// 1.dur
			36,			// 2.midi note
			
			// 3. no of steps
			[ 32, [1,steps,\lin,1], midiControl, midiOffset+3, controlName++"Steps",
				//(label_:"Steps"),
				{|me,val,latency,send| this.setItem(3,val,latency,send) }
			],	         
			
			// 4. ruler
			[ 4, [2,16,\lin,1], midiControl, midiOffset+4, controlName++"Ruler",
				//(label_:"Ruler"),
				{|me,val,latency,send| this.setItem(4,val,latency,send) }
			],
				 
			// 5. adj
			0,
			 
			// 6. speed divider
			[ 2, [1,32,\lin,1], midiControl, midiOffset+6, controlName++"Speed",
				//(label_:"Speed"),
				{|me,val,latency,send| this.setItem(6,val,latency,send) }
			],
				
			// 7. type 0=midiNote, 1=midiControl
			1,
			
			// 8. interpolation  (0-16)
			[ 1, [1,16,\lin,1], midiControl,  midiOffset+8, controlName++"Interp",
				//(label_:"Interp"),
				{|me,val,latency,send| this.setItem(8,val,latency,send) }]
			
		].generateAllModels;
		
	}
	
	// used by grs to adjust all channel, a net specific version would be more efficient
	addValueToSP{|index,adjust|
		spModels[index].doValueAction_(spModels[index].value+adjust,nil,true,false,nil,1);
	}
	
	// init the vars

	initVars{
		api=LNX_API.newTemp(this,id,#[\netSeq,\netSetItem,\netMove]); // the API and it's interface
		seq = (seqModels[0].value) ! steps;
		gui=();
		nameModel="Step Seq".asModel;
		
		seqPresetMemory=[];
		sPPresetMemory=[];
		
		// i've put this here because model.enabled_ is not part of any kit
		gui[\stepsFuncAdaptor]=MVC_FuncAdaptor(spModels[3])
			.func_{|me,value|
				var enabled;
				{	
					seqModels.do({|model,index|
						enabled=(index<value).if(true,false);
						if (model.enabled!=enabled) { model.enabled_(enabled) };
					});
				}.defer;
			};
	}
	
	// put a rand sequence in
	rand {
		var r=31.rand+1;
		var l=0!32;
		var t=1;
		t.do{|i|
			if (0.5.coin) {
				l=l+((0!(((31-r).rand)/2).roundUp(1)*2)++Bjorklund(r.rand+1, r).extend(32,0));
			}{
				l=l+((0!(((31-r).rand)))++Bjorklund(r.rand+1, r).extend(32,0));	
			};
		};
		l=l.normalize(0,1);
		seqModels.do{|model,j| model.lazyValueAction_(l[j],nil,true) };
		this.makeSeqLists;
	}
	
	// clear the sequencer
	clearSequencer{
		seqModels.do{|model,j| model.lazyValueAction_(seqModels[0].controlSpec.default,nil,true) };
		this.makeSeqLists;
	}
	
	// get & set the name of this sequencer
	
	name{^nameModel.string}
	
	name_{|string| nameModel.string_(string) }
	
	// set methods ///////////////////////////
	
	// recordStep, used in bum note to record into seq (recordIndex is transport clock beat)
	recordStep{|recordIndex,value|
		this.setStep( (recordIndex/sP[6]).asInt%(sP[3]) ,value); // index is wrapped
	}

	// used in rand seq only! & now recordStep
	setStep{|index,value|
		seqModels[index].lazyValueAction_(value,nil,true);
		this.makeSeqLists;
	}

	// set a value in the sequence
	setSeq{|index,value,latency,send=false|
		if (seq[index]!=value) {
			seq[index]=value;
			if (send) { api.sendVP("ss"++index,'netSeq',index,value) };
			this.makeSeqLists;
		};	
	}
	
	// net version of setSeq it important to use lazy here to take (clock)
	netSeq{|index,value| seqModels[index].lazyValueAction_(value,nil,false) }

	// set an item
	setItem{|item,value,latency,send|
		if (sP[item]!=value) {
			sP[item]=value;
			if (send) { api.sendVP((id+"_nsi"+"_"+item).asSymbol,'netSetItem',item,value) };
			if ((item==3)||(item==6)) { this.makeSeqLists};
		}
	}
	
	// set model but don't resend over network
	netSetItem{|item,value| spModels[item].lazyValueAction_(value,nil,false) }
	
	// fill at start-up
	fill{|step=2|
		seq.size.do{|index|
			seqModels[index].value_((index%step==0).if(1,0));
			seq[index]=((index%step==0).if(1,0));
		};
		this.makeSeqLists;
	}
	
	////////////////////////////////////
		
	// work out the lists that help work out abs beat Number and time to next note
	// used to work note duration (time to next bang event) for LNX_MelodyMaker
		
	makeSeqLists{
	
		var n;
			
		// for arpeg seq and beatNo (take out seqInt out of this function)
		seqRaw = seq[0..(sP[3].asInt-1)].collect{|i| (i>0).if(1,0)};
		seqInt = seqRaw.integrate;
		
		// and dur to next
		// slightly complex but based around reverse integration of seqInt
		n=0;
		durInt = (1-((seqRaw++seqRaw).reverse)).collect{|v| n=n+1; n=n*v; n+1}.reverse.drop(1);
	
	}
	
	//clockIn is the clock pulse, with the current song pointer in beats
	clockIn   {|beat,latency|
			
		var vel, absPos, pos, speed;	
		var interp, nextVel, iVel, intN;
		var beatNo, dur;
			
		if (latch) {
			speed = 1;
		}{	
			speed = sP[6];
		};

		if ((beat%speed)==0) {
			
			absPos = (beat/speed).asInt;
			pos    = absPos%(sP[3]);
			vel    = seq[pos];
			interp = sP[8];
			
			//this.makeSeqLists; // to remove
			
			beatNo = (absPos.div(sP[3])* (seqInt.last)) + (seqInt.wrapAt(absPos)) - 1;
			dur = durInt[pos]*speed;
			
			if ((interpolate)and:{interp>1}) {
			
				// this used to be >=0 (off below, need to address this i.e zeroValue)
				if (vel>0) {  
					
					this.bang(vel,latency,absPos,beatNo,dur,pos);
					
					lastCCvalue=vel;
					nextVel=seq[(absPos+1)%(sP[3])];
					
					// this used to be >=
					if (nextVel>0) {
						nextVel=(nextVel+sP[5]);
						interpNo=interpNo+1;
						{
							intN=interpNo;
							(interp-1).do{|i|
								// this test stops pervious interps if they overlap
								if (intN==interpNo) {
									(studio.absTime*speed*3/interp).wait;
									iVel=vel+((nextVel-vel)/interp*(i+1));
									lastCCvalue=iVel;
									this.bang(iVel,latency,absPos,beatNo,dur,pos);
								};
							};
						}.fork(SystemClock);
					};
				
				};
				
			}{
				if (vel>0) { this.bang(vel,latency,absPos,beatNo,dur,pos) }; // bang it
			};
			
			{models[\pos].lazyValue_(pos,false)}.defer(latency);
		};

	}
	
	// bang it
	bang{|vel,latency,beat,beatNo,dur,pos| action.value(vel,latency,beat,beatNo,dur,pos) }
	
	//clockPlay { }
	clockStop {|latency| {models[\pos].value_(0).dependantsPerform(\pause)}.defer(latency) }
	clockPause{|latency| {models[\pos].dependantsPerform(\pause)}.defer(latency) }
	
	
	//////////////////////////////////////
		
	freeAutomation{
		spModels.do(_.freeAutomation);
		seqModels.do(_.freeAutomation);
	}
	
	free{
		this.freeAutomation;
		gui.do(_.free);
		api.free;
		api = midiControl = action  = nil;
	}
	
	getSaveList{
		var l;
		// the header
		l=[objectHeaderType+"v"++version, sP.size, seq.size, seqPresetMemory.size];

		// the sequence
		l=l++sP;
		l=l++seq;
		
		// and the presets
		seqPresetMemory.size.do{|i|
				l=l++(sPPresetMemory[i]);
				l=l++(seqPresetMemory[i]);
		};
		
		l=l++(["*** END OBJECT DOC ***"]);
		^l
	}
	
	putLoadList{|l|
		var header, loadVersion, spSize, seqSize, seqPMSize;
		l=l.reverse;
		header=l.popS;
		loadVersion=header.version;
		if ((header.documentType==objectHeaderType)&&(version>=loadVersion)) {
			spSize   =l.popI;
			seqSize  =l.popI;
			seqPMSize=l.popI;
			sP=l.popNF(spSize);  // need to extend or reduce as needed
			seq=l.popNF(seqSize)[0..steps].extend(steps,0);
			// need preset load here
			
			sPPresetMemory=[];
			seqPresetMemory=[];
			
			seqPMSize.do{|i|
				sPPresetMemory=sPPresetMemory.add(l.popNF(spSize));				seqPresetMemory=seqPresetMemory.add(l.popNF(seqSize));
			};
			
			this.updateGUI;
			this.makeSeqLists;
		}
	}
	
	updateGUI{
		var enabled;		
		seqModels.do{|model,j| if (seq[j]!=model.value) {model.value_(seq[j])} };
		spModels.do {|model,j| if (sP[j]!=model.value) {model.lazyValueAction_(sP[j]) }};
	}
	
	// Presets /////////////////////////
	
	iGetPrestSize{ ^2+(sP.size)+(seq.size) }
	
	// get the current state as a list
	iGetPresetList{ ^[sP.size,seq.size]++sP++seq }


	// add a statelist to the presets
	iAddPresetList{|l|
		var sPSize,seqSize;
		l=l.reverse;
		#sPSize,seqSize=l.popNI(2);
		sPPresetMemory = sPPresetMemory.add(l.popNF(sPSize));
		seqPresetMemory=seqPresetMemory.add(l.popNF(seqSize));
	}
	
	// save a state list over a current preset
	iSavePresetList{|i,l|
		var sPSize,seqSize;
		l=l.reverse;
		#sPSize,seqSize=l.popNI(2);
		sPPresetMemory[i]=l.popNF(sPSize);
		seqPresetMemory[i]=l.popNF(seqSize);
	}
	

	// for your own load preset
	iLoadPreset{|i|
		seqModels.do{|m,j| m.lazyValueAction_(seqPresetMemory[i][j],nil,false,false) };
		spModels.do{|m,j| m.lazyValueAction_(sPPresetMemory[i][j],nil,false,false) };
		this.makeSeqLists;
	}
	
	// for your own remove preset
	iRemovePreset{|i| seqPresetMemory.removeAt(i); sPPresetMemory.removeAt(i);}
	
	// for your own removal of all presets
	iRemoveAllPresets{ seqPresetMemory=[]; sPPresetMemory=[] }
	
	///////////////////////////
			
	/////////
	//     //
	// GUI //
	//     //
	/////////
	
	// classic style
	
	createWidgets{|window, bounds, colors|
		
		var seqViews;
		var l,t,w,h, sw, os, rh, ph; // slider width, offset (left), ruler height, position height
		
		colors = colors ? ( \background:Color(0,0,0,0.8), \slider:Color.green, 
							\border:Color(0.5,0.66,1,1), \string:Color.white );
		
		l=bounds.left;
		t=bounds.top;
		w=bounds.width;
		h=bounds.height;
		
		os=60;
		rh=15;
		ph=5;
		
		sw=(w-os/steps).asInt;
		
		seqViews = nil ! steps;
		
		// step sliders
		steps.do{|x|
			var c,col,col2;
			c=(x%(sP[4])).map(0,sP[4],1,0.4);
			col=colors[\slider];
			col2=colors[\border];
			seqViews[x]= MVC_FlatSlider(window, seqModels[x],
										Rect(l+os+(x*sw),t+rh+3,sw-1,h-rh-ph-6))
				.color_(\background,colors[\background])
				.color_(\border,
					Color(col2.red*c/3.4,col2.green*c/3.4,col2.blue*c/3.4,col2.alpha))
				.color_(\slider,Color(col.red*c,col.green*c,col.blue*c,col.alpha))
				.seqItems_(seqViews);
		};
		
		gui[\theme1]=(	\orientation_  : \horizontal,
						\resoultion_	 : 1,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.1,0.1,0.1),
										\string : colors[\string],
										\focus : Color(0,0,0,0)));
		
		// 4.ruler
		MVC_NumberBox(spModels[4], window, Rect(l+34,t,23,16), gui[\theme1]).label_("Ruler");
		MVC_RulerView(spModels[4], window, Rect(l+os,t,steps*sw,rh))
 	  		.buttonWidth_(sw)
 	   		.label_(nil)
 	   		.steps_(steps)
 	   		.color_(\on,colors[\slider]+0.4)
 	   		.color_(\string,colors[\slider])
			.color_(\background,colors[\background]);
			
		//3.steps
		MVC_NumberBox(spModels[3], window, Rect(l+34,t+25-6,23,16),gui[\theme1]).label_("Steps");
		// 6.speed
	  	MVC_NumberBox(spModels[6], window, Rect(l+34,t+44-6,23,16),gui[\theme1]).label_("Speed");
			
		// pos
	  	MVC_PosView(models[\pos], window, Rect(l+os, t+h-ph, steps*sw, ph))
			.color_(\on,colors[\slider])
			.buttonWidth_(sw)
			.color_(\background,colors[\background]);
			
		// 4.ruler as a MVC_FuncAdaptor to format slider colors to model
		gui[\sliderRulerFuncAdaptor]=MVC_FuncAdaptor(spModels[4])
			.func_{|me,value|
				{
					steps.do{|x|	
						var c,col,col2;
						c=((x)%value).map(0,value,1,0.4);
						col=colors[\slider];
						col2=colors[\border];
						seqViews[x]
							.color_(\border,
								Color(col.red*c/3.4,col.green*c/3.4,col.blue*c/3.4))
							.color_(\slider,Color(col.red*c,col.green*c,col.blue*c));
							
					}
				}.defer;
			};
			
	}
	
	// new pin style
	
	createButtonWidgets{|window, bounds, colors, controls=true|
		
		var seqViews;
		var l,t,w,h, sw, os, rh, ph; // slider width, offset (left), ruler height, position height
		
		colors = colors ? ( \background:Color(0,0,0,0.8), \on:Color(61/128,47/95,43/75),
							\border:Color(0.5,0.66,1,1), \string:Color.white,
							\rulerOn: Color.white, \rulerBackground : Color (1,1,1,0.4),
							\position : Color.white, \pinOn : Color.green  ); // ++  colors;
		
		l=bounds.left;
		t=bounds.top;
		w=bounds.width;
		h=bounds.height;
		os=60;
		rh=10;
		sw=((w-80)-os/steps).asInt;
		
		ph=4;
		
		seqViews = nil ! steps;
		
		gui[\theme2]=(	\orientation_  : \horizontal,
						\resoultion_	 : 1,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.1,0.1,0.1,0.67),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : colors[\string],
										\focus : Color(0,0,0,0)));
		
		nameWidget = MVC_StaticText(nameModel, window, Rect(l-2-5,t+rh-2,os-8+8+5,sw-1+5+1))
			.font_(Font("Arial", 12))
			.color_(\stringDown,Color.black)
			.shadowDown_(false)
			.action_{ nameClickAction.value(this) };
		
		// step sliders
		steps.do{|x|
			var c,col,col2;
			c=(x%(sP[4])).map(0,sP[4],1,0.5);
			col=colors[\on];
			col2=colors[\border];
			seqViews[x]=MVC_PinSeqView(window, seqModels[x],Rect(l+os+(x*sw),t+rh+2,sw,sw+1))
				.left_(x%4!=0)
				.right_(x+1%4!=0)
				.color_(\background,colors[\background])
				.color_(\on,colors[\pinOn]??{Color.green})
				.seqItems_(seqViews);
		};
			
		// pos ColorAdaptor
		gui[\posColorAdaptor]=MVC_ColorAdaptor(models[\pos])
			.views_(seqViews)
			.colorIndex_(\background)
			.color_(\on,Color.white)
			.valueDo_(models[\pos].value); // this shoud be in adaptor
				

		// 4.ruler as a MVC_FuncAdaptor to format pins to model
		gui[\buttonStepsFuncAdaptor]=MVC_FuncAdaptor(spModels[4])
			.func_{|me,val|
				{
					steps.do{|x|
						seqViews[x].left_(x%val!=0).right_(x+1%val!=0)
					};
					seqViews.last.right_(false);
					seqViews.do(_.refresh);
				}.defer;
			};
		
		if (controls) {
		
			// 4.ruler
			MVC_NumberCircle(spModels[4], window,
				Rect(l+os+(steps*sw)+6,t+rh,18,18),gui[\theme2]);
		
			//3.steps
			MVC_NumberCircle(spModels[3], window,
				Rect(l+os+(steps*sw)+6+20+20,t+rh,18,18),gui[\theme2]);
	
			// 6.speed
			MVC_NumberCircle(spModels[6], window,
				Rect(l+os+(steps*sw)+6+20,t+rh,18,18),gui[\theme2]);
				
		}
	
	}
	
	rulerModel {^spModels[4]}
	stepsModel {^spModels[3]}
	speedModel {^spModels[6]}
	
	// round style
	
	createRoundWidgets{|window, bounds, colors, hiliteMode='inner'|
		
		var seqViews, c;
		var l,t,w,h, sw, os, rh, ph; // slider width, offset (left), ruler height, position height
		var lastX, lastValue;
		
		c =  ( \background:Color(0,0,0), \slider:Color(50/77,61/77,1), 
					\border:Color.black, \string:Color.white, \hilite : Color(0,1,0));
		
		if (colors.notNil) { colors=c++colors } {colors=c};

		l=bounds.left;
		t=bounds.top;
		w=bounds.width;
		h=bounds.height;
		
		os=60;
		rh=15;
		ph=10;
		
		sw=(w-os/steps).asInt;
		
		seqViews = nil ! steps;
										
		gui[\theme2]=(	\orientation_  : \horizontal,
						\resoultion_	 : 1,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",12),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.1,0.1,0.1,0.67),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : colors[\string],
										\focus : Color(0,0,0,0)));
										
		// step sliders
		steps.do{|x|
			var c,col,col2;
			c=(x%(sP[4])).map(0,sP[4],1,0.4);
			col=colors[\hilite];
			col2=colors[\on];
			seqViews[x]= MVC_PinSlider(window, seqModels[x],
										Rect(l+os+(x*sw)+1,t+rh+3,sw-2,h-rh-ph-6))
				.hiliteMode_(hiliteMode)
				.color_(\background,colors[\background])
				.color_(\on,Color(col2.red*c,col2.green*c,col2.blue*c))
				.color_(\hilite,Color(col.red*c,col.green*c,col.blue*c))
				.seqItems_(seqViews);
		};
	
											
		// 4.ruler
		MVC_NumberCircle(spModels[4], window, Rect(l+34,t,20,20), gui[\theme2])
			.label_("Ruler");
		
		// 4.ruler
		MVC_RulerView(spModels[4], window, Rect(l+os,t,steps*sw,rh))
 	  		.buttonWidth_(sw)
 	   		.label_(nil)
 	   		.steps_(steps)
 	   		.color_(\on,Color.black)
 	   		.color_(\string,Color.white)
			.color_(\background,Color(0,0,0,0.4));
			
		// 4.ruler as a MVC_FuncAdaptor to format slider colors to model
		gui[\sliderRulerFuncAdaptor]=MVC_FuncAdaptor(spModels[4])
			.func_{|me,value|
				{
					steps.do{|x|	
						var c,col,col2;
						c=((x)%value).map(0,value,1,0.4);
						col=colors[\hilite];
						col2=colors[\border];
						seqViews[x]
							.color_(\on,Color(col.red*c,col.green*c,col.blue*c))
							.color_(\hilite,Color(col.red*c,col.green*c,col.blue*c))						
					}
				}.defer;
			};
			
		//3.steps
		MVC_NumberCircle(spModels[3], window, Rect(l+34,t+23,20,20),gui[\theme2])
			.label_("Steps");
			
		// 6.speed
	  	MVC_NumberCircle(spModels[6], window, Rect(l+34,t+46,20,20),gui[\theme2])
	  		.label_("Speed");
	  	
	  	// 8. interpolation
	  	if (interpolate) {
	  		MVC_NumberCircle(spModels[8], window, Rect(l+34,t+69,20,20),gui[\theme2])
	  			.label_("Interp");
	  	};
			
		// pos
	  	MVC_PosView(models[\pos], window, Rect(l+os, t+h-ph-3, steps*sw, ph))
			.color_(\on,colors[\slider])
			.type_(\circle)
			.buttonWidth_(sw)
			.color_(\background,Color(0,0,0,0));
			
		// pos Hilite Adaptor
		gui[\posHiliteAdaptor]=MVC_HiliteAdaptor(models[\pos])
			.refreshZeros_(false)
			.views_(seqViews);

			
		// move <->	
		MVC_UserView(window,Rect(w+9,t+86-90,35,20))
			.drawFunc_{|me|
				if (lastValue==nil) {Color.black.set} {Color.white.set};
				DrawIcon( \lineArrow, Rect(-3,-1,me.bounds.width+2,me.bounds.height+2) , pi);
				DrawIcon( \lineArrow, Rect(3,-1,me.bounds.width+2,me.bounds.height+2) );
			}
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				lastX=x;
				lastValue=0;
				me.refresh;
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var val = (x-lastX).div(15);
				var offset = (lastValue-val);
				lastValue = val;
				if (offset!=0) { this.move(offset) };
				me.refresh;
			}
			.mouseUpAction_{|me|
				lastX=nil;
				lastValue=nil;
				me.refresh;
			};
			
	}
	
	// move sequencer left or right
	move{|offset| api.groupCmdOD(\netMove,offset) }
	
	// net of above
	netMove{|offset|	
		var seqCopy = seq.copy[0..(sP[3].asInt-1)];
		offset=offset.asInt;
		sP[3].asInt.do{|i|
			seq[i]=seqCopy.wrapAt(i+offset);
			seqModels[i].value_(seq[i],false);	
		};
	}
	

////////////////

// oct 


	// new pin style
	
	createOctaveWidgets{|window, bounds, colors, controls=true|
		
		var seqViews;
		var l,t,w,h, sw, os, rh, ph; // slider width, offset (left), ruler height, position height
		
		colors = colors ? ( \background:Color(0,0,0,0.8), \on:Color(61/128,47/95,43/75),
							\border:Color(0.5,0.66,1,1), \string:Color.white,
							\rulerOn: Color.white, \rulerBackground : Color (1,1,1,0.4),
							\position : Color.white, \pinOn : Color.green  );
		
		l=bounds.left;
		t=bounds.top;
		w=bounds.width;
		h=bounds.height;
		os=60;
		rh=10;
		sw=((w-80)-os/steps).asInt;
		
		ph=4;
		
		seqViews = nil ! steps;
		
		gui[\theme2]=(	\orientation_  : \horizontal,
						\resoultion_	 : 1,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.1,0.1,0.1,0.67),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : colors[\string],
										\focus : Color(0,0,0,0)));
		
		nameWidget = MVC_StaticText(nameModel, window, Rect(l-2-5,t+(h/2)-15,os-8+8+5,20))
			.font_(Font("Arial", 12))
			.color_(\stringDown,Color.black)
			.shadowDown_(false)
			.action_{ nameClickAction.value(this) };
		
		// step sliders
		steps.do{|x|
			var c,col,col2;
			c=(x%(sP[4])).map(0,sP[4],1,0.5);
			col=colors[\on];
			col2=colors[\border];
			seqViews[x]=MVC_OctSeqView(window, seqModels[x],Rect(l+os+(x*sw),t,sw,h))
				.left_(x%4!=0)
				.right_(x+1%4!=0)
				.color_(\background,colors[\background])
				.color_(\on,colors[\pinOn]??{Color.green})
				.seqItems_(seqViews);
		};
			
			
		// this isn't very efficient
			
		// pos adaptor for last value
		gui[\buttonPosFuncAdaptor]=MVC_FuncAdaptor(spModels[4])
			.func_{|me,val|
				{
					if (lastCCvalue.notNil) {
						steps.do{|x|
							seqViews[x].lastValue_(lastCCvalue);
							
						};
					};
				}.defer;
			};
			
			
		// pos ColorAdaptor
		gui[\posColorAdaptor]=MVC_ColorAdaptor(models[\pos])
			.views_(seqViews)
			.colorIndex_(\background)
			.color_(\on,Color.white-0.2)
			.valueDo_(models[\pos].value); // this shoud be in adaptor
				

		// 4.ruler as a MVC_FuncAdaptor to format pins to model
		gui[\buttonStepsFuncAdaptor]=MVC_FuncAdaptor(spModels[4])
			.func_{|me,val|
				{
					steps.do{|x|
	//					var c,col;
	//					
	//					c=((x)%val).map(0,val,1,0.4);
	//					col=Color.green;
	//					seqViews[x]
	//						.color_(\on,Color(col.red*c,col.green*c,col.blue*c));
	//					
	//					
						seqViews[x].left_(x%val!=0).right_(x+1%val!=0);
	
					};
					seqViews.last.right_(false);
					seqViews.do(_.refresh);
				}.defer;
			};
			
		
		if (controls) {
		
			// 4.ruler
			MVC_NumberCircle(spModels[4], window,
				Rect(l+os+(steps*sw)+6,t+(h/2)-15,18,18),gui[\theme2]);
		
			//3.steps
			MVC_NumberCircle(spModels[3], window,
				Rect(l+os+(steps*sw)+6+20+20,t+(h/2)-15,18,18),gui[\theme2]);
	
			// 6.speed
			MVC_NumberCircle(spModels[6], window,
				Rect(l+os+(steps*sw)+6+20,t+(h/2)-15,18,18),gui[\theme2]);
				
		}
	
	}
	
	// round style
	
	createVelocityWidgets{|window, bounds, colors, hiliteMode='inner', controls=true|
		
		var seqViews, c;
		var l,t,w,h, sw, os, rh, ph; // slider width, offset (left), ruler height, position height
		
		c =  ( \background:Color(0,0,0), \slider:Color(50/77,61/77,1), 
					\border:Color.black, \string:Color.white, \hilite : Color(0,1,0));
		
		if (colors.notNil) { colors=c++colors } {colors=c};
		
		l=bounds.left;
		t=bounds.top;
		w=bounds.width;
		h=bounds.height;
		
		os=60;
		rh=0;
		
		sw=((w-80)-os/steps).asInt;
		
		ph=10;
		
		seqViews = nil ! steps;
										
		gui[\theme2]=(	\orientation_  : \horizontal,
						\resoultion_	 : 1,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",12),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.1,0.1,0.1,0.67),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : colors[\string],
										\focus : Color(0,0,0,0)));
										
		nameWidget = MVC_StaticText(nameModel, window, Rect(l-7,t+(h/2)-15,os+5,20))
			.font_(Font("Arial", 12))
			.color_(\stringDown,Color.black)
			.shadowDown_(false)
			.action_{ nameClickAction.value(this) };
		
		
		// step sliders
		steps.do{|x|
			var c,col,col2;
			c=(x%(sP[4])).map(0,sP[4],1,0.4);
			col=colors[\hilite];
			col2=colors[\on];
			seqViews[x]= MVC_PinSlider(window, seqModels[x],
										Rect(l+os+(x*sw),t+rh+3,sw,h-rh-ph-6))
										
				.hiliteMode_(hiliteMode)
				.left_(x%4!=0)
				.right_(x+1%4!=0)
				.width_(6.5)
				.color_(\background,colors[\background])
				.color_(\on,Color(col2.red*c,col2.green*c,col2.blue*c))
				.color_(\hilite,Color(col.red*c,col.green*c,col.blue*c))
				.seqItems_(seqViews);
		};
	
											
					
		// pos Hilite Adaptor
		gui[\posHiliteAdaptor]=MVC_HiliteAdaptor(models[\pos])
			.refreshZeros_(false)
			.views_(seqViews);

//		// pos
//	  	MVC_PosView(models[\pos], window, Rect(l+os, t+h-ph-3, steps*sw, ph))
//			.color_(\on,colors[\slider])
//			.type_(\circle)
//			.buttonWidth_(sw)
//			.color_(\background,Color(0,0,0,0));			

			
		// 4.ruler as a MVC_FuncAdaptor to format slider colors to model
		gui[\sliderRulerFuncAdaptor]=MVC_FuncAdaptor(spModels[4])
			.func_{|me,value|
				{
					steps.do{|x|	
						var c,col,col2;
						
						seqViews[x].left_(x%value!=0).right_(x+1%value!=0);
						
						c=((x)%value).map(0,value,1,0.4);
						col=colors[\hilite];
						col2=colors[\border];
						seqViews[x]
							.color_(\on,Color(col.red*c,col.green*c,col.blue*c))
							.color_(\hilite,Color(col.red*c,col.green*c,col.blue*c));
					}
				}.defer;
			};
			
		if (controls) {
				
		// 4.ruler
		MVC_NumberCircle(spModels[4], window,
			Rect(l+os+(steps*sw)+6,t+(h/2)-15,18,18),gui[\theme2]);
		
			//3.steps
			MVC_NumberCircle(spModels[3], window,
				Rect(l+os+(steps*sw)+6+20+20,t+(h/2)-15,18,18),gui[\theme2]);
	
			// 6.speed
			MVC_NumberCircle(spModels[6], window,
				Rect(l+os+(steps*sw)+6+20,t+(h/2)-15,18,18),gui[\theme2]);
				
		};
						
			
	}
	
	
	// used for notes in bumnote2
		
	createSmoothWidgets{|window, bounds, colors, controls=true, writeModel|
		
		var seqViews;
		var l,t,w,h, sw, os, rh, ph; // slider width, offset (left), ruler height, position height
		
		colors = colors ? ( \background:Color(0,0,0,0.5), \on:Color(61/128,47/95,43/75),
							\border:Color(0.5,0.66,1,1), \string:Color.white,
							\rulerOn: Color.white, \rulerBackground : Color (1,1,1,0.4),
							\position : Color.white, \pinOn : Color.green  );
		
		l=bounds.left;
		t=bounds.top;
		w=bounds.width;
		h=bounds.height;
		os=60;
		rh=10;
		sw=((w-80)-os/steps).asInt;
		ph=4;
		seqViews = nil ! steps;
		
		gui[\theme2]=(	\orientation_  : \horizontal,
						\resoultion_	 : 1,
						\font_		 : Font("Helvetica",10),
						\labelFont_	 : Font("Helvetica",10),
						\showNumberBox_: false,
						\colors_       : (	\label : Color.white,
										\background : Color(0.1,0.1,0.1,0.67),
										\backgroundDown : Color(0.1,0.1,0.1,0.85),
										\string : colors[\string],
										\focus : Color(0,0,0,0)));
		
		nameWidget = MVC_StaticText(nameModel, window, Rect(l-2-5,t+(h/2)-15,os-8+8+5,20))
			.font_(Font("Arial", 12))
			.color_(\stringDown,Color.black)
			.shadowDown_(false)
			.action_{ nameClickAction.value(this) };
		
		// step sliders
		steps.do{|x|
			var c,col,col2;
			c=(x%(sP[4])).map(0,sP[4],1,0.5);
			col=colors[\on];
			col2=colors[\border];
			seqViews[x]=MVC_SmoothSlider(window, seqModels[x],Rect(l+os+(x*sw)+1,t,sw-2,(h-20)))
				.action_{
					writeModel.value_(x);	
				}
				.numberFunc_(\note)
				.numberOffset_(12*(x%2)+20)
				.drawNegative_(false)
				.thumbSize_(8)
				.numberFont_(Font("Helvetica",11))
				.numberWidth_(-17)
				.color_(\background,colors[\background])
				.color_(\on,colors[\pinOn]??{Color.green})
				.color_(\numberUp,Color.black)
				.color_(\numberDown,Color.white)
				.color_(\hilite,Color(0,0,0,0))
				.color_(\knob,Color(0,0.5,1))
				.color_(\backgroundDisabled,Color(0.1,0.1,0.1))
				.color_(\knobDisabled,Color(0.3,0.3,0.3))
				.seqItems_(seqViews);
		};
				
		// pos ColorAdaptor
		gui[\posColorAdaptor]=MVC_ColorAdaptor(models[\pos])
			.views_(seqViews)
			.colorIndex_(\knobBorder)
			.color_(\on,Color.white)
			.valueDo_(models[\pos].value); // this shoud be in adaptor
				
		// 4.ruler as a MVC_FuncAdaptor to format pins to model
		gui[\buttonStepsFuncAdaptor]=MVC_FuncAdaptor(spModels[4])
			.func_{|me,val|
				{
					steps.do{|x|
						var c,col;
						//c=((x)%val).map(0,val,0.25,0.5);
						c= (x.div(val)%2).map(0,1,0.25,0.5); // this one looks better
						seqViews[x]
							.color_(\background,Color(0,0,0,c));
					};
				}.defer;
			}.freshAdaptor;
			
		// writeModel ColorAdaptor
		gui[\writePositionAdaptor]=MVC_ColorAdaptor(writeModel)
			.views_(seqViews)
			.colorIndex_(\numberUp)
			.color_(\on,Color.white)
			.color_(\off,Color.black)
			.valueDo_(models[\pos].value); // this shoud be in adaptor

		if (controls) {
		
			// 4.ruler
			MVC_NumberCircle(spModels[4], window,
				Rect(l+os+(steps*sw)+6,t,18,18),gui[\theme2]);
		
			//3.steps
			MVC_NumberCircle(spModels[3], window,
				Rect(l+os+(steps*sw)+6+20+20,t,18,18),gui[\theme2]);
	
			// 6.speed
			MVC_NumberCircle(spModels[6], window,
				Rect(l+os+(steps*sw)+6+20,t,18,18),gui[\theme2]);
				
		};
	
	}
	
	// set latch
	latch_{|bool|
		latch=bool.isTrue;
		spModels[6].enabled_(latch.not);
		
	}
	
}
