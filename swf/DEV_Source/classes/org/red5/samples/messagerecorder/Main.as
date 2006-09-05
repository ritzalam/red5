// ** AUTO-UI IMPORT STATEMENTS **
import org.red5.utils.Connector;
import org.red5.samples.messagerecorder.ThankYou;
import org.red5.samples.messagerecorder.Recorder;
// ** END AUTO-UI IMPORT STATEMENTS **
import com.neoarchaic.ui.Tooltip;
import org.red5.utils.Delegate;
import com.blitzagency.xray.util.XrayLoader;
import com.blitzagency.xray.logger.XrayLog;
import com.acmewebworks.controls.BaseClip;
import com.mosesSupposes.fuse.*;

class org.red5.samples.messagerecorder.Main extends BaseClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.messagerecorder.Main;
	public static var LINKAGE_ID:String = "org.red5.samples.messagerecorder.Main";
// Public Properties:
// Private Properties:
	var log:XrayLog;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var connector:Connector;
	private var recorder:Recorder;
	private var thankYou:ThankYou;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function Main() {}
	private function onLoad():Void 
	{ 
		fscommand("fullscreen", true);
		fscommand("allowscale", false);
		
		mx.styles.StyleManager.registerColorName("special_orange", 0xFF9900);

		_global.style.setStyle ("themeColor", "special_orange");
		_global.style.setStyle("fontFamily", "_sans");
		_global.style.setStyle("fontSize", 10);
		
		ZigoEngine.register(Shortcuts, FuseItem, PennerEasing, Fuse, FuseFMP);
		
		// hide the connector
		connector._visible = false;
		
		// hide thank you
		thankYou.addEventListener("onHide", Delegate.create(recorder, recorder.enableControls));
		
		XrayLoader.addEventListener(XrayLoader.LOADCOMPLETE, this, "configUI");
		XrayLoader.loadConnector("xrayConnector_1.6.1.swf", null, false);
		log = new XrayLog();
	}

// Public Methods:
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		_level0.__xrayConnector._visible = false;
		
		// to show / hide the connector
		Key.addListener(this);
		
		// register for stopRecording messages
		recorder.addEventListener("stopRecording", Delegate.create(thankYou, thankYou.show));

		// setup the tooltip defaults
		Tooltip.options = {size:10, font:"_sans", corner:0};
		
		// get notified of connection changes
		connector.addEventListener("connectionChange", this);
		
		// set the uri
		Connector.red5URI = "rtmp://localhost/messageRecorder";
		
		// initialize the connector
		connector.configUI();
		
		// connect automatically
		connector.makeConnection();
	}
	
	private function connectionChange(evtObj:Object):Void
	{		
		if(evtObj.connected) 
		{				
			recorder.registerConnection(evtObj.connection);
		}
	}
	
	private function onKeyDown():Void
	{
		
		var key:Number = Key.getAscii();
		// SHIFT + ~
		if(Key.isDown(Key.SHIFT) && key == 126)
		{
			//trace(log.debug("key!!", Key.getAscii()));
			connector._visible = !connector._visible;
		}
	}
}