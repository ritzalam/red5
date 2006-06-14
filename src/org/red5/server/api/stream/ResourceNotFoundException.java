package org.red5.server.api.stream;

public class ResourceNotFoundException extends Exception {
	private static final long serialVersionUID = -1963629259187714996L;

	public ResourceNotFoundException() {
		super();
	}

	public ResourceNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResourceNotFoundException(String message) {
		super(message);
	}

	public ResourceNotFoundException(Throwable cause) {
		super(cause);
	}

}
