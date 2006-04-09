package org.red5.server.api.stream;

public interface ISubscriberStreamService {

	/**
	 * Returns a stream that can subscribe a broadcast stream with the given name
	 * using "IBroadcastStream.subscribe".
	 *  
	 * @param name
	 * 			the name of the stream
	 * @return the stream object 
	 */
	public ISubscriberStream getSubscriberStream(String name);
	
}
