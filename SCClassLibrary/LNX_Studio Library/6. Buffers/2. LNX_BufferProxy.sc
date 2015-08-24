
// a LNX_BufferProxy  ////////////////////////////////////////////////////////////////////
//
// manages the various states a buffer could be in
//
/////////// use the url as the path for saving !!!!!!!!!!!!!!!!!!!!!!!!

LNX_BufferProxy {

	classvar <cashePath, <>verbose=false, <userPath, <trash;
	
	classvar <containers, <paths, tempPaths, <>rebootFunc;
	
	classvar >studio;
	
	var <server, <path, <>buffer, completionFunc, free=false;
	var <source, <url, <dir, <name, <download, <convertedPath;
	
	var <models, <sampleData, <resolution=200;
	
	*initClass {
		cashePath = "~/".absolutePath +/+ "music/LNX_Studio Sample Cache";
		containers=[];
		paths=[];
		trash=[];
		
		userPath = cashePath +/+ "file";
		
		if (userPath.pathExists(false).not) { userPath.makeDir }; // make the dir
		
		[
			"LNX_Studio Local Sound Folder",
			"=============================",
			"",
			"This is the local sounds folder, any sound files you place in here",
			"will be available in LNX_Studio.",
			"If you wish to use this content in a collaboration make sure all users",
			"have the same content in this folder."
		].saveList(userPath+/+"READ ME.txt");
			
	}
	
	// from a URL /////////////////////////////////////////////////////////////
	
	isURL{ ^source === \url }
	
	*url {|server,url,action,replace| ^super.new.initURL(server,url,action,replace) }
	
	// this only inits a url, the loading is done in init called at the bottom
	initURL {|argServer, argURL, argCompletionFunc, replace=false|
		
		this.initInstance;
		this.initModels;
		
		// source is url & make a filename path from the url
		source = \url;	
		server = argServer ? Server.default;
		url    = argURL;
		path   = cashePath +/+ url.replace("://","/");
		path   = path.replace("%20", " ");
		dir    = path.dirname;
		name   = path.basename;

		convertedPath = path.getNewPath(LNX_URLDownload.format);
		
		sampleData = [0];
		
		completionFunc = argCompletionFunc;
		
		// does it already exist
		if (this.actualPath.pathExists(false)) {
		
			// is it a sound file ?
			if (this.actualPath.isSoundFile) {	
				// we have it in the cashe as a sound	 file
				models[\percentageComplete].value_(-3);
				if (verbose) { "Using existing file in cashe, completed".postln };
				
			}{
				// is the path not a part of a current download ?
				if (LNX_URLDownloadManager.pathAsDownload(path).isEmpty) {
					
					if (verbose) {"File in cashe is not a sound file".postln };
					path.removeFile(false,false,false);  // so delete it
					this.requestDownload(replace);
					
				}{
					// let the other download finish and update this buffer in reload
					download = LNX_URLDownloadManager.pathAsDownload(path).choose;
					// the can be only one
					if (verbose) {"Waiting for a download alreadying doing this".postln };
				};
			};	
		}{
			this.requestDownload(replace);
		};
		
		// use init to load the path / soundFile
		this.init( server, path, { completionFunc.value(this) } );
	
	}
	
	initModels{
		models=();
		models[\percentageComplete] = [-1,[-5,100]].asModel;
	}
	
	// request a new download from the LNX_URLDownloadManager
	requestDownload{|replace,runFunc=true|
		download = LNX_URLDownloadManager(url, path, {|download|
			if (runFunc) {
				this.reload(completionFunc); 		// now we can load it from the cashe
			}{
				this.reload
			};
			if (verbose) { "Completed".postln };
		}, replace);		
	}
	
	
	// the autual path used to load a sound to the server, this maybe different due to converstion
	actualPath{ if (convertedPath.isSoundFile) {^convertedPath} {^path} }
	
	// what if another BufferProxy is using this download ?? Should I check?
	// yes i should, i need to do this !!!
	cancelDownload{ if (download.notNil) {download.cancelDownload} }
	retryDownload{ if (download.notNil) {download.retryDownload} }
	
	status{ if (download.notNil) {^download.status} {^\finished} }
	info{ if (download.notNil) { ^download.info } {^nil} }
	
	// class & init //////////////////////////////////////////////////////////
	
	*serverReboot{
		rebootFunc={ tempPaths=nil };
		tempPaths=[];
		if (verbose) { "Loading buffers...".postln };
		this.recursiveLoad(0);
	}

	// this is a class method to load all buffers
	*recursiveLoad{|i|
		var ifPresentIndex, buf;
		if (i<containers.size) {
			
			studio.flashServerIcon; // gui
			
			buf=containers[i];	
			ifPresentIndex=nil;
			tempPaths.do({|p,j| if (buf.path==p) { ifPresentIndex=j }});
			tempPaths=tempPaths.add(buf.path);
			
			if (buf.server.serverRunning) {
			
				if (ifPresentIndex==nil) {
					if (buf.actualPath.isSoundFile) {
											
						LNX_BufferArray.read(buf.server, buf.actualPath, action: {|thisBuffer|
							buf.buffer=thisBuffer;
							
							buf.loaded;
							
							this.recursiveLoad(i+1);
						});
						
						if (verbose) { buf.path.postln }; // this is useful & annoying
						
					}{
						if (buf.source==\url) {
							if (buf.download.notNil) {
								buf.download.forceRedownload; // if we have a downlaod reload
							}{
								buf.requestDownload(runFunc:false); // else request a new one
							};
						}{
							if (verbose) {
								"RecursiveLoad failed, couldn't find: ".post;
								buf.path.postln
							};
						}; 
									
						this.recursiveLoad(i+1);
	
					};
				}{
					buf.buffer=containers[ifPresentIndex].buffer;
					this.recursiveLoad(i+1);
				};
			
			};
		}{
			rebootFunc.value;
			studio.stopFlashServerIcon;
		};
		
	} // recursive
	
	// from a FILE /////////////////////////////////////////////////////////////////
	
	*read {|server,path,action| ^super.new.initRead(server,path,action) }
	
	initRead { |argServer, argPath, argAction|
		source = \file;
		this.initInstance;	
		this.initModels;
		this.init(argServer, argPath, argAction);
	}
	
	// All buffers use this. init files & urls ////////////////////////////////////////
	
	loaded{ LNX_SampleBank.callUpdate(this) }
	
	isLoaded{ ^buffer.isNil.not }
	
	// where as this is an instance method and loads just one buffer
	init {|argServer, argPath, argAction|
	
		var ifPresentIndex=false;

		server = argServer ? Server.default;		
		path   = argPath;
		paths.do({|p,j| [p,path]; if (p==path) { ifPresentIndex=j }});
		paths  = paths.add(path);
		
		if (ifPresentIndex==false) {
			if (server.serverRunning) {
				
				if (this.actualPath.isSoundFile) {
					
					buffer=LNX_BufferArray.read(server, this.actualPath, action: {|buf|
						argAction.value; // what is this for?
										// it is only used in recursiveLoad
					});
					
					this.loaded;
					
					if (verbose) { "Loaded: ".post; path.postln }; // useful but annoying
				}{
					if (source==\file) {
						if (verbose) { "Init failed, couldn't find: ".post; path.postln };
					}{
						if (verbose) { "Not found in cashe: ".post; path.postln };
					};
				};
			}{
				{argAction.value}.defer(0.001); 
				// this defer is need else the add action will not add this
				// what does this mean & is it really true
			};
		}{
			buffer=containers[ifPresentIndex].buffer;
			{argAction.value}.defer(0.001);
			// this defer is need else the argAction will not recieve add this
		};
		
	}
	
	// reload called by completed download action
	reload{|action|
		if (server.serverRunning) {
			if (this.actualPath.isSoundFile) {	
				buffer=LNX_BufferArray.read(server, this.actualPath, action: {|buf|
					//argAction.value;
					if (verbose) { path.postln };
					// check for others that may use this
					containers.do{|bufferProxy|
						if (bufferProxy.path==path) {
							bufferProxy.buffer = buf;
						};
					};
					action.value(this);
				});
				this.loaded;
			}{
				if (verbose) { "Reload failed, couldn't find: ".post; path.postln };
			};	
		}
	}
	
	// play this buffer
	play {|loop = false, mul = 1, offset = 0|
		if (buffer.notNil) {
			^buffer.play(loop,mul,offset);
		}{
			^nil
		}
	}
	
	initInstance{ containers=containers.add(this) }
	
	getWithRef{|index,action,ref1,ref2,channel|
		buffer.getWithRef(index,action,ref1,ref2,channel)
	}
	
	bufnum		{ ^buffer.notNil.if{buffer.bufnum} }
	numFrames		{ ^buffer.notNil.if{buffer.numFrames} }
	numChannels	{ ^buffer.notNil.if{buffer.numChannels} }
	sampleRate    { ^buffer.notNil.if{buffer.sampleRate} }
	duration      { ^buffer.notNil.if{buffer.duration} }
	
	// has this been freed
	isFree{^free}
		
	// *** What about using a trash can ??	
		
	// free this BufferProxy and any buffers & downloads not being used elsewhere
	
	// this will empty the trash
	*emptyTrash{
		if (verbose) {"Trash emptied".postln;};
		trash.do(_.freeNow);
		trash=[];
	}
	
	// free is really add to trash
	free{ 
		if (verbose) {"Added to trash: ".post; this.postln};
		trash=trash.add(this);
	} 
	
	// and this is really the free method
	freeNow{
		var ifPresent=false, index;
			
		if (free.not) {	

			// only stop the download if not used by others
			if ((source==\url)and:{download.notNil}) {
				if (containers.select{|c| c.download==download }.size<=1) {
					download.free;
				}
			};
			
			// only remove the buffer if not used by others
			paths.do{|p,j| if ((p==path)&&{containers[j]!=this}) { ifPresent=true } };
			if (ifPresent==false) {
				if (verbose) {"Free: ".post; path.postln};
				buffer.free;
				
			};
			
			// and remove this from the container
			index = containers.indexOf(this);
			paths.removeAt(index);
			containers.removeAt(index);
			free=true;
		}
	 }
	 
	 // clock safe versions //////////////////////////////////////
	 
	 // a clock safe version of cashe
	 *fetchCashe{|action|
		var list=[];	
		var nonUserContent = this.nonUserContent; // just a few folders here
		var done = 1 ! (nonUserContent.size);
		
		nonUserContent.do{|folder,i|
			FolderContents.fetchFolderContents(folder,inf,{|contents|
				list = list ++ (contents.select(_.isLNXSoundFile));
				done[i] = 0;
				if (done.sum==0) {action.value(list)};
			});
		};
		
	}
	
	// a clock safe version of easyCashe
	*fetchEasyCashe{|action|
		var calls = 1, modInterval=300;
		var size=cashePath.size+1;	
		{ 
			this.fetchCashe{|cashe|
				cashe = cashe.collect{|s|
					if (calls%modInterval==0) { 0.01.wait };
					calls = calls + 1;
					s = s[size..].split;
					s[0] = s[0] ++ ":/";
					s.join($/);
				};
				action.value(cashe);
			}	
		}.fork(AppClock)
	}
		
	*fetchUserContent{|action|
		FolderContents.fetchFolderContents(userPath,inf,{|contents|
				action.value(contents.select(_.isLNXSoundFile));
		});
	}
	
	*fetchEasyUserContent{|action|
		var calls = 1, modInterval=300;
		var size=cashePath.size+1;	
		{ 
			this.fetchUserContent{|cashe|
				cashe = cashe.collect{|s|
					if (calls%modInterval==0) { 0.01.wait };
					calls = calls + 1;
					s = s[size..].split;
					s[0] = s[0] ++ ":/";
					s.join($/);
				};
				action.value(cashe);
			}	
		}.fork(AppClock)
	}
	
	/////////////////////////
	
	*nonUserContent{
		var tempString = (userPath++"/").toLower;
		^(cashePath++"/").folderContents(0).select{|f| f.toLower!=tempString};
	}
	
	*cashe{
		var list=[];
		
		this.nonUserContent.do{|folder|
			list=list++(folder.folderContents.select(_.isLNXSoundFile))
		};
		^list;
		
	} // don't use .isSoundFile
	
	*easyCashe{
		var size=cashePath.size+1;	
		^this.cashe.collect{|s|
			s = s[size..].split;
			s[0] = s[0] ++ ":/";
			s.join($/);
		}
	}
	
	///////////////////
	
	*userContent{
		^userPath.folderContents.select(_.isLNXSoundFile)
	}
	
	*easyUserContent{
		var size=userPath.size- ("file".size);
		^this.userContent.collect{|s|
			s = s[size..].split;
			s[0] = s[0] ++ ":/";
			s.join($/);
		}
	}
	
}

// this is a AppClock safe version of folderContents

FolderContents {
	
	classvar calls=1, modInterval=150; // how many calls before a 0.01 wait
	
	*fetchFolderContents{|string, levels = inf, action|
		{
			action.value(this.folderContentsWait(string, levels)); // call folderContentsWait
		}.fork(AppClock);					                      // on the AppClock
	}
	
	*folderContentsWait { |string, levels = inf| // all folder contents
		var out = [];

		out = (string.standardizePath ++ "*").pathMatch; // changed
		if (levels != 0) {
			out = out.collect{ |item|
				
				calls = calls + 1;                       // inc calls
				if (calls%modInterval==0) { 0.01.wait }; // wait 0.01 after modInterval
				
				if( item.last == $/ ) {
					this.folderContentsWait(item, levels - 1); // next folder
				}{
					item; // or its an item
				};
			};
		};
		^out.flatNoString;
	}
			
}


///////////////////////////////

+ Nil { isSoundFile {^false} }
