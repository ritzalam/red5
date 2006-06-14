package org.red5.server.api.stream;

public interface ISubscriberStream extends IClientStream {
	/**
	 * Start playing.
	 */
	void play();
	
	/**
	 * Pause at a position for current playing item.
	 * @param position Position for pause in millisecond.
	 */
	void pause(int position);
	
	/**
	 * Resume from a position for current playing item.
	 * @param position Position for resume in millisecond.
	 */
	void resume(int position);
	
	/**
	 * Stop playing.
	 */
	void stop();
	
	/**
	 * Seek into a position for current playing item.
	 * @param position Position for seek in millisecond.
	 */
	void seek(int position);
}
