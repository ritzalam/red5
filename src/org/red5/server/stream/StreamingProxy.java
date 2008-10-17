package org.red5.server.stream;

import java.io.IOException;

import org.red5.server.net.rtmp.RTMPClient;
import org.red5.server.net.rtmp.INetStreamEventHandler;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.messaging.IMessage;
import org.red5.server.messaging.IMessageComponent;
import org.red5.server.messaging.IPipe;
import org.red5.server.messaging.IPipeConnectionListener;
import org.red5.server.messaging.IPushableConsumer;
import org.red5.server.messaging.OOBControlMessage;
import org.red5.server.messaging.PipeConnectionEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.message.RTMPMessage;

/**
 * A proxy to publish stream from server to server.
 * TODO: Use timer to monitor the connect/stream creation.
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class StreamingProxy
implements IPushableConsumer, IPipeConnectionListener,
		INetStreamEventHandler, IPendingServiceCallback {
	private static final int STOPPED = 0;
	private static final int CONNECTING = 1;
	private static final int STREAM_CREATING = 2;
	private static final int PUBLISHING = 3;
	private static final int PUBLISHED = 4;
	
	private String host;
	private int port;
	private String app;
	private RTMPClient rtmpClient;
	private int state;
	private String publishName;
	private int streamId;
	
	public void init() {
		rtmpClient = new RTMPClient();
		state = STOPPED;
	}
	
	synchronized public void start(String publishName) {
		state = CONNECTING;
		this.publishName = publishName;
		rtmpClient.connect(host, port, app, this);
	}
	
	synchronized public void stop() {
		if (state >= STREAM_CREATING) {
			rtmpClient.disconnect();
		}
		state = STOPPED;
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		// nothing to do
	}

	synchronized public void pushMessage(IPipe pipe, IMessage message) throws IOException {
		if (state >= PUBLISHED && message instanceof RTMPMessage) {
			RTMPMessage rtmpMsg = (RTMPMessage) message;
			rtmpClient.publishStreamData(streamId, rtmpMsg);
		}
	}

	public void onOOBControlMessage(IMessageComponent source, IPipe pipe,
			OOBControlMessage oobCtrlMsg) {
		// TODO Auto-generated method stub
		
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setApp(String app) {
		this.app = app;
	}

	synchronized public void onStreamEvent(Notify notify) {
		ObjectMap<?, ?> map = (ObjectMap<?, ?>) notify.getCall().getArguments()[0];
		String code = (String) map.get("code");
		if (StatusCodes.NS_PUBLISH_START.equals(code)) {
			state = PUBLISHED;
		}
	}

	synchronized public void resultReceived(IPendingServiceCall call) {
		if ("connect".equals(call.getServiceMethodName())) {
			state = STREAM_CREATING;
			rtmpClient.createStream(this);
		} else if ("createStream".equals(call.getServiceMethodName())) {
			state = PUBLISHING;
			Object result = call.getResult();
			if (result instanceof Integer) {
				Integer streamIdInt = (Integer) result;
				streamId = streamIdInt.intValue();
				rtmpClient.publish(streamIdInt.intValue(), publishName, "live", this);
			} else {
				rtmpClient.disconnect();
				state = STOPPED;
			}
		}
	}
}
