import com.gskinner.events.GDispatcher;
//import org.red5.utils.Delegate;

class org.red5.samples.livestream.videoconference.Connection extends NetConnection
{
// Constants:
	public static var CLASS_REF = org.red5.samples.livestream.videoconference.Connection;
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
	// FITC Demo method
	private function setId(id:String) 
	{
	   _global.tt("SetId", id);
	   dispatchEvent({type:"onSetID", id:id})
	   return "Okay";
	}
	
	private function newStream(p_newStream:Object):Void
	{
		_global.tt("Connector.newStream", p_newStream, typeof p_newStream);
		dispatchEvent({type:"newStream", newStream:p_newStream});
	}
	
	private function onStatus(evtObj:Object):Void
	{
		switch(evtObj.code)
		{
			case "NetConnection.Connect.Success":
				connected = true;
				dispatchEvent({type:"success", code:evtObj.code, connected:true});
				break;
			
			case "NetConnection.Connect.Closed":
				connected = false;
				dispatchEvent({type:"close", code:evtObj.code, connected:false});
				break;
			
			case"NetConnection.Connect.Failed":
				connected = false;
				dispatchEvent({type:"failed", code:evtObj.code, connected:false});
			break;
			
			case"NetConnection.Connect.AppShutdown":
				connected = false;
				dispatchEvent({type:"appShutDown", code:evtObj.code});
			break;
			
			case"NetConnection.Call.Failed":
				dispatchEvent({type:"callFailed", code:evtObj.code});
			break;
			
			case"NetConnection.Connect.InvalidApp":
				dispatchEvent({type:"invalidApp", code:evtObj.code});
			break;
			
			case"NetConnection.Connect.Rejected":
				dispatchEvent({type:"rejected", code:evtObj.code});
			break;
		}
		
		dispatchEvent({type:evtObj.level.toLowerCase(), code:evtObj.code, description:evtObj.description, details:evtObj.details});
	}
}
