package org.red5.server.net.rtmpt;

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

import java.util.Arrays;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.red5.server.api.Red5;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmpt.codec.RTMPTCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for RTMPT messages.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
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
	 * Handle raw buffer reciept
	 * @param conn        RTMP connection
	 * @param state       Protocol state
	 * @param in          Byte buffer with input raw data
	 */
	private void rawBufferRecieved(RTMPTConnection conn, ProtocolState state, IoBuffer in) {
		final RTMP rtmp = (RTMP) state;

		if (rtmp.getState() != RTMP.STATE_HANDSHAKE) {
			log.warn("Raw buffer after handshake, something odd going on");
		}

		IoBuffer out = IoBuffer.allocate((Constants.HANDSHAKE_SIZE * 2) + 1);

		log.debug("Writing handshake reply, handskake size: {}", in.remaining());

		if (in.get(4) == 0) {
			log.debug("Using old style handshake");
			out = IoBuffer.allocate((Constants.HANDSHAKE_SIZE * 2) + 1);
			out.put((byte) RTMPConnection.RTMP_NON_ENCRYPTED);
			// set server uptime in seconds
			out.putInt((int) Red5.getUpTime() / 1000);
			out.put(RTMPHandshake.HANDSHAKE_PAD_BYTES).put(in).flip();
		} else {
			log.debug("Using new style handshake");
			//save resource by only doing this after the first request
			if (RTMPHandshake.HANDSHAKE_PAD_BYTES == null) {
				RTMPHandshake.HANDSHAKE_PAD_BYTES = new byte[Constants.HANDSHAKE_SIZE - 4];
				//fill pad bytes
				Arrays.fill(RTMPHandshake.HANDSHAKE_PAD_BYTES, (byte) 0x00);
			}
			RTMPHandshake shake = new InboundHandshake();
			out = shake.doHandshake(in);
		}
		conn.rawWrite(out);
	}

	/** {@inheritDoc} */
	@Override
	public void messageReceived(Object in, IoSession session) throws Exception {
		if (in instanceof IoBuffer) {
			RTMPTConnection conn = (RTMPTConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
			RTMP state = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
			rawBufferRecieved(conn, state, (IoBuffer) in);
			((IoBuffer) in).free();
			in = null;
		} else {
			super.messageReceived(in, session);
		}
	}
}
