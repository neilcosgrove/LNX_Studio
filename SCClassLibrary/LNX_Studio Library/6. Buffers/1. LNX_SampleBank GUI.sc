// GUI support //////////////////////////////////////////////////////////////////////////

+ LNX_SampleBank {

	// GUI widgets ///////////////////////////////////////////////////////////////////////////
	// now i can add a samplesWidgets
	// what about later when i want to do this in other gui's? needs to true mvc some how.
	//////////////////////////////////////////////////////////////////////////////////////////

	// calls update func in all gui's
	*callUpdate{|buffer| {updateFuncs.do{|f| f.value(buffer)}}.defer; }

	window_{|argWindow| window = argWindow }

	isOpen{ ^((window.notNil)and:{window.isClosed.not}) }

	// select the sample in this gui
	selectSample{|i,focus=false,send=true,update=true|
		i=i.clip(0,samples.size-1);
		selectedSampleNo=i;
		selectedAction.value(this,selectedSampleNo,send,update);
		// + update gui
		guiList.do{|gui,j|
			if (i==j) {
				gui[\compositeView].color_(\background, Color.new255(232,232,232));

				if (gui[\compositeView2].notNil) {
					gui[\compositeView2].color_(\background, Color.new255(232,232,232));
				};

				if (focus) {gui[\mouseActions].focus};
			}{
				gui[\compositeView].color_(\background, Color.new255(173,173,173));

				if (gui[\compositeView2].notNil) {
					gui[\compositeView2].color_(\background, Color.new255(173,173,173));
				};

			};
		};
	}

	// used to update the selected sample in GSRhythm
	updateSelectedSample{|i,update=true|
		i=i.clip(0,samples.size-1);
		selectedSampleNo=i;
		if (update) { selectedAction.value(this,selectedSampleNo) };
	}

	// used to update the sample name list in GSRhythm
	updateList{|send=true, updateBank=true| itemAction.value(this,this.names,send,updateBank) }

	// this is called by putLoadListURL
	updateGUI{|updateBank=true| this.updateList(false,updateBank) }

	// swap samples i & j (this is really move not swap!)
	guiSwap{|i,j| api.groupCmdOD(\netSwap,i,j) }

	// net version of above
	netSwap{|i,j|
		i=i.asInt;
		j=j.asInt;

		guiList     = guiList.move(i,j);
		samples     = samples.move(i,j);
		metaModels  = metaModels.move(i,j);
		otherModels  = otherModels.move(i,j);

		guiList.do{|gui,i| gui[\i]=i }; // aline gui index numbers

		this.adjustViews;
		this.updateList(false);
	}

	// play the buffer
	play{|i,loop=false|
		var otherModel = otherModels[i];

		if (samples[i].isLoaded.not) {^nil}; // drop out

		this.stop;

		lastModel=otherModel;

		lastSynth=samples[i].play(loop, this.amp(i).dbamp, this.start(i), this.end(i));

		// for the gui playback
		task= Task({
			var startTime = AppClock.now;
			var dur = samples[i].duration;
			inf.do{
				var s   = this.start(i);
				var e   = this.end(i);
				if  ((loop.not)and:{((AppClock.now-startTime)>(dur*(1-s)))}) {
					otherModel[\pos2].valueAction_(-1,0,true);
					task.stop;
				}{
					otherModel[\pos2].valueAction_(
						(AppClock.now-startTime/dur+s).wrap(s,e)
					);
				};
				(1/30).wait;
			};
		}).start(AppClock);

	}

	// stop playback
	stop{
		task.stop; // clear the playback markers just in case
		if ((lastModel.notNil)and:{lastModel[\pos2].notNil}) {
			lastModel[\pos2].valueAction_(-1,0,true);
		};
		lastSynth.free;
		lastModel = nil;
		lastSynth = nil;
	}

	// play the currently selected sample
	playSelectedSample{ if (selectedSampleNo.notNil) { this.play(selectedSampleNo)} }

	// for tuning samples
	*initUGens{|server|
		SynthDef("tune",{ arg freq=440, amp=1;
			Out.ar(0, Mix( LFTri.ar(freq * [1,2,3], 0, amp* [0.1,0.055,0.035]) ).dup);
		}).send(server);
	}

	// adjust the position of the gui items, i.e when moving or deleting
	adjustViews{
		var w = window.bounds.width;
		var h = 33;
		guiList.do{|gui,i|
			var t;
			i = gui[\i];
			t = i * h;
			gui[\compositeView].bounds_(Rect(0,t,w,h-1));

			// and scCode interface list if it exists
			if (gui[\compositeView2].notNil) {
				var w = window2.bounds.width;
				var h = 20;
				var t = i * h;
				gui[\compositeView2].bounds_(Rect(0,t,w,h-1));
			};
		};
	}

	// open the metadata editor for this sample
	openSelectedMeta{|window|
		if (this.size>0) {
			this.openMetadataEditor(window, selectedSampleNo);
		}
	}

	// create the widgets for the sampleLIst item i
	addSampleWidgets{|i,select=true|
		var buf = samples[i];
		var h = 33;
		var t = i * h;
		var w = window.bounds.width;
		var gui=();
		var moved=false;

		var playingSynth;

		h=h-1;

		gui[\i]=i; // used to store i because it can change

		// the composite view
		gui[\compositeView] = MVC_CompositeView(window, Rect(0,t,w,h-1), false)
			.hasBorder_(false)
			.color_(\background, Color(1,1,1,0.3));

		// the name
		gui[\name] = MVC_StaticText(gui[\compositeView],Rect(2,0,w-4,18))
			.string_(buf.name)
			.shadow_(false)
			.color_(\string,Color.black)
			.font_(Font("Helvetica",12));

		// % complete
		gui[\percentageComplete] = MVC_DownloadStatus(gui[\compositeView], Rect(2,19,w-8,10),
				buf.models[\percentageComplete])
			.locked_(true);

		gui[\line] = MVC_PlainSquare(gui[\compositeView], Rect(0,h,w,1))
			.color_(\on,Color.black)
			.color_(\off,Color.black);

		gui[\line2] = MVC_PlainSquare(gui[\compositeView], Rect(0,0,w,1))
			.color_(\on,Color(1,1,1,0.33))
			.color_(\off,Color(1,1,1,0.33));

		// this is over the top
		gui[\mouseActions] = MVC_UserView(gui[\compositeView], Rect(0,0,w,h))
			.canFocus_(true)
			.focusColor_(Color.clear)
			.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				//[i,me, x, y, modifiers, buttonNumber, clickCount].postln;
				if (clickCount==2) {
					this.openMetadataEditor(window.parent, gui[\i]);
					moved=true;
				}{
					this.selectSample(gui[\i]); // for gui
					this.updateSelectedSample(gui[\i]);

					// used only in scCode at mo
					if (window2.notNil) {
						selectSampleFuncs.do{|func| func.value(selectedSampleNo) }
					}{
						// i need to document why is this here? to check
						selectMeFunc.value(selectedSampleNo);
						moved=false;
					};
				}
			}
			.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				var j=(gui[\i]+((y/h).floor)).clip(0,samples.size-1);
				if (gui[\i]!=j) {
					moved=true;
					this.guiSwap(gui[\i],j);
					this.updateSelectedSample(j);
				};
			}
			.mouseUpAction_{|me, x, y, modifiers, buttonNumber, clickCount|
				if (moved.not) {
					if (LNX_WebBrowser.preview) {
						this.playSelectedSample;
					};
				};
				moved=false;
			}
			.keyDownAction_{|me, char, modifiers, unicode, keycode, key|
				// 49 space, 36 return, 51 delete, up,down,left,right 126,125,123,124
				// @TODO: key Qt "key" codes
				switch (keycode)
					{49} { }
					{36} {
						this.selectSample(gui[\i]+1,true);
						this.updateSelectedSample(gui[\i]+1);
						selectMeFunc.value(selectedSampleNo);
						this.playSelectedSample;
					}
					{51} {
						this.guiRemove(gui[\i]);
						this.selectSample(gui[\i],true); // for gui
						selectMeFunc.value(selectedSampleNo);
						this.updateSelectedSample(gui[\i]);
					}
					{126} {
						this.selectSample(gui[\i]-1,true);
						selectMeFunc.value(selectedSampleNo);
						this.updateSelectedSample(gui[\i]-1);
					}
					{125} {
						this.selectSample(gui[\i]+1,true);
						selectMeFunc.value(selectedSampleNo);
						this.updateSelectedSample(gui[\i]+1);
					}
					{123} {}
					{124} {}
					{};
			};

		// the 2nd list in SCCode

		if (window2.notNil) {
			var h = 20;
			var t = i * h;
			var w = window2.bounds.width;


			gui[\numberBoxTheme] = (
				font_:Font("Helvetica",12),
				colors_:(\background:Color(0,0,0,0),\typing:Color.orange/2),
				visualRound_:0.01,
				showBorder_:false,
				mouseDownAction_:{ this.allInterfacesSelect(gui[\i]) }
			);

			// the composite view
			gui[\compositeView2] = MVC_CompositeView(window2, Rect(0,t,315,h), false)
				.hasBorder_(false)
				.color_(\background, Color(1,1,1,0.3));

			gui[\line] = MVC_PlainSquare(gui[\compositeView2], Rect(0,0,w,1))
				.color_(\on,Color.black)
				.color_(\off,Color.black);

			gui[\line2] = MVC_PlainSquare(gui[\compositeView2], Rect(0,1,w,1))
				.color_(\on,Color(1,1,1,0.33))
				.color_(\off,Color(1,1,1,0.33));

			// the name
			gui[\name] = MVC_Text(gui[\compositeView2],Rect(2,2,200-16-15,17))
				.canFocus_(true)
				.clipChars_(true)
				.string_(buf.name)
				.shadow_(false)
				//.hasBorder_(true)
				.color_(\string,Color.black)
				.font_(Font("Helvetica",12))
				.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					this.allInterfacesSelect(gui[\i]);
					moved=false;
				}
				.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					var j=(gui[\i]+((y/h).floor)).clip(0,samples.size-1);
					if (gui[\i]!=j) {
						moved=true;
						this.guiSwap(gui[\i],j);
						this.updateSelectedSample(j);
					};
				}
				.mouseUpAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					if (moved.not) {
						if (LNX_WebBrowser.preview) {
							//this.playSelectedSample;
						};
					};
					moved=false;
				}

				.keyDownAction_{|me, char, modifiers, unicode, keycode, key|
					// 49 space, 36 return, 51 delete, up,down,left,right 126,125,123,124
					switch (keycode)
						{49} { // space
							this.playSelectedSample
						}
						{36} { // return
							this.allInterfacesSelect(
								(selectedSampleNo+1).asInt.wrap(0,this.size.asInt-1) ,true);
							this.playSelectedSample;
							if (((window2.bounds.origin_(0@0).moveBy
								(*window2.visibleOrigin.asArray)).contains(
								guiList[selectedSampleNo][\compositeView2]
								.bounds.insetBy(1,1))).not) {
									window2.visibleOrigin_(0@
										(guiList[selectedSampleNo][\compositeView2]
										.bounds.top)
									);
							};
						}
						{51} { // delete
							this.guiRemove(selectedSampleNo);
							this.allInterfacesSelect(selectedSampleNo,true);
							if (guiList[selectedSampleNo].notNil) {
								guiList[selectedSampleNo][\name].focus;
							};
						}
						{126} { // up
							this.allInterfacesSelect(selectedSampleNo-1,true);
							if (((window2.bounds.origin_(0@0).moveBy
								(*window2.visibleOrigin.asArray)).contains(
								guiList[selectedSampleNo][\compositeView2]
								.bounds.insetBy(1,1))).not) {
									window2.visibleOrigin_(0@
										(guiList[selectedSampleNo][\compositeView2]
										.bounds.top)- (h*4) + 1);
							};
						}
						{125} { // down
							this.allInterfacesSelect(selectedSampleNo+1,true);
							if (((window2.bounds.origin_(0@0).moveBy
								(*window2.visibleOrigin.asArray)).contains(
								guiList[selectedSampleNo][\compositeView2]
								.bounds.insetBy(1,1))).not) {
									window2.visibleOrigin_(0@
										(guiList[selectedSampleNo][\compositeView2]
										.bounds.top)
									);

							};
						}
						{123} {} // left
						{124} {} // right
				};

			// the sample amp
			gui[\listAmp] = MVC_NumberBox(gui[\compositeView2],metaModels[i][\amp],
						Rect(186-15,2, 61, 16),gui[\numberBoxTheme])
				.postfix_(" db")
				.step_(1)
				.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					this.allInterfacesSelect(gui[\i]);
					this.play( gui[\i], true);
					speakerIcon.color_(\iconUp,Color.green);
				}.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					lastSynth.set(\mul,me.value.dbamp);
					//lastSynth=samples[i].play(loop, this.amp(i).dbamp
					//playingSynth.set(\freq,me.value.midicps);
				}
				.mouseUpAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					this.stop;
					speakerIcon.color_(\iconUp,Color.black);
				};

			// the sample pitch
			gui[\listPitch] = MVC_NumberBox(gui[\compositeView2],metaModels[i][\pitch],
						Rect(247-15, 2, 75, 16),gui[\numberBoxTheme])
				.postfixFunc_{|val| (val.asNote)++"  "}
				.step_(1)       // keyboard
				.resoultion_(50) // mouse
				.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					this.allInterfacesSelect(gui[\i]);
					this.play( gui[\i], true);
					playingSynth=Synth(\tune,[\freq,me.value.midicps,\amp,0.5]);
					speakerIcon.color_(\iconUp,Color.green);
				}.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					playingSynth.set(\freq,me.value.midicps);
				}
				.mouseUpAction_{|me, x, y, modifiers, buttonNumber, clickCount|
					this.stop;
					playingSynth.free;
					playingSynth=nil;
					speakerIcon.color_(\iconUp,Color.black);
				};

		};

		window.visibleOrigin_(0@((gui[\i]+1)*h));
		guiList = guiList.add(gui);
		if (select) { this.selectSample(gui[\i],false,false)};

	}

	// select sample
	allInterfacesSelect{|i,send=false,update=true|
		this.selectSample(i,false,send,update);
		this.updateSelectedSample(i,update:false);
		selectSampleFuncs.do{|func| func.value(i,update:false) };	// this is a problem
	}


	// gui call for setting the interval
	guiInterval{|parentWindow|

		var window, scrollView;

		window = MVC_ModalWindow(parentWindow, 195@90);
		scrollView = window.scrollView;

		// the interval
		MVC_NumberBox(iModel, scrollView, Rect(68, 19, 43, 19))
			.label_("Interval between samples")
			.labelShadow_(false)
			.color_(\label,Color.black);

		// Cancel
		MVC_OnOffView(scrollView,Rect(5, 44, 55, 20),"Cancel")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 window.close };

		// Ok
		MVC_OnOffView(scrollView,Rect(119, 44, 50, 20),"Set")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{
				this.setInterval(iModel.value.asInt);
				window.close;
			};

	}

	// set the pitch interval between each sample
	setInterval{|interval|
		if (this.notEmpty) {
			var s = (selectedSampleNo?0).asInt;
			var v = this.pitch(s);
			this.size.do{|i| this.pitch_(i, (i-s) * interval + v ) };
		}
	}

	// *** Deleting while someone else on collaboration is still using will cause a crash !!!1

	// MetaData Widgets //////////////////////////////////////////////////////////////////////
	// this need to be more modular
	// so i can use anywhere
	//////////////////////////////////////////////////////////////////////////////////////////

	// i can have an option to add multiple of the same so we can use many offset
	// play and edit can just be buttons in the browser gui thinie

	// adding a download from search in the metaeditor doesn't update when loaded
	// use select same item when finishing downloads

	openMetadataEditor{|window,i,search=false,webBrowser,argColors,x=0,interface|

		var buffer, models, otherModel, size, numChannels,  gui, colors, width, zoom, offset,
			setVarsFunc, setModelsFunc, selectSampleFunc, lastPlayValue=false,
			pos=(-1), pos2=(-1), scrollTask, moveIDX=0, status=(-5), mvcWindow;
		var editMode 	= -1; // -1:doNothing, 0:start, 1:end, 2:addMaker, 3:moveMaker 4:deleteMarker
		var markerIndex = nil;
		var startOrEnd	= \start;

		// used after a buffer has loaded to update gui
		var updateFunc={|buf|
			if (buf==buffer) {
				setVarsFunc.value;
				setModelsFunc.value;
				gui[\sampleView].refresh;
				gui[\posWaveView].refresh;
			};
		};

		// tap tempo
		var tapTempo = LNX_TapTempo()
			.tapFunc_{|me, bpm| if (models[\bpm].notNil) { models[\bpm].valueAction_(bpm.round(0.1)) } }
			.firstTapFunc_{|me|
				if (this.notEmpty) {this.play(i, this.loop(i).isTrue)}
			}
			.stopFunc_{|me|
				if (this.notEmpty) { this.stop }
			};

		mvcWindow = window;
		if (window.isKindOf(MVC_Window)) { window=window.view };

		updateFuncs = updateFuncs.add(updateFunc);

		i=i.asInt.clip(0,this.size-1);

		// Functions used with the GUI

		// set the vars
		setVarsFunc={
			if (this.notEmpty) {
				buffer = samples[i];
				models = metaModels[i];
				otherModel = otherModels[i];
				if (buffer.buffer.isNil) {
					numChannels = 1;
					size = 1;
				}{
					numChannels = buffer.numChannels;
					size = (buffer.buffer.sampleData.size).div(numChannels);
					// i'm not holding here
				};
			}{
				// this is a guess
				buffer = ();
				models = ();
				otherModel = ();
				numChannels = 1;
				size = 1;
			}
		};

		setVarsFunc.value;

		// set the models (used when selecting different samples and the gui needs updating)
		setModelsFunc={
			if (this.notEmpty) {
				// when there are samples in the bank
				gui[\name].model_(models[\name]);
				gui[\sampleStartAdaptor].model_(models[\start]);
				gui[\sampleEndAdaptor  ].model_(models[\end]);
				gui[\refreshOnlyModel].model_(refreshOnlyModel);
				if (gui[\pitch   ].notNil) {gui[\pitch   ].model_(models[\pitch   ])};
				if (gui[\start   ].notNil) {gui[\start   ].model_(models[\start   ])};
				if (gui[\end     ].notNil) {gui[\end     ].model_(models[\end     ])};
				if (gui[\bpm     ].notNil) {gui[\bpm     ].model_(models[\bpm     ])};
				if (gui[\velocity].notNil) {gui[\velocity].model_(models[\velocity])};
				if (gui[\length  ].notNil) {gui[\length  ].model_(models[\length  ])};
				gui[\loop].model_(models[\loop]);
				gui[\samplePosAdaptor ].model_(otherModel[\pos ]);
				gui[\samplePosAdaptor2].model_(otherModel[\pos2]);
				gui[\path].string_(buffer.url);
				gui[\amp].model_(models[\amp]);

				gui[\percentageComplete].model_(
					(buffer.models.notNil).if{buffer.models[\percentageComplete]}{-5.asModel});
				zoom.controlSpec_([ (  (width/16/size).clip(0,1)   ),1,2]);
				if (buffer.isLoaded) {
					gui[\sampleRate ].string_((buffer.sampleRate/1000).asString+"kHz");
					gui[\duration   ].string_((buffer.duration.round(0.01)).asString+"sec(s)");
					gui[\numChannels].string_((buffer.numChannels).asString);
				}{
					gui[\sampleRate ].string_("- kHz");
					gui[\duration   ].string_("- sec(s)");
					gui[\numChannels].string_("-");
				};
			}{
				// when the bank is empty
				gui[\name].model_("Press search to add samples -->".asModel);
				gui[\sampleStartAdaptor].model_(nil);
				gui[\sampleEndAdaptor].model_(nil);
				gui[\refreshOnlyModel].model_(nil);
				if (gui[\pitch   ].notNil) {gui[\pitch   ].model_(nil)};
				if (gui[\start   ].notNil) {gui[\start   ].model_(nil)};
				if (gui[\end     ].notNil) {gui[\end     ].model_(nil)};
				if (gui[\bpm     ].notNil) {gui[\bpm     ].model_(nil)};
				if (gui[\velocity].notNil) {gui[\velocity].model_(nil)};
				if (gui[\length  ].notNil) {gui[\length  ].model_(nil)};
				gui[\loop].model_(nil);
				gui[\samplePosAdaptor].model_(nil);
				gui[\percentageComplete].model_(-5.asModel);
				gui[\samplePosAdaptor2].model_(nil);
				gui[\path].string_("");
				gui[\amp].model_(nil);
				gui[\sampleRate].string_("- kHz");
				gui[\duration].string_("- sec(s)");
				gui[\numChannels].string_("-");

			}
		};

		// select sample from within this method (there is 1 @ the bottom for outside this method
		selectSampleFunc={|j,update=true|
			i=(j.asInt).wrap(0,samples.size-1);
			setVarsFunc.value;
			setModelsFunc.value;
			gui[\sampleView].refresh;
			gui[\posWaveView].refresh;
			if (update) {
				this.selectSample(i,true);
				this.updateSelectedSample(i);
			};
		};

		selectSampleFuncs = selectSampleFuncs.add(selectSampleFunc);

		// the gui itself
		gui = ();
		colors = (
			background: 		Color(59/77,59/77,59/77),
			border2: 			Color(42/83,29/65,6/11),
			border1: 			Color(0,1/103,3/77,65/77),
			menuBackground:	Color(0.9,1,1)
		) ++ ( argColors ? () );

		gui[\knobTheme1]=(
			\colors_		: (\on : Color(50/77,61/77,1),
						   \numberUp:Color.black,
						   \numberDown:Color.white),
			\numberFont_  : Font("Helvetica",11));

		gui[\infoTheme]=(
			\orientation_ : \horiz,
			\labelShadow_ : false,
			\labelFont_ : Font("Helvetica", 10),
			\shadow_ : false,
			\align_ : \left,
			\colors_	: (\string : Color.black, \label : Color.black),
			\font_ : Font("Helvetica", 10)
		);

		width = 560+x;

		// the zoom model
		zoom = [1,[ (  (width/16/size).clip(0,1)   ),1,2]].asModel.action_{|me,value|
			if (this.notEmpty) {
				gui[\sampleView].refresh;
				gui[\posWaveView].refresh;
				gui[\offset].thumbSizeAsRatio_(value);
				offset.controlSpec_([0,1-value]);
			};
		};

		// the offset model
		offset = [0, [0,1-(zoom.value)]].asModel.action_{
			if (this.notEmpty) {
				gui[\sampleView].refresh;
				gui[\posWaveView].refresh;
			}
		};

//******** needs some work

		if (window.isKindOf(MVC_CompositeView)) {
			gui[\window] = window;
			gui[\scrollView] = window;
		}{
			// the window and its scrollview
			gui[\window] = MVC_ModalWindow(mvcWindow, (600+x)@(280), colors)
				.onClose_{
					updateFuncs.remove(updateFunc);
				};
			gui[\scrollView] = gui[\window].scrollView;
		};

		// name
		gui[\name] = MVC_StaticText( gui[\scrollView], Rect(42,5,490+x,18),
						models[\name]??{"Press search to add samples -->".asModel})
			.shadow_(false)
			.align_(\center)
			.color_(\string,Color.black)
			.font_(Font("Helvetica", 13, true));

		// path
		gui[\path] = MVC_StaticText( gui[\scrollView], Rect(42,27,490+x,18))
			.string_((this.notEmpty).if {buffer.url}{""})
			.shadow_(false)
			.align_(\center)
			.color_(\string,Color.black)
			.font_(Font("Helvetica", 10))
			.mouseDownAction_{ gui[\path].color_(\string,Color.white) }
			.mouseUpAction_{
				gui[\path].color_(\string,Color.black);
				this[i].convertedPath.postln.revealInFinder
			};

		// search the web button
		if (search) {
			MVC_FlatButton(gui[\scrollView] ,Rect(546+x, 12, 20, 20),"search")
				.color_(\up,Color(35/48,35/48,40/48)/3 )
				.color_(\down,Color(35/48,35/48,40/48)/3 )
				.color_(\string,Color.white)
				.rounded_(true)
				.mode_(\icon)
				.action_{ webBrowser.open };
		};

		// up
		MVC_OnOffView(gui[\scrollView],Rect(11, 4, 20, 20),"up")
			.rounded_(true)
			.mode_(\icon)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	  if (this.notEmpty) {selectSampleFunc.value(i-1)} };

		// down
		MVC_OnOffView(gui[\scrollView],Rect(11, 27, 20, 20),"down")
			.rounded_(true)
			.mode_(\icon)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 if (this.notEmpty) {selectSampleFunc.value(i+1)} };

		// play
		gui[\play] = MVC_OnOffView(gui[\scrollView],Rect(12, 145+30, 20, 20),"play")
			.mode_(\icon)
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 if (this.notEmpty) {this.play(i, this.loop(i).isTrue)} };

		// stop
		gui[\stop] = MVC_OnOffView(gui[\scrollView],Rect(12+23, 145+30, 20, 20),"stop")
			.insetBy_(0)
			.mode_(\icon)
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 if (this.notEmpty) {this.stop} };

		// offset
		gui[\offset] = MVC_SmoothSlider(gui[\scrollView],offset,Rect(61, 175, 436+x, 20))
			.thumbSizeAsRatio_(zoom.value)
			.color_(\knob,Color(1,1,1,86/125))
			.color_(\hilite,Color(0,0,0,0.5))
			.color_(\numberUp,Color.black)
			.color_(\numberDown,Color.white);

		// zoom out
		MVC_OnOffView(gui[\scrollView],Rect(472+30+x, 145+30, 20, 20),"-")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{
				if (this.notEmpty) {
					zoom.multipyValueAction_(2);
					if ((follow.value.isFalse) or: ({pos<0 and: {pos2<0}})) {} {
						if (pos2>=0) {
							offset.value_(pos2 - (zoom.value*pos2));
						}{
							offset.value_(pos - (zoom.value*pos));
						};
					}
				}
			};

		// zoom in
		MVC_OnOffView(gui[\scrollView],Rect(472+24+30+x, 145+30, 20, 20),"+")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{
				if (this.notEmpty) {
					zoom.multipyValueAction_(0.5);
					if ((follow.value.isFalse) or: ({pos<0 and: {pos2<0}})) {
						var zoomInSite;
						if (markerIndex.notNil) { zoomInSite = models[\markers][markerIndex] };

						if (startOrEnd===\start) {
							zoomInSite = zoomInSite ? this.start(i);
						}{
							zoomInSite = zoomInSite ? this.end(i);
						};
						offset.valueAction_( zoomInSite -  (zoomInSite*(zoom.value)));
					}{
						if (pos2>=0) {
							offset.value_(pos2 - (zoom.value*pos2));
						}{
							offset.value_(pos - (zoom.value*pos));
						}
					};
				}
			};

		// zoom to fit
		MVC_OnOffView(gui[\scrollView],Rect(550+x, 145+30, 20, 20),"=")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 if (this.notEmpty) {zoom.valueAction_(1)} };

		// only update sample view (used by amp)
		gui[\refreshOnlyModel] =  MVC_FuncAdaptor ( refreshOnlyModel )
			.func_{ if (this.notEmpty) { gui[\sampleView].refresh} };

		// update the start position
		gui[\sampleStartAdaptor] = MVC_FuncAdaptor ( models[\start] )
			.func_{ if (this.notEmpty) {gui[\sampleView].refresh} };

		// update the end position
		gui[\sampleEndAdaptor] = MVC_FuncAdaptor ( models[\end] )
			.func_{ if (this.notEmpty) {gui[\sampleView].refresh} };

		// update the position and use to colour play button
		gui[\samplePosAdaptor2] = MVC_FuncAdaptor ( otherModel[\pos2] )
			.func_{|me,value,delta|
				if (this.notEmpty) {
					{
						pos2=value;
						if (follow.value.isTrue) {
							var prevoiusValue = offset.value;
							offset.value_(pos2 - (zoom.value*pos2));
							if (prevoiusValue!=offset.value) { gui[\sampleView].refresh };
						};
						gui[\posWaveView].refresh;

						// change the color of the play button
						if (((value>=0)==lastPlayValue).not) {
							lastPlayValue = (value>=0);
							if (value<0) {
								gui[\play].color_(\icon,Color.black)
										.color_(\on,Color(1,1,1,0.5))
										.color_(\off,Color(1,1,1,0.5))
								.refresh;
								lastPlayValue=false;
							}{
								gui[\play].color_(\icon,Color.green)
										.color_(\on,Color(1,1,1,0.77)*0.1)
										.color_(\off,Color(1,1,1,0.77)*0.1)
								.refresh;
								lastPlayValue=true;
							};
						}
					}.defer(delta?0);
				}
			};

		// update the position and use to colour play button (from meta player?)
		gui[\samplePosAdaptor] = MVC_FuncAdaptor ( otherModel[\pos] )
			.func_{|me,value,delta|
				if (this.notEmpty) {
					if (otherModel[\pos2].value.notNil) {
						if (otherModel[\pos2].value<0) {
							{
								pos=value;
								if (follow.value.isTrue) {
									var prevoiusValue = offset.value;
									offset.value_(pos - (zoom.value*pos));
									if (prevoiusValue!=offset.value) { gui[\sampleView].refresh };
								};
								gui[\posWaveView].refresh;

								// change the color of the play button
								if (((value>=0)==lastPlayValue).not) {
									lastPlayValue = (value>=0);
									if (value<0) {
										gui[\play].color_(\icon,Color.black)
												.color_(\on,Color(1,1,1,0.5))
												.color_(\off,Color(1,1,1,0.5))
										.refresh;
										lastPlayValue=false;
									}{
										gui[\play].color_(\icon,Color.green)
												.color_(\on,Color(1,1,1,0.77)*0.1)
												.color_(\off,Color(1,1,1,0.77)*0.1)
										.refresh;
										lastPlayValue=true;
									};
								}
							}.defer(delta?0);
						}{
							{ pos=value }.defer(delta?0);
						}
					}
				}
			};

		// % complete
		gui[\percentageComplete] = MVC_FuncAdaptor(
				(buffer.models.notNil).if{buffer.models[\percentageComplete]}{-5.asModel})
			.func_{|me,value,delta|
				status = value;
				gui[\sampleView].refresh;
		};

		// the sample view
		gui[\sampleView] = MVC_UserView(gui[\scrollView],Rect(10,50,width,120))

			.drawFunc_{|me|
				var amp = (models[\amp].value?0).dbamp;
				var w = me.bounds.width;
				var h = me.bounds.height;
				var h2= h/2;
				var channelsToDraw=numChannels.clip(1,2);

				MVC_LazyRefresh.incRefreshN(2);

				width = w; // incase of resize

				// the background
				Pen.width_(3);
				Pen.smoothing_(true);

				Color(0,0,0).set;
				Pen.strokeRect(Rect(0,0,w,h));

				Color(0,0,0,0.75).set;
				Pen.fillRect(Rect(0,0,w,h));

				w=w-4;

				Pen.smoothing_(false);
				Pen.width_(1);

				Color.black.set;

				Pen.moveTo(2@(h2.asInt));
				Pen.lineTo(w@(h2.asInt));
				Pen.stroke;

				(Color(0.8,0.8,0.8,0.8)/3).set;

				Pen.moveTo(2@(h2.asInt+1));
				Pen.lineTo(w@(h2.asInt+1));
				Pen.stroke;

				if (channelsToDraw==2) {
					Color.black.set;
				}{
					Color(0,0,0,0.33).set;
				};

				Pen.moveTo(2@((h*0.75).asInt));
				Pen.lineTo(w@((h*0.75).asInt));
				Pen.moveTo(2@((h*0.25).asInt));
				Pen.lineTo(w@((h*0.25).asInt));
				Pen.stroke;

				if (channelsToDraw==2) {
					(Color(0.8,0.8,0.8,0.8)/3).set;
				}{
					Color(0.8,0.8,0.8,0.1).set;
				};

				Pen.moveTo(2@((h*0.75+1).asInt));
				Pen.lineTo(w@((h*0.75+1).asInt));
				Pen.moveTo(2@((h*0.25+1).asInt));
				Pen.lineTo(w@((h*0.25+1).asInt));
				Pen.stroke;

				if (this.notEmpty) {
					var sampleData; // sampleData, should lead to LNX_BufferArray.sampleData
					var z = zoom.value;
					var o = offset.value;
					var w2= z/w*size;
					var start = this.actualStart(i);
					var end = this.actualEnd(i);
					var y;
					var visualOffset;

					// show selected regions, used in StrangeLoop
					if (selectedNotes.size>0) {
						selectedNotes.do{|i|
							var x1, x2;
							i=i%(models[\enabledMarkers].size+1); 								// wrap to list size
							if (i==0) {
								x1 = start * w / z - ( o * w / z) + 2; 							// use start
							}{
								x1 = models[\enabledMarkers].at(i-1) * w / z - ( o * w / z) + 2;// x1 is a marker
							};
							if (i==(models[\enabledMarkers].size)) {
								x2 = end * w / z - ( o * w / z) + 2; 							// use end
							}{
								x2 = models[\enabledMarkers].at(i) * w / z - ( o * w / z) + 2;	// x2 is a marker
							};
							if  ( ( ((x1<0) && (x2<0)) || ((x1>w) && (x2>w)) ).not) {			// is it on screen
								Color(1,1,1,0.2).set;
								Pen.fillRect(Rect(x1,0,x2-x1,h));								// draw it
							};
						};
					};

					if (buffer.buffer.notNil) {

						// sampleData, should lead to LNX_BufferArray.sampleData
						sampleData=buffer.buffer.sampleData;

						if (sampleData.notNil) {

							// the wave form
							Pen.smoothing_(true); // this can be false when moving
							Pen.width_(1.5);
							Color(0.7,0.8,1,0.8).set;

							channelsToDraw.do{|channelNo|
								var h4, h3, visDiff;
								h4 = h2 / channelsToDraw;
								if (channelsToDraw==2) { h3 = h2 * ( (channelNo==0).if(0.5,1.5)) }{ h3 = h2 };
								visualOffset = (o * size).round(w2*2); // *2 because w/2 in below do

								Pen.moveTo(2@((sampleData.atInt(visualOffset.asInt, numChannels, channelNo)*amp )*h4+h3));

								(w/2).asInt.do{|i|
									i=i*2;
									Pen.lineTo((i+2)@((
										sampleData.atInt(
											(( i * w2 ) + visualOffset).asInt, numChannels, channelNo
										)*amp
									)*h4+h3));
								};
								Pen.stroke;
							};

						};
					};

					Color.white.set;

					// the start index
					start = start * w / z - ( o * w / z) + 2;
					if ((start>=0)&&(start<=(w+2))) {
						Pen.moveTo(start@(h-2));
						Pen.lineTo(start@1);
						Pen.stroke;
						Pen.moveTo(start@1);
						Pen.lineTo((start+7)@6);
						Pen.lineTo(start@11);
						Pen.fill;
					};

					// the end index
					end = end * w / z - ( o * w / z) + 2;
					if ((end>=0)&&(end<=(w+2+15))) {
						Pen.moveTo(end@(h-2));
						Pen.lineTo(end@1);
						Pen.stroke;
						Pen.moveTo(end@1);
						Pen.lineTo((end-7)@6);
						Pen.lineTo(end@11);
						Pen.fill;
					};

					Pen.font_(Font.sansSerif(9));

					models[\enabledMarkers].do{|x,i|
						var rect;
						x = x * w / z - ( o * w / z) + 2;
						if ((x>=(-15))&&(x<=w)) { // only draw if in view bounds
							if ((i+models[\firstMarker])==markerIndex)
								{ Color(1,1,1,0.6).set } { Color(0.8,0.8,1,0.5).set };
							Pen.moveTo(x@(h-2));
							Pen.lineTo(x@1);
							Pen.stroke;
							rect = Rect(x,h-14,15,12);
							Pen.fillRect(rect);
							Color.black.set;
							Pen.stringCenteredIn((i+1).asString,rect);
						};
					};

					models[\disabledMarkers].do{|x,i|
						x = x * w / z - ( o * w / z) + 2;
						if ((x>=(-15))&&(x<=w)) { // only draw if in view bounds
						if (i==markerIndex) { Color(1,1,1,0.6).set } { Color(0.8,0.8,1,0.5).set };
							Pen.moveTo(x@(h-2));
							Pen.lineTo(x@1);
							Pen.stroke;
						};
					};

					// faded start
					if (start>0) {
						Color(0,0,0,0.5).set;
						Pen.fillRect(Rect(0,0,start-1,h));
					};

					// faded end
					if (end<w) {
						Color(0,0,0,0.5).set;
						Pen.fillRect(Rect(end+1,0,w-end,h));
					};

					sampleData=nil; // release so it can be freed

				};

				if (buffer.source===\new) {
					Pen.smoothing_(true);
					Color(1,1,1,0.5).set;
					Pen.fillRect(Rect(w-33,h-16,33,14));
					Pen.fillColor_(Color.black);
					Pen.font_(Font("Helvetica",12,true));
 					Pen.stringCenteredIn("New",Rect(w-33,h-15,33,14))
				};

				if (buffer.source===\temp) {
					Pen.smoothing_(true);
					Color(1,1,1,0.5).set;
					Pen.fillRect(Rect(w-33,h-16,33,14));
					Pen.fillColor_(Color.black);
					Pen.font_(Font("Helvetica",12,true));
 					Pen.stringCenteredIn("Temp",Rect(w-33,h-16,33,14))
				};

				if (status!=(-5)) {
					if (status>=0) {
						Pen.smoothing_(true);
						Pen.fillColor_(Color.white);
						Pen.font_(Font("Helvetica",12));
						Pen.stringCenteredIn ("Downloading "++(status.asInt)++"%",Rect(0,0,w,h));
					};
					if (status==(-1)) {
						Pen.smoothing_(true);
						Pen.fillColor_(Color.white);
						Pen.font_(Font("Helvetica",12));
						Pen.stringCenteredIn ("Connecting.",Rect(0,0,w,h));
					};
					if (status==(-2)) {
						Pen.smoothing_(true);
						Pen.fillColor_(Color.white);
						Pen.font_(Font("Helvetica",12));
						Pen.stringCenteredIn ("Converting.",Rect(0,0,w,h));
					};
					if (status==(-4)) {
						Pen.smoothing_(true);
						Pen.fillColor_(Color.white);
						Pen.font_(Font("Helvetica",12));
						Pen.stringCenteredIn ("Download Failed.",Rect(0,0,w,h));
					};
					if (status==(-4.5)) {
						Pen.smoothing_(true);
						Pen.fillColor_(Color.white);
						Pen.font_(Font("Helvetica",12));
						Pen.stringCenteredIn ("File Not Found.",Rect(0,0,w,h));
					};
				};

			};

			// the was pos view
			gui[\posWaveView] = MVC_UserView(gui[\scrollView],Rect(10,50,width,120))
				.drawFunc_{|me|
					var w = me.bounds.width;
					var h = me.bounds.height;
					var z = zoom.value;
					var o = offset.value;
					var y;
					MVC_LazyRefresh.incRefresh;
					if(pos>=0) {
						// the playbackIndex index
						y = pos * w / z - ( o * w / z) + 2;
						Color.yellow.set;
						Pen.moveTo(y@1);
						Pen.lineTo(y@(h-2));
					};
					if(pos2>=0) {
						y = pos2 * w / z - ( o * w / z) + 2;
						Color.yellow.set;
						Pen.moveTo(y@1);
						Pen.lineTo(y@(h-2));
					};
					Pen.stroke;
			}
			.mouseDownAction_{|me,x,y,modifiers, buttonNumber, clickCount|
				var w 		 = me.bounds.width-4;
				var z 		 = zoom.value;
				var o 		 = offset.value;
				var index	 = ((x-2 ).clip(0,w) + ( o * w / z) * z / w).clip(0,1);
				var minIndex = ((x-10).clip(0,w) + ( o * w / z) * z / w).clip(0,1);
				var maxIndex = ((x+6 ).clip(0,w) + ( o * w / z) * z / w).clip(0,1);

				MVC_LazyRefresh.mouseDown;

				if (this.notEmpty) {

					// // -1:doNothing, 0:start, 1:end, 2:addMaker, 3:moveMaker 4:deleteMarker
					editMode    = -1;
					markerIndex = nil;

					if ( models[\start].value.inclusivelyBetween(minIndex,maxIndex)) {
						editMode   = 0;
						startOrEnd = \start;
					};
					if ( models[\end  ].value.inclusivelyBetween(minIndex,maxIndex)) {
						editMode   = 1;
						startOrEnd = \end;
					};

					if (clickCount==2) {
						editMode = 2;
						this.addMarker(selectedSampleNo,index);
						markerIndex = models[\markers].indexOf(index);
						gui[\sampleView].refresh;
						this.updateMarkers(i,true); // this will sort as well
					};

					if (editMode == (-1)) {
						models[\markers].do{|val,i|
							if (val.inclusivelyBetween(minIndex,maxIndex)) {
								editMode    = 3;
								markerIndex = i;
							};
						};
					};

					if (editMode==0) {models[\start].valueAction_(index,0,true)};
					if (editMode==1) {models[\end  ].valueAction_(index,0,true)};

					gui[\sampleView].refresh;

					moveIDX=0;
			         // scroll when off edge
					scrollTask=Task({inf.do{
						(1/30).wait;
						if ((follow.value.isFalse) or: ({pos<0 and: {pos2<0}})) {
							if (moveIDX!=0) {
								offset.valueAction_(offset.value+(zoom.value/20*moveIDX));
								// move start
								if (editMode==0) {
									models[\start].valueAction_(models[\start].value+(zoom.value/20*moveIDX));
								};
								// move end
								if (editMode==1) {
									models[\end  ].valueAction_(models[\end  ].value);
								};
								// move marker
								if (editMode==3) {
									// this needs networking
									var index = (models[\markers][markerIndex]+(zoom.value/20*moveIDX)).clip(0,1);
									models[\markers][markerIndex] = index;
									models[\markers] = models[\markers];
									this.updateMarkers(i,true); // this will sort as well
									markerIndex = models[\markers].indexOf(index);
									gui[\sampleView].refresh;
									gui[\posWaveView].refresh;
								};
							};
						};
					}}).start(AppClock);

				}
			}
			.mouseMoveAction_{|me,x,y|
				var w 	  = me.bounds.width-4;
				var z	  = zoom.value;
				var o	  = offset.value;
				var index = ((x-2).clip(0,w) + ( o * w / z) * z / w).clip(0,1);

				if (this.notEmpty) {
					moveIDX=0;
					if (x<0) { moveIDX = -1 * ((x).abs/40+0.25) };
					if (x>w) { moveIDX = 1 * ((x-w).abs/40+0.25) };
					if (editMode==0) {
						models[\start].valueAction_(index,0,true);
						if (lastSynth.notNil) { lastSynth.set(\start,index) };
					};
					if (editMode==1) {
						models[\end  ].valueAction_(index,0,true);
						if (lastSynth.notNil) { lastSynth.set(\end,index) };
					};
					if (editMode==3) {
						// this needs networking
						models[\markers][markerIndex] = index;
						models[\markers] = models[\markers];
						this.updateMarkers(i,true); // this will sort as well
						markerIndex = models[\markers].indexOf(index);
						gui[\sampleView].refresh;
					};


				}
			}
			.mouseUpAction_{|me,x,y|
				scrollTask.stop;
				scrollTask=nil;
				//markerIndex = nil;
				//gui[\sampleView].refresh;
				MVC_LazyRefresh.mouseUp;
			}
			.mouseWheelAction_{|me,x,y,modifiers, dx, dy|
				var move;

				if (dx<0) {
					move = 1 * (dx.abs/20+0.25);
					offset.valueAction_(offset.value+(zoom.value/20*move));
				};

				if (dx>0) {
					move = -1 * (dx.abs/20+0.25);
					offset.valueAction_(offset.value+(zoom.value/20*move));
				};

			}
			.keyDownAction_{|me, char, modifiers, unicode, keycode, key|
				// 27 -, 24 +, 49 space, 125 down, 126 up, 123 left, 124 right
				if ((key.isDel) && (markerIndex.notNil)) {
					if (markerIndex>(models[\markers].size-1)){
						markerIndex = nil;
					}{
						this.guiDeleteMarker(i, markerIndex);
						gui[\sampleView].refresh;
						this.updateMarkers(i,true); // this will sort as well
					};
				};

				switch (keycode)
					{27} { if (this.notEmpty) { zoom.multipyValueAction_(2) } }
					{24} {
						if (this.notEmpty) {
							var zoomInSite;
							zoom.multipyValueAction_(0.5);
							if (markerIndex.notNil) { zoomInSite = models[\markers][markerIndex] };
							if (startOrEnd===\start) {
								zoomInSite = zoomInSite ? this.start(i);
							}{
								zoomInSite = zoomInSite ? this.end(i);
							};
							offset.valueAction_( zoomInSite -  (zoomInSite*(zoom.value)));
						}
					}
					{49} { if (this.notEmpty) {this.play(this.geti(models))} }
					{123} { if (this.notEmpty) {
							offset.valueAction_(offset.value-(zoom.value/2)) }
					}
					{124} { if (this.notEmpty) {
							offset.valueAction_(offset.value+(zoom.value/2)) }
					}
					{126} { selectSampleFunc.value(i-1) }
					{125} { selectSampleFunc.value(i+1) }
			}
			.focusColor_(Color.clear)
			.canFocus_(true)
			.focus;



		if (interface.isNil) {
			if (window.isKindOf(MVC_CompositeView).not) { interface = \gsr } { interface = \sccode }
		};

		if (interface == \strangeLoop ) { // interface in strange loop
/*			// the edit mode
			gui[\amp] = MVC_MyKnob3(gui[\scrollView], editModel, Rect(95,245, 28, 28),
				gui[\knobTheme1])
				.label_("Edit mode");*/

			// length
			gui[\length]=MVC_NumberBox(gui[\scrollView],models[\length], Rect(602, 107, 42, 16))
				.resoultion_(250)
				.rounded_(true)
				.visualRound_(1)
				.label_("Length (n) beats")
				.font_(Font("Helvetica", 11))
				.color_(\focus,Color.grey(alpha:0))
				.color_(\string,Color.white)
				.color_(\typing,Color.black)
				.color_(\background,Color(46/77,46/79,72/145)/1.5);

			// follow
			gui[\follow] = MVC_OnOffView(gui[\scrollView],Rect(611, 139, 50, 20),"Follow", follow)
				.rounded_(true)
				.color_(\on,Color(50/77,61/77,1))
				.color_(\off,Color(1,1,1,0.88)/4);

			// the sample amp
			gui[\amp] = MVC_MyKnob3(gui[\scrollView], models[\amp], Rect(722, 200, 28, 28),
				gui[\knobTheme1])
				.label_("Amp");

			// bpm
			// gui[\bpm] = MVC_MyKnob3(gui[\scrollView], models[\bpm], Rect(539,282, 28, 28),
			// gui[\knobTheme1])
			// .label_("BPM");

			// tap button
			MVC_FlatButton(gui[\scrollView], Rect(720, 106, 33, 18),"Tap").downAction_{ tapTempo.tap }
				.rounded_(true)
				.font_(Font("Helvetica",12,true))
				.color_(\up,Color(50/77,61/77,1))
				.color_(\down,Color(1,1,1,0.88)/4);

			gui[\bpm]=MVC_NumberBox(gui[\scrollView],models[\bpm], Rect(674, 107, 42, 16))
				.resoultion_(25)
				.rounded_(true)
				.visualRound_(0.01)
				.label_("BPM")
				.font_(Font("Helvetica", 11))
				.color_(\focus,Color.grey(alpha:0))
				.color_(\string,Color.white)
				.color_(\typing,Color.yellow)
				.color_(\background,Color(46/77,46/79,72/145)/1.5);

			// the sample loop
			gui[\loop]= MVC_OnOffView(gui[\scrollView], models[\loop],
										Rect(697, 140, 46, 20),"Loop")
				.rounded_(true)
				.color_(\on,Color(50/77,61/77,1))
				.color_(\off,Color(1,1,1,0.88)/4);

			// sampleRate
			gui[\sampleRate] = MVC_StaticText( gui[\scrollView], Rect(322,196,65,18), gui[\infoTheme])
				.label_("Sample Rate:");

			// duration
			gui[\duration] = MVC_StaticText( gui[\scrollView], Rect(477,196,65,18), gui[\infoTheme])
				.label_("Duration:");

			//numChannels
			gui[\numChannels] = MVC_StaticText( gui[\scrollView], Rect(144,196,65,18), gui[\infoTheme])
				.label_("Num Channels:");

			if ((this.notEmpty) and:{ buffer.isLoaded}) {
				gui[\sampleRate].string_((buffer.sampleRate/1000).asString+"kHz");
				gui[\duration].string_((buffer.duration.round(0.01)).asString+"sec(s)");
				gui[\numChannels].string_((buffer.numChannels).asString);
			}{
				gui[\sampleRate].string_("- kHz");
				gui[\duration].string_("- sec(s)");
				gui[\numChannels].string_("-");
			};

		};


		if (interface == \gsr) { // interface in GSRythmn

			// sampleRate
			gui[\sampleRate] = MVC_StaticText( gui[\scrollView], Rect(297,196,65,18),
								gui[\infoTheme])
				.label_("Sample Rate:");

			// duration
			gui[\duration] = MVC_StaticText( gui[\scrollView], Rect(431,196,65,18),
								gui[\infoTheme])
				.label_("Duration:");

			//numChannels
			gui[\numChannels] = MVC_StaticText( gui[\scrollView], Rect(182,196,65,18),
								gui[\infoTheme])
				.label_("Num Channels:");

			if ((this.notEmpty) and:{ buffer.isLoaded}) {
				gui[\sampleRate].string_((buffer.sampleRate/1000).asString+"kHz");
				gui[\duration].string_((buffer.duration.round(0.01)).asString+"sec(s)");
				gui[\numChannels].string_((buffer.numChannels).asString);
			}{
				gui[\sampleRate].string_("- kHz");
				gui[\duration].string_("- sec(s)");
				gui[\numChannels].string_("-");
			};

			// the sample amp
			gui[\amp] = MVC_MyKnob3(gui[\scrollView], models[\amp], Rect(18,213, 28, 28),
				gui[\knobTheme1])
				.label_("Amp");

//			// the sample pitch
//			gui[\pitch] = MVC_MyKnob3(gui[\scrollView], models[\pitch],
//								Rect(18+(45*1),213, 28, 28),
//				gui[\knobTheme1])
//				.label_("Pitch");

			// the sample start
			gui[\start] = MVC_MyKnob3(gui[\scrollView], models[\start],
								Rect(18+(45*1),213, 28, 28),
				gui[\knobTheme1])
				.label_("Start");

			// the sample loop
			gui[\loop]= MVC_OnOffView(gui[\scrollView], models[\loop],
										Rect(265,225, 40, 20),"Loop")
				.rounded_(true)
				.color_(\on,Color(50/77,61/77,1))
				.color_(\off,Color(1,1,1,0.88)/4);

			// follow
			gui[\follow] = MVC_OnOffView(gui[\scrollView],Rect(520+x, 206, 50, 20),
									"Follow", follow)
				.rounded_(true)
				.color_(\on,Color(0.5,1,0.5,0.88))
				.color_(\off,Color(1,1,1,0.88)/4);

			// Ok
			MVC_OnOffView(gui[\scrollView],Rect(520, 230, 50, 20),"Ok")
				.rounded_(true)
				.color_(\on,Color(1,1,1,0.5))
				.color_(\off,Color(1,1,1,0.5))
				.action_{
					gui[\window].close;
					// gui.do(_.free);  // this causes a crash?
				};
		};

		if (interface == \sccode) { // interface in SCCode

			// follow
			gui[\follow] = MVC_OnOffView(gui[\scrollView],Rect(562, 200, 50, 20),"Follow", follow)
				.rounded_(true)
				.color_(\on,Color(0.5,1,0.5,0.88))
				.color_(\off,Color(1,1,1,0.88)/4);

			// the sample amp
			gui[\amp] = MVC_MyKnob3(gui[\scrollView], models[\amp], Rect(496,282, 28, 28),
				gui[\knobTheme1])
				.label_("Amp");

			// the sample pitch
			gui[\pitch] = MVC_MyKnob3(gui[\scrollView], models[\pitch], Rect(539,282, 28, 28),
				gui[\knobTheme1])
				.label_("Pitch");

			// set interval
			gui[\interval] = MVC_OnOffView(gui[\scrollView],Rect(489, 234, 77, 20),
									"Set interval")
				.rounded_(true)
				.color_(\on,Color(1,1,1,0.5))
				.color_(\off,Color(1,1,1,0.5))
				.action_{ this.guiInterval(
						window.parent.parent.parent.parent.findWindow; // should be it

					)  };

			// the sample start
			gui[\start] = MVC_MyKnob3(gui[\scrollView], models[\start], Rect(584, 282, 28, 28),
				gui[\knobTheme1])
				.label_("Start");

			// the sample loop
			gui[\loop]= MVC_OnOffView(gui[\scrollView], models[\loop],
										Rect(574, 234, 46, 20),"Loop")
				.rounded_(true)
				.color_(\on,Color(50/77,61/77,1))
				.color_(\off,Color(1,1,1,0.88)/4);

			// sampleRate
			gui[\sampleRate] = MVC_StaticText( gui[\scrollView], Rect(322,196,65,18),
										gui[\infoTheme])
				.label_("Sample Rate:");

			// duration
			gui[\duration] = MVC_StaticText( gui[\scrollView], Rect(477,196,65,18),
										gui[\infoTheme])
				.label_("Duration:");

			//numChannels
			gui[\numChannels] = MVC_StaticText( gui[\scrollView], Rect(144,196,65,18),
										gui[\infoTheme])
				.label_("Num Channels:");

			if ((this.notEmpty) and:{ buffer.isLoaded}) {
				gui[\sampleRate].string_((buffer.sampleRate/1000).asString+"kHz");
				gui[\duration].string_((buffer.duration.round(0.01)).asString+"sec(s)");
				gui[\numChannels].string_((buffer.numChannels).asString);
			}{
				gui[\sampleRate].string_("- kHz");
				gui[\duration].string_("- sec(s)");
				gui[\numChannels].string_("-");
			};

		};

		bankGUIs = bankGUIs.add(gui);

		// i'm returning extra stuff so the web browser can update gsRythmns sample only
		^[gui[\window],  this,  {|j|
			if (gui[\window].isOpen) {
				i=(j.asInt).wrap(0,samples.size-1);
				setVarsFunc.value;
				setModelsFunc.value;
				gui[\sampleView].refresh;
				gui[\posWaveView].refresh;
			};

		},

		gui; // gui accessed in strangeLoop

		];

	}

	// notes selected by pRoll in StrangeLoop
	selectedNotes_{|notes|
		selectedNotes = notes;
		{ bankGUIs.do{|gui| gui[\sampleView].refresh } }.deferIfNeeded;
	}

}

// for accessing sample data in SCLang

+ FloatArray {

	// integer index into a flat multichannel FloatArray
	atInt{|index, numChannels=1, channel=0|
		^ this.clipAt( index * numChannels + channel);
	}

	// simple linear interpolation into a flat multichannel FloatArray
	atL{|index, numChannels=1, channel=0|
		var i= index.frac;
		var index1 = index.asInteger * numChannels + channel;
		^ (( this.clipAt(index1 + numChannels) )*i)+( ( this.clipAt(index1) )*(1-i));
	}

}

+ DoubleArray {

	// integer index into a flat multichannel FloatArray
	atInt{|index, numChannels=1, channel=0|
		^ this.clipAt( index * numChannels + channel);
	}

	// simple linear interpolation into a flat multichannel FloatArray
	atL{|index, numChannels=1, channel=0|
		var i= index.frac;
		var index1 = index.asInteger * numChannels + channel;
		^ (( this.clipAt(index1 + numChannels) )*i)+( ( this.clipAt(index1) )*(1-i));
	}

}

// used for... remove gui[\i] = Number which isn't a widget. Number doesn't respond to remove.

+ Number { remove{^this} }


	