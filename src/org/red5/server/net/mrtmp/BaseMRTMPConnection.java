package org.red5.server.net.mrtmp;

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

import org.apache.mina.common.IoSession;
import org.red5.server.net.rtmp.message.Packet;

/**
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class BaseMRTMPConnection implements IMRTMPConnection {
	private IoSession ioSession;
	
	public void write(int clientId, Packet packet) {
		MRTMPPacket mrtmpPacket = new MRTMPPacket();
		MRTMPPacket.RTMPHeader header = new MRTMPPacket.RTMPHeader();
		MRTMPPacket.RTMPBody body = new MRTMPPacket.RTMPBody();
		mrtmpPacket.setHeader(header);
		mrtmpPacket.setBody(body);
		header.setType(MRTMPPacket.RTMP);
		header.setClientId(clientId);
		// header and body length will be filled in the protocol codec
		header.setRtmpType(packet.getHeader().getDataType());
		body.setRtmpPacket(packet);
		ioSession.write(mrtmpPacket);
	}
	
	public void connect(int clientId) {
		MRTMPPacket mrtmpPacket = new MRTMPPacket();
		MRTMPPacket.Header header = new MRTMPPacket.Header();
		MRTMPPacket.Body body = new MRTMPPacket.Body();
		mrtmpPacket.setHeader(header);
		mrtmpPacket.setBody(body);
		header.setType(MRTMPPacket.CONNECT);
		header.setClientId(clientId);
		// header and body length will be filled in the protocol codec
		ioSession.write(mrtmpPacket);
	}

	public void disconnect(int clientId) {
		MRTMPPacket mrtmpPacket = new MRTMPPacket();
		MRTMPPacket.Header header = new MRTMPPacket.Header();
		MRTMPPacket.Body body = new MRTMPPacket.Body();
		mrtmpPacket.setHeader(header);
		mrtmpPacket.setBody(body);
		header.setType(MRTMPPacket.CLOSE);
		header.setClientId(clientId);
		// header and body length will be filled in the protocol codec
		ioSession.write(mrtmpPacket);		
	}

	public void close() {
		ioSession.close();
	}

	public IoSession getIoSession() {
		return ioSession;
	}

	public void setIoSession(IoSession ioSession) {
		this.ioSession = ioSession;
	}
}
