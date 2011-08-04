package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.red5.server.BaseConnection;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.net.mrtmp.EdgeRTMPMinaConnection;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class RTMPConnManager implements IRTMPConnManager, ApplicationContextAware {

	private static final Logger log = LoggerFactory.getLogger(RTMPConnManager.class);

	private ConcurrentMap<Integer, RTMPConnection> connMap = new ConcurrentHashMap<Integer, RTMPConnection>();

	private ReadWriteLock lock = new ReentrantReadWriteLock();

	private ApplicationContext appCtx;

	public RTMPConnection createConnection(Class<?> connCls) {
		if (!RTMPConnection.class.isAssignableFrom(connCls)) {
			return null;
		}
		try {
			RTMPConnection conn = createConnectionInstance(connCls);
			lock.writeLock().lock();
			try {
				int clientId = BaseConnection.getNextClientId();
				conn.setId(clientId);
				connMap.put(clientId, conn);
				log.debug("Connection created, id: {}", conn.getId());
			} finally {
				lock.writeLock().unlock();
			}
			return conn;
		} catch (Exception e) {
			return null;
		}
	}

	public RTMPConnection getConnection(int clientId) {
		lock.readLock().lock();
		try {
			return connMap.get(clientId);
		} finally {
			lock.readLock().unlock();
		}
	}

	public RTMPConnection removeConnection(int clientId) {
		lock.writeLock().lock();
		try {
			log.debug("Removing connection with id: {}", clientId);
			return connMap.remove(clientId);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public Collection<RTMPConnection> removeConnections() {
		ArrayList<RTMPConnection> list = new ArrayList<RTMPConnection>(connMap.size());
		lock.writeLock().lock();
		try {
			list.addAll(connMap.values());
			return list;
		} finally {
			lock.writeLock().unlock();
		}
	}

	public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
		this.appCtx = appCtx;
	}

	public RTMPConnection createConnectionInstance(Class<?> cls) throws Exception {
		RTMPConnection conn = null;
		if (cls == RTMPMinaConnection.class) {
			conn = (RTMPMinaConnection) appCtx.getBean("rtmpMinaConnection");
		} else if (cls == EdgeRTMPMinaConnection.class) {
			conn = (EdgeRTMPMinaConnection) appCtx.getBean("rtmpEdgeMinaConnection");
		} else if (cls == RTMPTConnection.class) {
			conn = (RTMPTConnection) appCtx.getBean("rtmptConnection");
		} else {
			conn = (RTMPConnection) cls.newInstance();
		}
		//set the scheduling service for easy access in the connection
		conn.setSchedulingService((ISchedulingService) appCtx.getBean(ISchedulingService.BEAN_NAME));
		return conn;
	}
}
