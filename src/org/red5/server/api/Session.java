package org.red5.server.api;


public interface Session {

	
	public void setAttribute(String name, Object value);
	public Object getAttribute(String name);
	
	// This should be as close to the servlet API as possible
	
}
