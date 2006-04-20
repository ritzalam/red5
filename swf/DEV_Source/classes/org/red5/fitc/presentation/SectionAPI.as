// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **

import com.blitzagency.data.ContentFarm;
import org.red5.samples.livestream.videoconference.Connection;
import org.red5.samples.livestream.videoconference.Subscriber;

class org.red5.fitc.presentation.SectionAPI extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.fitc.presentation.SectionAPI;
	public static var LINKAGE_ID:String = "org.red5.fitc.presentation.SectionAPI";
// Public Properties:
	public var connected:Boolean;
// Private Properties:
	private var connection:Connection;
	private var subscribe:Subscriber;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var api:MovieClip;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function SectionAPI() {}
	private function onLoad():Void { configUI(); }

// Public Methods:
	function transitionIn():Void
	{
		//content.htmlText = ContentFarm.getContent("section_0");
		_visible = true;
		gotoAndPlay("transitionIn");
	}
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		subscribe = api.subscribe;
		hide();
	}
	
	private function connect():Void
	{
		connection = new Connection();
		connected = connection.connect("rtmp://fancycode.com/fitcDemo");
		
		subscribe.subscribe("api_presentation", connection);
		subscribe.setUserName("Luke Hubbard - Red5");
		_global.tt("connected?", connected);
	}
	
	private function hide():Void
	{
		//content.htmlText = "";
		connection.close();
		subscribe.streamStop();
		_visible = false;
		gotoAndStop(1);
	}
}