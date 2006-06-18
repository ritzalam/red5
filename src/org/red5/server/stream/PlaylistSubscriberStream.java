package org.red5.server.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IPlaylistController;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
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
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.net.rtmp.message.Status;
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
		engine.start();
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
	
	/**
	 * Notified by RTMPHandler when a message has been sent.
	 * Glue for old code base.
	 * @param message
	 */
	public void written(Message message) {
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
	
	/**
	 * A play engine for playing an IPlayItem.
	 */
	private class PlayEngine extends TimerTask
	implements IFilter, IPushableConsumer, IPipeConnectionListener {
		// XXX shall we make this as a configurable property?
		private static final long LIVE_WAIT_TIMEOUT = 5000;
		
		private State state;
		
		private IMessageInput msgIn;
		private IMessageOutput msgOut;
		
		private boolean isPullMode = false;
		
		private Timer timer;
		private boolean isWaiting = false;
		private int vodStartTS = 0;
		
		private IPlayItem currentItem;
		private int currentVideoTS = 0;
		private int currentAudioTS = 0;
		private int currentDataTS = 0;
		
		public PlayEngine() {
			state = State.UNINIT;
		}
		
		synchronized public void start() {
			if (state != State.UNINIT) throw new IllegalStateException();
			state = State.STOPPED;
			IConsumerService consumerManager =
				(IConsumerService) getScope().getContext().getBean(IConsumerService.KEY);
			msgOut = consumerManager.getConsumerOutput(PlaylistSubscriberStream.this);
			msgOut.subscribe(this, null);
			timer = new Timer(true);
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
			sendResetPing();
			sendResetStatus(item);
			sendStartStatus(item);
			switch (decision) {
			case 0:
				msgIn = liveInput;
				msgIn.subscribe(this, null);
				if (item.getLength() >= 0) {
					timer.schedule(this, item.getLength());
				}
				break;
			case 2:
				msgIn = liveInput;
				msgIn.subscribe(this, null);
				isWaiting = true;
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						isWaiting = false;
						onItemEnd();
					}
				}, LIVE_WAIT_TIMEOUT);
				break;
			case 1:
				msgIn = vodInput;
				msgIn.subscribe(this, null);

				break;
			default:
				throw new StreamNotFoundException(item.getName());
			}
			state = State.PLAYING;
			if (decision == 1) {
				sendVODInitCM(msgIn, item);
				vodStartTS = -1;
				pullAndPush();
			}
		}
		
		synchronized public void pause(int position) throws IllegalStateException {
			if (state != State.PLAYING) throw new IllegalStateException();
			if (isPullMode) {
				state = State.PAUSED;
				sendPauseStatus(currentItem);
			}
		}
		
		synchronized public void resume(int position) throws IllegalStateException {
			if (state != State.PAUSED) throw new IllegalStateException();
			if (isPullMode) {
				state = State.PLAYING;
				sendVODSeekCM(msgIn, position);
				sendResetPing();
				sendResumeStatus(currentItem);
			}
		}
		
		synchronized public void seek(int position) throws IllegalStateException {
			if (state != State.PLAYING && state != State.PAUSED) {
				throw new IllegalStateException();
			}
			if (isPullMode) {
				sendResetPing();
				sendSeekStatus(currentItem, position);
				sendStartStatus(currentItem);
				sendVODSeekCM(msgIn, position);
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
			timer.purge();
		}
		
		synchronized public void close() {
			if (state == State.PLAYING || state == State.PAUSED) {
				if (msgIn != null) {
					msgIn.unsubscribe(this);
					msgIn = null;
				}
			}
			state = State.CLOSED;
			timer.cancel();
		}
		
		synchronized private void pullAndPush() {
			if (state == State.PLAYING && isPullMode) {
				IMessage msg = msgIn.pullMessage();
				if (vodStartTS == -1) {
					if (msg instanceof RTMPMessage) {
						vodStartTS = ((RTMPMessage) msg).getBody().getTimestamp();
						System.out.println("Init vodStartTS" + vodStartTS);
					}
				} else {
					if (msg instanceof RTMPMessage) {
						if (currentItem.getLength() >= 0) {
							int diff = ((RTMPMessage) msg).getBody().getTimestamp() - vodStartTS;
							System.out.println("vod length, " + currentItem.getLength() + " vod diff " + diff);
							if (diff > currentItem.getLength()) {
								// stop this item
								state = State.STOPPED;
								onItemEnd();
							}
						}
					}
				}
				if (msg != null) msgOut.pushMessage(msg);
			}
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
			
			AudioData blankAudio = new AudioData();
			blankAudio.setTimestamp(0);
			
			RTMPMessage blankAudioMsg = new RTMPMessage();
			blankAudioMsg.setBody(blankAudio);
			msgOut.pushMessage(blankAudioMsg);
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
						timer.purge();
						if (currentItem.getLength() >= 0) {
							timer.schedule(this, currentItem.getLength());
						}
						isWaiting = false;
					}
				}
				break;
			case PipeConnectionEvent.PROVIDER_DISCONNECT:
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
			msgOut.pushMessage(message);
		}

		@Override
		public void run() {
			synchronized (this) {
				if (msgIn != null) {
					msgIn.unsubscribe(this);
					msgIn = null;
				}
				state = State.STOPPED;
			}
			onItemEnd();
		}
		
	}
	
	private class StreamNotFoundException extends Exception {
		private static final long serialVersionUID = 812106823615971891L;

		public StreamNotFoundException(String name) {
			super("Stream " + name + " not found.");
		}
		
	}
}