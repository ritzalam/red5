package org.red5.server.api.stream;

import org.red5.server.api.IScope;

/**
 * This interface represents the stream methods that can be called throug RTMP.
 */
public interface IStreamService {

	public static final String STREAM_SERVICE = "streamService";
	
	public int createStream();
	
	public void closeStream();
	
	public void deleteStream(int number);
		
	public void deleteStream(IStreamCapableConnection conn, int number);
	
	public void play(String name);
	
	public void play(String name, Double type);
	
	public void play(String name, Double type, int length);
	
	public void play(String name, Double type, int length, boolean flushPlaylist);
	
	public void publish(String name);
	
	public void publish(String name, String mode);
	
	public void publish(boolean dontStop);
}
