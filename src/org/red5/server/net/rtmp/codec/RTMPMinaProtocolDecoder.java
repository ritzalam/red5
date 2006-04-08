package org.red5.server.net.rtmp.codec;

import java.util.Iterator;
import java.util.List;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoder; 
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.red5.server.net.protocol.ProtocolState;

public class RTMPMinaProtocolDecoder extends RTMPProtocolDecoder implements ProtocolDecoder {

    public void decode( IoSession session, ByteBuffer in,
            ProtocolDecoderOutput out ) throws ProtocolCodecException {
		
    	final ProtocolState state = (ProtocolState) session.getAttribute(ProtocolState.SESSION_KEY);
    	
		ByteBuffer buf = (ByteBuffer) session.getAttribute("buffer");
		if(buf==null){
			buf = ByteBuffer.allocate(2048);
			buf.setAutoExpand(true);
			session.setAttribute("buffer",buf);
		}
		buf.put(in);
		buf.flip();
		
		List objects = decodeBuffer(state, buf);
		if (objects == null || objects.isEmpty())
			return;
			
		Iterator it = objects.iterator();
		while (it.hasNext())
			out.write(it.next());
    }
	
	public void dispose(IoSession ioSession) throws Exception {
		// TODO Auto-generated method stub
		
	}
}
