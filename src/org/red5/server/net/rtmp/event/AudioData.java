package org.red5.server.net.rtmp.event;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.stream.IStreamData;


public class AudioData extends BaseEvent implements IStreamData {

	protected ByteBuffer data = null;
	
	public AudioData(){
		this(ByteBuffer.allocate(0).flip());
	}
	
	public AudioData(ByteBuffer data){
		super(Type.STREAM_DATA);
		this.data = data;
	}

	public byte getDataType() {
		return TYPE_AUDIO_DATA;
	}
	
	public ByteBuffer getData(){
		return data;
	}
	
	public String toString(){
		return "Audio  ts: "+getTimestamp();
	}
	
	public void release() {
		if (data != null)
			data.release();
		super.release();
	}
}