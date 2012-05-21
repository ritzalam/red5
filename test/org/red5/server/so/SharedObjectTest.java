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

package org.red5.server.so;

import static junit.framework.Assert.assertTrue;

import java.util.Set;

import junit.framework.TestCase;

import org.junit.Test;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.scope.WebScope;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * This is for testing SharedObject issues.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ContextConfiguration(locations = { "SharedObjectTest.xml" })
public class SharedObjectTest extends AbstractJUnit4SpringContextTests {

	protected static Logger log = LoggerFactory.getLogger(SharedObjectTest.class);

	private static WebScope appScope;
	
	@SuppressWarnings("unused")
	private String host = "localhost";
	
	@SuppressWarnings("unused")
	private String appPath = "junit";
	
	@SuppressWarnings("unused")
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
	public void testSharedObject() {
		log.debug("testSharedObject");
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			log.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		SOApplication app = (SOApplication) applicationContext.getBean("web.handler");
		String soName = "foo";

		//Room 1
		// /default/junit/room1
		TestCase.assertTrue(appScope.createChildScope("room1"));
		IScope room1 = appScope.getScope("room1");
		log.debug("Room 1: {}", room1);
		assertTrue(room1.getDepth() == 2);

		// get the SO
		ISharedObject sharedObject = app.getSharedObject(room1, soName, true);
		log.debug("SO: {}", sharedObject);
		assertTrue(sharedObject != null);		

		log.debug("testSharedObject-end");
	}

	@Test
	public void testGetSONames() throws Exception {
		log.debug("testGetSONames");
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			log.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
		log.debug("Room 1 scope: {}", room1);
		Set<String> names = room1.getScopeNames();
		log.debug("Names: {}", names);
		assertTrue(names.size() > 0);
		log.debug("testGetSONames-end");
	}

	@Test
	public void testRemoveSO() throws Exception {
		log.debug("testRemoveSO");
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			log.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		String soName = "foo";
		IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
		room1.removeChildren();
		log.debug("Child exists: {}", room1.hasChildScope(soName));
		
		log.debug("testRemoveSO-end");
	}

	/**
	 * Test for Issue 209
	 * http://code.google.com/p/red5/issues/detail?id=209
	 */
	@Test
	public void testPersistentCreation() throws Exception {
		log.debug("testPersistentCreation");
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			log.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		SOApplication app = (SOApplication) applicationContext.getBean("web.handler");
		String soName = "foo";
		// get our room
		IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
		// create the SO
		app.createSharedObject(room1, soName, true);
		// get the SO
		ISharedObject sharedObject = app.getSharedObject(room1, soName, true);
		assertTrue(sharedObject != null);		
		log.debug("testPersistentCreation-end");
	}

}
