package org.red5.server.net.rtmp.event;

import org.apache.mina.common.ByteBuffer;

public class SerializeUtils {
	public static byte[] ByteBufferToByteArray(ByteBuffer buf) {
		byte[] byteBuf = new byte[buf.limit()];
		int pos = buf.position();
		buf.rewind();
		buf.get(byteBuf);
		buf.position(pos);
		return byteBuf;
	}
	
	public static void ByteArrayToByteBuffer(byte[] byteBuf, ByteBuffer buf) {
		buf.put(byteBuf);
		buf.flip();
	}
}
