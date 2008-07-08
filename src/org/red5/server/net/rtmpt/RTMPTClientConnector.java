package org.red5.server.net.rtmpt;

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

import java.util.List;
import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.RTMPClientConnManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Anton Lebedevich
 */
class RTMPTClientConnector extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RTMPTClientConnector.class);
	
	
	private static final String CONTENT_TYPE = "application/x-fcs";
	
	private static final ByteArrayRequestEntity ZERO_REQUEST_ENTITY = new ByteArrayRequestEntity(new byte[]{0}, CONTENT_TYPE);
	
	/** 
	 * Size to split messages queue by, borrowed from RTMPTServlet.RESPONSE_TARGET_SIZE 
	 */
	private static final int SEND_TARGET_SIZE = 32768;
	
	
	private HttpClient httpClient = new HttpClient();
	private RTMPTClient client;
	private RTMPClientConnManager connManager;
	
	private int clientId;
	private long messageCount = 1;

	public RTMPTClientConnector(String server, int port, RTMPTClient client) {
		httpClient.getHostConfiguration().setHost(server, port);
		httpClient.getHttpConnectionManager().closeIdleConnections(0);
		
		HttpClientParams params = new HttpClientParams();
		params.setVersion(HttpVersion.HTTP_1_1);
		httpClient.setParams(params);
		
		this.client = client;
		this.connManager = client.getConnManager();
	}
	
	public void run() {
		try {
			RTMPTClientConnection connection = openConnection();
			
			while (!connection.isClosing()) {
				ByteBuffer toSend = connection.getPendingMessages(SEND_TARGET_SIZE);
				PostMethod post;
				if (toSend.limit() > 0) {
					post = makePost("send");
					post.setRequestEntity(new InputStreamRequestEntity(toSend.asInputStream(), 
							CONTENT_TYPE));
				} else {
					post = makePost("idle");
					post.setRequestEntity(ZERO_REQUEST_ENTITY);
				}
				httpClient.executeMethod(post);
				byte[] received = post.getResponseBody();
				
				ByteBuffer data = ByteBuffer.allocate(received.length);
				data.put(received);
				data.flip();
				data.skip(1); // XXX: polling interval lies in this byte
				List messages = connection.decode(data);
				data.release();
				
				if (messages == null || messages.isEmpty()) {
					try {
						// XXX handle polling delay
						Thread.sleep(250);
					} catch (InterruptedException e) {
						// ignore
					}
					continue;
				}

				for (Object message : messages) {
					try {
						client.messageReceived(connection, connection.getState(), message);
					} catch (Exception e) {
						log.error("Could not process message.", e);
					}
				}
			}

			finalizeConnection();
			
			client.connectionClosed(connection, connection.getState());
			
		} catch (Throwable e) {
			log.debug("RTMPT handling exception", e);
			client.handleException(e);
		}
	}

	private RTMPTClientConnection openConnection() throws IOException {
		
		PostMethod openPost = new PostMethod("/open/1");
		setCommonHeaders(openPost);
		openPost.setRequestEntity(ZERO_REQUEST_ENTITY);

		httpClient.executeMethod(openPost);

		String response = openPost.getResponseBodyAsString();
		clientId = Integer.parseInt(response.substring(0, response.length() - 1));
		log.debug("Got client id {}", clientId);

		RTMPTClientConnection connection = (RTMPTClientConnection)connManager
				.createConnection(RTMPTClientConnection.class);
		
		RTMP state = new RTMP(RTMP.MODE_CLIENT);
		connection.setState(state);
		
		connection.setClient(client);

		log.debug("Handshake 1st phase");
		ByteBuffer handshake = ByteBuffer.allocate(Constants.HANDSHAKE_SIZE + 1);
		handshake.put((byte)0x03);
		handshake.fill((byte)0x01, Constants.HANDSHAKE_SIZE);
		handshake.flip();
		connection.rawWrite(handshake);
		
		return connection;
	}

	private void finalizeConnection() throws IOException {
		log.debug("Sending close post");
		PostMethod closePost = new PostMethod(makeUrl("close"));
		closePost.setRequestEntity(ZERO_REQUEST_ENTITY);
		httpClient.executeMethod(closePost);
		closePost.getResponseBody();
	}

	private PostMethod makePost(String command) {
		PostMethod post = new PostMethod(makeUrl(command));
		setCommonHeaders(post);
		return post;
	}

	private String makeUrl(String command) {
		// use message count from connection
		return new StringBuffer().append('/').append(command)
				.append('/').append(clientId).append('/').append(messageCount++).toString();
	}

	private void setCommonHeaders(PostMethod post) {
		post.setRequestHeader("Connection", "Keep-Alive");
		post.setRequestHeader("Cache-Control", "no-cache");
	}
}
