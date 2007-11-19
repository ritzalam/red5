package org.red5.samples.echo 
{
	import flash.events.EventDispatcher;
	import flash.net.Responder;
	import flash.utils.ByteArray;
	import flash.utils.getTimer;
	
	import mx.rpc.Fault;
	import mx.rpc.events.FaultEvent;
	
	import org.red5.samples.echo.data.OutputObject;
	import org.red5.samples.echo.events.TestResultEvent;
	
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
	
	/**
	 * @author Joachim Bauch ( jojo@struktur.de )
	 * @author Thijs Triemstra ( info@collab.nl )
	*/	
	public class TestResult extends EventDispatcher
	{
		private var _input			: *;
		private var _result			: OutputObject;
		private var _responder		: Responder; 
		private var _testTimer		: int;
		
		public const TEST_INIT 		: String = "TEST_INIT";
		public const TEST_COMPLETE 	: String = "TEST_COMPLETE";
		public const TEST_FAILED 	: String = "TEST_FAILED";
		public const TEST_ERROR	 	: String = "TEST_ERROR";
		
		public function TestResult ( input:* )
		{
			this._input = input;
			this._testTimer = getTimer();
			this._result = new OutputObject();
			this._responder = new Responder( onRemotingResult, onRemotingError );
			//
			startTest();
		}
		
		public function get input(): *
		{
			return _input;
		}
		
		public function set input( val:* ): void
		{
			_input = val;
		}
		
		public function get responder(): Responder
		{
			return _responder;
		}
		
		public function get result(): OutputObject
		{
			return _result;
		}
		
		private function startTest(): void
		{
			var msg:String;
			//
			if ( _input is String && (_input as String).length >= 100 ) 
			{
				msg = "Testing String with " + _input.length + " characters";
			} 
			else if ( _input is ByteArray ) 
			{
				msg = "Testing ByteArray containing " + _input.length + " bytes";
			} 
			else if ( input is XML ) 
			{
				msg = "Testing XML with " + _input.toXMLString().length + " characters";
			} 
			else if ( _input is Array && _input.length > 0 && _input[0] is ByteArray ) 
			{
				msg = "Testing Array of " + _input.length + " ByteArrays containing ";
				var totalSize:int = 0;
				for (var d:int=0;d<_input.length;d++)
				{
					totalSize += _input[d].length;
				}
				msg += totalSize + " bytes";
			} 
			else 
			{
				msg = "Testing " + _input;
			}
			//
			_result.code = TEST_INIT;
			_result.request = _input;
			_result.msg = msg;
			//
			dispatchEvent( new TestResultEvent( TEST_INIT, _result ));
		}
		
		private function onRemotingResult( result : * ): void 
		{
			var testTime 		: Number = getTestTime( _testTimer );
			var msg				: String;
			var validResult		: Boolean = extendedEqual( _input, result );
			//
			if ( result == null )
				msg = "(null) in " + testTime + " sec";
			else if ( result is String && ( (result as String).length >= 1000 || (result as String).length == 0 ))
				msg = "(String with " + result.length + " chars) in " + testTime + " sec";
			else if ( result is ByteArray )
				msg = "(ByteArray containing " + result.length + " bytes) in " + testTime + " sec";
			else if ( result is Array && result.length > 0 && result[0] is ByteArray )
				msg = "(Array of " + result.length + " ByteArrays) in " + testTime + " sec";
			else
				msg = "(" + result.toString() + ") in " + testTime + " sec";
			
			if ( validResult ) 
			{
				dispatchEvent( new TestResultEvent( TEST_COMPLETE, _result ));
			} 
			else 
			{
				//
				// testsFailed++;
				dispatchEvent( new TestResultEvent( TEST_FAILED, _result ));
			}
		}
		
		private function onRemotingError( result : * ) : void 
		{
			var testTime : Number = getTestTime( _testTimer );
			var msg : String = "AMF error received (after " + testTime + " sec)";
			
			if ( result is FaultEvent ) 
			{
				// AMF error
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
				msg += "<br>   <b>level</b>: " + result.level;
				msg += "<br>   <b>code</b>: " + result.code;
				msg += "<br>   <b>description</b>: " + result.description;
				msg += "<br>   <b>application</b>: " + result.application;
			}
			
			dispatchEvent( new TestResultEvent( TEST_ERROR, _result ));
		}
		
		private function referenceError( e:ReferenceError ): void
		{
			trace("Error: " + e.getStackTrace() + "<br/>");
		}
		
		private function extendedEqual( a: Object, b: Object ): Boolean 
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
		
		private function getTestTime( timer:int ): Number
		{
			return (getTimer() - timer)/1000;
		}
		
	}
}