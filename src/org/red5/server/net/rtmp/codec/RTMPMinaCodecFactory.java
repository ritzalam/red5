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

package org.red5.server.net.rtmp.codec;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

/**
 * RTMP codec factory.
 */
public class RTMPMinaCodecFactory implements ProtocolCodecFactory {
	
    /**
     * RTMP Mina protocol decoder.
     */
	protected RTMPMinaProtocolDecoder decoder;
    /**
     * RTMP Mina protocol encoder.
     */
	protected RTMPMinaProtocolEncoder encoder;

    /**
     * Initialization. 
     * Create and setup of encoder/decoder and serializer/deserializer is handled by Spring.
     */
    public void init() {
	}

	/**
     * Setter for encoder.
     *
     * @param encoder  Encoder
     */
    public void setMinaEncoder(RTMPMinaProtocolEncoder encoder) {
		this.encoder = encoder;
    }

	/**
     * Setter for decoder
     *
     * @param decoder  Decoder
     */
    public void setMinaDecoder(RTMPMinaProtocolDecoder decoder) {
		this.decoder = decoder;
    }

	/** {@inheritDoc} */
    public ProtocolDecoder getDecoder(IoSession session) {
		return decoder;
	}

	/** {@inheritDoc} */
    public ProtocolEncoder getEncoder(IoSession session) {
		return encoder;
	}
	
	/**
	 * 
	 * @return decoder
	 */
    public RTMPMinaProtocolDecoder getMinaDecoder() {
		return decoder;
	}

	/**
	 * 
	 * @return encoder
	 */
    public RTMPMinaProtocolEncoder getMinaEncoder() {
		return encoder;
	}	

}
