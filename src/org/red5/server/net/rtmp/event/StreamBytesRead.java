package org.red5.server.net.rtmp.event;

public class StreamBytesRead extends BaseEvent {
	
	private int bytesRead = 0;

	public StreamBytesRead(int bytesRead){
		super(Type.STREAM_CONTROL);
		this.bytesRead = bytesRead;
	}
	
	public byte getDataType() {
		return TYPE_STREAM_BYTES_READ;
	}
	
	public int getBytesRead(){
		return bytesRead;
	}

	public void setBytesRead(int bytesRead) {
		this.bytesRead = bytesRead;
	}

	protected void doRelease() {
		bytesRead = 0;
	}
	
	public String toString(){
		return "StreamBytesRead: "+bytesRead;
	}
	
}