package org.red5.server.api;

public interface Mapping {
	
	public ScopeHandler mapContextPathToScopeHandler(String contextPath);
	public Object mapServiceNameToService(String serviceName);
	
}