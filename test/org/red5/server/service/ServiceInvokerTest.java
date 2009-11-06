package org.red5.server.service;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;
import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.Test;
import org.red5.server.Context;
import org.red5.server.DummyClient;
import org.red5.server.Scope;
import org.red5.server.WebScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.TestConnection;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ContextConfiguration(locations={"testcontext.xml"})
public class ServiceInvokerTest extends AbstractJUnit4SpringContextTests {

	// TODO: Add more tests!
	// we dont have to test all the echo methods, more test the call object works as expected
	// the correct types of status are returned (method not found) etc.
	// Also, we need to add tests which show the way the parameter conversion works.
	// So have a few methods with the same name, and try with diff params, making sure right one gets called.

	protected static Logger log = LoggerFactory.getLogger(ServiceInvokerTest.class);
	
	private final static List<String> workerList = new ArrayList<String>(11);
	private static AtomicInteger finishedCount = new AtomicInteger(0);
	
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "bin");
		System.setProperty("red5.config_root", "bin/conf");
	}

	@Test
	public void testAppContextLoaded() {
		Assert.assertNotNull(applicationContext);
		//Assert.assertNotNull(applicationContext.getBean("serviceInvoker"));
		Assert.assertNotNull(applicationContext.getBean("echoService"));
	}

	@Test
	public void testExceptionStatus() {
		ServiceInvoker invoker = null;
		if (applicationContext.containsBean(ServiceInvoker.SERVICE_NAME)) {
			invoker = (ServiceInvoker) applicationContext.getBean(ServiceInvoker.SERVICE_NAME);
		} else {
			invoker = (ServiceInvoker) applicationContext.getBean("global.serviceInvoker");
		}
		Object service = applicationContext.getBean("echoService");
		Object[] params = new Object[] { "Woot this is cool" };
		Call call = new Call("echoService", "doesntExist", params);
		invoker.invoke(call, service);
		Assert.assertEquals(false, call.isSuccess());
		Assert.assertEquals(Call.STATUS_METHOD_NOT_FOUND, call.getStatus());
		params = new Object[] { "too", "many", "params" };
		call = new Call("echoService", "echoNumber", params);
		invoker.invoke(call, service);
		Assert.assertEquals(false, call.isSuccess());
		Assert.assertEquals(Call.STATUS_METHOD_NOT_FOUND, call.getStatus());
	}

	@Test
	public void testSimpleEchoCall() {
		ServiceInvoker invoker = null;
		if (applicationContext.containsBean(ServiceInvoker.SERVICE_NAME)) {
			invoker = (ServiceInvoker) applicationContext.getBean(ServiceInvoker.SERVICE_NAME);
		} else {
			invoker = (ServiceInvoker) applicationContext.getBean("global.serviceInvoker");
		}		
		Object[] params = new Object[] { "Woot this is cool" };
		Object service = applicationContext.getBean("echoService");
		PendingCall call = new PendingCall("echoService", "echoString", params);
		invoker.invoke(call, service);
		Assert.assertEquals(true, call.isSuccess());
		Assert.assertEquals(params[0], call.getResult());
	}

	/**
	 * Test for memory leak bug #631
	 * http://trac.red5.org/ticket/631
	 */
	@Test
	public void testBug631() {
				
		final String message = "This is a test";
		
		//create our sender conn and set local
		IClientRegistry dummyReg = (IClientRegistry) applicationContext.getBean("global.clientRegistry");

		IScope scp = (WebScope) applicationContext.getBean("web.scope"); //conn.getScope();
		IContext ctx = (Context) applicationContext.getBean("web.context"); //scope.getContext();
		
		IConnection recipient = new SvcCapableTestConnection("localhost", "/junit", "1");//host, path, session id
		IClient rClient = dummyReg.newClient(new Object[]{"recipient"});
		((TestConnection) recipient).setClient(rClient);
		((TestConnection) recipient).setScope((Scope) scp);
		((DummyClient) rClient).registerConnection(recipient);
		
		IConnection sender = new SvcCapableTestConnection("localhost", "/junit", "2");//host, path, session id
		IClient sClient = dummyReg.newClient(new Object[]{"sender"});
		((TestConnection) sender).setClient(sClient);
		((TestConnection) sender).setScope((Scope) scp);
		((DummyClient) sClient).registerConnection(sender);
		
		Red5.setConnectionLocal(sender);
	
		//start standard process
		
		IConnection conn = Red5.getConnectionLocal();
		
		log.debug("Check s/c -\n s1: {} s2: {}\n c1: {} c2: {}", new Object[]{scp, conn.getScope(), ctx, conn.getScope().getContext()});
		
		IClient client = conn.getClient();
		IScope scope = (WebScope) conn.getScope();
		IContext context = (Context) scope.getContext();
		
		IClientRegistry reg = context.getClientRegistry();
		log.debug("Client registry: {}", reg.getClass().getName());

		IServiceCapableConnection serviceCapCon = null;

		if (reg.hasClient("recipient")) {
			IClient recip = reg.lookupClient("recipient");
			Set<IConnection> rcons = recip.getConnections(scope);
			log.debug("Recipient has {} connections", rcons.size());
			Object[] sendobj = new Object[] { client.getId(), message };
			for (IConnection rcon : rcons) {
				if (rcon instanceof IServiceCapableConnection) {
					serviceCapCon = (IServiceCapableConnection) rcon;
					serviceCapCon.invoke("privMessage", sendobj);
					break;
				} else {
					log.info("Connection is not service capable");
				}
			}
		}
		
		Assert.assertTrue(((SvcCapableTestConnection) serviceCapCon).getPrivateMessageCount() == 1);
	}
	
	/**
	 * Test for memory leak bug #631 with multiple threads
	 * http://trac.red5.org/ticket/631
	 */
	@Test
	public void testMultiThreadBug631() throws Throwable {
				
		//leak doesnt appear unless this is around 1000 and running outside eclipse
		int threadCount = 500;
		
		//init and run
		
		TestRunnable[] trs = new TestRunnable[threadCount];
		for (int t = 0; t < threadCount; t++) {
			ConnectionWorker worker = new ConnectionWorker(createConnection(t), t);
			trs[t] = worker;
			workerList.add(worker.getName());
		}

		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

		long start = System.nanoTime();
		mttr.runTestRunnables();
		log.info("Runtime: {} ns", (System.nanoTime() - start));

		for (TestRunnable r : trs) {
			ConnectionWorker wkr = (ConnectionWorker) r;
			String name = (wkr.getName());
			Assert.assertNotNull(name);
			Assert.assertTrue(wkr.getServiceCapableConnection().getPrivateMessageCount() > threadCount);
		}		
		
		//make sure all threads finished
		Assert.assertEquals(threadCount, finishedCount.get());
		
	}	
	
	private IConnection createConnection(int id) {
		//create our sender conn and set local
		IClientRegistry dummyReg = (IClientRegistry) applicationContext.getBean("global.clientRegistry");

		IScope scp = (WebScope) applicationContext.getBean("web.scope");
		//IContext ctx = (Context) applicationContext.getBean("web.context");
		
		IConnection conn = new SvcCapableTestConnection("localhost", "/junit", id + "");//host, path, session id
		IClient rClient = dummyReg.newClient(new Object[]{"client-" + id});
		((TestConnection) conn).setClient(rClient);
		((TestConnection) conn).setScope((Scope) scp);
		((DummyClient) rClient).registerConnection(conn);

		return conn;
	}
	
	final static class ConnectionWorker extends TestRunnable {
		
		Random rnd = new Random();
		
		IConnection conn;
		String name;
		
		public ConnectionWorker(IConnection conn, int index) {
			this.conn = conn;
			this.name = "client-" + index;
		}

		public void runTest() throws Throwable {
			Red5.setConnectionLocal(conn);
			
			//start standard process
			
			IConnection conn = Red5.getConnectionLocal();
			
			IClient client = conn.getClient();
			IScope scope = (WebScope) conn.getScope();
			IContext context = (Context) scope.getContext();
			
			IClientRegistry reg = context.getClientRegistry();

			IServiceCapableConnection serviceCapCon = null;

			//go through the client list and send at least one message to everyone
			for (String worker : workerList) {
				//dont send to ourself
				if (name.equals(worker)) {
					log.debug("Dont send to self");
					continue;
				}
    			if (reg.hasClient(worker)) {
    				IClient recip = reg.lookupClient(worker);
    				Set<IConnection> rcons = recip.getConnections(scope);
    				//log.debug("Recipient has {} connections", rcons.size());
    				Object[] sendobj = new Object[] { client.getId(), "This is a message from " + name};
    				for (IConnection rcon : rcons) {
    					if (rcon instanceof IServiceCapableConnection) {
    						serviceCapCon = (IServiceCapableConnection) rcon;
    						serviceCapCon.invoke("privMessage", sendobj);
    						break;
    					} else {
    						log.info("Connection is not service capable");
    					}
    				}
    			} else {
    				log.warn("Client not registered {}", worker);
    			}
			}
			
			//number of connections
			int connectionCount = workerList.size();
			
			//now send N messages to random recipients
			for (int i = 0; i < 1000; i++) {
				String worker = workerList.get(rnd.nextInt(connectionCount));
				//dont send to ourself
				if (name.equals(worker)) {
					log.debug("Dont send to self");
					continue;
				}
    			if (reg.hasClient(worker)) {
    				IClient recip = reg.lookupClient(worker);
    				Set<IConnection> rcons = recip.getConnections(scope);
    				//log.debug("Recipient has {} connections", rcons.size());
    				Object[] sendobj = new Object[] { client.getId(), "This is a message from " + name};
    				for (IConnection rcon : rcons) {
    					if (rcon instanceof IServiceCapableConnection) {
    						serviceCapCon = (IServiceCapableConnection) rcon;
    						serviceCapCon.invoke("privMessage", sendobj);
    						break;
    					} else {
    						log.info("Connection is not service capable");
    					}
    				}
    			} else {
    				log.warn("Client not registered {}", worker);
    			}
			}
			
			finishedCount.incrementAndGet();
			
		}

		public String getName() {
			return name;
		}
		
		public SvcCapableTestConnection getServiceCapableConnection() {
			return (SvcCapableTestConnection) conn;
		}
	}	
	
	final static class SvcCapableTestConnection extends TestConnection implements IServiceCapableConnection {

		private int privateMessageCount = 0;
		
		public SvcCapableTestConnection(String host, String path, String sessionId) {
			super(host, path, sessionId);
		}

		public int getPrivateMessageCount() {
			return privateMessageCount;
		}
		
		public void invoke(String method, Object[] params) {
			log.debug("Invoke on connection: {}", this.client.getId());
			if ("privMessage".equals(method)) {
				log.info("Got a private message from: {} message: {}", params);
				privateMessageCount++;
			} else {
				log.warn("Method: {} not implemented", method);
			}
		}

		@Override
		public void invoke(IServiceCall call) {
		}

		@Override
		public void invoke(IServiceCall call, int channel) {
		}

		@Override
		public void invoke(String method) {
		}

		@Override
		public void invoke(String method, IPendingServiceCallback callback) {
		}

		@Override
		public void invoke(String method, Object[] params, IPendingServiceCallback callback) {
		}

		@Override
		public void notify(IServiceCall call) {
		}

		@Override
		public void notify(IServiceCall call, int channel) {
		}

		@Override
		public void notify(String method) {			
		}

		@Override
		public void notify(String method, Object[] params) {
		}
		
	}
	
}
