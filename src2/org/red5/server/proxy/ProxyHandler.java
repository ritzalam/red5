package org.red5.server.proxy;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.io.IoHandlerAdapter;
import org.apache.mina.io.IoSession;
import org.apache.mina.io.socket.SocketConnector;

public class ProxyHandler extends IoHandlerAdapter {

	protected static Log log =
        LogFactory.getLog(ProxyHandler.class.getName());
	
	private int forwardTimeOut = 10000;
	private String forwardHost = "";
	private int forwardPort = 1935;
	private SocketAddress forwardAddress = null;
	private SocketConnector connector = new SocketConnector();
	
	// -----------------------------------------------------------------------------
	
	public void setForwardHost(String forwardHost) {
		this.forwardHost = forwardHost;
	}

	public void setForwardPort(int forwardPort) {
		this.forwardPort = forwardPort;
	}

	public void setForwardTimeOut(int forwardTimeOut) {
		this.forwardTimeOut = forwardTimeOut;
	}
	
	// -----------------------------------------------------------------------------

	public void initialize(){
		if(forwardHost.equals("")){
			log.warn("Please set forwardHost on proxy");
		} else {
			forwardAddress = new InetSocketAddress(forwardHost,forwardPort);
			log.debug("Initialize proxy: "+forwardAddress);
		}
	}
	
	public void decodeBuffer(IoSession session, ByteBuffer in){
		// do nothing
	}
	
	public void dataRead(IoSession session, ByteBuffer buf) throws Exception {
		final ProxyConnector conn = (ProxyConnector) session.getAttachment();
		conn.getLog().debug(buf.getHexDump());
		int position = buf.position();

		decodeBuffer(session, buf);
		buf.position(position);
		buf.acquire();
		conn.write(buf);
		super.dataRead(session, buf);
	}

	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.error("Error caught", cause);
		super.exceptionCaught(session, cause);
	}

	public void sessionClosed(IoSession session) throws Exception {
		final ProxyConnector conn = (ProxyConnector) session.getAttachment();
		conn.getLog().debug("Session closed");
		conn.close();
		super.sessionClosed(session);
	}
	
	public void sessionCreated(IoSession session) throws Exception {
		if(session.getRemoteAddress().equals(forwardAddress)){
			log.debug("Forward connect");
		} else {
			log.debug("Client connect, opening forward channel");
			final IoSession forward = connector.connect(forwardAddress,forwardTimeOut,this);
			if(forward.isConnected()){
				final ProxyConnector up = new ProxyConnector(forward, ProxyConnector.UP);
				final ProxyConnector down = new ProxyConnector(session, ProxyConnector.DOWN);
				session.setAttachment(up);
				forward.setAttachment(down);
			} else {
				log.debug("Forward timeout, closing client session");
				session.close();
			}
		}
		super.sessionCreated(session);
	}
	
	public class ProxyConnector {
		
		public static final boolean UP = true;
		public static final boolean DOWN = false;
		
		private IoSession session;
		private boolean direction = false;
		private Log log = null;
		private int bytesSkipped = 0;
	    
		ProxyConnector(IoSession session, boolean direction){
			this.session = session;
			this.direction = direction;
			final String logName = ProxyConnector.class.getName() 
				+ (direction ? ".UP" : ".DN" ); 
			this.log = LogFactory.getLog(logName);
		}
		
		public void write(ByteBuffer buf){
			session.write(buf,null);
		}
		
		public void close(){
			if(session.isConnected())
				session.close();
		}
		
		public boolean getDirection(){
			return direction;
		}
		
		public boolean isUp(){
			return direction == UP;
		}
		
		public boolean isDown(){
			return direction == DOWN;
		}
		
		public Log getLog(){
			return log;
		}

		public int getBytesSkipped() {
			return bytesSkipped;
		}

		public void setBytesSkipped(int bytesSkipped) {
			this.bytesSkipped = bytesSkipped;
		}
		
	}
	
}
