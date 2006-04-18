// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **
import org.red5.net.Stream;
import org.red5.net.Connection;
import org.red5.utils.Delegate;


class org.red5.samples.livestream.videoconference.Subscriber extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.livestream.videoconference.Subscriber;
	public static var LINKAGE_ID:String = "org.red5.samples.livestream.videoconference.Subscriber";
// Public Properties:
	public var connected:Boolean = false;
// Private Properties:
	private var stream:Stream;
	private var nc:Connection;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var publish_video:Video;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function Subscriber() {}
	private function onLoad():Void { configUI(); }

// Public Methods:
	public function subscribe(p_subscriptionID:String, p_connection:Connection):Void
	{
		_global.tt("Subscriber.subscribe", p_subscriptionID);
		stream = new Stream(p_connection);
		//stream.setBuffer(1);
		stream.addEventListener("unpublishNotify", Delegate.create(this, streamStop));
		stream.play(p_subscriptionID, -1);
		publish_video.attachVideo(stream);
		connected = true;
	}
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void {};
	
	private function streamStop(evtObj:Object):Void
	{
		reset();
	}
	
	private function reset():Void
	{
		connected = false;
		publish_video.clear();
		stream.close();
	}
}