
// multi-lined dialog view. Created because TextView cannot scroll
// to the bottom of the text and hence is useless for dialog purposes.
// use .addText("Another line")

MVC_DialogView : MVC_View {

	var <dialog, lastDisplayTime, <>lines=6, <string;

	// set your defaults
	initView{
		colors=colors++(
			'background'	: Color(0.05,0.05,0.1),
			'string'     	: Color(1,1,1)
		);
		canFocus=false;
		if (string.isNil) {string=""};
		font=Font("Helvetica", 12);
		dialog=[];

	}

	// make the view
	createView{
		view=TextView(window,rect)
			.string_(string)
			.editable_(false)
			.font_ (font)
			.background_(colors[\background])
			.stringColor_(colors[\string]);
	}

	addControls{ }

	// set the colour in the Dictionary
	// need to do disable here
	color_{|index,color|
		colors[index]=color;
		if (index=='string'     ) { {if (view.notClosed) { view.stringColor_(color) } }.defer };
		if (index=='background' ) { {if (view.notClosed) { view.background_(color)  } }.defer };
	}

	dialog_{|d,flash=false|
		var str="";
		dialog=d;
		lines.clip(0,dialog.size).do({|j|
			if (j==0) {
				str=str++dialog[dialog.size-  lines.clip(0,dialog.size) + j ];
			}{
				str=str++"\r"++dialog[dialog.size-  lines.clip(0,dialog.size) + j ];
			};
		});
		string=str;
		if (view.notClosed) {
			view.string_(string);
			MVC_LazyRefresh.incRefresh;
			if (flash) {
				if ((lastDisplayTime.isNil) or:{(SystemClock.seconds-lastDisplayTime)>20}) {
					view.background_(colors[\string])
						.stringColor_(colors[\background]);
					{view.background_(colors[\background])
						.stringColor_(colors[\string])}.defer(0.2);
				};
			};
		};
	}

	clear{ this.dialog_([]) }

	addText{|t,flash=false|

		var stringList, toAdd, i, width, str;

		width=w-10;

		stringList=t.split($ ).collect({|i| i++" "});
		if (stringList.size>0) {
			stringList[stringList.size-1]=stringList.last.drop(-1);
		};

		while ( { (stringList.size)>0  },{

			toAdd="";

			i = 0;
			while ( { (i < (stringList.size)) && (toAdd.bounds(font).width<width) },{
				toAdd=toAdd++stringList[i];
				i = i + 1;
				if (toAdd.bounds(font).width>=width) { i=i-1 };
			});

			i=i.clip(0,stringList.size);

			if (i>0){
				toAdd="";
				i.do({|j|
					toAdd=toAdd++stringList[0];
					stringList.removeAt(0);
				});
				dialog=dialog++[toAdd];
			}{
				//else string to big for 1 line
				// this still needs breaking up
				toAdd=stringList[0];
				stringList.removeAt(0);
				dialog=dialog++[toAdd];
			};

		});

		str="";

		lines.clip(0,dialog.size).do({|j|
			if (j==0) {
				str=str++dialog[dialog.size-  lines.clip(0,dialog.size) + j ];
			}{
				str=str++"\r"++dialog[dialog.size-  lines.clip(0,dialog.size) + j ];
			};
		});

		string=str;

		if (view.notClosed) {
			view.string_(string);
			if (flash) {
				if ((lastDisplayTime.isNil) or:{(SystemClock.seconds-lastDisplayTime)>20}) {
					view.background_(colors[\string])
						.stringColor_(colors[\background]);
					{view.background_(colors[\background])
						.stringColor_(colors[\string])}.defer(0.2);
				};
			}
		};

		lastDisplayTime=SystemClock.seconds;
	}

}
