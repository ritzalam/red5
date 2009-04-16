package org.red5.server.net.proxy;

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
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.WritableByteChannel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

public class DebugProxyHandler extends IoHandlerAdapter implements
		ResourceLoaderAware {

	protected static Logger log = LoggerFactory.getLogger(DebugProxyHandler.class);

	private ResourceLoader loader;

	private ProtocolCodecFactory codecFactory;

	private InetSocketAddress forward;

	private String dumpTo = "./dumps/";

	/** {@inheritDoc} */
    public void setResourceLoader(ResourceLoader loader) {
		this.loader = loader;
	}

	/**
     * Setter for property 'codecFactory'.
     *
     * @param codecFactory Value to set for property 'codecFactory'.
     */
    public void setCodecFactory(ProtocolCodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}

	/**
     * Setter for property 'forward'.
     *
     * @param forward Value to set for property 'forward'.
     */
    public void setForward(String forward) {
		int split = forward.indexOf(':');
		String host = forward.substring(0, split);
		int port = Integer.parseInt(forward.substring(split + 1, forward
				.length()));
		this.forward = new InetSocketAddress(host, port);
	}

	/**
     * Setter for property 'dumpTo'.
     *
     * @param dumpTo Value to set for property 'dumpTo'.
     */
    public void setDumpTo(String dumpTo) {
		this.dumpTo = dumpTo;
	}

	/** {@inheritDoc} */
    @Override
	public void sessionOpened(IoSession session) throws Exception {
		// TODO Auto-generated method stub

		SocketSessionConfig ssc = (SocketSessionConfig) session.getConfig();
		ssc.setTcpNoDelay(true);
		//ssc.setReceiveBufferSize(2048);
		//ssc.setSendBufferSize(2048);

		super.sessionOpened(session);

	}

	/** {@inheritDoc} */
    @Override
	public void sessionCreated(IoSession session) throws Exception {

		boolean isClient = session.getRemoteAddress().equals(forward);
		//session.getConfig();

		if (log.isDebugEnabled()) {
			log.debug("Is downstream: " + isClient);

			session.setAttribute(ProtocolState.SESSION_KEY, new RTMP(isClient));

			session.getFilterChain().addFirst("protocol",
					new ProtocolCodecFilter(codecFactory));
		}

		session.getFilterChain().addFirst("proxy",
				new ProxyFilter(isClient ? "client" : "server"));

		if (true) {

			String fileName = System.currentTimeMillis() + '_'
					+ forward.getHostName() + '_' + forward.getPort() + '_'
					+ (isClient ? "DOWNSTREAM" : "UPSTREAM");

			File headersFile = loader.getResource(dumpTo + fileName + ".cap")
					.getFile();
			headersFile.createNewFile();

			File rawFile = loader.getResource(dumpTo + fileName + ".raw")
					.getFile();
			rawFile.createNewFile();

			FileOutputStream headersFos = new FileOutputStream(headersFile);
			WritableByteChannel headers = headersFos.getChannel();

			FileOutputStream rawFos = new FileOutputStream(rawFile);
			WritableByteChannel raw = rawFos.getChannel();

			IoBuffer header = IoBuffer.allocate(1);
			header.put((byte) (isClient ? 0x00 : 0x01));
			header.flip();
			headers.write(header.buf());

			session.getFilterChain().addFirst("dump",
					new NetworkDumpFilter(headers, raw));
		}

		//session.getFilterChain().addLast(
		//        "logger", new LoggingFilter() );

		if (!isClient) {
			log.debug("Connecting..");
			IoConnector connector = new NioSocketConnector();
			connector.setHandler(this);
			ConnectFuture future = connector.connect(forward);
			future.awaitUninterruptibly(); // wait for connect, or timeout
			if (future.isConnected()) {
				if (log.isDebugEnabled()) {
					log.debug("Connected: {}", forward);
				}
				IoSession client = future.getSession();
				client.setAttribute(ProxyFilter.FORWARD_KEY, session);
				session.setAttribute(ProxyFilter.FORWARD_KEY, client);
			}
		}
		super.sessionCreated(session);
	}

	/** {@inheritDoc} */
    @Override
	public void messageReceived(IoSession session, Object in) {

		if (!log.isDebugEnabled()) {
			return;
		}

		if (in instanceof IoBuffer) {
			log.debug("Handskake");
			return;
		}

		try {

			final Packet packet = (Packet) in;
			final Object message = packet.getMessage();
			final Header source = packet.getHeader();

			log.debug("{}", source);
			log.debug("{}", message);

		} catch (RuntimeException e) {
			log.error("Exception", e);
		}
	}

	/** {@inheritDoc} */
    @Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
    	if (log.isDebugEnabled()) {
    		log.debug("Exception caught", cause);
    	}
	}

}
