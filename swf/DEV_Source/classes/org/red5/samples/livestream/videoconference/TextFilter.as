import com.blitzagency.data.DecodeHTML;

class org.red5.samples.livestream.videoconference.TextFilter 
{
// Constants:
	public static var CLASS_REF = org.red5.samples.livestream.videoconference.TextFilter;
	public static var initialized:Boolean = initialize();
// Public Properties:
// Private Properties:

// Public Methods:
	public static function encodeText(p_msg:String):String
	{
		p_msg = DecodeHTML.decode(p_msg);
		return p_msg;
	}
// Semi-Private Methods:
// Private Methods:
	private static function initialize():Boolean
	{
		DecodeHTML.addStrings
		(
			new Array(
				{from: ":)", to: "<b>:)</b>"},
				{from: ";)", to: "<b>;)</b>"},
				{from: ":(", to: "<b>:(</b>"},
				{from: ";(", to: "<b>;(</b>"},
				{from: ":p", to: "<b>:p</b>"},
				{from: ";p", to: "<b>;p</b>"}
			)
		)
		trace("initialized");
		return true;
	}
	
	private static function bold(p_msg):String
	{
		return null;
	}
}