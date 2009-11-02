package org.red5.server.net.rtmpt.codec;

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

import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.net.rtmp.codec.RTMPCodecFactory;
import org.red5.server.net.rtmp.codec.RTMPProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;

/**
 * RTMPT codec factory creates RTMP codec objects
 */
public class RTMPTCodecFactory extends RTMPCodecFactory {

    /**
     * RTMP decoder
     */
	private RTMPTProtocolDecoder decoder;
	
    /**
     * RTMP encoder
     */
	private RTMPTProtocolEncoder encoder;

    /**
     * Initialization
     */
    public void init() {
		decoder = new RTMPTProtocolDecoder();
		decoder.setDeserializer(deserializer);
		encoder = new RTMPTProtocolEncoder();
		encoder.setSerializer(serializer);
	}

	/**
     * Setter for deserializer.
     *
     * @param deserializer  Deserializer used by this codec factory.
     */
    public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	/**
     * Setter for serializer
     *
     * @param serializer Value to set for property 'serializer'.
     */
    public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	/** {@inheritDoc} */
    @Override
    public RTMPProtocolDecoder getRTMPDecoder() {
		return decoder;
	}

	/** {@inheritDoc} */
    @Override
    public RTMPProtocolEncoder getRTMPEncoder() {
		return encoder;
	}
}
