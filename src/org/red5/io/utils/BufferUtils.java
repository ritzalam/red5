/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.io.utils;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Buffer Utility class which reads/writes intergers to the input/output buffer  
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Andy Shaules (bowljoman@hotmail.com)
 */
public class BufferUtils {

	//private static Logger log = LoggerFactory.getLogger(BufferUtils.class);
	
	/**
	 * Writes a Medium Int to the output buffer
	 * 
	 * @param out          Container to write to
	 * @param value        Integer to write
	 */
	public static void writeMediumInt(IoBuffer out, int value) {
		byte[] bytes = new byte[3];
		bytes[0] = (byte) ((value >>> 16) & 0x000000FF);
		bytes[1] = (byte) ((value >>> 8) & 0x000000FF);
		bytes[2] = (byte) (value & 0x00FF);
		out.put(bytes);
	}

	/**
	 * Reads an unsigned Medium Int from the in buffer
	 * 
	 * @param in           Source
	 * @return int         Integer value
	 */
	public static int readUnsignedMediumInt(IoBuffer in) {
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
	 * 
	 * @param in           Source
	 * @return int         Medium int
	 */
	public static int readMediumInt(IoBuffer in) {
		byte[] bytes = new byte[3];
		in.get(bytes);
		int val = 0;
		val += bytes[0] * 256 * 256;
		val += bytes[1] * 256;
		val += bytes[2];
		if (val < 0) {
			val += 256;
		}
		return val;
	}

	/**
	 * Puts input buffer stream to output buffer
	 * and returns number of bytes written
	 * @param out                Output buffer
	 * @param in                 Input buffer
	 * @param numBytesMax        Number of bytes max
	 * @return int               Number of bytes written
	 */
	@SuppressWarnings("unused")
	public final static int put(IoBuffer out, IoBuffer in, int numBytesMax) {
		//log.trace("Put - out buffer: {} in buffer: {} max bytes: {}", new Object[]{out, in, numBytesMax});
		int limit = in.limit(); 
		int capacity = in.capacity(); 
		int numBytesRead = (numBytesMax > in.remaining()) ? in.remaining() : numBytesMax;
		//log.trace("limit: {} capacity: {} bytes read: {}", new Object[]{limit, capacity, numBytesRead});
		// buffer.limit 
		// The new limit value, must be non-negative and no larger than this buffer's capacity 
		// http://java.sun.com/j2se/1.4.2/docs/api/java/nio/Buffer.html#limit(int); 
		// This is causing decoding error by raising RuntimeException IllegalArgumentError in 
		// RTMPProtocolDecoder.decode to ProtocolException. 
		int thisLimit = (in.position() + numBytesRead <= in.capacity()) ? in.position() + numBytesRead : capacity;
		//somehow the "in" buffer becomes null here occasionally
		if (in != null) {
    		in.limit(thisLimit);
    		// any implication to giving output buffer in with limit set to capacity? 
    		// Reduces numBytesRead, triggers continueDecode?
    		out.put(in);
		} else {
			numBytesRead = 0;
		}
		in.limit(limit);
		return numBytesRead;
	}

}
