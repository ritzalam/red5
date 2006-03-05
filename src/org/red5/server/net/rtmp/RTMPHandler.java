package org.red5.server.net.rtmp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.server.context.AppContext;
import org.red5.server.context.BaseApplication;
import org.red5.server.context.HostContext;
import org.red5.server.context.PersistentSharedObject;
import org.red5.server.context.Scope;
import org.red5.server.net.BaseHandler;
import org.red5.server.net.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.InPacket;
import org.red5.server.net.rtmp.message.Invoke;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.net.rtmp.message.PacketHeader;
import org.red5.server.net.rtmp.message.Ping;
import org.red5.server.net.rtmp.message.SharedObject;
import org.red5.server.net.rtmp.message.SharedObjectEvent;
import org.red5.server.net.rtmp.message.StreamBytesRead;
import org.red5.server.net.rtmp.message.Unknown;
import org.red5.server.net.rtmp.status.StatusObjectService;
import org.red5.server.service.Call;
import org.red5.server.stream.Stream;

public class RTMPHandler extends BaseHandler implements Constants{

	protected static Log log =
        LogFactory.getLog(RTMPHandler.class.getName());
	
	public StatusObjectService statusObjectService = null;

	public void setStatusObjectService(StatusObjectService statusObjectService) {
		this.statusObjectService = statusObjectService;
	}
	
	//	 ------------------------------------------------------------------------------
	
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.debug("Exception caught", cause);
	}

	public void messageReceived(IoSession session, Object in) throws Exception {
		final Connection conn = (Connection) session.getAttachment();
		final ProtocolState state = (ProtocolState) session.getAttribute(RTMP.SESSION_KEY);
		
		messageReceived(conn, state, in);
	}
	
	public void messageReceived(Connection conn, ProtocolState state, Object in) throws Exception {
	
		if(in instanceof ByteBuffer){
			rawBufferRecieved(conn, state, (ByteBuffer) in);
			return;
		}
		
		try {
			
			
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
			Scope.setChannel(channel);
			Scope.setStream(stream);
			Scope.setStatusObjectService(statusObjectService);
			
			switch(message.getDataType()){
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
		BaseApplication app = (BaseApplication) ctx.getBean(AppContext.APP_SERVICE_NAME);
		String name = request.getName();
		
		log.debug("Received SO request from " + channel + "(" + request + ")");
		org.red5.server.api.SharedObject so = app.getSharedObject(name, request.isPersistent());
		
		so.beginUpdate();
		Iterator it = request.getEvents().iterator();
		while (it.hasNext()) {
			SharedObjectEvent event = (SharedObjectEvent) it.next();
			
			switch (event.getType())
			{
			case SO_CONNECT:
				// Register client for this shared object and send initial state
				so.registerClient(conn, source.getChannelId());
				break;
			
			case SO_CLEAR:
				// Clear the shared object
				if (!request.isPersistent())
					so.unregisterClient(conn);
				
				/* XXX: should we really clear the SO here?  I think this is rather
				 *      a "disconnect" - the same event is sent for the "clear" method
				 *      as well as the disconnect of a client.
				 */
				so.clear();
				break;
			
			case SO_SET_ATTRIBUTE:
				// The client wants to update an attribute
				so.updateAttribute(event.getKey(), event.getValue());
				break;
			
			case SO_DELETE_ATTRIBUTE:
				// The client wants to remove an attribute
				so.deleteAttribute(event.getKey());
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
	
	private void rawBufferRecieved(Connection conn, ProtocolState state, ByteBuffer in) {
		
		final RTMP rtmp = (RTMP) state;
		
		if(rtmp.getState() != RTMP.STATE_HANDSHAKE){
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
		conn.write(out);
	}

	public void messageSent(IoSession session, Object message) throws Exception {
		final Connection conn = (Connection) session.getAttachment();

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

	public void sessionClosed(IoSession session) throws Exception {
		final RTMP rtmp = (RTMP) session.getAttribute(RTMP.SESSION_KEY);
		final Connection conn = (Connection) session.getAttachment();
		rtmp.setState(RTMP.STATE_DISCONNECTED);
		conn.close();
		invokeCall(conn, new Call("disconnect"));
		if(log.isDebugEnabled())
			log.debug("Session closed");
	}

	public void sessionCreated(IoSession session) throws Exception {
		if(log.isDebugEnabled())
			log.debug("Session created");
		
		// moved protocol state from connection object to rtmp object
		session.setAttribute(RTMP.SESSION_KEY,new RTMP(RTMP.MODE_SERVER));
		
		session.getFilterChain().addFirst(
                "protocolFilter",new ProtocolCodecFilter(codecFactory) );
        session.getFilterChain().addLast(
                "logger", new LoggingFilter() );
        
		session.setAttachment(new Connection(session));
		
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
		log.info("Stream Bytes Read: "+streamBytesRead.getBytesRead());
		// TODO: pass this to streaming code
		// can be used to work out client bandwidth
	}
	
	//	 ---------------------------------------------------------------------------
}
