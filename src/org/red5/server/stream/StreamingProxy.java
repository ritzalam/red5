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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import org.red5.server.net.rtmp.INetStreamEventHandler;
import org.red5.server.net.rtmp.RTMPClient;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.stream.message.RTMPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proxy to publish stream from server to server.
 *
 * TODO: Use timer to monitor the connect/stream creation.
 *
 * @author Steven Gong (steven.gong@gmail.com)
 * @author Andy Shaules (bowljoman@hotmail.com)
 */
public class StreamingProxy implements IPushableConsumer, IPipeConnectionListener, INetStreamEventHandler, IPendingServiceCallback {

	private static Logger log = LoggerFactory.getLogger(StreamingProxy.class);

	private List<IMessage> frameBuffer = new ArrayList<IMessage>();

	public static final String LIVE = "live";

	public static final String RECORD = "record";

	public static final String APPEND = "append";

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

	private String publishMode;

	public void init() {
		rtmpClient = new RTMPClient();
		state = STOPPED;
	}

	public synchronized void start(String publishName, String publishMode, Object[] params) {
		state = CONNECTING;
		this.publishName = publishName;
		this.publishMode = publishMode;

		Map<String, Object> defParams = rtmpClient.makeDefaultConnectionParams(host, port, app);
		rtmpClient.connect(host, port, defParams, this, params);
	}

	public synchronized void stop() {
		if (state >= STREAM_CREATING) {
			rtmpClient.disconnect();
		}
		state = STOPPED;
	}

	public void onPipeConnectionEvent(PipeConnectionEvent event) {
		// nothing to do
	}

	public synchronized void pushMessage(IPipe pipe, IMessage message) throws IOException {
		if (state >= PUBLISHED && message instanceof RTMPMessage) {
			RTMPMessage rtmpMsg = (RTMPMessage) message;
			rtmpClient.publishStreamData(streamId, rtmpMsg);
		} else {
			frameBuffer.add(message);
		}
	}

	public void onOOBControlMessage(IMessageComponent source, IPipe pipe, OOBControlMessage oobCtrlMsg) {
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

	public synchronized void onStreamEvent(Notify notify) {
		log.debug("onStreamEvent: {}", notify);
		ObjectMap<?, ?> map = (ObjectMap<?, ?>) notify.getCall().getArguments()[0];
		String code = (String) map.get("code");
		log.debug("<:{}", code);
		if (StatusCodes.NS_PUBLISH_START.equals(code)) {
			state = PUBLISHED;
			rtmpClient.invoke("FCPublish", new Object[] { publishName }, this);
			while (frameBuffer.size() > 0) {
				rtmpClient.publishStreamData(streamId, frameBuffer.remove(0));
			}
		}
	}

	public synchronized void resultReceived(IPendingServiceCall call) {
		log.debug("resultReceived:> {}", call.getServiceMethodName());
		if ("connect".equals(call.getServiceMethodName())) {
			state = STREAM_CREATING;
			rtmpClient.createStream(this);
		} else if ("createStream".equals(call.getServiceMethodName())) {
			state = PUBLISHING;
			Object result = call.getResult();
			if (result instanceof Integer) {
				Integer streamIdInt = (Integer) result;
				streamId = streamIdInt.intValue();
				log.debug("Publishing: {}", state);
				rtmpClient.publish(streamIdInt.intValue(), publishName, publishMode, this);
			} else {
				rtmpClient.disconnect();
				state = STOPPED;
			}
		}
	}
}
