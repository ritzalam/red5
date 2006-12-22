package org.red5.server.net.rtmp.codec;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
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

import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;

/**
 * RTMP is RTMP protocol state representation
 */
public class RTMP extends ProtocolState {
    /**
     * Connect state
     */
	public static final byte STATE_CONNECT = 0x00;
    /**
     * Handshake state. Server sends handshake request to client right after connection estabilished.
     */
	public static final byte STATE_HANDSHAKE = 0x01;
    /**
     * Connected
     */
	public static final byte STATE_CONNECTED = 0x02;
    /**
     * Error
     */
	public static final byte STATE_ERROR = 0x03;
    /**
     * Disconnected
     */
	public static final byte STATE_DISCONNECTED = 0x04;
    /**
     * Client mode
     */
	public static final boolean MODE_CLIENT = true;
    /**
     * Server mode
     */
	public static final boolean MODE_SERVER = false;
    /**
     * Default chunk size. Packets are read and written chunk-by-chunk
     */
	public static final int DEFAULT_CHUNK_SIZE = 128;
    /**
     * RTMP state
     */
	private byte state = STATE_CONNECT;
    /**
     * Server mode by default
     */
	private boolean mode = MODE_SERVER;
    /**
     * Debug flag
     */
	private boolean debug;
    /**
     * Last read channel
     */
	private byte lastReadChannel = 0x00;
    /**
     * Last write channel
     */
	private byte lastWriteChannel = 0x00;
    /**
     * Read headers
     */
	private Header[] readHeaders = new Header[128];
    /**
     * Write headers
     */
	private Header[] writeHeaders = new Header[128];
    /**
     * Read packets
     */
	private Packet[] readPackets = new Packet[128];
    /**
     * Written packets
     */
	private Packet[] writePackets = new Packet[128];
    /**
     * Read chunk size. Packets are read and written chunk-by-chunk
     */
	private int readChunkSize = DEFAULT_CHUNK_SIZE;
    /**
     * Write chunk size. Packets are read and written chunk-by-chunk
     */
	private int writeChunkSize = DEFAULT_CHUNK_SIZE;

    /**
     * Creates RTMP object with initial mode
     * @param mode            Initial mode
     */
	public RTMP(boolean mode) {
		this.mode = mode;
	}

	/**
     * Return current mode
     *
     * @return Value for property 'mode'.
     */
    public boolean getMode() {
		return mode;
	}

	/**
     * Getter for property 'debug'.
     *
     * @return Value for property 'debug'.
     */
    public boolean isDebug() {
		return debug;
	}

	/**
     * Setter for property 'debug'.
     *
     * @param debug Value to set for property 'debug'.
     */
    public void setDebug(boolean debug) {
		this.debug = debug;
	}

	/**
     * Return current state
     *
     * @return  State
     */
    public byte getState() {
		return state;
	}

    /**
     * Releases number of packets
     * @param packets            Packets to release
     */
    private void freePackets(Packet[] packets) {
		for (Packet packet : packets) {
			if (packet != null && packet.getData() != null) {
				packet.getData().release();
				packet.setData(null);
			}
		}
	}

	/**
     * Setter for state
     *
     * @param state  New state
     */
    public void setState(byte state) {
		this.state = state;
		if (state == STATE_DISCONNECTED) {
			// Free temporary packets
			freePackets(readPackets);
			freePackets(writePackets);
		}
	}

    /**
     * Setter for last read header
     * @param channelId            Channel id
     * @param header               Header
     */
    public void setLastReadHeader(byte channelId, Header header) {
		lastReadChannel = channelId;
		readHeaders[channelId] = header;
	}

    /**
     * Return last read header for channel
     * @param channelId             Channel id
     * @return                      Last read header
     */
	public Header getLastReadHeader(byte channelId) {
		return readHeaders[channelId];
	}

    /**
     * Setter for last written header
     * @param channelId             Channel id
     * @param header                Header
     */
	public void setLastWriteHeader(byte channelId, Header header) {
		lastWriteChannel = channelId;
		writeHeaders[channelId] = header;
	}

    /**
     * Return last written header for channel
     * @param channelId             Channel id
     * @return                      Last written header
     */
	public Header getLastWriteHeader(byte channelId) {
		return writeHeaders[channelId];
	}

    /**
     * Setter for last read packet
     * @param channelId           Channel id
     * @param packet              Packet
     */
	public void setLastReadPacket(byte channelId, Packet packet) {
		Packet prevPacket = readPackets[channelId];
		if (prevPacket != null && prevPacket.getData() != null) {
			prevPacket.getData().release();
			prevPacket.setData(null);
		}

		readPackets[channelId] = packet;
	}

    /**
     * Return last read packet for channel
     * @param channelId           Channel id
     * @return                    Last read packet for that channel
     */
	public Packet getLastReadPacket(byte channelId) {
		return readPackets[channelId];
	}

    /**
     * Setter for last written packet
     * @param channelId           Channel id
     * @param packet              Last written packet
     */
	public void setLastWritePacket(byte channelId, Packet packet) {
		Packet prevPacket = writePackets[channelId];
		if (prevPacket != null && prevPacket.getData() != null) {
			prevPacket.getData().release();
			prevPacket.setData(null);
		}

		writePackets[channelId] = packet;
	}

    /**
     * Return packet that has been written last
     * @param channelId           Channel id
     * @return                    Packet that has been written last
     */
	public Packet getLastWritePacket(byte channelId) {
		return writePackets[channelId];
	}

	/**
     * Return channel being read last
     *
     * @return  Last read channel
     */
    public byte getLastReadChannel() {
		return lastReadChannel;
	}

	/**
     * Getter for channel being written last
     *
     * @return  Last write channel
     */
    public byte getLastWriteChannel() {
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
     * Setter for  write chunk size
     *
     * @param writeChunkSize  Write chunk size
     */
    public void setWriteChunkSize(int writeChunkSize) {
		this.writeChunkSize = writeChunkSize;
	}

}