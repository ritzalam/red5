package org.red5.server.api.stream;

public interface ISubscriberStream extends IStream {

	public void start();
	
	public void start(int startTS, int length);
	
}
