
// a starting template to work from

MVC_PlainSquare : MVC_View {

	// set your defaults
	initView{
		colors=colors++(
			'on'	: Color.green,
			'off'	: Color.red
		);
	}

	// make the view
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				MVC_LazyRefresh.incRefresh;
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				Pen.use{
					Pen.smoothing_(false);
					if (value>=0.5) { colors[\on].set; }  { colors[\off].set; };
					Pen.fillRect(Rect(0,0,w,h));
				}; // end.pen
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
			mouseDownAction.value(this, x, y, modifiers, buttonNumber, clickCount);
			if (editMode||viewEditMode) {lw=lh=nil; if (verbose==verbose) {view.bounds.postln} };
			if (modifiers.isAlt)	{buttonNumber = 1.5    };
			if (modifiers.isCtrl)	{buttonNumber = 2      };
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
