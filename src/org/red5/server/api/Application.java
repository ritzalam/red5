package org.red5.server.api;

import java.util.List;

import org.red5.server.context.AppContext;

public interface Application {

	public String getName();
	public AppContext getContext();
	public List getScopes();
	
}