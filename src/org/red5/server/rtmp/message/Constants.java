package org.red5.server.rtmp.message;

public interface Constants {

	public static final byte TYPE_INVOKE = 0x14;
	public static final byte TYPE_NOTIFY = 0x12;
	
	public static final byte TYPE_VIDEO_DATA = 0x09;
	public static final byte TYPE_AUDIO_DATA = 0x08;
	
	public static final byte TYPE_CLIENT_BANDWIDTH = 0x06;
	public static final byte TYPE_SERVER_BANDWIDTH = 0x05;
	public static final byte TYPE_PING = 0x04;
	public static final byte TYPE_STREAM_BYTES_READ = 0x03;
	
	// somthing to do with shared object
	public static final byte TYPE_SHARED_OBJECT = 0x13;
	//public static final byte TYPE_SHARED_OBJECT_CONNECT = 0x13;
	
	public static final byte TYPE_HANDSHAKE = 0x30;
	public static final byte TYPE_HANDSHAKE_REPLY = 0x31;
	
	public static final byte HEADER_NEW = 0x00;
	public static final byte HEADER_SAME_SOURCE = 0x01;
	public static final byte HEADER_TIMER_CHANGE = 0x02;
	public static final byte HEADER_CONTINUE = 0x03;
	
	public static final int HANDSHAKE_SIZE = 1536;
	public static final int CHUNK_SIZE = 128;
	
}
