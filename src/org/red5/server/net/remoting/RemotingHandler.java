package org.red5.server.net.remoting;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.LoggingFilter;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.red5.server.service.ServiceInvoker;
import org.red5.server.zcontext.GlobalContext;
//import org.red5.server.net.remoting.message.RemotingResponse;

public class RemotingHandler extends IoHandlerAdapter {

	protected static Log log =
        LogFactory.getLog(RemotingHandler.class.getName());
	
	private ProtocolCodecFactory codecFactory = null;
	private ServiceInvoker serviceInvoker = null;
	private GlobalContext globalContext = null;

	public void setGlobalContext(GlobalContext globalContext) {
		this.globalContext = globalContext;
	}
	
	public void setCodecFactory(ProtocolCodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}
	
	public void setServiceInvoker(ServiceInvoker serviceInvoker) {
		this.serviceInvoker = serviceInvoker;
	}

	public void exceptionCaught(IoSession arg0, Throwable arg1) throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(arg0, arg1);
	}

	public void messageReceived(IoSession session, Object message) throws Exception {
		log.info("Message recieved: "+message);
		if(!(message instanceof RemotingPacket))
			return;
		RemotingPacket req = (RemotingPacket) message;
		Iterator it = req.getCalls().iterator();
		while(it.hasNext()){
			RemotingCall call = (RemotingCall) it.next();
			//serviceInvoker.invoke(call, globalContext);
			call.setResult(call.getArguments()[0]);
			call.setStatus(RemotingCall.STATUS_SUCCESS_RESULT);
		}
		//RemotingResponse resp = new RemotingResponse(req.getCalls());
		//session.write(resp).join(); // wait for it to write
	}

	public void messageSent(IoSession arg0, Object arg1) throws Exception {
		// TODO Auto-generated method stub
		super.messageSent(arg0, arg1);
	}

	public void sessionClosed(IoSession arg0) throws Exception {
		// TODO Auto-generated method stub
		super.sessionClosed(arg0);
	}

	public void sessionCreated(IoSession session) throws Exception {
		// TODO Auto-generated method stub
		
		session.getFilterChain().addFirst(
                "protocolFilter",new ProtocolCodecFilter(codecFactory) );
        session.getFilterChain().addLast(
                "logger", new LoggingFilter() );
		
	}

	public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {
		// TODO Auto-generated method stub
		super.sessionIdle(arg0, arg1);
	}

	public void sessionOpened(IoSession arg0) throws Exception {
		// TODO Auto-generated method stub
		super.sessionOpened(arg0);
	}

	
}
