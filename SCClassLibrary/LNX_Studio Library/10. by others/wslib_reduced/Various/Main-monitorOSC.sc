// wslib 2011
// quick way of monitoring all incoming osc messages
// exclude can be an array of Symbols with extra messages to exclude (i.e. not post)

+ Main {
	monitorOSC { |bool = true, exclude|
		if( bool == true ) {
			recvOSCfunc = { |time, addr, msg|
				if( ([ 
				'/status.reply', 
				'/localhostOutLevels', 
				'/localhostInLevels' ] 
					++ exclude.asCollection )
				.includes( msg[0] ).not ) {
					[ time.asSMPTEString, addr, msg ].postln;
				};
			};	
		} {
			recvOSCfunc = nil;
		};
	}
}
