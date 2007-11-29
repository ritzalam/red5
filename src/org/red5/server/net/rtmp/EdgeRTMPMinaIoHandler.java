package org.red5.server.net.rtmp;

public class EdgeRTMPMinaIoHandler extends RTMPMinaIoHandler {
	private IRTMPConnManager rtmpConnManager;

	@Override
	protected RTMPMinaConnection createRTMPMinaConnection() {
		return (RTMPMinaConnection) rtmpConnManager.createConnection(EdgeRTMPMinaConnection.class);
	}

	public void setRtmpConnManager(IRTMPConnManager rtmpConnManager) {
		this.rtmpConnManager = rtmpConnManager;
	}
}
