
// safe mode

LNX_Mode {

	// launch either SC or the standlone app in safe mode (as user nobody)
	// as a result all disk writes will fail, this also applys to any servers lauched in safe mode

	classvar <username, <isSafe;

	*initClass {
		//Class.initClassTree(LNX_File);
		username="user";
		Pipe.do("whoami", {|name| username=name??"user" } );
		isSafe = (username=="nobody");
	}

// sudo -u nobody doesn't work in osx >=10.8
/*
LNX_Mode.launchSafeMode;
Platform.userAppSupportDir;
Platform.systemExtensionDir;
Platform.userExtensionDir;
*/

	*launchSafeMode {|isStandalone=false, quitOnLaunch=false|
		var script = String.scDir +/+ "safeMode.bash";
		var app, result;

		app = String.scDir.dirname.dirname +/+ "Contents/MacOS/SC-StandAlone";

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
/*
(
Class.allClasses.postList;
Class.allClasses.collect{|class| class.methods.collect(_.name)?[]}.flat.asSet.asList.sort[7152..].postList;
q="/Users/neilcosgrove/Desktop/band2.scd".loadList;
q=q ++ ["Class"];
q=q.asSet;
q=q.asList;
q=q.sort;
q.postList;
q.do{|s| "\"".post; s.post; "\", ".post};""
)
*/

	interpretSafe{

		var array = this.split($\n).collect{|i|
						var comment=i.find("//");
						if (comment.isNil) {i}{i.keep(comment)}
					}.join.splitCode;

		var excluded = #[

"Archive", "AtsFile", "AutoCompClassBrowser", "BundleNetAddr", "CSVFileReader", "Class", "ClassBrowser", "CocoaDocument", "DiskOut", "Document", "EZControlSpecEditor", "EZText", "File", "FileReader", "Function", "History", "HistoryGui", "Interpreter", "LNX_API", "LNX_Applications", "LNX_BufferProxy", "LNX_File", "LNX_Log", "LNX_Protocols", "LNX_Safe", "LNX_Studio", "LNX_URLDownloadManager", "Main", "Method", "NetAddr", "OSCBundle", "Object", "PathName", "Pipe", "Player", "ProcMod", "Process", "Quark", "RecNodeProxy", "RoundNumberBox", "SCWindow", "SaveConsole", "ScIDE", "SemiColonFileReader", "Server", "ServerOptions", "SimpleMIDIFile", "String", "TabFileReader", "TaskProxyGui", "UnixFILE", "Window", "ZArchive", "addTimeSignatureString", "addURL", "addUniqueMethod", "addr", "afconvert", "allClasses", "allDocuments", "allSubclasses", "allWindows", "api", "applyTo", "archiveDir_", "argNames", "asArchive", "asBinaryArchive", "asClass", "autoRun", "autoRun_", "buildFromFile", "children", "class", "classVarNames", "classVars", "cmdLine", "compile", "compileFile", "copy", "copyTo", "copyToDir", "createUserSupportDir", "curl", "curlInfo", "curlInfoFreeSound", "current", "currentClass", "currentMethod", "defaultReceiveDrag", "delete", "deleteList", "deletePref", "dependantsDictionary", "dependantsPerform", "die", "doFunctionPerform", "doListOp", "doSave", "doSaveAs", "doUnixCmdAction", "document", "doesNotUnderstand", "download", "executeFile", "exit", "findMethod", "findRespondingMethodFor", "fromString", "functionPerformList", "getSlots", "groupCmdOD", "groupCmdSync", "groupNormalize", "guiAddURL", "hostCmd", "hostCmdClumpedList", "hostCmdGD", "hostCmdOD", "implementationClass", "init", "initClass", "initIncoming", "initURL", "instVarNames", "interpret", "interpretCmdLine", "interpretFunc", "interpretFunc_", "interpretPrint", "interpretPrintCmdLine", "interpretPrintSelectedText", "kill", "killApp", "lauchSafeMode", "listSendBundle", "listSendMsg", "listener", "lnxp", "lnxp_d", "lnxp_gd", "lnxp_l", "lnxp_nd", "lnxp_od", "lnxp_tH", "lnxp_tU", "lnxp_tUND", "load", "loadDirectory", "loadPaths", "loadRelative", "loadStory", "loadSynthDef", "loadTimeLine", "makeAutoHelp", "makeDir", "makeFlopFunc", "makeHelp", "makeViews", "makeWikiPage", "metaclass", "methods", "mkdir", "moveRename", "moveTo", "multiChannelPerform", "nCS", "nI", "netAddURL", "newHex", "newMessage", "newPermanent", "newTemp", "nextclass", "ngcS", "nhcGD", "normalize", "nsdOD", "nstGD", "nstOD", "objPerform", "open", "openApplication", "openOS", "openSystemSupportDir", "openUDPPort", "openURL", "openWrite", "ownerClass", "pP", "parent", "path", "pathName", "path_", "perform", "performAPI", "performAPIClump", "performAPIList", "performAPIMsg", "performAPIMsgArg1", "performInPlace", "performKeyValuePairs", "performList", "performMsg", "performODMsgs", "performOnEach", "performWithEnvir", "platform", "playFromFile", "prAdd", "prOpen", "prOpenUDPPort", "prOpenWrite", "prSend", "prSetFileName", "prTry", "prUnixCmd", "prWriteToFile", "preProcessor", "prepareForRecord", "promptToSave_", "propen", "putLoadList", "putLoadListURL", "quit", "quitAll", "rM", "read", "readArchive", "readFile", "readFromDoc", "readInterpret", "readNew", "readSelectionWithTask", "readWithTask", "realEndNumber", "recieveGD", "recieveOD", "recompile", "record", "recordNRT", "recordNRTThen", "recordPM", "recordingsDir", "recordingsDir_", "recordpath", "recordpath_", "registerMessage", "registerObject", "registerPermanentObject", "removeFile", "request", "restrictedPath", "restrictedPath_", "rewrite", "runInTerminal", "save", "saveAs", "saveCS", "saveFolder", "saveFolder_", "saveForSC", "saveInstToLibrary", "saveLPCFile", "saveList", "savePref", "saveScript", "saveStory", "saveTimeLine", "saveToFile", "saveToFiles", "saveToLPCFile", "saveToProcEvents", "scaleAndWrite", "schedSend", "schedSync", "scroot", "scroot_", "send", "sendAtTime", "sendBundle", "sendClumpedBundles", "sendClumpedList", "sendDelta", "sendDeltaOD", "sendGD", "sendList", "sendMsg", "sendMsgSync", "sendND", "sendOD", "sendODND", "sendRaw", "sendTo", "sendToGD", "sendToND", "sendToOD", "sendVP", "shutdown", "species", "store", "string_", "studio", "studio_", "studios", "subclasses", "superPerform", "superPerformList", "superclass", "superclasses", "superclassesDo", "svnpath", "svnpath_", "sync", "synthDefDir", "synthDefDir_", "systemCmd", "systemSupportDir", "systemSupportDir_", "themeMethods_", "this", "thisMethod", "thisProcess", "timeSignatureAsArray", "timeSignatures", "toCSV", "try", "tryPerform", "unformatTime", "uniqueMethods", "unixCmd", "unixCmdActions", "unixCmdActions_", "unixCmdGetStdOut", "unixCmdGetStdOutLines", "unixCmdInferPID", "unixCmdThen", "url", "url_", "use", "userSupportDir", "userSupportDir_", "varNames", "wikiDir", "wikiDir_", "write", "writeAction", "writeAction_", "writeArchive", "writeAsPlist", "writeBinaryArchive", "writeClientCSS", "writeClose", "writeConstants", "writeData", "writeDef", "writeDefFile", "writeDefFileOld", "writeDefOld", "writeFile", "writeFromFile", "writeInputSpec", "writeInputSpecOld", "writeItem", "writeLE", "writeLog", "writeMetadata", "writeMetadataFile", "writeMsg", "writeOSCFile", "writeOnce", "writeOutputSpec", "writeOutputSpecs", "writeSynthDefFile", "writeTextArchive", "writeZArchive", "Message", "MethodQuote", "makeHead", "ClassInspector", "FunctionDefInspector", "Inspector", "Inspector", "MethodInspector", "MixedBundle", "addMessage", "addOnSendMessage", "ObjectInspector", "SlotInspector", "copyFile"

		];

		array.do{|word| if (excluded.containsString(word)) { ^word } };
		^true

	}

}

