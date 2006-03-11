package org.red5.server.api;

/**
 * Use to Map between names and objects
 * Provides mapping for:
 * context path to scope handler
 * service name to service object
 */
public interface Mapping {

	/**
	 * Maps between a contextPath and a scope handler object
	 * 
	 * @param contextPath	the name of the context path eg: /myapp/room
	 * @return scope handler object, or null if no handler was found
	 */
	public ScopeHandler mapContextPathToScopeHandler(String contextPath);

	/**
	 * Maps between a service name and a service object
	 * 
	 * @param serviceName	the name of the service
	 * @return	the service object, or null if no service was found
	 */
	public Object mapServiceNameToService(String serviceName);

}