package org.red5.server.api;

/**
 * Extends stream to add methods for on demand access
 */
public interface OnDemandStream extends Stream {
	
	/**
	 * Start playback
	 */
	public void play();

	/**
	 * Seek to the keyframe nearest to position
	 * @param position
	 * 	position in seconds
	 */
	public void seek(int position);

	/**
	 * Pause the stream
	 */
	public void pause();

	/**
	 * Resume a paused stream
	 */
	public void resume();

	/**
	 * Stop the stream, this resets the position to the start
	 */
	public void stop();

	/**
	 * Is the stream paused
	 * 
	 * @return true if the stream is paused
	 */
	public boolean isPaused();

	/**
	 * Is the stream stopped
	 * 
	 * @return true if the stream is stopped
	 */
	public boolean isStopped();
	
	/**
	 * Is the stream playing
	 * 
	 * @return true if the stream is playing
	 */
	public boolean isPlaying();
	
}