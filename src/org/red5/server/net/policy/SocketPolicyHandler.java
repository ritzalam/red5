package org.red5.server.net.policy;

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoAcceptor;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.transport.socket.nio.SocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the socket policy file.
 *
 * @see http://www.adobe.com/devnet/flashplayer/articles/socket_policy_files.html
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class SocketPolicyHandler extends IoHandlerAdapter {

	protected static Logger log = LoggerFactory
			.getLogger(SocketPolicyHandler.class);

	private String host = "0.0.0.0";

	private int port = 843;

	private String policyFileName = "flashpolicy.xml";

	private static IoAcceptor acceptor;
	
	private static ByteBuffer policyData;

	public void start() {
		log.debug("Starting socket policy file server");
        try {
			acceptor = new SocketAcceptor();
			acceptor.bind(new InetSocketAddress(host, port), this);
			log.info("Socket policy file server listening on port {}", port);
			//get the file
			File file = new File(System.getProperty("red5.config_root"), policyFileName);
			if (file.exists()) {
				//read the policy file
				policyData = ByteBuffer.allocate(Long.valueOf(file.length()).intValue());
				//temp space for reading file
				byte[] buf = new byte[128];
				//read it
				FileInputStream fis = new FileInputStream(file);
				while (fis.read(buf) != -1) {
					policyData.put(buf);
				}
				policyData.flip();
				fis.close();
				file = null;
				buf = null;
				log.info("Policy file read successfully");				
			} else {
				log.error("Policy file was not found");
			}			
		} catch (IOException e) {
			log.error("Exception initializing socket policy server", e);
		}			
	}

	public void stop() {
		log.debug("Stopping socket policy file server");
		acceptor.unbindAll();
	}
	
	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		log.info("Incomming: {}", session.getRemoteAddress().toString());
		session.write(policyData);
		session.close();
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable ex)
			throws Exception {
		log.info("Exception: {}", session.getRemoteAddress().toString(), ex);
	}	
	
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

}
