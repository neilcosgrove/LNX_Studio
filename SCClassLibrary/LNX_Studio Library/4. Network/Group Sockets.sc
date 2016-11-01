
// socket for Internal or LAN connections //////////////////////////////////////////////////////////
//
// this get restarted everytime we move room sp keep lanAddrs, otherLanAddrs & myAddrs as classvars.
//
// Infuture i can do this...
// 		responders.add(id -> OSCdef(id, function, id));
// instead of...
//		responders.add((id -> OSCresponderNode(nil, id, function).add));

LNX_LANGroup {

	classvar lanAddrs, otherLanAddrs, <myAddrs, port, <>uid, otherAddrs, <previousAddrs;

	classvar <>verbose=false;

	var serveraddress, username, password, groupname, grouppassword, serverport, localtoremoteport,
		localtxport, localrxport, <responders, <pid, <netAddr;

	var <myNetAddr, masterResponder, <isInternal=true, <>reportFunc, task, <ipModel;



	*new {arg serveraddress, username, password, groupname, grouppassword, serverport = 22242,
			localtoremoteport = 22243, localtxport = 22244, localrxport;
		^super.newCopyArgs(serveraddress, username, password, groupname, grouppassword, serverport,
			localtoremoteport, localtxport, localrxport).init;
	}

	*initClass{ this.loadPreviousAddrsFromPrefs }

	init{
		this.initMasterResponder;

		ipModel=0.asModel.action_{|me,val|
			this.scan;
			this.report( "Connected to LAN on:  "++(myNetAddr.simpleString));

		};
		responders = IdentityDictionary[];
		this.addResp('scan' ,      {|time, resp, msg| this.recieveScan(*msg) });
		this.addResp('returnScan' ,{|time, resp, msg| this.returnScan (*msg) });
		this.scan;
	}

	join{ if (verbose) {("LNX_LANGroup started.").postln} }

	// remove the master responder
	removeMasterResponder{
		masterResponder.remove;
		masterResponder = nil;
	}

	// search with an ip number, a hostname or blank for general search
	search{|string|
		var addr = {NetAddr(string, port)}.try;
		if(string.size>0) {
			if (addr.isNil) {
				this.report("Not a valid IP address. Searching...");
				this.scan;
			}{
				this.report("Searching "++(addr.ip)++"");
				this.scan(addr)
			};
		}{
			this.report("Searching...");
			this.scan;
		};
	}

	// reports to the room dialog
	report{|string| {reportFunc.value(string)}.defer(0.1) }

	// check for connection
	taskCheckingForConnection{
		if (task.isNil) {
			task=Task{
				inf.do{|i|
					if ( Pipe.findValuesForKey("ifconfig", "inet").size>1) {
						task.stop;
						task=nil;
						this.scan;
					};
					(i+1).wait;
				};
			}.start;
		}
	}

	// scan for other computers (can use a specific address)
	scan{|userIP|
		var intAddr, startPort, ip, minDigit;
		port = NetAddr.localAddr.port; // lang port
		// get all my ip's
		myAddrs = Pipe.findValuesForKey("ifconfig", "inet")
			.reverse
			.collect{|ip| NetAddr(ip.replace("addr:", "").replace("adr:", ""),port) };
			//.reject {|addr| addr.digit(0)==127 or: { addr.digit(4)==0 }} // remove 127. so not selected by mistake
		// is local if size>0
		if (myAddrs.size>0) {
			// is local
			ipModel.themeMethods_((items_:  myAddrs.collect(_.ip)));
			myNetAddr = myAddrs[ipModel.value]; // is this a better option than the last ip
			// transition
			if (isInternal) {
				// CHANGED was INTERNAL before
				lanAddrs  = Set[myNetAddr]; // reset search
				this.report( "Connected to LAN on:  "++(myNetAddr.simpleString));
				if (task.notNil) {	task.stop; task=nil; };
			}{
				// NO change was LOCAL before
				if (lanAddrs.isNil) {
					lanAddrs  = Set[myNetAddr];
					this.report("Connected to LAN on:  "++(myNetAddr.simpleString));
					if (task.notNil) {	task.stop; task=nil; };
				} {
					lanAddrs = lanAddrs.union(Set[myNetAddr]);
				};
			};
			isInternal = false;
		}{
			// is internal
			myNetAddr = NetAddr.localAddr;
			// transition
			if (isInternal) {
				// NO change was INTERNAL before
				if (lanAddrs.isNil) {
					lanAddrs  = Set[myNetAddr];
					this.report( "Not connected..." );
					this.taskCheckingForConnection;
				} {
					lanAddrs = lanAddrs.union(Set[myNetAddr]);
				};
			}{
				// CHANGED was LOCAL before
				lanAddrs  = Set[myNetAddr]; // reset search
				this.report( "Not connected..." );
				this.taskCheckingForConnection;
			};
			isInternal = true;
		};
		// get the integer address to use in search
		if (userIP.notNil) { intAddr = userIP.addr }{ intAddr = myNetAddr.addr };
		// fan out a search of addrs(-50,+50) & ports (-1,+1)
		{
			// we have found users here in the past
			previousAddrs.do{|addr|
				{
					addr.sendBundle(nil, [\m,uid]++[\scan]++
						(lanAddrs.collect{|addr| [addr.addr,addr.port]}.asList.flat)
					);
				}.try;
				0.1.wait;
			};

			minDigit = NetAddr.fromIP(intAddr,port).digit(0); // lowest 1st digit to scan
			// now scan 50 * 3 addresses
			50.do{|a|
				a= (a+1).div(2)*(a.odd.if(1,-1)); // [0,1,-1,2,-2..]
				3.do{|p|
					var tryAddr;
					p= (p+1).div(2)*(p.odd.if(1,-1));
					tryAddr = NetAddr.fromIP(intAddr+a,port+p);
					// do i also want to check for intAddr equal
					// and warning 126. is this ok ?  myNetAddr.array[0]==tryAddr.array[0]
					if (tryAddr!=myNetAddr and: {tryAddr.digit(3) > 0} and: {tryAddr.digit(0) >= minDigit}
                        and:{tryAddr.hostname!="192.168.1.255"}) // my router denies access
                    {
						{
							tryAddr.sendBundle(nil, [\m,uid]++[\scan]++
								(lanAddrs.collect{|addr| [addr.addr,addr.port]}.asList.flat)
							);
						}.try;
					};
					0.1.wait;
				};
			};
		}.fork;
	}

	// add as union of lanAddrs & set
	addUnion{|set|
		var diff;
		set=set.reject {|addr| addr==NetAddr.localAddr };
		// i want to test this futher
		// maybe include all my previous addresses
		diff = set.difference(lanAddrs);
		lanAddrs=lanAddrs.union(set);
		if (diff.size>0) {
			diff.do{|addr|
				this.report("Found user at:  "++(addr.simpleString));
				previousAddrs = (previousAddrs.add(addr)).asSet;
				this.savePreviousAddrsToPrefs;
			};
		};

		otherAddrs = lanAddrs.difference(myAddrs); // now make a list of all other addrs
	}

	savePreviousAddrsToPrefs{
		previousAddrs.collect{|addr| [addr.hostname, addr.port]}.asList.flatNoString.savePref("prev_Addrs")
	}

	*loadPreviousAddrsFromPrefs{
		previousAddrs=("prev_Addrs".loadPref ? []).clump(2).collect{|l| NetAddr(l[0],l[1].asInt)}.asSet
	}

	*clearPreviousAddrs{
		previousAddrs = Set[];
		previousAddrs.savePref("prev_Addrs")
	}

	// recieved a scan and respond
	recieveScan{|symbol...addrs|
		addrs = addrs.clump(2).collect{|list| NetAddr.fromIP(list[0], list[1]) }.asSet;
		this.addUnion(addrs);
		this.sendBundle(nil,
			[\returnScan]++(lanAddrs.collect{|addr| [addr.addr,addr.port]}.asList.flat)
		);
		// this didn't work to make send more effient. why??
		// otherLanAddrs=lanAddrs.copy.remove(myNetAddr);
	}

	// a returned scan
	returnScan{|symbol...addrs|
		addrs = addrs.clump(2).collect{|list| NetAddr.fromIP(list[0], list[1]) }.asSet;
		this.addUnion(addrs);
		// this didn't work to make send more effient. why??
		// otherLanAddrs=lanAddrs.copy.remove(myNetAddr);
	}

	// close this connection
	close {
		this.removeMasterResponder;
		responders = IdentityDictionary[];
	}

	// add a responder
	addResp{|id,function| responders[id] = function }

	// remove a responder
	removeResp {|id| responders[id] = nil }

	// all messages come via the masterResponder
	initMasterResponder{

		masterResponder = OSCresponderNode(nil, \m,{|time, resp, msg|

			var inUID;

			msg=msg.drop(1);        // drop m
			inUID=msg[0];           // the incoming user ID
			msg=msg.drop(1);        // and drop it from the message

			// the test below is a saftey net to stop lnx sending messages to itself.
			// could stop hangs and bugs
			if ((uid.asSymbol)!=(inUID.asSymbol)) {
				if ((verbose)and:{msg[0]!=\broadcastProfile}and:{msg[4]!=\rcs}) {
					"IN: ".post;
					msg.postln;
				};
				responders[msg[0]].value(time, resp, msg);
			};

		}).add;
	}

	// send a bundled message (this isn't really sendBundle but i got used to this method's name)
	sendBundle {|time,msg|
		if ((verbose)and:{msg[0]!=\broadcastProfile}and:{msg[4]!=\rcs}) {
			"OUT: ".post;
			msg.postln
		};
		otherAddrs.do{|addr| addr.sendBundle(time,[\m,uid]++msg); };
	}

}

// socket for public networks - OSCGroups by Ross //////////////////////////////////////////////////

OscGroupClient {

	classvar <>verbose=true;

	classvar <>program, <>uid;

	var serveraddress, username, password, groupname, grouppassword, serverport, localtoremoteport,
		localtxport, localrxport, <responders, <pid, <netAddr;

	var masterResponder;

	*new {arg serveraddress, username, password, groupname, grouppassword, serverport = 22242,
			localtoremoteport = 22243, localtxport = 22244, localrxport;

		"OscGroupClient(".post;
		[serveraddress, username, password, groupname, grouppassword, serverport,
			localtoremoteport, localtxport, localrxport].do{|i,j|
				i.cs.post;
				if (j<8) {", ".post}{");".postln};
			};

		^super.newCopyArgs(serveraddress, username, password, "lnx1.3_"++groupname,
				grouppassword, serverport, localtoremoteport, localtxport, localrxport).init;
	}

	*initClass {
		program = (String.scDir +/+ "LNX_Client").unixSafe;
		"LNX_Client".killApp;
	}

	init {
		this.initMasterResponder;
		responders = IdentityDictionary[];
		if (localrxport.isNil) { localrxport = NetAddr.langPort };
	}

	join {
		(program + serveraddress + serverport + localtoremoteport + localtxport + localrxport +
			username + password + groupname + grouppassword).postln.unixCmdInferPID{|id|
				pid = id;
				if (pid.notNil) {
					program.postln;
					("OscGroupClient successfully started, attempting to connect to"
						+ serveraddress).postln;
					netAddr = NetAddr("localhost", localtxport);
					ShutDown.add{
						//"LNX_Client".killApp;
						("kill" + pid).systemCmd
					};
				}{
					"Check connections... the client could not be started".warn
				};
			}
	}

	// remove the master responder
	removeMasterResponder{
		masterResponder.remove;
		masterResponder = nil;
	}

	scan{ "OscGroupClient doesn't scan".reportError }

	close {
		this.removeMasterResponder;
		responders = IdentityDictionary[];
		("kill" + pid).systemCmd;
		pid = nil;
		responders = IdentityDictionary[];
	}

	// add a responder
	addResp{|id,function|
		id = this.formatSymbol(id);
		responders[id] = function
	}

	// remove a responder
	removeResp {|id|
		id = this.formatSymbol(id);
		responders[id] = nil
	}

	// all messages come via the masterResponder
	initMasterResponder{
		masterResponder = OSCresponderNode(nil, \m,{|time, resp, msg|
			var inUID;
			msg=msg.drop(1);        // drop \m
			inUID=msg[0];           // the incoming user ID
			msg=msg.drop(1);        // and drop it from the message

			// the test below is a saftey net to stop lnx sending messages to itself.
			// could stop hangs and bugs
			if ((uid.asSymbol)!=(inUID.asSymbol)) {
				if ((verbose)and:{msg[0]!=\broadcastProfile}and:{msg[4]!=\rcs}) {
					"IN: ".post;
					msg.postln;
				};
				responders[msg[0]].value(time, resp, msg);
			};

		}).add;
	}

	// send a bundled message (this isn't really sendBundle but i got used to this method's name)
	sendBundle {|time,msg|

		// if oscGroups running
		if (pid.notNil) {

			// junk
			var junk = [5.rand];
			junk = junk ++ (junk[0].collect{|r| [(2**15).rand,String.rand(5.rand)].choose});

			if ((verbose)and:{msg[0]!=\broadcastProfile}and:{msg[4]!=\rcs}) {
				"OUT: ".post;
				msg.postln
			};

			msg = [\m]++junk++[uid]++msg;		// make message
			msg[0] = this.formatSymbol(msg[0]);	// format symbol
			netAddr.sendBundle(time,msg);		// send message

		}

	}

	formatSymbol {|symbol|
		var str;
		str = symbol.asString;
		if (str[0] == $/) {
			^str.asSymbol
		}{
			^("/"++str).asSymbol
		}
	}

}

// a server for OSC. (This can cause issues when connection to yourself) ///////////////////////////

OscGroupServer {

	classvar <pid, <>program, <>serverCloseOnQuit=true;

	*initClass {

		if (LNX_Studio.isStandalone) {
			program = String.scDir.dropFolder(-2).unixSafe +/+ "LNX_Server";
		}{
			program = (String.scDir.unixSafe +/+ "LNX_Server");
		};
		program =
			String.scDir.dropFolder(LNX_Studio.isStandalone.if(-2,0)).unixSafe +/+ "LNX_Server";

		pid = "LNX_Server".pid;

		ShutDown.add{ if (serverCloseOnQuit.isTrue) {this.close} };
	}

	*start {|port=22242,timeoutSeconds=60,maxUsers=100,maxGroups=50|
		if (this.isRunning.not) {
			{
				(program + "-p" + port + "-t" + timeoutSeconds + "-u" + maxUsers + "-g"
					+ maxGroups ).runInTerminal;
			}.defer(0.25);
			{
				pid = "LNX_Server".pid;
			}.defer(0.5);
		}{
			"OscGroupServer is already running".postln;
		}
	}

	*close {
		("kill" + pid).systemCmd;
		"LNX_Server".killApp;
		"Terminal".killApp;
		pid = nil;
	}

	*isRunning{ ^("LNX_Server".pid).notNil }

}


// a null network (debugging)

LNX_NullSocket{
	classvar <>verbose=false;
	*join              {|...msg| if (verbose) {msg.postln} }
	*close             {|...msg| if (verbose) {msg.postln} }
	*sendMsg           {|...msg| if (verbose) {msg.postln} }
	*sendMsgArray      {|...msg| if (verbose) {msg.postln} }
	*sendBundle        {|...msg| if (verbose) {msg.postln} }
	*addResp           {|...msg| if (verbose) {msg.postln} }
	*removeResp        {|...msg| if (verbose) {msg.postln} }
	*sendRaw           {|...msg| if (verbose) {msg.postln} }
	*sendClumpedBundles{|...msg| if (verbose) {msg.postln} }
	*scan{ "LNX_NullSocket doesn't scan".reportError }
}

///////////////////////////////////////////////////////////////////////////////////////////////////

LNX_Applications{

	*all{
		var text,list,apps;
		text="ps -A -o pid -o command".unixCmdGetStdOut;
		list=text.split($\n).drop(1);
		apps=();
		list.do({|l,i| apps.add((l[6..].basename.split($ )[0]).asSymbol -> (l[0..4].asInteger)) });
		^apps
	}

	*allBySymbol{

		var text,list,apps;
		text="ps -A -o pid -o command".unixCmdGetStdOut;
		list=text.split($\n).drop(1);
		apps=();
		list.do{|l,i| apps[l[6..].asSymbol] = l[0..4].asInteger };
		^apps

	}


	*app{|pid| this.add[pid] }

	*allPID{
		var text,list,apps;
		text="ps -A -o pid -o command".unixCmdGetStdOut;
		list=text.split($\n).drop(1);
		apps=();
		list.do({|l,i| apps.add((l[0..4].asInteger) ->  (l[6..].basename.split($ )[0]).asSymbol ) });
		^apps
	}

	*post{
		var apps=this.all;
		apps.do({|i| (i+" ").post; apps.findKeyForValue(i).postln});
		^this
	}

	*pid{|app| ^this.all[app.asSymbol] }

	*kill{|app|
		var pid;
		pid = this.pid(app);
		if (pid.isNil) {
			^nil
		}{
			("kill" + pid).systemCmd;

			("kill" + pid).postln;
		}
	}

}

+ String {
	killApp{^LNX_Applications.kill(this)}
	pid{^LNX_Applications.pid(this)}
}

+ Symbol {
	killApp{^LNX_Applications.kill(this)}
	pid{^LNX_Applications.pid(this)}
}

///

/*
 NetAddr.localAddr.array
 NetAddr.fromArray([ 127, 0, 0, 1, 57120 ])
*/

+NetAddr{
	array{ ^this.ip.split($.).collect(_.asInt)++[port] }
	*fromArray {|array| ^this.new(array[0]++"."++array[1]++"."++array[2]++"."++array[3],array[4]) }
	digit{|digit=0| ^this.array[digit] }
	simpleString{^this.ip} //++"  "++port}
}


