// ** AUTO-UI IMPORT STATEMENTS **
import org.red5.controls.GraphicButton;
// ** END AUTO-UI IMPORT STATEMENTS **
import com.blitzagency.xray.logger.XrayLog;
import org.red5.utils.Delegate;
import org.red5.net.Connection;
import com.acmewebworks.controls.BaseClip;
import mx.controls.Alert;

class org.red5.samples.messagerecorder.Recorder extends BaseClip 
{
// Constants:
	public static var CLASS_REF = org.red5.samples.messagerecorder.Recorder;
	public static var LINKAGE_ID:String = "org.red5.samples.messagerecorder.Recorder";
// Public Properties:
	public var recording:Boolean = false;
// Private Properties:
	private var log:XrayLog;
	private var charList:Array;
	private var connection:Connection;
	private var ns:NetStream;
	private var cam:Camera;
	private var mic:Microphone;
	private var timeoutSI:Number;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var nameBG:MovieClip;
	private var publish_video:Video;
	private var record:GraphicButton;
	private var stopRecord:GraphicButton;
	private var txtName:TextField;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function Recorder() {}
	private function onLoad():Void { configUI(); }

// Public Methods:

	public function registerConnection(p_connection:Connection):Void
	{
		trace(log.debug("registerConnection", p_connection));
		connection = p_connection;
		ns = new NetStream(connection);
	}
	
	public function disableControls():Void
	{
		// we're in record mode
		txtName.type = "dynamic";
		record.enabled = false;
		stopRecord.enabled = true;
	}
	
	public function enableControls():Void
	{
		// there isn't an enabled property on textfields, so we just switch them between dynamic and input.
		
		record.enabled = true;
		stopRecord.enabled = false;
		txtName.type = "input";
		txtName.text = "1.  enter your name(s)";
		Selection.setFocus(null);
		trace(log.debug("enableControls", txtName.text));
	}
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		log = new XrayLog();
		
		charList = [" ", ".", "&", "@", ":", "\"", "'", ";", "/", "\\", ">", "<"];
	
		txtName.onSetFocus = Delegate.create(this, onSetFocus);
		record.addEventListener("click", Delegate.create(this, recordClickHandler));
		stopRecord.addEventListener("click", Delegate.create(this, stopRecordClickHandler));
		
			// setup cam
		cam = Camera.get();
		cam.setMode(320, 270, 30);
		cam.setQuality(0,80);
		
		// setup mic
		mic = Microphone.get();
		mic.setRate(44);
		
		enableControls();
	}
	
	private function onSetFocus(evtObj:Object):Void
	{
		if(txtName.text.toLowerCase() == "1.  enter your name(s)") txtName.text = "";
	}
	
	private function recordClickHandler(evtObj:Object):Void
	{
		if(txtName.text.toLowerCase() == "1.  enter your name(s)" || txtName.text.length <= 0)
		{
			Alert.show("Please enter a valid name for your video", "Error in Name(s)",Alert.OK, null, enableControls);
		}else
		{
			// clean name
			var flvName:String = cleanString(txtName.text) + "_" + getTimer();
			trace(log.debug("flvName", flvName));
			
			// start recording
			startRecording(flvName);
		}
	}
	
	private function stopRecordClickHandler(evtObj:Object):Void
	{
		stopRecording();
	}
	
	private function startRecording(flvName:String):Void
	{
		if(recording) return;
		// lock down controls
		disableControls();
		recording = true;
		
		// connect for recording
		ns.publish(flvName, "record");
		ns.attachVideo(cam);
		ns.attachAudio(mic);
		
		// attach to video object on stage
		publish_video.attachVideo(cam);
		
		// turn off in 3 minutes if not already done so
		timeoutSI = setInterval(this, "stopRecording", 180000);
	}
	
	private function stopRecording():Void
	{
		if(!recording) return;
		trace(log.debug("stopRecording"));
		
		recording = false;
		
		// clear video on stage
		publish_video.attachVideo(null);
		publish_video.clear();
		
		// close netStream
		ns.close();
		
		dispatchEvent({type:"stopRecording"});
	}
	
	private function cleanString(str:String):String
	{
		for(var i:Number=0;i<charList.length;i++)
		{
			str = str.split(charList[i]).join("");
		}
		return str.toLowerCase();
	}
}