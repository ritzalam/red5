package org.red5.server.stream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.stream.ISubscriberStreamNew;
import org.red5.server.messaging.IConsumer;
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
import org.red5.server.messaging.InMemoryPullPullPipe;
import org.red5.server.messaging.InMemoryPushPushPipe;
import org.red5.server.messaging.OOBControlMessage;
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

	private IMessageOutput msgOut;
	private IMessageInput msgIn;
	private boolean isPullMode = false;
	
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
		if (isPullMode && !isPaused) {
			isPaused = true;
		}
	}

	public void resume(int position) {
		if (msgIn == null) return;
		if (isPullMode && isPaused) {
			isPaused = false;
			sendVODSeekCM(msgIn, position);
			sendResetPing();
		}
	}

	public void seek(int position) {
		if (msgIn == null) return;
		if (isPullMode) {
			sendResetPing();
			sendVODSeekCM(msgIn, position);
		}
	}

	public void stop() {
		if (msgIn != null) {
			msgIn.unsubscribe(this);
			msgIn = null;
		}
		status = STOPPED;
		currentItem = 0;
	}
	
	public void setConnection(IConnection conn) {
		this.conn = conn;
	}
	
	public IConnection getConnection() {
		return this.conn;
	}
	
	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
		// TODO Auto-generated method stub
		
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
			msgOut = consumerManager.getConsumerOutput(this);
			msgOut.subscribe(this, null);
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
			if (msgIn != null) {
				msgIn.unsubscribe(this);
				msgIn = null;
			}
			PlayItem item = playList.get(0);
			play(item);
		}
	}
	
	private void play(PlayItem item) {
		sendResetPing();
		// decision: 0 for Live, 1 for File, 2 for Wait, 3 for N/A
		int decision = 3;
		
		IProviderService providerService =
			(IProviderService) conn.getScope().getContext().getApplicationContext().getBean(IProviderService.KEY);
		IMessageInput liveInput = providerService.getLiveProviderInput(conn.getScope(), item.getName(), false);
		IMessageInput vodInput = providerService.getVODProviderInput(conn.getScope(), item.getName());
		boolean isPublishedStream = liveInput != null;
		boolean isFileStream = vodInput != null;
		
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
		if (decision == 2) liveInput = providerService.getLiveProviderInput(
				conn.getScope(), item.getName(), true);
		
		switch (decision) {
		case 0:
		case 2:
			msgIn = liveInput;
			msgIn.subscribe(this, null);
			break;
		case 1:
			msgIn = vodInput;
			msgIn.subscribe(this, null);
			sendVODInitCM(msgIn, item);
			pullAndPush();
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
		if (isPullMode) pullAndPush();
	}
	
	public void pushMessage(IPipe pipe, IMessage message) {
		msgOut.pushMessage(message);
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
		case PipeConnectionEvent.PROVIDER_DISCONNECT:
			if (event.getProvider() != this) {
				if (msgIn != null) {
					msgIn.unsubscribe(this);
					msgIn = null;
				}
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
	
	private void pullAndPush() {
		if (!isPaused) {
			IMessage msg = msgIn.pullMessage();
			if (msg != null) msgOut.pushMessage(msg);
		}
	}

	private void sendVODInitCM(IMessageInput msgIn, PlayItem item) {
		OOBControlMessage oobCtrlMsg = new OOBControlMessage();
		oobCtrlMsg.setTarget(IPassive.KEY);
		oobCtrlMsg.setServiceName("init");
		Map<Object, Object> paramMap = new HashMap<Object, Object>();
		paramMap.put("startTS", new Integer(item.getType()));
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
		msgOut.pushMessage(blankAudioMsg);

		StatusMessage resetMsg = new StatusMessage();
		resetMsg.setBody(reset);
		msgOut.pushMessage(resetMsg);
		
		StatusMessage startMsg = new StatusMessage();
		startMsg.setBody(start);
		msgOut.pushMessage(startMsg);
	}
	
	private void sendResetPing() {
		Ping ping1 = new Ping();
		ping1.setValue1((short) 4);
		ping1.setValue2(streamId);

		RTMPMessage ping1Msg = new RTMPMessage();
		ping1Msg.setBody(ping1);
		msgOut.pushMessage(ping1Msg);
		
		Ping ping2 = new Ping();
		ping2.setValue1((short) 0);
		ping2.setValue2(streamId);
		
		RTMPMessage ping2Msg = new RTMPMessage();
		ping2Msg.setBody(ping2);
		msgOut.pushMessage(ping2Msg);
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
