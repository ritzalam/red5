package org.red5.server.api.stream;

/**
 * This interface represents the stream methods that can be called throug RTMP.
 */
public interface IStreamService {

	public static final String STREAM_SERVICE = "streamService";
	
	/**
	 * Create a stream and return a corresponding id.
	 * @return
	 */
	public int createStream();
	
	/**
	 * Close the stream but not deallocate the resources.
	 */
	public void closeStream();
	
	/**
	 * Close the stream if not been closed.
	 * Deallocate the related resources.
	 * @param number
	 */
	public void deleteStream(int streamId);
	
	public void deleteStream(IStreamCapableConnection conn, int streamId);
	
	public void play(String name);
	
	public void play(String name, int start);
	
	public void play(String name, int start, int length);
	
	public void play(String name, int start, int length, boolean flushPlaylist);
	
	public void publish(String name);
	
	public void publish(String name, String mode);
	
	public void publish(boolean dontStop);
	
	public void seek(int position);
	
	public void pause(boolean pausePlayback, int position);
}
