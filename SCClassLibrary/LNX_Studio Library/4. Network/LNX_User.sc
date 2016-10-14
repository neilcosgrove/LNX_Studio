
// all users /////////////////////////////////////////////////////////////////////////////

LNX_User {

	classvar >network;

	var	<>isUser, // user could be random generated and not a real person (used in testing)
		<>isHost, // is this user currently the host. this can change. all important comms go via host

		>name,				<>netAddr,			<>id,
		<>password,		// a user password is used by oscGroups. this is just generated randomly and not that important
		<>permanentID,  // permanentID is made on 1st open

		// profile stuff
		<>description,		<>location,		<>genres,
		<>email,			<>skypeName, 	<>emailIsPublic,
		<>skypeIsPublic,	<>webPage,		<>shortName,
		<>picture,			<>pictID, 		// picture not currently used

		<editNo=0,		// edit number, times profile has been edited
		inColab=false,	// is this user currently in a colab?

		// vars used to work out common time on the network
		<commonTimePings,	<>maxLatency,
		<>delta,			<>latency,

		<>lastPingTime, // last ping time, used to time out users
		<>color,		// the colour of this user in the gui
		<>instID,		// which instrument is this user looking at

		<>isListening=true,	<>onSoloList, // is user listening to the group and their onSolo details

		// Guaranteed and Ordered delivery vars
		<>gD_msgs, <>gD_ids, <>gD_nextID, <>gD_min, <>gD_theirMin,
		<>oD_msgs          , <>oD_nextID, <>oD_min, <>oD_theirMin,

		// not currently used
		<>publicKey,		<>privteKey;

	// create this user (there should only be 1 of these and thats me)
	*thisUser{ ^super.new.init }

	// or create a new public user from a TXlist (everyone else is one of these)
	*newPublicUser{|publicList| ^super.new.initPublic(publicList) }

	// init this user
	init{
		var ip;
		var profile="userProfile".loadPref;
		var varPermanentID="permanentID".loadPref; // get my permanentID from prefs

		// if i don't already have a permanentID make 1 and save it
		if (varPermanentID.isNil) {
			permanentID=("~/".standardizePath.split[2]++"_"++(String.rand(8,8.rand)))
				.replace(" ","_").asSymbol;
			[permanentID].savePref("permanentID");
		}{
			permanentID=varPermanentID[0];
		};

		// safe mode not working at moment and maybe removed
		if (LNX_Mode.isSafe) {
			profile=(Platform.lnxResourceDir++"/PFS").loadList;
		}{
			(Platform.lnxResourceDir++"/PFS").removeFile(silent:true)
		};
		if (profile.isNil) {
			this.defaultProfile; // if i don't a profile use the default 1
		}{
			this.putLoadList(profile); // else use my profile
		};

		isUser=true;
		id=String.rand(8,8.rand).asSymbol; 			// new id issued every time lnx is opened
		password=String.rand(8,4);		   			// new pass issued every time lnx is opened, for oscgroups
		ip=Pipe.findValuesForKey("ifconfig", "inet")[1]; // use 2nd in list, for me this is normally the router
		if (ip.isNil) { ip=NetAddr.localAddr.ip };	// use local if i don't have 1
		ip = ip.replace("addr:", ""); 				// why this?
		netAddr=NetAddr.new(ip,NetAddr.localAddr.port); // this is now my net address

		commonTimePings=[]; // new empty list for pings to set up common time
		latency=0;
		maxLatency=0;		// the max latency this user has shown
		delta=0;			// this users delta to the common time clock
		color=Color.fromArray([1,1.0.rand,0.5.rand].scramble);
		this.initMsgs;		// init gd & od protol message stuff

	}

	inColab{ if (isUser) {^network.isConnected} {^inColab} } // am i in a colaboration?

	// the default profile for a new user
	defaultProfile{
		if (LNX_Mode.isSafe) {
			name="~/".standardizePath.split[2].profileSafe;
		}{
			name=LNX_Mode.username.profileSafe;
		};
		shortName=name[0..7];
		description="";
		location="";
		genres="";
		email="";
		emailIsPublic=false;
		skypeName="";
		skypeIsPublic=false;
		webPage="";
		pictID=0;
		editNo=0;
		inColab=false;
	}

	// init a new public users from a TXlist (TXlist is just what i call a transfer list for some silly reason)
	initPublic{|publicList|
		this.putPublicList(publicList);
		isUser=false;
		lastPingTime=SystemClock.now;
		commonTimePings=[];
		latency=0;
		maxLatency=0;
		delta=0;
		color=Color.fromArray([1,1.0.rand,0.5.rand].scramble);
		this.initMsgs;
	}
	name{^name.asSymbol} // a temp fix for net vs this user sorting

	// get the save list for saving to disk
	getSaveList{
		^[name,shortName,description,location,genres,
			email,emailIsPublic,skypeName,skypeIsPublic,webPage]
	}

	// save this user as the user profile for this copy of lnx_studio
	saveUserProfile{
		this.getSaveList.savePref("UserProfile")
	}

	// profile for safe mode. safe mode isn't working at the moment
	saveProfileForSafeMode{
		this.getSaveList.saveList(Platform.lnxResourceDir++"/PFS")
	}

	// put the load list for the user profile
	putLoadList{|list|
		#name,shortName,description,location,genres,
			email,emailIsPublic,skypeName,skypeIsPublic,webPage,pictID=list;
		emailIsPublic=(emailIsPublic=="true").if(true,false); // show email?
		skypeIsPublic=(skypeIsPublic=="true").if(true,false); // show skype?
	}

	// get a public version of this user for TX
	getPublicList{
		if (this.inColab!=inColab) { inColab=inColab.not; editNo=editNo+1 };
		^[id,permanentID,name,shortName,"0.0.0.0",0,description,location,genres,
			emailIsPublic.if(email,""),emailIsPublic,
			skypeIsPublic.if(skypeName,""),skypeIsPublic,webPage,pictID,this.inColab,editNo]
	}

	// get a LAN version of this profile
	getPublicListLAN{
		if (this.inColab!=inColab) { inColab=inColab.not; editNo=editNo+1 };
		^[id,permanentID,name,shortName,netAddr.ip,netAddr.port,description,location,genres,
			emailIsPublic.if(email,""),emailIsPublic,
			skypeIsPublic.if(skypeName,""),skypeIsPublic,webPage,pictID,this.inColab,editNo]
	}

	// put a public list
	putPublicList{|list|
		var addr,port;
		#id,permanentID,name,shortName,addr,port,description,location,genres,
			email,emailIsPublic,skypeName,skypeIsPublic,webPage,pictID,inColab,editNo
			=list;
		netAddr=NetAddr.new(addr.asString,port.asInt);
		id=id.asSymbol;
		permanentID   = permanentID.asSymbol;
		emailIsPublic = emailIsPublic.isTrue;
		skypeIsPublic = skypeIsPublic.isTrue;
		inColab       = inColab.isTrue;
	}

	encrypt{}
	decrypt{}

	// the display string for the profile
	displayString{
		var string="";
		[
			"Name       : ", 	name, 		"\n",
			"Short Name : ", 	shortName, 	"\n",
			"Description: ",	description, 	"\n",
			"Location   : ",	location,		"\n",
			"Genres     : ",	genres,		"\n",
			"Email      : ",	email,		"\n",
			"Skype Name : ",	skypeName,	"\n",
			"Web Page   : ",	webPage
			//"User ID     : ",	permanentID,	"\n",
		//	"IP Address  : ", 	(netAddr.ip=="0.0.0.0").if(
		//				"Public",netAddr.ip.asString++" / "++netAddr.port),"\n"
		].do{|i| string=string++(i.asString)};
		^string
	}

	fieldsString{
			^("Name       :\n"++
			  "Short Name :\n"++
			  "Description:\n"++
			  "Location   :\n"++
			  "Genres     :\n"++
			  "Email      :\n"++
			  "Skype Name :\n"++
			  "Web Page   : "
			)
	}

	field{|n| ^[name,shortName,description,location,genres,email,skypeName,webPage][n].asString }

	fields{ ^[name,shortName,description,location,genres,email,skypeName,webPage] }

	setField{|n,val|
		val=val.copy;
		switch (n)
			{0} { name=val }
			{1} { shortName=val}
			{2} { description=val}
			{3} { location=val}
			{4} { genres=val}
			{5} { email=val}
			{6} { skypeName=val}
			{7} { webPage=val};
		editNo=editNo+1;
		this.saveUserProfile;
	}

	postln { ("a LNX_User ("++shortName++" '"++id++"') ").postln;}
	printOn { arg stream; stream << ("a LNX_User ("++shortName++" '"++id++"') ") }

	// COMMON TIME /////////////////////////////////////////////////////////////

	// work out common time network stats
	returnCommonTimePing{|argDelta,argLatency|
		commonTimePings=commonTimePings.add([argDelta,argLatency]);
	}

	// compute for common time the average delta, average latency and max latency
	computeCommonTime{
		var s;
		// sort by latency
		commonTimePings.sort{|a,b| a[1]<b[1]};

		// delta
		s=(commonTimePings.size/2).ceil; // drop slowest 1/2 of pings
		delta=0;
		s.do{|i| delta=delta+(commonTimePings[i][0]) };
		delta=delta/s; // this is the average of the lowest 50%

		//latency
		latency=0;
		commonTimePings.do{|i| latency=latency+i[1] };
		latency=latency/(commonTimePings.size);

		//max latency
		if (commonTimePings[1].last>maxLatency) { maxLatency=commonTimePings[1].last };
	}

	// every time we reconnect we work out another common time so stats need clearing
	clearCommonTimeData{
		commonTimePings=[];
		latency=0;
		maxLatency=0;
		delta=0;
		this.initMsgs;
	}

	// post common time info
	ct{
		this.postln;
		"--------------".postln;
		commonTimePings.do(_.postln);
		"latency:".post;
		latency.postln;
		"maxLatency:".post;
		maxLatency.postln;
		"delta:".post;
		delta.postln;
		"".postln;
	}

	// gD & oD messaging ///////////////////////////////////////////////////
	// we keep track of the messages sent by every and any missed are requested to be sent again

	initMsgs{
		gD_msgs     = IdentityDictionary();
		gD_ids      = [];
		gD_nextID   = -1;
		gD_min      = -1;
		gD_theirMin = -1;

		oD_msgs     = IdentityDictionary();
		oD_nextID   = -1;
		oD_min      = -1;
		oD_theirMin = -1;
	}

	oD_getNextID{ ^oD_nextID=oD_nextID+1 }
	gD_getNextID{ ^gD_nextID=gD_nextID+1 }

	pingIn{ lastPingTime = SystemClock.now }

	// a random user (for testing) ////////////////////////////////

	*rand{
		var firstName;

		firstName=#[
				"al","ian","Jon","Chris","dave","Dean","scott","anne","Jake","alison","Cath",
				"cat","Dallas","simbar","JON","ross","lucy","MAL","nick","ollie","phil",
				"simon","BeKah","ben","caroline","Denzil","guy","Jacob","JanE","kenny",
				"Michelle"
			].choose;
		^this.newPublicUser([
			String.rand(8,4).asSymbol,
			(firstName++"_"++String.rand(8,4)).asSymbol,
			firstName+(#[
				"finlay","knight","haynes","Willingham","","","Goom","McConnell","cosgrove",
				"johnson","Thompson","lucas","jarrett","Ferreira","moyes","Walker","WALLACE",
				"Ursula","RydeR","locker","Cochrane","paroissien","GOMEZ","bardoe","Pryce",
				"Robinson","KING","dalgety","morris","JONES","taylor"
			].choose),
			firstName,
			"0.0.0.0",
			"0",
			#["dj","musician","producer","writter","None","Beginner","programmer"].choose,
			#["London","Brighton","Scotland","Cornwall","Berlin","New York"].choose,
			#["Acid","House","Techno","Drum & Bass","Glitch","Ambient","Funk","Dubstep",
				"Electronica","Experimental","hardcore"].choose,
			"Email@"++(String.rand(8,4)),
			#["true","false"].choose,
			"Skype_"++(String.rand(8,4)),
			#["true","false"].choose,
			"www."++firstName++"sWebPage.com",
			0
		]);

	}

}

// end //
