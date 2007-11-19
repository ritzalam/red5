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
	
	import flash.events.AsyncErrorEvent;
	import flash.events.IOErrorEvent;
	import flash.events.NetStatusEvent;
	import flash.events.SecurityErrorEvent;
	import flash.net.NetConnection;
	import flash.net.ObjectEncoding;
	import flash.net.SharedObject;
	import flash.system.Capabilities;
	import flash.utils.getTimer;
	
	import mx.collections.ArrayCollection;
	import mx.controls.*;
	import mx.core.Application;
	import mx.events.FlexEvent;
	import mx.rpc.remoting.RemoteObject;
	
	import org.red5.samples.echo.data.EchoTestData;
	import org.red5.samples.echo.events.TestResultEvent;
	
	/**
	 * Echo test application for AMF and RTMP server implementations.
	 * 
	 * Contains tests for ActionScript 1.0 and 2.0 data types (AMF0),
	 * and ActionScript 3.0 data types (AMF3). Tests are transported
	 * using the NetConnection class over the AMF or RTMP protocol.
	 * 
	 * @author Joachim Bauch ( jojo@struktur.de )
	 * @author Thijs Triemstra ( info@collab.nl )
	*/	
	public class EchoTest extends Application
	{
		[Bindable]
		public var testResults			: ArrayCollection;
		
		[Bindable]
		private var testParams			: Array;
		
		[Bindable]
		private var testIndex			: Number;
		
		[Bindable]
		private var AMF0Count			: Number;
		
		[Bindable]
		private var testsFailed			: Number;
		
		[Bindable]
		public var fpVersion 			: String;
		
		[Bindable]
		public var testResult 			: String;
		
		[Bindable]
		public var rtmp_txt 			: TextInput;
		
		[Bindable]
		public var http_txt 			: TextInput;
		
		[Bindable]
		public var username_txt 		: TextInput;
		
		[Bindable]
		public var password_txt 		: TextInput;
		
		public var null_test			: CheckBox;
		public var undefined_test		: CheckBox;
		public var boolean_test			: CheckBox;
		public var string_test			: CheckBox;
		public var number_test			: CheckBox;
		public var array_test			: CheckBox;
		public var object_test			: CheckBox;
		public var date_test			: CheckBox;
		public var remote_test			: CheckBox;
		public var custom_test			: CheckBox;
		public var xml0_test			: CheckBox;
		public var xml3_test			: CheckBox;
		public var externalizable_test	: CheckBox;
		public var arraycollection_test	: CheckBox;
		public var objectproxy_test		: CheckBox;
		public var bytearray_test		: CheckBox;
		
		public var amf0_tests			: Array = new Array();
		public var amf3_tests			: Array = new Array();
		public var amf_tests 			: Array = new Array();
		private var success 			: String = "<font color='#149D25'>";
		private var failure 			: String = "<font color='#FF1300'>";
		private var globalTimer			: int;
		private var testTimer			: int;
		private var mySo				: SharedObject;
		private var testData			: EchoTestData;
		private var nc					: NetConnection;
		private var echoService 		: RemoteObject;
    
		/**
	 	 * Create and send test data.
		 */	
        public function EchoTest() : void
        {
        	this.addEventListener( FlexEvent.CREATION_COMPLETE, setupTest );
        }
		
		/**
		 * 
		 * @param event
		 */		
		private function setupTest( event:FlexEvent ) : void
		{
			amf0_tests = [ null_test, undefined_test, boolean_test, string_test, number_test, array_test,
					   object_test, date_test, xml0_test, remote_test, custom_test ];
			
			amf3_tests = [ xml3_test, externalizable_test, arraycollection_test, objectproxy_test, bytearray_test ];
			
			amf_tests = [ amf0_tests, amf3_tests ];
			
			// Display FP version nr.
        	fpVersion = "Flash Player " + Capabilities.version + " - " + Capabilities.playerType;
        	
        	// Load http and rtmp uri's from shared object
        	mySo = SharedObject.getLocal("EchoTest");
        	
        	// load url's from flookie
        	if ( mySo.data.rtmpUri != null ) {
        		rtmp_txt.text = mySo.data.rtmpUri;
        	} else {
        		rtmp_txt.text = "rtmp://localhost/echo";
        	}
        	if ( mySo.data.httpUri != null) {
        		http_txt.text = mySo.data.httpUri;
        	} else {
        		http_txt.text = "http://localhost:5080/echo/gateway";
        	}
			
			// Setup a single NetConnection for every test suite.
			nc = new NetConnection();
			nc.addEventListener( AsyncErrorEvent.ASYNC_ERROR, netASyncError );
            nc.addEventListener( SecurityErrorEvent.SECURITY_ERROR, netSecurityError );
            nc.addEventListener( IOErrorEvent.IO_ERROR, netIOError );
		}
		
		public function onConnect( protocol: String, encoding: uint ) : void 
		{
			if ( nc.connected ) 
			{
				nc.removeEventListener( NetStatusEvent.NET_STATUS, netStatusHandler );
				nc.close();
			}
			nc.objectEncoding = encoding;
			var url: String;
			if ( protocol == "http" ) {
			    // Remoting...
				url = http_txt.text;
				mySo.data.httpUri = url;
			} 
			else 
			{
				// RTMP...
				url = rtmp_txt.text;
				mySo.data.rtmpUri = url;
			}
            
            testResult = "Connecting through <b>" + protocol.toUpperCase() + "</b> using <b>AMF" + encoding  + "</b> encoding...<br/>";
            
            var flushStatus:String = null;
            try {
                flushStatus = mySo.flush();
            } 
            catch (error:Error) 
            {
            	printText( "<b>" + failure + "SharedObject error: </font></b>" + error.getStackTrace() + "<br/>" );
            }
            
			//
			if ( protocol == "remoteObject" ) {
				// Setup a RemoteObject
            	echoService = new RemoteObject( "Red5Echo" );
            	echoService.source = "EchoService";
            	// echoService.addEventListener( ResultEvent.RESULT, onRem );
            	
				if ( username_txt.text.length > 0 ) {
					// test credentials feature
					echoService.setCredentials( username_txt.text, password_txt.text );
					testResult += " ( using setCredentials )";
				}
				testResult += "...";
				startTests();
				// ignore rest of setup logic
				return;
			}
			
			if ( username_txt.text.length > 0 ) {
				// test credentials feature
				nc.addHeader("Credentials", false, {userid: username_txt.text, password: password_txt.text});
				testResult += " ( using setCredentials )";
			}
			//
			testResult += "..." ;
			if ( echoService != null )
			{
				// http_txt.text
				echoService.destination = null;
			}
			// (re)start connection
			nc.addEventListener( NetStatusEvent.NET_STATUS, netStatusHandler ); 
			nc.connect( url );
			//			
			if ( protocol == "http" ) {
				// Don't wait for a successfull connection for AMF0/AMF3 remoting tests.
				startTests();
			}
		}
		
		private function printText( msg : String ) : void
		{
			testResult += msg;
		}
		
		private function printResult( result : TestResult ) : void
		{
			testResults.addItem( result );
		}
		
		private function startTests(): void 
		{
			testParams = new EchoTestData( amf_tests ).items;
			testIndex = 0;
			testsFailed = 0;
			globalTimer = getTimer();
			if ( testParams.length > 0 ) {
				doTest();
			}
		}
		
		private function doTest(): void 
		{
			var testObj:TestResult = new TestResult( testParams[ testIndex ] );
			testObj.addEventListener( testObj.TEST_COMPLETE, onTestComplete );
			testObj.addEventListener( testObj.TEST_FAILED, onTestFailed );
			
			// Call method in remote service
			if ( echoService == null || echoService.destination == null ) 
			{
				// NetConnection requests
				nc.call( "echo", testObj.responder, testObj.input );
			} 
			else 
			{
				// RemotingObject requests
				echoService.echo( testObj.input );
			}
		}
		
		private function onTestComplete( event:TestResultEvent ): void 
		{
			testIndex += 1;
			var testCount: Number = testParams.length;
			if ( nc.objectEncoding == ObjectEncoding.AMF0 ) 
			{
				testCount = AMF0Count;
			}
			printTestResults( testCount );
		}
		
		private function onTestFailed( event:TestResultEvent ): void 
		{
			testIndex += 1;
			//
			onDisconnect();
		}
		
		private function printTestResults( testCount : Number ) : void
		{
			var testTime : Number = (getTimer() - globalTimer)/1000;
			if (testIndex < testCount) {
				doTest();
			} else if ( testsFailed == 0 ) {
				printText( "<br><b>Successfully ran " + success + testCount + "</font> test(s) in " + testTime + " seconds.</b>" );
				onDisconnect();
			} else {
				printText( "<br><b>Ran " + success + testCount + "</font> test(s) in " + testTime + " seconds, " + 
							failure + testsFailed + "</font> test(s) failed.</b>" );
				onDisconnect();
			}
		}
		
		private function netStatusHandler( event: NetStatusEvent ) : void 
		{
			switch( event.info.code ) 
			{
				case "NetConnection.Connect.Success":
					printText( event.info.code );
					startTests();
					break;
				
				case "NetConnection.Connect.Rejected":
					printText( event.info.code );
					onDisconnect();
					break;
					
				case "NetConnection.Connect.Failed":
				case "NetConnection.Connect.Closed":
					printText( event.info.code );
					onDisconnect();
					break;
			}
		}
		
		private function netSecurityError( event : SecurityErrorEvent ) : void 
		{
			printText( "<b>" + failure + "Security error</font></b>: " + event.text );
		}
				
		private function netIOError( event : IOErrorEvent ) : void 
		{
			printText( "<b>" + failure + "IO error</font></b>: " + event.text );
		}
				
		private function netASyncError( event : AsyncErrorEvent ) : void 
		{
			printText( "<b>" + failure + "ASync error</font></b>: " + event.error.getStackTrace() );
		}
		
		private function onDisconnect() : void 
		{
			nc.close();
		}
		
	}
}
