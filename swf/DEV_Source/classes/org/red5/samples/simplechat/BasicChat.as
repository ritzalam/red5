// ** AUTO-UI IMPORT STATEMENTS **
import org.red5.ui.controls.IconButton;
import mx.controls.TextInput;
import mx.controls.TextArea;
// ** END AUTO-UI IMPORT STATEMENTS **
import org.red5.utils.GlobalObject;
import org.red5.net.Connection;
import org.red5.utils.Delegate;

class org.red5.samples.simplechat.BasicChat extends MovieClip {
// Constants:
	public static var CLASS_REF = org.red5.samples.simplechat.BasicChat;
	public static var LINKAGE_ID:String = "org.red5.samples.simplechat.BasicChat";
// Public Properties:
	public var connection:Connection;
// Private Properties:
	private var connected:Boolean;
	private var so:GlobalObject;
	private var history:Array;
// UI Elements:

// ** AUTO-UI ELEMENTS **
	private var chatBody:TextArea;
	private var clearChat:IconButton;
	private var message:TextInput;
// ** END AUTO-UI ELEMENTS **

// Initialization:
	private function BasicChat() {}
	private function onLoad():Void { configUI(); }

// Public Methods:
	public function registerConnection(p_connection:Connection):Void
	{
		connection = p_connection;
	}
	
	public function connectSO():Void
	{
		// parms
		// @ SO name
		// @ Connection reference
		// @ persistance
		connected = so.connect("SampleChat", connection, false);
	}
// Semi-Private Methods:
// Private Methods:
	private function configUI():Void 
	{
		// instantiate history object
		history = new Array();
		
		// add key listener for enter key
		Key.addListener(this);
		
		// create GlobalObject
		so = new GlobalObject();
		
		// add listener for sync events
		so.addEventListener("onSync", Delegate.create(this, newMessageHandler));
		
		// setup the clearChat button
		clearChat.addEventListener("click", Delegate.create(this, clear))
		clearChat.tooltip = "Clear Chat";
	}	
	
	private function onKeyUp():Void
	{
		if(Key.getCode() == 13 && message.length > 0)
		{
			// send message
			so.setData("simpleChat", message.text);
			
			// clear text input
			message.text = "";
		}
	}
	
	private function clear():Void
	{
		// clear chat
		chatBody.text = "";
		
		// clear doesn't work on Red5 yet
		so.clear();
	}
	
	private function newMessageHandler(evtObj:Object):Void
	{
		// we've been notified that there's a new message, go get it
		var newChat:String = so.getData("simpleChat");
		
		// push to history
		history.push(newChat);
		
		// show in chat
		chatBody.text = history.join("\n");
		
		// scroll the chat window
		chatBody.vPosition = chatBody.maxVPosition;
	}
}