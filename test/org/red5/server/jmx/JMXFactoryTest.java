package org.red5.server.jmx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.management.ObjectName;

import org.junit.Test;

public class JMXFactoryTest implements JMXFactoryTestMBean {

	@Test
	public void testCreateMBean() {
		//test for APPSERVER-121 fix
		ObjectName objectName = null;
		try {
			objectName = JMXFactory.createMBean(
					"org.red5.server.jmx.JMXFactoryTest", "test=1");
			assertNotNull(objectName);
			objectName = JMXFactory.createMBean(
					"org.red5.server.jmx.JMXFactoryTest", "test=2");
			assertNotNull(objectName);
			objectName = JMXFactory.createMBean(
					"org.red5.server.jmx.JMXFactoryTest", "test=1");
			assertNotNull(objectName);
		} catch (Exception e) {
			fail("Exception occured");
		}

	}

	@Test
	public void testCreateSimpleMBean() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetDefaultDomain() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetDomain() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testGetMBeanServer() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testRegisterMBeanObjectStringClass() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testRegisterMBeanObjectStringClassString() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testRegisterNewMBeanStringClass() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testRegisterNewMBeanStringClassString() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testSetDomain() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testUnregisterMBean() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testUpdateMBeanAttributeObjectNameStringInt() {
		fail("Not yet implemented"); // TODO
	}

	@Test
	public void testUpdateMBeanAttributeObjectNameStringString() {
		fail("Not yet implemented"); // TODO
	}

}
