package org.red5.server.api;

import java.util.Map;

/**
 * Subclasses: RTMPConnection, RemotingConnection, AJAXConnection, HttpConnection, etc
 */
public interface Connection {

	public static final String PERSISTENT = "persistent";
	public static final String POLLING = "polling";
	public static final String TRANSIENT = "transient";
	
	public String getType(); // PERSISTENT | POLLING | TRANSIENT
	
	// Is this connection connected, for transitent it will return false unless we are in an active rpc connection.
	// For persistent or polling this will return true
	public boolean isConnected();
	public void close();
	
	public Client getClient();
	public Scope getScope();
	
	// Send an object down to the client, this is like an event dispatcher, not like rpc
	public void dispatchEvent(Object object);
	
	public Map getParameters(); // read only hash map of parameters
	public String[] getParameterNames(); // return all the parameter names
	public String getParameter(String name); // parameters which came with the initial connect
	
}