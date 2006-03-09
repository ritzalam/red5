package org.red5.server.net.rtmp;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.context.AppContext;
import org.red5.server.context.Client;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.stream.DownStreamSink;
import org.red5.server.stream.Stream;

public abstract class BaseConnection extends Client {

	protected static Log log =
        LogFactory.getLog(BaseConnection.class.getName());

	//private Context context;
	private Channel[] channels = new Channel[64];
	private Stream[] streams = new Stream[12];
	private AppContext appCtx = null;
	
	public AppContext getAppContext() {
		return appCtx;
	}
	
	public void setAppContext(AppContext appCtx) {
		this.appCtx = appCtx;
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

	// This method must be overwritten by the connection implementation
	public abstract void write(OutPacket packet);
	
	// This method must be overwritten by the connection implementation
	public abstract void write(ByteBuffer packet);
	
	public void setParameters(Map params){
		this.params = params;
	}
	
	public Stream getStreamById(int id){
		return streams[id-1];
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
	
	public void close(){
		for(int i=0; i<streams.length; i++){
			Stream stream = streams[i];
			if(stream!=null) stream.close();
		}
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
