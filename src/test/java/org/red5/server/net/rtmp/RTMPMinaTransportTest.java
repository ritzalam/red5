package org.red5.server.net.rtmp;

import java.net.InetSocketAddress;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.client.net.rtmp.ClientExceptionHandler;
import org.red5.client.net.rtmp.RTMPClient;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.message.Header;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = { "../context.xml", "../../scope/ScopeTest.xml" })
public class RTMPMinaTransportTest extends AbstractJUnit4SpringContextTests {

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "target/test-classes");
		System.setProperty("red5.config_root", "src/main/server/conf");
		System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLoad() throws Exception {
		RTMPMinaTransport mina = new RTMPMinaTransport();
		mina.setBacklog(128);
		mina.setEnableDefaultAcceptor(false);
		mina.setEnableMinaMonitor(false);
		mina.setInitialPoolSize(16);
		mina.setMaxPoolSize(64);
		mina.setMinaPollInterval(15);
		mina.setTcpNoDelay(true);
		mina.setTrafficClass(24);
		// create an address
		mina.setConnector(new InetSocketAddress("0.0.0.0", 1935));
		// start
		mina.start();
		// create some clients
		int threads = 10;
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new CreatorWorker();
		}
		Runtime rt = Runtime.getRuntime();
		long startFreeMem = rt.freeMemory();
		System.out.printf("Free mem: %s\n", startFreeMem);
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);
		try {
			mttr.runTestRunnables();
		} catch (Throwable e1) {
			e1.printStackTrace();
		}
		for (TestRunnable r : trs) {
			TestClient cli = ((CreatorWorker) r).getClient();
			cli.disconnect();
		}
		System.out.printf("Free mem: %s\n", rt.freeMemory());
		try {
			Thread.sleep(1000L);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// stop
		mina.stop();
	}

	private class CreatorWorker extends TestRunnable {
		TestClient client;

		public void runTest() throws Throwable {
			client = new TestClient();
			client.connect();
			Thread.sleep(1000L);

		}

		public TestClient getClient() {
			return client;
		}

	}

	private class TestClient extends RTMPClient {

		private String server = "localhost";

		private int port = 1935;

		private String application = "junit";

		public void connect() {
			setExceptionHandler(new ClientExceptionHandler() {
				@Override
				public void handleException(Throwable throwable) {
					throwable.printStackTrace();
				}
			});
			setStreamEventDispatcher(streamEventDispatcher);
			connect(server, port, application, connectCallback);
		}

		private IEventDispatcher streamEventDispatcher = new IEventDispatcher() {
			public void dispatchEvent(IEvent event) {
				System.out.println("ClientStream.dispachEvent()" + event.toString());
			}
		};

		private IPendingServiceCallback connectCallback = new IPendingServiceCallback() {
			public void resultReceived(IPendingServiceCall call) {
				System.out.println("connectCallback");
				ObjectMap<?, ?> map = (ObjectMap<?, ?>) call.getResult();
				String code = (String) map.get("code");
				if ("NetConnection.Connect.Rejected".equals(code)) {
					System.out.printf("Rejected: %s\n", map.get("description"));
					disconnect();
				} else if ("NetConnection.Connect.Success".equals(code)) {
					createStream(createStreamCallback);
				} else {
					System.out.printf("Unhandled response code: %s\n", code);
				}
			}
		};

		private IPendingServiceCallback createStreamCallback = new IPendingServiceCallback() {
			public void resultReceived(IPendingServiceCall call) {
				int streamId = (Integer) call.getResult();
				conn.ping(new Ping(Ping.CLIENT_BUFFER, streamId, 500));
			}
		};

		protected void onInvoke(RTMPConnection conn, Channel channel, Header header, Notify notify, RTMP rtmp) {
			super.onInvoke(conn, channel, header, notify, rtmp);
			System.out.println("onInvoke, header = " + header.toString());
			System.out.println("onInvoke, notify = " + notify.toString());
			System.out.println("onInvoke, rtmp = " + rtmp.toString());
		}
	}

}
