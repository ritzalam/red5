package org.red5.server.net.rtmp_refactor.event;

public class ChunkSize {
		
	private int size = 0;
	
	public ChunkSize(int size){
		super();
		this.size = size;
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
	
	public boolean equals(Object obj){
		if(!(obj instanceof ChunkSize)) return false;
		final ChunkSize other = (ChunkSize) obj;
		return getSize() == other.getSize();
	}
	
}