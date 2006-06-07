package org.red5.server.exception;

/**
 * The client is not allowed to connect.
 *
 */
public class ClientRejectedException extends RuntimeException {

	private Object reason;
	
	public ClientRejectedException() {
		this(null);
	}
	
	public ClientRejectedException(Object reason) {
		super();
		this.reason = reason;
	}

	public Object getReason() {
		return reason;
	}

}
