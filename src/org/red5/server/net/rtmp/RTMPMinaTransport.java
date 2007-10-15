package org.red5.server.net.rtmp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.integration.jmx.IoServiceManager;
import org.apache.mina.integration.jmx.IoServiceManagerMBean;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;
import org.red5.server.jmx.JMXAgent;
import org.red5.server.jmx.JMXFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport setup class configures socket acceptor and thread pools for RTMP in mina.
 * Note: This code originates from AsyncWeb, I've modified it for use with Red5. - Luke
 */
public class RTMPMinaTransport implements RTMPMinaTransportMBean {

	private static final int DEFAULT_EVENT_THREADS_CORE = 16;

	private static final int DEFAULT_EVENT_THREADS_KEEPALIVE = 60;

	private static final int DEFAULT_EVENT_THREADS_MAX = 32;

	private static final int DEFAULT_EVENT_THREADS_QUEUE = -1;

	private static final int DEFAULT_IO_THREADS = Runtime.getRuntime()
			.availableProcessors();

	private static final int DEFAULT_PORT = 1935;

	private static final int DEFAULT_RECEIVE_BUFFER_SIZE = 256 * 1024;

	private static final int DEFAULT_SEND_BUFFER_SIZE = 64 * 1024;

	private static final boolean DEFAULT_TCP_NO_DELAY = false;

	private static final boolean DEFAULT_USE_HEAP_BUFFERS = true;

	private static final Logger log = LoggerFactory.getLogger(RTMPMinaTransport.class);

	private SocketAcceptor acceptor;

	private String address = null;

	private ExecutorService eventExecutor;

	private int eventThreadsCore = DEFAULT_EVENT_THREADS_CORE;

	private int eventThreadsKeepalive = DEFAULT_EVENT_THREADS_KEEPALIVE;

	private int eventThreadsMax = DEFAULT_EVENT_THREADS_MAX;

	private int eventThreadsQueue = DEFAULT_EVENT_THREADS_QUEUE;

	private ExecutorService ioExecutor;

	private IoHandlerAdapter ioHandler;

	private int ioThreads = DEFAULT_IO_THREADS;

	private boolean isLoggingTraffic = false;

	/**
	 * MBean object name used for de/registration purposes.
	 */
	private ObjectName oName;
	private ObjectName serviceManagerObjectName;
	
	private int jmxPollInterval = 1000;

	private int port = DEFAULT_PORT;

	private int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;

	private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;

	private boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;

	private boolean useHeapBuffers = DEFAULT_USE_HEAP_BUFFERS;

	private void initIOHandler() {
		if (ioHandler == null) {
			log.info("No rtmp IO Handler associated - using defaults");
			ioHandler = new RTMPMinaIoHandler();
		}
	}

	public void setAddress(String address) {
		if ("*".equals(address) || "0.0.0.0".equals(address)) {
			address = null;
		}
		this.address = address;
		//update the mbean
		//TODO: get the correct address for fallback when address is null
		JMXAgent.updateMBeanAttribute(oName, "address",
				(address == null ? "0.0.0.0" : address));
	}

	public void setEventThreadsCore(int eventThreadsCore) {
		this.eventThreadsCore = eventThreadsCore;
	}

	public void setEventThreadsKeepalive(int eventThreadsKeepalive) {
		this.eventThreadsKeepalive = eventThreadsKeepalive;
	}

	public void setEventThreadsMax(int eventThreadsMax) {
		this.eventThreadsMax = eventThreadsMax;
	}

	public void setEventThreadsQueue(int eventThreadsQueue) {
		this.eventThreadsQueue = eventThreadsQueue;
	}

	public void setIoHandler(IoHandlerAdapter rtmpIOHandler) {
		this.ioHandler = rtmpIOHandler;
	}

	public void setIoThreads(int ioThreads) {
		this.ioThreads = ioThreads;
	}

	public void setIsLoggingTraffic(boolean isLoggingTraffic) {
		this.isLoggingTraffic = isLoggingTraffic;
	}

	public void setPort(int port) {
		this.port = port;
		JMXAgent.updateMBeanAttribute(oName, "port", port);
	}

	public void setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
	}

	public void setSendBufferSize(int sendBufferSize) {
		this.sendBufferSize = sendBufferSize;
	}

	public void setTcpNoDelay(boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
	}

	public void setUseHeapBuffers(boolean useHeapBuffers) {
		this.useHeapBuffers = useHeapBuffers;
	}

	public void start() throws Exception {
		initIOHandler();

		ByteBuffer.setUseDirectBuffers(!useHeapBuffers); // this is global, oh well.
		if (useHeapBuffers)
			ByteBuffer.setAllocator(new SimpleByteBufferAllocator()); // dont pool for heap buffers.

		log.info("RTMP Mina Transport Settings");
		log.info("IO Threads: " + ioThreads + "+1");
		log.info("Event Threads:" + " core: " + eventThreadsCore + "+1"
				+ " max: " + eventThreadsMax + "+1" + " queue: "
				+ eventThreadsQueue + " keepalive: " + eventThreadsKeepalive);

		ioExecutor = new ThreadPoolExecutor(ioThreads + 1, ioThreads + 1, 60,
				TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

		eventExecutor = new ThreadPoolExecutor(eventThreadsCore + 1,
				eventThreadsMax + 1, eventThreadsKeepalive, TimeUnit.SECONDS,
				threadQueue(eventThreadsQueue));

		acceptor = new SocketAcceptor(ioThreads, ioExecutor);

		acceptor.getFilterChain().addLast("threadPool",
				new ExecutorFilter(eventExecutor));

		SocketAcceptorConfig config = acceptor.getDefaultConfig();
		config.setThreadModel(ThreadModel.MANUAL);
		config.setReuseAddress(true);
		config.setBacklog(100);

		log.info("TCP No Delay: " + tcpNoDelay);
		log.info("Receive Buffer Size: " + receiveBufferSize);
		log.info("Send Buffer Size: " + sendBufferSize);

		SocketSessionConfig sessionConf = (SocketSessionConfig) config
				.getSessionConfig();
		sessionConf.setReuseAddress(true);
		sessionConf.setTcpNoDelay(tcpNoDelay);
		sessionConf.setReceiveBufferSize(receiveBufferSize);
		sessionConf.setSendBufferSize(sendBufferSize);

		if (isLoggingTraffic) {
			log.info("Configuring traffic logging filter");
			IoFilter filter = new LoggingFilter();
			acceptor.getFilterChain().addFirst("LoggingFilter", filter);
		}

		SocketAddress socketAddress = (address == null) ? new InetSocketAddress(
				port)
				: new InetSocketAddress(address, port);
		acceptor.bind(socketAddress, ioHandler);

		log.info("RTMP Mina Transport bound to " + socketAddress.toString());

		//create a new mbean for this instance
		oName = JMXFactory.createObjectName("type", "RTMPMinaTransport",
				"address", (address == null ? "0.0.0.0" : address), "port",
				port + "");
		JMXAgent.registerMBean(this, this.getClass().getName(),
				RTMPMinaTransportMBean.class, oName);
		
		//enable only if user wants it
		if (JMXAgent.isEnableMinaMonitor()) {
    		//add a service manager to allow for more introspection into the workings of mina
    		IoServiceManager serviceManager = new IoServiceManager(acceptor);
    		//poll every second
    		serviceManager.startCollectingStats(jmxPollInterval);
    		serviceManagerObjectName = JMXFactory.createObjectName("type", "IoServiceManager",
    				"address", (address == null ? "0.0.0.0" : address), "port",
    				port + "");
    		JMXAgent.registerMBean(serviceManager, serviceManager.getClass().getName(), IoServiceManagerMBean.class, serviceManagerObjectName);		
		}
	}

	public void stop() {
		log.info("RTMP Mina Transport unbind");
		acceptor.unbindAll();
		ioExecutor.shutdown();
		eventExecutor.shutdown();
		// deregister with jmx
		JMXAgent.unregisterMBean(oName);
		if (serviceManagerObjectName != null) {
			JMXAgent.unregisterMBean(serviceManagerObjectName);
		}
	}

	private BlockingQueue<Runnable> threadQueue(int size) {
		switch (size) {
			case -1:
				return new LinkedBlockingQueue<Runnable>();
			case 0:
				return new SynchronousQueue<Runnable>();
			default:
				return new ArrayBlockingQueue<Runnable>(size);
		}
	}

	public String toString() {
		return "RTMP Mina Transport [port=" + port + "]";
	}

	public int getJmxPollInterval() {
		return jmxPollInterval;
	}

	public void setJmxPollInterval(int jmxPollInterval) {
		this.jmxPollInterval = jmxPollInterval;
	}
}