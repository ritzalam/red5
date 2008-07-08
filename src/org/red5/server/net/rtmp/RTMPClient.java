package org.red5.server.net.rtmp;

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

import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.red5.server.net.rtmp.codec.RTMP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP client object. Initial client mode code by Christian Eckerle.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Christian Eckerle (ce@publishing-etc.de)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Steven Gong (steven.gong@gmail.com)
 *
 */
public class RTMPClient extends BaseRTMPClientHandler {
    /**
     * Logger
     */
	private static final Logger log = LoggerFactory.getLogger(RTMPClient.class);
	
	/**
     * I/O handler
     */
	private RTMPMinaIoHandler ioHandler;

	/** Constructs a new RTMPClient. */
    public RTMPClient() {
		ioHandler = new RTMPMinaIoHandler();
		ioHandler.setCodecFactory(codecFactory);
		ioHandler.setMode(RTMP.MODE_CLIENT);
		ioHandler.setHandler(this);
		
		RTMPClientConnManager rtmpClientConnManager = new RTMPClientConnManager();
		ioHandler.setRtmpConnManager(rtmpClientConnManager);
		connManager = rtmpClientConnManager; 
	}

	public Map<String, Object> makeDefaultConnectionParams(String server, int port, String application) {
		Map<String, Object> params = super.makeDefaultConnectionParams(server, port, application);
		
		if (!params.containsKey("tcUrl"))
			params.put("tcUrl", "rtmp://"+server+':'+port+'/'+application);
		
		return params;
	}

	@Override
	protected void startConnector(String server, int port) {
		SocketConnector connector = new SocketConnector();
		connector.connect(new InetSocketAddress(server, port), ioHandler).addListener(
				new IoFutureListener() {
					public void operationComplete(IoFuture future) {
						try {
							// will throw RuntimeException after connection error
							future.getSession(); 
						} catch (Throwable e) {
							handleException(e);
						}
					}
				}
		);
	}
}
