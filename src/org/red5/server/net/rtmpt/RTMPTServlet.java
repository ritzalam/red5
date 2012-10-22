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

package org.red5.server.net.rtmpt;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.IRTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.servlet.ServletUtils;
import org.slf4j.Logger;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet that handles all RTMPT requests.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPTServlet extends HttpServlet {

	/**
	 * Serialization UID
	 */
	private static final long serialVersionUID = 5925399677454936613L;

	/**
	 * Logger
	 */
	protected static Logger log = Red5LoggerFactory.getLogger(RTMPTServlet.class);

	/**
	 * HTTP request method to use for RTMPT calls.
	 */
	private static final String REQUEST_METHOD = "POST";

	/**
	 * Content-Type to use for RTMPT requests / responses.
	 */
	private static final String CONTENT_TYPE = "application/x-fcs";

	/**
	 * Try to generate responses that contain at least 32768 bytes data.
	 * Increasing this value results in better stream performance, but also increases the latency.
	 */
	private static int targetResponseSize = 32768;

	/**
	 * Reference to RTMPT handler;
	 */
	private static RTMPTHandler handler;

	private static IRTMPConnManager rtmpConnManager;

	/**
	 * Response sent for ident2 requests. If this is null a 404 will be returned
	 */
	private static String ident2;

	// Whether or not to enforce content type checking for requests
	private boolean enforceContentTypeCheck;

	/**
	 * Thread local for request info storage
	 */
	protected ThreadLocal<RequestInfo> requestInfo = new ThreadLocal<RequestInfo>();

	/**
	 * Web app context
	 */
	protected transient WebApplicationContext appCtx;

	/** {@inheritDoc} */
	@Override
	public void init() throws ServletException {
		super.init();
		ServletContext ctx = getServletContext();
		appCtx = WebApplicationContextUtils.getWebApplicationContext(ctx);
		if (appCtx == null) {
			appCtx = (WebApplicationContext) ctx.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
	}

	/**
	 * Return an error message to the client.
	 * 
	 * @param message Message
	 * @param resp Servlet response
	 * @throws IOException I/O exception
	 */
	protected void handleBadRequest(String message, HttpServletResponse resp) throws IOException {
		log.debug("handleBadRequest {}", message);
		resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		resp.setHeader("Connection", "Keep-Alive");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType("text/plain");
		resp.setContentLength(message.length());
		resp.getWriter().write(message);
		resp.flushBuffer();
	}

	/**
	 * Return a single byte to the client.
	 * 
	 * @param message Message
	 * @param resp Servlet response
	 * @throws IOException I/O exception
	 */
	protected void returnMessage(byte message, HttpServletResponse resp) throws IOException {
		log.debug("returnMessage {}", message);
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setHeader("Connection", "Keep-Alive");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType(CONTENT_TYPE);
		resp.setContentLength(1);
		resp.getWriter().write(message);
		resp.flushBuffer();
	}

	/**
	 * Return a message to the client.
	 * 
	 * @param message Message
	 * @param resp Servlet response
	 * @throws IOException I/O exception
	 */
	protected void returnMessage(String message, HttpServletResponse resp) throws IOException {
		log.debug("returnMessage {}", message);
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setHeader("Connection", "Keep-Alive");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType(CONTENT_TYPE);
		resp.setContentLength(message.length());
		resp.getWriter().write(message);
		resp.flushBuffer();
	}

	/**
	 * Return raw data to the client.
	 * 
	 * @param client RTMP connection
	 * @param buffer Raw data as byte buffer
	 * @param resp Servlet response
	 * @throws IOException I/O exception
	 */
	protected void returnMessage(RTMPTConnection client, IoBuffer buffer, HttpServletResponse resp) throws IOException {
		log.trace("returnMessage {}", buffer);
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setHeader("Connection", "Keep-Alive");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType(CONTENT_TYPE);
		resp.setContentLength(buffer.limit() + 1);
		byte pollingDelay = client.getPollingDelay();
		log.debug("Sending {} bytes; polling delay: {}", buffer.limit(), pollingDelay);
		ServletOutputStream output = resp.getOutputStream();
		output.write(pollingDelay);
		ServletUtils.copy(buffer.asInputStream(), output);
		buffer.free();
		buffer = null;
	}

	/**
	 * Sets the request info for the current request. Request info contains the session id and request number gathered from
	 * the incoming request. The URI is in this form /[method]/[session id]/[request number] ie. /send/CAFEBEEF01/7
	 *
	 * @param req Servlet request
	 */
	protected void setRequestInfo(HttpServletRequest req) {
		String[] arr = req.getRequestURI().trim().split("/");
		log.trace("Request parts: {}", Arrays.toString(arr));
		RequestInfo info = new RequestInfo(arr[2], Integer.valueOf(arr[3]));
		requestInfo.set(info);
	}

	/**
	 * Skip data sent by the client.
	 * 
	 * @param req
	 *            Servlet request
	 * @throws IOException
	 *             I/O exception
	 */
	protected void skipData(HttpServletRequest req) throws IOException {
		log.trace("skipData {}", req);
		IoBuffer data = IoBuffer.allocate(req.getContentLength());
		ServletUtils.copy(req.getInputStream(), data.asOutputStream());
		data.flip();
		data.free();
		data = null;
	}

	/**
	 * Send pending messages to client.
	 * 
	 * @param client RTMP connection
	 * @param resp Servlet response
	 */
	protected void returnPendingMessages(RTMPTConnection client, HttpServletResponse resp) {
		log.debug("returnPendingMessages {}", client);
		// grab any pending outgoing data
		IoBuffer data = client.getPendingMessages(targetResponseSize);
		if (data != null) {
			try {
				returnMessage(client, data, resp);
			} catch (Exception ex) {
				// using "Exception" is meant to catch any exception that would occur when doing a write
				// this can be an IOException or a container specific one like ClientAbortException from catalina
				log.warn("Exception returning outgoing data", ex);
				client.realClose();
			}
		} else {
			log.debug("No messages to send");
			if (client.isClosing()) {
				log.debug("Client is closing, send close notification");
				try {
					// tell client to close connection
					returnMessage((byte) 0, resp);
				} catch (IOException ex) {
					log.warn("Exception returning outgoing data - close notification", ex);
				}
			} else {
				try {
					returnMessage(client.getPollingDelay(), resp);
				} catch (IOException ex) {
					log.warn("Exception returning outgoing data - polling delay", ex);
				}
			}
		}
	}

	/**
	 * Start a new RTMPT session.
	 * 
	 * @param req Servlet request
	 * @param resp Servlet response
	 * @throws IOException I/O exception
	 */
	protected void handleOpen(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.debug("handleOpen");
		// skip sent data
		skipData(req);
		// TODO: should we evaluate the pathinfo?
		RTMPTConnection connection = createConnection();
		if (connection != null) {
			connection.setServlet(this);
			connection.setServletRequest(req);
			if (connection.getId() != 0) {
				// return session id to client
				returnMessage(connection.getSessionId() + "\n", resp);
			} else {
				// no more clients are available for serving
				returnMessage((byte) 0, resp);
			}
		} else {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.setHeader("Connection", "Keep-Alive");
			resp.setHeader("Cache-Control", "no-cache");
			resp.flushBuffer();
		}
	}

	/**
	 * Close a RTMPT session.
	 * 
	 * @param req Servlet request
	 * @param resp Servlet response
	 * @throws IOException I/O exception
	 */
	protected void handleClose(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.debug("handleClose");
		// skip sent data
		skipData(req);
		// get the associated connection
		RTMPTConnection connection = getConnection();
		if (connection != null) {
			log.debug("Pending messges on close: {}", connection.getPendingMessages());
			handler.connectionClosed(connection, connection.getState());
			returnMessage((byte) 0, resp);
			connection.close();
			connection.realClose();
		} else {
			handleBadRequest(String.format("Close: unknown client session: %d", requestInfo.get().getSessionId()), resp);
		}
	}

	/**
	 * Add data for an established session.
	 * 
	 * @param req Servlet request
	 * @param resp Servlet response
	 * @throws IOException I/O exception
	 */
	protected void handleSend(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.debug("handleSend");
		final RTMPTConnection connection = getConnection();
		if (connection != null) {
			// put the received data in a ByteBuffer
			int length = req.getContentLength();
			log.trace("Request content length: {}", length);
			final IoBuffer data = IoBuffer.allocate(length);
			ServletUtils.copy(req.getInputStream(), data.asOutputStream());
			data.flip();
			//			Thread reader = new Thread(new Runnable() {
			//				public void run() {
			// decode the objects in the data
			final List<?> messages = connection.decode(data);
			// clear the buffer
			data.free();
			// messages are either of IoBuffer or Packet type
			// handshaking uses IoBuffer and everything else should be Packet
			connection.read(messages);
			//				}
			//			}, "Reader#" + sessionId + "@" + System.nanoTime());
			//			reader.start();
			// return pending
			returnPendingMessages(connection, resp);
			//
			//			try {
			//				reader.join();
			//			} catch (InterruptedException e) {
			//				e.printStackTrace();
			//			}
		} else {
			handleBadRequest(String.format("Send: unknown client session: %s", requestInfo.get().getSessionId()), resp);
		}
	}

	/**
	 * Poll RTMPT session for updates.
	 * 
	 * @param req Servlet request
	 * @param resp Servlet response
	 * @throws IOException I/O exception
	 */
	protected void handleIdle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.debug("handleIdle");
		// skip sent data
		skipData(req);
		// get associated connection
		RTMPTConnection connection = getConnection();
		if (connection != null) {
			// return pending
			returnPendingMessages(connection, resp);
		} else {
			handleBadRequest(String.format("Idle: unknown client session: %s", requestInfo.get().getSessionId()), resp);
		}
	}

	/**
	 * Main entry point for the servlet.
	 * 
	 * @param req Request object
	 * @param resp Response object
	 * @throws IOException I/O exception
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		log.debug("Request - method: {} content type: {} path: {}", new Object[] { req.getMethod(), req.getContentType(), req.getServletPath() });
		// allow only POST requests with valid content length
		if (!REQUEST_METHOD.equals(req.getMethod()) || req.getContentLength() == 0) {
			// Bad request - return simple error page
			handleBadRequest("Bad request, only RTMPT supported.", resp);
			return;
		}
		// decide whether or not to enforce request content checks
		if (enforceContentTypeCheck && !CONTENT_TYPE.equals(req.getContentType())) {
			handleBadRequest(String.format("Bad request, unsupported content type: %s.", req.getContentType()), resp);
			return;
		}
		// get the uri
		String uri = req.getRequestURI().trim();
		log.debug("URI: {}", uri);
		// get the path
		String path = req.getServletPath();
		// since the only current difference in the type of request that we are interested in is the 'second' character, we can double
		// the speed of this entry point by using a switch on the second character.
		char p = path.charAt(1);
		switch (p) {
			case 'o': // OPEN_REQUEST
				handleOpen(req, resp);
				break;
			case 'c': // CLOSE_REQUEST
				setRequestInfo(req);
				handleClose(req, resp);
				requestInfo.remove();
				break;
			case 's': // SEND_REQUEST
				setRequestInfo(req);
				handleSend(req, resp);
				requestInfo.remove();
				break;
			case 'i': // IDLE_REQUEST
				setRequestInfo(req);
				handleIdle(req, resp);
				requestInfo.remove();
				break;
			case 'f': // HTTPIdent request (ident and ident2)
				//if HTTPIdent is requested send back some Red5 info
				//http://livedocs.adobe.com/flashmediaserver/3.0/docs/help.html?content=08_xmlref_011.html			
				String ident = "<fcs><Company>Red5</Company><Team>Red5 Server</Team></fcs>";
				// handle ident2 slightly different to appease osx clients
				if (uri.charAt(uri.length() - 1) == '2') {
					// check for pre-configured ident2 value
					if (ident2 != null) {
						ident = ident2;
					} else {
						// just send 404 back if no ident2 value is set
						resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
						resp.setHeader("Connection", "Keep-Alive");
						resp.setHeader("Cache-Control", "no-cache");
						resp.flushBuffer();
						break;
					}
				}
				resp.setStatus(HttpServletResponse.SC_OK);
				resp.setHeader("Connection", "Keep-Alive");
				resp.setHeader("Cache-Control", "no-cache");
				resp.setContentType(CONTENT_TYPE);
				resp.setContentLength(ident.length());
				resp.getWriter().write(ident);
				resp.flushBuffer();
				break;
			default:
				handleBadRequest(String.format("RTMPT command %s is not supported.", path), resp);
		}
		// clear thread local reference
		Red5.setConnectionLocal(null);
	}

	/** {@inheritDoc} */
	@Override
	public void destroy() {
		if (rtmpConnManager != null) {
			// Cleanup connections
			Collection<RTMPConnection> conns = rtmpConnManager.removeConnections();
			for (RTMPConnection conn : conns) {
				if (conn instanceof RTMPTConnection) {
					log.debug("Connection scope on destroy: {}", conn.getScope());
					if (conn.isConnected() && !((RTMPTConnection) conn).isClosing()) {
						conn.close();
					}
				}
			}
		}
		super.destroy();
	}

	/**
	 * A connection has been closed that was created by this servlet.
	 * 
	 * @param conn
	 */
	protected void notifyClosed(RTMPTConnection conn) {
		rtmpConnManager.removeConnection(conn.getId());
	}

	/**
	 * Returns a connection based on the current client session id.
	 * 
	 * @return RTMPTConnection
	 */
	protected RTMPTConnection getConnection() {
		String sessionId = requestInfo.get().getSessionId();
		RTMPTConnection conn = (RTMPTConnection) rtmpConnManager.getConnectionBySessionId(sessionId);
		if (conn != null) {
			// clear thread local reference
			Red5.setConnectionLocal(conn);
		} else {
			log.warn("Null connection for session id: {}", sessionId);
		}
		return conn;
	}

	/**
	 * Creates an RTMPT connection.
	 * 
	 * @return RTMPT connection
	 */
	protected RTMPTConnection createConnection() {
		RTMPTConnection conn = (RTMPTConnection) rtmpConnManager.createConnection(RTMPTConnection.class);
		if (conn != null) {
			conn.setHandler(handler);
			conn.setDecoder(handler.getCodecFactory().getRTMPDecoder());
			conn.setEncoder(handler.getCodecFactory().getRTMPEncoder());
			handler.connectionOpened(conn, conn.getState());
			// set thread local reference
			Red5.setConnectionLocal(conn);
		}
		return conn;
	}

	protected void removeConnection(String sessionId) {
		RTMPTConnection conn = (RTMPTConnection) rtmpConnManager.getConnectionBySessionId(sessionId);
		if (conn != null) {
			rtmpConnManager.removeConnection(conn.getId());
		} else {
			log.warn("Remove failed, null connection for session id: {}", sessionId);
		}
	}

	public void setRtmpConnManager(IRTMPConnManager rtmpConnManager) {
		RTMPTServlet.rtmpConnManager = rtmpConnManager;
	}

	/**
	 * Set the RTMPTHandler to use in this servlet.
	 * 
	 * @param handler handler
	 */
	public void setHandler(RTMPTHandler handler) {
		RTMPTServlet.handler = handler;
	}

	/** 
	 * Set the fcs/ident2 string
	 * 
	 * @param ident2
	 */
	public void setIdent2(String ident2) {
		RTMPTServlet.ident2 = ident2;
	}

	/**
	 * Sets the target size for responses
	 * 
	 * @param targetResponseSize the targetResponseSize to set
	 */
	public void setTargetResponseSize(int targetResponseSize) {
		RTMPTServlet.targetResponseSize = targetResponseSize;
	}

	/**
	 * @return the enforceContentTypeCheck
	 */
	public boolean isEnforceContentTypeCheck() {
		return enforceContentTypeCheck;
	}

	/**
	 * @param enforceContentTypeCheck the enforceContentTypeCheck to set
	 */
	public void setEnforceContentTypeCheck(boolean enforceContentTypeCheck) {
		this.enforceContentTypeCheck = enforceContentTypeCheck;
	}

	/**
	 * Used to store request information per thread.
	 */
	protected final class RequestInfo {

		private String sessionId;

		private Integer requestNumber;

		RequestInfo(String sessionId, Integer requestNumber) {
			this.sessionId = sessionId;
			this.requestNumber = requestNumber;
		}

		/**
		 * @return the sessionId
		 */
		public String getSessionId() {
			return sessionId;
		}

		/**
		 * @return the requestNumber
		 */
		public Integer getRequestNumber() {
			return requestNumber;
		}

	}

}
