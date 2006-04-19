import org.red5.utils.Delegate;
import com.gskinner.events.GDispatcher;
import org.red5.samples.livestream.videoconference.Connection;

class org.red5.samples.livestream.videoconference.GlobalObject
{
// Constants:
	public static var CLASS_REF = org.red5.samples.livestream.videoconference.GlobalObject;
// Public Properties:
	public var addEventListener:Function;
	public var removeEventListener:Function;
	public var soName:String;	
	public var connected:Boolean;
	public var data:Object;
	public var so:SharedObject;
// Private Properties:
	private var dispatchEvent:Function;
	private var nc:Connection;

// Initialization:
	public function GlobalObject()
	{
		super();
		GDispatcher.initialize(this);
	}

// Public Methods:
	
	public function connect(p_soName:String, p_nc:Connection, p_persistant:Boolean):Boolean
	{
		// store Connection reference
		nc = p_nc;
		
		// create StoredObject
		so = SharedObject.getRemote(p_soName, nc.uri, p_persistant); 
		
		so["newMessage"] = Delegate.create(this, newMessageHandler);
		so["newName"] = Delegate.create(this, newNameHandler);
		so["getName"] = Delegate.create(this, getNameHandler);
		
		// setup the onSync events
		so.onSync = Delegate.create(this, onStatus);
		
		// connect to the SO
		connected = so.connect(nc);
		
		return connected;
	}
	
	public function sendMessage(p_message:String):Void
	{
		so.send("newMessage", p_message);
	}
	
	public function sendName(p_name:String, p_videoID:Number):Void
	{
		so.send("newName", p_name, p_videoID);
	}
	
	public function sendGetNames():Void
	{
		so.send("getName");
	}
	
	private function newMessageHandler(p_message):Void
	{
		dispatchEvent({type:"onNewMessage", message:p_message});
	}
	
	private function newNameHandler(p_name, p_videoID):Void
	{
		dispatchEvent({type:"onNewName", name:p_name, videoID: p_videoID});
	}
	
	private function getNameHandler():Void
	{
		dispatchEvent({type:"onGetName"});
	}
	
	// status for the sharedobject
	private function onStatus(evtObj):Void
	{
		// an update has been recieved, send out to the concerned parties
		dispatchEvent({type:"onSync"});
	}
// Semi-Private Methods:
// Private Methods:
	public function getData(p_key:String)
	{
		// if no value, return null
		if (so.data[p_key] == undefined) return null;
		return so.data[p_key];
	}

	public function setData(p_key:String, p_value):Void 
	{
		// updating the key causes the SO to update on the server.
		so.data[p_key] = p_value;
	}
	
	public function clear():Void
	{
		// clear not supported on Red5 yet
		so.clear();
	}
}