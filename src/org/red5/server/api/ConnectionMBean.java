package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

/**
 * The connection object.
 *
 * Each connection has an associated client and scope. Connections may be
 * persistent, polling, or transient. The aim of this interface is to provide
 * basic connection methods shared between different types of connections
 *
 * Future subclasses: RTMPConnection, RemotingConnection, AJAXConnection,
 * HttpConnection, etc
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public interface ConnectionMBean {

	public String getType();

	public void initialize(IClient client);

	public boolean connect(IScope scope);

	public boolean connect(IScope scope, Object[] params);

	public boolean isConnected();

	public void close();

	public Map<String, Object> getConnectParams();

	public IClient getClient();

	public String getHost();

	public String getRemoteAddress();

	public List<String> getRemoteAddresses();

	public int getRemotePort();

	public String getPath();

	public String getSessionId();

	public long getReadBytes();

	public long getWrittenBytes();

	public long getReadMessages();

	public long getWrittenMessages();

	public long getDroppedMessages();

	public long getPendingMessages();

	public void ping();

	public int getLastPingTime();

	public IScope getScope();

	public Iterator<IBasicScope> getBasicScopes();

}
