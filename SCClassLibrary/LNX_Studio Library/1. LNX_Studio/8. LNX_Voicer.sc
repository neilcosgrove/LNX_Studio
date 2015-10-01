////////////////////////////////////////////////////////////////////////////////////////////////////
// a VoicerNode represents a playing node //////////////////////////////////////////////////////////

LNX_VoicerNode{
	
	var <server, <note, <start, <latency, <node;
	
	*new{|server,note,start,latency| ^super.new.init(server,note,start,latency) }
	
	init{|argServer,argNote,argStart,argLatency|
		server=argServer;
		node=server.nextNodeID;
		note=argNote;
		start=argStart;
		latency=argLatency;
	}
	
	free{ server = node = note = start = latency = nil }
	
	printOn {|stream| stream << this.class.name << "(" << server << "," << note << "," <<
		start << "," << latency << ")" << " : " << node}
		
}

// a voicer: creates, controls, releases and kills VoicerNodes within a polyphony model
// FIFO unless note is already playing then it reclaims there instead 

LNX_Voicer {
	
	classvar <sampleRate, <blockSize, <minDur, <minDurL;
	var <>verbose = false;
	var <>server, <>poly=8;
	var <onNodes, <releasedNodes, <>allNodes, <>noteToNode;
	
	*new{|server| ^super.new.init(server) }
	
	init{|argSever|
		
		server        = argSever;
		LNX_Voicer.update_(server);
		onNodes       = [];
		releasedNodes = [];
		allNodes      = [];
		noteToNode	= IdentityDictionary[];
	}
	
	// get server details and update minDur & minDur
	*update_{|server|
		sampleRate    = server.sampleRate;
		blockSize     = server.options.blockSize;
		minDur        = (blockSize*19.2)/sampleRate;   // min dur before we adjust latency
		minDurL       = blockSize/sampleRate;          // dur to add to midi latency
	}
	
	// create a new note and return a LNX_VoicerNode
	noteOn{|note, velocity, latency|
		
		var voicerNode;
	
		if (verbose) { ("NoteOn:"+note).postln };
	
		note = note.asInt;
	
		this.killNote(note,latency); // stop previous of this, if any ?
		this.limitPoly(latency,1);   // and then limit poly
		
		voicerNode       = LNX_VoicerNode(server,note.asInt,SystemClock.now,latency);
		onNodes          = onNodes.add(voicerNode);
		allNodes         = allNodes.add(voicerNode);
		noteToNode[note] = voicerNode;
			
		^voicerNode;
		
	}
	
	// limit the poly
	limitPoly{|latency,size=1|
		var notes;
		if (verbose) { "Limit Poly".postln };
		if (allNodes.size>=poly) {
			notes = (releasedNodes++onNodes).collect(_.note);
			(allNodes.size+size-poly).do{|i|
				this.killNote(notes[i],latency);
			};
		};
	}

	// release all playing notes
	releaseAllNotes{|latency| allNodes.do{|node| this.releaseNote(node.note,latency)}  }
	
	// release a note
	releaseNote{|note,inLatency|
		
		var durOn, voicerNode, latency;
		
		if (verbose) { ("Release:"+note).postln };
		
		note = note.asInt;
		voicerNode = noteToNode[note];
		
		if ((voicerNode.notNil)and:{onNodes.includes(voicerNode)}) {
			
			latency = voicerNode.latency; // use the latency the note was created with
			
			// clip duration to block/sampleRate else gates might not close
			durOn = (SystemClock.now) - (voicerNode.start);
			
			if (durOn<minDur) {
				if (latency.isNil) {
					latency = minDur
				}{
					latency = latency + minDurL;
				};
			};
			
			// release it and move to released nodes
			server.sendBundle(latency,[\n_set, voicerNode.node, \gate, 0]);
	
			onNodes.remove(voicerNode);
			releasedNodes = releasedNodes.add(voicerNode);
			
		}
		
	}
	
	// kill all notes (either playing or been released)
	killAllNotes{|latency| allNodes.do{|node| this.killNote(node.note,latency)}  }
	
	// kill a note
	killNote{|note,inLatency|
		
		var durOn, voicerNode, latency;
		
		note = note.asInt;
		voicerNode = noteToNode[note];
		
		if (verbose) { ("Kill:"+note).postln };
		
		if (voicerNode.notNil) {

			latency = voicerNode.latency; // use the latency the note was created with
	
			// clip duration to block/sampleRate else gates might not close
			durOn = (SystemClock.now) - (voicerNode.start);
					
			if (durOn<minDur) {
				if (latency.isNil) {
					latency = minDur
				}{
					latency = latency + minDurL;
				};
			};
			
			// kill it
			server.sendBundle(latency,[\n_set, voicerNode.node, \gate, -1.01]);
			
			onNodes      .remove(voicerNode);
			releasedNodes.remove(voicerNode);
			allNodes     .remove(voicerNode);
			noteToNode[note] = nil;
				
		}
		
	}
	
}

