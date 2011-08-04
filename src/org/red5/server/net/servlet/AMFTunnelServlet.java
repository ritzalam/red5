package org.red5.server.net.servlet;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.util.HttpConnectionUtil;
import org.slf4j.Logger;

/**
 * Servlet to tunnel to the AMF gateway servlet.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AMFTunnelServlet extends HttpServlet {

	private static final long serialVersionUID = -35436145164322090L;

	protected Logger log = Red5LoggerFactory.getLogger(AMFTunnelServlet.class);

	private static final String REQUEST_TYPE = "application/x-amf";

	private static String postAcceptorURL = "http://localhost:8080/gateway";

	private static int connectionTimeout = 30000;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		//get the url for posting
		if (config.getInitParameter("tunnel.acceptor.url") != null) {
			postAcceptorURL = config.getInitParameter("tunnel.acceptor.url");
		}
		log.debug("POST acceptor URL: {}", postAcceptorURL);
		//get the connection timeout
		if (config.getInitParameter("tunnel.timeout") != null) {
			connectionTimeout = Integer.valueOf(config.getInitParameter("tunnel.timeout"));
		}
		log.debug("POST connection timeout: {}", postAcceptorURL);
	}

	/**
	 * Redirect to HTTP port.
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		DefaultHttpClient client = HttpConnectionUtil.getClient();
		client.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, connectionTimeout);
		//setup POST
		HttpPost post = null;
		try {
			post = new HttpPost(postAcceptorURL);
			String path = req.getContextPath();
			if (path == null) {
				path = "";
			}
			log.debug("Path: {}", path);
			if (req.getPathInfo() != null) {
				path += req.getPathInfo();
			}
			log.debug("Path 2: {}", path);
			int reqContentLength = req.getContentLength();
			if (reqContentLength > 0) {
				log.debug("Request content length: {}", reqContentLength);
				IoBuffer reqBuffer = IoBuffer.allocate(reqContentLength);
				ServletUtils.copy(req.getInputStream(), reqBuffer.asOutputStream());
				reqBuffer.flip();
				post.setEntity(new InputStreamEntity(reqBuffer.asInputStream(), reqContentLength));
				post.addHeader("Content-Type", REQUEST_TYPE);
				// get.setPath(path);
				post.addHeader("Tunnel-request", path);
				// execute the method
				HttpResponse response = client.execute(post);
				int code = response.getStatusLine().getStatusCode();
				log.debug("HTTP response code: {}", code);
				if (code == HttpStatus.SC_OK) {
					HttpEntity entity = response.getEntity();
					if (entity != null) {
						resp.setContentType(REQUEST_TYPE);
						// get the response as bytes
						byte[] bytes = EntityUtils.toByteArray(entity);
						IoBuffer resultBuffer = IoBuffer.wrap(bytes);
						resultBuffer.flip();
						ServletUtils.copy(resultBuffer.asInputStream(), resp.getOutputStream());
						resp.flushBuffer();
					}
				} else {
					resp.sendError(code);
				}
			} else {
				resp.sendError(HttpStatus.SC_BAD_REQUEST);
			}
		} catch (Exception ex) {
			log.error("", ex);
			post.abort();
		}
	}
}
