package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IFlowControllable;
import org.red5.server.api.IScope;
import org.red5.server.stream.IFlowControlService;

/**
 * Client is an abstraction representing user connected to Red5 application.
 * Clients are tied to connections and registred in ClientRegistry
 */
public class Client extends AttributeStore implements IClient {
    /**
     *  Logger
     */
	protected static Log log = LogFactory.getLog(Client.class.getName());
    /**
     *  Clients identificator
     */
	protected String id;
    /**
     *  Creation time as Timestamp
     */
	protected long creationTime;
    /**
     *  Client registry where Client is registred
     */
	protected ClientRegistry registry;
    /**
     *  Scopes this client connected to
     */
	protected HashMap<IConnection, IScope> connToScope = new HashMap<IConnection, IScope>();
    /**
     *  Bandwith configuration context. For each connection server-side application may vary
     *  broadcasting quality preferences. These are stored in special object of type IBandwidthConfigure
     *
     *  @see  org.red5.server.api.stream.support.SimpleBandwidthConfigure
     */
	private IBandwidthConfigure bandwidthConfig;

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
	}

    /**
     *
     * @return
     */
	public String getId() {
		return id;
	}

    /**
     *
     * @return
     */
	public long getCreationTime() {
		return creationTime;
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
     * if overriding equals then also do hashCode
     * @return
     */
    @Override
	public int hashCode() {
		return id.hashCode();
	}

    /**
     *
     * @return
     */
	@Override
	public String toString() {
		return "Client: " + id;
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

		Set<IConnection> result = new HashSet<IConnection>();
		for (Entry<IConnection, IScope> entry : connToScope.entrySet()) {
			if (scope.equals(entry.getValue())) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

    /**
     *
     * @return
     */
	public Collection<IScope> getScopes() {
		return connToScope.values();
	}

    /**
     *  Disconnects client from Red5 application
     */
	public void disconnect() {
		if (log.isDebugEnabled()) {
			log.debug("Disconnect, closing " + getConnections().size()
				+ " connections");
		}

        // Close all associated connections
        Iterator<IConnection> conns = getConnections().iterator();
		while (conns.hasNext()) {
			conns.next().close();
		}
		IContext context = getContextFromConnection();
		if (context == null) {
			return;
		}

        // Release flow controllable
        IFlowControlService fcs = (IFlowControlService) context
				.getBean(IFlowControlService.KEY);
		fcs.releaseFlowControllable(this);
	}

    /**
     * Return bandwidth configuration context, that is, broadcasting bandwidth and quality settings for this client
     * @return      Bandwidth configuration context
     */
	public IBandwidthConfigure getBandwidthConfigure() {
		return this.bandwidthConfig;
	}

    /**
     * Parent flow controllable object, that is, parent object that is used to determine client broadcast bandwidth
     * settings. In case of base Client class parent is host.
     *
     * @return     IFlowControllable instance
     */
	public IFlowControllable getParentFlowControllable() {
		// parent is host
		return null;
	}

    /**
     * Set new bandwidth configuration context
     * @param config             Bandwidth configuration context
     */
	public void setBandwidthConfigure(IBandwidthConfigure config) {
		IContext context = getContextFromConnection();
		if (context == null) {
			return;
		}
		IFlowControlService fcs = (IFlowControlService) context
				.getBean(IFlowControlService.KEY);
		this.bandwidthConfig = config;
		fcs.updateBWConfigure(this);
	}

    /**
     * Associate connection with client
     * @param conn         Connection object
     */
	protected void register(IConnection conn) {
		connToScope.put(conn, conn.getScope());
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
			// This client is not connected to any scopes, remove from registry.
			registry.removeClient(this);
		}
	}

	/**
	 * Get the context from anyone of the IConnection.
	 *
	 * @return            Context
	 */
	private IContext getContextFromConnection() {
		IConnection conn = null;
		try {
			conn = connToScope.keySet().iterator().next();
		} catch (Exception e) {
			log.debug("getContextFromConnection caught Exception");
		}
		if (conn != null) {
			return conn.getScope().getContext();
		}
		return null;
	}

}
