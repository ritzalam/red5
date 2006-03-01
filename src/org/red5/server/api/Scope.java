package org.red5.server.api;

import java.util.List;

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
 * 
 */
public interface Scope {

	public String getName();
	public boolean hasParent();
	public Scope getParent();
	
	public String getContextPath(); 
	public ApplicationContext getContext();
	public Resource getResource(String name);
	public Resource[] getResources(String pattern);

	public List getChildScopeNames();
	public Scope getChildScope(String name);
	
	public List getSharedObjectNames();
	public SharedObject getSharedObject(String name, boolean persistent);
		
	public List getClients();
	public void dispatchEvent(Object event);
	
	public boolean hasAttribute(String name);
	public void setAttribute(String name, Object value);
	public Object getAttribute(String name);
		
}