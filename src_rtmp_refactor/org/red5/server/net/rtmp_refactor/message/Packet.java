package org.red5.server.net.rtmp_refactor.message;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.event.IEvent;

public class Packet {

	protected Header header;
	protected Object message;
	protected ByteBuffer data;

	public Packet(Header header){
		this.header = header;
		data = ByteBuffer.allocate(header.getSize());
	}
	
	public Packet(Header header, Object event){
		this.header = header;
		this.message = event;
	}
	
	public Header getHeader() {
		return header;
	}
	
	public void setMessage(Object message){
		this.message = message;
	}
	
	public Object getMessage() {
		return message;
	}
	
	public void setData(ByteBuffer data){
		this.data = data;
	}
	
	public ByteBuffer getData(){
		return data;
	}
	
}