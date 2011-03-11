package org.red5.server;

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
		IScope room5 = ScopeUtils.resolveScope(appScope, "/junit/room1/room4/room5");
		Iterator<String> names = room1.getScopeNames();
		while (names.hasNext()) {
			log.debug("Scope: {}", names.next());
		}
	}

}
