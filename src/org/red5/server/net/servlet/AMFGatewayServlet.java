package org.red5.server.net.servlet;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.common.ByteBuffer;
import org.red5.server.api.IContext;
import org.red5.server.api.IScope;
import org.red5.server.api.Red5;
import org.red5.server.net.remoting.codec.RemotingCodecFactory;
import org.red5.server.net.remoting.message.RemotingCall;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet that handles remoting requests.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard (luke@codegent.com)
 */
public class AMFGatewayServlet extends HttpServlet {

	private static final long serialVersionUID = 7174018823796785619L;
    /**
     * Logger
     */
	protected static Log log = LogFactory.getLog(AMFGatewayServlet.class);
    /**
     * AMF MIME type
     */
	public static final String APPLICATION_AMF = "application/x-amf";
    /**
     * Web app context
     */
	protected WebApplicationContext webAppCtx;
    /**
     * Web context
     */
	protected IContext webContext;
    /**
     * Bean factory
     */
	protected BeanFactory netContext;
    /**
     * Remoting codec factory
     */
	protected RemotingCodecFactory codecFactory;

	/** {@inheritDoc} */
    @Override
	public void init() throws ServletException {
		webAppCtx = WebApplicationContextUtils
				.getWebApplicationContext(getServletContext());
		if (webAppCtx != null) {
			webContext = (IContext) webAppCtx.getBean("web.context");
			codecFactory = (RemotingCodecFactory) webAppCtx
					.getBean("remotingCodecFactory");
		} else {
			log.debug("No web context");
		}
	}

	/** {@inheritDoc} */
    @Override
	public void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		if (log.isDebugEnabled()) {
			log.debug("Remoting request" + req.getContextPath() + ' ' + req.getServletPath());
		}
		if (req.getContentType() != null && req.getContentType().equals(APPLICATION_AMF)) {
			serviceAMF(req, resp);
		} else {
			resp.getWriter().write("Red5 : Remoting Gateway");
		}
	}

    /**
     * Works out AMF request
     * @param req                     Request
     * @param resp                    Response
     * @throws ServletException       Servlet exception
     * @throws IOException            I/O exception
     */
    protected void serviceAMF(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			RemotingPacket packet = decodeRequest(req);
			if (packet == null) {
				log.error("Packet should not be null");
				return;
			}
			handleRemotingPacket(req, packet);
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType(APPLICATION_AMF);
			sendResponse(resp, packet);
		} catch (Exception e) {
			log.error("Error handling remoting call", e);
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

    /**
     * Decode request
     * @param req                    Request
     * @return                       Remoting packet
     * @throws Exception             General exception
     */
    protected RemotingPacket decodeRequest(HttpServletRequest req)
			throws Exception {
		ByteBuffer reqBuffer = ByteBuffer.allocate(req.getContentLength());
		ServletUtils.copy(req.getInputStream(), reqBuffer.asOutputStream());
		reqBuffer.flip();
		RemotingPacket packet = (RemotingPacket) codecFactory
				.getSimpleDecoder().decode(null, reqBuffer);
		String path = req.getContextPath();
		if (req.getPathInfo() != null) {
			path += req.getPathInfo();
		}
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		packet.setScopePath(path);
		reqBuffer = null;
		return packet;
	}

    /**
     * Handles AMF request by making calls
     * @param req              Request
     * @param message          Remoting packet
     * @return                 <code>true</code> on success
     */
    protected boolean handleRemotingPacket(HttpServletRequest req,
			RemotingPacket message) {
		IScope scope = webContext.resolveScope(message.getScopePath());
		// Provide a valid IConnection in the Red5 object
		Red5.setConnectionLocal(new ServletConnection(req, scope));

        for (Object o : message.getCalls()) {
            RemotingCall call = (RemotingCall) o;
            webContext.getServiceInvoker().invoke(call, scope);
        }
        return true;
	}

    /**
     * Sends response to client
     * @param resp             Response
     * @param packet           Remoting packet
     * @throws Exception       General exception
     */
    protected void sendResponse(HttpServletResponse resp, RemotingPacket packet)
			throws Exception {
		ByteBuffer respBuffer = codecFactory.getSimpleEncoder().encode(null,
				packet);
		final ServletOutputStream out = resp.getOutputStream();
		resp.setContentLength(respBuffer.limit());
		ServletUtils.copy(respBuffer.asInputStream(), out);
		out.flush();
		out.close();
		respBuffer = null;
    }

}