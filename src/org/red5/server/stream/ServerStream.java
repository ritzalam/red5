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
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistController;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.api.stream.ResourceExistException;
import org.red5.server.api.stream.ResourceNotFoundException;
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
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.ResetMessage;

/**
 * An implementation for server side stream.
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class ServerStream extends AbstractStream implements IServerStream,
		IFilter, IPushableConsumer, IPipeConnectionListener {
	private static final Log log = LogFactory.getLog(ServerStream.class);
	
	private enum State {
		UNINIT,
		CLOSED,
		STOPPED,
		PLAYING
	}
	
	private State state;
	
	private String publishedName;
	
	private IPlaylistController controller;
	private IPlaylistController defaultController;
	private boolean isRewind = false;
	private boolean isRandom = false;
	private boolean isRepeat = false;
	
	private List<IPlayItem> items;
	private int currentItemIndex;
	private IPlayItem currentItem;
	
	private IMessageInput msgIn;
	private IMessageOutput msgOut;
	
	private ISchedulingService scheduler;
	private String liveJobName;
	private String vodJobName;
	
	private long vodStartTS;
	private long serverStartTS;
	private long nextVideoTS;
	private long nextAudioTS;
	private long nextDataTS;
	private long nextTS;
	private RTMPMessage nextRTMPMessage;
	
	public ServerStream() {
		defaultController = new SimplePlaylistController();
		items = new ArrayList<IPlayItem>();
		state = State.UNINIT;
	}
	
	synchronized public void addItem(IPlayItem item) {
		items.add(item);
	}

	synchronized public void addItem(IPlayItem item, int index) {
		items.add(index, item);
	}

	synchronized public void removeItem(int index) {
		if (index < 0 || index >= items.size()) return;
		items.remove(index);
	}

	synchronized public void removeAllItems() {
		items.clear();
	}

	public int getItemSize() {
		return items.size();
	}

	synchronized public void previousItem() {
		stop();
		moveToPrevious();
		if (currentItemIndex == -1) return;
		IPlayItem item = items.get(currentItemIndex);
		play(item);
	}

	synchronized public void nextItem() {
		stop();
		moveToNext();
		if (currentItemIndex == -1) return;
		IPlayItem item = items.get(currentItemIndex);
		play(item);
	}

	synchronized public void setItem(int index) {
		if (index < 0 || index >= items.size()) return;
		currentItemIndex = index;
		IPlayItem item = items.get(currentItemIndex);
		play(item);
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

	public void saveAs(String filePath, boolean isAppend)
			throws ResourceNotFoundException, ResourceExistException {
		// TODO Auto-generated method stub

	}

	public IProvider getProvider() {
		return this;
	}

	public String getPublishedName() {
		return publishedName;
	}

	public void setPublishedName(String name) {
		publishedName = name;
	}

	/**
	 * Start the server-side stream
	 */
	public void start() {
		if (state != State.UNINIT) {
			throw new IllegalStateException("State " + state + " not valid to start");
		}
		if (items.size() == 0) {
			throw new IllegalStateException("At least one item should be specified to start");
		}
		if (publishedName == null) {
			throw new IllegalStateException("A published name is needed to start");
		}
		IProviderService providerService =
			(IProviderService) getScope().getContext().getBean(IProviderService.KEY);
		providerService.registerBroadcastStream(getScope(), publishedName, this);
		scheduler = (ISchedulingService) getScope().getContext().getBean(ISchedulingService.SCHEDULING_SERVICE);
		state = State.STOPPED;
		currentItemIndex = -1;
		nextItem();
	}
	
	synchronized public void stop() {
		if (state != State.PLAYING) return;
		if (liveJobName != null) {
			scheduler.removeScheduledJob(liveJobName);
			liveJobName = null;
		}
		if (vodJobName != null) {
			scheduler.removeScheduledJob(vodJobName);
			vodJobName = null;
		}
		if (msgIn != null) {
			msgIn.unsubscribe(this);
			msgIn = null;
		}
		if (nextRTMPMessage != null)
			nextRTMPMessage.getBody().release();
		state = State.STOPPED;
	}

	synchronized public void close() {
		if (state == State.PLAYING) {
			stop();
		}
		IProviderService providerService =
			(IProviderService) getScope().getContext().getBean(IProviderService.KEY);
		providerService.unregisterBroadcastStream(getScope(), publishedName);
		state = State.CLOSED;
	}

	public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
			OOBControlMessage oobCtrlMsg) {
	}

	public void pushMessage(IPipe pipe, IMessage message) {
		msgOut.pushMessage(message);
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
		case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
			if (event.getProvider() == this &&
					(event.getParamMap() == null || !event.getParamMap().containsKey("record"))) {
				this.msgOut = (IMessageOutput) event.getSource();
			}
			break;
		case PipeConnectionEvent.PROVIDER_DISCONNECT:
			if (this.msgOut == event.getSource()) {
				this.msgOut = null;
			}
			break;
		default:
			break;
		}
	}
	
	/**
	 * Play a specific IPlayItem.
	 * The strategy for now is VOD first, Live second.
	 * Should be called in a synchronized context.
	 * @param item
	 */
	private void play(IPlayItem item) {
		if (state != State.STOPPED) return;
		boolean isLive = false;
		IProviderService providerService =
			(IProviderService) getScope().getContext().getBean(IProviderService.KEY);
		msgIn = providerService.getVODProviderInput(getScope(), item.getName());
		if (msgIn == null) {
			msgIn = providerService.getLiveProviderInput(getScope(), item.getName(), true);
			isLive = true;
		}
		if (msgIn == null) {
			log.warn("ABNORMAL Can't get both VOD and Live input from providerService");
			return;
		}
		state = State.PLAYING;
		currentItem = item;
		sendResetMessage();
		if (isLive) {
			if (item.getLength() >= 0) {
				liveJobName = scheduler.addScheduledOnceJob(item.getLength(), new IScheduledJob() {
					public void execute(ISchedulingService service) {
						synchronized (ServerStream.this) {
							if (liveJobName == null) return;
							liveJobName = null;
							onItemEnd();
						}
					}
				});
			}
		} else {
			long start = item.getStart();
			if (start < 0) start = 0;
			sendVODInitCM(msgIn, (int) start);
			startBroadcastVOD();
		}
	}
	
	private void onItemEnd() {
		nextItem();
	}

	private void sendResetMessage() {
		msgOut.pushMessage(new ResetMessage());
	}
	
	private void startBroadcastVOD() {
		nextVideoTS = nextAudioTS = nextDataTS = 0;
		nextRTMPMessage = null;
		vodStartTS = 0;
		serverStartTS = System.currentTimeMillis();
		scheduleNextMessage();
	}
	
	/**
	 * Pull the next message from IMessageInput and schedule
	 * it for push according to the timestamp.
	 */
	private void scheduleNextMessage() {
		boolean first = nextRTMPMessage == null;
		
		nextRTMPMessage = getNextRTMPMessage();
		if (nextRTMPMessage == null) {
			onItemEnd();
			return;
		}

		if (first) {
			// FIXME hack the first Metadata Tag from FLVReader
			// the FLVReader will issue a metadata tag of ts 0
			// even if it is seeked to somewhere in the middle
			// which will cause the stream to wait too long.
			// Is this an FLVReader's bug?
			if (!(nextRTMPMessage.getBody() instanceof VideoData) &&
					!(nextRTMPMessage.getBody() instanceof AudioData) &&
					nextRTMPMessage.getBody().getTimestamp() == 0) {
				nextRTMPMessage.getBody().release();
				nextRTMPMessage = getNextRTMPMessage();
				if (nextRTMPMessage == null) {
					onItemEnd();
					return;
				}
			}
		}
		
		IRTMPEvent rtmpEvent = nextRTMPMessage.getBody();
		if (rtmpEvent instanceof VideoData) {
			if (!nextRTMPMessage.isTimerRelative()) {
				nextVideoTS = rtmpEvent.getTimestamp();
			} else {
				nextVideoTS += rtmpEvent.getTimestamp();
			}
			nextTS = nextVideoTS;
		} else if (rtmpEvent instanceof AudioData) {
			if (!nextRTMPMessage.isTimerRelative()) {
				nextAudioTS = rtmpEvent.getTimestamp();
			} else {
				nextAudioTS += rtmpEvent.getTimestamp();
			}
			nextTS = nextAudioTS;
		} else {
			if (!nextRTMPMessage.isTimerRelative()) {
				nextDataTS = rtmpEvent.getTimestamp();
			} else {
				nextDataTS += rtmpEvent.getTimestamp();
			}
			nextTS = nextDataTS;
		}
		if (first) {
			vodStartTS = nextTS;
		}
		long delta = nextTS - vodStartTS - (System.currentTimeMillis() - serverStartTS);
		
		vodJobName = scheduler.addScheduledOnceJob(delta, new IScheduledJob() {
			public void execute(ISchedulingService service) {
				synchronized (ServerStream.this) {
					if (vodJobName == null) return;
					vodJobName = null;
					msgOut.pushMessage(nextRTMPMessage);
					nextRTMPMessage.getBody().release();
					long start = currentItem.getStart();
					if (start < 0) start = 0;
					if (currentItem.getLength() >= 0 &&
							nextTS - currentItem.getStart() > currentItem.getLength()) {
						onItemEnd();
						return;
					}
					scheduleNextMessage();
				}
			}
		});
	}
	
	private RTMPMessage getNextRTMPMessage() {
		IMessage message = null;
		do {
			message = msgIn.pullMessage();
			if (message == null) {
				return null;
			}
		} while (!(message instanceof RTMPMessage));
		return (RTMPMessage) message;
	}
	
	private void sendVODInitCM(IMessageInput msgIn, int start) {
		OOBControlMessage oobCtrlMsg = new OOBControlMessage();
		oobCtrlMsg.setTarget(IPassive.KEY);
		oobCtrlMsg.setServiceName("init");
		Map<Object, Object> paramMap = new HashMap<Object, Object>();
		paramMap.put("startTS", new Integer(start));
		oobCtrlMsg.setServiceParamMap(paramMap);
		msgIn.sendOOBControlMessage(this, oobCtrlMsg);
	}
	
	/**
	 * Move to the next item updating the currentItemIndex.
	 * Should be called in synchronized context.
	 */
	private void moveToNext() {
		if (currentItemIndex >= items.size()) {
			currentItemIndex = items.size() - 1;
		}
		if (controller != null) {
			currentItemIndex = controller.nextItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.nextItem(this, currentItemIndex);
		}
	}
	
	/**
	 * Move to the previous item updating the currentItemIndex.
	 * Should be called in synchronized context.
	 */
	private void moveToPrevious() {
		if (currentItemIndex >= items.size()) {
			currentItemIndex = items.size() - 1;
		}
		if (controller != null) {
			currentItemIndex = controller.previousItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.previousItem(this, currentItemIndex);
		}
	}
}
