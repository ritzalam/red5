package org.red5.server.stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamAware;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Notify;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.net.rtmp.message.Status;
import org.red5.server.net.rtmp.message.StreamBytesRead;
import org.red5.server.net.rtmp.message.VideoData;

public class Stream extends BaseStream implements Constants, IStream, IEventDispatcher {
	
	public static final String MODE_READ = "read";
	public static final String MODE_RECORD = "record";
	public static final String MODE_APPEND = "append";
	public static final String MODE_LIVE = "live";
	
	protected static Log log =
        LogFactory.getLog(Stream.class.getName());
	
	private int writeQueue = 0;
	
	private long startTime = 0;
	private long startTS = 0;
	private long currentTS = 0;
	private int playLength = -1;
	private String name = "";
	private boolean paused = false;
	private String mode = MODE_READ;
	private int video_ts = 0;
	private int audio_ts = 0;
	private int metadata_ts = 0;
	private int bytesReadPacketCount = 0;
	
	private OutputStream downstream = null;
	private IStreamSource source = null;
	private VideoCodecFactory videoCodecFactory = null;
	
	private boolean initialMessage = true;
	
	private RTMPConnection conn;
	
	public Stream(IScope scope, RTMPConnection conn){
		super(scope);
		this.conn = conn;
	}
	
	public Stream(IScope scope, RTMPConnection conn, String type) {
		super(scope);
		this.conn = conn;
		this.mode = type;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public OutputStream getDownstream() {
		return downstream;
	}

	public void setDownstream(OutputStream downstream) {
		this.downstream = downstream;
	}

	public IStreamSource getSource() {
		return source;
	}

	public void setSource(IStreamSource source) {
		this.source = source;
	}

	public void setVideoCodecFactory(VideoCodecFactory factory) {
		this.videoCodecFactory = factory;
	}

	protected int bytesReadInterval = 125000;
	protected int bytesRead = 0;
	
	public void publish(){
		Status publish = new Status(Status.NS_PUBLISH_START);
		publish.setClientid(streamId);
		publish.setDetails(name);
		conn.getChannel((byte) 3).sendStatus(publish);
		
		initialMessage = true;
	}
	
	public void pause(){
		if (paused) return;
		paused = true;
		Status pause  = new Status("NetStream.Pause.Notify");
		pause.setClientid(streamId);
		pause.setDetails(name);
		conn.getChannel((byte) 3).sendStatus(pause);
	}
	
	public boolean isPaused() {
		return this.paused;
	}
	
	public void resume(int resumeTS){
		if (!paused) return;
		paused = false;
		if (!(source instanceof ISeekableStreamSource)) return;
		ISeekableStreamSource sss = (ISeekableStreamSource) source;
		int ts = sss.seek(resumeTS);
		
		Ping ping = new Ping();
		ping.setValue1((short) 4);
		ping.setValue2(streamId);
		
		conn.ping(ping);
		
		Ping ping2 = new Ping();
		ping2.setValue1((short) 0);
		ping2.setValue2(streamId);
		
		conn.ping(ping2);
		
		Status play  = new Status("NetStream.Unpause.Notify");
		play.setClientid(streamId);
		play.setDetails(name);
		conn.getChannel((byte) 3).sendStatus(play);
		
		AudioData blankAudio = new AudioData();
		blankAudio.setTimestamp(ts);
		dispatchEvent(blankAudio);
		
		if(source != null && source.hasMore()){
			dispatchEvent(source.dequeue());
		}
	}
	
	public void seek(int time) {
		if (!(source instanceof ISeekableStreamSource)) return;
		ISeekableStreamSource sss = (ISeekableStreamSource) source;
		int ts = sss.seek(time);
		
		if (!paused) {
			// seems not necessary, but seen in FMS's dump
			Ping ping0 = new Ping();
			ping0.setValue1((short) 1);
			ping0.setValue2(streamId);
			conn.ping(ping0);
		}
		
		Ping ping1 = new Ping();
		ping1.setValue1((short) 4);
		ping1.setValue2(streamId);
		
		conn.ping(ping1);
		
		Ping ping2 = new Ping();
		ping2.setValue1((short) 0);
		ping2.setValue2(streamId);
		
		conn.ping(ping2);
		
		Status play  = new Status("NetStream.Seek.Notify");
		play.setClientid(streamId);
		play.setDetails(name);
		conn.getChannel((byte) 3).sendStatus(play);
		
		AudioData blankAudio = new AudioData();
		blankAudio.setTimestamp(ts);
		dispatchEvent(blankAudio);
		
		if (!paused) {
			if (source !=null && source.hasMore()){
				dispatchEvent(source.dequeue());
			}
		}
	}
	
	public void start(int startTS, int length) {
		startTime = System.currentTimeMillis();
		playLength = length;
		
		if (startTS > 0 && source instanceof ISeekableStreamSource) {
			((ISeekableStreamSource) source).seek(startTS);
		}
		
		Status reset = new Status(Status.NS_PLAY_RESET);
		Status start = new Status(Status.NS_PLAY_START);
		reset.setClientid(streamId);
		start.setClientid(streamId);
		reset.setDetails(name);
		start.setDetails(name);
		
		// This hack fixes the on meta data problem
		// TODO: Perhaps its a good idea to init each channel with a blank audio packet
		AudioData blankAudio = new AudioData();
		downstream.getData().write(blankAudio);
				
		downstream.getData().sendStatus(reset);
		downstream.getVideo().sendStatus(start);
		
		initialMessage = true;
	}
	
	public void start(){
		start(0, -1);
	}
	
	public void stop(){
		
	}

	public long getBufferLength(){
		final long now = System.currentTimeMillis();
		final long time = now - startTime;
		final long sentTS = currentTS - startTS;
		return time - sentTS;
	}
	
	public boolean canAccept(){
		return downstream.canAccept();
	}
	
	public void dispatchEvent(Object obj) {
		if (!(obj instanceof Message))
			return;
		
		final Message message = (Message) obj;		
		if (downstream.canAccept()){
			if (initialMessage) {
				initialMessage = false;
				startTS = message.getTimestamp();
				if (this.videoCodec != null) {
					ByteBuffer keyframe = this.videoCodec.getKeyframe();
					if (keyframe != null) {
						// Send initial keyframe to client
						Message msg = new VideoData();
						msg.setTimestamp(message.getTimestamp()-1);
						msg.setData(keyframe);
						msg.setSealed(true);
						dispatchEvent(msg);
					}
				}
			}
			currentTS = message.getTimestamp();
			if (playLength >= 0 && currentTS - startTS > playLength) return;
			
			if(log.isDebugEnabled())
				log.debug("Sending downstream: " + message.getTimestamp());
			//writeQueue++;
			
			position = message.getTimestamp();
			downstream.dispatchEvent(message);
		}
	}
	
	public void dispatchEvent(IEvent event) {
		if ((event.getType() != IEvent.Type.STREAM_CONTROL) &&
			(event.getType() != IEvent.Type.STREAM_DATA))
			return;
		
		dispatchEvent(event.getObject());
	}
	
	protected boolean calcTS = true;
	protected boolean useServerTS = true;
	
		
	public void publish(Message message){
		ByteBuffer data = message.getData();
		if (this.initialMessage && (message instanceof VideoData)) {
			this.initialMessage = false;
			startTime = System.currentTimeMillis();
			if (this.videoCodecFactory != null) {
				this.videoCodec = this.videoCodecFactory.getVideoCodec(data);
				setVideoCodec(this.videoCodec);
			}
			// XXX: HACK HACK HACK, just for 0.4
			//log.warn("scope: "+scope);
			if(scope.getHandler() instanceof IStreamAware){
				//log.warn("publish start"+name);
				((IStreamAware) scope.getHandler()).streamBroadcastStart(name);
			}
		}
		
		long runTime = System.currentTimeMillis() - startTime;
		
		if (this.videoCodec != null)
			this.videoCodec.addData(data);
		
		if(calcTS){
			if (message instanceof AudioData) {
				audio_ts += message.getTimestamp();
				if(useServerTS) message.setTimestamp((int)runTime);
				else message.setTimestamp(audio_ts);
			} else if (message instanceof VideoData) {
				video_ts += message.getTimestamp();
				if(useServerTS) message.setTimestamp((int)runTime);
				else message.setTimestamp(video_ts);
			} else if (message instanceof Notify) {
				metadata_ts += message.getTimestamp();
				if(useServerTS) message.setTimestamp((int)runTime);
				else message.setTimestamp(metadata_ts);
			}
		}
		
		long lag = video_ts - runTime;
		
		// XXX: not sure my lag calc works, but runtimeTS seems to improve lag a little
		//log.info("lag: "+lag);
		//log.info("v:"+video_ts+" a:"+audio_ts+" lag:"+lag); 
		
		bytesRead += message.getData().limit();
		if(bytesReadPacketCount < Math.floor(bytesRead / bytesReadInterval)){
			bytesReadPacketCount++;
			StreamBytesRead streamBytesRead = new StreamBytesRead();
			streamBytesRead.setBytesRead(bytesRead);
			log.debug(streamBytesRead);
			conn.getChannel((byte)2).write(streamBytesRead);
		}
		dispatchEvent(message);
		message.release();
	}
	
	public void written(Message message){
		if(paused) return;
		writeQueue--;
		synchronized (this) {
			if(source !=null && source.hasMore()){
				dispatchEvent(source.dequeue());
			}
		}
	}
	
	public void close(){
		if(downstream!=null) downstream.close();
		if(source!=null) source.close();
		super.close();
	}
	
}
