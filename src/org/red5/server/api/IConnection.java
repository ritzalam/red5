package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import java.util.Iterator;
import java.util.Map;

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
	 * Encoding types.
	 */
	public static enum Encoding {
		AMF0,
		AMF3
	};
	
	/**
	 * Persistent connection type, eg RTMP.
	 */
	public static final String PERSISTENT = "persistent";

	/**
	 * Polling connection type, eg RTMPT.
	 */
	public static final String POLLING = "polling";

	/**
	 * Transient connection type, eg Remoting, HTTP, etc.
	 */
	public static final String TRANSIENT = "transient";

	/**
	 * Get the connection type.
	 * 
	 * @return string containing one of connection types
	 */
	public String getType(); // PERSISTENT | POLLING | TRANSIENT

	/**
	 * Get the object encoding for this connection.
	 * 
	 * @return the used encoding.
	 */
	public Encoding getEncoding();
	
	/**
	 * Initialize the connection.
     * @param client
     */
	public void initialize(IClient client);

	/**
	 * Try to connect to the scope.
     * @return
     * @param scope
     */
	public boolean connect(IScope scope);

	/**
	 * Try to connect to the scope with a list of connection parameters.
     * @param params
     * @return
     * @param scope
     */
	public boolean connect(IScope scope, Object[] params);

	/**
	 * Is the client connected to the scope.
	 * 
	 * @return <code>true</code> if the connection is persistent or polling,
	 *         otherwise <code>false</code>
	 */
	public boolean isConnected();

	/**
	 * Close this connection.  This will disconnect the client from the
	 * associated scope.
	 */
	public void close();

	/**
	 * Return the parameters that were given in the call to "connect".
	 * 
	 * @return
	 */
	public Map<String, Object> getConnectParams();

	/**
	 * Get the client object associated with this connection.
	 * 
	 * @return client object
	 */
	public IClient getClient();

	/**
	 * Get the hostname that the client is connected to. If they are connected
	 * to an IP, the IP address will be returned as a String.
	 * 
	 * @return String containing the hostname
	 */
	public String getHost();

	/**
	 * Get the ip address the client is connected from.
	 * 
	 * @return the ip address of the client
	 */
	public String getRemoteAddress();

	/**
	 * Get the port the client is connected from.
	 * 
	 * @return the port of the client
	 */
	public int getRemotePort();

	/**
	 * Get the path for this connection.
	 * This is not updated if you switch scope.
	 * 
	 * @return path
	 */
	public String getPath();

	/**
	 * Get the session id, this may be <code>null</code>.
	 * 
	 * @return session id
	 */
	public String getSessionId();

	/**
	 * Total number of bytes read from the connection.
	 * 
	 * @return number of read bytes
	 */
	public long getReadBytes();

	/**
	 * Total number of bytes written to the connection.
	 * 
	 * @return number of written bytes
	 */
	public long getWrittenBytes();

	/**
	 * Total number of messages read from the connection.
	 * 
	 * @return number of read messages 
	 */
	public long getReadMessages();

	/**
	 * Total number of messages written to the connection.
	 * 
	 * @return number of written messages
	 */
	public long getWrittenMessages();

	/**
	 * Total number of messages that have been dropped.
	 * 
	 * @return number of dropped messages
	 */
	public long getDroppedMessages();

	/**
	 * Total number of messages that are pending to be sent to the connection.
	 * 
	 * @return number of pending messages
	 */
	public long getPendingMessages();

	/**
	 * Start measuring the roundtrip time for a packet on the connection.
	 */
	public void ping();

	/**
	 * Return roundtrip time of last ping command.
	 * 
	 * @return roundtrip time in milliseconds
	 */
	public int getLastPingTime();

	/**
	 * Get the scope this is connected to.
	 * 
	 * @return the connected scope
	 */
	public IScope getScope();

	/**
	 * Get the basic scopes this connection has subscribed.  This list will
	 * contain the shared objects and broadcast streams the connection
	 * connected to.
	 * 
	 * @return list of basic scopes
	 */
	public Iterator<IBasicScope> getBasicScopes();

}
