package org.red5.server.api;

public interface Connection {

	public static final String PERSISTENT = "persistent";
	public static final String POLLING = "polling";
	public static final String TRANSIENT = "transient";
	
	public Client getClient();
	public Scope getScope();
	public Application getApplication();
	public boolean isConnected();
	public void send(Object object);
	public String getParameter(String name);
	public void close();
	public String getType();
	
}
