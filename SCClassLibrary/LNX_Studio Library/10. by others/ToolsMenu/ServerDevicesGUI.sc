+ Server {
	deviceGui {
		var devs = ServerOptions.devices;
		var win = GUI.window.new(
			"I/O Device for % server".format(this.name), 
			Rect(128,512,400,50));
		var popup = GUI.popUpMenu.new(win, Rect(10,10,300,30))
			.items_(devs)
			.value_(devs.indexOfEqual(this.options.device) ?? 0);
		GUI.button.new(win, Rect(320,10,70,30))
			.states_([["OK"]])
			.action_({ 
				this.options.device = devs[popup.value]; 
				win.close;
				"***selected device: %\n".postf(devs[popup.value]);
			});
		win.front;
	}
	
	*deviceGuis {
	
		var servers = Server.set.asArray;
		var devsIn = ServerOptions.inDevices;
		var devsOut = ServerOptions.outDevices;
		var devsInNames = devsIn.collect({ |name|
			var b,a = name.as(List);
			a.remove(/*(*/$));
			a.remove($(/*)*/);
			a.as(String);
			
			}).asArray;
		var 	devsOutNames = devsOut.collect({ |name|
			var b,a = name.as(List);
			a.remove(/*(*/$));
			a.remove($(/*)*/);
			a.as(String);
			
			}).asArray;
		var w = Window("Sound Card Options",Rect(128,512,400,100),false).front;
		var win = TabbedView(w,Rect(0,0,400,90),["All"]++ Server.set.collect(_.name));		
		var popupIn = PopUpMenu.new(win.views[0], Rect(10,10,300,30))
			.items_(devsInNames)
			.value_(0);
			
		var popupOut = PopUpMenu.new(win.views[0], Rect(10,45,300,30))
			.items_(devsOutNames)
			.value_(0);
			
		Button.new(win.views[0], Rect(320,10,70,30))
			.states_([["OK"]])
			.action_({ 
				servers.do({ |server| 
					postln("updating server"++server);
					server.options.inDevice = devsIn[popupIn.value];
					server.options.outDevice = devsOut[popupOut.value]}); 
				//win.view.remove;
				"***selected in device: %\n".postf(devsIn[popupIn.value]);
				"***selected out device: %\n".postf(devsOut[popupOut.value]);

			});
		
		servers.do{ |server,i|
			var popupIn = PopUpMenu.new(win.views[i+1], Rect(10,10,300,30))
			.items_(devsInNames)
			.value_(0);
			
			var popupOut = PopUpMenu.new(win.views[i+1], Rect(10,45,300,30))
				.items_(devsOutNames)
				.value_(0);
				
			Button.new(win.views[i+1], Rect(320,10,70,30))
				.states_([["OK"]])
				.action_({ 
				
					server.options.inDevice = devsIn[popupIn.value];
					server.options.outDevice = devsOut[popupOut.value]; 
					//win.view.remove;
					"***selected in device: %\n".postf(devsIn[popupIn.value]);
					"***selected out device: %\n".postf(devsOut[popupOut.value]);
	
				});

		};
	^w.front;
		
	}
	
}