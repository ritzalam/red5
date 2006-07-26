package org.red5.server.stream.consumer;

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
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.StatusMessage;

public class ConnectionConsumer implements IPushableConsumer,
		IPipeConnectionListener {
	private static final Log log = LogFactory.getLog(ConnectionConsumer.class);
	
	public static final String KEY = ConnectionConsumer.class.getName();
	
	private RTMPConnection conn;
	private Channel video;
	private Channel audio;
	private Channel data;
	private boolean videofirst=true;
	private boolean audiofirst=true;
	private int deltafirst=0;
	private int chunkSize = -1;
	
	public ConnectionConsumer(RTMPConnection conn, byte videoChannel, byte audioChannel, byte dataChannel) {
		this.conn = conn;
		this.video = conn.getChannel(videoChannel);
		this.audio = conn.getChannel(audioChannel);
		this.data = conn.getChannel(dataChannel);
	}

	public void pushMessage(IPipe pipe, IMessage message) {
		if (message instanceof StatusMessage) {
			StatusMessage statusMsg = (StatusMessage) message;
			data.sendStatus(statusMsg.getBody());
		}
		else if (message instanceof RTMPMessage) {
			RTMPMessage rtmpMsg = (RTMPMessage) message;
			IRTMPEvent msg = rtmpMsg.getBody();
			if (msg.getHeader() == null) {
				msg.setHeader(new Header());
			}
			msg.getHeader().setTimerRelative(rtmpMsg.isTimerRelative());
			switch (msg.getDataType()) {
			case Constants.TYPE_STREAM_METADATA:
				Notify notify = new Notify(((Notify) msg).getData().asReadOnlyBuffer());
				notify.setHeader(msg.getHeader());
				notify.setTimestamp(msg.getTimestamp());
				video.write(notify);
				break;
			case Constants.TYPE_VIDEO_DATA:
				VideoData videoData = new VideoData(((VideoData) msg).getData().asReadOnlyBuffer());
				videoData.setHeader(msg.getHeader());
				videoData.setTimestamp(msg.getTimestamp());
				if (videofirst && audiofirst) {
					videofirst = false;
					deltafirst = videoData.getTimestamp();
					videoData.setTimestamp(0);
					videoData.getHeader().setTimerRelative(false);
				}
				if (videofirst && !audiofirst) {
					videofirst = false;
					videoData.setTimestamp(videoData.getTimestamp() - deltafirst);
				}
				video.write(videoData);
				break;
			case Constants.TYPE_AUDIO_DATA:
				AudioData audioData = new AudioData(((AudioData) msg).getData().asReadOnlyBuffer());
				audioData.setHeader(msg.getHeader());
				audioData.setTimestamp(msg.getTimestamp());
				if (audiofirst && videofirst) {
					audiofirst = false;
					deltafirst = audioData.getTimestamp();
					audioData.setTimestamp(0);
					audioData.getHeader().setTimerRelative(false);
					
					AudioData blankAudio=new AudioData();
					blankAudio.setTimestamp(0);
					audio.write(blankAudio);
				}
				if (audiofirst && !videofirst) {
					audiofirst = false;
					audioData.setTimestamp(audioData.getTimestamp() - deltafirst);
				}
				audio.write(audioData);
				break;
			case Constants.TYPE_PING:
				msg.getHeader().setTimerRelative(false);
				msg.setTimestamp(0);
				conn.ping((Ping) msg);
				break;
			case Constants.TYPE_BYTES_READ:
				msg.getHeader().setTimerRelative(false);
				msg.setTimestamp(0);
				conn.getChannel((byte) 2).write((BytesRead) msg);
				break;
			default:
				data.write(msg);
				break;
			}
		}
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		// TODO close channels on pipe disconnect
	}

	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
		if (!"ConnectionConsumer".equals(oobCtrlMsg.getTarget()))
			return;
		
		if ("pendingCount".equals(oobCtrlMsg.getServiceName())) {
			oobCtrlMsg.setResult(conn.getPendingMessages());
		} else if ("streamSend".equals(oobCtrlMsg.getServiceName())) {
			IServiceCall call = null;
			if (oobCtrlMsg.getServiceParamMap() != null)
				call = (IServiceCall) oobCtrlMsg.getServiceParamMap().get("call");
			
			if (call != null)
				// Call method on the stream
				conn.notify(call, data.getId());
		} else if ("pendingVideoCount".equals(oobCtrlMsg.getServiceName())) {
			IClientStream stream = conn.getStreamByChannelId(video.getId());
			oobCtrlMsg.setResult(conn.getPendingVideoMessages(stream.getStreamId()));
		} else if ("chunkSize".equals(oobCtrlMsg.getServiceName())) {
			int newSize = (Integer) oobCtrlMsg.getServiceParamMap().get("chunkSize");
			if (newSize != chunkSize) {
				chunkSize = newSize;
				ChunkSize chunkSizeMsg = new ChunkSize(chunkSize);
				conn.getChannel((byte) 2).write(chunkSizeMsg);
			}
		}
	}

}
