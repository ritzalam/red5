package org.red5.server.net.rtmp.message;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.rtmp.event.IRTMPEvent;

public class Packet {

	protected Header header;
	protected IRTMPEvent message;
	protected ByteBuffer data;

	public Packet(Header header){
		this.header = header;
		data = ByteBuffer.allocate(header.getSize());
	}
	
	public Packet(Header header, IRTMPEvent event){
		this.header = header;
		this.message = event;
	}
	
	public Header getHeader() {
		return header;
	}
	
	public void setMessage(IRTMPEvent message){
		this.message = message;
	}
	
	public IRTMPEvent getMessage() {
		return message;
	}
	
	public void setData(ByteBuffer data){
		this.data = data;
	}
	
	public ByteBuffer getData(){
		return data;
	}
	
}