package org.red5.server.api.stream;

/**
 * This interface represents the stream methods that can be called throug RTMP.
 */
public interface IStreamService {

	public int createStream();
	
	public void play(String name);
	
	public void play(String name, Double type);
	
	public void play(String name, Double type, int length);
	
	public void play(String name, Double type, int length, boolean flushPlaylist);
	
	public void publish(String name, String mode);
}
