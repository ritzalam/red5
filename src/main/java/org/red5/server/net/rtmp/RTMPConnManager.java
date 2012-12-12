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

import org.apache.commons.lang3.RandomStringUtils;
import org.red5.server.BaseConnection;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class RTMPConnManager implements IRTMPConnManager, ApplicationContextAware {

	private static final Logger log = LoggerFactory.getLogger(RTMPConnManager.class);

	private ConcurrentMap<Integer, RTMPConnection> connMap = new ConcurrentHashMap<Integer, RTMPConnection>();

	private ConcurrentMap<String, Integer> sessionMap = new ConcurrentHashMap<String, Integer>();

	private ApplicationContext appCtx;

	public RTMPConnection createConnection(Class<?> connCls) {
		RTMPConnection conn = null;
		if (RTMPConnection.class.isAssignableFrom(connCls)) {
			try {
				conn = createConnectionInstance(connCls);
				connMap.put(conn.getId(), conn);
				log.debug("Connection created, id: {}", conn.getId());
			} catch (Exception ex) {
				log.warn("Exception creating connection", ex);
			}
		}
		return conn;
	}

	public RTMPConnection getConnection(int clientId) {
		return connMap.get(clientId);
	}

	public RTMPConnection getConnectionBySessionId(String sessionId) {
		Integer clientId = sessionMap.get(sessionId);
		if (clientId != null) {
			return getConnection(clientId);
		}
		return null;
	}	
	
	public RTMPConnection removeConnection(int clientId) {
		log.debug("Removing connection with id: {}", clientId);
		// remove the conn
		RTMPConnection conn = connMap.remove(clientId);
		if (conn.getSessionId() != null) {
			// also remove session map entry
			sessionMap.remove(conn.getSessionId());
		}
		return conn;
	}

	public Collection<RTMPConnection> removeConnections() {
		ArrayList<RTMPConnection> list = new ArrayList<RTMPConnection>(connMap.size());
		list.addAll(connMap.values());
		return list;
	}

	public RTMPConnection createConnectionInstance(Class<?> cls) throws Exception {
		final Integer clientId = BaseConnection.getNextClientId();
		RTMPConnection conn = null;
		if (cls == RTMPMinaConnection.class) {
			conn = (RTMPMinaConnection) appCtx.getBean("rtmpMinaConnection");
			conn.setId(clientId);
		} else if (cls == RTMPTConnection.class) {
			conn = (RTMPTConnection) appCtx.getBean("rtmptConnection");
			conn.setId(clientId);
			String sessionId = RandomStringUtils.randomAlphanumeric(13).toUpperCase();
			log.debug("Generated session id: {}", sessionId);
			((RTMPTConnection) conn).setSessionId(sessionId);
			sessionMap.put(sessionId, clientId);
		} else {
			conn = (RTMPConnection) cls.newInstance();
			conn.setId(clientId);
		}
		// set the scheduling service for easy access in the connection
		conn.setSchedulingService((ISchedulingService) appCtx.getBean(ISchedulingService.BEAN_NAME));
		return conn;
	}
	
	public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
		this.appCtx = appCtx;
	}
	
}
