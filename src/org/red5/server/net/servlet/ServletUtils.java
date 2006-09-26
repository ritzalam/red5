package org.red5.server.net.servlet;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ServletUtils {

	/**
	 * Default value is 2048.
	 */
	public static final int DEFAULT_BUFFER_SIZE = 2048;

	/**
	 * Copies information from the input stream to the output stream using a
	 * default buffer size of 2048 bytes.
	 * 
	 * @throws java.io.IOException
	 */
	public static void copy(InputStream input, OutputStream output)
			throws IOException {
		copy(input, output, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Copies information from the input stream to the output stream using the
	 * specified buffer size
	 * 
	 * @throws java.io.IOException
	 */
	public static void copy(InputStream input, OutputStream output,
			int bufferSize) throws IOException {
		byte[] buf = new byte[bufferSize];
		int bytesRead = input.read(buf);
		while (bytesRead != -1) {
			output.write(buf, 0, bytesRead);
			bytesRead = input.read(buf);
		}
		output.flush();
	}

	/**
	 * Copies information between specified streams and then closes both of the
	 * streams.
	 * 
	 * @throws java.io.IOException
	 */
	public static void copyThenClose(InputStream input, OutputStream output)
			throws IOException {
		copy(input, output);
		input.close();
		output.close();
	}

	/**
	 * @returns a byte[] containing the information contained in the specified
	 *          InputStream.
	 * @throws java.io.IOException
	 */
	public static byte[] getBytes(InputStream input) throws IOException {
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		copy(input, result);
		result.close();
		return result.toByteArray();
	}

}
