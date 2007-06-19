package org.red5.samples.echo
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
	import flash.net.*;
	
	import mx.controls.*;
	import mx.core.Application;
	
	/**
	 * 
	 * @author Joachim Bauch
	 * @author Thijs Triemstra
	 */	
	public class EchoTest extends Application
	{
		[Bindable]
		private var nc: NetConnection;
		
		[Bindable]
		private var testParams: Array;
		
		[Bindable]
		private var testIndex: Number;
		
		[Bindable]
		private var AMF0Count: Number;
		
		[Bindable]
		private var testsFailed: Number;
		
		[Bindable]
		public var textArea : TextArea;
		
		[Bindable]
		public var rtmp_txt : TextInput;
		
		[Bindable]
		public var http_txt : TextInput;
		
		[Bindable]
		public var username_txt : TextInput;
		
		[Bindable]
		public var password_txt : TextInput;
		
        public function EchoTest(): void
        {
			// Prepare test values
			testParams = new Array();
			testParams.push(null);
			testParams.push(true);
			testParams.push(false);
			testParams.push("");
			testParams.push("Hello world!");
			var i: Number;
			var longString: String = "";
			for (i=0; i<4000; i++)
				longString = longString + "0123456789";
			testParams.push(longString);
			var reallyLongString: String = "";
			for (i=0; i<7000; i++)
				reallyLongString = reallyLongString + "0123456789";
			testParams.push(reallyLongString);
			var evenLongerString: String = "";
			for (i=0; i<100000; i++)
				evenLongerString = evenLongerString + "0123456789";
			testParams.push(evenLongerString);
			var strings: Array = new Array();
			strings.push("test");
			strings.push("test");
			strings.push("test");
			strings.push("test");
			testParams.push(strings);
			testParams.push(0);
			testParams.push(1);
			testParams.push(-1);
			testParams.push(256);
			testParams.push(-256);
			testParams.push(65536);
			testParams.push(-65536);
			testParams.push(0.0);
			testParams.push(1.5);
			testParams.push(-1.5);
			testParams.push(new Array());
			var tmp1: Array = new Array();
			tmp1.push(1);
			testParams.push(tmp1);
			testParams.push(new Array(1, 2));
			testParams.push(new Array(1, 2, 3));
			var tmp2: Array = new Array();
			tmp2.push(1);
			tmp2[100] = 100;
			testParams.push(tmp2);
			var tmp3: Array = new Array();
			tmp3.push(1);
			tmp3["one"] = 1;
			testParams.push(tmp3);
			var tmp4: Object = {a: "foo", b: "bar"};
			testParams.push(tmp4);
			var tmp5: Array = new Array();
			tmp5.push(tmp4);
			tmp5.push(tmp4);
			testParams.push(tmp5);
			var now: Date = new Date();
			testParams.push(now);
			var tmp6: Array = new Array();
			tmp6.push(now);
			tmp6.push(now);
			testParams.push(tmp6);
			var tmp7: EchoClass = new EchoClass();
			tmp7.attr1 = "one";
			tmp7.attr2 = 1;
			testParams.push(tmp7);
			var tmp8: Array = new Array();
			tmp8.push(tmp7);
			tmp8.push(tmp7);
			testParams.push(tmp8);
			var remote: RemoteClass = new RemoteClass();
			remote.attribute1 = "one";
			remote.attribute2 = 2;
			testParams.push(remote);
			var tmp9: Array = new Array();
			var remote1: RemoteClass = new RemoteClass();
			remote1.attribute1 = "one";
			remote1.attribute2 = 1;
			tmp8.push(remote1);
			var remote2: RemoteClass = new RemoteClass();
			remote2.attribute1 = "two";
			remote2.attribute2 = 2;
			tmp9.push(remote2);
			testParams.push(tmp9);

			AMF0Count = testParams.length;
			
			// Add AMF3 specific tests below
			var ext: ExternalizableClass = new ExternalizableClass();
			testParams.push(ext);
			var tmp_1: Array = new Array();
			tmp_1.push(ext);
			tmp_1.push(ext);
			testParams.push(tmp_1);
			
			nc = new NetConnection();
			nc.addEventListener(NetStatusEvent.NET_STATUS, netStatusHandler); 
			nc.addEventListener( AsyncErrorEvent.ASYNC_ERROR, netASyncError );
            nc.addEventListener( SecurityErrorEvent.SECURITY_ERROR, netSecurityError );
            nc.addEventListener( IOErrorEvent.IO_ERROR, netIOError );
        }
		
		public function onConnect(protocol: String, encoding: uint): void {
			if (nc.connected) {
				nc.close();
			}
			textArea.text = "";
			nc.objectEncoding = encoding;
			//
			if ( username_txt.text.length > 0 ) {
				nc.addHeader("Credentials", false, {username: username_txt.text, password: password_txt.text});
			}
			var url: String;
			if (protocol == "http") {
			    // Remoting...
				url = http_txt.text;
			} else {
				// RTMP...
				url = rtmp_txt.text;
			}
			textArea.text = "Connecting through " + protocol + " using AMF" + encoding + "...\n";
			nc.connect(url);
			
			if (protocol == "http") {
				// Don't wait for a successfull connection for remoting.
				onTest();
			}
		}
		
		private function netStatusHandler(event: NetStatusEvent): void {
			switch(event.info.code) {
				case "NetConnection.Connect.Success":
					textArea.text += event.info.code + ": " + event.info.description + "\n";
					onTest();
					break;
				
				case "NetConnection.Connect.Rejected":
					textArea.text += event.info.code + ": " + event.info.description + "\n";
					onDisconnect();
					break;
					
				case "NetConnection.Connect.Failed":
				case "NetConnection.Connect.Closed":
					textArea.text += event.info.code + "\n";
					onDisconnect();
					break;
			}
		}
		
		private function netSecurityError( event : SecurityErrorEvent ) : void 
		{
			textArea.text += "Security error - " + event.text + "\n";
		}
				
		private function netIOError( event : IOErrorEvent ) : void 
		{
			textArea.text += "IO error - " + event.text + "\n";
		}
				
		private function netASyncError( event : AsyncErrorEvent ) : void 
		{
			textArea.text += "ASync error - " + event.error + "\n";
		}
		
		private function doTest(): void {
			if (testParams[testIndex] is String && (testParams[testIndex] as String).length >= 100)
				textArea.text += "Testing String with " + testParams[testIndex].length + " chars: ";
			else
				textArea.text += "Testing " + testParams[testIndex] + ": ";
			nc.call("echo", new Responder(this.onResult), testParams[testIndex]);
		}
		
		private function onTest(): void {
			testIndex = 0;
			testsFailed = 0;
			doTest();
		}

		private function onDisconnect(): void {
			nc.close();
		}

		private function extendedEqual(a: Object, b: Object): Boolean {
			var key: String;
			if (a == null && b != null) {
				return false;
			} else if (a != null && b == null) {
				return false;
			} else if (a is Array && b is Array) {
				if (a.length != (b as Array).length) {
					return false;
				}
				var i: Number;
				for (i=0; i<(a as Array).length; i++) {
					if (!extendedEqual((a as Array)[i], (b as Array)[i])) {
						return false;
					}
				}
				return true;
			} else if (a is ExternalizableClass && b is ExternalizableClass) {
				return (a.failed == b.failed && a.failed == 0);
			} else if (a is Object && !(b is Object)) {
				for (key in a) {
					if (!extendedEqual(a[key], (b as Array)[key])) {
						return false;
					}
				}
				return true;
			} else if (!(a is Object) && b is Object) {
				for (key in b) {
					if (!extendedEqual((a as Array)[key], b[key])) {
						return false;
					}
				}
				return true;
			} else if (a is Object && b is Object) {
				for (key in a) {
					if (!extendedEqual(a[key], b[key])) {
						return false;
					}
				}
				return true;
			} else {
				return (a == b);
			}
		}
		
		private function onResult(result: Object): void {
		    if (extendedEqual(testParams[testIndex], result)) {
				if (result == null)
					textArea.text += "OK (null)\n";
				else if (result is String && (result as String).length >= 1000)
					textArea.text += "OK (String with " + result.length + " chars)\n";
				else
					textArea.text += "OK (" + result.toString() + ")\n";
			} else {
				if (result == null)
					textArea.text += "FAILED (null)\n";
				else if (result is String && (result as String).length >= 1000)
					textArea.text += "FAILED (String with " + result.length + " chars)\n";
				else
					textArea.text += "FAILED (" + result.toString() + ")\n";
				testsFailed++;
			}
			testIndex += 1;
			var testCount: Number = testParams.length;
			if (nc.objectEncoding == ObjectEncoding.AMF0)
				testCount = AMF0Count;
			if (testIndex < testCount) {
				doTest();
			} else if (testsFailed == 0) {
				textArea.text += "Successfully ran " + testCount + " tests\n";
				onDisconnect();
			} else {
				textArea.text += "Ran " + testCount + " tests, " + testsFailed + " failed\n";
				onDisconnect();
			}
		}
		
	}
}
