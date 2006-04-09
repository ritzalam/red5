package org.red5.server.api.stream;

import java.util.Iterator;

public interface IBroadcastStreamService {

	/**
	 * Does the scope have a broadcast stream registered with a given name
	 * 
	 * @param name
	 *            name of the broadcast
	 * @return true is a stream exists, otherwise false
	 */
	public boolean hasBroadcastStream(String name);

	/**
	 * Get a broadcast stream by name
	 * 
	 * @param name
	 *            the name of the broadcast
	 * @return broadcast stream object
	 */
	public IBroadcastStream getBroadcastStream(String name);

	/**
	 * Get a set containing the names of all the broadcasts
	 * 
	 * @return set containing all broadcast names
	 */
	public Iterator<String> getBroadcastStreamNames();
	
	
}
