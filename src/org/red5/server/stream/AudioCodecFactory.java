package org.red5.server.stream;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.stream.IAudioStreamCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for audio codecs. Creates and returns audio codecs
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Miguel Molina - SplitmediaLabs (MiMo@splitmedialabs.com)
 */
public class AudioCodecFactory {
    /**
     * Object key
     */
	public static final String KEY = "audioCodecFactory";
	
    /**
     * Logger for audio factory
     */
	private static Logger log = LoggerFactory.getLogger(AudioCodecFactory.class);
    
	/**
     * List of available codecs
     */
	private static List<IAudioStreamCodec> codecs = new ArrayList<IAudioStreamCodec>(1);

	/**
     * Setter for codecs
     *
     * @param codecs List of codecs
     */
    public void setCodecs(List<IAudioStreamCodec> codecs) {
    	AudioCodecFactory.codecs = codecs;
	}

    /**
     * Create and return new audio codec applicable for byte buffer data
     * @param data                 Byte buffer data
     * @return                     audio codec
     */
	public static IAudioStreamCodec getAudioCodec(IoBuffer data) {
		IAudioStreamCodec result = null;
		//get the codec identifying byte
		int codecId = (data.get() & 0xf0) >> 4;		
		try {
    		switch (codecId) {
    			case 10: //aac 
    				result = (IAudioStreamCodec) Class.forName("org.red5.server.stream.codec.AACAudio").newInstance();
    				break;
    		}
		} catch (Exception ex) {
			log.error("Error creating codec instance", ex);			
		}
		data.rewind();
		//if codec is not found do the old-style loop
		if (result == null) {
    		for (IAudioStreamCodec storedCodec: codecs) {
    			IAudioStreamCodec codec;
    			// XXX: this is a bit of a hack to create new instances of the
    			// configured audio codec for each stream
    			try {
    				codec = storedCodec.getClass().newInstance();
    			} catch (Exception e) {
    				log.error("Could not create audio codec instance", e);
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

}
