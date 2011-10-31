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

import java.beans.ConstructorProperties;
import java.lang.ref.WeakReference;
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

import javax.management.openmbean.CompositeData;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.stream.bandwidth.ClientServerDetection;
import org.red5.server.stream.bandwidth.ServerClientDetection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client is an abstraction representing user connected to Red5 application.
 * Clients are tied to connections and registered in ClientRegistry
 */
public class Client extends AttributeStore implements IClient {

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
	 *  Client registry where Client is registered
	 */
	protected WeakReference<ClientRegistry> registry;

	/**
	 * Creates client, sets creation time and registers it in ClientRegistry
	 * DW: nope, does not currently register it in ClientRegistry!
	 *
	 * @param id             Client id
	 * @param registry       ClientRegistry
	 */
	@ConstructorProperties({ "id", "registry" })
	public Client(String id, ClientRegistry registry) {
		this.id = id;
		// use a weak reference to prevent any hard-links to the registry
		this.registry = new WeakReference<ClientRegistry>(registry);
		this.creationTime = System.currentTimeMillis();
	}

	/**
	 *  Disconnects client from Red5 application
	 */
	public void disconnect() {
		log.debug("Disconnect - id: {}, closing {} connections", id, getConnections().size());
		// close all connections held to Red5 by client
		for (IConnection con : getConnections()) {
			try {
				con.close();
			} catch (Exception e) {
				// closing a connection calls into application code, so exception possible
				log.error("Unexpected exception closing connection {}", e);
			}
		}
		// unregister client
		removeInstance();
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
	 * Sets the time at which the client was created.
	 * 
	 * @param creationTime
	 */
	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	/**
	 * Returns the time at which the client was created.
	 * 
	 * @return creation time
	 */
	public long getCreationTime() {
		return creationTime;
	}

	/**
	 * Sets the client id
	 */
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
			IScope scp = conn.getScope();
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
	 * Removes client-connection association for given connection
	 * @param conn         Connection object
	 */
	protected void unregister(IConnection conn) {
		unregister(conn, true);
	}

	/**
	 * Removes client-connection association for given connection
	 * @param conn         Connection object
	 * @param deleteIfNoConns Whether to delete this client if it no longer has any connections
	 */
	protected void unregister(IConnection conn, boolean deleteIfNoConns) {
		// Remove connection from connected scopes list
		connToScope.remove(conn);
		// If client is not connected to any scope any longer then remove
		if (deleteIfNoConns && connToScope.isEmpty()) {
			// TODO DW dangerous the way this is called from BaseConnection.initialize(). Could we unexpectedly pop a Client out of the registry?
			removeInstance();
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

	/** {@inheritDoc} */
	public void checkBandwidth() {
		//do something to check the bandwidth, Dan what do you think?
		ServerClientDetection detection = new ServerClientDetection();
		detection.checkBandwidth(Red5.getConnectionLocal());
	}

	/** {@inheritDoc} */
	public Map<String, Object> checkBandwidthUp(Object[] params) {
		//do something to check the bandwidth, Dan what do you think?
		ClientServerDetection detection = new ClientServerDetection();
		// if dynamic bw is turned on, we switch to a higher or lower
		return detection.checkBandwidth(params);
	}

	/**
	 * Allows for reconstruction via CompositeData.
	 *
	 * @param cd composite data
	 * @return Client class instance
	 */
	public static Client from(CompositeData cd) {
		Client instance = null;
		if (cd.containsKey("id")) {
			String id = (String) cd.get("id");
			instance = new Client(id, null);
			instance.setCreationTime((Long) cd.get("creationTime"));
			instance.setAttribute(PERMISSIONS, cd.get(PERMISSIONS));
		}
		return instance;
	}

	/**
	 * Removes this instance from the client registry.
	 */
	private void removeInstance() {
		// unregister client
		ClientRegistry ref = registry.get();
		if (ref != null) {
			ref.removeClient(this);
		} else {
			log.warn("Client registry reference was not accessable, removal failed");
			// TODO: attempt to lookup the registry via the global.clientRegistry
		}
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
	 *
	 * @return string representation of client
	 */
	@Override
	public String toString() {
		return "Client: " + id;
	}
	
}
