package org.red5.server.net.rtmp;

import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.net.mrtmp.IMRTMPConnection;
import org.red5.server.net.mrtmp.IMRTMPEdgeManager;
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
		synchronized (state) {
			if (state.getState() == RTMP.STATE_CONNECTED) {
				needNotifyOrigin = true;
				// now we are disconnecting ourselves
				state.setState(RTMP.STATE_EDGE_DISCONNECTING);
			}
		}
		if (needNotifyOrigin) {
			IMRTMPConnection conn = mrtmpManager.lookupMRTMPConnection(this);
			if (conn != null) {
				conn.disconnect(getId());
			}
		}
		synchronized (state) {
			if (state.getState() == RTMP.STATE_DISCONNECTED) {
				return;
			} else {
				state.setState(RTMP.STATE_DISCONNECTED);
			}
		}
		super.close();
	}

	@Override
	protected void startWaitForHandshake(ISchedulingService service) {
		// FIXME do nothing to avoid disconnect.
	}
}
