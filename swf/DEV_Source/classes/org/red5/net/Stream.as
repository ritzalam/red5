// ** AUTO-UI IMPORT STATEMENTS **
import org.red5.utils.Connector;
// ** END AUTO-UI IMPORT STATEMENTS **
//import org.red5.utils.Delegate;
//import com.gskinner.events.GDispatcher;
import org.red5.net.Connection;

class org.red5.net.Stream extends NetStream {
// Constants:
	public static var CLASS_REF = org.red5.net.Stream;
	public static var LINKAGE_ID:String = "org.red5.net.Stream";
// Public Properties:
// Private Properties:
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
		connection = p_connection;		
	}

// Public Methods:
// Semi-Private Methods:
// Private Methods:
	private function onStatus(evtObj:Object):Void 
	{
		_global.tt("Stream.onStatus", evtObj);
	}
}