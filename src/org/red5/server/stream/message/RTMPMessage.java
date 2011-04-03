package org.red5.server.stream.message;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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

import org.red5.server.messaging.AbstractMessage;
import org.red5.server.net.rtmp.event.IRTMPEvent;

/**
 * RTMP message
 */
public class RTMPMessage extends AbstractMessage {
	
	private final IRTMPEvent body;

	/**
	 * Creates a new rtmp message.
	 * 
	 * @param body value to set for property 'body'
	 */
	private RTMPMessage(IRTMPEvent body) {
		this.body = body;
	}
	
	/**
	 * Creates a new rtmp message.
	 * 
	 * @param body value to set for property 'body'
	 * @param eventTime updated timestamp
	 */
	private RTMPMessage(IRTMPEvent body, int eventTime) {
		this.body = body;
		this.body.setTimestamp(eventTime);
	}
	
	/**
	 * Return RTMP message body
	 *
	 * @return Value for property 'body'.
	 */
	public IRTMPEvent getBody() {
		return body;
	}

	/**
	 * Builder for RTMPMessage.
	 * 
	 * @param body event data
	 * @return Immutable RTMPMessage
	 */
	public final static RTMPMessage build(IRTMPEvent body) {
		return new RTMPMessage(body);
	}
	
	/**
	 * Builder for RTMPMessage.
	 * 
	 * @param body event data
	 * @param eventTime time value to set on the event body
	 * @return Immutable RTMPMessage
	 */
	public final static RTMPMessage build(IRTMPEvent body, int eventTime) {
		return new RTMPMessage(body, eventTime);
	}
	
}
