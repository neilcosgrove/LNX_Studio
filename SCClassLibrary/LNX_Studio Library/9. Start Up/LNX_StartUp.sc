  //*************************//
 // LNX_STUDIO Version 1.6  //
//*************************//
//
// start appropriate services (developer or standalone mode)
// remove this if you don't want lnx to auto-start on launch
// LNX_Studio(Server.local); // do this instead for a manual start
//

LNX_StartUp {
	
	classvar >studio;
	
	*postStartUp{
	}
	
	*initClass{


		StartUp.add {
			
			SCDoc.indexAllDocuments;
						
			studio = LNX_Studio(Server.local); 	// start the studio
			
			// add LNX Help menu
//			SCMenuItem('Help',"LNX_Studio Help", 0).setShortCut("d").action_{
//				//( String.scDir++ "/Help/LNX_Studio/Help.html").openDocument
//				"LNX_Studio Help".help
//			};

			// temp to remove
//			{
//				{if (Document.listener.notNil) {Document.listener.close};}.try;
//				Document.new(" post ","",makeListener:true);
//				Document.listener.bounds=Rect(studio.class.osx,35,600,290);
//				
//			}.defer(1);
			
			
			// add appropriate menus
			if (studio.isStandalone) {
				LNX_AppMenus.addDeveloperMenus(studio); // to remove for release
				LNX_AppMenus.addReleaseMenus(studio);
				
				//Document.listener.close;
				
				Document.listener.bounds=Rect(LNX_Studio.osx,32,535,175);
			}{	
				LNX_AppMenus.addDeveloperMenus(studio);
				QuarkSVNRepository.svnpath= "/opt/subversion/bin/svn"; // svn for supercollider
			};
							
			// resize help documents to readable sizes
	 		Document.initAction=Document.initAction.addFunc{|doc|
				var b;
				{
					if ((doc.name=="LNX_BumNote")
						||(doc.name=="LNX_DrumSynth")
						||(doc.name=="LNX_Code")
						||(doc.name=="LNX_GSRhythm")
					) {
						b=doc.bounds;
						doc.bounds_(Rect(b.left,b.top,760,b.height+230));
					};
					if (doc.name=="Quick Start Guide") {
						b=doc.bounds;
						doc.bounds_(Rect( 
							SCWindow.screenBounds.width/2-(498/2),
							SCWindow.screenBounds.height-635-40,
							497,635));
						doc.alwaysOnTop_(true);
					};
				}.defer(0.1);
			};
				
			if (studio.isStandalone) {
				
				// load songs dropped or opened in SC
				Document.initAction=Document.initAction.addFunc{|doc|
					if (doc.string[..14]=="SC Studio Doc v") {
						{
							doc.bounds_(Rect(0,0,0,0));
						}.defer(0.05);
						{
							studio.dropLoad(doc);
						}.defer(0.1);
						{
							doc.close
						}.defer(0.15)
					};
				};
				
		
				// stop edit of help documents
//				Document.initAction=Document.initAction.addFunc{|doc|
//					if	(studio.isStandalone) {
//						doc.editable_(false)
//					};
//				};
				
				// load songs dropped or opened in SC at start-up
				Document.allDocuments.do{|doc|
					if (doc.string[..14]=="SC Studio Doc v") {
						{
							doc.bounds_(Rect(0,0,0,0));
						}.defer(0.05);
						{
							studio.dropLoad(doc);
						}.defer(0.1);
						{
							doc.close
						}.defer(0.15)
					};
				};
				
			};
			
			// add lnx preferences as the preferences
			thisProcess.preferencesAction = { studio.preferences };
			
			if (studio.isStandalone && LNX_Mode.isSafe) {
				thisProcess.platform.recordingsDir = 
					"/Users/Shared";
					//String.scDir.dirname.dirname +/+ "Contents/Safe Mode Recordings";
			}{
				// set the recording directory
				thisProcess.platform.recordingsDir = "~/Desktop".standardizePath;
			};
			
			this.interpreterDebugging;
		
		};
		
		ShutDown.add { studio.onClose };		// on shutdown
		
		this.postStartUp;

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
	
	*studio{^studio}

}

