
// the start up splash screen //////////////////////////////////

LNX_SplashScreen{

	classvar window, backgroundPath="screen shot 1.3.jpg", gui;

	*init {|studio|
		
		var image, rect, w, h, gap=5, gap2=2,texts, image2, adj;
		
		gui=IdentityDictionary[];
				
		{
			if (("SplashScreen".loadPref.isNil) and: {(window.isNil) or: {window.isClosed}} ) {
			
				image=SCImage.new(String.scDir +/+ backgroundPath);
				w=image.width/1.5;
				h=image.height/1.5;
			
				rect=Rect.aboutPoint(SCWindow.screenBounds.center,w/2, h/2)
					.insetBy(gap.neg,gap.neg);
			
				window = SCWindow("LNX_Studio"+(studio.version),rect,false,false)
					.alwaysOnTop_(true);

				window.view.background_(Color.black);
				
				UserView.new(window,Rect(gap,gap,w,h))
					.drawFunc_{|me|
						image.drawInRect(Rect(0,0,w,h),Rect(0,0,w*1.5,h*1.5), 2, 1.0)
					};
					
				image2=SCImage.new(String.scDir +/+ "LNX.jpg");	
				UserView.new(window,Rect(16-gap2,13-gap2,40+gap2+gap2,60+gap2+gap2))
					.drawFunc_{|me|
						Color.black.set;
						Pen.fillRect(Rect(0,0,40+gap2+gap2,60+gap2+gap2));
						image2.drawInRect(Rect(0+gap2,0+gap2,40,60),
							Rect(0,0,image2.width,image2.height), 2, 1.0);
					};
	
				texts=[
					"		LNX_Studio"+(studio.version.drop(1)) ,
					"",
					"		March 2016",
					"",
					"		           LNX_Studio - Digital Audio Workstation",
					"                                  (created in SuperCollider)",
					"",
					" Do you want to..."	
				];
			
				texts.do{|text,j|
				
					adj=#[7,0,-5,0,-2,-2,-2,0,0][j];
				
					if (text.size>0) {
						4.do{|i|
							StaticText(window,
								Rect(10+([-1,-1,1,1][i]),10+([-1,1,-1,1][i])+(j*14),
										w-10,20+adj))
								.string_(text)
								.font_(Font("Helvetica",15+adj))
								.stringColor_(Color.black)
								.align_(\left);
						};
							
						StaticText(window,Rect(10,10+(j*14),w-10,20+adj))
							.string_(text)
							.font_(Font("Helvetica",15+adj))
							.stringColor_(Color.white)
							.align_(\left);
					}
				};
		
		 		MVC_FlatButton(window,Rect(50,160,w/2.8,30), "Load demo song")
					.rounded_(true)
					.shadow_(true)
					.canFocus_(false)
					.color_(\up,Color(257/643,62/157,195/463))
					.color_(\down,Color(257/643,62/157,195/463)/2)
					.color_(\string,Color.white)
					.action_{					
						if (studio.canLoadSong) {
							studio.loadDemoSong;
							this.close;
					} };

				MVC_FlatButton(window,Rect((w/4*2)+20,160,w/2.8,30), "Open help")
					.rounded_(true)
					.shadow_(true)
					.canFocus_(false)
					.color_(\up,Color(257/643,62/157,195/463))
					.color_(\down,Color(257/643,62/157,195/463)/2)
					.color_(\string,Color.white)
					.action_{		
						"LNX_Studio Help".help;
						this.close;	
					};
					
				MVC_FlatButton(window,Rect((w/4)+35,160+40,w/2.8,30), "Don't show this again")
					.rounded_(true)
					.shadow_(true)
					.canFocus_(false)
					.color_(\up,Color(257/643,62/157,195/463))
					.color_(\down,Color(257/643,62/157,195/463)/2)
					.color_(\string,Color.white)
					.action_{	
					    ["Delete this file to show the splash screen."].savePref("SplashScreen");
						this.close;
					};
					
				window.front;
				{gui[\dummyWindow]=Window("",Rect(0,0,1,1)).front;}.defer(0.5);
				{window.front;}.defer(0.75);
				// all this to get window to close on click into studio window
			}
		}.defer;
		
	}
	
	*close{
		if ((window.notNil) and: {window.isClosed.not}) {
			window.close;
			gui[\dummyWindow].close;
			window=nil;
		}
	}

}

