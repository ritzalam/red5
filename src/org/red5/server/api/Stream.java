package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright © 2006 by respective authors (see below). All rights reserved.
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
 * Base interface for stream objects
 * 
 * Provides all the common methods shared between OnDemandStream and
 * BroadcastStream
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
