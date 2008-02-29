package org.red5.samples.client;

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
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.transport.socket.nio.SocketConnector;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.IRTMPHandler;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaIoHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPCodecFactory;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.service.PendingCall;

/**
 * Sample class that uses the client mode of the RTMP library
 * to connect to the "oflaDemo" application on the current server.
 *
 * @see http://mirror1.cvsdude.com/trac/osflash/red5/ticket/94
 * @author ce@publishing-etc.de
 */
public class SimpleClient implements IRTMPHandler {
	
	public static void main(String[] args) {
		RTMPCodecFactory codecFactory=new RTMPCodecFactory();
		codecFactory.setDeserializer(new Deserializer());
		codecFactory.setSerializer(new Serializer());
		codecFactory.init();
		
		RTMPMinaIoHandler ioHandler=new RTMPMinaIoHandler();
		ioHandler.setCodecFactory(codecFactory);
		ioHandler.setMode(RTMP.MODE_CLIENT);
		ioHandler.setHandler(new SimpleClient());
		
		SocketConnector connector = new SocketConnector();
		connector.connect(new InetSocketAddress("localhost",1935), ioHandler);
	}
	
	/** {@inheritDoc} */
    public void connectionOpened(RTMPConnection conn, RTMP state) {
		System.out.println("opened");
		Channel channel=conn.getChannel((byte)3);
		Map<String,Object> params=new HashMap<String, Object>();
		params.put("app","test");
		params.put("flashVer", "WIN 9,0,16,0");
		params.put("swfUrl","http://localhost/test.swf");
		params.put("tcUrl", "rtmp://localhost/oflaDemo");
		params.put("fpad", false);
		params.put("audioCodecs",(double)615);
		params.put("videoCodecs",(double)76);
		params.put("pageUrl","http://localhost/test.html");
		params.put("objectEncoding",(double)0);
		PendingCall pendingCall=new PendingCall("connect");
		Invoke invoke=new Invoke(pendingCall);
		invoke.setConnectionParams(params);
		invoke.setInvokeId(1);
		channel.write(invoke);
	}
	
	/** {@inheritDoc} */
    public void messageReceived(RTMPConnection conn, ProtocolState state, Object message) throws Exception {
		System.out.println("message received "+message);
		if(message instanceof Packet) {
			Packet p=(Packet)message;
			System.out.println("got packet "+p.getMessage());
		}
	}
	
	/** {@inheritDoc} */
    public void messageSent(RTMPConnection conn, Object message) {
		System.out.println("message sent "+message);
		
	}

	/** {@inheritDoc} */
    public void connectionClosed(RTMPConnection conn, RTMP state) {
		System.out.println("connection closed");
		
	}

}