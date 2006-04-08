package org.red5.server.api;

/**
 * Resolve the scope given a host and path
 */
public interface IScopeResolver {

	public IGlobalScope getGlobalScope();
	public IScope resolveScope(String path);
	
}