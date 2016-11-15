// A global choke object
/*
LNX_GlobalChoke.globalChokes;
*/

LNX_GlobalChoke{

	classvar <globalChokes, <noOfChokes=8; // 8 should be enough

	var <dependants;

	*initClass{ globalChokes = { this.new } ! noOfChokes }

	*new { ^super.new.init }

	init{ dependants = IdentitySet[] }

	// add an object or function
	*addDependant{|i,object| globalChokes[i].addDependant(object)}
	addDependant{|object| dependants.add(object) }

	// remove an object or function
	*removeDependant{|i,object| globalChokes[i].removeDependant(object)}
	removeDependant{|object| dependants.remove(object) }

	// choke a choke group from one of the dependants
	*coke{|i,sendingObject| globalChokes[i].choke(sendingObject) }
	choke{|sendingObject|
		dependants.do{|object|
			if (object!=sendingObject) {
				if (object.isFunction) {
					object.value;		// use a function
				}{
					object.releaseAll;	// or use Inst template via releaseAll
				};
				// can choose between release or kill
				// and maybe another choice object.stopAllNotes; // or killAll (would only work on SCCode & BumNote)
			}
		}
	}

	// free ME! (used during close / open song. just incase anything left over from last track.)
	*free{ globalChokes.do(_.free) }
	free{ dependants = IdentitySet[] }

}

// end