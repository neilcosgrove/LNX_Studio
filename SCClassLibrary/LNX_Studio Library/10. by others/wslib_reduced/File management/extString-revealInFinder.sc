/*
thisProcess.platform.recordingsDir.revealInFinder;
*/

+ String {

	revealInFinder {
		var path, string;	
		var lines, cmd;
		path = this.standardizePath;
		if( path[0] != $/ ) { path = String.scDir +/+ path };
		string="tell application \"Finder\"
			activate
			reveal POSIX file %
		end tell".format( path.quote );
		lines = string.split( $\n );
		cmd = "osascript";
		lines.do({ |line|
			cmd = cmd + "-e" + line.asCompileString;
			});
		cmd.unixCmd;
		}

	
	}