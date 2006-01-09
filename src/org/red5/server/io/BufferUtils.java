package org.red5.server.io;

import org.apache.mina.common.ByteBuffer;

/**
 * Buffer Utility class which reads/writes intergers to the input/output buffer  
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 *
 */
public class BufferUtils {


	/**
	 * Writes a Medium Int to the output buffer
	 * @param out
	 * @param value
	 * @return void
	 */
	public static void writeMediumInt(ByteBuffer out, int value) {
		byte[] bytes = new byte[3];
		bytes[0] = (byte) ((value >>> 16) & 0x000000FF);
		bytes[1] = (byte) ((value >>> 8) & 0x000000FF);
		bytes[2] = (byte) (value & 0x00FF);
		out.put(bytes);
	}
	
	/**
	 * Reads an unsigned Medium Int from the in buffer
	 * @param in
	 * @return int
	 */
	public static int readUnsignedMediumInt(ByteBuffer in) {
		byte[] bytes = new byte[3];
		in.get(bytes);
		int val = 0;
		val += (bytes[0] & 0xFF) * 256 * 256;
		val += (bytes[1] & 0xFF) * 256;
		val += (bytes[2] & 0xFF);
		return val;
	}
	
	
	/**
	 * Reads a Medium Int to the in buffer
	 * @param in
	 * @return int
	 */
	public static int readMediumInt(ByteBuffer in) {
		byte[] bytes = new byte[3];
		in.get(bytes);
		int val = 0;
		val += bytes[0] * 256 * 256;
		val += bytes[1] * 256;
		val += bytes[2];
		if (val < 0)
			val += 256;
		return val;
	}
	
	/**
	 * Puts an in buffer stream onto an out buffer stream
	 * and returns the bytes written
	 * @param out
	 * @param in
	 * @param numBytesMax
	 * @return int
	 */
	public static int put(ByteBuffer out, ByteBuffer in, int numBytesMax){
		final int limit = in.limit();
		final int numBytesRead = (numBytesMax > in.remaining()) ? in.remaining() : numBytesMax;
		in.limit(in.position()+numBytesRead);
		out.put(in);
		in.limit(limit);
		return numBytesRead;
	}
	
}
