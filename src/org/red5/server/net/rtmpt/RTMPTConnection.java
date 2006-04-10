package org.red5.server.net.rtmpt;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.protocol.SimpleProtocolDecoder;
import org.red5.server.net.protocol.SimpleProtocolEncoder;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.OutPacket;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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

/**
 * A RTMPT client / session.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */

public class RTMPTConnection extends RTMPConnection {

	protected static Log log =
        LogFactory.getLog(RTMPTConnection.class.getName());
	
	/**
	 * Start to increase the polling delay after this many empty results
	 */
	protected static final long INCREASE_POLLING_DELAY_COUNT = 10;
	
	/**
	 * Polling delay to start with.
	 */
	protected static final byte INITIAL_POLLING_DELAY = 0;
	
	/**
	 * Maximum polling delay. 
	 */
	protected static final byte MAX_POLLING_DELAY = 32;
	
	protected RTMP state;
	protected SimpleProtocolDecoder decoder;
	protected SimpleProtocolEncoder encoder;
	protected RTMPHandler handler;
	protected ByteBuffer buffer;
	protected List pendingMessages = new LinkedList();
	protected List notifyMessages = new LinkedList();
	protected byte pollingDelay = INITIAL_POLLING_DELAY;
	protected long noPendingMessages = 0;
	
	public RTMPTConnection(RTMPTHandler handler) {
		super( POLLING );
		this.state = new RTMP(RTMP.MODE_SERVER);
		this.buffer = ByteBuffer.allocate(2048);
		this.buffer.setAutoExpand(true);
		this.handler = handler;
		this.decoder = handler.getCodecFactory().getSimpleDecoder();
		this.encoder = handler.getCodecFactory().getSimpleEncoder();
	}
	
	/**
	 * Return the client id for this connection.
	 * 
	 * @return the client id
	 */
	public String getId() {
		return new Integer(this.hashCode()).toString();
	}
	
	/**
	 * Return the current decoder state.
	 * 
	 * @return the current decoder state.
	 */
	public ProtocolState getState() {
		return this.state;
	}
	
	/**
	 * Return the polling delay to use.
	 * 
	 * @return the polling delay
	 */
	public byte getPollingDelay() {
		return (byte) (this.pollingDelay + 1);
	}
	
	/**
	 * Decode data sent by the client.
	 * 
	 * @param data
	 * 			the data to decode
	 * @return a list of decoded objects
	 */
	public List decode(ByteBuffer data) {
		this.buffer.put(data);
		this.buffer.flip();
		return this.decoder.decodeBuffer(this.state, this.buffer);
	}

	/**
	 * Send RTMP packet down the connection.
	 * 
	 * @param packet
	 * 			the packet to send
	 */
	public void write(OutPacket packet){
		ByteBuffer data;
		try {
			data = this.encoder.encode(this.state, packet);
		} catch (Exception e) {
			log.error("Could not encode message " + packet, e);
			return;
		}
		
		// Enqueue encoded packet data to be sent to client
		rawWrite(data);
		
		// Make sure stream subsystem will be notified about sent packet later
		synchronized (this.notifyMessages) {
			this.notifyMessages.add(packet);
		}
	}

	/**
	 * Send raw data down the connection.
	 * 
	 * @param packet
	 * 			the buffer containing the raw data
	 */
	public void rawWrite(ByteBuffer packet){
		synchronized (this.pendingMessages) {
			this.pendingMessages.add(packet);
		}
	}
	
	/**
	 * Send event down the connection.
	 * 
	 * @param event
	 * 			the event to send
	 */
	public void dispatchEvent(Object event) {
		if (event instanceof ByteBuffer) {
			rawWrite((ByteBuffer) event);
		} else if (event instanceof OutPacket) {
			write((OutPacket) event);
		} else
			log.error("Don't know how to dispatch event " + event);
	}

	/**
	 * Return any pending messages up to a given size.
	 * 
	 * @param targetSize
	 * 			the size the resulting buffer should have
	 * @return a buffer containing the data to send or null if no messages are pending
	 */
	public ByteBuffer getPendingMessages(int targetSize) {
		if (this.pendingMessages.isEmpty()) {
			this.noPendingMessages += 1;
			if (this.noPendingMessages > INCREASE_POLLING_DELAY_COUNT) {
				if (this.pollingDelay == 0)
					this.pollingDelay = 1;
				this.pollingDelay = (byte) (this.pollingDelay * 2);
				if (this.pollingDelay > MAX_POLLING_DELAY)
					this.pollingDelay = MAX_POLLING_DELAY;
			}
			return null;
		}		
		
		ByteBuffer result = ByteBuffer.allocate(2048);
		result.setAutoExpand(true);
		
		log.debug("Returning " + this.pendingMessages.size() + " messages to client.");
		this.noPendingMessages = 0;
		this.pollingDelay = INITIAL_POLLING_DELAY;
		while (result.limit() < targetSize) {
			if (this.pendingMessages.isEmpty())
				break;
			
			synchronized (this.pendingMessages) {
				Iterator it = this.pendingMessages.iterator();
				while (it.hasNext())
					result.put((ByteBuffer) it.next());
				
				this.pendingMessages.clear();
			}
			
			// We'll have to create a copy here to avoid endless recursion
			List toNotify = new LinkedList();
			synchronized (this.notifyMessages) {
				toNotify.addAll(this.notifyMessages);
				this.notifyMessages.clear();
			}
			
			Iterator it = toNotify.iterator();
			while (it.hasNext()) {
				try {
					handler.messageSent(this, it.next());
				} catch (Exception e) {
					log.error("Could not notify stream subsystem about sent message.", e);
					continue;
				}
			}
		}
		
		result.flip();
		return result;
	}
	
}
