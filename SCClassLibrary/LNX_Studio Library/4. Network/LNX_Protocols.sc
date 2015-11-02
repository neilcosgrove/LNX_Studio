
// Network protocols, various ways of sending messages and commands in a collaboration //
/*
	LNX_Protocols.testFilterWindow;
*/

LNX_Protocols{

	classvar <>verbose = false;

	classvar >network, 	>socket,		api,
			objects,		permanentObjects,
			messages,		tasks,		<latency,
			cid,			<uid,		thisUser,
			variableParameters;
	
	// these class vars are set: user experience & app performance
	// max size of a packet send via clumped list
	
//	classvar clumpPacketSize = 8192; 
//	classvar clumpPacketSize = 16384; 
	classvar clumpPacketSize = 32768;
//	classvar clumpPacketSize = 65536;   // ok on LAN
//	classvar clumpPacketSize = 81920;  // LAN: works but using 65536 for buffer overflow safety
//	classvar clumpPacketSize = 98304; // LAN: no... buffer overflow
		
	classvar clumpedInterval = 0.1;     // time between packets
	classvar updateInterval  = 0.1;    // time between comms update broadcasts
	classvar vpLifeSpand     = 0.1;   // life spand of vp updating 
	classvar testFilter      = 1;    // (for testing) probability of messages recieved 
	classvar testMode        = true;
		
	/////////////////////////////////////	

	// instance vars for sending clumped lists
	
	var id, isSender, message, objectID, time, packetNo;
	var method, size, totalSize, toHost, msgAPI, compress;

	// init the comms
	
	*initClass{
		permanentObjects   = IdentityDictionary[];
		objects            = IdentityDictionary[];
		messages           = IdentityDictionary[];
		variableParameters = IdentityDictionary[];
		tasks              = IdentityDictionary[];
		latency=0;
		// api for message support
		api=LNX_API.newPermanent(this,\pro,#[ \pP, \nsdOD, \nhcGD, \nstGD, \nstOD, \ngcS,
			\groupCmdSync, \hostCmdOD, \rcs, \nrGD, \nrOD, \nCS, \nI, \testFilter_]); 
	}
	
	// set the network latency
	*latency_{|l| latency=l+0.05} // margin (to be decided, may need to look at location)
	
	// register an object with an id
	*registerObject{|obj,id| if (id.notNil) { objects[id.asSymbol]=obj } }
	
	// register a permanent object that will never be removed
	*registerPermanentObject{|obj,id| if (id.notNil) {
		objects[id.asSymbol]=obj;
		permanentObjects[id.asSymbol]=obj
	}}
	
	// register a message (for use with sendClumpedList)
	*registerMessage{|obj,id| if (id.notNil) {
		objects[id.asSymbol]=obj;
		messages[id.asSymbol]=obj
	}}
	
	// remove a message
	*removeMessage{|id| messages[id.asSymbol]=nil; objects[id.asSymbol]=nil }
	
	// remove an object
	*removeObject{|id| objects[id.asSymbol]=nil; }
	
	// remove all objects
	*clearObjects{
		objects=(permanentObjects.copy)++(messages.copy);
	}
	
	*isConnected{^network.isConnected}
	
	///////////////////////////////////////////////////////////////////////////////////////////////
	// the senders ////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////
	
	// a simple message to everyone, no timing or order needed. this will defer
	// messages might be lost
	// recieves object.method(arg1,arg2...)
	
	*send{|object,method...args|
		if (network.isConnected) {
			socket.sendBundle(0,['lnxp',cid,uid,object,method]++args);
		}
	}
	
	// like send but will not defer. used for Common Time pinging
	// set up to be as quick as possible
	// recieves object.method(arg1,arg2...)
	
	*sendND{|object,method...args|
		if (network.isConnected) {
			socket.sendBundle(0,['lnxp_nd',cid,uid,object,method]++args);
		}
	}
	
	// like send but sends a list as the 1st arg
	// recieves object.method(list)
	
	*sendList{|object,method,list|
		if (network.isConnected) {
			socket.sendBundle(0,['lnxp_l',cid,uid,object,method]++list);
		}
	}
	
	// send to user, with no from user
	// user recieves object.method(arg1,arg2...)
	
	*sendTo{|userID,object,method...args|
		if (network.isConnected) {
			socket.sendBundle(0,['lnxp_tU',cid,uid,userID,object,method]++args);
		}
	}
	
	// like sendTo but no defer
	// user recieves object.method(arg1,arg2...)
	
	*sendToND{|userID,object,method...args|
		if (network.isConnected) {
			socket.sendBundle(0,['lnxp_tUND',cid,uid,userID,object,method]++args);
		}
	}
	
	// send a message which supplies a delta from Common Time
	// recieves object.method(delta,arg1,arg2...)
	
	*sendDelta{|delta,object,method...args|
		if ((network.isConnected)&&(network.isHost)) {
			delta=delta?latency; // use comms delta
			socket.sendBundle(0,['lnxp_d',cid,uid,object,method,this.now+delta]++args);
		}
	}
	
	// send a Variable Parameter, a unique id needs to be supplied.
	// changing values are sent using send, then after vpLifeSpand a GD of the last value is sent
	// this enables changing messages to be lost but a finale value agreed upon.
	
	*sendVP{|vpid,object,method...args|
		variableParameters[vpid]=[SystemClock.now,object,method,args]; // register this
		api.sendND(\pP,object,method,*args);
	}
	
	// send a message which supplies a delta from Common Time ////////////////////////////////
	// recieves object.method(delta,arg1,arg2...)
	
	*sendDeltaOD{|delta,object,method...args|
		if ((network.isConnected)&&(network.isHost)) {
			delta=delta?latency; // use comms delta
			api.sendOD(\nsdOD,object,method,this.now+delta,*args);
		}
	}
	
	// net version of sendDeltaOD
	
	*nsdOD{|object,method,delta...args|
		objects[object.asSymbol].performAPIMsgArg1(method,delta-this.now,args)
	}
	
	// do a command on the host studio, this sends with from user ID ////////////////////////////
	// host recieves object.method(userID,arg1,arg2...)
	// use GD because been sent to the host only
	
	*hostCmdGD{|object,method ...args|
		if ((network.isConnected.not) or: {network.isHost}){
			objects[object.asSymbol].performAPIMsgArg1(method,network.thisUser.id,args);
		};
		if (network.isConnected) {
			api.sendGD(\nhcGD,network.host,object,method,uid,*args);
		}
	}
	
	// net version of hostCmdGD
		
	*nhcGD{|hostID,object,method...args|
		if ((hostID==uid)and:{network.isHost}) {
			objects[object.asSymbol].performAPIMsg(method,args)
		}
	}
	
	// send to user, with no from user (Guaranteed Delivery) /////////////////////////////////
	// user recieves object.method(arg1,arg2...)
	
	*sendToGD{|userID,object,method...args|
		if (userID==uid) {
			this.nstGD(userID,object,method,*args);
		}{
			api.sendGD(\nstGD,userID,object,method,*args)
		}
	}
	
	// net version of sendToGD
		
	*nstGD{|userID,object,method...args|
		if (userID==uid) { objects[object.asSymbol].performAPIMsg(method,args) }
	}
	
	// send to user, with no from user (Ordered Delivery) ////////////////////////////////////
	// user recieves object.method(arg1,arg2...)
	
	*sendToOD{|userID,object,method...args|
		if (userID==uid) {
			this.nstOD(userID,object,method,*args);
		}{
			api.sendOD(\nstOD,userID,object,method,*args)
		}
	}
	
	// net version of sendToOD
		
	*nstOD{|userID,object,method...args|
		if (userID==uid) { objects[object.asSymbol].performAPIMsg(method,args) }
	}

	// all messages not recieved are resent and performed in order (Ordered Delivery) ////////
	
	*sendOD{|object,method...args|
		var msg, mid;
		if (network.isConnected) {
			mid = thisUser.oD_getNextID;			                // get next id
			msg = ['lnxp_od',cid,uid,mid,object,method,true]++args;
			thisUser.oD_msgs[mid] = msg;			                // store this message
			socket.sendBundle(0,msg);  		                // and send it
		}
	}
	
	// all messages not recieved are resent and performed in order (Ordered Delivery)
	// non deferal version, used in sendClumpedList (import or large messages fall silent)
	
	*sendODND{|object,method...args|
		var msg, mid;
		if (network.isConnected) {
			mid = thisUser.oD_getNextID;			                // get next id
			msg = ['lnxp_od',cid,uid,mid,object,method,false]++args;
			thisUser.oD_msgs[mid] = msg;			                // store this message
			socket.sendBundle(0,msg);  		                // and send it
		}
	}
			
	// all messages not recieved are resent (Guaranteed Delivery, order doesn't matter)
	
	*sendGD{|object,method...args|
		var msg, mid;
		if (network.isConnected) {
			mid = thisUser.gD_getNextID;			                // get next id
			msg = ['lnxp_gd',cid,uid,mid,object,method]++args;
			thisUser.gD_msgs[mid] = msg;			                // store this message
			socket.sendBundle(0,msg); 		                // and send it
		}
	}

	// a group command syched via the host. this includes host to themselves
	// a next availble sched time, (could be late due to delays and short latency)
	// everyone recieves object.method(arg1,arg2...) at the same time
	
	*groupCmdSync{|object,method ...args|
		if (network.isConnected) {
			if (network.isHost) {
				api.sendOD(\ngcS,object,method,this.now+latency,*args);
				{ objects[object.asSymbol].performAPIMsg(method,args); nil; }.sched(latency);
			}{
				api.sendToOD(network.host,\groupCmdSync,object,method,*args);
				                                                                 //send to host
			};
		}
	}
	
	// do a command on the host studio, this sends with from user ID
	// host recieves object.method(userID,arg1,arg2...)
	
	*hostCmd{|object,method ...args|
		if ((network.isHost)or:{network.isConnected.not}){
			objects[object.asSymbol].performAPIMsgArg1(method,network.thisUser.id,args);
		};
	
		if (network.isConnected) {
			socket.sendBundle(0,
				['lnxp_tH',cid,uid,network.host,object,method]++args);
		}
	}
	
	// the most powerful but with the most overheads
	// command will be evaluated by all in order via host
		
	*groupCmdOD{|object,method...args|
		if ((network.isConnected.not) or: {network.isHost}){
			this.hostCmdOD(object,method,*args);
		}{
			api.sendToOD(network.host,\hostCmdOD,object,method,*args);
		}
	}	
	
	///////////////////////////////////////////////////////////////////////////////////////////
	// the responders /////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////
	
	*initResponders{
		
		cid=network.collaboration.id;
		thisUser=network.thisUser;
		uid=thisUser.id;
		network.users.do(_.initMsgs);
		
		// from sendGD
		socket.addResp('lnxp_gd', {|time, resp, msg|
			if (msg[1]==cid) {
				if (testFilter.coin) {this.recieveGD(msg)}; // for testing
			};
		});
		
		// from sendOD
		socket.addResp('lnxp_od', {|time, resp, msg|
			if (verbose) {
				[msg[1], cid, msg[1].class, cid.class].postln;
			};
			if (msg[1]==cid) {
				if (testFilter.coin) {this.recieveOD(msg)}; // for testing
			};
		});
		
		// from send
		socket.addResp('lnxp', {|time, resp, msg|
			if (msg[1]==cid) {
				if (testFilter.coin) {
					{ objects[msg[3].asSymbol].performAPI(msg.drop(4)) }.defer;
				};
			};
		});
		
		// from sendND (normal delivery)
		socket.addResp('lnxp_nd', {|time, resp, msg|
			if (msg[1]==cid) {
				if (testFilter.coin) {
					objects[msg[3].asSymbol].performAPI(msg.drop(4));
				};
			};
		});
		
		// from sendList
		socket.addResp('lnxp_l', {|time, resp, msg|
			if (msg[1]==cid) {
				{ objects[msg[3].asSymbol].performAPIList(msg[4],[msg.drop(5)])}.defer;
			};
		});
		
		// from sendTo
		socket.addResp('lnxp_tU', {|time, resp, msg|
			if ((network.isConnected) and: {msg[1]==cid} and: {msg[3]==uid}) {
				{ objects[msg[4].asSymbol].performAPIMsg(msg[5],msg.drop(6)) }.defer;
			};
		});
		
		// from sendToND
		socket.addResp('lnxp_tUND', {|time, resp, msg|
			if ((msg[1]==cid) && (msg[3]==uid)) {
				objects[msg[4].asSymbol].performAPIMsg(msg[5],msg.drop(6)); // send with userID
			};
		});
		
		// from sendDelta
		socket.addResp('lnxp_d', {|time, resp, msg|
			if (msg[1]==cid) {
				objects[msg[3].asSymbol].performAPIMsgArg1( msg[4],msg[5]-this.now,msg.drop(6))
			};
		});
		
		// from hostCmd : double checks for id and is host
		socket.addResp('lnxp_tH', {|time, resp, msg|
			if ((network.isHost) and: {msg[1]==cid} and: {msg[3]==uid}) {
				{ objects[msg[4].asSymbol].performAPIMsgArg1(msg[5],msg[2],msg.drop(6)) }.defer;
			};
		});
		
		this.broadcastCommsStatus;
		this.startVariableParameterTask;
		// if (testMode) { {this.testFilterWindow}.defer(1) } { testFilter=1 };

	}
	
	// and to remove these responders
	
	*removeResponders{
		['lnxp'    ,	'lnxp_nd',	'lnxp_l',
		 'lnxp_tH' ,	'lnxp_tU',	'lnxp_tUND',
		 'lnxp_d'  ,	'lnxp_gd',	'lnxp_od'
		].do{|id| socket.removeResp(id) };
		
		tasks[\broadcastCommsStatus].stop;
		tasks[\broadcastCommsStatus]=nil;
		tasks[\checkVariableParameter].stop;
		tasks[\checkVariableParameter]=nil;
		
		LNX_Room.broadcastNormal;
		
	}
	
	// now in common time
	
	*now{ ^SystemClock.now-(network.thisUser.delta) }
	
	*postNow{|msg| ("Common time is: "+(this.now)).postln; msg.postln; }
	
	
	//////////////////////////////////////////////////////////////////////////////////
	// gD and oD (Guaranteed and Ordered Delivery) methods ///////////////////////////
	//////////////////////////////////////////////////////////////////////////////////
		
	////////
	// gD //
	////////
		
	// Reciever
	// recieve a gd message
	*recieveGD{|msg|
		var userID   =msg[2];
		var msgID    =msg[3];
		var objectID =msg[4];
		var method   =msg[5];
		var user = network.connectedUsers[userID];
		var ids, min, missing, object;
		if (user.notNil) {
			ids = user.gD_ids;
			min = user.gD_min;
			// if message id is greater than our min and not in ids then we haven't had it
			if ((msgID>min)&&(ids.includes(msgID).not)) {
				object=objects[objectID.asSymbol];
				if (object.notNil) {
					{objects[objectID.asSymbol].performAPIMsg(method,msg.drop(6))}.defer;
				}{
					("WARNING: Message for "++(objectID.asString)++
								" fell silent, object not registered.").postln;
					[objectID,method,msg].postln;
					
				};
				// remove and msgIDs from min up to the next missing id
				ids=ids.add(msgID);
				while {ids.includes(min+1)} {
					min=min+1;
					ids.remove(min);
				};
				// update user gD info			
				user.gD_ids_(ids);
				user.gD_min_(min);
			};		
		};	
	}
	
	// Sender (was called netResendGD)
	*nrGD{|userID,theirMin ...missing|
		// resend all messages
		missing.do{|i| socket.sendBundle(0,thisUser.gD_msgs[i]); };
		
		"Resent: ".post; missing.size.postln;
		
		// update my min of this user
		if (network.isUserConnected(userID)) {
			network.connectedUsers[userID].gD_theirMin_(theirMin);
			this.deleteGDHistory;
		};
	}
	
	// Sender
	*deleteGDHistory{
		var totalMin;
		// get the totalMin user min last id
		totalMin=network.connectedUsers.asList.difference(					[thisUser]).collect(_.gD_theirMin).minItem;
		// and remove these messages
		thisUser.gD_msgs.keys.select(_<=totalMin).do{|i| thisUser.gD_msgs[i]=nil };
	}
	
	////////
	// oD //
	////////
	
	// Reciever
	// recieve an od message
	*recieveOD{|msg|
	
		var userID   =msg[2];
		var msgID    =msg[3];
		
		var min, missing, msgs;
		var user = network.connectedUsers[userID]; // get user
		
		if (verbose) {
			[userID.class, msgID.class].post;
			msg.postln;
			"1".post;
		};
		
		if (user.notNil) {
			if (verbose) { "2".post };
			min = user.oD_min; // get the last min for this user
			if (verbose) { "3".post };
			if (msgID>min) {
				if (verbose) {"4".post};
				user.oD_msgs[msgID]=msg; // store it
				while {
					if (verbose) {"5".post};
					msg=user.oD_msgs[min+1];
					msg.notNil				// while next message not nil
				}{
					if (verbose) {"6".post};
					min=min+1;
					this.performODMsgs(msg.copy);	// perform it
					user.oD_msgs[min]=nil;			// remove it 
					user.oD_min_(min);				// store latest id
				};	
			};		
		};	
	}
	
	// perform is mainly used for error posting
	*performODMsgs{|msg|
		var object;
		if (verbose) {"7".post};
		if (msg.isNil) {
			"ERROR: Nil in OD perform, msg is nil.".postln;
		}{
			if (verbose) {"8".post};
			object=objects[msg[4].asSymbol];
			if (object.notNil) {
				if (verbose) {"9".post};
				if (msg[6].booleanValue) {
					{
						objects[msg[4].asSymbol].performAPIMsg(msg[5],msg.drop(7) );
					}.defer;
				}{
					objects[msg[4].asSymbol].performAPIMsg(msg[5],msg.drop(7) );
				};
			}{
				("WARNING: Message for "++(msg[4].asString)++
					" fell silent, object not registered.").postln;
					msg.postln;
			}
		}
	}
		
	// Sender (was called netResendOD)
	*nrOD{|userID,theirMin ...missing|
		
		if (verbose) {":".post; missing.post};
		
		if (network.isUserConnected(userID.asSymbol)) {
			
			if (verbose) {";".postln};
			
			{
			
				missing.do{|i|
					
					if (verbose) {thisUser.oD_msgs[i].postln};
					
					socket.sendBundle(0,thisUser.oD_msgs[i]); // resend all messages
					
					//0.01.wait; // this seems to overload network
					
				};
				
				// "Resent: ".post; missing.size.postln;
				
				if (theirMin>(network.connectedUsers[userID].oD_theirMin)) {
					// update my min of this user
					network.connectedUsers[userID].oD_theirMin_(theirMin);
					this.deleteODHistory;
				};
			
			}.fork(AppClock);
		}
	}
	
	// Sender
	*deleteODHistory{
		var totalMin;
		// get the totalMin user min last id
		totalMin=network.connectedUsers.asList.difference(					[thisUser]).collect(_.oD_theirMin).minItem;
		// and remove these messages
		thisUser.oD_msgs.keys.select(_<=totalMin).do{|i| thisUser.oD_msgs[i]=nil };
	}
	
	///////////////////////
	// gD & oD Broadcast //
	///////////////////////
	
	// Sender
	// broadcast what was your last message and what was the last thing you recieved from others
	*broadcastCommsStatus{
		var msg;
		tasks[\broadcastCommsStatus]={
			loop {
				msg=[uid,thisUser.gD_nextID,thisUser.oD_nextID];
				network.connectedUsers.asList.difference([thisUser]).do{|user|
					msg=msg++[user.id,user.gD_min,user.oD_min]
				};
				api.sendND(\rcs,*msg);
				updateInterval.wait;
			};
		}.fork(AppClock);
		LNX_Room.broadcastSlow;
	}
	
	// Reciever
	// recieve a comms broadcast, request any resends and clear message history
	*rcs{|userID,gD_nextID,oD_nextID...othersMin|
		var user = network.connectedUsers[userID];
		var ids, min, max, missing;
		
		if (user.notNil) {
			user.pingIn; // update last ping time
			
			ids = user.gD_ids;
			min = user.gD_min;

			// request a resend of all missing messages
			if (gD_nextID>min) {
				missing=((min+1)..gD_nextID).difference(ids)[0..1000];
				api.sendToND(userID,\nrGD,uid,min,*missing);
			};
			// and remove any used messages
			othersMin.clump(3).do{|l|
				if (l[0]==uid) {
					if (user.gD_theirMin<l[1]) {
						user.gD_theirMin_(l[1]);
						this.deleteGDHistory;
					};
				};
			};
			
			// + OD stuff as well
			
			ids = user.oD_msgs.keys;
			min = user.oD_min;

			if (verbose) {
//				"!".postln;
//				[oD_nextID,min,oD_nextID.class,min.class].postln;
			};

			// request a resend of all missing messages
			if (oD_nextID>min) {
				missing=((min+1)..oD_nextID).difference(ids)[0..1000];
				api.sendToND(userID,\nrOD,uid,min,*missing);
				if (verbose) {"*".postln};
			};
			// and remove any used messages
			othersMin.clump(3).do{|l|
				if (l[0]==uid) {
					if (user.oD_theirMin<l[2]) {
						user.oD_theirMin_(l[2]);
						this.deleteODHistory;
					};
				};
			};
		
		};	
	}
	
	/////////////////////////////
	// support for other sends //
	/////////////////////////////

	*startVariableParameterTask{
		var now;
		tasks[\checkVariableParameter]={
			loop {
				now=SystemClock.now;
				variableParameters.keysValuesDo{|vpid,l|
					//then,object,method,args
					if ((now-l[0])>vpLifeSpand) {
						api.groupCmdOD(\pP,l[1],l[2],*l[3]);
						variableParameters[vpid]=nil;
					}
				};
				0.15.wait;
			};
		}.fork(AppClock);
	}

	// host version of groupCmdOD
	// this is called by host in groupCmdOD (not used directly ??)
	
	*hostCmdOD{|object,method...args|
		objects[object.asSymbol].performAPIMsg(method,args); // ?? netSendTo is the same
		api.sendOD(\pP,object,method,*args);
	}
	
	// do a perform on .... (used in serveral methods)
	
	// pP means protocol perform and abriviated to help make shorter messages 
	
	*pP{|object,method...args|
		objects[object.asSymbol].performAPIMsg(method,args)
	}
	
	// net version of groupCmdSync (was called netGroupCmdSync)
	
	*ngcS{|object,method,time...msg|
		// sched to common time
		{
			objects[object.asSymbol].performAPIMsg(method,msg);
			nil;
		}.sched(time-(this.now));
	}

	//////////////////////////////////////////////////////////////////////////////////
	// send a clumped list ///////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////

	// do a command on the host studio, this sends with from user ID
	// host recieves object.method(list)
	
	*hostCmdClumpedList{|object,method,list,compress=true|
		if ((network.isHost)or:{network.isConnected.not}){
			objects[object.asSymbol].performAPIClump(method,list);
		};
	
		if ((network.isConnected)and:{network.isHost.not}) {
			this.sendClumpedList(object,method,list,true,compress);
		};
	}

	// send a large list (normally used to send song lists)	// uses OD to send all data
	
	*sendClumpedList{|objectID,method,list,toHost=false,compress=true|
		
		var clumpedList;
		
		if (compress) {list=list.compress};
		
		// attempt to fix remote networking issues (failed)
		// list = list.covertStrings;
		
		list=list.asArray; // msgSize only works on array and not a list

		// temp fix for [].msgSize bug && (1..1000000).msgSize
		clumpedList=list.lnx_ClumpBundles(clumpPacketSize);
		
		if (verbose) {
			"Sending clumps: ".post;
			list.size.post;
			" / ".post;
			clumpedList.size.postln;
		};
		
		// if we don't need to clump then send as 1 OD message
		if (clumpedList.size>1) {
			^super.new.init(objectID,method,clumpedList,toHost,compress,true,list.size)
		}{	
			if (toHost) {
				api.sendToOD(network.host,\nCS,objectID,method,compress,*list);
			}{
				api.sendOD(\nCS,objectID,method,compress,*list);
			}
		};
				
	}

	// send a large list clumped into packets
	// think about transfering this to gD ???
	
	init{|argObjectID, argMethod, argMessage, argToHost, compress,
			alreadyClumped=false, unclumpedsize|
				
		if (alreadyClumped) {
			totalSize=unclumpedsize;
			message = argMessage;
		}{
			totalSize = argMessage.size;
			message = argMessage.lnx_ClumpBundles(clumpPacketSize);
		};
		
		isSender     = true;
		objectID     = argObjectID;
		method       = argMethod;
		size         = message.size;
		toHost       = argToHost;
		
		id=LNX_ID.nextMsgID; // my next message id
		
		msgAPI=LNX_API.newMessage(this,id,[]); // no interface, only transmission
		
		api.sendODND(\nI,id,objectID,method,size,totalSize,toHost.if(1,0), compress.if(1,0));
		
		//  and wait for the others to register the message before i send it
		{this.startSending; nil;}.sched(clumpedInterval);
		
		//if (verbose) {~o=this};
		
	}
	
	startSending{
		{
			message.do{|msg,i|
				if (verbose) {
					"sending ".post; i.postln;
					msg.postln;
				};
				
				msgAPI.sendODND(\rM,*msg);
				clumpedInterval.wait;
				
			};
			this.remove;
		}.fork(AppClock);
	}

	// recieve ////
	// used if list size < 255 (was called netClumpledSmall)
	// covertStrings.reconstructStrings
	
	*nCS{|objectID,method,compress ... message|
		
		if (verbose) {
			"nCS: ".post;
			message.postln;	
			//~n=message;
		};
		
		if (compress.isTrue) {
			
			// attempt to fix remote networking issues (failed)
			//~q=message.flatten(1).reconstructStrings.decompress; 
			
			message.flatten(1).decompress;
			
			{objects[objectID.asSymbol].performAPIClump(
				
				// attempt to fix remote networking issues (failed)
				// method,message.flatten(1).reconstructStrings.decompress
				
				 method,message.flatten(1).decompress
				
			)}.defer;
		}{
			
			// attempt to fix remote networking issues (failed)
			//~q=message.flatten(1).reconstructStrings;
			
			message.flatten(1);
			
			{objects[objectID.asSymbol].performAPIClump(
				
				// attempt to fix remote networking issues (failed)
				// method,message.flatten(1).reconstructStrings
				
				method,message.flatten(1)
				
			)}.defer;
		}
	}
	
	// else sent in packets (clumps) (was called newIncoming)
	
	*nI{|id,objectID,method,size,totalSize,toHost,compress|
		if (network.isConnected) {
			
			if (verbose) {
				//~i=[id,objectID,method,size,totalSize,toHost,compress];
				"incoming".postln;
				[id,objectID,method,size,totalSize,toHost,compress].postln;
			};
			
			^super.new.initIncoming(id,objectID,method,size,totalSize,toHost,compress)
		}{
			^nil
		}
	}
	
	initIncoming{|argID,argObjectID,argMethod,argSize,argTotalSize,argToHost,argCompress|
		id           = argID;
		isSender     = false;
		objectID     = argObjectID;
		method       = argMethod;
		size         = argSize;
		totalSize    = argTotalSize;
		toHost       = argToHost.isTrue;
		message      = []!size;
		packetNo     = 0;
		compress     = argCompress.isTrue;
		
		if (verbose) {[id,objectID,method,size,totalSize,toHost,compress].postln};
		
		// register this message
		msgAPI=LNX_API.newMessage(this,id.asSymbol,[\rM]); // only interface is recieveMessage 
		
		time=SystemClock.now;
		
		//if (verbose) {~p=this};
		
	}
	
	// was called recieveMessage
	
	rM{|...msg|
		
		if (verbose) {
			"rM: ".post;
			msg.postln;
			//~m=msg;
		};
		
		message[packetNo]=msg;
		packetNo=packetNo+1;
		if (packetNo==size) {
			{
				if ((toHost.not)or:{toHost&&(network.isHost)}) {
					if (compress) {
						
						//~q=message.flatten(1).reconstructStrings.decompress;
						
						objects[objectID.asSymbol].performAPIClump(
						
							//attempt to fix remote networking issues (failed)
							//method,message.flatten(1).reconstructStrings.decompress
							
							method,message.flatten(1).decompress
							
						);
					}{
						
						//~q=message.flatten(1).reconstructStrings;
						
						objects[objectID.asSymbol].performAPIClump(
						
							//attempt to fix remote networking issues (failed)
							// method,message.flatten(1).reconstructStrings
							
							method,message.flatten(1)
							
						);
					}
				};
				this.remove;
			}.defer;
		};
	}

	remove{
		{
			msgAPI.free;
			id = isSender = message = objectID = time = packetNo
			= method = size = totalSize = toHost = msgAPI = nil;
		}.defer(1); // this needs defering and i'm not sure why
	}

		
	//////////////
	// test GUI //
	//////////////
	
	*testFilterWindow{
		var w;

		testMode=true;
		w = MVC_Window("Comms Test Filter",Rect(1075,0,200,70)).create;
		w.onClose_{
			testFilter=1;
			api.sendOD(\testFilter_,1);
		};
		MVC_MyKnob(w,Rect(90,20,26,26))
			.label_("Filter")
			.action_{|me| var val=me.value;
				testFilter=val.map(0,1,0.1,1);
				api.sendOD(\testFilter_,testFilter);
			}
			.numberFunc_{|n| (n).asFormatedString(1,2)}
			.value_(testFilter)
	
	}
	
	*testFilter_{|val| testFilter=val.clip(0.1,1) }
	
}

// end //

// ID's for Network objects //

LNX_ID{

	classvar	nextID=0,		nextMsgID=0,	>uid;
	
	// get an id for an object
	*nextID{ nextID=nextID+1; ^(nextID-1); }
	
	// get an id for a message
	*nextMsgID{
		nextMsgID=nextMsgID+1;
		^("m"++((nextMsgID-1).asString)++uid).asSymbol;
	}
	
	// what is the next id
	*queryNextID{^nextID}
	
	// set the next id
	*setNextID{|id| nextID=id.asInteger }
	
}

+ Nil { performAPIMsg{|...args|  "API didn't find ".post; args.postln } }

+ Object { isSymbol{^false} }

+ Symbol { isSymbol{^true} }

+ SequenceableCollection {
	
/*
	[\a,1,-3.33,2, "abc", 333].covertStrings;
	[\a,1,-3.33,2, "abc", 333, -3.33, -3.33, 2].covertStrings.reconstructStrings.postcs;
	a.a.getSaveList.covertStrings.reconstructStrings.postList;
	a.a.getSaveList.covertStrings.size;
	a.a.getSaveList.size;
	
	a.getSaveList.postList;
	a.getSaveList.size;
	a.getSaveList.covertStrings.reconstructStrings;
	a.getSaveList.collect(_.class).asSet
	a.getSaveList.covertStrings
	
*/
	
	covertStrings{
		var list=[];
		this.do{|i|
			if (i.isSymbol) { i=i.asString };	
			if (i.isString) {
				list=list.add(-3);
				list=list.add(i.size);
				list=list.add(i.ascii);
				
			}{
				if (i==(-3)) {
					list=list.add(-3);
					list=list.add(-3);
					
				}{
					list=list.add(i);
				};
			};
		};
		^list.flat
	}
	
	reconstructStrings{
		var list=[];
		var i=0;
		var size= this.size;
		var sSize;
		
		while {i<size} {
			if (this.at(i)==(-3)) {			
				if (this.at(i+1)==(-3)) {
					list = list.add(-3);
					i=i+2;
				}{
					sSize = this.at(i+1);
					if (sSize==0) {
						list = list.add("");
						i=i+2;
					}{
						list = list.add( 
							this[(i+2)..(i+1+sSize)].asArray.asciiToString.asSymbol) ;
						i=i+2+sSize;
					};
				};
			}{
				list = list.add(this.at(i));
				i=i+1;	
			};
		};
		^list;	
	}
}
