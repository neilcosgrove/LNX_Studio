// adjustment to new menu system so I can dictate the order of menus

+ MainMenu {

	*initClass {
		applicationMenu = LNX_AppMenus.menus;
		registered = ();
		this.clear();
	}

	*prUpdate {
		var menus = registered.collect {|groups, name|
			var menu = Menu().title_(name.asString);
			groups.do {|group, groupName|
				menu.addAction(Action.separator.string_(groupName));
				group.do {|action|
					menu.addAction(action.asAction)
				}
			};
			menu
		};
		var actualotherMenus = (applicationMenu ++ menus).asArray();
		this.prSetAppMenus(actualotherMenus);
	}

}