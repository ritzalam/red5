package org.red5.server.jmx;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMXAgentTest {

	private static Logger logger = LoggerFactory.getLogger(JMXAgentTest.class);

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

	@Test
	public void testUpdateMBeanAttributeObjectNameStringInt() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testUpdateMBeanAttributeObjectNameStringString() {
		System.out.println("Not yet implemented"); // TODO
	}
}
