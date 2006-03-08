package org.red5.server.net;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.red5.server.context.GlobalContext;
import org.red5.server.service.ServiceInvoker;

public class BaseHandler extends IoHandlerAdapter {

	protected static Log log =
        LogFactory.getLog(BaseHandler.class.getName());
	
	public GlobalContext globalContext = null;
	public ServiceInvoker serviceInvoker = null;
	public ProtocolCodecFactory codecFactory = null;
	
	public void setGlobalContext(GlobalContext globalContext) {
		this.globalContext = globalContext;
	}
	
	public void setServiceInvoker(ServiceInvoker serviceInvoker) {
		this.serviceInvoker = serviceInvoker;
	}
	
	public void setCodecFactory(ProtocolCodecFactory codecFactory) {
		this.codecFactory = codecFactory;
	}

	public void exceptionCaught(IoSession arg0, Throwable arg1) throws Exception {
		// TODO Auto-generated method stub
		super.exceptionCaught(arg0, arg1);
	}

	public void messageReceived(IoSession arg0, Object arg1) throws Exception {
		// TODO Auto-generated method stub
		super.messageReceived(arg0, arg1);
	}

	public void messageSent(IoSession arg0, Object arg1) throws Exception {
		// TODO Auto-generated method stub
		super.messageSent(arg0, arg1);
	}

	public void sessionClosed(IoSession arg0) throws Exception {
		// TODO Auto-generated method stub
		super.sessionClosed(arg0);
	}

	public void sessionCreated(IoSession arg0) throws Exception {
		// TODO Auto-generated method stub
		super.sessionCreated(arg0);
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
