// ** AUTO-UI IMPORT STATEMENTS **
import mx.controls.TextInput;
import mx.controls.Button;
import mx.controls.List;
// ** END AUTO-UI IMPORT STATEMENTS **
import org.red5.utils.GlobalObject;
import org.red5.net.Connection;
import com.gskinner.events.GDispatcher;
import org.red5.utils.Delegate;
import com.blitzagency.util.SimpleDialog;

class org.red5.samples.games.othello.MultiPlayerManager extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.games.othello.MultiPlayerManager;
	public static var LINKAGE_ID:String = "org.red5.samples.games.othello.MultiPlayerManager";
// Public Properties:
	public var addEventDispatcher:Function
	public var removeEventDispatcher:Function;
	public var soConnected:Boolean;
// Private Properties:
	private var dispatchEvent:Function;
	private var so:GlobalObject;
	private var alert:SimpleDialog;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var addNewUser:Button;
	private var black:TextInput;
	private var newUserName:TextInput;
	private var players:List;
	private var startGame:Button;
	private var white:TextInput;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function MultiPlayerManager() {GDispatcher.initialize(this);}
	private function onLoad():Void {}

// Public Methods:
	
	
	public function registerAlert(p_alert:SimpleDialog):Void
	{
		alert = p_alert;
	}
	
	public function configUI(p_connection:Connection):Void 
	{
		// setup buttons
		addNewUser.addEventListener("click", Delegate.create(this, addUser));
		startGame.addEventListener("click", Delegate.create(this, createGame));
		
		so = new GlobalObject();
		so.addEventListener("onSync", this);
		soConnected = so.connect("othelloRoomList", p_connection, false);
		
		
		// now get room list
		//getRoom();
	}
// Semi-Private Methods:
// Private Methods:
	private function getRoom():Void
	{
		// get the room list and set to the list view
		var obj = so.getData("mainLobby");
		_global.tt("getRoom");
		players.setDataProvider(obj);
	}
	
	private function onSync(evtObj:Object):Void
	{
		//_global.tt("onSync", evtObj);
		getRoom();
	}
	
	private function createGame():Void
	{
		
	}
	
	private function checkDuplicatePlayers(p_name:String):Boolean
	{
		for(var i:Number = 0;i<players.length;i++)
		{
			if(players.getItemAt(i).data == p_name) return true;
		}
		return false;
	}
	
	private function updateSOList():Void
	{
		so.setData("mainLobby", players.dataProvider);
	}
	
	private function addUser():Void
	{
		// check addNewUser is not empty
		// check for duplicates
		// addName
		// update SO
		
		if(newUserName.text.length < 1 || checkDuplicatePlayers(newUserName.text))
		{
			alert.title = "Invalide Entry";
			alert.show("Please enter a unique player name.")
			return;
		}
		
		players.addItem({label:newUserName.text, data:newUserName.text});
		
		// updating the mainLobby will force everyone else to get a copy
		updateSOList();
	}
}