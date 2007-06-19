package org.red5.compatibility.flex.messaging.messages;

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

import java.util.Map;

/**
 * Base class for all flex compatibility messages.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class AbstractMessage {

	public long timestamp;
	
	public Map headers;
	
	public Object body;
	
	public String messageRefType;
	
	public String messageId;
	
	public long timeToLive;
	
	public Object clientId;
	
	public String destination;
	
	/**
	 * Add message properties to string.
	 * 
	 * @param result <code>StringBuilder</code> to add properties to
	 */
	protected void addParameters(StringBuilder result) {
		result.append("ts="+timestamp+",");
		result.append("headers="+headers+",");
		result.append("body="+body+",");
		result.append("messageRefType="+messageRefType+",");
		result.append("messageId="+messageId+",");
		result.append("timeToLive="+timeToLive+",");
		result.append("clientId="+clientId+",");
		result.append("destination="+destination);
	}
	
	/** {@inheritDoc} */
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getName());
		result.append("(");
		addParameters(result);
		result.append(")");
		return result.toString();
	}
	
}
