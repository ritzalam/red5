package org.red5.server.stream;

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.stream.IVideoStreamCodec;

public class VideoCodecFactory {

	public static final String KEY = "videoCodecFactory";

	private Log log = LogFactory.getLog(VideoCodecFactory.class.getName());

	private List codecs = new ArrayList();

	public void setCodecs(List codecs) {
		this.codecs = codecs;
	}

	public IVideoStreamCodec getVideoCodec(ByteBuffer data) {
		IVideoStreamCodec result = null;
		Iterator it = this.codecs.iterator();
		while (it.hasNext()) {
			IVideoStreamCodec codec;
			IVideoStreamCodec storedCodec = (IVideoStreamCodec) it.next();
			// XXX: this is a bit of a hack to create new instances of the
			// configured
			//      video codec for each stream
			try {
				codec = storedCodec.getClass().newInstance();
			} catch (Exception e) {
				log.error("Could not create video codec instance.", e);
				continue;
			}

			log.info("Trying codec " + codec);
			if (codec.canHandleData(data)) {
				result = codec;
				break;
			}
		}

		// No codec for this video data
		return result;
	}
}
