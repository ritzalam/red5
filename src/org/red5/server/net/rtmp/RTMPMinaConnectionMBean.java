package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import java.util.List;
import java.util.Map;

/**
 * Base abstract class for connections. Adds connection specific functionality like work with clients
 * to AttributeStore.
 */
public interface RTMPMinaConnectionMBean {

	public String getType();

	public String getHost();

	public String getRemoteAddress();

	public List<String> getRemoteAddresses();

	public int getRemotePort();

	public String getPath();

	public String getSessionId();

	public Map<String, Object> getConnectParams();

	public boolean isConnected();

	public void close();

	public long getReadBytes();

	public long getWrittenBytes();

	public long getReadMessages();

	public long getWrittenMessages();

	public long getDroppedMessages();

	public long getPendingMessages();

	public long getPendingVideoMessages(int streamId);
	
    public void invokeMethod(String method);

}
