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

package org.red5.server.net.rtmps;

import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.red5.server.net.rtmp.RTMPClient;
import org.red5.server.net.rtmp.RTMPClientConnManager;
import org.red5.server.net.rtmp.codec.RTMP;

/**
 * RTMPS client object
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Kevin Green (kevygreen@gmail.com)
 */
public class RTMPSClient extends RTMPClient {

	// I/O handler
	private final RTMPSMinaIoHandler ioHandler;
	
	/** Constructs a new RTMPClient. */
    public RTMPSClient() {
		ioHandler = new RTMPSMinaIoHandler();
		ioHandler.setCodecFactory(getCodecFactory());
		ioHandler.setMode(RTMP.MODE_CLIENT);
		ioHandler.setHandler(this);
		ioHandler.setRtmpConnManager(RTMPClientConnManager.getInstance());
	}

	public Map<String, Object> makeDefaultConnectionParams(String server, int port, String application) {
		Map<String, Object> params = super.makeDefaultConnectionParams(server, port, application);
		if (!params.containsKey("tcUrl")) {
			params.put("tcUrl", String.format("rtmps://%s:%s/%s", server, port, application));
		}
		return params;
	}
	
	@SuppressWarnings({ "rawtypes" })
	@Override
	protected void startConnector(String server, int port) {
		socketConnector = new NioSocketConnector();
		socketConnector.setHandler(ioHandler);
		future = socketConnector.connect(new InetSocketAddress(server, port));
		future.addListener(
				new IoFutureListener() {
					public void operationComplete(IoFuture future) {
						try {
							// will throw RuntimeException after connection error
							future.getSession(); 
						} catch (Throwable e) {
							//if there isn't an ClientExceptionHandler set, a 
							//RuntimeException may be thrown in handleException
							handleException(e);
						}
					}
				}
		);
	    // Do the close requesting that the pending messages are sent before
	    // the session is closed
		future.getSession().close(false);
	    // Now wait for the close to be completed
		future.awaitUninterruptibly(CONNECTOR_WORKER_TIMEOUT);
	    // We can now dispose the connector
		socketConnector.dispose();
	}
	
}
