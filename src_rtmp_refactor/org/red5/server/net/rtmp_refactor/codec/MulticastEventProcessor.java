package org.red5.server.net.rtmp_refactor.codec;

import org.apache.mina.common.ByteBuffer;

public class MulticastEventProcessor {

	public byte getCacheId() {
		return 0;
	}

	public void disposeCached(Object obj) {
		if(obj == null) return;
		final ByteBuffer[] chunks = (ByteBuffer[]) obj;
		for(ByteBuffer buf : chunks){
			buf.release();
		}
	}

	public static ByteBuffer[] chunkBuffer(ByteBuffer buf, int size){
		final int num = (int) Math.ceil(buf.limit() / (float) size);
		final ByteBuffer[] chunks = new ByteBuffer[num];
		for(int i=0; i<num; i++){
			chunks[i] = buf.asReadOnlyBuffer(); 
			final ByteBuffer chunk = chunks[i];
			int position = size*num;
			chunk.position(position);
			if(position + size < chunk.limit()) 
				chunk.limit(position + size);
		}
		return chunks;
	}

}
