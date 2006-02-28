import com.gskinner.events.GDispatcher;
//import org.red5.utils.Delegate;

class org.red5.net.Connection extends NetConnection
{
// Constants:
	public static var CLASS_REF = org.red5.net.Connection;
// Public Properties:
	public var addEventListener:Function;
	public var removeEventListener:Function;
	public var connected:Boolean;
	//public var nc:NetConnection;
// Private Properties:
	private var dispatchEvent:Function;	

// Initialization:
	public function Connection() 
	{
		super();
		GDispatcher.initialize(this);
	}

// Public Methods:
	public function connect():Boolean
	{
		// if the URI is valid, it will return true.  This does NOT mean it's connected however.
		var goodURI:Boolean = super.connect.apply(super, arguments);
		
		return goodURI;
	}
	
	public function close():Void
	{
		// closes the connection to red5
		dispatchEvent({type:"connectionChange", connected:connected});
		super.close();
	}
// Semi-Private Methods:
// Private Methods:
	private function onStatus(evtObj:Object):Void
	{
		if(evtObj.code == "NetConnection.Connect.Success")
		{
			connected = true;
			dispatchEvent({type:"connectionChange", connected:true});
		}
		if(evtObj.code == "NetConnection.Connect.Closed")
		{
			connected = false;
			dispatchEvent({type:"connectionChange", connected:false});
		}
	}

}