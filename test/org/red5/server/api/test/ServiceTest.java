package org.red5.server.api.test;

import static junit.framework.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.service.PendingCall;

public class ServiceTest extends BaseTest {

	@Test
	public void simpletest() {
		IPendingServiceCall call = new PendingCall("echoService", "echoString",
				new Object[] { "My String" });
		context.getServiceInvoker().invoke(call, context);
		assertTrue("result null", call.getResult() != null);
	}

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ServiceTest.class);
	}

}
