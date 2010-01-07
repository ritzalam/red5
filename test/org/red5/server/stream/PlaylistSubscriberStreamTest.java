package org.red5.server.stream;

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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.red5.server.Context;
import org.red5.server.Scope;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.stream.OperationNotSupportedException;
import org.red5.server.api.stream.support.SimplePlayItem;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.OOBControlMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

@SuppressWarnings("deprecation")
public class PlaylistSubscriberStreamTest extends AbstractDependencyInjectionSpringContextTests {

	protected static Logger log = LoggerFactory.getLogger(PlaylistSubscriberStreamTest.class);

	private static PlaylistSubscriberStream pss;
	
	@Override
	protected String[] getConfigLocations() {
		return new String[] { "org/red5/server/stream/PlaylistSubscriberStreamTest.xml" };
	}
	
	@Override
	protected void onSetUp() throws Exception {
		System.out.println("onSetUp");
		super.onSetUp();
		//
		System.setProperty("red5.deployment.type", "junit");
		if (pss == null) {
			pss = (PlaylistSubscriberStream) applicationContext.getBean("playlistSubscriberStream");
			Context ctx = new Context();
			ctx.setApplicationContext(applicationContext);
			Scope scope = new DummyScope();
			scope.setName("");
			scope.setContext(ctx);
			pss.setScope(scope);
			//
			ISchedulingService schedulingService = (ISchedulingService) applicationContext.getBean(ISchedulingService.BEAN_NAME);
			IConsumerService consumerService = (IConsumerService) applicationContext.getBean(IConsumerService.KEY);
			IProviderService providerService = (IProviderService) applicationContext.getBean(IProviderService.BEAN_NAME);
			//create and get the engine
			PlayEngine engine = pss.createEngine(schedulingService, consumerService, providerService);
			//mock the message output
			engine.setMessageOut(new DummyMessageOut());
		}
	}

	@Override
	protected void onTearDown() throws Exception {
		System.out.println("onTearDown");
		super.onTearDown();
	}

	@Test
	public void testGetExecutor() {
		System.out.println("testGetExecutor");
		assertTrue(pss.getExecutor() != null);
	}

	@Test
	public void testStart() {
		System.out.println("testStart");
		SimplePlayItem item = new SimplePlayItem();
		item.setName("DarkKnight.flv");
		pss.addItem(item);
		pss.start();
	}

	@Test
	public void testPlay() throws Exception {
		System.out.println("testPlay");
		pss.play();
	}

	@Test
	public void testPause() {		
		System.out.println("testPause");
		long sent = pss.getBytesSent();
		pss.pause((int) sent);
	}

	@Test
	public void testResume() {
		System.out.println("testResume");
		long sent = pss.getBytesSent();
		pss.resume((int) sent);		
	}

	@Test
	public void testSeek() {
		System.out.println("testSeek");
		long sent = pss.getBytesSent();
		try {
			pss.seek((int) (sent * 2));
		} catch (OperationNotSupportedException e) {
			e.printStackTrace();
		}		
	}

	@Test
	public void testAddItemIPlayItem() {
		System.out.println("testAddItemIPlayItem");
		SimplePlayItem item = new SimplePlayItem();
		item.setName("IronMan.flv");
		pss.addItem(item);
	}

	@Test
	public void testPreviousItem() {
		log.error("Not yet implemented -- get on that");
	}

	@Test
	public void testNextItem() {
		log.error("Not yet implemented -- get on that");
	}

	@Test
	public void testSetItem() {
		log.error("Not yet implemented -- get on that");
	}
	
	@Test
	public void testStop() {		
		System.out.println("testStop");
		pss.stop();
	}	
	
	@Test
	public void testClose() {		
		System.out.println("testClose");
		pss.close();
	}	
	
	private class DummyScope extends Scope {

	}
	
	private class DummyMessageOut implements IMessageOutput {

		public List<IProvider> getProviders() {
			System.out.println("getProviders");
			return null;
		}

		public void pushMessage(IMessage message) throws IOException {
			System.out.println("pushMessage: " + message);	
		}

		public void sendOOBControlMessage(IProvider provider,
				OOBControlMessage oobCtrlMsg) {
			System.out.println("sendOOBControlMessage");			
		}

		public boolean subscribe(IProvider provider, Map<?, ?> paramMap) {
			System.out.println("subscribe");
			return true;
		}

		public boolean unsubscribe(IProvider provider) {
			System.out.println("unsubscribe");
			return true;
		}
	}

}
