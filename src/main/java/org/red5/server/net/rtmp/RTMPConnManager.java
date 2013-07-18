/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmp;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.JMX;
import javax.management.ObjectName;

import org.red5.server.api.Red5;
import org.red5.server.jmx.mxbeans.RTMPMinaTransportMXBean;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Responsible for management and creation of RTMP based connections.
 * 
 * @author The Red5 Project
 */
public class RTMPConnManager implements IConnectionManager<RTMPConnection>, ApplicationContextAware, DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(RTMPConnManager.class);

	private static ApplicationContext applicationContext;

	protected ConcurrentMap<String, RTMPConnection> connMap = new ConcurrentHashMap<String, RTMPConnection>();

	protected AtomicInteger conns = new AtomicInteger();

	protected static IConnectionManager<RTMPConnection> instance;

	protected boolean debug;

	public static RTMPConnManager getInstance() {
		if (instance == null) {
			if (applicationContext != null && applicationContext.containsBean("rtmpConnManager")) {
				instance = (RTMPConnManager) applicationContext.getBean("rtmpConnManager");
			} else {
				instance = new RTMPConnManager();
			}
		}
		return (RTMPConnManager) instance;
	}

	/**
	 * Creates a connection of the type specified.
	 * 
	 * @param connCls
	 */
	public RTMPConnection createConnection(Class<?> connCls) {
		RTMPConnection conn = null;
		if (RTMPConnection.class.isAssignableFrom(connCls)) {
			try {
				// create connection
				conn = createConnectionInstance(connCls);
				// add to local map
				connMap.put(conn.getSessionId(), conn);
				log.trace("Connections: {}", conns.incrementAndGet());
				// set the scheduler
				if (applicationContext.containsBean("rtmpScheduler") && conn.getScheduler() == null) {
					conn.setScheduler((ThreadPoolTaskScheduler) applicationContext.getBean("rtmpScheduler"));
				}
				log.trace("Connection created: {}", conn);
				// start the wait for handshake
				conn.startWaitForHandshake();
			} catch (Exception ex) {
				log.warn("Exception creating connection", ex);
			}
		}
		return conn;
	}

	/**
	 * Adds a connection.
	 * 
	 * @param conn
	 */
	public void setConnection(RTMPConnection conn) {
		log.debug("Adding connection: {}", conn);
		int id = conn.getId();
		if (id == -1) {
			log.debug("Connection has unsupported id, using session id hash");
			id = conn.getSessionId().hashCode();
		}
		log.debug("Connection id: {} session id hash: {}", conn.getId(), conn.getSessionId().hashCode());
		if (debug) {
			log.info("Connection count (map): {}", connMap.size());
			try {
				RTMPMinaTransportMXBean proxy = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), new ObjectName("org.red5.server:type=RTMPMinaTransport"),
						RTMPMinaTransportMXBean.class, true);
				if (proxy != null) {
					log.info("{}", proxy.getStatistics());
				}
			} catch (Exception e) {
				log.warn("Error on jmx lookup", e);
			}
		}
	}

	/**
	 * Returns a connection for a given client id.
	 * 
	 * @param clientId
	 * @return connection if found and null otherwise
	 */
	public RTMPConnection getConnection(int clientId) {
		log.debug("Getting connection by client id: {}", clientId);
		for (RTMPConnection conn : connMap.values()) {
			if (conn.getId() == clientId) {
				return connMap.get(conn.getSessionId());
			}
		}
		return null;
	}

	/**
	 * Returns a connection for a given session id.
	 * 
	 * @param sessionId
	 * @return connection if found and null otherwise
	 */
	public RTMPConnection getConnectionBySessionId(String sessionId) {
		log.debug("Getting connection by session id: {}", sessionId);
		if (connMap.containsKey(sessionId)) {
			return connMap.get(sessionId);
		} else {
			log.warn("Connection not found for {}", sessionId);
			if (log.isTraceEnabled()) {
				log.trace("Connections ({}) {}", connMap.size(), connMap.values());
			}
		}
		return null;
	}

	/** {@inheritDoc} */
	public RTMPConnection removeConnection(int clientId) {
		log.debug("Removing connection with id: {}", clientId);
		// remove from map
		for (RTMPConnection conn : connMap.values()) {
			if (conn.getId() == clientId) {
				// remove the conn
				return removeConnection(conn.getSessionId());
			}
		}
		log.warn("Connection was not removed by id: {}", clientId);
		return null;
	}

	/** {@inheritDoc} */
	public RTMPConnection removeConnection(String sessionId) {
		log.debug("Removing connection with session id: {}", sessionId);
		if (log.isTraceEnabled()) {
			log.trace("Connections ({}) at pre-remove: {}", connMap.size(), connMap.values());
		}
		// remove from map
		RTMPConnection conn = connMap.remove(sessionId);
		if (conn != null) {
			log.trace("Connections: {}", conns.decrementAndGet());
			Red5.setConnectionLocal(null);
		}
		return conn;
	}

	/**
	 * Returns all the current connections. It doesn't remove anything.
	 * 
	 * @return list of connections
	 */
	public Collection<RTMPConnection> removeConnections() {
		ArrayList<RTMPConnection> list = new ArrayList<RTMPConnection>(connMap.size());
		list.addAll(connMap.values());
		return list;
	}

	/**
	 * Creates a connection instance based on the supplied type.
	 * 
	 * @param cls
	 * @return connection
	 * @throws Exception
	 */
	public RTMPConnection createConnectionInstance(Class<?> cls) throws Exception {
		RTMPConnection conn = null;
		if (cls == RTMPMinaConnection.class) {
			conn = (RTMPMinaConnection) applicationContext.getBean(RTMPMinaConnection.class);
		} else if (cls == RTMPTConnection.class) {
			conn = (RTMPTConnection) applicationContext.getBean(RTMPTConnection.class);
		} else {
			conn = (RTMPConnection) cls.newInstance();
		}
		return conn;
	}

	/**
	 * @param debug the debug to set
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		RTMPConnManager.applicationContext = applicationContext;
	}

	public void destroy() throws Exception {
	}

}
