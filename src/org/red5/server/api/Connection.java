package org.red5.server.api;

import java.util.List;
import java.util.Set;

/**
 * The connection object
 * 
 * Each connection has an associated client and scope. Connections may be
 * persistent, polling, or transitent. The aim of this interface is to provide
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
	 * Close this connection, this will disconnect the client from the assicated
	 * scope
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
	public boolean switchScope(String contextPath); // attempt to enter a new
													// scope

	/**
	 * Get a list of the stream object associated with this connection
	 * 
	 * @return set of stream objects
	 */
	public Set getStreams(); // should this be split between onDemand and
								// broadcast ?

	/**
	 * Get the connection params
	 * 
	 * @return list of params the client sent when connecting
	 */
	public List getParams(); // read only hash list of connect parameters

}