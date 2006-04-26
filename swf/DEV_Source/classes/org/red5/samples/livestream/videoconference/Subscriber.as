// ** AUTO-UI IMPORT STATEMENTS **
import org.red5.samples.livestream.videoconference.Connector;
// ** END AUTO-UI IMPORT STATEMENTS **
import org.red5.net.Stream;
import org.red5.samples.livestream.videoconference.Connection;
import org.red5.utils.Delegate;


class org.red5.samples.livestream.videoconference.Subscriber extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.livestream.videoconference.Subscriber;
	public static var LINKAGE_ID:String = "org.red5.samples.livestream.videoconference.Subscriber";
// Public Properties:
	public var connected:Boolean = false;
	public var videoStream:String;
// Private Properties:
	private var stream:Stream;
	private var nc:Connection;
	private var checkTime:Number;
	private var si:Number;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var connector:Connector;
	private var userName:TextField;
	private var noStream:MovieClip;
	private var publish_video:Video;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function Subscriber() {}
	private function onLoad():Void { configUI(); }

// Public Methods:
	public function setUserName(p_name:String):Void
	{
		userName.text = p_name;
	}
	
	public function streamStop(evtObj:Object):Void
	{
		reset();
	}
	
	public function subscribe(p_subscriptionID:String, p_connection:Connection):Void
	{
		_global.tt("Subscriber.subscribe", p_subscriptionID);
		
		videoStream = p_subscriptionID;
		
		stream = new Stream(p_connection);
		
		// set to false to NOT see status events
		stream.debug = true;
		
		//stream.setBuffer(1);
		// add listener for when someone disconnects
		stream.addEventListener("unpublishNotify", Delegate.create(this, streamStop));
		
		// play the stream
		stream.play(p_subscriptionID, -1);
		
		checkTime = stream.time;
		si = setInterval(this, "doubleCheckTime", 1500);
		
		// connect to the video object
		publish_video.attachVideo(stream);
		
		// set connected
		connected = true;
		
		// show the silloutte just incase there's no stream
		noStream._visible = true;
	}
// Semi-Private Methods:
// Private Methods:

	private function doubleCheckTime():Void
	{
		clearInterval(si);
		if(checkTime == stream.time) reset();
	}
	private function configUI():Void 
	{
		userName.text = "";
		noStream._visible = false;
	};
	
	private function reset():Void
	{
		connected = false;
		userName.text = "";
		noStream._visible = false;
		publish_video.clear();
		stream.close();
	}
}