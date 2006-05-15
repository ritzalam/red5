package org.red5.server.stream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.red5.server.api.IScope;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.stream.IBroadcastStreamNew;
import org.red5.server.messaging.IFilter;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IMessageOutput;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IProvider;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.message.AudioData;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.Notify;
import org.red5.server.net.rtmp.message.Status;
import org.red5.server.net.rtmp.message.VideoData;
import org.red5.server.stream.consumer.FileConsumer;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.message.StatusMessage;
import org.red5.server.stream.pipe.RefCountPushPushPipe;
import org.springframework.core.io.Resource;

public class BroadcastStreamNew
implements IBroadcastStreamNew, IFilter, IPushableConsumer,
IPipeConnectionListener, IEventDispatcher {
	private String name;
	private int streamId;
	private IConnection conn;
	
	private IMessageOutput connMsgOut;
	private IPipe livePipe;
	private IPipe recordPipe;
	
	private long startTime;
	
	public int getStreamId() {
		return streamId;
	}
	
	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

	public String getName() {
		return name;
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
		return this.conn.getScope();
	}

	public void close() {
		if (livePipe != null) {
			livePipe.unsubscribe((IProvider) this);
		}
		recordPipe.unsubscribe((IProvider) this);
	}

	public void pushMessage(IPipe pipe, IMessage message) {
		// TODO Auto-generated method stub
		
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		switch (event.getType()) {
		case PipeConnectionEvent.PROVIDER_CONNECT_PUSH:
			if (event.getProvider() == this &&
					(event.getParamMap() == null || !event.getParamMap().containsKey("record"))) {
				this.livePipe = (IPipe) event.getSource();
			}
			break;
		case PipeConnectionEvent.PROVIDER_DISCONNECT:
			if (this.livePipe == event.getSource()) {
				this.livePipe = null;
			}
			break;
		default:
			break;
		}
	}

	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
		// TODO Auto-generated method stub
		
	}

	public void saveAs(String name, boolean isAppend) {
		try {
			IScope scope = conn.getScope();
			Resource res = scope.getResource(getStreamFilename(name, ".flv"));
			if (!isAppend && res.exists()) 
				res.getFile().delete();
			
			if (!res.exists())
				res = scope.getResource(getStreamDirectory()).createRelative(name + ".flv");
			
			if (!res.exists())
				res.getFile().createNewFile();
			FileConsumer fc = new FileConsumer(scope, res.getFile());
			Map<Object, Object> paramMap = new HashMap<Object, Object>();
			if (isAppend) {
				paramMap.put("mode", "append");
			} else {
				paramMap.put("mode", "record");
			}
			recordPipe.subscribe(fc, paramMap);
		} catch (IOException e) {}
	}

	public void dispatchEvent(IEvent event) {
		if ((event.getType() != IEvent.Type.STREAM_CONTROL) &&
				(event.getType() != IEvent.Type.STREAM_DATA))
				return;
			
		dispatchEvent(event.getObject());
	}

	public void dispatchEvent(Object obj) {
		if (!(obj instanceof Message))
			return;
		
		final Message message = (Message) obj;
		long runTime = System.currentTimeMillis() - startTime;
		if (message instanceof AudioData) {
			message.setTimestamp((int)runTime);
		} else if (message instanceof VideoData) {
			message.setTimestamp((int)runTime);
		} else if (message instanceof Notify) {
			message.setTimestamp((int)runTime);
		}
		RTMPMessage msg = new RTMPMessage();
		msg.setBody(message);
		if (livePipe != null) {
			// XXX probable race condition here
			livePipe.pushMessage(msg);
		}
		recordPipe.pushMessage(msg);
		message.release();
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public IConnection getConnection() {
		return this.conn;
	}
	
	public void setConnection(IConnection conn) {
		this.conn = conn;
	}
	
	public void start() {
		IConsumerService consumerManager =
			(IConsumerService) conn.getScope().getContext().getBean(IConsumerService.KEY);
		connMsgOut = consumerManager.getConsumerOutput(this);
		recordPipe = new RefCountPushPushPipe();
		Map<Object, Object> recordParamMap = new HashMap<Object, Object>();
		recordParamMap.put("record", null);
		recordPipe.subscribe((IProvider) this, recordParamMap);
		startTime = System.currentTimeMillis();
		sendStartNotify();
	}
	
	private void sendStartNotify() {
		Status start = new Status(Status.NS_PUBLISH_START);
		start.setClientid(streamId);
		start.setDetails(name);
		
		StatusMessage startMsg = new StatusMessage();
		startMsg.setBody(start);
		connMsgOut.pushMessage(startMsg);
	}
	
	private String getStreamDirectory() {
		return "streams/";
	}
	
	private String getStreamFilename(String name, String extension) {
		String result = getStreamDirectory() + name;
		if (extension != null && !extension.equals(""))
			result += extension;
		return result;
	}
}
