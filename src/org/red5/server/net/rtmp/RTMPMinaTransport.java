package org.red5.server.net.rtmp;

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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.management.ObjectName;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.apache.mina.core.service.AbstractIoService;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.service.IoServiceStatistics;
import org.apache.mina.integration.jmx.IoServiceMBean;
import org.apache.mina.transport.socket.SocketAcceptor;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioProcessor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.red5.server.jmx.JMXAgent;
import org.red5.server.jmx.JMXFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport setup class configures socket acceptor and thread pools for RTMP in Mina.
 * 
 * <br />
 * <i>Note: This code originates from AsyncWeb. Originally modified by Luke Hubbard.</i>
 * <br />
 * 
 * @author Luke Hubbard
 * @author Paul Gregoire
 */
public class RTMPMinaTransport {

	private static final int DEFAULT_CONN_THREADS = Runtime.getRuntime().availableProcessors();

	private static final int DEFAULT_IO_THREADS = DEFAULT_CONN_THREADS * 2;

	private static final int DEFAULT_RECEIVE_BUFFER_SIZE = 256 * 1024;

	private static final int DEFAULT_SEND_BUFFER_SIZE = 64 * 1024;

	private static final boolean DEFAULT_TCP_NO_DELAY = false;

	private static final boolean DEFAULT_USE_HEAP_BUFFERS = true;

	private static final Logger log = LoggerFactory.getLogger(RTMPMinaTransport.class);

	protected SocketAcceptor acceptor;

	protected Set<SocketAddress> addresses = new HashSet<SocketAddress>();

	protected IoHandlerAdapter ioHandler;

	protected IoServiceStatistics stats;

	protected int connectionThreads = DEFAULT_CONN_THREADS;

	protected int ioThreads = DEFAULT_IO_THREADS;

	/**
	 * MBean object name used for de/registration purposes.
	 */
	protected ObjectName serviceManagerObjectName;

	protected int jmxPollInterval = 1000;

	protected int receiveBufferSize = DEFAULT_RECEIVE_BUFFER_SIZE;

	protected int sendBufferSize = DEFAULT_SEND_BUFFER_SIZE;

	protected boolean tcpNoDelay = DEFAULT_TCP_NO_DELAY;

	protected boolean useHeapBuffers = DEFAULT_USE_HEAP_BUFFERS;

	private void initIOHandler() {
		if (ioHandler == null) {
			log.info("No RTMP IO Handler associated - using defaults");
			ioHandler = new RTMPMinaIoHandler();
		}
	}

	public void start() throws Exception {
		initIOHandler();

		IoBuffer.setUseDirectBuffer(!useHeapBuffers); // this is global, oh well
		if (useHeapBuffers) {
			// dont pool for heap buffers
			IoBuffer.setAllocator(new SimpleBufferAllocator());
		}

		log.info("RTMP Mina Transport Settings");
		log.info("Connection Threads: {}", connectionThreads);
		log.info("I/O Threads: {}", ioThreads);

		//use default parameters, and given number of NioProcessor for multithreading I/O operations
		//acceptor = new NioSocketAcceptor(ioThreads);
		
		//create separate executors to handle connections and i/o
		Executor connectionExecutor = Executors.newFixedThreadPool(connectionThreads);
		Executor ioExecutor = Executors.newFixedThreadPool(ioThreads);
		acceptor = new NioSocketAcceptor(connectionExecutor, new NioProcessor(ioExecutor));
		
		acceptor.setHandler(ioHandler);
		acceptor.setBacklog(50);

		log.info("TCP No Delay: {}", tcpNoDelay);
		log.info("Receive Buffer Size: {}", receiveBufferSize);
		log.info("Send Buffer Size: {}", sendBufferSize);

		//get the current session config that would be used during create
		SocketSessionConfig sessionConf = acceptor.getSessionConfig();
		//reuse the addresses
		sessionConf.setReuseAddress(true);
		sessionConf.setTcpNoDelay(tcpNoDelay);
		sessionConf.setReceiveBufferSize(receiveBufferSize);
		sessionConf.setSendBufferSize(sendBufferSize);

		//set reuse address on the socket acceptor as well
		acceptor.setReuseAddress(true);

		String addrStr = addresses.toString();
		log.debug("Binding to {}", addrStr);
		acceptor.bind(addresses);

		//create a new mbean for this instance
		// RTMPMinaTransport
		String cName = this.getClass().getName();
		if (cName.indexOf('.') != -1) {
			cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
		}
		//enable only if user wants it
		if (JMXAgent.isEnableMinaMonitor()) {
			//add a service manager to allow for more introspection into the workings of mina
			stats = new IoServiceStatistics((AbstractIoService) acceptor);
			//poll every second
			stats.setThroughputCalculationInterval(jmxPollInterval);
			//construct a object containing all the host and port combos
			StringBuilder addressAndPorts = new StringBuilder();
			for (SocketAddress sa : addresses) {
				InetSocketAddress isa = ((InetSocketAddress) sa);
				if (!isa.isUnresolved()) {
					addressAndPorts.append(isa.getHostName());
					addressAndPorts.append('|');
					addressAndPorts.append(isa.getPort());
					addressAndPorts.append(';');
				}
			}
			addressAndPorts.deleteCharAt(addressAndPorts.length() - 1);
			serviceManagerObjectName = JMXFactory.createObjectName("type", "IoServiceManager", "addresses", addressAndPorts.toString());
			JMXAgent.registerMBean(stats, stats.getClass().getName(), IoServiceMBean.class, serviceManagerObjectName);
		}
	}

	public void stop() {
		log.info("RTMP Mina Transport unbind");
		acceptor.unbind();
		// deregister with jmx
		if (serviceManagerObjectName != null) {
			JMXAgent.unregisterMBean(serviceManagerObjectName);
		}
	}

	public void setConnector(InetSocketAddress connector) {
		addresses.add(connector);
		log.info("RTMP Mina Transport bound to {}", connector.toString());
	}

	public void setConnectors(List<InetSocketAddress> connectors) {
		for (InetSocketAddress addr : connectors) {
			addresses.add(addr);
			log.info("RTMP Mina Transport bound to {}", addr.toString());
		}
	}

	public void setConnectionThreads(int connectionThreads) {
		this.connectionThreads = connectionThreads;
	}

	public void setIoHandler(IoHandlerAdapter rtmpIOHandler) {
		this.ioHandler = rtmpIOHandler;
	}

	public void setIoThreads(int ioThreads) {
		this.ioThreads = ioThreads;
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

	public int getJmxPollInterval() {
		return jmxPollInterval;
	}

	public void setJmxPollInterval(int jmxPollInterval) {
		this.jmxPollInterval = jmxPollInterval;
	}

	public String toString() {
		return String.format("RTMP Mina Transport %s", addresses.toString());
	}

}
