package org.red5.server.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.ISubscriberStreamNew;
import org.red5.server.messaging.IConsumer;
import org.red5.server.messaging.IFilter;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IPassive;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.InMemoryPullPullPipe;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.messaging.PipeUtils;
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.net.rtmp.message.Status;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.StatusMessage;
import org.red5.server.stream.provider.FileProvider;

public class SubscriberStreamNew
implements ISubscriberStreamNew, IFilter, IPushableConsumer, IPipeConnectionListener {

	private int status;
	private IConnection conn;
	private int streamId;
	private String name;
	private List<PlayItem> playList = new ArrayList<PlayItem>();
	private int currentItem;
	private Timer listTimer;

	private IPipe sourcePipe;
	private IProvider sourceProvider;
	private IPipe downpipe;
	
	private boolean isPaused = false;
	
	public SubscriberStreamNew() {
		status = INIT;
		listTimer = new Timer(true);
	}

	public int getStreamId() {
		return streamId;
	}
	
	public void setStreamId(int id) {
		this.streamId = id;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	public int getCurrentPosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean hasAudio() {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasVideo() {
		// TODO Auto-generated method stub
		return false;
	}

	public String getVideoCodecName() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getAudioCodecName() {
		// TODO Auto-generated method stub
		return null;
	}

	public IScope getScope() {
		return conn.getScope();
	}

	public void close() {
		stop();
		playList.clear();
		currentItem = -1;
		status = INIT;
	}

	public int getCapability() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getStatus() {
		return status;
	}

	public void pause(int position) {
		if (sourceProvider instanceof IPassive && !isPaused) {
			isPaused = true;
		}
	}

	public void resume(int position) {
		if (sourceProvider instanceof IPassive && isPaused) {
			isPaused = false;
			if (sourceProvider instanceof ISeekableProvider) {
				((ISeekableProvider) sourceProvider).seek(position);
			}
			sendResetPing();
		}
	}

	public void seek(int position) {
		if (sourceProvider instanceof ISeekableProvider) {
			sendResetPing();
			((ISeekableProvider) sourceProvider).seek(position);
		}
	}

	public void stop() {
		if (sourceProvider != null) sourcePipe.unsubscribe(sourceProvider);
		status = STOPPED;
		currentItem = 0;
	}
	
	public void setConnection(IConnection conn) {
		this.conn = conn;
	}
	
	public IConnection getConnection() {
		return this.conn;
	}
		
	synchronized void start() {
		if (playList.size() == 0) {
			throw new IllegalStateException("nothing in playlist");
		}
		if (status != INIT && status != STOPPED) {
			throw new IllegalStateException("can't restart in middle of playing");
		}
		if (status == INIT) {
			// prepare the pipeline
			IConsumerService consumerManager =
				(IConsumerService) conn.getScope().getContext().getBean(IConsumerService.KEY);
			IConsumer consumer = consumerManager.getConsumer(this);
			downpipe = new InMemoryPushPushPipe();
			PipeUtils.connect(this, downpipe, consumer);
		}
		currentItem = 0;
		sendStartNotify();
		// start item 0
		PlayItem first = playList.get(currentItem);
		play(first);
		status = PLAYING;
	}
	
	/**
	 * Go ahead for next item.
	 */
	private void nextItem() {
		if (++currentItem == playList.size()) {
			// we reaches the end
			status = STOPPED;
		} else {
			if (sourceProvider != null) {
				// disconnect from the original provider first
				PipeUtils.disconnect(sourceProvider, sourcePipe, this);
				sourceProvider = null;
			}
			PlayItem item = playList.get(0);
			play(item);
		}
	}
	
	private void play(PlayItem item) {
		sendResetPing();
		// decision: 0 for Live, 1 for File, 2 for Wait, 3 for N/A
		int decision = 3;
		
		IProviderService providerManager =
			(IProviderService) conn.getScope().getContext().getApplicationContext().getBean(IProviderService.KEY);
		sourceProvider = providerManager.getProvider(conn.getScope(), item.getName());
		boolean isPublishedStream = sourceProvider != null && !(sourceProvider instanceof IPassive);
		boolean isFileStream = sourceProvider != null && sourceProvider instanceof IPassive;
		
		switch (item.getType()) {
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
			} else {
				// TODO: Wait for it, then continue with next item in playlist (?)
			}
			break;
		}
		
		switch (decision) {
		case 0:
			break;
		case 1:
			if (sourceProvider instanceof FileProvider) {
				FileProvider fileProvider = (FileProvider) sourceProvider;
				fileProvider.setStart(item.getType());
				fileProvider.setEnd(item.getType() + item.getLength());
			}
			sourcePipe = new InMemoryPullPullPipe();
			PipeUtils.connect(sourceProvider, sourcePipe, this);
			pullAndPush();
			break;
		case 2:
			break;
		default:
			break;
		}
	}

	public void addPlayItem(String name, int type, int length) {
		PlayItem item = new PlayItem(name, type, length);
		playList.add(item);
	}
	
	public void written(Message msg) {
		pullAndPush();
	}
	
	public void pushMessage(IPipe pipe, IMessage message) {
		// TODO Auto-generated method stub
		
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
		case PipeConnectionEvent.PROVIDER_DISCONNECT:
			if (sourceProvider == event.getProvider()) {
				sourceProvider = null;
			}
			break;
		default:
			break;
		}
	}
	
	private void pullAndPush() {
		if (!isPaused) {
			IMessage msg = sourcePipe.pullMessage();
			if (msg != null) downpipe.pushMessage(msg);
		}
	}

	private void sendStartNotify() {
		Status reset = new Status(Status.NS_PLAY_RESET);
		Status start = new Status(Status.NS_PLAY_START);
		reset.setClientid(streamId);
		start.setClientid(streamId);
		reset.setDetails(name);
		start.setDetails(name);

		// This hack fixes the on meta data problem
		// TODO: Perhaps its a good idea to init each channel with a blank audio packet
		AudioData blankAudio = new AudioData();
		
		RTMPMessage blankAudioMsg = new RTMPMessage();
		blankAudioMsg.setBody(blankAudio);
		downpipe.pushMessage(blankAudioMsg);

		StatusMessage resetMsg = new StatusMessage();
		resetMsg.setBody(reset);
		downpipe.pushMessage(resetMsg);
		
		StatusMessage startMsg = new StatusMessage();
		startMsg.setBody(start);
		downpipe.pushMessage(startMsg);
	}
	
	private void sendResetPing() {
		Ping ping1 = new Ping();
		ping1.setValue1((short) 4);
		ping1.setValue2(streamId);

		RTMPMessage ping1Msg = new RTMPMessage();
		ping1Msg.setBody(ping1);
		downpipe.pushMessage(ping1Msg);
		
		Ping ping2 = new Ping();
		ping2.setValue1((short) 0);
		ping2.setValue2(streamId);
		
		RTMPMessage ping2Msg = new RTMPMessage();
		ping2Msg.setBody(ping2);
		downpipe.pushMessage(ping2Msg);
	}
	
	public class PlayItem {
		private String name;
		private int type;
		private int length;
		
		public PlayItem(String name, int type, int length) {
			this.name = name;
			this.type = type;
			this.length = length;
		}

		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}
		
	}
	
	private class PlayListTimerTask extends TimerTask {

		@Override
		public void run() {
			
		}
		
	}
}
