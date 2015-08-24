
+ SCContainerView {
	visible_ { arg bool;
		children.do({ arg obj;
			if ( obj.isKindOf(AppKitView) or:{ obj.isKindOf(SCContainerView) }) {
				obj.visible_(bool);
			}{
				if ( obj.isKindOf(FlowView) ) {
					obj.view.visible_( bool );
				};
			};
		});
		super.visible_(bool)
	}
	enabled_ { arg bool;
		children.do({ arg obj;
			if ( obj.isKindOf(AppKitView) or:{ obj.isKindOf(SCContainerView) }) {
				obj.enabled_(bool);
			}{
				if ( obj.isKindOf(FlowView) ) {
					obj.view.enabled_( bool );
				};
			};
		});
		super.enabled_(bool)
	}
}

