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
	
	import flash.events.EventDispatcher;
	import flash.events.TimerEvent;
	import flash.net.Responder;
	import flash.utils.ByteArray;
	import flash.utils.Timer;
	import flash.utils.getQualifiedClassName;
	import flash.utils.getTimer;
	
	import mx.rpc.Fault;
	import mx.rpc.events.FaultEvent;
	
	import org.red5.samples.echo.data.OutputObject;
	import org.red5.samples.echo.events.TestResultEvent;
	
	[Event(name="testInit", type="org.red5.samples.echo.events.TestResultEvent")]
	[Event(name="testActive", type="org.red5.samples.echo.events.TestResultEvent")]
	[Event(name="testComplete", type="org.red5.samples.echo.events.TestResultEvent")]
	[Event(name="testFailed", type="org.red5.samples.echo.events.TestResultEvent")]
	[Event(name="testError", type="org.red5.samples.echo.events.TestResultEvent")]
	[Event(name="testTimeOut", type="org.red5.samples.echo.events.TestResultEvent")]
	
	/**
	 * @author Joachim Bauch ( jojo@struktur.de )
	 * @author Thijs Triemstra ( info@collab.nl )
	*/	
	public class TestResult extends EventDispatcher
	{
		private var _id				: int;
		private var _input			: *;
		private var _result			: OutputObject;
		private var _responder		: Responder; 
		private var _testTimer		: int;
		private var _stopwatch		: Timer;
		private var _timeOut		: Number = 5;
		
		public function TestResult()
		{
			this._result = new OutputObject();
			this._responder = new Responder( onRemotingResult, onRemotingError );
			this._stopwatch = new Timer( 10 );
            this._stopwatch.addEventListener( TimerEvent.TIMER, stopwatchHandler );
		}
		
		public function get input(): *
		{
			return _input;
		}
		
		public function get responder(): Responder
		{
			return _responder;
		}
		
		public function get result(): OutputObject
		{
			return _result;
		}
		
		public function setupTest( id:int, input:* ) : void
		{
			this._id = id;
			this._input = input;
			this._testTimer = getTimer();
			
			_result.id = _id + 1;
			_result.status = TestResultEvent.TEST_INIT;
			_result.request = getObjectDescription( _input );
			
			dispatchEvent( new TestResultEvent( TestResultEvent.TEST_INIT, _result ));
			
            _stopwatch.start();
		}
		
		private function onRemotingResult( result : Object ) : void 
		{
			var res			: String;
			var testTime 	: Number = getTestTime( _testTimer );
			var validResult	: Boolean = extendedEqual( _input, result );
			
			if ( validResult ) {
				res = TestResultEvent.TEST_COMPLETE;
			} else {
				res = TestResultEvent.TEST_FAILED;
			}
			
			_result.status = res;
			_result.response = getObjectDescription( result );
			_result.speed = testTime;
			
			_stopwatch.stop();
			
			dispatchEvent( new TestResultEvent( res, _result ));
		}
		
		private function onRemotingError( result : * ) : void 
		{
			var msg : String;
			var testTime : Number = getTestTime( _testTimer );
			
			if ( result is FaultEvent ) 
			{
				// AMF error
				msg = "AMF error received";
				var fault:Fault = result.fault;
				msg += "<br>   <b>description</b>: " + fault.faultString;
				msg += "<br>   <b>code</b>: " + fault.faultCode;
				if ( fault.faultDetail.length > 0 ) 
				{
					msg += "<br>   <b>details</b>: " + fault.faultDetail;
					for ( var s:int=0;s<fault.faultDetail.length;s++ ) {
						try {
							var stackTrace:Object = fault.faultDetail[s];
							msg += "<br>             at " 
												 + stackTrace.className 
												 + "(" + stackTrace.fileName 
												 + ":" + stackTrace.lineNumber + ")";
						} catch ( e:ReferenceError ) {
							referenceError(e);
							break;
						}
					}
					msg += "<br>";
				}
			} 
			else 
			{
				// RTMP error
				msg = "RTMP error received";
				msg += "<br>   <b>level</b>: " + result.level;
				msg += "<br>   <b>code</b>: " + result.code;
				msg += "<br>   <b>description</b>: " + result.description;
				msg += "<br>   <b>application</b>: " + result.application;
			}
			
			_result.status = TestResultEvent.TEST_ERROR;
			_result.response = msg;
			_result.speed = testTime;
			
			_stopwatch.stop();
			
			dispatchEvent( new TestResultEvent( TestResultEvent.TEST_ERROR, _result ));
		}
		
		private function referenceError( e:ReferenceError ) : void
		{
			trace("Error: " + e.getStackTrace() + "<br/>");
		}
		
		private function stopwatchHandler( event:TimerEvent ) : void
		{
			var testTime : Number = getTestTime( _testTimer );
			var res		 : String;
			
			// server timeout
			if ( testTime >= _timeOut ) 
			{
				_stopwatch.stop();
				
				res = TestResultEvent.TEST_TIMEOUT;
			}
			else
			{
				res = TestResultEvent.TEST_ACTIVE;
			}
			
			_result.status = res;
			_result.speed = testTime;
			
			dispatchEvent( new TestResultEvent( res, _result ));
		}
		
		/**
		 * @param obj
		 * @return 
		 */		
		private function getObjectDescription( obj:* ) : String
		{
			var msg:String = getQualifiedClassName( obj );
			
			if ( obj is Array ) 
			{
				if ( obj.length > 0 ) 
				{
					var firstItem:* = obj[0];
					msg = obj.length + " " + getQualifiedClassName( firstItem ) + " item";
					if ( obj.length > 1 ) {
						msg += "s";
					}
				}
			}
			else if ( obj is String )
			{
				if ( obj.length < 25 && obj.length > 0 ) {
					msg += " (" + obj + ")";
				} else {
					msg += " with " + obj.length + " characters";
				}
			}
			else if ( obj is XML )
			{
				msg += " with " + obj.toXMLString().length + " characters";
			}
			else if ( obj is Boolean || obj is int || obj is Number || obj is Date )
			{
				msg += " (" + obj + ")";
			}
			
			return msg;
		}
		
		/**
		 * Compare two objects.
		 * 
		 * @param a
		 * @param b
		 * @return True or false.
		 */		
		private function extendedEqual( a: Object, b: Object ) : Boolean 
		{
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
					try {
						if (!extendedEqual((a as Array)[i], (b as Array)[i])) {
							return false;
						}
					} catch ( e:ReferenceError ) {
					    referenceError(e);
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
					try {
						if (!extendedEqual(a[key], (b as Array)[key])) {
							return false;
						}
					} catch (e:ReferenceError) {
					    referenceError(e);
					    return false;
					}
				}
				return true;
			} else if (!(a is Object) && b is Object) {
				for (key in b) {
					try {
						if (!extendedEqual((a as Array)[key], b[key])) {
							return false;
						}
					} catch (e:ReferenceError) {
					    referenceError(e);
					    return false;
					}
				}
				return true;
			} else if (a is Object && b is Object) {
				for (key in a) {
					try {
					    if (!extendedEqual(a[key], b[key])) {
							return false;
						}
					} catch (e:ReferenceError) {
					    referenceError(e);
					    return false;
					}
				}
				return true;
			} else {
				return (a == b);
			}
		}
		
		private function getTestTime( timer:Number ) : Number
		{
			return (getTimer() - timer)/1000;
		}
		
	}
}