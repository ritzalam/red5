/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.api;

import org.red5.server.BaseConnection;
import org.red5.server.scope.Scope;

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

	public void setBandwidth(int mbits) {
	}

}
