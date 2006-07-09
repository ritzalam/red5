package org.red5.server.net.rtmp.event;

import org.red5.server.api.event.IEvent.Type;

public class ClientBW extends BaseEvent {
	
	private int bandwidth = 0;
	private byte value2 = 0;

	public ClientBW(int bandwidth, byte value2){
		super(Type.STREAM_CONTROL);
		this.bandwidth = bandwidth;
		this.value2 = value2;
	}
	
	public byte getDataType() {
		return TYPE_CLIENT_BANDWIDTH;
	}
	
	public int getBandwidth() {
		return bandwidth;
	}

	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}
	
	public byte getValue2() {
		return value2;
	}

	public void setValue2(byte value2) {
		this.value2 = value2;
	}

	public String toString(){
		return "ClientBW: "+bandwidth+" value2: "+value2;
	}
}
