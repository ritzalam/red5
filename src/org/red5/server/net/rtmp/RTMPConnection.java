package org.red5.server.net.rtmp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.BaseConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectCapableConnection;
import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IBroadcastStreamService;
import org.red5.server.api.stream.IOnDemandStream;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.net.rtmp.message.Invoke;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.so.ScopeWrappingSharedObjectService;
import org.red5.server.stream.BroadcastStream;
import org.red5.server.stream.BroadcastStreamScope;
import org.red5.server.stream.VideoCodecFactory;
import org.red5.server.stream.OnDemandStream;
import org.red5.server.stream.OutputStream;
import org.red5.server.stream.ScopeWrappingStreamService;
import org.red5.server.stream.Stream;
import org.red5.server.stream.SubscriberStream;
import org.springframework.context.ApplicationContext;

public abstract class RTMPConnection extends BaseConnection 
	implements ISharedObjectCapableConnection, IStreamCapableConnection, IServiceCapableConnection {

	protected static Log log =
        LogFactory.getLog(RTMPConnection.class.getName());

	private final static int MAX_STREAMS = 12;
	private final static String VIDEO_CODEC_FACTORY = "videoCodecFactory";
	
	//private Context context;
	private Channel[] channels = new Channel[64];
	private IStream[] streams = new IStream[MAX_STREAMS];
	private boolean[] reservedStreams = new boolean[MAX_STREAMS];
	protected ISharedObjectService sharedObjectService;
	protected ScopeWrappingStreamService streamService;
	protected HashMap<String,ISharedObject> sharedObjects;

	public RTMPConnection(String type) {
		// We start with an anonymous connection without a scope.
		// These parameters will be set during the call of "connect" later.
		//super(null, "");	temp fix to get things to compile
		super(type,null,null,null,null);
		sharedObjects = new HashMap<String,ISharedObject>();
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
	
	public int reserveStreamId() {
		int result = -1;
		synchronized (reservedStreams) {
			for (int i=0; i<reservedStreams.length; i++) {
				if (!reservedStreams[i]) {
					reservedStreams[i] = true;
					result = i;
					break;
				}
			}
		}
		return result;
	}
	
	private OutputStream createOutputStream(int streamId) {
		byte channelId = (byte) (4 + (streamId * 5));
		final Channel data = getChannel(channelId++);
		final Channel video = getChannel(channelId++);
		final Channel audio = getChannel(channelId++);
		//final Channel unknown = getChannel(channelId++);
		//final Channel ctrl = getChannel(channelId++);
		return new OutputStream(this.getScope(), video, audio, data);
	}
	
	public VideoCodecFactory getVideoCodecFactory() {
		final IContext context = scope.getContext();
		ApplicationContext appCtx = context.getApplicationContext();
		if (!appCtx.containsBean(VIDEO_CODEC_FACTORY))
			return null;
	
		return (VideoCodecFactory) appCtx.getBean(VIDEO_CODEC_FACTORY);
	}
	
	public IBroadcastStream newBroadcastStream(String name, int streamId) {
		if (!reservedStreams[streamId])
			// StreamId has not been reserved before
			return null;
		
		if (streams[streamId] != null)
			// Another stream already exists with this id
			return null;
		
		final IBroadcastStream result = streamService.getBroadcastStream(name);
		if (result instanceof BroadcastStream) {
			((BroadcastStream) result).setStreamId(streamId+1);
			((BroadcastStream) result).setDownstream(createOutputStream(streamId));
			((BroadcastStream) result).setVideoCodecFactory(getVideoCodecFactory());
		} else if (result instanceof BroadcastStreamScope) {
			((BroadcastStreamScope) result).setStreamId(streamId+1);
			((BroadcastStreamScope) result).setDownstream(createOutputStream(streamId));
			((BroadcastStreamScope) result).setVideoCodecFactory(getVideoCodecFactory());
		} else
			log.error("Can't initialize broadcast stream.");
		streams[streamId] = result;
		return result;
	}
	
	public ISubscriberStream newSubscriberStream(String name, int streamId) {
		if (!reservedStreams[streamId])
			// StreamId has not been reserved before
			return null;
		
		if (streams[streamId] != null)
			// Another stream already exists with this id
			return null;
		
		SubscriberStream result = new SubscriberStream(getScope(), this);
		result.setStreamId(streamId+1);
		result.setDownstream(createOutputStream(streamId));
		final IBroadcastStream broadcast = streamService.getBroadcastStream(name);
		broadcast.subscribe(result);
		streams[streamId] = result;
		return result;
	}
	
	public IOnDemandStream newOnDemandStream(String name, int streamId) {
		if (!reservedStreams[streamId])
			// StreamId has not been reserved before
			return null;
		
		if (streams[streamId] != null)
			// Another stream already exists with this id
			return null;
	
		final IOnDemandStream stream = streamService.getOnDemandStream(name);
		if (stream instanceof OnDemandStream) {
			((OnDemandStream) stream).setStreamId(streamId+1);
			((OnDemandStream) stream).setDownstream(createOutputStream(streamId));
		}
		streams[streamId] = stream;
		return stream;
	}
	
	public IStream getStreamById(int id){
		if (id <= 0 || id > MAX_STREAMS-1)
			return null;
		
		return streams[id-1];
	}
	
	public IStream getStreamByChannelId(byte channelId){
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
				IStream stream = streams[i];
				if(stream != null) {
					streamService.deleteStream(this, stream.getStreamId());
					streams[i] = null;
				}
			}
		}
		super.close();
	}
	
	public void deleteStreamById(int streamId) {
		if (streamId >= 0 && streamId < MAX_STREAMS-1) {
			streams[streamId-1] = null;
			reservedStreams[streamId-1] = false;
		}
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
	public boolean connect(IScope newScope, Object[] params) {
		if(super.connect(newScope, params)){
			sharedObjectService = new ScopeWrappingSharedObjectService(newScope);
			streamService = new ScopeWrappingStreamService(newScope);
			return true;
		} else {
			return false;
		}
	}
	
	public IServiceCall invoke(IServiceCall call) {
		Invoke invoke = new Invoke();
		invoke.setCall(call);
		getChannel((byte) 3).write(invoke);
		return call;
	}
}
