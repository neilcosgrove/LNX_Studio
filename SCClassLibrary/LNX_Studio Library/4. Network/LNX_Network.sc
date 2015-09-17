
// The Network ////////////////////////////////////////////////

LNX_Network {

	var	<studio,				<socket,
		<location,			<oscGroup,
		<api,			
		
		<thisUser,			<room,
		<rooms,				<inRoom=false,
		<collaboration,
		
		<tasks,				<gui,
		<window;
		
	var	<beenInCollaboration;

	// and transfer these over to collaboration or change or remove
		
	var	<myNetAddr,			<theirNetAddr,
		<pingTime,			<netClock=false;
		
	// network preferences
		
	var	<networkOnOpen, 		<serverOnOpen, 
		<serverCloseOnQuit,	<serverPort,
		<serverMaxUsers, 		<serverMaxGroups,
		<serverTimeOut,		<clientPort;
		
	// test Public on LAN
		
	var	prefWin,		prefGUI;
	
	var	<isLAN;
	
	var	<autoJoin = true;  // auto join if not standlone
		
	////////////////////////////////////////////////////////////////////
		 		
	*new {|studio| ^super.new.init(studio); }
	
	toggleLAN{
		"toggleLAN".postln;
		isLAN=isLAN.not;
		[isLAN].savePref("isLAN");
		this.disconnect;
		{ this.guiConnect }.defer(0.1);
	}
	
	init {|argStudio|

		studio=argStudio;
		this.socketAsNil;
		
		isLAN="isLAN".loadPref;
		if (isLAN.isNil) { isLAN = true } { isLAN = isLAN[0].isTrue };

		// connect this network to everything
		[
			LNX_Room, LNX_Collaboration, LNX_Invite, LNX_MyInvite,
			LNX_Protocols, LNX_Request, LNX_User, LNX_SampleBank
		].do(_.network_(this));
			
		api   = LNX_API.newPermanent(this,\network,[\netIsListening]);
		tasks = IdentityDictionary[];
		gui   = IdentityDictionary[];
		this.loadRooms;
		
		thisUser=LNX_User.thisUser;	    // make this user
	
		LNX_LANGroup.uid_(  thisUser.id); // to help stop circular messages coming back
		OscGroupClient.uid_(thisUser.id); // to help stop circular messages coming back
		LNX_ID.uid_(        thisUser.id);
	
		// short cut for debugging, remove any in code neil!
		myNetAddr=thisUser.netAddr;
		
		theirNetAddr=NetAddr.new(myNetAddr.ip,myNetAddr.port+1); // not used anymore ?
	
		//  make the default room (rooms[0][0])
		room=LNX_Room.new(*rooms[0][0])
			.addAction_{|person| collaboration.addToWishList(person) }
			.clearAction_{|id| collaboration.removeFromWishList(id) }
			.removeAction_{|id| collaboration.removeUser(id) };
		
		// and pass it to the collaboration
		collaboration=LNX_Collaboration.new(room)
			.action_{ this.initCollaboration }
			.moveAction_{|roomList,func,value| this.moveRoom(roomList,func,value)};
			
		this.startUp;
				
	}
	
	// auto open network, server etc...
	
	startUp{
		var prefList="networkPrefs".loadPref;
		if (prefList.isNil) {
			this.resetPreferences;
			this.savePreferences;
		}{
			#networkOnOpen,serverOnOpen,serverCloseOnQuit,serverPort,
						serverMaxUsers,serverMaxGroups,serverTimeOut,clientPort=prefList;
						
			clientPort=clientPort.asInt;
		};
		
		if (networkOnOpen.isTrue) { {this.guiConnect(false)}.defer(0.1) };
		if (serverOnOpen.isTrue) { this.startServer };
		OscGroupServer.serverCloseOnQuit_(serverCloseOnQuit.isTrue);
		
		beenInCollaboration="beenInCollaboration".loadPref.notNil;
		
	}
	
	startServer{
		OscGroupServer.start(serverPort,serverTimeOut,serverMaxUsers,serverMaxGroups);
	}
	
	savePreferences{
		[networkOnOpen,serverOnOpen,serverCloseOnQuit,serverPort,
			serverMaxUsers,serverMaxGroups,serverTimeOut,clientPort].savePref("networkPrefs")
	}
	
	resetPreferences{
		networkOnOpen     = false;
		serverOnOpen      = false;
		serverCloseOnQuit = false;
		serverPort        = 22242;
		serverMaxUsers    = 100;
		serverMaxGroups   = 50;
		serverTimeOut     = 60;
		clientPort        = 22242;
	}
	
	////// temp call from on gui to connect
	
	guiConnect{|openWindow=true|
		if ((this.isConnected||inRoom).not) {
			this.initPublic;
			this.createNetworkWindow(openWindow);
			room.announceRoom;
			studio.models[\network].value_(1);
			studio.models[\network].dependantsPerform(\color_,\on,Color.yellow);
		}{
			if (window.isClosed) {
				if (openWindow) { this.createNetworkWindow };
			}{
				window.front;
			};
		}
	}
		
	// disconnect network
	
	disconnect{
		this.closeNetworkWindow;
		studio.models[\network].value_(0);
		tasks.do(_.stop);
		tasks=IdentityDictionary[];
		collaboration.guiLeave;
		collaboration.close;
		room.close;
		inRoom=false;
		socket.close; // remove all responders
		this.socketAsNil;
	}
	
	// init a public network
	
	initPublic{

		inRoom=true;
		// join the public network
		this.joinPublicNetwork(
			room.address,
			thisUser.id,
			thisUser.password,
			room.name,
			room.password
		);
		
		// and start services

		room.startServices;
		collaboration.startServices;
		
	}
	
	// join the public network
	// the LAN is used for testing on 1 computer via local or internal IP address
	joinPublicNetwork{|serveraddress, username, password, groupname, grouppassword|

		if (isLAN) {
			// local: for LAN connections 
			oscGroup=LNX_LANGroup(
				serveraddress, username, password, groupname, grouppassword, clientPort);
			oscGroup.join;
			this.socketAsPublic;
			oscGroup.reportFunc_{|text| room.addText(text) };	
		}{
			// public: for internet connections 
			oscGroup=OscGroupClient(
				serveraddress, username, password, groupname, grouppassword, clientPort);
			oscGroup.join;
			this.socketAsPublic;
		};
		
		
		this.initPingResponders;
	}
	
	// ping ME! :)
	initPingResponders{
		socket.addResp('ping', {|time, resp, msg|
			msg.postln;
			socket.sendBundle(0,['returnPing', "You pinged"+thisUser.name]);
		});
		socket.addResp('returnPing', {|time, resp, msg|
			(msg.asString+"in"+((SystemClock.now-pingTime).asString)).postln;
		});
	}
	
	/// state methods
	
	thisUserID	{^thisUser.id}
	isUserConnected{|id| ^collaboration.isUserConnected(id)}
	connectedUsers{^collaboration.connectedUsers}
	users		{^collaboration.users}
	otherUsers    {^collaboration.otherUsers}
	isConnected   {^collaboration.isConnected}
	isConnecting	{^collaboration.isConnecting}
	isHost        {^collaboration.isHost}
	host          {^collaboration.host}
	c             {^collaboration} // for debugging
			
	// change sockets ////////
	
	socketAsLAN{
		location='LAN';
		socket=theirNetAddr;
		LNX_Protocols.socket_(socket);
	}
	
	socketAsPublic{
		location='Public';
		socket=oscGroup;
		LNX_Protocols.socket_(socket);
	}
	
	socketAsNil{
		location=nil;
		socket=LNX_NullSocket;
		LNX_Protocols.socket_(socket);
	}
	
	/////// rooms in network /////////////////////////////////////////////
	
	// [[],[],[],[]]
	// [0]=Home room,
	// [1]=Default rooms,
	// [2]=favourites,
	// [3]=recent rooms.
	///////////////////////////
	
	// get a stored room
	// not used yet
	// i tried to add studio1-3 to list of locations on invite but cpt returned on nil user 2nd go
	
	getRoom{|type=0,number=0| 
		if (rooms[type][number].notNil) {
			^LNX_Room.new(*rooms[type][number])
		}{
			^nil
		}	
	}
	
	getRoomList{|type=0,number=0| ^rooms[type][number] }
	
	// save the rooms
	saveRooms{
		rooms.collect{|l|[l.size,l]}.flatNoString.savePref("rooms")
	}
	
	// load rooms from disk
	loadRooms{
		var list="rooms".loadPref;
		if (list.isNil) {
			rooms=[[LNX_Room.default.getTXList],[
				LNX_Room.default.getTXList,
				["realizedsound.mooo.com", "studio1", "password", 'Public'],
				["realizedsound.mooo.com", "studio2", "password", 'Public'],
				["realizedsound.mooo.com", "studio3", "password", 'Public']
				//["127.0.0.1", "home", "password", 'Public']
			],[],[]];
		}{	
			list=list.reverse;
			rooms=[[],[],[],[]];
			4.do{|i|		
				list.popI.do{
					rooms[i]=rooms[i].add([list.popNS(3),list.pop.asSymbol].flatNoString)
				}
			}
		};
		gui[\rooms]=rooms.flatten(1);
	}
	
	// add room to the list of my rooms
	addRoom{|roomList,type=3|
		if ((rooms[0].containsRoomList(roomList).not)&&(rooms[1].containsRoomList(roomList).not)) {
			if((rooms[2].containsRoomListNotPassword(roomList))||
			 			(rooms[3].containsRoomListNotPassword(roomList))){
			  	rooms[2].do{|r,i|
			  		if (r.isSameRoomList(roomList)) { rooms[2][i]=roomList} // update password
			  	};
			  	rooms[3].do{|r,i|
			  		if (r.isSameRoomList(roomList)) { rooms[3][i]=roomList;} // update password
			  	};
			}{
			  	if ((rooms[2].containsRoomList(roomList).not)&&
			  					(rooms[3].containsRoomList(roomList).not)) {
					rooms[type]=rooms[type].add(roomList);
				};
			};
			this.saveRooms;
			this.refreshRoomMenu;
		};
	}
	
	// similar to clear history
	clearRooms{
		rooms[3]=[];
		this.saveRooms;
		this.refreshRoomMenu;
	}
	
	clearFavourites{
		rooms[2]=[];
		this.saveRooms;
		this.refreshRoomMenu;
	}
	
	// called from room text description
	gotoUserTextRoom{
		var goRoom;
		
		goRoom=[gui[\serverAddress].value,gui[\roomText].value,gui[\passwordText].value,
			(gui[\roomText].value=="home").if('Public','Private')];
		
		(rooms[0]++rooms[1]).do{|storedRoom|
			if (goRoom.isSameRoomList(storedRoom)) {goRoom=storedRoom}; // set public if needed
		};		
		
		if (room.getTXList.isSameRoomListIncPassword(goRoom).not) {
			this.moveRoom(goRoom);
		}{
			this.setToRoom(goRoom)
		};
	
	}
	
	// set text gui's of room
	setToRoom{|roomList|
		{
			if (window.isClosed.not) {
				gui[\roomText].value_(roomList@1);
				gui[\serverAddress].value_(roomList@0);
				gui[\passwordText].value_(roomList@2);
			};
		}.defer;
	}
	
	// move to room, then call func with value
	moveRoom{|roomList,func,value|
	
		if (roomList.isSameRoomListIncPassword(room.getTXList).not) {
			
			room.stop;
			socket.close;
			room.moveTo(*roomList);
			this.addRoom(roomList);
			{
				this.joinPublicNetwork(
					room.address,
					thisUser.id,
					thisUser.password,
					room.name,
					room.password
				);
				room.restart;
				this.setToRoom(room.getTXList);
				collaboration.room_(room);
				collaboration.startServices;
				func.value(value);
			}.defer(0.5);
			
		};
	}
	
	startTimeOut{
		if (this.isConnected) {
			room.occupants.do{|u| u.lastPingTime_(SystemClock.now) };
			room.tasks[\timeOutUsers].start;
		}
	}
	
	stopTimeOut {	if (this.isConnected) {room.tasks[\timeOutUsers].stop } }
	
	
	// GUI //////////////////////////////////////////////////////////////////////////
	
	// add text to the dialog in the studio window
	addTextToDialog{|text,flash=true,force=false| studio.addTextToDialog(text,flash,force) }
	
	// The main Network GUI window ////////////////////////////////////////
	
	// unpdate items in room menu
	refreshRoomMenu{
		{
			
			gui[\newRoomMenu]=["(LNX_Rooms","-"];
			gui[\newRoomFunc]=[nil,nil];
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add(
			
			//"Use Local Area Network: ["++(isLAN.if("ON","OFF"))++"]"
				isLAN.if(
				"Connecting via LAN... swap to Internet",
				"Connecting via Internet... swap to LAN"
				)
			);
			gui[\newRoomFunc]=gui[\newRoomFunc].add({this.toggleLAN});
			
			
			gui[\newRoomMenu]=gui[\newRoomMenu]++["-","(Home Room"];
			gui[\newRoomFunc]=gui[\newRoomFunc]++[nil,nil];
			
			gui[\newRoomMenu]=gui[\newRoomMenu]++(rooms[0].collect{|r|
											"  "+r[3]+": "+r[1]+"@"+r[0]});
			gui[\newRoomFunc]=gui[\newRoomFunc]++(rooms[0].collect{|r,i|
				{	
					if (this.isConnected) {collaboration.guiLeave};
					this.moveRoom(rooms[0][i]);
				}
			});
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add("-");
			gui[\newRoomFunc]=gui[\newRoomFunc].add(nil);
			gui[\newRoomMenu]=gui[\newRoomMenu].add("(Default Rooms");
			gui[\newRoomFunc]=gui[\newRoomFunc].add(nil);
			
			gui[\newRoomMenu]=gui[\newRoomMenu]++(rooms[1].collect{|r|
											"  "+r[3]+": "+r[1]+"@"+r[0]});
			gui[\newRoomFunc]=gui[\newRoomFunc]++(rooms[1].collect{|r,i|
				{	
					if (this.isConnected) {collaboration.guiLeave};
					this.moveRoom(rooms[1][i]);
				}
			});
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add("-");
			gui[\newRoomFunc]=gui[\newRoomFunc].add(nil);
			gui[\newRoomMenu]=gui[\newRoomMenu].add("(Favourites");
			gui[\newRoomFunc]=gui[\newRoomFunc].add(nil);
			
			gui[\newRoomMenu]=gui[\newRoomMenu]++(rooms[2].collect{|r|
											"  "+r[3]+": "+r[1]+"@"+r[0]});
			gui[\newRoomFunc]=gui[\newRoomFunc]++(rooms[2].collect{|r,i|
				{	
					if (this.isConnected) {collaboration.guiLeave};
					this.moveRoom(rooms[2][i]);
				}
			});
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add("-");
			gui[\newRoomFunc]=gui[\newRoomFunc].add(nil);
			gui[\newRoomMenu]=gui[\newRoomMenu].add("(Recent Rooms");
			gui[\newRoomFunc]=gui[\newRoomFunc].add(nil);
			
			gui[\newRoomMenu]=gui[\newRoomMenu]++(rooms[3].collect{|r|
											"  "+r[3]+": "+r[1]+"@"+r[0]});
			gui[\newRoomFunc]=gui[\newRoomFunc]++(rooms[3].collect{|r,i|
				{	
					if (this.isConnected) {collaboration.guiLeave};
					this.moveRoom(rooms[3][i]);
				}
			});
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add("-");
			gui[\newRoomFunc]=gui[\newRoomFunc].add(nil);
		
			gui[\newRoomMenu]=gui[\newRoomMenu].add("Set as Home Room");
			gui[\newRoomFunc]=gui[\newRoomFunc].add({this.setAsHomeRoom});
		
			gui[\newRoomMenu]=gui[\newRoomMenu].add("Add to Favourites");
			gui[\newRoomFunc]=gui[\newRoomFunc].add({this.addRoomToFavourites});
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add("-");
			gui[\newRoomFunc]=gui[\newRoomFunc].add(nil);
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add("Delete Favourites");
			gui[\newRoomFunc]=gui[\newRoomFunc].add({this.clearFavourites});
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add("Clear History");
			gui[\newRoomFunc]=gui[\newRoomFunc].add({this.clearRooms});
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add("-");
			gui[\newRoomFunc]=gui[\newRoomFunc].add(nil);
			
			gui[\newRoomMenu]=gui[\newRoomMenu].add("Network Preferences");
			gui[\newRoomFunc]=gui[\newRoomFunc].add({this.preferences});


			if (window.isClosed.not) {
				gui[\roomMenu].items_(gui[\newRoomMenu]);
			};
	
		}.defer
	}
	
	addRoomToFavourites{
		if(rooms[2].containsRoomListNotPassword(room.getTXList).not){
			rooms[2]=rooms[2].add(room.getTXList);
			this.saveRooms;
			this.refreshRoomMenu;
		};
	}
	
	setAsHomeRoom{
		rooms[0]=[room.getTXList];
		this.saveRooms;
		this.refreshRoomMenu;
	}
	
	refreshUserView{ collaboration.refreshUserView }
	
	////////////////////////
	// preferences window //
	////////////////////////
	
	preferences{
		
		prefGUI=IdentityDictionary[];
	
		prefWin=Window.new(window, 300@235);
		prefWin.view.background = Gradient(Color(0.2,0.2,0.2)*0.8,Color(0.6,0.6,0.4)*0.8);

		StaticText.new(prefWin,Rect(72+4, 5, 200, 22))
			.font_(Font("Helvetica", 16))
			.string_("Network Preferences")
			.stringColor_(Color.black);

		StaticText.new(prefWin,Rect(71+4, 4, 200, 22))
			.font_(Font("Helvetica", 16))
			.string_("Network Preferences")
			.stringColor_(Color.black);

		StaticText.new(prefWin,Rect(70+4, 3, 200, 22))
			.font_(Font("Helvetica", 16))
			.string_("Network Preferences")
			.stringColor_(Color.white);
			
		StaticText.new(prefWin,Rect(5+2, 95-73+2, 60, 22))
			.font_(Font("Helvetica",13))
			.string_("Client")
			.stringColor_(Color.black);
			
		StaticText.new(prefWin,Rect(5, 95-73, 60, 22))
			.font_(Font("Helvetica",13))
			.string_("Client")
			.stringColor_(Color.white);
			
		// line		
		UserView(prefWin,Rect(50,107-73,300-55-10+5,1))
			.background_(Color.orange*1.5/1.5).canFocus_(false);
		UserView(prefWin,Rect(51,108-73,300-55-10+5,1))
			.background_(Color.black).canFocus_(false);
			

		StaticText.new(prefWin,Rect(5, 23+4+17, 70, 22))
			.string_("On Open")
			.align_(\right)
			.stringColor_(Color.white);

		// networkOnOpen
		MVC_OnOffView(prefWin,Rect(80,46,114,17),"Remain offline","Connect to server")
			.action_{|me|
				networkOnOpen=(me.value.isTrue);
				this.savePreferences;
			}
			.font_(Font("Helvetica",11))
			.value_(networkOnOpen.isTrue.if(1,0))
			.color_(\on,Color.orange)
			.color_(\off,Color(0.4,0.4,0.4));
			
		StaticText.new(prefWin,Rect(27,70,120,22))
			.string_("In public rooms show")
			.stringColor_(Color.white);
			
		// email
		MVC_OnOffView(prefWin,Rect(152,65,42,16),"Email")
			.action_{|me|
				thisUser.emailIsPublic_(me.value.isTrue);
				thisUser.saveUserProfile;
			}
			.font_(Font("Helvetica",11))
			.value_(thisUser.emailIsPublic.if(1,0))
			.color_(\on,Color.orange)
			.color_(\off,Color(0.4,0.4,0.4));
			
		// skype
		MVC_OnOffView(prefWin,Rect(152,83,42,16),"Skype")
			.action_{|me|
				thisUser.skypeIsPublic_(me.value.isTrue);
				thisUser.saveUserProfile;
			}
			.font_(Font("Helvetica",11))
			.value_(thisUser.skypeIsPublic.if(1,0))
			.color_(\on,Color.orange)
			.color_(\off,Color(0.4,0.4,0.4));
			
		
		StaticText.new(prefWin,Rect(208-11, 25+18, 80, 22))
			.string_("Connect to :")
			.align_(\right)
			.font_(Font("Helvetica",11))
			.stringColor_(Color.white);
		
		StaticText.new(prefWin,Rect(208, 25+17+20, 30, 22))
			.string_("Port")
			.align_(\right)
			.font_(Font("Helvetica",11))
			.stringColor_(Color.white);

		// client port
		MVC_NumberBox(prefWin,Rect(245, 27+18+20, 35, 16))
			.action_{|me| 
				var value;	
				value=(me.value).asInt.clip(1,99999);
				me.value_(value);
				clientPort=value;
				this.savePreferences
			}
			.controlSpec_([1,99999,\linear,1])
			.value_(clientPort.asInt)
			.font_(Font("Helvetica",11))
			.resoultion_(2000)
			.color_(\background,Color.ndcMenuBG)
			.align_(\center);
					
		// server options ////////////////////////
		
		StaticText.new(prefWin,Rect(5+2, 95+2, 60, 22))
			.font_(Font("Helvetica",13))
			.string_("Server")
			.stringColor_(Color.black);

		StaticText.new(prefWin,Rect(5, 95, 60, 22))
			.font_(Font("Helvetica",13))
			.string_("Server")
			.stringColor_(Color.white);
			
		// line		
		UserView(prefWin,Rect(55,107,300-55-10,1))
			.background_(Color.orange*1.5/1.5).canFocus_(false);
		UserView(prefWin,Rect(56,108,300-55-10,1))
			.background_(Color.black).canFocus_(false);
			
		StaticText.new(prefWin,Rect(5, 63+55, 70, 22))
			.string_("On Open")
			.align_(\right)
			.stringColor_(Color.white);
			
		// serverOnOpen
		MVC_OnOffView(prefWin,Rect(80,120,114,17),"Don't start server","Start a server")
			.action_{|me|
				serverOnOpen=(me.value.isTrue);
				this.savePreferences;
			}
			.font_(Font("Helvetica",11))
			.value_(serverOnOpen.isTrue.if(1,0))
			.color_(\on,Color.orange)
			.color_(\off,Color(0.4,0.4,0.4));
		
		StaticText.new(prefWin,Rect(5, 83+55, 70, 22))
			.string_("On Close")
			.align_(\right)
			.stringColor_(Color.white);
			
		// serverCloseOnQuit
		MVC_OnOffView(prefWin,Rect(80,140,114,17),"Leave server running","Quit server + terminal")
			.action_{|me|
				serverCloseOnQuit=(me.value.isTrue);
				OscGroupServer.serverCloseOnQuit_(serverCloseOnQuit);
				this.savePreferences;
			}
			.font_(Font("Helvetica",11))
			.value_(serverCloseOnQuit.isTrue.if(1,0))
			.color_(\on,Color.yellow)
			.color_(\off,Color.orange);
				
		gui[\severIcon]=UserView(prefWin,Rect(190,60-8+55,110,110))
			.canFocus_( false )
			.drawFunc_{|me|
				var isRunning;
				isRunning=OscGroupServer.isRunning;
				Color.black.set;
				Pen.smoothing_(true);
				DrawIcon( \polygon, Rect(-15,-15,30+110,30+100));
				if (isRunning) {Color.green.set} {Color.red.set};
				DrawIcon( \polygon, Rect(-15,-15,30+110,30+100).insetBy(3,3));
				Color.black.set;
				DrawIcon( \polygon, Rect(-15,-15,30+110,30+100).insetBy(7,7));
				Pen.fillColor_(isRunning.if(Color.green,Color.red));
				
				Pen.font_(Font("Helvetica",11));
				
				if (isRunning) {
					Pen.stringCenteredIn("Server",Rect(0,34,110,20));
					Pen.stringCenteredIn("Is Running",Rect(0,46,110,20));
				}{
				
					Pen.stringCenteredIn("Server",Rect(0,34,110,20));
					Pen.stringCenteredIn("Not Running",Rect(0,46,110,20));
				}
				
			};
			
		StaticText.new(prefWin,Rect(5, 109-1+55, 70, 22))
			.string_("Max Users")
			.align_(\right)
			.font_(Font("Helvetica",11))
			.stringColor_(Color.white);
			
		// max users
		MVC_NumberBox(prefWin,Rect(76+4, 110+55, 35, 16))
			.action_{|me| 
				var value;	
				value=(me.value).asInt.clip(2,99999);
				me.value_(value);
				serverMaxUsers=value;
				this.savePreferences
			}
			.controlSpec_([2,99999,\linear,1])
			.value_(serverMaxUsers.asInt)
			.font_(Font("Helvetica",11))
			.resoultion_(2000)
			.color_(\background,Color.ndcMenuBG)
			.align_(\center);
			
		StaticText.new(prefWin,Rect(1+4, 109+22-1+55, 70, 22))
			.string_("Max Groups")
			.font_(Font("Helvetica",11))
			.align_(\right)
			.stringColor_(Color.white);
			
		// max groups
		MVC_NumberBox(prefWin,Rect(76+4, 110+22+55, 35, 16))
			.action_{|me| 
				var value;	
				value=(me.value).asInt.clip(1,99999);
				me.value_(value);
				serverMaxGroups=value;
				this.savePreferences
			}
			.controlSpec_([1,99999,\linear,1])
			.value_(serverMaxGroups.asInt)
			.font_(Font("Helvetica",11))
			.resoultion_(2000)
			.color_(\background,Color.ndcMenuBG)
			.align_(\center);
			
		StaticText.new(prefWin,Rect(5, 109+22+22-1+55, 70, 22))
			.string_("Timeout users")
			.align_(\right)
			.font_(Font("Helvetica",11))
			.stringColor_(Color.white);
			
		StaticText.new(prefWin,Rect(76+4+35+4, 109+22+22-1+55, 40, 22))
			.string_("(secs)")
			.font_(Font("Helvetica",11))
			.stringColor_(Color.white);
			
		// timeout users
		MVC_NumberBox(prefWin,Rect(76+4, 110+22+22+55, 35, 16))
			.action_{|me| 
				var value;	
				value=(me.value).asInt.clip(1,99999);
				me.value_(value);
				serverTimeOut=value;
				this.savePreferences
			}
			.controlSpec_([1,99999,\linear,1])
			.value_(serverTimeOut.asInt)
			.font_(Font("Helvetica",11))
			.resoultion_(2000)
			.color_(\background,Color.ndcMenuBG)
			.align_(\center);

		StaticText.new(prefWin,Rect(125, 109-1+55, 30, 22))
			.string_("Port")
			.align_(\right)
			.font_(Font("Helvetica",11))
			.stringColor_(Color.white);

		// port
		MVC_NumberBox(prefWin,Rect(159, 110+55, 35, 16))
			.action_{|me| 
				var value;	
				value=(me.value).asInt.clip(1,99999);
				me.value_(value);
				serverPort=value;
				this.savePreferences
			}
			.controlSpec_([1,99999,\linear,1])
			.value_(serverPort.asInt)
			.font_(Font("Helvetica",11))
			.resoultion_(2000)
			.color_(\background,Color.ndcMenuBG)
			.align_(\center);
					
		// start
		gui[\startButton]=Button.new(prefWin,Rect(125+19+7+5+2, 113+30-11+55, 38, 19))  
			.states_([ ["Start", Color.green, Color.grey*0.4 ],
								["Stop", Color.red, Color.grey*0.4 ]])
			.value_(OscGroupServer.isRunning.if(1,0))
			.font_(Font("Helvetica",12))
			.canFocus_(false)
			.action_{|me|
				if (me.value==1) {
					this.startServer;
				}{
				
					OscGroupServer.close;
				};
			}
			.focus;
		
		// update task	
		if (tasks[\updateGUI].isNil) {
			tasks[\updateGUI]=Task({
				loop {
					if (prefWin.isClosed) {
						tasks[\updateGUI].stop;
					}{
						gui[\severIcon].refresh
					};
					0.5.wait;	
				};
			},AppClock).start;
		}{
			tasks[\updateGUI].start;
		};
	
		// Ok
		Button.new(prefWin,Rect(240, 210, 50, 20))  
			.states_([ [ "OK", Color.black, Color.orange ]])
			.action_{	 prefWin.close }
			.focus;
			
		MVC_FlatButton(prefWin,Rect(241, 7, 45, 17),"Reset")
			.action_{	
				this.resetPreferences;
				this.savePreferences;
				prefWin.close
			}
			.font_(Font("Helvetica",11))
			.color_(\down,Color.grey/4.2)
			.color_(\up,Color.grey/2.8)
			.color_(\string,Color.red+0.3);
	
	}
	
	/////////////////////
	// network window //
	////////////////////
	
	createNetworkWindow{|toFront=true|
		
		var userIndex, dialogOutList, w,h, s;
		
		if ( (window.isNil) or: {window.isClosed } ) {

			// the window
			window=MVC_Window("Network",
						Rect(212+studio.class.osx+3,50, 525+22, 521+22+3)
					,resizable: false, border:true)
				.toFrontAction_{
					window.toFrontAction_{
						studio.frontWindow_(window);
						LNX_SplashScreen.close;	
						window.toFrontAction_{
							studio.frontWindow_(window);
						}
					};					
				}
				.create(toFront).view;
			window.view.background_(Color(0,3/77,1/103,65/77));
			
			
//			window = MVC_ModalWindow(
//				(studio.mixerWindow.isVisible).if(studio.mixerWindow.view,studio.window.view), 
//				
//				(525)@(523));
			
			
			gui[\rb] = MVC_RoundBounds(window, Rect(11,11,525,523))
				.width_(6)
				.color_(\background, Color(29/65,6/11,42/83));
			
			// the main view
			gui[\bg]=ScrollView(window, Rect(11,11,525,521-1+3))
				.background_(Color(56/77,59/77,56/77))
				.hasHorizontalScroller_(false)
				.hasVerticalScroller_(false);
				
			gui[\mainView]=gui[\bg];
							
				
			w=gui[\bg].bounds.width;
			h=gui[\bg].bounds.height;

				
			// network on/off
			MVC_IconButton(gui[\bg],Rect(493,12,25,25))
				.icon_('power')
				.action_{ this.disconnect }
				//.focusColor_(Color.grey(alpha:0.05))
				.color_(\up,Color.gray/2)
				.color_(\down,Color.gray/6)
				.color_(\iconUp,Color.green)
				.color_(\iconDown,Color.grey/2);
				
			MVC_ImageView(gui[\bg],Rect(10,7,25,35))
				.image_("lnx.jpg");
			
			s=		"          "++
					"Room"++
					"                        "++
					isLAN.if("Local Area Network", "          Server Address   ")
					++"                   "++
					isLAN.if("          My Address","Password");
			
			// Text Labels
			StaticText(gui[\bg],Rect(65,4,450,17))
				.font_(Font("Helvetica-Bold",12))
				.string_(s).align_(\left).stringColor_(Color.black);
			
			// room menu
			gui[\roomMenu]=MVC_PopUpMenu3(gui[\bg],Rect(75-30,15+6,16,16))
				.staticText_("")
				.showTick_(false)
				.color_(\background,Color.ndcMenuBG)
				.action_{|me|
					gui[\newRoomFunc][me.value].value(me.value);
					me.value_(0);
				}
				.value_(0)
				.font_(Font("Helvetica", 12));
			this.refreshRoomMenu;
				

			// room text
			gui[\roomText]=TextField.new(gui[\bg], Rect(95-30,15+6,95,16))
				.action_{|me|
					me.value_(me.value.addressSafe);
					//this.gotoUserTextRoom
				}
				.keyUpAction_{|me| me.value_(me.value.addressSafe)}
				.value_("Room")
				.background_(Color.ndcMenuBG)
				.stringColor_(Color.black)
				.align_(\center)
				.font_(Font("Helvetica",11));
					

			if (isLAN.not) {
				// @
				StaticText(gui[\bg],Rect(195-35+1,15+6+1,18,17))
					.string_("@").align_(\center).stringColor_(Color.black);
			}{
				
				// search button
				MVC_OnOffView(gui[\bg],Rect(327,21,44,16).insetBy(-2,-2),"Search")
					.rounded_(true) 
					.font_(Font("Helvetica",11))
					.color_(\on,Color(0.9,0.9,0.9))
					.color_(\off,Color(0.9,0.9,0.9))
					.action_{socket.search(gui[\ipField].value)};
						
				// ip field
				gui[\ipField]=TextField(gui[\bg], Rect(194,15+6,125,16))
					.action_{|me| socket.search(me.value) }
					.value_("")
					.background_(Color.ndcMenuBG)
					.stringColor_(Color.black)
					.align_(\center)
					.font_(Font("Helvetica",11));	
					
					
				gui[\myIP]=MVC_PopUpMenu3(socket.ipModel, gui[\bg], Rect(380,15+6,105,16) )
					.font_(Font("Helvetica",11));			
			};
				
			// server address
			gui[\serverAddress]=TextField.new(gui[\bg],
					isLAN.if(Rect(0,0,0,0), Rect(215-35,15+6,190,16))
				)
				.action_{|me|
					me.value_(me.value.addressSafe);
					//this.gotoUserTextRoom
				}
				.keyUpAction_{|me| me.value_(me.value.addressSafe)}
				.value_("Room")
				.background_(Color.ndcMenuBG)
				.stringColor_(Color.black)
				.align_(\center)
				.font_(Font("Helvetica",11));
				
			// password text
			gui[\passwordText]=TextField.new(gui[\bg],
					isLAN.if(Rect(0,0,0,0), Rect(390-15,21,85,16))
				)
				.action_{|me|	this.gotoUserTextRoom}
				.action_{|me|
					me.value_(me.value.addressSafe);
					//this.gotoUserTextRoom
				}
				.keyUpAction_{|me| me.value_(me.value.addressSafe)}
				.background_(Color.ndcMenuBG)
				.stringColor_(Color.black)
				.align_(\center)
				.font_(Font("Helvetica",11));
				
			// goto address button		
			MVC_OnOffView(gui[\bg],Rect(isLAN.if(166 ,465),21,21,16).insetBy(-2,-2),"Go")
				.rounded_(true) 
				.font_(Font("Helvetica",11))
				.color_(\on,Color(0.9,0.9,0.9))
				.color_(\off,Color(0.9,0.9,0.9))
				.action_{ this.gotoUserTextRoom };
			

			this.setToRoom(room.getTXList);
			
			room.createGUI(gui[\mainView],window,gui[\mainView]);
			collaboration.createGUI(gui[\mainView],0,20);
			
		}
					
	}
		
	closeNetworkWindow{ if ((window.isNil.not) and: {window.isClosed.not}) { window.close } }
	
	///////////////////////////////////////
	// COLLABORATION                     //
	///////////////////////////////////////
	
	// collaboration tools ///////////////////////////////////
	
	// start collaboration CALLED from LNX_Colloboration when finished
	initCollaboration{
		{
			studio.showNetworkGUI;
			studio.gui[\userDialog].clear;  // should prob move this to a studio method
			if (location=='LAN'){
				this.addTextToDialog("            You are now in a",flash:true);
				this.addTextToDialog("           LAN collaboration");
			}{
				this.addTextToDialog("            You are now in a",flash:true);
				this.addTextToDialog("          Public collaboration");
			};
			this.addTextToDialog("--------------------------------------------");
			studio.models[\network].dependantsPerform(\color_,\on,Color.green);
		}.defer;
		
		if (beenInCollaboration.not) {
			beenInCollaboration=true;
			["Delete this file to show network intro"].savePref("beenInCollaboration");
		};
		
		studio.forceInternalClock; // force internal clock
	}
	
	disconnectCollaboration{
		studio.hideNetworkGUI;
		LNX_Protocols.removeResponders;
		{studio.models[\network].dependantsPerform(\color_,\on,Color.yellow)}.defer;
	}
	
	////////////////////////////////////
	
	// sync onSolo ///////////////////////////////////////////////////////////////////////
	
	//////////////////////////////////// move this!!! and i don't need an api just for this
	
	isListening	{^(this.isConnected)and:{thisUser.isListening} }
	
	isListening_{|value|
		if (this.isConnected) {
			thisUser.isListening_(value);
			collaboration.refreshUserView;
			api.sendOD(\netIsListening,thisUser.id,value);
			studio.userHasChangedListening;
		};
	}
	
	netIsListening{|userID,value|
		this.users[userID].isListening_(value.booleanValue);
		collaboration.refreshUserView;
	}
	
}

// end //
