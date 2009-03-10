package org.red5.server.net.rtmp;

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

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.server.api.Red5;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Handles all RTMP protocol events fired by the MINA framework.
 */
public class RTMPMinaIoHandler extends IoHandlerAdapter implements
		ApplicationContextAware {
	/**
	 * Logger
	 */
	private static Logger log = LoggerFactory
			.getLogger(RTMPMinaIoHandler.class);

	/**
	 * RTMP events handler
	 */
	private IRTMPHandler handler;

	/**
	 * Mode
	 */
	private boolean mode = RTMP.MODE_SERVER;

	/**
	 * Application context
	 */
	@SuppressWarnings("unused")
	private ApplicationContext appCtx;

	/**
	 * RTMP protocol codec factory
	 */
	private ProtocolCodecFactory codecFactory;

	private IRTMPConnManager rtmpConnManager;

	/**
	 * Setter for handler.
	 * 
	 * @param handler RTMP events handler
	 */
	public void setHandler(IRTMPHandler handler) {
		this.handler = handler;
	}

	/**
	 * Setter for mode.
	 * 
	 * @param mode <code>true</code> if handler should work in server mode,
	 *            <code>false</code> otherwise
	 */
	public void setMode(boolean mode) {
		this.mode = mode;
	}

	/**
	 * Setter for codec factory.
	 * 
	 * @param codecFactory RTMP protocol codec factory
	 */
	public void setCodecFactory(ProtocolCodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}

	public void setRtmpConnManager(IRTMPConnManager rtmpConnManager) {
		this.rtmpConnManager = rtmpConnManager;
	}

	protected IRTMPConnManager getRtmpConnManager() {
		return rtmpConnManager;
	}

	/** {@inheritDoc} */
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		log.warn("Exception caught {}", cause.getMessage());
		if (log.isDebugEnabled()) {
			log.error("Exception detail: ", cause);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void messageReceived(IoSession session, Object in) throws Exception {
		log.trace("messageRecieved");
		final ProtocolState state = (ProtocolState) session
				.getAttribute(ProtocolState.SESSION_KEY);
		if (in instanceof ByteBuffer) {
			rawBufferRecieved(state, (ByteBuffer) in, session);
			return;
		}
		final RTMPMinaConnection conn = (RTMPMinaConnection) session
				.getAttachment();
		handler.messageReceived(conn, state, in);
	}

	/**
	 * Handle raw buffer receiving event.
	 * 
	 * @param state
	 *            Protocol state
	 * @param in
	 *            Data buffer
	 * @param session
	 *            I/O session, that is, connection between two endpoints
	 */
	protected void rawBufferRecieved(ProtocolState state, ByteBuffer in,
			IoSession session) {

		final RTMP rtmp = (RTMP) state;
		ByteBuffer out = null;
		final RTMPMinaConnection conn = (RTMPMinaConnection) session
				.getAttachment();
		conn.getWriteLock().lock();
		try {
			if (rtmp.getMode() == RTMP.MODE_SERVER) {
				if (rtmp.getState() != RTMP.STATE_HANDSHAKE) {
					log.warn("Raw buffer after handshake, something odd going on");
				}
				log.debug("Handshake 2nd phase - size: {}", in.remaining());
				//if the 5th byte is 0 then dont generate new-style handshake
				if (log.isTraceEnabled()) {
					byte[] bIn = in.array();
					log.debug("First few bytes (in): {},{},{},{},{},{},{},{},{},{}", 
							new Object[]{bIn[0],bIn[1],bIn[2],bIn[3],bIn[4],bIn[5],bIn[6],bIn[7],bIn[8],bIn[9]});
				}	
    			if (in.get(4) == 0) {
    				log.debug("Using old style handshake");
    				out = ByteBuffer.allocate((Constants.HANDSHAKE_SIZE * 2) + 1);
    				out.put((byte) 0x03);
    				// set server uptime in seconds
    				out.putInt((int) Red5.getUpTime() / 1000); //0x01
    				out.put(RTMPHandshake.HANDSHAKE_PAD_BYTES).put(in).flip();
    			} else {
    				log.debug("Using new style handshake");
    				RTMPHandshake shake = new RTMPHandshake();
    				out = shake.generateResponse(in);
    			}
				if (log.isTraceEnabled()) {
					byte[] bOut = out.array();
					log.debug("First few bytes (out): {},{},{},{},{},{},{},{},{},{}", 
							new Object[]{bOut[0],bOut[1],bOut[2],bOut[3],bOut[4],bOut[5],bOut[6],bOut[7],bOut[8],bOut[9]});
				}
				// Skip first 8 bytes when comparing the handshake, they seem to
				// be changed when connecting from a Mac client.
				rtmp.setHandshake(out, 9, Constants.HANDSHAKE_SIZE-8);
			} else {
				log.debug("Handshake 3d phase - size: {}", in.remaining());
				in.skip(1);
				out = ByteBuffer.allocate(Constants.HANDSHAKE_SIZE);
				int limit = in.limit();
				in.limit(in.position() + Constants.HANDSHAKE_SIZE);
				out.put(in);
				out.flip();
				in.limit(limit);
				in.skip(Constants.HANDSHAKE_SIZE);
			}
		} finally {
			conn.getWriteLock().unlock();
			if (out != null) {
				session.write(out);			
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		log.debug("messageSent");
		session.getAttribute(ProtocolState.SESSION_KEY);
		final RTMPMinaConnection conn = (RTMPMinaConnection) session
				.getAttachment();
		handler.messageSent(conn, message);
		if (mode == RTMP.MODE_CLIENT) {
			if (message instanceof ByteBuffer) {
				if (((ByteBuffer) message).limit() == Constants.HANDSHAKE_SIZE) {
					handler.connectionOpened(conn, (RTMP) session
							.getAttribute(ProtocolState.SESSION_KEY));
				}
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		super.sessionOpened(session);

		RTMP rtmp = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
		if (rtmp.getMode() == RTMP.MODE_CLIENT) {
			log.debug("Handshake 1st phase");
			ByteBuffer out = ByteBuffer.allocate(Constants.HANDSHAKE_SIZE + 1);
			out.put((byte) 0x03);
			out.put(RTMPHandshake.getHandshakeBytes());
			out.flip();
			session.write(out);
		} else {
			final RTMPMinaConnection conn = (RTMPMinaConnection) session
					.getAttachment();
			handler.connectionOpened(conn, rtmp);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		final RTMP rtmp = (RTMP) session
				.getAttribute(ProtocolState.SESSION_KEY);
		final RTMPMinaConnection conn = (RTMPMinaConnection) session
				.getAttachment();
		this.handler.connectionClosed(conn, rtmp);
		session.removeAttribute(ProtocolState.SESSION_KEY);
		session.setAttachment(null);
		rtmpConnManager.removeConnection(conn.getId());
	}

	/** {@inheritDoc} */
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		log.debug("Session created");
		// moved protocol state from connection object to RTMP object
		RTMP rtmp = new RTMP(mode);
		session.setAttribute(ProtocolState.SESSION_KEY, rtmp);
		session.getFilterChain().addFirst("protocolFilter",
				new ProtocolCodecFilter(codecFactory));
		if (log.isDebugEnabled()) {
			session.getFilterChain().addLast("logger", new LoggingFilter());
		}
		RTMPMinaConnection conn = createRTMPMinaConnection();
		conn.setIoSession(session);
		conn.setState(rtmp);
		session.setAttachment(conn);
	}

	/** {@inheritDoc} */
	public void setApplicationContext(ApplicationContext appCtx)
			throws BeansException {
		this.appCtx = appCtx;
	}

	protected RTMPMinaConnection createRTMPMinaConnection() {
		return (RTMPMinaConnection) rtmpConnManager
				.createConnection(RTMPMinaConnection.class);
	}
}
