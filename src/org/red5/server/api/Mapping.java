package org.red5.server.api;

/**
 * Mapping between context path and scope handler
 * Mapping between service name and service
 */
public interface Mapping {
	
	public ScopeHandler mapContextPathToScopeHandler(String contextPath);
	public Object mapServiceNameToService(String serviceName);
	
}