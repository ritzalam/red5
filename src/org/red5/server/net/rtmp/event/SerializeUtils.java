package org.red5.server.net.rtmp.event;

/**
 * The utility class provides conversion methods to ease the use of
 * byte arrays, mina bytebuffers, and nio bytebuffers.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class SerializeUtils {

	public static byte[] ByteBufferToByteArray(org.apache.mina.common.ByteBuffer buf) {
		byte[] byteBuf = new byte[buf.limit()];
		int pos = buf.position();
		buf.rewind();
		buf.get(byteBuf);
		buf.position(pos);
		return byteBuf;
	}
	
	public static byte[] NioByteBufferToByteArray(java.nio.ByteBuffer buf) {
		byte[] byteBuf = new byte[buf.limit()];
		int pos = buf.position();
		buf.position(0);
		buf.get(byteBuf);
		buf.position(pos);
		return byteBuf;
	}	
	
	public static void ByteArrayToByteBuffer(byte[] byteBuf, org.apache.mina.common.ByteBuffer buf) {
		buf.put(byteBuf);
		buf.flip();
	}
	
	public static void ByteArrayToNioByteBuffer(byte[] byteBuf, java.nio.ByteBuffer buf) {
		buf.put(byteBuf);
		buf.flip();
	}
	
}
