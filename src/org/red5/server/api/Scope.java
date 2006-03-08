package org.red5.server.api;

import java.io.IOException;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

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
 */
 public interface Scope extends AttributeStore {

	public boolean hasParent();
	public Scope getParent();
	
	public String getContextPath(); 
	public ApplicationContext getContext();
	public Resource getResource(String name);
	public Resource[] getResources(String pattern) throws IOException;

	public boolean hasChildScope(String name);
	public Set getChildScopeNames();
	public Scope getChildScope(String name);
	
	public Set getSharedObjectNames();
	public boolean createSharedObject(String name, boolean peristent);
	public SharedObject getSharedObject(String name);
		
	public Set getClients();
	public void dispatchEvent(Object event);
		
	public ScopeHandler getHandler();
	
	public boolean hasBroadcastStream(String name);
	public BroadcastStream getBroadcastStream(String name);
	public Set getBroadcastStreamNames();
	
}