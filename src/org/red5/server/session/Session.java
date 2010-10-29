package org.red5.server.session;

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
 
import org.red5.server.api.session.ISession;

/**
 * Represents the most basic type of "Session", loosely modeled after the HTTP Session used
 * in J2EE applications.
 *
 *
 * @author The Red5 Project (red5@osflash.org) 
 * @author Paul Gregoire (mondain@gmail.com)   
 */ 
public class Session implements ISession {

	private static final long serialVersionUID = 2893666721L;
	
	//time at which this session instance was created
	protected long created;
	
	//whether or not this session is in an active state
	protected boolean active;
	
	//unique identifier for this session
	protected String sessionId;
	
	//location where resources may be stored for this instance
	protected String destinationDirectory;
	
	//flash client identifier
	protected String clientId;
	
	{
		//set current time as created time
		created = System.currentTimeMillis();
		//set as active
		active = true;
	}	
	
	public Session() {
	}
	
	public Session(String sessionId) {
		this.sessionId = sessionId;
	}

	public long getCreated() {
		return created;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void reset() {
	    clientId = null;
	}
	
	public boolean isActive() {
	    return active;	
	}

	public void end() {
	    active = false;
	}
	
	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public void setDestinationDirectory(String destinationDirectory) {
		this.destinationDirectory = destinationDirectory;
	}

	public String getDestinationDirectory() {
		return destinationDirectory;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((sessionId == null) ? 0 : sessionId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Session other = (Session) obj;
		if (sessionId == null) {
			if (other.sessionId != null)
				return false;
		} else if (!sessionId.equals(other.sessionId))
			return false;
		return true;
	}
	
}
