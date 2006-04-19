import org.osflash.data.XMLObject;
import com.dynamicflash.utils.Delegate;
//import com.gskinner.events.GDispatcher;
import mx.events.EventDispatcher

class org.red5.fitc.presentation.ConfigDelegate {
// Constants:
	public static var CLASS_REF = org.red5.fitc.presentation.ConfigDelegate;
// Public Properties:
	public static var initialized:Boolean = initialize();
	public static var addEventListener:Function;
	public static var removeEventListener:Function;
// Private Properties:
	private static var dispatchEvent:Function;
	private static var xmlDoc:XML;
	private static var loaded:Boolean = false;
	private static var content:Object;


// Public Methods:
	public static function initialize():Boolean
	{
		EventDispatcher.initialize(ConfigDelegate);
		xmlDoc = new XML();
		xmlDoc.ignoreWhite = true;
		return true;
	}
	
	public static function getContent(p_section:String):String
	{
		_global.tt("getContent", p_section, content[p_section]);
		return content[p_section];
	}
	
	public static function loadData(p_xml:String):Void
	{
		_global.tt("loadData", p_xml);
		if(p_xml == undefined) 
		{
			_global.tt("No XML doc was specified");
			return;
		}
		
		xmlDoc.onLoad = onDataLoad;
		xmlDoc.load(p_xml);
	}
// Semi-Private Methods:
// Private Methods:
	private static function onDataLoad(success):Void
	{
		//_global.tt("onDataLoad", success);
		if(!success) _global.tt("XML Data not loaded");
		loaded = true;
		
		content = XMLObject.getObject(xmlDoc).root.presentation;
		_global.tt("content", content);
		dispatchEvent({type:"onLoadComplete"});
	}
}