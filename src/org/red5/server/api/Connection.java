package org.red5.server.api;

import java.util.List;
import java.util.Set;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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
 * The connection object
 * 
 * Each connection has an associated client and scope. Connections may be
 * persistent, polling, or transient. The aim of this interface is to provide
 * basic connection methods shared between different types of connections
 * 
 * Future subclasses: RTMPConnection, RemotingConnection, AJAXConnection,
 * HttpConnection, etc
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface Connection extends AttributeStore {

	/**
	 * Persistent connection type, eg RTMP
	 */
	public static final String PERSISTENT = "persistent";

	/**
	 * Polling connection type, eg RTMPT
	 */
	public static final String POLLING = "polling";

	/**
	 * Transient connection type, eg Remoting, HTTP, etc
	 */
	public static final String TRANSIENT = "transient";

	/**
	 * Get the connection type
	 * 
	 * @return string containing one of connection types
	 */
	public String getType(); // PERSISTENT | POLLING | TRANSIENT

	/**
	 * Is the client connected to the scope
	 * 
	 * @return true if the connection is persistent or polling, otherwise false
	 */
	public boolean isConnected();

	/**
	 * Close this connection, this will disconnect the client from the
	 * associated scope
	 */
	public void close();

	/**
	 * Get the client object associated with this connection
	 * 
	 * @return client object
	 */
	public Client getClient();

	/**
	 * Get the scope object assicated with this connection
	 * 
	 * @return scope object
	 */
	public Scope getScope();

	/**
	 * Dispatch an event down this connection You should call isConnected first
	 * to check its possible
	 * 
	 * @param object
	 *            any kind of simple object
	 */
	public void dispatchEvent(Object object);

	/**
	 * Attempt to enter a new scope
	 * 
	 * @param contextPath
	 *            the desired context path, can be relative or absolute
	 * @return true of the switch succeeded
	 */
	public boolean switchScope(String contextPath);

	/**
	 * Get a list of the stream object associated with this connection
	 * 
	 * @return set of stream objects
	 */
	public Set getStreams();

	/**
	 * Get the connection params
	 * 
	 * @return readonly list of connect params
	 */
	public List getParams();

}