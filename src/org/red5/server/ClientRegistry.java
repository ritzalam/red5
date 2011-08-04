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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.lang3.StringUtils;
import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;
import org.red5.server.exception.ClientNotFoundException;
import org.red5.server.exception.ClientRejectedException;
import org.red5.server.jmx.JMXAgent;
import org.red5.server.jmx.JMXFactory;
import org.red5.server.jmx.mxbeans.ClientRegistryMXBean;

/**
 * Registry for clients. Associates client with it's id so it's possible to get client by id
 * from whenever we need.
 *
 * @author The Red5 Project (red5@osflash.org)
 */
public class ClientRegistry implements IClientRegistry, ClientRegistryMXBean {
	/**
	 * Clients map
	 */
	private ConcurrentMap<String, IClient> clients = new ConcurrentHashMap<String, IClient>();

	/**
	 *  Next client id
	 */
	private AtomicInteger nextId = new AtomicInteger();

	/**
	 * The identifier for this client registry
	 */
	private String name;

	public ClientRegistry() {
		JMXAgent.registerMBean(this, this.getClass().getName(), ClientRegistryMXBean.class);
	}

	//allows for setting a "name" to be used with jmx for lookup
	public ClientRegistry(String name) {
		this.name = name;
		if (StringUtils.isNotBlank(this.name)) {
			try {
				String className = JMXAgent.trimClassName(getClass().getName());
				ObjectName oName = new ObjectName(String.format("%s:type=%s,name=%s", JMXFactory.getDefaultDomain(), className, name));
				JMXAgent.registerMBean(this, getClass().getName(), ClientRegistryMXBean.class, oName);
			} catch (MalformedObjectNameException e) {
				//log.error("Invalid object name. {}", e);
			}
		}
	}

	/**
	 * Add client to registry
	 * @param client           Client to add
	 */
	protected void addClient(IClient client) {
		addClient(client.getId(), client);
	}

	/**
	 * Add the client to the registry
	 */
	private void addClient(String id, IClient client) {
		//check to see if the id already exists first
		if (!hasClient(id)) {
			clients.put(id, client);
		} else {
			// DW the Client object is meant to be unifying connections from a remote user. But currently the only case we
			// specify this currently is when we use a remoting session. So we actually just create an arbitrary id, which means
			// RTMP connections from same user are not combined.
			//get the next available client id
			String newId = nextId();
			//update the client
			client.setId(newId);
			//add the client to the list
			addClient(newId, client);
		}
	}

	public Client getClient(String id) throws ClientNotFoundException {
		Client result = (Client) clients.get(id);
		if (result == null) {
			throw new ClientNotFoundException(id);
		}
		return result;
	}

	/**
	 * Returns a list of Clients.
	 */
	public ClientList<Client> getClientList() {
		ClientList<Client> list = new ClientList<Client>();
		for (IClient c : clients.values()) {
			list.add((Client) c);
		}
		return list;
	}

	/**
	 * Check if client registry contains clients.
	 *
	 * @return             <code>True</code> if clients exist, otherwise <code>False</code>
	 */
	protected boolean hasClients() {
		return !clients.isEmpty();
	}

	/**
	 * Return collection of clients
	 * @return             Collection of clients
	 */
	@SuppressWarnings("unchecked")
	protected Collection<IClient> getClients() {
		if (!hasClients()) {
			// Avoid creating new Collection object if no clients exist.
			return Collections.EMPTY_SET;
		}
		return Collections.unmodifiableCollection(clients.values());
	}

	/**
	 * Check whether registry has client with given id
	 *
	 * @param id         Client id
	 * @return           true if client with given id was register with this registry, false otherwise
	 */
	public boolean hasClient(String id) {
		if (id == null) {
			// null ids are not supported
			return false;
		}
		return clients.containsKey(id);
	}

	/**
	 * Return client by id
	 *
	 * @param id          Client id
	 * @return            Client object associated with given id
	 * @throws ClientNotFoundException if we can't find client
	 */
	public IClient lookupClient(String id) throws ClientNotFoundException {
		return getClient(id);
	}

	/**
	 * Return client from next id with given params
	 *
	 * @param params                         Client params
	 * @return                               Client object
	 * @throws ClientNotFoundException if client not found
	 * @throws ClientRejectedException if client rejected
	 */
	public IClient newClient(Object[] params) throws ClientNotFoundException, ClientRejectedException {
		// DW I'm guessing perhaps that originally there was some idea to derive client id from the connection params?
		String id = nextId();
		IClient client = new Client(id, this);
		addClient(id, client);
		return client;
	}

	/**
	 * Return next client id
	 * @return         Next client id
	 */
	public String nextId() {
		//when we reach max int, reset to zero
		if (nextId.get() == Integer.MAX_VALUE) {
			nextId.set(0);
		}
		return String.format("%s", nextId.getAndIncrement());
	}

	/**
	 * Return previous client id
	 * @return        Previous client id
	 */
	public String previousId() {
		return String.format("%s", nextId.get());
	}

	/**
	 * Removes client from registry
	 * @param client           Client to remove
	 */
	protected void removeClient(IClient client) {
		clients.remove(client.getId());
	}

}
