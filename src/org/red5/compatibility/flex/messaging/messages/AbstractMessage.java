package org.red5.compatibility.flex.messaging.messages;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.red5.io.utils.RandomGUID;

/**
 * Base class for all Flex compatibility messages.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
@SuppressWarnings("unchecked")
public class AbstractMessage implements Message, Serializable {

	private static final long serialVersionUID = -2297508940724279014L;

	public long timestamp;
	
	public Map<String, Object> headers = Collections.EMPTY_MAP;
	
	public Object body;
	
	public String messageId;
	
	public long timeToLive;
	
	public String clientId;
	
	public String destination;
	
	/**
	 * Initialize default message fields.
	 */
	public AbstractMessage() {
		timestamp = System.currentTimeMillis();
		messageId = new RandomGUID().toString();
	}
	
	/**
	 * Add message properties to string.
	 * 
	 * @param result <code>StringBuilder</code> to add properties to
	 */
	protected void addParameters(StringBuilder result) {
		result.append("ts=");
		result.append(timestamp);
		result.append(",headers=");
		result.append(headers);
		result.append(",body=");
		result.append(body);
		result.append(",messageId=");
		result.append(messageId);
		result.append(",timeToLive=");
		result.append(timeToLive);
		result.append(",clientId=");
		result.append(clientId);
		result.append(",destination=");
		result.append(destination);
	}

	@Override
	public Object getBody() {
		return body;
	}

	@Override
	public String getClientId() {
		return clientId;
	}

	@Override
	public String getDestination() {
		return destination;
	}

	@Override
	public Object getHeader(String name) {
		return headers.get(name);
	}

	@Override
	public Map<String, Object> getHeaders() {
		return headers;
	}

	@Override
	public String getMessageId() {
		return messageId;
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public long getTimeToLive() {
		return timeToLive;
	}

	@Override
	public boolean headerExists(String name) {
		return headers.containsKey(name);
	}

	@Override
	public void setBody(Object value) {
		body = value;
	}

	@Override
	public void setClientId(String value) {
		clientId = value;
	}

	@Override
	public void setDestination(String value) {
		destination = value;
	}

	@Override
	public void setHeader(String name, Object value) {
		headers.put(name, value);
	}

	@Override
	public void setHeaders(Map<String, Object> value) {
		if (!headers.isEmpty()) {
			headers.clear();
		}
		headers.putAll(value);
	}

	@Override
	public void setMessageId(String value) {
		messageId = value;
	}

	@Override
	public void setTimestamp(long value) {
		timestamp = value;
	}

	@Override
	public void setTimeToLive(long value) {
		timeToLive  = value;
	}

	/**
	 * Return string representation of the message.
	 * 
	 * @return value
	 */
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getName());
		result.append("(");
		addParameters(result);
		result.append(")");
		return result.toString();
	}
	
}
