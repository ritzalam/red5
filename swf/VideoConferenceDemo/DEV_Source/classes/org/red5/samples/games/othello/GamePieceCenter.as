// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **

class org.red5.samples.games.othello.GamePieceCenter extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.games.othello.GamePieceCenter;
	public static var LINKAGE_ID:String = "org.red5.samples.games.othello.GamePieceCenter";
// Public Properties:
// Private Properties:
// UI Elements:

// ** AUTO-UI ELEMENTS **
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function GamePieceCenter() {}
	private function onLoad():Void { configUI(); }

// Public Methods:
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		gotoAndStop("white");
	}

}