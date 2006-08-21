package org.red5.server.api.stream;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

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
	
	public void play(Boolean dontStop);
	
	public void play(String name);
	
	public void play(String name, int start);
	
	public void play(String name, int start, int length);
	
	public void play(String name, int start, int length, boolean flushPlaylist);
	
	public void publish(String name);
	
	public void publish(String name, String mode);
	
	public void publish(Boolean dontStop);
	
	public void seek(int position);
	
	public void pause(boolean pausePlayback, int position);
	
	public void receiveVideo(boolean receive);
	
	public void receiveAudio(boolean receive);
	
}
