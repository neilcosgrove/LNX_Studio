
// safe mode

LNX_Mode {
	
	// launch either SC or the standlone app in safe mode (as user nobody)
	// as a result all disk writes will fail, this also applys to any servers lauched in safe mode

	classvar <username, <isSafe;
	
	*initClass {
		Class.initClassTree(LNX_File);
		username="user";
		Pipe.do("whoami", {|name| username=name??"user" } );
		isSafe = (username=="nobody");
	}

	*launchSafeMode {|isStandalone=false, quitOnLaunch=false|
		var script = String.scDir +/+ "safeMode.bash";
		var app, result;
		if (isStandalone) {
			app = String.scDir.dirname.dirname +/+ "Contents/MacOS/SC-StandAlone";
		}{
			app = String.scDir +/+ "SuperCollider.app/Contents/MacOS/SuperCollider";
		};
		this.saveScript(script,["#!/bin/bash", "sudo -u nobody"+app.quote]);
		result = ("open -a Terminal" + script.quote).systemCmd;
		if (quitOnLaunch && (result==0)) { thisProcess.shutdown; 0.exit };
	}

	*saveScript{|path,list|
		var file;	
		file = File(path,"w");
		list.do({|line| file.write(line.asString++"\n") });
		file.close;
		("chmod +x" + path.quote).systemCmd;
	}
	
	*makeGUI{|parentWindow,func|
		var win, scrollView,texts;	
		win = MVC_ModalWindow(parentWindow, 420@(302-16));
		scrollView = win.scrollView;
		
		texts=[
			"LNX_Studio & Sandbox",
			"",
			"In this mode LNX_Studio has reduced disk write permissions.",
			"You will only be able to save or record songs in specific locations.",
			"",
			"This mode is in addition to the secuirty features already present",
			"and is ONLY recommended when collaborating...",
			"		1) with people you don't know",
			"		2) or in a public room on the internet.",
			"",
			"You will need to supply your password.",
			"",
			"Do you wish to quit LNX_Studio and relaunch in Sandbox mode?"
		];
	
		texts.do{|text,j|
			var adj=#[4,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,0]|@|j;
			var shadow=#[true,false]|@|j;
			var color=[Color.white,Color.black]|@|j;
			if (text.size>0) {
				MVC_StaticText(scrollView,Rect(10,10+(j*16),405-20,18+adj))
					.string_(text)
					.color_(\string,color)
					.shadow_(shadow)
					.font_(Font("Helvetica",13+adj))
					.align_(\left);
			}
		};

		// Cancel
		MVC_OnOffView(scrollView,Rect(242+15,248-16, 55, 20),"Cancel")
			.rounded_(true)  
			.color_(\on,Color(1,1,1,0.5))
			.color_(\off,Color(1,1,1,0.5))
			.action_{	 win.close };
			
		// Ok
		MVC_OnOffView(scrollView,Rect(305+15,248-16, 70, 20),"Sandbox")
			.rounded_(true)  
			.color_(\on,Color(0.25,1,0.25,0.75))
			.color_(\off,Color(0.25,1,0.25,0.75))
			.action_{	
				//Server.quitAll;
				func.value;
				win.close;
				{
					LNX_Mode.launchSafeMode(LNX_Studio.isStandalone,true)
				}.defer(0.2);
			};			

	}
	
}

// scan code for interpretSafe

+ String {
	
	splitCode {
		var word="";
		var array=[];
		var wordStart=false;
		
		// findReplaceAll("// to send\n","// to sendOut\n")
		
		
		this.do{|letter,i|	
			if (wordStart.not) {
				if (letter.isAlpha) {
					wordStart=true;
					word=word++letter;
				};
			}{
				if (letter.isAlphaNum or:{letter==$_} ) {
					word=word++letter;
				}{
					if (word.size>0) { array=array.add(word) } ;
					wordStart=false;
					word="";
				};
			};
		};
		if (word.size>0) { ^array.add(word) } {^array};
	}
	
	interpretSafe{
		
		var array = this.split($\n).collect{|i|
						var comment=i.find("//");
						if (comment.isNil) {i}{i.keep(comment)}
					}.join.splitCode;		
		
		var excluded = #[
		
			// standard sc library
		
			"doUnixCmdAction", "prUnixCmd", "unixCmd", "unixCmdGetStdOut", "systemCmd", 
			"compile", "interpret", "interpretPrint", "exit",
			"UnixFILE", "Pipe", "writeLE",
			"performMsg", "perform", "performList", "functionPerformList", "superPerform",
			"superPerformList", "try", "prTry", "tryPerform", "makeFlopFunc",
			"multiChannelPerform", "performWithEnvir", "doesNotUnderstand",
			"performKeyValuePairs", "applyTo", "runInTerminal",
			"File", "delete", "write", "CSVFileReader", "PathName", "mkdir",
			"openWrite",  "prOpenWrite", "writeData",  "scaleAndWrite",
			
			"Object", "writeArchive",  "addUniqueMethod", "doListOp", 
			"Class", "superclass", "asClass", "allClasses", "class", "classVars",
			"allSubclasses", "nextclass", "superclasses", "methods", "instVarNames",
			"classVarNames", "findMethod", "findRespondingMethodFor", "argNames", "varNames",
			"species", "metaclass", "subclasses", "Method", "ownerClass", "getSlots",
			"writeDefFile", "writeDef", "writeConstants", "executeFile", "compileFile", 
			"Interpreter", "interpretPrintCmdLine", "interpretPrintCmdLine",
			"compileFile", "cmdLine", "preProcessor", "recompile", "ownerClass",
			"this", "thisMethod", "thisProcess", "platform", "Main", "shutdown",
			"ClassBrowser", "current", "currentMethod", "currentClass", "interpretCmdLine",
			
			"SCWindow", "Window", "allWindows", "children",
			"Document", "allDocuments", "open", "listener", "string_", "prSetFileName",
			"CocoaDocument",  "promptToSave_", "propen", "prSetFileName", 
			"implementationClass", "makeWikiPage", "autoRun_", "readNew",
			
			"load", "loadPaths", "loadRelative", "objPerform", "loadPaths", "loadRelative",
			"writeFromFile", "recordNRT", "openOS",
			"makeHelp", "makeAutoHelp", "writeTextArchive",
			"path_", "prSetFileName", "wikiDir_",
			"SaveConsole", "saveAs", "doSave", "doSaveAs",
			"prepareForRecord", "record",
			"Archive", "ZArchive", "writeItem",  "writeClose", "writeZArchive",
			"asArchive", "asBinaryArchive", "writeBinaryArchive",
			"saveCS", "saveStory", "rewrite", "RecNodeProxy", "open",
			"writeAsPlist",
			
			"Server", "ServerOptions", "addr", "quit", "quitAll", 
			"NetAddr", "sendRaw", "sendMsg", "sendBundle", "sendClumpedBundles", "sync",
			"BundleNetAddr", "send", "sendBundle",
			"OSCBundle", "schedSend", "sendAtTime", "prSend",
			"synthDefDir_", "DiskOut",
			
			"ScIDE", "request", "unixCmdGetStdOutLines", "applyTo",
			
			// lnx
			
			"LNX_Studio", "LNX_File", "LNX_Log", "LNX_API", "LNX_Protocols",
			"save", "savePref" , "deletePref", "deleteList", "studios",
			"dependantsPerform", "themeMethods_",
			"LNX_Safe", "lauchSafeMode", "saveScript", "saveInstToLibrary",
			"LNX_Applications", "kill", "killApp", "openApplication", "curl",
			"performAPI", "performAPIMsg", "performAPIList", "performAPIMsgArg1",
			"performAPIClump",
			
			// wslib
			
			"makeDir", "copy", "copyTo", "copyToDir", "moveTo", "moveRename", "removeFile",
			
			
			// extensions
			
			"AtsFile", "saveForSC",
			
		];
		
		array.do{|word| if (excluded.containsString(word)) { ^word } };
		^true
		
	}
	
}

