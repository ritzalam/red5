package org.red5.server.net.rtmpt;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.ProtocolState;
import org.red5.server.net.rtmp.Connection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.net.rtmpt.codec.RTMPTProtocolDecoder;
import org.red5.server.net.rtmpt.codec.RTMPTProtocolEncoder;
import org.red5.server.stream.Stream;

public class RTMPTClient extends Connection {

	protected static Log log =
        LogFactory.getLog(RTMPTClient.class.getName());
	
	// Start to increase the polling delay after this many empty results
	protected static final long INCREASE_POLLING_DELAY_COUNT = 10;
	protected static final byte INITIAL_POLLING_DELAY = 0;
	protected static final byte MAX_POLLING_DELAY = 32;
	
	protected RTMP state;
	protected RTMPTHandler handler;
	protected RTMPTProtocolDecoder decoder;
	protected RTMPTProtocolEncoder encoder;
	protected ByteBuffer buffer;
	protected List pendingMessages = new LinkedList();
	protected List notifyMessages = new LinkedList();
	protected byte pollingDelay = INITIAL_POLLING_DELAY;
	protected long noPendingMessages = 0;
	
	public RTMPTClient(RTMPTHandler handler) {
		super(null);
		this.state = new RTMP(RTMP.MODE_SERVER);
		this.buffer = ByteBuffer.allocate(2048);
		this.buffer.setAutoExpand(true);
		this.handler = handler;
		this.decoder = (RTMPTProtocolDecoder) handler.getCodecFactory().getSimpleDecoder();
		this.encoder = (RTMPTProtocolEncoder) handler.getCodecFactory().getSimpleEncoder();
	}
	
	public String getId() {
		return new Integer(this.hashCode()).toString();
	}
	
	public ProtocolState getState() {
		return this.state;
	}
	
	public byte getPollingDelay() {
		return (byte) (this.pollingDelay + 1);
	}
	
	public List decode(ByteBuffer data) {
		this.buffer.put(data);
		this.buffer.flip();
		return this.decoder.decodeBuffer(this.state, this.buffer);
	}

	public void write(OutPacket packet){
		ByteBuffer data;
		try {
			data = this.encoder.encode(this.state, packet);
		} catch (Exception e) {
			log.error("Could not encode message " + packet, e);
			return;
		}
		
		// Enqueue encoded packet data to be sent to client
		write(data);
		
		// Make sure stream subsystem will be notified about sent packet later
		synchronized (this.notifyMessages) {
			this.notifyMessages.add(packet);
		}
	}

	public void write(ByteBuffer packet){
		synchronized (this.pendingMessages) {
			this.pendingMessages.add(packet);
		}
	}

	public ByteBuffer getPendingMessages() {
		if (this.pendingMessages.size() == 0) {
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
			OutPacket packet = (OutPacket) it.next();
			try {
				// Notify stream subsystem that packet has been sent
				final byte channelId = packet.getDestination().getChannelId();
				final Stream stream = this.getStreamByChannelId(channelId);
				if (stream != null) {
					stream.written(packet.getMessage());
				}
			} catch (Exception e) {
				log.error("Could not notify stream subsystem about sent message.", e);
				continue;
			}
		}
		
		result.flip();
		return result;
	}
	
}
