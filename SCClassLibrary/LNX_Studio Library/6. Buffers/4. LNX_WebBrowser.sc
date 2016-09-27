//
// ****	A WebBrowser for LNX
//       used for getting the url's of sound files and more to follow ******
//
/*

LNX_WebBrowser(s).open; // magic!

*/

LNX_WebBrowser{

	classvar <formats, <homePage, <favourites, <history, <historyMenuItems, <historyFunc,
			<classModels, <userFilesText, <guiMenuItems, <guiMenuFuncs, <guis, <previewBank;

	var <server, <gui, <window, <url, <downloads, <sampleBank, failedURL, lastURL, listView;

	// init the class
	*initClass {

		Class.initClassTree(MVC_NumberFunc); // for \switch below

		// we can load the following formats : )
		formats = #[
			'3gpp', '3gp', '3gp2', '3g2', 'aac', 'adts', 'ac-3', 'ac3', 'aifc', 'aiff',
			'aif', 'amrf', 'amr', 'm4af', 'm4a', 'm4r', 'm4bf', 'm4b', 'caff', 'caf',
			'mpg1', 'mp1', 'mpeg', 'mpa', 'mpg2', 'mp2', 'mpg3', 'mp3', 'mp4f', 'mp4',
			'next', 'snd', 'au', 'sd2f', 'sd2', 'wav', 'wave', 'ircam', 'sun', 'mat4',
			'mat5', 'paf', 'svx', 'nist', 'voc', 'w64', 'pvf', 'xi', 'htk', 'sds',
			'avr', 'flac'
		];

		guis = IdentitySet[];

		this.loadHomePage;
		this.loadFavourites;
		this.makeMenus;

		history = [];
		this.makeHistoryMenu;

		// class models
		classModels = IdentityDictionary[];
		classModels[\preview]= \switch.asModel.value_(1);

		this.refreshUserFiles;

	}

	*new{|server,sampleBank| ^super.new.init(server,sampleBank) }

	// init stuff
	init{|argServer,argSampleBank|
		server      = argServer;
		url         = homePage;
		downloads   = [];
		sampleBank  = argSampleBank ?? {LNX_SampleBank(server, apiID:String.rand)}; // 1 user only
		if (previewBank.isNil) { // so we only have a singleton and can use server
			previewBank = LNX_SampleBank.newP(server, apiID:"__preview"++String.rand); // for preview
		};
		this.createWidgets;
	}

	free{
		sampleBank.removeDependant(this);
		guis.remove(gui);
	}

	// set the webView url
	url_{|argUrl|
		url = argUrl;
		lastURL=nil;
		if (url.contains("://").not) { url = "http://"++url }; // this is not needed Qt version
		gui[\ ].url_(url);
	}

	// a url link was clicked on
	onLink_{|url|
		var ext = url.extension;    // had to change wslib a bit
		if (url=="") { ^this };     // exception
		url=url.replace("%20"," "); // replace %20 with space
		url=url.replace("%34",34.asAscii.asString); // reverse replace

		if (ext.notNil) {
			ext=ext.toLower.asSymbol;
			if (formats.includes(ext)) {      // is recognised audio
				gui[\urlView].string_(url);  // set gui string
				this.download(url);          // & download
			}{
				gui[\webView].url_(url);     // webview
			}
		}{
			gui[\webView].url_(url);          // webview
		}
	}

	// download the url into a LNX_SampleBank & a LNX_BufferProxy
	download{|url|
		var path = LNX_BufferProxy.cashePath +/+ url.replace("://","/");

		// if .aiff exists in cashe see if another format exists so it can be used
		// for others on the network. easy way to reslove loading from cashe
		// to saved song to others downloading the same content.
		if ((url.extension.toLower=="aiff") and: {path.pathExists(false)}) {
			var parent = PathName(path).parentPath;
			var content = parent.folderContents;
			content = content.select{|file| file.extension.toLower!="aiff"};
			if (content.size>0) {
				url = url.removeExtension++"."++(content.first.extension);
			};
		};
		sampleBank.guiAddURL(url);
	}

	preview{^classModels[\preview].value.isTrue}

	*preview{^classModels[\preview].value.isTrue}

	// gui /////////////////////////////////////////////////////////////////////////////

	// states
	isOpen{ if (window.isNil) {^false} {^ window.isClosed.not } }
	isClosed{^this.isOpen.not}

	// open me if not already created
	open{
		if (window.isClosed) {
			window.create;
			window.open;
		}{
			window.front;
		}
	}

	// now close me
	close{ if (window.notNil) {window.close} }

	// create the gui
	createWidgets{

		var bounds, findi=1, lastFindString;

		gui = IdentityDictionary[]; // there is only 1 gui so this can go into inst vars

		gui[\buttonTheme ] = (\colors_ : (	\up    : Color(6/11,42/83,29/65),
										\down  : Color(6/11,42/83,29/65)/2,
					 					\string: Color.white),
							\rounded_ : true);

		gui[\buttonTheme2 ] = (\colors_ : (	\up    : Color(6/11,42/83,29/65),
										\down  : Color(6/11,42/83,29/65)/2,
					 					\string: Color.black),
							\rounded_ : true);

		gui[\onOffTheme ] = (\colors_ : (	\on    : Color(0.4, 1, 0.4),
										\off   : Color(6/11,42/83,29/65)/1.8,
					 					\string: Color.white),
							\rounded_ : true);

		gui[\textTheme] = (	\canEdit_:true,
							\hasBorder_:true,
							\shadow_:false,
							\enterStopsEditing_:true,
							\font_:Font("Helvetica",15),
							\colors_:(
								\edit:Color.black,
								\focus:Color(0.5, 0.5,0.5),
								\string:Color.black,
								\editBackground:Color.white,
								\background:Color.white,
								\border:Color(0.33,0.33,0.33)
							)
						);

		// the window
		window = MVC_Window("",Rect(215,50,1000,630))
			.color_(\background,Color(0,1/103,3/77) );

		bounds = window.bounds.copy;

		gui[\window]=window;

		gui[\masterCom] = MVC_RoundedComView(window, Rect(9,9,bounds.width-18,bounds.height-17),
				( \background:Color(0.40,0.40,0.40),
					\border: Color(42/83,29/65,6/11),
					\border2: Color(0,1/103,3/77) )
		).resizeAction_{
			listView.bounds_(Rect(3,30,window.bounds.width-141, window.bounds.height-74));
		};

		// the Web / Dialog tab view
		gui[\tabView] =MVC_TabbedView(gui[\masterCom],Rect(115,2,bounds.width-133,bounds.height-22))
			.action_{|me|
			{listView.bounds_(Rect(3,30,window.bounds.width-141, window.bounds.height-74))}.defer(0.1);
			}
			.labels_(["Web","Dialog"])
			.resize_(5)
			.font_(Font("Helvetica", 14))
			.tabPosition_(\top)
			.unfocusedColors_( Color(0.6,0.6,0.6)!2)
			.labelColors_(   Color(0.8,0.8,0.8)!2 )
			.backgrounds_(  Color(0.8,0.8,0.8)!2 )
			.tabCurve_(5)
			.tabHeight_(18)
			.followEdges_(true)
			.value_(1)
			.resizeAction_{};

		gui[\webTab] = gui[\tabView].mvcTab(0);

		gui[\browserCompositeView] = MVC_CompositeView(gui[\webTab],Rect(0,2,bounds.width-131,bounds.height-40))
			.resize_(5)
			.hasBorder_(false); // but this shows a different story

		this.createDialogWidgets(gui[\tabView].mvcTab(1),bounds);

		// the main web view (***!!!! this must be before ANY ScrollView else it hangs!!!***)
		gui[\webView] = MVC_WebView(gui[\browserCompositeView],
			gui[\browserCompositeView].bounds.size.asRect.resizeBy(-5,-60).moveBy(0,29))
			.resize_(5)
			.url_(url)
			.onLinkActivated_{|view,url|
				[view,url].postln;
				switch (url)
					{"http://***LNX_DeleteCashe.mp3"} {
						LNX_URLDownloadManager.deleteCashe;
						this.showCasheHTML("LNX_Studio");
						window.name_("Cashe");
					} {"http://***LNX_OpenLocalFilesFolder.mp3"} {
						(LNX_BufferProxy.userPath+/+"READ ME.txt").revealInFinder;
					} {"http://***LNX_Refresh.mp3"} {
						this.class.refreshUserFiles(action:{
							this.showCasheHTML("LNX_Studio");
							window.name_("Cashe");
						});
					}{
						this.onLink_(url);
					};
			}
			.onLoadFinished_{|view|
				if (view.url[0..14]!="applewebdata://") {
					url=view.url;
					LNX_WebBrowser.addToHistory(url);
					gui[\addressView].string_(url);
					window.name_(view.title);
					lastURL=nil;
				};
			}.onLoadFailed_{|view|
				var text="";
				failedURL=url;
				text = text ++ "Failed - Could not connect to: "++failedURL ++ "<br/>"++ "<br/>";
				this.showCasheHTML(text);
			};

		// the sampleBank (***!!!! this must be after ANY WebView else it hangs!!!***)
		gui[\sampleBankCompositeView] = MVC_ScrollView(window,Rect(9,9,112,bounds.height-50))
			.color_(\background, Color.new255(142,142,142))
			.autoScrolls_(true)
			.hasVerticalScroller_(true)
			.hasHorizontalScroller_(false)
			.resize_(4)
			.hasBorder_(true);

		sampleBank.window_(gui[\sampleBankCompositeView]);

		// the address text field
		gui[\addressView] = MVC_Text(gui[\browserCompositeView],gui[\textTheme],Rect(85, 3,
									gui[\browserCompositeView].bounds.width-119, 20))
			.string_(url)
			.resize_(2)
			.enterKeyAction_{|me,string|
				if (string.contains("://")) {
					gui[\webView].url_(string);
				}{
					gui[\webView].url_("http://"++string);
				};
			};

		// the download url text field
		gui[\urlView] = MVC_Text(gui[\browserCompositeView],gui[\textTheme],Rect(54, 565,
							 gui[\browserCompositeView].bounds.width-266, 20))
			.string_("")
			.resize_(8)
			.canEdit_(true)
			.enterKeyAction_{|me,string|
				var ext = string.extension;
				if (ext.notNil) {
					ext=ext.toLower.asSymbol;
					if (formats.includes(ext)) { // is recognised audio
						this.download(string);
					};
				};
			};

		// find text
		gui[\findTextView] = MVC_Text(gui[\browserCompositeView],gui[\textTheme],
						Rect(gui[\browserCompositeView].bounds.width-155,565,120, 20))
			.string_("")
			.resize_(9)
			.enterStopsEditing_(false)
			.enterKeyAction_{|me,string|
				if (string!=lastFindString) {findi=1};
				gui[\webView].findText(gui[\findTextView].string);
				findi=findi+1;
				lastFindString=string;
			};

		// find button
		MVC_FlatButton(gui[\browserCompositeView],
			Rect(gui[\browserCompositeView].bounds.width-202, 565, 40, 20),
											gui[\buttonTheme2 ] ,"Find")
			.font_(Font("Helvetica", 12, true))
			.resize_(9)
			.canFocus_(false)
			.action_{
				var string=gui[\findTextView].string;
				if (string!=lastFindString) {findi=1};
				findi.do{ gui[\webView].findText(gui[\findTextView].string) };
				findi=findi+1;
				lastFindString=string;
			};

		// filter
		MVC_FlatButton(gui[\browserCompositeView] , "search", gui[\buttonTheme2 ],
					Rect(gui[\browserCompositeView].bounds.width-28, 565, 20, 20))
			.rounded_(true)
			.mode_(\icon)
			.resize_(9)
			.action_{
				var string=gui[\findTextView].string;
				if (string.size>0) {

					gui[\webView].html_("<h1>Searching for \""++string++"\"</h1>");

					this.showCasheHTML("Search results for \""++string++"\"",true,string);

				}{
					this.showCasheHTML("LNX_Studio");
				};
			};

		// back
		MVC_FlatButton(gui[\browserCompositeView],Rect(6,3,20,20),gui[\buttonTheme2],"back")
			.mode_(\icon)
			.action_{
				if (lastURL.notNil) {
					this.url_(lastURL)
				}{
					gui[\webView].back;
				};
			};

		// forward
		MVC_FlatButton(gui[\browserCompositeView],Rect(30, 3, 20, 20),
											gui[\buttonTheme2] ,"play")
			.mode_(\icon)
			.action_{gui[\webView].forward};

		// download selected url
		MVC_FlatButton(gui[\browserCompositeView],Rect(8, 565, 20, 20),
											gui[\buttonTheme2 ] ,"down")
			.mode_(\icon)
			.resize_(7)
			.action_{
				var string = gui[\urlView].string;
				var ext = string.extension;
				if (ext.notNil) {
					ext=ext.toLower.asSymbol;
					if (formats.includes(ext)) { // is recognised audio
						this.download(string);
					};
				};
			};

		// file icon
		MVC_Icon(gui[\browserCompositeView],Rect(28, 560, 30, 30))
			.icon_("file")
			.color_(\iconDown,Color.white)
			.resize_(7)
			.action_{
				lastURL=url;
				this.showCasheHTML("LNX_Studio");
				window.name_("Cashe");
			};

		// favourites menu
		gui[\favMenu] = MVC_PopUpMenu3( gui[\browserCompositeView], Rect(60, 5, 17, 17))
			.color_(\background,Color(6/11,42/83,29/65))
			.staticText_("")
			.showTick_(false)
			.items_(guiMenuItems)
			.action_{|me|
				guiMenuFuncs[me.value].value(me,gui,this);
				me.value_(0);
			};

		// History menu
		gui[\historyMenu] = MVC_PopUpMenu3( gui[\browserCompositeView],
					 Rect(gui[\browserCompositeView].bounds.width-27, 5, 17, 17))
			.color_(\background,Color(6/11,42/83,29/65))
			.staticText_("")
			.showTick_(false)
			.items_(historyMenuItems)
			.resize_(3)
			.action_{|me|
				historyFunc[me.value].value(me,gui,this);
				me.value_(0);
			};



		MVC_PlainSquare(window, Rect(9, 589, 111, 32) )
			.color_(\on,Color(0.23,0.23,0.23))
			.color_(\off,Color(0.23,0.23,0.23))
			.resize_(7);

		// sample List menu
		MVC_PopUpMenu3(window, Rect(15,596,17,17))
			.color_(\background,Color(6/11,42/83,29/65))
			.resize_(7)
			.staticText_("")
			.showTick_(false)
			.items_(["Downloads","-","Stop Downloads","Retry failed Downloads ","-",
					"Copy all Downloads","Paste all Downloads"])
			.action_{|me|
				if (me.value==2) { LNX_URLDownloadManager.cancelAllDownLoads };
				if (me.value==3) { LNX_URLDownloadManager.retryAllFailedDownloads };
				if (me.value==5) { sampleBank.copyAll };
				if (me.value==6) { sampleBank.pasteAll };
				me.value_(0);
			};

		// delete
		MVC_FlatButton(window, Rect(38,595,22,20), "delete", gui[\buttonTheme2])
			.mode_(\icon)
			.resize_(7)
			.action_{sampleBank.deleteSelectedSample};

		// edit meta
		MVC_FlatButton(window, Rect(65,595,22,20), "sine")
			.color_(\up,Color(35/48,35/48,40/48)/3 )
			.color_(\down,Color(35/48,35/48,40/48)/3 )
			.color_(\string,Color.white)
			.rounded_(true)
			.mode_(\icon)
			.resize_(7)
			.action_{ sampleBank.openSelectedMeta(window)};

		// previewp
		MVC_OnOffView(window, Rect(92,595,22,20), classModels[\preview], gui[\onOffTheme ], "speaker")
			.resize_(7)
			.mode_(\icon);


		MVC_PlainSquare(window, Rect(120, 589, 1, 32) )
			.color_(\on,Color.black)
			.color_(\off,Color.black)
			.resize_(7);

		MVC_PlainSquare(window, Rect(9, 589, 1, 32) )
			.color_(\on,Color.black)
			.color_(\off,Color.black)
			.resize_(7);

		MVC_PlainSquare(window, Rect(9, 589+32, 111, 1) )
			.color_(\on,Color.black)
			.color_(\off,Color.black)
			.resize_(7);

		guis=guis.add(gui);

	}

	// make the widgets for the dialog browser
	createDialogWidgets{|window,bounds|
		var basepath = (LNX_BufferProxy.cashePath)++"/"; 	// base folder for cashe files
		var fileBase = basepath ++ "file/";				 	// user folder
		var path = fileBase;                             	// current path
		var history = [ path ];                          	// history of navigation
		var contents, folders, files, items, itemNames, textView;
		// function that creates lists based on contents of path and sets gui
		var pathContents = {
			contents  = path.folderContents(0);			 	// everything in path
			contents  = contents.sort{|a,b| (a.toLower) < (b.toLower)};	// case insentive sory
			folders   = contents.select{|i| i.isFolder };	// just the folders
			if (path==basepath) {
				folders.removeAt(folders.indexOfString(fileBase)); // leave file out of basefolder
			};
			// and these are the sound files (selected by LNX_WebBrowser.formats)
			files     = contents.select{|i| (LNX_WebBrowser.formats.includes(i.extension.toLower.asSymbol)) };
			items     = folders ++ files;				 	// all files & folders
			itemNames = folders.collect{|i| "./" ++ i.basename} ++ files.collect(_.basename); // friendly names
			listView.items_(itemNames);
			listView.value_(0);
			textView.string_("."++path.dropFolder(4));
		};

		// list view of path items
		listView=MVC_ListView2(window,Rect(3,30,bounds.width-141,bounds.height-74))
			.color_(\background, Color.black)
			.folderDialog_(true)
			.items_([])
			.font_(Font("Helvetica",14,true))
			.actions_(\upDoubleClickAction,{|me|
				var index=me.value.asInt;
				if (items[index].isFolder) {       					// if its a folder
					path=items[index];           					// make the path this
					history = history.add(path);   					// add to history
					pathContents.();               					// update to this new path
				    listView.zeroOrigin;           					// scoll to top
				};
			})
			.actions_(\anyClickAction,{|me|
				var index=me.value.asInt;
				if (items[index].isFolder.not) {					// if not folder is sound file
					var file = items[index][basepath.size..];		// remove basepath from filename
					var firstDir = PathName(file).diskName;			// what is the 1st folder called
					file = firstDir++":/"++file[firstDir.size..];	// now use this to add prefix file://
					sampleBank.guiAddURL(file);						// add the sample to the bank
				};
			})
			.actions_(\enterKeyAction,{|me|
				var index=me.value.asInt;
				if (items[index].isFolder.not) {					// if not folder is sound file
					var file = items[index][basepath.size..];		// remove basepath from filename
					var firstDir = PathName(file).diskName;			// what is the 1st folder called
					file = firstDir++":/"++file[firstDir.size..];	// now use this to add prefix file://
					sampleBank.guiAddURL(file);						// add the sample to the bank
				};
			})
			.actions_(\spaceKeyAction,{|me|
				var index=me.value.asInt;
				if (items[index].isFolder.not) {					// if not folder is sound file
					var file = items[index][basepath.size..];		// remove basepath from filename
					var firstDir = PathName(file).diskName;			// what is the 1st folder called
					file = firstDir++":/"++file[firstDir.size..];	// now use this to add prefix file://
					previewBank.guiAddURL(file,select:false);		// add the sample to the bank
				if (previewBank.size>1) {previewBank.netRemove(0)}; // remove 1st item so preview doesn't grow
				};
			});

		// text view of current path
		textView = MVC_Text(window, Rect(120,5,600,25))
			.color_(\string,Color.black)
			.string_("")
			.shadow_(false)
			.mouseDownAction_{ textView.color_(\string,Color.white) }
			.mouseUpAction_{
				textView.color_(\string,Color.black);
				(items[listView.value.asInt]).revealInFinder;		// open in os
			};

		pathContents.(); // create lists for 1st creation

		// back
		MVC_FlatButton(window,Rect(5,5,20,20),gui[\buttonTheme2],"back")
			.mode_(\icon)
			.action_{
				if (history.size>1) {			// only go back if size>1
					history = history.drop(-1);	// remove the lastest item
					path = history.last;		// and goto previous item
				};
				pathContents.();				// update to this new path
				listView.zeroOrigin;			// zero the list origin
			};

		// all button
		MVC_FlatButton(window, Rect (30,5,33,20),gui[\buttonTheme2],"Web")
			.font_(Font("Helvetica", 12, true))
			.action_{
				path=basepath;					// basepath has access to web cashe
				pathContents.();				// update to this new path
				listView.zeroOrigin;			// zero the list origin
				history = history.add(path);	// add to history
			};

		// local button
		MVC_FlatButton(window, Rect (68,5,46,20), gui[\buttonTheme2], "Local")
			.font_(Font("Helvetica", 12, true))
			.action_{
				path=fileBase;					// filebase is local content
				pathContents.();				// update to this new path
				listView.zeroOrigin;			// zero the list origin
				history = history.add(path);	// add to history
			};

	}

	/////////////////////////////////////////////////////////////////////////////////////////////

	// these methods are designed to be clock safe, i.e help prevent "lates"

	// create the html code for the cashe and show it
	showCasheHTML{|prefix="",filter=false,filterString|
		if (filter) {
			this.showCasheHTMLFilter(prefix,filter,filterString)
		}{
			this.showCasheHTMLNoFilter(prefix)
		}
	}

	// filter it
	showCasheHTMLFilter{|prefix="",filter=false,filterString|
		var calls = 1, modInterval=600;
		var text, size;

		{
			this.class.filterUserFiles(true,filterString, {|userFiles|

				LNX_BufferProxy.fetchEasyCashe{|cashe|

					text = cashe.collect {|str|
						if (calls%modInterval==0) { 0.01.wait };
						calls = calls + 1;
						"<a href='%'>%</a><br/>".format( str,str)
					};

					if (filterString.size>0) {
						filterString=filterString.toLower;
						text=text.select{|s|
							if (calls%modInterval==0) { 0.01.wait };
							calls = calls + 1;
							s.toLower.contains(filterString);

						};
					};

					size = text.size;
					text = text.join;

					text = [
						"<h1>",prefix,"</h1>",
						userFiles,

						"<br/>",


						"The Cashe.<br/>Found ",
						size,
						" sounds out of ",
						cashe.size ,
						" in the cashe� ",
				"<a href='http://***LNX_DeleteCashe.mp3'>[Delete all cashe files]</a><br/><br/>",
							text
					].join;

					gui[\webView].html_(text);

				};

			});

		}.fork;

	}

	showCasheHTMLNoFilter{|prefix=""|
		var calls = 1, modInterval=600;
		var text, size;
		{
			LNX_BufferProxy.fetchEasyCashe{|cashe|
				text = cashe.collect {|str|
					if (calls%modInterval==0) { 0.01.wait };
					calls = calls + 1;
					"<a href='%'>%</a><br/>".format( str,str)

				};
				size = text.size;
				text = text.join;
				text = [
					"<h1>",prefix,"</h1>",

					userFilesText,
					"<br/>",

					"The Cashe.<br/>There are ",
					size,
					" sounds available in the cashe� ",
			"<a href='http://***LNX_DeleteCashe.mp3'>[Delete all cashe files]</a><br/><br/>",
					text

				].join;
				gui[\webView].html_(text);

			};

		}.fork;

	}

	// make the html text for all the local files
	*refreshUserFiles{|filter=false,filterString,action|
		var calls = 1, modInterval=600;
		var text, size;

		{
			LNX_BufferProxy.fetchEasyUserContent{|userContent|

				text = userContent.collect {|str,i|
					if (calls%modInterval==0) { 0.01.wait };
					calls = calls + 1;
					"<a href=\"%\">%</a><br/>".format( str,str.drop(7));
				};

				size = text.size;
				text = text.join;

				userFilesText = [
					"Local Files.<br/>There are ",
					//userContent.size ,
					size,
					" sounds available in local files� " ++
				"<a href='http://***LNX_OpenLocalFilesFolder.mp3'>[Open local folder]</a>    ",
					"<a href='http://***LNX_Refresh.mp3'>[Refresh]</a><br/><br/>",
					text
				].join;

				action.value

			}
		}.fork

	}

	// filter / search local files for the string and make the html text
	*filterUserFiles{|filter=false,filterString,action|
		var calls = 1, modInterval=600;
		var text, size, filteredContent;

		{

			LNX_BufferProxy.fetchEasyUserContent{|userContent|

				if (filterString.size>0) {
					filterString=filterString.toLower;
					filteredContent = userContent.select{|s|
						if (calls%modInterval==0) { 0.01.wait };
						calls = calls + 1;
						s.toLower.contains(filterString);
					};
				}{
					filteredContent = userContent;
				};

				text = filteredContent.collect {|str|
					if (calls%modInterval==0) { 0.01.wait };
					calls = calls + 1;
					"<a href=\"%\">%</a><br/>".format( str,str.drop(7));
				};

				size = text.size;
				text = text.join;

				action.value(["Local Files.<br/>Found ",
					size,
					" sounds out of ",
					userContent.size ,
					" in local files� ",
				"<a href='http://***LNX_OpenLocalFilesFolder.mp3'>[Open local folder]</a>    ",
					"<a href='http://***LNX_Refresh.mp3'>[Refresh]</a><br/><br/>",
					text
				].join);

			}

		}.fork;
	}


	//////////////////////////////////////////////////////////////////////////////////////////////

	// get the home page from the prefernces file
	*loadHomePage{ homePage = ("Browser Home Page".loadPref ?? {
								["http://www.findsounds.com/types.html"]})[0] }

	// save the home page to the preferences file
	*saveHomePage{ [homePage].savePref("Browser Home Page") }

	// get the favourites from the prefernces file
	*loadFavourites{
		favourites = ("Browser Favourites".loadPref ?? {[
			"https://www.google.com",
			"http://www.findsounds.com/types.html",
			//"http://www.freesound.org/",
			"https://www.youtube.com/",
			"http://anything2mp3.com/",
			"http://www.soundjay.com/",
			"http://www.grsites.com/archive/sounds/",
			"http://www.mediacollege.com/downloads/sound-effects/"
		]})
	}

	// save the favourites to the prefernces file
	*saveFavourites{ favourites.savePref("Browser Favourites") }

	// create the menus and funcs for the favourites menu
	*makeMenus{
		guiMenuItems = ["Links","-","Home"];
		guiMenuFuncs = [nil,nil,nil];

		guiMenuItems = guiMenuItems.add("    "++(homePage.asSimpleAddress));
		guiMenuFuncs = guiMenuFuncs.add({|me,gui| gui[\webView].url_(homePage) });

		guiMenuItems = guiMenuItems++["-","Favourites"];
		guiMenuFuncs = guiMenuFuncs++[nil,nil];

		guiMenuItems = guiMenuItems++(favourites.collect{|s| "    "++(s.asSimpleAddress)});
		favourites.do{|s| guiMenuFuncs = guiMenuFuncs.add({|me,gui| gui[\webView].url_(s) }) };

		guiMenuItems = guiMenuItems++["-", "Add to Favourites",
			"Manage Favourites","-","Set Current Page as Home",
//			"-","Free Sound - API Key"
		];

		guiMenuFuncs = guiMenuFuncs.add(nil);

		// add to Favourites
		guiMenuFuncs = guiMenuFuncs.add({|me,gui,browser|
			if (favourites.containsString(browser.url).not) {
				favourites=favourites.add(browser.url);
				this.makeMenus;
				this.updateMenus;
				this.saveFavourites;
			};
		});

		// Manage Favourites
		guiMenuFuncs = guiMenuFuncs.add({|me,gui,browser|
			this.manageFavourites(me,gui,browser)
		});

		guiMenuFuncs = guiMenuFuncs.add(nil);

		// Set Current Page as Home
		guiMenuFuncs = guiMenuFuncs.add({|me,gui,browser|
			homePage=browser.url;
			this.makeMenus;
			this.updateMenus;
			this.saveHomePage;
		});

//		guiMenuFuncs = guiMenuFuncs.add(nil);
//
//		// Free Sound - API Key
//		guiMenuFuncs = guiMenuFuncs.add({|me,gui,browser|
//			this.freeSoundAPIKey(me,gui,browser)
//		});

	}

	// update all guis with new fav menu items
	*updateMenus{ guis.do{|gui| gui[\favMenu].items_(guiMenuItems) } }

	// add this url to the history
	*addToHistory{|url|
		var i= history.indexOfString(url);
		if (i.notNil) {history.removeAt(i)};
		history = history.insert(0,url);
		this.makeHistoryMenu;
		this.updateHistoryMenus;
	}

	// make the history menu items and funcs
	*makeHistoryMenu{
		historyMenuItems = ["History","-","Clear History","-"];
		historyFunc = [nil,nil,{this.clearHistory},nil];
		historyMenuItems = historyMenuItems++(history.collect{|s| "    "++(s.asSimpleAddress)});
		history.do{|s| historyFunc = historyFunc.add({|me,gui| gui[\webView].url_(s) }) };
	}

	// update all guis with new history items
	*updateHistoryMenus{ guis.do{|gui| gui[\historyMenu].items_(historyMenuItems) } }

	// clear the history and update gui
	*clearHistory{
		history=[];
		this.makeHistoryMenu;
		this.updateHistoryMenus;
	}

	// the gui for the free sound api key
	*freeSoundAPIKey{|me,parentGUI,browser|
		var gui=();

		gui[\window] = MVC_ModalWindow(parentGUI[\window],300@110,(
			background:	Color(59/77,59/77,59/77),
			border2:		Color(42/83,29/65,6/11),
			border1:		Color(0,1/103,3/77,65/77)
		));
		gui[\scrollView] = gui[\window].scrollView;

		MVC_Text(gui[\scrollView],Rect(0,5,280,20))
					.align_(\center)
					.string_("FreeSound API Key")
					.shadow_(false)
					.color_(\string,Color.black)
					.font_(Font("Helvetica",16));

		MVC_Text(gui[\scrollView],Rect(5,30,270,20), LNX_FreeSoundAPIKey.keyModel)
			.canEdit_(true)
			.shadow_(false)
			.hasBorder_(true)
			.color_(\string,Color.black)
			.color_(\border,Color.black)
			.color_(\edit,Color.black)
			.color_(\background,Color.white)
			.color_(\cursor,Color.black)
			.color_(\focus,Color(0,0,0,0.5))
			.color_(\editBackground, Color(1,1,1))
			.font_(Font("Helvetica",15));

		// Ok
		MVC_OnOffView(gui[\scrollView],Rect(220, 60, 50, 20),"Ok")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 gui[\window].close };
	}

	// the gui for managing the the favourites
	*manageFavourites{|me,parentGUI,browser|
		var gui=();
		var favGUI=[];
		var selected=0;
		var funcs=();
		var h=20;
		var lastY;

		funcs[\setColors] ={
			favGUI.do{|item,i|
				if (i==selected) {
					item.color_(\string,Color.white).color_(\background,Color(0,0,0,0.5));
				}{
					item.color_(\string,Color.black).color_(\background,Color(0,0,0,0));
				};
			};
		};

		funcs[\setStrings] ={
			favGUI.do{|item,i|
				item.string_(favourites[i]?"-")
			};
		};

		funcs[\addGUI] ={|fav,i|
			// the name
			favGUI = favGUI.add (
				MVC_StaticText(gui[\favScrollView],Rect(0,i*h,357,h))
					.string_(fav)
					.shadow_(false)
					.color_(\string,Color.black)
					.font_(Font("Helvetica",14))
					.mouseDownAction_{|me, x, y, modifiers, buttonNumber, clickCount|
						lastY=y;
						selected=i;
						funcs[\setColors].value;
					}
					.mouseMoveAction_{|me, x, y, modifiers, buttonNumber, clickCount|
						var j= (y-lastY/h).asInt;
						if (j!=0) {
							var to = (selected+j).clip(0,favourites.size-1);
							favourites.move(selected.asInt,to.asInt);
							selected=to;
							funcs[\setStrings].value;
							funcs[\setColors].value;
							lastY=y.round(h);
						};
					}
					.mouseUpAction_{
						LNX_WebBrowser.makeMenus.updateMenus.saveFavourites;
					}
			);
		};

		gui[\window] = MVC_ModalWindow(parentGUI[\window],400@400,(
			background:	Color(59/77,59/77,59/77),
			border2:		Color(42/83,29/65,6/11),
			border1:		Color(0,1/103,3/77,65/77)
		));
		gui[\scrollView] = gui[\window].scrollView;

		MVC_StaticText(gui[\scrollView],Rect(0,5,357,20))
					.align_(\center)
					.string_("Manage Favourites")
					.shadow_(false)
					.color_(\string,Color.black)
					.font_(Font("Helvetica",16));

		// fav scroll view
		gui[\favScrollView] = MVC_ScrollView(gui[\scrollView],Rect(10,30,357,310))
			.autoScrolls_(true)
			.color_(\background,Color(0,0,0,0.25))
			.hasVerticalScroller_(true);

		favourites.do(funcs[\addGUI]);

		funcs[\setColors].value;

		gui[\text]=MVC_Text(gui[\scrollView],Rect(40,350,243-15,20),"".asModel)
			.canEdit_(true)
			.shadow_(false)
			.hasBorder_(true)
			.color_(\string,Color.black)
			.color_(\border,Color.black)
			.color_(\edit,Color.black)
			.color_(\background,Color.white)
			.color_(\cursor,Color.black)
			.color_(\focus,Color(0,0,0,0.5))
			.color_(\editBackground, Color(1,1,1))
			.font_(Font("Helvetica",15));

		// add
		MVC_OnOffView(gui[\scrollView],Rect(317-25-18, 350, 35, 20),"Add")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{
				var url=gui[\text].string;
				favourites=favourites.add(url);
				this.makeMenus;
				this.updateMenus;
				this.saveFavourites;
				funcs[\addGUI].value(url,favourites.size-1);
			};

		// delete
		MVC_FlatButton(gui[\scrollView], Rect(10,350,22,20), "delete")
			.mode_(\icon)
			.color_(\up,Color(0,0,0,0.3))
			.color_(\down,Color(0,0,0,0.7))
			.rounded_(true)
			.resize_(7)
			.action_{
				if (favourites.size>0) {
					favourites[selected.asInt].remove;
					favourites.removeAt(selected.asInt);
					selected=selected.clip(0,favourites.size-1);
					funcs[\setStrings].value;
					funcs[\setColors].value;
					LNX_WebBrowser.makeMenus.updateMenus.saveFavourites;
				};
			};

		// Ok
		MVC_OnOffView(gui[\scrollView],Rect(317, 350, 50, 20),"Ok")
			.rounded_(true)
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 gui[\window].close };

	}

	///////////////////////////////////////////////////////////////////////////////////////////////

}

// this is different to swap and needs to happen in sampleBank as well

+ SequenceableCollection{
	move{|at,to| this.insert(to, this.removeAt(at)) }
}

+ String{
	asSimpleAddress{
		var string = this;
		if (this.find("://").isNumber) { string = string[string.find("://")+3..] };
		//if (string[..3]=="www.") { string=string.drop(4) };
		string = string.split($?)[0];
		if (string.last==$/) { string=string.drop(-1) };
		^string
	}
}

