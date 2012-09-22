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

package org.red5.server.scope;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Semaphore;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;

import org.apache.commons.lang3.StringUtils;
import org.red5.server.AttributeStore;
import org.red5.server.Server;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IServer;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.persistence.PersistenceUtils;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeAware;
import org.red5.server.api.scope.IScopeHandler;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.statistics.IScopeStatistics;
import org.red5.server.api.statistics.support.StatisticsCounter;
import org.red5.server.jmx.mxbeans.ScopeMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.style.ToStringCreator;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * The scope object.
 * <p>
 * A stateful object shared between a group of clients connected to the same
 * context path. Scopes are arranged in a hierarchical way, so its possible for
 * a scope to have a parent. If a client is connect to a scope then they are
 * also connected to its parent scope. The scope object is used to access
 * resources, shared object, streams, etc.</p>
 * Scope layout:
 * <pre>
 *  /Global scope - Contains application scopes
 *      /Application scope - Contains room, shared object, and stream scopes
 *          /Room scope - Contains other room, shared object, and / or stream scopes
 *              /Shared object scope - Contains shared object
 *              /Broadcast stream scope - Contains a broadcast stream
 * </pre>
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Nathan Smith (nathgs@gmail.com)
 */
@ManagedResource(objectName = "org.red5.server:type=Scope", description = "Scope")
public class Scope extends BasicScope implements IScope, IScopeStatistics, ScopeMXBean {

	protected static Logger log = LoggerFactory.getLogger(Scope.class);

	/**
	 * Unset flag constant
	 */
	private static final int UNSET = -1;

	/**
	 * Auto-start flag
	 */
	private boolean autoStart = true;

	/**
	 * Child scopes
	 */
	private final ConcurrentScopeSet<IBasicScope> children = new ConcurrentScopeSet<IBasicScope>();

	/**
	 * Clients and connection map
	 */
	private final ConcurrentMap<IClient, Set<IConnection>> clients = new ConcurrentHashMap<IClient, Set<IConnection>>();

	/**
	 * Storage for scope attributes
	 */
	protected final AttributeStore attributes = new AttributeStore();

	/**
	 * Statistics about clients connected to the scope.
	 */
	protected final StatisticsCounter clientStats = new StatisticsCounter();

	/**
	 * Statistics about connections to the scope.
	 */
	protected final StatisticsCounter connectionStats = new StatisticsCounter();

	/**
	 * Statistics about sub-scopes.
	 */
	protected final StatisticsCounter subscopeStats = new StatisticsCounter();

	/**
	 * Scope context
	 */
	private IContext context;

	/**
	 * Timestamp the scope was created.
	 */
	private long creationTime;

	/**
	 * Scope nesting depth, unset by default
	 */
	private int depth = UNSET;

	/**
	 * Whether scope is enabled
	 */
	private boolean enabled = true;

	/**
	 * Scope handler
	 */
	private IScopeHandler handler;

	/**
	 * Whether scope is running
	 */
	private boolean running;

	/**
	 * Lock for critical sections, to prevent concurrent modification. 
	 * A "fairness" policy is used wherein the longest waiting thread will be granted access before others.
	 */
	protected Semaphore lock = new Semaphore(1, true);

	/**
	 * Registered service handlers for this scope. The map is created on-demand
	 * only if it's accessed for writing.
	 */
	private volatile ConcurrentMap<String, Object> serviceHandlers;

	/**
	 * Mbean object name.
	 */
	protected ObjectName oName;

	{
		creationTime = System.currentTimeMillis();
	}

	/**
	 * Creates a scope
	 */
	@ConstructorProperties(value = { "" })
	public Scope() {
		super(null, ScopeType.UNDEFINED, null, false);
	}

	/**
	 * Creates scope using a Builder
	 * 
	 * @param builder
	 */
	@ConstructorProperties({ "builder" })
	public Scope(Builder builder) {
		super(builder.parent, builder.type, builder.name, builder.persistent);
	}

	/**
	 * Add child scope to this scope
	 * 
	 * @param scope Child scope
	 * @return <code>true</code> on success (if scope has handler and it
	 *         accepts child scope addition), <code>false</code> otherwise
	 */
	public boolean addChildScope(IBasicScope scope) {
		log.debug("Add child: {}", scope);
		if (!children.contains(scope)) {
			if (scope.isValid()) {
				log.debug("Add child scope: {} to {}", scope, this);
				// if child scope has no persistence store, use same class as parent
				if (scope.getStore() == null) {
					try {
						if (scope instanceof Scope) {
							((Scope) scope).setPersistenceClass(persistenceClass);
						}
					} catch (Exception error) {
						log.error("Could not set persistence class.", error);
					}
				}
				return children.add(scope);
			} else {
				log.warn("Invalid scope was not added: {}", scope);
			}
		} else {
			log.warn("Child scope already exists");
		}
		return false;
	}

	/**
	 * Connect to scope
	 * 
	 * @param conn Connection object
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean connect(IConnection conn) {
		return connect(conn, null);
	}

	/**
	 * Connect to scope with parameters. To successfully connect to scope it
	 * must have handler that will accept this connection with given set of
	 * parameters. Client associated with connection is added to scope clients set,
	 * connection is registered as scope event listener.
	 * 
	 * @param conn Connection object
	 * @param params Parameters passed with connection
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean connect(IConnection conn, Object[] params) {
		log.debug("Connect: {}", conn);
		if (hasParent() && !parent.connect(conn, params)) {
			return false;
		}
		if (hasHandler() && !getHandler().connect(conn, this, params)) {
			return false;
		}
		final IClient client = conn.getClient();
		if (!conn.isConnected()) {
			// timeout while connecting client
			return false;
		}
		// we would not get this far if there is no handler
		if (hasHandler() && !getHandler().join(client, this)) {
			return false;
		}
		// checking the connection again? why?
		if (!conn.isConnected()) {
			// Timeout while connecting client
			return false;
		}

		Set<IConnection> conns = clients.get(client);
		if (conns == null) {
			conns = new CopyOnWriteArraySet<IConnection>();
			clients.put(client, conns);
		}
		conns.add(conn);

		clientStats.increment();
		addEventListener(conn);
		connectionStats.increment();

		IScope connScope = conn.getScope();
		log.trace("Connection scope: {}", connScope);
		if (this.equals(connScope)) {
			final IServer server = getServer();
			if (server instanceof Server) {
				((Server) server).notifyConnected(conn);
			}
		}
		return true;
	}

	/**
	 * Create child scope with given name
	 * 
	 * @param name Child scope name
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	public boolean createChildScope(String name) {
		final Scope scope = new Builder(this, ScopeType.ROOM, name, false).build();
		return addChildScope(scope);
	}

	/**
	 * Destroys scope
	 */
	public void destroy() {
		log.debug("Destroy scope");
		if (hasParent()) {
			parent.removeChildScope(this);
		}
		if (hasHandler()) {
			// Because handler can be null when there is a parent handler
			getHandler().stop(this);
		}
		// kill all child scopes
		for (IBasicScope child : children) {
			removeChildScope(child);
			if (child instanceof Scope) {
				((Scope) child).uninit();
			}
		}
	}

	/**
	 * Disconnect connection from scope
	 * 
	 * @param conn Connection object
	 */
	public void disconnect(IConnection conn) {
		log.debug("Disconnect: {}", conn);
		// We call the disconnect handlers in reverse order they were called
		// during connection, i.e. roomDisconnect is called before
		// appDisconnect.
		final IClient client = conn.getClient();
		if (client == null) {
			// Early bail out
			removeEventListener(conn);
			connectionStats.decrement();
			if (hasParent()) {
				parent.disconnect(conn);
			}
			return;
		}
		// remove it if it exists
		final Set<IConnection> conns = clients.remove(client);
		if (conns != null) {
			// decrement if there was a set of connections
			clientStats.decrement();
			conns.remove(conn);
			IScopeHandler handler = null;
			if (hasHandler()) {
				handler = getHandler();
				try {
					handler.disconnect(conn, this);
				} catch (Exception e) {
					log.error("Error while executing \"disconnect\" for connection {} on handler {}. {}", new Object[] { conn, handler, e });
				}
			}
			if (conns.isEmpty()) {
				if (handler != null) {
					try {
						// there may be a timeout here ?
						handler.leave(client, this);
					} catch (Exception e) {
						log.error("Error while executing \"leave\" for client {} on handler {}. {}", new Object[] { conn, handler, e });
					}
				}
			}
			removeEventListener(conn);
			connectionStats.decrement();
			if (this.equals(conn.getScope())) {
				final IServer server = getServer();
				if (server instanceof Server) {
					((Server) server).notifyDisconnected(conn);
				}
			}
		}
		if (hasParent()) {
			parent.disconnect(conn);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void dispatchEvent(IEvent event) {
		Collection<Set<IConnection>> conns = getConnections();
		for (Set<IConnection> set : conns) {
			for (IConnection conn : set) {
				try {
					conn.dispatchEvent(event);
				} catch (RuntimeException e) {
					log.error("Exception during dispatching event: {}", event, e);
				}
			}
		}
	}

	/** {@inheritDoc} */
	public Object getAttribute(String name) {
		return attributes.getAttribute(name);
	}

	/** {@inheritDoc} */
	public boolean setAttribute(String name, Object value) {
		return attributes.setAttribute(name, value);
	}

	/** {@inheritDoc} */
	public boolean hasAttribute(String name) {
		return attributes.hasAttribute(name);
	}

	/** {@inheritDoc} */
	public boolean removeAttribute(String name) {
		return attributes.removeAttribute(name);
	}

	/** {@inheritDoc} */
	public Set<String> getAttributeNames() {
		return attributes.getAttributeNames();
	}

	/** {@inheritDoc} */
	public Map<String, Object> getAttributes() {
		return attributes.getAttributes();
	}

	/** {@inheritDoc} */
	public int getActiveClients() {
		return clients.size();
	}

	/** {@inheritDoc} */
	public int getActiveConnections() {
		return connectionStats.getCurrent();
	}

	/** {@inheritDoc} */
	public int getActiveSubscopes() {
		return subscopeStats.getCurrent();
	}

	/**
	 * Return the broadcast scope for a given name
	 * 
	 * @param name
	 * @return broadcast scope or null if not found
	 */
	public IBroadcastScope getBroadcastScope(String name) {
		for (IBasicScope child : children) {
			if (child.getType().equals(ScopeType.BROADCAST) && child.getName().equals(name)) {
				log.debug("Returning broadcast scope");
				return (IBroadcastScope) child;
			}
		}
		return null;
	}

	/**
	 * Return base scope of given type with given name
	 * 
	 * @param type Scope type
	 * @param name Scope name
	 * @return Basic scope object
	 */
	public IBasicScope getBasicScope(ScopeType type, String name) {
		for (IBasicScope child : children) {
			if (child.getType().equals(type) && child.getName().equals(name)) {
				log.debug("Returning basic scope");
				return child;
			}
		}
		return null;
	}

	/**
	 * Return basic scope names matching given type
	 * 
	 * @param type Scope type
	 * @return set of scope names 
	 */
	public Set<String> getBasicScopeNames(ScopeType type) {
		if (type != null) {
			Set<String> names = new HashSet<String>();
			for (IBasicScope child : children) {
				if (child.getType().equals(type)) {
					names.add(child.getName());
				}
			}
			return names;
		} else {
			return getScopeNames();
		}
	}

	/**
	 * Return current thread context classloader
	 * 
	 * @return Current thread context classloader
	 */
	public ClassLoader getClassLoader() {
		return getContext().getClassLoader();
	}

	/**
	 * Return set of clients
	 * 
	 * @return Set of clients bound to scope
	 */
	public Set<IClient> getClients() {
		return clients.keySet();
	}

	/**
	 * Return connection iterator
	 * 
	 * @return Connections iterator
	 */
	public Collection<Set<IConnection>> getConnections() {
		return clients.values();
	}

	/**
	 * Return scope context. If scope doesn't have context, parent's context is
	 * returns, and so forth.
	 * 
	 * @return Scope context or parent context
	 */
	public IContext getContext() {
		if (!hasContext() && hasParent()) {
			//log.debug("returning parent context");
			return parent.getContext();
		} else {
			//log.debug("returning context");
			return context;
		}
	}

	/**
	 * Return scope context path
	 * 
	 * @return Scope context path
	 */
	public String getContextPath() {
		if (hasContext()) {
			return "";
		} else if (hasParent()) {
			return parent.getContextPath() + '/' + name;
		} else {
			return null;
		}
	}

	/** {@inheritDoc} */
	public long getCreationTime() {
		return creationTime;
	}

	/**
	 * return scope depth
	 * 
	 * @return Scope depth
	 */
	@Override
	public int getDepth() {
		if (depth == UNSET) {
			if (hasParent()) {
				depth = parent.getDepth() + 1;
			} else {
				depth = 0;
			}
		}
		return depth;
	}

	/**
	 * Return scope handler or parent's scope handler if this scope doesn't have
	 * one
	 * 
	 * @return Scope handler (or parent's one)
	 */
	public IScopeHandler getHandler() {
		if (handler != null) {
			return handler;
		} else if (hasParent()) {
			return getParent().getHandler();
		} else {
			return null;
		}
	}

	/** {@inheritDoc} */
	public int getMaxClients() {
		return clientStats.getMax();
	}

	/** {@inheritDoc} */
	public int getMaxConnections() {
		return connectionStats.getMax();
	}

	/** {@inheritDoc} */
	public int getMaxSubscopes() {
		return subscopeStats.getMax();
	}

	/**
	 * Return parent scope
	 * 
	 * @return Parent scope
	 */
	@Override
	public IScope getParent() {
		return parent;
	}

	/**
	 * Return scope path calculated from parent path and parent scope name
	 * 
	 * @return Scope path
	 */
	@Override
	public String getPath() {
		if (hasParent()) {
			return parent.getPath() + '/' + parent.getName();
		} else {
			return "";
		}
	}

	/**
	 * Return resource located at given path
	 * 
	 * @param path Resource path
	 * @return Resource
	 */
	public Resource getResource(String path) {
		if (hasContext()) {
			return context.getResource(path);
		}
		return getContext().getResource(getContextPath() + '/' + path);
	}

	/**
	 * Return array of resources from path string, usually used with pattern
	 * path
	 * 
	 * @param path Resources path
	 * @return Resources
	 * @throws IOException I/O exception
	 */
	public Resource[] getResources(String path) throws IOException {
		if (hasContext()) {
			return context.getResources(path);
		}
		return getContext().getResources(getContextPath() + '/' + path);
	}

	/**
	 * Return child scope by name
	 * 
	 * @param name Scope name
	 * @return Child scope with given name
	 */
	public IScope getScope(String name) {
		for (IBasicScope child : children) {
			if (child.getName().equals(name)) {
				log.debug("Returning child scope");
				return (IScope) child;
			}
		}
		return null;
	}

	/**
	 * Return child scope names iterator
	 * 
	 * @return Child scope names iterator
	 */
	public Set<String> getScopeNames() {
		log.debug("Children: {}", children);
		Set<String> names = new HashSet<String>();
		for (IBasicScope child : children) {
			names.add(child.getName());
		}
		return names;
	}

	/**
	 * Return service handler by name
	 * 
	 * @param name Handler name
	 * @return Service handler with given name
	 */
	public Object getServiceHandler(String name) {
		Map<String, Object> serviceHandlers = getServiceHandlers(false);
		if (serviceHandlers == null) {
			return null;
		}
		return serviceHandlers.get(name);
	}

	/**
	 * Return set of service handler names. Removing entries from the set
	 * unregisters the corresponding service handler.
	 * 
	 * @return Set of service handler names
	 */
	@SuppressWarnings("unchecked")
	public Set<String> getServiceHandlerNames() {
		Map<String, Object> serviceHandlers = getServiceHandlers(false);
		if (serviceHandlers == null) {
			return Collections.EMPTY_SET;
		}
		return serviceHandlers.keySet();
	}

	/**
	 * Return map of service handlers. The map is created if it doesn't exist
	 * yet.
	 * 
	 * @return Map of service handlers
	 */
	protected Map<String, Object> getServiceHandlers() {
		return getServiceHandlers(true);
	}

	/**
	 * Return map of service handlers and optionally created it if it doesn't
	 * exist.
	 * 
	 * @param allowCreate
	 *            Should the map be created if it doesn't exist?
	 * @return Map of service handlers
	 */
	protected Map<String, Object> getServiceHandlers(boolean allowCreate) {
		if (serviceHandlers == null) {
			if (allowCreate) {
				serviceHandlers = new ConcurrentHashMap<String, Object>();
			}
		}
		return serviceHandlers;
	}

	/** {@inheritDoc} */
	public IScopeStatistics getStatistics() {
		return this;
	}

	/** {@inheritDoc} */
	public int getTotalClients() {
		return clientStats.getTotal();
	}

	/** {@inheritDoc} */
	public int getTotalConnections() {
		return connectionStats.getTotal();
	}

	/** {@inheritDoc} */
	public int getTotalSubscopes() {
		return subscopeStats.getTotal();
	}

	/**
	 * Handles event. To be implemented in subclasses.
	 * 
	 * @param event Event to handle
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	@Override
	public boolean handleEvent(IEvent event) {
		return false;
	}

	/**
	 * Check whether scope has child scope with given name
	 * 
	 * @param name Child scope name
	 * @return <code>true</code> if scope has child node with given name,
	 *         <code>false</code> otherwise
	 */
	public boolean hasChildScope(String name) {
		log.debug("Has child scope? {} in {}", name, this);
		for (IBasicScope child : children) {
			if (child.getName().equals(name)) {
				log.debug("Child scope exists");
				return true;
			}
		}
		log.debug("Child scope does not exist");
		return false;
	}

	/**
	 * Check whether scope has child scope with given name and type
	 * 
	 * @param type Child scope type
	 * @param name Child scope name
	 * @return <code>true</code> if scope has child node with given name and
	 *         type, <code>false</code> otherwise
	 */
	public boolean hasChildScope(ScopeType type, String name) {
		log.debug("Has child scope? {} in {}", name, this);
		for (IBasicScope child : children) {
			if (child.getName().equals(name) && child.getType().equals(type)) {
				log.debug("Child scope exists");
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if scope has a context
	 * 
	 * @return <code>true</code> if scope has context, <code>false</code>
	 *         otherwise
	 */
	public boolean hasContext() {
		return context != null;
	}

	/**
	 * Check if scope or it's parent has handler
	 * 
	 * @return <code>true</code> if scope or it's parent scope has a handler,
	 *         <code>false</code> otherwise
	 */
	public boolean hasHandler() {
		return (handler != null || (hasParent() && getParent().hasHandler()));
	}

	/**
	 * Check if scope has parent scope
	 * 
	 * @return <code>true</code> if scope has parent scope, <code>false</code>
	 *         otherwise`
	 */
	@Override
	public boolean hasParent() {
		return (parent != null);
	}

	/**
	 * Initialization actions, start if autostart is set to <code>true</code>
	 */
	public void init() {
		log.debug("Init scope: {} parent: {}", name, parent);
		if (hasParent()) {
			if (parent.addChildScope(this)) {
				log.debug("Scope added to parent");
			} else {
				return;
			}
		} else {
			log.debug("Scope has no parent");
		}
		if (autoStart) {
			start();
		}
	}

	/**
	 * Uninitialize scope and unregister from parent.
	 */
	public void uninit() {
		log.debug("Un-init scope");
		for (IBasicScope child : children) {
			if (child instanceof Scope) {
				((Scope) child).uninit();
			}
		}
		stop();
		if (hasParent()) {
			if (parent.hasChildScope(name)) {
				parent.removeChildScope(this);
			}
		}
	}

	/**
	 * Check if scope is enabled
	 * 
	 * @return <code>true</code> if scope is enabled, <code>false</code>
	 *         otherwise
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Here for JMX only, uses isEnabled()
	 */
	public boolean getEnabled() {
		return isEnabled();
	}

	/**
	 * Check if scope is in running state
	 * 
	 * @return <code>true</code> if scope is in running state,
	 *         <code>false</code> otherwise
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Here for JMX only, uses isEnabled()
	 */
	public boolean getRunning() {
		return isRunning();
	}

	/**
	 * Looks up connections for client
	 * 
	 * @param client Client
	 * @return Connection
	 */
	public Set<IConnection> lookupConnections(IClient client) {
		return clients.get(client);
	}

	/**
	 * Register service handler by name
	 * 
	 * @param name Service handler name
	 * @param handler Service handler
	 */
	public void registerServiceHandler(String name, Object handler) {
		Map<String, Object> serviceHandlers = getServiceHandlers();
		serviceHandlers.put(name, handler);
	}

	/**
	 * Removes child scope
	 * 
	 * @param scope Child scope to remove
	 */
	public void removeChildScope(IBasicScope scope) {
		log.debug("removeChildScope: {}", scope);
		if (hasChildScope(scope.getName())) {
			// remove from parent
			children.remove(scope);
			if (scope instanceof Scope) {
				unregisterJMX();
			}
		}
	}

	/**
	 * Removes all the child scopes
	 */
	public void removeChildren() {
		for (IBasicScope child : children) {
			removeChildScope(child);
		}
	}

	/**
	 * Setter for autostart flag
	 * 
	 * @param autoStart Autostart flag value
	 */
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	/**
	 * Setter for child load path. Should be implemented in subclasses?
	 * 
	 * @param pattern Load path pattern
	 */
	public void setChildLoadPath(String pattern) {

	}

	/**
	 * Setter for context
	 * 
	 * @param context Context object
	 */
	public void setContext(IContext context) {
		log.debug("Set context: {}", context);
		this.context = context;
	}

	/**
	 * Set scope depth
	 * 
	 * @param depth Scope depth
	 */
	public void setDepth(int depth) {
		this.depth = depth;
	}

	/**
	 * Enable or disable scope by setting enable flag
	 * 
	 * @param enabled Enable flag value
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Setter for scope event handler
	 * 
	 * @param handler Event handler
	 */
	public void setHandler(IScopeHandler handler) {
		log.debug("Set handler: {}", handler);
		this.handler = handler;
		if (handler instanceof IScopeAware) {
			((IScopeAware) handler).setScope(this);
		}
	}

	/**
	 * Setter for scope name
	 * 
	 * @param name Scope name
	 */
	@Override
	public void setName(String name) {
		log.debug("Set name: {}", name);
		if (oName != null) {
			unregisterJMX();
		}
		this.name = name;
		if (StringUtils.isNotBlank(name)) {
			registerJMX();
		}
	}

	/**
	 * Setter for parent scope
	 * 
	 * @param parent Parent scope
	 */
	public void setParent(IScope parent) {
		log.debug("Set parent scope: {}", parent);
		this.parent = parent;
	}

	/**
	 * Set scope persistence class
	 * 
	 * @param persistenceClass Scope's persistence class
	 * @throws Exception Exception
	 */
	public void setPersistenceClass(String persistenceClass) throws Exception {
		this.persistenceClass = persistenceClass;
		if (persistenceClass != null) {
			store = PersistenceUtils.getPersistenceStore(this, persistenceClass);
		}
	}

	/**
	 * Starts scope
	 * 
	 * @return <code>true</code> if scope has handler and it's start method
	 *         returned true, <code>false</code> otherwise
	 */
	public boolean start() {
		log.debug("Start scope");
		boolean result = false;
		if (enabled && !running) {
			// check for any handlers
			if (hasHandler()) {
				try {
					lock.acquire();
					// if we dont have a handler of our own dont try to start it
					if (handler != null) {
						result = handler.start(this);
					}
				} catch (Throwable e) {
					log.error("Could not start scope {} {}", this, e);
				} finally {
					lock.release();
				}
			} else {
				// Always start scopes without handlers
				log.debug("Scope {} has no handler, allowing start", this);
				result = true;
			}
			running = result;
		}
		return result;
	}

	/**
	 * Stops scope
	 */
	public void stop() {
		log.debug("Stop scope");
		if (enabled && running && handler != null) {
			try {
				lock.acquire();
				// if we dont have a handler of our own dont try to stop it
				handler.stop(this);
			} catch (Throwable e) {
				log.error("Could not stop scope {}", this, e);
			} finally {
				lock.release();
			}
		}
		running = false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		return tsc.append("Depth", getDepth()).append("Path", getPath()).append("Name", getName()).toString();
	}

	/**
	 * Unregisters service handler by name
	 * 
	 * @param name Service handler name
	 */
	public void unregisterServiceHandler(String name) {
		Map<String, Object> serviceHandlers = getServiceHandlers(false);
		if (serviceHandlers == null) {
			return;
		}
		serviceHandlers.remove(name);
	}

	/**
	 * Return the server instance connected to this scope.
	 * 
	 * @return the server instance
	 */
	public IServer getServer() {
		if (!hasParent()) {
			return null;
		}
		final IScope parent = getParent();
		if (parent instanceof Scope) {
			return ((Scope) parent).getServer();
		} else if (parent instanceof IGlobalScope) {
			return ((IGlobalScope) parent).getServer();
		} else {
			return null;
		}
	}

	//for debugging
	public void dump() {
		if (log.isDebugEnabled()) {
			log.debug("Scope: {} {}", this.getClass().getName(), this);
			log.debug("Running: {}", running);
			if (hasParent()) {
				log.debug("Parent: {}", parent);
				Set<String> names = parent.getBasicScopeNames(null);
				log.debug("Sibling count: {}", names.size());
				for (String sib : names) {
					log.debug("Siblings - {}", sib);
				}
				names = null;
			}
			log.debug("Handler: {}", handler);
			log.debug("Child count: {}", children.size());
			for (IBasicScope entry : children) {
				log.debug("Child: {}", entry);
			}
		}
	}

	protected void registerJMX() {
		// register with jmx
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			String cName = this.getClass().getName();
			if (cName.indexOf('.') != -1) {
				cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
			}
			oName = new ObjectName(String.format("org.red5.server:type=%s,name=%s", cName, name));
			// don't reregister
			if (!mbs.isRegistered(oName)) {
				mbs.registerMBean(new StandardMBean(this, ScopeMXBean.class, true), oName);
			}
		} catch (Exception e) {
			log.warn("Error on jmx registration", e);
		}
	}

	protected void unregisterJMX() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		if (oName != null && mbs.isRegistered(oName)) {
			try {
				mbs.unregisterMBean(oName);
			} catch (Exception e) {
				log.warn("Exception unregistering: {}", oName, e);
			}
			oName = null;
		}
	}

	/**
	 * Allows for reconstruction via CompositeData.
	 * 
	 * @param cd composite data
	 * @return Scope class instance
	 */
	public static Scope from(CompositeData cd) {
		IScope parent = null;
		ScopeType type = ScopeType.UNDEFINED;
		String name = null;
		boolean persistent = false;
		if (cd.containsKey("parent")) {
			parent = (IScope) cd.get("parent");
		}
		if (cd.containsKey("type")) {
			type = (ScopeType) cd.get("type");
		}
		if (cd.containsKey("name")) {
			name = (String) cd.get("name");
		}
		if (cd.containsKey("persistent")) {
			persistent = (Boolean) cd.get("persistent");
		}
		return new Scope(new Builder(parent, type, name, persistent));
	}

	@SuppressWarnings({ "hiding", "serial" })
	private final class ConcurrentScopeSet<IBasicScope> extends CopyOnWriteArraySet<IBasicScope> {

		@Override
		public boolean add(IBasicScope scope) {
			if (scope instanceof IScope) {
				return add((IScope) scope);
			} else {
				log.debug("Adding basic scope to scope set");
				if (hasHandler() && !getHandler().addChildScope((org.red5.server.api.scope.IBasicScope) scope)) {
					log.debug("Failed to add child scope: {} to {}", scope, this);
					return false;
				}
				boolean added = super.add(scope);
				if (added) {
					subscopeStats.increment();
				}
				return added;
			}
		}

		public boolean add(IScope scope) {
			log.debug("Adding scope to scope set");
			if (hasHandler()) {
				// add the scope to the handler
				if (!getHandler().addChildScope(scope)) {
					log.debug("Failed to add child scope: {} to {}", scope, this);
					return false;
				}
				// start the scope
				if (!getHandler().start((IScope) scope)) {
					log.debug("Failed to start child scope: {} in {}", scope, this);
					return false;
				}
			}
			// add the entry
			@SuppressWarnings("unchecked")
			boolean added = super.add((IBasicScope) scope);
			if (added) {
				subscopeStats.increment();
				// post notification
				IServer server = getServer();
				((Server) server).notifyScopeCreated((IScope) scope);
			}
			return added;
		}

		@Override
		public boolean remove(Object scope) {
			log.debug("Remove child scope: {}", scope);
			if (hasHandler()) {
				log.debug("Remove child scope");
				getHandler().removeChildScope((org.red5.server.api.scope.IBasicScope) scope);
			}
			if (scope instanceof IScope) {
				if (hasHandler()) {
					getHandler().stop((IScope) scope);
				}
				// remove all children
				((IScope) scope).removeChildren();
				// post notification
				final IServer server = getServer();
				if (server instanceof Server) {
					((Server) server).notifyScopeRemoved((IScope) scope);
				}
			}
			// remove the entry
			boolean removed = super.remove(scope);
			if (removed) {
				subscopeStats.decrement();
			}
			return removed;
		}

	}

	/**
	 * Builder pattern
	 */
	public final static class Builder {
		private IScope parent;

		private ScopeType type;

		private String name;

		private boolean persistent;

		public Builder(IScope parent, ScopeType type, String name, boolean persistent) {
			this.parent = parent;
			this.type = type;
			this.name = name;
			this.persistent = persistent;
		}

		public Scope build() {
			return new Scope(this);
		}

	}

}
