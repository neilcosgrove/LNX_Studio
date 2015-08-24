
// a URL Download Manager  ////////////////////////////////////////////////////////////////////

// manages new, running and failed URL downloads

LNX_URLDownloadManager {
	
	classvar <downloads, <failed, <succeeded, <task, <>verbose=false;	
	// register all new downloads with the download manager
	// there could be another download trying to get the same file

	*new{|url,path,action,replace=false|
		var urlKey = url.asSymbol;
		if (downloads[urlKey].isNil) {
			if (downloads.isEmpty) {this.startChecking}; // for updating info & gui
			downloads[urlKey] = LNX_URLDownload(url, path, action, replace);
			^downloads[urlKey];
		}{
			"This url is already downloading".warn; // this shouldn't happen
			^downloads[urlKey]
		};
	}
	
	// init the download manager
	*initClass {
		downloads = IdentityDictionary[]; // all active downloads
		failed    = IdentityDictionary[]; // all failed downloads
		succeeded = IdentityDictionary[]; // all successful downloads
		ShutDown.add { this.cancelAllDownLoads };
		CmdPeriod.add(this);
		// a single task for the manager to update all downloads & their models
		task=Task({
			inf.do{
				(0.25/6).wait;
				if (downloads.isEmpty) {
					this.stopChecking
				}{			
					downloads.do{|download|			
						if (download.status==\connecting) {
							if (verbose) { 
								// " Connecting.".postln;
							};
						}{
							// don't forget i'm in the manager here!
							this.updateModels(download);
						};
					};
				};
			 };
		},AppClock); // a text feedback task
	}
	
	// update the models in LNX_BufferProxy with the latest download stats
	*updateModels{|download|
		download.pollStats;
		LNX_BufferProxy.containers.do{|buf,i|
			if ((buf.isURL)and:{(buf.url)==(download.url)}) {
				buf.models[\percentageComplete].value_(download.percentageComplete);
				// + anything else i want
			};
		}
	}
	
	// get all downloads
	*all{ ^downloads ++ failed ++ succeeded }
	
	// PRIVATE: download was a success, used by LNX_URLDownload 
	*successfulDownload{|download|
		this.updateModels(download); 				// update the models
		succeeded[download.url.asSymbol] = download;	// add to succeeded
		downloads[download.url.asSymbol] = nil;		// remove from downloads
		if (downloads.isEmpty) {this.stopChecking};	// stop updates if no downloads
		if (verbose) { "success".postln };
	}

	// PRIVATE: download was a failure, used by LNX_URLDownload 
	*failedDownload{|download|
		var urlKey = download.url.asSymbol;
		this.updateModels(download);				// update the models
		downloads[download.url.asSymbol] = nil;		// remove from downloads
		if (downloads.isEmpty) {this.stopChecking};	// stop updates if no downloads
		failed[urlKey] = download;					// add to failed
		if (verbose) { "failed".postln };
	}
	
	// retry all failed downloads
	*retryAllFailedDownloads{
		if (failed.notEmpty) {
			failed.do{|download|
				downloads[download.url.asSymbol] = download;
				download.startDownload;
			};
			failed = IdentityDictionary[];
			this.startChecking;
		};
	}
	
	// PRIVATE: retry a failed download, used by LNX_URLDownload 
	*retryDownload{|download|
		var urlKey = download.url.asSymbol;
		downloads[urlKey] = download;
		failed[urlKey] = nil;
		this.startChecking;
	}
	
	// does the path exist as an active download, if so what is it as a dict. 
	*pathAsDownload{|path| ^(downloads++failed).select{|download| download.path==path} }
	
	// PRIVATE: for updating verbose & GUI
	*cmdPeriod{this.startChecking}
	*startChecking{ task.start }
	*stopChecking { task.stop  }
	
	// cancel all active downloads
	*cancelAllDownLoads{
		downloads.do(_.cancelDownload);
		downloads = IdentityDictionary[];
		this.stopChecking;
		if (verbose) {"Cancelled all downloads".postln};
	}
	
	// delete the cashe
	*deleteCashe{
		if (downloads.isEmpty) {
			LNX_BufferProxy.nonUserContent.do{|item|
				item.removeFile(false,false,true);
			};
			
		}{
			"Can't delete cashe, downloads in progress".warn;
		}	
	}
	
	// the bufferProxy has been freed and the download needs removing, used by LNX_URLDownload:free
	*removeDownload{|download|
		downloads[downloads.findKeyForValue(download)] = nil;
		failed[failed.findKeyForValue(download)] = nil;
		succeeded[succeeded.findKeyForValue(download)] = nil;
	}

}
