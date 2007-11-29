package org.red5.server.net.mrtmp;

import org.red5.server.net.rtmp.RTMPConnection;

public interface IMRTMPManager {
	/**
	 * Map a client to an Edge/Origin MRTMP connection.
	 * On Edge, the server will find an Origin connection per routing logic.
	 * On Origin, the server will send to the original in-coming connection
	 * if the client connection type is persistent. Or the latest in-coming
	 * connection will be used.
	 * @param conn
	 * @return
	 */
	IMRTMPConnection lookupMRTMPConnection(RTMPConnection conn);
	
	/**
	 * Register a MRTMP connection so that it can be later
	 * been looked up.
	 * @param conn
	 * @return whether the registration is successful
	 */
	boolean registerConnection(IMRTMPConnection conn);
	
	/**
	 * Unregister a MRTMP connection.
	 * @param conn
	 * @return whether the registration is successful
	 */
	boolean unregisterConnection(IMRTMPConnection conn);
}
