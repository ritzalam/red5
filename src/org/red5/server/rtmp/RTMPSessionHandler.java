package org.red5.server.rtmp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.io.socket.SocketSessionConfig;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;
import org.red5.server.context.AppContext;
import org.red5.server.context.BaseApplication;
import org.red5.server.context.Client;
import org.red5.server.context.GlobalContext;
import org.red5.server.context.HostContext;
import org.red5.server.context.PersistentSharedObject;
import org.red5.server.context.Scope;
import org.red5.server.rtmp.message.Constants;
import org.red5.server.rtmp.message.Handshake;
import org.red5.server.rtmp.message.HandshakeReply;
import org.red5.server.rtmp.message.InPacket;
import org.red5.server.rtmp.message.Invoke;
import org.red5.server.rtmp.message.Message;
import org.red5.server.rtmp.message.OutPacket;
import org.red5.server.rtmp.message.PacketHeader;
import org.red5.server.rtmp.message.Ping;
import org.red5.server.rtmp.message.SharedObject;
import org.red5.server.rtmp.message.SharedObjectEvent;
import org.red5.server.rtmp.message.StreamBytesRead;
import org.red5.server.rtmp.message.Unknown;
import org.red5.server.rtmp.status.StatusObjectService;
import org.red5.server.service.Call;
import org.red5.server.service.ServiceInvoker;
import org.red5.server.stream.Stream;

public class RTMPSessionHandler implements ProtocolHandler, Constants{

	protected static Log log =
        LogFactory.getLog(RTMPSessionHandler.class.getName());
	
	public StatusObjectService statusObjectService = null;
	public GlobalContext globalContext = null;
	public ServiceInvoker serviceInvoker = null;

	public void setStatusObjectService(StatusObjectService statusObjectService) {
		this.statusObjectService = statusObjectService;
	}
	
	public void setGlobalContext(GlobalContext globalContext) {
		this.globalContext = globalContext;
	}
	
	public void setServiceInvoker(ServiceInvoker serviceInvoker) {
		this.serviceInvoker = serviceInvoker;
	}
	
	//	 ------------------------------------------------------------------------------

	public void exceptionCaught(ProtocolSession session, Throwable cause) throws Exception {
		// TODO Auto-generated method stub
		log.error("Exception caught", cause);
	}

	public void messageReceived(ProtocolSession session, Object in) throws Exception {
	
		if(in instanceof ByteBuffer){
			rawBufferRecieved(session, (ByteBuffer) in);
			return;
		}
		
		try {
			
			
			final Connection conn = (Connection) session.getAttachment();
			final InPacket packet = (InPacket) in;
			final Message message = packet.getMessage();
			final PacketHeader source = packet.getSource();
			final Channel channel = conn.getChannel(packet.getSource().getChannelId());
			final Stream stream = conn.getStreamByChannelId(channel.getId());
			
			if(log.isDebugEnabled()){
				log.debug("Message recieved");
				log.debug("Stream Id: "+source);
				log.debug("Channel: "+channel);
			}
				
			if(stream != null){
				stream.setStreamId(source.getStreamId());
			}
			
			// Is this a bad for performance ?
			Scope.setClient(conn);
			Scope.setStream(stream);
			Scope.setStatusObjectService(statusObjectService);
			
			switch(message.getDataType()){
			case TYPE_HANDSHAKE:
				onHandshake(conn, channel, source, (Handshake) message);
				break;
			case TYPE_INVOKE:
			case TYPE_NOTIFY: // just like invoke, but does not return
				onInvoke(conn, channel, source, (Invoke) message);
				break;
			case TYPE_PING:
				onPing(conn, channel, source, (Ping) message);
				break;
			case TYPE_STREAM_BYTES_READ:
				onStreamBytesRead(conn, channel, source, (StreamBytesRead) message);
				break;
			case TYPE_AUDIO_DATA:
			case TYPE_VIDEO_DATA:
				log.info("in packet: "+source.getSize()+" ts:"+source.getTimer());
				stream.publish(message);
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

	protected void onSharedObject(Connection conn, Channel channel, PacketHeader  source, SharedObject request) {
		AppContext ctx = conn.getAppContext();
		BaseApplication app = (BaseApplication) ctx.getBean(ctx.APP_SERVICE_NAME);
		String name = request.getName();
		
		PersistentSharedObject so = app.getSharedObject(name);
		if (so == null) {
			log.info("Creating new shared object " + name);
			so = new PersistentSharedObject(name);
			app.setSharedObject(name, so);
		}

		SharedObject reply = new SharedObject();
		reply.setName(name);
		reply.setTimestamp(0);
		
		SharedObject sync = new SharedObject();
		sync.setName(name);
		sync.setTimestamp(0);
		
		boolean updates = false;
		
		Iterator it = request.getEvents().iterator();
		while (it.hasNext()) {
			SharedObjectEvent event = (SharedObjectEvent) it.next();
			
			switch (event.getType())
			{
			case SO_CONNECT:
				// Register client for this shared object and send initial state
				reply.addEvent(new SharedObjectEvent(SO_CLIENT_INITIAL_DATA, null, null));
				if (!so.getData().isEmpty())
					reply.addEvent(new SharedObjectEvent(SO_CLIENT_UPDATE_DATA, null, so.getData()));
				so.registerClient(conn, source.getChannelId());
				break;
			
			case SO_SET_ATTRIBUTE:
				// The client wants to update an attribute
				so.updateAttribute(event.getKey(), event.getValue());
				// Send confirmation to client
				reply.addEvent(new SharedObjectEvent(SO_CLIENT_UPDATE_ATTRIBUTE, event.getKey(), null));
				sync.addEvent(new SharedObjectEvent(SO_CLIENT_UPDATE_DATA, event.getKey(), event.getValue()));
				updates = true;
				break;
			
			case SO_DELETE_ATTRIBUTE:
				// The client wants to remove an attribute
				so.deleteAttribute(event.getKey());
				// Send confirmation to client
				reply.addEvent(new SharedObjectEvent(SO_CLIENT_DELETE_DATA, event.getKey(), null));
				sync.addEvent(new SharedObjectEvent(SO_CLIENT_DELETE_DATA, event.getKey(), null));
				updates = true;
				break;
				
			default:
				log.error("Unknown shared object update event " + event.getType());
			}
		}
		
		if (updates)
			// The client sent at least one update -> increase version of SO
			so.updateVersion();
		
		reply.setSoId(so.getVersion());
		channel.write(reply);
		
		if (updates && sync.getEvents().size() > 0) {
			// Synchronize updates with all registered clients of this shared object
			sync.setSoId(so.getVersion());
			// Acquire the packet, this will stop the data inside being released
			sync.acquire();
			HashMap all_clients = so.getClients();
			Iterator clients = all_clients.keySet().iterator();
			while (clients.hasNext()) {
				Connection connection = (Connection) clients.next();
				if (connection == conn) {
					// Don't re-send update to active client
					log.info("Skipped " + connection);
					continue;
				}
				
				Iterator channels = ((HashSet) all_clients.get(connection)).iterator();
				while (channels.hasNext()) {
					Channel c = connection.getChannel(((Integer) channels.next()).byteValue());
					c.write(sync);
				}
			}
			// After sending the packet down all the channels we can release the packet, 
			// which in turn will allow the data buffer to be released
			sync.release();
		}
	}
	
	private void rawBufferRecieved(ProtocolSession session, ByteBuffer in) {
		
		final Connection conn = (Connection) session.getAttachment();
		
		if(conn.getState() != Connection.STATE_HANDSHAKE){
			log.warn("Raw buffer after handshake, something odd going on");
		}
		
		ByteBuffer out = ByteBuffer.allocate((Constants.HANDSHAKE_SIZE*2)+1);
		
		if(log.isDebugEnabled()){
			log.debug("Writing handshake reply");
			log.debug("handskake size:"+in.remaining());
		}
		
		out.put((byte)0x03);
		out.fill((byte)0x00,Constants.HANDSHAKE_SIZE);
		out.put(in).flip();
		session.write(out);
	}

	public void messageSent(ProtocolSession session, Object message) throws Exception {
		final Connection conn = (Connection) session.getAttachment();
		// TODO Auto-generated method stub
		if(log.isDebugEnabled())
			log.debug("Message sent");
		
		if(message instanceof ByteBuffer){
			return;
		}
		
		OutPacket sent = (OutPacket) message;
		final byte channelId = sent.getDestination().getChannelId();
		final Stream stream = conn.getStreamByChannelId(channelId);
		if(stream!=null){
			stream.written(sent.getMessage());
		}
	}

	public void sessionClosed(ProtocolSession session) throws Exception {
		final Connection conn = (Connection) session.getAttachment();
		conn.setState(Connection.STATE_DISCONNECTED);
		invokeCall(conn, new Call("disconnect"));
		if(log.isDebugEnabled())
			log.debug("Session closed");
	}

	public void sessionCreated(ProtocolSession session) throws Exception {
		if(log.isDebugEnabled())
			log.debug("Session created");
		
		SessionConfig cfg = session.getConfig();
		
		try {
			if (cfg instanceof SocketSessionConfig) {
				SocketSessionConfig sessionConfig = (SocketSessionConfig) cfg;
				sessionConfig.setSessionReceiveBufferSize(256);
				sessionConfig.setSendBufferSize(256);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		session.setAttachment(new Connection(session));
	}

	public void sessionIdle(ProtocolSession session, IdleStatus status) throws Exception {
		// TODO Auto-generated method stub
		log.debug("Session idle");
	}

	public void sessionOpened(ProtocolSession session) throws Exception {
		log.debug("Session opened");
	}

	// ------------------------------------------------------------------------------
	
	public AppContext lookupAppContext(Connection conn){
		
		final String app = conn.getParameter("app");
		final String hostname = conn.getParameter("tcUrl").split("/")[2];
		
		if(log.isDebugEnabled()){
			log.debug("Hostname: "+hostname);
			log.debug("App: "+app);
		}
			
		final HostContext host = (globalContext.hasHostContext(hostname)) ?
				globalContext.getHostContext(hostname) : globalContext.getDefaultHost();
		
		if(!host.hasAppContext(app)){
			log.warn("Application \"" + app + "\" not found");
			return null; // todo close connection etc, send status etc
		}
		
		return host.getAppContext(app);
		
	}
	
	public void invokeCall(Connection conn, Call call){
		
		if(call.getServiceName()==null){
			call.setServiceName(AppContext.APP_SERVICE_NAME);
		} 
		
		serviceInvoker.invoke(call, conn.getAppContext());
		
	}
	
	// ------------------------------------------------------------------------------
	
	public void onHandshake(Connection conn, Channel channel, PacketHeader source, Handshake handshake){
		log.debug("Handshake Connect");
		log.debug("Channel: "+channel);

		conn.setState(Connection.STATE_HANDSHAKE);
		final HandshakeReply reply = new HandshakeReply();
		reply.getData().put(handshake.getData()).flip();
		channel.write(reply);
	}
	

	public void onInvoke(Connection conn, Channel channel, PacketHeader source, Invoke invoke){
		
		log.debug("Invoke");
		
		if(invoke.getConnectionParams()!=null){
			log.debug("Setting connection params: "+invoke.getConnectionParams());
			conn.setParameters(invoke.getConnectionParams());
			log.debug("Setting application context");
			conn.setAppContext(lookupAppContext(conn));
		}
		
		final Call call = invoke.getCall();
		
		invokeCall(conn,call);
		
		if(invoke.isAndReturn()){
		
			if(call.getStatus() == Call.STATUS_SUCCESS_VOID ||
				call.getStatus() == Call.STATUS_SUCCESS_NULL ){
				log.debug("Method does not have return value, do not reply");
				return;
			}
			Invoke reply = new Invoke();
			reply.setCall(call);
			reply.setInvokeId(invoke.getInvokeId());
			channel.write(reply);
		}
	}
	
	public void onPing(Connection conn, Channel channel, PacketHeader source, Ping ping){
		final Ping pong = new Ping();
		pong.setValue1((short)(ping.getValue1()+1));
		pong.setValue2(ping.getValue2());
		channel.write(pong);
		log.info(ping);
		// No idea why this is needed, 
		// but it was the thing stopping the new rtmp code streaming
		final Ping pong2 = new Ping();
		pong2.setValue1((short)0);
		pong2.setValue2(1);
		channel.write(pong2);
	}
	
	public void onStreamBytesRead(Connection conn, Channel channel, PacketHeader source, StreamBytesRead streamBytesRead){
		// get the stream, pass the event to the stream
		log.info("Stream Bytes Read: "+streamBytesRead.getBytesRead());
	}
	
	//	 ---------------------------------------------------------------------------
	
	
	
}
