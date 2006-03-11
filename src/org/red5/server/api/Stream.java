package org.red5.server.api;

/**
 * Base interface for stream objects
 * 
 * Provides all the common methods shared between OnDemandStream and BroadcastStream
 */
public abstract interface Stream {

	/**
	 * Get the current position in seconds
	 * 
	 * @return current position in seconds
	 */
	public int getCurrentPosition();

	/**
	 * Check if the stream has audio 
	 * 
	 * @return true if there is an audio channel
	 */
	public boolean hasAudio();

	/** 
	 * Check if the stream has video
	 * 
	 * @return true if there is a video channel
	 */
	public boolean hasVideo();

	/**
	 * Get the name of the current video codec
	 * 
	 * @return the name of the coded, eg: vp6
	 */
	public String getVideoCodecName();

	/**
	 * Get the name of the current audio coded
	 * 
	 * @return the name of the codec, eg: mp3
	 */
	public String getAudioCodecName();

	/**
	 * Get the scope this stream is associated with
	 * 
	 * @return scope object
	 */
	public Scope getScope();

	/**
	 * Close this stream, this will disconnect all clients
	 */
	public void close();

}
