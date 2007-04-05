package org.red5.samples.publisher.model
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
	 
	import flash.events.*;
	import flash.media.*;
	import flash.net.*;
	
	import mx.events.*;
	
	import org.red5.samples.publisher.business.*;
	import org.red5.samples.publisher.view.general.Images;
	import org.red5.samples.publisher.vo.*;
	import org.red5.samples.publisher.vo.settings.*;
	
	/**
	 * 
	 * @author Thijs Triemstra
	 */	
	public class MonitorTransaction
	{
		// available values for the monitor tabnavigator
		public var MONITOR_SERVER : int =		0;
		public var MONITOR_VIEW : int =			1;
		public var MONITOR_PUBLISH : int =		2;
		
		// available values for the settings view
		public var SETTINGS_SERVER : int =	0;
		public var SETTINGS_VIDEO : int =	1;
		public var SETTINGS_AUDIO : int =	2;
		
		[Bindable]
		/**
		* 
		*/		
		public var selectedPreset : int = 0;

		[Bindable]
		/**
		* 
		*/		
		public var monitorDisplayViewing : Number = MONITOR_VIEW;
		
		[Bindable]
		/**
		* 
		*/		
		public var settingsViewing : Number = SETTINGS_SERVER;
		
		[Bindable]
		/**
		* 
		*/		
		public var monitorBarIndex : Number = SETTINGS_SERVER;

		[Bindable]
		/**
		* 
		*/		
		public var status : int;
		
		[Bindable]
		/**
		* 
		*/		
		public var streamName : String;
		
		[Bindable]
		/**
		* 
		*/		
		public var publishMode : int;
		
		[Bindable]	
		/**
		* 
		*/		
		public var netConnected : Boolean;
		
		[Bindable]
		/**
		* 
		*/		
		public var previewState : Boolean = false;
		
		[Bindable]
		/**
		* 
		*/		
		public var playbackState : Boolean = false;
		
		[Bindable]
		/**
		* 
		*/		
		public var publishState : Boolean = false;

		[Bindable]
		/**
		* 
		*/		
		public var serverStatusMessage : String = "Choose your server and press Connect";
		
		[Bindable]
		/**
		* 
		*/		
		public var audioStatusMessage : String = "Choose your audio device";
		
		[Bindable]
		/**
		* 
		*/		
		public var videoStatusMessage : String = "Choose your video device";

		/**
		* 
		*/		
		public var docsURL : String = "http://livedocs.macromedia.com/flex/201/langref/flash/";
		
		[Bindable]
		/**
		* 
		*/		
		public var infoMessage : String = "Info";
		
		[Bindable]
		/**
		* 
		*/		
		public var serverMessage : String = "NetConnection";
		
		[Bindable]
		/**
		* 
		*/		
		public var audioMessage : String = "Audio";
		
		[Bindable]
		/**
		* 
		*/		
		public var videoMessage : String = "Video";
		
		[Bindable]
		/**
		* 
		*/		
		public var debugMessage : String = "Debug";
		
		[Bindable]
		/**
		* 
		*/		
		public var streamMessage : String = "NetStream";
		
		[Bindable]
		/**
		* 
		*/		
		public var metadataMessage : String = "MetaData";
		
		[Bindable]
		/**
		* 
		*/		
		public var cuepointMessage : String = "CuePoints";
		
		[Bindable]
		/**
		* 
		*/		
		public var btnConnect : String = "Connect";
		
		[Bindable]
		/**
		* 
		*/		
		public var btnConnected : String = "Close";
		
		[Bindable]
		/**
		* 
		*/		
		public var connectButtonLabel : String =  btnConnect;
		
		[Bindable]
		/**
		* 
		*/		
		public var btnPlay : String = "Play";
		
		[Bindable]
		/**
		* 
		*/		
		public var btnStop : String = "Stop";
		
		[Bindable]
		/**
		* 
		*/		
		public var playButtonLabel : String =  btnPlay;

		[Bindable]
		/**
		* 
		*/		
		public var btnPublish : String = "Publish";
		
		[Bindable]
		/**
		* 
		*/		
		public var btnUnpublish : String = "Stop";
		
		[Bindable]
		/**
		* 
		*/		
		public var publishButtonLabel : String =  btnPublish;
		
		/**
		* SharedObject to store presets.
		*/		
		public var mySo : SharedObject = SharedObject.getLocal( "publisher" );
				
		[Bindable] 
		/**
		* 
		*/		
		public var tempServerPreset : ServerPreset;
				
		[Bindable]
		/**
		* 
		*/		
		public var images : Images;
		
		[Bindable]
		/**
		* 
		*/		
		public var media : Media;
		
		/**
		* 
		*/		
		public var nc_delegate : NetConnectionDelegate;
		
		/**
		* 
		*/		
		public var ns_delegate : NetStreamDelegate;
				
		[Bindable]
		/**
		* 
		*/		
		public var generalSettings : GeneralSettings;
		
		[Bindable]
		/**
		* 
		*/		
		public var audioSettings : AudioSettings;
		
		[Bindable] 
		/**
		* 
		*/		
		public var videoSettings : VideoSettings;

		/**
		* 
		*/		
		public var orgServerPresets : Array;
		
		[Bindable]
		/**
		* 
		*/		
		public var settingsArray : Array = new Array();
		
		[Bindable]
		/**
		* 
		*/		
		public var serverTypes : Array = new Array();
			
		[Bindable]
		/**
		* 
		*/		
		public var publishTypes : Array =  	 [
										 		{ label: "Live", data: "live" },
										 		{ label: "Record", data: "record" },
										 		{ label: "Append", data: "append" }
										 	 ];
		[Bindable]
		/**
		* 
		*/		
		public var monitorMenu : Array = 	 [	
												{ label: "View", toolTip: "View Stream", data: MONITOR_VIEW }, 
										  		{ label: "Publish", toolTip: "Publish Stream", data: MONITOR_PUBLISH }
										 	 ];
										 	 
		[Bindable]
		/**
		* 
		*/		
		public var objectEncodeTypes : Array =  [	
													{ label: "AMF0", data: ObjectEncoding.AMF0 },
													{ label: "AMF3", data: ObjectEncoding.AMF3 }
											 	];
											 
		[Bindable]
		/**
		* 
		*/		
		public var serverPresets : Array = 	 [	
												new ServerPreset( "localhost oflaDemo",
																  "rtmp://localhost/oflaDemo", 
																  0, 0, 0 ), 
								  				{ label:"------------------------------------" },
								  				{ label:"Save server preset..." },
								  				{ label:"Remove presets..." }
								  			 ];	
								  			 
		[Bindable]
		/**
		* 
		*/		
		public var proxyTypes : Array = 	[
												{ label:"None", data:"none" },
												{ label:"HTTP", data:"HTTP" },
												{ label:"Connect", data:"CONNECT" },
												{ label:"Best", data:"best" }
											];			 
		[Bindable]
		/**
		* 
		*/		
		public var cameraNames : Array = 	[
												"Select video device",
												"No video"
											];
		[Bindable]
		/**
		* 
		*/		
		public var microphoneNames : Array = [
												"Select audio device",
												"No audio"
											 ];				 
		/**
		 *
		 */		
		public function MonitorTransaction()
		{
			// Create blank general settings VO.
			generalSettings = new GeneralSettings();
			// Create new video settings VO and use default parameters.
			videoSettings = new VideoSettings();
			// Create new video settings VO and use default parameters.
			audioSettings = new AudioSettings();
			// Create references to the bitmap images used in this application.
			images = new Images();
			//
			media = new Media();
			//
			serverTypes = 	[	
								{ label: "Red5", img: images.red5_img },
								{ label: "Flash Media", img: images.fms_img }
							];
			//							   	 
			settingsArray = [	
								{ label: "Server", toolTip: "Server Settings", 
								  data: SETTINGS_SERVER, img: images.server_img }, 
									
								{ label: "Video", toolTip: "Video Settings", 
								  data: SETTINGS_VIDEO, img: images.webcam_img },
									 
								{ label: "Audio", toolTip: "Audio Settings", 
								  data: SETTINGS_AUDIO, img: images.sound_img }
							];
			//
			images.serverLogo = images.red5_img;
			images.settingsIcon = images.server_img;
			//
			images.serverStatusImage = images.goServer_img;
			//		
		    images.audioStatusImage = images.goSound_img;
			//
			images.videoStatusImage = images.goWebcam_img;
			// keep a copy of original presets
			orgServerPresets = serverPresets.slice();
			// load serverpresets from SharedObjects when available
			if ( mySo.data.serverPresets != null ) 
			{
				serverPresets = mySo.data.serverPresets;
			}
		}
		
	}
}