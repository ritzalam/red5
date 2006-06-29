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
