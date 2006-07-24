package org.red5.server.stream;

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

import org.red5.server.stream.message.RTMPMessage;

public interface IStreamFlow {

	public int getMaxTimeBuffer();

	public void setMaxTimeBuffer(int maxTimeBuffer);

	public int getMinTimeBuffer();

	public void setMinTimeBuffer(int minTimeBuffer);

	public long getClientTimeBuffer();
	
	public void setClientTimeBuffer(long clientTimeBuffer);
	
	public int getDataBitRate();

	public int getSegmentBytesTransfered();

	public int getSegmentDataTime();

	public long getSegmentStreamTime();

	public int getStreamBitRate();

	public boolean isBufferTimeIncreasing();

	public long getTotalBytesTransfered();

	public long getTotalDataTime();

	public long getTotalStreamTime();
	
	public int getBufferTime();

	public void reset();
	
	public void pause();
	
	public void resume();
	
	public void clear();
	
	public void update(RTMPMessage msg);

	public long getZeroToStreamTime();
	
}