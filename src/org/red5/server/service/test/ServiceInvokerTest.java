package org.red5.server.service.test;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors. All rights reserved.
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
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.service.Call;
import org.red5.server.service.ServiceInvoker;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ServiceInvokerTest extends TestCase {

	// TODO: Add more tests!
	// we dont have to test all the echo methods, more test the call object works as expected
	// the correct types of status are returned (method not found) etc. 
	// Also, we need to add tests which show the way the parameter conversion works.
	// So have a few methods with the same name, and try with diff params, making sure right one gets called.
	
	protected static Log log =
        LogFactory.getLog(ServiceInvokerTest.class.getName());
	
	protected ApplicationContext appCtx = null;
	
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		appCtx = new ClassPathXmlApplicationContext("org/red5/server/service/test/testcontext.xml");
	}
	
	public void testAppContextLoaded(){
		Assert.assertNotNull(appCtx);
		Assert.assertNotNull(appCtx.getBean("serviceInvoker"));
		Assert.assertNotNull(appCtx.getBean("echoService"));
	}

	public void testSimpleEchoCall(){
		Object[] params = new Object[]{"Woot this is cool"};
		Call call = new Call("echoService","echoString", params);
		ServiceInvoker invoker = (ServiceInvoker) appCtx.getBean(ServiceInvoker.SERVICE_NAME);
		invoker.invoke(call, appCtx);
		Assert.assertEquals(call.isSuccess(), true);
		Assert.assertEquals(call.getResult(), params[0]);
	}
	
	public void testExceptionStatus(){
		Object[] params = new Object[]{"Woot this is cool"};
		Call call = new Call("doesntExist","echoString", params);
		ServiceInvoker invoker = (ServiceInvoker) appCtx.getBean(ServiceInvoker.SERVICE_NAME);
		invoker.invoke(call, appCtx);
		Assert.assertEquals(call.isSuccess(), false);
		Assert.assertEquals(call.getStatus(), Call.STATUS_SERVICE_NOT_FOUND);
		call = new Call("echoService","doesntExist", params);
		invoker.invoke(call, appCtx);
		Assert.assertEquals(call.isSuccess(), false);
		Assert.assertEquals(call.getStatus(), Call.STATUS_METHOD_NOT_FOUND);
		params = new Object[]{"too","many","params"};
		call = new Call("echoService","echoString", params);
		invoker.invoke(call, appCtx);
		Assert.assertEquals(call.isSuccess(), false);
		Assert.assertEquals(call.getStatus(), Call.STATUS_METHOD_NOT_FOUND);
	}
	
	protected void tearDown() throws Exception {
		// TODO Auto-generated method stub
		super.tearDown();
		
	}

	
	
}
