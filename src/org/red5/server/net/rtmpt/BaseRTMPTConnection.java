package org.red5.server.net.rtmpt;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
	protected SimpleProtocolDecoder decoder;
	
	/**
	 * Protocol encoder
	 */
	protected SimpleProtocolEncoder encoder;
	
	/**
	 * List of pending messages
	 */
	protected List<ByteBuffer> pendingMessages = new LinkedList<ByteBuffer>();// reentrant lock for pending messages
    private final ReentrantReadWriteLock pendingRWLock = new ReentrantReadWriteLock();
	protected final Lock pendingRead  = pendingRWLock.readLock();
	protected final Lock pendingWrite = pendingRWLock.writeLock();
	
	/**
	 * List of notification messages
	 */
	protected List<Object> notifyMessages = new LinkedList<Object>();// reentrant lock for notification messages
    private final ReentrantReadWriteLock notifyRWLock = new ReentrantReadWriteLock();
	protected final Lock notifyRead  = notifyRWLock.readLock();
	protected final Lock notifyWrite = notifyRWLock.writeLock();
	
	/**
	 * Closing flag
	 */
	volatile protected boolean closing;
	/**
	 * Number of read bytes
	 */
	protected long readBytes;
	
	/**
	 * Number of written bytes
	 */
	protected long writtenBytes;
	
	/**
	 * Byte buffer
	 */
	protected ByteBuffer buffer;
	
	/**
	 * RTMP events handler
	 */
	protected IRTMPHandler handler;

	public BaseRTMPTConnection(String type) {
		super(type);
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
		if (buffer != null) {
			buffer.release();
			buffer = null;
		}
		notifyMessages.clear();
		state.setState(RTMP.STATE_DISCONNECTED);
		super.close();
		for (ByteBuffer buf : pendingMessages) {
			buf.release();
		}
		pendingMessages.clear();
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
		pendingWrite.lock();
		try {
			pendingMessages.add(packet);
		} catch (Exception e) {
			log.warn("Exception adding pending packet", e);
		} finally {
			pendingWrite.unlock();
		}		
	}

	/** {@inheritDoc} */
	@Override
	public long getReadBytes() {
		return readBytes;
	}

	/** {@inheritDoc} */
	@Override
	public long getWrittenBytes() {
		return writtenBytes;
	}

	/** {@inheritDoc} */
	@Override
	public long getPendingMessages() {
		pendingRead.lock();
		try {
			return pendingMessages.size();
		} finally {
			pendingRead.unlock();
		}
	}

	/**
	 * Decode data sent by the client.
	 *
	 * @param data
	 * 			the data to decode
	 * @return a list of decoded objects
	 */
	public List decode(ByteBuffer data) {
		if (closing || state.getState() == RTMP.STATE_DISCONNECTED) {
			// Connection is being closed, don't decode any new packets
			return Collections.EMPTY_LIST;
		}

		Red5.setConnectionLocal(this);
		readBytes += data.limit();
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
	@Override
	public void write(final Packet packet) {
		if (closing || state.getState() == RTMP.STATE_DISCONNECTED) {
			// Connection is being closed, don't send any new packets
			return;
		}

		// We need to synchronize to prevent two packages to the
		// same channel to be sent in different order thus resulting
		// in wrong headers being generated.
		ByteBuffer data;
		try {
			data = this.encoder.encode(this.state, packet);
		} catch (Exception e) {
			log.error("Could not encode message {}", packet, e);
			return;
		}

		// Mark packet as being written
		writingMessage(packet);

		// Enqueue encoded packet data to be sent to client
		rawWrite(data);

		// Make sure stream subsystem will be notified about sent packet later
		notifyWrite.lock();
		try {
			notifyMessages.add(packet);
		} catch (Exception e) {
			log.warn("Exception adding notify packet", e);
		} finally {
			notifyWrite.unlock();
		}		
	}

	protected ByteBuffer foldPendingMessages(int targetSize) {
		ByteBuffer result = ByteBuffer.allocate(2048);
		result.setAutoExpand(true);
		while (result.limit() < targetSize) {
			pendingRead.lock();
			try {
				if (pendingMessages.isEmpty()) {
					break;
				}
				
				for (ByteBuffer buffer : pendingMessages) {
					result.put(buffer);
					buffer.release();
				}
			} catch (Exception e) {
				log.warn("Exception adding pending messages", e);
			} finally {
				pendingRead.unlock();
			}	
			
			pendingWrite.lock();
			try {
				pendingMessages.clear();
			} catch (Exception e) {
				log.warn("Exception clearing pending messages", e);
			} finally {
				pendingWrite.unlock();
			}				

			// We'll have to create a copy here to avoid endless recursion
			List<Object> toNotify = new LinkedList<Object>();
			
			notifyRead.lock();
			try {
				toNotify.addAll(notifyMessages);
			} catch (Exception e) {
				log.warn("Exception adding notify messages", e);
			} finally {
				notifyRead.unlock();
			}	

			notifyWrite.lock();
			try {
				notifyMessages.clear();
			} catch (Exception e) {
				log.warn("Exception clearing notify messages", e);
			} finally {
				notifyWrite.unlock();
			}				
			
			for (Object message : toNotify) {
				try {
					handler.messageSent(this, message);
				} catch (Exception e) {
					log.error("Could not notify stream subsystem about sent message.", e);
					continue;
				}
			}
		}

		result.flip();
		writtenBytes += result.limit();
		return result;
	}
}
