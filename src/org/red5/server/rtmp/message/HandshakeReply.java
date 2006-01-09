package org.red5.server.rtmp.message;

public class HandshakeReply extends Message {

	public HandshakeReply(){
		super(TYPE_HANDSHAKE_REPLY, HANDSHAKE_SIZE * 2 + 1);
		getData().put((byte)0x03).fill((byte) 0x00, HANDSHAKE_SIZE);
	}
	
}
