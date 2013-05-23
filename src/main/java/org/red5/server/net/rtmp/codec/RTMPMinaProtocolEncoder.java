/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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

package org.red5.server.net.rtmp.codec;

import java.util.LinkedList;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.red5.server.net.rtmp.RTMPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mina protocol encoder for RTMP.
 */
public class RTMPMinaProtocolEncoder extends ProtocolEncoderAdapter {

	protected static Logger log = LoggerFactory.getLogger(RTMPMinaProtocolEncoder.class);

	private RTMPProtocolEncoder encoder = new RTMPProtocolEncoder();

	private int targetChunkSize = 2048;

	/** {@inheritDoc} */
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws ProtocolCodecException {
		// pass the connection to the encoder for its use; encoders are PER connection, they are not shared
		RTMPConnection conn = (RTMPConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY);
		encoder.setConnection(conn);
		// get our state
		RTMP state = conn.getState();
		try {
			final IoBuffer buf = encoder.encode(state, message);
			if (buf != null) {
				int requestedWriteChunkSize = state.getWriteChunkSize();
				log.debug("Requested chunk size: {} target chunk size: {}", requestedWriteChunkSize, targetChunkSize);
				if (buf.remaining() <= targetChunkSize * 2) {
					out.write(buf);
				} else {
					log.debug("Chunking output data");
					LinkedList<IoBuffer> chunks = Chunker.chunk(buf, requestedWriteChunkSize, targetChunkSize);
					for (IoBuffer chunk : chunks) {
						out.write(chunk);
					}
					chunks.clear();
					chunks = null;
				}
			} else {
				log.trace("Response buffer was null after encoding");
			}
			//			WriteFuture future = out.flush();
			//			if (future != null) {
			//				future.addListener(new IoFutureListener<WriteFuture>() {
			//					@Override
			//					public void operationComplete(WriteFuture future) {
			//						//log.debug("Buffer freed");
			//						buf.free();
			//					}
			//				});
			//			}
		} catch (Exception ex) {
			log.error("Exception during encode", ex);
		}
	}

	/**
	 * Sets an RTMP protocol encoder
	 * @param encoder the RTMP encoder
	 */
	public void setEncoder(RTMPProtocolEncoder encoder) {
		this.encoder = encoder;
	}

	/**
	 * Returns an RTMP encoder
	 * @return RTMP encoder
	 */
	public RTMPProtocolEncoder getEncoder() {
		return encoder;
	}

	/**
	 * Setter for baseTolerance
	 * */
	public void setBaseTolerance(long baseTolerance) {
		encoder.setBaseTolerance(baseTolerance);
	}

	/**
	 * Setter for dropLiveFuture
	 * */
	public void setDropLiveFuture(boolean dropLiveFuture) {
		encoder.setDropLiveFuture(dropLiveFuture);
	}

	/**
	 * @return the targetChunkSize
	 */
	public int getTargetChunkSize() {
		return targetChunkSize;
	}

	/**
	 * @param targetChunkSize the targetChunkSize to set
	 */
	public void setTargetChunkSize(int targetChunkSize) {
		this.targetChunkSize = targetChunkSize;
	}

	/**
	 * Output data chunker.
	 */
	private static final class Chunker {

		public static LinkedList<IoBuffer> chunk(IoBuffer message, int chunkSize, int desiredSize) {
			LinkedList<IoBuffer> chunks = new LinkedList<IoBuffer>();
			int targetSize = desiredSize > chunkSize ? desiredSize : chunkSize;
			int limit = message.limit();
			do {
				int length = 0;
				int pos = message.position();
				while (length < targetSize && pos < limit) {
					byte basicHeader = message.get(pos);
					length += getDataSize(basicHeader) + chunkSize;
					pos += length;
				}
				log.debug("Length: {} remaining: {} pos+len: {} limit: {}", new Object[] { length, message.remaining(), (message.position() + length), limit });
				if (length > message.remaining()) {
					length = message.remaining();
				}
				// add a chunk
				chunks.add(message.getSlice(length));
			} while (message.hasRemaining());
			return chunks;
		}

		private static int getDataSize(byte basicHeader) {
			final int streamId = basicHeader & 0x0000003F;
			final int headerType = (basicHeader >> 6) & 0x00000003;
			int size = 0;
			switch (headerType) {
				case 0:
					size = 12;
					break;
				case 1:
					size = 8;
					break;
				case 2:
					size = 4;
					break;
				default:
					size = 1;
					break;
			}
			if (streamId == 0) {
				size += 1;
			} else if (streamId == 1) {
				size += 2;
			}
			return size;
		}
	}

}
