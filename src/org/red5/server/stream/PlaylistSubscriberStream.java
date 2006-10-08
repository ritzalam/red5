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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistController;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.IVideoStreamCodec;
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
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.FrameType;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.ITokenBucket.ITokenBucketCallback;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.ResetMessage;
import org.red5.server.stream.message.StatusMessage;

public class PlaylistSubscriberStream extends AbstractClientStream implements
		IPlaylistSubscriberStream {
	private static final Log log = LogFactory
			.getLog(PlaylistSubscriberStream.class);

	private enum State {
		UNINIT, STOPPED, PLAYING, PAUSED, CLOSED
	}

	private IPlaylistController controller;

	private IPlaylistController defaultController;

	private List<IPlayItem> items;

	private int currentItemIndex;

	private PlayEngine engine;

	private IFlowControlService flowControlService;

	private StreamFlowController streamFlowController = new StreamFlowController();

	private boolean isRewind = false;

	private boolean isRandom = false;

	private boolean isRepeat = false;

	private boolean receiveVideo = true;

	private boolean receiveAudio = true;

	public PlaylistSubscriberStream() {
		defaultController = new SimplePlaylistController();
		items = new ArrayList<IPlayItem>();
		engine = new PlayEngine();
		currentItemIndex = 0;
	}

	public void start() {
		flowControlService = (IFlowControlService) getScope().getContext()
				.getBean(IFlowControlService.KEY);
		engine.start();
		notifySubscriberStart();
	}

	public void play() {
		synchronized (items) {
			if (items.size() == 0) {
				return;
			}
			if (currentItemIndex == -1) {
				moveToNext();
			}
			IPlayItem item = items.get(currentItemIndex);
			int count = items.size();
			while (count-- > 0) {
				try {
					engine.play(item);
					break;
				} catch (StreamNotFoundException e) {
					// go for next item
					moveToNext();
					if (currentItemIndex == -1) {
						// we reaches the end.
						break;
					}
					item = items.get(currentItemIndex);
				} catch (IllegalStateException e) {
					// an stream is already playing
					break;
				}
			}
		}
	}

	public void pause(int position) {
		try {
			engine.pause(position);
		} catch (IllegalStateException e) {
		}
	}

	public void resume(int position) {
		try {
			engine.resume(position);
		} catch (IllegalStateException e) {
		}
	}

	public void stop() {
		try {
			engine.stop();
		} catch (IllegalStateException e) {
		}
	}

	public void seek(int position) {
		try {
			engine.seek(position);
		} catch (IllegalStateException e) {
		}
	}

	public void close() {
		engine.close();
		flowControlService.releaseFlowControllable(this);
		notifySubscriberClose();
	}

	public boolean isPaused() {
		return (engine.state == State.PAUSED);
	}

	public void addItem(IPlayItem item) {
		synchronized (items) {
			items.add(item);
		}
	}

	public void addItem(IPlayItem item, int index) {
		synchronized (items) {
			items.add(index, item);
		}
	}

	public void removeItem(int index) {
		synchronized (items) {
			if (index < 0 || index >= items.size()) {
				return;
			}
			int originSize = items.size();
			items.remove(index);
			if (currentItemIndex == index) {
				// set the next item.
				if (index == originSize - 1) {
					currentItemIndex = index - 1;
				}
			}
		}
	}

	public void removeAllItems() {
		synchronized (items) {
			// we try to stop the engine first
			stop();
			items.clear();
		}
	}

	public void previousItem() {
		synchronized (items) {
			stop();
			moveToPrevious();
			if (currentItemIndex == -1) {
				return;
			}
			IPlayItem item = items.get(currentItemIndex);
			int count = items.size();
			while (count-- > 0) {
				try {
					engine.play(item);
					break;
				} catch (StreamNotFoundException e) {
					// go for next item
					moveToPrevious();
					if (currentItemIndex == -1) {
						// we reaches the end.
						break;
					}
					item = items.get(currentItemIndex);
				} catch (IllegalStateException e) {
					// an stream is already playing
					break;
				}
			}
		}
	}

	public void nextItem() {
		synchronized (items) {
			stop();
			moveToNext();
			boolean needPause = false;
			if (currentItemIndex == -1) {
				if (items.size() > 0) {
					// move to the head of the list and pause at the beginning
					moveToNext();
					if (currentItemIndex >= 0) {
						needPause = true;
					} else return;
				} else return;
			}
			IPlayItem item = items.get(currentItemIndex);
			int count = items.size();
			while (count-- > 0) {
				try {
					engine.play(item);
					if (needPause) {
						engine.pause(0);
					}
					break;
				} catch (StreamNotFoundException e) {
					// go for next item
					moveToNext();
					if (currentItemIndex == -1) {
						// we reaches the end.
						break;
					}
					item = items.get(currentItemIndex);
				} catch (IllegalStateException e) {
					// an stream is already playing
					break;
				}
			}
		}
	}

	public void setItem(int index) {
		synchronized (items) {
			if (index < 0 || index >= items.size()) {
				return;
			}
			stop();
			currentItemIndex = index;
			IPlayItem item = items.get(currentItemIndex);
			try {
				engine.play(item);
			} catch (StreamNotFoundException e) {
				// let the engine retain the STOPPED state
				// and wait for control from outside
			} catch (IllegalStateException e) {

			}
		}
	}

	public boolean isRandom() {
		return isRandom;
	}

	public void setRandom(boolean random) {
		isRandom = random;
	}

	public boolean isRewind() {
		return isRewind;
	}

	public void setRewind(boolean rewind) {
		isRewind = rewind;
	}

	public boolean isRepeat() {
		return isRepeat;
	}

	public void setRepeat(boolean repeat) {
		isRepeat = repeat;
	}

	public void receiveVideo(boolean receive) {
		receiveVideo = receive;
	}

	public void receiveAudio(boolean receive) {
		receiveAudio = receive;
	}

	public void setPlaylistController(IPlaylistController controller) {
		this.controller = controller;
	}

	public int getItemSize() {
		return items.size();
	}

	public int getCurrentItemIndex() {
		return currentItemIndex;
	}

	public IPlayItem getItem(int index) {
		try {
			return items.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public void setBandwidthConfigure(IBandwidthConfigure config) {
		super.setBandwidthConfigure(config);
		engine.updateBandwithConfigure();
	}

	/**
	 * Notified by RTMPHandler when a message has been sent.
	 * Glue for old code base.
	 * @param message
	 */
	public void written(Object message) {
		engine.pullAndPush();
	}

	/**
	 * Move the current item to the next in list.
	 */
	private void moveToNext() {
		if (controller != null) {
			currentItemIndex = controller.nextItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.nextItem(this,
					currentItemIndex);
		}
	}

	/**
	 * Move the current item to the previous in list.
	 */
	private void moveToPrevious() {
		if (controller != null) {
			currentItemIndex = controller.previousItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.previousItem(this,
					currentItemIndex);
		}
	}

	/**
	 * Notified by the play engine when the current item reaches the end.
	 */
	private void onItemEnd() {
		nextItem();
	}

	private void notifySubscriberStart() {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamSubscriberStart(this);
			} catch (Throwable t) {
				log.error("error notify streamSubscriberStart", t);
			}
		}
	}

	private void notifySubscriberClose() {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamSubscriberClose(this);
			} catch (Throwable t) {
				log.error("error notify streamSubscriberClose", t);
			}
		}
	}

	private void notifyItemPlay(IPlayItem item, boolean isLive) {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamPlaylistItemPlay(this, item, isLive);
			} catch (Throwable t) {
				log.error("error notify streamPlaylistItemPlay", t);
			}
		}
	}

	private void notifyItemStop(IPlayItem item) {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamPlaylistItemStop(this, item);
			} catch (Throwable t) {
				log.error("error notify streamPlaylistItemStop", t);
			}
		}
	}

	private void notifyItemPause(IPlayItem item, int position) {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamPlaylistVODItemPause(this, item, position);
			} catch (Throwable t) {
				log.error("error notify streamPlaylistVODItemPause", t);
			}
		}
	}

	private void notifyItemResume(IPlayItem item, int position) {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamPlaylistVODItemResume(this, item, position);
			} catch (Throwable t) {
				log.error("error notify streamPlaylistVODItemResume", t);
			}
		}
	}

	private void notifyItemSeek(IPlayItem item, int position) {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamPlaylistVODItemSeek(this, item, position);
			} catch (Throwable t) {
				log.error("error notify streamPlaylistVODItemSeek", t);
			}
		}
	}

	/**
	 * A play engine for playing an IPlayItem.
	 */
	private class PlayEngine implements IFilter, IPushableConsumer,
			IPipeConnectionListener, ITokenBucketCallback, IScheduledJob {

		private State state;

		private IMessageInput msgIn;

		private IMessageOutput msgOut;

		private boolean isPullMode = false;

		private ISchedulingService schedulingService = null;

		private String waitLiveJob;

		private String playLengthJob;

		private String waitStopJob;

		private String adaptFlowJob;

		private boolean isWaiting = false;

		private int vodStartTS = 0;

		private IPlayItem currentItem;

		private ITokenBucket audioBucket;

		private ITokenBucket videoBucket;

		private RTMPMessage pendingMessage;

		private boolean isWaitingForToken = false;

		// State machine for video frame dropping in live streams
		private IFrameDropper videoFrameDropper = new VideoFrameDropper();

		public PlayEngine() {
			state = State.UNINIT;
		}

		synchronized public void start() {
			if (state != State.UNINIT) {
				throw new IllegalStateException();
			}
			state = State.STOPPED;
			schedulingService = (ISchedulingService) getScope().getContext()
					.getBean(ISchedulingService.SCHEDULING_SERVICE);
			IConsumerService consumerManager = (IConsumerService) getScope()
					.getContext().getBean(IConsumerService.KEY);
			msgOut = consumerManager
					.getConsumerOutput(PlaylistSubscriberStream.this);
			msgOut.subscribe(this, null);
			audioBucket = flowControlService
					.getAudioTokenBucket(PlaylistSubscriberStream.this);
			videoBucket = flowControlService
					.getVideoTokenBucket(PlaylistSubscriberStream.this);
		}

		synchronized public void play(IPlayItem item)
				throws StreamNotFoundException, IllegalStateException {
			if (state != State.STOPPED) {
				throw new IllegalStateException();
			}
			int type = (int) (item.getStart() / 1000);
			// see if it's a published stream
			IScope thisScope = getScope();
			IContext context = thisScope.getContext();
			IProviderService providerService = (IProviderService) context
					.getBean(IProviderService.KEY);
			IMessageInput liveInput = providerService.getLiveProviderInput(
					thisScope, item.getName(), false);
			IMessageInput vodInput = providerService.getVODProviderInput(
					thisScope, item.getName());
			boolean isPublishedStream = liveInput != null;
			boolean isFileStream = vodInput != null;
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
			if (decision == 2) {
				liveInput = providerService.getLiveProviderInput(thisScope,
						item.getName(), true);
			}
			currentItem = item;
			switch (decision) {
				case 0:
					msgIn = liveInput;
					// Drop all frames up to the next keyframe
					videoFrameDropper.reset(IFrameDropper.SEND_KEYFRAMES_CHECK);
					if (msgIn instanceof IBroadcastScope) {
						// Send initial keyframe
						IClientBroadcastStream stream = (IClientBroadcastStream) ((IBroadcastScope) msgIn)
								.getAttribute(IBroadcastScope.STREAM_ATTRIBUTE);
						if (stream != null && stream.getCodecInfo() != null) {
							IVideoStreamCodec videoCodec = stream
									.getCodecInfo().getVideoCodec();
							if (videoCodec != null) {
								ByteBuffer keyFrame = videoCodec.getKeyframe();
								if (keyFrame != null) {
									VideoData video = new VideoData(keyFrame);
									try {
										sendReset();
										//sendBlankAudio(0);
										//sendBlankVideo(0);
										sendResetStatus(item);
										sendStartStatus(item);

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
					if (item.getLength() >= 0) {
						playLengthJob = schedulingService.addScheduledOnceJob(
								item.getLength(), this);
					}
					break;
				case 2:
					msgIn = liveInput;
					msgIn.subscribe(this, null);
					isWaiting = true;
					if (type == -1 && item.getLength() >= 0) {
						// Wait given timeout for stream to be published
						waitLiveJob = schedulingService.addScheduledOnceJob(
								item.getLength(), new IScheduledJob() {
									public void execute(
											ISchedulingService service) {
										waitLiveJob = null;
										isWaiting = false;
										onItemEnd();
									}
								});
					}
					break;
				case 1:
					msgIn = vodInput;
					msgIn.subscribe(this, null);
					if (item.getLength() >= 0) {
						playLengthJob = schedulingService.addScheduledOnceJob(
								item.getLength(), this);
					}
					break;
				default:
					sendStreamNotFoundStatus(currentItem);
					throw new StreamNotFoundException(item.getName());
			}
			if (sendNotifications) {
				sendReset();
				sendResetStatus(item);
				sendStartStatus(item);
			}
			state = State.PLAYING;
			if (decision == 1) {
				releasePendingMessage();
				sendVODInitCM(msgIn, item);
				vodStartTS = -1;
				pullAndPush();
			}
			notifyItemPlay(currentItem, !isPullMode);
		}

		synchronized public void pause(int position)
				throws IllegalStateException {
			if (state != State.PLAYING) {
				throw new IllegalStateException();
			}
			if (isPullMode) {
				state = State.PAUSED;
				getStreamFlow().pause();
				releasePendingMessage();
				clearWaitJobs();
				sendClearPing();
				sendPauseStatus(currentItem);
				notifyItemPause(currentItem, position);
			}
		}

		synchronized public void resume(int position)
				throws IllegalStateException {
			if (state != State.PAUSED) {
				throw new IllegalStateException();
			}
			if (isPullMode) {
				state = State.PLAYING;
				getStreamFlow().resume();
				if (currentItem.getLength() >= 0) {
					long length = currentItem.getLength() - vodStartTS
							+ position;
					if (length < 0) {
						length = 0;
					}
					playLengthJob = schedulingService.addScheduledOnceJob(
							length, this);
				}
				flowControlService
						.resetTokenBuckets(PlaylistSubscriberStream.this);
				sendReset();
				sendResumeStatus(currentItem);
				sendVODSeekCM(msgIn, position);
				notifyItemResume(currentItem, position);
			}
		}

		synchronized public void seek(int position)
				throws IllegalStateException {
			if (state != State.PLAYING && state != State.PAUSED) {
				throw new IllegalStateException();
			}
			if (isPullMode) {
				if (state == State.PLAYING && currentItem.getLength() >= 0) {
					long length = currentItem.getLength() - vodStartTS
							+ position;
					if (length < 0) {
						length = 0;
					}
					playLengthJob = schedulingService.addScheduledOnceJob(
							length, this);
				}
				releasePendingMessage();
				getStreamFlow().clear();
				clearWaitJobs();
				flowControlService
						.resetTokenBuckets(PlaylistSubscriberStream.this);
				isWaitingForToken = false;
				sendClearPing();
				sendReset();
				sendSeekStatus(currentItem, position);
				sendStartStatus(currentItem);
				int seekPos = sendVODSeekCM(msgIn, position);
				// We seeked to the nearest keyframe so use real timestamp now 
				if (seekPos == -1) {
					seekPos = position;
				}
				notifyItemSeek(currentItem, seekPos);
				if (state == State.PAUSED) {
					// we send a single snapshot on pause.
					// XXX we need to take BWC into account, for
					// now send forcefully.
					IMessage msg = msgIn.pullMessage();
					while (msg != null) {
						if (msg instanceof RTMPMessage) {
							RTMPMessage rtmpMessage = (RTMPMessage) msg;
							IRTMPEvent body = rtmpMessage.getBody();
							if (body instanceof VideoData
									&& ((VideoData) body).getFrameType() == FrameType.KEYFRAME) {
								body.setTimestamp(seekPos);
								msgOut.pushMessage(rtmpMessage);
								rtmpMessage.getBody().release();
								break;
							}
						}
					}
				}
			}
		}

		synchronized public void stop() throws IllegalStateException {

			if (state != State.PLAYING && state != State.PAUSED) {
				throw new IllegalStateException();
			}
			if (msgIn != null) {
				msgIn.unsubscribe(this);
				msgIn = null;
			}
			state = State.STOPPED;
			releasePendingMessage();
			getStreamFlow().reset();
			clearWaitJobs();
			flowControlService.resetTokenBuckets(PlaylistSubscriberStream.this);
			isWaitingForToken = false;
			notifyItemStop(currentItem);
			sendStopStatus(currentItem);
			sendClearPing();
			sendReset();
		}

		synchronized public void close() {

			if (state == State.PLAYING || state == State.PAUSED) {
				if (msgIn != null) {
					msgIn.unsubscribe(this);
					msgIn = null;
				}
			}
			releasePendingMessage();

			state = State.CLOSED;
			getStreamFlow().reset();
			clearWaitJobs();
			sendClearPing();
		}

		synchronized private void pullAndPush() {
			if (state == State.PLAYING && isPullMode && !isWaitingForToken) {
				int size;
				if (pendingMessage != null) {
					IRTMPEvent body = pendingMessage.getBody();
					if (!(body instanceof IStreamData)) {
						throw new RuntimeException(
								"expected IStreamData but got " + body);
					}

					size = ((IStreamData) body).getData().limit();
					boolean toSend = true;
					if (body instanceof VideoData) {
						if (!videoBucket.acquireTokenNonblocking(size, this)) {
							isWaitingForToken = true;
							toSend = false;
						}
					} else if (body instanceof AudioData) {
						if (!audioBucket.acquireTokenNonblocking(size, this)) {
							isWaitingForToken = true;
							toSend = false;
						}
					}
					if (toSend) {
						sendMessage(pendingMessage);
						releasePendingMessage();
					}
				} else {
					while (true) {
						IMessage msg = msgIn.pullMessage();
						if (adaptFlowJob == null) {
							adaptFlowJob = schedulingService.addScheduledJob(
									100, new IScheduledJob() {
										public void execute(
												ISchedulingService service) {
											streamFlowController
													.adaptBandwidthForFlow(
															getStreamFlow(),
															PlaylistSubscriberStream.this);
										}
									});
						}

						if (msg == null) {
							// end of the VOD stream
							final IStreamFlow streamFlow = getStreamFlow();
							int timeDelta = (int) (streamFlow.getBufferTime() + streamFlow
									.getZeroToStreamTime());
							// wait until the client finishes
							if (waitStopJob == null) {
								waitStopJob = schedulingService
										.addScheduledOnceJob(timeDelta,
												new IScheduledJob() {
													public void execute(
															ISchedulingService service) {
														// OMFG: it works god dammit! now we stop it.
														stop();
														onItemEnd();
														log.info("Stop");
													}
												});
								log.info("Scheduled stop in: " + timeDelta);
							}
							break;
						} else {
							if (msg instanceof RTMPMessage) {
								RTMPMessage rtmpMessage = (RTMPMessage) msg;
								IRTMPEvent body = rtmpMessage.getBody();
								if (!(body instanceof IStreamData)) {
									throw new RuntimeException(
											"expected IStreamData but got "
													+ body);
								}

								size = ((IStreamData) body).getData().limit();
								boolean toSend = true;
								if (body instanceof VideoData) {
									if (!videoBucket.acquireTokenNonblocking(
											size, this)) {
										isWaitingForToken = true;
										toSend = false;
									}
								} else if (body instanceof AudioData) {
									if (!audioBucket.acquireTokenNonblocking(
											size, this)) {
										isWaitingForToken = true;
										toSend = false;
									}
								}
								if (toSend) {
									sendMessage(rtmpMessage);
									((IStreamData) body).getData().release();
								} else {
									pendingMessage = rtmpMessage;
								}
								break;
							}
						}
					}
				}
			}
		}

		private void clearWaitJobs() {
			if (adaptFlowJob != null) {
				schedulingService.removeScheduledJob(adaptFlowJob);
				adaptFlowJob = null;
			}
			if (waitStopJob != null) {
				schedulingService.removeScheduledJob(waitStopJob);
				waitStopJob = null;
			}
			if (waitLiveJob != null) {
				schedulingService.removeScheduledJob(waitLiveJob);
				waitLiveJob = null;
			}
			if (playLengthJob != null) {
				schedulingService.removeScheduledJob(playLengthJob);
				playLengthJob = null;
			}
		}

		private void sendMessage(RTMPMessage message) {
			if (vodStartTS == -1) {
				vodStartTS = message.getBody().getTimestamp();
			} else {
				if (currentItem.getLength() >= 0) {
					int duration = message.getBody().getTimestamp()
							- vodStartTS;
					if (duration > currentItem.getLength()
							&& playLengthJob == null) {
						// stop this item
						stop();
						onItemEnd();
						return;
					}
				}
			}
			getStreamFlow().update(message);
			msgOut.pushMessage(message);
		}

		private void sendClearPing() {
			Ping ping1 = new Ping();
			ping1.setValue1((short) 1);
			ping1.setValue2(getStreamId());

			RTMPMessage ping1Msg = new RTMPMessage();
			ping1Msg.setBody(ping1);
			msgOut.pushMessage(ping1Msg);
		}

		private void sendReset() {
			if (isPullMode) {
				Ping ping1 = new Ping();
				ping1.setValue1((short) 4);
				ping1.setValue2(getStreamId());

				RTMPMessage ping1Msg = new RTMPMessage();
				ping1Msg.setBody(ping1);
				msgOut.pushMessage(ping1Msg);
			}

			Ping ping2 = new Ping();
			ping2.setValue1((short) 0);
			ping2.setValue2(getStreamId());

			RTMPMessage ping2Msg = new RTMPMessage();
			ping2Msg.setBody(ping2);
			msgOut.pushMessage(ping2Msg);

			ResetMessage reset = new ResetMessage();
			msgOut.pushMessage(reset);
		}

		private void sendResetStatus(IPlayItem item) {
			Status reset = new Status(StatusCodes.NS_PLAY_RESET);
			reset.setClientid(getStreamId());
			reset.setDetails(item.getName());
			reset
					.setDesciption("Playing and resetting " + item.getName()
							+ ".");

			StatusMessage resetMsg = new StatusMessage();
			resetMsg.setBody(reset);
			msgOut.pushMessage(resetMsg);
		}

		private void sendStartStatus(IPlayItem item) {
			Status start = new Status(StatusCodes.NS_PLAY_START);
			start.setClientid(getStreamId());
			start.setDetails(item.getName());
			start.setDesciption("Started playing " + item.getName() + ".");

			StatusMessage startMsg = new StatusMessage();
			startMsg.setBody(start);
			msgOut.pushMessage(startMsg);
		}

		private void sendStopStatus(IPlayItem item) {
			Status stop = new Status(StatusCodes.NS_PLAY_STOP);
			stop.setClientid(getStreamId());
			stop.setDetails(item.getName());

			StatusMessage stopMsg = new StatusMessage();
			stopMsg.setBody(stop);
			msgOut.pushMessage(stopMsg);
		}

		private void sendSeekStatus(IPlayItem item, int position) {
			Status seek = new Status(StatusCodes.NS_SEEK_NOTIFY);
			seek.setClientid(getStreamId());
			seek.setDetails(item.getName());
			seek.setDesciption("Seeking " + position + " (stream ID: "
					+ getStreamId() + ").");

			StatusMessage seekMsg = new StatusMessage();
			seekMsg.setBody(seek);
			msgOut.pushMessage(seekMsg);
		}

		private void sendPauseStatus(IPlayItem item) {
			Status pause = new Status(StatusCodes.NS_PAUSE_NOTIFY);
			pause.setClientid(getStreamId());
			pause.setDetails(item.getName());

			StatusMessage pauseMsg = new StatusMessage();
			pauseMsg.setBody(pause);
			msgOut.pushMessage(pauseMsg);
		}

		private void sendResumeStatus(IPlayItem item) {
			Status resume = new Status(StatusCodes.NS_UNPAUSE_NOTIFY);
			resume.setClientid(getStreamId());
			resume.setDetails(item.getName());

			StatusMessage resumeMsg = new StatusMessage();
			resumeMsg.setBody(resume);
			msgOut.pushMessage(resumeMsg);
		}

		private void sendPublishedStatus(IPlayItem item) {
			Status unpublished = new Status(StatusCodes.NS_PLAY_PUBLISHNOTIFY);
			unpublished.setClientid(getStreamId());
			unpublished.setDetails(item.getName());

			StatusMessage unpublishedMsg = new StatusMessage();
			unpublishedMsg.setBody(unpublished);
			msgOut.pushMessage(unpublishedMsg);
		}

		private void sendUnpublishedStatus(IPlayItem item) {
			Status unpublished = new Status(StatusCodes.NS_PLAY_UNPUBLISHNOTIFY);
			unpublished.setClientid(getStreamId());
			unpublished.setDetails(item.getName());

			StatusMessage unpublishedMsg = new StatusMessage();
			unpublishedMsg.setBody(unpublished);
			msgOut.pushMessage(unpublishedMsg);
		}

		private void sendStreamNotFoundStatus(IPlayItem item) {
			Status notFound = new Status(StatusCodes.NS_PLAY_STREAMNOTFOUND);
			notFound.setClientid(getStreamId());
			notFound.setLevel(Status.ERROR);
			notFound.setDetails(item.getName());

			StatusMessage notFoundMsg = new StatusMessage();
			notFoundMsg.setBody(notFound);
			msgOut.pushMessage(notFoundMsg);
		}

		private void sendVODInitCM(IMessageInput msgIn, IPlayItem item) {
			OOBControlMessage oobCtrlMsg = new OOBControlMessage();
			oobCtrlMsg.setTarget(IPassive.KEY);
			oobCtrlMsg.setServiceName("init");
			Map<Object, Object> paramMap = new HashMap<Object, Object>();
			paramMap.put("startTS", new Integer((int) item.getStart()));
			oobCtrlMsg.setServiceParamMap(paramMap);
			msgIn.sendOOBControlMessage(this, oobCtrlMsg);
		}

		private int sendVODSeekCM(IMessageInput msgIn, int position) {
			OOBControlMessage oobCtrlMsg = new OOBControlMessage();
			oobCtrlMsg.setTarget(ISeekableProvider.KEY);
			oobCtrlMsg.setServiceName("seek");
			Map<Object, Object> paramMap = new HashMap<Object, Object>();
			paramMap.put("position", new Integer(position));
			oobCtrlMsg.setServiceParamMap(paramMap);
			msgIn.sendOOBControlMessage(this, oobCtrlMsg);
			if (oobCtrlMsg.getResult() instanceof Integer) {
				return (Integer) oobCtrlMsg.getResult();
			} else {
				return -1;
			}
		}

		public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
				OOBControlMessage oobCtrlMsg) {
			if ("ConnectionConsumer".equals(oobCtrlMsg.getTarget())) {
				if (source instanceof IProvider) {
					msgOut
							.sendOOBControlMessage((IProvider) source,
									oobCtrlMsg);
				}
			}
		}

		public void onPipeConnectionEvent(PipeConnectionEvent event) {
			switch (event.getType()) {
				case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
					if (event.getProvider() != this) {
						if (isWaiting) {
							schedulingService.removeScheduledJob(waitLiveJob);
							waitLiveJob = null;
							if (currentItem.getLength() >= 0) {
								playLengthJob = schedulingService
										.addScheduledOnceJob(currentItem
												.getLength(), this);
							}
							isWaiting = false;
						}
						sendPublishedStatus(currentItem);
					}
					break;
				case PipeConnectionEvent.PROVIDER_DISCONNECT:
					if (isPullMode) {
						sendStopStatus(currentItem);
					} else {
						sendUnpublishedStatus(currentItem);
					}
					break;
				case PipeConnectionEvent.CONSUMER_CONNECT_PULL:
					if (event.getConsumer() == this) {
						isPullMode = true;
					}
					break;
				case PipeConnectionEvent.CONSUMER_CONNECT_PUSH:
					if (event.getConsumer() == this) {
						isPullMode = false;
					}
					break;
				default:
					break;
			}
		}

		synchronized public void pushMessage(IPipe pipe, IMessage message) {
			if (message instanceof ResetMessage) {
				sendReset();
			}
			if (message instanceof RTMPMessage) {
				RTMPMessage rtmpMessage = (RTMPMessage) message;
				IRTMPEvent body = rtmpMessage.getBody();
				if (!(body instanceof IStreamData)) {
					throw new RuntimeException("expected IStreamData but got "
							+ body);
				}

				int size = ((IStreamData) body).getData().limit();
				if (body instanceof VideoData) {
					IVideoStreamCodec videoCodec = null;
					if (msgIn instanceof IBroadcastScope) {
						IClientBroadcastStream stream = (IClientBroadcastStream) ((IBroadcastScope) msgIn)
								.getAttribute(IBroadcastScope.STREAM_ATTRIBUTE);
						if (stream != null && stream.getCodecInfo() != null) {
							videoCodec = stream.getCodecInfo().getVideoCodec();
						}
					}

					if (videoCodec == null || videoCodec.canDropFrames()) {
						// Only check for frame dropping if the codec supports it
						long pendingVideos = pendingVideoMessages();
						if (!videoFrameDropper.canSendPacket(rtmpMessage,
								pendingVideos)) {
							//System.err.println("Dropping1: " + body + " " + pendingVideos);
							return;
						}

						boolean drop = !videoBucket.acquireToken(size, 0);
						if (!receiveVideo || pendingVideos > 1 || drop) {
							//System.err.println("Dropping2: " + receiveVideo + " " + pendingVideos + " " + videoBucket + " size: " + size + " drop: " + drop);
							videoFrameDropper.dropPacket(rtmpMessage);
							return;
						}

						videoFrameDropper.sendPacket(rtmpMessage);
					}
				} else if (body instanceof AudioData) {
					if (!receiveAudio || !audioBucket.acquireToken(size, 0)) {
						return;
					}
				}
			}
			msgOut.pushMessage(message);
		}

		synchronized public void execute(ISchedulingService service) {
			if (playLengthJob == null) {
				return;
			}
			playLengthJob = null;
			stop();
			onItemEnd();
		}

		synchronized public void available(ITokenBucket bucket,
				double tokenCount) {
			isWaitingForToken = false;
			pullAndPush();
		}

		public void reset(ITokenBucket bucket, double tokenCount) {
			isWaitingForToken = false;
		}

		public void updateBandwithConfigure() {
			flowControlService.updateBWConfigure(PlaylistSubscriberStream.this);
		}

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

		private void releasePendingMessage() {
			if (pendingMessage != null) {
				IRTMPEvent body = pendingMessage.getBody();
				if (body instanceof IStreamData) {
					((IStreamData) body).getData().release();
				}

				pendingMessage = null;
			}
		}
	}

	private class StreamNotFoundException extends Exception {
		private static final long serialVersionUID = 812106823615971891L;

		public StreamNotFoundException(String name) {
			super("Stream " + name + " not found.");
		}

	}
}
