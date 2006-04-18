// ** AUTO-UI IMPORT STATEMENTS **
import com.blitzagency.util.SimpleDialog;
import mx.controls.TextInput;
import org.red5.ui.ConnectionLight;
import org.red5.ui.controls.IconButton;
// ** END AUTO-UI IMPORT STATEMENTS **
import org.red5.samples.livestream.videoconference.Connection;
import org.red5.utils.Delegate;
import com.gskinner.events.GDispatcher;


class org.red5.samples.livestream.videoconference.Connector extends MovieClip 
{
// Constants:
	public static var CLASS_REF = org.red5.samples.livestream.videoconference.Connector;
	public static var LINKAGE_ID:String = "org.red5.samples.livestream.videoconference.Connector";
	public static var red5URI:String = "rtmp://localhost/";
// Public Properties:
	public var addEventListener:Function;
	public var removeEventListener:Function;
	public var connection:Connection;
// Private Properties:
	private var dispatchEvent:Function;
	
	
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var alert:SimpleDialog;
	private var connect:IconButton;
	private var disconnect:IconButton;
	private var light:ConnectionLight;
	private var uri:TextInput;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function Connector() {GDispatcher.initialize(this);}
	private function onLoad():Void {}

// Public Methods:
	public function configUI():Void 
	{
		// instantiate the con 	qnection
		connection  = new Connection();
		
		// register the connection with the light so it can add a listener
		light.registerNC(connection);
		
		// hide disconnect button
		disconnect._visible = false;
		
		// set the URI
		uri.text = red5URI;
		
		// setup the buttons
		connect.addEventListener("click", Delegate.create(this, makeConnection));
		disconnect.addEventListener("click", Delegate.create(this, closeConnection));
		connect.tooltip = "Connect to Red5";
		disconnect.tooltip = "Disconnect from Red5";
		
		// add listener for connection changes
		connection.addEventListener("success", Delegate.create(this, manageButtons));
		connection.addEventListener("onSetID", this);
		
		// FITC VIDEO CONFERENCE
		connection.addEventListener("newStream", this);
		connection.addEventListener("close", Delegate.create(this, manageButtons));
	}
	
	
	
	public function makeConnection(evtObj:Object):Void
	{
		if(uri.length > 0) 
		{
			var goodURI = connection.connect(uri.text, getTimer());
			if(!goodURI) alert.show("Please check connection URI String and try again.");
		}
	}
// Semi-Private Methods:
// Private Methods:

	// FITC VIDEO CONFERENCE
	private function newStream(evtObj:Object):Void
	{
		dispatchEvent({type:"newStream", newStream:evtObj.newStream});
	}
	
	// FITC VIDEO CONFERENCE
	private function onSetID(evtObj:Object):Void
	{
		dispatchEvent({type:"onSetID", id:evtObj.id})
	}	
	
	private function closeConnection(evtObj:Object):Void
	{
		if(connection.connected) connection.close();
	}
	
	private function manageButtons(evtObj:Object):Void
	{
		// based on the connection value, hide/show the respective buttons
		connect._visible = !evtObj.connected;
		disconnect._visible = evtObj.connected;
		
		// since Main doesn't really have access to Light, we're going to pass along the notification
		dispatchEvent({type:"connectionChange", connected: evtObj.connected});
	}
}