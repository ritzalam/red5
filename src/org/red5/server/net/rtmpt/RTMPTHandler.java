package org.red5.server.net.rtmpt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.net.protocol.ProtocolState;
import org.red5.server.net.protocol.SimpleProtocolCodecFactory;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;

/*
 * Jetty implementation of the RTMP handler.
 * 
 */
public class RTMPTHandler extends RTMPHandler implements Constants {

	protected static Log log =
        LogFactory.getLog(RTMPTHandler.class.getName());
	
	public static final String HANDLER_ATTRIBUTE = "red5.RMPTHandler";
	
	protected SimpleProtocolCodecFactory codecFactory = null;
	
	public void setCodecFactory(SimpleProtocolCodecFactory factory) {
		this.codecFactory = factory;		
	}
	
	public SimpleProtocolCodecFactory getCodecFactory() {
		return this.codecFactory;		
	}

	private void rawBufferRecieved(RTMPConnection conn, ProtocolState state, ByteBuffer in) {
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
		
		conn.rawWrite(out);
	}
	
	public void messageReceived(RTMPConnection conn, ProtocolState state, Object in) throws Exception {
		if (in instanceof ByteBuffer)
			rawBufferRecieved(conn, state, (ByteBuffer) in);
		else
			super.messageReceived(conn, state, in);
	}
	
}
