package org.red5.server.net.remoting;

/*
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

import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.red5.compatibility.flex.messaging.messages.AbstractMessage;
import org.red5.compatibility.flex.messaging.messages.AcknowledgeMessage;
import org.red5.compatibility.flex.messaging.messages.AsyncMessage;
import org.red5.compatibility.flex.messaging.messages.CommandMessage;
import org.red5.compatibility.flex.messaging.messages.Constants;
import org.red5.compatibility.flex.messaging.messages.ErrorMessage;
import org.red5.compatibility.flex.messaging.messages.RemotingMessage;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceInvoker;
import org.red5.server.service.ConversionUtils;
import org.red5.server.service.PendingCall;

/**
 * Service that can execute compatibility flex messages.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class FlexMessagingService {

	/** Name of the service. */
	public static final String SERVICE_NAME = "flexMessaging";
	
    /**
     * Logger
     */
	protected static Log log = LogFactory.getLog(FlexMessagingService.class.getName());

	/** Service invoker to use. */
	protected IServiceInvoker serviceInvoker;
	
	/** Configured endpoints. */
	protected Map<String, Object> endpoints = Collections.EMPTY_MAP;
	
	/**
	 * Setup available end points.
	 * 
	 * @param endPoints
	 */
	public void setEndpoints(Map<String, Object> endpoints) {
		this.endpoints = endpoints;
		log.info("Configured endpoints: " + endpoints);
	}
	
	/**
	 * Set the service invoker to use.
	 * 
	 * @param serviceInvoker
	 */
	public void setServiceInvoker(IServiceInvoker serviceInvoker) {
		this.serviceInvoker = serviceInvoker;
	}
	
	/**
	 * Construct error message.
	 * 
	 * @param request
	 * @param faultCode
	 * @param faultString
	 * @param faultDetail
	 * @return
	 */
	public static ErrorMessage returnError(AbstractMessage request, String faultCode, String faultString, String faultDetail) {
		ErrorMessage result = new ErrorMessage();
		result.timestamp = System.currentTimeMillis();
		result.headers = request.headers;
		result.destination = request.destination;
		result.correlationId = request.messageId;
		result.faultCode = faultCode;
		result.faultString = faultString;
		result.faultDetail = faultDetail;
		return result;
	}
	
	/**
	 * Handle request coming from <code>mx:RemoteObject</code> tags.
	 * 
	 * @param msg
	 * @return
	 */
	public AsyncMessage handleRequest(RemotingMessage msg) {
		if (serviceInvoker == null) {
			log.error("No service invoker configured: " + msg);
			return returnError(msg, "Server.Invoke.Error", "No service invoker configured.", "No service invoker configured.");
		}
		
		Object endpoint = endpoints.get(msg.destination);
		if (endpoint == null) {
			log.error("Endpoint " + msg.destination + " doesn't exist (" + msg + ")");
			return returnError(msg, "Server.Invoke.Error", "Endpoint " + msg.destination + " doesn't exist.", "Endpoint " + msg.destination + " doesn't exist.");
		}
		
		Object[] args = (Object[]) ConversionUtils.convert(msg.body, Object[].class);
		IPendingServiceCall call = new PendingCall(msg.operation, args);
		try {
			if (!serviceInvoker.invoke(call, endpoint)) {
				return returnError(msg, "Server.Invoke.Error", "Can't invoke method.", "");
			}
		} catch (Throwable err) {
			log.error("Error while invoking method.", err);
			return returnError(msg, "Server.Invoke.Error", "Error while invoking method.", err.getMessage());
		}
		
		// We got a valid result from the method call.
		AcknowledgeMessage result = new AcknowledgeMessage();
		result.body = call.getResult();
		result.headers = msg.headers;
		result.clientId = msg.clientId;
		result.correlationId = msg.messageId;
		return result;
	}

	/**
	 * Handle command message request.
	 * 
	 * @param msg
	 * @return
	 */
	public AsyncMessage handleRequest(CommandMessage msg) {
		AsyncMessage result = null;
		switch (msg.operation) {
		case Constants.OPERATION_PING:
			// Send back pong message
			result = new AcknowledgeMessage();
			result.clientId = msg.clientId;
			result.correlationId = msg.messageId;
			break;
				
		default:
			log.error("Unknown CommandMessage request: " + msg);
			result = returnError(msg, "notImplemented", "Don't know how to handle " + msg, "Don't know how to handle " + msg);
		}
		return result;
	}

	/**
	 * Fallback method to handle arbitrary messages.
	 * 
	 * @param msg
	 * @return
	 */
	public ErrorMessage handleRequest(AbstractMessage msg) {
		log.error("Unknown CommandMessage request: " + msg);
		return returnError(msg, "notImplemented", "Don't know how to handle " + msg, "Don't know how to handle " + msg);
	}
	
}
