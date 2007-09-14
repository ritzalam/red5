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
	
	import flash.display.BitmapData;
	import flash.events.*;
	import flash.net.*;
	import flash.system.Capabilities;
	import flash.utils.ByteArray;
	import flash.utils.getTimer;
	
	import mx.collections.ArrayCollection;
	import mx.controls.*;
	import mx.core.Application;
	import mx.rpc.Fault;
	import mx.rpc.events.FaultEvent;
	import mx.rpc.remoting.mxml.RemoteObject;
	import mx.utils.ObjectProxy;
	
	import org.red5.samples.echo.EchoClass;
	import org.red5.samples.echo.RemoteClass;
	import org.red5.samples.echo.ExternalizableClass;
	import org.red5.utils.PNGEnc;
	
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
		public var rtmp_txt : TextInput;
		
		[Bindable]
		public var http_txt : TextInput;
		
		[Bindable]
		public var responder : Responder;
		
		[Bindable]
		public var echoService : RemoteObject;
		
		[Bindable]
		public var username_txt : TextInput;
		
		[Bindable]
		public var password_txt : TextInput;
		
		[Bindable]
		public var fpVersion : String;
		
		[Bindable]
		public var testResult : String;
		
		private var success : String = "<font color='#149D25'>";
		private var failure : String = "<font color='#FF1300'>";
		private var timer : int;
		
		/**
	 	 * Create and send test data.
		 */	
        public function EchoTest(): void
        {
        	// Display FP version nr.
        	fpVersion = "FP " + Capabilities.version;
        	
			// stores the test data
			testParams = new Array();
			testParams.push(null);
			// test Booleans
			testParams.push(true);
			testParams.push(false);
			testParams.push("");
			testParams.push("Hello world!");
			// long Strings
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
			// Strings
			var strings: Array = new Array();
			strings.push("test");
			strings.push("test");
			strings.push("test");
			strings.push("test");
			testParams.push(strings);
			// Numbers
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
			// Arrays
			var tmp1: Array = new Array();
			tmp1.push(1);
			testParams.push(tmp1);
			testParams.push([1, 2]);
			testParams.push([1, 2, 3]);
			var tmp2: Array = new Array();
			tmp2.push(1);
			tmp2[100] = 100;
			testParams.push(tmp2);
			var tmp3: Array = new Array();
			tmp3.push(1);
			tmp3["one"] = 1;
			testParams.push(tmp3);
			// Object
			var tmp4: Object = {a: "foo", b: "bar"};
			testParams.push(tmp4);
			var tmp5: Array = new Array();
			tmp5.push(tmp4);
			tmp5.push(tmp4);
			testParams.push(tmp5);
			// Date
			var now: Date = new Date();
			testParams.push(now);
			var tmp6: Array = new Array();
			tmp6.push(now);
			tmp6.push(now);
			testParams.push(tmp6);
			// Custom class
			var tmp7: EchoClass = new EchoClass();
			tmp7.attr1 = "one";
			tmp7.attr2 = 1;
			testParams.push(tmp7);
			var tmp8: Array = new Array();
			tmp8.push(tmp7);
			tmp8.push(tmp7);
			testParams.push(tmp8);
			// Remote class
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
			var remote3: RemoteClass = new RemoteClass();
			remote3.attribute1 = "three";
			remote3.attribute2 = 1234567890;
			testParams.push(remote3);
			var remote4: RemoteClass = new RemoteClass();
			remote4.attribute1 = "four";
			remote4.attribute2 = 1185292800000;
			testParams.push(remote4);
			// XML
			XML.ignoreComments = false;
			XML.ignoreProcessingInstructions = false;
			XML.prettyIndent = 0;
			XML.prettyPrinting = false;
			var customSettings:Object = XML.settings();
			var tmpXML:XML =
                <employees>
                    <employee ssn="123-123-1234">
                        <name first="John" last="Doe"/>
                        <address>
                            <street>11 Main St.</street>
                            <city>San Francisco</city>
                            <state>CA</state>
                            <zip>98765</zip>
                        </address>
                    </employee>
                    <employee ssn="789-789-7890">
                        <name first="Mary" last="Roe"/>
                        <address>
                            <street>99 Broad St.</street>
                            <city>Newton</city>
                            <state>MA</state>
                            <zip>01234</zip>
                        </address>
                    </employee>
                </employees>;
            testParams.push(tmpXML);
            var tmp10: Array = new Array();
			tmp10.push(tmpXML);
			tmp10.push(tmpXML);
			testParams.push(tmp10);
			AMF0Count = testParams.length;
			
			// Add AMF3 specific tests below
			var ext: ExternalizableClass = new ExternalizableClass();
			testParams.push(ext);
			var tmp_1: Array = new Array();
			tmp_1.push(ext);
			tmp_1.push(ext);
			testParams.push(tmp_1);
			// ArrayCollection
			var tmp11: ArrayCollection = new ArrayCollection();
			tmp11.addItem("one");
			tmp11.addItem(1);
			tmp11.addItem(null);
			testParams.push(tmp11);
			// ObjectProxy
			var temp12: ObjectProxy = new ObjectProxy({ a: "foo", b: 5 });
			testParams.push(temp12);
			var temp13: Array = new Array();
			temp13.push(temp12);
			temp13.push(temp12);
			testParams.push(temp13);
			// ByteArray
			// draw a red line in a BitmapData object
			var bmp:BitmapData = new BitmapData(80, 80, false, 0xCCCCCC);
			for (var g:uint = 0; g < 80; g++) {
			    var red:uint = 0xFF0000;
			    bmp.setPixel(g, 40, red);
			}
			// Create ByteArray with PNG data
			var temp14: ByteArray = PNGEnc.encode( bmp );
			temp14.compress();
			testParams.push(temp14);
			var tmp15: Array = new Array();
			tmp15.push(temp14);
			tmp15.push(temp14);
			testParams.push(tmp15);
			
  			// Create responder for result and error handlers
  			responder = new Responder( onRemotingResult, onRemotingError );
  			
			// Setup NetConnection
			nc = new NetConnection();
			nc.addEventListener( NetStatusEvent.NET_STATUS, netStatusHandler ); 
			nc.addEventListener( AsyncErrorEvent.ASYNC_ERROR, netASyncError );
            nc.addEventListener( SecurityErrorEvent.SECURITY_ERROR, netSecurityError );
            nc.addEventListener( IOErrorEvent.IO_ERROR, netIOError );
        }
		
		public function onConnect(protocol: String, encoding: uint): void {
			if (nc.connected) {
				nc.close();
			}
			nc.objectEncoding = encoding;
			var url: String;
			if (protocol == "http") {
			    // Remoting...
				url = http_txt.text;
			} else {
				// RTMP...
				url = rtmp_txt.text;
			}
			testResult = "Connecting through <b>" + protocol.toUpperCase() + "</b> using <b>AMF" + encoding;
			//
			if (protocol == "remoteObject") {
				echoService.endpoint = http_txt.text;
				if ( username_txt.text.length > 0 ) {
					echoService.setCredentials( username_txt.text, password_txt.text );
					printText( " (setCredentials)" );
				}
				printText( "</b>..." );
				onTest();
				return;
			}
			//
			echoService.endpoint = null;
			//
			nc.connect( url );
			if ( username_txt.text.length > 0 ) {
				nc.addHeader("Credentials", false, {userid: username_txt.text, password: password_txt.text});
				printText( " (setCredentials)" );
			}
			//
			printText( "</b>..." );
			
			if (protocol == "http") {
				// Don't wait for a successfull connection for remoting.
				onTest();
			}
		}
		
		private function printText( msg : String ) : void
		{
			testResult += msg;
		}
		
		private function netStatusHandler(event: NetStatusEvent): void {
			switch(event.info.code) {
				case "NetConnection.Connect.Success":
					printText( "<br>" + event.info.code + ": " + event.info.description );
					onTest();
					break;
				
				case "NetConnection.Connect.Rejected":
					printText( "<br>" + event.info.code + ": " + event.info.description );
					onDisconnect();
					break;
					
				case "NetConnection.Connect.Failed":
				case "NetConnection.Connect.Closed":
					printText( "<br>" + event.info.code );
					onDisconnect();
					break;
			}
		}
		
		private function netSecurityError( event : SecurityErrorEvent ) : void 
		{
			printText( "<br><b>" + failure + "Security error</font></b> - " + event.text );
		}
				
		private function netIOError( event : IOErrorEvent ) : void 
		{
			printText( "<br><b>" + failure + "IO error</font></b> - " + event.text + "<br>" );
		}
				
		private function netASyncError( event : AsyncErrorEvent ) : void 
		{
			printText( "<br><b>" + failure + "ASync error</font></b> - " + event.error + "<br>" );
		}
		
		private function doTest(): void {
			var testObj:* = testParams[testIndex];
			
			if (testObj is String && (testObj as String).length >= 100) {
				printText( "<br>Testing String with " + testObj.length + " chars: " );
			} else if (testObj is ByteArray) {
				printText( "<br>Testing ByteArray containing " + testObj.length + " bytes: " );
			} else if (testObj is XML) {
				printText( "<br>Testing XML " + testObj.toXMLString() + ": " );
			} else if (testObj is Array && testObj.length > 0 && testObj[0] is ByteArray) {
				printText( "<br>Tesing array of " + testObj.length + " ByteArrays: " );
			} else {
				printText( "<br>Testing " + testObj + ": " );
			}
			if (echoService.endpoint == null) {
				// NetConnection requests
				nc.call("echo", responder, testObj);
			} else {
				// RemotingObject requests
				echoService.echo( testObj );
			}
		}
		
		public function onRemotingResult( result : * ): void {
			checkResult(result);
			var testCount: Number = testParams.length;
			if (nc.objectEncoding == ObjectEncoding.AMF0) {
				testCount = AMF0Count;
			}
			printTestResults( testCount );
		}
		
		public function onRemotingError( result : * ): void {
			printText( "<br><b>" + failure + "AMF error received:</font></b>");
			if ( result is FaultEvent ){
				// HTTP error
				var fault:Fault = result.fault;
				printText( "<br>   <b>description</b>: " + fault.faultString);
				printText( "<br>   <b>code</b>: " + fault.faultCode);
				if ( fault.faultDetail.length > 0 ) {
					printText( "<br>   <b>details</b>: " + fault.faultDetail);
					for (var s:int=0;s<fault.faultDetail.length;s++) {
						var stackTrace:Object = fault.faultDetail[s];
						printText( "<br>             at " 
											 + stackTrace.className 
											 + "(" + stackTrace.fileName 
											 + ":" + stackTrace.lineNumber + ")");
					}
					printText( "<br>");
				}
			} else {
				// NetConnection error
				printText( "<br>   <b>level</b>: " + result.level);
				printText( "<br>   <b>code</b>: " + result.code);
				printText( "<br>   <b>description</b>: " + result.description);
				printText( "<br>   <b>application</b>: " + result.application);
			}
			//
			onDisconnect();
		}
		
		private function onTest(): void {
			testIndex = 0;
			testsFailed = 0;
			timer = getTimer();
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
			} else if (a is ByteArray && b is ByteArray) {
				return (a as ByteArray).toString() == (b as ByteArray).toString();
			} else if (a is ExternalizableClass && b is ExternalizableClass) {
				return (a.failed == b.failed && a.failed == 0);
			} else if (a is XML && b is XML) {
				return ((a as XML).toXMLString() == (b as XML).toXMLString());
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
		
		private function checkResult(result: Object): void {
		    if (extendedEqual(testParams[testIndex], result)) {
				if (result == null)
					printText( success + "OK</font> (null)" );
				else if (result is String && (result as String).length >= 1000)
					printText( success + "OK</font> (String with " + result.length + " chars)" );
				else if (result is ByteArray)
					printText( success + "OK</font> (ByteArray containing " + result.length + " bytes)" );
				else if (result is Array && result.length > 0 && result[0] is ByteArray)
					printText( success + "OK</font> (Array of " + result.length + " ByteArrays)" );
				else
					printText( success + "OK</font> (" + result.toString() + ")" );
			} else {
				if (result == null)
					printText( failure + "<b>FAILED</b></font> (null)" );
				else if (result is String && (result as String).length >= 1000)
					printText( failure + "<b>FAILED</b></font> (String with " + result.length + " chars)" );
				else if (result is ByteArray)
					printText( success + "<b>FAILED</b></font> (ByteArray containing " + result.length + " bytes)" );
				else if (result is Array && result.length > 0 && result[0] is ByteArray)
					printText( success + "<b>FAILED</b></font> (Array of " + result.length + " ByteArrays)" );
				else
					printText( failure + "<b>FAILED</b></font> (" + result.toString() + ")" );
				testsFailed++;
			}
			testIndex += 1;
		}
		
		private function printTestResults( testCount : Number ) : void
		{
			var testTime : Number = (getTimer() - timer)/1000;
			if (testIndex < testCount) {
				doTest();
			} else if (testsFailed == 0) {
				printText( "<br><b>Successfully ran " + success + testCount + "</font> test(s) in " + testTime + " seconds.</b>" );
				onDisconnect();
			} else {
				printText( "<br><b>Ran " + success + testCount + "</font> test(s) in " + testTime + " seconds, " + 
							failure + testsFailed + "</font> test(s) failed.</b>" );
				onDisconnect();
			}
		}
		
	}
}
