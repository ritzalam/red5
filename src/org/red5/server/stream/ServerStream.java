/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.stream;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistController;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.IStreamFilenameGenerator;
import org.red5.server.api.stream.IStreamFilenameGenerator.GenerationType;
import org.red5.server.api.stream.IStreamListener;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.api.stream.ResourceExistException;
import org.red5.server.api.stream.ResourceNotFoundException;
import org.red5.server.api.stream.StreamState;
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
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.consumer.FileConsumer;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.ResetMessage;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation for server side stream.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class ServerStream extends AbstractStream implements IServerStream, IFilter, IPushableConsumer, IPipeConnectionListener {
	/**
	 * Logger
	 */
	private static final Logger log = LoggerFactory.getLogger(ServerStream.class);

	private static final long WAIT_THRESHOLD = 0;

	/**
	 * Stream published name
	 */
	protected String publishedName;

	/**
	 * Actual playlist controller
	 */
	protected IPlaylistController controller;

	/**
	 * Default playlist controller
	 */
	protected IPlaylistController defaultController;

	/**
	 * Rewind flag state
	 */
	private boolean isRewind;

	/**
	 * Random flag state
	 */
	private boolean isRandom;

	/**
	 * Repeat flag state
	 */
	private boolean isRepeat;

	/**
	 * List of items in this playlist
	 */
	protected CopyOnWriteArrayList<IPlayItem> items;

	/**
	 * Current item index
	 */
	private int currentItemIndex;

	/**
	 * Current item
	 */
	protected IPlayItem currentItem;

	/**
	 * Message input
	 */
	private IMessageInput msgIn;

	/**
	 * Message output
	 */
	private IMessageOutput msgOut;

	/**
	 * Pipe for recording
	 */
	private IPipe recordPipe;

	/**
	 * The filename we are recording to.
	 */
	protected String recordingFilename;

	/**
	 * Scheduling service
	 */
	private ISchedulingService scheduler;

	/**
	 * Live broadcasting scheduled job name
	 */
	private String liveJobName;

	/**
	 * VOD scheduled job name
	 */
	private String vodJobName;

	/**
	 * VOD start timestamp
	 */
	private long vodStartTS;

	/**
	 * Server start timestamp
	 */
	private long serverStartTS;

	/**
	 * Next msg's timestamp
	 */
	private long nextTS;

	/**
	 * Next RTMP message
	 */
	private RTMPMessage nextRTMPMessage;

	/** Listeners to get notified about received packets. */
	private CopyOnWriteArraySet<IStreamListener> listeners = new CopyOnWriteArraySet<IStreamListener>();

	/** Constructs a new ServerStream. */
	public ServerStream() {
		defaultController = new SimplePlaylistController();
		items = new CopyOnWriteArrayList<IPlayItem>();
	}

	/** {@inheritDoc} */
	public void addItem(IPlayItem item) {
		items.add(item);
	}

	/** {@inheritDoc} */
	public void addItem(IPlayItem item, int index) {
		items.add(index, item);
		if (index <= currentItemIndex) {
			// item was added before the currently playing
			currentItemIndex++;
		}
	}

	/** {@inheritDoc} */
	public void removeItem(int index) {
		if (index < 0 || index >= items.size()) {
			return;
		}
		items.remove(index);
		if (index < currentItemIndex) {
			// item was removed before the currently playing
			currentItemIndex--;
		} else if (index == currentItemIndex) {
			// TODO: the currently playing item is removed - this should be handled differently
			currentItemIndex--;
		}
	}

	/** {@inheritDoc} */
	public void removeAllItems() {
		items.clear();
	}

	/** {@inheritDoc} */
	public int getItemSize() {
		return items.size();
	}

	/** {@inheritDoc} */
	public int getCurrentItemIndex() {
		return currentItemIndex;
	}

	/** {@inheritDoc} */
	public IPlayItem getCurrentItem() {
		return currentItem;
	}

	/** {@inheritDoc} */
	public IPlayItem getItem(int index) {
		try {
			return items.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	/** {@inheritDoc} */
	public void previousItem() {
		stop();
		moveToPrevious();
		if (currentItemIndex == -1) {
			return;
		}
		IPlayItem item = items.get(currentItemIndex);
		play(item);
	}

	/** {@inheritDoc} */
	public boolean hasMoreItems() {
		int nextItem = currentItemIndex + 1;
		if (nextItem >= items.size() && !isRepeat) {
			return false;
		} else {
			return true;
		}
	}

	/** {@inheritDoc} */
	public void nextItem() {
		stop();
		moveToNext();
		if (currentItemIndex == -1) {
			return;
		}
		IPlayItem item = items.get(currentItemIndex);
		play(item);
	}

	/** {@inheritDoc} */
	public void setItem(int index) {
		if (index < 0 || index >= items.size()) {
			return;
		}
		stop();
		currentItemIndex = index;
		IPlayItem item = items.get(currentItemIndex);
		play(item);
	}

	/** {@inheritDoc} */
	public boolean isRandom() {
		return isRandom;
	}

	/** {@inheritDoc} */
	public void setRandom(boolean random) {
		isRandom = random;
	}

	/** {@inheritDoc} */
	public boolean isRewind() {
		return isRewind;
	}

	/** {@inheritDoc} */
	public void setRewind(boolean rewind) {
		isRewind = rewind;
	}

	/** {@inheritDoc} */
	public boolean isRepeat() {
		return isRepeat;
	}

	/** {@inheritDoc} */
	public void setRepeat(boolean repeat) {
		isRepeat = repeat;
	}

	/** {@inheritDoc} */
	public void setPlaylistController(IPlaylistController controller) {
		this.controller = controller;
	}

	/** {@inheritDoc} */
	public void saveAs(String name, boolean isAppend) throws IOException, ResourceNotFoundException, ResourceExistException {
		try {
			IScope scope = getScope();
			IStreamFilenameGenerator generator = (IStreamFilenameGenerator) ScopeUtils.getScopeService(scope, IStreamFilenameGenerator.class, DefaultStreamFilenameGenerator.class);

			String filename = generator.generateFilename(scope, name, ".flv", GenerationType.RECORD);
			// Get file for that filename
			File file;
			if (generator.resolvesToAbsolutePath()) {
				file = new File(filename);
			} else {
				file = scope.getContext().getResource(filename).getFile();
			}
			if (!isAppend) {
				if (file.exists()) {
					// Per livedoc of FCS/FMS:
					// When "live" or "record" is used,
					// any previously recorded stream with the same stream URI is deleted.
					if (!file.delete())
						throw new IOException("file could not be deleted");
				}
			} else {
				if (!file.exists()) {
					// Per livedoc of FCS/FMS:
					// If a recorded stream at the same URI does not already exist,
					// "append" creates the stream as though "record" was passed.
					isAppend = false;
				}
			}

			if (!file.exists()) {
				// Make sure the destination directory exists
				String path = file.getAbsolutePath();
				int slashPos = path.lastIndexOf(File.separator);
				if (slashPos != -1) {
					path = path.substring(0, slashPos);
				}
				File tmp = new File(path);
				if (!tmp.isDirectory()) {
					tmp.mkdirs();
				}

				if (!file.canWrite()) {
					log.warn("File cannot be written to {}", file.getCanonicalPath());
				}
				file.createNewFile();
			} else {
				//remove existing meta file
				File meta = new File(file.getAbsolutePath() + ".meta");
				if (meta.delete()) {
					log.debug("Meta file deleted - {}", meta.getName());
				} else {
					log.warn("Meta file was not deleted - {}", meta.getName());
					meta.deleteOnExit();
				}
			}
			FileConsumer recordingFile = null;
			log.debug("Recording file: {}", file.getCanonicalPath());
			// get instance via spring
			if (scope.getContext().hasBean("fileConsumer")) {
				recordingFile = (FileConsumer) scope.getContext().getBean("fileConsumer");
				recordingFile.setScope(scope);
				recordingFile.setFile(file);
			} else {
				// get a new instance
				recordingFile = new FileConsumer(scope, file);
			}
			Map<String, Object> paramMap = new HashMap<String, Object>();
			if (isAppend) {
				paramMap.put("mode", "append");
			} else {
				paramMap.put("mode", "record");
			}
			if (null == recordPipe) {
				recordPipe = new InMemoryPushPushPipe();
			}
			recordPipe.subscribe(recordingFile, paramMap);
			recordingFilename = filename;
		} catch (IOException e) {
			log.warn("Save as exception", e);
		}
	}

	/** {@inheritDoc} */
	public String getSaveFilename() {
		return recordingFilename;
	}

	/** {@inheritDoc} */
	public IProvider getProvider() {
		return this;
	}

	/** {@inheritDoc} */
	public String getPublishedName() {
		return publishedName;
	}

	/** {@inheritDoc} */
	public void setPublishedName(String name) {
		publishedName = name;
	}

	/**
	 * Start this server-side stream
	 */
	public void start() {
		if (state != StreamState.UNINIT) {
			throw new IllegalStateException("State " + state + " not valid to start");
		}
		if (items.size() == 0) {
			throw new IllegalStateException("At least one item should be specified to start");
		}
		if (publishedName == null) {
			throw new IllegalStateException("A published name is needed to start");
		}
		// publish this server-side stream
		IProviderService providerService = (IProviderService) getScope().getContext().getBean(IProviderService.BEAN_NAME);
		providerService.registerBroadcastStream(getScope(), publishedName, this);
		Map<String, Object> recordParamMap = new HashMap<String, Object>();
		recordPipe = new InMemoryPushPushPipe();
		recordParamMap.put("record", null);
		recordPipe.subscribe((IProvider) this, recordParamMap);
		recordingFilename = null;
		scheduler = (ISchedulingService) getScope().getContext().getBean(ISchedulingService.BEAN_NAME);
		state = StreamState.STOPPED;
		currentItemIndex = -1;
		nextItem();
	}

	/**
	 * Stop this server-side stream
	 */
	public synchronized void stop() {
		if (state != StreamState.PLAYING && state != StreamState.PAUSED) {
			return;
		}
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
		if (nextRTMPMessage != null) {
			nextRTMPMessage.getBody().release();
		}
		state = StreamState.STOPPED;
	}

	/** {@inheritDoc} */
	public void pause() {
		if (state == StreamState.PLAYING) {
			state = StreamState.PAUSED;
		} else if (state == StreamState.PAUSED) {
			state = StreamState.PLAYING;
			vodStartTS = 0;
			serverStartTS = System.currentTimeMillis();
			scheduleNextMessage();
		}
	}

	/** {@inheritDoc} */
	public void seek(int position) {
		if (state != StreamState.PLAYING && state != StreamState.PAUSED) {
			// Can't seek when stopped/closed
			return;
		}
		sendVODSeekCM(msgIn, position);
	}

	/** {@inheritDoc} */
	public synchronized void close() {
		if (state == StreamState.PLAYING || state == StreamState.PAUSED) {
			stop();
		}
		if (msgOut != null) {
			msgOut.unsubscribe(this);
		}
		recordPipe.unsubscribe((IProvider) this);
		notifyBroadcastClose();
		state = StreamState.CLOSED;
	}

	/** {@inheritDoc} */
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
	}

	/** {@inheritDoc} */
	public void pushMessage(IPipe pipe, IMessage message) throws IOException {
		pushMessage(message);
	}

	/**
	 * Pipe connection event handler. There are two types of pipe connection events so far,
	 * provider push connection event and provider disconnection event.
	 *
	 * Pipe events handling is the most common way of working with pipes.
	 *
	 * @param event        Pipe connection event context
	 */
	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
			case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
				if (event.getProvider() == this && (event.getParamMap() == null || !event.getParamMap().containsKey("record"))) {
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
	 *
	 * @param item        Item to play
	 */
	protected void play(IPlayItem item) {
		// Return if already playing
		if (state != StreamState.STOPPED) {
			return;
		}
		// Assume this is not live stream
		boolean isLive = false;
		// Get provider service from Spring bean factory
		IProviderService providerService = (IProviderService) getScope().getContext().getBean(IProviderService.BEAN_NAME);
		msgIn = providerService.getVODProviderInput(getScope(), item.getName());
		if (msgIn == null) {
			msgIn = providerService.getLiveProviderInput(getScope(), item.getName(), true);
			isLive = true;
		}
		if (msgIn == null) {
			log.warn("ABNORMAL Can't get both VOD and Live input from providerService");
			return;
		}
		state = StreamState.PLAYING;
		currentItem = item;
		sendResetMessage();
		msgIn.subscribe(this, null);
		if (isLive) {
			if (item.getLength() >= 0) {
				liveJobName = scheduler.addScheduledOnceJob(item.getLength(), new IScheduledJob() {
					public void execute(ISchedulingService service) {
						synchronized (ServerStream.this) {
							if (liveJobName == null) {
								return;
							}
							liveJobName = null;
							onItemEnd();
						}
					}
				});
			}
		} else {
			long start = item.getStart();
			if (start < 0) {
				start = 0;
			}
			sendVODInitCM(msgIn, (int) start);
			startBroadcastVOD();
		}
	}

	/**
	 * Play next item on item end
	 */
	protected void onItemEnd() {
		nextItem();
	}

	/**
	 * Push message
	 * @param message     Message
	 */
	private void pushMessage(IMessage message) throws IOException {
		msgOut.pushMessage(message);
		recordPipe.pushMessage(message);

		// Notify listeners about received packet
		if (message instanceof RTMPMessage) {
			final IRTMPEvent rtmpEvent = ((RTMPMessage) message).getBody();
			if (rtmpEvent instanceof IStreamPacket) {
				for (IStreamListener listener : getStreamListeners()) {
					try {
						listener.packetReceived(this, (IStreamPacket) rtmpEvent);
					} catch (Exception e) {
						log.error("Error while notifying listener " + listener, e);
					}
				}
			}
		}
	}

	/**
	 * Send reset message
	 */
	private void sendResetMessage() {
		// Send new reset message
		try {
			pushMessage(new ResetMessage());
		} catch (IOException err) {
			log.error("Error while sending reset message.", err);
		}
	}

	/**
	 * Begin VOD broadcasting
	 */
	protected void startBroadcastVOD() {
		nextRTMPMessage = null;
		vodStartTS = 0;
		serverStartTS = System.currentTimeMillis();
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			if (recordingFilename != null) {
				handler.streamRecordStart(this);
			} else {
				handler.streamPublishStart(this);
			}
		}
		notifyBroadcastStart();
		scheduleNextMessage();
	}

	/**
	 *  Notifies handler on stream broadcast stop
	 */
	protected void notifyBroadcastClose() {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamBroadcastClose(this);
			} catch (Throwable t) {
				log.error("error notify streamBroadcastStop", t);
			}
		}
	}

	/**
	 *  Notifies handler on stream broadcast start
	 */
	protected void notifyBroadcastStart() {
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		if (handler != null) {
			try {
				handler.streamBroadcastStart(this);
			} catch (Throwable t) {
				log.error("error notify streamBroadcastStart", t);
			}
		}
	}

	/**
	 * Pull the next message from IMessageInput and schedule
	 * it for push according to the timestamp.
	 */
	protected void scheduleNextMessage() {
		boolean first = nextRTMPMessage == null;
		long delta;

		while (true) {
			nextRTMPMessage = getNextRTMPMessage();
			if (nextRTMPMessage == null) {
				onItemEnd();
				return;
			}

			IRTMPEvent rtmpEvent = nextRTMPMessage.getBody();
			// filter all non-AV messages
			if (!(rtmpEvent instanceof VideoData) && !(rtmpEvent instanceof AudioData)) {
				continue;
			}
			rtmpEvent = nextRTMPMessage.getBody();
			nextTS = rtmpEvent.getTimestamp();
			if (first) {
				vodStartTS = nextTS;
				first = false;
			}

			delta = nextTS - vodStartTS - (System.currentTimeMillis() - serverStartTS);
			if (delta < WAIT_THRESHOLD) {
				if (!doPushMessage()) {
					return;
				}
				if (state != StreamState.PLAYING) {
					// Stream is paused, don't load more messages
					nextRTMPMessage = null;
					return;
				}
			} else {
				break;
			}
		}
		vodJobName = scheduler.addScheduledOnceJob(delta, new IScheduledJob() {
			/** {@inheritDoc} */
			public void execute(ISchedulingService service) {
				synchronized (ServerStream.this) {
					if (vodJobName == null) {
						return;
					}
					vodJobName = null;
					if (!doPushMessage()) {
						return;
					}
					if (state == StreamState.PLAYING) {
						scheduleNextMessage();
					} else {
						// Stream is paused, don't load more messages
						nextRTMPMessage = null;
					}
				}
			}
		});
	}

	private boolean doPushMessage() {
		boolean sent = false;
		long start = currentItem.getStart();
		if (start < 0) {
			start = 0;
		}
		if (currentItem.getLength() >= 0 && nextTS - start > currentItem.getLength()) {
			onItemEnd();
			return sent;
		}
		if (nextRTMPMessage != null) {
			sent = true;
			try {
				pushMessage(nextRTMPMessage);
			} catch (IOException err) {
				log.error("Error while sending message.", err);
			}
			nextRTMPMessage.getBody().release();
		}
		return sent;
	}

	/**
	 * Getter for next RTMP message.
	 *
	 * @return  Next RTMP message
	 */
	protected RTMPMessage getNextRTMPMessage() {
		IMessage message;
		do {
			// Pull message from message input object...
			try {
				message = msgIn.pullMessage();
			} catch (IOException err) {
				log.error("Error while pulling message.", err);
				message = null;
			}
			// If message is null then return null
			if (message == null) {
				return null;
			}
		} while (!(message instanceof RTMPMessage));
		// Cast and return
		return (RTMPMessage) message;
	}

	/**
	 * Send VOD initialization control message
	 * @param msgIn            Message input
	 * @param start            Start timestamp
	 */
	private void sendVODInitCM(IMessageInput msgIn, int start) {
		// Create new out-of-band control message
		OOBControlMessage oobCtrlMsg = new OOBControlMessage();
		// Set passive type
		oobCtrlMsg.setTarget(IPassive.KEY);
		// Set service name of init
		oobCtrlMsg.setServiceName("init");
		// Create map for parameters
		Map<String, Object> paramMap = new HashMap<String, Object>(1);
		// Put start timestamp into Map of params
		paramMap.put("startTS", start);
		// Attach to OOB control message and send it
		oobCtrlMsg.setServiceParamMap(paramMap);
		msgIn.sendOOBControlMessage(this, oobCtrlMsg);
	}

	/**
	 * Send VOD seek control message
	 * 
	 * @param msgIn				Message input
	 * @param position			New timestamp to play from
	 */
	private void sendVODSeekCM(IMessageInput msgIn, int position) {
		OOBControlMessage oobCtrlMsg = new OOBControlMessage();
		oobCtrlMsg.setTarget(ISeekableProvider.KEY);
		oobCtrlMsg.setServiceName("seek");
		Map<String, Object> paramMap = new HashMap<String, Object>(1);
		paramMap.put("position", Integer.valueOf(position));
		oobCtrlMsg.setServiceParamMap(paramMap);
		msgIn.sendOOBControlMessage(this, oobCtrlMsg);
		synchronized (this) {
			// Reset properties
			vodStartTS = 0;
			serverStartTS = System.currentTimeMillis();
			if (nextRTMPMessage != null) {
				try {
					pushMessage(nextRTMPMessage);
				} catch (IOException err) {
					log.error("Error while sending message.", err);
				}
				nextRTMPMessage.getBody().release();
				nextRTMPMessage = null;
			}
			ResetMessage reset = new ResetMessage();
			try {
				pushMessage(reset);
			} catch (IOException err) {
				log.error("Error while sending message.", err);
			}
			scheduleNextMessage();
		}
	}

	/**
	 * Move to the next item updating the currentItemIndex.
	 * Should be called in synchronized context.
	 */
	protected void moveToNext() {
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
	protected void moveToPrevious() {
		if (currentItemIndex >= items.size()) {
			currentItemIndex = items.size() - 1;
		}
		if (controller != null) {
			currentItemIndex = controller.previousItem(this, currentItemIndex);
		} else {
			currentItemIndex = defaultController.previousItem(this, currentItemIndex);
		}
	}

	public void addStreamListener(IStreamListener listener) {
		listeners.add(listener);
	}

	public Collection<IStreamListener> getStreamListeners() {
		return listeners;
	}

	public void removeStreamListener(IStreamListener listener) {
		listeners.remove(listener);
	}
}
