// ** AUTO-UI IMPORT STATEMENTS **
import org.red5.utils.Connector;
// ** END AUTO-UI IMPORT STATEMENTS **
import com.neoarchaic.ui.Tooltip;
import org.red5.net.Stream;
//import org.red5.utils.Delegate;
import com.blitzagency.xray.util.XrayLoader;

class org.red5.samples.livestream.subscriber.Main extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.livestream.subscriber.Main;
	public static var LINKAGE_ID:String = "org.red5.samples.livestream.subscriber.Main";
// Public Properties:
// Private Properties:
	private var stream:Stream;
	private var cam:Camera;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var connector:Connector;
	private var publish_video:Video;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function Main() {XrayLoader.loadConnector("xray.swf");}
	private function onLoad():Void { configUI(); }

// Public Methods:
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		// setup the tooltip defaults
		Tooltip.options = {size:10, font:"_sans", corner:0};
		
		// setup cam
		cam = Camera.get();
		
		// get notified of connection changes
		connector.addEventListener("connectionChange", this);
		
		// set the uri
		Connector.red5URI = "rtmp://localhost/oflaDemo";
		
		// initialize the connector
		connector.configUI();	
	}
	
	private function connectionChange(evtObj:Object):Void
	{		
		
		if(evtObj.connected) 
		{
			// setup stream
			// XXX: odd hack needed for flashIDE.
			var conn = evtObj.connection; 
			var x = new Stream(conn);
			stream = x;
			stream.play("red5StreamDemo", -1);
			publish_video.attachVideo(stream);
		}
	}

}