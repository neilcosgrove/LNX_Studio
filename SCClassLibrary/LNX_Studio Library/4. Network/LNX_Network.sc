// The Network ////////////////////////////////////////////////////////////////////////////
/*

manages the network, its user interface & the network objects below...

LNX_User : A network user + profile.
LNX_Room : Places people meet to find each other.
LNX_Collaboration : Start & maintain a collection of users in a collaboration. (also common time).
LNX_Invite : An invite to start a collaboration from someone else
LNX_MyInvite : My invite to other to join me in a collaboration
LNX_Request : Are things all users need to agree to, like... deleting an instrument, closing a song etc

LNX_API : Create an API for an object & expose chosen methods to the network + access its different protocols
LNX_Protocols : Different ways of sending messsages. Normal, Guaranteed, Ordered, Large Clumped Lists.

LNX_LANGroup : The network socket for LAN connections
OscGroupClient : The network socket for OscGroup client connections
OscGroupServer : Opening and managing your own OscGroup servers

*/

LNX_Network {

	// main vars
	var	<studio,			<socket,			<location,
		<api,				<thisUser,			<room,			<rooms,
		<inRoom=false,		<collaboration,		<tasks,			<gui,
		<window,			<beenInCollaboration;

	// and transfer these over to collaboration or change or remove
	var	<myNetAddr,			<theirNetAddr,
		<pingTime,			<netClock=false;

	// network preferences
	var	<networkOnOpen, 	<serverOnOpen,
		<serverCloseOnQuit,	<serverPort,
		<serverMaxUsers, 	<serverMaxGroups,
		<serverTimeOut,		<clientPort;

	// test Public on LAN
	var	prefWin,			prefGUI;
	var	<isLAN, 			<autoJoin = true;  // auto join if not standlone

	////////////////////////////////////////////////////////////////////

	// LNX_Network is used like a singleton so an instance is not really needed
	*new {|studio| ^super.new.init(studio); }

	// toggle between LAN and internet sockets
	toggleLAN{
		isLAN=isLAN.not;
		[isLAN].savePref("isLAN");		// save to prefs
		this.disconnect; 				// leave current connection
		{ this.guiConnect }.defer(0.1); // and start new one
	}

	// init the network
	init{|argStudio|
		var prefList;

		studio=argStudio;
		this.socketAsNil; // uses a LNX_NullSocket to absorb any method calls not needed when offline

		isLAN="isLAN".loadPref;
		if (isLAN.isNil) { isLAN = true } { isLAN = isLAN[0].isTrue }; // use LAN by default else use prefs

		// connect this network to all these classes so they refer to the network later
		[LNX_Room, LNX_Collaboration, LNX_Invite, LNX_MyInvite, LNX_Protocols, LNX_Request,
			LNX_User, LNX_SampleBank ].do(_.network_(this));

		api   = LNX_API.newPermanent(this,\network,[\netIsListening]); // used to sync isListening (groupSong)
		tasks = IdentityDictionary[]; // tasks[\updateGUI] updates the preferences window on server running status
		gui   = IdentityDictionary[];
		this.loadRooms;				  // used mainly by oscGroups. get default, recent & favoutite rooms

		thisUser=LNX_User.thisUser;	  // make this user

		LNX_LANGroup.uid_(  thisUser.id); // to help stop circular messages coming back
		OscGroupClient.uid_(thisUser.id); // to help stop circular messages coming back
		LNX_ID.uid_(        thisUser.id); // LNX_ID creates unquie IDs for network messages, clumped list sending

		myNetAddr=thisUser.netAddr; // short cut for debugging ?

		theirNetAddr=NetAddr.new(myNetAddr.ip,myNetAddr.port+1); // not used anymore ?

		//  make the default room (which is rooms[0][0])
		room=LNX_Room.new(*rooms[0][0])
			.addAction_{|person| collaboration.addToWishList(person) } // add a user to the wish list
			.clearAction_{|id|   collaboration.removeFromWishList(id)} // remove a user from the wish list
			.removeAction_{|id|  collaboration.removeUser(id) };       // remove a user due to leaving room

		// and pass it to the collaboration
		collaboration=LNX_Collaboration.new(room) // room is added to collaboration as 1st room
			.action_{ this.initCollaboration }    // action to perform on succesful collaboration start
			.moveAction_{|roomList,func,value| this.moveRoom(roomList,func,value)}; // change the room OSCGroups

		// get network preferences.. network opens on app Open but mainly OSCGroups setting
		// onOpen, closeOnQuit, port, maxUsers, maxGroups, timeOut + client port
		prefList="networkPrefs".loadPref;
		if (prefList.isNil) {
			this.resetPreferences; // use default if not preferences file
			this.savePreferences;  // now save
		}{
			#networkOnOpen, serverOnOpen, serverCloseOnQuit, serverPort, serverMaxUsers, serverMaxGroups,
			serverTimeOut, clientPort = prefList; // put prefs in vars
			clientPort=clientPort.asInt;
		};

		if (networkOnOpen.isTrue) { {this.guiConnect(false)}.defer(0.1) }; // start network if prefs say so
		if (serverOnOpen.isTrue ) { this.startServer };                    // start OSCGroup severs if needed
		OscGroupServer.serverCloseOnQuit_(serverCloseOnQuit.isTrue);       // update server close on quit

		// has this user been in a collobration before ?
		beenInCollaboration="beenInCollaboration".loadPref.notNil;
	}

	// start OSCGroups server
	startServer{
		OscGroupServer.start(serverPort,serverTimeOut,serverMaxUsers,serverMaxGroups);
	}

	// save the network preferences
	savePreferences{
		[networkOnOpen, serverOnOpen, serverCloseOnQuit, serverPort, serverMaxUsers, serverMaxGroups,
			serverTimeOut, clientPort].savePref("networkPrefs")
	}

	// default network settings
	resetPreferences{
		networkOnOpen     = false; // LNX_Network opens when app starts
		serverOnOpen      = false; // OSCGroups sevrer opens when app opens
		serverCloseOnQuit = false; // OSCGroups server closed when app quits
		serverPort        = 22242; // OSCGroups server port
		serverMaxUsers    = 100;   // OSCGroups server max users
		serverMaxGroups   = 50;    // OSCGroups server max groups
		serverTimeOut     = 60;    // OSCGroups server time out users
		clientPort        = 22242; // OSCGroups client port
	}

	// gui call to open the network connection
	guiConnect{|openWindow=true|
		if ((this.isConnected||inRoom).not) {
			this.initNetwork;						// join a network
			this.createNetworkWindow(openWindow);	// create the network window
			room.announceRoom;						// post room details to the room dialog
			studio.models[\network].value_(1);		// the mixer window gui widget that shows network is on
			studio.models[\network].dependantsPerform(\color_,\on,Color.yellow); // and is yellow
		}{
			if (window.isClosed) {
				if (openWindow) { this.createNetworkWindow }; // open network window
			}{
				window.front; // or bring it to the front
			};
		}
	}

	// init the network
	initNetwork{
		inRoom=true;
		// join the public network
		this.joinNetwork(room.address, thisUser.id, thisUser.password, room.name, room.password);
		// and start services
		room.startServices; // boardcast my own profile and listen for others, leaving users & time out users
		collaboration.startServices; // listen for invites & timeOutInvites
	}

	// join the network using either LNX_LANGroup or OscGroupClient
	joinNetwork{|serveraddress, username, password, groupname, grouppassword|
		if (isLAN) {
			// local: for LAN connections
			socket=LNX_LANGroup(serveraddress, username, password, groupname, grouppassword, clientPort);
			location='LAN';
			LNX_Protocols.socket_(socket); // use this socket in LNX_Protocols as the method of networking
			socket.reportFunc_{|text| room.addText(text) };
		}{
			// public: for internet connections (not very reliable now)
			socket=OscGroupClient(serveraddress, username, password, groupname, grouppassword, clientPort);
			socket.join;                   // start the osc client
			location='Public';
			LNX_Protocols.socket_(socket); // use this socket in LNX_Protocols as the method of networking
		};
		this.initPingResponders; // for debugging only
	}

	// disconnect network, used by gui to close network or called on app quit
	disconnect{
		this.closeNetworkWindow;			// close the network window
		studio.models[\network].value_(0);	// turn off the mixer window gui widget that shows network is on
		tasks.do(_.stop);					// stop server watcher task
		tasks=IdentityDictionary[];
		collaboration.guiLeave;				// leave any collaboration we are in
		collaboration.close;				// and free it
		room.close;							// leave the room
		inRoom=false;						// network is no longer in any room
		socket.close; 						// remove all network responders
		this.socketAsNil;					// set network socket as LNX_NullSocket
	}

	// ping me
	initPingResponders{
		socket.addResp('ping', {|time, resp, msg|
			msg.postln;
			socket.sendBundle(0,['returnPing', "You pinged"+thisUser.name]);
		});
		socket.addResp('returnPing', {|time, resp, msg|
			(msg.asString+"in"+((SystemClock.now-pingTime).asString)).postln;
		});
	}

	/// state/info methods
	thisUserID		{^thisUser.id}
	isUserConnected	{|id| ^collaboration.isUserConnected(id)}
	connectedUsers	{^collaboration.connectedUsers}
	users			{^collaboration.users}
	otherUsers		{^collaboration.otherUsers}
	isConnected		{^collaboration.isConnected}
	isConnecting	{^collaboration.isConnecting}
	isHost			{^collaboration.isHost}
	host			{^collaboration.host}
	c				{^collaboration} // for debugging

	// a LNX_NullSocket is used when the network is closed.
	// It doesn't do anything but has the same interface as the other OSCGroups & LAN sockets
	socketAsNil{
		location=nil;
		socket=LNX_NullSocket;
		LNX_Protocols.socket_(socket);  // LNX_NullSocket is now used in LNX_Protocols
	}

	/////// rooms in network /////////////////////////////////////////////

	// [[],[],[],[]]
	// [0]=Home room,
	// [1]=Default rooms,
	// [2]=favourites,
	// [3]=recent rooms.
	///////////////////////////

	// save the rooms to preferences
	saveRooms{ rooms.collect{|l|[l.size,l]}.flatNoString.savePref("rooms") }

	// load rooms from the preferences
	loadRooms{
		var list="rooms".loadPref; // get pref list
		if (list.isNil) {          // if nil use defaults
			rooms=[[LNX_Room.default.getTXList],[
				LNX_Room.default.getTXList, // default room on 1st open
				["realizedsound.mooo.com", "studio1", "password", 'Public'], // this server prob not working now
				["realizedsound.mooo.com", "studio2", "password", 'Public'], // this server prob not working now
				["realizedsound.mooo.com", "studio3", "password", 'Public']  // this server prob not working now
				//["127.0.0.1", "home", "password", 'Public']
			],[],[]];
		}{
			list=list.reverse;
			rooms=[[],[],[],[]];
			4.do{|i|
				list.popI.do{
					rooms[i]=rooms[i].add([list.popNS(3),list.pop.asSymbol].flatNoString); // make room lists
				}
			}
		};
		gui[\rooms]=rooms.flatten(1); // for use by gui
	}

	// add room to the list of my rooms.
	// type 0=Home room, 1=Default rooms, 2=favourites, 3=recent rooms.
	addRoom{|roomList,type=3|
		if ((rooms[0].containsRoomList(roomList).not)&&(rooms[1].containsRoomList(roomList).not)) {
			if((rooms[2].containsRoomListNotPassword(roomList))|| (rooms[3].containsRoomListNotPassword(roomList))){
			  	rooms[2].do{|r,i|
			  		if (r.isSameRoomList(roomList)) { rooms[2][i]=roomList} // just update password
			  	};
			  	rooms[3].do{|r,i|
			  		if (r.isSameRoomList(roomList)) { rooms[3][i]=roomList;} // just update password
			  	};
			}{
			  	if ((rooms[2].containsRoomList(roomList).not)&& (rooms[3].containsRoomList(roomList).not)) {
					rooms[type]=rooms[type].add(roomList);  // add room to list of rooms
				};
			};
			this.saveRooms;       // save to prefs
			this.refreshRoomMenu; // update gui menu items
		};
	}

	// delete recent rooms (history)
	clearRooms{
		rooms[3]=[];        	// clear list
		this.saveRooms;			// save to prefs
		this.refreshRoomMenu;	// update gui menu items
	}

	// delete favourite rooms
	clearFavourites{
		rooms[2]=[];			// clear list
		this.saveRooms;			// save to prefs
		this.refreshRoomMenu;	// update gui menu items
	}

	// called from go button in address bar
	gotoUserTextRoom{
		var goRoom = [gui[\serverAddress].value,gui[\roomText].value,gui[\passwordText].value,
			(gui[\roomText].value=="home").if('Public','Private')];
		(rooms[0]++rooms[1]).do{|storedRoom|
			if (goRoom.isSameRoomList(storedRoom)) {goRoom=storedRoom}; // copy from home or defaults if room matches
		};
		 // if room is different than current room then move to it
		if (room.getTXList.isSameRoomListIncPassword(goRoom).not) { this.moveRoom(goRoom) };
	}

	// set text gui's of room (room, address & password)
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
		// only move if not the same room
		if (roomList.isSameRoomListIncPassword(room.getTXList).not) {
			room.stop;				// stop broadcasting profile and timing out users out in this room
			socket.close;			// close the network socket
			room.moveTo(*roomList);	// move to new room
			this.addRoom(roomList); // add new room to history
			{
				// start network in new room
				this.joinNetwork(room.address, thisUser.id, thisUser.password, room.name, room.password);
				room.restart;					// start broadcasting profile and timing out users
				this.setToRoom(room.getTXList); // update gui
				collaboration.room_(room);		// update
				collaboration.startServices;	// responders & tasks for starting a collaboration
				func.value(value);				// call func once we have moved room
			}.defer(0.5); // defer this for 0.5s.
		};
	}

	// add currebt room to favourites
	addRoomToFavourites{
		if(rooms[2].containsRoomListNotPassword(room.getTXList).not){ // if current room not in list
			rooms[2]=rooms[2].add(room.getTXList);	// add it
			this.saveRooms;							// save to prefs
			this.refreshRoomMenu;					// refresh the menu
		};
	}

	// set current room as home
	setAsHomeRoom{
		rooms[0]=[room.getTXList];	// set home as current room
		this.saveRooms;				// save to preds
		this.refreshRoomMenu;		// update menu
	}

	// used while loading a song. for large songs this can take a few seconds and we don't want to disconnect
	// users just for loading a large song
	stopTimeOut { if (this.isConnected) {room.tasks[\timeOutUsers].stop } } // stop room timing out users

	// start timing users out in room, used after a song load to start timing out users again
	startTimeOut{
		if (this.isConnected) {
			room.occupants.do{|u| u.lastPingTime_(SystemClock.now) }; // update last time we saw users as now
			room.tasks[\timeOutUsers].start; // room start timing out users
		}
	}

	// GUI //////////////////////////////////////////////////////////////////////////

	// add text to the dialog in the studio window
	addTextToDialog{|text,flash=true,force=false| studio.addTextToDialog(text,flash,force) }

	// The main Network GUI window ////////////////////////////////////////

	// unpdate items in room menu
	refreshRoomMenu{
		{
			// make a manu list and the funcs to call when those menu items are selected
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

	refreshUserView{ collaboration.refreshUserView }

	////////////////////////
	// preferences window //
	////////////////////////

	preferences{
		prefGUI=IdentityDictionary[];
		prefWin=MVC_ModalWindow(studio.mixerWindow, 320@255);
		prefWin.front;
		prefGUI[\scrollView] = prefWin.scrollView;

		prefGUI[\buttonTheme] = (
			orientation_:\horizontal,
			rounded_:	true,
			colors_: (up:Color(0.9,0.9,0.9), down:Color(0.9,0.9,0.9)/2)
		);

		MVC_Text.new(prefGUI[\scrollView],Rect(72+4, 5, 200, 22))
			.font_(Font("Helvetica", 16))
			.shadow_(false)
			.string_("Network Preferences")
			.color_(\string,Color.black);


		MVC_Text.new(prefGUI[\scrollView],Rect(5+2, 95-73+2, 60, 22))
			.font_(Font("Helvetica",13))
			.shadow_(false)
			.string_("Client")
			.color_(\string,Color.black);

		MVC_Text.new(prefGUI[\scrollView],Rect(5, 23+4+17, 70, 22))
			.string_("On Open")
			.shadow_(false)
			.align_(\right)
			.color_(\string,Color.black);

		// networkOnOpen
		MVC_OnOffView(prefGUI[\scrollView],Rect(80,46,114,17),"Remain offline","Connect to server")
			.action_{|me|
				networkOnOpen=(me.value.isTrue);
				this.savePreferences;
			}
			.font_(Font("Helvetica",11))
			.value_(networkOnOpen.isTrue.if(1,0))
			.color_(\on,Color.orange)
			.color_(\off,Color(0.4,0.4,0.4));

		MVC_Text.new(prefGUI[\scrollView],Rect(27,70,120,22))
			.shadow_(false)
			.string_("In public rooms show")
			.color_(\string,Color.black);

		// email
		MVC_OnOffView(prefGUI[\scrollView],Rect(152,65,42,16),"Email")
			.action_{|me|
				thisUser.emailIsPublic_(me.value.isTrue);
				thisUser.saveUserProfile;
			}
			.font_(Font("Helvetica",11))
			.value_(thisUser.emailIsPublic.if(1,0))
			.color_(\on,Color.orange)
			.color_(\off,Color(0.4,0.4,0.4));

		// skype
		MVC_OnOffView(prefGUI[\scrollView],Rect(152,83,42,16),"Skype")
			.action_{|me|
				thisUser.skypeIsPublic_(me.value.isTrue);
				thisUser.saveUserProfile;
			}
			.font_(Font("Helvetica",11))
			.value_(thisUser.skypeIsPublic.if(1,0))
			.color_(\on,Color.orange)
			.color_(\off,Color(0.4,0.4,0.4));


		MVC_Text.new(prefGUI[\scrollView],Rect(208-11, 25+18, 80, 22))
			.string_("Connect to :")
			.shadow_(false)
			.align_(\right)
			.font_(Font("Helvetica",11))
			.color_(\string,Color.black);

		MVC_Text.new(prefGUI[\scrollView],Rect(208, 25+17+20, 30, 22))
			.string_("Port")
			.shadow_(false)
			.align_(\right)
			.font_(Font("Helvetica",11))
			.color_(\string,Color.black);

		// client port
		MVC_NumberBox(prefGUI[\scrollView],Rect(245, 27+18+20, 35, 16))
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

		MVC_Text.new(prefGUI[\scrollView],Rect(5+2, 95+2, 60, 22))
			.font_(Font("Helvetica",13))
			.shadow_(false)
			.string_("Server")
			.color_(\string,Color.black);

		// serverOnOpen
		MVC_OnOffView(prefGUI[\scrollView],Rect(80,120,114,17),"Don't start server","Start a server")
			.action_{|me|
				serverOnOpen=(me.value.isTrue);
				this.savePreferences;
			}
			.font_(Font("Helvetica",11))
			.value_(serverOnOpen.isTrue.if(1,0))
			.color_(\on,Color.orange)
			.color_(\off,Color(0.4,0.4,0.4));

		MVC_Text.new(prefGUI[\scrollView],Rect(5, 83+55, 70, 22))
			.string_("On Close")
			.shadow_(false)
			.align_(\right)
			.color_(\string,Color.black);

		// serverCloseOnQuit
		MVC_OnOffView(prefGUI[\scrollView],Rect(80,140,114,17),"Leave server running","Quit server + terminal")
			.action_{|me|
				serverCloseOnQuit=(me.value.isTrue);
				OscGroupServer.serverCloseOnQuit_(serverCloseOnQuit);
				this.savePreferences;
			}
			.font_(Font("Helvetica",11))
			.value_(serverCloseOnQuit.isTrue.if(1,0))
			.color_(\on,Color.yellow)
			.color_(\off,Color.orange);

		gui[\severIcon]=MVC_UserView(prefGUI[\scrollView],Rect(190,60-8+55,110,110))
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

		MVC_Text.new(prefGUI[\scrollView],Rect(5, 109-1+55, 70, 22))
			.string_("Max Users")
			.shadow_(false)
			.align_(\right)
			.font_(Font("Helvetica",11))
			.color_(\string,Color.black);

		// max users
		MVC_NumberBox(prefGUI[\scrollView],Rect(76+4, 110+55, 35, 16))
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

		MVC_Text.new(prefGUI[\scrollView],Rect(1+4, 109+22-1+55, 70, 22))
			.string_("Max Groups")
			.shadow_(false)
			.font_(Font("Helvetica",11))
			.align_(\right)
			.color_(\string,Color.black);

		// max groups
		MVC_NumberBox(prefGUI[\scrollView],Rect(76+4, 110+22+55, 35, 16))
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

		MVC_Text.new(prefGUI[\scrollView],Rect(5, 109+22+22-1+55, 70, 22))
			.string_("Timeout users")
			.shadow_(false)
			.align_(\right)
			.font_(Font("Helvetica",11))
			.color_(\string,Color.black);

		MVC_Text.new(prefGUI[\scrollView],Rect(76+4+35+4, 109+22+22-1+55, 40, 22))
			.string_("(secs)")
			.shadow_(false)
			.font_(Font("Helvetica",11))
			.color_(\string,Color.black);

		// timeout users
		MVC_NumberBox(prefGUI[\scrollView],Rect(76+4, 110+22+22+55, 35, 16))
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

		MVC_Text.new(prefGUI[\scrollView],Rect(125, 109-1+55, 30, 22))
			.string_("Port")
			.shadow_(false)
			.align_(\right)
			.font_(Font("Helvetica",11))
			.color_(\string,Color.black);

		// port
		MVC_NumberBox(prefGUI[\scrollView],Rect(159, 110+55, 35, 16))
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
		gui[\startButton]=MVC_MultiOnOffView(prefGUI[\scrollView],Rect(125+19+7+5+2, 113+30-11+55, 38, 19))
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
		MVC_FlatButton(prefGUI[\scrollView],Rect(240, 210, 50, 20),"Ok", prefGUI[\buttonTheme])
			.canFocus_(true)
			.color_(\up,Color.white)
			.action_{ prefWin.close };

		MVC_FlatButton(prefGUI[\scrollView],Rect(241, 7, 45, 17),"Reset")
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
//				(studio.mixerWindow.isVisible).if(studio.mixerWindow,studio.window),
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
				.image_("lnx.png");

			s=		"          "++
					"Room"++
					"                        "++
					isLAN.if("Local Area Network", "          Server Address   ")
					++"                   "++
					isLAN.if("          My Address","Password");

			// Text Labels
			StaticText(gui[\bg],Rect(65,4,450,17))
				.font_(Font("Helvetica", 12, true))
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
			gui[\serverAddress]=TextField.new(gui[\bg], Rect(215-35,15+6,190,16))
				.visible_(isLAN.not)
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
			gui[\passwordText]=TextField.new(gui[\bg], Rect(390-15,21,85,16) )
				.visible_(isLAN.not)
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
		studio.hideNetworkGUI; // hide the gui in the mixer window
		LNX_Protocols.removeResponders; // turn of all protocol responders
		{studio.models[\network].dependantsPerform(\color_,\on,Color.yellow)}.defer; // gui yellow
	}

	// sync onSolo ///////////////////////////////////////////////////////////////////////

	isListening	{^(this.isConnected)and:{thisUser.isListening} } // am i listening to the 'group song'

	// set isListening to group song
	isListening_{|value|
		if (this.isConnected) {
			thisUser.isListening_(value);  					// set is listening to this user
			collaboration.refreshUserView; 					// update gui
			api.sendOD(\netIsListening,thisUser.id,value);	// tell everyone else if i'm listening
			studio.userHasChangedListening; 				// update onSolos
		};
	}

	netIsListening{|userID,value|
		this.users[userID].isListening_(value.booleanValue); // someone has started or stop listening to the 'group'
		collaboration.refreshUserView;						 // update gui
	}

}

// end //
