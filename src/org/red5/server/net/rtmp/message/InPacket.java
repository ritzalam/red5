package org.red5.server.net.rtmp.message;

public class InPacket {

	private PacketHeader source = null;
	private Message message = null;
	
	public Message getMessage() {
		return message;
	}
	
	public void setMessage(Message message) {
		this.message = message;
	}
	
	public PacketHeader getSource() {
		return source;
	}
	
	public void setSource(PacketHeader source) {
		this.source = source;
	}
	
}
