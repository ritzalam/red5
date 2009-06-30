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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IScope;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IVideoStreamCodec;
import org.red5.server.api.stream.OperationNotSupportedException;
import org.red5.server.messaging.AbstractMessage;
import org.red5.server.messaging.IFilter;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IMessageInput;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IPassive;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.FrameType;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.AbstractStream.State;
import org.red5.server.stream.ITokenBucket.ITokenBucketCallback;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.ResetMessage;
import org.red5.server.stream.message.StatusMessage;
import org.slf4j.Logger;

/**
 * A play engine for playing an IPlayItem.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Dan Rossi 
 */
public final class PlayEngine implements IFilter, IPushableConsumer,
		IPipeConnectionListener, ITokenBucketCallback {

	private static final Logger log = Red5LoggerFactory.getLogger(PlayEngine.class);

	private IMessageInput msgIn;

	private IMessageOutput msgOut;

	private final PlaylistSubscriberStream playlistSubscriberStream;

	private ISchedulingService schedulingService;

	private IConsumerService consumerService;

	private IProviderService providerService;

	/**
	 * Service that controls bandwidth
	 */
	private IBWControlService bwController;

	/**
	 * Operating context for bandwidth controller
	 */
	private IBWControlContext bwContext;

	private int streamId;
	
	/**
	 * Receive video?
	 */
	private boolean receiveVideo = true;

	/**
	 * Receive audio?
	 */
	private boolean receiveAudio = true;	

	private boolean pullMode;
		
	private String waitLiveJob;

	private boolean waiting;

	/**
	 * timestamp of first sent packet
	 */
	private int streamStartTS;

	private IPlayItem currentItem;

	private ITokenBucket audioBucket;

	private ITokenBucket videoBucket;

	private RTMPMessage pendingMessage;

	private boolean waitingForToken = false;

	private boolean checkBandwidth = true;

	/**
	 * Interval in ms to check for buffer underruns in VOD streams.
	 */
	private int bufferCheckInterval = 0;	
	
	/**
	 * Number of pending messages at which a <code>NetStream.Play.InsufficientBW</code>
	 * message is generated for VOD streams.
	 */
	private int underrunTrigger = 10;	
	
	/**
	 * State machine for video frame dropping in live streams
	 */
	private IFrameDropper videoFrameDropper = new VideoFrameDropper();

	private int timestampOffset = 0;

	/**
	 * Last message sent to the client.
	 */
	private IRTMPEvent lastMessage;

	/**
	 * Number of bytes sent.
	 */
	private long bytesSent = 0;

	/**
	 * Start time of stream playback.
	 * It's not a time when the stream is being played but
	 * the time when the stream should be played if it's played
	 * from the very beginning.
	 * Eg. A stream is played at timestamp 5s on 1:00:05. The
	 * playbackStart is 1:00:00.
	 */
	private long playbackStart;

	/**
	 * Scheduled future job that makes sure messages are sent to the client.
	 */
	private volatile ScheduledFuture<?> pullAndPushFuture = null;

	/**
	 * Offset in ms the stream started.
	 */
	private int streamOffset;

	/**
	 * Timestamp when buffer should be checked for underruns next. 
	 */
	private long nextCheckBufferUnderrun;	
	
	/**
	 * Send blank audio packet next?
	 */
	private boolean sendBlankAudio;

	/**
	 * Constructs a new PlayEngine.
	 */
	private PlayEngine(Builder builder) {
		playlistSubscriberStream = builder.playlistSubscriberStream;
		schedulingService = builder.schedulingService;
		consumerService = builder.consumerService;
		providerService = builder.providerService;
		//
		streamId = playlistSubscriberStream.getStreamId();
	}

	/**
	 * Builder pattern
	 */
	public static class Builder {
		//Required for play engine
		private PlaylistSubscriberStream playlistSubscriberStream;

		//Required for play engine
		private ISchedulingService schedulingService;

		//Required for play engine
		private IConsumerService consumerService;

		//Required for play engine
		private IProviderService providerService;

		public Builder(PlaylistSubscriberStream playlistSubscriberStream,
				ISchedulingService schedulingService,
				IConsumerService consumerService,
				IProviderService providerService) {
			
			this.playlistSubscriberStream = playlistSubscriberStream;
			this.schedulingService = schedulingService;
			this.consumerService = consumerService;
			this.providerService = providerService;
		}

		public PlayEngine build() {
			return new PlayEngine(this);
		}

	}

	public void setBandwidthController(IBWControlService bwController,
			IBWControlContext bwContext) {
		this.bwController = bwController;
		this.bwContext = bwContext;
	}
	
	public void setBufferCheckInterval(int bufferCheckInterval) {
		this.bufferCheckInterval = bufferCheckInterval;
	}
	
	public void setUnderrunTrigger(int underrunTrigger) {
		this.underrunTrigger = underrunTrigger;
	}
	
	void setMessageOut(IMessageOutput msgOut) {
		this.msgOut = msgOut;
	}

	/**
	 * Start stream
	 */
	public synchronized void start() {
		if (playlistSubscriberStream.state != State.UNINIT) {
			throw new IllegalStateException();
		}
		playlistSubscriberStream.state = State.STOPPED;
		if (msgOut == null) {
			msgOut = consumerService.getConsumerOutput(playlistSubscriberStream);
			msgOut.subscribe(this, null);
		}
		audioBucket = bwController.getAudioBucket(bwContext);
		videoBucket = bwController.getVideoBucket(bwContext);
	}

	/**
	 * Play stream
	 * @param item                  Playlist item
	 * @throws StreamNotFoundException       Stream not found
	 * @throws IllegalStateException         Stream is in stopped state
	 * @throws IOException Stream had io exception
	 */
	public void play(IPlayItem item) throws StreamNotFoundException,
			IllegalStateException, IOException {
		play(item, true);
	}

	/**
	 * Play stream
	 * @param item                  Playlist item
	 * @param withReset				Send reset status before playing.
	 * @throws StreamNotFoundException       Stream not found
	 * @throws IllegalStateException         Stream is in stopped state
	 * @throws IOException Stream had IO exception
	 */
	public synchronized void play(IPlayItem item, boolean withReset)
			throws StreamNotFoundException, IllegalStateException, IOException {
		// Can't play if state is not stopped
		if (playlistSubscriberStream.state != State.STOPPED) {
			throw new IllegalStateException();
		}
		if (msgIn != null) {
			msgIn.unsubscribe(this);
			msgIn = null;
		}
		// Play type determination
		// http://livedocs.adobe.com/flex/3/langref/flash/net/NetStream.html#play%28%29
		// The start time, in seconds. Allowed values are -2, -1, 0, or a positive number. 
		// The default value is -2, which looks for a live stream, then a recorded stream, 
		// and if it finds neither, opens a live stream. 
		// If -1, plays only a live stream. 
		// If 0 or a positive number, plays a recorded stream, beginning start seconds in.
		//
		// -2: live then recorded, -1: live, >=0: recorded
		int type = (int) (item.getStart() / 1000);
		// see if it's a published stream
		IScope thisScope = playlistSubscriberStream.getScope();
		String itemName = item.getName();
		//check for input and type
		IProviderService.INPUT_TYPE sourceType = providerService.lookupProviderInput(thisScope, itemName);
		
		boolean isPublishedStream = sourceType == IProviderService.INPUT_TYPE.LIVE;
		boolean isFileStream = sourceType == IProviderService.INPUT_TYPE.VOD;
		
		boolean sendNotifications = true;

		// decision: 0 for Live, 1 for File, 2 for Wait, 3 for N/A
		int decision = 3;

		switch (type) {
			case -2:
				if (isPublishedStream) {
					decision = 0;
				} else if (isFileStream) {
					decision = 1;
				} else {
					decision = 2;
				}
				break;

			case -1:
				if (isPublishedStream) {
					decision = 0;
				} else {
					decision = 2;
				}
				break;

			default:
				if (isFileStream) {
					decision = 1;
				}
				break;
		}
		log.debug("play decision is {}", decision);
		currentItem = item;
		long itemLength = item.getLength();
		switch (decision) {
			case 0:
				//get source input without create
				msgIn = providerService.getLiveProviderInput(thisScope, itemName, false);
				// Drop all frames up to the next keyframe
				videoFrameDropper.reset(IFrameDropper.SEND_KEYFRAMES_CHECK);
				if (msgIn instanceof IBroadcastScope) {
					// Send initial keyframe
					IBroadcastStream stream = (IBroadcastStream) ((IBroadcastScope) msgIn)
							.getAttribute(IBroadcastScope.STREAM_ATTRIBUTE);
					if (stream != null && stream.getCodecInfo() != null) {
						IVideoStreamCodec videoCodec = stream.getCodecInfo()
								.getVideoCodec();
						if (videoCodec != null) {
							IoBuffer keyFrame = videoCodec.getKeyframe();
							if (keyFrame != null) {
								VideoData video = new VideoData(keyFrame);
								try {
									if (withReset) {
										sendReset();
										sendResetStatus(item);
										sendStartStatus(item);
									}

									video.setTimestamp(0);

									RTMPMessage videoMsg = new RTMPMessage();
									videoMsg.setBody(video);
									msgOut.pushMessage(videoMsg);
									sendNotifications = false;
									// Don't wait for keyframe
									videoFrameDropper.reset();
								} finally {
									video.release();
								}
							}
						}
					}
				}
				msgIn.subscribe(this, null);
				break;
			case 2:
				//get source input with create
				msgIn = providerService.getLiveProviderInput(thisScope,	itemName, true);
				msgIn.subscribe(this, null);
				waiting = true;
				if (type == -1 && itemLength >= 0) {
					// Wait given timeout for stream to be published
					waitLiveJob = schedulingService.addScheduledOnceJob(
							itemLength, new IScheduledJob() {
								public void execute(ISchedulingService service) {
									waitLiveJob = null;
									waiting = false;
									playlistSubscriberStream.onItemEnd();
								}
							});
				}
				break;
			case 1:
				msgIn = providerService.getVODProviderInput(thisScope, itemName);
				if (msgIn == null) {
					sendStreamNotFoundStatus(currentItem);
					throw new StreamNotFoundException(itemName);
				}
				if (!msgIn.subscribe(this, null)) {
					log.error("Input source subscribe failed");
				}
				break;
			default:
				sendStreamNotFoundStatus(currentItem);
				throw new StreamNotFoundException(itemName);
		}
		playlistSubscriberStream.state = State.PLAYING;
		IMessage msg = null;
		streamOffset = 0;
		streamStartTS = -1;
		if (decision == 1) {
			if (withReset) {
				releasePendingMessage();
			}
			sendVODInitCM(msgIn, item);
			// Don't use pullAndPush to detect IOExceptions prior to sending
			// NetStream.Play.Start
			if (item.getStart() > 0) {
				streamOffset = sendVODSeekCM(msgIn, (int) item.getStart());
				// We seeked to the nearest keyframe so use real timestamp now
				if (streamOffset == -1) {
					streamOffset = (int) item.getStart();
				}
			}
			msg = msgIn.pullMessage();
			if (msg instanceof RTMPMessage) {
				IRTMPEvent body = ((RTMPMessage) msg).getBody();
				if (itemLength == 0) {
					// Only send first video frame
					body = ((RTMPMessage) msg).getBody();
					while (body != null && !(body instanceof VideoData)) {
						msg = msgIn.pullMessage();
						if (msg == null) {
							break;
						}
						if (!(msg instanceof RTMPMessage)) {
							continue;
						}
						body = ((RTMPMessage) msg).getBody();
					}
				}

				if (body != null) {
					// Adjust timestamp when playing lists
					body.setTimestamp(body.getTimestamp() + timestampOffset);
				}
			}
		}
		if (sendNotifications) {
			if (withReset) {
				sendReset();
				sendResetStatus(item);
			}
			sendStartStatus(item);
			if (!withReset) {
				sendSwitchStatus();
			}
		}
		if (msg != null) {
			sendMessage((RTMPMessage) msg);
		}
		playlistSubscriberStream.notifyItemPlay(currentItem, !pullMode);
		if (withReset) {
			long currentTime = System.currentTimeMillis();
			playbackStart = currentTime - streamOffset;
			nextCheckBufferUnderrun = currentTime + bufferCheckInterval;
			if (currentItem.getLength() != 0) {
				ensurePullAndPushRunning();
			}
		}
	}

	/**
	 * Pause at position
	 * @param position                  Position in file
	 * @throws IllegalStateException    If stream is stopped
	 */
	public synchronized void pause(int position) throws IllegalStateException {
		if ((playlistSubscriberStream.state != State.PLAYING && playlistSubscriberStream.state != State.STOPPED)
				|| currentItem == null) {
			throw new IllegalStateException();
		}
		playlistSubscriberStream.state = State.PAUSED;
		releasePendingMessage();
		clearWaitJobs();
		sendClearPing();
		sendPauseStatus(currentItem);
		playlistSubscriberStream.notifyItemPause(currentItem, position);
	}

	/**
	 * Resume playback
	 * @param position                   Resumes playback
	 * @throws IllegalStateException     If stream is stopped
	 */
	public synchronized void resume(int position) throws IllegalStateException {
		if (playlistSubscriberStream.state != State.PAUSED) {
			throw new IllegalStateException();
		}
		playlistSubscriberStream.state = State.PLAYING;
		sendReset();
		sendResumeStatus(currentItem);
		if (pullMode) {
			sendVODSeekCM(msgIn, position);
			playlistSubscriberStream.notifyItemResume(currentItem, position);
			playbackStart = System.currentTimeMillis() - position;
			if (currentItem.getLength() >= 0
					&& (position - streamOffset) >= currentItem.getLength()) {
				// Resume after end of stream
				stop();
			} else {
				ensurePullAndPushRunning();
			}
		} else {
			playlistSubscriberStream.notifyItemResume(currentItem, position);
			videoFrameDropper.reset(VideoFrameDropper.SEND_KEYFRAMES_CHECK);
		}
	}

	/**
	 * Seek position in file
	 * @param position                  Position
	 * @throws IllegalStateException    If stream is in stopped state
	 * @throws OperationNotSupportedException If this object doesn't support the operation.
	 */
	public synchronized void seek(int position) throws IllegalStateException,
			OperationNotSupportedException {
		log.trace("Seek: {}", position);
		if (playlistSubscriberStream.state != State.PLAYING
				&& playlistSubscriberStream.state != State.PAUSED
				&& playlistSubscriberStream.state != State.STOPPED) {
			throw new IllegalStateException();
		}
		if (!pullMode) {
			throw new OperationNotSupportedException();
		}

		releasePendingMessage();
		clearWaitJobs();
		bwController.resetBuckets(bwContext);
		waitingForToken = false;
		sendClearPing();
		sendReset();
		sendSeekStatus(currentItem, position);
		sendStartStatus(currentItem);
		int seekPos = sendVODSeekCM(msgIn, position);
		// We seeked to the nearest keyframe so use real timestamp now
		if (seekPos == -1) {
			seekPos = position;
		}
		playbackStart = System.currentTimeMillis() - seekPos;
		log.trace("Playback start: {} seek pos: {}", playbackStart, seekPos);
		playlistSubscriberStream.notifyItemSeek(currentItem, seekPos);
		boolean messageSent = false;
		boolean startPullPushThread = false;
		if ((playlistSubscriberStream.state == State.PAUSED 
				|| playlistSubscriberStream.state == State.STOPPED)
				&& sendCheckVideoCM(msgIn)) {
			// we send a single snapshot on pause.
			// XXX we need to take BWC into account, for
			// now send forcefully.
			IMessage msg;
			try {
				msg = msgIn.pullMessage();
			} catch (Throwable err) {
				log.error("Error while pulling message", err);
				msg = null;
			}
			while (msg != null) {
				if (msg instanceof RTMPMessage) {
					RTMPMessage rtmpMessage = (RTMPMessage) msg;
					IRTMPEvent body = rtmpMessage.getBody();
					if (body instanceof VideoData
							&& ((VideoData) body).getFrameType() == FrameType.KEYFRAME) {
						body.setTimestamp(seekPos);
						doPushMessage(rtmpMessage);
						rtmpMessage.getBody().release();
						messageSent = true;
						lastMessage = body;
						break;
					}
				}

				try {
					msg = msgIn.pullMessage();
				} catch (Throwable err) {
					log.error("Error while pulling message", err);
					msg = null;
				}
			}
		} else {
			startPullPushThread = true;
		}

		if (!messageSent) {
			// Send blank audio packet to notify client about new position
			log.debug("Sending blank audio packet");
			AudioData audio = new AudioData();
			audio.setTimestamp(seekPos);
			audio.setHeader(new Header());
			audio.getHeader().setTimer(seekPos);
			audio.getHeader().setTimerRelative(false);
			RTMPMessage audioMessage = new RTMPMessage();
			audioMessage.setBody(audio);
			lastMessage = audio;
			doPushMessage(audioMessage);
		}

		if (startPullPushThread) {
			ensurePullAndPushRunning();
		}

		if (playlistSubscriberStream.state != State.STOPPED && currentItem.getLength() >= 0
				&& (position - streamOffset) >= currentItem.getLength()) {
			// Seeked after end of stream
			stop();
		}
	}

	/**
	 * Stop playback
	 * @throws IllegalStateException    If stream is in stopped state
	 */
	public synchronized void stop() throws IllegalStateException {
		if (playlistSubscriberStream.state != State.PLAYING 
				&& playlistSubscriberStream.state != State.PAUSED) {
			throw new IllegalStateException();
		}
		playlistSubscriberStream.state = State.STOPPED;
		if (msgIn != null && !pullMode) {
			msgIn.unsubscribe(this);
			msgIn = null;
		}
		playlistSubscriberStream.notifyItemStop(currentItem);
		clearWaitJobs();
		if (!playlistSubscriberStream.hasMoreItems()) {
			releasePendingMessage();
			bwController.resetBuckets(bwContext);
			waitingForToken = false;
			if (playlistSubscriberStream.getItemSize() > 0) {
				sendCompleteStatus();
			}
			bytesSent = 0;
			sendClearPing();
			sendStopStatus(currentItem);
		} else {
			if (lastMessage != null) {
				// Remember last timestamp so we can generate correct
				// headers in playlists.
				timestampOffset = lastMessage.getTimestamp();
			}
			playlistSubscriberStream.nextItem();
		}
	}

	/**
	 * Close stream
	 */
	public synchronized void close() {
		if (msgIn != null) {
			msgIn.unsubscribe(this);
			msgIn = null;
		}
		playlistSubscriberStream.state = State.CLOSED;
		clearWaitJobs();
		releasePendingMessage();
		lastMessage = null;
		sendClearPing();
	}

	/**
	 * Check if it's okay to send the client more data. This takes the configured
	 * bandwidth as well as the requested client buffer into account.
	 * 
	 * @param message
	 * @return
	 */
	private boolean okayToSendMessage(IRTMPEvent message) {
		if (!(message instanceof IStreamData)) {
			String itemName = "Undefined";
			//if current item exists get the name to help debug this issue
			if (currentItem != null) {
				itemName = currentItem.getName();
			}
			Object[] errorItems = new Object[]{message.getClass(), message.getDataType(), itemName};
			throw new RuntimeException(String.format("Expected IStreamData but got %s (type %s) for %s", errorItems));
		}
		final long now = System.currentTimeMillis();
		// check client buffer length when we've already sent some messages
		if (lastMessage != null) {
			// Duration the stream is playing / playback duration
			final long delta = now - playbackStart;
			// Buffer size as requested by the client
			final long buffer = playlistSubscriberStream.getClientBufferDuration();
			// Expected amount of data present in client buffer
			final long buffered = lastMessage.getTimestamp() - delta;
			log.debug("okayToSendMessage: timestamp {} delta {} buffered {} buffer {}",
						new Object[]{lastMessage.getTimestamp(), delta, buffered, buffer});
			//Fix for SN-122, this sends double the size of the client buffer
			if (buffer > 0 && buffered > (buffer * 2)) {
				// Client is likely to have enough data in the buffer
				return false;
			}
		}

		long pending = pendingMessages();
		if (bufferCheckInterval > 0 && now >= nextCheckBufferUnderrun) {
			if (pending > underrunTrigger) {
				// Client is playing behind speed, notify him
				sendInsufficientBandwidthStatus(currentItem);
			}
			nextCheckBufferUnderrun = now + bufferCheckInterval;
		}

		if (pending > underrunTrigger) {
			// Too many messages already queued on the connection
			return false;
		}

		IoBuffer ioBuffer = ((IStreamData) message).getData();
		if (ioBuffer != null) {
			final int size = ioBuffer.limit();
			if (message instanceof VideoData) {
				if (checkBandwidth
						&& !videoBucket.acquireTokenNonblocking(size, this)) {
					waitingForToken = true;
					return false;
				}
			} else if (message instanceof AudioData) {
				if (checkBandwidth
						&& !audioBucket.acquireTokenNonblocking(size, this)) {
					waitingForToken = true;
					return false;
				}
			}		
		}

		return true;
	}

	/**
	 * Make sure the pull and push processing is running.
	 */
	private void ensurePullAndPushRunning() {
		if (pullMode && pullAndPushFuture == null) {
			synchronized (this) {
				// client buffer is at least 100ms
				pullAndPushFuture = playlistSubscriberStream.getExecutor().scheduleWithFixedDelay(
						new PullAndPushRunnable(), 0, 10,
						TimeUnit.MILLISECONDS);
			}
		}
	}

	/**
	 * Receive then send if message is data (not audio or video)
	 */
	protected synchronized void pullAndPush() throws IOException {
		if (playlistSubscriberStream.state == State.PLAYING && pullMode && !waitingForToken) {
			if (pendingMessage != null) {
				IRTMPEvent body = pendingMessage.getBody();
				if (!okayToSendMessage(body)) {
					return;
				}
				sendMessage(pendingMessage);
				releasePendingMessage();
			} else {
				while (true) {
					IMessage msg = msgIn.pullMessage();
					if (msg == null) {
						// No more packets to send
						stop();
						break;
					} else {
						if (msg instanceof RTMPMessage) {
							RTMPMessage rtmpMessage = (RTMPMessage) msg;
							IRTMPEvent body = rtmpMessage.getBody();
							if (!receiveAudio && body instanceof AudioData) {
								// The user doesn't want to get audio packets
								((IStreamData) body).getData().free();
								if (sendBlankAudio) {
									// Send reset audio packet
									sendBlankAudio = false;
									body = new AudioData();
									// We need a zero timestamp
									if (lastMessage != null) {
										body.setTimestamp(lastMessage
												.getTimestamp()
												- timestampOffset);
									} else {
										body.setTimestamp(-timestampOffset);
									}
									rtmpMessage.setBody(body);
								} else {
									continue;
								}
							} else if (!receiveVideo
									&& body instanceof VideoData) {
								// The user doesn't want to get video packets
								((IStreamData) body).getData().free();
								continue;
							}

							// Adjust timestamp when playing lists
							body.setTimestamp(body.getTimestamp()
									+ timestampOffset);
							if (okayToSendMessage(body)) {
								log.trace("ts: {}", rtmpMessage.getBody().getTimestamp());
								sendMessage(rtmpMessage);
								((IStreamData) body).getData().free();
							} else {
								pendingMessage = rtmpMessage;
							}
							ensurePullAndPushRunning();
							break;
						}
					}
				}
			}
		}
	}

	/**
	 * Clear all scheduled waiting jobs
	 */
	private void clearWaitJobs() {
		if (pullAndPushFuture != null) {
			pullAndPushFuture.cancel(false);
			pullAndPushFuture = null;
		}
		if (waitLiveJob != null) {
			schedulingService.removeScheduledJob(waitLiveJob);
			waitLiveJob = null;
		}
	}

	/**
	 * Sends a status message.
	 * 
	 * @param status
	 */
	private void doPushMessage(Status status) {
		StatusMessage message = new StatusMessage();
		message.setBody(status);
		doPushMessage(message);
	}
	
	/**
	 * Send message to output stream and handle exceptions.
	 * 
	 * @param message The message to send.
	 */
	private void doPushMessage(AbstractMessage message) {
		try {
			msgOut.pushMessage(message);
			if (message instanceof RTMPMessage) {
				IRTMPEvent body = ((RTMPMessage) message).getBody();
				IoBuffer streamData = null;
				if (body instanceof IStreamData
						&& (streamData = ((IStreamData) body).getData()) != null) {
					bytesSent += streamData.limit();
				}
			}
		} catch (IOException err) {
			log.error("Error while pushing message", err);
		}
	}

	/**
	 * Send RTMP message
	 * @param message        RTMP message
	 */
	private void sendMessage(RTMPMessage message) {
		int ts = message.getBody().getTimestamp();
		log.debug("sendMessage: streamStartTS={}, length={}, streamOffset={}, timestamp={}",
				new Object[]{streamStartTS, currentItem.getLength(), streamOffset, ts});
		if (streamStartTS == -1) {
			log.debug("sendMessage: resetting streamStartTS");
			streamStartTS = ts;
		} else {
			if (currentItem.getLength() >= 0) {
				int duration = ts - streamStartTS;
				if (duration - streamOffset >= currentItem.getLength()) {
					// Sent enough data to client
					stop();
					return;
				}
			}
		}
		lastMessage = message.getBody();
		doPushMessage(message);
	}

	/**
	 * Send clear ping. Lets client know that stream has no more data to
	 * send.
	 */
	private void sendClearPing() {
		Ping eof = new Ping();
		eof.setEventType(Ping.STREAM_PLAYBUFFER_CLEAR);
		eof.setValue2(streamId);

		RTMPMessage eofMsg = new RTMPMessage();
		eofMsg.setBody(eof);
		doPushMessage(eofMsg);
	}

	/**
	 * Send reset message
	 */
	private void sendReset() {
		if (pullMode) {
			Ping recorded = new Ping();
			recorded.setEventType(Ping.RECORDED_STREAM);
			recorded.setValue2(streamId);

			RTMPMessage recordedMsg = new RTMPMessage();
			recordedMsg.setBody(recorded);
			doPushMessage(recordedMsg);
		}

		Ping begin = new Ping();
		begin.setEventType(Ping.STREAM_BEGIN);
		begin.setValue2(streamId);

		RTMPMessage beginMsg = new RTMPMessage();
		beginMsg.setBody(begin);
		doPushMessage(beginMsg);

		ResetMessage reset = new ResetMessage();
		doPushMessage(reset);
	}

	/**
	 * Send reset status for item
	 * @param item            Playlist item
	 */
	private void sendResetStatus(IPlayItem item) {
		Status reset = new Status(StatusCodes.NS_PLAY_RESET);
		reset.setClientid(streamId);
		reset.setDetails(item.getName());
		reset.setDesciption(String.format("Playing and resetting %s.", item.getName()));

		doPushMessage(reset);
	}

	/**
	 * Send playback start status notification
	 * @param item            Playlist item
	 */
	private void sendStartStatus(IPlayItem item) {
		Status start = new Status(StatusCodes.NS_PLAY_START);
		start.setClientid(streamId);
		start.setDetails(item.getName());
		start.setDesciption(String.format("Started playing %s.", item.getName()));

		doPushMessage(start);
	}

	/**
	 * Send playback stoppage status notification
	 * @param item            Playlist item
	 */
	private void sendStopStatus(IPlayItem item) {
		Status stop = new Status(StatusCodes.NS_PLAY_STOP);
		stop.setClientid(streamId);
		stop.setDesciption(String.format("Stopped playing %s.", item.getName()));
		stop.setDetails(item.getName());

		doPushMessage(stop);
	}

	private void sendOnPlayStatus(String code, int duration, long bytes) {
		IoBuffer buf = IoBuffer.allocate(1024);
		buf.setAutoExpand(true);
		Output out = new Output(buf);
		out.writeString("onPlayStatus");
		Map<Object, Object> props = new HashMap<Object, Object>();
		props.put("code", code);
		props.put("level", "status");
		props.put("duration", duration);
		props.put("bytes", bytes);
		out.writeMap(props, new Serializer());
		buf.flip();

		IRTMPEvent event = new Notify(buf);
		if (lastMessage != null) {
			int timestamp = lastMessage.getTimestamp();
			event.setTimestamp(timestamp);
		} else {
			event.setTimestamp(0);
		}
		RTMPMessage msg = new RTMPMessage();
		msg.setBody(event);
		doPushMessage(msg);
	}

	/**
	 * Send playlist switch status notification
	 */
	private void sendSwitchStatus() {
		// TODO: find correct duration to send
		int duration = 1;
		sendOnPlayStatus(StatusCodes.NS_PLAY_SWITCH, duration, bytesSent);
	}

	/**
	 * Send playlist complete status notification
	 *
	 */
	private void sendCompleteStatus() {
		// TODO: find correct duration to send
		int duration = 1;
		sendOnPlayStatus(StatusCodes.NS_PLAY_COMPLETE, duration, bytesSent);
	}

	/**
	 * Send seek status notification
	 * @param item            Playlist item
	 * @param position        Seek position
	 */
	private void sendSeekStatus(IPlayItem item, int position) {
		Status seek = new Status(StatusCodes.NS_SEEK_NOTIFY);
		seek.setClientid(streamId);
		seek.setDetails(item.getName());
		seek.setDesciption(String.format("Seeking %d (stream ID: %d).", position, streamId));

		doPushMessage(seek);
	}

	/**
	 * Send pause status notification
	 * @param item            Playlist item
	 */
	private void sendPauseStatus(IPlayItem item) {
		Status pause = new Status(StatusCodes.NS_PAUSE_NOTIFY);
		pause.setClientid(streamId);
		pause.setDetails(item.getName());

		doPushMessage(pause);
	}

	/**
	 * Send resume status notification
	 * @param item            Playlist item
	 */
	private void sendResumeStatus(IPlayItem item) {
		Status resume = new Status(StatusCodes.NS_UNPAUSE_NOTIFY);
		resume.setClientid(streamId);
		resume.setDetails(item.getName());

		doPushMessage(resume);
	}

	/**
	 * Send published status notification
	 * @param item            Playlist item
	 */
	private void sendPublishedStatus(IPlayItem item) {
		Status published = new Status(StatusCodes.NS_PLAY_PUBLISHNOTIFY);
		published.setClientid(streamId);
		published.setDetails(item.getName());

		doPushMessage(published);
	}

	/**
	 * Send unpublished status notification
	 * @param item            Playlist item
	 */
	private void sendUnpublishedStatus(IPlayItem item) {
		Status unpublished = new Status(StatusCodes.NS_PLAY_UNPUBLISHNOTIFY);
		unpublished.setClientid(streamId);
		unpublished.setDetails(item.getName());

		doPushMessage(unpublished);
	}

	/**
	 * Stream not found status notification
	 * @param item            Playlist item
	 */
	private void sendStreamNotFoundStatus(IPlayItem item) {
		Status notFound = new Status(StatusCodes.NS_PLAY_STREAMNOTFOUND);
		notFound.setClientid(streamId);
		notFound.setLevel(Status.ERROR);
		notFound.setDetails(item.getName());

		doPushMessage(notFound);
	}

	/**
	 * Insufficient bandwidth notification
	 * @param item            Playlist item
	 */
	private void sendInsufficientBandwidthStatus(IPlayItem item) {
		Status insufficientBW = new Status(StatusCodes.NS_PLAY_INSUFFICIENT_BW);
		insufficientBW.setClientid(streamId);
		insufficientBW.setLevel(Status.WARNING);
		insufficientBW.setDetails(item.getName());
		insufficientBW
				.setDesciption("Data is playing behind the normal speed.");

		doPushMessage(insufficientBW);
	}

	/**
	 * Send VOD init control message
	 * @param msgIn           Message input
	 * @param item            Playlist item
	 */
	private void sendVODInitCM(IMessageInput msgIn, IPlayItem item) {
		OOBControlMessage oobCtrlMsg = new OOBControlMessage();
		oobCtrlMsg.setTarget(IPassive.KEY);
		oobCtrlMsg.setServiceName("init");
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("startTS", (int) item.getStart());
		oobCtrlMsg.setServiceParamMap(paramMap);
		msgIn.sendOOBControlMessage(this, oobCtrlMsg);
	}

	/**
	 * Send VOD seek control message
	 * @param msgIn            Message input
	 * @param position         Playlist item
	 * @return                 Out-of-band control message call result or -1 on failure
	 */
	private int sendVODSeekCM(IMessageInput msgIn, int position) {
		OOBControlMessage oobCtrlMsg = new OOBControlMessage();
		oobCtrlMsg.setTarget(ISeekableProvider.KEY);
		oobCtrlMsg.setServiceName("seek");
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("position", position);
		oobCtrlMsg.setServiceParamMap(paramMap);
		msgIn.sendOOBControlMessage(this, oobCtrlMsg);
		if (oobCtrlMsg.getResult() instanceof Integer) {
			return (Integer) oobCtrlMsg.getResult();
		} else {
			return -1;
		}
	}

	/**
	 * Send VOD check video control message
	 * 
	 * @param msgIn
	 * @return
	 */
	private boolean sendCheckVideoCM(IMessageInput msgIn) {
		OOBControlMessage oobCtrlMsg = new OOBControlMessage();
		oobCtrlMsg.setTarget(IStreamTypeAwareProvider.KEY);
		oobCtrlMsg.setServiceName("hasVideo");
		msgIn.sendOOBControlMessage(this, oobCtrlMsg);
		if (oobCtrlMsg.getResult() instanceof Boolean) {
			return (Boolean) oobCtrlMsg.getResult();
		} else {
			return false;
		}
	}

	/** {@inheritDoc} */
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
			OOBControlMessage oobCtrlMsg) {
		if ("ConnectionConsumer".equals(oobCtrlMsg.getTarget())) {
			if (source instanceof IProvider) {
				msgOut.sendOOBControlMessage((IProvider) source, oobCtrlMsg);
			}
		}
	}

	/** {@inheritDoc} */
	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
			case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
				if (event.getProvider() != this) {
					if (waiting) {
						schedulingService.removeScheduledJob(waitLiveJob);
						waitLiveJob = null;
						waiting = false;
					}
					sendPublishedStatus(currentItem);
				}
				break;
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
				if (pullMode) {
					sendStopStatus(currentItem);
				} else {
					sendUnpublishedStatus(currentItem);
				}
				break;
			case PipeConnectionEvent.CONSUMER_CONNECT_PULL:
				if (event.getConsumer() == this) {
					pullMode = true;
				}
				break;
			case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
				if (event.getConsumer() == this) {
					pullMode = false;
				}
				break;
			default:
		}
	}

	/** {@inheritDoc} */
	public synchronized void pushMessage(IPipe pipe, IMessage message)
			throws IOException {
		if (message instanceof ResetMessage) {
			sendReset();
			return;
		}
		if (message instanceof RTMPMessage) {
			RTMPMessage rtmpMessage = (RTMPMessage) message;
			IRTMPEvent body = rtmpMessage.getBody();
			if (!(body instanceof IStreamData)) {
				throw new RuntimeException(String.format("Expected IStreamData but got %s (type %s)", body.getClass(), body.getDataType()));
			}

			int size = ((IStreamData) body).getData().limit();
			if (body instanceof VideoData) {
				IVideoStreamCodec videoCodec = null;
				if (msgIn instanceof IBroadcastScope) {
					IBroadcastStream stream = (IBroadcastStream) ((IBroadcastScope) msgIn)
							.getAttribute(IBroadcastScope.STREAM_ATTRIBUTE);
					if (stream != null && stream.getCodecInfo() != null) {
						videoCodec = stream.getCodecInfo().getVideoCodec();
					}
				}

				//dont try to drop frames if video codec is null - related to SN-77
				if (videoCodec != null && videoCodec.canDropFrames()) {
					if (playlistSubscriberStream.state == State.PAUSED) {
						// The subscriber paused the video
						videoFrameDropper.dropPacket(rtmpMessage);
						return;
					}

					// Only check for frame dropping if the codec supports it
					long pendingVideos = pendingVideoMessages();
					if (!videoFrameDropper.canSendPacket(rtmpMessage,
							pendingVideos)) {
						// Drop frame as it depends on other frames that were dropped before.
						return;
					}

					boolean drop = !videoBucket.acquireToken(size, 0);
					if (!receiveVideo || drop) {
						// The client disabled video or the app doesn't have enough bandwidth
						// allowed for this stream.
						videoFrameDropper.dropPacket(rtmpMessage);
						return;
					}

					if (pendingVideos > 1) {
						// We drop because the client has insufficient bandwidth.
						long now = System.currentTimeMillis();
						if (bufferCheckInterval > 0
								&& now >= nextCheckBufferUnderrun) {
							// Notify client about frame dropping (keyframe)
							sendInsufficientBandwidthStatus(currentItem);
							nextCheckBufferUnderrun = now + bufferCheckInterval;
						}
						videoFrameDropper.dropPacket(rtmpMessage);
						return;
					}

					videoFrameDropper.sendPacket(rtmpMessage);
				}
			} else if (body instanceof AudioData) {
				if (!receiveAudio && sendBlankAudio) {
					// Send blank audio packet to reset player
					sendBlankAudio = false;
					body = new AudioData();
					if (lastMessage != null) {
						body.setTimestamp(lastMessage.getTimestamp());
					} else {
						body.setTimestamp(0);
					}
					rtmpMessage.setBody(body);
				} else if (playlistSubscriberStream.state == State.PAUSED 
						|| !receiveAudio
						|| !audioBucket.acquireToken(size, 0)) {
					return;
				}
			}
			sendMessage(rtmpMessage);
		} else {
			msgOut.pushMessage(message);
		}
	}

	/** {@inheritDoc} */
	public synchronized void available(ITokenBucket bucket, long tokenCount) {
		waitingForToken = false;
		checkBandwidth = false;
		try {
			pullAndPush();
		} catch (Throwable err) {
			log.error("Error while pulling message.", err);
		}
		checkBandwidth = true;
	}

	/** {@inheritDoc} */
	public void reset(ITokenBucket bucket, long tokenCount) {
		waitingForToken = false;
	}

	/**
	 * Update bandwidth configuration
	 */
	public void updateBandwithConfigure() {
		bwController.updateBWConfigure(bwContext);
	}

	/**
	 * Get number of pending video messages
	 * @return          Number of pending video messages
	 */
	private long pendingVideoMessages() {
		OOBControlMessage pendingRequest = new OOBControlMessage();
		pendingRequest.setTarget("ConnectionConsumer");
		pendingRequest.setServiceName("pendingVideoCount");
		msgOut.sendOOBControlMessage(this, pendingRequest);
		if (pendingRequest.getResult() != null) {
			return (Long) pendingRequest.getResult();
		} else {
			return 0;
		}
	}

	/**
	 * Get number of pending messages to be sent
	 * @return          Number of pending messages
	 */
	private long pendingMessages() {
		return playlistSubscriberStream.getConnection().getPendingMessages();
	}
	
	public boolean isPullMode() {
		return pullMode;
	}
	
	public boolean isPaused() {
		return playlistSubscriberStream.state == State.PAUSED;
	}
	
	public IRTMPEvent getLastMessage() {
		return lastMessage;
	}
	
	public long getPlaybackStart() {
		return playbackStart;
	}
	
	public void sendBlankAudio(boolean sendBlankAudio) {
		this.sendBlankAudio = sendBlankAudio;
	}

	/**
	 * Returns true if the engine currently receives audio.
	 * 
	 * @return engine receives audio
	 */
	public boolean receiveAudio() {
		return receiveAudio;
	}	
	
	/**
	 * Returns true if the engine currently receives audio and
	 * sets the new value.
	 * 
	 * @param receive new value
	 * @return old value
	 */
	public boolean receiveAudio(boolean receive) {
		boolean oldValue = receiveAudio;
		//set new value
		if (receiveAudio != receive) {
			receiveAudio = receive;
		}
		return oldValue;
	}
	
	/**
	 * Returns true if the engine currently receives video.
	 * 
	 * @return receive video
	 */
	public boolean receiveVideo() {
		return receiveVideo;
	}	
	
	/**
	 * Returns true if the engine currently receives video and
	 * sets the new value.
	 * @param receive new value
	 * @return old value
	 */
	public boolean receiveVideo(boolean receive) {
		boolean oldValue = receiveVideo;
		//set new value
		if (receiveVideo != receive) {
			receiveVideo = receive;
		}
		return oldValue;
	}	
	
	/**
	 * Releases pending message body, nullifies pending message object
	 */
	private synchronized void releasePendingMessage() {
		if (pendingMessage != null) {
			IRTMPEvent body = pendingMessage.getBody();
			if (body instanceof IStreamData
					&& ((IStreamData) body).getData() != null) {
				((IStreamData) body).getData().free();
			}
			pendingMessage.setBody(null);
			pendingMessage = null;
		}
	}

	/**
	 * Periodically triggered by executor to send messages to the client.
	 */
	private class PullAndPushRunnable implements Runnable {

		/**
		 * Trigger sending of messages.
		 */
		public void run() {
			try {
				pullAndPush();
			} catch (IOException err) {
				// We couldn't get more data, stop stream.
				log.error("Error while getting message", err);
				PlayEngine.this.stop();
			}
		}

	}

}
