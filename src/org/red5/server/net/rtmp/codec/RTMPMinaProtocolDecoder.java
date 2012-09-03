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

import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.red5.io.object.Deserializer;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP protocol decoder.
 */
public class RTMPMinaProtocolDecoder extends ProtocolDecoderAdapter {

	protected static Logger log = LoggerFactory.getLogger(RTMPMinaProtocolDecoder.class);

	private RTMPProtocolDecoder decoder = new RTMPProtocolDecoder();

	/** {@inheritDoc} */
	public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws ProtocolCodecException {
		//get our state
		final ProtocolState state = (ProtocolState) session.getAttribute(ProtocolState.SESSION_KEY);
		//create a buffer and store it on the session
		IoBuffer buf = (IoBuffer) session.getAttribute("buffer");
		if (buf == null) {
			buf = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
			buf.setAutoExpand(true);
			session.setAttribute("buffer", buf);
		}
		buf.put(in);
		buf.flip();
		//construct any objects from the decoded bugger
		List<?> objects = decoder.decodeBuffer(state, buf);
		if (objects != null) {
			for (Object object : objects) {
				out.write(object);
			}
		}
	}

	/**
	 * Sets the RTMP protocol decoder.
	 * 
	 * @param decoder
	 */
	public void setDecoder(RTMPProtocolDecoder decoder) {
		this.decoder = decoder;
	}

	/**
	 * Returns an RTMP decoder
	 * @return RTMP decoder
	 */
	public RTMPProtocolDecoder getDecoder() {
		return decoder;
	}
	
	/**
	 * Setter for deserializer.
	 * 
	 * @param deserializer Deserializer
	 */
	public void setDeserializer(Deserializer deserializer) {
		decoder.setDeserializer(deserializer);
	}


}
