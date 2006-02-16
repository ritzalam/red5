package org.red5.server.net.rtmp.message;

public class OutPacket {

	private PacketHeader destination = null;
	private Message message = null;
	
	public Message getMessage() {
		return message;
	}
	
	public void setMessage(Message message) {
		this.message = message;
	}
	
	public PacketHeader getDestination() {
		return destination;
	}
	
	public void setDestination(PacketHeader destination) {
		this.destination = destination;
	}
	
}
