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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;
import org.red5.server.exception.ClientNotFoundException;
import org.red5.server.exception.ClientRejectedException;

/**
 * Registry for clients. Associates client with it's id so it's possible to get client by id
 * from whenever we need
 */
public class ClientRegistry implements IClientRegistry {
    /**
     * Clients map
     */
	private HashMap<String, IClient> clients = new HashMap<String, IClient>();
    /**
     *  Next client id
     */
	private int nextId = 0;

    /**
     * Return next client id
     * @return         Next client id
     */
	public synchronized String nextId() {
		return "" + nextId++;
	}

    /**
     * Check whether registry has client with given id
     *
     * @param id         Client id
     * @return           true if client with given id was register with this registry, false otherwise
     */
	public boolean hasClient(String id) {
		return clients.containsKey(id);
	}

    /**
     * Return client by id
     *
     * @param id          Client id
     * @return            Client object associated with given id
     * @throws ClientNotFoundException
     */
	public IClient lookupClient(String id) throws ClientNotFoundException {
		if (!hasClient(id)) {
			throw new ClientNotFoundException(id);
		}

		return clients.get(id);
	}

    /**
     * Return client from next id with given params
     *
     * @param params                         Client params
     * @return                               Client object
     * @throws ClientNotFoundException
     * @throws ClientRejectedException
     */
	public IClient newClient(Object[] params) throws ClientNotFoundException,
			ClientRejectedException {
		IClient client = new Client(nextId(), this);
		addClient(client);
		return client;
	}

    /**
     * Add client to registry
     * @param client           Client to add
     */
	protected void addClient(IClient client) {
		clients.put(client.getId(), client);
	}

    /**
     * Removes client from registry
     * @param client           Client to remove
     */
	protected void removeClient(IClient client) {
		clients.remove(client.getId());
	}

    /**
     * Return collection of clients
     * @return             Collection of clients
     */
	protected Collection<IClient> getClients() {
		return Collections.unmodifiableCollection(clients.values());
	}
}
