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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Packet;

public class RTMPMinaConnection extends RTMPConnection {

	protected static Log log =
        LogFactory.getLog(RTMPMinaConnection.class.getName());

	private IoSession ioSession;
	private Map<Integer, Integer> pendingVideos = new HashMap<Integer, Integer>();
	
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

	long towrite = 0;

	@Override
	protected void messageSent(Packet message) {
		if (message.getMessage() instanceof VideoData) {
			int streamId = message.getHeader().getStreamId();
			synchronized (pendingVideos) {
				Integer pending = pendingVideos.get(streamId);
				if (pending != null)
					pendingVideos.put(streamId, pending-1);
				towrite -= 1;
			}
		}
		
		super.messageSent(message);
	}
	
	@Override
	public void write(Packet out) {
		if (out.getMessage() instanceof VideoData) {
			int streamId = out.getHeader().getStreamId();
			synchronized (pendingVideos) {
				Integer old = pendingVideos.get(streamId);
				if (old == null)
					old = new Integer(0);
				pendingVideos.put(streamId, old+1);
				towrite += 1;
			}
		}
		
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
	
	public long getPendingVideoMessages(int streamId) {
		synchronized (pendingVideos) {
			Integer count = pendingVideos.get(streamId);
			long result = (count != null ? count.intValue() - getUsedStreamCount() : 0);
			return (result > 0 ? result : 0);
		}
	}

	@Override
	public void deleteStreamById(int streamId) {
		if (streamId > 0 && streamId <= MAX_STREAMS) {
			synchronized (pendingVideos) {
				pendingVideos.remove(streamId);
			}
		}
		super.deleteStreamById(streamId);
	}

	public void close() {
		super.close();
		ioSession.close();
	}
}
