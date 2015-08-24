Freesound2{

	classvar <base_uri 		      = "http://www.freesound.org/api";
	classvar <uri_sounds              = "/sounds/";
	classvar <uri_sounds_search       = "/sounds/search/";
	classvar <uri_sound               = "/sounds/%/";
	classvar <uri_sound_analysis      = "/sounds/%/analysis/";
	classvar <uri_sound_siilar        = "/sounds/%/similar/";	classvar <uri_users               = "/people/";
	classvar <uri_user                = "/people/%/";
	classvar <uri_user_sounds         = "/people/%/sounds/";
	classvar <uri_user_packs          = "/people/%/packs/";
	classvar <uri_packs               = "/packs/";
	classvar <uri_pack                = "/packs/%/";
	classvar <uri_pack_sounds         = "/packs/%/sounds/";
	classvar <>api_key;

	*uri{|uri,args|
		^(Freesound2.base_uri++uri.format(args));
	}
	
	*toIdentityDict{|node|	
		var newNode;
		node.class.switch(
			Event,{
				var dict = IdentityDictionary.new;
				node.keysValuesDo({|k,v|					
					dict.put(k.asSymbol,[Event,Array].includes(v.class).if(
						{Freesound2.toIdentityDict(v)},{v})
					)
				});
				newNode = dict;
			},
			Array,{
				newNode = node.collect({|item| 
					[Event,Array].includes(item.class).if(
						{Freesound2.toIdentityDict(item)},{item}
					)
				})
			}
		);
		^newNode;
	}
	*parseJSON{|jsonStr|
		var parsed = jsonStr;	
		var a,x;
		jsonStr.do({|char,pos|
			var inString = false;
			char.switch( 				
				$",{(jsonStr[pos-1]==$\ && inString).not.if({inString = inString.not})}, 
				${,{ if(inString.not){parsed[pos] = $(} },
				$},{ if(inString.not){parsed[pos] = $)} }				
			)
		});
		^Freesound2.toIdentityDict(parsed.interpret);
	}
}

FS2Req{
	*get{|uri,params|
		var paramsArray,paramsString,cmd, result,response;		
		if (params.isNil,{params = IdentityDictionary.new});
		params.put(\api_key,Freesound2.api_key);
		paramsArray=params.keys(Array).collect({|k|k.asString++"="++params[k].asString.urlEncode});
		paramsString=paramsArray.join("&");		paramsString.postln;
		cmd = "curl '"++uri++"?"++paramsString++"'";
		cmd.postln;
		result = cmd.postln.unixCmdGetStdOut.replace("\n","");
		response = Freesound2.parseJSON(result);
		^response;		
	}
	*retrieve{|uri,path,doneAction| //assuming no params for retrieve uris
		var cmd;
		uri = uri++"?api_key="++Freesound2.api_key;
		cmd = "curl %>'%'".format(uri,path);
		cmd.postln.unixCmd(doneAction);		
	}
}

FS2Obj : Dictionary{}

FS2Sound : FS2Obj{
	*get_sound{|soundId| ^FS2Sound.newFrom(FS2Req.get(Freesound2.uri(Freesound2.uri_sound,soundId)))}
	*search{|... params| ^FS2Req.get(Freesound2.uri(Freesound2.uri_sounds_search),*params)}
	retrieve{|path, doneAction|
		FS2Req.retrieve(this[\serve],path++"/"++this[\original_filename],doneAction);
	}
	retrieve_preview{|path, doneAction, quality="hq", format="mp3"|
		var key = "%-%-%".format("preview",quality,format).asSymbol;
		var fname = this[\original_filename].splitext[0]++"."++format;
		FS2Req.retrieve(this[key],path++"/"++fname,doneAction);
	}
	get_analysis{|filter, showAll=false|
		var url = Freesound2.uri(Freesound2.uri_sound_analysis,this[\id]);
		var params = nil;
		if(filter.notNil){url = url ++filter++"/"};
		if(showAll){params = ('all':1)};
		url.postln;
		^FS2Req.get(url,params);
	}
	retrieve_analysis_frames{|path, doneAction|
		var fname = this[\original_filename].splitext[0]++".json";
		FS2Req.retrieve(this[\analysis_frames],path++"/"++fname,doneAction);
	}
	get_similar{|preset="lowlevel", num_results=15|
		var params = ('preset':preset,'num_results':num_results);
		var url = Freesound2.uri(Freesound2.uri_sound_siilar,this[\id]);
		^FS2Req.get(url,params);
	}
}


+String{
	urlEncode{
		^this.collect({|c| if(c.isAlphaNum){c}{"%"++c.ascii.asHexString(2)}})
	}
}
