package org.red5.server.stream;

import java.io.FileOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.Connection;
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Status;
import org.red5.server.net.rtmp.message.StreamBytesRead;
import org.red5.server.net.rtmp.message.VideoData;

public class Stream extends BaseStreamSink implements Constants, IStream, IStreamSink {
	
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
	private String name = "";
	private boolean paused = false;
	private String mode = MODE_READ;
	
	private DownStreamSink downstream = null;
	private IStreamSink upstream = null;
	private IStreamSource source = null;
	private VideoCodecFactory videoCodecFactory = null;
	
	private int streamId = 0;
	private boolean initialMessage = true;
	
	private Connection conn;
	
	public Stream(Connection conn){
		this.conn = conn;
	}
	
	public Stream(Connection conn, String type) {
		this.conn = conn;
		this.mode = type;
	}
	
	public int getStreamId() {
		return streamId;
	}
	
	public void setStreamId(int streamId) {
		this.streamId = streamId;
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

	public DownStreamSink getDownstream() {
		return downstream;
	}

	public void setDownstream(DownStreamSink downstream) {
		this.downstream = downstream;
	}

	public IStreamSource getSource() {
		return source;
	}

	public void setSource(IStreamSource source) {
		this.source = source;
	}

	public IStreamSink getUpstream() {
		return upstream;
	}

	public void setUpstream(IStreamSink upstream) {
		this.upstream = upstream;
	}

	public void setVideoCodecFactory(VideoCodecFactory factory) {
		this.videoCodecFactory = factory;
	}

	protected int bytesReadInterval = 125000;
	protected int bytesRead = 0;
	
	public void publish(){
		Status publish = new Status(Status.NS_PUBLISH_START);
		publish.setClientid(1);
		publish.setDetails(name);
		Channel data = downstream.getData();
		if (data != null)
			// temporary streams don't have a data channel so check for it
			data.sendStatus(publish);
		
		initialMessage = true;
	}
	
	public void pause(){
		paused = true;
		//Status pause  = new Status(Status.NS_PLAY_STOP);
		//pause.setClientid(1);
		//pause.setDetails(name);
		//downstream.getData().sendStatus(pause);
	}
	
	public void resume(){
		paused = false;
		Status play  = new Status(Status.NS_PLAY_RESET);
		play.setClientid(1);
		play.setDetails(name);
		downstream.getData().sendStatus(play);
		
		if(source !=null && source.hasMore()){
			write(source.dequeue());
		}
	}
	
	public void start(){
		startTime = System.currentTimeMillis();
		
		Status reset = new Status(Status.NS_PLAY_RESET);
		Status start = new Status(Status.NS_PLAY_START);
		reset.setClientid(1);
		start.setClientid(1);
		reset.setDetails(name);
		start.setDetails(name);
		
		// This hack fixes the on meta data problem
		// TODO: Perhaps its a good idea to init each channel with a blank audio packet
		AudioData blankAudio = new AudioData();
		downstream.getData().write(blankAudio);
				
		downstream.getData().sendStatus(reset);
		downstream.getVideo().sendStatus(start);
		//downstream.getVideo().sendStatus(new Status(Status.NS_DATA_START));
		
		/*
		if(source!=null && source.hasMore()){
			write(source.dequeue());
		}
		*/
		
		if (this.videoCodec != null) {
			ByteBuffer keyframe = this.videoCodec.getKeyframe();
			if (keyframe != null) {
				// Send initial keyframe to client
				Message msg = new VideoData();
				msg.setTimestamp(0);
				msg.setData(keyframe);
				msg.setSealed(true);
				this.write(msg);
			}
		}
		
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
	
	public void enqueue(Message message){ 
		write(message);
	}
	
	protected void write(Message message){
		if (downstream.canAccept()){
			if(log.isDebugEnabled())
				log.debug("Sending downstream");
			//writeQueue++;
			currentTS = message.getTimestamp();
			downstream.enqueue(message);
		}
	}
	
	private int ts = 0;
	private int bytesReadPacketCount = 0;
	
	public void publish(Message message){
		ByteBuffer data = message.getData();
		if (this.initialMessage) {
			this.initialMessage = false;
			
			if (this.videoCodecFactory != null) {
				this.videoCodec = this.videoCodecFactory.getVideoCodec(data);
				if (this.upstream != null)
					this.upstream.setVideoCodec(this.videoCodec);
			}
		}
		
		if (this.videoCodec != null)
			this.videoCodec.addData(data);
		
		ts += message.getTimestamp();
		bytesRead += message.getData().limit();
		if(bytesReadPacketCount < Math.floor(bytesRead / bytesReadInterval)){
			bytesReadPacketCount++;
			StreamBytesRead streamBytesRead = new StreamBytesRead();
			streamBytesRead.setBytesRead(bytesRead);
			log.debug(streamBytesRead);
			conn.getChannel((byte)2).write(streamBytesRead);
		}
		message.setTimestamp(ts);
		if(upstream != null && upstream.canAccept()){
			upstream.enqueue(message);
		} else {
			log.warn("No where for upstream packet to go :(");
		}
	}
	
	public void written(Message message){
		if(paused) return;
		writeQueue--;
		if(source !=null && source.hasMore()){
			write(source.dequeue());
		}
	}
	
	public void close(){
		if(upstream!=null) upstream.close();
		if(downstream!=null) downstream.close();
		if(source!=null) source.close();
		super.close();
	}
	
}
