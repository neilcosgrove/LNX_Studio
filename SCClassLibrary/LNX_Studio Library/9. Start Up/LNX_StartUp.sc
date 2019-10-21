  //*************************//
 // LNX_STUDIO Version 1.6  //
//*************************//
//
// start appropriate services (developer or standalone mode)
// remove this if you don't want lnx to auto-start on launch
// LNX_Studio(Server.local); // do this instead for a manual start
//

LNX_StartUp {

	classvar >studio, <hasWindowMenus=false;

	*initClass{
		Class.initClassTree(LNX_File);
		Platform.case(
			\linux,		{ this.linuxInitClass   },
		);
		StartUp.add {
			Platform.case(
				\osx,		{ this.osxStartUp     },
				\linux,		{ this.linuxStartUp   },
				\windows,	{ this.windowsStartUp }
			);
		};
		ShutDown.add { studio.onClose };		 // and on shutdown
	}

	*linuxInitClass{
		var lrd = Platform.lnxResourceDir;
		var cwd = File.getcwd;
		if (lrd.pathExists(false).not) {
			lrd.makeDir;
		};
		if ((lrd +/+ "lnx.jpg").pathExists(false).not) {
			File.copy(cwd +/+ "lnx.jpg", lrd +/+ "lnx.jpg");
		};
		if ((lrd +/+ "demo song").pathExists(false).not) {
			File.copy(cwd +/+ "demo song", lrd +/+ "demo song");
		};
		if ((lrd +/+ "sounds").pathExists(false).not) {
			var sounds = PathName(cwd +/+ "sounds").deepFiles;
			sounds.do {|s|
				var newPath = PathName(s.fullPath.replace(cwd, lrd));
				newPath.pathOnly.makeDir;
				File.copy(s.fullPath,
					newPath.fullPath);
			};
		};
		if ((lrd +/+ "default library").pathExists(false).not) {
			var lib = PathName(cwd +/+ "default library").deepFiles;
			lib.do {|l|
				var newPath = PathName(l.fullPath.replace(cwd, lrd));
				newPath.pathOnly.makeDir;
				File.copy(l.fullPath,
					newPath.fullPath);
			};
		};
	}

	*xPlatStartUp{
		studio = LNX_Studio(Server.local); 	// start the studio, use local server
//		studio = LNX_Studio(Server.internal); // start the studio, use internal server
		LNX_AppMenus.studio_(studio);
		thisProcess.platform.recordingsDir = (Platform.userHomeDir +/+ "Desktop").standardizePath;
	}

	// TODO: any windows specific startup here
	*windowsStartUp{
		hasWindowMenus = true;
		this.xPlatStartUp;
		this.postStartUp;
	}

	// TODO: any linux specific startup here
	*linuxStartUp{
		hasWindowMenus = true;
		this.xPlatStartUp;
		this.postStartUp;
	}

	*osxStartUp{
		hasWindowMenus = false;
		Server.quitAll;
		"scsynth".killApp;
		studio = LNX_Studio(Server.local); 	// start the studio, use local server
//		studio = LNX_Studio(Server.internal); // start the studio, use internal server
		LNX_AppMenus.studio_(studio); // so menus can access studio
		// set the recording directory
		thisProcess.platform.recordingsDir = "~/Desktop".standardizePath;
//		SCDoc.renderAll;
//		SCDoc.indexAllDocuments;
		this.postStartUp;

		LNX_AppMenus.initMyHack(studio);

	}

	*postStartUp{
		this.interpreterDebugging;
	}

	*interpreterDebugging{
		// attach various objects to interpreter variables for debugging
		"
			a=LNX_StartUp.studio;
			s=a.server;
		 	n=a.network;
		 	u=n.thisUser;
		 	i=a.insts;
		 	//p=LNX_API(a,'test');
		".interpret;
	}

	*studio{^studio} // to remove for release

}

