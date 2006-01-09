package org.red5.server.proxy;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.SessionConfig;
import org.apache.mina.common.TransportType;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.socket.SocketSessionConfig;
import org.apache.mina.protocol.ProtocolDecoder;
import org.apache.mina.protocol.ProtocolEncoder;
import org.apache.mina.protocol.ProtocolFilterChain;
import org.apache.mina.protocol.ProtocolHandler;
import org.apache.mina.protocol.ProtocolSession;
import org.apache.mina.protocol.ProtocolViolationException;
import org.apache.mina.protocol.SimpleProtocolDecoderOutput;
import org.apache.mina.util.Queue;
import org.red5.server.rtmp.Channel;
import org.red5.server.rtmp.Connection;
import org.red5.server.rtmp.message.Constants;
import org.red5.server.rtmp.message.InPacket;
import org.red5.server.rtmp.message.Message;
import org.red5.server.rtmp.message.PacketHeader;

public class DebugProxyHandler extends ProxyHandler {

	protected static Log log =
        LogFactory.getLog(DebugProxyHandler.class.getName());
	
	protected ProtocolDecoder decoder = null;
	protected HashMap protocolSessions = new HashMap();

	public void setDecoder(ProtocolDecoder decoder) {
		this.decoder = decoder;
	}
	
	public void sessionCreated(IoSession session) throws Exception {
		SessionConfig cfg = session.getConfig();
		try {
			if (cfg instanceof SocketSessionConfig) {
				SocketSessionConfig sessionConfig = (SocketSessionConfig) cfg;
				//sessionConfig.setSessionReceiveBufferSize(5000);
				//sessionConfig.setSendBufferSize(5000);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		super.sessionCreated(session);
		final ProxyConnector conn = (ProxyConnector) session.getAttachment();
		final ProtocolSession protocolSession = new MockProtocolSession();
		final Connection connection = new Connection(protocolSession);
		connection.setMode(conn!=null ? Connection.MODE_CLIENT : Connection.MODE_SERVER);;
		protocolSession.setAttachment(connection);
		protocolSessions.put(session,protocolSession);
	}

	public void decodeBuffer(IoSession session, ByteBuffer in){
		//log.info("DECODE BUFFER");
		final SimpleProtocolDecoderOutput out = new SimpleProtocolDecoderOutput();
		final ProtocolSession protocolSession = (ProtocolSession) protocolSessions.get(session);
		final ProxyConnector conn = (ProxyConnector) session.getAttachment();
		final Connection connection = (Connection) protocolSession.getAttachment();
		/*
		int handshakeSize = (Constants.HANDSHAKE_SIZE * 2);
		if(conn.isUp()) handshakeSize += 1;
		if(conn.isDown()) handshakeSize += 0;
		if(connection.getState() == Connection.STATE_CONNECT){			
			conn.getLog().debug("hs: "+handshakeSize +" skipped:"+ conn.getBytesSkipped()+" remain:"+in.remaining());
			if(conn.getBytesSkipped() < handshakeSize){
				int skip = (int) handshakeSize - conn.getBytesSkipped();
				if(skip > in.remaining()) skip = in.remaining();
				in.position(in.position()+skip);
				conn.setBytesSkipped(conn.getBytesSkipped()+skip);
			} 
			if(conn.getBytesSkipped() == handshakeSize){
				conn.getLog().debug("Passed handshake, set state == connected");
				connection.setState(Connection.STATE_CONNECTED);
				//log.debug(">>>"+in.getHexDump());
			} else {
				return; 
			}
		}
		*/
		try {
			decoder.decode(protocolSession,in,out);
		} catch (ProtocolViolationException e) {
			conn.getLog().error("Decoding error", e);
		}
		Queue queue = out.getMessageQueue();
		while(!queue.isEmpty()){
			messageReceived(conn.getLog(), protocolSession, queue.pop());
		}
		//queue.
	}
	
	public void messageReceived(Log log, ProtocolSession session, Object in) {
		
		if(in instanceof ByteBuffer){
			log.debug("Handskake");
			return;
		}
		
		try {
			
			final Connection conn = (Connection) session.getAttachment();
						
			final InPacket packet = (InPacket) in;
			final Message message = packet.getMessage();
			final PacketHeader source = packet.getSource();
			final Channel channel = conn.getChannel(packet.getSource().getChannelId());
			
			log.info(source);
			log.info(message);
			
			//log.info(source + " | " + message );
			
		} catch (RuntimeException e) {
			// TODO Auto-generated catch block
			log.debug("Exception",e);
		}
	}
	
	
	public class MockProtocolSession implements ProtocolSession {
		
		private Object attachment = null;
		private HashMap attributes = new HashMap();
		
		public Object getAttachment() {
			return attachment;
		}

		public Object setAttachment(Object attachment) {
			this.attachment = attachment;
			return attachment;
		}

		public ProtocolDecoder getDecoder() {
			return decoder;
		}

		public Object getAttribute(String key) {
			return attributes.get(key);
		}

		public Set getAttributeKeys() {
			return attributes.keySet();
		}
		
		public Object removeAttribute(String key) {
			return attributes.get(key);
		}

		public Object setAttribute(String key, Object value) {
			attributes.put(key, value);
			return value;
		}
		
		// -------------------------------------------------------------------------
		
		// Methods below are not called
		
		public void write(Object message) {
			// TODO Auto-generated method stub
		}
		
		public ProtocolEncoder getEncoder() {
			// TODO Auto-generated method stub
			return null;
		}

		public ProtocolFilterChain getFilterChain() {
			// TODO Auto-generated method stub
			return null;
		}

		public ProtocolHandler getHandler() {
			// TODO Auto-generated method stub
			return null;
		}
		
		public void close() {
			// TODO Auto-generated method stub
			
		}

		public void close(boolean wait) {
			// TODO Auto-generated method stub
		}

		

		public SessionConfig getConfig() {
			// TODO Auto-generated method stub
			return null;
		}

		public long getCreationTime() {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getIdleCount(IdleStatus status) {
			// TODO Auto-generated method stub
			return 0;
		}

		public long getLastIdleTime(IdleStatus status) {
			// TODO Auto-generated method stub
			return 0;
		}

		public long getLastIoTime() {
			// TODO Auto-generated method stub
			return 0;
		}

		public long getLastReadTime() {
			// TODO Auto-generated method stub
			return 0;
		}

		public long getLastWriteTime() {
			// TODO Auto-generated method stub
			return 0;
		}

		public SocketAddress getLocalAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		public long getReadBytes() {
			// TODO Auto-generated method stub
			return 0;
		}

		public SocketAddress getRemoteAddress() {
			// TODO Auto-generated method stub
			return null;
		}

		public int getScheduledWriteRequests() {
			// TODO Auto-generated method stub
			return 0;
		}

		public TransportType getTransportType() {
			// TODO Auto-generated method stub
			return TransportType.SOCKET;
		}

		public long getWrittenBytes() {
			// TODO Auto-generated method stub
			return 0;
		}

		public long getWrittenWriteRequests() {
			// TODO Auto-generated method stub
			return 0;
		}

		public boolean isConnected() {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isIdle(IdleStatus status) {
			// TODO Auto-generated method stub
			return false;
		}

		
		
	}
	
}
