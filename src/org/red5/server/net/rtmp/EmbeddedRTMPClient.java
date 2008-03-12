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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.mina.transport.socket.nio.SocketConnector;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.service.IServiceInvoker;
import org.red5.server.api.so.IClientSharedObject;
import org.red5.server.net.rtmp.BaseRTMPHandler;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.DeferredResult;
import org.red5.server.net.rtmp.EdgeRTMPMinaConnection;
import org.red5.server.net.rtmp.IRTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.RTMPMinaIoHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPCodecFactory;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.red5.server.service.Call;
import org.red5.server.service.MethodNotFoundException;
import org.red5.server.service.PendingCall;
import org.red5.server.service.ServiceInvoker;
import org.red5.server.so.ClientSharedObject;
import org.red5.server.so.SharedObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP client object. Initial client mode code by Christian Eckerle.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Christian Eckerle (ce@publishing-etc.de)
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */
public class EmbeddedRTMPClient extends BaseRTMPHandler {
    /**
     * Logger
     */
	protected static Logger log = LoggerFactory.getLogger(EmbeddedRTMPClient.class);
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
    
    /** Analogous to NetConnection.objectEncoding */
    private Integer objectEncoding = 0;

	/** Constructs a new RTMPClient. */
    public EmbeddedRTMPClient() {
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
		params.put("objectEncoding", objectEncoding);
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
		if (!this.connectionParams.containsKey("objectEncoding")) {
			this.connectionParams.put("objectEncoding", objectEncoding);
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
		IConnection conn = Red5.getConnectionLocal();
		if (conn == null) {
		    log.info("Connection was null 1");
		}
		//get it from the conn manager
		conn = connManager.getConnection();
		if (conn == null) {
		    log.info("Connection was null 2");
		}
		if (conn instanceof IServiceCapableConnection) {
			((IServiceCapableConnection) conn).invoke(method, callback);
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
		IConnection conn = Red5.getConnectionLocal();
		if (conn == null) {
		    log.info("Connection was null 1");
		}
		//get it from the conn manager
		conn = connManager.getConnection();
		if (conn == null) {
		    log.info("Connection was null 2");
		}
		if (conn instanceof IServiceCapableConnection) {
			((IServiceCapableConnection) conn).invoke(method, params, callback);
		}
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
	    log.debug("onInvoke");	
	    log.debug("Connection encoding: {} channel: {} header: {}", new Object[]{conn.getEncoding(), channel, source});
		final IServiceCall call = invoke.getCall();
		String serviceMethodName = call.getServiceMethodName();
		if ("_result".equals(serviceMethodName) || "_error".equals(serviceMethodName)) {
			log.debug("Invoke id: {}", invoke.getInvokeId());
			final IPendingServiceCall pendingCall = conn.getPendingCall(invoke.getInvokeId());
			log.debug("Pending call: {}", pendingCall);
			if (pendingCall != null && "connect".equals(pendingCall.getServiceMethodName())) {
				//get the connection state
				RTMP state = conn.getState();
				log.debug("Connection state: {}", state.getState());				
				if (objectEncoding == 3 && !conn.getEncoding().equals(IConnection.Encoding.AMF3)) {
		    		log.debug("Encoding didnt match, enforcing AMF3");
		    		state.setEncoding(Encoding.AMF3);
		    	}				
			}
			handlePendingCallResult(conn, invoke);
			return;
		}
		
		log.debug("Service provider: {}", serviceProvider);
		if (serviceProvider == null) {
			// Client doesn't support calling methods on him
			call.setStatus(Call.STATUS_METHOD_NOT_FOUND);
			call.setException(new MethodNotFoundException(
					call.getServiceMethodName()));
		} else {
			serviceInvoker.invoke(call, serviceProvider);
		}
		
		if (call instanceof IPendingServiceCall) {
			log.debug("Pending service call");
			IPendingServiceCall psc = (IPendingServiceCall) call;
			Object result = psc.getResult();
			if (result instanceof DeferredResult) {
				log.debug("Result is DeferredResult");
				DeferredResult dr = (DeferredResult) result;
				dr.setInvokeId(invoke.getInvokeId());
				dr.setServiceCall(psc);
				dr.setChannel(channel);
				conn.registerDeferredResult(dr);
			} else {
				log.debug("Result is not a DeferredResult");
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
		log.debug("Received SO request: {}", object);
		so.dispatchEvent(object);
	}

	public Integer getObjectEncoding() {
		return objectEncoding;
	}

	public void setObjectEncoding(Integer objectEncoding) {
		this.objectEncoding = objectEncoding;
	}   
    
    private class RTMPClientConnManager implements IRTMPConnManager {
    	
    	private Map<Integer, RTMPConnection> connMap = new HashMap<Integer, RTMPConnection>();
    	protected AtomicInteger clientId = new AtomicInteger();

		public RTMPConnection createConnection(Class connCls) {
		    log.debug("Creating connection, class: {}", connCls.getName());
			if (!RTMPConnection.class.isAssignableFrom(connCls)) {
			    log.debug("Class was not assignable");
				return null;
			}
			try {
				RTMPConnection conn =  new RTMPMinaConnection();
				conn.setId(clientId.incrementAndGet());
				log.debug("Connection id set {}", conn.getId());
				//check encoding
				IConnection.Encoding encoding = conn.getEncoding();
				log.debug("Connection encoding: {}", encoding);
				if (appCtx == null) {
					log.debug("Application context was not found, so scheduler will not be added");
				} else {
					ISchedulingService scheduler = (ISchedulingService) appCtx.getBean(ISchedulingService.BEAN_NAME);
					log.debug("Found scheduler");
					conn.setSchedulingService(scheduler);
				}
				connMap.put(conn.getId(), conn);
				log.debug("Connection added to the map");
				return conn;
			} catch (Exception e) {
				return null;
			}
		}

		public RTMPConnection getConnection() {
		    log.debug("Returning first map entry");
			return connMap.values().iterator().next();
		}

		public RTMPConnection getConnection(int clientId) {
		    log.debug("Returning map entry for client id: {}", clientId);
			return connMap.get(clientId);
		}

		public RTMPConnection removeConnection(int clientId) {
			return connMap.remove(clientId);
		}

		public Collection<RTMPConnection> removeConnections() {
		    connMap.clear();
			return null;
		}
    }

}
