package org.red5.server.net.rtmp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.transport.socket.nio.SocketConnector;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.IConnection;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.scheduling.ISchedulingService;
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
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.message.Constants;
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
 * RTMP client object. Initial client mode code by Christian Eckerle.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Christian Eckerle (ce@publishing-etc.de)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Steven Gong (steven.gong@gmail.com)
 *
 */
public class RTMPClient extends BaseRTMPHandler {
    /**
     * Logger
     */
	protected static Logger log = LoggerFactory.getLogger(RTMPClient.class);
    /**
     * I/O handler
     */
	private RTMPMinaIoHandler ioHandler;
    /**
     * Connection parameters
     */
    private Map<String, Object> connectionParams;
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
    private Map<String, ClientSharedObject> sharedObjects = new HashMap<String, ClientSharedObject>();
    
    private RTMPClientConnManager connManager;
    
    private Map<Integer, NetStreamPrivateData> streamDataMap =
    	new HashMap<Integer, NetStreamPrivateData>();

	/** Constructs a new RTMPClient. */
    public RTMPClient() {
		RTMPCodecFactory codecFactory = new RTMPCodecFactory();
		codecFactory.setDeserializer(new Deserializer());
		codecFactory.setSerializer(new Serializer());
		codecFactory.init();
		
		ioHandler = new RTMPMinaIoHandler();
		ioHandler.setCodecFactory(codecFactory);
		ioHandler.setMode(RTMP.MODE_CLIENT);
		ioHandler.setHandler(this);
		
		connManager = new RTMPClientConnManager();
		ioHandler.setRtmpConnManager(connManager);
	}

    /**
     * Connect RTMP client to server's application via given port
     * @param server                 Server
     * @param port                   Connection port
     * @param application            Application at that server
     */
    public void connect(String server, int port, String application) {
   	    log.debug("connect server: {} port {} application {}", new Object[]{server, port, application});	
		connect(server, port, application, null);
	}

    /**
     * Connect RTMP client to server's application via given port with given connection callback
     * @param server                Server
     * @param port                  Connection port
     * @param application           Application at that server
     * @param connectCallback       Connection callback
     */
    public void connect(String server, int port, String application, IPendingServiceCallback connectCallback) {
   	    log.debug("connect server: {} port {} application {} connectCallback {}", new Object[]{server, port, application, connectCallback});		    
		// Initialize default connection parameters
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("app", application);
		params.put("tcUrl", "rtmp://"+server+':'+port+'/'+application);
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
		
		connect(server, port, params, connectCallback);
	}

    /**
     * Connect RTMP client to server via given port and with given connection parameters
     * @param server                 Server
     * @param port                   Connection port
     * @param connectionParams       Connection parameters
     */
    public void connect(String server, int port, Map<String, Object> connectionParams) {
   	    log.debug("connect server: {} port {} connectionParams {}", new Object[]{server, port, connectionParams});		
		connect(server, port, connectionParams, null);
	}

    /**
     * 
     * @param server
     * @param port
     * @param connectionParams
     * @param connectCallback
     */
    public void connect(String server, int port, Map<String, Object> connectionParams, IPendingServiceCallback connectCallback) {
   	    log.debug("connect server: {} port {} connectionParams {} connectCallback {}", new Object[]{server, port, connectionParams, connectCallback});		
		this.connectionParams = connectionParams;
		if (!connectionParams.containsKey("objectEncoding")) {
			connectionParams.put("objectEncoding", (double) 0);
		}
		this.connectCallback = connectCallback;
		SocketConnector connector = new SocketConnector();
		connector.connect(new InetSocketAddress(server, port), ioHandler);
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

    /**
     * Disconnect the connection with the matching id
     */
    public void disconnect(String clientId) {
    	log.debug("disconnect id: {}", clientId);
		IConnection conn = connManager.getConnection(Integer.valueOf(clientId));
		if (conn == null) {
		    log.info("Connection was null for id: {}", clientId);
		} else {
			streamDataMap.clear();
			conn.close();
		}
    }	
	
	/**
	 * Register object that provides methods that can be called by the server.
	 * 
	 * @param serviceProvider         Service provider
	 */
	public void setServiceProvider(Object serviceProvider) {
		this.serviceProvider = serviceProvider;
	}
	
	/**
	 * Connect to client shared object.
	 * 
	 * @param name                    Client shared object name
	 * @param persistent              SO persistence flag
	 * @return                        Client shared object instance
	 */
	public synchronized IClientSharedObject getSharedObject(String name, boolean persistent) {
	    log.debug("getSharedObject name: {} persistent {}", new Object[]{name, persistent});		
		ClientSharedObject result = sharedObjects.get(name);
		if (result != null) {
			if (result.isPersistentObject() != persistent) {
				throw new RuntimeException("Already connected to a shared object with this name, but with different persistence.");
			}
			return result;
		}
		
		result = new ClientSharedObject(name, persistent);
		sharedObjects.put(name, result);
		return result;	
	}

	/**
	 * Invoke a method on the server.
	 * 
	 * @param method                  Method name
	 * @param callback                Callback handler
	 */
	public void invoke(String method, IPendingServiceCallback callback) {
	    log.debug("invoke method: {} params {} callback {}", new Object[]{method, callback});			
		//get it from the conn manager
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
	 * @param method                 Method
	 * @param params                 Method call parameters
	 * @param callback               Callback object
	 */
	public void invoke(String method, Object[] params, IPendingServiceCallback callback) {
	    log.debug("invoke method: {} params {} callback {}", new Object[]{method, params, callback});		
		//get it from the conn manager
		RTMPConnection conn = connManager.getConnection();
		if (conn == null) {
		    log.info("Connection was null 2");
		}
		if (conn instanceof IServiceCapableConnection) {
			((IServiceCapableConnection) conn).invoke(method, params, callback);
		}
	}
	
	public void createStream(IPendingServiceCallback callback) {
		IPendingServiceCallback wrapper = new CreateStreamCallBack(callback);
		invoke("createStream", null, wrapper);
	}
	
	public void deleteStream(int streamId, IPendingServiceCallback callback) {
		// TODO
	}
	
	public void publish(int streamId, String name, String mode, INetStreamEventHandler handler) {
		log.debug("publish stream {}, name: {}, mode {}",
				new Object[] {streamId, name, mode});
		RTMPConnection conn = (RTMPConnection) connManager.getConnection();
		if (conn == null) {
		    log.info("Connection was null ?");
		}
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
	
	public void publishStreamData(int streamId, IMessage message) {
		NetStreamPrivateData streamData = streamDataMap.get(streamId);
		if (streamData != null && streamData.connConsumer != null) {
			streamData.connConsumer.pushMessage(null, message);
		}
	}
	
	public void play(int streamId, String name, int start, int length) {
		log.debug("play stream {}, name: {}, start {}, length {}",
				new Object[] {streamId, name, start, length});
		RTMPConnection conn = (RTMPConnection) connManager.getConnection();
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
		Invoke invoke = new Invoke(pendingCall);
		invoke.setConnectionParams(connectionParams);
		invoke.setInvokeId(conn.getInvokeId());
		if (connectCallback != null) {
			pendingCall.registerCallback(connectCallback);
		}
		conn.registerPendingCall(invoke.getInvokeId(), pendingCall);
		channel.write(invoke);
	}
	
	/** {@inheritDoc} */
    @Override
	protected void onInvoke(RTMPConnection conn, Channel channel, Header source, Notify invoke, RTMP rtmp) {
	    log.debug("onInvoke:" + invoke);	
		final IServiceCall call = invoke.getCall();
		if (call.getServiceMethodName().equals("_result")
				|| call.getServiceMethodName().equals("_error")) {
			final IPendingServiceCall pendingCall = conn.getPendingCall(invoke
					.getInvokeId());
			if (pendingCall != null) {
				if ("connect".equals(pendingCall.getServiceMethodName())) {
					Integer encoding = (Integer) connectionParams.get("objectEncoding");
					if (encoding != null && encoding.intValue() == 3) {
						log.debug("set encoding to AMF3");
						rtmp.setEncoding(Encoding.AMF3);
					}
				}
			}
			handlePendingCallResult(conn, invoke);
			return;
		}
		
		if (call.getServiceMethodName().equals("onStatus")) {
			// XXX better to serialize ObjectMap to Status object 
			ObjectMap objMap = (ObjectMap) call.getArguments()[0];
			Integer clientId = (Integer) objMap.get("clientid");
			NetStreamPrivateData streamData = streamDataMap.get(clientId);
			if (streamData != null && streamData.handler != null) {
				streamData.handler.onStreamEvent(invoke);
			}
		}
		
		if (serviceProvider == null) {
			// Client doesn't support calling methods on him
			call.setStatus(Call.STATUS_METHOD_NOT_FOUND);
			call.setException(new MethodNotFoundException(
					call.getServiceMethodName()));
		} else {
			serviceInvoker.invoke(call, serviceProvider);
		}
		
		if (call instanceof IPendingServiceCall) {
			IPendingServiceCall psc = (IPendingServiceCall) call;
			Object result = psc.getResult();
			if (result instanceof DeferredResult) {
				DeferredResult dr = (DeferredResult) result;
				dr.setInvokeId(invoke.getInvokeId());
				dr.setServiceCall(psc);
				dr.setChannel(channel);
				conn.registerDeferredResult(dr);
			} else {
				Invoke reply = new Invoke();
				reply.setCall(call);
				reply.setInvokeId(invoke.getInvokeId());
				channel.write(reply);
			}
		}
	}

	/** {@inheritDoc} */
    @Override
	protected void onChunkSize(RTMPConnection conn, Channel channel, Header source, ChunkSize chunkSize) {
		    log.debug("onChunkSize");	
		// TODO: implement this
		log.info("ChunkSize is not implemented yet: {}", chunkSize);
	}

	/** {@inheritDoc} */
    @Override
	protected void onPing(RTMPConnection conn, Channel channel,	Header source, Ping ping) {
			    log.debug("onPing");	
		switch (ping.getValue1()) {
			case 6:
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
	protected void onSharedObject(RTMPConnection conn, Channel channel,	Header source, SharedObjectMessage object) {
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
		if (log.isDebugEnabled()) {
			log.debug("Received SO request: {}", object);
		}
		so.dispatchEvent(object);
	}

    private class RTMPClientConnManager implements IRTMPConnManager {
    	
    	private RTMPConnection rtmpConn;

		public RTMPConnection createConnection(Class connCls) {
		    log.debug("Creating connection, class: {}", connCls.getName());
			if (!RTMPConnection.class.isAssignableFrom(connCls)) {
			    log.debug("Class was not assignable");
				return null;
			}
			try {
				RTMPConnection conn = new RTMPMinaConnection();
				conn.setId(0);
				log.debug("Connection id set {}", conn.getId());
				if (appCtx == null) {
					log.debug("Application context was not found, so scheduler will not be added");
				} else {
					ISchedulingService scheduler = (ISchedulingService) appCtx.getBean(ISchedulingService.BEAN_NAME);
					log.debug("Found scheduler");
					conn.setSchedulingService(scheduler);
				}
				rtmpConn = conn;
				log.debug("Connection added to the map");
				return conn;
			} catch (Exception e) {
				return null;
			}
		}

		public RTMPConnection getConnection() {
		    log.debug("Returning first map entry");
			return rtmpConn;
		}

		public RTMPConnection getConnection(int clientId) {
		    log.debug("Returning map entry for client id: {}", clientId);
			if (clientId == 0) {
				return rtmpConn;
			} else {
				return null;
			}
		}

		public RTMPConnection removeConnection(int clientId) {
			RTMPConnection connReturn = null;
			if (clientId == 0) {
				connReturn = rtmpConn;
				rtmpConn = null;
			}
			return connReturn;
		}

		public Collection<RTMPConnection> removeConnections() {
		    rtmpConn = null;
		    return null;
		}
    }
    
    private class NetStream extends AbstractClientStream
    implements IEventDispatcher {

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
			// TODO put event handling here
		}
		
    }
    
    private class CreateStreamCallBack implements IPendingServiceCallback {
    	private IPendingServiceCallback wrapped;
    	
    	public CreateStreamCallBack(IPendingServiceCallback wrapped) {
			super();
			this.wrapped = wrapped;
		}
    	
		public void resultReceived(IPendingServiceCall call) {
			Integer streamIdInt = (Integer) call.getResult();
			RTMPConnection conn = connManager.getConnection();
			if (conn != null && streamIdInt != null) {
				NetStream stream = new NetStream();
				stream.setConnection(conn);
				stream.setStreamId(streamIdInt.intValue());
				conn.addClientStream(stream);
				NetStreamPrivateData streamData = new NetStreamPrivateData();
				streamData.outputStream = conn.createOutputStream(streamIdInt.intValue());
				streamData.connConsumer = new ConnectionConsumer(
						conn,
						streamData.outputStream.getVideo().getId(),
						streamData.outputStream.getAudio().getId(),
						streamData.outputStream.getData().getId()
						);
				streamDataMap.put(streamIdInt, streamData);
			}
			wrapped.resultReceived(call);
		}
    }
    
    public interface INetStreamEventHandler {
    	void onStreamEvent(Notify notify);
    }
    
    private class NetStreamPrivateData {
    	public INetStreamEventHandler handler;
    	public OutputStream outputStream;
    	public ConnectionConsumer connConsumer;
    }
}
