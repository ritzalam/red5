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

import java.util.HashMap;
import java.util.Map;

import org.red5.server.api.IConnection.Encoding;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;

/**
 * RTMP is the RTMP protocol state representation.
 */
public class RTMP extends ProtocolState {
	
	public String[] states = {"connect", "handshake", "connected", "error", "disconnecting", "disconnected"};
	
	/**
	 * Connect state.
	 */
	public static final byte STATE_CONNECT = 0x00;

	/**
	 * Handshake state. Server sends handshake request to client right after connection established.
	 */
	public static final byte STATE_HANDSHAKE = 0x01;

	/**
	 * Connected.
	 */
	public static final byte STATE_CONNECTED = 0x02;

	/**
	 * Error.
	 */
	public static final byte STATE_ERROR = 0x03;

	/**
	 * In the processing of disconnecting
	 */
	public static final byte STATE_DISCONNECTING = 0x04;	
	
	/**
	 * Disconnected.
	 */
	public static final byte STATE_DISCONNECTED = 0x05;

	/**
	 * Sent the connect message to origin.
	 */
	public static final byte STATE_EDGE_CONNECT_ORIGIN_SENT = 0x11;

	/**
	 * Forwarded client's connect call to origin.
	 */
	public static final byte STATE_ORIGIN_CONNECT_FORWARDED = 0x12;

	/**
	 * Edge is disconnecting, waiting Origin close connection.
	 */
	public static final byte STATE_EDGE_DISCONNECTING = 0x13;

	/**
	 * Default chunk size. Packets are read and written chunk-by-chunk.
	 */
	public static final int DEFAULT_CHUNK_SIZE = 128;

	/**
	 * RTMP state.
	 */
	private volatile byte state = STATE_CONNECT;

	/**
	 * Debug flag.
	 */
	private boolean debug;

	/**
	 * Encryption flag.
	 */
	private boolean encrypted = false;

	/**
	 * Last read channel.
	 */
	private int lastReadChannel = 0x00;

	/**
	 * Last write channel.
	 */
	private int lastWriteChannel = 0x00;

	/**
	 * Read headers, keyed by channel id.
	 */
	private final Map<Integer, Header> readHeaders = new HashMap<Integer, Header>();

	/**
	 * Write headers, keyed by channel id.
	 */
	private final Map<Integer, Header> writeHeaders = new HashMap<Integer, Header>();

	/**
	 * Headers actually used for a packet, keyed by channel id.
	 */
	private final Map<Integer, Header> readPacketHeaders = new HashMap<Integer, Header>();

	/**
	 * Read packets, keyed by channel id.
	 */
	private final Map<Integer, Packet> readPackets = new HashMap<Integer, Packet>();

	/**
	 * Written packets, keyed by channel id.
	 */
	private final Map<Integer, Packet> writePackets = new HashMap<Integer, Packet>();

	/**
	 * Written timestamps
	 */
	private final Map<Integer, Integer> writeTimestamps = new HashMap<Integer, Integer>();

	/**
	 * Class for mapping between clock time and stream time for live streams
	 * @author aclarke
	 *
	 */
	static class LiveTimestampMapping {
		private final long clockStartTime;

		private final long streamStartTime;

		private boolean keyFrameNeeded;

		private long lastStreamTime;

		public LiveTimestampMapping(long clockStartTime, long streamStartTime) {
			this.clockStartTime = clockStartTime;
			this.streamStartTime = streamStartTime;
			this.keyFrameNeeded = true; // Always start with a key frame
			this.lastStreamTime = streamStartTime;
		}

		public long getStreamStartTime() {
			return streamStartTime;
		}

		public long getClockStartTime() {
			return clockStartTime;
		}

		public void setKeyFrameNeeded(boolean keyFrameNeeded) {
			this.keyFrameNeeded = keyFrameNeeded;
		}

		public boolean isKeyFrameNeeded() {
			return keyFrameNeeded;
		}

		public long getLastStreamTime() {
			return lastStreamTime;
		}

		public void setLastStreamTime(long lastStreamTime) {
			this.lastStreamTime = lastStreamTime;
		}
	}

	/**
	 * Mapping between channel and the last clock to stream mapping
	 */
	private final Map<Integer, LiveTimestampMapping> liveTimestamps = new HashMap<Integer, LiveTimestampMapping>();

	/**
	 * Read chunk size. Packets are read and written chunk-by-chunk.
	 */
	private int readChunkSize = DEFAULT_CHUNK_SIZE;

	/**
	 * Write chunk size. Packets are read and written chunk-by-chunk.
	 */
	private int writeChunkSize = DEFAULT_CHUNK_SIZE;

	/**
	 * Encoding type for objects.
	 */
	private Encoding encoding = Encoding.AMF0;

	/**
	 * Creates RTMP object; essentially for storing session information.
	 */
	public RTMP() {
	}

	/**
	 * Getter for debug.
	 *
	 * @return  Debug state
	 */
	public boolean isDebug() {
		return debug;
	}

	/**
	 * Setter for debug.
	 *
	 * @param debug  Debug flag new value
	 */
	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
	 * @return the encrypted
	 */
	public boolean isEncrypted() {
		return encrypted;
	}

	/**
	 * @param encrypted the encrypted to set
	 */
	public void setEncrypted(boolean encrypted) {
		this.encrypted = encrypted;
	}

	/**
	 * Return current state.
	 *
	 * @return  State
	 */
	public byte getState() {
		return state;
	}

	/**
	 * Releases number of packets.
	 *
	 * @param packets            Packets to release
	 */
	private void freePackets(Map<Integer, Packet> packets) {
		for (Packet packet : packets.values()) {
			if (packet != null && packet.getData() != null) {
				packet.getData().free();
				packet.setData(null);
			}
		}
		packets.clear();
	}

	/**
	 * Setter for state.
	 *
	 * @param state  New state
	 */
	public void setState(byte state) {
		this.state = state;
		if (state == STATE_DISCONNECTED) {
			// Free temporary packets
			freePackets(readPackets);
			freePackets(writePackets);
			readHeaders.clear();
			writeHeaders.clear();
		}
	}

	/**
	 * Setter for last read header.
	 *
	 * @param channelId            Channel id
	 * @param header               Header
	 */
	public void setLastReadHeader(int channelId, Header header) {
		lastReadChannel = channelId;
		readHeaders.put(channelId, header);
	}

	/**
	 * Return last read header for channel.
	 *
	 * @param channelId             Channel id
	 * @return                      Last read header
	 */
	public Header getLastReadHeader(int channelId) {
		return readHeaders.get(channelId);
	}

	/**
	 * Setter for last written header.
	 *
	 * @param channelId             Channel id
	 * @param header                Header
	 */
	public void setLastWriteHeader(int channelId, Header header) {
		lastWriteChannel = channelId;
		writeHeaders.put(channelId, header);
	}

	/**
	 * Return last written header for channel.
	 *
	 * @param channelId             Channel id
	 * @return                      Last written header
	 */
	public Header getLastWriteHeader(int channelId) {
		return writeHeaders.get(channelId);
	}

	/**
	 * Setter for last read packet.
	 *
	 * @param channelId           Channel id
	 * @param packet              Packet
	 */
	public void setLastReadPacket(int channelId, Packet packet) {
		Packet prevPacket = readPackets.put(channelId, packet);
		if (prevPacket != null && prevPacket.getData() != null) {
			prevPacket.getData().free();
			prevPacket.setData(null);
		}
	}

	/**
	 * Return last read packet for channel.
	 *
	 * @param channelId           Channel id
	 * @return                    Last read packet for that channel
	 */
	public Packet getLastReadPacket(int channelId) {
		return readPackets.get(channelId);
	}

	/**
	 * Setter for last written packet.
	 *
	 * @param channelId           Channel id
	 * @param packet              Last written packet
	 */
	public void setLastWritePacket(int channelId, Packet packet) {
		// Disabled to help GC because we currently don't use the write packets
		/*
		Packet prevPacket = writePackets.put(channelId, packet);
		if (prevPacket != null && prevPacket.getData() != null) {
			prevPacket.getData().release();
			prevPacket.setData(null);
		}
		*/
	}

	/**
	 * Return packet that has been written last.
	 *
	 * @param channelId           Channel id
	 * @return                    Packet that has been written last
	 */
	public Packet getLastWritePacket(int channelId) {
		return writePackets.get(channelId);
	}

	/**
	 * Return channel being read last.
	 *
	 * @return  Last read channel
	 */
	public int getLastReadChannel() {
		return lastReadChannel;
	}

	/**
	 * Getter for channel being written last.
	 *
	 * @return  Last write channel
	 */
	public int getLastWriteChannel() {
		return lastWriteChannel;
	}

	/**
	 * Getter for  write chunk size. Data is being read chunk-by-chunk.
	 *
	 * @return  Read chunk size
	 */
	public int getReadChunkSize() {
		return readChunkSize;
	}

	/**
	 * Setter for  read chunk size. Data is being read chunk-by-chunk.
	 *
	 * @param readChunkSize Value to set for property 'readChunkSize'.
	 */
	public void setReadChunkSize(int readChunkSize) {
		this.readChunkSize = readChunkSize;
	}

	/**
	 * Getter for  write chunk size. Data is being written chunk-by-chunk.
	 *
	 * @return  Write chunk size
	 */
	public int getWriteChunkSize() {
		return writeChunkSize;
	}

	/**
	 * Setter for  write chunk size.
	 *
	 * @param writeChunkSize  Write chunk size
	 */
	public void setWriteChunkSize(int writeChunkSize) {
		this.writeChunkSize = writeChunkSize;
	}

	/**
	 * Getter for encoding version.
	 * 
	 * @return Encoding version
	 */
	public Encoding getEncoding() {
		return encoding;
	}

	/**
	 * Setter for encoding version.
	 * 
	 * @param encoding	Encoding version
	 */
	public void setEncoding(Encoding encoding) {
		this.encoding = encoding;
	}

	public void setLastFullTimestampWritten(int channelId, int timer) {
		writeTimestamps.put(channelId, timer);
	}

	public Integer getLastFullTimestampWritten(int channelId) {
		return writeTimestamps.get(channelId);
	}

	public void setLastReadPacketHeader(int channelId, Header header) {
		readPacketHeaders.put(channelId, header);
	}

	public Header getLastReadPacketHeader(int channelId) {
		return readPacketHeaders.get(channelId);
	}

	LiveTimestampMapping getLastTimestampMapping(int channelId) {
		return liveTimestamps.get(channelId);
	}

	void setLastTimestampMapping(int channelId, LiveTimestampMapping mapping) {
		liveTimestamps.put(channelId, mapping);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "RTMP [state=" + states[state] + ", debug=" + debug + ", encrypted=" + encrypted + ", lastReadChannel=" + lastReadChannel + ", lastWriteChannel="
				+ lastWriteChannel + ", readHeaders=" + readHeaders + ", writeHeaders=" + writeHeaders + ", readPacketHeaders=" + readPacketHeaders + ", readPackets="
				+ readPackets + ", writePackets=" + writePackets + ", writeTimestamps=" + writeTimestamps + ", liveTimestamps=" + liveTimestamps + ", readChunkSize="
				+ readChunkSize + ", writeChunkSize=" + writeChunkSize + ", encoding=" + encoding + "]";
	}

}
