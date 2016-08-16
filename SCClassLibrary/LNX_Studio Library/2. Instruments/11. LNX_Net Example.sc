// a demo of lnx protocols
// for teaching purposes

LNX_NetExample : LNX_InstrumentTemplate {

	*new { arg server=Server.default,studio,instNo,bounds,open=true,id,loadList;
		^super.new(server,studio,instNo,bounds,open,id,loadList)
	}

	*studioName {^"NET Example"}
	*sortOrder{^2}
	isInstrument{^false}

	*isVisible{^false}

	header {
		// define your document header details
		instrumentHeaderType="SC NetExample Doc";
		version="v1.3";
	}

	// the models
	initModel {

		#models,defaults=[
			// 0.solo
			[0, \switch, (\strings_:"S"), midiControl, 0, "Solo",
				{|me,val,latency,send,toggle| this.solo(val,latency,send,toggle) },
				\action2_ -> {|me| this.soloAlt(me.value) }],

			// 1.onOff
			[1, \switch, (\strings_:((this.instNo+1).asString)), midiControl, 1, "On/Off",
				{|me,val,latency,send,toggle| this.onOff(val,latency,send,toggle) },
				\action2_ -> {|me| this.onOffAlt(me.value) }],

			// 2. ND
			[ [0,1,\lin,0.01,0] , midiControl, 2, "ND", (\label_:"ND", \numberFunc_:'float2'),
				{|me,val,latency,send| this.setP(2,val,latency,send) }],

			// 3. GD
			[ [0,1,\lin,0.01,0] , midiControl, 3, "GD", (\label_:"GD", \numberFunc_:'float2'),
				{|me,val,latency,send| this.setPGD(3,val,latency,send) }],

			// 4. OD
			[ [0,1,\lin,0.01,0] , midiControl, 4, "OD", (\label_:"OD", \numberFunc_:'float2'),
				{|me,val,latency,send| this.setPOD(4,val,latency,send) }],

			// 5. VP
			[ [0,1,\lin,0.01,0] , midiControl, 5, "VP", (\label_:"VP", \numberFunc_:'float2'),
				{|me,val,latency,send| this.setPVP(5,val,latency,send) }]

		].generateAllModels;

		// list all parameters you want exluded from a preset change
		presetExclusion=[0,1];
		randomExclusion=[0,1];
		autoExclusion=[];

	}

	// GUI

	*thisWidth  {^(210*4)+10}
	*thisHeight {^290}

	createWindow{|bounds|
		this.createTemplateWindow(bounds,Color(0.15,0.125,0.1,0.5));
	}

	// create all the GUI widgets while attaching them to models
	createWidgets{

		gui[\knob2Theme]=(	\labelFont_   : Font("Helvetica",40),
						\numberFont_	: Font("Helvetica",40),
						\numberWidth_ : 0,
						\colors_      : (\on : Color(1,0.5,0),
									   \numberDown : Color(1,0.5,0)/4),
						\resoultion_	: 1.5,
						\penWidth_	: 20);

		// 2.ND
		MVC_MyKnob(models[2],window,Rect(15+(210*0),50,190,190),gui[\knob2Theme]);
		// 3.GD
		MVC_MyKnob(models[3],window,Rect(15+(210*1),50,190,190),gui[\knob2Theme]);
		// 4.OD
		MVC_MyKnob(models[4],window,Rect(15+(210*2),50,190,190),gui[\knob2Theme]);
		// 5.VP
		MVC_MyKnob(models[5],window,Rect(15+(210*3),50,190,190),gui[\knob2Theme]);

	}

} // end ////////////////////////////////////
