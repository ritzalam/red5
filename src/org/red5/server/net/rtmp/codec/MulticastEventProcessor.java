package org.red5.server.net.rtmp.codec;

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

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Processes multicast events.
 */
public class MulticastEventProcessor {

	/**
     * Getter for cache ID.
     *
     * @return  Cache ID
     */
    public byte getCacheId() {
		return 0;
	}

    /**
     * Disposes cached object.
	 *
     * @param obj                Cached object
     */
    public void disposeCached(Object obj) {
		if (obj == null) {
			return;
		}
		final IoBuffer[] chunks = (IoBuffer[]) obj;
		for (int c=0;c < chunks.length;c++) {
			chunks[c].free();
			chunks[c] = null;
		}
	}

    /**
     * Breaks buffer into chunks of given size.
	 *
     * @param buf                IoBuffer
     * @param size               Chunk size
     * @return                   Array of byte buffers, chunks
     */
    public static IoBuffer[] chunkBuffer(IoBuffer buf, int size) {
		final int num = (int) Math.ceil(buf.limit() / (float) size);
		final IoBuffer[] chunks = new IoBuffer[num];
		for (int i = 0; i < num; i++) {
			chunks[i] = buf.asReadOnlyBuffer();
			final IoBuffer chunk = chunks[i];
			int position = size * num;
			chunk.position(position);
			if (position + size < chunk.limit()) {
				chunk.limit(position + size);
			}
		}
		return chunks;
	}

}
