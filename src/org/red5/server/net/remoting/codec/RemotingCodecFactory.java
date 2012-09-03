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

package org.red5.server.net.remoting.codec;

import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

/**
 * Factory for remoting codec
 */
public class RemotingCodecFactory {
	
	/**
	 * Deserializer
	 */
	protected Deserializer deserializer;

	/**
	 * Serializers
	 */
	protected Serializer serializer;

	/**
	 * Remoting protocol decoder
	 */
	protected RemotingProtocolDecoder decoder;

	/**
	 * Remoting protocol encoder
	 */
	protected RemotingProtocolEncoder encoder;

	/**
	 * Initialization, creates and binds encoder and decoder to serializer and deserializer
	 */
	public void init() {
		decoder = new RemotingProtocolDecoder();
		decoder.setDeserializer(deserializer);
		encoder = new RemotingProtocolEncoder();
		encoder.setSerializer(serializer);
	}

	/**
	 * Setter for deserializer.
	 *
	 * @param deserializer Deserializer.
	 */
	public void setDeserializer(Deserializer deserializer) {
		this.deserializer = deserializer;
	}

	/**
	 * Setter for serializer.
	 *
	 * @param serializer Sserializer.
	 */
	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}

	/**
	 * Returns the remoting decoder.
	 * 
	 * @return decoder
	 */
	public RemotingProtocolDecoder getRemotingDecoder() {
		return decoder;
	}

	/**
	 * Returns the remoting encoder.
	 * 
	 * @return encoder
	 */
	public RemotingProtocolEncoder getRemotingEncoder() {
		return encoder;
	}

}
