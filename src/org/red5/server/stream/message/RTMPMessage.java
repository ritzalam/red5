package org.red5.server.stream.message;

import org.red5.server.messaging.AbstractMessage;
import org.red5.server.net.rtmp.message.Message;

public class RTMPMessage extends AbstractMessage {
	private Message body;

	public Message getBody() {
		return body;
	}

	public void setBody(Message body) {
		this.body = body;
	}
	
	public void acquire() {
		if (body != null) body.acquire();
	}
	
	public void release() {
		if (body != null) body.release();
	}
	
}
