package org.red5.server.net.proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.Connection;
import org.red5.server.net.rtmp.message.InPacket;
import org.red5.server.net.rtmp.message.Message;
import org.red5.server.net.rtmp.message.PacketHeader;

public class DebugProxyHandler extends ProxyHandler {

	protected static Log log =
        LogFactory.getLog(DebugProxyHandler.class.getName());
	
	protected ProtocolDecoder decoder = null;

	public void setDecoder(ProtocolDecoder decoder) {
		this.decoder = decoder;
	}
	
	public void decodeBuffer(IoSession session, ByteBuffer in){

		/*
		
		final SimpleProtocolDecoderOutput out = new SimpleProtocolDecoderOutput();
		final IoSession ioSession = (IoSession) protocolSessions.get(session);
		final ProxyConnector conn = (ProxyConnector) session.getAttachment();
		final Connection connection = (Connection) ioSession.getAttachment();
		try {
			decoder.decode(ioSession,in,out);
		} catch (ProtocolCodecException e) {
			conn.getLog().error("Decoding error", e);
		} catch (Exception e){
			conn.getLog().error("Decoding error", e);
		}
		
		Queue queue = out.getMessageQueue();
		while(!queue.isEmpty()){
			messageReceived(conn.getLog(), ioSession, queue.pop());
		}
		//queue.
		
		*/
	}
	
	public void messageReceived(Log log, IoSession session, Object in) {
		
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

	
}
