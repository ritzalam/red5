package org.red5.server.api;

import java.util.Iterator;

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
 * The connection object.
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
public interface IConnection extends ICoreObject {

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
	 * Initialize the connection
	 */
	public void initialize(IClient client);

	/**
	 * Try to connect to the scope
	 */
	public boolean connect(IScope scope);

	/**
	 * Try to connect to the scope with a list of connection parameters.
	 */
	public boolean connect(IScope scope, Object[] params);

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
	public IClient getClient();

	/**
	 * Get the hostname that the client is connected to. If they connected to an
	 * IP, the IP address will be returned as a String.
	 * 
	 * @return String containing the hostname
	 */
	public String getHost();
	
	/**
	 * Get the path for this connection
	 * This is not updated if you switch scope
	 * 
	 * @return path
	 */
	public String getPath();
	
	/**
	 * Get the session id, this may be null
	 * 
	 * @return session id
	 */
	public String getSessionId();

	public IScope getScope();
	
	public Iterator<IBasicScope> getBasicScopes();
	
}