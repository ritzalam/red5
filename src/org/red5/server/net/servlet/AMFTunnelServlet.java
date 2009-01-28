package org.red5.server.net.servlet;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2009 by respective authors (see below). All rights reserved.
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.mina.common.ByteBuffer;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/**
 * Servlet to tunnel to the AMF gateway servlet.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AMFTunnelServlet extends HttpServlet {

	/**
	 * Serialization UID
	 */
	private static final long serialVersionUID = -35436145164322090L;

	protected Logger logger = Red5LoggerFactory.getLogger(AMFTunnelServlet.class);

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
		logger.debug("POST acceptor URL: {}", postAcceptorURL);
		//get the connection timeout
		if (config.getInitParameter("tunnel.timeout") != null) {
			connectionTimeout = Integer.valueOf(config.getInitParameter("tunnel.timeout"));
		}		
		logger.debug("POST connection timeout: {}", postAcceptorURL);
	}

	/**
	 * Redirect to HTTP port.
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		HttpClient client = new HttpClient(
				new MultiThreadedHttpConnectionManager());
		client.getHttpConnectionManager().getParams().setConnectionTimeout(connectionTimeout);
		PostMethod get = new PostMethod(postAcceptorURL);

		try {
			// copy all the headers
			// Enumeration headerNames = req.getHeaderNames();
			// while (headerNames.hasMoreElements()) {
			// String headerName = (String) headerNames.nextElement();
			// logger.debug("Adding received header to tunnel request: "
			// + headerName);
			// get.addRequestHeader(headerName, req.getHeader(headerName));
			// }
			// Enumeration parameterNames = req.getParameterNames();
			// while (parameterNames.hasMoreElements()) {
			// String parameterName = (String) parameterNames.nextElement();
			// logger.debug("Adding received parameter to tunnel request: "
			// + parameterName);
			// get.getParams().setParameter(parameterName,
			// req.getParameter(parameterName));
			// }
			// Enumeration attributeNames = req.getAttributeNames();
			// while (attributeNames.hasMoreElements()) {
			// String attributeName = (String) attributeNames.nextElement();
			// logger.debug("Adding received attribute to tunnel request: "
			// + attributeName);
			// }

			String path = req.getContextPath();
			if (path == null) {
				path = "";
			}
			logger.debug("Path: {}", path);
			if (req.getPathInfo() != null) {
				path += req.getPathInfo();
			}
			logger.debug("Path 2: {}", path);

			int reqContentLength = req.getContentLength();
			if (reqContentLength > 0) {
				logger.debug("Request content length: {}", reqContentLength);

				ByteBuffer reqBuffer = ByteBuffer.allocate(reqContentLength);
				ServletUtils.copy(req.getInputStream(), reqBuffer
						.asOutputStream());
				reqBuffer.flip();
				get
						.setRequestEntity(new InputStreamRequestEntity(
								reqBuffer.asInputStream(), reqContentLength,
								REQUEST_TYPE));
				// get.setPath(path);
				get.addRequestHeader("Tunnel-request", path);

				client.executeMethod(get);
				logger.debug("Response code: {}", get.getStatusCode());

				if (get.getStatusCode() == HttpStatus.SC_OK) {
					resp.setContentType(REQUEST_TYPE);
					int responseLength = ((Long) get.getResponseContentLength())
							.intValue();
					ByteBuffer respBuffer = ByteBuffer.allocate(responseLength);
					ServletUtils.copy(get.getResponseBodyAsStream(), respBuffer
							.asOutputStream());
					respBuffer.flip();
					ServletUtils.copy(respBuffer.asInputStream(), resp
							.getOutputStream());
					resp.flushBuffer();
				} else {
					resp.sendError(get.getStatusCode());
				}

			} else {
				resp.sendError(HttpStatus.SC_BAD_REQUEST);
			}
		} catch (Exception ex) {
			logger.error("", ex);
		} finally {
			get.releaseConnection();
		}
	}
}
