import mx.remoting.*;
import mx.rpc.*;
import mx.utils.Delegate;

class org.red5.data.RemotingTestData {
// Constants:
	public static var CLASS_REF = org.red5.data.RemotingTestData;
// Public Properties:
	public static var argumentsList:Object;
// Private Properties:
	public static var chkArray:Array;
	public static var chkArrayRepeating:Array;
	public static var chkMixedArray:Array;
	public static var chkMixedObject:Object;
	public static var chkNestedArray:Array;
	public static var chkNestedObject:Object;
	public static var chkNull;
	public static var chkObject:Object;
	public static var chkObjectRepeating:Object;
	public static var chkUndefined;
	public static var chkString:String;
	public static var chkNumber:Number;
	
	public static var repeatReference:Object;
	

//Change the gateway URL as needed
	public static var gatewayUrl:String = "";
	public static var serviceName:String = "";
	public static var remoteMethod:String = "";
	public static var service:Service;

	public static function initialize():Boolean
	{
		// initialize is called from RemotingTest when the xrayLoadComplete method fires
		
		argumentsList = {};
		
		// repeating object reference
		repeatReference = {};
		repeatReference.name = "Red5";
		repeatReference.url = "http://www.osflash.org/red5";
		
		// array data types
		chkArray = ["Joachim", "Luke", "John", "Chris"];
		chkArrayRepeating = [repeatReference,repeatReference,repeatReference];
		
		chkMixedArray = [];
		chkMixedArray.push({prop_0:true});
		chkMixedArray.push(["Van Halen", "Rush", "Dream Theater", "Cheap Trick"]);
		
		chkNestedArray = [];
		chkNestedArray.push(["Star Wars", "The Matrix",["The Merovingean", "Percephone"], "The Great Escape"]);
		
		// object data types
		chkObject = {};
		chkObject.prop_0 = "John";
		chkObject.prop_1 = 37;
		chkObject.prop_3 = true;
		
		chkMixedObject = {};
		chkMixedObject.prop_0 = chkArray;
		chkMixedObject.prop_1 = chkObject;
		
		chkNestedObject = {};
		chkNestedObject["key_0"] = {prop_0:"Luke"};
		chkNestedObject["key_1"] = {prop_0:"Skywalker"};
		chkNestedObject["key_2"] = {};
		chkNestedObject["key_2"].prop_0 = {prop_0:"Red5"};
		
		chkObjectRepeating = {};
		chkObjectRepeating.prop_0 = repeatReference;
		chkObjectRepeating.prop_1 = repeatReference;
		chkObjectRepeating.prop_2 = repeatReference;
		
		//hardcoding that it will be indeed, undefined ;)
		chkUndefined = undefined;
		chkNull = null;
		
		//_global.tt(chkNestedArray, chkNestedObject, chkArrayRepeating, chkObjectRepeating)
		
		return true;
	}
	
	public static function setGateway():Void
	{
		service = new Service(gatewayUrl, null, serviceName);
	}
		
	//Returns all session meta data in an array	
	public static function testRemoteMethod()
	{
		setGateway();
		var args:Array = buildArguments();
		if(args.length < 1) return; // nothing to send
		
		_global.tt("submit", args);
		var pc:PendingCall = service[remoteMethod](args);
		pc.responder = new RelayResponder(RemotingTestData, "handleGetDetails", "handleRemotingError");
		_global.tt("setGateway", service);
	}
		
	private static function buildArguments():Array
	{
		var ary:Array = [];
		
		for(var items:String in argumentsList)
		{ 
			var obj:Object = argumentsList[items];
			if(obj.send)
			{
				//_global.tt("should push", items, RemotingTestData[items]);
				ary.push(RemotingTestData[items]);
			}
		}
		
		return ary;
	}
	
	private static function handleGetDetails(re:ResultEvent)
	{
		//Implement custom callback code
		_global.tt("Remoting result", re);
	}
	
	private static function handleRemotingError( fault:FaultEvent ):Void 
	{
		_global.tt("remoting error", fault);
	}

}