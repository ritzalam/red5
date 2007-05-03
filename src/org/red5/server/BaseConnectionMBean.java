package org.red5.server;

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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IScope;

/**
 * Base abstract class for connections. Adds connection specific functionality like work with clients
 * to AttributeStore.
 */
public interface BaseConnectionMBean {

	/**
	 *
	 * @return
	 */
	public String getType();

	/**
	 *
	 * @return
	 */
	public String getHost();

	/**
	 *
	 * @return
	 */
	public String getRemoteAddress();

	/**
	 *
	 * @return
	 */
	public List<String> getRemoteAddresses();

	/**
	 *
	 * @return
	 */
	public int getRemotePort();

	/**
	 *
	 * @return
	 */
	public String getPath();

	/**
	 *
	 * @return
	 */
	public String getSessionId();

	/**
	 * Return connection parameters
	 * @return
	 */
	public Map<String, Object> getConnectParams();

	/**
	 *
	 * @return
	 */
	public IClient getClient();

	/**
	 * Check whether connection is alive
	 * @return       true if connection is bound to scope, false otherwise
	 */
	public boolean isConnected();

	/**
	 *
	 * @return
	 */
	public IScope getScope();

	/**
	 *  Closes connection
	 */
	public void close();

	/**
	 *
	 * @return
	 */
	public Iterator<IBasicScope> getBasicScopes();

	/**
	 *
	 * @return
	 */
	public long getReadBytes();

	/**
	 *
	 * @return
	 */
	public long getWrittenBytes();

	/**
	 *
	 * @return
	 */
	public long getReadMessages();

	/**
	 *
	 * @return
	 */
	public long getWrittenMessages();

	/**
	 *
	 * @return
	 */
	public long getDroppedMessages();

	/**
	 *
	 * @return
	 */
	public long getPendingMessages();

	/**
	 *
	 * @param streamId
	 * @return
	 */
	public long getPendingVideoMessages(int streamId);

}
