package org.red5.server;

import org.red5.server.api.IConnection;

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


/**
 * Client is an abstraction representing user connected to Red5 application.
 * Clients are tied to connections and registred in ClientRegistry
 */
public class DummyClient extends Client {
	
	private IConnection conn;
	
	public DummyClient(String id, ClientRegistry registry) {
		super(id, registry);
	}
	
	public void registerConnection(IConnection conn) {
		this.conn = conn;
		register(this.conn);
	}
	
	public void unregisterConnection(IConnection conn) {
		unregister(conn);
		disconnect();
	}
	
}