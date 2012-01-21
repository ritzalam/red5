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

package org.red5.server.jmx;

import org.junit.Test;

public class JMXAgentTest {

	//private static Logger logger = LoggerFactory.getLogger(JMXAgentTest.class);

	@Test
	public void testGetHtmlAdapterPort() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testInit() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testRegisterMBeanObjectStringClass() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testRegisterMBeanObjectStringClassString() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testSetEnableHtmlAdapterBoolean() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testSetEnableHtmlAdapterString() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testSetHtmlAdapterPort() {
		System.out.println("Not yet implemented"); // TODO
	}

/* 
	@Test
	public void testUnregisterMBean() throws Exception {
		logger.info("Default jmx domain: {}", JMXFactory.getDefaultDomain());
		JMXAgent agent = new JMXAgent();
		agent.init();
		MBeanServer mbs = JMXFactory.getMBeanServer();
		//create a new mbean for this instance
		ObjectName oName = JMXFactory.createMBean(
				"org.red5.server.net.rtmp.RTMPMinaConnection",
				"connectionType=persistent,host=10.0.0.1,port=1935,clientId=1");
		assertNotNull(oName);

		ObjectName oName2 = new ObjectName(
				"org.red5.server:type=RTMPMinaConnection,connectionType=persistent,host=10.0.0.2,port=1935,clientId=2");

		logger.info("Register check 1: {}", mbs.isRegistered(oName));
		assertTrue(JMXAgent.unregisterMBean(oName));
		logger.info("Register check 2: {}", mbs.isRegistered(oName));
		assertFalse(JMXAgent.unregisterMBean(oName));
		logger.info("Register check 3: {}", mbs.isRegistered(oName2));
		assertFalse(JMXAgent.unregisterMBean(oName2));
	}
*/
	@Test
	public void testUpdateMBeanAttributeObjectNameStringInt() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testUpdateMBeanAttributeObjectNameStringString() {
		System.out.println("Not yet implemented"); // TODO
	}
}
