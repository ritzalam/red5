package org.red5.server.net.rtmpt;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.BaseRTMPClientHandler;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPT client object
 * 
 * @author Anton Lebedevich
 */
public class RTMPTClient extends BaseRTMPClientHandler {

	private static final Logger log = LoggerFactory
			.getLogger(RTMPTClient.class);

	// guarded by this
	private RTMPTClientConnector connector = null;
	
	public RTMPTClient() {
	}

	public Map<String, Object> makeDefaultConnectionParams(String server,
			int port, String application) {
		Map<String, Object> params = super.makeDefaultConnectionParams(server,
				port, application);

		if (!params.containsKey("tcUrl")) {
			params.put("tcUrl", "rtmpt://" + server + ':' + port + '/'
					+ application);
		}

		return params;
	}

	protected synchronized void startConnector(String server, int port) {
		connector = new RTMPTClientConnector(server, port, this);
		log.debug("Created connector {}", connector);
		connector.start();
	}

	/** {@inheritDoc} */
	@Override
	public void messageReceived(RTMPConnection conn, ProtocolState state,
			Object in) throws Exception {
		if (in instanceof IoBuffer) {
			rawBufferRecieved(conn, state, (IoBuffer) in);
		} else {
			super.messageReceived(conn, state, in);
		}
	}

	/**
	 * Handle raw buffer receipt
	 * 
	 * @param conn
	 *            RTMP connection
	 * @param state
	 *            Protocol state
	 * @param in
	 *            IoBuffer with input raw data
	 */
	private void rawBufferRecieved(RTMPConnection conn, ProtocolState state,
			IoBuffer in) {

		log.debug("Handshake 3d phase - size: {}", in.remaining());
		in.skip(1);
		IoBuffer out = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
		in.limit(in.position() + Constants.HANDSHAKE_SIZE);
		out.put(in);
		out.flip();
		conn.rawWrite(out);
		connectionOpened(conn, conn.getState());
	}

	public synchronized void disconnect() {
		if (connector != null) {
			connector.setStopRequested(true);
			connector.interrupt();
		}
		super.disconnect();
	}
}
