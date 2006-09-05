// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **
import com.acmewebworks.controls.BaseClip;
import com.mosesSupposes.fuse.Fuse;

class org.red5.samples.messagerecorder.ThankYou extends BaseClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.messagerecorder.ThankYou;
	public static var LINKAGE_ID:String = "org.red5.samples.messagerecorder.ThankYou";
// Public Properties:
// Private Properties:
	private var si:Number;
// UI Elements:

// ** AUTO-UI ELEMENTS **
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function ThankYou() {}
	private function onLoad():Void { configUI(); }

// Public Methods:
	public function show():Void
	{
		_visible = true;
		var f:Fuse = new Fuse();
		f.push({target:this, start_alpha:0, alpha:100, seconds:2});
		f.start();
		clearInterval(si);
		si = setInterval(this, "hide", 4000);
	}
	
	public function hide():Void
	{
		clearInterval(si);		
		var f:Fuse = new Fuse();
		f.push({target:this, start_alpha:100, alpha:0, seconds:2});
		f.start();
		dispatchEvent({type:"onHide"});
	}
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void {_visible=false};
}