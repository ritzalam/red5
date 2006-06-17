package org.red5.server.api.stream;

import java.util.List;
import org.red5.server.api.IScope;

public interface IBroadcastStreamService {

	public final static String BROADCAST_STREAM_SERVICE = "broadcastStreamService";
	
	/**
	 * Does the scope have a broadcast stream registered with a given name
	 * 
	 * @param scope
	 * 			  the scope to check for the stream
	 * @param name
	 *            name of the broadcast
	 * @return true is a stream exists, otherwise false
	 */
	public boolean hasBroadcastStream(IScope scope, String name);

	/**
	 * Get a broadcast stream by name
	 * 
	 * @param scope
	 * 			  the scope to return the stream from
	 * @param name
	 *            the name of the broadcast
	 * @return broadcast stream object
	 */
	public IBroadcastStream getBroadcastStream(IScope scope, String name);

	/**
	 * Get a set containing the names of all the broadcasts
	 * 
	 * @param scope
	 * 			  the scope to search for streams
	 * @return set containing all broadcast names
	 */
	public List<String> getBroadcastStreamNames(IScope scope);
	
	
}
