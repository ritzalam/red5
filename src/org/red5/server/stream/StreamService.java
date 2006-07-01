package org.red5.server.stream;

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

import org.red5.server.BaseConnection;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.api.stream.support.SimplePlayItem;
import org.red5.server.net.rtmp.RTMPHandler;

public class StreamService implements IStreamService {
	public void closeStream() {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) return;
		IClientStream stream = ((IStreamCapableConnection) conn).getStreamById(getCurrentStreamId());
		stream.close();
	}

	public int createStream() {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) return -1;
		return ((IStreamCapableConnection) conn).reserveStreamId();
	}

	public void deleteStream(int streamId) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) return;
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		deleteStream(streamConn, streamId);
	}
	
	public void deleteStream(IStreamCapableConnection conn, int streamId) {
		IClientStream stream = conn.getStreamById(streamId);
		conn.unreserveStreamId(streamId);
		if (stream != null) {
			stream.close();
			if (stream instanceof IClientBroadcastStream) {
				IClientBroadcastStream bs = (IClientBroadcastStream) stream;
				IBroadcastScope bsScope = getBroadcastScope(conn.getScope(), bs.getPublishedName());
				if (bsScope != null && conn instanceof BaseConnection)
					((BaseConnection) conn).unregisterBasicScope(bsScope);
			}
		}
	}

	public void pause(boolean pausePlayback, int position) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) return;
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		IClientStream stream = streamConn.getStreamById(streamId);
		if (stream == null || !(stream instanceof ISubscriberStream)) return;
		ISubscriberStream subscriberStream = (ISubscriberStream) stream;
		if (pausePlayback) {
			subscriberStream.pause(position);
		} else {
			subscriberStream.resume(position);
		}
	}
	
	// "play" sometimes is called with "null" as last parameter.
	public void play(String name, int start, int length, Object flushPlaylist) {
		if (flushPlaylist instanceof Boolean)
			play(name, start, length, ((Boolean) flushPlaylist).booleanValue());
		else
			play(name, start, length);
	}

	public void play(String name, int start, int length, boolean flushPlaylist) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) return;
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		IClientStream stream = streamConn.getStreamById(streamId);
		if (stream == null) {
			stream = streamConn.newPlaylistSubscriberStream(streamId);
			stream.start();
		}
		if (!(stream instanceof ISubscriberStream)) return;
		ISubscriberStream subscriberStream = (ISubscriberStream) stream;
		SimplePlayItem item = new SimplePlayItem();
		item.setName(name);
		item.setStart(start);
		item.setLength(length);
		if (subscriberStream instanceof IPlaylistSubscriberStream) {
			IPlaylistSubscriberStream playlistStream = (IPlaylistSubscriberStream) subscriberStream;
			if (flushPlaylist) {
				playlistStream.removeAllItems();
			}
			playlistStream.addItem(item);
		} else if (subscriberStream instanceof ISingleItemSubscriberStream) {
			ISingleItemSubscriberStream singleStream = (ISingleItemSubscriberStream) subscriberStream;
			singleStream.setPlayItem(item);
		} else {
			// not supported by this stream service
			return;
		}
		subscriberStream.play();
	}

	public void play(String name, int start, int length) {
		play(name, start, length, false);
	}

	public void play(String name, int start) {
		play(name, start, -1, false);
	}

	public void play(String name) {
		play(name, -2000, -1, false);
	}

	public void publish(boolean dontStop) {
		if (!dontStop) {
			IConnection conn = Red5.getConnectionLocal();
			if (!(conn instanceof IStreamCapableConnection)) return;
			IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
			int streamId = getCurrentStreamId();
			IClientStream stream = streamConn.getStreamById(streamId);
			if (!(stream instanceof IBroadcastStream)) return;
			IBroadcastStream bs = (IBroadcastStream) stream;
			if (bs.getPublishedName() == null) return;
			IBroadcastScope bsScope = getBroadcastScope(conn.getScope(), bs.getPublishedName());
			if (bsScope != null) {
				bsScope.unsubscribe(bs.getProvider());
				if (conn instanceof BaseConnection)
					((BaseConnection) conn).unregisterBasicScope(bsScope);
			}
			bs.close();
		}
	}

	public void publish(String name, String mode) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) return;
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		IClientStream stream = streamConn.getStreamById(streamId);
		if (stream != null && !(stream instanceof IClientBroadcastStream)) return;
		if (stream == null) {
			stream = streamConn.newBroadcastStream(streamId);
			stream.start();
		} else {
			// already published
			return;
		}
		IClientBroadcastStream bs = (IClientBroadcastStream) stream;
		try {
			if (IClientStream.MODE_RECORD.equals(mode)) {
				bs.start();
				bs.saveAs(name, false);
			} else if (IClientStream.MODE_APPEND.equals(mode)) {
				bs.start();
				bs.saveAs(name, true);
			} else if (IClientStream.MODE_LIVE.equals(mode)) {
				IContext context = conn.getScope().getContext();
				IProviderService providerService = (IProviderService) context.getBean(IProviderService.KEY);
				bs.setPublishedName(name);
				// TODO handle registration failure
				if (providerService.registerBroadcastStream(conn.getScope(), name, bs)) {
					IBroadcastScope bsScope = getBroadcastScope(conn.getScope(), bs.getPublishedName());
					bsScope.setAttribute(IBroadcastScope.STREAM_ATTRIBUTE, bs);
					if (conn instanceof BaseConnection)
						((BaseConnection) conn).registerBasicScope(bsScope);
				}
				bs.start();
			}
		} catch (Exception e) {
			// TODO report publish error
		}
	}

	public void publish(String name) {
		publish(name, IClientStream.MODE_LIVE);
	}

	public void seek(int position) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) return;
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		IClientStream stream = streamConn.getStreamById(streamId);
		if (stream == null || !(stream instanceof ISubscriberStream)) return;
		ISubscriberStream subscriberStream = (ISubscriberStream) stream;
		subscriberStream.seek(position);
	}

	private int getCurrentStreamId() {
		// TODO: this must come from the current connection!
		return RTMPHandler.getStreamId();
	}
	
	public IBroadcastScope getBroadcastScope(IScope scope, String name) {
		synchronized (scope) {
			IBasicScope basicScope = scope.getBasicScope(IBroadcastScope.TYPE, name);
			if (!(basicScope instanceof IBroadcastScope)) return null;
			else return (IBroadcastScope) basicScope;
		}
	}
}
