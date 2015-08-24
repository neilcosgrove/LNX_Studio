
(
Sheet({ arg layout;
	var i, b;
	t = TabbedView.new(layout, 400@200);
	
	t.views[0].flow({  arg layout;
		b = NSButton.new(layout, Rect(100,50,100,24));
		b.setTitle_("Animate");
		b.initAction("doAction:");
		b.action_({ if (b.state == 1) { i.startAnimation_(nil); }{ i.stopAnimation_(nil); } });
		b.setBezelStyle_(6);	// NSShadowlessSquareBezelStyle
		b.setButtonType_(1);

		i = NSProgressIndicator.new(layout, Rect(10,50,196,18));

		SCSlider.new(layout, 180@18);

		layout.background_(Color.blue(0.1,0.1));
		l = layout;
	});
	
}, "Sheet");
)

l.visible
l.visible = false;
l.visible = true;
l.children;

l.enabled;
l.enabled = false;
l.enabled = true;
