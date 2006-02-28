package org.red5.server.api;

import java.util.List;

/**
 * @author luke
 * An application can have multiple scopes. 
 * The hold the state for an application instance
 * The application should generally be stateless
 * 
 * eg:
 * rtmp://server:port/app/scope
 * http://server:port/gateway/app/scope
 * 
 * If no scope is given, the default scope will be used
 * 
 */
public interface Scope {

	public String getName();

	// Like session but just for an application
	public void setAttribute(String name, Object value);
	public Object getAttribute(String name);
	
	public List getClients();

	// get a shared object by name
	public SharedObject getSharedObject(String name, boolean persistent);
	
	// These will be called internally ?? 
	//public void join(Client client);
	//public void leave(Client client);
	
	//public void sendToAll(Object object, boolean excludeSelf);
	
	
}