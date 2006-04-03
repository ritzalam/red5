package org.red5.server.stream.codec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.stream.IVideoStreamCodec;

/*
 * Red5 video codec for the sorenson video format.
 * 
 * VERY simple implementation, just stores last keyframe.
 * 
 * @author Joachim Bauch (jojo@struktur.de)
 */

public class SorensonVideo implements IVideoStreamCodec {

	private Log log = LogFactory.getLog(SorensonVideo.class.getName());
	
	static final byte FLV_FRAME_KEY = 0x10;
	static final byte FLV_CODEC_SORENSON = 0x02;

	private byte[] blockData;
	private int dataCount;
	private int blockSize;
	
	public SorensonVideo() {
		this.reset();
	}
	
	public void reset() {
		this.blockData = null;
		this.blockSize = 0;
		this.dataCount = 0;
	}

	public boolean canHandleData(ByteBuffer data) {
		byte first = data.get();
		boolean result = ((first & 0x0f) == FLV_CODEC_SORENSON);
		data.rewind();
		return result;
	}

	public boolean addData(ByteBuffer data) {
		if (!this.canHandleData(data))
			return false;
		
		byte first = data.get();
		data.rewind();
		if ((first & 0xf0) != FLV_FRAME_KEY) {
			// Not a keyframe
			return true;
		}
		
		// Store last keyframe
		this.dataCount = data.limit();
		if (this.blockSize < this.dataCount) {
			this.blockSize = this.dataCount;
			this.blockData = new byte[this.blockSize];
		}
		
		data.get(this.blockData, 0, this.dataCount);
		data.rewind();
		return true;
	}

	public ByteBuffer getKeyframe() {
		if (this.dataCount == 0)
			return null;
		
		ByteBuffer result = ByteBuffer.allocate(this.dataCount);
		result.put(this.blockData, 0, this.dataCount);
		result.rewind();
		return result;
	}
}
