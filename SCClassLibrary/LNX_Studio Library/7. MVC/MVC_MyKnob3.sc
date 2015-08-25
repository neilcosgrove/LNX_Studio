
// LNX_MyKnobView

/*(
w=MVC_Window();
128.do{|i|
MVC_MyKnob3(w,Rect(i%16*40+20,i.div(16)*60+20,30,30)).value_(1.0.rand).label_((i+1).asString);
};
w.create;
)*/

MVC_MyKnob3 : MVC_MyKnob {
	
	initView{
		// 0: Color.ndcKnobOn, 1: Color.ndcKnobOff, 2: Color.ndcKnobPin, 3: Color.ndcKnobText
		colors=colors++(			
			'on' 	: Color.ndcKnobOn,
			'off'	: Color.ndcKnobOff,
			'pin'	: Color.ndcKnobPin
		);
		
		isSquare=true;
		this.numberFunc_(\float2);
	}
	
	createView{
		view=UserView.new(window,rect)
			.drawFunc={|me|
				var di,active,col,val,val2;
				var radius,offset,circum,zeroClipped;
				var x1,x2,x3;
				var pW;
				
				if (showLabelBackground) { Color(0,0,0,0.2).set; Pen.fillRect(Rect(0,0,w,h)) };
				
				if (verbose) { [this.class.asString, 'drawFunc' , label].postln };
				
				// useful numbers for working out the angles
				radius=w/2;
				offset=2pi*21/64;
				circum=2pi*54/64;

				zeroClipped=zeroValue.clip(-2pi,2pi);

				if (penWidth==\auto) {
					pW=w*0.11538461538462;
				}{
					pW=penWidth;
				};
				
				// unmap both values
				if (controlSpec.notNil) {
					val=controlSpec.unmap(value);
				}{
					val=value.clip(0,1);
				};
				
				val2=0;

				// left & right values	clipped
				x1=(val-val2).clip(0,1);
				x2=(val+val2).clip(0,1);
				x3=val.clip(0,1);
				
				// what colour should it be
				active= (midiLearn or: {midiLearn2} or: {enabled.not} ).not;
				if (active.not) {
					col= midiLearn.if(\midiLearn,\disabled);
					if (midiLearn2) {col=\midiLearn2};
				};
									
				Pen.use{
				
					Pen.smoothing_(true);	
					
					// pin to outer circle dark line
					colors[active.if(\off,col)].set;
					Pen.width_(5);
					Pen.addWedge(
						(radius)@(radius), 
						radius,  	
						2pi*(val.map(0,1,0.3295,1.1705)),
						0
					);
					Pen.perform(\stroke);
					
					colors[active.if(\off,col)].set;
					di=circum*zeroClipped;
		
					// right side
					Pen.addAnnularWedge(
						(radius)@(radius), 
						radius-pW, 
						radius, 	
						offset+(circum*(val.clip(zeroClipped,1))), 
						circum-(circum*(val.clip(zeroClipped,1)))
					);
					// left side
					Pen.addAnnularWedge(
						(radius)@(radius), 
						radius-pW, 
						radius, 	
						offset+(circum*(0.clip(0,zeroClipped))), 
						circum*(val.clip(0,zeroClipped))
					);
					Pen.perform(\fill);			
					colors[active.if(\on,col)].set;
					// active section (zero value to value)
					di=circum*zeroClipped;
					Pen.addAnnularWedge(
						(radius)@(radius), 
						radius-pW, 
						radius, 	
						offset+di, 
						circum*val-di
					);
					Pen.perform(\fill);
					
					// innner fill background
					colors[active.if(\on,col)].darken(0.25).set;
					Pen.addWedge(
						(radius)@(radius), 
						radius-3, 	
						offset, 
						circum
					);
					Pen.perform(\fill);

					// outter ring on
					colors[active.if(\on,col)].set;
					di=circum*x1;
					Pen.addAnnularWedge(
						(radius)@(radius), 
						radius-3, 
						radius, 	
						offset+di, 
						circum*x2-di
					);
					Pen.perform(\fill);
					
					// line from pin to outter ring, left
					colors[active.if(\on,col)].set;
					Pen.width_(2);
					Pen.addWedge(
						(radius)@(radius), 
						radius,  	
						2pi*(x1.map(0,1,0.3295,1.1705)),
						0
					);
					
					// line from pin to outter ring, right
					Pen.addWedge(
						(radius)@(radius), 
						radius,  	
						2pi*(x2.map(0,1,0.3295,1.1705)),
						0
					);
					Pen.perform(\stroke);	
												
					// centre pin
					colors[active.if(colors[\pin].notNil.if(\pin,\on),col)].set;
					Pen.addWedge(
						(radius)@(radius), 
						4, 	
						0, 
						2pi
					);
					Pen.perform(\fill);
									
				}; // end.pen
						
			};
			
	}
}