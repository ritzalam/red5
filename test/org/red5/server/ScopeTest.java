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

package org.red5.server;

import static org.junit.Assert.*;

import java.util.Iterator;

import junit.framework.TestCase;

import org.junit.Test;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.ScopeUtils;
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

	private static WebScope appScope;

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
		room5.setPersistent(true);
		log.debug("Room 5: {}", room5);
		assertTrue(room5.getDepth() == 4);

		IContext rmCtx5 = room5.getContext();
		log.debug("Context 5: {}", rmCtx5);

		//Context ctx = new Context();
		//ctx.setApplicationContext(applicationContext);

		//Scope scope = new DummyScope();
		//scope.setName("");
		//scope.setContext(ctx);

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
		Iterator<String> names = room1.getScopeNames();
		while (names.hasNext()) {
			log.debug("Scope: {}", names.next());
		}
		IScope room5 = ScopeUtils.resolveScope(appScope, "/junit/room1/room4/room5");
		log.debug("Room 5 scope: {}", room5);
		assertTrue(room5.getDepth() == 4);
		names = room1.getScopeNames();
		while (names.hasNext()) {
			log.debug("Scope: {}", names.next());
		}
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
