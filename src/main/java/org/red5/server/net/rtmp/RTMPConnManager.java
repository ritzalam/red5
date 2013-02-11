/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Responsible for management and creation of RTMP based connections.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public class RTMPConnManager implements IConnectionManager<RTMPConnection>, ApplicationContextAware {

	private static final Logger log = LoggerFactory.getLogger(RTMPConnManager.class);

	private ConcurrentMap<Integer, RTMPConnection> connMap = new ConcurrentHashMap<Integer, RTMPConnection>();

	private static ApplicationContext applicationContext;
	
	public static RTMPConnManager getInstance() {
		return applicationContext.getBean(RTMPConnManager.class);
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
				// set the scheduling service for easy access in the connection
				conn.setSchedulingService((ISchedulingService) applicationContext.getBean(ISchedulingService.BEAN_NAME));
				log.trace("Connection created: {}", conn);
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
		// add to local map
		connMap.put(conn.getId(), conn);
	}

	/**
	 * Returns a connection for a given client id.
	 * 
	 * @param clientId
	 * @return connection if found and null otherwise
	 */
	public RTMPConnection getConnection(int clientId) {
		log.debug("Getting connection by client id: {}", clientId);
		return connMap.get(clientId);
	}

	/**
	 * Returns a connection for a given session id.
	 * 
	 * @param sessionId
	 * @return connection if found and null otherwise
	 */
	public RTMPConnection getConnectionBySessionId(String sessionId) {
		log.debug("Getting connection by session id: {}", sessionId);
		for (RTMPConnection conn : connMap.values()) {
			if (conn instanceof RTMPTConnection) {
				if (sessionId.equals(((RTMPTConnection) conn).getSessionId())) {
					return conn;
				}
			}
		}
		return null;
	}	
	
	/**
	 * Removes a connection by the given clientId.
	 * 
	 * @param clientId
	 * @return connection that was removed
	 */
	public RTMPConnection removeConnection(int clientId) {
		log.debug("Removing connection with id: {}", clientId);
		// remove the conn
		return connMap.remove(clientId);
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

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		RTMPConnManager.applicationContext = applicationContext;
	}
	
}
