package org.red5.server.net.mrtmp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.red5.server.api.IConnection;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPOriginConnection;

public class SimpleMRTMPOriginManager implements IMRTMPOriginManager {
	private static final Logger log = LoggerFactory.getLogger(SimpleMRTMPOriginManager.class);
	
	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private Set<IMRTMPConnection> connSet = new HashSet<IMRTMPConnection>();
	private Map<RTMPConnection, IMRTMPConnection> clientToConnMap;
	private OriginMRTMPHandler originMRTMPHandler;
	
	public SimpleMRTMPOriginManager() {
		// XXX Use HashMap instead of WeakHashMap temporarily
		// to avoid package routing issue before Terracotta
		// integration.
		clientToConnMap = Collections.synchronizedMap(
				new HashMap<RTMPConnection, IMRTMPConnection>());
	}

	public void setOriginMRTMPHandler(OriginMRTMPHandler originMRTMPHandler) {
		this.originMRTMPHandler = originMRTMPHandler;
	}

	public boolean registerConnection(IMRTMPConnection conn) {
		lock.writeLock().lock();
		try {
			return connSet.add(conn);
		} finally {
			lock.writeLock().unlock();
		}
	}

	public boolean unregisterConnection(IMRTMPConnection conn) {
		boolean ret;
		ArrayList<RTMPConnection> list = new ArrayList<RTMPConnection>();
		lock.writeLock().lock();
		try {
			ret = connSet.remove(conn);
			if (ret) {
				for (Iterator<Entry<RTMPConnection, IMRTMPConnection>> iter = clientToConnMap.entrySet().iterator(); iter.hasNext(); ) {
					Entry<RTMPConnection, IMRTMPConnection> entry = iter.next();
					if (entry.getValue() == conn) {
						list.add(entry.getKey());
					}
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
		// close all RTMPOriginConnections
		for (RTMPConnection rtmpConn : list) {
			log.debug("Close RTMPOriginConnection " + rtmpConn.getId() + " due to MRTMP Connection closed!");
			originMRTMPHandler.closeConnection((RTMPOriginConnection) rtmpConn);
		}
		return ret;
	}

	public void associate(RTMPConnection rtmpConn, IMRTMPConnection mrtmpConn) {
		clientToConnMap.put(rtmpConn, mrtmpConn);
	}

	public void dissociate(RTMPConnection rtmpConn) {
		clientToConnMap.remove(rtmpConn);
	}

	public IMRTMPConnection lookupMRTMPConnection(RTMPConnection rtmpConn) {
		lock.readLock().lock();
		try {
			IMRTMPConnection conn = clientToConnMap.get(rtmpConn);
			if (conn != null && !connSet.contains(conn)) {
				clientToConnMap.remove(rtmpConn);
				conn = null;
			}
			// mrtmp connection not found, we locate the next mrtmp connection
			// when the connection is not persistent.
			if (conn == null && !rtmpConn.getType().equals(IConnection.PERSISTENT)) {
				if (connSet.size() > 0) {
					conn = connSet.iterator().next();
				}
			}
			// TODO handle conn == null case
			return conn;
		} finally {
			lock.readLock().unlock();
		}
	}

}
