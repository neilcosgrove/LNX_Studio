// 
// LNX_EQ is adapted from the wslib quark 2009
/*

******* Make a Dub Delay out of it *********** !!!!
// to be used in 3 places instrument mixer, master & effect

flat button, [no clip, tanh, soft,hard], wslib

e=LNX_EQ().open;
f=LNX_EQ(s, a.eqGroup, a.a.instGroupChannel).open;
e.channel
f.channel
e.close;
e.open;
e.channel_(a.b.instGroupChannel);
e.stopDSP;
e.startDSP;

use different clipping in pong dub

MULTI band distortion

Remember this window was or wasn't open for circle A-1-X options to show inst windows in Mixer.

*/
// api, midiControls, getSaveList, models, covert gui

LNX_EQ {
	
	classvar <version=1.0;
	
	var <window, <bounds, <p, <server, <gui, selected = -1, <>quant=2;
	var isPlaying = false, <group, <synth, <nodeID, <id;
	var <models, api, <midiControl, <midiOffset=9900;
	var lastTime=0, nextTime, fps=25; // has its own fps
	var <name="EQ", <channel=0, <presetMemory;
	
	var <pSize, <>syncDelay=0; // for popping in presets
	
	// make new
	*new {|server, group, channel, id, window, bounds, midiControl, midiOffset|
		^super.new.init(server, group, channel, id, window, bounds, midiControl, midiOffset)
	}
	
	// init everything
	init{|argServer,argGroup,argChannel, argID, argWindow, argBounds, argMIDIControl, argMIDIOfset|
		server       = argServer ? Server.default;
		group        = argGroup;
		channel      = argChannel ? channel;
		id           = argID;
		api          = LNX_API.newTemp(this,id ?? {Symbol.rand},#[\netSetP]);
		midiControl  = argMIDIControl ?? {LNX_MIDIControl()};
		midiOffset   = argMIDIOfset ? midiOffset;
		window       = argWindow;
		bounds       = argBounds;
		presetMemory = [];
		this.initModels;
	}
	
	// make the models
	initModels{

		#models,p=[
		
			////// low shelf
		
			// 0. low shelf freq
			[100, \freq, (label_:"Freq"), midiControl, midiOffset+0, "EQ Low Freq",
				{|me,val,latency,send,toggle| this.setGraphP(0,val,latency,send,toggle.not) }],
			
			// 1. low shelf Q
			[1, [0.6, 10, \exp], (label_:"Q"), midiControl, midiOffset+1, "EQ Low Q",
				{|me,val,latency,send,toggle| this.setGraphP(1,val,latency,send,toggle.not) }],
			
			// 2. low shelf amp dbs
			[\eqAmp, (label_:"Amp"), midiControl, midiOffset+2, "EQ Low Amp",
				{|me,val,latency,send,toggle| this.setGraphP(2,val,latency,send,toggle.not) }],
			
			/////// peak 1
		
			// 3. peak 1 freq
			[250, \freq, (label_:"Freq"), midiControl, midiOffset+3, "EQ Peak 1 Freq",
				{|me,val,latency,send,toggle| this.setGraphP(3,val,latency,send,toggle.not) }],
			
			// 4. peak 1 Q
			[1, [0.1, 10, \exp], (label_:"Q"), midiControl, midiOffset+4, "EQ Peak 1 Q",
				{|me,val,latency,send,toggle| this.setGraphP(4,val,latency,send,toggle.not) }],
			
			// 5. peak 1 amp dbs
			[\eqAmp, (label_:"Amp"), midiControl, midiOffset+5, "EQ Peak 1 Amp",
				{|me,val,latency,send,toggle| this.setGraphP(5,val,latency,send,toggle.not) }],
			
			/////// peak 2
		
			// 6. peak 2 freq
			[1000, \freq, (label_:"Freq"), midiControl, midiOffset+6, "EQ Peak 2 Freq",
				{|me,val,latency,send,toggle| this.setGraphP(6,val,latency,send,toggle.not) }],

			// 7. peak 2 Q
			[1, [0.1, 10, \exp], (label_:"Q"), midiControl, midiOffset+7, "EQ Peak 2 Q",
				{|me,val,latency,send,toggle| this.setGraphP(7,val,latency,send,toggle.not) }],
			
			// 8. peak 2 amp dbs
			[\eqAmp, (label_:"Amp"), midiControl, midiOffset+8, "EQ Peak 2 Amp",
				{|me,val,latency,send,toggle| this.setGraphP(8,val,latency,send,toggle.not) }],
			
			/////// peak 3
		
			// 9. peak 3 freq
			[3500, \freq, (label_:"Freq"), midiControl, midiOffset+9, "EQ Peak 3 Freq",
				{|me,val,latency,send,toggle| this.setGraphP(9,val,latency,send,toggle.not) }],

			// 10. peak 3 Q
			[1, [0.1, 10, \exp], (label_:"Q"), midiControl, midiOffset+10, "EQ Peak 3 Q",
				{|me,val,latency,send,toggle| this.setGraphP(10,val,latency,send,toggle.not) }],
			
			// 11. peak 3 amp dbs
			[\eqAmp, (label_:"Amp"), midiControl, midiOffset+11, "EQ Peak 3 Amp",
				{|me,val,latency,send,toggle| this.setGraphP(11,val,latency,send,toggle.not) }],
					
			////// high shelf
		
			// 12. high shelf freq
			[6000, \freq, (label_:"Freq"), midiControl, midiOffset+12, "EQ High Freq",
				{|me,val,latency,send,toggle| this.setGraphP(12,val,latency,send,toggle.not) }],

			// 13. high shelf Q
			[1, [0.6, 10, \exp], (label_:"Q"), midiControl, midiOffset+13, "EQ High Q",
				{|me,val,latency,send,toggle| this.setGraphP(13,val,latency,send,toggle.not) }],
							
			// 14. high shelf amp dbs
			[\eqAmp, (label_:"Amp"), midiControl, midiOffset+14, "EQ High Amp",
				{|me,val,latency,send,toggle| this.setGraphP(14,val,latency,send,toggle.not) }],
			
			//// others
			
			// 15. on/off
			[0, \switch, midiControl, midiOffset+15, "EQ On/Off",
				{|me,val,latency,send,toggle|
					this.setPVPModel(15,val,latency,send);
					switch( val.asInt,
						1, { this.startDSP },
						0, { this.stopDSP });
			 }],
			 
			// 16. lag
			[0.1, [0.1,1,\exp], (label_:"Lag"), midiControl, midiOffset+16, "EQ Lag",
				{|me,val,latency,send,toggle|
					this.sendSynthArg(16, val, \lag, val, latency, send);
			}],

			// 17. clip (0 = none, 1=tanh, 2=softclip, 3=distort, 4=clip)
			[0, [0,4,\lin,1], midiControl, midiOffset+17, "EQ Clip",
				(\items_: [ "no clip", "tanh", "soft clip", "distort", "hard clip"]),
				{|me,val,latency,send,toggle|
					this.sendSynthArg(17, val, \clip, val, latency, send);
			}],
			
			// 18.preAmp (to be used)
			[\db6]
			
		].generateAllModels;
		
		pSize = p.size; // so external classes can get p size, used in presete
		
	}
	
	// set & network p + model via VP
	setPVPModel{|i,val,latency,send|
		p[i]=val;	
		if (send) { api.sendVP(id++"_eqvpm_"++i,'netSetP',i,val) }; // network
	}
	
	// set p from model
	setGraphP{|i,val,latency,send,refresh=false|
		p[i]=val;	
		this.sendControls(latency); // send controls to server
		if (refresh) { {this.lazyRefresh }.defer }; // refresh if not from graph mouseIn
		if (send) { api.sendVP(id++"_eqvpm_"++i,'netSetP',i,val) }; // network
	}
	
	// set synth args used for lag & in/out channel
	sendSynthArg{|i, val, name, val2, latency, send|
		p[i]=val;		
		if (synth.notNil) {
			if (synth.nodeID.notNil) {
				server.sendBundle(latency +! syncDelay, [\n_set, synth.nodeID, name, val2])
			};
		};
		if (send) { api.sendVP(id++"_eqvpm_"++i,'netSetP',i,val) }; // network
	}
	
	// net of above
	netSetP{|index,value| models[index].lazyValueAction_(value,nil,false,false) }
	
	// for mixer GUI
	onOffModel{^models[15]}

	//////////////////////////////////////////////////////////////////////////////////////////////
	//                                                                                          //
	// DSP stuFF                                                                                //
	//                                                                                          //
	//////////////////////////////////////////////////////////////////////////////////////////////
		
	// send to the server
	sendControls{|latency|
		if (synth.notNil and:{synth.nodeID.notNil}) {
			server.sendBundle(latency +! syncDelay,
				[\n_setn, synth.nodeID, \eq_controls, 15]++p[0..14]
			)
		}
	}

	// set the in/out channel
	channel_{|num, latency|
		channel = num;	
		if (synth.notNil and:{synth.nodeID.notNil}) {
			server.sendBundle(latency +! syncDelay, [\n_set, synth.nodeID, \in, channel]);
		}
	}
		
	// the synthDef
	*initUGens{|server|
		
		// the synth def
		SynthDef( "param_beq", {|in=0, gate=1, fadeTime=0.05, doneAction=2, lag=0.1, clip=0|
			var frdb, input, env;
			
			input = In.ar(in, 2);
			
			frdb  = (Control.names([\eq_controls]).kr(0!15)).clump(3);
			input = BLowShelf.ar(input, *frdb[0][[0,1,2]].lag(lag));
			input = BPeakEQ.ar  (input, *frdb[1][[0,1,2]].lag(lag));
			input = BPeakEQ.ar  (input, *frdb[2][[0,1,2]].lag(lag));
			input = BPeakEQ.ar  (input, *frdb[3][[0,1,2]].lag(lag));
			input = BHiShelf.ar (input, *frdb[4][[0,1,2]].lag(lag));
			input = Select.ar(clip,
				[input, input.tanh, input.softclip, input.distort, input.clip(-1,1)]);
			env   = EnvGen.kr( Env.asr(fadeTime,1,fadeTime), gate, doneAction: doneAction );
					
			XOut.ar( in, env, input ); // crossfade on/off
			
			//Max.kr(input.abs).poll;
				
		}).send(server);	
	}
		
	// turn ON eq
	startDSP{|latency|
		if (synth.notNil) { this.stopDSP(latency) }; // if still playing previous then stop it
		
		group     = group ?? (Group.basicNew(server)); // make a group if needed
		synth     = Synth.basicNew("param_beq");       // get a new synth/node but don't start
		nodeID    = synth.nodeID;                      // get its ID
	 	isPlaying = true;		
			
		// send everything to the server	
		server.sendBundle(latency +! syncDelay, group.newMsg(server, \addAfter),
			*([ synth.newMsg( group,
				[\in, channel, \doneAction, 2, \lag: p[16], \clip: p[17]]),
					synth.setnMsg(\eq_controls, p[0..14])] )
		);
	}
	
	// turn OFF eq
	stopDSP{|latency|		
		if (nodeID.notNil) {
			server.sendBundle(latency +! syncDelay,["/n_set", nodeID, \gate, -1.01]); // release
		};
		synth     = nil;
		nodeID    = nil;
		isPlaying = false;
	}
	
	// server reboot
	restartDSP{|argGroup|
		group = argGroup;
		if(p[15].isTrue) { this.startDSP };
	}
		
	// update the synth, used after a preset change
	updateDSP{|latency|
		// what about on/off ? 
		if(p[15].isTrue && (isPlaying.not)) { this.startDSP(latency); ^this }; // turn on if needed
		if(p[15].isFalse && (isPlaying)) { this.stopDSP(latency); ^this };    // turn off if needed
		
		if (synth.notNil) {
			server.sendBundle( latency +! syncDelay, 
				*([ synth.newMsg( group,
					[\in, channel, \doneAction, 2, \lag: p[16], \clip: p[17]]),
						synth.setnMsg(\eq_controls, p[0..14])] )
			);	
		}
	}
	
	// free everything
	free{
		this.stopDSP;
		this.close;
		api.free;
		gui.do(_.free);
		models.do(_.free);
		{
			window = bounds = p = server = gui = group = synth = models = api = midiControl = nil;
		}.defer(1)
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	//                                                                                          //
	// PRESETS & Disk IO                                                                        //
	//                                                                                          //
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// get the current state as a list
	getPresetList{ ^p }
	
	// add a statelist to the presets
	addPresetList{|l| presetMemory=presetMemory.add(l) }
	
	// save a state list over a current preset
	savePresetList{|i,l| presetMemory[i]=l }
	
	// load preset
	loadPreset{|i,latency|
		models.do{|m,j|
			p[j]=presetMemory[i][j];
			m.lazyValue_(presetMemory[i][j],false);
		};
		this.updateDSP(latency);
		this.lazyRefresh;
	}
	
	// remove preset
	removePreset{|i| presetMemory.removeAt(i) }
	
	// remove all presets
	removeAllPresets{ presetMemory=[] }
	
	// get save list for eq
	// This changes instTemp load versions to 1.3
	getSaveList{
		var l;
		l = ["LNX_EQ Doc v"++version, p.size, presetMemory.size];
		l = l ++ p ++ (presetMemory.flat);
		l = l++(["*** END LNX_EQ DOC ***"]);
		^l;
	}
	
	// put the save list back into the eq
	putLoadList{|l|
		var header;
		l = l.reverse;
		header=l.popS;
		if ((header.documentType)=="LNX_EQ Doc") {	
			var noP   = l.popI;	// pop number of elements in p
			var noPre = l.popI;	// pop number of presets
			p=l.popNF(noP);		// pop p
			p.do{|val,i| models[i].lazyValue_(val,false) }; // put p into models
			presetMemory = [];
			noPre.do{ presetMemory = presetMemory.add(l.popNF(noP)) }; // pop presets
			this.updateDSP;		// no update synth
		};	
	}
	
	// no save list cause loading older version so make presets as needed
	noSaveList{|noPresets| presetMemory = p ! noPresets }
		
	//////////////////////////////////////////////////////////////////////////////////////////////
	//                                                                                          //
	// GUI stuFF                                                                                //
	//                                                                                          //
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// open or show the window	
	open{
		if (gui.isNil) { this.createWidgets; ^this };	// make widgets
		if (window.notNil) { window.open };			// or open window
	}
	
	openIfOn{
		if (p[15].isTrue) {
			if (gui.isNil) { this.createWidgets; ^this }; // make widgets
			if (window.notNil) { window.open };           // or open window
		};
	}
	
	// close the window
	close{ if (window.notNil) { window.close } }
	
	// name the window
	name_{|string|
		name=string;
		{ if (window.notNil) { window.name_(string) } }.defer;
	}
	
	// create gui widgets
	createWidgets{|argWindow, argBounds|
		
		gui = ();
		
		bounds = argBounds ? bounds ? Rect(900, 130, 325, 235);
		
		window = argWindow ? window ?? {MVC_Window(name, bounds , false)
			.color_(\background, Color(3/77,1/103,0,65/77))}; 
				
		gui[\scrollView] = MVC_RoundedComView(window,
									Rect(11,11,bounds.width-22,bounds.height-23))
			.color_(\background,Color(59/77,59/77,59/77))
			.color_(\border, Color(6/11,42/83,29/65))
			.resize_(5);

		// this is the main user view / eq grap for the gui
		gui[\graph] = MVC_UserView( gui[\scrollView], Rect(10, 27, 285, 140))
			.resize_(5)
			.focusColor_(Color.clear)
			
			// draw function ////////////////////////////////////////////////////////////
			
			.drawFunc_{|view|
				var freqs, svals, values, bounds, zeroline;
				var freq = 1200, rq = 0.5, db = 12;
				var min = 20, max = 22050, range = 24;
				var vlines = [100,1000,10000];
				var dimvlines = [25,50,75, 250,500,750, 2500,5000,7500];
				var hlines = [-18,-12,-6,6,12,18];
				var pt, strOffset = 11;
				
				bounds = view.bounds.moveTo(0,0);
				
				freq = p[0];
				rq   = p[1];
				db   = p[2];
				
				freqs = (0..bounds.width.div(quant)); // big array for every pixel
				freqs = freqs.linexp(0, bounds.width.div(quant), min, max);
				
				// get each curve value in a list
				
				values = [
					BLowShelf.magResponse( freqs, 44100, p[0], p[1], p[2]),
					BPeakEQ.magResponse  ( freqs, 44100, p[3], p[4], p[5]),
					BPeakEQ.magResponse  ( freqs, 44100, p[6], p[7], p[8]),
					BPeakEQ.magResponse  ( freqs, 44100, p[9], p[10], p[11]),
					BHiShelf.magResponse ( freqs, 44100, p[12], p[13], p[14])
				].ampdb.max(-200).min(200);
				
				zeroline = 0.linlin(range.neg,range, bounds.height, 0, \none);
				
				svals = values.sum.linlin(range.neg,range, bounds.height, 0, \none);
				values = values.linlin(range.neg,range, bounds.height, 0, \none);
				
				vlines = vlines.explin(min, max, 0, bounds.width);
				dimvlines = dimvlines.explin( min, max, 0, bounds.width );
				
				pt = p[0..14].clump(3).collect{|array|
					(array[0].explin( min, max, 0, bounds.width ))
					@
					(array[2].linlin(range.neg,range,bounds.height,0,\none));
				};

				Pen.color_( Color.white.alpha_(0.25) );
				Pen.roundedRect( bounds, [6,6,0,0] ).fill;
				Pen.color = Color.gray(0.2).alpha_(0.5);
				Pen.roundedRect( bounds.insetBy(0,0), [6,6,0,0] ).clip;
				
				Pen.color = Color.gray(0.2).alpha_(0.125);
				
				// horizontal lines
				hlines.do{|hline,i|
					hline = hline.linlin( range.neg,range, bounds.height, 0, \none );
					Pen.line( 0@hline, bounds.width@hline )
				};
					
				dimvlines.do{|vline,i|
					Pen.line( vline@0, vline@bounds.height );
				};
				Pen.stroke;
			
				Pen.color = Color.gray(0.2).alpha_(0.5);
				
				// vertical lines
				vlines.do{|vline,i|
					Pen.line(vline@0, vline@bounds.height );
				};
				Pen.line(0@zeroline, bounds.width@zeroline ).stroke;
								
				Pen.font = Font( Font.defaultSansFace, 10 );
				Pen.color = Color.black.alpha_(0.66);
				
				// horizontal scale text
				hlines.do{|hline|
					Pen.stringAtPoint( hline.asString ++ "dB", 
						3@(hline.linlin( range.neg,range, bounds.height, 0, \none ) 
							- strOffset) );
				};
					
				// vertical scale text
				vlines.do{|vline,i|
					Pen.stringAtPoint( ["100Hz", "1KHz", "10KHz"][i], 
						(vline+2)@(bounds.height - (strOffset + 1)) );
					};
						
				values.do{|svals,i|
					var color;
					color = Color.hsv( i.linlin(0,values.size,0,1), 0.75, 0.5)
						.alpha_( if(selected==i) { 0.5 } { 0.25 });
					
					// under response curve fill 
					Pen.color = color;
					Pen.moveTo(0@(svals[0]));
					svals[1..].do{|val, i|
						Pen.lineTo((i+1*quant)@val);
					};
					Pen.lineTo(bounds.width@(bounds.height/2));
					Pen.lineTo(0@(bounds.height/2));
					Pen.lineTo(0@(svals[0]));
					Pen.fill;
					
					// point circles
					Pen.addArc(pt[i], 5, 0, 2pi);
					Pen.color = color.alpha_(0.75);
					Pen.stroke;
		
				};
				
				// response curve line
				Pen.color = Color.blue(0.5);
				Pen.moveTo(0@(svals[0]));
				svals[1..].do{|val, i|
					Pen.lineTo((i+1*quant)@val);
				};
				Pen.stroke;
				
				Pen.extrudedRect(bounds, [6,6,0,0], 1, inverse:true);
				
			}
			
			// mouseDown function ////////////////////////////////////////////////////////////
			
			.mouseDownAction_{|view,x,y,mod|
				var bounds;
				var pt;
				var min = 20, max = 22050, range = 24;
				
				bounds = view.bounds.moveTo(0,0);
				
				pt = (x@y);
				
				selected =  p[0..14].clump(3).detectIndex{ |array|
					(( array[ 0 ].explin( min, max, 0, bounds.width ) )@
					( array[ 2 ].linlin( range.neg, range, bounds.height, 0, \none ) ))
						.dist( pt ) <= 5;
				} ? -1;
					
				if( selected != -1 ) { gui[\tabView].value_( selected ) };
				view.refresh;
			}
			
			// mouseMove function ////////////////////////////////////////////////////////////
			
			.mouseMoveAction_{ |view,x,y,mod|
				var bounds;
				var pt;
				var min = 20, max = 22050, range = 24;
				var freq, amp, q;
				
				bounds = view.bounds.moveTo(0,0);
				pt = (x@y);
				
				if( selected != -1 ) {
					case { ModKey( mod ).alt } { 
						if(  ModKey( mod ).shift ) {
							// quant & set q
							q = y.linexp( bounds.height, 0, 0.1, 10, \none ).nearestInList(
								if( [0,4].includes(selected) ) 
									{[0.6,1,2.5,5,10]} 
									{[0.1,0.25,0.5,1,2.5,5,10]}
								);
							models[selected*3+1].valueAction_(q,send:true,toggle:true);
							
						}{
							// set q
							q = y.linexp( bounds.height, 0, 0.1, 10, \none ).clip(
								if( [0,4].includes(selected) ) { 0.6 } {0.1}, 10).round(0.01);
							models[selected*3+1].valueAction_(q,send:true,toggle:true);
							
						};
					} { ModKey( mod ).shift } {
						// quant and set freq & amp
						freq = pt.x.linexp(0, bounds.width, min, max ).nearestInList(
							[25,50,75,100,250,500,750,1000,2500,5000,7500,10000] );
						models[selected*3].valueAction_(freq,send:true,toggle:true);
						
						amp = pt.y.linlin( 0, bounds.height, range, range.neg, \none )
							.clip2( range ).round(6);
						models[selected*3+2].valueAction_(amp,send:true,toggle:true);
							
					} { true } {
						// set freq & amp
						freq = pt.x.linexp(0, bounds.width, min, max ).clip(20,20000).round(1);
						models[selected*3].valueAction_(freq,send:true,toggle:true);
						
						amp = pt.y.linlin( 0, bounds.height, range, range.neg, \none)
							.clip2(range).round(0.25);
						models[selected*3+2].valueAction_(amp,send:true,toggle:true);

					};
					
					view.refresh;
				};
			};

		// the tab view
		gui[\tabView] = MVC_TabbedView(gui[\scrollView], Rect(10, 167, 285, 39) , offset:(0@0))
			.labels_([ "low shelf", "peak 1", "peak 2", "peak 3", "high shelf" ])
			.tabWidth_([59,54,54,54,60])
			.tabHeight_(18)
			.labelColors_({ |i| Color.hsv( i.linlin(0,5,0,1), 0.75, 0.5).alpha_( 0.25 ); }!5)
			.backgrounds_({ |i| Color.hsv( i.linlin(0,5,0,1), 0.75, 0.5).alpha_( 0.25 ); }!5)
			.font_( Font( Font.defaultSansFace, 12 ) )
			.resize_( 8 )
			.tabPosition_( \bottom )
			.focusActions_({ |i| { selected = i; gui[\graph].refresh;  }; } !5 );
		
		// models as numberboxes
		models[0..14].do{|model,i|
			MVC_NumberBox(gui[\tabView].mvcTab(i.div(3)),
				Rect([0,2,1.05][i%3]*95+33, 2, 55, 17),model)
				.color_(\background,Color(1,1,1,0.33))
				.labelShadow_(false)
				.visualRound_(0.1)
				.orientation_(\horizontal)
				.color_(\label,Color.black);
		};
		
		gui[\menuTheme]=(
			\font_		: Font("Arial", 10),
			\colors_      : (\background : Color(1,1,1,0.33)),
			\canFocus_	: false
		);
		
		// logo
		MVC_UserView(gui[\scrollView], Rect(235, 5, 60, 17))
			.drawFunc_{|view|
				var rect = view.bounds.moveTo(0,0);
				Color(0,0,0,0.2).set;
				RoundedRect.fromRect(rect).fill;
				Pen.font = Font( "Helvetica-Bold", 11);
				Pen.fillColor_(Color(1,1,1,0.4));
				Pen.stringCenteredIn("wslib EQ",rect);
			};
		
		// 17. clip
		MVC_PopUpMenu3(models[17],gui[\scrollView  ],Rect(48, 5, 67, 16),gui[\menuTheme]);

		// 16. lag 				
		MVC_NumberBox(models[16], gui[\scrollView], Rect( 155, 4, 28, 17) )
			.color_(\background,Color(1,1,1,0.33))
			.labelShadow_(false)
			.visualRound_(0.01)
			.orientation_(\horizontal)
			.color_(\label,Color.black);
			
		// 15.onOff				
		MVC_OnOffView(models[15], gui[\scrollView] , Rect(10, 4,25,19))
			.font_(Font("Helvetica-Bold", 12))
			.resize_(7)
			.color_(\on, Color(0.25,1,0.25) )
			.color_(\off, Color(0.4,0.4,0.4) )
			.rounded_(true);
			
		window.create; // now make it
		
	}
	
	// only refresh at a max frame rate
	lazyRefresh{
		var now=SystemClock.seconds;			// the time now
		if ((now-lastTime)>(1/fps)) {			// if time since last refresh is > frame duration
			lastTime=now;						// so last time becomes now
			nextTime=nil;						// nothing is now scheduled for the future
			if (thisThread.clock==SystemClock) {	// do i need to defer to the AppClock ?
				{this.refresh}.defer;			// defer the refresh
			}{
				this.refresh;					// refresh now
			};
		}{									// else time since last refresh is < frame dur
			if (nextTime.isNil) {				// if there isn't a refresh coming up
				nextTime=lastTime+(1/fps);		// do it when a frame duration has passed
				{
					this.refresh;				// do the refresh
					nextTime=nil;				// no next time set now
				}.defer(nextTime-now);			// defer to the correct delta
				lastTime=nextTime;				// this is now the last time a refreshed happened
			}
		}
	}
	
	// refresh the graph, if it still exists
	refresh{ if ((gui.notNil)and:{gui[\graph].notNil}) { gui[\graph].refresh } }
	
		
}