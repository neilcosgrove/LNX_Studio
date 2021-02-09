
// footnote to help files

+ SCDocHTMLRenderer{

	*renderFooter {|stream, doc|
		stream << "<div class='doclink'>";
		if (LNX_Studio.isStandalone) {

			stream	<< "LNX_Studio " << LNX_Studio.version << "</div>"
					<< "</div></body></html>";
		}{

			doc.fullPath !? {
				stream << "source: <a href='" << URI.fromLocalPath(doc.fullPath).asString << "'>"
					<< doc.fullPath << "</a><br>"
				};
			stream << "link::" << doc.path << "::<br>"
			<< "sc version: " << Main.version << "</div>"
			<< "</div></body></html>";
		}

	}
}

// intercept the demo song link

+ HelpBrowser {

	// classvar >studio; // ** THIS HAS BEEN ADDED IN CLASS DEF

	goTo {|urlString, brokenAction|
		var url, newPath, oldPath;

		if (urlString.endsWith("/Demo%20Song.html")) {
			LNX_StartUp.studio.loadDemoSong
		}{

			window.front;
			this.startAnim;

			brokenAction = brokenAction ? { |fragment|
				var brokenUrl = URI.fromLocalPath( SCDoc.helpTargetDir++"/BrokenLink.html" );
				brokenUrl.fragment = fragment;
				brokenUrl;
			};

			url = URI(urlString);

			rout = Routine {
				try {
					url = SCDoc.prepareHelpForURL(url) ?? { brokenAction.(urlString) };
					newPath = url.path;
					oldPath = URI(webView.url).path;
					webView.url = url.asString;
					// needed since onLoadFinished is not called if the path did not change:
					if(newPath == oldPath) {webView.onLoadFinished.value};
					webView.focus;
				} {|err|
					webView.html = err.errorString;
					err.throw;
				};
				CmdPeriod.remove(this);
				rout = nil;
			}.play(AppClock);
			CmdPeriod.add(this);
		}
	}
}

// just add UGens to help

+ Help {
	*addToMenu {
		// @TODO: uncomment
		// var ugens, menu;
		// var addSubMenu = { |parent, dict, name, index=8|
		// 	var menu = SCMenuGroup.new(parent, name, index);
		// 	var keys = dict.keys.asArray;
		// 	keys.sort {|a,b| a.asString <= b.asString };
		// 	keys.do{ |key, subindex|
		// 		if(dict[key].class == Dictionary) {
		// 			// Add submenu
		// 			addSubMenu.value(menu, dict[key], key[2..key.size-3], subindex)
		// 		}{
		// 			// Add selectable menu item
		// 			SCMenuItem.new(menu, key.asString, subindex).action_(
		// 				{ key.asString.openHelpFile }
		// 			)
		// 		}
		// 	};
		// 	menu
		// };
		// if (LNX_Studio.isStandalone) {
		// 	addSubMenu.value('Help', Help.tree["[[UGens]]"], "UGens");
		// }{
		// 	addSubMenu.value('Help', Help.tree, "Help Tree");
		// };
	}
}

// suppress NaNs

+ Object {

	filterNaN { ^this }

}

+ SimpleNumber {

	filterNaN { if (this >= 0 or: { this <= 0 }) { ^this } { ^0 } }

	hashScape{|res=1073741788|
		var floor = this.asFloat.floor.hash.wrap(0.0,res)/res;
		var ceil  = this.asFloat.ceil.hash.wrap(0.0,res)/res;
		var frac  = this.frac;
		^(floor*(1-frac)) + (ceil*frac)
	}

	hashScapeSin{|res=1073741788|
		var floor = this.asFloat.floor.hash.wrap(0.0,res)/res;
		var ceil  = this.asFloat.ceil.hash.wrap(0.0,res)/res;
		var frac  = (this.frac - 0.5 * pi).sin + 1 * 0.5;
		^(floor*(1-frac)) + (ceil*frac)
	}

}


+ SequenceableCollection {

	filterNaN { ^this.collect{|i| i.filterNaN } }

	// mean blur
	blur {|kernalSize=3, amount=1, wrap=true|
		var dataSize     = this.size;
		var newData      = FloatArray.fill(dataSize,0);
		var kernal       = FloatArray.fill(kernalSize,0);
		var kernalOffset = kernalSize.div(2);
		amount = amount.clip(0,1);
		if (wrap.isTrue) {
			kernalSize.do{|i| kernal[i]=this.wrapAt(i-kernalOffset) };
			(dataSize).do{|i|
				newData[i] = (kernal.mean * amount) + (this.at(i)*(1-amount) );
				kernal.wrapPut(i, this.wrapAt(i+kernalSize-kernalOffset));
			};
		}{
			kernalSize.do{|i| kernal[i]=this.clipAt(i-kernalOffset) };
			(dataSize).do{|i|
				newData[i] = (kernal.mean * amount) + (this.at(i)*(1-amount) );
				kernal.wrapPut(i, this.clipAt(i+kernalSize-kernalOffset));
			};
		};
		^newData;
	}

}

// filter those NaNs out of ControlSpec to stop certain errors

+ ControlSpec {

	map { arg value;
		// maps a value from [0..1] to spec range
		^warp.map(value.clip(0.0, 1.0)).round(step).filterNaN;
	}

	mapNoRound{ arg value;
		// maps a value from [0..1] to spec range
		^warp.map(value.clip(0.0, 1.0)).filterNaN;
	}

	unmap { arg value;
		// maps a value from spec range to [0..1]
		^warp.unmap(value.round(step).clip(clipLo, clipHi)).filterNaN;
	}

	unmapNoClip { arg value;
		// maps a value from spec range to [0..1]
		^warp.unmap(value.round(step)).filterNaN;
	}

	constrain { arg value;
		^value.filterNaN.asFloat.clip(clipLo, clipHi).round(step)
	}

	constrainLow { arg value;
		value=value.filterNaN.asFloat.round(step);
		if ((value<clipLo)or:{value>clipHi}) {^clipLo}{^value};
	}

	constrainHigh { arg value;
		value=value.filterNaN.asFloat.round(step);
		if ((value<clipLo)or:{value>clipHi}) {^clipHi}{^value};
	}

}

// fix for 1 ! inf;

+ Object {
	dup { arg n = 2;
		if (n==inf) {
			"Infinate duplicate".reportError;
			n=1;
		};
		^Array.fill(n, { this.copy })
	}
}

+ Buffer {

	play { arg loop = false, mul = 1;
		^{arg gate=1;
			var player;
			player = PlayBuf.ar(numChannels, bufnum, BufRateScale.kr(bufnum),
				loop: loop.binaryValue) * EnvGen.ar(Env.asr, gate, doneAction:2);
			if(loop.not, FreeSelfWhenDone.kr(player));
			player * mul;
		}.play(server)
	}

}

+ Server {

	// for security

	// *default_ { |server|
	// 	default = server; // sync with s?
	// 	if (LNX_Studio.isStandalone.not) {
	// 		if (sync_s, { thisProcess.interpreter.s = server });
	// 	};
	// 	this.all.do(_.changed(\default));
	// }

	// to use LNX_Audio as a name, I may not do this anymore. think about it

	// *initClass {
	// 	Class.initClassTree(ServerOptions);
	// 	Class.initClassTree(NotificationCenter);
	// 	named = IdentityDictionary.new;
	// 	set = Set.new;
	// 	default = local = Server.new(\localhost, NetAddr("127.0.0.1", 57110));
	// 	Platform.switch(\windows, {
	// 		program = "LNX_Audio.exe";
	// 	}, {
	// 		internal = Server.new(\internal, NetAddr.new);
	// 		program = "cd % && exec ./LNX_Audio".format(String.scDir.quote);
	// 	});
	// }

	prepareForRecord { arg path;
		if (path.isNil) {
			if(File.exists(thisProcess.platform.recordingsDir).not) {
				systemCmd("mkdir" + thisProcess.platform.recordingsDir.quote);
			};

			// temporary kludge to fix Date's brokenness on windows
			if(thisProcess.platform.name == \windows) {
				path = thisProcess.platform.recordingsDir +/+ "LNX_" ++ Main.elapsedTime.round(0.01) ++ "." ++ recHeaderFormat;

			} {
				path = thisProcess.platform.recordingsDir
				+/+
				(LNX_Studio.is_in_a_khole.if("[k]hole ","LNX "))
				++ (Date.getDate.format("%Y-%d-%e %R:%S").replace(":",".").drop(2)) ++ "." ++ recHeaderFormat;
			};
		};
		recordBuf = Buffer.alloc(this, 65536, recChannels,
			{arg buf; buf.writeMsg(path, recHeaderFormat, recSampleFormat, 0, 0, true);},
			this.options.numBuffers + 1); // prevent buffer conflicts by using reserved bufnum
		SynthDef("server-record", { arg bufnum;
			DiskOut.ar(bufnum, In.ar(0, recChannels))
		}).send(this);
		// cmdPeriod support
		CmdPeriod.add(this);
		^path;
	}

}


