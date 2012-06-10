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

package org.red5.server.net.rtmpt;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.core.buffer.IoBuffer;
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
	private ConcurrentLinkedQueue<PendingData> pendingMessages = new ConcurrentLinkedQueue<PendingData>();

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
	private volatile IoBuffer buffer;

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
		log.debug("close - state: {}", state.getState());
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
		if (isClosing()) {
			if (buffer != null) {
				buffer.free();
				buffer = null;
			}
			state.setState(RTMP.STATE_DISCONNECTED);
			pendingMessages.clear();
			super.close();
		}
	}

	/**
	 * Send raw data down the connection.
	 *
	 * @param packet the buffer containing the raw data
	 */
	@Override
	public void writeRaw(IoBuffer packet) {
		log.debug("Adding pending message from raw packet");
		pendingMessages.add(new PendingData(packet));
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
		log.debug("Checking pending queue size");
		return pendingMessages.size();
	}

	/**
	 * Decode data sent by the client.
	 *
	 * @param data the data to decode
	 * @return a list of decoded objects
	 */
	public List<?> decode(IoBuffer data) {
		log.debug("decode");
		if (closing || state.getState() == RTMP.STATE_DISCONNECTED) {
			// connection is being closed, don't decode any new packets
			return Collections.EMPTY_LIST;
		}
		readBytes.addAndGet(data.limit());
		buffer.put(data);
		buffer.flip();
		return decoder.decodeBuffer(state, buffer);
	}

	/**
	 * Send RTMP packet down the connection.
	 *
	 * @param packet the packet to send
	 */
	@Override
	public void write(final Packet packet) {
		log.debug("write - packet: {}", packet);
		log.trace("state: {}", state);
		if (closing || state.getState() == RTMP.STATE_DISCONNECTED) {
			// connection is being closed, don't send any new packets
			log.debug("No write completed due to connection disconnecting");
		} else {
			IoBuffer data = null;
			try {
				data = encoder.encode(state, packet);
			} catch (Exception e) {
				log.error("Could not encode message {}", packet, e);
			}
			if (data != null) {
				// add to pending
				log.debug("Adding pending message from packet");
				pendingMessages.add(new PendingData(data, packet));
			} else {
				log.info("Response buffer was null after encoding");
			}
		}
	}

	protected IoBuffer foldPendingMessages(int targetSize) {
		log.debug("foldPendingMessages - target size: {}", targetSize);
		IoBuffer result = null;
		if (!pendingMessages.isEmpty()) {
			int sendSize = 0;
			LinkedList<PendingData> sendList = new LinkedList<PendingData>();
			while (!pendingMessages.isEmpty()) {
				// get the buffer size
				int limit = pendingMessages.peek().getBuffer().limit();
				if ((limit + sendSize) < targetSize) {
					if (sendList.add(pendingMessages.poll())) {
						sendSize += limit;
					}
				} else {
					if (sendSize == 0) {
						if (sendList.add(pendingMessages.poll())) {
							sendSize = limit;
						}
					}
					break;
				}
			}
			log.debug("Send size: {}", sendSize);
			result = IoBuffer.allocate(sendSize);
			for (PendingData pendingMessage : sendList) {
				result.put(pendingMessage.getBuffer());
				Packet packet = pendingMessage.getPacket(); 
				if (packet != null) {
					try {
						handler.messageSent(this, packet);
						// mark packet as being written
						writingMessage(packet);
					} catch (Exception e) {
						log.error("Could not notify stream subsystem about sent message", e);
					}
				} else {
					log.debug("Pending message did not have a packet");
				}
			}
			sendList.clear();
			result.flip();
			writtenBytes.addAndGet(sendSize);
		}
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
