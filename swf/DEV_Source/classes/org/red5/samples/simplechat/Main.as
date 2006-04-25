// ** AUTO-UI IMPORT STATEMENTS **
import org.red5.samples.simplechat.BasicChat;
import org.red5.utils.Connector;
// ** END AUTO-UI IMPORT STATEMENTS **
import com.neoarchaic.ui.Tooltip;
//import org.red5.utils.Delegate;
import com.blitzagency.xray.util.XrayLoader;

class org.red5.samples.simplechat.Main extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.simplechat.Main;
	public static var LINKAGE_ID:String = "org.red5.samples.simplechat.Main";
// Public Properties:
// Private Properties:
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var chat:BasicChat;
	private var connector:Connector;
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
		
		// get notified of connection changes
		connector.addEventListener("connectionChange", this);
		
		// set the uri
		Connector.red5URI = "rtmp://localhost/SOSample";
		
		// initialize the connector
		connector.configUI();
	}
	
	private function connectionChange(evtObj:Object):Void
	{		
		
		if(evtObj.connected) 
		{
			// register the NetConnection that GlobalObject will need
			chat.registerConnection(evtObj.connection);
			
			// connect the shared object
			chat.connectSO();
		}
	}
}