/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.jmx.mxbeans;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import javax.management.MXBean;

import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeHandler;
import org.springframework.core.io.Resource;

/**
 * An MBean interface for the scope object.
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@MXBean
public interface ScopeMXBean {

	/**
	 * Check if scope is enabled
	 * @return                  <code>true</code> if scope is enabled, <code>false</code> otherwise
	 */
	public boolean getEnabled();

	/**
	 * Enable or disable scope by setting enable flag
	 * @param enabled            Enable flag value
	 */
	public void setEnabled(boolean enabled);

	/**
	 * Check if scope is in running state
	 * @return                   <code>true</code> if scope is in running state, <code>false</code> otherwise
	 */
	public boolean getRunning();

	/**
	 * Setter for autostart flag
	 * @param autoStart         Autostart flag value
	 */
	public void setAutoStart(boolean autoStart);

	/**
	 * Initialization actions, start if autostart is set to <code>true</code>
	 */
	public void init();

	/**
	 * Starts scope
	 * @return     <code>true</code> if scope has handler and it's start method returned true, <code>false</code> otherwise
	 */
	public boolean start();

	/**
	 * Stops scope
	 */
	public void stop();

	/**
	 * Destroys scope
	 */
	public void destroy();

	/**
	 * Set scope persistence class
	 *
	 * @param persistenceClass       Scope's persistence class
	 * @throws Exception             Exception
	 */
	public void setPersistenceClass(String persistenceClass) throws Exception;

	/**
	 * Setter for child load path. Should be implemented in subclasses?
	 * @param pattern            Load path pattern
	 */
	public void setChildLoadPath(String pattern);

	/**
	 * Check whether scope has child scope with given name
	 * @param name               Child scope name
	 * @return                   <code>true</code> if scope has child node with given name, <code>false</code> otherwise
	 */
	public boolean hasChildScope(String name);

	/**
	 * Check whether scope has child scope with given name and type
	 * @param type               Child scope type
	 * @param name               Child scope name
	 * @return                   <code>true</code> if scope has child node with given name and type, <code>false</code> otherwise
	 */
	public boolean hasChildScope(String type, String name);

	/**
	 * Return child scope names iterator
	 * @return                   Child scope names iterator
	 */
	public Iterator<String> getScopeNames();

	/**
	 * Return set of clients
	 * @return                   Set of clients bound to scope
	 */
	public Set<IClient> getClients();

	/**
	 * Check if scope has a context
	 * @return                   <code>true</code> if scope has context, <code>false</code> otherwise
	 */
	public boolean hasContext();

	/**
	 * Return scope context. If scope doesn't have context, parent's context is returns, and so forth.
	 * @return                   Scope context or parent context
	 */
	public IContext getContext();

	/**
	 * Return scope context path
	 * @return                   Scope context path
	 */
	public String getContextPath();

	/**
	 * Setter for scope name
	 * @param name               Scope name
	 */
	public void setName(String name);

	/**
	 * Return scope path calculated from parent path and parent scope name
	 * @return                   Scope path
	 */
	public String getPath();

	/**
	 * Check if scope or it's parent has handler
	 * @return                     <code>true</code> if scope or it's parent scope has a handler, <code>false</code> otherwise
	 */
	public boolean hasHandler();

	/**
	 * Return scope handler or parent's scope handler if this scope doesn't have one
	 * @return                     Scope handler (or parent's one)
	 */
	public IScopeHandler getHandler();

	/**
	 * Return parent scope
	 * @return                      Parent scope
	 */
	public IScope getParent();

	/**
	 * Check if scope has parent scope
	 * @return                      <code>true</code> if scope has parent scope, <code>false</code> otherwise`
	 */
	public boolean hasParent();

	/**
	 * Set scope depth
	 * @param depth         Scope depth
	 */
	public void setDepth(int depth);

	/**
	 * return scope depth
	 * @return              Scope depth
	 */
	public int getDepth();

	/**
	 * Return array of resources from path string, usually used with pattern path
	 * @param path           Resources path
	 * @return               Resources
	 * @throws IOException   I/O exception
	 */
	public Resource[] getResources(String path) throws IOException;

	/**
	 * Return resource located at given path
	 * @param path           Resource path
	 * @return               Resource
	 */
	public Resource getResource(String path);

	/**
	 * Return connection iterator
	 * @return                Connections iterator
	 */
	public Collection<Set<IConnection>> getConnections();

	/**
	 * Create child scope with given name
	 * @param name           Child scope name
	 * @return               <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean createChildScope(String name);

	/**
	 * Return base scope of given type with given name
	 * @param type           Scope type
	 * @param name           Scope name
	 * @return               Basic scope object
	 */
	public IBasicScope getBasicScope(String type, String name);

	/**
	 * Return basic scope names iterator
	 * @param type           Scope type
	 * @return               Iterator
	 */
	public Iterator<String> getBasicScopeNames(String type);

	/**
	 * Return child scope by name
	 * @param name           Scope name
	 * @return               Child scope with given name
	 */
	public IScope getScope(String name);

	/**
	 * Register service handler by name
	 * @param name       Service handler name
	 * @param handler    Service handler
	 */
	public void registerServiceHandler(String name, Object handler);

	/**
	 * Unregisters service handler by name
	 * @param name        Service handler name
	 */
	public void unregisterServiceHandler(String name);

	/**
	 * Return service handler by name
	 * @param name        Handler name
	 * @return            Service handler with given name
	 */
	public Object getServiceHandler(String name);

	/**
	 * Return set of service handler names
	 * @return            Set of service handler names
	 */
	public Set<String> getServiceHandlerNames();

	/**
	 * Return total number of connections to the scope.
	 * 
	 * @return number of connections
	 */
	public int getTotalConnections();
	
	/**
	 * Return maximum number of concurrent connections to the scope.
	 * 
	 * @return number of connections
	 */
	public int getMaxConnections();
	
	/**
	 * Return current number of connections to the scope.
	 * 
	 * @return number of connections
	 */
	public int getActiveConnections();
	
	/**
	 * Return total number of clients connected to the scope.
	 * 
	 * @return number of clients
	 */
	public int getTotalClients();
	
	/**
	 * Return maximum number of clients concurrently connected to the scope.
	 * 
	 * @return number of clients
	 */
	public int getMaxClients();
	
	/**
	 * Return current number of clients connected to the scope.
	 * 
	 * @return number of clients
	 */
	public int getActiveClients();
	
	/**
	 * Return total number of subscopes created.
	 * 
	 * @return number of subscopes created
	 */
	public int getTotalSubscopes();
	
	/**
	 * Return maximum number of concurrently existing subscopes.
	 * 
	 * @return number of subscopes
	 */
	public int getMaxSubscopes();
	
	/**
	 * Return number of currently existing subscopes.
	 * 
	 * @return number of subscopes
	 */
	public int getActiveSubscopes();

}
