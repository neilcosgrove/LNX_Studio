
// an invite recieved from someone else ///////////////////////////////////////////////////////

// this differs from LNX_MyInvite in that rejects and invitedUsers are stored as lists of ids

LNX_Invite{

	classvar >network;

	var	<id,
		<hostID,			<hostName,
		<roomList,		<invitedUsers,
		<rejects,			<wishList,

		<lastPingTime,	<tasks,
		
		<>cancelAction,	<window,
		
		<versionMajor,	<versionMinor;
		
	*new{|inviteTX| ^super.new.init(inviteTX) }

	init{|inviteTX|
		tasks=IdentityDictionary[];
		this.update(inviteTX);
	}
	
	update{|inviteTX|
		var i;
	
		inviteTX=inviteTX.reverse;
		
		id=inviteTX.pop.asSymbol;
		hostID=inviteTX.pop.asSymbol;
		hostName=inviteTX.popS;
		
		versionMajor=inviteTX.popI;
		versionMinor=inviteTX.popI;
		
		i=inviteTX.popI;
		roomList=inviteTX.popNS(i);
		roomList[3]=roomList[3].asSymbol;
		
		wishList=IdentityDictionary[];
		i=inviteTX.popI;
		i.do{ wishList[inviteTX.pop.asSymbol]=inviteTX.popS };
		
		i=inviteTX.popI;
		invitedUsers=inviteTX.popNS(i).collect(_.asSymbol);
		
		i=inviteTX.popI;
		rejects=inviteTX.popNS(i).collect(_.asSymbol);
		
		lastPingTime=SystemClock.now;
		
		this.wasIaccepted;
		
	}
	
	wasIaccepted{
		if (tasks[\accept_invite].notNil) {
			if (invitedUsers.includes(~thisUserID)) {
				tasks[\accept_invite].stop;
				tasks[\accept_invite]=nil;
			}
		};
	}
	
	includes{|person| ^(wishList.includesKey(person.id)) }
	
	confirms{|person| ^(invitedUsers.includes(person.id)) }
	
	inviteString{
		var string="";
		string=string++hostName.asString;
		string=string++" wants to start a ";
		string=string++(roomList[3]);
		string=string++" collaboration in\nRoom: ";
		string=string++(roomList[1]);
		string=string++" @ ";
		string=string++(roomList[0]);
		^string; 
	}
	
	close{
		tasks.do(_.stop);
		{window.close}.defer;
		this.free;
	}
	
	start{
		if (tasks[\accept_invite].notNil) {
			tasks[\accept_invite].stop;
			tasks[\accept_invite]=nil;
		};
		{window.string2_("Starting...")}.defer;
	}
	
	connected{
		tasks.do(_.stop);
		{ window.close }.defer
	}
	
	createGUI{
		{
			window=LNX_InviteInterface(network.window,this.inviteString,
				[ "Decline", "Accept" ],[{
					// no
					if (tasks[\accept_invite].notNil) {
						tasks[\accept_invite].stop;
						tasks[\accept_invite]=nil;
					};
					{
						20.do{
							network.socket.sendBundle(nil,
								['decline_invite',id,network.thisUser.id]);
							0.2.wait;	
						};
					}.fork;
					tasks[\waiting].stop;
					cancelAction.value(this);
				},{
					// yes
					if (tasks[\accept_invite].isNil) {
						tasks[\accept_invite]={
							loop {
								network.socket.sendBundle(nil,
									['accept_invite',id,network.thisUser.id]);
								0.2.wait;	
							};
						}.fork(AppClock);
					};
				}],
				[true,false],
				color:Color(29/65,1,42/83)*1.5,
				background:Gradient(Color(0.4,0.4,0.4),Color(0.4,0.45,0.4)),
				w:100,
				h:30,
				name:"An Invite.",
				wishList:wishList,
				invitedUsers:invitedUsers,
				rejects:rejects
			).onCloseIndex_(0);
			tasks[\waiting]=Task({
				var i = 0;
				loop { 
					window.iconName = ("wait_" ++ (i = i+(1/16))).asSymbol;
					0.1.wait;
				};
			}).start(AppClock);

			// auto start if all below are true
			if ((network.studio.isStandalone.not)
				&& (network.autoJoin)) { window.hit };
			
		}.defer
	}
	
	refresh{
		{
			if (window.win.isClosed.not) {	
				window.wishList_(wishList)
					.invitedUsers_(invitedUsers)
					.rejects_(rejects)
					.refresh;
				window.string_(this.inviteString);
			}
		}.defer;
	}
	
	dump{
		"Instance of LNX_Invite ------------".postln;
		("ID: "++id).postln;
		("Host ID: "++hostID).postln;
		("Host Name: "++hostName).postln;
		"wish list: ".postln;
		wishList.do{|p| (p+"").post};
		"\ninvited users".postln;
		invitedUsers.do{|p| (p+"").post};
		"\nroomList".postln;
		roomList.do{|p| (p+"").post};
		"\n".postln;
	}

}

// end //
