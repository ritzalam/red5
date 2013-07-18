package org.red5.server.net.rtmp;

import org.red5.server.api.Red5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReceivedMessageTask implements Runnable {

	private final static Logger log = LoggerFactory.getLogger(ReceivedMessageTask.class);
	
	private RTMPConnection conn;
	
	private final IRTMPHandler handler;

	private final String sessionId;
	
	private Object message;

	public ReceivedMessageTask(String sessionId, Object message, IRTMPHandler handler) {
		this(sessionId, message, handler, (RTMPConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId));
	}
	
	public ReceivedMessageTask(String sessionId, Object message, IRTMPHandler handler, RTMPConnection conn) {
		this.sessionId = sessionId;
		this.message = message;
		this.handler = handler;
		this.conn = conn;
	}	

	public void run() {
		// set connection to thread local
		Red5.setConnectionLocal(conn);
		try {
			// pass message to the handler
			handler.messageReceived(message);
		} catch (Exception e) {
			log.error("Error processing received message {}", sessionId, e);
		} finally {
			// clear thread local
			Red5.setConnectionLocal(null);
		}
	}
	
}