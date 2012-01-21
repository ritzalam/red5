/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.api;

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
