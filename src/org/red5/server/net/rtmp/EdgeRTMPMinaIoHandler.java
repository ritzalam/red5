package org.red5.server.net.rtmp;

public class EdgeRTMPMinaIoHandler extends RTMPMinaIoHandler {
	@Override
	protected RTMPMinaConnection createRTMPMinaConnection() {
		return (RTMPMinaConnection) getRtmpConnManager()
				.createConnection(EdgeRTMPMinaConnection.class);
	}
}
