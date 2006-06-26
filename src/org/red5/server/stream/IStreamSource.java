package org.red5.server.stream;

import org.red5.server.api.event.IEvent;

public interface IStreamSource {

	public abstract boolean hasMore();
	
	public abstract IEvent dequeue();
	
	public abstract void close();
	
}
