/*
	w= Window().front;
	m = MVC_ModalWindow(w, 195@90);
	m.front;
	v = m.scrollView;
	m.close;
	w.close;
*/

// TO DO: adapt into proper MVC style window + add adaption for Windows & Linux

MVC_ModalWindow{

	var <parent, <mvc_parentWindow, <window, <pointSize, <scrollView, <>onClose, <parentViews;


	*new{|parent,pointSize,colors| ^super.new.init(parent,pointSize,colors) }

	init{|argParent,argPointSize,colors|
		// var bounds;
		parentViews = [this];

		if (argParent.isKindOf(MVC_Window)) {
			parent = argParent.view;
			mvc_parentWindow = argParent;
		}{
			parent = argParent;
			mvc_parentWindow = nil;
		};

		pointSize=argPointSize;
		colors = (
			background:		Color(59/77,59/77,59/77),
			border2:		Color(6/11,42/83,29/65),
			border1:		Color(3/77,1/103,0,65/77)
		) ++ (colors?());

		// the window   w = 195   h = 90
		window = ModalSheet(parent, pointSize)
			.alwaysOnTop_(true)
			.onClose_{ onClose.value };
		window.view.background_(colors[\border1]);

		// scroll view for instruments
		scrollView = MVC_CompositeView(window, Rect(11,11,pointSize.x-22,pointSize.y-22))
			.color_(\background,colors[\background])
			.hasBorder_(false);
		//	.autoScrolls_(true)
		//	.hasVerticalScroller_(true)

		// candy
		MVC_RoundBounds(window, Rect(11,11,pointSize.x-22,pointSize.y-22))
			.width_(6)
			.color_(\background,colors[\border2]);

		this.front;

		mvc_parentWindow.addDependant(this); // when parent window is closed this will be updated

	}

	// update comes from closing MVC_Window (s)
	update{|theChanged, theChanger|
		if (theChanger==\windowClosed) {

			this.close;
		};
	}

	isClosed{ ^window.isClosed }
	isOpen{^this.isClosed.not}

	front { window.front }

	close{
		mvc_parentWindow.removeDependant(this);
		window.close
	}

}

ModalSheet : Window {

	*new {|parent, size|
		var b, pb;
		pb = {parent.bounds}.try ?? {Window.screenBounds.insetBy(100)};
		b = Rect(
			pb.left + ((pb.width-size.x)/2),
			pb.bottom - size.y,
			size.x,size.y);
		^super.new(parent.name ? "", b, border: false);
	}

}