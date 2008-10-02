package org.red5.server.api;

import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.service.PendingCall;

public class ServiceTest extends BaseTest {

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ServiceTest.class);
	}

	@Test
	public void simpletest() {
		IPendingServiceCall call = new PendingCall("echoService", "echoString",
				new Object[] { "My String" });
		context.getServiceInvoker().invoke(call, context.getBean("echoService"));
		assertTrue("result null", call.getResult() != null);
	}

}
