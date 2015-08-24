+ SimpleMIDIFile {
	
	p { |inst| // experimental
		var thisFile;
		inst = ( inst ? 'default' ).asCollection;
		if( timeMode == 'seconds' )
			{ thisFile = this }
			{ thisFile = this.copy.timeMode_( 'seconds' ); };
		 ^Ppar(
			({ |tr|
				var sustainEvents, deltaTimes;
				sustainEvents = thisFile.noteSustainEvents( nil, tr );
				if( sustainEvents.size > 0 )
					{ 
					deltaTimes = sustainEvents.flop[1].differentiate;
					Pbind(
						\instrument, inst.wrapAt( tr + 1 ),
						\dur, Pseq( deltaTimes ++ [0], 1 ),
						\midinote, Pseq( [\rest] ++ sustainEvents.flop[4], 1 ),
						\amp, Pseq( [0] ++ ( sustainEvents.flop[5] / 127 ) * 0.1, 1 ),
						\sustain, Pseq( [0] ++ sustainEvents.flop[6], 1 )
						)   
					}
					{ nil }
				}!this.tracks).select({ |item| item.notNil })
			);
		}
	
	}