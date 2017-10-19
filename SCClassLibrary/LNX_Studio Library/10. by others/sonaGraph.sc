// an implementation of the spectrum analyzer
// in the Kay Sonogram, as a bank of filters
// with piano-based frequency
// provided with interactive GUIs
// it exports spectral data in a musical format
// AV since 15/11/2016

SonaGraph {

	var ampResp, pitchResp, hasPitchResp ;
	var <>pitch, <>hasPitch, <>amp, <>anRate ;
	var <>buf ;
	var <>spectrum, <>maxima ;
	var <>sonoChord ;
	var <>avHasPitch, <>avPitch ;
	var synths, synthRt ;

	*prepare {

		Server.local.waitForBoot{
			SynthDef(\pitch, { arg in, out, freq = 10 ;
				var pt, hpt;
				#pt, hpt = Lag3.kr(Tartini.kr(In.ar(in))) ;
				SendReply.ar(Impulse.ar(freq), '/pitch', values:  pt.cpsmidi.round) ;
				SendReply.ar(Impulse.ar(freq), '/hasPitch', values:  hpt)
			}).add ;

			SynthDef(\bank, {arg in = 0, out = 0, dbGain = 0, freq = 10, rq = 0.001;
				var amp;
				var source = In.ar(in,1) * dbGain.dbamp;
				amp = Array.fill(88, {|i|
					Lag.kr(Amplitude.kr(
						BPF.ar(source, (21+i).midicps, rq))
				).ampdb}) ;
				SendReply.ar(Impulse.ar(freq), '/amp', values:  amp)
			}).add ;

			SynthDef(\sine, {|freq = 440, out = 0, amp = 0.1 |
				Out.ar(out, SinOsc.ar(freq, mul:amp)*EnvGen.kr(Env.perc, doneAction:2))
			}).add ;

			SynthDef(\player, {|buf, start = 0, out =0 |
				Out.ar(out, PlayBuf.ar(1, buf, startPos:start))
			}).add ;
		} ;

		SynthDef(\mdaPiano, { |out=0, freq=440, gate=1,
			vel = 100, decay  = 0.8, thresh = 0.01, mul = 0.1|
			var son = MdaPiano.ar(freq, gate, vel, decay,
				release: 0.5, stereo: 0.3, sustain: 0);
			DetectSilence.ar(son, thresh, doneAction:2);
			Out.ar(out, son * mul);
		}).add;

		SynthDef(\sinePlay, { arg freq, amp;
			Out.ar(0, SinOsc.ar(freq, mul:amp))
		}).add


	}

	analyze { |buffer, rate = 10, rq = 0.01|
		var x = Synth(\bank, [\freq, rate, \rq, rq]) ;
		var y = Synth(\pitch, [\freq, rate]) ;
		var anBus = Bus.audio(Server.local, 1) ;
		var z = {Out.ar([anBus,0], PlayBuf.ar(1,buffer, doneAction:2))}.play ;
		amp = [] ; pitch = []; hasPitch = []; anRate = rate ;
		buf = buffer ;
		ampResp = OSCFunc({ |msg|  amp = amp.add(msg[3..]) }, '/amp');
		pitchResp = OSCFunc({ |msg|  pitch = pitch.add(msg[3..].postln) }, '/pitch');
		hasPitchResp = OSCFunc({ |msg|  hasPitch = hasPitch.add(msg[3..].postln) }, '/hasPitch');

		x.set(\in, anBus) ; y.set(\in, anBus) ;
		{
			(buffer.numFrames/Server.local.sampleRate).round.wait;
			ampResp.free ; pitchResp.free ;
			x.free; y.free ;
			//clean up
			// avoid -inf
			amp = amp.collect{ |i|
				if(i.includes(-inf)){
					i = Array.fill(88, {-96})}{i}
			} ;
			// flat and remove strange values
			pitch = pitch.flat.postln.collect{|i|
				case {i < 21} {i = 21 }
				{i > (88+21)} {i = (88+21) }
				{(i >=21)&&(i<=(88+21))} {i}}.postln ;
			hasPitch = hasPitch.flat.postln ;
			amp = amp[..
				(buffer.numFrames/Server.local.sampleRate*rate).asInteger
			] ;
			pitch = pitch[..
				(buffer.numFrames/Server.local.sampleRate*rate).asInteger
			] ;
			hasPitch = hasPitch[..
				(buffer.numFrames/Server.local.sampleRate*rate).asInteger
			] ;
		}.fork
	}

	// spectral slice methods are intended for short
	// as they work by averaging data

	// set average pitch of the sound
	calculateAvPitch {
		avPitch = pitch.sum/pitch.size
	}

	// set average pitchedness of the sound
	calculateAvHasPitch {
		avHasPitch = hasPitch.sum/hasPitch.size
	}

	// spectrum is the average db of the db seq
	calculateSpectrum {
		// spectrum sum and average, from 21 to 88+21
		spectrum = amp.flop.collect{|i| i.sum/i.size}
	}

	// an interactive GUI, could be moved out
	plotSpectrum  {|step = 10|
		var w = Window.new("spectrum",
			Rect(100, 100, step*88, 96*4)).front ;
		if (spectrum.isNil){ this.calculateSpectrum } ;
		w.drawFunc = {
			Pen.font = Font( "DIN Condensed", step );
			Array.series(9, -10, -10).do{|i,j|
				Pen.strokeColor = Color.gray(0.5) ;
				Pen.line(
					Point(0, i.neg*4),
					Point(w.bounds.width, i.neg*4)
				) ;
				Pen.stroke ;
				Pen.fillColor = Color.red ;
				Pen.stringAtPoint(i.asString,Point(0, i.neg*4)) ;
				Pen.fill
			} ;
			Pen.stroke ;
			amp.flop.collect{|i| i.sum/i.size}.do{|i,j|
				Pen.fillColor = Color.red ;
				if(((j+21)%12) == 0){
					Pen.stringAtPoint(
						((j+21).midinote.last).asString, Point(step*j+step, 10)) ;
					Pen.fillStroke ;
					Pen.strokeColor = Color.gray(0.5) ;
					Pen.line(
						Point(step*j, 0),
						Point(step*j, 96*4)
					) ;
					Pen.stroke ;
				} ;
				Pen.strokeColor = Color.white ;
				Pen.line(
					Point(step*j, i.neg*4),
					Point(step*j, 96*4)
				) ;
				Pen.stroke ;
				Pen.addOval(
					Rect(step*j-(step*0.25), i.neg*4, step*0.5, step*0.5)
				) ;
				Pen.fill ;
				Pen.fillColor = Color.black ;
				Pen.stringAtPoint((j+21).midinote.toUpper[..1],Point(step*j-(step*0.5), i.neg*4+step)) ;
				Pen.stringAtPoint((j+21).asString,Point(step*j-(step*0.5), i.neg*4-step)) ;
				Pen.fill
			} ;
		} ;
		w.view.mouseDownAction_{|view, x, y, mod|
			var pitch =
			x.linlin(0, view.bounds.width, 0, 88).round + 21;
			Synth(\mdaPiano, [\freq, pitch.midicps]) ;
		}
	}

	// set the maxima arr as a num of spectral maxima and db
	specMaxima { arg num = 4 ;
		var amps  ;
		maxima = [] ;
		if (spectrum.isNil){ this.calculateSpectrum } ;
		// we do a copy because of sort
		amps = spectrum.collect{|i| i};
		amps = amps.sort.reverse[..(num-1)] ;
		amps.do{|amp|
			maxima =
			maxima.add([spectrum.indexOf(amp)+21, amp]) ;
		} ;
	}

	// gives you back the chord of maxima, pitches and no dbs
	maximaChord { ^maxima.collect{|i| i[0]} }

	// plays back the maxima chord, db weighted
	playMaxima {|boost = 20| // lotta dbs coz typically low
		maxima.do{|i|
			Synth(\mdaPiano,
				[\freq, i[0].midicps, \mul, (i[1]+boost).dbamp])
		};
	}

	// spec to lily
	// PRIVATE
	createLilyNote {|midi|
		var post = "" ;
		var name = midi.midinote[..1] ;
		var oct = midi.midinote[2].asString.asInteger ;
		name = name.replace(" ", "").replace("#", "is") ;
		if (oct >= 5){
			(oct-4).do{|i|
				post = post++"'"
			}
		}{
			(4-oct).do{
				post = post++","
			}
		};
		^name++post++4;
	}

	createLilyChord  {|chord|
		var treble = [], tCh = "" ;
		var bass = [], bCh = "";
		chord.postln.do{|midi|
			if (midi >= 60){
				treble = treble.add(this.createLilyNote(midi))
			}{
				bass = bass.add(this.createLilyNote(midi))
			}
		};
		if (treble == []) {
			tCh = "	\\hideNotes c'4 \\unHideNotes  \\override Stem.transparent = ##t"
		}{
			treble.do{|n| tCh = tCh+n}};
		if (bass == []) {
			bCh = "	\\hideNotes c,4 \\unHideNotes  \\override Stem.transparent = ##t"
		}{
			bass.do{|n| bCh = bCh+n}};
		if (bass.size > 0) { bCh = "<<"+bCh+">>" };
		if (treble.size > 0) { tCh = "<<"+tCh+">>" };
		^[tCh, bCh]
	}



	writeLilyChord {|chord, path|
		var treble = "" ;
		var bass = "" ;
		var score ;
		var lilyChFile, ch ;
		path = if (path.isNil){"/tmp/sonoLily.ly"}{path} ;
		lilyChFile = File(path,"w") ;
		score = "

\\version \"2.18.2\"

\\header {
tagline = \"\"  % removed
}

#(set! paper-alist (cons '(\"my size\" . (cons (* 1.5 in) (* 2.5 in))) paper-alist))

\\paper {
#(set-paper-size \"my size\")
}


woodstaff = {
% This defines a staff with only one lines.
% It also defines its position
\\override Staff.StaffSymbol.line-positions = #'(0)
\\override Staff.TimeSignature #'stencil = ##f
\\override Stem.transparent = ##t
\\set fontSize = -3
%\tempo 4 = BPM
\\override Score.MetronomeMark.X-offset = #-3
\\override Score.MetronomeMark.Y-offset = #6
\\clef percussion
\\override Staff.Clef  #'stencil = ##f
\\time 1/4
\\hideNotes
% This is necessary; if not entered, the barline would be too short!
\\override Staff.BarLine.bar-extent = #'(-1 . 1)

}

\\score {
<<
\\new PianoStaff

<<
\\new Staff

{\\override Staff.TimeSignature #'stencil = ##f
\\override Stem.transparent = ##t
\\set fontSize = -1
%\tempo UN = BPM
\\time 1/4

TREBLE



}

\\new Staff

{\\clef bass
\\override Staff.TimeSignature #'stencil = ##f
\\override Stem.transparent = ##t


\\set fontSize = -1
%\tempo UN = BPM
\\time 1/4

BASS


\\bar \"|.\"

}
>>
>>
}
" ;
		ch = this.createLilyChord(chord) ;
		treble = treble + ch[0]++"\n" ;
	bass = bass +ch[1] ++"\n" ;
		score = score.replace("TREBLE", treble)
		.replace("BASS", bass)
		;
		lilyChFile.write(score);
		lilyChFile.close;
	}

	// PUBLIC
	specToLily {|path|
		this.writeLilyChord(this.maximaChord, path)
	}

	renderLily {|path|
		path = if (path.isNil){"/tmp/sonoLily.ly"}{path} ;
		(
			"Applications/LilyPond.app/Contents/Resources/bin/lilypond  -fpng --output="++path.splitext[0] + path
		).unixCmd
	}

	showSpec { |num = 6|
		var im, w ;
		this.specMaxima(num) ;
		{
			this.specToLily ;
			1.wait ;
			this.renderLily ;
			1.wait ;

			im = Image.new("/tmp/sonoLily.png");

			w = Window.new("", Rect(400, 400, 100, 180));
			w.view.background_(Color.white);
			w.view.backgroundImage_(im);
			w.front;
			w.view.mouseDownAction_{this.playMaxima}
		}.fork(AppClock)
	}

	// END OF SPECTRUM METHODS

	// converts the amp seq into a chord seq
	sonoToChord {|thresh = -30|
		sonoChord =
		amp.collect{|slice|
			slice.collect{|i,j|
				[j+21, i]}
		}
		.collect{|slice|
			slice.select{|pa|
				pa[1]>=thresh
			}
		}
	}

	// plays it back immediately
	// if a note is present in the previous block
	// it is not played for sake of intelligibility
	playSonoChord { |boost = 15|
		{
			var playing = [] ;
			sonoChord.do{|chord|
				if (chord.size>0) {
					chord.do{|note|
						if (playing.includes(note[0]).not){
							Synth(\mdaPiano,
								[\freq, note[0].midicps,
									\mul, (note[1]+boost).dbamp]
						) }
					}
				} ;
				playing = chord.collect{|i| i[0]} ;
				anRate.reciprocal.wait
			}
		}.fork ;
	}

	// create a voice from a sequence by grouping
	// i.e. note with att, dur, db
	// asProfile if true->  returns sequence of amps (profile)
	makeVoice {|voice, asProfile = false|
		var v = Pseq(voice).asStream ;
		var next = v.next;
		var actual = next;
		var time = 0, att = 0, dur = 0 ;
		var dyn ;
		var notes = [] ;
		voice.size.do{
			case
			{ (next.isNil).and(actual.isNil) }
			{ actual = next ; next = v.next; time = time+1 }
			// start
			{ (next.isNil.not).and(actual.isNil) }
			{ att = time ;
				dur = 1 ;
				dyn = [next] ;
				actual = next ; next = v.next; time = time+1 }
			// sus
			{ (next.isNil.not).and(actual.isNil.not) }
			{
				dur = dur+1 ;
				dyn = dyn++[next] ;
				actual = next ; next = v.next; time = time+1 }

			// end
			{ (next.isNil).and(actual.isNil.not) }
			{
				if (asProfile)
				{notes = notes.add([dyn, att, dur])}
				{notes = notes.add([(dyn.sum/dyn.size), att, dur]) } ;
				actual = next ; next = v.next; time = time+1 }
		} ;
		^notes
	}

	// create all voices from a sonagram
	makeVoices {|thresh, asProfile = false|
		var voices = amp.flop.collect{|i| i.collect{|i|
			if(i < thresh){nil}{i}
		}} ;
		var vc, vDict = () ;
		voices.do{|i,j|
			vc = this.makeVoice(i, asProfile) ;
			if(vc.size>0){vDict[j] = vc}
		} ;
		^vDict
	}

	// writes a midi file, can be used directly
	// as it calls makeVoices
	voicesToMidi {|path, voices, thresh|
		var m = SimpleMIDIFile(path);
		// create empty file
		m.init1( 1, 120, "4/4" );
		// init for type 1 (multitrack); 3 tracks, 120bpm, 4/4 measures
		m.timeMode = \seconds;
		// change from default to something useful
		voices = if (voices.isNil){this.makeVoices(thresh)}{voices} ;
		voices.keys.do{|key,j|
			voices[key].do {|ev|
				[key, ev].postln ;
				m.addNote(
					key+21,
					ev[0].linlin(-96,0, 0, 127).asInteger,
					ev[1]*anRate.reciprocal,
					ev[2]*anRate.reciprocal
			)} ;
		} ;
		m.write
	}

	// synthesize sonagram
	synthesize  { arg thresh ;
		thresh = if (thresh.isNil) {-96.neg}{thresh} ;
		synthRt = {
			synths = Array.fill(88, {Synth(\sinePlay)}) ;
			amp.do{|bl|
				bl.do{|v, i|
					v = if (v >= thresh){ v }{ -96} ;
					synths[i].set(
						\freq, (21+i).midicps,
						\amp, v.dbamp)
				} ;
				anRate.reciprocal.wait ;
			};
			synths.do{|i| i.free}
		}.fork;
		^synthRt
	}

	// clean out
	stopSynthesize{
		synthRt.stop; synths.do{|i| i.free}
	}

	writeArchive { |path|
		[amp, pitch, hasPitch, anRate].writeArchive(path)
	}

	readArchive {|path|
		#amp, pitch, hasPitch, anRate = Object.readArchive(path)
	}


	// redirect and helper
	gui {|buffer, hStep = 2.5, vStep = 6, thresh|
		var bf = case
		{ buffer.notNil }{ buffer}
		{ buffer.isNil }{ buf } ;
		thresh = if(thresh.isNil){-96}{thresh} ;
		if (bf.isNil){"Please pass a buffer!".postln}{
			SonaGraphGui(this, bf, hStep, vStep).makeGui(thresh)
		}
	}


	postScript { arg path, buffer, width = 600, height = 200,
		frame = 30,
		xEvery = 1,
		xGridOn = true, yGridOn = true,
		xLabelOn = true, gridCol = Color.red(0.6),
		frameCol = Color.green(0.5),
		cellType = \oval;
		var grCol = [gridCol.red, gridCol.green, gridCol.blue] ;
		var frCol = [frameCol.red, frameCol.green, frameCol.blue] ;
		var bf = case
		{ buffer.notNil }{ buffer}
		{ buffer.isNil }{ buf } ;
		if (bf.isNil){"Please pass a buffer!".postln}{
			this.ps(path, bf, width:width, height:height,
				frame:frame,
				xEvery: xEvery, xGridOn:xGridOn,
				yGridOn:xGridOn, xLabelOn:xLabelOn,
				gridCol: grCol, frameCol:frCol, cellType:cellType)
		}
	}

	ps {
		arg path, buf, width = 600, height = 200, frame = 30,
		xEvery = 2, xLabEvery = 2,
		xGridOn = true, yGridOn = true,
		xLabelOn = true, gridCol = [1, 0, 0], frameCol = [0.2,0.5,0.7],
		cellType = \oval;
		var dur = buf.numFrames/Server.local.sampleRate ;
		var xEv = xEvery.linlin(0, dur, 0, amp.size) ;
		//var xLM = xEv.linlin(0, amp.size, 0, dur);
		PsSonaGraph(amp, path, width:width, height:height,
			frame: frame,
			xEvery: xEv, xGridOn:xGridOn,
			xLabMul: xEvery,
			yGridOn:xGridOn, xLabelOn:xLabelOn,
			gridCol: gridCol, frameCol:frameCol,
			cellType:cellType
		) ;
	}

}

// GUI class for plotting and interactive usage
SonaGraphGui {

	var <>sonaGraph ;
	var <>sf, <>buffer ;
	var <>amp, <>pitch, <>anRate ;
	var <>thresh ;
	var <>w, <>u ; // user window for sonagraph
	var <>hStep, <>vStep ;
	var <>cursor, <>cursorView, <>display, <>sfView;


	*new { arg sonaGraph, buffer, hStep = 2.5, vStep = 6 ;
		^super.new.initSonaGraphGui(sonaGraph, buffer, hStep, vStep)
	}

	initSonaGraphGui { arg aSonaGraph, aBuffer,
		aHStep, aVStep ;
		sonaGraph = aSonaGraph ;
		buffer = aBuffer ;
		hStep = aHStep; vStep = aVStep ;
		sf = SoundFile.new ;
		sf.openRead(buffer.path) ;
		amp = sonaGraph.amp;
		pitch = sonaGraph.pitch;
		anRate = sonaGraph.anRate;
	}

	makeGui { |thresh = -60, sfViewH = 100|
		// thresh: used to select active band AND pitch
		var flag, player ;
		w = Window("SonaGraph",
			Rect(10, 100, hStep*amp.size, vStep*amp[0].size+sfViewH))
		.background_(Color.gray)
		.front ;
		u = UserView(w, Rect(0, 0, hStep*amp.size, vStep*amp[0].size))
		.background_(Color.white)
		.drawFunc_{
			88.do{|i|
				if (i%12 == 0){
					Pen.strokeColor_(Color.hsv(0.1, 0.2, 1)) ;
					Pen.line(0 @ (vStep*(i+1)), u.bounds.width @ (vStep*(i+1)) ) ;
					Pen.stroke
				}
			} ;
			amp.do{|pitchArr,i|
				pitchArr.do{|p, j|
					if (p > thresh){
						Pen.fillColor_(Color.gray(p.linlin(thresh, -10, 0.9, 0))) ;
						Pen.fillOval(Rect(i*hStep,
							vStep*amp[0].size-(j*vStep)-(vStep*0.5),
							hStep, vStep))
					}
				}
			} ;
			Pen.fillColor_(Color.red) ;
			pitch.do{|p, i|
				if(amp[i][p-21] > thresh){
					Pen.fillOval(Rect(i*hStep,
						vStep*amp[0].size-((p-21)*vStep)-(vStep*0.5),
						hStep, vStep))
				}
			}
		} ;
		cursor = [0,0]; flag = true ;
		cursorView =  UserView(w, Rect(0, 0, hStep*amp.size, vStep*amp[0].size))
		.background_(Color(1,1,1,0));
		display = StaticText(w, Rect(5, 5, 200, 15))
		.font_(Font("DIN Condensed", 12)).align_(\left) ;
		cursorView.mouseDownAction_{|view, x, y, mod|
			var pitch = ((88+21)-(y/vStep)).round ;
			//var time = (x/amp.size).round(0.001) ;
			// should be related to buf dur
			var time = x.linlin(0, u.bounds.width,
				0, buffer.numFrames/Server.local.sampleRate).round(0.001) ;
			Synth(\sine, [\freq, pitch.midicps]) ;
			display.string_((time.asString++"    M:"
				++pitch.asString
				++"  N:"++pitch.midinote.capitalize.replace(" ", "")
				++"  F:"++pitch.midicps.round(0.01).asString)
			);
			cursor = [x,y];
			cursorView.refresh ;
		}
		.drawFunc_{
			//~display.bounds_(Rect(~cursor[0]+3, ~cursor[1]+3, 100, 15));
			display.bounds_(Rect(cursor[0]-23, cursorView.bounds.height-15, 200, 15));
			Pen.strokeColor = Color.red ;
			Pen.line(0 @ cursor[1], cursorView.bounds.width @ cursor[1]);
			Pen.line(cursor[0] @ 0, cursor[0] @ cursorView.bounds.height);
			Pen.stroke
		}
		.keyDownAction_{ |doc, char, mod, unicode, keycode, key|
			if ((unicode == 32)&&(flag == true)){
				flag = false ;
				player = Synth(\player, [\buf, buffer, \start,
					cursor[0].linlin(0, cursorView.bounds.width, 0, sf.numFrames)])
			}
			{
				flag = true;
				player.free;
			}
		} ;
		sfView = SoundFileView.new(w, Rect(0, cursorView.bounds.height, hStep*amp.size, sfViewH))
		.gridColor_(Color.gray(0.3)) ;

		sfView.soundfile = sf;            // set soundfile
		sfView.read(0, sf.numFrames);     // read in the entire file.
		sfView.refresh;

	}
}


/*

PsSonaGraph generates a postscript/pdf file with a plot
from amp data structure of SonaGraph

Andrea Valle scripsit 15/11/2016
*/

PsSonaGraph {

	var <>str ; // the string
	var <>path ; // where to write it
	var <>width, <>height ; // fig dimensions in points
	var <>xEvery,  <>frame ; // x and y grid, frame in pt
	var <>yGridOn, <>xGridOn ; // plot grid?
	var <>gridWidth, <>frameWidth, <>curveWidth ; // stroke widths
	var <>gridCol, <>frameCol, <>curveCol ; // stroke widths
	var <>speckleWidth, <>speckleCol ;
	var <>barWidth, <>barCol ;
	var <>data, <>thresh, <>rate;
	var <>fontName, <>fontSize, <>fontCol ;
	var <>yLabelOn, <>xLabelOn ;
	var <>xLabRound, <>yLabRound ;
	var <>xLabMul;
	var <>xLabStart, <>yLabStart ;
	var <>xLabEvery, <>yLabEvery ;
	var <>samplePoints ; // number of point for resampling
	var <>duration ; // duration to be calculated for functions
	var <>zero, <>zeroWidth ;
	var <>name, <>ext ;
	var <>cellType ;

	// first we assume data is an array
	*new { arg
		data, path, thresh= -60, rate = 10,
		width = 400, height = 200,
		xEvery = 25, frame = 30,
		yGridOn = true, xGridOn = true,
		gridWidth = 0.2, frameWidth = 1,
		gridCol = [0.5, 0.5, 0.5],
		frameCol = [0,0,0], curveCol = [0,0,0],
		fontName = "Courier", fontSize = 6, fontCol = [0,0,0],
		yLabelOn = true, xLabelOn = true,
		xLabRound = 0.01, yLabRound = 0.01,
		xLabMul = 1,
		xLabStart = -10, yLabStart = -25,
		xLabEvery = 2, yLabEvery = 2, cellType = \oval ;
		// this is ugly
		^super.new.initPsPlotter(
			data, path, thresh,rate,
			width, height, xEvery, frame, yGridOn, xGridOn,
			gridWidth, frameWidth,
			gridCol, frameCol,
			fontName, fontSize, fontCol,
			yLabelOn, xLabelOn,
			xLabRound , yLabRound,
			xLabMul,
			xLabStart, yLabStart, xLabEvery, yLabEvery,
			cellType
		)
	}

	initPsPlotter { arg
		// this can't be looked at
		adata, apath, athresh, arate,
		awidth, aheight, axEvery, aframe, ayGridOn, axGridOn,
		agridWidth, aframeWidth,
		agridCol, aframeCol,
		afontName, afontSize, afontCol,
		ayLabelOn, axLabelOn,
		axLabRound, ayLabRound,
		axLabMul,
		axLabStart, ayLabStart,
		axLabEvery, ayLabEvery,
		aCellType;
		var arr ;
		# data, path, thresh, rate,
		width, height, xEvery, frame, yGridOn, xGridOn,
		gridWidth, frameWidth,
		gridCol, frameCol,
		fontName, fontSize, fontCol,
		yLabelOn, xLabelOn,
		xLabRound , yLabRound,
		xLabMul,
		xLabStart, yLabStart, xLabEvery, yLabEvery,
		cellType
		=
		[adata, apath, athresh,arate,
			awidth, aheight, axEvery, aframe, ayGridOn, axGridOn,
			agridWidth, aframeWidth,
			agridCol, aframeCol,
			afontName, afontSize, afontCol,
			ayLabelOn, axLabelOn,
			axLabRound, ayLabRound,
			axLabMul,
			axLabStart, ayLabStart,
			axLabEvery, ayLabEvery,
			aCellType
		];
		#name, ext = path.split($.) ;
		str = "" ;
		this.plot ;
		this.write
	}

	cell {|x,y,width,height, gray|
		if (cellType == \oval){
			^this.circleCell(x, y, width, height, gray)}
		{ ^this.squareCell(x, y, width, height, gray)}
	}

	squareCell {|x, y, width, height, gray|
		var cell = "newpath
X Y moveto
0 height rlineto
width 0 rlineto
0 -height rlineto
closepath
GR setgray
fill
"
		.replace("X", x).replace("Y", y)
		.replace("height", height).replace("width", width)
		.replace("GR", gray) ;
		^cell
	}

	circleCell {|x,y,width, height, gray|
		var ratio = height/width ;
		var cell = "gsave
ratio 1 scale
X Y width 0 360 arc
GR setgray
fill
grestore
"
		.replace("X", x*ratio.reciprocal).replace("Y", y)
		.replace("width", width*0.5).replace("ratio", ratio)
		.replace("GR", gray) ;
		^cell
	}

	setWidth {
		str = str++"% document size\n";
		str = str++"<< /PageSize [X Y] >> setpagedevice\n"
		.replace("X", width+(frame*2))
		.replace("Y", height+(frame*2))
	}

	setFont {
		str = str++"% font setting grid\n";
		str = str++"/"++fontName ++" findfont "++ fontSize.asString++" scalefont setfont\n" ;
	}

	drawFrame {
		str = str++"% frame\n";
		str = str++frame.asString++" "++frame.asString++" translate\n" ;
		str = str++frameWidth.asString++" setlinewidth\n" ;
		frameCol.do{|i| str = str++i.asString++" "} ;
		str = str++"setrgbcolor\n" ;
		str = str++"newpath\n0 0 moveto\n";
		str = str++width.asString++" "+ 0.asString++" lineto\n";
		str = str++width.asString++" "+ height.asString++" lineto\n";
		str = str++0.asString++" "+ height.asString++" lineto\n";
		str = str++0.asString++" "+ 0.asString++" lineto\n";
		str = str++"closepath stroke\n" ;
	}


	drawYGrid {
		var step = height/88 ;
		str = str++"% horizontal grid\n";
		str = str++gridWidth.asString++" setlinewidth\n" ;
		gridCol.do{|i| str = str++i.asString++" "} ;
		str = str++"setrgbcolor\n" ;
		str = str++"newpath\n";
		88.do{|i|
			if (i%12 == 0){
				str = str++"-3 "++ (height-(step*(i+1))).asString ++" moveto\n" ;
				str = str++(width+2).asString++" "++(height-(step*(i+1))).asString++" lineto\n" ;
			}
		} ;
		str = str++"closepath stroke\n" ;
	}

	drawYLabels {
		var step = height/88 ;
		var j = 0 ;
		var frq = Array.series(8, 21+88-1, -12).midicps.round(0.01) ;
		str = str++"% horizontal labels\n";
		fontCol.do{|i| str = str++i.asString++" "} ;
		str = str++"setrgbcolor\n" ;
		str = str++(-14).asString++" "++(height+fontSize).asString++" moveto\n" ;
		str = str ++"(Oct) show\n" ;
		str = str++(width+2).asString++" "++(height+fontSize).asString++" moveto\n" ;
		str = str ++"(Hz) show\n" ;
		str = str++"newpath\n";
		88.do{|i|
			if (i%12 == 0){
				str = str++"-8 "++ (height-(step*(i+1))).asString ++" moveto\n" ;
				str = str++"(@) show\n".replace("@", (9-j).asString) ;
				str = str++(width+2).asString++" "++(height-(step*(i+1))).asString ++" moveto\n" ;
				str = str++"(@) show\n".replace("@", (frq[j]).asString) ;

				j = j+1
			}
		} ;
		str = str++"closepath stroke\n" ;
	}

	// maybe some glitch in time calculation
	drawXGrid {
		var num, x ;
		str = str++"% vertical grid\n";
		str = str++gridWidth.asString++" setlinewidth\n" ;
		gridCol.do{|i| str = str++i.asString++" "} ;
		str = str++"setrgbcolor\n" ;
		str = str++"newpath\n";
		num = (data.size/xEvery).trunc ;
		(num+1).do{|i|
			x = i*(width/num);
			str = str++ x.asString ++" -3 moveto\n" ;
			str = str++x.asString++" "++height.asString++" lineto\n"
		} ;
		str = str++"closepath stroke\n" ;
	}


	drawXLabels {
		var num, x, val ;
		str = str++"% vertical labels\n";
		fontCol.do{|i| str = str++i.asString++" "} ;
		str = str++"setrgbcolor\n" ;
		str = str++(width*2/5).asString++" "++(xLabStart*2).asString++" moveto\n" ;
		str = str ++"(time (seconds)) show\n" ;
		num = (data.size/xEvery).trunc ;
		(num+1).do{|i|
			x = i*(width/num);
			val = i*xEvery ;
			//.asTimeString;
			str = str++ (x-(val.asString.size*0.5*fontSize*0.25)).asString ++" "++xLabStart.asString++" moveto\n" ;
			str = str++"("++(i*xLabMul)++") show\n"

		} ;
	}

	drawSonagraph {
		// curve
		var xStep = width/data.size ;
		var yStep = height/data[0].size ;
		var gray ;
		data.do{|ampArr,i|
			ampArr.do{|p, j|
				if (p > thresh){
					gray = p.linlin(thresh, -10, 0.9, 0) ;
					str = str++
					this.cell(i*xStep, j*yStep, xStep, yStep, gray)
					//this.circleCell(i*xStep, j*yStep, xStep, yStep, gray)
				}
			}
		} ;
	}

	// total
	plot {
		this.setWidth ;
		this.setFont ;
		this.drawFrame ;
		this.drawSonagraph ;
		if (xGridOn) { this.drawXGrid } ;
		if (xLabelOn) { this.drawXLabels };
		if (yGridOn) { this.drawYGrid } ;
		if (yLabelOn) { this.drawYLabels };
	}

	write {
		var file = File(name++".ps", "w") ;
		file.write(str++"showpage\n") ;
		file.close ;
		if (ext.asSymbol == \pdf) {
			("pstopdf"+name++".ps").unixCmd{
				("rm"+name++".ps").unixCmd
			}
		}
	}

}
/*

(
a = SonaGraph.new;
a.readArchive("/Users/andrea/Desktop/sonaChal.log")
~data = a.amp ;

p = PsSonaGraph(~data, ~sample, "/Users/andrea/Desktop/untitled.pdf", width:600, xEvery: 30, xLabEvery:2, xGridOn:true, yGridOn:true, xLabelOn:true, gridCol: [1, 0, 0], frameCol:[0.2,0.5,0.7]) ;


)

*/


/*

// here we start up server and defs
SonaGraph.prepare ;

// something to analyze, i.e a buffer
~path ="/Users/andrea/musica/recordings/audioRumentario/pezzi/indie_I/fiati/chalumeau or.wav"; ~sample = Buffer.read(s, ~path).normalize ;

// an istance
a = SonaGraph.new;
// now analyzing in real-time
a.analyze(~sample,30) ; // high rate!

// checking size of coupled arrays
// a.amp.size == a.pitch.size ; // true

// here we calculate and plot the average spectrum
//a.plotSpectrum
// GUI is interactive, click and you here the sound

// writing to an archive
a.writeArchive("/Users/andrea/Desktop/sonaChal.log")

a.gui(hStep:1) ; // directly, if anRate=1 then default hStep fine

// again
a = SonaGraph.new ;
// read the log, may require some time
a.readArchive("/Users/andrea/Desktop/sonaChal.log") ;

a.gui(~sample, 1) ; // we still need the sample for playback
// same as:
g = SonaGraphGui(a, ~sample,1).makeGui

// resynthesis
a.synthesize ; // start synthesis
a.stopSynthesize ; // stop synth routine and free

// write to postscript
a.postScript("/Users/andrea/Desktop/sonaChal.pdf") ;


// using spectrum data

// here we start up server and defs
SonaGraph.prepare ;

// something to analyze, i.e a buffer
~path ="/Users/andrea/musica/regna/fossilia/compMine/erelerichnia/fragm/snd/vareseOctandreP18M5N[8,9,0,7,11,6].aif"; ~sample = Buffer.read(s, ~path).normalize ;

// an istance
a = SonaGraph.new;
// now analyzing in real-time
a.analyze(~sample,15) ; // high rate!

a.gui(hStep:10)
a.plotSpectrum
a.showSpec(4)

a.specMaxima(6) // explictly calculating
a.maximaChord // access
a.playMaxima
a.hasPitch
a.pitch
a.calculateAvPitch
a.calculateAvHasPitch
a.avHasPitch
a.avPitch

a.specToLily("/Users/andrea/musica/regna/fossilia/compMine/erelerichnia/chords.ly")



// something to analyze, i.e a buffer
~path ="/Users/andrea/musica/regna/fossilia/compMine/erelerichnia/fragm/octandreExc2.aif"; ~sample = Buffer.read(s, ~path).normalize ;

// an istance
a = SonaGraph.new;
// now analyzing in real-time
a.analyze(~sample,15) ; // high rate!

a.gui(hStep:6) ;

a.sonoToChord(thresh:-40) ; // here you get chord of peaks above thresh
a.sonoChord ; // which is
a.playSonoChord // play it

v = a.makeVoices(-40) ; // reconstructs events from bins above -40 thresh
// write it down to MIDI
a.voicesToMidi("~/Desktop/midifiletest.mid", thresh:-40 ) ;





*/
