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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.so.SharedObjectMessage;
import org.red5.server.stream.PlaylistSubscriberStream;

/**
 * Base class for all RTMP handlers.
 * 
 * @author The Red5 Project (red5@osflash.org)
 */
public abstract class BaseRTMPHandler implements IRTMPHandler, Constants, StatusCodes {

	protected static Log log = LogFactory.getLog(BaseRTMPHandler.class.getName());

	// XXX: HACK HACK HACK to support stream ids
	private static ThreadLocal<Integer> streamLocal = new ThreadLocal<Integer>();

	/**
     * Getter for property 'streamId'.
     *
     * @return Value for property 'streamId'.
     */ // XXX: HACK HACK HACK to support stream ids
	public static int getStreamId() {
		return streamLocal.get().intValue();
	}

	/**
     * Setter for property 'streamId'.
     *
     * @param id Value to set for property 'streamId'.
     */
    private static void setStreamId(int id) {
		streamLocal.set(id);
	}

	/** {@inheritDoc} */
    public void connectionOpened(RTMPConnection conn, RTMP state) {
		// Nothing to do here
	}

	/** {@inheritDoc} */
    public void messageReceived(RTMPConnection conn, ProtocolState state,
			Object in) throws Exception {

		IRTMPEvent message = null;
		try {

			final Packet packet = (Packet) in;
			message = packet.getMessage();
			final Header header = packet.getHeader();
			final Channel channel = conn.getChannel(header.getChannelId());
			final IClientStream stream = conn.getStreamById(header
					.getStreamId());

			if (log.isDebugEnabled()) {
				log.debug("Message recieved");
				log.debug("Stream Id: " + header);
				log.debug("Channel: " + channel);
			}

			// Thread local performance ? Should we benchmark
			Red5.setConnectionLocal(conn);

			// XXX: HACK HACK HACK to support stream ids
			BaseRTMPHandler.setStreamId(header.getStreamId());

			// Increase number of received messages
			conn.messageReceived();

			//if (message instanceof IRTMPEvent) {
			message.setSource(conn);
			//}

			switch (header.getDataType()) {
				case TYPE_CHUNK_SIZE:
					onChunkSize(conn, channel, header, (ChunkSize) message);
					break;

				case TYPE_INVOKE:
				case TYPE_FLEX_MESSAGE:
					onInvoke(conn, channel, header, (Invoke) message);
					if(message.getHeader().getStreamId()!=0  
							&& ((Invoke)message).getCall().getServiceName()==null
							&& ACTION_PUBLISH.equals(((Invoke)message).getCall().getServiceMethodName())) {
						IClientStream s = conn.getStreamById(header.getStreamId());
						if (s != null)
							// Only dispatch if stream really was created 
							((IEventDispatcher) s).dispatchEvent(message);
					}
					break;

				case TYPE_NOTIFY: // just like invoke, but does not return
					if (((Notify) message).getData() != null && stream != null) {
						// Stream metadata
						((IEventDispatcher) stream).dispatchEvent(message);
					} else {
						onInvoke(conn, channel, header, (Notify) message);
					}
					break;
				case TYPE_PING:
					onPing(conn, channel, header, (Ping) message);
					break;

				case TYPE_BYTES_READ:
					onStreamBytesRead(conn, channel, header,
							(BytesRead) message);
					break;

				case TYPE_AUDIO_DATA:
				case TYPE_VIDEO_DATA:
					// log.info("in packet: "+source.getSize()+"
					// ts:"+source.getTimer());
					
					// NOTE: If we respond to "publish" with "NetStream.Publish.BadName",
					// the client sends a few stream packets before stopping. We need to
					// ignore them.
					if (stream != null)
						((IEventDispatcher) stream).dispatchEvent(message);
					break;
				case TYPE_SHARED_OBJECT:
					onSharedObject(conn, channel, header,
							(SharedObjectMessage) message);
					break;
			}
			if (message instanceof Unknown) {
				log.info(message);
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			log.error("Exception", e);
		}
		if (message != null) {
			message.release();
		}
	}

	/** {@inheritDoc} */
    public void messageSent(RTMPConnection conn, Object message) {
		if (log.isDebugEnabled()) {
			log.debug("Message sent");
		}

		if (message instanceof ByteBuffer) {
			return;
		}

		// Increase number of sent messages
		conn.messageSent((Packet) message);

		Packet sent = (Packet) message;
		final byte channelId = sent.getHeader().getChannelId();
		final IClientStream stream = conn.getStreamByChannelId(channelId);
		// XXX we'd better use new event model for notification
		if (stream != null && (stream instanceof PlaylistSubscriberStream)) {
			((PlaylistSubscriberStream) stream).written(sent.getMessage());
		}
	}

	/** {@inheritDoc} */
    public void connectionClosed(RTMPConnection conn, RTMP state) {
		state.setState(RTMP.STATE_DISCONNECTED);
		conn.close();
	}

	protected String getHostname(String url) {
		if (log.isDebugEnabled()) {
			log.debug("url: " + url);
		}
		String[] parts = url.split("/");
		if (parts.length == 2) {
			// TODO: is this a good default hostname?
			return "";
		} else {
			return parts[2];
		}
	}

	protected abstract void onChunkSize(RTMPConnection conn, Channel channel,
			Header source, ChunkSize chunkSize);

	protected void handlePendingCallResult(RTMPConnection conn, Notify invoke) {
		final IServiceCall call = invoke.getCall();
		final IPendingServiceCall pendingCall = conn.getPendingCall(invoke
				.getInvokeId());
		if (pendingCall != null) {
			// The client sent a response to a previously made call.
			Object[] args = call.getArguments();
			if ((args != null) && (args.length > 0)) {
				// TODO: can a client return multiple results?
				pendingCall.setResult(args[0]);
			}
	
			Set<IPendingServiceCallback> callbacks = pendingCall
					.getCallbacks();
			if (callbacks.isEmpty()) {
				return;
			}
	
			HashSet<IPendingServiceCallback> tmp = new HashSet<IPendingServiceCallback>();
			tmp.addAll(callbacks);
			Iterator<IPendingServiceCallback> it = tmp.iterator();
			while (it.hasNext()) {
				IPendingServiceCallback callback = it.next();
				try {
					callback.resultReceived(pendingCall);
				} catch (Exception e) {
					log.error("Error while executing callback " + callback,
							e);
				}
			}
		}
		return;
	}

	protected abstract void onInvoke(RTMPConnection conn, Channel channel,
			Header source, Notify invoke);
	
	protected abstract void onPing(RTMPConnection conn, Channel channel,
			Header source, Ping ping);

	protected void onStreamBytesRead(RTMPConnection conn, Channel channel,
			Header source, BytesRead streamBytesRead) {
		conn.receivedBytesRead(streamBytesRead.getBytesRead());
	}

	protected abstract void onSharedObject(RTMPConnection conn, Channel channel,
			Header source, SharedObjectMessage object);

}
