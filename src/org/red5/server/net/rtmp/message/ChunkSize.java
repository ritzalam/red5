package org.red5.server.net.rtmp.message;

public class ChunkSize extends Message {
	
	public static final int INITIAL_CAPACITY = 4;
	
	private int size = 0;
	
	public ChunkSize(){
		super(TYPE_CHUNK_SIZE, INITIAL_CAPACITY);
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	protected void doRelease() {
		size = 0;
	}
	
	public String toString(){
		return "ChunkSize: "+size;
	}
	
}