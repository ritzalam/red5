package org.red5.server.stream;

import org.red5.server.net.rtmp.message.Message;

public interface IStream {

	// start pull downstream
	public abstract void start();

	// stop pull downsteam
	public abstract void stop();
	
	// pull down stream, this is where we pause
	public abstract void written(Message message);
	
	// push up stream
	public abstract void publish(Message message);	
	
}
