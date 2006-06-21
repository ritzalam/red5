package org.red5.server.net.rtmp_refactor.event;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.rtmp_refactor.message.Header;
import org.red5.server.stream.IStreamData;


public class AudioData implements IHeaderAware, IStreamData {

	protected ByteBuffer data = null;
	protected int timestamp = -1;
	
	public AudioData(ByteBuffer data){
		super();
		this.data = data;
	}

	public void setHeader(Header header) {
		
	}
	
	public int getTimestamp(){
		return timestamp;
	}
	
	public ByteBuffer getData(){
		return data;
	}
	
	public String toString(){
		return "Audio  ts: "+getTimestamp();
	}
	
}