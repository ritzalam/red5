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

package org.red5.server.stream;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.red5.server.Context;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.api.stream.OperationNotSupportedException;
import org.red5.server.api.stream.support.SimplePlayItem;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.scope.Scope;
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
		SimplePlayItem item = SimplePlayItem.build("DarkKnight.flv");
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
			log.warn("Exception {}", e);
		}
	}

	@Test
	public void testAddItemIPlayItem() {
		System.out.println("testAddItemIPlayItem");
		SimplePlayItem item = SimplePlayItem.build("IronMan.flv");
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
		
		DummyScope() {
			super(new Scope.Builder(null, ScopeType.ROOM, "dummy", false));
		}
		
	}

	private class DummyMessageOut implements IMessageOutput {

		public List<IProvider> getProviders() {
			System.out.println("getProviders");
			return null;
		}

		public void pushMessage(IMessage message) throws IOException {
			System.out.println("pushMessage: " + message);
		}

		public void sendOOBControlMessage(IProvider provider, OOBControlMessage oobCtrlMsg) {
			System.out.println("sendOOBControlMessage");
		}

		public boolean subscribe(IProvider provider, Map<String, Object> paramMap) {
			System.out.println("subscribe");
			return true;
		}

		public boolean unsubscribe(IProvider provider) {
			System.out.println("unsubscribe");
			return true;
		}
	}

}
