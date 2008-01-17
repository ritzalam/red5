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
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testGetDefaultDomain() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testGetDomain() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testGetMBeanServer() {
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
	public void testRegisterNewMBeanStringClass() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testRegisterNewMBeanStringClassString() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testSetDomain() {
		System.out.println("Not yet implemented"); // TODO
	}

}
