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

import org.red5.server.api.IConnection;

/**
 * Standard stream for play as a list
 
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface ISubscriberStreamNew extends IStream {
	// capability mask
	public static final int NO_CAP = 0;
	public static final int PAUSABLE = 1;
	public static final int SEEKABLE = 2;
	public static final int STOPPABLE = 4;
	// status
	public static final int INVALID = 0;
	public static final int INIT = 1;
	public static final int PAUSED = 2;
	public static final int PLAYING = 3;
	public static final int STOPPED = 4;
	
	int getCapability();
	void seek(int position);
	void pause(int position);
	void resume(int position);
	void stop();
	int getStatus();
	IConnection getConnection();
}
