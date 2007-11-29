package org.red5.server.net.mrtmp;

import org.red5.server.net.rtmp.RTMPConnection;

public interface IMRTMPOriginManager extends IMRTMPManager {
	/**
	 * Associate the client to a MRTMP connection so that the packet
	 * will be sent via this MRTMP connection.
	 * The association has different impacts on persistent and polling
	 * connections. For persistent connection, the mapping is static while
	 * for polling connection, the mapping is dynamic and might not be
	 * honored.
	 * @param clientId
	 * @param conn
	 */
	void associate(RTMPConnection rtmpConn, IMRTMPConnection mrtmpConn);
	
	/**
	 * Deassociate the client from the MRTMP connection previously
	 * associated to.
	 * @param rtmpConn
	 */
	void dissociate(RTMPConnection rtmpConn);
}
