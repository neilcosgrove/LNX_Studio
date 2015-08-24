/*

//// obsolete
+ UGen {
	sin {
		case { rate === 'audio' }
			{ ^SinOsc.ar( 0, this ) }
			{ rate === 'control' }
			{ ^SinOsc.kr( 0, this ) }
			{ true }
			{ ^super.cos };
		}
	cos {
		case { rate === 'audio' }
			{ ^SinOsc.ar( 0, this + 0.5pi ) }
			{ rate === 'control' }
			{ ^SinOsc.kr( 0, this + 0.5pi ) }
			{ true }
			{ ^super.cos };
		}
	}
*/

+ Object {
		
	fadeOut { |center = 0, range = 0.5, silent = 0.25| // range should not be 0
		^( (this-center).excess( silent ).abs.min( range ) / range );
		}
	
	cosFadeOut { |center = 0, range = 0.5, silent = 0.25| // range should not be 0
		^( this.fadeOut( center, range, silent ) * 0.5pi).sin;
		}
		
	fadeIn { |center = 0, range = 0.5, on = 0.25| // range should not be 0
		^(1 - this.fadeOut( center, range, on ));
		}
			
	cosFadeIn { |center = 0, range = 0.5, on = 0.25|
		^( this.fadeOut( center, range, on ) * 0.5pi).cos;
		}
		
	}