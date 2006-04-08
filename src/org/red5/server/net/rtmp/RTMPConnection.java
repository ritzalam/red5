package org.red5.server.net.rtmp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.BaseConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectCapableConnection;
import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.so.ScopeWrappingSharedObjectService;
import org.red5.server.stream.DownStreamSink;
import org.red5.server.stream.Stream;

public abstract class RTMPConnection extends BaseConnection 
	implements ISharedObjectCapableConnection {

	protected static Log log =
        LogFactory.getLog(RTMPConnection.class.getName());

	private final static int MAX_STREAMS = 12;
	
	//private Context context;
	private Channel[] channels = new Channel[64];
	private Stream[] streams = new Stream[MAX_STREAMS];
	protected ISharedObjectService sharedObjectService;
	protected HashMap<String,ISharedObject> sharedObjects;

	public RTMPConnection(String type) {
		// We start with an anonymous connection without a scope.
		// These parameters will be set during the call of "connect" later.
		//super(null, "");	temp fix to get things to compile
		super(type,null,null,null,null);
		sharedObjects = new HashMap();
	}
	
	public void setup(String host, String path, String sessionId, Map<String, String> params){
		this.host = host;
		this.path = path;
		this.sessionId = sessionId;
		this.params = params;
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

	
	/* Returns a stream for the next available stream id or null if all slots are in use. */
	public Stream createNewStream() {
		synchronized (streams) {
			for (int i=0; i<streams.length; i++)
				if (streams[i] == null) {
					Stream stream = createStream(i);
					streams[i] = stream;
					return stream;
				}
		}
		
		return null;
	}
	
	public Stream getStreamById(int id){
		if (id <= 0 || id > MAX_STREAMS-1)
			return null;
		
		return streams[id-1];
	}
	
	public Stream getStreamByChannelId(byte channelId){
		if (channelId < 4)
			return null;
		
		//log.debug("Channel id: "+channelId);
		int streamId = (int) Math.floor((channelId-4)/5);
		//log.debug("Stream: "+streamId);
		return streams[streamId];
	}
	
	public void close(){
		synchronized (streams) {
			for(int i=0; i<streams.length; i++){
				Stream stream = streams[i];
				if(stream != null) {
					stream.close();
					streams[i] = null;
				}
			}
		}
		super.close();
	}
	
	protected Stream createStream(int streamId){
		byte channelId = (byte) (4 + (streamId * 5));
		Stream stream = new Stream(this);
		stream.setStreamId(streamId+1);
		final Channel data = getChannel(channelId++);
		final Channel video = getChannel(channelId++);
		final Channel audio = getChannel(channelId++);
		//final Channel unknown = getChannel(channelId++);
		//final Channel ctrl = getChannel(channelId++);
		final DownStreamSink down = new DownStreamSink(video,audio,data);
		stream.setDownstream(down);
		return stream;
	}
	
	public void deleteStreamById(int streamId) {
		if (streamId >= 0 && streamId < MAX_STREAMS-1)
			streams[streamId-1] = null;
	}
	
	public void ping(Ping ping){
		getChannel((byte)2).write(ping);
	}
	
	public abstract void rawWrite(ByteBuffer out);
	public abstract void write(OutPacket out);

	public boolean connectSharedObject(String name, boolean persistent) {
		if(!sharedObjectService.hasSharedObject(name)){
			if(!sharedObjectService.createSharedObject(name, persistent)){
				return false;
			}
		}
		final ISharedObject so = sharedObjectService.getSharedObject(name);
		so.addEventListener(this);
		sharedObjects.put(name, so);
		return true;
	}

	public void disconnectSharedObject(String name) {
		sharedObjects.get(name).removeEventListener(this);
	}

	public ISharedObject getConnectedSharedObject(String name) {
		return sharedObjects.get(name);
	}

	public Iterator<String> getConnectedSharedObjectNames() {
		return sharedObjects.keySet().iterator();
	}
	
	public boolean isConnectedToSharedObject(String name) {
		return sharedObjects.containsKey(name);
	}

	@Override
	public boolean connect(IScope newScope) {
		if(super.connect(newScope)){
			sharedObjectService = new ScopeWrappingSharedObjectService(newScope);
			return true;
		} else {
			return false;
		}
	}
	
}
