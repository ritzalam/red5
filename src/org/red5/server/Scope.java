package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeAware;
import org.red5.server.api.IScopeHandler;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.PersistenceUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.style.ToStringCreator;

/**
 * The scope object.
 *
 * A statefull object shared between a group of clients connected to the same
 * context path. Scopes are arranged in a hierarchical way, so its possible for
 * a scope to have a parent. If a client is connect to a scope then they are
 * also connected to its parent scope. The scope object is used to access
 * resources, shared object, streams, etc.
 *
 * The following are all names for scopes: application, room, place, lobby.
 *
 * @author The Red5 Project (red5@osflash.org)
 */
public class Scope extends BasicScope implements IScope {
    /**
     * Logger
     */
	protected static Log log = LogFactory.getLog(Scope.class.getName());
    /**
     * Unset flag constant
     */
	private static final int UNSET = -1;
    /**
     * Scope type constant
     */
	private static final String TYPE = "scope";
    /**
     * Scope nesting depth, unset by default
     */
	private int depth = UNSET;
    /**
     * Scope context
     */
	private IContext context;
    /**
     * Scope handler
     */
	private IScopeHandler handler;
    /**
     * Autostart flag
     */
	private boolean autoStart = true;
    /**
     * Whether scope is enabled
     */
	private boolean enabled = true;
    /**
     * Whether scope is running
     */
	private boolean running;

    /**
     * Child scopes map (child scopes are named)
     */
	private Map<String, IBasicScope> children = new ConcurrentHashMap<String, IBasicScope>();
    /**
     * Clients and connection map
     */
	private Map<IClient, Set<IConnection>> clients = new ConcurrentHashMap<IClient, Set<IConnection>>();
	/**
	 * Registered service handlers for this scope. The map is created on-demand only
	 * if it's accessed for writing.
	 */
	private volatile Map<String, Object> serviceHandlers;

    /**
     * Creates unnamed scope
     */
	public Scope() {
		this(null);
	}

    /**
     * Creates scope with given name
     * @param name                      Scope name
     */
	public Scope(String name) {
		super(null, TYPE, name, false);
	}

    /**
     * Check if scope is enabled
     * @return                  <code>true</code> if scope is enabled, <code>false</code> otherwise
     */
	public boolean isEnabled() {
		return enabled;
	}

    /**
     * Enable or disable scope by setting enable flag
     * @param enabled            Enable flag value
     */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

    /**
     * Check if scope is in running state
     * @return                   <code>true</code> if scope is in running state, <code>false</code> otherwise
     */
	public boolean isRunning() {
		return running;
	}

    /**
     * Setter for autostart flag
     * @param autoStart         Autostart flag value
     */
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

    /**
     * Setter for context
     * @param context           Context object
     */
	public void setContext(IContext context) {
		this.context = context;
	}

    /**
     * Setter for scope event handler
     * @param handler           Event handler
     */
	public void setHandler(IScopeHandler handler) {
		this.handler = handler;
		if (handler instanceof IScopeAware) {
			((IScopeAware) handler).setScope(this);
		}
	}

    /**
     * Initialization actions, start if autostart is set to <code>true</code>
     */
	public void init() {
		if (hasParent()) {
			if (!parent.hasChildScope(name)) {
				if (!parent.addChildScope(this)) {
					return;
				}
			}
		}
		if (autoStart) {
			start();
		}
	}

    /**
     * Starts scope
     * @return     <code>true</code> if scope has handler and it's start method returned true, <code>false</code> otherwise
     */
	public boolean start() {
        return enabled && !running && !(hasHandler() && !handler.start(this));
    }

    /**
     * Stops scope
     */
	public void stop() {
	}

    /**
     * Destroys scope
     */
	public void destory() {
		if (hasParent()) {
			parent.removeChildScope(this);
		}
		if (hasHandler()) {
			handler.stop(this);
			// TODO:  kill all child scopes
		}
	}

    /**
     * Set scope persistence class
     *
     * @param persistenceClass       Scope's persistence class
     * @throws Exception             Exception
     */
	public void setPersistenceClass(String persistenceClass) throws Exception {
		this.persistenceClass = persistenceClass;
		if (persistenceClass != null) {
			setStore(PersistenceUtils.getPersistenceStore(this,
					persistenceClass));
		} else {
			setStore(null);
		}
	}

    /**
     * Add child scope to this scope
     * @param scope        Child scope
     * @return             <code>true</code> on success (if scope has handler and it accepts child scope addition), <code>false</code> otherwise
     */
	public boolean addChildScope(IBasicScope scope) {
		if (scope.getStore() == null) {
			// Child scope has no persistence store, use same class as parent.
			try {
				if (scope instanceof Scope) {
					((Scope) scope).setPersistenceClass(this.persistenceClass);
				}
			} catch (Exception error) {
				log.error("Could not set persistence class.", error);
			}
		}
		if (hasHandler() && !getHandler().addChildScope(scope)) {
			if (log.isDebugEnabled()) {
				log.debug("Failed to add child scope: " + scope + " to " + this);
			}
			return false;
		}
		if (scope instanceof IScope) {
			// start the scope
			if (hasHandler() && !getHandler().start((IScope) scope)) {
				if (log.isDebugEnabled()) {
					log.debug("Failed to start child scope: " + scope + " in " + this);
				}
				return false;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("Add child scope: " + scope + " to " + this);
		}
		children.put(scope.getType() + SEPARATOR + scope.getName(), scope);
		return true;
	}

    /**
     * Setter for child load path. Should be implemented in subclasses?
     * @param pattern            Load path pattern
     */
	public void setChildLoadPath(String pattern) {

	}

    /**
     * Removes child scope
     * @param scope       Child scope to remove
     */
	public void removeChildScope(IBasicScope scope) {
		if (scope instanceof IScope) {
			if (hasHandler()) {
				getHandler().stop((IScope) scope);
			}
		}
		children.remove(scope.getType() + SEPARATOR + scope.getName());
		if (hasHandler()) {
			log.debug("Remove child scope");
			getHandler().removeChildScope(scope);
		}
	}

    /**
     * Check whether scope has child scope with given name
     * @param name               Child scope name
     * @return                   <code>true</code> if scope has child node with given name, <code>false</code> otherwise
     */
	public boolean hasChildScope(String name) {
		if (log.isDebugEnabled()) {
			log.debug("Has child scope? " + name + " in " + this);
		}
		return children.containsKey(TYPE + SEPARATOR + name);
	}

    /**
     * Check whether scope has child scope with given name and type
     * @param type               Child scope type
     * @param name               Child scope name
     * @return                   <code>true</code> if scope has child node with given name and type, <code>false</code> otherwise
     */
	public boolean hasChildScope(String type, String name) {
		return children.containsKey(type + SEPARATOR + name);
	}

    /**
     * Return child scope names iterator
     * @return                   Child scope names iterator
     */
	public Iterator<String> getScopeNames() {
		return new PrefixFilteringStringIterator(children.keySet().iterator(),
				"scope");
	}

    /**
     * Return set of clients
     * @return                   Set of clients bound to scope
     */
	public Set<IClient> getClients() {
		return clients.keySet();
	}

    /**
     * Check if scope has a context
     * @return                   <code>true</code> if scope has context, <code>false</code> otherwise
     */
	public boolean hasContext() {
		return context != null;
	}

    /**
     * Return scope context. If scope doesn't have context, parent's context is returns, and so forth.
     * @return                   Scope context or parent context
     */
	public IContext getContext() {
		if (!hasContext() && hasParent()) {
			log.debug("returning parent context");
			return parent.getContext();
		} else {
			log.debug("returning context");
			return context;
		}
	}

    /**
     * Return scope context path
     * @return                   Scope context path
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

    /**
     * Setter for scope name
     * @param name               Scope name
     */
    @Override
	public void setName(String name) {
		this.name = name;
	}

    /**
     * Return scope path calculated from parent path and parent scope name
     * @return                   Scope path
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
     * Setter for parent scope
     * @param parent              Parent scope
     */
	public void setParent(IScope parent) {
		this.parent = parent;
	}

    /**
     * Check if scope or it's parent has handler
     * @return                     <code>true</code> if scope or it's parent scope has a handler, <code>false</code> otherwise
     */
	public boolean hasHandler() {
		return (handler != null || (hasParent() && getParent().hasHandler()));
	}

    /**
     * Return scope handler or parent's scope handler if this scope doesn't have one
     * @return                     Scope handler (or parent's one)
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

    /**
     * Return parent scope
     * @return                      Parent scope
     */
	@Override
	public IScope getParent() {
		return parent;
	}

    /**
     * Check if scope has parent scope
     * @return                      <code>true</code> if scope has parent scope, <code>false</code> otherwise`
     */
	@Override
	public boolean hasParent() {
		return (parent != null);
	}

    /**
     * Connect to scope
     * @param conn                  Connection object
     * @return                      <code>true</code> on success, <code>false</code> otherwise
     */
	public synchronized boolean connect(IConnection conn) {
		return connect(conn, null);
	}

    /**
     * Connect to scope with parameters. To successfully connect to scope it must have handler that will accept
     * this connection with given set of params. Client associated with connection is added to scope clients set,
     * connection is registred as scope event listener.
     *
     * @param conn                  Connection object
     * @param params                Params passed with connection
     * @return                      <code>true</code> on success, <code>false</code> otherwise
     */
	public synchronized boolean connect(IConnection conn, Object[] params) {
		//log.debug("Connect: "+conn+" to "+this);
		//log.debug("has handler? "+hasHandler());
		if (hasParent() && !parent.connect(conn, params)) {
			return false;
		}
		if (hasHandler() && !getHandler().connect(conn, this, params)) {
			return false;
		}
		final IClient client = conn.getClient();
		//log.debug("connected to: "+this);
		if (!clients.containsKey(client)) {
			//log.debug("Joining: "+this);
			if (hasHandler() && !getHandler().join(client, this)) {
				return false;
			}
			final Set<IConnection> conns = new HashSet<IConnection>();
			conns.add(conn);
			clients.put(conn.getClient(), conns);
			log.debug("adding client");
		} else {
			final Set<IConnection> conns = clients.get(client);
			conns.add(conn);
		}
		addEventListener(conn);
		return true;
	}

    /**
     * Disconnect connection from scope
     * @param conn            Connection object
     */
	public synchronized void disconnect(IConnection conn) {
		// We call the disconnect handlers in reverse order they were called
		// during connection, i.e. roomDisconnect is called before
		// appDisconnect.
		final IClient client = conn.getClient();
		if (client != null && clients.containsKey(client)) {
			final Set<IConnection> conns = clients.get(client);
			conns.remove(conn);
			IScopeHandler handler = null;
			if (hasHandler()) {
				handler = getHandler();
				try {
					handler.disconnect(conn, this);
				} catch (Exception e) {
					log.error(
							"Error while executing \"disconnect\" for connection "
									+ conn + " on handler " + handler, e);
				}
			}

			if (conns.isEmpty()) {
				clients.remove(client);
				if (handler != null) {
					try {
						// there may be a timeout here ?
						handler.leave(client, this);
					} catch (Exception e) {
						log.error("Error while executing \"leave\" for client "
								+ client + " on handler " + handler, e);
					}
				}
			}
			removeEventListener(conn);
		}
		if (hasParent()) {
			parent.disconnect(conn);
		}
	}

    /**
     * Set scope depth
     * @param depth         Scope depth
     */
	public void setDepth(int depth) {
		this.depth = depth;
	}

    /**
     * return scope depth
     * @return              Scope depth
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
     * Return array of resources from path string, usually used with pattern path
     * @param path           Resources path
     * @return               Resources
     * @throws IOException   I/O exception
     */
	public Resource[] getResources(String path) throws IOException {
		if (hasContext()) {
			return context.getResources(path);
		}
		return getContext().getResources(getContextPath() + '/' + path);
	}

    /**
     * Return resource located at given path
     * @param path           Resource path
     * @return               Resource
     */
	public Resource getResource(String path) {
		if (hasContext()) {
			return context.getResource(path);
		}
		return getContext().getResource(getContextPath() + '/' + path);
	}

    /**
     * Return connection iterator
     * @return                Connections iterator
     */
	public Iterator<IConnection> getConnections() {
		return new ConnectionIterator();
	}

    /**
     * Looks up connections for client
     * @param client          Client
     * @return                Connection
     */
	public Set<IConnection> lookupConnections(IClient client) {
		return clients.get(client);
	}

	/** {@inheritDoc} */
    @Override
	public void dispatchEvent(IEvent event) {
		Iterator<IConnection> conns = getConnections();
		while (conns.hasNext()) {
			try {
				conns.next().dispatchEvent(event);
			} catch (RuntimeException e) {
				log.error(e);
			}
		}
	}

    /**
     * Iterator that filters strings by given prefix
     */
	class PrefixFilteringStringIterator implements Iterator<String> {
        /**
         * Iterator
         */
		private Iterator<String> iterator;
        /**
         * Prefix
         */
		private String prefix;
        /**
         * Next object
         */
		private String next;

        /**
         * Creates prefix filtering string iterator from iterator and prefix
         * @param iterator             Iterator
         * @param prefix               Prefix
         */
		public PrefixFilteringStringIterator(Iterator<String> iterator,
				String prefix) {
			this.iterator = iterator;
			this.prefix = prefix;
		}

		/** {@inheritDoc} */
        public boolean hasNext() {
			if (next != null) {
				return true;
			}
			do {
				next = (iterator.hasNext()) ? iterator.next() : null;
			} while (next != null && !next.startsWith(prefix));
			return next != null;
		}

		/** {@inheritDoc} */
        public String next() {
			if (next != null) {
				final String result = next;
				next = null;
				return result.substring(prefix.length());
			}
			if (hasNext()) {
				return next();
			} else {
				return null;
			}
		}

		/** {@inheritDoc} */
        public void remove() {
			throw new UnsupportedOperationException();
		}

	}

    /**
     * Iterates through connections
     */
	class ConnectionIterator implements Iterator<IConnection> {
        /**
         * Set iterator
         */
		private Iterator<Set<IConnection>> setIterator;
        /**
         * Connections iterator
         */
		private Iterator<IConnection> connIterator;
        /**
         * Current connection
         */
		private IConnection current;

        /**
         * Creates connection iterator
         */
		public ConnectionIterator() {
			setIterator = clients.values().iterator();
		}

        /**
         * {@inheritDoc}
         */
		public boolean hasNext() {
			if (connIterator != null && connIterator.hasNext()) {
				// More connections for this client
				return true;
			}
			
			if (!setIterator.hasNext()) {
				// No more clients
				return false;
			}
			
			connIterator = setIterator.next().iterator();
			while (connIterator != null) {
				if (connIterator.hasNext()) {
					// Found client with connections
					return true;
				}
				
				if (!setIterator.hasNext()) {
					// No more clients
					return false;
				}
				
				// Advance to next client
				connIterator = setIterator.next().iterator();
			}
			return false;
		}

        /**
         * {@inheritDoc}
         */
		public IConnection next() {
			if (connIterator == null || !connIterator.hasNext()) {
				if (!setIterator.hasNext()) {
					// No more clients
					throw new NoSuchElementException();
				}
				
				connIterator = setIterator.next().iterator();
				while (!connIterator.hasNext()) {
					// Client has no connections, search next one
					if (!setIterator.hasNext()) {
						// No more clients
						throw new NoSuchElementException();
					}
					
					connIterator = setIterator.next().iterator();
				}
			}
            // Always of type IConnection, no need to cast
            current = connIterator.next();
			return current;
		}

        /**
         * {@inheritDoc}
         */
		public void remove() {
			if (current != null) {
				disconnect(current);
			}
		}

	}

    /**
     * Create child scope with given name
     * @param name           Child scope name
     * @return               <code>true</code> on success, <code>false</code> otherwise
     */
	public boolean createChildScope(String name) {
		final Scope scope = new Scope(name);
		scope.setParent(this);
		return addChildScope(scope);
	}

    /**
     * Handles event. To be implemented in subclasses.
     * @param event          Event to handle
     * @return               <code>true</code> on success, <code>false</code> otherwise
     */
	@Override
	public boolean handleEvent(IEvent event) {
		return false;
	}

    /**
     * Return base scope of given type with given name
     * @param type           Scope type
     * @param name           Scope name
     * @return               Basic scope object
     */
	public IBasicScope getBasicScope(String type, String name) {
		return children.get(type + SEPARATOR + name);
	}

    /**
     * Return basic scope names iterator
     * @param type           Scope type
     * @return               Iterator
     */
	public Iterator<String> getBasicScopeNames(String type) {
		if (type == null) {
			return children.keySet().iterator();
		} else {
			return new PrefixFilteringStringIterator(children.keySet()
					.iterator(), type + SEPARATOR);
		}
	}

    /**
     * Return child scope by name
     * @param name           Scope name
     * @return               Child scope with given name
     */
	public IScope getScope(String name) {
		return (IScope) children.get(TYPE + SEPARATOR + name);
	}

    /**
     * Child scopes iterator
     * @return               Child scopes iterator
     */
	@Override
	public Iterator<IBasicScope> iterator() {
		return children.values().iterator();
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String toString() {
		final ToStringCreator tsc = new ToStringCreator(this);
		return tsc.append("Depth", getDepth()).append("Path", getPath())
				.append("Name", getName()).toString();
	}

    /**
     * Return map of service handlers. The map is created if it doesn't exist yet.
     * @return                Map of service handlers
     */
	protected Map<String, Object> getServiceHandlers() {
		return getServiceHandlers(true);
	}
	
    /**
     * Return map of service handlers and optionally created it if it doesn't exist.
     * @param allowCreate     Should the map be created if it doesn't exist?
     * @return                Map of service handlers
     */
	protected Map<String, Object> getServiceHandlers(boolean allowCreate) {
		if (serviceHandlers == null) {
			if (!allowCreate)
				return null;
			
			// Only synchronize if potentially needs to be created
			synchronized (this) {
				if (serviceHandlers == null) {
					serviceHandlers = new ConcurrentHashMap<String, Object>();
				}
			}
		}
		return serviceHandlers;
	}

    /**
     * Register service handler by name
     * @param name       Service handler name
     * @param handler    Service handler
     */
	public void registerServiceHandler(String name, Object handler) {
		Map<String, Object> serviceHandlers = getServiceHandlers();
		serviceHandlers.put(name, handler);
	}

    /**
     * Unregisters service handler by name
     * @param name        Service handler name
     */
	public void unregisterServiceHandler(String name) {
		Map<String, Object> serviceHandlers = getServiceHandlers(false);
		if (serviceHandlers == null)
			return;
		
		serviceHandlers.remove(name);
	}

    /**
     * Return service handler by name
     * @param name        Handler name
     * @return            Service handler with given name
     */
	public Object getServiceHandler(String name) {
		Map<String, Object> serviceHandlers = getServiceHandlers(false);
		if (serviceHandlers == null)
			return null;
		
		return serviceHandlers.get(name);
	}

    /**
     * Return set of service handler names. Removing entries from the
     * set unregisters the corresponding service handler.
     * @return            Set of service handler names
     */
	public Set<String> getServiceHandlerNames() {
		Map<String, Object> serviceHandlers = getServiceHandlers(false);
		if (serviceHandlers == null)
			return Collections.EMPTY_SET;
		
		return serviceHandlers.keySet();
	}

    /**
     * Return current thread context classloader
     * @return         Current thread context classloader
     */
	public ClassLoader getClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}
}
