// ** AUTO-UI IMPORT STATEMENTS **
// ** END AUTO-UI IMPORT STATEMENTS **
import org.red5.utils.Delegate;
import com.gskinner.events.GDispatcher;
//import org.red5.utils.GridManager;
import org.red5.samples.games.othello.GameBoard;
import org.red5.samples.games.othello.GameManager;

class org.red5.samples.games.othello.GamePiece extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.games.othello.GamePiece;
	public static var LINKAGE_ID:String = "org.red5.samples.games.othello.GamePiece";
// Public Properties:
	public var addEventListener:Function
	public var removeEventListener:Function;
	public var state:String
	public var siblings:Object;
	public var __index:Number;
	public var type:Number; // 0=corner, 1=side, 2=middle
// Private Properties:
	private var dispatchEvent:Function;
	private var gameBoard:GameBoard;
	private var gameManager:GameManager;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var center:MovieClip;
// ** END AUTO-UI ELEMENTS **

	public function get index():Number
	{
		return __index;
	}
	
	public function set index(newValue:Number):Void
	{
		__index = newValue;
		resolveSiblings();
	}

// Initialization:
	private function GamePiece() {GDispatcher.initialize(this); hide();}
	private function onLoad():Void { configUI(); }

// Public Methods:
	public function show():Void
	{
		_visible = true;
	}
	
	public function hide():Void
	{
		_visible = false;
	}
	
	public function changeState(p_newState:String):Void
	{
		if(p_newState != undefined) 
		{
			state = p_newState;
		}else
		{
			state = state == "white" ? "black" : "white"
		}
		
		center.gotoAndStop(state);
		show();
	}
	
	public function registerGameBoard(p_gameBoard:GameBoard):Void
	{
		gameBoard = p_gameBoard;
		addEventListener("badPiecePlayed", Delegate.create(gameBoard, gameBoard.removeGamePiece));
	}
	
	public function registerGameManager(p_gameManager:GameManager):Void
	{
		gameManager = p_gameManager;
		addEventListener("goodPiecePlayed", Delegate.create(gameManager, gameManager.processChangeList));
	}
	
	public function removeMovieClip():Void
	{
		super.removeMovieClip();
	}
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		onRelease = Delegate.create(this, clickHandler);
	}
	
	private function clickHandler(evtObj:Object):Void
	{
		dispatchEvent({type:"click"});
	}
	
	private function getSiblingCount():Number
	{
		var i:Number = 0;
		for(var items:String in siblings) i++;
		return i;
	}
	
	private function getSiblingType():Number
	{
		var count:Number = getSiblingCount();
		switch(count)
		{
			case 3:
				// corner
				return 0;
			break;
			
			case 4:
				// side
				return 1;
			break;
			
			default:
				// middle
				return 2;
		}
	}
	
	private function resolveSiblings():Void
	{
		siblings = gameBoard.resolveSiblings(index);
		type = getSiblingType();
		
		if(!gameManager.checkState(siblings, state)) 
		{
			dispatchEvent({type:"badPiecePlayed"});
		}else
		{
			dispatchEvent({type:"goodPiecePlayed"});
		}
	}	
}