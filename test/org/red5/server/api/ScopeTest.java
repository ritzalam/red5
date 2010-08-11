package org.red5.server.api;

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

import static junit.framework.Assert.*;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.red5.server.Scope;

public class ScopeTest extends BaseTest {

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ScopeTest.class);
	}

	@Test
	public void client() {
		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);
		assertTrue("client should not be null", client != null);
	}

	@Test
	public void connectionHandler() {

		TestConnection conn = new TestConnection(host, "/", null);
		IScope scope = context.resolveScope("/");
		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);
		assertNotNull(client);
		conn.initialize(client);
		if (!conn.connect(scope)) {
			assertTrue("didnt connect", false);
		} else {
			assertTrue("should have a scope", conn.getScope() != null);
			conn.close();
			assertTrue("should not be connected", !conn.isConnected());
		}
	}

	@Test
	public void context() {
		IScope testRoom = context.resolveScope(path_room);
		IContext context = testRoom.getContext();
		assertTrue("context should not be null", context != null);
		log.debug("{}", testRoom.getContext().getResource(""));
		log.debug("{}", testRoom.getResource(""));
		log.debug("{}", testRoom.getParent().getResource(""));
	}

	@Test
	public void handler() {

		Scope testApp = (Scope) context.resolveScope(path_app);
		assertTrue("should have a handler", testApp.hasHandler());

		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);

		TestConnection conn = new TestConnection(host, path_app, client.getId());
		conn.initialize(client);

		assertTrue("client should not be null", client != null);
		log.debug("{}", client);

		String key = "key";
		String value = "value";
		client.setAttribute(key, value);
		assertTrue("attributes not working", client.getAttribute(key) == value);

		conn.connect(testApp);

		assertTrue("app should have 1 client", testApp.getClients().size() == 1);
		assertTrue("host should have 1 client", testApp.getParent()
				.getClients().size() == 1);

		conn.close();

		assertTrue("app should have 0 client", testApp.getClients().size() == 0);
		assertTrue("host should have 0 client", testApp.getParent()
				.getClients().size() == 0);

		//client.disconnect();
	}

	@Test
	public void scopeResolver() {

		// Global
		IScope global = context.getGlobalScope();
		assertNotNull("global scope should be set", global);
		assertTrue("should be global", ScopeUtils.isGlobal(global));
		log.debug("{}", global);

		// Test App
		IScope testApp = context.resolveScope(path_app);
		assertTrue("testApp scope not null", testApp != null);
		log.debug("{}", testApp);

		// Test Room
		IScope testRoom = context.resolveScope(path_room);
		log.debug("{}", testRoom);

		// Test App Not Found
		try {
			IScope notFoundApp = context.resolveScope(path_app + "notfound");
			log.debug("{}", notFoundApp);
			assertTrue("should have thrown an exception", false);
		} catch (RuntimeException e) {
		}

	}
}
