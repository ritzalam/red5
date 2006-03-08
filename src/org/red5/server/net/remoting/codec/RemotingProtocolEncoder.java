package org.red5.server.net.remoting.codec;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.server.net.remoting.message.RemotingCall;
//import org.red5.server.net.remoting.message.RemotingResponse;

public class RemotingProtocolEncoder implements ProtocolEncoder {

	protected static Log log =
        LogFactory.getLog(RemotingProtocolEncoder.class.getName());

	protected static Log ioLog =
        LogFactory.getLog(RemotingProtocolEncoder.class.getName()+".out");
	
	private Serializer serializer = null;
	
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) 
		throws ProtocolCodecException {
		
		log.info("encode: "+message);
		
		//if(!(message instanceof RemotingResponse)) return;
		log.info("encoding calls");
		try {
			/*
			RemotingResponse resp = (RemotingResponse) message;
			Iterator it = resp.getCalls().iterator();
			ByteBuffer buf = ByteBuffer.allocate(1024);
			buf.setAutoExpand(true);
			Output output = new Output(buf);
			buf.putShort((short) 0); // write the version
			buf.putShort((short) 0); // write the header count
			buf.putShort((short) resp.getCalls().size()); // write the number of bodies
			while(it.hasNext()){
				log.debug("Call");
				RemotingCall call = (RemotingCall) it.next();
				Output.putString(buf,call.getClientResponse());
			   	Output.putString(buf,"null");
			   	buf.putInt(-1);
			   	serializer.serialize(output, call.getClientResult());
			}
			//buf.compact();
			buf.flip();
			log.info(">>"+buf.getHexDump());
			out.write(buf);
			//out.flush();
			*/
		} catch (RuntimeException e) {
			log.error("error",e);
		}
	}				

	public void dispose(IoSession ioSession) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void setSerializer(Serializer serializer) {
		this.serializer = serializer;
	}	
	
}
