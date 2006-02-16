package org.red5.server.net.proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.support.SimpleProtocolDecoderOutput;
import org.apache.mina.util.Queue;

public class DecoderLogFilter extends IoFilterAdapter {

	protected static Log log =
        LogFactory.getLog(DecoderLogFilter.class.getName());
	
	protected ProtocolCodecFactory codec;
	
	public ProtocolCodecFactory getCodec() {
		return codec;
	}

	public void setCodec(ProtocolCodecFactory codec) {
		this.codec = codec;
	}

	public void filterWrite(NextFilter next, IoSession session, WriteRequest write) throws Exception {
		
		if(write.getMessage() instanceof ByteBuffer) {
			decodeBuffer(session,(ByteBuffer) write.getMessage());
		}

		super.filterWrite(next, session, write);
	}

	public void messageReceived(NextFilter next, IoSession session, Object message) throws Exception {
		
		if(message instanceof ByteBuffer) {
			decodeBuffer(session,(ByteBuffer) message);
		}
		
		super.messageReceived(next, session, message);
	}

	public void decodeBuffer(IoSession session, ByteBuffer buf){
		
		final SimpleProtocolDecoderOutput out = new SimpleProtocolDecoderOutput();
		
		try {
			codec.getDecoder().decode(session, buf, out);
		} catch (ProtocolCodecException e) {
			log.error("Decoding Error", e);
		} catch (Exception e){
			log.error("Decoding Error", e);
		}
	
		Queue queue = out.getMessageQueue();
		while(!queue.isEmpty()){
			logMessage(session, queue.pop());
		}
	}
	
	public void logMessage(IoSession session, Object message){
		log.debug(message);
	}
	
	public void sessionClosed(NextFilter arg0, IoSession arg1) throws Exception {
		// TODO Auto-generated method stub
		super.sessionClosed(arg0, arg1);
	}

	public void sessionCreated(NextFilter arg0, IoSession arg1) throws Exception {
		// TODO Auto-generated method stub
		super.sessionCreated(arg0, arg1);
	}

	public void sessionOpened(NextFilter arg0, IoSession arg1) throws Exception {
		// TODO Auto-generated method stub
		super.sessionOpened(arg0, arg1);
	}
	
}
