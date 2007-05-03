package org.red5.server.net.rtmp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilter;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.apache.mina.transport.socket.nio.SocketAcceptorConfig;
import org.apache.mina.transport.socket.nio.SocketSessionConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.io.filter.ExecutorFilter;

/** 
 * Transport setup class configures socket acceptor and thread pools for RTMP in mina.
 * Note: This code originates from AsyncWeb, I've modified it for use with Red5. - Luke
 */
public class RTMPMinaTransport {

	private static final Log log = LogFactory.getLog(RTMPMinaTransport.class);

	private static final int DEFAULT_PORT = 1935;

	private static final int DEFAULT_IO_THREADS = Runtime.getRuntime()
			.availableProcessors();

	private static final int DEFAULT_EVENT_THREADS = 16;
	
	private static final int DEFAULT_MAX_EVENT_THREADS = 128;
	
	private static final int DEFAULT_THREAD_KEEP_ALIVE_TIME = 60;

	private static final int DEFAULT_SEND_BUFFER_SIZE = 64 * 1024;

	private static final int DEFAULT_RECEIVE_BUFFER_SIZE = 256 * 1024;

	private static final boolean DEFAULT_TCP_NO_DELAY = false;
	
	private static final boolean DEFAULT_USE_HEAP_BUFFERS = true;

	private SocketAcceptor acceptor;

	private ExecutorService ioExecutor;

	private String address = null;

	private int port = DEFAULT_PORT;

	private int ioThreads = DEFAULT_IO_THREADS;

	private int eventThreads = DEFAULT_EVENT_THREADS;
	
	private int maxEventThreads = DEFAULT_MAX_EVENT_THREADS;
	
	private int threadKeepAliveTime = DEFAULT_THREAD_KEEP_ALIVE_TIME;

	private int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;

	private int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;

	private boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;
	
	private boolean useHeapBuffers = DEFAULT_USE_HEAP_BUFFERS;

	private RTMPMinaIoHandler ioHandler;

	private boolean isLoggingTraffic = false;

	public void setAddress(String address) {
		if ("*".equals(address) || "0.0.0.0".equals(address)) {
			address = null;
		}
		this.address = address;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getIoThreads() {
		return ioThreads;
	}

	public void setIoThreads(int ioThreads) {
		this.ioThreads = ioThreads;
	}

	public int getEventThreads() {
		return eventThreads;
	}

	public void setEventThreads(int eventThreads) {
		this.eventThreads = eventThreads;
	}

	public void setIsLoggingTraffic(boolean isLoggingTraffic) {
		this.isLoggingTraffic = isLoggingTraffic;
	}

	public void setIoHandler(RTMPMinaIoHandler rtmpIOHandler) {
		this.ioHandler = rtmpIOHandler;
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

	public int getMaxEventThreads() {
		return maxEventThreads;
	}

	public void setMaxEventThreads(int maxEventThreads) {
		this.maxEventThreads = maxEventThreads;
	}

	public int getThreadKeepAliveTime() {
		return threadKeepAliveTime;
	}

	public void setThreadKeepAliveTime(int threadKeepAliveTime) {
		this.threadKeepAliveTime = threadKeepAliveTime;
	}

	public void start() throws Exception {
		initIOHandler();
		
		ByteBuffer.setUseDirectBuffers(!useHeapBuffers); // this is global, oh well.
		
		ioExecutor = new ThreadPoolExecutor(ioThreads + 1, ioThreads + 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		
		acceptor = new SocketAcceptor(ioThreads, ioExecutor);
		
		acceptor.getFilterChain().addLast("threadPool",
				new ExecutorFilter(eventThreads, maxEventThreads, threadKeepAliveTime));

		SocketAcceptorConfig config = acceptor.getDefaultConfig();
		config.setReuseAddress(true);
		config.setBacklog(100);
		
		SocketSessionConfig sessionConf = (SocketSessionConfig) config.getSessionConfig();
		sessionConf.setReuseAddress(true);
		sessionConf.setTcpNoDelay(tcpNoDelay);
		sessionConf.setReceiveBufferSize(receiveBufferSize);
		sessionConf.setSendBufferSize(sendBufferSize);
		
		if (isLoggingTraffic) {
			log.info("Configuring traffic logging filter");
			IoFilter filter = new LoggingFilter();
			acceptor.getFilterChain().addFirst("LoggingFilter", filter);
		}
		
		SocketAddress socketAddress = (address == null) ? new InetSocketAddress(port) : new InetSocketAddress(address, port);
		acceptor.bind(socketAddress, ioHandler);

		log.info("RTMP Transport bound to " + socketAddress.toString());

	}

	public void stop() {
		log.info("RTMP Transport unbind");
		acceptor.unbindAll();
		ioExecutor.shutdown();
	}

	public String toString() {
		return "RTMP Transport [port=" + port + "]";
	}

	private void initIOHandler() {
		if (ioHandler == null) {
			log.info("No rtmp IO Handler associated - using defaults");
			ioHandler = new RTMPMinaIoHandler();
		}
	}
}