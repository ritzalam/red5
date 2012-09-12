package org.red5.server.api;

import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.ClientRegistry;
import org.red5.server.DummyClientRegistry;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.ServiceUtils;

public class ServiceUtilsTest extends BaseTest {

	private static int callbackCounter;

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ServiceUtilsTest.class);
	}

	@Before
	public void setUp() throws Exception {
		context = (IContext) applicationContext.getBean("red5.context");
		callbackCounter = 0;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testInvokeOnConnectionStringObjectArray() {
		System.out.println("------------------------------------------------------------------------\ntestInvokeOnConnectionStringObjectArray");
		// create client reg
		DummyClientRegistry registry = new DummyClientRegistry();
		// create a connection
		TestConnection conn = new TestConnection(host, "/", null);
		conn.setClient(registry.newClient(null));
		// add the connection to thread local
		Red5.setConnectionLocal(conn);
		// invoke on it
		assertTrue(ServiceUtils.invokeOnConnection("echo", new Object[] { "test 123" }, new GenericCallback()));
		long loopStart = System.currentTimeMillis();
		do {
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
			}
			if ((System.currentTimeMillis() - loopStart) > 120000) {
				break;
			}
		} while (callbackCounter < 1);
		assertTrue(callbackCounter == 1);
	}

	@Test
	public void testInvokeOnAllConnectionsStringObjectArray() {
		System.out.println("------------------------------------------------------------------------\ntestInvokeOnAllConnectionsStringObjectArray");
		IScope scope = context.resolveScope("/");
		// create client reg
		ClientRegistry registry = new ClientRegistry();
		// create a few connections
		TestConnection conn1 = new TestConnection(host, "/", null, "127.0.0.1");
		conn1.setClient(registry.newClient(null));
		conn1.connect(scope);
		TestConnection conn2 = new TestConnection(host, "/", null, "127.0.0.2");
		conn2.setClient(registry.newClient(null));
		conn2.connect(scope);
		TestConnection conn3 = new TestConnection(host, "/", null, "127.0.0.3");
		conn3.setClient(registry.newClient(null));
		conn3.connect(scope);
		// add the first connection to thread local
		Red5.setConnectionLocal(conn1);
		// invoke on it
		ServiceUtils.invokeOnAllConnections("echo", new Object[] { "test 123" }, new GenericCallback());
		long loopStart = System.currentTimeMillis();
		do {
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
			}
			if ((System.currentTimeMillis() - loopStart) > 120000) {
				break;
			}
		} while (callbackCounter < 3);
		assertTrue(callbackCounter == 3);
	}

	private final class GenericCallback implements IPendingServiceCallback {

		@Override
		public void resultReceived(IPendingServiceCall call) {
			log.debug("resultReceived - success: {} call: {}", call.isSuccess(), call);
			if (call.isSuccess()) {
				callbackCounter++;
			}
		}

	}

}
