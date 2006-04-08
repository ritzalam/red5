package org.red5.server.net.rtmp.codec;

import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.message.InPacket;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.net.rtmp.message.PacketHeader;

public class RTMP extends ProtocolState {
	
	public static final byte STATE_CONNECT = 0x00;
	public static final byte STATE_HANDSHAKE = 0x01;
	public static final byte STATE_CONNECTED = 0x02;
	public static final byte STATE_DISCONNECTED = 0x03;
	
	public static final boolean MODE_CLIENT = true;
	public static final boolean MODE_SERVER = false;
	
	public static final int DEFAULT_CHUNK_SIZE = 128;
	
	private byte state = STATE_CONNECT;
	private boolean mode = MODE_SERVER;
	
	private byte lastReadChannel = 0x00;
	private byte lastWriteChannel = 0x00;
	private PacketHeader[] readHeaders = new PacketHeader[128]; 
	private PacketHeader[] writeHeaders = new PacketHeader[128]; 
	private InPacket[] readPackets = new InPacket[128];
	private OutPacket[] writePackets = new OutPacket[128];
	private int readChunkSize = DEFAULT_CHUNK_SIZE;
	private int writeChunkSize = DEFAULT_CHUNK_SIZE;
	
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
	
	public void setLastReadHeader(byte channelId, PacketHeader header){
		lastReadChannel = channelId;
		readHeaders[channelId] = header;
	}
	
	public PacketHeader getLastReadHeader(byte channelId){
		return readHeaders[channelId];
	}
	
	public void setLastWriteHeader(byte channelId, PacketHeader header){
		lastWriteChannel = channelId;
		writeHeaders[channelId] = header;
	}
	
	public PacketHeader getLastWriteHeader(byte channelId){
		return writeHeaders[channelId];
	}

	public void setLastReadPacket(byte channelId, InPacket packet){
		readPackets[channelId] = packet;
	}
	
	public InPacket getLastReadPacket(byte channelId){
		return readPackets[channelId];
	}
	
	public void setLastWritePacket(byte channelId, OutPacket packet){
		writePackets[channelId] = packet;
	}
	
	public OutPacket getLastWritePacket(byte channelId){
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