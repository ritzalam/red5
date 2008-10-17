package org.red5.server.service;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2008 by respective authors. All rights reserved.
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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
*/
public class ServiceInvokerTest extends TestCase {

	// TODO: Add more tests!
	// we dont have to test all the echo methods, more test the call object works as expected
	// the correct types of status are returned (method not found) etc.
	// Also, we need to add tests which show the way the parameter conversion works.
	// So have a few methods with the same name, and try with diff params, making sure right one gets called.

	protected static Logger log = LoggerFactory.getLogger(ServiceInvokerTest.class);

	protected ApplicationContext appCtx = null;

	/** {@inheritDoc} */
    @Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		appCtx = new ClassPathXmlApplicationContext(
				"org/red5/server/service/testcontext.xml");
	}

	/** {@inheritDoc} */
    @Override
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();

	}

	public void testAppContextLoaded() {
		Assert.assertNotNull(appCtx);
		Assert.assertNotNull(appCtx.getBean("serviceInvoker"));
		Assert.assertNotNull(appCtx.getBean("echoService"));
	}

	public void testExceptionStatus() {
		ServiceInvoker invoker = (ServiceInvoker) appCtx
		.getBean(ServiceInvoker.SERVICE_NAME);
		Object service = appCtx.getBean("echoService");
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

	public void testSimpleEchoCall() {
		Object[] params = new Object[] { "Woot this is cool" };
		Object service = appCtx.getBean("echoService");
		PendingCall call = new PendingCall("echoService", "echoString", params);
		ServiceInvoker invoker = (ServiceInvoker) appCtx
				.getBean(ServiceInvoker.SERVICE_NAME);
		invoker.invoke(call, service);
		Assert.assertEquals(true, call.isSuccess());
		Assert.assertEquals(params[0], call.getResult());
	}

}
