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
			/*==========[ BUFFER NOTIFICATIONS ]=================*/
			case "NetStream.Buffer.Full":
				dispatchEvent({type:"bufferFull", code:evtObj.code});
			break;
			
			case "NetStream.Buffer.Empty":
				dispatchEvent({type:"bufferEmpty", code:evtObj.code});
			break;
			
			/*==========[ PLAYBACK NOTIFICATIONS ]=================*/
			case "NetStream.Play.Stop":
				dispatchEvent({type:"playStop", code:evtObj.code});
			break;
			
			case "NetStream.Play.Failed":
				dispatchEvent({type:"playFailed", code:evtObj.code, description:evtObj.description});
			break;
			
			case "NetStream.Play.Reset":
				dispatchEvent({type:"playReset", code:evtObj.code});
			break;
			
			case "NetStream.Play.Start":
				dispatchEvent({type:"playStart", code:evtObj.code, details:evtObj.details});
			break;
			
			case "NetStream.Play.Stop":
				dispatchEvent({type:"playStop", code:evtObj.code});
			break;
			
			case "NetStream.Play.StreamNotFound":
				dispatchEvent({type:"playStreamNotFound", code:evtObj.code});
			break;
			
			case "NetStream.Pause.Notify":
				dispatchEvent({type:"pauseNotify", code:evtObj.code});
			break;
			
			case "NetStream.Unpause.Notify":
				dispatchEvent({type:"unpauseNotify", code:evtObj.code});
			break;
			
			case "NetStream.Seek.Failed":
				dispatchEvent({type:"seekFailed", code:evtObj.code});
			break;
			
			case "NetStream.Seek.Notify":
				dispatchEvent({type:"seekNotify", code:evtObj.code});
			break;
			
			/*==========[ PUBLISH NOTIFICATIONS ]=================*/
			case "NetStream.Publish.Start":
				dispatchEvent({type:"publishStart", code:evtObj.code});
			break;
			
			case "NetStream.Publish.BadName":
				dispatchEvent({type:"publishBadName", code:evtObj.code});
			break;
			
			case "NetStream.Publish.Idle":
				dispatchEvent({type:"publishIdle", code:evtObj.code});
			break;
			
			case "NetStream.Play.PublishNotify":
				dispatchEvent({type:"playPublishNotify", code:evtObj.code});
			break;
			
			case "NetStream.Play.UnpublishNotify":
				dispatchEvent({type:"unpublishNotify", code:evtObj.code});
			break;
			
			case "NetStream.Unpublish.Success":
				dispatchEvent({type:"unpublishSuccess", code:evtObj.code});
			break;
			
			/*==========[ RECORD NOTIFICATIONS ]=================*/
			case "NetStream.Record.Failed":
				dispatchEvent({type:"recordFailed", code:evtObj.code, description:evtObj.description});
			break;
			
			case "NetStream.Record.Start":
				dispatchEvent({type:"recordStart", code:evtObj.code});
			break;
			
			case "NetStream.Record.NoAccess":
				dispatchEvent({type:"recordNoAcess", code:evtObj.code});
			break;
			
			case "NetStream.Record.Stop":
				dispatchEvent({type:"recordStop", code:evtObj.code});
			break;

			/*==========[ UNSPECIFIED ERROR NOTIFICATIONS ]=================*/
			case "NetStream.Failed":
				dispatchEvent({type:"failed", code:evtObj.code, description:evtObj.description});
			break;
		}
		
		dispatchEvent({type:evtObj.level.toLowerCase(), code:evtObj.code, description:evtObj.description, details:evtObj.details});
	}
}