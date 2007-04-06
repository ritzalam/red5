package org.red5.samples.publisher.business
{
	/**
	 * RED5 Open Source Flash Server - http://www.osflash.org/red5
	 *
	 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
	 *
	 * This library is free software; you can redistribute it and/or modify it under the
	 * terms of the GNU Lesser General Public License as published by the Free Software
	 * Foundation; either version 2.1 of the License, or (at your option) any later
	 * version.
	 *
	 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
	 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
	 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
	 *
	 * You should have received a copy of the GNU Lesser General Public License along
	 * with this library; if not, write to the Free Software Foundation, Inc.,
	 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
	 */
	 
	import com.adobe.cairngorm.business.ServiceLocator;
	import com.adobe.cairngorm.control.CairngormEventDispatcher;
	
	import flash.events.*;
	import flash.media.*;
	import flash.net.*;
	
	import mx.rpc.IResponder;
	
	import org.red5.samples.publisher.events.*;
	import org.red5.samples.publisher.model.*;
	import org.red5.samples.publisher.view.general.Images;	
	
	/**
	 * 
	 * @author Thijs Triemstra
	 */	
	public class NetStreamDelegate
	{	
		/**
		* 
		*/		
		private var model : ModelLocator = ModelLocator.getInstance();
		
		/**
		* 
		*/		
		private var responder : IResponder;
		
		/**
		* 
		*/		
		private var service : Object;
		
		/**
	 	* 
	 	*/	 	
	 	private var logger : Logger = model.logger;

		/**
		* 
		*/		
		public var monitorTransaction : MonitorTransaction = model.monitorTransaction;
		
		/**
	 	* 
	 	*/	 	
	 	private var images : Images = monitorTransaction.images;
	 	
		/**
		* 
		*/		
		private var serverMessage : String = monitorTransaction.serverMessage;
		
		/**
		* 
		*/		
		private var streamMessage : String = monitorTransaction.streamMessage;
				
		/**
		*  
		*/		
		private var nsPublish : NetStream = monitorTransaction.media.nsPublish;
		
		/**
		*  
		*/		
		private var nsPlay : NetStream = monitorTransaction.media.nsPlay;
		
		/**
		*  
		*/		
		private var camera : Camera;
		
		/**
		*  
		*/		
		private var microphone : Microphone;
		
		/**
		 * 
		 * @param res
		 */		
		public function NetStreamDelegate( res : IResponder )
		{
			// Generate unique streamname.
			monitorTransaction.streamName = "stream" + String( Math.floor( new Date().getTime() ) );
			// Listen and capture the NetConnection info and error events.
			responder = res;
		}
		
		/**
		 * 
		 * @param bufferTime
		 * @param streamName
		 */			
		public function startPlayback( bufferTime : int, streamName : String ) : void
		{
			try 
			{
				// Check for reconnect.
				if ( nsPlay != null ) 
				{
					// Stop and close previous NetStream.
					var stopStreamEvent : StopStreamEvent = new StopStreamEvent();
					CairngormEventDispatcher.getInstance().dispatchEvent( stopStreamEvent );
				}
				// Setup NetStream for playback.
				nsPlay = new NetStream( monitorTransaction.media.nc );
				//
				nsPlay.addEventListener( NetStatusEvent.NET_STATUS, netStatusEvent );
				nsPlay.addEventListener( IOErrorEvent.IO_ERROR, netIOError );
				nsPlay.addEventListener( AsyncErrorEvent.ASYNC_ERROR, netASyncError );
				//
				nsPlay.bufferTime = bufferTime;
				//
				nsPlay.client = responder;
				//
				monitorTransaction.media.videoRemote = new Video( 320, 240 );
				monitorTransaction.media.videoRemote.attachNetStream( nsPlay );
				//
				monitorTransaction.playButtonLabel = monitorTransaction.btnStop;
				//
				nsPlay.play( streamName );
				
			}
			catch( e : ArgumentError ) 
			{
				//
				monitorTransaction.playbackState = false;
				// Invalid parameters
				switch ( e.errorID ) 
				{
					// NetStream object must be connected.
					case 2126 :
						//
						logger.logMessage( "Can't play stream, not connected to server", serverMessage );
						logger.monitorMessage( "Not connected to server", 
											   images.warnServer_img, 
											   serverMessage );
						break;
					//
					default :
					   //
					   break;
				}
			}
		}
		
		/**
		 * 
		 * 
		 */		
		public function stopPlayback() : void
		{
			// Change Button label.
			monitorTransaction.playButtonLabel = monitorTransaction.btnPlay;
			// Close the NetStream.
			nsPlay.close();
		}
		
		/**
		 * 
		 * @param publishMode
		 * @param streamName
		 */		
		public function startPublish( publishMode : String, streamName : String ) : void
		{
			try 
			{
				camera = monitorTransaction.media.camera;
				microphone = monitorTransaction.media.microphone;
				//
				if ( microphone != null || camera != null ) 
				{
					// close previous stream
					if ( nsPublish != null ) 
					{
						// Stop and unpublish current NetStream.
						var unpublishStreamEvent : UnpublishStreamEvent = new UnpublishStreamEvent();
						CairngormEventDispatcher.getInstance().dispatchEvent( unpublishStreamEvent );
					}
					// Setup NetStream for publishing.
					nsPublish = new NetStream( monitorTransaction.media.nc );
					//
					nsPublish.addEventListener( NetStatusEvent.NET_STATUS, netStatusEvent );
					nsPublish.addEventListener( IOErrorEvent.IO_ERROR, netIOError );
					nsPublish.addEventListener( AsyncErrorEvent.ASYNC_ERROR, netASyncError );
					//
					nsPublish.client = responder;	
					// attach devices to NetStream.
					if ( camera != null ) 
					{
						nsPublish.attachCamera( camera );
					}
					if ( microphone != null) 
					{
						nsPublish.attachAudio( microphone );
					}
					//
					monitorTransaction.publishButtonLabel = monitorTransaction.btnUnpublish;
					//
					monitorTransaction.publishState = true;
					// Start publishing.
					nsPublish.publish( streamName, publishMode );
				} 
				else 
				{
					logger.logMessage( "Can't publish stream, no input device(s) selected", streamMessage );
					//
					monitorTransaction.publishState = false;
				}
			}
			catch( e : ArgumentError ) 
			{
				//
				monitorTransaction.publishState = false;
				// Invalid parameters
				switch ( e.errorID ) 
				{
					// NetStream object must be connected.
					case 2126 :
						//
						logger.logMessage( "Can't publish stream, not connected to server", streamMessage );
						logger.monitorMessage(	"Not connected to server", 
												images.warnServer_img, 
												serverMessage );
						break;
					//
					default :
					   //
					   logger.logMessage( String( e ), streamMessage );
					   break;
				}
			}
		}
		
		/**
		 * 
		 * 
		 */		
		public function stopPublish() : void
		{
			//
			monitorTransaction.publishButtonLabel = monitorTransaction.btnPublish;
			//
			nsPublish.close();
			//
			monitorTransaction.publishState = false;
		}
			
		/**
		 * 
		 * @param event
		 */		
		protected function netStatusEvent( event : NetStatusEvent ) : void 
		{
			// Pass NetStatusEvent to Command.
			responder.result( event );
		}
		
		/**
		 * 
		 * @param event
		 */		
		protected function netSecurityError( event : SecurityErrorEvent ) : void 
		{
			// Pass SecurityErrorEvent to Command.
		    responder.fault( new SecurityErrorEvent ( SecurityErrorEvent.SECURITY_ERROR, false, true,
		    										  "Security error - " + event.text ) );
		}
		
		/**
		 * 
		 * @param event
		 */		
		protected function netIOError( event : IOErrorEvent ) : void 
		{
			// Pass IOErrorEvent to Command.
			responder.fault( new IOErrorEvent ( IOErrorEvent.IO_ERROR, false, true, 
							 "Input/output error - " + event.text ) );
		}
		
		/**
		 * 
		 * @param event
		 */		
		protected function netASyncError( event : AsyncErrorEvent ) : void 
		{
			// Pass AsyncErrorEvent to Command.
			responder.fault( new AsyncErrorEvent ( AsyncErrorEvent.ASYNC_ERROR, false, true,
							 "Asynchronous code error - <i>" + event.error + "</i>" ) );
		}
		
    }
}