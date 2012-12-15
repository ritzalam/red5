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

package org.red5.server.scope;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.NoSuchElementException;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.Test;
import org.red5.server.Context;
import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.TestConnection;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * This is for testing Scope issues. First created to address:
 * http://jira.red5.org/browse/APPSERVER-278
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ContextConfiguration(locations = { "ScopeTest.xml" })
public class ScopeTest extends AbstractJUnit4SpringContextTests {

	protected static Logger log = LoggerFactory.getLogger(ScopeTest.class);

	private static Context context;

	private static WebScope appScope;

	private String host = "localhost";

	private String appPath = "junit";

	private String roomPath = "/junit/room1";

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "bin");
		System.setProperty("red5.config_root", "bin/conf");
		System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
	}

	{
		log.debug("Property - user.dir: {}", System.getProperty("user.dir"));
		log.debug("Property - red5.root: {}", System.getProperty("red5.root"));
		log.debug("Property - red5.config_root: {}", System.getProperty("red5.config_root"));
	}

	@Test
	public void client() {
		context = (Context) applicationContext.getBean("web.context");
		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);
		assertTrue("client should not be null", client != null);
	}

	@Test
	public void connectionHandler() {
		context = (Context) applicationContext.getBean("web.context");
		TestConnection conn = new TestConnection(host, "/", null);
		// add the connection to thread local
		Red5.setConnectionLocal(conn);
		// resolve root
		IScope scope = context.resolveScope("/");
		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);
		assertNotNull(client);

		log.debug("----------------------------------\nDump scope details");
		((Scope) scope).dump();
		log.debug("----------------------------------\n");

		conn.initialize(client);
		if (!conn.connect(scope)) {
			assertTrue("didnt connect", false);
		} else {
			assertTrue("should have a scope", conn.getScope() != null);
			conn.close();
			assertTrue("should not be connected", !conn.isConnected());
		}
		Red5.setConnectionLocal(null);
	}

	@Test
	public void context() {
		context = (Context) applicationContext.getBean("web.context");
		IScope testRoom = context.resolveScope(roomPath);
		IContext context = testRoom.getContext();
		assertTrue("context should not be null", context != null);
		log.debug("{}", testRoom.getContext().getResource(""));
		log.debug("{}", testRoom.getResource(""));
		log.debug("{}", testRoom.getParent().getResource(""));
	}

	@Test
	public void handler() {
		context = (Context) applicationContext.getBean("web.context");

		Scope testApp = (Scope) context.resolveScope(appPath);
		assertTrue("should have a handler", testApp.hasHandler());

		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);

		TestConnection conn = new TestConnection(host, appPath, client.getId());
		conn.initialize(client);

		Red5.setConnectionLocal(conn);

		assertTrue("client should not be null", client != null);
		log.debug("{}", client);

		String key = "key";
		String value = "value";
		client.setAttribute(key, value);
		assertTrue("attributes not working", client.getAttribute(key) == value);

		assertTrue(conn.connect(testApp));

		assertTrue("app should have 1 client", testApp.getClients().size() == 1);
		assertTrue("host should have 1 client", testApp.getParent().getClients().size() == 1);

		conn.close();

		assertTrue("app should have 0 client", testApp.getClients().size() == 0);
		assertTrue("host should have 0 client", testApp.getParent().getClients().size() == 0);

		//client.disconnect();
		Red5.setConnectionLocal(null);
	}

	@Test
	public void scopeResolver() {
		context = (Context) applicationContext.getBean("web.context");

		// Global
		IScope global = context.getGlobalScope();
		assertNotNull("global scope should be set", global);
		assertTrue("should be global", ScopeUtils.isGlobal(global));
		log.debug("{}", global);

		// Test App
		IScope testApp = context.resolveScope(appPath);
		assertTrue("testApp scope not null", testApp != null);
		log.debug("{}", testApp);

		// Test Room
		IScope testRoom = context.resolveScope(roomPath);
		log.debug("{}", testRoom);

		// Test App Not Found
		try {
			IScope notFoundApp = context.resolveScope(appPath + "notfound");
			log.debug("{}", notFoundApp);
			assertTrue("should have thrown an exception", false);
		} catch (RuntimeException e) {
		}
	}

	@Test
	public void testScope() {
		log.debug("testScope");
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			log.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		//Room 1
		// /default/junit/room1
		TestCase.assertTrue(appScope.createChildScope("room1"));
		IScope room1 = appScope.getScope("room1");
		log.debug("Room 1: {}", room1);
		assertTrue(room1.getDepth() == 2);

		IContext rmCtx1 = room1.getContext();
		log.debug("Context 1: {}", rmCtx1);

		//Room 2
		// /default/junit/room1/room2
		TestCase.assertTrue(room1.createChildScope("room2"));
		IScope room2 = room1.getScope("room2");
		log.debug("Room 2: {}", room2);
		assertTrue(room2.getDepth() == 3);

		IContext rmCtx2 = room2.getContext();
		log.debug("Context 2: {}", rmCtx2);

		//Room 3
		// /default/junit/room1/room2/room3
		TestCase.assertTrue(room2.createChildScope("room3"));
		IScope room3 = room2.getScope("room3");
		log.debug("Room 3: {}", room3);
		assertTrue(room3.getDepth() == 4);

		IContext rmCtx3 = room3.getContext();
		log.debug("Context 3: {}", rmCtx3);

		//Room 4 attaches at Room 1 (per bug example)
		// /default/junit/room1/room4
		TestCase.assertTrue(room1.createChildScope("room4"));
		IScope room4 = room1.getScope("room4");
		log.debug("Room 4: {}", room4);
		assertTrue(room4.getDepth() == 3);

		IContext rmCtx4 = room4.getContext();
		log.debug("Context 4: {}", rmCtx4);

		//Room 5
		// /default/junit/room1/room4/room5
		TestCase.assertTrue(room4.createChildScope("room5"));
		IScope room5 = room4.getScope("room5");
		log.debug("Room 5: {}", room5);
		assertTrue(room5.getDepth() == 4);

		IContext rmCtx5 = room5.getContext();
		log.debug("Context 5: {}", rmCtx5);

		//Context ctx = new Context();
		//ctx.setApplicationContext(applicationContext);

		//Scope scope = new DummyScope();
		//scope.setName("");
		//scope.setContext(ctx);

		// Additional test section for issue #259

		// a little pre-setup is needed first
		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);
		TestConnection conn = new TestConnection(host, appPath, client.getId());
		conn.initialize(client);
		Red5.setConnectionLocal(conn);
		conn.connect(room5);
		// their code
		IScope scope = Red5.getConnectionLocal().getScope();
		for (Set<IConnection> collection : scope.getConnections()) {
			try {
				for (IConnection tempConn : collection) {
					if (tempConn instanceof IServiceCapableConnection) {
						@SuppressWarnings("unused")
						IServiceCapableConnection sc = (IServiceCapableConnection) tempConn;
						//sc.invoke(methodName, objArrays);
					}
				}
			} catch (NoSuchElementException e) {
				log.warn("Previous scope connection is unavailable", e);
			}
		}
	}

	@Test
	public void testGetScopeNames() throws Exception {
		log.debug("testGetScopeNames");
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			log.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
		log.debug("Room 1 scope: {}", room1);
		assertTrue(room1.getDepth() == 2);
		Set<String> names = room1.getScopeNames();
		log.debug("Scope: {}", names);
		IScope room5 = ScopeUtils.resolveScope(appScope, "/junit/room1/room4/room5");
		log.debug("Room 5 scope: {}", room5);
		assertTrue(room5.getDepth() == 4);
		names = room1.getScopeNames();
		log.debug("Scope: {}", names);
	}

	@Test
	public void testRemoveScope() throws Exception {
		log.debug("testRemoveScope");
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			log.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
		IScope room4 = ScopeUtils.resolveScope(appScope, "/junit/room1/room4");
		log.debug("Room 4 scope: {}", room4);
		assertTrue(room4.getDepth() == 3);
		log.debug("Room 4 child scope exists: {}", room1.hasChildScope("room4"));
		room1.removeChildScope(room4);
		log.debug("Room 4 child scope exists: {}", room1.hasChildScope("room4"));
	}

	/**
	 * Test for Issue 73
	 * http://code.google.com/p/red5/issues/detail?id=73
	 * 
	 */
	@Test
	public void testGetContextPath() throws Exception {
		log.debug("testGetContextPath");
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			log.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		log.debug("Context path: {}", appScope.getContextPath());
	}

}
