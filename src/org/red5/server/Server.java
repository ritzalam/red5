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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.style.ToStringCreator;

/**
 * Red5 server core class implementation.
 */
public class Server implements IServer, ApplicationContextAware {

	// Initialize Logging
	protected static Log log = LogFactory.getLog(Server.class.getName());
    /**
     *  List of global scopes
     */
	protected ConcurrentHashMap<String, IGlobalScope> globals = new ConcurrentHashMap<String, IGlobalScope>();
    /**
     * Mappings
     */
	protected ConcurrentHashMap<String, String> mapping = new ConcurrentHashMap<String, String>();
    /**
     *  Spring application context
     */
	protected ApplicationContext applicationContext;
    /**
     *  Constant for slash
     */
	private static final String SLASH = "/";
    /**
     *  Constant for empty string
     */
	private static final String EMPTY = "";

    /**
     * Setter for Spring application context
     * @param applicationContext     Application context
     */
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

    /**
     * Return scope key. Scope key consists of host name concatenated with context path by slash symbol
     *
     * @param hostName           Host name
     * @param contextPath        Context path
     * @return                   Scope key as string
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
     * @param hostName              Host name
     * @param contextPath           Context path
     * @return                      Global scope
     */
	public IGlobalScope lookupGlobal(String hostName, String contextPath) {
        // Init mappings key
        String key = getKey(hostName, contextPath);
        // If context path contains slashes get complex key and look up for it in mappings
        while (contextPath.indexOf(SLASH) != -1) {
			key = getKey(hostName, contextPath);
			if (log.isDebugEnabled()) {
				log.debug("Check: " + key);
			}
			if (mapping.containsKey(key)) {
				return getGlobal(mapping.get(key));
			}
            final int slashIndex = contextPath.lastIndexOf(SLASH);
            // Context path is substring from the beginning and till last slash index
            contextPath = contextPath.substring( 0, slashIndex );
		}

        // Get global scope key
        key = getKey(hostName, contextPath);
		if (log.isDebugEnabled()) {
			log.debug("Check host and path: " + key);
		}

        // Look up for global scope switching keys if still not found
        if (mapping.containsKey(key)) {
			return getGlobal(mapping.get(key));
		}
		key = getKey(EMPTY, contextPath);
		if (log.isDebugEnabled()) {
			log.debug("Check wildcard host with path: " + key);
		}
		if (mapping.containsKey(key)) {
			return getGlobal(mapping.get(key));
		}
		key = getKey(hostName, EMPTY);
		if (log.isDebugEnabled()) {
			log.debug("Check host with no path: " + key);
		}
		if (mapping.containsKey(key)) {
			return getGlobal(mapping.get(key));
		}
		key = getKey(EMPTY, EMPTY);
		if (log.isDebugEnabled()) {
			log.debug("Check default host, default path: " + key);
		}
		return getGlobal(mapping.get(key));
	}

    /**
     * Return global scope by name
     *
     * @param name       Global scope name
     * @return           Global scope
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
     * @param scope       Global scope to register
     */
	public void registerGlobal(IGlobalScope scope) {
		log.info("Registering global scope: " + scope.getName());
		globals.put(scope.getName(), scope);
	}

    /**
     * Map key (host + / + context path) and global scope name
     *
     * @param hostName          Host name
     * @param contextPath       Context path
     * @param globalName        Global scope name
     * @return                  true if mapping was added, false if already exist
     */
	public boolean addMapping(String hostName, String contextPath,
			String globalName) {
		final String key = getKey(hostName, contextPath);
		if (log.isDebugEnabled()) {
			log.debug("Add mapping: " + key + " => " + globalName);
		}
		if (mapping.containsKey(key)) {
			return false;
		}
		mapping.put(key, globalName);
		return true;
	}

    /**
     * Remove mapping with given key
     *
     * @param hostName              Host name
     * @param contextPath           Context path
     * @return                      true if mapping was removed, false if key doesn't exist
     */
	public boolean removeMapping(String hostName, String contextPath) {
		final String key = getKey(hostName, contextPath);
		if (log.isDebugEnabled()) {
			log.debug("Remove mapping: " + key);
		}
		if (!mapping.containsKey(key)) {
			return false;
		}
		mapping.remove(key);
		return true;
	}

    /**
     * Return mapping
     *
     * @return             Map of "scope key / scope name" pairs
     */
	public Map<String, String> getMappingTable() {
		return mapping;
	}

    /**
     * Return global scope names set iterator
     *
     * @return           Iterator
     */
	public Iterator<String> getGlobalNames() {
		return globals.keySet().iterator();
	}

    /**
     * Return global scopes set iterator
     *
     * @return           Iterator
     */
	public Iterator<IGlobalScope> getGlobalScopes() {
		return globals.values().iterator();
	}

    /**
     * String representation of server
     *
     * @return            String representation of server
     */
	@Override
	public String toString() {
		return new ToStringCreator(this).append(mapping).toString();
	}

}
