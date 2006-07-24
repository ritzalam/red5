package org.red5.server.net.rtmp;

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
import org.apache.mina.common.IoSession;
import org.red5.server.net.rtmp.message.Packet;

public class RTMPMinaConnection extends RTMPConnection {

	protected static Log log =
        LogFactory.getLog(RTMPMinaConnection.class.getName());

	private IoSession ioSession;
	
	public RTMPMinaConnection(IoSession protocolSession) {
		super(PERSISTENT);
		SocketAddress remote = protocolSession.getRemoteAddress();
		if (remote instanceof InetSocketAddress) {
			remoteAddress = ((InetSocketAddress) remote).getAddress().getHostAddress();
			remotePort = ((InetSocketAddress) remote).getPort();
		} else {
			remoteAddress = remote.toString();
			remotePort = -1;
		}
		this.ioSession = protocolSession;
	}
		
	public IoSession getIoSession() {
		return ioSession;
	}

	/*
	public void dispatchEvent(Object packet){
		ioSession.write(packet);
	}
	*/
	
	@Override
	public void rawWrite(ByteBuffer out) {
		ioSession.write(out);
	}

	@Override
	public void write(Packet out) {
		writingMessage(out);
		ioSession.write(out);
	}

	public boolean isConnected() {
		return super.isConnected() && ioSession.isConnected();
	}
	
	public long getReadBytes() {
		return ioSession.getReadBytes();
	}
	
	public long getWrittenBytes() {
		return ioSession.getWrittenBytes();
	}
	
	public long getPendingMessages() {
		return ioSession.getScheduledWriteRequests();
	}

	public void close() {
		super.close();
		ioSession.close();
	}
}
