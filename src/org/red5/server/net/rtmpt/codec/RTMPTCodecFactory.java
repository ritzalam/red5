/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
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

package org.red5.server.net.rtmpt.codec;

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
	
	private long baseTolerance = 5000;
	
	private boolean dropLiveFuture;
	
	/**
	 * Initialization
	 */
	public void init() {
		decoder = new RTMPTProtocolDecoder();
		decoder.setDeserializer(deserializer);
		encoder = new RTMPTProtocolEncoder();
		encoder.setSerializer(serializer);
		encoder.setBaseTolerance(baseTolerance);
		encoder.setDropLiveFuture(dropLiveFuture);
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
	
	/**
	 * @param baseTolerance the baseTolerance to set
	 */
	public void setBaseTolerance(long baseTolerance) {
		this.baseTolerance = baseTolerance;
	}

	/**
	 * @param dropLiveFuture the dropLiveFuture to set
	 */
	public void setDropLiveFuture(boolean dropLiveFuture) {
		this.dropLiveFuture = dropLiveFuture;
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
