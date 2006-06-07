package org.red5.server.exception;

public class ClientNotFoundException extends RuntimeException {

	public ClientNotFoundException(String id){
		super("Client \""+id+"\" not found.");
	}

}
