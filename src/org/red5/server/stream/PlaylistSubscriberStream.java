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
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.stream.ITokenBucket.ITokenBucketCallback;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.StatusMessage;

public class PlaylistSubscriberStream extends AbstractClientStream
implements IPlaylistSubscriberStream {
	private static final Log log = LogFactory.getLog(PlaylistSubscriberStream.class);
	
	private enum State {
		UNINIT,
		STOPPED,
		PLAYING,
		PAUSED,
		CLOSED
	}
	
	private IPlaylistController controller;
	private IPlaylistController defaultController;
	
	private List<IPlayItem> items;
	private int currentItemIndex;
	
	private PlayEngine engine;
	private IFlowControlService flowControlService;
	
	private boolean isRewind = false;
	private boolean isRandom = false;
	private boolean isRepeat = false;
	
	public PlaylistSubscriberStream() {
		defaultController = new SimplePlaylistController();
		items = new ArrayList<IPlayItem>();
		engine = new PlayEngine();
		currentItemIndex = 0;
	}
	
	public void start() {
		flowControlService =
			(IFlowControlService) getScope().getContext().getBean(IFlowControlService.KEY);
		engine.start();
		notifySubscriberStart();
	}

	public void play() {
		synchronized (items) {
			if (items.size() == 0) return;
			if (currentItemIndex == -1) moveToNext();
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
		} catch (IllegalStateException e) {}
	}

	public void resume(int position) {
		try {
			engine.resume(position);
		} catch (IllegalStateException e) {}
	}

	public void stop() {
		try {
			engine.stop();
		} catch (IllegalStateException e) {}
	}

	public void seek(int position) {
		try {
			engine.seek(position);
		} catch (IllegalStateException e) {}
	}

	public void close() {
		engine.close();
		flowControlService.releaseFlowControllable(this);
		notifySubscriberClose();
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
			if (index < 0 || index >= items.size()) return;
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
			try {
				engine.stop();
			} catch (IllegalStateException e) {}
			items.clear();
		}
	}

	public void previousItem() {
		synchronized (items) {
			moveToPrevious();
			if (currentItemIndex == -1) return;
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
			moveToNext();
			if (currentItemIndex == -1) return;
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

	public void setItem(int index) {
		synchronized (items) {
			if (index < 0 || index >= items.size()) return;
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

	public void setPlaylistController(IPlaylistController controller) {
		this.controller = controller;
	}

	public int getItemSize() {
		return items.size();
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
			currentItemIndex = defaultController.nextItem(this, currentItemIndex);
		}
	}
	
	/**
	 * Move the current item to the previous in list.
	 */
	private void moveToPrevious() {
		if (controller != null) {
			currentItemIndex = controller.previousItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.previousItem(this, currentItemIndex);
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
	private class PlayEngine
	implements IFilter, IPushableConsumer, IPipeConnectionListener,
	ITokenBucketCallback, IScheduledJob {
		// XXX shall we make this as a configurable property?
		private static final long LIVE_WAIT_TIMEOUT = 5000;
		
		private State state;
		
		private IMessageInput msgIn;
		private IMessageOutput msgOut;
		
		private boolean isPullMode = false;
		
		private ISchedulingService schedulingService = null;
		private String waitLiveJob;
		private String liveLengthJob;
		private boolean isWaiting = false;
		private int vodStartTS = 0;
		
		private IPlayItem currentItem;
		
		private ITokenBucket audioBucket;
		private ITokenBucket videoBucket;
		private RTMPMessage pendingMessage;
		private boolean isWaitingForToken = false;
		
		public PlayEngine() {
			state = State.UNINIT;
		}
		
		synchronized public void start() {
			if (state != State.UNINIT) throw new IllegalStateException();
			state = State.STOPPED;
			schedulingService = (ISchedulingService) getScope().getContext().getBean(ISchedulingService.SCHEDULING_SERVICE);
			IConsumerService consumerManager =
				(IConsumerService) getScope().getContext().getBean(IConsumerService.KEY);
			msgOut = consumerManager.getConsumerOutput(PlaylistSubscriberStream.this);
			msgOut.subscribe(this, null);
			audioBucket = flowControlService.getAudioTokenBucket(PlaylistSubscriberStream.this);
			videoBucket = flowControlService.getVideoTokenBucket(PlaylistSubscriberStream.this);
		}
		
		synchronized public void play(IPlayItem item)
		throws StreamNotFoundException, IllegalStateException {
			if (state != State.STOPPED) throw new IllegalStateException();
			int type = (int) (item.getStart() / 1000);
			// see if it's a published stream
			IScope thisScope = getScope();
			IContext context = thisScope.getContext();
			IProviderService providerService = (IProviderService) context.getBean(IProviderService.KEY);
			IMessageInput liveInput = providerService.getLiveProviderInput(thisScope, item.getName(), false);
			IMessageInput vodInput = providerService.getVODProviderInput(thisScope, item.getName());
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
					// TODO: Wait for stream to be created until timeout, otherwise continue
					// with next item in playlist (see Macromedia documentation)
					// NOTE: For now we create a temporary stream
					decision = 2;
				}
				break;
				
			default:
				if (isFileStream) {
					decision = 1;
				}
				break;
			}
			if (decision == 2) liveInput = providerService.getLiveProviderInput(
					thisScope, item.getName(), true);
			currentItem = item;
			switch (decision) {
			case 0:
				msgIn = liveInput;
				if (msgIn instanceof IBroadcastScope) {
					// Send initial keyframe
					IClientBroadcastStream stream = (IClientBroadcastStream) ((IBroadcastScope) msgIn).getAttribute(IBroadcastScope.STREAM_ATTRIBUTE);
					if (stream != null && stream.getCodecInfo() != null) {
						IVideoStreamCodec videoCodec = stream.getCodecInfo().getVideoCodec();
						if (videoCodec != null) {
							ByteBuffer keyFrame = videoCodec.getKeyframe();
							if (keyFrame != null) {
								VideoData video = new VideoData(keyFrame);
								try {
									sendResetPing();
									sendResetStatus(item);
									sendStartStatus(item);
									sendBlankAudio();
									
									video.setTimestamp(0);
									
									RTMPMessage videoMsg = new RTMPMessage();
									videoMsg.setBody(video);
									videoMsg.setTimerRelative(false);
									msgOut.pushMessage(videoMsg);
									sendNotifications = false;
								} finally {
									video.release();
								}
							}
						}
					}
				}
				msgIn.subscribe(this, null);
				if (item.getLength() >= 0) {
					liveLengthJob = schedulingService.addScheduledOnceJob(item.getLength(), this);
				}
				break;
			case 2:
				msgIn = liveInput;
				msgIn.subscribe(this, null);
				isWaiting = true;
				waitLiveJob = schedulingService.addScheduledOnceJob(LIVE_WAIT_TIMEOUT,
						new IScheduledJob() {
					public void execute(ISchedulingService service) {
						waitLiveJob = null;
						isWaiting = false;
						onItemEnd();
					}
				});
				break;
			case 1:
				msgIn = vodInput;
				msgIn.subscribe(this, null);

				break;
			default:
				sendStreamNotFoundStatus(currentItem);
				throw new StreamNotFoundException(item.getName());
			}
			if (sendNotifications) {
				sendResetPing();
				sendResetStatus(item);
				sendStartStatus(item);
				sendBlankAudio();
				sendBlankVideo();
			}
			state = State.PLAYING;
			if (decision == 1) {
				pendingMessage = null;
				sendVODInitCM(msgIn, item);
				vodStartTS = -1;
				pullAndPush();
			}
			notifyItemPlay(currentItem, !isPullMode);
		}
		
		synchronized public void pause(int position) throws IllegalStateException {
			if (state != State.PLAYING) throw new IllegalStateException();
			if (isPullMode) {
				state = State.PAUSED;
				sendPauseStatus(currentItem);
				notifyItemPause(currentItem, position);
			}
		}
		
		synchronized public void resume(int position) throws IllegalStateException {
			if (state != State.PAUSED) throw new IllegalStateException();
			if (isPullMode) {
				state = State.PLAYING;
				flowControlService.resetTokenBuckets(PlaylistSubscriberStream.this);
				sendVODSeekCM(msgIn, position);
				sendResetPing();
				sendResumeStatus(currentItem);
				sendBlankAudio();
				notifyItemResume(currentItem, position);
			}
		}
		
		synchronized public void seek(int position) throws IllegalStateException {
			if (state != State.PLAYING && state != State.PAUSED) {
				throw new IllegalStateException();
			}
			if (isPullMode) {
				flowControlService.resetTokenBuckets(PlaylistSubscriberStream.this);
				isWaitingForToken = false;
				sendResetPing();
				sendSeekStatus(currentItem, position);
				sendStartStatus(currentItem);
				sendBlankAudio();
				sendVODSeekCM(msgIn, position);
				notifyItemSeek(currentItem, position);
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
			if (waitLiveJob != null) {
				schedulingService.removeScheduledJob(waitLiveJob);
				waitLiveJob = null;
			}
			if (liveLengthJob != null) {
				schedulingService.removeScheduledJob(liveLengthJob);
				liveLengthJob = null;
			}
			flowControlService.resetTokenBuckets(PlaylistSubscriberStream.this);
			isWaitingForToken = false;
			notifyItemStop(currentItem);
		}
		
		synchronized public void close() {
			if (state == State.PLAYING || state == State.PAUSED) {
				if (msgIn != null) {
					msgIn.unsubscribe(this);
					msgIn = null;
				}
			}
			if (pendingMessage != null) {
				IRTMPEvent body = pendingMessage.getBody();
				if (body instanceof IStreamData)
					((IStreamData) body).getData().release();
				
				pendingMessage = null;
			}
			
			state = State.CLOSED;
			if (waitLiveJob != null) {
				schedulingService.removeScheduledJob(waitLiveJob);
				waitLiveJob = null;
			}
			if (liveLengthJob != null) {
				schedulingService.removeScheduledJob(liveLengthJob);
				liveLengthJob = null;
			}
		}
		
		synchronized private void pullAndPush() {
			if (state == State.PLAYING && isPullMode && !isWaitingForToken) {
				int size;
				if (pendingMessage != null) {
					IRTMPEvent body = pendingMessage.getBody();
					if (!(body instanceof IStreamData))
						throw new RuntimeException("expected IStreamData but got " + body);
					
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
						((IStreamData) body).getData().release();
						pendingMessage = null;
					}
				} else {
					while (true) {
						IMessage msg = msgIn.pullMessage();
						if (msg == null) {
							// end of the VOD
							stop();
							onItemEnd();
							break;
						} else {
							if (msg instanceof RTMPMessage) {
								RTMPMessage rtmpMessage = (RTMPMessage) msg;
								IRTMPEvent body = rtmpMessage.getBody();
								if (!(body instanceof IStreamData))
									throw new RuntimeException("expected IStreamData but got " + body);
								
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
		
		private void sendMessage(RTMPMessage message) {
			if (vodStartTS == -1) {
				vodStartTS = message.getBody().getTimestamp();
			} else {
				if (currentItem.getLength() >= 0) {
					int diff = message.getBody().getTimestamp() - vodStartTS;
					if (diff > currentItem.getLength()) {
						// stop this item
						stop();
						onItemEnd();
						return;
					}
				}
			}
			msgOut.pushMessage(message);
		}
		
		private void sendResetPing() {
			Ping ping1 = new Ping();
			ping1.setValue1((short) 4);
			ping1.setValue2(getStreamId());

			RTMPMessage ping1Msg = new RTMPMessage();
			ping1Msg.setBody(ping1);
			msgOut.pushMessage(ping1Msg);
			
			Ping ping2 = new Ping();
			ping2.setValue1((short) 0);
			ping2.setValue2(getStreamId());
			
			RTMPMessage ping2Msg = new RTMPMessage();
			ping2Msg.setBody(ping2);
			msgOut.pushMessage(ping2Msg);
		}
		
		private void sendBlankAudio() {
			AudioData blankAudio = new AudioData();
			try {
				blankAudio.setTimestamp(0);
				
				RTMPMessage blankAudioMsg = new RTMPMessage();
				blankAudioMsg.setBody(blankAudio);
				blankAudioMsg.setTimerRelative(false);
				msgOut.pushMessage(blankAudioMsg);
			} finally {
				blankAudio.release();
			}
		}
		
		private void sendBlankVideo() {
			VideoData blankVideo = new VideoData();
			try {
				blankVideo.setTimestamp(0);
				
				RTMPMessage blankVideoMsg = new RTMPMessage();
				blankVideoMsg.setBody(blankVideo);
				blankVideoMsg.setTimerRelative(false);
				msgOut.pushMessage(blankVideoMsg);
			} finally {
				blankVideo.release();
			}
		}
		
		private void sendResetStatus(IPlayItem item) {
			Status reset = new Status(Status.NS_PLAY_RESET);
			reset.setClientid(getStreamId());
			reset.setDetails(item.getName());
			reset.setDesciption("Playing and resetting " + item.getName() + ".");
			
			StatusMessage resetMsg = new StatusMessage();
			resetMsg.setBody(reset);
			msgOut.pushMessage(resetMsg);
		}
		
		private void sendStartStatus(IPlayItem item) {
			Status start = new Status(Status.NS_PLAY_START);
			start.setClientid(getStreamId());
			start.setDetails(item.getName());
			start.setDesciption("Started playing " + item.getName() + ".");
			
			StatusMessage startMsg = new StatusMessage();
			startMsg.setBody(start);
			msgOut.pushMessage(startMsg);
		}
		
		private void sendStopStatus(IPlayItem item) {
			Status stop = new Status(Status.NS_PLAY_STOP);
			stop.setClientid(getStreamId());
			stop.setDetails(item.getName());
			
			StatusMessage stopMsg = new StatusMessage();
			stopMsg.setBody(stop);
			msgOut.pushMessage(stopMsg);
		}
		
		private void sendSeekStatus(IPlayItem item, int position) {
			Status seek = new Status(Status.NS_SEEK_NOTIFY);
			seek.setClientid(getStreamId());
			seek.setDetails(item.getName());
			seek.setDesciption("Seeking " + position + " (stream ID: " + getStreamId() + ").");
			
			StatusMessage seekMsg = new StatusMessage();
			seekMsg.setBody(seek);
			msgOut.pushMessage(seekMsg);
		}
		
		private void sendPauseStatus(IPlayItem item) {
			Status pause = new Status(Status.NS_PAUSE_NOTIFY);
			pause.setClientid(getStreamId());
			pause.setDetails(item.getName());
			
			StatusMessage pauseMsg = new StatusMessage();
			pauseMsg.setBody(pause);
			msgOut.pushMessage(pauseMsg);
		}
		
		private void sendResumeStatus(IPlayItem item) {
			Status resume = new Status(Status.NS_UNPAUSE_NOTIFY);
			resume.setClientid(getStreamId());
			resume.setDetails(item.getName());
			
			StatusMessage resumeMsg = new StatusMessage();
			resumeMsg.setBody(resume);
			msgOut.pushMessage(resumeMsg);
		}
		
		private void sendUnpublishedStatus(IPlayItem item) {
			Status unpublished = new Status(Status.NS_PLAY_UNPUBLISHNOTIFY);
			unpublished.setClientid(getStreamId());
			unpublished.setDetails(item.getName());
			
			StatusMessage unpublishedMsg = new StatusMessage();
			unpublishedMsg.setBody(unpublished);
			msgOut.pushMessage(unpublishedMsg);
		}
		
		private void sendStreamNotFoundStatus(IPlayItem item) {
			Status notFound = new Status(Status.NS_PLAY_STREAMNOTFOUND);
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
		
		private void sendVODSeekCM(IMessageInput msgIn, int position) {
			OOBControlMessage oobCtrlMsg = new OOBControlMessage();
			oobCtrlMsg.setTarget(ISeekableProvider.KEY);
			oobCtrlMsg.setServiceName("seek");
			Map<Object, Object> paramMap = new HashMap<Object, Object>();
			paramMap.put("position", new Integer(position));
			oobCtrlMsg.setServiceParamMap(paramMap);
			msgIn.sendOOBControlMessage(this, oobCtrlMsg);
		}

		public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
			
		}

		public void onPipeConnectionEvent(PipeConnectionEvent event) {
			switch (event.getType()) {
			case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
				if (event.getProvider() != this) {
					if (isWaiting) {
						schedulingService.removeScheduledJob(waitLiveJob);
						waitLiveJob = null;
						if (currentItem.getLength() >= 0) {
							liveLengthJob = schedulingService.addScheduledOnceJob(currentItem.getLength(), this);
						}
						isWaiting = false;
					}
				}
				break;
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
				if (isPullMode)
					sendStopStatus(currentItem);
				else {
					sendUnpublishedStatus(currentItem);
					stop();
					nextItem();
					if (state == State.STOPPED)
						// No more items in the playlist, close stream.
						close();
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
			if (message instanceof RTMPMessage) {
				RTMPMessage rtmpMessage = (RTMPMessage) message;
				IRTMPEvent body = rtmpMessage.getBody();
				if (!(body instanceof IStreamData))
					throw new RuntimeException("expected IStreamData but got " + body);
				
				int size = ((IStreamData) body).getData().limit();
				if (body instanceof VideoData) {
					if (!videoBucket.acquireToken(size, 0)) {
						return;
					}
				} else if (body instanceof AudioData) {
					if (!audioBucket.acquireToken(size, 0)) {
						return;
					}
				}
			}
			msgOut.pushMessage(message);
		}

		public void execute(ISchedulingService service) {
			liveLengthJob = null;
			stop();
			onItemEnd();
		}

		synchronized public void available(ITokenBucket bucket, long tokenCount) {
			isWaitingForToken = false;
			pullAndPush();
		}
		
		public void reset(ITokenBucket bucket, long tokenCount) {
			isWaitingForToken = false;
		}

		public void updateBandwithConfigure() {
			flowControlService.updateBWConfigure(PlaylistSubscriberStream.this);
		}
		
	}
	
	private class StreamNotFoundException extends Exception {
		private static final long serialVersionUID = 812106823615971891L;

		public StreamNotFoundException(String name) {
			super("Stream " + name + " not found.");
		}
		
	}
}