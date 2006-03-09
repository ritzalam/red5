package org.red5.server.net.servlet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;

import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmpt.RTMPTClient;
import org.red5.server.net.rtmpt.RTMPTHandler;
import org.red5.server.service.Call;

public class RTMPTServlet extends HttpServlet {

	protected static Log log =
        LogFactory.getLog(RTMPTServlet.class.getName());

	// RTMPT only supports POST with a content type of "application/x-fcs"
	private static final String REQUEST_METHOD = "POST";
	private static final String CONTENT_TYPE = "application/x-fcs";
	
	// Called to start RTMPT connection
	private static final String OPEN_REQUEST = "/open";
	
	// Called to close RTMPT connection
	private static final String CLOSE_REQUEST = "/close";
	
	// Called to send data through RTMPT connection
	private static final String SEND_REQUEST = "/send";
	
	// Called to poll RTMPT connection
	private static final String IDLE_REQUEST = "/idle";

	// Try to generate responses that contain at least 32768 bytes data.
	// Increasing this value results in better stream performance, but
	// also increases the latency.
	private static final int RESPONSE_TARGET_SIZE = 32768; 
	
	protected HashMap rtmptClients = new HashMap(); 
	
	protected void handleBadRequest(String message, HttpServletResponse resp)
		throws IOException {
		resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		resp.setContentType("text/plain");
		resp.setContentLength(message.length());
		resp.getWriter().write(message);
		resp.flushBuffer();
	}
	
	protected void returnMessage(byte message, HttpServletResponse resp)
		throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setHeader("Connection", "Keep-Alive");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType(CONTENT_TYPE);
		resp.setContentLength(1);
		resp.getWriter().write(message);
		resp.flushBuffer();
	}

	protected void returnMessage(String message, HttpServletResponse resp)
		throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setHeader("Connection", "Keep-Alive");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType(CONTENT_TYPE);
		resp.setContentLength(message.length());
		resp.getWriter().write(message);
		resp.flushBuffer();
	}
	
	protected void returnMessage(RTMPTClient client, ByteBuffer buffer, HttpServletResponse resp)
		throws IOException {
		resp.setStatus(HttpServletResponse.SC_OK);
		resp.setHeader("Connection", "Keep-Alive");
		resp.setHeader("Cache-Control", "no-cache");
		resp.setContentType(CONTENT_TYPE);
		log.debug("Sending " + buffer.limit() + " bytes.");
		resp.setContentLength(buffer.limit() + 1);
		ServletOutputStream output = resp.getOutputStream(); 
		output.write(client.getPollingDelay());
		ServletUtils.copy(buffer.asInputStream(), output);
	}

	/*
	 * Returns the client id from a url like /send/123456/12 -> 123456
	 */
	protected String getClientId(HttpServletRequest req) {
		String path = req.getPathInfo();
		if (path.equals(""))
			return "";
		
		if (path.charAt(0) == '/')
			path = path.substring(1);
		
		int endPos = path.indexOf('/');
		if (endPos != -1)
			path = path.substring(0, endPos);
		
		return path;
	}

	protected RTMPTClient getClient(HttpServletRequest req) {
		String id = getClientId(req);
		if (id == "" || !rtmptClients.containsKey(id)) {
			log.debug("Unknown client id: " + id);
			return null;
		}
		
		return (RTMPTClient) rtmptClients.get(id);
	}

	protected void skipData(HttpServletRequest req) throws IOException {
		ByteBuffer data = ByteBuffer.allocate(req.getContentLength());
		ServletUtils.copy(req.getInputStream(), data.asOutputStream());
		data.flip();
	}
	
	protected void returnPendingMessages(RTMPTClient client, HttpServletResponse resp)
		throws IOException {
		
		ByteBuffer data = client.getPendingMessages(RESPONSE_TARGET_SIZE);
		if (data == null) {
			// no more messages to send...
			returnMessage(client.getPollingDelay(), resp);
			return;
		}
		
		returnMessage(client, data, resp);
	}
	
	protected void handleOpen(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException, IOException {
		
		// Skip sent data
		skipData(req);
		
		// TODO: should we evaluate the pathinfo?
		
		RTMPTHandler handler = (RTMPTHandler) getServletContext().getAttribute(RTMPTHandler.HANDLER_ATTRIBUTE);
		RTMPTClient client = new RTMPTClient(handler);
		synchronized (rtmptClients) {
			rtmptClients.put(client.getId(), client);
		}
		
		// Return connection id to client
		returnMessage(client.getId() + "\n", resp);
	}
	
	protected void handleClose(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException, IOException {

		// Skip sent data
		skipData(req);

		RTMPTClient client = getClient(req);
		if (client == null) {
			handleBadRequest("Unknown client.", resp);
			return;
		}

		synchronized (rtmptClients) {
			rtmptClients.remove(client.getId());
		}
		
		RTMPTHandler handler = (RTMPTHandler) getServletContext().getAttribute(RTMPTHandler.HANDLER_ATTRIBUTE);
		handler.connectionClosed(client, (RTMP) client.getState());
		
		returnMessage((byte) 0, resp);
	}

	protected void handleSend(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException, IOException {
		
		RTMPTClient client = getClient(req);
		if (client == null) {
			handleBadRequest("Unknown client.", resp);
			return;
		}
		
		// Put the received data in a ByteBuffer
		int length = req.getContentLength();
		ByteBuffer data = ByteBuffer.allocate(length);
		ServletUtils.copy(req.getInputStream(), data.asOutputStream());
		data.flip();
		
		// Decode the objects in the data
		List messages = client.decode(data);
		if (messages == null || messages.isEmpty()) {
			returnMessage(client.getPollingDelay(), resp);
			return;
		}
		
		// Execute the received RTMP messages
		RTMPTHandler handler = (RTMPTHandler) getServletContext().getAttribute(RTMPTHandler.HANDLER_ATTRIBUTE);
		Iterator it = messages.iterator();
		while (it.hasNext()) {
			try {
				handler.messageReceived(client, client.getState(), it.next());
			} catch (Exception e) {
				log.error("Could not process message.", e);
			}
		}
		
		// Send results to client
		returnPendingMessages(client, resp);
	}
	
	protected void handleIdle(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException, IOException {
		
		// Skip sent data
		skipData(req);
		
		RTMPTClient client = getClient(req);
		if (client == null) {
			handleBadRequest("Unknown client.", resp);
			return;
		}
		
		returnPendingMessages(client, resp);
	}
	
	protected void service(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException, IOException {

		if (!req.getMethod().equals(REQUEST_METHOD) ||
			req.getContentLength() == 0 ||
			req.getContentType() == null ||
			!req.getContentType().equals(CONTENT_TYPE)) {
			// Bad request - return simple error page
			handleBadRequest("Bad request, only RTMPT supported.", resp);
			return;
		}

		String path = req.getServletPath();
		if (path.equals(OPEN_REQUEST)) {
			handleOpen(req, resp);
		} else if (path.equals(CLOSE_REQUEST)) {
			handleClose(req, resp);
		} else if (path.equals(SEND_REQUEST)) {
			handleSend(req, resp);
		} else if (path.equals(IDLE_REQUEST)) {
			handleIdle(req, resp);
		} else {
			handleBadRequest("RTMPT command " + path + " is not supported.", resp);
		}
	}
}
