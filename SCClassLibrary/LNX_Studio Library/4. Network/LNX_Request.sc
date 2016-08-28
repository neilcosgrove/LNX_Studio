
// group and user requests ///////////////////////////////////

LNX_Request{

	classvar >network, nextID=0, >classAPI;

	var to,theirWinName, theirText, theirCancelText, theirAcceptText, theirIcon, doClose,
	       myWinName,    myText,    myCancelText,    myAcceptText,    myIcon,
	       cancelAction, acceptAction;

	var	<id, 		<uid, 		<gui;
	var	<users, 	<yesUsers, 	<noUsers;
	var	<isSender, 	<forMe,     <tasks;

	var <requestAPI;

	// start comms 1st and register this class with it

	*initClass{
		Class.initClassTree(LNX_Protocols);
		classAPI=LNX_API.newPermanent(this,\request,#[\newIncoming]);
	}

	// a new request

	// to: (nil is all users) or an id or user or a list of ids and users
	// doClose : when the others say yes will it close their window

	*new{|to,theirWinName, theirText, theirCancelText, theirAcceptText, theirIcon, doClose,
	            myWinName,    myText,    myCancelText,    myAcceptText,    myIcon,
	            cancelAction, acceptAction|
		^super.new.init(
		     to,theirWinName,theirText,theirCancelText,theirAcceptText,theirIcon, doClose,
	 	           myWinName,   myText,   myCancelText,   myAcceptText,   myIcon,
	 	           cancelAction,acceptAction)
	}

	// init this request (the Sender)

	init{|...args|

		var uid=network.thisUser.id;

		#to,theirWinName,theirText,theirCancelText,theirAcceptText,theirIcon, doClose,
		       myWinName,   myText,   myCancelText,   myAcceptText,   myIcon,
		                                           cancelAction, acceptAction = args;

		// make an id for this request and register it
		id=(uid++"_rq_"++nextID).asSymbol;
		nextID=nextID+1;
		requestAPI=LNX_API.newMessage(this,id,#[\userSaidNo,\userSaidYes]);

		tasks=IdentityDictionary[];

		isSender=true;					// i am the sender
		forMe=true;						// and part of this request

		yesUsers=IdentityDictionary[];
		yesUsers[uid]=network.thisUser;
		noUsers=IdentityDictionary[];

		// work out who its for and make a list of users (including sender)
		if (to.isNil) {
			users=network.connectedUsers; // get all users
		}{
			users=IdentityDictionary[];
			users[uid]=network.thisUser;
			if (to.isSequenceableCollection.not) {to=[to]}; // make into a list
			to.do{|u|
				if (u.isKindOf(Symbol)) {
					users[u]=network.users[u]; // add symbols as users
				}{
					users[u.id]=u;		 	// add users as users
				};
			};
		};

		// send out the request
		classAPI.sendOD(\newIncoming,
			id,uid,theirWinName,theirText,theirCancelText,theirAcceptText,theirIcon,doClose,
		  	*users.asList.collect(_.id)
		);

		// and make the GUI
		this.createWindow(
			{ requestAPI.sendOD(\netClose); cancelAction.value(this) },
			{ requestAPI.sendOD(\netClose); acceptAction.value(this) },
			true
		);
	}

	// a user sent a response and they say "YES" :)

	userSaidYes{|uid|
		if (forMe) {
			yesUsers[uid]=network.users[uid];	  // add to yes users
			this.refresh;						  // refresh the GUI
			if ((yesUsers.size)==(users.size)) {	  // has everyonse said yes?
				if (isSender) {				  // if i am the sender
					acceptAction.value(this);	  // do the action
					{
						requestAPI.sendOD(\netClose);	  // tell everyone else to close
						this.netClose;		  // and close me
					}.defer(0.33);
				};
			};
		};
	}

	// a user sent a response and they say "NO" :(

	userSaidNo{|uid|
		if (forMe) {
			if (isSender) {cancelAction.value(this)}; // do cancel action
			noUsers[uid]=network.users[uid];
			yesUsers[uid]=nil;
			this.refresh;
		};
		{this.netClose}.defer(0.66); // close a little later so we can see who said no
	}

	// recieve a new request from anoter user

	*newIncoming{|...args| ^super.new.initIncoming(args) }

	// init an new incoming request

	initIncoming{|args|

		var ids, myID=network.thisUser.id;

		// put all the vars in
		#id,uid, myWinName,myText,myCancelText,myAcceptText,myIcon,doClose ...ids = args;

		requestAPI=LNX_API.newMessage(this,id,#['netClose']);

		isSender=false;				   // but i'm not the sender

		tasks=IdentityDictionary[];

		// am i included in this request ?
		forMe=ids.includes(myID);

		if (forMe) {

			// make the user lists
			users=IdentityDictionary[];
			yesUsers=IdentityDictionary[];
			noUsers=IdentityDictionary[];
			ids.do{|id| users[id]=network.users[id]};
			yesUsers[uid]=network.users[uid];

			myWinName=myWinName.asString;
			myText=myText.asString;
			myCancelText=(myCancelText==0).if(nil,myCancelText.asString);
			myAcceptText=(myAcceptText==0).if(nil,myAcceptText.asString);
			doClose=(doClose==0).if(false,true);

			// make the GUI
			this.createWindow(
				{
					requestAPI.sendOD(\userSaidNo,myID);  // i am saying NO
					this.netClose;
				},{
					requestAPI.sendOD(\userSaidYes,myID); // i am saying YES
					yesUsers[myID]=network.thisUser;
					this.refresh;
				},
				doClose
			);
		};

	}

	// close this request

	netClose{
		if (forMe) {
			tasks[\waiting].stop;
			tasks[\waiting]=nil;
			gui.doOnClose_(false);
			gui.close;
		};
		requestAPI.free;
		{
			to=theirWinName=theirText=theirCancelText=theirAcceptText=theirIcon=doClose=
		     myWinName=myText=myCancelText=myAcceptText=myIcon=cancelAction=acceptAction=
		     id=uid=gui=users=yesUsers=noUsers=isSender=forMe=tasks=nil;
		}.defer(1);
	}

	/////////////////////////////////////////////////////////////////////////////////////////

	// refresh the GUI

	refresh{
		{
			if (gui.win.isClosed.not) {
				gui.invitedUsers=yesUsers;
				gui.refresh;
			};
		}.defer;
	}

	// make the GUI with a LNX_InviteInterface

	createWindow{|cancelAction,acceptAction,doClose|

		var buttons=[], actions=[];

		// work out what buttons we need

		if (myCancelText.notNil) {
			buttons=buttons.add(myCancelText);
			actions=actions.add(cancelAction);
		};

		if (myAcceptText.notNil) {
			buttons=buttons.add(myAcceptText);
			actions=actions.add(acceptAction);
		};

		if (buttons.size==0) {
			buttons=["Cancel"];
			actions=[{}];
		};

		// and make it

		gui=LNX_InviteInterface(nil,myText,
				buttons,
				actions,
				doClose!(buttons.size),
				color:        Color.orange,
				background:   Gradient(Color(0.4,0.4,0.4),Color(0.6,0.6,0.4)),
				w:            100,
				h:            30,
				iconName:     myIcon,
				name:         myWinName,
				wishList:     users,
				invitedUsers: yesUsers,
				rejects:      noUsers,
				myInvite:     true
			).onCloseIndex_(0);

		if (myIcon=='wait_0') {

			tasks[\waiting]=Task({
					var i = 0;
					loop {
						gui.iconName = ("wait_" ++ (i = i+(1/16))).asSymbol;
						0.1.wait;
					};
				}).start(AppClock);
		};

	}

}

// end //
