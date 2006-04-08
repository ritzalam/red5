package org.red5.server.net.rtmp.codec;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.red5.server.net.protocol.ProtocolState;

public class RTMPMinaProtocolEncoder extends RTMPProtocolEncoder implements ProtocolEncoder {

	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws ProtocolCodecException {
		try {
			final ProtocolState state = (ProtocolState) session.getAttribute(ProtocolState.SESSION_KEY);
			final ByteBuffer buf = encode(state, message);
			if(buf != null) out.write(buf);
		} catch(Exception ex){
			log.error(ex);
		}
	}
	
	public void dispose(IoSession ioSession) throws Exception {
		// TODO Auto-generated method stub		
	}
}
