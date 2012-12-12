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
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.scope.Scope;
import org.red5.server.service.PendingCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConnection extends BaseConnection implements IServiceCapableConnection {

	private static final Logger log = LoggerFactory.getLogger(TestConnection.class);
	
	public TestConnection(String host, String path, String sessionId) {
		super(PERSISTENT, host, null, 0, path, sessionId, null);
	}

	public TestConnection(String host, String path, String sessionId, String remoteAddress) {
		super(PERSISTENT, host, remoteAddress, 0, path, sessionId, null);
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
	
	public void echo(Object[] params) {
		log.debug("Client #{} - echo: {}", client.getId(), params[0]);
	}

	@Override
	public void invoke(IServiceCall call) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invoke(IServiceCall call, int channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invoke(String method) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invoke(String method, IPendingServiceCallback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invoke(String name, Object[] params) {
		if ("echo".equals(name)) {
			echo(params);
		}
//		try {
//			Method method = this.getClass().getMethod(name, Object[].class);
//			method.invoke(this, params);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}		
	}

	@Override
	public void invoke(String name, Object[] params, IPendingServiceCallback callback) {
		if ("echo".equals(name)) {
			echo(params);
		}		
		IPendingServiceCall call = new PendingCall(null, name, params);
		call.setResult(Boolean.TRUE);
		callback.resultReceived(call);
	}

	@Override
	public void notify(IServiceCall call) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notify(IServiceCall call, int channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notify(String method) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notify(String method, Object[] params) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void status(Status status) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void status(Status status, int channel) {
		// TODO Auto-generated method stub
		
	}
	
}
