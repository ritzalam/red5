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
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlrpc.XmlRpcServer;
import org.red5.server.api.IContext;
import org.red5.server.statistics.ScopeStatistics;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet that processes the statistics XML-RPC requests.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class StatisticsServlet extends HttpServlet {

	private static Log log = LogFactory.getLog(StatisticsServlet.class
			.getName());

	private XmlRpcServer server = new XmlRpcServer();

	protected WebApplicationContext webAppCtx;

	protected IContext webContext;

	@Override
	public void init() throws ServletException {
		webAppCtx = WebApplicationContextUtils
				.getWebApplicationContext(getServletContext());
		if (webAppCtx == null) {
			throw new ServletException("No web application context found.");
		}

		webContext = (IContext) webAppCtx.getBean("web.context");

		// Register handlers in XML-RPC server
		server.addHandler("scopes", new ScopeStatistics(webContext
				.getGlobalScope()));
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Process request with XML-RPC server
		byte[] result = server.execute(request.getInputStream());
		response.setContentType("text/xml");
		response.setContentLength(result.length);
		OutputStream out = response.getOutputStream();
		out.write(result);
		out.close();
	}
}
