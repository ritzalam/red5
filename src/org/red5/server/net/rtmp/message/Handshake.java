package org.red5.server.net.rtmp.message;

public class Handshake extends Message {

	public Handshake(){
		super(TYPE_HANDSHAKE, HANDSHAKE_SIZE);		
	}
	
}
