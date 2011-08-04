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

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.api.IGlobalScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test exercises the Server class. 
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ServerTest {

	protected static Logger log = LoggerFactory.getLogger(ServerTest.class);
	
	
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "bin");
		System.setProperty("red5.config_root", "bin/conf");
	}
	
	{
		log.debug("Property - user.dir: {}", System.getProperty("user.dir"));
		log.debug("Property - red5.root: {}", System.getProperty("red5.root"));
		log.debug("Property - red5.config_root: {}", System.getProperty("red5.config_root"));
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGlobalLookupsForVirtualHostsWithSameIP() {
		final Server server = new Server();

		IGlobalScope g0 = new DummyGlobalScope("default");
		IGlobalScope g1 = new DummyGlobalScope("default.vhost1");
		IGlobalScope g2 = new DummyGlobalScope("default.vhost2");
		
		//local server
		server.registerGlobal(g0);
		
		server.addMapping("localhost", "", "default");
		server.addMapping("localhost", "oflaDemo", "default");

		//virtual host 1
		
		server.registerGlobal(g1);

		server.addMapping("", "", "default.vhost1");
		server.addMapping("localhost", "oflaDemo", "default.vhost1");
		server.addMapping("localhost:8088", "", "default.vhost1");
		server.addMapping("127.0.0.1", "oflaDemo", "default.vhost1");
		//
		server.addMapping("vhost1.localdomain", "", "default.vhost1");
		server.addMapping("vhost1.localdomain", "oflaDemo", "default.vhost1");
		
		//virtual host 2
		
		server.registerGlobal(g2);

		server.addMapping("", "", "default.vhost2");
		server.addMapping("localhost", "oflaDemo", "default.vhost2");
		server.addMapping("localhost:8088", "", "default.vhost2");
		server.addMapping("127.0.0.1", "oflaDemo", "default.vhost2");
		//
		server.addMapping("vhost2.localdomain", "", "default.vhost2");
		server.addMapping("vhost2.localdomain", "oflaDemo", "default.vhost2");

		//assertions
		
		Assert.assertTrue(server.lookupGlobal("vhost2.localdomain", "blah") != null);
		Assert.assertTrue(server.lookupGlobal("vhost2.localdomain", "oflaDemo") != null);
		
		IGlobalScope tmp = server.lookupGlobal("vhost2.localdomain", "oflaDemo");
		log.debug("Global 2: {}", tmp);
		Assert.assertTrue(tmp.getName().equals("default.vhost2"));
		
		tmp = server.lookupGlobal("vhost1.localdomain", "oflaDemo");
		log.debug("Global 1: {}", tmp);
		Assert.assertTrue(tmp.getName().equals("default.vhost1"));

	}
	
	@Test
	public void testMultiThreaded() throws Throwable {

		int threads = 10;
		
		final Server server = new Server();

		IGlobalScope g0 = new DummyGlobalScope("default");
		
		//local server
		server.registerGlobal(g0);
		
		server.addMapping("localhost", "", "default");
		server.addMapping("localhost", "oflaDemo", "default");
		
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new HostAddWorker(server, t);
		}

		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

		//kickstarts the MTTR & fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		log.info("Runtime: {} ns", (System.nanoTime() - start));

		for (TestRunnable r : trs) {
			String name = ((HostAddWorker) r).getName();
			Assert.assertTrue(server.lookupGlobal(name + ".localdomain", "nonexistentscope") != null);
			IGlobalScope tmp = server.lookupGlobal(name + ".localdomain", "oflaDemo");
			Assert.assertTrue(tmp.getName().equals("default." + name));
		}		
	}
	
	private class HostAddWorker extends TestRunnable {
		
		Server server;
		String name;
		
		public HostAddWorker(Server server, int index) {
			this.server = server;
			this.name = "vhost" + index;
		}

		public void runTest() throws Throwable {
			IGlobalScope gs = new DummyGlobalScope("default." + name);

			server.registerGlobal(gs);
			
			for (int i = 0; i < 6; i++) {
				server.addMapping("", "", "default." + name);
				server.addMapping("localhost", "oflaDemo", "default." + name);
				server.addMapping("localhost:8088", "", "default." + name);
				server.addMapping("127.0.0.1", "oflaDemo", "default." + name);
				//
				server.addMapping(name + ".localdomain", "", "default." + name);
				server.addMapping(name + ".localdomain", "oflaDemo", "default." + name);				
			}
		}

		public String getName() {
			return name;
		}
	}
	
	
	private final static class DummyGlobalScope extends GlobalScope {
		public DummyGlobalScope(String name) {
			super();
			this.name = name;
		}
	}
	
}
