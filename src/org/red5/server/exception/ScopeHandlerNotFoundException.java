package org.red5.server.exception;

public class ScopeHandlerNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1894151808129303439L;

	public ScopeHandlerNotFoundException(String handlerName){
		super("No scope handler found: "+handlerName);
	}
	
}
