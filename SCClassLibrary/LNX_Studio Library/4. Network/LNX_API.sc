
// Application Programming Interface //////////////////////////////////
// extreme testing
/*
a.api.sendClumpedList(\postMe, (0.1..100000.9) ); // yes !!
a.api.sendClumpedList(\postList, {1.0.rand2}!1000000 ); // yes !!
*/
// (1:23) = 45 secs to send (22.67sec @44.1k)
// 1.98 sec to send 1 sec on 192.168.0.1
//////////////////////////////////////////////////////////////////////


LNX_API{

	var object, id, interface1, interface2, type; // private

	*initClass{ Class.initClassTree(LNX_Protocols) }

	*new{ "You can't create a new API".error; ^nil }

	// add an object and make it's methods avalible
	*newTemp{|object, id, primaryInterface, secondaryInterface|
		^super.new.init(object,id,primaryInterface, secondaryInterface) }

	*newPermanent{|object,id,primaryInterface, secondaryInterface|
		^super.new.initPermanent(object,id,primaryInterface, secondaryInterface) }

	*newMessage{|object,id,primaryInterface, secondaryInterface|
		^super.new.initMessage(object,id,primaryInterface, secondaryInterface) }

	// outgoing messages /////////////////////////////////////////////////////////////

	// all these messages can go missing
	send             {|method ...args| LNX_Protocols.send             (id,method,*args)}
	sendND           {|method ...args| LNX_Protocols.sendND           (id,method,*args)}
	sendList         {|method,   list| LNX_Protocols.sendList         (id,method, list)}
	sendTo   {|userID, method ...args| LNX_Protocols.sendTo    (userID,id,method,*args)}
	sendToND {|userID, method ...args| LNX_Protocols.sendToND  (userID,id,method,*args)}
	sendDelta  {|delta,method ...args| LNX_Protocols.sendDelta  (delta,id,method,*args)}
	hostCmd          {|method ...args| LNX_Protocols.hostCmd          (id,method,*args)}

	// all these will be delivered (Guaranteed)
	sendOD           {|method ...args| LNX_Protocols.sendOD           (id,method,*args)}
	sendODND         {|method ...args| LNX_Protocols.sendODND         (id,method,*args)}
	sendGD           {|method ...args| LNX_Protocols.sendGD           (id,method,*args)}
	sendVP       {|vpid,method...args|
	                     LNX_Protocols.sendVP((id++"_vp_"++vpid).asSymbol,id,method,*args)}
	sendToGD {|userID, method ...args| LNX_Protocols.sendToGD  (userID,id,method,*args)}
	sendToOD {|userID, method ...args| LNX_Protocols.sendToOD  (userID,id,method,*args)}
	sendDeltaOD{|delta,method ...args| LNX_Protocols.sendDeltaOD(delta,id,method,*args)}
	sendClumpedList  {|method,list,compress=true|
					LNX_Protocols.sendClumpedList(id,method, list,false,compress)}
	hostCmdClumpedList{|method,list,compress=true|
					LNX_Protocols.hostCmdClumpedList(id,method,list,compress)}
	hostCmdGD        {|method ...args| LNX_Protocols.hostCmdGD        (id,method,*args)}

	groupCmdOD       {|method ...args| LNX_Protocols.groupCmdOD       (id,method,*args)}
	groupCmdSync     {|method ...args| LNX_Protocols.groupCmdSync     (id,method,*args)}

	// nb sendDelta is designed to work only from the host

	/*
	/////////////////////////////////////////////////////////////////////////////////////////

	METHOD               WHO                         ARGS                   COMMENTS
	send               : others recieve  this.method(arg1,arg2...)          defers
	sendND             : others recieve  this.method(arg1,arg2...)          no defer
	sendList		     : others recieve  this.method(list)
	sendTo             : user   recieves this.method(arg1,arg2...)
	sendToND           : user   recieves this.method(arg1,arg2...)          no defer
	sendDelta          : users  recieve  this.method(delta,arg1,arg2...)    from host only
	hostCmd		     : host   recieves this.method(userID,arg1,arg2...)   with userID

	sendOD             : same as send but Guaranteed in the Order sent
	sendGD             : same as send but Guaranteed, order may change
	sendVP             : send a variable message with a final value sent via sendOD (needs id)
	sendToGD           : same as sendTo but Guaranteed
	sendToOD           : same as sendTo but Guaranteed in the Order sent
	sendDeltaOD        : same as sendDelta but Guaranteed in the Order sent
	sendClumpedList    : others recieve  this.method(list)
	hostCmdClumpedList : send a clumped command list to the host. Guaranteed.
	hostCmdGD          : same as hostCmd but Guaranteed

	groupCmdOD		: everyone will do command in order via host
	groupCmdSync		: everyone will do command in order via host at the same time

	/////////////////////////////////////////////////////////////////////////////////////////
	*/

	init{|argObject,argID,argInterface1,argInterface2|
		object=argObject;
		id=argID;
		interface1=argInterface1;
		interface2=argInterface2;
		type=\object;
		LNX_Protocols.registerObject(this,id); // register this
	}

	initPermanent{|argObject,argID,argInterface1,argInterface2|
		object=argObject;
		id=argID;
		interface1=argInterface1;
		interface2=argInterface2;
		type=\permanent;
		LNX_Protocols.registerPermanentObject(this,id); // register this
	}

	initMessage{|argObject,argID,argInterface1,argInterface2|
		object=argObject;
		id=argID;
		interface1=argInterface1;
		interface2=argInterface2;
		type=\message;
		LNX_Protocols.registerMessage(this,id); // register this
	}

	// free it

	free{
		case {type==\object} {
			LNX_Protocols.removeObject(id);
			object = id = interface1 = interface2 = nil;
		} {type==\permanent} {
			"you can't remove a permanent object from the API".warn
		} {type==\message} {
			LNX_Protocols.removeMessage(id);
			object = id = interface1 = interface2 = nil;
		}
	}

	// incoming messages /////////////////////////////////////////////////////////////

	performAPI{|msg|
		if ((interface1.isNil)or:{interface1.includes(msg[0])}) {
			object.perform(*msg)
		}{
			if ((interface2.notNil)and:{interface2.includes(msg[0])}) {
				object.perform(*msg)
			}{
				this.illegalMethod(object,msg[0]);
			};
		}
	}

	performAPIList{|method,list|
		if ((interface1.isNil)or:{interface1.includes(method)}) {
			object.performList(method,list)
		}{
			if ((interface2.notNil)and:{interface2.includes(method)}) {
				object.performList(method,list);
			}{
				this.illegalMethod(object,method);
			}
		}
	}

	performAPIMsg{|method,msg|
		if ((interface1.isNil)or:{interface1.includes(method)}) {
			object.perform(method,*msg)
		}{
			if ((interface2.notNil)and:{interface2.includes(method)}) {
				object.perform(method,*msg)
			}{
				this.illegalMethod(object,method);
			}
		}
	}

	performAPIMsgArg1{|method,arg1,msg|
		if ((interface1.isNil)or:{interface1.includes(method)}) {
			object.perform(method,arg1,*msg)
		}{
			if ((interface2.notNil)and:{interface2.includes(method)}) {
				object.perform(method,arg1,*msg)
			}{
				this.illegalMethod(object,method);
			}
		}
	}

	performAPIClump{|method,list|
		if ((interface1.isNil)or:{interface1.includes(method)}) {
			object.perform(method,list)
		}{
			if ((interface2.notNil)and:{interface2.includes(method)}) {
				object.perform(method,list)
			}{
				this.illegalMethod(object,method);
			}
		}
	}

	illegalMethod{|object,method|
		var s="API call to ";
		if (object.class.isMetaClass) { s=s++"Meta_"};
		(s++(object.class.name)++":"++(method.asString)++" is Illegal.").warn;
	}

	isConnected{ ^LNX_Protocols.isConnected }

	uid{ ^LNX_Protocols.uid }

}
