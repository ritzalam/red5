package org.red5.server.stream;

import org.red5.server.net.rtmp.message.Message;

public interface IStreamSource {

	public abstract boolean hasMore();
	
	public abstract Message dequeue();
	
	public abstract void close();
	
}
