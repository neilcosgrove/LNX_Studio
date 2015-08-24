
/* LNX_BufferProxy ***********************************

afconvert to covert  
Dialog.openPanel{|p| p.afconvert({}, "WAVE", "LEI16") };

(
LNX_URLDownloadManager.deleteCashe;
b=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/bass%20606.aif"); // yes
b=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/bass 606.aif"); // yes
b=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/bass 606.aif bad name"); // no
b=LNX_BufferProxy.url(s,"http://sep800.mine.nu/files/sounds/kickdoor.wav");
b=LNX_BufferProxy.url(s,"http://www.freesfx.co.uk/rx2/mp3s/9/10515_1374578572.mp3"); // not a sound
b=LNX_BufferProxy.url(s,"http://lnxstudio.sourceforge.net/bass%20606.aif"); // yes
b=LNX_BufferProxy.url(s,"http://lnxstudio.sourceforge.net/bass 606.aif"); // yes
b=LNX_BufferProxy.url(s,"http://lnxstudio.sourceforge.net/bass 606.aif bad name"); // no
c=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/fifteen_quiet_years_lp.zip");

b.cancelDownload;

//("kill" + (b.pid+1)).systemCmd;download

//path = "file://"++path;

b=LNX_BufferProxy.url(s,"file://" +/+ "mac/bass606.aif bad name",{}); // no
b=LNX_BufferProxy.url(s,"file://" +/+ "mac/bass606.aif",{}); // from file
b=LNX_BufferProxy.url(s,"file://" +/+ "mac/bass 606.aif",{}); // from file

b.status

***** Using file dialog

b = Dialog.openPanel{|path| b=LNX_BufferProxy.url(s,("file://" +/+ "mac" ++ path))};
b.download.info.postList
b.dump
b.play
)

******************

c=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/fifteen_quiet_years_lp.zip");
c.status;
b.cancelDownload;
c.status;
c.retryDownload;
c.status;

LNX_URLDownloadManager.retryAllFailedDownloads;
LNX_URLDownloadManager.cancelAllDownLoads;
LNX_URLDownloadManager.deleteCashe;

*/

/* *******************************************************************

LNX_URLDownloadManager.deleteCashe;

b=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/Firefox 20080422 2137.aiff");
b.cancelDownload;
b.status;
b.retryDownload;

b.buffer.play;

b.info.postList;
b.download.dump;

b.retry;

b.free; // needs sorting as well


TEST for double loading after deleteCashe i.e

b=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/Firefox 20080422 2137.aiff");
LNX_URLDownloadManager.deleteCashe;
b=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/Firefox 20080422 2137.aiff");


LNX_URLDownloadManager.cancelAllDownLoads;

LNX_URLDownloadManager.deleteCashe;
b=2.collect{ LNX_BufferProxy.url(s,"file://" +/+ "mac/bass606.aif",{});}; // from file

b[0].play;
b[0].free;
b[0].play;


b[1].free;
b[1].play;
b[0].play;

b[0].status
b[1].status

** info from Manager
LNX_URLDownloadManager.all.postList;
LNX_URLDownloadManager.downloads.postList;
LNX_URLDownloadManager.failed.postList;
LNX_URLDownloadManager.succeeded.postList;

LNX_BufferProxy.containers.collect(_.path).collect(_.basename).postList

LNX_URLDownloadManager.deleteCashe;
b=2.collect{ LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/Firefox 20080422 2137.aiff") }; // from file

b[0].free;
b[1].play;
b[1].free;
b[1].play;

***** Using file dialog for MP3 converting TESTS ******

LNX_URLDownloadManager.deleteCashe;

Dialog.openPanel{|path| a=LNX_BufferProxy.url(s,("file://" +/+ "mac" ++ path))};
a.play;
Dialog.openPanel{|path| b=LNX_BufferProxy.url(s,("file://" +/+ "mac" ++ path))};
b.play;
Dialog.openPanel{|path| c=LNX_BufferProxy.url(s,("file://" +/+ "mac" ++ path))};
c.play;

d=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/music/07%20Birdsong%20Diminish.mp3");

e=LNX_BufferProxy.url(s,"https://dl.dropboxusercontent.com/u/4751569/06%20Cliquot.m4a");
e.play;

d.play;

s.reboot; // restart server

a.play;
b.play;
c.play;

LNX_URLDownloadManager.deleteCashe;
s.reboot; // restart server

a.play; // yes
b.play; // no ??
c.play; // no ??

b.download.forceRedownload; // ??

LNX_BufferProxy.containers.select{|c| c.source==\url};

)

*/

/*
// play all downloads
LNX_BufferProxy.containers.select{|b| b.download.notNil}.select{|b| b.download.isComplete}.do(_.play)
*/

//////// MAKE EVERYTHING SAFE IN LNX_MODE !!!!!!!!!!!

// a URL Download  ////////////////////////////////////////////////////////////////////

// an actual URL download

LNX_URLDownload {
	
	classvar <>verbose=false, <>format="AIFF", <>data="BEI16";
	
	// when converting audiofiles use...
	// format : data (as below)
	// ========================
	// AIFF : I8 BEI16 BEI24 BEI32 
	// WAVE : UI8 LEI16 LEI24 LEI32 LEF32 LEF64
	
	var <status, <url, <path, <action, <replace, <pid, <dir, <name, <pSymbolKill;
	var <covertedPath;
	var <lastTime, <lastSize, <size, <time, <rate, <info, <initTime, <failReason;
	var <rateString, <sizeString, <percentageComplete= -1;
	
	*new{|url,path,action,replace=false| ^super.new.init(url,path,action,replace) }
	
	// init a new download
	init{|argURL, argPath, argAction, argReplace|
		status   = \connecting;
		url      = argURL;
		path     = argPath;
		action   = argAction;
		replace  = argReplace;
		dir      = path.dirname;
		name     = path.basename;
		initTime = SystemClock.now;
		
		this.startDownload;
	}
	
	// start the download by getting the curl -I
	startDownload{
		if (this.isRunning.not) {		
			if (dir.pathExists(false).not) { dir.makeDir }; // make the dir
			if ((path.pathExists(false).not)||replace) {
				var selector;
				status = \connecting;
				// for killing the curl
				pSymbolKill = ("curl " ++ url.replace(" ", "%20") ++ " -I").asSymbol; // not fs!
				selector = (url[..23]=="http://www.freesound.org").if(
												\curlInfoFreeSound,\curlInfo);
				url.perform(selector,path,{|res,pid,argInfo,found|
					// if curl finished with result = 0
					if (res==0) {
						info=argInfo;
						// was the file found?
						if (found) {
							// is it audio ?
							if ((info['content-type'].isNil) or:
								{info['content-type'].contains("audio")})
									{
										this.downloadFile;
									}{
										this.failed("Not an audio file");
										info.postList;
									};
						}{
							this.failed("File not found"); // not found 404 error
						}
					}{
						this.failed("Connection error"); // curl didn't finish
					};				
				});  
				if (verbose) { 
					{
						if (this.isRunning) {("PID:"+pid+"- Starting download.").post;};
					}.defer(0.125); // just for post
				};
			}{
				// the download failed (this should never happen but just incase)
				"Failed to start, another download doing it now.".warn;
				status  = \failed;
				LNX_URLDownloadManager.failedDownload(this);
			};
		}{
			"The CURL for this download is already running.".warn;
			// should i do thisÉ?
			// status  = \failed;
			// LNX_URLDownloadManager.failedDownload(this);
		}
	}
	
	// the download failed
	failed{|reason|
		failReason = reason;						
		status     = \failed;
		//info       = nil;
		this.removeFile;
		LNX_URLDownloadManager.failedDownload(this);
		if (verbose) {"Failed ( ".post; reason.post; " ) :".post; path.postln };
	}
	
	// the download was successful
	succeeded{
		status  = \finished;
		this.finishedStats;
		LNX_URLDownloadManager.successfulDownload(this);
		action.value(this);
		action = nil; // this should only happen once & stops preview playback when reloading
	}
	
	// now download the file
	downloadFile{
		if (status == \connecting) { // just a check
			status = \downloading;
			this.resetStats;	
			// for killing the curl
			pSymbolKill = ("curl " ++ url.freeSound2URL.replace(" ", "%20")).asSymbol;  
			pid = url.curl(path,{|res,pid|
				if (res==0) {
					if (this.isSoundFile) {
						this.succeeded; 			// the download was successful
					}{					
						this.convert; 			// try coverting it
					};
				}{
					this.failed("Download failed"); 	// curl failed
				};				
			});
		};
	}
	
	// convert the downloaded file
	convert{
		//  transcode file with afconvert
		status  = \converting;
		path.afconvert({|res,pid|
			if (res==0) {
				covertedPath = path.getNewPath(format); // is this needed here ?
				if (verbose) {"Converted to: ".post; covertedPath.postln };
				this.succeeded; // the transcoding was successful
				
			}{
				// Not an audio file or not reckonised format
				this.failed("Not a recognised audio file");
			}
		}, format, data);
	}
	
	// is the download still running?
	isRunning{ ^((pid.notNil)and:{pid.pidRunning}) }
	
	// cancel the download
	cancelDownload{
		var pidKill;
		// is the download running?
		if (this.isRunning) { 
			// get the curl pid, this is different than pid which links to sh
			pidKill = LNX_Applications.allBySymbol[pSymbolKill];
			if (pidKill.isNumber) {
				("kill" + pidKill).systemCmd; // stop it
				if (verbose) { "Canceled Download".postln };
			};					
			status = \failed;
			if (this.isSoundFile.not) { this.removeFile }; // delete file
			
			// should i do? this.failed("Reason");
		};
	}
	
	// is the downloaded file a soundfile?
	isSoundFile{ ^path.isSoundFile }
	
	// has the download been successful?
	isComplete{ ^status==\finished }
	
	// delete the downloaded file
	removeFile{
		if (path.pathExists(false)) {
			path.removeFile(false,false,true);
			if (verbose) { "Deleted: ".post; path.postln };
			this.resetStats;
		};
	}
	
	// retry after a fail
	retryDownload{
		if ((this.isComplete.not)and:{this.isRunning.not}){
			this.startDownload;
			LNX_URLDownloadManager.retryDownload(this);
		};
	}
	
	// as above but forces test for isComplete to false, used by recursiveLoad
	forceRedownload{
		status=nil;
		this.retryDownload;
	}
	
	// reset vars for download stats
	resetStats{
		lastTime = AppClock.now;
		lastSize = 0;
		size     = 0;
		time     = lastTime;
		rate     = 0;
		percentageComplete = 0;
		failReason = nil;
	}
	
	// gets the file size
	fileSize{ if (path.pathExists(false)) { ^File.fileSize(path) }{ ^0 } }
	
	// get the file size as a friendly string
	fileSizeString{ ^this.fileSize.bytesAsString } // actual value not polled
	
	// poll the time and size so we can workout the download stats
	pollStats{
		lastTime   = time;
		time       = AppClock.now;
		lastSize   = size;
		size       = this.fileSize;
		sizeString = size.bytesAsString;
		rate       = (size-lastSize)/(time-lastTime);
		rateString = rate.bytesAsString++"/s";	
		
		if (status==\connecting) { percentageComplete = -1; ^this };
		if (status==\converting) { percentageComplete = -2; ^this };
		if (status==\finished  ) { percentageComplete = -3; ^this };
		if (status==\failed    ) { percentageComplete = -4; ^this };
		
		if (info.notNil && {info['content-length'].notNil or:{  info['filesize'].notNil    }  }) {			percentageComplete = size / (info['content-length']?info['filesize'])*100;
		}{
			percentageComplete = 0;
		};	
	}
	
	// final stats on a completed download
	finishedStats{
		size       = this.fileSize;
		sizeString = size.bytesAsString;
		rate       = 0;
		rateString = "";
		percentageComplete = -3;	
	}
	
	// free this bufferProxy, no testing for others using it. Use BufferPorxy::free instead
	free{
		this.cancelDownload;
		LNX_URLDownloadManager.removeDownload(this);
		status=\freed;
		if (verbose) { "Freed: ".post; path.postln; }
	}
	
}

/////////////////////////////////////////////////////////////////////////////////

LNX_FreeSoundAPIKey{

	classvar lnxKey, <keyModel;

	*initClass{
		lnxKey = "fcf35c2b3ac4423c8b57dcec9603662a";
		keyModel = (("FreeSound API Key".loadPref ?? {[""]})[0]).asModel;
		keyModel.actions_(\stringAction,{|me|
			[keyModel.string].savePref("FreeSound API Key");
		});	
	}
	
	*key{	
		if (keyModel.string.replace(" ","").size>0) { ^keyModel.string.replace(" ","")} {^lnxKey }
	}
	
}

+ SimpleNumber {

	bytesAsString{
		case
			{this>=1.024e6} { ^(this/1.024e6).asFormatedString(1,1)+"MB" }
			{this>=1.024e5} { ^(this/1.024e3).asFormatedString(1,0)+"KB" }
			{this>=1.024e4} { ^(this/1.024e3).asFormatedString(1,0)+"KB" }
			{this>=1.024e3} { ^(this/1.024e3).asFormatedString(1,0)+"KB" }
			{this>=1.024e2} { ^      this.asFormatedString(1,0)+"bytes"  }
			{this>=1.024e1} { ^      this.asFormatedString(1,0)+"bytes"  }
			{this==1}       { ^      "1 byte"}
			{true}          { ^      this.asFormatedString(1,0)+"bytes"  }
	}
	
}

+ Nil { toLower{""} } // fix for below for no extension (faster)

+ String {
	
	// convert audiofile
	// format : data
	// ==============
	// AIFF : I8 BEI16 BEI24 BEI32 
	// WAVE : UI8 LEI16 LEI24 LEI32 LEF32 LEF64
	
	isLNXSoundFile{
		^#[ 'wav', 'aif', 'aiff','mp3', 'm4a',
			'3gpp', '3gp', '3gp2', '3g2', 'aac', 'adts', 'ac-3', 'ac3', 'aifc', 
			'amrf', 'amr', 'm4af',  'm4r', 'm4bf', 'm4b', 'caff', 'caf',
			'mpg1', 'mp1', 'mpeg', 'mpa', 'mpg2', 'mp2', 'mpg3', 'mp4f', 'mp4',
			'next', 'snd', 'au', 'sd2f', 'sd2',  'wave', 'ircam', 'sun', 'mat4',
			'mat5', 'paf', 'svx', 'nist', 'voc', 'w64', 'pvf', 'xi', 'htk', 'sds',
			'avr', 'flac'].includes(this.extension.toLower.asSymbol)
	}
	
	afconvert{|action, format="AIFF", data="BEI16", outputPath|
		^("afconvert"
			++ " -f "
			++ format
			++ " -d "
			++ data
			++ " \""
			++ this 
			++ "\" \""
			++ (outputPath ?? { this.getNewPath(format) })
			++ "\""
		).unixCmd(action); 
	}
	
	// get the new path with the new extenstion
	getNewPath{|format="AIFF"|
		var extension = format.toLower;
		if (extension=="wave") { extension="wav" };
		^this.removeExtension ++ "." ++ extension;
	}
		
	// returns pid
	curl{|path,action|
		^("curl \""
			++ (this.freeSound2URL.replace(" ", "%20"))
			++ "\" > \""
			++ (path.replace("%20", " "))
			++ "\""
		).unixCmd(action); 
	}

	// get info from the header as an IdentityDictionary (used curl -I)
	curlInfo{|path,action| 
		^("curl \""
			++ (this.replace(" ", "%20"))
			++ "\" > \""
			++ (path.replace("%20", " "))
			++ "\" -I"
		).unixCmd({|res,pid|
			var info = IdentityDictionary[];
			var list = path.loadList;
			var found = true;
			
			list	.collect{|s| s.split($:)}
				.select {|s| s.size>=2}
				.do     {|l|
					var value = l[1..].join($:);
					// this should always be true, but not tested
					if (value[0]==($ )) { value = value[1..] };
					// convert to int or symbol
					if (value.asInt.asString==value) {
						value = value.asInt
					}{ 
						if (value.asFloat.asString==value) { value = value.asFloat};
					}; 
					// add it to the IdentityDictionary
					info[l[0].toLower.asSymbol] = value;
				};
			
			// test for not found
			list	.collect{|s| s.split($:)}
				.select {|s| s.size==1}
				.do{|item|
				item=item[0];
					if (item.contains("HTTP")) {
						if (item.contains("404")) {
							found = false; // 404 error
						};
					};	
				};
			
			if (info.isEmpty) { found = false };
			
			action.value(res,pid,info,found);
			
		}); 
	}
	
	// FreeSound Support ////////////////////////////////////////////////////////////////////
	
	// make url friendly to freesound api, used in curl
	freeSound2URL{
		var string=this;
		if (string[..23]=="http://www.freesound.org"){
			string="http://www.freesound.org/api/sounds"+/+
					(string.split[6])+/+"serve/?api_key="++(LNX_FreeSoundAPIKey.key)
		};
		^string;
	}
	
	// make url friendly to freesound api, used in curlInfoFreeSound
	freeSound2URLI{
		var string=this;
		if (string[..23]=="http://www.freesound.org"){
			string="http://www.freesound.org/api/sounds"+/+
				(string.split[6])+/+"?api_key="++(LNX_FreeSoundAPIKey.key)
		};
		^string;
	}
	
	// get parseYAMLFile but with keys as symbols and numbers as numbers!!
	parseYAMLFileBySymbol{
		var info = ();		
		this.parseYAMLFile.pairsDo{|key,value|
			if (value.isString) {
				if (value.asInt.asString==value) {
					value = value.asInt
				}{ 
					if (value.asFloat.asString==value) { value = value.asFloat};
				};
			};
			info[key.toLower.asSymbol]=value;
		};
		^info;
	}
	
	// get info from FreeSound as an IdentityDictionary
	curlInfoFreeSound{|path,action| 
		^("curl \""
			++ (this.freeSound2URLI.replace(" ", "%20"))
			++ "\" > \""
			++ (path.replace("%20", " "))
			++ "\""
		).unixCmd({|res,pid|
			var info, found = true;
			if (res==0) {
				info = path.parseYAMLFileBySymbol;	
				if ((info[\error].notNil) and:{info[\error].isTrue}) {
					action.value(res,pid,info,false);
				}{
					action.value(res,pid,info,true);
				};	
			}{
				action.value(res,pid,(),false);
			};
		}); 
	}

}

// end
