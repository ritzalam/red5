package org.red5.server.net.rtmpt;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.protocol.SimpleProtocolCodecFactory;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Handler for RTMPT messages.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class RTMPTHandler extends RTMPHandler implements ApplicationContextAware {

    /**
     * Logger
     */
    protected static Log log = LogFactory.getLog(RTMPTHandler.class.getName());

    /**
     * Handler constant
     */
    public static final String HANDLER_ATTRIBUTE = "red5.RMPTHandler";

    /**
     * Protocol codec factory
     */
    protected SimpleProtocolCodecFactory codecFactory;
    /**
     * App context
     */
	protected ApplicationContext appCtx;

	/**
     * Setter for codec factory
     *
     * @param factory  Codec factory to use
     */
    public void setCodecFactory(SimpleProtocolCodecFactory factory) {
		this.codecFactory = factory;
	}

	/**
     * Getter for codec factory
     *
     * @return Codec factory
     */
    public SimpleProtocolCodecFactory getCodecFactory() {
		return this.codecFactory;
	}

    /**
     * Handle raw buffer reciept
     * @param conn        RTMP connection
     * @param state       Protocol state
     * @param in          Byte buffer with input raw data
     */
    private void rawBufferRecieved(RTMPConnection conn, ProtocolState state,
			ByteBuffer in) {
		final RTMP rtmp = (RTMP) state;

		if (rtmp.getState() != RTMP.STATE_HANDSHAKE) {
			log.warn("Raw buffer after handshake, something odd going on");
		}

		ByteBuffer out = ByteBuffer
				.allocate((Constants.HANDSHAKE_SIZE * 2) + 1);

		if (log.isDebugEnabled()) {
			log.debug("Writing handshake reply");
			log.debug("handskake size:" + in.remaining());
		}

		out.put((byte) 0x03);
		out.fill((byte) 0x00, Constants.HANDSHAKE_SIZE);
		out.put(in).flip();

		conn.rawWrite(out);
	}

	/** {@inheritDoc} */
    @Override
	public void messageReceived(RTMPConnection conn, ProtocolState state,
			Object in) throws Exception {
		if (in instanceof ByteBuffer) {
			rawBufferRecieved(conn, state, (ByteBuffer) in);
			((ByteBuffer) in).release();
			in = null;
		} else {
			super.messageReceived(conn, state, in);
		}
	}

	/** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
		this.appCtx = appCtx;
	}

    /**
     * Creates RTMP connection
     * @return       RTMP connection
     */
    RTMPTConnection createRTMPTConnection() {
		RTMPTConnection client =
			(RTMPTConnection) appCtx.getBean("rtmptConnection");
		client.setRTMPTHandle(this);
		return client;
	}
}
