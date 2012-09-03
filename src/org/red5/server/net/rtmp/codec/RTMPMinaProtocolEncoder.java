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

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mina protocol encoder for RTMP.
 */
public class RTMPMinaProtocolEncoder extends ProtocolEncoderAdapter {

	protected static Logger log = LoggerFactory.getLogger(RTMPMinaProtocolEncoder.class);

	private RTMPProtocolEncoder encoder = new RTMPProtocolEncoder();
	
	/** {@inheritDoc} */
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws ProtocolCodecException {
		final ProtocolState state = (ProtocolState) session.getAttribute(ProtocolState.SESSION_KEY);
		// pass the connection to the encoder for its use
		encoder.setConnection((RTMPConnection) session.getAttribute(RTMPConnection.RTMP_CONNECTION_KEY));
		try {
			// We need to synchronize on the output and flush the generated data to prevent two packages to the same channel
			// to be sent in different order thus resulting in wrong headers being generated.
			final IoBuffer buf = encoder.encode(state, message);
			if (buf != null) {
				out.write(buf);
				out.mergeAll();
				out.flush();
			} else {
				log.trace("Response buffer was null after encoding");
			}
		} catch (Exception ex) {
			log.error("Exception during encode", ex);
		}
	}

	/**
	 * Sets an RTMP protocol encoder
	 * @param encoder the RTMP encoder
	 */
	public void setEncoder(RTMPProtocolEncoder encoder) {
		this.encoder = encoder;
	}

	/**
	 * Returns an RTMP encoder
	 * @return RTMP encoder
	 */
	public RTMPProtocolEncoder getEncoder() {
		return encoder;
	}

	/**
	 * Setter for serializer.
	 *
	 * @param serializer Serializer
	 */
	public void setSerializer(org.red5.io.object.Serializer serializer) {
		encoder.setSerializer(serializer);
	}

	/**
	 * Setter for baseTolerance
	 * */
	public void setBaseTolerance(long baseTolerance) {
		encoder.setBaseTolerance(baseTolerance);
	}

	/**
	 * Setter for dropLiveFuture
	 * */
	public void setDropLiveFuture(boolean dropLiveFuture) {
		encoder.setDropLiveFuture(dropLiveFuture);
	}

}
