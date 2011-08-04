package org.red5.server.jmx;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2011 by respective authors. All rights reserved.
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
