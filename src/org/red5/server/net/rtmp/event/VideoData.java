package org.red5.server.net.rtmp.event;

import org.apache.mina.common.ByteBuffer;
import org.red5.io.IoConstants;
import org.red5.server.stream.IStreamData;

public class VideoData extends BaseEvent implements IoConstants, IStreamData {

	public static enum FrameType {
		UNKNOWN,
		KEYFRAME,
		INTERFRAME,
		DISPOSABLE_INTERFRAME,
	}
	
	protected ByteBuffer data = null;
	private FrameType frameType = FrameType.UNKNOWN;
	
	public VideoData(ByteBuffer data){
		super(Type.STREAM_DATA);
		this.data = data;
		if (data != null && data.limit() > 0) {
			int oldPos = data.position();
			int firstByte = ((int) data.get()) & 0xff;
			data.position(oldPos);
			int frameType = (firstByte & MASK_VIDEO_FRAMETYPE) >> 4;
			if (frameType == FLAG_FRAMETYPE_KEYFRAME)
				this.frameType = FrameType.KEYFRAME;
			else if (frameType == FLAG_FRAMETYPE_INTERFRAME)
				this.frameType = FrameType.INTERFRAME;
			else if (frameType == FLAG_FRAMETYPE_DISPOSABLE)
				this.frameType = FrameType.DISPOSABLE_INTERFRAME;
			else
				this.frameType = FrameType.UNKNOWN;
		}
	}

	public byte getDataType() {
		return TYPE_VIDEO_DATA;
	}
	
	public ByteBuffer getData(){
		return data;
	}
	
	public String toString(){
		return "Audio  ts: "+getTimestamp();
	}
	
	public FrameType getFrameType() {
		return frameType;
	}
	
	public void release() {
		if (data != null) {
			data.release();
			data = null;
		}
		super.release();
	}
}