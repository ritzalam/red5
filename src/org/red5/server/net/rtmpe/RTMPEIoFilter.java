package org.red5.server.net.rtmpe;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
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

import javax.crypto.Cipher;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.codec.RTMP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPE IO filter
 * 
 * @author Peter Thomas (ptrthomas@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPEIoFilter extends IoFilterAdapter {

	private static final Logger log = LoggerFactory.getLogger(RTMPEIoFilter.class);

	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session, Object obj) throws Exception {
		//if there is a handshake on the session, ensure the type has been set
		if (session.containsAttribute(RTMPConnection.RTMP_HANDSHAKE)) {
			log.trace("Handshake exists on the session");
			RTMP rtmp = (RTMP) session.getAttribute(ProtocolState.SESSION_KEY);
			//get the handshake from the session
			RTMPHandshake handshake = (RTMPHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
			if (handshake.getHandshakeType() == 0) {
				log.trace("Handshake type is not currently set");
				// holds the handshake type, default is un-encrypted
				byte handshakeType = RTMPConnection.RTMP_NON_ENCRYPTED;
				//get the current message
				if (obj instanceof IoBuffer) {
					IoBuffer message = (IoBuffer) obj;
					message.mark();
					handshakeType = message.get();
					message.reset();
				}
				//set the type
				handshake.setHandshakeType(handshakeType);
				//set on the rtmp state
				rtmp.setEncrypted(handshakeType == RTMPConnection.RTMP_ENCRYPTED ? true : false);
			}
			//if we are connected and doing encryption, add the ciphers
			if (rtmp.getState() == RTMP.STATE_CONNECTED) {
				log.debug("In connected state");
				// remove handshake from session now that we are connected
				session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
    			// if we are using encryption then put the ciphers in the session
        		if (handshake.getHandshakeType() == RTMPConnection.RTMP_ENCRYPTED) {
    				log.debug("Using encrypted communications");
        			//make sure they are not already on the session
        			if (session.containsAttribute(RTMPConnection.RTMPE_CIPHER_IN)) {
            			log.debug("Ciphers already exist on the session");
        			} else {
            			log.debug("Adding ciphers to the session");
            			session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherIn());
            			session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherOut());
        			}
        		} else {
    				log.debug("Using non-encrypted communications");
				}
			}			
		}
		Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN);
		if (cipher != null) { //may want to verify handshake is complete as well
			// assume message is an IoBuffer
			IoBuffer message = (IoBuffer) obj;
			log.debug("Decrypting buffer: {}", message);
			byte[] encrypted = new byte[message.remaining()];
			message.get(encrypted);
			message.clear();
			message.free();
			byte[] plain = cipher.update(encrypted);
			IoBuffer messageDecrypted = IoBuffer.wrap(plain);
			log.debug("Decrypted buffer: {}", messageDecrypted);
			nextFilter.messageReceived(session, messageDecrypted);
		} else {
			log.trace("Not decrypting message received: {}", obj);
			nextFilter.messageReceived(session, obj);
		}
	}

	@Override
	public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception {
		Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
		if (cipher != null) { //may want to verify handshake is complete as well
			IoBuffer message = (IoBuffer) request.getMessage();
			if (!message.hasRemaining()) {
				// Ignore empty buffers
				log.debug("Buffer was empty");
			} else {
				log.debug("Encrypting buffer: {}", message);
				byte[] plain = new byte[message.remaining()];
				message.get(plain);
				message.clear();
				message.free();
				//encrypt and write
				byte[] encrypted = cipher.update(plain);
				IoBuffer messageEncrypted = IoBuffer.wrap(encrypted);
				log.debug("Encrypted buffer: {}", messageEncrypted);
				nextFilter.filterWrite(session, new EncryptedWriteRequest(request, messageEncrypted));
			}
		} else {
			log.trace("Not encrypting write request");
			nextFilter.filterWrite(session, request);
		}
	}

	private static class EncryptedWriteRequest extends WriteRequestWrapper {
		private final IoBuffer encryptedMessage;

		private EncryptedWriteRequest(WriteRequest writeRequest, IoBuffer encryptedMessage) {
			super(writeRequest);
			this.encryptedMessage = encryptedMessage;
		}

		@Override
		public Object getMessage() {
			return encryptedMessage;
		}
	}

}
