// LNX_GSRVoicer //////////////////////////////////////////////////////////////////////////////////
// the voicer for GSRythmn. Allows choking, and multiple synths/nodes for each channel
/*
	create & destory nodes within a monophony model
	each channel can have multiple nodes identified by nodeName,
		this allows me to chain synths under each channel
		i.e  osc1 --> STATIC filter --> envelope & out 
	use timeOfDeath to reduce message sends so only sends set messages while node is alive
	uses choke models as needed
*/

// is a voicer node for a synth

LNX_GSRVoicerNode{
	
	var <server, <nodeID, <bundle, <duration, <timeOfDeath, <velocity;
	
	// you need to supply the nodeID and bundle. Will not been sent until .play is called
	*new{|server, nodeID, duration, vel, bundle|
		^super.new.init(server, nodeID, duration, vel, bundle) }
	
	// store the init vars
	init{|argServer, argNodeID, argDuration, argVel, argBundle|
		server=argServer;
		nodeID=argNodeID;
		duration=argDuration;
		velocity=argVel;
		bundle=argBundle;
	}
			
	// only now play the bundle when this is called
	play{|latency,func|
		if (bundle.notNil) {
			server.sendBundle(latency, *bundle);      // send it
			bundle = nil;                             // not needed anymore
			timeOfDeath = SystemClock.now + duration; // work out time of death
			func.value;                               // call play func
			^true                                     // yes i played
		}{
			^false                                    // no i didn't play
		};	
	}

	// am i alive ( good question... are any of us really alive? )
	isAlive{ ^((timeOfDeath.notNil) and: {(SystemClock.now)<=timeOfDeath}) }
	
	// how long until this node ends
	timeRemaining{ if (this.isAlive) { ^timeOfDeath - SystemClock.now }{ ^nil } }
	
	// adjusts latency to catch new synth
	kill{|latency|
		// is playing
		if (this.isAlive) {
			if (latency.isNumber) {latency=latency+0.005};             // adjust latency
			server.sendBundle(latency,["/n_set", nodeID, \gate, -1.01]); // release
			this.free;
		};
		// is about to play
		if (bundle.notNil) { this.free };
	}
	
	// does not adjust latency
	killNow{|latency|
		if (this.isAlive) {
			[latency,"/n_set", nodeID, \gate, -1.01];
			server.sendBundle(latency,["/n_set", nodeID, \gate, -1.01]);
			this.free;
		};
	}
	
	// update a synth arg	
	setArg{|synthArg, value, latency| server.sendBundle(latency,["/n_set",nodeID,synthArg,value]) }
	
	free{ server = nodeID = bundle = nil }
	
	printOn {|stream| stream << this.class.name << "(" << server << "," << nodeID << "," <<
		bundle << ")"}
	
}

// this is the multichannel voicer for LNX_GSRVoicerNode(s)

LNX_GSRVoicer{

	var <server, <noChannels, <nodesOn, <>onKillAction, <>chokeModels;
	
	// give me a voicer
	*new { arg server=Server.default, noChannels; ^super.new.init(server,noChannels) }
	
	// store the init vars
	init{|argServer, argNoChannels|
		server      = argServer;
		noChannels  = argNoChannels;
		chokeModels = nil ! noChannels;
		nodesOn     = {IdentityDictionary[]} ! noChannels;
	}
	
	// this will only store the bundle not send it, you need to supply your own nodeID
	storeBundle{|channel, nodeName, nodeID, duration, vel ... bundle|
		nodesOn[channel][nodeName] = LNX_GSRVoicerNode(server,nodeID,duration,vel,bundle);
	}
	
	// released used when pressing pause on the clock
	releaseAll{
		noChannels.do{|c| this.killChannelNow(c)}
	}
	
	// choke other channels
	choke{|channel,latency|
		var choke = chokeModels[channel].value; // get my choke channel ID
		if ((choke.notNil)and:{choke>0}) {
			noChannels.do{|i|
				if ((i!=channel)and:{chokeModels[i].value==choke}) { // are they the same as me?
					this.killChannel(i,latency);                    // then kill them
				};
			};
		};	
	}
	
	// will add a slight delay so synth is created 1st
	killChannel{|channel,latency|
		nodesOn[channel].do(_.kill(latency));
		nodesOn[channel]=IdentityDictionary[];
		onKillAction.value(channel);
	}
	
	// kill it now! called in bang before creation to kill previous (mono!)
	killChannelNow{|channel,latency|
		nodesOn[channel].do(_.killNow(latency));
		nodesOn[channel]=IdentityDictionary[];
	}
	
	// play a channel, if beat happens evaluate func
	play{|channel,latency,func|
		nodesOn[channel].do{|node|	
			node.play(latency).if { func.value(channel,node.velocity) };
		}
	}
	
	// play all channels, if beat happens evaluate func
	playAll{|latency,func|
		noChannels.do{|i| this.play(i,latency,func) }
	}	
	
	// update a synth arg	
	setArg{|channel, nodeName, synthArg, value, latency|
		if (nodesOn[channel][nodeName].notNil){
			if (nodesOn[channel][nodeName].isAlive) {
				nodesOn[channel][nodeName].setArg(synthArg, value, latency);
			}{
				nodesOn[channel][nodeName].free;
				nodesOn[channel][nodeName]=nil;
			}
		};
	}
	
	// am i alive
	isAlive{|channel, nodeName|
		if (nodesOn[channel][nodeName].notNil){
			^nodesOn[channel][nodeName].isAlive
		}{
			^false
		}
	}
	
	// how long until this node ends
	timeRemaining{|channel, nodeName|
		if (nodesOn[channel][nodeName].notNil){
			^nodesOn[channel][nodeName].timeRemaining
		}{
			^nil
		}
	}
}

