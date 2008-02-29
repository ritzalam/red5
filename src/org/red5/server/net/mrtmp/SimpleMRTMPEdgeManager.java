package org.red5.server.net.mrtmp;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.red5.server.net.rtmp.IRTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;

/**
 * A simple Edge connection manager that only manages one Edge/Origin connection.
 * @author Steven Gong (steven.gong@gmail.com)
 * @version $Id$
 */
public class SimpleMRTMPEdgeManager implements IMRTMPEdgeManager {
	private IRTMPConnManager rtmpConnManager;
	private List<IMRTMPConnection> connList = new ArrayList<IMRTMPConnection>();
	
	public void setRtmpConnManager(IRTMPConnManager rtmpConnManager) {
		this.rtmpConnManager = rtmpConnManager;
	}

	public boolean registerConnection(IMRTMPConnection conn) {
		return connList.add(conn);
	}

	public boolean unregisterConnection(IMRTMPConnection conn) {
		boolean ret = connList.remove(conn);
		if (ret) {
			Collection<RTMPConnection> rtmpConns = rtmpConnManager.removeConnections();
			for (Iterator<RTMPConnection> iter = rtmpConns.iterator(); iter.hasNext(); ) {
				RTMPConnection rtmpConn = iter.next();
				rtmpConn.close();
			}
		}
		return ret;
	}

	public IMRTMPConnection lookupMRTMPConnection(RTMPConnection conn) {
		if (connList.size() > 0) {
			return connList.get(0);
		} else {
			return null;
		}
	}
	
}
