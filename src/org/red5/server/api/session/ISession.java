package org.red5.server.api.session;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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

/**
 * Represents the most basic type of "Session", loosely modeled after the HTTP Session used
 * in J2EE applications.
 *
 *
 * @author The Red5 Project (red5@osflash.org) 
 * @author Paul Gregoire (mondain@gmail.com)   
 */ 
public interface ISession extends Serializable {

	/**
	 * Returns creation time in milliseconds.
	 * 
	 * @return creation time
	 */
	public long getCreated();

	/**
	 * Returns the session's identifier.
	 * 
	 * @return session id
	 */
	public String getSessionId();
	
	/**
	 * Resets a specified set of internal parameters.
	 */
	public void reset();
	
	/**
	 * Returns the active state of the session.
	 * @return is active
	 */
	public boolean isActive();
	
	/**
	 * Ends the session, no further modifications should be allowed.
	 */
	public void end();
	
	/**
	 * Sets the associated client id.
	 * 
	 * @param clientId client id
	 */
	public void setClientId(String clientId);

	/**
	 * Returns the client id associated with this session.
	 * 
	 * @return client id
	 */
	public String getClientId();
	
	/**
	 * Sets where session resources will be located if persisted to disk.
	 * 
	 * @param destinationDirectory destination directory
	 */
	public void setDestinationDirectory(String destinationDirectory);

    /**
     * Returns the directory used to store session resources.
     *
     * @return destination directory
     */	
	public String getDestinationDirectory();
	
}
