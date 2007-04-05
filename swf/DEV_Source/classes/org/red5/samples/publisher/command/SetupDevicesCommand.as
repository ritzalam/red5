package org.red5.samples.publisher.command 
{	
	import com.adobe.cairngorm.commands.ICommand;
	import com.adobe.cairngorm.control.CairngormEvent;
	
	import flash.events.*;
	import flash.media.*;
	import flash.net.*;
	
	import org.red5.samples.publisher.business.*;
	import org.red5.samples.publisher.events.*;
	import org.red5.samples.publisher.model.*;
	
	/**
	 * Find and list the available audio and video devices on the system.
	 * <p>The <code>camera</code> and <code>microphone</code> devices are stored in the Model.</p>
	 * 
	 * @author Thijs Triemstra
	 */	
	public class SetupDevicesCommand implements ICommand
	{
		/**
		* 
		*/			
		private var model : ModelLocator = ModelLocator.getInstance();
	 	
	 	[Bindable]
	 	/**
	 	* 
	 	*/	 	
	 	public var monitorTransaction : MonitorTransaction = model.monitorTransaction;
	 
	 	/**
	 	 * 
	 	 * @param cgEvent
	 	 */	 	 	
	 	public function execute( cgEvent : CairngormEvent ) : void
	    { 
			var event : SetupDevicesEvent = SetupDevicesEvent( cgEvent );
			//
			if ( Camera.names.length != 0 ) 
			{
				// Merge options with devices array.
				monitorTransaction.cameraNames = monitorTransaction.cameraNames.concat( Camera.names );
			}
			//
			if ( Microphone.names.length != 0 ) 
			{
				// Merge options with devices array.
				monitorTransaction.microphoneNames = monitorTransaction.microphoneNames.concat( Microphone.names );
			}
		}

	}
}