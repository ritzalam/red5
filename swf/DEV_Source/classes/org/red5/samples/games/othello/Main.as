// ** AUTO-UI IMPORT STATEMENTS **
import com.blitzagency.util.SimpleDialog;
import org.red5.samples.games.othello.SignupConnector;
import org.red5.samples.games.othello.MultiPlayerManager;
import org.red5.samples.games.othello.GameBoard;
// ** END AUTO-UI IMPORT STATEMENTS **
import org.red5.utils.GridManager;
import org.red5.samples.games.othello.GameManager;
import com.blitzagency.xray.util.XrayLoader;
import com.neoarchaic.ui.Tooltip;

class org.red5.samples.games.othello.Main extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.games.othello.Main;
	public static var LINKAGE_ID:String = "org.red5.samples.games.othello.Main";
// Public Properties:
// Private Properties:
	private var gameManager:GameManager;
	private var res:Object;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var alert:SimpleDialog;
	private var board:GameBoard;
	private var connector:SignupConnector;
	private var multiPlayerManager:MultiPlayerManager;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function Main() {XrayLoader.loadConnector("xray.swf");}
	private function onLoad():Void { configUI(); }

// Public Methods:
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
			
		// setup the tooltip defaults
		Tooltip.options = {size:10, font:"_sans", corner:0};
		
		// get notified of connection changes
		connector.addEventListener("connectionChange", this);
		
		// set the uri
		SignupConnector.red5URI = "rtmp://192.168.1.2/SOSample";
		
		// initialize the connector
		connector.configUI();
				
		// init grid
		GridManager.addEventListener("gridLocation", this);
		board.createBoard();
		
		// init the GameManager
		gameManager = new GameManager();
		gameManager.registerGameBoard(board);
		
		// register alerts
		gameManager.registerAlert(alert);
		multiPlayerManager.registerAlert(alert);
		
		board.registerGameManager(gameManager);
	}
	
	private function connectionChange(evtObj:Object):Void
	{		
		_global.tt("connectionChange", evtObj);
		if(evtObj.connected) 
		{
			_global.tt(0);
			//gameManager.resetGame();
			multiPlayerManager.configUI(connector.connection, evtObj.userName);
		}
	}

}