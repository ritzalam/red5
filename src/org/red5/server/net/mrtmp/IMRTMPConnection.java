package org.red5.server.net.mrtmp;

import org.red5.server.net.rtmp.message.Packet;

public interface IMRTMPConnection {
	/**
	 * Send RTMP packet to other side
	 * @param clientId
	 * @param packet
	 */
	void write(int clientId, Packet packet);
	
	/**
	 * Send connect message to other side
	 * @param clientId
	 */
	void connect(int clientId);
	
	/**
	 * Send disconnect message to other side
	 * @param clientId
	 */
	void disconnect(int clientId);
	
	void close();
}
