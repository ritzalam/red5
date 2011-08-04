package org.red5.server.stream;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.stream.IVideoStreamCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for video codecs. Creates and returns video codecs
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class VideoCodecFactory {
    /**
     * Object key
     */
	public static final String KEY = "videoCodecFactory";
	
    /**
     * Logger for video factory
     */
	private static Logger log = LoggerFactory.getLogger(VideoCodecFactory.class);
    
	/**
     * List of available codecs
     */
	private static List<IVideoStreamCodec> codecs = new ArrayList<IVideoStreamCodec>(3);

	/**
     * Setter for codecs
     *
     * @param codecs List of codecs
     */
    public void setCodecs(List<IVideoStreamCodec> codecs) {
    	VideoCodecFactory.codecs = codecs;
	}

    /**
     * Create and return new video codec applicable for byte buffer data
     * @param data                 Byte buffer data
     * @return                     Video codec
     */
	public static IVideoStreamCodec getVideoCodec(IoBuffer data) {
		IVideoStreamCodec result = null;
		//get the codec identifying byte
		int codecId = data.get() & 0x0f;		
		try {
    		switch (codecId) {
    			case 2: //sorenson 
    				result = (IVideoStreamCodec) Class.forName("org.red5.server.stream.codec.SorensonVideo").newInstance();
    				break;
    			case 3: //screen video
    				result = (IVideoStreamCodec) Class.forName("org.red5.server.stream.codec.ScreenVideo").newInstance();
    				break;
    			case 7: //avc/h.264 video
    				result = (IVideoStreamCodec) Class.forName("org.red5.server.stream.codec.AVCVideo").newInstance();
    				break;
    		}
		} catch (Exception ex) {
			log.error("Error creating codec instance", ex);			
		}
		data.rewind();
		//if codec is not found do the old-style loop
		if (result == null) {
    		for (IVideoStreamCodec storedCodec: codecs) {
    			IVideoStreamCodec codec;
    			// XXX: this is a bit of a hack to create new instances of the
    			// configured video codec for each stream
    			try {
    				codec = storedCodec.getClass().newInstance();
    			} catch (Exception e) {
    				log.error("Could not create video codec instance", e);
    				continue;
    			}
    			if (codec.canHandleData(data)) {
    				result = codec;
    				break;
    			}
    		}
		}
		return result;
	}
	
//	private boolean isScreenVideo(byte first) {
//    	log.debug("Trying ScreenVideo");
//		boolean result = ((first & 0x0f) == 3);
//		return result;
//	}
//	
//	private boolean isSorenson(byte first) {
//    	log.debug("Trying Sorenson");
//		boolean result = ((first & 0x0f) == 2);
//		return result;
//	}
	
}
