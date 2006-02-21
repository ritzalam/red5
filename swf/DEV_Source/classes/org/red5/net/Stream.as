// ** AUTO-UI IMPORT STATEMENTS **
import org.red5.utils.Connector;
// ** END AUTO-UI IMPORT STATEMENTS **
//import org.red5.utils.Delegate;
import com.gskinner.events.GDispatcher;
import org.red5.net.Connection;

class org.red5.net.Stream extends NetStream {
// Constants:
	public static var CLASS_REF = org.red5.net.Stream;
	public static var LINKAGE_ID:String = "org.red5.net.Stream";
// Public Properties:
	public var addEventListener:Function;
	public var removeEventListener:Function;
// Private Properties:
	private var dispatchEvent:Function;
	private var connection:Connection;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var connector:Connector;
	private var publish_video:Video;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	function Stream(p_connection:Connection) 
	{
		super(p_connection);
		GDispatcher.initialize(this);
		connection = p_connection;		
	}

// Public Methods:
// Semi-Private Methods:
// Private Methods:
	private function onStatus(evtObj:Object):Void 
	{
		switch(evtObj.code)
		{
			case "NetStream.Buffer.Full":
				dispatchEvent({type:"playStream"});
			break;
			
			case "NetStream.Buffer.Empty":
				dispatchEvent({type:"bufferEmpty"});
			break;
			
			case "NetStream.Play.Stop":
				dispatchEvent({type:"stopStream"});
			break;
			
			case "NetStream.Publish.Start":
				dispatchEvent({type:"publishStart"});
			break;
			
			case "NetStream.Record.Failed":
				dispatchEvent({type:"recordFailed"});
			break;
			
			case "NetStream.Record.Start":
				dispatchEvent({type:"recordStart"});
			break;
			
			case "NetStream.Record.Stop":
				dispatchEvent({type:"recordStop"});
			break;
		}
	}
}