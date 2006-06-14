package org.red5.server.api.stream;

import org.red5.server.messaging.IMessageInput;

public interface IPlayItem {
	/**
	 * Get name of item.
	 * The VOD or Live stream provider is found according
	 * to this name.
	 * @return
	 */
	String getName();
	
	/**
	 * Start time in millisecond.
	 * @return
	 */
	long getStart();
	
	/**
	 * Play length in millisecond.
	 * @return
	 */
	long getLength();
	
	/**
	 * Get a message input for play.
	 * This object overrides the default algorithm for finding
	 * the appropriate VOD or Live stream provider according to
	 * the item name.
	 * @return
	 */
	IMessageInput getMessageInput();
}
