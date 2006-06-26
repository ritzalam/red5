package org.red5.server.stream.message;

import org.red5.server.messaging.AbstractMessage;
import org.red5.server.net.rtmp.event.IRTMPEvent;

public class RTMPMessage extends AbstractMessage {
	private IRTMPEvent body;

	public IRTMPEvent getBody() {
		return body;
	}

	public void setBody(IRTMPEvent body) {
		this.body = body;
	}
	
	public void acquire() {
		// TODO: is this still needed?
	}
	
	public void release() {
		// TODO: is this still needed?
	}
	
}
