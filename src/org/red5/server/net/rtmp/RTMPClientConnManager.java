package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTMPClientConnManager implements IRTMPConnManager {
	
	private static final Logger log = LoggerFactory.getLogger(RTMPClientConnManager.class);
	
	protected RTMPConnection rtmpConn;

	synchronized public RTMPConnection getConnection() {
		log.debug("Returning first map entry");
		return rtmpConn;
	}

	synchronized public RTMPConnection getConnection(int clientId) {
		log.debug("Returning map entry for client id: {}", clientId);
		if (clientId == 0) {
			return rtmpConn;
		} else {
			return null;
		}
	}

	synchronized public RTMPConnection removeConnection(int clientId) {
		RTMPConnection connReturn = null;
		if (clientId == 0) {
			connReturn = rtmpConn;
			rtmpConn = null;
		}
		return connReturn;
	}

	synchronized public Collection<RTMPConnection> removeConnections() {
		rtmpConn = null;
		return null;
	}

	synchronized public RTMPConnection createConnection(Class connCls) {
		log.debug("Creating connection, class: {}", connCls.getName());
		if (!RTMPConnection.class.isAssignableFrom(connCls)) {
			throw new IllegalArgumentException("Class was not assignable");
		}
		try {
			RTMPConnection conn = (RTMPConnection) connCls.newInstance();
			conn.setId(0);
			log.debug("Connection id set {}", conn.getId());
			rtmpConn = conn;
			log.debug("Connection added to the map");
			return conn;
		} catch (Exception e) {
			log.error("RTMPConnection creation failed", e);
			throw new RuntimeException(e);
		}
	}
}
