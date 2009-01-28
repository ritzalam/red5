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

import static org.junit.Assert.fail;
import junit.framework.Assert;

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

}
