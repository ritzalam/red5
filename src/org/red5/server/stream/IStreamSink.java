package org.red5.server.stream;

import org.red5.server.net.rtmp.message.Message;

public interface IStreamSink {

	public abstract boolean canAccept();
	
	// push down stream
	public abstract void enqueue(Message message);
	
	public abstract void close();
	
	public abstract void setVideoCodec(IVideoStreamCodec codec);
	
}
