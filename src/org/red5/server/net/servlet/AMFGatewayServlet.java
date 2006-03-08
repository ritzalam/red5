package org.red5.server.net.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.WriteFuture;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeConnector;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;


public class AMFGatewayServlet extends HttpServlet {

	protected static Log log =
        LogFactory.getLog(AMFGatewayServlet.class.getName());
	
	public static final String APPLICATION_AMF = "application/x-amf";
	
	protected void service(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException, IOException {
		
		Continuation cont = ContinuationSupport.getContinuation(req, this);
		if(cont.isNew()){
			// read the packet and send it down the connection to the app
		}
		
		log.info("Service");
		
		if( req.getContentLength() == 0 
				|| req.getContentType() == null
				|| ! req.getContentType().equals(APPLICATION_AMF )){ 
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.getWriter().write("Gateway");
			resp.flushBuffer();
			return;
		}
		
		ByteBuffer reqBuffer = null;
		ByteBuffer respBuffer = null;
		
		try {
			
			//req.getSession().getAttribute(REMOTING_CONNECTOR);
			
			reqBuffer = ByteBuffer.allocate(req.getContentLength());
			ServletUtils.copy(req.getInputStream(),reqBuffer.asOutputStream());
			reqBuffer.flip();
			
			 // Connect to the server.
	        VmPipeConnector connector = new VmPipeConnector();
	        
	       // IoHandlerAdapter handler = 
	        
	        VmPipeAddress address = new VmPipeAddress( 5080 );
		    
	        IoHandler handler = new Handler(req, resp);
	        ConnectFuture connectFuture = connector.connect(address, handler);
	        connectFuture.join();
	        IoSession session = connectFuture.getSession();
	        session.setAttachment(resp);

	        session.write(reqBuffer);
			
	        ContinuationSupport.getContinuation(req, handler).suspend(1000); 
			
		} catch (IOException e) {
		
			e.printStackTrace();
		
		} finally {
						
		}
		log.info("End");
	}
	
	protected class Handler extends IoHandlerAdapter {

		protected HttpServletResponse resp;
		protected HttpServletRequest req;
		
		public Handler(HttpServletRequest req, HttpServletResponse resp){
			this.req = req;
			this.resp = resp;
		}
	
		public void messageReceived(IoSession session, Object message) throws Exception {
			log.info("<< message " + message);
			
			if(message instanceof ByteBuffer){
				final Continuation cont = ContinuationSupport.getContinuation(req, this);
				if(cont.isPending()) cont.resume();
				try {
					final ServletOutputStream out = resp.getOutputStream();
					ByteBuffer buf = (ByteBuffer) message;
					resp.setStatus(HttpServletResponse.SC_OK);
					resp.setContentType(req.getContentType());
			        resp.setContentLength(buf.limit());
					ServletUtils.copy(buf.asInputStream(),out);
					out.flush();
					out.close();
				} catch (IOException e) {
					log.error("Error sending response",e);
				} 
			}
			
		}
		
		
		
		
	}

	
}
