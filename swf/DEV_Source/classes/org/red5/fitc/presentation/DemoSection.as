// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **

import com.blitzagency.data.ContentFarm;

class org.red5.fitc.presentation.DemoSection extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.fitc.presentation.DemoSection;
	public static var LINKAGE_ID:String = "org.red5.fitc.presentation.DemoSection";
// Public Properties:
// Private Properties:
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var title:TextField;
	private var content:MovieClip;
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
		//content.loadMovie("FITCvideoConference.swf");
		hide();
	}
	
	private function hide():Void
	{
		//content.htmlText = "";
		_visible = false;
		gotoAndStop(1);
	}
}