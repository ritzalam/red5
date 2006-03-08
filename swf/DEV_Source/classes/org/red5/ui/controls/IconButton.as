// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **
import com.gskinner.events.GDispatcher;
import org.red5.utils.Delegate;
import com.neoarchaic.ui.Tooltip;
import org.red5.ui.controls.Help;

class org.red5.ui.controls.IconButton extends Help {
// Constants:
	public static var CLASS_REF = org.red5.ui.controls.IconButton;
	public static var LINKAGE_ID:String = "org.red5.ui.controls.IconButton";
// Public Properties:
	public var addEventListener:Function;
	public var removeEventListener:Function;
	public var tooltip:String = "Set Tool Tip";
// Private Properties:
	private var dispatchEvent:Function;
// UI Elements:

// ** AUTO-UI ELEMENTS **
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function IconButton() {GDispatcher.initialize(this);}
	private function onLoad():Void { configUI(); }

// Public Methods:
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		onRelease = Delegate.create(this, onClickHandler);
		onRollOver = Delegate.create(this, onRollOverHandler);
		onRollOut = Delegate.create(this, onRollOutHandler);
	}
	
	private function onClickHandler():Void
	{
		Tooltip.hide();
		dispatchEvent({type:"click"});
	}
	
	private function onRollOverHandler():Void
	{
		Tooltip.show(tooltip);
		dispatchEvent({type:"rollOver"});
	}
	
	private function onRollOutHandler():Void
	{
		Tooltip.hide();
		dispatchEvent({type:"rollOut"});
	}

}