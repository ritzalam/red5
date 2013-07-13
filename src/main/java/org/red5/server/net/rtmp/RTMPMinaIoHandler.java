/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmp;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmpe.RTMPEIoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles all RTMP protocol events fired by the MINA framework.
 */
public class RTMPMinaIoHandler extends IoHandlerAdapter {

	private static Logger log = LoggerFactory.getLogger(RTMPMinaIoHandler.class);

	/**
	 * RTMP events handler
	 */
	protected IRTMPHandler handler;

	protected ProtocolCodecFactory codecFactory;

	/** {@inheritDoc} */
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		log.debug("Session created");
		// add rtmpe filter
		session.getFilterChain().addFirst("rtmpeFilter", new RTMPEIoFilter());
		// add protocol filter next
		session.getFilterChain().addLast("protocolFilter", new ProtocolCodecFilter(codecFactory));
		// create a connection
		RTMPMinaConnection conn = createRTMPMinaConnection();
		// add session to the connection
		conn.setIoSession(session);
		// add the handler
		conn.setHandler(handler);
		// add the connections session id for look up using the connection manager
		session.setAttribute(RTMPConnection.RTMP_SESSION_ID, conn.getSessionId());
		// add the in-bound handshake
		session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, new InboundHandshake());
	}

	/** {@inheritDoc} */
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		log.info("Session opened: {}", session.getId());
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		log.trace("Session id: {}", sessionId);
		RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
		handler.connectionOpened(conn);
	}

	/** {@inheritDoc} */
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		log.info("Session closed: {}", session.getId());
		if (log.isTraceEnabled()) {
			log.trace("Session attributes: {}", session.getAttributeKeys());
		}
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		if (sessionId != null) {
			log.trace("Session id: {}", sessionId);
			RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
			if (conn != null) {
				// fire-off closed event
				handler.connectionClosed(conn);
				// clear any session attributes we may have previously set
				// TODO: verify this cleanup code is necessary. The session is over and will be garbage collected surely?
				if (session.containsAttribute(RTMPConnection.RTMP_HANDSHAKE)) {
					session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
				}
				if (session.containsAttribute(RTMPConnection.RTMPE_CIPHER_IN)) {
					session.removeAttribute(RTMPConnection.RTMPE_CIPHER_IN);
					session.removeAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
				}
				//session.suspendWrite();
			} else {
				log.warn("Connection was null in session");
			}
		} else {
			log.debug("Connections session id was null in session, may already be closed");
		}
	}

	/**
	 * Handle raw buffer receiving event.
	 *
	 * @param in
	 *            Data buffer
	 * @param session
	 *            I/O session, that is, connection between two endpoints
	 */
	protected void rawBufferRecieved(IoBuffer in, IoSession session) {
		log.trace("rawBufferRecieved: {}", in);
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		log.trace("Session id: {}", sessionId);
		RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
		RTMPHandshake handshake = (RTMPHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
		if (handshake != null) {
			if (conn.getStateCode() != RTMP.STATE_HANDSHAKE) {
				log.warn("Raw buffer after handshake, something odd going on");
			}
			log.debug("Handshake - server phase 1 - size: {}", in.remaining());
			IoBuffer out = handshake.doHandshake(in);
			if (out != null) {
				log.trace("Output: {}", out);
				session.write(out);
				//if we are connected and doing encryption, add the ciphers
				if (conn.getStateCode() == RTMP.STATE_CONNECTED) {
					// remove handshake from session now that we are connected
					// if we are using encryption then put the ciphers in the session
					if (handshake.getHandshakeType() == RTMPConnection.RTMP_ENCRYPTED) {
						log.debug("Adding ciphers to the session");
						session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherIn());
						session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherOut());
					}
				}
			}
		} else {
			log.warn("Handshake was not found for this connection: {}", conn);
			log.debug("Session: {}", session.getId());
		}
	}

	/** {@inheritDoc} */
	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		log.trace("messageReceived");
		if (message instanceof IoBuffer) {
			rawBufferRecieved((IoBuffer) message, session);
		} else {
			String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
			log.trace("Session id: {}", sessionId);
			RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
			conn.handleMessageReceived(message);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		log.trace("messageSent");
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		log.trace("Session id: {}", sessionId);
		RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);
		handler.messageSent(conn, message);
	}

	/** {@inheritDoc} */
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.warn("Exception caught on session: {}", session.getId(), cause.getCause());
		if (session.containsAttribute(RTMPConnection.RTMP_SESSION_ID)) {
			log.debug("Forcing call to sessionClosed");
			sessionClosed(session);
		}
	}

	/**
	 * Setter for handler.
	 *
	 * @param handler RTMP events handler
	 */
	public void setHandler(IRTMPHandler handler) {
		this.handler = handler;
	}

	/**
	 * @param codecFactory the codecFactory to set
	 */
	public void setCodecFactory(ProtocolCodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}

	protected RTMPMinaConnection createRTMPMinaConnection() {
		return (RTMPMinaConnection) RTMPConnManager.getInstance().createConnection(RTMPMinaConnection.class);
	}
}
