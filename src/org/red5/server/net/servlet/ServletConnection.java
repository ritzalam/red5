package org.red5.server.net.servlet;

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
    /**
     * Scope
     */
	protected IScope scope;
    /**
     * Servlet request
     */
	protected HttpServletRequest request;

    /**
     * Create servlet connection from request and scope
     * @param request           Servlet request
     * @param scope             Scope
     */
    public ServletConnection(HttpServletRequest request, IScope scope) {
		this.request = request;
		this.scope = scope;
	}

    /**
     * Throws Not supported runtime exception
     */
    private void notSupported() {
		throw new RuntimeException("not supported for this type of connection");
	}

    /**
     * Return encoding (AMF0 or AMF3)
     * @return        Encoding, currently AMF0
     */
    public Encoding getEncoding() {
		return Encoding.AMF0;
	}

    /** {@inheritDoc} */
    public String getType() {
		return IConnection.TRANSIENT;
	}

	/** {@inheritDoc} */
    public void initialize(IClient client) {
		notSupported();
	}

	/** {@inheritDoc} */
    public boolean connect(IScope scope) {
		notSupported();
		return false;
	}

	/** {@inheritDoc} */
    public boolean connect(IScope scope, Object[] params) {
		notSupported();
		return false;
	}

	/** {@inheritDoc} */
    public boolean isConnected() {
		return false;
	}

	/** {@inheritDoc} */
    public void close() {
		// Nothing to do.
	}

	public Map<String, Object> getConnectParams() {
		return new HashMap<String, Object>();
	}

	/** {@inheritDoc} */
    public IClient getClient() {
		return null;
	}

	/** {@inheritDoc} */
    public String getHost() {
		return request.getLocalName();
	}

	/** {@inheritDoc} */
    public String getRemoteAddress() {
		return request.getRemoteAddr();
	}

	/** {@inheritDoc} */
    public int getRemotePort() {
		return request.getRemotePort();
	}

	/** {@inheritDoc} */
    public String getPath() {
		String path = request.getContextPath();
		if (request.getPathInfo() != null) {
			path += request.getPathInfo();
		}
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		return path;
	}

	/** {@inheritDoc} */
    public String getSessionId() {
		return null;
	}

	/** {@inheritDoc} */
    public long getReadBytes() {
		return request.getContentLength();
	}

	/** {@inheritDoc} */
    public long getWrittenBytes() {
		return 0;
	}

	/** {@inheritDoc} */
    public long getPendingMessages() {
		return 0;
	}

	/**
     * Return pending video messages number.
     *
     * @return  Pending video messages number
     */
    public long getPendingVideoMessages() {
		return 0;
	}

	/** {@inheritDoc} */
    public long getReadMessages() {
		return 1;
	}

	/** {@inheritDoc} */
    public long getWrittenMessages() {
		return 0;
	}

	/** {@inheritDoc} */
    public long getDroppedMessages() {
		return 0;
	}

	/** {@inheritDoc} */
    public void ping() {
		notSupported();
	}

	/** {@inheritDoc} */
    public int getLastPingTime() {
		return -1;
	}

	/** {@inheritDoc} */
    public IScope getScope() {
		return scope;
	}

	/** {@inheritDoc} */
    public Iterator<IBasicScope> getBasicScopes() {
		notSupported();
		return null;
	}

	public void dispatchEvent(Object event) {
		notSupported();
	}

	/** {@inheritDoc} */
    public void dispatchEvent(IEvent event) {
		notSupported();
	}

	/** {@inheritDoc} */
    public boolean handleEvent(IEvent event) {
		notSupported();
		return false;
	}

	/** {@inheritDoc} */
    public void notifyEvent(IEvent event) {
		notSupported();
	}

}
