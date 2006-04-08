package org.red5.server.net.servlet;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.IContext;
import org.red5.server.net.remoting.codec.RemotingCodecFactory;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class AMFGatewayServlet extends HttpServlet {

	private static final long serialVersionUID = 7174018823796785619L;

	protected static Log log =
        LogFactory.getLog(AMFGatewayServlet.class.getName());
	
	public static final String APPLICATION_AMF = "application/x-amf";
	protected WebApplicationContext webAppCtx;
	protected IContext webContext;
	protected BeanFactory netContext;
	protected RemotingCodecFactory codecFactory;
	
	@Override
	public void init() throws ServletException {
		webAppCtx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		if(webAppCtx != null){
			webContext  = (IContext) webAppCtx.getBean("web.context");
			codecFactory = (RemotingCodecFactory) webAppCtx.getBean("remotingCodecFactory");
		} else {
			log.debug("No web context");
		}
	}

	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		log.debug("Remoting request"+req.getContextPath()+""+req.getServletPath());
		if(req.getContentType() != null && req.getContentType().equals(APPLICATION_AMF)){
			serviceAMF(req,resp);
		} else resp.getWriter().write("Red5 : Remoting Gateway");	
	}
		
	protected void serviceAMF(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			RemotingPacket packet = decodeRequest(req);
			if(packet == null){
				log.error("Packet should not be null");
				return;
			}
			handleRemotingPacket(packet);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType(APPLICATION_AMF);
			sendResponse(resp, packet);
		} catch (Exception e) {
			log.error("Error handling remoting call", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	protected RemotingPacket decodeRequest(HttpServletRequest req) throws Exception {
		ByteBuffer reqBuffer = ByteBuffer.allocate(req.getContentLength());
		ServletUtils.copy(req.getInputStream(),reqBuffer.asOutputStream());
		reqBuffer.flip();
		RemotingPacket packet = (RemotingPacket) codecFactory.getSimpleDecoder().decode(null, reqBuffer);	
		reqBuffer.release();
		return packet;
	}

	protected boolean handleRemotingPacket(RemotingPacket message){
		Iterator it = message.getCalls().iterator();
		while(it.hasNext()){
			RemotingCall call = (RemotingCall) it.next();
			webContext.getServiceInvoker().invoke(call, webContext);
		}
		return true;
	}
	
	protected void sendResponse(HttpServletResponse resp, RemotingPacket packet) throws Exception{
		ByteBuffer respBuffer = codecFactory.getSimpleEncoder().encode(null, packet);
		final ServletOutputStream out = resp.getOutputStream();
        resp.setContentLength(respBuffer.limit());
		ServletUtils.copy(respBuffer.asInputStream(),out);
		respBuffer.release();
		out.flush();
		out.close();
	}
	
}