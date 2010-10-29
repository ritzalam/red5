package org.red5.server.net.mrtmp;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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

import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;

public class EdgeRTMPMinaConnection extends RTMPMinaConnection {
	
	private IMRTMPEdgeManager mrtmpManager;
	
	public void setMrtmpManager(IMRTMPEdgeManager mrtmpManager) {
		this.mrtmpManager = mrtmpManager;
	}

	@Override
	public void close() {
		boolean needNotifyOrigin = false;
		RTMP state = getState();
		getWriteLock().lock();
		try{
			if (state.getState() == RTMP.STATE_CONNECTED) {
				needNotifyOrigin = true;
				// now we are disconnecting ourselves
				state.setState(RTMP.STATE_EDGE_DISCONNECTING);
			}
		} finally {
			getWriteLock().unlock();
		}
		if (needNotifyOrigin) {
			IMRTMPConnection conn = mrtmpManager.lookupMRTMPConnection(this);
			if (conn != null) {
				conn.disconnect(getId());
			}
		}
		getWriteLock().lock();
		try {
			if (state.getState() == RTMP.STATE_DISCONNECTED) {
				return;
			} else {
				state.setState(RTMP.STATE_DISCONNECTED);
			}
		} finally {
			getWriteLock().unlock();
		}
		super.close();
	}

	@Override
	protected void startWaitForHandshake(ISchedulingService service) {
		// FIXME: do nothing to avoid disconnect.
	}
}
