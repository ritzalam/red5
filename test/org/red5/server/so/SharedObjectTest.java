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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.Test;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectBase;
import org.red5.server.api.so.ISharedObjectListener;
import org.red5.server.scope.WebScope;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * This is for testing SharedObject issues.
 * 
 * http://help.adobe.com/en_US/FlashMediaServer/3.5_SS_ASD/WS5b3ccc516d4fbf351e63e3d11a11afc95e-7e63.html
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ContextConfiguration(locations = { "SharedObjectTest.xml" })
public class SharedObjectTest extends AbstractJUnit4SpringContextTests {

	protected static Logger log = LoggerFactory.getLogger(SharedObjectTest.class);

	private static WebScope appScope;
	
	private static TestRunnable[] trs;

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

	@Test
	public void testDeepDirty() throws Throwable {
		log.debug("testDeepDirty");
		SOApplication app = (SOApplication) applicationContext.getBean("web.handler");
		// get our room
		IScope room = ScopeUtils.resolveScope(appScope, "/junit/room1");
		// create the SO
		app.createSharedObject(room, "dirtySO", true);
		// test runnables represent clients
		trs = new TestRunnable[2];
		for (int t = 0; t < 2; t++) {
			trs[t] = new SOClientWorker(t, app, room);
		}
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

		// fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.println("Runtime: " + (System.nanoTime() - start) + "ns");

		// go to sleep
		try {
			Thread.sleep(3000);
		} catch (Exception e) {
		}

		for (TestRunnable r : trs) {
			SOClientWorker cl = (SOClientWorker) r;
			log.debug("Worker: {} shared object: {}", cl.getId(), cl.getSO().getAttributes());
		}

		log.debug("testDeepDirty-end");
	}
	
	// Used to ensure all the test-runnables are in "runTest" block.
	private static boolean allThreadsRunning() {
		for (TestRunnable r : trs) {
			if (!((SOClientWorker) r).isRunning()) {
				return false;
			}
		}
		return true;
	}

	private class SOClientWorker extends TestRunnable {

		private int id;

		private ISharedObject so;

		private volatile boolean running = false;
		
		public SOClientWorker(int id, SOApplication app, IScope room) {
			this.id = id;
			this.so = app.getSharedObject(room, "dirtySO", true);
			ISharedObjectListener listener = new MySOListener(id);
			so.addSharedObjectListener(listener);
		}

		public void runTest() throws Throwable {
			log.debug("runTest#{}", id);
			running = true;
			do {
				Thread.sleep(100);
			} while (!allThreadsRunning());
			// create complex type object
			Complex complex = (Complex) so.getAttribute("complex");
			if (complex == null) {
				complex = new Complex();
				complex.getMap().put("myId", id);
				so.setAttribute("complex", complex);
			}
			Thread.sleep(500);
			log.debug("runTest-end#{}", id);
			running = false;
		}

		public int getId() {
			return id;
		}

		public ISharedObject getSO() {
			return so;
		}
 
		public boolean isRunning() {
			return running;
		}
	}

	private class MySOListener implements ISharedObjectListener {

		private int id;

		public MySOListener(int id) {
			this.id = id;
		}

		@Override
		public void onSharedObjectConnect(ISharedObjectBase so) {
			log.trace("onSharedObjectConnect");
		}

		@Override
		public void onSharedObjectDisconnect(ISharedObjectBase so) {
			log.trace("onSharedObjectDisconnect");
		}

		@Override
		public void onSharedObjectUpdate(ISharedObjectBase so, String key, Object value) {
			log.trace("onSharedObjectUpdate - key: {} value: {}", key, value);
		}

		@Override
		public void onSharedObjectUpdate(ISharedObjectBase so, IAttributeStore values) {
			log.trace("onSharedObjectUpdate - values: {}", values);
		}

		@Override
		public void onSharedObjectUpdate(ISharedObjectBase so, Map<String, Object> values) {
			log.trace("onSharedObjectUpdate - values: {}", values);
		}

		@Override
		public void onSharedObjectDelete(ISharedObjectBase so, String key) {
			log.trace("onSharedObjectDelete");
		}

		@Override
		public void onSharedObjectClear(ISharedObjectBase so) {
			log.trace("onSharedObjectClear");
		}

		@Override
		public void onSharedObjectSend(ISharedObjectBase so, String method, List<?> params) {
			log.trace("onSharedObjectSend");
		}
	}

	private class Complex {
		
		private long x = System.currentTimeMillis();

		private String s = "Complex object";

		@SuppressWarnings("rawtypes")
		private Map map = new HashMap();

		public long getX() {
			return x;
		}

		public void setX(long x) {
			this.x = x;
		}

		public String getS() {
			return s;
		}

		public void setS(String s) {
			this.s = s;
		}

		public Map getMap() {
			return map;
		}

		public void setMap(Map map) {
			this.map = map;
		}

		@Override
		public String toString() {
			return "Complex [x=" + x + ", s=" + s + ", map=" + map + "]";
		}

	}

}
