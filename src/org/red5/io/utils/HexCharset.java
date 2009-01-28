package org.red5.io.utils;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * This was borrowed from the Soupdragon base64 library.
 *
 * @author paul.gregoire
 */
public class HexCharset extends Charset {

	public class Decoder extends CharsetDecoder {

		private int charCount;

		public CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
			while (in.remaining() > 0) {
				if (measure != null && charCount >= measure.intValue()) {
					if (out.remaining() == 0)
						return CoderResult.OVERFLOW;
					out.put('\n');
					charCount = 0;
				}
				if (out.remaining() < 2)
					return CoderResult.OVERFLOW;
				int b = in.get() & 0xff;
				out.put(codes.charAt(b >>> 4));
				out.put(codes.charAt(b & 0xf));
				charCount += 2;
			}
			return CoderResult.UNDERFLOW;
		}

		protected void implReset() {
			charCount = 0;
		}

		private Decoder() {
			super(HexCharset.this, 2.0F,
					measure != null ? 2.0F + 2.0F / (float) measure.intValue()
							: 2.0F);
		}

		Decoder(_cls1 x1) {
			this();
			return;
		}

	}

	public class Encoder extends CharsetEncoder {

		private boolean unpaired;

		private int nyble;

		protected CoderResult implFlush(ByteBuffer out) {
			if (!unpaired) {
				implReset();
				return CoderResult.UNDERFLOW;
			} else {
				throw new IllegalArgumentException(
						"Hex string must be an even number of digits");
			}
		}

		public CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
			do {
				if (in.remaining() <= 0)
					break;
				if (out.remaining() <= 0)
					return CoderResult.OVERFLOW;
				char inch = in.get();
				if (!Character.isWhitespace(inch)) {
					int d = Character.digit(inch, 16);
					if (d < 0)
						throw new IllegalArgumentException(
								(new StringBuilder()).append(
										"Bad hex character ").append(inch)
										.toString());
					if (unpaired)
						out.put((byte) (nyble | d));
					else
						nyble = d << 4;
					unpaired = !unpaired;
				}
			} while (true);
			return CoderResult.UNDERFLOW;
		}

		protected void implReset() {
			unpaired = false;
			nyble = 0;
		}

		private Encoder() {
			super(HexCharset.this, 0.49F, 1.0F);
		}

		Encoder(_cls1 x1) {
			this();
			return;
		}

	}

	private static final String codeHEX = "0123456789ABCDEF";

	private static final String codehex = "0123456789abcdef";

	private String codes;

	private Integer measure;

	public HexCharset(boolean caps) {
		super(caps ? "HEX" : "hex", new String[] { "HEX" });
		codes = caps ? "0123456789ABCDEF" : "0123456789abcdef";
	}

	public HexCharset(boolean caps, int measure) {
		super((new StringBuilder()).append(caps ? "HEX" : "hex").append(":")
				.append(measure).toString(), new String[] { "HEX" });
		codes = caps ? codeHEX : codehex;
		this.measure = Integer.valueOf(measure);
	}

	public CharsetEncoder newEncoder() {
		return new Encoder();
	}

	public CharsetDecoder newDecoder() {
		return new Decoder();
	}

	public boolean contains(Charset cs) {
		return cs instanceof HexCharset;
	}

	static Integer access$200(HexCharset x0) {
		return x0.measure;
	}

	static String access$300(HexCharset x0) {
		return x0.codes;
	}

	// Unreferenced inner class soupdragon/codec/HexCharset$1

	/* anonymous class */
	static class _cls1 {

	}

}