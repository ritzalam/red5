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

import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.SocketConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmpe.RTMPEIoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP client object. Initial client mode code by Christian Eckerle.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Christian Eckerle (ce@publishing-etc.de)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Steven Gong (steven.gong@gmail.com)
 * @author Anton Lebedevich (mabrek@gmail.com)
 * @author Tiago Daniel Jacobs (tiago@imdt.com.br)
 * @author Jon Valliere
 */
public class RTMPClient extends BaseRTMPClientHandler {

	private static final Logger log = LoggerFactory.getLogger(RTMPClient.class);

	protected static final int CONNECTOR_WORKER_TIMEOUT = 7000; // seconds

	// I/O handler
	private final RTMPClientIoHandler ioHandler;

	// Socket connector, disposed on disconnect
	protected SocketConnector socketConnector;

	// 
	protected ConnectFuture future;

	/** Constructs a new RTMPClient. */
	public RTMPClient() {
		ioHandler = new RTMPClientIoHandler();
		ioHandler.setMode(RTMP.MODE_CLIENT);
		ioHandler.setHandler(this);
		ioHandler.setRtmpConnManager(RTMPClientConnManager.getInstance());
	}

	public Map<String, Object> makeDefaultConnectionParams(String server, int port, String application) {
		Map<String, Object> params = super.makeDefaultConnectionParams(server, port, application);
		if (!params.containsKey("tcUrl")) {
			params.put("tcUrl", String.format("rtmp://%s:%s/%s", server, port, application));
		}
		return params;
	}

	@Override
	protected void startConnector(String server, int port) {
		socketConnector = new NioSocketConnector();
		socketConnector.setHandler(ioHandler);
		future = socketConnector.connect(new InetSocketAddress(server, port));
		future.addListener(new IoFutureListener<ConnectFuture>() {
			public void operationComplete(ConnectFuture future) {
				try {
					// will throw RuntimeException after connection error
					future.getSession();
				} catch (Throwable e) {
					socketConnector.dispose(false);
					//if there isn't an ClientExceptionHandler set, a 
					//RuntimeException may be thrown in handleException
					handleException(e);
				}
			}
		});
		// Now wait for the close to be completed
		future.awaitUninterruptibly(CONNECTOR_WORKER_TIMEOUT);
	}

	@Override
	public void disconnect() {
		if (future != null) {
			try {
				// close requesting that the pending messages are sent before the session is closed
				future.getSession().close(false);
				// now wait for the close to be completed
				future.awaitUninterruptibly(CONNECTOR_WORKER_TIMEOUT);
			} catch (Exception e) {
				log.warn("Exception during disconnect", e);
			} finally {
				// We can now dispose the connector
				socketConnector.dispose(false);
			}
		}
		super.disconnect();
	}
	
	protected class RTMPClientIoHandler extends RTMPMinaIoHandler {
		
		@Override
		public void sessionCreated(IoSession session) throws Exception {
			log.debug("Session created");
			// moved protocol state from connection object to RTMP object
			RTMP rtmp = new RTMP(mode);
			session.setAttribute(ProtocolState.SESSION_KEY, rtmp);
			//add rtmpe filter
			session.getFilterChain().addFirst("rtmpeFilter", new RTMPEIoFilter());
			//add protocol filter next
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
			//create inbound or outbound handshaker
			if (rtmp.getMode() == RTMP.MODE_CLIENT) {
				// create an outbound handshake
				OutboundHandshake outgoingHandshake = new OutboundHandshake();
				//if handler is rtmpe client set encryption on the protocol state
				//if (handler instanceof RTMPEClient) {
				//rtmp.setEncrypted(true);
				//set the handshake type to encrypted as well
				//outgoingHandshake.setHandshakeType(RTMPConnection.RTMP_ENCRYPTED);
				//}
				//add the handshake
				session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, outgoingHandshake);
				// set a reference to the connection on the client
				if (handler instanceof BaseRTMPClientHandler) {
					((BaseRTMPClientHandler) handler).setConnection((RTMPConnection) conn);
				}
			} else {
				//add the handshake
				session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, new InboundHandshake());
			}
		}
	}
	
}
