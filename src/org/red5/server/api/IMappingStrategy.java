package org.red5.server.api;

/**
 * This interface encapsulates the mapping strategy used by the context. 
 */
public interface IMappingStrategy {

	public String mapServiceName(String name);
	public String mapScopeHandlerName(String contextPath);
	public String mapResourcePrefix(String contextPath);
	
}