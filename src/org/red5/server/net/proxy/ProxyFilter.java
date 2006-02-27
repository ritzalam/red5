package org.red5.server.net.proxy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoSession;

public class ProxyFilter extends IoFilterAdapter {

	public static final String FORWARD_KEY = "proxy_forward_key";
	
	protected static Log log =
        LogFactory.getLog(ProxyFilter.class.getName());
	
	protected String name;
	
	public ProxyFilter(String name){
		this.name = name;
	}
	
	public void messageReceived(NextFilter next, IoSession session, Object message) throws Exception {
		IoSession forward = (IoSession) session.getAttribute(FORWARD_KEY);
		if(forward != null && forward.isConnected()) {
			
			if(message instanceof ByteBuffer){
				final ByteBuffer buf = (ByteBuffer) message;
				//buf.acquire();
				
				if(log.isDebugEnabled())
					log.debug("[ "+name+" ] RAW >> "+buf.getHexDump());
				
				ByteBuffer copy = ByteBuffer.allocate(buf.limit());
				int limit = buf.limit();
				copy.put(buf);
				copy.flip();
				forward.write(copy);
				buf.flip();
				buf.position(0);
				buf.limit(limit);
				//buf.flip();
			}
			
			if(message instanceof ByteBuffer){
				final ByteBuffer buf = (ByteBuffer) message;
				//buf.flip();
			}
		}
		next.messageReceived(session, message);
	}

	public void sessionClosed(NextFilter next, IoSession session) throws Exception {
		IoSession forward = (IoSession) session.getAttribute(FORWARD_KEY);
		if(forward != null && forward.isConnected() && ! forward.isClosing()) {
			log.debug("[ "+name+" ] Closing: "+forward);
			forward.close();
		}
		next.sessionClosed(session);
	}

}