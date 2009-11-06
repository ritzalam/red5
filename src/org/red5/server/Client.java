package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.persistence.IPersistable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client is an abstraction representing user connected to Red5 application.
 * Clients are tied to connections and registred in ClientRegistry
 */
public class Client extends AttributeStore implements IClient {
	/**
	 *  Logger
	 */
	protected static Logger log = LoggerFactory.getLogger(Client.class);

	/**
	 * Name of connection attribute holding the permissions.
	 */
	protected static final String PERMISSIONS = IPersistable.TRANSIENT_PREFIX + "_red5_permissions";

	/**
	 *  Scopes this client connected to
	 */
	protected ConcurrentMap<IConnection, IScope> connToScope = new ConcurrentHashMap<IConnection, IScope>();

	/**
	 *  Creation time as Timestamp
	 */
	protected long creationTime;

	/**
	 *  Clients identifier
	 */
	protected String id;

	/**
	 * MBean object name used for de/registration purposes.
	 */
	//private ObjectName oName;

	/**
	 *  Client registry where Client is registered
	 */
	protected ClientRegistry registry;

	public Client() {
		//here for jmx reference only
		log.trace("Default ctor called");
	}

	/**
	 * Creates client, sets creation time and registers it in ClientRegistry
	 *
	 * @param id             Client id
	 * @param registry       ClientRegistry
	 */
	public Client(String id, ClientRegistry registry) {
		this.id = id;
		this.registry = registry;
		this.creationTime = System.currentTimeMillis();
		//create a new mbean for this instance
		//oName = JMXFactory.createObjectName("type", "Client", "id", id);
		//JMXAgent.registerMBean(this, this.getClass().getName(), ClientMBean.class, oName);
	}

	/**
	 *  Disconnects client from Red5 application
	 */
	public void disconnect() {
		log.debug("Disconnect - id: {}, closing {} connections", id, getConnections().size());
		// Close all associated connections
		for (IConnection con : getConnections()) {
			con.close();
		}
		connToScope.clear();
	}

	/**
	 * Check clients equality by id
	 *
	 * @param obj        Object to check against
	 * @return           true if clients ids are the same, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Client)) {
			return false;
		}
		return ((Client) obj).getId().equals(id);
	}

	/**
	 * Return set of connections for this client
	 *
	 * @return           Set of connections
	 */
	public Set<IConnection> getConnections() {
		return connToScope.keySet();
	}

	/**
	 * Return client connections to given scope
	 *
	 * @param scope           Scope
	 * @return                Set of connections for that scope
	 */
	public Set<IConnection> getConnections(IScope scope) {
		if (scope == null) {
			return getConnections();
		}
		Set<IConnection> result = new HashSet<IConnection>(connToScope.size());
		for (Entry<IConnection, IScope> entry : connToScope.entrySet()) {
			if (scope.equals(entry.getValue())) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	/**
	 *
	 * @return creation time
	 */
	public long getCreationTime() {
		return creationTime;
	}

	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the client id
	 * @return client id
	 */
	public String getId() {
		return id;
	}

	/**
	 *
	 * @return scopes on this client
	 */
	public Collection<IScope> getScopes() {
		return connToScope.values();
	}

	/**
	 * if overriding equals then also do hashCode
	 * @return a has code
	 */
	@Override
	public int hashCode() {
		return Integer.valueOf(id);
	}

	/**
	 * Iterate through the scopes and their attributes.
	 * Used by JMX
	 *
	 * @return list of scope attributes
	 */
	public List<String> iterateScopeNameList() {
		log.debug("iterateScopeNameList called");
		int scopeCount = connToScope.values().size();
		List<String> scopeNames = new ArrayList<String>(scopeCount);
		log.debug("Scopes: {}", scopeCount);
		for (IScope scope : connToScope.values()) {
			log.debug("Client scope: {}", scope);
			for (Map.Entry<String, Object> entry : scope.getAttributes().entrySet()) {
				log.debug("Client scope attr: {} = {}", entry.getKey(), entry.getValue());
			}
		}
		return scopeNames;
	}

	/**
	 * Associate connection with client
	 * @param conn         Connection object
	 */
	protected void register(IConnection conn) {
		log.debug("Registering connection for this client {}", id);
		if (conn != null) {
			IScope scp =  conn.getScope();
			if (scp != null) {
				connToScope.put(conn, scp);
			} else {
				log.warn("Clients scope is null. Id: {}", id);
			}
		} else {
			log.warn("Clients connection is null. Id: {}", id);
		}
	}

	/**
	 *
	 * @return string representation of client
	 */
	@Override
	public String toString() {
		return "Client: " + id;
	}

	/**
	 * Removes client-connection association for given connection
	 * @param conn         Connection object
	 */
	protected void unregister(IConnection conn) {
		// Remove connection from connected scopes list
		connToScope.remove(conn);
		// If client is not connected to any scope any longer then remove
		if (connToScope.isEmpty()) {
			if (registry != null) {
				// This client is not connected to any scopes, remove from registry.
				registry.removeClient(this);
				registry = null;
			}
			// deregister with jmx
			//JMXAgent.unregisterMBean(oName);
		}
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	public Collection<String> getPermissions(IConnection conn) {
		Collection<String> result = (Collection<String>) conn.getAttribute(PERMISSIONS);
		if (result == null) {
			result = Collections.EMPTY_SET;
		}
		return result;
	}

	/** {@inheritDoc} */
	public boolean hasPermission(IConnection conn, String permissionName) {
		final Collection<String> permissions = getPermissions(conn);
		return permissions.contains(permissionName);
	}

	/** {@inheritDoc} */
	public void setPermissions(IConnection conn, Collection<String> permissions) {
		if (permissions == null) {
			conn.removeAttribute(PERMISSIONS);
		} else {
			conn.setAttribute(PERMISSIONS, permissions);
		}
	}
}
