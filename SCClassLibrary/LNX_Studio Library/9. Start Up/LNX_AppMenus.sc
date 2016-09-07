
// the application menus's  /////////////////////////////////////////////////////////////

LNX_AppMenus {

	classvar >studio;

	*initClass { Class.initClassTree(Menu) }

	// new sc1.8 style menus //////////////////////////////////////////////////////////

	*menus{
		^[
			// main menu
			Menu(
				Action("About LNX_Studio",       { LNX_SplashScreen.init(studio,true) }),
				Action.separator,
				Action("Preferences", { studio.preferences }),
				Action.separator,
				Action("Hide LNX_Studio", {
					Platform.case(\osx,{
						"osascript -e 'tell application \"Finder\"' -e 'set visible of process \"LNX_Studio\" to false' -e 'end tell'".unixCmd
					});
				}).shortcut_("Ctrl+H"),
				Action.separator,
				Action("Quit",        { studio.quit }).shortcut_("Ctrl+Q")
			).title_("SuperCollider"),

			// file menu
			Menu(
				Action("Open",				{ studio.loadDialog }).shortcut_("Ctrl+O"),
				Action("Open Last Song",	{ studio.quickLoad }).shortcut_("Ctrl+Shift+O"),
				Action("Open Demo Song",	{ studio.loadDemoSong }),
				Action.separator,
				Action("Save",				{ studio.saveDialog }).shortcut_("Ctrl+S"),
				Action("Save As...",		{ studio.saveAsDialog }).shortcut_("Ctrl+Shift+S"),
				Action.separator,
				Action("Close Song", 		{ studio.guiCloseStudio }).shortcut_("Ctrl+Shift+W"),
			).title_("File"),

			// edit menu
			Menu(
				Action("Stop Audio",			{CmdPeriod.run}).shortcut_("Ctrl+."),
				Action.separator,
				Action("Copy Instrument",		{ studio.guiCopy }).shortcut_("Ctrl+Shift+C"),
				Action("Paste Instrument",		{ studio.guiPaste }).shortcut_("Ctrl+Shift+V"),
				Action("Duplicate Instrument",	{ studio.guiDuplicate }).shortcut_("Ctrl+Shift+D"),
				Action.separator,
				Action("Delete Instrument",		{ studio.guiDeleteInst }).shortcut_("Ctrl+Shift+Backspace"),
				Action.separator,
				Action("Clear Instrument Sequencer", {
					if (studio.insts.selectedInst.notNil) {
						studio.insts.selectedInst.clearSequencer
					}
				}),
				Action("Clear All Sequencers",			{ studio.insts.do(_.clearSequencer) }),
				Action("Clear Instrument Automation",	{
/*					if (studio.insts.selectedInst.notNil) {
						studio.insts.selectedInst.freeAutomation
					}*/
				}),
				Action("Clear Studio Automation",		{
					//studio.freeAutomation
				}),
				Action("Clear All Automation",			{
					//studio.freeAllAutomation
				}),
				Action.separator,
				Action("All MIDI Controls",		{ studio.editMIDIControl }).shortcut_("Ctrl+Shift+M"),

			).title_("Edit"),

			// library menu
			Menu(
				Action("Add Instrument to Library",	{ studio.guiSaveInstToLibrary }).shortcut_("Ctrl+L"),
				Action.separator,
				Action("Backup Library to Desktop", { studio.backupLibrary }),
				Action("Restore Library Defaults",	{ studio.restoreLibraryDefaults }),
				Action("Check For Library Updates",	{ studio.checkForLibraryUpdates }),
				Action("Open Library Folder",		{ studio.openLibraryFolderInOS }),
			).title_("Library"),

			// network menu
			Menu(
				Action("Open Network", 	{ studio.network.guiConnect }).shortcut_("Ctrl+N"),
				Action.separator,
				Action("Leave Colaboration",	{ studio.network.collaboration.guiLeave }),
				Action("Close Network",			{ studio.network.disconnect }).shortcut_("Ctrl+Shift+N"),
				Action.separator,
				Action("Network Prefereces",	{ studio.network.preferences }),
			).title_("Network"),

			//  dev menu
			Menu(
				Action("Code Window",{
					TextView().enterInterpretsSelection_(true).front;
				}).shortcut_("Ctrl+1"),

				Action("Recompile Class Libray",{thisProcess.platform.recompile}).shortcut_("Ctrl+K"),

				Action.separator,
				Action("Save interval / Stop",{ studio.saveInterval }),
				Action("Start Batch Recording",{
					studio.hackOn_(true);
					studio.batchOn_(true);
					studio.batch_(1);
					studio.batchFolder_(studio.batchFolder+1)
				}),
				Action("Stop Batch Recording",{
					studio.batchOn_(false);
					studio.hackOn_(false);
				}),
				Action("Reset Batch",{ studio.batchFolder_(0) }),
				Action.separator,
				Action("My Hack",{
					studio.hackOn_(true);
					studio.myHack[\window].create.front;
				}),
				Action("Index all help files",{  SCDoc.indexAllDocuments }),
				Action("Render all help files",{ SCDoc.renderAll }),
				Action("Open Browser",{ ~b=LNX_WebBrowser().open }).shortcut_("Ctrl+0"),
				Action.separator,
				Action("Quarks",{ Quarks.gui }),
				Action.separator,
				Action("MVC Verbose",{ MVC_View.verbose_(MVC_View.verbose.not) }),
				Action("MVC Show Background",{
					MVC_View.showLabelBackground_(MVC_View.showLabelBackground.not)
				}).shortcut_("Ctrl+B"),
				Action("MVC Edit / Resize",{
					if (MVC_View.editMode==false) {
						MVC_View.editResize=false;
						MVC_View.editMode_(true);
						"Edit mode: On".postln;
						"MVC_View.grid_(1)".postln;
					}{
						if (MVC_View.editResize==false) {
							MVC_View.editResize=true;
							"Edit mode: Resize".postln;
						}{
							MVC_View.editMode_(false);
							"Edit mode: Off".postln;
						};
					};
				}).shortcut_("Ctrl+R"),
				Action("ColorPicker",{ ColorPicker() }),
				Action.separator,
				Action("Server Window",{ studio.server.makeWindow }),
				Action("Network Verbose",{
					studio.network.socket.verbose_(studio.network.socket.verbose.not);
				}),
				Action.separator,
			).title_("Dev"),

			//  windows menu
			Menu(
				Action("Minimise",			{ MVC_Window.frontWindow.minimize }).shortcut_("Ctrl+M"),
				Action("Arrange",			{}),
				Action("Close Window",		{ MVC_Window.frontWindow.guiClose }),
				Action("Close All Window",	{}),
			).title_("Windows"),

			//  help menu
			Menu(
				Action("Help with LNX_Studio",		{studio.openHelp}).shortcut_("Ctrl+D"),
				Action("Help with Supercollider",	{Help.help}),
				Action.separator,
			).title_("Help"),

		];
	}

/*

			//  menu
			Menu(
				Action("",				{}).shortcut_("Ctrl+"),
				Action("",				{}),
				Action.separator,
			).title_("Name"),
*/


	///////////////////////////////////////////////////////////////////////////////////////

	// application menus for the standalone release

	*addReleaseMenus {|studio|

		// @TODO: OSX to get xplat menus?
		Platform.case(
			\osx, { LNX_AppMenus.addReleaseMenusOSX(studio) },
			{LNX_AppMenus.addReleaseMenusXPlat(studio)}
		);
	}

	*addReleaseMenusXPlat {|studio|

		var mw, i, mWidth, mHeight;

		mw = studio.mixerWindow;

		// width: 74, height: 22
		mWidth = 74;
		mHeight = 19;

		// tools menu
		i = 2;
		MVC_PopUpMenu2(mw, Rect(7 + (i * (mWidth+1)), 3, mWidth, mHeight))
			.items_(["  Tools","Add preset to all instruments", " - ",
				"Clear Instrument Sequencer", "Clear All Sequencers",
				"Clear Instrument Automation", "Clear Studio Automation",
				"Clear All Automation", " - ",
				"Master EQ", " - ",
				"Backup Library to Desktop", "Restore Library Defaults",
				"Check For Library Updates", "Open Library Folder"])
			.action_({|me,val|
				switch (me.value.asInt)
				 {0}  { studio.guiAllInstsAddPreset }
				 // -
				 {2}  {
				 	if (studio.insts.selectedInst.notNil) {
						studio.insts.selectedInst.clearSequencer
					}
				 }
				 {3}  { studio.insts.do(_.clearSequencer) }
				 // -
				 {4}  {
				 	if (studio.insts.selectedInst.notNil) {
						studio.insts.selectedInst.freeAutomation
					}
				 }
				 {5}  { studio.freeAutomation }
				 {6}  { studio.freeAllAutomation }
				 // -
				 {8}  { MasterEQ.new }
				 // -
				 {10}  { studio.backupLibrary }
				 {11}  { studio.restoreLibraryDefaults }
				 {12}  { studio.checkForLibraryUpdates }
				 {13}  { (LNX_File.prefDir+/+"Library").openOS }
				;
			})
			.revert_(true)
			.create(mw.view, true);

	}

	*addReleaseMenusOSX {|studio|

		var tools  = SCMenuGroup.new(nil, "Tools",9);

		// add LNX Help menu
		SCMenuItem('Help',"LNX_Studio Help", 0).setShortCut("d").action_{studio.openHelp};

		SCMenuItem.new(tools,  "Save").setShortCut("s").action_({ studio.saveDialog });
		SCMenuItem.new(tools,  "Open...").setShortCut("o").action_({ studio.loadDialog });
		SCMenuItem.new(tools,  "Network").setShortCut("n").action_({ studio.network.guiConnect });

		SCMenuSeparator.new(tools);

		SCMenuItem.new(tools,  "Add preset to all instruments")
			.action_{studio.guiAllInstsAddPreset};

		SCMenuItem.new(tools,  "Add all to POP")
			.action_{studio.guiAllToPOP};

		SCMenuItem.new(tools,  "Add all to POP (no FX)")
			.setShortCut("p")
			.action_{studio.guiAllToPOP(false)};

		SCMenuSeparator.new(tools);

		SCMenuItem.new(tools,  "Clear Instrument Sequencer").action_({
			if (studio.insts.selectedInst.notNil) {
				studio.insts.selectedInst.clearSequencer
			}
		});

		SCMenuItem.new(tools,  "Clear All Sequencers").action_({
			studio.insts.do(_.clearSequencer);
		});


		SCMenuItem.new(tools,  "Clear Instrument Automation").action_({
			if (studio.insts.selectedInst.notNil) {
				studio.insts.selectedInst.freeAutomation
			}
		});

		SCMenuItem.new(tools,  "Clear Studio Automation").action_({
			studio.freeAutomation;
		});

		SCMenuItem.new(tools,  "Clear All Automation").action_({
			studio.freeAllAutomation;
		});
//
//		SCMenuSeparator.new(tools);
//		SCMenuItem.new(tools,  "Master EQ").action_({MasterEQ.new});
//
		SCMenuSeparator.new(tools);
		SCMenuItem.new(tools,  "Backup Library to Desktop").action_{studio.backupLibrary};
		SCMenuItem.new(tools,  "Restore Library Defaults").action_{studio.restoreLibraryDefaults};
		SCMenuItem.new(tools,  "Check For Library Updates").action_{studio.checkForLibraryUpdates};
		SCMenuItem.new(tools,  "Open Library in Finder").action_{
			("open" + (LNX_Studio.libraryFolder).quote ).systemCmd};

//		SCMenuSeparator.new(tools);
//		SCMenuItem.new(tools, "Quit LNX_Studio").setShortCut("q").action_{
//			studio.server.quit;
//			MIDIClient.disposeClient;
//			{0.exit}.defer(0.5);
//		};
	}

	// application menus for the developer mode

	*addDeveloperMenus {|studio|

		this.initMyHack(studio);

		// @TODO: OSX to get xplat menus?
		Platform.case(
			\osx, { LNX_AppMenus.addDeveloperMenusOSX(studio) },
			{LNX_AppMenus.addDeveloperMenusXPlat(studio)}
		);
	}

	*initMyHack {|studio|
		// *** HACK *** //

		var myHack=studio.myHack;

		myHack[\netAddrModel]= "NetAddr(\"192.168.0.10\",57120)".asModel
			.actions_(\stringAction,{|me|
				myHack[\netAddr] = myHack[\netAddrModel].string.interpret;

			});

		myHack[\codeModel]=

		"{|studio,beat,latency,netAddr|

	var insts=studio.insts.visualOrder;

	if (studio.batchOn) {

		(beat%16==0).if{ studio.models[\\record].lazyValueAction_(1);  (beat/16+1).postln};
		(beat%16==15).if{ studio.models[\\record].lazyValueAction_(0) };

	};


}".asModel
			.actions_(\stringAction,{|me|
				myHack[\codeFunc] = myHack[\codeModel].string.interpret;

			});


		myHack[\startModel]= "{|studio,netAddr| netAddr.sendMsg(\\start,studio.bpm) }".asModel
			.actions_(\stringAction,{|me|
				myHack[\startFunc] = myHack[\startModel].string.interpret;
			});

		myHack[\stopModel]= "{|studio,netAddr|
netAddr.sendMsg(\\stop);

if (studio.batchOn) {
	studio.models[\\record].lazyValueAction_(0);
	if (studio.beat==0) {
		studio.makeBatch;
	};
};


			}".asModel
			.actions_(\stringAction,{|me|
				myHack[\stopFunc] = myHack[\stopModel].string.interpret;
			});

		myHack[\netAddrModel].actions[\stringAction].value;
		myHack[\codeModel   ].actions[\stringAction].value;
		myHack[\startModel  ].actions[\stringAction].value;
		myHack[\stopModel   ].actions[\stringAction].value;

		myHack[\window]=MVC_Window("My Hack",Rect(0,0,640,355))
			.color_(\background,Color(6/11,42/83,29/65))
			.minWidth_(237)
			.minHeight_(146);

		MVC_TextView(myHack[\window],myHack[\netAddrModel],Rect(15,15,610,20))
			.resize_(2)
			.label_("Network Address")
			.labelShadow_(false)
			.color_(\label,Color.black)
			.font_(Font("Helvetica", 14))
			.color_(\string,Color.black)
			.color_(\background,Color.white)
			.color_(\focus,Color(1,1,1,0.5))
			.colorizeOnOpen_(true)
			.autoColorize_(true);

		MVC_TextView(myHack[\window],myHack[\startModel],Rect(15,15+35,610,20))
			.resize_(2)
			.label_("Start")
			.labelShadow_(false)
			.color_(\label,Color.black)
			.font_(Font("Helvetica", 14))
			.color_(\string,Color.black)
			.color_(\background,Color.white)
			.color_(\focus,Color(1,1,1,0.5))
			.colorizeOnOpen_(true)
			.autoColorize_(true);

		MVC_TextView(myHack[\window],myHack[\stopModel],Rect(15,15+35+35,610,20))
			.resize_(2)
			.label_("Stop")
			.labelShadow_(false)
			.color_(\label,Color.black)
			.font_(Font("Helvetica", 14))
			.color_(\string,Color.black)
			.color_(\background,Color.white)
			.color_(\focus,Color(1,1,1,0.5))
			.colorizeOnOpen_(true)
			.autoColorize_(true);

		MVC_TextView(myHack[\window],myHack[\codeModel],Rect(15,15+35+35+35,610,300-35-45))
			.label_("Beat")
			.labelShadow_(false)
			.color_(\label,Color.black)
			.font_(Font("Helvetica", 14))
			.resize_(5)
			.color_(\string,Color.black)
			.color_(\background,Color.white)
			.color_(\focus,Color(1,1,1,0.5))
			.colorizeOnOpen_(true)
			.autoColorize_(true);
	}

	*addDeveloperMenusXPlat {|studio|

		var myHack=studio.myHack;
		var mw, i, mWidth, mHeight;

		mw = studio.mixerWindow;

		// width: 74, height: 22
		mWidth = 74;
		mHeight = 19;

		// dev menu
		i = 3;
		MVC_PopUpMenu2(mw, Rect(7 + (i * (mWidth+1)), 3, mWidth, mHeight))
			.items_(["  Dev","Save interval / Stop",
				"Start Batch Recording", "Stop Batch Recording", "Reset Batch", " - ",
				"My Hack", "Index all help files", "Open Browser", " - ",
				"Quarks", " - ", "MVC Verbose", "MVC Show Background",
				"MVC Edit / Resize", "ColorPicker", " - ",
				"1 FPS", "12 FPS", "24 FPS", "50 FPS", " - ",
				"Server Window", "Graph: Latency & Delta", "Network Verbose"])
			.action_({|me,val|
				switch (me.value.asInt)
				 {0}  { studio.saveInterval }
				 {1}  {
				 	studio.hackOn_(true);
					studio.batchOn_(true);
					studio.batch_(1);
					studio.batchFolder_(studio.batchFolder+1)
				 }
				 {2}  {
				 	studio.batchOn_(false);
					studio.hackOn_(false);
				 }
				 {3}  { studio.batchFolder_(0) }
				 // -
				 {5} {
				 	studio.hackOn_(true);
					myHack[\window].create.front;
				 }
				 {6} { SCDoc.indexAllDocuments }
				 {7} { ~b=LNX_WebBrowser().open }
				 // -
				 {9} { Quarks.gui }
				 // -
				 {11} { MVC_View.verbose_(MVC_View.verbose.not) }
				 {12} { MVC_View.showLabelBackground_(MVC_View.showLabelBackground.not) }
				 {13} {
					if (MVC_View.editMode==false) {
						MVC_View.editResize=false;
						MVC_View.editMode_(true);
						"Edit mode: On".postln;
						"MVC_View.grid_(1)".postln;
					}{
						if (MVC_View.editResize==false) {
							MVC_View.editResize=true;
							"Edit mode: Resize".postln;
						}{
							MVC_View.editMode_(false);
							"Edit mode: Off".postln;
						};
					};
				 }
				 {14} { ColorPicker() }
				 // -
				 {16} { MVC_LazyRefresh.globalFPS_(1) }
				 {17} { MVC_LazyRefresh.globalFPS_(12) }
				 {18} { MVC_LazyRefresh.globalFPS_(24) }
				 {19} { MVC_LazyRefresh.globalFPS_(50) }
				 // -
				 {21} { studio.server.makeWindow }
				 {22} {
					studio.network.otherUsers.do{|user|
						var g=user.commonTimePings.flop.reverse;
						if (g[0].size>0) {
							g.plot2("Latency & Delta ("++(user.name)++")").specs_(
								[[0,g[0].last,\lin,0,0,"s"].asSpec,
								[g[1].copy.sort.first,g[1].copy.sort.last,\lin,0,0,"s"].asSpec]
							).plotMode_(\points)
						}{
							"No data".postln;
						}
				 	}
				 }
				 {23} {
					studio.network.socket.verbose_(studio.network.socket.verbose.not);
				 }
				;
			})
			.revert_(true)
			.create(mw.view, true);

		this.addDebugMenusXPlat(studio);

	}

	*addDeveloperMenusOSX {|studio|

		var sub;
		var tools  = SCMenuGroup.new(nil, "Dev",9),midi, audio, files;
		var myHack=studio.myHack;

		// add LNX Help menu
		SCMenuItem('Help',"LNX_Studio Help", 0).setShortCut("d").action_{studio.openHelp};
//
//
//		SCMenuItem.new(tools,  "Close Post Window")
//			.setShortCut("4")
//			.action_{
//				{Document.listener.close}.try;
//				{thisProcess.recompile}.defer(0.1);
//			};
//
//		if (Document.listener.isClosed) {
//			Document.new(" post ","",makeListener:true);
//			Document.listener.bounds=Rect(0,35,600,290);
//		};
//

		// post window
		SCMenuItem(tools,  "Save interval / Stop")
			.action_({studio.saveInterval});

		SCMenuItem(tools,  "Start Batch Recording")
			.action_({
				studio.hackOn_(true);
				studio.batchOn_(true);
				studio.batch_(1);
				studio.batchFolder_(studio.batchFolder+1)
			});

		SCMenuItem(tools,  "Stop Batch Recording")
			.action_({
				studio.batchOn_(false);
				studio.hackOn_(false);
			});

		SCMenuItem(tools,  "Reset Batch")
			.action_({
				studio.batchFolder_(0);
			});

		SCMenuSeparator.new(tools);

		SCMenuItem.new(tools,  "Post Window").setShortCut("1")
			.action_({
				{if (Document.listener.notNil) {Document.listener.close};}.try;
				Document.new(" post ","",makeListener:true);
				Document.listener.bounds=Rect(studio.class.osx,35,600,290);
			});

		SCMenuItem.new(tools,  "Close Post Window").setShortCut("2")
			.action_({
				Document.listener.close	;
			});

		SCMenuItem.new(tools,  "My Hack").action_({

				studio.hackOn_(true);
				myHack[\window].create.front;


			})
			.setShortCut("3");// *** HACK *** //

		SCMenuItem.new(tools,  "Index all help files")
			.action_({
				SCDoc.indexAllDocuments(true)
			});

		SCMenuItem.new(tools,  "Interpret & Replace").setShortCut("9")
			.action_({
					Document.current.selectedString =
					Document.current.selectedString.interpret.asCompileString
			});

		SCMenuItem.new(tools,  "Open Browser").setShortCut("0").action_({
			~b=LNX_WebBrowser().open;
		});

		SCMenuSeparator.new(tools);

		// eq
		SCMenuItem.new(tools,  "EQ").action_({MasterEQ.new});

		SCMenuSeparator.new(tools);
		//lang
		SCMenuItem.new(tools,  "Auto Syntax Colorizing").setShortCut("b").action_({
		// @TODO: new Qt "key" codes
		Document.current.keyDownAction_{|doc, char, mod, unicode, keycode, key|
	  		  if(unicode==13 or:(unicode==32) or: (unicode==3)){
	     		   Document.current.syntaxColorize
	   		 }
			};

		});
		SCMenuItem.new(tools,  "Quarks").action_({ Quarks.gui});

		SCMenuSeparator.new(tools);

		SCMenuItem.new(tools,  "MVC verbose").action_({MVC_View.verbose_(MVC_View.verbose.not)});
		SCMenuItem.new(tools,  "MVC show background").action_({
		MVC_View.showLabelBackground_(MVC_View.showLabelBackground.not)});

		sub=SCMenuGroup( tools, "MVC fps" );

		SCMenuItem(sub, "1 fps" ).action_{ MVC_LazyRefresh.globalFPS_(1) };
		SCMenuItem(sub, "12 fps" ).action_{ MVC_LazyRefresh.globalFPS_(12) };
		SCMenuItem(sub, "25 fps" ).action_{ MVC_LazyRefresh.globalFPS_(25) };
		SCMenuItem(sub, "50 fps" ).action_{ MVC_LazyRefresh.globalFPS_(50) };

		SCMenuItem(tools, "MVC Edit / Resize" )
			.setShortCut( "r" )
			.action_{
				//if (studio.isStandalone.not) {
					if (MVC_View.editMode==false) {
						MVC_View.editResize=false;
						MVC_View.editMode_(true);
						"Edit mode: On".postln;
						"MVC_View.grid_(1)".postln;
					}{
						if (MVC_View.editResize==false) {
							MVC_View.editResize=true;
							"Edit mode: Resize".postln;
						}{
							MVC_View.editMode_(false);
							"Edit mode: Off".postln;
						};
					};
				//}
			};

		SCMenuItem.new(tools,  "ColorPicker").action_({ColorPicker()});

		SCMenuSeparator.new(tools);
		SCMenuItem.new(tools,  "Server Window").action_({
			studio.server.makeWindow
		});

		this.addDebugMenusOSX(studio,tools);

//		SCMenuSeparator.new(tools);
//		SCMenuItem.new(tools,  "Open Library in Finder").action_{
//			("open" + (LNX_File.prefDir++"Library").quote ).systemCmd};
//		SCMenuItem.new(tools,  "Backup Library to Desktop").action_{studio.backupLibrary};
//		SCMenuItem.new(tools,  "Restore Library").action_{studio.restoreLibraryDefaults};
//		SCMenuItem.new(tools,  "Check For Updates").action_{studio.checkForLibraryUpdates};
//
		SCMenuSeparator.new(tools);

		SCMenuItem.new(tools,  "Graph: Latency & Delta").action_({
			studio.network.otherUsers.do{|user|
				var g=user.commonTimePings.flop.reverse;
				if (g[0].size>0) {
					g.plot("Latency & Delta ("++(user.name)++")").specs_(
						[[0,g[0].last,\lin,0,0,"s"].asSpec,
						[g[1].copy.sort.first,g[1].copy.sort.last,\lin,0,0,"s"].asSpec]
					).plotMode_(\points)
				}{
					"No data".postln;
				}
			}
		});

	}


	*addDebugMenusOSX{|studio,menu|

		var lnx=();

		SCMenuItem.new(menu,  "Query All Nodes").action_({

			lnx[\doc]=Document("Query All Nodes","",true).alwaysOnTop_(true);

			lnx[\doc].bounds_(lnx[\doc].bounds.resizeBy(-375,0));


			lnx[\refreshRate]=0.25;
			lnx[\task]=AppClock.sched(0,{
				if (lnx[\refreshRate].notNil) {
					Document.listener.string_("Query all nodes\n~~~~~~~~~~~~~~~\n\n");
					studio.server.queryAllNodes;
				};
				//lnx[\doc].bounds.resizeTo(80,60).moveBy(-80,lnx[\doc].bounds.height-80);
				lnx[\refreshRate]
			});
			lnx[\win]=Window("Close Query",
				lnx[\doc].bounds.resizeTo(80,60).moveBy(-80,lnx[\doc].bounds.height-80))
				.front
				.alwaysOnTop_(true)
				.userCanClose_(false);

			Button(lnx[\win],Rect(10,10,60,40)).states_([["Close", Color.black, Color.red]])
				.action_{
					lnx[\refreshRate]=nil;
					lnx[\doc].close;
					lnx[\win].close;
					[Document.allDocuments.detect{|n| n.name==" post "}].do{|doc|
						Document.new(" post ","",true).alwaysOnTop_(false)
							.bounds_(doc.bounds)
							.string_(doc.string);
							//.selectLine(inf);
						doc.close;
					};
				};

		});

		SCMenuItem.new(menu,  "Network verbose").action_({
			studio.network.socket.verbose_(studio.network.socket.verbose.not);
		});

		SCMenuSeparator.new(menu);

		SCMenuItem.new(menu,  "Inspect: Studio").action_({
			var w,t,text,string,tSize;
			w = Window.new("Inspect Studio").alwaysOnTop_(true).front;
			t = TextView(w.asView,Rect(10,10, 380,360))
				.hasHorizontalScroller_(true)
				.hasVerticalScroller_(true)
				.autohidesScrollers_(true)
				.resize_(5);
			t.focus;
			AppClock.sched(0,{
				if (w.isClosed.not) {
					text="";
					studio.getSlots.do({|a,i|
						string=a.asString;
						text=text++string;
						tSize=23-(string.size.clip(0,22));
						if (i.odd) {text=text++"\n"} {tSize.do({text=text++" "})}
					});
					t.string_(text);
					0.25
				}{
				   nil
				}
			});
		});

		SCMenuItem.new(menu,  "Inspect: Instruments").action_({
			var w,t,text,string,tSize,skipNext=false;
			w = Window.new("Inspect LNX_Instruments",Rect(128,64,900,240))
							.alwaysOnTop_(true).front;
			t = TextView(w.asView,Rect(10,10, 880,220))
				.hasHorizontalScroller_(true)
				.hasVerticalScroller_(true)
				.autohidesScrollers_(true)
				.resize_(5);
			t.focus;
			AppClock.sched(0,{
				if (w.isClosed.not) {
					text=studio.insts.asString;
					text=text++"\n";
					studio.insts.getSlots.do({|a,i|
						if (a!=\array) {
							if (skipNext) {
								skipNext=false
							}{
								string=a.asString;
								text=text++string;
								tSize=23-(string.size.clip(0,22));
								if (i.odd) {text=text++"\n"} {tSize.do({text=text++" "})}
							}
						}{
							skipNext=true;
						};
					});
					t.string_(text);
					0.25
				}{
				   nil
				}
			});
		});

		SCMenuItem.new(menu,  "Inspect: Insts[0]").action_({
			var w,t,text,string,tSize;
			w = Window.new("Inspect Insts[0]").alwaysOnTop_(true).front;
			t = TextView(w.asView,Rect(10,10, 380,360))
				.hasHorizontalScroller_(true)
				.hasVerticalScroller_(true)
				.autohidesScrollers_(true)
				.resize_(5);
			t.focus;
			AppClock.sched(0,{
				if (w.isClosed.not) {
					text="";
					studio.insts.visualAt(0).getSlots.do({|a,i|
						string=a.asString;
						text=text++string;
						tSize=23-(string.size.clip(0,22));
						if (i.odd) {text=text++"\n"} {tSize.do({text=text++" "})}
					});
					t.string_(text);
					0.25
				}{
				   nil
				}
			});
		});

		SCMenuItem.new(menu,  "Inspect: Network").action_({
			var w,t,text,string,tSize;
			w = Window.new("Inspect Network").alwaysOnTop_(true).front;
			t = TextView(w.asView,Rect(10,10, 380,360))
				.hasHorizontalScroller_(true)
				.hasVerticalScroller_(true)
				.autohidesScrollers_(true)
				.resize_(5);
			t.focus;
			AppClock.sched(0,{
				if (w.isClosed.not) {
					text="";
					studio.network.getSlots.do({|a,i|
						string=a.asString;
						text=text++string;
						tSize=23-(string.size.clip(0,22));
						if (i.odd) {text=text++"\n"} {tSize.do({text=text++" "})}
					});
					t.string_(text);
					0.25
				}{
				   nil
				}
			});
		});

		SCMenuItem.new(menu,  "Inspect: Collaboration").action_({
			var w,t,text,string,tSize;
			w = Window.new("Inspect Collaboration").alwaysOnTop_(true).front;
			t = TextView(w.asView,Rect(10,10, 380,360))
				.hasHorizontalScroller_(true)
				.hasVerticalScroller_(true)
				.autohidesScrollers_(true)
				.resize_(5);
			t.focus;
			AppClock.sched(0,{
				if (w.isClosed.not) {
					text="";
					studio.network.collaboration.getSlots.do({|a,i|
						string=a.asString;
						text=text++string;
						tSize=23-(string.size.clip(0,22));
						if (i.odd) {text=text++"\n"} {tSize.do({text=text++" "})}
					});
					t.string_(text);
					0.25
				}{
				   nil
				}
			});
		});

		SCMenuItem.new(menu,  "Inspect: Protocols").action_({
			var w,t,text,string,tSize;
			w = Window.new("Inspect Comms",Rect(128,64,500,160)).alwaysOnTop_(true).front;
			t = TextView(w.asView,Rect(10,10, 480,130))
				.hasHorizontalScroller_(true)
				.hasVerticalScroller_(true)
				.autohidesScrollers_(true)
				.resize_(5);
			t.focus;
			AppClock.sched(0,{
				if (w.isClosed.not) {
					text="";
					["network           ",LNX_Protocols.network,
					 "\nobjects           ",LNX_Protocols.objects,
					 "\npermanentObjects  ",LNX_Protocols.permanentObjects,
					 "\nmessages         ",LNX_Protocols.messages,
					 "\ntasks             ",LNX_Protocols.tasks,
					 "\nvariableParameters",LNX_Protocols.variableParameters

					].do{|i| text=text++(i.asString)};

					t.string_(text);
					0.25
				}{
				   nil
				}
			});
		});

		SCMenuItem.new(menu,  "Inspect: OnSoloGroup").action_({
			var w,t,text,string,tSize,oSG;
			w = Window.new("Inspect OnSoloGroup",Rect(928,264,200,460))
				.alwaysOnTop_(true).front;
			t = TextView(w.asView,Rect(10,10, 180,430))
				.hasHorizontalScroller_(true)
				.hasVerticalScroller_(true)
				.autohidesScrollers_(true)
				.resize_(5);
			t.focus;
			AppClock.sched(0,{
				if (w.isClosed.not) {
					text="Actual OnSolo\n";

					oSG=studio.onSoloGroup;

					oSG.onSolos.keys.asList.sort.collect{|i|
						[oSG.onSolos[i].on,oSG.onSolos[i].solo]
					}.do{|i,j|
						text=text+j+":"+(i.asString)+"\n";
					};

					[
					"\n",
					oSG.usersOnSoloList.asString
					].do{|i| text=text++(i.asString)};
					t.string_(text);
					0.25
				}{
				   nil
				}
			});
		});

	}


	*addDebugMenusXPlat{|studio,menu|}

}
