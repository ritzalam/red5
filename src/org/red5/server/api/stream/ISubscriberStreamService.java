package org.red5.server.api.stream;

import org.red5.server.api.IScope;

public interface ISubscriberStreamService {

	public final static String SUBSCRIBER_STREAM_SERVICE = "subscriberStreamService";
	
	/**
	 * Returns a stream that can subscribe a broadcast stream with the given name
	 * using "IBroadcastStream.subscribe".
	 *  
	 * @param scope
	 * 			the scope to return the stream from
	 * @param name
	 * 			the name of the stream
	 * @return the stream object 
	 */
	public ISubscriberStream getSubscriberStream(IScope scope, String name);
	
}
