package org.red5.server.api;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 *
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import org.red5.server.BaseConnection;
import org.red5.server.Scope;

public class TestConnection extends BaseConnection {

	public TestConnection(String host, String path, String sessionId) {
		super(PERSISTENT, host, null, 0, path, sessionId, null);
	}

	/**
	 * Return encoding (currently AMF0)
	 * @return          AMF0 encoding constant
	 */
	public Encoding getEncoding() {
		return Encoding.AMF0;
	}

	/** {@inheritDoc} */
	public int getLastPingTime() {
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public long getReadBytes() {
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public long getWrittenBytes() {
		return 0;
	}

	/** {@inheritDoc} */
	public void ping() {

	}

	public void setClient(IClient client) {
		this.client = client;
	}

	public void setScope(Scope scope) {
		this.scope = scope;
	}

	public void setContext(IContext context) {
		this.scope.setContext(context);
	}

}
