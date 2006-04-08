package org.red5.server.net.rtmp.message;

public interface Constants {

	public static final int MEDIUM_INT_MAX = 16777215;
	
	public static final byte TYPE_CHUNK_SIZE = 0x01;
	// Unknown: 0x02 
	public static final byte TYPE_STREAM_BYTES_READ = 0x03;
	public static final byte TYPE_PING = 0x04;
	public static final byte TYPE_SERVER_BANDWIDTH = 0x05;
	public static final byte TYPE_CLIENT_BANDWIDTH = 0x06;
	// Unknown: 0x07
	public static final byte TYPE_AUDIO_DATA = 0x08;
	public static final byte TYPE_VIDEO_DATA = 0x09;
	// Unknown: 0x0A ...  0x11
	public static final byte TYPE_NOTIFY = 0x12;
	public static final byte TYPE_STREAM_METADATA = 0x12;
	public static final byte TYPE_SHARED_OBJECT = 0x13;
	public static final byte TYPE_INVOKE = 0x14;
	
	public static final byte HEADER_NEW = 0x00;
	public static final byte HEADER_SAME_SOURCE = 0x01;
	public static final byte HEADER_TIMER_CHANGE = 0x02;
	public static final byte HEADER_CONTINUE = 0x03;
	
	public static final int HANDSHAKE_SIZE = 1536;
	
	public static final byte SO_CLIENT_UPDATE_DATA = 0x04; //update data
	public static final byte SO_CLIENT_UPDATE_ATTRIBUTE = 0x05; //5: update attribute
	public static final byte SO_CLIENT_SEND_MESSAGE = 0x06;  // 6: send message
    public static final byte SO_CLIENT_STATUS = 0x07;  // 7: status (usually returned with error messages)
    public static final byte SO_CLIENT_DELETE_DATA = 0x09; // 9: delete data
    public static final byte SO_CLIENT_INITIAL_DATA = 0x0B; // 11: initial data

    public static final byte SO_CONNECT = 0x01;
    public static final byte SO_CLEAR = 0x02;
    public static final byte SO_SET_ATTRIBUTE = 0x03;
    public static final byte SO_SEND_MESSAGE = 0x06;
    public static final byte SO_DELETE_ATTRIBUTE = 0x0A;
    public static final byte SO_LIST = 0x0B;
    public static final byte SO_CONNECT_OK = 0x08;
    
    public static final String ACTION_CONNECT = "connect";
    public static final String ACTION_DISCONNECT = "disconnect";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_SEEK = "seek";
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_STOP = "disconnect";    
    
}
