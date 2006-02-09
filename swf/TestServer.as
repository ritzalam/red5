import mx.utils.*;
import mx.controls.*;

class TestServer extends MovieClip
{
	private static var CLICK="click";
	
	
	private var helloButton:Button;
	private var connectButton:Button;
	private var resultTextArea:TextArea;
	private var urlTextInput:TextInput;
	
	private var nc:NetConnection;
	
	
	
	public function TestServer()
	{
		
	}
	
	public function onLoad()
	{
		addResult("Client Started");
		helloButton.addEventListener(CLICK, Delegate.create(this, onHelloClicked));
		connectButton.addEventListener(CLICK, connect);
		nc = new NetConnection();
		
		nc.onStatus = Delegate.create(this, onStatus);
		
		urlTextInput.text = "rtmp://localhost:7009/test";
		connect();
		
	}
	
	private function addResult(result:String)
	{
		resultTextArea.text+= result + "\n";
	}
	
	private function connect()
	{
		addResult("Connecting to " + urlTextInput.text);
		nc.connect(urlTextInput.text);
		
		
	}
	
	private function onStatus(info)
	{
		addResult(info);
	}
	
	private function onHelloClicked(eventObj:Object)
	{
		addResult("Attempting to connect to hello service");
		var hello:String = "hello";
		nc.call("HelloTest", onStatus,hello);
	}
	
	
}