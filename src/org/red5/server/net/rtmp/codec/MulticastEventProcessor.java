package org.red5.server.net.rtmp.codec;

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

import org.apache.mina.common.ByteBuffer;

public class MulticastEventProcessor {

	/**
     * Getter for property 'cacheId'.
     *
     * @return Value for property 'cacheId'.
     */
    public byte getCacheId() {
		return 0;
	}

	public void disposeCached(Object obj) {
		if (obj == null) {
			return;
		}
		final ByteBuffer[] chunks = (ByteBuffer[]) obj;
		for (ByteBuffer buf : chunks) {
			buf.release();
		}
	}

	public static ByteBuffer[] chunkBuffer(ByteBuffer buf, int size) {
		final int num = (int) Math.ceil(buf.limit() / (float) size);
		final ByteBuffer[] chunks = new ByteBuffer[num];
		for (int i = 0; i < num; i++) {
			chunks[i] = buf.asReadOnlyBuffer();
			final ByteBuffer chunk = chunks[i];
			int position = size * num;
			chunk.position(position);
			if (position + size < chunk.limit()) {
				chunk.limit(position + size);
			}
		}
		return chunks;
	}

}
