package org.red5.server.net.rtmp_refactor.event;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.stream.IStreamData;
import org.red5.server.net.rtmp_refactor.message.Header;

public class VideoData  implements IHeaderAware, IStreamData {

	protected ByteBuffer data = null;
	protected int timestamp = -1;
	
	public VideoData(ByteBuffer data){
		super();
		this.data = data;
	}

	public void setHeader(Header header){
		
	}
	
	public void setTimestamp(int ts) {
		timestamp = ts;
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