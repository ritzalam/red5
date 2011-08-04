package org.red5.server;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.red5.io.utils.RandomGUID;
import org.red5.server.api.IClient;
import org.red5.server.exception.ClientNotFoundException;
import org.red5.server.exception.ClientRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple registry for unit tests
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class DummyClientRegistry extends ClientRegistry {

	protected static Logger log = LoggerFactory.getLogger(DummyClientRegistry.class);

	private ConcurrentMap<String, IClient> clients = new ConcurrentHashMap<String, IClient>();

	@Override
	public boolean hasClient(String id) {
		return clients.containsKey(id);
	}

	@Override
	public IClient lookupClient(String id) throws ClientNotFoundException {
		return clients.get(id);
	}
	
	@Override
	public IClient newClient(Object[] params) throws ClientNotFoundException, ClientRejectedException {
		String id = null;
		if (params != null) {
			id = params[0].toString();
		} else {
			RandomGUID idGen = new RandomGUID();
			id = idGen.toString();
		}
		IClient client = new DummyClient(id, this);
		log.debug("New client: {}", client);		
		//add it
		clients.put(client.getId(), client);
		return client;
	}

}