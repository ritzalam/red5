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

package org.red5.io.mp4;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Wrapper class for input streams containing MPEG4 data.
 * 
 * Original idea based on code from MediaFrame (http://www.mediaframe.org)
 * 
 * 11/2011 	- Converted to use NIO
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class MP4DataStream {

	/** The input stream. */
	private FileChannel channel;

	/** The current offset (position) in the stream. */
	private long offset = 0;

	/**
	 * Constructs an <code>MP4DataStream</code> object using the specified
	 * MPEG4 input stream.
	 * 
	 * @param is the MPEG4 input stream.
	 */
	public MP4DataStream(FileInputStream is) {
		channel = is.getChannel();
	}

	/**
	 * Reads "n" bytes from the channel.
	 * 
	 * @param n
	 * @return long value from the channel
	 * @throws IOException
	 */
	public long readBytes(int n) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(n);
		channel.read(buf);
		buf.flip();
		long result = 0;
		switch (n) {
			case 1:
				result = buf.get();
				break;
			case 2:
				result = buf.getShort();
				break;
			case 4:
				result = buf.getInt();
				break;
			case 8:
				result = buf.getLong();
				break;
			default:
				// anything longer than long is not yet supported
		}
		buf.clear();
		// set the current position in offset
		offset = channel.position();
		//System.out.println(result);
		return result;
	}

	/**
	 * Returns a string of "n" bytes in length from the channel.
	 * 
	 * @param n
	 * @return string from the channel
	 * @throws IOException
	 */
	public String readString(int n) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(n);
		channel.read(buf);
		buf.flip();
		// set the current position in offset
		offset = channel.position();
		String result = new String(buf.array());
		//System.out.println(result);
		return result;
	}

	/**
	 * Skip ahead in the channel by "n" bytes.
	 * 
	 * @param n
	 * @throws IOException
	 */
	public void skipBytes(long n) throws IOException {
		channel.position(channel.position() + n);
		// set the current position in offset
		offset = channel.position();
	}

	public long getOffset() {
		try {
			return channel.position();
		} catch (IOException e) {
		}
		return offset;
	}

	public FileChannel getChannel() {
		return channel;
	}

	public void close() throws IOException {
		channel.close();
		channel = null;
	}
}
