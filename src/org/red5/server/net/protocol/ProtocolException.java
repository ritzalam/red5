package org.red5.server.net.protocol;

public class ProtocolException extends RuntimeException {

	protected String message = null;
	
	public ProtocolException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
