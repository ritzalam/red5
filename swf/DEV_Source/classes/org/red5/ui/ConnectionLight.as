// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **
import org.red5.utils.Delegate;
import org.red5.net.Connection;

class org.red5.ui.ConnectionLight extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.ui.ConnectionLight;
	public static var LINKAGE_ID:String = "org.red5.ui.ConnectionLight";
// Public Properties:
// Private Properties:
	private var connection:Connection;
// UI Elements:

// ** AUTO-UI ELEMENTS **
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function ConnectionLight() {}
	private function onLoad():Void { configUI(); }

// Public Methods:
	public function registerNC(p_connection:Connection):Void
	{
		// when we receive the connection reference, we can add the listener
		connection = p_connection;
		connection.addEventListener("success", Delegate.create(this, updateConnection));
		connection.addEventListener("close", Delegate.create(this, updateConnection));
	}
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		// it's a 2 frame moveiclip.  frame 1 is disconnected, frame 2 is connected
		gotoAndStop(1);
	}
	
	private function updateConnection(evtObj:Object):Void
	{
		// when update is received, we change frames
		var frame:Number = evtObj.connected ? 2 : 1;
		gotoAndStop(frame);
	}

}