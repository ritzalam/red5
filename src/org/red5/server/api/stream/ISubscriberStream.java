package org.red5.server.api.stream;

public interface ISubscriberStream extends IStream {

	public void start();
	
	/**
	 * 
	 * @param startTS start time in milliseconds
	 * @param length length to play in milliseconds (-1 for complete stream)
	 */
	public void start(int startTS, int length);
	
}
