package org.red5.server.api;

/**
 * Extends stream to add methods for broadcast streaming
 * 
 * TODO: What should be pass to the methods?
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface BroadcastStream extends Stream {

	/**
	 * Subscribe to this stream
	 */
	public void subscribe();

	/**
	 * Unsubscire to this stream
	 */
	public void unsubscirbe();

}