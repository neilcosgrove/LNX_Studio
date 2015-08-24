
// an onSolo combines 2 things for an instrument or channel..  1. on/off mute 2. solo

LNX_OnSolo {

	var	<>onSoloNo, <>on=1, <solo=0, <group, <id;

	*new { |group,on=0,solo=0,instID| ^super.new.init(group,on,solo,instID) }
	
	init { |argGroup,argOn,argSolo,argInstID|
		id=argInstID;
		group=argGroup;
		on=argOn;
		solo=argSolo;	
		group.add(this);
	}
	
	isOn {^(this.onOff==1)}
	
	isOff{^(this.onOff==0)}
		
	solo_{|s|
		solo=s;
		group.updateIsSoloOn;
	}
	
	onOff { if (group.isSoloOn) { ^solo } { ^on } } // main method call. Am i on/playing/active?
	
	isSoloOn { ^group.isSoloOn }
	
	free {
		group.remove(this);
		onSoloNo = on = solo = group = id = nil;
		^nil
	}
	
	onSolo{^[on,solo]}
	
	groupSongOnOff{
		if (group.isGroupSongSoloOn) {
			^group.userInstSolo('lnx_song',id)
		}{
			^group.userInstOn('lnx_song',id)
		}
	}
	
}

// the container group

// there is a problem with loading over previous songs that have an active solo, solo is not reset

LNX_OnSoloGroup{

	var <onSolos, <isSoloOn=false, <usersOnSoloList, <isGroupSongSoloOn=false;
	
	*new { ^super.new.init }
	
	init {
		onSolos=IdentityDictionary[];
		usersOnSoloList=IdentityDictionary[];
	}
	
	clear{
		isSoloOn=false;
		isGroupSongSoloOn=false;
	}
		
	add{|onSolo|
		if (onSolo.solo==1) { isSoloOn=true };
		// now add to users
		if (onSolo.id.notNil) {
			onSolos[onSolo.id]=onSolo;
			usersOnSoloList.do{|userID| userID[onSolo.id]=[onSolo.on,onSolo.solo] };
		};
	}
	
	updateIsSoloOn {
		isSoloOn=false;
		onSolos.do({|os| if (os.solo==1) { isSoloOn=true } })
	}
	
	remove{|onSolo|
		onSolos[onSolo.id]=nil;
		usersOnSoloList.do{|userID| userID[onSolo.id]=nil };
		this.updateIsSoloOn	
	}
	
	/// for users
	
	addUser{|userID|
		var userOnSolo=IdentityDictionary[];
		onSolos.do{|onSolo| userOnSolo[onSolo.id]=[onSolo.on,onSolo.solo] };
		usersOnSoloList[userID]=userOnSolo;
	}
	
	removeUser{|userID| usersOnSoloList[userID]=nil}
	
	userInstOn_  {|userID,instID,val| usersOnSoloList[userID][instID][0]=val }
	
	userInstSolo_{|userID,instID,val|
		usersOnSoloList[userID][instID][1]=val;
		this.updateIsGroupSongSoloOn
	}
	
	userInstOn   {|userID,instID| ^usersOnSoloList[userID][instID][0] }
	
	userInstSolo {|userID,instID| ^usersOnSoloList[userID][instID][1] }

	updateIsGroupSongSoloOn {
		isGroupSongSoloOn=false;
		usersOnSoloList['lnx_song'].do({|list| if (list[1]==1) { isGroupSongSoloOn=true } })
	}

}
