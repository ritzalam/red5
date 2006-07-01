package org.red5.server.net.proxy;

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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;

public class DebugProxyHandler extends IoHandlerAdapter {

	protected static Log log =
        LogFactory.getLog(DebugProxyHandler.class.getName());
	
	public ProtocolCodecFactory codecFactory = null;
	private SocketAddress forward = null;

	public void setCodecFactory(ProtocolCodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}

	public void setForward(String forward) {
		int split = forward.indexOf(':');
		String host = forward.substring(0,split);
		int port = Integer.parseInt( forward.substring(split+1,forward.length()) );
		this.forward = new InetSocketAddress(host,port); 
	}
	
	public void sessionCreated(IoSession session) throws Exception {

		boolean isClient = session.getRemoteAddress().equals(forward);
		
		log.debug("Is client: "+isClient);
		
		session.setAttribute(RTMP.SESSION_KEY,new RTMP(isClient));
		
		if(isClient){
			session.getFilterChain().addFirst(
                "protocol",new ProtocolCodecFilter(codecFactory) );
		}
		
		session.getFilterChain().addFirst(
                "proxy", new ProxyFilter(isClient ? "down" : "up") );

        //session.getFilterChain().addLast(
        //        "logger", new LoggingFilter() );
		
        if(!isClient){
        	SocketConnector connector = new SocketConnector();
        	ConnectFuture future = connector.connect(forward, this);
    		future.join(); // wait for connect, or timeout
    		if(future.isConnected()){
    			log.debug("Connected: "+forward);
    			IoSession client = future.getSession();
    			client.setAttribute(ProxyFilter.FORWARD_KEY, session);
    			session.setAttribute(ProxyFilter.FORWARD_KEY,client);
    		}
		}    
		super.sessionCreated(session);
	}
	
	public void messageReceived(IoSession session, Object in) {
		
		if(in instanceof ByteBuffer){
			log.debug("Handskake");
			return;
		}
		
		try {
			
			final RTMPMinaConnection conn = (RTMPMinaConnection) session.getAttachment();
						
			final Packet packet = (Packet) in;
			final Object message = packet.getMessage();
			final Header source = packet.getHeader();
			final Channel channel = conn.getChannel(packet.getHeader().getChannelId());
			
			log.info(source);
			log.info(message);
			
		} catch (RuntimeException e) {
			log.debug("Exception",e);
		}
	}

}
