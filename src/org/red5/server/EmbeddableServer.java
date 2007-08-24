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

import java.util.Iterator;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IScope;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.listeners.IScopeListener;
import org.springframework.context.ApplicationContext;
import org.springframework.core.style.ToStringCreator;

/**
 * Red5 server core class implementation for use in J2EE archive deployments (war, ear, sar etc).
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class EmbeddableServer extends Server {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(EmbeddableServer.class
			.getName());

	private static EmbeddableServer instance = null;

	private static InitialContext ctx = null;
	
	{
		try {
			if (ctx == null) {
				ctx = new InitialContext();
			}
            //test enum code
            log.debug("Binding list");
            try {
                NamingEnumeration enm = ctx.list("");
                while (enm.hasMore()) {
                    NameClassPair entry = (NameClassPair) enm.next();
                    log.debug("Enum entry: " + entry);
                }
            } catch (Exception e) {
                log.debug("Enum Error: " + e.getMessage());
            }			
            javax.naming.Context red5Ctx = (javax.naming.Context) ctx.lookup("red5");
			instance = (EmbeddableServer) red5Ctx.lookup("server");
			log.info("RED5 JNDI lookup server is null? " + (instance == null));
		} catch (NamingException e) {
			try {
				log.warn("RED5 JNDI lookup error");
				javax.naming.Context red5Ctx = ctx.createSubcontext("red5");
				instance = this;
				red5Ctx.rebind("server", instance);
			} catch (NamingException ne) {
				log.warn("RED5 JNDI subcontext creation error", ne);
			}
		}
	}

	private void getInstance() {
		try {
			if (ctx == null) {
				ctx = new InitialContext();
			}
            javax.naming.Context red5Ctx = (javax.naming.Context) ctx.lookup("red5");
			instance = (EmbeddableServer) red5Ctx.lookup("server");
			log.info("RED5 JNDI lookup server is null? (getInstance)" + (instance == null));
		} catch (NamingException e) {
			log.warn("RED5 JNDI lookup error");
		}
	}

	/**
	 * Setter for Spring application context
	 * 
	 * @param applicationContext
	 *            Application context
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (instance == null) {
			getInstance();
		}
		if (instance.applicationContext == null) {
			log.debug("Setting application context");
			instance.applicationContext = applicationContext;
		} else {
			log.info("Application context has already been set");
		}
	}

	/**
	 * Does global scope lookup for host name and context path
	 * 
	 * @param hostName
	 *            Host name
	 * @param contextPath
	 *            Context path
	 * @return Global scope
	 */
	@Override
	public IGlobalScope lookupGlobal(String hostName, String contextPath) {
		// Init mappings key
		String key = getKey(hostName, contextPath);
		// If context path contains slashes get complex key and look up for it
		// in mappings
		while (contextPath.indexOf(SLASH) != -1) {
			key = getKey(hostName, contextPath);
			if (log.isDebugEnabled()) {
				log.debug("Check: " + key);
			}
			if (instance.mapping.containsKey(key)) {
				return getGlobal(instance.mapping.get(key));
			}
			final int slashIndex = contextPath.lastIndexOf(SLASH);
			// Context path is substring from the beginning and till last slash
			// index
			contextPath = contextPath.substring(0, slashIndex);
		}

		// Get global scope key
		key = getKey(hostName, contextPath);
		if (log.isDebugEnabled()) {
			log.debug("Check host and path: " + key);
		}

		// Look up for global scope switching keys if still not found
		if (instance.mapping.containsKey(key)) {
			return getGlobal(instance.mapping.get(key));
		}
		key = getKey(EMPTY, contextPath);
		if (log.isDebugEnabled()) {
			log.debug("Check wildcard host with path: " + key);
		}
		if (instance.mapping.containsKey(key)) {
			return getGlobal(instance.mapping.get(key));
		}
		key = getKey(hostName, EMPTY);
		if (log.isDebugEnabled()) {
			log.debug("Check host with no path: " + key);
		}
		if (instance.mapping.containsKey(key)) {
			return getGlobal(instance.mapping.get(key));
		}
		key = getKey(EMPTY, EMPTY);
		if (log.isDebugEnabled()) {
			log.debug("Check default host, default path: " + key);
		}
		return getGlobal(instance.mapping.get(key));
	}

	/**
	 * Return global scope by name
	 * 
	 * @param name
	 *            Global scope name
	 * @return Global scope
	 */
	@Override
	public IGlobalScope getGlobal(String name) {
		if (name == null) {
			return null;
		}
		return instance.globals.get(name);
	}

	/**
	 * Register global scope
	 * 
	 * @param scope
	 *            Global scope to register
	 */
	@Override
	public void registerGlobal(IGlobalScope scope) {
		log.info("Registering global scope: " + scope.getName());
		instance.globals.put(scope.getName(), scope);
	}

	/**
	 * Map key (host + / + context path) and global scope name
	 * 
	 * @param hostName
	 *            Host name
	 * @param contextPath
	 *            Context path
	 * @param globalName
	 *            Global scope name
	 * @return true if mapping was added, false if already exist
	 */
	@Override
	public boolean addMapping(String hostName, String contextPath,
			String globalName) {
		log.info("Add mapping global: " + globalName + " host: " + hostName
				+ " context: " + contextPath);
		final String key = getKey(hostName, contextPath);
		if (log.isDebugEnabled()) {
			log.debug("Add mapping: " + key + " => " + globalName);
		}
		if (instance.mapping.containsKey(key)) {
			return false;
		}
		instance.mapping.put(key, globalName);
		return true;
	}

	/**
	 * Remove mapping with given key
	 * 
	 * @param hostName
	 *            Host name
	 * @param contextPath
	 *            Context path
	 * @return true if mapping was removed, false if key doesn't exist
	 */
	@Override
	public boolean removeMapping(String hostName, String contextPath) {
		log.info("Remove mapping host: " + hostName + " context: "
				+ contextPath);
		final String key = getKey(hostName, contextPath);
		if (log.isDebugEnabled()) {
			log.debug("Remove mapping: " + key);
		}
		if (!instance.mapping.containsKey(key)) {
			return false;
		}
		instance.mapping.remove(key);
		return true;
	}

	/**
	 * Return mapping
	 * 
	 * @return Map of "scope key / scope name" pairs
	 */
	@Override
	public Map<String, String> getMappingTable() {
		return instance.mapping;
	}

	/**
	 * Return global scope names set iterator
	 * 
	 * @return Iterator
	 */
	@Override
	public Iterator<String> getGlobalNames() {
		return instance.globals.keySet().iterator();
	}

	/**
	 * Return global scopes set iterator
	 * 
	 * @return Iterator
	 */
	@Override
	public Iterator<IGlobalScope> getGlobalScopes() {
		return instance.globals.values().iterator();
	}

	/**
	 * String representation of server
	 * 
	 * @return String representation of server
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this).append(instance.mapping).toString();
	}

	/** {@inheritDoc} */
	@Override
	public void addListener(IScopeListener listener) {
		instance.scopeListeners.add(listener);
	}

	/** {@inheritDoc} */
	@Override
	public void addListener(IConnectionListener listener) {
		instance.connectionListeners.add(listener);
	}

	/** {@inheritDoc} */
	@Override
	public void removeListener(IScopeListener listener) {
		instance.scopeListeners.remove(listener);
	}

	/** {@inheritDoc} */
	@Override
	public void removeListener(IConnectionListener listener) {
		instance.connectionListeners.remove(listener);
	}

	/**
	 * Notify listeners about a newly created scope.
	 * 
	 * @param scope
	 *            the scope that was created
	 */
	@Override
	protected void notifyScopeCreated(IScope scope) {
		for (IScopeListener listener : instance.scopeListeners) {
			listener.notifyScopeCreated(scope);
		}
	}

	/**
	 * Notify listeners that a scope was removed.
	 * 
	 * @param scope
	 *            the scope that was removed
	 */
	@Override
	protected void notifyScopeRemoved(IScope scope) {
		for (IScopeListener listener : instance.scopeListeners) {
			listener.notifyScopeRemoved(scope);
		}
	}

	/**
	 * Notify listeners that a new connection was established.
	 * 
	 * @param conn
	 *            the new connection
	 */
	@Override
	protected void notifyConnected(IConnection conn) {
		for (IConnectionListener listener : instance.connectionListeners) {
			listener.notifyConnected(conn);
		}
	}

	/**
	 * Notify listeners that a connection was disconnected.
	 * 
	 * @param conn
	 *            the disconnected connection
	 */
	@Override
	protected void notifyDisconnected(IConnection conn) {
		for (IConnectionListener listener : instance.connectionListeners) {
			listener.notifyDisconnected(conn);
		}
	}

}
