package org.red5.server.net.rtmp.codec;

import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;

public class RTMP extends ProtocolState {
	
	public static final byte STATE_CONNECT = 0x00;
	public static final byte STATE_HANDSHAKE = 0x01;
	public static final byte STATE_CONNECTED = 0x02;
	public static final byte STATE_ERROR = 0x03;
	public static final byte STATE_DISCONNECTED = 0x04;
	
	public static final boolean MODE_CLIENT = true;
	public static final boolean MODE_SERVER = false;
	
	public static final int DEFAULT_CHUNK_SIZE = 128;
	
	private byte state = STATE_CONNECT;
	private boolean mode = MODE_SERVER;
	
	private final static int MAX_STREAMS = 12;
	
	private byte lastReadChannel = 0x00;
	private byte lastWriteChannel = 0x00;
	private Header[] readHeaders = new Header[128]; 
	private Header[] writeHeaders = new Header[128]; 
	private Packet[] readPackets = new Packet[128];
	private Packet[] writePackets = new Packet[128];
	private int readChunkSize = DEFAULT_CHUNK_SIZE;
	private int writeChunkSize = DEFAULT_CHUNK_SIZE;
	
	private int[] streamIds = new int[MAX_STREAMS];
	
	public RTMP(boolean mode){
		this.mode = mode;
	}
	
	public boolean getMode(){
		return mode;
	}

	public byte getState() {
		return state;
	}
	
	private void freePackets(Packet[] packets) {
		for (Packet packet: packets) {
			if (packet != null && packet.getData() != null) {
				packet.getData().release();
				packet.setData(null);
			}
		}
	}
	
	public void setState(byte state) {
		this.state = state;
		if (state == STATE_DISCONNECTED) {
			// Free temporary packets
			freePackets(readPackets);
			freePackets(writePackets);
		}
	}
	
	public void setLastReadHeader(byte channelId, Header header){
		lastReadChannel = channelId;
		readHeaders[channelId] = header;
	}
	
	public Header getLastReadHeader(byte channelId){
		return readHeaders[channelId];
	}
	
	public void setLastWriteHeader(byte channelId, Header header){
		lastWriteChannel = channelId;
		writeHeaders[channelId] = header;
	}
	
	public Header getLastWriteHeader(byte channelId){
		return writeHeaders[channelId];
	}

	public void setLastReadPacket(byte channelId, Packet packet){
		Packet prevPacket = readPackets[channelId];
		if (prevPacket != null && prevPacket.getData() != null) {
			prevPacket.getData().release();
			prevPacket.setData(null);
		}

		readPackets[channelId] = packet;
	}
	
	public Packet getLastReadPacket(byte channelId){
		return readPackets[channelId];
	}
	
	public void setLastWritePacket(byte channelId, Packet packet){
		Packet prevPacket = writePackets[channelId];
		if (prevPacket != null && prevPacket.getData() != null) {
			prevPacket.getData().release();
			prevPacket.setData(null);
		}

		writePackets[channelId] = packet;
	}
	
	public Packet getLastWritePacket(byte channelId){
		return writePackets[channelId];
	}

	public byte getLastReadChannel() {
		return lastReadChannel;
	}

	public byte getLastWriteChannel() {
		return lastWriteChannel;
	}

	public int getReadChunkSize() {
		return readChunkSize;
	}

	public void setReadChunkSize(int readChunkSize) {
		this.readChunkSize = readChunkSize;
	}

	public int getWriteChunkSize() {
		return writeChunkSize;
	}

	public void setWriteChunkSize(int writeChunkSize) {
		this.writeChunkSize = writeChunkSize;
	}
	
}