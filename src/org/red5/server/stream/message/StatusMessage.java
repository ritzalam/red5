package org.red5.server.stream.message;

import org.red5.server.messaging.AbstractMessage;
import org.red5.server.net.rtmp.status.Status;

public class StatusMessage extends AbstractMessage {
	private Status body;

	public Status getBody() {
		return body;
	}

	public void setBody(Status body) {
		this.body = body;
	}
	
}
