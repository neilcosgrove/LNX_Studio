
// LNX_SeqView & LNX_VelocityView (use zero value)
// zero value works but is a little messy, tidy up later
/*
(
w=MVC_Window();
MVC_Scale(w,Rect(10,10,20,200));
w.create;
)
*/

MVC_Scale : MVC_View {

	var <>right=false, <>direction=\vertical, <>marks = 10;

	initView{
		colors=colors++(
			'background'	: Color.black.alpha_(0.4),
			'marks'		: Color.black
		);
		if (w>h) { direction=\horizontal }{ direction=\vertical };

	}

	// set the bounds, account for direction
	bounds_{|argRect|
		rect=argRect;
		l=rect.left;
		t=rect.top;
		w=rect.width;
		h=rect.height;
		if (w>h) { direction=orientation=\horizontal }{ direction=orientation=\vertical };
		if (view.notClosed) { view.bounds_(rect) };
		this.adjustLabels;
	}

	createView{

		view=UserView.new(window,rect)
			.drawFunc={|me|
				var val, r,c, h2;
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };

				Pen.use{
					Pen.smoothing_(false);
					colors[\background].set;

					Pen.fillRect(Rect(0,0,w,h));

					Pen.strokeColor = colors[\marks];

					h2=(h-1)/(marks);

					(marks).do{|i|

						Pen.line( 0@(h2*i+1), w@(h2*i+1));
					};
					Pen.fillStroke;


				};
			};
	}




	addControls{

		// i should do this everywhere in own method
		view.onClose_{|me|
			onClose.value(this);
			me.drawFunc_(nil).mouseDownAction_(nil).mouseMoveAction_(nil).onClose_(nil);
		};

		view.mouseDownAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			startX=x;
			startY=y;
		//	mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			if (editMode||viewEditMode) {lw=lh=nil; if (verbose==verbose) {view.bounds.postln} };
			if (modifiers==524576)	{buttonNumber = 1.5    };
			if (modifiers==262401)	{buttonNumber = 2      };
			if (buttonNumber==2)	{this.toggleMIDIactive };
			buttonPressed=buttonNumber; // store for move
		};

		view.mouseMoveAction={|me, x, y, modifiers, buttonNumber, clickCount|
			// mods 256:none, 131330:shift, 8388864:func, 262401:ctrl, 524576:alt, 1048840:apple
			if (editMode||viewEditMode) {
				this.moveBy(x-startX,y-startY,buttonPressed);
			};
		};

	}



	}
