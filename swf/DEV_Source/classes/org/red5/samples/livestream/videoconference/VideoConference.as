// ** AUTO-UI IMPORT STATEMENTS **
import mx.controls.TextArea;
import mx.controls.Button;
import org.red5.samples.livestream.videoconference.VideoPool;
import org.red5.samples.livestream.videoconference.Chat;
import org.red5.samples.livestream.videoconference.Broadcaster;
// ** END AUTO-UI IMPORT STATEMENTS **

import org.red5.samples.livestream.videoconference.GlobalObject;
import com.blitzagency.xray.util.XrayLoader;

import org.red5.utils.Delegate;
import org.red5.samples.livestream.videoconference.Connection;

class org.red5.samples.livestream.videoconference.VideoConference extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.livestream.videoconference.VideoConference;
	public static var LINKAGE_ID:String = "org.red5.samples.livestream.videoconference.VideoConference";
// Public Properties:
	public var SO:GlobalObject;
	public var videoID:Number;
// Private Properties:
	private var connection:Connection;
	private var result:Object;
	private var streamQue:Array;
	private var si:Number;
	
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var broadcaster:Broadcaster;
	private var output:TextArea;
	private var videoPool:VideoPool;
	private var clearTrace:Button;
	private var chat:Chat;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function VideoConference() 
	{
		XrayLoader.addEventListener("LoadComplete", this, "xrayLoadComplete"); 
		XrayLoader.loadConnector("xrayconnector.swf");
	}
	private function onLoad():Void { configUI(); }

// Public Methods:
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		clearTrace.onRelease = Delegate.create(this, clearOutput);
		//SO = new GlobalObject();
		result = {};
		result.onResult = Delegate.create(this, onResult);

		broadcaster.registerController(this);
		broadcaster.addEventListener("connected", this);
		broadcaster.addEventListener("disconnected", this);
		broadcaster.addEventListener("onSetID", this);
	}
	
	private function clearOutput():Void
	{
		output.text = "";
	}
	
	private function xrayLoadComplete():Void
	{
		var xr = _global.com.blitzagency.xray.XrayTrace.getInstance();
		xr.addEventListener("onTrace", this);
	}
	
	private function onTrace(traceInfo):Void
	{
		output.text += traceInfo.sInfo+"\n";	
	//	info.maxScroll
	}
	
	private function onResult(evtObj:Array):Void
	{
		_global.tt("Streams List recieved", evtObj);
		if(evtObj.length > 0) streamQue = evtObj;
		si = setInterval(this, "processQue", 250);
	}
	
	private function processQue():Void
	{
		if(streamQue.length <= 0) 
		{
			clearInterval(si);
			return;
		}
		
		var id:Number = Number(streamQue.shift().split("_")[1]);
		videoPool.subscribe(id);
	}
	
	private function setID(p_id:Number, p_connection:Connection):Void
	{
		//set local videoID
		videoID = Number(p_id);
		
		// set connection
		connection = p_connection;
		
		chat.registerConnection(p_connection);
		chat.connectSO("videoConferenceChat");
		
		// connect to so
		//SO.connect("fitcDemoSO", p_connection, false);
		//SO.addEventListener("onSync", this);
		
		// let everyone else know your ID so they can subscribe
		//SO.setData("videoID", videoID);
		
		// get list of current streams and subscribe
		getStreams();
	}
	
	private function getStreams():Void
	{
		connection.call("getStreams", this.result);
	}
	
	private function newStream(evtObj:Object):Void
	{
		_global.tt("NewStream Recieved", evtObj.newStream);
		videoPool.subscribe(evtObj.newStream.split("_")[1]);
	}
	
	private function onSync():Void
	{
		// fetch latest
		/*
		var newSubscription:Number = SO.getData("videoID");
		
		if(newSubscription != videoID) 
		{
			_global.tt("received new video stream", newSubscription);
			videoPool.subscribe(newSubscription);
		}
		*/
	}
	
	private function connected(evtObj:Object):Void
	{
		// connect to the SO after making connection
		
		videoPool.setConnection(evtObj.connection);
	}
	
	private function disconnected(evtObj:Object):Void
	{
		// reset the video pool	
		videoPool.resetAll();
	}
	
	private function updateSO():Void
	{
		
	}
}