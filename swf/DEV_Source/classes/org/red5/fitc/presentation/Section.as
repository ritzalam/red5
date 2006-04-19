// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **

import com.blitzagency.data.ContentFarm;

class org.red5.fitc.presentation.Section extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.fitc.presentation.Section;
	public static var LINKAGE_ID:String = "org.red5.fitc.presentation.Section";
// Public Properties:
// Private Properties:
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var title:TextField;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function Section() {}
	private function onLoad():Void { configUI(); }

// Public Methods:
	function transitionIn():Void
	{
		//content.htmlText = ContentFarm.getContent("section_0");
		_visible = true;
		gotoAndPlay("transitionIn");
	}
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		hide();
	}
	
	private function hide():Void
	{
		//content.htmlText = "";
		_visible = false;
		gotoAndStop(1);
	}
}