
// get the mouse pos in SCApp using the Mouse UGens, for Windows & Linux

LNX_MouseXY{
	
	classvar <pos, dependants, server;
	
	*initClass{
		var sbW = Window.screenBounds.width;
		var sbH = Window.screenBounds.height;
		pos = ((0.5*sbW)@(0.5*sbH));
		dependants=IdentitySet[];
		
		OSCresponder(nil, 'mouseXY', { |t, r, msg|
			pos = ( ((msg[3]*sbW).asInt) @ ((msg[4]*sbH).asInt) );
			
			dependants.do{|d| d.value(pos.x,pos.y)};
		}).add;
	}
	
	*add{|dependant| dependants.add(dependant) }
	
	*remove{|dependant| dependants.remove(dependant) }
	
	*active{^server.serverRunning}
	
	*startDSP{|argServer,rate=2|
		server=argServer;
		{
			SendReply.kr(Impulse.kr(rate), 'mouseXY',
				[MouseX.kr(0,1,0,0),MouseY.kr(0,1,0,0)], 99999);
		}.play(server);	
	}
	
}
