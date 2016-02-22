
// a collaboration of users //////////////////////////////////

LNX_Collaboration{

	classvar >network, <>ctPings=100, <>ctInterval=0.03, <>verbose=false;

	var	<isInviting=false,	
		<isStarting=false,
		<isConnected=false;
		
	var	<api;
	
	var	<id,			<host,
		<isHost=true,	<hostOrder,
		<users,		<disconnectedUsers;
	
	var	<room,		<wishList,		
		<invites,		<myInvite,
		<cancelList,  <ignoreList;
	
	var	<window, 		<gui,
		<tasks,		<maxLatency,
		<inviteLocation=1;
	
	var	<>action,		<>moveAction;
	
	*new{|room| ^super.new.init(room) }
	
	init{|argRoom|
		api  = LNX_API.newPermanent(this,\collaboration,#[
			\netUserLeave, \makeHost, \closeInviteWindow, \startTimeOut,
			\netSetUserColor, \setInviteString, \userCommonTimePing,
			\hostReturnCommonTimePing, \userUpdateLatency
		]);
		room = argRoom;
		id   = String.rand(8,8.rand).asSymbol;
		wishList          = IdentityDictionary[];
		users             = IdentityDictionary[];
		disconnectedUsers = IdentityDictionary[];
		invites           = IdentityDictionary[];
		tasks             = IdentityDictionary[];
		gui               = IdentityDictionary[];
		cancelList        = Set[];
		ignoreList        = Set[];
		maxLatency        = 0;
		LNX_Protocols.latency_(maxLatency);
		//ctPings=(LNX_Studio.isStandalone).if(ctPings,5); // set to 5 if testing
		if (room.name=="home") {inviteLocation=0} {inviteLocation=0}; // set invite location
	}
	
	// active users in a connected collaboration
	connectedUsers{ ^users.select{|user| disconnectedUsers.includesKey(user.id).not }}
	
	otherUsers{ ^this.connectedUsers.select{|user| user.id!=(network.thisUser.id) } }
	
	isUserConnected{|id| ^((users.includesKey(id)) and: {disconnectedUsers.includesKey(id).not}) }
	
	isConnecting{^(isStarting||isInviting)}
	
	// start things
	startServices{
		this.initResponders;
		this.timeOutInvites;
	}
	
	room_{|argRoom|
		room=argRoom;
		if (room.name=="home") {
			inviteLocation=0;
		}{
			inviteLocation=0;
		};
		if (window.isClosed.not) { gui[\location].value_(inviteLocation); };
	}
	
	// check all invites for time outs
	timeOutInvites{
		tasks[\timeOutInvites] = Task({
			loop { 
				invites.do({|invite|
					if (((SystemClock.now) - (invite.lastPingTime))>5) {
						this.removeInvite(invite.id);
					};	
				});
				1.wait;
			};
		}).start;
	}
	
	// add user to wish list
	addToWishList{|person|
		if ((wishList.includesKey(person.id)).not) {
			wishList[person.id] = person;
			this.refresh;
		};
	}
	
	// remove the user
	removeFromWishList{|id|
		if (wishList.includesKey(id)) {
			wishList[id] = nil;
			this.refresh;
		};
	}
	
	// gui call for the invite button
	guiInviteWishList{
		var inviteRoom, alreadyInCollab,t,newWishList;
		
		if (verbose) { "1. LNX_Collaboration:guiInviteWishList".postln };	
		switch (inviteLocation)
			{0} {inviteRoom=network.room    }                // this room
			{1} {inviteRoom=LNX_Room.random }                // a random room
			{2} {inviteRoom=LNX_Room(*network.rooms[1][1]) } // studio 1
			{3} {inviteRoom=LNX_Room(*network.rooms[1][2]) } // studio 2
			{4} {inviteRoom=LNX_Room(*network.rooms[1][3]) } // studio 3
		;
		
		if (network.isLAN) { inviteRoom=network.room };
		
		if (wishList.size==0){
			network.room.addViaInvite; // untidy but works
		};
		if (isConnected){
			// adding new users to a current collaboration will take alittle working out
			"Invite others to this collaboration... TO DO.".postln;
		}{
			// some checks 1st
			if ((isInviting.not) && (wishList.size>0)) {
				// work out if anyone is in a collab
				alreadyInCollab=wishList.select(_.inColab);
				if (alreadyInCollab.size>0) { // if they are tell the user
					t="";
					alreadyInCollab.collect(_.shortName).do{|i,j|
						t=t+i;
						case
							{j==(alreadyInCollab.size-1)} {t=t++""  }
							{j==(alreadyInCollab.size-2)} {t=t+"and"}
							{true}          {t=t++"," };
						};
					t=t+((alreadyInCollab.size>1).if("are","is"));
					network.room.addText("-"++t+
						"already in a collobration and will not see your invite.");
				};
				// if everyone is in a collab there is no point in starting an invite
				if (alreadyInCollab.size!=wishList.size){
					// but if not then lets invite the ones that are free
					isInviting=true;
					newWishList=wishList.copy;
					alreadyInCollab.keysDo{|id| newWishList[id]=nil};
					
					if (verbose) { 
						"2. LNX_Collaboration:guiInviteWishList (startInvite)".postln };
					
					// start a new invite process
					myInvite=LNX_MyInvite(inviteRoom,newWishList)
						.action_{|me| this.hostJoin(me) }
						.cancelAction_{|me|
							this.refresh;
							isInviting=false;
						};
				};
			};
		};
	}
	
	// these responders are here so we can create new invites
	initResponders{
		// to recieve an invite from someone
		network.socket.addResp('net_invite', {|time, resp, msg|
			if (isConnected.not) {
				this.netRecieveInvite(msg.drop(1));
			}
		});
		// they cancelled the invite
		network.socket.addResp('net_cancel_Invite', {|time, resp, msg|
			if (isConnected.not) {
				this.netCancelInvite(msg.drop(1));
			};
		});
		// start the collaboration
		network.socket.addResp('start_collaboration', {|time, resp, msg|
			if (isConnected.not) {
				this.userJoin(msg.drop(1));
			};
		});
		// user has left the collaboration
		network.socket.addResp('user_leave_collaboration', {|time, resp, msg|
			this.netUserLeave(msg.drop(1));
		});
	}
	
	// USER
	// to recieve an invite from someone (pass onto invites)
	netRecieveInvite{|msg|
		var invite=LNX_Invite(msg);
		
		if (verbose) { "3. LNX_Collaboration:netRecieveInvite".postln };
		
		if ((invite.versionMajor==LNX_Studio.versionMajor)
			and:{invite.versionMinor==LNX_Studio.versionMinor}) {
			
			if (verbose) { "4. LNX_Collaboration:netRecieveInvite (checked version)".postln };
			
			if (cancelList.includes(invite.id).not) {
				if (invite.includes(network.thisUser)) {
					// update for 1st object is still the invite
					
					if (verbose) {
						"5. LNX_Collaboration:netRecieveInvite (user in invite)".postln };
					
					if (invites[invite.id].isNil) {
						invite.cancelAction_{|me|
							invites[me.id]=nil; // remove this invite from my pending invites
							cancelList=cancelList.add(me.id);
							// do a block or ignore here
						};
						invites[invite.id]=invite;
						invite.createGUI;			// gui gets created here
					}{
						invites[invite.id].update(msg);
						invites[invite.id].refresh;
					};
				};
			};
			this.testUsersRespond(invite);
		}{
			
			if (verbose) { "6. LNX_Collaboration:netRecieveInvite (warn bad version)".postln };
			
			network.socket.sendBundle(nil,['decline_invite',invite.id, network.thisUser.id,
										LNX_Studio.versionMajor,LNX_Studio.versionMinor]);
			this.warnVersion(invite);
		}
	}
	
	// USER
	// the invite was cancelled by the host
	netCancelInvite{|msg| this.removeInvite(msg[0]) }
	
	// remove an invite (id)
	removeInvite{|id|
		if (invites.includesKey(id)) {
			invites[id].close;
			invites.removeAt(id);
		};
	}
	
	// called from room i.e. has left	
	removeUser {|userID|
		if (verbose) { "7. LNX_Collaboration:removeUser".postln };
		wishList.removeAt(userID);
		// when inviting
		if (isInviting) {myInvite.removeUser(userID)};
		
		// when connected
		if (isConnected) {
			if (users.includesKey(userID)) {
				if (verbose) { "8. LNX_Collaboration:removeUser (yes)".postln };
				// remove if in collaboration
				this.userHasLeftCollaboration(userID);
				// let others know
				api.sendClumpedList(\netUserLeave,[id,userID]);
					// this keeps it in the collaboration
			};
		};
		this.refresh;
	}
	
	/////////////////////////////////////////////////
	
	// recieve a user has left message 
	netUserLeave{|msg|
		var cid=msg[0].asSymbol;
		if (cid==id) {
			if (verbose) { "9. LNX_Collaboration:netUserLeave".postln };
			this.userHasLeftCollaboration(msg[1].asSymbol);
		}
	}	
	
	// used in leave and time out
	userHasLeftCollaboration{|userID|
		var nextHost;
		
		if (verbose) { "10. LNX_Collaboration:userHasLeftCollaboration".postln };
		
		if (this.isUserConnected(userID)) {
			users[userID].clearCommonTimeData;
			disconnectedUsers[userID]=users[userID];
			this.updateStudioUserGUI;
			
			this.addTextToDialog("-"+(users[userID].shortName.asString)+
				"has left collaboration.",true,true);
			
			LNX_InstrumentTemplate.onSoloGroup.removeUser(userID);
			// close collaboration if last user has left
			if ((disconnectedUsers.size+1)==(users.size)) {
				this.guiLeave;
			}{
				// else check for host status (checking if it's me just in case)
				if ((userID==host)and:{(userID==network.thisUser.id).not}) {
					hostOrder.reverse.do{|id|
						if (disconnectedUsers.includesKey(id).not) {
							nextHost=id;
						};
					};
					if (nextHost==network.thisUser.id) {
						this.makeMeHost;
						api.sendGD(\makeHost,network.thisUser.id);
					};	
				};
			};
		};
	}
	
	// the old host has left and i'm next in line, so make me the new host.
	makeMeHost{
		isHost=true;
		host=network.thisUser.id;
		this.addTextToDialog("You are now the host.");
		// do ping timing from here
	}
	
	// net call comes via network
	makeHost{|id|
		isHost=false; // not needed but just incase
		host=id;
		this.addTextToDialog(users[id].shortName++" is now the host.");
	}
	
	// add text to the dialog in the studio window
	addTextToDialog{|text,flash=true,force=false|
		 {network.addTextToDialog(text,flash,force)}.defer }
	
	updateStudioUserGUI{
		// do what you need here, i.e update GUI
		this.refreshUserView;
		{network.studio.setInstNumberColours}.defer; // this should update instColors
	}
	
	// gui call for leave collaboration
	guiLeave{
		if (isConnected) {
			network.socket.sendBundle(nil,['user_leave_collaboration',id,network.thisUser.id]);
			this.leave;
			network.disconnectCollaboration;
		}
	}
	
	// leave this collaboration (me)
	leave {
		this.addTextToDialog("You have left the collaboration",true); // dialog
		this.clearCommonTimeData; // remove common time
		tasks.do(_.stop);         // stop all tasks & reset vars
		tasks             = IdentityDictionary[];
		wishList          = IdentityDictionary[];
		users             = IdentityDictionary[];
		disconnectedUsers = IdentityDictionary[];
		maxLatency = 0;
		LNX_Protocols.latency_(maxLatency);
		isInviting=false;
		isConnected=false;
		isStarting=false;
	}
	
	// do i need any of this ?
	close{
		cancelList=Set[];
		ignoreList=Set[];
	}
	
	// gui call to press invite from user list double click or return
	pressInvite{
		gui[\invite].value_(1);
		{gui[\invite].value_(0);}.defer(0.3);
		this.guiInviteWishList;
	}
		
	//////////////////////////////////////////////////////////////////////////////
	
	// USERS
	// recieve a start_collaboration message from host
	userJoin{|msg|
		var invite, thisInvite;
		if (verbose) { "11. LNX_Collaboration:userJoin".postln };
		if (isStarting.not) {
			// msg comes in as a LNX_Invite and not a LNX_MyInvite
			invite=LNX_Invite(msg);
			if (invites[invite.id].notNil) {
				if (invite.confirms(network.thisUser)) {
					
					if (verbose) { "12. LNX_Collaboration:userJoin (is in invite)".postln };
					
					isInviting=false;
					isConnected=false;
					isStarting=true;
				
					// do i need to use thisInvite?
					thisInvite=invites[invite.id];
					thisInvite.update(msg);
					thisInvite.refresh;
					thisInvite.start;
					//invites[thisInvite.id]=nil;
					invite=thisInvite;
					
					wishList=IdentityDictionary[];
					this.refresh;
					
					if (verbose) {
						"Compair: ".post; [invite.roomList, network.room.getTXList].postln;
					};
					
					// do we need to change rooms
					if (((invite.roomList)==(network.room.getTXList))||(network.isLAN)){
						if (verbose) { "13. LNX_Collaboration:userJoin (this Room)".postln };
						this.startUser(invite);
					}{
						// move to roomList after evaluating func with value
						if (verbose) { 
							"14. LNX_Collaboration:userJoin (private Room)".postln };
						moveAction.value(invite.roomList,
							{|invite| this.userNewRoomStart(invite)},invite);
					};
				};
			};
		}
	}
	
	// HOST
	// join your own invite
	hostJoin {|invite|
		if (verbose) { "15. LNX_Collaboration:hostJoin".postln };
		isInviting=false;
		isConnected=false;
		isStarting=true;
		wishList=IdentityDictionary[];
		this.refresh;
		// do we need to change rooms
		
		if ((invite.roomList)==(network.room.getTXList)) {
			if (verbose) { "16. LNX_Collaboration:hostJoin (this room)".postln };
			this.startHost(invite)
		}{
			// move to roomList after evaluating func with value
			if (verbose) { "17. LNX_Collaboration:hostJoin (private room)".postln };
			moveAction.value(invite.roomList,{|invite| this.hostNewRoomStart(invite)},invite);
		};
	}
	
	// HOST
	// we have to change room 1st
	// the host waits for people to arrive and then says start
	hostNewRoomStart{|invite|
		if (verbose) { "18. LNX_Collaboration:hostNewRoomStart".postln };
		room=network.room; // update this room to changed room (for invitie)
		myInvite.gui[\myInvite].disable(1);
		myInvite.gui[\myInvite].disable(0);
		tasks[\newRoomCheck]= Task({
			var isEveryoneHere,i=0;
			loop { 
				isEveryoneHere=true;
				invite.invitedUsers.do{|person|
					// make sure its not me
					if (person.id!=network.thisUser.id) { 
						if (room.includes(person).not) {isEveryoneHere=false}
					}
				};
				if (isEveryoneHere) {
					tasks[\newRoomCheck].stop;
					{20.do{
						network.socket.sendBundle(nil,
							['start_collaboration_after_wait',invite.id]);
						0.05.wait;
					};}.fork;
					this.startHost(invite);
				};
				myInvite.gui[\myInvite].string2_("Waiting for other users: "++
										((10.8-i).asInt.asString));
				0.2.wait;
				i=i+0.2;
				if ((i>5)&&(i<5.5)) {
					myInvite.gui[\myInvite].enable(1);
					myInvite.gui[\myInvite].actions[1]={
						tasks[\newRoomCheck].stop;
						{20.do{
							network.socket.sendBundle(nil,
								['start_collaboration_after_wait',invite.id]);
							0.03.wait;
						};}.fork;
						this.startHost(invite);
					};
				};
				if (i>10) {
					tasks[\newRoomCheck].stop;
					{20.do{
						network.socket.sendBundle(nil,
							['start_collaboration_after_wait',invite.id]);
						0.03.wait;
					};}.fork;
					this.startHost(invite);
				};
			};
		}).start(AppClock);
	}
	
	// USERS
	// we have to change room 1st
	// time out host
	userNewRoomStart{|invite|
		if (verbose) { "19. LNX_Collaboration:userNewRoomStart".postln };
		room=network.room; // update this room to changed room (for invitie)
		tasks[\newRoomCheck]= Task({
			var i=0;
			invite.window.disable(1).disable(0);
			loop { 
				invite.window.string2_("Waiting for other users: "
					++((10.8-i).asInt.asString));
				0.3.wait; // this is a little slower than the host
				i=i+0.2;
				if (i>10) {
					tasks[\newRoomCheck].stop;
					this.removeInvite(invite.id);
					isInviting=false;
					isConnected=false;
					isStarting=false;
				};
			};
		}).start(AppClock);
		
		network.socket.addResp('start_collaboration_after_wait', {|time, resp, msg|
			if (msg[1].asSymbol==(invite.id.asSymbol)) {
				tasks[\newRoomCheck].stop;
				network.socket.removeResp('start_collaboration_after_wait');
				this.startUser(invite);
			};
		});
	}
	
	// exchange users in the collaboration ////////////////////////////////////////////////
	
	// HOST start
	startHost{|invite|
		var colorList, list, l,t;
		if (verbose) { "20. LNX_Collaboration:startHost".postln };
		{
			// make a list of all users in room and in the invite
			users=IdentityDictionary[];
			invite.invitedUsers.do{|person|
				if (room.includes(person)) {
					users[person.id]=room.occupants[person.id];
				};
			};
			
			// i am the host
			// and i'm 1st in host order
			// everyone else is later, the order doesn't matter
			isHost=true;
			hostOrder=[network.thisUser.id]++users.collect(_.id);
			host=hostOrder[0]; // me
			users[network.thisUser.id]=network.thisUser; // now add me to the users
			
			colorList=[Color(1.0,0.5,0.0),Color(1.0,0.0,0.0),Color(0.0,1.0,0.0),
			           Color(0.0,0.0,1.0),Color(1.0,0.0,0.5),
			           Color(0.5,1.0,0.0),Color(0.4,0.4,1.0)]++
					[{Color.fromArray([0.5.rand+0.5,1.0.rand,0.5.rand].scramble)}];
			hostOrder.do{|uID,i| users[uID].color_(colorList.clipAt(i).value)};

			{
				list=['setup_collaboration',invite.id]++(this.getTXList);
				50.do{
					network.socket.sendBundle(nil,list);
					0.06.wait;
				};
			}.fork(AppClock);
			
			gui[\inviteGUI]=invite.gui[\myInvite];
			
			network.room.tasks[\timeOutUsers].stop; // stop time out temp
			
			network.studio.stopNow; // stop the clock 
			
			LNX_Protocols.initResponders; // start communications
			this.startAllCollaborationSevices;
			{this.hostEstablishCommonTime(users.size)}.defer(0.6);
						// the host establishs common time
			{
				0.6.wait;
				this.hostSetString("Syncing network... 3");
				(ctInterval*ctPings/3).wait;
				this.hostSetString("Syncing network... 2");
				(ctInterval*ctPings/3).wait;
				this.hostSetString("Syncing network... 1");
				(ctInterval*ctPings/3).wait;
				this.hostSetString("Syncing network.");
			}.fork(AppClock);
			
			{
				this.computeCommonTime;
				network.studio.syncCollaboration;
			}.defer(1+(ctInterval*ctPings)); // send common time & the song
			
			{
				api.groupCmdSync(\closeInviteWindow,invite.id); // this needs to arrive !
				api.groupCmdSync(\startTimeOut);                // this needs to arrive !
				users.do{|u|
					api.sendGD(\netSetUserColor,u.id,u.color.red,u.color.green,u.color.blue)
				};
				
				// announce the collaboration to the room
				l=users.asList.collect(_.shortName);
				t="";
				l.do{|i,j|
					t=t+i;
					case
						{j==(l.size-1)} {t=t++""  }
						{j==(l.size-2)} {t=t+"and"}
						{true}          {t=t++"," };
					};
				room.roomMessage("-"++t+"have started a collobration together.");
				
			}.defer(2.0+(ctInterval*ctPings)); // close wins
			
		}.defer(0.5); // a slight delay for users to catch up
	}
	
	// net set the colour of the users
	netSetUserColor{|userID,r,g,b|
		users[userID].color_(Color(r,g,b));
		this.refreshUserView;
	}
	
	// USER start
	// user invite Process step: F
	startUser{|invite|
		if (verbose) { "21. LNX_Collaboration:startUser".postln };
		network.socket.addResp('setup_collaboration', {|time, resp, msg|
			if (msg[1].asSymbol==(invite.id.asSymbol)) {
				network.socket.removeResp('setup_collaboration');
				this.putTXList(msg.drop(2));
				host=hostOrder[0]; // the host is 1st in the list
				isHost=false;
				
				gui[\inviteGUI]=invite.window;
				network.room.tasks[\timeOutUsers].stop;  // stop time out temp
				
				LNX_Protocols.initResponders; // start communications
				this.startAllCollaborationSevices;
			};	
		});
	}
	
	closeInviteWindow{|id|
		{
			if (myInvite.notNil) { myInvite.connected};
			invites.do(_.connected);
			gui[\inviteGUI]=nil;
			invites[id]=nil;
			network.closeNetworkWindow;
		}.defer;
	}
	
	// set string in invite
	hostSetString{|text|
		api.send(\setInviteString,text);
		this.setInviteString(text);
	}
	
	setInviteString{|text|
		{
			if ((gui[\inviteGUI].notNil)&&(network.window.isClosed.not)) {
				gui[\inviteGUI].string2_(text.asString)
			};
		}.defer;
	}
	
	startTimeOut{
		network.room.tasks[\timeOutUsers].start
	}
	
	// and the magic starts here ///////////////////////////////////////////////////
	
	// HOST + USERS !!!
	startAllCollaborationSevices{
		if (verbose) { "22. LNX_Collaboration:startAllCollaborationSevices".postln };
		isInviting=false;
		isStarting=false;
		isConnected=true;
		this.refreshUserView;
		users.do{|user| LNX_InstrumentTemplate.onSoloGroup.addUser(user.id) };
		action.value; // move over to the network object now
	}
	
	// COMMON TIME /////////////////////////////////////////////////////////////////
	
	// used to establish a common time for use in clocks and timed messages
	hostEstablishCommonTime{|noUsers|
		//noUsers.postln;
		{
			ctPings.do{|i|
				api.sendND(\userCommonTimePing,network.thisUser.id,SystemClock.now,i);
				ctInterval.wait;
			};
		}.fork(SystemClock);
	}
	
	// user recieves a ctp
	userCommonTimePing{|userID,time,i|
		api.sendToND(userID,\hostReturnCommonTimePing,network.thisUser.id,time,SystemClock.now,i);
	}
	
	// a returning ctp
	hostReturnCommonTimePing{|userID,myTime,theirTime,i|
		var now, latency, delta;
		now=SystemClock.now;
		latency=(now-myTime)/2; // average latency
		delta=theirTime-latency-myTime;
		users[userID].returnCommonTimePing(delta,latency);
	}
	
	// compute for common time the average delta, average latency and max latency 
	// for all users and send results to users
	computeCommonTime{
		this.otherUsers.do{|user|
			user.computeCommonTime;
			api.sendGD(\userUpdateLatency,user.id,user.delta,user.latency,user.maxLatency);
			if (user.maxLatency>maxLatency) {
				maxLatency=user.maxLatency;
				LNX_Protocols.latency_(user.maxLatency);
			};
		}
	}
	
	// clear all users ping data.
	clearCommonTimeData{
		this.users.do{|user|
			user.clearCommonTimeData;
		}
	}
	
	// update user latency
	userUpdateLatency{|userID,delta,latency,maxLatency|
		if (userID==(network.thisUser.id)) {
			network.thisUser.delta_(delta)
				.latency_(latency)
				.maxLatency_(maxLatency);
		}{
			users[userID].delta_(delta)
				.latency_(latency)
				.maxLatency_(maxLatency);
		};
	}

	////////////////////////////////////////////////////////////////////
	
	// put the TX List of a collaboration
	putTXList{|list|
		var noUsers, profileSize,user,userList;
		list=list.reverse;
		users=IdentityDictionary[];
		id=list.popS.asSymbol;
		#noUsers,profileSize=list.popNI(2);
		noUsers.do{
			userList=list.popNS(profileSize);
			if (userList[0].asSymbol==network.thisUser.id) {
				user=network.thisUser;
			}{
				user=room.occupants[userList[0].asSymbol];
				if (user.isNil) {
					"I didn't get user ****".postln;
					user=LNX_User.newPublicUser(userList);
				};	
			};
			users[user.id]=user;
		};
		hostOrder=list.popNS(noUsers);
		hostOrder=hostOrder.collect(_.asSymbol);
	}
	
	// get the TX list of this collaboration
	getTXList{
		// collaborationID, noUsers, profileSize, [users], [hostOrder]
		var list;
		list=[id,users.size,network.thisUser.getPublicList.size];
		users.do{|user|
			list=list++(user.getPublicList);
		};
		list=list++hostOrder;
		^list
	}
	
	testUsersRespond{|invite|
		// for other test users /////  TEST ***************
		network.room.testUsers.do{|u|
			if (invite.includes(u)) {
				if (0.3.coin) {
					network.socket.sendBundle(nil,['accept_invite',invite.id,u.id]);
				}{
					if (0.3.coin) {	
						network.socket.sendBundle(nil,['decline_invite',invite.id,u.id]);
					};
				};
			}
		}
	}

	//////////////////////////////
	//                          //
	//  GUI                     //
	//                          //
	//////////////////////////////

	createGUI{|argWindow,x=0,y=0|	
	
		var col;
	
		window=argWindow;
		
		col=Color(0,0,0,0.5);
		
		gui[\tabView]=TabbedView.new(window,Rect(131+x,363+40+y,380+4+5,90+80-75),
			["                                         ",
			 "                                                                           "])
			.tabPosition_    (\bottom)
			.followEdges_    (true)
			.labelColors_    ([Color(0,0,0,0),col])
			.unfocusedColors_([Color(0,0,0,0),col])
			.backgrounds_    ([col,col])
			.font_(GUI.font.new("Helvetica",11))
			.tabHeight_(25)
			.value_(0)
			.action_{|me|};		
		
		MVC_PlainSquare(window,Rect(136+x,368+40+y,379,59).insetBy(-1,-1))
			.color_(\off,Color.white*0.6);
		
		// invite list
		gui[\wishList] = TextView(window,Rect(136+x,368+y+40,379,59))
			.editable_(false)
			.background_(Color(0,0,0))
			.stringColor_(Color.white)
			.font_(Font("STXihei", 12));
			
		this.refresh;
		
		// invite
		gui[\invite] = MVC_OnOffView(window,Rect(460+x,432+y+40,55,20).insetBy(-1,-1),"Invite")
			.rounded_(true) 
			.font_(Font("Helvetica-Bold",12))
			.color_(\on,Color(0.2,1,0.2))
			.color_(\off,Color(0.2,1,0.2))
			.action_{ this.guiInviteWishList };
			
		// leave
		gui[\invite] = MVC_OnOffView(window,Rect(400+x,432+y+40,55,20).insetBy(-1,-1),"Leave")
			.rounded_(true) 
			.font_(Font("Helvetica-Bold",12))
			.color_(\on,Color.gray*1.5)
			.color_(\off,Color.gray*1.5)
			.action_{ this.guiLeave };
				
		// Invite to... text
		StaticText(window,Rect(284+x-200+6-20,436+y+40,180+20,18))
			.canFocus_(false)
			.string_("Invite to a collaboration in…").align_(\right).stringColor_(Color.black)
			.font_(GUI.font.new("Helvetica",12));
			
		// location to go on invite
		gui[\location]=MVC_PopUpMenu3(window,Rect(284+x,435+y+40,108,16))
			.items_(["This Room","A Private Room",
					"Studio1: Public","Studio2: Public","Studio3: Public"])
			.color_(\background,Color.ndcMenuBG)
			//.focusColor_(Color.grey(alpha:0.05))
			.action_{|me|
				inviteLocation=me.value;
			}
			.font_(Font("Helvetica",11))
			.value_(inviteLocation);
			
	}
	
	// refresh the wishList
	refresh{
		var text;
		{
			text="";
			wishList.collect{|p| p.name }
			        .asList
			        .sort{|a,b| (a.asString.toLower)<(b.asString.toLower)}
			        .do{|n| text=text++n++",  " };
			if (window.isClosed.not) {        
				gui[\wishList].string=text.drop(-3);
			};
		}.defer;
	}
	
	// autosize window and gui items to no of users /////////////
	
	autoSizeGUI{|top=0|
		var noLines, h, y = 0;
		noLines=((users.size/2).ceil);
		if (noLines>4) {
			gui[\userScrollView].hasHorizontalScroller_(true)
		}{
			gui[\userScrollView].hasHorizontalScroller_(false)
		};
		
		if (noLines==0) {
			h = -6;
			y = 100;
			// gui[\userScrollView].parent.removeView(gui[\userScrollView]);
		} {
			h = (noLines*16+8).clip(1,4*16+5);
		};
		
	//	gui[\userScrollView].bounds_(gui[\userScrollView].bounds.height_(h));
			
		gui[\userScrollView].bounds_( Rect(5,488-h+y+top,202,h) );
			
			
		gui[\userView].bounds_(gui[\userView].bounds.height_(noLines*16+5));
		
		^h;
	}
	
	// warn about version //
	
	warnVersion{|invite|
	
		network.room.addText("");
		network.room.addText(					
			"You have been invited to join a collaboration by" + invite.hostName +
			"but can not take part in it because you don't have the same version"+
			"of LNX_Studio as they do."
		);
		network.room.addText(
			"You are using v"++
			(LNX_Studio.versionMajor)++"."++(LNX_Studio.versionMinor)+
			"and they are using v"++
			invite.versionMajor++"."++invite.versionMinor
		);
		network.room.addText("");		
		network.room.addText(
			"You can download the latest version of this software at"+
			"www.lnx_studio@sourceforge.net"
		);
		network.room.addText("");
	
	}
	
	// studio window users /////////////
	
	createStudioGUI{|window|
	
		var y,h,t,l,w,swb;
		
		h=130;
		swb=window.bounds;
		
	//	y=swb.height+115+1+12-4;
		y=393;
		
		w=190;
		t=swb.top;
		l=swb.left;
	
		// scroll view for user
		gui[\userScrollView] =MVC_ScrollView(window, Rect(5, 435, 202, 8))
			.hasBorder_(true)
			.color_(\background,Color(59/77,59/77,59/77)/6)
			.hasVerticalScroller_(true); // to remove
	
		// view for users in collaboration
		gui[\userView] = MVC_UserView(gui[\userScrollView],Rect(0,1,w,16*1))
			.canFocus_(false)
			.drawFunc={|me|
				if (isConnected) {
					Pen.smoothing_(true);
					Pen.font_(Font("Sathu",12));
					hostOrder.do{|userID,i|
						var x1,y1,h1,connected,user;
						user=users[userID];
						connected=this.isUserConnected(userID);
						h1=16;
						x1=((i%2)*(w/2));
						y1=((i/2).asInt*h1);
						if (connected) {
							Pen.fillColor_(Color.black);
					
							Pen.stringInRect(user.shortName.asString[0..7],
											Rect(x1+15+15+2,y1+1,(w/2)-15,h1));
							Pen.fillColor_(Color.white);
						}{
							Pen.fillColor_(Color.white);
						};
						Pen.stringInRect(user.shortName.asString[0..7],
											Rect(x1+15+15,y1,(w/2)-15,h1));
						Pen.fillColor_(Color.black);
						DrawIcon( \user, Rect(x1+13-1,y1+1,h1+3,h1+2).insetBy(-1,-1) );
						DrawIcon( \user, Rect(x1+13+1,y1+1,h1+3,h1+2).insetBy(-1,-1) );
						DrawIcon( \speaker, Rect(x1-1+(0/2),y1,h1+3,h1+3).insetBy(-3,-3) );
						if (connected) {
							Pen.fillColor_((user.color*0.9).set);
						}{
							Pen.fillColor_(Color.black);
						};
						DrawIcon( \body, Rect(x1+13,y1+1,h1+3,h1+2).insetBy(0.5,0.5) );
						if (connected) {
							Pen.fillColor_(user.color*1.3+0.3);
						}{
							Pen.fillColor_(Color.black);
						};
						DrawIcon( \head, Rect(x1+13,y1+1,h1+3,h1+2).insetBy(0.5,0.5) );
						if ((connected)&&(user.isListening)) {
							Pen.fillColor_(Color.green);
						}{
							Pen.fillColor_(Color.black);
						};
						DrawIcon( \speaker, Rect(x1-1+(0/2),y1,h1+3,h1+3) );
					};
				};
			};
		
	}
	
	refreshUserView{
		{
			if (gui[\userView].notNil) {
				gui[\userView].refresh;
			};
		}.defer
	}

}

// end //
