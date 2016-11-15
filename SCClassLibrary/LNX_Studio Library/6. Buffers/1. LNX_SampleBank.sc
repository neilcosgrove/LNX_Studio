////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                                //
// LNX_SampleBank : a collection of LNX_BufferProxy(s)                                            //
//                                                                                                //
// NB. this is a collection of proxies not the proxies themselves                                 //
//                                                                                                //
////////////////////////////////////////////////////////////////////////////////////////////////////
/*

// from files (old style)
q=LNX_SampleBank(s);
q.addDialog;
q.sample(0).buffer;
q.saveDialog;  // old sytle only save name, pitch, amp & loop
q.loadDialog;
q.loadFile("/Applications/SuperCollider/sounds/808/Basic Set");
q.samples;
q.addMarker(1,100).addMarker(1,200).addMarker(1,300);
q.metaModels;
q=LNX_SampleBank(s,"/Applications/SuperCollider/sounds/808/Basic Set");
q=LNX_SampleBank(s,"sounds/808/Basic Set");
q=LNX_SampleBank(s,"sounds/808/basic cymbol test");
q=LNX_SampleBank.sampleBanks.asList.select(_.isLoading)[0]
LNX_SampleBank.sampleBanks.asList.select(_.isLoading).collect(_.title)
LNX_BufferProxy.containers.collect(_.path).postList;
q.dump
q.samples
*/

LNX_SampleBank{

	classvar >network, <sampleBanks, <masterMeta, <versionKeys;
	classvar >updateFuncs, >clipboard, >studio;
	classvar <waitingToEmptyTrash = false;
	classvar <maxNoMarkers = 1000;

	var <api, 			<id;

	var <server,		<>updateFunc,		<freed=false,
	    <samples,		<metaModels, 		<otherModels,
	    <>title="",		<isLoading=false;

	var <window, 		<guiList, 			<selectedSampleNo=0,
		<lastSynth,		<task, 				<lastModel,
		follow, 		iModel, 			>selectSampleFuncs;

	var <>window2,		<zeroBuffer,		<>speakerIcon,
		<>itemAction,	<>loadedAction,		<>selectedAction,
		<>selectMeFunc,	<>metaDataUpdateFunc;

	// init the class
	*initClass {
		sampleBanks=Set[];
		updateFuncs=Set[];       // update sample func for each gui (loading)

		// do not add to this metadata without changing the saveList
		masterMeta = (
			name:				"",     // rename sample
			pitch:				60.0,   // nearest pitch
			amp:				1,      // adjust amp
			loop:				0,      // loop sample
			start:				0,      // start frame
			end:				1,      // end frame (1 is end of sample)
			active:				1,      // on/off (mute)
			velocity: 			100,	// nearest velocity
			markers: 			[],		// list of all markers
			enabledMarkers:		[],		// all markers >start && <end   (not saved)
			disabledMarkers:	[],		// all markers <=start || >=end (not saved)
			workingMakers:      [],     // start ++ enabledMarkers ++ end
			workingDur:         [],     // and their durations
			bpm:				120,	// the bpm
			length:				64		// the length of the loop in beats
		);

		// metadata & order data saved
		versionKeys = ();
		versionKeys[1.1] = #[\active, \amp, \bpm, \end, \loop, \markers, \name, \pitch, \start, \velocity];
		versionKeys[1.2] = #[\active, \amp, \bpm, \end, \length, \loop, \markers, \name, \pitch, \start, \velocity]

	}

	// add metadata models to list and return those models
	addMetaModel{
		var metaModel = ();
		var otherModel = ();

		metaModel[\name    ] 		=        "".asModel;            // rename sample
		metaModel[\pitch   ] 		=     \midi.asModel;            // nearest pitch
		metaModel[\amp     ] 		=      \db6.asModel;            // need to change loading
		metaModel[\loop    ] 		=   \switch.asModel;            // loop sample
		metaModel[\start   ] 		= \unipolar.asModel;            // start frame
		metaModel[\end     ] 		= \unipolar.asModel.value_(1);  // end frame (1 is end of sample)
		metaModel[\active  ] 		=   \switch.asModel.value_(1);  // on/off (mute)
		metaModel[\velocity] 		= [100,[0,127]].asModel;        // nearest velocity
		metaModel[\markers ] 		= [];                           // list of markers (not a model yet)

		metaModel[\enabledMarkers ]	= [];							// all markers >start && <end   (not saved)
		metaModel[\disabledMarkers] = [];							// all markers <=start || >=end (not saved)
		metaModel[\workingMarkers ] = [];							// start ++ enabledMarkers ++ end
		metaModel[\workingDur     ] = [];							// and their durations
		metaModel[\firstMarker    ] = 0;							// and their durations

		metaModel[\bpm     ]     	= \bpm.asModel;                 // the bpm
		metaModel[\length  ]		= \length.asModel;				// the length of the loop in beats

		// network stuff
		#[\pitch,\amp,\loop,\start,\end,\active,\velocity,\bpm,\length].do{|key|
			metaModel[key].action_{|me,val,latency,send,toggle|
				this.setModelVP(this.geti(metaModel),key,val,latency,send);
				if ((key===\start)|| (key===\end)) { this.updateMarkers(this.geti(metaModel)) }; // this will sort as well
			}
		};

		otherModel[\pos]  = \bipolar.asModel.value_(-1); // -1 is not playing
		otherModel[\pos2] = \bipolar.asModel.value_(-1); // -1 is not playing

		metaModels = metaModels.add(metaModel);
		otherModels=otherModels.add(otherModel);

		^metaModel;
	}

	// used in strnagle loop
	modelValueAction_{|i,model,val,latency,send| metaModels.wrapAt(i)[model].valueAction_(val,latency,send)}

	// find index model
	geti{|metaModel| ^metaModels.indexOf(metaModel) }

	// variable parameter uses send until end value is sent using sendOD via host
	setModelVP{|index,key,value,latency,send=true|
		if (send) {
			api.sendVP(id++key++"_vp_"++index,'netSetModelVP',index,key,value);
		};
		metaDataUpdateFunc.value(this,key);		 // used in StrangeLoop
	}

	// net reciever of all the 5 methods above
	netSetModelVP{|index,key,value|
		key=key.asSymbol;
		if (metaModels[index][key]!=value) {
			metaModels[index][key].lazyValue_(value,false);
		}
	}

	// easy access data. anything you might want to know

	@			{|i| ^samples[i] }
	at			{|i| ^samples[i] }
	sample		{|i| ^samples.wrapAt(i)}
	size		{ ^samples.size}
	notEmpty    { ^(samples.size>0) }
	isEmpty		{ ^(samples.size<=0) }

	path		{|i| ^samples.wrapAt(i).path }
	paths		{|i| ^samples.collect(_.path) }
	numChannels {|i| ^samples.wrapAt(i).numChannels}
	selectedSample{ if (selectedSampleNo.notNil) { ^this.sample(selectedSampleNo) }{ ^nil }}

	metaModel	{|i| ^metaModels.wrapAt(i) }

	name		{|i| ^metaModels.wrapAt(i)[\name    ].string }
	names		{ ^metaModels.collect{|b| b.name.string}   }
	name_		{|i,string| metaModels[i][\name].string_(string) }

	pitch		{|i| ^metaModels.wrapAt(i)[\pitch   ].value }
	pitches		{ ^metaModels.collect{|b| b.pitch.value } }
	pitch_		{|i,value| metaModels[i][\pitch].value_(value) }

	amp			{|i| ^metaModels.wrapAt(i)[\amp     ].value }
	amps		{ ^metaModels.collect{|b| b.amp.value   } }
	amp_		{|i,value| metaModels[i][\amp].value_(value) }

	loop		{|i| ^metaModels.wrapAt(i)[\loop    ].value }
	loops		{ ^metaModels.collect{|b| b.loop.value    } }
	loop_		{|i,value| metaModels[i][\loop].value_(value) }

	start		{|i| ^metaModels.wrapAt(i)[\start   ].value }
	starts		{ ^metaModels.collect{|b| b.start.value} }
	start_		{|i,value| metaModels[i][\start].value_(value) }

	end			{|i| ^metaModels.wrapAt(i)[\end     ].value }
	ends		{ ^metaModels.collect{|b| b.end.value    } }
	end_		{|i,value| metaModels[i][\end].value_(value) }

	actualStart {|i| ^this.start(i).min(this.end(i)) }
	actualEnd   {|i| ^this.start(i).max(this.end(i)) }

	active		{|i| ^metaModels.wrapAt(i)[\active  ].value }
	actives		{ ^metaModels.collect{|b| b.active.value  } }
	active_		{|i,value| metaModels[i][\active].value_(value) }

	velocity	{|i| ^metaModels.wrapAt(i)[\velocity].value }
	velocities	{ ^metaModels.collect{|b| b.velocity} }
	velocity_	{|i,value| metaModels[i][\start].value_(value) }

	bpm			{|i| ^metaModels.wrapAt(i)[\bpm   ].value }
	bpms		{ ^metaModels.collect{|b| b.bpm.value } }
	bpm_		{|i,value| metaModels[i][\bpm].value_(value) }

	length		{|i| ^metaModels.wrapAt(i)[\length   ].value }
	lengths		{ ^metaModels.collect{|b| b.length.value } }
	length_		{|i,value| metaModels[i][\length].value_(value) }

	workingMarkers{|i| ^metaModels.wrapAt(i)[\workingMarkers] }
	workingDurs   {|i| ^metaModels.wrapAt(i)[\workingDur    ] }

	url			{|i| ^samples[i].url}
	urls		{|i| ^samples.collect(_.url)}

	numFrames{|i| ^this.sample(i).numFrames }

	duration{|i| ^this.sample(i).duration }

	markers { ^metaModels.collect( _.markers ) }

	addMarker{|i,pos|
		var markers = metaModels.wrapAt(i)[\markers];
		if (markers.size>=maxNoMarkers) {^this}; // no more than max markers
		markers = markers.add(pos);
		metaModels.wrapAt(i)[\markers] = markers;
		this.updateMarkers(i);
	}
	removeMarker{|i,j|
		metaModels.wrapAt(i)[\markers].removeAt(j);
	}
	removeAllMarkers{|i|
		metaModels.wrapAt(i)[\markers] = [];
	}

	guiDeleteMarker{|i,j|
		this.removeMarker(i,j);
		this.updateMarkers(i);
	}

	// update metadata on markers.
	updateMarkers{|i,updateFunc=false|
		var enabled, working;
		var j       = i.wrap(0,this.size.asInt-1);
		var markers = metaModels[j][\markers];
		var start   = this.actualStart(i);
		var end     = this.actualEnd(i);
		markers     					= markers.sort;
		metaModels[j][\markers]         = markers;
		enabled							= markers.select{|marker| (marker> start) && (marker< end) };
		metaModels[j][\enabledMarkers]  = enabled;
		metaModels[j][\disabledMarkers] = markers.select{|marker| (marker<=start) || (marker>=end) };
		working							= [start]++enabled++end;
		metaModels[j][\workingMarkers]  = working.drop(-1);  		// i may drop the drop(-1). i might need end pos
		metaModels[j][\workingDur]      = working.differentiate.drop(1);
		metaModels[j][\firstMarker]		= markers.indexOf(enabled[0]) ? 0;
		if (updateFunc) {metaDataUpdateFunc.value(this,\markers)};	// used in StrangeLoop
	}


	// music instrument (give args on desired note and sample bank will return bufNumbers & rate)
	getMusicInst{|note,root,spo,transpose,static|

		var i, sampleArray, left, right, rate, dur;

		if (this.isEmpty||isLoading) { ^[0,0,0,0,0,0,0,0] }; // drop out

		note  = note + transpose - static;		                      	// apply trans & static
		note  = (note - root) / spo * 12 + root;                        // adjust note to steps/oct
		i     = this.pitches.indexOfNearest(note);                      // find the nearest sample
		rate  = (note + (static / spo * 12) - this.pitch(i)).midiratio; // work out rate for note
		rate  = rate.round(0.0000000001);                               // round it
		sampleArray = samples[i].buffer;                                // get the bufferArray
		if (sampleArray.notNil) {
			left  = sampleArray.bufnum(0);                             // the left buffer
			right = sampleArray.bufnum(1) ? left;                      // the right buffer
			dur   = sampleArray.duration*rate;                         // the duration @ this rate
			^[left, right, rate, this.amp(i),
				this.start(i)*(sampleArray.numFrames), this.start(i), this.loop(i), dur ]
			// ^[bufL, bufR, bufRate, bufAmp, bufStartFrame, bufStartPos, bufLoop, bufDur]
		}{
			^[0,0,0,0,0,0,0,0] // drop out
		};

	}

	// sample List (as getMusicInst but for a simple list with no change to rate)
	getSampleList{|note,root,spo,transpose,static|

		var i, sampleArray, left, right, rate=1, dur;

		if (this.isEmpty||isLoading) { ^[0,0,0,0,0,0,0,0] }; // drop out

		i = (note - root).asInt.wrap(0,samples.size-1);      // find sample at index
		sampleArray = samples[i].buffer;                     // get the bufferArray
		if (sampleArray.notNil) {
			left  = sampleArray.bufnum(0);                  // the left buffer
			right = sampleArray.bufnum(1) ? left;           // the right buffer
			dur = sampleArray.duration*rate;                // the duration @ this rate
			^[left, right, rate, this.amp(i),
				this.start(i)*(sampleArray.numFrames),this.start(i),this.loop(i), dur ];
			// ^[bufL, bufR, bufRate, bufAmp, bufStartFrame, bufStartPos, bufLoop, bufDur]
		}{
			^[0,0,0,0,0,0,0,0] // drop out
		};

	}

	// init the class //////////////////////////////////////////////////////////////////

	*new {arg server=Server.default, path, apiID; ^super.new.init(server, path, apiID) }

	init{|argServer,path,apiID|
		id          = UniqueID.next; // each sampleBank has a UniqueID
		server      = argServer;
		api         = LNX_API.newTemp(this, apiID,
						#[\netAddURL, \netRemove, \netSwap, \netSetModelVP] );
		sampleBanks = sampleBanks.add(this);        // add this to the list of all banks
		this.initVars;                              // init the lists / arrays
		if (path.isString) { this.loadFile(path) }; // load
		follow = \switch.asModel;                   // the follow waveform model
		iModel = [1,[1,24,\lin,1,1]].asModel;       // not sure what this is
	}

	// bank for preview, will only hold 1 sample
	// newP uses a permanent API because the temp API is deleted on song close.
	*newP {arg server=Server.default, path, apiID; ^super.new.initP(server, path, apiID) }

	initP{|argServer,path,apiID|
		id          = UniqueID.next; // each sampleBank has a UniqueID
		server      = argServer;
		api         = LNX_API.newPermanent(this, apiID, //uses a permanent API
						#[\netAddURL, \netRemove, \netSwap, \netSetModelVP] );
		sampleBanks = sampleBanks.add(this);        // add this to the list of all banks
		this.initVars;                              // init the lists / arrays
		if (path.isString) { this.loadFile(path) }; // load
		follow = \switch.asModel;                   // the follow waveform model
		iModel = [1,[1,24,\lin,1,1]].asModel;       // not sure what this is
	}

	// init the lists / arrays
	initVars{
		samples     = [];
		metaModels  = [];
		otherModels = [];
		guiList     = [];
		selectSampleFuncs=Set[]; // select sample func for each gui
	}

	////////////// load ///////////////////

	// load a bank only from a dialog, clears what was there previously
	loadDialog{
		if (server.serverRunning) {
			Dialog.getPaths({ arg path;
				var g,l,i;
				path=path@0;
				g = File(path,"r"); /////// open
					l=[g.getLine];
					if (l[0]=="SC Sample Bank Doc v1.0") {
						loadedAction={};
						i = g.getLine;
						while ( { (i.notNil)&&(i!="*** END ***")  }, {
							l=l.add(i);
							i = g.getLine;
						});
						this.putLoadList(l,fromApp:false);
					};
				g.close; /////////////////// close
			},allowsMultiple:true);
		}
	}

	// load only a bank from a saved bank file
	loadFile{|path|
		var g,l,i;
		if (File.exists(path)) {
			g = File(path,"r"); /////// open
				l=[g.getLine];
				if (l[0]=="SC Sample Bank Doc v1.0") {
					loadedAction={ };
					i = g.getLine;
					while ( { (i.notNil)&&(i!="*** END ***")  }, {
						l=l.add(i);
						i = g.getLine;
					});
					this.putLoadList(l);
				};
			g.close; /////////////////// close
		}
	}

	// put in the load list, used by addDialog, loadDialog or loadFile
	putLoadList{|l,clear=true,fromApp=true|
		var n,p;
		l=l.reverse;
		if ((l.popS)=="SC Sample Bank Doc v1.0") {
			isLoading=true;
			if (clear) { this.removeAllSamples }; // carefull with this
			this.title_(l.popS);
			n=l.pop.asString.asFloat;
			p=[];
			n.do ({|i| p=p.add(l.pop) });
			this.loadBuffers(p,true,fromApp);  // true because we will overwrite info with load

			n.do{|i|
				//var metadata     = this.addMetadata; // add the default metadata
				var metaModel    = this.addMetaModel;
				metaModel[\name ].string_(l.popS);
				metaModel[\pitch].value_(l.popF);
				metaModel[\amp  ].value_(l.popF.ampdb);
				metaModel[\loop ].value_(l.popF);
			};
		};
	}

	// save old style ///////////////////////////////////////////////////////////////

	// save bank as a file
	saveDialog{
		CocoaDialog.savePanel({|path|
			var f,g,saveList;
			saveList=this.getSaveList;
			f = File(path,"w");  //////////// open
				title=path.basename;
				saveList.do{|i| f.write(i.asString++"\n") };
				this.title_(path.basename);
			f.close; /////////////////////// close
		});
	}

	// get the save list from the old style SampleBank
	getSaveList{
		var saveList;
		saveList=["SC Sample Bank Doc v1.0",title.asSymbol,this.size];
		this.size.do{|i|
			saveList=saveList.add(samples[i].path);
		};
		this.size.do{|i|
			saveList=saveList.add(this.name(i));
			saveList=saveList.add(this.pitch(i));
			saveList=saveList.add(this.amp(i));
			saveList=saveList.add(this.loop(i));
		};
		saveList=saveList.add("*** END ***");
		^saveList
	}

	// load old style //////////////////////////////////////////////////////////////

	// add a bank, sample or samples from a dialog
	addDialog{
		var file,list,item;
		if (server.serverRunning) {
			CocoaDialog.getPaths({ arg paths; //get soundfiles;
				file = File(paths@0,"r"); /////// open
					list=[file.getLine];
					if (list[0]=="SC Sample Bank Doc v1.0") { // add a Sample Bank
						loadedAction={};
						item = file.getLine;
						while ( { (item.notNil)&&(item!="*** END ***")  }, {
							list=list.add(item);
							item = file.getLine;
						});
						this.putLoadList(list,false,false);
						file.close; /////////////////// close
					}{
						file.close; /////////////////// close / or add samples
						isLoading=true;
						loadedAction={};
						this.loadBuffers(paths,false,false);
						updateFunc.value(this);
					};
			},{},allowsMultiple:true);
		}
	}

	// recursive addition of buffers to help reduce load on server
	loadBuffers{|paths, justSamples=false, fromApp=true|  // justSamples because of loading a bank
		var path, buffer, metadata, metaModel;

		studio.flashServerIcon; // gui

		if (paths.size>0) {

			path = paths[0];
			if (fromApp) { path=Platform.lnxResourceDir+/+path }; // add the lnx sample directory

			if (path.isSoundFile) {
				// in future i might not want 2 do above test (these are local files ie 808 only)

				buffer=LNX_BufferProxy.read(server, path, action: {

					samples = samples.add(buffer);

					if (justSamples.not) {
						//metadata        = this.addMetadata;  // add the default metadata
						metaModel       = this.addMetaModel;
						metaModel[\name].string_( path.split.pop.splitext[0].asString);
					};

					paths.removeAt(0);
					this.loadBuffers(paths, justSamples);
				});
			}{ // else.if not a sound file
				paths.removeAt(0);
				this.loadBuffers(paths, justSamples);
			};
		}{ // else.if paths.size==0
			this.finishedLoading;
			loadedAction.value(this);
			loadedAction=nil;
		}; // end.if paths.size
	}

	// URL's //////////////////////////////////////////////////////////////////////////////////

	// add a url buffer (used in LNX_WebBrowser)
	// an orderedSet of paths really

	// this is used by both load & web, need to seperate for API
	guiAddURL{|url,select=true|
		api.groupCmdOD(\netAddURL,url,select,network.thisUser.id);
	}

	// this still needs user id to go with preview
	netAddURL{|url,select=true,uid|
		// make onComplete Func
		var action = {|buf|
			var i = samples.indexOf(buf);
			if (uid.asSymbol == network.thisUser.id) {
				if (LNX_WebBrowser.preview.isTrue) {
					this.play(i);
				}; // play when finished downloading
				{
					selectMeFunc.value(i);
					// used only in scCode at mo
					if (window2.notNil) {
						selectSampleFuncs.do{|func| func.value(selectedSampleNo) }
					};
				}.defer;  // will this work with many users?
			};
			{
				this.selectSample(i,false,false);
				// used only in scCode at mo
				if (window2.notNil) {
					selectSampleFuncs.do{|func| func.value(selectedSampleNo) }
				};
			}.defer;   // will this work with many users?
		};
		this.addURL(url.asString,action,select.isTrue); // now add it
	}

	// add a url as a buffer. We maybe offline or it may not exist or not be in the cashe
	addURL{|url,action,select=true,updateBank=true|  // select sample

		var buffer, path, metadata, metaModel, alreadyExists=false;

		// check to see if it already exists in this bank
		samples.do{|buf,i|
			if (buf.url == url) {
				buffer = buf;		// the one in existence is now this buffer also
				alreadyExists = true;
				if (select) {this.selectSample(i,false,false) };
										// this should be in webBrowser maybe?
			};
		};

		// only add once to this sample bank
		if (alreadyExists.not) {
						    // why ?? here it should always happen
			buffer          = buffer ?? {LNX_BufferProxy.url(server,url,action,true)};
			path            = buffer.path;
			samples         = samples.add(buffer);
			metaModel       = this.addMetaModel;
			metaModel[\name].string_(path.basename);

			this.updateList(false,updateBank); // add to list
			if (select) {
				this.selectSample(samples.size-1,false,false); // select in...
				selectMeFunc.value(samples.size-1); // select in...
			};

			if (window.notNil) { this.addSampleWidgets(samples.size-1,select)} ; // add the widgets

		};

		if (alreadyExists) { action.value(buffer) }; // if it exists we have it and can use it now
	}

	// get the save list from the new style URL SampleBank
	getSaveListURL{
		var saveList;
		saveList=["SC URL Bank Doc v1.2",title.asSymbol,this.size];

		samples.do{|sample,i|
			saveList=saveList.add(samples[i].url);
			versionKeys[1.2].do{|key|
				if (key==\markers) {
					saveList=saveList.add( metaModels[i][key].size );
					saveList=saveList++metaModels[i][key];
				}{
					if (key==\name) {
						saveList=saveList.add( metaModels[i][key].string );
					}{
						saveList=saveList.add( metaModels[i][key].value );
					}
				}
			};
		};

		saveList=saveList.add("*** END URL Bank Doc ***");
		^saveList
	}


	// put the load list from the new style URL SampleBank
	putLoadListURL{|l,clear=true,updateBank=true|
		var n,p, version, versionString;
		l=l.reverse;

		versionString=l.popS;
		version=versionString.version;

		if (versionString[..14]=="SC URL Bank Doc") {
			isLoading=true;

			if (clear) { this.removeAllSamples }; // carefull with this

			this.title_(l.popS);
			n=l.popI;
			if ((n>0)and:{freed.not}) {
				this.recursiveLoadURL(n,0,l,version,updateBank);
			}{
				this.finishedLoading;
			};
		};

	}

	// recursive add each url sample
	recursiveLoadURL{|n,i,l,version,updateBank=true|

		var metadata, metaModel, keysToUse;

		studio.flashServerIcon; // gui

		this.addURL(l.popS,{},false,updateBank); // add this URL

		metaModel = metaModels.last;  // its the last one added in addURL

		// use version keys to load data
		if (version <= 1.1) { keysToUse = versionKeys[1.1] } { keysToUse = versionKeys[1.2] };

		// add metadata
		keysToUse.do{|key|
			if ((version==1)and:{key==\bpm}) {
				"Samplebank v1.0 bpm added".postln; // v1 didn't have bpm
			}{
				if (key==\markers) {
					var size = l.popI;                       // no of markers
					metaModel[key] = l.popNF(size);          // now pop them all
					this.updateMarkers(i);
				}{
					if (masterMeta[key].isFloat) {           // if master is float
						metaModel[key].value_(l.popF);      // pop a float
					}{
						if (masterMeta[key].isInteger) {    // if master is integer
							metaModel[key].value_(l.popF); // pop a float as well (why?)
						}{
							metaModel[key].string_(l.popS); // else pop a string
						}
					};
				};
			};
		};

		// recursive add each buffer to reduce load on cpu
		{
			if (freed.not) {
				if ((i+1)<n) {
					{
						if (freed.not) {
							this.recursiveLoadURL(n,i+1,l,version,updateBank)
						}{
							this.finishedLoading;
						};
					}.defer; // add one by one
				}{
					this.finishedLoading; // notLoading so can play now
					selectedSampleNo=0;  // what about loading???
					this.updateGUI(updateBank);	 // and update GUI
				};
			};
		}.defer( studio.isPlaying.if(0.025,0.005)); // use different rates depending on isPlaying

	}

	// this sampleBank has now finished loading
	finishedLoading{
		isLoading=false;
		if (sampleBanks.select(_.isLoading).isEmpty) {
			if (waitingToEmptyTrash) {
				LNX_BufferProxy.emptyTrash; // after everything has loaded and we need to empty
				waitingToEmptyTrash=false;
			};
			studio.stopFlashServerIcon;
		};
	}

	// empty trash
	*emptyTrash{
		if (sampleBanks.select(_.isLoading).isEmpty) {
			LNX_BufferProxy.emptyTrash;	// if no banks are loading then just empty now
		}{
			waitingToEmptyTrash=true; // else wait for them to finish 1st
		}
	}

	//////////////////////////////////////////////////////////////////////////////////

	// delete the selected sample from the bank
	deleteSelectedSample{
		if (this.size>0) {
			this.guiRemove(selectedSampleNo);
			this.selectSample(selectedSampleNo.clip(0,this.size-1),true); // for gui
			this.updateSelectedSample(selectedSampleNo);
			if (this.isEmpty) {
				selectMeFunc.value(0);
			};
		}
	}

	// gui remove item from
	guiRemove{|i|
		if(i<(this.size)) {
			api.groupCmdOD(\netRemove,i);
		}
	}

	// the net version
	netRemove{|i|
		if(i<(this.size)) {

			if (guiList.notEmpty) {
				guiList[i].do(_.free);
				guiList.removeAt(i);
				guiList.do{|gui,i| gui[\i]=i }; // aline gui index numbers
				this.adjustViews;
			};

			samples[i].freeNow;
			samples.removeAt(i);

			// do i want to free all models, this will remove gui as well.
			metaModels.removeAt(i);
			otherModels.removeAt(i);

			this.updateList(false);
		};
	}

	// remove all samples, doesn't update list because maybe loading new
	removeAllSamples{
		samples.do(_.free);
		samples     = [];
		metaModels  = [];
		otherModels = [];
	}

	clear{ this.removeAllSamples } // same thing, different name

	// free this and all the samples
	free{
		freed=true;
		api.free;
		this.removeAllSamples;
		sampleBanks.remove(this);
		if (this.isOpen) { window.parent.close };
		this.finishedLoading;
		//gui.postln.do(_.free); // this will cause a crash, should find out why at some point
	}

	// copy buffers
	copyAll{ clipboard = [ this.urls, metaModels.collect{|c| c.collect(_.value)} ] }

	// and paste them
	pasteAll{
		if (clipboard.notNil) {
			clipboard[0].do{|path| this.guiAddURL(path) };

			// need to do metadata too.
		};
	}

} ////////////////////////////// end.SampleBank Object

