package org.red5.samples.echo.view
{
	/**
	 * RED5 Open Source Flash Server - http://www.osflash.org/red5
	 *
	 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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
	
	import flash.events.AsyncErrorEvent;
	import flash.events.IOErrorEvent;
	import flash.events.NetStatusEvent;
	import flash.events.SecurityErrorEvent;
	
	import mx.containers.Panel;
	import mx.events.FlexEvent;
	
	import org.red5.samples.echo.events.ConnectEvent;
	import org.red5.samples.echo.events.DisconnectEvent;
	import org.red5.samples.echo.events.PrintTextEvent;
	import org.red5.samples.echo.events.SetupConnectionEvent;
	import org.red5.samples.echo.events.StartTestsEvent;
	import org.red5.samples.echo.events.TestSelectEvent;
	import org.red5.samples.echo.model.ModelLocator;
	
	/**
	 * Echo test application for AMF and RTMP server implementations.
	 * 
	 * Contains tests for ActionScript 1.0 and 2.0 data types (AMF0),
	 * and ActionScript 3.0 data types (AMF3). Tests are transported
	 * using the NetConnection or RemoteObject class over the AMF or 
	 * RTMP protocol.
	 * 
	 * @author Joachim Bauch (jojo@struktur.de)
	 * @author Thijs Triemstra (info@collab.nl)
	*/	
	public class Main extends Panel
	{
		[Bindable]
		public var model : ModelLocator = ModelLocator.getInstance();
		
		private var printTextEvent : PrintTextEvent;
		
        public function Main()
        {
        	this.addEventListener(FlexEvent.CREATION_COMPLETE, setupApp);
        }
		
		private function setupApp(event:FlexEvent):void
		{
			var setupEvent:SetupConnectionEvent = new SetupConnectionEvent();
			setupEvent.dispatch();
		}
		
		public function onConnect(protocol:String, encoding:uint):void 
		{
			var connectEvent:ConnectEvent = new ConnectEvent(protocol, encoding);
			connectEvent.dispatch();
			
			model.nc.client = this;
			model.nc.addEventListener(NetStatusEvent.NET_STATUS, 			netStatusHandler); 
			model.nc.addEventListener(AsyncErrorEvent.ASYNC_ERROR, 			netASyncError);
            model.nc.addEventListener(SecurityErrorEvent.SECURITY_ERROR, 	netSecurityError);
            model.nc.addEventListener(IOErrorEvent.IO_ERROR, 				netIOError);
		}
		
		public function setupTests(event:TestSelectEvent):void
		{
			//trace(event.result.parent + " : " + event.result.name);
			model.tests_selection.push(event.result);
		}
		
		public function updateTests(event:TestSelectEvent):void
		{
			for (var d:int=0;d<model.tests_selection.length;d++) {
				if (model.tests_selection[d].name == event.result.name &&
					model.tests_selection[d].parent == event.result.parent) {
						model.tests_selection.splice(d, 1, model.tests_selection[d]);
						trace(event.result.parent + " : " + event.result.name + " : " + event.result.selected);
						break;
					}
			}
		}
		
		private function netStatusHandler(event:NetStatusEvent):void 
		{
			var infoCode:String = event.info.code;
			var disconnectEvent:DisconnectEvent;
			
			model.connecting = false;
			
			switch (infoCode) 
			{
				case "NetConnection.Connect.Success":
					var startTestsEvent	: StartTestsEvent = new StartTestsEvent();
					startTestsEvent.dispatch();
					break;
				
				case "NetConnection.Connect.Rejected":
					disconnectEvent = new DisconnectEvent();
					disconnectEvent.dispatch();
					break;
					
				case "NetConnection.Connect.Failed":
				case "NetConnection.Connect.Closed":
					disconnectEvent = new DisconnectEvent();
					disconnectEvent.dispatch();
					break;
			}
		}
		
		private function netSecurityError(event:SecurityErrorEvent):void 
		{
			printTextEvent = new PrintTextEvent("<b>" + model.failure +
												"Security error</font></b>: " + event.text);
			printTextEvent.dispatch();
		}
				
		private function netIOError(event:IOErrorEvent):void 
		{
			printTextEvent = new PrintTextEvent("<b>" + model.failure +
																   "I/O error</font></b>: " +
																   event.text);
			printTextEvent.dispatch();
		}
				
		private function netASyncError(event:AsyncErrorEvent):void 
		{
			printTextEvent = new PrintTextEvent("<b>" + model.failure +
												"Async error</font></b>: " +
												event.error.getStackTrace());
			printTextEvent.dispatch();
		}
		
	}
}
