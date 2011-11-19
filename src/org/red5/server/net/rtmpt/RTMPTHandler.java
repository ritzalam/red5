package org.red5.server.net.rtmpt;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmpt.codec.RTMPTCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for RTMPT messages.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPTHandler extends RTMPHandler {

	/**
	 * Logger
	 */
	private static final Logger log = LoggerFactory.getLogger(RTMPTHandler.class);

	/**
	 * Handler constant
	 */
	public static final String HANDLER_ATTRIBUTE = "red5.RMPTHandler";

	/**
	 * Protocol codec factory
	 */
	protected RTMPTCodecFactory codecFactory;

	/**
	 * Setter for codec factory
	 *
	 * @param factory  Codec factory to use
	 */
	public void setCodecFactory(RTMPTCodecFactory factory) {
		this.codecFactory = factory;
	}

	/**
	 * Getter for codec factory
	 *
	 * @return Codec factory
	 */
	public RTMPTCodecFactory getCodecFactory() {
		return this.codecFactory;
	}
	
	/**
	 * Return hostname for URL.
	 * 
	 * @param url
	 *            URL
	 * @return Hostname from that URL
	 */
	@Override
	protected String getHostname(String url) {
		log.debug("url: {}", url);
		String[] parts = url.split("/");
		if (parts.length == 2) {
			// TODO: is this a good default hostname?
			return "";
		} else {
			String host = parts[2];
			// Strip out default port in case the client
			// added the port explicitly.
			if (host.endsWith(":80")) {
				// Remove default port from connection string
				return host.substring(0, host.length() - 3);
			}
			return host;
		}
	}

	/**
	 * Handle raw buffer received
	 * @param conn        RTMP connection
	 * @param state       Protocol state
	 * @param in          Byte buffer with input raw data
	 */
	private void rawBufferReceived(RTMPTConnection conn, RTMP state, IoBuffer in) {
		log.debug("rawBufferRecieved: {}", in);
		if (state.getState() != RTMP.STATE_HANDSHAKE) {
			log.warn("Raw buffer after handshake, something odd going on");
		}
		log.debug("Writing handshake reply, handskake size: {}", in.remaining());
		RTMPHandshake shake = new InboundHandshake();
		shake.setHandshakeType(RTMPConnection.RTMP_NON_ENCRYPTED);
		conn.rawWrite(shake.doHandshake(in));
	}

	/** {@inheritDoc} */
	@Override
	public void messageReceived(Object in, IoSession session) throws Exception {
		log.debug("messageReceived");
		if (in instanceof IoBuffer) {
			RTMPTConnection conn = (RTMPTConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
			RTMP state = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
			log.trace("state: {}", state);
			rawBufferReceived(conn, state, (IoBuffer) in);
			((IoBuffer) in).free();
			in = null;
		} else {
			super.messageReceived(in, session);
		}
	}
}
