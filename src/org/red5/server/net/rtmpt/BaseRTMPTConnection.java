package org.red5.server.net.rtmpt;

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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.IRTMPHandler;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmpt.codec.RTMPTProtocolDecoder;
import org.red5.server.net.rtmpt.codec.RTMPTProtocolEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseRTMPTConnection extends RTMPConnection {
	
	private static final Logger log = LoggerFactory.getLogger(BaseRTMPTConnection.class);
	
	/**
	 * Protocol decoder
	 */
	private RTMPTProtocolDecoder decoder;
	
	/**
	 * Protocol encoder
	 */
	private RTMPTProtocolEncoder encoder;
	
	private static class PendingData {
		private IoBuffer buffer;
		private Packet packet;

		private PendingData(IoBuffer buffer, Packet packet) {
			this.buffer = buffer;
			this.packet = packet;
		}

		private PendingData(IoBuffer buffer) {
			this.buffer = buffer;
		}

		public IoBuffer getBuffer() {
			return buffer;
		}

		public Packet getPacket() {
			return packet;
		}

		public String toString() {
			return getClass().getName() + "(buffer=" + buffer + "; packet=" + packet + ")";
		}
	}
	
	/**
	 * List of pending messages
	 */
	private LinkedList<PendingData> pendingMessages = new LinkedList<PendingData>();
	
	/**
	 * Closing flag
	 */
	private volatile boolean closing;
	
	/**
	 * Number of read bytes
	 */
	private AtomicLong readBytes = new AtomicLong(0);
	
	/**
	 * Number of written bytes
	 */
	private AtomicLong writtenBytes = new AtomicLong(0);
	
	/**
	 * Byte buffer
	 */
	private IoBuffer buffer;
	
	/**
	 * RTMP events handler
	 */
	private volatile IRTMPHandler handler;

	public BaseRTMPTConnection(String type) {
		super(type);
		this.buffer = IoBuffer.allocate(2048);
		this.buffer.setAutoExpand(true);
	}
	
	/**
	 * Return any pending messages up to a given size.
	 *
	 * @param targetSize the size the resulting buffer should have
	 * @return a buffer containing the data to send or null if no messages are
	 *         pending
	 */
	abstract public IoBuffer getPendingMessages(int targetSize);


	/** {@inheritDoc} */
	@Override
	public void close() {
		// Defer actual closing so we can send back pending messages to the client.
		closing = true;
	}

	/**
	 * Getter for property 'closing'.
	 *
	 * @return Value for property 'closing'.
	 */
	public boolean isClosing() {
		return closing;
	}
	
	/**
	 * Real close
	 */
	public void realClose() {
		if (!isClosing()) {
			return;
		}
		getWriteLock().lock();
		try {
			if (buffer != null) {
				buffer.free();
				buffer = null;
			}
			state.setState(RTMP.STATE_DISCONNECTED);
			pendingMessages.clear();
		} finally {
			getWriteLock().unlock();
		}
		super.close();
	}

	/**
	 * Send raw data down the connection.
	 *
	 * @param packet the buffer containing the raw data
	 */
	@Override
	public void rawWrite(IoBuffer packet) {
		getWriteLock().lock();
		try {
			pendingMessages.add(new PendingData(packet));
		} finally {
			getWriteLock().unlock();
		}
	}

	/** {@inheritDoc} */
	@Override
	public long getReadBytes() {
		return readBytes.get();
	}

	/** {@inheritDoc} */
	@Override
	public long getWrittenBytes() {
		return writtenBytes.get();
	}

	/** {@inheritDoc} */
	@Override
	public long getPendingMessages() {
		getReadLock().lock();
		try {
			return pendingMessages.size();
		} finally {
			getReadLock().unlock();
		}
	}

	/**
	 * Decode data sent by the client.
	 *
	 * @param data the data to decode
	 * @return a list of decoded objects
	 */
	public List<?> decode(IoBuffer data) {
		if (closing || state.getState() == RTMP.STATE_DISCONNECTED) {
			// Connection is being closed, don't decode any new packets
			return Collections.EMPTY_LIST;
		}
		//set the local connection
		Red5.setConnectionLocal(this);
		getWriteLock().lock();
		try {
			readBytes.addAndGet(data.limit());
			this.buffer.put(data);
			this.buffer.flip();
			return this.decoder.decodeBuffer(this.state, this.buffer);
		} finally {
			getWriteLock().unlock();
		}
	}

	/**
	 * Send RTMP packet down the connection.
	 *
	 * @param packet the packet to send
	 */
	@Override
	public void write(final Packet packet) {
		if (closing || state.getState() == RTMP.STATE_DISCONNECTED) {
			// Connection is being closed, don't send any new packets
			return;
		}
		getWriteLock().lock();
		try {
			IoBuffer data;
			try {
				data = this.encoder.encode(this.state, packet);
			} catch (Exception e) {
				log.error("Could not encode message {}", packet, e);
				return;
			}

			if (data != null) {
    			// Mark packet as being written
    			writingMessage(packet);
    			//add to pending
    			pendingMessages.add(new PendingData(data, packet));			
			} else {
				log.info("Response buffer was null after encoding");
			}
		} finally {
			getWriteLock().unlock();
		}
	}

	protected IoBuffer foldPendingMessages(int targetSize) {
		if (pendingMessages.isEmpty()) {
			return null;
		}
		
		IoBuffer result = IoBuffer.allocate(2048);
		result.setAutoExpand(true);
		
		// We'll have to create a copy here to avoid endless recursion
		List<Object> toNotify = new LinkedList<Object>();

		getWriteLock().lock();
		try {
			while (!pendingMessages.isEmpty()) {
				PendingData pendingMessage = pendingMessages.remove();
				result.put(pendingMessage.getBuffer());
				if (pendingMessage.getPacket() != null) {
					toNotify.add(pendingMessage.getPacket());
				}
				if ((result.position() > targetSize)) {
					break;
				}
			}
		} finally {
			getWriteLock().unlock();
		}
		
		for (Object message : toNotify) {
			try {
				handler.messageSent(this, message);
			} catch (Exception e) {
				log.error("Could not notify stream subsystem about sent message.", e);
			}
		}

		result.flip();
		writtenBytes.addAndGet(result.limit());
		return result;
	}

	public void setHandler(IRTMPHandler handler) {
		this.handler = handler;
	}

	public void setDecoder(RTMPProtocolDecoder decoder) {
		this.decoder = (RTMPTProtocolDecoder) decoder;
	}

	public void setEncoder(RTMPProtocolEncoder encoder) {
		this.encoder = (RTMPTProtocolEncoder) encoder;
	}
}
