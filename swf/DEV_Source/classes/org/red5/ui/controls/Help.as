// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **
import com.neoarchaic.ui.Tooltip;
import org.red5.utils.Delegate;
class org.red5.ui.controls.Help extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.ui.controls.Help;
	public static var LINKAGE_ID:String = "org.red5.ui.controls.Help";
// Public Properties:
// Private Properties:
// UI Elements:

// ** AUTO-UI ELEMENTS **
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function Help() {}
	private function onLoad():Void { configUI(); }

// Public Methods:
	public function showTip(p_tip:String):Void
	{
		Tooltip.show(p_tip);
	}
	
	public function hideTip():Void
	{
		Tooltip.hide();
	}
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		onRollOut = Delegate.create(this, hideTip);
	}
}