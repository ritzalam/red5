package org.red5.server.net.servlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.io.utils.HexDump;

public class RequestDumpServlet extends HttpServlet {

	protected static Log log =
        LogFactory.getLog(RequestDumpServlet.class.getName());
	
	public static final String APPLICATION_AMF = "application/x-amf";
	
	protected void service(HttpServletRequest req, HttpServletResponse resp) 
		throws ServletException, IOException {
		
		Enumeration en = req.getHeaderNames();
		while(en.hasMoreElements()){
			String name = (String) en.nextElement();
			log.info(name + " => "+req.getHeader(name));
		}
		
		ByteBuffer reqBuffer = null;
		
		try {
			
			//req.getSession().getAttribute(REMOTING_CONNECTOR);
			
			reqBuffer = ByteBuffer.allocate(req.getContentLength());
			ServletUtils.copy(req.getInputStream(),reqBuffer.asOutputStream());
			//reqBuffer.flip();
			
			log.info( HexDump.formatHexDump(reqBuffer.getHexDump()) );
			
		} catch (IOException e) {
		
			e.printStackTrace();
		
		} finally {
						
		}
		log.info("End");
	}
		
	
}
