package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2007 by respective authors (see below). All rights reserved.
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

import static org.red5.server.api.ScopeUtils.getScopeService;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.BaseConnection;
import org.red5.server.api.IBWControllable;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IConnectionBWConfig;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ClientBW;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.ServerBW;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.IBWControlContext;
import org.red5.server.stream.IBWControlService;
import org.red5.server.stream.OutputStream;
import org.red5.server.stream.PlaylistSubscriberStream;
import org.red5.server.stream.StreamService;
import org.red5.server.stream.VideoCodecFactory;
import org.springframework.context.ApplicationContext;

/**
 * RTMP connection. Stores information about client streams, data transfer channels,
 * pending RPC calls, bandwidth configuration, used encoding (AMF0/AMF3), connection state (is alive, last
 * ping time and ping result) and session.
 */
public abstract class RTMPConnection extends BaseConnection implements
		IStreamCapableConnection, IServiceCapableConnection {
	/**
	 * Logger
	 */
	protected static Log log = LogFactory
			.getLog(RTMPConnection.class.getName());

	/**
	 * Video codec factory constant
	 */
	private static final String VIDEO_CODEC_FACTORY = "videoCodecFactory";

	// private Context context;

	/**
	 * Connection channels
	 *
	 * @see org.red5.server.net.rtmp.Channel
	 */
	private Map<Integer, Channel> channels = new ConcurrentHashMap<Integer, Channel>();

	/**
	 * Client streams
	 *
	 * @see org.red5.server.api.stream.IClientStream
	 */
	private Map<Integer, IClientStream> streams = new ConcurrentHashMap<Integer, IClientStream>();

	private Map<Integer, Boolean> reservedStreams = new ConcurrentHashMap<Integer, Boolean>();

	/**
	 * Identifier for remote calls
	 */
	protected AtomicInteger invokeId = new AtomicInteger(1);

	/**
	 * Hash map that stores pending calls and ids as pairs.
	 */
	protected Map<Integer, IPendingServiceCall> pendingCalls = new ConcurrentHashMap<Integer, IPendingServiceCall>();

	/**
	 * Deferred results set
	 *
	 * @see org.red5.server.net.rtmp.DeferredResult
	 */
	protected HashSet<DeferredResult> deferredResults = new HashSet<DeferredResult>();

	/**
	 * Last ping timestamp
	 */
	protected int lastPingTime = -1;

	/**
	 * Timestamp when last ping command was sent.
	 */
	protected long lastPingSent;

	/**
	 * Timestamp when last ping result was received.
	 */
	protected long lastPongReceived;

	/**
	 * Name of quartz job that keeps connection alive
	 */
	protected String keepAliveJobName;

	/**
	 * Ping interval in ms to detect dead clients
	 */
	protected int pingInterval = 5000;

	/**
	 * Max. time in ms after a client is disconnected because of inactivity
	 */
	protected int maxInactivity = 60000;

	/**
	 * Data read interval
	 */
	private int bytesReadInterval = 120 * 1024;

	/**
	 * Previously number of bytes read from connection.
	 */
	private long lastBytesRead = 0;

	/**
	 * Number of bytes to read next
	 */
	private int nextBytesRead = 120 * 1024;

	/**
	 * Number of bytes the client reported to have received
	 */
	private int clientBytesRead = 0;

	/**
	 * Bandwidth configure
	 */
	private IConnectionBWConfig bwConfig;

	/**
	 * Bandwidth context used by bandwidth controller
	 */
	private IBWControlContext bwContext;

	/**
	 * Map for pending video packets and stream IDs
	 */
	private Map<Integer, Integer> pendingVideos = new ConcurrentHashMap<Integer, Integer>();

	/**
	 * Number of streams used
	 */
	private int usedStreams;

	/**
	 * AMF version, AMF0 by default
	 */
	protected Encoding encoding = Encoding.AMF0;

	/**
	 * Remembered stream buffer durations
	 */
	protected Map<Integer, Integer> streamBuffers = new HashMap<Integer, Integer>();

	/**
	 * MBean object name used for de/registration purposes.
	 */
	protected ObjectName oName;

	/**
	 * Service that is waiting for handshake.
	 */
	private ISchedulingService waitForHandshakeService;
	
	/**
	 * Name of job that is waiting for a valid handshake.
	 */
	private String waitForHandshakeJob;
	
	/**
	 * Max. time in milliseconds to wait for a valid handshake.
	 */
	private int maxHandshakeTimeout = 5000;
	
	/**
	 * Creates anonymous RTMP connection without scope
	 * @param type          Connection type
	 */
	public RTMPConnection(String type) {
		// We start with an anonymous connection without a scope.
		// These parameters will be set during the call of "connect" later.
		// super(null, ""); temp fix to get things to compile
		super(type, null, null, 0, null, null, null);
	}

	@Override
	public boolean connect(IScope newScope, Object[] params) {
		boolean success = super.connect(newScope, params);
		if (success) {
			// XXX Bandwidth control service should not be bound to
			// a specific scope because it's designed to control
			// the bandwidth system-wide.
			if (getScope() != null && getScope().getContext() != null) {
				IBWControlService bwController = (IBWControlService) getScope()
						.getContext().getBean(IBWControlService.KEY);
				bwContext = bwController.registerBWControllable(this);
			}
			
			if (waitForHandshakeJob != null) {
				waitForHandshakeService.removeScheduledJob(waitForHandshakeJob);
				waitForHandshakeJob = null;
				waitForHandshakeService = null;
			}
		}
		return success;
	}

	/**
	 * Initialize connection
	 *
	 * @param host             Connection host
	 * @param path             Connection path
	 * @param sessionId        Connection session id
	 * @param params           Params passed from client
	 */
	public void setup(String host, String path, String sessionId,
			Map<String, Object> params) {
		this.host = host;
		this.path = path;
		this.sessionId = sessionId;
		this.params = params;
		if (params.get("objectEncoding") == Integer.valueOf(3)) {
			encoding = Encoding.AMF3;
		}
	}

	/**
	 * Return AMF protocol encoding used by this connection
	 * @return                  AMF encoding used by connection
	 */
	public Encoding getEncoding() {
		return encoding;
	}

	/**
	 * Getter for  next available channel id
	 *
	 * @return  Next available channel id
	 */
	public synchronized int getNextAvailableChannelId() {
		int result = 4;
		while (isChannelUsed(result))
			result++;
		return result;
	}

	/**
	 * Checks whether channel is used
	 * @param channelId        Channel id
	 * @return                 <code>true</code> if channel is in use, <code>false</code> otherwise
	 */
	public boolean isChannelUsed(int channelId) {
		return channels.get(channelId) != null;
	}

	/**
	 * Return channel by id
	 * @param channelId        Channel id
	 * @return                 Channel by id
	 */
	public Channel getChannel(int channelId) {
		synchronized (channels) {
			Channel result = channels.get(channelId);
			if (result == null) {
				result = new Channel(this, channelId);
				channels.put(channelId, result);
			}
			return result;
		}
	}

	/**
	 * Closes channel
	 * @param channelId       Channel id
	 */
	public void closeChannel(int channelId) {
		channels.remove(channelId);
	}

	/**
	 * Getter for client streams
	 *
	 * @return  Client streams as array
	 */
	protected Collection<IClientStream> getStreams() {
		return streams.values();
	}

	/** {@inheritDoc} */
	public int reserveStreamId() {
		int result = -1;
		synchronized (reservedStreams) {
			for (int i = 0; true; i++) {
				Boolean value = reservedStreams.get(i);
				if (value == null || !value) {
					reservedStreams.put(i, true);
					result = i;
					break;
				}
			}
		}
		return result + 1;
	}

	/**
	 * Creates output stream object from stream id. Output stream consists of audio, data and video channels.
	 *
	 * @see   org.red5.server.stream.OutputStream
	 * @param streamId          Stream id
	 * @return                  Output stream object
	 */
	public OutputStream createOutputStream(int streamId) {
		int channelId = (4 + ((streamId - 1) * 5));
		final Channel data = getChannel(channelId++);
		final Channel video = getChannel(channelId++);
		final Channel audio = getChannel(channelId++);
		// final Channel unknown = getChannel(channelId++);
		// final Channel ctrl = getChannel(channelId++);
		return new OutputStream(video, audio, data);
	}

	/**
	 * Getter for  video codec factory
	 *
	 * @return  Video codec factory
	 */
	public VideoCodecFactory getVideoCodecFactory() {
		final IContext context = scope.getContext();
		ApplicationContext appCtx = context.getApplicationContext();
		if (!appCtx.containsBean(VIDEO_CODEC_FACTORY)) {
			return null;
		}

		return (VideoCodecFactory) appCtx.getBean(VIDEO_CODEC_FACTORY);
	}

	/** {@inheritDoc} */
	public IClientBroadcastStream newBroadcastStream(int streamId) {
		Boolean value = reservedStreams.get(streamId - 1);
		if (value == null || !value) {
			// StreamId has not been reserved before
			return null;
		}

		synchronized (streams) {
			if (streams.get(streamId - 1) != null) {
				// Another stream already exists with this id
				return null;
			}
			ApplicationContext appCtx = scope.getContext()
					.getApplicationContext();
			ClientBroadcastStream cbs = (ClientBroadcastStream) appCtx
					.getBean("clientBroadcastStream");
			/**
			 * Picking up the ClientBroadcastStream defined as a spring prototype
			 * in red5-common.xml
			 */
			Integer buffer = streamBuffers.get(streamId - 1);
			if (buffer != null)
				cbs.setClientBufferDuration(buffer);
			cbs.setStreamId(streamId);
			cbs.setConnection(this);
			cbs.setName(createStreamName());
			cbs.setScope(this.getScope());

			streams.put(streamId - 1, cbs);
			usedStreams++;
			return cbs;
		}
	}

	/** {@inheritDoc}
	 * To be implemented.
	 */
	public ISingleItemSubscriberStream newSingleItemSubscriberStream(
			int streamId) {
		// TODO implement it
		return null;
	}

	/** {@inheritDoc} */
	public IPlaylistSubscriberStream newPlaylistSubscriberStream(int streamId) {
		Boolean value = reservedStreams.get(streamId - 1);
		if (value == null || !value) {
			// StreamId has not been reserved before
			return null;
		}

		synchronized (streams) {
			if (streams.get(streamId - 1) != null) {
				// Another stream already exists with this id
				return null;
			}
			ApplicationContext appCtx = scope.getContext()
					.getApplicationContext();
			/**
			 * Picking up the PlaylistSubscriberStream defined as a spring prototype
			 * in red5-common.xml
			 */
			PlaylistSubscriberStream pss = (PlaylistSubscriberStream) appCtx
					.getBean("playlistSubscriberStream");
			Integer buffer = streamBuffers.get(streamId - 1);
			if (buffer != null)
				pss.setClientBufferDuration(buffer);
			pss.setName(createStreamName());
			pss.setConnection(this);
			pss.setScope(this.getScope());
			pss.setStreamId(streamId);
			streams.put(streamId - 1, pss);
			usedStreams++;
			return pss;
		}
	}

	/**
	 * Getter for used stream count
	 *
	 * @return Value for property 'usedStreamCount'.
	 */
	protected int getUsedStreamCount() {
		return usedStreams;
	}

	/** {@inheritDoc} */
	public IClientStream getStreamById(int id) {
		if (id <= 0) {
			return null;
		}

		return streams.get(id - 1);
	}

	/**
	 * Return stream id for given channel id
	 * @param channelId        Channel id
	 * @return                 ID of stream that channel belongs to
	 */
	public int getStreamIdForChannel(int channelId) {
		if (channelId < 4) {
			return 0;
		}

		return ((channelId - 4) / 5) + 1;
	}

	/**
	 * Return stream for given channel id
	 * @param channelId        Channel id
	 * @return                 Stream that channel belongs to
	 */
	public IClientStream getStreamByChannelId(int channelId) {
		if (channelId < 4) {
			return null;
		}

		return streams.get(getStreamIdForChannel(channelId) - 1);
	}

	/** {@inheritDoc} */
	@Override
	public void close() {
		if (keepAliveJobName != null) {
			ISchedulingService schedulingService = (ISchedulingService) getScope()
					.getContext().getBean(ISchedulingService.BEAN_NAME);
			schedulingService.removeScheduledJob(keepAliveJobName);
			keepAliveJobName = null;
		}
		Red5.setConnectionLocal(this);
		IStreamService streamService = (IStreamService) getScopeService(scope,
				IStreamService.class, StreamService.class);
		if (streamService != null) {
			synchronized (streams) {
				for (Map.Entry<Integer, IClientStream> entry : streams
						.entrySet()) {
					IClientStream stream = entry.getValue();
					if (stream != null) {
						if (log.isDebugEnabled()) {
							log
									.debug("Closing stream: "
											+ stream.getStreamId());
						}
						streamService.deleteStream(this, stream.getStreamId());
						usedStreams--;
					}
				}
				streams.clear();
			}
		}
		channels.clear();

		if (bwContext != null && getScope() != null
				&& getScope().getContext() != null) {
			IBWControlService bwController = (IBWControlService) getScope()
					.getContext().getBean(IBWControlService.KEY);
			bwController.unregisterBWControllable(bwContext);
			bwContext = null;
		}
		super.close();
	}

	/** {@inheritDoc} */
	public void unreserveStreamId(int streamId) {
		deleteStreamById(streamId);
		if (streamId > 0) {
			reservedStreams.remove(streamId - 1);
		}
	}

	/** {@inheritDoc} */
	public void deleteStreamById(int streamId) {
		if (streamId > 0) {
			if (streams.get(streamId - 1) != null) {
				synchronized (pendingVideos) {
					pendingVideos.remove(streamId);
				}
				usedStreams--;
				streams.remove(streamId - 1);
				streamBuffers.remove(streamId - 1);
			}
		}
	}

	/**
	 * Handler for ping event
	 * @param ping        Ping event context
	 */
	public void ping(Ping ping) {
		getChannel((byte) 2).write(ping);
	}

	/**
	 * Write raw byte buffer
	 * @param out           Byte buffer
	 */
	public abstract void rawWrite(ByteBuffer out);

	/**
	 * Write packet
	 * @param out           Packet
	 */
	public abstract void write(Packet out);

	/**
	 * Update number of bytes to read next value
	 */
	protected void updateBytesRead() {
		long bytesRead = getReadBytes();
		if (bytesRead >= nextBytesRead) {
			BytesRead sbr = new BytesRead((int) bytesRead);
			getChannel((byte) 2).write(sbr);
			log.info(sbr);
			nextBytesRead += bytesReadInterval;
		}
	}

	/**
	 * Read number of recieved bytes
	 * @param bytes                Number of bytes
	 */
	public void receivedBytesRead(int bytes) {
		log.info("Client received " + bytes + " bytes, written "
				+ getWrittenBytes() + " bytes, " + getPendingMessages()
				+ " messages pending");
		clientBytesRead = bytes;
	}

	/**
	 * Get number of bytes the client reported to have received.
	 * 
	 * @return number of bytes
	 */
	public int getClientBytesRead() {
		return clientBytesRead;
	}

	/** {@inheritDoc} */
	public void invoke(IServiceCall call) {
		invoke(call, (byte) 3);
	}

	/**
	 * Generate next invoke id
	 *
	 * @return  Next invoke id for RPC
	 */
	protected int getInvokeId() {
		return invokeId.incrementAndGet();
	}

	/**
	 * Register pending call (remote function call that is yet to finish)
	 * @param invokeId             Deferred operation id
	 * @param call                 Call service
	 */
	protected void registerPendingCall(int invokeId, IPendingServiceCall call) {
		pendingCalls.put(invokeId, call);
	}

	/** {@inheritDoc} */
	public void invoke(IServiceCall call, byte channel) {
		// We need to use Invoke for all calls to the client
		Invoke invoke = new Invoke();
		invoke.setCall(call);
		invoke.setInvokeId(getInvokeId());
		if (call instanceof IPendingServiceCall) {
			registerPendingCall(invoke.getInvokeId(),
					(IPendingServiceCall) call);
		}
		getChannel(channel).write(invoke);
	}

	/** {@inheritDoc} */
	public void invoke(String method) {
		invoke(method, null, null);
	}

	/** {@inheritDoc} */
	public void invoke(String method, Object[] params) {
		invoke(method, params, null);
	}

	/** {@inheritDoc} */
	public void invoke(String method, IPendingServiceCallback callback) {
		invoke(method, null, callback);
	}

	/** {@inheritDoc} */
	public void invoke(String method, Object[] params,
			IPendingServiceCallback callback) {
		IPendingServiceCall call = new PendingCall(method, params);
		if (callback != null) {
			call.registerCallback(callback);
		}

		invoke(call);
	}

	/** {@inheritDoc} */
	public void notify(IServiceCall call) {
		notify(call, (byte) 3);
	}

	/** {@inheritDoc} */
	public void notify(IServiceCall call, byte channel) {
		Notify notify = new Notify();
		notify.setCall(call);
		getChannel(channel).write(notify);
	}

	/** {@inheritDoc} */
	public void notify(String method) {
		notify(method, null);
	}

	/** {@inheritDoc} */
	public void notify(String method, Object[] params) {
		IServiceCall call = new Call(method, params);
		notify(call);
	}

	/** {@inheritDoc} */
	public IBandwidthConfigure getBandwidthConfigure() {
		return bwConfig;
	}

	/** {@inheritDoc} */
	public IBWControllable getParentBWControllable() {
		// TODO return the client object
		return null;
	}

	/** {@inheritDoc} */
	public void setBandwidthConfigure(IBandwidthConfigure config) {
		if (!(config instanceof IConnectionBWConfig)) {
			return;
		}

		this.bwConfig = (IConnectionBWConfig) config;
		// Notify client about new bandwidth settings (in bytes per second)
		if (bwConfig.getDownstreamBandwidth() > 0) {
			ServerBW serverBW = new ServerBW((int) bwConfig
					.getDownstreamBandwidth() / 8);
			getChannel((byte) 2).write(serverBW);
		}
		if (bwConfig.getUpstreamBandwidth() > 0) {
			ClientBW clientBW = new ClientBW((int) bwConfig
					.getUpstreamBandwidth() / 8, (byte) 0);
			getChannel((byte) 2).write(clientBW);
			// Update generation of BytesRead messages
			// TODO: what are the correct values here?
			bytesReadInterval = (int) bwConfig.getUpstreamBandwidth() / 8;
			nextBytesRead = (int) getWrittenBytes();
		}
		if (bwContext != null) {
			IBWControlService bwController = (IBWControlService) getScope()
					.getContext().getBean(IBWControlService.KEY);
			bwController.updateBWConfigure(bwContext);
		}
	}

	/** {@inheritDoc} */
	@Override
	public long getReadBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	/** {@inheritDoc} */
	@Override
	public long getWrittenBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Get pending call service by id
	 * @param invokeId               Pending call service id
	 * @return                       Pending call service object
	 */
	protected IPendingServiceCall getPendingCall(int invokeId) {
		return pendingCalls.remove(invokeId);
	}

	/**
	 * Generates new stream name
	 * @return       New stream name
	 */
	protected String createStreamName() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Mark message as being written.
	 *
	 * @param message        Message to mark
	 */
	protected void writingMessage(Packet message) {
		if (message.getMessage() instanceof VideoData) {
			int streamId = message.getHeader().getStreamId();
			synchronized (pendingVideos) {
				Integer old = pendingVideos.get(streamId);
				if (old == null) {
					old = Integer.valueOf(0);
				}
				pendingVideos.put(streamId, old + 1);
			}
		}
	}

	/**
	 * Increases number of read messages by one. Updates number of bytes read.
	 */
	protected void messageReceived() {
		readMessages++;

		// Trigger generation of BytesRead messages
		updateBytesRead();
	}

	/**
	 * Mark message as sent.
	 *
	 * @param message           Message to mark
	 */
	protected void messageSent(Packet message) {
		if (message.getMessage() instanceof VideoData) {
			int streamId = message.getHeader().getStreamId();
			synchronized (pendingVideos) {
				Integer pending = pendingVideos.get(streamId);
				if (pending != null) {
					pendingVideos.put(streamId, pending - 1);
				}
			}
		}

		writtenMessages++;
	}

	/**
	 * Increases number of dropped messages
	 */
	protected void messageDropped() {
		droppedMessages++;
	}

	/** {@inheritDoc} */
	@Override
	public long getPendingVideoMessages(int streamId) {
		Integer count = pendingVideos.get(streamId);
		long result = (count != null ? count.intValue()
				- getUsedStreamCount() : 0);
		return (result > 0 ? result : 0);
	}

	/** {@inheritDoc} */
	public void ping() {
		Ping pingRequest = new Ping();
		pingRequest.setValue1((short) Ping.PING_CLIENT);
		lastPingSent = System.currentTimeMillis();
		int now = (int) (lastPingSent & 0xffffffff);
		pingRequest.setValue2(now);
		pingRequest.setValue3(Ping.UNDEFINED);
		ping(pingRequest);
	}

	/**
	 * Marks that pingback was recieved
	 * @param pong            Ping object
	 */
	protected void pingReceived(Ping pong) {
		lastPongReceived = System.currentTimeMillis();
		int now = (int) (lastPongReceived & 0xffffffff);
		lastPingTime = now - pong.getValue2();
	}

	/** {@inheritDoc} */
	public int getLastPingTime() {
		return lastPingTime;
	}

	/**
	 * Setter for ping interval
	 *
	 * @param pingInterval Interval in ms to ping clients. Set to <code>0</code> to disable ghost detection code.
	 */
	public void setPingInterval(int pingInterval) {
		this.pingInterval = pingInterval;
	}

	/**
	 * Setter for max. inactivity
	 * 
	 * @param maxInactivity Max. time in ms after which a client is disconnected in case of inactivity.
	 */
	public void setMaxInactivity(int maxInactivity) {
		this.maxInactivity = maxInactivity;
	}

	/**
	 * Starts measurement
	 */
	public void startRoundTripMeasurement() {
		if (pingInterval <= 0)
			// Ghost detection code disabled
			return;

		ISchedulingService schedulingService = (ISchedulingService) getScope()
				.getContext().getBean(ISchedulingService.BEAN_NAME);
		IScheduledJob keepAliveJob = new KeepAliveJob();
		keepAliveJobName = schedulingService.addScheduledJob(pingInterval,
				keepAliveJob);
	}

	/**
	 * Inactive state event handler
	 */
	protected abstract void onInactive();

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return getClass().getSimpleName() + " from " + getRemoteAddress() + ':'
				+ getRemotePort() + " to " + getHost() + " (in: "
				+ getReadBytes() + ", out: " + getWrittenBytes() + ')';
	}

	/**
	 * Registers deffered result
	 * @param result            Result to register
	 */
	protected void registerDeferredResult(DeferredResult result) {
		deferredResults.add(result);
	}

	/**
	 * Unregister deffered result
	 * @param result             Result to unregister
	 */
	protected void unregisterDeferredResult(DeferredResult result) {
		deferredResults.remove(result);
	}

	protected void rememberStreamBufferDuration(int streamId, int bufferDuration) {
		streamBuffers.put(streamId - 1, bufferDuration);
	}

	/**
	 * Set max. time to wait for valid handshake in milliseconds.
	 * @param maxHandshakeTimeout max. time in milliseconds
	 */
	public void setMaxHandshakeTimeout(int maxHandshakeTimeout) {
		this.maxHandshakeTimeout = maxHandshakeTimeout;
	}
	
	/**
	 * Start waiting for a valid handshake.
	 * 
	 * @param service		the scheduling service to use
	 */
	protected void startWaitForHandshake(ISchedulingService service) {
		waitForHandshakeService = service;
		waitForHandshakeJob = service.addScheduledOnceJob(maxHandshakeTimeout, new WaitForHandshakeJob());
	}
	
	/**
	 * Quartz job that keeps connection alive and disconnects if client is dead.
	 */
	private class KeepAliveJob implements IScheduledJob {

		/** {@inheritDoc} */
		public void execute(ISchedulingService service) {
			long thisRead = getReadBytes();
			if (thisRead > lastBytesRead) {
				// Client sent data since last check and thus is not dead. No need to ping.
				lastBytesRead = thisRead;
				return;
			}

			if (lastPingSent - lastPongReceived > maxInactivity) {
				// Client didn't send response to ping command for too long, disconnect
				service.removeScheduledJob(keepAliveJobName);
				keepAliveJobName = null;
				log.warn("Closing " + RTMPConnection.this
						+ " due to too much inactivity ("
						+ (lastPingSent - lastPongReceived) + ").");
				onInactive();
				return;
			}

			// Send ping command to client to trigger sending of data.
			ping();
		}
	}
	
	/**
	 * Quartz job that waits for a valid handshake and disconnects the client if
	 * none is received.
	 */
	private class WaitForHandshakeJob implements IScheduledJob {
		
		/** {@inheritDoc} */
		public void execute(ISchedulingService service) {
			waitForHandshakeJob = null;
			waitForHandshakeService = null;
			// Client didn't send a valid handshake, disconnect.
			onInactive();
		}
		
	}
	
}
