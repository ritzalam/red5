package org.red5.server.rtmp;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.protocol.ProtocolSession;
import org.red5.server.context.AppContext;
import org.red5.server.context.Client;
import org.red5.server.rtmp.message.OutPacket;
import org.red5.server.rtmp.message.Ping;
import org.red5.server.stream.DownStreamSink;
import org.red5.server.stream.Stream;

public class Connection extends Client {

	protected static Log log =
        LogFactory.getLog(Connection.class.getName());
	
	public static final byte STATE_CONNECT = 0;
	public static final byte STATE_HANDSHAKE = 1;
	public static final byte STATE_CONNECTED = 2;
	public static final byte STATE_DISCONNECTED = 3;
	public static final boolean MODE_CLIENT = true;
	public static final boolean MODE_SERVER = false;
	
	private ProtocolSession protocolSession;
	//private Context context;
	private byte state = STATE_CONNECT;
	private Channel[] channels = new Channel[64];
	private Stream[] streams = new Stream[12];
	private Channel lastReadChannel = null;
	private Channel lastWriteChannel = null;
	private AppContext appCtx = null;
	private boolean mode = MODE_SERVER;
	private int clientUptime = 0;
	
	public Connection(ProtocolSession protocolSession){
		this.protocolSession = protocolSession;
	}
	
	public AppContext getAppContext() {
		return appCtx;
	}
	
	public void setAppContext(AppContext appCtx) {
		this.appCtx = appCtx;
	}
	
	public ProtocolSession getProtocolSession() {
		return protocolSession;
	}
	
	public byte getState() {
		return state;
	}
	
	public void setState(byte state) {
		this.state = state;
	}

	public boolean getMode() {
		return mode;
	}

	public void setMode(boolean mode) {
		this.mode = mode;
	}

	public int getNextAvailableChannelId(){
		int result = -1;
		for(byte i=4; i<channels.length; i++){
			if(!isChannelUsed(i)){
				result = i;
				break;
			}
		}
		return result;
	}
	
	public boolean isChannelUsed(byte channelId){
		return (channels[channelId] != null);
	}

	public Channel getChannel(byte channelId){
		if(!isChannelUsed(channelId)) 
			channels[channelId] = new Channel(this, channelId);
		return channels[channelId];
	}
	
	public void closeChannel(byte channelId){
		channels[channelId] = null;
	}


	public Channel getLastReadChannel() {
		return lastReadChannel;
	}

	public void setLastReadChannel(Channel lastReadChannel) {
		this.lastReadChannel = lastReadChannel;
	}

	public Channel getLastWriteChannel() {
		return lastWriteChannel;
	}

	public void setLastWriteChannel(Channel lastWriteChannel) {
		this.lastWriteChannel = lastWriteChannel;
	}
	
	public void write(OutPacket packet){
		protocolSession.write(packet);
	}
	
	public void setParameters(Map params){
		this.params = params;
	}
	
	public int getClientTimer() {
		return clientUptime;
	}

	public void setClientTimer(int clientTimer) {
		this.clientUptime = clientTimer;
	}

	public Stream getStreamByChannelId(byte channelId){
		if(channelId < 4) return null;
		//log.debug("Channel id: "+channelId);
		int streamId = (int) Math.floor((channelId-4)/5);
		//log.debug("Stream: "+streamId);
		if(streams[streamId]==null) 
			streams[streamId] = createStream(streamId);
		return streams[streamId];
	}
	
	protected Stream createStream(int streamId){
		byte channelId = (byte) (streamId + 4);
		Stream stream = new Stream(this);
		final Channel data = getChannel(channelId++);
		final Channel video = getChannel(channelId++);
		final Channel audio = getChannel(channelId++);
		final Channel unknown = getChannel(channelId++);
		final Channel ctrl = getChannel(channelId++);
		final DownStreamSink down = new DownStreamSink(video,audio,data);
		stream.setDownstream(down);
		return stream;
	}
	
	public void ping(Ping ping){
		getChannel((byte)2).write(ping);
	}
	
}
