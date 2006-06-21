package org.red5.server.net.rtmp_refactor.codec;

import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp_refactor.message.Header;
import org.red5.server.net.rtmp_refactor.message.Packet;

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
	
	public void setState(byte state) {
		this.state = state;
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
		readPackets[channelId] = packet;
	}
	
	public Packet getLastReadPacket(byte channelId){
		return readPackets[channelId];
	}
	
	public void setLastWritePacket(byte channelId, Packet packet){
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