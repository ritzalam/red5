package org.red5.server.api;

/**
 * Subclasses: RTMPConnection, RemotingConnection, AJAXConnection, HttpConnection, etc
 */
public interface Connection {

	public static final String PERSISTENT = "persistent";
	public static final String POLLING = "polling";
	public static final String TRANSIENT = "transient";
	
	public Client getClient();
	public Scope getScope();
	public Application getApplication();
	
	// Is this connection connected, for transitent it will return false unless we are in an active rpc connection.
	// For persistent or polling this will return true
	public boolean isConnected();
	
	// Send an object down to the client, this is like an event dispatcher, not like rpc
	public void write(Object object);
	
	public String getParameter(String name); // parameters which came with the initial connect
	
	public String getType(); // PERSISTENT | POLLING | TRANSIENT
	
	public void close();
	
}
