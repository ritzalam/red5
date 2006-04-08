package org.red5.server.api.test;

import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.service.Call;
import static junit.framework.Assert.assertTrue;

public class ServiceTest extends BaseTest {

	@Test public void simpletest(){
		IServiceCall call = new Call("echoService","echoString",new Object[]{"My String"});
		call = context.getServiceInvoker().invoke(call, context);
		assertTrue("result null",call.getResult()!=null);
	}
	
	public static junit.framework.Test suite(){
		return new JUnit4TestAdapter(ServiceTest.class);
	}
	
}
