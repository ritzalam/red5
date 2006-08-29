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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;

public class Channel {

	protected static Log log =
        LogFactory.getLog(Channel.class.getName());
	
	private RTMPConnection connection = null;
	private byte id = 0;
	//private Stream stream;

	public Channel(RTMPConnection conn, byte channelId){
		connection = conn;
		id = channelId;
	}
	
	public void close() {
		connection.closeChannel(id);
	}
	
	public byte getId(){
		return id;
	}
	
	/*
	public Stream getStream() {
		return stream;
	}
	*/

	public void write(IRTMPEvent event){
		final IClientStream stream = connection.getStreamByChannelId(id);
		if (id > 3 && stream == null) {
			log.info("Stream doesn't exist any longer, discarding message " + event);
			return;
		}
		/*
		final int streamId = (
				stream==null || (
						message.getDataType() != Constants.TYPE_AUDIO_DATA && 
						message.getDataType() != Constants.TYPE_VIDEO_DATA )
				) ? 0 : stream.getStreamId();
				*/
		final int streamId = ( stream==null ) ? 0 : stream.getStreamId();
		write(event, streamId);
	}
	
	private void write(IRTMPEvent event, int streamId){
		
		final Header header = new Header();
		final Packet packet = new Packet(header, event);
		
		header.setChannelId(id);
		header.setTimer(event.getTimestamp());
		header.setStreamId(streamId);
		header.setDataType(event.getDataType());
		if (event.getHeader() != null)
			header.setTimerRelative(event.getHeader().isTimerRelative());
		
		// should use RTMPConnection specific method.. 
		connection.write(packet);
		
	}

	public void sendStatus(Status status) {
		final boolean andReturn = !status.getCode().equals(Status.NS_DATA_START);
		final Invoke invoke;
		if (andReturn) {
			final PendingCall call = new PendingCall(null,"onStatus",new Object[]{status});
			invoke = new Invoke();
			invoke.setInvokeId(1);
			invoke.setCall(call);
		} else {
			final Call call = new Call(null,"onStatus",new Object[]{status});
			invoke = (Invoke) new Notify();
			invoke.setInvokeId(1);
			((Notify) invoke).setCall(call);
		}
		write(invoke);
	}

}
