package org.red5.server.net.rtmp.message;

public class StreamBytesRead extends Message {
	
	private static final int INITIAL_CAPACITY = 4;
	
	private int bytesRead = 0;

	public StreamBytesRead(){
		super(TYPE_STREAM_BYTES_READ, INITIAL_CAPACITY);
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
