package org.red5.server.net.rtmp;

import static org.red5.server.api.ScopeUtils.getScopeService;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.IContext;
import org.red5.server.api.IGlobalScope;
import org.red5.server.api.IScope;
import org.red5.server.api.IScopeHandler;
import org.red5.server.api.IServer;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEventDispatcher;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.exception.ClientRejectedException;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.InPacket;
import org.red5.server.net.rtmp.message.Invoke;
import org.red5.server.net.rtmp.message.Notify;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.net.rtmp.message.PacketHeader;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.net.rtmp.message.SharedObject;
import org.red5.server.net.rtmp.message.SharedObjectEvent;
import org.red5.server.net.rtmp.message.StreamBytesRead;
import org.red5.server.net.rtmp.message.Unknown;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.net.rtmp.status.StatusObjectService;
import org.red5.server.service.Call;
import org.red5.server.so.SharedObjectService;
import org.red5.server.stream.SubscriberStreamNew;
import org.red5.server.stream.Stream;
import org.red5.server.stream.StreamService;

public class RTMPHandler 
	implements Constants, StatusCodes {
	
	protected static Log log =
        LogFactory.getLog(RTMPHandler.class.getName());
	
	protected StatusObjectService statusObjectService;
	protected IServer server;
	
	// XXX: HACK HACK HACK to support stream ids
	private static ThreadLocal<Integer> streamLocal = new ThreadLocal<Integer>();
	
	public void setServer(IServer server) {
		this.server = server;
	}

	public void setStatusObjectService(StatusObjectService statusObjectService) {
		this.statusObjectService = statusObjectService;
	}

	// XXX: HACK HACK HACK to support stream ids
	public static int getStreamId() {
		return streamLocal.get().intValue();
	}
	private static void setStreamId(int id) {
		streamLocal.set(id);
	}
	
	public void messageReceived(RTMPConnection conn, ProtocolState state, Object in) throws Exception {
			
		try {
			
			final InPacket packet = (InPacket) in;
			final Message message = packet.getMessage();
			final PacketHeader source = packet.getSource();
			final Channel channel = conn.getChannel(packet.getSource().getChannelId());
			final IStream stream = conn.getStreamById(source.getStreamId());
			
			if(log.isDebugEnabled()){
				log.debug("Message recieved");
				log.debug("Stream Id: "+source);
				log.debug("Channel: "+channel);
			}
				
			// Thread local performance ? Should we benchmark
			Red5.setConnectionLocal(conn);
			
			// XXX: HACK HACK HACK to support stream ids
			RTMPHandler.setStreamId(source.getStreamId());
			
			// Increase number of received messages
			conn.messageReceived();
			
			switch(message.getDataType()){
			case TYPE_INVOKE:
				onInvoke(conn, channel, source, (Invoke) message);
				break;
				
			case TYPE_NOTIFY: // just like invoke, but does not return
				if (((Notify) message).getCall() == null && stream != null)
					// Stream metadata
					((IEventDispatcher) stream).dispatchEvent(message);
				else
					onInvoke(conn, channel, source, (Notify) message);
				break;
			case TYPE_PING:
				onPing(conn, channel, source, (Ping) message);
				break;
			/*
			case TYPE_STREAM_BYTES_READ:
				onStreamBytesRead(conn, channel, source, (StreamBytesRead) message);
				break;
			*/
			case TYPE_AUDIO_DATA:
			case TYPE_VIDEO_DATA:
				//log.info("in packet: "+source.getSize()+" ts:"+source.getTimer());
				((IEventDispatcher) stream).dispatchEvent(message);
				break;
			case TYPE_SHARED_OBJECT:
				SharedObject so = (SharedObject) message;
				onSharedObject(conn, channel, source, so);
				break;
			}
			if(message instanceof Unknown){
				log.info(message);
			}
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			log.error("Exception",e);
		}
	}

	public void messageSent(RTMPConnection conn, Object message) {
		if(log.isDebugEnabled())
			log.debug("Message sent");
		
		if(message instanceof ByteBuffer){
			return;
		}
		
		// Increase number of sent messages
		conn.messageSent();
		
		OutPacket sent = (OutPacket) message;
		final byte channelId = sent.getDestination().getChannelId();
		final IStream stream = conn.getStreamByChannelId(channelId);
		if (stream != null && (stream instanceof Stream)) {
			((Stream) stream).written(sent.getMessage());
		}
		// XXX we'd better use new event model for notification
		if (stream != null && (stream instanceof SubscriberStreamNew)) {
			((SubscriberStreamNew) stream).written(sent.getMessage());
		}
	}

	public void connectionClosed(RTMPConnection conn, RTMP state) {
		state.setState(RTMP.STATE_DISCONNECTED);
		conn.close();
	}
	
	
	public void invokeCall(RTMPConnection conn, IServiceCall call){
		final IScope scope = conn.getScope();
		final IScopeHandler handler = scope.getHandler();
		log.debug("Scope: " + scope);
		log.debug("Handler: " + handler);
		if (!handler.serviceCall(conn, call)) {
			// XXX: What do do here? Return an error?
			return;
		}
		
		final IContext context = scope.getContext();
		log.debug("Context: " + context);
		context.getServiceInvoker().invoke(call, handler);
	}
	
	public void invokeCall(RTMPConnection conn, IServiceCall call, Object service){
		final IScope scope = conn.getScope();
		final IContext context = scope.getContext();
		log.debug("Scope: " + scope);
		log.debug("Service: " + service);
		log.debug("Context: " + context);
		context.getServiceInvoker().invoke(call, service);
	}
	
	// ------------------------------------------------------------------------------
	
	protected String getHostname(String url) {
		log.debug("url: "+url);
		String[] parts = url.split("/");
		if (parts.length == 2)
			// TODO: is this a good default hostname?
			return "";
		else
			return parts[2];
	}
	
	public void onInvoke(RTMPConnection conn, Channel channel, PacketHeader source, Notify invoke){
		
		log.debug("Invoke");
		
		final IServiceCall call = invoke.getCall();
		if (call.getServiceMethodName().equals("_result") || call.getServiceMethodName().equals("_error")) {
			final IPendingServiceCall pendingCall = conn.getPendingCall(invoke.getInvokeId());
			if (pendingCall != null) {
				// The client sent a response to a previously made call.
				Object[] args = call.getArguments(); 
				if ((args != null) && (args.length > 0)) {
					// TODO: can a client return multiple results?
					pendingCall.setResult(args[0]);
				}
				
				Set<IPendingServiceCallback> callbacks = pendingCall.getCallbacks();
				if (callbacks.isEmpty())
					return;
				
				HashSet<IPendingServiceCallback> tmp = new HashSet<IPendingServiceCallback>();
				tmp.addAll(callbacks);
				Iterator<IPendingServiceCallback> it = tmp.iterator();
				while (it.hasNext()) {
					IPendingServiceCallback callback = it.next();
					try {
						callback.resultReceived(pendingCall);
					} catch (Exception e) {
						log.error("Error while executing callback " + callback, e);
					}
				}
			}
			return;
		}
		
		// Make sure we don't use invoke ids that are used by the client
		synchronized (conn.invokeId) {
			if (conn.invokeId <= invoke.getInvokeId())
				conn.invokeId = invoke.getInvokeId() + 1;
		}
		
		boolean disconnectOnReturn = false;
		if(call.getServiceName() == null){
			log.info("call: "+call);
			final String action = call.getServiceMethodName();
			log.info("--"+action);
			if(!conn.isConnected()){
				
				if(action.equals(ACTION_CONNECT)){
					log.debug("connect");
					final Map params = invoke.getConnectionParams();
					String host = getHostname((String) params.get("tcUrl"));
					if (host.endsWith(":1935"))
						// Remove default port from connection string
						host = host.substring(0, host.length() - 5);
					final String path = (String) params.get("app");
					final String sessionId = null;
					conn.setup(host, path, sessionId, params);
					try {
						final IGlobalScope global = server.lookupGlobal(host,path);
						if (global == null) {
							call.setStatus(Call.STATUS_SERVICE_NOT_FOUND);
							if (call instanceof IPendingServiceCall)
								((IPendingServiceCall) call).setResult(getStatus(NC_CONNECT_FAILED));
							log.info("No global scope found for " + path + " on " + host);
							conn.close();
						} else {
							final IContext context = global.getContext();
							final IScope scope = context.resolveScope(path);
							log.info("Connecting to: "+scope);
							boolean okayToConnect;
							try {
								if (call.getArguments() != null)
									okayToConnect = conn.connect(scope, call.getArguments());
								else
									okayToConnect = conn.connect(scope);
								if (okayToConnect){
									log.debug("connected");
									log.debug("client: "+conn.getClient());
									call.setStatus(Call.STATUS_SUCCESS_RESULT);
									if (call instanceof IPendingServiceCall)
										((IPendingServiceCall) call).setResult(getStatus(NC_CONNECT_SUCCESS));
								} else {
									log.debug("connect failed");
									call.setStatus(Call.STATUS_ACCESS_DENIED);
									if (call instanceof IPendingServiceCall)
										((IPendingServiceCall) call).setResult(getStatus(NC_CONNECT_REJECTED));
									disconnectOnReturn = true;
								}
							} catch (ClientRejectedException rejected) {
								log.debug("connect rejected");
								call.setStatus(Call.STATUS_ACCESS_DENIED);
								if (call instanceof IPendingServiceCall) {
									StatusObject status = (StatusObject) getStatus(NC_CONNECT_REJECTED);
									status.setApplication(rejected.getReason());
									((IPendingServiceCall) call).setResult(status);
								}
								disconnectOnReturn = true;
							}
						}
					} catch (RuntimeException e) {
						call.setStatus(Call.STATUS_GENERAL_EXCEPTION);
						if (call instanceof IPendingServiceCall)
							((IPendingServiceCall) call).setResult(getStatus(NC_CONNECT_FAILED));
						log.error("Error connecting",e);
						disconnectOnReturn = true;
					}
				}
			} else if(action.equals(ACTION_DISCONNECT)){
				conn.close();
			} else if (action.equals(ACTION_CREATE_STREAM) || action.equals(ACTION_DELETE_STREAM) ||
					   action.equals(ACTION_PUBLISH) || action.equals(ACTION_PLAY) || action.equals(ACTION_SEEK) ||
					   action.equals(ACTION_PAUSE) || action.equals(ACTION_CLOSE_STREAM)) {
				IStreamService streamService = (IStreamService) getScopeService(conn.getScope(), IStreamService.STREAM_SERVICE, StreamService.class);
				invokeCall(conn, call, streamService);
			} else {
				invokeCall(conn, call);
			}
		} else if(conn.isConnected()){
			// Service calls, must be connected.
			invokeCall(conn, call);
		} else {
			// Warn user attemps to call service without being connected
			log.warn("Not connected, closing connection");
			conn.close();
		}
		
		if (invoke instanceof Invoke){
			
			if(call.getStatus() == Call.STATUS_SUCCESS_VOID ||
				call.getStatus() == Call.STATUS_SUCCESS_NULL ){
				log.debug("Method does not have return value, do not reply");
				return;
			}
			Invoke reply = new Invoke();
			reply.setCall(call);
			reply.setInvokeId(invoke.getInvokeId());
			log.debug("sending reply");
			channel.write(reply);
			if (disconnectOnReturn)
				conn.close();
		}
	}
	
	public Object getStatus(String code){
		return statusObjectService.getStatusObject(code);
	}
	
	public void onPing(RTMPConnection conn, Channel channel, PacketHeader source, Ping ping){
		if (ping.getValue1() == 7) {
			// This is the response to an IConnection.ping request
			conn.pingReceived(ping);
			return;
		}
		
		final Ping pong = new Ping();
		pong.setValue1((short) 4);
		pong.setValue2(RTMPHandler.getStreamId());
		channel.write(pong);
		log.info(ping);
		// No idea why this is needed, 
		// but it was the thing stopping the new rtmp code streaming
		final Ping pong2 = new Ping();
		pong2.setValue1((short) 0);
		pong2.setValue2(RTMPHandler.getStreamId());
		channel.write(pong2);
	}
	
	public void onStreamBytesRead(RTMPConnection conn, Channel channel, PacketHeader source, StreamBytesRead streamBytesRead){
		log.info("Stream Bytes Read: "+streamBytesRead.getBytesRead());
		// pass to stream handler
	}
	
	public void onSharedObject(RTMPConnection conn, Channel channel, PacketHeader source, SharedObject object) {
		final ISharedObject so;
		final String name = object.getName();
		IScope scope = conn.getScope();
		ISharedObjectService sharedObjectService = (ISharedObjectService) getScopeService(scope, ISharedObjectService.SHARED_OBJECT_SERVICE, SharedObjectService.class);
		if (!sharedObjectService.hasSharedObject(scope, name)) {
			if (!sharedObjectService.createSharedObject(scope, name, object.isPersistent())) {
				// TODO: return error to client.
				return;
			}
		}
		so = sharedObjectService.getSharedObject(scope, name);
		so.beginUpdate(conn);
		Iterator it = object.getEvents().iterator();
		while (it.hasNext()) {
			SharedObjectEvent event = (SharedObjectEvent) it.next();
			switch (event.getType()) {
			case SO_CONNECT:
				// Register client for this shared object and send initial state
				so.addEventListener(conn);
				break;
			
			case SO_DISCONNECT:
				// The client disconnected from the SO
				so.removeEventListener(conn);
				break;
			
			case SO_SET_ATTRIBUTE:
				// The client wants to update an attribute
				so.setAttribute(event.getKey(), event.getValue());
				break;
			
			case SO_DELETE_ATTRIBUTE:
				// The client wants to remove an attribute
				so.removeAttribute(event.getKey());
				break;
				
			case SO_SEND_MESSAGE:
				// The client wants to send a message
				so.sendMessage(event.getKey(), (List) event.getValue());
				break;
				
			default:
				log.error("Unknown shared object update event " + event.getType());
			}
		}
		so.endUpdate();
	}
	
}