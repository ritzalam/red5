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

import static org.junit.Assert.fail;
import junit.framework.Assert;
import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.Test;
import org.red5.server.api.IClient;
import org.red5.server.exception.ClientNotFoundException;

public class ClientRegistryTest {

	private static ClientRegistry reg;

	static {
		if (reg == null) {
			reg = new ClientRegistry();
		} 
	}

	@Test
	public void testNewClient() {
		IClient client = reg.newClient(null);
		Assert.assertNotNull(client);
		Assert.assertTrue(client.getId() != null);
		Assert.assertTrue(Integer.valueOf(client.getId()) >= 0);
	}

	@Test
	public void testAddClient() {
		reg.addClient(new Client(reg.nextId(), reg));
		Assert.assertNotNull(reg.getClient("1"));
		Assert.assertTrue(reg.getClients().size() >= 1);
	}

	@Test
	public void testLookupClient() {
		IClient client = reg.lookupClient("0");
		Assert.assertNotNull(client);
	}

	@Test
	public void testGetClient() {
		IClient client = reg.getClient("0");
		Assert.assertNotNull(client);

		IClient client2 = null;
		try {
			client2 = reg.getClient("999999");
			fail("An exception should occur here");
		} catch (ClientNotFoundException e) {
			Assert.assertTrue(true);
		}

		Assert.assertNull(client2);
	}

	@Test
	public void testGetClientList() {
		ClientList<Client> clients = reg.getClientList();
		int listSize = clients.size();
		Assert.assertTrue(listSize > 0);
		System.out.println("List size: " + listSize);
		for (int c = 0; c < listSize; c++) {
			Client client = clients.get(c);
			System.out.println(client);
			Assert.assertTrue(client.getId() != null);
		}

	}

	@Test
	public void testGetClients() {
		//create and add 10 clients
		for (int c = 0; c < 10; c++) {
			reg.addClient(new Client(reg.nextId(), reg));
		}
		Assert.assertNotNull(reg.getClient("2"));
		System.gc();
		try {
			Thread.sleep(2000);
			System.gc();
		} catch (InterruptedException e) {
		}
		Assert.assertTrue(reg.getClients().size() >= 10);
	}

	@Test
	public void testRemoveClient() {
		IClient client = reg.lookupClient("1");
		Assert.assertNotNull(client);

		reg.removeClient(client);

		IClient client2 = null;
		try {
			client2 = reg.getClient("1");
			fail("An exception should occur here");
		} catch (ClientNotFoundException e) {
			Assert.assertTrue(true);
		}

		Assert.assertNull(client2);
	}

	// this should run last or it may affect the other tests
	@Test
	public void testLifecycle() throws Throwable {
		int threads = 500;
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new ClientCreatorWorker();
		}
		Runtime rt = Runtime.getRuntime();
		long startFreeMem = rt.freeMemory();
		System.out.printf("Free mem: %s\n", startFreeMem);
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.printf("Runtime: %s ns\n", (System.nanoTime() - start));
		for (TestRunnable r : trs) {
			IClient cli = ((ClientCreatorWorker) r).getClient();
			Assert.assertTrue(cli == null);
		}
		System.gc();
		Thread.sleep(1000);
		System.out.printf("Free mem diff at end: %s\n", Math.abs(startFreeMem - rt.freeMemory()));
	}	
	
	private class ClientCreatorWorker extends TestRunnable {
		IClient client;

		public void runTest() throws Throwable {
			client = reg.newClient(null);
			String id = client.getId();
			client.setAttribute("time", System.currentTimeMillis());
			Thread.sleep(42);
			client.disconnect();
			Thread.sleep(42);
			try {
				client = reg.getClient(id);
			} catch (ClientNotFoundException e) {
				client = null;
			}
		}

		public IClient getClient() {
			return client;
		}

	}
}
