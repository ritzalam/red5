package org.red5.server.stream.codec;

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
import org.red5.io.utils.HexDump;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.stream.IAudioStreamCodec;
import org.slf4j.Logger;

/**
 * Red5 audio codec for the AAC audio format.
 *
 * Stores the decoder configuration
 * 
 * @author Paul Gregoire (mondain@gmail.com) 
 * @author Wittawas Nakkasem (vittee@hotmail.com)
 */
public class AACAudio implements IAudioStreamCodec {

	private static Logger log = Red5LoggerFactory.getLogger(AACAudio.class);
	
	public static final int[] AAC_SAMPLERATES = { 96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050, 16000, 12000, 11025, 8000, 7350 };
	
    /**
     * AAC audio codec constant
     */
	static final String CODEC_NAME = "AAC";
	
	/**
     * Block of data (AAC DecoderConfigurationRecord)
     */
	private byte[] blockDataAACDCR;
	
	private int sampleRateIndex = 0;
	private int channels = 0;
	private int profile = 0;
	private int currentFrameLeft = 0;
	private int rawDataBlock = 0;
	
	/** Constructs a new AVCVideo. */
    public AACAudio() {
		this.reset();
	}

	/** {@inheritDoc} */
    public String getName() {
		return CODEC_NAME;
	}

	/** {@inheritDoc} */
    public void reset() {
		blockDataAACDCR = null;
	}

	/** {@inheritDoc} */
    public boolean canHandleData(IoBuffer data) {
		if (data.limit() == 0) {
			// Empty buffer
			return false;
		}

		byte first = data.get();
		boolean result = ((first & 0x0f) == AudioCodec.AAC.getId());
		data.rewind();
		return result;
	}

	/** {@inheritDoc} */
    public boolean addData(IoBuffer data) {
		int dataLength = data.limit();
		if (dataLength > 0) {
		
    		//ensure we are at the beginning
    		data.rewind();
    	
    		byte frameType = data.get();
    		log.trace("Frame type: {}", frameType);
    
    		//go back to beginning
    		data.rewind();
    			
    		//If we don't have the AACDecoderConfigurationRecord stored...
    		if (blockDataAACDCR == null) {
        		// parse
        		int offset = 0;
        		int adtsSkipped = 0;
        		while ((dataLength - offset) > 7) {
    				if ((data.get(offset++) & 0xff) == 0xff) {
    					if ((data.get(offset++) & 0xf6) == 0xf0) {
    						byte b = data.get(offset);
    						profile = (b & 0xC0) >> 6;
    						log.trace("Profile: {}", profile);
    						sampleRateIndex = (b & 0x3C) >> 2;
    						log.trace("Sample rate index: {}", sampleRateIndex);
    						channels = ((b & 0x01) << 2) | ((data.get(offset + 1) & 0xC0) >> 6);			
    						log.trace("Channels: {}", channels);
    						
    						// frame length, ADTS excluded
    						currentFrameLeft = (((data.get(offset + 1) & 0x03) << 8) 
    								| ((data.get(offset + 2) & 0xff) << 3) | ((data.get(offset + 3) & 0xff) >>> 5)) - 7;
    						log.trace("Current frame left: {}", currentFrameLeft);
    						rawDataBlock = data.get(offset + 4) & 0x3;						
    						log.trace("Raw data: {}", rawDataBlock);
    						offset += 5; // skip ADTS		
    						adtsSkipped += 7;
    
    		        		//generate the config data
    		        		byte[] config = getAACSpecificConfig();

    		        		//create the decoder config array
    		        		blockDataAACDCR = new byte[config.length + 2];

    		        		//add the aac configuration packet prefix
    		        		blockDataAACDCR[0] = (byte) 0xaf;
    		        		blockDataAACDCR[1] = (byte) 0x00;
    		        		//copy the remaining config data
    		        		System.arraycopy(config, 0, blockDataAACDCR, 2, config.length - 1);
    						
    						break;
    					}
    				}						
        		}		
        		       		
        		data.rewind();
    		}
		
		}

		return true;
	}
    
    /** {@inheritDoc} */
    public IoBuffer getDecoderConfiguration() {
		if (blockDataAACDCR == null) {
			return null;
		}

		IoBuffer result = IoBuffer.allocate(4);
		result.setAutoExpand(true);
		result.put(blockDataAACDCR);
		result.rewind();
		return result;
	}
    
    @SuppressWarnings("unused")
	private long sample2TC(long time, int sampleRate) {
    	return (time * 1000L / sampleRate);
    }
    
	private final byte[] getAACSpecificConfig() {		
		byte[] b = new byte[] { 
				(byte) (0x10 | /*((profile > 2) ? 2 : profile << 3) | */((sampleRateIndex >> 1) & 0x03)),
				(byte) (((sampleRateIndex & 0x01) << 7) | ((channels & 0x0F) << 3))
			};
		log.debug("SpecificAudioConfig {}", HexDump.toHexString(b));
		return b;	
	}    
}
