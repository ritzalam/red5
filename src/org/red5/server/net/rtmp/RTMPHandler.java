package org.red5.server.net.rtmp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.server.net.ProtocolState;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.OutPacket;
import org.red5.server.service.Call;
import org.red5.server.stream.Stream;

/*
 * Mina implementation of the RTMP handler.
 * 
 */
public class RTMPHandler extends BaseRTMPHandler {

	protected static Log log =
        LogFactory.getLog(RTMPHandler.class.getName());
	
	//	 ------------------------------------------------------------------------------
	
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.debug("Exception caught", cause);
	}

	public void messageReceived(IoSession session, Object in) throws Exception {
		final Connection conn = (Connection) session.getAttachment();
		final ProtocolState state = (ProtocolState) session.getAttribute(RTMP.SESSION_KEY);
		
		messageReceived(conn, state, in);
	}
	
	public void messageSent(IoSession session, Object message) throws Exception {
		final Connection conn = (Connection) session.getAttachment();

		messageSent(conn, message);
	}

	public void sessionClosed(IoSession session) throws Exception {
		final RTMP rtmp = (RTMP) session.getAttribute(RTMP.SESSION_KEY);
		final Connection conn = (Connection) session.getAttachment();
		
		connectionClosed(conn, rtmp);
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
}
