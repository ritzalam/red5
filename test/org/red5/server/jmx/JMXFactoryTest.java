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
