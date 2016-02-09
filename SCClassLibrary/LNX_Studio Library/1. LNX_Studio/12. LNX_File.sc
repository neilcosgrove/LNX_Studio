
// quick and easy flat lists to files  //////////////////////////////////

LNX_File{

	classvar <prefDir;

	*initClass {	
		Class.initClassTree(LNX_Mode);
		if (LNX_Mode.isSafe.not) {
			// get from studio
			prefDir=("~/Library/Preferences/LNX_Studio_"++
				(LNX_Studio.versionMajor)++"."++(LNX_Studio.versionMinor)++"/").absolutePath;
		}{
			prefDir=String.scDir++"/preferences/".absolutePath;
		};
		if (prefDir.pathExists(false).not) { prefDir.makeDir };
	}

	*savePref{|path,list| this.save(prefDir++path,list)}

	*save{|path,list|
		var file;
		{
			file = File(path,"w");
				list.do({|line|
					file.write(line.asString++"\n");
				});
			file.close;
		}.defer(0.01); // for safe mode start-up
	}

	*load{|path|
		var file,list,line;
		if (File.exists(path).not) {^nil};
		file = File(path,"r");
		line = file.getLine;
		list=[];
		while ( {line.notNil}, { 
			list=list.add(line); 
			line = file.getLine;
		});
		file.close;
		^list
	}
	
	*loadPref{|path| ^this.load(prefDir++path)}
	
	*delete{|path| path.removeFile(silent:true) }
	
	*deletePref{|path| this.delete(prefDir++path)}
	
}

// debug logger ///////////////////////

LNX_Log{

	classvar <fileName, log, <>start, <>no, <>verbose=true, <>on=false;
	
	*initClass{
		start=Date.getDate.asString;
		fileName=("LNX_Log"+start).replace(":",".");
		log=["Log start:"+(start.asString),"------------------------------------"];
		no=0;
		if (on) {
			fileName.postln;
			this.writeLog;
		};
	}
	
	*log{|object...args|
		var string,text,tSize;
		if (on) {
			log=log.add(no+": "++Date.getDate.hourStamp,"");
			log=log.add(object.asString);
			log=log++args;
			log=log.add("");
			if (verbose) {
				"Logging ".post;
				args.post;
				" : ".post;
				object.postln;
			};
			if (object.isSequenceableCollection) {
				object.do{|i,j|
					log=log.add(
						(j.asString++"         ")[0..7]
						++
						(i.species.asString+"           ")[0..10]
						++
						(i.asString)
					);
				};
			}{
				text="";
				object.getSlots.do({|a,i|
					string=a.asString;
					text=text++string;
					tSize=23-(string.size.clip(0,22));
					if (i.odd) {text=text++"\n"} {tSize.do({text=text++" "})}
				});
				log=log.add(text);
			};
			log=log++["------------------------"];
			this.writeLog
		}
	}
	
	*mark{|object...args|
		var string,text,tSize;
		if (on) {
			log=log.add(no+": "++Date.getDate.hourStamp,"");
			log=log.add(object.asString);
			log=log++args;
			log=log.add("");
			if (verbose) {
				"Marking ".post;
				args.post;
				" : ".post;
				object.postln;
			};
			log=log++["------------------------"];
			this.writeLog
		}
	}
	
	*writeLog{
		if (on) {
			log.saveList(String.scDir++"/logs/"++fileName);
			no=no+1;
		};
	}

}

// convience methods

+String{
	loadList{^LNX_File.load(this)}
	loadPref{^LNX_File.loadPref(this)}
	
	deleteList{^LNX_File.delete(this)}
	deletePref{^LNX_File.deletePref(this)}
}

+Symbol{
	loadList{^LNX_File.load(this.asString)}
	loadPref{^LNX_File.loadPref(this.asString)}
}

+Collection {
	savePref{|path| LNX_File.savePref(path,this) }
	saveList{|path| LNX_File.save(path,this) }
}

+ Object {
	logIt{|...args| LNX_Log.log(this,*args)}
	*logIt{|...args| LNX_Log.log(this,*args)}
	markIt{|...args| LNX_Log.mark(this,*args)}
	*markIt{|...args| LNX_Log.mark(this,*args)}
}

