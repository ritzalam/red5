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
	 
	import flash.system.Capabilities;
	
	/**
	 * 
	 * @author Thijs Triemstra
	 */	
	public class Logger
	{

		[Bindable]
		/**
		* 
		*/		
		private var monitorTransaction : MonitorTransaction;
		
		[Bindable]
		/**
		* 
		*/		
		public var flashVersion : String;
		
		[Bindable]
		/**
		* 
		*/		
		public var statusText : String = "";
		
		[Bindable]
		/**
		* 
		*/		
		public var meta_check : Boolean = true;
		
		[Bindable]
		/**
		* 
		*/		
		public var cue_check : Boolean = true;
		
		[Bindable]
		/**
		* 
		*/		
		public var stream_check : Boolean = true;
		
		[Bindable]
		/**
		* 
		*/		
		public var server_check : Boolean = true;
		
		[Bindable]
		/**
		* 
		*/		
		public var audio_check : Boolean = true;
		
		[Bindable]
		/**
		* 
		*/		
		public var video_check : Boolean = true;
		
		/**
		 * 
		 * @return 
		 */		
		public function Logger( env : MonitorTransaction )
		{
			// get Flash version number
			flashVersion = "Using " + Capabilities.manufacturer + " Flash Player " +
						   Capabilities.version.substr( String( Capabilities.version ).lastIndexOf(" ") + 1 );
			//
			if ( Capabilities.isDebugger ) {
				flashVersion += " (Debugger)";
			}
			//
			monitorTransaction = env;
			
			// display flashplayer version
			logMessage( flashVersion, monitorTransaction.infoMessage );

		}
		
		/**
		 * 
		 * @param msg
		 * @param msgType
		 */		
		public function logMessage( msg : String, msgType : String ) : void 
		{
			if (( msgType == monitorTransaction.serverMessage && server_check ) || 
				( msgType == monitorTransaction.streamMessage && stream_check ) ||
				( msgType == monitorTransaction.audioMessage && audio_check ) ||
				( msgType == monitorTransaction.videoMessage && video_check ) ||
				( msgType == monitorTransaction.metadataMessage && meta_check ) ||
				( msgType == monitorTransaction.cuepointMessage && cue_check ) ||
				  msgType == monitorTransaction.infoMessage ||
				  msgType == monitorTransaction.debugMessage ) {
				 //
				 statusText += iso( new Date() ) + " - " + msg + "<br>";
			}
		}
	
		/**
		 * 
		 * @param msg
		 * @param img
		 * @param msgType
		 */		
		public function monitorMessage ( msg : String, 
										 img : Class, 
										 msgType : String ) : void 
		{
			if ( msgType == monitorTransaction.serverMessage ) 
			{
				monitorTransaction.serverStatusMessage = msg;
				monitorTransaction.images.serverStatusImage = img;
			} 
			else if ( msgType == monitorTransaction.audioMessage ) 
			{
				monitorTransaction.audioStatusMessage = msg;
				monitorTransaction.images.audioStatusImage = img;
			} 
			else if ( msgType == monitorTransaction.videoMessage ) 
			{
				monitorTransaction.videoStatusMessage = msg;
				monitorTransaction.images.videoStatusImage = img;
			}
		}
		
		/**
		 * 
		 * @param value
		 * @return 
		 */		
		private function doubleDigits( value : Number ) : String 
		{
			if ( value > 9 ) {
				return String( value );
			} else { 
				return "0" + value;
			}
		}
		
		/**
		 * 
		 * @param value
		 * @return 
		 */		
		private function tripleDigits( value : Number ) : String 
		{
			var newStr : String;
			if ( value > 9 && value < 100 ) {
				newStr = String( value ) + "0";
			} else { 
				newStr = String( value ) + "00";
			}
			return newStr.substr( 0, 3 );
		}
		
		/**
		 * 
		 * @param date
		 * @return 
		 */		
		private function iso( date : Date ) : String 
		{
			return  doubleDigits( date.getHours() )
					+ ":"
					+ doubleDigits( date.getMinutes() )
					+ ":"
					+ doubleDigits( date.getSeconds() )
					+ ":"
					+ tripleDigits( date.getMilliseconds() );
		}
		
	}
}