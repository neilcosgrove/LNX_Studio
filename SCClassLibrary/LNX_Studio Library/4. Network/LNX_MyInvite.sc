
// my invite to other people //////////////////////////////////////////////////////////////////

LNX_MyInvite{

	classvar >network;

	var	<id,
		<hostID,
		<hostName,
		<rejects,
		<wishList,
		<invitedUsers,
		<room,

		<lastPingTime,
		
		<tasks,
		
		<window,
		
		<gui,
		
		<>action,
		<>cancelAction;
		
	*new{|room,wishList| ^super.new.init(room,wishList) }

	// i am allowing colabs in the home room now, i stopped this before
	init{|argRoom,argWishList|
		//if (argRoom.name!="home") {
			this.initInvite(argRoom,argWishList);
		//}{
//			{cancelAction.value(this)}.defer(1);
//			LNX_InviteInterface(network.window,
//				"You can not start a collaboration in the \"Home\" Room !\n"++
//				"TIP: Move to another Public Room or invite others to a Private Room",
//				[ "Ok" ],[{0}],[true],
//				color:Color.orange,
//				background:Gradient(Color(0.4,0.4,0.4),Color(0.6,0.6,0.4)),
//				w:100,
//				h:30,
//				name:"Warning",
//				wishList:(),
//				invitedUsers:(),
//				rejects:(),
//				myInvite:true
//			).onCloseIndex_(0);	
//		};
	}
	
	initInvite{|argRoom,argWishList|
		id = String.rand(10,4).asSymbol;
		hostID=network.thisUser.id;
		hostName=network.thisUser.name;
		room=argRoom;
		wishList=argWishList;
		wishList[network.thisUser.id]=network.thisUser;
		invitedUsers=IdentityDictionary[];
		invitedUsers[network.thisUser.id]=network.thisUser;
		rejects=IdentityDictionary[];
		tasks=IdentityDictionary[];
		gui=IdentityDictionary[];
		this.startInvitation;
		this.createGUI;
		this.initResponders;
	}
	
	// start the invitation
	startInvitation{
		tasks['invite_wishList'] = Task({
			loop {
				network.socket.sendBundle(0,['net_invite']++(this.getTXList));
				0.25.wait;
			};
		}).start;
	}
	
	initResponders{
		network.socket.addResp('accept_invite', {|time, resp, msg|
			this.netAcceptInvite(msg.drop(1));
		});
		network.socket.addResp('decline_invite', {|time, resp, msg|
			this.netDeclineInvite(msg.drop(1));
		});
	}
	
	removeResponders{
		['accept_invite','decline_invite'].do{|id|
			network.socket.removeResp(id)
		};
	}
	
	// user said "Yes"
	netAcceptInvite{|msg|
		var iid=msg[0].asSymbol ,uid=msg[1].asSymbol;	
		if (iid==id) {
			invitedUsers[uid]=wishList[uid];
			rejects[uid]=nil; 				// shouldn't be needed but safer with it
			this.refresh;
			if (invitedUsers.size>=2) {
				if (((invitedUsers.size)+(rejects.size))==(wishList.size)) {
					this.start;
				}{
					{gui[\myInvite].enable(1)}.defer;
				};
			};
		};
	}
	
	// user says "No"
	netDeclineInvite{|msg|
		var iid=msg[0].asSymbol,uid=msg[1].asSymbol;
		// if they supplied their version number then warn user
		msg.postln;
		
		if (msg.size>2) {
			network.room.addText("");
			network.room.addText(					
				"You tried to invite" +
				
				((room.occupants[uid].isNil).if{"a user"}{room.occupants[uid].name}) +
				
				"to a collaboration"+
				"but they can not take part because they do not have the same version of"+
				"LNX_Studio as you do."
			);
			network.room.addText(
				"You are using v"++
				(LNX_Studio.versionMajor)++"."++(LNX_Studio.versionMinor)+
				"and they are using v"++
				(msg[2])++"."++(msg[3])
			);
			network.room.addText("");		
			network.room.addText(
				"You can download the latest version of this software at"+
				"www.lnx_studio@sourceforge.net"
			);
			network.room.addText("");
		};
		if (iid==id) {
			rejects[uid]=wishList[uid];
			invitedUsers[uid]=nil;			// shouldn't be needed but safer with it
			this.refresh;
			if (invitedUsers.size<2) {
				{gui[\myInvite].disable(1)}.defer;
				if ((rejects.size+1)==(wishList.size)) {
					this.allRejected;
				};
			}{
				if (((invitedUsers.size)+(rejects.size))==(wishList.size)) {
					this.start;
				};
			};
		};
	}
	
	// user has left the room
	removeUser{|id|
		if (wishList.includesKey(id)) {
			rejects[id]=wishList[id];
			invitedUsers[id]=nil;
			this.refresh;
			if (invitedUsers.size<2) {
				{
					if (gui[\myInvite].window.isClosed.not) {
						gui[\myInvite].disable(1)
					};
					
				}.defer;
				if ((rejects.size+1)==(wishList.size)) {
					this.allRejected;
				};
			}{
				if (((invitedUsers.size)+(rejects.size))==(wishList.size)) {
					this.start;
				};
			};
		};
	}

	getTXList{
		var list=[], l;
		
		list=list++[id,hostID,hostName,LNX_Studio.versionMajor,LNX_Studio.versionMinor];
		
		l=room.getTXList; // this should be encrypted for each user
		list=list++[l.size]++l;
		
		// all the people i want (ids and names)
		l=wishList.asList.sort{|a,b| (a.name.asString.toLower)<(b.name.asString.toLower)};
		list=list++[l.size]++(l.collect({|p| [p.id,p.name]}).flat);
		// the people who said yes (just ids)
		l=invitedUsers;
		list=list++[l.size]++(l.collect(_.id));
		
		// and thoose that said no (just ids)
		l=rejects;
		list=list++[l.size]++(l.collect(_.id));
		
		^list;
	}
	
	close{
		tasks.do(_.stop);
		network.socket.sendBundle(0,['net_cancel_Invite',id]);
		this.removeResponders;
		cancelAction.value(this);
		invitedUsers=IdentityDictionary[];
		rejects=IdentityDictionary[];
		wishList=IdentityDictionary[];
		{gui[\myInvite].close}.defer;
		//this.free;
	}
	
	roomList{^room.getTXList}
	
	// host start: send out the collaboration txList
	start{
		var list;
		if (invitedUsers.size>=2) {
			tasks['invite_wishList'].stop;
			{
				list=['start_collaboration']++(this.getTXList);
				20.do{		                                                       /// ********         
					network.socket.sendBundle(nil,list);
					0.03.wait;
				};
				// do above 1st because action.value might change rooms which disconnects client
				action.value(this);
				{
					gui[\myInvite].disable(1);
					gui[\myInvite].string2_("Starting...");
				}.defer;
				this.removeResponders; // stop listening
			}.fork;
		};
	}
	
	allRejected{
		tasks.do(_.stop);
		network.socket.sendBundle(0,['net_cancel_Invite',id]);
		this.removeResponders;
		{
			gui[\myInvite].string2_("Failed.");
			gui[\myInvite].iconName = \warning;
		}.defer;
		{
			cancelAction.value(this);
			invitedUsers=IdentityDictionary[];
			rejects=IdentityDictionary[];
			wishList=IdentityDictionary[];
			gui[\myInvite].close;
			//this.free;
		}.defer(5);
		
	}
	
	connected{
		tasks.do(_.stop);
		gui[\myInvite].close;
	}
	
	
	createGUI{
		
		gui[\myInvite]=LNX_InviteInterface(network.window,this.inviteString,
			[ "Cancel", "Start" ],[{this.close},{this.start}],[true,false],
			color:Color(29/65,1,42/83)*1.5,
			background:Gradient(Color(0.4,0.4,0.4),Color(0.4,0.45,0.4)),
			w:100,
			h:30,
			name:"Inviting...",
			wishList:wishList,
			invitedUsers:invitedUsers,
			rejects:rejects,
			myInvite:true
			
		).disable(1).onCloseIndex_(0);
		tasks[\waiting]=Task({
			var i = 0;
			loop { 
				gui[\myInvite].iconName = ("wait_" ++ (i = i+(1/16))).asSymbol;
				0.07.wait;
			};
		}).start(AppClock);
	}
	
	refresh{
		{
			if (gui[\myInvite].window.isClosed.not) {	
				gui[\myInvite].wishList_(wishList)
							.invitedUsers_(invitedUsers)
							.rejects_(rejects)
							.string_(this.inviteString)
							.refresh;
			}
		}.defer;
	}

	inviteString{
		var string="You want to start a ";
		if (network.isLAN) {
			string=string++"LAN collaboration in\nRoom: ";
			string=string++(room.name);
		}{
			string=string++(room.location);
			string=string++" collaboration in\nRoom: ";
			string=string++(room.name);
			string=string++" @ ";
			string=string++(room.address);
		};
		^string; 
	}
	
	dump{
		"Instance of LNX_MyInvite ------------".postln;
		("ID: "++id).postln;
		("Host ID: "++hostID).postln;
		("Host Name: "++hostName).postln;
		"wish list: ".postln;
		wishList.do{|p| (p.name++" ").post};
		"\ninvited users".postln;
		invitedUsers.do{|p| (p.name++" ").post};
		"\nroomList".postln;
		this.roomList.do{|p| (p+"").post};
		"\n".postln;
	}

}

// end //
