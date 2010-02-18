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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.statistics.IPlaylistSubscriberStreamStatistics;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistController;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.OperationNotSupportedException;
import org.red5.server.api.stream.StreamState;
import org.slf4j.Logger;

/**
 * Stream of playlist subscriber
 */
public class PlaylistSubscriberStream extends AbstractClientStream implements IPlaylistSubscriberStream,
		IPlaylistSubscriberStreamStatistics {

	private static final Logger log = Red5LoggerFactory.getLogger(PlaylistSubscriberStream.class);

	private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

	private final Lock read = readWriteLock.readLock();

	private final Lock write = readWriteLock.writeLock();

	/**
	 * Playlist controller
	 */
	private IPlaylistController controller;

	/**
	 * Default playlist controller
	 */
	private IPlaylistController defaultController;

	/**
	 * Playlist items
	 */
	private final List<IPlayItem> items;

	/**
	 * Current item index
	 */
	private int currentItemIndex = 0;

	/**
	 * Plays items back
	 */
	protected PlayEngine engine;

	/**
	 * Rewind mode state
	 */
	protected boolean rewind;

	/**
	 * Random mode state
	 */
	protected boolean random;

	/**
	 * Repeat mode state
	 */
	protected boolean repeat;

	/**
	 * Executor that will be used to schedule stream playback to keep
	 * the client buffer filled.
	 */
	protected static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(16);

	/**
	 * Interval in ms to check for buffer underruns in VOD streams.
	 */
	protected int bufferCheckInterval = 0;

	/**
	 * Number of pending messages at which a <code>NetStream.Play.InsufficientBW</code>
	 * message is generated for VOD streams.
	 */
	protected int underrunTrigger = 10;

	/**
	 * Timestamp this stream was created.
	 */
	protected long creationTime = System.currentTimeMillis();

	/**
	 * Number of bytes sent.
	 */
	protected long bytesSent = 0;

	/** Constructs a new PlaylistSubscriberStream. */
	public PlaylistSubscriberStream() {
		defaultController = new SimplePlaylistController();
		items = new ArrayList<IPlayItem>();
	}

	/**
	 * Creates a play engine based on current services (scheduling service, consumer service, and provider service).
	 * This method is useful during unit testing.
	 */
	PlayEngine createEngine(ISchedulingService schedulingService, IConsumerService consumerService,
			IProviderService providerService) {
		engine = new PlayEngine.Builder(this, schedulingService, consumerService, providerService).build();
		return engine;
	}

	/**
	 * Set the executor to use.
	 * 
	 * @param executor the executor
	 */
	public void setExecutor(ScheduledThreadPoolExecutor executor) {
		PlaylistSubscriberStream.executor = executor;
	}

	/**
	 * Return the executor to use.
	 * 
	 * @return the executor
	 */
	public ScheduledThreadPoolExecutor getExecutor() {
		if (executor == null) {
			log.warn("ScheduledThreadPoolExecutor was null on request");
		}
		return executor;
	}

	/**
	 * Set interval to check for buffer underruns. Set to <code>0</code> to
	 * disable.
	 * 
	 * @param bufferCheckInterval interval in ms
	 */
	public void setBufferCheckInterval(int bufferCheckInterval) {
		this.bufferCheckInterval = bufferCheckInterval;
	}

	/**
	 * Set maximum number of pending messages at which a
	 * <code>NetStream.Play.InsufficientBW</code> message will be
	 * generated for VOD streams
	 * 
	 * @param underrunTrigger the maximum number of pending messages
	 */
	public void setUnderrunTrigger(int underrunTrigger) {
		this.underrunTrigger = underrunTrigger;
	}

	/** {@inheritDoc} */
	public void start() {
		//ensure the play engine exists
		if (engine == null) {
			IScope scope = getScope();
			if (scope != null) {
				IContext ctx = scope.getContext();
				ISchedulingService schedulingService = (ISchedulingService) ctx.getBean(ISchedulingService.BEAN_NAME);
				IConsumerService consumerService = (IConsumerService) ctx.getBean(IConsumerService.KEY);
				IProviderService providerService = (IProviderService) ctx.getBean(IProviderService.BEAN_NAME);

				engine = new PlayEngine.Builder(this, schedulingService, consumerService, providerService).build();
			} else {
				log.info("Scope was null on start");
			}
		}
		//set buffer check interval
		engine.setBufferCheckInterval(bufferCheckInterval);
		//set underrun trigger
		engine.setUnderrunTrigger(underrunTrigger);
		// Start playback engine
		engine.start();
		// Notify subscribers on start
		onChange(StreamState.STARTED);
	}

	/** {@inheritDoc} */
	public void play() throws IOException {
		// Check how many is yet to play...
		int count = items.size();
		// Return if playlist is empty
		if (count == 0) {
			return;
		}
		// Move to next if current item is set to -1
		if (currentItemIndex == -1) {
			moveToNext();
		}
		// If there's some more items on list then play current item
		while (count-- > 0) {
			IPlayItem item = null;
			read.lock();
			try {
				// Get playlist item
				item = items.get(currentItemIndex);
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
			} finally {
				read.unlock();
			}
		}
	}

	/** {@inheritDoc} */
	public void pause(int position) {
		try {
			engine.pause(position);
		} catch (IllegalStateException e) {
			log.debug("pause caught an IllegalStateException");
		}
	}

	/** {@inheritDoc} */
	public void resume(int position) {
		try {
			engine.resume(position);
		} catch (IllegalStateException e) {
			log.debug("resume caught an IllegalStateException");
		}
	}

	/** {@inheritDoc} */
	public void stop() {
		try {
			engine.stop();
		} catch (IllegalStateException e) {
			log.debug("stop caught an IllegalStateException");
		}
	}

	/** {@inheritDoc} */
	public void seek(int position) throws OperationNotSupportedException {
		try {
			engine.seek(position);
		} catch (IllegalStateException e) {
			log.debug("seek caught an IllegalStateException");
		}
	}

	/** {@inheritDoc} */
	public void close() {
		engine.close();
		onChange(StreamState.CLOSED);
		items.clear();
	}

	/** {@inheritDoc} */
	public boolean isPaused() {
		return engine.isPaused();
	}

	/** {@inheritDoc} */
	public void addItem(IPlayItem item) {
		write.lock();
		try {
			items.add(item);
		} finally {
			write.unlock();
		}
	}

	/** {@inheritDoc} */
	public void addItem(IPlayItem item, int index) {
		write.lock();
		try {
			items.add(index, item);
		} finally {
			write.unlock();
		}
	}

	/** {@inheritDoc} */
	public void removeItem(int index) {
		if (index < 0 || index >= items.size()) {
			return;
		}
		int originSize = items.size();
		write.lock();
		try {
			items.remove(index);
		} finally {
			write.unlock();
		}
		if (currentItemIndex == index) {
			// set the next item.
			if (index == originSize - 1) {
				currentItemIndex = index - 1;
			}
		}
	}

	/** {@inheritDoc} */
	public void removeAllItems() {
		// we try to stop the engine first
		stop();
		write.lock();
		try {
			items.clear();
		} finally {
			write.unlock();
		}
	}

	/** {@inheritDoc} */
	public void previousItem() {
		stop();
		moveToPrevious();
		if (currentItemIndex == -1) {
			return;
		}
		IPlayItem item = null;
		int count = items.size();
		while (count-- > 0) {
			read.lock();
			try {
				item = items.get(currentItemIndex);
				engine.play(item);
				break;
			} catch (IOException err) {
				log.error("Error while starting to play item, moving to previous.", err);
				// go for next item
				moveToPrevious();
				if (currentItemIndex == -1) {
					// we reaches the end.
					break;
				}
			} catch (StreamNotFoundException e) {
				// go for next item
				moveToPrevious();
				if (currentItemIndex == -1) {
					// we reaches the end.
					break;
				}
			} catch (IllegalStateException e) {
				// an stream is already playing
				break;
			} finally {
				read.unlock();
			}
		}
	}

	/** {@inheritDoc} */
	public boolean hasMoreItems() {
		int nextItem = currentItemIndex + 1;
		if (nextItem >= items.size() && !repeat) {
			return false;
		} else {
			return true;
		}
	}

	/** {@inheritDoc} */
	public void nextItem() {
		moveToNext();
		if (currentItemIndex == -1) {
			return;
		}
		IPlayItem item = null;
		int count = items.size();
		while (count-- > 0) {
			read.lock();
			try {
				item = items.get(currentItemIndex);
				engine.play(item, false);
				break;
			} catch (IOException err) {
				log.error("Error while starting to play item, moving to next", err);
				// go for next item
				moveToNext();
				if (currentItemIndex == -1) {
					// we reaches the end.
					break;
				}
			} catch (StreamNotFoundException e) {
				// go for next item
				moveToNext();
				if (currentItemIndex == -1) {
					// we reaches the end.
					break;
				}
			} catch (IllegalStateException e) {
				// an stream is already playing
				break;
			} finally {
				read.unlock();
			}
		}
	}

	/** {@inheritDoc} */
	public void setItem(int index) {
		if (index < 0 || index >= items.size()) {
			return;
		}
		stop();
		currentItemIndex = index;
		read.lock();
		try {
			IPlayItem item = items.get(currentItemIndex);
			engine.play(item);
		} catch (IOException e) {
			log.error("setItem caught a IOException", e);
		} catch (StreamNotFoundException e) {
			// let the engine retain the STOPPED state
			// and wait for control from outside
			log.debug("setItem caught a StreamNotFoundException");
		} catch (IllegalStateException e) {
			log.error("Illegal state exception on playlist item setup", e);
		} finally {
			read.unlock();
		}
	}

	/** {@inheritDoc} */
	public boolean isRandom() {
		return random;
	}

	/** {@inheritDoc} */
	public void setRandom(boolean random) {
		this.random = random;
	}

	/** {@inheritDoc} */
	public boolean isRewind() {
		return rewind;
	}

	/** {@inheritDoc} */
	public void setRewind(boolean rewind) {
		this.rewind = rewind;
	}

	/** {@inheritDoc} */
	public boolean isRepeat() {
		return repeat;
	}

	/** {@inheritDoc} */
	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	/**
	 * Seek to current position to restart playback with audio and/or video.
	 */
	private void seekToCurrentPlayback() {
		if (engine.isPullMode()) {
			try {
				// TODO: figure out if this is the correct position to seek to
				final long delta = System.currentTimeMillis() - engine.getPlaybackStart();
				engine.seek((int) delta);
			} catch (OperationNotSupportedException err) {
				// Ignore error, should not happen for pullMode engines
			}
		}
	}

	/** {@inheritDoc} */
	public void receiveVideo(boolean receive) {
		boolean receiveVideo = engine.receiveVideo(receive);
		if (!receiveVideo && receive) {
			//video has been re-enabled
			seekToCurrentPlayback();
		}
	}

	/** {@inheritDoc} */
	public void receiveAudio(boolean receive) {
		//check if engine currently receives audio, returns previous value
		boolean receiveAudio = engine.receiveAudio(receive);
		if (receiveAudio && !receive) {
			//send a blank audio packet to reset the player
			engine.sendBlankAudio(true);
		} else if (!receiveAudio && receive) {
			//do a seek	
			seekToCurrentPlayback();
		}
	}

	/** {@inheritDoc} */
	public void setPlaylistController(IPlaylistController controller) {
		this.controller = controller;
	}

	/** {@inheritDoc} */
	public int getItemSize() {
		return items.size();
	}

	/** {@inheritDoc} */
	public int getCurrentItemIndex() {
		return currentItemIndex;
	}

	/**
	 * {@inheritDoc}
	 */
	public IPlayItem getCurrentItem() {
		return getItem(getCurrentItemIndex());
	}

	/** {@inheritDoc} */
	public IPlayItem getItem(int index) {
		read.lock();
		try {
			return items.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		} finally {
			read.unlock();
		}
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
	 * {@inheritDoc}
	 */
	public void onChange(final StreamState state, final Object... changed) {
		Notifier notifier = null;
		IStreamAwareScopeHandler handler = getStreamAwareHandler();
		switch (state) {
			case SEEK:
				//notifies subscribers on seek
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void run() {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get item being played
							IPlayItem item = (IPlayItem) changed[0];
							//seek position
							int position = (Integer) changed[1];
							try {
								handler.streamPlaylistVODItemSeek(stream, item, position);
							} catch (Throwable t) {
								log.error("error notify streamPlaylistVODItemSeek", t);
							}
						}
					};
				}
				break;
			case PAUSED:
				//set the paused state
				this.setState(StreamState.PAUSED);
				//notifies subscribers on pause
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void run() {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get item being played
							IPlayItem item = (IPlayItem) changed[0];
							//playback position
							int position = (Integer) changed[1];
							try {
								handler.streamPlaylistVODItemPause(stream, item, position);
							} catch (Throwable t) {
								log.error("error notify streamPlaylistVODItemPause", t);
							}
						}
					};
				}
				break;
			case RESUMED:
				//resume playing
				this.setState(StreamState.PLAYING);
				//notifies subscribers on resume
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void run() {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get item being played
							IPlayItem item = (IPlayItem) changed[0];
							//playback position
							int position = (Integer) changed[1];
							try {
								handler.streamPlaylistVODItemResume(stream, item, position);
							} catch (Throwable t) {
								log.error("error notify streamPlaylistVODItemResume", t);

							}
						}
					};
				}
				break;
			case PLAYING:
				//notifies subscribers on play
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void run() {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get item being played
							IPlayItem item = (IPlayItem) changed[0];
							//is it a live broadcast
							boolean isLive = (Boolean) changed[1];
							try {
								handler.streamPlaylistItemPlay(stream, item, isLive);
							} catch (Throwable t) {
								log.error("error notify streamPlaylistItemPlay", t);
							}
						}
					};
				}
				break;
			case CLOSED:
				//notifies subscribers on close
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void run() {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							try {
								handler.streamSubscriberClose(stream);
							} catch (Throwable t) {
								log.error("error notify streamSubscriberClose", t);
							}
						}
					};
				}
				break;
			case STARTED:
				//notifies subscribers on start
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void run() {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							try {
								handler.streamSubscriberStart(stream);
							} catch (Throwable t) {
								log.error("error notify streamSubscriberStart", t);
							}
						}
					};
				}
				break;
			case STOPPED:
				//set the stopped state
				this.setState(StreamState.STOPPED);
				//notifies subscribers on stop
				if (handler != null) {
					notifier = new Notifier(this, handler) {
						public void run() {
							//make sure those notified have the correct connection
							Red5.setConnectionLocal(conn);
							//get the item that was stopped
							IPlayItem item = (IPlayItem) changed[0];
							try {
								handler.streamPlaylistItemStop(stream, item);
							} catch (Throwable t) {
								log.error("error notify streamPlaylistItemStop", t);
							}
						}
					};
				}
				break;
			case END:
				//notified by the play engine when the current item reaches the end
				nextItem();
				break;
			default:
				//there is no "default" handling
		}
		if (notifier != null) {
			IConnection conn = Red5.getConnectionLocal();
			notifier.setConnection(conn);
			executor.execute(notifier);
		}
	}

	/** {@inheritDoc} */
	public IPlaylistSubscriberStreamStatistics getStatistics() {
		return this;
	}

	/** {@inheritDoc} */
	public long getCreationTime() {
		return creationTime;
	}

	/** {@inheritDoc} */
	public int getCurrentTimestamp() {
		int lastMessageTs = engine.getLastMessageTimestamp();
		if (lastMessageTs >= 0) {
			return 0;
		}
		return lastMessageTs;
	}

	/** {@inheritDoc} */
	public long getBytesSent() {
		return bytesSent;
	}

	/** {@inheritDoc} */
	public double getEstimatedBufferFill() {
		//check to see if any messages have been sent
		int lastMessageTs = engine.getLastMessageTimestamp();
		if (lastMessageTs < 0) {
			// Nothing has been sent yet
			return 0.0;
		}

		// Buffer size as requested by the client
		final long buffer = getClientBufferDuration();
		if (buffer == 0) {
			return 100.0;
		}

		// Duration the stream is playing
		final long delta = System.currentTimeMillis() - engine.getPlaybackStart();
		// Expected amount of data present in client buffer
		final long buffered = lastMessageTs - delta;
		return (buffered * 100.0) / buffer;
	}

	/** {@inheritDoc} */
	@Override
	public StreamState getState() {
		return state;
	}

	/** {@inheritDoc} */
	@Override
	public void setState(StreamState state) {
		this.state = state;
	}

	/**
	 * Handles notifications in a separate thread.
	 */
	public class Notifier implements Runnable {

		IPlaylistSubscriberStream stream;

		IStreamAwareScopeHandler handler;
		
		IConnection conn;

		public Notifier(IPlaylistSubscriberStream stream, IStreamAwareScopeHandler handler) {
			log.trace("Notifier - stream: {} handler: {}", stream, handler);
			this.stream = stream;
			this.handler = handler;
		}
		
		public void setConnection(IConnection conn) {
			this.conn = conn;
		}

		public void run() {
		}

	}

}
