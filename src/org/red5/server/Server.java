package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.red5.server.api.IConnection;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IScope;
import org.red5.server.api.IServer;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.listeners.IScopeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.style.ToStringCreator;

/**
 * Red5 server core class implementation.
 */
public class Server implements IServer, ApplicationContextAware, InitializingBean, DisposableBean {

	// Initialize Logging
	protected static Logger log = LoggerFactory.getLogger(Server.class);

	/**
	 * Executor service used to provide asynchronous notifications.
	 */
	private static ExecutorService notifier;
	
	/**
	 * List of global scopes
	 */
	protected ConcurrentMap<String, IGlobalScope> globals = new ConcurrentHashMap<String, IGlobalScope>();

	/**
	 * Mappings
	 */
	protected ConcurrentMap<String, String> mapping = new ConcurrentHashMap<String, String>();

	/**
	 * Spring application context
	 */
	protected ApplicationContext applicationContext;

	/**
	 * Constant for slash
	 */
	protected static final String SLASH = "/";

	/**
	 * Constant for empty string
	 */
	protected static final String EMPTY = "";

	public Set<IScopeListener> scopeListeners = new CopyOnWriteArraySet<IScopeListener>();

	public Set<IConnectionListener> connectionListeners = new CopyOnWriteArraySet<IConnectionListener>();

	//number of threads in the notifier pool
	private int notifierThreadPoolSize = 4;

	/**
	 * Setter for Spring application context
	 * 
	 * @param applicationContext Application context
	 */
	public void setApplicationContext(ApplicationContext applicationContext) {
		log.debug("Setting application context");
		this.applicationContext = applicationContext;
	}

	/**
	 * Initialization section.
	 */
	public void afterPropertiesSet() throws Exception {
		Server.notifier = Executors.newFixedThreadPool(notifierThreadPoolSize);
	}

	/**
	 * Destruction section.
	 */
	public void destroy() throws Exception {
		//disable new tasks from being submitted
		notifier.shutdown(); 
		try {
			//wait a while for existing tasks to terminate
			if (!notifier.awaitTermination(3, TimeUnit.SECONDS)) {
				notifier.shutdownNow(); // cancel currently executing tasks
				//wait a while for tasks to respond to being canceled
				if (!notifier.awaitTermination(3, TimeUnit.SECONDS)) {
					System.err.println("Notifier pool did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			// re-cancel if current thread also interrupted
			notifier.shutdownNow();
			// preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}	
	
	/**
	 * Return scope key. Scope key consists of host name concatenated with
	 * context path by slash symbol
	 * 
	 * @param hostName Host name
	 * @param contextPath Context path
	 * @return Scope key as string
	 */
	protected String getKey(String hostName, String contextPath) {
		if (hostName == null) {
			hostName = EMPTY;
		}
		if (contextPath == null) {
			contextPath = EMPTY;
		}
		return hostName + SLASH + contextPath;
	}

	/**
	 * Does global scope lookup for host name and context path
	 * 
	 * @param hostName Host name
	 * @param contextPath Context path
	 * @return Global scope
	 */
	public IGlobalScope lookupGlobal(String hostName, String contextPath) {
		log.trace("{}", this);
		log.debug("Lookup global scope - host name: {} context path: {}", hostName, contextPath);
		// Init mappings key
		String key = getKey(hostName, contextPath);
		// If context path contains slashes get complex key and look for it
		// in mappings
		while (contextPath.indexOf(SLASH) != -1) {
			key = getKey(hostName, contextPath);
			log.trace("Check: {}", key);
			String globalName = mapping.get(key);
			if (globalName != null) {
				return getGlobal(globalName);
			}
			final int slashIndex = contextPath.lastIndexOf(SLASH);
			// Context path is substring from the beginning and till last slash
			// index
			contextPath = contextPath.substring(0, slashIndex);
		}

		// Get global scope key
		key = getKey(hostName, contextPath);
		log.trace("Check host and path: {}", key);

		// Look up for global scope switching keys if still not found
		String globalName = mapping.get(key);
		if (globalName != null) {
			return getGlobal(globalName);
		}
		key = getKey(EMPTY, contextPath);
		log.trace("Check wildcard host with path: {}", key);
		globalName = mapping.get(key);
		if (globalName != null) {
			return getGlobal(globalName);
		}
		key = getKey(hostName, EMPTY);
		log.trace("Check host with no path: {}", key);
		globalName = mapping.get(key);
		if (globalName != null) {
			return getGlobal(globalName);
		}
		key = getKey(EMPTY, EMPTY);
		log.trace("Check default host, default path: {}", key);
		return getGlobal(mapping.get(key));
	}

	/**
	 * Return global scope by name
	 * 
	 * @param name Global scope name
	 * @return Global scope
	 */
	public IGlobalScope getGlobal(String name) {
		if (name == null) {
			return null;
		}
		return globals.get(name);
	}

	/**
	 * Register global scope
	 * 
	 * @param scope Global scope to register
	 */
	public void registerGlobal(IGlobalScope scope) {
		log.trace("Registering global scope: {}", scope.getName(), scope);
		globals.put(scope.getName(), scope);
	}

	/**
	 * Map key (host + / + context path) and global scope name
	 * 
	 * @param hostName Host name
	 * @param contextPath Context path
	 * @param globalName Global scope name
	 * @return true if mapping was added, false if already exist
	 */
	public boolean addMapping(String hostName, String contextPath, String globalName) {
		log.info("Add mapping global: {} host: {} context: {}", new Object[] { globalName, hostName, contextPath });
		final String key = getKey(hostName, contextPath);
		log.debug("Add mapping: {} => {}", key, globalName);
		return (mapping.putIfAbsent(key, globalName) == null);
	}

	/**
	 * Remove mapping with given key
	 * 
	 * @param hostName Host name
	 * @param contextPath Context path
	 * @return true if mapping was removed, false if key doesn't exist
	 */
	public boolean removeMapping(String hostName, String contextPath) {
		log.info("Remove mapping host: {} context: {}", hostName, contextPath);
		final String key = getKey(hostName, contextPath);
		log.debug("Remove mapping: {}", key);
		return (mapping.remove(key) != null);
	}

	/**
	 * Remove all mappings with given context path
	 * 
	 * @param contextPath Context path
	 * @return true if mapping was removed, false if key doesn't exist
	 */
	public boolean removeMapping(String contextPath) {
		log.info("Remove mapping context: {}", contextPath);
		final String key = getKey("", contextPath);
		log.debug("Remove mapping: {}", key);
		return (mapping.remove(key) != null);
	}	
	
	/**
	 * Return mapping
	 * 
	 * @return Map of "scope key / scope name" pairs
	 */
	public Map<String, String> getMappingTable() {
		return mapping;
	}

	/**
	 * Return global scope names set iterator
	 * 
	 * @return Iterator
	 */
	public Iterator<String> getGlobalNames() {
		return globals.keySet().iterator();
	}

	/**
	 * Return global scopes set iterator
	 * 
	 * @return Iterator
	 */
	public Iterator<IGlobalScope> getGlobalScopes() {
		return globals.values().iterator();
	}

	/**
	 * String representation of server
	 * 
	 * @return String representation of server
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this).append(mapping).toString();
	}

	/** {@inheritDoc} */
	public void addListener(IScopeListener listener) {
		scopeListeners.add(listener);
	}

	/** {@inheritDoc} */
	public void addListener(IConnectionListener listener) {
		connectionListeners.add(listener);
	}

	/** {@inheritDoc} */
	public void removeListener(IScopeListener listener) {
		scopeListeners.remove(listener);
	}

	/** {@inheritDoc} */
	public void removeListener(IConnectionListener listener) {
		connectionListeners.remove(listener);
	}

	/**
	 * Notify listeners about a newly created scope.
	 * 
	 * @param scope
	 *            the scope that was created
	 */
	protected void notifyScopeCreated(final IScope scope) {
		Runnable notification = new Runnable(){
			public void run() {				
        		for (IScopeListener listener : scopeListeners) {
        			listener.notifyScopeCreated(scope);
        		}
			}
		};
		notifier.execute(notification);				
	}

	/**
	 * Notify listeners that a scope was removed.
	 * 
	 * @param scope
	 *            the scope that was removed
	 */
	protected void notifyScopeRemoved(final IScope scope) {
		Runnable notification = new Runnable(){
			public void run() {		
        		for (IScopeListener listener : scopeListeners) {
        			listener.notifyScopeRemoved(scope);
        		}
			}
		};
		notifier.execute(notification);		
	}

	/**
	 * Notify listeners that a new connection was established.
	 * 
	 * @param conn
	 *            the new connection
	 */
	protected void notifyConnected(final IConnection conn) {
		Runnable notification = new Runnable(){
			public void run() {
				for (IConnectionListener listener : connectionListeners) {
					listener.notifyConnected(conn);
				}				
			}
		};
		notifier.execute(notification);
	}

	/**
	 * Notify listeners that a connection was disconnected.
	 * 
	 * @param conn
	 *            the disconnected connection
	 */
	protected void notifyDisconnected(final IConnection conn) {
		Runnable notification = new Runnable(){
			public void run() {
        		for (IConnectionListener listener : connectionListeners) {
        			listener.notifyDisconnected(conn);
        		}
        	}
        };
        notifier.execute(notification);		
	}

	public int getNotifierThreadPoolSize() {
		return notifierThreadPoolSize;
	}

	public void setNotifierThreadPoolSize(int notifierThreadPoolSize) {
		this.notifierThreadPoolSize = notifierThreadPoolSize;
	}

}
