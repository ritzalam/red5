package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.service.EchoService;
import org.red5.server.service.PendingCall;

public class ServiceTest extends BaseTest {

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ServiceTest.class);
	}

	@Test
	public void simpletest() {
		if (context.hasBean("echoService")) {
    		EchoService service = (EchoService) context.getBean("echoService");
    		IPendingServiceCall call = new PendingCall("echoService", "echoString",
    				new Object[] { "My String" });
    		context.getServiceInvoker().invoke(call, service);
    		assertTrue("result null", call.getResult() != null);
		} else {
			System.out.println("No echo service found");
			assertTrue(false);
		}
	}

}
