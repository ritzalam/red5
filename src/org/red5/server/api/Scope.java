package org.red5.server.api;

import java.util.List;

public interface Scope {

	public String getName();

	// Like session but just for an application
	public void setAttribute(String name, Object value);
	public Object getAttribute(String name);
	
	public List getClients();
	
	// These will be called internally ?? 
	//public void join(Client client);
	//public void leave(Client client);
	
	//public void sendToAll(Object object, boolean excludeSelf);
	
	
}