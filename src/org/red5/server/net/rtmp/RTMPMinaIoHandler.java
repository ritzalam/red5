/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
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
import org.apache.mina.filter.logging.LoggingFilter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmpe.RTMPEIoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Handles all RTMP protocol events fired by the MINA framework.
 */
public class RTMPMinaIoHandler extends IoHandlerAdapter implements ApplicationContextAware {

	private static Logger log = LoggerFactory.getLogger(RTMPMinaIoHandler.class);

	/**
	 * RTMP events handler
	 */
	protected IRTMPHandler handler;

	/**
	 * Application context
	 */
	protected ApplicationContext appCtx;

	protected IRTMPConnManager rtmpConnManager;

	/** {@inheritDoc} */
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		log.debug("Session created");
		// moved protocol state from connection object to RTMP object
		RTMP rtmp = new RTMP();
		session.setAttribute(ProtocolState.SESSION_KEY, rtmp);
		//add rtmpe filter
		session.getFilterChain().addFirst("rtmpeFilter", new RTMPEIoFilter());
		//add protocol filter next
		ProtocolCodecFactory codecFactory = (ProtocolCodecFactory) appCtx.getBean("rtmpCodecFactory");
		session.getFilterChain().addLast("protocolFilter", new ProtocolCodecFilter(codecFactory));
		if (log.isTraceEnabled()) {
			session.getFilterChain().addLast("logger", new LoggingFilter());
		}
		//create a connection
		RTMPMinaConnection conn = createRTMPMinaConnection();
		conn.setIoSession(session);
		conn.setState(rtmp);
		//add the connection
		session.setAttribute(RTMPConnection.RTMP_CONNECTION_KEY, conn);
		//add the inbound handshake
		session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, new InboundHandshake());
	}

	/** {@inheritDoc} */
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		log.debug("Session opened");
		super.sessionOpened(session);
		// get protocol state
		RTMP rtmp = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
		handler.connectionOpened((RTMPMinaConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY), rtmp);
	}

	/** {@inheritDoc} */
	@Override
	public void sessionClosed(IoSession session) throws Exception {
		log.debug("Session closed");
		RTMP rtmp = (RTMP) session.removeAttribute(ProtocolState.SESSION_KEY);
		log.debug("RTMP state: {}", rtmp);
		RTMPMinaConnection conn = (RTMPMinaConnection) session.removeAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
		try {
			conn.sendPendingServiceCallsCloseError();
			// fire-off closed event
			handler.connectionClosed(conn, rtmp);
			// clear any session attributes we may have previously set
			// TODO: verify this cleanup code is necessary. The session is over and will be garbage collected surely?
			session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
			session.removeAttribute(RTMPConnection.RTMPE_CIPHER_IN);
			session.removeAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
		} finally {
			// DW we *always* remove the connection from the RTMP manager even if unexpected exception gets thrown e.g. by handler.connectionClosed
			// Otherwise connection stays around forever, and everything it references e.g. Client, ...
			rtmpConnManager.removeConnection(conn.getId());
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
		log.debug("rawBufferRecieved: {}", in);
		final RTMP rtmp = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
		log.debug("state: {}", rtmp);
		final RTMPMinaConnection conn = (RTMPMinaConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
		RTMPHandshake handshake = (RTMPHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
		if (handshake != null) {
			if (rtmp.getState() != RTMP.STATE_HANDSHAKE) {
				log.warn("Raw buffer after handshake, something odd going on");
			}
			log.debug("Handshake - server phase 1 - size: {}", in.remaining());
			IoBuffer out = handshake.doHandshake(in);
			if (out != null) {
				log.debug("Output: {}", out);
				session.write(out);
				//if we are connected and doing encryption, add the ciphers
				if (rtmp.getState() == RTMP.STATE_CONNECTED) {
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
			log.debug("RTMP state: {} Session: {}", rtmp, session);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		log.trace("messageReceived");
		if (message instanceof IoBuffer) {
			rawBufferRecieved((IoBuffer) message, session);
		} else {
			log.trace("Setting connection local");
			Red5.setConnectionLocal((IConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY));
			handler.messageReceived(message, session);
			log.trace("Removing connection local");
			Red5.setConnectionLocal(null);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		log.debug("messageSent");
		final RTMPMinaConnection conn = (RTMPMinaConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
		handler.messageSent(conn, message);
	}

	/** {@inheritDoc} */
	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.warn("Exception caught {}", cause.getMessage());
		if (log.isDebugEnabled()) {
			log.error("Exception detail", cause);
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

	public void setRtmpConnManager(IRTMPConnManager rtmpConnManager) {
		this.rtmpConnManager = rtmpConnManager;
	}

	protected IRTMPConnManager getRtmpConnManager() {
		return rtmpConnManager;
	}

	/** {@inheritDoc} */
	public void setApplicationContext(ApplicationContext appCtx) throws BeansException {
		log.debug("Setting application context: {} {}", appCtx.getDisplayName(), appCtx);
		this.appCtx = appCtx;
	}

	protected RTMPMinaConnection createRTMPMinaConnection() {
		return (RTMPMinaConnection) rtmpConnManager.createConnection(RTMPMinaConnection.class);
	}
}
