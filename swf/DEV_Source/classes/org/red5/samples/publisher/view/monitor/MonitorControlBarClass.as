package org.red5.samples.publisher.view.monitor
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
	 
	import com.adobe.cairngorm.control.CairngormEventDispatcher;
	
	import flash.events.*;
	
	import mx.containers.Box;
	import mx.controls.*;
	import mx.events.*;
	
	import org.red5.samples.publisher.model.MonitorTransaction;
	import org.red5.samples.publisher.events.*;
	
	/**
	 * 
	 * @author Thijs Triemstra
	 */	
	public class MonitorControlBarClass extends Box
	{		
		[Bindable]
		/**
		* 
		*/			
		public var monitorTransaction : MonitorTransaction;
		
		/**
		* 
		*/		
		public var publish_cb : ComboBox;
		
		[Bindable]
		/**
		* 
		*/		
		public var playback_txt : TextInput;
		
		[Bindable]
		/**
		* 
		*/		
		public var publish_txt : TextInput;
		
		[Bindable]
		/**
		* 
		*/		
		public var tooShortError : String = "This name is shorter than the minimum allowed length.";
		
		/**
		 * 
		 * 
		 */		
		public function recordStream() : void
		{
			var publishMode : String = publish_cb.selectedItem.data;
			var streamName : String = publish_txt.text;
			//
			monitorTransaction.publishState = ( monitorTransaction.publishButtonLabel == monitorTransaction.btnPublish );
			//
			if ( monitorTransaction.netConnected ) 
			{
				//
				if ( monitorTransaction.publishState ) 
				{
					// Start publishing.
					var publishStreamEvent : PublishStreamEvent = new PublishStreamEvent( publishMode, streamName );
					CairngormEventDispatcher.getInstance().dispatchEvent( publishStreamEvent );
				} 
				else
				{
					// Stop publishing.
					var unpublishStreamEvent : UnpublishStreamEvent = new UnpublishStreamEvent();
					CairngormEventDispatcher.getInstance().dispatchEvent( unpublishStreamEvent );
				}
			}
		}
		
		/**
		 * 
		 * 
		 */		
		public function viewStream() : void
		{
			var bufferTime : int = monitorTransaction.generalSettings.bufferTime;
			var streamName : String = playback_txt.text;
			//
			monitorTransaction.playbackState = ( monitorTransaction.playButtonLabel == monitorTransaction.btnPlay );
			//
			if ( monitorTransaction.netConnected ) 
			{
				//
				if ( monitorTransaction.playbackState ) 
				{
					// Start playback.
					var playStreamEvent : PlayStreamEvent = new PlayStreamEvent( bufferTime, streamName );
					CairngormEventDispatcher.getInstance().dispatchEvent( playStreamEvent );
				} 
				else
				{
					// Stop playback.
					var stopStreamEvent : StopStreamEvent = new StopStreamEvent();
					CairngormEventDispatcher.getInstance().dispatchEvent( stopStreamEvent );
				}	
			}
		}
		
	}
}