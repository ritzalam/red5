package org.red5.server.net.servlet;

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.red5.server.AttributeStore;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;

/**
 * Simple connection class so the Red5 object works in methods invoked through
 * remoting.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ServletConnection extends AttributeStore implements IConnection {

	protected IScope scope;

	protected HttpServletRequest request;

	public ServletConnection(HttpServletRequest request, IScope scope) {
		this.request = request;
		this.scope = scope;
	}

	private void notSupported() {
		throw new RuntimeException("not supported for this type of connection");
	}

	public String getType() {
		return IConnection.TRANSIENT;
	}

	public void initialize(IClient client) {
		notSupported();
	}

	public boolean connect(IScope scope) {
		notSupported();
		return false;
	}

	public boolean connect(IScope scope, Object[] params) {
		notSupported();
		return false;
	}

	public boolean isConnected() {
		return false;
	}

	public void close() {
		// Nothing to do.
	}

	public Map<String, String> getConnectParams() {
		return new HashMap<String, String>();
	}

	public IClient getClient() {
		return null;
	}

	public String getHost() {
		return request.getLocalName();
	}

	public String getRemoteAddress() {
		return request.getRemoteAddr();
	}

	public int getRemotePort() {
		return request.getRemotePort();
	}

	public String getPath() {
		String path = request.getContextPath();
		if (request.getPathInfo() != null) {
			path += request.getPathInfo();
		}
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return path;
	}

	public String getSessionId() {
		return null;
	}

	public long getReadBytes() {
		return request.getContentLength();
	}

	public long getWrittenBytes() {
		return 0;
	}

	public long getPendingMessages() {
		return 0;
	}

	public long getPendingVideoMessages() {
		return 0;
	}

	public long getReadMessages() {
		return 1;
	}

	public long getWrittenMessages() {
		return 0;
	}

	public long getDroppedMessages() {
		return 0;
	}

	public void ping() {
		notSupported();
	}

	public int getLastPingTime() {
		return -1;
	}

	public IScope getScope() {
		return scope;
	}

	public Iterator<IBasicScope> getBasicScopes() {
		notSupported();
		return null;
	}

	public void dispatchEvent(Object event) {
		notSupported();
	}

	public void dispatchEvent(IEvent event) {
		notSupported();
	}

	public boolean handleEvent(IEvent event) {
		notSupported();
		return false;
	}

	public void notifyEvent(IEvent event) {
		notSupported();
	}

}
