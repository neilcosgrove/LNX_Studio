// jph 2019-10-26 Action replaced by MenuAction


// the application menus's  /////////////////////////////////////////////////////////////

LNX_AppMenus {

	classvar >studio;

	*initClass { Platform.case(\osx, {Class.initClassTree(Menu)}); }

	*addWindowMenus{|window|

		var menuTheme = ( \showTick_:false, canFocus_:false, font_: Font("Helvetica", 13, true),
			\colors_  : ( \background : Color(1,1,1,0.7), \string : Color.black ));

		// Main menu
		MVC_PopUpMenu3(window, Rect(5, 4, 100, 18), menuTheme)
				.staticText_("LNX_Studio")
				.items_([
					"About LNX_Studio",
					"-",
					"Preferences",
					"-",
					"Hide LNX_Studio",
					"-",
					"Quit"
				])
				.action_{|me|
					switch (me.value.asInt)
					 {0}{ //LNX_SplashScreen.init(studio,true)
					}{2}{ studio.preferences
					}{4}{ Platform.case(\osx,{
"osascript -e 'tell application \"Finder\"' -e 'set visible of process \"LNX_Studio\" to false' -e 'end tell'".unixCmd})
					}{6}{ studio.quit
					};
				};

		// File menu
		MVC_PopUpMenu3(window, Rect(105, 4, 62, 18), menuTheme)
				.staticText_("File")
				.items_([
					"Open",
					"Open Last Song",
					"-",
					"Save",
					"Save As...",
					"-",
					"Close Song"
				])
				.action_{|me|
					switch (me.value.asInt)
					 {0}{ studio.loadDialog
					}{1}{ studio.quickLoad
					}{3}{ studio.saveDialog
					}{4}{ studio.saveAsDialog
					}{6}{ studio.guiCloseStudio
					}
				};


		// edit menu
		MVC_PopUpMenu3(window, Rect(167, 4, 58, 18), menuTheme)
				.staticText_("Edit")
				.items_([
					"Stop Audio",
					"-",
					"Copy Instrument",
					"Paste Instrument",
					"Duplicate Instrument",
					"-",
					"Delete Instrument",
					"-",
					"Clear Instrument Sequencer",
					"Clear All Sequencers",
					"-",
					"All MIDI Controls"
				])
				.action_{|me|
					switch (me.value.asInt)
					 {0}{ CmdPeriod.run
					}{2}{ studio.guiCopy
					}{3}{ studio.guiPaste
					}{4}{ studio.guiDuplicate
					}{6}{ studio.guiDeleteInst
					}{8}{ if (studio.insts.selectedInst.notNil) { studio.insts.selectedInst.clearSequencer }
					}{9}{ studio.insts.do(_.clearSequencer)
					}{11}{  studio.editMIDIControl
					}
				};

			// Library menu
			MVC_PopUpMenu3(window, Rect(225, 4, 75, 18), menuTheme)
				.staticText_("Library")
				.items_([
					 "Add Instrument to Library",
					"-",
					"Backup Library to Desktop",
					"Restore Library Defaults",
					"Check For Library Updates",
					"Open Library Folder"
				])
				.action_{|me|
					switch (me.value.asInt)
					 {0}{ studio.guiSaveInstToLibrary
					}{2}{ studio.backupLibrary
					}{3}{ studio.restoreLibraryDefaults
					}{4}{ studio.checkForLibraryUpdates
					}{5}{ studio.openLibraryFolderInOS
					}
				};

			// network menu
			MVC_PopUpMenu3(window, Rect(300, 4, 75, 18), menuTheme)
				.staticText_("Network")
				.items_([
					"Open Network",
					"-",
					"Leave Colaboration",
					"Close Network",
					"Forget Users",
					"-",
					"Network Prefereces",
				])
				.action_{|me|
					switch (me.value.asInt)
					 {0}{ studio.network.guiConnect
					}{2}{ studio.network.collaboration.guiLeave
					}{3}{ studio.network.disconnect
					}{4}{ LNX_LANGroup.clearPreviousAddrs
					}{6}{ studio.network.preferences
					}
				};

			// dev
			MVC_PopUpMenu3(window, Rect(375, 4, 53, 18), menuTheme)
				.staticText_("Dev")
				.items_([
					"Code Window",
					"Recompile Class Libray",
					"-",
					"Save interval / Stop",
					"Start Batch Recording",
					"Stop Batch Recording",
					"Reset Batch",
					"-",
					"My Hack",
					"Index all help files",
					"Render all help files",
					"Open Browser",
					"-",
					"Quarks",
					"-",
					"MVC Verbose",
					"MVC Show Background",
					"MVC Edit / Resize",
					"ColorPicker",
					"-",
					"Server Window",
					"Network Verbose",
				])
				.action_{|me|
					switch (me.value.asInt)
					 {0}{ TextView().enterInterpretsSelection_(true).front
					}{1}{ thisProcess.platform.recompile
					}{3}{ studio.saveInterval
					}{4}{ studio.hackOn_(true).batchOn_(true).batch_(1).batchFolder_(studio.batchFolder+1)
					}{5}{ studio.batchOn_(false).hackOn_(false)
					}{6}{ studio.batchFolder_(0)
					}{8}{ studio.hackOn_(true).myHack[\window].create.front
					}{9}{ SCDoc.indexAllDocuments
					}{10}{ SCDoc.renderAll
					}{11}{ ~b=LNX_WebBrowser().open
					}{13}{ Quarks.gui
					}{15}{ MVC_View.verbose_(MVC_View.verbose.not)
					}{16}{ MVC_View.showLabelBackground_(MVC_View.showLabelBackground.not)
					}{17}{
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
					}{18}{ ColorPicker()
					}{20}{ studio.server.makeWindow
					}{21}{ studio.network.socket.verbose_(studio.network.socket.verbose.not)
					}
				};

		// windows
		MVC_PopUpMenu3(window, Rect(428, 4, 83, 18), menuTheme)
				.staticText_("Windows")
				.items_([
					"Minimise",
					"Arrange",
					"Close Window",
					"Close All Window",
				])
				.action_{|me|
					switch (me.value.asInt)
					 {0}{ studio.insts.selectedInst.window.minimize
					}{1}{
					}{2}{ studio.insts.selectedInst.closeWindow
					}{3}{ studio.insts.do(_.closeWindow)
					}
				};

		// help
		MVC_PopUpMenu3(window, Rect(511, 4, 60, 18), menuTheme)
				.staticText_("Help")
				.items_([
					"Help with LNX_Studio",
					"Help with Supercollider",
					"-",
					"Open Demo Song",
				])
				.action_{|me|
					switch (me.value.asInt)
					 {0}{ studio.openHelp
					}{1}{ Help.help
					}{3}{ studio.loadDemoSong
					}
				};

	}

	// new sc1.8 style menus //////////////////////////////////////////////////////////

	*menus{
		^[
			// main menu
			Menu(
				MenuAction("About LNX_Studio",       { LNX_SplashScreen.init(studio,true) }),
				MenuAction.separator,
				MenuAction("Preferences", { studio.preferences }),
				MenuAction.separator,
				MenuAction("Hide LNX_Studio", {
					Platform.case(\osx,{
						"osascript -e 'tell application \"Finder\"' -e 'set visible of process \"LNX_Studio\" to false' -e 'end tell'".unixCmd
					});
				}).shortcut_("Ctrl+H"),
				MenuAction.separator,
				MenuAction("Quit",        { studio.quit }).shortcut_("Ctrl+Q")
			).title_("SuperCollider"),

			// file menu
			Menu(
				MenuAction("Open",				{ studio.loadDialog }).shortcut_("Ctrl+O"),
				MenuAction("Open Last Song",	{ studio.quickLoad }).shortcut_("Ctrl+Shift+O"),
				MenuAction.separator,
				MenuAction("Save",				{ studio.saveDialog }).shortcut_("Ctrl+S"),
				MenuAction("Save As...",		{ studio.saveAsDialog }).shortcut_("Ctrl+Shift+S"),
				MenuAction.separator,
				MenuAction("Close Song", 		{ studio.guiCloseStudio }).shortcut_("Ctrl+Shift+W"),
			).title_("File"),

			// edit menu
			Menu(
				MenuAction("Stop Audio",			{CmdPeriod.run}).shortcut_("Ctrl+."),
				MenuAction.separator,
				MenuAction("Copy Instrument",		{ studio.guiCopy }).shortcut_("Ctrl+Shift+C"),
				MenuAction("Paste Instrument",		{ studio.guiPaste }).shortcut_("Ctrl+Shift+V"),
				MenuAction("Duplicate Instrument",	{ studio.guiDuplicate }).shortcut_("Ctrl+Shift+D"),
				MenuAction.separator,
				MenuAction("Delete Instrument",		{ studio.guiDeleteInst }).shortcut_("Ctrl+Shift+Backspace"),
				MenuAction.separator,
				MenuAction("Clear Instrument Sequencer", {
					if (studio.insts.selectedInst.notNil) {
						studio.insts.selectedInst.clearSequencer
					}
				}),
				MenuAction("Clear All Sequencers",			{ studio.insts.do(_.clearSequencer) }),
				MenuAction.separator,
				MenuAction("All MIDI Controls",		{ studio.editMIDIControl }).shortcut_("Ctrl+Shift+M"),

			).title_("Edit"),

			// library menu
			Menu(
				MenuAction("Add Instrument to Library",	{ studio.guiSaveInstToLibrary }).shortcut_("Ctrl+L"),
				MenuAction.separator,
				MenuAction("Backup Library to Desktop", { studio.backupLibrary }),
				MenuAction("Restore Library Defaults",	{ studio.restoreLibraryDefaults }),
				MenuAction("Check For Library Updates",	{ studio.checkForLibraryUpdates }),
				MenuAction("Open Library Folder",		{ studio.openLibraryFolderInOS }),
			).title_("Library"),

			// network menu
			Menu(
				MenuAction("Open Network", 			{ studio.network.guiConnect }).shortcut_("Ctrl+N"),
				MenuAction.separator,
				MenuAction("Leave Colaboration",	{ studio.network.collaboration.guiLeave }),
				MenuAction("Close Network",			{ studio.network.disconnect }).shortcut_("Ctrl+Shift+N"),
				MenuAction("Forget Users", { LNX_LANGroup.clearPreviousAddrs }),
				MenuAction.separator,
				MenuAction("Network Prefereces",	{ studio.network.preferences }),
			).title_("Network"),

			//  dev menu
			Menu(
				MenuAction("Code Window",{TextView().enterInterpretsSelection_(true).front }).shortcut_("Ctrl+1"),
				MenuAction("Recompile Class Libray",{thisProcess.platform.recompile}).shortcut_("Ctrl+K"),
				MenuAction.separator,
				MenuAction("Save interval / Stop",{ studio.saveInterval }),
				MenuAction("Start Batch Recording",{
					studio.hackOn_(true).batchOn_(true).batch_(1).batchFolder_(studio.batchFolder+1)
				}),
				MenuAction("Stop Batch Recording",{ studio.batchOn_(false).hackOn_(false) }),
				MenuAction("Reset Batch",{ studio.batchFolder_(0) }),
				MenuAction.separator,
				MenuAction("My Hack",{ studio.hackOn_(true).myHack[\window].create.front }),
				MenuAction("Index all help files",{  SCDoc.indexAllDocuments }),
				MenuAction("Render all help files",{ SCDoc.renderAll }),
				MenuAction("Open Browser",{ ~b=LNX_WebBrowser().open }).shortcut_("Ctrl+0"),
				MenuAction.separator,
				MenuAction("Quarks",{ Quarks.gui }),
				MenuAction.separator,
				MenuAction("MVC Verbose",{ MVC_View.verbose_(MVC_View.verbose.not) }),
				MenuAction("MVC Show Background",{
					MVC_View.showLabelBackground_(MVC_View.showLabelBackground.not)
				}).shortcut_("Ctrl+B"),
				MenuAction("MVC Edit / Resize",{
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
				MenuAction("ColorPicker",{ ColorPicker() }),
				MenuAction.separator,
				MenuAction("Server Window",{ studio.server.makeWindow }),
				MenuAction("Network Verbose",{
					studio.network.socket.verbose_(studio.network.socket.verbose.not);
				}),
				MenuAction.separator,
			).title_("Dev"),

			//  windows menu
			Menu(
				MenuAction("Minimise",			{ MVC_Window.frontWindow.minimize }).shortcut_("Ctrl+M"),
				MenuAction("Arrange",			{}),
				MenuAction("Close Window",		{ MVC_Window.frontWindow.guiClose }),
				MenuAction("Close All Window",	{ studio.insts.do(_.closeWindow) }),
			).title_("Windows"),

			//  help menu
			Menu(
				MenuAction("Help with LNX_Studio",		{studio.openHelp}).shortcut_("Ctrl+D"),
				MenuAction("Help with Supercollider",	{Help.help}),
				MenuAction.separator,
				MenuAction("Open Demo Song",	{ studio.loadDemoSong }),
			).title_("Help"),

		];
	}

	// my hack stuff

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

}
