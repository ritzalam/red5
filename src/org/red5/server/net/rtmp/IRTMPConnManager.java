package org.red5.server.net.rtmp;

import java.util.Collection;

public interface IRTMPConnManager {
	RTMPConnection getConnection(int clientId);
	RTMPConnection createConnection(Class connCls);
	RTMPConnection removeConnection(int clientId);
	Collection<RTMPConnection> removeConnections();
}
