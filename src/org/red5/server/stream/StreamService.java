package org.red5.server.stream;

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
import java.io.IOException;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.red5.server.BaseConnection;
import org.red5.server.api.IBasicScope;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.ScopeUtils;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPlaybackSecurity;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.IStreamSecurityService;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.api.stream.OperationNotSupportedException;
import org.red5.server.api.stream.support.SimplePlayItem;
import org.red5.server.net.rtmp.BaseRTMPHandler;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stream service
 */
public class StreamService implements IStreamService {

	private static Logger logger = LoggerFactory.getLogger(StreamService.class);

	/** {@inheritDoc} */
    public void closeStream() {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) {
			return;
		}
		IClientStream stream = ((IStreamCapableConnection) conn)
				.getStreamById(getCurrentStreamId());
		if (stream != null) {
			if (stream instanceof IClientBroadcastStream) {
				IClientBroadcastStream bs = (IClientBroadcastStream) stream;
				IBroadcastScope bsScope = getBroadcastScope(conn.getScope(), bs
						.getPublishedName());
				if (bsScope != null && conn instanceof BaseConnection) {
					((BaseConnection) conn).unregisterBasicScope(bsScope);
				}
			}
			stream.close();
		}
		((IStreamCapableConnection) conn)
				.deleteStreamById(getCurrentStreamId());
	}

	/**
	 * Close stream.
	 * This method can close both IClientBroadcastStream (coming from Flash Player to Red5)
	 * and ISubscriberStream (from Red5 to Flash Player).
	 * Corresponding application handlers (streamSubscriberClose, etc.) are called as if
	 * close was initiated by Flash Player.
	 * 
	 * It is recommended to remember stream id in application handlers, ex.:
	 * <pre>
	 * public void streamBroadcastStart(IBroadcastStream stream) {
	 * 	super.streamBroadcastStart(stream);
	 * 	if (stream instanceof IClientBroadcastStream) {
	 * 		int publishedStreamId = ((ClientBroadcastStream)stream).getStreamId();
	 * 		Red5.getConnectionLocal().setAttribute(PUBLISHED_STREAM_ID_ATTRIBUTE, publishedStreamId);
	 * 	}
	 * }
	 * </pre>
	 * <pre>
	 * public void streamPlaylistItemPlay(IPlaylistSubscriberStream stream, IPlayItem item, boolean isLive) {
	 * 	super.streamPlaylistItemPlay(stream, item, isLive);
	 * 	Red5.getConnectionLocal().setAttribute(WATCHED_STREAM_ID_ATTRIBUTE, stream.getStreamId());
	 * }
	 * </pre>
	 * When stream is closed, corresponding NetStream status will be sent to stream provider / consumers.
	 * Implementation is based on Red5's StreamService.close()
	 * @param connection client connection
	 * @param streamId stream ID (number: 1,2,...)
	 */
	public static void closeStream(IConnection connection, int streamId) {
		if (!(connection instanceof IStreamCapableConnection)) {
			logger.warn("Connection is not instance of IStreamCapableConnection: {}", connection);
			return;
		}

		IStreamCapableConnection scConnection = (IStreamCapableConnection) connection;
		IClientStream stream = scConnection.getStreamById(streamId);
		if (stream == null) {
			logger.info("Stream not found: streamId={}, connection={}", streamId, connection);
			return;
		}

		if (stream instanceof IClientBroadcastStream) {
			// this is a broadcasting stream (from Flash Player to Red5)
			IClientBroadcastStream bs = (IClientBroadcastStream) stream;
			IBroadcastScope bsScope = (IBroadcastScope)connection.getScope().getBasicScope(
					IBroadcastScope.TYPE, bs.getPublishedName());
			if (bsScope != null && connection instanceof BaseConnection) {
				((BaseConnection) connection).unregisterBasicScope(bsScope);
			}
		}
		stream.close();
		scConnection.deleteStreamById(streamId);

		// in case of broadcasting stream, status is sent automatically by Red5
		if (!(stream instanceof IClientBroadcastStream)) {
			StreamService.sendNetStreamStatus(connection, StatusCodes.NS_PLAY_STOP, "Stream closed by server", stream.getName(), Status.STATUS, streamId);
		}
	}    
    
	/** {@inheritDoc} */
    public int createStream() {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) {
			return -1;
		}
		return ((IStreamCapableConnection) conn).reserveStreamId();
	}

	/** {@inheritDoc} */
    public void deleteStream(int streamId) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) {
			return;
		}
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		deleteStream(streamConn, streamId);
	}

	/** {@inheritDoc} */
    public void deleteStream(IStreamCapableConnection conn, int streamId) {
		IClientStream stream = conn.getStreamById(streamId);
		if (stream != null) {
			if (stream instanceof IClientBroadcastStream) {
				IClientBroadcastStream bs = (IClientBroadcastStream) stream;
				IBroadcastScope bsScope = getBroadcastScope(conn.getScope(), bs
						.getPublishedName());
				if (bsScope != null && conn instanceof BaseConnection) {
					((BaseConnection) conn).unregisterBasicScope(bsScope);
				}
			}
			stream.close();
		}
		conn.unreserveStreamId(streamId);
	}
    
	/** {@inheritDoc} */
    public void releaseStream(String streamName) {
    	// XXX: what to do here?
    }

	/** {@inheritDoc} */
    public void pause(boolean pausePlayback, int position) {
		pause(Boolean.valueOf(pausePlayback), position);
	}

	/** {@inheritDoc} */
	public void pauseRaw(boolean pausePlayback, int position) {
		pause(pausePlayback, position);
	}
	
    /**
     * Pause at given position. Required as "pausePlayback" can be "null" if no flag is passed by the
	 * client
     * @param pausePlayback         Pause playback or not
     * @param position              Pause position
     */
    public void pause(Boolean pausePlayback, int position) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) {
			return;
		}
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		IClientStream stream = streamConn.getStreamById(streamId);
		if (stream == null || !(stream instanceof ISubscriberStream)) {
			return;
		}
		ISubscriberStream subscriberStream = (ISubscriberStream) stream;
		// pausePlayback can be "null" if "pause" is called without any parameters from flash
		if (pausePlayback == null) {
			pausePlayback = !subscriberStream.isPaused();
		}
		if (pausePlayback) {
			subscriberStream.pause(position);
		} else {
			subscriberStream.resume(position);
		}
	}

    public void play(String name, int start, int length, Object flushPlaylist) {
		if (flushPlaylist instanceof Boolean) {
			play(name, start, length, ((Boolean) flushPlaylist).booleanValue());
		} else {
			play(name, start, length);
		}
	}

	/** {@inheritDoc} */
    public void play(String name, int start, int length, boolean flushPlaylist) {
    	logger.debug("Play called - name: {} start: {} length: {} flush playlist: {}", new Object[]{name, start, length, flushPlaylist});
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) {
			return;
		}
		IScope scope = conn.getScope();
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		if (StringUtils.isEmpty(name)) {
			sendNSFailed((RTMPConnection) streamConn, "The stream name may not be empty.", name, streamId);
			return;
		}
		IStreamSecurityService security = (IStreamSecurityService) ScopeUtils.getScopeService(scope, IStreamSecurityService.class);
		if (security != null) {
			Set<IStreamPlaybackSecurity> handlers = security.getStreamPlaybackSecurity();
			for (IStreamPlaybackSecurity handler: handlers) {
				if (!handler.isPlaybackAllowed(scope, name, start, length, flushPlaylist)) {
					sendNSFailed((RTMPConnection) streamConn, "You are not allowed to play the stream.", name, streamId);
					return;
				}
			}
		}
		IClientStream stream = streamConn.getStreamById(streamId);
		boolean created = false;
		if (stream == null) {
			stream = streamConn.newPlaylistSubscriberStream(streamId);
			stream.start();
			created = true;
		}
		if (!(stream instanceof ISubscriberStream)) {
			return;
		}
		ISubscriberStream subscriberStream = (ISubscriberStream) stream;
		SimplePlayItem item = new SimplePlayItem();
		item.setName(name);
		item.setStart(start);
		item.setLength(length);
		
		//get file size in bytes if available
		IProviderService providerService = (IProviderService) scope.getContext().getBean(IProviderService.BEAN_NAME);
		if (providerService != null) {
			File file = providerService.getVODProviderFile(scope, name);
			if (file != null) {
				item.setSize(file.length());
			} else {
				logger.debug("File was null, this is ok for live streams");
			}
		} else {
			logger.debug("ProviderService was null");
		}

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
		try {
			subscriberStream.play();
		} catch (IOException err) {
			if (created) {
				stream.close();
				streamConn.deleteStreamById(streamId);
			}
			sendNSFailed((RTMPConnection) streamConn, err.getMessage(), name, streamId);
		}
	}

	/** {@inheritDoc} */
    public void play(String name, int start, int length) {
		play(name, start, length, true);
	}

	/** {@inheritDoc} */
    public void play(String name, int start) {
		play(name, start, -1000, true);
	}

	/** {@inheritDoc} */
    public void play(String name) {
		play(name, -2000, -1000, true);
	}

	/** {@inheritDoc} */
    public void play(Boolean dontStop) {
    	logger.debug("Play called. Dont stop param: {}", dontStop);
		if (!dontStop) {
			IConnection conn = Red5.getConnectionLocal();
			if (!(conn instanceof IStreamCapableConnection)) {
				return;
			}
			IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
			int streamId = getCurrentStreamId();
			IClientStream stream = streamConn.getStreamById(streamId);
			if (stream != null) {
				stream.stop();
			}
		}
	}

	/** {@inheritDoc} */
    public void publish(Boolean dontStop) {
		if (!dontStop) {
			IConnection conn = Red5.getConnectionLocal();
			if (!(conn instanceof IStreamCapableConnection)) {
				return;
			}
			IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
			int streamId = getCurrentStreamId();
			IClientStream stream = streamConn.getStreamById(streamId);
			if (!(stream instanceof IBroadcastStream)) {
				return;
			}
			IBroadcastStream bs = (IBroadcastStream) stream;
			if (bs.getPublishedName() == null) {
				return;
			}
			IBroadcastScope bsScope = getBroadcastScope(conn.getScope(), bs
					.getPublishedName());
			if (bsScope != null) {
				bsScope.unsubscribe(bs.getProvider());
				if (conn instanceof BaseConnection) {
					((BaseConnection) conn).unregisterBasicScope(bsScope);
				}
			}
			bs.close();
			streamConn.deleteStreamById(streamId);
		}
	}

	/** {@inheritDoc} */
    public void publish(String name, String mode) {
    	if (name != null && name.contains("?"))
    		name = name.substring(0, name.indexOf("?"));
    	
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) {
			return;
		}
		
		IScope scope = conn.getScope();
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		if (StringUtils.isEmpty(name)) {
			sendNSFailed((RTMPConnection) streamConn, "The stream name may not be empty.", name, streamId);
			return;
		}

		IStreamSecurityService security = (IStreamSecurityService) ScopeUtils.getScopeService(scope, IStreamSecurityService.class);
		if (security != null) {
			Set<IStreamPublishSecurity> handlers = security.getStreamPublishSecurity();
			for (IStreamPublishSecurity handler: handlers) {
				if (!handler.isPublishAllowed(scope, name, mode)) {
					sendNSFailed((RTMPConnection) streamConn, "You are not allowed to publish the stream.", name, streamId);
					return;
				}
			}
		}
		
		IBroadcastScope bsScope = getBroadcastScope(scope, name);
		if (bsScope != null && !bsScope.getProviders().isEmpty()) {
			// Another stream with that name is already published.
			Status badName = new Status(StatusCodes.NS_PUBLISH_BADNAME);
			badName.setClientid(streamId);
			badName.setDetails(name);
			badName.setLevel("error");

			// FIXME: there should be a direct way to send the status
			Channel channel = ((RTMPConnection) streamConn).getChannel((byte) (4 + ((streamId-1) * 5)));
			channel.sendStatus(badName);
			return;
		}

		IClientStream stream = streamConn.getStreamById(streamId);
		if (stream != null && !(stream instanceof IClientBroadcastStream)) {
			return;
		}
		boolean created = false;
		if (stream == null) {
			stream = streamConn.newBroadcastStream(streamId);
			created = true;
		}

		IClientBroadcastStream bs = (IClientBroadcastStream) stream;
		try {
			bs.setPublishedName(name);
			IContext context = conn.getScope().getContext();
			IProviderService providerService = (IProviderService) context
					.getBean(IProviderService.BEAN_NAME);
			// TODO handle registration failure
			if (providerService.registerBroadcastStream(conn.getScope(),
					name, bs)) {
				bsScope = getBroadcastScope(conn.getScope(), name);
				bsScope.setAttribute(IBroadcastScope.STREAM_ATTRIBUTE, bs);
				if (conn instanceof BaseConnection) {
					((BaseConnection) conn).registerBasicScope(bsScope);
				}
			}
			logger.debug("Mode: {}", mode);
			if (IClientStream.MODE_RECORD.equals(mode)) {
				bs.start();
				bs.saveAs(name, false);
			} else if (IClientStream.MODE_APPEND.equals(mode)) {
				bs.start();
				bs.saveAs(name, true);
			} else if (IClientStream.MODE_PUBLISH.equals(mode) || IClientStream.MODE_LIVE.equals(mode)) {
				bs.start();
			}
			bs.startPublishing();
		} catch (IOException e) {
			Status accessDenied = new Status(StatusCodes.NS_RECORD_NOACCESS);
			accessDenied.setClientid(streamId);
			accessDenied.setDesciption("The file could not be created/written to.");
			accessDenied.setDetails(name);
			accessDenied.setLevel("error");

			// FIXME: there should be a direct way to send the status
			Channel channel = ((RTMPConnection) streamConn).getChannel((byte) (4 + ((streamId-1) * 5)));
			channel.sendStatus(accessDenied);
			bs.close();
			if (created) {
				streamConn.deleteStreamById(streamId);
			}
			return;
		} catch (Exception e) {
			logger.warn("Exception on publish", e);
		}
	}

	/** {@inheritDoc} */
    public void publish(String name) {
		publish(name, IClientStream.MODE_LIVE);
	}

	/** {@inheritDoc} */
    public void seek(int position) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) {
			return;
		}
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		IClientStream stream = streamConn.getStreamById(streamId);
		if (stream == null || !(stream instanceof ISubscriberStream)) {
			return;
		}
		ISubscriberStream subscriberStream = (ISubscriberStream) stream;
		try {
			subscriberStream.seek(position);
		} catch (OperationNotSupportedException err) {
			Status seekFailed = new Status(StatusCodes.NS_SEEK_FAILED);
			seekFailed.setClientid(streamId);
			seekFailed.setDesciption("The stream doesn't support seeking.");
			seekFailed.setLevel("error");

			// FIXME: there should be a direct way to send the status
			Channel channel = ((RTMPConnection) streamConn).getChannel((byte) (4 + ((streamId-1) * 5)));
			channel.sendStatus(seekFailed);
		}
	}

	/** {@inheritDoc} */
    public void receiveVideo(boolean receive) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) {
			return;
		}
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		IClientStream stream = streamConn.getStreamById(streamId);
		if (stream == null || !(stream instanceof ISubscriberStream)) {
			return;
		}
		ISubscriberStream subscriberStream = (ISubscriberStream) stream;
		subscriberStream.receiveVideo(receive);
	}

	/** {@inheritDoc} */
    public void receiveAudio(boolean receive) {
		IConnection conn = Red5.getConnectionLocal();
		if (!(conn instanceof IStreamCapableConnection)) {
			return;
		}
		IStreamCapableConnection streamConn = (IStreamCapableConnection) conn;
		int streamId = getCurrentStreamId();
		IClientStream stream = streamConn.getStreamById(streamId);
		if (stream == null || !(stream instanceof ISubscriberStream)) {
			return;
		}
		ISubscriberStream subscriberStream = (ISubscriberStream) stream;
		subscriberStream.receiveAudio(receive);
	}

	/**
     * Getter for current stream id.
     *
     * @return  Current stream id
     */
    private int getCurrentStreamId() {
		// TODO: this must come from the current connection!
		return BaseRTMPHandler.getStreamId();
	}

    /**
     * Return broadcast scope object for given scope and child scope name
     * @param scope          Scope object
     * @param name           Child scope name
     * @return               Broadcast scope
     */
    public IBroadcastScope getBroadcastScope(IScope scope, String name) {
		IBasicScope basicScope = scope.getBasicScope(IBroadcastScope.TYPE,
				name);
		if (!(basicScope instanceof IBroadcastScope)) {
			return null;
		} else {
			return (IBroadcastScope) basicScope;
		}
	}
    
    /**
     * Send a <code>NetStream.Failed</code> message to the client.
     * 
     * @param conn
     * @param description
     * @param name
     * @param streamId
     */
    private void sendNSFailed(RTMPConnection conn, String description, String name, int streamId) {
    	StreamService.sendNetStreamStatus(conn, StatusCodes.NS_FAILED, description, name, Status.ERROR, streamId);
    }    
    
	/**
	 * Send <code>NetStream.Status</code> to client (Flash Player)
	 * @param conn
	 * @param statusCode see StatusCodes class
	 * @param description
	 * @param name
	 * @param streamId
	 */    
    @SuppressWarnings("unused")
	private void sendNSStatus(IConnection conn, String statusCode, String description, String name, int streamId) {
    	StreamService.sendNetStreamStatus(conn, statusCode, description, name, Status.STATUS, streamId);
	}
    
	/**
	 * Send <code>NetStream.Status</code> to client (Flash Player)
	 *  
	 * @param conn connection
	 * @param statusCode NetStream status code
	 * @param description description
	 * @param name name
	 * @param status The status - error, warning, or status
	 * @param streamId stream id
	 */
	public static void sendNetStreamStatus(IConnection conn, String statusCode, String description, String name, String status, int streamId) {
		if (!(conn instanceof RTMPConnection)) {
			throw new RuntimeException("Connection is not RTMPConnection: " + conn);
		}

		Status s = new Status(statusCode);
		s.setClientid(streamId);
		s.setDesciption(description);
		s.setDetails(name);
		s.setLevel(status);

		Channel channel = ((RTMPConnection) conn).getChannel((byte) (4 + ((streamId - 1) * 5)));
		channel.sendStatus(s);
	}    
    
}
