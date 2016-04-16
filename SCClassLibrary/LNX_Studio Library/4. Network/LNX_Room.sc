/*(
n.room.addTestUsers(10); // for testing
)*/
// rooms ///////////////////////////////////////////////////////////////////////////////////

LNX_Room{

	classvar >network;
	classvar <broadcastInterval=0.49;       // time between profile broadcasts
	classvar <timeOutDuration  =10;      // length of time before timing out users

	var <address, <name, <password, <location, <occupants;
	var <tasks, <gui, <window, <>addAction, <>removeAction, <>clearAction;
	var <testUsers, nudge;
	
	*broadcastNormal{ broadcastInterval=0.49 }
	*broadcastSlow  { broadcastInterval=1.49 }
	
	// make a new room
	
	*new{|address, name, password, location|
		^super.new.init(address, name, password, location)
	}

	getTXList{ ^[address, name, password, location] }
	
//	getTXList{
//		if (network.isLAN) {
//			^["LAN","Home","password",'Public']
//		}{
//			^[address, name, password, location]
//		}
//	}
	
	// the default room for LNX_Studio
	
	*default{
		^super.new.init("realizedsound.mooo.com", "home", "password", 'Public')
			// server address, userid, userpassword, groupid, grouppassword
			// or use "131.191.106.117"
	}
	
	// make a random room
	
	*random{
		^super.new.init("realizedsound.mooo.com",String.rand(8,0),String.rand(8,0),'Private')
	}
	
	init{|argAddress, argName, argPassword, argLocation|
		#address, name, password, location=[argAddress, argName, argPassword, argLocation];
		location=location.asSymbol;
		occupants=IdentityDictionary[];
		tasks=IdentityDictionary[];
		gui=IdentityDictionary[];
		nudge=IdentityDictionary[];
	}
	
	// move this room to...
	
	moveTo{|argAddress, argName, argPassword, argLocation|
		#address, name, password, location=[argAddress, argName, argPassword, argLocation];
		location=location.asSymbol;
		occupants=IdentityDictionary[];
		{this.userListUpdate}.defer(1.5);
	}
	
	// used as a little fix
	
	move{|argAddress, argName, argPassword, argLocation|
		#address, name, password, location=[argAddress, argName, argPassword, argLocation];
		location=location.asSymbol;
	}
	
	// start & stop services in this room (used in moving address + room)
	
	stop{
		this.removeResponders;
		tasks.do(_.stop);
		occupants=IdentityDictionary[];
	}
	
	restart{
		occupants=IdentityDictionary[];
		tasks=IdentityDictionary[];
		gui[\selectedUser]=nil;
		gui[\occupants]=IdentityDictionary[];
		this.broadcastProfile;
		this.initResponders;
		this.timeOutUsers;
		this.announceRoom;
	}
	
	// start all services for this room
	
	startServices{
		this.broadcastProfile;
		this.initResponders;
		this.timeOutUsers;
		this.addTestUsers(0); // for testing
	}
	
	// broadcast thisUser profile
	
	broadcastProfile{
		tasks[\broadcastProfile] = Task({
			loop { 
				network.socket.sendBundle(nil,
						['broadcastProfile',name]++(network.thisUser.getPublicList));
				//("b"+broadcastInterval).post;
				broadcastInterval.wait; // (25 broadcasts per 1 timeOut intervals)
			};
		}).start;
	}
	
	broadcastOnce{
		network.socket.sendBundle(nil,
				['broadcastProfile',name]++(network.thisUser.getPublicList));
	}

	// room responders
	
	initResponders{		
		network.socket.addResp('broadcastProfile', {|time, resp, msg|
			
			//msg.postln;
			
			if (msg[1]==(name.asSymbol)) {  // oscGroup returns as string/symbol ?
				this.recieveBroadcast(msg.drop(2));
			};
		});
		network.socket.addResp('userLeftRoom', {|time, resp, msg|
			this.removeUser(msg[1].asSymbol);
		});
		network.socket.addResp('netTalk', {|time, resp, msg|
			if (msg[1]==(name.asSymbol)) {  // oscGroup returns as string/symbol ?
				this.netTalk(msg[2]);
			}
		});
		network.socket.addResp('requestPict', {|time,resp, msg|
			"pict request recieved".postln;
		});
		network.socket.addResp('broadcastPict', {|time,resp, msg|
			"pict broadcast recieved".postln;
		});
		network.socket.addResp('nudge', {|time,resp, msg|
			{this.recieveNudge(*(msg.drop(1)));}.defer;
		});
		network.socket.addResp('netRoomMessage', {|time, resp, msg|
			this.netRoomMessage(msg[1]);
		});
	}
		
	removeResponders{
		['broadcastProfile','userLeftRoom','netTalk','requestPict',
							'broadcastPict','poke','netRoomMessage'].do{|id|
			network.socket.removeResp(id)
		};
	}
	
	// every second time out any users that have not broadcast in the last 5 secs
	
	timeOutUsers{
		tasks[\timeOutUsers] = Task({
			loop { 
				occupants.do({|person|
					if (((SystemClock.now)-(person.lastPingTime))>timeOutDuration) {
						"--".postln;
						"I am timing out ".post;person.postln;
						LNX_Protocols.now.asFormatedString(0,2).post;
						[person.shortName,person.identityHash,
							SystemClock.now,person.lastPingTime].postln;
						{this.removeUser(person.id);}.fork;
					};
				});
				1.wait;
			};
		}).start;
	}
	
	// revieve a user broadcast message
	
	recieveBroadcast{|list|
		var id;
		id=list[0].asSymbol;
		if (network.thisUser.id!=id) {
			
			{
				if ((occupants.includesKey(id)).not) {
					occupants.add(id -> LNX_User.newPublicUser(list));
					
					
					//occupants[id].shortName
					
					this.addTextViaRoom("- "++occupants[id].shortName+
						"has entered the room.",false,true);
					
					this.refresh;
					if (occupants.size==1) {
						gui[\selectedUser]=gui[\occupants][0].id;
						if (window.isClosed.not) {
							gui[\profileView].string_(gui[\occupants][0].displayString);
						};
					};
				}{
					occupants[id].pingIn;
					if (occupants[id].editNo!=list[16]) {
						occupants[id].putPublicList(list);
						if (window.isClosed.not) {
							// update profile display
							this.userListUpdate;
							if (gui[\selectedUser]==id) {
								this.updateUserProfile;
							};
						};
						network.refreshUserView;
					};
				};
			}.defer;
		}
	}
	
	// update the gui list of users
	
	userListUpdate{
		var userIndex,canSee;
		// store the users in a sorted list
		gui[\occupants]=occupants.asList.sort({|a,b|
			(a.name.asString.toLower)<(b.name.asString.toLower)}
		);
		// and the ids in the same order
		gui[\ids]=gui[\occupants].collect({|p| p.id});
		// could i see the user last time
		
		{
			if (window.isClosed.not) {
				canSee=gui[\userList].canSee;
				// update the list of users
				gui[\userList].items_(gui[\occupants].collect({|p|
					(p.inColab).if("*","")++p.name
				}));
				// find the same user again
				userIndex=gui[\ids].indexOf(gui[\selectedUser]);
				// and if still there
				if (userIndex.isNumber) {
					// move to that position
					gui[\userList].value_(userIndex,canSee);
				}
			};
			
			
			
		}.defer;
		
	}
	
	// remove user from room
	
	removeUser{|id|
		if (occupants[id].notNil) {
			this.addTextViaRoom("- "++occupants[id].shortName++" has left the room.",false,true);
			occupants.removeAt(id);
			removeAction.value(id);
			this.refresh;
		};
	}
	
	// does the room have person in it
	
	includes{|person| ^(occupants.includesKey(person.id)) }
	
	// refresh the gui
	
	refresh{ this.userListUpdate }
	
	// close this room
	
	close{
		network.socket.sendBundle(nil,['userLeftRoom',network.thisUser.id]);
		tasks.do(_.stop);
		this.removeResponders;
		tasks=IdentityDictionary[];
		occupants=IdentityDictionary[];
		gui=nil;
	}
	
	// send a message
	
	roomMessage{|msg|
		network.socket.sendBundle(0,['netRoomMessage',Êmsg]);
		this.addText(msg);
	}
	
	netRoomMessage{|msg| this.addText(msg.asString) }
	
	talk{|msg|
		msg=network.thisUser.shortName+":"+msg;
		network.socket.sendBundle(0,['netTalk',Êname, msg]);
		this.addTextViaRoom(msg,false,true);
	}
	
	// recieve a message
		
	netTalk{|msg|
		this.addTextViaRoom(msg.asString,false,true);
	}
	
	// send a nudge
	
	nudge{|uid|
		if ((nudge[uid].isNil)or:{((SystemClock.now-nudge[uid]))>30}) {
			network.socket.sendBundle(nil,['nudge',network.thisUser.id,uid]);
			this.addText("You have nudged"+(occupants[uid].name)++".");
		};
		nudge[uid]=SystemClock.now;
	}
	
	// recieve a nudge
	
	recieveNudge{|fromUID,toUID|
		fromUID=fromUID.asSymbol;
		toUID=toUID.asSymbol;
		if (network.thisUser.id==toUID) {
			// for me
			this.addText("You have been nudged by"+(occupants[fromUID].name)++".");
			network.guiConnect;
		}{
			// for someone else
			this.addText(
				""+(occupants[fromUID].name)+"has nudged"+(occupants[toUID].name)++"."
			);
		};	
	}
	
	// add text to the dialog
	
	addText{|msg|
		{
			msg=msg.asString;
			if (gui.notNil and:{gui[\dialogOut].notNil}) {
				gui[\dialogOut].addText(msg);
				gui[\dialogOutList]=gui[\dialogOut].dialog;
			};
			network.addTextToDialog(msg);
		}.defer;
	}
	
	// same as addText but put "[]" at the start in studio dialog
	
	addTextViaRoom{|msg,flash=false,force=false|
		{
			if (gui.notNil) {	
				msg=msg.asString;
				if (gui[\dialogOut].notNil) {
					gui[\dialogOut].addText(msg);
					network.addTextToDialog(msg,flash,force);
					gui[\dialogOutList]=gui[\dialogOut].dialog;
				};	
			}
		}.defer;
	}
	
	// announce the room in the dialog
	
	announceRoom{
		{
			gui[\dialogOutList]=[];
			gui[\dialogOut].dialog_(gui[\dialogOutList]);
			this.addText("------------------------------------------------------------");
			
			if (network.isLAN) {			
				this.addText("-    You are in "++(location.asString)++" LAN room:");
				this.addText("-    "++name);
			}{
				this.addText("-    You are in "++(location.asString)++" room:");
				this.addText("-    "++name++"@"+address);
				if (location=='Private'){
					this.addText("-    Password: "++password);
				};
			};
				
			this.addText("-    "++Date.localtime);
			this.addText("------------------------------------------------------------");
			//this.addText("");
			
			this.addText("");
			
			if ((network.isLAN.not) && (location==\Public)) {
				
//				this.addText("Sandbox is recommend in Public rooms.");
//				this.addText("");
			};
			
			if (name=="home") {
							
				this.addText("This is the \"Home\" room, which functions as a general meeting place.");
				this.addText("");
			
				if (network.beenInCollaboration.not) {
			
					{
						this.addText("A list of users in this room is on the left.");
						//this.addText("");
					}.defer(1.5);
					{
						this.addText(
							"You can edit your own profile in the tab \"My Profile\" below.");
						//this.addText("");
					}.defer(3);
					{
						this.addText(
	"Invite people to a collaboration in a room with the \"Add\" and \"Invite\" buttons at the bottom.");
						//this.addText("");
					}.defer(4.5);
					
				};
			
			};
			
		}.defer;
	}
	
	// make the gui
	
	createGUI{|argWindow,topWindow,topView|
	
		var userIndex, dialogOutList, col;
	
		window=argWindow;
		
		// get last user index if gui was open
		if (gui.notNil) {
			dialogOutList=gui[\dialogOutList]?[];
			if (gui[\selectedUser].notNil) {
				userIndex=gui[\ids].indexOf(gui[\selectedUser]); // find user before init gui
			}
		}{
			gui=IdentityDictionary[]; // else new gui
		};
	
		// user list
		gui[\userList] = MVC_ListView(window,Rect(8,10+40,120,370))
			.items_([])
			.action_{|me|
				var index=me.value.wrap(0,occupants.size-1);
				if ((index.isNumber)&&(occupants.size>0)) {
					gui[\selectedUser]=gui[\occupants][index].id;
					gui[\profileView].string_(gui[\occupants][index].displayString);
				};
			}
			.actions_(\doubleClickAction, {|me|
				var index=me.value.wrap(0,occupants.size-1);
				if ((index.isNumber)&&(occupants.size>0)) {
					gui[\selectedUser]=gui[\occupants][index].id;
					gui[\profileView].string_(gui[\occupants][index].displayString);
					addAction.value(gui[\occupants][index]);
					this.pressAdd;
				};
			})
			.actions_(\tripleClickAction, {|me|
				// i'm doing short cut here again
				network.collaboration.pressInvite;
			})
			.actions_(\deleteKeyAction, {|me|
				var index=me.value.wrap(0,occupants.size-1);
				if ((index.isNumber)&&(occupants.size>0)) {
					gui[\selectedUser]=gui[\occupants][index].id;
					gui[\profileView].string_(gui[\occupants][index].displayString);
					clearAction.value(gui[\occupants][index].id);
				};
			})
			.color_(\background,Color(0,0,0,0.25))
			.color_(\string,Color.black)
			.color_(\selectedString,Color.white)
			.color_(\hilite,Color(0,0,0,0.5))
			.font_(Font("Helvetica", 12))
			.fontHeight_(18);
			
		gui[\userList].actions_(\enterKeyAction,gui[\userList].actions[\doubleClickAction]);
		
		this.refresh;
		
		col=Color(0,0,0,0.35);
		
		gui[\dialogView]=TabbedView.new(window,Rect(131,11-4-2+40,389,217+15+6),
			["                                                                              ",
			 "                                      "])
			.tabPosition_    (\bottom)
			.tabCurve_(13)
			.followEdges_    (true)
			.labelColors_    ([Color(0,0,0,0),col])
			.unfocusedColors_([Color(0,0,0,0),col])
			.backgrounds_    ([col,col])
			.font_(GUI.font.new("Helvetica",11))
			.tabHeight_(15)
			.value_(0)
			.action_{|me|};	
		
		MVC_PlainSquare(window,Rect(135,10-2+40,380,155+15+5+22))
			.color_(\off,Color.black);
	
		// dialog output
		gui[\dialogOut] = MVC_DialogView(window,Rect(136, 9+40, 378, 195))
			.color_(\string,Color.black)
			.color_(\background,Color.ndcMenuBG)
			.lines_(16)
			.dialog_(dialogOutList);
			
//		MVC_PlainSquare(window,Rect(135, 209+40, 380, 16))
//			.color_(\off,Color.black);

		// user dialog input
		MVC_StaticText(window,Rect(135,209+40,380,15))
			.string_("")
			.canEdit_(true)
			.enterStopsEditing_(false)
			.enterKeyAction_{|me,string|
				this.talk(string);
				me.string_("");
			}
			.color_(\string,Color(59/77,59/77,59/77)*1.4)
			.color_(\edit,Color(59/77,59/77,59/77)*1.4)
			.color_(\background,Color(0.14,0.12,0.11)*0.4)
			.color_(\focus,Color(1,1,1,0.4))
			.color_(\editBackground, Color(0,0,0,0.7))
			.color_(\cursor,Color.white)
			.font_(Font.new("Helvetica", 13));
			
			
		// Room Dialog text
		SCStaticText(window,Rect(404, 226+40, 100, 18))
				.string_(" Room").align_(\center).stringColor_(Color.black)
				.font_(GUI.font.new("Helvetica",12));
			
		col=Color(0.1,0.1,0.18,0.5);
			
		gui[\tabView]=TabbedView.new(window,Rect(131,231+40,389,151),
												["<  Profile","My Profile"])
			.tabPosition_    (\top)
			.followEdges_    (true)
			.labelColors_    ([Color(0.1,0.1,0.2,0.5),col])
			.unfocusedColors_([Color(0,0,0,0.5),Color(0,0,0,0.5)])
			.backgrounds_    ([Color(0.1,0.1,0.2,0.5),col])
			.font_(GUI.font.new("Helvetica",12))
			.tabHeight_(15)
			.value_(0)
			.tabWidth_(125)
			.tabCurve_(13)
			.action_{|me|};	

		// profile view
		gui[\profileView] = SCTextView(gui[\tabView].views[0],Rect(4,4,381,128))
			.editable_(false)
			.background_(Color(0.267,0.282,0.267))
			.stringColor_(Color.white)
			.font_(Font("Monaco",12));
		
		// and select if still there
		if (userIndex.isNumber) {
			// move position if i could see them last time
			gui[\userList].value_(userIndex,true);
			gui[\selectedUser]=gui[\occupants][userIndex].id;
			gui[\profileView].string_(gui[\occupants][userIndex].displayString);
		};
		
		// my profile view
		gui[\myProfileView] = SCTextView(gui[\tabView].views[1],Rect(4,4,101,128))
			.editable_(true)
			.canFocus_(false)
			.background_(Color(0.267,0.282,0.267))
			.stringColor_(Color.white)
			.font_(Font("Monaco",12))
			.string_(network.thisUser.fieldsString);
		
		
		MVC_PlainSquare(gui[\tabView].views[1],Rect(105,4,280,(7*16)))
			.color_(\off,Color.black);
		
		// the fields of my profile
		8.do{|i|
			gui[("f"++i).asSymbol]=MVC_Text(gui[\tabView].views[1],Rect(105,(i*16)+4,280,16))
				.maxStringSize_(38)
				.shadow_(false)
				.canEdit_(true)
				.actions_(\stringAction,{|me|
					var val=me.string.profileSafe;
					if (i==1) {val=val[0..7]};
					me.string_(val);
					network.thisUser.setField(i,val);
					if (i==1) {network.refreshUserView};
				})
				.color_(\background,Color(0.267,0.282,0.267))
				.color_(\edit,Color.orange)
				.color_(\cursor,Color.white)
				.color_(\string,Color.white)
				.font_(Font("Monaco",12))
				.string_(network.thisUser.field(i));
		};
			
		MVC_OnOffView(window,Rect(8,386+40,57,30),"Add all")
			.rounded_(true) 
			.font_(Font("Helvetica-Bold",12))
			.color_(\on,Color(0.9,0.9,0.9))
			.color_(\off,Color(0.9,0.9,0.9))
			.action_{
				var val;
				if (occupants.size>0) {
					val = gui[\userList].value;
					gui[\selectedUser]=gui[\occupants][val].id;
					gui[\profileView].string_(gui[\occupants][val].displayString);
					occupants.do{|p| addAction.value(p)}
				};	
			};
			
		gui[\add]=MVC_OnOffView(window,Rect(71,386+40,57,30),"Add -->")
			.rounded_(true) 
			.font_(Font("Helvetica-Bold",12))
			.color_(\on,Color(0.9,0.9,0.9))
			.color_(\off,Color(0.9,0.9,0.9))
			.action_{
				var val;
				if (occupants.size>0) {
					val = gui[\userList].value;
					gui[\selectedUser]=gui[\occupants][val].id;
					gui[\profileView].string_(gui[\occupants][val].displayString);
					addAction.value(gui[\occupants][val]);
				};
			};
			
			
		// delete all
		MVC_OnOffView(window,Rect(71,424+40,57,20).insetBy(8,-1),"Clear")
			.rounded_(true) 
			.font_(Font("Helvetica-Bold",10))
			.color_(\on,Color(0.9,0.9,0.9))
			.color_(\off,Color(0.9,0.9,0.9))
			.action_{
				occupants.do{|p| clearAction.value(p.id)}
			};
			
		// nudge
		MVC_OnOffView(window,Rect(8,424+40,57,20).insetBy(8,-1),"Nudge")
			.rounded_(true) 
			.font_(Font("Helvetica-Bold",10))
			.color_(\on,Color(0.9,0.9,0.9))
			.color_(\off,Color(0.9,0.9,0.9))
			.action_{
				var val,uid;
				if (occupants.size>0) {
					val = gui[\userList].value;
					uid = gui[\occupants][val].id;
					this.nudge(uid);
				};
			};
				
//		// Safe Mode
//		if (LNX_Studio.isStandalone && LNX_Mode.isSafe) {
//			MVC_OnOffView(window,Rect(20, 453+40, 100, 20),"Sandbox: ON")
//				.rounded_(true)  
//				.color_(\on,Color.black)
//				.color_(\string,Color.green)
//				.color_(\off,Color.black)
//				.action_{	}	
//		}{
//			MVC_OnOffView(window,Rect(40, 453+40, 60, 20),"Sandbox")
//				.rounded_(true)  
//				.color_(\on,Color(0.2,1,0.2))
//				.color_(\off,Color(0.2,1,0.2))
//				.action_{	 LNX_Mode.makeGUI(topWindow,{
//					network.thisUser.saveProfileForSafeMode
//				}) }
//		};
				
	}
	
	pressAdd{
		gui[\add].down_(true);
		{gui[\add].down_(false);}.defer(0.225)
	}
	
	addViaInvite{
		var val;
		if (occupants.size>0) {
			val = gui[\userList].value;
			gui[\selectedUser]=gui[\occupants][val].id;
			gui[\profileView].string_(gui[\occupants][val].displayString);
			addAction.value(gui[\occupants][val]);
		};
		this.pressAdd;
	}
	
	updateUserProfile{
		var val;
		if (occupants.size>0) {
			val = gui[\userList].value;
			gui[\selectedUser]=gui[\occupants][val].id;
			gui[\profileView].string_(gui[\occupants][val].displayString);
		}	
	}
	
	//
	
	////////////////////////////////////////////////////////////////////////////////////
	
	// make users for testing
	
	addTestUsers{|i|
		var firstName;
		testUsers=i.collect{LNX_User.rand};
		testUsers.do({|user|
			tasks.add((user.id) -> Task({
				var msg, message=['broadcastProfile']++(user.getPublicList);
				loop { 
						(2.7.rand+2.7).wait;
						network.socket.sendBundle(nil,message);
						this.recieveBroadcast(	
							message.drop(1).collect({|i|
								(i.class==String).if(i.asSymbol,i) // why do i need to do this
							})
						);
						
						if (0.01.coin) {
							{
								msg=user.shortName+":"+(["hello ?","lets make some music",
									"i want some fun", "what do i do?",
									"who wants to make some music?", "acid anyone?",
									"techno techno", "bored now...", "lets DANCE", ""
								].choose);
								network.socket.sendBundle(0,['netTalk',Êmsg]);
								gui[\dialogOut].addText(msg);
								gui[\dialogOutList]=gui[\dialogOut].dialog;
							}.defer;
						};
				  	};
				}).start;
			);
		});
	}
	
	postln { ("a LNX_Room ("+address+name+password+location+") ").postln;}	
	printOn { arg stream; stream << ("a LNX_Room ("+address+name+password+location+") ")}

}

// some convenient tests 

+ SequenceableCollection {

	containsRoomList{|rL|
		this.do{|r|
			if ((r[0]==rL[0])and:{r[1]==rL[1]}and:{r[2]==rL[2]}) {
				^true
			};
		};
		^false;
	}
	
	containsRoomListNotPassword{|rL|
		this.do{|r|
			if ((r[0]==rL[0])and:{r[1]==rL[1]}) {
				^true
			};
		};
		^false;
	}
	
	isSameRoomList{|rL|
		var r=this;
		if ((r[0]==rL[0])and:{r[1]==rL[1]}) {^true}{^false}
	}
	
	isSameRoomListIncPassword{|rL|
		var r=this;
		if ((r[0]==rL[0])and:{r[1]==rL[1]}and:{r[2]==rL[2]}) {^true}{^false}
	}
	
}

// end //
