
// the instrument library

+ LNX_Studio {
	
	// comment this neil!!
	checkForLibraryUpdates{
		var internetLibraryIndex;
		if (LNX_Mode.isSafe.not) {
			{
				(String.scDir+/+"internet_library_index").removeFile(false,false,true);
				this.dialog1("Checking...",Color.white);
				this.dialog2("",Color.white);
				("curl http://lnxstudio.sourceforge.net/default_library/index > \""++
					String.scDir+/+"internet_library_index\"").unixCmd;
				3.wait;
				internetLibraryIndex = (String.scDir+/+"internet_library_index").loadList;
				if (internetLibraryIndex[0]=="*** LNX Library Index ***") {
					this.dialog2("Connected",Color.white);
					1.wait;
					this.downLoadUpdates(internetLibraryIndex.drop(1));
				}{
					this.dialog1("Checking......",Color.white);
					("curl http://lnxstudio.sourceforge.net/default_library/index > \""++
						String.scDir+/+"internet_library_index\"").unixCmd;
					5.wait;
					internetLibraryIndex = (String.scDir+/+"internet_library_index").loadList;
					if (internetLibraryIndex[0]=="*** LNX Library Index ***") {
						this.dialog2("Connected",Color.white);
						1.wait;
						this.downLoadUpdates(internetLibraryIndex.drop(1));
					}{
						this.dialog1("Checking.........",Color.white);
						("curl http://lnxstudio.sourceforge.net/default_library/index > \""++
							String.scDir+/+"internet_library_index\"").unixCmd;
						7.wait;
						internetLibraryIndex = (String.scDir+/+
							"internet_library_index").loadList;
						if (internetLibraryIndex[0]=="*** LNX Library Index ***") {
							this.dialog2("Connected",Color.white);
							1.wait;
							this.downLoadUpdates(internetLibraryIndex.drop(1));
						}{
							this.dialog1(
						"Failed connecting to http://lnxstudio.sourceforge.net",Color.white);
							this.dialog2("",Color.white);
						};	
					};
				};
			}.fork(AppClock);
		};
	}
	
	// comment this neil!!
	downLoadUpdates{|internetLibraryIndex|	
		var folder = String.scDir++"/default library".absolutePath;
		internetLibraryIndex.collect{|file|
			if ((folder+/+file).pathExists(false).not) {
				this.dialog1("Downloading... ",Color.white);
				this.dialog2(file,Color.white);
				("curl http://lnxstudio.sourceforge.net/default_library/"
					++ (file.replace(" ", "%20"))
					++ " > \""
					++ folder+/+file
					++"\""
				).unixCmd;
				(0.25/4).wait;		
			};
		};
		this.dialog1("Installing... ",Color.white);
		this.dialog2("",Color.white);
		1.wait;
		this.restoreLibraryDefaults;
		this.dialog1("Finished",Color.white);
	}
	
	backupLibrary{
		// desktop folder
		var folder = "~/".absolutePath +/+ "Desktop/default_library" + (Date.getDate.format("%Y-%d-%e %R:%S").replace(":",".").drop(2));
		// library paths
		var paths  = instLibraryFileNames.select{|i| i.size>0 }
					.collect{|i,k| i.collect{|p| k.asString+/+p}}.asList.flatNoString;
		// create folder
		if (folder.pathExists(false).not) {folder.makeDir(true)};
		// save index
		(["*** LNX Library Index ***"]++paths).saveList(folder +/+ "index");
		// copy files
		LNX_Studio.instLibraryFileNames.select{|i| i.size>0 }.keys.do{|instFolder|
			(LNX_File.prefDir++"Library"+/+instFolder++"/").folderContents(1).select(_.isFile)
				.do{|file|
					file.copyToDir(folder+/+instFolder,silent:true,overwrite:false)
				}
		};
		"Library backed-up".postln;
	}
	
	// make all the folders for the instrument library ////////////////////////////////////////////
	
	initLibrary{|forceRestore=false|
		// master
		var masterDir = String.scDir++"/default library/".absolutePath;
		if (masterDir.pathExists(false).not) { masterDir.makeDir };
		
		visibleTypes.collect(_.studioName).do{|name|
			// the directory
			var dir = masterDir++name++"/";
			// create it if absent
			if (dir.pathExists(false).not) { dir.makeDir };
		};
		
		// actual
		instLibraryFileNames = IdentityDictionary[];
		visibleTypes.collect(_.studioName).do{|name|
			
			// the directory
			var dir = LNX_File.prefDir++"Library"+/+name++"/";
			
			// create & copy from master if absent
			if ((dir.pathExists(false).not)||forceRestore) {
				
				if (dir.pathExists(false).not) { dir.makeDir };
				
				// check file is Ok to copy else delete...
				(masterDir++name++"/").folderContents(0).select(_.isFile).do{|file|
					var loadList = file.loadList;
					if  ((loadList.size>1) and:{loadList[0].size>2} 
										and:{loadList[0][0..2]=="SC "}) {
						// do nothing
					}{
						file.removeFile(true,false,false);
					};
				};
				
				// now copy remaining
				(masterDir++name++"/").folderContents(0).select(_.isFile).do{|file|
					file.copyToDir(dir,silent:true,overwrite:false)
				};
			};
		};
		
		instTypes.collect(_.studioName).do{|name|
						
			// the directory
			var dir = LNX_File.prefDir++"Library"+/+name++"/";
			
			// get all files in that dir
			instLibraryFileNames[name.asSymbol] =
				dir.folderContents(0).select(_.isFile).collect(_.basename).sort({|a,b|
					(a.toLower) <= (b.toLower)
				});
		};
	}
	
	// remove & remake the entire library gui, used when showing / hiding moog & korg
	recreateLibraryGUI{
		this.createInstrumentList;
		visibleTypesGUI.do(_.free);
		mixerGUI[\libraryScrollView].free;
		this.createLibraryScrollView;
		this.createLibraryWidgets;
		this.autoSizeGUI;
		this.libraryGUIBugFix;
	}
		
	// scroll view for library widgets
	createLibraryScrollView{
		// the library scroll view
		mixerGUI[\libraryScrollView] = MVC_RoundedScrollView (mixerWindow,Rect(11, 33, 190, 299+25))
			.resizeList_([1,1,1,1,1]) //  0:view 1:left 2:top 3:right 4:bottom
			.hasBorder_(false)
			.addFlowLayout(nil,1@1)
			.autoScrolls_(false)
			.hasVerticalScroller_(true)
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border,Color(6/11,42/83,29/65))
			.hasHorizontalScroller_(true);
	}
	
	// make the gui
	createLibraryWidgets{
		
		libraryGUI       = IdentityDictionary[];
		visibleTypesGUI  = IdentityDictionary[];
		
		visibleTypes.do{|type|
			var view;
			var typeSymbol = type.studioName.asSymbol;
			var files = instLibraryFileNames[typeSymbol];

			// the expand view
			visibleTypesGUI[typeSymbol] =
				MVC_ExpandView( mixerGUI[\libraryScrollView], 182@((files.size)*19+18), 182@18  )
					.color_(\background,Color(0.88,0.88,0.88));
			
			// the main instrument text
			MVC_StaticText(visibleTypesGUI[typeSymbol],Rect(0,1,125,16))
				.canBeHidden_(false)
				.font_(Font("Helvetica",11))
				.shadow_(false)
				.color_(\string,Color.black)
				.string_(type.studioName.asString);
				
			// the main instrument add button
			MVC_FlatButton(visibleTypesGUI[typeSymbol],Rect(127+3,0,30,17)).strings_("Add")
				.canBeHidden_(false)
				.rounded_(true)
				.color_(\background,Color(0,0,0,0.3))
				.color_(\up,Color(0,0,0,0.2))
				.color_(\down,Color(0,0,0,0.4))
				.action_{	this.guiAddInst(type) };
				
			this.addLibraryWidgets(type);

		};
	}
	
	// add the library widgets to the gui
	addLibraryWidgets{|type|
		
		var typeSymbol = type.studioName.asSymbol;
		
		// the library widgets
	
		libraryGUI[typeSymbol]=IdentityDictionary[];
		
		instLibraryFileNames[type.studioName.asSymbol].do{|file,i|
			
			// lib inst name
			libraryGUI[typeSymbol][(file++"_text").asSymbol] =
				MVC_StaticText(visibleTypesGUI[typeSymbol],Rect(15,1+((i+1)*19),110,16))
					.font_(Font("Helvetica",11))
					.shadow_(false)
					.color_(\string,Color.black)
					.string_(file);
			
			// add / load inst from library
			libraryGUI[typeSymbol][(file++"_button").asSymbol] =
				MVC_FlatButton(visibleTypesGUI[typeSymbol],Rect(127+3,0+((i+1)*19),30,17))
					.strings_("Add")
					.rounded_(true)
					.color_(\background,Color(0,0,0,0.3))
					.color_(\up,Color(0,0,0,0.3))
					.color_(\down,Color(0,0,0,0.6))
					.action_{
						this.guiLoadInstFromLibrary(
							"Library"+/+(type.studioName)+/+file,type,file);
					};

			// delete inst from library
			libraryGUI[typeSymbol][(file++"_delete").asSymbol] =
				MVC_FlatButton(visibleTypesGUI[typeSymbol],Rect(0,3+((i+1)*19),12,12))
					.strings_("-")
					.rounded_(true)
					.color_(\background,Color(0,0,0,0.3))
					.color_(\up,Color(0,0,0,0.1))
					.color_(\down,Color(0,0,0,0.2))
					.actions_(\mouseUpDoubleClickAction,{
						("Library"+/+(type.studioName)+/+file).deletePref;
						{this.refreshLibrary(type)}.defer(0.1);
					});
					
		};
	}
	
	// temp fix to gui which displays beyond bounds 
	libraryGUIBugFix{
		visibleTypesGUI.do(_.expand);
		visibleTypesGUI.do(_.collapse);
	}
	
	// gui call to load an intrument from the library
	guiLoadInstFromLibrary{|filename,type,name|
		var list;
		if ((isLoading.not)and:{server.serverRunning}) {
			list = filename.loadPref;
			if (list.notNil) {
				list[3]=name; // fix for naming over network
				this.guiAddInst(type,list,name);
				{MVC_Automation.updateDurationAndGUI.refreshGUI}.defer(1);
			}
		}
	}
	
	// gui call to save the current instrument to the library
	guiSaveInstToLibrary{
		
		var window, scrollView, class, saveList, filename, studioName;
			
		if (insts.selectedInst.notNil) {
			class      = insts.selectedInst.class;
			LNX_MIDIControl.autoSave_(false); // disable save with Automation
			saveList   = insts.selectedInst.getSaveListForLibrary;
			LNX_MIDIControl.autoSave_(true);  // enable save with Automation
			filename   = insts.selectedInst.name;
			studioName = insts.selectedInst.studioName;
			
			window = MVC_ModalWindow(mixerWindow.view, 195@90);
			scrollView = window.scrollView;
	
			// text field for the instrument / filename
			MVC_Text(scrollView,Rect(10,21,142,16))
				.string_(filename)
				.label_("Add instrument to library as...")
				.labelShadow_(false)
				.shadow_(false)
				.canEdit_(true)
				.hasBorder_(true)
				.enterStopsEditing_(false)
				.color_(\label,Color.black)
				.color_(\edit,Color.white)
				.color_(\editBackground,Color(0,0,0,0.3))
				.color_(\background,Color(0,0,0,0.3))
				.color_(\focus,Color(0,0,0,0.1))
				.font_(Font.new("STXihei", 13))
				.stringAction_{|me,string|
					filename=string.filenameSafe;
					me.string_(filename);
				}
				.enterKeyAction_{|me,string|	
					filename=string.filenameSafe;
					me.string_(filename);
					window.close;
					{this.saveInstToLibrary(class, saveList, filename, studioName)}.defer(0.1);
				}
				.focus.startEditing;	
	
			// Cancel
			MVC_OnOffView(scrollView,Rect(130-11-60, 55-11, 55, 20),"Cancel")
				.rounded_(true)  
				.color_(\on,Color(1,1,1,0.5))
				.color_(\off,Color(1,1,1,0.5))
				.action_{	 window.close };
				
			// Ok
			MVC_OnOffView(scrollView,Rect(130-11, 55-11, 50, 20),"Ok")
				.rounded_(true)  
				.color_(\on,Color(1,1,1,0.5))
				.color_(\off,Color(1,1,1,0.5))
				.action_{	
					window.close;
					{this.saveInstToLibrary(class, saveList, filename, studioName)}.defer(0.1);
				};
				
		}
			
	}
	
	// save this instrument to the library
	saveInstToLibrary{|class, saveList, filename, studioName|
		saveList.savePref("Library"+/+studioName+/+filename);
		visibleTypesGUI[class.studioName.asSymbol].expand;
		{this.refreshLibrary(class)}.defer(0.1);
	}
	
	// refresh the instrument library for this class only
	refreshLibrary{|class,forceRestore=false|
		var typeSymbol=class.studioName.asSymbol;
		// remove old widgets
		libraryGUI[typeSymbol].do(_.free);
		
		// refresh the filelist	
		this.initLibrary(forceRestore);
		// change the bounds
		visibleTypesGUI[typeSymbol].bounds_(
			visibleTypesGUI[typeSymbol].bounds.height_(
				instLibraryFileNames[typeSymbol].size*19+18 )
		);
		// add the new widgets
		this.addLibraryWidgets(class);
	}

	// restore inst library
	restoreLibraryDefaults{
		visibleTypes.do{|class|			
			var typeSymbol=class.studioName.asSymbol;
			// remove old widgets
			libraryGUI[typeSymbol].do(_.free);
		};
		// refresh the filelist	
		this.initLibrary(true);
		
		visibleTypes.do{|class|	
			var typeSymbol=class.studioName.asSymbol;	
			// change the bounds
			visibleTypesGUI[typeSymbol].bounds_(
				visibleTypesGUI[typeSymbol].bounds.height_(
					instLibraryFileNames[typeSymbol].size*19+18 )
			);
			// add the new widgets
			this.addLibraryWidgets(class);
		};
	}

}
