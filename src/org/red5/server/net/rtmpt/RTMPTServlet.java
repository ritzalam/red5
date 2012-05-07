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
import java.net.URL;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.IRTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
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
	 * Web app context
	 */
	protected transient WebApplicationContext appCtx;

	/**
	 * Reference to RTMPT handler;
	 */
	private static RTMPTHandler handler;

	private static IRTMPConnManager rtmpConnManager;

	// Response sent for ident2 requests. If this is null a 404 will be returned
	private static String ident2;
	
	// Whether or not to enforce content type checking for requests
	private boolean enforceContentTypeCheck;

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
	 * Return an error message to the client.
	 * 
	 * @param message
	 *            Message
	 * @param resp
	 *            Servlet response
	 * @throws IOException
	 *             I/O exception
	 */
	protected void handleBadRequest(String message, HttpServletResponse resp) throws IOException {
		log.debug("handleBadRequest {}", message);
		resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		resp.setContentType("text/plain");
		resp.setContentLength(message.length());
		resp.getWriter().write(message);
		resp.flushBuffer();
	}

	/**
	 * Return a single byte to the client.
	 * 
	 * @param message
	 *            Message
	 * @param resp
	 *            Servlet response
	 * @throws IOException
	 *             I/O exception
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
	 * @param message
	 *            Message
	 * @param resp
	 *            Servlet response
	 * @throws IOException
	 *             I/O exception
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
	 * @param client
	 *            RTMP connection
	 * @param buffer
	 *            Raw data as byte buffer
	 * @param resp
	 *            Servlet response
	 * @throws IOException
	 *             I/O exception
	 */
	protected void returnMessage(RTMPTConnection client, IoBuffer buffer, HttpServletResponse resp) throws IOException {
		log.debug("returnMessage {}", buffer);
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setHeader("Connection", "Keep-Alive");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType(CONTENT_TYPE);
		log.debug("Sending {} bytes", buffer.limit());
		resp.setContentLength(buffer.limit() + 1);
		ServletOutputStream output = resp.getOutputStream();
		output.write(client.getPollingDelay());
		ServletUtils.copy(buffer.asInputStream(), output);
		buffer.free();
		buffer = null;
	}

	/**
	 * Return the client id from a url like /send/123456/12 -> 123456
	 *
	 * @param req Servlet request
	 * @return Client id
	 */
	protected Integer getClientId(HttpServletRequest req) {
		String uri = req.getRequestURL().toString();
		URL url = null;
		try {
			url = new URL(uri);
		} catch (Exception e) {
			log.warn("getclientId: error parsing url: {}", uri);
			return null;
		}
		// get path
		String path = url.getPath();
		if (path.equals("")) {
			log.error("getClientId: path is empty");
			return null;
		}
		// trim off end
		int pos = path.lastIndexOf('/');
		path = path.substring(0, pos);
		// trim off beginning
		pos = path.lastIndexOf('/');
		if (pos != -1) {
			path = path.substring(pos + 1);
		}
		try {
			return Integer.valueOf(path);
		} catch (Exception e) {
			log.error("getClientId: parse error", e);
			return null;
		}
	}

	/**
	 * Get the RTMPT client for a session.
	 * 
	 * @param req
	 *            Servlet request
	 * @return RTMP client connection
	 */
	protected RTMPTConnection getClientConnection(HttpServletRequest req) {
		final Integer id = getClientId(req);
		return getConnection(id);
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
		log.debug("skipData {}", req);
		IoBuffer data = IoBuffer.allocate(req.getContentLength());
		ServletUtils.copy(req.getInputStream(), data.asOutputStream());
		data.flip();
		data.free();
		data = null;
	}

	/**
	 * Send pending messages to client.
	 * 
	 * @param client
	 *            RTMP connection
	 * @param resp
	 *            Servlet response
	 * @throws IOException
	 *             I/O exception
	 */
	protected void returnPendingMessages(RTMPTConnection client, HttpServletResponse resp) throws IOException {
		log.debug("returnPendingMessages {}", client);
		IoBuffer data = client.getPendingMessages(targetResponseSize);
		if (data != null) {
			returnMessage(client, data, resp);
		} else {
			// no more messages to send...
			if (client.isClosing()) {
				// Tell client to close connection
				returnMessage((byte) 0, resp);
			} else {
				returnMessage(client.getPollingDelay(), resp);
			}
		}
	}

	/**
	 * Start a new RTMPT session.
	 * 
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @throws ServletException
	 *             Servlet exception
	 * @throws IOException
	 *             I/O exception
	 */
	protected void handleOpen(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("handleOpen");
		// Skip sent data
		skipData(req);
		// TODO: should we evaluate the pathinfo?
		RTMPTConnection connection = createConnection();
		connection.setServlet(this);
		connection.setServletRequest(req);
		if (connection.getId() != 0) {
			// Return connection id to client
			returnMessage(connection.getId() + "\n", resp);
		} else {
			// no more clients are available for serving
			returnMessage((byte) 0, resp);
		}
	}

	/**
	 * Close a RTMPT session.
	 * 
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @throws ServletException
	 *             Servlet exception
	 * @throws IOException
	 *             I/O exception
	 */
	protected void handleClose(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("handleClose");
		// skip sent data
		skipData(req);
		// get the associated connection
		RTMPTConnection connection = getClientConnection(req);
		if (connection == null) {
			handleBadRequest(String.format("Close: unknown client with id: %s", getClientId(req)), resp);
			return;
		}		
		removeConnection(connection.getId());
		handler.connectionClosed(connection, connection.getState());
		returnMessage((byte) 0, resp);
		connection.realClose();
	}

	/**
	 * Add data for an established session.
	 * 
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @throws ServletException
	 *             Servlet exception
	 * @throws IOException
	 *             I/O exception
	 */
	protected void handleSend(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("handleSend");
		RTMPTConnection connection = getClientConnection(req);
		if (connection == null) {
			handleBadRequest(String.format("Send: unknown client with id: %s", getClientId(req)), resp);
			return;
		} else if (connection.getStateCode() == RTMP.STATE_DISCONNECTED) {
			removeConnection(connection.getId());
			handleBadRequest("Connection already closed", resp);
			return;
		}
		// put the received data in a ByteBuffer
		int length = req.getContentLength();
		IoBuffer data = IoBuffer.allocate(length);
		ServletUtils.copy(req.getInputStream(), data.asOutputStream());
		data.flip();
		// decode the objects in the data
		List<?> messages = connection.decode(data);
		data.free();
		data = null;
		if (messages == null || messages.isEmpty()) {
			returnMessage(connection.getPollingDelay(), resp);
			return;
		}
		// execute the received RTMP messages
		IoSession session = new DummySession();
		session.setAttribute(RTMPConnection.RTMP_CONNECTION_KEY, connection);
		session.setAttribute(ProtocolState.SESSION_KEY, connection.getState());
		for (Object message : messages) {
			try {
				handler.messageReceived(message, session);
			} catch (Exception e) {
				log.error("Could not process message", e);
			}
		}
		// send results to client
		returnPendingMessages(connection, resp);
	}

	/**
	 * Poll RTMPT session for updates.
	 * 
	 * @param req
	 *            Servlet request
	 * @param resp
	 *            Servlet response
	 * @throws ServletException
	 *             Servlet exception
	 * @throws IOException
	 *             I/O exception
	 */
	protected void handleIdle(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("handleIdle");
		// skip sent data
		skipData(req);
		// get associated connection
		RTMPTConnection connection = getClientConnection(req);
		if (connection == null) {
			handleBadRequest("Idle: unknown client with id: " + getClientId(req), resp);
			return;
		} else if (connection.isClosing()) {
			// tell client to close the connection
			returnMessage((byte) 0, resp);
			connection.realClose();
			return;
		} else if (connection.getStateCode() == RTMP.STATE_DISCONNECTED) {
			removeConnection(connection.getId());
			handleBadRequest("Connection already closed", resp);
			return;
		}
		returnPendingMessages(connection, resp);
	}

	/**
	 * Main entry point for the servlet.
	 * 
	 * @param req
	 *            Request object
	 * @param resp
	 *            Response object
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
		//get the path
		String path = req.getServletPath();
		// since the only current difference in the type of request that we are interested in is the 'second' character, we can double
		// the speed of this entry point by using a switch on the second character.
		char p = path.charAt(1);
		switch (p) {
			case 'o': // OPEN_REQUEST
				handleOpen(req, resp);
				break;
			case 'c': // CLOSE_REQUEST
				handleClose(req, resp);
				break;
			case 's': // SEND_REQUEST
				handleSend(req, resp);
				break;
			case 'i': // IDLE_REQUEST
				handleIdle(req, resp);
				break;
			case 'f': // HTTPIdent request (ident and ident2)
				//if HTTPIdent is requested send back some Red5 info
				//http://livedocs.adobe.com/flashmediaserver/3.0/docs/help.html?content=08_xmlref_011.html			
				String ident = "<fcs><Company>Red5</Company><Team>Red5 Server</Team></fcs>";
				// handle ident2 slightly different to appease osx clients
				String uri = req.getRequestURI().trim();
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
	public void init() throws ServletException {
		super.init();
		ServletContext ctx = getServletContext();
		appCtx = WebApplicationContextUtils.getWebApplicationContext(ctx);
		if (appCtx == null) {
			appCtx = (WebApplicationContext) ctx.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void destroy() {
		if (rtmpConnManager != null) {
			// Cleanup connections
			Collection<RTMPConnection> conns = rtmpConnManager.removeConnections();
			for (RTMPConnection conn : conns) {
				conn.close();
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

	protected RTMPTConnection getConnection(int clientId) {
		RTMPTConnection conn = (RTMPTConnection) rtmpConnManager.getConnection(clientId);
		if (conn != null) {
			// clear thread local reference
			Red5.setConnectionLocal(conn);
		} else {
			log.warn("Null connection for clientId: {}", clientId);
		}
		return conn;
	}

	protected RTMPTConnection createConnection() {
		RTMPTConnection conn = (RTMPTConnection) rtmpConnManager.createConnection(RTMPTConnection.class);
		conn.setHandler(handler);
		conn.setDecoder(handler.getCodecFactory().getRTMPDecoder());
		conn.setEncoder(handler.getCodecFactory().getRTMPEncoder());
		handler.connectionOpened(conn, conn.getState());
		// set thread local reference
		Red5.setConnectionLocal(conn);
		return conn;
	}

	protected void removeConnection(int clientId) {
		rtmpConnManager.removeConnection(clientId);
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
}
