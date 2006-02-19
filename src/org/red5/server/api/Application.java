package org.red5.server.api;

import java.util.List;

import org.red5.server.context.AppContext;

public interface Application {

	public String getName(); // The name of the application
	public AppContext getContext(); // The spring context
	public List getScopeNames(); // A list of the scopes 
	public SharedObject getSharedObject(String name, boolean persistent); // get a shared object by name
	
	// Should we have attributes ? Or read only properties ?
	
}