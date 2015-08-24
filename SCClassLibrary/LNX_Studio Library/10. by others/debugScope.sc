+ Bus {
	debugScope {
		| title, names |
		var w, ms, listArray, size=380, sliderLoc=0, routine, bus, 
			min=1000.0, max=(-1000.0), minBox, maxBox, val=0, valBox, synth, m, n, bottom,
			playSynthFunction, cmdPeriodFunction;
			
		title = title ? "bus %".format( index );
		
		m = 0.0; n=0.0;
		w = SCWindow( title, Rect( 0, 0, 510, 510 ), scroll:true, resizable:false );
		w.view.hasHorizontalScroller = false;

		listArray = Array.fill(200,0.0) ! numChannels;
		
		playSynthFunction = {
			{ SharedOut.kr( this.index, this.kr ) }.play(target: server);
		};
		synth = playSynthFunction.();
		
		ms = Array.newClear( numChannels );
		maxBox = Array.newClear( numChannels );
		minBox = Array.newClear( numChannels );
		valBox = Array.newClear( numChannels );

		min = 0 ! numChannels;
		max = 0 ! numChannels;
		
		numChannels.do({
			| i |
			var comp;
			var margin = 5;
			var y = (i*121);
			comp = SCCompositeView( w, Rect( 0, y, 500, 120) )
				.resize_(2)
				.background_(Color.grey);
			SCStaticText( comp, Rect( 20, 40, 350, 40 ))
				.font_( Font("Helvetica-Bold", 34) )
				.stringColor_( Color.grey(0.2) )
				.string_( names.notNil.if({ names[i] }, { i + index }) );
			ms[i] = SCMultiSliderView( comp, Rect( 0, 0, 400, 120).insetBy(margin,margin) )
				.value_(listArray)
				.xOffset_(2)
				.drawLines_(true)
				.thumbSize_(1)
				.indexThumbSize_(0)
				.valueThumbSize_(0)
				.resize_(2);
			maxBox[i] = SCDragSink( comp, Rect(400, 0, 100, 24).insetBy(margin,margin))
				.mouseDownAction_({ |obj| max[i]=(-1000.0) })
				.string_(" " + 0.asString)
				.resize_(3);
				
			minBox[i] = SCDragSink( comp, Rect(400, 120-24, 100, 24).insetBy(margin,margin))
				.mouseDownAction_({ |obj| min[i]=(1000.0) })
				.string_(" " + 0.asString)
				.resize_(3);
			
			valBox[i] = SCDragSink( comp, Rect(400, 60-7, 100, 24).insetBy(margin,margin))
				.string_(" " + 0.asString)
				.stringColor_(Color.green)
				.resize_(3);
				
			bottom = comp.bounds.top + comp.bounds.height;
		});
		w.bounds = w.bounds.height_( max( min( bottom+10, 510 ), 60 ) );
		
		routine =  SkipJack({
					numChannels.do({
						| i |
						var aMin, aMax;
						if( server.inProcess, {
							val = server.getSharedControl(this.index+i);
						},{
							"Non-internal servers not yet supported.".throw;
						});
						if( val > max[i], {max[i] = val});
						if( val < min[i], {min[i] = val});
						minBox[i].string_( " " + min[i].asString[0..7] ); 
						maxBox[i].string_( " " + max[i].asString[0..7] );
						valBox[i].string_(" " + val.asString[0..7] );
						listArray[i] = listArray[i].copyRange(1, 198) ++ [val];
						ms[i].value_( (listArray[i]-min[i])/(max[i]-min[i]) );
					})
				}.defer,
				dt: 0.1,
				name: "debugScope"
			);
		routine.start;
		
		cmdPeriodFunction = {
			if(synth.notNil, {
				synth.free; 
				synth = nil;
			});
			synth = playSynthFunction.();
		};
		
		CmdPeriod.add(cmdPeriodFunction);


		w.onClose = {
			routine.stop; 
			synth.free;
			CmdPeriod.remove(cmdPeriodFunction);
		};
		
		w.front;
	}	
}

+NodeProxy {
	debugScope {
		this.bus.debugScope();
	}
}