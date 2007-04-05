package org.red5.samples.publisher.command 
{	
	import com.adobe.cairngorm.commands.ICommand;
	import com.adobe.cairngorm.control.CairngormEvent;
	
	import flash.events.*;
	import flash.media.*;
	import flash.net.*;
	
	import mx.rpc.IResponder;
	
	import org.red5.samples.publisher.business.*;
	import org.red5.samples.publisher.events.*;
	import org.red5.samples.publisher.model.*;
	
	/**
	 * Setup two NetStreams for publishing and playback.
	 * <p>The NetStreams are stored in the Model.</p>
	 * 
	 * @see org.red5.samples.publisher.model.Media#nsPlay nsPlay
	 * @see org.red5.samples.publisher.model.Media#nsPublish nsPublish
	 * @author Thijs Triemstra
	 */	
	public class SetupStreamsCommand implements ICommand, IResponder 
	{
		/**
		* 
		*/			
		private var model : ModelLocator = ModelLocator.getInstance();
	 	
	 	/**
	 	* 
	 	*/	 	
	 	private var monitorTransaction : MonitorTransaction = model.monitorTransaction;
	 	
	 	/**
	 	* 
	 	*/	 	
	 	private var streamMessage : String = monitorTransaction.streamMessage;
	 	
	 	/**
		* 
		*/		
		private var metadataMessage : String = monitorTransaction.metadataMessage;
		
		/**
		* 
		*/		
		private var cuepointMessage : String = monitorTransaction.cuepointMessage;
	 	
	 	/**
	 	* 
	 	*/	 	
	 	private var logger : Logger = model.logger;
	 	
	 	/**
	 	* 
	 	*/	 	
	 	private var playbackFinished : Boolean = false;
	 
	 	/**
	 	 * 
	 	 * @param cgEvent
	 	 */	 	 	
	 	public function execute( cgEvent : CairngormEvent ) : void
	    { 
			var event : SetupStreamsEvent = SetupStreamsEvent( cgEvent );
			// Setup the permanent Delegate to create NetStreams.
			monitorTransaction.ns_delegate = new NetStreamDelegate( this );
		}
		
		/**
		 * 
		 * The result method is called when the delegate receives 
		 * a result from the service
		 * 
		 * @param event
		 */		
		public function result(  event : Object  ) : void 
		{
			var info : Object = event.info;
			var statusCode : String = info.code;
			var ns_type : String = "Playback";
			//
			switch ( statusCode ) {
				case "NetStream.Play.Start" :
					playbackFinished = false;
					break;
					
				case "NetStream.Play.Stop":	
					playbackFinished = true;		
					break;
				
				case "NetStream.Buffer.Empty":	
					//
					if ( playbackFinished ) 
					{
						//
						playbackStopped();
					}		
					break;
					
				case "NetStream.Play.StreamNotFound":
					break;
					
				case "NetStream.Publish.Start":
					ns_type = "Publish";
					break;
					
				case "NetStream.Publish.Idle":
					ns_type = "Publish";
					break;
					
				case "NetStream.Record.Failed":
					ns_type = "Publish";
					break;
					
				case "NetStream.Record.Stop":
					ns_type = "Publish";
					break;
					
				case "NetStream.Record.Start":
					ns_type = "Publish";
					break;
					
				case "NetStream.Unpublish.Success":
					ns_type = "Publish";
					break;
					
				case "NetStream.Publish.BadName":
					ns_type = "Publish";
					//
					publishStopped();
					break;
			}
			//
			logger.logMessage( ns_type + " - " + statusCode, streamMessage );
		}
		
		/**
		 * The fault method is called when the delegate receives a fault from the service
		 * 
		 * @param event
		 */		
		public function fault(  event : Object  ) : void
		{
			//
			logger.logMessage ( event.text, streamMessage );
		}
		
		/**
		 * <p>Not available in FCS 1.5.</p>
		 * 
		 * @param info
		 */		
		public function onPlayStatus( info : Object ) : void 
		{	
			//
			logger.logMessage( "Playback - " + info.code, streamMessage );
		}
		
		/**
		 * 
		 * @param info
		 */		
		public function onMetaData ( info : Object ) : void 
		{
			for ( var d : String in info ) 
			{
				logger.logMessage( "Metadata - " + d + ": " + info[ d ], metadataMessage );
			}
		}
				
		/**
		 * 
		 * @param info
		 */		
		public function onCuePoint( info : Object ) : void 
		{
			for ( var d : String in info ) 
			{
				logger.logMessage( "Cuepoint - " + d + ": " + info[ d ], cuepointMessage );
			}
		}
		
		/**
		 * 
		 * 
		 */		
		private function playbackStopped() : void
		{
			//
			playbackFinished = false;
			//
			monitorTransaction.playButtonLabel = monitorTransaction.btnPlay;
			//
			monitorTransaction.playbackState = false;
		}
		
		/**
		 * 
		 * 
		 */		
		private function publishStopped() : void 
		{
			//
			monitorTransaction.publishButtonLabel = monitorTransaction.btnPublish;
			//
			monitorTransaction.publishState = false;
		}

	}
}