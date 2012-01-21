/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2012 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.servlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcServer;
import org.red5.server.api.IContext;
import org.red5.server.statistics.XmlRpcScopeStatistics;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Servlet that processes the statistics XML-RPC requests.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class StatisticsServlet extends HttpServlet {

	private static final long serialVersionUID = 5810139109603229027L;

	private final transient XmlRpcServer server = new XmlRpcServer();

	protected transient WebApplicationContext webAppCtx;

	protected transient IContext webContext;

	/** {@inheritDoc} */
	@Override
	public void init() throws ServletException {
		webAppCtx = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		if (webAppCtx == null) {
			webAppCtx = (WebApplicationContext) getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		if (webAppCtx == null) {
			throw new ServletException("No web application context found.");
		}

		webContext = (IContext) webAppCtx.getBean("web.context");

		// Register handlers in XML-RPC server
		server.addHandler("scopes", new XmlRpcScopeStatistics(webContext.getGlobalScope()));
	}

	/** {@inheritDoc} */
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Process request with XML-RPC server
		byte[] result = server.execute(request.getInputStream());
		response.setContentType("text/xml");
		response.setContentLength(result.length);
		OutputStream out = response.getOutputStream();
		out.write(result);
		out.close();
	}
}
