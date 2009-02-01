package org.red5.server.net.rtmp;

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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.IConnection;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.service.IServiceInvoker;
import org.red5.server.api.so.IClientSharedObject;
import org.red5.server.messaging.IMessage;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPCodecFactory;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.service.Call;
import org.red5.server.service.MethodNotFoundException;
import org.red5.server.service.PendingCall;
import org.red5.server.service.ServiceInvoker;
import org.red5.server.so.ClientSharedObject;
import org.red5.server.so.SharedObjectMessage;
import org.red5.server.stream.AbstractClientStream;
import org.red5.server.stream.OutputStream;
import org.red5.server.stream.consumer.ConnectionConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for clients (RTMP and RTMPT)
 */
public abstract class BaseRTMPClientHandler extends BaseRTMPHandler {

	private static final Logger log = LoggerFactory
			.getLogger(BaseRTMPClientHandler.class);

	/**
	 * Connection parameters
	 */
	private Map<String, Object> connectionParams;

	/**
	 * Connect call arguments
	 */
	private Object[] connectArguments = null;

	/**
	 * Connection callback
	 */
	private IPendingServiceCallback connectCallback;

	/**
	 * Service provider
	 */
	private Object serviceProvider;

	/**
	 * Service invoker
	 */
	private IServiceInvoker serviceInvoker = new ServiceInvoker();

	/**
	 * Shared objects map
	 */
	private ConcurrentMap<String, ClientSharedObject> sharedObjects = new ConcurrentHashMap<String, ClientSharedObject>();

	private final RTMPClientConnManager connManager = new RTMPClientConnManager();

	private ConcurrentMap<Object, NetStreamPrivateData> streamDataMap = new ConcurrentHashMap<Object, NetStreamPrivateData>();

	/**
	 * Task to start on connection close
	 */
	private Runnable connectionClosedHandler;

	/**
	 * Task to start on connection errors
	 */
	private ClientExceptionHandler exceptionHandler;

	private RTMPCodecFactory codecFactory;

	private IEventDispatcher streamEventDispatcher = null;

	protected BaseRTMPClientHandler() {
		codecFactory = new RTMPCodecFactory();
		codecFactory.setDeserializer(new Deserializer());
		codecFactory.setSerializer(new Serializer());
		codecFactory.init();
	}
	
	public RTMPClientConnManager getConnManager() {
		return connManager;
	}

	public void setConnectionClosedHandler(Runnable connectionClosedHandler) {
		this.connectionClosedHandler = connectionClosedHandler;
	}

	public void setExceptionHandler(ClientExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Start network connection to server
	 * 
	 * @param server
	 *            Server
	 * @param port
	 *            Connection port
	 */
	protected abstract void startConnector(String server, int port);

	/**
	 * Connect RTMP client to server's application via given port
	 * 
	 * @param server Server
	 * @param port Connection port
	 * @param application Application at that server
	 */
	public void connect(String server, int port, String application) {
		log.debug("connect server: {} port {} application {}", new Object[] {
				server, port, application });
		connect(server, port, application, null);
	}

	/**
	 * Connect RTMP client to server's application via given port with given
	 * connection callback
	 * 
	 * @param server Server
	 * @param port Connection port
	 * @param application Application at that server
	 * @param connectCallback Connection callback
	 */
	public void connect(String server, int port, String application,
			IPendingServiceCallback connectCallback) {
		log.debug(
				"connect server: {} port {} application {} connectCallback {}",
				new Object[] { server, port, application, connectCallback });

		connect(server, port, makeDefaultConnectionParams(server, port,
				application), connectCallback);
	}

	/**
	 * @param server Server
	 * @param port Connection port
	 * @param application Application at that server
	 * @return default connection parameters
	 */
	public Map<String, Object> makeDefaultConnectionParams(String server,
			int port, String application) {
		Map<String, Object> params = new ObjectMap<String, Object>();
		params.put("app", application);
		params.put("objectEncoding", Integer.valueOf(0));
		params.put("fpad", Boolean.FALSE);
		params.put("flashVer", "WIN 9,0,115,0");
		params.put("audioCodecs", Integer.valueOf(1639));
		params.put("videoFunction", Integer.valueOf(1));
		params.put("pageUrl", null);
		params.put("path", application);
		params.put("capabilities", Integer.valueOf(15));
		params.put("swfUrl", null);
		params.put("videoCodecs", Integer.valueOf(252));
		return params;
	}

	/**
	 * Connect RTMP client to server via given port and with given connection
	 * parameters
	 * 
	 * @param server Server
	 * @param port Connection port
	 * @param connectionParams Connection parameters
	 */
	public void connect(String server, int port,
			Map<String, Object> connectionParams) {
		log.debug("connect server: {} port {} connectionParams {}",
				new Object[] { server, port, connectionParams });
		connect(server, port, connectionParams, null);
	}

	/**
	 * Connect RTMP client to server's application via given port
	 * 
	 * @param server Server
	 * @param port Connection port
	 * @param connectionParams Connection parameters
	 * @param connectCallback Connection callback
	 */
	public void connect(String server, int port,
			Map<String, Object> connectionParams,
			IPendingServiceCallback connectCallback) {
		connect(server, port, connectionParams, connectCallback, null);
	}

	/**
	 * Connect RTMP client to server's application via given port
	 * 
	 * @param server Server
	 * @param port Connection port
	 * @param connectionParams Connection parameters
	 * @param connectCallback Connection callback
	 * @param connectCallArguments Arguments for 'connect' call
	 */
	public void connect(String server, int port,
			Map<String, Object> connectionParams,
			IPendingServiceCallback connectCallback,
			Object[] connectCallArguments) {
		log
				.debug(
						"connect server: {} port {} connectionParams {} connectCallback {} conectCallArguments {}",
						new Object[] { server, port, connectionParams,
								connectCallback,
								Arrays.toString(connectCallArguments) });

		this.connectionParams = connectionParams;
		this.connectArguments = connectCallArguments;

		if (!connectionParams.containsKey("objectEncoding")) {
			connectionParams.put("objectEncoding", (int) 0);
		}

		this.connectCallback = connectCallback;

		startConnector(server, port);
	}

	/**
	 * Register object that provides methods that can be called by the server.
	 * 
	 * @param serviceProvider Service provider
	 */
	public void setServiceProvider(Object serviceProvider) {
		this.serviceProvider = serviceProvider;
	}

	/**
	 * Connect to client shared object.
	 * 
	 * @param name Client shared object name
	 * @param persistent SO persistence flag
	 * @return Client shared object instance
	 */
	public synchronized IClientSharedObject getSharedObject(String name,
			boolean persistent) {
		log.debug("getSharedObject name: {} persistent {}", new Object[] {
				name, persistent });
		ClientSharedObject result = sharedObjects.get(name);
		if (result != null) {
			if (result.isPersistentObject() != persistent) {
				throw new RuntimeException(
						"Already connected to a shared object with this name, but with different persistence.");
			}
			return result;
		}

		result = new ClientSharedObject(name, persistent);
		sharedObjects.put(name, result);
		return result;
	}

	/** {@inheritDoc} */
	@Override
	protected void onChunkSize(RTMPConnection conn, Channel channel,
			Header source, ChunkSize chunkSize) {
		log.debug("onChunkSize");
		// TODO: implement this
		log.info("ChunkSize is not implemented yet: {}", chunkSize);
	}

	/** {@inheritDoc} */
	@Override
	protected void onPing(RTMPConnection conn, Channel channel, Header source,
			Ping ping) {
		log.debug("onPing");
		switch (ping.getValue1()) {
			case Ping.PING_CLIENT:
			case Ping.STREAM_CLEAR:
			case Ping.STREAM_RESET:
			case Ping.STREAM_PLAYBUFFER_CLEAR:
				// The server wants to measure the RTT
				Ping pong = new Ping();
				pong.setValue1((short) Ping.PONG_SERVER);
				int now = (int) (System.currentTimeMillis() & 0xffffffff);
				pong.setValue2(now);
				pong.setValue3(Ping.UNDEFINED);
				conn.ping(pong);
				break;

			default:
				log.warn("Unhandled ping: {}", ping);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onSharedObject(RTMPConnection conn, Channel channel,
			Header source, SharedObjectMessage object) {
		log.debug("onSharedObject");
		ClientSharedObject so = sharedObjects.get(object.getName());
		if (so == null) {
			log.error("Ignoring request for non-existend SO: {}", object);
			return;
		}
		if (so.isPersistentObject() != object.isPersistent()) {
			log.error("Ignoring request for wrong-persistent SO: {}", object);
			return;
		}
		log.debug("Received SO request: {}", object);
		so.dispatchEvent(object);
	}

	/**
	 * Invoke a method on the server.
	 * 
	 * @param method Method name
	 * @param callback Callback handler
	 */
	public void invoke(String method, IPendingServiceCallback callback) {
		log.debug("invoke method: {} params {} callback {}", new Object[] {
				method, callback });
		// get it from the conn manager
		RTMPConnection conn = connManager.getConnection();
		if (conn == null) {
			log.info("Connection was null 2");
		}
		if (conn instanceof IServiceCapableConnection) {
			conn.invoke(method, callback);
		}
	}

	/**
	 * Invoke a method on the server and pass parameters.
	 * 
	 * @param method Method
	 * @param params Method call parameters
	 * @param callback Callback object
	 */
	public void invoke(String method, Object[] params,
			IPendingServiceCallback callback) {
		log.debug("invoke method: {} params {} callback {}", new Object[] {
				method, params, callback });
		// get it from the conn manager
		RTMPConnection conn = connManager.getConnection();
		if (conn == null) {
			log.info("Connection was null 2");
		}
		if (conn instanceof IServiceCapableConnection) {
			((IServiceCapableConnection) conn).invoke(method, params, callback);
		}
	}

	/**
	 * Disconnect the first connection in the connection map
	 */
	public void disconnect() {
		log.debug("disconnect");
		IConnection conn = connManager.getConnection();
		if (conn == null) {
			log.info("Connection was null");
		} else {
			streamDataMap.clear();
			conn.close();
		}
	}

	public void createStream(IPendingServiceCallback callback) {
		IPendingServiceCallback wrapper = new CreateStreamCallBack(callback);
		invoke("createStream", null, wrapper);
	}

	public void publish(int streamId, String name, String mode,
			INetStreamEventHandler handler) {
		log.debug("publish stream {}, name: {}, mode {}", new Object[] {
				streamId, name, mode });
		RTMPConnection conn = connManager.getConnection();
		Object[] params = new Object[2];
		params[0] = name;
		params[1] = mode;
		PendingCall pendingCall = new PendingCall("publish", params);
		conn.invoke(pendingCall, (streamId - 1) * 5 + 4);
		if (handler != null) {
			NetStreamPrivateData streamData = streamDataMap.get(streamId);
			if (streamData != null) {
				streamData.handler = handler;
			}
		}
	}

	public void unpublish(int streamId) {
		log.debug("unpublish stream {}", streamId);
		PendingCall pendingCall = new PendingCall("publish",
				new Object[] { false });
		connManager.getConnection().invoke(pendingCall, (streamId - 1) * 5 + 4);
	}

	public void publishStreamData(int streamId, IMessage message) {
		NetStreamPrivateData streamData = streamDataMap.get(streamId);
		if (streamData != null && streamData.connConsumer != null) {
			streamData.connConsumer.pushMessage(null, message);
		}
	}

	public void play(int streamId, String name, int start, int length) {
		log.debug("play stream {}, name: {}, start {}, length {}",
				new Object[] { streamId, name, start, length });
		RTMPConnection conn = connManager.getConnection();
		if (conn == null) {
			log.info("Connection was null ?");
		}
		Object[] params = new Object[3];
		params[0] = name;
		params[1] = start;
		params[2] = length;
		PendingCall pendingCall = new PendingCall("play", params);
		conn.invoke(pendingCall, (streamId - 1) * 5 + 4);
	}

	/** {@inheritDoc} */
	@Override
	public void connectionOpened(RTMPConnection conn, RTMP state) {
		log.debug("connectionOpened");
		// Send "connect" call to the server
		Channel channel = conn.getChannel((byte) 3);
		PendingCall pendingCall = new PendingCall("connect");
		pendingCall.setArguments(connectArguments);
		Invoke invoke = new Invoke(pendingCall);
		invoke.setConnectionParams(connectionParams);
		invoke.setInvokeId(1);
		if (connectCallback != null) {
			pendingCall.registerCallback(connectCallback);
		}
		conn.registerPendingCall(invoke.getInvokeId(), pendingCall);
		log.debug("Writing 'connect' invoke: {}, invokeId: {}", invoke, invoke
				.getInvokeId());
		channel.write(invoke);
	}

	@Override
	public void connectionClosed(RTMPConnection conn, RTMP state) {
		log.debug("connectionClosed");
		super.connectionClosed(conn, state);
		if (connectionClosedHandler != null) {
			connectionClosedHandler.run();
		}
	}

	/** {@inheritDoc} */
	@Override
	protected void onInvoke(RTMPConnection conn, Channel channel,
			Header source, Notify invoke, RTMP rtmp) {
		if (invoke.getType() == IEvent.Type.STREAM_DATA) {
			log.debug("Ignoring stream data notify with header: {}", source);
			return;
		}
		log.debug("onInvoke: {}, invokeId: {}", invoke, invoke.getInvokeId());
		final IServiceCall call = invoke.getCall();
		if (call.getServiceMethodName().equals("_result")
				|| call.getServiceMethodName().equals("_error")) {
			final IPendingServiceCall pendingCall = conn.getPendingCall(invoke
					.getInvokeId());
			log.debug("Received result for pending call {}", pendingCall);
			if (pendingCall != null) {
				if ("connect".equals(pendingCall.getServiceMethodName())) {
					Integer encoding = (Integer) connectionParams
							.get("objectEncoding");
					if (encoding != null && encoding.intValue() == 3) {
						log.debug("set encoding to AMF3");
						rtmp.setEncoding(IConnection.Encoding.AMF3);
					}
				}
			}
			handlePendingCallResult(conn, invoke);
			return;
		}

		// potentially used twice so get the value once
		boolean onStatus = call.getServiceMethodName().equals("onStatus");

		if (onStatus) {
			// XXX better to serialize ObjectMap to Status object
			ObjectMap<?, ?> objMap = (ObjectMap<?, ?>) call.getArguments()[0];
			// should keep this as an Object to stay compatible with FMS3 etc
			Object clientId = (Object) objMap.get("clientid");
			if (clientId == null) {
				clientId = source.getStreamId();
			}
			log.debug("Client id: {}", clientId);
			if (clientId != null) {
				NetStreamPrivateData streamData = streamDataMap.get(clientId);
				if (streamData != null && streamData.handler != null) {
					streamData.handler.onStreamEvent(invoke);
				}
			}
		}

		if (serviceProvider == null) {
			// Client doesn't support calling methods on him
			call.setStatus(Call.STATUS_METHOD_NOT_FOUND);
			call.setException(new MethodNotFoundException(call
					.getServiceMethodName()));
		} else {
			serviceInvoker.invoke(call, serviceProvider);
		}

		if (call instanceof IPendingServiceCall) {
			IPendingServiceCall psc = (IPendingServiceCall) call;
			Object result = psc.getResult();
			log.debug("Pending call result is: {}", result);
			if (result instanceof DeferredResult) {
				DeferredResult dr = (DeferredResult) result;
				dr.setInvokeId(invoke.getInvokeId());
				dr.setServiceCall(psc);
				dr.setChannel(channel);
				conn.registerDeferredResult(dr);
			} else if (!onStatus) {
				Invoke reply = new Invoke();
				reply.setCall(call);
				reply.setInvokeId(invoke.getInvokeId());
				log.debug("Sending empty call reply: {}", reply);
				channel.write(reply);
			}
		}
	}

	/**
	 * Setter for codec factory
	 * 
	 * @param factory Codec factory to use
	 */
	public void setCodecFactory(RTMPCodecFactory factory) {
		this.codecFactory = factory;
	}

	/**
	 * Getter for codec factory
	 * 
	 * @return Codec factory
	 */
	public RTMPCodecFactory getCodecFactory() {
		return this.codecFactory;
	}

	public void handleException(Throwable throwable) {
		if (exceptionHandler != null) {
			exceptionHandler.handleException(throwable);
		} else {
			log.error("Connection exception", throwable);
			throw new RuntimeException(throwable);
		}
	}

	/**
	 * Setter for stream event dispatcher (useful for saving playing stream to
	 * file)
	 * 
	 * @param streamEventDispatcher event dispatcher
	 */
	public void setStreamEventDispatcher(IEventDispatcher streamEventDispatcher) {
		this.streamEventDispatcher = streamEventDispatcher;
	}

	private class NetStream extends AbstractClientStream implements
			IEventDispatcher {
		private IEventDispatcher dispatcher;

		public NetStream(IEventDispatcher dispatcher) {
			this.dispatcher = dispatcher;
		}

		public void close() {
			// do nothing
		}

		public void start() {
			// do nothing
		}

		public void stop() {
			// do nothing
		}

		public void dispatchEvent(IEvent event) {
			if (dispatcher != null)
				dispatcher.dispatchEvent(event);
		}
	}

	private class CreateStreamCallBack implements IPendingServiceCallback {
		private IPendingServiceCallback wrapped;

		public CreateStreamCallBack(IPendingServiceCallback wrapped) {
			this.wrapped = wrapped;
		}

		public void resultReceived(IPendingServiceCall call) {
			Integer streamIdInt = (Integer) call.getResult();
			log.debug("Stream id: {}", streamIdInt);
			RTMPConnection conn = connManager.getConnection();
			if (conn != null && streamIdInt != null) {
				NetStream stream = new NetStream(streamEventDispatcher);
				stream.setConnection(conn);
				stream.setStreamId(streamIdInt);
				conn.addClientStream(stream);
				NetStreamPrivateData streamData = new NetStreamPrivateData();
				streamData.outputStream = conn.createOutputStream(streamIdInt);
				streamData.connConsumer = new ConnectionConsumer(conn,
						streamData.outputStream.getVideo().getId(),
						streamData.outputStream.getAudio().getId(),
						streamData.outputStream.getData().getId());
				streamDataMap.put(streamIdInt, streamData);
			}
			wrapped.resultReceived(call);
		}
	}

	private class NetStreamPrivateData {
		public volatile INetStreamEventHandler handler;

		public volatile OutputStream outputStream;

		public volatile ConnectionConsumer connConsumer;
	}
}
