package org.red5.server.net.rtmpt;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.Red5;
import org.red5.server.net.protocol.SimpleProtocolDecoder;
import org.red5.server.net.protocol.SimpleProtocolEncoder;
import org.red5.server.net.rtmp.IRTMPHandler;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseRTMPTConnection extends RTMPConnection {
	
	private static final Logger log = LoggerFactory.getLogger(BaseRTMPTConnection.class);
	
	/**
	 * Protocol decoder
	 */
	private SimpleProtocolDecoder decoder;
	
	/**
	 * Protocol encoder
	 */
	private SimpleProtocolEncoder encoder;
	
	private static class PendingData {
		private ByteBuffer buffer;
		private Packet packet;

		private PendingData(ByteBuffer buffer, Packet packet) {
			this.buffer = buffer;
			this.packet = packet;
		}

		private PendingData(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		public ByteBuffer getBuffer() {
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
	private ByteBuffer buffer;
	
	/**
	 * RTMP events handler
	 */
	private volatile IRTMPHandler handler;

	public BaseRTMPTConnection(String type) {
		super(type);
		this.buffer = ByteBuffer.allocate(2048);
		this.buffer.setAutoExpand(true);
	}
	
	/**
	 * Return any pending messages up to a given size.
	 *
	 * @param targetSize
	 * 			the size the resulting buffer should have
	 * @return a buffer containing the data to send or null if no messages are
	 *         pending
	 */
	abstract public ByteBuffer getPendingMessages(int targetSize);


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
			buffer = null;
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
	 * @param packet
	 * 			the buffer containing the raw data
	 */
	@Override
	public void rawWrite(ByteBuffer packet) {
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
	 * @param data
	 * 			the data to decode
	 * @return a list of decoded objects
	 */
	public List<?> decode(ByteBuffer data) {
		getWriteLock().lock();
		try {
			if (closing || state.getState() == RTMP.STATE_DISCONNECTED) {
				// Connection is being closed, don't decode any new packets
				return Collections.EMPTY_LIST;
			}

			Red5.setConnectionLocal(this);
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
	 * @param packet
	 * 			the packet to send
	 */
	@Override
	public void write(final Packet packet) {
		getWriteLock().lock();
		try {
			if (closing || state.getState() == RTMP.STATE_DISCONNECTED) {
				// Connection is being closed, don't send any new packets
				return;
			}

			ByteBuffer data;
			try {
				data = this.encoder.encode(this.state, packet);
			} catch (Exception e) {
				log.error("Could not encode message {}", packet, e);
				return;
			}

			// Mark packet as being written
			writingMessage(packet);

			pendingMessages.add(new PendingData(data, packet));
		} finally {
			getWriteLock().unlock();
		}
	}

	protected ByteBuffer foldPendingMessages(int targetSize) {
		ByteBuffer result = ByteBuffer.allocate(2048);
		result.setAutoExpand(true);
		
		// We'll have to create a copy here to avoid endless recursion
		List<Object> toNotify = new LinkedList<Object>();

		getWriteLock().lock();
		try {
			if (pendingMessages.isEmpty()) {
				return null;
			}

			while (!pendingMessages.isEmpty() 
					&& (result.limit() + pendingMessages.peek().getBuffer().limit() 
						< targetSize)) 
			{
				PendingData pendingMessage = pendingMessages.remove();
				result.put(pendingMessage.getBuffer());
				if (pendingMessage.getPacket() != null)
					toNotify.add(pendingMessage.getPacket());
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

	public void setDecoder(SimpleProtocolDecoder decoder) {
		this.decoder = decoder;
	}

	public void setEncoder(SimpleProtocolEncoder encoder) {
		this.encoder = encoder;
	}
}
